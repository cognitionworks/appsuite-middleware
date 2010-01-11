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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.ajp13;

import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.ServletException;
import com.openexchange.ajp13.exception.AJPv13Exception;

/**
 * {@link AJPv13Request} - Abstract super class for AJP requests.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class AJPv13Request {

    /**
     * Max size of an incoming request body:<br>
     * 8192 (8K) - 4 bytes (0x12 + 0x34 + data length integer).
     * 
     * @value 8188
     */
    protected final static int MAX_REQUEST_BODY_CHUNK_SIZE = 8188;

    private final byte[] payloadData;

    private int pos;

    /**
     * Initializes a new {@link AJPv13Request}.
     * 
     * @param payloadData The payload data
     */
    protected AJPv13Request(final byte[] payloadData) {
        this.payloadData = payloadData;
        pos = 0;
    }

    /**
     * Process the AJP request.
     * 
     * @param ajpRequestHandler The AJP request handler providing session data
     * @throws AJPv13Exception If an AJP error occurs
     * @throws IOException If an I/O error occurs
     */
    public abstract void processRequest(AJPv13RequestHandler ajpRequestHandler) throws AJPv13Exception, IOException;

    /**
     * Writes AJP response package.
     * 
     * @param ajpRequestHandler The AJP request handler providing session data
     * @throws AJPv13Exception If an AJP error occurs
     * @throws ServletException If a servlet error occurs
     * @throws IOException If an I/O error occurs
     */
    public void response(final AJPv13RequestHandler ajpRequestHandler) throws AJPv13Exception, ServletException, IOException {
        final AJPv13Connection ajpConnection = ajpRequestHandler.getAJPConnection();
        if (!ajpRequestHandler.isServiceMethodCalled()) {
            /*
             * Ensure completeness in case of form data
             */
            if (ajpRequestHandler.isFormData()) {
                if (!ajpRequestHandler.isAllDataRead()) {
                    final OutputStream ajpOut = ajpConnection.getOutputStream();
                    do {
                        ajpOut.write(AJPv13Response.getGetBodyChunkBytes(ajpRequestHandler.getNumOfBytesToRequestFor()));
                        ajpOut.flush();
                        /*
                         * Trigger request handler to process expected incoming data package
                         */
                        ajpRequestHandler.processPackage();
                    } while (!ajpRequestHandler.isAllDataRead());
                }
                /*
                 * Turn form's post data into request parameters
                 */
                ajpRequestHandler.doParseQueryString(ajpRequestHandler.peekData());
            }
            /*
             * Call servlet's service() method which will then request all receivable data chunks from client through servlet input stream
             */
            ajpRequestHandler.doServletService();
        }
        final OutputStream out = ajpConnection.getOutputStream();
        /*
         * Send response headers first.
         */
        ajpRequestHandler.doWriteHeaders(out);
        /*
         * Get remaining data
         */
        byte[] remainingData = null;
        try {
            remainingData = ajpRequestHandler.getAndClearResponseData();
        } catch (final IOException e) {
            remainingData = new byte[0];
        }
        if (remainingData.length > 0) {
            /*
             * Send rest of data cut into MAX_BODY_CHUNK_SIZE pieces
             */
            writeChunked(remainingData, out);
        }
        /*-
         * 
        else {
            writeEmpty(out);
        }
         */
        /*
         * Write END_RESPONSE package
         */
        writeResponse(AJPv13Response.getEndResponseBytes(), out, true);
        ajpRequestHandler.setEndResponseSent();
    }

    /**
     * Writes specified data chunked as SEND_BODY packages.
     * 
     * @param data The data to write
     * @param out The AJP connection's output stream to write to
     * @throws IOException If an I/O error occurs
     * @throws AJPv13Exception If an AJP error occurs
     */
    public static void writeChunked(final byte[] data, final OutputStream out) throws IOException, AJPv13Exception {
        int offset = 0;
        final int maxLen = AJPv13Response.MAX_SEND_BODY_CHUNK_SIZE;
        while (offset < data.length) {
            final int b = (data.length - offset);
            final int curLen = ((maxLen <= b) ? maxLen : b);
            out.write(AJPv13Response.getSendBodyChunkBytes(data, offset, curLen));
            out.flush();
            offset += curLen;
        }
    }

    private static final byte[] EMPTY = new byte[0];

    /**
     * Writes an empty SEND_BODY package.
     * 
     * @param out The AJP connection's output stream to write to
     * @throws IOException If an I/O error occurs
     * @throws AJPv13Exception If an AJP error occurs
     */
    public static void writeEmpty(final OutputStream out) throws IOException, AJPv13Exception {
        out.write(AJPv13Response.getSendBodyChunkBytes(EMPTY));
        out.flush();
    }

    /**
     * Writes specified bytes into given output stream.
     * 
     * @param responseBytes The bytes to write
     * @param out The output stream to write to
     * @param flushStream Whether to flush the output stream
     * @throws IOException If an I/O error occurs
     */
    protected static final void writeResponse(final byte[] responseBytes, final OutputStream out, final boolean flushStream) throws IOException {
        out.write(responseBytes);
        if (flushStream) {
            out.flush();
        }
    }

    /**
     * Writes specified AJP response into given output stream.
     * 
     * @param response The AJP response
     * @param out The output stream to write to
     * @param flushStream Whether to flush the output stream
     * @throws IOException If an I/O error occurs
     * @throws AJPv13Exception If an AJP error occurs
     */
    protected static final void writeResponse(final AJPv13Response response, final OutputStream out, final boolean flushStream) throws IOException, AJPv13Exception {
        writeResponse(response.getResponseBytes(), out, flushStream);
    }

    /**
     * Checks if payload data is <code>null</code>.
     * 
     * @return <code>true</code> if payload data is <code>null</code>; otherwise <code>false</code>
     */
    protected final boolean isPayloadNull() {
        return payloadData == null;
    }

    /**
     * Gets the payload data length.
     * 
     * @return The payload data length
     */
    protected final int getPayloadLength() {
        return payloadData.length;
    }

    /**
     * Parses the next <code>int</code> value (which consumes next two bytes).
     * 
     * @return The next <code>int</code> value
     */
    protected final int parseInt() {
        return ((payloadData[pos++] & 0xff) << 8) + (payloadData[pos++] & 0xff);
    }

    /**
     * Gets the next bytes (which consumes next <code>numOfBytes</code> bytes).
     * 
     * @param numOfBytes The number of bytes to return
     * @return The next bytes
     */
    protected final byte[] getByteSequence(final int numOfBytes) {
        final byte[] retval = new byte[numOfBytes];
        final int available = payloadData.length - pos;
        System.arraycopy(payloadData, pos, retval, 0, numOfBytes > available ? available : numOfBytes);
        pos += numOfBytes;
        return retval;
    }

    /**
     * Gets the next <i>unsigned</i> byte.
     * 
     * @return The next <i>unsigned</i> byte
     */
    protected final int nextByte() {
        return (payloadData[pos++] & 0xff);
    }

    /**
     * Compares next available unsigned byte with given value.
     * 
     * @param compareTo The value to compare to
     * @return <code>true</code> if next available unsigned byte is equal to given value; otherwise <code>false</code>
     */
    protected final boolean compareNextByte(final int compareTo) {
        if (hasNext()) {
            return ((payloadData[pos] & 0xff) == compareTo);
        }
        return false;
    }

    /**
     * Checks if there's another byte available.
     * 
     * @return <code>true</code> if there's another byte available; otherwise <code>false</code>
     */
    protected final boolean hasNext() {
        return (pos < payloadData.length);
    }

}
