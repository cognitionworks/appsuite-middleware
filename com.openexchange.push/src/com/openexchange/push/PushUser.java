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

package com.openexchange.push;

import java.util.Optional;

/**
 * {@link PushUser}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.0
 */
public class PushUser implements Comparable<PushUser> {

    private final int userId;
    private final int contextId;
    private final int hash;
    private final String optSessionId;

    /**
     * Initializes a new {@link PushUser}.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    public PushUser(int userId, int contextId) {
        this(userId, contextId, Optional.empty());
    }

    /**
     * Initializes a new {@link PushUser}.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param idOfIssuingSession The optional identifier of the session that issues an operation for this push user
     */
    public PushUser(int userId, int contextId, Optional<String> idOfIssuingSession) {
        super();
        this.userId = userId;
        this.contextId = contextId;
        this.optSessionId = idOfIssuingSession.orElse(null);

        int prime = 31;
        int result = prime * 1 + contextId;
        result = prime * result + userId;
        hash = result;
    }

    @Override
    public int compareTo(PushUser other) {
        int thisInt = this.contextId;
        int otherInt = other.contextId;
        if (thisInt == otherInt) {
            thisInt = this.userId;
            otherInt = other.userId;
        }
        return (thisInt < otherInt) ? -1 : (thisInt == otherInt ? 0 : 1);
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
     * Gets the optional identifier of the session that issues an operation for this push user.
     *
     * @return The session identifier or empty
     */
    public Optional<String> getIdOfIssuingSession() {
        return Optional.ofNullable(optSessionId);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PushUser)) {
            return false;
        }
        PushUser other = (PushUser) obj;
        if (contextId != other.contextId) {
            return false;
        }
        if (userId != other.userId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder(32).append("[userId=").append(userId).append(", contextId=").append(contextId).append(']').toString();
    }

}
