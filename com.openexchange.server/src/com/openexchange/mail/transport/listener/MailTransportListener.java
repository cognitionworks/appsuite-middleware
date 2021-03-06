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

package com.openexchange.mail.transport.listener;

import javax.mail.Address;
import javax.mail.internet.MimeMessage;
import com.openexchange.exception.OXException;
import com.openexchange.mail.dataobjects.SecuritySettings;
import com.openexchange.session.Session;


/**
 * {@link MailTransportListener} - A listener for message transport.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.0
 */
public interface MailTransportListener {

    /**
     * Checks if specified security settings are orderly handled.
     *
     * @param securitySettings The security settings to consider
     * @param session The associated session
     * @return <code>true</code> if handled; otherwise <code>false</code>
     * @throws OXException If check fails unexpectedly
     */
    boolean checkSettings(SecuritySettings securitySettings, Session session) throws OXException;

    /**
     * Called before a message transport takes place.
     *
     * @param message The message about to send
     * @param recipients An array of recipients
     * @param securitySettings The optional security settings to consider or <code>null</code>
     * @param session The associated session
     * @return The processing result
     * @throws OXException If processing the message fails
     */
    Result onBeforeMessageTransport(MimeMessage message, Address[] recipients, SecuritySettings securitySettings, Session session) throws OXException;

    /**
     * Called after a message transport took place.
     *
     * @param message The sent message
     * @param exception The possible exception that occurred while attempting to transport the message; otherwise <code>null</code>
     * @param session The associated session
     * @return The processing result
     * @throws OXException If processing the sent message fails
     */
    void onAfterMessageTransport(MimeMessage message, Exception exception, Session session) throws OXException;

}
