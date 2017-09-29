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
 *    trademarks of the OX Software GmbH group of companies.
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

package com.openexchange.ajax.chronos;

import java.text.ParseException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import com.openexchange.ajax.chronos.factory.AlarmFactory;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.manager.ChronosApiException;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.AlarmTrigger;
import com.openexchange.testing.httpclient.models.AlarmTriggerData;
import com.openexchange.testing.httpclient.models.DateTimeData;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventId;
import com.openexchange.testing.httpclient.models.Trigger.RelatedEnum;

/**
 *
 * {@link TimezoneAlarmTriggerTest} tests alarm triggers for events with different start and end timezones.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.0
 */
public class TimezoneAlarmTriggerTest extends AbstractUserTimezoneAlarmTriggerTest {

    /**
     * Initialises a new {@link TimezoneAlarmTriggerTest}.
     */
    public TimezoneAlarmTriggerTest() {
        super();
    }

    @Test
    public void testTrigger() throws ApiException, ParseException, ChronosApiException {
        Calendar start = DateTimeUtil.getUTCCalendar();
        start.add(Calendar.DAY_OF_MONTH, 1);
        DateTimeData startDate = DateTimeUtil.getDateTime(start);

        TimeZone endTimeZone = TimeZone.getTimeZone("Europe/Berlin");
        Calendar end = Calendar.getInstance(endTimeZone);
        end.setTimeInMillis(start.getTimeInMillis());
        end.add(Calendar.HOUR, 10);
        DateTimeData endDate = DateTimeUtil.getDateTime(end);

        /*
         * 1. Test alarm related to start
         */
        {
            EventData toCreate = EventFactory.createSingleEventWithSingleAlarm(defaultUserApi.getCalUser(), testUser.getLogin(), "testPositiveDurationTrigger", startDate, endDate, AlarmFactory.createDisplayAlarm("-PT10M"));
            EventData event = eventManager.createEvent(toCreate);
            getAndAssertAlarms(event, 1);

            // Get alarms within the next two days
            AlarmTriggerData triggers = getAndCheckAlarmTrigger(1); // one trigger
            AlarmTrigger alarmTrigger = triggers.get(0);
            checkAlarmTime(alarmTrigger, event.getId(), start.getTimeInMillis() - TimeUnit.MINUTES.toMillis(10));
            EventId eventId = new EventId();
            eventId.setFolderId(event.getFolder());
            eventId.setId(event.getId());
            eventId.setRecurrenceId(event.getRecurrenceId());
            eventManager.deleteEvent(eventId);
        }
        /*
         * 2. Test alarm related to end
         */
        {
            EventData toCreate = EventFactory.createSingleEventWithSingleAlarm(defaultUserApi.getCalUser(), testUser.getLogin(), "testPositiveDurationTrigger", startDate, endDate, AlarmFactory.createAlarm("-PT10M", RelatedEnum.END));
            EventData event = eventManager.createEvent(toCreate);
            getAndAssertAlarms(event, 1);

            // Get alarms within the next two days
            AlarmTriggerData triggers = getAndCheckAlarmTrigger(1); // one trigger
            AlarmTrigger alarmTrigger = triggers.get(0);
            int offset = endTimeZone.getOffset(end.getTimeInMillis());
            checkAlarmTime(alarmTrigger, event.getId(), end.getTimeInMillis() - offset - TimeUnit.MINUTES.toMillis(10));
        }
    }

    @Test
    public void testTriggerWithPositiveDuration() throws ApiException, ParseException, ChronosApiException {
        Calendar start = DateTimeUtil.getUTCCalendar();
        start.add(Calendar.DAY_OF_MONTH, 1);
        DateTimeData startDate = DateTimeUtil.getDateTime(start);

        TimeZone endTimeZone = TimeZone.getTimeZone("Europe/Berlin");
        Calendar end = Calendar.getInstance(endTimeZone);
        end.setTimeInMillis(start.getTimeInMillis());
        end.add(Calendar.HOUR, 10);
        DateTimeData endDate = DateTimeUtil.getDateTime(end);

        /*
         * 1. Test alarm related to start
         */
        {
            EventData toCreate = EventFactory.createSingleEventWithSingleAlarm(defaultUserApi.getCalUser(), testUser.getLogin(), "testPositiveDurationTrigger", startDate, endDate, AlarmFactory.createDisplayAlarm("-PT10M"));
            EventData event = eventManager.createEvent(toCreate);
            getAndAssertAlarms(event, 1);

            // Get alarms within the next two days
            AlarmTriggerData triggers = getAndCheckAlarmTrigger(1); // one trigger
            AlarmTrigger alarmTrigger = triggers.get(0);
            checkAlarmTime(alarmTrigger, event.getId(), start.getTimeInMillis() - TimeUnit.MINUTES.toMillis(10));
            EventId eventId = new EventId();
            eventId.setFolderId(event.getFolder());
            eventId.setId(event.getId());
            eventId.setRecurrenceId(event.getRecurrenceId());
            eventManager.deleteEvent(eventId);
        }
        /*
         * 2. Test alarm related to end
         */
        {
            EventData toCreate = EventFactory.createSingleEventWithSingleAlarm(defaultUserApi.getCalUser(), testUser.getLogin(), "testPositiveDurationTrigger", startDate, endDate, AlarmFactory.createAlarm("PT10M", RelatedEnum.END));
            EventData event = eventManager.createEvent(toCreate);
            getAndAssertAlarms(event, 1);

            // Get alarms within the next two days
            AlarmTriggerData triggers = getAndCheckAlarmTrigger(1); // one trigger
            AlarmTrigger alarmTrigger = triggers.get(0);
            int offset = endTimeZone.getOffset(end.getTimeInMillis());
            checkAlarmTime(alarmTrigger, event.getId(), end.getTimeInMillis() - offset + TimeUnit.MINUTES.toMillis(10));
        }
    }
}
