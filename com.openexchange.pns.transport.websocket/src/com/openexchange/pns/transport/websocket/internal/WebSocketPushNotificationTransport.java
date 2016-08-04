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
 *    trademarks of the OX Software GmbH. group of companies.
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

package com.openexchange.pns.transport.websocket.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.BufferingQueue;
import com.openexchange.pns.DefaultPushSubscription;
import com.openexchange.pns.KnownTransport;
import com.openexchange.pns.Message;
import com.openexchange.pns.PushExceptionCodes;
import com.openexchange.pns.PushMatch;
import com.openexchange.pns.PushMessageGenerator;
import com.openexchange.pns.PushMessageGeneratorRegistry;
import com.openexchange.pns.PushNotification;
import com.openexchange.pns.PushNotificationTransport;
import com.openexchange.pns.PushSubscriptionRegistry;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.UserAndContext;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;
import com.openexchange.pns.PushSubscription.Nature;
import com.openexchange.pns.transport.websocket.WebSocketToClientResolver;
import com.openexchange.websockets.WebSocket;
import com.openexchange.websockets.WebSocketListener;
import com.openexchange.websockets.WebSocketService;

/**
 * {@link WebSocketPushNotificationTransport}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public class WebSocketPushNotificationTransport implements PushNotificationTransport, WebSocketListener {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(WebSocketPushNotificationTransport.class);

    private static final String ID = KnownTransport.WEB_SOCKET.getTransportId();

    private static volatile Long delayDuration;

    private static long delayDuration(ServiceLookup services) {
        Long tmp = delayDuration;
        if (null == tmp) {
            synchronized (WebSocketPushNotificationTransport.class) {
                tmp = delayDuration;
                if (null == tmp) {
                    int defaultValue = 5000;
                    ConfigurationService service = services.getOptionalService(ConfigurationService.class);
                    if (null == service) {
                        return defaultValue;
                    }
                    tmp = Long.valueOf(service.getIntProperty("com.openexchange.pns.transport.websocket.delayDuration", defaultValue));
                    delayDuration = tmp;
                }
            }
        }
        return tmp.longValue();
    }

    private static volatile Long timerFrequency;

    private static long timerFrequency(ServiceLookup services) {
        Long tmp = timerFrequency;
        if (null == tmp) {
            synchronized (WebSocketPushNotificationTransport.class) {
                tmp = timerFrequency;
                if (null == tmp) {
                    int defaultValue = 2000;
                    ConfigurationService service = services.getOptionalService(ConfigurationService.class);
                    if (null == service) {
                        return defaultValue;
                    }
                    tmp = Long.valueOf(service.getIntProperty("com.openexchange.pns.transport.websocket.timerFrequency", defaultValue));
                    timerFrequency = tmp;
                }
            }
        }
        return tmp.longValue();
    }

    // ---------------------------------------------------------------------------------------------------------------

    private final ServiceLookup services;
    private final WebSocketToClientResolverRegistry resolvers;
    private final WebSocketService webSocketService;
    private final PushSubscriptionRegistry subscriptionRegistry;
    private final PushMessageGeneratorRegistry generatorRegistry;
    private final ConcurrentMap<WebSocket, String> socketToClientMapping;
    private final BufferingQueue<Unsubscription> scheduledUnsubscriptions;
    private ScheduledTimerTask scheduledTimerTask; // Accessed synchronized
    private boolean stopped; // Accessed synchronized

    /**
     * Initializes a new {@link WebSocketPushNotificationTransport}.
     */
    public WebSocketPushNotificationTransport(WebSocketToClientResolverRegistry resolvers, ServiceLookup services) {
        super();
        this.resolvers = resolvers;
        this.webSocketService = services.getService(WebSocketService.class);
        this.generatorRegistry = services.getService(PushMessageGeneratorRegistry.class);
        this.subscriptionRegistry = services.getService(PushSubscriptionRegistry.class);
        this.services = services;
        socketToClientMapping = new ConcurrentHashMap<>(1024);
        scheduledUnsubscriptions = new BufferingQueue<>(delayDuration(services));
    }

    /**
     * Stops this Web Socket transport.
     */
    public void stop() {
        synchronized (this) {
            scheduledUnsubscriptions.clear();
            cancelTimerTask();
            socketToClientMapping.clear();
            stopped = true;
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    private String resolveToClient(WebSocket socket) {
        String client;
        for (WebSocketToClientResolver resolver : resolvers) {
            try {
                client = resolver.getClientFor(socket);
                if (null != client) {
                    return client;
                }
            } catch (OXException e) {
                LOG.error("Failed to resolve Web Socket to a client using path {} from user {} in context {}", socket.getPath(), socket.getUserId(), socket.getContextId(), e);
            }
        }
        // Not resolveable
        return null;
    }

    @Override
    public void onMessage(WebSocket socket, String text) {
        // No use for client-initiated push message...
        LOG.debug("Received message: {}", text);
    }

    @Override
    public void onWebSocketConnect(WebSocket socket) {
        String client = resolveToClient(socket);
        if (null == client) {
            // Not resolveable to a certain client... Ignore
            return;
        }

        synchronized (this) {
            // Stopped
            if (stopped) {
                return;
            }

            if (null != socketToClientMapping.putIfAbsent(socket, client)) {
                // Already contained...
                return;
            }

            // Create subscription instance
            DefaultPushSubscription subscription = new DefaultPushSubscription.Builder()
                .client(client)
                .contextId(socket.getContextId())
                .nature(Nature.PERSISTENT)
                .token(socket.getConnectionId().getId())
                .topics(Collections.singletonList("*"))
                .transportId(ID)
                .userId(socket.getUserId())
                .build();

            // Check if there is a queued unsubscription for it
            boolean removed = scheduledUnsubscriptions.remove(new Unsubscription(subscription));
            if (removed) {
                // Already/still subscribed...
            } else {
                // Subscribe...
                try {
                    subscriptionRegistry.registerSubscription(subscription);
                } catch (OXException e) {
                    LOG.error("Failed to add push subscription for Web Socket on path {} for client {} from user {} in context {}", socket.getPath(), client, socket.getUserId(), socket.getContextId(), e);
                }
            }
        }
    }

    @Override
    public void onWebSocketClose(WebSocket socket) {
        String client = socketToClientMapping.remove(socket);
        if (null == client) {
            // Not tracked
            return;
        }

        synchronized (this) {
            // Stopped
            if (stopped) {
                return;
            }

            // Check time task is alive
            ScheduledTimerTask scheduledTimerTask = this.scheduledTimerTask;
            if (null == scheduledTimerTask) {
                // Initialize timer task
                TimerService timerService = services.getService(TimerService.class);
                Runnable timerTask = new Runnable() {

                    @Override
                    public void run() {
                        checkUnsubscription();
                    }
                };
                long delay = timerFrequency(services); // 2sec delay
                this.scheduledTimerTask = timerService.scheduleWithFixedDelay(timerTask, delay, delay);
                LOG.info("Initialized new timer task for unsubscribing Web Socket push subscriptions");
            }

            DefaultPushSubscription subscription = new DefaultPushSubscription.Builder()
                .client(client)
                .contextId(socket.getContextId())
                .nature(Nature.PERSISTENT)
                .token(socket.getConnectionId().getId())
                .topics(Collections.singletonList("*"))
                .transportId(ID)
                .userId(socket.getUserId())
                .build();
            scheduledUnsubscriptions.offerOrReplace(new Unsubscription(subscription));
            LOG.info("Scheduled unsubscribing Web Socket push subscription for client {} from user {} in context", client, socket.getUserId(), socket.getContextId());
        }
    }

    /**
     * Checks for an available unsubscriptions.
     */
    protected void checkUnsubscription() {
        synchronized (this) {
            // Stopped
            if (stopped) {
                return;
            }

            for (Unsubscription unsubscription; (unsubscription = scheduledUnsubscriptions.poll()) != null;) {
                try {
                    subscriptionRegistry.unregisterSubscription(unsubscription.getSubscription());
                    LOG.info("Unsubscribed Web Socket push for client {} from user {} in context", unsubscription.getClient(), unsubscription.getUserId(), unsubscription.getContextId());
                } catch (OXException e) {
                    LOG.info("Failed to unsubscribe Web Socket push for client {} from user {} in context", unsubscription.getClient(), unsubscription.getUserId(), unsubscription.getContextId(), e);
                }
            }

            if (scheduledUnsubscriptions.isEmpty()) {
                // No more planned rescheduling operations
                cancelTimerTask();
            }
        }
    }

    /**
     * Cancels the currently running timer task (if any).
     */
    protected void cancelTimerTask() {
        ScheduledTimerTask scheduledTimerTask = this.scheduledTimerTask;
        if (null != scheduledTimerTask) {
            scheduledTimerTask.cancel();
            this.scheduledTimerTask = null;
            LOG.info("Canceled timer task for unsubscribing Web Sockets push subscriptions");
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    @Override
    public boolean servesClient(String client) throws OXException {
        return resolvers.getAllSupportedClients().contains(client);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void transport(PushNotification notification, Collection<PushMatch> matches) throws OXException {
        if (null != notification && null != matches && !matches.isEmpty()) {
            // Determine associated client and path filter
            Map<String, ClientAndPathFilter> clientToFilter = getResolveResultsFor(matches);

            // Sort incoming matches by user-context pairs
            for (Map.Entry<UserAndContext, List<PushMatch>> userAssociatedMatches : sortByUser(matches).entrySet()) {
                UserAndContext uac = userAssociatedMatches.getKey();
                for (String client : extractClientsFrom(userAssociatedMatches.getValue())) {
                    ClientAndPathFilter clientAndPathFilter = clientToFilter.get(client);
                    if (null != clientAndPathFilter) {
                        transport(notification, clientAndPathFilter, uac);
                    }
                }
            }
        }
    }

    private void transport(PushNotification notification, ClientAndPathFilter clientAndPathFilter, UserAndContext uac) throws OXException {
        String client = clientAndPathFilter.getClient();
        PushMessageGenerator generator = generatorRegistry.getGenerator(client);
        if (null == generator) {
            throw PushExceptionCodes.NO_SUCH_GENERATOR.create(client);
        }

        Message<?> message = generator.generateMessageFor(ID, notification);
        String textMessage = (String) message.getMessage();

        webSocketService.sendMessage(textMessage, clientAndPathFilter.getPathFilter(), uac.getUserId(), uac.getContextId());
    }

    private Map<String, ClientAndPathFilter> getResolveResultsFor(Collection<PushMatch> matches) throws OXException {
        // Only care about clients for Web Sockets
        Set<String> clients = extractClientsFrom(matches);

        Map<String, ClientAndPathFilter> applicable = null;
        for (String client : clients) {
            for (WebSocketToClientResolver resolver : resolvers) {
                String pathFilter = resolver.getPathFilterFor(client);
                if (pathFilter != null) {
                    if (null == applicable) {
                        applicable = new HashMap<>(clients.size());
                    }
                    applicable.put(client, new ClientAndPathFilter(client, pathFilter));
                }
            }
        }
        return applicable;
    }

    private Set<String> extractClientsFrom(Collection<PushMatch> userMatches) {
        Set<String> clients = new HashSet<>();
        for (PushMatch userMatch : userMatches) {
            clients.add(userMatch.getClient());
        }
        return clients;
    }

    private Map<UserAndContext, List<PushMatch>> sortByUser(Collection<PushMatch> matches) {
        Map<UserAndContext, List<PushMatch>> byUser = new LinkedHashMap<>();

        // Only one match is needed as there is no difference per match
        for (PushMatch match : matches) {
            UserAndContext uac = UserAndContext.newInstance(match.getUserId(), match.getContextId());
            List<PushMatch> matchList = byUser.get(uac);
            if (null == matchList) {
                matchList = new LinkedList<>();
                byUser.put(uac, matchList);
            }
            matchList.add(match);
        }

        return byUser;
    }

}
