package com.openexchange.ajax;


import java.io.ByteArrayInputStream;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONObject;

import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.writer.ContactWriter;
import com.openexchange.groupware.container.Contact;
import com.openexchange.tools.URLParameter;

public class MultipleTest extends AbstractAJAXTest {
	
	public MultipleTest(final String name) {
		super(name);
	}

	private static final String MULTIPLE_URL = "/ajax/multiple";
	
	public void testMultiple() throws Exception {
		final int folderId = FolderTest.getStandardContactFolder(getWebConversation(), getHostName(), getSessionId()).getObjectID();
		
		final URLParameter parameter = new URLParameter();
		parameter.setParameter(AJAXServlet.PARAMETER_SESSION, getSessionId());
		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		jsonObj.put(Multiple.MODULE, Multiple.MODULE_CONTACT);
		jsonObj.put(Multiple.PARAMETER_ACTION, Multiple.ACTION_NEW);
		
		Contact contactObj = new Contact();
		contactObj.setSurName("testMultiple1");
		contactObj.setParentFolderID(folderId);
		
		JSONObject jsonDataObj = new JSONObject();
		ContactWriter contactWriter = new ContactWriter(TimeZone.getDefault());
		contactWriter.writeContact(contactObj, jsonDataObj);
		
		jsonObj.put("data", jsonDataObj);
		jsonArray.put(jsonObj);
		
		jsonObj = new JSONObject();
		jsonObj.put(Multiple.MODULE, Multiple.MODULE_CONTACT);
		jsonObj.put(Multiple.PARAMETER_ACTION, Multiple.ACTION_NEW);
		
		contactObj = new Contact();
		contactObj.setSurName("testMultiple2");
		contactObj.setParentFolderID(folderId);
		
		jsonDataObj = new JSONObject();
		contactWriter = new ContactWriter(TimeZone.getDefault());
		contactWriter.writeContact(contactObj, jsonDataObj);
		
		jsonObj.put("data", jsonDataObj);
		jsonArray.put(jsonObj);
		
		final ByteArrayInputStream bais = new ByteArrayInputStream(jsonArray.toString().getBytes());
		final WebRequest req = new PutMethodWebRequest(PROTOCOL + getHostName() + MULTIPLE_URL + parameter.getURLParameters(), bais, "text/javascript");
		final WebResponse resp = getWebConversation().getResponse(req);
		
		assertEquals(200, resp.getResponseCode());
		jsonArray = new JSONArray(resp.getText());
		for (int a = 0; a < jsonArray.length(); a++) {
			final Response response = Response.parse(jsonArray.getJSONObject(a).toString());
			
			if (response.hasError()) {
				fail("json error: " + response.getErrorMessage());
			}
		}
	}
	
	/*
	public static com.openexchange.groupware.ldap.Resource[] searchResource(WebConversation webCon, String searchpattern, String host, String session) throws Exception {
		final URLParameter parameter = new URLParameter();
		parameter.setParameter(AJAXServlet.PARAMETER_SESSION, session);
		parameter.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_SEARCH);
	 
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pattern", searchpattern);
	 
		ByteArrayInputStream bais = new ByteArrayInputStream(jsonObj.toString().getBytes());
		WebRequest req = new PutMethodWebRequest(host + RESOURCE_URL + parameter.getURLParameters(), bais, "text/javascript");
		WebResponse resp = webCon.getResponse(req);
	 
		assertEquals(200, resp.getResponseCode());
	 
		final Response response = Response.parse(resp.getText());
	 
		if (response.hasError()) {
			fail("json error: " + response.getErrorMessage());
		}
	 
		assertNotNull("timestamp is null", response.getTimestamp());
	 
		JSONArray jsonArray = (JSONArray)response.getData();
		com.openexchange.groupware.ldap.Resource[] r = new com.openexchange.groupware.ldap.Resource[jsonArray.length()];
		for (int a = 0; a < r.length; a++) {
			JSONObject jObj = jsonArray.getJSONObject(a);
			r[a] = new com.openexchange.groupware.ldap.Resource();
			r[a].setIdentifier(jObj.getInt(ParticipantsFields.ID));
			if (jObj.has(ParticipantsFields.DISPLAY_NAME)) {
				r[a].setDisplayName(jObj.getString(ParticipantsFields.DISPLAY_NAME));
			}
		}
	 
		return r;
	}
	 */
}

