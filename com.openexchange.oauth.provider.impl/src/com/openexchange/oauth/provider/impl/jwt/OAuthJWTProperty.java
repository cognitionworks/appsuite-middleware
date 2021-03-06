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

import com.openexchange.config.lean.Property;

/**
 * {@link OAuthJWTProperty}
 *
 * @author <a href="mailto:sebastian.lutz@open-xchange.com">Sebastian Lutz</a>
 * @since v7.10.5
 */
public enum OAuthJWTProperty implements Property {

    /**
     * Specifies a JWKS URI used to fetch signature keys for validation.
     * This URI can either be local file path or an URL.
     *
     * Example:
     *  file:/Users/sebastianlutz/git/core/com.openexchange.oauth.provider.impl/conf/jwk.json,
     *  http://127.0.0.1:8085/auth/realms/demo/protocol/openid-connect/certs
     *
     */
    JWKS_URI("jwksUri", OAuthJWTProperty.EMPTY),

    /**
     * Path for loading a JWK set from a local JSON file
     */
    JWKS_JSON_PATH("jwksJson.path", OAuthJWTProperty.EMPTY);

    public static final String PREFIX = "com.openexchange.oauth.provider.jwt.";
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
