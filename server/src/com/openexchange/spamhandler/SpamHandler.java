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

package com.openexchange.spamhandler;

import com.openexchange.mail.MailException;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.session.Session;

/**
 * {@link SpamHandler} - The abstract spam handler class used by mail module to handle spam-related actions.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class SpamHandler {

    /**
     * The fallback spam handler
     */
    public static final String SPAM_HANDLER_FALLBACK = "NoSpamHandler";

    /**
     * The fullname of the INBOX folder
     */
    protected static final String FULLNAME_INBOX = "INBOX";

    private final int hashCode;

    /**
     * Initializes a new {@link SpamHandler}
     */
    protected SpamHandler() {
        super();
        hashCode = getSpamHandlerName().hashCode();
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof SpamHandler)) {
            return false;
        }
        final SpamHandler other = (SpamHandler) obj;
        if (getSpamHandlerName() == null) {
            if (other.getSpamHandlerName() != null) {
                return false;
            }
        } else if (!getSpamHandlerName().equals(other.getSpamHandlerName())) {
            return false;
        }
        return true;
    }

    @Override
    public final int hashCode() {
        return hashCode;
    }

    /**
     * Indicates whether to create the confirmed-spam folder during check for default mail folders during login if spam is enabled for
     * logged-in user.<br>
     * Default is <code>true</code>.
     * <p>
     * Implementations may override this method to change the default behavior.
     * 
     * @return <code>true</code> to create the confirmed-spam folder during check for default mail folders; otherwise <code>false</code> to
     *         not create the folder
     */
    public boolean isCreateConfirmedSpam() {
        return true;
    }

    /**
     * Indicates whether to create the confirmed-ham folder during check for default mail folders during login if spam is enabled for
     * logged-in user.<br>
     * Default is <code>true</code>.
     * <p>
     * Implementations may override this method to change the default behavior.
     * 
     * @return <code>true</code> to create the confirmed-ham folder during check for default mail folders; otherwise <code>false</code> to
     *         not create the folder
     */
    public boolean isCreateConfirmedHam() {
        return true;
    }

    /**
     * Indicates whether the confirmed-spam/confirmed-ham folders shall automatically be unsubscribed during login.<br>
     * Default is <code>true</code>.
     * <p>
     * Implementations may override this method to change the default behavior.
     * 
     * @return <code>true</code> to automatically unsubscribe the confirmed-spam/confirmed-ham folders; otherwise <code>false</code> to
     *         leave subscription status unchanged.
     */
    public boolean isUnsubscribeSpamFolders() {
        return true;
    }

    /**
     * Handles messages that should be treated as spam messages. This means to copy the mails identified by specified mail IDs to the
     * defined confirmed spam folder to properly teach the spam system to handle these mails as spam.
     * <p>
     * This method may be overridden if another spam handling is desired.
     * 
     * @param accountId The account ID
     * @param fullname The fullname of the folder containing spam messages
     * @param mailIDs The mail IDs
     * @param move If <code>true</code> the mails identified by specified mail IDs are moved to spam folder; otherwise the mails remain in
     *            the source folder.
     * @param session The session providing needed user data
     * @throws MailException If handling spam fails
     */
    public void handleSpam(final int accountId, final String fullname, final String[] mailIDs, final boolean move, final Session session) throws MailException {
        /*
         * Copy to confirmed spam folder
         */
        final MailAccess<?, ?> mailAccess = MailAccess.getInstance(session, accountId);
        mailAccess.connect();
        try {
            if (isCreateConfirmedSpam()) {
                final String confirmedSpamFullname = mailAccess.getFolderStorage().getConfirmedSpamFolder();
                mailAccess.getMessageStorage().copyMessages(fullname, confirmedSpamFullname, mailIDs, true);
            }
            if (move) {
                /*
                 * Move to spam folder
                 */
                final String spamFullname = mailAccess.getFolderStorage().getSpamFolder();
                mailAccess.getMessageStorage().moveMessages(fullname, spamFullname, mailIDs, true);
            }
        } finally {
            mailAccess.close(true);
        }
    }

    /**
     * Gets the spam handler name which is used on registration
     * 
     * @return The spam handler name
     */
    public abstract String getSpamHandlerName();

    /**
     * Handles messages that are located in spam folder but should be treated as ham messages. This means to copy the mails identified by
     * specified mail IDs to the defined confirmed ham folder to properly teach the spam system to handle these mails as ham.
     * <p>
     * Dependent on the used spam system, the spam messages cannot be copied/moved as they are, but need to be parsed in the way the spam
     * system wraps spam messages. If spam system does not wrap original messages, then the default spam handler is supposed to be used.
     * 
     * @param accountId The account ID
     * @param spamFullname The spam folder's fullname
     * @param mailIDs The mail IDs
     * @param move If <code>true</code> the mails identified by specified mail IDs are moved to INBOX folder; otherwise the mails remain in
     *            spam folder
     * @param session The session providing needed user data
     * @throws MailException If handling ham fails
     */
    public abstract void handleHam(int accountId, String spamFullname, String[] mailIDs, boolean move, Session session) throws MailException;

}
