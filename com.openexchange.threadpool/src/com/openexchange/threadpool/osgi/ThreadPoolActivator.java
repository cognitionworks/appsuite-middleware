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

package com.openexchange.threadpool.osgi;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.Constants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import com.openexchange.config.ConfigurationService;
import com.openexchange.management.ManagementService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.session.Session;
import com.openexchange.session.SessionThreadCounter;
import com.openexchange.sessionCount.SessionThreadCounterImpl;
import com.openexchange.sessiond.SessiondEventConstants;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.internal.ThreadPoolProperties;
import com.openexchange.threadpool.internal.ThreadPoolServiceImpl;
import com.openexchange.timer.TimerService;
import com.openexchange.timer.internal.CustomThreadPoolExecutorTimerService;

/**
 * {@link ThreadPoolActivator} - The {@link BundleActivator activator} for thread pool bundle.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ThreadPoolActivator extends HousekeepingActivator {

    public static final AtomicReference<ThreadPoolService> REF_THREAD_POOL = new AtomicReference<ThreadPoolService>();

    public static final AtomicReference<TimerService> REF_TIMER = new AtomicReference<TimerService>();

    private volatile ThreadPoolServiceImpl threadPool;

    /**
     * Initializes a new {@link ThreadPoolActivator}.
     */
    public ThreadPoolActivator() {
        super();
    }

    @Override
    protected void startBundle() throws Exception {
        final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ThreadPoolActivator.class);
        try {
            LOG.info("starting bundle: com.openexchange.threadpool");
            /*
             * Initialize thread pool
             */
            final ConfigurationService confService = getService(ConfigurationService.class);
            final ThreadPoolProperties init = new ThreadPoolProperties().init(confService);
            threadPool = ThreadPoolServiceImpl.newInstance(init);
            if (init.isPrestartAllCoreThreads()) {
                threadPool.prestartAllCoreThreads();
            }
            /*
             * Service trackers
             */
            track(ManagementService.class, new ManagementServiceTrackerCustomizer(context, threadPool));
            /*
             * Register
             */
            REF_THREAD_POOL.set(threadPool);
            registerService(ThreadPoolService.class, threadPool);
            {
                final Dictionary<String, Object> dict = new Hashtable<String, Object>(4);
                dict.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
                dict.put(Constants.SERVICE_DESCRIPTION, "The Open-Xchange ExecutorService");
                dict.put(Constants.SERVICE_VENDOR, "Open-Xchange Inc.");
                registerService(ExecutorService.class, threadPool.getExecutor(), dict);
            }
            final TimerService timerService = new CustomThreadPoolExecutorTimerService(threadPool.getThreadPoolExecutor());
            REF_TIMER.set(timerService);
            registerService(TimerService.class, timerService);
            /*
             * Register SessionThreadCounter service
             */
            final int notifyThreashold = confService.getIntProperty("com.openexchange.session.maxThreadNotifyThreshold", -1);
            final SessionThreadCounterImpl counterImpl = new SessionThreadCounterImpl(notifyThreashold, this);
            registerService(SessionThreadCounter.class, counterImpl);
            SessionThreadCounter.REFERENCE.set(counterImpl);
            /*
             * Event handler for session count events
             */
            {
                final SessionThreadCountEventHandler handler = new SessionThreadCountEventHandler(context, notifyThreashold, counterImpl);
                rememberTracker(handler);
                final Dictionary<String, Object> dict = new Hashtable<String, Object>(1);
                dict.put(EventConstants.EVENT_TOPIC, SessionThreadCounter.EVENT_TOPIC);
                registerService(EventHandler.class, handler, dict);
                track(ManagementService.class, new ManagementServiceTrackerCustomizer2(context, counterImpl, handler));
            }
            /*
             * Event handler for session events
             */
            {
                final EventHandler sessionEventHandler = new EventHandler() {

                    @Override
                    public void handleEvent(final Event event) {
                        final String topic = event.getTopic();
                        if (SessiondEventConstants.TOPIC_REMOVE_DATA.equals(topic)) {
                            @SuppressWarnings("unchecked") final Map<String, Session> container = (Map<String, Session>) event.getProperty(SessiondEventConstants.PROP_CONTAINER);
                            for (final Session session : container.values()) {
                                removeFor(session);
                            }
                        } else if (SessiondEventConstants.TOPIC_REMOVE_SESSION.equals(topic)) {
                            removeFor((Session) event.getProperty(SessiondEventConstants.PROP_SESSION));
                        } else if (SessiondEventConstants.TOPIC_REMOVE_CONTAINER.equals(topic)) {
                            @SuppressWarnings("unchecked") final Map<String, Session> container = (Map<String, Session>) event.getProperty(SessiondEventConstants.PROP_CONTAINER);
                            for (final Session session : container.values()) {
                                removeFor(session);
                            }
                        }
                    }

                    private void removeFor(final Session session) {
                        final SessionThreadCounter threadCounter = SessionThreadCounter.REFERENCE.get();
                        if (null != threadCounter) {
                            threadCounter.remove(session.getSessionID());
                        }
                    }

                };
                final Dictionary<String, Object> dict = new Hashtable<String, Object>(1);
                dict.put(EventConstants.EVENT_TOPIC, SessiondEventConstants.getAllTopics());
                registerService(EventHandler.class, sessionEventHandler, dict);
            }
            /*
             * Open service trackers
             */
            openTrackers();
        } catch (final Exception e) {
            LOG.error("Failed start-up of bundle com.openexchange.threadpool", e);
            throw e;
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ThreadPoolActivator.class);
        try {
            LOG.info("stopping bundle: com.openexchange.threadpool");
            REF_THREAD_POOL.set(null);
            REF_TIMER.set(null);
            cleanUp();
            SessionThreadCounter.REFERENCE.set(null);
            /*
             * Stop thread pool
             */
            final ThreadPoolServiceImpl threadPool = this.threadPool;
            if (null != threadPool) {
                try {
                    threadPool.shutdownNow();
                    threadPool.awaitTermination(10000L);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    this.threadPool = null;
                }
            }
        } catch (final Exception e) {
            LOG.error("Failed shut-down of bundle com.openexchange.threadpool", e);
            throw e;
        }
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ConfigurationService.class };
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ThreadPoolActivator.class);
        LOG.info("Appeared service: {}", clazz.getName());
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ThreadPoolActivator.class);
        LOG.info("Disappeared service: {}", clazz.getName());
    }

}
