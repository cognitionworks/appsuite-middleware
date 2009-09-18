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

package com.openexchange.admin.storage.mysqlStorage;

import static com.openexchange.tools.sql.DBUtils.closeSQLStuff;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openexchange.admin.daemons.ClientAdminThread;
import com.openexchange.admin.properties.AdminProperties;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Database;
import com.openexchange.admin.rmi.dataobjects.MaintenanceReason;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.dataobjects.UserModuleAccess;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.PoolException;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.storage.interfaces.OXToolStorageInterface;
import com.openexchange.admin.storage.interfaces.OXUserStorageInterface;
import com.openexchange.admin.tools.AdminCache;
import com.openexchange.admin.tools.PropertyHandler;
import com.openexchange.database.DBPoolingException;
import com.openexchange.groupware.update.UpdateTaskCollection;

public class OXContextMySQLStorageCommon {

    public static final String LOG_ERROR_CLOSING_STATEMENT = "Error closing statement";

    private static final Log log = LogFactory.getLog(OXContextMySQLStorageCommon.class);

    private final OXUtilMySQLStorageCommon oxutilcommon;

    private static AdminCache cache = null;

    private static PropertyHandler prop = null;

    static {
        cache = ClientAdminThread.cache;
        prop = cache.getProperties();
    }

    public OXContextMySQLStorageCommon() {
        oxutilcommon = new OXUtilMySQLStorageCommon();
    }
    
    // TODO: The average size parameter can be removed if we have an new property handler which can
    // deal right with plugin properties
    public Context getData(final Context ctx, final Connection configdb_con, final long average_size) throws SQLException, PoolException  {
        Connection oxdb_read = null;
        PreparedStatement prep = null;
        final int context_id = ctx.getId();

        try {
            oxdb_read = cache.getConnectionForContext(context_id);

            prep = configdb_con.prepareStatement("SELECT context.name, context.enabled, context.reason_id, context.filestore_id, context.filestore_name, context.quota_max, context_server2db_pool.write_db_pool_id, context_server2db_pool.read_db_pool_id, context_server2db_pool.db_schema, login2context.login_info FROM context LEFT JOIN ( login2context, context_server2db_pool, server ) ON ( context.cid = context_server2db_pool.cid AND context_server2db_pool.server_id = server.server_id AND context.cid = login2context.cid ) WHERE context.cid = ? AND server.name = ?");
            prep.setInt(1, context_id);
            prep.setString(2, prop.getProp(AdminProperties.Prop.SERVER_NAME, "local"));
            ResultSet rs = prep.executeQuery();

            final Context cs = new Context();

            // DATABASE HANDLE
            if (rs.next()) {
                // filestore_id | filestore_name | filestore_login |
                // filestore_passwd | quota_max
                final String name = rs.getString(1); // name
                // name of the context, currently same with contextid
                if (name != null) {
                    cs.setName(name);
                }

                cs.setEnabled(rs.getBoolean(2)); // enabled
                int reason_id = rs.getInt(3); //reason
                // CONTEXT STATE INFOS #
                if (-1 != reason_id) {
                    cs.setMaintenanceReason(new MaintenanceReason(reason_id));
                }
                cs.setFilestoreId(rs.getInt(4)); // filestore_id
                cs.setFilestore_name(rs.getString(5)); //filestorename
                long quota_max = rs.getLong(6); //quota max
                if (quota_max != -1) {
                    quota_max /= Math.pow(2, 20);
                    // set quota max also in context setup object
                    cs.setMaxQuota(quota_max);
                }
                int write_pool = rs.getInt(7); // write_pool_id
                int read_pool = rs.getInt(8); //read_pool_id
                final String db_schema = rs.getString(9); // db_schema
                if (null != db_schema) {
                    cs.setReadDatabase(new Database(read_pool, db_schema));
                    cs.setWriteDatabase(new Database(write_pool, db_schema));
                }
                //DO NOT RETURN THE CONTEXT ID AS A MAPPING!!
                // THIS CAN CAUSE ERRORS IF CHANGING LOGINMAPPINGS AFTERWARDS!
                // SEE #11094 FOR DETAILS!
                String login_mapping = rs.getString(10);   
                if(!ctx.getIdAsString().equals(login_mapping)){
                    cs.addLoginMapping(login_mapping);
                }
            }
            // All other lines contain the same content except the mapping so we concentrate on the mapping here
            while (rs.next()) {
                String login_mapping = rs.getString(10);       
                // DO NOT RETURN THE CONTEXT ID AS A MAPPING!!
                // THIS CAN CAUSE ERRORS IF CHANGING LOGINMAPPINGS AFTERWARDS!
                // SEE #11094 FOR DETAILS!
                if(!ctx.getIdAsString().equals(login_mapping)){
                    cs.addLoginMapping(login_mapping);
                }                
            }

            // ######################

            rs.close();
            prep.close();

            prep = oxdb_read.prepareStatement("SELECT filestore_usage.used FROM filestore_usage WHERE filestore_usage.cid = ?");
            prep.setInt(1, context_id);
            rs = prep.executeQuery();

            long quota_used = 0;
            while (rs.next()) {
                quota_used = rs.getLong(1);
            }
            rs.close();
            prep.close();
            quota_used /= Math.pow(2, 20);
            // set used quota in context setup
            cs.setUsedQuota(quota_used);

            cs.setAverage_size(average_size);

            // context id
            cs.setId(context_id);
            return cs;
        } finally {
            closePreparedStatement(prep);
            try {
                if (oxdb_read != null) {
                    cache.pushConnectionForContext(context_id, oxdb_read);
                }
            } catch (final PoolException exp) {
                log.error("Pool Error pushing ox read connection to pool!",exp);
            }
        }
    }

