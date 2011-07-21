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

package com.openexchange.messaging.mail.osgi;

import static com.openexchange.messaging.mail.services.MailMessagingServiceRegistry.getServiceRegistry;
import java.util.ArrayList;
import java.util.List;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.context.ContextService;
import com.openexchange.exceptions.osgi.ComponentRegistration;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.messaging.MessagingService;
import com.openexchange.messaging.mail.MailMessageService;
import com.openexchange.messaging.mail.MailMessagingException;
import com.openexchange.messaging.mail.exceptions.MailMessagingExceptionFactory;
import com.openexchange.server.osgiservice.DeferredActivator;
import com.openexchange.server.osgiservice.ServiceRegistry;
import com.openexchange.timer.ScheduledTimerTask;

/**
 * {@link MailMessagingActivator}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since Open-Xchange v6.18
 */
public final class MailMessagingActivator extends DeferredActivator {

    private List<ServiceTracker> trackers;

    private List<ServiceRegistration> registrations;

    private ComponentRegistration componentRegistration;

    private ScheduledTimerTask scheduledTimerTask;

    /**
     * Initializes a new {@link MailMessagingActivator}.
     */
    public MailMessagingActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ContextService.class, MailAccountStorageService.class };
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        final org.apache.commons.logging.Log logger = com.openexchange.log.Log.valueOf(com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(MailMessagingActivator.class)));
        if (logger.isInfoEnabled()) {
            logger.info("Re-available service: " + clazz.getName());
        }
        final Object service = getService(clazz);
        getServiceRegistry().addService(clazz, service);
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        final org.apache.commons.logging.Log logger = com.openexchange.log.Log.valueOf(com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(MailMessagingActivator.class)));
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
                for (final Class<?> classe : classes) {
                    final Object service = getService(classe);
                    if (null != service) {
                        registry.addService(classe, service);
                    }
                }
            }
            /*
             * Register component
             */
            componentRegistration =
                new ComponentRegistration(
                    context,
                    MailMessagingException.MAIL_COMPONENT,
                    "com.openexchange.messaging.mail",
                    MailMessagingExceptionFactory.getInstance());

            trackers = new ArrayList<ServiceTracker>();
            for (final ServiceTracker tracker : trackers) {
                tracker.open();
            }
            /*
             * Register services
             */
            registrations = new ArrayList<ServiceRegistration>(2);
            registrations.add(context.registerService(MessagingService.class.getName(), MailMessageService.newInstance(), null));
        } catch (final Exception e) {
            com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(MailMessagingActivator.class)).error(e.getMessage(), e);
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
            /*
             * Clear service registry
             */
            getServiceRegistry().clearRegistry();
        } catch (final Exception e) {
            com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(MailMessagingActivator.class)).error(e.getMessage(), e);
            throw e;
        }
    }

}
