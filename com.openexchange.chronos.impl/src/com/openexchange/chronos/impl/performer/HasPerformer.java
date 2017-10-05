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

package com.openexchange.chronos.impl.performer;

import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.initCalendar;
import static com.openexchange.chronos.common.CalendarUtils.isGroupScheduled;
import static com.openexchange.chronos.common.CalendarUtils.isInRange;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.common.CalendarUtils.truncateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.Period;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.SelfProtectionFactory;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.SearchOptions;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.java.util.TimeZones;

/**
 * {@link HasPerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class HasPerformer extends AbstractFreeBusyPerformer {

    /**
     * Initializes a new {@link HasPerformer}.
     *
     * @param storage The underlying calendar storage
     * @param session The calendar session
     */
    public HasPerformer(CalendarSession session, CalendarStorage storage, SelfProtectionFactory protectionFactory) throws OXException {
        super(session, storage, protectionFactory);
    }

    /**
     * Performs the "has events between" operation.
     *
     * @param userID The identifier of the user to evaluate the "has" flags for
     * @param from The start date of the period to consider
     * @param until The end date of the period to consider
     * @return The "has" result, i.e. an array of <code>boolean</code> values representing the days where appointments are in
     */
    public boolean[] perform(int userID, Date from, Date until) throws OXException {
        /*
         * interpret range as "utc" dates
         */
        Calendar calendar = initCalendar(TimeZones.UTC, from);
        Date rangeStart = truncateTime(calendar).getTime();
        calendar.setTime(until);
        truncateTime(calendar);
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date rangeEnd = calendar.getTime();
        /*
         * search overlapping events
         */
        EventField[] fields = Utils.getFields(Utils.DEFAULT_FIELDS.toArray(new EventField[Utils.DEFAULT_FIELDS.size()]), EventField.ORGANIZER, EventField.ATTENDEES);
        List<Attendee> attendees = Collections.singletonList(session.getEntityResolver().applyEntityData(new Attendee(), userID));
        List<Event> events = storage.getEventStorage().searchOverlappingEvents(attendees, true, new SearchOptions().setRange(rangeStart, rangeEnd), fields);
        events = storage.getUtilities().loadAdditionalEventData(-1, events, fields);
        /*
         * step through events day-wise & check for present events
         */
        List<Boolean> hasEventsList = new ArrayList<Boolean>();
        TimeZone timeZone = TimeZones.UTC;//getTimeZone(session);
        calendar = initCalendar(timeZone, from);
        calendar.setTime(rangeStart);
        Date minimumEndTime = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date maximumStartTime = calendar.getTime();
        while (maximumStartTime.before(rangeEnd)) {
            boolean hasEvents = false;
            for (int i = 0; i < events.size() && false == hasEvents; i++) {
                Event event = events.get(i);
                if (isGroupScheduled(event)) {
                    Attendee attendee = find(event.getAttendees(), userID);
                    if (null == attendee || ParticipationStatus.DECLINED.equals(attendee.getPartStat())) {
                        continue; // skip if user does not attend
                    }
                } else if (false == matches(event.getCalendarUser(), userID)) {
                    continue; // skip if user doesn't match event owner
                }
                if (isSeriesMaster(event)) {
                    long duration = event.getEndDate().getTimestamp() - event.getStartDate().getTimestamp();
                    Iterator<RecurrenceId> iterator = Utils.getRecurrenceIterator(storage, session, event, minimumEndTime, maximumStartTime);
                    while (iterator.hasNext() && false == hasEvents) {
                        RecurrenceId recurrenceId = iterator.next();
                        Period occurence = new Period(new Date(recurrenceId.getValue().getTimestamp()), new Date(recurrenceId.getValue().getTimestamp() + duration), event.getStartDate().isAllDay());
                        hasEvents |= isInRange(occurence, minimumEndTime, maximumStartTime, timeZone);
                    }
                } else {
                    hasEvents |= isInRange(event, minimumEndTime, maximumStartTime, timeZone);
                }
            }
            hasEventsList.add(Boolean.valueOf(hasEvents));
            minimumEndTime = maximumStartTime;
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            maximumStartTime = calendar.getTime();
        }
        boolean[] hasEventsArray = new boolean[hasEventsList.size()];
        for (int i = 0; i < hasEventsArray.length; i++) {
            hasEventsArray[i] = hasEventsList.get(i).booleanValue();
        }
        return hasEventsArray;

    }

}
