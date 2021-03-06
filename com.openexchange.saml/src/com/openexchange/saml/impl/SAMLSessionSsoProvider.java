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

package com.openexchange.saml.impl;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.ajax.login.HashCalculator;
import com.openexchange.ajax.login.LoginConfiguration;
import com.openexchange.ajax.login.LoginTools;
import com.openexchange.exception.OXException;
import com.openexchange.saml.SAMLSessionParameters;
import com.openexchange.saml.tools.SAMLLoginTools;
import com.openexchange.session.Session;
import com.openexchange.session.SessionSsoProvider;
import com.openexchange.sessiond.SessiondService;

/**
 * {@link SAMLSessionSsoProvider}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.10.3
 */
public class SAMLSessionSsoProvider implements SessionSsoProvider {

    private final LoginConfigurationLookup loginConfigLookup;

    private final SessiondService sessiondService;

    /**
     * Initializes a new {@link SAMLSessionSsoProvider}.
     * @param sessiondService
     */
    public SAMLSessionSsoProvider(LoginConfigurationLookup loginConfigLookup, SessiondService sessiondService) {
        super();
        this.loginConfigLookup = loginConfigLookup;
        this.sessiondService = sessiondService;
    }

    @Override
    public boolean isSsoSession(Session session) throws OXException {
        return session != null && "true".equals(session.getParameter(SAMLSessionParameters.AUTHENTICATED));
    }

    @Override
    public boolean skipAutoLoginAttempt(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws OXException {
        LoginConfiguration loginConfiguration = loginConfigLookup.getLoginConfiguration();
        Cookie sessionCookie = SAMLLoginTools.getSessionCookie(httpRequest, loginConfiguration);
        Session session = SAMLLoginTools.getSessionForSessionCookie(sessionCookie, sessiondService);
        if (session == null) {
            return false;
        }

        if (isSsoSession(session)) {
            String hash = HashCalculator.getInstance().getHash(httpRequest, LoginTools.parseUserAgent(httpRequest), LoginTools.parseClient(httpRequest, false, loginConfiguration.getDefaultClient()));
            return SAMLLoginTools.isValidSession(httpRequest, session, hash);
        }
        return false;
    }

}
