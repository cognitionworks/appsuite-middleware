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

package com.openexchange.pns.transport.apns_http2.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.util.RankedService;
import com.openexchange.pns.DefaultPushSubscription;
import com.openexchange.pns.KnownTransport;
import com.openexchange.pns.Message;
import com.openexchange.pns.PushExceptionCodes;
import com.openexchange.pns.PushMatch;
import com.openexchange.pns.PushMessageGenerator;
import com.openexchange.pns.PushMessageGeneratorRegistry;
import com.openexchange.pns.PushNotification;
import com.openexchange.pns.PushSubscriptionRegistry;
import com.turo.pushy.apns.ApnsClient;
import com.turo.pushy.apns.ApnsPushNotification;
import com.turo.pushy.apns.PushNotificationResponse;
import com.turo.pushy.apns.util.SimpleApnsPushNotification;
import com.turo.pushy.apns.util.concurrent.PushNotificationFuture;

/**
 * {@link ApnsHttp2PushPerformer} - Sends the push notifications to the Apple Push Notification System via HTTP/2
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.4
 */
public class ApnsHttp2PushPerformer {

    private static final Logger LOG = LoggerFactory.getLogger(ApnsHttp2PushPerformer.class);

    private final String ID;
    private final List<RankedService<?>> providers;
    private final PushSubscriptionRegistry subscriptionRegistry;
    private final PushMessageGeneratorRegistry generatorRegistry;

    /**
     * Initializes a new {@link ApnsHttp2PushPerformer}.
     * @param ID The ID of the transport mechanism, see {@link KnownTransport}
     * @param providers A prior sorted list of {@link ApnOptionsProvider} or {@link ApnsHttp2OptionsProvider}
     * @param subscriptionRegistry The PNS subscription registry
     * @param generatorRegistry The message generator registry
     */
    public ApnsHttp2PushPerformer(String ID, List<RankedService<?>> providers, PushSubscriptionRegistry subscriptionRegistry, PushMessageGeneratorRegistry generatorRegistry) {
        super();
        this.ID = ID;
        this.providers = providers;
        this.subscriptionRegistry = subscriptionRegistry;
        this.generatorRegistry = generatorRegistry;
    }

    /**
     * Sends the push notification to the Apple Push Notification System
     *
     * @param notifications The raw push notifications
     * @throws OXException
     */
    public void sendPush(Map<PushNotification, List<PushMatch>> notifications) throws OXException {
        if (null == notifications || notifications.isEmpty()) {
            return;
        }
        /*
         * generate message payloads for all matches of all notifications, associated to targeted client
         */
        Map<String, List<Entry<PushMatch, ApnsPushNotification>>> payloadsPerClient = new HashMap<String, List<Entry<PushMatch, ApnsPushNotification>>>();
        Map<String, ApnsHttp2Options> optionsPerClient = new HashMap<String, ApnsHttp2Options>();
        for (Map.Entry<PushNotification, List<PushMatch>> entry : notifications.entrySet()) {
            for (Entry<PushMatch, ApnsPushNotification> payload : getPayloadsPerDevice(entry.getKey(), entry.getValue(), optionsPerClient).entrySet()) {
                String client = payload.getKey().getClient();
                com.openexchange.tools.arrays.Collections.put(payloadsPerClient, client, payload);
            }
        }
        /*
         * perform transport to devices of each client
         */
        for (Map.Entry<String, List<Entry<PushMatch, ApnsPushNotification>>> entry : payloadsPerClient.entrySet()) {
            transport(entry.getKey(), entry.getValue(), optionsPerClient);
        }
    }

    private Map<PushMatch, ApnsPushNotification> getPayloadsPerDevice(PushNotification notification, Collection<PushMatch> matches, Map<String, ApnsHttp2Options> optionsPerClient) throws OXException {
        Map<PushMatch, ApnsPushNotification> payloadsPerDevice = new HashMap<PushMatch, ApnsPushNotification>(matches.size());
        for (PushMatch match : matches) {
            ApnsPushNotification payloadPerDevice = getPayloadPerDevice(notification, match, optionsPerClient);
            if (null != payloadPerDevice) {
                payloadsPerDevice.put(match, payloadPerDevice);
            }
        }
        return payloadsPerDevice;
    }

    private ApnsPushNotification getPayloadPerDevice(PushNotification notification, PushMatch match, Map<String, ApnsHttp2Options> optionsPerClient) throws OXException {
        PushMessageGenerator generator = generatorRegistry.getGenerator(match.getClient());
        if (null == generator) {
            throw PushExceptionCodes.NO_SUCH_GENERATOR.create(match.getClient());
        }
        ApnsHttp2Options options = optionsPerClient.get(match.getClient());
        if (null == options) {
            options = getHighestRankedApnOptionsFor(match.getClient());
            optionsPerClient.put(match.getClient(), options);
        }
        Message<?> message = generator.generateMessageFor(ID, notification);
        return getPayload(message.getMessage(), match.getToken(), options.getTopic());
    }

