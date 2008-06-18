package com.openexchange.ajax.contact;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openexchange.ajax.ContactTest;
import com.openexchange.groupware.container.ContactObject;

public class Bug4409Test extends ContactTest {

	private static final Log LOG = LogFactory.getLog(Bug4409Test.class);
	
	public Bug4409Test(final String name) {
		super(name);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	public void testBug4409() throws Exception {
		final ContactObject contactObj = new ContactObject();
		contactObj.setSurName("testBug4409");
		contactObj.setParentFolderID(contactFolderId);
		
		final int objectId = insertContact(getWebConversation(), contactObj, getHostName(), getSessionId());
		
		loadImage(getWebConversation(),objectId, contactFolderId, getHostName(), getSessionId());
	}
}