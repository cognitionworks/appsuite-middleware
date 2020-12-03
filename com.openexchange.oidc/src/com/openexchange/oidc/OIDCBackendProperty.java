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
package com.openexchange.oidc;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.openexchange.authentication.NamePart;
import com.openexchange.config.lean.Property;


/**
 * Backend specific properties. OP = OpenID Provider
 *
 * @author <a href="mailto:vitali.sjablow@open-xchange.com">Vitali Sjablow</a>
 * @since v7.10.0
 */
public enum OIDCBackendProperty implements Property {
    /**
     * clientId - The client id, which was assigned by thr OP to this client/backend
     * on registration
     */
    clientId(OIDCProperty.PREFIX, OIDCProperty.EMPTY),
    /**
     * redirectURIAuth - The path to the authentication servlet of this backend
     */
    rpRedirectURIAuth(OIDCProperty.PREFIX, OIDCProperty.EMPTY),
    /**
     * authorizationEndpoint - The OPs authorization endpoint
     */
    opAuthorizationEndpoint(OIDCProperty.PREFIX, OIDCProperty.EMPTY),
    /**
     * tokenEndpoint - The OPs token endpoint
     */
    opTokenEndpoint(OIDCProperty.PREFIX, OIDCProperty.EMPTY),
    /**
     * clientSecret - The client secret, which was assigned by the OP to this client/backend
     * on registration
     */
    clientSecret(OIDCProperty.PREFIX, OIDCProperty.EMPTY),
    /**
     * jwkSetEndpoint - The OPs JWK Set endpoint
     */
    opJwkSetEndpoint(OIDCProperty.PREFIX, OIDCProperty.EMPTY),
    /**
     * jwsAlgorithm - The used JWS encryption algorithm
     */
    jwsAlgorithm(OIDCProperty.PREFIX, OIDCProperty.EMPTY),
    /**
     * scope - The scope to request during OIDC authorization flow. This is a space-separated list
     * of scope values, e.g. <code>openid offline</code>. Scope values are case-sensitive!
     */
    scope(OIDCProperty.PREFIX, "openid"),
    /**
     * issuer - The OPs issuer path
     */
    opIssuer(OIDCProperty.PREFIX, OIDCProperty.EMPTY),
    /**
     * responseType - The OPs response type, which also identifies the used flow
     */
    responseType(OIDCProperty.PREFIX, "code"),
    /**
     * logoutEndpoint - The OPs logout endpoint
     */
    opLogoutEndpoint(OIDCProperty.PREFIX, OIDCProperty.EMPTY),
    /**
     * redirectURIPostSSOLogout - The location where the Browser should be redirected after logout
     * from OP
     */
    rpRedirectURIPostSSOLogout(OIDCProperty.PREFIX, OIDCProperty.EMPTY),
    /**
     * ssoLogout - Whether to redirect to the OP on logout trigger from client or not
     */
    ssoLogout(OIDCProperty.PREFIX, Boolean.FALSE),
    /**
     * redirectURILogout - Where to redirect the user after a valid logout
     */
    rpRedirectURILogout(OIDCProperty.PREFIX, OIDCProperty.EMPTY),
    /**
     * autologinCookieMode - Which login mode is enabled look at {@link OIDCBackendConfig.AutologinMode}
     * for all valid values.
     * so far the following values are valid: {off, ox_direct}.
     * <br>
     *   off - no autologin<br>
     *   ox_direct - load user session from cookie and load Appsuite directly<br>
     */
    autologinCookieMode(OIDCProperty.PREFIX, OIDCBackendConfig.AutologinMode.OX_DIRECT.getValue()),
    /**
     * oauthRefreshTime - Time in milliseconds determines how long before the expiration of the
     * OAuth {@link AccessToken} a new {@link AccessToken} should be requested. "refresh_token"
     * grant type must be registered for this client.
     */
    oauthRefreshTime(OIDCProperty.PREFIX, 60000),
    /**
     * uiWebPath - This backends UI path
     */
    uiWebPath(OIDCProperty.PREFIX, OIDCProperty.EMPTY),
    /**
     * backendPath - This backends servlet path, which is appended to the default /oidc/ path.
     */
    backendPath(OIDCProperty.PREFIX, OIDCProperty.EMPTY),
    /**
     * hosts - This contains a comma separated list of hosts, that this backend supports.
     */
    hosts(OIDCProperty.PREFIX, "all"),
    /**
     * failureRedirect - Defines where a user should be redirected if an error occurs that
     * does not need a special handling.
     */
    failureRedirect(OIDCProperty.PREFIX, OIDCProperty.EMPTY),
    /**
     * contextLookupClaim - Gets the name of the ID token claim that will be used by the
     * default backend to resolve a context.
     */
    contextLookupClaim(OIDCProperty.PREFIX, "sub"),
    /**
     * contextLookupNamePart - Gets the {@link NamePart} of the ID token claim value used for
     * determining the context of a user.
     *
     * @see #contextLookupClaim
     */
    contextLookupNamePart(OIDCProperty.PREFIX, NamePart.DOMAIN.getConfigName()),
    /**
     * userLookupClaim - Gets the name of the ID token claim that will be used by the
     * default backend to resolve a user.
     */
    userLookupClaim(OIDCProperty.PREFIX, "sub"),
    /**
     * userLookupNamePart - Gets the {@link NamePart} of the ID token claim value used for
     * determining a user.
     *
     * @see #userLookupClaim
     */
    userLookupNamePart(OIDCProperty.PREFIX, NamePart.LOCAL_PART.getConfigName()),
    /**
     * passwordGrantUserNamePart - Gets the {@link NamePart} to be used for an issued Resource
     * Owner Password Credentials Grant ({@link https://tools.ietf.org/html/rfc6749#section-4.3}).
     * The part is taken from the user-provided login name.
     *
     * @see OIDCProperty#enablePasswordGrant
     */
    passwordGrantUserNamePart(OIDCProperty.PREFIX, NamePart.FULL.getConfigName()),
    /**
     * tokenLockTimeoutSeconds - Lock timeout before giving up trying to refresh an access token for
     * a session. If multiple threads try to check or refresh the access token
     * at the same time, only one gets a lock and blocks the others. In case
     * of a timeout, this is logged as a temporary issue and the request continued
     * as usual.
     */
    tokenLockTimeoutSeconds(OIDCProperty.PREFIX, 5),
    /**
     * tryRecoverStoredTokens - Whether token refresh should try to recover valid tokens from
     * the session instance that is present in {@link SessionStorageService}.
     * This is only tried as a fall-back, after token refresh failed with an
     * {@code invalid_grant} error.
     */
    tryRecoverStoredTokens(OIDCProperty.PREFIX, Boolean.FALSE)
    ;

    private final String fqn;
    private final Object defaultValue;

    private OIDCBackendProperty(String fqn, Object defaultValue) {
        this.fqn = fqn;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getFQPropertyName() {
        return fqn + name();
    }

    @Override
    public Object getDefaultValue() {
        return this.defaultValue;
    }

}
