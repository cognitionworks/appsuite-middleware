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

package com.openexchange.websockets.grizzly.remote;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * {@link Distribution}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public class Distribution {

    private final Queue<DistributionPayload> payloads;
    private final int userId;
    private final int contextId;
    private volatile Integer hash;

    /**
     * Initializes a new {@link Distribution}.
     */
    public Distribution(String message, String pathFilter, int userId, int contextId, boolean async) {
        super();
        this.userId = userId;
        this.contextId = contextId;
        payloads = new ConcurrentLinkedQueue<>(Arrays.asList(new DistributionPayload(message, pathFilter, async)));
    }

    /**
     * Gets the user identifier
     *
     * @return The user identifier
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Gets the context identifier
     *
     * @return The context identifier
     */
    public int getContextId() {
        return contextId;
    }

    /**
     * Gets the payloads
     *
     * @return The payloads
     */
    public Queue<DistributionPayload> getPayloads() {
        return payloads;
    }

    /**
     * Merges this distribution's payloads with other ones
     *
     * @param other The distribution providing the pay,oads to merge
     */
    public void mergeWith(Distribution other) {
        if (this == other) {
            return;
        }
        if (null == other) {
            return;
        }
        Queue<DistributionPayload> thisPayloads = payloads;
        synchronized (thisPayloads) {
            for (DistributionPayload otherPayload : other.payloads) {
                if (!thisPayloads.contains(otherPayload)) {
                    thisPayloads.add(otherPayload);
                }
            }
        }
    }

    @Override
    public int hashCode() {
        Integer tmp = hash;
        if (null == tmp) {
            // May be computed concurrently...
            int prime = 31;
            int result = 1;
            result = prime * result + contextId;
            result = prime * result + userId;
            tmp = Integer.valueOf(result);
            hash = tmp;
        }
        return tmp.intValue();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Distribution)) {
            return false;
        }
        Distribution other = (Distribution) obj;
        if (contextId != other.contextId) {
            return false;
        }
        if (userId != other.userId) {
            return false;
        }
        return true;
    }

    // --------------------------------------------------------------------------------------------------------

    /** A distribution payload */
    public static class DistributionPayload {

        final String message;
        final String pathFilter;
        final boolean async;
        private volatile Integer hash;

        DistributionPayload(String message, String pathFilter, boolean async) {
            super();
            this.message = message;
            this.pathFilter = pathFilter;
            this.async = async;
        }

        @Override
        public int hashCode() {
            Integer tmp = hash;
            if (null == tmp) {
                // May be computed concurrently...
                int prime = 31;
                int result = 1;
                result = prime * result + (async ? 1231 : 1237);
                result = prime * result + ((message == null) ? 0 : message.hashCode());
                result = prime * result + ((pathFilter == null) ? 0 : pathFilter.hashCode());
                tmp = Integer.valueOf(result);
                hash = tmp;
            }
            return tmp.intValue();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof DistributionPayload)) {
                return false;
            }
            DistributionPayload other = (DistributionPayload) obj;
            if (async != other.async) {
                return false;
            }
            if (message == null) {
                if (other.message != null) {
                    return false;
                }
            } else if (!message.equals(other.message)) {
                return false;
            }
            if (pathFilter == null) {
                if (other.pathFilter != null) {
                    return false;
                }
            } else if (!pathFilter.equals(other.pathFilter)) {
                return false;
            }
            return true;
        }
    }

}
