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

package com.openexchange.chronos.service;

import java.util.Date;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import com.openexchange.chronos.Classification;
import com.openexchange.chronos.EventField;

/**
 * {@link CalendarParameters}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public interface CalendarParameters {

    /**
     * {@link Boolean}
     * <p/>
     * Indicates whether "private" events of the calendar owner, i.e. events whose classification is {@link Classification#PRIVATE} or
     * {@link Classification#CONFIDENTIAL} should be included when fetching events from a shared folder or not. Such events are
     * anonymized by stripping away all information except start date, end date and recurrence information.
     */
    static final String PARAMETER_INCLUDE_PRIVATE = "showPrivate";

    /**
     * {@link Boolean}
     * <p/>
     * Configures if only the <i>master</i> event of a series should be returned, or if recurring events should be resolved into their
     * individual instances.
     *
     * @see <a href="https://tools.ietf.org/html/rfc4791#section-9.6.5">RFC 4791, section 9.6.5</a>
     */
    static final String PARAMETER_EXPAND_OCCURRENCES = "expand";

    /**
     * {@link Date}
     * <p/>
     * Specifies the lower inclusive limit of the queried range, i.e. only events which start on or after this date should be returned.
     */
    static final String PARAMETER_RANGE_START = "start";

    /**
     * {@link Date}
     * <p/>
     * Specifies the upper exclusive limit of the queried range, i.e. only appointments which end before this date should be returned.
     */
    static final String PARAMETER_RANGE_END = "end";

    /**
     * Array of {@link EventField}
     * <p/>
     * Allows to restrict the returned properties of retrieved event data.
     */
    static final String PARAMETER_FIELDS = "fields";

    /**
     * {@link EventField}
     * <p/>
     * Specifies the field for sorting the results.
     */
    static final String PARAMETER_ORDER_BY = "sort";

    /**
     * {@link String}
     * <p/>
     * The sort order to apply, either <code>ASC</code> for ascending, or <code>DESC</code> for descending.
     */
    static final String PARAMETER_ORDER = "order";

    /**
     * {@link TimeZone}
     * <p/>
     * Provides a (possibly overridden) timezone used on a per-request basis.
     * <p/>
     * The timezone is used to resolve <i>floating</i> date-times to concrete timestamps when determining if an event intersects with a
     * given range.
     *
     * @see <a href="https://tools.ietf.org/html/rfc4791#section-9.8">RFC 4791, section 9.8</a>
     */
    static final String PARAMETER_TIMEZONE = "timezone";

    /**
     * {@link Boolean}
     * <p/>
     * Indicates whether (soft) conflicts of attendees should be ignored when saving a meeting.
     */
    static final String PARAMETER_IGNORE_CONFLICTS = "ignore_conflicts";

    /**
     * {@link Boolean}
     * <p/>
     * Specifies that attendees should be notified about the changes when saving a meeting or not.
     */
    static final String PARAMETER_NOTIFICATION = "notification";

    /**
     * {@link Integer}
     * <p/>
     * A positive integer number to specify the "left-hand" limit of the range to return.
     */
    static final String PARAMETER_LEFT_HAND_LIMIT = "left_hand_limit";

    /**
     * {@link Integer}
     * <p/>
     * A positive integer number to specify the "right-hand" limit of the range to return.
     */
    static final String PARAMETER_RIGHT_HAND_LIMIT = "right_hand_limit";

    /**
     * {@link String[]}
     * <p/>
     * A collection of values that should be "ignored" when retrieving results, currently known values are <code>deleted</code> and
     * <code>changed</code> when serving the "updates" request.
     */
    static final String PARAMETER_IGNORE = "ignore";

    /**
     * {@link Boolean}
     * <p/>
     * Indicates whether the current calendar user should be added as default attendee to events implicitly or not, independently of the
     * event being <i>group-scheduled</i> or not.
     * <p/>
     * If set to <code>true</code>, an attendee representing the current calendar user as well as a corresponding organizer will be
     * implicitly added by the service during event creation, and an attempt to remove this default attendee will be ignored silently.
     */
    static final String PARAMETER_DEFAULT_ATTENDEE = "default_attendee";

    /**
     * {@link String}
     * <p/>
     * The recurrence id of a series event
     * <p/>
     * If set the operation only applies to this recurrence and not to the master.
     */
    static final String PARAMETER_RECURRENCE_ID = "recurrenceId";

    /**
     * {@link String}
     * <p/>
     * The identifier of an existing event or event series to ignore when calculating free/busy information.
     * <p/>
     * If set, existing events with this identifier are implicitly excluded during free/busy lookups, which aids to ignore the event
     * itself when it is about to be re-scheduled. If the identifier of an event series is specified, all regular occurrences of the
     * series, as well as any overridden instance will be excluded, too.
     *
     * @see <a href="https://raw.githubusercontent.com/apple/ccs-calendarserver/master/doc/Extensions/icalendar-maskuids.txt">icalendar-maskuids-03, section 4.1</a>
     */
    static final String PARAMETER_MASK_ID = "maskId";

    /**
     * {@link Long}
     * <p/>
     * Specifies the field for the timestamp.
     */
    static final String PARAMETER_TIMESTAMP = "timestamp";

    /**
     * Sets a parameter.
     *
     * @param parameter The parameter name to set
     * @param value The value to set
     * @return A self reference
     */
    <T> CalendarParameters set(String parameter, T value);

    /**
     * Gets a parameter.
     *
     * @param parameter The parameter name
     * @param clazz The value's target type
     * @return The parameter value, or <code>null</code> if not set
     */
    <T> T get(String parameter, Class<T> clazz);

    /**
     * Gets a parameter, falling back to a custom default value if not set.
     *
     * @param parameter The parameter name
     * @param clazz The value's target type
     * @param defaultValue The default value to use as fallback if the parameter is not set
     * @return The parameter value, or the passed default value if not set
     */
    <T> T get(String parameter, Class<T> clazz, T defaultValue);

    /**
     * Gets a value indicating whether a specific parameter is set.
     *
     * @param parameter The parameter name
     * @return <code>true</code> if the parameter is set, <code>false</code>, otherwise
     */
    boolean contains(String parameter);

    /**
     * Gets a set of all configured parameters.
     *
     * @return All parameters as set
     */
    Set<Entry<String, Object>> entrySet();

}