    public final void createStandardGroupForContext(final int context_id, final Connection ox_write_con, final String display_name, final int group_id, final int gid_number) throws SQLException {
        // TODO: this must be defined somewhere else
        final int NOGROUP = 65534;
        PreparedStatement group_stmt = ox_write_con.prepareStatement("INSERT INTO groups (cid, id, identifier, displayname,lastModified,gidNumber) VALUES (?,?,'users',?,?,?);");
        group_stmt.setInt(1, context_id);
        group_stmt.setInt(2, group_id);
        group_stmt.setString(3, display_name);
        group_stmt.setLong(4, System.currentTimeMillis());
        if (Integer.parseInt(prop.getGroupProp(AdminProperties.Group.GID_NUMBER_START, "-1")) > 0) {
            group_stmt.setInt(5, gid_number);
        } else {
            group_stmt.setInt(5, NOGROUP);
        }
        group_stmt.executeUpdate();
        group_stmt.close();
    }

    /**
     * @param ctx
     * @param admin_user
     * @param con writable context database connection.
     * @param internal_user_id
     * @param contact_id
     * @param uid_number
     * @param access
     * @throws StorageException
     * @throws InvalidDataException
     */
    public final void createAdminForContext(final Context ctx, final User admin_user, final Connection con, final int internal_user_id, final int contact_id, final int uid_number, final UserModuleAccess access) throws StorageException, InvalidDataException {
        OXUserStorageInterface oxs = OXUserStorageInterface.getInstance();
        OXToolStorageInterface tool = OXToolStorageInterface.getInstance();
        tool.primaryMailExists(con, ctx, admin_user.getPrimaryEmail());
        admin_user.setDefaultSenderAddress(admin_user.getPrimaryEmail());
        oxs.create(ctx, admin_user, access, con, internal_user_id, contact_id, uid_number);
    }

