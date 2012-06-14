package com.openexchange.ajax.reminder;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import com.openexchange.ajax.AppointmentTest;
import com.openexchange.ajax.FolderTest;
import com.openexchange.ajax.config.ConfigTools;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.reminder.ReminderObject;

public class Bug6408Test extends ReminderTest {

	public Bug6408Test(final String name) {
		super(name);
	}

	public void testBug6408() throws Exception {
		ConfigTools.getUserId(getWebConversation(), getHostName(), getSessionId());
		final TimeZone timeZone = ConfigTools.getTimeZone(getWebConversation(), getHostName(), getSessionId());

		final Calendar c = Calendar.getInstance();
		c.setTimeZone(timeZone);
		c.add(Calendar.DAY_OF_MONTH, +2);

		c.set(Calendar.HOUR_OF_DAY, 8);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);

		final long startTime = c.getTimeInMillis();
		final long endTime = startTime + 3600000;

		final FolderObject folderObj = FolderTest.getStandardCalendarFolder(getWebConversation(), getHostName(), getSessionId());
		final int folderId = folderObj.getObjectID();

		final int alarmMinutes = 60;

		final Appointment appointmentObj = new Appointment();
		appointmentObj.setTitle("testBug6408");
		appointmentObj.setStartDate(new Date(startTime));
		appointmentObj.setEndDate(new Date(endTime));
		appointmentObj.setShownAs(Appointment.ABSENT);
		appointmentObj.setAlarm(alarmMinutes);
		appointmentObj.setParentFolderID(folderId);
		appointmentObj.setIgnoreConflicts(true);

		final int targetId = AppointmentTest.insertAppointment(getWebConversation(), appointmentObj, timeZone, getHostName(), getSessionId());

		final ReminderObject reminderObj = new ReminderObject();
		reminderObj.setTargetId(targetId);
		reminderObj.setFolder(folderId);
		reminderObj.setDate(new Date(startTime-(alarmMinutes*60*1000)));

		ReminderObject[] reminderArray = listReminder(getWebConversation(), new Date(endTime), timeZone, getHostName(), getSessionId());

		int pos = -1;
		for (int a = 0; a < reminderArray.length; a++) {
			if (reminderArray[a].getTargetId() == targetId) {
				pos = a;
				reminderObj.setObjectId(reminderArray[a].getObjectId());
				compareReminder(reminderObj, reminderArray[a]);
			}
		}

		final int newAlarmMinutes = 60*24*7*4;

		appointmentObj.removeParentFolderID();
		appointmentObj.setAlarm(newAlarmMinutes);

		final long alarmInMillies = newAlarmMinutes*60*1000L;

		reminderObj.setDate(new Date(startTime-alarmInMillies));

		AppointmentTest.updateAppointment(getWebConversation(), appointmentObj, targetId, folderId, timeZone, getHostName(), getSessionId());

		reminderArray = listReminder(getWebConversation(), new Date(endTime), timeZone, getHostName(), getSessionId());

		boolean found = false;

		pos = -1;
		for (int a = 0; a < reminderArray.length; a++) {
			if (reminderArray[a].getTargetId() == targetId) {
				pos = a;
				reminderObj.setObjectId(reminderArray[a].getObjectId());
				compareReminder(reminderObj, reminderArray[a]);
				found = true;
			}
		}

		assertTrue("no reminder find for target id " + targetId + " in response", found);

		deleteReminder(getWebConversation(), reminderArray[pos].getObjectId(), getHostName(), getSessionId());
		AppointmentTest.deleteAppointment(getWebConversation(), targetId, folderId, getHostName(), getSessionId(), false);
	}
}

