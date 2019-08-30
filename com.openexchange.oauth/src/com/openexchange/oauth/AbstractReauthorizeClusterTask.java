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

package com.openexchange.oauth;

import static com.openexchange.java.Autoboxing.I;
import java.util.HashMap;
import java.util.Map;
import org.scribe.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link AbstractReauthorizeClusterTask}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public abstract class AbstractReauthorizeClusterTask {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractReauthorizeClusterTask.class);

    private final String taskName;
    private final Session session;
    private final OAuthAccount cachedAccount;
    private final ServiceLookup services;
    private OAuthAccount dbAccount;
    private OAuthService oauthService;

    /**
     * Initialises a new {@link AbstractReauthorizeClusterTask} and generates the name of
     * the task.
     *
     * <p>The task name consists out of the following parts:</p>
     *
     * <pre>[USER_ID]@[CONTEXT_ID]:[ACCOUNT_ID]:[SERVICE_ID]</pre>
     * e.g.
     * <pre>SomeProviderReauthorizeClusterTask:1138@31145:34:tld.domain.provider.oauth</pre>
     *
     * @param services The {@link ServiceLookup} instance
     * @param session The groupware {@link Session}
     * @param cachedAccount The cached {@link OAuthAccount}
     */
    public AbstractReauthorizeClusterTask(ServiceLookup services, Session session, OAuthAccount cachedAccount) {
        super();
        this.services = services;
        this.session = session;
        this.cachedAccount = cachedAccount;

        StringBuilder builder = new StringBuilder();
        builder.append(session.getUserId()).append("@");
        builder.append(session.getContextId());
        builder.append(":").append(cachedAccount.getId());
        builder.append(":").append(cachedAccount.getAPI().getDisplayName());

        taskName = builder.toString();
    }

    /**
     * Returns the context identifier
     *
     * @return the context identifier
     */
    public int getContextId() {
        return session.getContextId();
    }

    /**
     * Returns the user identifier
     *
     * @return the user identifier
     */
    public int getUserId() {
        return session.getUserId();
    }

    /**
     * Gets the task name.
     *
     * @return The task name
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * Gets the session
     *
     * @return The session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Gets the cachedAccount
     *
     * @return The cachedAccount
     */
    public OAuthAccount getCachedAccount() {
        return cachedAccount;
    }

    /**
     * Returns the database {@link OAuthAccount}
     *
     * @return The database {@link OAuthAccount}
     * @throws OXException if the {@link OAuthAccount} cannot be retrieved
     */
    public OAuthAccount getDBAccount() throws OXException {
        if (dbAccount == null) {
            dbAccount = getOAuthService().getAccount(session, cachedAccount.getId());
        }
        return dbAccount;
    }

    /**
     * Returns the {@link OAuthService}
     *
     * @return the {@link OAuthService}
     */
    public OAuthService getOAuthService() {
        if (oauthService == null) {
            oauthService = services.getService(OAuthService.class);
        }
        return oauthService;
    }

    /**
     * Common logic for the perform
     *
     * @return The re-authorised OAuthAccount
     * @throws OXException if an error is occurred
     */
    public OAuthAccount perform() throws OXException {
        dbAccount = getDBAccount();

        // Cached account does not match the database account. DB account is always considered to be up-to-date, thus return it
        if (false == dbAccount.getToken().equals(cachedAccount.getToken()) || false == dbAccount.getSecret().equals(cachedAccount.getSecret())) {
            return dbAccount;
        }

        /// TEMP ///
        checkForEmptySecrets();

        // Perform the actual re-authorise
        Token token = reauthorize();

        // Did the OAuth provider returned a new refresh token?
        String refreshToken = (Strings.isEmpty(token.getSecret())) ? dbAccount.getSecret() : token.getSecret();

        // Set the arguments for the update
        int accountId = dbAccount.getId();
        Map<String, Object> arguments = new HashMap<>(2);
        arguments.put(OAuthConstants.ARGUMENT_REQUEST_TOKEN, new DefaultOAuthToken(token.getToken(), refreshToken));
        arguments.put(OAuthConstants.ARGUMENT_SESSION, session);

        // Update the account
        OAuthService oAuthService = getOAuthService();
        oAuthService.updateAccount(session, accountId, arguments);

        // Reload
        return oAuthService.getAccount(session, accountId);
    }

    /// TEMP ///
    private void checkForEmptySecrets() {
        if (Strings.isEmpty(cachedAccount.getSecret())) {
            LOG.debug("The cached account {} of user {} in context {} has an empty secret", I(cachedAccount.getId()), I(session.getUserId()), I(session.getContextId()));
        }
        if (Strings.isEmpty(dbAccount.getSecret())) {
            LOG.debug("The DB account {} of user {} in context {} has an empty secret", I(dbAccount.getId()), I(session.getUserId()), I(session.getContextId()));
        }
    }

    /**
     * Performs the actual re-authorise task
     *
     * @return The re-authorised OAuthAccount
     * @throws OXException if an error is occurred
     */
    protected abstract Token reauthorize() throws OXException;
}
