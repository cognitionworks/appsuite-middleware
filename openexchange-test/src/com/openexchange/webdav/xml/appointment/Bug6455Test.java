package com.openexchange.webdav.xml.appointment;

import com.openexchange.exception.OXException;
import com.openexchange.groupware.calendar.OXCalendarExceptionCodes;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.webdav.xml.AppointmentTest;

public class Bug6455Test extends AppointmentTest {
	
	public Bug6455Test(final String name) {
		super(name);
	}
	
	public void testDummy() {
		
	}
	
	public void testBug6455() throws Exception {
		final StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("testBug6455");
		stringBuffer.append(" - ");
		stringBuffer.append("012345678901234567890123456789012345678901234567890123456789"); // 60 chars
		stringBuffer.append("012345678901234567890123456789012345678901234567890123456789"); // 60 chars
		stringBuffer.append("012345678901234567890123456789012345678901234567890123456789"); // 60 chars
		stringBuffer.append("012345678901234567890123456789012345678901234567890123456789"); // 60 chars
		stringBuffer.append("012345678901234567890123456789012345678901234567890123456789"); // 60 chars
	
		final Appointment appointmentObj = new Appointment();
		appointmentObj.setTitle(stringBuffer.toString());
		appointmentObj.setStartDate(startTime);
		appointmentObj.setEndDate(endTime);
		appointmentObj.setShownAs(Appointment.ABSENT);
		appointmentObj.setParentFolderID(appointmentFolderId);
		appointmentObj.setIgnoreConflicts(true);
		
		int objectId = 0;
		
		try {
			objectId = insertAppointment(getWebConversation(), appointmentObj, getHostName(), getLogin(), getPassword(), context);
			fail("permission exception expected!");
		} catch (final OXException exc) {
			assertTrue(exc.similarTo(OXCalendarExceptionCodes.TRUNCATED_SQL_ERROR));
		}
		
		if (objectId > 0) {
			deleteAppointment(getWebConversation(), objectId, appointmentFolderId, getHostName(), getLogin(), getPassword(), context);
		}
	}
}

