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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

import static com.openexchange.database.Databases.autocommit;
import static com.openexchange.database.Databases.rollback;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import org.slf4j.Logger;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTaskAdapter;
import com.openexchange.tools.update.Tools;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * {@link CalendarRevokeAddNewPrimaryKeyForConfirmPerOccurrence} - Adapts the keys for those calendar tables that carry confirmation information to new "occurrence" column.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class CalendarRevokeAddNewPrimaryKeyForConfirmPerOccurrence extends UpdateTaskAdapter {

    /**
     * Initializes a new {@link CalendarRevokeAddNewPrimaryKeyForConfirmPerOccurrence}.
     */
    public CalendarRevokeAddNewPrimaryKeyForConfirmPerOccurrence() {
        super();
    }

    @Override
    public String[] getDependencies() {
        return new String[] { CalendarAddNewPrimaryKeyForConfirmPerOccurrence.class.getName() };
    }

    @Override
    public void perform(final PerformParameters params) throws OXException {
        final Connection con = params.getConnection();
        int rollback = 0;
        try {
            if (false == Databases.tablesExist(con, "prg_dates", "del_dates", "dateExternal", "delDateExternal", "prg_dates_members", "del_dates_members", "prg_date_rights", "del_date_rights")) {
                return;
            }
            con.setAutoCommit(false);
            rollback = 1;

            final Logger logger = org.slf4j.LoggerFactory.getLogger(CalendarRevokeAddNewPrimaryKeyForConfirmPerOccurrence.class);

            {
                final TIntObjectMap<TIntSet> cid2ids = determineAffectedAppointments("del_dates_members", "delDateExternal", con);

                if (null != cid2ids) {
                    cid2ids.forEachEntry(new TIntObjectProcedure<TIntSet>() {

                        @Override
                        public boolean execute(final int contextId, final TIntSet ids) {

                            final String str;
                            {
                                final StringBuilder sqlIn = new StringBuilder(ids.size() << 4);
                                sqlIn.append('(');
                                ids.forEach(new TIntProcedure() {

                                    @Override
                                    public boolean execute(final int objectId) {
                                        sqlIn.append(objectId).append(',');
                                        return true;
                                    }
                                });
                                sqlIn.deleteCharAt(sqlIn.length() - 1);
                                sqlIn.append(')');
                                str = sqlIn.toString();
                            }


                            {
                                Statement stmt = null;
                                try {
                                    stmt = con.createStatement();
                                    stmt.executeUpdate("DELETE FROM delDateExternal WHERE cid = " + contextId + " AND objectId IN " + str);
                                } catch (Exception e) {
                                    logger.error("Couldn't delete appointment data.", e);
                                } finally {
                                    Databases.closeSQLStuff(stmt);
                                }
                            }

                            {
                                Statement stmt = null;
                                try {
                                    stmt = con.createStatement();
                                    stmt.executeUpdate("DELETE FROM del_date_rights WHERE cid = " + contextId + " AND object_id IN " + str);
                                } catch (Exception e) {
                                    logger.error("Couldn't delete appointment data.", e);
                                } finally {
                                    Databases.closeSQLStuff(stmt);
                                }
                            }

                            {
                                Statement stmt = null;
                                try {
                                    stmt = con.createStatement();
                                    stmt.executeUpdate("DELETE FROM del_dates_members WHERE cid = " + contextId + " AND object_id IN " + str);
                                } catch (Exception e) {
                                    logger.error("Couldn't delete appointment data.", e);
                                } finally {
                                    Databases.closeSQLStuff(stmt);
                                }
                            }

                            {
                                Statement stmt = null;
                                try {
                                    stmt = con.createStatement();
                                    stmt.executeUpdate("DELETE FROM del_dates WHERE cid = " + contextId + " AND intfield01 IN " + str);
                                } catch (Exception e) {
                                    logger.error("Couldn't delete appointment data.", e);
                                } finally {
                                    Databases.closeSQLStuff(stmt);
                                }
                            }

                            return true;
                        }
                    });
                }
            }

            {
                final TIntObjectMap<TIntSet> cid2ids = determineAffectedAppointments("prg_dates_members", "dateExternal", con);

                if (null != cid2ids) {
                    cid2ids.forEachEntry(new TIntObjectProcedure<TIntSet>() {

                        @Override
                        public boolean execute(final int contextId, final TIntSet ids) {

                            final String str;
                            {
                                final StringBuilder sqlIn = new StringBuilder(ids.size() << 4);
                                sqlIn.append('(');
                                ids.forEach(new TIntProcedure() {

                                    @Override
                                    public boolean execute(final int objectId) {
                                        sqlIn.append(objectId).append(',');
                                        return true;
                                    }
                                });
                                sqlIn.deleteCharAt(sqlIn.length() - 1);
                                sqlIn.append(')');
                                str = sqlIn.toString();
                            }

                            {
                                Statement stmt = null;
                                try {
                                    stmt = con.createStatement();
                                    stmt.executeUpdate("DELETE FROM dateExternal WHERE cid = " + contextId + " AND objectId IN " + str);
                                } catch (Exception e) {
                                    logger.error("Couldn't delete appointment data.", e);
                                } finally {
                                    Databases.closeSQLStuff(stmt);
                                }
                            }

                            {
                                Statement stmt = null;
                                try {
                                    stmt = con.createStatement();
                                    stmt.executeUpdate("DELETE FROM prg_date_rights WHERE cid = " + contextId + " AND object_id IN " + str);
                                } catch (Exception e) {
                                    logger.error("Couldn't delete appointment data.", e);
                                } finally {
                                    Databases.closeSQLStuff(stmt);
                                }
                            }

                            {
                                Statement stmt = null;
                                try {
                                    stmt = con.createStatement();
                                    stmt.executeUpdate("DELETE FROM prg_dates_members WHERE cid = " + contextId + " AND object_id IN " + str);
                                } catch (Exception e) {
                                    logger.error("Couldn't delete appointment data.", e);
                                } finally {
                                    Databases.closeSQLStuff(stmt);
                                }
                            }

                            {
                                Statement stmt = null;
                                try {
                                    stmt = con.createStatement();
                                    stmt.executeUpdate("DELETE FROM prg_dates WHERE cid = " + contextId + " AND intfield01 IN " + str);
                                } catch (Exception e) {
                                    logger.error("Couldn't delete appointment data.", e);
                                } finally {
                                    Databases.closeSQLStuff(stmt);
                                }
                            }

                            return true;
                        }
                    });
                }
            }

            {
                final String[] tables = new String[] { "prg_dates_members", "del_dates_members" };

                // Drop & re-create unique key
                {
                    final String[] oldCols = new String[] {"cid","member_uid","object_id", "occurrence"};
                    final String[] newCols = new String[] {"cid","member_uid","object_id"};
                    checkUniqueKey(oldCols, newCols, tables, con);
                }

                // Drop & re-create primary key
                final String[] columns = new String[] {"cid","object_id","member_uid","pfid"};

                final int[] lengths = new int[4];
                Arrays.fill(lengths, 0);
                checkPrimaryKey(columns, lengths, tables, con);
            }

            {
                // Drop foreign key: dateExternal(cid, objectId) -> prg_dates(cid, intfield01)
                String foreignKey = Tools.existsForeignKey(con, "prg_dates", new String[] {"cid", "intfield01"}, "dateExternal", new String[] {"cid", "objectId"});
                if (null != foreignKey && !foreignKey.equals("")) {
                    Tools.dropForeignKey(con, "dateExternal", foreignKey);
                }

                // Drop foreign key: delDateExternal(cid, objectId) -> del_dates(cid, intfield01)
                foreignKey = Tools.existsForeignKey(con, "del_dates", new String[] {"cid", "intfield01"}, "delDateExternal", new String[] {"cid", "objectId"});
                if (null != foreignKey && !foreignKey.equals("")) {
                    Tools.dropForeignKey(con, "delDateExternal", foreignKey);
                }

                // Drop & re-create primary key
                final String[] tables = new String[] { "dateExternal", "delDateExternal" };
                final String[] columns = new String[] {"cid","objectId","mailAddress"};
                final int[] lengths = new int[3];
                Arrays.fill(lengths, 0);
                lengths[2] = 255;
                checkPrimaryKey(columns, lengths, tables, con);
            }

            con.commit();
            rollback = 2;
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } finally {
            if (rollback > 0) {
                if (rollback==1) {
                    rollback(con);
                }
                autocommit(con);
            }
        }
    }

    private TIntObjectMap<TIntSet> determineAffectedAppointments(final String table, final String table2, final Connection connnection) throws SQLException {
        final TIntObjectMap<TIntSet> cid2ids = new TIntObjectHashMap<TIntSet>(64);
        if (Tools.columnExists(connnection, table, "occurrence")) {
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                // GROUP BY CLAUSE: ensure ONLY_FULL_GROUP_BY compatibility
                stmt = connnection.prepareStatement("SELECT cid, object_id FROM "+table+" GROUP BY cid, object_id, member_uid HAVING count(*) > 1");
                rs = stmt.executeQuery();
                if (rs.next()) {
                    do {
                        final int contextId = rs.getInt(1);
                        TIntSet ids = cid2ids.get(contextId);
                        if (null == ids) {
                            ids = new TIntHashSet();
                            cid2ids.put(contextId, ids);
                        }
                        ids.add(rs.getInt(2));
                    } while (rs.next());
                }
            } finally {
                Databases.closeSQLStuff(rs, stmt);
            }
        }
        if (Tools.columnExists(connnection, table2, "occurrence")) {
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                // GROUP BY CLAUSE: ensure ONLY_FULL_GROUP_BY compatibility
                stmt = connnection.prepareStatement("SELECT cid, objectId FROM "+table2+" GROUP BY cid, objectId, mailAddress HAVING count(*) > 1");
                rs = stmt.executeQuery();
                if (rs.next()) {
                    do {
                        final int contextId = rs.getInt(1);
                        TIntSet ids = cid2ids.get(contextId);
                        if (null == ids) {
                            ids = new TIntHashSet();
                            cid2ids.put(contextId, ids);
                        }
                        ids.add(rs.getInt(2));
                    } while (rs.next());
                }
            } finally {
                Databases.closeSQLStuff(rs, stmt);
            }
        }
        return cid2ids;
    }

    private void checkPrimaryKey(final String[] columns, final int[] lengths, final String[] tables, final Connection connnection) throws SQLException {
        for (final String table : tables) {
            if (!Tools.existsPrimaryKey(connnection, table, columns)) {
                try {
                    Tools.dropPrimaryKey(connnection, table);
                } catch (@SuppressWarnings("unused") final Exception x) {
                    // Ignore failed deletion
                }
                Tools.createPrimaryKey(connnection, table, columns, lengths);
            }
        }
    }

    private void checkUniqueKey(final String[] oldColumns, final String[] newColumns, final String[] tables, final Connection connnection) throws SQLException {
        for (final String table : tables) {
            final String oldIndex = Tools.existsIndex(connnection, table, oldColumns);
            if (null != oldIndex) {
                Tools.dropIndex(connnection, table, oldIndex);
            }

            final String newIndex = Tools.existsIndex(connnection, table, newColumns);
            if (null == newIndex) {
                Tools.createIndex(connnection, table, "member", newColumns, true);
            }
        }
    }

}
