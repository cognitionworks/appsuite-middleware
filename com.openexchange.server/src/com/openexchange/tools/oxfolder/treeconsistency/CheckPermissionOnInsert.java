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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.tools.oxfolder.treeconsistency;

import static com.openexchange.java.Autoboxing.I;
import java.sql.Connection;
import java.sql.SQLException;
import org.osgi.service.event.EventAdmin;
import com.openexchange.cache.impl.FolderCacheManager;
import com.openexchange.cache.impl.FolderQueryCacheManager;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.FolderPermissionType;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.tools.oxfolder.OXFolderExceptionCode;
import com.openexchange.tools.oxfolder.OXFolderSQL;
import com.openexchange.tools.oxfolder.memory.ConditionTreeMapManagement;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * {@link CheckPermissionOnInsert} - Checks for system permissions which shall be inserted.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CheckPermissionOnInsert extends CheckPermission {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CheckPermissionOnInsert.class);

    /**
     * Initializes a new {@link CheckPermissionOnInsert}
     *
     * @param session The session
     * @param writeCon A connection with write capability
     * @param ctx The context
     */
    public CheckPermissionOnInsert(final Session session, final Connection writeCon, final Context ctx) {
        super(session, writeCon, ctx);
    }

    /**
     * Checks for parental visibility permissions and adds a folder-read-only-permission for non-tree-visible parent folder if user has
     * admin permission (optional).
     *
     * @param parent The parent folder ID
     * @param perms The current permissions that shall be applied to affected folder
     * @param lastModified The last-modified time stamp to use when adding permissions
     * @throws OXException If checking parental visibility permissions fails
     */
    public void checkParentPermissions(final int parent, final OCLPermission[] perms, final long lastModified) throws OXException {
        try {
            final TIntObjectMap<ToDoPermission> map = new TIntObjectHashMap<ToDoPermission>();
            for (int i = 0; i < perms.length; i++) {
                final OCLPermission assignedPerm = perms[i];
                if (assignedPerm.isFolderVisible()) {
                    /*
                     * Grant system-permission for this entity to parent folders
                     */
                    ensureParentVisibility(parent, assignedPerm.getEntity(), assignedPerm.isGroupPermission(), map);
                }
            }
            /*
             * Auto-insert system-folder-read permission to make possible non-visible parent folders visible in folder tree
             */
            int mapSize = map.size();
            if (mapSize > 0) {
                final TIntObjectIterator<ToDoPermission> it = map.iterator();
                for ( int i = mapSize; i-- > 0; ) {
                    it.advance();
                    final int folderId = it.key();
                    /*
                     * Insert read permissions
                     */
                    final ToDoPermission toDoPermission = it.value();
                    {
                        int[] users = toDoPermission.getUsers();
                        int length = users.length;
                        if (length > 0) {
                            for (int j = length; j-- > 0;) {
                                LOG.debug("Auto-Insert system-folder-read permission for user {} to folder {}", I(users[j]), I(folderId));
                                addSystemFolderReadPermission(folderId, users[j], false);
                            }
                        }
                    }
                    {
                        int[] groups = toDoPermission.getGroups();
                        int length = groups.length;
                        if (length > 0) {
                            for (int j = length; j-- > 0;) {
                                LOG.debug("Auto-Insert system-folder-read permission for group {} to folder {}", I(groups[j]), I(folderId));
                                addSystemFolderReadPermission(folderId, groups[j], true);
                            }
                        }
                    }
                    /*
                     * Update folders last-modified
                     */
                    OXFolderSQL.updateLastModified(folderId, lastModified, ctx.getMailadmin(), writeCon, ctx);
                    /*
                     * Update caches
                     */
                    ConditionTreeMapManagement.dropFor(ctx.getContextId());
                    try {
                        if (FolderCacheManager.isEnabled()) {
                            FolderCacheManager.getInstance().removeFolderObject(folderId, ctx);
                        }
                        broadcastEvent(folderId, true, ServerServiceRegistry.getInstance().getService(EventAdmin.class));
                        if (FolderQueryCacheManager.isInitialized()) {
                            FolderQueryCacheManager.getInstance().invalidateContextQueries(session);
                        }
                    } catch (OXException e) {
                        LOG.error("", e);
                    }
                }
            }
        } catch (SQLException e) {
            throw OXFolderExceptionCode.SQL_ERROR.create(e, e.getMessage());
        }
    }

    private void ensureParentVisibility(final int parent, final int entity, final boolean isGroup, final TIntObjectMap<ToDoPermission> map) throws OXException, SQLException {
        if (parent < FolderObject.MIN_FOLDER_ID) {
            /*
             * We reached a context-created folder
             */
            return;
        }
        final FolderObject parentFolder = getFolderFromMaster(parent);
        /*
         * Check for system-read-folder permission for current entity
         */
        if (!containsSystemPermission(parentFolder.getPermissions(), entity)) {
            /*
             * Add system-read-folder permission for current entity
             */
            ToDoPermission todo = map.get(parent);
            if (todo == null) {
                todo = new ToDoPermission(parent);
                map.put(parent, todo);
            }
            if (isGroup) {
                todo.addGroup(entity);
            } else {
                todo.addUser(entity);
            }
        }
        /*
         * Recursive call with parent's parent
         */
        ensureParentVisibility(parentFolder.getParentFolderID(), entity, isGroup, map);
    }

    /**
     * Adds system-read-folder permission to specified folder for given entity
     *
     * @param folderId The folder ID
     * @param entity The entity
     * @param isGroup whether entity denotes a group
     * @throws OXException If a pooling error occurs
     * @throws SQLException If a SQL error occurs
     */
    private void addSystemFolderReadPermission(final int folderId, final int entity, final boolean isGroup) throws OXException, SQLException {
        /*
         * Add folder-read permission
         */
        OXFolderSQL.addSinglePermission(
            folderId,
            entity,
            isGroup,
            OCLPermission.READ_FOLDER,
            OCLPermission.NO_PERMISSIONS,
            OCLPermission.NO_PERMISSIONS,
            OCLPermission.NO_PERMISSIONS,
            false,
            OCLPermission.SYSTEM_SYSTEM,
            FolderPermissionType.NORMAL,
            null,
            writeCon,
            ctx);
    }

}
