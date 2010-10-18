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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.ajax.folder;

import java.util.Iterator;
import com.openexchange.ajax.folder.actions.API;
import com.openexchange.ajax.folder.actions.DeleteRequest;
import com.openexchange.ajax.folder.actions.InsertRequest;
import com.openexchange.ajax.folder.actions.InsertResponse;
import com.openexchange.ajax.folder.actions.ListRequest;
import com.openexchange.ajax.folder.actions.ListResponse;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.groupware.container.FolderObject;

/**
 * @author <a href="mailto:steffen.templin@open-xchange.com>Steffen Templin</a>
 */
public class Bug16899Test extends AbstractAJAXSession {

    private AJAXClient client;

    /**
     * Initializes a new {@link Bug16899Test}.
     * 
     * @param name name of the test.
     */
    public Bug16899Test(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        client = getClient();
    }

    public void testBug16899() throws Exception {
        FolderObject folder = Create.createPrivateFolder("Bug 16899 Test", FolderObject.MAIL, client.getValues().getUserId());
        folder.setFullName("default0/INBOX/Bug 16899 Test");
        InsertRequest insertFolder = new InsertRequest(API.OX_OLD, folder);
        InsertResponse execute = client.execute(insertFolder);

        execute.fillObject(folder);

        String inbox = client.getValues().getInboxFolder();
        ListRequest request = new ListRequest(API.OUTLOOK, inbox);
        ListResponse response = client.execute(request);
        Iterator<FolderObject> iter = response.getFolder();
        boolean found = false;
        System.out.println("First ListRequest:");
        while (iter.hasNext()) {
            final FolderObject fo = iter.next();
            System.out.println(fo.getFullName());
            if (fo.containsFullName() && fo.getFullName().equals(folder.getFullName())) {            	
                found = true;
                break;
            }
        }
        assertTrue("Testfolder not found in inbox.", found);
        
        System.out.println("Delete request");
        DeleteRequest deleteFolder = new DeleteRequest(API.OX_OLD, folder);
        client.execute(deleteFolder);

        request = new ListRequest(API.OUTLOOK, inbox, FolderObject.ALL_COLUMNS, false, false);
        response = client.execute(request);

        iter = response.getFolder();
        found = false;
        System.out.println("Second ListRequest:");
        while (iter.hasNext()) {
            final FolderObject fo = iter.next();
            System.out.println(fo.getFullName());
            if (fo.containsFullName() && fo.getFullName().equals(inbox)) {
                found = true;
                break;
            }
        }
        assertFalse("Testfolder was not deleted.", found);
        assertNull("Error during ListRequest.", response.getException());
        
        System.out.println("finished");
    }

}
