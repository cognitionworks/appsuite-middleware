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

package com.openexchange.tools.oxfolder;

import java.sql.Connection;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.openexchange.ajax.fields.FolderFields;
import com.openexchange.api2.OXException;
import com.openexchange.cache.FolderCacheManager;
import com.openexchange.cache.FolderQueryCacheManager;
import com.openexchange.groupware.UserConfiguration;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.calendar.CalendarCache;
import com.openexchange.groupware.calendar.CalendarSql;
import com.openexchange.groupware.contact.Contacts;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.infostore.InfostoreFacade;
import com.openexchange.groupware.infostore.facade.impl.InfostoreFacadeImpl;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.tasks.Tasks;
import com.openexchange.groupware.tx.DBPoolProvider;
import com.openexchange.server.DBPool;
import com.openexchange.server.DBPoolingException;
import com.openexchange.server.EffectivePermission;
import com.openexchange.server.OCLPermission;
import com.openexchange.sessiond.SessionObject;
import com.openexchange.tools.StringCollection;
import com.openexchange.tools.oxfolder.OXFolderException.FolderCode;

/**
 * OXFolderManagerImpl implements interface
 * <code>com.openexchange.tools.oxfolder.OXFolderManager</code>
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class OXFolderManagerImpl implements OXFolderManager {

	private final Connection readCon;

	private final Connection writeCon;

	private final Context ctx;

	private final UserConfiguration userConfig;

	private final User user;

	private final SessionObject session;

	private OXFolderAccess oxfolderAccess;

	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory
			.getLog(OXFolderManagerImpl.class);

	/**
	 * Constructor which only uses <code>SessionObject</code>. Optional
	 * connection are going to be set to <code>null</code>.
	 */
	public OXFolderManagerImpl(final SessionObject session) {
		this(session, null, null);
	}

	/**
	 * Constructor which uses <code>SessionObject</code> and also uses a
	 * readable and a writeable <code>Connection</code>.
	 */
	public OXFolderManagerImpl(final SessionObject session, final Connection readCon, final Connection writeCon) {
		super();
		this.session = session;
		this.ctx = session.getContext();
		this.userConfig = session.getUserConfiguration();
		this.user = session.getUserObject();
		this.readCon = readCon;
		this.writeCon = writeCon;
	}

	private final OXFolderAccess getOXFolderAccess() {
		if (oxfolderAccess != null) {
			return oxfolderAccess;
		}
		return (oxfolderAccess = new OXFolderAccess(readCon, ctx));
	}

	private static final String PREFIX_CREATE = "Folder creation aborted: ";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.openexchange.tools.oxfolder.OXFolderManager#createFolder(com.openexchange.groupware.container.FolderObject,
	 *      boolean, long)
	 */
	public FolderObject createFolder(final FolderObject folderObj, final boolean checkPermissions, final long createTime)
			throws OXException {
		if (!folderObj.containsFolderName() || folderObj.getFolderName() == null
				|| folderObj.getFolderName().length() == 0) {
			throw new OXFolderException(FolderCode.MISSING_FOLDER_ATTRIBUTE, PREFIX_CREATE, FolderFields.TITLE, "", Integer.valueOf(ctx
					.getContextId()));
		} else if (!folderObj.containsParentFolderID()) {
			throw new OXFolderException(FolderCode.MISSING_FOLDER_ATTRIBUTE, PREFIX_CREATE, FolderFields.FOLDER_ID, "",
					Integer.valueOf(ctx.getContextId()));
		} else if (!folderObj.containsModule()) {
			throw new OXFolderException(FolderCode.MISSING_FOLDER_ATTRIBUTE, PREFIX_CREATE, FolderFields.MODULE, "",
					Integer.valueOf(ctx.getContextId()));
		} else if (!folderObj.containsType()) {
			throw new OXFolderException(FolderCode.MISSING_FOLDER_ATTRIBUTE, PREFIX_CREATE, FolderFields.TYPE, "", Integer.valueOf(ctx
					.getContextId()));
		} else if (folderObj.getPermissions() == null || folderObj.getPermissions().size() == 0) {
			throw new OXFolderException(FolderCode.MISSING_FOLDER_ATTRIBUTE, PREFIX_CREATE, FolderFields.PERMISSIONS,
					"", Integer.valueOf(ctx.getContextId()));
		}
		final FolderObject parentFolder;
		if (FolderCacheManager.isEnabled()) {
			parentFolder = FolderCacheManager.getInstance().getFolderObject(folderObj.getParentFolderID(), true, ctx,
					readCon);
		} else {
			parentFolder = FolderObject.loadFolderObjectFromDB(folderObj.getParentFolderID(), ctx, readCon);
		}
		if (checkPermissions) {
			/*
			 * Check, if user holds right to create a subfolder in given parent
			 * folder
			 */
			try {
				final EffectivePermission p = parentFolder
						.getEffectiveUserPermission(user.getId(), userConfig, readCon);
				if (!p.canCreateSubfolders()) {
					if (p.getUnderlyingPermission().canCreateSubfolders()) {
						throw new OXFolderPermissionException(FolderCode.NO_CREATE_SUBFOLDER_PERMISSION, PREFIX_CREATE,
								getUserName(user.getId(), ctx), getFolderName(parentFolder), Integer.valueOf(ctx.getContextId()));
					}
					throw new OXFolderException(FolderCode.NO_CREATE_SUBFOLDER_PERMISSION, PREFIX_CREATE,
							Category.USER_CONFIGURATION, getUserName(user.getId(), ctx), getFolderName(parentFolder),
							Integer.valueOf(ctx.getContextId()));
				} else if (parentFolder.getType() == FolderObject.PUBLIC && !userConfig.hasFullPublicFolderAccess()) {
					throw new OXFolderException(FolderCode.NO_PUBLIC_FOLDER_WRITE_ACCESS, PREFIX_CREATE, getUserName(
							user.getId(), ctx), getFolderName(parentFolder), Integer.valueOf(ctx.getContextId()));
				}
			} catch (SQLException e) {
				throw new OXFolderException(FolderCode.SQL_ERROR, PREFIX_CREATE, e, true, Integer.valueOf(ctx.getContextId()));
			} catch (DBPoolingException e) {
				throw new OXFolderException(FolderCode.DBPOOLING_ERROR, PREFIX_CREATE, e, true, Integer.valueOf(ctx.getContextId()));
			}
		}
		/*
		 * Check folder types
		 */
		if (!checkFolderTypeAgainstParentType(parentFolder, folderObj.getType())) {
			throw new OXFolderLogicException(FolderCode.INVALID_TYPE, PREFIX_CREATE, getFolderName(parentFolder),
					folderType2String(folderObj.getType()), Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Check folder module
		 */
		if (!checkFolderModuleAgainstParentModule(parentFolder, folderObj.getModule())) {
			throw new OXFolderLogicException(FolderCode.INVALID_MODULE, PREFIX_CREATE, getFolderName(parentFolder),
					folderModule2String(folderObj.getModule()), Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Check if admin exists and permission structure
		 */
		checkFolderPermissions(folderObj, user.getId(), PREFIX_CREATE, ctx);
		/*
		 * Check if duplicate folder exists
		 */
		try {
			if (OXFolderSQL.lookUpFolder(folderObj.getParentFolderID(), folderObj.getFolderName(), folderObj
					.getModule(), readCon, ctx) != -1) {
				/*
				 * A duplicate folder exists
				 */
				throw new OXFolderException(FolderCode.NO_DUPLICATE_FOLDER, PREFIX_CREATE, getFolderName(parentFolder),
						Integer.valueOf(ctx.getContextId()));
			}
		} catch (SQLException e) {
			throw new OXFolderException(FolderCode.SQL_ERROR, PREFIX_CREATE, e, true, Integer.valueOf(ctx.getContextId()));
		} catch (DBPoolingException e) {
			throw new OXFolderException(FolderCode.DBPOOLING_ERROR, PREFIX_CREATE, e, true, Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Get new folder ID
		 */
		int fuid = -1;
		try {
			fuid = OXFolderSQL.getNextSerial(ctx, writeCon);
		} catch (SQLException e) {
			throw new OXFolderException(FolderCode.SQL_ERROR, PREFIX_CREATE, e, true, Integer.valueOf(ctx.getContextId()));
		}
		if (fuid < FolderObject.MIN_FOLDER_ID) {
			throw new OXFolderException(FolderCode.INVALID_SEQUENCE_ID, PREFIX_CREATE, Integer.valueOf(fuid),
					Integer.valueOf(FolderObject.MIN_FOLDER_ID), Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Call SQL insert
		 */
		try {
			OXFolderSQL.insertFolderSQL(fuid, user.getId(), folderObj, createTime, ctx, writeCon);
		} catch (DBPoolingException e) {
			throw new OXFolderException(FolderCode.DBPOOLING_ERROR, PREFIX_CREATE, e, true, Integer.valueOf(ctx.getContextId()));
		} catch (DataTruncation e) {
			throw new OXFolderException(FolderCode.TRUNCATED, PREFIX_CREATE, e, false, folderObj.getFolderName());
		} catch (SQLException e) {
			throw new OXFolderException(FolderCode.SQL_ERROR, PREFIX_CREATE, e, true, Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Update cache with writeable connection!
		 */
		final Date creatingDate = new Date(createTime);
		folderObj.setObjectID(fuid);
		folderObj.setCreationDate(creatingDate);
		folderObj.setCreatedBy(user.getId());
		folderObj.setLastModified(creatingDate);
		folderObj.setModifiedBy(user.getId());
		folderObj.setSubfolderFlag(false);
		folderObj.setDefaultFolder(false);
		parentFolder.setSubfolderFlag(true);
		parentFolder.setLastModified(creatingDate);
		try {
			Connection wc = writeCon;
			final boolean create = (wc == null);
			try {
				if (create) {
					wc = DBPool.pickupWriteable(ctx);
				}
				// final FolderObject retval;
				if (FolderCacheManager.isInitialized()) {
					FolderCacheManager.getInstance().putFolderObject(parentFolder, ctx);
					folderObj.fill(FolderCacheManager.getInstance().getFolderObject(fuid, false, ctx, wc));
				} else {
					folderObj.fill(FolderObject.loadFolderObjectFromDB(fuid, ctx, wc));
				}
				if (FolderQueryCacheManager.isInitialized()) {
					FolderQueryCacheManager.getInstance().invalidateUserQueries(session);
				}
				if (CalendarCache.isInitialized()) {
					CalendarCache.getInstance().invalidateGroup(ctx.getContextId());
				}
				return folderObj;
			} finally {
				if (create && wc != null) {
					DBPool.closeWriterSilent(ctx, wc);
					wc = null;
				}
			}
		} catch (DBPoolingException e) {
			throw new OXFolderException(FolderCode.DBPOOLING_ERROR, PREFIX_CREATE, e, true, Integer.valueOf(ctx.getContextId()));
		}
	}

	private static final String PREFIX_UPDATE = "Folder update aborted: ";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.openexchange.tools.oxfolder.OXFolderManager#updateFolder(com.openexchange.groupware.container.FolderObject,
	 *      boolean, long)
	 */
	public FolderObject updateFolder(final FolderObject fo, final boolean checkPermissions, final long lastModified)
			throws OXException {
		if (checkPermissions) {
			if (fo.containsType() && fo.getType() == FolderObject.PUBLIC
					&& !session.getUserConfiguration().hasFullPublicFolderAccess()) {
				throw new OXFolderException(FolderCode.NO_PUBLIC_FOLDER_WRITE_ACCESS, PREFIX_UPDATE,
						getUserName(session), getFolderName(fo), Integer.valueOf(ctx.getContextId()));
			}
			/*
			 * Fetch effective permission from storage
			 */
			final EffectivePermission perm = getOXFolderAccess().getFolderPermission(fo.getObjectID(), user.getId(),
					userConfig);
			if (!perm.isFolderVisible()) {
				if (!perm.getUnderlyingPermission().isFolderVisible()) {
					throw new OXFolderPermissionException(FolderCode.NOT_VISIBLE, PREFIX_UPDATE, getFolderName(fo),
							getUserName(session), Integer.valueOf(ctx.getContextId()));
				}
				throw new OXFolderException(FolderCode.NOT_VISIBLE, PREFIX_UPDATE, Category.USER_CONFIGURATION,
						getFolderName(fo), getUserName(session), Integer.valueOf(ctx.getContextId()));
			} else if (!perm.isFolderAdmin()) {
				if (!perm.getUnderlyingPermission().isFolderAdmin()) {
					throw new OXFolderPermissionException(FolderCode.NO_ADMIN_ACCESS, PREFIX_UPDATE,
							getUserName(session), getFolderName(fo), Integer.valueOf(ctx.getContextId()));
				}
				throw new OXFolderException(FolderCode.NO_ADMIN_ACCESS, PREFIX_UPDATE, Category.USER_CONFIGURATION,
						getUserName(session), getFolderName(fo), Integer.valueOf(ctx.getContextId()));
			}
		}
		final boolean performMove = fo.containsParentFolderID();
		if (fo.containsPermissions()) {
			if (performMove) {
				move(fo.getObjectID(), fo.getParentFolderID(), lastModified);
			}
			update(fo, lastModified);
		} else if (fo.containsFolderName()) {
			if (performMove) {
				move(fo.getObjectID(), fo.getParentFolderID(), lastModified);
			}
			rename(fo, lastModified);
		} else if (performMove) {
			move(fo.getObjectID(), fo.getParentFolderID(), lastModified);
		}
		/*
		 * Finally update cache
		 */
		try {
			Connection wc = writeCon;
			final boolean create = (wc == null);
			try {
				if (create) {
					wc = DBPool.pickupWriteable(ctx);
				}
				if (FolderCacheManager.isEnabled()) {
					fo.fill(FolderCacheManager.getInstance().getFolderObject(fo.getObjectID(), false, ctx, wc));
					/*
					 * Update parent, too
					 */
					if (performMove) {
						FolderCacheManager.getInstance().loadFolderObject(fo.getParentFolderID(), ctx, wc);
					}
				} else {
					fo.fill(FolderObject.loadFolderObjectFromDB(fo.getObjectID(), ctx, wc));
				}
				if (FolderQueryCacheManager.isInitialized()) {
					FolderQueryCacheManager.getInstance().invalidateUserQueries(session);
				}
				if (CalendarCache.isInitialized()) {
					CalendarCache.getInstance().invalidateGroup(ctx.getContextId());
				}
				return fo;
			} finally {
				if (create && wc != null) {
					DBPool.closeWriterSilent(ctx, wc);
					wc = null;
				}
			}
		} catch (DBPoolingException e) {
			throw new OXFolderException(FolderCode.DBPOOLING_ERROR, PREFIX_UPDATE, e, true, Integer.valueOf(ctx.getContextId()));
		}
	}

	private final void update(final FolderObject folderObj, final long lastModified) throws OXException {
		if (folderObj.getObjectID() <= 0) {
			throw new OXFolderException(FolderCode.INVALID_OBJECT_ID, PREFIX_UPDATE, getFolderName(folderObj));
		} else if (folderObj.getPermissions() == null || folderObj.getPermissions().size() == 0) {
			throw new OXFolderException(FolderCode.MISSING_FOLDER_ATTRIBUTE, PREFIX_UPDATE, FolderFields.PERMISSIONS,
					getFolderName(folderObj), Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Get storage version (and thus implicitely check existence)
		 */
		final FolderObject storageObj;
		try {
			/*
			 * Fetch from master database, cause a move operation could be done
			 * before
			 */
			Connection wc = writeCon;
			final boolean create = (wc == null);
			try {
				if (create) {
					wc = DBPool.pickupWriteable(ctx);
				}
				storageObj = FolderObject.loadFolderObjectFromDB(folderObj.getObjectID(), ctx, wc);
			} finally {
				if (create && wc != null) {
					DBPool.closeWriterSilent(ctx, wc);
					wc = null;
				}
			}
		} catch (OXFolderNotFoundException e) {
			throw new OXFolderNotFoundException(PREFIX_UPDATE, folderObj.getObjectID(), ctx.getContextId());
		} catch (DBPoolingException e) {
			throw new OXFolderException(FolderCode.DBPOOLING_ERROR, PREFIX_UPDATE, e, true, Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Check if a move is done here
		 */
		if (folderObj.containsParentFolderID() && storageObj.getParentFolderID() != folderObj.getParentFolderID()) {
			throw new OXFolderLogicException(FolderCode.NO_MOVE_THROUGH_UPDATE, PREFIX_UPDATE, getFolderName(folderObj));
		}
		/*
		 * Check folder name
		 */
		if (folderObj.containsFolderName()) {
			if (folderObj.getFolderName() == null || folderObj.getFolderName().trim().length() == 0) {
				throw new OXFolderException(FolderCode.MISSING_FOLDER_ATTRIBUTE, PREFIX_UPDATE, FolderFields.TITLE,
						getFolderName(folderObj), Integer.valueOf(ctx.getContextId()));
			} else if (storageObj.isDefaultFolder() && !folderObj.getFolderName().equals(storageObj.getFolderName())) {
				throw new OXFolderException(FolderCode.NO_DEFAULT_FOLDER_RENAME, PREFIX_UPDATE,
						getFolderName(folderObj), Integer.valueOf(ctx.getContextId()));
			}
		}
		/*
		 * Check if shared
		 */
		if (storageObj.isShared(user.getId())) {
			throw new OXFolderException(FolderCode.NO_SHARED_FOLDER_UPDATE, PREFIX_UPDATE, getFolderName(folderObj),
					Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Check Permissions
		 */
		folderObj.setType(storageObj.getType());
		folderObj.setModule(storageObj.getModule());
		folderObj.setCreatedBy(storageObj.getCreatedBy());
		checkFolderPermissions(folderObj, user.getId(), PREFIX_UPDATE, ctx);
		/*
		 * Call SQL update
		 */
		try {
			OXFolderSQL.updateFolderSQL(user.getId(), folderObj, lastModified, ctx, writeCon);
		} catch (DBPoolingException e) {
			throw new OXFolderException(FolderCode.DBPOOLING_ERROR, PREFIX_UPDATE, e, true, Integer.valueOf(ctx.getContextId()));
		} catch (DataTruncation e) {
			throw new OXFolderException(FolderCode.TRUNCATED, PREFIX_UPDATE, e, false, folderObj.getFolderName());
		} catch (SQLException e) {
			throw new OXFolderException(FolderCode.SQL_ERROR, PREFIX_UPDATE, e, true, Integer.valueOf(ctx.getContextId()));
		}
	}

	private static final String PREFIX_RENAME = "Folder rename aborted: ";

	private final void rename(final FolderObject folderObj, final long lastModified) throws OXException {
		if (folderObj.getObjectID() <= 0) {
			throw new OXFolderException(FolderCode.INVALID_OBJECT_ID, PREFIX_RENAME, getFolderName(folderObj));
		} else if (folderObj.getFolderName() == null || folderObj.getFolderName().trim().length() == 0) {
			throw new OXFolderException(FolderCode.MISSING_FOLDER_ATTRIBUTE, PREFIX_RENAME, FolderFields.TITLE, "", Integer.valueOf(ctx
					.getContextId()));
		}
		/*
		 * Get storage version (and thus implicitely check existence)
		 */
		final FolderObject storageObj;
		try {
			/*
			 * Fetch from master database, cause a move operation could be done
			 * before
			 */
			Connection wc = writeCon;
			final boolean create = (wc == null);
			try {
				if (create) {
					wc = DBPool.pickupWriteable(ctx);
				}
				storageObj = FolderObject.loadFolderObjectFromDB(folderObj.getObjectID(), ctx, wc);
			} finally {
				if (create && wc != null) {
					DBPool.closeWriterSilent(ctx, wc);
					wc = null;
				}
			}
		} catch (OXFolderNotFoundException e) {
			throw new OXFolderNotFoundException(PREFIX_UPDATE, folderObj.getObjectID(), ctx.getContextId());
		} catch (DBPoolingException e) {
			throw new OXFolderException(FolderCode.DBPOOLING_ERROR, PREFIX_UPDATE, e, true, Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Check if rename can be avoided (cause new name equals old one) and
		 * prevent default folder rename
		 */
		if (storageObj.getFolderName().equals(folderObj.getFolderName())) {
			return;
		} else if (storageObj.isDefaultFolder()) {
			throw new OXFolderException(FolderCode.NO_DEFAULT_FOLDER_RENAME, PREFIX_RENAME, getFolderName(folderObj),
					Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Check for duplicate folder
		 */
		try {
			if (OXFolderSQL.lookUpFolder(storageObj.getParentFolderID(), folderObj.getFolderName(), storageObj
					.getModule(), readCon, ctx) != -1) {
				throw new OXFolderException(FolderCode.NO_DUPLICATE_FOLDER, PREFIX_RENAME, getFolderName(storageObj
						.getParentFolderID(), ctx), Integer.valueOf(ctx.getContextId()));
			}
		} catch (DBPoolingException e) {
			throw new OXFolderException(FolderCode.DBPOOLING_ERROR, PREFIX_RENAME, e, true, Integer.valueOf(ctx.getContextId()));
		} catch (SQLException e) {
			throw new OXFolderException(FolderCode.SQL_ERROR, PREFIX_RENAME, e, true, Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Call SQL rename
		 */
		try {
			OXFolderSQL.renameFolderSQL(user.getId(), folderObj, lastModified, ctx, writeCon);
		} catch (DBPoolingException e) {
			throw new OXFolderException(FolderCode.DBPOOLING_ERROR, PREFIX_RENAME, e, true, Integer.valueOf(ctx.getContextId()));
		} catch (DataTruncation e) {
			throw new OXFolderException(FolderCode.TRUNCATED, PREFIX_RENAME, e, false, folderObj.getFolderName());
		} catch (SQLException e) {
			throw new OXFolderException(FolderCode.SQL_ERROR, PREFIX_RENAME, e, true, Integer.valueOf(ctx.getContextId()));
		}
	}

	private static final String PREFIX_MOVE = "Folder move aborted: ";

	private final void move(final int folderId, final int targetFolderId, final long lastModified) throws OXException {
		/*
		 * Load source folder
		 */
		final FolderObject storageSrc;
		try {
			if (FolderCacheManager.isEnabled()) {
				storageSrc = FolderCacheManager.getInstance().getFolderObject(folderId, true, ctx, readCon);
			} else {
				storageSrc = FolderObject.loadFolderObjectFromDB(folderId, ctx, readCon);
			}
		} catch (OXFolderNotFoundException e) {
			throw new OXFolderNotFoundException(PREFIX_MOVE, folderId, ctx.getContextId());
		}
		/*
		 * Folder is already in target folder
		 */
		if (storageSrc.getParentFolderID() == targetFolderId) {
			return;
		}
		/*
		 * Default folder must not be moved
		 */
		if (storageSrc.isDefaultFolder()) {
			throw new OXFolderException(FolderCode.NO_DEFAULT_FOLDER_MOVE, PREFIX_MOVE, getFolderName(storageSrc), Integer.valueOf(ctx
					.getContextId()));
		}
		/*
		 * For further checks we need to load destination folder
		 */
		final FolderObject storageDest;
		try {
			if (FolderCacheManager.isEnabled()) {
				storageDest = FolderCacheManager.getInstance().getFolderObject(targetFolderId, true, ctx, readCon);
			} else {
				storageDest = FolderObject.loadFolderObjectFromDB(targetFolderId, ctx, readCon);
			}
		} catch (OXFolderNotFoundException e) {
			throw new OXFolderNotFoundException(PREFIX_MOVE, targetFolderId, ctx.getContextId());
		}
		/*
		 * Check for a duplicate folder in target folder
		 */
		try {
			if (OXFolderSQL.lookUpFolder(targetFolderId, storageSrc.getFolderName(), storageSrc.getModule(), readCon,
					ctx) != -1) {
				throw new OXFolderException(FolderCode.TARGET_FOLDER_CONTAINS_DUPLICATE, PREFIX_MOVE,
						getFolderName(storageDest), Integer.valueOf(ctx.getContextId()));
			}
		} catch (SQLException e) {
			throw new OXFolderException(FolderCode.SQL_ERROR, PREFIX_MOVE, e, true, Integer.valueOf(ctx.getContextId()));
		} catch (DBPoolingException e) {
			throw new OXFolderException(FolderCode.DBPOOLING_ERROR, PREFIX_MOVE, e, true, Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Check a bunch of possible errors
		 */
		try {
			if (storageSrc.isShared(user.getId())) {
				throw new OXFolderException(FolderCode.NO_SHARED_FOLDER_MOVE, PREFIX_MOVE, getFolderName(storageSrc),
						Integer.valueOf(ctx.getContextId()));
			} else if (storageDest.isShared(user.getId())) {
				throw new OXFolderException(FolderCode.NO_SHARED_FOLDER_TARGET, PREFIX_MOVE,
						getFolderName(storageDest), Integer.valueOf(ctx.getContextId()));
			} else if (storageSrc.getType() == FolderObject.SYSTEM_TYPE) {
				throw new OXFolderException(FolderCode.NO_SYSTEM_FOLDER_MOVE, PREFIX_MOVE, getFolderName(storageSrc),
						Integer.valueOf(ctx.getContextId()));
			} else if (storageSrc.getType() == FolderObject.PRIVATE
					&& ((storageDest.getType() == FolderObject.PUBLIC || (storageDest.getType() == FolderObject.SYSTEM_TYPE && targetFolderId != FolderObject.SYSTEM_PRIVATE_FOLDER_ID)))) {
				throw new OXFolderException(FolderCode.ONLY_PRIVATE_TO_PRIVATE_MOVE, PREFIX_MOVE,
						getFolderName(storageSrc), Integer.valueOf(ctx.getContextId()));
			} else if (storageSrc.getType() == FolderObject.PUBLIC
					&& ((storageDest.getType() == FolderObject.PRIVATE || (storageDest.getType() == FolderObject.SYSTEM_TYPE && targetFolderId != FolderObject.SYSTEM_PUBLIC_FOLDER_ID)))) {
				throw new OXFolderException(FolderCode.ONLY_PUBLIC_TO_PUBLIC_MOVE, PREFIX_MOVE,
						getFolderName(storageSrc), Integer.valueOf(ctx.getContextId()));
			} else if (storageSrc.getModule() == FolderObject.INFOSTORE
					&& storageDest.getModule() != FolderObject.INFOSTORE) {
				throw new OXFolderException(FolderCode.INCOMPATIBLE_MODULES, PREFIX_MOVE,
						folderModule2String(storageSrc.getModule()), folderModule2String(storageDest.getModule()));
			} else if (storageSrc.getModule() != FolderObject.INFOSTORE
					&& storageDest.getModule() == FolderObject.INFOSTORE) {
				throw new OXFolderException(FolderCode.INCOMPATIBLE_MODULES, PREFIX_MOVE,
						folderModule2String(storageSrc.getModule()), folderModule2String(storageDest.getModule()));
			} else if (storageDest.getEffectiveUserPermission(user.getId(), userConfig).getFolderPermission() < OCLPermission.CREATE_SUB_FOLDERS) {
				throw new OXFolderPermissionException(FolderCode.NO_CREATE_SUBFOLDER_PERMISSION, PREFIX_MOVE,
						getUserName(user.getId(), ctx), getFolderName(storageDest), Integer.valueOf(ctx.getContextId()));
			} else if (folderId == targetFolderId) {
				throw new OXFolderPermissionException(FolderCode.NO_EQUAL_MOVE, PREFIX_MOVE, Integer.valueOf(ctx.getContextId()));
			}
		} catch (SQLException e) {
			throw new OXFolderException(FolderCode.SQL_ERROR, PREFIX_MOVE, e, true, Integer.valueOf(ctx.getContextId()));
		} catch (DBPoolingException e) {
			throw new OXFolderException(FolderCode.DBPOOLING_ERROR, PREFIX_MOVE, e, true, Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Check if source folder has subfolders
		 */
		try {
			if (storageSrc.hasSubfolders()) {
				/*
				 * Check if target is a descendant folder
				 */
				final List<Integer> parentIDList = new ArrayList<Integer>(1);
				parentIDList.add(Integer.valueOf(storageSrc.getObjectID()));
				if (isDescendantFolder(parentIDList, targetFolderId, readCon, ctx)) {
					throw new OXFolderException(FolderCode.NO_SUBFOLDER_MOVE, PREFIX_MOVE, getFolderName(storageSrc),
							Integer.valueOf(ctx.getContextId()));
				}
				/*
				 * Count all moveable subfolders: TODO: Recursive check???
				 */
				final int numOfMoveableSubfolders = OXFolderSQL.getNumOfMoveableSubfolders(storageSrc.getObjectID(),
						user.getId(), user.getGroups(), readCon, ctx);
				if (numOfMoveableSubfolders != storageSrc.getSubfolderIds(true, ctx).size()) {
					throw new OXFolderPermissionException(FolderCode.NO_SUBFOLDER_MOVE_ACCESS, PREFIX_MOVE,
							getUserName(session), getFolderName(storageSrc), Integer.valueOf(ctx.getContextId()));
				}
			}
		} catch (SQLException e) {
			throw new OXFolderException(FolderCode.SQL_ERROR, PREFIX_MOVE, e, true, Integer.valueOf(ctx.getContextId()));
		} catch (DBPoolingException e) {
			throw new OXFolderException(FolderCode.DBPOOLING_ERROR, PREFIX_MOVE, e, true, Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Call SQL move
		 */
		try {
			OXFolderSQL.moveFolderSQL(user.getId(), storageSrc, storageDest, lastModified, ctx, readCon, writeCon);
		} catch (DataTruncation e) {
			throw new OXFolderException(FolderCode.TRUNCATED, PREFIX_MOVE, e, false, storageSrc.getFolderName());
		} catch (SQLException e) {
			throw new OXFolderException(FolderCode.SQL_ERROR, PREFIX_MOVE, e, true, Integer.valueOf(ctx.getContextId()));
		} catch (DBPoolingException e) {
			throw new OXFolderException(FolderCode.DBPOOLING_ERROR, PREFIX_MOVE, e, true, Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Update OLD parent in cache, cause this can only be done here
		 */
		if (FolderCacheManager.isEnabled()) {
			try {
				Connection wc = writeCon;
				final boolean create = (wc == null);
				try {
					if (create) {
						wc = DBPool.pickupWriteable(ctx);
					}
					FolderCacheManager.getInstance().loadFolderObject(storageSrc.getParentFolderID(), ctx, wc);
				} finally {
					if (create && wc != null) {
						DBPool.closeWriterSilent(ctx, wc);
					}
				}
			} catch (DBPoolingException e) {
				throw new OXFolderException(FolderCode.DBPOOLING_ERROR, PREFIX_MOVE, e, true, Integer.valueOf(ctx.getContextId()));
			}
		}
	}

	private static final boolean isDescendantFolder(final List<Integer> parentIDList, final int possibleDescendant,
			final Connection readCon, final Context ctx) throws SQLException, DBPoolingException {
		final int size = parentIDList.size();
		final Iterator<Integer> iter = parentIDList.iterator();
		boolean isDescendant = false;
		for (int i = 0; i < size && !isDescendant; i++) {
			final List<Integer> subfolderIDs = OXFolderSQL.getSubfolderIDs(iter.next().intValue(), readCon, ctx);
			final int subsize = subfolderIDs.size();
			final Iterator<Integer> subiter = subfolderIDs.iterator();
			for (int j = 0; j < subsize && !isDescendant; j++) {
				final int current = subiter.next().intValue();
				isDescendant |= (current == possibleDescendant);
			}
			if (isDescendant) {
				/*
				 * Matching descendant found
				 */
				return true;
			}
			/*
			 * Recursive call with collected subfolder ids
			 */
			isDescendant = isDescendantFolder(subfolderIDs, possibleDescendant, readCon, ctx);
		}
		return isDescendant;
	}

	private static final String PREFIX_DELETE = "Folder deletion aborted: ";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.openexchange.tools.oxfolder.OXFolderManager#deleteFolder(com.openexchange.groupware.container.FolderObject,
	 *      boolean, long)
	 */
	public FolderObject deleteFolder(final FolderObject fo, final boolean checkPermissions, final long lastModified)
			throws OXException {
		if (fo.getObjectID() <= 0) {
			throw new OXFolderException(FolderCode.INVALID_OBJECT_ID, PREFIX_DELETE, getFolderName(fo));
		}
		if (!fo.containsParentFolderID() || fo.getParentFolderID() <= 0) {
			/*
			 * Incomplete, wherby its existence is checked
			 */
			fo.setParentFolderID(getOXFolderAccess().getParentFolderID(fo.getObjectID()));
		} else {
			/*
			 * Check existence
			 */
			try {
				if (!OXFolderSQL.exists(fo.getObjectID(), readCon, ctx)) {
					throw new OXFolderNotFoundException(fo.getObjectID(), ctx.getContextId());
				}
			} catch (DBPoolingException e) {
				throw new OXFolderException(FolderCode.DBPOOLING_ERROR, PREFIX_DELETE, e, true, Integer.valueOf(ctx.getContextId()));
			} catch (SQLException e) {
				throw new OXFolderException(FolderCode.SQL_ERROR, PREFIX_DELETE, e, true, Integer.valueOf(ctx.getContextId()));
			}
		}
		if (checkPermissions) {
			/*
			 * Check permissions
			 */
			final EffectivePermission p = getOXFolderAccess().getFolderPermission(fo.getObjectID(), user.getId(),
					userConfig);
			if (!p.isFolderVisible()) {
				if (p.getUnderlyingPermission().isFolderVisible()) {
					throw new OXFolderPermissionException(FolderCode.NOT_VISIBLE, PREFIX_DELETE, getFolderName(fo),
							getUserName(user.getId(), ctx), Integer.valueOf(ctx.getContextId()));
				}
				throw new OXFolderException(FolderCode.NOT_VISIBLE, PREFIX_DELETE, Category.USER_CONFIGURATION,
						getFolderName(fo), getUserName(user.getId(), ctx), Integer.valueOf(ctx.getContextId()));
			} else if (!p.isFolderAdmin()) {
				if (!p.getUnderlyingPermission().isFolderAdmin()) {
					throw new OXFolderPermissionException(FolderCode.NO_ADMIN_ACCESS, PREFIX_DELETE, getUserName(user
							.getId(), ctx), getFolderName(fo), Integer.valueOf(ctx.getContextId()));
				}
				throw new OXFolderException(FolderCode.NO_ADMIN_ACCESS, PREFIX_DELETE, Category.USER_CONFIGURATION,
						getUserName(user.getId(), ctx), getFolderName(fo), Integer.valueOf(ctx.getContextId()));
			}
		}
		/*
		 * Get parent
		 */
		final FolderObject parentObj = getOXFolderAccess().getFolderObject(fo.getParentFolderID());
		/*
		 * Gather all deleteable subfolders
		 */
		final HashMap<Integer, HashMap> deleteableFolders;
		try {
			deleteableFolders = gatherDeleteableFolders(fo.getObjectID(), user.getId(), userConfig, StringCollection
					.getSqlInString(user.getId(), user.getGroups()));
		} catch (DBPoolingException e) {
			throw new OXFolderException(FolderCode.DBPOOLING_ERROR, PREFIX_MOVE, e, true, Integer.valueOf(ctx.getContextId()));
		} catch (SQLException e) {
			throw new OXFolderException(FolderCode.SQL_ERROR, PREFIX_MOVE, e, true, Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Delete folders
		 */
		deleteValidatedFolders(deleteableFolders, lastModified);
		/*
		 * Invalidate query caches
		 */
		if (FolderQueryCacheManager.isInitialized()) {
			FolderQueryCacheManager.getInstance().invalidateUserQueries(session);
		}
		if (CalendarCache.isInitialized()) {
			CalendarCache.getInstance().invalidateGroup(ctx.getContextId());
		}
		/*
		 * Continue
		 */
		try {
			Connection wc = writeCon;
			final boolean create = (wc == null);
			try {
				if (create) {
					wc = DBPool.pickupWriteable(ctx);
				}
				/*
				 * Check parent subfolder flag
				 */
				final boolean hasSubfolders = (OXFolderSQL.getSubfolderIDs(parentObj.getObjectID(), wc, ctx).size() > 0);
				OXFolderSQL.updateSubfolderFlag(parentObj.getObjectID(), hasSubfolders, lastModified, wc, ctx);
				/*
				 * Update cache
				 */
				if (FolderCacheManager.isEnabled() && FolderCacheManager.isInitialized()) {
					FolderCacheManager.getInstance().loadFolderObject(parentObj.getObjectID(), ctx, wc);
				}
				/*
				 * Load return value
				 */
				return fo.fill(FolderObject.loadFolderObjectFromDB(fo.getObjectID(), ctx, wc, true, false,
						"del_oxfolder_tree", "del_oxfolder_permissions"));
			} finally {
				if (create && wc != null) {
					DBPool.closeWriterSilent(ctx, wc);
				}
			}
		} catch (DBPoolingException e) {
			throw new OXFolderException(FolderCode.DBPOOLING_ERROR, PREFIX_MOVE, e, true, Integer.valueOf(ctx.getContextId()));
		} catch (SQLException e) {
			throw new OXFolderException(FolderCode.SQL_ERROR, PREFIX_MOVE, e, true, Integer.valueOf(ctx.getContextId()));
		}

	}

	@SuppressWarnings("unchecked")
	private final void deleteValidatedFolders(final HashMap<Integer, HashMap> deleteableIDs, final long lastModified)
			throws OXException {
		final int deleteableIDsSize = deleteableIDs.size();
		final Iterator<Map.Entry<Integer, HashMap>> iter = deleteableIDs.entrySet().iterator();
		for (int i = 0; i < deleteableIDsSize; i++) {
			final Map.Entry<Integer, HashMap> entry = iter.next();
			final Integer folderID = entry.getKey();
			final HashMap<Integer, HashMap> hashMap = entry.getValue();
			/*
			 * Delete subfolders first, if any exist
			 */
			if (hashMap != null) {
				deleteValidatedFolders(hashMap, lastModified);
			}
			deleteValidatedFolder(folderID.intValue(), lastModified);
		}
	}

	private final void deleteValidatedFolder(final int folderID, final long lastModified) throws OXException {
		/*
		 * Delete folder
		 */
		final int module = getOXFolderAccess().getFolderModule(folderID);
		switch (module) {
		case FolderObject.CALENDAR:
			deleteContainedAppointments(folderID);
			break;
		case FolderObject.TASK:
			deleteContainedTasks(folderID);
			break;
		case FolderObject.CONTACT:
			deleteContainedContacts(folderID);
			break;
		case FolderObject.UNBOUND:
			break;
		case FolderObject.INFOSTORE:
			deleteContainedDocuments(folderID);
			break;
		case FolderObject.PROJECT:
			// TODO: Delete all projects in project folder
			break;
		default:
			throw new OXFolderException(FolderCode.UNKNOWN_MODULE, PREFIX_DELETE, Integer.valueOf(module), Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Call SQL delete
		 */
		try {
			OXFolderSQL.delWorkingOXFolder(folderID, session.getUserObject().getId(), lastModified, ctx, writeCon);
		} catch (DBPoolingException e) {
			throw new OXFolderException(FolderCode.DBPOOLING_ERROR, "", e, true, Integer.valueOf(ctx.getContextId()));
		} catch (SQLException e) {
			throw new OXFolderException(FolderCode.SQL_ERROR, "", e, true, Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Remove from cache
		 */
		if (FolderQueryCacheManager.isInitialized()) {
			FolderQueryCacheManager.getInstance().invalidateContextQueries(ctx.getContextId());
		}
		if (CalendarCache.isInitialized()) {
			CalendarCache.getInstance().invalidateGroup(ctx.getContextId());
		}
		if (FolderCacheManager.isEnabled() && FolderCacheManager.isInitialized()) {
			FolderCacheManager.getInstance().removeFolderObject(folderID, ctx);
		}
	}

	private final void deleteContainedAppointments(final int folderID) throws OXException {
		final CalendarSql cSql = new CalendarSql(session);
		try {
			cSql.deleteAppointmentsInFolder(folderID);
		} catch (SQLException e) {
			throw new OXFolderException(FolderCode.SQL_ERROR, "", e, true, Integer.valueOf(ctx.getContextId()));
		}
	}

	private final void deleteContainedTasks(final int folderID) throws OXException {
		final Tasks tasks = Tasks.getInstance();
		tasks.deleteTasksInFolder(session, folderID);
	}

	private final void deleteContainedContacts(final int folderID) throws OXException {
		try {
			Connection readCon = this.readCon;
			Connection writeCon = this.writeCon;
			final boolean createReadCon = (readCon == null);
			final boolean createWriteCon = (writeCon == null);
			if (createReadCon) {
				readCon = DBPool.pickup(ctx);
			}
			if (createWriteCon) {
				writeCon = DBPool.pickupWriteable(ctx);
			}
			try {
				Contacts.trashContactsFromFolder(folderID, session, readCon, writeCon, false);
			} finally {
				if (createReadCon && readCon != null) {
					try {
						DBPool.push(ctx, readCon);
					} catch (DBPoolingException e) {
						/*
						 * Just log here cause nevertheless writeable connection
						 * should be closed
						 */
						LOG.error(e.getMessage(), e);
					}
				}
				if (createWriteCon && writeCon != null) {
					DBPool.pushWrite(ctx, writeCon);
				}
			}
		} catch (DBPoolingException e) {
			throw new OXFolderException(FolderCode.DBPOOLING_ERROR, "", e, true, Integer.valueOf(ctx.getContextId()));
		}
	}

	private final void deleteContainedDocuments(final int folderID) throws OXException {
		final InfostoreFacade db = new InfostoreFacadeImpl(new DBPoolProvider());
		db.removeDocument(folderID, System.currentTimeMillis(), session);
	}

	/**
	 * Gathers all deleteable folders
	 */
	private final HashMap<Integer, HashMap> gatherDeleteableFolders(final int folderID, final int userId,
			final UserConfiguration userConfig, final String permissionIDs) throws OXException, DBPoolingException,
			SQLException {
		final HashMap<Integer, HashMap> deleteableIDs = new HashMap<Integer, HashMap>();
		gatherDeleteableSubfoldersRecursively(folderID, userId, userConfig, permissionIDs, deleteableIDs);
		return deleteableIDs;
	}

	/**
	 * Gathers all deleteable folders
	 */
	private final void gatherDeleteableSubfoldersRecursively(final int folderID, final int userId,
			final UserConfiguration userConfig, final String permissionIDs,
			final HashMap<Integer, HashMap> deleteableIDs) throws OXException, DBPoolingException, SQLException {
		final FolderObject delFolder = getOXFolderAccess().getFolderObject(folderID);
		/*
		 * Check if shared
		 */
		if (delFolder.isShared(userId)) {
			throw new OXFolderPermissionException(FolderCode.NO_SHARED_FOLDER_DELETION, PREFIX_DELETE, getUserName(
					userId, ctx), getFolderName(folderID, ctx), Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Check if marked as default folder
		 */
		if (delFolder.isDefaultFolder()) {
			throw new OXFolderPermissionException(FolderCode.NO_DEFAULT_FOLDER_DELETION, PREFIX_DELETE, getUserName(
					userId, ctx), getFolderName(folderID, ctx), Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Check user's effective permission
		 */
		final EffectivePermission effectivePerm = getOXFolderAccess().getFolderPermission(folderID, userId, userConfig);
		if (!effectivePerm.isFolderVisible()) {
			if (!effectivePerm.getUnderlyingPermission().isFolderVisible()) {
				throw new OXFolderPermissionException(FolderCode.NOT_VISIBLE, PREFIX_DELETE, getFolderName(folderID,
						ctx), getUserName(userId, ctx), Integer.valueOf(ctx.getContextId()));
			}
			throw new OXFolderException(FolderCode.NOT_VISIBLE, PREFIX_DELETE, Category.USER_CONFIGURATION,
					getFolderName(folderID, ctx), getUserName(userId, ctx), Integer.valueOf(ctx.getContextId()));
		} else if (!effectivePerm.isFolderAdmin()) {
			if (!effectivePerm.getUnderlyingPermission().isFolderAdmin()) {
				throw new OXFolderPermissionException(FolderCode.NO_ADMIN_ACCESS, PREFIX_DELETE, getUserName(userId,
						ctx), getFolderName(folderID, ctx), Integer.valueOf(ctx.getContextId()));
			}
			throw new OXFolderException(FolderCode.NO_ADMIN_ACCESS, PREFIX_DELETE, Category.USER_CONFIGURATION,
					getUserName(userId, ctx), getFolderName(folderID, ctx), Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Check delete permission on folder's objects
		 */
		if (!getOXFolderAccess().canDeleteAllObjectsInFolder(delFolder, session)) {
			throw new OXFolderPermissionException(FolderCode.NOT_ALL_OBJECTS_DELETION, PREFIX_DELETE, getUserName(
					userId, ctx), getFolderName(folderID, ctx), Integer.valueOf(ctx.getContextId()));
		}
		/*
		 * Check, if folder has subfolders
		 */
		if (!delFolder.hasSubfolders()) {
			deleteableIDs.put(Integer.valueOf(folderID), null);
			return;
		}
		/*
		 * No subfolders detected
		 */
		final List<Integer> subfolders = OXFolderSQL.getSubfolderIDs(delFolder.getObjectID(), readCon, ctx);
		if (subfolders.size() == 0) {
			deleteableIDs.put(Integer.valueOf(folderID), null);
			return;
		}
		final HashMap<Integer, HashMap> subMap = new HashMap<Integer, HashMap>();
		final int size = subfolders.size();
		final Iterator<Integer> it = subfolders.iterator();
		for (int i = 0; i < size; i++) {
			final int fuid = it.next().intValue();
			gatherDeleteableSubfoldersRecursively(fuid, userId, userConfig, permissionIDs, subMap);
		}
		deleteableIDs.put(Integer.valueOf(folderID), subMap);
	}

	/**
	 * This routine is called through AJAX' folder tests!
	 */
	public void cleanUpTestFolders(final int[] fuids, final Context ctx) {
		for (int i = 0; i < fuids.length; i++) {
			try {
				OXFolderSQL.hardDeleteOXFolder(fuids[i], ctx, null);
				if (FolderCacheManager.isEnabled() && FolderCacheManager.isInitialized()) {
					try {
						FolderCacheManager.getInstance().removeFolderObject(fuids[i], ctx);
					} catch (OXException e) {
						LOG.warn(e.getMessage(), e);
					}
				}
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}
	}

	// ----------------- static helper methods ----------------------

	private static final boolean checkFolderTypeAgainstParentType(final FolderObject parentFolder,
			final int newFolderType) {
		final int enforcedType;
		switch (parentFolder.getObjectID()) {
		case FolderObject.SYSTEM_PRIVATE_FOLDER_ID:
			enforcedType = FolderObject.PRIVATE;
			break;
		case FolderObject.SYSTEM_PUBLIC_FOLDER_ID:
			enforcedType = FolderObject.PUBLIC;
			break;
		case FolderObject.SYSTEM_INFOSTORE_FOLDER_ID:
			enforcedType = FolderObject.PUBLIC;
			break;
		default:
			enforcedType = parentFolder.getType();
		}
		return (newFolderType == enforcedType);
	}

	private static final boolean checkFolderModuleAgainstParentModule(final FolderObject parentFolder,
			final int newFolderModule) throws OXException {
		final int[] sortedStandardModules = new int[] { FolderObject.TASK, FolderObject.CALENDAR, FolderObject.CONTACT,
				FolderObject.UNBOUND };
		switch (parentFolder.getModule()) {
		case FolderObject.TASK:
		case FolderObject.CALENDAR:
		case FolderObject.CONTACT:
			return (Arrays.binarySearch(sortedStandardModules, newFolderModule) >= 0);
		case FolderObject.SYSTEM_MODULE:
			if (parentFolder.getObjectID() == FolderObject.SYSTEM_PRIVATE_FOLDER_ID
					|| parentFolder.getObjectID() == FolderObject.SYSTEM_PUBLIC_FOLDER_ID) {
				return (Arrays.binarySearch(sortedStandardModules, newFolderModule) >= 0);
			} else if (parentFolder.getObjectID() == FolderObject.SYSTEM_INFOSTORE_FOLDER_ID) {
				return (newFolderModule == FolderObject.INFOSTORE);
			}
			break;
		case FolderObject.PROJECT:
			return (newFolderModule == FolderObject.PROJECT);
		case FolderObject.INFOSTORE:
			return (newFolderModule == FolderObject.INFOSTORE);
		default:
			throw new OXFolderException(FolderCode.UNKNOWN_MODULE, PREFIX_CREATE, Integer.valueOf(parentFolder.getModule()), "");
		}
		return true;
	}

	private static final void checkFolderPermissions(final FolderObject folderObj, final int userId,
			final String excPrefix, final Context ctx) throws OXException {
		final boolean isPrivate = (folderObj.getType() == FolderObject.PRIVATE || folderObj.getType() == FolderObject.SHARED);
		int adminPermissionCount = 0;
		final int permissionsSize = folderObj.getPermissions().size();
		final Iterator<OCLPermission> iter = folderObj.getPermissions().iterator();
		for (int i = 0; i < permissionsSize; i++) {
			final OCLPermission oclPerm = iter.next();
			if (oclPerm.getEntity() < 0) {
				throw new OXFolderException(FolderCode.INVALID_ENTITY, excPrefix, Integer.valueOf(oclPerm.getEntity()),
						getFolderName(folderObj), Integer.valueOf(ctx.getContextId()));
			}
			if (oclPerm.isFolderAdmin()) {
				adminPermissionCount++;
				if (isPrivate && folderObj.getModule() != FolderObject.SYSTEM_MODULE) {
					if (adminPermissionCount > 1) {
						throw new OXFolderLogicException(FolderCode.ONLY_ONE_PRIVATE_FOLDER_ADMIN, excPrefix);
					}
					if (oclPerm.isGroupPermission()) {
						throw new OXFolderLogicException(FolderCode.NO_PRIVATE_FOLDER_ADMIN_GROUP, excPrefix);
					}
					if (folderObj.containsCreatedBy()) {
						if (folderObj.getCreatedBy() != oclPerm.getEntity()) {
							throw new OXFolderLogicException(FolderCode.ONLY_PRIVATE_FOLDER_OWNER_ADMIN, excPrefix);
						}
					} else if (userId != oclPerm.getEntity()) {
						throw new OXFolderLogicException(FolderCode.ONLY_PRIVATE_FOLDER_OWNER_ADMIN, excPrefix);
					}
				}
			}
		}
		if (adminPermissionCount == 0) {
			throw new OXFolderLogicException(FolderCode.NO_FOLDER_ADMIN, excPrefix);
		}
	}
	
	private static final String STR_EMPTY = "";

	public static final String getFolderName(final FolderObject fo) {
		return new StringBuilder().append(fo.getFolderName() == null ? STR_EMPTY : fo.getFolderName()).append(" (")
				.append(fo.getObjectID()).append(')').toString();
	}

	public static final String getFolderName(final int folderId, final Context ctx) {
		try {
			return new StringBuilder().append(new OXFolderAccess(ctx).getFolderName(folderId)).append(" (").append(
					folderId).append(')').toString();
		} catch (OXException e) {
			return String.valueOf(folderId);
		}
	}

	public static final String getUserName(final SessionObject sessionObj) {
		if (sessionObj == null) {
			return "";
		}
		final User u = sessionObj.getUserObject();
		if (u.getDisplayName() == null) {
			return new StringBuilder().append(u.getGivenName()).append(' ').append(u.getSurname()).append(" (").append(
					u.getId()).append(')').toString();
		}
		return new StringBuilder().append(u.getDisplayName()).append(" (").append(u.getId()).append(')').toString();
	}

	public static final String getUserName(final int userId, final Context ctx) {
		final User u;
		try {
			u = UserStorage.getInstance(ctx).getUser(userId);
		} catch (LdapException e) {
			return String.valueOf(userId);
		}
		if (u == null) {
			return String.valueOf(userId);
		} else if (u.getDisplayName() == null) {
			return new StringBuilder().append(u.getGivenName()).append(' ').append(u.getSurname()).append(" (").append(
					userId).append(')').toString();
		}
		return new StringBuilder().append(u.getDisplayName()).append(" (").append(userId).append(')').toString();
	}

	private static final String STR_TYPE_PRIVATE = "'private'";

	private static final String STR_TYPE_PUBLIC = "'public'";

	private static final String STR_SYSTEM = "'system'";

	private static final String STR_UNKNOWN = "'unknown'";

	public static final String folderType2String(final int type) {
		switch (type) {
		case FolderObject.PRIVATE:
			return STR_TYPE_PRIVATE;
		case FolderObject.PUBLIC:
			return STR_TYPE_PUBLIC;
		case FolderObject.SYSTEM_TYPE:
			return STR_SYSTEM;
		default:
			return STR_UNKNOWN;
		}
	}

	private static final String STR_MODULE_CALENDAR = "'calendar'";

	private static final String STR_MODULE_TASK = "'task'";

	private static final String STR_MODULE_CONTACT = "'contact'";

	private static final String STR_MODULE_UNBOUND = "'unbound'";

	private static final String STR_MODULE_PROJECT = "'project'";

	private static final String STR_MODULE_INFOSTORE = "'infostore'";

	public static final String folderModule2String(final int module) {
		switch (module) {
		case FolderObject.CALENDAR:
			return STR_MODULE_CALENDAR;
		case FolderObject.TASK:
			return STR_MODULE_TASK;
		case FolderObject.CONTACT:
			return STR_MODULE_CONTACT;
		case FolderObject.UNBOUND:
			return STR_MODULE_UNBOUND;
		case FolderObject.SYSTEM_MODULE:
			return STR_SYSTEM;
		case FolderObject.PROJECT:
			return STR_MODULE_PROJECT;
		case FolderObject.INFOSTORE:
			return STR_MODULE_INFOSTORE;
		default:
			return STR_UNKNOWN;
		}
	}

}
