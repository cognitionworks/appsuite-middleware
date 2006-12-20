package com.openexchange.ajax.reminder;

import com.openexchange.ajax.AppointmentTest;
import com.openexchange.ajax.ConfigMenuTest;
import com.openexchange.ajax.FolderTest;
import com.openexchange.groupware.container.AppointmentObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.reminder.ReminderObject;
import com.openexchange.test.TestException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


public class DeleteTest extends ReminderTest {

	public DeleteTest(String name) {
		super(name);
	}
	
	public void testDummy() {
		
	}
	
	public void testDelete() throws Exception {
		final int userId = ConfigMenuTest.getUserId(getWebConversation(), getHostName(), getSessionId());
		final TimeZone timeZone = ConfigMenuTest.getTimeZone(getWebConversation(), getHostName(), getSessionId());
		
		Calendar c = Calendar.getInstance();
		c.setTimeZone(timeZone);
		c.set(Calendar.HOUR_OF_DAY, 8);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		
		long startTime = c.getTimeInMillis();
		startTime += timeZone.getOffset(startTime);
		long endTime = startTime + 3600000;
		
		
		final FolderObject folderObj = FolderTest.getStandardCalendarFolder(getWebConversation(), getHostName(), getSessionId());
		final int folderId = folderObj.getObjectID();
		
		AppointmentObject appointmentObj = new AppointmentObject();
		appointmentObj.setTitle("testDelete");
		appointmentObj.setStartDate(new Date(startTime));
		appointmentObj.setEndDate(new Date(endTime));
		appointmentObj.setShownAs(AppointmentObject.ABSENT);
		appointmentObj.setAlarm(45);
		appointmentObj.setParentFolderID(folderId);
		appointmentObj.setIgnoreConflicts(true);
		
		final int targetId = AppointmentTest.insertAppointment(getWebConversation(), appointmentObj, timeZone, getHostName(), getSessionId());
		final String target = String.valueOf(targetId);
		
		ReminderObject[] reminderObj = listReminder(getWebConversation(), new Date(endTime), timeZone, getHostName(), getSessionId());

		int pos = -1;
		for (int a = 0; a < reminderObj.length; a++) {
			if (target.equals(reminderObj[a].getTargetId())) {
				pos = a;
			}
		}
		
		deleteReminder(getWebConversation(), reminderObj[pos].getObjectId(), getHostName(), getSessionId());
		AppointmentTest.deleteAppointment(getWebConversation(), targetId, folderId, getHostName(), getSessionId());
	} 
	
	public void testDeleteWithNonExisting() throws Exception {
		final int userId = ConfigMenuTest.getUserId(getWebConversation(), getHostName(), getSessionId());
		final TimeZone timeZone = ConfigMenuTest.getTimeZone(getWebConversation(), getHostName(), getSessionId());
		
		Calendar c = Calendar.getInstance();
		c.setTimeZone(timeZone);
		c.set(Calendar.HOUR_OF_DAY, 8);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		
		long startTime = c.getTimeInMillis();
		startTime += timeZone.getOffset(startTime);
		long endTime = startTime + 3600000;
		
		
		final FolderObject folderObj = FolderTest.getStandardCalendarFolder(getWebConversation(), getHostName(), getSessionId());
		final int folderId = folderObj.getObjectID();
		
		AppointmentObject appointmentObj = new AppointmentObject();
		appointmentObj.setTitle("testDeleteWithNonExisting");
		appointmentObj.setStartDate(new Date(startTime));
		appointmentObj.setEndDate(new Date(endTime));
		appointmentObj.setShownAs(AppointmentObject.ABSENT);
		appointmentObj.setAlarm(45);
		appointmentObj.setParentFolderID(folderId);
		appointmentObj.setIgnoreConflicts(true);
		
		final int targetId = AppointmentTest.insertAppointment(getWebConversation(), appointmentObj, timeZone, getHostName(), getSessionId());
		final String target = String.valueOf(targetId);
		
		ReminderObject[] reminderObj = listReminder(getWebConversation(), new Date(endTime), timeZone, getHostName(), getSessionId());

		int pos = -1;
		for (int a = 0; a < reminderObj.length; a++) {
			if (target.equals(reminderObj[a].getTargetId())) {
				pos = a;
			}
		}
		try {
			deleteReminder(getWebConversation(), reminderObj[pos].getObjectId()+1000, getHostName(), getSessionId());
			fail("object not found exception excected!");
		} catch (TestException ex) {
			assertTrue(true);
		}
		
		AppointmentTest.deleteAppointment(getWebConversation(), targetId, folderId, getHostName(), getSessionId());
	}
}

