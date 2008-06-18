package com.openexchange.ajax.appointment;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openexchange.ajax.AppointmentTest;
import com.openexchange.groupware.container.AppointmentObject;

public class SearchTest extends AppointmentTest {

	private static final Log LOG = LogFactory.getLog(SearchTest.class);
	
	public SearchTest(final String name) {
		super(name);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	public void testSimpleSearch() throws Exception {
		final AppointmentObject appointmentObj = new AppointmentObject();
		final String date = String.valueOf(System.currentTimeMillis());
		appointmentObj.setTitle("testSimpleSearch" + date);
		appointmentObj.setStartDate(new Date(startTime));
		appointmentObj.setEndDate(new Date(endTime));
		appointmentObj.setParentFolderID(appointmentFolderId);
		appointmentObj.setIgnoreConflicts(true);
		
		final int objectId = insertAppointment(getWebConversation(), appointmentObj, timeZone, PROTOCOL + getHostName(), getSessionId());
		
		final AppointmentObject[] appointmentArray = searchAppointment(getWebConversation(), "testSimpleSearch" + date, appointmentFolderId, new Date(), new Date(), APPOINTMENT_FIELDS, timeZone, PROTOCOL + getHostName(), getSessionId());
		assertTrue("appointment array size is 0", appointmentArray.length > 0);
		
		deleteAppointment(getWebConversation(), objectId, appointmentFolderId, PROTOCOL + getHostName(), getSessionId());
	}
}