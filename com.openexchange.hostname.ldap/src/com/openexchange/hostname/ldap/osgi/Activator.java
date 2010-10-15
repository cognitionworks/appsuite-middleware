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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.hostname.ldap.osgi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceRegistration;
import com.openexchange.caching.CacheException;
import com.openexchange.caching.CacheService;
import com.openexchange.config.ConfigurationService;
import com.openexchange.groupware.notify.hostname.HostnameService;
import com.openexchange.hostname.ldap.LDAPHostnameCache;
import com.openexchange.hostname.ldap.LDAPHostnameService;
import com.openexchange.hostname.ldap.configuration.ConfigurationException;
import com.openexchange.hostname.ldap.configuration.LDAPHostnameProperties;
import com.openexchange.hostname.ldap.configuration.Property;
import com.openexchange.hostname.ldap.services.HostnameLDAPServiceRegistry;
import com.openexchange.server.osgiservice.DeferredActivator;
import com.openexchange.server.osgiservice.ServiceRegistry;

public class Activator extends DeferredActivator {

    private static transient final Log LOG = LogFactory.getLog(Activator.class);

    // add services which we need in our plugins later
    private static final Class<?>[] NEEDED_SERVICES = { CacheService.class, ConfigurationService.class };

    private ServiceRegistration hostname_registration;

    private LDAPHostnameService hostnameservice;

    public Activator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return NEEDED_SERVICES;
    }

    @Override
    protected void handleAvailability(Class<?> clazz) {
        if (LOG.isWarnEnabled()) {
            LOG.warn("Absent service: " + clazz.getName());
        }

        HostnameLDAPServiceRegistry.getServiceRegistry().addService(clazz, getService(clazz));
    }

    @Override
    protected void handleUnavailability(Class<?> clazz) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Re-available service: " + clazz.getName());
        }

        HostnameLDAPServiceRegistry.getServiceRegistry().removeService(clazz);
    }

    @Override
    protected void startBundle() throws Exception {

        // try to load all the needed services like config service and hostnameservice
        try {
            final ServiceRegistry registry = HostnameLDAPServiceRegistry.getServiceRegistry();
            registry.clearRegistry();
            final Class<?>[] classes = getNeededServices();
            for (int i = 0; i < classes.length; i++) {
                final Object service = getService(classes[i]);
                if (null != service) {
                    registry.addService(classes[i], service);
                }
            }

            checkConfiguration();
            
            activateCaching();
            
            // register hostname service to modify hostnames in directlinks, this will also init the cache class
            hostnameservice = new LDAPHostnameService();
            
            LDAPHostnameCache.getInstance().outputSettings();
            
            hostname_registration = context.registerService(HostnameService.class.getName(), hostnameservice, null);

        } catch (final Throwable t) {
            LOG.error(t.getMessage(), t);
            throw t instanceof Exception ? (Exception) t : new Exception(t);
        }

    }

    @Override
    protected void stopBundle() throws Exception {
        try {
            // stop hostname service
            hostname_registration.unregister();
            hostnameservice = null;

            deactivateCaching();

            HostnameLDAPServiceRegistry.getServiceRegistry().clearRegistry();
        } catch (final Throwable t) {
            LOG.error(t.getMessage(), t);
            throw t instanceof Exception ? (Exception) t : new Exception(t);
        }
    }

    private void activateCaching() throws ConfigurationException {
        final String cacheConfigFile = LDAPHostnameProperties.getProperty(HostnameLDAPServiceRegistry.getServiceRegistry().getService(ConfigurationService.class), Property.cache_config_file);
        try {
            HostnameLDAPServiceRegistry.getServiceRegistry().getService(CacheService.class).loadConfiguration(cacheConfigFile.trim());
        } catch (final CacheException e) {
            throw new ConfigurationException(e);
        }
    }

    private void checkConfiguration() throws ConfigurationException {
        LDAPHostnameProperties.check(HostnameLDAPServiceRegistry.getServiceRegistry(), Property.values(), LDAPHostnameCache.REGION_NAME);
    }

    private void deactivateCaching() {
        LDAPHostnameCache.releaseInstance();
        final CacheService cacheService = HostnameLDAPServiceRegistry.getServiceRegistry().getService(CacheService.class);
        if (null != cacheService) {
            try {
                cacheService.freeCache(LDAPHostnameCache.REGION_NAME);
            } catch (final CacheException e) {
                LOG.error(e.getMessage(), e);
            }
        }

    }

}
