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

package com.openexchange.groupware.update.tasks;

import static com.openexchange.tools.sql.DBUtils.autocommit;
import static com.openexchange.tools.sql.DBUtils.closeSQLStuff;
import static com.openexchange.tools.sql.DBUtils.rollback;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.database.DBPoolingException;
import com.openexchange.databaseold.Database;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.EnumComponent;
import com.openexchange.groupware.OXExceptionSource;
import com.openexchange.groupware.OXThrowsMultiple;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.ProgressState;
import com.openexchange.groupware.update.UpdateTaskAdapter;
import com.openexchange.groupware.update.exception.Classes;
import com.openexchange.groupware.update.exception.UpdateException;
import com.openexchange.groupware.update.exception.UpdateExceptionFactory;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.tools.oxfolder.OXFolderProperties;

/**
 * {@link GlobalAddressBookPermissionsResolverTask} - Resolves GAB's group permission to individual user permissions.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@OXExceptionSource(classId = Classes.UPDATE_TASK, component = EnumComponent.UPDATE)
public final class GlobalAddressBookPermissionsResolverTask extends UpdateTaskAdapter {

    private static final Log LOG = LogFactory.getLog(GlobalAddressBookPermissionsResolverTask.class);

    /**
     * Initializes a new {@link GlobalAddressBookPermissionsResolverTask}.
     */
    public GlobalAddressBookPermissionsResolverTask() {
        super();
    }

    public int addedWithVersion() {
        return 94;
    }

    public int getPriority() {
        return UpdateTaskPriority.HIGH.priority;
    }

    public void perform(PerformParameters params) throws AbstractOXException {
        ProgressState status = params.getProgressState();
        /*
         * Get all contexts with contained users
         */
        final Map<Integer, List<Integer>> m = getAllUsers(params.getContextId());
        status.setTotal(m.size());
        /*
         * Iterate per context
         */
        for (Map.Entry<Integer, List<Integer>> me : m.entrySet()) {
            final int currentContextId = me.getKey().intValue();
            try {
                iterateUsersPerContext(me.getValue(), currentContextId);
            } catch (final AbstractOXException e) {
                final StringBuilder sb = new StringBuilder(128);
                sb.append("GlobalAddressBookPermissionsResolverTask experienced an error while resolving to individual permissions for users in context ");
                sb.append(currentContextId);
                sb.append(":\n");
                sb.append(e.getMessage());
                LOG.error(sb.toString(), e);
            }
            status.incrementState();
        }
    }

    private static Map<Integer, List<Integer>> getAllUsers(final int contextId) throws UpdateException {
        final Connection readCon;
        try {
            readCon = Database.getNoTimeout(contextId, false);
        } catch (final DBPoolingException e) {
            throw new UpdateException(e);
        }
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = readCon.prepareStatement("SELECT cid, id FROM user");
            rs = stmt.executeQuery();
            if (!rs.next()) {
                return Collections.emptyMap();
            }
            final Map<Integer, List<Integer>> m = new HashMap<Integer, List<Integer>>();
            do {
                final Integer cid = Integer.valueOf(rs.getInt(1));
                final Integer user = Integer.valueOf(rs.getInt(2));
                final List<Integer> l;
                if (!m.containsKey(cid)) {
                    l = new ArrayList<Integer>();
                    m.put(cid, l);
                } else {
                    l = m.get(cid);
                }
                l.add(user);
            } while (rs.next());
            return m;
        } catch (final SQLException e) {
            throw createSQLError(e);
        } finally {
            closeSQLStuff(rs, stmt);
            Database.backNoTimeout(contextId, false, readCon);
        }
    }

    private static void iterateUsersPerContext(final List<Integer> users, final int contextId) throws UpdateException {
        /*
         * Fetch write-connection
         */
        final Connection writeCon;
        try {
            writeCon = Database.get(contextId, true);
            writeCon.setAutoCommit(false); // BEGIN
        } catch (final DBPoolingException e) {
            throw new UpdateException(e);
        } catch (final SQLException e) {
            // Auto-Commit mode could not be changed
            throw createSQLError(e);
        }
        try {
            checkGABPermissions4Users(users, contextId, writeCon);
            writeCon.commit(); // COMMIT
        } catch (final SQLException e) {
            rollback(writeCon);
            throw createSQLError(e);
        } catch (final UpdateException e) {
            rollback(writeCon);
            throw e;
        } catch (final Exception e) {
            rollback(writeCon);
            throw new UpdateException(
                EnumComponent.UPDATE,
                Category.CODE_ERROR,
                9999,
                "Unexpected error: %1$s",
                e,
                new Object[] { e.getMessage() });
        } finally {
            /*
             * Release write-connection
             */
            autocommit(writeCon); // RESTORE AUTO-COMMIT
            Database.back(contextId, true, writeCon);
        }
    }

    private static void checkGABPermissions4Users(final List<Integer> users, final int contextId, final Connection writeCon) throws UpdateException {
        boolean found = false;
        final Set<Integer> detectedUsers = new HashSet<Integer>();
        int[] permissions = null;
        /*
         * SQL variables
         */
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt =
                writeCon.prepareStatement("SELECT permission_id, fp, orp, owp, odp FROM oxfolder_permissions WHERE cid = ? AND fuid = ?");
            stmt.setInt(1, contextId);
            stmt.setInt(2, FolderObject.SYSTEM_LDAP_FOLDER_ID);
            rs = stmt.executeQuery();
            while (rs.next()) {
                final int permissionId = rs.getInt(1);
                if (OCLPermission.ALL_GROUPS_AND_USERS == permissionId) {
                    found = true;
                    permissions = new int[4];
                    permissions[0] = rs.getInt(2);
                    permissions[1] = rs.getInt(3);
                    permissions[2] = rs.getInt(4);
                    permissions[3] = rs.getInt(5);
                } else {
                    detectedUsers.add(Integer.valueOf(permissionId));
                }
            }
            closeSQLStuff(rs, stmt);
            rs = null;
            /*
             * Drop permission if found
             */
            if (found) {
                stmt = writeCon.prepareStatement("DELETE FROM oxfolder_permissions WHERE cid = ? AND fuid = ? AND permission_id = ?");
                stmt.setInt(1, contextId);
                stmt.setInt(2, FolderObject.SYSTEM_LDAP_FOLDER_ID);
                stmt.setInt(3, OCLPermission.ALL_GROUPS_AND_USERS);
                stmt.executeUpdate();
            }
        } catch (final SQLException e) {
            throw createSQLError(e);
        } finally {
            closeSQLStuff(rs, stmt);
            rs = null;
            stmt = null;
        }
        /*
         * Check missing permissions
         */
        checkMissingUserPermissions(users, detectedUsers, permissions, contextId, writeCon);
        /*
         * Log
         */
        if (LOG.isInfoEnabled()) {
            LOG.info(new StringBuilder("Global Address Book permission resolved for context ").append(contextId).toString());
        }
    }

    private static void checkMissingUserPermissions(final List<Integer> users, final Set<Integer> detectedUsers, final int[] permissions, final int contextId, final Connection writeCon) throws UpdateException {
        /*
         * Any missing permission?
         */
        if (detectedUsers.size() < users.size()) {
            final int[] permissions2Insert = null == permissions ? getConfiguredPermissions() : permissions;
            final int adminUserId;
            try {
                adminUserId = getContextMailAdmin(writeCon, contextId);
            } catch (final SQLException e) {
                throw createSQLError(e);
            }
            PreparedStatement stmt = null;
            try {
                stmt =
                    writeCon.prepareStatement("INSERT INTO oxfolder_permissions (cid, fuid, permission_id, fp, orp, owp, odp, admin_flag, group_flag, system) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                int pos;
                for (final Integer userId : users) {
                    if (!detectedUsers.contains(userId)) {
                        final int user = userId.intValue();
                        // Insert
                        pos = 1;
                        stmt.setInt(pos++, contextId); // cid
                        stmt.setInt(pos++, FolderObject.SYSTEM_LDAP_FOLDER_ID); // fuid
                        stmt.setInt(pos++, user); // permission_id
                        stmt.setInt(pos++, permissions2Insert[0]); // fp
                        stmt.setInt(pos++, permissions2Insert[1]); // orp
                        stmt.setInt(pos++, permissions2Insert[2]); // owp
                        stmt.setInt(pos++, OCLPermission.NO_PERMISSIONS); // odp
                        stmt.setInt(pos++, user == adminUserId ? 1 : 0); // admin_flag
                        stmt.setInt(pos++, 0); // group_flag
                        stmt.setInt(pos++, 0); // system
                        stmt.addBatch();
                    }
                }
                stmt.executeBatch();
            } catch (final SQLException e) {
                throw createSQLError(e);
            } finally {
                closeSQLStuff(stmt);
                stmt = null;
            }
        }
    }

    private static int[] getConfiguredPermissions() {
        if (OXFolderProperties.isEnableInternalUsersEdit()) {
            return new int[] {
                OCLPermission.READ_FOLDER, OCLPermission.READ_ALL_OBJECTS, OCLPermission.WRITE_OWN_OBJECTS, OCLPermission.NO_PERMISSIONS };
        }
        return new int[] {
            OCLPermission.READ_FOLDER, OCLPermission.READ_ALL_OBJECTS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS };
    }

    private static final String SQL_GET_CONTEXT_MAILADMIN = "SELECT user FROM user_setting_admin WHERE cid = ?";

    private static int getContextMailAdmin(final Connection con, final int cid) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement(SQL_GET_CONTEXT_MAILADMIN);
            stmt.setInt(1, cid);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return -1;
        } finally {
            closeSQLStuff(rs, stmt);
        }
    }

    @OXThrowsMultiple(category = { Category.CODE_ERROR }, desc = { "" }, exceptionId = { 1 }, msg = { "A SQL error occurred while performing task GlobalAddressBookPermissionsResolverTask: %1$s." })
    private static UpdateException createSQLError(final SQLException e) {
        return new UpdateExceptionFactory(GlobalAddressBookPermissionsResolverTask.class).create(1, e, e.getMessage());
    }

}
