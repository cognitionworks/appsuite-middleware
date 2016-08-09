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
 *     Copyright (C) 2004-2016 Open-Xchange, Inc.
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

import java.util.Map;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.FileStorageAccount;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.file.storage.dropbox.DropboxConfiguration;
import com.openexchange.file.storage.dropbox.DropboxConstants;
import com.openexchange.file.storage.dropbox.DropboxServices;
import com.openexchange.oauth.OAuthAccount;
import com.openexchange.oauth.OAuthService;
import com.openexchange.oauth.access.OAuthAccess;
import com.openexchange.oauth.access.OAuthClient;
import com.openexchange.session.Session;

/**
 * {@link DropboxOAuth2Access}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class DropboxOAuth2Access implements OAuthAccess {

    private final FileStorageAccount fsAccount;
    private final Session session;
    private OAuthClient<DbxClientV2> oauthClient;
    private volatile OAuthAccount dropboxOAuthAccount;

    /**
     * Initialises a new {@link DropboxOAuth2Access}.
     */
    public DropboxOAuth2Access(FileStorageAccount fsAccount, Session session) throws OXException {
        super();
        this.fsAccount = fsAccount;
        this.session = session;
        initialise();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.oauth.access.OAuthAccess#initialise()
     */
    @Override
    public void initialise() throws OXException {
        final OAuthService oAuthService = DropboxServices.getService(OAuthService.class);
        try {
            final OAuthAccount oauthAccount = oAuthService.getAccount(getAccountId(), session, session.getUserId(), session.getContextId());
            DbxRequestConfig config = new DbxRequestConfig(DropboxConfiguration.getInstance().getProductName());
            oauthClient = new OAuthClient<>(new DbxClientV2(config, oauthAccount.getToken()));
        } catch (RuntimeException e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.oauth.access.OAuthAccess#revoke()
     */
    @Override
    public void revoke() throws OXException {
        // TODO: revoke the token
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.oauth.access.OAuthAccess#ensureNotExpired()
     */
    @Override
    public OAuthAccess ensureNotExpired() throws OXException {
        // nothing yet
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.oauth.access.OAuthAccess#getOAuthAccount()
     */
    @Override
    public OAuthAccount getOAuthAccount() {
        return dropboxOAuthAccount;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.oauth.access.OAuthAccess#ping()
     */
    @Override
    public boolean ping() throws OXException {
        try {
            oauthClient.client.users().getCurrentAccount();
            return true;
        } catch (DbxException e) {
            //TODO: handle the exception
            e.printStackTrace();
            throw new OXException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.oauth.access.OAuthAccess#dispose()
     */
    @Override
    public void dispose() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.oauth.access.OAuthAccess#getClient()
     */
    @Override
    public OAuthClient<?> getClient() throws OXException {
        return oauthClient;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.oauth.access.OAuthAccess#getAccountId()
     */
    @Override
    public int getAccountId() throws OXException {
        /*
         * Get OAuth account identifier from messaging account's configuration
         */
        final int oauthAccountId;
        {
            final Map<String, Object> configuration = fsAccount.getConfiguration();
            if (null == configuration) {
                throw FileStorageExceptionCodes.MISSING_CONFIG.create(DropboxConstants.ID, fsAccount.getId());
            }
            final Object accountId = configuration.get("account");
            if (null == accountId) {
                throw FileStorageExceptionCodes.MISSING_CONFIG.create(DropboxConstants.ID, fsAccount.getId());
            }
            if (accountId instanceof Integer) {
                oauthAccountId = ((Integer) accountId).intValue();
            } else {
                try {
                    oauthAccountId = Integer.parseInt(accountId.toString());
                } catch (final NumberFormatException e) {
                    throw FileStorageExceptionCodes.MISSING_CONFIG.create(e, DropboxConstants.ID, fsAccount.getId());
                }
            }
        }
        return oauthAccountId;
    }
}
