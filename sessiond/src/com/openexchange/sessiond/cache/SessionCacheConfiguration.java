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

package com.openexchange.sessiond.cache;

import static com.openexchange.sessiond.services.SessiondServiceRegistry.getServiceRegistry;
import com.openexchange.caching.CacheService;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.server.Initialization;
import com.openexchange.sessiond.SessionExceptionCodes;

/**
 * {@link SessionCacheConfiguration} - Configures the session cache
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SessionCacheConfiguration implements Initialization {

    private static final org.apache.commons.logging.Log LOG = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(SessionCacheConfiguration.class));

    private static final SessionCacheConfiguration instance = new SessionCacheConfiguration();

    /**
     * No instantiation
     */
    private SessionCacheConfiguration() {
        super();
    }

    /**
     * Gets the singleton instance of {@link SessionCacheConfiguration}
     *
     * @return The singleton instance of {@link SessionCacheConfiguration}
     */
    public static SessionCacheConfiguration getInstance() {
        return instance;
    }

    public void start() throws OXException {
        final ConfigurationService configurationService = getServiceRegistry().getService(ConfigurationService.class);
        if (null == configurationService) {
            throw SessionExceptionCodes.SESSIOND_CONFIG_EXCEPTION.create();
        }
        final String cacheConfigFile = configurationService.getProperty("com.openexchange.sessiond.sessionCacheConfig");
        if (cacheConfigFile == null) {
            /*
             * Not found
             */
            final OXException e = SessionExceptionCodes.MISSING_PROPERTY.create("com.openexchange.sessiond.sessionCacheConfig");
            if (LOG.isWarnEnabled()) {
                LOG.warn(new StringBuilder(128).append("Cannot setup lateral session cache: ").append(e.getMessage()).toString(), e);
            }
            return;
        }
        if (LOG.isInfoEnabled()) {
            LOG.info(new StringBuilder("Sessiond property: com.openexchange.sessiond.sessionCacheConfig=").append(cacheConfigFile).toString());
        }
        final CacheService cacheService = getServiceRegistry().getService(CacheService.class);
        if (null != cacheService) {
            try {
                cacheService.loadConfiguration(cacheConfigFile.trim());
            } catch (final OXException e) {
                throw e;
            }
        }
    }

    public void stop() {
        final CacheService cacheService = getServiceRegistry().getService(CacheService.class);
        if (null != cacheService) {
            try {
                cacheService.freeCache(SessionCache.LATERAL_REGION_NAME);
                cacheService.freeCache(SessionCache.REGION_NAME);
            } catch (final OXException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }
}
