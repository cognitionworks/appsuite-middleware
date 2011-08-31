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

package com.openexchange.imap.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import javax.mail.MessagingException;
import com.openexchange.exception.OXException;
import com.openexchange.imap.AccessedIMAPStore;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.mime.MIMEMailException;
import com.openexchange.session.Session;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;

/**
 * {@link ListLsubCache} - A user-bound cache for LIST/LSUB entries.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ListLsubCache {

    /**
     * The logger.
     */
    protected static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ListLsubCache.Key.class);

    private static final class Key {

        private final int cid;

        private final int user;

        private final int hash;

        public Key(final int user, final int cid) {
            super();
            this.user = user;
            this.cid = cid;
            final int prime = 31;
            int result = 1;
            result = prime * result + cid;
            result = prime * result + user;
            hash = result;
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
            if (!(obj instanceof Key)) {
                return false;
            }
            final Key other = (Key) obj;
            if (cid != other.cid) {
                return false;
            }
            if (user != other.user) {
                return false;
            }
            return true;
        }

    } // End of class Key

    private static Key keyFor(final Session session) {
        return new Key(session.getUserId(), session.getContextId());
    }

    private static final long TIMEOUT = 300000;

    private static final String INBOX = "INBOX";

    private static final boolean DO_STATUS = false;

    private static final boolean DO_GETACL = true;

    private static final ConcurrentMap<Key, ConcurrentMap<Integer, Future<ListLsubCollection>>> MAP = new ConcurrentHashMap<Key, ConcurrentMap<Integer, Future<ListLsubCollection>>>();

    /**
     * No instance
     */
    private ListLsubCache() {
        super();
    }

    /**
     * Drop caches for given session's user.
     *
     * @param session The session providing user information
     */
    public static void dropFor(final Session session) {
        MAP.remove(keyFor(session));
        if (LOG.isDebugEnabled()) {
            LOG.debug(new StringBuilder("Cleaned user-sensitive LIST/LSUB cache for user ").append(session.getUserId()).append(
                " in context ").append(session.getContextId()).toString());
        }
    }

    /**
     * Removes cached LIST/LSUB entry.
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param session The session
     */
    public static void removeCachedEntry(final String fullName, final int accountId, final Session session) {
        final ConcurrentMap<Integer, Future<ListLsubCollection>> map = MAP.get(keyFor(session));
        if (null == map) {
            return;
        }
        final ListLsubCollection collection = getSafeFrom(map.get(Integer.valueOf(accountId)));
        if (null != collection) {
            synchronized (collection) {
                collection.remove(fullName);
            }
        }
    }

    /**
     * Checks if associated mailbox is considered as MBox format.
     *
     * @param accountId The account ID
     * @param imapFolder The IMAP folder
     * @param session The session
     * @return {@link Boolean#TRUE} for MBox format, {@link Boolean#FALSE} for no MBOX format or <code>null</code> if undetermined
     * @throws OXException if a mail error occurs
     */
    public static Boolean consideredAsMBox(final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException {
        final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        synchronized (collection) {
            checkTimeStamp(imapFolder, collection);
            return collection.consideredAsMBox();
        }
    }

    /**
     * Clears the cache.
     *
     * @param accountId The account ID
     * @param session The session
     */
    public static void clearCache(final int accountId, final Session session) {
        final ConcurrentMap<Integer, Future<ListLsubCollection>> map = MAP.get(keyFor(session));
        if (null == map) {
            return;
        }
        final ListLsubCollection collection = getSafeFrom(map.get(Integer.valueOf(accountId)));
        if (null != collection) {
            synchronized (collection) {
                collection.clear();
            }
        }
    }

    /**
     * Adds single entry to cache. Replaces any existing entry.
     *
     * @param fullName The entry's full name
     * @param accountId The account ID
     * @param imapFolder The IMAP folder providing connected protocol
     * @param session The session
     * @throws OXException If entry could not be added
     */
    public static void addSingle(final String fullName, final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException {
        final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection)) {
                return;
            }
            collection.addSingle(fullName, imapFolder, DO_STATUS, DO_GETACL);
        }
    }

    /**
     * Gets the separator character.
     *
     * @param accountId The account ID
     * @param imapStore The connected IMAP store instance
     * @param session The session
     * @return The separator
     * @throws OXException If a mail error occurs
     */
    public static char getSeparator(final int accountId, final AccessedIMAPStore imapStore, final Session session) throws OXException {
        try {
            return getSeparator(accountId, (IMAPFolder) imapStore.getFolder(INBOX), session);
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e);
        }
    }

    /**
     * Gets the separator character.
     *
     * @param accountId The account ID
     * @param imapFolder An IMAP folder
     * @param session The session
     * @return The separator
     * @throws OXException If a mail error occurs
     */
    public static char getSeparator(final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException {
        return getCachedLISTEntry(INBOX, accountId, imapFolder, session).getSeparator();
    }

    /**
     * Gets cached LSUB entry for specified full name.
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param imapFolder The IMAP
     * @param session The session
     * @return The cached LSUB entry
     * @throws OXException If loading the entry fails
     */
    public static ListLsubEntry getCachedLSUBEntry(final String fullName, final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException {
        final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection)) {
                final ListLsubEntry entry = collection.getLsub(fullName);
                return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
            }
            /*
             * Return
             */
            ListLsubEntry entry = collection.getLsub(fullName);
            if (null != entry && (entry.canOpen() || entry.isNamespace())) {
                return entry;
            }
            /*
             * Update & re-check
             */
            collection.update(fullName, imapFolder, DO_STATUS, DO_GETACL);
            entry = collection.getLsub(fullName);
            return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
        }
    }

    /**
     * Gets cached LIST entry for specified full name.
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param imapStore The IMAP store
     * @param session The session
     * @return The cached LIST entry
     * @throws OXException If loading the entry fails
     */
    public static ListLsubEntry getCachedLISTEntry(final String fullName, final int accountId, final AccessedIMAPStore imapStore, final Session session) throws OXException {
        try {
            final IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(INBOX);
            final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
            synchronized (collection) {
                if (checkTimeStamp(imapFolder, collection)) {
                    final ListLsubEntry entry = collection.getList(fullName);
                    return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
                }
                /*
                 * Return
                 */
                ListLsubEntry entry = collection.getList(fullName);
                if (null != entry && (entry.canOpen() || entry.isNamespace())) {
                    return entry;
                }
                /*
                 * Update & re-check
                 */
                collection.update(fullName, imapFolder, DO_STATUS, DO_GETACL);
                entry = collection.getList(fullName);
                return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
            }
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e);
        }
    }

    /**
     * Gets up-to-date LIST entry for specified full name.
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param imapStore The IMAP store
     * @param session The session
     * @return The cached LIST entry
     * @throws MailException If loading the entry fails
     */
    public static ListLsubEntry getActualLISTEntry(final String fullName, final int accountId, final AccessedIMAPStore imapStore, final Session session) throws OXException {
        try {
            final IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(INBOX);
            final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
            synchronized (collection) {
                return collection.getActualEntry(fullName, imapFolder);
            }
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e);
        }
    }

    /**
     * Gets cached LIST entry for specified full name.
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param imapFolder The IMAP
     * @param session The session
     * @return The cached LIST entry
     * @throws OXException If loading the entry fails
     */
    public static ListLsubEntry getCachedLISTEntry(final String fullName, final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException {
        final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection)) {
                final ListLsubEntry entry = collection.getList(fullName);
                return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
            }
            /*
             * Return
             */
            ListLsubEntry entry = collection.getList(fullName);
            if (null != entry && (entry.canOpen() || entry.isNamespace() || entry.hasChildren())) {
                return entry;
            }
            /*
             * Update & re-check
             */
            collection.update(fullName, imapFolder, DO_STATUS, DO_GETACL);
            entry = collection.getList(fullName);
            return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
        }
    }

    private static boolean checkTimeStamp(final IMAPFolder imapFolder, final ListLsubCollection collection) throws OXException {
        /*
         * Check collection's stamp
         */
        if (collection.isDeprecated() || ((System.currentTimeMillis() - collection.getStamp()) > TIMEOUT)) {
            collection.reinit(imapFolder, DO_STATUS, DO_GETACL);
            return true;
        }
        return false;
    }

    /**
     * Gets cached LIST/LSUB entry for specified full name.
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param imapFolder The IMAP
     * @param session The session
     * @return The cached LIST/LSUB entry
     * @throws OXException If loading the entry fails
     */
    public static ListLsubEntry[] getCachedEntries(final String fullName, final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException {
        final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection)) {
                final ListLsubEntry listEntry = collection.getLsub(fullName);
                final ListLsubEntry lsubEntry = collection.getLsub(fullName);
                final ListLsubEntry emptyEntryFor = ListLsubCollection.emptyEntryFor(fullName);
                return new ListLsubEntry[] { listEntry == null ? emptyEntryFor : listEntry, lsubEntry == null ? emptyEntryFor : lsubEntry };
            }
            /*
             * Return
             */
            ListLsubEntry listEntry = collection.getList(fullName);
            if (null == listEntry || (!listEntry.canOpen() && !listEntry.isNamespace())) {
                /*
                 * Update & re-check
                 */
                collection.update(fullName, imapFolder, DO_STATUS, DO_GETACL);
                listEntry = collection.getList(fullName);
            }
            final ListLsubEntry lsubEntry = collection.getLsub(fullName);
            final ListLsubEntry emptyEntryFor = ListLsubCollection.emptyEntryFor(fullName);
            return new ListLsubEntry[] { listEntry == null ? emptyEntryFor : listEntry, lsubEntry == null ? emptyEntryFor : lsubEntry };
        }
    }

    private static ListLsubCollection getCollection(final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException {
        /*
         * Initialize appropriate cache entry
         */
        final Key key = keyFor(session);
        ConcurrentMap<Integer, Future<ListLsubCollection>> map = MAP.get(key);
        if (null == map) {
            final ConcurrentMap<Integer, Future<ListLsubCollection>> newmap = new ConcurrentHashMap<Integer, Future<ListLsubCollection>>();
            map = MAP.putIfAbsent(key, newmap);
            if (null == map) {
                map = newmap;
            }
        }
        Future<ListLsubCollection> f = map.get(Integer.valueOf(accountId));
        boolean caller = false;
        if (null == f) {
            final FutureTask<ListLsubCollection> ft = new FutureTask<ListLsubCollection>(new Callable<ListLsubCollection>() {

                @Override
                public ListLsubCollection call() throws OXException {
                    String[] shared;
                    String[] user;
                    try {
                        final IMAPStore imapStore = (IMAPStore) imapFolder.getStore();
                        try {
                            shared = check(NamespaceFoldersCache.getSharedNamespaces(imapStore, true, session, accountId));
                        } catch (final MessagingException e) {
                            if (imapStore.hasCapability("NAMESPACE")) {
                                LOG.warn("Couldn't get shared namespaces.", e);
                            } else {
                                LOG.debug("Couldn't get shared namespaces.", e);
                            }
                            shared = new String[0];
                        } catch (final RuntimeException e) {
                            LOG.warn("Couldn't get shared namespaces.", e);
                            shared = new String[0];
                        }
                        try {
                            user = check(NamespaceFoldersCache.getUserNamespaces(imapStore, true, session, accountId));
                        } catch (final MessagingException e) {
                            if (imapStore.hasCapability("NAMESPACE")) {
                                LOG.warn("Couldn't get user namespaces.", e);
                            } else {
                                LOG.debug("Couldn't get user namespaces.", e);
                            }
                            user = new String[0];
                        } catch (final RuntimeException e) {
                            LOG.warn("Couldn't get user namespaces.", e);
                            user = new String[0];
                        }
                    } catch (final MessagingException e) {
                        throw MIMEMailException.handleMessagingException(e);
                    }
                    return new ListLsubCollection(imapFolder, shared, user, DO_STATUS, DO_GETACL);
                }
            });
            f = map.putIfAbsent(Integer.valueOf(accountId), ft);
            if (null == f) {
                f = ft;
                ft.run();
                caller = true;
            }
        }
        try {
            return getFrom(f);
        } catch (final OXException e) {
            if (caller) {
                MAP.remove(key);
            }
            throw e;
        }
    }

    static String[] check(final String[] array) {
        final List<String> list = new ArrayList<String>(array.length);
        for (int i = 0; i < array.length; i++) {
            final String s = array[i];
            if (!isEmpty(s)) {
                list.add(s);
            }
        }
        return list.toArray(new String[list.size()]);
    }

    private static boolean isEmpty(final String s) {
        if (null == s) {
            return true;
        }
        final char[] chars = s.toCharArray();
        boolean whitespace = true;
        for (int i = 0; whitespace && i < chars.length; i++) {
            whitespace = Character.isWhitespace(chars[i]);
        }
        return whitespace;
    }

    /**
     * Checks for any subscribed subfolder.
     *
     * @param fullName The full name
     * @return <code>true</code> if a subscribed subfolder exists; otherwise <code>false</code>
     * @throws OXException If a mail error occurs
     */
    public static boolean hasAnySubscribedSubfolder(final String fullName, final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException {
        final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        synchronized (collection) {
            checkTimeStamp(imapFolder, collection);
            return collection.hasAnySubscribedSubfolder(fullName);
        }
    }

    private static ListLsubCollection getFrom(final Future<ListLsubCollection> future) throws OXException {
        if (null == future) {
            return null;
        }
        try {
            return future.get();
        } catch (final InterruptedException e) {
            // Cannot occur
            throw new IllegalStateException(e);
        } catch (final ExecutionException e) {
            final Throwable t = e.getCause();
            if (t instanceof OXException) {
                throw (OXException) t;
            }
            if (t instanceof RuntimeException) {
                throw MailExceptionCode.UNEXPECTED_ERROR.create(t, t.getMessage());
            }
            if (t instanceof Error) {
                throw (Error) t;
            }
            throw MailExceptionCode.UNEXPECTED_ERROR.create(t, t.getMessage());
        }
    }

    private static ListLsubCollection getSafeFrom(final Future<ListLsubCollection> future) {
        if (null == future) {
            return null;
        }
        try {
            return future.get();
        } catch (final InterruptedException e) {
            // Cannot occur
            throw new IllegalStateException(e);
        } catch (final ExecutionException e) {
            final Throwable t = e.getCause();
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                throw new IllegalStateException("Not unchecked", t);
            }
        }
    }

}
