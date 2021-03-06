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

package com.openexchange.login.internal;

import java.util.HashMap;
import java.util.Map;
import com.openexchange.authentication.Authenticated;
import com.openexchange.authentication.AuthenticationService;
import com.openexchange.authentication.application.AppAuthenticatorService;
import com.openexchange.authentication.application.AppLoginRequest;
import com.openexchange.authentication.application.RestrictedAuthentication;
import com.openexchange.authentication.service.Authentication;
import com.openexchange.exception.OXException;
import com.openexchange.login.LoginRequest;
import com.openexchange.server.services.ServerServiceRegistry;

/**
 * Uses the normal login authentication method to perform the authentication.
 * 
 * @see AuthenticationService#handleLoginInfo(com.openexchange.authentication.LoginInfo)
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 * @author <a href="mailto:greg.hill@open-xchange.com">Greg Hill</a>
 */
final class NormalLoginMethod implements LoginMethodClosure {

    private final Map<String, Object> properties;
    private final LoginRequest request;

    /**
     * Initializes a new {@link NormalLoginMethod}.
     *
     * @param request The login request
     * @param properties The arbitrary properties; e.g. <code>"headers"</code> or <code>{@link com.openexchange.authentication.Cookie "cookies"}</code>
     */
    NormalLoginMethod(LoginRequest request, Map<String, Object> properties) {
        super();
        this.request = request;
        this.properties = properties;
    }

    @Override
    public Authenticated doAuthentication(final LoginResultImpl retval) throws OXException {
        AppAuthenticatorService appAuthenticator = ServerServiceRegistry.getInstance().getService(AppAuthenticatorService.class);
        if (null != appAuthenticator) {
            AppLoginRequest appLoginRequest = getAppLoginRequest(request);
            if (appAuthenticator.applies(appLoginRequest)) {
                RestrictedAuthentication authentication = appAuthenticator.doAuth(appLoginRequest);
                if (null != authentication) {
                    return authentication;
                }
            }
        }
        return Authentication.login(request.getLogin(), request.getPassword(), properties);
    }

    private static AppLoginRequest getAppLoginRequest(LoginRequest request) {
        return new AppLoginRequest() {

            @Override
            public String getUserAgent() {
                return request.getUserAgent();
            }

            @Override
            public String getPassword() {
                return request.getPassword();
            }

            @Override
            public Map<String, Object> getParameters() {
                Map<String, Object> parameters = new HashMap<String, Object>();
                parameters.put(com.openexchange.login.LoginRequest.class.getName(), request);
                return parameters;
            }

            @Override
            public String getLogin() {
                return request.getLogin();
            }

            @Override
            public String getClientIP() {
                return request.getClientIP();
            }

            @Override
            public String getClient() {
                return request.getClient();
            }
        };
    }

}
