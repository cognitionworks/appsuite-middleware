package com.openexchange.ajax;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.openexchange.ajax.container.Response;

public class InfostoreAJAXTest extends AbstractAJAXTest {

	public static final String INFOSTORE_FOLDER = "infostore.folder";
	
	protected int folderId;
	
	protected String sessionId;
	
	protected List<Integer> clean = new ArrayList<Integer>();
	
	protected String hostName = null;
	
	public InfostoreAJAXTest(final String name){
		super(name);
	}	
	
	@Override
	public void setUp() throws Exception {
		this.sessionId = getSessionId();
		final int userId = FolderTest.getUserId(getWebConversation(), getHostName(), getLogin(), getPassword());
		this.folderId = FolderTest.getMyInfostoreFolder(getWebConversation(),getHostName(),sessionId,userId).getObjectID();
		
		Map<String,String> create = m(
			"folder_id" 		,	((Integer)folderId).toString(),
			"title"  		,  	"test knowledge",
			"description" 	, 	"test knowledge description"
		);
		
		int c = this.createNew(getWebConversation(),getHostName(), sessionId, create);
		
		clean.add(c);
		
		create = m(
				"folder_id" 		, 	((Integer)folderId).toString(),
				"title"  		,  	"test url",
				"description" 	, 	"test url description",
				"url" 			, 	"http://www.open-xchange.com"
			);
			
		c = this.createNew(getWebConversation(),getHostName(), sessionId, create);
		
		clean.add(c);
	}
	
	@Override
	public void tearDown() throws Exception {
		final int[][] toDelete = new int[clean.size()][2];
		
		for(int i = 0; i < toDelete.length; i++) {
			toDelete[i][0] = folderId; // FIXME: Put a correct folderId here
			toDelete[i][1] = clean.get(i);
		}
		
		final int[] notDeleted = delete(getWebConversation(),getHostName(),sessionId, System.currentTimeMillis(), toDelete);
		
		//assertEquals("Couldn't delete "+j(notDeleted),0,notDeleted.length);
	}
	
	
	private String j(final int[] ids) {
		final StringBuffer b = new StringBuffer("[ ");
		for(final int i : ids) {
			b.append(i);
			b.append(" ");
		}
		b.append("]");
		return b.toString();
	}
	
	// Methods from the specification
	
	
	public Response all(final WebConversation webConv, final String hostname, final String sessionId, final int folderId, final int[] columns) throws MalformedURLException, JSONException, IOException, SAXException  {
		return all(webConv,hostname,sessionId,folderId,columns, -1, null);
	}
	
	public Response all(final WebConversation webConv, final String hostname, final String sessionId, final int folderId, final int[] columns, final int sort, final String order) throws MalformedURLException, JSONException, IOException, SAXException {
		final StringBuffer url = getUrl(sessionId,"all", hostname);
		url.append("&folder=");
		url.append(folderId);
		url.append("&columns=");
		for(final int col : columns) {
			url.append(col);
			url.append(",");
		}
		url.deleteCharAt(url.length()-1);
		
		if(sort != -1) {
			url.append("&sort=");
			url.append(sort);
		}
		
		if(order != null){
			url.append("&order=");
			url.append(order);
		}
		
		return gT(webConv, url.toString());
	}
	
	public Response list(final WebConversation webConv, final String hostname, final String sessionId, final int[] columns, final int[][] ids) throws MalformedURLException, JSONException, IOException, SAXException {
		final StringBuffer url = getUrl(sessionId,"list", hostname);
		url.append("&columns=");
		for(final int col : columns) {
			url.append(col);
			url.append(",");
		}
		url.deleteCharAt(url.length()-1);
		
		final StringBuffer data = new StringBuffer("[");
		if(ids.length > 0) {
			for(final int[] tuple : ids) {
				data.append("{folder : ");
				data.append(tuple[0]);
				data.append(", id : ");
				data.append(tuple[1]);
				data.append("},");
			}
			data.deleteCharAt(data.length()-1);
		}
		data.append("]");
		
		return putT(webConv,url.toString(), data.toString()); 
	}

