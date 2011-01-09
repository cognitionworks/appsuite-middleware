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

package com.openexchange.control.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.MBeanException;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import com.openexchange.server.impl.Version;

/**
 * {@link GeneralControl} - Provides several methods to manage OSGi application.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class GeneralControl implements GeneralControlMBean, MBeanRegistration {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(GeneralControl.class);

    private MBeanServer server;

    private final BundleContext bundleContext;

    public GeneralControl(final BundleContext bundleContext) {
        super();
        this.bundleContext = bundleContext;
    }

    public List<Map<String, String>> list() {
        LOG.info("control command: list");
        final List<Map<String, String>> arrayList = new ArrayList<Map<String, String>>();
        final Bundle[] bundles = bundleContext.getBundles();
        for (int a = 0; a < bundles.length; a++) {
            final Map<String, String> map = new HashMap<String, String>();
            map.put("bundlename", bundles[a].getSymbolicName());
            map.put("status", resolvState(bundles[a].getState()));
            arrayList.add(map);
        }

        return arrayList;
    }

    public void start(final String name) throws MBeanException {
        LOG.info("control command: start package " + name);
        final Bundle bundle = getBundleByName(name, bundleContext.getBundles());
        try {
            if (bundle != null) {
                bundle.start();
            } else {
                throw new MBeanException(null, "bundle " + name + " not found");
            }
        } catch (final BundleException exc) {
            LOG.error("cannot start bundle: " + name, exc);
        }
    }

    public void stop(final String name) throws MBeanException {
        LOG.info("control command: stop package " + name);
        final Bundle bundle = getBundleByName(name, bundleContext.getBundles());
        try {
            if (bundle != null) {
                bundle.stop();
            } else {
                throw new MBeanException(null, "bundle " + name + " not found");
            }
        } catch (final BundleException exc) {
            LOG.error("cannot stop bundle: " + name, exc);
        }
    }

    public void restart(final String name) throws MBeanException {
        stop(name);
        start(name);
    }

    public void install(final String location) {
        LOG.info("install package: " + location);
        try {
            bundleContext.installBundle(location);
        } catch (final BundleException exc) {
            LOG.error("cannot install bundle: " + location, exc);
        }
    }

    public void uninstall(final String name) throws MBeanException {
        LOG.info("uninstall package");
        final Bundle bundle = getBundleByName(name, bundleContext.getBundles());
        try {
            if (bundle != null) {
                bundle.uninstall();
            } else {
                throw new MBeanException(null, "bundle " + name + " not found");
            }
        } catch (final BundleException exc) {
            LOG.error("cannot uninstall bundle: " + name, exc);
        }
    }

    public void update(final String name, final boolean autofresh) throws MBeanException {
        LOG.info("control command: update package: " + name);
        final Bundle bundle = getBundleByName(name, bundleContext.getBundles());
        try {
            if (bundle != null) {
                bundle.update();
                if (autofresh) {
                    freshPackages(bundleContext);
                }
            } else {
                throw new MBeanException(null, "bundle " + name + " not found");
            }
        } catch (final BundleException exc) {
            LOG.error("cannot update bundle: " + name, exc);
        }
    }

    public void refresh() {
        LOG.info("control command: refresh");
        freshPackages(bundleContext);
    }

    public void shutdown() {
        LOG.info("control command: shutdown");
        shutdown(bundleContext, false);
    }

    /**
     * Shutdown of active bundles through closing system bundle
     * 
     * @param bundleContext The bundle context
     * @param waitForExit <code>true</code> to wait for the OSGi framework being shut down completely; otherwise <code>false</code>
     */
    public static final void shutdown(final BundleContext bundleContext, final boolean waitForExit) {
        try {
            /*
             * Simply shut-down the system bundle to enforce invocation of close() method on all running bundles
             */
            final Bundle systemBundle = bundleContext.getBundle(0);
            if (null != systemBundle && systemBundle.getState() == Bundle.ACTIVE) {
                LOG.info("Stopping system bundle...");
                // Note that stopping process is done in a separate thread
                systemBundle.stop();
                if (waitForExit) {
                    /*
                     * TODO: This a bad solution for waiting for thread termination.
                     */
                    try {
                        Thread.sleep(2000);
                    } catch (final InterruptedException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
        } catch (final BundleException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> services() {
        LOG.info("control command: services");
        final List<Map<String, Object>> serviceList = new ArrayList<Map<String, Object>>();

        ServiceReference[] services;
        try {
            /*
             * Null parameters to get all services from BundleContext.getServiceReferences(String clazz, String filter);
             */
            final String clazz = null;
            final String filter = null;
            services = bundleContext.getServiceReferences(clazz, filter);
            if (services != null) {
                final int size = services.length;
                if (size > 0) {
                    for (int j = 0; j < size; j++) {
                        final Map<String, Object> hashMap = new HashMap<String, Object>();

                        final ServiceReference service = services[j];

                        hashMap.put("service", service.toString());
                        hashMap.put("registered_by", service.getBundle().toString());

                        final Bundle[] usedByBundles = service.getUsingBundles();
                        final List<String> bundleList = new ArrayList<String>();
                        if (usedByBundles != null) {
                            for (int a = 0; a < usedByBundles.length; a++) {
                                final String bundleName = usedByBundles[a].getSymbolicName();
                                if (bundleName != null) {
                                    bundleList.add(bundleName);
                                }
                            }
                        }

                        if (bundleList.size() > 0) {
                            hashMap.put("bundles", bundleList);
                        }

                        serviceList.add(hashMap);
                    }
                }
            }
        } catch (final InvalidSyntaxException exc) {
            LOG.error(exc.getMessage(), exc);
        }

        return serviceList;
    }

    public String version() {
        return Version.getVersionString();
    }

    private Bundle getBundleByName(final String name, final Bundle[] bundle) {
        for (int a = 0; a < bundle.length; a++) {
            if (bundle[a].getSymbolicName().equals(name)) {
                return bundle[a];
            }
        }
        return null;
    }

    public ObjectName preRegister(final MBeanServer server, final ObjectName nameArg) throws Exception {
        ObjectName name = nameArg;
        if (name == null) {
            name = new ObjectName(
                new StringBuilder(server.getDefaultDomain()).append(":name=").append(this.getClass().getName()).toString());
        }
        this.server = server;
        return name;
    }

    public void postRegister(final Boolean registrationDone) {
        if (LOG.isTraceEnabled()) {
            LOG.trace(new StringBuilder("postRegister() with ").append(registrationDone));
        }
    }

    public void preDeregister() throws Exception {
        if (LOG.isTraceEnabled()) {
            LOG.trace("preDeregister()");
        }
    }

    public void postDeregister() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("postDeregister()");
        }
    }

    public Integer getNbObjects() {
        try {
            return Integer.valueOf((server.queryMBeans(new ObjectName("*:*"), null)).size());
        } catch (final Exception e) {
            return Integer.valueOf(-1);
        }
    }

    private static String resolvState(final int state) {
        // TODO: add all states
        switch (state) {
        case Bundle.ACTIVE:
            return "ACTIVE";
        case Bundle.INSTALLED:
            return "INSTALLED";
        case Bundle.RESOLVED:
            return "RESOLVED";
        case Bundle.STOPPING:
            return "STOPPING";
        case Bundle.UNINSTALLED:
            return "UNINSTALLED";
        default:
            return "UNKNOWN";
        }
    }

    protected static void freshPackages(final BundleContext bundleContext) {
        final ServiceReference serviceReference = bundleContext.getServiceReference("org.osgi.service.packageadmin.PackageAdmin");
        final PackageAdmin packageAdmin = (PackageAdmin) bundleContext.getService(serviceReference);
        packageAdmin.refreshPackages(null);
    }
}
