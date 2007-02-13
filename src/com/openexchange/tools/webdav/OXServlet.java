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



package com.openexchange.tools.webdav;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.openexchange.groupware.contexts.ContextNotFoundException;
import com.openexchange.sessiond.InvalidCredentialsException;
import com.openexchange.sessiond.LoginException;
import com.openexchange.sessiond.MaxSessionLimitException;
import com.openexchange.sessiond.PasswordExpiredException;
import com.openexchange.sessiond.SessionObject;
import com.openexchange.sessiond.SessiondConnector;
import com.openexchange.sessiond.SessiondException;
import com.openexchange.sessiond.UserNotActivatedException;
import com.openexchange.sessiond.UserNotFoundException;
import com.openexchange.tools.StringCollection;

/**
 * This servlet can be used as super class for all OX webdav servlets.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein </a>
 */
public abstract class OXServlet extends WebDavServlet {

    /**
     * Logger.
     */
    private static final transient Log LOG = LogFactory.getLog(OXServlet.class);

    /**
     * Store the session object under this name in the request.
     */
    private static final String SESSION = OXServlet.class.getName() + "SESSION";

    /**
     * Authentication identifier.
     */
    private static String authIdentifier = "OX WebDAV";

    /**
     * Defines if this servlet uses the HTTP Authorization header to identify
     * the user. Set it to false to deactivate the use of the HTTP Authorization
     * header. Do the authorization with the extending class through the method
     * initializeService() and don't forget to cleanup after your service with
     * the methode cleanupService().
     */
    protected transient boolean httpAuth = true;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void service(final HttpServletRequest req,
        final HttpServletResponse resp) throws ServletException, IOException {
        if (!"TRACE".equals(req.getMethod()) && httpAuth
            && !doAuth(req, resp)) {
            return;
        }
        try {
            LOG.trace("Entering HTTP sub method. Session: " + getSession(req));
            super.service(req, resp);
        } catch (RuntimeException e) {
            throw new ServletException(e.getMessage(), e);
        } catch (ServletException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e.getMessage(), e);
        }
    }

    /**
     * Does the whole authentication mechanism.
     * @param req http servlet request.
     * @param resp http servlet response.
     * @return <code>true</code> if the authentication can be done correctly.
     * @throws IOException if a communication problem occurs.
     * @throws ServletException if adding a session fails.
     */
    protected boolean doAuth(final HttpServletRequest req,
        final HttpServletResponse resp) throws IOException, ServletException {
        final String sessionId = getSessionId(req);
        SessionObject session = null;
        if (null != sessionId) {
            session = getSession(sessionId);
        }
        if (null == sessionId || null == session) {
            LOG.trace("No sessionId cookie found.");
            final String auth = req.getHeader(AUTH_HEADER);
            if (!checkForBasicAuthorization(auth)) {
                LOG.debug("Authentication header missing.");
                addUnauthorizedHeader(resp);
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Authorization Required!");
                return false;
            }
            LOG.trace("Authorization header found.");
            final String[] userpass = OXServlet.decodeAuthorization(auth);
            final String login = userpass[0].toLowerCase();
            final String pass = userpass[1];
            if (!checkLogin(login, pass, req.getRemoteAddr())) {
                addUnauthorizedHeader(resp);
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Authorization Required!");
                return false;
            }
            try {
                session = addSession(login, pass, req.getRemoteAddr());
            } catch (LoginException e) {
                LOG.error(e.getMessage(), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage());
                return false;
            }
            if (null == session) {
                LOG.debug("Can't authenticate user.");
                addUnauthorizedHeader(resp);
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Authorization Required!");
                return false;
            }
            LOG.trace("Session created.");
            resp.addCookie(new Cookie("sessionid", session.getSessionID()));
            if (null != sessionId) {
                final Cookie cookie = new Cookie("sessionid", sessionId);
                cookie.setMaxAge(0);
                resp.addCookie(cookie);
            }
        }
        req.setAttribute(SESSION, session);
        return true;
    }

    /**
     * This methods reads the session identifier from the cookies.
     * TODO Fix handling if request contains old session identifier.
     * @param req http request.
     * @return the session identifier or <code>null</code> if it can't be found.
     */
    private static String getSessionId(final HttpServletRequest req) {
        final Cookie[] cookies = req.getCookies();
        String sessionId = null;
        if (null != cookies) {
            for (int i = 0; i < cookies.length; i++) {
                if ("sessionid".equals(cookies[i].getName())) {
                    sessionId = cookies[i].getValue();
                    break;
                }
            }
        }
        return sessionId;
    }

    /**
     * Checks if the client sends a correct basic authorization header.
     * @param auth Authorization header.
     * @return <code>true</code> if the client sent a correct authorization
     * header.
     */
    private static boolean checkForBasicAuthorization(final String auth) {
        if (null == auth) {
            return false;
        }
        if (auth.length() <= BASIC_AUTH.length()) {
            return false;
        }
        if (!auth.substring(0, BASIC_AUTH.length())
            .equalsIgnoreCase(BASIC_AUTH)) {
            return false;
        }
        return true;
    }

    /**
     * Decodes the base64 encoded authorization header. The leading basic will
     * be removed if it is present.
     * @param auth the base64 encoded value of the authorization header
     * @return a string array with user and password
     * @throws IOException if the base64 can't be decoded
     */
    protected static String[] decodeAuthorization(final String auth)
        throws IOException {
        if (!checkForBasicAuthorization(auth)) {
            throw new IOException(
                "Authorization header is missing the leading \"basic\"!");
        }
        final byte[] decoded = new sun.misc.BASE64Decoder().decodeBuffer(auth
            .substring(6));
        final String userpass = new String(decoded, "UTF-8").trim();
        final int delimiter = userpass.indexOf(':');
        String user = "";
        String pass = "";
        if (-1 != delimiter) {
            user = userpass.substring(0, delimiter);
            pass = userpass.substring(delimiter + 1);
        }
        return new String[] { user, pass };
    }

    /**
     * Checks if the login contains only valid values.
     * @param login login name of the user
     * @param pass password of the user
     * @param ipAddress Client IP.
     * @return false if the login contains illegal values.
     */
    protected static boolean checkLogin(final String login,
        final String pass, final String ipAddress) {
        // made by stefan - !!!login without password bugfix - moped!!!
        // check if the user wants to login without password.
        // ldap bind doesn't fail with empty password. so check it here.
        if (pass == null || StringCollection.isEmpty(pass)) {
            return false;
        }
        return true;
    }

    /**
     * Adds the header to the response message for authorization. Only add this
     * header if the authorization of the user failed.
     * @param resp the response to that the header should be added.
     */
    protected static void addUnauthorizedHeader(
        final HttpServletResponse resp) {
        resp.setHeader("WWW-Authenticate", "Basic realm=\"" + authIdentifier
            + "\"");
    }

    /**
     * This method tries to create a session for the given user.
     * @param login login name of the user.
     * @param pass plain text password of the user.
     * @param ipAddress client IP.
     * @return the initilized session or <code>null</code>.
     * @throws LoginException if an error occurs while creating the session.
     */
    private SessionObject addSession(final String login, final String pass,
        final String ipAddress) throws LoginException {
        final SessiondConnector connector = SessiondConnector.getInstance();
        SessionObject session = null;
        try {
            if (true) {
                throw new SessiondException("blah", null);
            }
            session = connector.addSession(login, pass, ipAddress);
        } catch (InvalidCredentialsException e) {
            log(e.getMessage(), e);
        } catch (UserNotFoundException e) {
            log(e.getMessage(), e);
        } catch (UserNotActivatedException e) {
            log(e.getMessage(), e);
        } catch (PasswordExpiredException e) {
            log(e.getMessage(), e);
        } catch (ContextNotFoundException e) {
            log(e.getMessage(), e);
        } catch (LoginException e) {
            if (LoginException.Source.SYSTEM == e.getSource()) {
                throw e;
            }
            log(e.getMessage(), e);
        } catch (SessiondException e) {
            throw new LoginException(LoginException.Code.DATABASE_DOWN, e);
        } catch (MaxSessionLimitException e) {
            throw new LoginException(LoginException.Code.DATABASE_DOWN, e);
        }
        return session;
    }

    /**
     * This method tries to get the session for the given session identifier.
     * @param sessionId session identifier.
     * @return the session object or <code>null</code> if the session doesn't
     * exist.
     */
    private SessionObject getSession(final String sessionId) {
        final SessiondConnector connector = SessiondConnector.getInstance();
        return connector.getSession(sessionId);
    }

    /**
     * @param req Request.
     * @return the session object.
     */
    protected SessionObject getSession(final HttpServletRequest req) {
        final SessionObject session = (SessionObject) req
            .getAttribute(SESSION);
        if (null == session) {
            LOG.error("Somebody gets a null session.");
        }
        return session;
    }

    /**
     * Parses the xml request body and returns a JDOM document.
     * @param req the HttpServletRequest that body should be parsed.
     * @return a JDOM document of the parsed body.
     * @throws JDOMException if JDOM gets an exception
     * @throws IOException if an exception occurs while reading the body.
     */
    protected org.jdom.Document getJDOMDocument(final HttpServletRequest req)
        throws JDOMException, IOException {
        org.jdom.Document doc = null;
        if (req.getContentLength() > 0) {
            doc = new SAXBuilder().build(req.getInputStream());
        }
        return doc;
    }

    /**
     * Name of the header containing the authorization data.
     */
    private static final String AUTH_HEADER = "authorization";

    /**
     * Basic type for authorization.
     */
    private static final String BASIC_AUTH = "basic";

}
