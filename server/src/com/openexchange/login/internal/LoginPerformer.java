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

package com.openexchange.login.internal;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Iterator;
import com.openexchange.authentication.Authenticated;
import com.openexchange.authentication.LoginException;
import com.openexchange.authentication.LoginExceptionCodes;
import com.openexchange.authentication.service.Authentication;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.login.Login;
import com.openexchange.login.LoginHandlerService;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.threadpool.AbstractTask;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.ThreadRenamer;
import com.openexchange.threadpool.behavior.CallerRunsBehavior;

/**
 * {@link LoginPerformer} - Performs a login for specified credentials.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class LoginPerformer {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(LoginPerformer.class);

    private static volatile LoginPerformer instance;

    /**
     * Gets the login performer instance.
     * 
     * @return The login performer instance.
     */
    public static LoginPerformer getInstance() {
        LoginPerformer tmp = instance;
        if (null == tmp) {
            synchronized (LoginPerformer.class) {
                tmp = instance;
                if (tmp == null) {
                    instance = tmp = new LoginPerformer();
                }
            }
        }
        return tmp;
    }

    /**
     * Releases the login performer instance.
     */
    public static void releaseInstance() {
        LoginPerformer tmp = instance;
        if (null != tmp) {
            synchronized (LoginPerformer.class) {
                tmp = instance;
                if (tmp != null) {
                    instance = null;
                }
            }
        }
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
    public Login doLogin(final String login, final String password, final String remoteAddress) throws LoginException {
        Session session = null;
        try {
            final Authenticated authed = Authentication.login(login, password);

            final String contextname = authed.getContextInfo();
            final String username = authed.getUserInfo();

            final ContextStorage contextStor = ContextStorage.getInstance();
            final int contextId = contextStor.getContextId(contextname);
            if (ContextStorage.NOT_FOUND == contextId) {
                throw new LoginException(new ContextException(ContextException.Code.NO_MAPPING, contextname));
            }
            final Context context = contextStor.getContext(contextId);
            if (null == context) {
                throw new LoginException(new ContextException(ContextException.Code.NOT_FOUND, Integer.valueOf(contextId)));
            }

            int userId = -1;
            User u = null;

            try {
                final UserStorage us = UserStorage.getInstance();
                userId = us.getUserId(username, context);
                u = us.getUser(userId, context);
            } catch (final LdapException e) {
                switch (e.getDetail()) {
                case ERROR:
                    throw LoginExceptionCodes.UNKNOWN.create(e, e.getMessage());
                case NOT_FOUND:
                    throw LoginExceptionCodes.USER_NOT_FOUND.create(e, username, Integer.valueOf(contextId));
                default:
                    throw new LoginException(e);
                }
            }

            // is user active
            if (u.isMailEnabled()) {
                if (u.getShadowLastChange() == 0) {
                    throw LoginExceptionCodes.PASSWORD_EXPIRED.create(new Object[0]);
                }
            } else {
                throw LoginExceptionCodes.USER_NOT_ACTIVE.create(new Object[0]);
            }

            try {
                if (!context.isEnabled() || !u.isMailEnabled()) {
                    throw LoginExceptionCodes.INVALID_CREDENTIALS.create();
                }
            } catch (final UndeclaredThrowableException e) {
                throw LoginExceptionCodes.UNKNOWN.create(e);
            }

            final SessiondService sessiondService = ServerServiceRegistry.getInstance().getService(SessiondService.class);
            if (sessiondService == null) {
                throw LoginExceptionCodes.COMMUNICATION.create();
            }
            final String sessionId = sessiondService.addSession(userId, username, password, context, remoteAddress, login);
            session = sessiondService.getSession(sessionId);

            final LoginImpl retval = new LoginImpl(session, context, u);
            /*
             * Trigger registered login handlers
             */
            triggerLoginHandlers(retval);
            return retval;
        } catch (final LoginException e) {
            throw e;
        } catch (final AbstractOXException e) {
            throw new LoginException(e);
        }
    }

    /**
     * Performs the logout for specified session ID.
     * 
     * @param sessionId The session ID
     * @throws LoginException If logout fails
     */
    public void doLogout(final String sessionId) throws LoginException {
        /*
         * Drop the session
         */
        final SessiondService sessiondService = ServerServiceRegistry.getInstance().getService(SessiondService.class);
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
        final LoginImpl logout = new LoginImpl(session, context, u);
        /*
         * Remove session
         */
        sessiondService.removeSession(sessionId);
        /*
         * Trigger registered logout handlers
         */
        triggerLogoutHandlers(logout);
    }

    private static void triggerLoginHandlers(final LoginImpl login) {
        final ThreadPoolService executor = ServerServiceRegistry.getInstance().getService(ThreadPoolService.class);
        for (final Iterator<LoginHandlerService> it = LoginHandlerRegistry.getInstance().getLoginHandlers(); it.hasNext();) {
            final LoginHandlerService handler = it.next();
            executor.submit(new LoginPerformerTask() {

                public Object call() {
                    try {
                        handler.handleLogin(login);
                    } catch (final LoginException e) {
                        login.setError(e);
                    }
                    return null;
                }
            }, CallerRunsBehavior.getInstance());
        }
    }

    private static void triggerLogoutHandlers(final LoginImpl logout) {
        final ThreadPoolService executor = ServerServiceRegistry.getInstance().getService(ThreadPoolService.class);
        for (final Iterator<LoginHandlerService> it = LoginHandlerRegistry.getInstance().getLoginHandlers(); it.hasNext();) {
            final LoginHandlerService handler = it.next();
            executor.submit(new LoginPerformerTask() {

                public Object call() {
                    try {
                        handler.handleLogout(logout);
                    } catch (final LoginException e) {
                        logout.setError(e);
                    }
                    return null;
                }
            }, CallerRunsBehavior.getInstance());
        }
    }

    /*-
     * #####################################################################
     */

    private static abstract class LoginPerformerTask extends AbstractTask<Object> {

        protected LoginPerformerTask() {
            super();
        }

        @Override
        public void setThreadName(final ThreadRenamer threadRenamer) {
            threadRenamer.renamePrefix("LoginPerformer");
        }

    }

}
