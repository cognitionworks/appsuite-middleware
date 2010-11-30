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
import com.openexchange.folderstorage.Folder;
import com.openexchange.folderstorage.FolderException;
import com.openexchange.folderstorage.FolderExceptionErrorMessage;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.FolderStorageDiscoverer;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.SortableId;
import com.openexchange.folderstorage.internal.CalculatePermission;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link UnsubscribePerformer} - Serves the <code>UNSUBSCRIBE</code> action.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class UnsubscribePerformer extends AbstractPerformer {

    /**
     * Initializes a new {@link UnsubscribePerformer}.
     * 
     * @param session
     */
    public UnsubscribePerformer(final ServerSession session) {
        super(session);
    }

    /**
     * Initializes a new {@link UnsubscribePerformer}.
     * 
     * @param user
     * @param context
     */
    public UnsubscribePerformer(final User user, final Context context) {
        super(user, context);
    }

    /**
     * Initializes a new {@link UnsubscribePerformer}.
     * 
     * @param session The session
     * @param folderStorageDiscoverer The folder storage discoverer
     */
    public UnsubscribePerformer(final ServerSession session, final FolderStorageDiscoverer folderStorageDiscoverer) {
        super(session, folderStorageDiscoverer);
    }

    /**
     * Initializes a new {@link UnsubscribePerformer}.
     * 
     * @param user The user
     * @param context The context
     * @param folderStorageDiscoverer The folder storage discoverer
     */
    public UnsubscribePerformer(final User user, final Context context, final FolderStorageDiscoverer folderStorageDiscoverer) {
        super(user, context, folderStorageDiscoverer);
    }

    /**
     * Performs the <code>UNSUBSCRIBE</code> action.
     * 
     * @param treeId The virtual tree identifier
     * @param folderId The folder identifier
     * @throws FolderException If a folder error occurs
     */
    public void doUnsubscribe(final String treeId, final String folderId) throws FolderException {
        if (FolderStorage.REAL_TREE_ID.equals(treeId)) {
            throw FolderExceptionErrorMessage.NO_REAL_UNSUBSCRIBE.create(treeId);
        }
        final FolderStorage virtualStorage = folderStorageDiscoverer.getFolderStorage(treeId, folderId);
        if (null == virtualStorage) {
            throw FolderExceptionErrorMessage.NO_STORAGE_FOR_ID.create(treeId, folderId);
        }
        final java.util.List<FolderStorage> openedStorages = new ArrayList<FolderStorage>(4);
        if (virtualStorage.startTransaction(storageParameters, false)) {
            openedStorages.add(virtualStorage);
        }
        try {
            final Folder folder = virtualStorage.getFolder(treeId, folderId, storageParameters);
            {
                /*
                 * Check folder permission for parent folder
                 */
                final Permission parentPermission;
                if (null == getSession()) {
                    parentPermission = CalculatePermission.calculate(folder, getUser(), getContext(), ALL_ALLOWED);
                } else {
                    parentPermission = CalculatePermission.calculate(folder, getSession(), ALL_ALLOWED);
                }
                if (!parentPermission.isVisible()) {
                    throw FolderExceptionErrorMessage.FOLDER_NOT_VISIBLE.create(
                        getFolderInfo4Error(folder),
                        getUserInfo4Error(),
                        getContextInfo4Error());
                }
            }
            {
                /*
                 * No unsubscribe on a folder which has subfolders
                 */
                final String[] ids = folder.getSubfolderIDs();
                if (null == ids) {
                    final SortableId[] tmp = virtualStorage.getSubfolders(treeId, folderId, storageParameters);
                    if (tmp.length > 0) {
                        throw FolderExceptionErrorMessage.NO_UNSUBSCRIBE.create(folderId, treeId);
                    }
                } else {
                    if (ids.length > 0) {
                        throw FolderExceptionErrorMessage.NO_UNSUBSCRIBE.create(folderId, treeId);
                    }
                }
            }

            virtualStorage.deleteFolder(treeId, folderId, storageParameters);

            for (final FolderStorage fs : openedStorages) {
                fs.commitTransaction(storageParameters);
            }
        } catch (final FolderException e) {
            for (final FolderStorage fs : openedStorages) {
                fs.rollback(storageParameters);
            }
            throw e;
        } catch (final Exception e) {
            for (final FolderStorage fs : openedStorages) {
                fs.rollback(storageParameters);
            }
            throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
        }

    }

}
