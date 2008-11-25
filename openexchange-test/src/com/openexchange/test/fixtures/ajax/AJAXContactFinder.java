package com.openexchange.test.fixtures.ajax;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.xml.sax.SAXException;

import com.openexchange.ajax.contact.action.AllRequest;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.CommonAllResponse;
import com.openexchange.groupware.contact.ContactException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.contact.helpers.ContactSetter;
import com.openexchange.groupware.container.ContactObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.test.fixtures.ContactFinder;
import com.openexchange.test.fixtures.SimpleCredentials;
import com.openexchange.tools.servlet.AjaxException;

public class AJAXContactFinder implements ContactFinder {

	private AJAXClient client;
	private HashMap<Integer, ContactObject> globalAddressBook;
	
	public AJAXContactFinder(AJAXClient client) {
		this.client = client;
	}
	
	private void loadGlobalAddressBook() {
		AllRequest all = new AllRequest(FolderObject.SYSTEM_LDAP_FOLDER_ID, ContactObject.ALL_COLUMNS);
		
		try {
			CommonAllResponse response = client.execute(all);
			globalAddressBook = new HashMap<Integer, ContactObject>();
			JSONArray rows = (JSONArray) response.getData();
			for(int i = 0, size = rows.length(); i < size; i++) {
				JSONArray row = rows.getJSONArray(i);
				ContactObject contact = new ContactObject();
				ContactSetter setter = new ContactSetter();
				for(int index = 0; index < ContactObject.ALL_COLUMNS.length; index++) {
					int column = ContactObject.ALL_COLUMNS[index];
					ContactField field = ContactField.getByValue(column);
					field.doSwitch(setter, contact, row.get(index));
				}
				globalAddressBook.put(contact.getInternalUserId(), contact);
			}
		} catch (AjaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (ContactException e) {
			e.printStackTrace();
		}
	}
	
	public ContactObject getContact(SimpleCredentials credentials) {
		return getContact( credentials.getUserId() );
	}

	public ContactObject getContact(int userId){
		if(globalAddressBook == null) {
			loadGlobalAddressBook(); 
		}		
		return globalAddressBook.get(userId);
	}
}
