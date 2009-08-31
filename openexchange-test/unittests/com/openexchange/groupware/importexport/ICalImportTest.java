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

package com.openexchange.groupware.importexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
import java.util.Date;
import junit.framework.JUnit4TestAdapter;
import org.junit.Test;
import com.openexchange.api.OXObjectNotFoundException;
import com.openexchange.api2.AppointmentSQLInterface;
import com.openexchange.api2.OXException;
import com.openexchange.api2.TasksSQLInterface;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.groupware.calendar.CalendarField;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.importexport.exceptions.ImportExportException;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.groupware.tasks.TasksSQLInterfaceImpl;
import com.openexchange.calendar.CalendarSql;
import com.openexchange.data.conversion.ical.ConversionWarning;

public class ICalImportTest extends AbstractICalImportTest {

    // workaround for JUnit 3 runner
    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ICalImportTest.class);
    }

    /**
     * Initially this tests if confidential is not imported at all as wanted in bug 7472. But bug 14337 state to import it as private.
     */
    @Test
    public void test7472_confidential() throws UnsupportedEncodingException, LdapException, OXException {
        folderId = createTestFolder(FolderObject.CALENDAR, sessObj, ctx, "icalAppointmentTestFolder");
        final String ical = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Apple Computer\\, Inc//iCal 2.0//EN\nBEGIN:VEVENT\nCLASS:CONFIDENTIAL\nDTSTART:20070514T150000Z\nDTEND:20070514T163000Z\nLOCATION:Olpe\nSUMMARY:Simple iCal Appointment\nDESCRIPTION:Notes here...\nEND:VEVENT\nEND:VCALENDAR\n";

        assertTrue("Can import?", imp.canImport(sessObj, format, _folders(), null));

        final List<ImportResult> results = imp.importData(
            sessObj,
            format,
            new ByteArrayInputStream(ical.getBytes("UTF-8")),
            _folders(),
            null);
        for (final ImportResult res : results) {
            assertTrue("Shouldn't have error", res.isCorrect());
        }

    }

    @Test
    public void test7472_private() throws UnsupportedEncodingException, LdapException, OXException {
        folderId = createTestFolder(FolderObject.CALENDAR, sessObj, ctx, "icalAppointmentTestFolder");
        final String ical = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Apple Computer\\, Inc//iCal 2.0//EN\nBEGIN:VEVENT\nCLASS:PRIVATE\nDTSTART:20070514T150000Z\nDTEND:20070514T163000Z\nLOCATION:Olpe\nSUMMARY:Simple iCal Appointment\nDESCRIPTION:Notes here...\nEND:VEVENT\nEND:VCALENDAR\n";

        assertTrue("Can import?", imp.canImport(sessObj, format, _folders(), null));

        final List<ImportResult> results = imp.importData(
            sessObj,
            format,
            new ByteArrayInputStream(ical.getBytes("UTF-8")),
            _folders(),
            null);
        for (final ImportResult res : results) {
            assertTrue("Shouldn't have error", res.isCorrect());
        }
    }

    /*
     * Unexpected exception 25! Was related to the ATTENDEE property and it not differing between external and internal users
     */
    @Test
    public void test6825_unexpectedException() throws SQLException, OXObjectNotFoundException, NumberFormatException, OXException, UnsupportedEncodingException, LdapException {
        // setup
        final String testMailAddress = "stephan.martin@open-xchange.com";
        final String ical = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:OPEN-XCHANGE\nBEGIN:VEVENT\nCLASS:PUBLIC\nCREATED:20060519T120300Z\nDTSTART:20060519T110000Z\nDTSTAMP:20070423T063205Z\nSUMMARY:External 1&1 Review call\nDTEND:20060519T120000Z\nATTENDEE:mailto:" + testMailAddress + "\nEND:VEVENT\nEND:VCALENDAR";

        // import and basic tests
        final int objectId = Integer.parseInt(performOneEntryCheck(ical, Format.ICAL, FolderObject.CALENDAR, "6825_unexpected", ctx, false).getObjectId());

        // checking participants
        final AppointmentSQLInterface appointmentSql = new CalendarSql(sessObj);
        final Appointment appointmentObj = appointmentSql.getObjectById(objectId, folderId);
        assertTrue("Has participants", appointmentObj.containsParticipants());
        final Participant[] participants = appointmentObj.getParticipants();
        assertEquals("Two participants", Integer.valueOf(2), Integer.valueOf(participants.length)); // ugly, but necessary to bridge
                                                                                                    // JUNIT3/4
        assertTrue(
            "One user is " + testMailAddress + " (external user)",
            testMailAddress.equals(participants[0].getEmailAddress()) || testMailAddress.equals(participants[1].getEmailAddress()));
        assertTrue(
            "One user is the user doing the import",
            participants[0].getIdentifier() == userId || participants[1].getIdentifier() == userId);
    }

    @Test
    public void test6825_tooMuchInformation() throws OXObjectNotFoundException, NumberFormatException, OXException, UnsupportedEncodingException, LdapException {
        // setup: building an ICAL file with a summary longer than 255 characters.
        final String testMailAddress = "stephan.martin@open-xchange.com";
        final String stringTooLong = "zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... ";
        final String ical = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:OPEN-XCHANGE\nBEGIN:VEVENT\nCLASS:PUBLIC\nCREATED:20060519T120300Z\nDTSTART:20060519T110000Z\nDTSTAMP:20070423T063205Z\nSUMMARY:" + stringTooLong + "\nDTEND:20060519T120000Z\nATTENDEE:mailto:" + testMailAddress + "\nEND:VEVENT\nEND:VCALENDAR";

        final AbstractOXException e = performOneEntryCheck(ical, Format.ICAL, FolderObject.CALENDAR, "6825_tmi", ctx, true).getException();

        assertEquals("Should be truncation error", Category.TRUNCATED, e.getCategory());
        e.printStackTrace();
        assertEquals(
            "SUMMARY was too long",
            Integer.valueOf(CalendarField.TITLE.getAppointmentObjectID()),
            Integer.valueOf(((AbstractOXException.Truncated) e.getProblematics()[0]).getId()));
    }

    /*
     * Description gets lost when importing task
     */
    @Test
    public void test7718() throws UnsupportedEncodingException, NumberFormatException, OXException, LdapException {
        final String description = "das ist ein ical test";
        final String summary = "summariamuttergottes";
        final String ical = "BEGIN:VCALENDAR\nPRODID:-//K Desktop Environment//NONSGML libkcal 3.2//EN\nVERSION:2.0\nBEGIN:VTODO\nDTSTAMP:20070531T093649Z\nORGANIZER;CN=Stephan Martin:MAILTO:stephan.martin@open-xchange.com\nCREATED:20070531T093612Z\nUID:libkcal-1172232934.1028\nSEQUENCE:0\nLAST-MODIFIED:20070531T093612Z\nDESCRIPTION:" + description + "\nSUMMARY:" + summary + "\nLOCATION:daheim\nCLASS:PUBLIC\nPRIORITY:5\nDUE;VALUE=DATE:20070731\nPERCENT-COMPLETE:30\nEND:VTODO\nEND:VCALENDAR";

        final ImportResult res = performOneEntryCheck(ical, Format.ICAL, FolderObject.TASK, "7718", ctx, false);

        final TasksSQLInterface tasks = new TasksSQLInterfaceImpl(sessObj);
        final Task task = tasks.getTaskById(Integer.parseInt(res.getObjectId()), Integer.parseInt(res.getFolder()));
        assertEquals("Summary", summary, task.getTitle());
        assertEquals("Description:", description, task.getNote());
    }

    /*
     * Problem with DAILY recurrences
     */
    @Test
    public void test7703() throws SQLException, UnsupportedEncodingException, NumberFormatException, OXException, LdapException {
        final int interval = 3;
        final String ical = generateRecurringICAL(interval, "DAILY");

        final ImportResult res = performOneEntryCheck(ical, Format.ICAL, FolderObject.CALENDAR, "7703", ctx, false);

        final AppointmentSQLInterface appointments = new CalendarSql(sessObj);
        final Appointment app = appointments.getObjectById(Integer.valueOf(res.getObjectId()).intValue(), Integer.valueOf(
            res.getFolder()).intValue());
        assertEquals("Comparing interval: ", Integer.valueOf(interval), Integer.valueOf(app.getInterval())); // ugly, but necessary to
                                                                                                             // bridge JUnit 3/4
    }

    @Test
    public void test12177() throws LdapException, SQLException, OXException, ImportExportException, UnsupportedEncodingException {
        StringBuilder icalText = new StringBuilder(1500);
        icalText.append("BEGIN:VCALENDAR\n");
        icalText.append("VERSION:2.0").append('\n');
        icalText.append("PRODID:OPEN-XCHANGE").append('\n');

        icalText.append("BEGIN:VEVENT").append('\n');
        icalText.append("CLASS:SUPERCALIFRAGILISTICEXPLIALIDOCIOUS").append('\n');
        icalText.append("DTSTART:20070101T080000Z").append('\n');
        icalText.append("DTEND:20070101T100000Z").append('\n');
        icalText.append("SUMMARY: appointmentWithWarnings ICalImportTest#testWarnings " + System.currentTimeMillis()).append('\n');
        icalText.append("TRANSP:OPAQUE").append('\n');
        icalText.append("END:VEVENT").append('\n');

        icalText.append("END:VCALENDAR");

        final ImportResult res = performOneEntryCheck(icalText.toString(), Format.ICAL, FolderObject.CALENDAR, "12177", ctx, true);

        try {
            assertNotNull(res.getException());

            List<ConversionWarning> warnings = res.getWarnings();
            assertNotNull(warnings);
            assertEquals(Integer.valueOf(1), Integer.valueOf(warnings.size()));

        } finally {
            final AppointmentSQLInterface appointments = new CalendarSql(sessObj);
            CalendarDataObject appointment = new CalendarDataObject();
            appointment.setObjectID(Integer.parseInt(res.getObjectId()));
            appointment.setParentFolderID(Integer.parseInt(res.getFolder()));
            appointment.setContext(sessObj.getContext());
            appointments.deleteAppointmentObject(appointment, appointment.getParentFolderID(), new Date(Long.MAX_VALUE));
        }

    }

    // /*
    // * Unexpected exception 25!
    // * Was related to the ATTENDEE property and it not differing between external and internal users
    // */
    // @Test public void test6825_moreComplexAttendee() throws DBPoolingException, SQLException, OXObjectNotFoundException,
    // NumberFormatException, OXException, UnsupportedEncodingException{
    // //setup
    // folderId = createTestFolder(FolderObject.CALENDAR, sessObj, "ical6825Folder");
    // final String testMailAddress = "stephan.martin@open-xchange.com";
    // final String ical =
    // "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:OPEN-XCHANGE\nBEGIN:VEVENT\nCLASS:PUBLIC\nCREATED:20060519T120300Z\nDTSTART:20060519T110000Z\nDTSTAMP:20070423T063205Z\nSUMMARY:External 1&1 Review call\nDTEND:20060519T120000Z\nATTENDEE;MEMBER=\"MAILTO:DEV-GROUP@host2.com\":MAILTO:joecool@host2.com:mailto:"+
    // testMailAddress + "\nEND:VEVENT\nEND:VCALENDAR";
    // //import and basic tests
    // List<ImportResult> results = imp.importData(sessObj, format, new ByteArrayInputStream(ical.getBytes("UTF-8")), folders, null);
    // assertEquals("One import?" , 1 , results.size());
    // ImportResult res = results.get(0);
    // assertEquals("Shouldn't have error" , null , res.getException() );
    // //checking participants
    // final AppointmentSQLInterface appointmentSql = new CalendarSql(sessObj);
    // final AppointmentObject appointmentObj = appointmentSql.getObjectById(Integer.parseInt( res.getObjectId() ), folderId);
    // assertTrue("Has participants" , appointmentObj.containsParticipants());
    // Participant[] participants = appointmentObj.getParticipants();
    // assertEquals("Two participants" , 2 , participants.length);
    // assertTrue("One user is " + testMailAddress + " (external user)", testMailAddress.equals(participants[0].getEmailAddress()) ||
    // testMailAddress.equals(participants[1].getEmailAddress()) );
    // assertTrue("One user is the user doing the import", participants[0].getIdentifier() == userId || participants[1].getIdentifier() ==
    // userId );
    //		
    // }
    /*
     * Imported appointment loses reminder
     */
    @Test
    public void test7473() throws SQLException, UnsupportedEncodingException, OXObjectNotFoundException, OXException, LdapException {
        final int alarm = 180;
        final String ical = "BEGIN:VCALENDAR\n" + "VERSION:2.0\n" + "PRODID:-//Apple Computer\\, Inc//iCal 2.0//EN\n" + "BEGIN:VEVENT\n" + "CLASS:PRIVATE\n" + "DTSTART:20070614T150000Z\n" + "DTEND:20070614T163000Z\n" + "LOCATION:Olpe\n" + "SUMMARY:Simple iCal Appointment\n" + "DESCRIPTION:Notes here...\n" + "BEGIN:VALARM\nTRIGGER:-PT" + alarm + "M\n" + "ACTION:DISPLAY\n" + "DESCRIPTION:Reminder\n" + "END:VALARM\nEND:VEVENT\n" + "END:VCALENDAR";

        final ImportResult res = performOneEntryCheck(ical, Format.ICAL, FolderObject.CALENDAR, "7473", ctx, false);

        final AppointmentSQLInterface appointmentSql = new CalendarSql(sessObj);
        final Appointment appointmentObj = appointmentSql.getObjectById(Integer.parseInt(res.getObjectId()), folderId);
        assertTrue("Has alarm", appointmentObj.containsAlarm());
        assertEquals("Alarm is " + alarm + " minutes earlier", Integer.valueOf(alarm), Integer.valueOf(appointmentObj.getAlarm()));
    }

    /*
     * "Every sunday in a month" - this is supposed to work
     */
    @Test
    public void test7735_positive() throws SQLException, UnsupportedEncodingException, OXObjectNotFoundException, NumberFormatException, OXException, LdapException {
        // positive prefix for RRULE
        final String ical = "BEGIN:VCALENDAR\n" + "VERSION:2.0\n" + "PRODID:-//Microsoft Corporation//Outlook 12.0 MIMEDIR//EN\n" + "BEGIN:VEVENT\n" + "DTSTART:20070814T150000Z\n" + "DTEND:20070814T163000Z\n" + "LOCATION:Olpe\nSUMMARY:Komplizierte Intervalle\n" + "DESCRIPTION:Jeden ersten Sonntag im April\n" + "RRULE:FREQ=YEARLY;BYDAY=1SU;BYMONTH=4\n" + "END:VEVENT\n" + "END:VCALENDAR";

        final ImportResult res = performOneEntryCheck(ical, Format.ICAL, FolderObject.CALENDAR, "7735_positive", ctx, false);
        assertFalse(res.hasError());
        final AppointmentSQLInterface appointmentSql = new CalendarSql(sessObj);
        final Appointment appointmentObj = appointmentSql.getObjectById(Integer.parseInt(res.getObjectId()), folderId);
        assertTrue(Appointment.YEARLY == appointmentObj.getRecurrenceType());
        assertTrue(Appointment.SUNDAY == appointmentObj.getDays());
        assertTrue(1 == appointmentObj.getDayInMonth());
        assertTrue(Calendar.APRIL == appointmentObj.getMonth());
    }

    /*
     * "Every last sunday in a month" - this is supposed to fail, because this kind of setup is not supported.
     */
    @Test
    public void test7735_negative() throws SQLException, UnsupportedEncodingException, OXObjectNotFoundException, NumberFormatException, OXException, LdapException {
        // positive prefix for RRULE
        final String ical = "BEGIN:VCALENDAR\n" + "VERSION:2.0\n" + "PRODID:-//Microsoft Corporation//Outlook 12.0 MIMEDIR//EN\n" + "BEGIN:VEVENT\n" + "DTSTART:20070814T150000Z\n" + "DTEND:20070814T163000Z\n" + "LOCATION:Olpe\nSUMMARY:Komplizierte Intervalle\n" + "DESCRIPTION:Jeden letzten Sonntag im April\n" + "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=4\n" + "END:VEVENT\n" + "END:VCALENDAR";
        final ImportResult res = performOneEntryCheck(ical, Format.ICAL, FolderObject.CALENDAR, "7735_negative", ctx, false);
        assertFalse(res.hasError());
        final AppointmentSQLInterface appointmentSql = new CalendarSql(sessObj);
        final Appointment appointmentObj = appointmentSql.getObjectById(Integer.parseInt(res.getObjectId()), folderId);
        assertTrue(Appointment.YEARLY == appointmentObj.getRecurrenceType());
        assertTrue(Appointment.SUNDAY == appointmentObj.getDays());
        assertTrue(5 == appointmentObj.getDayInMonth());
        assertTrue(Calendar.APRIL == appointmentObj.getMonth());
    }

    /*
     * "Every second last sunday in a month" - this is supposed to fail, because this kind of setup is not supported.
     */
    @Test
    public void test7735_negative_above_1() throws UnsupportedEncodingException, OXObjectNotFoundException, NumberFormatException, OXException, LdapException {
        // positive prefix for RRULE
        final String ical = "BEGIN:VCALENDAR\n" + "VERSION:2.0\n" + "PRODID:-//Microsoft Corporation//Outlook 12.0 MIMEDIR//EN\n" + "BEGIN:VEVENT\n" + "DTSTART:20070814T150000Z\n" + "DTEND:20070814T163000Z\n" + "LOCATION:Olpe\nSUMMARY:Komplizierte Intervalle\n" + "DESCRIPTION:Jeden letzten Sonntag im April\n" + "RRULE:FREQ=YEARLY;BYDAY=-2SU;BYMONTH=4\n" + "END:VEVENT\n" + "END:VCALENDAR";
        final ImportResult res = performOneEntryCheck(ical, Format.ICAL, FolderObject.CALENDAR, "7735_negative", ctx, true);
        assertTrue(res.hasError());
    }

    @Test
    public void test7470() throws UnsupportedEncodingException, SQLException, OXObjectNotFoundException, NumberFormatException, OXException, LdapException {
        final String ical = "BEGIN:VCALENDAR\n" + "PRODID:-//Microsoft Corporation//Outlook 12.0 MIMEDIR//EN\n" + "VERSION:2.0\n" + "METHOD:REQUEST\n" + "X-MS-OLK-FORCEINSPECTOROPEN:TRUE\n" + "BEGIN:VEVENT\n" + "ATTENDEE;CN=\"Camil Bartkowiak (cbartkowiak@oxhemail.open-xchange.com)\";RSVP=TRUE:mailto:cbartkowiak@oxhemail.open-xchange.com\n" + "CLASS:PUBLIC\n" + "CREATED:20070521T150327Z\n" + "DESCRIPTION:Hallo Hallo\\n\\n\n" + "DTEND:20070523T090000Z\n" + "DTSTAMP:20070521T150327Z\n" + "DTSTART:20070523T083000Z\n" + "LAST-MODIFIED:20070521T150327Z\n" + "LOCATION:Location here\n" + "ORGANIZER;CN=Tobias:mailto:tfriedrich@oxhemail.open-xchange.com\n" + "PRIORITY:5\n" + "SEQUENCE:0\n" + "SUMMARY;LANGUAGE=de:Simple Appointment with participant\n" + "TRANSP:OPAQUE\n" + "UID:040000008200E00074C5B7101A82E0080000000060565ABBC99BC701000000000000000010000000E4B2BA931D32B84DAFB227C9E0CA348C\n" + "X-ALT-DESC;FMTTYPE=text/html:<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2//E\n	N\">\\n<HTML>\\n<HEAD>\\n<META NAME=\"Generator\" CONTENT=\"MS Exchange Server ve\\n	rsion 08.00.0681.000\">\\n<TITLE></TITLE>\\n</HEAD>\\n<BODY>\\n<!-- Converted f\n	rom text/rtf format -->\\n\\n<P DIR=LTR><SPAN LANG=\"de\"><FONT FACE=\"Calibri\"\n	>Hallo Hallo</FONT></SPAN></P>\\n\\n<P DIR=LTR><SPAN LANG=\"de\"></SPAN></P>\\n\\n	\\n</BODY>\\n</HTML>\n" + "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\n" + "X-MICROSOFT-CDO-IMPORTANCE:1\n" + "X-MICROSOFT-DISALLOW-COUNTER:FALSE\n" + "X-MS-OLK-ALLOWEXTERNCHECK:TRUE\n" + "X-MS-OLK-AUTOFILLLOCATION:FALSE\n" + "X-MS-OLK-CONFTYPE:0\n" + "BEGIN:VALARM\n" + "TRIGGER:PT0M\n" + "ACTION:DISPLAY\n" + "DESCRIPTION:Reminder\n" + "END:VALARM\n" + "END:VEVENT\n" + "END:VCALENDAR\n";

        final ImportResult res = performOneEntryCheck(ical, Format.ICAL, FolderObject.CALENDAR, "7470", ctx, false);
        final AppointmentSQLInterface appointments = new CalendarSql(sessObj);
        final Appointment app = appointments.getObjectById(Integer.valueOf(res.getObjectId()).intValue(), Integer.valueOf(
            res.getFolder()).intValue());
        final Participant[] participants = app.getParticipants();
        assertEquals("Two participants?", Integer.valueOf(2), Integer.valueOf(participants.length));
        boolean found = false;
        for (final Participant p : participants) {
            if ("cbartkowiak@oxhemail.open-xchange.com".equals(p.getEmailAddress())) {
                found = true;
            }
        }
        assertTrue("Found attendee?", found);
    }
}
