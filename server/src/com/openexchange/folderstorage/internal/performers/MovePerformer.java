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

package com.openexchange.folderstorage.internal.performers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.openexchange.folderstorage.AbstractFolder;
import com.openexchange.folderstorage.Folder;
import com.openexchange.folderstorage.FolderException;
import com.openexchange.folderstorage.FolderExceptionErrorMessage;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.FolderStorageDiscoverer;
import com.openexchange.folderstorage.SortableId;
import com.openexchange.folderstorage.StorageParameters;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link MovePerformer} - Serves the <code>UPDATE</code> request.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
final class MovePerformer extends AbstractPerformer {

    private static final class FolderInfo {

        final String id;

        final String name;

        final List<FolderInfo> subfolders;

        final Map<String, FolderInfo> subfoldersMap;

        public FolderInfo(final String id, final String name) {
            super();
            this.id = id;
            this.name = name;
            this.subfolders = new ArrayList<FolderInfo>();
            subfoldersMap = new HashMap<String, FolderInfo>();
        }

        public void addSubfolder(final FolderInfo subfolder) {
            subfolders.add(subfolder);
            subfoldersMap.put(subfolder.name, subfolder);
        }

        public FolderInfo getByName(final String name) {
            return subfoldersMap.get(name);
        }

        @Override
        public String toString() {
            return new StringBuilder("{id=").append(id).append(", name=").append(name).append(", subfolders=").append(subfolders.toString()).append(
                '}').toString();
        }

    }

    private static final class UpdateFolder extends AbstractFolder {

        private static final long serialVersionUID = -6666991788068206301L;

        public UpdateFolder() {
            super();
        }

        public boolean isGlobalID() {
            return false;
        }

        @Override
        public boolean isCacheable() {
            return false;
        }
    }

    /**
     * Initializes a new {@link MovePerformer} from given session.
     * 
     * @param session The session
     */
    MovePerformer(final ServerSession session) {
        super(session);
    }

    /**
     * Initializes a new {@link MovePerformer} from given user-context-pair.
     * 
     * @param user The user
     * @param context The context
     */
    MovePerformer(final User user, final Context context) {
        super(user, context);
    }

    /**
     * Initializes a new {@link MovePerformer}.
     * 
     * @param session The session
     * @param folderStorageDiscoverer The folder storage discoverer
     */
    MovePerformer(final ServerSession session, final FolderStorageDiscoverer folderStorageDiscoverer) {
        super(session, folderStorageDiscoverer);
    }

    /**
     * Initializes a new {@link MovePerformer}.
     * 
     * @param user The user
     * @param context The context
     * @param folderStorageDiscoverer The folder storage discoverer
     */
    MovePerformer(final User user, final Context context, final FolderStorageDiscoverer folderStorageDiscoverer) {
        super(user, context, folderStorageDiscoverer);
    }

    void doMoveReal(final Folder folder, final FolderStorage folderStorage, final FolderStorage realParentStorage, final FolderStorage newRealParentStorage) throws FolderException {
        // if (folderStorage.equals(realParentStorage) && newRealParentStorage.equals(realParentStorage)) {
        // throw FolderExceptionErrorMessage.MOVE_NOT_PERMITTED.create(new Object[0]);
        // }
        folderStorage.updateFolder(folder, storageParameters);
    }

