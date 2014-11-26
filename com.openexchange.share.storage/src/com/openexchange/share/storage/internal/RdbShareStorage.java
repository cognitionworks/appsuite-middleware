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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.share.storage.internal;

import static com.openexchange.share.storage.internal.SQL.SHARE_MAPPER;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.openexchange.database.DatabaseService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Autoboxing;
import com.openexchange.share.Share;
import com.openexchange.share.ShareExceptionCodes;
import com.openexchange.share.ShareTarget;
import com.openexchange.share.storage.ShareStorage;
import com.openexchange.share.storage.StorageParameters;
import com.openexchange.share.storage.internal.ConnectionProvider.ConnectionMode;
import com.openexchange.share.storage.mapping.RdbShare;
import com.openexchange.share.storage.mapping.ShareField;
import com.openexchange.tools.arrays.Arrays;
import com.openexchange.tools.sql.DBUtils;

/**
 * {@link RdbShareStorage}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.0
 */
public class RdbShareStorage implements ShareStorage {

    private final DatabaseService databaseService;

    /**
     * Initializes a new {@link RdbShareStorage}.
     *
     * @param databaseService The database service
     */
    public RdbShareStorage(DatabaseService databaseService) {
        super();
        this.databaseService = databaseService;
    }

    @Override
    public List<Share> loadSharesForGuest(int contextID, int guest, StorageParameters parameters) throws OXException {
        ConnectionProvider provider = getReadProvider(contextID, parameters);
        try {
            return new ShareSelector(contextID).guests(new int[] { guest }).select(provider.get());
        } finally {
            provider.close();
        }
    }

    @Override
    public List<Share> loadSharesForContext(int contextID, StorageParameters parameters) throws OXException {
        ConnectionProvider provider = getReadProvider(contextID, parameters);
        try {
            return new ShareSelector(contextID).select(provider.get());
        } finally {
            provider.close();
        }
    }

    @Override
    public List<Share> loadSharesExpiredAfter(int contextID, Date expires, StorageParameters parameters) throws OXException {
        ConnectionProvider provider = getReadProvider(contextID, parameters);
        try {
            return new ShareSelector(contextID).expiredAfter(expires).select(provider.get());
        } finally {
            provider.close();
        }
    }

    @Override
    public int deleteSharesExpiredAfter(int contextID, Date expires, StorageParameters parameters) throws OXException {
        ConnectionProvider provider = getWriteProvider(contextID, parameters);
        try {
            return new ShareSelector(contextID).expiredAfter(expires).delete(provider.get());
        } finally {
            provider.close();
        }
    }

    @Override
    public boolean hasShares(int contextID, int guest, StorageParameters parameters) throws OXException {
        ConnectionProvider provider = getReadProvider(contextID, parameters);
        try {
            return hasShares(provider.get(), contextID, guest);
        } catch (SQLException e) {
            throw ShareExceptionCodes.DB_ERROR.create(e, e.getMessage());
        } finally {
            provider.close();
        }
    }

    @Override
    public Set<Integer> getSharedModules(int contextID, int guest, StorageParameters parameters) throws OXException {
        ConnectionProvider provider = getReadProvider(contextID, parameters);
        try {
            return getModules(provider.get(), contextID, guest);
        } catch (SQLException e) {
            throw ShareExceptionCodes.DB_ERROR.create(e, e.getMessage());
        } finally {
            provider.close();
        }
    }

    @Override
    public Set<Integer> getSharingUsers(int contextID, int guest, StorageParameters parameters) throws OXException {
        ConnectionProvider provider = getReadProvider(contextID, parameters);
        try {
            return getCreatedByForGuest(provider.get(), contextID, guest);
        } catch (SQLException e) {
            throw ShareExceptionCodes.DB_ERROR.create(e, e.getMessage());
        } finally {
            provider.close();
        }
    }

    @Override
    public List<Share> loadSharesCreatedBy(int contextID, int createdBy, StorageParameters parameters) throws OXException {
        ConnectionProvider provider = getReadProvider(contextID, parameters);
        try {
            return new ShareSelector(contextID).createdBy(createdBy).select(provider.get());
        } finally {
            provider.close();
        }
    }

    @Override
    public List<Share> loadSharesForModule(int contextID, int moduleId, StorageParameters parameters) throws OXException {
        ConnectionProvider provider = getReadProvider(contextID, parameters);
        try {
            return new ShareSelector(contextID).forModule(moduleId).select(provider.get());
        } finally {
            provider.close();
        }
    }

