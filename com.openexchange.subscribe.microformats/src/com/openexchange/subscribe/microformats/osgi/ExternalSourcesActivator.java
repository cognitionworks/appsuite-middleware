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

package com.openexchange.subscribe.microformats.osgi;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.server.osgiservice.DeferredActivator;
import com.openexchange.subscribe.SubscriptionSourceDiscoveryService;
import com.openexchange.subscribe.external.ExternalSubscriptionSourceDiscoveryService;
import com.openexchange.subscribe.microformats.OXMFParserFactoryService;
import com.openexchange.subscribe.microformats.parser.CybernekoOXMFFormParser;
import com.openexchange.subscribe.microformats.parser.HTMLMicroformatParserFactory;
import com.openexchange.subscribe.microformats.parser.OXMFFormParser;
import com.openexchange.timer.TimerService;


/**
 * {@link ExternalSourcesActivator}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 *
 */
public class ExternalSourcesActivator extends DeferredActivator {

    private static final Log LOG = LogFactory.getLog(ExternalSourcesActivator.class);
    
    private static final String SOURCES_LIST = "com.openexchange.subscribe.external.sources";

    private static final int MINUTES = 60 * 1000;

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[]{ConfigurationService.class, TimerService.class};
    }


    @Override
    protected void handleAvailability(Class<?> clazz) {
        tryConfig();
    }


    @Override
    protected void handleUnavailability(Class<?> clazz) {

    }


    @Override
    protected void startBundle() throws Exception {
        tryConfig();
    }


    @Override
    protected void stopBundle() throws Exception {
        
    }
    
    private void tryConfig() {
        ConfigurationService config = getService(ConfigurationService.class);
        TimerService timer = getService(TimerService.class);
        if(config == null || timer == null) {
            return;
        }
        String sourcesList = config.getProperty(SOURCES_LIST);
        
        OXMFParserFactoryService oxmfParserFactory = new HTMLMicroformatParserFactory();
        OXMFFormParser formParser = new CybernekoOXMFFormParser();
        
        if(sourcesList != null) {
            final List<ExternalSubscriptionSourceDiscoveryService> services = new ArrayList<ExternalSubscriptionSourceDiscoveryService>();
            for(String source : sourcesList.split("\\s*,\\s*")) {
                ExternalSubscriptionSourceDiscoveryService discoveryService = new ExternalSubscriptionSourceDiscoveryService(source, oxmfParserFactory, formParser);
                services.add(discoveryService);
                context.registerService(SubscriptionSourceDiscoveryService.class.getName(), discoveryService, null);
            }
            
            timer.scheduleAtFixedRate(new Runnable() {

                public void run() {
                    for (ExternalSubscriptionSourceDiscoveryService service : services) {
                        try {
                            service.refresh();
                        } catch (OXException x) {
                            LOG.error(x.getMessage(),x);
                        }
                    }
                }
                
            }, 0, 30 *MINUTES);
            
        }
        
        
        
    }

    
    

}