    void doMoveVirtual(final Folder folder, final FolderStorage virtualStorage, final FolderStorage realParentStorage, final FolderStorage newRealParentStorage, final Folder storageFolder, final List<FolderStorage> openedStorages) throws FolderException {
        /*
         * Get subfolders
         */
        final FolderStorage realStorage = folderStorageDiscoverer.getFolderStorage(FolderStorage.REAL_TREE_ID, folder.getID());
        if (null == realStorage) {
            throw FolderExceptionErrorMessage.NO_STORAGE_FOR_ID.create(FolderStorage.REAL_TREE_ID, folder.getID());
        }
        final String oldParent = storageFolder.getParentID();
        if (virtualStorage.equals(realStorage)) {
            virtualStorage.updateFolder(folder, storageParameters);
        } else {
            final String treeId = folder.getTreeID();
            /*
             * Equality checks
             */
            final boolean destEquality = realStorage.equals(newRealParentStorage);
            final boolean parentEquality = newRealParentStorage.equals(realParentStorage);
            if (destEquality) {
                /*
                 * Perform the move in virtual storage
                 */
                virtualStorage.updateFolder(folder, storageParameters);
                /*
                 * Remember previous virtual entries
                 */
                final String oldId = folder.getID();
                final FolderInfo oldFolderInfo = new FolderInfo(oldId, storageFolder.getName());
                gatherSubfolders(treeId, oldFolderInfo, storageParameters, virtualStorage);
                /*
                 * Destination folder is compatible: Perform real move
                 */
                checkOpenedStorage(realStorage, openedStorages);
                realStorage.updateFolder(folder, storageParameters);
                final String newId = folder.getID();
                if (!parentEquality) {
                    /*
                     * Delete in virtual storage
                     */
                    virtualStorage.deleteFolder(treeId, oldId, storageParameters);
                }
                /*
                 * Generate map
                 */
                final FolderInfo newFolderInfo = new FolderInfo(newId, folder.getName());
                gatherSubfolders(treeId, newFolderInfo, storageParameters, realStorage);
                final Map<String, String> parentIDMap = generateParentIDMap(oldFolderInfo, newFolderInfo);
                final Map<String, String> idMap = generateIDMap(oldFolderInfo, newFolderInfo);
                for (final Entry<String, String> entry : parentIDMap.entrySet()) {
                    final Folder up = new UpdateFolder();
                    final String id = entry.getKey();
                    up.setID(id);
                    up.setParentID(entry.getValue());
                    up.setTreeID(treeId);
                    final String newIdent = idMap.get(id);
                    if (null != newIdent) {
                        up.setNewID(newIdent);
                    }
                    virtualStorage.updateFolder(up, storageParameters);
                }
                /*
                 * Leave method
                 */
                return;
            }
            /*
             * Other cases
             */
            final boolean parentChildEquality = realStorage.equals(realParentStorage);
            if (parentChildEquality && parentEquality) {
                checkOpenedStorage(realStorage, openedStorages);
                /*
                 * Perform the move in real storage
                 */
                final Folder clone4Real = (Folder) folder.clone();
                clone4Real.setName(nonExistingName(
                    clone4Real.getName(),
                    FolderStorage.REAL_TREE_ID,
                    clone4Real.getParentID(),
                    openedStorages));
                realStorage.updateFolder(clone4Real, storageParameters);
                /*
                 * Perform the move in virtual storage
                 */
                virtualStorage.updateFolder(folder, storageParameters);
                /*
                 * Update new/old parent's last-modified
                 */
                final Date lastModified = clone4Real.getLastModified();
                virtualStorage.updateLastModified(lastModified.getTime(), treeId, folder.getParentID(), storageParameters);
                virtualStorage.updateLastModified(lastModified.getTime(), treeId, oldParent, storageParameters);
            } else if (!parentChildEquality && parentEquality) {
                /*
                 * No real action required in this case. Perform the move in virtual storage only.
                 */
                virtualStorage.updateFolder(folder, storageParameters);
            } else if (parentChildEquality && !parentEquality) {
                /*
                 * Move to default location in real storage
                 */
                checkOpenedStorage(realStorage, openedStorages);
                final String defaultParentId =
                    realStorage.getDefaultFolderID(user, FolderStorage.REAL_TREE_ID, realStorage.getDefaultContentType(), storageParameters);
                if (null == defaultParentId) {
                    /*
                     * No default folder found
                     */
                    throw FolderExceptionErrorMessage.NO_DEFAULT_FOLDER.create(
                        realStorage.getDefaultContentType(),
                        FolderStorage.REAL_TREE_ID);
                }
                // TODO: Check permission for obtained default folder ID?
                final Folder clone4Real = (Folder) folder.clone();
                clone4Real.setParentID(defaultParentId);
                clone4Real.setName(nonExistingName(clone4Real.getName(), FolderStorage.REAL_TREE_ID, defaultParentId, openedStorages));
                realStorage.updateFolder(clone4Real, storageParameters);
                /*
                 * Perform the move in virtual storage
                 */
                virtualStorage.updateFolder(folder, storageParameters);
            } else {
                /*
                 * (!parentChildEquality && !parentEquality) ?
                 */
                throw FolderExceptionErrorMessage.MOVE_NOT_PERMITTED.create(new Object[0]);
            }
        }
    }

