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

import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventFlag;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.EventOccurrence;
import com.openexchange.chronos.compat.PositionAwareRecurrenceId;
import com.openexchange.chronos.service.RecurrenceData;
import com.openexchange.exception.OXException;
import com.openexchange.java.util.TimeZones;

/**
 * {@link RecurrenceIterator}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.0
 */
public class RecurrenceIterator extends AbstractRecurrenceIterator<Event> {

    private final Event master;

    /**
     * Initializes a new {@link RecurrenceIterator}.
     *
     * @param master The master event containing all necessary information like recurrence rule, star and end date, timezones etc.
     * @param forwardToOccurrence <code>true</code> to fast-forward the iterator to the first occurrence if the series master's start
     *            does not fall into the pattern, <code>false</code> otherwise
     * @param start The left side boundary for the calculation. Optional, can be null.
     * @param end The right side boundary for the calculation. Optional, can be null.
     * @param limit The maximum number of calculated instances. Optional, can be null.
     * @param ignoreExceptions Determines if exceptions should be ignored. If true, all occurrences are calculated as if no exceptions exist. Note: This does not add change exceptions. See {@link ChangeExceptionAwareRecurrenceIterator}
     */
    public RecurrenceIterator(Event master, boolean forwardToOccurrence, Calendar start, Calendar end, Integer limit, boolean ignoreExceptions) throws OXException {
        super(master, forwardToOccurrence, start, end, limit, ignoreExceptions);
        this.master = master;
    }

    /**
     * Initializes a new {@link RecurrenceIterator}.
     *
     * @param recurrenceData The recurrence data
     * @param master The master event containing all necessary information like recurrence rule, star and end date, timezones etc.
     * @param forwardToOccurrence <code>true</code> to fast-forward the iterator to the first occurrence if the series master's start
     *            does not fall into the pattern, <code>false</code> otherwise
     * @param start The left side boundary for the calculation. Optional, can be null.
     * @param end The right side boundary for the calculation. Optional, can be null.
     * @param limit The maximum number of calculated instances. Optional, can be null.
     */
    public RecurrenceIterator(RecurrenceData recurrenceData, Event master, boolean forwardToOccurrence, Calendar start, Calendar end, Integer limit) throws OXException {
        super(recurrenceData, getEventDuration(master), forwardToOccurrence, start, end, null, limit);
        this.master = master;
    }

    @Override
    protected Event nextInstance() {
        PositionAwareRecurrenceId recurrenceId = new PositionAwareRecurrenceId(recurrenceData, next, position, CalendarUtils.truncateTime(new Date(next.getTimestamp()), TimeZones.UTC));
        /*
         * extend flags (if actually set and not null in master) by "first" / "last" occurrence flag dynamically
         */
        if (1 == position) {
            return new EventOccurrence(master, recurrenceId) {

                @Override
                public EnumSet<EventFlag> getFlags() {
                    EnumSet<EventFlag> flags = super.getFlags();
                    if (null != flags) {
                        flags = EnumSet.copyOf(flags);
                        flags.add(EventFlag.FIRST_OCCURRENCE);
                    }
                    return flags;
                }
            };
        }
        if (isLastOccurrence()) {
            return new EventOccurrence(master, recurrenceId) {

                @Override
                public EnumSet<EventFlag> getFlags() {
                    EnumSet<EventFlag> flags = super.getFlags();
                    if (null != flags) {
                        flags = EnumSet.copyOf(flags);
                        flags.add(EventFlag.LAST_OCCURRENCE);
                    }
                    return flags;
                }
            };
        }
        return new EventOccurrence(master, recurrenceId);
    }

}
