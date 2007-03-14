
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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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
package com.openexchange.groupware.imap;

import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.sessiond.SessionObject;

/**
 * OXUser2IMAPLogin - Maps an user ID to his IMAP login and vice versa
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class OXUser2IMAPLogin {

	private OXUser2IMAPLogin() {
		super();
	}

	public static String getIMAPLogin(final int userId, final Context ctx) throws LdapException {
		final UserStorage us = UserStorage.getInstance(ctx);
		return us.getUser(userId).getImapLogin();
	}

	public static String getLocalIMAPLogin(final SessionObject session) {
		String imapLogin = session.getUserObject().getImapLogin();
		if (imapLogin == null || imapLogin.length() == 0) {
			imapLogin = session.getUserlogin() != null && session.getUserlogin().length() > 0 ? session.getUserlogin()
					: session.getUsername();
		}
		return imapLogin;
	}

	public static int getUserID(final String imapLogin, final Context ctx) throws IMAPException, LdapException {
		if (!IMAPProperties.isSupportsACLs()) {
			return -1;
		}
		final UserStorage us = UserStorage.getInstance(ctx);
		return us.getUserIdByIMAPLogin(imapLogin);
	}

}
