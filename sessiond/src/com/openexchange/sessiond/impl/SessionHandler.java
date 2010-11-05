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

package com.openexchange.sessiond.impl;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.sessiond.services.SessiondServiceRegistry.getServiceRegistry;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import com.openexchange.caching.CacheException;
import com.openexchange.caching.objects.CachedSession;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.server.ServiceException;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessionExceptionCodes;
import com.openexchange.sessiond.SessiondEventConstants;
import com.openexchange.sessiond.SessiondException;
import com.openexchange.sessiond.cache.SessionCache;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;

/**
 * {@link SessionHandler} - Provides access to sessions
 * 
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SessionHandler {

    private static SessionIdGenerator sessionIdGenerator;

    private static SessiondConfigInterface config;

    private static SessionData sessionData;

    private static boolean noLimit;

    private static final AtomicBoolean initialized = new AtomicBoolean();

    private static final Log LOG = LogFactory.getLog(SessionHandler.class);

    private static final boolean INFO = LOG.isInfoEnabled();

    private static final boolean DEBUG = LOG.isDebugEnabled();

    private static ScheduledTimerTask shortSessionContainerRotator;

    private static ScheduledTimerTask longSessionContainerRotator;

    /**
     * Initializes a new {@link SessionHandler session handler}
     */
    private SessionHandler() {
        super();
    }

    /**
     * Initializes the {@link SessionHandler session handler}
     * 
     * @param newConfig The appropriate configuration
     */
    public static void init(final SessiondConfigInterface newConfig) {
        SessionHandler.config = newConfig;
        sessionData = new SessionData(
            config.getNumberOfSessionContainers(),
            config.getMaxSessions(),
            config.getRandomTokenTimeout(),
            config.getNumberOfLongTermSessionContainers());
        if (initialized.compareAndSet(false, true)) {
            try {
                sessionIdGenerator = SessionIdGenerator.getInstance();
            } catch (final SessiondException exc) {
                LOG.error("create instance of SessionIdGenerator", exc);
            }

            noLimit = (newConfig.getMaxSessions() == 0);
        }
    }

    /**
     * Removes all sessions associated with given user in specified context
     * 
     * @param userId The user ID
     * @param contextId The context ID
     * @param propagate <code>true</code> for remote removal; otherwise <code>false</code>
     * @return The wrapper objects for removed sessions
     */
    public static SessionControl[] removeUserSessions(final int userId, final int contextId, final boolean propagate) {
        final SessionControl[] retval = sessionData.removeUserSessions(userId, contextId);
        if (propagate) {
            for (final SessionControl sessionControl : retval) {
                try {
                    SessionCache.getInstance().putCachedSessionForRemoteRemoval(
                        ((SessionImpl) sessionControl.getSession()).createCachedSession());
                } catch (final CacheException e) {
                    LOG.error("Remote removal failed for session " + sessionControl.getSession().getSecret(), e);
                } catch (final ServiceException e) {
                    LOG.error("Remote removal failed for session " + sessionControl.getSession().getSecret(), e);
                }
            }
        }
        if (INFO) {
            LOG.info(new StringBuilder(64).append(propagate ? "Remote" : "Local").append(" removal of user sessions: User=").append(userId).append(
                ", Context=").append(contextId).toString());
        }
        return retval;
    }

    /**
     * Gets all sessions associated with given user in specified context
     * 
     * @param userId The user ID
     * @param contextId The context ID
     * @return The wrapper objects for sessions
     */
    public static SessionControl[] getUserSessions(final int userId, final int contextId) {
        return sessionData.getUserSessions(userId, contextId);
    }

    /**
     * Adds a new session containing given attributes to session container(s)
     * 
     * @param userId The user ID
     * @param loginName The user's login name
     * @param password The user's password
     * @param context The context
     * @param clientHost The client host name or IP address
     * @param login The full user's login; e.g. <i>test@foo.bar</i>
     * @return The session ID associated with newly created session
     * @throws SessiondException If creating a new session fails
     */
    protected static String addSession(final int userId, final String loginName, final String password, final Context context, final String clientHost, final String login, final String authId, final String hash) throws SessiondException {
        checkMaxSessPerUser(userId, context);
        checkAuthId(login, authId);
        final String sessionId = sessionIdGenerator.createSessionId(loginName, clientHost);
        final SessionImpl session =
            new SessionImpl(userId, loginName, password, context.getContextId(), sessionId, sessionIdGenerator.createSecretId(
                loginName,
                Long.toString(System.currentTimeMillis())), sessionIdGenerator.createRandomId(), clientHost, login, authId, hash);
        // Add session
        sessionData.addSession(session, noLimit);
        // Post event for created session
        postSessionCreation(session);
        // Return session ID
        return sessionId;
    }

    private static void checkMaxSessPerUser(final int userId, final Context context) throws SessiondException {
        final int maxSessPerUser = config.getMaxSessionsPerUser();
        if (maxSessPerUser > 0) {
            final int count = sessionData.getNumOfUserSessions(userId, context);
            if (count >= maxSessPerUser) {
                throw SessionExceptionCodes.MAX_SESSION_PER_USER_EXCEPTION.create(I(userId), I(context.getContextId()));
            }
        }
    }

    private static void checkAuthId(final String login, final String authId) throws SessiondException {
        sessionData.checkAuthId(login, authId);
    }

    /**
     * Refreshes the session's last-accessed time stamp
     * 
     * @param sessionid The session ID denoting the session
     * @return <code>true</code> if a refreshing last-accessed time stamp was successful; otherwise <code>false</code>
     */
    protected static boolean refreshSession(final String sessionid) {
        if (DEBUG) {
            LOG.debug(new StringBuilder("refreshSession <").append(sessionid).append('>').toString());
        }
        return sessionData.getSession(sessionid) != null;
    }

    /**
     * Clears the session denoted by given session ID from session container(s)
     * 
     * @param sessionid The session ID
     * @return <code>true</code> if a session could be removed; otherwise <code>false</code>
     */
    protected static boolean clearSession(final String sessionid) {
        final SessionControl sessionControl = sessionData.clearSession(sessionid);
        if (null == sessionControl) {
            LOG.debug("Cannot find session id to remove session <" + sessionid + '>');
            return false;
        }
        postSessionRemoval(sessionControl.getSession());
        return true;
    }

    /**
     * Changes the password stored in session denoted by given session ID
     * 
     * @param sessionid The session ID
     * @param newPassword The new password
     * @throws SessiondException If changing the password fails
     */
    protected static void changeSessionPassword(final String sessionid, final String newPassword) throws SessiondException {
        if (DEBUG) {
            LOG.debug(new StringBuilder("changeSessionPassword <").append(sessionid).append('>').toString());
        }
        final SessionControl sessionControl = sessionData.getSession(sessionid);
        if (null == sessionControl) {
            throw SessionExceptionCodes.PASSWORD_UPDATE_FAILED.create();
        }
        // TODO: Check permission via security service
        ((SessionImpl) sessionControl.getSession()).setPassword(newPassword);
    }

    protected static Session getSessionByRandomToken(final String randomToken, final String localIp) {
        final SessionControl sessionControl = sessionData.getSessionByRandomToken(randomToken, localIp);
        if (null == sessionControl) {
            return null;
        }
        return sessionControl.getSession();
    }

    /**
     * Gets the session associated with given session ID
     * 
     * @param sessionId The session ID
     * @return The session associated with given session ID; otherwise <code>null</code> if expired or none found
     */
    protected static SessionControl getSession(final String sessionId) {
        if (DEBUG) {
            LOG.debug(new StringBuilder("getSession <").append(sessionId).append('>').toString());
        }
        final SessionControl sessionControl = sessionData.getSession(sessionId);
        if (null == sessionControl) {
            return null;
        }
        // Look-up cache if current session wrapped by session-control is marked for removal
        try {
            final SessionCache cache = SessionCache.getInstance();
            final Session session = sessionControl.getSession();
            final CachedSession cachedSession = cache.getCachedSessionByUser(session.getUserId(), session.getContextId());
            if (null != cachedSession) {
                if (cachedSession.isMarkedAsRemoved()) {
                    cache.removeCachedSession(cachedSession.getSecret());
                    removeUserSessions(cachedSession.getUserId(), cachedSession.getContextId(), false);
                    return null;
                }
            }
        } catch (final CacheException e) {
            LOG.error("Unable to look-up session cache", e);
        } catch (final ServiceException e) {
            LOG.error("Unable to look-up session cache", e);
        }
        return sessionControl;
    }

    /**
     * Gets (and removes) the session bound to given session identifier in cache.
     * <p>
     * Session is going to be added to local session containers on a cache hit.
     * 
     * @param sessionId The session identifier
     * @return A wrapping instance of {@link SessionControl} or <code>null</code>
     */
    public static SessionControl getCachedSession(final String sessionId) {
        if (DEBUG) {
            LOG.debug(new StringBuilder("getCachedSession <").append(sessionId).append('>').toString());
        }
        try {
            final CachedSession cachedSession = SessionCache.getInstance().removeCachedSession(sessionId);
            if (null != cachedSession) {
                if (cachedSession.isMarkedAsRemoved()) {
                    removeUserSessions(cachedSession.getUserId(), cachedSession.getContextId(), false);
                } else {
                    // A cache hit! Add to local session containers
                    LOG.info("Migrate session: " + cachedSession.getSessionId());
                    return sessionData.addSession(new SessionImpl(cachedSession), noLimit);
                }
            }
        } catch (final CacheException e) {
            LOG.error(e.getMessage(), e);
        } catch (final SessiondException e) {
            LOG.error(e.getMessage(), e);
        } catch (final ServiceException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Gets all available instances of {@link SessionControl}
     * 
     * @return All available instances of {@link SessionControl}
     */
    public static List<SessionControl> getSessions() {
        if (DEBUG) {
            LOG.debug("getSessions");
        }
        return sessionData.getShortTermSessions();
    }

    protected static void cleanUp() {
        if (DEBUG) {
            LOG.debug("session cleanup");
        }
        final List<SessionControl> controls = sessionData.rotateShort();
        if (INFO) {
            for (final SessionControl sessionControl : controls) {
                LOG.info("Session timed out. ID: " + sessionControl.getSession().getSessionID());
            }
        }
        postSessionDataRemoval(controls);
    }

    protected static void cleanUpLongTerm() {
        List<SessionControl> controls = sessionData.rotateLongTerm();
        if (INFO) {
            for (SessionControl control : controls) {
                LOG.info("Session timed out. ID: " + control.getSession().getSessionID());
            }
        }
        postContainerRemoval(controls);
    }

    public static void close() {
        if (initialized.compareAndSet(true, false)) {
            postContainerRemoval(sessionData.getShortTermSessions());
            sessionData.clear();
            sessionIdGenerator = null;
            config = null;
            noLimit = false;
        }
    }

    public static int getNumberOfActiveSessions() {
        return sessionData.countSessions();
    }

    private static void postSessionCreation(final Session session) {
        final EventAdmin eventAdmin = getServiceRegistry().getService(EventAdmin.class);
        if (eventAdmin != null) {
            final Dictionary<Object, Object> dic = new Hashtable<Object, Object>();
            dic.put(SessiondEventConstants.PROP_SESSION, session);
            final Event event = new Event(SessiondEventConstants.TOPIC_ADD_SESSION, dic);
            eventAdmin.postEvent(event);
            if (DEBUG) {
                LOG.debug("Posted event for added session");
            }
        }
    }

    private static void postSessionRemoval(final Session session) {
        final EventAdmin eventAdmin = getServiceRegistry().getService(EventAdmin.class);
        if (eventAdmin != null) {
            final Dictionary<Object, Object> dic = new Hashtable<Object, Object>();
            dic.put(SessiondEventConstants.PROP_SESSION, session);
            final Event event = new Event(SessiondEventConstants.TOPIC_REMOVE_SESSION, dic);
            eventAdmin.postEvent(event);
            if (DEBUG) {
                LOG.debug("Posted event for removed session");
            }
        }
    }

    private static void postContainerRemoval(final List<SessionControl> sessionControls) {
        final EventAdmin eventAdmin = getServiceRegistry().getService(EventAdmin.class);
        if (eventAdmin != null) {
            final Dictionary<Object, Object> dic = new Hashtable<Object, Object>();
            final Map<String, Session> eventMap = new HashMap<String, Session>();
            for (final SessionControl sessionControl : sessionControls) {
                final Session session = sessionControl.getSession();
                eventMap.put(session.getSessionID(), session);
            }
            dic.put(SessiondEventConstants.PROP_CONTAINER, eventMap);
            final Event event = new Event(SessiondEventConstants.TOPIC_REMOVE_CONTAINER, dic);
            eventAdmin.postEvent(event);
            if (DEBUG) {
                LOG.debug("Posted event for removed session container");
            }
        }
    }

    private static void postSessionDataRemoval(final List<SessionControl> controls) {
        final EventAdmin eventAdmin = getServiceRegistry().getService(EventAdmin.class);
        if (eventAdmin != null) {
            final Dictionary<Object, Object> dic = new Hashtable<Object, Object>();
            final Map<String, Session> eventMap = new HashMap<String, Session>();
            for (final SessionControl sessionControl : controls) {
                final Session session = sessionControl.getSession();
                eventMap.put(session.getSessionID(), session);
            }
            dic.put(SessiondEventConstants.PROP_CONTAINER, eventMap);
            final Event event = new Event(SessiondEventConstants.TOPIC_REMOVE_DATA, dic);
            eventAdmin.postEvent(event);
            if (DEBUG) {
                LOG.debug("Posted event for removing temporary session data.");
            }
        }
    }

    static void postSessionReactivation(final Session session) {
        final EventAdmin eventAdmin = getServiceRegistry().getService(EventAdmin.class);
        if (eventAdmin != null) {
            final Dictionary<Object, Object> dic = new Hashtable<Object, Object>();
            dic.put(SessiondEventConstants.PROP_SESSION, session);
            final Event event = new Event(SessiondEventConstants.TOPIC_ADD_SESSION, dic);
            eventAdmin.postEvent(event);
            if (DEBUG) {
                LOG.debug("Posted event for added session");
            }
        }
    }

    public static void addThreadPoolService(final ThreadPoolService service) {
        sessionData.addThreadPoolService(service);
    }

    public static void removeThreadPoolService() {
        sessionData.removeThreadPoolService();
    }

    public static void addTimerService(TimerService service) {
        sessionData.addTimerService(service);
        shortSessionContainerRotator = service.scheduleWithFixedDelay(
            new ShortSessionContainerRotator(),
            config.getSessionContainerTimeout(),
            config.getSessionContainerTimeout());
        longSessionContainerRotator = service.scheduleWithFixedDelay(
            new LongSessionContainerRotator(),
            config.getLongTermSessionContainerTimeout(),
            config.getLongTermSessionContainerTimeout());
    }

    public static void removeTimerService() {
        if (longSessionContainerRotator != null) {
            longSessionContainerRotator.cancel(false);
            longSessionContainerRotator = null;
        }
        if (shortSessionContainerRotator != null) {
            shortSessionContainerRotator.cancel(false);
            shortSessionContainerRotator = null;
        }
        sessionData.removeTimerService();
    }
}
