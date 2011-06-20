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

package com.openexchange.database.internal;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.tools.sql.DBUtils.closeSQLStuff;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.database.ConfigDatabaseService;
import com.openexchange.database.DBPoolingException;
import com.openexchange.database.DBPoolingExceptionCodes;
import com.openexchange.pooling.PoolingException;

/**
 * Implements the database service to the config database.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public final class ConfigDatabaseServiceImpl implements ConfigDatabaseService {

    private static final Log LOG = LogFactory.getLog(ConfigDatabaseServiceImpl.class);

    private final boolean forceWriteOnly;

    private final Pools pools;

    private final ConfigDatabaseAssignmentService assignmentService;

    ConfigDatabaseServiceImpl(boolean forceWriteOnly, ConfigDatabaseAssignmentService assignmentService, Pools pools) {
        super();
        this.forceWriteOnly = forceWriteOnly;
        this.assignmentService = assignmentService;
        this.pools = pools;
    }

    private Connection get(boolean write) throws DBPoolingException {
        final Assignment assign = assignmentService.getConfigDBAssignment();
        final int poolId;
        if (write || forceWriteOnly) {
            poolId = assign.getWritePoolId();
        } else {
            poolId = assign.getReadPoolId();
        }
        try {
            Connection retval = pools.getPool(poolId).get();
            ReplicationMonitor.incrementFetched(assign, write);
            return retval;
        } catch (final PoolingException e) {
            throw DBPoolingExceptionCodes.NO_CONFIG_DB.create(e);
        }
        // TODO Enable the following if the configuration database gets a table replicationMonitor.
        // return ReplicationMonitor.checkActualAndFallback(pools, assign, false, write || forceWriteOnly);
        
    }

    private void back(Connection con, boolean write) {
        final Assignment assign = assignmentService.getConfigDBAssignment();
        final int poolId;
        if (write || forceWriteOnly) {
            poolId = assign.getWritePoolId();
        } else {
            poolId = assign.getReadPoolId();
        }
        final ConnectionPool pool;
        try {
            pool = pools.getPool(poolId);
        } catch (DBPoolingException e) {
            LOG.error(e.getMessage(), e);
            return;
        }
        try {
            pool.back(con);
        } catch (PoolingException e) {
            final DBPoolingException e1 = DBPoolingExceptionCodes.RETURN_FAILED.create(e, I(poolId));
            LOG.error(e1.getMessage(), e1);
        }
        // TODO enable the following if the configuration database gets a table replicationMonitor.
        // try {
        //     con.close();
        // } catch (SQLException e) {
        //     DBPoolingException e1 = DBPoolingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        //     LOG.error(e1.getMessage(), e1);
        // }
    }

    public Connection getReadOnly() throws DBPoolingException {
        return get(false);
    }

    public Connection getWritable() throws DBPoolingException {
        return get(true);
    }

    public void backReadOnly(Connection con) {
        back(con, false);
    }

    public void backWritable(Connection con) {
        back(con, true);
    }

    public int[] listContexts(int poolId) throws DBPoolingException {
        List<Integer> tmp = new ArrayList<Integer>();
        Connection con = getReadOnly();
        final String getcid = "SELECT cid FROM context_server2db_pool WHERE read_db_pool_id=? OR write_db_pool_id=?";
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = con.prepareStatement(getcid);
            stmt.setInt(1, poolId);
            stmt.setInt(2, poolId);
            result = stmt.executeQuery();
            while (result.next()) {
                tmp.add(I(result.getInt(1)));
            }
        } catch (SQLException e) {
            throw DBPoolingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(result, stmt);
            backReadOnly(con);
        }
        int[] retval = new int[tmp.size()];
        for (int i = 0; i < tmp.size(); i++) {
            retval[i] = tmp.get(i).intValue();
        }
        return retval;
    }

    public int getServerId() throws DBPoolingException {
        return Server.getServerId();
    }
}
