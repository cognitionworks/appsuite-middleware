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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.ajp13.najp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import com.openexchange.ajp13.AJPv13CPingRequest;
import com.openexchange.ajp13.AJPv13Config;
import com.openexchange.ajp13.AJPv13Connection;
import com.openexchange.ajp13.AJPv13ForwardRequest;
import com.openexchange.ajp13.AJPv13Request;
import com.openexchange.ajp13.AJPv13RequestBody;
import com.openexchange.ajp13.AJPv13RequestHandler;
import com.openexchange.ajp13.AJPv13Response;
import com.openexchange.ajp13.AJPv13ServletInputStream;
import com.openexchange.ajp13.AJPv13ServletOutputStream;
import com.openexchange.ajp13.AJPv13Utility;
import com.openexchange.ajp13.exception.AJPv13Exception;
import com.openexchange.ajp13.exception.AJPv13UnknownPrefixCodeException;
import com.openexchange.ajp13.exception.AJPv13Exception.AJPCode;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.tools.servlet.http.HttpErrorServlet;
import com.openexchange.tools.servlet.http.HttpServletManager;
import com.openexchange.tools.servlet.http.HttpServletRequestWrapper;
import com.openexchange.tools.servlet.http.HttpServletResponseWrapper;

/**
 * {@link AJPv13RequestHandlerImpl} - The AJP request handler processes incoming AJP packages dependent on their prefix code and/or
 * associated package number.
 * <p>
 * Whenever an AJP connection delegates processing to a request handler, it waits on AJP connection's input stream for incoming data. The
 * data is then processed and control is returned to AJP connection.
 * <p>
 * Sub-sequential AJP communication may be initiated through {@link AJPv13ServletInputStream} and {@link AJPv13ServletOutputStream} during
 * servlets' processing.
 * 
 * @see AJPv13ServletInputStream
 * @see AJPv13ServletOutputStream
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class AJPv13RequestHandlerImpl implements AJPv13RequestHandler {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(AJPv13RequestHandlerImpl.class);

    private HttpServlet servlet;

    private final StringBuilder servletId;

    private HttpServletRequestWrapper request;

    private HttpServletResponseWrapper response;

    private AJPv13Request ajpRequest;

    private AJPv13ConnectionImpl ajpCon;

    private long contentLength;

    private boolean bContentLength;

    private long totalRequestedContentLength;

    private boolean headersSent;

    private boolean serviceMethodCalled;

    private boolean endResponseSent;

    private boolean isFormData;

    private String httpSessionId;

    private boolean httpSessionJoined;

    private String servletPath;

    private State state;

    private byte[] clonedForwardPackage;

    /**
     * Initializes a new {@link AJPv13RequestHandlerImpl}
     */
    AJPv13RequestHandlerImpl() {
        super();
        state = State.IDLE;
        servletId = new StringBuilder(16);
    }

    /**
     * Processes an incoming AJP package from web server. If first package of an AJP cycle is processed its prefix code determines further
     * handling. Any subsequent packages are treated as data-only packages.
     * 
     * @throws AJPv13Exception If package processing fails
     */
    public void processPackage() throws AJPv13Exception {
        try {
            if (State.IDLE.equals(state)) {
                state = State.ASSIGNED;
            }
            ajpCon.incrementPackageNumber();
            final boolean firstPackage = (ajpCon.getPackageNumber() == 1);
            final int dataLength = ajpCon.readInitialBytes(firstPackage && AJPv13Config.getAJPListenerReadTimeout() > 0);
            /*
             * Check if we received the first package which must contain a prefix code
             */
            if (firstPackage) {
                /*
                 * Read Prefix Code from Input Stream
                 */
                final int prefixCode = ajpCon.getInputStream().read();
                if (FORWARD_REQUEST_PREFIX_CODE == prefixCode) {
                    /*
                     * Special handling for a forward request
                     */
                    handleForwardRequest(dataLength);
                    return;
                }
                if (SHUTDOWN_PREFIX_CODE == prefixCode) {
                    LOG.error("AJPv13 Shutdown command NOT supported");
                    return;
                }
                if (PING_PREFIX_CODE == prefixCode) {
                    LOG.error("AJPv13 Ping command NOT supported");
                    return;
                }
                if (CPING_PREFIX_CODE == prefixCode) {
                    ajpRequest = new AJPv13CPingRequest(ajpCon.getPayloadData(dataLength - 1, true));
                } else {
                    /*
                     * Unknown prefix code in first package: Leave routine.
                     */
                    if (LOG.isWarnEnabled()) {
                        final AJPv13Exception ajpExc = new AJPv13UnknownPrefixCodeException(prefixCode);
                        LOG.warn(ajpExc.getMessage(), ajpExc);
                        /*
                         * Dump package
                         */
                        final byte[] payload = ajpCon.getPayloadData(dataLength - 1, true);
                        final byte[] clonedPackage = new byte[payload.length + 5];
                        clonedPackage[0] = 0x12;
                        clonedPackage[1] = 0x34;
                        clonedPackage[2] = (byte) (dataLength >> 8);
                        clonedPackage[3] = (byte) (dataLength & (255));
                        clonedPackage[4] = (byte) prefixCode;
                        System.arraycopy(payload, 0, clonedPackage, 5, payload.length);
                        LOG.warn("Corresponding AJP package:\n" + AJPv13Utility.dumpBytes(clonedPackage));
                    }
                    return;
                }
            } else {
                /*
                 * Any following packages after package #1 have to be a request body package which does not contain a prefix code
                 */
                ajpRequest = new AJPv13RequestBody(ajpCon.getPayloadData(dataLength, true));
            }
            ajpRequest.processRequest(this);
        } catch (final IOException e) {
            throw new AJPv13Exception(AJPCode.IO_ERROR, false, e, e.getMessage());
        }
    }

    private void handleForwardRequest(final int dataLength) throws IOException, AJPv13Exception {
        if (AJPv13Config.isLogForwardRequest()) {
            /*
             * Clone bytes from forward request
             */
            final byte[] payload = ajpCon.getPayloadData(dataLength - 1, true);
            clonedForwardPackage = new byte[payload.length + 5];
            clonedForwardPackage[0] = 0x12;
            clonedForwardPackage[1] = 0x34;
            clonedForwardPackage[2] = (byte) (dataLength >> 8);
            clonedForwardPackage[3] = (byte) (dataLength & (255));
            clonedForwardPackage[4] = FORWARD_REQUEST_PREFIX_CODE;
            System.arraycopy(payload, 0, clonedForwardPackage, 5, payload.length);
            /*
             * Create forward request with payload data
             */
            ajpRequest = new AJPv13ForwardRequest(payload);
        } else {
            ajpRequest = new AJPv13ForwardRequest(ajpCon.getPayloadData(dataLength - 1, true));
        }
        /*
         * Process forward request
         */
        ajpRequest.processRequest(this);
        /*
         * Check for possible long-running EAS request
         */
        ajpCon.setLongRunning(("/Microsoft-Server-ActiveSync".equals(request.getRequestURI()) && "Ping".equals(request.getParameter("Cmd"))));
        /*
         * Handle the important Content-Length header which controls further processing
         */
        if (contentLength == NOT_SET) {
            /*
             * This condition is reached when no content-length header was present in forward request package (transfer-encoding: chunked)
             */
            request.setData(new byte[0]);
        } else if (contentLength == 0) {
            /*
             * This condition is reached when content-length header's value is set to '0'
             */
            request.setData(null);
        } else {
            /*
             * Forward request is immediately followed by a data package
             */
            ajpCon.incrementPackageNumber();
            /*
             * We got a following request body package.
             */
            final int bodyRequestDataLength = ajpCon.readInitialBytes(false);
            ajpRequest = new AJPv13RequestBody(ajpCon.getPayloadData(bodyRequestDataLength, true));
            ajpRequest.processRequest(this);
        }
    }

    /**
     * Creates and writes the AJP response package corresponding to last received AJP package.
     * 
     * @throws AJPv13Exception If an AJP error occurs
     * @throws ServletException If processing the request fails
     */
    public void createResponse() throws AJPv13Exception, ServletException {
        try {
            if (ajpRequest == null) {
                /*
                 * We received an unsupported prefix code before, thus ajpRequest is null. Terminate AJP cycle.
                 */
                final OutputStream out = ajpCon.getOutputStream();
                out.write(AJPv13Response.getEndResponseBytes());
                out.flush();
                endResponseSent = true;
                return;
            }
            ajpRequest.response(this);
        } catch (final IOException e) {
            throw new AJPv13Exception(AJPCode.IO_ERROR, false, e, e.getMessage());
        }
    }

    /**
     * Gets the forward request's bytes as a formatted string or "&lt;not enabled&gt;" if not enabled via configuration.
     * 
     * @return The forward request's bytes as a formatted string
     */
    public String getForwardRequest() {
        return AJPv13Config.isLogForwardRequest() ? AJPv13Utility.dumpBytes(clonedForwardPackage) : "<not enabled>";
    }

    /**
     * Gets the AJP connection of this request handler.
     * 
     * @return The AJP connection of this request handler
     */
    public AJPv13Connection getAJPConnection() {
        return ajpCon;
    }

    /**
     * Sets the AJP connection of this request handler.
     * 
     * @param ajpCon The AJP connection
     */
    void setAJPConnection(final AJPv13ConnectionImpl ajpCon) {
        this.ajpCon = ajpCon;
    }

    private void releaseServlet() {
        if (servletId.length() > 0) {
            HttpServletManager.putServlet(servletId.toString(), servlet);
        }
        servletId.setLength(0);
        servlet = null;
    }

    /**
     * Releases associated servlet instance and resets request handler to hold initial values
     */
    public void reset(final boolean discardConnection) {
        if (state.equals(State.IDLE)) {
            return;
        }
        releaseServlet();
        try {
            if (request != null && request.getInputStream() != null) {
                request.getInputStream().close();
                request.removeInputStream();
            }
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
        }
        try {
            if (!endResponseSent && response != null && response.getServletOutputStream() != null) {
                response.getServletOutputStream().close();
                response.removeServletOutputStream();
            }
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
        }
        request = null;
        response = null;
        ajpRequest = null;
        contentLength = 0;
        bContentLength = false;
        totalRequestedContentLength = 0;
        headersSent = false;
        serviceMethodCalled = false;
        endResponseSent = false;
        isFormData = false;
        httpSessionId = null;
        httpSessionJoined = false;
        servletPath = null;
        state = State.IDLE;
        clonedForwardPackage = null;
        if (discardConnection) {
            ajpCon = null;
        }
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final String delim = " | ";
        final StringBuilder sb = new StringBuilder(300);
        sb.append("State: ").append(state.equals(State.IDLE) ? "IDLE" : "ASSIGNED").append(delim);
        sb.append("Servlet: ").append(servlet == null ? "null" : servlet.getClass().getName()).append(delim);
        sb.append("Current Request: ").append(ajpRequest.getClass().getName()).append(delim);
        sb.append("Content Length: ").append(bContentLength ? String.valueOf(contentLength) : "Not available").append(delim);
        sb.append("Servlet triggered: ").append(serviceMethodCalled).append(delim);
        sb.append("Headers sent: ").append(headersSent).append(delim);
        sb.append("End Response sent: ").append(endResponseSent).append(delim);
        return sb.toString();
    }

    /**
     * Sets this request hander's servlet reference to the one bound to given path argument
     * 
     * @param requestURI The request URI
     */
    public void setServletInstance(final String requestURI) {
        /*
         * Remove leading slash character
         */
        final String path = preparePath(requestURI);
        /*
         * Lookup path in available servlet paths
         */
        if (servletId.length() > 0) {
            servletId.setLength(0);
        }
        HttpServlet servletInst = HttpServletManager.getServlet(path, servletId);
        if (servletInst == null) {
            servletInst = new HttpErrorServlet("No servlet bound to path/alias: " + path);
        }
        servlet = servletInst;
        // servletId = pathStorage.length() > 0 ? pathStorage.toString() : null;
        if (servletId.length() > 0) {
            servletPath = servletId.toString().replaceFirst("\\*", ""); // path;
        }
        supplyRequestWrapperWithServlet();
    }

    private static String preparePath(final String path) {
        final int start = path.charAt(0) == '/' ? 1 : 0;
        final int len = path.length();
        final int end = path.charAt(len - 1) == '/' ? len - 1 : len;
        return path.substring(start, end);
    }

    /**
     * Triggers the servlet's service method to start processing the request and flushes the response to output stream.
     * <p>
     * This request handler is then marked to have the service() method called; meaning {@link #isServiceMethodCalled()} will return
     * <code>true</code>.
     * 
     * @throws IOException If an I/O error occurs
     * @throws ServletException If a servlet error occurs
     */
    public void doServletService() throws ServletException, IOException {
        servlet.service(request, response);
        doResponseFlush();
        serviceMethodCalled = true;
    }

    /**
     * Flushes the response to output stream
     * 
     * @throws IOException If an I/O error occurs
     */
    private void doResponseFlush() throws IOException {
        if (response != null) {
            response.flushBuffer();
            // response.getServletOutputStream().flush();
        }
    }

    /**
     * Writes the HTTP headers to specified output stream if not already written.
     * 
     * @param out The output stream
     * @throws AJPv13Exception If composing the <code>SEND_HEADERS</code> package fails
     * @throws IOException If an I/O error occurs
     */
    public void doWriteHeaders(final OutputStream out) throws AJPv13Exception, IOException {
        if (!headersSent) {
            out.write(AJPv13Response.getSendHeadersBytes(response));
            out.flush();
            response.setCommitted(true);
            headersSent = true;
        }
    }

    /**
     * Gets the response output stream's data and clears it
     * 
     * @return The response output stream's data
     * @throws IOException If an I/O error occurs
     */
    public byte[] getAndClearResponseData() throws IOException {
        if (null == response) {
            return new byte[0];
        }
        final byte[] retval = response.getServletOutputStream().getData();
        response.getServletOutputStream().clearByteBuffer();
        return retval;
    }

    /**
     * Sets/appends new data to servlet request's input stream
     * 
     * @param newData The new data to set
     * @throws IOException If an I/O error occurs
     */
    public void setData(final byte[] newData) throws IOException {
        request.setData(newData);
    }

    public byte[] peekData() throws IOException {
        return ((AJPv13ServletInputStream) request.getInputStream()).peekData();
    }

    /**
     * Parses given form's data into servlet request
     * 
     * @param contentBytes The content bytes representing a form's data
     * @throws UnsupportedEncodingException If encoding is not supported
     */
    public void doParseQueryString(final byte[] contentBytes) throws UnsupportedEncodingException {
        String charEnc = request.getCharacterEncoding();
        if (charEnc == null) {
            charEnc = ServerConfig.getProperty(ServerConfig.Property.DefaultEncoding);
        }
        AJPv13ForwardRequest.parseQueryString(request, new String(contentBytes, charEnc));
    }

    /**
     * Sets the servlet request
     * 
     * @param request The servlet request
     */
    public void setServletRequest(final HttpServletRequestWrapper request) {
        this.request = request;
        supplyRequestWrapperWithServlet();
    }

    /**
     * Sets the servlet response
     * 
     * @param response The servlet response
     */
    public void setServletResponse(final HttpServletResponseWrapper response) {
        this.response = response;
    }

    /**
     * Gets the content length
     * 
     * @return The content length
     */
    public long getContentLength() {
        return contentLength;
    }

    /**
     * @return <code>true</code> if content length has been set; otherwise <code>false</code>
     */
    public boolean containsContentLength() {
        return bContentLength;
    }

    /**
     * Sets the request's content length
     * 
     * @param contentLength The content length
     */
    public void setContentLength(final long contentLength) {
        this.contentLength = contentLength;
        bContentLength = true;
    }

    /**
     * Gets the total requested content length
     * 
     * @return The total requested content length
     */
    public long getTotalRequestedContentLength() {
        return totalRequestedContentLength;
    }

    /**
     * Increases the total requested content length by specified argument
     * 
     * @param increaseBy The value by which the total requested content length is increased
     */
    public void increaseTotalRequestedContentLength(final long increaseBy) {
        totalRequestedContentLength += increaseBy;
    }

    /**
     * Checks if the <code>service()</code> method has already been called
     * 
     * @return <code>true</code> if <code>service()</code> method has already been called; otherwise <code>false</code>
     */
    public boolean isServiceMethodCalled() {
        return serviceMethodCalled;
    }

    /**
     * Checks if AJP's end response package has been sent to web server
     * 
     * @return <code>true</code> if AJP's end response package has been sent to web server; otherwise <code>false</code>
     */
    public boolean isEndResponseSent() {
        return endResponseSent;
    }

    /**
     * Sets the end response flag
     */
    public void setEndResponseSent() {
        endResponseSent = true;
    }

    /**
     * Indicates if request content type equals <code>application/x-www-form-urlencoded</code>
     * 
     * @return <code>true</code> if request content type equals <code>application/x-www-form-urlencoded</code>; otherwise <code>false</code>
     */
    public boolean isFormData() {
        return isFormData;
    }

    /**
     * Marks that requests content type equals <code>application/x-www-form-urlencoded</code>
     * 
     * @param isFormData <code>true</code> if request content type equals <code>application/x-www-form-urlencoded</code>; otherwise
     *            <code>false</code>
     */
    public void setFormData(final boolean isFormData) {
        this.isFormData = isFormData;
    }

    /**
     * Gets the number of bytes that are left for being requested from web server
     * 
     * @return The number of bytes that are left for being requested
     */
    public int getNumOfBytesToRequestFor() {
        final long retval = contentLength - totalRequestedContentLength;
        if (retval > AJPv13Response.MAX_INT_VALUE || retval < 0) {
            return AJPv13Response.MAX_INT_VALUE;
        }
        return (int) retval;
    }

    /**
     * Checks if amount of received data is equal to value of header 'Content-Length'.
     * <p>
     * This method will always return false if content-length is not set unless method {@link #makeEqual()} is invoked
     * 
     * @return <code>true</code> if amount of received data is equal to value of header 'Content-Length'; otherwise <code>false</code>
     */
    public boolean isAllDataRead() {
        /*
         * This method will always return false if content-length is not set unless method makeEqual() is invoked
         */
        return (totalRequestedContentLength == contentLength);
    }

    /**
     * Indicates if servlet container still expects data from web server that is amount of received data is less than value of header
     * 'Content-Length'.
     * <p>
     * No empty data package received AND requested data length is still less than header 'Content-Length'.
     * 
     * @return <code>true</code> if servlet container still expects data from web server; otherwise <code>false</code>
     */
    public boolean isMoreDataExpected() {
        /*
         * No empty data package received AND requested data length is still less than header Content-Length
         */
        return (contentLength != NOT_SET && totalRequestedContentLength < contentLength);
    }

    /**
     * Checks if header 'Content-Length' has not been set
     * 
     * @return <code>true</code> if header 'Content-Length' has not been set; otherwise <code>false</code>
     */
    public boolean isNotSet() {
        return (contentLength == NOT_SET);
    }

    /**
     * Indicates if amount of received data exceeds value of header "content-length"
     * 
     * @return
     */
    public boolean isMoreDataReadThanExpected() {
        return (contentLength != NOT_SET && totalRequestedContentLength > contentLength);
    }

    /**
     * Total requested content length is made equal to header "content-length" which has the effect that no more data is going to be
     * requested from web server cause <code>AJPv13RequestHandler.isAllDataRead()</code> will return <code>true</code>.
     */
    public void makeEqual() {
        totalRequestedContentLength = contentLength;
    }

    /**
     * Gets the HTTP session ID
     * 
     * @return The HTTP session ID
     */
    public String getHttpSessionId() {
        return httpSessionId;
    }

    /**
     * Sets the HTTP session ID
     * 
     * @param httpSessionId The HTTP session ID
     * @param join <code>true</code> if the HTTP session has joined a previous HTTP session; otherwise <code>false</code>
     */
    public void setHttpSessionId(final String httpSessionId, final boolean join) {
        this.httpSessionId = httpSessionId;
        httpSessionJoined = join;
    }

    /**
     * Checks if the HTTP session has joined a previous HTTP session
     * 
     * @return <code>true</code> if the HTTP session has joined a previous HTTP session; otherwise <code>false</code>
     */
    public boolean isHttpSessionJoined() {
        return httpSessionJoined;
    }

    /**
     * Gets the servlet path (which is not the request path). The servlet path is defined in servlet mapping configuration.
     * 
     * @return The servlet path
     */
    public String getServletPath() {
        return servletPath;
    }

    private void supplyRequestWrapperWithServlet() {
        if (request != null && servlet != null) {
            request.setServletInstance(servlet);
        }
    }

}
