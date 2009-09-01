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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.ajax.importexport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.json.JSONException;
import org.xml.sax.SAXException;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXSession;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.importexport.ImportResult;
import com.openexchange.groupware.modules.Module;
import com.openexchange.test.FolderTestManager;
import com.openexchange.test.TestException;
import com.openexchange.webdav.xml.ContactTest;

/**
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 */
public class VCardImportTest extends AbstractVCardTest {

    final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private FolderTestManager folderManager;

    private AJAXClient client;

    private FolderObject testFolder;

    public VCardImportTest(final String name) throws Exception {
        super(name);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        client = new AJAXClient(new AJAXSession(getWebConversation(), getSessionId()));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        folderManager = new FolderTestManager(client);
        testFolder = folderManager.generateFolder(
            "VCard Interface Tests",
            Module.CONTACTS.getFolderConstant(),
            client.getValues().getPrivateContactFolder(),
            client.getValues().getUserId());
        folderManager.insertFolderOnServer(testFolder);
        contactFolderId = testFolder.getObjectID();
    }

    @Override
    protected void tearDown() throws Exception {
        folderManager.cleanUp();
        super.tearDown();
    }

    public void testDummy() throws Exception {

    }

    public void testImportVCard() throws Exception {
        final Contact contactObj = new Contact();
        contactObj.setSurName("testImportVCard" + System.currentTimeMillis());
        contactObj.setGivenName("givenName");
        contactObj.setBirthday(simpleDateFormat.parse("2007-04-04"));

        final ImportResult[] importResult = importVCard(
            getWebConversation(),
            new Contact[] { contactObj },
            contactFolderId,
            timeZone,
            emailaddress,
            getHostName(),
            getSessionId());

        assertEquals("import result size is not 1", 1, importResult.length);
        assertTrue("server errors of server", importResult[0].isCorrect());

        final int objectId = Integer.parseInt(importResult[0].getObjectId());

        assertTrue("object id is 0", objectId > 0);

        final Contact[] contactArray = exportContact(
            getWebConversation(),
            contactFolderId,
            emailaddress,
            timeZone,
            getHostName(),
            getSessionId());

        boolean found = false;

        for (int a = 0; a < contactArray.length; a++) {
            if (contactObj.getSurName().equals(contactArray[a].getSurName())) {
                contactObj.setParentFolderID(contactFolderId);
                ContactTest.compareObject(contactObj, contactArray[a]);

                found = true;
            }
        }

        assertTrue("inserted object not found in response", found);
    }

    public void testImportVCardWithBrokenContact() throws Exception {
        final StringBuffer stringBuffer = new StringBuffer();

        // cont1
        stringBuffer.append("BEGIN:VCARD").append('\n');
        stringBuffer.append("VERSION:3.0").append('\n');
        stringBuffer.append("FN:testImportVCardWithBrokenContact1").append('\n');
        stringBuffer.append("N:testImportVCardWithBrokenContact1;givenName;;;").append('\n');
        stringBuffer.append("BDAY:20070404").append('\n');
        stringBuffer.append("END:VCARD").append('\n');

        // cont2
        stringBuffer.append("BEGIN:VCARD").append('\n');
        stringBuffer.append("VERSION:3.0").append('\n');
        stringBuffer.append("FN:testImportVCardWithBrokenContact2").append('\n');
        stringBuffer.append("N:testImportVCardWithBrokenContact2;givenName;;;").append('\n');
        stringBuffer.append("BDAY:INVALID_DATE").append('\n');
        stringBuffer.append("END:VCARD").append('\n');

        // cont3
        stringBuffer.append("BEGIN:VCARD").append('\n');
        stringBuffer.append("VERSION:3.0").append('\n');
        stringBuffer.append("FN:testImportVCardWithBrokenContact3").append('\n');
        stringBuffer.append("N:testImportVCardWithBrokenContact3;givenName;;;").append('\n');
        stringBuffer.append("BDAY:20070404").append('\n');
        stringBuffer.append("END:VCARD").append('\n');

        final ImportResult[] importResult = importVCard(
            getWebConversation(),
            new ByteArrayInputStream(stringBuffer.toString().getBytes()),
            contactFolderId,
            timeZone,
            emailaddress,
            getHostName(),
            getSessionId());

        assertEquals("invalid import result array size", 3, importResult.length);

        assertTrue("server errors of server", importResult[0].isCorrect());
        assertTrue("server errors of server", importResult[1].hasError());
        assertTrue("server errors of server", importResult[2].isCorrect());
    }

