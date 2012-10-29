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

package com.openexchange.sessionstorage.hazelcast.osgi;

import org.apache.commons.logging.Log;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.core.HazelcastInstance;
import com.openexchange.config.ConfigurationService;
import com.openexchange.crypto.CryptoService;
import com.openexchange.log.LogFactory;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.sessionstorage.SessionStorageService;
import com.openexchange.sessionstorage.hazelcast.HazelcastSessionStorageConfiguration;
import com.openexchange.sessionstorage.hazelcast.HazelcastSessionStorageService;
import com.openexchange.sessionstorage.hazelcast.Services;
import com.openexchange.sessionstorage.hazelcast.exceptions.OXHazelcastSessionStorageExceptionCodes;

/**
 * {@link HazelcastSessionStorageActivator}
 * 
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public class HazelcastSessionStorageActivator extends HousekeepingActivator {

    private static Log LOG = LogFactory.getLog(HazelcastSessionStorageActivator.class);

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ConfigurationService.class, CryptoService.class, HazelcastInstance.class };
    }

    @Override
    protected void startBundle() throws Exception {
        LOG.info("Starting bundle: com.openexchange.sessionstorage.hazelcast");
        Services.setServiceLookup(this);
        final ConfigurationService configService = getService(ConfigurationService.class);
        final boolean enabled = configService.getBoolProperty("com.openexchange.sessionstorage.hazelcast.enabled", false);
        if (enabled) {
            final String encryptionKey = configService.getProperty("com.openexchange.sessionstorage.hazelcast.encryptionKey");
            if (encryptionKey == null) {
                throw OXHazelcastSessionStorageExceptionCodes.HAZELCAST_SESSIONSTORAGE_NO_ENCRYPTION_KEY.create();
            }
            final MapConfig mapConfig = new MapConfig();
            final String mapName = configService.getProperty("com.openexchange.sessionstorage.hazelcast.map.name");
            final int backupCount = configService.getIntProperty("com.openexchange.sessionstorage.hazelcast.map.backupcount", 1);
            final int asyncBackup = configService.getIntProperty("com.openexchange.sessionstorage.hazelcast.map.asyncbackup", 0);
            final int ttl = configService.getIntProperty("com.openexchange.sessionstorage.hazelcast.map.ttl", 0);
            final int maxidle = configService.getIntProperty("com.openexchange.sessionstorage.hazelcast.map.maxidle", 0);
            final String evictionPolicy = configService.getProperty("com.openexchange.sessionstorage.hazelcast.map.evictionpolicy");
            final int evictionPercentage = configService.getIntProperty("com.openexchange.sessionstorage.hazelcast.map.evictionpercentage", 25);
            final int maxSize = configService.getIntProperty("com.openexchange.sessionstorage.hazelcast.map.maxsize", 0);
            final String mergePolicy = configService.getProperty("com.openexchange.sessionstorage.hazelcast.map.mergepolicy");
            if (mapName == null || !checkEvictionPolicy(evictionPolicy) || !checkMergePolicy(mergePolicy)) {
                throw OXHazelcastSessionStorageExceptionCodes.HAZELCAST_SESSIONSTORAGE_CONFIG_FILE.create();
            }
            mapConfig.setName(mapName);
            mapConfig.setBackupCount(backupCount);
            mapConfig.setAsyncBackupCount(asyncBackup);
            mapConfig.setTimeToLiveSeconds(ttl);
            mapConfig.setMaxIdleSeconds(maxidle);
            mapConfig.setEvictionPolicy(evictionPolicy);
            mapConfig.setEvictionPercentage(evictionPercentage);
            final MaxSizeConfig maxSizeConfig = new MaxSizeConfig();
            maxSizeConfig.setSize(maxSize);
            mapConfig.setMaxSizeConfig(maxSizeConfig);
            mapConfig.setMergePolicy(mergePolicy);
            final HazelcastSessionStorageConfiguration config = new HazelcastSessionStorageConfiguration(encryptionKey, mapConfig);
            registerService(SessionStorageService.class, new HazelcastSessionStorageService(config));
        }
    }

    @Override
    public void stopBundle() throws Exception {
        LOG.info("Stopping bundle: com.openexchange.sessionstorage.hazelcast");
        cleanUp();
        Services.setServiceLookup(null);
    }

    private boolean checkEvictionPolicy(String evictionPolicy) {
        return ("NONE".equals(evictionPolicy) || "LRU".equals(evictionPolicy) || "LFU".equals(evictionPolicy));
    }

    private boolean checkMergePolicy(String mergePolicy) {
        return ("hz.NO_MERGE".equals(mergePolicy) || "hz.ADD_NEW_ENTRY".equals(mergePolicy) || "hz.HIGHER_HITS".equals(mergePolicy) || "hz.LATEST_UPDATE".equals(mergePolicy));
    }

}
