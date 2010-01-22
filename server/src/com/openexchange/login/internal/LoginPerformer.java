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

package com.openexchange.login.internal;

import static com.openexchange.java.Autoboxing.I;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Iterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.authentication.Authenticated;
import com.openexchange.authentication.LoginException;
import com.openexchange.authentication.LoginExceptionCodes;
import com.openexchange.authentication.service.Authentication;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserException;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.java.Strings;
import com.openexchange.login.LoginResult;
import com.openexchange.login.LoginHandlerService;
import com.openexchange.login.LoginRequest;
import com.openexchange.server.ServiceException;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.sessiond.AddSessionParameter;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.sessiond.exception.SessiondException;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.behavior.CallerRunsBehavior;

/**
 * {@link LoginPerformer} - Performs a login for specified credentials.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class LoginPerformer {

    private static final Log LOG = LogFactory.getLog(LoginPerformer.class);

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
     * Performs the login for specified credentials.
     * 
     * @param login The login string
     * @param password The password string
     * @param remoteAddress The Internet Protocol (IP) address of the client
     * @return The login providing login information
     * @throws LoginException If login fails
     */
    public LoginResult doLogin(final LoginRequest request) throws LoginException {
        final LoginResultImpl retval = new LoginResultImpl();
        try {
            final Authenticated authed = Authentication.login(request.getLogin(), request.getPassword());
            final Context ctx = findContext(authed.getContextInfo());
            retval.setContext(ctx);
            final String username = authed.getUserInfo();
            final User user = findUser(ctx, username);
            retval.setUser(user);
            // Checks if something is deactivated.
            try {
                if (!ctx.isEnabled()) {
                    throw LoginExceptionCodes.INVALID_CREDENTIALS.create();
                }
            } catch (final UndeclaredThrowableException e) {
                throw LoginExceptionCodes.UNKNOWN.create(e);
            }
            if (user.isMailEnabled()) {
                if (user.getShadowLastChange() == 0) {
                    throw LoginExceptionCodes.PASSWORD_EXPIRED.create();
                }
            } else {
                throw LoginExceptionCodes.USER_NOT_ACTIVE.create();
            }
            // Create session
            final SessiondService sessiondService = ServerServiceRegistry.getInstance().getService(SessiondService.class, true);
            final String sessionId = sessiondService.addSession(new AddSessionParameter() {
                public String getClientIP() {
                    return request.getClientIP();
                }
                public Context getContext() {
                    return ctx;
                }
                public String getFullLogin() {
                    return request.getLogin();
                }
                public String getUserLoginInfo() {
                    return username;
                }
                public String getPassword() {
                    return request.getPassword();
                }
                public int getUserId() {
                    return user.getId();
                }
                public String getAuthId() {
                    return request.getAuthId();
                }
            });
            retval.setSession(sessiondService.getSession(sessionId));
            // Trigger registered login handlers
            triggerLoginHandlers(retval);
        } catch (ServiceException e) {
            logLoginRequest(request, retval);
            throw LoginExceptionCodes.COMMUNICATION.create(e);
        } catch (final LoginException e) {
            logLoginRequest(request, retval);
            throw e;
        } catch (ContextException e) {
            logLoginRequest(request, retval);
            throw new LoginException(e);
        } catch (UserException e) {
            logLoginRequest(request, retval);
            throw new LoginException(e);
        } catch (SessiondException e) {
            logLoginRequest(request, retval);
            throw new LoginException(e);
        }
        logLoginRequest(request, retval);
        return retval;
    }

    private Context findContext(String contextInfo) throws ContextException {
        final ContextStorage contextStor = ContextStorage.getInstance();
        final int contextId = contextStor.getContextId(contextInfo);
        if (ContextStorage.NOT_FOUND == contextId) {
            throw new ContextException(ContextException.Code.NO_MAPPING, contextInfo);
        }
        final Context context = contextStor.getContext(contextId);
        if (null == context) {
            throw new ContextException(ContextException.Code.NOT_FOUND, I(contextId));
        }
        return context;
    }

    private User findUser(Context ctx, String userInfo) throws UserException {
        final UserStorage us = UserStorage.getInstance();
        final User u;
        try {
            int userId = us.getUserId(userInfo, ctx);
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
     * @throws LoginException If logout fails
     */
    public void doLogout(final String sessionId) throws LoginException {
        // Drop the session
        final SessiondService sessiondService;
        try {
            sessiondService = ServerServiceRegistry.getInstance().getService(SessiondService.class, true);
        } catch (ServiceException e) {
            throw LoginExceptionCodes.COMMUNICATION.create(e);
        }
        final Session session = sessiondService.getSession(sessionId);
        if (null == session) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No session found for ID: " + sessionId);
            }
            return;
        }
        // Get context
        final ContextStorage contextStor = ContextStorage.getInstance();
        final Context context;
        try {
            context = contextStor.getContext(session.getContextId());
        } catch (final ContextException e) {
            throw new LoginException(e);
        }
        if (null == context) {
            throw new LoginException(new ContextException(ContextException.Code.NOT_FOUND, Integer.valueOf(session.getContextId())));
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
    }

    private static void triggerLoginHandlers(final LoginResult login) {
        final ThreadPoolService executor = ServerServiceRegistry.getInstance().getService(ThreadPoolService.class);
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

    private static void triggerLogoutHandlers(final LoginResult logout) {
        final ThreadPoolService executor = ServerServiceRegistry.getInstance().getService(ThreadPoolService.class);
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

    static void logError(LoginException e) {
        switch (e.getCategory()) {
        case USER_INPUT:
            LOG.debug(e.getMessage(), e);
            break;
        default:
            LOG.error(e.getMessage(), e);
        }
    }

    private static void logLoginRequest(LoginRequest request, LoginResult result) {
        StringBuilder sb = new StringBuilder();
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
        Context ctx = result.getContext();
        if (null != ctx) {
            sb.append(" Context:");
            sb.append(ctx.getContextId());
            sb.append('(');
            sb.append(Strings.join(ctx.getLoginInfo(), ","));
            sb.append(')');
        }
        User user = result.getUser();
        if (null != user) {
            sb.append(" User:");
            sb.append(user.getId());
            sb.append('(');
            sb.append(user.getLoginInfo());
            sb.append(')');
        }
        Session session = result.getSession();
        if (null != session) {
            sb.append(" Session:");
            sb.append(session.getSessionID());
        }
        LOG.info(sb.toString());
    }

    private static void logLogout(LoginResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Logout ");
        Context ctx = result.getContext();
        sb.append(" Context:");
        sb.append(ctx.getContextId());
        sb.append('(');
        sb.append(Strings.join(ctx.getLoginInfo(), ","));
        sb.append(')');
        User user = result.getUser();
        sb.append(" User:");
        sb.append(user.getId());
        sb.append('(');
        sb.append(user.getLoginInfo());
        sb.append(')');
        Session session = result.getSession();
        sb.append(" Session:");
        sb.append(session.getSessionID());
        LOG.info(sb.toString());
    }
}
