package com.openexchange.webdav.xml.task;

import com.openexchange.api2.OXException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.server.OCLPermission;
import com.openexchange.webdav.xml.TaskTest;
import com.openexchange.webdav.xml.FolderTest;
import com.openexchange.webdav.xml.XmlServlet;

public class PermissionTest extends TaskTest {
	
	public void testDummy() {
		
	}
	
	public void _notestInsertTaskInPrivateFolderWithoutPermission() throws Exception {
		FolderObject folderObj = new FolderObject();
		folderObj.setFolderName("testInsertTaskInPrivateFolderWithoutPermission" + System.currentTimeMillis());
		folderObj.setModule(FolderObject.TASK);
		folderObj.setType(FolderObject.PRIVATE);
		folderObj.setParentFolderID(1);
		
		OCLPermission[] permission = new OCLPermission[] { 
			FolderTest.createPermission( userId, false, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS, OCLPermission.DELETE_OWN_OBJECTS),
		};
		
		folderObj.setPermissionsAsArray( permission );
		
		final int parentFolderId = FolderTest.insertFolder(getWebConversation(), folderObj, PROTOCOL + getHostName(), getLogin(), getPassword());
		
		Task taskObj = new Task();
		taskObj.setTitle("testInsertTaskInPrivateFolderWithoutPermission");
		taskObj.setStartDate(startTime);
		taskObj.setEndDate(endTime);
		taskObj.setParentFolderID(parentFolderId);
		
		try {
			final int taskObjectId = insertTask(getSecondWebConversation(), taskObj, PROTOCOL + getHostName(), getSecondLogin(), getPassword());
			fail("permission exception expected!");
		} catch (OXException exc) {
			assertExceptionMessage(exc.getMessage(), XmlServlet.PERMISSION_STATUS);
		}
		
		FolderTest.deleteFolder(getWebConversation(), new int[] { parentFolderId }, PROTOCOL + getHostName(), getLogin(), getPassword());
	}
	
	public void _notestInsertTaskInPublicFolderWithoutPermission() throws Exception {
		FolderObject folderObj = new FolderObject();
		folderObj.setFolderName("testInsertTaskInPublicFolderWithoutPermission" + System.currentTimeMillis());
		folderObj.setModule(FolderObject.TASK);
		folderObj.setType(FolderObject.PUBLIC);
		folderObj.setParentFolderID(2);
		
		OCLPermission[] permission = new OCLPermission[] { 
			FolderTest.createPermission( userId, false, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS, OCLPermission.DELETE_OWN_OBJECTS)
		};
		
		folderObj.setPermissionsAsArray( permission );
		
		final int parentFolderId = FolderTest.insertFolder(getWebConversation(), folderObj, PROTOCOL + getHostName(), getLogin(), getPassword());
		
		Task taskObj = new Task();
		taskObj.setTitle("testInsertTaskInPublicFolderWithoutPermission");
		taskObj.setStartDate(startTime);
		taskObj.setEndDate(endTime);
		taskObj.setParentFolderID(parentFolderId);
		
		try {
			final int taskObjectId = insertTask(getSecondWebConversation(), taskObj, PROTOCOL + getHostName(), getSecondLogin(), getPassword());
			fail("permission exception expected!");
		} catch (OXException exc) {
			assertExceptionMessage(exc.getMessage(), XmlServlet.PERMISSION_STATUS);
		}
		
		FolderTest.deleteFolder(getWebConversation(), new int[] { parentFolderId }, PROTOCOL + getHostName(), getLogin(), getPassword());
	}
	
	public void _notestUpdateTaskInPrivateFolderWithoutPermission() throws Exception {
		FolderObject folderObj = new FolderObject();
		folderObj.setFolderName("testInsertTaskInPrivateFolderWithoutPermission" + System.currentTimeMillis());
		folderObj.setModule(FolderObject.TASK);
		folderObj.setType(FolderObject.PRIVATE);
		folderObj.setParentFolderID(1);
		
		OCLPermission[] permission = new OCLPermission[] { 
			FolderTest.createPermission( userId, false, OCLPermission.CREATE_OBJECTS_IN_FOLDER, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS, OCLPermission.DELETE_OWN_OBJECTS),
		};
		
		folderObj.setPermissionsAsArray( permission );
		
		final int parentFolderId = FolderTest.insertFolder(getWebConversation(), folderObj, PROTOCOL + getHostName(), getLogin(), getPassword());
		
		Task taskObj = new Task();
		taskObj.setTitle("testInsertTaskInPrivateFolderWithoutPermission");
		taskObj.setStartDate(startTime);
		taskObj.setEndDate(endTime);
		taskObj.setParentFolderID(parentFolderId);
		
		final int taskObjectId = insertTask(getWebConversation(), taskObj, PROTOCOL + getHostName(), getLogin(), getPassword());
		taskObj.setObjectID(taskObjectId);
		
		try {
			updateTask(getSecondWebConversation(), taskObj, taskObjectId, parentFolderId, PROTOCOL + getHostName(), getSecondLogin(), getPassword());
			fail("permission exception expected!");
		} catch (OXException exc) {
			assertExceptionMessage(exc.getMessage(), XmlServlet.PERMISSION_STATUS);
		}
		
		FolderTest.deleteFolder(getWebConversation(), new int[] { parentFolderId }, PROTOCOL + getHostName(), getLogin(), getPassword());
	}
	
