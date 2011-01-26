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

package com.openexchange.ajp13.xajp.http;

import java.security.Principal;
import java.text.ParseException;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import com.openexchange.ajp13.exception.AJPv13Exception;
import com.openexchange.ajp13.servlet.ServletConfigLoader;
import com.openexchange.ajp13.servlet.http.HttpSessionManagement;
import com.openexchange.ajp13.servlet.http.HttpSessionWrapper;
import com.openexchange.ajp13.xajp.XAJPv13Session;
import com.openexchange.tools.servlet.http.Tools;

/**
 * {@link HttpServletRequestWrapper}
 * 
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

    private HttpSessionWrapper session;

    private boolean requestedSessionIdFromCookie = true;

    private boolean requestedSessionIdFromURL;

    private final XAJPv13Session ajpSession;

    private HttpServlet servletInstance;

    /**
     * Initializes a new {@link HttpServletRequestWrapper}
     * 
     * @param ajpSession The AJP session
     * @throws AJPv13Exception If instantiation fails
     */
    public HttpServletRequestWrapper(final XAJPv13Session ajpSession) throws AJPv13Exception {
        super();
        this.ajpSession = ajpSession;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#getAuthType()
     */
    public String getAuthType() {
        return authType;
    }

    public void setCookies(final Cookie[] cookies) {
        this.cookies = new Cookie[cookies.length];
        System.arraycopy(cookies, 0, this.cookies, 0, cookies.length);
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#getCookies()
     */
    public Cookie[] getCookies() {
        if (cookies == null) {
            return null;
        }
        final Cookie[] retval = new Cookie[cookies.length];
        System.arraycopy(cookies, 0, retval, 0, cookies.length);
        return retval;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#getDateHeader(java.lang.String)
     */
    public long getDateHeader(final String name) {
        return containsHeader(name) ? getDateValueFromHeaderField(getHeader(name)) : -1;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#getIntHeader(java.lang.String)
     */
    public int getIntHeader(final String name) {
        return containsHeader(name) ? Integer.parseInt(getHeader(name)) : -1;
    }

    public void setMethod(final String method) {
        this.method = method;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#getMethod()
     */
    public String getMethod() {
        return method;
    }

    public void setPathInfo(final String path_info) {
        pathInfo = path_info;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#getPathInfo()
     */
    public String getPathInfo() {
        return pathInfo;
    }

    public void setPathTranslated(final String path_translated) {
        pathTranslated = path_translated;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#getPathTranslated()
     */
    public String getPathTranslated() {
        return pathTranslated;
    }

    public void setContextPath(final String context_path) {
        contextPath = context_path;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#getContextPath()
     */
    public String getContextPath() {
        return contextPath;
    }

    public void setQueryString(final String query_string) {
        queryString = query_string;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#getQueryString()
     */
    public String getQueryString() {
        return queryString;
    }

    public void setRemoteUser(final String remote_user) {
        remoteUser = remote_user;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
     */
    public String getRemoteUser() {
        return remoteUser;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#isUserInRole(java.lang.String)
     */
    public boolean isUserInRole(final String role) {
        if (LOG.isWarnEnabled()) {
            LOG.warn("Method isUserInRole() is not implemented in HttpServletRequestWrapper, yet!");
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
     */
    public java.security.Principal getUserPrincipal() {
        return userPrincipal;
    }

    public void setUserPrincipal(final Principal userPrincipal) {
        this.userPrincipal = userPrincipal;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#getRequestedSessionId()
     */
    public String getRequestedSessionId() {
        return session.getId();
    }

    public void setRequestURI(final String request_uri) {
        requestURI = request_uri;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#getRequestURI()
     */
    public String getRequestURI() {
        return requestURI;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#getRequestURL()
     */
    public StringBuffer getRequestURL() {
        if (null == requestURL) {
            final StringBuilder tmp = new StringBuilder(256);
            tmp.append(getProtocol());
            if (isSecure()) {
                tmp.append('s');
            }
            tmp.append("://").append(getServerName());
            if (requestURI.charAt(0) != '/') {
                tmp.append('/');
            }
            tmp.append(getRequestURI());
            requestURL = tmp.toString();
        }
        return new StringBuffer(requestURL);
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#getServletPath()
     */
    public String getServletPath() {
        return servletPath;
    }

    public void setServletPath(final String servlet_path) {
        servletPath = servlet_path;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#getSession(boolean)
     */
    public HttpSession getSession(final boolean create) {
        if (session != null) {
            return session;
        }
        /*
         * First look-up HttpSessionManagement if a session already exists
         */
        final String id = ajpSession.getHttpSessionId();
        final HttpSessionWrapper httpSession = HttpSessionManagement.getHttpSession(id);
        if (httpSession != null) {
            if (!HttpSessionManagement.isHttpSessionExpired(httpSession)) {
                session = httpSession;
                session.setNew(false);
                session.setServletContext(getServletContext());
                return session;
            }
            /*
             * Invalidate session
             */
            httpSession.invalidate();
            HttpSessionManagement.removeHttpSession(id);
        }
        /*
         * Create a new session
         */
        if (create) {
            /*
             * Create new session
             */
            session = (HttpSessionWrapper) HttpSessionManagement.createAndGetHttpSession(id);
            session.setNew(true);
            session.setServletContext(getServletContext());
        }
        return session;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#getSession()
     */
    public HttpSession getSession() {
        return getSession(true);
    }

    public void setSession(final HttpSession session) {
        this.session = (HttpSessionWrapper) session;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdValid()
     */
    public boolean isRequestedSessionIdValid() {
        return !HttpSessionManagement.isHttpSessionExpired(session);
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromCookie()
     */
    public boolean isRequestedSessionIdFromCookie() {
        return requestedSessionIdFromCookie;
    }

    public void setRequestedSessionIdFromCookie(final boolean requestedSessionIdFromCookie) {
        this.requestedSessionIdFromCookie = requestedSessionIdFromCookie;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromURL()
     */
    public boolean isRequestedSessionIdFromURL() {
        return requestedSessionIdFromURL;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromUrl()
     */
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
