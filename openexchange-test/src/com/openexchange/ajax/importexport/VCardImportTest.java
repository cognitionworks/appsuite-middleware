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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.xml.sax.SAXException;

import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.container.ContactObject;
import com.openexchange.groupware.importexport.ImportResult;
import com.openexchange.test.TestException;
import com.openexchange.webdav.xml.ContactTest;

public class VCardImportTest extends AbstractVCardTest {
	
	final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	private static final Log LOG = LogFactory.getLog(VCardImportTest.class);
	
	public VCardImportTest(final String name) {
		super(name);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	public void testDummy() throws Exception {
		
	}
	
	public void testImportVCard() throws Exception {
		final ContactObject contactObj = new ContactObject();
		contactObj.setSurName("testImportVCard" + System.currentTimeMillis());
		contactObj.setGivenName("givenName");
		contactObj.setBirthday(simpleDateFormat.parse("2007-04-04"));

		final ImportResult[] importResult = importVCard(getWebConversation(), new ContactObject[]  { contactObj }, contactFolderId, timeZone, emailaddress, getHostName(), getSessionId());
		
		assertEquals("import result size is not 1", 1, importResult.length);
		assertTrue("server errors of server", importResult[0].isCorrect());
		
		final int objectId = Integer.parseInt(importResult[0].getObjectId());
		
		assertTrue("object id is 0", objectId > 0);
		
		final ContactObject[] contactArray = exportContact(getWebConversation(), contactFolderId, emailaddress, timeZone, getHostName(), getSessionId());
		
		boolean found = false;
		
		for (int a = 0; a < contactArray.length; a++) {
			//System.out.println("surname: " + contactArray[a].getSurName() + " == " + contactObj.getSurName());
			if (contactObj.getSurName().equals(contactArray[a].getSurName()) ) {
				contactObj.setParentFolderID(contactFolderId);
				ContactTest.compareObject(contactObj, contactArray[a]);
				
				found = true;
			}
		}
		
		assertTrue("inserted object not found in response", found);
		
		ContactTest.deleteContact(getWebConversation(), Integer.parseInt(importResult[0].getObjectId()), contactFolderId, getHostName(), getLogin(), getPassword());
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
		
		final ImportResult[] importResult = importVCard(getWebConversation(), new ByteArrayInputStream(stringBuffer.toString().getBytes()), contactFolderId, timeZone, emailaddress, getHostName(), getSessionId());
		
		assertEquals("invalid import result array size", 3, importResult.length);
		
		assertTrue("server errors of server", importResult[0].isCorrect());
		assertTrue("server errors of server", importResult[1].hasError());
		assertTrue("server errors of server", importResult[2].isCorrect());
		
		//ContactObject[] contactArray = exportContact(getWebConversation(), contactFolderId, emailaddress, timeZone, getHostName(), getSessionId());
		
		ContactTest.deleteContact(getWebConversation(), Integer.parseInt(importResult[0].getObjectId()), contactFolderId, getHostName(), getLogin(), getPassword());
		ContactTest.deleteContact(getWebConversation(), Integer.parseInt(importResult[2].getObjectId()), contactFolderId, getHostName(), getLogin(), getPassword());
	}
	
	public void test6823() throws TestException, IOException, SAXException, JSONException, Exception{
		//final String vcard = "BEGIN:VCARD\nVERSION:3.0\nPRODID:OPEN-XCHANGE\nFN:Prinz\\, Tobias\nN:Prinz;Tobias;;;\nNICKNAME:Tierlieb\nBDAY:19810501\nADR;TYPE=work:;;;Meinerzhagen;NRW;58540;DE\nTEL;TYPE=home,voice:+49 2358 7192\nEMAIL:tobias.prinz@open-xchange.com\nORG:- deactivated -\nREV:20061204T160750.018Z\nURL:www.tobias-prinz.de\nUID:80@ox6.netline.de\nEND:VCARD\n";
		final String vcard ="BEGIN:VCARD\nVERSION:3.0\nN:;Svetlana;;;\nFN:Svetlana\nTEL;type=CELL;type=pref:6670373\nCATEGORIES:Nicht abgelegt\nX-ABUID:CBC739E8-694E-4589-8651-8C30E1A6E724\\:ABPerson\nEND:VCARD";
		final ImportResult[] importResult = importVCard(getWebConversation(), new ByteArrayInputStream(vcard.getBytes()), contactFolderId, timeZone, emailaddress, getHostName(), getSessionId());
		
		assertTrue("Only one import" , importResult.length == 1);
		assertFalse("No error?", importResult[0].hasError());
		
		ContactTest.deleteContact(getWebConversation(), Integer.parseInt(importResult[0].getObjectId()), contactFolderId, getHostName(), getLogin(), getPassword());
	}
	
	public void test6962followup() throws TestException, IOException, SAXException, JSONException, Exception{
		final String vcard ="BEGIN:VCARD\nVERSION:3.0\nN:;Svetlana;;;\nFN:Svetlana\nTEL;type=CELL;type=pref:673730\nEND:VCARD\nBEGIN:VCARD\nVERSION:666\nN:;Svetlana;;;\nFN:Svetlana\nTEL;type=CELL;type=pref:6670373\nEND:VCARD";
		final ImportResult[] importResult = importVCard(getWebConversation(), new ByteArrayInputStream(vcard.getBytes()), contactFolderId, timeZone, emailaddress, getHostName(), getSessionId());
		
		assertTrue("Two import attempts" , importResult.length == 2);
		assertFalse("No error on first attempt?", importResult[0].hasError());
		assertTrue("Error on second attempt?", importResult[1].hasError());
		final AbstractOXException ex = importResult[1].getException();

		//following line was removed since test environment cannot relay correct error messages from server
		//assertEquals("Correct error code?", "I_E-0605",ex.getErrorCode());
		ContactTest.deleteContact(getWebConversation(), Integer.parseInt(importResult[0].getObjectId()), contactFolderId, getHostName(), getLogin(), getPassword());
	}
	
	public void test7106() throws TestException, IOException, SAXException, JSONException, Exception{
		final String vcard ="BEGIN:VCARD\nVERSION:3.0\nN:;H\u00fcb\u00fcrt;;;\nFN:H\u00fcb\u00fcrt S\u00f6nderzeich\u00f6n\nTEL;type=CELL;type=pref:6670373\nEND:VCARD\n";
		final ImportResult[] importResult = importVCard(getWebConversation(), new ByteArrayInputStream(vcard.getBytes("UTF-8")), contactFolderId, timeZone, emailaddress, getHostName(), getSessionId());
		
		assertFalse("Worked?", importResult[0].hasError());
		final int contactId = Integer.parseInt(importResult[0].getObjectId());
		final ContactObject myImport = ContactTest.loadContact(getWebConversation(), contactId, contactFolderId, getHostName(), getLogin(), getPassword());
		assertEquals("Checking surname:" , "H\u00fcb\u00fcrt S\u00f6nderzeich\u00f6n" , myImport.getDisplayName());
	
		ContactTest.deleteContact(getWebConversation(), contactId, contactFolderId, getHostName(), getLogin(), getPassword());
	}
	
	/**
	 * Deals with N and ADR properties and different amount of semicola used. 
	 * Also tests an input stream with no terminating newline
	 */
	public void test7248() throws TestException, IOException, SAXException, JSONException, Exception{
		final String vcard ="BEGIN:VCARD\nVERSION:2.1\nN:Colombara;Robert\nFN:Robert Colombara\nADR;WORK:;;;;;;DE\nADR;HOME:;;;;;- / -\nEND:VCARD";
		final ImportResult[] importResult = importVCard(getWebConversation(), new ByteArrayInputStream(vcard.getBytes()), contactFolderId, timeZone, emailaddress, getHostName(), getSessionId());
		assertTrue("Only one import?", importResult.length == 1 );
		assertFalse("Import worked?", importResult[0].hasError());
		
		final int contactId = Integer.parseInt(importResult[0].getObjectId());
		final ContactObject myImport = ContactTest.loadContact(getWebConversation(), contactId, contactFolderId, getHostName(), getLogin(), getPassword());
		assertEquals("Checking surname:" , "Colombara" , myImport.getSurName());
	
		ContactTest.deleteContact(getWebConversation(), contactId, contactFolderId, getHostName(), getLogin(), getPassword());
	}
	
	/**
	 * Deals with broken E-Mail adresses as encountered in Resources in the example file
	 */
	public void test7249() throws TestException, IOException, SAXException, JSONException, Exception{
		final String vcard ="BEGIN:VCARD\nVERSION:2.1\nFN:Conference_Room_Olpe\nEMAIL;PREF;INTERNET:Conference_Room_Olpe_EMAIL\nEND:VCARD";
		final ImportResult[] importResult = importVCard(getWebConversation(), new ByteArrayInputStream(vcard.getBytes()), contactFolderId, timeZone, emailaddress, getHostName(), getSessionId());
		
		assertFalse("Worked?", importResult[0].hasError());
		final int contactId = Integer.parseInt(importResult[0].getObjectId());
		final ContactObject myImport = ContactTest.loadContact(getWebConversation(), contactId, contactFolderId, getHostName(), getLogin(), getPassword());
		assertEquals("Checking surname:" , "Conference_Room_Olpe" , myImport.getDisplayName());
		assertEquals("Checking email1 (must be null):" , null , myImport.getEmail1());
		assertEquals("Checking email2 (must be null):" , null , myImport.getEmail2());
		assertEquals("Checking email3 (must be null):" , null , myImport.getEmail3());
	
		ContactTest.deleteContact(getWebConversation(), contactId, contactFolderId, getHostName(), getLogin(), getPassword());
	}
	
	/**
	 * Deals with umlauts
	 */
	public void test7250() throws TestException, IOException, SAXException, JSONException, Exception{
		final String vcard ="BEGIN:VCARD\nVERSION:2.1\nN;CHARSET=Windows-1252:B\u00f6rnig;Anke;;;\nFN;CHARSET=Windows-1252:Anke  B\u00f6rnig\nEND:VCARD";
		final ImportResult[] importResult = importVCard(getWebConversation(), new ByteArrayInputStream(vcard.getBytes("Cp1252")), contactFolderId, timeZone, emailaddress, getHostName(), getSessionId());
		
		assertFalse("Worked?", importResult[0].hasError());
		final int contactId = Integer.parseInt(importResult[0].getObjectId());
		final ContactObject myImport = ContactTest.loadContact(getWebConversation(), contactId, contactFolderId, getHostName(), getLogin(), getPassword());
		assertEquals("Checking surname:" , "B\u00f6rnig" , myImport.getSurName());
	
		ContactTest.deleteContact(getWebConversation(), contactId, contactFolderId, getHostName(), getLogin(), getPassword());
	}
}