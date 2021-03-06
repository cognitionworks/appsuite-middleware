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

package com.openexchange.twitter.internal;

import java.util.Date;
import com.openexchange.twitter.RetweetDetails;
import com.openexchange.twitter.User;
import twitter4j.Status;

/**
 * {@link RetweetDetailsImpl} - The retweet details implementation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class RetweetDetailsImpl implements RetweetDetails {

    private final twitter4j.Status retweetStatus;

    private User user;

    /**
     * Initializes a new {@link RetweetDetailsImpl}.
     *
     * @param retweetStatus
     * @param retweetCount
     */
    public RetweetDetailsImpl(final Status retweetStatus, final long retweetCount) {
        super();
        this.retweetStatus = retweetStatus;
    }

    public int getRateLimitLimit() {
        return retweetStatus.getRateLimitStatus().getLimit();
    }

    public int getRateLimitRemaining() {
        return retweetStatus.getRateLimitStatus().getRemaining();
    }

    public long getRateLimitReset() {
        return retweetStatus.getRateLimitStatus().getResetTimeInSeconds();
    }

    @Override
    public Date getRetweetedAt() {
        return retweetStatus.getCreatedAt();
    }

    @Override
    public long getRetweetId() {
        return retweetStatus.getId();
    }

    @Override
    public User getRetweetingUser() {
        if (null == user) {
            user = new UserImpl(retweetStatus.getUser());
        }
        return user;
    }

    @Override
    public String toString() {
        return retweetStatus.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((retweetStatus == null) ? 0 : retweetStatus.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RetweetDetailsImpl)) {
            return false;
        }
        final RetweetDetailsImpl other = (RetweetDetailsImpl) obj;
        if (retweetStatus == null) {
            if (other.retweetStatus != null) {
                return false;
            }
        } else if (!retweetStatus.equals(other.retweetStatus)) {
            return false;
        }
        return true;
    }

}
