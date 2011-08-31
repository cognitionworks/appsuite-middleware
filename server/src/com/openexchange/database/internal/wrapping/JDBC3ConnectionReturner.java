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

package com.openexchange.database.internal.wrapping;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;
import com.openexchange.database.internal.Assignment;
import com.openexchange.database.internal.Pools;
import com.openexchange.database.internal.ReplicationMonitor;

/**
 * {@link JDBC3ConnectionReturner}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public abstract class JDBC3ConnectionReturner implements Connection {

    private final Pools pools;

    private final Assignment assign;

    protected Connection delegate;

    private final boolean noTimeout;

    private final boolean write;

    private final boolean usedAsRead;

    public JDBC3ConnectionReturner(Pools pools, Assignment assign, Connection delegate, boolean noTimeout, boolean write, boolean usedAsRead) {
        super();
        this.pools = pools;
        this.assign = assign;
        this.delegate = delegate;
        this.noTimeout = noTimeout;
        this.write = write;
        this.usedAsRead = usedAsRead;
    }

    @Override
    public void clearWarnings() throws SQLException {
        checkForAlreadyClosed();
        delegate.clearWarnings();
    }

    @Override
    public void close() {
        Connection toReturn = delegate;
        delegate = null;
        ReplicationMonitor.backAndIncrementTransaction(pools, assign, toReturn, noTimeout, write, usedAsRead);
    }

    @Override
    public void commit() throws SQLException {
        checkForAlreadyClosed();
        delegate.commit();
    }

    @Override
    public Statement createStatement() throws SQLException {
        checkForAlreadyClosed();
        return new JDBC4StatementWrapper(delegate.createStatement(), this);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        checkForAlreadyClosed();
        return new JDBC4StatementWrapper(delegate.createStatement(resultSetType, resultSetConcurrency), this);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        checkForAlreadyClosed();
        return new JDBC4StatementWrapper(delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability), this);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        checkForAlreadyClosed();
        return delegate.getAutoCommit();
    }

    @Override
    public String getCatalog() throws SQLException {
        checkForAlreadyClosed();
        return delegate.getCatalog();
    }

    @Override
    public int getHoldability() throws SQLException {
        checkForAlreadyClosed();
        return delegate.getHoldability();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        checkForAlreadyClosed();
        return delegate.getMetaData();
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        checkForAlreadyClosed();
        return delegate.getTransactionIsolation();
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        checkForAlreadyClosed();
        return delegate.getTypeMap();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        checkForAlreadyClosed();
        return delegate.getWarnings();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return delegate == null || delegate.isClosed();
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        checkForAlreadyClosed();
        return delegate.isReadOnly();
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        checkForAlreadyClosed();
        return delegate.nativeSQL(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        checkForAlreadyClosed();
        return delegate.prepareCall(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        checkForAlreadyClosed();
        return delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        checkForAlreadyClosed();
        return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        checkForAlreadyClosed();
        return new JDBC4PreparedStatementWrapper(delegate.prepareStatement(sql), this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        checkForAlreadyClosed();
        return new JDBC4PreparedStatementWrapper(delegate.prepareStatement(sql, autoGeneratedKeys), this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        checkForAlreadyClosed();
        return new JDBC4PreparedStatementWrapper(delegate.prepareStatement(sql, resultSetType, resultSetConcurrency), this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        checkForAlreadyClosed();
        return new JDBC4PreparedStatementWrapper(delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        checkForAlreadyClosed();
        return new JDBC4PreparedStatementWrapper(delegate.prepareStatement(sql, columnIndexes), this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        checkForAlreadyClosed();
        return new JDBC4PreparedStatementWrapper(delegate.prepareStatement(sql, columnNames), this);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        checkForAlreadyClosed();
        delegate.releaseSavepoint(savepoint);
    }

    @Override
    public void rollback() throws SQLException {
        checkForAlreadyClosed();
        delegate.rollback();
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        checkForAlreadyClosed();
        delegate.rollback(savepoint);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        checkForAlreadyClosed();
        delegate.setAutoCommit(autoCommit);
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        checkForAlreadyClosed();
        delegate.setCatalog(catalog);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        checkForAlreadyClosed();
        delegate.setHoldability(holdability);
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        checkForAlreadyClosed();
        delegate.setReadOnly(readOnly);
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        checkForAlreadyClosed();
        return delegate.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        checkForAlreadyClosed();
        return delegate.setSavepoint(name);
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        checkForAlreadyClosed();
        delegate.setTransactionIsolation(level);
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        checkForAlreadyClosed();
        delegate.setTypeMap(map);
    }

    private void checkForAlreadyClosed() throws SQLException {
        if (null == delegate) {
            throw new SQLException("Connection was already closed.");
        }
    }
}
