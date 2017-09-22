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

package com.openexchange.pns.subscription.storage;

import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.ServiceListing;
import com.openexchange.pns.Hits;
import com.openexchange.pns.PushMatch;
import com.openexchange.pns.PushSubscription;
import com.openexchange.pns.PushSubscriptionRegistry;
import com.openexchange.pns.subscription.storage.inmemory.InMemoryPushSubscriptionRegistry;
import com.openexchange.pns.subscription.storage.rdb.RdbPushSubscriptionRegistry;
import com.openexchange.pns.PushSubscriptionListener;
import com.openexchange.pns.PushSubscription.Nature;
import com.openexchange.pns.PushSubscriptionProvider;


/**
 * {@link CompositePushSubscriptionRegistry}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.0
 */
public class CompositePushSubscriptionRegistry implements PushSubscriptionRegistry {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(CompositePushSubscriptionRegistry.class);

    private final List<PushSubscriptionRegistry> registries;
    private final RdbPushSubscriptionRegistry persistentRegistry;
    private final InMemoryPushSubscriptionRegistry volatileRegistry;
    private final ServiceListing<PushSubscriptionProvider> providers;
    private final ServiceListing<PushSubscriptionListener> listeners;

    /**
     * Initializes a new {@link CompositePushSubscriptionRegistry}.
     */
    public CompositePushSubscriptionRegistry(RdbPushSubscriptionRegistry persistentRegistry, InMemoryPushSubscriptionRegistry volatileRegistry, ServiceListing<PushSubscriptionProvider> providers, ServiceListing<PushSubscriptionListener> listeners, boolean useVolatileRegistry) {
        super();
        this.persistentRegistry = persistentRegistry;
        this.volatileRegistry = useVolatileRegistry ? volatileRegistry : null;

        List<PushSubscriptionRegistry> registries = new ArrayList<>(2);
        if (useVolatileRegistry) {
            registries.add(volatileRegistry);
        }
        registries.add(persistentRegistry);
        this.registries = new CopyOnWriteArrayList<PushSubscriptionRegistry>(registries);
        this.providers = providers;
        this.listeners = listeners;
    }

    @Override
    public boolean hasInterestedSubscriptions(int userId, int contextId, String topic) throws OXException {
        return hasInterestedSubscriptions(null, userId, contextId, topic);
    }

