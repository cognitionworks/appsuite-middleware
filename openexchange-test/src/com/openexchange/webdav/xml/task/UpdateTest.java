package com.openexchange.webdav.xml.task;

import com.openexchange.group.Group;
import com.openexchange.groupware.container.GroupParticipant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.test.TestException;
import com.openexchange.webdav.xml.GroupUserTest;
import com.openexchange.webdav.xml.TaskTest;
import com.openexchange.webdav.xml.XmlServlet;
import java.util.Date;

public class UpdateTest extends TaskTest {

	public UpdateTest(String name) {
		super(name);
	}
	
	public void testUpdateTask() throws Exception {
		Task taskObj = createTask("testUpdateTask");
		int objectId = insertTask(webCon, taskObj, PROTOCOL + hostName, login, password);
		
		taskObj = createTask("testUpdateTask2");
		taskObj.setNote(null);
		
		updateTask(webCon, taskObj, objectId, taskFolderId, PROTOCOL + hostName, login, password);
		deleteTask(getWebConversation(), objectId, taskFolderId, PROTOCOL + getHostName(), getLogin(), getPassword());
	}
	
	public void testUpdateTaskRemoveAlarm() throws Exception {
		Task taskObj = createTask("testUpdateTaskRemoveAlarm");
		taskObj.setAlarm(new Date(startTime.getTime()-(2*dayInMillis)));
		int objectId = insertTask(webCon, taskObj, PROTOCOL + hostName, login, password);
		
		taskObj = createTask("testUpdateTaskRemoveAlarm2");
		taskObj.setNote(null);
		taskObj.setAlarmFlag(false);
		
		updateTask(webCon, taskObj, objectId, taskFolderId, PROTOCOL + hostName, login, password);
		Task loadTask = loadTask(getWebConversation(), objectId, taskFolderId, PROTOCOL + getHostName(), getLogin(), getPassword());
		compareObject(taskObj, loadTask);
		deleteTask(getWebConversation(), objectId, taskFolderId, PROTOCOL + getHostName(), getLogin(), getPassword());
	}
	
	public void testUpdateTaskWithParticipants() throws Exception {
		Task taskObj = createTask("testUpdateTask");
		int objectId = insertTask(webCon, taskObj, PROTOCOL + hostName, login, password);
		
		taskObj = createTask("testUpdateTask");
		
		int userParticipantId = GroupUserTest.getUserId(getWebConversation(), PROTOCOL + getHostName(), userParticipant3, getPassword());
		assertTrue("user participant not found", userParticipantId != -1);
		Group[] groupArray = GroupUserTest.searchGroup(webCon, groupParticipant, new Date(0), PROTOCOL + hostName, login, password);
		assertTrue("group array size is not > 0", groupArray.length > 0);
		int groupParticipantId = groupArray[0].getIdentifier();
		
		com.openexchange.groupware.container.Participant[] participants = new com.openexchange.groupware.container.Participant[2];
		participants[0] = new UserParticipant();
		participants[0].setIdentifier(userParticipantId);
		participants[1] = new GroupParticipant();
		participants[1].setIdentifier(groupParticipantId);
		
		taskObj.setParticipants(participants);
		
		updateTask(webCon, taskObj, objectId, taskFolderId, PROTOCOL + hostName, login, password);
		Task loadTask = loadTask(getWebConversation(), objectId, taskFolderId, PROTOCOL + getHostName(), getLogin(), getPassword());
		compareObject(taskObj, loadTask);
		deleteTask(getWebConversation(), objectId, taskFolderId, PROTOCOL + getHostName(), getLogin(), getPassword());
	}
	
	public void testUpdateConcurentConflict() throws Exception {
		Task taskObj = createTask("testUpdateTaskConcurentConflict");
		int objectId = insertTask(webCon, taskObj, PROTOCOL + hostName, login, password);
		
		taskObj = createTask("testUpdateTaskConcurentConflict2");
		
		try {
			updateTask(webCon, taskObj, objectId, taskFolderId, new Date(0), PROTOCOL + hostName, login, password);
			fail("expected concurent modification exception!");
		} catch (TestException exc) {
			assertExceptionMessage(exc.getMessage(), XmlServlet.MODIFICATION_STATUS);
		}
		
		int[][] objectIdAndFolderId = { {objectId, taskFolderId } };
		deleteTask(webCon, objectIdAndFolderId, PROTOCOL + hostName, login, password );
	}
	
	public void testUpdateNotFound() throws Exception {
		Task taskObj = createTask("testUpdateTaskNotFound");
		int objectId = insertTask(webCon, taskObj, PROTOCOL + hostName, login, password);
		
		taskObj = createTask("testUpdateTaskNotFound2");
		
		try {
			updateTask(webCon, taskObj, (objectId + 1000), taskFolderId, new Date(0), PROTOCOL + hostName, login, password);
			fail("expected object not found exception!");
		} catch (TestException exc) {
			assertExceptionMessage(exc.getMessage(), XmlServlet.OBJECT_NOT_FOUND_STATUS);
		}
		
		int[][] objectIdAndFolderId = { {objectId, taskFolderId } };
		deleteTask(webCon, objectIdAndFolderId, PROTOCOL + hostName, login, password );
	}
}

