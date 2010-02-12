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

package com.openexchange.messaging.facebook.osgi;

import static com.openexchange.messaging.facebook.services.FacebookMessagingServiceRegistry.getServiceRegistry;
import java.util.ArrayList;
import java.util.List;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.config.ConfigurationService;
import com.openexchange.context.ContextService;
import com.openexchange.exceptions.osgi.ComponentRegistration;
import com.openexchange.messaging.MessagingService;
import com.openexchange.messaging.facebook.FacebookConfiguration;
import com.openexchange.messaging.facebook.FacebookConstants;
import com.openexchange.messaging.facebook.FacebookMessagingException;
import com.openexchange.messaging.facebook.FacebookMessagingService;
import com.openexchange.messaging.facebook.exception.FacebookMessagingExceptionFactory;
import com.openexchange.server.osgiservice.DeferredActivator;
import com.openexchange.server.osgiservice.ServiceRegistry;
import com.openexchange.user.UserService;

/**
 * {@link FacebookMessagingActivator}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class FacebookMessagingActivator extends DeferredActivator {

    private List<ServiceTracker> trackers;

    private List<ServiceRegistration> registrations;

    private ComponentRegistration componentRegistration;

    /**
     * Initializes a new {@link FacebookMessagingActivator}.
     */
    public FacebookMessagingActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ConfigurationService.class, ContextService.class, UserService.class };
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        final org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory.getLog(FacebookMessagingActivator.class);
        if (logger.isInfoEnabled()) {
            logger.info("Re-available service: " + clazz.getName());
        }
        getServiceRegistry().addService(clazz, getService(clazz));
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        final org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory.getLog(FacebookMessagingActivator.class);
        if (logger.isWarnEnabled()) {
            logger.warn("Absent service: " + clazz.getName());
        }
        getServiceRegistry().removeService(clazz);
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            /*
             * (Re-)Initialize service registry with available services
             */
            {
                final ServiceRegistry registry = getServiceRegistry();
                registry.clearRegistry();
                final Class<?>[] classes = getNeededServices();
                for (int i = 0; i < classes.length; i++) {
                    final Object service = getService(classes[i]);
                    if (null != service) {
                        registry.addService(classes[i], service);
                    }
                }
            }

            FacebookConstants.init();
            FacebookConfiguration.getInstance().configure(getService(ConfigurationService.class));

            // Do this once we can register more than one app for a given component, otherwise this will not work with the basic messaging
            // bundle.
            /*
             * Register component
             */
            componentRegistration =
                new ComponentRegistration(
                    context,
                    FacebookMessagingException.FACEBOOK_COMPONENT,
                    "com.openexchange.messaging.facebook",
                    FacebookMessagingExceptionFactory.getInstance());

            trackers = new ArrayList<ServiceTracker>();
            for (final ServiceTracker tracker : trackers) {
                tracker.open();
            }

            registrations = new ArrayList<ServiceRegistration>();
            registrations.add(context.registerService(MessagingService.class.getName(), new FacebookMessagingService(), null));

            try {
                // new StartUpTest().test();
            } catch (final Exception e) {
                org.apache.commons.logging.LogFactory.getLog(FacebookMessagingActivator.class).error(e.getMessage(), e);
            }

        } catch (final Exception e) {
            org.apache.commons.logging.LogFactory.getLog(FacebookMessagingActivator.class).error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        try {
            /*
             * Unregister component
             */
            if (null != componentRegistration) {
                componentRegistration.unregister();
                componentRegistration = null;
            }
            if (null != trackers) {
                while (!trackers.isEmpty()) {
                    trackers.remove(0).close();
                }
                trackers = null;
            }
            if (null != registrations) {
                while (!registrations.isEmpty()) {
                    registrations.remove(0).unregister();
                }
                registrations = null;
            }

            FacebookConstants.drop();
            FacebookConfiguration.getInstance().drop();

            /*
             * Clear service registry
             */
            getServiceRegistry().clearRegistry();
        } catch (final Exception e) {
            org.apache.commons.logging.LogFactory.getLog(FacebookMessagingActivator.class).error(e.getMessage(), e);
            throw e;
        }
    }

}
