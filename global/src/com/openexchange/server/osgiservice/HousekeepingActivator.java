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

package com.openexchange.server.osgiservice;

import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * A {@link HousekeepingActivator} helps with housekeeping tasks like remembering service trackers or service registrations and cleaning
 * them up later.
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public abstract class HousekeepingActivator extends DeferredActivator {

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
     * Adds specified service tracker to this activator. Thus it is automatically closed and removed by {@link #cleanUp()}.
     * 
     * @param tracker The service tracker
     */
    protected void rememberTracker(final ServiceTracker<?, ?> tracker) {
        serviceTrackers.add(tracker);
    }

    /**
     * Removes specified service tracker from this activator.
     * 
     * @param tracker The service tracker
     */
    protected void forgetTracker(final ServiceTracker<?, ?> tracker) {
        serviceTrackers.remove(tracker);
    }

    /**
     * Creates and starts a new {@link ServiceTracker} instance parameterized with given customizer.
     * 
     * @param clazz The class of the tracked service
     * @param customizer The customizer applied to newly created {@link ServiceTracker} instance
     * @return The newly created {@link ServiceTracker} instance
     */
    protected <S> ServiceTracker<S, S> track(final Class<? extends S> clazz, final ServiceTrackerCustomizer<S, S> customizer) {
        final ServiceTracker<S, S> tracker = new ServiceTracker<S, S>(context, clazz.getName(), customizer);
        rememberTracker(tracker);
        return tracker;
    }

    /**
     * Creates and starts a new {@link ServiceTracker} instance parameterized with given customizer.
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
     * Creates and starts a new {@link ServiceTracker} instance for specified service's class.
     * 
     * @param clazz The service's class
     * @return The newly created {@link ServiceTracker} instance
     */
    protected <S> ServiceTracker<S, S> track(final Class<? extends S> clazz) {
        return track(clazz, (ServiceTrackerCustomizer<S, S>) null);
    }

    /**
     * Creates and starts a new {@link ServiceTracker} instance for specified filter.
     * 
     * @param filter The filter to apply
     * @return The newly created {@link ServiceTracker} instance
     */
    protected <S> ServiceTracker<S, S> track(final Filter filter) {
        return track(filter, (ServiceTrackerCustomizer<S, S>) null);
    }

    /**
     * Creates and starts a new {@link ServiceTracker} instance with given listener applied.
     * 
     * @param clazz The service's class
     * @param listener The service's listener triggered on {@link ServiceTracker#addingService(ServiceReference)} and so on
     * @return The newly created {@link ServiceTracker} instance
     */
    protected <S> ServiceTracker<S, S> track(final Class<? extends S> clazz, final SimpleRegistryListener<S> listener) {
        return track(clazz, new ServiceTrackerCustomizer<S, S>() {

            @Override
            public S addingService(final ServiceReference<S> arg0) {
                final S service = context.getService(arg0);
                listener.added(arg0, service);
                return service;
            }

            @Override
            public void modifiedService(final ServiceReference<S> arg0, final S arg1) {
                // Don't care
            }

            @Override
            public void removedService(final ServiceReference<S> arg0, final S arg1) {
                listener.removed(arg0, arg1);
                context.ungetService(arg0);
            }

        });
    }

    /**
     * Creates and starts a new {@link ServiceTracker} instance with given listener applied.
     * 
     * @param filter The service filter
     * @param listener The service's listener triggered on {@link ServiceTracker#addingService(ServiceReference)} and so on
     * @return The newly created {@link ServiceTracker} instance
     */
    protected <S> ServiceTracker<S, S> track(final Filter filter, final SimpleRegistryListener<S> listener) {
        return track(filter, new ServiceTrackerCustomizer<S, S>() {

            @Override
            public S addingService(final ServiceReference<S> arg0) {
                final S service = context.getService(arg0);
                listener.added(arg0, service);
                return service;
            }

            @Override
            public void modifiedService(final ServiceReference<S> arg0, final S arg1) {
                // TODO Auto-generated method stub

            }

            @Override
            public void removedService(final ServiceReference<S> arg0, final S arg1) {
                listener.removed(arg0, arg1);
                context.ungetService(arg0);
            }

        });
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
