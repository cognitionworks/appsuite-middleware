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

package com.openexchange.tools.servlet.http;

import java.io.UnsupportedEncodingException;
import com.openexchange.tools.StringCollection;
import com.openexchange.tools.encoding.Base64;

/**
 * {@link Authorization}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public final class Authorization {

    private static final String BASIC_AUTH = "basic";

    private Authorization() {
        super();
    }

    /**
     * Checks if the client sends a correct basic authorization header.
     *
     * @param auth Authorization header.
     * @return <code>true</code> if the client sent a correct authorization header.
     */
    public static boolean checkForBasicAuthorization(final String auth) {
        if (null == auth) {
            return false;
        }
        if (auth.length() <= BASIC_AUTH.length()) {
            return false;
        }
        if (!auth.substring(0, BASIC_AUTH.length()).equalsIgnoreCase(BASIC_AUTH)) {
            return false;
        }
        return true;
    }

    public static class Credentials {
        private final String login;
        private final String password;
        Credentials(String login, String password) {
            super();
            this.login = login;
            this.password = password;
        }

        public String getLogin() {
            return login;
        }

        public String getPassword() {
            return password;
        }
    }

    public static Credentials decode(String auth) throws UnsupportedEncodingException {
        final byte[] decoded = Base64.decode(auth.substring(6));
        final String userpass = new String(decoded, "UTF-8").trim();
        final int delimiter = userpass.indexOf(':');
        String login = "";
        String pass = "";
        if (-1 != delimiter) {
            login = userpass.substring(0, delimiter);
            pass = userpass.substring(delimiter + 1);
        }
        return new Credentials(login, pass);
    }

    /**
     * Checks if the login contains only valid values.
     *
     * @param pass password of the user
     * @return false if the login contains illegal values.
     */
    public static boolean checkLogin(final String pass) {
        // check if the user wants to login without password.
        // ldap bind doesn't fail with empty password. so check it here.
        return (pass != null && !StringCollection.isEmpty(pass));
    }

}
