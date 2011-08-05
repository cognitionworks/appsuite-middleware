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

package com.openexchange.config.cascade.osgi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.config.cascade.ConfigProviderService;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.cascade.impl.ConfigCascade;
import com.openexchange.exception.OXException;
import com.openexchange.server.osgiservice.HousekeepingActivator;
import com.openexchange.tools.strings.StringParser;


/**
 * {@link ConfigCascadeActivator}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class ConfigCascadeActivator extends HousekeepingActivator{

    // private static final Class<?>[] NEEDED = {ConfigProviderService.class, StringParser.class};

    static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(ConfigCascadeActivator.class));

    private boolean configured = false;

    private ConfigCascade configCascade;

    // private final int INFINITY = 10;

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[0];
    }

    @Override
    protected void startBundle() throws Exception {
        configCascade = new ConfigCascade();

        final ServiceTracker stringParsers = track(StringParser.class);

        configCascade.setStringParser(new StringParser() {

            @Override
            public <T> T parse(final String s, final Class<T> t) {
                final Object service = stringParsers.getService();
                if(service == null) {
                    LOG.fatal("Could not find suitable string parser in OSGi system");
                    return null;
                }
                final StringParser parser = (StringParser) service;
                return parser.parse(s, t);
            }

        });

        final ServiceTracker serverProviders = track(createFilter("server"), new ServiceTrackerCustomizer() {

            @Override
            public Object addingService(final ServiceReference reference) {
                final ConfigProviderService provider = (ConfigProviderService) context.getService(reference);
                if (isServerProvider(reference)) {
                    final String scopes = getScopes(provider);
                    configure(scopes, configCascade);
                    configCascade.setProvider("server", provider);
                    registerService(ConfigViewFactory.class, configCascade);
                }
                return provider;
            }

            @Override
            public void modifiedService(final ServiceReference reference, final Object service) {
                // IGNORE
            }

            @Override
            public void removedService(final ServiceReference reference, final Object service) {

            }

        });


        openTrackers();



    }


    @Override
    protected void stopBundle() throws Exception {
        super.stopBundle();
    }

    boolean isServerProvider(final ServiceReference reference) {
        final Object scope = reference.getProperty("scope");
        return scope != null && scope.equals("server");
    }

    String getScopes(final ConfigProviderService config) {
        try {
            return config.get("com.openexchange.config.cascade.scopes", ConfigProviderService.NO_CONTEXT, ConfigProviderService.NO_USER).get();
        } catch (final OXException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    void configure(String scopes, final ConfigCascade cascade) {
        if(configured) {
            return;
        }
        if (scopes == null) {
            scopes = "user, context, server";
        }
        configured = true;

        final String[] searchPath = scopes.split("\\s*,\\s*");
        cascade.setSearchPath(searchPath);

        for (final String scope : searchPath) {
            if(scope.equals("server")) {
                continue;
            }

            final ServiceTracker tracker = track(createFilter(scope));
            cascade.setProvider(scope, new TrackingProvider(tracker));
            tracker.open();
        }
    }

    Filter createFilter(final String scope) {
        try {
            return context.createFilter("(& (objectclass="+ConfigProviderService.class.getName()+") (scope="+scope+"))");
        } catch (final InvalidSyntaxException e) {
            LOG.fatal(e.getMessage(), e);
        }
        return null;
    }

}
