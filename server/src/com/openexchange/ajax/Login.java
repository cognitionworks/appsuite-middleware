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

package com.openexchange.ajax;

import static com.openexchange.login.Interface.HTTP_JSON;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.UUID;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.fields.Header;
import com.openexchange.ajax.fields.LoginFields;
import com.openexchange.ajax.helper.Send;
import com.openexchange.ajax.writer.LoginWriter;
import com.openexchange.ajax.writer.ResponseWriter;
import com.openexchange.ajp13.AJPv13RequestHandler;
import com.openexchange.authentication.LoginException;
import com.openexchange.authentication.LoginExceptionCodes;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.java.util.UUIDs;
import com.openexchange.login.Interface;
import com.openexchange.login.LoginRequest;
import com.openexchange.login.LoginResult;
import com.openexchange.login.internal.LoginPerformer;
import com.openexchange.server.ServiceException;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.sessiond.exception.SessiondException;
import com.openexchange.tools.servlet.AjaxException;
import com.openexchange.tools.servlet.OXJSONException;
import com.openexchange.tools.servlet.http.Tools;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * Servlet doing the login and logout stuff.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class Login extends AJAXServlet {

    private static final long serialVersionUID = 7680745138705836499L;

    private static final String PARAM_NAME = "name";

    private static final String PARAM_PASSWORD = "password";

    private static final String REDIRECT_URL = "/index.html#id=";

    public static final String COOKIE_PREFIX = "open-xchange-session-";

    private static final Log LOG = LogFactory.getLog(Login.class);

    private boolean checkIP = true;

    public Login() {
        super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        checkIP = Boolean.parseBoolean(config.getInitParameter(ServerConfig.Property.IP_CHECK.getPropertyName()));
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        final String action = req.getParameter(PARAMETER_ACTION);
        if (action == null) {
            logAndSendException(resp, new AjaxException(AjaxException.Code.MISSING_PARAMETER, PARAMETER_ACTION));
            return;
        }
        if (ACTION_LOGIN.equals(action)) {
            // Look-up necessary credentials
            try {
                doLogin(req, resp);
            } catch (AjaxException e) {
                logAndSendException(resp, e);
                return;
            }
        } else if (ACTION_LOGOUT.equals(action)) {
            // The magic spell to disable caching
            Tools.disableCaching(resp);
            final String cookieId = req.getParameter(PARAMETER_SESSION);
            if (cookieId == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            // Drop relevant cookies
            String sessionId = null;
            final Cookie[] cookies = req.getCookies();
            if (cookies != null) {
                final String cookieName = new StringBuilder(Login.COOKIE_PREFIX).append(cookieId).toString();
                int stat = 0;
                for (int a = 0; a < cookies.length && stat != 3; a++) {
                    if (cookieName.equals(cookies[a].getName())) {
                        sessionId = cookies[a].getValue();
                        final Cookie respCookie = new Cookie(cookieName, sessionId);
                        respCookie.setPath("/");
                        respCookie.setMaxAge(0); // delete
                        resp.addCookie(respCookie);
                        stat |= 1;
                    } else if (AJPv13RequestHandler.JSESSIONID_COOKIE.equals(cookies[a].getName())) {
                        final Cookie jsessionIdCookie = new Cookie(AJPv13RequestHandler.JSESSIONID_COOKIE, cookies[a].getValue());
                        jsessionIdCookie.setPath("/");
                        jsessionIdCookie.setMaxAge(0); // delete
                        resp.addCookie(jsessionIdCookie);
                        stat |= 2;
                    }
                }
            }
            if (sessionId == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("no session cookie found in request!");
                }
            } else {
                // Do logout
                try {
                    LoginPerformer.getInstance().doLogout(sessionId);
                } catch (final LoginException e) {
                    LOG.error("Logout failed", e);
                }
            }
        } else if (ACTION_REDIRECT.equals(action)) {
            // The magic spell to disable caching
            Tools.disableCaching(resp);
            final String randomToken = req.getParameter(LoginFields.PARAM_RANDOM);
            if (randomToken == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            final SessiondService sessiondService = ServerServiceRegistry.getInstance().getService(SessiondService.class);
            if (sessiondService == null) {
                final ServiceException se = new ServiceException(ServiceException.Code.SERVICE_UNAVAILABLE, SessiondService.class.getName());
                LOG.error(se.getMessage(), se);
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            final Session session = sessiondService.getSessionByRandomToken(randomToken, req.getRemoteAddr());
            if (session == null) {
                // Unknown random token; throw error
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No session could be found for random token: " + randomToken, new Throwable());
                } else if (LOG.isInfoEnabled()) {
                    LOG.info("No session could be found for random token: " + randomToken);
                }
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            try {
                final Context context = ContextStorage.getInstance().getContext(session.getContextId());
                final User user = UserStorage.getInstance().getUser(session.getUserId(), context);
                if (!context.isEnabled() || !user.isMailEnabled()) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                }
            } catch (final UndeclaredThrowableException e) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch (final ContextException e) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch (final LdapException e) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
            writeCookie(resp, session);
            resp.sendRedirect(REDIRECT_URL + session.getSecret());
        } else if (ACTION_AUTOLOGIN.equals(action)) {
            final Cookie[] cookies = req.getCookies();
            final Response response = new Response();
            try {
                if (cookies == null) {
                    throw new OXJSONException(OXJSONException.Code.INVALID_COOKIE);
                }
                final SessiondService sessiondService = ServerServiceRegistry.getInstance().getService(SessiondService.class);
                for (final Cookie cookie : cookies) {
                    final String cookieName = cookie.getName();
                    if (cookieName.startsWith(COOKIE_PREFIX)) {
                        final String sessionId = cookie.getValue();
                        if (sessiondService.refreshSession(sessionId)) {
                            final Session session = sessiondService.getSession(sessionId);
                            SessionServlet.checkIP(checkIP, session, req.getRemoteAddr());
                            try {
                                final Context ctx = ContextStorage.getInstance().getContext(session.getContextId());
                                final User user = UserStorage.getInstance().getUser(session.getUserId(), ctx);
                                if (!ctx.isEnabled() || !user.isMailEnabled()) {
                                    throw LoginExceptionCodes.INVALID_CREDENTIALS.create();
                                }
                            } catch (final UndeclaredThrowableException e) {
                                throw LoginExceptionCodes.UNKNOWN.create(e);
                            }
                            JSONObject json = new JSONObject();
                            new LoginWriter().writeLogin(session, json);
                            response.setData(json);
                            break;
                        }
                        // Not found session. Delete old OX cookie.
                        final Cookie respCookie = new Cookie(cookie.getName(), cookie.getValue());
                        respCookie.setPath("/");
                        respCookie.setMaxAge(0); // delete
                        resp.addCookie(respCookie);
                    }
                }
                if (null == response.getData()) {
                    throw new OXJSONException(OXJSONException.Code.INVALID_COOKIE);
                }
            } catch (final SessiondException e) {
                LOG.debug(e.getMessage(), e);
                response.setException(e);
            } catch (final OXJSONException e) {
                LOG.debug(e.getMessage(), e);
                response.setException(e);
            } catch (final JSONException e) {
                final OXJSONException oje = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e);
                LOG.error(oje.getMessage(), oje);
                response.setException(oje);
            } catch (final ContextException e) {
                LOG.debug(e.getMessage(), e);
                response.setException(e);
            } catch (final LdapException e) {
                LOG.debug(e.getMessage(), e);
                response.setException(e);
            } catch (final LoginException e) {
                if (AbstractOXException.Category.USER_INPUT == e.getCategory()) {
                    LOG.debug(e.getMessage(), e);
                } else {
                    LOG.error(e.getMessage(), e);
                }
                response.setException(e);
            }
            // The magic spell to disable caching
            Tools.disableCaching(resp);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType(CONTENTTYPE_JAVASCRIPT);
            try {
                if (response.hasError()) {
                    ResponseWriter.write(response, resp.getWriter());
                } else {
                    ((JSONObject) response.getData()).write(resp.getWriter());
                }
            } catch (final JSONException e) {
                log(RESPONSE_ERROR, e);
                sendError(resp);
            }
        } else {
            logAndSendException(resp, new AjaxException(AjaxException.Code.UnknownAction, action));
        }
    }

    private void logAndSendException(final HttpServletResponse resp, final AjaxException e) throws IOException {
        LOG.debug(e.getMessage(), e);
        final Response response = new Response();
        response.setException(e);
        Send.sendResponse(response, resp);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

    /**
     * Writes the (groupware's) session cookie to specified HTTP servlet response whose name is composed by cookie prefix
     * <code>"open-xchange-session-"</code> and a secret cookie identifier.
     * 
     * @param resp The HTTP servlet response
     * @param session The session providing the secret cookie identifier
     */
    protected static void writeCookie(final HttpServletResponse resp, final Session session) {
        final Cookie cookie = new Cookie(COOKIE_PREFIX + session.getSecret(), session.getSessionID());
        cookie.setPath("/");
        resp.addCookie(cookie);
    }

    private void doLogin(HttpServletRequest req, HttpServletResponse resp) throws AjaxException, IOException {
        final LoginRequest request = parseLogin(req);
        // Perform the login
        final Response response = new Response();
        LoginResult result = null;
        try {
            result = LoginPerformer.getInstance().doLogin(request);
            // Write response
            JSONObject json = new JSONObject();
            new LoginWriter().writeLogin(result.getSession(), json);
            response.setData(json);
        } catch (final LoginException e) {
            if (AbstractOXException.Category.USER_INPUT == e.getCategory()) {
                LOG.debug(e.getMessage(), e);
            } else {
                LOG.error(e.getMessage(), e);
            }
            response.setException(e);
        } catch (final JSONException e) {
            final OXJSONException oje = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e);
            LOG.error(oje.getMessage(), oje);
            response.setException(oje);
        }
        // The magic spell to disable caching
        Tools.disableCaching(resp);
        resp.setContentType(CONTENTTYPE_JAVASCRIPT);
        try {
            if (response.hasError() || null == result) {
                ResponseWriter.write(response, resp.getWriter());
            } else {
                final Session session = result.getSession();
                // Store associated session
                SessionServlet.rememberSession(req, new ServerSessionAdapter(session, result.getContext(), result.getUser()));
                writeCookie(resp, session);
                // Login response is unfortunately not conform to default responses.
                ((JSONObject) response.getData()).write(resp.getWriter());
            }
        } catch (final JSONException e) {
            if (e.getCause() instanceof IOException) {
                // Throw proper I/O error since a serious socket error could been occurred which prevents further communication. Just
                // throwing a JSON error possibly hides this fact by trying to write to/read from a broken socket connection.
                throw (IOException) e.getCause();
            }
            LOG.error(RESPONSE_ERROR, e);
            sendError(resp);
        }
    }

    private LoginRequest parseLogin(final HttpServletRequest req) throws AjaxException {
        final String login = req.getParameter(PARAM_NAME);
        if (null == login) {
            throw new AjaxException(AjaxException.Code.MISSING_PARAMETER, PARAM_NAME);
        }
        final String password = req.getParameter(PARAM_PASSWORD);
        if (null == password) {
            throw new AjaxException(AjaxException.Code.MISSING_PARAMETER, PARAM_PASSWORD);
        }
        final String authId = null == req.getParameter(LoginFields.AUTHID_PARAM) ? UUIDs.getUnformattedString(UUID.randomUUID()) : req.getParameter(LoginFields.AUTHID_PARAM);
        LoginRequest loginRequest = new LoginRequest() {
            public String getLogin() {
                return login;
            }
            public String getPassword() {
                return password;
            }
            public String getClientIP() {
                return req.getRemoteAddr();
            }
            public String getUserAgent() {
                return req.getHeader(Header.USER_AGENT);
            }
            public String getAuthId() {
                return authId;
            }
            public String getClient() {
                return req.getParameter(LoginFields.CLIENT_PARAM);
            }
            public String getVersion() {
                return req.getParameter(LoginFields.VERSION_PARAM);
            }
            public Interface getInterface() {
                return HTTP_JSON;
            }
        };
        return loginRequest;
    }
}
