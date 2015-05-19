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

import java.util.Locale;
import com.openexchange.share.notification.ShareNotificationService.Transport;


/**
 * A {@link ShareNotification} encapsulates all information necessary to notify
 * the according recipient about a share and provide her a link to access that share.
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public interface ShareNotification<T> {

    /**
     * Possible notification types. Meant to be extended for future requirements.
     */
    public enum NotificationType {
        /**
         * Notification type for a newly created share.
         * Use to send notifications about a new share to
         * a recipient of any type.
         */
        SHARE_CREATED,

        /**
         * Notification type for internally created shares.
         */
        INTERNAL_SHARE_CREATED,

        /**
         * Notification type for a password-reset that needs to be confirmed.
         * Use to send a request to confirm the password-reset to the share's recipient.
         */
        CONFIRM_PASSWORD_RESET

    }

    /**
     * Gets the transport that shall be used to deliver this notification.
     *
     * @return The {@link Transport}, never <code>null</code>
     */
    Transport getTransport();

    /**
     * Gets the type of this notification (e.g. "a share has been created").
     *
     * @return The {@link NotificationType}, never <code>null</code>
     */
    NotificationType getType();

    /**
     * Gets the transport information used to notify the recipient.
     *
     * @return The transport information, never <code>null</code>
     */
    T getTransportInfo();

    /**
     * Gets the {@link LinkProvider} used for obtaining necessary URLs that are
     * part of the notification messages.
     *
     * @return The provider, never <code>null</code>
     */
    LinkProvider getLinkProvider();

    /**
     * Gets the ID of the context where the share is located.
     *
     * @return The context ID
     */
    int getContextID();

    /**
     * Gets the locale used to translate the notification message before it is sent out.
     *
     * @return The locale
     */
    Locale getLocale();

}
