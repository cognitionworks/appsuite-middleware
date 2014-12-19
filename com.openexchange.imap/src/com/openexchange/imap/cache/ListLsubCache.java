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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import javax.mail.MessagingException;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.slf4j.Logger;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheKey;
import com.openexchange.caching.CacheService;
import com.openexchange.exception.OXException;
import com.openexchange.imap.config.IMAPProperties;
import com.openexchange.imap.services.Services;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.server.ServiceExceptionCode;
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
     * The logger
     */
    protected static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ListLsubCache.class);

    private static final class KeyedCache {

        private final Cache cache;
        private final CacheKey key;

        KeyedCache(Cache cache, CacheKey key) {
            super();
            this.cache = cache;
            this.key = key;
        }

        void clear() throws OXException {
            cache.clear();
        }

        /**
         * Gets the associated object from cache
         *
         * @return The object or <code>null</code>
         */
        <V> V get() {
            return (V) cache.get(key);
        }

        /**
         * Safely puts given object into cache
         *
         * @param value The object to put
         * @return The object previously in cache or <code>null</code>
         * @throws OXException If there is already such an entry in cache
         */
        <V> V putIfAbsent(Serializable value) {
            try {
                cache.putSafe(key, value);
                return null;
            } catch (OXException e) {
                return (V) cache.get(key);
            }
        }

        /**
         * Removes the object from cache
         *
         * @throws OXException If removal fails
         */
        void remove() throws OXException {
            cache.remove(key);
        }

    } // End of class KeyedCache

    /**
     * The region name.
     */
    public static final String REGION = "ListLsubCache";

    private static KeyedCache optCache(Session session) {
        return optCache(session.getUserId(), session.getContextId());
    }

    private static KeyedCache optCache(int userId, int contextId) {
        try {
            CacheService cacheService = Services.optService(CacheService.class);
            if (null == cacheService) {
                return null;
            }

            Cache cache = cacheService.getCache(REGION);
            return new KeyedCache(cache, cacheService.newCacheKey(contextId, userId));
        } catch (Exception e) {
            LOG.error("Could not return cache for {}", REGION, e);
            return null;
        }
    }

    /** The default timeout for LIST/LSUB cache (5 minutes) */
    private static final long DEFAULT_TIMEOUT = 300000;

    private static final String INBOX = "INBOX";

    private static final boolean DO_STATUS = false;

    private static final boolean DO_GETACL = false;

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
        if (null != session) {
            dropFor(session.getUserId(), session.getContextId());
        }
    }

    /**
     * Drop caches for given user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    public static void dropFor(final int userId, final int contextId) {

        KeyedCache cache = optCache(userId, contextId);
        if (null != cache) {
            try {
                cache.remove();
                LOG.debug("Cleaned user-sensitive LIST/LSUB cache for user {} in context {}", userId, contextId);
            } catch (OXException e) {
                LOG.error("Could not remove entry from cache {} for user {} in context {}", REGION, userId, contextId, e);
            }
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
        KeyedCache cache = optCache(session);
        if (null != cache) {
            Object object = cache.get();
            if (object instanceof ConcurrentMap) {
                @SuppressWarnings("unchecked")
                ConcurrentMap<Integer, Future<ListLsubCollection>> map = (ConcurrentMap<Integer, Future<ListLsubCollection>>) object;
                ListLsubCollection collection = getSafeFrom(map.get(Integer.valueOf(accountId)));
                if (null != collection) {
                    synchronized (collection) {
                        collection.remove(fullName);
                    }
                }
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
     * @throws MessagingException If a messaging error occurs
     */
    public static Boolean consideredAsMBox(final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException, MessagingException {
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
        clearCache(accountId, session.getUserId(), session.getContextId());
    }

    /**
     * Clears the cache.
     *
     * @param accountId The account ID
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    public static void clearCache(final int accountId, final int userId, final int contextId) {
        KeyedCache cache = optCache(userId, contextId);
        if (null != cache) {
            Object object = cache.get();
            if (object instanceof ConcurrentMap) {
                @SuppressWarnings("unchecked")
                ConcurrentMap<Integer, Future<ListLsubCollection>> map = (ConcurrentMap<Integer, Future<ListLsubCollection>>) object;
                ListLsubCollection collection = getSafeFrom(map.get(Integer.valueOf(accountId)));
                if (null != collection) {
                    synchronized (collection) {
                        collection.clear();
                    }
                }
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
     * @throws MessagingException If a messaging error occurs
     */
    public static void addSingle(final String fullName, final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException, MessagingException {
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
    public static char getSeparator(final int accountId, final IMAPStore imapStore, final Session session) throws OXException {
        try {
            return getSeparator(accountId, (IMAPFolder) imapStore.getFolder(INBOX), session);
        } catch (final MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
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
     * @throws MessagingException If a messaging error occurs
     */
    public static char getSeparator(final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException, MessagingException {
        return getCachedLISTEntry(INBOX, accountId, imapFolder, session).getSeparator();
    }

    private static boolean seemsValid(final ListLsubEntry entry) {
        return (null != entry) && (entry.canOpen() || entry.isNamespace() || entry.hasChildren());
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
     * @throws MessagingException If a messaging error occurs
     */
    public static ListLsubEntry getCachedLSUBEntry(final String fullName, final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException, MessagingException {
        final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        if (isAccessible(collection)) {
            final ListLsubEntry entry = collection.getLsub(fullName);
            if (seemsValid(entry)) {
                return entry;
            }
        }
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection)) {
                final ListLsubEntry entry = collection.getLsub(fullName);
                return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
            }
            /*
             * Return
             */
            ListLsubEntry entry = collection.getLsub(fullName);
            if (seemsValid(entry)) {
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
    public static ListLsubEntry getCachedLISTEntry(final String fullName, final int accountId, final IMAPStore imapStore, final Session session) throws OXException {
        try {
            final IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(INBOX);
            final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
            if (isAccessible(collection)) {
                final ListLsubEntry entry = collection.getList(fullName);
                if (seemsValid(entry)) {
                    return entry;
                }
            }
            synchronized (collection) {
                if (checkTimeStamp(imapFolder, collection)) {
                    final ListLsubEntry entry = collection.getList(fullName);
                    return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
                }
                /*
                 * Return
                 */
                ListLsubEntry entry = collection.getList(fullName);
                if (seemsValid(entry)) {
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
            throw MimeMailException.handleMessagingException(e);
        }
    }

    /**
     * Initializes ACL list
     *
     * @param accountId The account identifier
     * @param imapStore The IMAP store
     * @param session The session
     * @throws OXException If initialization fails
     */
    public static void initACLs(final int accountId, final IMAPStore imapStore, final Session session) throws OXException {
        if (DO_GETACL) {
            // Already perform during initialization
            return;
        }
        try {
            final IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(INBOX);
            final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
            synchronized (collection) {
                collection.initACLs(imapFolder);
            }
        } catch (final MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
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
    public static ListLsubEntry getActualLISTEntry(final String fullName, final int accountId, final IMAPStore imapStore, final Session session) throws OXException {
        try {
            final IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(INBOX);
            final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
            synchronized (collection) {
                return collection.getActualEntry(fullName, imapFolder);
            }
        } catch (final MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
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
     * @throws MessagingException If a messaging error occurs
     */
    public static ListLsubEntry getCachedLISTEntry(final String fullName, final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException, MessagingException {
        final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        if (isAccessible(collection)) {
            final ListLsubEntry entry = collection.getList(fullName);
            if (seemsValid(entry)) {
                return entry;
            }
        }
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection)) {
                final ListLsubEntry entry = collection.getList(fullName);
                return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
            }
            /*
             * Return
             */
            ListLsubEntry entry = collection.getList(fullName);
            if (seemsValid(entry)) {
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

    private static boolean checkTimeStamp(final IMAPFolder imapFolder, final ListLsubCollection collection) throws MessagingException {
        /*
         * Check collection's stamp
         */
        if (collection.isDeprecated() || ((System.currentTimeMillis() - collection.getStamp()) > getTimeout())) {
            collection.reinit(imapFolder, DO_STATUS, DO_GETACL);
            return true;
        }
        return false;
    }

    private static boolean isAccessible(final ListLsubCollection collection) {
        return !collection.isDeprecated() && ((System.currentTimeMillis() - collection.getStamp()) <= getTimeout());
    }

    private static long getTimeout() {
        return IMAPProperties.getInstance().allowFolderCaches() ? DEFAULT_TIMEOUT : 20000L;
    }

    /**
     * Gets all LIST/LSUB entries.
     *
     * @param optParentFullName The optional full name of the parent
     * @param accountId The account identifier
     * @param subscribedOnly <code>false</code> for LIST entries; otherwise <code>true</code> for LSUB ones
     * @param imapStore The IMAP store
     * @param session The session
     * @return All LSUB entries
     * @throws OXException If loading the entry fails
     * @throws MessagingException If a messaging error occurs
     */
    public static List<ListLsubEntry> getAllEntries(String optParentFullName, int accountId, boolean subscribedOnly, IMAPStore imapStore, Session session) throws OXException, MessagingException {
        IMAPFolder imapFolder = (IMAPFolder) imapStore.getDefaultFolder();
        ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        if (isAccessible(collection)) {
            if (null == optParentFullName) {
                return subscribedOnly ? collection.getLsubs() : collection.getLists();
            }

            ListLsubEntry entry = subscribedOnly ? collection.getLsub(optParentFullName) : collection.getList(optParentFullName);
            if (null != entry) {
                return entry.getChildren();
            }
        }
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection)) {
                if (null == optParentFullName) {
                    return subscribedOnly ? collection.getLsubs() : collection.getLists();
                }

                ListLsubEntry entry = subscribedOnly ? collection.getLsub(optParentFullName) : collection.getList(optParentFullName);
                if (null != entry) {
                    return entry.getChildren();
                }
            }
            /*
             * Update & re-check
             */
            collection.reinit(imapStore, DO_STATUS, DO_GETACL);
            if (null == optParentFullName) {
                return subscribedOnly ? collection.getLsubs() : collection.getLists();
            }

            ListLsubEntry entry = subscribedOnly ? collection.getLsub(optParentFullName) : collection.getList(optParentFullName);
            if (null != entry) {
                return entry.getChildren();
            }
            return Collections.emptyList();
        }
    }

    /**
     * Gets cached LIST/LSUB entry for specified full name.
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param imapFolder The IMAP folder
     * @param session The session
     * @return The cached LIST/LSUB entry
     * @throws OXException If loading the entry fails
     * @throws MessagingException If a messaging error occurs
     */
    public static ListLsubEntry[] getCachedEntries(String fullName, int accountId, IMAPFolder imapFolder, Session session) throws OXException, MessagingException {
        final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        if (isAccessible(collection)) {
            final ListLsubEntry listEntry = collection.getList(fullName);
            if (seemsValid(listEntry)) {
                final ListLsubEntry lsubEntry = collection.getLsub(fullName);
                final ListLsubEntry emptyEntryFor = ListLsubCollection.emptyEntryFor(fullName);
                return new ListLsubEntry[] { listEntry, lsubEntry == null ? emptyEntryFor : lsubEntry };
            }
        }
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection)) {
                final ListLsubEntry listEntry = collection.getList(fullName);
                final ListLsubEntry lsubEntry = collection.getLsub(fullName);
                final ListLsubEntry emptyEntryFor = ListLsubCollection.emptyEntryFor(fullName);
                return new ListLsubEntry[] { listEntry == null ? emptyEntryFor : listEntry, lsubEntry == null ? emptyEntryFor : lsubEntry };
            }
            /*
             * Return
             */
            ListLsubEntry listEntry = collection.getList(fullName);
            if (!seemsValid(listEntry)) {
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

    /**
     * Gets the LIST entry marked with "\Drafts" attribute.
     * <p>
     * Needs the <code>"SPECIAL-USE"</code> capability.
     *
     * @param accountId The account identifier
     * @param imapFolder The IMAP folder
     * @param session The session
     * @return The entry or <code>null</code>
     * @throws OXException If loading the entry fails
     * @throws MessagingException If a messaging error occurs
     */
    public static ListLsubEntry getDraftsEntry(final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException, MessagingException {
        final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        if (isAccessible(collection)) {
            return collection.getDraftsEntry();
        }
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection)) {
                return collection.getDraftsEntry();
            }
            /*
             * Return
             */
            return collection.getDraftsEntry();
        }
    }

    /**
     * Gets the LIST entry marked with "\Junk" attribute.
     * <p>
     * Needs the <code>"SPECIAL-USE"</code> capability.
     *
     * @param accountId The account identifier
     * @param imapFolder The IMAP folder
     * @param session The session
     * @return The entry or <code>null</code>
     * @throws OXException If loading the entry fails
     * @throws MessagingException If a messaging error occurs
     */
    public static ListLsubEntry getJunkEntry(final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException, MessagingException {
        final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        if (isAccessible(collection)) {
            return collection.getJunkEntry();
        }
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection)) {
                return collection.getJunkEntry();
            }
            /*
             * Return
             */
            return collection.getJunkEntry();
        }
    }

    /**
     * Gets the LIST entry marked with "\Sent" attribute.
     * <p>
     * Needs the <code>"SPECIAL-USE"</code> capability.
     *
     * @param accountId The account identifier
     * @param imapFolder The IMAP folder
     * @param session The session
     * @return The entry or <code>null</code>
     * @throws OXException If loading the entry fails
     * @throws MessagingException If a messaging error occurs
     */
    public static ListLsubEntry getSentEntry(final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException, MessagingException {
        final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        if (isAccessible(collection)) {
            return collection.getSentEntry();
        }
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection)) {
                return collection.getSentEntry();
            }
            /*
             * Return
             */
            return collection.getSentEntry();
        }
    }

    /**
     * Gets the LIST entry marked with "\Trash" attribute.
     * <p>
     * Needs the <code>"SPECIAL-USE"</code> capability.
     *
     * @param accountId The account identifier
     * @param imapFolder The IMAP folder
     * @param session The session
     * @return The entry or <code>null</code>
     * @throws OXException If loading the entry fails
     * @throws MessagingException If a messaging error occurs
     */
    public static ListLsubEntry getTrashEntry(final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException, MessagingException {
        final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        if (isAccessible(collection)) {
            return collection.getTrashEntry();
        }
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection)) {
                return collection.getTrashEntry();
            }
            /*
             * Return
             */
            return collection.getTrashEntry();
        }
    }

    private static ListLsubCollection getCollection(final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException, MessagingException {
        /*
         * Initialize appropriate cache entry
         */
        KeyedCache cache = optCache(session);
        if (null == cache) {
            throw ServiceExceptionCode.absentService(CacheService.class);
        }

        // Get the associated map
        @SuppressWarnings("unchecked")
        ConcurrentMap<Integer, Future<ListLsubCollection>> map = (ConcurrentMap<Integer, Future<ListLsubCollection>>) cache.get();
        if (null == map) {
            NonBlockingHashMap<Integer, Future<ListLsubCollection>> newmap = new NonBlockingHashMap<Integer, Future<ListLsubCollection>>();
            map = cache.putIfAbsent(newmap);
            if (null == map) {
                map = newmap;
            }
        }

        // Submit task
        Future<ListLsubCollection> f = map.get(Integer.valueOf(accountId));
        boolean caller = false;
        if (null == f) {
            FutureTask<ListLsubCollection> ft = new FutureTask<ListLsubCollection>(new Callable<ListLsubCollection>() {

                @Override
                public ListLsubCollection call() throws OXException, MessagingException {
                    String[] shared;
                    String[] user;
                    try {
                        IMAPStore imapStore = (IMAPStore) imapFolder.getStore();
                        try {
                            shared = check(NamespaceFoldersCache.getSharedNamespaces(imapStore, true, session, accountId));
                        } catch (MessagingException e) {
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
                        } catch (MessagingException e) {
                            if (imapStore.hasCapability("NAMESPACE")) {
                                LOG.warn("Couldn't get user namespaces.", e);
                            } else {
                                LOG.debug("Couldn't get user namespaces.", e);
                            }
                            user = new String[0];
                        } catch (RuntimeException e) {
                            LOG.warn("Couldn't get user namespaces.", e);
                            user = new String[0];
                        }
                    } catch (MessagingException e) {
                        throw MimeMailException.handleMessagingException(e);
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
        } catch (OXException e) {
            if (caller) {
                cache.remove();
            }
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw MailExceptionCode.INTERRUPT_ERROR.create(e, e.getMessage());
        }
    }

    static String[] check(String[] array) {
        List<String> list = new ArrayList<String>(array.length);
        for (int i = 0; i < array.length; i++) {
            String s = array[i];
            if (!isEmpty(s)) {
                list.add(s);
            }
        }
        return list.toArray(new String[list.size()]);
    }

    private static boolean isEmpty(String string) {
        if (null == string) {
            return true;
        }
        int len = string.length();
        boolean isWhitespace = true;
        for (int i = 0; isWhitespace && i < len; i++) {
            isWhitespace = com.openexchange.java.Strings.isWhitespace(string.charAt(i));
        }
        return isWhitespace;
    }

    /**
     * Checks for any subscribed subfolder.
     *
     * @param fullName The full name
     * @return <code>true</code> if a subscribed subfolder exists; otherwise <code>false</code>
     * @throws OXException If a mail error occurs
     * @throws MessagingException If a messaging error occurs
     */
    public static boolean hasAnySubscribedSubfolder(final String fullName, final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException, MessagingException {
        final ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        if (isAccessible(collection)) {
            return collection.hasAnySubscribedSubfolder(fullName);
        }
        synchronized (collection) {
            checkTimeStamp(imapFolder, collection);
            return collection.hasAnySubscribedSubfolder(fullName);
        }
    }

    private static ListLsubCollection getFrom(final Future<ListLsubCollection> future) throws OXException, InterruptedException, MessagingException {
        if (null == future) {
            return null;
        }
        try {
            return future.get();
        } catch (final ExecutionException e) {
            final Throwable t = e.getCause();
            if (t instanceof OXException) {
                throw (OXException) t;
            }
            if (t instanceof MessagingException) {
                throw (MessagingException) t;
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
            Thread.currentThread().interrupt();
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
