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

package com.openexchange.push.imapidle.osgi;

import static com.openexchange.push.imapidle.services.ImapIdleServiceRegistry.getServiceRegistry;
import java.util.ArrayList;
import java.util.List;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.config.ConfigurationService;
import com.openexchange.context.ContextService;
import com.openexchange.groupware.delete.DeleteListener;
import com.openexchange.imap.notify.IMAPNotifierRegistryService;
import com.openexchange.mail.service.MailService;
import com.openexchange.mailaccount.MailAccountDeleteListener;
import com.openexchange.push.PushManagerService;
import com.openexchange.push.imapidle.ImapIdleDeleteListener;
import com.openexchange.push.imapidle.ImapIdleMailAccountDeleteListener;
import com.openexchange.push.imapidle.ImapIdlePushListener;
import com.openexchange.push.imapidle.ImapIdlePushListenerRegistry;
import com.openexchange.push.imapidle.ImapIdlePushManagerService;
import com.openexchange.push.imapidle.services.ImapIdleServiceRegistry;
import com.openexchange.server.osgiservice.DeferredActivator;
import com.openexchange.server.osgiservice.RegistryServiceTrackerCustomizer;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.threadpool.ThreadPoolService;

/**
 * {@link ImapIdleActivator} - The IMAP IDLE activator.
 */
public final class ImapIdleActivator extends DeferredActivator {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ImapIdleActivator.class);

    private List<ServiceRegistration> serviceRegistrations;

    private String folder;

    private int errordelay;

    private List<ServiceTracker> serviceTrackers;

    /**
     * Initializes a new {@link ImapIdleActivator}.
     */
    public ImapIdleActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] {
            MailService.class, EventAdmin.class, ConfigurationService.class, ContextService.class, ThreadPoolService.class };
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Re-available service: " + clazz.getName());
        }
        getServiceRegistry().addService(clazz, getService(clazz));
        if (ThreadPoolService.class == clazz) {
            ImapIdlePushListenerRegistry.getInstance().openAll();
        }
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        if (LOG.isWarnEnabled()) {
            LOG.warn("Absent service: " + clazz.getName());
        }
        if (ThreadPoolService.class == clazz) {
            ImapIdlePushListenerRegistry.getInstance().closeAll();
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
                final ImapIdleServiceRegistry registry = getServiceRegistry();
                registry.clearRegistry();
                final Class<?>[] classes = getNeededServices();
                for (int i = 0; i < classes.length; i++) {
                    final Object service = getService(classes[i]);
                    if (null != service) {
                        registry.addService(classes[i], service);
                    }
                }
            }
            /*
             * Initialize & open tracker for SessionD service
             */
            serviceTrackers = new ArrayList<ServiceTracker>(2);
            {
                final ServiceTrackerCustomizer trackerCustomizer =
                    new RegistryServiceTrackerCustomizer<SessiondService>(context, getServiceRegistry(), SessiondService.class);
                serviceTrackers.add(new ServiceTracker(context, SessiondService.class.getName(), trackerCustomizer));
            }
            serviceTrackers.add(new ServiceTracker(context, IMAPNotifierRegistryService.class.getName(), new IMAPNotifierTracker(context)));
            for (final ServiceTracker tracker : serviceTrackers) {
                tracker.open();
            }
            /*
             * Read configuration
             */
            final ConfigurationService configurationService = getService(ConfigurationService.class);
            folder = "INBOX";
            {
                final String tmp = configurationService.getProperty("com.openexchange.push.imapidle.folder");
                if (null != tmp) {
                    folder = tmp.trim();
                }
            }

            errordelay = configurationService.getIntProperty("com.openexchange.push.imapidle.errordelay", 1000);

            final boolean debug = configurationService.getBoolProperty("com.openexchange.push.imapidle.debug", true);
            ImapIdlePushListener.setFolder(folder);
            ImapIdlePushListener.setDebugEnabled(debug);

            /*
             * Start-up
             */
            /*
             * Register push manager
             */
            serviceRegistrations = new ArrayList<ServiceRegistration>(4);
            serviceRegistrations.add(context.registerService(PushManagerService.class.getName(), new ImapIdlePushManagerService(), null));
            serviceRegistrations.add(context.registerService(
                MailAccountDeleteListener.class.getName(),
                new ImapIdleMailAccountDeleteListener(),
                null));
            serviceRegistrations.add(context.registerService(DeleteListener.class.getName(), new ImapIdleDeleteListener(), null));

            LOG.info("com.openexchange.push.imapidle bundle started");
            LOG.info(debug ? " debugging enabled" : "debugging disabled");
            LOG.info("Foldername: " + folder);
            LOG.info("Error delay: " + errordelay + "");
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        try {
            /*
             * Unregister push manager
             */
            if (null != serviceRegistrations) {
                while (!serviceRegistrations.isEmpty()) {
                    serviceRegistrations.remove(0).unregister();
                }
                serviceRegistrations = null;
            }
            /*
             * Close tracker
             */
            if (null != serviceTrackers) {
                while (!serviceTrackers.isEmpty()) {
                    serviceTrackers.remove(0).close();
                }
                serviceTrackers = null;
            }
            /*
             * Clear all running listeners
             */
            ImapIdlePushListenerRegistry.getInstance().purgeAllPushListener();
            /*
             * Shut down
             */
            ImapIdlePushListener.setFolder(null);
            ImapIdlePushListenerRegistry.getInstance().clear();
            /*
             * Clear service registry
             */
            getServiceRegistry().clearRegistry();
            /*
             * Reset
             */
            folder = null;
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

}
