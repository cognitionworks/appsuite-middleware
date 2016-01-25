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
 *     Copyright (C) 2004-2015 Open-Xchange, Inc.
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

package com.openexchange.groupware.alias.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import com.openexchange.databaseold.Database;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.alias.UserAliasStorage;
import com.openexchange.groupware.alias.UserAliasStorageExceptionCodes;
import com.openexchange.java.util.UUIDs;
import com.openexchange.tools.sql.DBUtils;
import com.openexchange.tools.update.Tools;

/**
 * {@link RdbAliasStorage}
 *
 * @author <a href="mailto:lars.hoogestraat@open-xchange.com">Lars Hoogestraat</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.8.0
 */
public class RdbAliasStorage implements UserAliasStorage {

    private static final String CREATE_ALIAS = "INSERT INTO user_alias (cid, user, alias) VALUES(?,?,?)";

    private static final String CREATE_ALIAS_WITH_UUID = "INSERT INTO user_alias (cid, user, alias, uuid) VALUES(?,?,?,?)";

    private static final String READ_ALIASES = "SELECT alias FROM user_alias WHERE cid=? AND user=?";

    private static final String UPDATE_ALIAS = "UPDATE user_alias SET alias=? WHERE cid=? AND user=? AND alias=?";

    private static final String DELETE_ALIAS = "DELETE FROM user_alias WHERE cid=? AND user=? AND alias=?";

    private static final String DELETE_ALL_ALIASES = "DELETE FROM user_alias WHERE cid=? AND user=?";
    
    /**
     * Initialises a new {@link RdbAliasStorage}.
     */
    public RdbAliasStorage() {
        super();
    }

    @Override
    public void invalidateAliases(int contextId, int userId) throws OXException {
        // Nothing
    }

    @Override
    public HashSet<String> getAliases(int contextId, int userId) throws OXException {
        final Connection con = Database.get(contextId, false);
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            int index = 0;
            stmt = con.prepareStatement("SELECT alias FROM user_alias WHERE cid=? AND user=?");
            stmt.setInt(++index, contextId);
            stmt.setInt(++index, userId);
            rs = stmt.executeQuery();
            HashSet<String> aliases = new HashSet<String>(6, 0.9F);
            while (rs.next()) {
                aliases.add(rs.getString(1));
            }
            return aliases;
        } catch (SQLException e) {
            throw UserAliasStorageExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            DBUtils.closeSQLStuff(rs, stmt);
            Database.back(contextId, false, con);
        }
    }

    @Override
    public int getUserId(int contextId, String alias) throws OXException {
        final Connection con = Database.get(contextId, false);
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            int index = 0;
            stmt = con.prepareStatement("SELECT user FROM user_alias WHERE cid=? AND alias LIKE ? COLLATE utf8_bin");
            stmt.setInt(++index, contextId);
            stmt.setString(++index, alias);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return -1;
        } catch (SQLException e) {
            throw UserAliasStorageExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            DBUtils.closeSQLStuff(rs, stmt);
            Database.back(contextId, false, con);
        }
    }

    @Override
    public boolean createAlias(Connection con, int contextId, int userId, String alias) throws OXException {
        boolean useExistingConnection = true;
        if(con == null) {
            con = Database.get(contextId, true);
            useExistingConnection = false;
        }
        PreparedStatement stmt = null;
        try {
            int index = 0;
            stmt = con.prepareStatement(CREATE_ALIAS_WITH_UUID);
            stmt.setInt(++index, contextId);
            stmt.setInt(++index, userId);
            stmt.setString(++index, alias);
            stmt.setBytes(++index, UUIDs.toByteArray(UUID.randomUUID()));
            
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw UserAliasStorageExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            DBUtils.closeSQLStuff(stmt);
            // Only take back newly created database connection to the database pool
            if (!useExistingConnection) {
                Database.back(contextId, false, con);
            }
        }
    }

    @Override
    public boolean updateAlias(Connection con, int contextId, int userId, String oldAlias, String newAlias) throws OXException {
        boolean useExistingConnection = true;
        if(con == null) {
            con = Database.get(contextId, true);
            useExistingConnection = false;
        }
        PreparedStatement stmt = null;
        try {
            int index = 0;
            stmt = con.prepareStatement("UPDATE user_alias SET alias=? WHERE cid=? AND user=? AND alias=?");
            stmt.setString(++index, newAlias);
            stmt.setInt(++index, contextId);
            stmt.setInt(++index, userId);
            stmt.setString(++index, oldAlias);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw UserAliasStorageExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            DBUtils.closeSQLStuff(stmt);
            // Only take back newly created database connection to the database pool
            if(!useExistingConnection) {
                Database.back(contextId, false, con);
            }
        }
    }

    @Override
    public boolean deleteAlias(Connection con, int contextId, int userId, String alias) throws OXException {
        boolean useExistingConnection = true;
        if(con == null) {
            con = Database.get(contextId, true);
            useExistingConnection = false;
        }
        PreparedStatement stmt = null;
        try {
            int index = 0;
            stmt = con.prepareStatement("DELETE FROM user_alias WHERE cid=? AND user=? AND alias=?");
            stmt.setInt(++index, contextId);
            stmt.setInt(++index, userId);
            stmt.setString(++index, alias);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw UserAliasStorageExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            DBUtils.closeSQLStuff(stmt);
            // Only take back newly created database connection to the database pool
            if(!useExistingConnection) {
                Database.back(contextId, false, con);
            }
        }
    }

    @Override
    public boolean deleteAliases(Connection con, int contextId, int userId) throws OXException {
        boolean useExistingConnection = true;
        if(con == null) {
            con = Database.get(contextId, true);
            useExistingConnection = false;
        }
        PreparedStatement stmt = null;
        try {
            int index = 0;
            stmt = con.prepareStatement(DELETE_ALL_ALIASES);
            stmt.setInt(++index, contextId);
            stmt.setInt(++index, userId);
            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw UserAliasStorageExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            DBUtils.closeSQLStuff(stmt);
            // Only take back newly created database connection to the database pool
            if(!useExistingConnection) {
                Database.back(contextId, false, con);
            }
        }
    }

    @Override
    public List<Integer> getUserIdsByAliasDomain(int contextId, String aliasDomain) throws OXException {

        Connection read_ox_con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (read_ox_con == null) {
                read_ox_con = Database.get(contextId, true);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("SELECT us.id FROM user us LEFT JOIN user_alias la ON us.id = la.user AND us.cid = la.cid ");
            sb.append("WHERE us.cid = ? and la.alias LIKE ?");

            String query = sb.toString();
            stmt = read_ox_con.prepareStatement(query);
            stmt.setInt(1, contextId);
            stmt.setString(2, "%" + aliasDomain);

            rs = stmt.executeQuery();
            ArrayList<Integer> retval = new ArrayList<Integer>();
            while (rs.next()) {
                retval.add(rs.getInt(1));
            }
            return retval;
        } catch (SQLException e) {
            throw UserAliasStorageExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw e;
        } finally {
            DBUtils.closeSQLStuff(stmt);
            Database.back(contextId, false, read_ox_con);
        }
    }
}