	public Response updates(final WebConversation webConv, final String hostname, final String sessionId, final int folderId, final int[] columns, final long timestamp) throws MalformedURLException, JSONException, IOException, SAXException {
		return updates(webConv,hostname,sessionId,folderId,columns,timestamp,-1, null, null);
	}
	
	public Response updates(final WebConversation webConv, final String hostname, final String sessionId, final int folderId, final int[] columns, final long timestamp, final String ignore) throws MalformedURLException, JSONException, IOException, SAXException {
		return updates(webConv,hostname,sessionId,folderId,columns,timestamp,-1, null, ignore);
	}
	
	public Response updates(final WebConversation webConv, final String hostname, final String sessionId, final int folderId, final int[] columns, final long timestamp, final int sort, final String order, final String ignore) throws MalformedURLException, JSONException, IOException, SAXException {
		final StringBuffer url = getUrl(sessionId,"updates", hostname);
		url.append("&folder=");
		url.append(folderId);
		url.append("&columns=");
		for(final int col : columns) {
			url.append(col);
			url.append(",");
		}
		url.deleteCharAt(url.length()-1);
		
		url.append("&timestamp=");
		url.append(timestamp);
		
		if(sort != -1) {
			url.append("&sort=");
			url.append(sort);
		}
		
		if(order != null){
			url.append("&order=");
			url.append(order);
		}
		
		if(ignore != null){
			url.append("&ignore=");
			url.append(ignore);	
		}
		
		return gT(webConv, url.toString());
	}
	
	public Response get(final WebConversation webConv, final String hostname, final String sessionId, final int objectId) throws MalformedURLException, JSONException, IOException, SAXException  {
		return get(webConv,hostname,sessionId, objectId, -1);
	}
	
	public Response get(final WebConversation webConv, final String hostname, final String sessionId, final int objectId, final int version) throws MalformedURLException, JSONException, IOException, SAXException {
		final StringBuffer url = getUrl(sessionId,"get", hostname);
		url.append("&id=");
		url.append(objectId);
		if(version != -1) {
			url.append("&version=");
			url.append(version);
		}
		
		return gT(webConv, url.toString());
	}
	
	public Response versions(final WebConversation webConv, final String hostname, final String sessionId, final int objectId, final int[] columns) throws MalformedURLException, JSONException, IOException, SAXException {
		return versions(webConv,hostname,sessionId,objectId,columns, -1, null);
	}
	
	public Response versions(final WebConversation webConv, final String hostname, final String sessionId, final int objectId, final int[] columns, final int sort, final String order) throws MalformedURLException, JSONException, IOException, SAXException {
		final StringBuffer url = getUrl(sessionId,"versions", hostname);
		url.append("&id=");
		url.append(objectId);
		url.append("&columns=");
		for(final int col : columns) {
			url.append(col);
			url.append(",");
		}
		url.deleteCharAt(url.length()-1);
		
		if(sort != -1) {
			url.append("&sort=");
			url.append(sort);
		}
		
		if(order != null){
			url.append("&order=");
			url.append(order);
		}
		
		return gT(webConv, url.toString());
	}
	
	private JSONObject toJSONArgs(final Map<String, String> modified) throws JSONException {
		final JSONObject obj = new JSONObject();
		for(final String attr : modified.keySet()) {
			if(attr.equals("categories")) {
				obj.put(attr, new JSONArray(modified.get(attr)));
			} else {
				obj.put(attr, modified.get(attr));
			}
		}
		return obj;
	}

	
	public Response update(final WebConversation webConv, final String hostname, final String sessionId, final int id, final long timestamp, final Map<String,String> modified) throws MalformedURLException, IOException, SAXException, JSONException {
		final StringBuffer url = getUrl(sessionId,"update", hostname);
		url.append("&id=");
		url.append(id);
		
		url.append("&timestamp=");
		url.append(timestamp);
		final JSONObject obj = toJSONArgs(modified);
		
		return putT(webConv,url.toString(), obj.toString());
	}
	
