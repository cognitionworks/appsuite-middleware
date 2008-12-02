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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.data.conversion.ical;

import static com.openexchange.data.conversion.ical.Assert.assertNoProperty;
import static com.openexchange.data.conversion.ical.Assert.assertProperty;
import static com.openexchange.data.conversion.ical.Assert.assertStandardAppFields;
import static com.openexchange.groupware.calendar.tools.CommonAppointments.D;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import com.openexchange.data.conversion.ical.ical4j.ICal4JEmitter;
import com.openexchange.data.conversion.ical.ical4j.internal.UserResolver;
import com.openexchange.groupware.container.AppointmentObject;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.container.ExternalUserParticipant;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.ResourceParticipant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.MockUserLookup;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.tasks.Task;

/**
 * @author Francisco Laguna <francisco.laguna@open-xchange.com>
 */
public class ICalEmitterTest extends TestCase {

    private ICal4JEmitter emitter;
    private MockUserLookup users;
    private UserResolver oldUserResolver;

    private AppointmentObject getDefault() {
        final AppointmentObject app = new AppointmentObject();

        final Date start = D("24/02/1981 10:00");
        final Date end = D("24/02/1981 12:00");

        app.setStartDate(start);
        app.setEndDate(end);

        return app;

    }

    public void testSimpleAppointment() throws Exception {
        final AppointmentObject app = new AppointmentObject();

        app.setTitle("The Title");
        app.setNote("The Note");
        app.setCategories("cat1, cat2, cat3");
        app.setLocation("The Location");

        final Date start = D("24/02/1981 10:00");
        final Date end = D("24/02/1981 12:00");

        app.setStartDate(start);
        app.setEndDate(end);

        final ICalFile ical = serialize(app);

        assertStandardAppFields(ical, start, end);
        assertProperty(ical, "SUMMARY","The Title");
        assertProperty(ical, "DESCRIPTION","The Note");
        assertProperty(ical, "CATEGORIES","cat1, cat2, cat3");
        assertProperty(ical, "LOCATION","The Location");
    }


    public void testAppWholeDay() throws IOException {
        final AppointmentObject app = new AppointmentObject();
        app.setStartDate(D("24/02/1981 00:00"));
        app.setEndDate(D("26/02/1981 00:00"));
        app.setFullTime(true);

        final ICalFile ical = serialize(app);

        assertProperty(ical, "DTSTART;VALUE=DATE", "19810224");
        assertProperty(ical, "DTEND;VALUE=DATE", "19810226");
    }

    public void testCategoriesMayBeNullOrUnset() throws Exception {
        final AppointmentObject app = new AppointmentObject();
        ICalFile ical = serialize(app);

        assertNoProperty(ical, "CATEGORIES");

        app.setCategories(null);
        ical = serialize(app);

        assertNoProperty(ical, "CATEGORIES");
    }

    public void testDeleteExceptionsMayBeNull() throws Exception {
        final AppointmentObject app = new AppointmentObject();
        app.setDeleteExceptions(null);
        serialize(app);
        assertTrue("Just testing survival", true);
    }

    public void testAppCreated() throws IOException {
        final AppointmentObject appointment = getDefault();
        appointment.setCreationDate(D("24/02/1981 10:00"));

        final ICalFile ical = serialize(appointment);

        assertProperty(ical, "CREATED", "19810224T100000Z");
    }

    public void testAppLastModified() throws IOException {
        final AppointmentObject appointment = getDefault();
        appointment.setLastModified(D("24/02/1981 10:00"));

        final ICalFile ical = serialize(appointment);

        assertProperty(ical, "LAST-MODIFIED", "19810224T100000Z");
    }

