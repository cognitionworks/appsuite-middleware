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

package com.openexchange.user.json.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.api2.ContactInterfaceFactory;
import com.openexchange.multiple.MultipleHandlerFactoryService;
import com.openexchange.server.osgiservice.RegistryServiceTrackerCustomizer;
import com.openexchange.user.UserService;
import com.openexchange.user.json.multiple.UserMultipleHandlerFactory;
import com.openexchange.user.json.services.ServiceRegistry;

/**
 * {@link Activator} - Activator for user component.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class Activator implements BundleActivator {

    private ServiceRegistration userMultipleService;

    private ServiceTracker httpTracker;

    private ServiceTracker userServiceTracker;

    private ServiceTracker contactInterfaceFactoryTracker;

    /**
     * Initializes a new {@link Activator}.
     */
    public Activator() {
        super();
    }

    public void start(final BundleContext context) throws Exception {
        try {
            /*
             * Register user multiple service
             */
            userMultipleService =
                context.registerService(MultipleHandlerFactoryService.class.getName(), new UserMultipleHandlerFactory(), null);
            /*
             * User service tracker
             */
            userServiceTracker =
                new ServiceTracker(context, UserService.class.getName(), new RegistryServiceTrackerCustomizer<UserService>(
                    context,
                    ServiceRegistry.getInstance(),
                    UserService.class));
            userServiceTracker.open();
            /*
             * Contact interface factory tracker
             */
            contactInterfaceFactoryTracker =
                new ServiceTracker(
                    context,
                    ContactInterfaceFactory.class.getName(),
                    new RegistryServiceTrackerCustomizer<ContactInterfaceFactory>(
                        context,
                        ServiceRegistry.getInstance(),
                        ContactInterfaceFactory.class));
            contactInterfaceFactoryTracker.open();
            /*
             * HTTP service tracker
             */
            httpTracker = new ServiceTracker(context, HttpService.class.getName(), new ServletRegisterer(context));
            httpTracker.open();
        } catch (final Exception e) {
            final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(Activator.class);
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

    public void stop(final BundleContext context) throws Exception {
        try {
            if (null != httpTracker) {
                httpTracker.close();
                httpTracker = null;
            }
            if (null != contactInterfaceFactoryTracker) {
                contactInterfaceFactoryTracker.close();
                contactInterfaceFactoryTracker = null;
            }
            if (null != userServiceTracker) {
                userServiceTracker.close();
                userServiceTracker = null;
            }
            if (null != userMultipleService) {
                userMultipleService.unregister();
                userMultipleService = null;
            }
        } catch (final Exception e) {
            final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(Activator.class);
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

}
