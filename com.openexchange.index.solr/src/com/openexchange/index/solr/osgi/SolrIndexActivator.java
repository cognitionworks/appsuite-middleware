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

package com.openexchange.index.solr.osgi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.junit.runner.JUnitCore;

import com.openexchange.config.ConfigurationService;
import com.openexchange.database.DatabaseService;
import com.openexchange.index.IndexFacadeService;
import com.openexchange.index.solr.SolrIndexFacadeTest;
import com.openexchange.index.solr.internal.Services;
import com.openexchange.index.solr.internal.SolrIndexFacadeService;
import com.openexchange.langdetect.LanguageDetectionService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.solr.SolrAccessService;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.timer.TimerService;
import com.openexchange.user.UserService;

/**
 * {@link SolrIndexActivator} - The activator of the index bundle.
 * 
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class SolrIndexActivator extends HousekeepingActivator {

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(SolrIndexActivator.class));
    

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] {
            DatabaseService.class, UserService.class, ConfigurationService.class, TimerService.class, ThreadPoolService.class,
            LanguageDetectionService.class, SolrAccessService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        LOG.info("Starting Bundle com.openexchange.index.solr");
        Services.setServiceLookup(this);

        final SolrIndexFacadeService solrFacadeService = new SolrIndexFacadeService();
        registerService(IndexFacadeService.class, solrFacadeService);
        addService(IndexFacadeService.class, solrFacadeService);
        registerService(CommandProvider.class, new UtilCommandProvider());
        
//        final SolrCoreConfigService indexService = new SolrCoreConfigServiceImpl();
//        registerService(SolrCoreConfigService.class, indexService);

        /*
         * Register UpdateTasks and DeleteListener. Uncomment for production. final DatabaseService dbService =
         * getService(DatabaseService.class); final CreateTableService createTableService = new IndexCreateTableService();
         * registerService(CreateTableService.class, createTableService); registerService(UpdateTaskProviderService.class, new
         * IndexUpdateTaskProviderService( new CreateTableUpdateTask(createTableService, new String[0], Schema.NO_VERSION, dbService), new
         * IndexCreateServerTableTask(dbService) )); registerService(DeleteListener.class, new IndexDeleteListener(indexService));
         */
    }
    
    public class UtilCommandProvider implements CommandProvider {

        public UtilCommandProvider() {
            super();
        }

        public String getHelp() {
            final StringBuilder help = new StringBuilder();
            help.append("\tstartTest - Start SolrIndexFacadeTest.\n");
            return help.toString();
        }

        public void _startTest(final CommandInterpreter commandInterpreter) {
            final JUnitCore jUnit = new JUnitCore();
            jUnit.run(SolrIndexFacadeTest.class);
        }
    }
}
