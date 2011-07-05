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

package com.openexchange.login.internal;

import static com.openexchange.java.Autoboxing.I;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import javax.security.auth.login.LoginException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.authentication.Authenticated;
import com.openexchange.authentication.LoginExceptionCodes;
import com.openexchange.authentication.service.Authentication;
import com.openexchange.authorization.Authorization;
import com.openexchange.authorization.AuthorizationException;
import com.openexchange.authorization.AuthorizationService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextExceptionCodes;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserException;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.java.Strings;
import com.openexchange.login.LoginHandlerService;
import com.openexchange.login.LoginRequest;
import com.openexchange.login.LoginResult;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondException;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.behavior.CallerRunsBehavior;

/**
 * {@link LoginPerformer} - Performs a login for specified credentials.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class LoginPerformer {

    private static final Log LOG = com.openexchange.exception.Log.valueOf(LogFactory.getLog(LoginPerformer.class));

    private static final LoginPerformer SINGLETON = new LoginPerformer();

    public static LoginPerformer getInstance() {
        return SINGLETON;
    }

    /**
     * Initializes a new {@link LoginPerformer}.
     */
    private LoginPerformer() {
        super();
    }

    /**
     * Performs the login for specified login request.
     * 
     * @param request The login request
     * @return The login providing login information
     * @throws LoginException If login fails
     */
    public LoginResult doLogin(final LoginRequest request) throws LoginException {
        return doLogin(request, Collections.<String, Object> emptyMap());
    }

    /**
     * Performs the login for specified login request.
     * 
     * @param request The login request
     * @return The login providing login information
     * @throws LoginException If login fails
     */
    public LoginResult doLogin(final LoginRequest request, final Map<String, Object> properties) throws LoginException {
        final LoginResultImpl retval = new LoginResultImpl();
        retval.setRequest(request);
        try {
            final Authenticated authed = Authentication.login(request.getLogin(), request.getPassword(), properties);
            final Context ctx = findContext(authed.getContextInfo());
            retval.setContext(ctx);
            final String username = authed.getUserInfo();
            final User user = findUser(ctx, username);
            retval.setUser(user);
            // Checks if something is deactivated.
            final AuthorizationService authService = Authorization.getService();
            if (null == authService) {
                // FIXME: what todo??
                final OXException e = new OXException(ServiceExceptionCode.SERVICE_INITIALIZATION_FAILED);
                LOG.error("unable to find AuthorizationService", e);
                throw e;
            }
            authService.authorizeUser(ctx, user);
            // Check if indicated client is allowed to perform a login
            checkClient(request, user, ctx);
            // Create session
            final SessiondService sessiondService = ServerServiceRegistry.getInstance().getService(SessiondService.class, true);
            final String sessionId = sessiondService.addSession(new AddSessionParameterImpl(username, request, user, ctx));
            retval.setSession(sessiondService.getSession(sessionId));
            // Trigger registered login handlers
            triggerLoginHandlers(retval);
        } catch (final OXException e) {
            logLoginRequest(request, retval);
            throw LoginExceptionCodes.COMMUNICATION.create(e);
        } catch (final LoginException e) {
            logLoginRequest(request, retval);
            throw e;
        } catch (final OXException e) {
            logLoginRequest(request, retval);
            throw new LoginException(e);
        } catch (final UserException e) {
            logLoginRequest(request, retval);
            throw new LoginException(e);
        } catch (final SessiondException e) {
            logLoginRequest(request, retval);
            throw new LoginException(e);
        } catch (final AuthorizationException e) {
            logLoginRequest(request, retval);
            throw new LoginException(e);
        }
        logLoginRequest(request, retval);
        return retval;
    }

    private void checkClient(final LoginRequest request, final User user, final Context ctx) throws LoginException {
        try {
            final String client = request.getClient();
            /*
             * Check for OLOX v2.0
             */
            if ("USM-JSON".equalsIgnoreCase(client)) {
                final UserConfigurationStorage ucs = UserConfigurationStorage.getInstance();
                final UserConfiguration userConfiguration = ucs.getUserConfiguration(user.getId(), user.getGroups(), ctx);
                if (!userConfiguration.hasOLOX20()) {
                    /*
                     * Deny login for OLOX v2.0 client since disabled as per user configuration
                     */
                    throw LoginExceptionCodes.CLIENT_DENIED.create(client);
                }
            }
        } catch (final LoginException e) {
            throw e;
        } catch (final AbstractOXException e) {
            throw new LoginException(e);
        }
    }

    private Context findContext(final String contextInfo) throws OXException {
        final ContextStorage contextStor = ContextStorage.getInstance();
        final int contextId = contextStor.getContextId(contextInfo);
        if (ContextStorage.NOT_FOUND == contextId) {
            throw ContextExceptionCodes.NO_MAPPING.create(contextInfo);
        }
        final Context context = contextStor.getContext(contextId);
        if (null == context) {
            throw ContextExceptionCodes.NOT_FOUND.create(I(contextId));
        }
        return context;
    }

    private User findUser(final Context ctx, final String userInfo) throws UserException {
        final String proxyDelimiter = MailProperties.getInstance().getAuthProxyDelimiter();
        final UserStorage us = UserStorage.getInstance();
        final User u;
        try {
            int userId = 0;
            if( proxyDelimiter != null && userInfo.contains(proxyDelimiter)) {
                userId = us.getUserId(userInfo.substring(userInfo.indexOf(proxyDelimiter)+proxyDelimiter.length(), userInfo.length()), ctx);
            } else {
                userId = us.getUserId(userInfo, ctx);
            }
            u = us.getUser(userId, ctx);
        } catch (final LdapException e) {
            throw new UserException(e);
        }
        return u;
    }

    /**
     * Performs the logout for specified session ID.
     * 
     * @param sessionId The session ID
     * @throws OXException If logout fails
     */
    public Session doLogout(final String sessionId) throws OXException {
        // Drop the session
        final SessiondService sessiondService;
        try {
            sessiondService = ServerServiceRegistry.getInstance().getService(SessiondService.class, true);
        } catch (final OXException e) {
            throw LoginExceptionCodes.COMMUNICATION.create(e);
        }
        final Session session = sessiondService.getSession(sessionId);
        if (null == session) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No session found for ID: " + sessionId);
            }
            return null;
        }
        // Get context
        final ContextStorage contextStor = ContextStorage.getInstance();
        final Context context;
        context = contextStor.getContext(session.getContextId());
        if (null == context) {
            throw ContextExceptionCodes.NOT_FOUND.create(Integer.valueOf(session.getContextId()));
        }
        // Get user
        final User u;
        try {
            final UserStorage us = UserStorage.getInstance();
            u = us.getUser(session.getUserId(), context);
        } catch (final LdapException e) {
            throw new LoginException(e);
        }
        final LoginResultImpl logout = new LoginResultImpl(session, context, u);
        // Remove session
        sessiondService.removeSession(sessionId);
        logLogout(logout);
        // Trigger registered logout handlers
        triggerLogoutHandlers(logout);
        return session;
    }

    private static void triggerLoginHandlers(final LoginResult login) {
        final ThreadPoolService executor = ServerServiceRegistry.getInstance().getService(ThreadPoolService.class);
        if (null == executor) {
            for (final Iterator<LoginHandlerService> it = LoginHandlerRegistry.getInstance().getLoginHandlers(); it.hasNext();) {
                try {
                    it.next().handleLogin(login);
                } catch (final LoginException e) {
                    logError(e);
                }
            }
        } else {
            for (final Iterator<LoginHandlerService> it = LoginHandlerRegistry.getInstance().getLoginHandlers(); it.hasNext();) {
                final LoginHandlerService handler = it.next();
                executor.submit(new LoginPerformerTask() {

                    public Object call() {
                        try {
                            handler.handleLogin(login);
                        } catch (final LoginException e) {
                            logError(e);
                        }
                        return null;
                    }
                }, CallerRunsBehavior.getInstance());
            }
        }
    }

    private static void triggerLogoutHandlers(final LoginResult logout) {
        final ThreadPoolService executor = ServerServiceRegistry.getInstance().getService(ThreadPoolService.class);
        if (null == executor) {
            for (final Iterator<LoginHandlerService> it = LoginHandlerRegistry.getInstance().getLoginHandlers(); it.hasNext();) {
                try {
                    it.next().handleLogout(logout);
                } catch (final LoginException e) {
                    logError(e);
                }
            }
        } else {
            for (final Iterator<LoginHandlerService> it = LoginHandlerRegistry.getInstance().getLoginHandlers(); it.hasNext();) {
                final LoginHandlerService handler = it.next();
                executor.submit(new LoginPerformerTask() {
                    public Object call() {
                        try {
                            handler.handleLogout(logout);
                        } catch (final LoginException e) {
                            logError(e);
                        }
                        return null;
                    }
                }, CallerRunsBehavior.getInstance());
            }
        }
    }

    static void logError(final LoginException e) {
        switch (e.getCategory()) {
        case USER_INPUT:
            LOG.debug(e.getMessage(), e);
            break;
        default:
            LOG.error(e.getMessage(), e);
        }
    }

    private static void logLoginRequest(final LoginRequest request, final LoginResult result) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Login:");
        sb.append(request.getLogin());
        sb.append(" IP:");
        sb.append(request.getClientIP());
        sb.append(" AuthID:");
        sb.append(request.getAuthId());
        sb.append(" Agent:");
        sb.append(request.getUserAgent());
        sb.append(" Client:");
        sb.append(request.getClient());
        sb.append('(');
        sb.append(request.getVersion());
        sb.append(") Interface:");
        sb.append(request.getInterface().toString());
        final Context ctx = result.getContext();
        if (null != ctx) {
            sb.append(" Context:");
            sb.append(ctx.getContextId());
            sb.append('(');
            sb.append(Strings.join(ctx.getLoginInfo(), ","));
            sb.append(')');
        }
        final User user = result.getUser();
        if (null != user) {
            sb.append(" User:");
            sb.append(user.getId());
            sb.append('(');
            sb.append(user.getLoginInfo());
            sb.append(')');
        }
        final Session session = result.getSession();
        if (null != session) {
            sb.append(" Session:");
            sb.append(session.getSessionID());
            sb.append(" Random:");
            sb.append(session.getRandomToken());
        } else {
            sb.append(" Failed.");
        }
        LOG.info(sb.toString());
    }

    private static void logLogout(final LoginResult result) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Logout ");
        final Context ctx = result.getContext();
        sb.append(" Context:");
        sb.append(ctx.getContextId());
        sb.append('(');
        sb.append(Strings.join(ctx.getLoginInfo(), ","));
        sb.append(')');
        final User user = result.getUser();
        sb.append(" User:");
        sb.append(user.getId());
        sb.append('(');
        sb.append(user.getLoginInfo());
        sb.append(')');
        final Session session = result.getSession();
        sb.append(" Session:");
        sb.append(session.getSessionID());
        LOG.info(sb.toString());
    }

    public Session lookupSession(final String sessionId) throws LoginException {
        try {
            return ServerServiceRegistry.getInstance().getService(SessiondService.class, true).getSession(sessionId);
        } catch (final OXException x) {
            throw new LoginException(x);
        }
    }
}
