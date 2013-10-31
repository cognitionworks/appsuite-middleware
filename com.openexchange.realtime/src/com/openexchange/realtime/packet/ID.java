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

package com.openexchange.realtime.packet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.realtime.packet.IDComponentsParser.IDComponents;
import com.openexchange.realtime.util.IdLookup;
import com.openexchange.realtime.util.IdLookup.UserAndContext;
import com.openexchange.sessiond.impl.SessionObject;
import com.openexchange.sessiond.impl.SessionObjectWrapper;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * An ID describes a valid sender or recipient of a {@link Stanza}. 
 * 
 * An ID has the following form: [protocol].[component]://[user]@[context]/[resource]:
 * <ol>
 *   <li><b>protocol</b>: specifies the protocol that the entity behind this ID uses to connect to the OX.</li>
 *   <li><b>component</b> (optional): corresponds to a specific backend service that registered itself for this component name</li>
 *   <li><b>user</b>: specifies the name of the entity we want to address</li>
 *   <li><b>context</b>: specifies the context of the entity we want to address</li>
 *   <li><b>resource</b>: is an unique identifier used to distinguish between multiple instances/connections of the same entitiy</li>
 * </ol>
 * 
 * <h4>Examples:</h4>
 * <ol>
 *   <li><b>ox://francisco.laguna@premium/20d39asd9da93249f009d</b>: we want to address the user francisco.laguna in the context premium.
 *       The user is connected via the ox channel that is used between the browser and the backend and has the identifier
 *       20d39asd9da93249f009d (tab/window/browser)</li>
 *   <li><b>synthetic.office://operations@premium/66499.62446</b>: The "synthetic" protocol declares that the entity is synthetic and has no
 *       counterpart in the real world (a bot, a room, general programm construct) instead of a user. The backendservice addressed with this
 *       ID is office. The entity addressed is operations@premium and the resource identifies document via folder.document notation</li>
 *   <li><b>call://356c4ad6a4af46948f9703217a1f5a2d@internal</b>:</li>
 * </ol>

 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a> JavaDoc
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 */
public class ID implements Serializable {

    private static final long serialVersionUID = -5237507998711320109L;

    private static final ConcurrentHashMap<ID, ConcurrentHashMap<String, List<IDEventHandler>>> LISTENERS = new ConcurrentHashMap<ID, ConcurrentHashMap<String, List<IDEventHandler>>>();

    private static final ConcurrentHashMap<ID, ConcurrentHashMap<String, Lock>> LOCKS = new ConcurrentHashMap<ID, ConcurrentHashMap<String, Lock>>();

    private String protocol;
    private String component;
    private String user;
    private String context;
    private String resource;

    /**
     * Initializes a new {@link ID}.
     * 
     * @param id the given String representation of an ID
     * @param defaultContext the default context to use if the string representation doesn't contain one
     * @throws IllegalArgumentException if no ID could be created from the given String 
     */
    public ID(final String id, String defaultContext) {
        IDComponents idComponents = IDComponentsParser.parse(id);
        protocol = idComponents.protocol;
        component = idComponents.component;
        user = idComponents.user;
        context = idComponents.context;
        resource = idComponents.resource;

        if (context == null) {
            context = defaultContext;
        }

        sanitize();
        validate();

    }

    /**
     * Initializes a new {@link ID}.
     * 
     * @param id the given String representation of an ID
     * @throws IllegalArgumentException if no ID could be created from the given String
     */
    public ID(final String id) {
        IDComponents idComponents = IDComponentsParser.parse(id);
        protocol = idComponents.protocol;
        component = idComponents.component;
        user = idComponents.user;
        context = idComponents.context;
        resource = idComponents.resource;

        sanitize();
        validate();
    }

    /**
     * Initializes a new {@link ID} without a component.
     *
     * @param protocol The protocol of the ID, ox, xmpp ...
     * @param user The user represented by this ID
     * @param context The context of the user represented by this ID
     * @param resource The resource of the connected user eg. "desktop" or ontoher string identifying the connected client. Must be unique
     *            to enable multiple logins.
     */
    public ID(final String protocol, final String user, final String context, final String resource) {
        this(protocol, null, user, context, resource);
    }

