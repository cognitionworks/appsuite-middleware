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
 *    trademarks of the OX Software GmbH. group of companies.
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

package com.openexchange.chronos.recurrence.service;

import java.util.Arrays;
import java.util.Calendar;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.recurrenceset.RecurrenceSetIterator;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.ChronosLogger;
import com.openexchange.chronos.common.DefaultRecurrenceData;
import com.openexchange.chronos.service.RecurrenceData;
import com.openexchange.chronos.service.RecurrenceIterator;
import com.openexchange.exception.OXException;

/**
 * {@link AbstractRecurrenceIterator}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.0
 */
public abstract class AbstractRecurrenceIterator<T> implements RecurrenceIterator<T> {

    public static final int MAX = 1000;

    protected final Calendar start;
    protected final Integer startPosition;
    protected final Calendar end;
    protected final Integer limit;
    protected final RecurrenceData recurrenceData;
    protected final long eventDuration;

    protected DateTime next;
    protected int count;
    protected int position;
    protected RecurrenceSetIterator inner;
    protected final long[] exceptionDates;



    /**
     * Initializes a new {@link AbstractRecurrenceIterator}.
     *
     * @param recurrenceData The underlying recurrence data
     * @param forwardToOccurrence <code>true</code> to fast-forward the iterator to the first occurrence if the recurrence data's start
     *            does not fall into the pattern, <code>false</code> otherwise
     * @param exceptionDates A sorted array of change- and delete-exception timestamps to ignore during iteration, or <code>null</code> if not set
     * @param start The left side boundary for the calculation. Optional, can be null.
     * @param end The right side boundary for the calculation. Optional, can be null.
     * @param limit The maximum number of calculated instances. Optional, can be null.
     * @param ignoreExceptions Determines if exceptions should be ignored. If true, all occurrences are calculated as if no exceptions exist. Note: This does not add change exceptions. See {@link ChangeExceptionAwareRecurrenceIterator}
     */
    protected AbstractRecurrenceIterator(Event master, boolean forwardToOccurrence, Calendar start, Calendar end, Integer limit, boolean ignoreExceptions) throws OXException {
        this(new DefaultRecurrenceData(master.getRecurrenceRule(), master.getStartDate(), null), getEventDuration(master), forwardToOccurrence, ignoreExceptions ? null : getExceptionDates(master), start, end, null, limit);
    }

    /**
     * Initializes a new {@link AbstractRecurrenceIterator}.
     *
     * @param recurrenceData The underlying recurrence data
     * @param eventDuration The duration of the underlying series master event, or <code>0</code> if not considered
     * @param forwardToOccurrence <code>true</code> to fast-forward the iterator to the first occurrence if the recurrence data's start
     *            does not fall into the pattern, <code>false</code> otherwise
     * @param exceptionDates A sorted array of change- and delete-exception timestamps to ignore during iteration, or <code>null</code> if not set
     * @param start The left side boundary for the calculation. Optional, can be null.
     * @param end The right side boundary for the calculation. Optional, can be null.
     * @param startPosition The start recurrence position for the calculation. Optional, can be null.
     * @param limit The maximum number of calculated instances. Optional, can be null.
     */
    protected AbstractRecurrenceIterator(RecurrenceData recurrenceData, long eventDuration, boolean forwardToOccurrence, long[] exceptionDates, Calendar start, Calendar end, Integer startPosition, Integer limit) throws OXException {
        super();
        this.recurrenceData = recurrenceData;
        this.eventDuration = eventDuration;
        this.start = start;
        this.startPosition = startPosition;
        this.end = end;
        this.limit = limit;
        this.exceptionDates = exceptionDates;
        if (limit != null && limit == 0) {
            ChronosLogger.debug("Occurrence limit set to 0, nothing to do.");
            return;
        }
        inner = RecurrenceUtils.getRecurrenceIterator(recurrenceData, forwardToOccurrence);
        next = null;
        position = 0;
        count = 0;
        init();
    }

    private Long lookAhead = null;

