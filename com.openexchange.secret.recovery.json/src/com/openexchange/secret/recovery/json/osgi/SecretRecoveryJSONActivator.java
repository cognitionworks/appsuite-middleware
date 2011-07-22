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

package com.openexchange.secret.recovery.json.osgi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import com.openexchange.groupware.settings.PreferencesItemService;
import com.openexchange.multiple.MultipleHandler;
import com.openexchange.multiple.MultipleHandlerFactoryService;
import com.openexchange.secret.SecretService;
import com.openexchange.secret.recovery.SecretInconsistencyDetector;
import com.openexchange.secret.recovery.SecretMigrator;
import com.openexchange.secret.recovery.json.SecretRecoveryMultipleHandler;
import com.openexchange.secret.recovery.json.SecretRecoveryServlet;
import com.openexchange.secret.recovery.json.preferences.Enabled;
import com.openexchange.server.osgiservice.DeferredActivator;
import com.openexchange.server.osgiservice.Whiteboard;
import com.openexchange.tools.service.SessionServletRegistration;

public class SecretRecoveryJSONActivator extends DeferredActivator{
    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(SecretRecoveryJSONActivator.class));
    
    private static final Class<?>[] NEEDED_SERVICES = new Class<?>[]{SecretMigrator.class, SecretInconsistencyDetector.class, SecretService.class};
    private ServiceRegistration registration;
    private ServiceRegistration enabledReg;

    private SessionServletRegistration servletRegistration;

    @Override
    protected Class<?>[] getNeededServices() {
        return NEEDED_SERVICES;
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            final Whiteboard whiteboard = new Whiteboard(context);
            
            final SecretService secretService = whiteboard.getService(SecretService.class);
            final SecretMigrator migrator = whiteboard.getService(SecretMigrator.class);
            final SecretInconsistencyDetector detector = whiteboard.getService(SecretInconsistencyDetector.class);
            
            SecretRecoveryServlet.detector = detector;
            SecretRecoveryServlet.migrator = migrator;
            SecretRecoveryServlet.secretService = secretService;
            
            servletRegistration = new SessionServletRegistration(context, new SecretRecoveryServlet(), "ajax/recovery/secret");
            servletRegistration.open();
            
            registration = context.registerService(MultipleHandlerFactoryService.class.getName(), new MultipleHandlerFactoryService() {

                public MultipleHandler createMultipleHandler() {
                    return new SecretRecoveryMultipleHandler(detector, migrator, secretService);
                }

                public String getSupportedModule() {
                    return "recovery/secret";
                }
                
            }, null);
            enabledReg = context.registerService(PreferencesItemService.class.getName(), new Enabled(), null);
            
        } catch (final Exception x) {
            LOG.error(x.getMessage(), x);
        }
    
    }

    @Override
    protected void stopBundle() throws Exception {
        if (servletRegistration != null) {
            servletRegistration.close();
            servletRegistration = null;
        }
        if(registration != null) {
            registration.unregister();
        }
        if(enabledReg != null) {
            enabledReg.unregister();
        }
    }


}
