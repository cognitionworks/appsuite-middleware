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

package com.openexchange.oauth.osgi;

import java.util.ArrayList;
import java.util.List;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.context.ContextService;
import com.openexchange.crypto.CryptoService;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.provider.DBProvider;
import com.openexchange.id.IDGeneratorService;
import com.openexchange.oauth.OAuthAccountDeleteListener;
import com.openexchange.oauth.OAuthService;
import com.openexchange.oauth.OAuthServiceMetaDataRegistry;
import com.openexchange.oauth.internal.DeleteListenerRegistry;
import com.openexchange.oauth.internal.OAuthServiceImpl;
import com.openexchange.oauth.services.ServiceRegistry;
import com.openexchange.secret.recovery.SecretConsistencyCheck;
import com.openexchange.secret.recovery.SecretMigrator;
import com.openexchange.server.osgiservice.DeferredActivator;
import com.openexchange.sessiond.SessiondService;

/**
 * {@link OAuthActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class OAuthActivator extends DeferredActivator {

    private List<ServiceRegistration<?>> registrations;

    private List<ServiceTracker<?,?>> trackers;

    private OSGiDelegateServiceMap delegateServices;

    /**
     * Initializes a new {@link OAuthActivator}.
     */
    public OAuthActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { DatabaseService.class, SessiondService.class, EventAdmin.class, CryptoService.class };
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        final org.apache.commons.logging.Log logger = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(OAuthActivator.class));
        if (logger.isInfoEnabled()) {
            logger.info("Re-available service: " + clazz.getName());
        }
        ServiceRegistry.getInstance().addService(clazz, getService(clazz));
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        final org.apache.commons.logging.Log logger = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(OAuthActivator.class));
        if (logger.isWarnEnabled()) {
            logger.warn("Absent service: " + clazz.getName());
        }
        ServiceRegistry.getInstance().removeService(clazz);
    }

    @Override
    public void startBundle() throws Exception {
        final org.apache.commons.logging.Log log = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(OAuthActivator.class));
        try {
            if (log.isInfoEnabled()) {
                log.info("starting bundle: com.openexchange.oauth");
            }
            /*
             * (Re-)Initialize service registry with available services
             */
            {
                final ServiceRegistry registry = ServiceRegistry.getInstance();
                registry.clearRegistry();
                final Class<?>[] classes = getNeededServices();
                for (final Class<?> classe : classes) {
                    final Object service = getService(classe);
                    if (null != service) {
                        registry.addService(classe, service);
                    }
                }
            }
            DeleteListenerRegistry.initInstance();
            /*
             * Collect OAuth services
             */
            final OSGiMetaDataRegistry registry = OSGiMetaDataRegistry.getInstance();
            registry.start(context);
            /*
             * Start other trackers
             */
            trackers = new ArrayList<ServiceTracker<?,?>>(4);
            trackers.add(new ServiceTracker<OAuthAccountDeleteListener,OAuthAccountDeleteListener>(context, OAuthAccountDeleteListener.class, new DeleteListenerServiceTracker(context)));
            for (final ServiceTracker<?,?> tracker : trackers) {
                tracker.open();
            }
            /*
             * Register
             */
            registrations = new ArrayList<ServiceRegistration<?>>(2);

            delegateServices = new OSGiDelegateServiceMap();
            delegateServices.put(DBProvider.class, new OSGiDatabaseServiceDBProvider().start(context));
            delegateServices.put(ContextService.class, new OSGiContextService().start(context));
            delegateServices.put(IDGeneratorService.class, new OSGiIDGeneratorService().start(context));
            delegateServices.startAll(context);

            final OAuthServiceImpl oauthService = new OAuthServiceImpl(
                delegateServices.get(DBProvider.class),
                delegateServices.get(IDGeneratorService.class),
                registry,
                delegateServices.get(ContextService.class));
            registrations.add(context.registerService(OAuthService.class.getName(), oauthService, null));
            registrations.add(context.registerService(OAuthServiceMetaDataRegistry.class.getName(), registry, null));
            registrations.add(context.registerService(SecretConsistencyCheck.class.getName(), oauthService, null));
            registrations.add(context.registerService(SecretMigrator.class.getName(), oauthService, null));

        } catch (final Exception e) {
            log.error("Starting bundle \"com.openexchange.oauth\" failed.", e);
            throw e;
        }
    }

    @Override
    public void stopBundle() throws Exception {
        final org.apache.commons.logging.Log log = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(OAuthActivator.class));
        try {
            if (log.isInfoEnabled()) {
                log.info("stopping bundle: com.openexchange.oauth");
            }
            if (null != trackers) {
                while (!trackers.isEmpty()) {
                    trackers.remove(0).close();
                }
                trackers = null;
            }
            if (null != registrations) {
                while (!registrations.isEmpty()) {
                    registrations.remove(0).unregister();
                }
                registrations = null;
            }
            if (null != delegateServices) {
                delegateServices.clear();
                delegateServices = null;
            }
            DeleteListenerRegistry.releaseInstance();
            OSGiMetaDataRegistry.releaseInstance();
        } catch (final Exception e) {
            log.error("Stopping bundle \"com.openexchange.oauth\" failed.", e);
            throw e;
        }
    }

}
