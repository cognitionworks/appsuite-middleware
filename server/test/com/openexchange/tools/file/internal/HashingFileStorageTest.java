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

package com.openexchange.tools.file.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import com.openexchange.tools.file.external.OXException;
import junit.framework.TestCase;


/**
 * {@link HashingFileStorageTest}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class HashingFileStorageTest extends AbstractHashingFileStorageTest {
  
    
    public void testLifecycle() throws Exception {
        String data = "I am nice data";
        String fileId = fs.saveNewFile( IS(data) );
        
        InputStream file = fs.getFile(fileId);
        InputStream compare = IS(data);
        
        int i = 0;
        while((i = file.read()) != -1) {
            assertEquals(i, compare.read());
        }
        assertEquals(-1, compare.read());
        
        file.close();
        compare.close();
    }
    
    public void testListFiles() throws Exception{
        Map<String, Integer> files = new HashMap<String, Integer>();
        for(int i = 0; i < 10; i++) {
            String data = "I am nice data in the file number "+i;
            String fileId = fs.saveNewFile( IS(data) );
            files.put(fileId, i);
        }
        
        SortedSet<String> fileList = fs.getFileList();
        
        assertEquals(files.size(), fileList.size());
        
        for(Map.Entry<String, Integer> entry : files.entrySet()) {
            assertTrue(entry.getKey()+" missing in: "+fileList, fileList.contains(entry.getKey()));
        }
    }

    public void testRemove() throws Exception {
        Map<String, Integer> files = new HashMap<String, Integer>();
        for(int i = 0; i < 10; i++) {
            String data = "I am nice data in the file number "+i;
            String fileId = fs.saveNewFile( IS(data) );
            files.put(fileId, i);
        }
        
        fs.remove();
        
        String[] list = tmpFile.list();
        assertTrue(list == null || list.length == 0);
    }
    
    // Error Cases
    
    public void testReadUnknownID() {
        try {
            fs.getFile("fantasyName");
            fail("Could read unkown file");
        } catch (OXException e) {
        }
    }
    
    public void testDeleteUnknownID() throws Exception {
        assertFalse(fs.deleteFile("fantasyName"));
    }

    public void testDeleteUnknownIDs() throws Exception {
        String data = "I am nice data";
        String fileId = fs.saveNewFile( IS(data) );
        
        Set<String> notDeleted = fs.deleteFiles(new String[]{"file1", fileId, "file2", "file3"});
        
        assertEquals(3, notDeleted.size());
        assertTrue(notDeleted.contains("file1"));
        assertTrue(notDeleted.contains("file2"));
        assertTrue(notDeleted.contains("file3"));
        
    }
 
    
}
