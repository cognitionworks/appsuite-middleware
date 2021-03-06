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

package com.openexchange.oauth.provider.json.osgi;

import org.osgi.framework.ServiceReference;
import com.openexchange.ajax.requesthandler.osgiservice.AJAXModuleActivator;
import com.openexchange.capabilities.CapabilityChecker;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.exception.OXException;
import com.openexchange.filemanagement.ManagedFileManagement;
import com.openexchange.i18n.TranslatorFactory;
import com.openexchange.oauth.provider.authorizationserver.grant.GrantManagement;
import com.openexchange.oauth.provider.json.OAuthProviderActionFactory;
import com.openexchange.oauth.provider.resourceserver.OAuthResourceService;
import com.openexchange.osgi.SimpleRegistryListener;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;


/**
 * {@link OAuthProviderJSONActivator}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public class OAuthProviderJSONActivator extends AJAXModuleActivator {

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { TranslatorFactory.class, ManagedFileManagement.class, CapabilityService.class };
    }

    @Override
    protected Class<?>[] getOptionalServices() {
        return new Class<?>[] { OAuthResourceService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        final ServiceLookup services = this;
        track(GrantManagement.class, new SimpleRegistryListener<GrantManagement>() {
            @Override
            public void added(ServiceReference<GrantManagement> ref, GrantManagement service) {
                addService(GrantManagement.class, service);
                getService(CapabilityService.class).declareCapability("oauth-grants");
                registerService(CapabilityChecker.class, new CapabilityChecker() {

                    @Override
                    public boolean isEnabled(String capability, Session session) throws OXException {
                        if ("oauth-grants".equals(capability)) {
                            ServerSession serverSession = ServerSessionAdapter.valueOf(session);
                            if (serverSession.isAnonymous() || serverSession.getUser().isGuest()) {
                                return false;
                            }
                            OAuthResourceService oAuthResourceService = getService(OAuthResourceService.class);
                            if (oAuthResourceService == null) {
                                return false;
                            }
                            return oAuthResourceService.isProviderEnabled(session.getContextId(), session.getUserId());
                        }
                        return true;
                    }
                });
                registerModule(new OAuthProviderActionFactory(services), "oauth/grants");
            }

            @Override
            public void removed(ServiceReference<GrantManagement> ref, GrantManagement service) {
                getService(CapabilityService.class).undeclareCapability("oauth-grants");
            }
        });
        openTrackers();
    }

    @Override
    public <S> boolean addService(Class<S> clazz, S service) {
        return super.addService(clazz, service);
    }

    @Override
    public <S> void registerService(Class<S> clazz, S service) {
        super.registerService(clazz, service);
    }

}
