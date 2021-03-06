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

/**
 * {@link DefaultOAuthToken} - The default OAuth token implementation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class DefaultOAuthToken implements OAuthToken {

    private String token;
    private String secret;
    private long expiry;

    /**
     * Initializes a new {@link DefaultOAuthToken}.
     */
    public DefaultOAuthToken() {
        super();
    }

    /**
     * Initializes a new {@link DefaultOAuthToken}.
     *
     * @param token The token string
     * @param secret The secret string
     * @param expiry The expiration time stamp
     */
    public DefaultOAuthToken(final String token, final String secret, long expiry) {
        super();
        this.token = token;
        this.secret = secret;
        this.expiry = expiry;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public String getSecret() {
        return secret;
    }

    @Override
    public long getExpiration() {
        return expiry;
    }

    /**
     * Sets the token
     *
     * @param token The token to set
     */
    public void setToken(final String token) {
        this.token = token;
    }

    /**
     * Sets the secret
     *
     * @param secret The secret to set
     */
    public void setSecret(final String secret) {
        this.secret = secret;
    }

    /**
     * Sets the expiration time stamp
     *
     * @param expiry The expiration time stamp to set
     */
    public void setExpiration(long expiry) {
        this.expiry = expiry;
    }
}
