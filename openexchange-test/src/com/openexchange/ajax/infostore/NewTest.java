package com.openexchange.ajax.infostore;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.json.JSONObject;

import com.openexchange.ajax.FolderTest;
import com.openexchange.ajax.InfostoreAJAXTest;
import com.openexchange.ajax.container.Response;
import com.openexchange.groupware.Init;


public class NewTest extends InfostoreAJAXTest {

	public static final int SIZE = 15; // Size of the large file in Megabytes
	
	private static final byte[] megabyte = new byte[1000000];
	
	public NewTest(String name) {
		super(name);
	}
	
	public void testNothing(){
		assertTrue(true);
	}
	
	public void testUpload() throws Exception{
		File upload = new File(Init.getTestProperty("ajaxPropertiesFile"));
		int id = createNew(
				getWebConversation(),
				getHostName(),
				sessionId,
				m(
						"folder_id" 		,	((Integer)folderId).toString(),
						"title"  		,  	"test upload",
						"description" 	, 	"test upload description"
				), upload, "text/plain"
		);
		clean.add(id);
		
		Response res = get(getWebConversation(),getHostName(), sessionId, id);
		JSONObject obj = (JSONObject) res.getData();
		
		assertEquals("test upload",obj.getString("title"));
		assertEquals("test upload description",obj.getString("description"));
		assertEquals(1,obj.getInt("version"));
		assertEquals("text/plain",obj.getString("file_mimetype"));
		assertEquals(upload.getName(),obj.getString("filename"));

		
		InputStream is = null;
		InputStream is2 = null;
		try {
			is = new FileInputStream(upload);
			is2 = document(getWebConversation(),getHostName(),sessionId, id, 1);
			
			//BufferedReader r = new BufferedReader(new InputStreamReader(is2));
			//String line = null;
			//while((line=r.readLine())!=null){
			//	System.out.println(line);
			//}
			
			assertSameContent(is,is2);
		} finally {
			if(is!=null)
				is.close();
			if(is2!=null)
				is2.close();
		}
		
		id = createNew(
			getWebConversation(),
			getHostName(),
			sessionId,
			m(
					"folder_id" 		,	((Integer)folderId).toString(),
					"title"  		,  	"test no upload",
					"description" 	, 	"test no upload description"
			), null, ""
		);
		clean.add(id);
		
		res = get(getWebConversation(),getHostName(), sessionId, id);
		obj = (JSONObject) res.getData();
		
		assertEquals("test no upload",obj.getString("title"));
		assertEquals("test no upload description",obj.getString("description"));
		
	}
	
	
	public void testLargeFileUpload() throws Exception{
		File largeFile = File.createTempFile("test","bin");
		largeFile.deleteOnExit();
		
		BufferedOutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(largeFile),1000000);
			for(int i = 0; i < SIZE; i++) {
				out.write(megabyte);
				out.flush();
			}
		} finally {
			if(out != null)
				out.close();
		}
		
		try {
			int id = createNew(
					getWebConversation(),
					getHostName(),
					sessionId,
					m(
							"folder_id" 		,	((Integer)folderId).toString(),
							"title"  		,  	"test large upload",
							"description" 	, 	"test large upload description"
					), largeFile, "text/plain"
			);
			clean.add(id);
			fail("Uploaded Large File and got no error");
		} catch (Exception x) {
			System.out.println(x.getMessage());
			assertTrue(true);
		}
	}
	
	// Bug 3877
	public void testEnforceFolderType() throws Exception {
		int folderId = FolderTest.getStandardCalendarFolder(getWebConversation(), getHostName(), sessionId).getObjectID();
		try {
			int id = createNew(
					getWebConversation(),
					getHostName(),
					sessionId,
					m(
							"folder_id" 		,	((Integer)folderId).toString(),
							"title"  		,  	"Save to Calendar Folder",
							"description" 	, 	"This shouldn't work"
					), null, ""
				);
			clean.add(id);
			fail("Could save infoitem in calendar folder");
		} catch (Exception x) {
			assertTrue(true);
		}
				
	}
	
	// Bug 3928
	public void testVersionCommentForNewDocument() throws Exception {
		File upload = new File(Init.getTestProperty("ajaxPropertiesFile"));
		int id = createNew(
				getWebConversation(),
				getHostName(),
				sessionId,
				m(
						"folder_id" 		,	((Integer)folderId).toString(),
						"title"  		,  	"test upload",
						"description" 	, 	"test upload description",
						"version_comment" , "Version Comment"
				), upload, "text/plain"
		);
		clean.add(id);
		
		Response res = get(getWebConversation(),getHostName(), sessionId, id);
		JSONObject obj = (JSONObject) res.getData();
		assertEquals(1,obj.getInt("version"));
		assertEquals("Version Comment", obj.getString("version_comment"));
	}


}