    private ApnsPushNotification getPayload(Object messageObject, String deviceToken, String topic) throws OXException {
        if (messageObject instanceof ApnsPushNotification) {
            return (ApnsPushNotification) messageObject;
        }
        if (messageObject instanceof Map) {
            return toPayload((Map<String, Object>) messageObject, deviceToken, topic);
        }
        if (messageObject instanceof JSONObject) {
            return toPayload((JSONObject) messageObject, deviceToken, topic);
        }
        if (messageObject instanceof String) {
            return new SimpleApnsPushNotification(deviceToken, topic, (String) messageObject);
        }
        throw PushExceptionCodes.UNSUPPORTED_MESSAGE_CLASS.create(null == messageObject ? "null" : messageObject.getClass().getName());
    }

    private ApnsPushNotification toPayload(Map<String, Object> message, String deviceToken, String topic) {
        ApnsHttp2Notification.Builder builder = new ApnsHttp2Notification.Builder(deviceToken, topic);

        Map<String, Object> source = new HashMap<>(message);
        {
            String sSound = (String) source.remove("sound");
            if (null != sSound) {
                builder.sound(sSound);
            }
        }

        {
            Integer iBadge = (Integer) source.remove("badge");
            if (null != iBadge) {
                builder.badge(iBadge.intValue());
            }
        }

        {
            String sAlert = (String) source.remove("alert");
            if (null != sAlert) {
                builder.alertBody(sAlert);
            }
        }

        {
            String sCategory = (String) source.remove("category");
            if (null != sCategory) {
                builder.category(sCategory);
            }
        }

        // Put remaining as custom dictionary
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (null == value) {
                LOG.warn("Ignoring unsupported null value");
            } else {
                if (value instanceof Number) {
                    builder.customField(entry.getKey(), value);
                } else if (value instanceof String) {
                    builder.customField(entry.getKey(), value);
                } else {
                    LOG.warn("Ignoring usupported value of type {}: {}", value.getClass().getName(), value.toString());
                }
            }
        }