    public final void deleteContextFromConfigDB(final Connection configdb_write_con, final int context_id) throws SQLException {
        // find out what db_schema context belongs to
        PreparedStatement stmt3 = null;
        PreparedStatement stmt2 = null;
        PreparedStatement stmt = null;
        try {
            boolean cs2db_broken = false;
            stmt2 = configdb_write_con.prepareStatement("SELECT db_schema,write_db_pool_id FROM context_server2db_pool WHERE cid = ?");
            stmt2.setInt(1, context_id);
            stmt2.executeQuery();
            ResultSet rs = stmt2.getResultSet();
            String db_schema = null;
            int pool_id = -1;
            if (!rs.next()) {
                // throw new OXContextException("Unable to determine db_schema
                // of context " + context_id);
                cs2db_broken = true;
                log.error("Unable to determine db_schema of context " + context_id);
            } else {
                db_schema = rs.getString("db_schema");
                pool_id = ((Integer) rs.getInt("write_db_pool_id")).intValue();
            }
            stmt2.close();
            // System.out.println("############# db_schema = " + db_schema);
            if (log.isDebugEnabled()) {
                log.debug("Deleting context_server2dbpool mapping for context " + context_id);
            }
            // delete context from context_server2db_pool
            stmt2 = configdb_write_con.prepareStatement("DELETE FROM context_server2db_pool WHERE cid = ?");
            stmt2.setInt(1, context_id);
            stmt2.executeUpdate();
            stmt2.close();
            // configdb_write_con.commit(); // temp disabled by c utmasta

            // tell pool, that database has been removed
            try {
                com.openexchange.databaseold.Database.reset(context_id);
            } catch (DBPoolingException e) {
                log.error(e.getMessage(), e);
            }

            if (!cs2db_broken) {
                try {
                    // check if any other context uses the same db_schema
                    // if not, delete it
                    stmt2 = configdb_write_con.prepareStatement("SELECT db_schema FROM context_server2db_pool WHERE db_schema = ?");
                    stmt2.setString(1, db_schema);
                    stmt2.executeQuery();
                    rs = stmt2.getResultSet();
    
                    if (!rs.next()) {
                        // get auth data from db_pool to delete schema
                        stmt3 = configdb_write_con.prepareStatement("SELECT url,driver,login,password FROM db_pool WHERE db_pool_id = ?");
                        stmt3.setInt(1, pool_id);
                        stmt3.executeQuery();
                        final ResultSet rs3 = stmt3.getResultSet();
    
                        if (!rs3.next()) {
                            throw new StorageException("Unable to determine authentication data of pool_id " + pool_id);
                        }
                        final Database db = new Database(rs3.getString("login"), rs3.getString("password"), rs3.getString("driver"), rs3.getString("url"), db_schema);
                        if (log.isDebugEnabled()) {
                            log.debug("Deleting database " + db_schema);
                        }
                        oxutilcommon.deleteDatabase(db);
    
                        stmt3.close();
                    }
                    stmt2.close();
                } catch (final Exception e) {
                    log.error("Problem deleting database while doing rollback, cid=" + context_id + ": ", e);
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Deleting login2context entries for context " + context_id);
            }
            stmt = configdb_write_con.prepareStatement("DELETE FROM login2context WHERE cid = ?");
            stmt.setInt(1, context_id);
            stmt.executeUpdate();
            stmt.close();
            if (log.isDebugEnabled()) {
                log.debug("Deleting context entry for context " + context_id);
            }
            stmt = configdb_write_con.prepareStatement("DELETE FROM context WHERE cid = ?");
            stmt.setInt(1, context_id);
            stmt.executeUpdate();
            stmt.close();
    
        } finally {
            closePreparedStatement(stmt);
            closePreparedStatement(stmt2);
            closePreparedStatement(stmt3);
        }
    }

    private void deleteSequenceTables(int contextId, Connection con) throws SQLException {
        log.debug("Deleting sequence entries for context " + contextId);
        PreparedStatement stmt = null;
        try {
            for (String tableName : determineSequenceTables(con)) {
                stmt = con.prepareStatement("DELETE FROM `" + tableName + "` WHERE cid=?");
                stmt.setInt(1, contextId);
                stmt.executeUpdate();
                stmt.close();
            }
        } finally {
            closeSQLStuff(stmt);
        }
    }

    private String[] determineSequenceTables(Connection con) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet result = null;
        List<String> tmp = new ArrayList<String>();
        try {
            stmt = con.prepareStatement("SHOW TABLES LIKE ?");
            stmt.setString(1, "%sequence_%");
            result = stmt.executeQuery();
            while (result.next()) {
                tmp.add(result.getString(1));
            }
        } finally {
            closeSQLStuff(result, stmt);
        }
        return tmp.toArray(new String[tmp.size()]);
    }

    public void fillContextAndServer2DBPool(final Context ctx, final Connection con, final Database db) throws SQLException, StorageException {
        // dbid is the id in db_pool of database engine to use for next context
    
        // if read id -1 (not set by client ) or 0 (there is no read db for this
        // cluster) then read id must be same as write id
        // else the db pool cannot resolve the database
        if (null == db.getRead_id() || 0 == db.getRead_id()) {
            db.setRead_id(db.getId());
        }
    
        // create context entry in configdb
        // quota is in MB, but we store in Byte
        long quota_max_temp = ctx.getMaxQuota();
        if (quota_max_temp != -1) {
            quota_max_temp *= Math.pow(2, 20);
            ctx.setMaxQuota(quota_max_temp);
        }
        fillContextTable(ctx, con);
    
        // insert in the context_server2dbpool table
        fillContextServer2DBPool(ctx, db, con);
    }

