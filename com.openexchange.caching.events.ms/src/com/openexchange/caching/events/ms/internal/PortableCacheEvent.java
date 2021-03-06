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

package com.openexchange.caching.events.ms.internal;

import static com.openexchange.caching.events.ms.internal.MsCacheEventHandler.getTopicName;
import static com.openexchange.tools.arrays.Collections.put;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;
import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;
import com.openexchange.caching.events.CacheEvent;
import com.openexchange.caching.events.CacheOperation;
import com.openexchange.hazelcast.serialization.AbstractCustomPortable;

/**
 * {@link PortableCacheEvent}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class PortableCacheEvent extends AbstractCustomPortable {

    /** The unique portable class ID of the {@link PortableCacheEvent} */
    public static final int CLASS_ID = 4;

    /** The class definition for PortableCacheEvent */
    public static ClassDefinition CLASS_DEFINITION = new ClassDefinitionBuilder(FACTORY_ID, CLASS_ID)
        .addUTFField("r")
        .addUTFField("g")
        .addUTFField("o")
        .addBooleanField("hk")
        .addPortableArrayField("k", PortableCacheKey.CLASS_DEFINITION)
    .build();

    /**
     * Wraps the supplied cache event into a portable cache event.
     *
     * @param cacheEvent The cache event to wrap
     * @return The portable cache event
     */
    public static PortableCacheEvent wrap(CacheEvent cacheEvent) {
        if (null == cacheEvent) {
            return null;
        }
        return new PortableCacheEvent(cacheEvent.getRegion(), cacheEvent.getGroupName(), cacheEvent.getOperation().getId(), PortableCacheKey.wrap(cacheEvent.getKeys()));
    }

    /**
     * Wraps the supplied cache event into one or more portable cache event, ready to be distributed on the designated topic(s).
     *
     * @param cacheEvent The cache event to wrap
     * @param topicCount The number of available topics to select from when constructing the name of the destination topic
     * @return The portable cache events, mapped to the corresponding topic names
     */
    public static Map<String, PortableCacheEvent> wrap(CacheEvent cacheEvent, int topicCount) {
        List<Serializable> keys = cacheEvent.getKeys();
        if (null == keys || keys.isEmpty()) {
            return Collections.singletonMap(getTopicName(null, topicCount), wrap(cacheEvent));
        }
        if (1 == keys.size()) {
            return Collections.singletonMap(getTopicName(keys.get(0), topicCount), wrap(cacheEvent));
        }
        Map<String, List<PortableCacheKey>> keysPerTopic = new HashMap<String, List<PortableCacheKey>>(topicCount);
        for (Serializable key : keys) {
            put(keysPerTopic, getTopicName(key, topicCount), PortableCacheKey.wrap(key));
        }
        Map<String, PortableCacheEvent> eventsPerTopic = new HashMap<String, PortableCacheEvent>(keysPerTopic.size());
        for (Entry<String, List<PortableCacheKey>> entry : keysPerTopic.entrySet()) {
            PortableCacheKey[] portableKeys = entry.getValue().toArray(new PortableCacheKey[entry.getValue().size()]);
            eventsPerTopic.put(entry.getKey(), new PortableCacheEvent(cacheEvent.getRegion(), cacheEvent.getGroupName(), cacheEvent.getOperation().getId(), portableKeys));
        }
        return eventsPerTopic;
    }

    /**
     * Unwraps the cache event from the supplied portable cache event.
     *
     * @param portableEvent The portable cache event
     * @return The cache event
     */
    public static CacheEvent unwrap(PortableCacheEvent portableEvent) {
        return new CacheEvent(CacheOperation.cacheOperationFor(portableEvent.operationId), portableEvent.region,
            PortableCacheKey.unwrap(portableEvent.keys), portableEvent.groupName);
    }

    private PortableCacheKey[] keys;
    private boolean hasKeys;
    private String operationId;
    private String groupName;
    private String region;

    /**
     * Initializes a new {@link PortableCacheEvent}.
     */
    public PortableCacheEvent() {
        super();
    }

    /**
     * Initializes a new {@link PortableCacheEvent}.
     * 
     * @param region The cache region
     * @param groupName The cache group name
     * @param operationId The cache operation identifier
     * @param keys The keys of the affected cache entries
     */
    private PortableCacheEvent(String region, String groupName, String operationId, PortableCacheKey[] keys) {
        super();
        this.region = region;
        this.groupName = groupName;
        this.operationId = operationId;
        this.hasKeys = null != keys;
        this.keys = keys;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void writePortable(PortableWriter writer) throws IOException {
        writer.writeUTF("r", region);
        writer.writeUTF("g", groupName);
        writer.writeUTF("o", operationId);
        writer.writeBoolean("hk", hasKeys);
        if (hasKeys) {
            writer.writePortableArray("k", keys);
        }
    }

    @Override
    public void readPortable(PortableReader reader) throws IOException {
        region = reader.readUTF("r");
        groupName = reader.readUTF("g");
        operationId = reader.readUTF("o");
        hasKeys = reader.readBoolean("hk");
        if (hasKeys) {
            Portable[] portableKeys = reader.readPortableArray("k");
            if (null != portableKeys) {
                keys = new PortableCacheKey[portableKeys.length];
                for (int i = portableKeys.length; i-- > 0;) {
                    keys[i] = (PortableCacheKey)portableKeys[i];
                }
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
        result = prime * result + ((keys == null) ? 0 : Arrays.hashCode(keys));
        result = prime * result + ((operationId == null) ? 0 : operationId.hashCode());
        result = prime * result + ((region == null) ? 0 : region.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PortableCacheEvent)) {
            return false;
        }
        PortableCacheEvent other = (PortableCacheEvent) obj;
        if (operationId != other.operationId) {
            return false;
        }
        if (groupName == null) {
            if (other.groupName != null) {
                return false;
            }
        } else if (!groupName.equals(other.groupName)) {
            return false;
        }
        if (region == null) {
            if (other.region != null) {
                return false;
            }
        } else if (!region.equals(other.region)) {
            return false;
        }
        if (keys == null) {
            if (other.keys != null) {
                return false;
            }
        } else if (!Arrays.equals(keys, other.keys)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128).append("PortableCacheEvent [");
        sb.append("operation=").append(operationId);
        sb.append(", region=").append(region);
        sb.append(", groupName=").append(groupName);
        sb.append(", keys=").append(null == keys ? "null" : Arrays.toString(keys));
        sb.append(']');
        return sb.toString();
    }

}
