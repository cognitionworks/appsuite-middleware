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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.file.storage.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.file.storage.FileStorageException;
import com.openexchange.file.storage.FileStorageExceptionCodes;

/**
 * {@link ConfigFileStorageAccountParser} - Provides configured accounts parsed from a <i>.properties</i> file.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since Open-Xchange v6.18.2
 */
public final class ConfigFileStorageAccountParser {

    private static final ConfigFileStorageAccountParser INSTANCE = new ConfigFileStorageAccountParser();

    /**
     * Gets the instance.
     * 
     * @return The instance
     */
    public static ConfigFileStorageAccountParser getInstance() {
        return INSTANCE;
    }

    private volatile Map<String, Map<String, ConfigFileStorageAccount>> map;

    /**
     * Initializes a new {@link ConfigFileStorageAccountParser}.
     */
    private ConfigFileStorageAccountParser() {
        super();
        map = Collections.emptyMap();
    }

    /**
     * Drops formerly parsed properties.
     */
    public void drop() {
        map = Collections.emptyMap();
    }

    /**
     * Gets the configured accounts for specified service identifier.
     * 
     * @param serviceId The service identifier
     * @return The configured accounts
     */
    public Map<String, ConfigFileStorageAccount> getAccountsFor(final String serviceId) {
        return map.get(serviceId);
    }

    private static final String PREFIX = "com.openexchange.file.storage.account.";

    private static final int PREFIX_LEN = PREFIX.length();

    /**
     * Parses specified properties to a map associating service identifier with configured file storage accounts.
     * 
     * @param properties The properties to parse
     */
    public void parse(final Properties properties) {
        /*
         * Parse identifiers
         */
        final Set<String> ids = new HashSet<String>();
        for (final Object key : properties.keySet()) {
            final String propName = ((String) key).toLowerCase(Locale.ENGLISH);
            if (propName.startsWith(PREFIX)) {
                final String id = propName.substring(PREFIX_LEN, propName.indexOf('.', PREFIX_LEN));
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        /*
         * Get the accounts for identifiers
         */
        final Map<String, Map<String, ConfigFileStorageAccount>> m = new ConcurrentHashMap<String, Map<String, ConfigFileStorageAccount>>();
        for (final String id : ids) {
            try {
                final ConfigFileStorageAccount account = parseAccount(id, properties);
                final String serviceId = account.getFileStorageService().getId();
                Map<String, ConfigFileStorageAccount> map = m.get(serviceId);
                if (null == map) {
                    map = new ConcurrentHashMap<String, ConfigFileStorageAccount>(2);
                    m.put(serviceId, map);
                }
                map.put(account.getId(), account);
            } catch (final FileStorageException e) {
                final Log logger = LogFactory.getLog(ConfigFileStorageAccountParser.class);
                logger.warn("Configuration for file storage account \"" + id + "\" is invalid: " + e.getMessage(), e);
            }
        }
        this.map = m;
    }

    private static ConfigFileStorageAccount parseAccount(final String id, final Properties properties) throws FileStorageException {
        final StringBuilder sb = new StringBuilder(PREFIX).append(id).append('.');
        final int resetLen = sb.length();
        /*
         * Create account
         */
        final ConfigFileStorageAccount account = new ConfigFileStorageAccount();
        account.setId(id);
        /*
         * Parse display name
         */
        final String displayName = properties.getProperty(sb.append("displayName").toString());
        if (null == displayName) {
            throw FileStorageExceptionCodes.MISSING_PARAMETER.create("displayName");
        }
        account.setDisplayName(dropQuotes(displayName));
        /*
         * Parse service identifier
         */
        sb.setLength(resetLen);
        final String serviceId = properties.getProperty(sb.append("serviceId").toString());
        if (null == serviceId) {
            throw FileStorageExceptionCodes.MISSING_PARAMETER.create("serviceId");
        }
        account.setServiceId(dropQuotes(serviceId));
        /*
         * Parse configuration
         */
        sb.setLength(resetLen);
        sb.append("config.");
        final String configPrefix = sb.toString();
        final int configPrefixLen = configPrefix.length();
        final Map<String, Object> configuration = new HashMap<String, Object>();
        for (final Entry<Object, Object> entry : properties.entrySet()) {
            final String value = (String) entry.getValue();
            final String propName = ((String) entry.getKey()).toLowerCase(Locale.ENGLISH);
            if (propName.startsWith(configPrefix) && null != value) {
                configuration.put(propName.substring(configPrefixLen), dropQuotes(value));
            }
        }
        account.setConfiguration(configuration);
        return account;
    }

    private static String dropQuotes(final String value) {
        final int mlen = value.length() - 1;
        if (mlen > 1 && '"' == value.charAt(0) && '"' == value.charAt(mlen)) {
            return value.substring(1, mlen);
        }
        return value;
    }

}
