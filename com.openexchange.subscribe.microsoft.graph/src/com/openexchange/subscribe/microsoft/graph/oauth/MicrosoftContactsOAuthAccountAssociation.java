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
 *     Copyright (C) 2018-2020 OX Software GmbH
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

package com.openexchange.subscribe.microsoft.graph.oauth;

import java.util.Collections;
import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.oauth.KnownApi;
import com.openexchange.oauth.OAuthAccount;
import com.openexchange.oauth.OAuthService;
import com.openexchange.oauth.OAuthServiceMetaData;
import com.openexchange.oauth.OAuthServiceMetaDataRegistry;
import com.openexchange.oauth.access.AbstractOAuthAccess;
import com.openexchange.oauth.association.Module;
import com.openexchange.oauth.association.Status;
import com.openexchange.oauth.microsoft.graph.MicrosoftGraphOAuthScope;
import com.openexchange.oauth.scope.OAuthScope;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.subscribe.Subscription;
import com.openexchange.subscribe.oauth.AbstractSubscribeOAuthAccountAssociation;

/**
 * {@link MicrosoftContactsOAuthAccountAssociation}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.1
 */
public class MicrosoftContactsOAuthAccountAssociation extends AbstractSubscribeOAuthAccountAssociation {

    private final ServiceLookup services;

    /**
     * Initialises a new {@link MicrosoftContactsOAuthAccountAssociation}.
     * 
     * @param services The {@link ServiceLookup} instance
     */
    public MicrosoftContactsOAuthAccountAssociation(int accountId, int userId, int contextId, String displayName, Subscription subscription, ServiceLookup services) {
        super(accountId, userId, contextId, displayName, subscription);
        this.services = services;
    }

    @Override
    public String getModule() {
        return Module.CONTACTS.getModuleName();
    }

    @Override
    public List<OAuthScope> getScopes() {
        return Collections.singletonList(MicrosoftGraphOAuthScope.contacts_ro);
    }

    @SuppressWarnings("unused")
    @Override
    public Status getStatus(Session session) throws OXException {
        String accessToken = null;
        String accessSecret = null;
        try {
            OAuthService service = services.getService(OAuthService.class);
            OAuthAccount account = service.getAccount(session, getOAuthAccountId());
            accessToken = account.getToken();
            accessSecret = account.getSecret();
        } catch (OXException e) {
            return Status.RECREATION_NEEDED;
        }
        if (Strings.isEmpty(accessToken) && Strings.isEmpty(accessSecret)) {
            return Status.INVALID_GRANT;
        }
        OAuthServiceMetaDataRegistry registry = services.getService(OAuthServiceMetaDataRegistry.class);
        OAuthServiceMetaData metadata = registry.getService(KnownApi.MICROSOFT_GRAPH.getServiceId(), session.getUserId(), session.getContextId());
        String userIdentity = metadata.getUserIdentity(session, getOAuthAccountId(), accessToken, accessSecret);
        if (Strings.isEmpty(userIdentity)) {
            return Status.INVALID_GRANT;
        }
        return Status.OK;
    }

    @Override
    protected AbstractOAuthAccess newAccess(Session session) throws OXException {
        // nope
        throw new UnsupportedOperationException("There is no OAuthAccess for Microsoft Graph Contacts.");
    }
}
