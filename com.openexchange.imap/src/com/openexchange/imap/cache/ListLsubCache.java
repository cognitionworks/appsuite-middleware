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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Strings.isEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.mail.MessagingException;
import org.slf4j.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.openexchange.caching.CacheService;
import com.openexchange.caching.events.CacheEvent;
import com.openexchange.caching.events.CacheEventService;
import com.openexchange.exception.OXException;
import com.openexchange.imap.config.IMAPProperties;
import com.openexchange.imap.services.Services;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.session.Session;
import com.openexchange.session.UserAndContext;
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

    private static final ListLsubCache INSTANCE = new ListLsubCache();

    /**
     * The region name.
     */
    public static final String REGION = "ListLsubCache";

    /** The default timeout for LIST/LSUB cache (6 minutes) */
    private static final long DEFAULT_TIMEOUT = 360000;

    private static final String INBOX = "INBOX";

    /** The cache */
    static final LoadingCache<UserAndContext, Cache<Integer, ListLsubCollection>> CACHE = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.DAYS).build(new CacheLoader<UserAndContext, Cache<Integer, ListLsubCollection>>() {

        @Override
        public Cache<Integer, ListLsubCollection> load(UserAndContext key) throws Exception {
            return CacheBuilder.newBuilder().expireAfterAccess(getTimeout() << 1, TimeUnit.MILLISECONDS).build();
        }
    });

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
    public static void dropFor(Session session) {
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
    public static void dropFor(int userId, int contextId) {
        dropFor(userId, contextId, true);
    }

    /**
     * Drop caches for given user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param notify Whether to notify
     */
    public static void dropFor(int userId, int contextId, boolean notify) {
        CACHE.invalidate(UserAndContext.newInstance(userId, contextId));

        if (notify) {
            fireInvalidateCacheEvent(userId, contextId);
        }
        LOG.debug("Cleaned user-sensitive LIST/LSUB cache for user {} in context {}", Integer.valueOf(userId), Integer.valueOf(contextId));
    }

    /**
     * Removes cached LIST/LSUB entry.
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param session The session
     */
    public static void removeCachedEntry(String fullName, int accountId, Session session) {
        Cache<Integer, ListLsubCollection> cache = CACHE.getIfPresent(UserAndContext.newInstance(session));
        if (null == cache) {
            return;
        }
        ListLsubCollection collection = cache.getIfPresent(Integer.valueOf(accountId));
        if (null != collection) {
            synchronized (collection) {
                collection.remove(fullName);
            }

            fireInvalidateCacheEvent(session);
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
    public static Boolean consideredAsMBox(int accountId, IMAPFolder imapFolder, Session session) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session);
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
    public static void clearCache(int accountId, Session session) {
        clearCache(accountId, session.getUserId(), session.getContextId());
    }

    /**
     * Clears the cache.
     *
     * @param accountId The account ID
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    public static void clearCache(int accountId, int userId, int contextId) {
        Cache<Integer, ListLsubCollection> cache = CACHE.getIfPresent(UserAndContext.newInstance(userId, contextId));
        if (null == cache) {
            return;
        }
        ListLsubCollection collection = cache.getIfPresent(Integer.valueOf(accountId));
        if (null != collection) {
            synchronized (collection) {
                collection.clear();
            }

            fireInvalidateCacheEvent(userId, contextId);
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
    public static void addSingle(String fullName, int accountId, IMAPFolder imapFolder, Session session) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection)) {
                return;
            }
            collection.addSingle(fullName, imapFolder, DO_STATUS, DO_GETACL);

            fireInvalidateCacheEvent(session);
        }
    }

    /**
     * Adds single entry to cache. Replaces any existing entry.
     *
     * @param accountId The account ID
     * @param imapFolder The IMAP folder providing connected protocol
     * @param session The session
     * @throws OXException If entry could not be added
     * @throws MessagingException If a messaging error occurs
     */
    public static ListLsubEntry addSingleByFolder(int accountId, IMAPFolder imapFolder, Session session) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        synchronized (collection) {
            boolean addIt = !checkTimeStamp(imapFolder, collection);
            return collection.addSingleByFolder(imapFolder, addIt);
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
    public static char getSeparator(int accountId, IMAPStore imapStore, Session session) throws OXException {
        try {
            return getSeparator(accountId, (IMAPFolder) imapStore.getFolder(INBOX), session);
        } catch (MessagingException e) {
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
    public static char getSeparator(int accountId, IMAPFolder imapFolder, Session session) throws OXException, MessagingException {
        return getCachedLISTEntry(INBOX, accountId, imapFolder, session).getSeparator();
    }

    private static boolean seemsValid(ListLsubEntry entry) {
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
    public static ListLsubEntry getCachedLSUBEntry(String fullName, int accountId, IMAPFolder imapFolder, Session session) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        if (isAccessible(collection)) {
            ListLsubEntry entry = collection.getLsub(fullName);
            if (seemsValid(entry)) {
                return entry;
            }
        }
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection)) {
                ListLsubEntry entry = collection.getLsub(fullName);
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
            fireInvalidateCacheEvent(session);
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
    public static ListLsubEntry getCachedLISTEntry(String fullName, int accountId, IMAPStore imapStore, Session session) throws OXException {
        try {
            IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(INBOX);
            ListLsubCollection collection = getCollection(accountId, imapFolder, session);
            if (isAccessible(collection)) {
                ListLsubEntry entry = collection.getList(fullName);
                if (seemsValid(entry)) {
                    return entry;
                }
            }
            synchronized (collection) {
                if (checkTimeStamp(imapFolder, collection)) {
                    ListLsubEntry entry = collection.getList(fullName);
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
                fireInvalidateCacheEvent(session);
                entry = collection.getList(fullName);
                return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
            }
        } catch (MessagingException e) {
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
    public static void initACLs(int accountId, IMAPStore imapStore, Session session) throws OXException {
        if (DO_GETACL) {
            // Already perform during initialization
            return;
        }
        try {
            IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(INBOX);
            ListLsubCollection collection = getCollection(accountId, imapFolder, session);
            synchronized (collection) {
                collection.initACLs(imapFolder);
            }
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        }
    }

    /**
     * Gets the pretty-printed cache content
     *
     * @param accountId The account identifier
     * @param session The associated session
     * @return The pretty-printed content or <code>null</code>
     */
    public static String prettyPrintCache(int accountId, Session session) {
        return prettyPrintCache(accountId, session.getUserId(), session.getContextId());
    }

    /**
     * Gets the pretty-printed cache content
     *
     * @param accountId The account identifier
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The pretty-printed content or <code>null</code>
     */
    public static String prettyPrintCache(int accountId, int userId, int contextId) {
        try {
            // Get the associated map
            Cache<Integer, ListLsubCollection> cache = CACHE.getIfPresent(UserAndContext.newInstance(userId, contextId));
            if (null == cache) {
                return null;
            }

            // Submit task
            ListLsubCollection collection = cache.getIfPresent(Integer.valueOf(accountId));
            if (null == collection) {
                return null;
            }

            return collection.toString();
        } catch (@SuppressWarnings("unused") Exception e) {
            return null;
        }
    }

    /**
     * Tries to gets cached LIST entry for specified full name.
     * <p>
     * Performs no initializations if cache or entry is absent
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param imapStore The IMAP store
     * @param session The session
     * @return The cached LIST entry
     * @throws MailException If loading the entry fails
     */
    public static ListLsubEntry getActualLISTEntry(String fullName, int accountId, IMAPStore imapStore, Session session) throws OXException {
        try {
            IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(INBOX);
            ListLsubCollection collection = getCollection(accountId, imapFolder, session);
            synchronized (collection) {
                return collection.getActualEntry(fullName, imapFolder);
            }
        } catch (MessagingException e) {
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
    public static ListLsubEntry getCachedLISTEntry(String fullName, int accountId, IMAPFolder imapFolder, Session session) throws OXException, MessagingException {
        return getCachedLISTEntry(fullName, accountId, imapFolder, session, false);
    }

    /**
     * Gets cached LIST entry for specified full name.
     *
     * @param fullName The full name
     * @param accountId The account ID
     * @param imapFolder The IMAP
     * @param session The session
     * @param ignoreSubscriptions Whether to ignore subscriptions
     * @param reinitSpecialUseIfLoaded <code>true</code> to re-initialize SPECIAL-USE folders in case cache is already loaded; otherwise <code>false</code>
     * @return The cached LIST entry
     * @throws OXException If loading the entry fails
     * @throws MessagingException If a messaging error occurs
     */
    public static ListLsubEntry getCachedLISTEntry(String fullName, int accountId, IMAPFolder imapFolder, Session session, boolean reinitSpecialUseIfLoaded) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        if (isAccessible(collection)) {
            ListLsubEntry entry = collection.getList(fullName);
            if (seemsValid(entry)) {
                if (reinitSpecialUseIfLoaded) {
                    collection.reinitSpecialUseFolders(imapFolder);
                }
                return entry;
            }
        }
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection)) {
                if (reinitSpecialUseIfLoaded) {
                    collection.reinitSpecialUseFolders(imapFolder);
                }
                ListLsubEntry entry = collection.getList(fullName);
                return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
            }
            /*
             * Return
             */
            ListLsubEntry entry = collection.getList(fullName);
            if (seemsValid(entry)) {
                if (reinitSpecialUseIfLoaded) {
                    collection.reinitSpecialUseFolders(imapFolder);
                }
                return entry;
            }
            /*
             * Update & re-check
             */
            collection.update(fullName, imapFolder, DO_STATUS, DO_GETACL);
            fireInvalidateCacheEvent(session);
            entry = collection.getList(fullName);
            return null == entry ? ListLsubCollection.emptyEntryFor(fullName) : entry;
        }
    }

    private static boolean checkTimeStamp(IMAPFolder imapFolder, ListLsubCollection collection) throws MessagingException {
        /*
         * Check collection's stamp
         */
        if (collection.isDeprecated() || ((System.currentTimeMillis() - collection.getStamp()) > getTimeout())) {
            collection.reinit(imapFolder, DO_STATUS, DO_GETACL);
            return true;
        }
        return false;
    }

    private static boolean isAccessible(ListLsubCollection collection) {
        return !collection.isDeprecated() && ((System.currentTimeMillis() - collection.getStamp()) <= getTimeout());
    }

    static long getTimeout() {
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
            fireInvalidateCacheEvent(session);
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
            ListLsubEntry listEntry = collection.getList(fullName);
            if (seemsValid(listEntry)) {
                ListLsubEntry lsubEntry = collection.getLsub(fullName);
                ListLsubEntry emptyEntryFor = ListLsubCollection.emptyEntryFor(fullName);
                return new ListLsubEntry[] { listEntry, lsubEntry == null ? emptyEntryFor : lsubEntry };
            }
        }
        synchronized (collection) {
            if (checkTimeStamp(imapFolder, collection)) {
                ListLsubEntry listEntry = collection.getList(fullName);
                ListLsubEntry lsubEntry = collection.getLsub(fullName);
                ListLsubEntry emptyEntryFor = ListLsubCollection.emptyEntryFor(fullName);
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
                fireInvalidateCacheEvent(session);
                listEntry = collection.getList(fullName);
            }
            ListLsubEntry lsubEntry = collection.getLsub(fullName);
            ListLsubEntry emptyEntryFor = ListLsubCollection.emptyEntryFor(fullName);
            return new ListLsubEntry[] { listEntry == null ? emptyEntryFor : listEntry, lsubEntry == null ? emptyEntryFor : lsubEntry };
        }
    }

    /**
     * Re-Initializes the SPECIAL-USE folders (only if the IMAP store advertises support for <code>"SPECIAL-USE"</code> capability)
     *
     * @param accountId The account identifier
     * @param imapFolder The IMAP store
     * @param session The session
     * @throws OXException If re-initialization fails
     * @throws MessagingException If a messaging error occurs
     */
    public static void reinitSpecialUseFolders(int accountId, IMAPFolder imapFolder, Session session) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        synchronized (collection) {
            if (isAccessible(collection)) {
                collection.reinitSpecialUseFolders(imapFolder);
            } else {
                checkTimeStamp(imapFolder, collection);
                collection.reinitSpecialUseFolders(imapFolder);
            }
        }
    }

    /**
     * Gets the LIST entries marked with "\Drafts" attribute.
     * <p>
     * Needs the <code>"SPECIAL-USE"</code> capability.
     *
     * @param accountId The account identifier
     * @param imapFolder The IMAP folder
     * @param session The session
     * @return The entries
     * @throws OXException If loading the entries fails
     * @throws MessagingException If a messaging error occurs
     */
    public static Collection<ListLsubEntry> getDraftsEntry(int accountId, IMAPFolder imapFolder, Session session) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        if (isAccessible(collection)) {
            return collection.getDraftsEntry();
        }
        synchronized (collection) {
            checkTimeStamp(imapFolder, collection);
            return collection.getDraftsEntry();
        }
    }

    /**
     * Gets the LIST entries marked with "\Junk" attribute.
     * <p>
     * Needs the <code>"SPECIAL-USE"</code> capability.
     *
     * @param accountId The account identifier
     * @param imapFolder The IMAP folder
     * @param session The session
     * @return The entries
     * @throws OXException If loading the entries fails
     * @throws MessagingException If a messaging error occurs
     */
    public static Collection<ListLsubEntry> getJunkEntry(int accountId, IMAPFolder imapFolder, Session session) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        if (isAccessible(collection)) {
            return  collection.getJunkEntry();
        }
        synchronized (collection) {
            checkTimeStamp(imapFolder, collection);
            return collection.getJunkEntry();
        }
    }

    /**
     * Gets the LIST entries marked with "\Sent" attribute.
     * <p>
     * Needs the <code>"SPECIAL-USE"</code> capability.
     *
     * @param accountId The account identifier
     * @param imapFolder The IMAP folder
     * @param session The session
     * @return The entries
     * @throws OXException If loading the entries fails
     * @throws MessagingException If a messaging error occurs
     */
    public static Collection<ListLsubEntry> getSentEntry(int accountId, IMAPFolder imapFolder, Session session) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        if (isAccessible(collection)) {
            return collection.getSentEntry();
        }
        synchronized (collection) {
            checkTimeStamp(imapFolder, collection);
            return collection.getSentEntry();
        }
    }

    /**
     * Gets the LIST entries marked with "\Trash" attribute.
     * <p>
     * Needs the <code>"SPECIAL-USE"</code> capability.
     *
     * @param accountId The account identifier
     * @param imapFolder The IMAP folder
     * @param session The session
     * @return The entries
     * @throws OXException If loading the entries fails
     * @throws MessagingException If a messaging error occurs
     */
    public static Collection<ListLsubEntry> getTrashEntry(int accountId, IMAPFolder imapFolder, Session session) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        if (isAccessible(collection)) {
            return collection.getTrashEntry();
        }
        synchronized (collection) {
            checkTimeStamp(imapFolder, collection);
            return collection.getTrashEntry();
        }
    }

    private static ListLsubCollection getCollection(final int accountId, final IMAPFolder imapFolder, final Session session) throws OXException, MessagingException {
        // Get the associated map
        Cache<Integer, ListLsubCollection> cache = CACHE.getUnchecked(UserAndContext.newInstance(session));

        // Check if present
        ListLsubCollection optCollection = cache.getIfPresent(I(accountId));
        if (optCollection != null) {
            return optCollection;
        }

        // Create loader
        final AtomicBoolean caller = new AtomicBoolean(false);
        Callable<ListLsubCollection> loader = new Callable<ListLsubCollection>() {

            @Override
            public ListLsubCollection call() throws OXException, MessagingException {
                // Determine shared and user namespaces
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
                    } catch (RuntimeException e) {
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

                // Create collection instance
                ListLsubCollection collection = new ListLsubCollection(imapFolder, shared, user, DO_STATUS, DO_GETACL);

                // Mark running thread as caller & return collection
                caller.set(true);
                return collection;
            }
        };

        try {
            return getFromCache(accountId, loader, cache);
        } catch (OXException e) {
            if (caller.get()) {
                CACHE.invalidate(UserAndContext.newInstance(session));
            }
            throw e;
        }
    }

    private static ListLsubCollection getFromCache(int accountId, Callable<ListLsubCollection> loader, Cache<Integer, ListLsubCollection> cache) throws OXException, MessagingException {
        try {
            return cache.get(I(accountId), loader);
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            if (t instanceof OXException) {
                throw (OXException) t;
            }
            if (t instanceof MessagingException) {
                throw (MessagingException) t;
            }
            if (t instanceof Error) {
                throw (Error) t;
            }
            throw MailExceptionCode.UNEXPECTED_ERROR.create(t, t.getMessage());
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

    /**
     * Checks for any subscribed subfolder.
     *
     * @param fullName The full name
     * @return <code>true</code> if a subscribed subfolder exists; otherwise <code>false</code>
     * @throws OXException If a mail error occurs
     * @throws MessagingException If a messaging error occurs
     */
    public static boolean hasAnySubscribedSubfolder(String fullName, int accountId, IMAPFolder imapFolder, Session session) throws OXException, MessagingException {
        ListLsubCollection collection = getCollection(accountId, imapFolder, session);
        if (isAccessible(collection)) {
            return collection.hasAnySubscribedSubfolder(fullName);
        }
        synchronized (collection) {
            checkTimeStamp(imapFolder, collection);
            return collection.hasAnySubscribedSubfolder(fullName);
        }
    }

    private static void fireInvalidateCacheEvent(Session session) {
        fireInvalidateCacheEvent(session.getUserId(), session.getContextId());
    }

    private static void fireInvalidateCacheEvent(int userId, int contextId) {
        CacheEventService cacheEventService = Services.optService(CacheEventService.class);
        if (null != cacheEventService && cacheEventService.getConfiguration().remoteInvalidationForPersonalFolders()) {
            CacheEvent event = newCacheEventFor(userId, contextId);
            if (null != event) {
                cacheEventService.notify(INSTANCE, event, false);
            }
        }
    }

    /**
     * Creates a new cache event
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The cache event
     */
    private static CacheEvent newCacheEventFor(int userId, int contextId) {
        CacheService service = Services.optService(CacheService.class);
        return null == service ? null : CacheEvent.INVALIDATE(REGION, Integer.toString(contextId), service.newCacheKey(contextId, userId));
    }

}
