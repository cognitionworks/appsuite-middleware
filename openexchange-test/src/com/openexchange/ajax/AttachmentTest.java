package com.openexchange.ajax;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebResponse;
import com.openexchange.ajax.container.Response;
import com.openexchange.groupware.Init;
import com.openexchange.groupware.attach.AttachmentMetadata;

public class AttachmentTest extends AbstractAJAXTest {
	
	protected String sessionId = null;
	protected File testFile = null;
	
	protected List<AttachmentMetadata> clean = new ArrayList<AttachmentMetadata>();
	
	public void setUp() throws Exception {
		sessionId = getSessionId();
		testFile = new File(Init.getTestProperty("ajaxPropertiesFile"));
	}
	
	public void tearDown() throws Exception {
		for(AttachmentMetadata attachment : clean) {
			detach(sessionId, attachment.getFolderId(), attachment.getAttachedId(), attachment.getModuleId(), new int[]{attachment.getId()});
		}
		clean.clear();
	}
	
	
	public Response attach(String sessionId, int folderId, int attachedId, int moduleId, File upload) throws JSONException, IOException {
		return attach(sessionId,folderId,attachedId,moduleId,upload,null,null);
	}
	
	public Response attach(String sessionId, int folderId, int attachedId, int moduleId, File upload, String filename, String mimeType) throws JSONException, IOException {
		StringBuffer url = getUrl(sessionId,"attach");
		
		PostMethodWebRequest req = new PostMethodWebRequest(url.toString());
		req.setMimeEncoded(true);
		
		
		JSONObject object = new JSONObject();
		
		object.put("folder", folderId);
		object.put("attached", attachedId);
		object.put("module",moduleId);
		if(filename != null)
			object.put("filename", filename);
		if(mimeType != null)
			object.put("file_mimetype",mimeType);
		
		req.setParameter("json",object.toString());
		if(upload != null) {
			req.selectFile("file",upload);
		}
		
		WebResponse resp = getWebConversation().getResource(req);
		
		String html = resp.getText();
		JSONObject response = extractFromCallback(html);
//		if(!"".equals(response.optString("error"))) {
//			throw new IOException(response.getString("error"));
//		}
		
		return Response.parse(response.toString());
	}

	public Response detach(String sessionId, int folderId, int attachedId, int moduleId, int[] ids) throws MalformedURLException, JSONException, IOException, SAXException {
		StringBuffer url = getUrl(sessionId,"detach");
		addCommon(url, folderId, attachedId, moduleId);
		
		StringBuffer data = new StringBuffer("[");
		for(int id : ids){
			data.append(id);
			data.append(",");
		}
		data.setLength(data.length()-1);
		data.append("]");
		
		return putT(url.toString(),data.toString());
	}
	
	public Response updates(String sessionId, int folderId, int attachedId, int moduleId, long timestamp, int[] columns, int sort, String order) throws MalformedURLException, JSONException, IOException, SAXException {
		StringBuffer url = getUrl(sessionId, "updates");
		addCommon(url, folderId, attachedId, moduleId);
		addSort(url,columns,sort,order);
		url.append("&timestamp="+timestamp);
		return gT(url.toString());
	}
	
	public Response all(String sessionId, int folderId, int attachedId, int moduleId, int[] columns, int sort, String order) throws MalformedURLException, JSONException, IOException, SAXException {
		StringBuffer url = getUrl(sessionId, "all");
		addCommon(url, folderId, attachedId, moduleId);
		addSort(url, columns,sort,order);
		
		
		return gT(url.toString());
	}
	
	private void addSort(StringBuffer url, int[] columns, int sort, String order) {
		StringBuffer cols = new StringBuffer();
		for(int id : columns) {
			cols.append(id);
			cols.append(",");
		}
		cols.setLength(cols.length()-1);
		
		url.append("&columns=");
		url.append(cols.toString());
		
		url.append("&sort=");
		url.append(sort);
		
		url.append("&order=");
		url.append(order);
	}

	public Response list(String sessionId, int folderId, int attachedId, int moduleId, int[] ids, int[] columns) throws JSONException, MalformedURLException, IOException, SAXException {
		StringBuffer url = getUrl(sessionId, "list");
		addCommon(url,folderId,attachedId,moduleId);
		StringBuffer data = new StringBuffer("[");
		for(int id : ids) {
			data.append(id);
			data.append(",");
		}
		data.setLength(data.length()-1);
		data.append("]");
		
		StringBuffer cols = new StringBuffer();
		for(int col : columns) {
			cols.append(col);
			cols.append(",");
		}
		cols.setLength(cols.length()-1);
		
		url.append("&columns=");
		url.append(cols);
		
		
		return putT(url.toString(), data.toString());
	}
	
	public Response get(String sessionId, int folderId, int attachedId, int moduleId, int id) throws MalformedURLException, JSONException, IOException, SAXException{
		StringBuffer url = getUrl(sessionId,"get");
		addCommon(url, folderId, attachedId, moduleId);
		url.append("&id="+id);
		return gT(url.toString());
	}
	
	public InputStream document(String sessionId, int folderId, int attachedId, int moduleId, int id) throws IOException{
		StringBuffer url = getUrl(sessionId,"document");
		addCommon(url, folderId, attachedId, moduleId);
		url.append("&id="+id);
		
		HttpClient client = new HttpClient();
		GetMethod get = new GetMethod(url.toString());
		client.executeMethod(get);
		
		return get.getResponseBodyAsStream();
	}
	
	private void addCommon(StringBuffer url, int folderId, int attachedId, int moduleId) {
		url.append("&folder="+folderId);
		url.append("&attached="+attachedId);
		url.append("&module="+moduleId);
	}

	protected StringBuffer getUrl(String sessionId, String action) {
		StringBuffer url = new StringBuffer("http://");
		url.append(getHostName());
		url.append("/ajax/attachment?session=");
		url.append(sessionId);
		url.append("&action=");
		url.append(action);
		return url;
	}
}
