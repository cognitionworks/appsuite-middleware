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

package com.openexchange.file.storage.dropbox.access;

import org.scribe.builder.ServiceBuilder;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.Token;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.HttpRequestor;
import com.dropbox.core.v2.DbxClientV2;
import com.openexchange.cluster.lock.ClusterLockService;
import com.openexchange.cluster.lock.ClusterTask;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.FileStorageAccount;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.file.storage.dropbox.DropboxConfiguration;
import com.openexchange.file.storage.dropbox.DropboxConstants;
import com.openexchange.file.storage.dropbox.DropboxServices;
import com.openexchange.oauth.AbstractReauthorizeClusterTask;
import com.openexchange.oauth.OAuthAccount;
import com.openexchange.oauth.OAuthService;
import com.openexchange.oauth.OAuthUtil;
import com.openexchange.oauth.access.AbstractOAuthAccess;
import com.openexchange.oauth.access.OAuthAccess;
import com.openexchange.oauth.access.OAuthClient;
import com.openexchange.oauth.api.DropboxApi2;
import com.openexchange.oauth.scope.OXScope;
import com.openexchange.policy.retry.ExponentialBackOffRetryPolicy;
import com.openexchange.session.Session;

/**
 * {@link DropboxOAuth2Access}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class DropboxOAuth2Access extends AbstractOAuthAccess {

    private final FileStorageAccount fsAccount;

    /**
     * Initializes a new {@link DropboxOAuth2Access}.
     */
    public DropboxOAuth2Access(FileStorageAccount fsAccount, Session session) {
        super(session);
        this.fsAccount = fsAccount;
    }

    @Override
    public void initialize() throws OXException {
        OAuthService oAuthService = DropboxServices.getService(OAuthService.class);
        try {
            OAuthAccount oauthAccount = oAuthService.getAccount(getSession(), getAccountId());
            verifyAccount(oauthAccount, oAuthService, OXScope.drive);
            HttpRequestor httpRequestor = new ApacheHttpClientHttpRequestor();
            DbxRequestConfig config = DbxRequestConfig.newBuilder(DropboxConfiguration.getInstance().getProductName()).withHttpRequestor(httpRequestor).build();
            String accessToken = oauthAccount.getToken();
            DbxClientV2 dbxClient = new DbxClientV2(config, accessToken);
            OAuthClient<DropboxClient> oAuthClient = new OAuthClient<DropboxClient>(new DropboxClient(dbxClient, httpRequestor), accessToken);
            setOAuthClient(oAuthClient);
            setOAuthAccount(oauthAccount);
        } catch (RuntimeException e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public OAuthAccess ensureNotExpired() throws OXException {
        if (isExpired()) {
            synchronized (this) {
                if (isExpired()) {
                    if (getOAuthAccount() == null) {
                        initialize();
                    }
                    ClusterLockService clusterLockService = DropboxServices.getService(ClusterLockService.class);
                    clusterLockService.runClusterTask(new DropboxReauthorizeClusterTask(getSession(), getOAuthAccount()), new ExponentialBackOffRetryPolicy());
                    // Re-set account and client and make all proper connections
                    initialize();
                }
            }
        }
        return this;
    }

    @Override
    public boolean ping() throws OXException {
        try {
            DropboxClient client = (DropboxClient) getClient().client;
            client.dbxClient.users().getCurrentAccount();
            return true;
        } catch (DbxException e) {
            throw DropboxExceptionHandler.handle(e, getSession(), getOAuthAccount());
        }
    }

    @Override
    public int getAccountId() throws OXException {
        try {
            return getAccountId(fsAccount.getConfiguration());
        } catch (IllegalArgumentException e) {
            throw FileStorageExceptionCodes.MISSING_CONFIG.create(DropboxConstants.ID, fsAccount.getId());
        }
    }

    private class DropboxReauthorizeClusterTask extends AbstractReauthorizeClusterTask implements ClusterTask<OAuthAccount> {

        /**
         * Initialises a new {@link DropboxReauthorizeClusterTask}.
         */
        public DropboxReauthorizeClusterTask(Session session, OAuthAccount cachedAccount) {
            super(DropboxServices.getServices(), session, cachedAccount);
        }

        @Override
        public Token reauthorize() throws OXException {
            ServiceBuilder serviceBuilder = new ServiceBuilder().provider(DropboxApi2.class);
            serviceBuilder.apiKey(getCachedAccount().getMetaData().getAPIKey(getSession())).apiSecret(getCachedAccount().getMetaData().getAPISecret(getSession()));
            DropboxApi2.DropboxOAuth2Service scribeOAuthService = DropboxApi2.DropboxOAuth2Service.class.cast(serviceBuilder.build());

            // Refresh the token
            try {
                return scribeOAuthService.getAccessToken(new Token(getCachedAccount().getToken(), getCachedAccount().getSecret()), null);
            } catch (OAuthException e) {
                OAuthAccount dbAccount = getDBAccount();
                throw OAuthUtil.handleScribeOAuthException(e, dbAccount, getSession());
            }
        }
    }
}
