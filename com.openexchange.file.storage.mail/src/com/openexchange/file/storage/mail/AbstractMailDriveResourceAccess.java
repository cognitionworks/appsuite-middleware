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
 *    trademarks of the OX Software GmbH. group of companies.
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

package com.openexchange.file.storage.mail;

import static com.openexchange.java.Autoboxing.I;
import java.util.Locale;
import javax.mail.MessagingException;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.file.storage.mail.osgi.Services;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.mail.utils.StorageUtility;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.UserService;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;

/**
 * {@link AbstractMailDriveResourceAccess}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.2
 */
public abstract class AbstractMailDriveResourceAccess {

    /** The associated session */
    protected final Session session;

    /** The locale of session-associated user */
    protected final Locale locale;

    /** The full names of the virtual attachment folders */
    protected final FullNameCollection fullNameCollection;

    /**
     * Initializes a new {@link AbstractMailDriveResourceAccess}.
     *
     * @throws OXException If initialization fails
     */
    protected AbstractMailDriveResourceAccess(FullNameCollection fullNameCollection, Session session) throws OXException {
        super();
        this.fullNameCollection = fullNameCollection;
        this.session = session;

        if (session instanceof ServerSession) {
            locale = ((ServerSession) session).getUser().getLocale();
        } else {
            UserService userService = Services.getOptionalService(UserService.class);
            locale = null == userService ? Locale.US : userService.getUser(session.getUserId(), session.getContextId()).getLocale();
        }
    }

    /**
     * Gets the root folder identifier
     *
     * @return The root folder identifier
     */
    public String getRootFolderId() {
        return MailFolder.ROOT_FOLDER_ID;
    }

    /**
     * Performs given closure.
     *
     * @param closure The closure to perform
     * @param httpClient The client to use
     * @return The return value
     * @throws OXException If performing closure fails
     */
    protected <R> R perform(MailDriveClosure<R> closure) throws OXException {
        return closure.perform(session);
    }

    /**
     * Closes specified IMAP folder w/o expunge.
     *
     * @param imapFolder The IMAP folder to close
     */
    protected static void closeSafe(IMAPFolder imapFolder) {
        closeSafe(imapFolder, false);
    }

    /**
     * Closes specified IMAP folder.
     *
     * @param imapFolder The IMAP folder to close
     * @param expunge <code>true</code> to expunge all messages marked as <code>\Deleted</code>; otherwise <code>false</code>
     */
    protected static void closeSafe(IMAPFolder imapFolder, boolean expunge) {
        if (null != imapFolder) {
            try {
                imapFolder.close(expunge);
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Checks validity of given folder identifier.
     *
     * @param folderId The folder identifier to check
     * @return The associated full name
     * @throws OXException If folder identifier is invalid
     */
    protected FullName checkFolderId(String folderId) throws OXException {
        FullName fullName = optFolderId(folderId);
        if (null == fullName) {
            throw FileStorageExceptionCodes.FOLDER_NOT_FOUND.create(folderId, MailDriveConstants.ACCOUNT_ID, MailDriveConstants.ID, I(session.getUserId()), I(session.getContextId()));
        }
        return fullName;
    }

    /**
     * Checks given folder identifier.
     *
     * @param folderId The folder identifier to check
     * @return The associated full name or <code>null</code> if folder identifier is invalid
     */
    protected FullName optFolderId(String folderId) {
        return MailDriveAccountAccess.optFolderId(folderId, fullNameCollection);
    }

    /**
     * Gets the IMAP folder for specified full name using given store.
     *
     * @param fullName The full name to fetch by
     * @param imapStore The store to get from
     * @return The IMAP folder
     * @throws MessagingException If IMAP folder cannot be returned
     */
    protected static IMAPFolder getIMAPFolderFor(final FullName fullName, IMAPStore imapStore) throws MessagingException {
        String fn = fullName.getFullName();
        return (IMAPFolder) (fn.length() == 0 ? imapStore.getDefaultFolder() : imapStore.getFolder(fn));
    }

    /**
     * Parses the string argument as a signed decimal <code>long</code>. The characters in the string must all be decimal digits.
     * <p>
     * Note that neither the character <code>L</code> (<code>'&#92;u004C'</code>) nor <code>l</code> (<code>'&#92;u006C'</code>) is
     * permitted to appear at the end of the string as a type indicator, as would be permitted in Java programming language source code.
     *
     * @param s A <code>String</code> containing the <code>long</code> representation to be parsed
     * @return The <code>long</code> represented by the argument in decimal or <code>-1</code> if the string does not contain a parsable
     *         <code>long</code>.
     */
    protected static long parseUnsignedLong(final String s) {
        return StorageUtility.parseUnsignedLong(s);
    }

}
