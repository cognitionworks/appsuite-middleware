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
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;

import junit.framework.JUnit4TestAdapter;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.openexchange.api.OXObjectNotFoundException;
import com.openexchange.api2.AppointmentSQLInterface;
import com.openexchange.api2.OXException;
import com.openexchange.api2.TasksSQLInterface;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.Init;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.groupware.calendar.CalendarSql;
import com.openexchange.groupware.contact.ContactConfig;
import com.openexchange.groupware.container.AppointmentObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.contexts.ContextImpl;
import com.openexchange.groupware.contexts.ContextStorage;
import com.openexchange.groupware.importexport.exceptions.ImportExportException;
import com.openexchange.groupware.importexport.importers.ICalImporter;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.groupware.tasks.TasksSQLInterfaceImpl;
import com.openexchange.server.DBPoolingException;
import com.openexchange.sessiond.SessionObjectWrapper;


public class ICalImportTest extends AbstractContactTest {
	public final Format format = Format.ICAL;
	
	//workaround for JUnit 3 runner
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(ICalImportTest.class);
	}
	
	
	
	@BeforeClass
	public static void initialize() throws SQLException, AbstractOXException {
		Init.initDB();
		ContactConfig.init();
		ContextStorage.init();
		final UserStorage uStorage = UserStorage.getInstance(new ContextImpl(1));
	    userId = uStorage.getUserId( Init.getAJAXProperty("login") );
	    sessObj = SessionObjectWrapper.createSessionObject(userId, 1, "csv-tests");
		userId = sessObj.getUserObject().getId();
		imp = new ICalImporter();
	}
	
	@After
	public void cleanUpAfterTest() throws OXException{
		deleteTestFolder(folderId);
	}



	@Test public void test7386() throws UnsupportedEncodingException, ImportExportException, DBPoolingException, SQLException{
		folderId = createTestFolder(FolderObject.TASK, sessObj, "icalTaskTestFolder");
		String ical =  "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//ellie//ellie//EN\nCALSCALE:GREGORIAN\nBEGIN:VTODO\nDTSTART;TZID=US/Eastern:20040528T000000\nSUMMARY:grade quizzes and m1\nUID:2\nSEQUENCE:0\nDTSTAMP:20040606T230400\nPRIORITY:2\nDUE;VALUE=DATE:20040528T000000\nEND:VTODO\nBEGIN:VTODO\nDTSTART;TZID=US/Eastern:20040525T000000\nSUMMARY:get timesheet signed\nUID:1\nSEQUENCE:0\nDTSTAMP:20040606T230400\nPRIORITY:1\nDUE;VALUE=DATE:20040525T000000\nEND:VTODO\nEND:VCALENDAR";

		assertTrue("Can import?" ,  imp.canImport(sessObj, format, _folders(), null));

		List<ImportResult> results = imp.importData(sessObj, format, new ByteArrayInputStream(ical.getBytes("UTF-8")), _folders(), null);
		for(ImportResult res : results){
			assertTrue("Should have error" , res.hasError());
			assertEquals("I_E-0505",res.getException().getErrorCode());
		}
	}
	

	@Test public void test7472_confidential() throws UnsupportedEncodingException, ImportExportException, DBPoolingException, SQLException{
		folderId = createTestFolder(FolderObject.CALENDAR, sessObj, "icalAppointmentTestFolder");
		String ical = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Apple Computer\\, Inc//iCal 2.0//EN\nBEGIN:VEVENT\nCLASS:CONFIDENTIAL\nDTSTART:20070514T150000Z\nDTEND:20070514T163000Z\nLOCATION:Olpe\nSUMMARY:Simple iCal Appointment\nDESCRIPTION:Notes here...\nEND:VEVENT\nEND:VCALENDAR\n";

		assertTrue("Can import?" ,  imp.canImport(sessObj, format, _folders(), null));

		List<ImportResult> results = imp.importData(sessObj, format, new ByteArrayInputStream(ical.getBytes("UTF-8")), _folders(), null);
		for(ImportResult res : results){
			assertTrue("Should have error" , res.hasError());
			assertEquals("Should be privacy error" , "I_E-0506",res.getException().getErrorCode());
		}

	}
	
	@Test public void test7472_private() throws UnsupportedEncodingException, ImportExportException, DBPoolingException, SQLException{
		folderId = createTestFolder(FolderObject.CALENDAR, sessObj, "icalAppointmentTestFolder");
		String ical = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Apple Computer\\, Inc//iCal 2.0//EN\nBEGIN:VEVENT\nCLASS:PRIVATE\nDTSTART:20070514T150000Z\nDTEND:20070514T163000Z\nLOCATION:Olpe\nSUMMARY:Simple iCal Appointment\nDESCRIPTION:Notes here...\nEND:VEVENT\nEND:VCALENDAR\n";

		assertTrue("Can import?" ,  imp.canImport(sessObj, format, _folders(), null));

		List<ImportResult> results = imp.importData(sessObj, format, new ByteArrayInputStream(ical.getBytes("UTF-8")), _folders(), null);
		for(ImportResult res : results){
			assertTrue("Shouldn't have error" , res.isCorrect());
		}
	}

	/*
	 * Unexpected exception 25!
	 * Was related to the ATTENDEE property and it not differing between external and internal users
	 */
	@Test public void test6825_unexpectedException() throws DBPoolingException, SQLException, OXObjectNotFoundException, NumberFormatException, OXException, UnsupportedEncodingException{
		//setup
		folderId = createTestFolder(FolderObject.CALENDAR, sessObj, "ical6825Folder");
		final String testMailAddress = "stephan.martin@open-xchange.com";
		final String ical = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:OPEN-XCHANGE\nBEGIN:VEVENT\nCLASS:PUBLIC\nCREATED:20060519T120300Z\nDTSTART:20060519T110000Z\nDTSTAMP:20070423T063205Z\nSUMMARY:External 1&1 Review call\nDTEND:20060519T120000Z\nATTENDEE:mailto:"+ testMailAddress + "\nEND:VEVENT\nEND:VCALENDAR";
		//import and basic tests
		List<ImportResult> results = imp.importData(sessObj, format, new ByteArrayInputStream(ical.getBytes("UTF-8")), _folders(), null);
		assertEquals("One import?" , 1 , results.size());
		ImportResult res = results.get(0);
		assertEquals("Shouldn't have error" , null , res.getException() );
		//checking participants
		final AppointmentSQLInterface appointmentSql = new CalendarSql(sessObj);
		final AppointmentObject appointmentObj = appointmentSql.getObjectById(Integer.parseInt( res.getObjectId() ), folderId);
		assertTrue("Has participants" , appointmentObj.containsParticipants());
		Participant[] participants = appointmentObj.getParticipants();
		assertEquals("Two participants" , 2 , participants.length);
		assertTrue("One user is " + testMailAddress + " (external user)", testMailAddress.equals(participants[0].getEmailAddress()) || testMailAddress.equals(participants[1].getEmailAddress()) );
		assertTrue("One user is the user doing the import", participants[0].getIdentifier() == userId || participants[1].getIdentifier() == userId );
	}
	
	@Test public void test6825_tooMuchInformation() throws DBPoolingException, SQLException, OXObjectNotFoundException, NumberFormatException, OXException, UnsupportedEncodingException{
		//setup: building an ICAL file with a summary longer than 255 characters.
		folderId = createTestFolder(FolderObject.CALENDAR, sessObj, "ical6825Folder");
		final String testMailAddress = "stephan.martin@open-xchange.com";
		final String stringTooLong = "zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... zwanzig zeichen.... ";
		final String ical = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:OPEN-XCHANGE\nBEGIN:VEVENT\nCLASS:PUBLIC\nCREATED:20060519T120300Z\nDTSTART:20060519T110000Z\nDTSTAMP:20070423T063205Z\nSUMMARY:" + stringTooLong + "\nDTEND:20060519T120000Z\nATTENDEE:mailto:"+ testMailAddress + "\nEND:VEVENT\nEND:VCALENDAR";
		//import and tests
		final List<ImportResult> results = imp.importData(sessObj, format, new ByteArrayInputStream(ical.getBytes("UTF-8")), _folders(), null);
		assertEquals("One import?" , 1 , results.size());
		assertTrue("Should have an error" , results.get(0).hasError() );
		final OXException e = results.get(0).getException();
		assertEquals("Should be truncation error" , Category.TRUNCATED , e.getCategory());
		assertEquals("SUMMARY was too long" , String.valueOf( AppointmentObject.TITLE ) , e.getMessageArgs()[0]);
	}
	
	/*
	 * Description gets lost when importing task
	 */
	@Test public void test7718() throws DBPoolingException, SQLException, UnsupportedEncodingException, NumberFormatException, OXException{
		folderId = createTestFolder(FolderObject.TASK, sessObj, "ical7718Folder");
		final String description = "das ist ein ical test";
		final String summary = "summariamuttergottes";
		final String ical = "BEGIN:VCALENDAR\nPRODID:-//K Desktop Environment//NONSGML libkcal 3.2//EN\nVERSION:2.0\nBEGIN:VTODO\nDTSTAMP:20070531T093649Z\nORGANIZER;CN=Stephan Martin:MAILTO:stephan.martin@open-xchange.com\nCREATED:20070531T093612Z\nUID:libkcal-1172232934.1028\nSEQUENCE:0\nLAST-MODIFIED:20070531T093612Z\nDESCRIPTION:"+description+"\nSUMMARY:"+summary+"\nLOCATION:daheim\nCLASS:PUBLIC\nPRIORITY:5\nDUE;VALUE=DATE:20070731\nPERCENT-COMPLETE:30\nEND:VTODO\nEND:VCALENDAR";
		
		final List<ImportResult> results = imp.importData(sessObj, format, new ByteArrayInputStream(ical.getBytes("UTF-8")), _folders(), null);
		assertEquals("One import?" , 1 , results.size());
		ImportResult res = results.get(0);
		assertEquals("Should have no error" , null, results.get(0).getException() );
		final TasksSQLInterface tasks = new TasksSQLInterfaceImpl(sessObj);
		Task task = tasks.getTaskById(Integer.valueOf( res.getObjectId()), Integer.valueOf(res.getFolder()) );
		assertEquals("Summary" , summary, task.getTitle());
		assertEquals("Description:" , description , task.getNote());
	}
	
	/*
	 * Problem with DAILY recurrences
	 */
	@Test public void test7703() throws DBPoolingException, SQLException, UnsupportedEncodingException, NumberFormatException, OXException{
		folderId = createTestFolder(FolderObject.CALENDAR, sessObj, "ical7703Folder");
		int interval = 3; 
		String ical = generateRecurringICAL(interval, "DAILY");
		
		final List<ImportResult> results = imp.importData(sessObj, format, new ByteArrayInputStream(ical.getBytes("UTF-8")), _folders(), null);
		assertEquals("One import?" , 1 , results.size());
		ImportResult res = results.get(0);
		assertEquals("Should have no error" , null, results.get(0).getException() );
		final AppointmentSQLInterface appointments = new CalendarSql(sessObj);
		CalendarDataObject app = appointments.getObjectById( Integer.valueOf(res.getObjectId()), Integer.valueOf(res.getFolder()) );
		assertEquals("Comparing interval: ", interval , app.getInterval() );
	}

