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

package com.openexchange.oauth.yahoo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openexchange.exception.OXException;
import com.openexchange.oauth.API;
import com.openexchange.oauth.HostInfo;
import com.openexchange.oauth.OAuthAccount;
import com.openexchange.oauth.OAuthInteraction;
import com.openexchange.oauth.OAuthInteractionType;
import com.openexchange.oauth.OAuthService;
import com.openexchange.oauth.OAuthServiceMetaData;
import com.openexchange.oauth.OAuthServiceMetaDataRegistry;
import com.openexchange.oauth.scope.OAuthScope;
import com.openexchange.session.Session;

/**
 * {@link MockOAuthService}
 *
 * @author <a href="mailto:karsten.will@open-xchange.com">Karsten Will</a>
 */
public class MockOAuthService implements OAuthService {

    private String token, tokenSecret;

    @Override
    public OAuthAccount createAccount(Session session, final String serviceMetaData, Set<OAuthScope> scopes, final Map<String, Object> arguments) throws OXException {
        // Nothing to do
        return null;
    }

    @Override
    public OAuthAccount createAccount(Session session, final String serviceMetaData, Set<OAuthScope> scopes, final OAuthInteractionType type, final Map<String, Object> arguments) throws OXException {
        // Nothing to do
        return null;
    }

    @Override
    public void deleteAccount(Session session, final int accountId) throws OXException {
        // Nothing to do

    }

    @Override
    public OAuthAccount getAccount(final Session session, final int accountId) throws OXException {
        return new OAuthAccount() {

            @Override
            public String getDisplayName() {
                // Nothing to do
                return null;
            }

            @Override
            public int getId() {
                // Nothing to do
                return 0;
            }

            @Override
            public OAuthServiceMetaData getMetaData() {
                // Nothing to do
                return null;
            }

            @SuppressWarnings("synthetic-access")
            @Override
            public String getSecret() {
                return tokenSecret;
            }

            @SuppressWarnings("synthetic-access")
            @Override
            public String getToken() {
                return token;
            }

            @Override
            public API getAPI() {
                // Nothing to do
                return null;
            }

            @Override
            public Set<OAuthScope> getEnabledScopes() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getUserIdentity() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public long getExpiration() {
                return Long.MAX_VALUE;
            }

        };
    }

    @Override
    public OAuthServiceMetaDataRegistry getMetaDataRegistry() {
        // Nothing to do
        return null;
    }

    @Override
    public OAuthInteraction initOAuth(Session session, final String serviceMetaData, final String callbackUrl, HostInfo currentHost, Set<OAuthScope> scopes) throws OXException {
        // Nothing to do
        return null;
    }

    @Override
    public void updateAccount(Session session, final int accountId, final Map<String, Object> arguments) throws OXException {
        // Nothing to do

    }

    @Override
    public OAuthAccount updateAccount(Session session, final int accountId, final String serviceMetaData, final OAuthInteractionType type, final Map<String, Object> arguments, Set<OAuthScope> scopes) throws OXException {
        // Nothing to do
        return null;
    }

    public void setToken(final String string) {
        this.token = string;
    }

    public void setSecret(final String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    @Override
    public OAuthAccount getDefaultAccount(API api, Session session) {
        // Nothing to do
        return null;
    }

    @Override
    public OAuthAccount upsertAccount(Session session, String serviceMetaData, int accountId, OAuthInteractionType type, Map<String, Object> arguments, Set<OAuthScope> scopes) throws OXException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<OAuthAccount> getAccounts(Session session) throws OXException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<OAuthAccount> getAccounts(Session session, String serviceMetaData) throws OXException {
        // TODO Auto-generated method stub
        return null;
    }

}
