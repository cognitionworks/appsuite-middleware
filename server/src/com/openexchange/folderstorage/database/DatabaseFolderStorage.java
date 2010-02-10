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
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.server.ServiceException;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.session.Session;
import com.openexchange.tools.iterator.FolderObjectIterator;
import com.openexchange.tools.iterator.SearchIteratorException;
import com.openexchange.tools.oxfolder.OXFolderAccess;
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

    private static OXFolderAccess getFolderAccess(final StorageParameters storageParameters, final FolderType folderType) {
        OXFolderAccess ret = (OXFolderAccess) storageParameters.getParameter(folderType, DatabaseParameterConstants.PARAM_ACCESS);
        if (null == ret) {
            ret = new OXFolderAccess(storageParameters.getContext());
            storageParameters.putParameterIfAbsent(folderType, DatabaseParameterConstants.PARAM_ACCESS, ret);
        }
        return ret;
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
            con = getParameter(Connection.class, DatabaseParameterConstants.PARAM_CONNECTION, params);
            writable = getParameter(Boolean.class, DatabaseParameterConstants.PARAM_WRITABLE, params);
        } catch (final FolderException e) {
            /*
             * Already committed
             */
            if (LOG.isWarnEnabled()) {
                LOG.warn(e.getMessage(), e);
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
        }
    }

    public void createFolder(final Folder folder, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getParameter(Connection.class, DatabaseParameterConstants.PARAM_CONNECTION, storageParameters);
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
                    createMe.setType(getFolderType(createMe.getParentFolderID(), storageParameters, getFolderType()));
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

    private static int getFolderType(final int folderIdArg, final StorageParameters storageParameters, final FolderType folderType) throws OXException {
        int type = -1;
        int folderId = folderIdArg;
        /*
         * Special treatment for system folders
         */
        if (folderId == FolderObject.SYSTEM_SHARED_FOLDER_ID) {
            folderId = FolderObject.SYSTEM_PRIVATE_FOLDER_ID;
            type = FolderObject.SHARED;
        } else if (folderId == FolderObject.SYSTEM_PRIVATE_FOLDER_ID) {
            type = FolderObject.PRIVATE;
        } else if (Arrays.binarySearch(PUBLIC_FOLDER_IDS, folderId) >= 0) {
            type = FolderObject.PUBLIC;
        } else if (folderId == FolderObject.SYSTEM_OX_PROJECT_FOLDER_ID) {
            type = FolderObject.PROJECT;
        } else {
            type = getFolderAccess(storageParameters, folderType).getFolderType(folderId);
        }
        return type;
    }

    public void clearFolder(final String treeId, final String folderId, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getParameter(Connection.class, DatabaseParameterConstants.PARAM_CONNECTION, storageParameters);
            final FolderObject fo = getFolderAccess(storageParameters, getFolderType()).getFolderObject(Integer.parseInt(folderId));
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
            final Connection con = getParameter(Connection.class, DatabaseParameterConstants.PARAM_CONNECTION, storageParameters);
            final FolderObject fo = new FolderObject();
            final int folderId = Integer.parseInt(folderIdentifier);
            fo.setObjectID(folderId);
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }
            final OXFolderManager folderManager = OXFolderManager.getInstance(session, con, con);
            {
                final Date clientLastModified = storageParameters.getTimeStamp();
                if (null != clientLastModified && getFolderAccess(storageParameters, getFolderType()).getFolderLastModified(folderId).after(
                    clientLastModified)) {
                    throw FolderExceptionErrorMessage.CONCURRENT_MODIFICATION.create();
                }
            }
            folderManager.deleteFolder(fo, true, System.currentTimeMillis());
        } catch (final OXFolderException e) {
            throw new FolderException(e);
        } catch (final OXException e) {
            throw new FolderException(e);
        }
    }

    public String getDefaultFolderID(final User user, final String treeId, final ContentType contentType, final StorageParameters storageParameters) throws FolderException {
        final Context context = storageParameters.getContext();
        try {
            final Connection con = getParameter(Connection.class, DatabaseParameterConstants.PARAM_CONNECTION, storageParameters);
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

    public Folder getFolder(final String treeId, final String folderIdentifier, final StorageParameters storageParameters) throws FolderException {
        return getFolder(treeId, folderIdentifier, StorageType.WORKING, storageParameters);
    }

    private static final int[] VIRTUAL_IDS =
        {
            FolderObject.VIRTUAL_LIST_TASK_FOLDER_ID, FolderObject.VIRTUAL_LIST_CALENDAR_FOLDER_ID,
            FolderObject.VIRTUAL_LIST_CONTACT_FOLDER_ID, FolderObject.VIRTUAL_LIST_INFOSTORE_FOLDER_ID };

    public Folder getFolder(final String treeId, final String folderIdentifier, final StorageType storageType, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getParameter(Connection.class, DatabaseParameterConstants.PARAM_CONNECTION, storageParameters);
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
                        final FolderObject fo = FolderObject.loadFolderObjectFromDB(folderId, ctx, con);

                        if (FolderObject.SYSTEM_SHARED_FOLDER_ID == folderId) {
                            /*
                             * The system shared folder
                             */
                            retval = SystemSharedFolder.getSystemSharedFolder(fo, user, userConfiguration, ctx, con);
                        } else if (FolderObject.SYSTEM_PUBLIC_FOLDER_ID == folderId) {
                            /*
                             * The system public folder
                             */
                            retval = SystemPublicFolder.getSystemPublicFolder(fo);
                        } else if (FolderObject.SYSTEM_INFOSTORE_FOLDER_ID == folderId) {
                            /*
                             * The system infostore folder
                             */
                            retval = SystemInfostoreFolder.getSystemInfostoreFolder(fo);
                        } else if (FolderObject.SYSTEM_PRIVATE_FOLDER_ID == folderId) {
                            /*
                             * The system private folder
                             */
                            retval = SystemPrivateFolder.getSystemPrivateFolder(fo);
                        } else {
                            /*
                             * Check for shared folder, that is folder is of type private and requesting user is different from folder's
                             * owner
                             */
                            retval = new DatabaseFolder(fo);
                            if (PrivateType.getInstance().equals(retval.getType()) && storageParameters.getUserId() != retval.getCreatedBy()) {
                                retval.setType(SharedType.getInstance());
                                /*
                                 * A shared folder has no subfolders in real tree
                                 */
                                retval.setSubfolderIDs(null);
                            } else if (FolderObject.SYSTEM_PRIVATE_FOLDER_ID != folderId) {
                                /*
                                 * Set subfolders for non-private folder. For private folder FolderStorage.getSubfolders() is supposed to be
                                 * used.
                                 */
                                final List<Integer> subfolderIds = FolderObject.getSubfolderIds(folderId, ctx, con);
                                final List<String> subfolderIdentifies = new ArrayList<String>(subfolderIds.size());
                                for (final Integer id : subfolderIds) {
                                    subfolderIdentifies.add(id.toString());
                                }
                                retval.setSubfolderIDs(subfolderIdentifies.toArray(new String[subfolderIdentifies.size()]));
                            }
                        }
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
        } catch (final DBPoolingException e) {
            throw new FolderException(e);
        } catch (final OXException e) {
            throw new FolderException(e);
        } catch (final SQLException e) {
            throw FolderExceptionErrorMessage.SQL_ERROR.create(e, e.getMessage());
        }
    }

    public FolderType getFolderType() {
        return DatabaseFolderType.getInstance();
    }

    public SortableId[] getSubfolders(final String treeId, final String parentIdentifier, final StorageParameters storageParameters) throws FolderException {
        try {
            final Connection con = getParameter(Connection.class, DatabaseParameterConstants.PARAM_CONNECTION, storageParameters);

            final int parentId = Integer.parseInt(parentIdentifier);

            if (FolderObject.SYSTEM_ROOT_FOLDER_ID == parentId) {
                final String[] subfolderIds = SystemRootFolder.getSystemRootFolderSubfolder();
                final List<SortableId> list = new ArrayList<SortableId>(subfolderIds.length);
                for (int i = 0; i < subfolderIds.length; i++) {
                    list.add(new DatabaseId(subfolderIds[i], i));
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
                    list.add(new DatabaseId(subfolderIds[i], i));
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
                    list.add(new DatabaseId(subfolderIds[i], i));
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
                    list.add(new DatabaseId(subfolderIds[i], i));
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
                    list.add(new DatabaseId(subfolderIds[i], i));
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
                    list.add(new DatabaseId(subfolderIds[i], i));
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
                list.add(new DatabaseId(subfolders.get(i).getObjectID(), i));
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
            con = getParameter(Connection.class, DatabaseParameterConstants.PARAM_CONNECTION, params);
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

    public StorageParameters startTransaction(final StorageParameters parameters, final boolean modify) throws FolderException {
        final FolderType folderType = getFolderType();
        if (null != parameters.getParameter(folderType, DatabaseParameterConstants.PARAM_CONNECTION)) {
            // Connection already present
            return parameters;
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
            return parameters;
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
            final Connection con = getParameter(Connection.class, DatabaseParameterConstants.PARAM_CONNECTION, storageParameters);
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
                if (null != clientLastModified && getFolderAccess(storageParameters, getFolderType()).getFolderLastModified(folderId).after(
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
                    updateMe.setParentFolderID(getFolderAccess(storageParameters, getFolderType()).getFolderObject(folderId).getParentFolderID());
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
            final Connection con = getParameter(Connection.class, DatabaseParameterConstants.PARAM_CONNECTION, storageParameters);
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
            final Connection con = getParameter(Connection.class, DatabaseParameterConstants.PARAM_CONNECTION, storageParameters);
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
            final Connection con = getParameter(Connection.class, DatabaseParameterConstants.PARAM_CONNECTION, storageParameters);
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
        if (TaskContentType.getInstance().equals(contentType)) {
            return FolderObject.TASK;
        }
        if (CalendarContentType.getInstance().equals(contentType)) {
            return FolderObject.CALENDAR;
        }
        if (ContactContentType.getInstance().equals(contentType)) {
            return FolderObject.CONTACT;
        }
        if (InfostoreContentType.getInstance().equals(contentType)) {
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

        private int compareById(final int id1, final int id2) {
            return (id1 < id2 ? -1 : (id1 == id2 ? 0 : 1));
        }

    } // End of FolderObjectComparator

}
