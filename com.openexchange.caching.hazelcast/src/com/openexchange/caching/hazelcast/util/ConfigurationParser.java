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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.caching.hazelcast.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.NearCacheConfig;
import com.openexchange.caching.hazelcast.HazelcastCacheService;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;
import com.openexchange.java.UnsynchronizedStringReader;

/**
 * {@link ConfigurationParser}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ConfigurationParser {

    private static final Log LOG = com.openexchange.log.Log.loggerFor(ConfigurationParser.class);

    private static final String NAME_PREFIX = HazelcastCacheService.NAME_PREFIX;

    /**
     * Initializes a new {@link ConfigurationParser}.
     */
    private ConfigurationParser() {
        super();
    }

    public static Collection<MapConfig> parseConfig(final String sConfig) throws IOException {
        if (null == sConfig) {
            return Collections.emptyList();
        }
        return parseConfig(new UnsynchronizedStringReader(sConfig));
    }

    public static Collection<MapConfig> parseConfig(final InputStream is) throws IOException {
        if (null == is) {
            return Collections.emptyList();
        }
        return parseConfig(new InputStreamReader(is, Charsets.ISO_8859_1));
    }

    private static final String defaultMapName = NAME_PREFIX + "default";

    private static final String PREFIX_DEFAULT = "jcs.default";

    private static final int PREFIX_DEFAULT_LENGTH = PREFIX_DEFAULT.length();

    private static final String PREFIX_REGION = "jcs.region.";

    private static final int PREFIX_REGION_LENGTH = PREFIX_REGION.length();

    public static Collection<MapConfig> parseConfig(final Reader r) throws IOException {
        if (null == r) {
            return Collections.emptyList();
        }
        final BufferedReader reader = r instanceof BufferedReader ? (BufferedReader) r : new BufferedReader(r);
        try {
            String line = reader.readLine();
            if (null == line) {
                return Collections.emptyList();
            }
            final Map<String, MapConfig> map = new HashMap<String, MapConfig>(8);
            final Map<String, Boolean> localOnly = new HashMap<String, Boolean>(8);
            do {
                final char c;
                if (!isEmpty(line) && '#' != (c = line.charAt(0)) && '!' != c) {
                    if (line.startsWith("jcs.default")) {
                        // First line of default region specification: Is auxiliary enabled?
                        final int next = PREFIX_DEFAULT_LENGTH;
                        final String auxiliary = next < line.length() ? line.substring(next) : null;
                        if (isEmpty(auxiliary)) {
                            localOnly.put(defaultMapName, Boolean.TRUE);
                        }
                        // Parse line
                        parseLine(line, defaultMapName, map);
                    } else if (line.startsWith(PREFIX_REGION)) {
                        final String name;
                        {
                            final int dotPos = line.indexOf('.', PREFIX_REGION_LENGTH+2);
                            if (dotPos < 0) {
                                final int equalPos = line.indexOf('=', PREFIX_REGION_LENGTH+2);
                                if (equalPos < 0) {
                                    // Huh...?
                                    name = null;
                                } else {
                                    name = NAME_PREFIX + line.substring(PREFIX_REGION_LENGTH+1, equalPos);
                                    // First line of a region specification: Is auxiliary enabled?
                                    final int next = equalPos + 1;
                                    final String auxiliary = next < line.length() ? line.substring(next) : null;
                                    if (isEmpty(auxiliary)) {
                                        localOnly.put(name, Boolean.TRUE);
                                    }
                                }
                            } else {
                                name = NAME_PREFIX + line.substring(PREFIX_REGION_LENGTH+1, dotPos);
                            }
                        }
                        if (null != name) {
                            parseLine(line, name, map);
                        }
                    }
                }
            } while ((line = reader.readLine()) != null);
            /*
             * Check parsed MapConfigs
             */
            final Collection<MapConfig> configs = map.values();
            for (final MapConfig mapConfig : configs) {
                final String evictionPolicy = mapConfig.getEvictionPolicy();
                if (null == evictionPolicy || "NONE".equals(evictionPolicy)) {
                    mapConfig.setEvictionPolicy("LRU");
                    final NearCacheConfig nearCacheConfig = mapConfig.getNearCacheConfig();
                    if (null != nearCacheConfig) {
                        nearCacheConfig.setEvictionPolicy("LRU");
                    }
                } else {
                    final NearCacheConfig nearCacheConfig = mapConfig.getNearCacheConfig();
                    if (null != nearCacheConfig) {
                        nearCacheConfig.setInvalidateOnChange(true);
                    }
                }
            }
            return configs;
        } finally {
            Streams.close(reader);
        }
    }

    private static void parseLine(final String line, final String name, final Map<String, MapConfig> map) {
        // Map max. size
        {
            final String tmp = parseValue(".cacheattributes.MaxObjects", line);
            if (null != tmp) {
                final MapConfig mapConfig = getMapConfigFor(name, map);
                MaxSizeConfig maxSizeConfig = mapConfig.getMaxSizeConfig();
                if (null == maxSizeConfig) {
                    maxSizeConfig = new MaxSizeConfig();
                    mapConfig.setMaxSizeConfig(maxSizeConfig);
                }
                maxSizeConfig.setMaxSizePolicy(MaxSizeConfig.POLICY_MAP_SIZE_PER_JVM);
                final int maxSize = Integer.parseInt(tmp.trim());
                maxSizeConfig.setSize(maxSize);

                NearCacheConfig nearCacheConfig = mapConfig.getNearCacheConfig();
                if (null == nearCacheConfig) {
                    nearCacheConfig = new NearCacheConfig();
                    mapConfig.setNearCacheConfig(nearCacheConfig);
                }
                nearCacheConfig.setMaxSize(maxSize);
                // Next line...
                return;
            }
        }
        // MemoryCacheName
        {
            final String tmp = parseValue(".cacheattributes.MemoryCacheName", line);
            if (null != tmp) {
                final MapConfig mapConfig = getMapConfigFor(name, map);
                if (tmp.endsWith("LRUMemoryCache")) {
                    mapConfig.setEvictionPolicy("LRU");

                    NearCacheConfig nearCacheConfig = mapConfig.getNearCacheConfig();
                    if (null == nearCacheConfig) {
                        nearCacheConfig = new NearCacheConfig();
                        mapConfig.setNearCacheConfig(nearCacheConfig);
                    }
                    nearCacheConfig.setEvictionPolicy("LRU");
                }
                // Next line...
                return;
            }
        }
        // .cacheattributes.MaxMemoryIdleTimeSeconds
        {
            final String tmp = parseValue(".cacheattributes.MaxMemoryIdleTimeSeconds", line);
            if (null != tmp) {
                final MapConfig mapConfig = getMapConfigFor(name, map);
                final int maxIdleSeconds = Integer.parseInt(tmp.trim());
                mapConfig.setMaxIdleSeconds(maxIdleSeconds);

                NearCacheConfig nearCacheConfig = mapConfig.getNearCacheConfig();
                if (null == nearCacheConfig) {
                    nearCacheConfig = new NearCacheConfig();
                    mapConfig.setNearCacheConfig(nearCacheConfig);
                }
                nearCacheConfig.setMaxIdleSeconds(maxIdleSeconds);
                // Next line...
                return;
            }
        }
        // .elementattributes.MaxLifeSeconds
        {
            final String tmp = parseValue(".elementattributes.MaxLifeSeconds", line);
            if (null != tmp) {
                final MapConfig mapConfig = getMapConfigFor(name, map);
                final int ttl = Integer.parseInt(tmp.trim());
                mapConfig.setTimeToLiveSeconds(ttl);

                NearCacheConfig nearCacheConfig = mapConfig.getNearCacheConfig();
                if (null == nearCacheConfig) {
                    nearCacheConfig = new NearCacheConfig();
                    mapConfig.setNearCacheConfig(nearCacheConfig);
                }
                nearCacheConfig.setTimeToLiveSeconds(ttl);
                // Next line...
                return;
            }
        }
        // .elementattributes.IdleTime
        {
            final String tmp = parseValue(".elementattributes.IdleTime", line);
            if (null != tmp) {
                final MapConfig mapConfig = getMapConfigFor(name, map);
                if (mapConfig.getMaxIdleSeconds() <= 0) {
                    final int maxIdleSeconds = Integer.parseInt(tmp.trim());
                    mapConfig.setMaxIdleSeconds(maxIdleSeconds);

                    NearCacheConfig nearCacheConfig = mapConfig.getNearCacheConfig();
                    if (null == nearCacheConfig) {
                        nearCacheConfig = new NearCacheConfig();
                        mapConfig.setNearCacheConfig(nearCacheConfig);
                    }
                    nearCacheConfig.setMaxIdleSeconds(maxIdleSeconds);
                }
                // Next line...
                return;
            }
        }
    }

    private static String parseValue(final String attr, final String line) {
        try {
            final int pos;
            if ((pos = line.indexOf(attr, 11)) > 0) {
                return line.substring(pos + attr.length() + 1); // Skip '='
            }
        } catch (final RuntimeException e) {
            LOG.warn("Parsing value for attribute '" + attr + "' failed: " + e.getMessage(), e);
        }
        return null;
    }

    private static MapConfig getMapConfigFor(final String name, final Map<String, MapConfig> map) {
        MapConfig tmp = map.get(name);
        if (null == tmp) {
            // Create new MapConfig instance
            tmp = new MapConfig();
            tmp.setName(name);
            tmp.setBackupCount(MapConfig.DEFAULT_BACKUP_COUNT);
            tmp.setEvictionPercentage(MapConfig.DEFAULT_EVICTION_PERCENTAGE);
            map.put(name, tmp);
        }
        return tmp;
    }

    private static boolean isEmpty(final String string) {
        if (null == string) {
            return true;
        }
        final int len = string.length();
        boolean isWhitespace = true;
        for (int i = 0; isWhitespace && i < len; i++) {
            isWhitespace = Character.isWhitespace(string.charAt(i));
        }
        return isWhitespace;
    }

}
