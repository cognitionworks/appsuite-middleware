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

package com.openexchange.groupware.reminder;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.tools.sql.DBUtils.autocommit;
import static com.openexchange.tools.sql.DBUtils.closeSQLStuff;
import static com.openexchange.tools.sql.DBUtils.rollback;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.api.OXConflictException;
import com.openexchange.api.OXMandatoryFieldException;
import com.openexchange.api2.ReminderService;
import com.openexchange.database.DBPoolingException;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.EnumComponent;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.impl.IDGenerator;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.reminder.ReminderException.Code;
import com.openexchange.groupware.reminder.internal.GetArisingReminder;
import com.openexchange.groupware.reminder.internal.RemindAgain;
import com.openexchange.groupware.reminder.internal.SQL;
import com.openexchange.groupware.reminder.internal.TargetRegistry;
import com.openexchange.server.impl.DBPool;
import com.openexchange.session.Session;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIteratorException;
import com.openexchange.tools.sql.DBUtils;

/**
 * ReminderHandler
 *
 * @author <a href="mailto:sebastian.kauss@open-xchange.org">Sebastian Kauss</a>
 */
public class ReminderHandler implements ReminderService {

    /**
     * Logger.
     */
    private static final Log LOG = com.openexchange.exception.Log.valueOf(LogFactory.getLog(ReminderHandler.class));

    final Context context;

    public ReminderHandler(final Context context) {
        super();
        this.context = context;
    }

    public int insertReminder( final ReminderObject reminderObj) throws OXException {
        Connection writeCon = null;

        try {
            writeCon = DBPool.pickupWriteable(context);
            writeCon.setAutoCommit(false);
            final int objectId = insertReminder(reminderObj, writeCon);
            writeCon.commit();
            return objectId;
        } catch (final SQLException exc) {
            DBUtils.rollback(writeCon);
            throw new ReminderException(Code.INSERT_EXCEPTION, exc);
        } catch (final DBPoolingException exc) {
            throw new OXException(exc);
        } finally {
            if (writeCon != null) {
                try {
                    writeCon.setAutoCommit(true);
                } catch (final SQLException exc) {
                    LOG.warn("cannot set autocommit to true on connection", exc);
                }
            }

            DBPool.closeWriterSilent(context,writeCon);
        }
    }

