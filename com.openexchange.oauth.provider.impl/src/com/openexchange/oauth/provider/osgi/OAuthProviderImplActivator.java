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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.oauth.provider.osgi;

import org.osgi.service.http.HttpService;
import com.openexchange.authentication.AuthenticationService;
import com.openexchange.config.ConfigurationService;
import com.openexchange.context.ContextService;
import com.openexchange.crypto.CryptoService;
import com.openexchange.database.DatabaseService;
import com.openexchange.dispatcher.DispatcherPrefixService;
import com.openexchange.oauth.provider.OAuthProviderService;
import com.openexchange.oauth.provider.internal.InMemoryOAuth2ProviderService;
import com.openexchange.oauth.provider.internal.OAuthProviderServiceLookup;
import com.openexchange.oauth.provider.servlets.AuthServlet;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.user.UserService;

/**
 * {@link OAuthProviderImplActivator} - The activator for OAuth provider implementation bundle.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class OAuthProviderImplActivator extends HousekeepingActivator {

    private static final String PATH_PREFIX = "oauth2/";

    /**
     * Initializes a new {@link OAuthProviderImplActivator}.
     */
    public OAuthProviderImplActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { DatabaseService.class, ConfigurationService.class, AuthenticationService.class, ContextService.class, UserService.class, CryptoService.class, HttpService.class, DispatcherPrefixService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        OAuthProviderServiceLookup.set(this);
        OAuthProviderService oauth2ProviderService = new InMemoryOAuth2ProviderService();
        registerService(OAuthProviderService.class, oauth2ProviderService);
        addService(OAuthProviderService.class, oauth2ProviderService);

        String prefix = getService(DispatcherPrefixService.class).getPrefix();
        getService(HttpService.class).registerServlet(prefix + PATH_PREFIX + AuthServlet.PATH, new AuthServlet(), null, null);

        openTrackers();
    }

    @Override
    protected void stopBundle() throws Exception {
        OAuthProviderServiceLookup.set(null);
        super.stopBundle();
    }

}
