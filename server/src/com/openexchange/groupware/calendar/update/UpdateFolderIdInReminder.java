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

package com.openexchange.groupware.calendar.update;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import com.openexchange.databaseold.Database;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.calendar.CalendarCollectionService;
import com.openexchange.groupware.calendar.OXCalendarExceptionCodes;
import com.openexchange.groupware.update.Schema;
import com.openexchange.groupware.update.UpdateTask;
import com.openexchange.server.services.ServerServiceRegistry;


/**
 * UpdateFolderIdInReminder
 * @author <a href="mailto:martin.kauss@open-xchange.org">Martin Kauss</a>
 */

public class UpdateFolderIdInReminder implements UpdateTask {
    
    private static final String DELETE_ZERO_REMINDERS = "DELETE from reminder WHERE module = " + Types.APPOINTMENT + " AND folder < 1";
    private static final String FIND_REMINDERS = "SELECT target_id, cid, userid, folder from reminder WHERE module = ?";
    private static final String FIND_CURRENT_USER_FOLDER = "SELECT pfid from prg_dates_members WHERE cid = ? AND member_uid = ? AND object_id = ?";
    private static final String UPDATE_REMINDER = "UPDATE reminder SET folder = ? WHERE target_id = ? AND cid = ? AND userid = ? and module = ?";
    private static final String DELETE_REMINDER = "DELETE FROM reminder WHERE cid = ? AND target_id = ? AND userid = ? AND module = ?";
    private static final String CHECK_MAIN_OBJECT = "SELECT intfield01 FROM prg_dates WHERE cid = ? AND intfield01 = ?";
    private static final String DELETE_ENTRIES_MEMBERS = "DELETE FROM prg_dates_members WHERE cid = ? AND object_id = ?";
    private static final String DELETE_ENTRIES_RIGTHS = "DELETE FROM prg_date_rights WHERE cid = ? AND object_id = ?";
    private static final String FIND_WITHOUT_REFERENCE = "SELECT reminder.object_id, reminder.cid, reminder.userid, reminder.folder FROM reminder LEFT JOIN prg_dates ON reminder.cid = prg_dates.cid AND reminder.target_id = prg_dates.intfield01 where reminder.module = 1 AND intfield01 is NULL";
    
    @Override
    public int addedWithVersion() {
        return 10;
    }
    
    @Override
    public int getPriority() {
        return UpdateTask.UpdateTaskPriority.NORMAL.priority;
    }
    
