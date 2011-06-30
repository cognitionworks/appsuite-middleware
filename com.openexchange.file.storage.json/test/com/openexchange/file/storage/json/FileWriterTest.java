/*
 *
 *    OPEN-XCHANGE legal information
 *
 *    All intellectual property rights in the Software are protected by
 *    international copyright laws.
 *
 *
 *    In some countries OX, OX Open-Xchange, open xchange and OXtender
 *    as well as the corresponding Logos OX Open-Xchange and OX are registered
 *    trademarks of the Open-Xchange, Inc. group of companies.
 *    The use of the Logos is not covered by the GNU General Public License.
 *    Instead, you are allowed to use these Logos according to the terms and
 *    conditions of the Creative Commons License, Version 2.5, Attribution,
 *    Non-commercial, ShareAlike, and the interpretation of the term
 *    Non-commercial applicable to the aforementioned license is published
 *    on the web site http://www.open-xchange.com/EN/legal/index.html.
 *
 *    Please make sure that third-party modules and libraries are used
 *    according to their respective licenses.
 *
 *    Any modifications to this package must retain all copyright notices
 *    of the original copyright holder(s) for the original code used.
 *
 *    After any such modifications, the original and derivative code shall remain
 *    under the copyright of the copyright holder(s) and/or original author(s)per
 *    the Attribution and Assignment Agreement that can be located at
 *    http://www.open-xchange.com/EN/developer/. The contributing author shall be
 *    given Attribution for the derivative code and a license granting use.
 *
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
 *     Mail: info@open-xchange.com
 *
 *
 *     This program is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License, Version 2 as published
 *     by the Free Software Foundation.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *     or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *     for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc., 59
 *     Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package com.openexchange.file.storage.json;

import java.util.Arrays;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.file.storage.DefaultFile;
import com.openexchange.file.storage.json.FileMetadataWriter;
import com.openexchange.json.JSONAssertion;
import static com.openexchange.json.JSONAssertion.assertValidates;
import static com.openexchange.file.storage.File.Field.*;
import static com.openexchange.time.TimeTools.D;

/**
 * {@link FileWriterTest}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class FileWriterTest extends FileTest {

    FileMetadataWriter writer = new FileMetadataWriter();

    public void testWriteFileAsArray() throws JSONException {
        DefaultFile f = createFile();
        
        JSONArray array = writer.writeArray(f, Arrays.asList(
            CATEGORIES,
            COLOR_LABEL,
            CREATED,
            CREATED_BY,
            DESCRIPTION,
            FILE_MD5SUM,
            FILE_MIMETYPE,
            FILENAME,
            FILE_SIZE,
            FOLDER_ID,
            ID,
            CURRENT_VERSION,
            LAST_MODIFIED,
            LAST_MODIFIED_UTC,
            LOCKED_UNTIL,
            MODIFIED_BY,
            NUMBER_OF_VERSIONS,
            TITLE,
            URL,
            VERSION,
            VERSION_COMMENT), null);

        assertNotNull(array);

        assertValidates(new JSONAssertion().isArray().withValues("cat1", "cat2", "cat3").inStrictOrder(), array.getJSONArray(0));
        assertEquals(12, array.getInt(1));
        assertEquals(D("Today at 08:00").getTime(), array.getLong(2));
        assertEquals(3, array.getInt(3));
        assertEquals("description", array.getString(4));
        assertEquals("md5sum", array.getString(5));
        assertEquals("mime/type", array.getString(6));
        assertEquals("name.txt", array.getString(7));
        assertEquals(1337, array.getLong(8));
        assertEquals("folder 3", array.getString(9));
        assertEquals("Id 23", array.getString(10));
        assertEquals(true, array.getBoolean(11));
        assertEquals(D("Today at 10:00").getTime(), array.getLong(12));
        assertEquals(D("Today at 10:00").getTime(), array.getLong(13));
        assertEquals(D("Today at 18:00").getTime(), array.getLong(14));
        assertEquals(22, array.getInt(15));
        assertEquals(2, array.getInt(16));
        assertEquals("Nice Title", array.getString(17));
        assertEquals("url", array.getString(18));
        assertEquals(2, array.getInt(19));
        assertEquals("version comment", array.getString(20));
    }

    private DefaultFile createFile() {
        DefaultFile f = new DefaultFile();
        f.setCategories("cat1, cat2, cat3");
        f.setColorLabel(12);
        f.setCreated(D("Today at 08:00"));
        f.setCreatedBy(3);
        f.setDescription("description");
        f.setFileMD5Sum("md5sum");
        f.setFileMIMEType("mime/type");
        f.setFileName("name.txt");
        f.setFileSize(1337);
        f.setFolderId("folder 3");
        f.setId("Id 23");
        f.setIsCurrentVersion(true);
        f.setLastModified(D("Today at 10:00"));
        f.setLockedUntil(D("Today at 18:00"));
        f.setModifiedBy(22);
        f.setNumberOfVersions(2);
        f.setTitle("Nice Title");
        f.setURL("url");
        f.setVersion(2);
        f.setVersionComment("version comment");
        return f;
    }

    public void testTimezone() throws JSONException {
        TimeZone tz = TimeZone.getTimeZone("GMT-2");
        DefaultFile f = new DefaultFile();
        f.setCreated(D("Today at 10:00"));
        f.setLastModified(D("Today at 12:00"));
        f.setLockedUntil(D("Today at 20:00"));
        
        JSONArray array = writer.writeArray(f, Arrays.asList(
            CREATED,
            LAST_MODIFIED,
            LAST_MODIFIED_UTC,
            LOCKED_UNTIL), tz);

        assertNotNull(array);
        
        assertEquals(D("Today at 08:00").getTime(), array.getLong(0));
        assertEquals(D("Today at 10:00").getTime(), array.getLong(1));
        assertEquals(D("Today at 12:00").getTime(), array.getLong(2)); // Last modified UTC doesn't get the timezone offset
        assertEquals(D("Today at 18:00").getTime(), array.getLong(3));
    }
    
    public void testWriteAsObject() {
        DefaultFile file = createFile();
        
        JSONObject object = writer.write(file, null);
        
        
        assertValidates(new JSONAssertion().isObject()
            .hasKey("categories").withValueArray().withValues("cat1", "cat2", "cat3").inStrictOrder()
            .hasKey("color_label").withValue(12)
            .hasKey("creation_date").withValue(D("Today at 08:00").getTime())
            .hasKey("created_by").withValue(3)
            .hasKey("description").withValue("description")
            .hasKey("file_md5sum").withValue("md5sum")
            .hasKey("file_mimetype").withValue("mime/type")
            .hasKey("file_size").withValue(1337)
            .hasKey("folder_id").withValue("folder 3")
            .hasKey("id").withValue("Id 23")
            .hasKey("current_version").withValue(true)
            .hasKey("last_modified").withValue(D("Today at 10:00").getTime())
            .hasKey("locked_until").withValue(D("Today at 18:00").getTime())
            .hasKey("modified_by").withValue(22)
            .hasKey("number_of_versions").withValue(2)
            .hasKey("title").withValue("Nice Title")
            .hasKey("url").withValue("url")
            .hasKey("version").withValue(2)
            .hasKey("version_comment").withValue("version comment"), object);
        
    }
}
