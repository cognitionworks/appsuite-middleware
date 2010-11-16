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

import java.security.Principal;
import java.text.ParseException;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import com.openexchange.ajp13.AJPv13RequestHandler;
import com.openexchange.ajp13.exception.AJPv13Exception;
import com.openexchange.config.ConfigTools;
import com.openexchange.config.ConfigurationService;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.servlet.ServletConfigLoader;
import com.openexchange.tools.servlet.ServletRequestWrapper;

/**
 * HttpServletRequestWrapper
 * 
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class HttpServletRequestWrapper extends ServletRequestWrapper implements HttpServletRequest {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(HttpServletRequestWrapper.class);

    private String authType;

    private Cookie[] cookies;

    private String method;

    private String pathInfo;

    private String requestURL;

    private String requestURI;

    private String pathTranslated;

    private String servletPath;

    private String queryString;

    private String contextPath = "";

    private String remoteUser;

    private Principal userPrincipal;

    private boolean requestedSessionIdFromCookie = true;

    private boolean requestedSessionIdFromURL;

    private final AJPv13RequestHandler ajpRequestHandler;

    private HttpServlet servletInstance;

    /**
     * Initializes a new {@link HttpServletRequestWrapper}
     * 
     * @param ajpRequestHandler The AJP request handler
     * @throws AJPv13Exception If instantiation fails
     */
    public HttpServletRequestWrapper(final AJPv13RequestHandler ajpRequestHandler) throws AJPv13Exception {
        super();
        this.ajpRequestHandler = ajpRequestHandler;
    }

    public String getAuthType() {
        return authType;
    }

    public void setCookies(final Cookie[] cookies) {
        this.cookies = new Cookie[cookies.length];
        System.arraycopy(cookies, 0, this.cookies, 0, cookies.length);
    }

    public Cookie[] getCookies() {
        if (cookies == null) {
            return null;
        }
        final Cookie[] retval = new Cookie[cookies.length];
        System.arraycopy(cookies, 0, retval, 0, cookies.length);
        return retval;
    }

    public long getDateHeader(final String name) {
        return containsHeader(name) ? getDateValueFromHeaderField(getHeader(name)) : -1;
    }

    public int getIntHeader(final String name) {
        return containsHeader(name) ? Integer.parseInt(getHeader(name)) : -1;
    }

    public void setMethod(final String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

    public void setPathInfo(final String pathInfo) {
        this.pathInfo = pathInfo;
    }

    public String getPathInfo() {
        return pathInfo;
    }

    public void setPathTranslated(final String path_translated) {
        pathTranslated = path_translated;
    }

    public String getPathTranslated() {
        return pathTranslated;
    }

    public void setContextPath(final String contextPath) {
        this.contextPath = contextPath;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setQueryString(final String queryString) {
        this.queryString = queryString;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setRemoteUser(final String remoteUser) {
        this.remoteUser = remoteUser;
    }

    public String getRemoteUser() {
        return remoteUser;
    }

    public boolean isUserInRole(final String role) {
        if (LOG.isWarnEnabled()) {
            LOG.warn("Method isUserInRole() is not implemented in HttpServletRequestWrapper, yet!");
        }
        return false;
    }

    public java.security.Principal getUserPrincipal() {
        return userPrincipal;
    }

    public void setUserPrincipal(final Principal userPrincipal) {
        this.userPrincipal = userPrincipal;
    }

    public String getRequestedSessionId() {
        return ajpRequestHandler.getHttpSessionCookie().getValue();
    }

    public void setRequestURI(final String requestURI) {
        this.requestURI = requestURI;
    }

    public String getRequestURI() {
        return requestURI;
    }

    /**
     * Default port for HTTP.
     */
    private static final int PORT_HTTP = 80;

    /**
     * Default port for HTTPS.
     */
    private static final int PORT_HTTPS = 443;

    public StringBuffer getRequestURL() {
        if (null == requestURL) {
            final StringBuilder tmp = new StringBuilder(256);
            if (isSecure()) {
                tmp.append("https://").append(getServerName());
                final int port = getServerPort();
                if (port != PORT_HTTPS) {
                    tmp.append(':').append(port);
                }
            } else {
                tmp.append("http://").append(getServerName());
                final int port = getServerPort();
                if (port != PORT_HTTP) {
                    tmp.append(':').append(port);
                }
            }
            /*
             * Append request URI
             */
            if (null != requestURI) {
                if (requestURI.charAt(0) != '/') {
                    tmp.append('/');
                }
                tmp.append(requestURI);
            }
            requestURL = tmp.toString();
        }
        return new StringBuffer(requestURL);
    }

    public String getServletPath() {
        return servletPath;
    }

    public void setServletPath(final String servletPath) {
        this.servletPath = servletPath;
    }

    public HttpSession getSession(final boolean create) {
        HttpSessionWrapper session = null;
        /*
         * First look-up HttpSessionManagement if a session already exists
         */
        final Cookie sessionCookie = ajpRequestHandler.getHttpSessionCookie();
        final String httpSessionId = sessionCookie.getValue();
        final HttpSessionWrapper httpSession = HttpSessionManagement.getHttpSession(httpSessionId);
        if (httpSession != null) {
            if (!HttpSessionManagement.isHttpSessionExpired(httpSession)) {
                session = httpSession;
                session.setNew(false);
                session.setServletContext(getServletContext());
                /*
                 * Add JSESSIONID cookie
                 */
                configureCookie(sessionCookie);
                ajpRequestHandler.getServletResponse().addCookie(sessionCookie);
                return session;
            }
            /*
             * Invalidate session
             */
            httpSession.invalidate();
            HttpSessionManagement.removeHttpSession(httpSessionId);
        }
        /*
         * Create a new session
         */
        if (create) {
            /*
             * Create new session
             */
            session = ((HttpSessionWrapper) HttpSessionManagement.createAndGetHttpSession(httpSessionId));
            session.setNew(true);
            session.setServletContext(getServletContext());
            /*
             * Add JSESSIONID cookie
             */
            configureCookie(sessionCookie);
            ajpRequestHandler.getServletResponse().addCookie(sessionCookie);
        }
        return session;
    }

    private static final String DEFAULT_PATH = "/";

    private static void configureCookie(final Cookie sessionCookie) {
        sessionCookie.setPath(DEFAULT_PATH);
        final ConfigurationService configurationService = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
        /*
         * Check if auto-login is enabled
         */
        if (null != configurationService && configurationService.getBoolProperty("com.openexchange.sessiond.autologin", false)) {
            final int maxAge = (int) (ConfigTools.parseTimespan(configurationService.getProperty("com.openexchange.cookie.ttl", "1W")) / 1000);
            sessionCookie.setMaxAge(maxAge);
        }
    }

    public HttpSession getSession() {
        return getSession(true);
    }

    public boolean isRequestedSessionIdValid() {
        return !HttpSessionManagement.isHttpSessionExpired((HttpSessionWrapper) getSession());
    }

    public boolean isRequestedSessionIdFromCookie() {
        return requestedSessionIdFromCookie;
    }

    public void setRequestedSessionIdFromCookie(final boolean requestedSessionIdFromCookie) {
        this.requestedSessionIdFromCookie = requestedSessionIdFromCookie;
    }

    public boolean isRequestedSessionIdFromURL() {
        return requestedSessionIdFromURL;
    }

    public boolean isRequestedSessionIdFromUrl() {
        return requestedSessionIdFromURL;
    }

    public void setRequestedSessionIdFromURL(final boolean requestedSessionIdFromURL) {
        this.requestedSessionIdFromURL = requestedSessionIdFromURL;
    }

    public void setAuthType(final String authType) {
        this.authType = authType;
    }

    public void setServletInstance(final HttpServlet servletInstance) {
        this.servletInstance = servletInstance;
    }

    private static final long getDateValueFromHeaderField(final String headerValue) {
        try {
            return Tools.parseHeaderDate(headerValue).getTime();
        } catch (final ParseException e) {
            LOG.error(e.getMessage(), e);
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private final ServletContext getServletContext() {
        return ServletConfigLoader.getDefaultInstance().getContext(servletInstance.getClass().getCanonicalName(), servletPath);
    }

}
