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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.subscribe.xing.osgi;

import org.osgi.framework.ServiceRegistration;
import com.openexchange.context.ContextService;
import com.openexchange.database.DatabaseService;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.groupware.generic.FolderUpdaterRegistry;
import com.openexchange.groupware.update.DefaultUpdateTaskProviderService;
import com.openexchange.groupware.update.UpdateTaskProviderService;
import com.openexchange.oauth.OAuthAccountDeleteListener;
import com.openexchange.oauth.OAuthService;
import com.openexchange.oauth.OAuthServiceMetaData;
import com.openexchange.oauth.association.spi.OAuthAccountAssociationProvider;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.subscribe.SubscribeService;
import com.openexchange.subscribe.SubscriptionExecutionService;
import com.openexchange.subscribe.xing.Services;
import com.openexchange.subscribe.xing.XingSubscribeService;
import com.openexchange.subscribe.xing.groupware.XingSubscriptionsOAuthAccountDeleteListener;
import com.openexchange.subscribe.xing.oauth.XingContactsOAuthAccountAssociationProvider;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.xing.access.XingOAuthAccessProvider;

/**
 * {@link XingSubscribeActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class XingSubscribeActivator extends HousekeepingActivator {

    private ServiceRegistration<SubscribeService> serviceRegistration;
    private ServiceRegistration<OAuthAccountAssociationProvider> associationProviderRegistration;
    private ServiceRegistration<OAuthAccountDeleteListener> deleteListenerRegistration;

    /**
     * Initializes a new {@link XingSubscribeActivator}.
     */
    public XingSubscribeActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { OAuthService.class, ContextService.class, SessiondService.class, DatabaseService.class, XingOAuthAccessProvider.class, ThreadPoolService.class, FolderService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        Services.setServices(this);
        track(OAuthServiceMetaData.class, new OAuthServiceMetaDataRegisterer(context, this));
        trackService(SubscriptionExecutionService.class);
        trackService(FolderUpdaterRegistry.class);
        openTrackers();
        /*
         * Register update task
         */
        DefaultUpdateTaskProviderService providerService = new DefaultUpdateTaskProviderService(new com.openexchange.subscribe.xing.groupware.XingCrawlerSubscriptionsRemoverTask());
        registerService(UpdateTaskProviderService.class.getName(), providerService);
    }

    @Override
    protected void stopBundle() throws Exception {
        unregisterSubscribeService();
        super.stopBundle();
        Services.setServices(null);
    }

    /**
     * Registers the subscribe service.
     *
     * @throws OXException
     */
    public synchronized void registerSubscribeService() throws OXException {
        if (null == serviceRegistration) {
            XingSubscribeService xingSubscribeService = new XingSubscribeService(getService(OAuthServiceMetaData.class), this);
            serviceRegistration = context.registerService(SubscribeService.class, xingSubscribeService, null);
            org.slf4j.LoggerFactory.getLogger(XingSubscribeActivator.class).info("XingSubscribeService was started");

            if (deleteListenerRegistration == null) {
                // Register the delete listener
                ContextService contextService = Services.getService(ContextService.class);
                XingSubscriptionsOAuthAccountDeleteListener deleteListener = new XingSubscriptionsOAuthAccountDeleteListener(xingSubscribeService, contextService);
                deleteListenerRegistration = context.registerService(OAuthAccountDeleteListener.class, deleteListener, null);
            }
        }
        if (associationProviderRegistration == null) {
            associationProviderRegistration = context.registerService(OAuthAccountAssociationProvider.class, new XingContactsOAuthAccountAssociationProvider(this), null);
        }
    }

    /**
     * Un-registers the subscribe service.
     */
    public synchronized void unregisterSubscribeService() {
        ServiceRegistration<SubscribeService> serviceRegistration = this.serviceRegistration;
        if (null != serviceRegistration) {
            serviceRegistration.unregister();
            this.serviceRegistration = null;
        }
        {
            ServiceRegistration<OAuthAccountDeleteListener> deleteRegistration = this.deleteListenerRegistration;
            if (null != deleteRegistration) {
                deleteRegistration.unregister();
                this.deleteListenerRegistration = null;
            }
        }
        {
            ServiceRegistration<OAuthAccountAssociationProvider> registration = this.associationProviderRegistration;
            if (null != registration) {
                registration.unregister();
                this.associationProviderRegistration = null;
            }
        }
        org.slf4j.LoggerFactory.getLogger(XingSubscribeActivator.class).info("XingSubscribeService was stopped");
    }

    /**
     * Adds given service.
     *
     * @param authServiceMetaData The service to add
     */
    public void setOAuthServiceMetaData(final OAuthServiceMetaData oAuthServiceMetaData) {
        addService(OAuthServiceMetaData.class, oAuthServiceMetaData);
    }
}
