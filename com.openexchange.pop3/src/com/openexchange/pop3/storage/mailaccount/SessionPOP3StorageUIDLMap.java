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

package com.openexchange.pop3.storage.mailaccount;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import com.openexchange.mail.MailException;
import com.openexchange.pop3.POP3Access;
import com.openexchange.pop3.services.POP3ServiceRegistry;
import com.openexchange.pop3.storage.FullnameUIDPair;
import com.openexchange.pop3.storage.POP3StorageUIDLMap;
import com.openexchange.session.Session;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;

/**
 * {@link SessionPOP3StorageUIDLMap} - Session-backed implementation of {@link POP3StorageUIDLMap}.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SessionPOP3StorageUIDLMap implements POP3StorageUIDLMap {

    /**
     * Gets the UIDL map bound to specified POP3 access.
     * 
     * @param pop3Access The POP3 access
     * @return The UIDL map bound to specified POP3 access
     * @throws MailException If instance cannot be returned
     */
    public static SessionPOP3StorageUIDLMap getInstance(final POP3Access pop3Access) throws MailException {
        final Session session = pop3Access.getSession();
        final String key = SessionParameterNames.getUIDLMap(pop3Access.getAccountId());
        SessionPOP3StorageUIDLMap cached;
        final Lock lock = (Lock) session.getParameter(Session.PARAM_LOCK);
        if (null != lock) {
            lock.lock();
            try {
                try {
                    cached = (SessionPOP3StorageUIDLMap) session.getParameter(key);
                } catch (final ClassCastException e) {
                    cached = null;
                }
                if (null == cached) {
                    cached = new SessionPOP3StorageUIDLMap(new RdbPOP3StorageUIDLMap(pop3Access), session, key);
                    session.setParameter(key, cached);
                }
            } finally {
                lock.unlock();
            }
        } else {
            synchronized (session) {
                try {
                    cached = (SessionPOP3StorageUIDLMap) session.getParameter(key);
                } catch (final ClassCastException e) {
                    cached = null;
                }
                if (null == cached) {
                    cached = new SessionPOP3StorageUIDLMap(new RdbPOP3StorageUIDLMap(pop3Access), session, key);
                    session.setParameter(key, cached);
                }
            }
        }
        return cached;
    }

    /*-
     * Member section
     */

    private final Map<String, FullnameUIDPair> uidl2pair;

    private final Map<FullnameUIDPair, String> pair2uidl;

    private final POP3StorageUIDLMap delegatee;

    private final ReadWriteLock rwLock;

    private final int[] mode;

    private SessionPOP3StorageUIDLMap(final POP3StorageUIDLMap delegatee, final Session session, final String key) throws MailException {
        super();
        rwLock = new ReentrantReadWriteLock();
        this.delegatee = delegatee;
        pair2uidl = new ConcurrentHashMap<FullnameUIDPair, String>();
        uidl2pair = new ConcurrentHashMap<String, FullnameUIDPair>();
        mode = new int[] { 1 };
        final ClearMapsRunnable cmr = new ClearMapsRunnable(session, key, uidl2pair, pair2uidl, rwLock, mode);
        final ScheduledTimerTask timerTask =
            POP3ServiceRegistry.getServiceRegistry().getService(TimerService.class).scheduleWithFixedDelay(
                cmr,
                SessionCacheProperties.SCHEDULED_TASK_INITIAL_DELAY,
                SessionCacheProperties.SCHEDULED_TASK_DELAY);
        cmr.setTimerTask(timerTask);
        init();
    }

    private void init() throws MailException {
        final Map<String, FullnameUIDPair> all = delegatee.getAllUIDLs();
        final int size = all.size();
        final Iterator<Entry<String, FullnameUIDPair>> iter = all.entrySet().iterator();
        for (int i = 0; i < size; i++) {
            final Entry<String, FullnameUIDPair> entry = iter.next();
            pair2uidl.put(entry.getValue(), entry.getKey());
            uidl2pair.put(entry.getKey(), entry.getValue());
        }
        mode[0] = 0;
    }

    private void checkInit(final Lock obtainedReadLock) throws MailException {
        final int m = mode[0];
        if (-1 == m) {
            throw new MailException(MailException.Code.UNEXPECTED_ERROR, "Error mode. Try again.");
        }
        if (1 == m) {
            /*
             * Upgrade lock: unlock first to acquire write lock
             */
            obtainedReadLock.unlock();
            final Lock writeLock = rwLock.writeLock();
            writeLock.lock();
            try {
                init();
            } finally {
                /*
                 * Downgrade lock: reacquire read without giving up write lock and...
                 */
                obtainedReadLock.lock();
                /*
                 * ... unlock write.
                 */
                writeLock.unlock();
            }
        }
    }

    public void addMappings(final String[] uidls, final FullnameUIDPair[] fullnameUIDPairs) throws MailException {
        final Lock readLock = rwLock.readLock();
        readLock.lock();
        try {
            checkInit(readLock);
            delegatee.addMappings(uidls, fullnameUIDPairs);
            for (int i = 0; i < fullnameUIDPairs.length; i++) {
                final String uidl = uidls[i];
                if (null != uidl) {
                    final FullnameUIDPair pair = fullnameUIDPairs[i];
                    pair2uidl.put(pair, uidl);
                    uidl2pair.put(uidl, pair);
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    public FullnameUIDPair getFullnameUIDPair(final String uidl) throws MailException {
        final Lock readLock = rwLock.readLock();
        readLock.lock();
        try {
            checkInit(readLock);
            return uidl2pair.get(uidl);
        } finally {
            readLock.unlock();
        }
    }

    public FullnameUIDPair[] getFullnameUIDPairs(final String[] uidls) throws MailException {
        final Lock readLock = rwLock.readLock();
        readLock.lock();
        try {
            checkInit(readLock);
            final FullnameUIDPair[] pairs = new FullnameUIDPair[uidls.length];
            for (int i = 0; i < pairs.length; i++) {
                pairs[i] = getFullnameUIDPair(uidls[i]);
            }
            return pairs;
        } finally {
            readLock.unlock();
        }
    }

    public String getUIDL(final FullnameUIDPair fullnameUIDPair) throws MailException {
        final Lock readLock = rwLock.readLock();
        readLock.lock();
        try {
            checkInit(readLock);
            return pair2uidl.get(fullnameUIDPair);
        } finally {
            readLock.unlock();
        }
    }

    public String[] getUIDLs(final FullnameUIDPair[] fullnameUIDPairs) throws MailException {
        final Lock readLock = rwLock.readLock();
        readLock.lock();
        try {
            checkInit(readLock);
            final String[] uidls = new String[fullnameUIDPairs.length];
            for (int i = 0; i < uidls.length; i++) {
                uidls[i] = getUIDL(fullnameUIDPairs[i]);
            }
            return uidls;
        } finally {
            readLock.unlock();
        }
    }

    public Map<String, FullnameUIDPair> getAllUIDLs() throws MailException {
        final Lock readLock = rwLock.readLock();
        readLock.lock();
        try {
            checkInit(readLock);
            final Map<String, FullnameUIDPair> copy = new HashMap<String, FullnameUIDPair>();
            copy.putAll(uidl2pair);
            return copy;
        } finally {
            readLock.unlock();
        }
    }

    public void deleteFullnameUIDPairMappings(final FullnameUIDPair[] fullnameUIDPairs) throws MailException {
        final Lock readLock = rwLock.readLock();
        readLock.lock();
        try {
            checkInit(readLock);
            delegatee.deleteFullnameUIDPairMappings(fullnameUIDPairs);
            for (int i = 0; i < fullnameUIDPairs.length; i++) {
                final String uidl = pair2uidl.remove(fullnameUIDPairs[i]);
                if (null != uidl) {
                    uidl2pair.remove(uidl);
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    public void deleteUIDLMappings(final String[] uidls) throws MailException {
        final Lock readLock = rwLock.readLock();
        readLock.lock();
        try {
            checkInit(readLock);
            delegatee.deleteUIDLMappings(uidls);
            for (int i = 0; i < uidls.length; i++) {
                final FullnameUIDPair pair = uidl2pair.remove(uidls[i]);
                if (null != pair) {
                    pair2uidl.remove(pair);
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    private static final class ClearMapsRunnable implements Runnable {

        private final Session tsession;

        private final String tkey;

        private final Map<String, FullnameUIDPair> tuidl2pair;

        private final Map<FullnameUIDPair, String> tpair2uidl;

        private final ReadWriteLock trwLock;

        private final int[] tmode;

        private ScheduledTimerTask timerTask;

        private int countEmptyRuns;

        public ClearMapsRunnable(final Session tsession, final String tkey, final Map<String, FullnameUIDPair> tuidl2pair, final Map<FullnameUIDPair, String> tpair2uidl, final ReadWriteLock trwLock, final int[] tmode) {
            super();
            this.tsession = tsession;
            this.tkey = tkey;
            this.trwLock = trwLock;
            this.tuidl2pair = tuidl2pair;
            this.tpair2uidl = tpair2uidl;
            this.tmode = tmode;
        }

        public void run() {
            final Lock writeLock = trwLock.writeLock();
            writeLock.lock();
            try {
                if (tuidl2pair.isEmpty() && tpair2uidl.isEmpty()) {
                    if (countEmptyRuns >= SessionCacheProperties.SCHEDULED_TASK_ALLOWED_EMPTY_RUNS) {
                        // Destroy!
                        timerTask.cancel();
                        synchronized (tsession) {
                            tsession.setParameter(tkey, null);
                            tmode[0] = -1;
                        }
                    }
                    countEmptyRuns++;
                    return;
                }
                countEmptyRuns = 0;
                tuidl2pair.clear();
                tpair2uidl.clear();
                tmode[0] = 1;
            } finally {
                writeLock.unlock();
            }
        }

        public void setTimerTask(final ScheduledTimerTask timerTask) {
            this.timerTask = timerTask;
        }

    }

}
