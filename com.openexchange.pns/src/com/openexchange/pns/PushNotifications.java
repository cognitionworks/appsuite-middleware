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

package com.openexchange.pns;

import java.util.Map;
import com.openexchange.java.Charsets;

/**
 * {@link PushNotifications} - The utility class for push notification module.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public class PushNotifications {

    private PushNotifications() {
        super();
    }

    /**
     * Gets the length in bytes for specified payload string.
     *
     * @param payload The payload
     * @return The length in bytes
     */
    public static int getPayloadLength(String payload) {
        if (null == payload) {
            return 0;
        }

        byte[] bytes;
        try {
            bytes = payload.getBytes(Charsets.UTF_8);
        } catch (Exception ex) {
            bytes = payload.getBytes();
        }
        return bytes.length;
    }

    /**
     * Checks if specified push notification appears to be a refresh
     *
     * @param notification The push notification to examine
     * @return <code>true</code> if push notification appears is a refresh; otherwise <code>false</code>
     */
    public static boolean isRefresh(PushNotification notification) {
        Map<String, Object> messageData = notification.getMessageData();
        if (null == messageData) {
            return false;
        }

        return "refresh".equals(messageData.get(PushNotificationField.MESSAGE.getId()));
    }

    /**
     * Gets the value from notification's data associated with specified field.
     *
     * @param field The field
     * @param notification The notification to grab from
     * @return The value or <code>null</code>
     */
    public static <V> V getValueFor(PushNotificationField field, PushNotification notification) {
        if (null == field) {
            return null;
        }
        return getValueFor(field.getId(), notification);
    }

    /**
     * Gets the value from notification's data associated with specified field.
     *
     * @param field The field
     * @param notification The notification to grab from
     * @return The value or <code>null</code>
     */
    public static <V> V getValueFor(String field, PushNotification notification) {
        if (null == field || null == notification) {
            return null;
        }
        return (V) notification.getMessageData().get(field);
    }

    // -----------------------------------------------------------------------------------------------------------

    /**
     * Validates the topic name.
     *
     * @param topic The topic name to validate.
     * @throws IllegalArgumentException If the topic name is invalid.
     */
    public static void validateTopicName(String topic) {
        int length = topic.length();
        if (length == 0) {
            throw new IllegalArgumentException("empty topic");
        }
        for (int i = 0; i < length; i++) {
            char ch = topic.charAt(i);
            if (ch == '/') {
                // Can't start or end with a '/' but anywhere else is okay
                if (i == 0 || (i == length - 1)) {
                    throw new IllegalArgumentException("invalid topic: " + topic);
                }
                // Can't have "//" as that implies empty token
                if (topic.charAt(i - 1) == '/') {
                    throw new IllegalArgumentException("invalid topic: " + topic);
                }
                continue;
            }
            if (('A' <= ch) && (ch <= 'Z')) {
                continue;
            }
            if (('a' <= ch) && (ch <= 'z')) {
                continue;
            }
            if (('0' <= ch) && (ch <= '9')) {
                continue;
            }
            if ((ch == '_') || (ch == '-')) {
                continue;
            }
            throw new IllegalArgumentException("invalid topic: " + topic);
        }
    }

}