        return builder.build();
    }

    private ApnsPushNotification toPayload(JSONObject message, String deviceToken, String topic) {
        ApnsHttp2Notification.Builder builder = new ApnsHttp2Notification.Builder(deviceToken, topic);
        builder.contentAvailable(true);
        try {
            JSONArray array = message.getJSONArray("args");
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                for (String key : obj.keySet()) {
                    builder.withCustomField(key, obj.get(key));
                }
            }
        } catch (JSONException e) {
            // will not happen
        }
        return builder.build();
    }

    private void transport(String client, List<Entry<PushMatch, ApnsPushNotification>> payloads, Map<String, ApnsHttp2Options> optionsPerClient) {
        List<NotificationResponsePerDevice> notifications = null;
        try {
            notifications = transport(optionsPerClient.get(client), getPayloadsPerDevice(payloads));
        } catch (Exception e) {
            LOG.warn("error submitting push notifications", e);
        }
        processNotificationResults(notifications, payloads);
    }

    private List<NotificationResponsePerDevice> transport(ApnsHttp2Options options, List<ApnsPushNotification> payloads) throws OXException {
        ApnsClient client = options.getApnsClient();
        List<NotificationResponsePerDevice> results = new ArrayList<NotificationResponsePerDevice>(payloads.size());
        for (ApnsPushNotification notification : payloads) {
            PushNotificationFuture<ApnsPushNotification, PushNotificationResponse<ApnsPushNotification>> sendNotificationFuture = client.sendNotification(notification);
            results.add(new NotificationResponsePerDevice(sendNotificationFuture, notification.getToken()));
        }
        return results;
    }

    private void processNotificationResults(List<NotificationResponsePerDevice> notifications, List<Entry<PushMatch, ApnsPushNotification>> payloads) {
        if (null == notifications || notifications.isEmpty()) {
            return;
        }

        for (NotificationResponsePerDevice notificationPerDevice : notifications) {
            PushNotificationFuture<ApnsPushNotification, PushNotificationResponse<ApnsPushNotification>> sendNotificationFuture = notificationPerDevice.sendNotificationFuture;
            String deviceToken = notificationPerDevice.deviceToken;
            try {
                PushNotificationResponse<ApnsPushNotification> pushNotificationResponse = sendNotificationFuture.get();
                if (pushNotificationResponse.isAccepted()) {
                    LOG.debug("Push notification for drive event accepted by APNs gateway for device token: {}", deviceToken);
                } else {
                    if (pushNotificationResponse.getTokenInvalidationTimestamp() != null) {
                        LOG.warn("Unsuccessful push notification due to inactive or invalid device token: {}", deviceToken);
                        PushMatch pushMatch = findMatching(deviceToken, payloads);
                        if (null != pushMatch) {
                            boolean removed = removeSubscription(pushMatch);
                            if (removed) {
                                LOG.info("Removed subscription for device with token: {}.", pushMatch.getToken());
                            }
                            LOG.debug("Could not remove subscriptions for device with token: {}.", pushMatch.getToken());
                        } else {
                            int removed = removeSubscriptions(deviceToken);
                            if (0 < removed) {
                                LOG.info("Removed {} subscriptions for device with token: {}.", Integer.valueOf(removed), deviceToken);
                            }
                        }
                    } else {
                        LOG.warn("Unsuccessful push notification for device with token: {}", deviceToken);
                    }
                }
            } catch (ExecutionException e) {
                LOG.warn("Failed to send push notification for drive event for device token {}", deviceToken, e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("Interrupted while sending push notification for drive event for device token {}", deviceToken, e.getCause());
                return;
            }
        }
    }

    private static PushMatch findMatching(String deviceToken, List<Entry<PushMatch, ApnsPushNotification>> payloads) {
        if (null != deviceToken) {
            for (Map.Entry<PushMatch, ApnsPushNotification> entry : payloads) {
                if (deviceToken.equals(entry.getValue().getToken())) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private int removeSubscriptions(String deviceToken) {
        if (null == deviceToken) {
            LOG.warn("Unsufficient device information to remove subscriptions for: {}", deviceToken);
            return 0;
        }

        try {
            return subscriptionRegistry.unregisterSubscription(deviceToken, ID);
        } catch (OXException e) {
            LOG.error("Error removing subscription", e);
        }
        return 0;
    }

    private boolean removeSubscription(PushMatch match) {
        try {
            DefaultPushSubscription subscription = DefaultPushSubscription.builder()
                .contextId(match.getContextId())
                .token(match.getToken())
                .transportId(ID)
                .userId(match.getUserId())
            .build();
            return subscriptionRegistry.unregisterSubscription(subscription);
        } catch (OXException e) {
            LOG.error("Error removing subscription", e);
        }
        return false;
    }

    private static List<ApnsPushNotification> getPayloadsPerDevice(List<Entry<PushMatch, ApnsPushNotification>> payloads) {
        List<ApnsPushNotification> payloadsPerDevice = new ArrayList<ApnsPushNotification>(payloads.size());
        for (Entry<PushMatch, ApnsPushNotification> entry : payloads) {
            payloadsPerDevice.add(entry.getValue());
        }
        return payloadsPerDevice;
    }

    private ApnsHttp2Options getHighestRankedApnOptionsFor(String client) throws OXException {
        ApnsHttp2Options options = optHighestRankedApnOptionsFor(client);
        if (null == options) {
            throw PushExceptionCodes.UNEXPECTED_ERROR.create("No options found for client: " + client);
        }
        return options;
    }

    private ApnsHttp2Options optHighestRankedApnOptionsFor(String client) {
        for (RankedService<?> rankedService : providers) {
            Object service = rankedService.service;
            if (ApnsHttp2OptionsProvider.class.isInstance(service)) {
                ApnsHttp2Options options = ((ApnsHttp2OptionsProvider) service).getOptions(client);
                if (null != options) {
                    return options;
                }
            }
            if (ApnOptionsProvider.class.isInstance(service)) {
                ApnOptions options = ((ApnOptionsProvider) service).getOptions(client);
                if (null != options) {
                    return convert(options);
                }
            }
        }
        return null;
    }

    private ApnsHttp2Options convert(ApnOptions options) {
        return new ApnsHttp2Options(options.getClientId(), options.getKeystore(), options.getPassword(), options.isProduction(), options.getTopic());
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private static class NotificationResponsePerDevice {

        final PushNotificationFuture<ApnsPushNotification, PushNotificationResponse<ApnsPushNotification>> sendNotificationFuture;
        final String deviceToken;

        NotificationResponsePerDevice(PushNotificationFuture<ApnsPushNotification, PushNotificationResponse<ApnsPushNotification>> sendNotificationFuture, String deviceToken) {
            super();
            this.sendNotificationFuture = sendNotificationFuture;
            this.deviceToken = deviceToken;
        }
    }

}
