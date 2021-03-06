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

package com.openexchange.filemanagement.osgi;

import java.util.ArrayList;
import java.util.List;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.config.ConfigurationService;
import com.openexchange.dispatcher.DispatcherPrefixService;
import com.openexchange.filemanagement.DistributedFileUtils;
import com.openexchange.filemanagement.ManagedFileManagement;
import com.openexchange.filemanagement.internal.DistributedFileUtilsImpl;
import com.openexchange.filemanagement.internal.ManagedFileManagementImpl;
import com.openexchange.osgi.DependentServiceRegisterer;
import com.openexchange.timer.TimerService;

/**
 * {@link ManagedFileManagementActivator}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public final class ManagedFileManagementActivator implements BundleActivator {

    private List<ServiceTracker<?,?>> trackers = null;
    private ServiceRegistration<DistributedFileUtils> distributedFileUtilsRegistration;

    /**
     * Initializes a new {@link ManagedFileManagementActivator}.
     */
    public ManagedFileManagementActivator() {
        super();
    }

    @Override
    public synchronized void start(BundleContext context) throws Exception {
        List<ServiceTracker<?,?>> trackers = new ArrayList<>(2);
        this.trackers = trackers;

        trackers.add(new ServiceTracker<ConfigurationService, ConfigurationService>(context, ConfigurationService.class, new TmpFileCleaner()));

        DependentServiceRegisterer<ManagedFileManagement> registerer = new DependentServiceRegisterer<ManagedFileManagement>(context, ManagedFileManagement.class, ManagedFileManagementImpl.class, null, TimerService.class, DispatcherPrefixService.class);
        trackers.add(new ServiceTracker<Object, Object>(context, registerer.getFilter(), registerer));

        for (ServiceTracker<?,?> tracker : trackers) {
            tracker.open();
        }

        distributedFileUtilsRegistration = context.registerService(DistributedFileUtils.class, new DistributedFileUtilsImpl(), null);
    }

    @Override
    public synchronized void stop(BundleContext context) {
        ServiceRegistration<DistributedFileUtils> distributedFileUtilsRegistration = this.distributedFileUtilsRegistration;
        if (distributedFileUtilsRegistration != null) {
            this.distributedFileUtilsRegistration = null;
            distributedFileUtilsRegistration.unregister();
        }

        List<ServiceTracker<?,?>> trackers = this.trackers;
        if (trackers != null) {
            this.trackers = null;
            for (ServiceTracker<?,?> tracker : trackers) {
                tracker.close();
            }
        }
    }

}
