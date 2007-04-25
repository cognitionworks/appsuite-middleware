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

package com.openexchange.groupware.infostore.facade.impl;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openexchange.api2.OXException;
import com.openexchange.event.EventClient;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.Component;
import com.openexchange.groupware.IDGenerator;
import com.openexchange.groupware.OXExceptionSource;
import com.openexchange.groupware.OXThrows;
import com.openexchange.groupware.OXThrowsMultiple;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.UserConfiguration;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.ContextException;
import com.openexchange.groupware.filestore.FilestoreException;
import com.openexchange.groupware.filestore.FilestoreStorage;
import com.openexchange.groupware.infostore.Classes;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.groupware.infostore.EffectiveInfostorePermission;
import com.openexchange.groupware.infostore.InfostoreException;
import com.openexchange.groupware.infostore.InfostoreExceptionFactory;
import com.openexchange.groupware.infostore.InfostoreFacade;
import com.openexchange.groupware.infostore.database.impl.CreateDocumentAction;
import com.openexchange.groupware.infostore.database.impl.CreateVersionAction;
import com.openexchange.groupware.infostore.database.impl.DatabaseImpl;
import com.openexchange.groupware.infostore.database.impl.DeleteDocumentAction;
import com.openexchange.groupware.infostore.database.impl.DeleteVersionAction;
import com.openexchange.groupware.infostore.database.impl.DocumentMetadataImpl;
import com.openexchange.groupware.infostore.database.impl.GetSwitch;
import com.openexchange.groupware.infostore.database.impl.InfostoreIterator;
import com.openexchange.groupware.infostore.database.impl.InfostoreQueryCatalog;
import com.openexchange.groupware.infostore.database.impl.InfostoreSecurityImpl;
import com.openexchange.groupware.infostore.database.impl.SetSwitch;
import com.openexchange.groupware.infostore.database.impl.UpdateDocumentAction;
import com.openexchange.groupware.infostore.database.impl.UpdateVersionAction;
import com.openexchange.groupware.infostore.utils.Metadata;
import com.openexchange.groupware.infostore.webdav.EntityLockManager;
import com.openexchange.groupware.infostore.webdav.EntityLockManagerImpl;
import com.openexchange.groupware.infostore.webdav.Lock;
import com.openexchange.groupware.infostore.webdav.LockManager;
import com.openexchange.groupware.infostore.webdav.LockManager.Scope;
import com.openexchange.groupware.infostore.webdav.LockManager.Type;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.results.Delta;
import com.openexchange.groupware.results.DeltaImpl;
import com.openexchange.groupware.results.TimedResult;
import com.openexchange.groupware.results.TimedResultImpl;
import com.openexchange.groupware.tx.DBProvider;
import com.openexchange.groupware.tx.DBProviderUser;
import com.openexchange.groupware.tx.DBService;
import com.openexchange.groupware.tx.ReuseReadConProvider;
import com.openexchange.groupware.tx.TransactionException;
import com.openexchange.server.EffectivePermission;
import com.openexchange.sessiond.SessionObject;
import com.openexchange.tools.collections.Injector;
import com.openexchange.tools.file.FileStorage;
import com.openexchange.tools.file.FileStorageException;
import com.openexchange.tools.file.QuotaFileStorage;
import com.openexchange.tools.file.SaveFileWithQuotaAction;
import com.openexchange.tools.iterator.CombinedSearchIterator;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIteratorAdapter;
import com.openexchange.tools.iterator.SearchIteratorException;

/**
 * DatabaseImpl
 * 
 * @author <a href="mailto:benjamin.otterbach@open-xchange.com">Benjamin
 *         Otterbach</a>
 */

