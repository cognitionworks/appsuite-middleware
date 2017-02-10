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

package com.openexchange.chronos.common;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.I2i;
import static org.slf4j.LoggerFactory.getLogger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.mail.internet.idn.IDNA;
import com.openexchange.chronos.Attachment;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.Period;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.service.EventConflict;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXException.ProblematicAttribute;
import com.openexchange.java.Strings;
import com.openexchange.search.Operand;
import com.openexchange.search.SingleSearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.search.internal.operands.ColumnFieldOperand;
import com.openexchange.search.internal.operands.ConstantOperand;

/**
 * {@link CalendarUtils}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class CalendarUtils {

    /**
     * Gets a value indicating whether a recurrence id's value matches a specific timestamp value.
     *
     * @param recurrenceId The recurrence identifier to check
     * @param value The value to check
     * @return <code>true</code> if the recurrence id's value matches the timestamp value, <code>false</code>, otherwise
     */
    public static boolean matches(RecurrenceId recurrenceId, long value) {
        return null != recurrenceId && recurrenceId.getValue() == value;
    }

    /**
     * Looks up a specific recurrence identifier in a collection based on its timestamp value.
     *
     * @param recurrenceIds The recurrence id's to search
     * @param value The timestamp value to lookup
     * @return The matching recurrence identifier, or <code>null</code> if not found
     * @see CalendarUtils#matches(RecurrenceId, long)
     */
    public static RecurrenceId find(Collection<RecurrenceId> recurrenceIds, long value) {
        if (null != recurrenceIds && 0 < recurrenceIds.size()) {
            for (RecurrenceId recurrenceId : recurrenceIds) {
                if (matches(recurrenceId, value)) {
                    return recurrenceId;
                }
            }
        }
        return null;
    }

    /**
     * Gets a value indicating whether a recurrence id with a specific timestamp value is present in a collection of recurrence
     * identifiers, utilizing the {@link CalendarUtils#matches(RecurrenceId, long) routine.
     *
     * @param recurrenceIds The recurrence id's to search
     * @param value The timestamp value to lookup
     * @return <code>true</code> if a matching recurrence identifier is contained in the collection, <code>false</code>, otherwise
     * @see CalendarUtils#matches(RecurrenceId, long)
     */
    public static boolean contains(Collection<RecurrenceId> recurrendeIds, long value) {
        return null != find(recurrendeIds, value);
    }

    /**
     * Gets a value indicating whether a specific recurrence id is present in a collection of recurrence identifiers, based on its value.
     *
     * @param recurrenceIds The recurrence id's to search
     * @param recurrenceId The recurrence id to lookup
     * @return <code>true</code> if a matching recurrence identifier is contained in the collection, <code>false</code>, otherwise
     * @see CalendarUtils#matches(RecurrenceId, long)
     */
    public static boolean contains(Collection<RecurrenceId> recurrendeIds, RecurrenceId recurrenceId) {
        return null != find(recurrendeIds, recurrenceId.getValue());
    }

    /**
     * Looks up a specific internal attendee in a collection of attendees, utilizing the
     * {@link CalendarUtils#matches(Attendee, Attendee)} routine.
     *
     * @param attendees The attendees to search
     * @param attendee The attendee to lookup
     * @return The matching attendee, or <code>null</code> if not found
     * @see CalendarUtils#matches(Attendee, Attendee)
     */
    public static Attendee find(List<Attendee> attendees, Attendee attendee) {
        if (null != attendees && 0 < attendees.size()) {
            for (Attendee candidateAttendee : attendees) {
                if (matches(attendee, candidateAttendee)) {
                    return candidateAttendee;
                }
            }
        }
        return null;
    }

    /**
     * Gets a value indicating whether a specific attendee is present in a collection of attendees, utilizing the
     * {@link CalendarUtils#matches(Attendee, Attendee)} routine.
     *
     * @param attendees The attendees to search
     * @param attendee The attendee to lookup
     * @return <code>true</code> if the attendee is contained in the collection of attendees, <code>false</code>, otherwise
     * @see CalendarUtils#matches(Attendee, Attendee)
     */
    public static boolean contains(List<Attendee> attendees, Attendee attendee) {
        return null != find(attendees, attendee);
    }

    /**
     * Gets a value indicating whether one calendar user matches another, by comparing the entity identifier for internal calendar users,
     * or trying to match the calendar user's URI for external ones.
     *
     * @param user1 The first calendar user to check
     * @param user2 The second calendar user to check
     * @return <code>true</code> if the objects <i>match</i>, i.e. are targeting the same calendar user, <code>false</code>, otherwise
     */
    public static boolean matches(CalendarUser user1, CalendarUser user2) {
        if (null == user1) {
            return null == user2;
        } else if (null != user2) {
            if (0 < user1.getEntity() && user1.getEntity() == user2.getEntity()) {
                return true;
            }
            if (null != user1.getUri() && user1.getUri().equalsIgnoreCase(user2.getUri())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Looks up a specific internal attendee in a collection of attendees based on its entity identifier.
     *
     * @param attendees The attendees to search
     * @param entity The entity identifier to lookup
     * @return The matching attendee, or <code>null</code> if not found
     */
    public static Attendee find(List<Attendee> attendees, int entity) {
        if (null != attendees && 0 < attendees.size()) {
            for (Attendee attendee : attendees) {
                if (entity == attendee.getEntity()) {
                    return attendee;
                }
            }
        }
        return null;
    }

    /**
     * Looks up a specific (managed) attachment in a collection of attachments based on its managed identifier.
     *
     * @param attachments The attachments to search
     * @param managedId The managed identifier to lookup
     * @return The matching attachment, or <code>null</code> if not found
     */
    public static Attachment findAttachment(List<Attachment> attachments, int managedId) {
        if (null != attachments && 0 < attachments.size()) {
            for (Attachment attachment : attachments) {
                if (managedId == attachment.getManagedId()) {
                    return attachment;
                }
            }
        }
        return null;
    }

    /**
     * Gets a value indicating whether an attendee represents an <i>internal</i> entity, i.e. an internal user, group or resource, or not.
     *
     * @param attendee The attendee to check
     * @return <code>true</code> if the attendee is internal, <code>false</code>, otherwise
     */
    public static boolean isInternal(Attendee attendee) {
        return 0 < attendee.getEntity() || 0 == attendee.getEntity() && CalendarUserType.GROUP.equals(attendee.getCuType());
    }

    /**
     * Gets a value indicating whether a collection of attendees contains a specific internal attendee based on its entity identifier or
     * not.
     *
     * @param attendees The attendees to search
     * @param entity The entity identifier to lookup
     * @return <code>true</code> if the attendee was found, <code>false</code>, otherwise
     */
    public static boolean contains(List<Attendee> attendees, int entity) {
        return null != find(attendees, entity);
    }

    /**
     * Gets a value indicating whether a specific user is the organizer of an event or not.
     *
     * @param event The event
     * @param userId The identifier of the user to check
     * @return <code>true</code> if the user with the supplied identifier is the organizer, <code>false</code>, otherwise
     */
    public static boolean isOrganizer(Event event, int userId) {
        return null != event.getOrganizer() && userId == event.getOrganizer().getEntity();
    }

    /**
     * Gets a value indicating whether a specific event is organized externally, i.e. no internal organizer entity is responsible.
     *
     * @param event The event to check
     * @return <code>true</code> if the event has an <i>external</i> organizer, <code>false</code>, otherwise
     */
    public static boolean hasExternalOrganizer(Event event) {
        return null != event.getOrganizer() && 0 >= event.getOrganizer().getEntity();
    }

    /**
     * Gets a value indicating whether a specific user is an attendee of an event or not.
     *
     * @param event The event
     * @param userId The identifier of the user to check
     * @return <code>true</code> if the user with the supplied identifier is an attendee, <code>false</code>, otherwise
     */
    public static boolean isAttendee(Event event, int userId) {
        return contains(event.getAttendees(), userId);
    }

    /**
     * Gets a value indicating whether a specific user is the only / the last internal user attendee in an attendee list.
     *
     * @param attendees The attendees to check
     * @param userID The identifier of the user to lookup in the attendee list
     * @return <code>true</code> if there are no other internal user attendees despite the specified one, <code>false</code>, otherwise
     */
    public static boolean isLastUserAttendee(List<Attendee> attendees, int userID) {
        List<Attendee> userAttendees = filter(attendees, Boolean.TRUE, CalendarUserType.INDIVIDUAL);
        return 1 == userAttendees.size() && userID == userAttendees.get(0).getEntity();
    }

    /**
     * Truncates the time part of the supplied date, i.e. sets the fields {@link Calendar#HOUR_OF_DAY}, {@link Calendar#MINUTE},
     * {@link Calendar#SECOND} and {@link Calendar#MILLISECOND} to <code>0</code>.
     *
     * @param date The date to truncate the time part for
     * @param timeZone The timezone to consider
     * @return A new date instance based on the supplied date with the time fraction truncated
     */
    public static Date truncateTime(Date date, TimeZone timeZone) {
        return truncateTime(initCalendar(timeZone, date)).getTime();
    }

    /**
     * Truncates the time part in the supplied calendar reference, i.e. sets the fields {@link Calendar#HOUR_OF_DAY},
     * {@link Calendar#MINUTE}, {@link Calendar#SECOND} and {@link Calendar#MILLISECOND} to <code>0</code>.
     *
     * @param calendar The calendar reference to truncate the time part in
     * @param timeZone The timezone to consider
     * @return The calendar reference
     */
    public static Calendar truncateTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    /**
     * Converts a so-called <i>floating</i> date into a date in a concrete timezone by applying the actual timezone offset on that date.
     *
     * @param floatingDate The floating date to convert (usually the raw date in <code>UTC</code>)
     * @param timeZone The target timezone
     * @return The date in the target timezone, with the corresponding timezone offset applied
     */
    public static Date getDateInTimeZone(Date floatingDate, TimeZone timeZone) {
        return new Date(floatingDate.getTime() - timeZone.getOffset(floatingDate.getTime()));
    }

    /**
     * Gets the identifiers of the supplied events in an array.
     *
     * @param events The events to get the identifiers for
     * @return The object identifiers
     */
    public static int[] getObjectIDs(List<Event> events) {
        int[] objectIDs = new int[events.size()];
        for (int i = 0; i < events.size(); i++) {
            objectIDs[i] = events.get(i).getId();
        }
        return objectIDs;
    }

    /**
     * Maps a collection of events by their identifier.
     *
     * @param events The events to map
     * @return The mapped events
     */
    public static Map<Integer, Event> getEventsByID(Collection<Event> events) {
        if (null == events) {
            return null;
        }
        Map<Integer, Event> eventsByID = new HashMap<Integer, Event>(events.size());
        for (Event event : events) {
            eventsByID.put(I(event.getId()), event);
        }
        return eventsByID;
    }

    /**
     * Gets a value indicating whether the supplied event is considered as the <i>master</i> event of a recurring series or not, based
     * on checking the properties {@link EventField#ID} and {@link EventField#SERIES_ID} for equality and the absence of an assigned
     * {@link EventField#RECURRENCE_ID}.
     *
     * @param event The event to check
     * @return <code>true</code> if the event is the series master, <code>false</code>, otherwise
     */
    public static boolean isSeriesMaster(Event event) {
        return null != event && event.getId() == event.getSeriesId() && null == event.getRecurrenceId();
    }

    /**
     * Gets a value indicating whether the supplied event is considered as an exceptional event of a recurring series or not, based on
     * the properties {@link EventField#ID} and {@link EventField#SERIES_ID}.
     *
     * @param event The event to check
     * @return <code>true</code> if the event is the series master, <code>false</code>, otherwise
     */
    public static boolean isSeriesException(Event event) {
        return null != event && 0 < event.getSeriesId() && event.getSeriesId() != event.getId();
    }

    /**
     * Initializes a new calendar in a specific timezone and sets the initial time.
     *
     * @param timeZone The timezone to use for the calendar
     * @param time The initial time to set, or <code>null</code> to intialize with the default time
     * @return A new calendar instance
     */
    public static Calendar initCalendar(TimeZone timeZone, Date time) {
        Calendar calendar = GregorianCalendar.getInstance(timeZone);
        if (null != time) {
            calendar.setTime(time);
        }
        return calendar;
    }

    /**
     * Initializes a new calendar in a specific timezone and sets the initial time.
     *
     * @param timeZone The timezone to use for the calendar
     * @param time The initial time in UTC milliseconds from the epoch
     * @return A new calendar instance
     */
    public static Calendar initCalendar(TimeZone timeZone, long time) {
        Calendar calendar = GregorianCalendar.getInstance(timeZone);
        calendar.setTimeInMillis(time);
        return calendar;
    }

    /**
     * Initializes a new calendar in a specific timezone and sets the initial time.
     * <p/>
     * The field {@link Calendar#MILLISECOND} is set to <code>0</code> explicitly.
     *
     * @param timeZone The timezone to use for the calendar
     * @param year The value used to set the {@link Calendar#YEAR} field
     * @param month The value used to set the {@link Calendar#MONTH} field
     * @param date The value used to set the {@link Calendar#DATE} field
     * @return A new calendar instance
     */
    public static Calendar initCalendar(TimeZone timeZone, int year, int month, int date) {
        return initCalendar(timeZone, year, month, date, 0, 0, 0);
    }

    /**
     * Initializes a new calendar in a specific timezone and sets the initial time.
     * <p/>
     * The field {@link Calendar#MILLISECOND} is set to <code>0</code> explicitly.
     *
     * @param timeZone The timezone to use for the calendar
     * @param year The value used to set the {@link Calendar#YEAR} field
     * @param month The value used to set the {@link Calendar#MONTH} field
     * @param date The value used to set the {@link Calendar#DATE} field
     * @param hourOfDay The value used to set the {@link Calendar#HOUR_OF_DAY} field
     * @param minute The value used to set the {@link Calendar#MINUTE} field
     * @param second The value used to set the {@link Calendar#SECOND} field
     * @return A new calendar instance
     */
    public static Calendar initCalendar(TimeZone timeZone, int year, int month, int date, int hourOfDay, int minute, int second) {
        Calendar calendar = GregorianCalendar.getInstance(timeZone);
        calendar.set(year, month, date, hourOfDay, minute, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    /**
     * Gets a value indicating whether a specific event falls (at least partly) into a time range.
     * <p/>
     * According to RFC 4791, an event overlaps a given time range if the condition for the corresponding component state specified in
     * the table below is satisfied:
     * <pre>
     * +---------------------------------------------------------------+
     * | VEVENT has the DTEND property?                                |
     * | +-------------------------------------------------------------+
     * | | VEVENT has the DURATION property?                           |
     * | | +-----------------------------------------------------------+
     * | | | DURATION property value is greater than 0 seconds?        |
     * | | | +---------------------------------------------------------+
     * | | | | DTSTART property is a DATE-TIME value?                  |
     * | | | | +-------------------------------------------------------+
     * | | | | | Condition to evaluate                                 |
     * +---+---+---+---+-----------------------------------------------+
     * | Y | N | N | * | (start < DTEND AND end > DTSTART)             |
     * +---+---+---+---+-----------------------------------------------+
     * | N | Y | Y | * | (start < DTSTART+DURATION AND end > DTSTART)  |
     * | | +---+---+---------------------------------------------------+
     * | | | N | * | (start <= DTSTART AND end > DTSTART)              |
     * +---+---+---+---+-----------------------------------------------+
     * | N | N | N | Y | (start <= DTSTART AND end > DTSTART)          |
     * +---+---+---+---+-----------------------------------------------+
     * | N | N | N | N | (start < DTSTART+P1D AND end > DTSTART)       |
     * +---+---+---+---+-----------------------------------------------+
     * </pre>
     *
     * @param event The event to check
     * @param from The lower inclusive limit of the range, i.e. the event should start on or after this date, or <code>null</code> for no limit
     * @param until The upper exclusive limit of the range, i.e. the event should end before this date, or <code>null</code> for no limit
     * @param timeZone The timezone to consider if the event has <i>floating</i> dates
     * @return <code>true</code> if the event falls into the time range, <code>false</code>, otherwise
     * @see <a href="https://tools.ietf.org/html/rfc4791#section-9.9">RFC 4791, section 9.9</a>
     */
    public static boolean isInRange(Event event, Date from, Date until, TimeZone timeZone) {
        /*
         * determine effective timestamps for check
         */
        long start = null == from ? Long.MIN_VALUE : from.getTime();
        long end = null == until ? Long.MAX_VALUE : until.getTime();
        long dtStart = event.getStartDate().getTime();
        long dtEnd = event.getEndDate().getTime();
        if (isFloating(event)) {
            dtStart -= timeZone.getOffset(dtStart);
            dtEnd -= timeZone.getOffset(dtEnd);
        }
        /*
         * check if a 'real' end date is set in event
         */
        boolean hasDtEnd;
        if (event.isAllDay()) {
            Calendar calendar = initCalendar(timeZone, dtStart);
            calendar.add(Calendar.DATE, 1);
            hasDtEnd = calendar.getTimeInMillis() != dtEnd;
        } else {
            hasDtEnd = dtStart != dtEnd;
        }
        /*
         * perform checks
         */
        if (hasDtEnd) {
            // VEVENT has the DTEND property? Y
            // (start <  DTEND AND end > DTSTART)
            return start < dtEnd && end > dtStart;
        } else {
            // VEVENT has the DTEND property? N
            if (false == event.isAllDay()) {
                // DTSTART property is a DATE-TIME value? Y
                // (start <= DTSTART AND end > DTSTART)
                return start <= dtStart && end > dtStart;
            } else {
                // DTSTART property is a DATE-TIME value? N
                // (start <  DTSTART+P1D AND end > DTSTART)
                // DTSTART+P1D == dtEnd
                return start < dtEnd && end > dtStart;
            }
        }
    }

    /**
     * Gets a value indicating whether a specific event falls (at least partly) into the time range of another event.
     *
     * @param event The event to check
     * @param event2 The second event to check against
     * @param timeZone The timezone to consider if one or the other event has <i>floating</i> dates
     * @return <code>true</code> if the event falls into the time range of the other, <code>false</code>, otherwise
     */
    public static boolean isInRange(Event event, Event event2, TimeZone timeZone) {
        Date from = isFloating(event2) ? getDateInTimeZone(event2.getStartDate(), timeZone) : event2.getStartDate();
        Date until = isFloating(event2) ? getDateInTimeZone(event2.getEndDate(), timeZone) : event2.getEndDate();
        return isInRange(event, from, until, timeZone);
    }

    /**
     * Gets a value indicating whether a specific period falls (at least partly) into a time range.
     *
     * @param period The period to check
     * @param from The lower inclusive limit of the range, i.e. the event should start on or after this date, or <code>null</code> for no limit
     * @param until The upper exclusive limit of the range, i.e. the event should end before this date, or <code>null</code> for no limit
     * @param timeZone The timezone to consider if the period is <i>all-day</i> (so has <i>floating</i> dates)
     * @return <code>true</code> if the event falls into the time range, <code>false</code>, otherwise
     */
    public static boolean isInRange(Period period, Date from, Date until, TimeZone timeZone) {
        Date startDate = period.isAllDay() ? getDateInTimeZone(period.getStartDate(), timeZone) : period.getStartDate();
        Date endDate = period.isAllDay() ? getDateInTimeZone(period.getEndDate(), timeZone) : period.getEndDate();
        return (null == until || startDate.before(until)) && (null == from || endDate.after(from));
    }

    /**
     * Gets a value indicating whether the supplied event contains so-called <i>floating</i> dates, i.e. the event doesn't start- and end
     * at a fixed date and time, but is always rendered in the view of the user's current timezone.
     * <p/>
     * Especially, <i>all-day</i> events are usually floating.
     *
     * @param event The event to check
     * @return <code>true</code> if the event is <i>floating</i>, <code>false</code>, otherwise
     */
    public static boolean isFloating(Event event) {
        // - floating events that are not "all-day"?
        // - better rely on null == event.getStartTimeZone()?
        return event.isAllDay();
    }

    /**
     * Filters a list of attendees based on their calendaruser type, and whether they represent "internal" attendees or not.
     *
     * @param attendees The attendees to filter
     * @param internal {@link Boolean#TRUE} to only consider internal entities, {@link Boolean#FALSE} for non-internal ones,
     *            or <code>null</code> to not filter by internal/external
     * @param cuTypes The {@link CalendarUserType}s to consider, or <code>null</code> to not filter by calendar user type
     * @return The filtered attendees
     */
    public static List<Attendee> filter(List<Attendee> attendees, Boolean internal, CalendarUserType... cuTypes) {
        if (null == attendees) {
            return null;
        }
        List<Attendee> filteredAttendees = new ArrayList<Attendee>(attendees.size());
        for (Attendee attendee : attendees) {
            if (null == cuTypes || com.openexchange.tools.arrays.Arrays.contains(cuTypes, attendee.getCuType())) {
                if (null == internal || internal.booleanValue() == isInternal(attendee)) {
                    filteredAttendees.add(attendee);
                }
            }
        }
        return filteredAttendees;
    }

    /**
     * Gets the entity identifiers of all attendees representing internal users.
     *
     * @param attendees The attendees to extract the user identifiers for
     * @return The user identifiers, or an empty array if there are none
     */
    public static int[] getUserIDs(List<Attendee> attendees) {
        if (null == attendees || 0 == attendees.size()) {
            return new int[0];
        }
        List<Integer> userIDs = new ArrayList<Integer>(attendees.size());
        for (Attendee attendee : attendees) {
            if (CalendarUserType.INDIVIDUAL.equals(attendee.getCuType()) && isInternal(attendee)) {
                userIDs.add(I(attendee.getEntity()));
            }
        }
        return I2i(userIDs);
    }

    /**
     * Gets a single search term using the field itself as column operand and a second operand.
     *
     * @param <V> The operand's type
     * @param <E> The field type
     * @param operation The operation to use
     * @param operand The second operand
     * @return A single search term
     */
    public static <V, E extends Enum<?>> SingleSearchTerm getSearchTerm(E field, SingleOperation operation, Operand<V> operand) {
        return getSearchTerm(field, operation).addOperand(operand);
    }

    /**
     * Gets a single search term using the field itself as column operand and adds the supplied value as constant operand.
     *
     * @param <V> The operand's type
     * @param <E> The field type
     * @param operation The operation to use
     * @param operand The value to use as constant operand
     * @return A single search term
     */
    public static <V, E extends Enum<?>> SingleSearchTerm getSearchTerm(E field, SingleOperation operation, V operand) {
        return getSearchTerm(field, operation, new ConstantOperand<V>(operand));
    }

    /**
     * Gets a single search term using the field itself as single column operand.
     *
     * @param <E> The field type
     * @param operation The operation to use
     * @param operand The value to use as constant operand
     * @return A single search term
     */
    public static <E extends Enum<?>> SingleSearchTerm getSearchTerm(E field, SingleOperation operation) {
        return new SingleSearchTerm(operation).addOperand(new ColumnFieldOperand<E>(field));
    }

    /**
     * Extracts all event conflicts from the problematic attributes of the supplied conflict exception
     * ({@link CalendarExceptionCodes#EVENT_CONFLICTS} or {@link CalendarExceptionCodes#HARD_EVENT_CONFLICTS}).
     *
     * @param conflictException The conflict exception
     * @return The extracted event conflicts, or an empty list if there are none
     */
    public static List<EventConflict> extractEventConflicts(OXException conflictException) {
        if (null != conflictException) {
            ProblematicAttribute[] problematics = conflictException.getProblematics();
            if (null != problematics && 0 < problematics.length) {
                List<EventConflict> eventConflicts = new ArrayList<EventConflict>(problematics.length);
                for (ProblematicAttribute problematic : problematics) {
                    if (EventConflict.class.isInstance(problematic)) {
                        eventConflicts.add((EventConflict) problematic);
                    }
                }
                return eventConflicts;
            }
        }
        return Collections.emptyList();
    }

    /**
     * Optionally gets the Java timezone for a given identifier.
     *
     * @param id The timezone identifier
     * @return The matching Java timezone, or <code>null</code> if not found
     */
    public static TimeZone optTimeZone(String id) {
        return optTimeZone(id, null);
    }

    /**
     * Optionally gets the Java timezone for a given identifier.
     *
     * @param id The timezone identifier
     * @param fallback The fallback timezone to return if no matching timezone was found
     * @return The matching Java timezone, or the fallback if not found
     */
    public static TimeZone optTimeZone(String id, TimeZone fallback) {
        TimeZone timeZone = TimeZone.getTimeZone(id);
        if ("GMT".equals(timeZone.getID()) && false == "GMT".equalsIgnoreCase(id)) {
            return fallback;
        }
        return timeZone;
    }

    /**
     * Extracts an e-mail address from the supplied URI string. Decoding of sequences of escaped octets is performed implicitly, which
     * includes decoding of percent-encoded scheme-specific parts. Additionally, any ASCII-encoded parts of the address string are decoded
     * back to their unicode representation.
     * <p/>
     * Examples:<br/>
     * <ul>
     * <li>For input string <code>horst@xn--mller-kva.de</code>, the mail address <code>horst@m&uuml;ller.de</code> is extracted</li>
     * <li>For input string <code>mailto:horst@m%C3%BCller.de</code>, the mail address <code>horst@m&uuml;ller</code> is extracted</li>
     * </ul>
     *
     * @param value The URI address string to extract the e-mail address from
     * @return The extracted e-mail address, or the value as-is if no further extraction/decoding was possible or necessary
     */
    public static String extractEMailAddress(String value) {
        URI uri = null;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            getLogger(CalendarUtils.class).debug("Error interpreting \"{}\" as URI, assuming \"mailto:\" protocol as fallback.", value, e);
            try {
                uri = new URI("mailto", value, null);
            } catch (URISyntaxException e2) {
                getLogger(CalendarUtils.class).debug("Error constructing \"mailto:\" URI for \"{}\", interpreting directly as fallback.", value, e2);
            }
        }
        /*
         * prefer scheme-specific part from "mailto:"-URI if possible
         */
        if (null != uri && "mailto".equalsIgnoreCase(uri.getScheme())) {
            value = uri.getSchemeSpecificPart();
        }
        /*
         * decode any punycoded names, too
         */
        return IDNA.toIDN(value);
    }

    /**
     * Gets a string representation of the <code>mailto</code>-URI for the supplied e-mail address.
     * <p/>
     * Non-ASCII characters are encoded implicitly as per {@link URI#toASCIIString()}.
     *
     * @param emailAddress The e-mail address to get the URI for
     * @return The <code>mailto</code>-URI, or <code>null</code> if no address was passed
     * @see {@link URI#toASCIIString()}
     */
    public static String getURI(String emailAddress) {
        if (Strings.isNotEmpty(emailAddress)) {
            try {
                return new URI("mailto", CalendarUtils.extractEMailAddress(emailAddress), null).toASCIIString();
            } catch (URISyntaxException e) {
                getLogger(CalendarUtils.class).debug(
                    "Error constructing \"mailto:\" URI for \"{}\", passign value as-is as fallback.", emailAddress, e);
            }
        }
        return emailAddress;
    }

}
