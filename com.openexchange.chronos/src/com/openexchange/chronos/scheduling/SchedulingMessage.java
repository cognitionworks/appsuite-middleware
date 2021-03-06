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

package com.openexchange.chronos.scheduling;

import java.io.InputStream;
import com.openexchange.annotation.NonNull;
import com.openexchange.annotation.Nullable;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.scheduling.changes.ScheduleChange;
import com.openexchange.exception.OXException;

/**
 * {@link SchedulingMessage} - A message containing all relevant information for scheduling / iTIP
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.3
 */
public interface SchedulingMessage {

    /**
     * The {@link SchedulingMethod} to process for the recipient
     * 
     * @return The {@link SchedulingMethod}
     */
    @NonNull
    SchedulingMethod getMethod();

    /**
     * The originator of the scheduling event. The originator can be
     * <li> an attendee</li>
     * <li> the organizer</li>
     * of an event. An attendee becomes originator e.g. if he declines an event an thus triggers an scheduling event.
     * The organizer is the originator e.g. if he changes the start time of the event.
     * 
     * In case another user acts on behalf of an calendar user, this acting user should be set like described in
     * <a href="https://tools.ietf.org/html/rfc5545#section-3.8.4.3">RFC 5545, Section 3.8.4.3</a>
     * 
     * @return The originator of the message.
     */
    @NonNull
    CalendarUser getOriginator();

    /**
     * The recipient of the message. Can be either an attendee or the organizer
     * 
     * @return The recipient of the message.
     */
    @NonNull
    CalendarUser getRecipient();

    /**
     * Get a the {@link CalendarObjectResource}.
     * 
     * @return {@link CalendarObjectResource}
     */
    @NonNull
    CalendarObjectResource getResource();

    /**
     * Get the {@link ScheduleChange} of what changes has been performed.
     *
     * @return A {@link ScheduleChange}
     */
    @NonNull
    ScheduleChange getScheduleChange();

    /**
     * Gets the binary attachment data for one of the attachments referenced by the calendar object resource.
     * 
     * @param managedId The identifier of the managed attachment
     * @return The attachment as {@link InputStream}
     * @throws OXException In case attachment can't be loaded
     */
    InputStream getAttachmentData(int managedId) throws OXException;

    /**
     * Gets the recipient-specific settings for the message.
     * 
     * @return The recipient specific settings
     */
    RecipientSettings getRecipientSettings();

    /**
     * Get additional information.
     * 
     * @param key The key for the value
     * @param clazz The class the value has
     * @return The value casted to the given class or <code>null</code> if not found
     */
    @Nullable
    <T> T getAdditional(String key, Class<T> clazz);

}