    public void testAppRecurrence() throws IOException {

        // DAILY

        AppointmentObject appointment = getDefault();
        appointment.setRecurrenceCount(3);
        appointment.setRecurrenceType(AppointmentObject.DAILY);
        appointment.setInterval(2);

        ICalFile ical = serialize(appointment);

        assertProperty(ical, "RRULE", "FREQ=DAILY;INTERVAL=2;COUNT=3");

        // WEEKLY

        appointment.setRecurrenceType(AppointmentObject.WEEKLY);

        int days = 0;
        days |= AppointmentObject.MONDAY;
        days |= AppointmentObject.WEDNESDAY;
        days |= AppointmentObject.FRIDAY;

        appointment.setDays(days);

        ical = serialize(appointment);

        assertProperty(ical, "RRULE", "FREQ=WEEKLY;INTERVAL=2;COUNT=3;BYDAY=MO,WE,FR");

        // MONTHLY

        // First form: on 23rd day every 2 months

        appointment.setRecurrenceType(AppointmentObject.MONTHLY);
        appointment.removeDays();
        appointment.setDayInMonth(23);

        ical = serialize(appointment);

        assertProperty(ical, "RRULE", "FREQ=MONTHLY;INTERVAL=2;COUNT=3;BYMONTHDAY=23");


        // Second form : the 2nd monday and tuesday every 2 months

        appointment.setDayInMonth(3);

        days = 0;
        days |= AppointmentObject.MONDAY;
        days |= AppointmentObject.TUESDAY;
        appointment.setDays(days);

        ical = serialize(appointment);

        assertProperty(ical, "RRULE", "FREQ=MONTHLY;INTERVAL=2;COUNT=3;BYWEEKNO=3;BYDAY=MO,TU");


        // Second form : the last tuesday every 2 months

        appointment.setDayInMonth(5);
        appointment.setDays(AppointmentObject.TUESDAY);

        ical = serialize(appointment);

        assertProperty(ical, "RRULE", "FREQ=MONTHLY;INTERVAL=2;COUNT=3;BYWEEKNO=-1;BYDAY=TU");

        appointment.removeDays();

        // YEARLY

        // First form: Every 2 years, the 23rd of March
        appointment.removeDays();
        appointment.setRecurrenceType(AppointmentObject.YEARLY);
        appointment.setMonth(2);
        appointment.setDayInMonth(23);
        ical = serialize(appointment);

        assertProperty(ical, "RRULE", "FREQ=YEARLY;INTERVAL=2;COUNT=3;BYMONTH=3;BYMONTHDAY=23");

        // Second form: 2nd monday and wednesday in april every 2 years
        appointment.setMonth(3);
        appointment.setDayInMonth(2);

        days = 0;
        days |= AppointmentObject.MONDAY;
        days |= AppointmentObject.WEDNESDAY;
        appointment.setDays(days);
        ical = serialize(appointment);

        assertProperty(ical, "RRULE", "FREQ=YEARLY;INTERVAL=2;COUNT=3;BYMONTH=4;BYWEEKNO=2;BYDAY=MO,WE");

        // UNTIL

        appointment = getDefault();
        appointment.setRecurrenceType(AppointmentObject.DAILY);
        appointment.setInterval(2);
        appointment.setUntil(D("23/04/1989 00:00"));
        ical = serialize(appointment);

        assertProperty(ical, "RRULE", "FREQ=DAILY;INTERVAL=2;UNTIL=19890423");

        // Ignore RRULE for Exceptions
        appointment = getDefault();
        appointment.setRecurrenceType(AppointmentObject.DAILY);
        appointment.setInterval(2);
        appointment.setUntil(D("23/04/1989 00:00"));
        appointment.setObjectID(1);
        appointment.setRecurrenceID(2);
        ical = serialize(appointment);

        assertNoProperty(ical, "RRULE");        

    }


    public void testAppAlarm() throws IOException {
        final int MINUTES = 1;

        final AppointmentObject appointment = getDefault();
        appointment.setAlarm(15 *MINUTES);
        appointment.setNote("Blupp");
        
        final ICalFile ical = serialize(appointment);

        assertProperty(ical, "BEGIN", "VALARM");
        assertProperty(ical, "ACTION", "DISPLAY");
        assertProperty(ical, "TRIGGER", "-PT15M");
        assertProperty(ical, "DESCRIPTION", "Blupp");


    }

