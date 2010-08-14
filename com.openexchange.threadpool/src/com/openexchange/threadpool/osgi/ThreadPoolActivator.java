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

package com.openexchange.threadpool.osgi;

import java.util.ArrayList;
import java.util.List;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.config.ConfigurationService;
import com.openexchange.management.ManagementService;
import com.openexchange.server.osgiservice.DeferredActivator;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.internal.QueueProvider;
import com.openexchange.threadpool.internal.ThreadPoolProperties;
import com.openexchange.threadpool.internal.ThreadPoolServiceImpl;
import com.openexchange.timer.TimerService;
import com.openexchange.timer.internal.CustomThreadPoolExecutorTimerService;

/**
 * {@link ThreadPoolActivator} - The {@link BundleActivator activator} for thread pool bundle.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ThreadPoolActivator extends DeferredActivator {

    private List<ServiceRegistration> registrations;

    private List<ServiceTracker> trackers;

    private ThreadPoolServiceImpl threadPool;

    /**
     * Initializes a new {@link ThreadPoolActivator}.
     */
    public ThreadPoolActivator() {
        super();
    }

    @Override
    protected void startBundle() throws Exception {
        final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ThreadPoolActivator.class);
        try {
            if (LOG.isInfoEnabled()) {
                LOG.info("starting bundle: com.openexchange.threadpool");
            }
            /*
             * Proper synchronous queue
             */
            String property = System.getProperty("java.specification.version");
            if (null == property) {
                property = System.getProperty("java.runtime.version");
                if (null == property) {
                    // JRE not detectable, use fallback
                    QueueProvider.initInstance(false);
                } else {
                    // "java.runtime.version=1.6.0_0-b14" OR "java.runtime.version=1.5.0_18-b02"
                    QueueProvider.initInstance(!property.startsWith("1.5"));
                }
            } else {
                // "java.specification.version=1.5" OR "java.specification.version=1.6"
                QueueProvider.initInstance("1.5".compareTo(property) < 0);
            }
            /*
             * Initialize thread pool
             */
            final ThreadPoolProperties init = new ThreadPoolProperties().init(getService(ConfigurationService.class));
            threadPool = ThreadPoolServiceImpl.newInstance(init);
            if (init.isPrestartAllCoreThreads()) {
                threadPool.prestartAllCoreThreads();
            }
            /*
             * Service trackers
             */
            trackers = new ArrayList<ServiceTracker>(1);
            trackers.add(new ServiceTracker(context, ManagementService.class.getName(), new ManagementServiceTrackerCustomizer(
                context,
                threadPool)));
            for (final ServiceTracker tracker : trackers) {
                tracker.open();
            }
            /*
             * Register
             */
            registrations = new ArrayList<ServiceRegistration>(2);
            registrations.add(context.registerService(ThreadPoolService.class.getName(), threadPool, null));
            registrations.add(context.registerService(TimerService.class.getName(), new CustomThreadPoolExecutorTimerService(
                threadPool.getThreadPoolExecutor()), null));
        } catch (final Exception e) {
            LOG.error("Failed start-up of bundle com.openexchange.threadpool: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ThreadPoolActivator.class);
        try {
            if (LOG.isInfoEnabled()) {
                LOG.info("stopping bundle: com.openexchange.threadpool");
            }
            /*
             * Unregister
             */
            if (null != registrations) {
                for (final ServiceRegistration registration : registrations) {
                    registration.unregister();
                }
                registrations.clear();
                registrations = null;
            }
            /*
             * Close trackers
             */
            if (null != trackers) {
                for (final ServiceTracker tracker : trackers) {
                    tracker.close();
                }
                trackers.clear();
                trackers = null;
            }
            /*
             * Stop thread pool
             */
            if (null != threadPool) {
                try {
                    threadPool.shutdownNow();
                    threadPool.awaitTermination(10000L);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    threadPool = null;
                }
            }
            /*
             * Drop queue provider
             */
            QueueProvider.releaseInstance();
        } catch (final Exception e) {
            LOG.error("Failed shut-down of bundle com.openexchange.threadpool: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ConfigurationService.class };
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ThreadPoolActivator.class);
        if (LOG.isInfoEnabled()) {
            LOG.info("Appeared service: " + clazz.getName());
        }
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ThreadPoolActivator.class);
        if (LOG.isInfoEnabled()) {
            LOG.info("Disappeared service: " + clazz.getName());
        }
    }

}
