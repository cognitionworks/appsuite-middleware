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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.chronos.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarService;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.groupware.ldap.User;

/**
 * {@link CalendarService}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class Consistency {

    public static Attendee addUserAttendeeIfMissing(Event event, int userID, int folderID) {
        List<Attendee> attendees = event.getAttendees();
        if (null == attendees) {
            attendees = new ArrayList<Attendee>();
            event.setAttendees(attendees);
        }
        Attendee attendee = CalendarUtils.find(attendees, userID);
        if (null == attendee) {
            attendee = new Attendee();
            attendee.setEntity(userID);
            attendees.add(attendee);
        }
        attendee.setCuType(CalendarUserType.INDIVIDUAL);
        attendee.setFolderID(folderID);
        if (null == attendee.getPartStat()) {
            attendee.setPartStat(ParticipationStatus.ACCEPTED);
        }
        return attendee;
    }

    /**
     * Sets an event's organizer to a specific calendar user.
     *
     * @param event The event to set the organizer for
     * @param user The user to become the organizer
     * @return The organizer, as added to the event
     */
    public static Organizer setOrganizer(Event event, User user) {
        return setOrganizer(event, user, null);
    }

    /**
     * Sets an event's organizer to a specific calendar user.
     *
     * @param event The event to set the organizer for
     * @param user The user to become the organizer
     * @param sentBy Another user who is acting on behalf of the organizer, or <code>null</code> if not set
     * @return The organizer, as added to the event
     */
    public static Organizer setOrganizer(Event event, User user, User sentBy) {
        Organizer organizer = event.getOrganizer();
        if (null == organizer) {
            organizer = new Organizer();
            event.setOrganizer(organizer);
        }
        organizer = CalendarUtils.applyProperties(organizer, user);
        if (null != sentBy) {
            organizer.setSentBy(CalendarUtils.getCalAddress(sentBy));
        }
        return organizer;
    }

    public static void setTimeZone(Event event, User user) {
        String startTimezone = event.getStartTimezone();
        if (null == startTimezone) {
            event.setStartTimezone(user.getTimeZone());
        } else {
            //TODO: validate timezone?
        }
    }

    public static void setModifiedNow(Event event, int modifiedBy) {
        event.setLastModified(new Date());
        event.setModifiedBy(modifiedBy);
    }

    public static void setCreatedNow(Event event, int createdBy) {
        event.setCreated(new Date());
        event.setCreatedBy(createdBy);
    }

    private Consistency() {
        super();
    }

}
