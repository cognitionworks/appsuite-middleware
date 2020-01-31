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

package com.openexchange.config.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.cascade.ConfigProviderService;
import com.openexchange.config.cascade.ReinitializableConfigProviderService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;

/**
 * {@link ConfigProviderServiceImpl} - The implementation of ConfigProviderService for the scope <code>"server"</code>.
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ConfigProviderServiceImpl implements ReinitializableConfigProviderService {

    private static final String META = "meta";

    private static final String SETTINGS = "settings";

    private static final String PREFRENCE_PATH = "preferencePath";

    private static final String VALUE = "value";

    private static final String PROTECTED = "protected";

    private static final String TRUE = "true";

    private static final Map<String, String> METADATA_PROTECTED = Collections.singletonMap(PROTECTED, TRUE);

    private static final ServerProperty EMPTY_PROPERTY = new ServerProperty(null, METADATA_PROTECTED);

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ConfigProviderServiceImpl.class);

    // -------------------------------------------------------------------------------------------------------------------

    private final ConfigurationService configService;
    private final ConcurrentMap<String, ServerProperty> properties;

    /**
     * Initializes a new {@link ConfigProviderServiceImpl}.
     *
     * @param configService The associated configuration service
     * @throws OXException If initialization fails
     */
    public ConfigProviderServiceImpl(final ConfigurationService configService) {
        super();
        this.configService = configService;
        properties = new ConcurrentHashMap<String, ServerProperty>(2048, 0.9f, 1);
        init(configService);
    }

    @Override
    public ServerProperty get(final String property, final int contextId, final int userId) throws OXException {
        if (null == property) {
            return null;
        }
        ServerProperty serverProperty = properties.get(property);
        if (null != serverProperty) {
            return serverProperty;
        }
        /*
         * no server property available yet, create if defined
         */
        String value = configService.getProperty(property);
        if (null == value) {
            if (property.startsWith("com.openexchange.capability.")) {
                Exception e = new Exception("No value for property \"" + property+ "\"");
                LOG.debug("Requested undefined server property as 'capability' through \"get({}, {}, {})\"",
                    property, Integer.valueOf(contextId), Integer.valueOf(userId), e);
            }
            return EMPTY_PROPERTY;
        }
        serverProperty = new ServerProperty(value, METADATA_PROTECTED);
        ServerProperty existingProperty = properties.putIfAbsent(property, serverProperty);
        return null != existingProperty ? existingProperty : serverProperty;
    }

    @Override
    public String getScope() {
    	return "server";
    }

    @Override
    public Collection<String> getAllPropertyNames(final int contextId, final int userId) throws OXException {
        final Iterator<String> propertyNames = configService.propertyNames();
        final Set<String> retval = new HashSet<String>();
        while (propertyNames.hasNext()) {
            retval.add(propertyNames.next());
        }
        retval.addAll(properties.keySet());
        return retval;
    }

    private void init(ConfigurationService configService) {
        initSettings(configService);
        initStructuredObjects(configService);
        initMetadata(configService);
    }

    private void initSettings(final ConfigurationService config) {
        Properties propertiesInFolder = config.getPropertiesInFolder(SETTINGS);
        for (Entry<Object, Object> entry : propertiesInFolder.entrySet()) {
            String value = null != entry.getValue() ? String.valueOf(entry.getValue()) : null;
            properties.put(String.valueOf(entry.getKey()), new ServerProperty(value, METADATA_PROTECTED));
        }
    }

    private void initStructuredObjects(final ConfigurationService config) {
        Map<String, Object> yamlInFolder = config.getYamlInFolder(SETTINGS);
        for (Object yamlContent : yamlInFolder.values()) {
            if (yamlContent instanceof Map) {
                Map<String, Object> entries = (Map<String, Object>) yamlContent;
                for (Map.Entry<String, Object> entry : entries.entrySet()) {
                    String namespace = entry.getKey();
                    Object subkeys = entry.getValue();
                    recursivelyInitStructuredObjects(namespace, subkeys);
                }
            }
        }
    }

    private void recursivelyInitStructuredObjects(String namespace, Object subkeys) {
        if (subkeys instanceof Map) {
            Map<String, Object> entries = (Map<String, Object>) subkeys;
            for (Map.Entry<String, Object> entry : entries.entrySet()) {
                recursivelyInitStructuredObjects(namespace + '/' + entry.getKey(), entry.getValue());
            }
        } else {
            // We found a leaf
            Map<String, String> metadata = new HashMap<String, String>(2);
            metadata.put(PREFRENCE_PATH, namespace);
            metadata.put(PROTECTED, TRUE);
            properties.put(namespace, new ServerProperty(String.valueOf(subkeys), metadata));
        }
    }

    private void initMetadata(final ConfigurationService config) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConfigProviderService.class);
        final Map<String, Object> yamlInFolder = config.getYamlInFolder(META);
        for (final Object o : yamlInFolder.values()) {
            if (false == checkMap(o, logger)) {
                continue;
            }
            @SuppressWarnings("unchecked") Map<String, Object> metadataDef = (Map<String, Object>) o;
            for (final Map.Entry<String, Object> entry : metadataDef.entrySet()) {
                final String propertyName = entry.getKey();
                final Object value2 = entry.getValue();
                if (false == checkMap(value2, logger)) {
                    continue;
                }

                @SuppressWarnings("unchecked") Map<String, Object> metadata = (Map<String, Object>) value2;
                Map<String, String> propertyMetadata = new HashMap<String, String>(metadata.size());
                for (Map.Entry<String, Object> metadataProp : metadata.entrySet()) {
                    if (metadataProp.getValue() != null) {
                        propertyMetadata.put(metadataProp.getKey(), String.valueOf(metadataProp.getValue()));
                    }
                }
                String value = propertyMetadata.get(VALUE);
                if (value == null) {
                    value = config.getProperty(propertyName);
                }
                properties.put(propertyName, new ServerProperty(value, propertyMetadata));
            }
        }
    }

    private boolean checkMap(Object o, org.slf4j.Logger logger) {
        if (!Map.class.isInstance(o)) {
            List<Object> args = new ArrayList<Object>();
            StringBuilder b = new StringBuilder("One of the .yml files in the meta configuration directory is improperly formatted{}");
            args.add(Strings.getLineSeparator());
            b.append("Please make sure they are formatted in this fashion:{}");
            args.add(Strings.getLineSeparator());
            b.append("ui/somepath:{}");
            args.add(Strings.getLineSeparator());
            b.append("\tprotected: false{}{}");
            args.add(Strings.getLineSeparator());
            args.add(Strings.getLineSeparator());
            b.append("ui/someOtherpath:{}");
            args.add(Strings.getLineSeparator());
            b.append("\tprotected: false{}{}");
            args.add(Strings.getLineSeparator());
            args.add(Strings.getLineSeparator());
            args.add(new IllegalArgumentException("Invalid .yml file"));
            logger.error(b.toString(), args.toArray(new Object[args.size()]));
            return false;
        }
        return true;
    }

    /**
     * Re-initializes this configuration provider.
     *
     * @throws OXException If operation fails
     */
    @Override
    public void reinit() throws OXException {
        properties.clear();
        init(this.configService);
    }

}
