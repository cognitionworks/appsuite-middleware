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

package com.openexchange.mailaccount.internal;

import java.security.GeneralSecurityException;
import com.openexchange.exception.OXException;
import com.openexchange.mail.utils.MailPasswordUtil;
import com.openexchange.mailaccount.MailAccountExceptionCodes;
import com.openexchange.secret.Decrypter;
import com.openexchange.session.Session;

/**
 * {@link GenericProperty}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class GenericProperty implements Decrypter {

    /** The account identifier */
    public final int accountId;

    /** The associated session */
    public final Session session;

    /** The login identifier */
    public final String login;

    /** The server name */
    public final String server;

    /**
     * Initializes a new {@link GenericProperty}.
     */
    public GenericProperty(final int accountId, final Session session, final String login, final String server) {
        super();
        this.accountId = accountId;
        this.session = session;
        this.login = login;
        this.server = server;
    }

    @Override
    public String getDecrypted(final Session session, final String encrypted) throws OXException {
        if (null == encrypted || encrypted.length() == 0) {
            // Set to empty string
            return "";
        }
        // Decrypt mail account's password using session password
        try {
            return MailPasswordUtil.decrypt(encrypted, session.getPassword());
        } catch (GeneralSecurityException e) {
            throw MailAccountExceptionCodes.PASSWORD_DECRYPTION_FAILED.create(e, login, server, Integer.valueOf(session.getUserId()), Integer.valueOf(session.getContextId()));
        }
    }

}
