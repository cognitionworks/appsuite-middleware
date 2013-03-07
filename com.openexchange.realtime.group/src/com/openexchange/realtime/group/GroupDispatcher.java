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

package com.openexchange.realtime.group;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.openexchange.exception.OXException;
import com.openexchange.realtime.Component;
import com.openexchange.realtime.ComponentHandle;
import com.openexchange.realtime.Component.EvictionPolicy;
import com.openexchange.realtime.dispatch.MessageDispatcher;
import com.openexchange.realtime.packet.ID;
import com.openexchange.realtime.packet.IDEventHandler;
import com.openexchange.realtime.packet.Stanza;
import com.openexchange.realtime.payload.PayloadElement;
import com.openexchange.realtime.payload.PayloadTree;
import com.openexchange.realtime.util.ActionHandler;
import com.openexchange.server.ServiceLookup;


/**
 * A {@link GroupDispatcher} is a utility superclass for implmenting chat room like functionality. 
 * Clients can join and leave the chat room, when the last user has left, the room closes itself and calls {@link #onDispose()} for cleanup
 * Subclasses can send messages to participants in the room via the handy {@link #relayToAll(Stanza, ID...)} method. Subclasses may pass in an ActionHandler 
 * to make use of the introspection magic
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class GroupDispatcher implements ComponentHandle {
    
    public static ServiceLookup services = null;
    
    private List<ID> ids = new CopyOnWriteArrayList<ID>();
    private Map<ID, String> stamps = new ConcurrentHashMap<ID, String>();
    
    private ID id;
    
    private ActionHandler handler = null;
    
    /**
     * Initializes a new {@link GroupDispatcher}. 
     * @param id the ID of this group.
     */
    public GroupDispatcher(ID id) {
        this(id, null);
    }
    
    /**
     * 
     * Initializes a new {@link GroupDispatcher}.
     * @param id The id of the group 
     * @param handler An action handler for introspection
     */
    public GroupDispatcher(ID id, ActionHandler handler) {
        this.id = id;
        this.handler = handler;
    }
    
    /**
     * Implements the {@link ComponentHandle} standard method. If it receives a group command (like join or leave) it
     * is handled internally otherwise processing is delegated to {@link #processStanza(Stanza)}
     * @param stanza
     * @throws OXException
     */
    @Override
    public void process(Stanza stanza) throws OXException {
        if (handleGroupCommand(stanza)) {
            return;
        }
        processStanza(stanza);
    }
    
    /**
     * Can be overidden by subclasses to implement a custom handling of non group commands. Defaults to using the
     * ActionHandler to call methods or calls {@link #defaultAction(Stanza)} if no suitable method 
     * @param stanza
     * @throws OXException
     */
    protected void processStanza(Stanza stanza) throws OXException {
        if (handler == null || !handler.callMethod(this, stanza)) {
            defaultAction(stanza);
        }
    }



    private boolean handleGroupCommand(Stanza stanza) throws OXException {
        PayloadElement payload = stanza.getPayload();
        if (payload == null) {
            return true;
        }
        
        Object data = payload.getData();
        if (GroupCommand.class.isInstance(data)) {
            ((GroupCommand) data).perform(stanza, this);
            return true;
        }
        
        return false;
    }
    
    /**
     * Send a copy of the stanza to all members of this group, excluding the ones provided as the rest of the arguments.
     */
    public void relayToAll(Stanza stanza, ID...excluded) throws OXException {
        MessageDispatcher dispatcher = services.getService(MessageDispatcher.class);
        Set<ID> ex = new HashSet<ID>(Arrays.asList(excluded));
        for(ID id: ids) {
            if (!ex.contains(id)) {
                // Send a copy of the stanza
                Stanza copy = copyFor(stanza, id);
                stamp(copy);
                dispatcher.send(copy);
            }
        }
    }
    
    /**
     * Relay this message to all except the original sender ("from") of the stanza.
     */
    public void relayToAllExceptSender(Stanza stanza) throws OXException {
        stamp(stanza);
        relayToAll(stanza, stanza.getFrom());
    }
    
    /**
     * Deliver this stanza to its recipient. Delegates to the {@link MessageDispatcher}
     */
    public void send(Stanza stanza) throws OXException {
        stamp(stanza);
        MessageDispatcher dispatcher = services.getService(MessageDispatcher.class);
        
        dispatcher.send(stanza);
    }
    
    
    /**
     * Add a member to this group. Can be invoked by sending the following message to this groups address. 
     * 
     * {
     *      element: "message", 
     *      selector: "mygroupSelector",
     *      to: "synthetic.componentName://roomID", 
     *      session: "da86ae8fc93340d389c51a1d92d6e997"
     *      payloads: [
     *          {
     *              namespace: 'group',
     *               element: 'command',
     *               data: 'join'
     *          }
     *       ], 
     *   }
     *
     * 
     * A selector provided in this stanza will be added to all stanzas sent by this group, so clients can know
     * the message was part of a given group.
     */
    public void join(ID id, String stamp) throws OXException {
        if (ids.contains(id)) {
            return;
        }
        
        beforeJoin(id);
        
        if (!mayJoin(id)) {
            return;
        }
        boolean first = ids.isEmpty();
        
        ids.add(id);
        stamps.put(id, stamp);
        id.on("dispose", LEAVE);
        firstJoined(id);
        onJoin(id);
    }
    
    /**
     * Leave the group by sending this stanza:
     * {
     *      element: "message", 
     *      to: "synthetic.componentName://roomID", 
     *      session: "da86ae8fc93340d389c51a1d92d6e997"
     *      payloads: [
     *          {
     *              namespace: 'group',
     *               element: 'command',
     *               data: 'leave'
     *          }
     *       ], 
     *   }
     */
    public void leave(ID id) throws OXException {
        beforeLeave(id);
        id.off("dispose", LEAVE);
        ids.remove(id);
        stamps.remove(id);
        if (ids.isEmpty()) {
            onDispose();
            id.trigger("dispose", this);
        }
        onLeave(id);
    }
    
    /**
     * Gets the selector with which this id joined
     */
    public String getStamp(ID id) {
        return stamps.get(id);
    }
    
    /**
     * Stamp a stanza with the selector for this recipient
     */
    public void stamp(Stanza s) {
        s.setSelector(getStamp(s.getTo()));
    }
    
    
    /**
     * Get a list of all members of this group
     */
    public List<ID> getIds() {
        return ids;
    }
    
    /**
     * Get the id of this group
     */
    public ID getId() {
        return id;
    }
    
    /**
     * Determine whether an ID is a member of this group. Useful if you want
     * to only accept messages for IDs that are members.
   */
    protected boolean isMember(ID id) {
        return ids.contains(id);
    }
    
    /**
     * Makes a copy of this stanza for a recipient. May be overridden.
     */
    protected Stanza copyFor(Stanza stanza, ID to) throws OXException {
        Stanza copy = stanza.newInstance();
        copy.setTo(to);
        copy.setFrom(stanza.getFrom());
        copyPayload(stanza, copy);
        
        return copy;
    }
    
    /**
     * Makes a copy of the payload in the stanza and puts it into the copy
     */
    protected void copyPayload(Stanza stanza, Stanza copy) throws OXException {
        List<PayloadTree> copyList = new ArrayList<PayloadTree>(stanza.getPayloads().size());
        for(PayloadTree tree: stanza.getPayloads()) {
            copyList.add(tree.internalClone());
        }
        copy.setPayloads(copyList);
    }
    
    /**
     * Subclasses can override this method to determine whether a potential participant is allowed to join this group.
     * @param id The id to check the permission for. 
     * @return true, if the participant may join this group, false otherwise
     * @see ID#toSession()
     */
    protected boolean mayJoin(ID id) {
        return true;
    }
    
    /**
     * Callback that is called before an ID joins the group. Override this to be notified of a member about to join the group.
     */
    protected void beforeJoin(ID id) {
        
    }
    
    /**
     * Callback that is called after a new member has joined the group. 
     */
    protected void onJoin(ID id) {
        
    }
    
    /**
     * Callback for when the first used joined
     */
    protected void firstJoined(ID id) {
        
    }
    
    /**
     * Callback that is called before a member leaves the group
     */
    protected void beforeLeave(ID id) {
        
    }
    
    /**
     * Callback that is called after a member left the group
     */
    protected void onLeave(ID id) {
        
    }
    
    /**
     * Called when the group is closed. This happens when the last member left the group, or the {@link EvictionPolicy} of the 
     * {@link Component} that created this group decides it is time to close the group
     */
    protected void onDispose() {
        
    }
    
    /**
     * Called for a stanza if no other handler is found.
     */
    protected void defaultAction(Stanza stanza) {
        
    }

    
    private IDEventHandler LEAVE = new IDEventHandler() {
        
        @Override
        public void handle(String event, ID id, Object source, Map<String, Object> properties) {
            try {
                leave(id);
            } catch (OXException e) {
            }
        }
    };

}
