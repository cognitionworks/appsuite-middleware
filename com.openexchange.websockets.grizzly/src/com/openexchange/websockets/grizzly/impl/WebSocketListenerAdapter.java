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
 *    trademarks of the OX Software GmbH. group of companies.
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

package com.openexchange.websockets.grizzly.impl;

import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.HandshakeException;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketException;
import com.openexchange.websockets.IndividualWebSocketListener;
import com.openexchange.websockets.WebSocketConnectException;
import com.openexchange.websockets.WebSocketListener;
import com.openexchange.websockets.WebSocketRuntimeException;

/**
 * {@link WebSocketListenerAdapter}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public class WebSocketListenerAdapter implements org.glassfish.grizzly.websockets.WebSocketListener {

    /**
     * Creates a new adapter for given Web Socket listener.
     *
     * @param webSocketListener The Web Socket listener
     * @return The adapter instance
     */
    public static WebSocketListenerAdapter newAdapterFor(WebSocketListener webSocketListener) {
        if (webSocketListener instanceof IndividualWebSocketListener) {
            // Have an individual instance per Web Socket
            return new IndividualWebSocketListenerAdapter((IndividualWebSocketListener) webSocketListener);
        }

        // A common instance shared among Web Sockets
        return new WebSocketListenerAdapter(webSocketListener);
    }

    // ------------------------------------------------------------------

    protected final WebSocketListener webSocketListener;
    protected final IndividualWebSocketListenerAdapter individualWebSocketListenerAdapter;

    /**
     * Initializes a new {@link WebSocketListenerAdapter}.
     */
    protected WebSocketListenerAdapter(WebSocketListener webSocketListener) {
        this(webSocketListener, null);
    }

    /**
     * Initializes a new {@link WebSocketListenerAdapter}.
     * @param newInstance
     * @param individualWebSocketListenerAdapter
     */
    protected WebSocketListenerAdapter(WebSocketListener webSocketListener, IndividualWebSocketListenerAdapter individualWebSocketListenerAdapter) {
        super();
        this.webSocketListener = webSocketListener;
        this.individualWebSocketListenerAdapter = individualWebSocketListenerAdapter;
    }

    /**
     * Gets whether this adapter instance was created by an {@link IndividualWebSocketListenerAdapter}.
     *
     * @return <code>true</code> if so, <code>false</code> if it is a non-individual instance
     */
    public boolean isIndividualInstance() {
        return individualWebSocketListenerAdapter != null;
    }

    /**
     * Gets the {@link IndividualWebSocketListenerAdapter} that created this instance, if applicable.
     *
     * @return The individual adapter or <code>null</code>
     */
    public IndividualWebSocketListenerAdapter getIndividualAdapter() {
        return individualWebSocketListenerAdapter;
    }

    @Override
    public int hashCode() {
        return 31 * 1 + ((webSocketListener == null) ? 0 : webSocketListener.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WebSocketListenerAdapter)) {
            return false;
        }
        WebSocketListenerAdapter other = (WebSocketListenerAdapter) obj;
        if (webSocketListener == null) {
            if (other.webSocketListener != null) {
                return false;
            }
        } else if (!webSocketListener.equals(other.webSocketListener)) {
            return false;
        }
        return true;
    }

    @Override
    public void onClose(WebSocket socket, DataFrame frame) {
        if (socket instanceof DefaultSessionBoundWebSocket) {
            webSocketListener.onWebSocketClose((DefaultSessionBoundWebSocket) socket);
        }
    }

    @Override
    public void onConnect(WebSocket socket) {
        if (socket instanceof DefaultSessionBoundWebSocket) {
            try {
                webSocketListener.onWebSocketConnect((DefaultSessionBoundWebSocket) socket);
            } catch (WebSocketConnectException e) {
                HandshakeException hsEx = new HandshakeException(e.getCode(), e.getMessage());
                hsEx.initCause(e);
                throw hsEx;
            } catch (WebSocketRuntimeException e) {
                throw new WebSocketException(e);
            }
        }
    }

    @Override
    public void onMessage(WebSocket socket, String text) {
        if (socket instanceof DefaultSessionBoundWebSocket) {
            webSocketListener.onMessage((DefaultSessionBoundWebSocket) socket, text);
        }
    }

    @Override
    public void onMessage(WebSocket socket, byte[] bytes) {
        // Unused by now
    }

    @Override
    public void onPing(WebSocket socket, byte[] bytes) {
        // Unused by now
    }

    @Override
    public void onPong(WebSocket socket, byte[] bytes) {
        // Unused by now
    }

    @Override
    public void onFragment(WebSocket socket, String fragment, boolean last) {
        // Unused by now
    }

    @Override
    public void onFragment(WebSocket socket, byte[] fragment, boolean last) {
        // Unused by now

    }

}
