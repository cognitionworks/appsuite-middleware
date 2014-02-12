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
package com.openexchange.ajax.importexport;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.apache.commons.logging.LogFactory;
import com.openexchange.ajax.contact.action.DeleteRequest;
import com.openexchange.ajax.contact.action.GetRequest;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXClient.User;
import com.openexchange.groupware.container.Contact;
import com.openexchange.webdav.xml.ContactTest;

public class VCardExportTest extends AbstractVCardTest {

	final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ICalImportTest.class);

	public VCardExportTest(final String name) {
		super(name);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testExportVCard() throws Exception {
		final String surname = "testImportVCard" + System.currentTimeMillis();

		final Contact contactObj = new Contact();
		contactObj.setSurName(surname);
		contactObj.setGivenName("givenName");
		contactObj.setBirthday(simpleDateFormat.parse("2007-04-04"));
		contactObj.setParentFolderID(contactFolderId);

		final int objectId = ContactTest.insertContact(getWebConversation(), contactObj, getHostName(), getLogin(), getPassword(), "");

		final Contact[] contactArray = exportContact(getWebConversation(), contactFolderId, emailaddress, timeZone, getHostName(), getSessionId());

		boolean found = false;
		for (int a = 0; a < contactArray.length; a++) {
			if (contactArray[a].getSurName() != null && contactArray[a].getSurName().equals(surname)) {
				found = true;
				ContactTest.compareObject(contactObj, contactArray[a]);
			}
		}
		assertTrue("contact with surname: " + surname + " not found", found);

		final AJAXClient client = new AJAXClient(User.User1);
		final GetRequest getRequest = new GetRequest(contactFolderId, objectId, client.getValues().getTimeZone(), false);
		Date lastModified;
		try {
            lastModified = client.execute(getRequest).getContact().getLastModified();
        } catch (final Exception e) {
            lastModified = new Date(System.currentTimeMillis() + 10000);
        }

		final DeleteRequest del = new DeleteRequest(client.getValues().getPrivateContactFolder(), objectId, lastModified, false);
		client.execute(del);
	}
}
