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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.realtime.hazelcast.directory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.hazelcast.core.Member;
import com.openexchange.realtime.exception.RealtimeException;
import com.openexchange.realtime.packet.ID;
import com.openexchange.realtime.packet.Presence;
import com.openexchange.realtime.packet.Presence.Type;
import com.openexchange.realtime.packet.PresenceState;

/**
 * {@link HazelcastResourceWrapper} - Helper class to map/unmap the HazelcastResource to a java.* based map that can be deserialized by
 * hazelcast.
 *
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 */
public class HazelcastResourceWrapper {

    private final static String resource_timestamp = "resource_timestamp";

    private final static String presence_from = "presence_from";

    private final static String presence_type = "presence_type";

    private final static String presence_message = "presence_message";

    private final static String presence_state = "presence_status";

    private final static String presence_priority = "presence_priority";

    private final static String routingInfo = "routingInfo";

    public final static String eviction_timestamp = "eviction_timestamp";

    /**
     * Map an instance of HazelcastResource to a Map from String to Serializable.
     *
     * @param hazelcastResource th resource to wrap
     * @return the wrapping map
     * @throws RealtimeException if wrapping the HazelcastResource fails
     */
    public static Map<String, Object> wrap(HazelcastResource hazelcastResource) {
        if(hazelcastResource == null) {
            return null;
        }
        HashMap<String, Object> wrap = new HashMap<String, Object>();

        Presence presence = hazelcastResource.getPresence();
        if (presence != null) {
            wrapPresence(wrap, presence);
        }

        wrap.put(routingInfo, hazelcastResource.getRoutingInfo());

        Date timestamp = hazelcastResource.getTimestamp();
        wrap.put(resource_timestamp, timestamp);

        //update to prevent eviction
        wrap.put(eviction_timestamp, System.currentTimeMillis());

        return wrap;
    }

    private static HashMap<String, Object> wrapPresence(HashMap<String, Object> wrap, Presence presence) {

        ID from = presence.getFrom();
        wrap.put(presence_from, from.toString());

        Type type = presence.getType();
        wrap.put(presence_type, type.name());

        String message = presence.getMessage();
        wrap.put(presence_message, message);

        PresenceState state = presence.getState();
        wrap.put(presence_state, state.name());

        Byte priority = presence.getPriority();
        wrap.put(presence_priority, priority);

        return wrap;
    }

    /**
     * Unwrap a previously wrapped HazelcastResource.
     *
     * @param map the wrapped HazelcastResource
     * @return the unwrapped HazelcastResource POJO, or null if map was null
     */
    public static HazelcastResource unwrap(Map<String, Object> map) {
        if(map == null) {
            return null;
        }
        Presence presence = null;
        Member member = null;
        Date timestamp = null;

        if(map.get(presence_from) != null) {
            presence = unwrapPresence(map);
        }

        member = (Member) map.get(routingInfo);
        timestamp = (Date) map.get(resource_timestamp);

        return new HazelcastResource(presence, timestamp, member);
    }

    private static Presence unwrapPresence(Map<String, Object> map) {

        if(map.get(presence_from) == null) {
            //the resource didn't have a presence
            return null;
        }

        String fromString = (String) map.get(presence_from);
        ID from = new ID(fromString);

        String typeString = (String) map.get(presence_type);
        Presence.Type type = getType(typeString);

        String message = (String) map.get(presence_message);

        String stateString = (String) map.get(presence_state);
        PresenceState state = getState(stateString);

        Byte priority = (Byte) map.get(presence_priority);

        Presence presence = Presence.builder()
            .from(from)
            .type(type)
            .state(state)
            .message(message)
            .priority(priority)
            .build();

        return presence;

    }

    private static Presence.Type getType(String type) {
        for (Presence.Type t : Presence.Type.values()) {
            if (t.name().equalsIgnoreCase(type)) {
                return t;
            }
        }
        return Type.NONE;
    }

    private static PresenceState getState(String state) {
        for (PresenceState ps : PresenceState.values()) {
            if (ps.name().equalsIgnoreCase(state)) {
                return ps;
            }
        }
        return PresenceState.ONLINE;
    }

}
