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

import java.sql.Connection;
import java.util.Set;

import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.tasks.TaskException.Code;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.tools.oxfolder.OXFolderNotFoundException;

/**
 * This class collects all information for getting tasks. It is also able to
 * check permissions in a fast way.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class GetTask {

    private final Context ctx;

    /**
     * read only database connection.
     */
    private final Connection con;

    private User user;

    private UserConfiguration userConfig;

    private final int folderId;

    private FolderObject folder;

    private final int taskId;

    private Task task;

    private final StorageType type;

    private Set<TaskParticipant> participants;

    private Set<Folder> folderMapping;

    /**
     * The task storage.
     */
    private final TaskStorage storage = TaskStorage.getInstance();

    /**
     * The participant storage.
     */
    private final ParticipantStorage partStor = ParticipantStorage.getInstance();

    /**
     * The folder storage.
     */
    private final FolderStorage foldStor = FolderStorage.getInstance();

    /**
     * Use this constructor if you want permission checks.
     */
    GetTask(final Context ctx, final User user,
        final UserConfiguration userConfig, final int folderId,
        final int taskId, final StorageType type) {
        this(ctx, null, user, userConfig, folderId, taskId, type);
    }

    /**
     * Use this constructor if you want permission checks.
     */
    GetTask(final Context ctx, final Connection con, final User user,
        final UserConfiguration userConfig, final int folderId,
        final int taskId, final StorageType type) {
        this.ctx = ctx;
        this.con = con;
        this.user = user;
        this.userConfig = userConfig;
        this.folderId = folderId;
        this.taskId = taskId;
        this.type = type;
    }

    /**
     * This constructor can be used if permission checks should not be done.
     */
    GetTask(final Context ctx, final int folderId, final int taskId,
        final StorageType type) {
        this(ctx, null, folderId, taskId, type);
    }

    /**
     * This constructor can be used if permission checks should not be done.
     */
    GetTask(final Context ctx, final Connection con, final int folderId,
        final int taskId, final StorageType type) {
        this.ctx = ctx;
        this.con = con;
        this.folderId = folderId;
        this.taskId = taskId;
        this.type = type;
    }

    /**
     * TODO instanciate this class with the normal folder object.
     */
    private FolderObject getFolder() throws TaskException {
        if (null == folder) {
            if (null == con) {
                folder = Tools.getFolder(ctx, folderId);
            } else {
                try {
                    folder = Tools.getFolder(ctx, con, folderId);
                } catch (final OXFolderNotFoundException e) {
                    throw new TaskException(e);
                }
            }
        }
        return folder;
    }

    private Task getTask() throws TaskException {
        if (null == task) {
            if (null == con) {
                task = storage.selectTask(ctx, taskId, type);
            } else {
                task = storage.selectTask(ctx, con, taskId, type);
            }
        }
        return task;
    }

    private Set<TaskParticipant> getParticipants() throws TaskException {
        if (null == participants) {
            if (null == con) {
                participants = partStor.selectParticipants(ctx, taskId, type);
            } else {
                participants = partStor.selectParticipants(ctx, con, taskId,
                    type);
            }
        }
        return participants;
    }

    private Set<Folder> getFolders() throws TaskException {
        if (null == folderMapping) {
            if (null == con) {
                folderMapping =  foldStor.selectFolder(ctx, taskId, type);
            } else {
                folderMapping =  foldStor.selectFolder(ctx, con, taskId, type);
            }
        }
        return folderMapping;
    }

    static Task load(final Context ctx, final Connection con,
        final int folderId, final int taskId, final StorageType type)
        throws TaskException {
        return new GetTask(ctx, con, folderId, taskId, type).load();
    }

    static Task load(final Context ctx, final int folderId, final int taskId,
        final StorageType type) throws TaskException {
        return new GetTask(ctx, folderId, taskId, type).load();
    }

    /**
     * Loads the task without checking permission. Use
     * {@link #checkPermission()} for checking access permissions. Use
     * {@link #fillReminder()} if the reminder for the loading user should be
     * loaded.
     */
    Task load() throws TaskException {
        fillParticipants();
        fillTask();
        return getTask();
    }

    Task loadAndCheck() throws TaskException {
        checkPermission();
        fillParticipants();
        fillTask();
        fillReminder();
        return getTask();
    }

    void checkPermission() throws TaskException {
        if (null == user || null == userConfig) {
            throw new TaskException(Code.UNIMPLEMENTED);
        }
        if (null == con) {
            Permission.canReadInFolder(ctx, user, userConfig, getFolder(),
                getTask());
        } else {
            Permission.canReadInFolder(ctx, con, user, userConfig, getFolder(),
                getTask());
        }
        final Folder check = FolderStorage.getFolder(getFolders(),
            folderId);
        if (null == check
            || (Tools.isFolderShared(getFolder(), user)
                && getTask().getPrivateFlag())) {
            throw new TaskException(Code.NO_PERMISSION, Integer.valueOf(taskId),
                getFolder().getFolderName(), Integer.valueOf(folderId));
        }
    }

    private boolean filledParts = false;

    private void fillParticipants() throws TaskException {
        if (filledParts) {
            return;
        }
        if (!Tools.isFolderPublic(getFolder())) {
            Tools.fillStandardFolders(ctx.getContextId(), taskId, getParticipants(), getFolders(), true);
        }
        filledParts = true;
    }

    private boolean filledTask = false;

    private void fillTask() throws TaskException {
        if (filledTask) {
            return;
        }
        final Task task = getTask();
        task.setParticipants(TaskLogic.createParticipants(getParticipants()));
        task.setUsers(TaskLogic.createUserParticipants(getParticipants()));
        task.setParentFolderID(folderId);
        filledTask = true;
    }

    void fillReminder() throws TaskException {
        Reminder.loadReminder(ctx, getUserId(), getTask());
    }

    /* ---------- Convenience methods ---------- */

    private int getUserId() throws TaskException {
        if (null == user) {
            throw new TaskException(Code.UNIMPLEMENTED);
        }
        return user.getId();
    }
}
