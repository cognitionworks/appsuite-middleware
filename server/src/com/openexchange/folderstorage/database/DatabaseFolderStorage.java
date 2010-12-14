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

package com.openexchange.folderstorage.database;

import static com.openexchange.folderstorage.database.DatabaseFolderStorageUtility.getUnsignedInteger;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import com.openexchange.api2.OXException;
import com.openexchange.cache.impl.FolderCacheManager;
import com.openexchange.database.DBPoolingException;
import com.openexchange.database.DatabaseService;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.Folder;
import com.openexchange.folderstorage.FolderException;
import com.openexchange.folderstorage.FolderExceptionErrorMessage;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.FolderType;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.SortableId;
import com.openexchange.folderstorage.StorageParameters;
import com.openexchange.folderstorage.StoragePriority;
import com.openexchange.folderstorage.StorageType;
import com.openexchange.folderstorage.SystemContentType;
import com.openexchange.folderstorage.Type;
import com.openexchange.folderstorage.database.contentType.CalendarContentType;
import com.openexchange.folderstorage.database.contentType.ContactContentType;
import com.openexchange.folderstorage.database.contentType.InfostoreContentType;
import com.openexchange.folderstorage.database.contentType.TaskContentType;
import com.openexchange.folderstorage.database.contentType.UnboundContentType;
import com.openexchange.folderstorage.database.getfolder.SharedPrefixFolder;
import com.openexchange.folderstorage.database.getfolder.SystemInfostoreFolder;
import com.openexchange.folderstorage.database.getfolder.SystemPrivateFolder;
import com.openexchange.folderstorage.database.getfolder.SystemPublicFolder;
import com.openexchange.folderstorage.database.getfolder.SystemRootFolder;
import com.openexchange.folderstorage.database.getfolder.SystemSharedFolder;
import com.openexchange.folderstorage.database.getfolder.VirtualListFolder;
import com.openexchange.folderstorage.type.PrivateType;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.folderstorage.type.SharedType;
import com.openexchange.folderstorage.type.SystemType;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.i18n.FolderStrings;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.tools.iterator.FolderObjectIterator;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.server.ServiceException;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.session.Session;
import com.openexchange.tools.iterator.SearchIteratorException;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.oxfolder.OXFolderBatchLoader;
import com.openexchange.tools.oxfolder.OXFolderException;
import com.openexchange.tools.oxfolder.OXFolderIteratorSQL;
import com.openexchange.tools.oxfolder.OXFolderManager;
import com.openexchange.tools.oxfolder.OXFolderNotFoundException;
import com.openexchange.tools.oxfolder.OXFolderSQL;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.tools.sql.DBUtils;

