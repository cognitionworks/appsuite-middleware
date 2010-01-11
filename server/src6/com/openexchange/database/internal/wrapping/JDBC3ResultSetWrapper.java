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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * The method {@link #getStatement()} must be overwritten to return a {@link JDBC3StatementWrapper}.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public abstract class JDBC3ResultSetWrapper implements ResultSet {

    private final ResultSet delegate;

    private final JDBC3StatementWrapper stmt;

    /**
     * Initializes a new {@link JDBC3ResultSetWrapper}.
     * 
     * @param delegate The delegate result set
     * @param stmt The statement wrapper
     */
    public JDBC3ResultSetWrapper(final ResultSet delegate, final JDBC3StatementWrapper stmt) {
        super();
        this.delegate = delegate;
        this.stmt = stmt;
    }

    public boolean absolute(final int row) throws SQLException {
        return delegate.absolute(row);
    }

    public void afterLast() throws SQLException {
        delegate.afterLast();
    }

    public void beforeFirst() throws SQLException {
        delegate.beforeFirst();
    }

    public void cancelRowUpdates() throws SQLException {
        delegate.cancelRowUpdates();
    }

    public void clearWarnings() throws SQLException {
        delegate.clearWarnings();
    }

    public void close() throws SQLException {
        delegate.close();
    }

    public void deleteRow() throws SQLException {
        delegate.deleteRow();
    }

    public int findColumn(final String columnName) throws SQLException {
        return delegate.findColumn(columnName);
    }

    public boolean first() throws SQLException {
        return delegate.first();
    }

    public Array getArray(final int i) throws SQLException {
        return new JDBC4ArrayWrapper(delegate.getArray(i), this);
    }

    public Array getArray(final String colName) throws SQLException {
        return new JDBC4ArrayWrapper(delegate.getArray(colName), this);
    }

    public InputStream getAsciiStream(final int columnIndex) throws SQLException {
        return delegate.getAsciiStream(columnIndex);
    }

    public InputStream getAsciiStream(final String columnName) throws SQLException {
        return delegate.getAsciiStream(columnName);
    }

    public BigDecimal getBigDecimal(final int columnIndex) throws SQLException {
        return delegate.getBigDecimal(columnIndex);
    }

    public BigDecimal getBigDecimal(final String columnName) throws SQLException {
        return delegate.getBigDecimal(columnName);
    }

    public BigDecimal getBigDecimal(final int columnIndex, final int scale) throws SQLException {
        return delegate.getBigDecimal(columnIndex, scale);
    }

    public BigDecimal getBigDecimal(final String columnName, final int scale) throws SQLException {
        return delegate.getBigDecimal(columnName, scale);
    }

    public InputStream getBinaryStream(final int columnIndex) throws SQLException {
        return delegate.getBinaryStream(columnIndex);
    }

    public InputStream getBinaryStream(final String columnName) throws SQLException {
        return delegate.getBinaryStream(columnName);
    }

    public Blob getBlob(final int i) throws SQLException {
        return delegate.getBlob(i);
    }

    public Blob getBlob(final String colName) throws SQLException {
        return delegate.getBlob(colName);
    }

    public boolean getBoolean(final int columnIndex) throws SQLException {
        return delegate.getBoolean(columnIndex);
    }

    public boolean getBoolean(final String columnName) throws SQLException {
        return delegate.getBoolean(columnName);
    }

    public byte getByte(final int columnIndex) throws SQLException {
        return delegate.getByte(columnIndex);
    }

    public byte getByte(final String columnName) throws SQLException {
        return delegate.getByte(columnName);
    }

    public byte[] getBytes(final int columnIndex) throws SQLException {
        return delegate.getBytes(columnIndex);
    }

    public byte[] getBytes(final String columnName) throws SQLException {
        return delegate.getBytes(columnName);
    }

    public Reader getCharacterStream(final int columnIndex) throws SQLException {
        return delegate.getCharacterStream(columnIndex);
    }

    public Reader getCharacterStream(final String columnName) throws SQLException {
        return delegate.getCharacterStream(columnName);
    }

    public Clob getClob(final int i) throws SQLException {
        return delegate.getClob(i);
    }

    public Clob getClob(final String colName) throws SQLException {
        return delegate.getClob(colName);
    }

    public int getConcurrency() throws SQLException {
        return delegate.getConcurrency();
    }

    public String getCursorName() throws SQLException {
        return delegate.getCursorName();
    }

    public Date getDate(final int columnIndex) throws SQLException {
        return delegate.getDate(columnIndex);
    }

    public Date getDate(final String columnName) throws SQLException {
        return delegate.getDate(columnName);
    }

    public Date getDate(final int columnIndex, final Calendar cal) throws SQLException {
        return delegate.getDate(columnIndex, cal);
    }

    public Date getDate(final String columnName, final Calendar cal) throws SQLException {
        return delegate.getDate(columnName, cal);
    }

    public double getDouble(final int columnIndex) throws SQLException {
        return delegate.getDouble(columnIndex);
    }

    public double getDouble(final String columnName) throws SQLException {
        return delegate.getDouble(columnName);
    }

    public int getFetchDirection() throws SQLException {
        return delegate.getFetchDirection();
    }

    public int getFetchSize() throws SQLException {
        return delegate.getFetchSize();
    }

    public float getFloat(final int columnIndex) throws SQLException {
        return delegate.getFloat(columnIndex);
    }

    public float getFloat(final String columnName) throws SQLException {
        return delegate.getFloat(columnName);
    }

    public int getInt(final int columnIndex) throws SQLException {
        return delegate.getInt(columnIndex);
    }

    public int getInt(final String columnName) throws SQLException {
        return delegate.getInt(columnName);
    }

    public long getLong(final int columnIndex) throws SQLException {
        return delegate.getLong(columnIndex);
    }

    public long getLong(final String columnName) throws SQLException {
        return delegate.getLong(columnName);
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return delegate.getMetaData();
    }

    public Object getObject(final int columnIndex) throws SQLException {
        return delegate.getObject(columnIndex);
    }

    public Object getObject(final String columnName) throws SQLException {
        return delegate.getObject(columnName);
    }

    public Object getObject(final int i, final Map<String, Class<?>> map) throws SQLException {
        return delegate.getObject(i, map);
    }

    public Object getObject(final String colName, final Map<String, Class<?>> map) throws SQLException {
        return delegate.getObject(colName, map);
    }

    public Ref getRef(final int i) throws SQLException {
        return delegate.getRef(i);
    }

    public Ref getRef(final String colName) throws SQLException {
        return delegate.getRef(colName);
    }

    public int getRow() throws SQLException {
        return delegate.getRow();
    }

    public short getShort(final int columnIndex) throws SQLException {
        return delegate.getShort(columnIndex);
    }

    public short getShort(final String columnName) throws SQLException {
        return delegate.getShort(columnName);
    }

    public JDBC3StatementWrapper getStatement() throws SQLException {
        return stmt;
    }

    public String getString(final int columnIndex) throws SQLException {
        return delegate.getString(columnIndex);
    }

    public String getString(final String columnName) throws SQLException {
        return delegate.getString(columnName);
    }

    public Time getTime(final int columnIndex) throws SQLException {
        return delegate.getTime(columnIndex);
    }

    public Time getTime(final String columnName) throws SQLException {
        return delegate.getTime(columnName);
    }

    public Time getTime(final int columnIndex, final Calendar cal) throws SQLException {
        return delegate.getTime(columnIndex, cal);
    }

    public Time getTime(final String columnName, final Calendar cal) throws SQLException {
        return delegate.getTime(columnName, cal);
    }

    public Timestamp getTimestamp(final int columnIndex) throws SQLException {
        return delegate.getTimestamp(columnIndex);
    }

    public Timestamp getTimestamp(final String columnName) throws SQLException {
        return delegate.getTimestamp(columnName);
    }

    public Timestamp getTimestamp(final int columnIndex, final Calendar cal) throws SQLException {
        return delegate.getTimestamp(columnIndex, cal);
    }

    public Timestamp getTimestamp(final String columnName, final Calendar cal) throws SQLException {
        return delegate.getTimestamp(columnName, cal);
    }

    public int getType() throws SQLException {
        return delegate.getType();
    }

    public URL getURL(final int columnIndex) throws SQLException {
        return delegate.getURL(columnIndex);
    }

    public URL getURL(final String columnName) throws SQLException {
        return delegate.getURL(columnName);
    }

    public InputStream getUnicodeStream(final int columnIndex) throws SQLException {
        return delegate.getUnicodeStream(columnIndex);
    }

    public InputStream getUnicodeStream(final String columnName) throws SQLException {
        return delegate.getUnicodeStream(columnName);
    }

    public SQLWarning getWarnings() throws SQLException {
        return delegate.getWarnings();
    }

    public void insertRow() throws SQLException {
        delegate.insertRow();
    }

    public boolean isAfterLast() throws SQLException {
        return delegate.isAfterLast();
    }

    public boolean isBeforeFirst() throws SQLException {
        return delegate.isBeforeFirst();
    }

    public boolean isFirst() throws SQLException {
        return delegate.isFirst();
    }

    public boolean isLast() throws SQLException {
        return delegate.isLast();
    }

    public boolean last() throws SQLException {
        return delegate.last();
    }

    public void moveToCurrentRow() throws SQLException {
        delegate.moveToCurrentRow();
    }

    public void moveToInsertRow() throws SQLException {
        delegate.moveToInsertRow();
    }

    public boolean next() throws SQLException {
        return delegate.next();
    }

    public boolean previous() throws SQLException {
        return delegate.previous();
    }

    public void refreshRow() throws SQLException {
        delegate.refreshRow();
    }

    public boolean relative(final int rows) throws SQLException {
        return delegate.relative(rows);
    }

    public boolean rowDeleted() throws SQLException {
        return delegate.rowDeleted();
    }

    public boolean rowInserted() throws SQLException {
        return delegate.rowInserted();
    }

    public boolean rowUpdated() throws SQLException {
        return delegate.rowUpdated();
    }

    public void setFetchDirection(final int direction) throws SQLException {
        delegate.setFetchDirection(direction);
    }

    public void setFetchSize(final int rows) throws SQLException {
        delegate.setFetchSize(rows);
    }

    public void updateArray(final int columnIndex, final Array x) throws SQLException {
        delegate.updateArray(columnIndex, x);
    }

    public void updateArray(final String columnName, final Array x) throws SQLException {
        delegate.updateArray(columnName, x);
    }

    public void updateAsciiStream(final int columnIndex, final InputStream x, final int length) throws SQLException {
        delegate.updateAsciiStream(columnIndex, x, length);
    }

    public void updateAsciiStream(final String columnName, final InputStream x, final int length) throws SQLException {
        delegate.updateAsciiStream(columnName, x, length);
    }

    public void updateBigDecimal(final int columnIndex, final BigDecimal x) throws SQLException {
        delegate.updateBigDecimal(columnIndex, x);
    }

    public void updateBigDecimal(final String columnName, final BigDecimal x) throws SQLException {
        delegate.updateBigDecimal(columnName, x);
    }

    public void updateBinaryStream(final int columnIndex, final InputStream x, final int length) throws SQLException {
        delegate.updateBinaryStream(columnIndex, x, length);
    }

    public void updateBinaryStream(final String columnName, final InputStream x, final int length) throws SQLException {
        delegate.updateBinaryStream(columnName, x, length);
    }

    public void updateBlob(final int columnIndex, final Blob x) throws SQLException {
        delegate.updateBlob(columnIndex, x);
    }

    public void updateBlob(final String columnName, final Blob x) throws SQLException {
        delegate.updateBlob(columnName, x);
    }

    public void updateBoolean(final int columnIndex, final boolean x) throws SQLException {
        delegate.updateBoolean(columnIndex, x);
    }

    public void updateBoolean(final String columnName, final boolean x) throws SQLException {
        delegate.updateBoolean(columnName, x);
    }

    public void updateByte(final int columnIndex, final byte x) throws SQLException {
        delegate.updateByte(columnIndex, x);
    }

    public void updateByte(final String columnName, final byte x) throws SQLException {
        delegate.updateByte(columnName, x);
    }

    public void updateBytes(final int columnIndex, final byte[] x) throws SQLException {
        delegate.updateBytes(columnIndex, x);
    }

    public void updateBytes(final String columnName, final byte[] x) throws SQLException {
        delegate.updateBytes(columnName, x);
    }

    public void updateCharacterStream(final int columnIndex, final Reader x, final int length) throws SQLException {
        delegate.updateCharacterStream(columnIndex, x, length);
    }

    public void updateCharacterStream(final String columnName, final Reader reader, final int length) throws SQLException {
        delegate.updateCharacterStream(columnName, reader, length);
    }

    public void updateClob(final int columnIndex, final Clob x) throws SQLException {
        delegate.updateClob(columnIndex, x);
    }

    public void updateClob(final String columnName, final Clob x) throws SQLException {
        delegate.updateClob(columnName, x);
    }

    public void updateDate(final int columnIndex, final Date x) throws SQLException {
        delegate.updateDate(columnIndex, x);
    }

    public void updateDate(final String columnName, final Date x) throws SQLException {
        delegate.updateDate(columnName, x);
    }

    public void updateDouble(final int columnIndex, final double x) throws SQLException {
        delegate.updateDouble(columnIndex, x);
    }

    public void updateDouble(final String columnName, final double x) throws SQLException {
        delegate.updateDouble(columnName, x);
    }

    public void updateFloat(final int columnIndex, final float x) throws SQLException {
        delegate.updateFloat(columnIndex, x);
    }

    public void updateFloat(final String columnName, final float x) throws SQLException {
        delegate.updateFloat(columnName, x);
    }

    public void updateInt(final int columnIndex, final int x) throws SQLException {
        delegate.updateInt(columnIndex, x);
    }

    public void updateInt(final String columnName, final int x) throws SQLException {
        delegate.updateInt(columnName, x);
    }

    public void updateLong(final int columnIndex, final long x) throws SQLException {
        delegate.updateLong(columnIndex, x);
    }

    public void updateLong(final String columnName, final long x) throws SQLException {
        delegate.updateLong(columnName, x);
    }

    public void updateNull(final int columnIndex) throws SQLException {
        delegate.updateNull(columnIndex);
    }

    public void updateNull(final String columnName) throws SQLException {
        delegate.updateNull(columnName);
    }

    public void updateObject(final int columnIndex, final Object x) throws SQLException {
        delegate.updateObject(columnIndex, x);
    }

    public void updateObject(final String columnName, final Object x) throws SQLException {
        delegate.updateObject(columnName, x);
    }

    public void updateObject(final int columnIndex, final Object x, final int scale) throws SQLException {
        delegate.updateObject(columnIndex, x, scale);
    }

    public void updateObject(final String columnName, final Object x, final int scale) throws SQLException {
        delegate.updateObject(columnName, x, scale);
    }

    public void updateRef(final int columnIndex, final Ref x) throws SQLException {
        delegate.updateRef(columnIndex, x);
    }

    public void updateRef(final String columnName, final Ref x) throws SQLException {
        delegate.updateRef(columnName, x);
    }

    public void updateRow() throws SQLException {
        delegate.updateRow();
    }

    public void updateShort(final int columnIndex, final short x) throws SQLException {
        delegate.updateShort(columnIndex, x);
    }

    public void updateShort(final String columnName, final short x) throws SQLException {
        delegate.updateShort(columnName, x);
    }

    public void updateString(final int columnIndex, final String x) throws SQLException {
        delegate.updateString(columnIndex, x);
    }

    public void updateString(final String columnName, final String x) throws SQLException {
        delegate.updateString(columnName, x);
    }

    public void updateTime(final int columnIndex, final Time x) throws SQLException {
        delegate.updateTime(columnIndex, x);
    }

    public void updateTime(final String columnName, final Time x) throws SQLException {
        delegate.updateTime(columnName, x);
    }

    public void updateTimestamp(final int columnIndex, final Timestamp x) throws SQLException {
        delegate.updateTimestamp(columnIndex, x);
    }

    public void updateTimestamp(final String columnName, final Timestamp x) throws SQLException {
        delegate.updateTimestamp(columnName, x);
    }

    public boolean wasNull() throws SQLException {
        return delegate.wasNull();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
