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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.osgi;

import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.server.ServiceLookup;

/**
 * A {@link HousekeepingActivator} helps with housekeeping tasks like remembering service trackers or service registrations and cleaning
 * them up later.
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class HousekeepingActivator extends DeferredActivator {

    /**
     * Puts/removes tracked service from activator's service look-up as the appear/disappear.
     */
    private static final class ServiceLookupTrackerCustomizer<S> implements ServiceTrackerCustomizer<S, S> {

        private final Class<S> clazz;
        private final HousekeepingActivator activator;
        private final BundleContext context;

        /**
         * Initializes a new {@link ServiceTrackerCustomizerImplementation}.
         *
         * @param clazz The service's class to look-up
         * @param activator The activator
         * @param context The bundle context
         */
        protected ServiceLookupTrackerCustomizer(final Class<S> clazz, final HousekeepingActivator activator, final BundleContext context) {
            super();
            this.clazz = clazz;
            this.activator = activator;
            this.context = context;
        }

        @Override
        public S addingService(final ServiceReference<S> reference) {
            final S service = context.getService(reference);
            activator.addService(clazz, service);
            return service;
        }

        @Override
        public void modifiedService(final ServiceReference<S> reference, final S service) {
            // Ignore
        }

        @Override
        public void removedService(final ServiceReference<S> reference, final S service) {
            activator.removeService(clazz);
            context.ungetService(reference);
        }
    }

    /**
     * Delegates tracker events to specified {@link SimpleRegistryListener} instance.
     */
    private static final class SimpleRegistryListenerTrackerCustomizer<S> implements ServiceTrackerCustomizer<S, S> {

        private final SimpleRegistryListener<S> listener;
        private final BundleContext context;

        /**
         * Initializes a new {@link SimpleRegistryListenerTrackerCustomizer}.
         *
         * @param listener The {@link SimpleRegistryListener} instance to delegate to
         * @param context The bundle context
         */
        protected SimpleRegistryListenerTrackerCustomizer(final SimpleRegistryListener<S> listener, final BundleContext context) {
            super();
            this.listener = listener;
            this.context = context;
        }

        @Override
        public S addingService(final ServiceReference<S> serviceReference) {
            final S service = context.getService(serviceReference);
            try {
                listener.added(serviceReference, service);
                return service;
            } catch (final Exception e) {
                context.ungetService(serviceReference);
                return null;
            }
        }

        @Override
        public void modifiedService(final ServiceReference<S> serviceReference, final S service) {
            // Don't care
        }

        @Override
        public void removedService(final ServiceReference<S> serviceReference, final S service) {
            try {
                listener.removed(serviceReference, service);
            } finally {
                context.ungetService(serviceReference);
            }
        }
    }

    private final List<ServiceTracker<?, ?>> serviceTrackers;

    private final List<ServiceRegistration<?>> serviceRegistrations;

    /**
     * Initializes a new {@link HousekeepingActivator}.
     */
    protected HousekeepingActivator() {
        super();
        serviceTrackers = new LinkedList<ServiceTracker<?, ?>>();
        serviceRegistrations = new LinkedList<ServiceRegistration<?>>();
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        // Override if needed
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        // Override if needed
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        /*
         * Invoking ServiceTracker.open() more than once is a no-op, therefore it can be safely called from here.
         */
        if (!serviceTrackers.isEmpty()) {
            openTrackers();
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        cleanUp();
    }

    /**
     * Checks if this activator has at least one service registered.
     *
     * @return <code>true</code> if this activator has at least one service registered; otherwise <code>false</code>
     */
    protected boolean hasRegisteredServices() {
        return !serviceRegistrations.isEmpty();
    }

    /**
     * Registers specified service with the specified properties under the specified class.
     *
     * @param clazz The service's class
     * @param service The service reference
     * @param properties The service's properties
     */
    protected <S> void registerService(final Class<S> clazz, final S service, final Dictionary<String, ?> properties) {
        serviceRegistrations.add(context.registerService(clazz.getName(), service, properties));
    }

       /**
     * Registers specified service under the specified class.
     *
     * @param clazz The service's class
     * @param service The service reference
     */
    protected <S> void registerService(final Class<S> clazz, final S service) {
        registerService(clazz, service, null);
    }
    
    /**
     * Registers specified Service or {@link org.osgi.framework.ServiceFactory} with the specified properties under the specified classname
     * @param className The service's class name
     * @param service The service reference
     * @param properties The service's properties
     */
    protected <S> void registerService (String className, Object service, Dictionary<String, ?> properties) {
        serviceRegistrations.add(context.registerService(className, service, properties));
    }
    
    /**
     * Registers specified Service or {@link org.osgi.framework.ServiceFactory} under the specified class name
     * @param className The service's class name
     * @param service The service reference
     */
    protected <S> void registerService (String className, Object service) {
        serviceRegistrations.add(context.registerService(className, service, null));
    }

    /**
     * Adds specified service tracker to this activator. Thus it is automatically closed and removed by {@link #cleanUp()}.
     * <br>
     * <div style="margin-left: 0.1in; margin-right: 0.5in; background-color:#FFDDDD;">
     * <p>
     * <b>NOTE</b>: Please {@link #openTrackers() open} trackers.
     * </p>
     * </div>
     *
     * @param tracker The service tracker
     */
    protected void rememberTracker(final ServiceTracker<?, ?> tracker) {
        serviceTrackers.add(tracker);
    }

    /**
     * Removes specified service tracker from this activator.
     * <br>
     * <div style="margin-left: 0.1in; margin-right: 0.5in; background-color:#FFDDDD;">
     * <p>
     * <b>NOTE</b>: Please {@link ServiceTracker#close() close} tracker if it has already been started.
     * </p>
     * </div>
     *
     * @param tracker The service tracker
     */
    protected void forgetTracker(final ServiceTracker<?, ?> tracker) {
        serviceTrackers.remove(tracker);
    }

    /**
     * Creates and remembers a new {@link ServiceTracker}. The tracked service is automatically {@link #addService(Class, Object) added to}/
     * {@link #removeService(Class) removed} from tracked services and thus available/disappearing when using this activator as
     * {@link ServiceLookup service look-up}.
     * <p>
     * <b>NOTE</b>: Don't forget to open tracker(s) with {@link #openTrackers()}.
     *
     * @param clazz The class of the tracked service
     * @return The newly created {@link ServiceTracker} instance
     */
    protected <S> ServiceTracker<S, S> trackService(final Class<S> clazz) {
        final ServiceTracker<S, S> tracker = new ServiceTracker<S, S>(context, clazz, new ServiceLookupTrackerCustomizer<S>(clazz, this, context));
        rememberTracker(tracker);
        return tracker;
    }

    /**
     * Creates and remembers a new {@link ServiceTracker} instance parameterized with given customizer.
     * <p>
     * <b>NOTE</b>: Don't forget to open tracker(s) with {@link #openTrackers()}.
     *
     * @param clazz The class of the tracked service
     * @param customizer The customizer applied to newly created {@link ServiceTracker} instance
     * @return The newly created {@link ServiceTracker} instance
     */
    protected <S> ServiceTracker<S, S> track(final Class<S> clazz, final ServiceTrackerCustomizer<S, S> customizer) {
        final ServiceTracker<S, S> tracker = new ServiceTracker<S, S>(context, clazz, customizer);
        rememberTracker(tracker);
        return tracker;
    }

    /**
     * Creates and remembers a new {@link ServiceTracker} instance parameterized with given customizer.
     * <p>
     * <b>NOTE</b>: Don't forget to open tracker(s) with {@link #openTrackers()}.
     *
     * @param filter The tracker's filter
     * @param customizer The customizer applied to newly created {@link ServiceTracker} instance
     * @return The newly created {@link ServiceTracker} instance
     */
    protected <S> ServiceTracker<S, S> track(final Filter filter, final ServiceTrackerCustomizer<S, S> customizer) {
        final ServiceTracker<S, S> tracker = new ServiceTracker<S, S>(context, filter, customizer);
        rememberTracker(tracker);
        return tracker;
    }

    /**
     * Creates and remembers a new {@link ServiceTracker} instance for specified service's class.
     * <p>
     * <b>NOTE</b>: Don't forget to open tracker(s) with {@link #openTrackers()}.
     *
     * @param clazz The service's class
     * @return The newly created {@link ServiceTracker} instance
     */
    protected <S> ServiceTracker<S, S> track(final Class<S> clazz) {
        return track(clazz, (ServiceTrackerCustomizer<S, S>) null);
    }

    /**
     * Creates and remembers a new {@link ServiceTracker} instance for specified filter.
     * <p>
     * <b>NOTE</b>: Don't forget to open tracker(s) with {@link #openTrackers()}.
     *
     * @param filter The filter to apply
     * @return The newly created {@link ServiceTracker} instance
     */
    protected <S> ServiceTracker<S, S> track(final Filter filter) {
        return track(filter, (ServiceTrackerCustomizer<S, S>) null);
    }

    /**
     * Creates and remembers a new {@link ServiceTracker} instance with given listener applied.
     * <p>
     * <b>NOTE</b>: Don't forget to open tracker(s) with {@link #openTrackers()}.
     *
     * @param clazz The service's class
     * @param listener The service's listener triggered on {@link ServiceTracker#addingService(ServiceReference)} and so on
     * @return The newly created {@link ServiceTracker} instance
     */
    protected <S> ServiceTracker<S, S> track(final Class<S> clazz, final SimpleRegistryListener<S> listener) {
        return track(clazz, new SimpleRegistryListenerTrackerCustomizer<S>(listener, context));
    }

    /**
     * Creates and remembers a new {@link ServiceTracker} instance with given listener applied.
     * <p>
     * <b>NOTE</b>: Don't forget to open tracker(s) with {@link #openTrackers()}.
     *
     * @param filter The service filter
     * @param listener The service's listener triggered on {@link ServiceTracker#addingService(ServiceReference)} and so on
     * @return The newly created {@link ServiceTracker} instance
     */
    protected <S> ServiceTracker<S, S> track(final Filter filter, final SimpleRegistryListener<S> listener) {
        return track(filter, new SimpleRegistryListenerTrackerCustomizer<S>(listener, context));
    }

    /**
     * Opens all trackers.
     */
    protected void openTrackers() {
        for (final ServiceTracker<?, ?> tracker : new LinkedList<ServiceTracker<?, ?>>(serviceTrackers)) {
            tracker.open();
        }
    }

    /**
     * Closes all trackers.
     */
    protected void closeTrackers() {
        for (final ServiceTracker<?, ?> tracker : new LinkedList<ServiceTracker<?, ?>>(serviceTrackers)) {
            tracker.close();
        }
    }

    /**
     * Drops all trackers kept by this activator.
     */
    protected void clearTrackers() {
        serviceTrackers.clear();
    }

    /**
     * Unregisters all services.
     */
    protected void unregisterServices() {
        for (final ServiceRegistration<?> reg : serviceRegistrations) {
            reg.unregister();
        }
        serviceRegistrations.clear();
    }

    /**
     * Performs whole clean-up:
     * <ul>
     * <li>Close all trackers</li>
     * <li>Clear all trackers</li>
     * <li>Unregister all services</li>
     * </ul>
     */
    protected void cleanUp() {
        closeTrackers();
        clearTrackers();
        unregisterServices();
    }

}
