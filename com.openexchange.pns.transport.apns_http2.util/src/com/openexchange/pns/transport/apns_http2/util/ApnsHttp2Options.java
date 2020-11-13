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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.pns.PushExceptionCodes;
import com.turo.pushy.apns.ApnsClient;
import com.turo.pushy.apns.ApnsClientBuilder;
import com.turo.pushy.apns.auth.ApnsSigningKey;

/**
 * {@link ApnsHttp2Options} - Holds the (immutable) options to communicate with the Apple Push Notification System via HTTP/2.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.0
 */
public class ApnsHttp2Options {

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

    // --------------------------------------------------------------------------------------------------------

    private final String clientId;
    private final AuthType authType;

    private final Object privateKey;
    private final String keyId;
    private final String teamId;

    private final String password;
    private final Object keystore;

    private final boolean production;
    private final String topic;

    private final AtomicReference<ApnsClient> clientReference;

    /**
     * Initializes a new immutable {@link ApnsHttp2Options} instance using a provider certificate.
     *
     * @param clientId The identifier of the associated client
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
     * @param bundleIdentifier The bundle identifier of the app
     * @param topic The app's topic, which is typically the bundle ID of the app
     */
    public ApnsHttp2Options(String clientId, Object keystore, String password, boolean production, String topic) {
        super();
        this.clientId = clientId;
        clientReference = new AtomicReference<ApnsClient>(null);
        authType = AuthType.CERTIFICATE;
        this.keystore = keystore;
        this.password = password;
        this.production = production;
        this.topic = topic;
        this.privateKey = null;
        this.keyId = null;
        this.teamId = null;
    }

    /**
     * Initializes a new immutable {@link ApnsHttp2Options} instance using a provider JSON Web Token (JWT).
     *
     * @param clientId The identifier of the associated client
     * @param privateKey The APNS authentication key; excluding <code>"-----BEGIN PRIVATE KEY-----"</code> and <code>"-----END PRIVATE KEY-----"</code>
     * @param keyId The key identifier obtained from developer account
     * @param teamId The team identifier obtained from developer account
     * @param production <code>true</code> to use Apple's production servers, <code>false</code> to use the sandbox servers
     * @param bundleIdentifier The bundle identifier of the app
     * @param topic The app's topic, which is typically the bundle ID of the app
     */
    public ApnsHttp2Options(String clientId, Object privateKey, String keyId, String teamId, boolean production, String topic) {
        super();
        this.clientId = clientId;
        clientReference = new AtomicReference<ApnsClient>(null);
        authType = AuthType.JWT;
        this.keystore = null;
        this.password = null;
        this.production = production;
        this.topic = topic;
        this.privateKey = privateKey;
        this.keyId = keyId;
        this.teamId = teamId;
    }

    /**
     * Gets the authentication type
     *
     * @return The authentication type
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
     * Gets the key identifier obtained from developer account
     *
     * @return The key identifier obtained from developer account
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * Gets the team identifier obtained from developer account
     *
     * @return The team identifier obtained from developer account
     */
    public String getTeamId() {
        return teamId;
    }

    /**
     * Gets the APNS auth key; excluding <code>"-----BEGIN PRIVATE KEY-----"</code> and <code>"-----END PRIVATE KEY-----"</code>
     *
     * @return The APNS auth key
     */
    public Object getPrivateKey() {
        return privateKey;
    }

    /**
     * Gets the apps's topic, which is typically the bundle ID of the app.
     *
     * @return The topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Gets the appropriate APNS HTTP/2 client for this options instance.
     *
     * @return The APNS HTTP/2 client
     * @throws OXException If creation of APNS HTTP/2 client fails
     */
    public ApnsClient getApnsClient() throws OXException {
        ApnsClient client = clientReference.get();
        if (null == client) {
            synchronized (this) {
                client = clientReference.get();
                if (null == client) {
                    client = createNewApnsClient();
                    clientReference.set(client);
                }
            }
        }
        return client;
    }

    private ApnsClient createNewApnsClient() throws OXException {
        try {
            ApnsClientBuilder clientBuilder = new ApnsClientBuilder();
            if (AuthType.CERTIFICATE == getAuthType()) {
                if (File.class.isInstance(keystore)) {
                    File keyStore = (File) keystore;
                    clientBuilder.setApnsServer(isProduction() ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                        .setClientCredentials(keyStore, getPassword());
                } else if (InputStream.class.isInstance(keystore)) {
                    InputStream keyStore = (InputStream) keystore;
                    clientBuilder.setApnsServer(isProduction() ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                        .setClientCredentials(keyStore, getPassword());
                } else {
                    byte[] keyStore = (byte[]) keystore;
                    clientBuilder.setApnsServer(isProduction() ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                    .setClientCredentials(new ByteArrayInputStream(keyStore), getPassword());
                }
            } else {
                Object privateKey = getPrivateKey();
                if (File.class.isInstance(privateKey)) {
                    clientBuilder.setApnsServer(isProduction() ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                        .setSigningKey(ApnsSigningKey.loadFromPkcs8File((File) privateKey, getTeamId(), getKeyId()));
                } else if (InputStream.class.isInstance(privateKey)) {
                    clientBuilder.setApnsServer(isProduction() ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                        .setSigningKey(ApnsSigningKey.loadFromInputStream((InputStream) privateKey, getTeamId(), getKeyId()));
                } else {
                    clientBuilder.setApnsServer(isProduction() ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                    .setSigningKey(ApnsSigningKey.loadFromInputStream(new ByteArrayInputStream((byte[]) privateKey), getTeamId(), getKeyId()));
                }
            }
            return clientBuilder.build();
        } catch (FileNotFoundException e) {
            throw PushExceptionCodes.UNEXPECTED_ERROR.create(e, "No such keystore file for client " + clientId + ": " + getKeystore());
        } catch (NoSuchAlgorithmException e) {
            throw PushExceptionCodes.UNEXPECTED_ERROR.create(e, "The algorithm used to check the integrity of the keystore for client " + clientId + " cannot be found");
        } catch (IOException e) {
            throw PushExceptionCodes.UNEXPECTED_ERROR.create(e, "There is an I/O or format problem with the keystore data of the keystore for client " + clientId + " or specified password is invalid");
        } catch (InvalidKeyException e) {
            throw PushExceptionCodes.UNEXPECTED_ERROR.create(e, "Invalid private key specified for client " + clientId);
        }
    }

}
