package com.openexchange.ajax.appointment.bugtests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openexchange.ajax.AppointmentTest;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.webdav.xml.FolderTest;

public class Bug4541Test extends AppointmentTest {
	
	private static final Log LOG = LogFactory.getLog(Bug4541Test.class);
	
	public Bug4541Test(final String name) {
		super(name);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	public void testBug4541() throws Exception {
		final FolderObject folderObj = new FolderObject();
		folderObj.setFolderName("testBug4541" + System.currentTimeMillis());
		folderObj.setParentFolderID(FolderObject.PRIVATE);
		folderObj.setModule(FolderObject.CALENDAR);
		folderObj.setType(FolderObject.PRIVATE);
		
		final OCLPermission[] permission = new OCLPermission[] {
			FolderTest.createPermission( userId, false, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION),
		};
		
		folderObj.setPermissionsAsArray( permission );
		
		final int newFolderId = FolderTest.insertFolder(getWebConversation(), folderObj, getHostName(), getLogin(), getPassword());

		final Appointment appointmentObj = createAppointmentObject("testBug4541");
		appointmentObj.setParentFolderID(newFolderId);
		appointmentObj.setIgnoreConflicts(true);
		
		final com.openexchange.groupware.container.Participant[] participants = new com.openexchange.groupware.container.Participant[1];
		participants[0] = new UserParticipant();
		participants[0].setIdentifier(userId);
		
		appointmentObj.setParticipants(participants);
		
		final int objectId = insertAppointment(getWebConversation(), appointmentObj, timeZone, getHostName(), getSessionId());
		appointmentObj.setObjectID(objectId);
		
		Appointment loadAppointment = loadAppointment(getWebConversation(), objectId, newFolderId, timeZone, getHostName(), getSessionId());
		compareObject(appointmentObj, loadAppointment, appointmentObj.getStartDate().getTime(), appointmentObj.getEndDate().getTime());
		
		appointmentObj.setTitle("testBug4541 - update");
		appointmentObj.removeParentFolderID();
		
		updateAppointment(getWebConversation(), appointmentObj, objectId, newFolderId, timeZone, getHostName(), getSessionId());
		appointmentObj.setParentFolderID(newFolderId);
		
		loadAppointment = loadAppointment(getWebConversation(), objectId, newFolderId, timeZone, getHostName(), getSessionId());
		compareObject(appointmentObj, loadAppointment, appointmentObj.getStartDate().getTime(), appointmentObj.getEndDate().getTime());
		
		deleteAppointment(getWebConversation(), objectId, newFolderId, getHostName(), getSessionId());
		FolderTest.deleteFolder(getWebConversation(), new int[] { newFolderId }, getHostName(), getLogin(), getPassword());
	}
}