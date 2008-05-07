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

package com.openexchange.ajp13;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.Arrays;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.openexchange.ajp13.AJPv13Exception.AJPCode;
import com.openexchange.monitoring.MonitoringInfo;
import com.openexchange.tools.servlet.OXServletInputStream;
import com.openexchange.tools.servlet.OXServletOutputStream;
import com.openexchange.tools.servlet.http.HttpErrorServlet;
import com.openexchange.tools.servlet.http.HttpServletManager;
import com.openexchange.tools.servlet.http.HttpServletRequestWrapper;
import com.openexchange.tools.servlet.http.HttpServletResponseWrapper;

/**
 * {@link AJPv13RequestHandler} - The AJP request handler processes incoming AJP
 * packages dependent on their prefix code and/or associated package number.
 * <p>
 * Whenever an AJP connection delegates processing to a request handler, it
 * waits on AJP connection's input stream for incoming data. The data is then
 * processed and control is returned to AJP connection.
 * <p>
 * Sub-sequential AJP communication may be initiated through
 * {@link OXServletInputStream} and {@link OXServletOutputStream} during
 * servlets' processing.
 * 
 * @see OXServletInputStream
 * @see OXServletOutputStream
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public final class AJPv13RequestHandler {

	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory
			.getLog(AJPv13RequestHandler.class);

	private static enum State {
		IDLE, ASSIGNED
	}

	/**
	 * Byte sequence indicating a packet from Web Server to Servlet Container.
	 */
	private static final int[] PACKAGE_FROM_SERVER_TO_CONTAINER = { 0x12, 0x34 };

	/**
	 * Starts the request handle cycle with following data.
	 * 
	 * @value 2
	 */
	private static final int FORWARD_REQUEST_PREFIX_CODE = 2;

	/**
	 * Web Server asks to shut down the Servlet Container.
	 * 
	 * @value 7
	 */
	private static final int SHUTDOWN_PREFIX_CODE = 7;

	/**
	 * Web Server asks the Servlet Container to take control (secure login
	 * phase).
	 * 
	 * @value 8
	 */
	private static final int PING_PREFIX_CODE = 8;

	/**
	 * Web Server asks the Servlet Container to respond quickly with a CPong.
	 * 
	 * @value 10
	 */
	private static final int CPING_PREFIX_CODE = 10;

	/**
	 * The value for a missing <i>Content-Length</i> header
	 * 
	 * @value -1
	 */
	static final int NOT_SET = -1;

	public static final String JSESSIONID_COOKIE = "JSESSIONID";

	public static final String JSESSIONID_URI = ";jsessionid=";

	private HttpServlet servlet;

	private final StringBuilder servletId;

	private int connectionType = -1;

	private HttpServletRequestWrapper request;

	private HttpServletResponseWrapper response;

	private AJPv13Request ajpRequest;

	private AJPv13Connection ajpCon;

	private int contentLength;

	private boolean bContentLength;

	private int totalRequestedContentLength;

	private boolean headersSent;

	private boolean serviceMethodCalled;

	private boolean endResponseSent;

	private boolean isFormData;

	private boolean emptyDataPackageReceived;

	private String httpSessionId;

	private boolean httpSessionJoined;

	private String servletPath;

	private State state;

	private byte[] clonedForwardPackage;

	public AJPv13RequestHandler() {
		super();
		state = State.IDLE;
		servletId = new StringBuilder(16);
	}

	/**
	 * Reads the (mandatory) first four bytes of an incoming AJPv13 package
	 * which indicate a package from Web Server to Servlet Container with its
	 * first two bytes and the payload data size in bytes in the following two
	 * bytes.
	 */
	private int readInitialBytes(final boolean enableTimeout) throws IOException, AJPv13Exception {
		int dataLength = -1;
		if (enableTimeout) {
			ajpCon.setSoTimeout(AJPv13Config.getAJPListenerReadTimeout());
		}
		/*
		 * Read a package from Web Server to Servlet Container.
		 */
		AJPv13Server.ajpv13ListenerMonitor.incrementNumWaiting();
		try {
			long start = 0L;
			final int[] magic;
			try {
				/*
				 * Read first two bytes
				 */
				ajpCon.markListenerNonProcessing();
				start = System.currentTimeMillis();
				magic = new int[] { ajpCon.getInputStream().read(), ajpCon.getInputStream().read() };
			} catch (final SocketException e) {
				throw new AJPv13SocketClosedException(AJPCode.SOCKET_CLOSED_BY_WEB_SERVER, e, Integer
						.valueOf(ajpCon == null ? 1 : ajpCon.getPackageNumber()), Long.valueOf((System
						.currentTimeMillis() - start)));
			}
			if (checkMagicBytes(magic)) {
				dataLength = (ajpCon.getInputStream().read() << 8) + ajpCon.getInputStream().read();
			} else if (magic[0] == -1 || magic[1] == -1) {
				throw new AJPv13SocketClosedException(AJPCode.EMPTY_INPUT_STREAM, null, Integer
						.valueOf(ajpCon == null ? 1 : ajpCon.getPackageNumber()), Long.valueOf(System
						.currentTimeMillis()
						- start));
			} else {
				throw new AJPv13InvalidByteSequenceException(Integer.valueOf(ajpCon.getPackageNumber()),
						toHexString(magic[0]), toHexString(magic[1]), dumpBytes((byte) magic[0], (byte) magic[1],
								getPayloadData(-1, ajpCon.getInputStream(), false)));
			}
			if (enableTimeout) {
				/*
				 * Set an infinite timeout
				 */
				ajpCon.setSoTimeout(0);
			}
			/*
			 * Initial bytes have been read, so processing (re-)starts now
			 */
			ajpCon.markListenerProcessing();
		} finally {
			AJPv13Server.ajpv13ListenerMonitor.decrementNumWaiting();
		}
		return dataLength;
	}

	private static boolean checkMagicBytes(final int[] magic) {
		if (AJPv13Config.getCheckMagicBytesStrict()) {
			return (magic[0] == PACKAGE_FROM_SERVER_TO_CONTAINER[0] && magic[1] == PACKAGE_FROM_SERVER_TO_CONTAINER[1]);
		}
		return (magic[0] == PACKAGE_FROM_SERVER_TO_CONTAINER[0] || magic[1] == PACKAGE_FROM_SERVER_TO_CONTAINER[1]);
	}

	private static String dumpBytes(final byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return "";
		}
		final String space = "    ";
		final StringBuilder sb = new StringBuilder(1024);
		int c = 0;
		int l = 0;
		for (int k = 0; k < bytes.length; k++) {
			if (c % 16 == 0) {
				sb.append('\r').append('\n');
				c = 0;
				final String hex = Integer.toHexString(l).toUpperCase();
				l += 16;
				final int nOZ = 4 - hex.length();
				for (int i = 0; i < nOZ; i++) {
					sb.append('0');
				}
				sb.append(hex).append(space);
			} else {
				sb.append(' ');
			}
			final String s = Integer.toHexString(bytes[k] & 0xff).toUpperCase();
			if (s.length() == 1) {
				sb.append('0');
			}
			sb.append(s);
			c++;
		}
		return sb.toString();
	}

	private static String dumpBytes(final byte magic1, final byte magic2, final byte[] bytes) {
		if (bytes == null) {
			return "";
		}
		final String space = "    ";
		final StringBuilder sb = new StringBuilder(1024);
		sb.append("0000").append(space).append(Integer.toHexString(magic1).toUpperCase()).append(' ').append(
				Integer.toHexString(magic2).toUpperCase());
		int c = 2;
		int l = 0;
		for (final byte b : bytes) {
			if (c == 16) {
				sb.append('\r').append('\n');
				c = 0;
				l += 16;
				final String hex = Integer.toHexString(l).toUpperCase();
				final int nOZ = 4 - hex.length();
				for (int i = 0; i < nOZ; i++) {
					sb.append('0');
				}
				sb.append(hex).append(space);
			} else {
				sb.append(' ');
			}
			final String s = Integer.toHexString(b & 0xff).toUpperCase();
			if (s.length() == 1) {
				sb.append('0');
			}
			sb.append(s);
			c++;
		}
		return sb.toString();
	}

	/**
	 * Processes an incoming AJP package from web server. If first package of an
	 * AJP cycle is processed its prefix code determines further handling. Any
	 * subsequent packages are treated as data-only packages.
	 * 
	 * @throws AJPv13Exception
	 *             If package processing fails
	 */
	public void processPackage() throws AJPv13Exception {
		try {
			if (state.equals(State.IDLE)) {
				state = State.ASSIGNED;
			}
			ajpCon.incrementPackageNumber();
			int dataLength = -1;
			final boolean firstPackage = (ajpCon.getPackageNumber() == 1);
			dataLength = readInitialBytes(firstPackage && AJPv13Config.getAJPListenerReadTimeout() > 0);
			/*
			 * We received the first package which must contain a prefix code
			 */
			int prefixCode = -1;
			if (firstPackage) {
				/*
				 * Read Prefix Code from Input Stream
				 */
				prefixCode = ajpCon.getInputStream().read();
				if (prefixCode == FORWARD_REQUEST_PREFIX_CODE) {
					if (AJPv13Config.isLogForwardRequest()) {
						/*
						 * Clone bytes from forward request
						 */
						final byte[] payload = getPayloadData(dataLength - 1, ajpCon.getInputStream(), true);
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
						ajpRequest = new AJPv13ForwardRequest(getPayloadData(dataLength - 1, ajpCon.getInputStream(),
								true));
					}
				} else if (prefixCode == SHUTDOWN_PREFIX_CODE) {
					LOG.error("AJPv13 Shutdown command NOT supported");
					return;
				} else if (prefixCode == PING_PREFIX_CODE) {
					LOG.error("AJPv13 Ping command NOT supported");
					return;
				} else if (prefixCode == CPING_PREFIX_CODE) {
					ajpRequest = new AJPv13CPingRequest(getPayloadData(dataLength - 1, ajpCon.getInputStream(), true));
				} else {
					/*
					 * Unknown prefix code in first package: Leave routine
					 */
					if (LOG.isWarnEnabled()) {
						final AJPv13Exception ajpExc = new AJPv13UnknownPrefixCodeException(prefixCode);
						LOG.warn(ajpExc.getMessage(), ajpExc);
					}
					return;
				}
			} else {
				/*
				 * Any following packages after package #1 have to be a request
				 * body package which does not deliver a prefix code
				 */
				ajpRequest = new AJPv13RequestBody(getPayloadData(dataLength, ajpCon.getInputStream(), true));
			}
			ajpRequest.processRequest(this);
			if (prefixCode == FORWARD_REQUEST_PREFIX_CODE) {
				handleContentLength();
			}
		} catch (final IOException e) {
			throw new AJPv13Exception(AJPCode.IO_ERROR, false, e, e.getMessage());
		}
	}

	private void handleContentLength() throws IOException, AJPv13Exception {
		int dataLength;
		if (contentLength == NOT_SET) {
			/*
			 * This condition is reached when no content-length header was
			 * present in forward request package (transfer-encoding: chunked)
			 */
			request.getOXInputStream().setData(new byte[0]);
		} else if (contentLength == 0) {
			/*
			 * This condition is reached when content-length header's value is
			 * set to '0'
			 */
			request.getOXInputStream().setData(null);
		} else {
			/*
			 * Forward request is immediately followed by a data package
			 */
			ajpCon.incrementPackageNumber();
			/*
			 * Processed package is an AJP forward request which indicates
			 * presence of a following request body package. Create a copy for
			 * logging on a possible error.
			 */
			dataLength = readInitialBytes(false);
			ajpRequest = new AJPv13RequestBody(getPayloadData(dataLength, ajpCon.getInputStream(), true));
			ajpRequest.processRequest(this);
		}
	}

	/**
	 * Creates and writes the corresponding AJP response package to the formerly
	 * received AJP package.
	 * 
	 * @throws AJPv13Exception
	 *             If an AJP-related error occurs
	 * @throws ServletException
	 *             If
	 *             {@link Servlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}
	 *             method invocation fails
	 */
	public void createResponse() throws AJPv13Exception, ServletException {
		try {
			if (ajpRequest == null) {
				/*
				 * We received an unsupported prefix code before, thus
				 * ajpRequest is null. Terminate ajp cycle
				 */
				ajpCon.getOutputStream().write(AJPv13Response.getEndResponseBytes());
				ajpCon.getOutputStream().flush();
				return;
			}
			ajpRequest.response(ajpCon.getOutputStream(), this);
		} catch (final IOException e) {
			throw new AJPv13Exception(AJPCode.IO_ERROR, false, e, e.getMessage());
		}
	}

	/**
	 * Gets the forward request's bytes as a formatted string or "&lt;not
	 * enabled&gt;" if not enabled via configuration
	 * 
	 * @return The forward request's bytes as a formatted string
	 */
	String getForwardRequest() {
		return AJPv13Config.isLogForwardRequest() ? dumpBytes(clonedForwardPackage) : "<not enabled>";
	}

	/**
	 * Gets the AJP connection of this request handler
	 * 
	 * @return The AJP connection of this request handler
	 */
	public AJPv13Connection getAJPConnection() {
		return ajpCon;
	}

	/**
	 * Sets the AJP connection of this request handler
	 * 
	 * @param ajpCon
	 *            The AJP connection
	 */
	public void setAJPConnection(final AJPv13Connection ajpCon) {
		this.ajpCon = ajpCon;
	}

	/**
	 * Reads a certain amount or all data from given <code>InputStream</code>
	 * instance dependent on boolean value of <code>strict</code>
	 * 
	 * @param payloadLength
	 * @param in
	 * @param strict
	 *            if <code>true</code> only <code>payloadLength</code> bytes
	 *            are read, otherwise all data is read
	 * @return
	 * @throws IOException
	 */
	private static byte[] getPayloadData(final int payloadLength, final InputStream in, final boolean strict)
			throws IOException {
		byte[] bytes = null;
		if (strict) {
			/*
			 * Read only payloadLength bytes
			 */
			bytes = new byte[payloadLength];
			int bytesRead = -1;
			int offset = 0;
			while ((bytesRead = in.read(bytes, offset, bytes.length - offset)) != -1 && offset < bytes.length) {
				offset += bytesRead;
			}
			if (offset < bytes.length) {
				Arrays.fill(bytes, offset, bytes.length, ((byte) -1));
				if (LOG.isWarnEnabled()) {
					LOG.warn(new StringBuilder().append("Incomplete payload data in AJP package: Should be ").append(
							payloadLength).append(" but was ").append(offset).toString(), new Throwable());
				}
			}
		} else {
			/*
			 * Read all available bytes
			 */
			int bytesRead = 0;
			int bytesAvailable = in.available();
			final byte[] fillMe = new byte[bytesAvailable];
			while (bytesRead != -1 && bytesAvailable > 0) {
				bytesRead = in.read(fillMe, 0, fillMe.length);
				bytesAvailable = in.available();
				if (bytes == null) {
					bytes = new byte[bytesRead];
					System.arraycopy(fillMe, 0, bytes, 0, bytesRead);
				} else {
					final byte[] tmp = bytes;
					bytes = new byte[tmp.length + bytesRead];
					System.arraycopy(tmp, 0, bytes, 0, tmp.length);
					System.arraycopy(fillMe, 0, bytes, tmp.length, bytesRead);
				}
			}
		}
		return bytes;
	}

	private void releaseServlet() {
		if (servletId.length() > 0) {
			HttpServletManager.putServlet(servletId.toString(), servlet);
			if (MonitoringInfo.UNKNOWN != connectionType) {
				MonitoringInfo.decrementNumberOfConnections(connectionType);
			}
		}
		servletId.setLength(0);
		servlet = null;
	}

	/**
	 * Releases associated servlet instance and resets request handler to hold
	 * initial values
	 */
	public void reset(final boolean discardConnection) {
		if (state.equals(State.IDLE)) {
			return;
		}
		releaseServlet();
		connectionType = -1;
		try {
			if (request != null && request.getInputStream() != null) {
				request.getInputStream().close();
				request.removeInputStream();
			}
		} catch (final Exception e) {
			LOG.error(e.getMessage(), e);
		}
		try {
			if (!endResponseSent && response != null && response.getOXOutputStream() != null) {
				response.getOXOutputStream().close();
				response.removeOXOutputStream();
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
		emptyDataPackageReceived = false;
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
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final String delim = " | ";
		final StringBuilder sb = new StringBuilder(300);
		sb.append("State: ").append(state.equals(State.IDLE) ? "IDLE" : "ASSIGNED").append(delim);
		sb.append("Servlet: ").append(servlet == null ? "null" : servlet.getClass().getName()).append(delim);
		sb.append("Current Request: ").append(ajpRequest.getClass().getName()).append(delim);
		sb.append("Content Length: ").append(bContentLength ? String.valueOf(contentLength) : "Not available").append(
				delim);
		sb.append("Servlet triggered: ").append(serviceMethodCalled).append(delim);
		sb.append("Headers sent: ").append(headersSent).append(delim);
		sb.append("End Response sent: ").append(endResponseSent).append(delim);
		return sb.toString();
	}

	/**
	 * Sets this request hander's servlet reference to the one bound to given
	 * path argument
	 * 
	 * @param pathArg
	 *            The request path
	 */
	public void setServletInstance(final String pathArg) {
		/*
		 * Remove leading slash character
		 */
		final String path = preparePath(pathArg);
		connectionType = MonitoringInfo.getConnectionType(path);
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
		if (MonitoringInfo.UNKNOWN != connectionType) {
			MonitoringInfo.incrementNumberOfConnections(connectionType);
		}
		supplyRequestWrapperWithServlet();
	}

	private static String preparePath(final String path) {
		final int start = path.charAt(0) == '/' ? 1 : 0;
		final int end = path.charAt(path.length() - 1) == '/' ? path.length() - 1 : path.length();
		return path.substring(start, end);
	}

	/**
	 * Gets the servlet reference
	 * 
	 * @return The servlet reference
	 */
	public HttpServlet getServlet() {
		return this.servlet;
	}

	/**
	 * Gets the servlet request
	 * 
	 * @return The servlet request
	 */
	public HttpServletRequestWrapper getServletRequest() {
		return request;
	}

	/**
	 * Sets the servlet request
	 * 
	 * @param request
	 *            The servlet request
	 */
	public void setServletRequest(final HttpServletRequestWrapper request) {
		this.request = request;
		supplyRequestWrapperWithServlet();
	}

	/**
	 * Gets the servlet response
	 * 
	 * @return The servlet response
	 */
	public HttpServletResponseWrapper getServletResponse() {
		return response;
	}

	/**
	 * Sets the servlet response
	 * 
	 * @param response
	 *            The servlet response
	 */
	public void setServletResponse(final HttpServletResponseWrapper response) {
		this.response = response;
	}

	/**
	 * Gets the content length
	 * 
	 * @return The content length
	 */
	public int getContentLength() {
		return contentLength;
	}

	/**
	 * @return <code>true</code> if content length has been set; otherwise
	 *         <code>false</code>
	 */
	public boolean containsContentLength() {
		return this.bContentLength;
	}

	/**
	 * Sets the request's content length
	 * 
	 * @param contentLength
	 *            The content length
	 */
	public void setContentLength(final int contentLength) {
		this.contentLength = contentLength;
		this.bContentLength = true;
	}

	/**
	 * Gets the total requested content length
	 * 
	 * @return The total requested content length
	 */
	public int getTotalRequestedContentLength() {
		return totalRequestedContentLength;
	}

	/**
	 * Increases the total requested content length by specified argument
	 * 
	 * @param increaseBy
	 *            The value by which the total requested content length is
	 *            increased
	 */
	public void increaseTotalRequestedContentLength(final int increaseBy) {
		this.totalRequestedContentLength += increaseBy;
	}

	/**
	 * Sets the total requested content length
	 * 
	 * @param totalRequestedContentLength
	 *            The total requested content length
	 */
	public void setTotalRequestedContentLength(final int totalRequestedContentLength) {
		this.totalRequestedContentLength = totalRequestedContentLength;
	}

	/**
	 * Checks if HTTP headers have already been sent to web server
	 * 
	 * @return <code>true</code> if HTTP headers have already been sent to web
	 *         server; otherwise <code>false</code>
	 */
	public boolean isHeadersSent() {
		return headersSent;
	}

	/**
	 * Sets if HTTP headers have already been sent to web server
	 * 
	 * @param headersSent
	 *            <code>true</code> if HTTP headers have already been sent to
	 *            web server; otherwise <code>false</code>
	 */
	public void setHeadersSent(final boolean headersSent) {
		this.headersSent = headersSent;
	}

	/**
	 * Checks if
	 * {@link Servlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}
	 * has already been called
	 * 
	 * @return <code>true</code> if
	 *         {@link Servlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}
	 *         has already been called; otherwise <code>false</code>
	 */
	public boolean isServiceMethodCalled() {
		return serviceMethodCalled;
	}

	/**
	 * Sets if
	 * {@link Servlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}
	 * has already been called
	 * 
	 * @param serviceMethodCalled
	 *            <code>true</code> if
	 *            {@link Servlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}
	 *            has already been called; otherwise <code>false</code>
	 */
	public void setServiceMethodCalled(final boolean serviceMethodCalled) {
		this.serviceMethodCalled = serviceMethodCalled;
	}

	/**
	 * Checks if AJP's end response package has been sent to web server
	 * 
	 * @return <code>true</code> if AJP's end response package has been sent
	 *         to web server; otherwise <code>false</code>
	 */
	public boolean isEndResponseSent() {
		return endResponseSent;
	}

	/**
	 * Sets the end response flag
	 * 
	 * @param endResponseSent
	 *            <code>true</code> if AJP's end response package has been
	 *            sent to web server; otherwise <code>false</code>
	 */
	public void setEndResponseSent(final boolean endResponseSent) {
		this.endResponseSent = endResponseSent;
	}

	/**
	 * Indicates if request content type equals
	 * <code>application/x-www-form-urlencoded</code>
	 * 
	 * @return <code>true</code> if request content type equals
	 *         <code>application/x-www-form-urlencoded</code>; otherwise
	 *         <code>false</code>
	 */
	public boolean isFormData() {
		return isFormData;
	}

	/**
	 * Marks that requests content type equals
	 * <code>application/x-www-form-urlencoded</code>
	 * 
	 * @param isFormData
	 *            <code>true</code> if request content type equals
	 *            <code>application/x-www-form-urlencoded</code>; otherwise
	 *            <code>false</code>
	 */
	public void setFormData(final boolean isFormData) {
		this.isFormData = isFormData;
	}

	public boolean emptyDataPackageAlreadyReceived() {
		return emptyDataPackageReceived;
	}

	/**
	 * Gets the number of bytes that are left for being requested from web
	 * server
	 * 
	 * @return The number of bytes that are left for being requested
	 */
	public int getNumOfBytesToRequestFor() {
		int retval = contentLength - totalRequestedContentLength;
		if (retval > AJPv13Response.MAX_INT_VALUE || retval < 0) {
			retval = AJPv13Response.MAX_INT_VALUE;
		}
		return retval;
	}

	public void setEmptyDataPackageReceived(final boolean emptyDataPackageReceived) {
		this.emptyDataPackageReceived = emptyDataPackageReceived;
	}

	/**
	 * Checks if amount of received data is equal to value of header
	 * 'Content-Length'
	 * 
	 * @return <code>true</code> if amount of received data is equal to value
	 *         of header 'Content-Length'; otherwise <code>false</code>
	 */
	public boolean isAllDataRead() {
		/*
		 * This method will always return false if content-length is not set
		 * unless method makeEqual() is invoked
		 */
		return (totalRequestedContentLength == contentLength);
	}

	/**
	 * Indicates if servlet container still expects data from web server that is
	 * amount of received data is less than value of header 'Content-Length'
	 * 
	 * @return <code>true</code> if servlet container still expects data from
	 *         web server; otherwise <code>false</code>
	 */
	public boolean isMoreDataExpected() {
		/*
		 * No empty data package received AND requested data length is still
		 * less than header Content-Length
		 */
		return (contentLength != NOT_SET && totalRequestedContentLength < contentLength);
	}

	/**
	 * Checks if header 'Content-Length' has not been set
	 * 
	 * @return <code>true</code> if header 'Content-Length' has not been set;
	 *         otherwise <code>false</code>
	 */
	public boolean isNotSet() {
		return (contentLength == NOT_SET);
	}

	/**
	 * Indicates if amount of received data exceeds value of header
	 * "content-length"
	 * 
	 * @return
	 */
	public boolean isMoreDataReadThanExpected() {
		return (contentLength != NOT_SET && totalRequestedContentLength > contentLength);
	}

	/**
	 * Total requested content length is made equal to header "content-length"
	 * which has the effect that no more data is going to be requested from web
	 * server cause <code>AJPv13RequestHandler.isAllDataRead()</code> will
	 * return <code>true</code>.
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
	 * @param httpSessionId
	 *            The HTTP session ID
	 * @param join
	 *            <code>true</code> if the HTTP session has joined a previous
	 *            HTTP session; otherwise <code>false</code>
	 */
	public void setHttpSessionId(final String httpSessionId, final boolean join) {
		this.httpSessionId = httpSessionId;
		this.httpSessionJoined = join;
	}

	/**
	 * Checks if the HTTP session has joined a previous HTTP session
	 * 
	 * @return <code>true</code> if the HTTP session has joined a previous
	 *         HTTP session; otherwise <code>false</code>
	 */
	public boolean isHttpSessionJoined() {
		return httpSessionJoined;
	}

	/**
	 * Gets the servlet path (which is not the request path). The servlet path
	 * is defined in servlet mapping configuration.
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

	private static String toHexString(final int i) {
		return new StringBuilder(4).append(i < 16 ? "0x0" : "0x").append(Integer.toHexString(i & 0xff).toUpperCase())
				.toString();
	}
}