	public void _notestUpdateTaskInPublicFolderWithoutPermission() throws Exception {
		FolderObject folderObj = new FolderObject();
		folderObj.setFolderName("testUpdateTaskInPublicFolderWithoutPermission" + System.currentTimeMillis());
		folderObj.setModule(FolderObject.TASK);
		folderObj.setType(FolderObject.PUBLIC);
		folderObj.setParentFolderID(2);
		
		OCLPermission[] permission = new OCLPermission[] { 
			FolderTest.createPermission( userId, false, OCLPermission.CREATE_OBJECTS_IN_FOLDER, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS, OCLPermission.DELETE_OWN_OBJECTS)
		};
		
		folderObj.setPermissionsAsArray( permission );
		
		final int parentFolderId = FolderTest.insertFolder(getWebConversation(), folderObj, PROTOCOL + getHostName(), getLogin(), getPassword());
		
		Task taskObj = new Task();
		taskObj.setTitle("testUpdateTaskInPublicFolderWithoutPermission");
		taskObj.setStartDate(startTime);
		taskObj.setEndDate(endTime);
		taskObj.setParentFolderID(parentFolderId);
		
		final int taskObjectId = insertTask(getWebConversation(), taskObj, PROTOCOL + getHostName(), getLogin(), getPassword());
		taskObj.setObjectID(taskObjectId);
		
		try {
			updateTask(getSecondWebConversation(), taskObj, taskObjectId, parentFolderId, PROTOCOL + getHostName(), getSecondLogin(), getPassword());
			fail("permission exception expected!");
		} catch (OXException exc) {
			assertExceptionMessage(exc.getMessage(), XmlServlet.PERMISSION_STATUS);
		}
		
		FolderTest.deleteFolder(getWebConversation(), new int[] { parentFolderId }, PROTOCOL + getHostName(), getLogin(), getPassword());
	}
	
	public void _notestDeleteTaskInPrivateFolderWithoutPermission() throws Exception {
		FolderObject folderObj = new FolderObject();
		folderObj.setFolderName("testDeleteTaskInPrivateFolderWithoutPermission" + System.currentTimeMillis());
		folderObj.setModule(FolderObject.TASK);
		folderObj.setType(FolderObject.PRIVATE);
		folderObj.setParentFolderID(1);
		
		OCLPermission[] permission = new OCLPermission[] { 
			FolderTest.createPermission( userId, false, OCLPermission.CREATE_OBJECTS_IN_FOLDER, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS, OCLPermission.DELETE_OWN_OBJECTS),
		};
		
		folderObj.setPermissionsAsArray( permission );
		
		final int parentFolderId = FolderTest.insertFolder(getWebConversation(), folderObj, PROTOCOL + getHostName(), getLogin(), getPassword());
		
		Task taskObj = new Task();
		taskObj.setTitle("testDeleteTaskInPrivateFolderWithoutPermission");
		taskObj.setStartDate(startTime);
		taskObj.setEndDate(endTime);
		taskObj.setParentFolderID(parentFolderId);
		
		final int taskObjectId = insertTask(getWebConversation(), taskObj, PROTOCOL + getHostName(), getLogin(), getPassword());
		taskObj.setObjectID(taskObjectId);
		
		try {
			deleteTask(getSecondWebConversation(), taskObjectId, parentFolderId, PROTOCOL + getHostName(), getSecondLogin(), getPassword());
			fail("permission exception expected!");
		} catch (OXException exc) {
			assertExceptionMessage(exc.getMessage(), XmlServlet.PERMISSION_STATUS);
		}
		
		FolderTest.deleteFolder(getWebConversation(), new int[] { parentFolderId }, PROTOCOL + getHostName(), getLogin(), getPassword());
	}
	
	public void _notestDeleteTaskInPublicFolderWithoutPermission() throws Exception {
		FolderObject folderObj = new FolderObject();
		folderObj.setFolderName("testDeleteTaskInPublicFolderWithoutPermission" + System.currentTimeMillis());
		folderObj.setModule(FolderObject.TASK);
		folderObj.setType(FolderObject.PUBLIC);
		folderObj.setParentFolderID(2);
		
		OCLPermission[] permission = new OCLPermission[] { 
			FolderTest.createPermission( userId, false, OCLPermission.CREATE_OBJECTS_IN_FOLDER, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS, OCLPermission.DELETE_OWN_OBJECTS)
		};
		
		folderObj.setPermissionsAsArray( permission );
		
		final int parentFolderId = FolderTest.insertFolder(getWebConversation(), folderObj, PROTOCOL + getHostName(), getLogin(), getPassword());
		
		Task taskObj = new Task();
		taskObj.setTitle("testDeleteTaskInPublicFolderWithoutPermission");
		taskObj.setStartDate(startTime);
		taskObj.setEndDate(endTime);
		taskObj.setParentFolderID(parentFolderId);
		
		final int taskObjectId = insertTask(getWebConversation(), taskObj, PROTOCOL + getHostName(), getLogin(), getPassword());
		taskObj.setObjectID(taskObjectId);
		
		try {
			deleteTask(getSecondWebConversation(), taskObjectId, parentFolderId, PROTOCOL + getHostName(), getSecondLogin(), getPassword());
			fail("permission exception expected!");
		} catch (OXException exc) {
			assertExceptionMessage(exc.getMessage(), XmlServlet.PERMISSION_STATUS);
		}
		
		FolderTest.deleteFolder(getWebConversation(), new int[] { parentFolderId }, PROTOCOL + getHostName(), getLogin(), getPassword());
	}
}