	public Response update(final WebConversation webConv, final String hostname, final String sessionId, final int id, final long timestamp, final Map<String, String> modified, final File upload, final String contentType) throws MalformedURLException, IOException, SAXException, JSONException {
		final StringBuffer url = getUrl(sessionId,"update", hostname);
		url.append("&id=");
		url.append(id);
		
		url.append("&timestamp=");
		url.append(timestamp);
		
		final PostMethodWebRequest req = new PostMethodWebRequest(url.toString());
		req.setMimeEncoded(true);
		
		final JSONObject obj = toJSONArgs(modified);
		
		req.setParameter("json",obj.toString());
		
		if(upload!=null) {
			req.selectFile("file",upload,contentType);
		}
		final WebResponse resp = webConv.getResource(req);
		final JSONObject res = extractFromCallback(resp.getText());
		return Response.parse(res.toString());
	}
	
	public int createNew(final WebConversation webConv, final String hostname, final String sessionId, final Map<String,String> fields) throws MalformedURLException, IOException, SAXException, JSONException  {
		final StringBuffer url = getUrl(sessionId,"new", hostname);
		final JSONObject obj = toJSONArgs(fields);
		
		final PutMethodWebRequest m = new PutMethodWebRequest(url.toString(), new ByteArrayInputStream(obj.toString().getBytes("UTF-8")),"text/javascript");
		
		final WebResponse resp = webConv.getResponse(m);
		try {
			return new Integer(new JSONObject(resp.getText()).getInt("data"));
		} catch (final JSONException x) {
			throw new JSONException("Got unexpected answer: "+resp.getText());
		}
	}
	
	public int createNew(final WebConversation webConv, final String hostname, final String sessionId, final Map<String, String> fields, final File upload, final String contentType) throws MalformedURLException, IOException, SAXException, JSONException {
		final StringBuffer url = getUrl(sessionId,"new", hostname);
		final PostMethodWebRequest req = new PostMethodWebRequest(url.toString());
		req.setMimeEncoded(true);
		
		final JSONObject obj = toJSONArgs(fields);
		
		req.setParameter("json",obj.toString());
		
		if(upload != null) {
			req.selectFile("file",upload,contentType);
		}
		
		final WebResponse resp = webConv.getResource(req);
		
		final String html = resp.getText();
		final JSONObject response = extractFromCallback(html);
		if(response == null) {
			throw new IOException("Didn't receive response");
		}
		if(!"".equals(response.optString("error"))) {
			throw new IOException(response.getString("error"));
		}
		try {
			return response.getInt("data");
		} catch (final JSONException x) {
			throw new JSONException("Got unexpected answer: "+response);
		}
	}
	
	public int saveAs(final WebConversation webConv, final String hostname, final String sessionId, final int folderId, final int attached, final int module, final int attachment, final Map<String,String> fields) throws MalformedURLException, IOException, SAXException, JSONException  {
		final StringBuffer url = getUrl(sessionId,"saveAs", hostname);
		url.append("&folder=");
		url.append(folderId);
		url.append("&attached=");
		url.append(attached);
		url.append("&module=");
		url.append(module);
		url.append("&attachment=");
		url.append(attachment);
		final JSONObject obj = toJSONArgs(fields);
		
		final PutMethodWebRequest m = new PutMethodWebRequest(url.toString(), new ByteArrayInputStream(obj.toString().getBytes()),"text/javascript");
		
		final WebResponse resp = webConv.getResponse(m);
		final Response res = Response.parse(resp.getText());
		if(res.hasError()) {
			throw new JSONException(res.getErrorMessage());
		}
		return (Integer) res.getData();
	}

	public int[] delete(final WebConversation webConv, final String hostname, final String sessionId, final long timestamp, final int[][] ids) throws MalformedURLException, JSONException, IOException, SAXException {
		final StringBuffer url = getUrl(sessionId,"delete", hostname);
		url.append("&timestamp=");
		url.append(timestamp);
		
		
		final StringBuffer data = new StringBuffer("[");
		
		if(ids.length > 0) {
			for(final int[] tuple : ids) {
				data.append("{folder : ");
				data.append(tuple[0]);
				data.append(", id : ");
				data.append(tuple[1]);
				data.append("},");
			}
			data.deleteCharAt(data.length()-1);
		}
		
		data.append("]");
		
		final JSONArray arr = put(webConv, url.toString(), data.toString()).getJSONArray("data");
		final int[] notDeleted = new int[arr.length()];
		
		for(int i = 0; i < arr.length(); i++) {
			notDeleted[i] = arr.getInt(i);
		}
		
		return notDeleted;
	}
	
