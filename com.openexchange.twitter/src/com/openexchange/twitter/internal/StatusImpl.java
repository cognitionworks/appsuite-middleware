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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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
import com.openexchange.twitter.Status;
import com.openexchange.twitter.User;

/**
 * {@link StatusImpl} - The status implementation.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class StatusImpl implements Status {

    private final twitter4j.Status twitter4jStatus;

    private User user;

    private RetweetDetailsImpl retweetDetails;

    /**
     * Initializes a new {@link StatusImpl}.
     * 
     * @param twitter4jStatus The twitter4j status
     */
    public StatusImpl(final twitter4j.Status twitter4jStatus) {
        super();
        this.twitter4jStatus = twitter4jStatus;
    }

    public Date getCreatedAt() {
        return twitter4jStatus.getCreatedAt();
    }

    public long getId() {
        return twitter4jStatus.getId();
    }

    public String getInReplyToScreenName() {
        return twitter4jStatus.getInReplyToScreenName();
    }

    public long getInReplyToStatusId() {
        return twitter4jStatus.getInReplyToStatusId();
    }

    public int getInReplyToUserId() {
        return twitter4jStatus.getInReplyToUserId();
    }

    public double getLatitude() {
        return twitter4jStatus.getLatitude();
    }

    public double getLongitude() {
        return twitter4jStatus.getLongitude();
    }

    public int getRateLimitLimit() {
        return twitter4jStatus.getRateLimitLimit();
    }

    public int getRateLimitRemaining() {
        return twitter4jStatus.getRateLimitRemaining();
    }

    public long getRateLimitReset() {
        return twitter4jStatus.getRateLimitReset();
    }

    public RetweetDetails getRetweetDetails() {
        if (null == retweetDetails) {
            retweetDetails = new RetweetDetailsImpl(twitter4jStatus.getRetweetDetails());
        }
        return retweetDetails;
    }

    public String getSource() {
        return twitter4jStatus.getSource();
    }

    public String getText() {
        return twitter4jStatus.getText();
    }

    public User getUser() {
        if (null == user) {
            user = new UserImpl(twitter4jStatus.getUser());
        }
        return user;
    }

    public boolean isFavorited() {
        return twitter4jStatus.isFavorited();
    }

    public boolean isRetweet() {
        return twitter4jStatus.isRetweet();
    }

    public boolean isTruncated() {
        return twitter4jStatus.isTruncated();
    }

    @Override
    public String toString() {
        return twitter4jStatus.toString();
    }

}
