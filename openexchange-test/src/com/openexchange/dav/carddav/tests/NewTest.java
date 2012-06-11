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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.dav.carddav.tests;

import java.util.Date;
import java.util.List;
import java.util.Map;

import net.sourceforge.cardme.vcard.types.ExtendedType;

import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.ThrowableHolder;
import com.openexchange.dav.carddav.CardDAVTest;
import com.openexchange.dav.carddav.VCardResource;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DistributionListEntryObject;
import com.openexchange.groupware.container.FolderObject;

/**
 * {@link NewTest}
 * 
 * Tests contact creation via the CardDAV interface 
 * 
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class NewTest extends CardDAVTest {

	public NewTest(String name) {
		super(name);
	}	
	
	public void testCreateSimpleOnClient() throws Exception {
		/*
		 * fetch sync token for later synchronization
		 */
		final String syncToken = super.fetchSyncToken();
		/*
		 * create contact
		 */
    	final String uid = randomUID();
    	final String firstName = "test";
    	final String lastName = "horst";
        final String vCard =
        		"BEGIN:VCARD" + "\r\n" +
   				"VERSION:3.0" + "\r\n" +
				"N:" + lastName + ";" + firstName + ";;;" + "\r\n" +
				"FN:" + firstName + " " + lastName + "\r\n" +
				"ORG:test3;" + "\r\n" +
				"EMAIL;type=INTERNET;type=WORK;type=pref:test@example.com" + "\r\n" +
				"TEL;type=WORK;type=pref:24235423" + "\r\n" +
				"TEL;type=CELL:352-3534" + "\r\n" +
				"TEL;type=HOME:346346" + "\r\n" +
				"UID:" + uid + "\r\n" +
				"REV:" + super.formatAsUTC(new Date()) + "\r\n" +
				"PRODID:-//Apple Inc.//AddressBook 6.0//EN" + "\r\n" +
				"END:VCARD" + "\r\n"
		;
        assertEquals("response code wrong", StatusCodes.SC_CREATED, super.putVCard(uid, vCard));
        /*
         * verify contact on server
         */
        final Contact contact = super.getContact(uid);
        super.rememberForCleanUp(contact);        
        assertEquals("uid wrong", uid, contact.getUid());
        assertEquals("firstname wrong", firstName, contact.getGivenName());
        assertEquals("lastname wrong", lastName, contact.getSurName());
        /*
         * verify contact on client
         */
        final Map<String, String> eTags = super.syncCollection(syncToken);
        assertTrue("no resource changes reported on sync collection", 0 < eTags.size());
        final List<VCardResource> addressData = super.addressbookMultiget(eTags.keySet());
        final VCardResource card = assertContains(uid, addressData);
        assertEquals("N wrong", firstName, card.getVCard().getName().getGivenName());
        assertEquals("N wrong", lastName, card.getVCard().getName().getFamilyName());
        assertEquals("FN wrong", firstName + " " + lastName, card.getVCard().getFormattedName().getFormattedName());
	}
	
	public void testCreateSimpleOnServer() throws Exception {
		/*
		 * fetch sync token for later synchronization
		 */
		final String syncToken = super.fetchSyncToken();		
		/*
		 * create contact
		 */
    	final String uid = randomUID();
    	final String firstName = "test";
    	final String lastName = "otto";    	
		Contact contact = new Contact();
		contact.setSurName(lastName);
		contact.setGivenName(firstName);
		contact.setDisplayName(firstName + " " + lastName);
		contact.setUid(uid);
		super.rememberForCleanUp(super.create(contact));
        /*
         * verify contact on client
         */
        final Map<String, String> eTags = super.syncCollection(syncToken);
        assertTrue("no resource changes reported on sync collection", 0 < eTags.size());
        final List<VCardResource> addressData = super.addressbookMultiget(eTags.keySet());
        final VCardResource card = assertContains(uid, addressData);
        assertEquals("N wrong", firstName, card.getVCard().getName().getGivenName());
        assertEquals("N wrong", lastName, card.getVCard().getName().getFamilyName());
        assertEquals("FN wrong", firstName + " " + lastName, card.getVCard().getFormattedName().getFormattedName());
        /*
         * verify contact on server
         */
        contact = super.getContact(uid);
        super.rememberForCleanUp(contact);        
        assertEquals("uid wrong", uid, contact.getUid());
        assertEquals("firstname wrong", firstName, contact.getGivenName());
        assertEquals("lastname wrong", lastName, contact.getSurName());
	}
	
	public void testAddContactInSubfolderServer() throws Exception {
		/*
		 * fetch sync token for later synchronization
		 */
		SyncToken syncToken = new SyncToken(super.fetchSyncToken());		
		/*
		 * create folder and contact on server
		 */
    	String folderName = "testfolder_" + randomUID();
    	FolderObject folder = super.createFolder(folderName);
		super.rememberForCleanUp(folder);
        FolderObject createdFolder = super.getFolder(folderName);
        assertNotNull("folder not found on server", createdFolder);
        assertEquals("foldername wrong", folderName, createdFolder.getFolderName());
    	String uid = randomUID();
    	String firstName = "test";
    	String lastName = "herbert";    	
		Contact contact = new Contact();
		contact.setSurName(lastName);
		contact.setGivenName(firstName);
		contact.setDisplayName(firstName + " " + lastName);
		contact.setUid(uid);
		super.rememberForCleanUp(super.create(contact, createdFolder.getObjectID()));
        /*
         * verify contact on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue("no resource changes reported on sync collection", 0 < eTags.size());
        List<VCardResource> addressData = super.addressbookMultiget(eTags.keySet());
        VCardResource card = assertContains(uid, addressData);
        assertEquals("N wrong", firstName, card.getVCard().getName().getGivenName());
        assertEquals("N wrong", lastName, card.getVCard().getName().getFamilyName());
        assertEquals("FN wrong", firstName + " " + lastName, card.getVCard().getFormattedName().getFormattedName());
	}

	public void testCreateContactInGroupOnClient() throws Throwable {
		/*
		 * fetch sync token for later synchronization
		 */
		SyncToken syncToken = new SyncToken(super.fetchSyncToken());		
		/*
		 * create empty distribution list on server
		 */
    	String listUid = randomUID();
    	String listName = "test distribution list";
		Contact distributionList = new Contact();
		distributionList.setDisplayName(listName);
		distributionList.setUid(listUid);
		distributionList.setMarkAsDistributionlist(true);
		super.rememberForCleanUp(super.create(distributionList));
        /*
         * verify distribution list on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue("no resource changes reported on sync collection", 0 < eTags.size());
        List<VCardResource> addressData = super.addressbookMultiget(eTags.keySet());
        VCardResource groupCard = assertContains(listUid, addressData);
        assertEquals("FN wrong", listName, groupCard.getFN());
        /*
         * add contact on client
         */
    	final String contactUid = randomUID();
    	String firstName = "test";
    	String lastName = "daphne";
    	String email = "daphne88@example.com";
        final String contactVCard =
        		"BEGIN:VCARD" + "\r\n" +
   				"VERSION:3.0" + "\r\n" +
				"N:" + lastName + ";" + firstName + ";;;" + "\r\n" +
				"FN:" + firstName + " " + lastName + "\r\n" +
				"ORG:test88;" + "\r\n" +
				"EMAIL;type=INTERNET;type=WORK;type=pref:" + email + "\r\n" +
				"TEL;type=WORK;type=pref:24235423" + "\r\n" +
				"TEL;type=CELL:352-3534" + "\r\n" +
				"TEL;type=HOME:547547" + "\r\n" +
				"UID:" + contactUid + "\r\n" +
				"REV:" + super.formatAsUTC(new Date()) + "\r\n" +
				"PRODID:-//Apple Inc.//AddressBook 6.0//EN" + "\r\n" +
				"END:VCARD" + "\r\n"
		;
        ExtendedType newMember = new ExtendedType("X-ADDRESSBOOKSERVER-MEMBER", "urn:uuid:" + contactUid);
        groupCard.getVCard().addExtendedType(newMember);
        /*
         * perform contact creation and group update on different threads to simulate concurrent requests of the client
         */
        final ThrowableHolder throwableHolder = new ThrowableHolder();
        Thread createThread = new Thread() {
        	@Override
            public void run() {
                try {
	                assertEquals("response code wrong", StatusCodes.SC_CREATED, putVCard(contactUid, contactVCard));
				} catch (Throwable t) {
					throwableHolder.setThrowable(t);
				}
    		}
        };
        createThread.start();
		assertEquals("response code wrong", StatusCodes.SC_CREATED, putVCardUpdate(groupCard.getUID(), groupCard.toString(), groupCard.getETag()));
        createThread.join();
        throwableHolder.reThrowIfSet();
        /*
         * verify contact and group on server
         */
        Contact contact = super.getContact(contactUid);
        assertNotNull("contact not found on server", contact);
        super.rememberForCleanUp(contact);
        assertEquals("uid wrong", contactUid, contact.getUid());
        assertEquals("firstname wrong", firstName, contact.getGivenName());
        assertEquals("lastname wrong", lastName, contact.getSurName());
        assertEquals("email wrong", email, contact.getEmail1());
        distributionList = super.getContact(listUid);
        assertNotNull("distribution list not found on server", distributionList);
        assertEquals("uid wrong", listUid, distributionList.getUid());
        assertEquals("displayname wrong", listName, distributionList.getDisplayName());
        assertTrue("list not marked as distribution list", distributionList.getMarkAsDistribtuionlist());
        assertNotNull("no members in distribution list", distributionList.getDistributionList());
        assertTrue("invalid member count in distribution list", 1 == distributionList.getNumberOfDistributionLists());
        assertTrue("invalid member count in distribution list", 1 == distributionList.getDistributionList().length);
        DistributionListEntryObject member = distributionList.getDistributionList()[0];
        assertNotNull("no member in distribution list", member);
        assertEquals("email wrong", email, member.getEmailaddress());
        /*
         * verify contact and group on client
         */
        eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue("no resource changes reported on sync collection", 0 < eTags.size());
        addressData = super.addressbookMultiget(eTags.keySet());
        VCardResource card = assertContains(contactUid, addressData);
        assertEquals("N wrong", firstName, card.getVCard().getName().getGivenName());
        assertEquals("N wrong", lastName, card.getVCard().getName().getFamilyName());
        assertEquals("FN wrong", firstName + " " + lastName, card.getVCard().getFormattedName().getFormattedName());
        card = assertContains(listUid, addressData);
        assertEquals("FN wrong", listName, card.getFN());
        assertContainsMemberUID(contactUid, card);
	}

}
