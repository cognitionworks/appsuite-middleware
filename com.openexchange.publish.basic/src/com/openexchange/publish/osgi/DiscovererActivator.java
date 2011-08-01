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

package com.openexchange.publish.osgi;

import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import com.openexchange.database.provider.DBProvider;
import com.openexchange.datatypes.genericonf.storage.GenericConfigurationStorageService;
import com.openexchange.groupware.delete.DeleteListener;
import com.openexchange.publish.PublicationTargetDiscoveryService;
import com.openexchange.publish.database.PublicationUserDeleteListener;
import com.openexchange.publish.helpers.AbstractPublicationService;
import com.openexchange.publish.helpers.FolderSecurityStrategy;
import com.openexchange.publish.sql.PublicationSQLStorage;
import com.openexchange.publish.tools.CompositePublicationTargetDiscoveryService;
import com.openexchange.server.osgiservice.Whiteboard;
import com.openexchange.userconf.UserConfigurationService;

/**
 * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
 */
public class DiscovererActivator implements BundleActivator {

    private ServiceRegistration discoveryRegistration;

    private OSGiPublicationTargetCollector pubServiceCollector;

    private OSGiPublicationTargetDiscovererCollector discovererCollector;

    private Whiteboard whiteboard;

    public void start(final BundleContext context) throws Exception {
        whiteboard = new Whiteboard(context);

        pubServiceCollector = new OSGiPublicationTargetCollector(context);
        discovererCollector = new OSGiPublicationTargetDiscovererCollector(context);

        final CompositePublicationTargetDiscoveryService compositeDiscovererCollector = new CompositePublicationTargetDiscoveryService();
        compositeDiscovererCollector.addDiscoveryService(pubServiceCollector);
        compositeDiscovererCollector.addDiscoveryService(discovererCollector);

        discovererCollector.ignore(compositeDiscovererCollector);

        final Hashtable<String, Object> discoveryDict = new Hashtable<String, Object>(1);
        discoveryDict.put(Constants.SERVICE_RANKING, Integer.valueOf(256));

        discoveryRegistration =
            context.registerService(PublicationTargetDiscoveryService.class.getName(), compositeDiscovererCollector, discoveryDict);

        FolderFieldActivator.setDiscoverer( compositeDiscovererCollector );

        final DBProvider provider = whiteboard.getService(DBProvider.class);
        final GenericConfigurationStorageService confStorage = whiteboard.getService(GenericConfigurationStorageService.class);

        AbstractPublicationService.setDefaultStorage( new PublicationSQLStorage(provider, confStorage, compositeDiscovererCollector) );
        AbstractPublicationService.FOLDER_ADMIN_ONLY = new FolderSecurityStrategy(whiteboard.getService(UserConfigurationService.class));

        final PublicationUserDeleteListener listener = new PublicationUserDeleteListener();
        listener.setDiscoveryService(compositeDiscovererCollector);
        listener.setGenConfStorage(confStorage);

        context.registerService(DeleteListener.class.getName(), listener, null);
    }

    public void stop(final BundleContext context) throws Exception {
        discoveryRegistration.unregister();
        discoveryRegistration = null;
        pubServiceCollector.close();
        pubServiceCollector = null;
        discovererCollector.close();
        discovererCollector = null;
        whiteboard.close();
        whiteboard = null;
    }

}
