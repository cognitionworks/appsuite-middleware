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

package com.openexchange.file.storage.osgi;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.file.storage.FileStorageAccountManager;
import com.openexchange.file.storage.FileStorageAccountManagerLookupService;
import com.openexchange.file.storage.FileStorageAccountManagerProvider;
import com.openexchange.file.storage.FileStorageException;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.file.storage.FileStorageService;

/**
 * {@link OSGIFileStorageAccountManagerLookupService}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since Open-Xchange v6.18.2
 */
public class OSGIFileStorageAccountManagerLookupService implements FileStorageAccountManagerLookupService {

    /**
     * The backing list.
     */
    final List<FileStorageAccountManagerProvider> providers;

    /**
     * The tracker instance.
     */
    private ServiceTracker tracker;

    /**
     * Initializes a new {@link OSGIFileStorageAccountManagerLookupService}.
     */
    public OSGIFileStorageAccountManagerLookupService() {
        super();
        providers = new CopyOnWriteArrayList<FileStorageAccountManagerProvider>();
    }

    /**
     * Starts the tracker.
     * 
     * @param context The bundle context
     */
    public void start(final BundleContext context) {
        if (null == tracker) {
            tracker = new ServiceTracker(context, FileStorageAccountManagerProvider.class.getName(), new Customizer(context));
            tracker.open();
        }
    }

    /**
     * Stops the tracker.
     */
    public void stop() {
        if (null != tracker) {
            tracker.close();
            tracker = null;
        }
    }

    public FileStorageAccountManager getAccountManagerFor(final FileStorageService service) throws FileStorageException {
        FileStorageAccountManagerProvider candidate = null;
        for (final FileStorageAccountManagerProvider provider : providers) {
            if (provider.supports(service) && ((null == candidate) || (provider.getRanking() > candidate.getRanking()))) {
                candidate = provider;
            }
        }
        if (null == candidate) {
            throw FileStorageExceptionCodes.NO_ACCOUNT_MANAGER_FOR_SERVICE.create(service.getId());
        }
        return candidate.getAccountManagerFor(service);
    }

    private final class Customizer implements ServiceTrackerCustomizer {

        private final BundleContext context;

        Customizer(final BundleContext context) {
            super();
            this.context = context;
        }

        public Object addingService(final ServiceReference reference) {
            final Object service = context.getService(reference);
            if ((service instanceof FileStorageAccountManagerProvider)) {
                final FileStorageAccountManagerProvider addMe = (FileStorageAccountManagerProvider) service;
                synchronized (providers) {
                    if (!providers.contains(addMe)) {
                        providers.add(addMe);
                        return service;
                    }
                }
                final org.apache.commons.logging.Log logger =
                    org.apache.commons.logging.LogFactory.getLog(OSGIFileStorageAccountManagerLookupService.Customizer.class);
                if (logger.isWarnEnabled()) {
                    logger.warn(new StringBuilder(128).append("File storage account manager provider ").append(addMe.getClass().getSimpleName()).append(
                        " could not be added. Provider is already present.").toString());
                }
            }
            /*
             * Adding to registry failed
             */
            context.ungetService(reference);
            return null;
        }

        public void modifiedService(final ServiceReference reference, final Object service) {
            // Nothing to do
        }

        public void removedService(final ServiceReference reference, final Object service) {
            if (null != service) {
                try {
                    if (service instanceof FileStorageAccountManagerProvider) {
                        final FileStorageAccountManagerProvider removeMe = (FileStorageAccountManagerProvider) service;
                        synchronized (providers) {
                            providers.remove(removeMe);
                        }
                    }
                } finally {
                    context.ungetService(reference);
                }
            }
        }
    } // End of Customizer class

}