//	/*
//	 * Unexpected exception 25!
//	 * Was related to the ATTENDEE property and it not differing between external and internal users
//	 */
//	@Test public void test6825_moreComplexAttendee() throws DBPoolingException, SQLException, OXObjectNotFoundException, NumberFormatException, OXException, UnsupportedEncodingException{
//		//setup
//		folderId = createTestFolder(FolderObject.CALENDAR, sessObj, "ical6825Folder");
//		final String testMailAddress = "stephan.martin@open-xchange.com";
//		final String ical = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:OPEN-XCHANGE\nBEGIN:VEVENT\nCLASS:PUBLIC\nCREATED:20060519T120300Z\nDTSTART:20060519T110000Z\nDTSTAMP:20070423T063205Z\nSUMMARY:External 1&1 Review call\nDTEND:20060519T120000Z\nATTENDEE;MEMBER=\"MAILTO:DEV-GROUP@host2.com\":MAILTO:joecool@host2.com:mailto:"+ testMailAddress + "\nEND:VEVENT\nEND:VCALENDAR";
//		//import and basic tests
//		List<ImportResult> results = imp.importData(sessObj, format, new ByteArrayInputStream(ical.getBytes("UTF-8")), folders, null);
//		assertEquals("One import?" , 1 , results.size());
//		ImportResult res = results.get(0);
//		assertEquals("Shouldn't have error" , null , res.getException() );
//		//checking participants
//		final AppointmentSQLInterface appointmentSql = new CalendarSql(sessObj);
//		final AppointmentObject appointmentObj = appointmentSql.getObjectById(Integer.parseInt( res.getObjectId() ), folderId);
//		assertTrue("Has participants" , appointmentObj.containsParticipants());
//		Participant[] participants = appointmentObj.getParticipants();
//		assertEquals("Two participants" , 2 , participants.length);
//		assertTrue("One user is " + testMailAddress + " (external user)", testMailAddress.equals(participants[0].getEmailAddress()) || testMailAddress.equals(participants[1].getEmailAddress()) );
//		assertTrue("One user is the user doing the import", participants[0].getIdentifier() == userId || participants[1].getIdentifier() == userId );
//		
//	}
//	/*
//	 * Imported appointment loses reminder
//	 */
//	@Test public void test7473() throws DBPoolingException, SQLException, UnsupportedEncodingException, OXObjectNotFoundException, OXException{
//		folderId = createTestFolder(FolderObject.CALENDAR, sessObj, "ical7473Folder");
//		String ical =  "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Apple Computer\\, Inc//iCal 2.0//EN\nBEGIN:VEVENT\nCLASS:PRIVATE\nDTSTART:20070614T150000Z\nDTEND:20070614T163000Z\nLOCATION:Olpe\nSUMMARY:Simple iCal Appointment\nDESCRIPTION:Notes here...\nBEGIN:VALARM\nTRIGGER:-PT180M\nACTION:DISPLAY\nDESCRIPTION:Reminder\nEND:VALARM\nEND:VEVENT\nEND:VCALENDAR";
//		assertTrue("Can import?" ,  imp.canImport(sessObj, format, folders, null));
//		List<ImportResult> results = imp.importData(sessObj, format, new ByteArrayInputStream(ical.getBytes("UTF-8")), folders, null);
//		assertEquals("One import?" , 1 , results.size());
//		ImportResult res = results.get(0);
//		assertEquals("Shouldn't have error" , null, res.getException());
//		
//		final AppointmentSQLInterface appointmentSql = new CalendarSql(sessObj);
//		final AppointmentObject appointmentObj = appointmentSql.getObjectById(Integer.parseInt( res.getObjectId() ), folderId);
//		assertTrue("Has alarm" , appointmentObj.containsAlarm());
//	}
//	
//	@Test public void test7474() throws DBPoolingException, SQLException{
//		folderId = createTestFolder(FolderObject.CALENDAR, sessObj, "ical6825Folder");
//		fail("TODO");
//	}
	
	public String generateRecurringICAL(final int interval, final String frequency){
		return 
		"BEGIN:VCALENDAR\n" +
		"VERSION:2.0\n" +
		"PRODID:-//The Horde Project//Horde_iCalendar Library//EN\n" +
		"METHOD:PUBLISH\n" +
		"BEGIN:VEVENT\n" +
		"DTSTART;VALUE=DATE:20070616\n" +
		"DTEND;VALUE=DATE:20070617\n" +
		"DTSTAMP:20070530T200206Z\n" +
		"UID:20070530220126.23mszu01hoo0@www.klein-intern.de\n" +
		"SUMMARY:Marc beim Umzug helfen\n" +
		"TRANSP:OPAQUE\nORGANIZER;CN=Marcus Klein:MAILTO:m.klein@sendung-mit-der-maus.com\n" +
		"LOCATION:Olpe\n" +
		"RRULE:FREQ="+frequency+";INTERVAL="+interval+";UNTIL=20070627\n" +
		"END:VEVENT\n" +
		"END:VCALENDAR\n";
	}
}
