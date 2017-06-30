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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.chronos.storage.rdb.legacy;

import static com.openexchange.chronos.common.AlarmUtils.filter;
import static com.openexchange.chronos.common.AlarmUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.groupware.tools.mappings.database.DefaultDbMapper.getParameters;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.AlarmAction;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.RelatedTo;
import com.openexchange.chronos.Trigger;
import com.openexchange.chronos.common.AlarmUtils;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.service.EntityResolver;
import com.openexchange.chronos.service.RecurrenceService;
import com.openexchange.chronos.storage.AlarmStorage;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.chronos.storage.rdb.RdbStorage;
import com.openexchange.chronos.storage.rdb.osgi.Services;
import com.openexchange.database.provider.DBProvider;
import com.openexchange.database.provider.DBTransactionPolicy;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.impl.IDGenerator;
import com.openexchange.java.util.TimeZones;

/**
 * {@link CalendarStorage}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class RdbAlarmStorage extends RdbStorage implements AlarmStorage {

    /** The module identifier used in the <code>reminder</code> table */
    private static final int REMINDER_MODULE = Types.APPOINTMENT;

    private final EntityResolver entityResolver;

    /**
     * Initializes a new {@link RdbAlarmStorage}.
     *
     * @param context The context
     * @param entityResolver The entity resolver to use
     * @param dbProvider The database provider to use
     * @param txPolicy The transaction policy
     */
    public RdbAlarmStorage(Context context, EntityResolver entityResolver, DBProvider dbProvider, DBTransactionPolicy txPolicy) {
        super(context, dbProvider, txPolicy);
        this.entityResolver = entityResolver;
    }

    @Override
    public int nextId() throws OXException {
        return 0; // no unique identifiers required
    }

    @Override
    public List<Alarm> loadAlarms(Event event, int userID) throws OXException {
        return loadAlarms(Collections.singletonList(event), userID).get(event.getId());
    }

    @Override
    public Map<Integer, List<Alarm>> loadAlarms(Event event) throws OXException {
        Map<Integer, List<Alarm>> alarmsByUserID = new HashMap<Integer, List<Alarm>>();
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            Map<Integer, ReminderData> remindersByUserID = selectReminders(connection, context.getContextId(), asInt(event.getId()));
            for (Map.Entry<Integer, ReminderData> entry : remindersByUserID.entrySet()) {
                List<Alarm> alarms = optAlarms(event, i(entry.getKey()), entry.getValue());
                if (null != alarms) {
                    alarmsByUserID.put(entry.getKey(), alarms);
                }
            }
            return alarmsByUserID;
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public Map<String, List<Alarm>> loadAlarms(List<Event> events, int userID) throws OXException {
        Map<String, Event> eventsByID = CalendarUtils.getEventsByID(events);
        Map<String, List<Alarm>> alarmsByEventID = new HashMap<String, List<Alarm>>(eventsByID.size());
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            Map<String, ReminderData> remindersByID = selectReminders(connection, context.getContextId(), eventsByID.keySet(), userID);
            for (Map.Entry<String, ReminderData> entry : remindersByID.entrySet()) {
                List<Alarm> alarms = optAlarms(eventsByID.get(entry.getKey()), userID, entry.getValue());
                if (null != alarms) {
                    alarmsByEventID.put(entry.getKey(), alarms);
                }
            }
            return alarmsByEventID;
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public Map<String, Map<Integer, List<Alarm>>> loadAlarms(List<Event> events) throws OXException {
        Map<String, Event> eventsByID = CalendarUtils.getEventsByID(events);
        Map<String, Map<Integer, List<Alarm>>> alarmsByUserByEventID = new HashMap<String, Map<Integer, List<Alarm>>>(eventsByID.size());
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            Map<String, Map<Integer, ReminderData>> remindersByUserByID = selectReminders(connection, context.getContextId(), eventsByID.keySet());
            for (Entry<String, Map<Integer, ReminderData>> entry : remindersByUserByID.entrySet()) {
                String eventID = entry.getKey();
                for (Entry<Integer, ReminderData> reminderEntry : entry.getValue().entrySet()) {
                    Integer userID = reminderEntry.getKey();
                    List<Alarm> alarms = optAlarms(eventsByID.get(eventID), i(userID), reminderEntry.getValue());
                    if (null != alarms) {
                        Map<Integer, List<Alarm>> alarmsByUser = alarmsByUserByEventID.get(eventID);
                        if (null == alarmsByUser) {
                            alarmsByUser = new HashMap<Integer, List<Alarm>>();
                            alarmsByUserByEventID.put(eventID, alarmsByUser);
                        }
                        alarmsByUser.put(userID, alarms);
                    }
                }
            }
            return alarmsByUserByEventID;
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public void insertAlarms(Event event, int userID, List<Alarm> alarms) throws OXException {
        ReminderData reminder = getNextReminder(event, userID, alarms, null);
        if (null == reminder) {
            return;
        }
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            updated += insertReminder(connection, context.getContextId(), event, userID, reminder);
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            release(connection, updated);
        }
    }

    @Override
    public void insertAlarms(Map<String, Map<Integer, List<Alarm>>> alarmsByUserByEventId) throws OXException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAlarms(Event event, int userID, List<Alarm> alarms) throws OXException {
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            ReminderData originalReminder = selectReminder(connection, context.getContextId(), asInt(event.getId()), userID);
            ReminderData updatedReminder = getNextReminder(event, userID, alarms, originalReminder);
            if (null == updatedReminder) {
                updated += deleteReminderMinutes(connection, context.getContextId(), asInt(event.getId()), new int[] { userID });
                updated += deleteReminderTriggers(connection, context.getContextId(), asInt(event.getId()), new int[] { userID });
            } else {
                updated += updateReminderMinutes(connection, context.getContextId(), event, userID, updatedReminder.reminderMinutes);
                updated += updateReminderTrigger(connection, context.getContextId(), event, userID, updatedReminder.nextTriggerTime);
            }
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            release(connection, updated);
        }
    }

    @Override
    public void deleteAlarms(String eventID, int userID) throws OXException {
        deleteAlarms(eventID, new int[] { userID });
    }

    @Override
    public void deleteAlarms(String eventID, int[] userIDs) throws OXException {
        if (null == userIDs || 0 == userIDs.length) {
            return;
        }
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            updated += deleteReminderMinutes(connection, context.getContextId(), asInt(eventID), userIDs);
            updated += deleteReminderTriggers(connection, context.getContextId(), asInt(eventID), userIDs);
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            release(connection, updated);
        }
    }

    @Override
    public void deleteAlarms(String eventID) throws OXException {
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            updated += deleteReminderMinutes(connection, context.getContextId(), asInt(eventID));
            updated += deleteReminderTriggers(connection, context.getContextId(), asInt(eventID));
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            release(connection, updated);
        }
    }

    @Override
    public void deleteAlarms(int[] alarmIds) throws OXException {
        throw new UnsupportedOperationException(); // not supported in legacy storage
    }

    /**
     * Optionally gets the alarms representing the stored reminder data. Invalid / malformed alarm data is ignored implicitly.
     *
     * @param event The event the alarms are associated with
     * @param userID The identifier of the user
     * @param reminderData The stored reminder data
     * @return The alarms, or <code>null</code> if there are none or if no alarms couldn't be derived
     */
    private List<Alarm> optAlarms(Event event, int userID, ReminderData reminderData) {
        try {
            return getAlarms(event, userID, reminderData);
        } catch (OXException e) {
            addInvalidDataWaring(event.getId(), EventField.ALARMS, "Ignoring invalid legacy " + reminderData + " for user " + userID, e);
            return null;
        }
    }

    /**
     * Gets the alarms representing the stored reminder data. Invalid / malformed alarm data is ignored implicitly.
     *
     * @param event The event the alarms are associated with
     * @param userID The identifier of the user
     * @param reminderData The stored reminder data
     * @return The alarms, or <code>null</code> if there are none
     */
    private List<Alarm> getAlarms(Event event, int userID, ReminderData reminderData) throws OXException {
        if (null == reminderData) {
            return null;
        }
        /*
         * construct primary alarm from reminder minutes
         */
        Alarm primaryAlarm = new Alarm(new Trigger("-PT" + reminderData.reminderMinutes + 'M'), AlarmAction.DISPLAY);
        //        primaryAlarm.setDescription("Alarm");
        primaryAlarm.setUid(new UUID(context.getContextId(), reminderData.id).toString().toUpperCase());
        /*
         * assume alarm is not yet acknowledged if next trigger still matches the primary alarm's regular trigger time
         */
        if (0 == reminderData.nextTriggerTime) {
            return Collections.singletonList(primaryAlarm);
        }
        Date acknowledgedGuardian = getAcknowledgedGuardian(reminderData);
        Date nextRegularTriggerTime = optNextTriggerTime(event, primaryAlarm, entityResolver.getTimeZone(userID), acknowledgedGuardian);
        if (null == nextRegularTriggerTime) {
            return Collections.singletonList(primaryAlarm);
        }
        if (reminderData.nextTriggerTime == nextRegularTriggerTime.getTime()) {
            /*
             * use primary alarm with acknowledged guardian to prevent premature triggers
             */
            primaryAlarm.setAcknowledged(acknowledgedGuardian);
            return Collections.singletonList(primaryAlarm);
        }
        /*
         * assume primary trigger has been snoozed by marking as acknowledged and adding an accompanying snooze trigger for the trigger time
         */
        primaryAlarm.setAcknowledged(acknowledgedGuardian);
        Alarm snoozeAlarm = new Alarm(new Trigger(new Date(reminderData.nextTriggerTime)), primaryAlarm.getAction());
        //        snoozeAlarm.setDescription(primaryAlarm.getDescription());
        snoozeAlarm.setRelatedTo(new RelatedTo("SNOOZE", primaryAlarm.getUid()));
        List<Alarm> alarms = new ArrayList<Alarm>(2);
        alarms.add(primaryAlarm);
        alarms.add(snoozeAlarm);
        return alarms;
    }

    /**
     * Evaluates the reminder data to insert for a specific event based on the supplied alarm list.
     *
     * @param event The event the alarms are associated with
     * @param userID The identifier of the user
     * @param alarms The alarms to derive the reminder data from
     * @param originalReminder The previously stored reminder data, or <code>null</code> when inserting a new reminder
     * @return The next reminder, or <code>null</code> if there is none
     */
    private ReminderData getNextReminder(Event event, int userID, List<Alarm> alarms, ReminderData originalReminder) throws OXException {
        /*
         * consider ACTION=DISPLAY alarms, only
         */
        List<Alarm> displayAlarms = filter(alarms, AlarmAction.DISPLAY);
        if (null == displayAlarms || 0 == displayAlarms.size()) {
            return null;
        }
        /*
         * distinguish between 'snooze' & regular alarms (via RELTYPE=SNOOZE)
         */
        List<Alarm> regularAlarms = new ArrayList<Alarm>();
        List<Alarm> snoozeAlarms = new ArrayList<Alarm>();
        for (Alarm alarm : displayAlarms) {
            if (AlarmUtils.isSnoozed(alarm, displayAlarms)) {
                snoozeAlarms.add(alarm);
            } else {
                regularAlarms.add(alarm);
            }
        }
        TimeZone timeZone = entityResolver.getTimeZone(userID);
        Alarm snoozeAlarm = chooseNextAlarm(event, originalReminder, snoozeAlarms, timeZone);
        if (null != snoozeAlarm) {
            /*
             * prefer the 'snooze' alarm along with the related 'snoozed' one
             */
            Alarm snoozedAlarm = find(regularAlarms, snoozeAlarm.getRelatedTo().getValue());
            if (null != snoozedAlarm) {
                Date nextTriggerTime = optNextTriggerTime(event, snoozeAlarm, timeZone, snoozedAlarm.getAcknowledged());
                if (null != nextTriggerTime) {
                    int reminderMinutes = getReminderMinutes(snoozedAlarm.getTrigger(), event, timeZone);
                    return new ReminderData(null != originalReminder ? originalReminder.id : 0, reminderMinutes, nextTriggerTime.getTime());
                }
            }
        } else {
            /*
             * regular alarm, only
             */
            Alarm regularAlarm = chooseNextAlarm(event, originalReminder, regularAlarms, timeZone);
            if (null != regularAlarm) {
                Date nextTriggerTime = optNextTriggerTime(event, regularAlarm, timeZone);
                if (null != nextTriggerTime) {
                    int reminderMinutes = getReminderMinutes(regularAlarm.getTrigger(), event, timeZone);
                    return new ReminderData(null != originalReminder ? originalReminder.id : 0, reminderMinutes, nextTriggerTime.getTime());
                }
            }
        }
        return null;
    }

    private static int getReminderMinutes(Trigger trigger, Event event, TimeZone timeZone) throws OXException {
        String duration = AlarmUtils.getTriggerDuration(trigger, event, Services.getService(RecurrenceService.class));
        return null == duration ? 0 : -1 * (int) TimeUnit.MILLISECONDS.toMinutes(AlarmUtils.getTriggerDuration(duration));
    }

    private static ReminderData selectReminder(Connection connection, int contextID, int eventID, int userID) throws SQLException, OXException {
        String sql = new StringBuilder()
            .append("SELECT m.reminder,r.object_id,r.alarm,r.last_modified FROM prg_dates_members AS m ")
            .append("LEFT JOIN reminder AS r ON m.cid=r.cid AND m.member_uid=r.userid AND m.object_id=r.target_id ")
            .append("WHERE m.cid=? AND m.member_uid=? AND m.object_id=?;")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, contextID);
            stmt.setInt(parameterIndex++, userID);
            stmt.setInt(parameterIndex++, eventID);
            try (ResultSet resultSet = logExecuteQuery(stmt)) {
                return resultSet.next() ? readReminder(resultSet) : null;
            }
        }
    }

    private static Map<String, ReminderData> selectReminders(Connection connection, int contextID, Collection<String> eventIDs, int userID) throws SQLException, OXException {
        Map<String, ReminderData> remindersByID = new HashMap<String, ReminderData>(eventIDs.size());
        String sql = new StringBuilder()
            .append("SELECT m.object_id,m.reminder,r.object_id,r.alarm,r.last_modified FROM prg_dates_members AS m ")
            .append("LEFT JOIN reminder AS r ON m.cid=r.cid AND m.member_uid=r.userid AND m.object_id=r.target_id ")
            .append("WHERE m.cid=? AND m.member_uid=? AND m.object_id IN (").append(getParameters(eventIDs.size())).append(");")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, contextID);
            stmt.setInt(parameterIndex++, userID);
            for (String eventID : eventIDs) {
                stmt.setInt(parameterIndex++, asInt(eventID));
            }
            try (ResultSet resultSet = logExecuteQuery(stmt)) {
                while (resultSet.next()) {
                    ReminderData reminder = readReminder(resultSet);
                    if (null != reminder) {
                        String eventID = asString(resultSet.getInt("m.object_id"));
                        remindersByID.put(eventID, reminder);
                    }
                }
            }
        }
        return remindersByID;
    }

    private static Map<Integer, ReminderData> selectReminders(Connection connection, int contextID, int eventID) throws SQLException, OXException {
        Map<Integer, ReminderData> remindersByUserID = new HashMap<Integer, ReminderData>();
        String sql = new StringBuilder()
            .append("SELECT m.member_uid,m.reminder,r.object_id,r.alarm,r.last_modified FROM prg_dates_members AS m ")
            .append("LEFT JOIN reminder AS r ON m.cid=r.cid AND m.member_uid=r.userid AND m.object_id=r.target_id ")
            .append("WHERE m.cid=? AND m.object_id=?;")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, contextID);
            stmt.setInt(parameterIndex++, eventID);
            try (ResultSet resultSet = logExecuteQuery(stmt)) {
                while (resultSet.next()) {
                    ReminderData reminder = readReminder(resultSet);
                    if (null != reminder) {
                        Integer userID = I(resultSet.getInt("m.member_uid"));
                        remindersByUserID.put(userID, reminder);
                    }
                }
            }
        }
        return remindersByUserID;
    }

    private static Map<String, Map<Integer, ReminderData>> selectReminders(Connection connection, int contextID, Collection<String> eventIDs) throws SQLException, OXException {
        Map<String, Map<Integer, ReminderData>> remindersByUserByID = new HashMap<String, Map<Integer, ReminderData>>();
        String sql = new StringBuilder()
            .append("SELECT m.object_id,m.member_uid,m.reminder,r.object_id,r.alarm,r.last_modified FROM prg_dates_members AS m ")
            .append("LEFT JOIN reminder AS r ON m.cid=r.cid AND m.member_uid=r.userid AND m.object_id=r.target_id ")
            .append("WHERE m.cid=? AND m.object_id IN (").append(getParameters(eventIDs.size())).append(");")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, contextID);
            for (String eventID : eventIDs) {
                stmt.setInt(parameterIndex++, asInt(eventID));
            }
            try (ResultSet resultSet = logExecuteQuery(stmt)) {
                while (resultSet.next()) {
                    ReminderData reminder = readReminder(resultSet);
                    if (null != reminder) {
                        String eventID = asString(resultSet.getInt("m.object_id"));
                        Integer userID = I(resultSet.getInt("m.member_uid"));
                        Map<Integer, ReminderData> remindersByUser = remindersByUserByID.get(eventID);
                        if (null == remindersByUser) {
                            remindersByUser = new HashMap<Integer, ReminderData>();
                            remindersByUserByID.put(eventID, remindersByUser);
                        }
                        remindersByUser.put(userID, reminder);
                    }
                }
            }
        }
        return remindersByUserByID;
    }

    private static int insertReminder(Connection connection, int contextID, Event event, int userID, ReminderData reminder) throws SQLException {
        int updated = 0;
        try (PreparedStatement stmt = connection.prepareStatement(
            "INSERT INTO reminder (cid,object_id,last_modified,target_id,module,userid,alarm,recurrence,folder) VALUES (?,?,?,?,?,?,?,?,?);")) {
            stmt.setInt(1, contextID);
            stmt.setInt(2, IDGenerator.getId(contextID, Types.REMINDER, connection));
            stmt.setLong(3, System.currentTimeMillis());
            stmt.setInt(4, asInt(event.getId()));
            stmt.setInt(5, Types.APPOINTMENT);
            stmt.setInt(6, userID);
            stmt.setTimestamp(7, new Timestamp(reminder.nextTriggerTime));
            stmt.setInt(8, isSeriesMaster(event) ? 1 : 0);
            stmt.setInt(9, asInt(event.getFolderId()));
            updated += logExecuteUpdate(stmt);
        }
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE prg_dates_members SET reminder=? WHERE cid=? AND object_id=? AND member_uid=?;")) {
            stmt.setInt(1, reminder.reminderMinutes);
            stmt.setInt(2, contextID);
            stmt.setInt(3, asInt(event.getId()));
            stmt.setInt(4, userID);
            updated += logExecuteUpdate(stmt);
        }
        return updated;
    }

    private static int updateReminderTrigger(Connection connection, int contextID, Event event, int userID, long triggerTime) throws SQLException {
        String sql = "INSERT INTO reminder (cid,object_id,last_modified,target_id,module,userid,alarm,recurrence,folder) VALUES (?,?,?,?,?,?,?,?,?) " +
            "ON DUPLICATE KEY UPDATE last_modified=?,alarm=?,recurrence=?,folder=?;"
        ;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, contextID);
            stmt.setInt(2, IDGenerator.getId(contextID, Types.REMINDER, connection));
            stmt.setLong(3, System.currentTimeMillis());
            stmt.setInt(4, asInt(event.getId()));
            stmt.setInt(5, REMINDER_MODULE);
            stmt.setInt(6, userID);
            stmt.setTimestamp(7, new Timestamp(triggerTime));
            stmt.setInt(8, isSeriesMaster(event) ? 1 : 0);
            stmt.setInt(9, asInt(event.getFolderId()));
            stmt.setLong(10, System.currentTimeMillis());
            stmt.setTimestamp(11, new Timestamp(triggerTime));
            stmt.setInt(12, isSeriesMaster(event) ? 1 : 0);
            stmt.setInt(13, asInt(event.getFolderId()));
            return logExecuteUpdate(stmt);
        }
    }

    private static int updateReminderMinutes(Connection connection, int contextID, Event event, int userID, int reminderMinutes) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE prg_dates_members SET reminder=? WHERE cid=? AND object_id=? AND member_uid=?;")) {
            stmt.setInt(1, reminderMinutes);
            stmt.setInt(2, contextID);
            stmt.setInt(3, asInt(event.getId()));
            stmt.setInt(4, userID);
            return logExecuteUpdate(stmt);
        }
    }

    private static int deleteReminderTriggers(Connection connection, int contextID, int eventID) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM reminder WHERE cid=? AND target_id=? AND module=?;")) {
            stmt.setInt(1, contextID);
            stmt.setInt(2, eventID);
            stmt.setInt(3, REMINDER_MODULE);
            return logExecuteUpdate(stmt);
        }
    }

    private static int deleteReminderTriggers(Connection connection, int contextID, int eventID, int[] userIDs) throws SQLException {
        String sql = new StringBuilder()
            .append("DELETE FROM reminder WHERE cid=? AND module=? AND target_id=? AND userid IN (")
            .append(getParameters(userIDs.length)).append(");")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, contextID);
            stmt.setInt(parameterIndex++, REMINDER_MODULE);
            stmt.setInt(parameterIndex++, eventID);
            for (Integer userID : userIDs) {
                stmt.setInt(parameterIndex++, i(userID));
            }
            return logExecuteUpdate(stmt);
        }
    }

    private static int deleteReminderMinutes(Connection connection, int contextID, int eventID) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE prg_dates_members SET reminder=? WHERE cid=? AND object_id=?;")) {
            stmt.setNull(1, java.sql.Types.INTEGER);
            stmt.setInt(2, contextID);
            stmt.setInt(3, eventID);
            return logExecuteUpdate(stmt);
        }
    }

    private static int deleteReminderMinutes(Connection connection, int contextID, int eventID, int[] userIDs) throws SQLException {
        String sql = new StringBuilder()
            .append("UPDATE prg_dates_members SET reminder=? WHERE cid=? AND object_id=? AND member_uid IN (")
            .append(getParameters(userIDs.length)).append(");")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            stmt.setNull(parameterIndex++, java.sql.Types.INTEGER);
            stmt.setInt(parameterIndex++, contextID);
            stmt.setInt(parameterIndex++, eventID);
            for (Integer userID : userIDs) {
                stmt.setInt(parameterIndex++, i(userID));
            }
            return logExecuteUpdate(stmt);
        }
    }

    /**
     * Reads reminder data from the supplied result set.
     *
     * @param resultSet The result set to read from
     * @return The reminder data, or <code>null</code> if not set
     */
    private static ReminderData readReminder(ResultSet resultSet) throws SQLException, OXException {
        int reminderMinutes = resultSet.getInt("m.reminder");
        if (resultSet.wasNull()) {
            return null;
        }
        int reminderID = resultSet.getInt("r.object_id");
        Timestamp nextTriggerTime = resultSet.getTimestamp("r.alarm");
        return new ReminderData(reminderID, reminderMinutes, null == nextTriggerTime ? 0L : nextTriggerTime.getTime());
    }

    /**
     * Determines the next date-time for a specific alarm trigger associated with an event.
     * <p/>
     * For non-recurring events, this is always the static time of the alarm's trigger.
     * <p/>
     * For event series, the trigger is calculated for the <i>next</i> occurrence after a certain start date, which may be either passed
     * in <code>startDate</code>, or is either the last acknowledged date of the alarm or the current server time.
     *
     * @param event The event the alarm is associated with
     * @param alarm The alarm associated with the event
     * @param timeZone The timezone to consider if the event has <i>floating</i> dates
     * @return The next trigger time, or <code>null</code> if there is none
     */
    private static Date optNextTriggerTime(Event event, Alarm alarm, TimeZone timeZone) {
        return optNextTriggerTime(event, alarm, timeZone, null);
    }

    /**
     * Determines the next date-time for a specific alarm trigger associated with an event.
     * <p/>
     * For non-recurring events, this is always the static time of the alarm's trigger.
     * <p/>
     * For event series, the trigger is calculated for the <i>next</i> occurrence after a certain start date, which may be supplied
     * directly via the <code>startDate</code> argument, or is either the last acknowledged date of the alarm or the current server time.
     *
     * @param event The event the alarm is associated with
     * @param alarm The alarm associated with the event
     * @param timeZone The timezone to consider if the event has <i>floating</i> dates
     * @param startDate The start date marking the lower (inclusive) limit for the actual event occurrence to begin, or <code>null</code>
     *            to select automatically
     * @return The next trigger time, or <code>null</code> if there is none
     */
    private static Date optNextTriggerTime(Event event, Alarm alarm, TimeZone timeZone, Date startDate) {
        if (false == isSeriesMaster(event)) {
            return AlarmUtils.getTriggerTime(alarm.getTrigger(), event, timeZone);
        }
        try {
            return AlarmUtils.getNextTriggerTime(event, alarm, startDate, timeZone, Services.getService(RecurrenceService.class));
        } catch (OXException e) {
            LOG.warn("Error determining next trigger time for alarm", e);
        }
        return null;
    }

    /**
     * Chooses the alarm with the 'nearest' trigger time, that is not yet acknowledged, from a list of multiple alarms.
     *
     * @param event The event the alarms are associated with
     * @param originalReminder The originally stored reminder data in case of updates, or <code>null</code> if not set
     * @param alarms The alarms to choose from
     * @param timeZone The timezone to consider when evaluating the next trigger time of <i>floating</i> events
     * @return The next alarm, or <code>null</code> if there is none
     */
    private static Alarm chooseNextAlarm(Event event, ReminderData originalReminder, List<Alarm> alarms, TimeZone timeZone) {
        if (null == alarms || 0 == alarms.size()) {
            return null;
        }
        Alarm nearestAlarm = null;
        Date nearestTriggerTime = null;
        for (Alarm alarm : alarms) {
            Date nextTriggerTime = optNextTriggerTime(event, alarm, timeZone);
            if (null != nextTriggerTime) {
                if (null != alarm.getAcknowledged() && false == alarm.getAcknowledged().before(nextTriggerTime)) {
                    /*
                     * skip acknowledged alarms, but ignore an auto-inserted acknowledged guardian if unchanged during an update
                     */
                    Date originalAcknowledgedGuardian = getAcknowledgedGuardian(originalReminder);
                    if (null == originalAcknowledgedGuardian || false == originalAcknowledgedGuardian.equals(alarm.getAcknowledged())) {
                        continue;
                    }
                }
                if (null == nearestTriggerTime || nearestTriggerTime.before(nextTriggerTime)) {
                    nearestAlarm = alarm;
                    nearestTriggerTime = nextTriggerTime;
                }
            }
        }
        return nearestAlarm;
    }

    /**
     * Gets the date that is used as <i>acknowledged guardian</i> to prevent premature alarm triggers at clients.
     *
     * @param triggerTime The trigger time of an alarm
     * @return The date of the corresponding acknowledged guardian
     */
    private static Date getAcknowledgedGuardian(ReminderData reminderData) {
        if (null != reminderData && 0 < reminderData.nextTriggerTime) {
            Calendar calendar = CalendarUtils.initCalendar(TimeZones.UTC, reminderData.nextTriggerTime);
            calendar.add(Calendar.MINUTE, -1);
            return calendar.getTime();
        }
        return null;
    }


    private static final class ReminderData {

        int reminderMinutes;
        long nextTriggerTime;
        int id;

        /**
         * Initializes a new {@link ReminderData}.
         *
         * @param id The identifier of the stored reminder
         * @param reminderMinutes The reminder minutes, relative to the targeted event's start date
         * @param nextTriggerTime The next trigger time
         */
        ReminderData(int id, int reminderMinutes, long nextTriggerTime) {
            super();
            this.id = id;
            this.reminderMinutes = reminderMinutes;
            this.nextTriggerTime = nextTriggerTime;
        }

        @Override
        public String toString() {
            return "ReminderData [reminderMinutes=" + reminderMinutes + ", nextTriggerTime=" + new Date(nextTriggerTime) + "]";
        }

    }

}
