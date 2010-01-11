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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.ajax.parser;

import static com.openexchange.ajax.fields.TaskFields.ACTUAL_DURATION;
import static com.openexchange.ajax.fields.TaskFields.TARGET_DURATION;
import java.util.TimeZone;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.fields.CalendarFields;
import com.openexchange.ajax.fields.TaskFields;
import com.openexchange.api.OXConflictException;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.tools.servlet.OXJSONException;

/**
 * TaskParser
 * 
 * @author <a href="mailto:sebastian.kauss@netline-is.de">Sebastian Kauss</a>
 */
public class TaskParser extends CalendarParser {

    public TaskParser(final TimeZone timeZone) {
        super(timeZone);
    }

    public TaskParser(final boolean parseAll, final TimeZone timeZone) {
        super(parseAll, timeZone);
    }

    public void parse(final Task taskobject, final JSONObject jsonobject)
        throws OXJSONException {
        try {
            parseElementTask(taskobject, jsonobject);
        } catch (final OXJSONException e) {
            throw e;
        } catch (final Exception exc) {
            throw new OXJSONException(OXJSONException.Code.JSON_READ_ERROR, exc);
        }
    }

    protected void parseElementTask(Task taskobject, JSONObject json) throws JSONException, OXJSONException, OXConflictException {
        if (json.has(CalendarFields.START_DATE)) {
            taskobject.setStartDate(parseDate(json, CalendarFields.START_DATE));
        }
        if (json.has(CalendarFields.END_DATE)) {
            taskobject.setEndDate(parseDate(json, CalendarFields.END_DATE));
        }
        if (json.has(TaskFields.STATUS)) {
            taskobject.setStatus(parseInt(json, TaskFields.STATUS));
        }
        if (json.has(TaskFields.ACTUAL_COSTS)) {
            taskobject.setActualCosts(parseFloat(json, TaskFields.ACTUAL_COSTS));
        }
        if (json.has(ACTUAL_DURATION)) {
            taskobject.setActualDuration(parseLong(json, ACTUAL_DURATION));
        }
        if (json.has(TaskFields.PERCENT_COMPLETED)) {
            taskobject.setPercentComplete(parseInt(json, TaskFields.PERCENT_COMPLETED));
        }
        if (json.has(TaskFields.DATE_COMPLETED)) {
            taskobject.setDateCompleted(parseDate(json, TaskFields.DATE_COMPLETED));
        }
        if (json.has(TaskFields.BILLING_INFORMATION)) {
            taskobject.setBillingInformation(parseString(json, TaskFields.BILLING_INFORMATION));
        }
        if (json.has(TaskFields.TARGET_COSTS)) {
            taskobject.setTargetCosts(parseFloat(json, TaskFields.TARGET_COSTS));
        }
        if (json.has(TARGET_DURATION)) {
            taskobject.setTargetDuration(parseLong(json, TARGET_DURATION));
        }
        if (json.has(TaskFields.PRIORITY)) {
            taskobject.setPriority(parseInt(json, TaskFields.PRIORITY));
        }
        if (json.has(TaskFields.CURRENCY)) {
            taskobject.setCurrency(parseString(json, TaskFields.CURRENCY));
        }
        if (json.has(TaskFields.TRIP_METER)) {
            taskobject.setTripMeter(parseString(json, TaskFields.TRIP_METER));
        }
        if (json.has(TaskFields.COMPANIES)) {
            taskobject.setCompanies(parseString(json, TaskFields.COMPANIES));
        }
        if (json.has(CalendarFields.ALARM)) {
            taskobject.setAlarm(parseTime(json, CalendarFields.ALARM, timeZone));
        }
        parseElementCalendar(taskobject, json);
    }
}