	public int[] detach(final WebConversation webConv, final String hostname, final String sessionId, final long timestamp, final int objectId, final int[] versions) throws MalformedURLException, JSONException, IOException, SAXException {
		final StringBuffer url = getUrl(sessionId,"detach", hostname);
		url.append("&timestamp=");
		url.append(timestamp);
		url.append("&id=");
		url.append(objectId);
		
		
		final StringBuffer data = new StringBuffer("[");
		
		if(versions.length > 0) {
			for(final int id : versions) {
				data.append(id);
				data.append(",");
			}
			data.deleteCharAt(data.length()-1);
		}
		
		data.append("]");
		
		final String content = putS(webConv, url.toString(), data.toString());
		JSONArray arr = null;
		try{
			arr = new JSONObject(content).getJSONArray("data");
		} catch (final JSONException x) {
			final Response res = Response.parse(content);
			if(res.hasError()) {
				throw new IOException(res.getErrorMessage());
			}
		}
		final int[] notDeleted = new int[arr.length()];
		
		for(int i = 0; i < arr.length(); i++) {
			notDeleted[i] = arr.getInt(i);
		}
		
		return notDeleted;
	}
	
	public Response revert(final WebConversation webConv, final String hostname, final String sessionId, final long timestamp, final int objectId) throws MalformedURLException, JSONException, IOException, SAXException {
		final StringBuffer url = getUrl(sessionId,"revert", hostname);
		url.append("&timestamp=");
		url.append(timestamp);
		url.append("&id=");
		url.append(objectId);
		
		return gT(webConv, url.toString());
	}
	
	public InputStream document(final WebConversation webConv, final String hostname, final String sessionId, final int id) throws HttpException, IOException {
		return document(webConv,hostname,sessionId, id, -1, null);
	}
	
	public InputStream document(final WebConversation webConv, final String hostname, final String sessionId, final int id, final String contentType) throws HttpException, IOException {
		return document(webConv, hostname, sessionId, id, -1, contentType);
	}
	
	public InputStream document(final WebConversation webConv, final String hostname, final String sessionId, final int id, final int version) throws HttpException, IOException{
		return document(webConv,hostname,sessionId,id,version, null);
	}
	
	public InputStream document(final WebConversation webConv, final String hostname, final String sessionId, final int id, final int version, final String contentType) throws HttpException, IOException{
		
		final GetMethodWebRequest m = documentRequest(sessionId, hostname, id, version, contentType);
		final WebResponse resp = webConv.getResource(m);
		
		return resp.getInputStream();
	}
	
	public GetMethodWebRequest documentRequest(final String sessionId, final String hostname, final int id, final int version, String contentType) {
		final StringBuffer url = getUrl(sessionId,"document", hostname);
		url.append("&id="+id);
		if(version!=-1) {
			url.append("&version="+version);
		}
		
		if(null != contentType) {
			contentType = contentType.replaceAll("/", "%2F");
			url.append("&content_type=");
			url.append(contentType);
		}
		return new GetMethodWebRequest(url.toString());
	}
	
	public int copy(final WebConversation webConv, final String hostname, final String sessionId, final int id, final long timestamp, final Map<String, String> modified, final File upload, final String contentType) throws JSONException, IOException {
		final StringBuffer url = getUrl(sessionId,"copy", hostname);
		url.append("&id=");
		url.append(id);
		
		url.append("&timestamp=");
		url.append(timestamp);
		
		final PostMethodWebRequest req = new PostMethodWebRequest(url.toString());
		req.setMimeEncoded(true);
		
		final JSONObject obj = new JSONObject();
		for(final String attr : modified.keySet()) {
			obj.put(attr, modified.get(attr));
		}
		
		req.setParameter("json",obj.toString());
		
		if(upload!=null) {
			req.selectFile("file",upload,contentType);
		}
		final WebResponse resp = webConv.getResource(req);
		final JSONObject res = extractFromCallback(resp.getText());
		return (Integer) Response.parse(res.toString()).getData();
	}
	
