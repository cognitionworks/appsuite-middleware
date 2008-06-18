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

package com.openexchange.groupware.importexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

import junit.framework.JUnit4TestAdapter;

import org.junit.BeforeClass;
import org.junit.Test;

import com.openexchange.api2.OXException;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.importexport.exceptions.ImportExportException;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.userconfiguration.OverridingUserConfigurationStorage;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.groupware.userconfiguration.UserConfigurationException;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;

/**
 * This class tests whether security checks for modularisation work
 * for all Importers.
 * 
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias 'Tierlieb' Prinz</a>
 *
 */
public class Bug8681forICAL extends AbstractICalImportTest {
	
	//workaround for JUnit 3 runner
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(Bug8681forICAL.class);
	}


    @BeforeClass

    public static void initialize() throws Exception {
        AbstractICalImportTest.initialize();
        ctx = ContextStorage.getInstance().getContext(ContextStorage.getInstance().getContextId("defaultcontext"));
    }

    @Test public void testDummy(){}

    @Test public void checkAppointment() throws AbstractOXException, UnsupportedEncodingException, SQLException, OXException, LdapException {
		folderId = createTestFolder(FolderObject.CALENDAR, sessObj,ctx, "bug8681 for ical appointments");
		
		final UserConfigurationStorage original = UserConfigurationStorage.getInstance();
        final OverridingUserConfigurationStorage override = new OverridingUserConfigurationStorage(original) {
            @Override
			public UserConfiguration getOverride(final int userId, final int[] groups, final Context ctx) throws UserConfigurationException {
                final UserConfiguration orig = delegate.getUserConfiguration(userId, ctx);
                final UserConfiguration copy = (UserConfiguration) orig.clone();
                copy.setCalendar(false);
                return copy;
            }
        };
        override.override();
		
		try {
			final String ical = //from bug 7732
				"BEGIN:VCALENDAR\n" +
				"PRODID:-//Microsoft Corporation//Outlook 12.0 MIMEDIR//EN\n" +
				"VERSION:2.0\n" +
				"METHOD:PUBLISH\n" +
					"BEGIN:VEVENT\n" +
					"CLASS:PUBLIC\n" +
					"CREATED:20070531T130514Z\n" +
					"DESCRIPTION:\\n\n" +
					"DTEND:20070912T083000Z\n" +
					"DTSTAMP:20070531T130514Z\n" +
					"DTSTART:20070912T080000Z\n" +
					"LAST-MODIFIED:20070531T130514Z\n" +
					"LOCATION:loc\n" +
					"PRIORITY:5\n" +
					"RRULE:FREQ=DAILY;COUNT=10\n" +
					"SEQUENCE:0\n" +
					"SUMMARY;LANGUAGE=de:Daily iCal\n" +
					"TRANSP:OPAQUE\n" +
					"UID:040000008200E00074C5B7101A82E008000000005059CADA94A3C701000000000000000010000000A1B56CAC71BB0948833B0C11C333ADB0\n" +
					"END:VEVENT\n" +
				"END:VCALENDAR";

			imp.canImport(sessObj, format, _folders(), null);
			imp.importData(sessObj, format, new ByteArrayInputStream(ical.getBytes("UTF-8")), _folders(), null);
			fail("Could import appointment into Calendar module without permission");
		} catch (final ImportExportException e){
			assertEquals(Category.PERMISSION, e.getCategory());
			assertEquals("I_E-0507", e.getErrorCode());
		} finally {
			override.takeBack();
			deleteTestFolder(folderId);
		}
		
	}

    @Test public void checkTask() throws AbstractOXException, UnsupportedEncodingException, SQLException, OXException, LdapException {
		folderId = createTestFolder(FolderObject.TASK, sessObj,ctx, "bug8681 for ical tasks");
		
		final UserConfigurationStorage original = UserConfigurationStorage.getInstance();
        final OverridingUserConfigurationStorage override = new OverridingUserConfigurationStorage(original) {
            @Override
			public UserConfiguration getOverride(final int userId, final int[] groups, final Context ctx) throws UserConfigurationException {
                final UserConfiguration orig = delegate.getUserConfiguration(userId, ctx);
                final UserConfiguration copy = (UserConfiguration) orig.clone();
                copy.setTask(false);
                return copy;
            }
        };
        override.override();
		try {
			final String ical = //from bug 7718
				"BEGIN:VCALENDAR\n" +
				"PRODID:-//K Desktop Environment//NONSGML libkcal 3.2//EN\n" +
				"VERSION:2.0\n" +
				"BEGIN:VTODO\n" +
				"DTSTAMP:20070531T093649Z\n" +
				"ORGANIZER;CN=Stephan Martin:MAILTO:stephan.martin@open-xchange.com\n" +
				"CREATED:20070531T093612Z\n" +
				"UID:libkcal-1172232934.1028\n" +
				"SEQUENCE:0\n" +
				"LAST-MODIFIED:20070531T093612Z\n" +
				"DESCRIPTION:Blub\n" +
				"SUMMARY:Blubsum\n" +
				"LOCATION:daheim\n" +
				"CLASS:PUBLIC\n" +
				"PRIORITY:5\n" +
				"DUE;VALUE=DATE:20070731\n" +
				"PERCENT-COMPLETE:30\n" +
				"END:VTODO\n" +
				"END:VCALENDAR";

			imp.canImport(sessObj, format, _folders(), null);
			imp.importData(sessObj, format, new ByteArrayInputStream(ical.getBytes("UTF-8")), _folders(), null);
			fail("Could import appointment into Calendar module without permission");
		} catch (final ImportExportException e){
			assertEquals(Category.PERMISSION, e.getCategory());
			assertEquals("I_E-0508", e.getErrorCode());
		} finally {
			override.takeBack();
			deleteTestFolder(folderId);
		}
		
	}
}
