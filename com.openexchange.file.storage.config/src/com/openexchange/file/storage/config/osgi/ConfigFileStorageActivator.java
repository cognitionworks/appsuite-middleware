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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.file.storage.config.osgi;

import java.util.Properties;
import com.openexchange.config.ConfigurationService;
import com.openexchange.file.storage.FileStorageAccountManagerProvider;
import com.openexchange.file.storage.config.ConfigFileStorageAccountManagerProvider;
import com.openexchange.file.storage.config.ConfigFileStorageAccountParser;
import com.openexchange.file.storage.config.services.ConfigFileStorageServiceRegistry;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.osgi.ServiceRegistry;

/**
 * {@link ConfigFileStorageActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since Open-Xchange v6.18.2
 */
public final class ConfigFileStorageActivator extends HousekeepingActivator {

    /**
     * Initializes a new {@link ConfigFileStorageActivator}.
     */
    public ConfigFileStorageActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ConfigurationService.class };
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        final org.apache.commons.logging.Log logger = com.openexchange.log.Log.valueOf(com.openexchange.log.LogFactory.getLog(ConfigFileStorageActivator.class));
        if (logger.isWarnEnabled()) {
            logger.warn("Absent service: " + clazz.getName());
        }
        if (ConfigurationService.class.equals(clazz)) {
            dropFileStorageProperties();
        }
        ConfigFileStorageServiceRegistry.getServiceRegistry().removeService(clazz);
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        final org.apache.commons.logging.Log logger = com.openexchange.log.Log.valueOf(com.openexchange.log.LogFactory.getLog(ConfigFileStorageActivator.class));
        if (logger.isInfoEnabled()) {
            logger.info("Re-available service: " + clazz.getName());
        }
        ConfigFileStorageServiceRegistry.getServiceRegistry().addService(clazz, getService(clazz));
        if (ConfigurationService.class.equals(clazz)) {
            parseFileStorageProperties(getService(ConfigurationService.class));
        }
    }

    @Override
    protected void startBundle() throws Exception {
        final org.apache.commons.logging.Log log = com.openexchange.log.Log.valueOf(com.openexchange.log.LogFactory.getLog(ConfigFileStorageActivator.class));
        try {
            if (log.isInfoEnabled()) {
                log.info("starting bundle: com.openexchange.file.storage.config");
            }
            /*
             * (Re-)Initialize service registry with available services
             */
            final ServiceRegistry registry = ConfigFileStorageServiceRegistry.getServiceRegistry();
            registry.clearRegistry();
            final Class<?>[] classes = getNeededServices();
            for (final Class<?> classe : classes) {
                final Object service = getService(classe);
                if (null != service) {
                    registry.addService(classe, service);
                }
            }
            /*
             * Parse file storage configuration
             */
            parseFileStorageProperties(registry.getService(ConfigurationService.class, true));
            /*
             * Register services
             */
            registerService(FileStorageAccountManagerProvider.class, new ConfigFileStorageAccountManagerProvider(), null);
        } catch (final Exception e) {
            log.error("Starting bundle \"com.openexchange.file.storage.config\" failed.", e);
            throw e;
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        final org.apache.commons.logging.Log log = com.openexchange.log.Log.valueOf(com.openexchange.log.LogFactory.getLog(ConfigFileStorageActivator.class));
        try {
            if (log.isInfoEnabled()) {
                log.info("stopping bundle: com.openexchange.file.storage.config");
            }
            cleanUp();
            dropFileStorageProperties();
        } catch (final Exception e) {
            log.error("Stopping bundle \"com.openexchange.file.storage.config\" failed.", e);
            throw e;
        }
    }

    private void parseFileStorageProperties(final ConfigurationService configurationService) {
        final Properties fsProperties = configurationService.getFile("filestorage.properties");
        ConfigFileStorageAccountParser.getInstance().parse(fsProperties);
    }

    private void dropFileStorageProperties() {
        ConfigFileStorageAccountParser.getInstance().drop();
    }

}
