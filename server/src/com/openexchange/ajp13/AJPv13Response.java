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

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.openexchange.ajp13.exception.AJPv13Exception;
import com.openexchange.ajp13.exception.AJPv13MaxPackgeSizeException;
import com.openexchange.ajp13.exception.AJPv13Exception.AJPCode;
import com.openexchange.tools.servlet.http.HttpServletResponseWrapper;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;

/**
 * {@link AJPv13Response} - Constructs AJP response packages for
 * <code>END_RESPONSE</code>, <code>SEND_BODY_CHUNK</code>,
 * <code>SEND_HEADERS</code>, <code>GET_BODY_CHUNK</code>, etc.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public class AJPv13Response {

	private static final String STR_EMPTY = "";

	private static final String STR_SET_COOKIE = "Set-Cookie";

	public static final int MAX_INT_VALUE = 65535;

	public static final int MAX_PACKAGE_SIZE = 8192;

	/**
	 * The max. allowed chunk size in a SEND_BODY_CHUNK package, which is the
	 * max. package size of 8192 (8K) minus 8 bytes (MagicBytes + DataLength +
	 * PrefixCode + CunkLength + TerminatingZeroByte)
	 * 
	 * <pre>
	 * 'A' 'B' Data Length PrefixCode Chunk Length [chunk bytes] 00
	 * </pre>
	 */
	public static final int MAX_SEND_BODY_CHUNK_SIZE = 8184; // 8192 - 8

	/**
	 * Byte sequence indicating a packet from Servlet Container to Web Server.
	 */
	private static final int[] PACKAGE_FROM_CONTAINER_TO_SERVER = { 'A', 'B' };

	public static final int SEND_BODY_CHUNK_PREFIX_CODE = 3;

	public static final int SEND_HEADERS_PREFIX_CODE = 4;

	public static final int END_RESPONSE_PREFIX_CODE = 5;

	public static final int GET_BODY_CHUNK_PREFIX_CODE = 6;

	public static final int CPONG_REPLY_PREFIX_CODE = 9;

	public static final Map<String, Integer> headerMap = new HashMap<String, Integer>();

	private static final byte[] cpongReplyBytes;

	/**
	 * Starting first 4 bytes:
	 * 
	 * <pre>
	 * 'A' + 'B' + [data length as 2 byte integer]
	 * </pre>
	 */
	private static final int RESPONSE_PREFIX_LENGTH = 4;

	static {
		/*
		 * Headers
		 */
		headerMap.put("Content-Type", Integer.valueOf(0x01));
		headerMap.put("Content-Language", Integer.valueOf(0x02));
		headerMap.put("Content-Length", Integer.valueOf(0x03));
		headerMap.put("Date", Integer.valueOf(0x04));
		headerMap.put("Last-Modified", Integer.valueOf(0x05));
		headerMap.put("Location", Integer.valueOf(0x06));
		headerMap.put("Set-Cookie", Integer.valueOf(0x07));
		headerMap.put("Set-Cookie2", Integer.valueOf(0x08));
		headerMap.put("Servlet-Engine", Integer.valueOf(0x09));
		headerMap.put("Status", Integer.valueOf(0x0A));
		headerMap.put("WWW-Authenticate", Integer.valueOf(0x0B));
		/*
		 * CPong reply
		 */
		cpongReplyBytes = new byte[5];
		cpongReplyBytes[0] = (byte) PACKAGE_FROM_CONTAINER_TO_SERVER[0];
		cpongReplyBytes[1] = (byte) PACKAGE_FROM_CONTAINER_TO_SERVER[1];
		cpongReplyBytes[2] = 0;
		cpongReplyBytes[3] = 1;
		cpongReplyBytes[4] = CPONG_REPLY_PREFIX_CODE;
	}

	private final int prefixCode;

	private int contentLength = -1;

	private byte[] responseDataChunk;

	private HttpServletResponseWrapper servletResponse;

	private boolean closeConnection;

	/**
	 * Initializes a new {@link AJPv13Response}
	 * 
	 * @param prefixCode
	 *            The prefix code determining kind of response package
	 */
	public AJPv13Response(final int prefixCode) {
		super();
		this.prefixCode = prefixCode;
	}

	/**
	 * Constructor for <code>END_RESPONSE</code>
	 * 
	 * @param prefixCode
	 *            - the <code>END_RESPONSE</code> prefix code
	 * @param closeConnection
	 *            - whether or not to signal to close the connection
	 */
	public AJPv13Response(final int prefixCode, final boolean closeConnection) {
		super();
		this.prefixCode = prefixCode;
		this.closeConnection = closeConnection;
	}

	/**
	 * Constructor for <code>SEND_BODY_CHUNK</code>
	 * 
	 * @param prefixCode
	 *            - the <code>SEND_BODY_CHUNK</code> prefix code
	 * @param responseDataChunk
	 *            - the data chunk as array of <code>byte</code>
	 */
	public AJPv13Response(final int prefixCode, final byte[] responseDataChunk) {
		super();
		this.prefixCode = prefixCode;
		this.responseDataChunk = new byte[responseDataChunk.length];
		System.arraycopy(responseDataChunk, 0, this.responseDataChunk, 0, responseDataChunk.length);
	}

	/**
	 * Constructor for <code>SEND_HEADERS</code>
	 * 
	 * @param prefixCode
	 *            - the <code>SEND_HEADERS</code> prefix code
	 * @param resp
	 *            - the <code>HttpServletResponse</code> object containing http
	 *            header data
	 */
	public AJPv13Response(final int prefixCode, final HttpServletResponseWrapper resp) {
		super();
		this.prefixCode = prefixCode;
		this.servletResponse = resp;
	}

	/**
	 * Constructor for <code>GET_BODY_CHUNK</code>
	 * 
	 * @param prefixCode
	 *            - the <code>GET_BODY_CHUNK</code> prefix code
	 * @param requestedLength
	 *            - the requested body chunk's length
	 */
	public AJPv13Response(final int prefixCode, final int requestedLength) {
		super();
		this.prefixCode = prefixCode;
		this.contentLength = requestedLength;
	}

	public final byte[] getResponseBytes() throws AJPv13Exception {
		final int dataLength;
		final ByteArrayOutputStream byteArray;
		switch (prefixCode) {
		case SEND_BODY_CHUNK_PREFIX_CODE:
			final int length = responseDataChunk.length;
			if (length == 0) {
				throw new AJPv13Exception(AJPCode.NO_EMPTY_SENT_BODY_CHUNK, true);
			}
			/*
			 * prefix + chunk_length (2 bytes) + chunk bytes + terminating zero
			 * byte
			 */
			dataLength = 4 + length;
			if (dataLength + RESPONSE_PREFIX_LENGTH > MAX_PACKAGE_SIZE) {
				throw new AJPv13MaxPackgeSizeException((dataLength + RESPONSE_PREFIX_LENGTH));
			}
			byteArray = new UnsynchronizedByteArrayOutputStream(dataLength + RESPONSE_PREFIX_LENGTH);
			fillStartBytes(prefixCode, dataLength, byteArray);
			writeInt(length, byteArray);
			writeByteArray(responseDataChunk, byteArray);
			writeByte(0, byteArray);
			break;
		case SEND_HEADERS_PREFIX_CODE:
			/*
			 * prefix + http_status_code + http_status_msg (empty string) +
			 * num_headers (integer)
			 */
			final String[][] formattedCookies = servletResponse.getFormatedCookies();
			dataLength = getHeaderSizeInBytes(servletResponse) + getCookiesSizeInBytes(formattedCookies) + 5
					+ servletResponse.getStatusMsg().length() + 2 + 1;
			if (dataLength + RESPONSE_PREFIX_LENGTH > MAX_PACKAGE_SIZE) {
				throw new AJPv13MaxPackgeSizeException((dataLength + RESPONSE_PREFIX_LENGTH));
			}
			byteArray = new UnsynchronizedByteArrayOutputStream(dataLength + RESPONSE_PREFIX_LENGTH);
			fillStartBytes(prefixCode, dataLength, byteArray);
			writeInt(servletResponse.getStatus(), byteArray);
			writeString(servletResponse.getStatusMsg(), byteArray);
			writeInt(servletResponse.getHeadersSize() + getNumOfCookieHeader(formattedCookies), byteArray);
			{
				final int headersSize = servletResponse.getHeadersSize();
				final Iterator<String> iter = servletResponse.getHeaderNames();
				for (int i = 0; i < headersSize; i++) {
					final String headerName = iter.next();
					final String headerValue = servletResponse.getHeader(headerName);
					writeHeader(headerName, headerValue, byteArray);
				}
			}
			for (int i = 0; i < formattedCookies.length; i++) {
				final String hdrName = i == 0 ? STR_SET_COOKIE : new StringBuilder(STR_SET_COOKIE.length() + 1).append(
						STR_SET_COOKIE).append(i + 1).toString();
				for (int j = 0; j < formattedCookies[i].length; j++) {
					writeHeader(hdrName, formattedCookies[i][j], byteArray);
				}
			}
			break;
		case END_RESPONSE_PREFIX_CODE:
			dataLength = 2; // prefix + boolean (1 byte)
			/*
			 * No need to check against max package size cause it's a static
			 * package size of 6
			 */
			byteArray = new UnsynchronizedByteArrayOutputStream(dataLength + RESPONSE_PREFIX_LENGTH);
			fillStartBytes(prefixCode, dataLength, byteArray);
			if (closeConnection) {
				writeBoolean(false, byteArray);
			} else if (AJPv13Config.isAJPModJK()) {
				writeBoolean(true, byteArray);
			} else {
				final boolean reuseConnection = AJPv13Server.getNumberOfOpenAJPSockets() <= AJPv13Config
						.getAJPMaxNumOfSockets();
				writeBoolean(reuseConnection, byteArray);
			}
			break;
		case GET_BODY_CHUNK_PREFIX_CODE:
			dataLength = 3; // prefix + integer (2 bytes)
			/*
			 * No need to check against max package size cause it's a static
			 * package size of 7
			 */
			byteArray = new UnsynchronizedByteArrayOutputStream(dataLength + RESPONSE_PREFIX_LENGTH);
			fillStartBytes(prefixCode, dataLength, byteArray);
			writeInt(contentLength, byteArray);
			break;
		case CPONG_REPLY_PREFIX_CODE:
			dataLength = 1; // just the single CPong prefix byte
			byteArray = new UnsynchronizedByteArrayOutputStream(dataLength + RESPONSE_PREFIX_LENGTH);
			fillStartBytes(prefixCode, dataLength, byteArray);
			break;
		default:
			throw new AJPv13Exception(AJPCode.UNKNOWN_PREFIX_CODE, true, Integer.valueOf(prefixCode));
		}
		return byteArray.toByteArray();
	}

	/**
	 * Creates the <code>SEND_BODY_CHUNK</code> response bytes
	 * 
	 * @param responseDataChunk
	 *            - the data chunk
	 * @return an array of <code>byte</code> containing the
	 *         <code>SEND_BODY_CHUNK</code> response bytes
	 * @throws AJPv13Exception
	 */
	public static final byte[] getSendBodyChunkBytes(final byte[] responseDataChunk) throws AJPv13Exception {
		final int length = responseDataChunk.length;
		if (length == 0) {
			throw new AJPv13Exception(AJPCode.NO_EMPTY_SENT_BODY_CHUNK, true);
		}
		/*
		 * prefix + chunk_length (2 bytes) + chunk bytes + terminating zero byte
		 */
		final int dataLength = 4 + length;
		if (dataLength + RESPONSE_PREFIX_LENGTH > MAX_PACKAGE_SIZE) {
			throw new AJPv13MaxPackgeSizeException((dataLength + RESPONSE_PREFIX_LENGTH));
		}
		final UnsynchronizedByteArrayOutputStream byteArray = new UnsynchronizedByteArrayOutputStream(dataLength
				+ RESPONSE_PREFIX_LENGTH);
		fillStartBytes(SEND_BODY_CHUNK_PREFIX_CODE, dataLength, byteArray);
		writeInt(length, byteArray);
		writeByteArray(responseDataChunk, byteArray);
		writeByte(0, byteArray);
		return byteArray.toByteArray();
	}

	/**
	 * Creates the <code>SEND_HEADERS</code> response bytes
	 * 
	 * @param servletResponse
	 *            - the <code>HttpServletResponse</code> object containing http
	 *            header data
	 * @return an array of <code>byte</code> containing the
	 *         <code>SEND_HEADERS</code> response bytes
	 * @throws AJPv13Exception
	 */
	public static final byte[] getSendHeadersBytes(final HttpServletResponseWrapper servletResponse)
			throws AJPv13Exception {
		/*
		 * prefix + http_status_code + http_status_msg (empty string) +
		 * num_headers (integer)
		 */
		final String[][] formattedCookies = servletResponse.getFormatedCookies();
		String statusMsg = servletResponse.getStatusMsg();
		if (null == statusMsg) {
			statusMsg = "";
		}
		final int dataLength = getHeaderSizeInBytes(servletResponse) + getCookiesSizeInBytes(formattedCookies) + 5
				+ statusMsg.length() + 2 + 1;
		if (dataLength + RESPONSE_PREFIX_LENGTH > MAX_PACKAGE_SIZE) {
			throw new AJPv13MaxPackgeSizeException((dataLength + RESPONSE_PREFIX_LENGTH));
		}
		final UnsynchronizedByteArrayOutputStream byteArray = new UnsynchronizedByteArrayOutputStream(dataLength
				+ RESPONSE_PREFIX_LENGTH);
		fillStartBytes(SEND_HEADERS_PREFIX_CODE, dataLength, byteArray);
		writeInt(servletResponse.getStatus(), byteArray);
		writeString(statusMsg, byteArray);
		writeInt(servletResponse.getHeadersSize() + getNumOfCookieHeader(formattedCookies), byteArray);
		{
			final int headersSize = servletResponse.getHeadersSize();
			final Iterator<String> iter = servletResponse.getHeaderNames();
			for (int i = 0; i < headersSize; i++) {
				final String headerName = iter.next();
				final String headerValue = servletResponse.getHeader(headerName);
				writeHeader(headerName, headerValue, byteArray);
			}
		}
		for (int i = 0; i < formattedCookies.length; i++) {
			final String hdrName = i == 0 ? STR_SET_COOKIE : new StringBuilder(STR_SET_COOKIE.length() + 1).append(
					STR_SET_COOKIE).append(i + 1).toString();
			for (int j = 0; j < formattedCookies[i].length; j++) {
				writeHeader(hdrName, formattedCookies[i][j], byteArray);
			}
		}
		return byteArray.toByteArray();
	}

	/**
	 * Creates the <code>END_RESPONSE</code> response bytes
	 * 
	 * @return an array of <code>byte</code> containing the
	 *         <code>END_RESPONSE</code> response bytes
	 * @throws AJPv13Exception
	 *             If <code>END_RESPONSE</code> bytes cannot be written
	 */
	public static final byte[] getEndResponseBytes() throws AJPv13Exception {
		return getEndResponseBytes(false);
	}

	/**
	 * Creates the <code>END_RESPONSE</code> response bytes
	 * 
	 * @param closeConnection
	 *            - whether or not to signal connection closure
	 * @return an array of <code>byte</code> containing the
	 *         <code>END_RESPONSE</code> response bytes
	 * @throws AJPv13Exception
	 *             If <code>END_RESPONSE</code> bytes cannot be written
	 */
	public static final byte[] getEndResponseBytes(final boolean closeConnection) throws AJPv13Exception {
		final int dataLength = 2; // prefix + boolean (1 byte)
		/*
		 * No need to check against max package size cause it's a static package
		 * size of 6
		 */
		final UnsynchronizedByteArrayOutputStream byteArray = new UnsynchronizedByteArrayOutputStream(dataLength
				+ RESPONSE_PREFIX_LENGTH);
		fillStartBytes(END_RESPONSE_PREFIX_CODE, dataLength, byteArray);
		if (closeConnection) {
			writeBoolean(false, byteArray);
		} else if (AJPv13Config.isAJPModJK()) {
			writeBoolean(true, byteArray);
		} else {
			final boolean reuseConnection = AJPv13Server.getNumberOfOpenAJPSockets() <= AJPv13Config
					.getAJPMaxNumOfSockets();
			writeBoolean(reuseConnection, byteArray);
		}
		return byteArray.toByteArray();
	}

	/**
	 * Creates the <code>GET_BODY_CHUNK</code> response bytes
	 * 
	 * @param requestedLength
	 *            - the requested chunk's size
	 * @return an array of <code>byte</code> containing the
	 *         <code>GET_BODY_CHUNK</code> response bytes
	 * @throws AJPv13Exception
	 */
	public static final byte[] getGetBodyChunkBytes(final int requestedLength) throws AJPv13Exception {
		final int dataLength = 3; // prefix + integer (2 bytes)
		/*
		 * No need to check against max package size cause it's a static package
		 * size of 7
		 */
		final UnsynchronizedByteArrayOutputStream byteArray = new UnsynchronizedByteArrayOutputStream(dataLength
				+ RESPONSE_PREFIX_LENGTH);
		fillStartBytes(GET_BODY_CHUNK_PREFIX_CODE, dataLength, byteArray);
		writeInt(requestedLength, byteArray);
		return byteArray.toByteArray();
	}

	/**
	 * Creates the CPong response bytes
	 * 
	 * @return an array of <code>byte</code> containing the CPong response bytes
	 */
	public static final byte[] getCPongBytes() {
		final byte[] retval = new byte[cpongReplyBytes.length];
		System.arraycopy(cpongReplyBytes, 0, retval, 0, retval.length);
		return retval;
	}

	/*
	 * +++++++++++++++++++++++++ Static helper methods +++++++++++++++++++++++++
	 */
	private static final int getHeaderSizeInBytes(final HttpServletResponseWrapper servletResponse) {
		int retval = 0;
		final StringBuilder sb = new StringBuilder(128);
		final Set<Map.Entry<String, String[]>> set = servletResponse.getHeaderEntrySet();
		for (final Map.Entry<String, String[]> hdr : set) {
			if (headerMap.containsKey(hdr.getKey())) {
				/*
				 * Header can be encoded as an integer
				 */
				retval += 2;
			} else {
				/*
				 * Header must be written as string which takes three extra
				 * bytes in addition to header name length
				 */
				retval += hdr.getKey().length() + 3;
			}
			sb.setLength(0);
			retval += array2string(hdr.getValue(), sb).length() + 3;
		}
		return retval;
	}

	private static final String array2string(final String[] sa, final StringBuilder sb) {
		if (sa == null || sa.length == 0) {
			return STR_EMPTY;
		}
		sb.append(sa[0]);
		for (int i = 1; i < sa.length; i++) {
			sb.append(',');
			sb.append(sa[i]);
		}
		return sb.toString();
	}

	private static final int getCookiesSizeInBytes(final String[][] formattedCookies) {
		int retval = 0;
		for (int i = 0; i < formattedCookies.length; i++) {
			final int hdrNameLen;
			{
				final String hdrName = i == 0 ? STR_SET_COOKIE : new StringBuilder(STR_SET_COOKIE.length() + 1).append(
						STR_SET_COOKIE).append(i + 1).toString();
				/*
				 * Set-Cookie and Set-Cookie2 is encoded in AJP protocol as an
				 * integer value
				 */
				hdrNameLen = headerMap.containsKey(hdrName) ? 2 : hdrName.length() + 3;
			}
			for (int j = 0; j < formattedCookies[i].length; j++) {
				retval += hdrNameLen;
				retval += formattedCookies[i][j].length() + 3;
			}
		}
		return retval;
	}

	private static final int getNumOfCookieHeader(final String[][] formattedCookies) {
		int retval = 0;
		for (int i = 0; i < formattedCookies.length; i++) {
			retval += formattedCookies[i].length;
		}
		return retval;
	}

	private static void writeHeader(final String name, final String value, final ByteArrayOutputStream byteArray)
			throws AJPv13Exception {
		if (headerMap.containsKey(name)) {
			final int code = (0xA0 << 8) + (headerMap.get(name)).intValue();
			writeInt(code, byteArray);
		} else {
			writeString(name, byteArray);
		}
		writeString(value, byteArray);
	}

	/**
	 * Writes the first 5 bytes of an AJP response:
	 * <ol>
	 * <li>Two bytes signaling a package from container to web server: A B</li>
	 * <li>The data length as an integer (takes two bytes)</li>
	 * <li>The response's prefix code</li>
	 * </ol>
	 * 
	 * @throws AJPv13Exception
	 *             If starting bytes cannot be written
	 */
	private static final void fillStartBytes(final int prefixCode, final int dataLength,
			final ByteArrayOutputStream byteArray) throws AJPv13Exception {
		writeByte(PACKAGE_FROM_CONTAINER_TO_SERVER[0], byteArray);
		writeByte(PACKAGE_FROM_CONTAINER_TO_SERVER[1], byteArray);
		writeInt(dataLength, byteArray);
		writeByte(prefixCode, byteArray);
	}

	private static final void writeByte(final int byteValue, final ByteArrayOutputStream byteArray) {
		byteArray.write(byteValue);
	}

	private static final void writeByteArray(final byte[] bytes, final ByteArrayOutputStream byteArray) {
		byteArray.write(bytes, 0, bytes.length);
	}

	private static final void writeInt(final int intValue, final ByteArrayOutputStream byteArray)
			throws AJPv13Exception {
		if (intValue > MAX_INT_VALUE) {
			throw new AJPv13Exception(AJPCode.INTEGER_VALUE_TOO_BIG, true, Integer.valueOf(intValue));
		}
		byteArray.write((intValue >> 8)); // high
		byteArray.write((intValue & (255))); // low
	}

	private static final void writeBoolean(final boolean boolValue, final ByteArrayOutputStream byteArray) {
		byteArray.write(boolValue ? 1 : 0);
	}

	private static final void writeString(final String strValue, final ByteArrayOutputStream byteArray)
			throws AJPv13Exception {
		final int strLength = strValue.length();
		writeInt(strLength, byteArray);
		/*
		 * Write string content and terminating '0'
		 */
		if (strLength > 0) {
			final char[] chars = strValue.toCharArray();
			final byte[] bytes = new byte[strLength];
			for (int i = 0; i < strLength; i++) {
				bytes[i] = (byte) chars[i];
			}
			byteArray.write(bytes, 0, strLength);
		}
		byteArray.write(0);
	}

}
