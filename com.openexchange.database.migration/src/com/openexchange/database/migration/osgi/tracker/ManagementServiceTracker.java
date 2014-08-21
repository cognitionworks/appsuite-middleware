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

package com.openexchange.database.migration.osgi.tracker;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.migration.DBMigrationExecutorService;
import com.openexchange.database.migration.mbean.DBMigrationMBean;
import com.openexchange.database.migration.mbean.DBMigrationMBeanImpl;
import com.openexchange.exception.OXException;
import com.openexchange.management.ManagementService;


/**
 * {@link ServiceTracker} to track occurrence of {@link ManagementService}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since 7.6.1
 */
public class ManagementServiceTracker implements ServiceTrackerCustomizer<ManagementService, ManagementService> {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ManagementServiceTracker.class);

    private final BundleContext context;

    private ObjectName objectName;

    private DatabaseService databaseService;

    private DBMigrationExecutorService dbMigrationExecutorService;

    /**
     * Initializes a new {@link ManagementServiceTracker}.
     *
     * @param context
     * @param databaseService
     */
    public ManagementServiceTracker(final BundleContext context, DBMigrationExecutorService dbMigrationExecutorService, DatabaseService databaseService) {
        super();
        this.context = context;
        this.dbMigrationExecutorService = dbMigrationExecutorService;
        this.databaseService = databaseService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ManagementService addingService(final ServiceReference<ManagementService> reference) {
        final ManagementService management = context.getService(reference);
        registerMBean(management);
        return management;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modifiedService(final ServiceReference<ManagementService> reference, final ManagementService service) {
        // Nothing to do.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removedService(final ServiceReference<ManagementService> reference, final ManagementService service) {
        final ManagementService management = service;
        unregisterMBean(management);
        context.ungetService(reference);
    }

    private void registerMBean(final ManagementService management) {
        if (objectName == null) {
            try {
                objectName = getObjectName(DBMigrationMBean.class.getName(), DBMigrationMBean.DOMAIN);
                management.registerMBean(objectName, new DBMigrationMBeanImpl(DBMigrationMBean.class, dbMigrationExecutorService, databaseService));
            } catch (final MalformedObjectNameException e) {
                LOG.error("", e);
            } catch (final OXException e) {
                LOG.error("", e);
            } catch (final Exception e) {
                LOG.error("", e);
            }
        }
    }

    private void unregisterMBean(final ManagementService management) {
        if (objectName != null) {
            try {
                management.unregisterMBean(objectName);
            } catch (final OXException e) {
                LOG.error("", e);
            } finally {
                objectName = null;
            }
        }
    }

    /**
     * Creates an appropriate instance of {@link ObjectName} from specified class name and domain name.
     *
     * @param className The class name to use as object name
     * @param domain The domain name
     * @return An appropriate instance of {@link ObjectName}
     * @throws MalformedObjectNameException If instantiation of {@link ObjectName} fails
     */
    private static ObjectName getObjectName(final String className, final String domain) throws MalformedObjectNameException {
        final int pos = className.lastIndexOf('.');
        return new ObjectName(domain, "name", (pos == -1 ? className : className.substring(pos + 1)));
    }
}
