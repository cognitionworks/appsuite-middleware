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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.control.osgi;

import static com.openexchange.control.internal.GeneralControl.shutdown;
import org.osgi.framework.BundleContext;
import com.openexchange.control.internal.GeneralControl;
import com.openexchange.control.internal.GeneralControlMBean;
import com.openexchange.management.ManagementService;
import com.openexchange.management.osgi.HousekeepingManagementTracker;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.version.VersionService;

/**
 * {@link ControlActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ControlActivator extends HousekeepingActivator {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ControlActivator.class);

    private Thread shutdownHookThread;

    /**
     * Initializes a new {@link ControlActivator}
     */
    public ControlActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { VersionService.class };
    }

    @Override
    protected synchronized void startBundle() throws Exception {
        LOG.info("starting bundle: com.openexchange.control");
        try {
            /*
             * Create & open service tracker
             */
            track(ManagementService.class, new HousekeepingManagementTracker(context, GeneralControlMBean.MBEAN_NAME, GeneralControlMBean.DOMAIN, new GeneralControl(context, getServiceSafe(VersionService.class))));
            openTrackers();
            /*
             * Add shutdown hook
             */
            final Thread shutdownHookThread = new ControlShutdownHookThread(context);
            Runtime.getRuntime().addShutdownHook(shutdownHookThread);
            this.shutdownHookThread = shutdownHookThread;
        } catch (Exception e) {
            LOG.error("", e);
            throw e;
        }
    }

    @Override
    protected synchronized void stopBundle() throws Exception {
        LOG.info("stopping bundle: com.openexchange.control");
        try {
            final Thread shutdownHookThread = this.shutdownHookThread;
            if (null != shutdownHookThread) {
                if (!shutdownHookThread.isAlive()) {
                    /*
                     * Remove shutdown hook if not running. Otherwise stop() is invoked by the thread itself.
                     */
                    try {
                        if (!Runtime.getRuntime().removeShutdownHook(shutdownHookThread)) {
                            LOG.error("com.openexchange.control shutdown hook could not be deregistered");
                        }
                    } catch (IllegalStateException e) {
                        /*
                         * Just for safety reason...
                         */
                        LOG.error("Virtual machine is already in the process of shutting down!", e);
                    }
                }
                this.shutdownHookThread = null;
            }
            // Do bundle clean-up
            cleanUp();
        } catch (Exception e) {
            LOG.error("", e);
            throw e;
        }
    }

    /**
     * {@link ControlShutdownHookThread} - The shutdown hook thread of control bundle.
     *
     * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
     */
    private static final class ControlShutdownHookThread extends Thread {

        private final BundleContext bundleContext;

        /**
         * Initializes a new {@link ControlShutdownHookThread}
         *
         * @param bundleContext The bundle context
         */
        ControlShutdownHookThread(final BundleContext bundleContext) {
            super();
            this.bundleContext = bundleContext;
        }

        @Override
        public void run() {
            shutdown(bundleContext, true);
        }

    } // End of ControlShutdownHookThread

}
