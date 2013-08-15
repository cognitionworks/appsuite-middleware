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

package com.openexchange.oauth.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.context.ContextService;
import com.openexchange.crypto.CryptoService;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.provider.DBProvider;
import com.openexchange.html.HtmlService;
import com.openexchange.http.client.builder.HTTPResponseProcessor;
import com.openexchange.http.deferrer.CustomRedirectURLDetermination;
import com.openexchange.id.IDGeneratorService;
import com.openexchange.oauth.OAuthAccountDeleteListener;
import com.openexchange.oauth.OAuthAccountInvalidationListener;
import com.openexchange.oauth.OAuthHTTPClientFactory;
import com.openexchange.oauth.OAuthService;
import com.openexchange.oauth.OAuthServiceMetaDataRegistry;
import com.openexchange.oauth.httpclient.impl.scribe.ScribeHTTPClientFactoryImpl;
import com.openexchange.oauth.internal.CallbackRegistry;
import com.openexchange.oauth.internal.DeleteListenerRegistry;
import com.openexchange.oauth.internal.InvalidationListenerRegistry;
import com.openexchange.oauth.internal.OAuthServiceImpl;
import com.openexchange.oauth.services.Services;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.osgi.SimpleRegistryListener;
import com.openexchange.secret.SecretEncryptionFactoryService;
import com.openexchange.secret.recovery.EncryptedItemCleanUpService;
import com.openexchange.secret.recovery.EncryptedItemDetectorService;
import com.openexchange.secret.recovery.SecretMigrator;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.timer.TimerService;
import com.openexchange.tools.session.SessionHolder;

/**
 * {@link OAuthActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class OAuthActivator extends HousekeepingActivator {

    private OSGiDelegateServiceMap delegateServices;

    /**
     * Initializes a new {@link OAuthActivator}.
     */
    public OAuthActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] {
            DatabaseService.class, SessiondService.class, EventAdmin.class, SecretEncryptionFactoryService.class, SessionHolder.class,
            CryptoService.class, ConfigViewFactory.class, TimerService.class };
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        final org.apache.commons.logging.Log logger = com.openexchange.log.Log.valueOf(com.openexchange.log.LogFactory.getLog(OAuthActivator.class));
        if (logger.isInfoEnabled()) {
            logger.info("Re-available service: " + clazz.getName());
        }
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        final org.apache.commons.logging.Log logger = com.openexchange.log.Log.valueOf(com.openexchange.log.LogFactory.getLog(OAuthActivator.class));
        if (logger.isWarnEnabled()) {
            logger.warn("Absent service: " + clazz.getName());
        }
    }

    @Override
    public void startBundle() throws Exception {
        final org.apache.commons.logging.Log log = com.openexchange.log.Log.valueOf(com.openexchange.log.LogFactory.getLog(OAuthActivator.class));
        try {
            if (log.isInfoEnabled()) {
                log.info("starting bundle: com.openexchange.oauth");
            }

            Services.setServiceLookup(this);

            DeleteListenerRegistry.initInstance();
            InvalidationListenerRegistry.initInstance();
            /*
             * Collect OAuth services
             */
            OSGiMetaDataRegistry.initialize();
            final OSGiMetaDataRegistry registry = OSGiMetaDataRegistry.getInstance();
            final BundleContext context = this.context;
            registry.start(context);
            /*
             * Start other trackers
             */
            track(OAuthAccountDeleteListener.class, new DeleteListenerServiceTracker(context));
            track(OAuthAccountInvalidationListener.class, new InvalidationListenerServiceTracker(context));
            trackService(HtmlService.class);
            openTrackers();
            /*
             * Register
             */
            CallbackRegistry cbRegistry = new CallbackRegistry();
            getService(TimerService.class).scheduleAtFixedRate(cbRegistry, 600000, 600000);

            delegateServices = new OSGiDelegateServiceMap();
            delegateServices.put(DBProvider.class, new OSGiDatabaseServiceDBProvider().start(context));
            delegateServices.put(ContextService.class, new OSGiContextService().start(context));
            delegateServices.put(IDGeneratorService.class, new OSGiIDGeneratorService().start(context));
            delegateServices.startAll(context);

            final OAuthServiceImpl oauthService = new OAuthServiceImpl(
                delegateServices.get(DBProvider.class),
                delegateServices.get(IDGeneratorService.class),
                registry,
                delegateServices.get(ContextService.class),
                cbRegistry);

            registerService(CustomRedirectURLDetermination.class, cbRegistry);
            registerService(OAuthService.class, oauthService);
            registerService(OAuthServiceMetaDataRegistry.class, registry);
            registerService(EncryptedItemDetectorService.class, oauthService);
            registerService(SecretMigrator.class, oauthService);
            registerService(EncryptedItemCleanUpService.class, oauthService);

            final ScribeHTTPClientFactoryImpl oauthFactory = new ScribeHTTPClientFactoryImpl();
			registerService(OAuthHTTPClientFactory.class, oauthFactory);

			SimpleRegistryListener<HTTPResponseProcessor> listener = new SimpleRegistryListener<HTTPResponseProcessor>() {

				@Override
                public void added(ServiceReference<HTTPResponseProcessor> ref,
						HTTPResponseProcessor service) {
					oauthFactory.registerProcessor(service);
				}

				@Override
                public void removed(ServiceReference<HTTPResponseProcessor> ref,
						HTTPResponseProcessor service) {
					oauthFactory.forgetProcessor(service);
				}


			};
			track(HTTPResponseProcessor.class, listener );

        } catch (final Exception e) {
            log.error("Starting bundle \"com.openexchange.oauth\" failed.", e);
            throw e;
        }
    }

    @Override
    public void stopBundle() throws Exception {
        final org.apache.commons.logging.Log log = com.openexchange.log.Log.valueOf(com.openexchange.log.LogFactory.getLog(OAuthActivator.class));
        try {
            if (log.isInfoEnabled()) {
                log.info("stopping bundle: com.openexchange.oauth");
            }
            cleanUp();
            if (null != delegateServices) {
                delegateServices.clear();
                delegateServices = null;
            }
            DeleteListenerRegistry.releaseInstance();
            OSGiMetaDataRegistry.releaseInstance();
            Services.setServiceLookup(null);
        } catch (final Exception e) {
            log.error("Stopping bundle \"com.openexchange.oauth\" failed.", e);
            throw e;
        }
    }

}
