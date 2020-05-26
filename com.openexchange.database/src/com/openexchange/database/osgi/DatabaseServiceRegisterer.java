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

package com.openexchange.database.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Reloadable;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.database.AssignmentFactory;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.GeneralDatabaseConnectionListener;
import com.openexchange.database.JdbcProperties;
import com.openexchange.database.internal.AssignmentFactoryImpl;
import com.openexchange.database.internal.Configuration;
import com.openexchange.database.internal.DatabaseServiceImpl;
import com.openexchange.database.internal.Initialization;
import com.openexchange.database.internal.JdbcPropertiesImpl;
import com.openexchange.database.internal.ConnectionReloaderImpl;
import com.openexchange.database.migration.DBMigrationExecutorService;
import com.openexchange.osgi.ServiceListing;

/**
 * Injects the {@link ConfigurationService} and publishes the DatabaseService.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class DatabaseServiceRegisterer implements ServiceTrackerCustomizer<Object, Object> {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DatabaseServiceRegisterer.class);

    private final BundleContext context;
    private final ServiceListing<GeneralDatabaseConnectionListener> connectionListeners;

    private ConfigurationService configService;
    private ConfigViewFactory configViewFactory;
    private DBMigrationExecutorService migrationService;

    private ServiceRegistration<DatabaseService> databaseServiceRegistration;

    /**
     * Initializes a new {@link DatabaseServiceRegisterer}.
     *
     * @param context The bundle context
     */
    public DatabaseServiceRegisterer(ServiceListing<GeneralDatabaseConnectionListener> connectionListeners, BundleContext context) {
        super();
        this.connectionListeners = connectionListeners;
        this.context = context;
    }

    @Override
    public synchronized Object addingService(final ServiceReference<Object> reference) {
        final Object obj = context.getService(reference);
        if (obj instanceof ConfigurationService) {
            configService = (ConfigurationService) obj;
        }
        if (obj instanceof ConfigViewFactory) {
            configViewFactory = (ConfigViewFactory) obj;
        }
        if (obj instanceof DBMigrationExecutorService) {
            migrationService = (DBMigrationExecutorService) obj;
        }
        boolean needsRegistration = null != configService && null != configViewFactory && null != migrationService;

        if (needsRegistration && !Initialization.getInstance().isStarted()) {
            DatabaseServiceImpl databaseService = null;
            try {
                Initialization.setConfigurationService(configService);
                // Parse configuration
                Configuration configuration = new Configuration();
                configuration.readConfiguration(configService);
                JdbcPropertiesImpl.getInstance().setJdbcProperties(configuration.getJdbcProps());
                ConnectionReloaderImpl reloader = new ConnectionReloaderImpl(configuration);
                context.registerService(Reloadable.class, reloader, null);
                context.registerService(JdbcProperties.class, JdbcPropertiesImpl.getInstance(), null);
                databaseService = Initialization.getInstance().start(configService, configViewFactory, migrationService, connectionListeners, configuration, reloader);
                LOG.info("Publishing DatabaseService.");
                databaseServiceRegistration = context.registerService(DatabaseService.class, databaseService, null);
            } catch (Exception e) {
                LOG.error("Publishing the DatabaseService failed.", e);
            }
            try {
                if (databaseService != null) {
                    AssignmentFactoryImpl assignmentFactoryImpl = new AssignmentFactoryImpl(databaseService);
                    assignmentFactoryImpl.reload();
                    LOG.info("Publishing AssignmentFactory.");
                    context.registerService(AssignmentFactory.class, assignmentFactoryImpl, null);
                } else {
                    LOG.error("Publishing AssignmentFactory failed due to missing DatabaseService.");
                }
            } catch (Exception e) {
                LOG.error("Publishing AssignmentFactory failed. This is normal until a server has been registered.", e);
            }
        }
        return obj;
    }

    @Override
    public void modifiedService(final ServiceReference<Object> reference, final Object service) {
        // Nothing to do.
    }

    @Override
    public synchronized void removedService(final ServiceReference<Object> reference, final Object service) {
        ServiceRegistration<DatabaseService> databaseServiceRegistration = this.databaseServiceRegistration;
        if (null != databaseServiceRegistration) {
            LOG.info("Unpublishing DatabaseService.");
            this.databaseServiceRegistration = null;
            databaseServiceRegistration.unregister();
            Initialization.getInstance().stop();
            Initialization.setConfigurationService(null);
        }
        context.ungetService(reference);
    }
}
