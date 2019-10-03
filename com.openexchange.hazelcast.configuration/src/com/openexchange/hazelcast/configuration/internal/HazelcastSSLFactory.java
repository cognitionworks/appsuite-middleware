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

package com.openexchange.hazelcast.configuration.internal;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.ImmutableList;
import com.hazelcast.nio.ssl.SSLContextFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.java.ConfigAwareKeyStore;
import com.openexchange.java.Strings;

/**
 * {@link HazelcastSSLFactory}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.1
 */
public class HazelcastSSLFactory implements SSLContextFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(HazelcastSSLFactory.class);

    private static final String PROP_SSL_PROTOCOLS = "com.openexchange.hazelcast.network.ssl.protocols";

    private static final String PROP_TRUST_STORE     = "com.openexchange.hazelcast.network.ssl.trustStore";
    private static final String PROP_TRUST_PASSWORD  = "com.openexchange.hazelcast.network.ssl.trustStorePassword";
    private static final String PROP_TRUST_TYPE      = "com.openexchange.hazelcast.network.ssl.trustStoreType";
    private static final String PROP_TRUST_ALGORITHM = "com.openexchange.hazelcast.network.ssl.trustManagerAlgorithm";

    private static final String PROP_KEY_STORE     = "com.openexchange.hazelcast.network.ssl.keyStore";
    private static final String PROP_KEY_PASSWORD  = "com.openexchange.hazelcast.network.ssl.keyStorePassword";
    private static final String PROP_KEY_TYPE      = "com.openexchange.hazelcast.network.ssl.keyStoreType";
    private static final String PROP_KEY_ALGORITHM = "com.openexchange.hazelcast.network.ssl.keyManagerAlgorithm";

    private final AtomicReference<SSLContext> sslContext;

    private final ConcurrentHashMap<String, ConfigAwareKeyStore> stores;

    /**
     * Initializes a new {@link HazelcastSSLFactory}.
     */
    public HazelcastSSLFactory() {
        super();
        this.stores = new ConcurrentHashMap<>(3);
        stores.put("truststore", new ConfigAwareKeyStore(PROP_TRUST_STORE, PROP_TRUST_PASSWORD, PROP_TRUST_TYPE));
        stores.put("keystore", new ConfigAwareKeyStore(PROP_KEY_STORE, PROP_KEY_PASSWORD, PROP_KEY_TYPE));
        this.sslContext = new AtomicReference<SSLContext>(null);
    }

    @Override
    public SSLContext getSSLContext() {
        return sslContext.get();
    }

    @Override
    public void init(Properties properties) throws Exception {
        if (properties.isEmpty()) {
            LOGGER.error("Can't initialize SSLContext without properties");
            return;
        }

        SSLContext context = this.sslContext.get();
        String protocols = properties.getProperty(PROP_SSL_PROTOCOLS, "TLS,TLSv1,TLSv1.1,TLSv1.2");
        if (Strings.isEmpty(protocols)) {
            throw new IllegalArgumentException("Property \"" + PROP_SSL_PROTOCOLS + "\" must not be empty!");
        }

        // Check if this is a reload
        if (null == context) {
            // Not yet initialized
            context = getSSLContext(protocols, true);
            loadKeyStore(properties);
        } else {
            SSLContext reloadedContext = getSSLContext(protocols, false);
            if (false == context.getProtocol().equals(reloadedContext.getProtocol())) {
                // New protocol, we need to reload
                context = reloadedContext;
            } else if (false == loadKeyStore(properties)) {
                // Nothing changed
                return;
            }
        }

        try {
            // Initialize SSLContext
            TrustManagerFactory trustManagerFactory = null;
            KeyManagerFactory keyManagerFactory = null;

            ConfigAwareKeyStore trustStore = stores.get("truststore");
            if (null != trustStore && trustStore.isConfigured()) {
                trustManagerFactory = TrustManagerFactory.getInstance(properties.getProperty(PROP_TRUST_ALGORITHM, TrustManagerFactory.getDefaultAlgorithm()));
                trustManagerFactory.init(trustStore.getKeyStore());
            }

            ConfigAwareKeyStore keyStore = stores.get("keystore");
            if (null != keyStore && keyStore.isConfigured()) {
                keyManagerFactory = KeyManagerFactory.getInstance(properties.getProperty(PROP_KEY_ALGORITHM, KeyManagerFactory.getDefaultAlgorithm()));
                keyManagerFactory.init(keyStore.getKeyStore(), null != properties.getProperty(PROP_KEY_PASSWORD) ? properties.getProperty(PROP_KEY_PASSWORD).toCharArray() : null);
            }

            context.init(null == keyManagerFactory ? new KeyManager[] {} : keyManagerFactory.getKeyManagers(), null == trustManagerFactory ? new TrustManager[] {} : trustManagerFactory.getTrustManagers(), null);
            this.sslContext.set(context);
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e) {
            LOGGER.error("Unable to initialize SSLContext for Hazelcast.", e);
            throw e;
        }
    }

    /**
     * Get the {@link SSLContext} to the first matching protocol
     *
     * @param protocols A comma-separated list of protocols to get the {@link SSLContext} for
     * @param log <code>true</code> if log messages should be written
     * @return The {@link SSLContext} for the first protocol that matches. See {@link SSLContext#getInstance(String)}
     * @throws IllegalStateException If no SSLContext could be loaded
     */
    private SSLContext getSSLContext(String protocols, boolean log) throws IllegalStateException {
        String[] prots = Strings.splitByComma(protocols);
        if (prots.length == 0) {
            throw new IllegalStateException("No protocols given to find an SSLContext");
        }

        for (int i = prots.length; i-- > 0;) {
            String protocol = prots[i];
            try {
                SSLContext context = SSLContext.getInstance(protocol);
                if (log) {
                    LOGGER.info("Using {} for Hazelcast encryption", protocol);
                }
                return context;
            } catch (Throwable e) {
                if (log) {
                    LOGGER.info("Didn't find SSLContext for {}. Trying next protocol in list.", protocol);
                }
            }
        }
        throw new IllegalStateException("Unable to find SSLContexts for: " + Arrays.toString(prots));
    }

    private boolean loadKeyStore(Properties properties) {
        boolean retval = false;
        for (Map.Entry<String, ConfigAwareKeyStore> entry : stores.entrySet()) {
            try {
                retval |= entry.getValue().reloadStore(properties);
            } catch (Exception e) {
                LOGGER.error("Unable to load keystore!", e);
            }
        }
        return retval;
    }

    private static final List<String> SSL_PROPERTY_NAMES = ImmutableList.of(
        PROP_SSL_PROTOCOLS,
        PROP_TRUST_STORE,
        PROP_TRUST_PASSWORD,
        PROP_TRUST_TYPE,
        PROP_TRUST_ALGORITHM,
        PROP_KEY_STORE,
        PROP_KEY_PASSWORD,
        PROP_KEY_TYPE,
        PROP_KEY_ALGORITHM);

    /**
     * Gets all necessary properties for an SSL configuration
     *
     * @param configService The {@link ConfigurationService} to get the properties from
     * @return The SSL properties
     */
    public static Properties getPropertiesFromService(ConfigurationService configService) {
        Properties properties = new Properties();
        for (String propertyName : SSL_PROPERTY_NAMES) {
            String value = configService.getProperty(propertyName);
            if (Strings.isNotEmpty(value)) {
                properties.setProperty(propertyName, value);
            }
        }
        return properties;
    }
}
