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

package com.openexchange.unifiedinbox.utility;

import static com.openexchange.mail.MailPath.SEPERATOR;
import static com.openexchange.mail.utils.MailFolderUtility.prepareFullname;
import static com.openexchange.mail.utils.MailFolderUtility.prepareMailFolderParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.openexchange.config.ConfigurationService;
import com.openexchange.mail.FullnameArgument;
import com.openexchange.mail.MailException;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.tools.stream.UnsynchronizedStringWriter;
import com.openexchange.unifiedinbox.UnifiedINBOXAccess;
import com.openexchange.unifiedinbox.UnifiedINBOXException;
import com.openexchange.unifiedinbox.UnifiedINBOXUID;
import com.openexchange.unifiedinbox.services.UnifiedINBOXServiceRegistry;

/**
 * {@link UnifiedINBOXUtility} - Utility methods for Unified INBOX.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class UnifiedINBOXUtility {

    /**
     * Initializes a new {@link UnifiedINBOXUtility}.
     */
    private UnifiedINBOXUtility() {
        super();
    }

    /**
     * Gets the default max. running millis.
     * 
     * @return The default max. running millis
     */
    public static long getMaxRunningMillis() {
        final ConfigurationService service = UnifiedINBOXServiceRegistry.getServiceRegistry().getService(ConfigurationService.class);
        if (null == service) {
            return 60000L;
        }
        return service.getIntProperty("AJP_WATCHER_MAX_RUNNING_TIME", 60000);
    }

    /**
     * Parses specified Unified INBOX mail IDs.
     * 
     * @param mailIDs The Unified INBOX mail IDs to parse
     * @return A map grouping referenced accounts and referenced fullnames and IDs.
     * @throws MailException If parsing mail IDs fails
     */
    public static Map<Integer, Map<String, List<String>>> parseMailIDs(final String[] mailIDs) throws MailException {
        final Map<Integer, Map<String, List<String>>> map = new HashMap<Integer, Map<String, List<String>>>(mailIDs.length);
        // Start parsing
        final UnifiedINBOXUID uidl = new UnifiedINBOXUID();
        for (final String mailID : mailIDs) {
            uidl.setUIDString(mailID);

            final Integer key = Integer.valueOf(uidl.getAccountId());
            Map<String, List<String>> folderUIDMap = map.get(key);
            if (null == folderUIDMap) {
                folderUIDMap = new HashMap<String, List<String>>(mailIDs.length / 2);
                map.put(key, folderUIDMap);
            }
            final String folder = uidl.getFullName();
            List<String> uids = folderUIDMap.get(folder);
            if (null == uids) {
                uids = new ArrayList<String>();
                folderUIDMap.put(folder, uids);
            }
            uids.add(uidl.getId());
        }
        return map;
    }

    /**
     * Generates a nested folder's fullname.
     * 
     * @param uiAccountId The Unified INBOX's account ID
     * @param uiFullname The Unified INBOX's fullname
     * @param nestedAccountId The nested account's ID
     * @param nestedFullname The nested folder's fullname
     * @return The generated nested folder's fullname.
     */
    public static String generateNestedFullname(final int uiAccountId, final String uiFullname, final int nestedAccountId, final String nestedFullname) {
        return new StringBuilder(32).append(prepareFullname(uiAccountId, uiFullname)).append(SEPERATOR).append(
            prepareFullname(nestedAccountId, nestedFullname)).toString();
    }

    /**
     * Parses nested fullname.
     * <p>
     * <code>"INBOX/default3/INBOX"</code> =&gt; <code>"default3/INBOX"</code>
     * 
     * @param nestedFullname The nested fullname to parse
     * @return The parsed nested fullname argument
     * @throws UnifiedINBOXException If specified nested fullname is invalid
     */
    public static FullnameArgument parseNestedFullname(final String nestedFullname) throws UnifiedINBOXException {
        // INBOX/default0/INBOX
        if (!startsWithKnownFullname(nestedFullname)) {
            throw new UnifiedINBOXException(
                UnifiedINBOXException.Code.FOLDER_NOT_FOUND,
                prepareMailFolderParam(nestedFullname).getFullname());
        }
        // Cut off starting known fullname and its separator character
        final String fn = nestedFullname.substring(nestedFullname.indexOf(SEPERATOR) + 1);
        return prepareMailFolderParam(fn);
    }

    private static boolean startsWithKnownFullname(final String fullname) {
        for (final String knownFullname : UnifiedINBOXAccess.KNOWN_FOLDERS) {
            if (fullname.startsWith(knownFullname)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the account's fullname.
     * 
     * @param mailAccess The mail access to desired account
     * @param fullname The fullname to look-up
     * @return The account's fullname
     * @throws MailException If fullname look-up fails
     */
    public static String determineAccountFullname(final MailAccess<?, ?> mailAccess, final String fullname) throws MailException {
        if (UnifiedINBOXAccess.INBOX.equals(fullname)) {
            return UnifiedINBOXAccess.INBOX;
        }
        if (UnifiedINBOXAccess.DRAFTS.equals(fullname)) {
            return mailAccess.getFolderStorage().getDraftsFolder();
        }
        if (UnifiedINBOXAccess.SENT.equals(fullname)) {
            return mailAccess.getFolderStorage().getSentFolder();
        }
        if (UnifiedINBOXAccess.SPAM.equals(fullname)) {
            return mailAccess.getFolderStorage().getSpamFolder();
        }
        if (UnifiedINBOXAccess.TRASH.equals(fullname)) {
            return mailAccess.getFolderStorage().getTrashFolder();
        }
        throw new UnifiedINBOXException(UnifiedINBOXException.Code.UNKNOWN_DEFAULT_FOLDER_INDEX, fullname);
    }

    /**
     * Prints specified {@link Throwable}'s stack trace to given string builder.
     * 
     * @param t The {@link Throwable} instance
     * @param builder The string builder to append to
     */
    public static void appendStackTrace2StringBuilder(final Throwable t, final StringBuilder builder) {
        t.printStackTrace(new java.io.PrintWriter(new UnsynchronizedStringWriter(builder)));
    }

}
