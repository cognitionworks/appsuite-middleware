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

package com.openexchange.contactcollector;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import com.openexchange.api2.OXException;
import com.openexchange.contactcollector.osgi.CCServiceRegistry;
import com.openexchange.groupware.Init;
import com.openexchange.groupware.contact.ContactException;
import com.openexchange.groupware.contact.ContactInterface;
import com.openexchange.groupware.contact.ContactInterfaceDiscoveryService;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DataObject;
import com.openexchange.groupware.container.FolderChildObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.search.ContactSearchObject;
import com.openexchange.session.Session;
import com.openexchange.setuptools.TestConfig;
import com.openexchange.setuptools.TestContextToolkit;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIteratorException;
import com.openexchange.tools.oxfolder.OXFolderAccess;

/**
 * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
 */
public class OrderByTest extends TestCase {

    private String user;

    private Context ctx;

    private int userId;

    private Session session;

    private FolderObject contactFolder;

    @Override
    public void setUp() throws Exception {
        Init.startServer();
        final TestConfig config = new TestConfig();
        user = config.getUser();
        
        final int pos = user.indexOf('@');
        final String contextName;
        if (pos == -1) {
            contextName = "defaultcontext";
        } else {
            contextName = user.substring(pos + 1);
            user = user.substring(0, pos);
        }

        final TestContextToolkit tools = new TestContextToolkit();
        ctx = tools.getContextByName(contextName);
        userId = tools.resolveUser(user, ctx);
        session = tools.getSessionForUser(user, ctx);
        contactFolder = getStandardContactFolder();
    }

    public void testOrderByUserfield20() throws Throwable {
        final ContactInterface contactInterface = CCServiceRegistry.getInstance().getService(
            ContactInterfaceDiscoveryService.class).getContactInterfaceProvider(contactFolder.getObjectID(), ctx.getContextId()).newContactInterface(
            session);

        final Contact contact1 = new Contact();
        final Contact contact2 = new Contact();
        final Contact contact3 = new Contact();
        contact1.setParentFolderID(contactFolder.getObjectID());
        contact2.setParentFolderID(contactFolder.getObjectID());
        contact3.setParentFolderID(contactFolder.getObjectID());
        contact1.setSurName("orderbyTest_contact1");
        contact2.setSurName("orderbyTest_contact2");
        contact3.setSurName("orderbyTest_contact3");
        contact1.setEmail1("orderbyTest@contact1.com");
        contact2.setEmail1("orderbyTest@contact2.com");
        contact3.setEmail1("orderbyTest@contact3.com");
        contact1.setUserField20("3");
        contact2.setUserField20("1");
        contact3.setUserField20("2");

        try {
            contactInterface.insertContactObject(contact1);
            contactInterface.insertContactObject(contact2);
            contactInterface.insertContactObject(contact3);

            final ContactSearchObject searchObject = new ContactSearchObject();
            searchObject.setEmailAutoComplete(true);
            searchObject.setDynamicSearchField(new int[] { Contact.EMAIL1, Contact.EMAIL2, Contact.EMAIL3, });
            searchObject.setDynamicSearchFieldValue(new String[] { "orderbyTest", "orderbyTest", "orderbyTest" });
            final int[] columns = new int[] {
                FolderChildObject.FOLDER_ID, DataObject.LAST_MODIFIED, DataObject.OBJECT_ID, Contact.SUR_NAME };
            final SearchIterator<Contact> iterator = contactInterface.getContactsByExtendedSearch(
                searchObject,
                Contact.USERFIELD20,
                "ASC",
                columns);

            final List<String> surnames = new ArrayList<String>();
            while (iterator.hasNext()) {
                Contact foundContact;
                try {
                    foundContact = iterator.next();
                } catch (final SearchIteratorException e) {
                    throw new ContactException(e);
                }
                if (foundContact.getSurName().startsWith("orderbyTest_")) {
                    surnames.add(foundContact.getSurName());
                }
            }
            assertEquals(3, surnames.size());
            assertEquals("Contact on wrong position", "orderbyTest_contact2", surnames.get(0));
            assertEquals("Contact on wrong position", "orderbyTest_contact3", surnames.get(1));
            assertEquals("Contact on wrong position", "orderbyTest_contact1", surnames.get(2));

        } finally {
            contactInterface.deleteContactObject(contact1.getObjectID(), contactFolder.getObjectID(), contact1.getLastModified());
            contactInterface.deleteContactObject(contact2.getObjectID(), contactFolder.getObjectID(), contact2.getLastModified());
            contactInterface.deleteContactObject(contact3.getObjectID(), contactFolder.getObjectID(), contact3.getLastModified());
        }

    }

    private FolderObject getStandardContactFolder() {
        final OXFolderAccess access = new OXFolderAccess(ctx);
        FolderObject fo = null;
        try {
            fo = access.getDefaultFolder(userId, FolderObject.CONTACT);
        } catch (final OXException e) {
            e.printStackTrace();
            return null;
        }
        return fo;
    }
}
