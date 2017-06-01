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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.util.Parameters;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.HandshakeException;
import org.glassfish.grizzly.websockets.ProtocolHandler;
import org.glassfish.grizzly.websockets.Utf8Utils;
import org.glassfish.grizzly.websockets.WebSocketException;
import org.glassfish.grizzly.websockets.WebSocketListener;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.openexchange.exception.OXException;
import com.openexchange.websockets.ConnectionId;
import com.openexchange.websockets.MessageTranscoder;
import com.openexchange.websockets.SendControl;
import com.openexchange.websockets.WebSocket;
import com.openexchange.websockets.WebSocketExceptionCodes;
import com.openexchange.websockets.WebSocketSession;
import com.openexchange.websockets.grizzly.SessionBoundWebSocket;

/**
 * {@link DefaultSessionBoundWebSocket} - The Web Socket bound to a certain session.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public class DefaultSessionBoundWebSocket extends SessionBoundWebSocket implements WebSocket {

    private final WebSocketSession webSocketSession;
    private final Parameters parameters;
    private volatile Map<String, String> paramsMap;
    private volatile MessageTranscoder transcoder;

    /**
     * Initializes a new {@link DefaultSessionBoundWebSocket}.
     */
    public DefaultSessionBoundWebSocket(ConnectionId connectionId, String path, Parameters parameters, ProtocolHandler protocolHandler, HttpRequestPacket request, WebSocketListener... listeners) {
        super(connectionId, path, protocolHandler, request, listeners);
        this.parameters = parameters;
        webSocketSession = new WebSocketSessionImpl();
    }

    /**
     * Gets the Web Socket session
     *
     * @return The Web Socket session
     */
    @Override
    public WebSocketSession getWebSocketSession() {
        return webSocketSession;
    }

    /**
     * Gets the available query parameters associated while this Web Socket was created; e.g. <code>"param1=foo&amp;param2=bar"</code>.
     *
     * @return The parameters
     */
    @Override
    public Map<String, String> getParameters() {
        Map<String, String> tmp = this.paramsMap;
        if (null == tmp) {
            // May be concurrently initialized
            Builder<String, String> builder = ImmutableMap.builder();
            for (String parameterName : parameters.getParameterNames()) {
                builder.put(parameterName, parameters.getParameter(parameterName));
            }
            tmp = builder.build();
            this.paramsMap = tmp;
        }
        return tmp;
    }

    /**
     * Gets the value for the denoted query parameter.
     *
     * @param parameterName The parameter name
     * @return The parameters value or <code>null</code> (if no such parameter was available while this Web Socket was created)
     */
    @Override
    public String getParameter(String parameterName) {
        return null == parameterName ? null : parameters.getParameter(parameterName);
    }

    /**
     * Applies a certain message transcoder to this Web Socket.
     * <p>
     * Every inbound and outbound messages are routed through that transcoder.
     *
     * @param transcoder The transcode to set
     */
    @Override
    public void setMessageTranscoder(MessageTranscoder transcoder) {
        this.transcoder = transcoder;
    }

    /**
     * Gets the scheme identifier for the currently active message transcoder.
     *
     * @return The scheme identifier or <code>null</code> if no trancoder is in place
     */
    @Override
    public String getMessageTranscoderScheme() {
        MessageTranscoder transcoder = this.transcoder;
        return null == transcoder ? null : transcoder.getId();
    }

    @Override
    public void onConnect() {
        try {
            super.onConnect();
        } catch (HandshakeException e) {
            throw e;
        } catch (Exception e) {
            HandshakeException hndshkExc = new HandshakeException(e.getMessage());
            hndshkExc.initCause(e);
            throw hndshkExc;
        }
    }

    // ---------------------------------------------------------- Outbound ---------------------------------------------------------------

    @Override
    public void sendMessage(String message) throws OXException {
        MessageTranscoder transcoder = this.transcoder;

        String transcoded = null == transcoder ? message : transcoder.onOutboundMessage(this, message);
        if (null != transcoded) {
            blockingSend(transcoded);
        }
    }

    @Override
    public void sendMessageRaw(String message) throws OXException {
        blockingSend(message);
    }

    private void blockingSend(String message) throws OXException {
        GrizzlyFuture<DataFrame> grizzlyFuture = send(Utf8Utils.encode(DataFrame.STRICT_UTF8_CHARSET, message));
        try {
            grizzlyFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw WebSocketExceptionCodes.UNEXPECTED_ERROR.create(e, "Interrupted");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof OXException) {
                throw (OXException) cause;
            }
            if (cause instanceof IOException) {
                throw WebSocketExceptionCodes.IO_ERROR.create(cause, cause.getMessage());
            }
            if (cause instanceof WebSocketException) {
                throw WebSocketExceptionCodes.PROTOCOL_ERROR.create(cause, cause.getMessage());
            }
            throw WebSocketExceptionCodes.UNEXPECTED_ERROR.create(cause, cause.getMessage());
        }
    }

    @Override
    public SendControl sendMessageAsync(String message) throws OXException {
        MessageTranscoder transcoder = this.transcoder;

        String transcoded = null == transcoder ? message : transcoder.onOutboundMessage(this, message);
        if (null == transcoded) {
            return new SendControlImpl<>(new GetFuture<Void>(null));
        }

        GrizzlyFuture<DataFrame> f = send(Utf8Utils.encode(DataFrame.STRICT_UTF8_CHARSET, transcoded));
        return new SendControlImpl<>(f);
    }

    // ---------------------------------------------------------- Inbound ----------------------------------------------------------------

    @Override
    public void onMessage(String text) {
        MessageTranscoder transcoder = this.transcoder;

        String transcoded = null == transcoder ? text : transcoder.onInboundMessage(this, text);
        if (null != transcoded) {
            super.onMessage(transcoded);
        }
    }

    @Override
    public void onMessage(byte[] data) {
        super.onMessage(data);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(128);
        builder.append("{");
        builder.append("userId=").append(getUserId());
        builder.append(", contextId=").append(getContextId());
        String sessionId = getSessionId();
        if (sessionId != null) {
            builder.append(", sessionId=").append(sessionId);
        }
        ConnectionId connectionId = getConnectionId();
        if (connectionId != null) {
            builder.append(", connectionId=").append(connectionId);
        }
        String path = getPath();
        if (path != null) {
            builder.append(", path=").append(path);
        }
        Map<String, String> parameters = getParameters();
        if (null != parameters) {
            builder.append(", parameters=").append(parameters);
        }
        if (webSocketSession != null) {
            builder.append(", webSocketSession=").append(webSocketSession);
        }
        builder.append("}");
        return builder.toString();
    }

    // -----------------------------------------------------------------------------------------------------------

    private static final class GetFuture<V> implements GrizzlyFuture<V> {

        private final V result;

        GetFuture(V result) {
            this.result = result;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return result;
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return result;
        }

        @Override
        public void recycle() {
            // Nothing
        }

        @Override
        public void markForRecycle(boolean recycleResult) {
            // Nothing
        }

        @Override
        public void recycle(boolean recycleResult) {
            // Nothing
        }

        @Override
        public void addCompletionHandler(CompletionHandler<V> completionHandler) {
            // Nothing
        }
    }

}
