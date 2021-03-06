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

package com.openexchange.pns.subscription.storage.rdb.cache;

import com.openexchange.pns.PushMatch;


/**
 * {@link InMemoryPushMatch}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public class InMemoryPushMatch implements PushMatch {

    private final int contextId;
    private final int userId;
    private final String client;
    private final String transportId;
    private final String token;
    private final String topic;
    private int hash; // Default to 0

    /**
     * Initializes a new {@link InMemoryPushMatch}.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param client The client identifier
     * @param transportId The transport identifier
     * @param token The token
     * @param topic The matching topic
     */
    public InMemoryPushMatch(int userId, int contextId, String client, String transportId, String token, String topic) {
        super();
        this.userId = userId;
        this.contextId = contextId;
        this.client = client;
        this.transportId = transportId;
        this.token = token;
        this.topic = topic;
    }

    @Override
    public String getClient() {
        return client;
    }

    @Override
    public int getUserId() {
        return userId;
    }

    @Override
    public int getContextId() {
        return contextId;
    }

    @Override
    public String getTransportId() {
        return transportId;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public int hashCode() {
        int result = hash;
        if (result == 0 ) {
            int prime = 31;
            result = 1;
            result = prime * result + contextId;
            result = prime * result + userId;
            result = prime * result + ((token == null) ? 0 : token.hashCode());
            result = prime * result + ((client == null) ? 0 : client.hashCode());
            result = prime * result + ((topic == null) ? 0 : topic.hashCode());
            result = prime * result + ((transportId == null) ? 0 : transportId.hashCode());

            hash = result;
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PushMatch)) {
            return false;
        }
        PushMatch other = (PushMatch) obj;
        if (contextId != other.getContextId()) {
            return false;
        }
        if (userId != other.getUserId()) {
            return false;
        }
        if (token == null) {
            if (other.getToken() != null) {
                return false;
            }
        } else if (!token.equals(other.getToken())) {
            return false;
        }
        if (client == null) {
            if (other.getClient() != null) {
                return false;
            }
        } else if (!client.equals(other.getClient())) {
            return false;
        }
        if (topic == null) {
            if (other.getTopic() != null) {
                return false;
            }
        } else if (!topic.equals(other.getTopic())) {
            return false;
        }
        if (transportId == null) {
            if (other.getTransportId() != null) {
                return false;
            }
        } else if (!transportId.equals(other.getTransportId())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(128);
        builder.append("{contextId=").append(contextId).append(", userId=").append(userId);
        if (client != null) {
            builder.append(", ").append("client=").append(client);
        }
        if (transportId != null) {
            builder.append(", ").append("transportId=").append(transportId);
        }
        if (token != null) {
            builder.append(", ").append("token=").append(token);
        }
        if (topic != null) {
            builder.append(", ").append("topic=").append(topic);
        }
        builder.append("}");
        return builder.toString();
    }

}
