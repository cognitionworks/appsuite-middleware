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

package com.openexchange.oauth.impl.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLHandshakeException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import com.openexchange.exception.ExceptionUtils;
import com.openexchange.exception.OXException;
import com.openexchange.html.HtmlService;
import com.openexchange.http.deferrer.DeferringURLService;
import com.openexchange.java.Charsets;
import com.openexchange.java.Strings;
import com.openexchange.net.ssl.exception.SSLExceptionCode;
import com.openexchange.oauth.API;
import com.openexchange.oauth.DefaultOAuthAccount;
import com.openexchange.oauth.HostInfo;
import com.openexchange.oauth.KnownApi;
import com.openexchange.oauth.OAuthAccount;
import com.openexchange.oauth.OAuthAccountStorage;
import com.openexchange.oauth.OAuthConstants;
import com.openexchange.oauth.OAuthEventConstants;
import com.openexchange.oauth.OAuthExceptionCodes;
import com.openexchange.oauth.OAuthInteraction;
import com.openexchange.oauth.OAuthInteractionType;
import com.openexchange.oauth.OAuthService;
import com.openexchange.oauth.OAuthServiceMetaData;
import com.openexchange.oauth.OAuthServiceMetaDataRegistry;
import com.openexchange.oauth.OAuthToken;
import com.openexchange.oauth.OAuthUtil;
import com.openexchange.oauth.impl.services.Services;
import com.openexchange.oauth.scope.OAuthScope;
import com.openexchange.session.Session;

