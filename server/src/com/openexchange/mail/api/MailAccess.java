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

package com.openexchange.mail.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.mail.MailAccessWatcher;
import com.openexchange.exception.OXException;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailInitialization;
import com.openexchange.mail.MailProviderRegistry;
import com.openexchange.mail.cache.EnqueueingMailAccessCache;
import com.openexchange.mail.cache.IMailAccessCache;
import com.openexchange.mail.cache.SingletonMailAccessCache;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.exception.OXException;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.server.OXException;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondService;

/**
 * {@link MailAccess} - Handles connecting to the mailing system while using an internal cache for connected access objects (see
 * {@link SingletonMailAccessCache}).
 * <p>
 * Moreover it provides access to either message storage, folder storage and logic tools.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class MailAccess<F extends IMailFolderStorage, M extends IMailMessageStorage> implements Serializable {

    /**
     * Serial version UID
     */
    private static final long serialVersionUID = -2580495494392812083L;

    private static final transient org.apache.commons.logging.Log LOG = com.openexchange.exception.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(MailAccess.class));

    private static final class Key {

        private final int user;

        private final int cid;

        private final int accountId;

        private final int hash;

        public Key(final int user, final int cid, final int accountId) {
            super();
            this.user = user;
            this.cid = cid;
            this.accountId = accountId;
            this.hash = hashCode0();
        }

        private int hashCode0() {
            final int prime = 31;
            int result = 1;
            result = prime * result + accountId;
            result = prime * result + cid;
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
            if (cid != other.cid) {
                return false;
            }
            if (user != other.user) {
                return false;
            }
            return true;
        }

    } // End of class Key

    private static final Object PRESENT = new Object();

    private static final BlockingQueue<Object> NO_RESTRICTION = new ArrayBlockingQueue<Object>(1);

    private static final ConcurrentMap<Key, BlockingQueue<Object>> COUNTER_MAP = new ConcurrentHashMap<MailAccess.Key, BlockingQueue<Object>>();

    private static Key getUserKey(final int user, final int accountId, final int cid) {
        return new Key(user, cid, accountId);
    }

    /**
     * Clears the counter map.
     */
    public static void clearCounterMap() {
        final Set<Entry<Key, BlockingQueue<Object>>> entrySet = COUNTER_MAP.entrySet();
        for (final Iterator<Entry<Key, BlockingQueue<Object>>> iterator = entrySet.iterator(); iterator.hasNext();) {
            final BlockingQueue<Object> queue = iterator.next().getValue();
            iterator.remove();
            while (queue.poll() != null) {
                ;
            }
        }
    }

    /**
     * Signal a closed {@link MailAccess} instance.
     * 
     * @param mailAccess The mail access which has been closed
     */
    private static void signalClosed(final MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) {
        if (MailAccount.DEFAULT_ID != mailAccess.accountId) {
            final Session session = mailAccess.session;
            final BlockingQueue<Object> queue = COUNTER_MAP.get(getUserKey(session.getUserId(), mailAccess.accountId, session.getContextId()));
            if (null != queue) {
                /*
                 * Dequeue
                 */
                queue.poll();
            }
        }
    }

    /**
     * Signals that specified {@link MailAccess} which shall be connected. Waiting if capacity bounds specified for a closed {@link MailAccess} instance.
     * 
     * @param accountId The account ID
     * @param session The session
     * @param provider The mail provider
     * @return <code>true</code> if an immediate enqueue was possible; otherwise <code>false</code> for a blocking enqueue.
     * @throws OXException If protocol specifies capacity bounds and waiting for space is interrupted
     */
    private static boolean signalConnectAttempt(final int accountId, final Session session, final MailProvider provider) throws OXException {
        if (MailAccount.DEFAULT_ID == accountId) {
            /*
             * No capacity restrictions for primary account.
             */
            return true;
        }
        final int userId = session.getUserId();
        final int contextId = session.getContextId();
        final Key key = getUserKey(userId, accountId, contextId);
        BlockingQueue<Object> queue = COUNTER_MAP.get(key);
        if (null == queue) {
            try {
                final MailAccountStorageService mass = ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class, true);
                final int max = provider.getProtocol().getMaxCount(mass.getMailAccount(accountId, userId, contextId).getMailServer());
                if (max > 0) {
                    final BlockingQueue<Object> nq = new ArrayBlockingQueue<Object>(max);
                    queue = COUNTER_MAP.putIfAbsent(key, nq);
                    if (null == queue) {
                        queue = nq;
                    }
                } else {
                    queue = COUNTER_MAP.putIfAbsent(key, NO_RESTRICTION);
                    if (null == queue) {
                        queue = NO_RESTRICTION;
                    }
                }
            } catch (final OXException e) {
                throw new OXException(e);
            } catch (final OXException e) {
                throw new OXException(e);
            }
        }
        if ((null == queue) || (NO_RESTRICTION.equals(queue)) || queue.offer(PRESENT)) {
            /*
             * No capacity restrictions or space was immediately available
             */
            return true;
        }
        /*-
         * Wasn't possible to immediately add to queue:
         * 
         * Perform blocking enqueue (waiting if necessary for space to become available).
         */
        try {
            queue.put(PRESENT);
        } catch (final InterruptedException e) {
            throw MailExceptionCode.INTERRUPT_ERROR.create(e, e.getMessage());
        }
        return false;
    }

    /*-
     * ############### MEMBERS ###############
     */

    protected final transient Session session;

    protected final int accountId;

    protected final Collection<OXException> warnings;

    protected boolean cacheable;

    /**
     * Indicates if {@link MailAccess} is currently held in {@link SingletonMailAccessCache}.
     */
    protected boolean cached;

    /**
     * A flag to check if this {@link MailAccess} is connected, but in IDLE mode, waiting for any server notifications.
     */
    protected boolean waiting;

    protected MailProvider provider;

    private transient MailConfig mailConfig;

    private Properties mailProperties;

    private transient Thread usingThread;

    private StackTraceElement[] trace;

    /**
     * Initializes a new {@link MailAccess} for session user's default mail account.
     * 
     * @param session The session
     */
    protected MailAccess(final Session session) {
        this(session, MailAccount.DEFAULT_ID);
    }

    /**
     * Initializes a new {@link MailAccess}.
     * 
     * @param session The session
     * @param accountId The account ID
     */
    protected MailAccess(final Session session, final int accountId) {
        super();
        warnings = new ArrayList<OXException>(2);
        this.session = session;
        this.accountId = accountId;
        cacheable = true;
    }

    /**
     * Gets the session associated with this {@link MailAccess} instance.
     * 
     * @return The session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Sets the associated {@link MailProvider} instance.
     * 
     * @param provider The mail provider
     * @return This instance with mail provider applied
     */
    protected MailAccess<F, M> setProvider(final MailProvider provider) {
        this.provider = provider;
        return this;
    }

    /**
     * Gets the associated {@link MailProvider} instance.
     * 
     * @return The mail provider
     */
    public MailProvider getProvider() {
        return provider;
    }

    /**
     * Adds given warnings.
     * 
     * @param warnings The warnings to add
     */
    public void addWarnings(final Collection<OXException> warnings) {
        this.warnings.addAll(warnings);
    }

    /**
     * Gets possible warnings.
     * 
     * @return Possible warnings.
     */
    public Collection<OXException> getWarnings() {
        return Collections.unmodifiableCollection(warnings);
    }

    /**
     * Resets this access' settings. Should be called when {@link #closeInternal()} is invoked.
     */
    protected final void resetFields() {
        mailProperties = null;
        usingThread = null;
        trace = null;
    }

    /**
     * Triggers all implementation-specific startup actions.
     * 
     * @param mailAccess An instance of {@link MailAccess}
     * @throws OXException If implementation-specific startup fails
     */
    protected static void startupImpl(final MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) throws OXException {
        mailAccess.startup();
    }

    /**
     * Triggers all implementation-specific shutdown actions.
     * 
     * @param mailAccess An instance of {@link MailAccess}
     * @throws OXException If implementation-specific shutdown fails
     */
    protected static void shutdownImpl(final MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) throws OXException {
        mailAccess.shutdown();
    }

    /**-
     * The max. number of {@link MailAccess} instanced allowed being cached concurrently for a user's account.
     * 
     * TODO: Add to configuration?
     */
    private static final int MAX_PER_USER = 2;

    /**
     * Gets the appropriate {@link IMailAccessCache mail access cache} instance.
     * 
     * @return The mail access cache
     * @throws OXException If cache cannot be initialized
     */
    public static IMailAccessCache getMailAccessCache() throws OXException {
        return 1 == MAX_PER_USER ? SingletonMailAccessCache.getInstance() : EnqueueingMailAccessCache.getInstance(MAX_PER_USER);
    }

    /**
     * Gets the proper instance of {@link MailAccess} for session user's default mail account.
     * <p>
     * When starting to work with obtained {@link MailAccess mail access} at first its {@link #connect()} method is supposed to be invoked.
     * On finished work the final {@link #close(boolean)} must be called:
     * 
     * <pre>
     * final MailAccess mailAccess = MailAccess.getInstance(session);
     * mailAccess.connect();
     * try {
     *  // Do something
     * } finally {
     *  mailAccess.close(putToCache)
     * }
     * </pre>
     * 
     * @param session The session
     * @return A proper instance of {@link MailAccess}
     * @throws OXException If instantiation fails or a caching error occurs
     */
    public static final MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> getInstance(final Session session) throws OXException {
        return getInstance(session, MailAccount.DEFAULT_ID);
    }

    /**
     * Gets the proper instance of {@link MailAccess} parameterized with given session and account ID.
     * <p>
     * When starting to work with obtained {@link MailAccess mail access} at first its {@link #connect()} method is supposed to be invoked.
     * On finished work the final {@link #close(boolean)} must be called:
     * 
     * <pre>
     * final MailAccess mailAccess = MailAccess.getInstance(session, accountID);
     * mailAccess.connect();
     * try {
     * 	// Do something
     * } finally {
     * 	mailAccess.close(putToCache)
     * }
     * </pre>
     * 
     * @param session The session
     * @param accountId The account ID
     * @return A proper instance of {@link MailAccess}
     * @throws OXException If instantiation fails or a caching error occurs
     */
    public static final MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> getInstance(final Session session, final int accountId) throws OXException {
        /*
         * Check for proper initialization
         */
        if (!MailInitialization.getInstance().isInitialized()) {
            throw MailExceptionCode.INITIALIZATION_PROBLEM.create();
        }
        /*
         * Check MailAccessCache
         */
        {
            final MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess =
                getMailAccessCache().removeMailAccess(session, accountId);
            if (mailAccess != null) {
                return mailAccess;
            }
        }
        if (MailAccount.DEFAULT_ID == accountId) {
            /*
             * No cached connection available, check for admin login
             */
            checkAdminLogin(session, accountId);
        }
        /*
         * Return new MailAccess instance
         */
        final MailProvider mailProvider = MailProviderRegistry.getMailProviderBySession(session, accountId);
        if (signalConnectAttempt(accountId, session, mailProvider)) {
            /*
             * Immediate enqueue performed
             */
            return mailProvider.createNewMailAccess(session, accountId).setProvider(mailProvider);
        }
        /*-
         * Blocking enqueue performed:
         * 
         * Re-check cache
         */
        final MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess =
            getMailAccessCache().removeMailAccess(session, accountId);
        return null == mailAccess ? mailProvider.createNewMailAccess(session, accountId).setProvider(mailProvider) : mailAccess;
    }

    /**
     * Gets the proper instance of {@link MailAccess} for specified user's default account.
     * <p>
     * When starting to work with obtained {@link MailAccess mail access} at first its {@link #connect()} method is supposed to be invoked.
     * On finished work the final {@link #close(boolean)} must be called:
     * 
     * <pre>
     * final MailAccess mailAccess = MailAccess.getInstance(session, accountID);
     * mailAccess.connect();
     * try {
     *  // Do something
     * } finally {
     *  mailAccess.close(putToCache)
     * }
     * </pre>
     * 
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return An appropriate {@link MailAccess mail access}
     * @throws OXException If instantiation fails or a caching error occurs
     */
    public static final MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> getInstance(final int userId, final int contextId) throws OXException {
        return getInstance(userId, contextId, MailAccount.DEFAULT_ID);
    }

    /**
     * Gets the proper instance of {@link MailAccess} for specified user and account ID.
     * <p>
     * When starting to work with obtained {@link MailAccess mail access} at first its {@link #connect()} method is supposed to be invoked.
     * On finished work the final {@link #close(boolean)} must be called:
     * 
     * <pre>
     * final MailAccess mailAccess = MailAccess.getInstance(session, accountID);
     * mailAccess.connect();
     * try {
     *  // Do something
     * } finally {
     *  mailAccess.close(putToCache)
     * }
     * </pre>
     * 
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param accountId The account identifier
     * @return An appropriate {@link MailAccess mail access}
     * @throws OXException If instantiation fails or a caching error occurs
     */
    public static final MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> getInstance(final int userId, final int contextId, final int accountId) throws OXException {
        final SessiondService sessiondService = ServerServiceRegistry.getInstance().getService(SessiondService.class);
        if (null != sessiondService) {
            final Collection<Session> sessions = sessiondService.getSessions(userId, contextId);
            if (!sessions.isEmpty()) {
                return getInstance(sessions.iterator().next(), accountId);
            }
        }
        /*
         * No appropriate session found.
         */
        throw MailExceptionCode.UNEXPECTED_ERROR.create("No appropriate session found.");
    }

    /**
     * @return the global access counter
     */
    public static final int getCounter() {
        return MailAccessWatcher.getNumberOfMailAccesses();
    }

    /**
     * Increments the global access counter.
     * <p>
     * <b>Does nothing at all since v6.20.</b>
     */
    protected static final void incrementCounter() {
        // No-op
    }

    /**
     * Decrements the global access counter.
     * <p>
     * <b>Does nothing at all since v6.20.</b>
     */
    protected static final void decrementCounter() {
        // No-op
    }

    /**
     * Gets the optional properties used on connect.
     * 
     * @return the mailProperties
     */
    public Properties getMailProperties() {
        return mailProperties;
    }

    /**
     * Sets optional properties used on connect. Herewith additional properties can be applied and checked later on.
     * 
     * @param mailProperties The properties
     */
    public void setMailProperties(final Properties mailProperties) {
        this.mailProperties = mailProperties;
    }

    /**
     * Checks if all necessary fields are set in this access object.
     * <p>
     * This routine is implicitly invoked by {@link #connect()}.
     * 
     * @throws OXException If a necessary field is missing
     * @see #connect()
     */
    protected void checkFieldsBeforeConnect(final MailConfig mailConfig) throws OXException {

        /*
         * Properties are implementation specific and therefore are created within connectInternal()
         */
        if (mailConfig.getServer() == null) {
            throw MailExceptionCode.MISSING_CONNECT_PARAM.create("mail server");
        } else if (delegateCheckMailServerPort() && (mailConfig.getPort() <= 0)) {
            throw MailExceptionCode.MISSING_CONNECT_PARAM.create("mail server port");
        } else if (mailConfig.getLogin() == null) {
            throw MailExceptionCode.MISSING_CONNECT_PARAM.create("login");
        } else if (mailConfig.getPassword() == null) {
            throw MailExceptionCode.MISSING_CONNECT_PARAM.create("password");
        }
    }

    /**
     * Pings the mail server to check if a connection can be established and and immediately closes connection.
     * <p>
     * Default implementation just delegates to {@link #connect()} but may be overridden in implementing subclass if not appropriate or a
     * faster way can be achieved.
     * 
     * @return <code>true</code> if a connection can be established; otherwise <code>false</code>
     * @throws OXException If the ping fails
     */
    public boolean ping() throws OXException {
        try {
            connect0(false);
            close(false);
            return true;
        } catch (final OXException e) {
            return false;
        }
    }

    /**
     * Opens this access. May be invoked on an already opened access.
     * 
     * @throws OXException If the connection could not be established for various reasons
     */
    public final void connect() throws OXException {
        connect0(true);
    }

    /**
     * Opens this access. May be invoked on an already opened access.
     * 
     * @param checkDefaultFolders <code>true</code> to check existence of default folders; otherwise <code>false</code> to omit check
     * @throws OXException If the connection could not be established for various reasons
     */
    public final void connect(final boolean checkDefaultFolders) throws OXException {
        connect0(checkDefaultFolders);
    }

    /**
     * Convenience method to obtain root folder in a fast way; meaning no default folder check is performed which is not necessary to return
     * the root folder.
     * <p>
     * The same result is yielded through calling <code>getFolderStorage().getRootFolder()</code> on a connected {@link MailAccess}.
     * <p>
     * Since this mail access instance is connected if not already done before, the {@link #close(boolean)} operation should be invoked
     * afterwards:
     * 
     * <pre>
     * final MailAccess mailAccess = MailAccess.getInstance(session);
     * final MailFolder rootFolder = mailAccess.getRootFolder();
     * try {
     *  // Do something with root folder
     * } finally {
     *  mailAccess.close(putToCache)
     * }
     * </pre>
     * 
     * @throws OXException If returning the root folder fails
     */
    public MailFolder getRootFolder() throws OXException {
        if (!isConnected()) {
            connect0(false);
        }
        return getFolderStorage().getRootFolder();
    }

    /**
     * Convenience method to obtain folder's number of unread messages in a fast way; meaning no default folder check is performed.
     * <p>
     * The same result is yielded through calling <code>getFolderStorage().getFolder().getUnreadMessageCount()</code> on a connected
     * {@link MailAccess}.
     * <p>
     * Since this mail access instance is connected if not already done before, the {@link #close(boolean)} operation should be invoked
     * afterwards:
     * 
     * <pre>
     * final MailAccess mailAccess = MailAccess.getInstance(session);
     * final int unreadCount = mailAccess.getNumberOfUnreadMessages();
     * try {
     *  // Do something with unread count
     * } finally {
     *  mailAccess.close(putToCache)
     * }
     * </pre>
     * 
     * @throws OXException If returning the unread count fails
     */
    public int getUnreadMessagesCount(final String fullname) throws OXException {
        if (!isConnected()) {
            connect0(false);
        }
        return getFolderStorage().getFolder(fullname).getUnreadMessageCount();
    }

    private final void connect0(final boolean checkDefaultFolder) throws OXException {
        applyNewThread();
        if (isConnected()) {
            if (checkDefaultFolder) {
                checkDefaultFolderOnConnect();
            }
        } else {
            checkFieldsBeforeConnect(getMailConfig());
            delegateConnectInternal();
            if (checkDefaultFolder) {
                checkDefaultFolderOnConnect();
            }
        }
        MailAccessWatcher.addMailAccess(this);
    }

    private void checkDefaultFolderOnConnect() throws OXException {
        try {
            getFolderStorage().checkDefaultFolders();
        } catch (final OXException e) {
            throw e;
        } catch (final Exception e) {
            final MailConfig mailConfig = getMailConfig();
            final OXException mailExc =
                new OXException(
                    MailExceptionCode.DEFAULT_FOLDER_CHECK_FAILED,
                    e,
                    mailConfig.getServer(),
                    Integer.valueOf(session.getUserId()),
                    mailConfig.getLogin(),
                    Integer.valueOf(session.getContextId()),
                    e.getMessage());
            LOG.error(mailExc.getMessage(), mailExc);
            delegateCloseInternal();
            throw mailExc;
        }
    }

    /**
     * Internal connect method to establish a mail connection.
     * 
     * @param mailConfig The mail configuration providing connect and login data
     * @throws OXException If connection could not be established
     */
    protected abstract void connectInternal() throws OXException;

    /**
     * For delegating purpose. Don't invoke.
     */
    public void delegateConnectInternal() throws OXException {
        connectInternal();
    }

    /**
     * Closes this access.
     * <p>
     * An already closed access is not going to be put into cache and is treated as a no-op.
     * 
     * @param put2Cache <code>true</code> to try to put this mail connection into cache; otherwise <code>false</code>
     */
    public final void close(final boolean put2Cache) {
        if (!isConnectedUnsafe()) {
            signalClosed(this);
            return;
        }
        boolean put = put2Cache;
        try {
            try {
                /*
                 * Release all used, non-cachable resources
                 */
                delegateReleaseResources();
            } catch (final Throwable t) {
                /*
                 * Dropping
                 */
                LOG.error("Resources could not be properly released. Dropping mail connection for safety reasons", t);
                put = false;
            }
            // resetFields();
            try {
                /*
                 * Cache connection if desired/possible anymore
                 */
                if (put && isCacheable() && getMailAccessCache().putMailAccess(session, accountId, this)) {
                    /*
                     * Successfully cached: return
                     */
                    signalClosed(this);
                    return;
                }
            } catch (final OXException e) {
                LOG.error(e.getMessage(), e);
            }
            /*
             * Close mail connection
             */
            delegateCloseInternal();
            if (!cached) {
                /*
                 * Not closed by MailAccessCache
                 */
                signalClosed(this);
            }
        } finally {
            /*
             * Remove from watcher no matter if cached or closed
             */
            MailAccessWatcher.removeMailAccess(this);
        }
    }

    /**
     * Gets the trace of the thread that lastly obtained this access.
     * <p>
     * This is useful to detect certain threads which uses an access for a long time
     * 
     * @return the trace of the thread that lastly obtained this access
     */
    public final String getTrace() {
        final StringBuilder sBuilder = new StringBuilder(512);
        sBuilder.append(toString());
        sBuilder.append("\nIMAP connection established (or fetched from cache) at: ").append('\n');
        /*
         * Start at index 3
         */
        for (int i = 3; i < trace.length; i++) {
            sBuilder.append("\tat ").append(trace[i]).append('\n');
        }
        if ((null != usingThread) && usingThread.isAlive()) {
            sBuilder.append("Current Using Thread: ").append(usingThread.getName()).append('\n');
            /*
             * Only possibility to get the current working position of a thread. This is only called if a thread is caught by
             * MailAccessWatcher.
             */
            final StackTraceElement[] trace = usingThread.getStackTrace();
            sBuilder.append("\tat ").append(trace[0]);
            for (int i = 1; i < trace.length; i++) {
                sBuilder.append('\n').append("\tat ").append(trace[i]);
            }
        }
        return sBuilder.toString();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(256);
        builder.append("{ MailAccess [accountId=").append(accountId).append(", cached=").append(cached).append(", ");
        if (provider != null) {
            builder.append("provider=").append(provider).append(", ");
        }
        if (mailConfig != null) {
            builder.append("mailConfig=").append(mailConfig);
        }
        builder.append("] }");
        return builder.toString();
    }

    /**
     * Returns the mail configuration appropriate for current user. It provides needed connection and login information.
     * 
     * @return The mail configuration
     */
    public MailConfig getMailConfig() throws OXException {
        if (null == mailConfig) {
            mailConfig = createMailConfig();
        }
        return mailConfig;
    }

    /**
     * Gets this mail access' account ID.
     * 
     * @return The account ID
     */
    public int getAccountId() {
        return accountId;
    }

    /**
     * Creates a new user-specific mail configuration.
     * 
     * @return A new user-specific mail configuration
     * @throws OXException If creating a new mail configuration fails
     */
    private final MailConfig createMailConfig() throws OXException {
        final MailConfig instance = delegateCreateNewMailConfig();
        instance.setMailProperties(delegateCreateNewMailProperties());
        return MailConfig.getConfig(instance.getClass(), instance, session, accountId);
    }

    /**
     * Apply new thread's trace information.
     */
    private final void applyNewThread() {
        usingThread = Thread.currentThread();
        /*
         * This is faster than Thread.getStackTrace() since a native method is used to fill thread's stack trace
         */
        trace = new Throwable().getStackTrace();
    }

    /**
     * Checks if session's user denotes the context admin user and whether admin user's try to login to mail system is permitted or not.
     * 
     * @param session The session
     * @param accountId The account ID
     * @throws OXException If session's user denotes the context admin user and admin user's try to login to mail system is not permitted
     */
    private static final void checkAdminLogin(final Session session, final int accountId) throws OXException {
        if (!MailProperties.getInstance().isAdminMailLoginEnabled()) {
            /*
             * Admin mail login is not permitted per configuration
             */
            final Context ctx;
            try {
                ctx = ContextStorage.getStorageContext(session.getContextId());
            } catch (final OXException e) {
                throw new OXException(e);
            }
            if (session.getUserId() == ctx.getMailadmin()) {
                throw MailExceptionCode.ACCOUNT_DOES_NOT_EXIST.create(Integer.valueOf(ctx.getContextId()));
            }
        }
    }

    /**
     * Gets the number of seconds this mail access is allowed to remain idle in {@link SingletonMailAccessCache cache} before being removed and
     * closed. If the default value shall be used for this mail access, return <code>-1</code>.
     * 
     * @return The number of allowed idle seconds or <code>-1</code> to signal using default value.
     */
    public int getCacheIdleSeconds() {
        return -1;
    }

    /**
     * Indicates if this mail access is cacheable.
     * 
     * @return <code>true</code> if this mail access is cacheable; otherwise <code>false</code>
     */
    public boolean isCacheable() {
        return cacheable;
    }

    /**
     * Sets whether this mail access is cacheable or not.
     * 
     * @param cacheable <code>true</code> if this mail access is cacheable; otherwise <code>false</code>
     */
    public void setCacheable(final boolean cacheable) {
        this.cacheable = cacheable;
    }

    /**
     * Indicates if this mail access is currently cached in {@link IMailAccessCache}.
     * 
     * @return <code>true</code> if this mail access is cached; otherwise <code>false</code>
     */
    public boolean isCached() {
        return cached;
    }

    /**
     * Sets whether this mail access is currently cached or not.
     * 
     * @param cacheable <code>true</code> if this mail access is cached; otherwise <code>false</code>
     */
    public void setCached(final boolean cached) {
        this.cached = cached;
    }

    /**
     * Indicates if this mail access is currently waiting for any server notifications (idle mode).
     * 
     * @return <code>true</code> if this mail access is waiting; otherwise <code>false</code>
     */
    public boolean isWaiting() {
        return waiting;
    }

    /**
     * Sets whether this mail access is currently waiting for any mail server notifications or not.
     * 
     * @param cacheable <code>true</code> if this mail access is waiting; otherwise <code>false</code>
     */
    public void setWaiting(final boolean waiting) {
        this.waiting = waiting;
    }

    /**
     * Gets an implementation-specific new instance of {@link MailConfig}.
     * 
     * @return An implementation-specific new instance of {@link MailConfig}
     */
    protected abstract MailConfig createNewMailConfig();

    /**
     * For delegating purpose. Don't invoke.
     */
    public MailConfig delegateCreateNewMailConfig() {
        return createNewMailConfig();
    }

    /**
     * Gets an implementation-specific new instance of {@link IMailProperties}.
     * 
     * @return An implementation-specific new instance of {@link IMailProperties}
     * @throws OXException If creating a new instance of {@link IMailProperties} fails
     */
    protected abstract IMailProperties createNewMailProperties() throws OXException;

    /**
     * For delegating purpose. Don't invoke.
     */
    public IMailProperties delegateCreateNewMailProperties() throws OXException {
        return createNewMailProperties();
    }

    /**
     * Defines if mail server port has to be present in provided mail configuration before establishing any connection.
     * 
     * @return <code>true</code> if mail server port has to be set before establishing any connection; otherwise <code>false</code>
     */
    protected abstract boolean checkMailServerPort();

    /**
     * For delegating purpose. Don't invoke.
     */
    public boolean delegateCheckMailServerPort() {
        return checkMailServerPort();
    }

    /**
     * Releases all used resources prior to caching or closing a connection.
     */
    protected abstract void releaseResources();

    /**
     * For delegating purpose. Don't invoke.
     */
    public void delegateReleaseResources() {
        releaseResources();
    }

    /**
     * Internal close method to drop a mail connection.
     */
    protected abstract void closeInternal();

    /**
     * For delegating purpose. Don't invoke.
     */
    public void delegateCloseInternal() {
        closeInternal();
    }

    /**
     * Gets the appropriate {@link IMailFolderStorage} implementation that is considered as the main entry point to a user's mailbox.
     * 
     * @return The appropriate {@link IMailFolderStorage} implementation
     * @throws OXException If connection is not established
     */
    public abstract F getFolderStorage() throws OXException;

    /**
     * Gets the appropriate {@link IMailMessageStorage} implementation that provides necessary message-related operations/methods.
     * 
     * @return The appropriate {@link IMailMessageStorage} implementation
     * @throws OXException If connection is not established
     */
    public abstract M getMessageStorage() throws OXException;

    /**
     * Gets the appropriate {@link MailLogicTools} implementation that provides operations/methods to create a reply/forward message from a
     * referenced message.
     * 
     * @return The appropriate {@link MailLogicTools} implementation
     * @throws OXException If connection is not established
     */
    public abstract MailLogicTools getLogicTools() throws OXException;

    /**
     * Checks if this connection is currently connected.
     * 
     * @return <code>true</code> if connected; otherwise <code>false</code>
     * @see #isConnectedUnsafe()
     */
    public abstract boolean isConnected();

    /**
     * Checks if this connection is currently connected in an unsafe, but faster manner than {@link #isConnected()}.
     * 
     * @return <code>true</code> if connected; otherwise <code>false</code>
     * @see #isConnected()
     */
    public abstract boolean isConnectedUnsafe();

    /**
     * Triggers all necessary startup actions.
     * 
     * @throws OXException If startup actions fail
     */
    protected abstract void startup() throws OXException;

    /**
     * Triggers all necessary shutdown actions.
     * 
     * @throws OXException If shutdown actions fail
     */
    protected abstract void shutdown() throws OXException;

}
