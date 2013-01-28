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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.sessionstorage.hazelcast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.logging.Log;
import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MultiMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.SqlPredicate;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.session.Session;
import com.openexchange.sessionstorage.SessionStorageExceptionCodes;
import com.openexchange.sessionstorage.SessionStorageService;

/**
 * {@link HazelcastSessionStorageService} - The {@link SessionStorageService} backed by {@link HazelcastInstance}.
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class HazelcastSessionStorageService implements SessionStorageService {

    private static final Log LOG = com.openexchange.log.Log.loggerFor(HazelcastSessionStorageService.class);
    private static final boolean DEBUG = LOG.isDebugEnabled();
    private static final AtomicReference<HazelcastInstance> REFERENCE = new AtomicReference<HazelcastInstance>();

    /**
     * Sets specified {@link HazelcastInstance}.
     *
     * @param hazelcast The {@link HazelcastInstance}
     */
    public static void setHazelcastInstance(final HazelcastInstance hazelcast) {
        REFERENCE.set(hazelcast);
    }

    private final String sessionsMapName;
    private final String userSessionsMapName;

    /**
     * Initializes a new {@link HazelcastSessionStorageService}.
     *
     * @param sessionsMapName The name of the distributed 'sessions' map
     * @param userSessionsMapName  The name of the distributed 'userSessions' map
     */
    public HazelcastSessionStorageService(String sessionsMapName, String userSessionsMapName) {
        super();
        this.sessionsMapName = sessionsMapName;
        this.userSessionsMapName = userSessionsMapName;
    }

    @Override
    public Session lookupSession(final String sessionId) throws OXException {
        try {
            HazelcastStoredSession storedSession = sessions().get(sessionId);
            if (null == storedSession) {
                throw SessionStorageExceptionCodes.NO_SESSION_FOUND.create(sessionId);
            }
            return storedSession;
        } catch (HazelcastException e) {
            throw SessionStorageExceptionCodes.NO_SESSION_FOUND.create(e, sessionId);
        } catch (OXException e) {
            if (ServiceExceptionCode.SERVICE_UNAVAILABLE.equals(e)) {
                throw SessionStorageExceptionCodes.NO_SESSION_FOUND.create(e, sessionId);
            }
            throw e;
        }
    }

    @Override
    public void addSessionsIfAbsent(final Collection<Session> sessions) throws OXException {
        if (null == sessions || sessions.isEmpty()) {
            return;
        }
        try {
            IMap<String, HazelcastStoredSession> sessionsMap = sessions();
            for (Session session : sessions) {
                try {
                    if (null == sessionsMap.putIfAbsent(session.getSessionID(), new HazelcastStoredSession(session))) {
                        userSessions().put(getKey(session.getContextId(), session.getUserId()), session.getSessionID());
                    }
                } catch (HazelcastException e) {
                    LOG.warn("Session " + session.getSessionID() + " could not be added to session storage.", e);
                }
            }
        } catch (RuntimeException e) {
            throw SessionStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public boolean addSessionIfAbsent(final Session session) throws OXException {
        if (null == session) {
            return false;
        }
        try {
            if (null == sessions().putIfAbsent(session.getSessionID(), new HazelcastStoredSession(session))) {
                userSessions().put(getKey(session.getContextId(), session.getUserId()), session.getSessionID());
                return true;
            }
            return false;
        } catch (final HazelcastException e) {
            throw SessionStorageExceptionCodes.SAVE_FAILED.create(e, session.getSessionID());
        } catch (final OXException e) {
            if (ServiceExceptionCode.SERVICE_UNAVAILABLE.equals(e)) {
                throw SessionStorageExceptionCodes.SAVE_FAILED.create(e, session.getSessionID());
            }
            throw e;
        }
    }

    @Override
    public void addSession(final Session session) throws OXException {
        if (null != session) {
            try {
                sessions().set(session.getSessionID(), new HazelcastStoredSession(session), 0, TimeUnit.SECONDS);
                userSessions().put(getKey(session.getContextId(), session.getUserId()), session.getSessionID());
            } catch (HazelcastException e) {
                throw SessionStorageExceptionCodes.SAVE_FAILED.create(e, session.getSessionID());
            } catch (OXException e) {
                if (ServiceExceptionCode.SERVICE_UNAVAILABLE.equals(e)) {
                    throw SessionStorageExceptionCodes.SAVE_FAILED.create(e, session.getSessionID());
                }
                throw e;
            }
        }
    }

    @Override
    public void removeSession(final String sessionId) throws OXException {
        if (null != sessionId) {
            try {
                HazelcastStoredSession removedSession = sessions().remove(sessionId);
                if (null != removedSession) {
                    userSessions().remove(getKey(removedSession.getContextId(), removedSession.getUserId()), sessionId);
                } else {
                    LOG.debug("Session with ID '" + sessionId + "' not found, unable to remove from storage.");
                }
            } catch (HazelcastException e) {
                throw SessionStorageExceptionCodes.REMOVE_FAILED.create(e, sessionId);
            } catch (OXException e) {
                if (ServiceExceptionCode.SERVICE_UNAVAILABLE.equals(e)) {
                    throw SessionStorageExceptionCodes.REMOVE_FAILED.create(e, sessionId);
                }
                throw e;
            }
        }
    }

    @Override
    public Session[] removeUserSessions(final int userId, final int contextId) throws OXException {
        try {
            Collection<String> removedSessionIDs = userSessions().remove(getKey(contextId, userId));
            if (null != removedSessionIDs && 0 < removedSessionIDs.size()) {
                Map<String, HazelcastStoredSession> removedSessions = sessions().getAll(new HashSet<String>(removedSessionIDs));
                if (null != removedSessions && 0 < removedSessions.size()) {
                    return removedSessions.values().toArray(new Session[removedSessions.size()]);
                }
            }
            return new Session[0];
        } catch (HazelcastException e) {
            if (DEBUG) {
                LOG.debug(e.getMessage(), e);
            }
            throw SessionStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (OXException e) {
            if (ServiceExceptionCode.SERVICE_UNAVAILABLE.equals(e)) {
                if (DEBUG) {
                    LOG.debug(e.getMessage(), e);
                }
                return new Session[0];
            }
            throw e;
        }
    }

    @Override
    public void removeContextSessions(final int contextId) throws OXException {
        try {
            IMap<String, HazelcastStoredSession> sessions = sessions();
            if (null != sessions && 0 < sessions.size()) {
                for (Entry<String, HazelcastStoredSession> entry : sessions.entrySet(new SqlPredicate("contextId = " + contextId))) {
                    Session removedSession = sessions.remove(entry.getKey());
                    if (null != removedSession) {
                        userSessions().remove(getKey(removedSession.getContextId(), removedSession.getContextId()));
                    }
                }
            }
        } catch (HazelcastException e) {
            if (DEBUG) {
                LOG.debug(e.getMessage(), e);
            }
            throw SessionStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (OXException e) {
            if (ServiceExceptionCode.SERVICE_UNAVAILABLE.equals(e)) {
                if (DEBUG) {
                    LOG.debug(e.getMessage(), e);
                }
            }
            throw e;
        }
    }

    @Override
    public boolean hasForContext(final int contextId) throws OXException {
        SqlPredicate predicate = new SqlPredicate("contextId = " + contextId);
        try {
            /*
             * try to lookup session from local keyset first
             */
            for (HazelcastStoredSession session : filterLocal(predicate)) {
                if (null != session && session.getContextId() == contextId) {
                    return true;
                }
            }
            /*
             * also query cluster if not yet found
             */
            for (HazelcastStoredSession session : filter(predicate)) {
                if (null != session && session.getContextId() == contextId) {
                    return true;
                }
            }
        } catch (HazelcastException e) {
            LOG.debug(e.getMessage(), e);
        }
        /*
         * none found
         */
        return false;
    }

    @Override
    public Session[] getUserSessions(final int userId, final int contextId) throws OXException {
        try {
            Collection<String> sessionIDs = userSessions().get(getKey(contextId, userId));
            if (null != sessionIDs && 0 < sessionIDs.size()) {
                Map<String, HazelcastStoredSession> sessions = sessions().getAll(new HashSet<String>(sessionIDs));
                if (null != sessions && 0 < sessions.size()) {
                    return sessions.values().toArray(new Session[sessions.values().size()]);
                }
            }
        } catch (final HazelcastException e) {
            if (DEBUG) {
                LOG.debug(e.getMessage(), e);
            }
        }
        return new Session[0];
    }

    @Override
    public Session getAnyActiveSessionForUser(final int userId, final int contextId) throws OXException {
        try {
            Collection<String> sessionIDs = userSessions().get(getKey(contextId, userId));
            if (null != sessionIDs && 0 < sessionIDs.size()) {
                IMap<String, HazelcastStoredSession> sessions = sessions();
                /*
                 * try to lookup session from local keyset first
                 */
                Set<String> localKeySet = sessions.localKeySet();
                if (null != localKeySet && 0 < localKeySet.size()) {
                    for (String sessionID : sessionIDs) {
                        if (localKeySet.contains(sessionID)) {
                            return sessions.get(sessionID);
                        }
                    }
                }
                /*
                 * get first session from cluster if not yet found
                 */
                return sessions.get(sessionIDs.iterator().next());
            }
        } catch (final HazelcastException e) {
            if (DEBUG) {
                LOG.debug(e.getMessage(), e);
            }
        }
        /*
         * not found
         */
        return null;
    }

    @Override
    public Session findFirstSessionForUser(final int userId, final int contextId) throws OXException {
        return getAnyActiveSessionForUser(userId, contextId);
    }

    @Override
    public List<Session> getSessions() {
        try {
            return new ArrayList<Session>(sessions().values());
        } catch (final HazelcastException e) {
            if (DEBUG) {
                LOG.debug(e.getMessage(), e);
            }
        } catch (OXException e) {
            if (DEBUG) {
                LOG.debug(e.getMessage(), e);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public int getNumberOfActiveSessions() {
        try {
            return sessions().size();
        } catch (OXException e) {
            if (DEBUG) {
                LOG.debug(e.getMessage(), e);
            }
        }
        return 0;
    }

    @Override
    public Session getSessionByRandomToken(final String randomToken, final String newIP) throws OXException {
        try {
            if (null != randomToken) {
                for (HazelcastStoredSession session : filter(new SqlPredicate("randomToken = '" + randomToken + "'"))) {
                    if (null != session && randomToken.equals(session.getRandomToken())) {
                        if (false == session.getLocalIp().equals(newIP)) {
                            session.setLocalIp(newIP);
                            // TODO: Re-Put needed to distribute change?
                            sessions().set(session.getSessionId(), session, 0, TimeUnit.SECONDS);
                        }
                        return session;
                    }
                }
            }
            throw SessionStorageExceptionCodes.RANDOM_NOT_FOUND.create(randomToken);
        } catch (final HazelcastException e) {
            if (DEBUG) {
                LOG.debug(e.getMessage(), e);
            }
            throw SessionStorageExceptionCodes.RANDOM_NOT_FOUND.create(e, randomToken);
        } catch (final OXException e) {
            if (ServiceExceptionCode.SERVICE_UNAVAILABLE.equals(e)) {
                if (DEBUG) {
                    LOG.debug(e.getMessage(), e);
                }
                throw SessionStorageExceptionCodes.RANDOM_NOT_FOUND.create(e, randomToken);
            }
            throw e;
        }
    }

    @Override
    public Session getSessionByAlternativeId(final String altId) throws OXException {
        try {
            if (null == altId) {
                throw new NullPointerException("altId is null.");
            }
            for (HazelcastStoredSession session : filter(new AltIdPredicate(altId))) {
                if (null != session && altId.equals(session.getParameter(Session.PARAM_ALTERNATIVE_ID))) {
                    return session;
                }
            }
            throw SessionStorageExceptionCodes.ALTID_NOT_FOUND.create(altId);
        } catch (final HazelcastException e) {
            if (DEBUG) {
                LOG.debug(e.getMessage(), e);
            }
            throw SessionStorageExceptionCodes.ALTID_NOT_FOUND.create(e, altId);
        } catch (final OXException e) {
            if (ServiceExceptionCode.SERVICE_UNAVAILABLE.equals(e)) {
                if (DEBUG) {
                    LOG.debug(e.getMessage(), e);
                }
                throw SessionStorageExceptionCodes.ALTID_NOT_FOUND.create(e, altId);
            }
            throw e;
        }
    }

    @Override
    public Session getCachedSession(final String sessionId) throws OXException {
        return lookupSession(sessionId);
    }

    @Override
    public void cleanUp() throws OXException {
        sessions().clear();
        userSessions().clear();
    }

    @Override
    public void changePassword(final String sessionId, final String newPassword) throws OXException {
        try {
            IMap<String, HazelcastStoredSession> sessions = sessions();
            HazelcastStoredSession storedSession = sessions.get(sessionId);
            if (null == storedSession) {
                throw SessionStorageExceptionCodes.NO_SESSION_FOUND.create(sessionId);
            }
            storedSession.setPassword(newPassword);
            sessions.set(sessionId, storedSession, 0, TimeUnit.SECONDS);
        } catch (HazelcastException e) {
            if (DEBUG) {
                LOG.debug(e.getMessage(), e);
            }
            throw SessionStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public void setLocalIp(String sessionId, String localIp) throws OXException {
        try {
            IMap<String, HazelcastStoredSession> sessions = sessions();
            HazelcastStoredSession storedSession = sessions.get(sessionId);
            if (null == storedSession) {
                throw SessionStorageExceptionCodes.NO_SESSION_FOUND.create(sessionId);
            }
            storedSession.setLocalIp(localIp);
            sessions.set(sessionId, storedSession, 0, TimeUnit.SECONDS);
        } catch (HazelcastException e) {
            if (DEBUG) {
                LOG.debug(e.getMessage(), e);
            }
            throw SessionStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public void setClient(String sessionId, String client) throws OXException {
        try {
            IMap<String, HazelcastStoredSession> sessions = sessions();
            HazelcastStoredSession storedSession = sessions.get(sessionId);
            if (null == storedSession) {
                throw SessionStorageExceptionCodes.NO_SESSION_FOUND.create(sessionId);
            }
            storedSession.setClient(client);
            sessions.set(sessionId, storedSession, 0, TimeUnit.SECONDS);
        } catch (HazelcastException e) {
            if (DEBUG) {
                LOG.debug(e.getMessage(), e);
            }
            throw SessionStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public void setHash(String sessionId, String hash) throws OXException {
        try {
            IMap<String, HazelcastStoredSession> sessions = sessions();
            HazelcastStoredSession storedSession = sessions.get(sessionId);
            if (null == storedSession) {
                throw SessionStorageExceptionCodes.NO_SESSION_FOUND.create(sessionId);
            }
            storedSession.setHash(hash);
            sessions.set(sessionId, storedSession, 0, TimeUnit.SECONDS);
        } catch (HazelcastException e) {
            if (DEBUG) {
                LOG.debug(e.getMessage(), e);
            }
            throw SessionStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public void checkAuthId(final String login, final String authId) throws OXException {
        if (null != authId) {
            try {
                for (HazelcastStoredSession session : filter(new SqlPredicate("authId = '" + authId + "'"))) {
                    if (null != session && authId.equals(session.getAuthId())) {
                        throw SessionStorageExceptionCodes.DUPLICATE_AUTHID.create(session.getLogin(), login);
                    }
                }
            } catch (final HazelcastException e) {
                if (DEBUG) {
                    LOG.debug(e.getMessage(), e);
                }
                throw SessionStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
        }
    }

    @Override
    public int getUserSessionCount(int userId, int contextId) throws OXException {
        Collection<String> sessionIDs = userSessions().get(getKey(contextId, userId));
        return null != sessionIDs ? sessionIDs.size() : 0;
    }

    /**
     * 'Touches' a session in the storage causing the map entry's idle time being reseted. 
     * 
     * @param sessionID The session ID
     * @throws OXException
     */
    public void touch(String sessionID) throws OXException {
        /*
         * calling containsKey resets map entries idle-time
         */
        if (false == sessions().containsKey(sessionID)) {
            LOG.warn("Ignoring keep-alive even for not found session ID: " + sessionID);
        } else {
            LOG.debug("Received keep-alive for '" + sessionID + "'.");
        }
    }
    
    /**
     * Filters the stored sessions by a {@link Predicate}.
     *
     * @param predicate The predicate to use for filtering
     * @return The stored sessions matching the predicate, or an empty collection if none were found
     * @throws OXException
     */
    private Collection<HazelcastStoredSession> filter(Predicate<?, ?> predicate) throws OXException {
        return sessions().values(predicate);
    }

    /**
     * Filters the locally available stored sessions by a {@link Predicate}.
     *
     * @param predicate The predicate to use for filtering
     * @param failIfPaused <code>true</code> to abort if the hazelcast instance is paused, <code>false</code>, otherwise
     * @return The stored sessions matching the predicate, or an empty collection if none were found
     * @throws OXException
     */
    private Collection<HazelcastStoredSession> filterLocal(Predicate<?, ?> predicate) throws OXException {
        IMap<String, HazelcastStoredSession> sessions = sessions();
        if (null == sessions) {
            return Collections.emptyList();
        }
        Collection<HazelcastStoredSession> values = new ArrayList<HazelcastStoredSession>();
        Set<String> localKeySet = sessions.localKeySet(predicate);
        if (null != localKeySet && 0 < localKeySet.size()) {
            for (String key : localKeySet) {
                HazelcastStoredSession storedSession = sessions.get(key);
                if (null != storedSession) {
                    values.add(storedSession);
                }
            }
        }
        return values;
    }

    /**
     * Gets the 'sessions' map that maps session-IDs to stored sessions.
     *
     * @return The 'sessions' map
     * @throws OXException
     */
    private IMap<String, HazelcastStoredSession> sessions() throws OXException {
        try {
            HazelcastInstance hazelcastInstance = REFERENCE.get();
            if (null == hazelcastInstance || false == hazelcastInstance.getLifecycleService().isRunning()) {
                throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(HazelcastInstance.class.getName());
            }
            return hazelcastInstance.getMap(sessionsMapName);
        } catch (HazelcastException e) {
            throw SessionStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw SessionStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Gets the 'userSessions' multi-map that maps context-/user-IDs to session IDs.
     *
     * @return The 'userSessions' multi-map
     * @throws OXException
     */
    private MultiMap<Long, String> userSessions() throws OXException {
        try {
            HazelcastInstance hazelcastInstance = REFERENCE.get();
            if (null == hazelcastInstance || false == hazelcastInstance.getLifecycleService().isRunning()) {
                throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(HazelcastInstance.class.getName());
            }
            return hazelcastInstance.getMultiMap(userSessionsMapName);
        } catch (HazelcastException e) {
            throw SessionStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw SessionStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Creates an unique key from the supplied context- and user-ID
     *
     * @param contextID The context ID
     * @param userID The user ID
     * @return A long created from the context- and user-ID
     */
    private static Long getKey(int contextID, int userID) {
        return Long.valueOf(((long) contextID << 32) | (userID & 0xFFFFFFFL));
    }

}
