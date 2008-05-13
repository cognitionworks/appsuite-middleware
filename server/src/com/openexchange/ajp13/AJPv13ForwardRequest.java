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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import javax.mail.MessagingException;
import javax.servlet.http.Cookie;

import com.openexchange.ajp13.AJPv13Exception.AJPCode;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.configuration.ServerConfig.Property;
import com.openexchange.tools.codec.QuotedPrintable;
import com.openexchange.tools.regex.RFC2616Regex;
import com.openexchange.tools.servlet.OXServletInputStream;
import com.openexchange.tools.servlet.OXServletOutputStream;
import com.openexchange.tools.servlet.http.HttpServletRequestWrapper;
import com.openexchange.tools.servlet.http.HttpServletResponseWrapper;
import com.openexchange.tools.servlet.http.HttpSessionManagement;

/**
 * AJPv13ForwardRequest - this class' purpose is mainly to fill the http servlet
 * request from AJP's forward request, to identify servlet instance through
 * request path and to apply the load-balancing and http-session-identifying
 * <tt>JSESSIONID</tt> cookie or URL parameter to the http serlvet response
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public final class AJPv13ForwardRequest extends AJPv13Request {

	private static final String STR_EMPTY = "";

	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory
			.getLog(AJPv13ForwardRequest.class);

	private static final String methods[] = { "OPTIONS", "GET", "HEAD", "POST", "PUT", "DELETE", "TRACE", "PROPFIND",
			"PROPPATCH", "MKCOL", "COPY", "MOVE", "LOCK", "UNLOCK", "ACL", "REPORT", "VERSION-CONTROL", "CHECKIN",
			"CHECKOUT", "UNCHECKOUT", "SEARCH", "MKWORKSPACE", "UPDATE", "LABEL", "MERGE", "BASELINE_CONTROL",
			"MKACTIVITY" };

	private static final Map<Integer, String> httpHeaderMapping = new HashMap<Integer, String>();

	private static final Map<Integer, String> attributeMapping = new HashMap<Integer, String>();

	/**
	 * A "set" to keep track of known JSESSIONIDs
	 */
	private static final Map<String, Object> jsessionids = new ConcurrentHashMap<String, Object>();

	private static final String DEFAULT_ENCODING = ServerConfig.getProperty(Property.DefaultEncoding);

	private static final String MIME_FORM_DATA = "application/x-www-form-urlencoded";

	private static final String HDR_CONTENT_TYPE = "content-type";

	private static final String HDR_CONTENT_LENGTH = "content-length";

	private static final String ATTR_STORED_METHOD = "stored_method";

	private static final String ATTR_QUERY_STRING = "query_string";

	private static final String ATTR_SSL_KEY_SIZE = "ssl_key_size";

	/**
	 * This byte value indicates termination of request.
	 */
	public static final int REQUEST_TERMINATOR = 0xFF;

	static {
		httpHeaderMapping.put(Integer.valueOf(0x01), "accept");
		httpHeaderMapping.put(Integer.valueOf(0x02), "accept-charset");
		httpHeaderMapping.put(Integer.valueOf(0x03), "accept-encoding");
		httpHeaderMapping.put(Integer.valueOf(0x04), "accept-language");
		httpHeaderMapping.put(Integer.valueOf(0x05), "authorization");
		httpHeaderMapping.put(Integer.valueOf(0x06), "connection");
		httpHeaderMapping.put(Integer.valueOf(0x07), HDR_CONTENT_TYPE);
		httpHeaderMapping.put(Integer.valueOf(0x08), HDR_CONTENT_LENGTH);
		httpHeaderMapping.put(Integer.valueOf(0x09), "cookie");
		httpHeaderMapping.put(Integer.valueOf(0x0a), "cookie2");
		httpHeaderMapping.put(Integer.valueOf(0x0b), "host");
		httpHeaderMapping.put(Integer.valueOf(0x0c), "pragma");
		httpHeaderMapping.put(Integer.valueOf(0x0d), "referer");
		httpHeaderMapping.put(Integer.valueOf(0x0e), "user-agent");
		attributeMapping.put(Integer.valueOf(0x01), "context");
		attributeMapping.put(Integer.valueOf(0x02), "servlet_path");
		attributeMapping.put(Integer.valueOf(0x03), "remote_user");
		attributeMapping.put(Integer.valueOf(0x04), "auth_type");
		attributeMapping.put(Integer.valueOf(0x05), ATTR_QUERY_STRING);
		attributeMapping.put(Integer.valueOf(0x06), "jvm_route");
		attributeMapping.put(Integer.valueOf(0x07), "ssl_cert");
		attributeMapping.put(Integer.valueOf(0x08), "ssl_cipher");
		attributeMapping.put(Integer.valueOf(0x09), "ssl_session");
		attributeMapping.put(Integer.valueOf(0x0a), "req_attribute");
		attributeMapping.put(Integer.valueOf(0x0b), ATTR_SSL_KEY_SIZE);
		attributeMapping.put(Integer.valueOf(0x0c), "secret_attribute");
		attributeMapping.put(Integer.valueOf(0x0d), ATTR_STORED_METHOD);
		attributeMapping.put(Integer.valueOf(REQUEST_TERMINATOR), "are_done");
	}

	public AJPv13ForwardRequest(final byte[] payloadData) {
		super(payloadData);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.openexchange.tools.ajp13.AJPv13Request#processRequest(com.openexchange.tools.ajp13.AJPv13RequestHandler)
	 */
	@Override
	public void processRequest(final AJPv13RequestHandler ajpRequestHandler) throws AJPv13Exception, IOException {
		processForwardRequest(ajpRequestHandler);
	}

	/**
	 * Processes an incoming AJP Forward Package which contains all header data
	 * that are written into servlet's request object.
	 */
	private void processForwardRequest(final AJPv13RequestHandler ajpRequestHandler) throws AJPv13Exception,
			IOException {
		/*
		 * Create Servlet Request with its InputStream
		 */
		final HttpServletRequestWrapper servletRequest = new HttpServletRequestWrapper(ajpRequestHandler);
		servletRequest.setOXInputStream(new OXServletInputStream(ajpRequestHandler.getAJPConnection()));
		/*
		 * Create Servlet Response with its OutputStream
		 */
		final HttpServletResponseWrapper servletResponse = new HttpServletResponseWrapper(servletRequest);
		servletResponse.setOXOutputStream(new OXServletOutputStream(ajpRequestHandler.getAJPConnection()));
		/*
		 * Determine method: If next byte is equal to 0xff then the method is
		 * given by "stored_method" attribute
		 */
		final byte encodedMethod = nextByte();
		if (encodedMethod != -1) {
			servletRequest.setMethod(methods[encodedMethod - 1]);
		}
		/*
		 * Determine protocol
		 */
		try {
			servletRequest.setProtocol(parseString());
		} catch (final AJPv13Exception e) {
			throw new AJPv13Exception(AJPCode.UNPARSEABLE_HEADER_FIELD, true, e, "protocol");
		}
		/*
		 * Determine req_uri
		 */
		String requestURI = null;
		String jsessionID = null;
		try {
			requestURI = parseString();
			final int pos = requestURI.toLowerCase(Locale.ENGLISH).indexOf(AJPv13RequestHandler.JSESSIONID_URI);
			if (pos > -1) {
				jsessionID = requestURI.substring(pos + 12);
				requestURI = requestURI.substring(0, pos);
				servletRequest.setRequestedSessionIdFromURL(true);
				servletRequest.setRequestedSessionIdFromCookie(false);
			}
			servletRequest.setRequestURI(requestURI);
			servletRequest.setPathInfo(requestURI);
		} catch (final AJPv13Exception e) {
			throw new AJPv13Exception(AJPCode.UNPARSEABLE_HEADER_FIELD, true, e, "req_uri");
		}
		/*
		 * Determine remote_addr
		 */
		try {
			servletRequest.setRemoteAddr(parseString());
		} catch (final AJPv13Exception e) {
			throw new AJPv13Exception(AJPCode.UNPARSEABLE_HEADER_FIELD, true, e, "remote_addr");
		}
		/*
		 * Determine remote_host
		 */
		try {
			servletRequest.setRemoteHost(parseString());
		} catch (final AJPv13Exception e) {
			throw new AJPv13Exception(AJPCode.UNPARSEABLE_HEADER_FIELD, true, e, "remote_host");
		}
		/*
		 * Determine server_name
		 */
		try {
			servletRequest.setServerName(parseString());
		} catch (final AJPv13Exception e) {
			throw new AJPv13Exception(AJPCode.UNPARSEABLE_HEADER_FIELD, true, e, "server_name");
		}
		/*
		 * Determine server_name
		 */
		servletRequest.setServerPort(parseInt());
		/*
		 * Determine is_ssl
		 */
		servletRequest.setSecure(parseBoolean());
		/*
		 * Determine num_headers
		 */
		final int numHeaders = parseInt();
		/*
		 * Determine Request Headers
		 */
		parseRequestHeaders(servletRequest, numHeaders);
		/*
		 * Set important header CONTENT_LENGTH which decides whether to further
		 * process an upcoming body request from web server or to terminate
		 * communication after this forward request. If this header is missing
		 * the getIntHeader() method returns -1 which represents a missing
		 * content length in request handler, too.
		 */
		final int contentLength = servletRequest.getIntHeader(HDR_CONTENT_LENGTH);
		if (contentLength == -1) {
			ajpRequestHandler.setContentLength(AJPv13RequestHandler.NOT_SET);
		} else {
			ajpRequestHandler.setContentLength(contentLength);
		}
		/*
		 * Determine if content type indicates form data
		 */
		if (servletRequest.containsHeader(HDR_CONTENT_TYPE)
				&& MIME_FORM_DATA.regionMatches(0, servletRequest.getHeader(HDR_CONTENT_TYPE), 0, 33)) {
			ajpRequestHandler.setFormData(true);
		}
		/*
		 * End of payload data NOT reached
		 */
		if (!compareNextByte(REQUEST_TERMINATOR)) {
			/*
			 * Determine Attributes
			 */
			parseAttributes(servletRequest);
			if ((encodedMethod == -1) && servletRequest.containsAttribute(ATTR_STORED_METHOD)) {
				servletRequest.setMethod((String) servletRequest.getAttribute(ATTR_STORED_METHOD));
			}
			if (servletRequest.containsAttribute(ATTR_QUERY_STRING)) {
				parseQueryString(servletRequest, (String) servletRequest.getAttribute(ATTR_QUERY_STRING));
			}
		}
		/*
		 * JSESSIONID
		 */
		if (jsessionID == null) {
			/*
			 * Look for JSESSIONID cookie, if request URI does not contain
			 * session id
			 */
			checkJSessionIDCookie(servletRequest, servletResponse, ajpRequestHandler);
		} else {
			final int dot = jsessionID.lastIndexOf('.');
			if ((dot != -1) && (!AJPv13Config.getJvmRoute().equals(jsessionID.substring(dot + 1)))) {
				/*
				 * JVM route does not match
				 */
				createJSessionIDCookie(servletResponse, ajpRequestHandler);
			} else {
				addJSessionIDCookie(jsessionID, servletResponse, ajpRequestHandler);
			}
		}
		/*
		 * Apply request/response to ajp request handler
		 */
		ajpRequestHandler.setServletRequest(servletRequest);
		ajpRequestHandler.setServletResponse(servletResponse);
		/*
		 * Create servlet instance dependent on requested URI
		 */
		ajpRequestHandler.setServletInstance(requestURI);
		if (null != ajpRequestHandler.getServletPath()) {
			/*
			 * Apply the servlet path with leading "/" to the request
			 */
			if (allPath(ajpRequestHandler.getServletPath())) {
				/*
				 * Set an empty string ("") if the servlet used to process this
				 * request was matched using the "/*" pattern.
				 */
				servletRequest.setServletPath(STR_EMPTY);
			} else {
				/*
				 * The path starts with a "/" character and includes either the
				 * servlet name or a path to the servlet, but does not include
				 * any extra path information or a query string.
				 */
				servletRequest.setServletPath(new StringBuilder().append('/')
						.append(ajpRequestHandler.getServletPath()).toString());
			}
		}
	}

	private static boolean allPath(final String servletPath) {
		return (servletPath.length() == 1) && (servletPath.charAt(0) == '*');
	}

	/**
	 * Parses a query string and puts resulting parameters into given servlet
	 * request
	 * 
	 * @param servletRequest
	 *            The servlet request
	 * @param queryStr
	 *            The query string to be parsed
	 * @throws UnsupportedEncodingException
	 *             If charset provided by servlet request is not supported
	 */
	static void parseQueryString(final HttpServletRequestWrapper servletRequest, final String queryStr)
			throws UnsupportedEncodingException {
		servletRequest.setQueryString(queryStr);
		final String[] paramsNVPs = queryStr.split("&");
		for (int i = 0; i < paramsNVPs.length; i++) {
			paramsNVPs[i] = paramsNVPs[i].trim();
		}
		for (int i = 0; i < paramsNVPs.length; i++) {
			final int pos = paramsNVPs[i].indexOf('=');
			if (pos > -1) {
				servletRequest.setParameter(paramsNVPs[i].substring(0, pos), decodeQueryStringValue(servletRequest
						.getCharacterEncoding(), paramsNVPs[i].substring(pos + 1)));
			} else {
				servletRequest.setParameter(paramsNVPs[i], STR_EMPTY);
			}
		}
	}

	private void parseRequestHeaders(final HttpServletRequestWrapper servletRequest, final int numHeaders)
			throws AJPv13Exception {
		boolean contentTypeSet = false;
		NextHeader: for (int i = 1; i <= numHeaders; i++) {
			final String headerName;
			final boolean isCookie;
			{
				final byte firstByte = nextByte();
				final byte secondByte = nextByte();
				if (firstByte == (byte) 0xA0) {
					/*
					 * Header name is encoded as an integer value.
					 */
					headerName = httpHeaderMapping.get(Integer.valueOf(secondByte));
					if (!contentTypeSet && (secondByte == 0x07)) {
						servletRequest.setContentType(parseString());
						contentTypeSet = true;
						continue NextHeader;
					}
					isCookie = (secondByte == 0x09);
				} else {
					headerName = parseString(firstByte, secondByte);
					if (!contentTypeSet && HDR_CONTENT_TYPE.equalsIgnoreCase(headerName)) {
						servletRequest.setContentType(parseString());
						contentTypeSet = true;
						continue NextHeader;
					}
					isCookie = ("cookie".equalsIgnoreCase(headerName));
				}
			}
			final String headerValue = parseString();
			if (isCookie) {
				servletRequest.setCookies(parseCookieHeader(headerValue));
			} else {
				servletRequest.setHeader(headerName, headerValue, false);
			}
		}
	}

	private static final Set<String> COOKIE_PARAMS = new HashSet<String>(Arrays.asList(new String[] { "$Path",
			"$Domain", "$Port" }));

	private static Cookie[] parseCookieHeader(final String headerValue) throws AJPv13Exception {
		final Matcher m = RFC2616Regex.COOKIE.matcher(headerValue);
		final List<Cookie> cookieList = new ArrayList<Cookie>();
		final StringBuilder valueBuilder = new StringBuilder(128);
		int prevEnd = -1;
		while (m.find()) {
			// offset + 1 -> complete single cookie
			int version = -1;
			{
				final String versionStr = m.group(2);
				if (versionStr == null) {
					version = 0;
				} else {
					try {
						version = Integer.parseInt(versionStr);
					} catch (final NumberFormatException e) {
						version = 0;
						if (LOG.isDebugEnabled()) {
							LOG.debug("Version set to 0. No number value in $Version cookie: " + versionStr);
						}
					}
				}
			}
			final String name = m.group(3);
			if (null == name) {
				// Regex has always at minimum 2 cookies as groups.
				continue;
			}
			if ((name.length() > 0) && (name.charAt(0) == '$')) {
				if (COOKIE_PARAMS.contains(name)) {
					/*
					 * A wrongly parsed cookie name which is actually a cookie
					 * parameter. Force re-parse of previous cookie.
					 */
					continue;
				}
				if (LOG.isInfoEnabled()) {
					LOG.info(new StringBuilder(32).append("Special cookie ").append(name).append(" not handled, yet!"));
				}
			}
			if (prevEnd != -1) {
				final int start = m.start();
				if (start > prevEnd) {
					// Last cookie skipped some characters
					reparsePrevCookie(headerValue, prevEnd, start, valueBuilder, cookieList);
				}
			}
			prevEnd = m.end();
			final Cookie c = new Cookie(name, stripQuotes(m.group(4)));
			c.setVersion(version);
			String attr = m.group(5);
			if (attr != null) {
				/*
				 * Set $Path parameter
				 */
				c.setPath(attr);
			}
			attr = m.group(6);
			if (attr != null) {
				/*
				 * Set $Domain parameter
				 */
				c.setDomain(attr);
			}
			/*
			 * Ignore $Port, apply version, and add to list
			 */
			cookieList.add(c);
		}
		final int len = headerValue.length();
		if (len > prevEnd) {
			// Last cookie skipped some characters
			reparsePrevCookie(headerValue, prevEnd, len, valueBuilder, cookieList);
		}
		if ((headerValue.length() > 0) && cookieList.isEmpty()) {
			throw new AJPv13Exception(AJPv13Exception.AJPCode.INVALID_COOKIE_HEADER, true, headerValue);
		}
		return cookieList.toArray(new Cookie[cookieList.size()]);
	}

	/**
	 * Re-Parse previous cookie in list
	 * 
	 * @param headerValue
	 *            The complete cookie header value
	 * @param prevEnd
	 *            The detected cookie's end position
	 * @param realEnd
	 *            The real cookie's end position
	 * @param valueBuilder
	 *            A string builder needed to compose proper cookie value
	 * @param cookieList
	 *            The cookie list
	 */
	private static void reparsePrevCookie(final String headerValue, final int prevEnd, final int realEnd,
			final StringBuilder valueBuilder, final List<Cookie> cookieList) {
		final String prevValue;
		final Cookie prevCookie = cookieList.get(cookieList.size() - 1);
		valueBuilder.append(prevCookie.getValue());
		String prevAttr = prevCookie.getPath();
		if (null != prevAttr) {
			valueBuilder.append("; $Path=").append(prevAttr);
		}
		prevAttr = prevCookie.getDomain();
		if (null != prevAttr) {
			valueBuilder.append("; $Domain=").append(prevAttr);
		}
		valueBuilder.append(headerValue.substring(prevEnd, realEnd));
		final String complVal = valueBuilder.toString();
		valueBuilder.setLength(0);
		int paramsStart = -1;
		/*
		 * Check for parameters except $Port
		 */
		Matcher paramMatcher = RFC2616Regex.COOKIE_PARAM_PATH.matcher(complVal);
		if (paramMatcher.find()) {
			paramsStart = paramMatcher.start();
			prevCookie.setPath(paramMatcher.group(1));
		}
		paramMatcher = RFC2616Regex.COOKIE_PARAM_DOMAIN.matcher(complVal);
		if (paramMatcher.find()) {
			paramsStart = Math.min(paramMatcher.start(), paramsStart);
			prevCookie.setDomain(paramMatcher.group(1));
		}
		if (paramsStart != -1) {
			prevValue = complVal.substring(0, paramsStart);
		} else {
			prevValue = complVal;
		}
		prevCookie.setValue(prevValue);
	}

	/**
	 * Removes heading/trailing quote character <code>'"'</code> if both
	 * present.
	 * 
	 * @param cookieValue
	 *            The cookie value to strip quotes from
	 * @return The stripped cookie value.
	 */
	private static String stripQuotes(final String cookieValue) {
		if (null == cookieValue || cookieValue.length() == 0) {
			return cookieValue;
		}
		final int mlen = cookieValue.length() - 1;
		if ((cookieValue.charAt(0) == '"') && (cookieValue.charAt(mlen) == '"')) {
			return cookieValue.substring(1, mlen);
		}
		return cookieValue;
	}

	private void parseAttributes(final HttpServletRequestWrapper servletRequest) throws AJPv13Exception {
		byte nextByte = (byte) REQUEST_TERMINATOR;
		while ((nextByte = nextByte()) != ((byte) REQUEST_TERMINATOR)) {
			final Integer attrNum = Integer.valueOf(unsignedByte2Int(nextByte));
			if (attrNum.intValue() == 0x0b) {
				servletRequest.setAttribute(ATTR_SSL_KEY_SIZE, Integer.valueOf(parseInt()));
			} else {
				final String attributeName = attributeMapping.get(attrNum);
				if (attributeName == null) {
					throw new AJPv13Exception(AJPCode.NO_ATTRIBUTE_NAME, true, Byte.valueOf(nextByte));
				}
				servletRequest.setAttribute(attributeName, parseString());
			}
		}
	}

	private static void checkJSessionIDCookie(final HttpServletRequestWrapper servletRequest,
			final HttpServletResponseWrapper resp, final AJPv13RequestHandler ajpRequestHandler) {
		final Cookie[] cookies = servletRequest.getCookies();
		Cookie jsessionIDCookie = null;
		if (cookies != null) {
			NextCookie: for (int i = 0; (i < cookies.length) && (jsessionIDCookie == null); i++) {
				final Cookie current = cookies[i];
				if (AJPv13RequestHandler.JSESSIONID_COOKIE.equals(current.getName())) {
					/*
					 * Check JVM route
					 */
					final String id = current.getValue();
					final int pos = id.lastIndexOf('.');
					if (pos > -1) {
						if ((AJPv13Config.getJvmRoute() != null)
								&& !AJPv13Config.getJvmRoute().equals(id.substring(pos + 1))) {
							/*
							 * Different JVM route detected -> Discard
							 */
							if (LOG.isDebugEnabled()) {
								LOG.debug(new StringBuilder("\n\tDifferent JVM route detected. Ignoring JSESSIONID: ")
										.append(id));
							}
							break NextCookie;
						}
						/*
						 * Check known JSESSIONIDs and corresponding HTTP
						 * session
						 */
						if (!jsessionids.containsKey(id) || !HttpSessionManagement.isHttpSessionValid(id)) {
							/*
							 * Invalid cookie
							 */
							if (LOG.isDebugEnabled()) {
								LOG.debug(new StringBuilder(
										"\n\tExpired or invalid cookie -> Removing JSESSIONID cookie: ").append(current
										.getValue()));
							}
							resp.removeCookie(current);
							break NextCookie;
						}
						jsessionIDCookie = current;
						ajpRequestHandler.setHttpSessionId(id, true);
					} else {
						/*
						 * Value does not apply to pattern [UID].[JVM-ROUTE],
						 * hence only UID is given through special cookie
						 * JSESSIONID.
						 */
						if (AJPv13Config.getJvmRoute() != null) {
							/*
							 * But this host defines a JVM route
							 */
							if (LOG.isDebugEnabled()) {
								LOG.debug(new StringBuilder("\n\tMissing JVM route in JESSIONID cookie").append(current
										.getValue()));
							}
							break NextCookie;
						}
						/*
						 * Check known JSESSIONIDs and corresponding HTTP
						 * session
						 */
						if (!jsessionids.containsKey(id) || !HttpSessionManagement.isHttpSessionValid(id)) {
							/*
							 * Invalid cookie
							 */
							jsessionids.remove(id);
							if (LOG.isDebugEnabled()) {
								LOG.debug(new StringBuilder(
										"\n\tExpired or invalid cookie -> Removing JSESSIONID cookie: ").append(current
										.getValue()));
							}
							resp.removeCookie(current);
							break NextCookie;
						}
						jsessionIDCookie = current;
						ajpRequestHandler.setHttpSessionId(id, true);
					}
				}
			}
		}
		if (jsessionIDCookie == null) {
			createJSessionIDCookie(resp, ajpRequestHandler);
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug(new StringBuilder("\n\tCurrent JSESSIONID: ").append(jsessionIDCookie.getValue()));
			}
		}
	}

	private static void createJSessionIDCookie(final HttpServletResponseWrapper resp,
			final AJPv13RequestHandler ajpRequestHandler) {
		addJSessionIDCookie(null, resp, ajpRequestHandler);
	}

	private static void addJSessionIDCookie(final String id, final HttpServletResponseWrapper resp,
			final AJPv13RequestHandler ajpRequestHandler) {
		final String jsessionIdVal;
		final boolean join;
		if (id == null) {
			/*
			 * Create a new unique id
			 */
			final StringBuilder jsessionIDVal = new StringBuilder(HttpSessionManagement.getNewUniqueId());
			if ((AJPv13Config.getJvmRoute() != null) && (AJPv13Config.getJvmRoute().length() > 0)) {
				jsessionIDVal.append('.').append(AJPv13Config.getJvmRoute());
			}
			jsessionIdVal = jsessionIDVal.toString();
			join = true;
		} else {
			/*
			 * Check known JSESSIONIDs and corresponding HTTP session
			 */
			if (!jsessionids.containsKey(id) || !HttpSessionManagement.isHttpSessionValid(id)) {
				/*
				 * Invalid cookie. Create a new unique id
				 */
				jsessionids.remove(id);
				final StringBuilder jsessionIDVal = new StringBuilder(HttpSessionManagement.getNewUniqueId());
				if ((AJPv13Config.getJvmRoute() != null) && (AJPv13Config.getJvmRoute().length() > 0)) {
					jsessionIDVal.append('.').append(AJPv13Config.getJvmRoute());
				}
				jsessionIdVal = jsessionIDVal.toString();
				join = true;
			} else {
				jsessionIdVal = id;
				join = false;
			}
		}
		final Cookie jsessionIDCookie = new Cookie(AJPv13RequestHandler.JSESSIONID_COOKIE, jsessionIdVal);
		jsessionIDCookie.setPath("/");
		jsessionIDCookie.setMaxAge(-1); // session cookie
		ajpRequestHandler.setHttpSessionId(jsessionIdVal, join);
		jsessionids.put(jsessionIdVal, STR_EMPTY);
		resp.addCookie(jsessionIDCookie);
		if (LOG.isDebugEnabled()) {
			LOG.debug(new StringBuilder("\n\tSetting JSESSIONID cookie to: ").append(jsessionIdVal));
		}
	}

	private String parseString() throws AJPv13Exception {
		return parseString(nextByte(), nextByte());
	}

	/**
	 * First two bytes, which indicate length of string, already consumed.
	 */
	private String parseString(final byte firstByte, final byte secondByte) throws AJPv13Exception {
		/*
		 * Special Byte 0xFF indicates absence of current string value.
		 */
		if ((firstByte == ((byte) REQUEST_TERMINATOR)) && (secondByte == (byte) REQUEST_TERMINATOR)) {
			return STR_EMPTY;
		}
		final StringBuilder sb = new StringBuilder();
		boolean encoded = false;
		final int strLength = (unsignedByte2Int(firstByte) << 8) + unsignedByte2Int(secondByte);
		for (int strIndex = 0; strIndex < strLength; strIndex++) {
			final byte b = nextByte();
			if (b < 0) {
				encoded = true;
				sb.append('=').append(Integer.toHexString(unsignedByte2Int(b)));
			} else {
				sb.append((char) b);
			}
		}
		if (nextByte() != 0x00) {
			throw new AJPv13Exception(AJPCode.UNPARSEABLE_STRING, true);
		}
		if (encoded) {
			try {
				return QuotedPrintable.decodeString(sb.toString(), DEFAULT_ENCODING);
			} catch (final IOException e) {
				throw new AJPv13Exception(AJPCode.IO_ERROR, true, e, e.getMessage());
			} catch (final MessagingException e) {
				throw new AJPv13Exception(AJPCode.MESSAGING_ERROR, true, e, e.getMessage());
			}
		}
		return sb.toString();
	}

	private boolean parseBoolean() {
		return (nextByte() > 0);
	}

	private static String decodeQueryStringValue(final String charEnc, final String queryStringValue)
			throws UnsupportedEncodingException {
		return URLDecoder.decode(queryStringValue, charEnc == null ? ServerConfig.getProperty(Property.DefaultEncoding)
				: charEnc);
	}
}
