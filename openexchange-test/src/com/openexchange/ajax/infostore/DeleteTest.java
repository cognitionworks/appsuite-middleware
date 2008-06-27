package com.openexchange.ajax.infostore;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.xml.sax.SAXException;

import com.openexchange.ajax.InfostoreAJAXTest;
import com.openexchange.ajax.container.Response;
import com.openexchange.groupware.infostore.utils.Metadata;

public class DeleteTest extends InfostoreAJAXTest {

	public DeleteTest(final String name) {
		super(name);
	}
	
	public void testBasic() throws Exception{
		tearDown();
		
		final Response res = this.all(getWebConversation(),getHostName(),sessionId, folderId, new int[]{Metadata.ID});
		
		assertNoError(res);
		
		final JSONArray a = (JSONArray) res.getData();
		
		assertEquals(0, a.length());
		
		clean.clear();
	}
	
	public void testConflict() throws Exception{
	
		final int[][] toDelete = new int[clean.size()][2];
		
		for(int i = 0; i < toDelete.length; i++) {
			toDelete[i][0] = folderId; 
			toDelete[i][1] = clean.get(i);
		}
		
		int[] notDeleted = delete(getWebConversation(),getHostName(),sessionId, 0, toDelete);
		assertEquals(toDelete.length,notDeleted.length);
		
		Set<Integer> notDeletedExpect = new HashSet<Integer>(clean);
		
		for(final int i : notDeleted) {
			assertTrue(notDeletedExpect.remove(i));
		}
		assertTrue(notDeletedExpect.isEmpty());
		
		notDeletedExpect = new HashSet<Integer>(clean);
		tearDown();
		clean.clear();
		
		notDeleted = delete(getWebConversation(),getHostName(),sessionId, 0, toDelete);
		assertEquals(toDelete.length,notDeleted.length);
		
		
		for(final int i : notDeleted) {
			assertTrue(notDeletedExpect.remove(i));
		}
		assertTrue(notDeletedExpect.isEmpty());
		
	}
	

    public void testDeleteSingle() throws JSONException, IOException, SAXException {
        final int[] notDeleted = deleteSingle(getWebConversation(), getHostName(), sessionId, System.currentTimeMillis(), folderId, clean.get(clean.size()-1));
        assertEquals(0, notDeleted.length);
        
    }

}
