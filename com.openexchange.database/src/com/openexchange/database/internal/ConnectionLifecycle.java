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

package com.openexchange.database.internal;

import static com.openexchange.database.internal.DBUtils.closeSQLStuff;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Properties;
import com.openexchange.database.DBPoolingExceptionCodes;
import com.openexchange.exception.OXException;
import com.openexchange.pooling.PoolableLifecycle;
import com.openexchange.pooling.PooledData;

/**
 * Life cycle for database connections.
 */
class ConnectionLifecycle implements PoolableLifecycle<Connection> {

    /**
     * SQL command for checking the connection.
     */
    private static final String TEST_SELECT = "SELECT 1 AS test";

    private final String url;
    private final Properties info;

    /**
     * Time between checks if a connection still works.
     */
    private final long checkTime = ConnectionPool.DEFAULT_CHECK_TIME;

    public ConnectionLifecycle(final String url, final Properties info) {
        this.url = url;
        this.info = info;
    }

    @Override
    public boolean activate(final PooledData<Connection> data) {
        final Connection con = data.getPooled();
        boolean retval;
        Statement stmt = null;
        ResultSet result = null;
        try {
            retval = !con.isClosed();
            if (data.getTimeDiff() > checkTime) {
                stmt = con.createStatement();
                result = stmt.executeQuery(TEST_SELECT);
                if (result.next()) {
                    retval = result.getInt(1) == 1;
                } else {
                    retval = false;
                }
            }
        } catch (final SQLException e) {
            retval = false;
        } finally {
            closeSQLStuff(result, stmt);
        }
        return retval;
    }

    @Override
    public Connection create() throws SQLException {
        return DriverManager.getConnection(url, info);
    }

    public Connection createWithoutTimeout() throws SQLException {
        final Properties withoutTimeout = new Properties();
        withoutTimeout.putAll(info);
        final Iterator<Object> iter = withoutTimeout.keySet().iterator();
        while (iter.hasNext()) {
            final Object test = iter.next();
            if (String.class.isAssignableFrom(test.getClass()) && ((String) test).toLowerCase().endsWith("timeout")) {
                iter.remove();
            }
        }
        return DriverManager.getConnection(url, withoutTimeout);
    }

    @Override
    public boolean deactivate(final PooledData<Connection> data) {
        boolean retval = true;
        try {
            retval = !data.getPooled().isClosed();
        } catch (final SQLException e) {
            retval = false;
        }
        return retval;
    }

    @Override
    public void destroy(final Connection obj) {
        try {
            obj.close();
        } catch (final SQLException e) {
            ConnectionPool.LOG.debug("Problem while closing connection.", e);
        }
    }
    private static void addTrace(final OXException dbe,
        final PooledData<Connection> data) {
        if (null != data.getTrace()) {
            dbe.setStackTrace(data.getTrace());
        }
    }

    @Override
    public boolean validate(final PooledData<Connection> data) {
        final Connection con = data.getPooled();
        boolean retval = true;
        try {
            if (con.isClosed()) {
                ConnectionPool.LOG.error("Found closed connection.");
                retval = false;
            } else if (!con.getAutoCommit()) {
                final OXException dbe = DBPoolingExceptionCodes.NO_AUTOCOMMIT.create();
                addTrace(dbe, data);
                ConnectionPool.LOG.error("", dbe);
                con.rollback();
                con.setAutoCommit(true);
            }
            // Getting number of open statements.
            final Class< ? extends Connection> connectionClass = con.getClass();
            try {
                final Method method = connectionClass.getMethod("getActiveStatementCount");
                final int active = ((Integer) method.invoke(con, new Object[0])).intValue();
                if (active > 0) {
                    final OXException dbe = DBPoolingExceptionCodes.ACTIVE_STATEMENTS.create(I(active));
                    addTrace(dbe, data);
                    ConnectionPool.LOG.error("", dbe);
                    retval = false;
                }
            } catch (final Exception e) {
                ConnectionPool.LOG.error("", e);
            }
            // Write warning if using this connection was longer than 2 seconds.
            if (data.getTimeDiff() > 2000) {
                final OXException dbe = DBPoolingExceptionCodes.TOO_LONG.create(L(data.getTimeDiff()));
                addTrace(dbe, data);
                ConnectionPool.LOG.warn("", dbe);
            }
        } catch (final SQLException e) {
            retval = false;
        }
        return retval;
    }

    @Override
    public String getObjectName() {
        return "Database connection";
    }
}
