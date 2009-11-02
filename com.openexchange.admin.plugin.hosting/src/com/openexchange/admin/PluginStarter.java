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

package com.openexchange.admin;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import com.openexchange.admin.daemons.AdminDaemon;
import com.openexchange.admin.daemons.ClientAdminThreadExtended;
import com.openexchange.admin.exceptions.OXGenericException;
import com.openexchange.admin.rmi.OXContextInterface;
import com.openexchange.admin.rmi.OXUtilInterface;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.tools.AdminCacheExtended;
import com.openexchange.admin.tools.PropertyHandlerExtended;

public class PluginStarter {

    private static Registry registry = null;
    private static Log LOG = LogFactory.getLog(PluginStarter.class);

    private static com.openexchange.admin.rmi.impl.OXContext oxctx_v2 = null;
    private static com.openexchange.admin.rmi.impl.OXUtil oxutil_v2 = null;

    private static PropertyHandlerExtended prop = null;

    public PluginStarter() {
        super();
    }

    public void start(final BundleContext context) throws RemoteException, AlreadyBoundException, StorageException, OXGenericException {
        try {
            initCache();
            registry = AdminDaemon.getRegistry();

            // Create all OLD Objects and bind export them
            oxctx_v2 = new com.openexchange.admin.rmi.impl.OXContext(context);
            final OXContextInterface oxctx_stub_v2 = (OXContextInterface) UnicastRemoteObject.exportObject(oxctx_v2, 0);

            oxutil_v2 = new com.openexchange.admin.rmi.impl.OXUtil();
            final OXUtilInterface oxutil_stub_v2 = (OXUtilInterface) UnicastRemoteObject.exportObject(oxutil_v2, 0);

            // bind all NEW Objects to registry
            registry.bind(OXContextInterface.RMI_NAME, oxctx_stub_v2);
            registry.bind(OXUtilInterface.RMI_NAME, oxutil_stub_v2);

//            startJMX();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Loading context implementation: " + prop.getProp(PropertyHandlerExtended.CONTEXT_STORAGE, null));
                LOG.debug("Loading util implementation: " + prop.getProp(PropertyHandlerExtended.UTIL_STORAGE, null));
            }
        } catch (final RemoteException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        } catch (final AlreadyBoundException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        } catch (final StorageException e) {
            LOG.fatal("Error while creating one instance for RMI interface", e);
            throw e;
        }
    }

    public void stop() throws AccessException, RemoteException, NotBoundException {
        try {
            if (null != registry) {
                registry.unbind(OXContextInterface.RMI_NAME);
                registry.unbind(OXUtilInterface.RMI_NAME);
            }
        } catch (final AccessException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        } catch (final RemoteException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        } catch (final NotBoundException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }

    }

    private void initCache() throws OXGenericException {
        final AdminCacheExtended cache = new AdminCacheExtended();
        cache.initCache();
        ClientAdminThreadExtended.cache = cache;
        prop = cache.getProperties();
        LOG.info("Cache and Pools initialized!");
    }
}
