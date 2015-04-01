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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.ajax.login.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.ajax.login.LoginRequestHandler;
import com.openexchange.ajax.login.SSOLogoutHandler;
import com.openexchange.config.ConfigurationService;
import com.openexchange.dispatcher.DispatcherPrefixService;
import com.openexchange.login.LoginRampUpService;
import com.openexchange.login.internal.LoginPerformer;
import com.openexchange.login.internal.format.CompositeLoginFormatter;
import com.openexchange.oauth.provider.OAuthProviderService;
import com.openexchange.oauth.provider.v2.OAuth2ProviderService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.osgi.ServiceSet;
import com.openexchange.osgi.Tools;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.reservation.SessionReservationService;
import com.openexchange.tokenlogin.TokenLoginService;

/**
 * {@link LoginActivator}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class LoginActivator extends HousekeepingActivator {

    public LoginActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ConfigurationService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        final BundleContext context = this.context;
        class ServerServiceRegistryTracker<S> implements ServiceTrackerCustomizer<S, S> {

            @Override
            public S addingService(final ServiceReference<S> reference) {
                final S service = context.getService(reference);
                ServerServiceRegistry.getInstance().addService(service);
                return service;
            }

            @Override
            public void modifiedService(final ServiceReference<S> reference, final S service) {
                // Nothing
            }

            @Override
            public void removedService(final ServiceReference<S> reference, final S service) {
                context.ungetService(reference);
                ServerServiceRegistry.getInstance().removeService(service.getClass());
            }
        }
        track(OAuthProviderService.class, new ServerServiceRegistryTracker<OAuthProviderService>());
        track(OAuth2ProviderService.class, new ServerServiceRegistryTracker<OAuth2ProviderService>());

        ServiceSet<LoginRampUpService> rampUp = new ServiceSet<LoginRampUpService>();
        track(LoginRampUpService.class, rampUp);

        Filter filter = Tools.generateServiceFilter(context,
            ConfigurationService.class,
            HttpService.class,
            DispatcherPrefixService.class,
            LoginRequestHandler.class,
            SSOLogoutHandler.class);
        rememberTracker(new ServiceTracker<Object, Object>(context, filter, new LoginServletRegisterer(context, rampUp)));

        track(TokenLoginService.class, new TokenLoginCustomizer(context));
        track(SessionReservationService.class, new SessionReservationCustomizer(context));
        openTrackers();

        final ConfigurationService configurationService = getService(ConfigurationService.class);
        final String loginFormat = configurationService.getProperty("com.openexchange.ajax.login.formatstring.login");
        final String logoutFormat = configurationService.getProperty("com.openexchange.ajax.login.formatstring.logout");
        LoginPerformer.setLoginFormatter(new CompositeLoginFormatter(loginFormat, logoutFormat));
    }
}