    public void test6823() throws TestException, IOException, SAXException, JSONException, Exception {
        final String vcard = "BEGIN:VCARD\n" +
        		"VERSION:3.0\n" +
        		"N:;Svetlana;;;\n" +
        		"FN:Svetlana\n" +
        		"TEL;type=CELL;type=pref:6670373\n" +
        		"CATEGORIES:Nicht abgelegt\n" +
        		"X-ABUID:CBC739E8-694E-4589-8651-8C30E1A6E724\\:ABPerson\n" +
        		"END:VCARD";
        final ImportResult[] importResult = importVCard(
            getWebConversation(),
            new ByteArrayInputStream(vcard.getBytes()),
            contactFolderId,
            timeZone,
            emailaddress,
            getHostName(),
            getSessionId());

        assertTrue("Only one import", importResult.length == 1);
        assertFalse("No error?", importResult[0].hasError());
    }

    public void test6962followup() throws TestException, IOException, SAXException, JSONException, Exception {
        final String vcard = "BEGIN:VCARD\n" +
        		"VERSION:3.0\n" +
        		"N:;Svetlana;;;\n" +
        		"FN:Svetlana\n" +
        		"TEL;type=CELL;type=pref:673730\n" +
        		"END:VCARD\n" +
        		"BEGIN:VCARD\n" +
        		"VERSION:666\n" +
        		"N:;Svetlana;;;\n" +
        		"FN:Svetlana\n" +
        		"TEL;type=CELL;type=pref:6670373\n" +
        		"END:VCARD";
        final ImportResult[] importResult = importVCard(
            getWebConversation(),
            new ByteArrayInputStream(vcard.getBytes()),
            contactFolderId,
            timeZone,
            emailaddress,
            getHostName(),
            getSessionId());

        assertTrue("Two import attempts", importResult.length == 2);
        assertFalse("No error on first attempt?", importResult[0].hasError());
        assertTrue("Error on second attempt?", importResult[1].hasError());
        final AbstractOXException ex = importResult[1].getException();

        // following line was removed since test environment cannot relay correct error messages from server
        // assertEquals("Correct error code?", "I_E-0605",ex.getErrorCode());
    }

    public void test7106() throws TestException, IOException, SAXException, JSONException, Exception {
        final String vcard = "BEGIN:VCARD\n" +
        		"VERSION:3.0\n" +
        		"N:;H\u00fcb\u00fcrt;;;\n" +
        		"FN:H\u00fcb\u00fcrt S\u00f6nderzeich\u00f6n\n" +
        		"TEL;type=CELL;type=pref:6670373\n" +
        		"END:VCARD\n";
        final ImportResult[] importResult = importVCard(
            getWebConversation(),
            new ByteArrayInputStream(vcard.getBytes("UTF-8")),
            contactFolderId,
            timeZone,
            emailaddress,
            getHostName(),
            getSessionId());

        assertFalse("Worked?", importResult[0].hasError());
        final int contactId = Integer.parseInt(importResult[0].getObjectId());
        final Contact myImport = ContactTest.loadContact(
            getWebConversation(),
            contactId,
            contactFolderId,
            getHostName(),
            getLogin(),
            getPassword());
        assertEquals("Checking surname:", "H\u00fcb\u00fcrt S\u00f6nderzeich\u00f6n", myImport.getDisplayName());
    }

    /**
     * Deals with N and ADR properties and different amount of semicola used. Also tests an input stream with no terminating newline
     */
    public void test7248() throws TestException, IOException, SAXException, JSONException, Exception {
        final String vcard = "BEGIN:VCARD\n" +
        		"VERSION:2.1\n" +
        		"N:Colombara;Robert\n" +
        		"FN:Robert Colombara\n" +
        		"ADR;WORK:;;;;;;DE\n" +
        		"ADR;HOME:;;;;;- / -\n" +
        		"END:VCARD";
        final ImportResult[] importResult = importVCard(
            getWebConversation(),
            new ByteArrayInputStream(vcard.getBytes()),
            contactFolderId,
            timeZone,
            emailaddress,
            getHostName(),
            getSessionId());
        assertTrue("Only one import?", importResult.length == 1);
        assertFalse("Import worked?", importResult[0].hasError());

        final int contactId = Integer.parseInt(importResult[0].getObjectId());
        final Contact myImport = ContactTest.loadContact(
            getWebConversation(),
            contactId,
            contactFolderId,
            getHostName(),
            getLogin(),
            getPassword());
        assertEquals("Checking surname:", "Colombara", myImport.getSurName());
    }