    public final void handleCreateContextRollback(final Connection configdb_write_con, final Connection ox_write_con, final int context_id) {
        try {
            if (configdb_write_con != null && !configdb_write_con.getAutoCommit()) {
                configdb_write_con.rollback();
            }
        } catch (final SQLException expd) {
            log.error("Error processing rollback of configdb connection!", expd);
        }
        try {
            // remove all entries from configdb cause rollback might not be
            // enough
            // cause of contextserver2dbpool entries
            if (configdb_write_con != null) {
                deleteContextFromConfigDB(configdb_write_con, context_id);
            }
        } catch (final SQLException ecp) {
            log.error("SQL Error removing/rollback entries from configdb for context " + context_id, ecp);
        }
        try {
            if (ox_write_con != null && !ox_write_con.getAutoCommit()) {
                ox_write_con.rollback();
            }
        } catch (final SQLException ex) {
            log.error("SQL Error processing rollback of ox connection!", ex);
    
        }
        try {
            // delete sequences
            if (ox_write_con != null) {
                deleteSequenceTables(context_id, ox_write_con);
            }
        } catch (final SQLException ep) {
            log.error("SQL Error deleting sequence tables on rollback create context", ep);
        }
    }

    public final void handleContextDeleteRollback(final Connection write_ox_con, final Connection con_write) {
        try {
            if (con_write != null && !con_write.getAutoCommit()) {
                con_write.rollback();
                log.debug("Rollback of configdb write connection ok");
            }
        } catch (final SQLException rexp) {
            log.error("SQL Error", rexp);
        }
        try {
            if (write_ox_con != null && !write_ox_con.getAutoCommit()) {
                write_ox_con.rollback();
                log.debug("Rollback of ox db write connection ok");
            }
        } catch (final SQLException rexp) {
            log.error("Error processing rollback of ox write connection!", rexp);
        }
    }

    public final void initSequenceTables(int contextId, Connection con) throws SQLException {
        PreparedStatement stmt = null;
        try {
            for (String tableName : determineSequenceTables(con)) {
                int startValue = modifyStartValue(tableName);
                stmt = con.prepareStatement("INSERT INTO `" + tableName + "` VALUES (?,?)");
                stmt.setInt(1, contextId);
                stmt.setInt(2, startValue);
                stmt.execute();
                stmt.close();
            }
        } finally {
            closeSQLStuff(stmt);
        }
    }

