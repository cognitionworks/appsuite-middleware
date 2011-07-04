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

package com.openexchange.tools.update;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.tools.sql.DBUtils.closeSQLStuff;
import java.io.File;
import java.net.URI;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.filestore.FilestoreException;
import com.openexchange.groupware.filestore.FilestoreStorage;
import com.openexchange.tools.file.FileStorage;
import com.openexchange.tools.file.QuotaFileStorage;
import com.openexchange.tools.file.external.OXException;

/**
 * This class contains some tools to ease update of database.
 * 
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class Tools {

    /**
     * Prevent instantiation
     */
    private Tools() {
        super();
    }

    public static final boolean isNullable(final Connection con, final String table, final String column) throws SQLException {
        final DatabaseMetaData meta = con.getMetaData();
        ResultSet result = null;
        boolean retval = false;
        try {
            result = meta.getColumns(null, null, table, column);
            if (result.next()) {
                retval = DatabaseMetaData.typeNullable == result.getInt(NULLABLE);
            } else {
                throw new SQLException("Can't get information for column " + column + " in table " + table + '.');
            }
        } finally {
            closeSQLStuff(result);
        }
        return retval;
    }

    public static final boolean existsPrimaryKey(final Connection con, final String table, final String[] columns) throws SQLException {
        final DatabaseMetaData metaData = con.getMetaData();
        final List<String> foundColumns = new ArrayList<String>();
        ResultSet result = null;
        try {
            result = metaData.getPrimaryKeys(null, null, table);
            while (result.next()) {
                final String columnName = result.getString(4);
                final int columnPos = result.getInt(5);
                while (foundColumns.size() < columnPos) {
                    foundColumns.add(null);
                }
                foundColumns.set(columnPos - 1, columnName);
            }
        } finally {
            closeSQLStuff(result);
        }
        boolean matches = columns.length == foundColumns.size();
        for (int i = 0; matches && i < columns.length; i++) {
            matches = columns[i].equalsIgnoreCase(foundColumns.get(i));
        }
        return matches;
    }

    /**
     * @param con readable database connection.
     * @param table table name that indexes should be tested.
     * @param columns column names that the index must cover.
     * @return the name of an index that matches the given columns or <code>null</code> if no matching index is found.
     * @throws SQLException if some SQL problem occurs.
     * @throws NullPointerException if one of the columns is <code>null</code>.
     */
    public static final String existsIndex(final Connection con, final String table, final String[] columns) throws SQLException {
        final DatabaseMetaData metaData = con.getMetaData();
        final Map<String, ArrayList<String>> indexes = new HashMap<String, ArrayList<String>>();
        ResultSet result = null;
        try {
            result = metaData.getIndexInfo(null, null, table, false, false);
            while (result.next()) {
                final String indexName = result.getString(6);
                final int columnPos = result.getInt(8);
                final String columnName = result.getString(9);
                ArrayList<String> foundColumns = indexes.get(indexName);
                if (null == foundColumns) {
                    foundColumns = new ArrayList<String>();
                    indexes.put(indexName, foundColumns);
                }
                while (foundColumns.size() < columnPos) {
                    foundColumns.add(null);
                }
                foundColumns.set(columnPos - 1, columnName);
            }
        } finally {
            closeSQLStuff(result);
        }
        String foundIndex = null;
        final Iterator<Entry<String, ArrayList<String>>> iter = indexes.entrySet().iterator();
        while (null == foundIndex && iter.hasNext()) {
            final Entry<String, ArrayList<String>> entry = iter.next();
            final ArrayList<String> foundColumns = entry.getValue();
            if (columns.length != foundColumns.size()) {
                continue;
            }
            boolean matches = true;
            for (int i = 0; matches && i < columns.length; i++) {
                matches = columns[i].equalsIgnoreCase(foundColumns.get(i));
            }
            if (matches) {
                foundIndex = entry.getKey();
            }
        }
        return foundIndex;
    }

    public static final String existsForeignKey(final Connection con, final String primaryTable, final String[] primaryColumns, final String foreignTable, final String[] foreignColumns) throws SQLException {
        final DatabaseMetaData metaData = con.getMetaData();
        final Set<ForeignKey> keys = new HashSet<ForeignKey>();
        ResultSet result = null;
        try {
            result = metaData.getImportedKeys(null, null, foreignTable);
            ForeignKey key = null;
            while (result.next()) {
                final String foundPrimaryTable = result.getString("PKTABLE_NAME");
                final String foundForeignTable = result.getString("FKTABLE_NAME");
                final String keyName = result.getString("FK_NAME");
                final ForeignKey tmp = new ForeignKey(keyName, foundPrimaryTable, foundForeignTable);
                if (null == key || !key.isSame(tmp)) {
                    key = tmp;
                    keys.add(key);
                }
                final String primaryColumn = result.getString("PKCOLUMN_NAME");
                final String foreignColumn = result.getString("FKCOLUMN_NAME");
                final int columnPos = result.getInt("KEY_SEQ");
                key.setPrimaryColumn(columnPos - 1, primaryColumn);
                key.setForeignColumn(columnPos - 1, foreignColumn);
            }
        } finally {
            closeSQLStuff(result);
        }
        for (final ForeignKey key : keys) {
            if (key.getPrimaryTable().equalsIgnoreCase(primaryTable) && key.getForeignTable().equalsIgnoreCase(foreignTable) && key.matches(primaryColumns, foreignColumns)) {
                return key.getName();
            }
        }
        return null;
    }

    /**
     * This method drops the primary key on the table. Beware, this method is vulnerable to SQL injection because table and index name can
     * not be set through a {@link PreparedStatement}.
     * @param con writable database connection.
     * @param table table name that primary key should be dropped.
     * @throws SQLExceptionif some SQL problem occurs.
     */
    public static final void dropPrimaryKey(final Connection con, final String table) throws SQLException {
        final String sql = "ALTER TABLE `" + table + "` DROP PRIMARY KEY";
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql);
        } finally {
            closeSQLStuff(null, stmt);
        }
    }

    /**
     * This method drops an index with the given name. Beware, this method is vulnerable to SQL injection because table and index name can
     * not be set through a {@link PreparedStatement}.
     * 
     * @param con writable database connection.
     * @param table table name that index should be dropped.
     * @param index name of the index to drop.
     * @throws SQLException if some SQL problem occurs.
     */
    public static final void dropIndex(final Connection con, final String table, final String index) throws SQLException {
        final String sql = "ALTER TABLE `" + table + "` DROP INDEX `" + index + "`";
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql);
        } finally {
            closeSQLStuff(null, stmt);
        }
    }

    public static final void dropForeignKey(final Connection con, final String table, final String foreignKey) throws SQLException {
        final String sql = "ALTER TABLE `" + table + "` DROP FOREIGN KEY `" + foreignKey + "`";
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql);
        } finally {
            closeSQLStuff(null, stmt);
        }
    }

    /**
     * This method creates a new primary key on a table. Beware, this method is vulnerable to SQL injection because table and column names
     * can not be set through a {@link PreparedStatement}.
     *
     * @param con writable database connection.
     * @param table name of the table that should get a new primary key.
     * @param columns names of the columns the primary key should cover.
     * @param lengths The column lengths; <code>-1</code> for full column
     * @throws SQLException if some SQL problem occurs.
     */
    public static final void createPrimaryKey(final Connection con, final String table, final String[] columns, final int[] lengths) throws SQLException {
        final StringBuilder sql = new StringBuilder("ALTER TABLE `");
        sql.append(table);
        sql.append("` ADD PRIMARY KEY (");
        {
            final String column = columns[0];
            sql.append('`').append(column).append('`');
            final int len = lengths[0];
            if (len > 0) {
                sql.append('(').append(len).append(')');
            }
        }
        for (int i = 1; i < columns.length; i++) {
            final String column = columns[i];
            sql.append(',');
            sql.append('`').append(column).append('`');
            final int len = lengths[i];
            if (len > 0) {
                sql.append('(').append(len).append(')');
            }
        }
        sql.append(')');
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql.toString());
        } finally {
            closeSQLStuff(null, stmt);
        }
    }

    /**
     * This method creates a new primary key on a table. Beware, this method is vulnerable to SQL injection because table and column names
     * can not be set through a {@link PreparedStatement}.
     *
     * @param con writable database connection.
     * @param table name of the table that should get a new primary key.
     * @param columns names of the columns the primary key should cover.
     * @throws SQLException if some SQL problem occurs.
     */
    public static final void createPrimaryKey(final Connection con, final String table, final String[] columns) throws SQLException {
        final int[] lengths = new int[columns.length];
        Arrays.fill(lengths, -1);
        createPrimaryKey(con, table, columns, lengths);
    }

    /**
     * This method creates a new index on a table. Beware, this method is vulnerable to SQL injection because table and column names can not
     * be set through a {@link PreparedStatement}.
     * 
     * @param con writable database connection.
     * @param table name of the table that should get a new index.
     * @param name name of the index or <code>null</code> to let the database define the name.
     * @param columns names of the columns the index should cover.
     * @param unique if this should be a unique index.
     * @throws SQLException if some SQL problem occurs.
     */
    public static final void createIndex(final Connection con, final String table, final String name, final String[] columns, final boolean unique) throws SQLException {
        final StringBuilder sql = new StringBuilder("ALTER TABLE `");
        sql.append(table);
        sql.append("` ADD ");
        if (unique) {
            sql.append("UNIQUE ");
        }
        sql.append("INDEX ");
        if (null != name) {
            sql.append('`');
            sql.append(name);
            sql.append("` ");
        }
        sql.append("(`");
        for (final String column : columns) {
            sql.append(column);
            sql.append("`,`");
        }
        sql.setLength(sql.length() - 2);
        sql.append(')');
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql.toString());
        } finally {
            closeSQLStuff(null, stmt);
        }
    }

    /**
     * This method creates a new index on a table. Beware, this method is vulnerable to SQL injection because table and column names can not
     * be set through a {@link PreparedStatement}.
     * 
     * @param con writable database connection.
     * @param table name of the table that should get a new index.
     * @param columns names of the columns the index should cover.
     * @throws SQLException if some SQL problem occurs.
     */
    public static final void createIndex(final Connection con, final String table, final String[] columns) throws SQLException {
        createIndex(con, table, null, columns, false);
    }

    public static void createForeignKey(final Connection con, final String primaryTable, final String[] primaryColumns, final String foreignTable, final String[] foreignColumns) throws SQLException {
        createForeignKey(con, null, primaryTable, primaryColumns, foreignTable, foreignColumns);
    }

    public static void createForeignKey(final Connection con, final String name, final String primaryTable, final String[] primaryColumns, final String foreignTable, final String[] foreignColumns) throws SQLException {
        final StringBuilder sql = new StringBuilder("ALTER TABLE `");
        sql.append(primaryTable);
        sql.append("` ADD FOREIGN KEY ");
        if (null != name) {
            sql.append('`');
            sql.append(name);
            sql.append("` ");
        }
        sql.append("(`");
        for (final String column : primaryColumns) {
            sql.append(column);
            sql.append("`,`");
        }
        sql.setLength(sql.length() - 2);
        sql.append(") REFERENCES `");
        sql.append(foreignTable);
        sql.append("`(`");
        for (final String column : foreignColumns) {
            sql.append(column);
            sql.append("`,`");
        }
        sql.setLength(sql.length() - 2);
        sql.append(')');
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql.toString());
        } finally {
            closeSQLStuff(null, stmt);
        }
    }

    /**
     * Checks if denoted table has any primary key set.
     * 
     * @param con The connection
     * @param table The table name
     * @return <code>true</code> if denoted table has any primary key set; otherwise <code>false</code>
     * @throws SQLException If a SQL error occurs
     */
    public static final boolean hasPrimaryKey(final Connection con, final String table) throws SQLException {
        final DatabaseMetaData metaData = con.getMetaData();
        // Get primary keys
        final ResultSet primaryKeys = metaData.getPrimaryKeys(null, null, table);
        try {
            return primaryKeys.next();
        } finally {
            closeSQLStuff(primaryKeys);
        }
    }

    /**
     * Checks if denoted column in given table is of type {@link java.sql.Types#VARCHAR}.
     * 
     * @param con The connection
     * @param table The table name
     * @param column The column name
     * @return <code>true</code> if denoted column in given table is of type {@link java.sql.Types#VARCHAR}; otherwise <code>false</code>
     * @throws SQLException If a SQL error occurs
     */
    public static final boolean isVARCHAR(final Connection con, final String table, final String column) throws SQLException {
        return isType(con, table, column, java.sql.Types.VARCHAR);
    }

    /**
     * Checks if denoted column in given table is of specified type from {@link java.sql.Types}.
     * 
     * @param con The connection
     * @param table The table name
     * @param column The column name
     * @param type The type to check against
     * @return <code>true</code> if denoted column in given table is of specified type; otherwise <code>false</code>
     * @throws SQLException If a SQL error occurs
     */
    public static final boolean isType(final Connection con, final String table, final String column, final int type) throws SQLException {
        return type == getColumnType(con, table, column);
    }

    /**
     * Gets the type of specified column in given table from {@link java.sql.Types}.
     * 
     * @param con The connection
     * @param table The table name
     * @param column The column name
     * @return The type of specified column in given table from {@link java.sql.Types} or <code>-1</code> if column does not exist
     * @throws SQLException If a SQL error occurs
     */
    public static final int getColumnType(final Connection con, final String table, final String column) throws SQLException {
        if (!columnExists(con, table, column)) {
            return -1;
        }
        final DatabaseMetaData metaData = con.getMetaData();
        ResultSet rs = null;
        int type = -1;
        try {
            rs = metaData.getColumns(null, null, table, column);
            while (rs.next()) {
                type = rs.getInt(5);
            }
        } finally {
            closeSQLStuff(rs);
        }
        return type;
    }

    public static final String getColumnTypeName(final Connection con, final String table, final String column) throws SQLException {
        if (!columnExists(con, table, column)) {
            return null;
        }
        final DatabaseMetaData metaData = con.getMetaData();
        ResultSet rs = null;
        String typeName = null;
        try {
            rs = metaData.getColumns(null, null, table, column);
            while (rs.next()) {
                typeName = rs.getString(6);
            }
        } finally {
            closeSQLStuff(rs);
        }
        return typeName;
    }

    public static final boolean tableExists(final Connection con, final String table) throws SQLException {
        final DatabaseMetaData metaData = con.getMetaData();
        ResultSet rs = null;
        boolean retval = false;
        try {
            rs = metaData.getTables(null, null, table, new String[] { TABLE });
            retval = (rs.next() && rs.getString("TABLE_NAME").equalsIgnoreCase(table));
        } finally {
            closeSQLStuff(rs);
        }
        return retval;
    }

    /**
     * Checks if specified column exists.
     * 
     * @param con The connection
     * @param table The table name
     * @param column The column name
     * @return <code>true</code> if specified column exists; otherwise <code>false</code>
     * @throws SQLException If a SQL error occurs
     */
    public static boolean columnExists(final Connection con, final String table, final String column) throws SQLException {
        final DatabaseMetaData metaData = con.getMetaData();
        ResultSet rs = null;
        boolean retval = false;
        try {
            rs = metaData.getColumns(null, null, table, column);
            while (rs.next()) {
                retval = rs.getString(4).equalsIgnoreCase(column);
            }
        } finally {
            closeSQLStuff(rs);
        }
        return retval;
    }

    private static final int NULLABLE = 11;

    private static final String TABLE = "TABLE";

    public static void removeFile(final int cid, final String fileStoreLocation) throws OXException, FilestoreException, ContextException {
        final Context ctx = ContextStorage.getInstance().loadContext(cid);
        final URI fileStorageURI = FilestoreStorage.createURI(ctx);
        final File file = new File(fileStorageURI);
        if (file.exists()) {
            final FileStorage fs = QuotaFileStorage.getInstance(fileStorageURI, ctx);
            fs.deleteFile(fileStoreLocation);
        }
    }

    public static boolean hasSequenceEntry(final String sequenceTable, final Connection con, final int ctxId) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT 1 FROM " + sequenceTable + " WHERE cid=?");
            stmt.setInt(1, ctxId);
            rs = stmt.executeQuery();
            return rs.next();
        } finally {
            closeSQLStuff(rs, stmt);
        }
    }

    public static List<Integer> getContextIDs(final Connection con) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        final List<Integer> contextIds = new LinkedList<Integer>();
        try {
            stmt = con.prepareStatement("SELECT DISTINCT cid FROM user");
            rs = stmt.executeQuery();
            while(rs.next()) {
                contextIds.add(I(rs.getInt(1)));
            }
            return contextIds;
        } finally {
            closeSQLStuff(rs, stmt);
        }
    }

    public static void exec(final Connection con, final String sql, final Object...args) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(sql);
            int i = 1;
            for(final Object arg : args) {
                stmt.setObject(i++, arg);
            }
            stmt.execute();
        } finally {
            closeSQLStuff(stmt);
        }
    }

    private static void addColumns(Connection con, String tableName, Column... cols) throws SQLException {
        StringBuffer sql = new StringBuffer("ALTER TABLE ");
        sql.append(tableName);
        for (Column column : cols) {
            sql.append(" ADD ");
            sql.append(column.getName());
            sql.append(' ');
            sql.append(column.getDefinition());
            sql.append(',');
        }
        if (sql.charAt(sql.length() - 1) == ',') {
            sql.setLength(sql.length() - 1);
        }
        if (sql.length() == 12 + tableName.length()) {
            return;
        }
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql.toString());
        } finally {
            closeSQLStuff(stmt);
        }
    }

    public static void checkAndAddColumns(Connection con, String tableName, Column... cols) throws SQLException {
        List<Column> notExisting = new ArrayList<Column>();
        for (Column col : cols) {
            if (!columnExists(con, tableName, col.getName())) {
                notExisting.add(col);
            }
        }
        if (!notExisting.isEmpty()) {
            addColumns(con, tableName, notExisting.toArray(new Column[notExisting.size()]));
        }
    }

    public static void modifyColumns(Connection con, String tableName, Collection<Column> columns) throws SQLException {
        modifyColumns(con, tableName, columns.toArray(new Column[columns.size()]));
    }

    public static void modifyColumns(Connection con, String tableName, Column... cols) throws SQLException {
        StringBuffer sql = new StringBuffer("ALTER TABLE ");
        sql.append(tableName);
        for (Column column : cols) {
            sql.append(" MODIFY COLUMN ");
            sql.append(column.getName());
            sql.append(' ');
            sql.append(column.getDefinition());
            sql.append(',');
        }
        if (sql.charAt(sql.length() - 1) == ',') {
            sql.setLength(sql.length() - 1);
        }
        if (sql.length() == 12 + tableName.length()) {
            return;
        }
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql.toString());
        } finally {
            closeSQLStuff(stmt);
        }
    }

    public static void checkAndModifyColumns(Connection con, String tableName, Column... cols) throws SQLException {
        List<Column> toDo = new ArrayList<Column>();
        for (Column col : cols) {
            if (!col.getDefinition().contains(getColumnTypeName(con, tableName, col.getName()))) {
                toDo.add(col);
            }
        }
        if (!toDo.isEmpty()) {
            modifyColumns(con, tableName, toDo.toArray(new Column[toDo.size()]));
        }
    }
}
