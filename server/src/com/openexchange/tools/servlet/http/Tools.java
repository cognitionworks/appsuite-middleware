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

package com.openexchange.tools.servlet.http;

import static com.openexchange.tools.TimeZoneUtils.getTimeZone;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.ajax.Login;
import com.openexchange.ajp13.AJPv13RequestHandler;

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
     * First Cache-Control value.
     */
    private static final String CACHE_VALUE1 = "no-store, no-cache, must-revalidate";

    /**
     * Second Cache-Control value.
     */
    private static final String CACHE_VALUE2 = "post-check=0, pre-check=0";

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
     * The magic spell to disable caching. Do not use these headers if response is directly written into servlet's output stream to initiate
     * a download.
     * 
     * @param resp the servlet response.
     * @see #removeCachingHeader(HttpServletResponse)
     */
    public static void disableCaching(final HttpServletResponse resp) {
        resp.addHeader(EXPIRES_KEY, EXPIRES_DATE);
        resp.addHeader(CACHE_CONTROL_KEY, CACHE_VALUE1);
        resp.addHeader(CACHE_CONTROL_KEY, CACHE_VALUE2);
        resp.addHeader(PRAGMA_KEY, PRAGMA_VALUE);
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

    private static final CookieNameMatcher OX_COOKIE_MATCHER = new CookieNameMatcher() {

        public boolean matches(final String cookieName) {
            return (null != cookieName && (cookieName.startsWith(Login.SESSION_PREFIX) || AJPv13RequestHandler.JSESSIONID_COOKIE.equals(cookieName)));
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
        public boolean matches(final String cookieName);
    }
}
