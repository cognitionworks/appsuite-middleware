package com.openexchange.webdav.xml.contact;

import com.openexchange.groupware.container.ContactObject;
import com.openexchange.webdav.xml.ContactTest;
import java.util.Date;

public class ListTest extends ContactTest {

	public void testPropFindWithModified() throws Exception {
		Date modified = new Date();
		
		ContactObject contactObj = createContactObject("testPropFindWithModified");
		int objectId1 = insertContact(webCon, contactObj, PROTOCOL + hostName, login, password);
		int objectId2 = insertContact(webCon, contactObj, PROTOCOL + hostName, login, password);
		
		ContactObject[] contactArray = listContact(webCon, contactFolderId, modified, true, false, PROTOCOL + hostName, login, password);
		
		assertTrue("check response", contactArray.length >= 2);
		deleteContact(getWebConversation(), objectId1, contactFolderId, PROTOCOL + getHostName(), getLogin(), getPassword());
		deleteContact(getWebConversation(), objectId2, contactFolderId, PROTOCOL + getHostName(), getLogin(), getPassword());
	}
	
	public void testPropFindWithDelete() throws Exception {
		Date modified = new Date();
		
		ContactObject contactObj = createContactObject("testPropFindWithModified");
		int objectId1 = insertContact(webCon, contactObj, PROTOCOL + hostName, login, password);
		int objectId2 = insertContact(webCon, contactObj, PROTOCOL + hostName, login, password);
		
		int[][] objectIdAndFolderId = { { objectId1, contactFolderId }, { objectId2, contactFolderId } };
		
		deleteContact(webCon, objectIdAndFolderId, PROTOCOL + hostName, login, password);
		
		ContactObject[] appointmentArray = listContact(webCon, contactFolderId, modified, false, true, PROTOCOL + hostName, login, password);
		
		assertTrue("wrong response array length", appointmentArray.length >= 2);
	}
	
	public void testPropFindWithObjectId() throws Exception {
		ContactObject contactObj = createContactObject("testPropFindWithObjectId");
		int objectId = insertContact(webCon, contactObj, PROTOCOL + hostName, login, password);
		
		ContactObject loadContact = loadContact(webCon, objectId, contactFolderId, PROTOCOL + hostName, login, password);
		
		contactObj.setObjectID(objectId);
		compareObject(contactObj, loadContact);
		deleteContact(getWebConversation(), objectId, contactFolderId, PROTOCOL + getHostName(), getLogin(), getPassword());
	}
	
	public void testListWithAllFields() throws Exception {
		ContactObject contactObj = createCompleteContactObject();
		
		Date modified = new Date();
		
		int objectId = insertContact(webCon, contactObj, PROTOCOL + hostName, login, password);
		
		ContactObject[] appointmentArray = listContact(webCon, contactFolderId, modified, true, false, PROTOCOL + hostName, login, password);
		
		assertTrue("wrong response array length", appointmentArray.length >= 1);
		
		ContactObject loadContact = appointmentArray[0];
		contactObj.setObjectID(objectId);
		
		compareObject(contactObj, loadContact);
		deleteContact(getWebConversation(), objectId, contactFolderId, PROTOCOL + getHostName(), getLogin(), getPassword());
	}
	
}
