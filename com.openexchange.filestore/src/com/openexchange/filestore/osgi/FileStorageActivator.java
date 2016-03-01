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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.filestore.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import com.openexchange.filestore.FileStorage2EntitiesResolver;
import com.openexchange.filestore.FileStorageService;
import com.openexchange.filestore.FileStorageUnregisterListenerRegistry;
import com.openexchange.filestore.FileStorages;
import com.openexchange.filestore.QuotaFileStorageService;


/**
 * {@link FileStorageActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.0
 */
public class FileStorageActivator implements BundleActivator {

    private volatile ServiceTracker<FileStorageService, FileStorageService> fsTracker;
    private volatile ServiceTracker<QuotaFileStorageService, QuotaFileStorageService> qfsTracker;
    private volatile ServiceTracker<FileStorage2EntitiesResolver, FileStorage2EntitiesResolver> resolverTracker;
    private volatile TrackingFileStorageUnregisterListenerRegistry listenerRegistry;
    private volatile ServiceRegistration<FileStorageUnregisterListenerRegistry> listenerRegistryRegistration;

    /**
     * Initializes a new {@link FileStorageActivator}.
     */
    public FileStorageActivator() {
        super();
    }

    @Override
    public void start(BundleContext context) throws Exception {
        try {
            ServiceTracker<FileStorageService, FileStorageService> fsTracker = new ServiceTracker<FileStorageService, FileStorageService>(context, FileStorageService.class, new FSTrackerCustomizer(context));
            this.fsTracker = fsTracker;

            ServiceTracker<QuotaFileStorageService, QuotaFileStorageService> qfsTracker = new ServiceTracker<QuotaFileStorageService, QuotaFileStorageService>(context, QuotaFileStorageService.class, new QFSTrackerCustomizer(context));
            this.qfsTracker = qfsTracker;

            ServiceTracker<FileStorage2EntitiesResolver, FileStorage2EntitiesResolver> resolverTracker = new ServiceTracker<FileStorage2EntitiesResolver, FileStorage2EntitiesResolver>(context, FileStorage2EntitiesResolver.class, new ResolverTrackerCustomizer(context));
            this.resolverTracker = resolverTracker;

            TrackingFileStorageUnregisterListenerRegistry listenerRegistry = new TrackingFileStorageUnregisterListenerRegistry(context);
            this.listenerRegistry = listenerRegistry;

            fsTracker.open();
            qfsTracker.open();
            resolverTracker.open();
            listenerRegistry.open();

            listenerRegistryRegistration = context.registerService(FileStorageUnregisterListenerRegistry.class, listenerRegistry, null);
        } catch (Exception e) {
            Logger logger = org.slf4j.LoggerFactory.getLogger(FileStorageActivator.class);
            logger.error("Failed to start {}", context.getBundle().getSymbolicName(), e);
            throw e;
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        try {
            ServiceRegistration<FileStorageUnregisterListenerRegistry> listenerRegistryRegistration = this.listenerRegistryRegistration;
            if (null != listenerRegistryRegistration) {
                listenerRegistryRegistration.unregister();
                this.listenerRegistryRegistration = null;
            }

            ServiceTracker<FileStorageService, FileStorageService> fsTracker = this.fsTracker;
            if (null != fsTracker) {
                fsTracker.close();
                this.fsTracker = null;
            }

            ServiceTracker<QuotaFileStorageService, QuotaFileStorageService> qfsTracker = this.qfsTracker;
            if (null != qfsTracker) {
                qfsTracker.close();
                this.qfsTracker = null;
            }

            ServiceTracker<FileStorage2EntitiesResolver, FileStorage2EntitiesResolver> resolverTracker = this.resolverTracker;
            if (null != resolverTracker) {
                resolverTracker.close();
                this.resolverTracker = null;
            }

            TrackingFileStorageUnregisterListenerRegistry listenerRegistry = this.listenerRegistry;
            if (null != listenerRegistry) {
                listenerRegistry.close();
                this.listenerRegistry = null;
            }
        } catch (Exception e) {
            Logger logger = org.slf4j.LoggerFactory.getLogger(FileStorageActivator.class);
            logger.error("Failed to stop {}", context.getBundle().getSymbolicName(), e);
            throw e;
        }
    }

    private static class FSTrackerCustomizer implements ServiceTrackerCustomizer<FileStorageService, FileStorageService> {

        private final BundleContext context;

        FSTrackerCustomizer(final BundleContext context) {
            this.context = context;
        }

        @Override
        public FileStorageService addingService(ServiceReference<FileStorageService> reference) {
            final FileStorageService service = context.getService(reference);
            FileStorages.setFileStorageService(service);
            return service;
        }

        @Override
        public void modifiedService(ServiceReference<FileStorageService> reference, FileStorageService service) {
            // Nothing to do here

        }

        @Override
        public void removedService(ServiceReference<FileStorageService> reference, FileStorageService service) {
            FileStorages.setFileStorageService(null);
            context.ungetService(reference);
        }

    }

    private static class QFSTrackerCustomizer implements ServiceTrackerCustomizer<QuotaFileStorageService, QuotaFileStorageService> {

        private final BundleContext context;

        QFSTrackerCustomizer(final BundleContext context) {
            this.context = context;
        }

        @Override
        public QuotaFileStorageService addingService(ServiceReference<QuotaFileStorageService> reference) {
            QuotaFileStorageService service = context.getService(reference);
            FileStorages.setQuotaFileStorageService(service);
            return service;
        }

        @Override
        public void modifiedService(ServiceReference<QuotaFileStorageService> reference, QuotaFileStorageService service) {
            // Nothing to do here

        }

        @Override
        public void removedService(ServiceReference<QuotaFileStorageService> reference, QuotaFileStorageService service) {
            FileStorages.setQuotaFileStorageService(null);
            context.ungetService(reference);
        }

    }

    private static class ResolverTrackerCustomizer implements ServiceTrackerCustomizer<FileStorage2EntitiesResolver, FileStorage2EntitiesResolver> {

        private final BundleContext context;

        ResolverTrackerCustomizer(final BundleContext context) {
            this.context = context;
        }

        @Override
        public FileStorage2EntitiesResolver addingService(ServiceReference<FileStorage2EntitiesResolver> reference) {
            FileStorage2EntitiesResolver service = context.getService(reference);
            FileStorages.setFileStorage2EntitiesResolver(service);
            return service;
        }

        @Override
        public void modifiedService(ServiceReference<FileStorage2EntitiesResolver> reference, FileStorage2EntitiesResolver service) {
            // Nothing to do here

        }

        @Override
        public void removedService(ServiceReference<FileStorage2EntitiesResolver> reference, FileStorage2EntitiesResolver service) {
            FileStorages.setFileStorage2EntitiesResolver(null);
            context.ungetService(reference);
        }

    }

}
