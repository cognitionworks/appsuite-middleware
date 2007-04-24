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

package com.openexchange.groupware.importexport.importers;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openexchange.api2.ContactSQLInterface;
import com.openexchange.api2.OXException;
import com.openexchange.api2.RdbContactSQLInterface;
import com.openexchange.groupware.Component;
import com.openexchange.groupware.OXExceptionSource;
import com.openexchange.groupware.OXThrowsMultiple;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.container.ContactObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.importexport.Format;
import com.openexchange.groupware.importexport.ImportResult;
import com.openexchange.groupware.importexport.Importer;
import com.openexchange.groupware.importexport.exceptions.ImportExportException;
import com.openexchange.groupware.importexport.exceptions.ImportExportExceptionClasses;
import com.openexchange.groupware.importexport.exceptions.ImportExportExceptionFactory;
import com.openexchange.server.DBPoolingException;
import com.openexchange.server.EffectivePermission;
import com.openexchange.sessiond.SessionObject;
import com.openexchange.tools.versit.Versit;
import com.openexchange.tools.versit.VersitDefinition;
import com.openexchange.tools.versit.VersitObject;
import com.openexchange.tools.versit.converter.OXContainerConverter;

@OXExceptionSource(
		classId=ImportExportExceptionClasses.VCARDIMPORTER,
		component=Component.IMPORT_EXPORT)
@OXThrowsMultiple(
		category={
	Category.PERMISSION,
	Category.SUBSYSTEM_OR_SERVICE_DOWN,
	Category.USER_INPUT,
	Category.CODE_ERROR,
	Category.CODE_ERROR},
		desc={"","","","",""},
		exceptionId={0,1,2,3,4},
		msg={
	"Could not import into the folder %s.",
	"Subsystem down",
	"User input Error %s",
	"Programming Error - Folder %s",
	"Could not load folder %s"})

/**
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias 'Tierlieb' Prinz</a> (minor: changes to new interface)
 */
public class VCardImporter implements Importer {
	
	private static final Log LOG = LogFactory.getLog(VCardImporter.class);
	
	private static ImportExportExceptionFactory importExportExceptionFactory = new ImportExportExceptionFactory(VCardImporter.class);
	
	public boolean canImport(final SessionObject sessObj, final Format format, final List<String> folders, final Map<String, String[]> optionalParams) throws ImportExportException {
		if (!format.equals(Format.VCARD)) {
			return false;
		}
		final Iterator iterator = folders.iterator();
		while (iterator.hasNext()) {
			final String folder = iterator.next().toString();
			
			int folderId = Integer.parseInt(folder);
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
				throw importExportExceptionFactory.create(1, folder);
			} catch (SQLException e) {
				throw importExportExceptionFactory.create(1, folder);
			}
			
			if (perm.canCreateObjects()) {
				return true;
			}
		}
		
		return false;
	}
	
	public List<ImportResult> importData(final SessionObject sessObj, final Format format, final InputStream is, final List<String> folders, final Map<String, String[]> optionalParams) throws ImportExportException {
		int contactFolderId = -1;
		
		final Iterator iterator = folders.iterator();
		while (iterator.hasNext()) {
			final String folder = iterator.next().toString();
			
			int folderId = Integer.parseInt(folder);
			FolderObject fo;
			try {
				fo = FolderObject.loadFolderObjectFromDB(folderId, sessObj.getContext());
			} catch (OXException e) {
				throw importExportExceptionFactory.create(4,folderId);
			}
			
			if (fo.getModule() == FolderObject.CONTACT) {
				contactFolderId = folderId;
				break;
			}
		}
		
		final ContactSQLInterface contactInterface = new RdbContactSQLInterface(sessObj);
		final OXContainerConverter oxContainerConverter = new OXContainerConverter(sessObj);
		
		List<ImportResult> list = new ArrayList<ImportResult>();
		
		try {
			final VersitDefinition def = Versit.getDefinition(format.getMimeType());
			final VersitDefinition.Reader versitReader = def.getReader(is, "UTF-8");
			final VersitObject rootVersitObject = def.parseBegin(versitReader);
			VersitObject versitObject = def.parseChild(versitReader, rootVersitObject);
			while (versitObject != null) {
				ImportResult importResult = new ImportResult();
				try {
					//final Property property = versitObject.getProperty("UID");
					
					importResult.setFolder(String.valueOf(contactFolderId));
								
					final ContactObject contactObj = oxContainerConverter.convertContact(versitObject);
					contactObj.setParentFolderID(contactFolderId);
					contactInterface.insertContactObject(contactObj);
						
					importResult.setObjectId(String.valueOf(contactObj.getObjectID()));
					importResult.setDate(contactObj.getLastModified());
				} catch (OXException exc) {
					LOG.debug("cannot import contact object", exc);
					importResult.setException(exc);
				}
				
				list.add(importResult);
				
				versitObject = def.parseChild(versitReader, rootVersitObject);
			}
		} catch (Exception exc) {
			throw importExportExceptionFactory.create(4, contactFolderId);
		} finally {
			oxContainerConverter.close();
		}
		
		return list;
	}
}