    public void testAppPrivateFlag() throws IOException {
        final AppointmentObject app = getDefault();

        app.setPrivateFlag(false);
        ICalFile ical = serialize(app);

        assertProperty(ical, "CLASS", "PUBLIC");

        app.setPrivateFlag(true);
        ical = serialize(app);

        assertProperty(ical, "CLASS", "PRIVATE");
    }

    public void testAppTransparency() throws IOException {
        // RESERVED

        final AppointmentObject app = getDefault();
        app.setShownAs(AppointmentObject.RESERVED);


        ICalFile ical = serialize(app);

        assertProperty(ical, "TRANSP", "OPAQUE");

        // FREE

        app.setShownAs(AppointmentObject.FREE);


        ical = serialize(app);

        assertProperty(ical, "TRANSP", "TRANSPARENT");


    }

    public void testAppAttendees() throws IOException {
        AppointmentObject app = getDefault();
        setParticipants(app, new String[]{"user1@internal.invalid", "user2@internal.invalid"}, new String[]{"external1@external.invalid", "external2@external.invalid"});

        ICalFile ical = serialize(app);

        assertProperty(ical, "ATTENDEE", "MAILTO:user1@internal.invalid");
        assertProperty(ical, "ATTENDEE", "MAILTO:user2@internal.invalid");
        assertProperty(ical, "ATTENDEE", "MAILTO:external1@external.invalid");
        assertProperty(ical, "ATTENDEE", "MAILTO:external2@external.invalid");

        app = getDefault();

        setUserParticipants(app, 2,4,6);

        ical = serialize(app);

        assertProperty(ical, "ATTENDEE", "MAILTO:user1@test.invalid");
        assertProperty(ical, "ATTENDEE", "MAILTO:user3@test.invalid");
        assertProperty(ical, "ATTENDEE", "MAILTO:user5@test.invalid");
                

    }

    private void setUserParticipants(final AppointmentObject app, final int...ids) {
        final Participant[] allParticipants = new Participant[ids.length];
        final UserParticipant[] users = new UserParticipant[ids.length];


        int i = 0,j = 0;
        for(final int id : ids) {
            final UserParticipant p = new UserParticipant(id);
            allParticipants[i++] = p;
            users[j++] = p;
        }
        app.setParticipants(allParticipants);
        app.setUsers(users);
    }


    private void setParticipants(final CalendarObject calendarObject, final String[] internal, final String[] external) {
        final Participant[] allParticipants = new Participant[internal.length+ external.length];
        final UserParticipant[] users = new UserParticipant[internal.length];


        int i = 0,j = 0;
        for(final String mail : internal) {
            final UserParticipant p = new UserParticipant(-1);
            p.setEmailAddress(mail);
            allParticipants[i++] = p;
            users[j++] = p;
        }

        j = 0;
        for(final String mail : external) {
            final ExternalUserParticipant p = new ExternalUserParticipant(mail);
            p.setEmailAddress(mail);
            allParticipants[i++] = p;

        }

        calendarObject.setParticipants(allParticipants);
        calendarObject.setUsers(users);

    }


    public void testAppResources() throws IOException {
        final AppointmentObject app = getDefault();
        setResources(app, "beamer", "toaster", "deflector");
        final ICalFile ical = serialize(app);

        assertProperty(ical, "RESOURCES", "beamer,toaster,deflector");


    }

    private void setResources(final CalendarObject calendarObject, final String...displayNames) {
        final Participant[] participants = new Participant[displayNames.length];
        int i = 0;
        for(final String displayName : displayNames) {
            final ResourceParticipant p = new ResourceParticipant(-1);
            p.setDisplayName(displayName);
            participants[i++] = p;
        }
        calendarObject.setParticipants(participants);

    }

    public void testAppDeleteExceptions() throws IOException {
        final AppointmentObject app = getDefault();
        app.setRecurrenceType(AppointmentObject.DAILY);
        app.setInterval(3);
        app.setRecurrenceCount(5);
        app.setDeleteExceptions(new Date[]{D("25/02/2009 10:00"), D("28/02/2009 12:00")});

        final ICalFile ical = serialize(app);

        assertProperty(ical, "EXDATE", "20090225T100000Z,20090228T120000Z");
    }

