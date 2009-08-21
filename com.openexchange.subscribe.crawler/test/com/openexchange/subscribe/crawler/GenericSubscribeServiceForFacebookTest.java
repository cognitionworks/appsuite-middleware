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

import java.util.LinkedList;
import java.util.List;

import org.ho.yaml.Yaml;

/**
 * @author <a href="mailto:karsten.will@open-xchange.com">Karsten Will</a>
 */
public class GenericSubscribeServiceForFacebookTest extends GenericSubscribeServiceTestHelpers {
	
	public void testGenericSubscribeServiceForFacebook(){
		// insert valid credentials here
		String username = "rodeldodel@wolke7.net";
		String password = "r0deld0del";
		
		//create a CrawlerDescription
		CrawlerDescription crawler = new CrawlerDescription();
		crawler.setDisplayName("Facebook");
		crawler.setId("com.openexchange.subscribe.crawler.facebook");
		List<Step> steps = new LinkedList<Step>(); 
        
        steps.add(new FacebookAPIStep(
        		"Get a user�s friend information from facebook via facebook-java-api", 
        		"",
        		username,
        		password,
        		"https://login.facebook.com/login.php?login_attempt=1",
        		"email",
        		"pass",
        		"(http://www.facebook.com/inbox/\\?ref=[a-z]*)"));

        Workflow workflow = new Workflow(steps);
        crawler.setWorkflowString(Yaml.dump(workflow));
        
        findOutIfThereAreContactsForThisConfiguration(username, password,crawler);
        //uncomment this if the if the crawler description was updated to get the new config-files
        //dumpThis(crawler,"conf/crawlers/", crawler.getDisplayName());
	}
}