    /**
     * Initializes a new {@link ID}.
     *
     * @param protocol The protocol of the ID, ox, xmpp ...
     * @param component The component of the id (to address Files and so on)
     * @param user The user represented by this ID
     * @param context The context of the user represented by this ID
     * @param resource The resource of the connected user eg. "desktop" or ontoher string identifying the connected client. Must be unique
     *            to enable multiple logins.
     */
    public ID(final String protocol, final String component, final String user, final String context, final String resource) {
        super();
        this.protocol = protocol;
        this.user = user;
        this.context = context;
        this.resource = resource;
        this.component = component;
        sanitize();
        validate();
    }

    /**
     * Check optional id components for emtpy strings and sanitize by setting to null or default values.
     */
    private void sanitize() {
        if (Strings.isEmpty(protocol)) {
            protocol = null;
        }

        if (Strings.isEmpty(resource)) {
            resource = null;
        }

        if (Strings.isEmpty(component)) {
                component = null;
        }

    }

    /*
     * Validate that mandatory id components exist.
     */
    private void validate() throws IllegalArgumentException {
        if (user == null) {
            throw new IllegalArgumentException("User information is obligatory for IDs");
        }

        if (context == null) {
            throw new IllegalArgumentException("Context information is obligatory for IDs");
        }
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
        validate();
    }

    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
        validate();

    }

    public String getContext() {
        return context;
    }

    public void setContext(final String context) {
        this.context = context;
        validate();
    }

    public String getResource() {
        return resource;
    }

    public void setResource(final String resource) {
        this.resource = resource;
        validate();
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
        result = prime * result + ((resource == null) ? 0 : resource.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        result = prime * result + ((component == null) ? 0 : component.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ID)) {
            return false;
        }
        final ID other = (ID) obj;
        if (context == null) {
            if (other.context != null) {
                return false;
            }
        } else if (!context.equals(other.context)) {
            return false;
        }
        if (protocol == null) {
            if (other.protocol != null) {
                return false;
            }
        } else if (!protocol.equals(other.protocol)) {
            return false;
        }
        if (resource == null) {
            if (other.resource != null) {
                return false;
            }
        } else if (!resource.equals(other.resource)) {
            return false;
        }
        if (user == null) {
            if (other.user != null) {
                return false;
            }
        } else if (!user.equals(other.user)) {
            return false;
        }
        if (component == null) {
            if (other.component != null) {
                return false;
            }
        } else if (!component.equals(other.component)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder(32);
        boolean needSep = false;
        if (protocol != null) {
            b.append(protocol);
            needSep = true;
        }
        if (component != null) {
            if(protocol != null) {
                b.append(".");
            }
            b.append(component);
            needSep = true;
        }
        if (needSep) {
            b.append("://");
        }
        b.append(user).append('@').append(context);
        if (resource != null) {
            b.append('/').append(resource);
        }
        return b.toString();
    }

    /**
     * Strip protocol and resource from this id so that it only contains user@context information.
     *
     * @return
     */
    public ID toGeneralForm() {
        return new ID(null, component, user, context, null);
    }

    /**
     * Gets a value indicating whether this ID is in general form or not, i.e. if it only contains the mandatory parts and no protocol or
     * concrete resource name.
     *
     * @return <code>true</code> if the ID is in general form, <code>false</code>, otherwise
     */
    public boolean isGeneralForm() {
        return null == protocol && null == resource;
    }

    public ServerSession toSession() throws OXException {
        UserAndContext userAndContextIDs = IdLookup.getUserAndContextIDs(this);
        if (userAndContextIDs != null) {
            return ServerSessionAdapter.valueOf(SessionObjectWrapper.createSessionObject(
                userAndContextIDs.getUserId(),
                userAndContextIDs.getContextId(),
                (resource != null) ? resource : "rt"));
        }
        SessionObject sessionObject = new SessionObject("anonymous");
        return ServerSessionAdapter.valueOf(sessionObject);
    }

    /**
     * Execute the event handler when the "event" happens
     */
    public void on(String event, IDEventHandler handler) {
        handlerList(event).add(handler);
    }

    /**
     * Execute the event handler when the event happens, but only once
     *
     * @param event
     * @param handler
     */
    public void one(String event, IDEventHandler handler) {
        on(event, new OneOf(handler));
    }

    /**
     * Remove the event handler
     */
    public void off(String event, IDEventHandler handler) {
        handlerList(event).remove(handler);
    }

    /**
     * Remove all event handlers for this ID
     */
    public void clearListeners() {
        LISTENERS.remove(this);
    }

    /**
     * Trigger an event on this ID, with the give properties
     */
    public void trigger(String event, Object source, Map<String, Object> properties) {
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        List<IDEventHandler> handlerList = new ArrayList<IDEventHandler>(handlerList(event));
        for (IDEventHandler handler : handlerList) {
            handler.handle(event, this, source, properties);
        }
        if (event.equals("dispose")) {
            clearListeners();
            clearLocks();
        }
    }

    /**
     * Trigger an event on this ID.
     */
    public void trigger(String event, Object source) {
        trigger(event, source, new HashMap<String, Object>());
    }


    private List<IDEventHandler> handlerList(String event) {
        ConcurrentHashMap<String, List<IDEventHandler>> events = LISTENERS.get(this);
        if (events == null) {
            events = new ConcurrentHashMap<String, List<IDEventHandler>>();
            LISTENERS.put(this, events);
        }

        List<IDEventHandler> list = events.get(event);

        if (list == null) {
            list = new CopyOnWriteArrayList<IDEventHandler>();
            events.put(event, list);
        }

        return list;

    }

    private class OneOf implements IDEventHandler {

        IDEventHandler delegate;

        public OneOf(IDEventHandler delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public void handle(String event, ID id, Object source, Map<String, Object> properties) {
            delegate.handle(event, id, source, properties);
            id.off(event, this);
        }
    }

    public void clearLocks() {
        LOCKS.remove(this);
    }

    public void lock(String scope) {
        getLock(scope).lock();
    }

    public void unlock(String scope) {
        getLock(scope).unlock();
    }

    public Lock getLock(String scope) {
        ConcurrentHashMap<String, Lock> locksPerId = LOCKS.get(this);
        if (locksPerId == null) {
            locksPerId = new ConcurrentHashMap<String, Lock>();
            ConcurrentHashMap<String, Lock> meantime = LOCKS.putIfAbsent(this, locksPerId);
            locksPerId = (meantime != null) ?  meantime : locksPerId;
        }

        Lock lock = locksPerId.get(scope);
        if (lock == null) {
            lock = new ReentrantLock();
            Lock l = locksPerId.putIfAbsent(scope, lock);
            lock = (l != null) ? l : lock;
        }

        return lock;
    }
    
    public void dispose(Object source, Map<String, Object> properties) {
        Map<String, Object> vetoProperties = new HashMap<String, Object>();
        this.trigger(Events.BEFOREDISPOSE, this, vetoProperties);
        Boolean veto = (Boolean) vetoProperties.get("veto");
        if (veto != null && !veto) {
            this.trigger(Events.DISPOSE, source, properties);
        }
    }

    /**
     * {@link Events} is a collection of event constants to be used with {@link ID#trigger(String, Object)} and
     * {@link ID#on(String, IDEventHandler)}
     * 
     * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
     */
    public static interface Events {

        /**
         * Triggered before dispose. Offers the chance to veto the disposal of the ID by adding a key named "veto" with a value of true to
         * the properties
         */
        public static final String BEFOREDISPOSE = "beforedispose";

        /**
         * This event is triggered, when an ID goes offline. You can use this to free up resources this ID uses, for example state
         * information associated with the ID. Use {@link ID#dispose(Object, Map)} to give EventHandlers a chance to veto the disposal of
         * the ID.
         */
        public static final String DISPOSE = "dispose";

        /**
         * This event is triggered to ensure that resources for this ID aren't freed up by eviction policies or sth. similar.
         */
        public static final String REFRESH = "refresh";

    }
}
