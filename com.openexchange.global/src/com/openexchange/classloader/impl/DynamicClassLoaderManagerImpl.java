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

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.openexchange.classloader.impl;

import java.util.Arrays;
import java.util.Comparator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.classloader.DynamicClassLoaderManager;
import com.openexchange.classloader.DynamicClassLoaderProvider;

/**
 * Originally taken from <a href="from http://sling.apache.org/site/apache-sling.html">Apache Sling</a>.
 * <p>
 * &nbsp;&nbsp;<img src="http://sling.apache.org/site/media.data/logo.png">
 * <p>
 * <p>
 * &nbsp;&nbsp;<img src="http://sling.apache.org/site/media.data/logo.png">
 * <p>
 * This is the default implementation of the dynamic class loader manager.
 */
public class DynamicClassLoaderManagerImpl extends ServiceTracker<DynamicClassLoaderProvider, DynamicClassLoaderProvider> implements DynamicClassLoaderManager {

    /** The package admin class loader. */
    private final PackageAdminClassLoader pckAdminCL;

    /** The dynamic class loader. */
    private final ClassLoaderFacade facade;

    /** The cached chain of class loaders. */
    private ClassLoader[] cache;

    /** The cached chain of dynamic class loader providers. */
    private DynamicClassLoaderProvider[] providerCache;

    /** Is this still active? */
    private volatile boolean active = true;

    /** Tracking count */
    private volatile int trackingCount = -1;

    /**
     * Create a new service instance
     *
     * @param ctx The bundle context of the class loader bundle
     * @param pckAdmin The package admin.
     * @param parent The parent class loader.
     */
    public DynamicClassLoaderManagerImpl(final BundleContext ctx, final PackageAdmin pckAdmin, final ClassLoader parent, final DynamicClassLoaderManagerFactory factory) {
        super(ctx, DynamicClassLoaderProvider.class, null);
        this.pckAdminCL = new PackageAdminClassLoader(pckAdmin, parent, factory);
        this.cache = new ClassLoader[] { this.pckAdminCL };
        this.providerCache = new DynamicClassLoaderProvider[0];
        this.open();
        this.facade = new ClassLoaderFacade(this);
    }

    private synchronized void updateCache() {
        if (this.trackingCount < this.getTrackingCount()) {
            final ServiceReference[] refs = this.getServiceReferences();
            final ClassLoader[] loaders;
            final DynamicClassLoaderProvider[] providers;
            if (refs == null || refs.length == 0) {
                loaders = new ClassLoader[] { this.pckAdminCL };
                providers = new DynamicClassLoaderProvider[0];
            } else {
                loaders = new ClassLoader[1 + refs.length];
                providers = new DynamicClassLoaderProvider[refs.length];
                Arrays.sort(refs, ServiceReferenceComparator.INSTANCE);
                int index = 0;
                for (final ServiceReference ref : refs) {
                    final DynamicClassLoaderProvider provider = this.getService(ref);
                    if (provider != null) {
                        loaders[index] = provider.getClassLoader(this.pckAdminCL);
                        providers[index] = provider;
                    }
                    index++;
                }
                loaders[index] = this.pckAdminCL;
            }
            // release old class loaders
            this.releaseProviders();

            // and now use new array
            this.cache = loaders;
            this.providerCache = providers;
            this.trackingCount = this.getTrackingCount();
        }
    }

    /**
     * Free used class loader providers
     */
    private void releaseProviders() {
        if (this.providerCache != null) {
            for (int i = 0; i < this.providerCache.length; i++) {
                if (this.cache[i] != null) {
                    this.providerCache[i].release(this.cache[i]);
                }
            }
        }
    }

    /**
     * Deactivate this service.
     */
    public void deactivate() {
        this.releaseProviders();
        this.active = false;
        this.close();
        this.providerCache = null;
        this.cache = null;
    }

    /**
     * Check if this service is still active.
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * @see org.apache.sling.commons.classloader.DynamicClassLoaderManager#getDynamicClassLoader()
     */
    @Override
    public ClassLoader getDynamicClassLoader() {
        return this.facade;
    }

    /**
     * This list contains the current list of class loaders. The first class loader is always the package admin class loader, therefore this
     * list is never null and has always a size greater than zero.
     *
     * @return The list of class loaders.
     */
    public ClassLoader[] getDynamicClassLoaders() {
        if (this.trackingCount < this.getTrackingCount()) {
            updateCache();
        }
        return this.cache;
    }

    /**
     * Comparator for service references.
     */
    protected static final class ServiceReferenceComparator implements Comparator<ServiceReference> {

        public static final ServiceReferenceComparator INSTANCE = new ServiceReferenceComparator();

        @Override
        public int compare(ServiceReference o1, ServiceReference o2) {
            return o1.compareTo(o2);
        }
    }
}
