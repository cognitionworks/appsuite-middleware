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

package com.openexchange.database.migration.osgi;

import java.rmi.Remote;
import java.util.Dictionary;
import java.util.Hashtable;
import com.openexchange.database.migration.DBMigrationExecutorService;
import com.openexchange.database.migration.DBMigrationMonitorService;
import com.openexchange.database.migration.internal.BundlePackageScanClassResolver;
import com.openexchange.database.migration.internal.DBMigrationExecutorServiceImpl;
import com.openexchange.database.migration.internal.DBMigrationMonitor;
import com.openexchange.database.migration.rmi.DBMigrationRMIServiceImpl;
import com.openexchange.osgi.HousekeepingActivator;
import liquibase.servicelocator.CustomResolverServiceLocator;
import liquibase.servicelocator.ServiceLocator;

/**
 * Activator for the main migration bundle
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since 7.6.1
 */
public class DBMigrationActivator extends HousekeepingActivator {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<?>[] getNeededServices() {
        return EMPTY_CLASSES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void startBundle() throws Exception {
        org.slf4j.LoggerFactory.getLogger(DBMigrationActivator.class).info("Starting bundle: {}", context.getBundle().getSymbolicName());

        // Important: Enable liquibase to load required classes (e.g. liquibase.logging.Logger implementation) from this bundle
        ServiceLocator.setInstance(new CustomResolverServiceLocator(new BundlePackageScanClassResolver(this.context.getBundle())));
        /*
         * instantiate & register services
         */
        DBMigrationExecutorServiceImpl executorService = new DBMigrationExecutorServiceImpl();
        DBMigrationRMIServiceImpl rmiService = new DBMigrationRMIServiceImpl(executorService);
        executorService.setRegisterer(rmiService);
        registerService(DBMigrationMonitorService.class, DBMigrationMonitor.getInstance());
        registerService(DBMigrationExecutorService.class, executorService);
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>(1);
        serviceProperties.put("RMI_NAME", DBMigrationRMIServiceImpl.RMI_NAME);
        registerService(Remote.class, rmiService, serviceProperties);
        openTrackers();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void stopBundle() throws Exception {
        org.slf4j.LoggerFactory.getLogger(DBMigrationActivator.class).info("Stopping bundle: {}", this.context.getBundle().getSymbolicName());
        ServiceLocator.reset();
        super.stopBundle();
    }
}