    @Override
    public void storeShares(int contextID, List<Share> shares, StorageParameters parameters) throws OXException {
        ConnectionProvider provider = getWriteProvider(contextID, parameters);
        try {
            insertOrUpdateShares(provider.get(), contextID, shares);
        } catch (SQLException e) {
            throw ShareExceptionCodes.DB_ERROR.create(e, e.getMessage());
        } finally {
            provider.close();
        }
    }

    @Override
    public void updateShares(int contextID, List<Share> shares, Date clientLastModified, StorageParameters parameters) throws OXException {
        ConnectionProvider provider = getWriteProvider(contextID, parameters);
        try {
            updateShares(provider.get(), contextID, shares, clientLastModified.getTime());
        } catch (SQLException e) {
            throw ShareExceptionCodes.DB_ERROR.create(e, e.getMessage());
        } finally {
            provider.close();
        }
    }

    @Override
    public int deleteShares(int contextID, List<Share> shares, StorageParameters parameters) throws OXException {
        ConnectionProvider provider = getWriteProvider(contextID, parameters);
        try {
            return SQL.sumUpdateCount(deleteShares(provider.get(), contextID, shares));
        } catch (SQLException e) {
            throw ShareExceptionCodes.DB_ERROR.create(e, e.getMessage());
        } finally {
            provider.close();
        }
    }

    @Override
    public int[] deleteTargets(int contextID, List<ShareTarget> targets, StorageParameters parameters) throws OXException {
        return deleteTargets(contextID, targets, false, parameters);
    }

    @Override
    public int[] deleteTargets(int contextID, List<ShareTarget> targets, boolean includeItems, StorageParameters parameters) throws OXException {
        ConnectionProvider provider = getWriteProvider(contextID, parameters);
        try {
            int[] guestIDs = selectGuests(provider.get(), contextID, targets, includeItems);
            if (0 < guestIDs.length) {
                deleteTargets(provider.get(), contextID, targets, includeItems);
            }
            return guestIDs;
        } catch (SQLException e) {
            throw ShareExceptionCodes.DB_ERROR.create(e, e.getMessage());
        } finally {
            provider.close();
        }
    }

    @Override
    public int countShares(int contextID, int createdBy, StorageParameters parameters) throws OXException {
        ConnectionProvider provider = getReadProvider(contextID, parameters);
        try {
            return new ShareSelector(contextID).createdBy(createdBy).count(provider.get());
        } finally {
            provider.close();
        }
    }

