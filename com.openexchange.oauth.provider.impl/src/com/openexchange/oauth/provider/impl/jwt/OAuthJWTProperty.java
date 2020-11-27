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

package com.openexchange.oauth.provider.impl.jwt;

import com.openexchange.authentication.NamePart;
import com.openexchange.config.lean.Property;

/**
 * {@link OAuthJWTProperty}
 *
 * @author <a href="mailto:sebastian.lutz@open-xchange.com">Sebastian Lutz</a>
 * @since v7.10.5
 */
public enum OAuthJWTProperty implements Property {

    /**
     * Specifies a JWKS endpoint used to fetch signature keys for validation.
     */
    JWKS_ENDPOINT("jwks.endpoint", OAuthJWTProperty.EMPTY),

    /**
     * Path to locally populated keystore if JWKs are not fetched from remote.
     */
    KEYSTORE_PATH("keystore.path", OAuthJWTProperty.EMPTY),

    /**
     * Password for the locally populated keystore.
     */
    KEYSTORE_PASSWORD("keystore.password", OAuthJWTProperty.EMPTY),

    /**
     * The keystore type.
     */
    KEYSTORE_TYPE("keystore.type", "JKS"),

    /**
     * A comma separated list of issuer names (JWT claim "iss") that tokens are accepted from.
     * If this property is empty, tokens are accepted from all issuers.
     */
    ALLOWED_ISSUER("allowedIssuer", OAuthJWTProperty.EMPTY),

    /**
     * Name of the JWT claim that will be used to resolve a context.
     */
    CONTEXT_LOOKUP_CLAIM("contextLookupClaim", "sub"),

    /**
     * Gets the {@link NamePart} used for determining the context
     * of a user for which a JWT has been obtained. The part
     * is taken from the value of the according {@link OAuthJWTProperty#CONTEXT_LOOKUP_CLAIM}.
     */
    CONTEXT_LOOKUP_NAME_PART("contextLookupNamePart", NamePart.DOMAIN.getConfigName()),

    /**
     * Name of the JWT claim that will be used to resolve a user.
     */
    USER_LOOKUP_CLAIM("userLookupClaim", "sub"),

    /**
     * Gets the {@link NamePart} used for determining the user for
     * which a JWT has been obtained. The part is taken from
     * the value of the according {@link OAuthJWTProperty#USER_LOOKUP_CLAIM}.
     */
    USER_LOOKUP_NAME_PART("userLookupNamePart", NamePart.LOCAL_PART.getConfigName());

    public static final String PREFIX = "com.openexchange.oauth.provider.";
    private static final String EMPTY = "";
    private final String fqn;
    private final Object defaultValue;

    /**
     * Initializes a new {@link OAuthJWTProperty}.
     *
     * @param suffix the suffix
     * @param defaultValue the default value
     */
    private OAuthJWTProperty(String suffix, Object defaultValue) {
        this.fqn = PREFIX + suffix;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getFQPropertyName() {
        return fqn;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }
}
