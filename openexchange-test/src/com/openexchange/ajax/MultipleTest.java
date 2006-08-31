package com.openexchange.ajax;


import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.openexchange.ajax.writer.ContactWriter;
import com.openexchange.groupware.container.ContactObject;
import com.openexchange.tools.URLParameter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import org.json.JSONArray;
import org.json.JSONObject;

public class MultipleTest extends AbstractAJAXTest {
	
	private static final String MULTIPLE_URL = "/ajax/resource";
	
	public void testMultiple() throws Exception {
		final URLParameter parameter = new URLParameter();
		parameter.setParameter(AJAXServlet.PARAMETER_SESSION, getSessionId());
		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		jsonObj.put(Multiple.MODULE, Multiple.MODULE_CONTACT);
		jsonObj.put(Multiple.PARAMETER_ACTION, Multiple.ACTION_NEW);
		
		ContactObject contactObj = new ContactObject();
		contactObj.setSurName("testMultiple1");
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		ContactWriter contactWriter = new ContactWriter(pw);
		contactWriter.writeContact(contactObj);
		pw.flush();		
		
		String data = new String(baos.toByteArray());
		jsonObj.put("data", new JSONObject(data));
		jsonArray.put(jsonObj);
		
		jsonObj = new JSONObject();
		jsonObj.put(Multiple.MODULE, Multiple.MODULE_CONTACT);
		jsonObj.put(Multiple.PARAMETER_ACTION, Multiple.ACTION_NEW);
		
		contactObj = new ContactObject();
		contactObj.setSurName("testMultiple2");
		
		baos = new ByteArrayOutputStream();
		pw = new PrintWriter(baos);
		contactWriter = new ContactWriter(pw);
		contactWriter.writeContact(contactObj);
		pw.flush();		
		
		data = new String(baos.toByteArray());
		jsonObj.put("data", new JSONObject(data));
		jsonArray.put(jsonObj);
		
		ByteArrayInputStream bais = new ByteArrayInputStream(jsonArray.toString().getBytes());
		WebRequest req = new PutMethodWebRequest(PROTOCOL + getHostName() + MULTIPLE_URL + parameter.getURLParameters(), bais, "text/javascript");
		WebResponse resp = getWebConversation().getResponse(req);
		
		assertEquals(200, resp.getResponseCode());
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