    private static int[] insertOrUpdateShares(Connection connection, int contextID, List<Share> shares) throws SQLException, OXException {
        ShareField[] updatableFields = {
            ShareField.EXPIRES, ShareField.META, ShareField.MODIFIED, ShareField.MODIFIED_BY
        };
        ShareField[] parameterFields = Arrays.add(ShareField.values(), updatableFields);
        StringBuilder stringBuilder = new StringBuilder().append("INSERT INTO share (")
            .append(SHARE_MAPPER.getColumns(ShareField.values())).append(") VALUES (")
            .append(SHARE_MAPPER.getParameters(ShareField.values().length)).append(") ON DUPLICATE KEY UPDATE ")
            .append(SHARE_MAPPER.getAssignments(updatableFields)).append(';')
        ;
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(stringBuilder.toString());
            for (Share share : shares) {
                SHARE_MAPPER.setParameters(stmt, new RdbShare(contextID, share), parameterFields);
                stmt.addBatch();
            }
            return SQL.logExecuteBatch(stmt);
        } finally {
            DBUtils.closeSQLStuff(stmt);
        }
    }

    private static int[] updateShares(Connection connection, int contextID, List<Share> shares, long maxLastModified) throws SQLException, OXException {
        ShareField[] updatableFields = {
            ShareField.EXPIRES, ShareField.META, ShareField.MODIFIED, ShareField.MODIFIED_BY
        };
        StringBuilder stringBuilder = new StringBuilder().append("UPDATE share SET ")
            .append(SHARE_MAPPER.getAssignments(updatableFields)).append(" WHERE ")
            .append(SHARE_MAPPER.get(ShareField.CONTEXT_ID).getColumnLabel()).append("=? AND ")
            .append(SHARE_MAPPER.get(ShareField.GUEST).getColumnLabel()).append("=? AND ")
            .append(SHARE_MAPPER.get(ShareField.MODULE).getColumnLabel()).append("=? AND ")
            .append(SHARE_MAPPER.get(ShareField.FOLDER).getColumnLabel()).append("=? AND ")
            .append(SHARE_MAPPER.get(ShareField.ITEM).getColumnLabel()).append("=? AND ")
            .append(SHARE_MAPPER.get(ShareField.MODIFIED).getColumnLabel()).append("<=?;")
        ;
        ShareField[] parameterFields = Arrays.add(updatableFields,
            ShareField.CONTEXT_ID, ShareField.GUEST, ShareField.MODULE, ShareField.FOLDER, ShareField.ITEM, ShareField.MODIFIED);
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(stringBuilder.toString());
            for (Share share : shares) {
                SHARE_MAPPER.setParameters(stmt, new RdbShare(contextID, share), parameterFields);
                stmt.addBatch();
            }
            return SQL.logExecuteBatch(stmt);
        } finally {
            DBUtils.closeSQLStuff(stmt);
        }
    }

    private static int[] deleteShares(Connection connection, int contextID, List<Share> shares) throws SQLException, OXException {
        StringBuilder stringBuilder = new StringBuilder().append("DELETE FROM share WHERE ")
            .append(SHARE_MAPPER.get(ShareField.CONTEXT_ID).getColumnLabel()).append("=? AND ")
            .append(SHARE_MAPPER.get(ShareField.GUEST).getColumnLabel()).append("=? AND ")
            .append(SHARE_MAPPER.get(ShareField.MODULE).getColumnLabel()).append("=? AND ")
            .append(SHARE_MAPPER.get(ShareField.FOLDER).getColumnLabel()).append("=? AND ")
            .append(SHARE_MAPPER.get(ShareField.ITEM).getColumnLabel()).append("=?;")
        ;
        ShareField[] fields = { ShareField.CONTEXT_ID, ShareField.GUEST, ShareField.MODULE, ShareField.FOLDER, ShareField.ITEM };
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(stringBuilder.toString());
            for (Share share : shares) {
                SHARE_MAPPER.setParameters(stmt, new RdbShare(contextID, share), fields);
                stmt.addBatch();
            }
            return SQL.logExecuteBatch(stmt);
        } finally {
            DBUtils.closeSQLStuff(stmt);
        }
    }

    private static int[] deleteTargets(Connection connection, int contextID, List<ShareTarget> targets, boolean includeItems) throws SQLException, OXException {
        StringBuilder stringBuilder = new StringBuilder()
            .append("DELETE FROM share WHERE ")
            .append(SHARE_MAPPER.get(ShareField.CONTEXT_ID).getColumnLabel()).append("=? AND ")
            .append(SHARE_MAPPER.get(ShareField.MODULE).getColumnLabel()).append("=? AND ")
            .append(SHARE_MAPPER.get(ShareField.FOLDER).getColumnLabel()).append("=?")
        ;
        ShareField[] fields;
        if (includeItems) {
            fields = new ShareField[] { ShareField.CONTEXT_ID, ShareField.MODULE, ShareField.FOLDER };
            stringBuilder.append(';');
        } else {
            fields = new ShareField[] { ShareField.CONTEXT_ID, ShareField.MODULE, ShareField.FOLDER, ShareField.ITEM };
            stringBuilder.append(" AND ").append(SHARE_MAPPER.get(ShareField.ITEM).getColumnLabel()).append("=?;");
        }
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(stringBuilder.toString());
            for (ShareTarget target : targets) {
                SHARE_MAPPER.setParameters(stmt, new RdbShare(contextID, new Share(0, target)), fields);
                stmt.addBatch();
            }
            return SQL.logExecuteBatch(stmt);
        } finally {
            DBUtils.closeSQLStuff(stmt);
        }
    }

    private static int[] selectGuests(Connection connection, int contextID, List<ShareTarget> targets, boolean includeItems) throws SQLException, OXException {
        StringBuilder stringBuilder = new StringBuilder()
            .append("SELECT DISTINCT ").append(SHARE_MAPPER.get(ShareField.GUEST).getColumnLabel()).append(" FROM share WHERE ")
            .append(SHARE_MAPPER.get(ShareField.CONTEXT_ID).getColumnLabel()).append("=? AND (")
            .append(SHARE_MAPPER.get(ShareField.MODULE).getColumnLabel()).append("=?").append(" AND ")
            .append(SHARE_MAPPER.get(ShareField.FOLDER).getColumnLabel()).append("=?");
        if (false == includeItems) {
            stringBuilder.append(" AND ").append(SHARE_MAPPER.get(ShareField.ITEM).getColumnLabel()).append("=?");
        }
        for (int i = 1; i < targets.size(); i++) {
            stringBuilder.append(" OR ").append(SHARE_MAPPER.get(ShareField.MODULE).getColumnLabel()).append("=?")
                .append(" AND ").append(SHARE_MAPPER.get(ShareField.FOLDER).getColumnLabel()).append("=?");
            if (false == includeItems) {
                stringBuilder.append(" AND ").append(SHARE_MAPPER.get(ShareField.ITEM).getColumnLabel()).append("=?");
            }
        }
        stringBuilder.append(");");
        List<Integer> guests = new ArrayList<Integer>();
        ResultSet result = null;
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(stringBuilder.toString());
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, contextID);
            for (ShareTarget target : targets) {
                RdbShare share = new RdbShare(contextID, new Share(0, target));
                SHARE_MAPPER.get(ShareField.MODULE).set(stmt, parameterIndex++, share);
                SHARE_MAPPER.get(ShareField.FOLDER).set(stmt, parameterIndex++, share);
                if (false == includeItems) {
                    SHARE_MAPPER.get(ShareField.ITEM).set(stmt, parameterIndex++, share);
                }
            }
            result = SQL.logExecuteQuery(stmt);
            while (result.next()) {
                guests.add(Integer.valueOf(result.getInt(1)));
            }
        } finally {
            DBUtils.closeSQLStuff(result, stmt);
        }
        return Autoboxing.I2i(guests);
    }

    private static boolean hasShares(Connection connection, int contextID, int guest) throws SQLException, OXException {
        StringBuilder stringBuilder = new StringBuilder().append("SELECT DISTINCT 1 FROM share WHERE ")
            .append(SHARE_MAPPER.get(ShareField.CONTEXT_ID).getColumnLabel()).append("=? AND ")
            .append(SHARE_MAPPER.get(ShareField.GUEST).getColumnLabel()).append("=?;")
        ;
        ResultSet result = null;
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(stringBuilder.toString());
            stmt.setInt(1, contextID);
            stmt.setInt(2, guest);
            result = SQL.logExecuteQuery(stmt);
            return null != result && result.next();
        } finally {
            DBUtils.closeSQLStuff(result, stmt);
        }
    }

    private static Set<Integer> getModules(Connection connection, int contextID, int guest) throws SQLException, OXException {
        StringBuilder stringBuilder = new StringBuilder().append("SELECT DISTINCT ")
            .append(SHARE_MAPPER.get(ShareField.MODULE).getColumnLabel()).append(" FROM share WHERE ")
            .append(SHARE_MAPPER.get(ShareField.CONTEXT_ID).getColumnLabel()).append("=? AND ")
            .append(SHARE_MAPPER.get(ShareField.GUEST).getColumnLabel()).append("=?;")
        ;
        Set<Integer> modules = new HashSet<Integer>();
        ResultSet result = null;
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(stringBuilder.toString());
            stmt.setInt(1, contextID);
            stmt.setInt(2, guest);
            result = SQL.logExecuteQuery(stmt);
            while (result.next()) {
                modules.add(Integer.valueOf(result.getInt(1)));
            }
        } finally {
            DBUtils.closeSQLStuff(result, stmt);
        }
        return modules;
    }

    private static Set<Integer> getCreatedByForGuest(Connection connection, int contextID, int guest) throws SQLException, OXException {
        StringBuilder stringBuilder = new StringBuilder()
            .append("SELECT DISTINCT ").append(SHARE_MAPPER.get(ShareField.CREATED_BY).getColumnLabel()).append(" FROM share WHERE ")
            .append(SHARE_MAPPER.get(ShareField.CONTEXT_ID).getColumnLabel()).append("=? AND ")
            .append(SHARE_MAPPER.get(ShareField.GUEST).getColumnLabel()).append("=?;")
        ;
        Set<Integer> modules = new HashSet<Integer>();
        ResultSet result = null;
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(stringBuilder.toString());
            stmt.setInt(1, contextID);
            stmt.setInt(2, guest);
            result = SQL.logExecuteQuery(stmt);
            while (result.next()) {
                modules.add(Integer.valueOf(result.getInt(1)));
            }
        } finally {
            DBUtils.closeSQLStuff(result, stmt);
        }
        return modules;
    }

    private ConnectionProvider getReadProvider(int contextId, StorageParameters parameters) throws OXException {
        return new ConnectionProvider(databaseService, parameters, ConnectionMode.READ, contextId);
    }

    private ConnectionProvider getWriteProvider(int contextId, StorageParameters parameters) throws OXException {
        return new ConnectionProvider(databaseService, parameters, ConnectionMode.WRITE, contextId);
    }

}
