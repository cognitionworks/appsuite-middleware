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

package com.openexchange.imap.util;

import javax.mail.FolderNotFoundException;
import javax.mail.MessagingException;
import com.openexchange.exception.OXException;
import com.openexchange.imap.IMAPCommandsCollection;
import com.openexchange.imap.IMAPException;
import com.openexchange.imap.IMAPFolderStorage;
import com.openexchange.imap.IMAPFolderWorker;
import com.openexchange.imap.cache.ListLsubCache;
import com.openexchange.imap.config.IMAPConfig;
import com.openexchange.imap.converters.IMAPFolderConverter;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.session.Session;
import com.sun.mail.iap.CommandFailedException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.protocol.ListInfo;

/**
 * {@link FolderUtility} - A session-bound cache for IMAP folders converted to a {@link MailFolder} instance.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class FolderUtility {

    /**
     * No instance
     */
    private FolderUtility() {
        super();
    }

    /**
     * Loads denoted IMAP folder.
     *
     * @param fullName The IMAP folder full name
     * @param folderStorage The connected IMAP folder storage
     * @return The IMAP folder
     * @throws OXException If loading the folder fails
     */
    public static final MailFolder loadFolder(String fullName, IMAPFolderStorage folderStorage) throws OXException {
        return loadFolder(fullName, folderStorage, null);
    }

    /**
     * Loads denoted IMAP folder.
     *
     * @param fullName The IMAP folder full name
     * @param folderStorage The connected IMAP folder storage
     * @param The possibly loaded IMAP folder; may be <code>null</code>
     * @return The IMAP folder
     * @throws OXException If loading the folder fails
     */
    public static final MailFolder loadFolder(String fullName, IMAPFolderStorage folderStorage, IMAPFolder imapFolder) throws OXException {
        Session session = folderStorage.getSession();
        IMAPConfig imapConfig = folderStorage.getImapConfig();
        String imapFullName = fullName;
        try {
            IMAPFolderWorker.checkFailFast(folderStorage.getImapStore(), fullName);
            if (null != imapFolder) {
                return IMAPFolderConverter.convertFolder(imapFolder, folderStorage.getSession(), folderStorage.getImapAccess(), folderStorage.getContext());
            }

            // Load w/o IMAP folder instance
            IMAPStore imapStore = folderStorage.getImapStore();
            IMAPFolder f;
            if (MailFolder.ROOT_FOLDER_ID.equals(fullName)) {
                f = (IMAPFolder) imapStore.getDefaultFolder();
                imapFullName = "";
            } else {
                f = (IMAPFolder) imapStore.getFolder(fullName);
                boolean ignoreSubscription = folderStorage.getImapConfig().getIMAPProperties().isIgnoreSubscription();
                boolean exists = "INBOX".equals(imapFullName) || ListLsubCache.getCachedLISTEntry(imapFullName, folderStorage.getAccountId(), f, session, ignoreSubscription).exists();
                if (!exists) {
                    // Do explicit LIST for "hidden" folders not appearing in LIST "" "*", but dedicatedly LISTable or EXAMINEable
                    ListInfo listInfo = IMAPCommandsCollection.getListInfo(imapFullName, f);
                    if (null == listInfo && false == canBeOpened(f)) {
                        // Is dedicatedly EXAMINEable?
                        f = folderStorage.checkForNamespaceFolder(imapFullName, f);
                        if (null == f) {
                            throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                        }
                    }
                }
            }
            return IMAPFolderConverter.convertFolder(f, session, folderStorage.getImapAccess(), folderStorage.getContext());
        } catch (FolderNotFoundException e) {
            ListLsubCache.removeCachedEntry(imapFullName, folderStorage.getAccountId(), session);
            throw folderStorage.handleMessagingException(fullName, e);
        } catch (MessagingException e) {
            throw folderStorage.handleMessagingException(fullName, e);
        }
    }

    /**
     * Checks if specified IMAP folder can be opened
     *
     * @param f The IMAP folder to check
     * @return <code>true</code> if EXAMINE succeeded; otherwise <code>false</code> in case a NO response is signaled
     * @throws MessagingException
     */
    public static boolean canBeOpened(IMAPFolder f) throws MessagingException {
        try {
            f.open(IMAPFolder.READ_ONLY);
            return true;
        } catch (@SuppressWarnings("unused") FolderNotFoundException e) {
            // Apparently no such folder exists, hence cannot be opened
            return false;
        } catch (MessagingException e) {
            if ("folder cannot contain messages".equals(e.getMessage())) {
                // Folder could not be opened because it cannot hold messages
                return false;
            }

            Exception exception = e.getNextException();
            if (exception instanceof CommandFailedException) {
                // Folder could not be opened due to a NO response
                return false;
            }

            // Got a BAD or a BYE? Then connection may be bad. Rethrow...
            throw e;
        }
    }

}
