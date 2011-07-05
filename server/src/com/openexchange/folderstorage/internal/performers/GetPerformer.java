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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.FolderExceptionErrorMessage;
import com.openexchange.folderstorage.FolderServiceDecorator;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.FolderStorageDiscoverer;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.folderstorage.internal.CalculatePermission;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link GetPerformer} - Serves the <code>GET</code> request.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class GetPerformer extends AbstractUserizedFolderPerformer {

    private static final org.apache.commons.logging.Log LOG = com.openexchange.exception.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(GetPerformer.class));

    private static final boolean DEBUG_ENABLED = LOG.isDebugEnabled();

    /**
     * Initializes a new {@link GetPerformer}.
     * 
     * @param session The session
     * @param decorator The optional folder service decorator
     */
    public GetPerformer(final ServerSession session, final FolderServiceDecorator decorator) {
        super(session, decorator);
    }

    /**
     * Initializes a new {@link GetPerformer}.
     * 
     * @param user The user
     * @param context The context
     * @param decorator The optional folder service decorator
     */
    public GetPerformer(final User user, final Context context, final FolderServiceDecorator decorator) {
        super(user, context, decorator);
    }

    /**
     * Initializes a new {@link GetPerformer}.
     * 
     * @param session The session
     * @param decorator The optional folder service decorator
     * @param folderStorageDiscoverer The folder storage discoverer
     */
    public GetPerformer(final ServerSession session, final FolderServiceDecorator decorator, final FolderStorageDiscoverer folderStorageDiscoverer) {
        super(session, decorator, folderStorageDiscoverer);
    }

    /**
     * Initializes a new {@link GetPerformer}.
     * 
     * @param user The user
     * @param context The context
     * @param decorator The optional folder service decorator
     * @param folderStorageDiscoverer The folder storage discoverer
     */
    public GetPerformer(final User user, final Context context, final FolderServiceDecorator decorator, final FolderStorageDiscoverer folderStorageDiscoverer) {
        super(user, context, decorator, folderStorageDiscoverer);
    }

    public UserizedFolder doGet(final String treeId, final String folderId) throws OXException {
        final FolderStorage folderStorage = folderStorageDiscoverer.getFolderStorage(treeId, folderId);
        if (null == folderStorage) {
            throw FolderExceptionErrorMessage.NO_STORAGE_FOR_ID.create(treeId, folderId);
        }
        final long start = DEBUG_ENABLED ? System.currentTimeMillis() : 0L;
        final java.util.List<FolderStorage> openedStorages = new ArrayList<FolderStorage>(4);
        if (folderStorage.startTransaction(storageParameters, false)) {
            openedStorages.add(folderStorage);
        }
        try {
            final Folder folder = folderStorage.getFolder(treeId, folderId, storageParameters);
            /*
             * Check folder permission for folder
             */
            final Permission ownPermission;
            if (null == getSession()) {
                ownPermission = CalculatePermission.calculate(folder, getUser(), getContext(), getAllowedContentTypes());
            } else {
                ownPermission = CalculatePermission.calculate(folder, getSession(), getAllowedContentTypes());
            }
            if (!ownPermission.isVisible()) {
                throw FolderExceptionErrorMessage.FOLDER_NOT_VISIBLE.create(
                    getFolderInfo4Error(folder),
                    getUserInfo4Error(),
                    getContextInfo4Error());
            }
            // TODO: All or only subscribed subfolders?
            final UserizedFolder userizedFolder =
                getUserizedFolder(folder, ownPermission, treeId, true, true, storageParameters, openedStorages);
            if (DEBUG_ENABLED) {
                final long duration = System.currentTimeMillis() - start;
                LOG.debug(new StringBuilder().append("Get.doGet() took ").append(duration).append("msec for folder: ").append(folderId).toString());
            }

            /*
             * Commit
             */
            for (final FolderStorage fs : openedStorages) {
                fs.commitTransaction(storageParameters);
            }

            return userizedFolder;
        } catch (final OXException e) {
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
