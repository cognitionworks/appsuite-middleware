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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import com.openexchange.classloader.DynamicClassLoaderManager;

/**
 * Originally taken from <a href="from http://sling.apache.org/site/apache-sling.html">Apache Sling</a>.
 * <p>
 * &nbsp;&nbsp;<img src="http://sling.apache.org/site/media.data/logo.png">
 * <p>
 * <p>
 * &nbsp;&nbsp;<img src="http://sling.apache.org/site/media.data/logo.png">
 * <p>
 * This is the service factory for the dynamic class loader manager.
 */
public class DynamicClassLoaderManagerFactory implements ServiceFactory<DynamicClassLoaderManager> {

    private static final Object PRESENT = new Object();

    /** The package admin. */
    private final PackageAdmin pckAdmin;

    /** The bundle context. */
    private final BundleContext context;

    private final ConcurrentMap<Long, Object> usedBundles;
    private final ConcurrentMap<String, Object> unresolvedPackages;

    /**
     * Create a new service instance
     *
     * @param ctx The bundle context.
     * @param pckAdmin The package admin.
     */
    public DynamicClassLoaderManagerFactory(final BundleContext ctx, final PackageAdmin pckAdmin) {
        super();
        usedBundles = new ConcurrentHashMap<Long, Object>();
        unresolvedPackages = new ConcurrentHashMap<String, Object>();
        this.context = ctx;
        this.pckAdmin = pckAdmin;
    }

    @Override
    public DynamicClassLoaderManager getService(final Bundle bundle, final ServiceRegistration<DynamicClassLoaderManager> registration) {
        final DynamicClassLoaderManagerImpl manager =
            new DynamicClassLoaderManagerImpl(this.context, this.pckAdmin, new BundleProxyClassLoader(bundle), this);
        return manager;
    }

    @Override
    public void ungetService(final Bundle bundle, final ServiceRegistration<DynamicClassLoaderManager> registration, final DynamicClassLoaderManager service) {
        if (service != null) {
            ((DynamicClassLoaderManagerImpl) service).deactivate();
        }
    }

    /**
     * Check if a bundle has been used for class loading.
     *
     * @param bundleId The bundle id.
     * @return <code>true</code> if the bundle has been used.
     */
    public boolean isBundleUsed(final long bundleId) {
        return usedBundles.containsKey(Long.valueOf(bundleId));
    }

    /**
     * Notify that a bundle is used as a source for class loading.
     *
     * @param bundle The bundle.
     */
    public void addUsedBundle(final Bundle bundle) {
        final long id = bundle.getBundleId();
        this.usedBundles.put(Long.valueOf(id), PRESENT);
    }

    /**
     * Notify that a package is not found during class loading.
     *
     * @param pckName The package name.
     */
    public void addUnresolvedPackage(final String pckName) {
        this.unresolvedPackages.put(pckName, PRESENT);
    }

    /**
     * Check if an exported package from the bundle has not been found during previous class loading attempts.
     *
     * @param bundle The bundle to check
     * @return <code>true</code> if a package has not be found before
     */
    public boolean hasUnresolvedPackages(final Bundle bundle) {
        if (!this.unresolvedPackages.isEmpty()) {
            final ExportedPackage[] pcks = this.pckAdmin.getExportedPackages(bundle);
            if (pcks != null) {
                for (final ExportedPackage pck : pcks) {
                    if (this.unresolvedPackages.containsKey(pck.getName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
