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

package com.openexchange.groupware.importexport.exporters;

import com.openexchange.api2.ContactSQLInterface;
import com.openexchange.api2.RdbContactSQLInterface;
import com.openexchange.groupware.importexport.SizedInputStream;
import java.sql.SQLException;

import com.openexchange.api2.OXException;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.Component;
import com.openexchange.groupware.OXExceptionSource;
import com.openexchange.groupware.OXThrowsMultiple;
import com.openexchange.groupware.container.ContactObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.importexport.Exporter;
import com.openexchange.groupware.importexport.Format;
import com.openexchange.groupware.importexport.exceptions.ImportExportException;
import com.openexchange.groupware.importexport.exceptions.ImportExportExceptionClasses;
import com.openexchange.groupware.importexport.exceptions.ImportExportExceptionFactory;
import com.openexchange.server.DBPoolingException;
import com.openexchange.server.EffectivePermission;
import com.openexchange.sessiond.SessionObject;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.versit.Versit;
import com.openexchange.tools.versit.VersitDefinition;
import com.openexchange.tools.versit.VersitObject;
import com.openexchange.tools.versit.converter.OXContainerConverter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

@OXExceptionSource(
		classId=ImportExportExceptionClasses.VCARDEXPORTER,
		component=Component.IMPORT_EXPORT)
@OXThrowsMultiple(
		category={
	Category.PERMISSION,
	Category.SUBSYSTEM_OR_SERVICE_DOWN,
	Category.USER_INPUT,
	Category.PROGRAMMING_ERROR},
		desc={"","","",""},
		exceptionId={0,1,2,3},
		msg={
	"Could not import into the folder %s.",
	"Subsystem down - Could not import into folder %s",
	"User input Error %s",
	"Programming Error - Could not import into folder %s"})
	
/**
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias 'Tierlieb' Prinz</a> (minor: changes to new interface)
 */
public class VCardExporter implements Exporter {
	
	private static ImportExportExceptionFactory importExportExceptionFactory = new ImportExportExceptionFactory(VCardExporter.class);
	
	public boolean canExport(final SessionObject sessObj, final Format format, final String folder, final Map<String, String[]> optionalParams) throws ImportExportException {
		final int folderId = Integer.parseInt(folder);
		FolderObject fo;
		try {
			fo = FolderObject.loadFolderObjectFromDB(folderId, sessObj.getContext());
		} catch (OXException e) {
			return false;
		}
		//check format of folder
		if ( fo.getModule() != FolderObject.CONTACT){
			return false;
		}
		//check read access to folder
		EffectivePermission perm;
		try {
			perm = fo.getEffectiveUserPermission(sessObj.getUserObject().getId(), sessObj.getUserConfiguration());
		} catch (DBPoolingException e) {
			throw importExportExceptionFactory.create(2, folder);
		} catch (SQLException e) {
			throw importExportExceptionFactory.create(2, folder);
		}
		
		if (perm.canReadAllObjects()) {
			if (format.getMimeType().equals("text/vcard")) {
				return true;
			}
		}
		
		return false;
	}
	
	public SizedInputStream exportData(SessionObject sessObj, Format format, String folder, int[] fieldsToBeExported, Map<String, String[]> optionalParams) throws ImportExportException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try {
			final VersitDefinition versitDefinition = Versit.getDefinition("text/calendar");
			VersitDefinition.Writer versitWriter = versitDefinition.getWriter(byteArrayOutputStream, "UTF-8");
			final VersitObject versitObjectContainer = OXContainerConverter.newCalendar("2.0");
			versitDefinition.writeProperties(versitWriter, versitObjectContainer);
			final VersitDefinition eventDef = versitDefinition.getChildDef("VEVENT");
			final VersitDefinition taskDef = versitDefinition.getChildDef("VTODO");
			
			final TimeZone timeZone = TimeZone.getTimeZone(sessObj.getUserObject().getTimeZone());
			final String mail = sessObj.getUserObject().getMail();
			
			final OXContainerConverter oxContainerConverter = new OXContainerConverter(timeZone, mail);
			
			final ContactSQLInterface contactSql = new RdbContactSQLInterface(sessObj);
			final SearchIterator searchIterator = contactSql.getModifiedContactsInFolder(Integer.parseInt(folder), fieldsToBeExported, new Date(0));
			
			while (searchIterator.hasNext()) {
				exportContact(oxContainerConverter, eventDef, versitWriter, (ContactObject)searchIterator.next());
			}
		} catch (Exception exc) {
			throw importExportExceptionFactory.create(3, folder);
		}
		
		return new SizedInputStream(
			new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), 
			byteArrayOutputStream.size(),
			Format.VCARD);
	}
	
	public SizedInputStream exportData(SessionObject sessObj, Format format, String folder, int objectId, int[] fieldsToBeExported, Map<String, String[]> optionalParams) throws ImportExportException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try {
			final VersitDefinition contactDef = Versit.getDefinition("text/vcard");
			final VersitDefinition.Writer versitWriter = contactDef.getWriter(byteArrayOutputStream, "UTF-8");
			final OXContainerConverter oxContainerConverter = new OXContainerConverter(sessObj);
			
			final ContactSQLInterface contactSql = new RdbContactSQLInterface(sessObj);
			final ContactObject contactObj = contactSql.getObjectById(objectId, Integer.parseInt(folder));
			
			exportContact(oxContainerConverter, contactDef, versitWriter, contactObj);
		} catch (Exception exc) {
			throw importExportExceptionFactory.create(3, folder);
		}
		
		return new SizedInputStream(
			new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), 
			byteArrayOutputStream.size(),
			Format.VCARD);
	}
	
	protected void exportContact(OXContainerConverter oxContainerConverter, VersitDefinition versitDef, VersitDefinition.Writer writer, ContactObject contactObj) throws Exception {
		VersitObject versitObject = oxContainerConverter.convertContact(contactObj, "3.0");
		versitDef.write(writer, versitObject);
	}
}
