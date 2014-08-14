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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.subscribe.google;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.user.SimUserService;
import com.openexchange.user.UserService;


/**
 * {@link GoogleCalendarTest}
 *
 * @author <a href="mailto:lars.hoogestraat@open-xchange.com">Lars Hoogestraat</a>
 * @since v7.6.1
 */

@PrepareForTest({com.openexchange.subscribe.google.osgi.Services.class})
public class GoogleCalendarTest extends AbstractGoogleTest {
    public void testCalendar() throws Exception {
        try {
            LinkedList<CalendarDataObject> calendarObjects = getGoogleCalendarObjects();

            // appointments for testing
            final String singleAppointment = "Single appointment - only organizer - 29 Jan 2014";
            boolean successSingleAppointment = false;

            // final String allDayAppointment = "All day appointment - only organizer - 30 Jan 2014";
            // boolean successAllDayAppointment = false;
            // final String recurrenceAppointment = "Recurrence appointment with organizer 27 Jan 2014 - 14 March 2014";
            // boolean successRecurrenceAppointment = false;
            // final String recurrenceAppointmentWithParts = "Recurrence appointment - with internal / external participants - 15 March 2014 - Never ending";
            // boolean successRecurrenceAppointmentWithParts = false;

            for(CalendarDataObject co : calendarObjects) {
                if(co.getTitle() != null) {
                    if(co.getTitle().equals(singleAppointment)) {
                        testSingleAppointment(co);
                        successSingleAppointment = true;
                    }
                }
                if(successSingleAppointment) {
                    return;
                }
            }

            assertTrue(successSingleAppointment);
        } catch(OXException e) {
            assertFalse(e.getMessage() ,true);
        }
    }

    private void testSingleAppointment(CalendarDataObject co) {
        assertNotNullAndEquals("context", 1, co.getContext().getContextId());
        assertNotNull("user id", 1, co.getUid());
        assertNotNullAndEquals("location", "Olpe, Deutschland" , co.getLocation());
        assertNotNullAndEquals("note", "Single appointment - only organizer - 29 Jan 2014 \n\nSome text..." , co.getNote());
        assertNotNullAndEquals("start date", setDateTime(29, 1, 2014, 13, 30), co.getStartDate());
        assertNotNullAndEquals("timezone", "America/Santiago", co.getTimezone());
        assertNotNullAndEquals("end date", setDateTime(29, 1, 2014, 15, 30), co.getEndDate());
        assertNotNullAndEquals("creation date", setDateTime(8, 8, 2014, 15, 32, 33), co.getCreationDate());
        assertNotNullAndEquals("created by", 1, co.getCreatedBy());
        assertNotNullAndEquals("alarm", 45 ,co.getAlarm());

        assertNull("This appointment has no confirmation, but the mapping exist", co.getConfirmations());
        assertNull("This appointment has no participants, but the mapping exist", co.getConfirmations());

        // TODO: currently not tested
        //        for(ConfirmableParticipant cp : co.getConfirmations()) {
        //            cp.getStatus();
        //            cp.getMessage();
        //            cp.getDisplayName();
        //        }

        //        for(Participant p : co.getParticipants()) {
        //            p.getDisplayName();
        //        }
        //        co.getOrganizer();
        //        co.getCategories();
        //        String confirmMessage = a.getComment();

    }

    private Date setDateTime(int day, int month, int year, int hour, int minute) {
        return setDateTime(day, month, year, hour, minute, 0);
    }

    private Date setDateTime(int day, int month, int year, int hour, int minute, int seconds) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, seconds);
        return cal.getTime();
    }

    protected void prepareAdditionalMocks() throws Exception {
        SimUserService simUser = new SimUserService();
        PowerMockito.mockStatic(com.openexchange.subscribe.google.osgi.Services.class);
        PowerMockito.doReturn(simUser).when(com.openexchange.subscribe.google.osgi.Services.class, "getService", Matchers.any(UserService.class));
    }


}
