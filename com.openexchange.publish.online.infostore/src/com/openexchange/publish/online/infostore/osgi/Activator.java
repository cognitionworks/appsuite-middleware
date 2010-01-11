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

package com.openexchange.publish.online.infostore.osgi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import com.openexchange.context.ContextService;
import com.openexchange.groupware.infostore.InfostoreFacade;
import com.openexchange.publish.PublicationDataLoaderService;
import com.openexchange.publish.PublicationService;
import com.openexchange.publish.online.infostore.InfostoreDocumentPublicationService;
import com.openexchange.publish.online.infostore.InfostorePublicationServlet;
import com.openexchange.server.osgiservice.DeferredActivator;
import com.openexchange.user.UserService;
import com.openexchange.userconf.UserConfigurationService;

public class Activator extends DeferredActivator {

    private static final Log LOG = LogFactory.getLog(Activator.class);

    private static final String ALIAS = InfostoreDocumentPublicationService.PREFIX+"*";
    private static final Class<?>[] NEEDED_SERVICES = {HttpService.class, PublicationDataLoaderService.class, ContextService.class, InfostoreFacade.class, UserService.class, UserConfigurationService.class };
    private ServiceRegistration serviceRegistration;
    private InfostorePublicationServlet servlet;


    @Override
    protected Class<?>[] getNeededServices() {
        return NEEDED_SERVICES;
    }

    @Override
    protected void handleAvailability(Class<?> clazz) {
            registerServlet();
    }

    @Override
    protected void handleUnavailability(Class<?> clazz) {
        unregisterServlet();
    }

    @Override
    protected void startBundle() throws Exception {
        InfostoreDocumentPublicationService infostorePublisher = new InfostoreDocumentPublicationService();
        InfostorePublicationServlet.setInfostoreDocumentPublicationService(infostorePublisher);
        serviceRegistration = context.registerService(PublicationService.class.getName(), infostorePublisher, null);
        
        registerServlet();

    }
    @Override
    protected void stopBundle() throws Exception {
        InfostorePublicationServlet.setInfostoreDocumentPublicationService(null);
        serviceRegistration.unregister();
        
        unregisterServlet();
    }
    
    private void unregisterServlet() {
        InfostorePublicationServlet.setContextService(null);
        InfostorePublicationServlet.setPublicationDataLoaderService(null);
        
        HttpService httpService = getService(HttpService.class);
        if(httpService != null && servlet != null) {
            httpService.unregister(ALIAS);
            servlet = null;
        }
        
    }

    private void registerServlet() {
        HttpService httpService = getService(HttpService.class);
        if(httpService == null) {
            return;
        }
        
        PublicationDataLoaderService dataLoader = getService(PublicationDataLoaderService.class);
        if(dataLoader == null) {
            return;
        }
        
        ContextService contexts = getService(ContextService.class);
        if(contexts == null) {
            return;
        }   
        
        UserService users = getService(UserService.class);
        if(users == null) {
            return;
        }
        
        UserConfigurationService userConfigs = getService(UserConfigurationService.class);
        if(userConfigs == null) {
            return;
        }
        
        InfostoreFacade infostore = getService(InfostoreFacade.class);
        if(infostore == null) {
            return;
        }
        
        InfostorePublicationServlet.setContextService(contexts);
        InfostorePublicationServlet.setUserService(users);
        InfostorePublicationServlet.setUserConfigService(userConfigs);
        
        InfostorePublicationServlet.setInfostoreFacade(infostore);
        InfostorePublicationServlet.setPublicationDataLoaderService(dataLoader);
        
        if(servlet == null) {
            try {
                httpService.registerServlet(ALIAS, servlet = new InfostorePublicationServlet(), null, null);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        
    }
    

}
