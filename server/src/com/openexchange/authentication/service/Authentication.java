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

package com.openexchange.authentication.service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.authentication.Authenticated;
import com.openexchange.authentication.AuthenticationService;
import com.openexchange.authentication.LoginInfo;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceExceptionCode;

/**
 * Provides a static method for the login servlet to do the authentication.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class Authentication {

    /**
     * Handles the reference to the authentication service.
     */
    private static final AtomicReference<AuthenticationService> SERVICE_REF = new AtomicReference<AuthenticationService>();

    /**
     * Default constructor.
     */
    private Authentication() {
        super();
    }

    /**
     * Performs a login using an authentication service.
     * @param login entered login.
     * @param pass entered password.
     * @return a string array with two elements in which the first contains the
     * login info for the context and the second contains the login info for the
     * user.
     * @throws OXException if something with the login info is wrong.
     * @throws OXException if the authentication service is not available.
     */
    public static Authenticated login(final String login, final String pass)
        throws OXException {
        return login(login, pass, Collections.<String, Object> emptyMap());
    }

    /**
     * Performs a login using an authentication service.
     * @param login entered login.
     * @param pass entered password.
     * @param properties The optional properties
     * @return a string array with two elements in which the first contains the
     * login info for the context and the second contains the login info for the
     * user.
     * @throws OXException if something with the login info is wrong.
     * @throws OXException if the authentication service is not available.
     */
    public static Authenticated login(final String login, final String pass, final Map<String, Object> properties)
        throws OXException {
        final AuthenticationService auth = SERVICE_REF.get();
        if (null == auth) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create( AuthenticationService.class.getName());
        }
        return auth.handleLoginInfo(new LoginInfo() {
            public String getPassword() {
                return pass;
            }
            public String getUsername() {
                return login;
            }
            public Map<String, Object> getProperties() {
                return properties;
            }
        });
    }

    /**
     * @return the service
     */
    public static AuthenticationService getService() {
        return SERVICE_REF.get();
    }

    /**
     * @param service the service to set
     */
    public static boolean setService(final AuthenticationService service) {
        return SERVICE_REF.compareAndSet(null, service);
    }

    /**
     * @param service the service to set
     */
    public static boolean dropService(final AuthenticationService service) {
        return SERVICE_REF.compareAndSet(service, null);
    }

}
