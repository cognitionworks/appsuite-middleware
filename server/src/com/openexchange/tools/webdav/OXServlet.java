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

package com.openexchange.tools.webdav;

import static com.openexchange.tools.servlet.http.Tools.copyHeaders;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.JDOMException;
import com.openexchange.ajax.fields.Header;
import com.openexchange.authentication.LoginException;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.java.util.UUIDs;
import com.openexchange.login.Interface;
import com.openexchange.login.LoginRequest;
import com.openexchange.login.internal.LoginPerformer;
import com.openexchange.server.ServiceException;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.tools.servlet.http.Authorization.Credentials;
import com.openexchange.tools.webdav.digest.Authorization;
import com.openexchange.tools.webdav.digest.DigestUtility;
import com.openexchange.webdav.WebdavException;
import com.openexchange.xml.jdom.JDOMParser;

/**
 * This servlet can be used as super class for all OX webdav servlets.
 * 
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein </a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class OXServlet extends WebDavServlet {

    private static final long serialVersionUID = 301910346402779362L;

    private static final transient Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(OXServlet.class));

    /**
     * Simple {@link LoginRequest} implementation.
     */
    private static final class LoginRequestImpl implements LoginRequest {

        private final String login;

        private final HttpServletRequest req;

        private final String pass;

        private final Interface interfaze;

        public LoginRequestImpl(final String login, final String pass, final Interface interfaze, final HttpServletRequest req) {
            this.login = login;
            this.req = req;
            this.pass = pass;
            this.interfaze = interfaze;
        }

        public String getUserAgent() {
            return req.getHeader("user-agent");
        }

        public String getPassword() {
            return pass;
        }

        public String getLogin() {
            return login;
        }

        public Interface getInterface() {
            return interfaze;
        }

        public String getClientIP() {
            return req.getRemoteAddr();
        }

        public String getAuthId() {
            return UUIDs.getUnformattedString(UUID.randomUUID());
        }

        public String getClient() {
            return null;
        }

        public String getVersion() {
            return null;
        }

        public String getHash() {
            return null;
        }

        public Map<String, List<String>> getHeaders() {
            return copyHeaders(req);
        }
    }

    /**
     * Store the session object under this name in the request.
     */
    private static final String SESSION = OXServlet.class.getName() + "SESSION";

    /**
     * Authentication identifier.
     */
    private static final String basicRealm = "OX WebDAV";

    private static final String digestRealm = "Open-Xchange";

    protected static final String COOKIE_SESSIONID = "sessionid";

    /**
     * Digest type for authorization.
     */
    private static final String DIGEST_AUTH = "digest";

    private static final LoginPerformer loginPerformer = LoginPerformer.getInstance();

    protected OXServlet() {
        super();
    }

    /**
     * Defines if this servlet uses the HTTP Authorization header to identify the user. Return false to deactivate the use of the HTTP
     * Authorization header. Do the authorization with the extending class through the method
     * {@link #doAuth(HttpServletRequest, HttpServletResponse)}.
     */
    protected boolean useHttpAuth() {
        return true;
    }

    protected abstract Interface getInterface();

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        if (!"TRACE".equals(req.getMethod()) && useHttpAuth() && !doAuth(req, resp, getInterface(), getLoginCustomizer())) {
            return;
        }
        try {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Entering HTTP sub method. Session: " + getSession(req));
            }
            super.service(req, resp);
        } catch (final ServletException e) {
            throw e;
        } catch (final IOException e) {
            throw e;
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            final ServletException se = new ServletException(e.getMessage(), e);
            throw se;
        }
    }
    
    protected LoginCustomizer getLoginCustomizer() {
        return null;
    }

    public static boolean doAuth(final HttpServletRequest req, final HttpServletResponse resp, Interface face) throws IOException {
        return doAuth(req, resp, face, null);
    }
    
    /**
     * Performs authentication.
     * 
     * @param req The HTTP servlet request.
     * @param resp The HTTP servlet response.
     * @param face the used interface.
     * @return <code>true</code> if the authentication was successful; otherwise <code>false</code>.
     * @throws IOException If an I/O error occurs
     */
    public static boolean doAuth(final HttpServletRequest req, final HttpServletResponse resp, Interface face, LoginCustomizer customizer) throws IOException {
        Session session;
        try {
            session = findSessionByCookie(req, resp);
        } catch (final ServiceException e) {
            LOG.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return false;
        }
        if (null == session) {
            /*
             * No session found by cookie
             */
            LoginRequest loginRequest;
            try {
                loginRequest = parseLogin(req, face);
                if (customizer != null) {
                    loginRequest = customizer.modifyLogin(loginRequest);
                }
            } catch (final WebdavException e) {
                LOG.debug(e.getMessage(), e);
                addUnauthorizedHeader(req, resp);
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authorization Required!");
                return false;
            }
            try {
                session = addSession(loginRequest);
            } catch (final LoginException e) {
                if (e.getCategory() == Category.USER_INPUT) {
                    addUnauthorizedHeader(req, resp);
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authorization Required!");
                } else {
                    LOG.error(e.getMessage(), e);
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                }
                return false;
            }
            resp.addCookie(new Cookie(COOKIE_SESSIONID, session.getSessionID()));
        } else {
            /*
             * Session found by cookie
             */
            final String address = req.getRemoteAddr();
            if (null == address || !address.equals(session.getLocalIp())) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Request to server denied for session: " + session.getSessionID() + ". in WebDAV XML interface. Client login IP changed from " + session.getLocalIp() + " to " + address + ".");
                }
                addUnauthorizedHeader(req, resp);
                removeSession(session.getSessionID());
                removeCookie(req, resp, COOKIE_SESSIONID);
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authorization Required!");
                return false;
            }
        }
        req.setAttribute(SESSION, session);
        return true;
    }

    private static void removeCookie(HttpServletRequest req, HttpServletResponse resp, String...cookiesToRemove) {
        final Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return;
        }
        final List<String> cookieNames = Arrays.asList(cookiesToRemove);
        for (final Cookie cookie : cookies) {
            final String name = cookie.getName();

            for (final String string : cookieNames) {
                if (name.startsWith(string)) {
                    final Cookie respCookie = new Cookie(name, cookie.getValue());
                    respCookie.setPath("/");
                    respCookie.setMaxAge(0); // delete
                    resp.addCookie(respCookie);
                }
            }
        }
    }

    /**
     * @param sessionID
     */
    private static void removeSession(String sessionID) {
        try {
            ServerServiceRegistry.getInstance().getService(SessiondService.class, true).removeSession(sessionID);
        } catch (ServiceException e) {
            // Ignore. Probably we're just about to shut down.
        }
    }

    /**
     * Checks if the client sends a correct digest authorization header.
     * 
     * @param auth Authorization header.
     * @return <code>true</code> if the client sent a correct authorization header.
     */
    private static boolean checkForDigestAuthorization(final String auth) {
        if (null == auth) {
            return false;
        }
        if (auth.length() <= DIGEST_AUTH.length()) {
            return false;
        }
        if (!auth.substring(0, DIGEST_AUTH.length()).equalsIgnoreCase(DIGEST_AUTH)) {
            return false;
        }
        return true;
    }

    /**
     * Adds the header to the response message for authorization. Only add this header if the authorization of the user failed.
     * 
     * @param resp the response to that the header should be added.
     */
    protected static void addUnauthorizedHeader(final HttpServletRequest req, final HttpServletResponse resp) {
        final StringBuilder builder = new StringBuilder(64);
        builder.append("Basic realm=\"").append(basicRealm).append('"');
        resp.setHeader("WWW-Authenticate", builder.toString());
        /*-
         * Digest realm="testrealm@host.com",
         * qop="auth,auth-int",
         * nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093",
         * opaque="5ccc069c403ebaf9f0171e9517f40e41" 
         */
        builder.setLength(0);
        builder.append("Digest realm=\"").append(digestRealm).append('"').append(", ");
        builder.append("qop=\"auth,auth-int\"").append(", ");
        builder.append("nonce=\"").append(DigestUtility.getInstance().generateNOnce(req)).append('"').append(", ");
        final String opaque = UUIDs.getUnformattedString(UUID.randomUUID());
        builder.append("opaque=\"").append(opaque).append('"').append(", ");
        builder.append("stale=\"false\"").append(", ");
        builder.append("algorithm=\"MD5\"");
//        resp.addHeader("WWW-Authenticate", builder.toString());
    }

    private static LoginRequest parseLogin(final HttpServletRequest req, Interface face) throws WebdavException, IOException {
        final String auth = req.getHeader(Header.AUTH_HEADER);
        if (null == auth) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Authentication header missing.");
            }
            throw new WebdavException(WebdavException.Code.MISSING_HEADER_FIELD, "Authorization");
        }
        if (com.openexchange.tools.servlet.http.Authorization.checkForBasicAuthorization(auth)) {
            Credentials creds = com.openexchange.tools.servlet.http.Authorization.decode(auth);
            if (!com.openexchange.tools.servlet.http.Authorization.checkLogin(creds.getPassword())) {
                throw new WebdavException(WebdavException.Code.EMPTY_PASSWORD);
            }
            return new LoginRequestImpl(creds.getLogin(), creds.getPassword(), face, req);
        }
        if (checkForDigestAuthorization(auth)) {
            /*
             * Digest auth
             */
            final DigestUtility digestUtility = DigestUtility.getInstance();
            final Authorization authorization = digestUtility.parseDigestAuthorization(auth);
            /*
             * Determine user by "username"
             */
            final String userName = authorization.getUser();
            final String password = digestUtility.getPasswordByUserName(userName);
            if (!com.openexchange.tools.servlet.http.Authorization.checkLogin(password)) {
                throw new WebdavException(WebdavException.Code.UNSUPPORTED_AUTH_MECH, "Digest");
            }
            /*
             * Calculate MD5
             */
            final String serverDigest = digestUtility.generateServerDigest(req, password);
            /*
             * Compare to client "response"
             */
            if (!serverDigest.equals(authorization.getResponse())) {
                throw new WebdavException(WebdavException.Code.AUTH_FAILED, userName);
            }
            /*
             * Return appropriate login request to generate a session
             */
            return new LoginRequestImpl(userName, password, face, req);
        }
        /*
         * No known auth mechanism
         */
        final int pos = auth.indexOf(' ');
        final String mech = pos > 0 ? auth.substring(0, pos) : auth;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Unsupported Authentication header.");
        }
        throw new WebdavException(WebdavException.Code.UNSUPPORTED_AUTH_MECH, mech);
    }

    /**
     * This method tries to create a session for the given user.
     * 
     * @param login login name of the user.
     * @param pass plain text password of the user.
     * @param ipAddress client IP.
     * @return the initialized session or <code>null</code>.
     * @throws LoginException if an error occurs while creating the session.
     */
    private static Session addSession(final LoginRequest request) throws LoginException {
        return loginPerformer.doLogin(request).getSession();
    }

    private static Session findSessionByCookie(final HttpServletRequest req, final HttpServletResponse resp) throws ServiceException {
        final Cookie[] cookies = req.getCookies();
        String sessionId = null;
        if (null != cookies) {
            for (final Cookie cookie : cookies) {
                if (COOKIE_SESSIONID.equals(cookie.getName())) {
                    sessionId = cookie.getValue();
                    break;
                }
            }
        }
        if (null == sessionId) {
            return null;
        }
        final Session session = ServerServiceRegistry.getInstance().getService(SessiondService.class, true).getSession(sessionId);
        if (null == session && resp != null) {
            final Cookie cookie = new Cookie(COOKIE_SESSIONID, sessionId);
            cookie.setMaxAge(0);
            resp.addCookie(cookie);
        }
        return session;
    }

    public static Session getSession(final HttpServletRequest req) {
        final Session session = (Session) req.getAttribute(SESSION);
        if (null == session) {
            LOG.error("Somebody gets a null session.");
        }
        return session;
    }

    /**
     * Parses the xml request body and returns a JDOM document.
     * 
     * @param req the HttpServletRequest that body should be parsed.
     * @return a JDOM document of the parsed body.
     * @throws JDOMException if JDOM gets an exception
     * @throws IOException if an exception occurs while reading the body.
     */
    protected Document getJDOMDocument(final HttpServletRequest req) throws JDOMException, IOException {
        org.jdom.Document doc = null;
        if (req.getContentLength() > 0) {
            doc = ServerServiceRegistry.getInstance().getService(JDOMParser.class).parse(req.getInputStream());
        }
        return doc;
    }

}
