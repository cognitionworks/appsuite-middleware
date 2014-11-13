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

package com.openexchange.filestore.impl.osgi;

import java.io.ByteArrayInputStream;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import com.openexchange.caching.CacheService;
import com.openexchange.context.ContextService;
import com.openexchange.database.DatabaseService;
import com.openexchange.filestore.FileStorageService;
import com.openexchange.filestore.impl.groupware.AddFilestoreColumnsToUserTable;
import com.openexchange.filestore.impl.groupware.AddUserColumnToFilestoreUsageTable;
import com.openexchange.groupware.update.DefaultUpdateTaskProviderService;
import com.openexchange.groupware.update.UpdateTaskProviderService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.user.UserService;

/**
 * {@link DBQuotaFileStorageActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.0
 */
public class DBQuotaFileStorageActivator extends HousekeepingActivator {

    /**
     * Initializes a new {@link DBQuotaFileStorageActivator}.
     */
    public DBQuotaFileStorageActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { DatabaseService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        final Logger logger = org.slf4j.LoggerFactory.getLogger(DBQuotaFileStorageActivator.class);
        final BundleContext context = this.context;

        Services.setServiceLookup(this);

        // Service trackers
        {
            ServiceTracker<FileStorageService,FileStorageService> tracker = new ServiceTracker<FileStorageService,FileStorageService>(context, FileStorageService.class, new DBQuotaFileStorageRegisterer(context));
            rememberTracker(tracker);
            trackService(ContextService.class);
            trackService(UserService.class);

            {
                ServiceTrackerCustomizer<CacheService, CacheService> customizer = new ServiceTrackerCustomizer<CacheService, CacheService>() {

                    private final String regionName = "QuotaFileStorages";

                    @Override
                    public CacheService addingService(ServiceReference<CacheService> reference) {
                        try {
                            CacheService cacheService = context.getService(reference);

                            int idleSeconds = 7200; // 2 hours
                            int shrinkInterval = 600; // Every 10 minutes
                            final byte[] ccf = ("jcs.region."+regionName+"=LTCP\n" +
                                "jcs.region."+regionName+".cacheattributes=org.apache.jcs.engine.CompositeCacheAttributes\n" +
                                "jcs.region."+regionName+".cacheattributes.MaxObjects=1000000\n" +
                                "jcs.region."+regionName+".cacheattributes.MemoryCacheName=org.apache.jcs.engine.memory.lru.LRUMemoryCache\n" +
                                "jcs.region."+regionName+".cacheattributes.UseMemoryShrinker=true\n" +
                                "jcs.region."+regionName+".cacheattributes.MaxMemoryIdleTimeSeconds="+idleSeconds+"\n" +
                                "jcs.region."+regionName+".cacheattributes.ShrinkerIntervalSeconds="+shrinkInterval+"\n" +
                                "jcs.region."+regionName+".elementattributes=org.apache.jcs.engine.ElementAttributes\n" +
                                "jcs.region."+regionName+".elementattributes.IsEternal=false\n" +
                                "jcs.region."+regionName+".elementattributes.MaxLifeSeconds=-1\n" +
                                "jcs.region."+regionName+".elementattributes.IdleTime="+idleSeconds+"\n" +
                                "jcs.region."+regionName+".elementattributes.IsSpool=false\n" +
                                "jcs.region."+regionName+".elementattributes.IsRemote=false\n" +
                                "jcs.region."+regionName+".elementattributes.IsLateral=false\n").getBytes();

                            cacheService.loadConfiguration(new ByteArrayInputStream(ccf), true);
                            addService(CacheService.class, cacheService);
                            return cacheService;
                        } catch (Exception e) {
                            // Failed to initialize cache region
                            logger.warn("Failed to initialize cache region {}", regionName, e);
                        }

                        context.ungetService(reference);
                        return null;
                    }

                    @Override
                    public void modifiedService(ServiceReference<CacheService> reference, CacheService service) {
                        // Ignore
                    }

                    @Override
                    public void removedService(ServiceReference<CacheService> reference, CacheService service) {
                        try {
                            service.freeCache(regionName);
                        } catch (Exception e) {
                            // Ignore
                        }
                        removeService(CacheService.class);
                        context.ungetService(reference);
                    }
                };

                track(CacheService.class, customizer);
            }

            openTrackers();
        }

        // Update tasks
        registerService(UpdateTaskProviderService.class, new DefaultUpdateTaskProviderService(new AddFilestoreColumnsToUserTable(), new AddUserColumnToFilestoreUsageTable()));

        logger.info("Bundle successfully started: {}", context.getBundle().getSymbolicName());
    }

    @Override
    protected void stopBundle() throws Exception {
        super.stopBundle();
        Services.setServiceLookup(null);
        Logger logger = org.slf4j.LoggerFactory.getLogger(DBQuotaFileStorageActivator.class);
        logger.info("Bundle successfully stopped: {}", context.getBundle().getSymbolicName());
    }

    @Override
    public <S> boolean addService(Class<S> clazz, S service) {
        return super.addService(clazz, service);
    }

    @Override
    public <S> boolean removeService(Class<? extends S> clazz) {
        return super.removeService(clazz);
    }

}
