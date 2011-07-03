/*
 *
 *    OPEN-XCHANGE legal information
 *
 *    All intellectual property rights in the Software are protected by
 *    international copyright laws.
 *
 *
 *    In some countries OX, OX Open-Xchange, open xchange and OXtender
 *    as well as the corresponding Logos OX Open-Xchange and OX are registered
 *    trademarks of the Open-Xchange, Inc. group of companies.
 *    The use of the Logos is not covered by the GNU General Public License.
 *    Instead, you are allowed to use these Logos according to the terms and
 *    conditions of the Creative Commons License, Version 2.5, Attribution,
 *    Non-commercial, ShareAlike, and the interpretation of the term
 *    Non-commercial applicable to the aforementioned license is published
 *    on the web site http://www.open-xchange.com/EN/legal/index.html.
 *
 *    Please make sure that third-party modules and libraries are used
 *    according to their respective licenses.
 *
 *    Any modifications to this package must retain all copyright notices
 *    of the original copyright holder(s) for the original code used.
 *
 *    After any such modifications, the original and derivative code shall remain
 *    under the copyright of the copyright holder(s) and/or original author(s)per
 *    the Attribution and Assignment Agreement that can be located at
 *    http://www.open-xchange.com/EN/developer/. The contributing author shall be
 *    given Attribution for the derivative code and a license granting use.
 *
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
 *     Mail: info@open-xchange.com
 *
 *
 *     This program is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License, Version 2 as published
 *     by the Free Software Foundation.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *     or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *     for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc., 59
 *     Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package com.openexchange.mail.cache;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.openexchange.mail.MailException;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.cache.queue.MailAccessQueue;
import com.openexchange.mail.cache.queue.MailAccessQueueImpl;
import com.openexchange.mail.cache.queue.SingletonMailAccessQueue;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountException;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.server.ServiceException;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;

/**
 * {@link EnqueueingMailAccessCache} - A very volatile cache for already connected instances of {@link MailAccess}.
 * <p>
 * A bounded {@link Queue} is used to store {@link MailAccess} instances to improve subsequent mail requests.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class EnqueueingMailAccessCache implements IMailAccessCache {

    /**
     * The logger instance.
     */
    protected static final org.apache.commons.logging.Log LOG = com.openexchange.exception.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(EnqueueingMailAccessCache.class));

    /**
     * The flag whether debug logging is enabled.
     */
    protected static final boolean DEBUG = LOG.isDebugEnabled();

    /**
     * Drop those queues of which all elements timed-out.
     */
    private static final boolean DROP_TIMED_OUT_QUEUES = false;

    private static volatile EnqueueingMailAccessCache singleton;

    /**
     * Gets the singleton instance.
     * 
     * @queueCapacity The max. queue capacity
     * @return The singleton instance
     * @throws MailException If instance initialization fails
     */
    public static EnqueueingMailAccessCache getInstance(final int queueCapacity) throws MailException {
        if (null == singleton) {
            synchronized (EnqueueingMailAccessCache.class) {
                if (null == singleton) {
                    singleton = new EnqueueingMailAccessCache(queueCapacity);
                }
            }
        }
        return singleton;
    }

    /**
     * Releases the singleton instance.
     */
    public static void releaseInstance() {
        if (null != singleton) {
            synchronized (EnqueueingMailAccessCache.class) {
                if (null != singleton) {
                    singleton.dispose();
                    singleton = null;
                }
            }
        }
    }

    /*
     * Field members
     */

    private final ConcurrentMap<Key, MailAccessQueue> map;

    private final int defaultIdleSeconds;

    private final ScheduledTimerTask timerTask;

    /**
     * The number of {@link MailAccess} instances which may be concurrently active/opened per account for a user.
     */
    private final int queueCapacity;

    /**
     * Prevent instantiation.
     * @queueCapacity The max. queue capacity
     * 
     * @throws MailException If an error occurs
     */
    private EnqueueingMailAccessCache(final int queueCapacity) throws MailException {
        super();
        this.queueCapacity = queueCapacity;
        try {
            map = new ConcurrentHashMap<Key, MailAccessQueue>();
            final int configuredIdleSeconds = MailProperties.getInstance().getMailAccessCacheIdleSeconds();
            defaultIdleSeconds = configuredIdleSeconds <= 0 ? 7 : configuredIdleSeconds;
            /*
             * Add timer task
             */
            final TimerService service = ServerServiceRegistry.getInstance().getService(TimerService.class, true);
            final int configuredShrinkerSeconds = MailProperties.getInstance().getMailAccessCacheShrinkerSeconds();
            final int shrinkerMillis = (configuredShrinkerSeconds <= 0 ? 3 : configuredShrinkerSeconds) * 1000;
            timerTask = service.scheduleWithFixedDelay(new PurgeExpiredRunnable(map), shrinkerMillis, shrinkerMillis);
        } catch (final ServiceException e) {
            throw new MailException(e);
        }
    }

    /**
     * Removes and returns a mail access from cache.
     * 
     * @param session The session
     * @param accountId The account ID
     * @return An active instance of {@link MailAccess} or <code>null</code>
     */
    public MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> removeMailAccess(final Session session, final int accountId) {
        final Key key = keyFor(accountId, session);
        final MailAccessQueue accessQueue = map.get(key);
        if (null == accessQueue) {
            return null;
        }
        synchronized (accessQueue) {
            if (accessQueue.isDeprecated()) {
                return null;
            }
            final PooledMailAccess pooledMailAccess = accessQueue.poll();
            if (null == pooledMailAccess) {
                return null;
            }
            final MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = pooledMailAccess.getMailAccess();
            mailAccess.setCached(false);
            if (DEBUG) {
                LOG.debug(new StringBuilder("Remove&Get for ").append(key).toString());
            }
            return mailAccess;
        }
    }

    /**
     * Puts given mail access into cache if none user-bound connection is already contained in cache.
     * 
     * @param session The session
     * @param accountId The account ID
     * @param mailAccess The mail access to put into cache
     * @return <code>true</code> if mail access could be successfully cached; otherwise <code>false</code>
     */
    public boolean putMailAccess(final Session session, final int accountId, final MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) {
        int idleSeconds = mailAccess.getCacheIdleSeconds();
        if (idleSeconds <= 0) {
            idleSeconds = defaultIdleSeconds;
        }
        final Key key = keyFor(accountId, session);
        MailAccessQueue accessQueue = map.get(key);
        if (null == accessQueue || accessQueue.isDeprecated()) {
            final MailAccessQueue tmp = 1 == queueCapacity ? new SingletonMailAccessQueue() : new MailAccessQueueImpl(queueCapacity);
            accessQueue = map.putIfAbsent(key, tmp);
            if (null == accessQueue) {
                accessQueue = tmp;
            }
        }
        synchronized (accessQueue) {
            if (accessQueue.isDeprecated()) {
                return false;
            }
            /*
             * Insert subsequent MailAccess instances with halved time-to-live seconds
             */
            idleSeconds = accessQueue.isEmpty() ? idleSeconds : (idleSeconds >> 1);
            if (accessQueue.offer(PooledMailAccess.valueFor(mailAccess, idleSeconds * 1000L))) {
                mailAccess.setCached(true);
                if (DEBUG) {
                    final int size = accessQueue.size();
                    if (size == 1) {
                        LOG.debug(new StringBuilder("Queued ONE mail access for ").append(key).toString());
                    } else if (size > queueCapacity) {
                        LOG.debug(new StringBuilder("\n\tExceeded queue capacity! Detected ").append(size).append(" mail access(es) for ").append(
                            key).append('\n').toString());
                    } else if (size == queueCapacity) {
                        LOG.debug(new StringBuilder("\n\tReached queue capacity! Queued ").append(size).append(" mail access(es) for ").append(
                            key).append('\n').toString());
                    } else {
                        LOG.debug(new StringBuilder("Queued ").append(size).append(" mail access(es) for ").append(key).toString());
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Checks if cache already holds a user-bound mail access for specified account.
     * 
     * @param session The session
     * @param accountId The account ID
     * @return <code>true</code> if a user-bound mail access is already present in cache; otherwise <code>false</code>
     */
    public boolean containsMailAccess(final Session session, final int accountId) {
        final MailAccessQueue accessQueue = map.get(keyFor(accountId, session));
        if (null == accessQueue) {
            return false;
        }
        synchronized (accessQueue) {
            return !accessQueue.isDeprecated() && !accessQueue.isEmpty();
        }
    }

    /**
     * Disposes this cache.
     */
    protected void dispose() {
        timerTask.cancel(false);
        for (final Key key : new HashSet<Key>(map.keySet())) {
            orderlyClearQueue(key);
        }
    }

    /**
     * Clears specified queue orderly.
     * 
     * @param key The key associated with the queue
     */
    protected void orderlyClearQueue(final Key key) {
        final MailAccessQueue accessQueue = map.remove(key);
        if (null == accessQueue) {
            return;
        }
        synchronized (accessQueue) {
            accessQueue.markDeprecated();
            PooledMailAccess pooledMailAccess;
            while (null != (pooledMailAccess = accessQueue.poll())) {
                final MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = pooledMailAccess.getMailAccess();
                if (DEBUG) {
                    LOG.debug(new StringBuilder("Dropping: ").append(mailAccess).toString());
                }
                mailAccess.setCached(false);
                mailAccess.close(false);
            }
        }
    }

    /**
     * Clears the cache entries kept for specified user.
     * 
     * @param session The session
     * @throws MailException If clearing user entries fails
     */
    public void clearUserEntries(final Session session) throws MailException {
        try {
            /*
             * Check if last...
             */
            final SessiondService service = ServerServiceRegistry.getInstance().getService(SessiondService.class);
            final int user = session.getUserId();
            final int cid = session.getContextId();
            if (null == service || 0 == service.getUserSessions(user, cid)) {
                final MailAccountStorageService storageService =
                    ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class, true);
                final MailAccount[] accounts = storageService.getUserMailAccounts(user, cid);
                for (final MailAccount mailAccount : accounts) {
                    orderlyClearQueue(keyFor(mailAccount.getId(), session));
                }
            }
        } catch (final ServiceException e) {
            throw new MailException(e);
        } catch (final MailAccountException e) {
            throw new MailException(e);
        }
    }

    private static Key keyFor(final int accountId, final Session session) {
        return new Key(accountId, session.getUserId(), session.getContextId());
    }

    private static final class PurgeExpiredRunnable implements Runnable {

        private final ConcurrentMap<Key, MailAccessQueue> map;

        protected PurgeExpiredRunnable(final ConcurrentMap<Key, MailAccessQueue> map) {
            super();
            this.map = map;
        }

        public void run() {
            try {
                /*
                 * Shall timed-out queues be removed from mapping?
                 */
                final boolean dropQueue = isDropQueue();
                /*
                 * Iterate mapping
                 */
                for (final Iterator<Entry<Key, MailAccessQueue>> iterator = map.entrySet().iterator(); iterator.hasNext();) {
                    final Entry<Key, MailAccessQueue> entry = iterator.next();
                    final MailAccessQueue accessQueue = entry.getValue();
                    synchronized (accessQueue) {
                        PooledMailAccess pooledMailAccess;
                        while (null != (pooledMailAccess = accessQueue.pollDelayed())) {
                            final MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess =
                                pooledMailAccess.getMailAccess();
                            mailAccess.setCached(false);
                            if (DEBUG) {
                                LOG.debug(new StringBuilder("Timed-out mail access for ").append(entry.getKey()).toString());
                            }
                            mailAccess.close(false);
                        }
                        if (dropQueue && accessQueue.isEmpty()) {
                            /*
                             * Current queue is empty. Mark as deprecated
                             */
                            accessQueue.markDeprecated();
                            if (DEBUG) {
                                LOG.debug(new StringBuilder("Dropped queue for ").append(entry.getKey()).toString());
                            }
                            iterator.remove();
                        }
                    }
                }
            } catch (final RuntimeException e) {
                // A runtime exception
                LOG.warn("Purge-expired run failed: " + e.getMessage(), e);
            }
        }

        private boolean isDropQueue() {
            return DROP_TIMED_OUT_QUEUES;
        }
    }

    private static final class Key {

        private final int user;

        private final int context;

        private final int accountId;

        private final int hash;

        protected Key(final int accountId, final int user, final int context) {
            super();
            this.user = user;
            this.context = context;
            this.accountId = accountId;
            hash = hashCode0();
        }

        private int hashCode0() {
            final int prime = 31;
            int result = 1;
            result = prime * result + accountId;
            result = prime * result + context;
            result = prime * result + user;
            return result;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Key other = (Key) obj;
            if (accountId != other.accountId) {
                return false;
            }
            if (context != other.context) {
                return false;
            }
            if (user != other.user) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder(16);
            builder.append("{ Key [accountId=").append(accountId).append(", user=").append(user).append(", context=").append(context).append(
                "] }");
            return builder.toString();
        }

    }

}
