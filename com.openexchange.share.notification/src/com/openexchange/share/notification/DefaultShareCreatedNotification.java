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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.share.notification;

import java.util.List;
import com.openexchange.session.Session;
import com.openexchange.share.AuthenticationMode;
import com.openexchange.share.ShareTarget;
import com.openexchange.share.notification.ShareNotificationService.Transport;

/**
 * A default implementation of {@link ShareCreatedNotification} that contains all
 * necessary data as fields. Plain setters can be used to initialize an instance.
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public class DefaultShareCreatedNotification<T> extends AbstractNotification<T> implements ShareCreatedNotification<T> {

    private Session session;

    private AuthenticationMode authMode;

    private String username;

    private String password;

    private List<ShareTarget> targets;

    private String message;
    
    private ShareCreationDetails creationDetails;
    
    private int guestContextID;
    
    private int guestID;

    /**
     * Initializes a new {@link DefaultShareCreatedNotification}.
     */
    public DefaultShareCreatedNotification(Transport transport) {
        super(transport, NotificationType.SHARE_CREATED);
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public AuthenticationMode getAuthMode() {
        return authMode;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public List<ShareTarget> getShareTargets() {
        return targets;
    }

    @Override
    public String getMessage() {
        return message;
    }
    
    public List<ShareTarget> getTargets() {
        return targets;
    }
    
    public int getGuestContextID() {
        return guestContextID;
    }
    
    public int getGuestID() {
        return guestID;
    }

    public void setTargets(List<ShareTarget> targets) {
        this.targets = targets;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void setAuthMode(AuthenticationMode authMode) {
        this.authMode = authMode;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    public void setGuestContextID(int guestContextID) {
        this.guestContextID = guestContextID;
    }
    
    public void setGuestID(int guestID) {
        this.guestID = guestID;
    }
    
    @Override
    public ShareCreationDetails getCreationDetails() {
        return creationDetails;
    }

    public void setCreationDetails(ShareCreationDetails creationDetails) {
        this.creationDetails = creationDetails;
    }

}
