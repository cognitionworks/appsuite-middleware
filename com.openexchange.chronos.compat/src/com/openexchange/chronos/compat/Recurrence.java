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

import static com.openexchange.chronos.common.CalendarUtils.initCalendar;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.Duration;
import org.dmfs.rfc5545.recur.Freq;
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.dmfs.rfc5545.recur.RecurrenceRule.Part;
import org.dmfs.rfc5545.recur.RecurrenceRule.WeekdayNum;
import org.dmfs.rfc5545.recur.RecurrenceRuleIterator;
import com.openexchange.chronos.Period;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultRecurrenceId;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.service.RecurrenceData;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.java.util.TimeZones;

/**
 * {@link Recurrence}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.0
 */
public class Recurrence {

    private static final Map<String, Integer> weekdays = new HashMap<String, Integer>();

    private static final Map<Integer, String> reverseDays = new HashMap<Integer, String>();

    private static final List<Integer> allDays = new LinkedList<Integer>();

    private static final SimpleDateFormat date;

    static {
        weekdays.put("SU", 1);
        weekdays.put("MO", 2);
        weekdays.put("TU", 4);
        weekdays.put("WE", 8);
        weekdays.put("TH", 16);
        weekdays.put("FR", 32);
        weekdays.put("SA", 64);

        for (final Map.Entry<String, Integer> entry : weekdays.entrySet()) {
            allDays.add(entry.getValue());
            reverseDays.put(entry.getValue(), entry.getKey());
        }
        Collections.sort(allDays); // nicer order in BYDAYS
        date = new SimpleDateFormat("yyyyMMdd");
        date.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Calculates the actual start- and end-date of the "master" recurrence for a specific series pattern, i.e. the start- and end-date of
     * a serie's first occurrence.
     *
     * @param seriesPeriod The implicit series period, i.e. the period spanning from the first until the "last" occurrence
     * @param absoluteDuration The absolute duration of one occurrence in days (the legacy "recurrence calculator" value)
     * @return The actual start- and end-date of the recurrence master, wrapped into a {@link Period} structure
     */
    public static Period getRecurrenceMasterPeriod(Period seriesPeriod, int absoluteDuration) {
        /*
         * determine "date" fraction of series start
         */
        Calendar calendar = initCalendar(TimeZones.UTC, seriesPeriod.getStartDate());
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int date = calendar.get(Calendar.DATE);
        Date startDate = calendar.getTime();
        /*
         * apply same "date" fraction to series end
         */
        calendar.setTime(seriesPeriod.getEndDate());
        calendar.set(year, month, date);
        /*
         * adjust end date considering absolute duration
         */
        if (calendar.getTime().before(startDate)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        calendar.add(Calendar.DAY_OF_YEAR, absoluteDuration);
        Date endDate = calendar.getTime();
        return new Period(startDate, endDate, seriesPeriod.isAllDay());
    }

    /**
     * Calculates the implicit start- and end-date of a recurring event series, i.e. the period spanning from the first until the "last"
     * occurrence.
     *
     * @param recurrenceData The recurrence data
     * @param masterPeriod The actual start- and end-date of the recurrence master, wrapped into a {@link Period} structure
     * @return The implicit period of a recurring event series
     */
    public static Period getImplicitSeriesPeriod(RecurrenceData recurrenceData, Period masterPeriod) throws OXException {
        /*
         * remember time fraction of actual start- and end-date
         */
        TimeZone timeZone = null != recurrenceData.getTimeZoneID() ? TimeZone.getTimeZone(recurrenceData.getTimeZoneID()) : TimeZones.UTC;
        Calendar calendar = initCalendar(timeZone, masterPeriod.getStartDate());
        int startHour = calendar.get(Calendar.HOUR_OF_DAY);
        int startMinute = calendar.get(Calendar.MINUTE);
        int startSecond = calendar.get(Calendar.SECOND);
        calendar.setTime(masterPeriod.getEndDate());
        int endHour = calendar.get(Calendar.HOUR_OF_DAY);
        int endMinute = calendar.get(Calendar.MINUTE);
        int endSecond = calendar.get(Calendar.SECOND);
        /*
         * iterate recurrence and take over start date of first occurrence
         */
        Date startDate;
        RecurrenceRuleIterator iterator = getRecurrenceIterator(recurrenceData, true);
        if (iterator.hasNext()) {
            calendar.setTimeInMillis(iterator.nextMillis());
            calendar.set(Calendar.HOUR_OF_DAY, startHour);
            calendar.set(Calendar.MINUTE, startMinute);
            calendar.set(Calendar.SECOND, startSecond);
            startDate = calendar.getTime();
        } else {
            startDate = masterPeriod.getStartDate();
        }
        /*
         * iterate recurrence and take over end date of "last" occurrence
         */
        //TODO recurrence service should know "max until"
        long millis = masterPeriod.getEndDate().getTime();
        for (int i = 1; i < 1000 && iterator.hasNext(); millis = iterator.nextMillis(), i++)
            ;
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, endHour);
        calendar.set(Calendar.MINUTE, endMinute);
        calendar.set(Calendar.SECOND, endSecond);
        calendar.add(Calendar.DAY_OF_YEAR, (int) masterPeriod.getTotalDays());
        Date endDate = calendar.getTime();
        /*
         * adjust end date if it falls into other timezone observance with different offset, just like it's done at
         * com.openexchange.calendar.CalendarOperation.calculateImplictEndOfSeries(CalendarDataObject, String, boolean)
         */
        int startOffset = timeZone.getOffset(startDate.getTime());
        int endOffset = timeZone.getOffset(endDate.getTime());
        if (startOffset != endOffset) {
            endDate.setTime(endDate.getTime() + endOffset - startOffset);
        }
        return new Period(startDate, endDate, masterPeriod.isAllDay());
    }

    /**
     * Gets a value indicating whether an event series lies in the past or not, i.e. the end-time of its last occurrence is before the
     * <i>current</i> time.
     * <p/>
     * Therefore, the recurrence rule's <code>UNTIL</code>- and <code>COUNT</code>-parameters are evaluated accordingly; for
     * <i>never-ending</i> event series, this method always returns <code>false</code>;
     *
     * @param recurrenceData The recurrence data to check
     * @param now The date to consider as <i>now</i> in the comparison
     * @param timeZone The timezone to consider if the event has <i>floating</i> dates
     * @return <code>true</code> if the event series is in the past, <code>false</code>, otherwise
     */
    public static boolean isInPast(RecurrenceData recurrenceData, Date now, TimeZone timeZone) throws OXException {
        RecurrenceRuleIterator iterator = getRecurrenceIterator(recurrenceData);
        iterator.fastForward(now.getTime());
        if (false == iterator.hasNext()) {
            return true;
        }
        DateTime occurrence = iterator.nextDateTime();
        if (occurrence.isFloating() || occurrence.isAllDay()) {
            return now.after(CalendarUtils.getDateInTimeZone(new Date(occurrence.getTimestamp()), timeZone));
        }
        return false;
    }

    /**
     * Gets a value indicating whether a certain recurrence identifier is actually part of a recurrence.
     *
     * @param recurrenceData The recurrence data to match against
     * @param recurrenceId The recurrence identifier to check
     * @return <code>true</code> if the recurrence identifier is a valid occurrence based on the recurrence data, <code>false</code>, otherwise
     */
    public static boolean isRecurrence(RecurrenceData recurrenceData, RecurrenceId recurrenceId) throws OXException {
        RecurrenceRuleIterator iterator = getRecurrenceIterator(recurrenceData, true);
        while (iterator.hasNext()) {
            long millis = iterator.nextMillis();
            if (recurrenceId.getValue() == millis) {
                return true;
            }
            if (millis > recurrenceId.getValue()) {
                return false;
            }
        }
        return false;
    }

    /**
     * Gets a value indicating whether the supplied recurrence data is supported or not, throwing an appropriate exception if not.
     *
     * @param recurrenceData The recurrence data
     * @throws OXException {@link CalendarExceptionCodes#INVALID_RRULE} {@link CalendarExceptionCodes#UNSUPPORTED_RRULE}
     */
    public static void checkIsSupported(RecurrenceData recurrenceData) throws OXException {
        RecurrenceRule rule = getRecurrenceRule(recurrenceData.getRecurrenceRule());
        //TODO: further checks
        switch (rule.getFreq()) {
            case DAILY:
                List<Integer> byMonthPart = rule.getByPart(Part.BYMONTH);
                if (null != byMonthPart && 0 < byMonthPart.size()) {
                    // bug #9840
                    throw CalendarExceptionCodes.UNSUPPORTED_RRULE.create(
                        recurrenceData.getRecurrenceRule(), "BYMONTH", "BYMONTH not supported in DAILY");
                }
                break;
            case MONTHLY:
                break;
            case WEEKLY:
                break;
            case YEARLY:
                break;
            default:
                // no BYSECOND, BYMINUTE, BYHOUR, ...
                throw CalendarExceptionCodes.UNSUPPORTED_RRULE.create(
                    recurrenceData.getRecurrenceRule(), "FREQ", rule.getFreq() + " not supported");
        }
        /*
         * initializing the iterator implicitly checks the rule within the constraints of the corresponding event parameters
         */
        getRecurrenceIterator(rule, recurrenceData.getSeriesStart(), recurrenceData.getTimeZoneID(), recurrenceData.isAllDay(), false);
    }

    /**
     * Initializes a new recurrence iterator for a specific recurrence rule.
     *
     * @param recurrenceData The recurrence data
     * @return The recurrence rule iterator
     * @throws OXException {@link CalendarExceptionCodes#INVALID_RRULE}
     */
    public static RecurrenceRuleIterator getRecurrenceIterator(RecurrenceData recurrenceData) throws OXException {
        return getRecurrenceIterator(recurrenceData, false);
    }

    /**
     * Initializes a new recurrence iterator for a specific recurrence rule, optionally advancing to the first occurrence. The latter
     * option ensures that the first date delivered by the iterator matches the start-date of the first occurrence.
     *
     * @param recurrenceData The recurrence data
     * @param forwardToOccurrence <code>true</code> to fast-forward the iterator to the first occurrence if the recurrence data's start
     *            does not fall into the pattern, <code>false</code> otherwise
     * @return The recurrence rule iterator
     * @throws OXException {@link CalendarExceptionCodes#INVALID_RRULE}
     */
    public static RecurrenceRuleIterator getRecurrenceIterator(RecurrenceData recurrenceData, boolean forwardToOccurrence) throws OXException {
        RecurrenceRule rule = getRecurrenceRule(recurrenceData.getRecurrenceRule());
        return getRecurrenceIterator(rule, recurrenceData.getSeriesStart(), recurrenceData.getTimeZoneID(), recurrenceData.isAllDay(), forwardToOccurrence);
    }

    /**
     * Initializes a new recurrence rule for the supplied recurrence rule string.
     *
     *
     * @param rrule The recurrence rule string
     * @return The recurrence rule
     * @throws OXException {@link CalendarExceptionCodes#INVALID_RRULE}
     */
    private static RecurrenceRule getRecurrenceRule(String rrule) throws OXException {
        try {
            return new RecurrenceRule(rrule);
        } catch (InvalidRecurrenceRuleException | IllegalArgumentException e) {
            throw CalendarExceptionCodes.INVALID_RRULE.create(e, rrule);
        }
    }

    /**
     * Initializes a new recurrence iterator for a specific recurrence rule, optionally advancing to the first occurrence. The latter
     * option ensures that the first date delivered by the iterator matches the start-date of the first occurrence.
     *
     * @param rule The recurrence rule
     * @param seriesStart The series start date, usually the date of the first occurrence
     * @param timeZoneID The timezone identifier applicable for the recurrence
     * @param allDay <code>true</code> if the recurrence is <i>all-day</i>, <code>false</code>, otherwise
     * @param forwardToOccurrence <code>true</code> to fast-forward the iterator to the first occurrence if the recurrence data's start
     *            does not fall into the pattern, <code>false</code> otherwise
     * @return The recurrence rule iterator
     * @throws OXException {@link CalendarExceptionCodes#INVALID_RRULE}
     */
    private static RecurrenceRuleIterator getRecurrenceIterator(RecurrenceRule rule, long seriesStart, String timeZoneID, boolean allDay, boolean forwardToOccurrence) throws OXException {
        DateTime start;
        if (allDay) {
            start = new DateTime(TimeZones.UTC, seriesStart).toAllDay();
        } else if (null != timeZoneID) {
            start = new DateTime(TimeZone.getTimeZone(timeZoneID), seriesStart);
        } else {
            start = new DateTime(seriesStart);
        }
        try {
            if (forwardToOccurrence && false == isPotentialOccurrence(start, rule)) {
                /*
                 * supplied start does not match recurrence rule, forward to first "real" occurrence
                 */
                DateTime firstOccurrence = null;
                Integer originalCount = rule.getCount();
                try {
                    if (null != originalCount) {
                        rule.setUntil(null);
                    }
                    for (RecurrenceRuleIterator iterator = rule.iterator(start); null == firstOccurrence && iterator.hasNext(); iterator.nextMillis()) {
                        // TODO: max_recurrences guard?
                        long millis = iterator.peekMillis();
                        if (millis > seriesStart) {
                            firstOccurrence = iterator.peekDateTime();
                        }
                    }
                    if (null != firstOccurrence) {
                        start = firstOccurrence;
                    } else {
                        throw CalendarExceptionCodes.INVALID_RECURRENCE_ID.create(new DefaultRecurrenceId(seriesStart), rule);
                    }
                } finally {
                    if (null != originalCount) {
                        rule.setCount(originalCount.intValue());
                    }
                }
            }
            return rule.iterator(start);
        } catch (IllegalArgumentException e) {
            throw CalendarExceptionCodes.INVALID_RRULE.create(e, rule);
        }
    }

    /**
     * Gets a value indicating whether a specific date-time is (or could be) part of a recurrence rule or not.
     * <p/>
     * This is always <code>true</code> for <i>relative</i> recurrences that can usually begin on any date and the continue with a
     * specific interval, e.g. as in a simple <code>FREQ=DAILY</code> rule.
     * <p/>
     * Otherwise, the occurrences are derived from the rule directly, and a specific date is not necessarily part of the rule, e.g. a
     * <code>FREQ=WEEKLY;BYDAY=WE</code> or a <code>FREQ=YEARLY;BYMONTH=10;BYMONTHDAY=8</code> event series.
     *
     * @param dateTime The date-time to check
     * @param recurrenceRule The recurrence rule to match against
     * @return <code>true</code> if the date-time is (or could be) an actual occurrence of the recurrence rule, <code>false</code>, otherwise
     */
    private static boolean isPotentialOccurrence(DateTime dateTime, RecurrenceRule recurrenceRule) {
        List<WeekdayNum> byDayPart = recurrenceRule.getByDayPart();
        List<Integer> byMonthPart = recurrenceRule.getByPart(Part.BYMONTH);
        List<Integer> byMonthDayPart = recurrenceRule.getByPart(Part.BYMONTHDAY);
        List<Integer> bySetPosPart = recurrenceRule.getByPart(Part.BYSETPOS);
        switch (recurrenceRule.getFreq()) {
            case SECONDLY:
            case MINUTELY:
            case HOURLY:
            case DAILY:
                return true;
            case WEEKLY:
                if (null != byDayPart && 0 < byDayPart.size()) {
                    return matchesDayOfWeek(dateTime, byDayPart);
                }
                break;
            case MONTHLY:
                if (null != byMonthDayPart && 0 < byMonthDayPart.size()) {
                    /*
                     * ~ "monthly 1"
                     */
                    return matchesDayOfMonth(dateTime, byMonthDayPart);
                }
                if (null != byDayPart && 0 < byDayPart.size() && null != bySetPosPart && 0 < bySetPosPart.size()) {
                    /*
                     * ~ "monthly 2"
                     */
                    return matchesDayOfWeekInMonth(dateTime, byDayPart, bySetPosPart);
                }
                break;
            case YEARLY:
                if (null != byMonthDayPart && 0 < byMonthDayPart.size() && null != byMonthPart && 0 < byMonthPart.size()) {
                    /*
                     * ~ "yearly 1"
                     */
                    return matchesMonth(dateTime, byMonthPart) && matchesDayOfMonth(dateTime, byMonthDayPart);
                }
                if (null != byMonthPart && 0 < byMonthPart.size() && null != byDayPart && 0 < byDayPart.size() && null != bySetPosPart && 0 < bySetPosPart.size()) {
                    /*
                     * ~ "yearly 2"
                     */
                    return matchesMonth(dateTime, byMonthPart) && matchesDayOfWeekInMonth(dateTime, byDayPart, bySetPosPart);
                }
                return true;
            default:
                return false;
        }
        return false;
    }

    /**
     * Gets a value indicating whether a specific date matches a certain weekday in the month as given through the supplied recurrence
     * rule fragments.
     *
     * @param dateTime The date-time to check
     * @param byDayPart The possible weekday numbers to match
     * @param bySetPosPart The possible <i>set</i> positions of the week in the month to match
     * @return <code>true</code> if the date-time matches the day of week in month, <code>false</code>, otherwise
     */
    private static boolean matchesDayOfWeekInMonth(DateTime dateTime, List<WeekdayNum> byDayPart, List<Integer> bySetPosPart) {
        for (Integer bySetPos : bySetPosPart) {
            Calendar calendar = initCalendar(null != dateTime.getTimeZone() ? dateTime.getTimeZone() : TimeZones.UTC, dateTime.getTimestamp());
            if (0 < bySetPos.intValue()) {
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                for (int matched = 0; dateTime.getMonth() == calendar.get(Calendar.MONTH); calendar.add(Calendar.DAY_OF_MONTH, 1)) {
                    if (matchesDayOfWeek(calendar, byDayPart) && ++matched == bySetPos.intValue() && dateTime.getTimestamp() == calendar.getTimeInMillis()) {
                        return true;
                    }
                }
            } else if (0 > bySetPos.intValue()) {
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                for (int matched = 0; dateTime.getMonth() == calendar.get(Calendar.MONTH); calendar.add(Calendar.DAY_OF_MONTH, -1)) {
                    if (matchesDayOfWeek(calendar, byDayPart) && ++matched == -1 * bySetPos.intValue() && dateTime.getTimestamp() == calendar.getTimeInMillis()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Gets a value indicating whether a specific date matches a certain weekday as given through the supplied recurrence rule fragments.
     *
     * @param dateTime The date-time to check
     * @param byDayPart The possible weekdays to match
     * @return <code>true</code> if the date-time matches the weekday, <code>false</code>, otherwise
     */
    private static boolean matchesDayOfWeek(DateTime dateTime, List<WeekdayNum> byDayPart) {
        for (WeekdayNum weekdayNum : byDayPart) {
            if (weekdayNum.weekday.ordinal() == dateTime.getDayOfWeek()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a value indicating whether the {@link Calendar#DAY_OF_WEEK} field of a specific calendar instance matches a certain weekday as
     * given through the supplied recurrence rule fragments.
     *
     * @param calendar The calendar to check
     * @param byDayPart The possible weekdays to match
     * @return <code>true</code> if the date-time matches the weekday, <code>false</code>, otherwise
     */
    private static boolean matchesDayOfWeek(Calendar calendar, List<WeekdayNum> byDayPart) {
        int weekDayOrdinal = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        for (WeekdayNum weekdayNum : byDayPart) {
            if (weekdayNum.weekday.ordinal() == weekDayOrdinal) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a value indicating whether a specific date matches a certain day of the month as given through the supplied recurrence rule
     * fragments.
     *
     * @param dateTime The date-time to check
     * @param byMonthDayPart The possible month days to match
     * @return <code>true</code> if the date-time matches the day of month, <code>false</code>, otherwise
     */
    private static boolean matchesDayOfMonth(DateTime dateTime, List<Integer> byMonthDayPart) {
        Calendar calendar = initCalendar(null != dateTime.getTimeZone() ? dateTime.getTimeZone() : TimeZones.UTC, dateTime.getTimestamp());
        int dayMonth = dateTime.getDayOfMonth();
        int maximumDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (Integer byMonthDay : byMonthDayPart) {
            if (byMonthDay.intValue() == dayMonth || dayMonth - maximumDayOfMonth - 1 == byMonthDay.intValue()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a value indicating whether a specific date matches a certain month as given through the supplied recurrence rule fragments.
     *
     * @param dateTime The date-time to check
     * @param byMonthPart The possible months to match
     * @return <code>true</code> if the date-time matches the month, <code>false</code>, otherwise
     */
    private static boolean matchesMonth(DateTime dateTime, List<Integer> byMonthPart) {
        for (Integer byMonth : byMonthPart) {
            if (byMonth.intValue() == dateTime.getMonth()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the recurrence rule appropriate for the supplied series pattern.
     *
     * @param seriesPattern The legacy, pipe-separated series pattern, e.g. <code>t|1|i|1|s|1313388000000|e|1313625600000|o|4|</code>
     * @return The corresponding recurrence rule
     */
    public static String getRecurrenceRule(String seriesPattern, TimeZone tz, Boolean fullTime) {
        return getRecurrenceRule(new SeriesPattern(seriesPattern, tz.getID(), fullTime.booleanValue()));
    }

    public static String getRecurrenceRule(SeriesPattern pattern) {
        if (pattern.isFullTime() == null) {
            throw new IllegalArgumentException("Fulltime value must be set.");
        }
        if (pattern.getTimeZone() == null) {
            throw new IllegalArgumentException("TimeZone must be set.");
        }
        try {
            switch (pattern.getType()) {
                case 1:
                    return daily(pattern);
                case 2:
                    return weekly(pattern);
                case 3:
                case 5:
                    return monthly(pattern);
                case 4:
                case 6:
                    return yearly(pattern);
                default:
                    return null;
            }
        } catch (InvalidRecurrenceRuleException e) {
            // TODO Auto-generated catch block
        }
        return null;
    }

    public static SeriesPattern generatePattern(String recur, Calendar startDate) throws OXException {
        RecurrenceRule rrule = getRecurrenceRule(recur);
        CalendarDataObject cObj = new CalendarDataObject();
        cObj.setTimezone(startDate.getTimeZone().getID());
        cObj.setStartDate(startDate.getTime());
        switch (rrule.getFreq()) {
            case DAILY:
                if (rrule.getByDayPart() != null && rrule.getByDayPart().size() > 0) {
                    // used as "each weekday" by some clients: FREQ=DAILY;INTERVAL=1;WKST=SU;BYDAY=MO,TU,WE,TH,FR
                    // save as 'weekly' type with daymask
                    cObj.setRecurrenceType(CalendarObject.WEEKLY);
                    setDays(cObj, rrule, startDate);
                } else {
                    cObj.setRecurrenceType(CalendarObject.DAILY);
                }
                if (rrule.getByPart(Part.BYMONTH) != null && !rrule.getByPart(Part.BYMONTH).isEmpty()) {
                    // TODO:
                }
                break;
            case WEEKLY:
                cObj.setRecurrenceType(CalendarObject.WEEKLY);
                setDays(cObj, rrule, startDate);
                break;
            case MONTHLY:
                cObj.setRecurrenceType(CalendarObject.MONTHLY);
                setMonthDay(cObj, rrule, startDate);
                break;
            case YEARLY:
                cObj.setRecurrenceType(CalendarObject.YEARLY);
                List<Integer> monthList = rrule.getByPart(Part.BYMONTH);
                if (null != monthList && !monthList.isEmpty()) {
                    cObj.setMonth(monthList.get(0).intValue());
                    setMonthDay(cObj, rrule, startDate);
                } else {
                    cObj.setMonth(startDate.get(Calendar.MONTH));
                    setMonthDay(cObj, rrule, startDate);
                }
                break;
            default:
                break;
        }

        int interval = rrule.getInterval();
        if (interval == -1) {
            interval = 1;
        }
        cObj.setInterval(interval);
        Integer count = rrule.getCount();
        if (count != null) {
            final int recurrenceCount = rrule.getCount();
            cObj.setRecurrenceCount(recurrenceCount);
            setOccurrenceIfNeededRecoveryFIXME(cObj, recurrenceCount);
        } else if (rrule.getUntil() != null) {
            try {
                cObj.setUntil(getSeriesEnd(rrule, startDate.getTime(), startDate.getTimeZone().getID()));
            } catch (OXException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return generatePattern(cObj, rrule);
    }

    /**
     * Calculates the (legacy) series end date from the <code>UNTIL</code> part of a specific recurrence rule, which is the date in UTC
     * (without time fraction) of the last occurrence, as used in the legacy series pattern.
     *
     * @param recurrenceRule The recurrence rule
     * @param seriesStart The series master event's start date
     * @param timeZoneID The timezone associated with the event series, or <code>null</code> for floating events
     * @return The series end date, or <code>null</code> if not set in the recurrence rule
     */
    private static Date getSeriesEnd(RecurrenceRule recurrenceRule, Date seriesStart, String timeZoneID) throws OXException {
        if (null == recurrenceRule || null == recurrenceRule.getUntil()) {
            return null;
        }
        DateTime until = recurrenceRule.getUntil();
        if (until.isAllDay()) {
            /*
             * consider DATE value type - already in legacy format
             */
            return new Date(until.getTimestamp());
        }
        /*
         * consider DATE-TIME value type - determine start date of first occurrence outside range
         */
        DateTime nextOccurrenceStart = null;
        if (until.getTimestamp() > seriesStart.getTime()) {
            try {
                recurrenceRule.setUntil(null);
                RecurrenceRuleIterator iterator = getRecurrenceIterator(recurrenceRule, seriesStart.getTime(), timeZoneID, false, true);
                while (iterator.hasNext()) {
                    DateTime occurrenceStart = iterator.nextDateTime();
                    if (occurrenceStart.after(until)) {
                        nextOccurrenceStart = occurrenceStart;
                        break;
                    }
                }
            } finally {
                /*
                 * ensure to reset UNTIL to previous value in any case
                 */
                recurrenceRule.setUntil(until);
            }
        }
        /*
         * check if the client-defined UNTIL is at least one day prior next occurrence (as observed in the timezone)
         */
        DateTime localUntil = null != timeZoneID ? new DateTime(TimeZone.getTimeZone(timeZoneID), until.getTimestamp()) : until;
        if (null == nextOccurrenceStart || nextOccurrenceStart.getYear() > localUntil.getYear() ||
            nextOccurrenceStart.getMonth() > localUntil.getMonth() || nextOccurrenceStart.getDayOfMonth() > localUntil.getDayOfMonth()) {
            /*
             * take over series end from UNTIL
             */
        } else {
            /*
             * shift series end one day earlier to prevent generation of an additional occurrence
             */
            localUntil = localUntil.addDuration(new Duration(-1, 1, 0));
        }
        return initCalendar(TimeZones.UTC, localUntil.getYear(), localUntil.getMonth(), localUntil.getDayOfMonth()).getTime();
    }

    private static String daily(SeriesPattern pattern) {
        return getRecurBuilder(Freq.DAILY, pattern).toString();
    }

    private static String weekly(SeriesPattern pattern) throws InvalidRecurrenceRuleException {
        RecurrenceRule recur = getRecurBuilder(Freq.WEEKLY, pattern);
        int days = pattern.getDaysOfWeek();
        addDays(days, recur);
        return recur.toString();
    }

    private static String monthly(SeriesPattern pattern) throws InvalidRecurrenceRuleException {
        RecurrenceRule recur = getRecurBuilder(Freq.MONTHLY, pattern);
        if (pattern.getType() == 5) {
            addDays(pattern.getDaysOfWeek(), recur);
            int weekNo = pattern.getDayOfMonth();
            if (5 == weekNo) {
                weekNo = -1;
            }
            recur.setByPart(Part.BYSETPOS, weekNo);
        } else if (pattern.getType() == 3) {
            recur.setByPart(Part.BYMONTHDAY, pattern.getDayOfMonth());
        } else {
            return null;
        }
        return recur.toString();
    }

    private static String yearly(SeriesPattern pattern) throws InvalidRecurrenceRuleException {
        RecurrenceRule recur = getRecurBuilder(Freq.YEARLY, pattern);
        if (pattern.getType() == 6) {
            addDays(pattern.getDaysOfWeek(), recur);
            recur.setByPart(Part.BYMONTH, pattern.getMonth());
            int weekNo = pattern.getDayOfMonth();
            if (5 == weekNo) {
                weekNo = -1;
            }
            recur.setByPart(Part.BYSETPOS, weekNo);
        } else if (pattern.getType() == 4) {
            recur.setByPart(Part.BYMONTH, pattern.getMonth()); //TODO +1 or not? " ... The value is a list of non-zero integers. ..."
            recur.setByPart(Part.BYMONTHDAY, pattern.getDayOfMonth());
        } else {
            return null;
        }
        return recur.toString();
    }

    private static void addDays(int days, final RecurrenceRule recur) throws InvalidRecurrenceRuleException {
        List<WeekdayNum> weekdays = new ArrayList<WeekdayNum>();
        for (int day : allDays) {
            if (day == (day & days)) {
                weekdays.add(WeekdayNum.valueOf(reverseDays.get(Integer.valueOf(day))));
            }
        }
        recur.setByDayPart(weekdays);
    }

    private static RecurrenceRule getRecurBuilder(Freq frequency, SeriesPattern pattern) {
        RecurrenceRule recur = new RecurrenceRule(frequency);
        recur.setInterval(pattern.getInterval());
        if (pattern.getOccurrences() != null) {
            recur.setCount(pattern.getOccurrences());
        } else if (pattern.getSeriesEnd() != null) {
            recur.setUntil(getUntil(pattern));
        }
        return recur;
    }

    /**
     * Determines the {@link net.fortuna.ical4j.model.Date} from the supplied
     * recurring calendar object, ready-to-use in ical4j components. <p/>
     * While date-only until dates are used as is (for tasks and whole day
     * appointments), date-time specific until dates are calculated based on
     * the appointments timezone to include the last-possible start-time of
     * the last occurrence.
     *
     * @param calendarObject the recurring calendar object
     * @return the calculated until date
     * @see http://tools.ietf.org/html/rfc5545#section-3.3.10
     */
    private static DateTime getUntil(SeriesPattern pattern) {
        if (pattern.getSeriesEnd() == null) {
            return null;
        }

        if (pattern.isFullTime()) {
            return new DateTime(pattern.getSeriesEnd()).toAllDay();
        }

        /*
         * "OX" model defines until as 00:00:00 utc if the day of the last occurrence.
         */
        Calendar utcUntilCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcUntilCalendar.setTimeInMillis(pattern.getSeriesEnd());
        /*
         * iCal wants a correct inclusive until value. With time zone and time. So extract time from the start date.
         */
        Calendar effectiveUntilCalendar = Calendar.getInstance(pattern.getTimeZone());
//        Calendar seriesStart = pattern.getSeriesStartCalendar();
        Calendar seriesStart = Calendar.getInstance(pattern.getTimeZone());
        seriesStart.setTimeInMillis(pattern.getSeriesStart().longValue());
        effectiveUntilCalendar.set(utcUntilCalendar.get(Calendar.YEAR), utcUntilCalendar.get(Calendar.MONTH), utcUntilCalendar.get(Calendar.DAY_OF_MONTH), seriesStart.get(Calendar.HOUR_OF_DAY), seriesStart.get(Calendar.MINUTE), seriesStart.get(Calendar.SECOND));
        /*
         * finally, build an ical4j date-time
         */
//        DateTime dt = new DateTime(pattern.getTimeZone(), effectiveUntilCalendar.getTimeInMillis());
        DateTime dt = new DateTime(effectiveUntilCalendar.getTimeInMillis());
        return dt;
    }

    private static void setDays(CalendarObject cObj, RecurrenceRule rrule, Calendar startDate) {
        List<WeekdayNum> weekdayList = rrule.getByDayPart();
        if (weekdayList.isEmpty()) {
            int day_of_week = startDate.get(Calendar.DAY_OF_WEEK);
            int days = -1;
            switch (day_of_week) {
                case Calendar.MONDAY:
                    days = CalendarObject.MONDAY;
                    break;
                case Calendar.TUESDAY:
                    days = CalendarObject.TUESDAY;
                    break;
                case Calendar.WEDNESDAY:
                    days = CalendarObject.WEDNESDAY;
                    break;
                case Calendar.THURSDAY:
                    days = CalendarObject.THURSDAY;
                    break;
                case Calendar.FRIDAY:
                    days = CalendarObject.FRIDAY;
                    break;
                case Calendar.SATURDAY:
                    days = CalendarObject.SATURDAY;
                    break;
                case Calendar.SUNDAY:
                    days = CalendarObject.SUNDAY;
                    break;
                default:
            }
            cObj.setDays(days);
        } else {
            int days = 0;
            for (WeekdayNum weekday : weekdayList) {
                Integer day = weekdays.get(weekday.weekday.name());
                if (null == day) {
                    // TODO:
                }
                days |= day.intValue();
            }
            cObj.setDays(days);
        }
    }

    private static void setMonthDay(CalendarObject cObj, RecurrenceRule rrule, Calendar startDate) {
        List<Integer> monthDayList = rrule.getByPart(Part.BYMONTHDAY);
        if (null == monthDayList || monthDayList.isEmpty()) {
            List<Integer> weekNoList = rrule.getByPart(Part.BYWEEKNO);
            if (null != weekNoList && !weekNoList.isEmpty()) {
                int week = weekNoList.get(0).intValue();
                if (week == -1) {
                    week = 5;
                }
                cObj.setDayInMonth(week); // Day in month stores week
                setDays(cObj, rrule, startDate);
            } else if (null != rrule.getByDayPart() && !rrule.getByDayPart().isEmpty()) {
                setWeekdayInMonth(cObj, rrule);
                setDayInMonthFromSetPos(cObj, rrule);
            } else {
                // Default to monthly series on specific day of month
                cObj.setDayInMonth(startDate.get(Calendar.DAY_OF_MONTH));
            }
        } else {
            cObj.setDayInMonth(monthDayList.get(0).intValue());
        }
    }

    private static void setDayInMonthFromSetPos(CalendarObject obj, RecurrenceRule rrule) {
        if (!rrule.getByPart(Part.BYSETPOS).isEmpty()) {
            int firstPos = rrule.getByPart(Part.BYSETPOS).get(0);
            if (firstPos == -1) {
                firstPos = 5;
            }
            obj.setDayInMonth(firstPos);
        }
    }

    private static void setWeekdayInMonth(CalendarObject cObj, RecurrenceRule rrule) {
        List<WeekdayNum> weekdayList = rrule.getByDayPart();
        if (!weekdayList.isEmpty()) {
            int days = 0;
            for (WeekdayNum weekday : weekdayList) {
                Integer day = weekdays.get(weekday.weekday.name());
                if (null == day) {
                    // TODO:
                }
                int offset = weekday.pos;
                if (offset != 0) {
                    if (offset == -1) {
                        offset = 5;
                    }
                    cObj.setDayInMonth(offset);
                }
                days |= day.intValue();
            }
            cObj.setDays(days);
        }
    }

    private static void setOccurrenceIfNeededRecoveryFIXME(CalendarDataObject cObj, int recurrenceCount) {
        if (Appointment.class.isAssignableFrom(cObj.getClass())) {
            cObj.setOccurrence(recurrenceCount);
        }
    }

    private static SeriesPattern generatePattern(CalendarDataObject cdao, RecurrenceRule rrule) {
        if (!cdao.containsStartDate()) {
            return null;
        }

        int recurrenceType = cdao.getRecurrenceType();
        int interval = cdao.getInterval(); // i
        int weekdays = cdao.getDays();
        int monthday = cdao.getDayInMonth();
        int month = cdao.getMonth();
        int occurrences = cdao.getOccurrence();
        if (!cdao.containsUntil() && !cdao.containsOccurrence()) {
            occurrences = -1;
        }

        SeriesPattern pattern = new SeriesPattern(recurrenceType, cdao.getTimezone(), cdao.getFullTime());
        pattern.setInterval(interval);
        pattern.setSeriesStart(cdao.getStartDate().getTime());

        if (recurrenceType == CalendarObject.DAILY) {
            pattern.setType(SeriesPattern.DAILY);
        } else if (recurrenceType == CalendarObject.WEEKLY) {
            pattern.setType(SeriesPattern.WEEKLY);
            pattern.setDaysOfWeek(weekdays);
        } else if (recurrenceType == CalendarObject.MONTHLY) {
            if (monthday <= 0) {
                // TODO:
            }
            if (weekdays <= 0) {
                if (monthday > 31) {
                    // TODO:
                }
                pattern.setType(SeriesPattern.MONTHLY_1);
                pattern.setDayOfMonth(monthday);
            } else {
                if (monthday > 5) {
                    // TODO:
                }
                pattern.setType(SeriesPattern.MONTHLY_2);
                pattern.setDaysOfWeek(weekdays);
                pattern.setDayOfMonth(monthday);
            }
        } else if (recurrenceType == CalendarObject.YEARLY) {
            if (weekdays <= 0) {
                if (monthday <= 0 || monthday > 31) {
                    // TODO:
                }
                pattern.setType(SeriesPattern.YEARLY_1);
                pattern.setDayOfMonth(monthday);
                pattern.setMonth(month);
            } else {
                if (monthday < 1 || monthday > 5) {
                    // TODO:
                }
                pattern.setType(SeriesPattern.YEARLY_2);
                pattern.setDaysOfWeek(weekdays);
                pattern.setDayOfMonth(monthday);
                pattern.setMonth(month);
            }
        } else {
            throw new IllegalArgumentException("Invalid value for recurrence type: " + recurrenceType);
        }

        if (occurrences > 0) {
            pattern.setSeriesEnd(calculateUntilForUnlimited(cdao, rrule).getTime());
            pattern.setOccurrences(occurrences);
        } else if (cdao.containsUntil() && cdao.getUntil() != null) {
            pattern.setSeriesEnd(cdao.getUntil().getTime());
        } else {
            //            pattern.setSeriesEnd(calculateUntilForUnlimited(cdao, rrule).getTime());
        }
        return pattern;
    }

    private static Date calculateUntilForUnlimited(CalendarDataObject cObj, RecurrenceRule rrule) {
        DateTime start = new DateTime(cObj.getStartDate().getTime());
        RecurrenceRuleIterator iterator = rrule.iterator(start);
        int count = 0;
        long millis = 0L;
        while (iterator.hasNext() && count++ <= 999) { // TODO: implicit limit
            millis = iterator.nextMillis();
        }
        Calendar until = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        until.setTimeInMillis(millis);
        until.set(Calendar.HOUR_OF_DAY, 0);
        until.set(Calendar.MINUTE, 0);
        until.set(Calendar.SECOND, 0);
        until.set(Calendar.MILLISECOND, 0);
        return until.getTime();
    }

}
