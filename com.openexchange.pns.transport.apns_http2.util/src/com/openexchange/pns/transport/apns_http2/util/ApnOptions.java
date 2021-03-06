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

package com.openexchange.pns.transport.apns_http2.util;

import com.openexchange.java.Strings;

/**
 * {@link ApnOptions} - Holds the (immutable) options to communicate with the Apple Push Notification System.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ApnOptions {

    /** The authentication types supported by APNs */
    public static enum AuthType {

        /** Connect to APNs using provider certificates */
        CERTIFICATE("certificate"),
        /** Connect to APNs using provider authentication JSON Web Token (JWT) */
        JWT("jwt"),
        ;

        private final String id;

        private AuthType(String id) {
            this.id = id;
        }

        /**
         * Gets the identifier
         *
         * @return The identifier
         */
        public String getId() {
            return id;
        }

        /**
         * Gets the authentication type for specified identifier.
         *
         * @param id The identifier
         * @return The authentication type or <code>null</code>
         */
        public static AuthType authTypeFor(String id) {
            if (Strings.isEmpty(id)) {
                return null;
            }

            for (AuthType authType : AuthType.values()) {
                if (authType.id.equalsIgnoreCase(id)) {
                    return authType;
                }
            }
            return null;
        }
    }

    private final AuthType authType;
    private final String password;
    private final Object keystore;
    private final boolean production;
    private final String topic;
    private final String clientId;
    private final Object privateKey;
    private final String keyId;
    private final String teamId;

    private ApnOptions(AuthType authType, Object keystore, String password, Object privateKey, String keyId, String teamId, String topic, boolean production, String clientId) {
        super();
        this.authType = authType;
        this.keystore = keystore;
        this.password = password;
        this.privateKey = privateKey;
        this.keyId = keyId;
        this.teamId = teamId;
        this.topic = topic;
        this.production = production;
        this.clientId = clientId;
    }

    /**
     * Initializes a new immutable {@link ApnOptions} instance.
     *
     * @param keystore A keystore containing the private key and the certificate signed by Apple.<br>
     *                 The following formats can be used:
     *                 <ul>
     *                 <li><code>java.io.File</code></li>
     *                 <li><code>java.io.InputStream</code></li>
     *                 <li><code>byte[]</code></li>
     *                 <li><code>java.security.KeyStore</code></li>
     *                 <li><code>java.lang.String</code> for a file path</li>
     *                 </ul>
     * @param password The keystore's password.
     * @param production <code>true</code> to use Apple's production servers, <code>false</code> to use the sandbox servers
     * @param topic The push topic, should be the app's bundle identifier
     * @param clientId The client identifier
     */
    public ApnOptions(Object keystore, String password, boolean production, String topic, String clientId) {
        this(AuthType.CERTIFICATE, keystore, password, null, null, null, topic, production, clientId);
    }

    /**
     * Initializes a new immutable {@link ApnOptions} instance.
     *
     * @param privateKey The APNS authentication key signed by Apple.<br>
     *                 The following formats can be used:
     *                 <ul>
     *                 <li><code>java.io.File</code></li>
     *                 <li><code>java.io.InputStream</code></li>
     *                 <li><code>byte[]</code></li>
     *                 <li><code>java.lang.String</code> for a file path</li>
     *                 </ul>
     * @param keyId The key identifier obtained from developer account
     * @param teamId The team identifier obtained from developer account
     * @param production <code>true</code> to use Apple's production servers, <code>false</code> to use the sandbox servers
     * @param topic The push topic, should be the app's bundle identifier
     * @param clientId The client identifier
     */
    public ApnOptions(Object privateKey, String keyId, String teamId, boolean production, String topic, String clientId) {
        this(AuthType.JWT, null, null, privateKey, keyId, teamId, topic, production, clientId);
    }

    /**
     * Gets the {@link AuthType}
     *
     * @return The auth type
     */
    public AuthType getAuthType() {
        return authType;
    }

    /**
     * Gets the password
     *
     * @return The password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the keystore
     *
     * @return The keystore
     */
    public Object getKeystore() {
        return keystore;
    }

    /**
     * Gets the production
     *
     * @return The production
     */
    public boolean isProduction() {
        return production;
    }

    /**
     * Gets the topic
     *
     * @return The topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Gets the client identifier
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Gets the private key
     *
     * @return The private key
     */
    public Object getPrivateKey() {
        return privateKey;
    }

    /**
     * Gets the key identifier
     *
     * @return The key identifier
     */
    public String keyId() {
        return keyId;
    }

    /**
     * Gets the team identifier
     *
     * @return The team identifier
     */
    public String teamId() {
        return teamId;
    }

}
