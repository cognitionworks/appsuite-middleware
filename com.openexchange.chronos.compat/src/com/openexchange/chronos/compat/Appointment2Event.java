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

package com.openexchange.chronos.compat;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.dmfs.rfc5545.recur.RecurrenceRuleIterator;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.AlarmAction;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Classification;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.Transp;
import com.openexchange.chronos.Trigger;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultRecurrenceData;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.service.RecurrenceData;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.java.util.TimeZones;

/**
 * {@link Appointment2Event}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class Appointment2Event {

    /**
     * Gets the event classification appropriate for the supplied "private flag" value.
     *
     * @param privateFlag The legacy "private flag"
     * @return The classification
     */
    public static Classification getClassification(boolean privateFlag) {
        return privateFlag ? Classification.CONFIDENTIAL : Classification.PUBLIC;
    }

    /**
     * Gets the event status appropriate for the supplied "shown as" value.
     *
     * @param confirm The legacy "shown as" constant
     * @return The event status, defaulting to {@value EventStatus#CONFIRMED} if not mappable
     */
    //    public static EventStatus getEventStatus(int shownAs) {
    //        switch (shownAs) {
    //            case 3: // com.openexchange.groupware.container.Appointment.TEMPORARY
    //                return EventStatus.TENTATIVE;
    //            default:
    //                return EventStatus.CONFIRMED;
    //        }
    //    }

    /**
     * Gets the time transparency appropriate for the supplied "shown as" value.
     *
     * @param confirm The legacy "shown as" constant
     * @return The time transparency, defaulting to {@value Transp#OPAQUE} if not mappable
     */
    public static Transp getTransparency(int shownAs) {
        return ShownAsTransparency.getTransparency(shownAs);
    }

    /**
     * Gets a participation status appropriate for the supplied confirmation status.
     *
     * @param confirm The legacy confirmation status constant
     * @return The participation status, or {@value ParticipationStatus#NEEDS_ACTION} if not mappable
     */
    public static ParticipationStatus getParticipationStatus(int confirm) {
        switch (confirm) {
            case 1: // com.openexchange.groupware.container.participants.ConfirmStatus.ACCEPT
                return ParticipationStatus.ACCEPTED;
            case 2: // com.openexchange.groupware.container.participants.ConfirmStatus.DECLINE
                return ParticipationStatus.DECLINED;
            case 3: // com.openexchange.groupware.container.participants.ConfirmStatus.TENTATIVE
                return ParticipationStatus.TENTATIVE;
            default: // com.openexchange.groupware.container.participants.ConfirmStatus.NONE
                return ParticipationStatus.NEEDS_ACTION;
        }
    }

    /**
     * Gets a calendar user type appropriate for the supplied participant type.
     *
     * @param type The legacy participant type constant
     * @return The calendar user type, or {@value CalendarUserType#UNKNOWN} if not mappable
     */
    public static CalendarUserType getCalendarUserType(int type) {
        switch (type) {
            case 1: // com.openexchange.groupware.container.Participant.USER
            case 5: // com.openexchange.groupware.container.Participant.EXTERNAL_USER
                return CalendarUserType.INDIVIDUAL;
            case 2: // com.openexchange.groupware.container.Participant.GROUP
            case 6: // com.openexchange.groupware.container.Participant.EXTERNAL_GROUP
                return CalendarUserType.GROUP;
            case 3: // com.openexchange.groupware.container.Participant.RESOURCE
            case 4: // com.openexchange.groupware.container.Participant.RESOURCEGROUP
                return CalendarUserType.RESOURCE;
            default: // com.openexchange.groupware.container.Participant.NO_ID
                return CalendarUserType.UNKNOWN;
        }
    }

    /**
     * Gets an <code>mailto</code>-URI for the supplied e-mail address.
     *
     * @param emailAddress The e-mail address to get the URI for
     * @return The <code>mailto</code>-URI, or <code>null</code> if no address was passed
     */
    public static String getURI(String emailAddress) {
        if (Strings.isNotEmpty(emailAddress)) {
            return "mailto:" + emailAddress;
        }
        return null;
    }

    /**
     * Gets the CSS3 color appropriate for the supplied color label.
     *
     * @param colorLabel The legacy color label constant
     * @return The color, or <code>null</code> if not mappable
     */
    public static String getColor(int colorLabel) {
        switch (colorLabel) {
            case 1:
                return "lightblue"; // #9bceff ~ #ADD8E6
            case 2:
                return "darkblue"; // #6ca0df ~ #00008B
            case 3:
                return "purple"; // #a889d6 ~ #800080
            case 4:
                return "pink"; // #e2b3e2 ~ #FFC0CB
            case 5:
                return "red"; // #e7a9ab ~ #FF0000
            case 6:
                return "orange"; // #ffb870 ~ FFA500
            case 7:
                return "yellow"; // #f2de88 ~ #FFFF00
            case 8:
                return "lightgreen"; // #c2d082 ~ #90EE90
            case 9:
                return "darkgreen"; // #809753 ~ #006400
            case 10:
                return "gray"; // #4d4d4d ~ #808080
            default:
                return null;
        }
    }

    /**
     * Gets a list of categories for the supplied comma-separated categories string.
     *
     * @param categories The legacy categories string
     * @return The categories list
     */
    public static List<String> getCategories(String categories) {
        // TODO: escaping?
        if (Strings.isEmpty(categories)) {
            return null;
        }
        return Strings.splitAndTrim(categories, ",");
    }

    /**
     * Gets an alarm appropriate for the supplied reminder minutes.
     *
     * @param reminder The legacy reminder value
     * @return The alarm
     */
    public static Alarm getAlarm(int reminder) {
        Alarm alarm = new Alarm();
        alarm.setAction(AlarmAction.DISPLAY);
        alarm.setDescription("Reminder");
        Trigger trigger = new Trigger();
        trigger.setDuration("-PT" + reminder + 'M');
        alarm.setTrigger(trigger);
        return alarm;
    }

    /**
     * Gets the recurrence data for the supplied series pattern.
     *
     * @param pattern The legacy series pattern
     * @return The recurrence data, or <code>null</code> if not mappable
     */
    public static RecurrenceData getRecurrenceData(SeriesPattern pattern) {
        if (null == pattern || null == pattern.getType()) {
            return null;
        }
        String recurrenceRule = Recurrence.getRecurrenceRule(pattern);
        return new DefaultRecurrenceData(recurrenceRule, pattern.isFullTime().booleanValue(), pattern.getTimeZone().getID(), pattern.getSeriesStart().longValue());
    }

    /**
     * Calculates the recurrence identifier, i.e. the start time of a specific occurrence of a recurring event, based on the legacy
     * recurrence date position.
     *
     * @param recurrenceData The corresponding recurrence data
     * @param recurrenceDatePosition The legacy recurrence date position, i.e. the date where the original occurrence would have been, as
     *            UTC date with truncated time fraction
     * @return The recurrence identifier
     * @throws {@link CalendarExceptionCodes#INVALID_RECURRENCE_ID}
     */
    public static RecurrenceId getRecurrenceID(RecurrenceData recurrenceData, Date recurrenceDatePosition) throws OXException {
        Calendar calendar = CalendarUtils.initCalendar(TimeZones.UTC, recurrenceData.getSeriesStart());
        RecurrenceRuleIterator iterator = Recurrence.getRecurrenceIterator(recurrenceData, true);
        int position = 0;
        while (iterator.hasNext()) {
            long nextMillis = iterator.nextMillis();
            calendar.setTimeInMillis(nextMillis);
            long nextDatePosition = CalendarUtils.truncateTime(calendar).getTimeInMillis();
            position++;
            if (recurrenceDatePosition.getTime() == nextDatePosition) {
                return new PositionAwareRecurrenceId(recurrenceData, nextMillis, position, new Date(nextDatePosition));
            }
            if (nextDatePosition > recurrenceDatePosition.getTime()) {
                break;
            }
        }
        throw CalendarExceptionCodes.INVALID_RECURRENCE_ID.create("legacy recurrence date position " + recurrenceDatePosition.getTime(), recurrenceData.getRecurrenceRule());
    }

    /**
     * Calculates the recurrence identifiers, i.e. the start times of the specific occurrences of a recurring event, for a list of legacy
     * recurrence date position.
     *
     * @param recurrenceData The corresponding recurrence data
     * @param recurrenceDatePositions The legacy recurrence date positions, i.e. the dates where the original occurrences would have been,
     *            as UTC date with truncated time fraction
     * @return The recurrence identifiers
     * @throws {@link CalendarExceptionCodes#INVALID_RECURRENCE_ID}
     */
    public static SortedSet<RecurrenceId> getRecurrenceIDs(RecurrenceData recurrenceData, Collection<Date> recurrenceDatePositions) throws OXException {
        // TODO
        if (null == recurrenceDatePositions) {
            return null;
        }
        SortedSet<RecurrenceId> recurrenceIDs = new TreeSet<RecurrenceId>();
        for (Date recurrenceDatePosition : recurrenceDatePositions) {
            RecurrenceId recurrenceID = getRecurrenceID(recurrenceData, recurrenceDatePosition);
            recurrenceIDs.add(recurrenceID);
        }
        return recurrenceIDs;
    }

    /**
     * Gets the recurrence identifier, i.e. the original start time of a recurrence instance, based on the supplied legacy recurrence
     * position number.
     *
     * @param recurrenceData The corresponding recurrence data
     * @param recurrencePosition The legacy, 1-based recurrence position
     * @return The recurrence identifier
     * @throws {@link CalendarExceptionCodes#INVALID_RECURRENCE_ID}
     */
    public static RecurrenceId getRecurrenceID(RecurrenceData recurrenceData, int recurrencePosition) throws OXException {
        RecurrenceRuleIterator iterator = Recurrence.getRecurrenceIterator(recurrenceData, true);
        int position = 0;
        while (iterator.hasNext()) {
            long nextMillis = iterator.nextMillis();
            if (++position == recurrencePosition) {
                Date datePosition = CalendarUtils.truncateTime(new Date(nextMillis), TimeZones.UTC);
                return new PositionAwareRecurrenceId(recurrenceData, nextMillis, position, datePosition);
            }
        }
        throw CalendarExceptionCodes.INVALID_RECURRENCE_ID.create("legacy recurrence position " + recurrencePosition, recurrenceData.getRecurrenceRule());
    }

    /**
     * Initializes a new {@link Appointment2Event}.
     */
    private Appointment2Event() {
        super();
    }

}
