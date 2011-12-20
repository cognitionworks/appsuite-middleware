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

package com.openexchange.file.storage.rdb.osgi;

import static com.openexchange.file.storage.rdb.services.FileStorageRdbServiceRegistry.getServiceRegistry;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.caching.CacheService;
import com.openexchange.context.ContextService;
import com.openexchange.crypto.CryptoService;
import com.openexchange.database.CreateTableService;
import com.openexchange.database.DatabaseService;
import com.openexchange.datatypes.genericonf.storage.GenericConfigurationStorageService;
import com.openexchange.file.storage.FileStorageAccountManagerProvider;
import com.openexchange.file.storage.FileStorageService;
import com.openexchange.file.storage.rdb.groupware.FileStorageRdbCreateTableTask;
import com.openexchange.file.storage.rdb.groupware.FileStorageRdbDeleteListener;
import com.openexchange.file.storage.rdb.internal.CachingFileStorageAccountStorage;
import com.openexchange.file.storage.rdb.internal.RdbFileStorageAccountManagerProvider;
import com.openexchange.file.storage.rdb.secret.RdbFileStorageSecretHandling;
import com.openexchange.file.storage.registry.FileStorageServiceRegistry;
import com.openexchange.groupware.delete.DeleteListener;
import com.openexchange.groupware.update.UpdateTask;
import com.openexchange.groupware.update.UpdateTaskProviderService;
import com.openexchange.secret.SecretService;
import com.openexchange.secret.osgi.tools.WhiteboardSecretService;
import com.openexchange.secret.recovery.EncryptedItemDetectorService;
import com.openexchange.secret.recovery.SecretMigrator;
import com.openexchange.server.osgiservice.HousekeepingActivator;
import com.openexchange.server.osgiservice.ServiceRegistry;

/**
 * {@link FileStorageRdbActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since Open-Xchange v6.18.2
 */
public class FileStorageRdbActivator extends HousekeepingActivator {

    private List<ServiceTracker<?,?>> trackers;

    private WhiteboardSecretService secretService;

    public FileStorageRdbActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] {
            DatabaseService.class, GenericConfigurationStorageService.class, ContextService.class, FileStorageServiceRegistry.class,
            CacheService.class, CryptoService.class };
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        final org.apache.commons.logging.Log logger = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(FileStorageRdbActivator.class));
        if (logger.isInfoEnabled()) {
            logger.info("Re-available service: " + clazz.getName());
        }
        getServiceRegistry().addService(clazz, getService(clazz));
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        final org.apache.commons.logging.Log logger = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(FileStorageRdbActivator.class));
        if (logger.isWarnEnabled()) {
            logger.warn("Absent service: " + clazz.getName());
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
                registry.addService(SecretService.class, secretService = new WhiteboardSecretService(context));
                secretService.open();
            }
            /*
             * Feed cache with additional cache configuration for file storage account cache
             */
            {
                /*
                 * FileStorageAccount region with 5 minutes time-out
                 */
                final String regionName = CachingFileStorageAccountStorage.getRegionName();
                final byte[] ccf = ("jcs.region."+regionName+"=LTCP\n" +
                		"jcs.region."+regionName+".cacheattributes=org.apache.jcs.engine.CompositeCacheAttributes\n" +
                		"jcs.region."+regionName+".cacheattributes.MaxObjects=10000000\n" +
                		"jcs.region."+regionName+".cacheattributes.MemoryCacheName=org.apache.jcs.engine.memory.lru.LRUMemoryCache\n" +
                		"jcs.region."+regionName+".cacheattributes.UseMemoryShrinker=true\n" +
                		"jcs.region."+regionName+".cacheattributes.MaxMemoryIdleTimeSeconds=180\n" +
                		"jcs.region."+regionName+".cacheattributes.ShrinkerIntervalSeconds=60\n" +
                		"jcs.region."+regionName+".elementattributes=org.apache.jcs.engine.ElementAttributes\n" +
                		"jcs.region."+regionName+".elementattributes.IsEternal=false\n" +
                		"jcs.region."+regionName+".elementattributes.MaxLifeSeconds=300\n" +
                		"jcs.region."+regionName+".elementattributes.IdleTime=180\n" +
                		"jcs.region."+regionName+".elementattributes.IsSpool=false\n" +
                		"jcs.region."+regionName+".elementattributes.IsRemote=false\n" +
                		"jcs.region."+regionName+".elementattributes.IsLateral=false\n").getBytes();
                getService(CacheService.class).loadConfiguration(new ByteArrayInputStream(ccf));
            }
            /*
             * Initialize and start service trackers
             */

            trackers = new ArrayList<ServiceTracker<?,?>>(2);
            final ServiceTracker<FileStorageService, FileStorageService> fileStorageServiceTracker = trackService(FileStorageService.class);
            trackers.add(fileStorageServiceTracker);
            for (final ServiceTracker<?,?> tracker : trackers) {
                tracker.open();
            }
            /*
             * Initialize and register services
             */
            registerService(FileStorageAccountManagerProvider.class, new RdbFileStorageAccountManagerProvider(), null);
            /*
             * The update task/create table service
             */
            final FileStorageRdbCreateTableTask createTableTask = new FileStorageRdbCreateTableTask();
            registerService(UpdateTaskProviderService.class, new UpdateTaskProviderService() {
                @Override
                public Collection<UpdateTask> getUpdateTasks() {
                    return Collections.<UpdateTask> singletonList(createTableTask);
                }
            }, null);
            registerService(CreateTableService.class, createTableTask, null);
            /*
             * The delete listener
             */
            registerService(DeleteListener.class, new FileStorageRdbDeleteListener(), null);
            /*
             * Secret Handling
             */
            {
                final RdbFileStorageSecretHandling secretHandling = new RdbFileStorageSecretHandling() {

                    @Override
                    protected Collection<FileStorageService> getFileStorageServices() {
                        final Object[] objects = fileStorageServiceTracker.getServices();
                        if (objects == null) {
                            return Collections.emptyList();
                        }
                        final List<FileStorageService> list = new ArrayList<FileStorageService>(objects.length);
                        for (final Object o : objects) {
                            list.add((FileStorageService) o);
                        }
                        return list;
                    }
                };
                registerService(EncryptedItemDetectorService.class, secretHandling, null);
                registerService(SecretMigrator.class, secretHandling, null);
            }
        } catch (final Exception e) {
            com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(FileStorageRdbActivator.class)).error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        try {
            if (null != trackers) {
                while (!trackers.isEmpty()) {
                    trackers.remove(0).close();
                }
                trackers = null;
            }
            cleanUp();
            /*
             * Clear service registry
             */
            getServiceRegistry().clearRegistry();
            secretService.close();
        } catch (final Exception e) {
            com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(FileStorageRdbActivator.class)).error(e.getMessage(), e);
            throw e;
        }
    }

}
