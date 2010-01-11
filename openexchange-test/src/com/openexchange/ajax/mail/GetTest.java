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

package com.openexchange.ajax.mail;

import org.json.JSONArray;
import org.json.JSONObject;
import com.openexchange.ajax.framework.Executor;
import com.openexchange.ajax.mail.actions.GetRequest;
import com.openexchange.ajax.mail.actions.GetResponse;
import com.openexchange.ajax.mail.actions.SendRequest;

/**
 * {@link GetTest}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class GetTest extends AbstractMailTest {

    /**
     * Default constructor.
     * 
     * @param name Name of this test.
     */
    public GetTest(final String name) {
        super(name);
    }

    /**
     * Tests the <code>action=get</code> request on INBOX folder
     * 
     * @throws Throwable
     */
    public void testGet() throws Throwable {
        /*
         * Clean everything
         */
        clearFolder(getInboxFolder());
        clearFolder(getSentFolder());
        clearFolder(getTrashFolder());
        /*
         * Create JSON mail object
         */
        final String mailObject_25kb = createSelfAddressed25KBMailObject().toString();
        /*
         * Insert mail through a send request
         */
        final String[] folderAndID = (Executor.execute(getSession(), new SendRequest(mailObject_25kb))).getFolderAndID();
        /*
         * Perform action=get
         */
        final GetResponse response = Executor.execute(getSession(), new GetRequest(folderAndID[0], folderAndID[1]));
        assertTrue("", response.getMail(getTimeZone()) != null);
        /*
         * Clean everything
         */
        clearFolder(getInboxFolder());
        clearFolder(getSentFolder());
        clearFolder(getTrashFolder());
    }

    /**
     * Tests the <code>action=get_structure</code> request on INBOX folder
     * 
     * @throws Throwable
     */
    public void testGetStructure() throws Throwable {
        /*
         * Clean everything
         */
        clearFolder(getInboxFolder());
        clearFolder(getSentFolder());
        clearFolder(getTrashFolder());
        /*
         * Create JSON mail object
         */
        final String mailObject_25kb = createSelfAddressed25KBMailObject().toString();
        /*
         * Insert mail through a send request
         */
        final String[] folderAndID = (Executor.execute(getSession(), new SendRequest(mailObject_25kb))).getFolderAndID();
        /*
         * Perform action=get
         */
        final GetResponse response = Executor.execute(getSession(), new GetRequest(folderAndID[0], folderAndID[1]).setStructure(true));

        final JSONObject obj = (JSONObject) response.getData();
        assertNotNull("Structured JSON mail object is null, but shouldn't", obj);

        final JSONArray bodyArray = obj.getJSONArray("body");
        final int length = bodyArray.length();
        assertTrue("Expected two body parts, but wasn't.", length > 0);

        /*
         * Clean everything
         */
        clearFolder(getInboxFolder());
        clearFolder(getSentFolder());
        clearFolder(getTrashFolder());
    }

}
