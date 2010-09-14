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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

import java.util.Arrays;
import javax.mail.Folder;
import javax.mail.MessagingException;
import com.openexchange.caching.CacheKey;
import com.openexchange.caching.CacheService;
import com.openexchange.imap.services.IMAPServiceRegistry;
import com.openexchange.mail.cache.SessionMailCache;
import com.openexchange.mail.cache.SessionMailCacheEntry;
import com.openexchange.session.Session;
import com.sun.mail.imap.IMAPStore;

/**
 * {@link NamespaceFoldersCache}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class NamespaceFoldersCache {

    private static final Integer NS_PERSONAL = Integer.valueOf(1);

    private static final Integer NS_USER = Integer.valueOf(2);

    private static final Integer NS_SHARED = Integer.valueOf(3);

    private static final String[] EMPTY_ARR = new String[0];

    /**
     * No instance
     */
    private NamespaceFoldersCache() {
        super();
    }

    /**
     * Gets cached personal namespaces when invoking <code>NAMESPACE</code> command on given IMAP store
     * 
     * @param imapStore The IMAP store on which <code>NAMESPACE</code> command is invoked
     * @param load Whether <code>NAMESPACE</code> command should be invoked if no cache entry present or not
     * @param session The session providing the session-bound cache
     * @param accountId The account ID
     * @return The <b>binary-sorted</b> personal namespace folders
     * @throws MessagingException If <code>NAMESPACE</code> command fails
     */
    public static String[] getPersonalNamespaces(final IMAPStore imapStore, final boolean load, final Session session, final int accountId) throws MessagingException {
        final NamespaceFoldersCacheEntry entry = new NamespaceFoldersCacheEntry(NS_PERSONAL);
        final SessionMailCache mailCache = SessionMailCache.getInstance(session, accountId);
        mailCache.get(entry);
        if (load && (null == entry.getValue())) {
            final Folder[] pns = imapStore.getPersonalNamespaces();
            if ((pns == null) || (pns.length == 0)) {
                entry.setValue(EMPTY_ARR);
            } else {
                final String[] fullnames = new String[pns.length];
                for (int i = 0; i < pns.length; i++) {
                    fullnames[i] = pns[i].getFullName();
                }
                Arrays.sort(fullnames);
                entry.setValue(fullnames);
            }
            mailCache.put(entry);
        }
        return entry.getValue();
    }

    /**
     * Checks if personal namespaces contain the specified fullname
     * 
     * @param fullname The fullname to check
     * @param imapStore The IMAP store
     * @param load Whether <code>NAMESPACE</code> command should be invoked if no cache entry present or not
     * @param session The session providing the session-bound cache
     * @param accountId The account ID
     * @return <code>true</code> if personal namespaces contain the specified fullname; otherwise <code>false</code>
     * @throws MessagingException If <code>NAMESPACE</code> command fails
     */
    public static boolean containedInPersonalNamespaces(final String fullname, final IMAPStore imapStore, final boolean load, final Session session, final int accountId) throws MessagingException {
        return Arrays.binarySearch(getPersonalNamespaces(imapStore, load, session, accountId), fullname) >= 0;
    }

    /**
     * Gets cached user namespaces when invoking <code>NAMESPACE</code> command on given IMAP store
     * 
     * @param imapStore The IMAP store on which <code>NAMESPACE</code> command is invoked
     * @param load Whether <code>NAMESPACE</code> command should be invoked if no cache entry present or not
     * @param session The session providing the session-bound cache
     * @param accountId The account ID
     * @return The <b>binary-sorted</b> user namespace folders
     * @throws MessagingException If <code>NAMESPACE</code> command fails
     */
    public static String[] getUserNamespaces(final IMAPStore imapStore, final boolean load, final Session session, final int accountId) throws MessagingException {
        final NamespaceFoldersCacheEntry entry = new NamespaceFoldersCacheEntry(NS_USER);
        final SessionMailCache mailCache = SessionMailCache.getInstance(session, accountId);
        mailCache.get(entry);
        if (load && (null == entry.getValue())) {
            final Folder[] uns = imapStore.getUserNamespaces(null);
            if ((uns == null) || (uns.length == 0)) {
                entry.setValue(EMPTY_ARR);
            } else {
                final String[] fullnames = new String[uns.length];
                for (int i = 0; i < uns.length; i++) {
                    fullnames[i] = uns[i].getFullName();
                }
                Arrays.sort(fullnames);
                entry.setValue(fullnames);
            }
            mailCache.put(entry);
        }
        return entry.getValue();
    }

    /**
     * Checks if user namespaces contain the specified fullname
     * 
     * @param fullname The fullname to check
     * @param imapStore The IMAP store
     * @param load Whether <code>NAMESPACE</code> command should be invoked if no cache entry present or not
     * @param session The session providing the session-bound cache
     * @param accountId The account ID
     * @return <code>true</code> if user namespaces contain the specified fullname; otherwise <code>false</code>
     * @throws MessagingException If <code>NAMESPACE</code> command fails
     */
    public static boolean containedInUserNamespaces(final String fullname, final IMAPStore imapStore, final boolean load, final Session session, final int accountId) throws MessagingException {
        return Arrays.binarySearch(getUserNamespaces(imapStore, load, session, accountId), fullname) >= 0;
    }

    /**
     * Gets cached shared namespaces when invoking <code>NAMESPACE</code> command on given IMAP store
     * 
     * @param imapStore The IMAP store on which <code>NAMESPACE</code> command is invoked
     * @param load Whether <code>NAMESPACE</code> command should be invoked if no cache entry present or not
     * @param session The session providing the session-bound cache
     * @param accountId The account ID
     * @return The <b>binary-sorted</b> shared namespace folders
     * @throws MessagingException If <code>NAMESPACE</code> command fails
     */
    public static String[] getSharedNamespaces(final IMAPStore imapStore, final boolean load, final Session session, final int accountId) throws MessagingException {
        final NamespaceFoldersCacheEntry entry = new NamespaceFoldersCacheEntry(NS_SHARED);
        final SessionMailCache mailCache = SessionMailCache.getInstance(session, accountId);
        mailCache.get(entry);
        if (load && (null == entry.getValue())) {
            final Folder[] sns = imapStore.getSharedNamespaces();
            if ((sns == null) || (sns.length == 0)) {
                entry.setValue(EMPTY_ARR);
            } else {
                final String[] fullnames = new String[sns.length];
                for (int i = 0; i < sns.length; i++) {
                    fullnames[i] = sns[i].getFullName();
                }
                Arrays.sort(fullnames);
                entry.setValue(fullnames);
            }
            mailCache.put(entry);
        }
        return entry.getValue();
    }

    /**
     * Checks if shared namespaces contain the specified fullname
     * 
     * @param fullname The fullname to check
     * @param imapStore The IMAP store
     * @param load Whether <code>NAMESPACE</code> command should be invoked if no cache entry present or not
     * @param session The session providing the session-bound cache
     * @param accountId The account ID
     * @return <code>true</code> if shared namespaces contain the specified fullname; otherwise <code>false</code>
     * @throws MessagingException If <code>NAMESPACE</code> command fails
     */
    public static boolean containedInSharedNamespaces(final String fullname, final IMAPStore imapStore, final boolean load, final Session session, final int accountId) throws MessagingException {
        return Arrays.binarySearch(getSharedNamespaces(imapStore, load, session, accountId), fullname) >= 0;
    }

    private static final class NamespaceFoldersCacheEntry implements SessionMailCacheEntry<String[]> {

        private final Integer namespaceKey;

        private String[] fullnames;

        private CacheKey key;

        public NamespaceFoldersCacheEntry(final Integer namespaceKey) {
            this(namespaceKey, null);
        }

        public NamespaceFoldersCacheEntry(final Integer namespaceKey, final String[] fullnames) {
            super();
            this.namespaceKey = namespaceKey;
            this.fullnames = fullnames;
        }

        private CacheKey getKeyInternal() {
            if (null == key) {
                key = IMAPServiceRegistry.getService(CacheService.class).newCacheKey(
                    MailCacheCode.NAMESPACE_FOLDERS.getCode(),
                    namespaceKey);
            }
            return key;
        }

        public CacheKey getKey() {
            return getKeyInternal();
        }

        public String[] getValue() {
            return fullnames;
        }

        public void setValue(final String[] value) {
            fullnames = value;
        }

        public Class<String[]> getEntryClass() {
            return String[].class;
        }

    }
}
