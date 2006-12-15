package com.openexchange.ajax.appointment;

import com.openexchange.ajax.Appointment;
import com.openexchange.ajax.AppointmentTest;
import com.openexchange.groupware.container.AppointmentObject;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AllTest extends AppointmentTest {

	private static final Log LOG = LogFactory.getLog(AllTest.class);
	
	public AllTest(String name) {
		super(name);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	public void testShowAppointmentsBetween() throws Exception {
		Date start = new Date(System.currentTimeMillis()-(dayInMillis*7));
		Date end = new Date(System.currentTimeMillis()+(dayInMillis*7));
		
		final int cols[] = new int[]{ AppointmentObject.OBJECT_ID };
		
		AppointmentObject[] appointmentArray = listAppointment(getWebConversation(), appointmentFolderId, cols, start, end, timeZone, false, PROTOCOL + getHostName(), getSessionId());
	}
	
	public void testShowAllAppointmentWhereIAmParticipant() throws Exception {
		Date start = new Date(System.currentTimeMillis()-(dayInMillis*7));
		Date end = new Date(System.currentTimeMillis()+(dayInMillis*7));
		
		final int cols[] = new int[]{ AppointmentObject.OBJECT_ID };
		
		AppointmentObject[] appointmentArray = listAppointment(getWebConversation(), appointmentFolderId, cols, start, end, timeZone, true, PROTOCOL + getHostName(), getSessionId());
	}
	
	public void _notestShowFullTimeAppointments() throws Exception {
		final int cols[] = new int[]{ AppointmentObject.OBJECT_ID };
		
		AppointmentObject appointmentObj = createAppointmentObject("testShowFullTimeAppointments");
		appointmentObj.setFullTime(true);
		appointmentObj.setIgnoreConflicts(true);
		int objectId = insertAppointment(getWebConversation(), appointmentObj, timeZone, getHostName(), getSessionId());
		
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		Date start = calendar.getTime();
		Date end = new Date(start.getTime()+dayInMillis);
		
		AppointmentObject[] appointmentArray = listAppointment(getWebConversation(), appointmentFolderId, cols, start, end, timeZone, false, getHostName(), getSessionId());
		
		boolean found = false;
		
		for (int a = 0; a < appointmentArray.length; a++) {
			if (appointmentArray[a].getObjectID() == objectId) {
				found = true;
			}
		}
		
		// one day less
		assertTrue("appointment not found in day view", found);
		
		start = new Date(calendar.getTimeInMillis()-dayInMillis);
		end = new Date(start.getTime()+dayInMillis);
		
		appointmentArray = listAppointment(getWebConversation(), appointmentFolderId, cols, start, end, timeZone, false, getHostName(), getSessionId());
		
		found = false;
		
		for (int a = 0; a < appointmentArray.length; a++) {
			if (appointmentArray[a].getObjectID() == objectId) {
				found = true;
			}
		}
		
		assertFalse("appointment found one day before start date in day view", found);
		
		// one day more
		start = new Date(calendar.getTimeInMillis()+dayInMillis);
		end = new Date(start.getTime()+dayInMillis);
		
		appointmentArray = listAppointment(getWebConversation(), appointmentFolderId, cols, start, end, timeZone, false, getHostName(), getSessionId());
		
		found = false;
		
		for (int a = 0; a < appointmentArray.length; a++) {
			if (appointmentArray[a].getObjectID() == objectId) {
				found = true;
			}
		}
		
		assertFalse("appointment found one day after start date in day view", found);
				

		deleteAppointment(getWebConversation(), objectId, appointmentFolderId, getHostName(), getSessionId());
	}
}