/**
 * {@link DatabaseFolderStorage} - The database folder storage.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class DatabaseFolderStorage implements FolderStorage {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DatabaseFolderStorage.class);

    /**
     * Initializes a new {@link DatabaseFolderStorage}.
     */
    public DatabaseFolderStorage() {
        super();
    }

    public void checkConsistency(final String treeId, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getConnection(storageParameters);
            final ServerSession session;
            {
                final Session s = storageParameters.getSession();
                if (null == s) {
                    throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
                }
                if (s instanceof ServerSession) {
                    session = ((ServerSession) s);
                } else {
                    session = new ServerSessionAdapter(s);
                }
            }
            /*
             * Determine folder with non-existing parents
             */
            int[] nonExistingParents = OXFolderSQL.getNonExistingParents(session.getContext(), con);
            final TIntHashSet shared = new TIntHashSet();
            final OXFolderManager manager = OXFolderManager.getInstance(session, con, con);
            final OXFolderAccess folderAccess = getFolderAccess(storageParameters);
            final int userId = session.getUserId();
            final long now = System.currentTimeMillis();
            do {
                for (final int folderId : nonExistingParents) {
                    if (folderId >= FolderObject.MIN_FOLDER_ID) {
                        if (FolderObject.SHARED == folderAccess.getFolderType(folderId, userId)) {
                            shared.add(folderId);
                        } else {
                            manager.deleteValidatedFolder(folderId, now, -1, true);
                        }
                    }
                }
                final TIntHashSet tmp = new TIntHashSet(OXFolderSQL.getNonExistingParents(session.getContext(), con));
                tmp.removeAll(shared.toArray());
                for (int i = 0; i < FolderObject.MIN_FOLDER_ID; i++) {
                    tmp.remove(i);
                }
                nonExistingParents = tmp.toArray();
            } while (null != nonExistingParents && nonExistingParents.length > 0);
        } catch (final OXException e) {
            throw new FolderException(e);
        } catch (ContextException e) {
            throw new FolderException(e);
        }
    }

    public ContentType[] getSupportedContentTypes() {
        return new ContentType[] {
            TaskContentType.getInstance(), CalendarContentType.getInstance(), ContactContentType.getInstance(),
            InfostoreContentType.getInstance(), UnboundContentType.getInstance(), SystemContentType.getInstance() };
    }

    public ContentType getDefaultContentType() {
        return ContactContentType.getInstance();
    }

    public void commitTransaction(final StorageParameters params) throws FolderException {
        final Connection con;
        final Boolean writable;
        try {
            con = getConnection(params);
            writable = getParameter(Boolean.class, DatabaseParameterConstants.PARAM_WRITABLE, params);
        } catch (final FolderException e) {
            /*
             * Already committed
             */
            if (LOG.isWarnEnabled()) {
                LOG.warn("Storage already committed:\n" + params.getCommittedTrace(), e);
            }
            return;
        }
        try {
            con.commit();
        } catch (final SQLException e) {
            throw FolderExceptionErrorMessage.SQL_ERROR.create(e, e.getMessage());
        } finally {
            DBUtils.autocommit(con);
            final DatabaseService databaseService = DatabaseServiceRegistry.getServiceRegistry().getService(DatabaseService.class);
            if (null != databaseService) {
                if (writable.booleanValue()) {
                    databaseService.backWritable(params.getContext(), con);
                } else {
                    databaseService.backReadOnly(params.getContext(), con);
                }
            }
            final FolderType folderType = getFolderType();
            params.putParameter(folderType, DatabaseParameterConstants.PARAM_CONNECTION, null);
            params.putParameter(folderType, DatabaseParameterConstants.PARAM_WRITABLE, null);
            params.markCommitted();
        }
    }

    public void restore(final String treeId, final String folderIdentifier, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getConnection(storageParameters);
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }
            final int folderId = Integer.parseInt(folderIdentifier);
            final Context context = storageParameters.getContext();
            FolderObject.loadFolderObjectFromDB(folderId, context, con, false, false, "del_oxfolder_tree", "del_oxfolder_permissions");
            /*
             * From backup to working table
             */
            OXFolderSQL.restore(folderId, context, null);
        } catch (final NumberFormatException e) {
            throw FolderExceptionErrorMessage.INVALID_FOLDER_ID.create(folderIdentifier);
        } catch (final OXException e) {
            throw new FolderException(e);
        } catch (final DBPoolingException e) {
            throw new FolderException(e);
        } catch (final SQLException e) {
            throw FolderExceptionErrorMessage.SQL_ERROR.create(e, e.getMessage());
        }
    }

    public void createFolder(final Folder folder, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getConnection(storageParameters);
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }
            final long millis = System.currentTimeMillis();

            final FolderObject createMe = new FolderObject();
            createMe.setCreatedBy(session.getUserId());
            createMe.setCreationDate(new Date(millis));
            createMe.setCreator(session.getUserId());
            createMe.setDefaultFolder(false);
            {
                final String name = folder.getName();
                if (null != name) {
                    createMe.setFolderName(name);
                }
            }
            createMe.setLastModified(new Date(millis));
            createMe.setModifiedBy(session.getUserId());
            {
                final ContentType ct = folder.getContentType();
                if (null != ct) {
                    createMe.setModule(getModuleByContentType(ct));
                }
            }
            {
                final String parentId = folder.getParentID();
                if (null != parentId) {
                    createMe.setParentFolderID(Integer.parseInt(parentId));
                }
            }
            {
                final Type t = folder.getType();
                if (null == t) {
                    /*
                     * Determine folder type by examining parent folder
                     */
                    createMe.setType(getFolderType(createMe.getParentFolderID(), storageParameters));
                } else {
                    createMe.setType(getTypeByFolderType(t));
                }
            }
            // Permissions
            final Permission[] perms = folder.getPermissions();
            if (null != perms) {
                final OCLPermission[] oclPermissions = new OCLPermission[perms.length];
                for (int i = 0; i < perms.length; i++) {
                    final Permission p = perms[i];
                    final OCLPermission oclPerm = new OCLPermission();
                    oclPerm.setEntity(p.getEntity());
                    oclPerm.setGroupPermission(p.isGroup());
                    oclPerm.setFolderAdmin(p.isAdmin());
                    oclPerm.setAllPermission(
                        p.getFolderPermission(),
                        p.getReadPermission(),
                        p.getWritePermission(),
                        p.getDeletePermission());
                    oclPerm.setSystem(p.getSystem());
                    oclPermissions[i] = oclPerm;
                }
                createMe.setPermissionsAsArray(oclPermissions);
            }
            // Create
            final OXFolderManager folderManager = OXFolderManager.getInstance(session, con, con);
            folderManager.createFolder(createMe, true, millis);
            folder.setID(String.valueOf(createMe.getObjectID()));
        } catch (final OXException e) {
            throw new FolderException(e);
        }
    }

    private static final int[] PUBLIC_FOLDER_IDS =
        {
            FolderObject.SYSTEM_PUBLIC_FOLDER_ID, FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID,
            FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID };

    private static int getFolderType(final int parentId, final StorageParameters storageParameters) throws OXException, FolderException {
        int type = -1;
        int pid = parentId;
        /*
         * Special treatment for system folders
         */
        if (pid == FolderObject.SYSTEM_SHARED_FOLDER_ID) {
            pid = FolderObject.SYSTEM_PRIVATE_FOLDER_ID;
            type = FolderObject.SHARED;
        } else if (pid == FolderObject.SYSTEM_PRIVATE_FOLDER_ID) {
            type = FolderObject.PRIVATE;
        } else if (Arrays.binarySearch(PUBLIC_FOLDER_IDS, pid) >= 0) {
            type = FolderObject.PUBLIC;
        } else if (pid == FolderObject.SYSTEM_OX_PROJECT_FOLDER_ID) {
            type = FolderObject.PROJECT;
        } else {
            type = getFolderAccess(storageParameters).getFolderType(pid);
        }
        return type;
    }

    public void clearFolder(final String treeId, final String folderId, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getConnection(storageParameters);
            final FolderObject fo = getFolderObject(Integer.parseInt(folderId), storageParameters.getContext(), con);
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }
            final OXFolderManager folderManager = OXFolderManager.getInstance(session, con, con);
            folderManager.clearFolder(fo, true, System.currentTimeMillis());
        } catch (final OXFolderException e) {
            throw new FolderException(e);
        } catch (final OXException e) {
            throw new FolderException(e);
        }
    }

    public void deleteFolder(final String treeId, final String folderIdentifier, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getConnection(storageParameters);
            final FolderObject fo = new FolderObject();
            final int folderId = Integer.parseInt(folderIdentifier);
            fo.setObjectID(folderId);
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }
            final OXFolderManager folderManager = OXFolderManager.getInstance(session, con, con);
            /*-
             * TODO: Perform last-modified check?
            {
                final Date clientLastModified = storageParameters.getTimeStamp();
                if (null != clientLastModified && getFolderAccess(storageParameters, getFolderType()).getFolderLastModified(folderId).after(
                    clientLastModified)) {
                    throw FolderExceptionErrorMessage.CONCURRENT_MODIFICATION.create();
                }
            }
             * 
             */
            folderManager.deleteFolder(fo, true, System.currentTimeMillis());
        } catch (final OXFolderException e) {
            throw new FolderException(e);
        } catch (final OXException e) {
            throw new FolderException(e);
        }
    }

    public String getDefaultFolderID(final User user, final String treeId, final ContentType contentType, final Type type, final StorageParameters storageParameters) throws FolderException {
        final Context context = storageParameters.getContext();
        try {
            final Connection con = getConnection(storageParameters);
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }
            final int folderId;
            if (TaskContentType.getInstance().equals(contentType)) {
                folderId = OXFolderSQL.getUserDefaultFolder(session.getUserId(), FolderObject.TASK, con, context);
            } else if (CalendarContentType.getInstance().equals(contentType)) {
                folderId = OXFolderSQL.getUserDefaultFolder(session.getUserId(), FolderObject.CALENDAR, con, context);
            } else if (ContactContentType.getInstance().equals(contentType)) {
                folderId = OXFolderSQL.getUserDefaultFolder(session.getUserId(), FolderObject.CONTACT, con, context);
            } else if (InfostoreContentType.getInstance().equals(contentType)) {
                folderId = OXFolderSQL.getUserDefaultFolder(session.getUserId(), FolderObject.INFOSTORE, con, context);
            } else {
                return null;
            }
            return String.valueOf(folderId);
        } catch (final DBPoolingException e) {
            throw new FolderException(e);
        } catch (final SQLException e) {
            throw FolderExceptionErrorMessage.SQL_ERROR.create(e, e.getMessage());
        }
    }

    public Type getTypeByParent(final User user, final String treeId, final String parentId, final StorageParameters storageParameters) throws FolderException {
        /*
         * Special treatment for system folders
         */
        final int pid = Integer.parseInt(parentId);
        if (pid == FolderObject.SYSTEM_SHARED_FOLDER_ID) {
            return SharedType.getInstance();
        } else if (pid == FolderObject.SYSTEM_PRIVATE_FOLDER_ID) {
            return PrivateType.getInstance();
        } else if (Arrays.binarySearch(PUBLIC_FOLDER_IDS, pid) >= 0) {
            return PublicType.getInstance();
        } else if (pid == FolderObject.SYSTEM_OX_PROJECT_FOLDER_ID) {
            return SystemType.getInstance();
        } else {
            try {
                final FolderObject p = getFolderAccess(storageParameters).getFolderObject(pid);
                final int parentType = p.getType();
                if (FolderObject.PRIVATE == parentType) {
                    return p.getCreatedBy() == user.getId() ? PrivateType.getInstance() : SharedType.getInstance();
                } else if (FolderObject.PUBLIC == parentType) {
                    return PublicType.getInstance();
                }
            } catch (final OXException e) {
                throw new FolderException(e);
            }
        }
        return SystemType.getInstance();
    }

    public boolean containsForeignObjects(final User user, final String treeId, final String folderIdentifier, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getConnection(storageParameters);
            final Context ctx = storageParameters.getContext();
            /*
             * A numeric folder identifier
             */
            final int folderId = getUnsignedInteger(folderIdentifier);
            if (folderId < 0) {
                throw new OXFolderNotFoundException(folderIdentifier, ctx);
            }
            if (FolderObject.SYSTEM_ROOT_FOLDER_ID == folderId) {
                return false;
            } else if (FolderObject.SYSTEM_SHARED_FOLDER_ID == folderId) {
                /*
                 * The system shared folder
                 */
                return false;
            } else if (FolderObject.SYSTEM_PUBLIC_FOLDER_ID == folderId) {
                /*
                 * The system public folder
                 */
                return false;
            } else if (FolderObject.SYSTEM_INFOSTORE_FOLDER_ID == folderId) {
                /*
                 * The system infostore folder
                 */
                return false;
            } else if (FolderObject.SYSTEM_PRIVATE_FOLDER_ID == folderId) {
                /*
                 * The system private folder
                 */
                return false;
            } else if (Arrays.binarySearch(VIRTUAL_IDS, folderId) >= 0) {
                /*
                 * A virtual database folder
                 */
                return true;
            } else {
                /*
                 * A non-virtual database folder
                 */
                final OXFolderAccess folderAccess = getFolderAccess(storageParameters);
                return folderAccess.containsForeignObjects(getFolderObject(folderId, ctx, con), storageParameters.getSession(), ctx);
            }
        } catch (final OXException e) {
            throw new FolderException(e);
        }
    }

    public boolean isEmpty(final String treeId, final String folderIdentifier, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getConnection(storageParameters);
            final Context ctx = storageParameters.getContext();
            /*
             * A numeric folder identifier
             */
            final int folderId = getUnsignedInteger(folderIdentifier);
            if (folderId < 0) {
                throw new OXFolderNotFoundException(folderIdentifier, ctx);
            }
            if (FolderObject.SYSTEM_ROOT_FOLDER_ID == folderId) {
                return true;
            } else if (FolderObject.SYSTEM_SHARED_FOLDER_ID == folderId) {
                /*
                 * The system shared folder
                 */
                return true;
            } else if (FolderObject.SYSTEM_PUBLIC_FOLDER_ID == folderId) {
                /*
                 * The system public folder
                 */
                return true;
            } else if (FolderObject.SYSTEM_INFOSTORE_FOLDER_ID == folderId) {
                /*
                 * The system infostore folder
                 */
                return true;
            } else if (FolderObject.SYSTEM_PRIVATE_FOLDER_ID == folderId) {
                /*
                 * The system private folder
                 */
                return true;
            } else if (Arrays.binarySearch(VIRTUAL_IDS, folderId) >= 0) {
                /*
                 * A virtual database folder
                 */
                return false;
            } else {
                /*
                 * A non-virtual database folder
                 */
                final OXFolderAccess folderAccess = getFolderAccess(storageParameters);
                return folderAccess.isEmpty(getFolderObject(folderId, ctx, con), storageParameters.getSession(), ctx);
            }
        } catch (final OXException e) {
            throw new FolderException(e);
        }
    }

    public void updateLastModified(final long lastModified, final String treeId, final String folderIdentifier, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getConnection(storageParameters);
            final Context ctx = storageParameters.getContext();
            final int folderId = getUnsignedInteger(folderIdentifier);
            if (getFolderAccess(storageParameters).getFolderLastModified(folderId).after(new Date(lastModified))) {
                throw FolderExceptionErrorMessage.CONCURRENT_MODIFICATION.create();
            }
            OXFolderSQL.updateLastModified(folderId, lastModified, storageParameters.getUserId(), con, ctx);
        } catch (final DBPoolingException e) {
            throw new FolderException(e);
        } catch (final SQLException e) {
            throw FolderExceptionErrorMessage.SQL_ERROR.create(e, e.getMessage());
        } catch (final OXException e) {
            throw new FolderException(e);
        }
    }

    public Folder getFolder(final String treeId, final String folderIdentifier, final StorageParameters storageParameters) throws FolderException {
        return getFolder(treeId, folderIdentifier, StorageType.WORKING, storageParameters);
    }

    private static final int[] VIRTUAL_IDS =
        {
            FolderObject.VIRTUAL_LIST_TASK_FOLDER_ID, FolderObject.VIRTUAL_LIST_CALENDAR_FOLDER_ID,
            FolderObject.VIRTUAL_LIST_CONTACT_FOLDER_ID, FolderObject.VIRTUAL_LIST_INFOSTORE_FOLDER_ID };

    public Folder getFolder(final String treeId, final String folderIdentifier, final StorageType storageType, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getConnection(storageParameters);
            final User user = storageParameters.getUser();
            final Context ctx = storageParameters.getContext();
            final UserConfiguration userConfiguration;
            {
                final Session s = storageParameters.getSession();
                if (s instanceof ServerSession) {
                    userConfiguration = ((ServerSession) s).getUserConfiguration();
                } else {
                    userConfiguration = UserConfigurationStorage.getInstance().getUserConfiguration(user.getId(), ctx);
                }
            }

            final DatabaseFolder retval;

            if (StorageType.WORKING.equals(storageType)) {
                if (DatabaseFolderStorageUtility.hasSharedPrefix(folderIdentifier)) {
                    retval = SharedPrefixFolder.getSharedPrefixFolder(folderIdentifier, user, userConfiguration, ctx, con);
                } else {
                    /*
                     * A numeric folder identifier
                     */
                    final int folderId = getUnsignedInteger(folderIdentifier);

                    if (folderId < 0) {
                        throw new OXFolderNotFoundException(folderIdentifier, ctx);
                    }

                    if (FolderObject.SYSTEM_ROOT_FOLDER_ID == folderId) {
                        retval = SystemRootFolder.getSystemRootFolder();
                    } else if (Arrays.binarySearch(VIRTUAL_IDS, folderId) >= 0) {
                        /*
                         * A virtual database folder
                         */
                        retval = VirtualListFolder.getVirtualListFolder(folderId);
                    } else {
                        /*
                         * A non-virtual database folder
                         */
                        final FolderObject fo = getFolderObject(folderId, ctx, con);
                        retval = DatabaseFolderConverter.convert(fo, user, userConfiguration, ctx, con);
                    }
                }
            } else {
                /*
                 * Get from backup tables
                 */
                final int folderId = getUnsignedInteger(folderIdentifier);

                if (folderId < 0) {
                    throw new OXFolderNotFoundException(folderIdentifier, ctx);
                }

                final FolderObject fo =
                    FolderObject.loadFolderObjectFromDB(folderId, ctx, con, true, false, "del_oxfolder_tree", "del_oxfolder_permissions");
                retval = new DatabaseFolder(fo);
            }
            retval.setTreeID(treeId);
            // TODO: Subscribed?

            return retval;
        } catch (final FolderException e) {
            throw e;
        } catch (final AbstractOXException e) {
            throw new FolderException(e);
        }
    }

    public List<Folder> getFolders(final String treeId, final List<String> folderIdentifiers, final StorageParameters storageParameters) throws FolderException {
        return getFolders(treeId, folderIdentifiers, StorageType.WORKING, storageParameters);
    }

    public List<Folder> getFolders(final String treeId, final List<String> folderIdentifiers, final StorageType storageType, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getConnection(storageParameters);
            final User user = storageParameters.getUser();
            final Context ctx = storageParameters.getContext();
            final UserConfiguration userConfiguration;
            {
                final Session s = storageParameters.getSession();
                if (s instanceof ServerSession) {
                    userConfiguration = ((ServerSession) s).getUserConfiguration();
                } else {
                    userConfiguration = UserConfigurationStorage.getInstance().getUserConfiguration(user.getId(), ctx);
                }
            }
            /*
             * Either from working or from backup storage type
             */
            if (StorageType.WORKING.equals(storageType)) {
                final int size = folderIdentifiers.size();
                final Folder[] ret = new Folder[size]; 
                final TIntIntHashMap map = new TIntIntHashMap(size);
                /*
                 * Check for special folder identifier
                 */
                for (int index = 0; index < size; index++) {
                    final String folderIdentifier = folderIdentifiers.get(index);
                    if (DatabaseFolderStorageUtility.hasSharedPrefix(folderIdentifier)) {
                        ret[index] = SharedPrefixFolder.getSharedPrefixFolder(folderIdentifier, user, userConfiguration, ctx, con);
                    } else {
                        /*
                         * A numeric folder identifier
                         */
                        final int folderId = getUnsignedInteger(folderIdentifier);
                        if (FolderObject.SYSTEM_ROOT_FOLDER_ID == folderId) {
                            ret[index] = SystemRootFolder.getSystemRootFolder();
                        } else if (Arrays.binarySearch(VIRTUAL_IDS, folderId) >= 0) {
                            ret[index] = VirtualListFolder.getVirtualListFolder(folderId);
                        } else {
                            map.put(folderId, index);
                        }
                    }
                }
                /*
                 * Batch load
                 */
                if (!map.isEmpty()) {
                    for (final FolderObject folderObject : getFolderObjects(map.keys(), ctx, con)) {
                        final int index = map.get(folderObject.getObjectID());
                        ret[index] = DatabaseFolderConverter.convert(folderObject, user, userConfiguration, ctx, con);
                    }
                }
                /*
                 * Set proper tree identifier
                 */
                for (final Folder folder : ret) {
                    folder.setTreeID(treeId);
                }
                /*
                 * Return
                 */
                final List<Folder> l = new ArrayList<Folder>(ret.length);
                for (final Folder folder : ret) {
                    if (null != folder) {
                        l.add(folder);
                    }
                }
                return l;
            }
            /*
             * Get from backup tables
             */
            final TIntArrayList list = new TIntArrayList(folderIdentifiers.size());
            for (final String folderIdentifier : folderIdentifiers) {
                list.add(getUnsignedInteger(folderIdentifier));
            }
            final List<FolderObject> folders = OXFolderBatchLoader.loadFolderObjectsFromDB(list.toNativeArray(), ctx, con, true, false, "del_oxfolder_tree", "del_oxfolder_permissions");
            final List<Folder> ret = new ArrayList<Folder>(folders.size());
            for (final FolderObject fo : folders) {
                final DatabaseFolder df = new DatabaseFolder(fo);
                df.setTreeID(treeId);
                ret.add(df);
            }

            return ret;
        } catch (final FolderException e) {
            throw e;
        } catch (final AbstractOXException e) {
            throw new FolderException(e);
        }
    }

    public FolderType getFolderType() {
        return DatabaseFolderType.getInstance();
    }

    public SortableId[] getVisibleFolders(final String treeId, final ContentType contentType, final Type type, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getConnection(storageParameters);
            final User user = storageParameters.getUser();
            final int userId = user.getId();
            final Context ctx = storageParameters.getContext();
            final UserConfiguration userConfiguration;
            {
                final Session s = storageParameters.getSession();
                if (s instanceof ServerSession) {
                    userConfiguration = ((ServerSession) s).getUserConfiguration();
                } else {
                    userConfiguration = UserConfigurationStorage.getInstance().getUserConfiguration(userId, ctx);
                }
            }

            final int iType = getTypeByFolderTypeWithShared(type);
            final int iModule = getModuleByContentType(contentType);
            final List<FolderObject> list =
                ((FolderObjectIterator) OXFolderIteratorSQL.getAllVisibleFoldersIteratorOfType(
                    userId,
                    user.getGroups(),
                    userConfiguration.getAccessibleModules(),
                    iType,
                    new int[] { iModule },
                    ctx, con)).asList();
            if (FolderObject.PRIVATE == iType) {
                /*
                 * Remove shared ones manually
                 */
                for (final Iterator<FolderObject> iterator = list.iterator(); iterator.hasNext();) {
                    if (iterator.next().getCreatedBy() != userId) {
                        iterator.remove();
                    }
                }
            } else if (FolderObject.PUBLIC == iType && FolderObject.CONTACT == iModule ) {
                try {
                    /*
                     * Add global address book manually
                     */
                    final FolderObject gab = getFolderObject(FolderObject.SYSTEM_LDAP_FOLDER_ID, ctx, con);
                    if (gab.isVisible(userId, userConfiguration)) {
                        gab.setFolderName(new StringHelper(user.getLocale()).getString(FolderStrings.SYSTEM_LDAP_FOLDER_NAME));
                        list.add(gab);
                    }
                } catch (final DBPoolingException e) {
                    throw new FolderException(e);
                } catch (final SQLException e) {
                    throw new FolderException(new OXFolderException(OXFolderException.FolderCode.SQL_ERROR, e, e.getMessage()));
                }
            }
            /*
             * Localize folder names
             */
            {
                StringHelper stringHelper = null;
                for (final FolderObject folderObject : list) {
                    /*
                     * Check if folder is user's default folder and set locale-sensitive name
                     */
                    if (folderObject.isDefaultFolder()) {
                        final int module = folderObject.getModule();
                        if (FolderObject.CALENDAR == module) {
                            if (null == stringHelper) {
                                stringHelper = new StringHelper(user.getLocale());
                            }
                            folderObject.setFolderName(stringHelper.getString(FolderStrings.DEFAULT_CALENDAR_FOLDER_NAME));
                        } else if (FolderObject.CONTACT == module) {
                            if (null == stringHelper) {
                                stringHelper = new StringHelper(user.getLocale());
                            }
                            folderObject.setFolderName(stringHelper.getString(FolderStrings.DEFAULT_CONTACT_FOLDER_NAME));
                        } else if (FolderObject.TASK == module) {
                            if (null == stringHelper) {
                                stringHelper = new StringHelper(user.getLocale());
                            }
                            folderObject.setFolderName(stringHelper.getString(FolderStrings.DEFAULT_TASK_FOLDER_NAME));
                        }
                    }
                }
            }
            if (FolderObject.PRIVATE == iType) {
                /*
                 * Sort them by default-flag and name: <user's default folder>, <aaa>, <bbb>, ... <zzz>
                 */
                Collections.sort(list, new FolderObjectComparator(user.getLocale()));
            } else {
                /*
                 * Sort them by name only
                 */
                Collections.sort(list, new FolderNameComparator(user.getLocale()));
            }
            /*
             * Extract IDs
             */
            final SortableId[] ret = new SortableId[list.size()];
            for (int i = 0; i < ret.length; i++) {
                final FolderObject folderObject = list.get(i);
                final String id = String.valueOf(folderObject.getObjectID());
                ret[i] = new DatabaseId(id, i, folderObject.getFolderName());
            }
            return ret;
        } catch (final OXException e) {
            throw new FolderException(e);
        } catch (final SearchIteratorException e) {
            throw new FolderException(e);
        }
    }

    public SortableId[] getSubfolders(final String treeId, final String parentIdentifier, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getConnection(storageParameters);

            final int parentId = Integer.parseInt(parentIdentifier);

            if (FolderObject.SYSTEM_ROOT_FOLDER_ID == parentId) {
                final String[] subfolderIds = SystemRootFolder.getSystemRootFolderSubfolder();
                final List<SortableId> list = new ArrayList<SortableId>(subfolderIds.length);
                for (int i = 0; i < subfolderIds.length; i++) {
                    list.add(new DatabaseId(subfolderIds[i], i, null));
                }
                return list.toArray(new SortableId[list.size()]);
            }

            if (Arrays.binarySearch(VIRTUAL_IDS, parentId) >= 0) {
                /*
                 * A virtual database folder
                 */
                final User user = storageParameters.getUser();
                final Context ctx = storageParameters.getContext();
                final UserConfiguration userConfiguration;
                {
                    final Session s = storageParameters.getSession();
                    if (s instanceof ServerSession) {
                        userConfiguration = ((ServerSession) s).getUserConfiguration();
                    } else {
                        userConfiguration = UserConfigurationStorage.getInstance().getUserConfiguration(user.getId(), ctx);
                    }
                }
                final String[] subfolderIds = VirtualListFolder.getVirtualListFolderSubfolders(parentId, user, userConfiguration, ctx, con);
                final List<SortableId> list = new ArrayList<SortableId>(subfolderIds.length);
                for (int i = 0; i < subfolderIds.length; i++) {
                    list.add(new DatabaseId(subfolderIds[i], i, null));
                }
                return list.toArray(new SortableId[list.size()]);
            }

            if (FolderObject.SYSTEM_PRIVATE_FOLDER_ID == parentId) {
                /*
                 * The system private folder
                 */
                final User user = storageParameters.getUser();
                final Context ctx = storageParameters.getContext();
                final UserConfiguration userConfiguration;
                {
                    final Session s = storageParameters.getSession();
                    if (s instanceof ServerSession) {
                        userConfiguration = ((ServerSession) s).getUserConfiguration();
                    } else {
                        userConfiguration = UserConfigurationStorage.getInstance().getUserConfiguration(user.getId(), ctx);
                    }
                }
                final String[] subfolderIds = SystemPrivateFolder.getSystemPrivateFolderSubfolders(user, userConfiguration, ctx, con);
                final List<SortableId> list = new ArrayList<SortableId>(subfolderIds.length);
                for (int i = 0; i < subfolderIds.length; i++) {
                    list.add(new DatabaseId(subfolderIds[i], i, null));
                }
                return list.toArray(new SortableId[list.size()]);
            }

            if (FolderObject.SYSTEM_SHARED_FOLDER_ID == parentId) {
                /*
                 * The system shared folder
                 */
                final User user = storageParameters.getUser();
                final Context ctx = storageParameters.getContext();
                final UserConfiguration userConfiguration;
                {
                    final Session s = storageParameters.getSession();
                    if (s instanceof ServerSession) {
                        userConfiguration = ((ServerSession) s).getUserConfiguration();
                    } else {
                        userConfiguration = UserConfigurationStorage.getInstance().getUserConfiguration(user.getId(), ctx);
                    }
                }
                final String[] subfolderIds = SystemSharedFolder.getSystemSharedFolderSubfolder(user, userConfiguration, ctx, con);
                final List<SortableId> list = new ArrayList<SortableId>(subfolderIds.length);
                for (int i = 0; i < subfolderIds.length; i++) {
                    list.add(new DatabaseId(subfolderIds[i], i, null));
                }
                return list.toArray(new SortableId[list.size()]);
            }

            if (FolderObject.SYSTEM_PUBLIC_FOLDER_ID == parentId) {
                /*
                 * The system public folder
                 */
                final User user = storageParameters.getUser();
                final Context ctx = storageParameters.getContext();
                final UserConfiguration userConfiguration;
                {
                    final Session s = storageParameters.getSession();
                    if (s instanceof ServerSession) {
                        userConfiguration = ((ServerSession) s).getUserConfiguration();
                    } else {
                        userConfiguration = UserConfigurationStorage.getInstance().getUserConfiguration(user.getId(), ctx);
                    }
                }
                final String[] subfolderIds = SystemPublicFolder.getSystemPublicFolderSubfolders(user, userConfiguration, ctx, con);
                final List<SortableId> list = new ArrayList<SortableId>(subfolderIds.length);
                for (int i = 0; i < subfolderIds.length; i++) {
                    list.add(new DatabaseId(subfolderIds[i], i, null));
                }
                return list.toArray(new SortableId[list.size()]);
            }

            if (FolderObject.SYSTEM_INFOSTORE_FOLDER_ID == parentId) {
                /*
                 * The system infostore folder
                 */
                final User user = storageParameters.getUser();
                final Context ctx = storageParameters.getContext();
                final UserConfiguration userConfiguration;
                {
                    final Session s = storageParameters.getSession();
                    if (s instanceof ServerSession) {
                        userConfiguration = ((ServerSession) s).getUserConfiguration();
                    } else {
                        userConfiguration = UserConfigurationStorage.getInstance().getUserConfiguration(user.getId(), ctx);
                    }
                }
                final String[] subfolderIds = SystemInfostoreFolder.getSystemInfostoreFolderSubfolders(user, userConfiguration, ctx, con);
                final List<SortableId> list = new ArrayList<SortableId>(subfolderIds.length);
                for (int i = 0; i < subfolderIds.length; i++) {
                    list.add(new DatabaseId(subfolderIds[i], i, null));
                }
                return list.toArray(new SortableId[list.size()]);
            }

            final List<Integer> subfolderIds = FolderObject.getSubfolderIds(parentId, storageParameters.getContext(), con);
            final List<FolderObject> subfolders = new ArrayList<FolderObject>(subfolderIds.size());
            for (final Integer folderId : subfolderIds) {
                subfolders.add(FolderObject.loadFolderObjectFromDB(folderId.intValue(), storageParameters.getContext(), con, false, false));
            }
            final ServerSession session;
            {
                final Session s = storageParameters.getSession();
                if (null == s) {
                    throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
                }
                if (s instanceof ServerSession) {
                    session = (ServerSession) s;
                } else {
                    session = new ServerSessionAdapter(s);
                }
            }
            Collections.sort(subfolders, new FolderObjectComparator(session.getUser().getLocale()));
            final int size = subfolders.size();
            final List<SortableId> list = new ArrayList<SortableId>(size);
            for (int i = 0; i < size; i++) {
                final FolderObject folderObject = subfolders.get(i);
                list.add(new DatabaseId(folderObject.getObjectID(), i, folderObject.getFolderName()));
            }
            return list.toArray(new SortableId[size]);
        } catch (final DBPoolingException e) {
            throw new FolderException(e);
        } catch (final SQLException e) {
            throw FolderExceptionErrorMessage.SQL_ERROR.create(e, e.getMessage());
        } catch (final OXException e) {
            throw new FolderException(e);
        } catch (final ContextException e) {
            throw new FolderException(e);
        }
    }

    public void rollback(final StorageParameters params) {
        final Connection con;
        final Boolean writable;
        try {
            con = getConnection(params);
            writable = getParameter(Boolean.class, DatabaseParameterConstants.PARAM_WRITABLE, params);
        } catch (final FolderException e) {
            LOG.error(e.getMessage(), e);
            return;
        }
        try {
            DBUtils.rollback(con);
        } finally {
            DBUtils.autocommit(con);
            final DatabaseService databaseService = DatabaseServiceRegistry.getServiceRegistry().getService(DatabaseService.class);
            if (null != databaseService) {
                if (writable.booleanValue()) {
                    databaseService.backWritable(params.getContext(), con);
                } else {
                    databaseService.backReadOnly(params.getContext(), con);
                }
            }
            params.putParameter(getFolderType(), DatabaseParameterConstants.PARAM_CONNECTION, null);
            params.putParameter(DatabaseFolderType.getInstance(), DatabaseParameterConstants.PARAM_WRITABLE, null);
        }
    }

    public boolean startTransaction(final StorageParameters parameters, final boolean modify) throws FolderException {
        final FolderType folderType = getFolderType();
        if (null != parameters.getParameter(folderType, DatabaseParameterConstants.PARAM_CONNECTION)) {
            // Connection already present
            return false;
        }
        try {
            final DatabaseService databaseService = DatabaseServiceRegistry.getServiceRegistry().getService(DatabaseService.class, true);
            final Context context = parameters.getContext();
            final Connection con = modify ? databaseService.getWritable(context) : databaseService.getReadOnly(context);
            con.setAutoCommit(false);
            // Put to parameters
            if (parameters.putParameterIfAbsent(folderType, DatabaseParameterConstants.PARAM_CONNECTION, con)) {
                // Success
                parameters.putParameterIfAbsent(folderType, DatabaseParameterConstants.PARAM_WRITABLE, Boolean.valueOf(modify));
            } else {
                // Fail
                con.setAutoCommit(true);
                if (modify) {
                    databaseService.backWritable(context, con);
                } else {
                    databaseService.backReadOnly(context, con);
                }
            }
            return true;
        } catch (final ServiceException e) {
            throw new FolderException(e);
        } catch (final DBPoolingException e) {
            throw new FolderException(e);
        } catch (final SQLException e) {
            throw FolderExceptionErrorMessage.SQL_ERROR.create(e, e.getMessage());
        }
    }

    public void updateFolder(final Folder folder, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getConnection(storageParameters);
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }
            final int folderId = Integer.parseInt(folder.getID());

            /*
             * Check for concurrent modification
             */
            {
                final Date clientLastModified = storageParameters.getTimeStamp();
                if (null != clientLastModified && getFolderAccess(storageParameters).getFolderLastModified(folderId).after(
                    clientLastModified)) {
                    throw FolderExceptionErrorMessage.CONCURRENT_MODIFICATION.create();
                }
            }

            final Date millis = new Date();

            final FolderObject updateMe = new FolderObject();
            updateMe.setObjectID(folderId);
            updateMe.setDefaultFolder(false);
            {
                final String name = folder.getName();
                if (null != name) {
                    updateMe.setFolderName(name);
                }
            }
            updateMe.setLastModified(millis);
            folder.setLastModified(millis);
            updateMe.setModifiedBy(session.getUserId());
            {
                final ContentType ct = folder.getContentType();
                if (null != ct) {
                    updateMe.setModule(getModuleByContentType(ct));
                }
            }
            {
                final String parentId = folder.getParentID();
                if (null == parentId) {
                    updateMe.setParentFolderID(getFolderObject(folderId, storageParameters.getContext(), con).getParentFolderID());
                } else {
                    updateMe.setParentFolderID(Integer.parseInt(parentId));
                }
            }
            {
                final Type t = folder.getType();
                if (null != t) {
                    updateMe.setType(getTypeByFolderType(t));
                }
            }
            // Permissions
            final Permission[] perms = folder.getPermissions();
            if (null != perms) {
                final OCLPermission[] oclPermissions = new OCLPermission[perms.length];
                for (int i = 0; i < perms.length; i++) {
                    final Permission p = perms[i];
                    final OCLPermission oclPerm = new OCLPermission();
                    oclPerm.setEntity(p.getEntity());
                    oclPerm.setGroupPermission(p.isGroup());
                    oclPerm.setFolderAdmin(p.isAdmin());
                    oclPerm.setAllPermission(
                        p.getFolderPermission(),
                        p.getReadPermission(),
                        p.getWritePermission(),
                        p.getDeletePermission());
                    oclPerm.setSystem(p.getSystem());
                    oclPermissions[i] = oclPerm;
                }
                updateMe.setPermissionsAsArray(oclPermissions);
            }
            final OXFolderManager folderManager = OXFolderManager.getInstance(session, con, con);
            folderManager.updateFolder(updateMe, true, millis.getTime());
        } catch (final OXException e) {
            throw new FolderException(e);
        }
    }

    public StoragePriority getStoragePriority() {
        return StoragePriority.NORMAL;
    }

    public boolean containsFolder(final String treeId, final String folderIdentifier, final StorageParameters storageParameters) throws FolderException {
        return containsFolder(treeId, folderIdentifier, StorageType.WORKING, storageParameters);
    }

    public boolean containsFolder(final String treeId, final String folderIdentifier, final StorageType storageType, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getConnection(storageParameters);
            final User user = storageParameters.getUser();
            final Context ctx = storageParameters.getContext();
            final UserConfiguration userConfiguration;
            {
                final Session s = storageParameters.getSession();
                if (s instanceof ServerSession) {
                    userConfiguration = ((ServerSession) s).getUserConfiguration();
                } else {
                    userConfiguration = UserConfigurationStorage.getInstance().getUserConfiguration(user.getId(), ctx);
                }
            }

            final boolean retval;

            if (StorageType.WORKING.equals(storageType)) {
                if (DatabaseFolderStorageUtility.hasSharedPrefix(folderIdentifier)) {

                    retval = SharedPrefixFolder.existsSharedPrefixFolder(folderIdentifier, user, userConfiguration, ctx, con);
                } else {
                    /*
                     * A numeric folder identifier
                     */
                    final int folderId = getUnsignedInteger(folderIdentifier);

                    if (folderId < 0) {
                        retval = false;
                    } else {
                        if (FolderObject.SYSTEM_ROOT_FOLDER_ID == folderId) {
                            retval = true;
                        } else if (Arrays.binarySearch(VIRTUAL_IDS, folderId) >= 0) {
                            /*
                             * A virtual database folder
                             */
                            retval = VirtualListFolder.existsVirtualListFolder(folderId, user, userConfiguration, ctx, con);
                        } else {
                            /*
                             * A non-virtual database folder
                             */

                            if (FolderObject.SYSTEM_SHARED_FOLDER_ID == folderId) {
                                /*
                                 * The system shared folder
                                 */
                                retval = true;
                            } else if (FolderObject.SYSTEM_PUBLIC_FOLDER_ID == folderId) {
                                /*
                                 * The system public folder
                                 */
                                retval = true;
                            } else if (FolderObject.SYSTEM_INFOSTORE_FOLDER_ID == folderId) {
                                /*
                                 * The system infostore folder
                                 */
                                retval = true;
                            } else if (FolderObject.SYSTEM_PRIVATE_FOLDER_ID == folderId) {
                                /*
                                 * The system private folder
                                 */
                                retval = true;
                            } else {
                                /*
                                 * Check for shared folder, that is folder is of type private and requesting user is different from folder's
                                 * owner
                                 */
                                retval = OXFolderSQL.exists(folderId, con, ctx);
                            }
                        }
                    }
                }
            } else {
                final int folderId = getUnsignedInteger(folderIdentifier);

                if (folderId < 0) {
                    retval = false;
                } else {
                    retval = OXFolderSQL.exists(folderId, con, ctx, "del_oxfolder_tree");
                }
            }
            return retval;
        } catch (final DBPoolingException e) {
            throw new FolderException(e);
        } catch (final OXException e) {
            throw new FolderException(e);
        } catch (final SQLException e) {
            throw FolderExceptionErrorMessage.SQL_ERROR.create(e, e.getMessage());
        }
    } // End of containsFolder()

    public String[] getModifiedFolderIDs(final String treeId, final Date timeStamp, final ContentType[] includeContentTypes, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getConnection(storageParameters);
            final Context ctx = storageParameters.getContext();

            final Queue<FolderObject> q =
                ((FolderObjectIterator) OXFolderIteratorSQL.getAllModifiedFoldersSince(
                    timeStamp == null ? new Date(0) : timeStamp,
                    ctx,
                    con)).asQueue();
            final int size = q.size();
            final Iterator<FolderObject> iterator = q.iterator();
            final String[] ret = new String[size];
            for (int i = 0; i < size; i++) {
                ret[i] = String.valueOf(iterator.next().getObjectID());
            }

            return ret;
        } catch (final SearchIteratorException e) {
            throw new FolderException(e);
        } catch (final OXException e) {
            throw new FolderException(e);
        }
    } // End of getModifiedFolderIDs()

    public String[] getDeletedFolderIDs(final String treeId, final Date timeStamp, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getConnection(storageParameters);
            final User user = storageParameters.getUser();
            final Context ctx = storageParameters.getContext();
            final UserConfiguration userConfiguration;
            {
                final Session s = storageParameters.getSession();
                if (s instanceof ServerSession) {
                    userConfiguration = ((ServerSession) s).getUserConfiguration();
                } else {
                    userConfiguration = UserConfigurationStorage.getInstance().getUserConfiguration(user.getId(), ctx);
                }
            }

            final Queue<FolderObject> q =
                ((FolderObjectIterator) OXFolderIteratorSQL.getDeletedFoldersSince(
                    timeStamp,
                    user.getId(),
                    user.getGroups(),
                    userConfiguration.getAccessibleModules(),
                    ctx,
                    con)).asQueue();
            final int size = q.size();
            final Iterator<FolderObject> iterator = q.iterator();
            final String[] ret = new String[size];
            for (int i = 0; i < size; i++) {
                ret[i] = String.valueOf(iterator.next().getObjectID());
            }

            return ret;
        } catch (final SearchIteratorException e) {
            throw new FolderException(e);
        } catch (final OXException e) {
            throw new FolderException(e);
        }

    } // End of getDeletedFolderIDs()

    /*-
     * ############################# HELPER METHODS #############################
     */

    private static FolderObject getFolderObject(final int folderId, final Context ctx, final Connection con) throws OXException {
        if (!FolderCacheManager.isEnabled()) {
            return FolderObject.loadFolderObjectFromDB(folderId, ctx, con);
        }
        final FolderCacheManager cacheManager = FolderCacheManager.getInstance();
        FolderObject fo = cacheManager.getFolderObject(folderId, ctx);
        if (null == fo) {
            fo = FolderObject.loadFolderObjectFromDB(folderId, ctx, con);
            cacheManager.putFolderObject(fo, ctx, false, null);
        }
        return fo;
    }

    private static List<FolderObject> getFolderObjects(final int[] folderIds, final Context ctx, final Connection con) throws OXException {
        if (!FolderCacheManager.isEnabled()) {
            /*
             * OX folder cache not enabled
             */
            return OXFolderBatchLoader.loadFolderObjectsFromDB(folderIds, ctx, con);
        }
        /*
         * Load them either from cache or from database
         */
        final int length = folderIds.length;
        final FolderObject[] ret = new FolderObject[length];
        final TIntIntHashMap toLoad = new TIntIntHashMap(length);
        final FolderCacheManager cacheManager = FolderCacheManager.getInstance();
        for (int index = 0; index < length; index++) {
            final int folderId = folderIds[index];
            final FolderObject fo = cacheManager.getFolderObject(folderId, ctx);
            if (null == fo) { // Cache miss
                toLoad.put(folderId, index);
            } else { // Cache hit
                ret[index] = fo;
            }
        }
        if (!toLoad.isEmpty()) {
            final List<FolderObject> list = OXFolderBatchLoader.loadFolderObjectsFromDB(toLoad.keys(), ctx, con);
            for (final FolderObject folderObject : list) {
                final int index = toLoad.get(folderObject.getObjectID());
                ret[index] = folderObject;
                cacheManager.putFolderObject(folderObject, ctx, false, null);
            }
        }
        return Arrays.asList(ret);
    }

    private static OXFolderAccess getFolderAccess(final StorageParameters storageParameters) throws FolderException {
        OXFolderAccess ret = (OXFolderAccess) storageParameters.getParameter(DatabaseFolderType.getInstance(), DatabaseParameterConstants.PARAM_ACCESS);
        if (null == ret) {
            final Connection con = getConnection(storageParameters);
            do {
                ret = new OXFolderAccess(con, storageParameters.getContext());
                if (!storageParameters.putParameterIfAbsent(DatabaseFolderType.getInstance(), DatabaseParameterConstants.PARAM_ACCESS, ret)) {
                    ret = (OXFolderAccess) storageParameters.getParameter(DatabaseFolderType.getInstance(), DatabaseParameterConstants.PARAM_ACCESS);
                }
            } while (null == ret);
        }
        return ret;
    }

    private static Connection getConnection(final StorageParameters storageParameters) throws FolderException {
        return getParameter(Connection.class, DatabaseParameterConstants.PARAM_CONNECTION, storageParameters);
    }

    private static <T> T getParameter(final Class<T> clazz, final String name, final StorageParameters parameters) throws FolderException {
        final Object obj = parameters.getParameter(DatabaseFolderType.getInstance(), name);
        if (null == obj) {
            throw new FolderException(new OXFolderException(OXFolderException.FolderCode.MISSING_PARAMETER, name));
        }
        try {
            return clazz.cast(obj);
        } catch (final ClassCastException e) {
            throw new FolderException(new OXFolderException(OXFolderException.FolderCode.MISSING_PARAMETER, e, name));
        }
    }

    private static int getModuleByContentType(final ContentType contentType) {
        final String cts = contentType.toString();
        if (TaskContentType.getInstance().toString().equals(cts)) {
            return FolderObject.TASK;
        }
        if (CalendarContentType.getInstance().toString().equals(cts)) {
            return FolderObject.CALENDAR;
        }
        if (ContactContentType.getInstance().toString().equals(cts)) {
            return FolderObject.CONTACT;
        }
        if (InfostoreContentType.getInstance().toString().equals(cts)) {
            return FolderObject.INFOSTORE;
        }
        return FolderObject.UNBOUND;
    }

    private static int getTypeByFolderType(final Type type) {
        if (PrivateType.getInstance().equals(type)) {
            return FolderObject.PRIVATE;
        }
        if (PublicType.getInstance().equals(type)) {
            return FolderObject.PUBLIC;
        }
        return FolderObject.SYSTEM_TYPE;
    }

    private static int getTypeByFolderTypeWithShared(final Type type) {
        if (PrivateType.getInstance().equals(type)) {
            return FolderObject.PRIVATE;
        }
        if (PublicType.getInstance().equals(type)) {
            return FolderObject.PUBLIC;
        }
        if (SharedType.getInstance().equals(type)) {
            return FolderObject.SHARED;
        }
        return FolderObject.SYSTEM_TYPE;
    }

    private static final class FolderObjectComparator implements Comparator<FolderObject> {

        private final Collator collator;

        public FolderObjectComparator(final Locale locale) {
            super();
            collator = Collator.getInstance(locale);
            collator.setStrength(Collator.SECONDARY);
        }

        public int compare(final FolderObject o1, final FolderObject o2) {
            if (o1.isDefaultFolder()) {
                if (o2.isDefaultFolder()) {
                    return compareById(o1.getObjectID(), o2.getObjectID());
                }
                return -1;
            } else if (o2.isDefaultFolder()) {
                return 1;
            }
            // Compare by name
            return collator.compare(o1.getFolderName(), o2.getFolderName());
        }

        private static int compareById(final int id1, final int id2) {
            return (id1 < id2 ? -1 : (id1 == id2 ? 0 : 1));
        }

    } // End of FolderObjectComparator

    private static final class FolderNameComparator implements Comparator<FolderObject> {

        private final Collator collator;

        public FolderNameComparator(final Locale locale) {
            super();
            collator = Collator.getInstance(locale);
            collator.setStrength(Collator.SECONDARY);
        }

        public int compare(final FolderObject o1, final FolderObject o2) {
            /*
             * Compare by name
             */
            return collator.compare(o1.getFolderName(), o2.getFolderName());
        }

    } // End of FolderNameComparator

}
