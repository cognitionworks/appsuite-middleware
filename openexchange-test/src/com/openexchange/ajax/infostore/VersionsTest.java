
package com.openexchange.ajax.infostore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;
import org.xml.sax.SAXException;
import com.openexchange.ajax.InfostoreAJAXTest;
import com.openexchange.ajax.container.Response;
import com.openexchange.groupware.infostore.utils.Metadata;
import com.openexchange.test.TestInit;

public class VersionsTest extends InfostoreAJAXTest {

    public VersionsTest() {
        super();
    }

    @Test
    public void testVersions() throws Exception {
        final File upload = new File(TestInit.getTestProperty("ajaxPropertiesFile"));
        Response res = update(getWebConversation(), getHostName(), sessionId, clean.get(0), Long.MAX_VALUE, m("version_comment", "Comment 1"), upload, "text/plain");
        assertNoError(res);
        res = update(getWebConversation(), getHostName(), sessionId, clean.get(0), Long.MAX_VALUE, m("version_comment", "Comment 2"), upload, "text/plain");
        assertNoError(res);
        res = update(getWebConversation(), getHostName(), sessionId, clean.get(0), Long.MAX_VALUE, m("version_comment", "Comment 3"), upload, "text/plain");
        assertNoError(res);

        res = versions(getWebConversation(), getHostName(), sessionId, clean.get(0), new int[] { Metadata.VERSION, Metadata.CURRENT_VERSION });
        assertNoError(res);

        assureVersions(new Integer[] { 1, 2, 3 }, res, 3);

        res = versions(getWebConversation(), getHostName(), sessionId, clean.get(0), new int[] { Metadata.VERSION, Metadata.VERSION_COMMENT });
        assertNoError(res);

        final Map<Integer, String> comments = new HashMap<Integer, String>();
        comments.put(1, "Comment 1");
        comments.put(2, "Comment 2");
        comments.put(3, "Comment 3");

        final JSONArray arrayOfarrays = (JSONArray) res.getData();

        for (int i = 0; i < arrayOfarrays.length(); i++) {
            final JSONArray payload = arrayOfarrays.getJSONArray(i);
            assertEquals(comments.remove(payload.getInt(0)), payload.getString(1));
        }

    }

    // Bug 13627
    @Test
    public void testVersionSorting() throws Exception {
        final File upload = new File(TestInit.getTestProperty("ajaxPropertiesFile"));
        Response res = update(getWebConversation(), getHostName(), sessionId, clean.get(0), Long.MAX_VALUE, m("version_comment", "Comment 1"), upload, "text/plain");
        assertNoError(res);
        res = update(getWebConversation(), getHostName(), sessionId, clean.get(0), Long.MAX_VALUE, m("version_comment", "Comment 2"), upload, "text/plain");
        assertNoError(res);
        res = update(getWebConversation(), getHostName(), sessionId, clean.get(0), Long.MAX_VALUE, m("version_comment", "Comment 3"), upload, "text/plain");
        assertNoError(res);

        res = versions(getWebConversation(), getHostName(), sessionId, clean.get(0), new int[] { Metadata.VERSION, Metadata.CURRENT_VERSION }, Metadata.VERSION, "desc");
        assertNoError(res);

        assureVersions(new Integer[] { 3, 2, 1 }, res, 3);
    }

    @Test
    public void testUniqueVersions() throws Exception {
        final File upload = new File(TestInit.getTestProperty("ajaxPropertiesFile"));
        Response res = update(getWebConversation(), getHostName(), sessionId, clean.get(0), Long.MAX_VALUE, m("version_comment", "Comment 1"), upload, "text/plain");
        assertNoError(res);
        res = update(getWebConversation(), getHostName(), sessionId, clean.get(0), Long.MAX_VALUE, m("version_comment", "Comment 2"), upload, "text/plain");
        assertNoError(res);
        res = update(getWebConversation(), getHostName(), sessionId, clean.get(0), Long.MAX_VALUE, m("version_comment", "Comment 3"), upload, "text/plain");
        assertNoError(res);

        res = versions(getWebConversation(), getHostName(), sessionId, clean.get(0), new int[] { Metadata.VERSION, Metadata.CURRENT_VERSION });
        assertNoError(res);

        assureVersions(new Integer[] { 1, 2, 3 }, res, 3);

        final int[] nd = detach(getWebConversation(), getHostName(), sessionId, res.getTimestamp().getTime(), clean.get(0), new int[] { 3 });
        assertEquals(0, nd.length);

        res = update(getWebConversation(), getHostName(), sessionId, clean.get(0), Long.MAX_VALUE, m("version_comment", "Comment 3"), upload, "text/plain");
        assertNoError(res);

        res = versions(getWebConversation(), getHostName(), sessionId, clean.get(0), new int[] { Metadata.VERSION, Metadata.CURRENT_VERSION });
        assertNoError(res);

        assureVersions(new Integer[] { 1, 2, 4 }, res, 4);

    }

    @Test
    public void testLastModifiedUTC() throws JSONException, IOException, SAXException {
        final File upload = new File(TestInit.getTestProperty("ajaxPropertiesFile"));
        Response res = update(getWebConversation(), getHostName(), sessionId, clean.get(0), Long.MAX_VALUE, m("version_comment", "Comment 1"), upload, "text/plain");
        assertNoError(res);
        res = update(getWebConversation(), getHostName(), sessionId, clean.get(0), Long.MAX_VALUE, m("version_comment", "Comment 2"), upload, "text/plain");
        assertNoError(res);
        res = update(getWebConversation(), getHostName(), sessionId, clean.get(0), Long.MAX_VALUE, m("version_comment", "Comment 3"), upload, "text/plain");
        assertNoError(res);

        res = versions(getWebConversation(), getHostName(), sessionId, clean.get(0), new int[] { Metadata.LAST_MODIFIED_UTC });
        assertNoError(res);

        final JSONArray arr = (JSONArray) res.getData();
        final int size = arr.length();
        assertTrue(size > 0);

        for (int i = 0; i < size; i++) {
            final JSONArray row = arr.optJSONArray(i);
            assertTrue(row.length() == 1);
            assertNotNull(row.optLong(0));
        }

    }

    public static final void assureVersions(final Integer[] ids, final Response res, final Integer current) throws JSONException {
        final Set<Integer> versions = new HashSet<Integer>(Arrays.asList(ids));
        final JSONArray arrayOfarrays = (JSONArray) res.getData();

        int numberOfVersions = versions.size();
        for (int i = 0; i < arrayOfarrays.length(); i++) {
            final JSONArray comp = arrayOfarrays.getJSONArray(i);
            assertTrue("Didn't expect " + comp.getInt(0), versions.remove(comp.getInt(0)));
            if (current != null && comp.getInt(0) != current) {
                assertFalse(comp.getBoolean(1));
            } else if (current != null) {
                assertTrue(comp.getBoolean(1));
            }
        }
        assertEquals(numberOfVersions, arrayOfarrays.length());
        assertTrue(versions.isEmpty());
    }
}