    final void initReplicationMonitor(Connection con, int contextId) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("INSERT INTO replicationMonitor (cid, transaction) VALUES (?,0)");
            stmt.setInt(1, contextId);
            stmt.execute();
        } finally {
            closeSQLStuff(stmt);
        }
    }

    private int modifyStartValue(final String tableName) {
        int retval = 0;
        if ("sequence_folder".equals(tableName)) {
            // below id 20 is reserved
            retval = 20;
        }
        // check for the uid number feature
        if ("sequence_uid_number".equals(tableName)) {
            int startnum = Integer.parseInt(prop.getUserProp(AdminProperties.User.UID_NUMBER_START, "-1"));
            if (startnum > 0) {
                // we use the uid number feature
                // set the start number in the sequence for uid_numbers 
                retval = startnum;
            }
        }
        // check for the gid number feature
        if ("sequence_gid_number".equals(tableName)){
            int startnum = Integer.parseInt(prop.getGroupProp(AdminProperties.Group.GID_NUMBER_START, "-1"));
            if (startnum > 0) {
                // we use the gid number feature
                // set the start number in the sequence for gid_numbers 
                retval = startnum;
            }
        }
        return retval;
    }

    public final void initVersionTable(final int context_id, final Connection con) throws SQLException, StorageException {
        PreparedStatement ps = null;
    
        try {
            ps = con.prepareStatement("INSERT INTO version (version,locked,gw_compatible,admin_compatible,server) VALUES(?,?,?,?,?);");
            ps.setInt(1, UpdateTaskCollection.getHighestVersion());
            ps.setInt(2, 0);
            ps.setInt(3, 1);
            ps.setInt(4, 1);
            ps.setString(5, prop.getProp(AdminProperties.Prop.SERVER_NAME, "local"));
            ps.executeUpdate();
            ps.close();
        } finally {
            closePreparedStatement(ps);
        }
    }

    private final int getMyServerID(final Connection configdb_write_con) throws SQLException, StorageException {
        PreparedStatement sstmt = null;
        int sid = 0;
        try {

            final String servername = prop.getProp(AdminProperties.Prop.SERVER_NAME, "local");
            sstmt = configdb_write_con.prepareStatement("SELECT server_id FROM server WHERE name = ?");
            sstmt.setString(1, servername);
            final ResultSet rs2 = sstmt.executeQuery();
            if (!rs2.next()) {
                throw new StorageException("No server registered with name=" + servername);
            }
            sid = Integer.parseInt(rs2.getString("server_id"));
            rs2.close();
        } catch (final SQLException sql) {
            log.error("SQL Error", sql);
            throw sql;
        } finally {
            closePreparedStatement(sstmt);
        }
        return sid;
    }

    private final void fillContextServer2DBPool(final Context ctx, final Database db, final Connection configdb_write_con) throws SQLException, StorageException {

        PreparedStatement stmt = null;
        try {
            if (null != db.getScheme() && null != db.getRead_id() && null != db.getId()) {
                int read_id = -1;
                int write_id = -1;
                String db_schema = "openexchange";
                read_id = db.getRead_id();
                write_id = db.getId();
                db_schema = db.getScheme();

                // ok database pools exist in configdb
                final int server_id = getMyServerID(configdb_write_con);
                stmt = configdb_write_con.prepareStatement("INSERT INTO context_server2db_pool (server_id,cid,read_db_pool_id,write_db_pool_id,db_schema)" + " VALUES " + " (?,?,?,?,?)");
                stmt.setInt(1, server_id);
                stmt.setInt(2, ctx.getId());
                stmt.setInt(3, read_id);
                stmt.setInt(4, write_id);
                stmt.setString(5, db_schema);
                stmt.executeUpdate();
                stmt.close();
            }
        } finally {
            closePreparedStatement(stmt);
        }
    }

    private final void fillContextTable(final Context ctx, final Connection configdb_write_con) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = configdb_write_con.prepareStatement("INSERT INTO context (cid,name,enabled,filestore_id,filestore_name,quota_max) VALUES (?,?,?,?,?,?)");
            stmt.setInt(1, ctx.getId().intValue());
            if (ctx.getName() != null && ctx.getName().trim().length() > 0) {
                stmt.setString(2, ctx.getName());
            } else {
                stmt.setString(2, ctx.getIdAsString());
            }
            stmt.setBoolean(3, true);
            stmt.setInt(4, ctx.getFilestoreId().intValue());
            stmt.setString(5, ctx.getFilestore_name());
            stmt.setLong(6, ctx.getMaxQuota().longValue());
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closePreparedStatement(stmt);
        }
    }

    public void fillLogin2ContextTable(final Context ctx, final Connection configdb_write_con) throws SQLException, StorageException {
        final HashSet<String> loginMappings = ctx.getLoginMappings();
        final Integer ctxid = ctx.getId();
        PreparedStatement stmt = null;
        PreparedStatement checkAvailable = null;
        ResultSet found = null;
        try {
            checkAvailable = configdb_write_con.prepareStatement("SELECT 1 FROM login2context WHERE login_info = ?");
            stmt = configdb_write_con.prepareStatement("INSERT INTO login2context (cid,login_info) VALUES (?,?)");
            for (final String mapping : loginMappings) {
                checkAvailable.setString(1, mapping);
                found = checkAvailable.executeQuery();
                boolean mappingTaken = found.next();
                found.close();

                if(mappingTaken) {
                    throw new StorageException("Cannot map '"+mapping+"' to the newly created context. This mapping is already in use.");
                }

                stmt.setInt(1, ctxid);
                stmt.setString(2, mapping);
                stmt.executeUpdate();
            }
        } catch (final SQLException sql) {
            log.error("SQL Error", sql);
            throw sql;
        } finally {
            closeResultSet(found);
            closePreparedStatement(checkAvailable);
            closePreparedStatement(stmt);

        }
    }

    private void closeResultSet(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            log.error(OXContextMySQLStorageCommon.LOG_ERROR_CLOSING_STATEMENT, e);
        }
    }
    
    private void closePreparedStatement(PreparedStatement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (final SQLException e) {
            log.error(OXContextMySQLStorageCommon.LOG_ERROR_CLOSING_STATEMENT, e);
        }
    }


}