/**
 * An {@link OAuthService} Implementation using the RDB for storage and Scribe OAuth library for the OAuth interaction.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class OAuthServiceImpl implements OAuthService {

    private static final String REAUTHORIZE_ACTION_HINT = "reauthorize";

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(OAuthServiceImpl.class);

    private final OAuthServiceMetaDataRegistry registry;
    private final OAuthAccountStorage oauthAccountStorage;

    private final CallbackRegistryImpl callbackRegistry;

    /**
     * Initialises a new {@link OAuthServiceImpl}.
     *
     * @param registry the {@link OAuthServiceMetaDataRegistry}
     * @param oauthAccountStorage The {@link OAuthAccountStorage}
     * @param cbRegistry The {@link CallbackRegistryImpl}
     */
    public OAuthServiceImpl(OAuthServiceMetaDataRegistry registry, OAuthAccountStorage oauthAccountStorage, CallbackRegistryImpl cbRegistry) {
        super();
        this.registry = registry;
        this.oauthAccountStorage = oauthAccountStorage;
        this.callbackRegistry = cbRegistry;
    }

    @Override
    public OAuthServiceMetaDataRegistry getMetaDataRegistry() {
        return registry;
    }

    @Override
    public List<OAuthAccount> getAccounts(final Session session) throws OXException {
        return oauthAccountStorage.getAccounts(session);
    }

    @Override
    public List<OAuthAccount> getAccounts(Session session, String serviceMetaData) throws OXException {
        return oauthAccountStorage.getAccounts(session, serviceMetaData);
    }

    @Override
    public OAuthInteraction initOAuth(final Session session, final String serviceMetaData, final String callbackUrl, final HostInfo currentHost, Set<OAuthScope> scopes) throws OXException {
        try {
            final int contextId = session.getContextId();
            final int userId = session.getUserId();
            final OAuthServiceMetaData metaData = registry.getService(serviceMetaData, userId, contextId);

            // Check for individual OAuthInteraction
            final OAuthInteraction interaction = metaData.initOAuth(callbackUrl, session);
            if (interaction != null) {
                return interaction;
            }
            String cbUrl = callbackUrl;

            // Apply possible modifications to call-back URL
            {
                final String modifiedUrl = metaData.modifyCallbackURL(cbUrl, currentHost, session);
                if (modifiedUrl != null) {
                    cbUrl = modifiedUrl;
                }
            }
            // Check for available deferrer service
            DeferringURLService ds = Services.getService(DeferringURLService.class);
            {
                boolean deferred = false;
                if (isDeferrerAvailable(ds, userId, contextId)) {
                    String deferredURL = ds.getDeferredURL(cbUrl, userId, contextId);
                    if (deferredURL != null) {
                        cbUrl = deferredURL;
                        deferred = true;
                    }
                }
                if (false == deferred && metaData.registerTokenBasedDeferrer()) {
                    // Not yet deferred, but wants to
                }
            }

            // Get token & authorization URL
            Token scribeToken;
            StringBuilder authorizationURL;
            {
                org.scribe.oauth.OAuthService service = getScribeService(metaData, cbUrl, session, scopes);
                scribeToken = metaData.needsRequestToken() ? service.getRequestToken() : null;
                authorizationURL = new StringBuilder(service.getAuthorizationUrl(scribeToken));
            }

            // Process authorization URL
            final String authURL = metaData.processAuthorizationURLCallbackAware(metaData.processAuthorizationURL(authorizationURL.toString(), session), cbUrl);
            // Register deferrer
            if (metaData.registerTokenBasedDeferrer()) {
                // Register by token
                if (null != scribeToken) {
                    registerTokenForDeferredAccess(scribeToken.getToken(), cbUrl, ds, userId, contextId);
                } else {
                    String registerToken = metaData.getRegisterToken(authURL);
                    if (null != registerToken) {
                        registerTokenForDeferredAccess(registerToken, cbUrl, ds, userId, contextId);
                    }
                }
            }

            if (LOG.isDebugEnabled()) {
                String message = scribeToken != null ? "Acquired a request token for '{}'" : "Did not acquire a request token for '{}' (not required)";
                LOG.debug(message, serviceMetaData);
            }
            // Return interaction
            OAuthToken requestToken = scribeToken == null ? OAuthToken.EMPTY_TOKEN : new ScribeOAuthToken(scribeToken);
            OAuthInteractionType interactionType = cbUrl == null ? OAuthInteractionType.OUT_OF_BAND : OAuthInteractionType.CALLBACK;
            return new OAuthInteractionImpl(requestToken, authURL, interactionType);
        } catch (org.scribe.exceptions.OAuthException e) {
            throw handleScribeOAuthException(e);
        } catch (Exception e) {
            throw OAuthExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public OAuthAccount createAccount(Session session, final String serviceMetaData, Set<OAuthScope> scopes, final Map<String, Object> arguments) throws OXException {
        isNull(arguments, OAuthConstants.ARGUMENT_DISPLAY_NAME, OAuthConstants.ARGUMENT_SESSION, OAuthConstants.ARGUMENT_TOKEN, OAuthConstants.ARGUMENT_SECRET);
        // Create appropriate OAuth account instance
        DefaultOAuthAccount account = new DefaultOAuthAccount();
        // Determine associated service's meta data
        OAuthServiceMetaData service = registry.getService(serviceMetaData, session.getUserId(), session.getContextId());
        account.setMetaData(service);
        // Set display name & identifier
        String displayName = (String) arguments.get(OAuthConstants.ARGUMENT_DISPLAY_NAME);
        account.setDisplayName(displayName);
        // Token & secret
        account.setToken((String) arguments.get(OAuthConstants.ARGUMENT_TOKEN));
        account.setSecret((String) arguments.get(OAuthConstants.ARGUMENT_SECRET));
        account.setEnabledScopes(scopes);
        // Store the account
        //Session session = (Session) arguments.get(OAuthConstants.ARGUMENT_SESSION);
        oauthAccountStorage.storeAccount(session, account);
        return account;
    }

    @Override
    public OAuthAccount upsertAccount(Session session, String serviceMetaData, int accountId, OAuthInteractionType type, Map<String, Object> arguments, Set<OAuthScope> scopes) throws OXException {
        DefaultOAuthAccount account = new DefaultOAuthAccount();
        OAuthServiceMetaData service = registry.getService(serviceMetaData, session.getUserId(), session.getContextId());
        account.setMetaData(service);

        obtainToken(type, arguments, account, scopes);

        isNull(arguments, OAuthConstants.ARGUMENT_SESSION);

        String displayName = (String) arguments.get(OAuthConstants.ARGUMENT_DISPLAY_NAME);
        account.setDisplayName(displayName);
        account.setEnabledScopes(scopes);

        //Session session = (Session) arguments.get(OAuthConstants.ARGUMENT_SESSION);
        String userIdentity = service.getUserIdentity(session, accountId, account.getToken(), account.getSecret());
        account.setUserIdentity(userIdentity);

        String actionHint = (String) arguments.get(OAuthConstants.ARGUMENT_ACTION_HINT);
        DefaultOAuthAccount existingAccount = getExistingAccount(session, userIdentity, serviceMetaData, accountId);

        if (existingAccount == null) {
            /*
             * No account found but 'reauthorize' was requested.
             * Background information: When initialising an account's oauth access, the underlying logic
             * checks for the user identity and if it's missing it will be fetched from the respective
             * OAuth provider and the account will be updated accordingly
             *
             * Therefore this edge case can only happen after an upgrade and only if the user has explicitly revoked
             * the access from the third party OAuth provider.In that case there is nothing that can be
             * done from the middleware's point of view, since a hint is required to somehow identify the
             * user's account (no accountId is provided, and no identity exists in the database from where
             * a match can be found).
             */
            if (Strings.isNotEmpty(actionHint) && REAUTHORIZE_ACTION_HINT.equals(actionHint)) {
                throw OAuthExceptionCodes.INVALID_ACCOUNT.create();
            }
            isNull(arguments, OAuthConstants.ARGUMENT_DISPLAY_NAME);
            oauthAccountStorage.storeAccount(session, account);
            return account;
        }

        // if found then update that account
        existingAccount.setToken(account.getToken());
        existingAccount.setSecret(account.getSecret());
        for (OAuthScope scope : scopes) {
            existingAccount.addEnabledScope(scope);
        }
        existingAccount.setUserIdentity(userIdentity);
        oauthAccountStorage.updateAccount(session, existingAccount);
        return existingAccount;
    }

    @Override
    public OAuthAccount createAccount(Session session, final String serviceMetaData, Set<OAuthScope> scopes, final OAuthInteractionType type, final Map<String, Object> arguments) throws OXException {
        isNull(arguments, OAuthConstants.ARGUMENT_DISPLAY_NAME, OAuthConstants.ARGUMENT_SESSION);
        try {
            DefaultOAuthAccount account = new DefaultOAuthAccount();

            OAuthServiceMetaData service = registry.getService(serviceMetaData, session.getUserId(), session.getContextId());
            account.setMetaData(service);

            String displayName = (String) arguments.get(OAuthConstants.ARGUMENT_DISPLAY_NAME);
            account.setDisplayName(displayName);

            obtainToken(type, arguments, account, scopes);

            account.setEnabledScopes(scopes);

            String userIdentity = service.getUserIdentity(session, -1, account.getToken(), account.getSecret());
            account.setUserIdentity(userIdentity);

            DefaultOAuthAccount existingAccount = (DefaultOAuthAccount) oauthAccountStorage.findByUserIdentity(session, userIdentity, serviceMetaData, false);
            if (existingAccount == null) {
                oauthAccountStorage.storeAccount(session, account);
            } else {
                existingAccount.setEnabledScopes(scopes);
                existingAccount.setToken(account.getToken());
                existingAccount.setSecret(account.getSecret());
                oauthAccountStorage.updateAccount(session, existingAccount);
            }
            return account;
        } catch (OXException x) {
            if (ExceptionUtils.isEitherOf(x, SSLHandshakeException.class)) {
                String url = (String) arguments.get(OAuthConstants.ARGUMENT_AUTH_URL);
                if (Strings.isNotEmpty(url)) {
                    try {
                        url = new URI(url).getHost();
                    } catch (URISyntaxException e) {
                        LOG.debug("{}", e.getMessage(), e);
                    }
                    List<Object> displayArgs = new ArrayList<>(2);
                    displayArgs.add(SSLExceptionCode.extractArgument(x, "fingerprint"));
                    displayArgs.add(url);
                    throw SSLExceptionCode.UNTRUSTED_CERTIFICATE.create(x.getCause(), displayArgs.toArray(new Object[] {}));
                }
            }
            throw x;
        }
    }

    @Override
    public void deleteAccount(Session session, final int accountId) throws OXException {
        oauthAccountStorage.deleteAccount(session, accountId);
        postOAuthDeleteEvent(accountId, session);
    }

    @Override
    public void updateAccount(Session session, final int accountId, final Map<String, Object> arguments) throws OXException {
        oauthAccountStorage.updateAccount(session, accountId, arguments);
    }

    @Override
    public OAuthAccount getAccount(final Session session, final int accountId) throws OXException {
        return oauthAccountStorage.getAccount(session, accountId);
    }

    @Override
    public OAuthAccount getDefaultAccount(final API api, final Session session) throws OXException {
        final int contextId = session.getContextId();
        final int userId = session.getUserId();
        final List<OAuthServiceMetaData> allServices = registry.getAllServices(userId, contextId);
        for (final OAuthServiceMetaData metaData : allServices) {
            if (metaData.getAPI() == api) {
                final List<OAuthAccount> accounts = getAccounts(session, metaData.getId());
                OAuthAccount likely = null;
                for (final OAuthAccount acc : accounts) {
                    if (likely == null || acc.getId() < likely.getId()) {
                        likely = acc;
                    }
                }
                if (likely != null) {
                    return likely;
                }
            }
        }
        throw OAuthExceptionCodes.ACCOUNT_NOT_FOUND.create("default:" + api.toString(), Integer.valueOf(userId), Integer.valueOf(contextId));
    }

    @Override
    public OAuthAccount updateAccount(Session session, final int accountId, final String serviceMetaData, final OAuthInteractionType type, final Map<String, Object> arguments, Set<OAuthScope> scopes) throws OXException {
        isNull(arguments, OAuthConstants.ARGUMENT_SESSION);
        DefaultOAuthAccount account = new DefaultOAuthAccount();

        OAuthServiceMetaData service = registry.getService(serviceMetaData, session.getUserId(), session.getContextId());
        account.setMetaData(service);

        String displayName = (String) arguments.get(OAuthConstants.ARGUMENT_DISPLAY_NAME);
        account.setDisplayName(displayName);
        account.setId(accountId);
        obtainToken(type, arguments, account, scopes);

        //Session session = (Session) arguments.get(OAuthConstants.ARGUMENT_SESSION);
        account.setEnabledScopes(scopes);
        // Lazy identity update
        if (!oauthAccountStorage.hasUserIdentity(session, accountId, serviceMetaData)) {
            account.setUserIdentity(service.getUserIdentity(session, accountId, account.getToken(), account.getSecret()));
        }
        oauthAccountStorage.updateAccount(session, account);
        return account;
    }

    ///////////////////////////////////// HELPERS //////////////////////////////////////////

    /**
     * Get the existing account for the specified user identity or account id
     *
     * @param session The {@link Session}
     * @param userIdentity The user identity
     * @param serviceMetaData The service id
     * @param accountId The optional account id
     * @return The found account or <code>null</code> if none found
     * @throws OXException if an error is occurred
     */
    private DefaultOAuthAccount getExistingAccount(Session session, String userIdentity, String serviceMetaData, int accountId) throws OXException {
        DefaultOAuthAccount existingAccount = (DefaultOAuthAccount) oauthAccountStorage.findByUserIdentity(session, userIdentity, serviceMetaData, false);
        if (existingAccount == null) {
            // Try by account identifier if provided; should always be present in case of 'reauthorize'
            if (accountId > 0) {
                existingAccount = (DefaultOAuthAccount) oauthAccountStorage.getAccount(session, accountId, false);
            }
        }
        return existingAccount;
    }

    /**
     * Registers the specified OAuth token for deferred access (i.e. for the provider's call-back)
     *
     * @param token The token to register
     * @param cbUrl the call-back URL
     * @param ds The {@link DeferringURLService}
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    private void registerTokenForDeferredAccess(final String token, String cbUrl, final DeferringURLService ds, final int userId, final int contextId) {
        // Is only applicable if call-back URL is deferred; e.g. /ajax/defer?redirect=http:%2F%2Fmy.host.com%2Fpath...
        if (isDeferrerAvailable(ds, userId, contextId)) {
            if (ds.seemsDeferred(cbUrl, userId, contextId)) {
                callbackRegistry.add(token, cbUrl);
            } else {
                LOG.warn("Call-back URL cannot be registered as it is not deferred: {}", Strings.abbreviate(cbUrl, 32));
            }
        } else {
            // No chance to check
            callbackRegistry.add(token, cbUrl);
        }
    }

    /**
     * Checks whether the {@link DeferringURLService} is available for the specified user in the specified context
     *
     * @param ds The {@link DeferringURLService}
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return <code>true</code> if the {@link DeferringURLService} is not <code>null</code> and available;
     *         <code>false</code> otherwise
     */
    private boolean isDeferrerAvailable(final DeferringURLService ds, final int userId, final int contextId) {
        return null != ds && ds.isDeferrerURLAvailable(userId, contextId);
    }

    /**
     * Posts an OSGi delete {@link Event} for the specified account
     *
     * @param accountId The account identifier
     * @param session The session
     */
    private void postOAuthDeleteEvent(final int accountId, final Session session) {
        final EventAdmin eventAdmin = Services.getService(EventAdmin.class);
        if (null == eventAdmin) {
            return;
        }
        final Dictionary<String, Object> props = new Hashtable<String, Object>(4);
        props.put(OAuthEventConstants.PROPERTY_SESSION, session);
        props.put(OAuthEventConstants.PROPERTY_CONTEXT, Integer.valueOf(session.getContextId()));
        props.put(OAuthEventConstants.PROPERTY_USER, Integer.valueOf(session.getUserId()));
        props.put(OAuthEventConstants.PROPERTY_ID, Integer.valueOf(accountId));
        final Event event = new Event(OAuthEventConstants.TOPIC_DELETE, props);
        eventAdmin.sendEvent(event);
    }

    /**
     * Obtains an OAuth {@link Token} with the specified interaction type for the specified account
     *
     * @param type The {@link OAuthInteractionType}
     * @param arguments The arguments
     * @param account The {@link OAuthAccount}
     * @param scopes The {@link OAuthScope}s
     * @throws OXException if the token cannot be retrieved
     */
    protected void obtainToken(final OAuthInteractionType type, final Map<String, Object> arguments, final DefaultOAuthAccount account, Set<OAuthScope> scopes) throws OXException {
        switch (type) {
            case OUT_OF_BAND:
                obtainTokenByOutOfBand(arguments, account, scopes);
                break;
            case CALLBACK:
                obtainTokenByCallback(arguments, account, scopes);
                break;
            default:
                break;
        }
    }

    /**
     * Obtains a token via {@link OAuthInteractionType#CALLBACK}
     *
     * @param arguments The arguments
     * @param account The {@link OAuthAccount}
     * @param scopes The {@link OAuthScope}s
     * @throws OXException if the token cannot be retrieved
     */
    private void obtainTokenByCallback(final Map<String, Object> arguments, final DefaultOAuthAccount account, Set<OAuthScope> scopes) throws OXException {
        obtainTokenByOutOfBand(arguments, account, scopes);
    }

    /**
     * Obtains a token via {@link OAuthInteractionType#OUT_OF_BAND}
     *
     * @param arguments The arguments
     * @param account The {@link OAuthAccount}
     * @param scopes The {@link OAuthScope}s
     * @throws OXException if the token cannot be retrieved
     */
    private void obtainTokenByOutOfBand(final Map<String, Object> arguments, final DefaultOAuthAccount account, Set<OAuthScope> scopes) throws OXException {
        try {
            final OAuthServiceMetaData metaData = account.getMetaData();
            final OAuthToken oAuthToken = metaData.getOAuthToken(arguments, scopes);
            if (null == oAuthToken) {
                isNull(arguments, OAuthConstants.ARGUMENT_PIN, OAuthConstants.ARGUMENT_REQUEST_TOKEN);

                String pin = (String) arguments.get(OAuthConstants.ARGUMENT_PIN);
                OAuthToken requestToken = (OAuthToken) arguments.get(OAuthConstants.ARGUMENT_REQUEST_TOKEN);
                Session session = (Session) arguments.get(OAuthConstants.ARGUMENT_SESSION);
                // With the request token and the verifier (which is a number) we need now to get the access token
                final Verifier verifier = new Verifier(pin);
                final org.scribe.oauth.OAuthService service = getScribeService(account.getMetaData(), null, session, scopes);
                final Token accessToken = service.getAccessToken(new Token(requestToken.getToken(), requestToken.getSecret()), verifier);
                account.setToken(accessToken.getToken());
                account.setSecret(accessToken.getSecret());
            } else {
                account.setToken(oAuthToken.getToken());
                account.setSecret(oAuthToken.getSecret());
            }
        } catch (org.scribe.exceptions.OAuthException e) {
            throw handleScribeOAuthException(e);
        } catch (Exception e) {
            throw OAuthExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Retrieves the {@link org.scribe.oauth.OAuthService} for the specified provider
     *
     * @param metaData The service provider's metadata
     * @param callbackUrl The call-back URL
     * @param session The {@link Session}
     * @param scopes The {@link OAuthScope}s
     * @return The {@link org.scribe.oauth.OAuthService}
     * @throws OXException if the desired service is not supported
     */
    private org.scribe.oauth.OAuthService getScribeService(final OAuthServiceMetaData metaData, final String callbackUrl, Session session, Set<OAuthScope> scopes) throws OXException {
        final Class<? extends Api> apiClass;
        if (metaData instanceof com.openexchange.oauth.impl.ScribeAware) {
            apiClass = ((com.openexchange.oauth.impl.ScribeAware) metaData).getScribeService();
        } else {
            String serviceId = Strings.asciiLowerCase(metaData.getId());
            KnownApi knownApi = KnownApi.getApiByServiceId(serviceId);
            if (knownApi == null) {
                throw OAuthExceptionCodes.UNSUPPORTED_SERVICE.create(serviceId);
            }
            apiClass = knownApi.getApiClass();
        }
        final ServiceBuilder serviceBuilder = new ServiceBuilder().provider(apiClass);
        serviceBuilder.apiKey(metaData.getAPIKey(session)).apiSecret(metaData.getAPISecret(session));
        if (null != callbackUrl) {
            serviceBuilder.callback(callbackUrl);
        }

        // Add requested scopes
        String mappings = OAuthUtil.providerScopesToString(scopes);
        if (Strings.isNotEmpty(mappings)) {
            serviceBuilder.scope(mappings);
        }

        return serviceBuilder.build();
    }

    /**
     * Handles the specified {@link OAuthException}
     *
     * @param e The {@link OAuthException} to handle
     * @return An {@link OXException}
     */
    private OXException handleScribeOAuthException(org.scribe.exceptions.OAuthException e) {
        final String message = e.getMessage();
        if (null != message) {
            final String lcMsg = com.openexchange.java.Strings.toLowerCase(message);
            String str = "can't extract token and secret from this:";
            int pos = lcMsg.indexOf(str);
            if (pos > 0) {
                final String msg = toText(message.substring(pos + str.length()));
                return OAuthExceptionCodes.DENIED_BY_PROVIDER.create(e, msg);
            }
            str = "can't extract a token from an empty string";
            pos = lcMsg.indexOf(str);
            if (pos > 0) {
                final String msg = toText(message.substring(pos));
                return OAuthExceptionCodes.DENIED_BY_PROVIDER.create(e, msg);
            }
            str = "can't extract a token from this:";
            pos = lcMsg.indexOf(str);
            if (pos > 0) {
                final String msg = toText(message.substring(pos + str.length()));
                return OAuthExceptionCodes.DENIED_BY_PROVIDER.create(e, msg);
            }
        }
        if (e instanceof org.scribe.exceptions.OAuthConnectionException) {
            return OAuthExceptionCodes.CONNECT_ERROR.create(e, e.getMessage());
        }
        return OAuthExceptionCodes.OAUTH_ERROR.create(e, e.getMessage());
    }

    /**
     * Converts specified HTML content to plain text via the {@link HtmlService}
     *
     * @param msg the message to convert
     * @return The converted message
     */
    private String toText(final String msg) {
        final HtmlService htmlService = Services.getService(HtmlService.class);
        if (null == htmlService) {
            return msg;
        }
        if (com.openexchange.java.HTMLDetector.containsHTMLTags(Charsets.toAsciiBytes(msg), 0, msg.length())) {
            return htmlService.html2text(msg, false);
        }
        return msg;
    }

    /**
     * Checks the specified {@link Map} with arguments for <code>null</code> values of the specified fields
     *
     * @param arguments The {@link Map} with the arguments
     * @param fields The fields to check
     * @throws OXException if an argument is missing or has a <code>null</code> value
     */
    private void isNull(Map<String, Object> arguments, String... fields) throws OXException {
        for (String field : fields) {
            Object object = arguments.get(field);
            if (null == object) {
                throw OAuthExceptionCodes.MISSING_ARGUMENT.create(field);
            }
        }
    }
}
