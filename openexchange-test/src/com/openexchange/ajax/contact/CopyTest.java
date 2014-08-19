package com.openexchange.ajax.contact;

import java.util.Date;
import org.json.JSONObject;
import com.openexchange.ajax.contact.action.CopyRequest;
import com.openexchange.ajax.contact.action.CopyResponse;
import com.openexchange.ajax.contact.action.DeleteRequest;
import com.openexchange.ajax.contact.action.GetRequest;
import com.openexchange.ajax.contact.action.GetResponse;
import com.openexchange.ajax.fields.DataFields;
import com.openexchange.ajax.folder.Create;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.InsertRequest;
import com.openexchange.ajax.folder.actions.InsertResponse;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;


public class CopyTest extends AbstractContactTest {

	private int objectId1;
	private long ts1, ts2;
    private int objectId2;
    private int targetFolder;
    private FolderObject folder;


    public CopyTest(final String name) {

		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testCopy() throws Exception {
		final Contact contactObj = new Contact();
		contactObj.setSurName("testCopy");
		contactObj.setParentFolderID(contactFolderId);
		objectId1 = insertContact(contactObj);

		folder = Create.createPrivateFolder("testCopy", FolderObject.CONTACT, userId);
		folder.setParentFolderID(client.getValues().getPrivateContactFolder());
		final InsertResponse folderCreateResponse = client.execute(new InsertRequest(EnumAPI.OUTLOOK, folder));
		folderCreateResponse.fillObject(folder);

		targetFolder = folder.getObjectID();

		final CopyRequest request = new CopyRequest(objectId1, contactFolderId, targetFolder, true);
		final CopyResponse response = client.execute(request);


		if (response.hasError()) {
			fail("json error: " + response.getErrorMessage());
		}

		objectId2 = 0;

		final JSONObject data = (JSONObject)response.getData();
		if (data.has(DataFields.ID)) {
			objectId2 = data.getInt(DataFields.ID);
		} else {
		    fail("Could not find copied contact.");
		}

		final GetRequest getFirstContactRequest = new GetRequest(contactFolderId, objectId1, tz);
		final GetResponse firstContactResponse = client.execute(getFirstContactRequest);
		final Contact firstContact = firstContactResponse.getContact();
		ts1 = firstContactResponse.getResponse().getTimestamp().getTime();
		final GetRequest getSecondContactRequest = new GetRequest(targetFolder, objectId2, tz);
		final GetResponse seconContactResponse = client.execute(getSecondContactRequest);
		final Contact secondContact = seconContactResponse.getContact();
		secondContact.setObjectID(objectId1);
		secondContact.setParentFolderID(contactFolderId);
		ts2 = seconContactResponse.getResponse().getTimestamp().getTime();

		compareObject(firstContact, secondContact, false);
	}


    @Override
    protected void tearDown() throws Exception {
        client.execute(new DeleteRequest(contactFolderId, objectId1, new Date(ts1), false));
        if (objectId2 > 0) {
            client.execute(new DeleteRequest(targetFolder, objectId2, new Date(ts2), false));
        }
        client.execute(new com.openexchange.ajax.folder.actions.DeleteRequest(EnumAPI.OUTLOOK, folder));

        super.tearDown();
    }
}
