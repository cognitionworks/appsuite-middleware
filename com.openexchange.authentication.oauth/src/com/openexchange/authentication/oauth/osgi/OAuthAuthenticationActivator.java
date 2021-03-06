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

package com.openexchange.authentication.oauth.osgi;

import com.openexchange.authentication.AuthenticationService;
import com.openexchange.authentication.oauth.http.OAuthAuthenticationHttpClientConfig;
import com.openexchange.authentication.oauth.impl.DefaultOAuthAuthenticationConfig;
import com.openexchange.authentication.oauth.impl.OAuthAuthenticationConfig;
import com.openexchange.authentication.oauth.impl.PasswordGrantAuthentication;
import com.openexchange.authentication.oauth.impl.PasswordGrantAuthenticationFailedHandler;
import com.openexchange.authentication.oauth.impl.PasswordGrantSessionInspector;
import com.openexchange.authentication.oauth.impl.SessionParameters;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.context.ContextService;
import com.openexchange.mail.api.AuthenticationFailedHandler;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.rest.client.httpclient.SpecificHttpClientConfigProvider;
import com.openexchange.session.inspector.SessionInspectorService;
import com.openexchange.session.oauth.SessionOAuthTokenService;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.sessionstorage.SessionStorageParameterNamesProvider;
import com.openexchange.user.UserService;
import com.openexchange.version.VersionService;

/**
 * {@link OAuthAuthenticationActivator}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.10.3
 */
public class OAuthAuthenticationActivator extends HousekeepingActivator {

    /**
     * Initializes a new {@link OAuthAuthenticationActivator}.
     */
    public OAuthAuthenticationActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] {
            LeanConfigurationService.class,
            ContextService.class,
            UserService.class,
            SessiondService.class,
            SessionOAuthTokenService.class,
            ConfigurationService.class
        };
    }

    @Override
    protected void startBundle() throws Exception {
        trackService(VersionService.class);
        trackService(HttpClientService.class);
        openTrackers();

        // Initialize configuration for out-bound HTTP traffic
        registerService(SpecificHttpClientConfigProvider.class, new OAuthAuthenticationHttpClientConfig(this));

        OAuthAuthenticationConfig config = new DefaultOAuthAuthenticationConfig(getService(LeanConfigurationService.class));
        PasswordGrantAuthentication passwordGrantAuthentication = new PasswordGrantAuthentication(config, this);
        registerService(AuthenticationService.class, passwordGrantAuthentication);
        registerService(SessionStorageParameterNamesProvider.class, new SessionParameters());
        PasswordGrantSessionInspector sessionInspector =
            new PasswordGrantSessionInspector(config, this);
        registerService(SessionInspectorService.class, sessionInspector);
        registerService(AuthenticationFailedHandler.class, new PasswordGrantAuthenticationFailedHandler(), 100);
    }

    @Override
    protected void stopBundle() throws Exception {
        HttpClientService httpClientService = getService(HttpClientService.class);
        if (httpClientService != null) {
            httpClientService.destroyHttpClient(OAuthAuthenticationHttpClientConfig.getClientIdOAuthAuthentication());
        }
        super.stopBundle();
    }

}
