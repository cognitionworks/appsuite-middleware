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

import static com.openexchange.chronos.common.CalendarUtils.filter;
import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.chronos.impl.Utils.anonymizeIfNeeded;
import static com.openexchange.chronos.impl.Utils.getFields;
import static com.openexchange.tools.arrays.Collections.put;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.FbType;
import com.openexchange.chronos.FreeBusyTime;
import com.openexchange.chronos.FreeBusyType;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.Transp;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.compat.ShownAsTransparency;
import com.openexchange.chronos.impl.Check;
import com.openexchange.chronos.impl.EventMapper;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.SearchOptions;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * {@link FreeBusyPerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class FreeBusyPerformer extends AbstractFreeBusyPerformer {

    /** The event fields returned in free/busy queries by default */
    private static final EventField[] FREEBUSY_FIELDS = {
        EventField.CREATED_BY, EventField.ID, EventField.SERIES_ID, EventField.PUBLIC_FOLDER_ID, EventField.COLOR, EventField.CLASSIFICATION,
        EventField.ALL_DAY, EventField.SUMMARY, EventField.START_DATE, EventField.START_TIMEZONE, EventField.END_DATE, EventField.END_TIMEZONE,
        EventField.CATEGORIES, EventField.TRANSP, EventField.LOCATION, EventField.RECURRENCE_ID, EventField.RECURRENCE_RULE
    };

    /** The restricted event fields returned in free/busy queries if the user has no access to the event */
    private static final EventField[] RESTRICTED_FREEBUSY_FIELDS = {
        EventField.CREATED_BY, EventField.ID, EventField.SERIES_ID, EventField.CLASSIFICATION, EventField.ALL_DAY,
        EventField.START_DATE, EventField.START_TIMEZONE, EventField.END_DATE, EventField.END_TIMEZONE,
        EventField.TRANSP, EventField.RECURRENCE_ID, EventField.RECURRENCE_RULE
    };

    /**
     * Initializes a new {@link FreeBusyPerformer}.
     *
     * @param storage The underlying calendar storage
     * @param session The calendar session
     */
    public FreeBusyPerformer(CalendarSession session, CalendarStorage storage) throws OXException {
        super(session, storage);
    }

    /**
     * Performs the free/busy operation.
     *
     * @param attendees The attendees to query free/busy information for
     * @param from The start date of the period to consider
     * @param until The end date of the period to consider
     * @return The free/busy result
     */
    public Map<Attendee, List<Event>> perform(List<Attendee> attendees, Date from, Date until) throws OXException {
        /*
         * prepare & filter internal attendees for lookup
         */
        Check.hasFreeBusy(ServerSessionAdapter.valueOf(session.getSession()));
        attendees = session.getEntityResolver().prepare(attendees);
        attendees = filter(attendees, Boolean.TRUE, CalendarUserType.INDIVIDUAL, CalendarUserType.RESOURCE, CalendarUserType.GROUP);
        if (0 == attendees.size()) {
            return Collections.emptyMap();
        }
        /*
         * search (potentially) overlapping events for the attendees
         */
        Map<Attendee, List<Event>> eventsPerAttendee = new HashMap<Attendee, List<Event>>(attendees.size());
        for (Attendee attendee : attendees) {
            eventsPerAttendee.put(attendee, new ArrayList<Event>());
        }
        SearchOptions searchOptions = new SearchOptions(session).setRange(from, until);
        EventField[] fields = getFields(FREEBUSY_FIELDS, EventField.DELETE_EXCEPTION_DATES, EventField.CHANGE_EXCEPTION_DATES, EventField.RECURRENCE_ID, EventField.START_TIMEZONE, EventField.END_TIMEZONE);
        List<Event> eventsInPeriod = storage.getEventStorage().searchOverlappingEvents(attendees, true, searchOptions, fields);
        if (0 == eventsInPeriod.size()) {
            return eventsPerAttendee;
        }
        readAttendeeData(eventsInPeriod, Boolean.TRUE);
        /*
         * step through events & build free/busy per requested attendee
         */
        for (Event eventInPeriod : eventsInPeriod) {
            if (false == considerForFreeBusy(eventInPeriod)) {
                continue; // exclude events classified as 'private' (but keep 'confidential' ones)
            }
            for (Attendee attendee : attendees) {
                Attendee eventAttendee = find(eventInPeriod.getAttendees(), attendee);
                if (null == eventAttendee || ParticipationStatus.DECLINED.equals(eventAttendee.getPartStat())) {
                    continue;
                }
                String folderID = CalendarUserType.INDIVIDUAL.equals(eventAttendee.getCuType()) ? chooseFolderID(eventInPeriod) : null;
                if (isSeriesMaster(eventInPeriod)) {
                    Iterator<RecurrenceId> iterator = getRecurrenceIterator(eventInPeriod, from, until);
                    while (iterator.hasNext()) {
                        put(eventsPerAttendee, attendee, getResultingOccurrence(eventInPeriod, iterator.next(), folderID));
                    }
                } else {
                    put(eventsPerAttendee, attendee, getResultingEvent(eventInPeriod, folderID));
                }
            }
        }
        return eventsPerAttendee;
    }

    /**
     * Performs the merged free/busy operation.
     *
     * @param attendees The attendees to query free/busy information for
     * @param from The start date of the period to consider
     * @param until The end date of the period to consider
     * @return The free/busy result
     */
    public Map<Attendee, List<FreeBusyTime>> performMerged(List<Attendee> attendees, Date from, Date until) throws OXException {
        Map<Attendee, List<Event>> eventsPerAttendee = perform(attendees, from, until);
        Map<Attendee, List<FreeBusyTime>> freeBusyDataPerAttendee = new HashMap<Attendee, List<FreeBusyTime>>(eventsPerAttendee.size());
        for (Map.Entry<Attendee, List<Event>> entry : eventsPerAttendee.entrySet()) {
            freeBusyDataPerAttendee.put(entry.getKey(), mergeFreeBusy(entry.getValue(), from, until, Utils.getTimeZone(session)));
        }
        return freeBusyDataPerAttendee;
    }

    /**
     * Gets a resulting userized event occurrence for the free/busy result based on the supplied data of the master event. Only a subset
     * of properties is copied over, and a folder identifier is applied optionally, depending on the user's access permissions for the
     * actual event data.
     *
     * @param masterEvent The event data to get the result for
     * @param recurrenceId The recurrence identifier of the occurrence
     * @param folderID The folder identifier representing the user's view on the event, or <code>null</code> if not accessible in any folder
     * @return The resulting event occurrence representing the free/busy slot
     */
    private Event getResultingOccurrence(Event masterEvent, RecurrenceId recurrenceId, String folderID) throws OXException {
        Event resultingOccurrence = getResultingEvent(masterEvent, folderID);
        resultingOccurrence.setRecurrenceRule(null);
        resultingOccurrence.removeSeriesId();
        resultingOccurrence.removeClassification();
        resultingOccurrence.setRecurrenceId(recurrenceId);
        resultingOccurrence.setStartDate(new Date(recurrenceId.getValue()));
        resultingOccurrence.setEndDate(new Date(recurrenceId.getValue() + (masterEvent.getEndDate().getTime()) - masterEvent.getStartDate().getTime()));
        return resultingOccurrence;
    }

    /**
     * Gets a resulting userized event for the free/busy result based on the supplied event data. Only a subset of properties is copied
     * over, and a folder identifier is applied optionally, depending on the user's access permissions for the actual event data.
     *
     * @param event The event data to get the result for
     * @param folderID The folder identifier representing the user's view on the event, or <code>null</code> if not accessible in any folder
     * @return The resulting event representing the free/busy slot
     */
    private Event getResultingEvent(Event event, String folderID) throws OXException {
        if (null != folderID) {
            Event resultingEvent = EventMapper.getInstance().copy(event, new Event(), FREEBUSY_FIELDS);
            resultingEvent.setFolderId(folderID);
            return anonymizeIfNeeded(session, resultingEvent);
        } else {
            return EventMapper.getInstance().copy(event, new Event(), RESTRICTED_FREEBUSY_FIELDS);
        }
    }

    /**
     * Normalizes the contained free/busy intervals. This means
     * <ul>
     * <li>the intervals are sorted chronologically, i.e. the earliest interval is first</li>
     * <li>all intervals beyond or above the 'from' and 'until' range are removed, intervals overlapping the boundaries are shortened to
     * fit</li>
     * <li>overlapping intervals are merged so that only the most conflicting ones of overlapping time ranges are used</li>
     * </ul>
     *
     * @param timeZone The timezone to consider if the event has <i>floating</i> dates
     */
    private static List<FreeBusyTime> mergeFreeBusy(List<Event> events, Date from, Date until, TimeZone timeZone) {
        if (null == events || 0 == events.size()) {
            return Collections.emptyList(); // nothing to do
        }
        /*
         * get free/busy times & normalize to period
         */
        List<FreeBusyTime> freeBusyTimes = adjustToBoundaries(getFreeBusyTimes(events, timeZone), from, until);
        if (2 > freeBusyTimes.size()) {
            return freeBusyTimes; // nothing more to do
        }
        /*
         * expand times to match all possible boundaries
         */
        Date[] times = getTimes(freeBusyTimes);
        ArrayList<FreeBusyTime> expandedIntervals = new ArrayList<FreeBusyTime>();
        for (FreeBusyTime freeBusyTime : freeBusyTimes) {
            List<Date> expandedTimes = new ArrayList<Date>();
            expandedTimes.add(freeBusyTime.getStartTime());
            for (Date time : times) {
                if (freeBusyTime.getStartTime().before(time) && freeBusyTime.getEndTime().after(time)) {
                    expandedTimes.add(time);
                }
            }
            expandedTimes.add(freeBusyTime.getEndTime());
            if (2 == expandedTimes.size()) {
                expandedIntervals.add(freeBusyTime);
            } else {
                for (int i = 0; i < expandedTimes.size() - 1; i++) {
                    expandedIntervals.add(new FreeBusyTime(freeBusyTime.getFbType(), expandedTimes.get(i), expandedTimes.get(i + 1)));
                }
            }
        }
        /*
         * condense all overlapping intervals to most conflicting one
         */
        Collections.sort(expandedIntervals);
        ArrayList<FreeBusyTime> mergedTimes = new ArrayList<FreeBusyTime>();
        Iterator<FreeBusyTime> iterator = expandedIntervals.iterator();
        FreeBusyTime current = iterator.next();
        while (iterator.hasNext()) {
            FreeBusyTime next = iterator.next();
            if (current.getStartTime().equals(next.getStartTime()) && current.getEndTime().equals(next.getEndTime())) {
                if (0 > current.getFbType().compareTo(next.getFbType())) {
                    /*
                     * less conflicting than next time, skip current timeslot
                     */
                    current = next;
                }
                continue;
            }
            mergedTimes.add(current);
            current = next;
        }
        mergedTimes.add(current);
        /*
         * expand consecutive intervals again
         */
        iterator = mergedTimes.iterator();
        while (iterator.hasNext()) {
            FreeBusyTime freeBusyTime = iterator.next();
            for (FreeBusyTime mergedTime : mergedTimes) {
                if (mergedTime.getFbType().getValue().equals(freeBusyTime.getFbType().getValue())) {
                    /*
                     * merge if next to another
                     */
                    if (mergedTime.getStartTime().equals(freeBusyTime.getEndTime())) {
                        mergedTime.setStartTime(freeBusyTime.getStartTime());
                        iterator.remove();
                        break;
                    } else if (mergedTime.getEndTime().equals(freeBusyTime.getStartTime())) {
                        mergedTime.setEndTime(freeBusyTime.getEndTime());
                        iterator.remove();
                        break;
                    }
                }
            }
        }
        /*
         * take over sorted intervals
         */
        return mergedTimes;
    }

    private static FbType getFbType(Event event) {
        Transp transp = event.getTransp();
        if (null == transp) {
            return FreeBusyType.BUSY;
        }
        if (ShownAsTransparency.class.isInstance(transp)) {
            switch ((ShownAsTransparency) transp) {
                case ABSENT:
                    return FreeBusyType.BUSY_UNAVAILABLE;
                case FREE:
                    return FreeBusyType.FREE;
                case TEMPORARY:
                    return FreeBusyType.BUSY_TENTATIVE;
                default:
                    return FreeBusyType.BUSY;
            }
        }
        return Transp.TRANSPARENT.equals(transp.getValue()) ? FreeBusyType.FREE : FreeBusyType.BUSY;
    }

    private static Date[] getTimes(List<FreeBusyTime> freeBusyTimes) {
        Set<Date> times = new HashSet<Date>();
        for (FreeBusyTime freeBusyTime : freeBusyTimes) {
            times.add(freeBusyTime.getStartTime());
            times.add(freeBusyTime.getEndTime());
        }
        Date[] array = times.toArray(new Date[times.size()]);
        Arrays.sort(array);
        return array;
    }

    /**
     * Normalizes a list of free/busy times to the boundaries of a given period, i.e. removes free/busy times outside range and adjusts
     * the start-/end-times of periods overlapping the start- or enddate of the period.
     *
     * @param freeBusyTimes The free/busy times to normalize
     * @param from The lower inclusive limit of the range
     * @param until The upper exclusive limit of the range
     * @return The normalized free/busy times
     */
    private static List<FreeBusyTime> adjustToBoundaries(List<FreeBusyTime> freeBusyTimes, Date from, Date until) {
        for (Iterator<FreeBusyTime> iterator = freeBusyTimes.iterator(); iterator.hasNext();) {
            FreeBusyTime freeBusyTime = iterator.next();
            if (freeBusyTime.getEndTime().after(from) && freeBusyTime.getStartTime().before(until)) {
                if (freeBusyTime.getStartTime().before(from)) {
                    freeBusyTime.setStartTime(from);
                }
                if (freeBusyTime.getEndTime().after(until)) {
                    freeBusyTime.setEndTime(until);
                }
            } else {
                iterator.remove(); // outside range
            }
        }
        return freeBusyTimes;
    }

    /**
     * Gets a list of free/busy times for the supplied events.
     *
     * @param events The events to get the free/busy times for
     * @param timeZone The timezone to consider if the event has <i>floating</i> dates
     * @return The free/busy times
     */
    private static List<FreeBusyTime> getFreeBusyTimes(List<Event> events, TimeZone timeZone) {
        List<FreeBusyTime> freeBusyTimes = new ArrayList<FreeBusyTime>(events.size());
        for (Event event : events) {
            Date start = event.getStartDate();
            Date end = event.getEndDate();
            if (CalendarUtils.isFloating(event)) {
                start = CalendarUtils.getDateInTimeZone(start, timeZone);
                end = CalendarUtils.getDateInTimeZone(end, timeZone);
            }
            freeBusyTimes.add(new FreeBusyTime(getFbType(event), start, end));
        }
        return freeBusyTimes;
    }

}