    // Omitting: DURATION. This is all handled with DTStart and DTEnd in emitting


    // --------------------------------- Tasks ---------------------------------

    /**
     * Tests task emitter for title and note.
     */
    public void testTaskSimpleFields() throws IOException {
        final Task task = new Task();
        task.setTitle("The Title");
        task.setNote("The Note");
        task.setCategories("cat1, cat2, cat3");
        task.setDateCompleted(D("24/02/2009 10:00"));
        task.setPercentComplete(23);

        final ICalFile ical = serialize(task);
        assertProperty(ical, "SUMMARY", "The Title");
        assertProperty(ical, "DESCRIPTION", "The Note");
        assertProperty(ical, "CATEGORIES","cat1, cat2, cat3");
        assertProperty(ical, "COMPLETED", "20090224T100000Z");
        assertProperty(ical, "PERCENT-COMPLETE", "23");
    }

    public void testTaskCategoriesMayBeNullOrUnset() throws Exception {
        final Task task = new Task();
        ICalFile ical = serialize(task);

        assertNoProperty(ical, "CATEGORIES");

        task.setCategories(null);
        ical = serialize(task);

        assertNoProperty(ical, "CATEGORIES");
    }

    public void testTaskCreated() throws IOException {
        final Task task = new Task();
        task.setCreationDate(D("24/02/1981 10:00"));

        final ICalFile ical = serialize(task);

        assertProperty(ical, "CREATED", "19810224T100000Z");
    }

    public void testTaskLastModified() throws IOException {
        final Task task = new Task();
        task.setLastModified(D("24/02/1981 10:00"));

        final ICalFile ical = serialize(task);

        assertProperty(ical, "LAST-MODIFIED", "19810224T100000Z");
    }


    public void testTaskDateFields() throws IOException {
        final Task task = new Task();
        final Date start = D("13/07/1976 15:00");
        final Date end = D("13/07/1976 17:00");
        task.setStartDate(start);
        task.setEndDate(end);
        final ICalFile ical = serialize(task);
        assertStandardAppFields(ical, start, end);
    }

    // SetUp

    @Override
	public void setUp() {
        users = new MockUserLookup();
        emitter = new ICal4JEmitter();
        oldUserResolver = com.openexchange.data.conversion.ical.ical4j.internal.calendar.Participants.userResolver;
        com.openexchange.data.conversion.ical.ical4j.internal.calendar.Participants.userResolver = new UserResolver(){
            public List<User> findUsers(final List<String> mails, final Context ctx) {
                final List<User> found = new LinkedList<User>();
                for(final String mail : mails) {
                    final User user = ICalEmitterTest.this.users.getUserByMail(mail);
                    if(user != null) {
                        found.add( user );
                    }
                }

                return found;
            }

            public User loadUser(final int userId, final Context ctx) throws LdapException {
                return ICalEmitterTest.this.users.getUser(userId);
            }
        };
    }

    @Override
	public void tearDown() {
        com.openexchange.data.conversion.ical.ical4j.internal.calendar.Participants.userResolver = oldUserResolver;
    }

    // Helper Class


    private ICalFile serialize(final AppointmentObject app) throws IOException {
        final String icalText = emitter.writeAppointments(Arrays.asList(app), null, new ArrayList<ConversionError>(), new ArrayList<ConversionWarning>());
        return new ICalFile(new StringReader(icalText));
    }

    /**
     * Serializes a task.
     * @param task task to serialize.
     * @return an iCal file.
     * @throws IOException if serialization fails.
     */
    private ICalFile serialize(final Task task) throws IOException {
        return new ICalFile(new StringReader(
            emitter.writeTasks(
                Arrays.asList(task),
                new ArrayList<ConversionError>(),
                new ArrayList<ConversionWarning>(), null)));
    }
}