    public int insertReminder( final ReminderObject reminderObj, final Connection writeCon) throws OXException {
        if (reminderObj.getUser() == 0) {
            throw new ReminderException(Code.MANDATORY_FIELD_USER, "missing user id");
        }

        if (0 == reminderObj.getTargetId()) {
            throw new ReminderException(Code.MANDATORY_FIELD_TARGET_ID, "missing target id");
        }

        if (reminderObj.getDate() == null) {
            throw new ReminderException(Code.MANDATORY_FIELD_ALARM, "missing alarm");
        }

        PreparedStatement ps = null;

        try {
            int a  = 0;

            final int objectId = IDGenerator.getId(context, Types.REMINDER, writeCon);
            reminderObj.setObjectId(objectId);

            ps = writeCon.prepareStatement(SQL.sqlInsert);
            ps.setInt(++a, reminderObj.getObjectId());
            ps.setLong(++a, context.getContextId());
            ps.setInt(++a, reminderObj.getTargetId());
            ps.setInt(++a, reminderObj.getModule());
            ps.setInt(++a, reminderObj.getUser());
            ps.setTimestamp(++a, new Timestamp(reminderObj.getDate().getTime()));
            ps.setBoolean(++a, reminderObj.isRecurrenceAppointment());
            ps.setLong(++a, System.currentTimeMillis());
            ps.setInt(++a, reminderObj.getFolder());

            ps.executeUpdate();

            return objectId;
        } catch (final SQLException exc) {
            throw new ReminderException(Code.INSERT_EXCEPTION, exc);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (final SQLException exc) {
                    LOG.warn("cannot close prepared statement", exc);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateReminder(final ReminderObject reminder) throws ReminderException {
        final Connection con;
        try {
            con = DBPool.pickupWriteable(context);
        } catch (final DBPoolingException e) {
            throw new ReminderException(e);
        }
        try {
            con.setAutoCommit(false);
            updateReminder(reminder, con);
            con.commit();
        } catch (final SQLException e) {
            rollback(con);
            throw new ReminderException(Code.UPDATE_EXCEPTION, e);
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (final SQLException e) {
                LOG.warn("cannot set autocommit to true on connection", e);
            }
            DBPool.closeWriterSilent(context, con);
        }
    }

    public void updateReminder(final ReminderObject reminder, final Connection con) throws ReminderException {
        isValid(reminder);
        final boolean containsId = (0 != reminder.getObjectId());
        PreparedStatement stmt = null;
        try {
            if (containsId) {
                stmt = con.prepareStatement(SQL.sqlUpdatebyId);
            } else {
                stmt = con.prepareStatement(SQL.sqlUpdate);
            }
            int pos = 1;
            stmt.setTimestamp(pos++, new Timestamp(reminder.getDate().getTime()));
            stmt.setBoolean(pos++, reminder.isRecurrenceAppointment());
            final String description = reminder.getDescription();
            if (description == null) {
                stmt.setNull(pos++, java.sql.Types.VARCHAR);
            } else {
                stmt.setString(pos++, description);
            }
            stmt.setLong(pos++, System.currentTimeMillis());
            stmt.setInt(pos++, reminder.getFolder());
            // Now the condition.
            stmt.setInt(pos++, context.getContextId());
            if (containsId) {
                stmt.setInt(pos++, reminder.getObjectId());
            } else {
                stmt.setString(pos++, String.valueOf(reminder.getTargetId()));
                stmt.setInt(pos++, reminder.getModule());
                stmt.setInt(pos++, reminder.getUser());
            }
            final int updateCount = stmt.executeUpdate();
            if (updateCount > 1) {
                throw new ReminderException(Code.TOO_MANY);
            }
        } catch (final SQLException e) {
            throw new ReminderException(Code.SQL_ERROR, e, e.getMessage());
        } finally {
            closeSQLStuff(stmt);
        }
    }

    public void isValid(final ReminderObject reminder) throws ReminderException {
        if (0 == reminder.getUser()) {
            throw new ReminderException(Code.MANDATORY_FIELD_USER);
        }
        if (0 == reminder.getModule()) {
            throw new ReminderException(Code.MANDATORY_FIELD_MODULE);
        }
        if (0 == reminder.getFolder()) {
            throw new ReminderException(Code.MANDATORY_FIELD_FOLDER);
        }
        if (0 == reminder.getTargetId()) {
            throw new ReminderException(Code.MANDATORY_FIELD_TARGET_ID);
        }
        if (null == reminder.getDate()) {
            throw new ReminderException(Code.MANDATORY_FIELD_ALARM);
        }
    }

    public void deleteReminder(final ReminderObject reminder) throws OXException {
        final int contextId = context.getContextId();
        Connection writeCon = null;
        PreparedStatement ps = null;
        try {
            writeCon = DBPool.pickupWriteable(context);
            int a = 0;
            ps = writeCon.prepareStatement(SQL.DELETE_WITH_ID);
            ps.setInt(++a, contextId);
            ps.setInt(++a, reminder.getObjectId());
            final int deleted = ps.executeUpdate();
            if (deleted == 0) {
                throw new ReminderException(Code.NOT_FOUND, reminder, I(contextId));
            }
            TargetRegistry.getInstance().getService(reminder.getModule()).updateTargetObject(context, writeCon, reminder.getTargetId(), reminder.getUser());
        } catch (final SQLException exc) {
            throw new ReminderException(Code.DELETE_EXCEPTION, exc);
        } catch (final DBPoolingException exc) {
            throw new ReminderException(Code.DELETE_EXCEPTION, exc);
        } catch (final NumberFormatException e) {
            throw new ReminderException(Code.MANDATORY_FIELD_TARGET_ID, "can't parse number.");
        } catch (final AbstractOXException e) {
            throw new ReminderException(e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (final SQLException exc) {
                    LOG.warn("cannot close prepared statement", exc);
                }
            }

            if (writeCon != null) {
                try {
                    writeCon.setAutoCommit(true);
                } catch (final SQLException exc) {
                    LOG.warn("cannot set autocommit to true on connection", exc);
                }
            }

            DBPool.closeWriterSilent(context,writeCon);
        }
    }

    public void deleteReminder(final int targetId, final int userId, final int module) throws OXMandatoryFieldException, OXConflictException, OXException {
        final Connection writeCon;
        try {
            writeCon = DBPool.pickupWriteable(context);
        } catch (final DBPoolingException e) {
            throw new OXException(e);
        }
        try {
            writeCon.setAutoCommit(false);
            deleteReminder(targetId, userId, module, writeCon);
            writeCon.commit();
        } catch (final SQLException exc) {
            DBUtils.rollback(writeCon);
            throw new ReminderException(Code.DELETE_EXCEPTION, exc);
        } finally {
            DBUtils.autocommit(writeCon);
            DBPool.closeWriterSilent(context,writeCon);
        }
    }

    public void deleteReminder(final int targetId, final int userId, final int module, final Connection con) throws OXException {
        final int contextId = context.getContextId();
        if (userId == 0) {
            throw new ReminderException(Code.MANDATORY_FIELD_USER, "missing user id");
        }
        PreparedStatement stmt = null;
        try {
            int pos = 1;
            stmt = con.prepareStatement(SQL.sqlDelete);
            stmt.setInt(pos++, contextId);
            stmt.setString(pos++, String.valueOf(targetId));
            stmt.setInt(pos++, module);
            stmt.setInt(pos++, userId);
            if (0 == stmt.executeUpdate()) {
                throw new ReminderException(Code.NOT_FOUND, I(targetId), I(contextId));
            }
            TargetRegistry.getInstance().getService(module).updateTargetObject(context, con, targetId, userId);
        } catch (final SQLException e) {
            throw new ReminderException(Code.DELETE_EXCEPTION, e);
        } catch (final ReminderException e) {
            throw e;
        } catch (final AbstractOXException e) {
            throw new ReminderException(e);
        } finally {
            closeSQLStuff(stmt);
        }
    }

    public void deleteReminder(final int targetId, final int module) throws OXMandatoryFieldException, OXConflictException, OXException {
        final Connection con;
        try {
            con = DBPool.pickupWriteable(context);
        } catch (final DBPoolingException e) {
            throw new OXException(e);
        }
        try {
            con.setAutoCommit(false);
            deleteReminder(targetId, module, con);
            con.commit();
        } catch (final SQLException e) {
            rollback(con);
            throw new ReminderException(Code.SQL_ERROR, e, e.getMessage());
        } finally {
            autocommit(con);
            DBPool.closeWriterSilent(context, con);
        }
    }

    public void deleteReminder(final int targetId, final int module, final Connection con) throws OXException {
        final int contextId = context.getContextId();
        PreparedStatement stmt = null;
        try {
            int pos = 1;
            stmt = con.prepareStatement(SQL.sqlDeleteReminderOfObject);
            stmt.setInt(pos++, contextId);
            stmt.setString(pos++, String.valueOf(targetId));
            stmt.setInt(pos++, module);
            if (0 == stmt.executeUpdate()) {
                throw new ReminderException(Code.NOT_FOUND, I(targetId), I(contextId));
            }
            TargetRegistry.getInstance().getService(module).updateTargetObject(context, con, targetId);
        } catch (final SQLException e) {
            throw new ReminderException(Code.SQL_ERROR, e, e.getMessage());
        } catch (final ReminderException e) {
            throw e;
        } catch (final AbstractOXException e) {
            throw new ReminderException(e);
        } finally {
            closeSQLStuff(stmt);
        }
    }
    
    public boolean existsReminder(final int targetId, final int userId, final int module, final Connection con) throws OXMandatoryFieldException, OXConflictException, OXException {
        try {
            if (con == null) {
                loadReminder(targetId, userId, module);
            } else {
                loadReminder(targetId, userId, module, con);
            }
            
            return true;
        } catch (final ReminderException exc) {
            if (Code.NOT_FOUND.getDetailNumber() == exc.getDetailNumber()) {
                return false;
            }
            throw exc;
        }
    }

    public boolean existsReminder(final int targetId, final int userId, final int module) throws OXMandatoryFieldException, OXConflictException, OXException {
        return existsReminder(targetId, userId, module, null);
    }

    public ReminderObject loadReminder( final int targetId, final int userId, final int module) throws OXMandatoryFieldException, OXConflictException, OXException {
        return loadReminder(String.valueOf(targetId), userId, module);
    }
    public ReminderObject loadReminder( final String targetId, final int userId, final int module) throws OXMandatoryFieldException, OXConflictException, OXException {
        final Connection readCon;
        try {
            readCon = DBPool.pickup(context);
        } catch (final DBPoolingException e) {
            throw new OXException(e);
        }
        try {
            return loadReminder(targetId, userId, module, readCon);
        } finally {
            DBPool.closeReaderSilent(context, readCon);
        }
    }

    public ReminderObject loadReminder( final int targetId, final int userId, final int module, final Connection readCon) throws OXMandatoryFieldException, OXConflictException, OXException {
        if (readCon == null) {
            return loadReminder(String.valueOf(targetId), userId, module);
        } else {
            return loadReminder(String.valueOf(targetId), userId, module, readCon);
        }        
    }

    public ReminderObject loadReminder(final String targetId, final int userId, final int module, final Connection con) throws OXException {
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            int pos = 1;
            stmt = con.prepareStatement(SQL.sqlLoad);
            stmt.setInt(pos++, context.getContextId());
            stmt.setString(pos++, targetId);
            stmt.setInt(pos++, module);
            stmt.setInt(pos++, userId);
            result = stmt.executeQuery();
            return result2Object(context, result, stmt, false);
        } catch (final SQLException e) {
            throw new ReminderException(Code.LOAD_EXCEPTION, e);
        } finally {
            closeSQLStuff(result, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ReminderObject[] loadReminder(final int[] targetIds,
            final int userId, final int module) throws OXException {
        Connection con = null;
        try {
            con = DBPool.pickup(context);
        } catch (final DBPoolingException e) {
            throw new OXException(e);
        }
        try {
            return loadReminder(targetIds, userId, module, con);
        } finally {
            DBPool.closeReaderSilent(context, con);
        }
    }

    /**
     * This method loads the reminder for several target objects.
     * @param targetIds unique identifier of several target objects.
     * @param userId unique identifier of the user.
     * @param module module type of target objects.
     * @param con readable database connection.
     * @return an array of found reminders.
     * @throws OXException if reading the reminder fails.
     */
    private ReminderObject[] loadReminder(final int[] targetIds, final int userId, final int module, final Connection con) throws OXException {
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = con.prepareStatement(DBUtils.getIN(SQL.sqlLoadMultiple, targetIds.length));
            int pos = 1;
            stmt.setInt(pos++, context.getContextId());
            stmt.setInt(pos++, module);
            stmt.setInt(pos++, userId);
            for (final int targetId : targetIds) {
                stmt.setString(pos++, String.valueOf(targetId));
            }
            result = stmt.executeQuery();
            return result2Object(result);
        } catch (final SQLException e) {
            throw new ReminderException(Code.LOAD_EXCEPTION, e);
        } finally {
            closeSQLStuff(result, stmt);
        }
    }

    /**
     * Reads the rows from the {@link ResultSet} stores the values in reminder
     * objects an returns them as an array.
     * @param result result with rows of reminders.
     * @return an array of reminder objects.
     * @throws SQLException if an error occurs.
     */
    private ReminderObject[] result2Object(final ResultSet result) throws SQLException {
        final Collection<ReminderObject> retval = new LinkedList<ReminderObject>();
        while (result.next()) {
            int pos = 1;
            try {
                final ReminderObject reminder = new ReminderObject();
                reminder.setObjectId(result.getInt(pos++));
                reminder.setTargetId(result.getInt(pos++));
                reminder.setModule(result.getInt(pos++));
                reminder.setUser(result.getInt(pos++));
                reminder.setDate(result.getTimestamp(pos++));
                reminder.setRecurrenceAppointment(result.getBoolean(pos++));
                reminder.setDescription(result.getString(pos++));
                reminder.setFolder(result.getInt(pos++));
                reminder.setLastModified(new Date(result.getLong(pos++)));
                retval.add(reminder);
            } catch (final SQLException e) {
                // Nothing to do here. Missed one reminder.
                LOG.error(new ReminderException(Code.SQL_ERROR, e, e.getMessage()));
            }
        }
        return retval.toArray(new ReminderObject[retval.size()]);
    }

    public ReminderObject loadReminder( final int objectId) throws OXMandatoryFieldException, OXConflictException, OXException {
        final Connection readCon;
        try {
            readCon = DBPool.pickup(context);
        } catch (final DBPoolingException e) {
            throw new OXException(e);
        }
        try {
            return loadReminder(objectId, readCon);
        } finally {
            DBPool.closeReaderSilent(context,readCon);
        }
    }

    public ReminderObject loadReminder( final int objectId, final Connection readCon) throws OXMandatoryFieldException, OXConflictException, OXException {
        final int contextId = context.getContextId();
        PreparedStatement ps = null;
        try {
            int a = 0;

            ps = readCon.prepareStatement(SQL.sqlLoadById);
            ps.setInt(++a, contextId);
            ps.setInt(++a, objectId);

            final ResultSet rs = ps.executeQuery();
            final ReminderObject reminderObj = result2Object(context, rs, ps, true);

            if (reminderObj != null) {
                return reminderObj;
            }
            throw new ReminderException(Code.NOT_FOUND, I(objectId), I(contextId));
        } catch (final SQLException exc) {
            throw new OXException(EnumComponent.REMINDER, Category.CODE_ERROR, -1, "SQL Problem.", exc);
        } finally {
            DBUtils.closeSQLStuff(ps);
        }
    }

    public static ReminderObject result2Object(final Context ctx, final ResultSet result, final PreparedStatement stmt, final boolean closeStatements) throws SQLException, ReminderException {
        try {
            if (result.next()) {
                int pos = 1;
                final ReminderObject reminderObj = new ReminderObject();
                reminderObj.setObjectId(result.getInt(pos++));
                reminderObj.setTargetId(result.getInt(pos++));
                reminderObj.setModule(result.getInt(pos++));
                reminderObj.setUser(result.getInt(pos++));
                reminderObj.setDate(result.getTimestamp(pos++));
                reminderObj.setRecurrenceAppointment(result.getBoolean(pos++));
                reminderObj.setDescription(result.getString(pos++));
                reminderObj.setFolder(result.getInt(pos++));
                reminderObj.setLastModified(new Date(result.getLong(pos++)));
                return reminderObj;
            }
            throw new ReminderException(Code.NOT_FOUND, I(-1), I(ctx.getContextId()));
        } finally {
            if (closeStatements) {
                closeSQLStuff(result, stmt);
            }
        }
    }

    public SearchIterator<ReminderObject> listReminder(final int module, final int targetId) throws OXException {
        final Connection con;
        try {
            con = DBPool.pickup(context);
        } catch (final DBPoolingException e) {
            throw new OXException(e);
        }
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(SQL.sqlListByTargetId);
            int pos = 1;
            ps.setInt(pos++, context.getContextId());
            ps.setInt(pos++, module);
            ps.setString(pos++, String.valueOf(targetId));
            rs = ps.executeQuery();
            return new ReminderSearchIterator(context, ps, rs, con);
        } catch (final SQLException e) {
            DBUtils.closeSQLStuff(rs, ps);
            DBPool.closeReaderSilent(context, con);
            throw new ReminderException(Code.SQL_ERROR, e, e.getMessage());
        } catch (final SearchIteratorException e) {
            DBUtils.closeSQLStuff(rs, ps);
            DBPool.closeReaderSilent(context, con);
            throw new OXException(e);
        }
    }

    public SearchIterator<ReminderObject> getArisingReminder(final Session session, final Context ctx, final User user, final Date end) throws OXException {
        final GetArisingReminder arising = new GetArisingReminder(session, ctx, user, end);
        return arising.loadWithIterator();
    }

    public void remindAgain(final ReminderObject reminder, final Session session, final Context ctx) throws OXException {
        Connection readCon = null;
        try {
            readCon = DBPool.pickupWriteable(context);
            remindAgain(reminder, session, ctx, readCon);
        } catch (final DBPoolingException exc) {
            throw new OXException(exc);
        } finally {
            DBPool.closeWriterSilent(context, readCon);
        }
    }

    public void remindAgain(final ReminderObject reminder, final Session session, final Context ctx, final Connection writeCon) throws OXException {
        final RemindAgain remindAgain = new RemindAgain(reminder, session, ctx, this);
        remindAgain.remindAgain();
        /*
         * Update target
         */
        try {
            TargetRegistry.getInstance().getService(reminder.getModule()).updateTargetObject(context, writeCon, reminder.getTargetId());
        } catch (final AbstractOXException e) {
            throw new ReminderException(e);
        }
    }

    public SearchIterator<ReminderObject> listModifiedReminder(final int userId, final Date lastModified) throws OXException {
        final Connection readCon;
        try {
            readCon = DBPool.pickup(context);
        } catch (DBPoolingException e) {
            throw new OXException(e);
        }

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = readCon.prepareStatement(SQL.sqlModified);
            ps.setInt(1, context.getContextId());
            ps.setInt(2, userId);
            ps.setTimestamp(3, new Timestamp(lastModified.getTime()));

            rs = ps.executeQuery();
            return new ReminderSearchIterator(context, ps, rs, readCon);
        } catch (final SearchIteratorException exc) {
            DBUtils.closeSQLStuff(rs, ps);
            DBPool.closeReaderSilent(context, readCon);
            throw new OXException(exc);
        } catch (final SQLException exc) {
            DBUtils.closeSQLStuff(rs, ps);
            DBPool.closeReaderSilent(context, readCon);
            throw new OXException(EnumComponent.REMINDER, Category.CODE_ERROR, -1, "SQL Problem.", exc);
        }
    }

}