    /**
     * Deals with broken E-Mail adresses as encountered in Resources in the example file
     */
    public void test7249() throws TestException, IOException, SAXException, JSONException, Exception {
        final String vcard = "BEGIN:VCARD\n" +
        		"VERSION:2.1\n" +
        		"FN:Conference_Room_Olpe\n" +
        		"EMAIL;PREF;INTERNET:Conference_Room_Olpe_EMAIL\n" +
        		"END:VCARD";
        final ImportResult[] importResult = importVCard(
            getWebConversation(),
            new ByteArrayInputStream(vcard.getBytes()),
            contactFolderId,
            timeZone,
            emailaddress,
            getHostName(),
            getSessionId());

        assertFalse("Worked?", importResult[0].hasError());
        final int contactId = Integer.parseInt(importResult[0].getObjectId());
        final Contact myImport = ContactTest.loadContact(
            getWebConversation(),
            contactId,
            contactFolderId,
            getHostName(),
            getLogin(),
            getPassword());
        assertEquals("Checking surname:", "Conference_Room_Olpe", myImport.getDisplayName());
        assertEquals("Checking email1 (must be null):", null, myImport.getEmail1());
        assertEquals("Checking email2 (must be null):", null, myImport.getEmail2());
        assertEquals("Checking email3 (must be null):", null, myImport.getEmail3());
    }

    /**
     * Deals with umlauts
     */
    public void test7250() throws TestException, IOException, SAXException, JSONException, Exception {
        final String vcard = "BEGIN:VCARD\n" +
        		"VERSION:2.1\n" +
        		"N;CHARSET=Windows-1252:B\u00f6rnig;Anke;;;\n" +
        		"FN;CHARSET=Windows-1252:Anke  B\u00f6rnig\n" +
        		"END:VCARD";
        final ImportResult[] importResult = importVCard(
            getWebConversation(),
            new ByteArrayInputStream(vcard.getBytes("Cp1252")),
            contactFolderId,
            timeZone,
            emailaddress,
            getHostName(),
            getSessionId());

        assertFalse("Worked?", importResult[0].hasError());
        final int contactId = Integer.parseInt(importResult[0].getObjectId());
        final Contact myImport = ContactTest.loadContact(
            getWebConversation(),
            contactId,
            contactFolderId,
            getHostName(),
            getLogin(),
            getPassword());
        assertEquals("Checking surname:", "B\u00f6rnig", myImport.getSurName());
    }

    public void test14350() throws Exception {
        String vcard = "BEGIN:VCARD\n"
            + "VERSION:3.0\n"
            + "PRODID:OPEN-XCHANGE\n"
            + "FN:Prinz\\, Tobias\n"
            + "N:Prinz;Tobias;;;\n"
            + "NICKNAME:Tierlieb\n"
            + "BDAY:19810501\n"
            + "ADR;TYPE=work:;;Broadway 3131 / 5th Ave;T�bingen;Baden-W�rttemberg;57621;Germany\n"
            + "ADR;TYPE=home:;;Testroad 4711;Port de la V�rde;Skol-upon-sea;37542;France\n"
            + "ORG:- deactivated -\n" + "REV:20061204T160750.018Z\n"
            + "UID:80@ox6.netline.de\n"
            + "END:VCARD\n";
        final ImportResult[] importResult = importVCard(
            getWebConversation(),
            new ByteArrayInputStream(vcard.getBytes("Cp1252")),
            testFolder.getObjectID(),
            timeZone,
            emailaddress,
            getHostName(),
            getSessionId());

        assertFalse("Worked?", importResult[0].hasError());

        final int contactId = Integer.parseInt(importResult[0].getObjectId());
        final Contact actual = ContactTest.loadContact(
            getWebConversation(),
            contactId,
            testFolder.getObjectID(),
            getHostName(),
            getLogin(),
            getPassword());

        assertEquals("Checking name:", "Prinz", actual.getSurName());

        assertEquals("Street, business", "Broadway 3131 / 5th Ave", actual.getStreetBusiness());
        assertEquals("City, business", "T�bingen", actual.getCityBusiness());
        assertEquals("State, business", "Baden-W�rttemberg", actual.getStateBusiness());
        assertEquals("ZIP, business", "57621", actual.getPostalCodeBusiness());
        assertEquals("Country, business", "Germany", actual.getCountryBusiness());

        assertEquals("Street, home", "Testroad 4711", actual.getStreetHome());
        assertEquals("City, home", "Port de la V�rde", actual.getCityHome());
        assertEquals("State, home", "Skol-upon-sea", actual.getStateHome());
        assertEquals("ZIP, home", "37542", actual.getPostalCodeHome());
        assertEquals("Country, home", "France", actual.getCountryHome());
    }
}
