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

package com.openexchange.folderstorage.database.osgi;

import static com.openexchange.folderstorage.database.DatabaseServiceRegistry.getServiceRegistry;
import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;
import com.openexchange.database.DatabaseService;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.database.DatabaseFolderStorage;
import com.openexchange.server.osgiservice.DeferredActivator;
import com.openexchange.server.osgiservice.ServiceRegistry;

/**
 * {@link DatabaseFolderStorageActivator} - {@link BundleActivator Activator} for database folder storage.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class DatabaseFolderStorageActivator extends DeferredActivator {

    private static final org.apache.commons.logging.Log LOG =
        com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(DatabaseFolderStorageActivator.class));

    private ServiceRegistration folderStorageRegistration;

    /**
     * Initializes a new {@link DatabaseFolderStorageActivator}.
     */
    public DatabaseFolderStorageActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { DatabaseService.class };
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Re-available service: " + clazz.getName());
        }
        getServiceRegistry().addService(clazz, getService(clazz));
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        if (LOG.isWarnEnabled()) {
            LOG.warn("Absent service: " + clazz.getName());
        }
        getServiceRegistry().removeService(clazz);
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            /*
             * (Re-)Initialize service registry with available services
             */
            {
                final ServiceRegistry registry = getServiceRegistry();
                registry.clearRegistry();
                final Class<?>[] classes = getNeededServices();
                for (final Class<?> classe : classes) {
                    final Object service = getService(classe);
                    if (null != service) {
                        registry.addService(classe, service);
                    }
                }
            }
            // Register folder storage
            final Dictionary<String, String> dictionary = new Hashtable<String, String>();
            dictionary.put("tree", FolderStorage.REAL_TREE_ID);
            folderStorageRegistration = context.registerService(FolderStorage.class.getName(), new DatabaseFolderStorage(), dictionary);
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        try {
            if (null != folderStorageRegistration) {
                folderStorageRegistration.unregister();
                folderStorageRegistration = null;
            }
            /*
             * Clear service registry
             */
            getServiceRegistry().clearRegistry();
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

}