    private void init() {
        if (null != start || null != startPosition && 1 < startPosition.intValue()) {
            while (inner.hasNext()) {
                long candidate = inner.next();
                if (isException(candidate)) {
                    position++;
                    continue;
                }
                if (start != null && candidate + eventDuration > start.getTimeInMillis() ||
                    start != null && 0L == eventDuration && candidate == start.getTimeInMillis() ||
                    startPosition != null && position + 1 >= startPosition.intValue()) {
                    lookAhead = candidate;
                    break;
                } else {
                    position++;
                }
            }
        }

        innerNext();
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public T next() {
        if (next == null) {
            throw new NoSuchElementException();
        }

        T retval = nextInstance();
        innerNext();
        return retval;
    }

    protected abstract T nextInstance();

    private void innerNext() {
        if (count >= MAX) {
            ChronosLogger.debug("Reached internal limit. Stop calculation.");
            next = null;
            return;
        }

        if (limit != null && count >= limit) {
            ChronosLogger.debug("Reached given limit. Stop calculation.");
            next = null;
            return;
        }

        if (!inner.hasNext() && lookAhead == null) {
            ChronosLogger.debug("No more instances available.");
            next = null;
            return;
        }

        if (lookAhead == null) {
            lookAhead = inner.next();
        }

        while (isException(lookAhead)) {
            ChronosLogger.debug("Next instance is exception.");
            position++;
            if (inner.hasNext()) {
                lookAhead = inner.next();
            } else {
                next = null;
                return;
            }
        }

        if (this.end != null && lookAhead >= this.end.getTimeInMillis()) {
            ChronosLogger.debug("Next instance ({}) reached end boundary ({}).", lookAhead, this.end.getTimeInMillis());
            next = null;
            return;
        }

        if (lookAhead == null) {
            next = toDateTime(inner.next());
        } else {
            next = toDateTime(lookAhead.longValue());
            lookAhead = null;
        }
        count++;
        position++;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    @Override
    public int getPosition() {
        return null == next ? position : position - 1;
    }

    /**
     * Gets the date-time representation of a timestamp retrieved from the underlying recurrence set iterator.
     * <p/>
     * The actual value type and timezone are taken over from the recurrence data.
     *
     * @param timestamp The timestamp as retrieved from the underlying iterator
     * @return The date-time with suitable type for actual recurrence data
     */
    private DateTime toDateTime(long timestamp) {
        DateTime seriesStart = recurrenceData.getSeriesStart();
        if (null == seriesStart) {
            return new DateTime(timestamp);
        }
        if (seriesStart.isAllDay()) {
            return new DateTime(null, timestamp).toAllDay();
        }
        return new DateTime(seriesStart.getCalendarMetrics(), seriesStart.getTimeZone(), timestamp);
    }

    private boolean isException(long start) {
        return null != exceptionDates && -1 < Arrays.binarySearch(exceptionDates, start);
    }

    /**
     * Builds a sorted array containing the recurrence identifier values from both the change- and delete-exceptions of the supplied
     * series master event.
     *
     * @param seriesMaster The series master event to get all exception dates for
     * @return The timestamps of the exceptions dates in a sorted array, or an <code>null</code> if there are none
     */
    private static long[] getExceptionDates(Event seriesMaster) {
        SortedSet<RecurrenceId> exceptionDates = new TreeSet<RecurrenceId>();
        if (null != seriesMaster.getDeleteExceptionDates()) {
            exceptionDates.addAll(seriesMaster.getDeleteExceptionDates());
        }
        if (exceptionDates.isEmpty()) {
            return null;
        }
        long[] timestamps = new long[exceptionDates.size()];
        int position = 0;
        for (RecurrenceId recurrenceId : exceptionDates) {
            timestamps[position++] = recurrenceId.getValue().getTimestamp();
        }
        return timestamps;
    }

    /**
     * Gets the event's duration in milliseconds.
     *
     * @param event The event
     * @return The duration
     */
    private static long getEventDuration(Event event) {
        return event.getEndDate().getTimestamp() - event.getStartDate().getTimestamp();
    }

}
