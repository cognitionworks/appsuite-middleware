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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.realtime.json.impl;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.openexchange.realtime.json.protocol.RTClientState;
import com.openexchange.realtime.json.protocol.StanzaTransmitter;
import com.openexchange.realtime.packet.ID;
import com.openexchange.realtime.packet.IDEventHandler;

/**
 * The {@link StateManager} manages the state of connected clients.
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class StateManager {

    private final ConcurrentHashMap<ID, RTClientState> states = new ConcurrentHashMap<ID, RTClientState>();

    private final ConcurrentHashMap<ID, StanzaTransmitter> transmitters = new ConcurrentHashMap<ID, StanzaTransmitter>();

    /**
     * Retrieves stored {@link RTClientState} or creates a new entry for the given id.
     * @param id The id
     * @return The stored or new {@link RTClientState} for the given id
     */
    public StateEntry retrieveState(ID id) {
        RTClientState state = states.get(id);
        boolean created = false;

        if (state == null) {
            state = new RTClientStateImpl(id);
            RTClientState meantime = states.putIfAbsent(id, state);
            created = meantime == null;
            state = (created) ? state : meantime;
            if (created) {
                id.on(ID.Events.DISPOSE, new IDEventHandler() {

                    @Override
                    public void handle(String event, ID id, Object source, Map<String, Object> properties) {
                        states.remove(id);
                        transmitters.remove(id);
                    }
                });
            }
        }
        StanzaTransmitter transmitter = transmitters.get(id);

        return new StateEntry(state, transmitter, created);
    }

    /**
     * Associate a {@linkStanza Transmitter} with an {@link ID}
     * 
     * @param id The ID
     * @param transmitter The transmitter that can be used to send messages to the associated ID
     */
    public void rememberTransmitter(ID id, StanzaTransmitter transmitter) {
        transmitters.put(id, transmitter);
    }

    /**
     * Remove a {@linkStanza Transmitter} <-> {@link ID} association
     * 
     * @param id The ID
     * @param transmitter The transmitter
     */
    public void forgetTransmitter(ID id, StanzaTransmitter transmitter) {
        transmitters.remove(id, transmitter);
    }

    /**
     * Times out states that haven't been touched in more than thirty minutes
     * 
     * @param timestamp - The timestamp to compare the lastSeen value to
     */
    public void timeOutStaleStates(long timestamp) {
        for (RTClientState state : new ArrayList<RTClientState>(states.values())) {
            if (state.isTimedOut(timestamp)) {
                state.getId().trigger(ID.Events.DISPOSE, this);
            }
        }
    }

    /**
     * Checks if we already have a state associated with this client 
     * 
     * @param id the {@link ID} representing the client
     * @return true if we already have a state associated with this client
     */
    public boolean isConnected(ID id) {
        return states.containsKey(id);
    }

}
