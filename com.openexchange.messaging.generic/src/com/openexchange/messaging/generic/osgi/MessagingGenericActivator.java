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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.messaging.generic.osgi;

import static com.openexchange.messaging.generic.services.MessagingGenericServiceRegistry.getServiceRegistry;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.caching.CacheService;
import com.openexchange.context.ContextService;
import com.openexchange.database.DatabaseService;
import com.openexchange.datatypes.genericonf.storage.GenericConfigurationStorageService;
import com.openexchange.groupware.delete.DeleteListener;
import com.openexchange.groupware.update.UpdateTask;
import com.openexchange.groupware.update.UpdateTaskProviderService;
import com.openexchange.messaging.generic.groupware.MessagingGenericCreateTableTask;
import com.openexchange.messaging.generic.groupware.MessagingGenericDeleteListener;
import com.openexchange.messaging.generic.internal.CachingMessagingAccountStorage;
import com.openexchange.messaging.registry.MessagingServiceRegistry;
import com.openexchange.server.osgiservice.DeferredActivator;
import com.openexchange.server.osgiservice.ServiceRegistry;

/**
 * {@link MessagingGenericActivator}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since Open-Xchange v6.16
 */
public class MessagingGenericActivator extends DeferredActivator {

    private List<ServiceTracker> trackers;

    private List<ServiceRegistration> registrations;

    public MessagingGenericActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] {
            DatabaseService.class, GenericConfigurationStorageService.class, ContextService.class, MessagingServiceRegistry.class,
            CacheService.class };
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        final org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory.getLog(MessagingGenericActivator.class);
        if (logger.isInfoEnabled()) {
            logger.info("Re-available service: " + clazz.getName());
        }
        getServiceRegistry().addService(clazz, getService(clazz));
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        final org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory.getLog(MessagingGenericActivator.class);
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
                for (int i = 0; i < classes.length; i++) {
                    final Object service = getService(classes[i]);
                    if (null != service) {
                        registry.addService(classes[i], service);
                    }
                }
            }
            
            {
                /*
                 * MessagingAccount region with 5 minutes time-out
                 */
                final String regionName = CachingMessagingAccountStorage.getRegionName();
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

            trackers = new ArrayList<ServiceTracker>();
            for (final ServiceTracker tracker : trackers) {
                tracker.open();
            }

            registrations = new ArrayList<ServiceRegistration>();
            registrations.add(context.registerService(UpdateTaskProviderService.class.getName(), new UpdateTaskProviderService() {

                public Collection<UpdateTask> getUpdateTasks() {
                    return Arrays.asList(((UpdateTask) new MessagingGenericCreateTableTask()));
                }
            }, null));
            registrations.add(context.registerService(DeleteListener.class.getName(), new MessagingGenericDeleteListener(), null));

        } catch (final Exception e) {
            org.apache.commons.logging.LogFactory.getLog(MessagingGenericActivator.class).error(e.getMessage(), e);
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
            if (null != registrations) {
                while (!registrations.isEmpty()) {
                    registrations.remove(0).unregister();
                }
                registrations = null;
            }
            final CacheService cacheService = getService(CacheService.class);
            if (null != cacheService) {
                cacheService.freeCache(CachingMessagingAccountStorage.getRegionName());
            }
            /*
             * Clear service registry
             */
            getServiceRegistry().clearRegistry();
        } catch (final Exception e) {
            org.apache.commons.logging.LogFactory.getLog(MessagingGenericActivator.class).error(e.getMessage(), e);
            throw e;
        }
    }

}
