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

package com.openexchange.tools.oxfolder;

import static com.openexchange.tools.sql.DBUtils.closeResources;
import static com.openexchange.tools.sql.DBUtils.closeSQLStuff;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectProcedure;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import com.openexchange.api2.OXException;
import com.openexchange.database.DBPoolingException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.server.impl.DBPool;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.tools.oxfolder.OXFolderException.FolderCode;

/**
 * {@link OXFolderBatchLoader}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class OXFolderBatchLoader {

    private static abstract class ErrorAwareTObjectProcedure<V> implements TObjectProcedure<V> {

        protected OXFolderException error;

        protected ErrorAwareTObjectProcedure() {
            super();
        }

        /**
         * Gets the possible error.
         * 
         * @return The error or <code>null</code> if no error occurred
         */
        public OXFolderException getError() {
            return error;
        }

    }

    private static final class FolderPermissionProcedure extends ErrorAwareTObjectProcedure<FolderObject> {

        private final Context ctx;

        private final TIntObjectHashMap<List<OCLPermission>> folderPermissions;

        private final Connection readCon;

        public FolderPermissionProcedure(final Context ctx, final Connection readCon, final TIntObjectHashMap<List<OCLPermission>> folderPermissions) {
            this.ctx = ctx;
            this.folderPermissions = folderPermissions;
            this.readCon = readCon;
        }

        public boolean execute(final FolderObject fo) {
            final int id = fo.getObjectID();
            final List<OCLPermission> permissions = folderPermissions.get(id);
            try {
                fo.setPermissionsNoClone(permissions == null ? Arrays.asList(OXFolderLoader.getFolderPermissions(id, ctx, readCon)) : permissions);
                return true;
            } catch (final DBPoolingException e) {
                error = new OXFolderException(e);
            } catch (final SQLException e) {
                error = new OXFolderException(OXFolderException.FolderCode.SQL_ERROR, e, e.getMessage());
            }
            return false;
        }
    }

    private static final class SubfolderProcedure extends ErrorAwareTObjectProcedure<FolderObject> {

        private final Context ctx;

        private final Connection readCon;

        private final TIntObjectHashMap<ArrayList<Integer>> subfolderIds;

        public SubfolderProcedure(final Context ctx, final Connection readCon, final TIntObjectHashMap<ArrayList<Integer>> subfolderIds) {
            this.ctx = ctx;
            this.readCon = readCon;
            this.subfolderIds = subfolderIds;
        }

        public boolean execute(final FolderObject fo) {
            final int id = fo.getObjectID();
            final ArrayList<Integer> ids = subfolderIds.get(id);
            try {
                fo.setSubfolderIds(ids == null ? OXFolderLoader.getSubfolderIds(id, ctx, readCon) : ids);
                return true;
            } catch (final DBPoolingException e) {
                error = new OXFolderException(e);
            } catch (final SQLException e) {
                error = new OXFolderException(OXFolderException.FolderCode.SQL_ERROR, e, e.getMessage());
            }
            return false;
        }

    }

    private static final String TABLE_OT = "oxfolder_tree";

    private static final String TABLE_OP = "oxfolder_permissions";

    private static final Pattern PAT_RPL_TABLE = Pattern.compile("#TABLE#");

    /**
     * Initializes a new {@link OXFolderBatchLoader}.
     */
    private OXFolderBatchLoader() {
        super();
    }

    public static List<FolderObject> loadFolderObjectsFromDB(final int[] folderIds, final Context ctx) throws OXException {
        return loadFolderObjectsFromDB(folderIds, ctx, null, true, false);
    }

    public static List<FolderObject> loadFolderObjectsFromDB(final int[] folderIds, final Context ctx, final Connection readCon) throws OXException {
        return loadFolderObjectsFromDB(folderIds, ctx, readCon, true, false);
    }

    /**
     * Loads specified folder from database.
     * 
     * @param folderId The folder ID
     * @param ctx The context
     * @param readConArg A connection with read capability; may be <code>null</code> to fetch from pool
     * @param loadPermissions <code>true</code> to load folder's permissions, otherwise <code>false</code>
     * @param loadSubfolderList <code>true</code> to load subfolders, otherwise <code>false</code>
     * @return The loaded folder object from database
     * @throws OXException If folder cannot be loaded
     */
    public static final List<FolderObject> loadFolderObjectsFromDB(final int[] folderIds, final Context ctx, final Connection readConArg, final boolean loadPermissions, final boolean loadSubfolderList) throws OXException {
        return loadFolderObjectsFromDB(folderIds, ctx, readConArg, loadPermissions, loadSubfolderList, TABLE_OT, TABLE_OP);
    }

    private static final int LIMIT = 1000;

    /**
     * Loads specified folder from database.
     * 
     * @param folderIds The folder IDs
     * @param ctx The context
     * @param readConArg A connection with read capability; may be <code>null</code> to fetch from pool
     * @param loadPermissions <code>true</code> to load folder's permissions, otherwise <code>false</code>
     * @param loadSubfolderList <code>true</code> to load subfolders, otherwise <code>false</code>
     * @param table The folder's working or backup table name
     * @param permTable The folder permissions' working or backup table name
     * @return The loaded folder object from database
     * @throws OXException If folder cannot be loaded
     */
    public static final List<FolderObject> loadFolderObjectsFromDB(final int[] folderIds, final Context ctx, final Connection readConArg, final boolean loadPermissions, final boolean loadSubfolderList, final String table, final String permTable) throws OXException {
        try {
            Connection readCon = readConArg;
            boolean closeCon = false;
            try {
                if (readCon == null) {
                    readCon = DBPool.pickup(ctx);
                    closeCon = true;
                }
                final FolderObject[] array = new FolderObject[folderIds.length];
                int pos = 0;
                if ((folderIds.length - pos) > LIMIT) {
                    final TIntIntHashMap indexes = new TIntIntHashMap(folderIds.length);
                    for (int i = 0; i < folderIds.length; i++) {
                        indexes.put(folderIds[i], i);
                    }
                    /*
                     * Chunked loading
                     */
                    do {
                        final int[] fids = new int[LIMIT];
                        System.arraycopy(folderIds, pos, fids, 0, LIMIT);
                        pos += LIMIT;
                        final TIntObjectHashMap<FolderObject> map = loadFolderObjectsFromDB0(fids, ctx, readCon, loadPermissions, loadSubfolderList, table, permTable);
                        for (int i = 0; i < fids.length; i++) {
                            final int fuid = fids[i];
                            final FolderObject fo = map.get(fuid);
                            array[indexes.get(fuid)] = fo;
                        }
                    } while ((folderIds.length - pos) > LIMIT);
                    if (pos < folderIds.length) {
                        final int len = folderIds.length - pos;
                        final int[] fids = new int[len];
                        System.arraycopy(folderIds, pos, fids, 0, len);
                        final TIntObjectHashMap<FolderObject> map = loadFolderObjectsFromDB0(fids, ctx, readCon, loadPermissions, loadSubfolderList, table, permTable);
                        for (int i = 0; i < fids.length; i++) {
                            final int fuid = fids[i];
                            final FolderObject fo = map.get(fuid);
                            array[indexes.get(fuid)] = fo;
                        }
                    }
                } else {
                    final TIntObjectHashMap<FolderObject> map = loadFolderObjectsFromDB0(folderIds, ctx, readCon, loadPermissions, loadSubfolderList, table, permTable);
                    for (int i = 0; i < folderIds.length; i++) {
                        final int fuid = folderIds[i];
                        final FolderObject fo = map.get(fuid);
                        array[i] = fo;
                    }
                }
                /*
                 * Return list
                 */
                return Arrays.asList(array);
            } finally {
                if (closeCon) {
                    DBPool.closeReaderSilent(ctx, readCon);
                }
            }
        } catch (final DBPoolingException e) {
            throw new OXFolderException(e);
        }
    }

    private static final TIntObjectHashMap<FolderObject> loadFolderObjectsFromDB0(final int[] folderIds, final Context ctx, final Connection readCon, final boolean loadPermissions, final boolean loadSubfolderList, final String table, final String permTable) throws OXException {
        try {
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                /*
                 * Compose statement
                 */
                {
                    final StringBuilder sb = new StringBuilder(512);
                    sb.append("SELECT parent, fname, module, type, creating_date, created_from, changing_date, changed_from, permission_flag, subfolder_flag, default_flag, fuid ");
                    sb.append("FROM #TABLE# AS t INNER JOIN (");
                    sb.append("SELECT ? AS fuid");
                    for (int i = 1; i < folderIds.length; i++) {
                        sb.append(" UNION ALL SELECT ?");
                    }
                    sb.append(") AS x ON t.fuid = x.fuid WHERE t.cid = ?");
                    stmt = readCon.prepareStatement(PAT_RPL_TABLE.matcher(sb.toString()).replaceFirst(table));
                }
                int pos = 1;
                for (final int folderId : folderIds) {
                    stmt.setInt(pos++, folderId);
                }
                stmt.setInt(pos, ctx.getContextId());
                rs = stmt.executeQuery();
                pos = 0;
                final TIntObjectHashMap<FolderObject> folders = new TIntObjectHashMap<FolderObject>();
                while (rs.next()) {
                    final int fuid = rs.getInt(12);
                    final FolderObject folderObj = new FolderObject(rs.getString(2), fuid, rs.getInt(3), rs.getInt(4), rs.getInt(6));
                    folderObj.setParentFolderID(rs.getInt(1));
                    folderObj.setCreatedBy(parseStringValue(rs.getString(6), ctx));
                    folderObj.setCreationDate(new Date(rs.getLong(5)));
                    folderObj.setSubfolderFlag(rs.getInt(10) > 0 ? true : false);
                    folderObj.setLastModified(new Date(rs.getLong(7)));
                    folderObj.setModifiedBy(parseStringValue(rs.getString(8), ctx));
                    folderObj.setPermissionFlag(rs.getInt(9));
                    final int defaultFolder = rs.getInt(11);
                    if (rs.wasNull()) {
                        folderObj.setDefaultFolder(false);
                    } else {
                        folderObj.setDefaultFolder(defaultFolder > 0);
                    }
                    folders.put(fuid, folderObj);
                }
                if (loadSubfolderList) {
                    final SubfolderProcedure procedure =
                        new SubfolderProcedure(ctx, readCon, getSubfolderIds(folderIds, ctx, readCon, table));
                    if (!folders.forEachValue(procedure)) {
                        final OXFolderException error = procedure.getError();
                        if (null != error) {
                            throw error;
                        }
                    }
                }
                if (loadPermissions) {
                    final FolderPermissionProcedure procedure =
                        new FolderPermissionProcedure(ctx, readCon, getFolderPermissions(folderIds, ctx, readCon, permTable));
                    if (!folders.forEachValue(procedure)) {
                        final OXFolderException error = procedure.getError();
                        if (null != error) {
                            throw error;
                        }
                    }
                }
                return folders;
            } finally {
                closeSQLStuff(rs, stmt);
            }
        } catch (final SQLException e) {
            throw new OXFolderException(FolderCode.SQL_ERROR, e, e.getMessage());
        } catch (final DBPoolingException e) {
            throw new OXFolderException(e);
        }
    }

    /**
     * Loads folder permissions from database. Creates a new connection if <code>null</code> is given.
     * 
     * @param folderIds The folder IDs
     * @param ctx The context
     * @param readConArg A connection with read capability; may be <code>null</code> to fetch from pool
     * @return The folder's permissions
     * @throws SQLException If a SQL error occurs
     * @throws DBPoolingException If a pooling error occurs
     */
    public static final TIntObjectHashMap<List<OCLPermission>> getFolderPermissions(final int[] folderIds, final Context ctx, final Connection readConArg) throws SQLException, DBPoolingException {
        return getFolderPermissions(folderIds, ctx, readConArg, TABLE_OP);
    }

    /**
     * Loads folder permissions from database. Creates a new connection if <code>null</code> is given.
     * 
     * @param folderId The folder ID
     * @param ctx The context
     * @param readCon A connection with read capability; may be <code>null</code> to fetch from pool
     * @param table Either folder permissions working or backup table name
     * @return The folder's permissions
     * @throws SQLException If a SQL error occurs
     * @throws DBPoolingException If a pooling error occurs
     */
    public static final TIntObjectHashMap<List<OCLPermission>> getFolderPermissions(final int[] folderIds, final Context ctx, final Connection readConArg, final String table) throws SQLException, DBPoolingException {
        Connection readCon = readConArg;
        boolean closeCon = false;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (readCon == null) {
                readCon = DBPool.pickup(ctx);
                closeCon = true;
            }
            {
                final StringBuilder sb = new StringBuilder(512);
                sb.append("SELECT permission_id, fp, orp, owp, odp, admin_flag, group_flag, system, p.fuid FROM #TABLE# AS p INNER JOIN (");
                sb.append("SELECT ? AS fuid ");
                for (int i = 1; i < folderIds.length; i++) {
                    sb.append(" UNION ALL SELECT ?");
                }
                sb.append(") AS x ON p.fuid = x.fuid WHERE cid = ?");
                stmt = readCon.prepareStatement(PAT_RPL_TABLE.matcher(sb.toString()).replaceFirst(table));
            }
            int pos = 1;
            for (final int folderId : folderIds) {
                stmt.setInt(pos++, folderId);
            }
            stmt.setInt(pos, ctx.getContextId());
            rs = stmt.executeQuery();
            final TIntObjectHashMap<List<OCLPermission>> ret = new TIntObjectHashMap<List<OCLPermission>>(folderIds.length);
            while (rs.next()) {
                final int fuid = rs.getInt(9);
                List<OCLPermission> list = ret.get(fuid);
                if (null == list) {
                    list = new ArrayList<OCLPermission>(4);
                    ret.put(fuid, list);
                }
                final OCLPermission p = new OCLPermission();
                p.setEntity(rs.getInt(1)); // Entity
                p.setAllPermission(rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5)); // fp, orp, owp, and odp
                p.setFolderAdmin(rs.getInt(6) > 0 ? true : false); // admin_flag
                p.setGroupPermission(rs.getInt(7) > 0 ? true : false); // group_flag
                p.setSystem(rs.getInt(8)); // system
                list.add(p);
            }
            stmt.close();
            rs = null;
            stmt = null;
            return ret;
        } finally {
            closeSQLStuff(rs, stmt);
            if (closeCon) {
                DBPool.closeReaderSilent(ctx, readCon);
            }
        }
    }

    /**
     * Gets the subfolder IDs of specified folder.
     * 
     * @param folderId The IDs of the folders whose subfolders' IDs shall be returned
     * @param ctx The context
     * @param readConArg A connection with read capability; may be <code>null</code> to fetch from pool
     * @return The subfolder IDs of specified folder
     * @throws SQLException If a SQL error occurs
     * @throws DBPoolingException If a pooling error occurs
     */
    public static final TIntObjectHashMap<ArrayList<Integer>> getSubfolderIds(final int[] folderIds, final Context ctx, final Connection readConArg) throws SQLException, DBPoolingException {
        return getSubfolderIds(folderIds, ctx, readConArg, TABLE_OT);
    }

    /**
     * Gets the subfolder IDs of specified folders.
     * 
     * @param folderIds The IDs of the folders whose subfolders' IDs shall be returned
     * @param ctx The context
     * @param readConArg A connection with read capability; may be <code>null</code> to fetch from pool
     * @param table The folder's working or backup table name
     * @return The subfolder IDs of specified folder
     * @throws SQLException If a SQL error occurs
     * @throws DBPoolingException If a pooling error occurs
     */
    public static final TIntObjectHashMap<ArrayList<Integer>> getSubfolderIds(final int[] folderIds, final Context ctx, final Connection readConArg, final String table) throws SQLException, DBPoolingException {
        Connection readCon = readConArg;
        boolean closeCon = false;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (readCon == null) {
                readCon = DBPool.pickup(ctx);
                closeCon = true;
            }
            {
                final StringBuilder sb = new StringBuilder(512);
                sb.append("SELECT fuid, t.parent FROM #TABLE# AS t INNER JOIN (");
                sb.append("SELECT ? AS parent ");
                for (int i = 1; i < folderIds.length; i++) {
                    sb.append(" UNION ALL SELECT ?");
                }
                sb.append(") AS x ON t.parent = x.parent WHERE cid = ? ORDER BY default_flag DESC, fname");
                stmt = readCon.prepareStatement(sb.toString().replaceFirst("#TABLE#", table));
            }
            int pos = 1;
            for (final int folderId : folderIds) {
                stmt.setInt(pos++, folderId);
            }
            stmt.setInt(pos, ctx.getContextId());
            rs = stmt.executeQuery();
            final TIntObjectHashMap<ArrayList<Integer>> ret = new TIntObjectHashMap<ArrayList<Integer>>(folderIds.length);
            while (rs.next()) {
                final int parent = rs.getInt(2);
                ArrayList<Integer> list = ret.get(parent);
                if (null == list) {
                    list = new ArrayList<Integer>(16);
                    ret.put(parent, list);
                }
                list.add(Integer.valueOf(rs.getInt(1)));
            }
            return ret;
        } finally {
            closeResources(rs, stmt, closeCon ? readCon : null, true, ctx);
        }
    }

    private static final int parseStringValue(final String str, final Context ctx) {
        if (null == str) {
            return -1;
        }
        try {
            return Integer.parseInt(str);
        } catch (final NumberFormatException e) {
            if (str.equalsIgnoreCase("system")) {
                return ctx.getMailadmin();
            }
        }
        return -1;
    }

}