@OXExceptionSource(classId = Classes.COM_OPENEXCHANGE_GROUPWARE_INFOSTORE_FACADE_IMPL_INFOSTOREFACADEIMPL, component = Component.INFOSTORE)
public class InfostoreFacadeImpl extends DBService implements InfostoreFacade,
		DBProviderUser {

	private static final Log LOG = LogFactory.getLog(InfostoreFacadeImpl.class);

	private static final InfostoreExceptionFactory EXCEPTIONS = new InfostoreExceptionFactory(
			InfostoreFacadeImpl.class);

	public static final InfostoreQueryCatalog QUERIES = new InfostoreQueryCatalog();

	private final DatabaseImpl db = new DatabaseImpl();

	private final InfostoreSecurityImpl security = new InfostoreSecurityImpl();

	private final EntityLockManager lockManager = new EntityLockManagerImpl(
			"infostore_lock");

	private final ThreadLocal<List<String>> fileIdRemoveList = new ThreadLocal<List<String>>();

	private final ThreadLocal<Context> ctxHolder = new ThreadLocal<Context>();

	public InfostoreFacadeImpl() {
		super();
	}

	public InfostoreFacadeImpl(DBProvider provider) {
		setProvider(provider);
	}

	public boolean exists(int id, int version, Context ctx, User user,
			UserConfiguration userConfig) throws OXException {
		InfostoreIterator iter = InfostoreIterator.loadDocumentIterator(id, version, getProvider(), ctx);
		boolean exists = iter.hasNext();
		try {
			iter.close();
		} catch (SearchIteratorException e) {
			throw new InfostoreException(e);
		}
		
		return exists;
	}

	@OXThrowsMultiple(
			category = {Category.USER_INPUT, Category.USER_INPUT},
			desc = {"The User does not have read permissions on the requested Infoitem. ", "The document could not be loaded because it doesn't exist."},
			exceptionId = {0,38},
			msg = {"You do not have sufficient read permissions.", "The document you requested doesn't exist."}
	)
	public DocumentMetadata getDocumentMetadata(int id, int version,
			Context ctx, User user, UserConfiguration userConfig)
			throws OXException {
		EffectiveInfostorePermission infoPerm = security
				.getInfostorePermission(id, ctx, user, userConfig);

		if (!infoPerm.canReadObject()) {
			throw EXCEPTIONS.create(0);
		}
		
		return addLocked(load(id,version,ctx), ctx, user, userConfig);
	}

	private DocumentMetadata load(int id, int version, Context ctx) throws OXException {
		InfostoreIterator iter = InfostoreIterator.loadDocumentIterator(id, version, getProvider(), ctx);
		if(!iter.hasNext()) {
			throw EXCEPTIONS.create(38);
		}
		DocumentMetadata dm;
		try {
			dm = iter.next();
			iter.close();
		} catch (SearchIteratorException e) {
			throw new InfostoreException(e);
		}
		return dm;
	}

	public void saveDocumentMetadata(DocumentMetadata document,
			long sequenceNumber, SessionObject sessionObj) throws OXException {
		saveDocument(document, null, sequenceNumber, sessionObj);
	}

	public void saveDocumentMetadata(DocumentMetadata document,
			long sequenceNumber, Metadata[] modifiedColumns,
			SessionObject sessionObj) throws OXException {
		saveDocument(document, null, sequenceNumber, modifiedColumns,
				sessionObj);
	}

	@OXThrowsMultiple(
			category = {Category.USER_INPUT, Category.SUBSYSTEM_OR_SERVICE_DOWN, Category.SUBSYSTEM_OR_SERVICE_DOWN},
			desc = {"The User does not have read permissions on the requested Infoitem. ", "The file store couldn't be reached and is probably down.", "The file could not be found in the file store. This means either that the file store was not available or that database and file store are inconsistent. Run the recovery tool."},
			exceptionId = {1,39,40},
			msg = {"You do not have sufficient read permissions.","The file store could not be reched", "The file could not be retrieved."})
	public InputStream getDocument(int id, int version, Context ctx, User user,
			UserConfiguration userConfig) throws OXException {
		EffectiveInfostorePermission infoPerm = security
				.getInfostorePermission(id, ctx, user, userConfig);
		if (!infoPerm.canReadObject()) {
			throw EXCEPTIONS.create(1);
		}
		DocumentMetadata dm = load(id, version, ctx);
		FileStorage fs = null;
		try {
			fs = getFileStorage(ctx);
		} catch (FilestoreException e) {
			throw new InfostoreException(e);
		} catch (FileStorageException e) {
			throw new InfostoreException(e);
		}
		try {
			return fs.getFile(dm.getFilestoreLocation());
		} catch (FileStorageException e) {
			throw new InfostoreException(e);
		}
	}

	@OXThrows(category = Category.USER_INPUT, desc = "The user does not have sufficient write permissions to lock this infoitem.", exceptionId = 18, msg = "You need write permissions to lock a document.")
	public void lock(int id, long diff, SessionObject sessionObj)
			throws OXException {
		EffectiveInfostorePermission infoPerm = security
				.getInfostorePermission(id, sessionObj.getContext(), sessionObj
						.getUserObject(), sessionObj.getUserConfiguration());
		if (!infoPerm.canWriteObject()) {
			throw EXCEPTIONS.create(18);
		}
		checkWriteLock(id, sessionObj);
		long timeout = 0;
		if (timeout == -1) {
			timeout = LockManager.INFINITE;
		} else {
			timeout = System.currentTimeMillis() + diff;
		}
		lockManager.lock(id, timeout, Scope.EXCLUSIVE, Type.WRITE, sessionObj
				.getUserlogin(), sessionObj.getContext(), sessionObj
				.getUserObject(), sessionObj.getUserConfiguration());
		touch(id, sessionObj);
	}

	@OXThrows(category = Category.USER_INPUT, desc = "The user does not have sufficient write permissions to unlock this infoitem.", exceptionId = 17, msg = "You need write permissions to unlock a document.")
	public void unlock(int id, SessionObject sessionObj) throws OXException {
		EffectiveInfostorePermission infoPerm = security
				.getInfostorePermission(id, sessionObj.getContext(), sessionObj
						.getUserObject(), sessionObj.getUserConfiguration());
		if (!infoPerm.canWriteObject()) {
			throw EXCEPTIONS.create(17);
		}
		checkMayUnlock(id, sessionObj);
		lockManager.removeAll(id, sessionObj.getContext(), sessionObj
				.getUserObject(), sessionObj.getUserConfiguration());
		touch(id, sessionObj);
	}

	private void touch(int id, SessionObject sessionObj) throws OXException {
		try {
			DocumentMetadata oldDocument = load(id, CURRENT_VERSION, sessionObj
							.getContext());
			DocumentMetadata document = new DocumentMetadataImpl(oldDocument);

			document.setLastModified(new Date());
			document.setModifiedBy(sessionObj.getUserObject().getId());

			UpdateDocumentAction updateDocument = new UpdateDocumentAction();
			updateDocument.setContext(sessionObj.getContext());
			updateDocument.setDocuments(Arrays.asList(document));
			updateDocument.setModified(Metadata.LAST_MODIFIED_LITERAL,
					Metadata.MODIFIED_BY_LITERAL);
			updateDocument.setOldDocuments(Arrays.asList(oldDocument));
			updateDocument.setProvider(this);
			updateDocument.setQueryCatalog(QUERIES);
			updateDocument.setTimestamp(oldDocument.getSequenceNumber());

			perform(updateDocument, true);

			UpdateVersionAction updateVersion = new UpdateVersionAction();
			updateVersion.setContext(sessionObj.getContext());
			updateVersion.setDocuments(Arrays.asList(document));
			updateVersion.setModified(Metadata.LAST_MODIFIED_LITERAL,
					Metadata.MODIFIED_BY_LITERAL);
			updateVersion.setOldDocuments(Arrays.asList(oldDocument));
			updateVersion.setProvider(this);
			updateVersion.setQueryCatalog(QUERIES);
			updateVersion.setTimestamp(oldDocument.getSequenceNumber());

			perform(updateVersion, true);

			EventClient ec = new EventClient(sessionObj);
			ec.modify(document);
		} catch (OXException x) {
			throw x;
		} catch (Exception e) {
			// FIXME Client
			LOG.debug("", e);
		}
	}

	@OXThrows(category = Category.INTERNAL_ERROR, desc = "The system couldn't iterate the result dataset. This can have numerous exciting causes.", exceptionId = 13, msg = "Could not iterate result")
	private Delta addLocked(Delta delta, Context ctx, User user,
			UserConfiguration userConfig) throws OXException {
		try {
			return new LockDelta(delta, ctx, user, userConfig);
		} catch (SearchIteratorException e) {
			e.printStackTrace();
			throw EXCEPTIONS.create(13,e);
		}
	}

	@OXThrows(category = Category.INTERNAL_ERROR, desc = "The system couldn't iterate the result dataset. This can have numerous exciting causes.", exceptionId = 14, msg = "Could not iterate result")
	private TimedResult addLocked(TimedResult tr, Context ctx, User user,
			UserConfiguration userConfig) throws OXException {
		try {
			return new LockTimedResult(tr, ctx, user, userConfig);
		} catch (SearchIteratorException e) {
			throw EXCEPTIONS.create(14);
		}
	}

	private DocumentMetadata addLocked(DocumentMetadata document, Context ctx,
			User user, UserConfiguration userConfig) throws OXException {
		List<Lock> locks = lockManager.findLocks(document.getId(), ctx, user,
				userConfig);
		long max = 0;
		for (Lock l : locks) {
			if (l.getTimeout() > max)
				max = l.getTimeout();
		}
		document.setLockedUntil(new Date(System.currentTimeMillis() + max));
		return document;
	}

	private SearchIterator lockedUntilIterator(SearchIterator iter,
			Context ctx, User user, UserConfiguration userConfig)
			throws SearchIteratorException, OXException {
		List<DocumentMetadata> list = new ArrayList<DocumentMetadata>();
		while (iter.hasNext()) {
			DocumentMetadata m = (DocumentMetadata) iter.next();
			addLocked(m, ctx, user, userConfig);
			list.add(m);
		}
		return new SearchIteratorAdapter(list.iterator());
	}

	private void checkWriteLock(int id, SessionObject sessionObj)
			throws OXException {
		DocumentMetadata document = load(id, CURRENT_VERSION,
				sessionObj.getContext());
		checkWriteLock(document, sessionObj);
	}

	@OXThrows(category = Category.CONCURRENT_MODIFICATION, desc = "The infoitem was locked by some other user. Only the user that locked the item (the one that modified the entry) can modify a locked infoitem.", exceptionId = 15, msg = "This document is locked.")
	private void checkWriteLock(DocumentMetadata document,
			SessionObject sessionObj) throws OXException {
		if (document.getModifiedBy() == sessionObj.getUserObject().getId())
			return;
		List<Lock> locks = lockManager.findLocks(document.getId(), sessionObj
				.getContext(), sessionObj.getUserObject(), sessionObj
				.getUserConfiguration());
		if (locks.size() > 0) {
			throw EXCEPTIONS.create(15);
		}
	}

	@OXThrows(category = Category.CONCURRENT_MODIFICATION, desc = "The infoitem was locked by some other user. Only the user that locked the item and the creator of the item can unlock a locked infoitem.", exceptionId = 16, msg = "You cannot unlock this document.")
	private void checkMayUnlock(int id, SessionObject sessionObj)
			throws OXException {
		DocumentMetadata document = load(id, CURRENT_VERSION,
				sessionObj.getContext());
		if (document.getCreatedBy() == sessionObj.getUserObject().getId()
				|| document.getModifiedBy() == sessionObj.getUserObject()
						.getId())
			return;
		List<Lock> locks = lockManager.findLocks(id, sessionObj.getContext(),
				sessionObj.getUserObject(), sessionObj.getUserConfiguration());
		if (locks.size() > 0) {
			throw EXCEPTIONS.create(16);
		}
	}

	@OXThrowsMultiple(category = { Category.USER_INPUT,
			Category.SUBSYSTEM_OR_SERVICE_DOWN, Category.INTERNAL_ERROR }, desc = {
			"The user may not create objects in the given folder. ",
			"The file store couldn't be reached.",
			"The IDGenerator threw an SQL Exception look at that one to find out what's wrong." }, exceptionId = {
			2, 19, 20 }, msg = {
			"You do not have sufficient permissions to create objects in this folder.",
			"The file store could not be reached.", "Could not generate new ID." })
	public void saveDocument(DocumentMetadata document, InputStream data,
			long sequenceNumber, SessionObject sessionObj) throws OXException {
		security.checkFolderId(document.getFolderId(), sessionObj.getContext());
		if (document.getId() == InfostoreFacade.NEW) {
			EffectivePermission isperm = security.getFolderPermission(document
					.getFolderId(), sessionObj.getContext(), sessionObj
					.getUserObject(), sessionObj.getUserConfiguration());
			if (!isperm.canCreateObjects()) {
				throw EXCEPTIONS.create(2);
			}
			setDefaults(document);
			checkUniqueFilename(document.getFileName(), document.getFolderId(), document.getId(), sessionObj.getContext());
			
			Connection writeCon = null;
			try {
				startDBTransaction();
				writeCon = getWriteConnection(sessionObj.getContext());
				document.setId(getId(sessionObj.getContext(), writeCon));
				commitDBTransaction();
			} catch (SQLException e) {
				throw EXCEPTIONS.create(20, e);
			} finally {
				releaseWriteConnection(sessionObj.getContext(), writeCon);
				finishDBTransaction();
			}
			document.setCreationDate(new Date(System.currentTimeMillis()));
			document.setLastModified(document.getCreationDate());
			document.setCreatedBy(sessionObj.getUserObject().getId());
			document.setModifiedBy(sessionObj.getUserObject().getId());

			// db.createDocument(document, data, sessionObj.getContext(),
			// sessionObj.getUserObject(), sessionObj.getUserConfiguration());

			if (null != data) {
				document.setVersion(1);
			} else {
				document.setVersion(0);
			}

			CreateDocumentAction createAction = new CreateDocumentAction();
			createAction.setContext(sessionObj.getContext());
			createAction.setDocuments(Arrays.asList(document));
			createAction.setProvider(this);
			createAction.setQueryCatalog(QUERIES);

			try {
				perform(createAction, true);
			} catch (OXException x) {
				throw x;
			} catch (AbstractOXException e1) {
				throw new InfostoreException(e1);
			}

			DocumentMetadata version0 = new DocumentMetadataImpl(document);
			version0.setFileName(null);
			version0.setFileSize(0);
			version0.setFileMD5Sum(null);
			version0.setFileMIMEType(null);
			version0.setVersion(0);

			CreateVersionAction createVersionAction = new CreateVersionAction();
			createVersionAction.setContext(sessionObj.getContext());
			createVersionAction.setDocuments(Arrays.asList(version0));
			createVersionAction.setProvider(this);
			createVersionAction.setQueryCatalog(QUERIES);

			try {
				perform(createVersionAction, true);
			} catch (OXException x) {
				throw x;
			} catch (AbstractOXException e1) {
				throw new InfostoreException(e1);
			}

			if (data != null) {
				SaveFileWithQuotaAction saveFile = new SaveFileWithQuotaAction();
				try {
					QuotaFileStorage qfs = (QuotaFileStorage) getFileStorage(sessionObj
							.getContext());
					saveFile.setStorage(qfs);
					saveFile.setSizeHint(document.getFileSize());
					saveFile.setIn(data);

					perform(saveFile, false);

					document.setVersion(1);
					document.setFilestoreLocation(saveFile.getId());
					if (document.getFileSize() == 0) {
						document.setFileSize(qfs.getFileSize(saveFile.getId()));
					}

					createVersionAction = new CreateVersionAction();
					createVersionAction.setContext(sessionObj.getContext());
					createVersionAction.setDocuments(Arrays.asList(document));
					createVersionAction.setProvider(this);
					createVersionAction.setQueryCatalog(QUERIES);

					perform(createVersionAction, true);

				} catch (FileStorageException e) {
					throw new InfostoreException(e);
				} catch (ContextException e) {
					throw new InfostoreException(e);
				} catch (OXException x) {
					throw x;
				} catch (AbstractOXException e) {
					throw new InfostoreException(e);
				}

			}

			EventClient ec = new EventClient(sessionObj);
			try {
				ec.create(document);
			} catch (Exception e) {
				LOG.error("", e);
			}

		} else {
			saveDocument(document, data, sequenceNumber, nonNull(document),
					sessionObj);
		}
	}

	private void setDefaults(DocumentMetadata document) {
		if(document.getTitle() == null || "".equals(document.getTitle()))
			document.setTitle(document.getFileName());
	}

	// FIXME Move 2 query builder
	private int getNextVersionNumberForInfostoreObject(int cid,
			int infostore_id, Connection con) throws SQLException {
		int retval = 0;

		PreparedStatement stmt = con
				.prepareStatement("SELECT MAX(version_number) FROM infostore_document WHERE cid=? AND infostore_id=?");
		stmt.setInt(1, cid);
		stmt.setInt(2, infostore_id);
		ResultSet result = stmt.executeQuery();
		if (result.next()) {
			retval = result.getInt(1);
		}
		result.close();
		stmt.close();

		stmt = con
				.prepareStatement("SELECT MAX(version_number) FROM del_infostore_document WHERE cid=? AND infostore_id=?");
		stmt.setInt(1, cid);
		stmt.setInt(2, infostore_id);
		result = stmt.executeQuery();
		if (result.next()) {
			int delVersion = result.getInt(1);
			if (delVersion > retval)
				retval = delVersion;
		}
		result.close();
		stmt.close();

		return retval + 1;
	}
	
	@OXThrows(
			category=Category.USER_INPUT,
			desc="To remain consistent in WebDAV no two current versions in a given folder may contain a file with the same filename. The user must either choose a different filename, or switch the other file to a version with a different filename.", 
			exceptionId=41,
			msg="Files attached to InfoStore items must have unique names. Filename: %s. The other document with this file name is %s."
	)
	private void checkUniqueFilename(String filename, long folderId, int id, Context ctx) throws OXException  {
		if(null == filename)
			return;
		if("".equals(filename.trim()))
			return;
		InfostoreIterator iter = null;
		try {
			iter = InfostoreIterator.documentsByFilename(folderId, filename, new Metadata[]{Metadata.ID_LITERAL, Metadata.TITLE_LITERAL}, getProvider(), ctx);
			while(iter.hasNext()) {
				DocumentMetadata dm = iter.next();
				if(dm.getId() != id)
					throw EXCEPTIONS.create(41,filename, dm.getTitle());
			}
		} catch (SearchIteratorException e) {
			throw new InfostoreException(e);
		} finally {
			try {
				iter.close();
			} catch (SearchIteratorException e) {
				throw new InfostoreException(e);
			}
		}
		
	}

	protected FileStorage getFileStorage(Context ctx) throws
			FilestoreException, FileStorageException {
		return FileStorage.getInstance(FilestoreStorage.createURI(ctx),ctx, this.getProvider());
	}

	private Metadata[] nonNull(DocumentMetadata document) {
		List<Metadata> nonNull = new ArrayList<Metadata>();
		GetSwitch get = new GetSwitch(document);
		for (Metadata metadata : Metadata.HTTPAPI_VALUES) {
			if (null != metadata.doSwitch(get))
				nonNull.add(metadata);
		}
		return nonNull.toArray(new Metadata[nonNull.size()]);
	}

	@OXThrowsMultiple(category = { Category.USER_INPUT, Category.USER_INPUT }, desc = {
			"The user doesn't have the required write permissions to update the infoitem.",
			"The user isn't allowed to create objects in the target folder when moving an infoitem." }, exceptionId = {
			3, 4 }, msg = { "You are not allowed to update this item.",
			"You are not allowed to create objects in the target folder." })
	public void saveDocument(DocumentMetadata document, InputStream data,
			long sequenceNumber, Metadata[] modifiedColumns,
			SessionObject sessionObj) throws OXException {
		try {
			EffectiveInfostorePermission infoPerm = security
					.getInfostorePermission(document.getId(), sessionObj
							.getContext(), sessionObj.getUserObject(),
							sessionObj.getUserConfiguration());
			if (!infoPerm.canWriteObject()) {
				throw EXCEPTIONS.create(3);
			}
			if ((Arrays.asList(modifiedColumns)
					.contains(Metadata.FOLDER_ID_LITERAL))
					&& (document.getFolderId() != -1) && infoPerm.getObject().getFolderId() != document.getFolderId()) {
				security.checkFolderId(document.getFolderId(), sessionObj
						.getContext());
				EffectivePermission isperm = security.getFolderPermission(
						document.getFolderId(), sessionObj.getContext(),
						sessionObj.getUserObject(), sessionObj
								.getUserConfiguration());
				if (!(isperm.canCreateObjects())) {
					throw EXCEPTIONS.create(4);
				}
			}
			checkWriteLock(document.getId(), sessionObj);

			document.setLastModified(new Date());
			document.setModifiedBy(sessionObj.getUserObject().getId());

			// db.updateDocument(document, data, sequenceNumber,
			// modifiedColumns, sessionObj.getContext(),
			// sessionObj.getUserObject(), sessionObj.getUserConfiguration());

			// db.createDocument(document, data, sessionObj.getContext(),
			// sessionObj.getUserObject(), sessionObj.getUserConfiguration());

			DocumentMetadata oldDocument = load(document
					.getId(), CURRENT_VERSION, sessionObj.getContext());

			Set<Metadata> updatedCols = new HashSet<Metadata>(Arrays
					.asList(modifiedColumns));
			updatedCols.add(Metadata.LAST_MODIFIED_LITERAL);
			updatedCols.add(Metadata.MODIFIED_BY_LITERAL);
			
			if(updatedCols.contains(Metadata.VERSION_LITERAL)) {
				String fname = load(document.getId(), document.getVersion(), sessionObj.getContext()).getFileName();
				if(fname != null && !fname.equals(oldDocument.getFileName())) {
					checkUniqueFilename(fname, oldDocument.getFolderId(), oldDocument.getId(), sessionObj.getContext());
				}
			}
			
			modifiedColumns = updatedCols.toArray(new Metadata[updatedCols
					.size()]);

			if(document.getFileName() != null && !document.getFileName().equals(oldDocument.getFileName())) {
				checkUniqueFilename(document.getFileName(), oldDocument.getFolderId(), oldDocument.getId(), sessionObj.getContext());
			}
			
			if (data != null) {
				
				SaveFileWithQuotaAction saveFile = new SaveFileWithQuotaAction();
				try {
					QuotaFileStorage qfs = (QuotaFileStorage) getFileStorage(sessionObj
							.getContext());
					saveFile.setStorage(qfs);
					saveFile.setSizeHint(document.getFileSize());
					saveFile.setIn(data);
					perform(saveFile, false);
					document.setFilestoreLocation(saveFile.getId());

					if (document.getFileSize() == 0) {
						document.setFileSize(qfs.getFileSize(saveFile.getId()));
					}

					GetSwitch get = new GetSwitch(oldDocument);
					SetSwitch set = new SetSwitch(document);
					Set<Metadata> alreadySet = new HashSet<Metadata>(Arrays
							.asList(modifiedColumns));
					for (Metadata m : Arrays.asList(
							Metadata.DESCRIPTION_LITERAL,
							Metadata.TITLE_LITERAL, Metadata.URL_LITERAL)) {
						if (alreadySet.contains(m))
							continue;
						set.setValue(m.doSwitch(get));
						m.doSwitch(set);
					}

					document.setCreatedBy(sessionObj.getUserObject().getId());
					document.setCreationDate(new Date());
					Connection con = null;
					try {
						con = getReadConnection(sessionObj.getContext());
						document
								.setVersion(getNextVersionNumberForInfostoreObject(
										sessionObj.getContext().getContextId(),
										document.getId(), con));
						updatedCols.add(Metadata.VERSION_LITERAL);
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						releaseReadConnection(sessionObj.getContext(), con);
					}

					CreateVersionAction createVersionAction = new CreateVersionAction();
					createVersionAction.setContext(sessionObj.getContext());
					createVersionAction.setDocuments(Arrays.asList(document));
					createVersionAction.setProvider(this);
					createVersionAction.setQueryCatalog(QUERIES);

					perform(createVersionAction, true);

				} catch (FileStorageException e) {
					throw new InfostoreException(e);
				} catch (ContextException e) {
					throw new InfostoreException(e);
				} catch (OXException x) {
					throw x;
				} catch (AbstractOXException e) {
					throw new InfostoreException(e);
				}

			} else if (QUERIES.updateVersion(modifiedColumns)) {
				if (!updatedCols.contains(Metadata.VERSION_LITERAL))
					document.setVersion(oldDocument.getVersion());
				UpdateVersionAction updateVersionAction = new UpdateVersionAction();
				updateVersionAction.setContext(sessionObj.getContext());
				updateVersionAction.setDocuments(Arrays.asList(document));
				updateVersionAction.setOldDocuments(Arrays.asList(oldDocument));
				updateVersionAction.setProvider(this);
				updateVersionAction.setQueryCatalog(QUERIES);
				updateVersionAction.setModified(modifiedColumns);
				updateVersionAction.setTimestamp(sequenceNumber);
				try {
					perform(updateVersionAction, true);
				} catch (OXException x) {
					throw x;
				} catch (AbstractOXException e1) {
					throw new InfostoreException(e1);
				}
			}

			modifiedColumns = updatedCols.toArray(new Metadata[updatedCols
					.size()]);
			if (QUERIES.updateDocument(modifiedColumns)) {
				UpdateDocumentAction updateAction = new UpdateDocumentAction();
				updateAction.setContext(sessionObj.getContext());
				updateAction.setDocuments(Arrays.asList(document));
				updateAction.setOldDocuments(Arrays.asList(oldDocument));
				updateAction.setProvider(this);
				updateAction.setQueryCatalog(QUERIES);
				updateAction.setModified(modifiedColumns);
				updateAction.setTimestamp(sequenceNumber);
				try {
					perform(updateAction, true);
				} catch (OXException x) {
					throw x;
				} catch (AbstractOXException e1) {
					throw new InfostoreException(e1);
				}
			}

			EventClient ec = new EventClient(sessionObj);
			DocumentMetadataImpl docForEvent = new DocumentMetadataImpl(oldDocument);
			SetSwitch set = new SetSwitch(docForEvent);
			GetSwitch get = new GetSwitch(document);
			for(Metadata metadata : modifiedColumns) {
				set.setValue(metadata.doSwitch(get));
				metadata.doSwitch(set);
			}
			ec.modify(docForEvent);
		} catch (OXException x) {
			throw x;
		} catch (Exception e) {
			// FIXME Client
			LOG.debug("", e);
		}
	}

	public void removeDocument(long folderId, long date,
			SessionObject sessionObj) throws OXException {
		DBProvider reuseProvider = new ReuseReadConProvider(getProvider());
		try {
			List<DocumentMetadata> allVersions = InfostoreIterator
					.allVersionsWhere("infostore.folder_id = " + folderId,
							Metadata.VALUES_ARRAY, reuseProvider,
							sessionObj.getContext()).asList();
			List<DocumentMetadata> allDocuments = InfostoreIterator
					.allDocumentsWhere("infostore.folder_id = " + folderId,
							Metadata.VALUES_ARRAY, reuseProvider,
							sessionObj.getContext()).asList();
			removeDocuments(allDocuments, allVersions, date, sessionObj, null);
		} catch (SearchIteratorException x) {
			throw new InfostoreException(x);
		}
	}

	@OXThrows(category = Category.CONCURRENT_MODIFICATION, desc = "Not all infoitems in the given folder could be deleted. This may be due to the infoitems being modified since the last request, or the objects might not even exist anymore or the user doesn't have enough delete permissions on certain objects.", exceptionId = 5, msg = "Could not delete all objects.")
	private void removeDocuments(List<DocumentMetadata> allDocuments,
			List<DocumentMetadata> allVersions, long date,
			SessionObject sessionObj, List<DocumentMetadata> rejected)
			throws OXException {
		List<DocumentMetadata> delDocs = new ArrayList<DocumentMetadata>();
		List<DocumentMetadata> delVers = new ArrayList<DocumentMetadata>();
		Set<Integer> rejectedIds = new HashSet<Integer>();

		Date now = new Date(); // FIXME: Recovery will change lastModified;

		for (DocumentMetadata m : allDocuments) {
			if (m.getSequenceNumber() > date) {
				if (rejected == null)
					throw EXCEPTIONS.create(5);
				rejected.add(m);
				rejectedIds.add(m.getId());
			} else {
				try {
					checkWriteLock(m, sessionObj);
					m.setLastModified(now);
					delDocs.add(m);
				} catch (InfostoreException x) {
					if (rejected != null) {
						rejected.add(m);
						rejectedIds.add(m.getId());
					} else
						throw x;
				}
			}
		}

		for (DocumentMetadata m : allVersions) {
			if (!rejectedIds.contains(m.getId())) {
				delVers.add(m);
				m.setLastModified(now);
				removeFile(sessionObj.getContext(), m.getFilestoreLocation());
			}
		}

		// Set<Integer> notDeleted = db.removeDocuments(deleteMe,
		// timed.sequenceNumber(), sessionObj.getContext(),
		// sessionObj.getUserObject(), sessionObj.getUserConfiguration());

		DeleteVersionAction deleteVersion = new DeleteVersionAction();
		deleteVersion.setContext(sessionObj.getContext());
		deleteVersion.setDocuments(delVers);
		deleteVersion.setProvider(this);
		deleteVersion.setQueryCatalog(QUERIES);

		try {
			perform(deleteVersion, true);
		} catch (OXException x) {
			throw x;
		} catch (AbstractOXException e1) {
			throw new InfostoreException(e1);
		}

		DeleteDocumentAction deleteDocument = new DeleteDocumentAction();
		deleteDocument.setContext(sessionObj.getContext());
		deleteDocument.setDocuments(delDocs);
		deleteDocument.setProvider(this);
		deleteDocument.setQueryCatalog(QUERIES);

		try {
			perform(deleteDocument, true);
		} catch (OXException x) {
			throw x;
		} catch (AbstractOXException e1) {
			throw new InfostoreException(e1);
		}

		EventClient ec = new EventClient(sessionObj);

		for (DocumentMetadata m : allDocuments) {
			try {
				ec.delete(m);
			} catch (Exception e) {
				LOG.error("", e);
			}
		}
	}

	@OXThrows(category = Category.SUBSYSTEM_OR_SERVICE_DOWN, desc = "Could not remove file from file store.", exceptionId = 37, msg = "Could not remove file from file store.")
	private void removeFile(Context context, String filestoreLocation)
			throws OXException {
		if (filestoreLocation == null)
			return;
		if (fileIdRemoveList.get() != null) {
			fileIdRemoveList.get().add(filestoreLocation);
			ctxHolder.set(context);
		} else {
			try {
				QuotaFileStorage qfs = (QuotaFileStorage) getFileStorage(context);
				qfs.deleteFile(filestoreLocation);
			} catch (FileStorageException x) {
				throw new InfostoreException(x);
			} catch (FilestoreException e) {
				throw new InfostoreException(e);
			}
		}
	}

	public int[] removeDocument(int[] id, long date,
			final SessionObject sessionObj) throws OXException {
		StringBuilder ids = new StringBuilder("(");
		for (int i : id) {
			ids.append(i).append(",");
		}
		ids.setLength(ids.length() - 1);
		ids.append(")");

		List<DocumentMetadata> allVersions = null;
		List<DocumentMetadata> allDocuments = null;

		DBProvider reuseProvider = new ReuseReadConProvider(getProvider());
		try {
			allVersions = InfostoreIterator.allVersionsWhere(
					"infostore.id IN " + ids.toString(), Metadata.VALUES_ARRAY,
					reuseProvider, sessionObj.getContext()).asList();
			allDocuments = InfostoreIterator.allDocumentsWhere(
					"infostore.id IN " + ids.toString(), Metadata.VALUES_ARRAY,
					reuseProvider, sessionObj.getContext()).asList();
		} catch (SearchIteratorException x) {
			x.printStackTrace();
			throw new InfostoreException(x);
		} catch (Throwable t) {
			t.printStackTrace();
		}

		// Check Permissions

		List<DocumentMetadata> rejected = new ArrayList<DocumentMetadata>();
		Set<Integer> rejectedIds = new HashSet<Integer>();

		Set<Integer> idSet = new HashSet<Integer>();
		for (int i : id) {
			idSet.add(i);
		}

		Map<Long, EffectivePermission> perms = new HashMap<Long, EffectivePermission>();

		List<DocumentMetadata> toDeleteDocs = new ArrayList<DocumentMetadata>();
		List<DocumentMetadata> toDeleteVersions = new ArrayList<DocumentMetadata>();

		for (DocumentMetadata m : allDocuments) {
			idSet.remove(m.getId());
			EffectivePermission p = perms.get(m.getFolderId());
			if (p == null) {
				p = security.getFolderPermission(m.getFolderId(), sessionObj
						.getContext(), sessionObj.getUserObject(), sessionObj
						.getUserConfiguration());
				perms.put(m.getFolderId(), p);
			}
			EffectiveInfostorePermission infoPerm = new EffectiveInfostorePermission(
					p, m, sessionObj.getUserObject());
			if (!infoPerm.canDeleteObject()) {
				rejected.add(m);
				rejectedIds.add(m.getId());
			} else {
				toDeleteDocs.add(m);
			}
		}

		for (DocumentMetadata m : allVersions) {
			if (!rejectedIds.contains(m.getId())) {
				toDeleteVersions.add(m);
			}
		}

		removeDocuments(toDeleteDocs, toDeleteVersions, date, sessionObj,
				rejected);

		int[] nd = new int[rejected.size() + idSet.size()];
		int i = 0;
		for (DocumentMetadata rej : rejected) {
			nd[i++] = rej.getId();
		}
		for (int notFound : idSet) {
			nd[i++] = notFound;
		}

		return nd;
	}

	@OXThrows(category = Category.USER_INPUT, desc = "The user must be allowed to delete the object in order to delete a version of it.", exceptionId = 6, msg = "You do not have sufficient permission to delete this version.")
	public int[] removeVersion(int id, int[] versionId, SessionObject sessionObj)
			throws OXException { 
		if (versionId.length <= 0)
			return versionId;

		DocumentMetadata metadata = load(id,
				InfostoreFacade.CURRENT_VERSION, sessionObj.getContext());
		try {
			checkWriteLock(metadata, sessionObj);
		} catch (OXException x) {
			return versionId;
		}
		EffectiveInfostorePermission infoPerm = security
				.getInfostorePermission(id, sessionObj.getContext(), sessionObj
						.getUserObject(), sessionObj.getUserConfiguration());
		if (!infoPerm.canDeleteObject()) {
			throw EXCEPTIONS.create(6);
		}
		StringBuilder versions = new StringBuilder("(");
		Set<Integer> versionSet = new HashSet<Integer>();

		for (int v : versionId) {
			versions.append(v).append(",");
			versionSet.add(v);
		}
		versions.setLength(versions.length() - 1);
		versions.append(")");

		List<DocumentMetadata> allVersions = null;
		try {
			allVersions = InfostoreIterator.allVersionsWhere(
					"infostore_document.infostore_id = " + id
							+ " AND infostore_document.version_number IN "
							+ versions.toString()
							+ " and infostore_document.version_number != 0 ",
					Metadata.VALUES_ARRAY, this, sessionObj.getContext())
					.asList();
		} catch (SearchIteratorException x) {
			throw new InfostoreException(x);
		}

		Date now = new Date();

		boolean removeCurrent = false;
		for (DocumentMetadata v : allVersions) {
			if (v.getVersion() == metadata.getVersion())
				removeCurrent = true;
			versionSet.remove(v.getVersion());
			v.setLastModified(now);
			removeFile(sessionObj.getContext(), v.getFilestoreLocation());
		}

		DeleteVersionAction deleteVersion = new DeleteVersionAction();
		deleteVersion.setContext(sessionObj.getContext());
		deleteVersion.setDocuments(allVersions);
		deleteVersion.setProvider(this);
		deleteVersion.setQueryCatalog(QUERIES);

		try {
			perform(deleteVersion, true);
		} catch (OXException x) {
			throw x;
		} catch (AbstractOXException e1) {
			throw new InfostoreException(e1);
		}

		DocumentMetadata update = new DocumentMetadataImpl(metadata);

		update.setLastModified(now);
		update.setModifiedBy(sessionObj.getUserObject().getId());

		Set<Metadata> updatedFields = new HashSet<Metadata>();
		updatedFields.add(Metadata.LAST_MODIFIED_LITERAL);
		updatedFields.add(Metadata.MODIFIED_BY_LITERAL);

		if (removeCurrent) {
			
			// Update Version 0
			DocumentMetadata oldVersion0 = load(id, 0,
					sessionObj.getContext());
			
			DocumentMetadata version0 = new DocumentMetadataImpl(metadata);
			version0.setVersion(0);

			UpdateVersionAction updateVersion = new UpdateVersionAction();
			updateVersion.setContext(sessionObj.getContext());
			updateVersion.setDocuments(Arrays.asList(version0));
			updateVersion.setModified(Metadata.DESCRIPTION_LITERAL,
					Metadata.TITLE_LITERAL, Metadata.URL_LITERAL,
					Metadata.LAST_MODIFIED_LITERAL,
					Metadata.MODIFIED_BY_LITERAL);
			updateVersion.setOldDocuments(Arrays.asList(oldVersion0));
			updateVersion.setProvider(this);
			updateVersion.setQueryCatalog(QUERIES);
			updateVersion.setTimestamp(Long.MAX_VALUE);
			try {
				perform(updateVersion, true);
			} catch (OXException x) {
				throw x;
			} catch (AbstractOXException e1) {
				throw new InfostoreException(e1);
			}

			// Set new Version Number
			update.setVersion(db.getMaxActiveVersion(metadata.getId(),
					sessionObj.getContext()));
			updatedFields.add(Metadata.VERSION_LITERAL);
		}

		UpdateDocumentAction updateDocument = new UpdateDocumentAction();
		updateDocument.setContext(sessionObj.getContext());
		updateDocument.setDocuments(Arrays.asList(update));
		updateDocument.setModified(updatedFields
				.toArray(new Metadata[updatedFields.size()]));
		updateDocument.setOldDocuments(Arrays.asList(metadata));
		updateDocument.setProvider(this);
		updateDocument.setQueryCatalog(QUERIES);
		updateDocument.setTimestamp(Long.MAX_VALUE);
		
		if(removeCurrent) {
			metadata = load(metadata.getId(), update.getVersion(), sessionObj.getContext());
			checkUniqueFilename(metadata.getFileName(), metadata.getFolderId(), metadata.getId(), sessionObj.getContext());
		}
		
		try {
			perform(updateDocument, true);
		} catch (OXException x) {
			throw x;
		} catch (AbstractOXException e1) {
			throw new InfostoreException(e1);
		}

		EventClient ec = new EventClient(sessionObj);
		try {
			ec.modify(metadata);
		} catch (Exception e) {
			LOG.error("", e); // FIXME
		}

		int[] retval = new int[versionSet.size()];
		int i = 0;
		for (Integer integer : versionSet) {
			retval[i++] = integer;
		}

		return retval;
	}

	public TimedResult getDocuments(long folderId, Context ctx, User user,
			UserConfiguration userConfig) throws OXException {
		return getDocuments(folderId,
				(Metadata[]) Metadata.HTTPAPI_VALUES_ARRAY, null, 0, ctx, user,
				userConfig);
	}

	public TimedResult getDocuments(long folderId, Metadata[] columns,
			Context ctx, User user, UserConfiguration userConfig)
			throws OXException {
		return getDocuments(folderId, columns, null, 0, ctx, user, userConfig);
	}

	@OXThrows(category = Category.USER_INPUT, desc = "The user may not create objects in the given folder. ", exceptionId = 7, msg = "You do not have sufficient permissions to create objects in this folder.")
	public TimedResult getDocuments(long folderId, Metadata[] columns,
			Metadata sort, int order, Context ctx, User user,
			UserConfiguration userConfig) throws OXException {
		boolean onlyOwn = false;
		EffectivePermission isperm = security.getFolderPermission(folderId,
				ctx, user, userConfig);
		if (isperm.getReadPermission() == EffectivePermission.NO_PERMISSIONS) {
			throw EXCEPTIONS.create(7);
		} else if (isperm.getReadPermission() == EffectivePermission.READ_OWN_OBJECTS) {
			onlyOwn = true;
		}
		boolean addLocked = false;
		for (Metadata m : columns) {
			if (m == Metadata.LOCKED_UNTIL_LITERAL) {
				addLocked = true;
				break;
			}
		}
		
		InfostoreIterator iter = null;
		if(onlyOwn) {
			iter = InfostoreIterator.documentsByCreator(folderId, user.getId(), columns, sort, order, getProvider(), ctx);
		} else {
			iter = InfostoreIterator.documents(folderId, columns, sort, order, getProvider(), ctx);	
		}
		TimedResult tr = new TimedResultImpl(iter, System.currentTimeMillis());
		if (addLocked)
			return addLocked(tr, ctx, user, userConfig);
		else
			return tr;
	}

	public TimedResult getVersions(int id, Context ctx, User user,
			UserConfiguration userConfig) throws OXException {
		return getVersions(id, (Metadata[]) Metadata.HTTPAPI_VALUES_ARRAY,
				null, 0, ctx, user, userConfig);
	}

	public TimedResult getVersions(int id, Metadata[] columns, Context ctx,
			User user, UserConfiguration userConfig) throws OXException {
		return getVersions(id, columns, null, 0, ctx, user, userConfig);
	}

	@OXThrows(category = Category.USER_INPUT, desc = "The user may not create objects in the given folder. ", exceptionId = 8, msg = "You do not have sufficient permissions to create objects in this folder.")
	public TimedResult getVersions(int id, Metadata[] columns, Metadata sort,
			int order, Context ctx, User user, UserConfiguration userConfig)
			throws OXException {
		EffectiveInfostorePermission infoPerm = security
				.getInfostorePermission(id, ctx, user, userConfig);
		if (!infoPerm.canReadObject()) {
			throw EXCEPTIONS.create(8);
		}
		boolean addLocked = false;
		for (Metadata m : columns) {
			if (m == Metadata.LOCKED_UNTIL_LITERAL) {
				addLocked = true;
				break;
			}
		}
		
		InfostoreIterator iter = InfostoreIterator.versions(id, columns, sort, order, getProvider(), ctx);	
		TimedResult tr = new TimedResultImpl(iter, System.currentTimeMillis());
		
		
		if (addLocked)
			return addLocked(tr, ctx, user, userConfig);
		else
			return tr;

	}

	@OXThrows(category = Category.USER_INPUT, desc = "The user may not create objects in the given folder. ", exceptionId = 9, msg = "You do not have sufficient permissions to create objects in this folder.")
	public TimedResult getDocuments(int[] ids, Metadata[] columns, Context ctx,
			User user, UserConfiguration userConfig) throws OXException {

		try {
			security.injectInfostorePermissions(ids, ctx, user, userConfig,
					null, new Injector<Object, EffectiveInfostorePermission>() {

						public Object inject(Object list,
								EffectiveInfostorePermission element) {
							if (!element.canReadObject()) {
								throw new NotAllowed(element.getObjectID());
							}
							return list;
						}

					});
		} catch (NotAllowed na) {
			throw EXCEPTIONS.create(9);
		}
		InfostoreIterator iter = InfostoreIterator.list(ids, columns, getProvider(), ctx);	
		TimedResult tr = new TimedResultImpl(iter, System.currentTimeMillis());
		
		for(Metadata m : columns) {
			if(m == Metadata.LOCKED_UNTIL_LITERAL)
				return addLocked(tr, ctx, user, userConfig);
		}
		return tr;
		
	}

	public Delta getDelta(long folderId, long updateSince, Metadata[] columns,
			boolean ignoreDeleted, Context ctx, User user,
			UserConfiguration userConfig) throws OXException {
		return getDelta(folderId, updateSince, columns, null, 0, ignoreDeleted,
				ctx, user, userConfig);
	}

	@OXThrows(category = Category.USER_INPUT, desc = "The user may not create objects in the given folder. ", exceptionId = 10, msg = "You do not have sufficient permissions to create objects in this folder.")
	public Delta getDelta(long folderId, long updateSince, Metadata[] columns,
			Metadata sort, int order, boolean ignoreDeleted, Context ctx,
			User user, UserConfiguration userConfig) throws OXException {
		boolean onlyOwn = false;

		EffectivePermission isperm = security.getFolderPermission(folderId,
				ctx, user, userConfig);
		if (isperm.getReadPermission() == EffectivePermission.NO_PERMISSIONS) {
			throw EXCEPTIONS.create(10);
		} else if (isperm.getReadPermission() == EffectivePermission.READ_OWN_OBJECTS) {
			onlyOwn = true;
		}
		boolean addLocked = true;
		for (Metadata m : columns) {
			if (m == Metadata.LOCKED_UNTIL_LITERAL) {
				addLocked = true;
				break;
			}
		}
		
		DBProvider reuse = new ReuseReadConProvider(getProvider());
		
		InfostoreIterator newIter = null;
		InfostoreIterator modIter = null;
		InfostoreIterator delIter = null;
		
		if(onlyOwn) {
			newIter = InfostoreIterator.newDocumentsByCreator(folderId, user.getId(), columns, sort, order, updateSince, reuse, ctx);
			modIter = InfostoreIterator.modifiedDocumentsByCreator(folderId, user.getId(), columns, sort, order, updateSince, reuse, ctx);
			if(!ignoreDeleted) {
				delIter = InfostoreIterator.deletedDocumentsByCreator(folderId, user.getId(), sort, order, updateSince, reuse, ctx);
			}
		} else {
			newIter = InfostoreIterator.newDocuments(folderId, columns, sort, order, updateSince, reuse, ctx);
			modIter = InfostoreIterator.modifiedDocuments(folderId, columns, sort, order, updateSince, reuse, ctx);
			if(!ignoreDeleted) {
				delIter = InfostoreIterator.deletedDocuments(folderId, sort, order, updateSince, reuse, ctx);
			}
		}
		
		Delta delta = new DeltaImpl(newIter, modIter, (ignoreDeleted ? SearchIteratorAdapter.createEmptyIterator() : delIter), System.currentTimeMillis());
		
		if (addLocked)
			return addLocked(delta, ctx,
					user, userConfig);
		else
			return delta;

	}

	@OXThrows(category = Category.USER_INPUT, desc = "The user may not create objects in the given folder. ", exceptionId = 11, msg = "You do not have sufficient permissions to create objects in this folder.")
	public int countDocuments(long folderId, Context ctx, User user,
			UserConfiguration userConfig) throws OXException {
		boolean onlyOwn = false;
		EffectivePermission isperm = security.getFolderPermission(folderId,
				ctx, user, userConfig);
		if (!(isperm.canReadAllObjects()) && !(isperm.canReadOwnObjects())) {
			throw EXCEPTIONS.create(11);
		} else if (isperm.canReadOwnObjects()) {
			onlyOwn = true;
		}
		return db.countDocuments(folderId, onlyOwn, ctx, user, userConfig);
	}

	public boolean hasFolderForeignObjects(long folderId, Context ctx,
			User user, UserConfiguration userConfig) throws OXException {
		return db.hasFolderForeignObjects(folderId, ctx, user, userConfig);
	}

	public boolean isFolderEmpty(long folderId, Context ctx) throws OXException {
		return db.isFolderEmpty(folderId, ctx);
	}

	public void removeUser(int id, Context ctx, SessionObject session) throws OXException {
		db.removeUser(id, ctx, session, lockManager);
	}

	private int getId(Context context, Connection writeCon) throws SQLException {
		boolean autoCommit = writeCon.getAutoCommit();
		if (autoCommit)
			writeCon.setAutoCommit(false);
		try {
			return IDGenerator.getId(context, Types.INFOSTORE, writeCon);
		} finally {
			if (autoCommit) {
				writeCon.commit();
				writeCon.setAutoCommit(true);
			}
		}
	}

	private static final class NotAllowed extends RuntimeException {
		private static final long serialVersionUID = 4872889537922290831L;

		public int id;

		public NotAllowed(int id) {
			this.id = id;
		}
	}

	@OXThrowsMultiple(category = { Category.SUBSYSTEM_OR_SERVICE_DOWN,
			Category.SUBSYSTEM_OR_SERVICE_DOWN }, desc = {
			"Cannot reach the file store so some documents were not deleted.",
			"Cannot reach the file store so some documents were not deleted. This propably means that file store and db are inconsistent. Run the recovery tool." }, exceptionId = {
			35, 36 }, msg = {
			"Cannot reach the file store so I cannot remove the documents.",
			"Cannot remove file. Database and file store are probably inconsistent. Please contact an administrator to run the recovery tool." }

	)
	public void commit() throws TransactionException {
		db.commit();
		security.commit();
		lockManager.commit();
		if (null != fileIdRemoveList.get() && fileIdRemoveList.get().size() > 0) {
			try {
				QuotaFileStorage qfs = (QuotaFileStorage) getFileStorage(ctxHolder
						.get());
				for (String id : fileIdRemoveList.get()) {
					try {
						//System.out.println("REMOVE " + id);
						qfs.deleteFile(id);
					} catch (FileStorageException x) {
						throw new TransactionException(x);
					}
				}
			} catch (FilestoreException e) {
				throw new TransactionException(e);
			} catch (FileStorageException e) {
				rollback();
				throw new TransactionException(e);
			}
		}
		super.commit();
	}

	public void finish() throws TransactionException {
		fileIdRemoveList.set(null);
		ctxHolder.set(null);
		db.finish();
		security.finish();
		super.finish();
	}

	public void rollback() throws TransactionException {
		db.rollback();
		security.rollback();
		lockManager.rollback();
		super.rollback();
	}

	public void setRequestTransactional(boolean transactional) {
		db.setRequestTransactional(transactional);
		security.setRequestTransactional(transactional);
		lockManager.setRequestTransactional(transactional);
		super.setRequestTransactional(transactional);
	}

	public void setTransactional(boolean transactional) {
		lockManager.setTransactional(transactional);
	}

	public void startTransaction() throws TransactionException {
		fileIdRemoveList.set(new ArrayList<String>());
		ctxHolder.set(null);
		db.startTransaction();
		security.startTransaction();
		lockManager.startTransaction();
		super.startTransaction();
	}

	public void setProvider(DBProvider provider) {
		super.setProvider(provider);
		db.setProvider(provider);
		security.setProvider(provider);
		if (lockManager instanceof DBService) {
			((DBService) lockManager).setProvider(provider);
		}
	}

	private final class LockTimedResult implements TimedResult {

		private long sequenceNumber = 0;

		private SearchIterator results;

		public LockTimedResult(TimedResult delegate, Context ctx, User user,
				UserConfiguration userConfig) throws SearchIteratorException,
				OXException {
			sequenceNumber = delegate.sequenceNumber();

			this.results = lockedUntilIterator(delegate.results(), ctx, user,
					userConfig);
		}

		public SearchIterator results() {
			return results;
		}

		public long sequenceNumber() {
			return sequenceNumber;
		}

	}

	private final class LockDelta implements Delta {

		private long sequenceNumber;

		private SearchIterator newIter;

		private SearchIterator modified;

		private SearchIterator deleted;

		public LockDelta(Delta delegate, Context ctx, User user,
				UserConfiguration userConfig) throws SearchIteratorException,
				OXException {
			SearchIterator deleted = delegate.getDeleted();
			if(null != deleted) {
				this.deleted = lockedUntilIterator(deleted, ctx,
						user, userConfig);
			}
			this.modified = lockedUntilIterator(delegate.getModified(), ctx,
					user, userConfig);
			this.newIter = lockedUntilIterator(delegate.getNew(), ctx, user,
					userConfig);
			this.sequenceNumber = delegate.sequenceNumber();
		}

		public SearchIterator getDeleted() {
			return deleted;
		}

		public SearchIterator getModified() {
			return modified;
		}

		public SearchIterator getNew() {
			return newIter;
		}

		public SearchIterator results() {
			return new CombinedSearchIterator(newIter, modified);
		}

		public long sequenceNumber() {
			return sequenceNumber;
		}

		public void close() throws SearchIteratorException {
			newIter.close();
			modified.close();
			deleted.close();
		}

	}
}
