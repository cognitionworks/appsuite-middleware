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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.session.management.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.Member;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.session.Session;
import com.openexchange.session.management.ManagedSession;
import com.openexchange.session.management.ManagedSession.ManagedSessionBuilder;
import com.openexchange.session.management.SessionManagementProperty;
import com.openexchange.session.management.SessionManagementService;
import com.openexchange.session.management.exception.SessionManagementExceptionCodes;
import com.openexchange.session.management.osgi.Services;
import com.openexchange.sessiond.SessionFilter;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.sessionstorage.hazelcast.serialization.PortableMultipleSessionRemoteLookUp;
import com.openexchange.sessionstorage.hazelcast.serialization.PortableSession;
import com.openexchange.sessionstorage.hazelcast.serialization.PortableSessionCollection;

/**
 * {@link SessionManagementServiceImpl}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.0
 */
public class SessionManagementServiceImpl implements SessionManagementService {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(SessionManagementServiceImpl.class);

    @Override
    public Collection<ManagedSession> getSessionsForUser(Session session) throws OXException {
        SessiondService service = Services.getService(SessiondService.class);
        if (null == service) {
            throw ServiceExceptionCode.absentService(SessiondService.class);
        }
        List<String> blackListedClients = getBlacklistedClients();

        Collection<Session> localSessions = service.getSessions(session.getUserId(), session.getContextId());
        Collection<PortableSession> remoteSessions = getRemoteSessionsForUser(session);

        ArrayList<ManagedSession> result = new ArrayList<>(localSessions.size() + remoteSessions.size());
        for (Session s : localSessions) {
            if (blackListedClients.contains(s.getClient())) {
                continue;
            }
            ManagedSessionBuilder builder = new ManagedSessionBuilder(s);
            ManagedSession managedSession = builder.build();
            result.add(managedSession);
        }
        for (PortableSession s : remoteSessions) {
            if (blackListedClients.contains(s.getClient())) {
                continue;
            }
            ManagedSessionBuilder builder = new ManagedSessionBuilder(s);
            ManagedSession managedSession = builder.build();
            result.add(managedSession);
        }
        return result;
    }

    @Override
    public void removeSession(Session session, String sessionIdToRemove) throws OXException {
        SessiondService sessiondService = Services.getService(SessiondService.class);
        StringBuilder sb = new StringBuilder("(&");
        sb.append("(").append(SessionFilter.SESSION_ID).append("=").append(sessionIdToRemove).append(")");
        sb.append("(").append(SessionFilter.CONTEXT_ID).append("=").append(session.getContextId()).append(")");
        sb.append("(").append(SessionFilter.USER_ID).append("=").append(session.getUserId()).append(")");
        sb.append(")");
        try {
            Collection<String> removedSessions = sessiondService.removeSessionsGlobally(SessionFilter.create(sb.toString()));
            if (removedSessions.isEmpty()) {
                throw SessionManagementExceptionCodes.SESSION_NOT_FOUND.create();
            }
        } catch (OXException e) {
            throw SessionManagementExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    private List<String> getBlacklistedClients() {
        LeanConfigurationService configService = Services.getService(LeanConfigurationService.class);
        if (null == configService) {
            return Collections.emptyList();
        }
        String value = configService.getProperty(SessionManagementProperty.clientBlacklist);
        if (Strings.isEmpty(value)) {
            return Collections.emptyList();
        }
        String[] clients = Strings.splitByComma(value);
        if (null == clients) {
            return Collections.emptyList();
        }
        return Arrays.asList(clients);
    }

    private Collection<PortableSession> getRemoteSessionsForUser(Session session) throws OXException {
        LeanConfigurationService configService = Services.getService(LeanConfigurationService.class);
        if (null == configService) {
            throw ServiceExceptionCode.absentService(LeanConfigurationService.class);
        }
        if (!configService.getBooleanProperty(SessionManagementProperty.globalLookup)) {
            return Collections.emptyList();
        }
        HazelcastInstance hzInstance = Services.optService(HazelcastInstance.class);
        if (null == hzInstance) {
            LOG.warn("Missing hazelcast instance.", ServiceExceptionCode.absentService(HazelcastInstance.class));
            return Collections.emptyList();
        }

        Cluster cluster = hzInstance.getCluster();

        // Get local member
        Member localMember = cluster.getLocalMember();

        // Determine other cluster members
        Set<Member> otherMembers = getOtherMembers(cluster.getMembers(), localMember);
        if (otherMembers.isEmpty()) {
            return Collections.emptyList();
        }
        IExecutorService executor = hzInstance.getExecutorService("default");
        Map<Member, Future<PortableSessionCollection>> futureMap = executor.submitToMembers(new PortableMultipleSessionRemoteLookUp(session.getUserId(), session.getContextId()), otherMembers);
        for (Iterator<Entry<Member, Future<PortableSessionCollection>>> it = futureMap.entrySet().iterator(); it.hasNext();) {
            Future<PortableSessionCollection> future = it.next().getValue();
            // Check Future's return value
            int retryCount = 3;
            while (retryCount-- > 0) {
                try {
                    PortableSessionCollection portableSessionCollection = future.get();
                    retryCount = 0;

                    PortableSession[] portableSessions = portableSessionCollection.getSessions();
                    if (null != portableSessions) {
                        return Arrays.asList(portableSessions);
                    }
                } catch (InterruptedException e) {
                    // Interrupted - Keep interrupted state
                    Thread.currentThread().interrupt();
                } catch (CancellationException e) {
                    // Canceled
                    retryCount = 0;
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();

                    // Check for Hazelcast timeout
                    if (!(cause instanceof com.hazelcast.core.OperationTimeoutException)) {
                        if (cause instanceof RuntimeException) {
                            throw ((RuntimeException) cause);
                        }
                        if (cause instanceof Error) {
                            throw (Error) cause;
                        }
                        throw new IllegalStateException("Not unchecked", cause);
                    }

                    // Timeout while awaiting remote result
                    if (retryCount <= 0) {
                        // No further retry
                        cancelFutureSafe(future);
                    }
                }
            }
        }
        return Collections.emptyList();

    }

    private Set<Member> getOtherMembers(Set<Member> allMembers, Member localMember) {
        Set<Member> otherMembers = new LinkedHashSet<Member>(allMembers);
        if (!otherMembers.remove(localMember)) {
            LOG.warn("Couldn't remove local member from cluster members.");
        }
        return otherMembers;
    }

    /**
     * Cancels given {@link Future} safely
     *
     * @param future The {@code Future} to cancel
     */
    static <V> void cancelFutureSafe(Future<V> future) {
        if (null != future) {
            try {
                future.cancel(true);
            } catch (Exception e) {
                /* Ignore */}
        }
    }

}
