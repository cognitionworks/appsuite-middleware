package com.openexchange.webdav.xml.appointment;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openexchange.groupware.container.AppointmentObject;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.webdav.xml.AppointmentTest;

public class Bug6535Test extends AppointmentTest {
	
	private static final Log LOG = LogFactory.getLog(Bug6535Test.class);
	
	public Bug6535Test(final String name) {
		super(name);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	public void testBug6535() throws Exception {
		final TimeZone timeZoneUTC = TimeZone.getTimeZone("UTC");
		
		final Calendar calendar = Calendar.getInstance(timeZoneUTC);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		calendar.add(Calendar.DAY_OF_MONTH, 2);
		
		final Date recurrenceDatePosition = calendar.getTime();
		
		calendar.add(Calendar.DAY_OF_MONTH, 3);
		
		final Date until = calendar.getTime();
		
		final AppointmentObject appointmentObj = new AppointmentObject();
		appointmentObj.setTitle("testBug6535");
		appointmentObj.setStartDate(startTime);
		appointmentObj.setEndDate(endTime);
		appointmentObj.setShownAs(AppointmentObject.ABSENT);
		appointmentObj.setParentFolderID(appointmentFolderId);
		appointmentObj.setRecurrenceType(AppointmentObject.DAILY);
		appointmentObj.setInterval(1);
		appointmentObj.setUntil(until);
		appointmentObj.setIgnoreConflicts(true);
		
		final UserParticipant[] users = new UserParticipant[1];
		users[0] = new UserParticipant(userId);
		users[0].setConfirm(AppointmentObject.ACCEPT);
		
		appointmentObj.setUsers(users);

		final int objectId = insertAppointment(getWebConversation(), appointmentObj, PROTOCOL + getHostName(), getLogin(), getPassword());
		
		final AppointmentObject recurrenceUpdate = new AppointmentObject();
		recurrenceUpdate.setTitle("testBug6535 - exception");
		recurrenceUpdate.setStartDate(new Date(startTime.getTime()+600000));
		recurrenceUpdate.setEndDate(new Date(endTime.getTime()+600000));
		recurrenceUpdate.setRecurrenceDatePosition(recurrenceDatePosition);
		recurrenceUpdate.setShownAs(AppointmentObject.ABSENT);
		recurrenceUpdate.setParentFolderID(appointmentFolderId);
		recurrenceUpdate.setIgnoreConflicts(true);
		recurrenceUpdate.setUsers(users);

		updateAppointment(getWebConversation(), recurrenceUpdate, objectId, appointmentFolderId, getHostName(), getLogin(), getPassword());
		
		appointmentObj.setObjectID(objectId);
		AppointmentObject loadAppointment = loadAppointment(getWebConversation(), objectId, appointmentFolderId, PROTOCOL + getHostName(), getLogin(), getPassword());
		compareObject(appointmentObj, loadAppointment);
		
		final Date modified = loadAppointment.getLastModified();
		
		loadAppointment = loadAppointment(getWebConversation(), objectId, appointmentFolderId, modified, getHostName(), getLogin(), getPassword());
		compareObject(appointmentObj, loadAppointment);
		
		deleteAppointment(getWebConversation(), objectId, appointmentFolderId, PROTOCOL + getHostName(), getLogin(), getPassword());
	}
}