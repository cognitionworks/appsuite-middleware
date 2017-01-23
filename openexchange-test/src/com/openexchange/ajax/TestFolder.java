
package com.openexchange.ajax;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.openexchange.groupware.container.FolderObject;

public class TestFolder {

    private String sessionId = null;

    private WebConversation wc = null;

    private WebRequest req = null;

    private WebResponse resp = null;

    @Before
    public void setUp() throws Exception {
        wc = new WebConversation();
        sessionId = LoginTest.getSessionId(wc, "127.0.0.1", "marcus", "netline");
    }

    @Test
    public void testGetRootFolders() throws IOException, SAXException {
        req = new GetMethodWebRequest("http://127.0.0.1/ajax/folders?session=" + sessionId);
        resp = wc.getResponse(req);
        assertFalse(resp.getText().indexOf("error") > -1);
    }

    @Test
    public void testGetSubfolders() throws IOException, SAXException {
        req = new GetMethodWebRequest("http://127.0.0.1/ajax/folders?session=" + sessionId + "&gparent=127");
        resp = wc.getResponse(req);
        assertFalse(resp.getText().indexOf("error") > -1);
        req = new GetMethodWebRequest("http://127.0.0.1/ajax/folders?session=" + sessionId + "&gparent=INBOX");
        resp = wc.getResponse(req);
        assertFalse(resp.getText().indexOf("error") > -1);
    }

    @Test
    public void testGetFolder() throws IOException, SAXException {
        req = new GetMethodWebRequest("http://127.0.0.1/ajax/folders?session=" + sessionId + "&id=290");
        resp = wc.getResponse(req);
        assertFalse(resp.getText().indexOf("error") > -1);
        req = new GetMethodWebRequest("http://127.0.0.1/ajax/folders?session=" + sessionId + "&id=INBOX");
        resp = wc.getResponse(req);
        assertFalse(resp.getText().indexOf("error") > -1);
    }

    @Test
    public void testCreateUpdateDeleteFolder() throws IOException, SAXException {
        final String oxFolderName = "CalendarFolder07";
        final String mailFolderName = "MyMailFolder25";
        final String newOXFolderObj = "{\"folder_id\":65,\"permissions\":[{\"group\":false,\"bits\":403710016,\"entity\":140}],\"title\":\"" + oxFolderName + "\",\"module\":2}";
        final String newMailFolderObj = "{\"folder_id\":\"INBOX\",\"permissions\":[{\"group\":false,\"bits\":\"lrswipcda\",\"entity\":140}],\"title\":\"" + mailFolderName + "\",\"module\":" + FolderObject.MAIL + "}";
        byte[] bytes;
        final String expectedInsertPrefix = "{OK: Folder successfully inserted";
        int oxFolderId = -1;
        String mailFolderFullName = null;
        /*
         * Insert OX Folder
         */
        bytes = newOXFolderObj.getBytes(com.openexchange.java.Charsets.UTF_8);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        req = new PutMethodWebRequest("http://127.0.0.1/ajax/folders?session=" + sessionId + "&folder=65", bais, "text/javascript; charset=UTF-8");
        resp = wc.getResponse(req);
        if (resp.getText().indexOf('(') > -1) {
            oxFolderId = Integer.parseInt(resp.getText().substring(resp.getText().indexOf('(') + 1, resp.getText().indexOf(')')));
        }
        assertTrue(resp.getText().startsWith(expectedInsertPrefix));
        /*
         * Insert Mail Folder
         */
        bytes = newMailFolderObj.getBytes(com.openexchange.java.Charsets.UTF_8);
        bais = new ByteArrayInputStream(bytes);
        req = new PutMethodWebRequest("http://127.0.0.1/ajax/folders?session=" + sessionId + "&folder=INBOX", bais, "text/javascript; charset=UTF-8");
        resp = wc.getResponse(req);
        if (resp.getText().indexOf('(') > -1) {
            mailFolderFullName = resp.getText().substring(resp.getText().indexOf('(') + 1, resp.getText().indexOf(')'));
        }
        assertTrue(resp.getText().startsWith(expectedInsertPrefix));
        final String updateOXFolderObj = "{\"folder_id\":65,\"permissions\":[{\"group\":false,\"bits\":403710016,\"entity\":140}],\"title\":\"" + oxFolderName + "_Changed\",\"module\":2}";
        final String updateMailFolderObj = "{\"folder_id\":\"INBOX\",\"permissions\":[{\"group\":false,\"bits\":\"lrswipcda\",\"entity\":140}],\"title\":\"" + mailFolderName + "Changed\",\"module\":" + FolderObject.MAIL + "}";
        final String expectedUpdatePrefix = "{OK: Folder successfully updated";
        /*
         * Update OX folder
         */
        bytes = updateOXFolderObj.getBytes(com.openexchange.java.Charsets.UTF_8);
        bais = new ByteArrayInputStream(bytes);
        req = new PutMethodWebRequest("http://127.0.0.1/ajax/folders?session=" + sessionId + "&id=" + oxFolderId, bais, "text/javascript; charset=UTF-8");
        resp = wc.getResponse(req);
        assertTrue(resp.getText().startsWith(expectedUpdatePrefix));
        /*
         * Update IMAP Folder
         */
        bytes = updateMailFolderObj.getBytes(com.openexchange.java.Charsets.UTF_8);
        bais = new ByteArrayInputStream(bytes);
        req = new PutMethodWebRequest("http://127.0.0.1/ajax/folders?session=" + sessionId + "&id=" + mailFolderFullName, bais, "text/javascript; charset=UTF-8");
        resp = wc.getResponse(req);
        assertTrue(resp.getText().startsWith(expectedUpdatePrefix));
        /*
         * Delete OX folder
         */
        final String expectedDeletePrefix = "{OK: Folder successfully deleted";
        bytes = "".getBytes(com.openexchange.java.Charsets.UTF_8);
        bais = new ByteArrayInputStream(bytes);
        req = new PostMethodWebRequest("http://127.0.0.1/ajax/folders?session=" + sessionId + "&delete=" + oxFolderId, bais, "text/javascript; charset=UTF-8");
        resp = wc.getResponse(req);
        assertTrue(resp.getText().startsWith(expectedDeletePrefix));
        /*
         * Delete IMAP folder
         */
        mailFolderFullName += "Changed";
        bytes = "".getBytes(com.openexchange.java.Charsets.UTF_8);
        bais = new ByteArrayInputStream(bytes);
        req = new PostMethodWebRequest("http://127.0.0.1/ajax/folders?session=" + sessionId + "&delete=" + mailFolderFullName, bais, "text/javascript; charset=UTF-8");
        resp = wc.getResponse(req);
        assertTrue(resp.getText().startsWith(expectedDeletePrefix));
    }

}
