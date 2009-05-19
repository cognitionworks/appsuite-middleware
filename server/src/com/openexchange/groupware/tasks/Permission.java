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

package com.openexchange.groupware.tasks;

import static com.openexchange.java.Autoboxing.I;
import java.sql.Connection;

import com.openexchange.api2.OXException;
import com.openexchange.database.DBPoolingException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.tasks.TaskException.Code;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.server.impl.DBPool;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.tools.oxfolder.OXFolderAccess;

/**
 * Contains convenience methods for checking access rights.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class Permission {

    /**
     * Prevent instantiation
     */
    private Permission() {
        super();
    }

    /**
     * Checks if a user 
     * @param ctx
     * @param user
     * @param userConfig
     * @param folder
     * @param task
     * @throws TaskException
     */
    public static void checkDelete(final Context ctx, final User user, final UserConfiguration userConfig, final FolderObject folder, final Task task) throws TaskException {
        checkForTaskFolder(folder);
        final OCLPermission permission = getPermission(ctx, user, userConfig, folder);
        if (!permission.canDeleteAllObjects() && !permission.canDeleteOwnObjects()) {
            throw new TaskException(Code.NO_DELETE_PERMISSION);
        }
        final boolean onlyOwn = !permission.canDeleteAllObjects() && permission.canDeleteOwnObjects();
        if (onlyOwn && task.getCreatedBy() != user.getId()) {
            throw new TaskException(Code.NO_DELETE_PERMISSION);
        }
        final boolean noPrivate = Tools.isFolderShared(folder, user);
        if (noPrivate && task.getPrivateFlag()) {
            throw new TaskException(Code.NO_DELETE_PERMISSION);
        }
    }

    /**
     * Checks if the user is allowed to delegate tasks.
     * @param userConfig groupware configuration of the user.
     * @param participants Participants of a task.
     * @throws TaskException if delegation is not allowed.
     */
    static void checkDelegation(final UserConfiguration userConfig, final Participant[] participants) throws TaskException {
        if (!userConfig.canDelegateTasks() && null != participants && participants.length > 0) {
            throw new TaskException(Code.NO_DELEGATE_PERMISSION);
        }
    }

    /**
     * Checks if a user is allowed to create a task in a folder.
     * @param ctx Context.
     * @param user User.
     * @param userConfig Module configuration of the user.
     * @param folder Folder in that a task should be created.
     * @throws TaskException if the user is not allowed to create the task.
     */
    static void checkCreate(final Context ctx, final User user, final UserConfiguration userConfig, final FolderObject folder) throws TaskException {
        if (!userConfig.hasTask()) {
            throw new TaskException(Code.NO_TASKS, Integer.valueOf(user.getId()));
        }
        checkForTaskFolder(folder);
        final OCLPermission permission = getPermission(ctx, user, userConfig, folder);
        if (!permission.canCreateObjects()) {
            throw new TaskException(Code.NO_CREATE_PERMISSION, folder.getFolderName(), I(folder.getObjectID()));
        }
    }

    /**
     * Checks if the user is only allowed to see the folder.
     * @param ctx Context.
     * @param user User.
     * @param userConfig Groupware configuration of the user.
     * @param folder folder object that should be tested for only see
     * permission.
     * @return <code>true</code> if the folder can only be seen.
     * @throws TaskException if the folder is not a task folder or getting the
     * user specific permissions fails.
     */
    static boolean canOnlySeeFolder(final Context ctx, final User user, final UserConfiguration userConfig, final FolderObject folder) throws TaskException {
        checkForTaskFolder(folder);
        final OCLPermission permission = getPermission(ctx, user, userConfig, folder);
        return permission.isFolderVisible() && !permission.canReadAllObjects() && !permission.canReadOwnObjects();
    }

    static boolean isFolderVisible(final Context ctx, final User user, final UserConfiguration userConfig, final FolderObject folder) throws TaskException {
        checkForTaskFolder(folder);
        final OCLPermission permission = getPermission(ctx, user, userConfig, folder);
        return permission.isFolderVisible();
    }

    /**
     * Checks if the user is allowed to read tasks in a folder. Beware that the
     * private flag of tasks must be checked.
     * @param ctx Context.
     * @param user User.
     * @param userConfig Groupware configuration of the user.
     * @param folder folder object that should be tested for read access.
     * @throws TaskException if the reading is not okay.
     */
    static boolean canReadInFolder(final Context ctx, final User user, final UserConfiguration userConfig, final FolderObject folder) throws TaskException {
        final Connection con;
        try {
            con = DBPool.pickup(ctx);
        } catch (final DBPoolingException e) {
            throw new TaskException(e);
        }
        try {
            return canReadInFolder(ctx, con, user, userConfig, folder);
        } finally {
            DBPool.closeReaderSilent(ctx, con);
        }
    }

    /**
     * Checks if the user is allowed to read tasks in a folder. Beware that the
     * private flag of tasks must be checked.
     * @param ctx Context.
     * @param con read only database connection.
     * @param user User.
     * @param userConfig Groupware configuration of the user.
     * @param folder folder object that should be tested for read access.
     * @throws TaskException if the reading is not okay.
     */
    static boolean canReadInFolder(final Context ctx, final Connection con, final User user, final UserConfiguration userConfig, final FolderObject folder) throws TaskException {
        checkForTaskFolder(folder);
        final OCLPermission permission = getPermission(ctx, con, user, userConfig, folder);
        if (!permission.canReadAllObjects() && !permission.canReadOwnObjects()) {
            throw new TaskException(Code.NO_READ_PERMISSION, folder.getFolderName(), I(folder.getObjectID()));
        }
        return !permission.canReadAllObjects() && permission.canReadOwnObjects();
    }

    /**
     * Checks if the user is allowed to read the task.
     * @param ctx Context.
     * @param user User.
     * @param userConfig Groupware configuration of the user.
     * @param folder folder object that should be tested for read access.
     * @param task Task to read.
     * @throws TaskException if the reading is not okay.
     */
    static void canReadInFolder(final Context ctx, final User user, final UserConfiguration userConfig, final FolderObject folder, final Task task) throws TaskException {
        final Connection con;
        try {
            con = DBPool.pickup(ctx);
        } catch (final DBPoolingException e) {
            throw new TaskException(e);
        }
        try {
            canReadInFolder(ctx, con, user, userConfig, folder, task);
        } finally {
            DBPool.closeReaderSilent(ctx, con);
        }
    }
    
    /**
     * Checks if the user is allowed to read the task.
     * @param ctx Context.
     * @param con read only database connection.
     * @param user User.
     * @param userConfig Groupware configuration of the user.
     * @param folder folder object that should be tested for read access.
     * @param task Task to read.
     * @throws TaskException if the reading is not okay.
     */
    static void canReadInFolder(final Context ctx, final Connection con, final User user, final UserConfiguration userConfig, final FolderObject folder, final Task task) throws TaskException {
        final boolean onlyOwn = canReadInFolder(ctx, con, user, userConfig, folder);
        if (onlyOwn && (user.getId() != task.getCreatedBy())) {
            throw new TaskException(Code.NO_READ_PERMISSION, folder.getFolderName(), I(folder.getObjectID()));
        }
    }

    /**
     * Checks if a user is allowed to update a task.
     * @param ctx Context.
     * @param user User.
     * @param userConfig Groupware configuration of the user.
     * @param folder folder object that should be tested for write access.
     * @param task Task to update.
     * @throws TaskException if the task can't be updated.
     */
    static void checkWriteInFolder(final Context ctx, final User user, final UserConfiguration userConfig, final FolderObject folder, final Task task) throws TaskException {
        checkForTaskFolder(folder);
        final OCLPermission permission = getPermission(ctx, user, userConfig, folder);
        if (!permission.canWriteAllObjects() && !(permission.canWriteOwnObjects() && (user.getId() == task.getCreatedBy()))) {
            throw new TaskException(Code.NO_WRITE_PERMISSION, folder.getFolderName(), I(folder.getObjectID()));
        }
    }

    static OCLPermission getPermission(final Context ctx, final User user, final UserConfiguration userConfig, final FolderObject folder) throws TaskException {
        try {
            return new OXFolderAccess(ctx).getFolderPermission(folder.getObjectID(), user.getId(), userConfig);
        } catch (final OXException e) {
            throw new TaskException(e);
        }
    }

    static OCLPermission getPermission(final Context ctx, final Connection con, final User user, final UserConfiguration userConfig, final FolderObject folder) throws TaskException {
        try {
            return new OXFolderAccess(con, ctx).getFolderPermission(folder.getObjectID(), user.getId(), userConfig);
        } catch (final OXException e) {
            throw new TaskException(e);
        }
    }

    static void checkForTaskFolder(final FolderObject folder) throws TaskException {
        if (!Tools.isFolderTask(folder)) {
            throw new TaskException(Code.NOT_TASK_FOLDER, folder.getFolderName(), I(folder.getObjectID()));
        }
    }
}
