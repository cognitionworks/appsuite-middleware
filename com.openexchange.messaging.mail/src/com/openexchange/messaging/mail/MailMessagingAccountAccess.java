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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.messaging.mail;

import com.openexchange.exception.OXException;
import com.openexchange.messaging.MessagingAccountAccess;
import com.openexchange.messaging.MessagingExceptionCodes;
import com.openexchange.messaging.MessagingFolder;
import com.openexchange.messaging.MessagingFolderAccess;
import com.openexchange.messaging.MessagingMessageAccess;
import com.openexchange.session.Session;

/**
 * {@link MailMessagingAccountAccess}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since Open-Xchange v6.18
 */
public final class MailMessagingAccountAccess extends MailMessagingResource implements MessagingAccountAccess {

    /**
     * The folder access.
     */
    private volatile MailMessagingFolderAccess folderAccess;

    /**
     * The message access.
     */
    private volatile MailMessagingMessageAccess messageAccess;

    /**
     * Initializes a new {@link MailMessagingAccountAccess}.
     *
     * @param accountId The account ID
     * @param session The session
     * @throws OXException If initialization fails
     */
    public MailMessagingAccountAccess(final int accountId, final Session session) throws OXException {
        super(accountId, session);
    }

    @Override
    public int getAccountId() {
        return accountId;
    }

    @Override
    public MessagingFolderAccess getFolderAccess() throws OXException {
        if (!mailAccess.isConnected()) {
            throw MessagingExceptionCodes.NOT_CONNECTED.create();
        }
        MailMessagingFolderAccess tmp = folderAccess;
        if (null == tmp) {
            try {
                folderAccess =
                    tmp =
                        new MailMessagingFolderAccess(
                            mailAccess.getFolderStorage(),
                            accountId,
                            mailAccess.getMailConfig().getCapabilities(),
                            session);
            } catch (final OXException e) {
                throw e;
            }
        }
        return tmp;
    }

    @Override
    public MessagingMessageAccess getMessageAccess() throws OXException {
        if (!mailAccess.isConnected()) {
            throw MessagingExceptionCodes.NOT_CONNECTED.create();
        }
        MailMessagingMessageAccess tmp = messageAccess;
        if (null == tmp) {
            try {
                messageAccess =
                    tmp = new MailMessagingMessageAccess(mailAccess.getMessageStorage(), mailAccess.getLogicTools(), accountId, session);
            } catch (final OXException e) {
                throw e;
            }
        }
        return tmp;
    }

    @Override
    public MessagingFolder getRootFolder() throws OXException {
        try {
            return MailMessagingFolderAccess.convert2MessagingFolder(
                mailAccess.getRootFolder(),
                mailAccess.getMailConfig().getCapabilities());
        } catch (final OXException e) {
            throw e;
        }
    }

}
