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

package com.openexchange.rest.services.osgi;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.eclipsesource.jaxrs.publisher.ApplicationConfiguration;
import com.openexchange.config.ConfigurationService;
import com.openexchange.rest.services.jersey.JSONReaderWriter;
import com.openexchange.rest.services.jersey.JerseyConfiguration;
import com.openexchange.rest.services.jersey.OXExceptionMapper;
import com.openexchange.rest.services.security.AuthenticationFilter;

/**
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.6.1
 */
public class Activator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> cmTracker;

    private ServiceTracker<ConfigurationService, ConfigurationService> csTracker;

    private AtomicBoolean authRegistered = new AtomicBoolean();

    public Activator() {
        super();
    }

    @Override
    public void start(final BundleContext context) throws Exception {
        cmTracker = new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(context, ConfigurationAdmin.class, null) {

            @Override
            public ConfigurationAdmin addingService(ServiceReference<ConfigurationAdmin> reference) {
                ConfigurationAdmin service = super.addingService(reference);
                if (service != null) {
                    try {
                        Configuration configuration = service.getConfiguration("com.eclipsesource.jaxrs.connector", null);
                        Dictionary<String, Object> properties = configuration.getProperties();
                        if (properties == null) {
                            properties = new Hashtable<String, Object>(1);
                        }
                        properties.put("root", "/preliminary");
                        configuration.update(properties);
                    } catch (IOException e) {
                        LOG.error("Could not set root path for jersey servlet. REST API will not be available!", e);
                    }
                }
                return service;
            }
        };
        cmTracker.open();

        csTracker = new ServiceTracker<ConfigurationService, ConfigurationService>(context, ConfigurationService.class, null) {

            @Override
            public ConfigurationService addingService(ServiceReference<ConfigurationService> reference) {
                ConfigurationService service = super.addingService(reference);
                if (service != null && authRegistered.compareAndSet(false, true)) {
                    String authLogin = service.getProperty("com.openexchange.rest.services.basic-auth.login");
                    String authPassword = service.getProperty("com.openexchange.rest.services.basic-auth.password");
                    context.registerService(AuthenticationFilter.class, new AuthenticationFilter(authLogin, authPassword), null);
                }
                return service;
            }
        };
        csTracker.open();

        context.registerService(JSONReaderWriter.class, new JSONReaderWriter(), null);
        context.registerService(OXExceptionMapper.class, new OXExceptionMapper(), null);
        context.registerService(ApplicationConfiguration.class, new JerseyConfiguration(), null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        cmTracker.close();
        csTracker.close();
    }

}
