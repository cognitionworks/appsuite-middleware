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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.ajp13.stable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import javax.servlet.ServletException;
import com.openexchange.ajp13.AJPv13Connection;
import com.openexchange.ajp13.AJPv13RequestHandler;
import com.openexchange.ajp13.AJPv13Response;
import com.openexchange.ajp13.BlockableBufferedInputStream;
import com.openexchange.ajp13.BlockableBufferedOutputStream;
import com.openexchange.ajp13.exception.AJPv13Exception;
import com.openexchange.ajp13.exception.AJPv13InvalidConnectionStateException;

/**
 * {@link AJPv13ConnectionImpl} - Represents an AJP connection which mainly delegates processing of incoming AJP data packages to an
 * assigned AJP request handler.
 * <p>
 * Moreover it keeps track of package numbers.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
final class AJPv13ConnectionImpl implements AJPv13Connection {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(AJPv13ConnectionImpl.class);

    private int state;

    private int packageNumber;

    private BlockableBufferedInputStream inputStream;

    private BlockableBufferedOutputStream outputStream;

    private AJPv13Listener listener;

    private AJPv13RequestHandlerImpl ajpRequestHandler;

    /**
     * Initializes a new {@link AJPv13ConnectionImpl}
     * 
     * @param listener The AJP listener providing client socket
     */
    AJPv13ConnectionImpl(final AJPv13Listener listener) {
        this();
        setAndApplyListener(listener);
    }

    /**
     * Initializes a new {@link AJPv13ConnectionImpl}
     */
    AJPv13ConnectionImpl() {
        super();
        state = IDLE_STATE;
        packageNumber = 0;
    }

    /**
     * Resets this connection instance and prepares it for next upcoming AJP cycle. That is associated request handler will be set to
     * <code>null</code>, its state is set to <code>IDLE</code> and the output stream is going to be flushed.
     * 
     * @param releaseRequestHandler
     */
    void resetConnection(final boolean releaseRequestHandler) {
        if (state == IDLE_STATE) {
            return;
        }
        if (ajpRequestHandler != null) {
            resetRequestHandler(releaseRequestHandler);
        }
        if (outputStream != null) {
            try {
                outputStream.flush();
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        state = IDLE_STATE;
        packageNumber = 0;
    }

    private void resetRequestHandler(final boolean release) {
        /*
         * Discard request handler's reference to this connection if it ought to be released from it
         */
        ajpRequestHandler.reset(release);
        if (release) {
            if (AJPv13RequestHandlerPool.isInitialized()) {
                /*
                 * Put reseted request handler back into pool
                 */
                AJPv13RequestHandlerPool.putRequestHandler(ajpRequestHandler);
            }
            /*
             * Discard request handler reference
             */
            ajpRequestHandler = null;
        }
    }

    /**
     * Waits for and processes incoming AJP package through delegating to associated request handler.
     * <p>
     * Moreover this connection's state is switched to <tt>ASSIGNED</tt> if it's still <tt>IDLE</tt>.
     * 
     * @throws IOException If AJP socket is closed
     * @throws AJPv13Exception If an AJP error occurs
     */
    void processRequest() throws IOException, AJPv13Exception {
        if (listener.getSocket().isClosed()) {
            throw new IOException("Socket is closed");
        }
        if (state == IDLE_STATE) {
            state = ASSIGNED_STATE;
            if (ajpRequestHandler == null) {
                /*
                 * Fetch or create a request handler to this newly assigned connection
                 */
                if (AJPv13RequestHandlerPool.isInitialized()) {
                    ajpRequestHandler = AJPv13RequestHandlerPool.getRequestHandler(this);
                } else {
                    ajpRequestHandler = new AJPv13RequestHandlerImpl();
                    ajpRequestHandler.setAJPConnection(this);
                }
            }
        }
        ajpRequestHandler.processPackage();
    }

    /**
     * Creates the AJP response data to previously received AJP package through delegating to request handler.
     * 
     * @throws AJPv13Exception If an AJP error occurs while creating response data or this connection is not in <tt>ASSIGNED</tt> state
     * @throws ServletException If a servlet error occurs
     */
    void createResponse() throws AJPv13Exception, ServletException {
        if (state != ASSIGNED_STATE) {
            throw new AJPv13InvalidConnectionStateException();
        }
        ajpRequestHandler.createResponse();
    }

    /**
     * Gets the associated AJP request handler which processes the AJP data sent over this connection
     * 
     * @return The associated AJP request handler.
     */
    public AJPv13RequestHandler getAjpRequestHandler() {
        return ajpRequestHandler;
    }

    /**
     * Gets the input stream from AJP client
     * 
     * @return The input stream from AJP client
     * @throws IOException If input stream cannot be returned
     */
    public InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            throw new IOException("Input stream not availalbe");
        }
        return inputStream;
    }

    /**
     * Gets the output stream to AJP client
     * 
     * @return The output stream to AJP client
     * @throws IOException If output stream cannot be returned
     */
    public OutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            throw new IOException("Output stream not available");
        }
        return outputStream;
    }

    /**
     * Gets the neutral output stream to AJP client
     * 
     * @return The neutral output stream to AJP client
     *  @throws IOException If neutral output stream cannot be returned
     */
    public OutputStream getNeutralOutputStream() throws IOException {
        if (outputStream == null) {
            throw new IOException("Output stream not available");
        }
        return outputStream.getNeutralOutputStream();
    }

    /**
     * Sets the SO_TIMEOUT with the specified timeout, in milliseconds.
     * 
     * @param millis The timeout in milliseconds
     * @throws AJPv13Exception If there is an error in the underlying protocol, such as a TCP error.
     */
    public void setSoTimeout(final int millis) throws AJPv13Exception {
        try {
            listener.getSocket().setSoTimeout(millis);
        } catch (final SocketException e) {
            throw new AJPv13Exception(AJPv13Exception.AJPCode.IO_ERROR, false, e, e.getMessage());
        }
    }

    /**
     * Gets the number of actual AJP package.
     * 
     * @return The number of actual AJP package.
     */
    public int getPackageNumber() {
        return packageNumber;
    }

    /**
     * Increments package number by one.
     */
    void incrementPackageNumber() {
        packageNumber++;
    }

    /**
     * Gets the current AJP connection's state
     * 
     * @return Current AJP connection's state
     */
    public int getState() {
        return state;
    }

    /**
     * Removes connection's listener reference and therefore implicitly invokes the <code>resetConnection(true)</code> method
     */
    void removeListener() {
        /*
         * Reset connection
         */
        resetConnection(true);
        /*
         * Remove underlying input/output stream
         */
        discardStreams();
        /*
         * Remove associated listener
         */
        listener = null;
    }

    /**
     * Sets both input and output stream to <code>null</code> and closes associated socket.
     */
    public void close() {
        discardStreams();
        listener.discardSocket();
    }

    /**
     * Sets both input and output stream to <code>null</code>
     */
    private void discardStreams() {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (final IOException e) {
                LOG.debug("Output stream could not be closed", e);
            }
            outputStream = null;
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (final IOException e) {
                LOG.debug("Input stream could not be closed", e);
            }
            inputStream = null;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("State: ").append(state == IDLE_STATE ? "IDLE" : "ASSIGNED").append(" | ");
        sb.append("Listener: ").append(listener.getListenerName()).append(" | ");
        return sb.toString();
    }

    boolean isAjpListenerNull() {
        return (listener.getSocket() == null);
    }

    /**
     * Marks corresponding AJP listener as processing
     */
    void markListenerProcessing() {
        listener.markProcessing();
    }

    /**
     * Marks corresponding AJP listener as non-processing
     */
    void markListenerNonProcessing() {
        listener.markNonProcessing();
    }

    /**
     * Increments number of AJP tasks waiting for incoming AJP data.
     */
    void incrementWaiting() {
        listener.incrementWaiting();
    }

    /**
     * Decrements number of AJP tasks waiting for incoming AJP data.
     */
    void decrementWaiting() {
        listener.decrementWaiting();
    }

    /**
     * Applies an AJP listener to this AJP connection
     * 
     * @param listener The AJP listener
     * @return This AJP connection with specified listener applied
     */
    AJPv13ConnectionImpl setListener(final AJPv13Listener listener) {
        setAndApplyListener(listener);
        return this;
    }

    private void setAndApplyListener(final AJPv13Listener listener) {
        this.listener = listener;
        try {
            final Socket client = listener.getSocket();
            inputStream = new BlockableBufferedInputStream(client.getInputStream(), true);
            outputStream = new BlockableBufferedOutputStream(client.getOutputStream(), AJPv13Response.MAX_SEND_BODY_CHUNK_SIZE, true);
        } catch (final IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void blockInputStream(final boolean block) {
        if (block) {
            inputStream.block();
        } else {
            inputStream.unblock();
        }
    }

    public void blockOutputStream(final boolean block) {
        if (block) {
            outputStream.block();
        } else {
            outputStream.unblock();
        }
    }
}
