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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.tools.oxfolder.property.osgi;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;
import com.openexchange.caching.CacheService;
import com.openexchange.database.CreateTableService;
import com.openexchange.database.DatabaseService;
import com.openexchange.groupware.update.UpdateTaskProviderService;
import com.openexchange.groupware.update.UpdateTaskV2;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.tools.oxfolder.property.FolderUserPropertyStorage;
import com.openexchange.tools.oxfolder.property.impl.CachingFolderUserPropertyStorage;
import com.openexchange.tools.oxfolder.property.impl.RdbFolderUserPropertyStorage;
import com.openexchange.tools.oxfolder.property.sql.CreateFolderUserPropertyTable;
import com.openexchange.tools.oxfolder.property.sql.CreateFolderUserPropertyTask;

/**
 * {@link FolderUserPropertyActivator}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.0
 */
public class FolderUserPropertyActivator extends HousekeepingActivator {

    /**
     * Initializes a new {@link FolderUserPropertyActivator}.
     *
     */
    public FolderUserPropertyActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { DatabaseService.class, CacheService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        // Register UpdateTask
        DatabaseService dbService = getService(DatabaseService.class);
        if (null == dbService) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(DatabaseService.class.getName());
        }
        final CreateFolderUserPropertyTask task = new CreateFolderUserPropertyTask(dbService);

        registerService(UpdateTaskProviderService.class, new UpdateTaskProviderService() {

            @Override
            public Collection<? extends UpdateTaskV2> getUpdateTasks() {
                return Arrays.asList(task);
            }

        });
        registerService(CreateTableService.class, new CreateFolderUserPropertyTable());

        // Initialize cache region
        {
            String regionName = CachingFolderUserPropertyStorage.getRegionName();
            byte[] ccf = ("jcs.region."+regionName+"=LTCP\n" +
                "jcs.region."+regionName+".cacheattributes=org.apache.jcs.engine.CompositeCacheAttributes\n" +
                "jcs.region."+regionName+".cacheattributes.MaxObjects=1000000\n" +
                "jcs.region."+regionName+".cacheattributes.MemoryCacheName=org.apache.jcs.engine.memory.lru.LRUMemoryCache\n" +
                "jcs.region."+regionName+".cacheattributes.UseMemoryShrinker=true\n" +
                "jcs.region."+regionName+".cacheattributes.MaxMemoryIdleTimeSeconds=360\n" +
                "jcs.region."+regionName+".cacheattributes.ShrinkerIntervalSeconds=60\n" +
                "jcs.region."+regionName+".elementattributes=org.apache.jcs.engine.ElementAttributes\n" +
                "jcs.region."+regionName+".elementattributes.IsEternal=false\n" +
                "jcs.region."+regionName+".elementattributes.MaxLifeSeconds=-1\n" +
                "jcs.region."+regionName+".elementattributes.IdleTime=360\n" +
                "jcs.region."+regionName+".elementattributes.IsSpool=false\n" +
                "jcs.region."+regionName+".elementattributes.IsRemote=false\n" +
                "jcs.region."+regionName+".elementattributes.IsLateral=false\n").getBytes();
            getService(CacheService.class).loadConfiguration(new ByteArrayInputStream(ccf));
        }

        // Register FolderUserPropertyStorage
        registerService(FolderUserPropertyStorage.class, new CachingFolderUserPropertyStorage(new RdbFolderUserPropertyStorage(this)));
    }

}
