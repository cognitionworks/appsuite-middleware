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

package com.openexchange.tools.servlet.http;

import static com.openexchange.tools.TimeZoneUtils.getTimeZone;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.ajax.Login;
import com.openexchange.ajax.helper.BrowserDetector;
import com.openexchange.config.ConfigurationService;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.java.Charsets;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.systemname.SystemNameService;
import com.openexchange.tools.encoding.Helper;

/**
 * Convenience methods for servlets.
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class Tools {

    /**
     * DateFormat for HTTP header.
     */
    private static final DateFormat HEADER_DATEFORMAT;

    /**
     * Cache-Control HTTP header name.
     */
    private static final String CACHE_CONTROL_KEY = "Cache-Control";

    /**
     * Cache-Control value.
     */
    private static final String CACHE_VALUE = "no-store, no-cache, must-revalidate, post-check=0, pre-check=0";

    /**
     * Expires HTTP header name.
     */
    private static final String EXPIRES_KEY = "Expires";

    /**
     * Expires HTTP header value.
     */
    private static final String EXPIRES_DATE;

    /**
     * Pragma HTTP header key.
     */
    private static final String PRAGMA_KEY = "Pragma";

    /**
     * Pragma HTTP header value.
     */
    private static final String PRAGMA_VALUE = "no-cache";

    /**
     * Prevent instantiation
     */
    private Tools() {
        super();
    }

    /**
     * Sets specified ETag header (and implicitly removes/replaces any existing cache-controlling header: <i>Expires</i>,
     * <i>Cache-Control</i>, and <i>Pragma</i>)
     *
     * @param eTag The ETag value
     * @param resp The HTTP servlet response to apply to
     */
    public static void setETag(final String eTag, final HttpServletResponse resp) {
        setETag(eTag, null, resp);
    }

    private static final long MILLIS_WEEK = 604800000L;

    private static final long MILLIS_YEAR = 56 * MILLIS_WEEK;

    /**
     * Sets specified ETag header (and implicitly removes/replaces any existing cache-controlling header: <i>Expires</i>,
     * <i>Cache-Control</i>, and <i>Pragma</i>)
     *
     * @param eTag The ETag value
     * @param expires The optional expires date, pass <code>null</code> to set default expiry (+ 1 year)
     * @param resp The HTTP servlet response to apply to
     */
    public static void setETag(final String eTag, final Date expires, final HttpServletResponse resp) {
        removeCachingHeader(resp);
        resp.setHeader("ETag", eTag);
        if (null == expires) {
            resp.setHeader(EXPIRES_KEY, Long.toString(System.currentTimeMillis() + MILLIS_YEAR));
        } else {
            synchronized (HEADER_DATEFORMAT) {
                resp.setHeader(EXPIRES_KEY, HEADER_DATEFORMAT.format(expires));
            }
        }
    }

    /**
     * The magic spell to disable caching. Do not use these headers if response is directly written into servlet's output stream to initiate
     * a download.
     *
     * @param resp the servlet response.
     * @see #removeCachingHeader(HttpServletResponse)
     */
    public static void disableCaching(final HttpServletResponse resp) {
        resp.setHeader(EXPIRES_KEY, EXPIRES_DATE);
        resp.setHeader(CACHE_CONTROL_KEY, CACHE_VALUE);
        resp.setHeader(PRAGMA_KEY, PRAGMA_VALUE);
    }

    /**
     * Remove <tt>Pragma</tt> response header value if we are going to write directly into servlet's output stream cause then some browsers
     * do not allow this header.
     *
     * @param resp the servlet response.
     */
    public static void removeCachingHeader(final HttpServletResponse resp) {
        resp.setHeader(PRAGMA_KEY, null);
        resp.setHeader(CACHE_CONTROL_KEY, null);
        resp.setHeader(EXPIRES_KEY, null);
    }

    /**
     * Formats a date for HTTP headers.
     *
     * @param date date to format.
     * @return the string with the formated date.
     */
    public static String formatHeaderDate(final Date date) {
        synchronized (HEADER_DATEFORMAT) {
            return HEADER_DATEFORMAT.format(date);
        }
    }

    /**
     * Parses a date from a HTTP date header.
     *
     * @param str The HTTP date header value
     * @return The parsed <code>java.util.Date</code> object
     * @throws ParseException If the date header value cannot be parsed
     */
    public static Date parseHeaderDate(final String str) throws ParseException {
        synchronized (HEADER_DATEFORMAT) {
            return HEADER_DATEFORMAT.parse(str);
        }
    }

    /**
     * HTTP header name containing the user agent.
     */
    public static final String HEADER_AGENT = "User-Agent";

    /**
     * HTTP header name containing the content type of the body.
     */
    public static final String HEADER_TYPE = "Content-Type";

    /**
     * HTTP header name containing the site that caused the request.
     */
    public static final String HEADER_REFERER = "Referer";

    /**
     * HTTP header name containing the length of the body.
     */
    public static final String HEADER_LENGTH = "Content-Length";

    /**
     * This method integrates interesting HTTP header values into a string for logging purposes. This is usefull if a client sent an illegal
     * request for discovering the cause of the illegal request.
     *
     * @param req the servlet request.
     * @return a string containing interesting HTTP headers.
     */
    public static String logHeaderForError(final HttpServletRequest req) {
        final StringBuilder message = new StringBuilder();
        message.append("|\n");
        message.append(HEADER_AGENT);
        message.append(": ");
        message.append(req.getHeader(HEADER_AGENT));
        message.append('\n');
        message.append(HEADER_TYPE);
        message.append(": ");
        message.append(req.getHeader(HEADER_TYPE));
        message.append('\n');
        message.append(HEADER_REFERER);
        message.append(": ");
        message.append(req.getHeader(HEADER_REFERER));
        message.append('\n');
        message.append(HEADER_LENGTH);
        message.append(": ");
        message.append(req.getHeader(HEADER_LENGTH));
        return message.toString();
    }

    /**
     * The name of the JSESSIONID cookie
     */
    public static final String JSESSIONID_COOKIE = "JSESSIONID";

    private static final CookieNameMatcher OX_COOKIE_MATCHER = new CookieNameMatcher() {

        @Override
        public boolean matches(final String cookieName) {
            return (null != cookieName && (cookieName.startsWith(Login.SESSION_PREFIX) || JSESSIONID_COOKIE.equals(cookieName)));
        }
    };

    /**
     * Deletes all OX specific cookies.
     *
     * @param req The HTTP servlet request.
     * @param resp The HTTP servlet response.
     */
    public static void deleteCookies(final HttpServletRequest req, final HttpServletResponse resp) {
        deleteCookies(req, resp, OX_COOKIE_MATCHER);
    }

    /**
     * Deletes all cookies which satisfy specified matcher.
     *
     * @param req The HTTP servlet request.
     * @param resp The HTTP servlet response.
     * @param matcher The cookie name matcher determining which cookie shall be deleted
     */
    public static void deleteCookies(final HttpServletRequest req, final HttpServletResponse resp, final CookieNameMatcher matcher) {
        final Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (final Cookie cookie : cookies) {
                final String cookieName = cookie.getName();
                if (matcher.matches(cookieName)) {
                    final Cookie respCookie = new Cookie(cookieName, cookie.getValue());
                    respCookie.setPath("/");
                    respCookie.setMaxAge(0);
                    resp.addCookie(respCookie);
                }
            }
        }
    }

    public static void setHeaderForFileDownload(final String userAgent, final HttpServletResponse resp, final String fileName) throws UnsupportedEncodingException {
        setHeaderForFileDownload(userAgent, resp, fileName, null);
    }

    public static void setHeaderForFileDownload(final String userAgent, final HttpServletResponse resp, final String fileName, final String contentDisposition) throws UnsupportedEncodingException {
        final BrowserDetector detector = new BrowserDetector(userAgent);
        String cd = contentDisposition;
        if (cd == null) {
            cd = "attachment";
        }
        String filename = null;

        if (detector.isMSIE()) {
            filename = Helper.encodeFilenameForIE(fileName, Charsets.UTF_8);
        } else if (detector.isSafari5()) {
            /*-
             * On socket layer characters are casted to byte values.
             *
             * See AJPv13Response.writeString():
             * sink.write((byte) chars[i]);
             *
             * Therefore ensure we have a one-character-per-byte charset, as it is with ISO-5589-1
             */
            filename = new String(fileName.getBytes(Charsets.UTF_8), Charsets.ISO_8859_1);
        } else {
            filename = Helper.escape(Helper.encodeFilename(fileName, "UTF-8"));
        }

        if (cd.indexOf(';') < 0 && filename != null) {
            cd = new StringBuilder(64).append(cd).append("; filename=\"").append(filename).append("\"").toString();
        }

        resp.setHeader("Content-Disposition", cd);
    }

    private static final Pattern PAT_BSLASH = Pattern.compile("\\\\");

    private static final Pattern PAT_QUOTE = Pattern.compile("\"");

    private static String escapeBackslashAndQuote(final String str) {
        return PAT_QUOTE.matcher(PAT_BSLASH.matcher(str).replaceAll("\\\\\\\\")).replaceAll("\\\\\\\"");
    }

    static {
        /*
         * Pattern for the HTTP header date format.
         */
        HEADER_DATEFORMAT = new SimpleDateFormat("EEE',' dd MMMM yyyy HH:mm:ss z", Locale.ENGLISH);
        HEADER_DATEFORMAT.setTimeZone(getTimeZone("GMT"));
        EXPIRES_DATE = HEADER_DATEFORMAT.format(new Date(799761600000L));
    }

    public static interface CookieNameMatcher {

        /**
         * Indicates if specified cookie name matches.
         *
         * @param cookieName The cookie name to check
         * @return <code>true</code> if specified cookie name matches; otherwise <code>false</code>
         */
        boolean matches(String cookieName);
    }

    /**
     * Tries to determine the best protocol used for accessing this server instance. If the configuration property
     * com.openexchange.forceHTTPS is set to true, this will always be https://, otherwise the request will be used to determine the
     * protocol. https:// if it was a secure request, http:// otherwise
     *
     * @param req The HttpServletRequest used to contact this server
     * @return "http://" or "https://" depending on what was deemed more appropriate
     */
    public static String getProtocol(final HttpServletRequest req) {
        return (considerSecure(req)) ? "https://" : "http://";
    }

    public static boolean considerSecure(final HttpServletRequest req) {
        final ConfigurationService configurationService = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
        if (configurationService != null) {
            return configurationService.getBoolProperty(ServerConfig.Property.FORCE_HTTPS.getPropertyName(), false);
        }
        return req.isSecure();
    }

    /**
     * Gets the route for specified HTTP session identifier to be used along with <i>";jsessionid"</i> URL part.
     *
     * @param httpSessionId The HTTP session identifier
     * @return The route
     */
    public static String getRoute(final String httpSessionId) {
        if (null == httpSessionId) {
            return null;
        }
        final int pos = httpSessionId.indexOf('.');
        return pos > 0 ? httpSessionId : httpSessionId + '.' + ServerServiceRegistry.getInstance().getService(SystemNameService.class).getSystemName();
    }

    public static final Map<String, List<String>> copyHeaders(final HttpServletRequest req) {
        final Map<String, List<String>> headers = new HashMap<String, List<String>>();
        for (final Enumeration<?> e = req.getHeaderNames(); e.hasMoreElements();) {
            final String name = (String) e.nextElement();
            List<String> values = headers.get(name);
            if (null == values) {
                values = new LinkedList<String>();
                headers.put(name, values);
            }
            for (final Enumeration<?> valueEnum = req.getHeaders(name); valueEnum.hasMoreElements();) {
                values.add((String) valueEnum.nextElement());
            }

        }
        return headers;
    }
}