    private static void gatherSubfolders(final String treeId, final FolderInfo folder, final StorageParameters params, final FolderStorage storage) throws FolderException {
        final SortableId[] subfolders = storage.getSubfolders(treeId, folder.id, params);
        if (0 == subfolders.length) {
            return;
        }
        /*
         * Iterate subfolders
         */
        for (final SortableId id : subfolders) {
            final String subfolderId = id.getId();
            final FolderInfo subfolder = new FolderInfo(subfolderId, storage.getFolder(treeId, subfolderId, params).getName());
            folder.addSubfolder(subfolder);
            gatherSubfolders(treeId, subfolder, params, storage);
        }
    }

    private static Map<String, String> generateIDMap(final FolderInfo oldFolder, final FolderInfo newFolder) {
        final Map<String, String> map = new HashMap<String, String>();
        fillIDMap(oldFolder, newFolder, map);
        return map;
    }

    private static void fillIDMap(final FolderInfo oldFolder, final FolderInfo newFolder, final Map<String, String> map) {
        map.put(oldFolder.id, newFolder.id);
        for (final FolderInfo oldSubfolder : oldFolder.subfolders) {
            final FolderInfo newSubfolder = newFolder.getByName(oldSubfolder.name);
            if (null != newSubfolder) {
                fillIDMap(oldSubfolder, newSubfolder, map);
            }
        }
    }

    private static Map<String, String> generateParentIDMap(final FolderInfo oldFolder, final FolderInfo newFolder) {
        final Map<String, String> map = new HashMap<String, String>();
        fillParentIDMap(oldFolder, newFolder, map);
        return map;
    }

    private static void fillParentIDMap(final FolderInfo oldFolder, final FolderInfo newFolder, final Map<String, String> map) {
        for (final FolderInfo oldSubfolder : oldFolder.subfolders) {
            map.put(oldSubfolder.id, newFolder.id);
            final FolderInfo newSubfolder = newFolder.getByName(oldSubfolder.name);
            if (null != newSubfolder) {
                fillParentIDMap(oldSubfolder, newSubfolder, map);
            }
        }
    }

    private void checkOpenedStorage(final FolderStorage storage, final List<FolderStorage> openedStorages) throws FolderException {
        for (final FolderStorage openedStorage : openedStorages) {
            if (openedStorage.equals(storage)) {
                return;
            }
        }
        storage.startTransaction(storageParameters, true);
        openedStorages.add(storage);
    }

    private boolean equallyNamedSibling(final String name, final String treeId, final String parentId, final Collection<FolderStorage> openedStorages) throws FolderException {
        final ListPerformer listPerformer;
        if (null == session) {
            listPerformer = new ListPerformer(user, context, null);
        } else {
            listPerformer = new ListPerformer(session, null);
        }
        listPerformer.setStorageParameters(storageParameters);
        final UserizedFolder[] subfolders = listPerformer.doList(treeId, parentId, true, openedStorages);
        for (final UserizedFolder userizedFolder : subfolders) {
            if (name.equals(userizedFolder.getName())) {
                return true;
            }
        }
        return false;
    }

    private String nonExistingName(final String name, final String treeId, final String parentId, final Collection<FolderStorage> openedStorages) throws FolderException {
        final ListPerformer listPerformer;
        if (null == session) {
            listPerformer = new ListPerformer(user, context, null);
        } else {
            listPerformer = new ListPerformer(session, null);
        }
        listPerformer.setStorageParameters(storageParameters);
        final UserizedFolder[] subfolders = listPerformer.doList(treeId, parentId, true, openedStorages);
        final StringBuilder sb = new StringBuilder();
        String nonExistingName = name;
        int i = 0;
        int count = 0;
        while (i < subfolders.length) {
            if (nonExistingName.equals(subfolders[i].getName())) {
                sb.setLength(0);
                sb.append(name).append('_').append(String.valueOf(++count));
                nonExistingName = sb.toString();
                i = 0;
            } else {
                i++;
            }
        }
        return nonExistingName;
    }

}
