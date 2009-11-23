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

package com.openexchange.subscribe.crawler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import org.ho.yaml.Yaml;
import com.openexchange.config.SimConfigurationService;
import com.openexchange.subscribe.crawler.osgi.Activator;

/**
 * @author <a href="mailto:karsten.will@open-xchange.com">Karsten Will</a>
 */
public class ActivatorTest extends GenericSubscribeServiceTestHelpers {

    /**
     * Get all yml-files in the config directory and create crawlers out of them. Use each crawler with a specified testuser.
     */
    public void testActivator() {
        
        HashMap<String, String> map = null;
        try {
            map = (HashMap<String, String>) Yaml.load(getSecretsFile());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        SimConfigurationService config = new SimConfigurationService();
        //test with the real crawlers
        config.stringProperties.put("com.openexchange.subscribe.crawler.path", "conf/crawlers/");
        Activator activator = new Activator();
        ArrayList<CrawlerDescription> crawlers = activator.getCrawlersFromFilesystem(config);

        for (CrawlerDescription crawler : crawlers) {
            String crawlerName = crawler.getDisplayName();
            if (map.containsKey(crawlerName+"_user") && map.containsKey(crawlerName+"_password")){
                String username = map.get(crawlerName+"_user");
                String password = map.get(crawlerName+"_password");
                System.out.println("***** Testing crawler : " + crawlerName);
                findOutIfThereAreContactsForThisConfiguration(username, password, crawler, true);
            } else {
                fail("***** No credentials for crawler : " + crawlerName);
            }
            
        }

    }

    private File getSecretsFile() {
        String value = System.getProperty("secretFile");
        if (null == value) {
            fail("File for crawler credentials is not defined.");
        }
        File secrets = new File(value);
        if (!secrets.exists()) {
            fail("File for crawler credentials does not exist.");
        }
        if (!secrets.isFile()) {
            fail("File for crawler credentials is not a file.");
        }
        if (!secrets.canRead()) {
            fail("File for crawler credentials can not be read.");
        }
        return secrets;
    }
}
