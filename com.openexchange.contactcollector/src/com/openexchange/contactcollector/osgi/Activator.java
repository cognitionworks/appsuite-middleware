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

package com.openexchange.contactcollector.osgi;

import java.util.ArrayList;
import java.util.List;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;
import com.openexchange.contactcollector.ContactCollectorService;
import com.openexchange.contactcollector.folder.ContactCollectorFolderCreator;
import com.openexchange.contactcollector.internal.ContactCollectorServiceImpl;
import com.openexchange.contactcollector.preferences.ContactCollectEnabled;
import com.openexchange.contactcollector.preferences.ContactCollectFolder;
import com.openexchange.contactcollector.preferences.ContactCollectOnMailAccess;
import com.openexchange.contactcollector.preferences.ContactCollectOnMailTransport;
import com.openexchange.context.ContextService;
import com.openexchange.database.DatabaseService;
import com.openexchange.groupware.contact.ContactInterfaceDiscoveryService;
import com.openexchange.groupware.settings.PreferencesItemService;
import com.openexchange.login.LoginHandlerService;
import com.openexchange.server.osgiservice.DeferredActivator;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.user.UserService;
import com.openexchange.userconf.UserConfigurationService;

/**
 * {@link BundleActivator Activator} for contact collector.
 * 
 * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
 */
public class Activator extends DeferredActivator {

    private List<ServiceRegistration> serviceRegistrations;

    private ContactCollectorServiceImpl collectorInstance;

    /**
     * Initializes a new {@link Activator}.
     */
    public Activator() {
        super();
    }

    @Override
    public void startBundle() throws Exception {
        /*
         * (Re-)Initialize service registry with available services
         */
        {
            final CCServiceRegistry registry = CCServiceRegistry.getInstance();
            registry.clearRegistry();
            final Class<?>[] classes = getNeededServices();
            for (int i = 0; i < classes.length; i++) {
                final Object service = getService(classes[i]);
                if (null != service) {
                    registry.addService(classes[i], service);
                }
            }
        }
        /*
         * Initialize service
         */
        collectorInstance = new ContactCollectorServiceImpl();
        collectorInstance.start();
        /*
         * Register all
         */
        serviceRegistrations = new ArrayList<ServiceRegistration>(6);
        serviceRegistrations.add(context.registerService(LoginHandlerService.class.getName(), new ContactCollectorFolderCreator(), null));
        serviceRegistrations.add(context.registerService(ContactCollectorService.class.getName(), collectorInstance, null));
        serviceRegistrations.add(context.registerService(PreferencesItemService.class.getName(), new ContactCollectFolder(), null));
        serviceRegistrations.add(context.registerService(PreferencesItemService.class.getName(), new ContactCollectEnabled(), null));
        serviceRegistrations.add(context.registerService(PreferencesItemService.class.getName(), new ContactCollectOnMailAccess(), null));
        serviceRegistrations.add(context.registerService(PreferencesItemService.class.getName(), new ContactCollectOnMailTransport(), null));
    }

    @Override
    public void stopBundle() throws Exception {
        /*
         * Unregister all
         */
        if (null != serviceRegistrations) {
            for (final ServiceRegistration serviceRegistration : serviceRegistrations) {
                serviceRegistration.unregister();
            }
            serviceRegistrations.clear();
            serviceRegistrations = null;
        }
        /*
         * Stop service
         */
        collectorInstance.stop();
        collectorInstance = null;
        /*
         * Clear service registry
         */
        CCServiceRegistry.getInstance().clearRegistry();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] {
            ContextService.class, UserService.class, UserConfigurationService.class, ContactInterfaceDiscoveryService.class,
            ThreadPoolService.class, DatabaseService.class };
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        CCServiceRegistry.getInstance().addService(clazz, getService(clazz));
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        CCServiceRegistry.getInstance().removeService(clazz);
    }

}