    @Override
    public void perform(final Schema schema, final int contextId) throws OXException {
        Connection writecon = null;
        Statement stmt = null;
        PreparedStatement pst = null;
        PreparedStatement pst2 = null;
        PreparedStatement pst3 = null;
        PreparedStatement pst4 = null;
        PreparedStatement pst5 = null;
        PreparedStatement pst6 = null;
        ResultSet rs = null;
        ResultSet rs2 = null;
        ResultSet rs3 = null;
        ResultSet rs4 = null;
        CalendarCollectionService collection = ServerServiceRegistry.getInstance().getService(CalendarCollectionService.class);
        
        try {
            writecon = Database.get(contextId, true);
            try {
                stmt = writecon.createStatement();
            } catch (final SQLException ex) {
                throw OXCalendarExceptionCodes.UPDATE_EXCEPTION.create(ex);
            }
            if (stmt != null) {
                try {
                    stmt.executeUpdate(DELETE_ZERO_REMINDERS);
                } catch (final SQLException ex) {
                    throw OXCalendarExceptionCodes.UPDATE_EXCEPTION.create(ex);
                }
            }
            final ArrayList update = new ArrayList(16);
            final ArrayList delete = new ArrayList(16);
            final ArrayList check = new ArrayList(16);
            try {
                pst = writecon.prepareStatement(FIND_REMINDERS, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
                pst2 = writecon.prepareStatement(FIND_CURRENT_USER_FOLDER, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
                pst.setInt(1, Types.APPOINTMENT);
                rs = pst.executeQuery();
                while (rs.next()) {
                    final int oid = rs.getInt(1);
                    final int cid = rs.getInt(2);
                    final int uid = rs.getInt(3);
                    final int fid = rs.getInt(4);
                    pst2.setInt(1, cid);
                    pst2.setInt(2, uid);
                    pst2.setInt(3, oid);
                    final ReminderUpdate ru = new ReminderUpdate();
                    ru.setOID(oid);
                    ru.setCID(cid);
                    ru.setUID(uid);
                    ru.setFID(fid);
                    rs2 = pst2.executeQuery();
                    if (rs2.next()) {
                        final int pfid = rs2.getInt(1);
                        if (pfid != fid) {
                            update.add(ru);
                        } else {
                            check.add(ru);
                        }
                    } else {
                        delete.add(ru);
                    }
                }
            } catch (final SQLException ex) {
                throw OXCalendarExceptionCodes.UPDATE_EXCEPTION.create(ex);
            }
            
            try {
                pst6 = writecon.prepareStatement(FIND_WITHOUT_REFERENCE, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
                rs4 = pst6.executeQuery();
                while (rs4.next()) {
                    final int oid = rs4.getInt(1);
                    final int cid = rs4.getInt(2);
                    final int uid = rs4.getInt(3);
                    final int fid = rs4.getInt(4);                    
                    final ReminderUpdate ru = new ReminderUpdate();
                    ru.setOID(oid);
                    if (rs4.next()) {
                        delete.add(ru);
                    }
                }
            } catch (final SQLException ex) {
                throw OXCalendarExceptionCodes.UPDATE_EXCEPTION.create(ex);
            }            
            
            try {
                if (update.size() > 0) {
                    pst3 = writecon.prepareStatement(UPDATE_REMINDER);
                    for (int a = 0; a < update.size(); a++) {
                        final ReminderUpdate ru = (ReminderUpdate) update.get(a);
                        pst3.setInt(1, ru.getFID());
                        pst3.setInt(2, ru.getOID());
                        pst3.setInt(3, ru.getCID());
                        pst3.setInt(4, ru.getUID());
                        pst3.setInt(5, Types.APPOINTMENT);
                        pst3.addBatch();
                    }
                    pst3.executeBatch();
                }
            } catch (final SQLException ex) {
                throw OXCalendarExceptionCodes.UPDATE_EXCEPTION.create(ex);
            }
            try {
                if (delete.size() > 0)  {
                    pst4 = writecon.prepareStatement(DELETE_REMINDER);
                    for (int a = 0; a < delete.size(); a++) {
                        final ReminderUpdate ru = (ReminderUpdate)delete.get(a);
                        pst4.setInt(1, ru.getCID());
                        pst4.setInt(2, ru.getOID());
                        pst4.setInt(3, ru.getUID());
                        pst4.setInt(4, Types.APPOINTMENT);
                        pst4.addBatch();
                    }
                    pst4.executeBatch();
                }
            } catch (final SQLException ex) {
                throw OXCalendarExceptionCodes.UPDATE_EXCEPTION.create(ex);
            }
            
            try {
                // SELECT intfield01 FROM prg_dates WHERE cid = ? AND intfield01 = ?
                if (update.size() > 0 || delete.size() > 0 || check.size() > 0) {
                    pst5 = writecon.prepareStatement(CHECK_MAIN_OBJECT, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
                    if (update.size() > 0) {
                        for (int a = 0; a < update.size(); a++) {
                            final ReminderUpdate ru = (ReminderUpdate)update.get(a);
                            pst5.setInt(1, ru.getCID());
                            pst5.setInt(2, ru.getOID());
                            rs3 = pst5.executeQuery();
                            if (!rs3.next()) {
                                final PreparedStatement members = writecon.prepareStatement(DELETE_ENTRIES_MEMBERS);
                                members.setInt(1, ru.getCID());
                                members.setInt(2, ru.getOID());
                                members.executeUpdate();
                                collection.closePreparedStatement(members);
                                final PreparedStatement rights = writecon.prepareStatement(DELETE_ENTRIES_RIGTHS);
                                rights.setInt(1, ru.getCID());
                                rights.setInt(2, ru.getOID());
                                rights.executeUpdate();
                                collection.closePreparedStatement(rights);
                            }
                        }
                    }
                    if (delete.size() > 0) {
                        for (int a = 0; a < delete.size(); a++) {
                            final ReminderUpdate ru = (ReminderUpdate)delete.get(a);
                            pst5.setInt(1, ru.getCID());
                            pst5.setInt(2, ru.getOID());
                            rs3 = pst5.executeQuery();
                            if (!rs3.next()) {
                                final PreparedStatement members = writecon.prepareStatement(DELETE_ENTRIES_MEMBERS);
                                members.setInt(1, ru.getCID());
                                members.setInt(2, ru.getOID());
                                members.executeUpdate();
                                collection.closePreparedStatement(members);
                                final PreparedStatement rights = writecon.prepareStatement(DELETE_ENTRIES_RIGTHS);
                                rights.setInt(1, ru.getCID());
                                rights.setInt(2, ru.getOID());
                                rights.executeUpdate();
                                collection.closePreparedStatement(rights);
                            }
                        }
                    }
                    if (check.size() > 0) {
                        for (int a = 0; a < check.size(); a++) {
                            final ReminderUpdate ru = (ReminderUpdate)check.get(a);
                            pst5.setInt(1, ru.getCID());
                            pst5.setInt(2, ru.getOID());
                            rs3 = pst5.executeQuery();
                            if (!rs3.next()) {
                                final PreparedStatement members = writecon.prepareStatement(DELETE_ENTRIES_MEMBERS);
                                members.setInt(1, ru.getCID());
                                members.setInt(2, ru.getOID());
                                members.executeUpdate();
                                collection.closePreparedStatement(members);
                                final PreparedStatement rights = writecon.prepareStatement(DELETE_ENTRIES_RIGTHS);
                                rights.setInt(1, ru.getCID());
                                rights.setInt(2, ru.getOID());
                                rights.executeUpdate();
                                collection.closePreparedStatement(rights);
                            }
                        }
                    }
                }
            } catch (final SQLException ex) {
                throw OXCalendarExceptionCodes.UPDATE_EXCEPTION.create(ex);
            }
        } finally {
            collection.closeResultSet(rs);
            collection.closeResultSet(rs2);
            collection.closeResultSet(rs3);
            collection.closeResultSet(rs4);
            collection.closePreparedStatement(pst);
            collection.closePreparedStatement(pst2);
            collection.closePreparedStatement(pst3);
            collection.closePreparedStatement(pst4);
            collection.closePreparedStatement(pst5);
            collection.closePreparedStatement(pst6);
            collection.closeStatement(stmt);
            if (writecon != null) {
                Database.back(contextId, true, writecon);
            }
        }
    }
    
    private static class ReminderUpdate {
        private int oid;
        private int uid;
        private int fid;
        private int cid;
        private void setOID(final int oid) {
            this.oid = oid;
        }
        private int getOID() {
            return oid;
        }
        private void setUID(final int uid) {
            this.uid = uid;
        }
        private int getUID() {
            return uid;
        }
        private void setCID(final int cid) {
            this.cid = cid;
        }
        private int getCID() {
            return cid;
        }
        private void setFID(final int fid) {
            this.fid = fid;
        }
        private int getFID() {
            return fid;
        }
    }
    
}
