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

package com.openexchange.conversion.engine.osgi;

import static com.openexchange.conversion.engine.internal.ConversionEngineRegistry.getInstance;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.conversion.DataHandler;
import com.openexchange.conversion.DataSource;
import com.openexchange.conversion.engine.internal.ConversionEngineRegistry;

/**
 * {@link ConversionEngineCustomizer} - The service tracker customizer for conversion engine.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ConversionEngineCustomizer implements ServiceTrackerCustomizer {

    private static final String PROP_IDENTIFIER = "identifier";

    private static final org.apache.commons.logging.Log LOG =
        org.apache.commons.logging.LogFactory.getLog(ConversionEngineCustomizer.class);

    private final BundleContext context;

    /**
     * Initializes a new {@link ConversionEngineCustomizer}
     * 
     * @param context The bundle context
     */
    public ConversionEngineCustomizer(final BundleContext context) {
        super();
        this.context = context;
    }

    public Object addingService(final ServiceReference reference) {
        final Object addedService = context.getService(reference);
        if (null == addedService) {
            LOG.warn("Added service is null!", new Throwable());
        }
        if (addedService instanceof DataHandler) {
            final Object identifier = reference.getProperty(PROP_IDENTIFIER);
            if (null == identifier) {
                LOG.error("Missing identifier in data handler: " + addedService.getClass().getName());
                return addedService;
            }
            final ConversionEngineRegistry registry = getInstance();
            synchronized (registry) {
                if (registry.getDataHandler(identifier.toString()) != null) {
                    LOG.error("A data handler is already registered for identifier: " + identifier.toString());
                    return addedService;
                }
                registry.putDataHandler(identifier.toString(), (DataHandler) addedService);
                LOG.info(new StringBuilder(64).append("Data handler for identifier '").append(identifier.toString()).append(
                    "' successfully registered"));
            }
        } else if (addedService instanceof DataSource) {
            final Object identifier = reference.getProperty(PROP_IDENTIFIER);
            if (null == identifier) {
                LOG.error("Missing identifier in data source: " + addedService.getClass().getName());
                return addedService;
            }
            final ConversionEngineRegistry registry = getInstance();
            synchronized (registry) {
                if (registry.getDataSource(identifier.toString()) != null) {
                    LOG.error("A data source is already registered for identifier: " + identifier.toString());
                    return addedService;
                }
                registry.putDataSource(identifier.toString(), (DataSource) addedService);
                LOG.info(new StringBuilder(64).append("Data source for identifier '").append(identifier.toString()).append(
                    "' successfully registered"));
            }
        }
        return addedService;
    }

    public void modifiedService(final ServiceReference reference, final Object service) {
        // Nothing to do
    }

    public void removedService(final ServiceReference reference, final Object service) {
        try {
            if (service instanceof DataHandler) {
                final Object identifier = reference.getProperty(PROP_IDENTIFIER);
                if (null == identifier) {
                    LOG.error("Missing identifier in data handler: " + service.getClass().getName());
                    return;
                }
                getInstance().removeDataHandler(identifier.toString());
                LOG.info(new StringBuilder(64).append("Data handler for identifier '").append(identifier.toString()).append(
                    "' successfully unregistered"));
            } else if (service instanceof DataSource) {
                final Object identifier = reference.getProperty(PROP_IDENTIFIER);
                if (null == identifier) {
                    LOG.error("Missing identifier in data source: " + service.getClass().getName());
                    return;
                }
                getInstance().removeDataSource(identifier.toString());
                LOG.info(new StringBuilder(64).append("Data source for identifier '").append(identifier.toString()).append(
                    "' successfully unregistered"));
            }
        } finally {
            context.ungetService(reference);
        }

    }

}