	public int copy(final WebConversation webConv, final String hostname, final String sessionId, final int id, final long timestamp, final Map<String, String> modified) throws MalformedURLException, JSONException, IOException, SAXException {
		final StringBuffer url = getUrl(sessionId,"copy", hostname);
		url.append("&id=");
		url.append(id);
		
		url.append("&timestamp=");
		url.append(timestamp);
		final JSONObject obj = new JSONObject();
		for(final String attr : modified.keySet()) {
			obj.put(attr, modified.get(attr));
		}
		
		final Response res = putT(webConv,url.toString(), obj.toString());
		if(res.hasError()) {
			throw new JSONException(res.getErrorMessage());
		}
		return (Integer) res.getData();
	}
	
	public Response lock(final WebConversation webConv, final String hostname, final String sessionId, final int id, final long timeDiff) throws MalformedURLException, JSONException, IOException, SAXException {
		final StringBuffer url = getUrl(sessionId,"lock", hostname);
		url.append("&id=");
		url.append(id);
		if(timeDiff > 0) {
			url.append("&diff=");
			url.append(timeDiff);
		}
		
		return gT(webConv, url.toString());
	}
	
	public Response lock(final WebConversation webConv, final String hostname, final String sessionId, final int id) throws MalformedURLException, JSONException, IOException, SAXException {
		return lock(webConv,hostname,sessionId, id, -1);
	}
	
	public Response unlock(final WebConversation webConv, final String hostname, final String sessionId, final int id ) throws MalformedURLException, JSONException, IOException, SAXException {
		final StringBuffer url = getUrl(sessionId,"unlock", hostname);
		url.append("&id=");
		url.append(id);
		
		return gT(webConv, url.toString());
	}
	
	public Response search(final WebConversation webConv, final String hostname, final String sessionId, final String query, final int[] columns) throws MalformedURLException, JSONException, IOException, SAXException {
		return search(webConv,hostname,sessionId,query,columns,-1,-1,null, -1, -1);
	}
	
	public Response search(final WebConversation webConv, final String hostname, final String sessionId, final String query, final int[] columns, final int folderId, final int sort, final String order, final int start, final int end) throws MalformedURLException, JSONException, IOException, SAXException {
		final StringBuffer url = getUrl(sessionId,"search", hostname);
		url.append("&columns=");
		for(final int c : columns) {
			url.append(c);
			url.append(",");
		}
		url.setLength(url.length()-1);
		if(folderId != -1) {
			url.append("&folder=");
			url.append(folderId);
		}
		
		if(sort != -1) {
			url.append("&sort=");
			url.append(sort);
			
			url.append("&order=");
			url.append(order);
			
			if(start != -1) {
				url.append("&start=");
				url.append(start);
			}
			
			if(end != -1) {
				url.append("&end=");
				url.append(end);
			}
		}
		final JSONObject queryObject = new JSONObject();
		queryObject.put("pattern",query);
		return putT(webConv,url.toString(), queryObject.toString());
	}
	
	public Response search(final WebConversation webConv, final String hostname, final String sessionId, final String query, final int[] columns, final int folderId, final int sort, final String order, final int limit) throws MalformedURLException, JSONException, IOException, SAXException {
		final StringBuffer url = getUrl(sessionId,"search", hostname);
		url.append("&columns=");
		for(final int c : columns) {
			url.append(c);
			url.append(",");
		}
		url.setLength(url.length()-1);
		if(folderId != -1) {
			url.append("&folder=");
			url.append(folderId);
		}
		
		if(sort != -1) {
			url.append("&sort=");
			url.append(sort);
			
			url.append("&order=");
			url.append(order);
			
			url.append("&limit=");
			url.append(limit);
		}
		final JSONObject queryObject = new JSONObject();
		queryObject.put("pattern",query);
		return putT(webConv,url.toString(), queryObject.toString());
	}
	
	protected StringBuffer getUrl(final String sessionId, final String action, final String hostname) {
		final StringBuffer url = new StringBuffer("http://");
		url.append((hostname == null) ? getHostName() : hostname );
		url.append("/ajax/infostore?session=");
		url.append(sessionId);
		url.append("&action=");
		url.append(action);
		return url;
	}

	@Override
	public String getHostName() {
		if(null == hostName) {
			return super.getHostName();
		}
		return hostName;
	}
	
	public void setHostName(final String hostName) {
		this.hostName = hostName;
	}

}