    @Override
    public boolean hasInterestedSubscriptions(String client, int userId, int contextId, String topic) throws OXException {
        for (PushSubscriptionRegistry registry : registries) {
            boolean hasAny = null == client ? registry.hasInterestedSubscriptions(userId, contextId, topic) : registry.hasInterestedSubscriptions(client, userId, contextId, topic);
            if (hasAny) {
                return true;
            }
        }

        for (PushSubscriptionProvider provider : providers) {
            boolean hasAny = null == client ? provider.hasInterestedSubscriptions(userId, contextId, topic) : provider.hasInterestedSubscriptions(client, userId, contextId, topic);
            if (hasAny) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Hits getInterestedSubscriptions(int userId, int contextId, String topic) throws OXException {
        return getInterestedSubscriptions(null, userId, contextId, topic);
    }

    @Override
    public Hits getInterestedSubscriptions(String client, int userId, int contextId, String topic) throws OXException {
        Map<ClientAndTransport, List<PushMatch>> map = null;

        for (PushSubscriptionRegistry registry : registries) {
            Hits currentHits = null == client ? registry.getInterestedSubscriptions(userId, contextId, topic) : registry.getInterestedSubscriptions(client, userId, contextId, topic);
            if (false == currentHits.isEmpty()) {
                Map<ClientAndTransport, List<PushMatch>> currentMap = ((MapBackedHits) currentHits).getMap();

                // Already initialized?
                if (null == map) {
                    map = currentMap;
                } else {
                    // Merge hits
                    for (Map.Entry<ClientAndTransport, List<PushMatch>> entry : currentMap.entrySet()) {
                        List<PushMatch> list = map.get(entry.getKey());
                        if (null == list) {
                            list = new LinkedList<>();
                            map.put(entry.getKey(), list);
                        }
                        for (PushMatch newMatch : entry.getValue()) {
                            if (!list.contains(newMatch)) {
                                list.add(newMatch);
                            }
                        }
                    }
                }
            }
        }

        // Build hits from queried registries
        MapBackedHits hits = null == map ? null : new MapBackedHits(map);

        // Check for more hits from providers
        LinkedList<Hits> moreHits = null;
        for (PushSubscriptionProvider provider : providers) {
            Hits currentHits = null == client ? provider.getInterestedSubscriptions(userId, contextId, topic) : provider.getInterestedSubscriptions(client, userId, contextId, topic);
            if (false == currentHits.isEmpty()) {
                if (null == moreHits) {
                    moreHits = new LinkedList<>();
                }
                moreHits.add(currentHits);
            }
        }

        if (null == moreHits) {
            return null == hits ? Hits.EMPTY_HITS : hits;
        }

        if (null != hits) {
            moreHits.addFirst(hits);
        }

        return new IteratorBackedHits(moreHits);
    }

    @Override
    public void registerSubscription(PushSubscription subscription) throws OXException {
        List<PushSubscriptionListener> listeners = this.listeners.getServiceList();
        for (PushSubscriptionListener listener : listeners) {
            try {
                if(!listener.addingSubscription(subscription)) {
                    LOG.info("Listener {} denied registration of subscription with topics '{}' for user {} in context {}", listener.getClass().getSimpleName(), subscription.getTopics(), I(subscription.getUserId()), I(subscription.getContextId()));
                }
            } catch (Exception e) {
                LOG.info("Listener {} failed handling registration of subscription with topics '{}' for user {} in context {}", listener.getClass().getSimpleName(), subscription.getTopics(), I(subscription.getUserId()), I(subscription.getContextId()), e);
            }
        }

        if (Nature.VOLATILE == subscription.getNature() && null != volatileRegistry) {
            volatileRegistry.registerSubscription(subscription);
        } else {
            persistentRegistry.registerSubscription(subscription);
        }

        for (PushSubscriptionListener listener : listeners) {
            try {
                listener.addedSubscription(subscription);
            } catch (Exception e) {
                LOG.info("Listener {} failed handling performed registration of subscription with topics '{}' for user {} in context {}", listener.getClass().getSimpleName(), subscription.getTopics(), I(subscription.getUserId()), I(subscription.getContextId()), e);
            }
        }
    }

    @Override
    public boolean unregisterSubscription(PushSubscription subscription) throws OXException {
        // Is nature given?
        Nature nature = subscription.getNature();

        boolean removed;
        PushSubscription removedSubscription = null;
        if (Nature.VOLATILE == subscription.getNature() && null != volatileRegistry) {
            removed = volatileRegistry.unregisterSubscription(subscription);
        } else if (Nature.PERSISTENT == nature) {
            removedSubscription = persistentRegistry.removeSubscription(subscription);
            removed = null != removedSubscription;
        } else {
            // Don't know better
            removedSubscription = persistentRegistry.removeSubscription(subscription);
            removed = null != removedSubscription;
            if (null != volatileRegistry) {
                removed |= volatileRegistry.unregisterSubscription(subscription);
            }
        }

        if (removed) {
            PushSubscription subscriptionToUse = null == removedSubscription ? subscription : removedSubscription;
            for (PushSubscriptionListener listener : listeners) {
                try {
                    listener.removedSubscription(subscriptionToUse);
                } catch (Exception e) {
                    LOG.info("Listener {} failed handling performed unregistration of subscription with topics '{}' for user {} in context {}", listener.getClass().getSimpleName(), subscriptionToUse.getTopics(), I(subscriptionToUse.getUserId()), I(subscriptionToUse.getContextId()), e);
                }
            }
        }

        return removed;
    }

    @Override
    public int unregisterSubscription(String token, String transportId) throws OXException {
        int numRemoved = persistentRegistry.unregisterSubscription(token, transportId);
        if (null != volatileRegistry) {
            numRemoved += volatileRegistry.unregisterSubscription(token, transportId);
        }
        return numRemoved;
    }

    @Override
    public boolean updateToken(PushSubscription subscription, String newToken) throws OXException {
        boolean updated = persistentRegistry.updateToken(subscription, newToken);
        if (null != volatileRegistry) {
            updated |= volatileRegistry.updateToken(subscription, newToken);
        }
        return updated;
    }

}
