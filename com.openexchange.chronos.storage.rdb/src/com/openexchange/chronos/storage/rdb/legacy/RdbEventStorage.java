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

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.I2i;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultRecurrenceData;
import com.openexchange.chronos.compat.Appointment2Event;
import com.openexchange.chronos.compat.Event2Appointment;
import com.openexchange.chronos.compat.PositionAwareRecurrenceId;
import com.openexchange.chronos.service.EntityResolver;
import com.openexchange.chronos.service.RecurrenceData;
import com.openexchange.chronos.service.SearchFilter;
import com.openexchange.chronos.service.SearchOptions;
import com.openexchange.chronos.storage.EventStorage;
import com.openexchange.chronos.storage.rdb.RdbStorage;
import com.openexchange.chronos.storage.rdb.migration.MigrationResult;
import com.openexchange.chronos.storage.rdb.migration.StorageMigration;
import com.openexchange.chronos.storage.rdb.osgi.Services;
import com.openexchange.database.provider.DBProvider;
import com.openexchange.database.provider.DBTransactionPolicy;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.impl.IDGenerator;
import com.openexchange.search.SearchTerm;

/**
 * {@link RdbEventStorage}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class RdbEventStorage extends RdbStorage implements EventStorage {

    private static final EventMapper MAPPER = EventMapper.getInstance();

    private final EntityResolver entityResolver;

    /**
     * Initializes a new {@link RdbEventStorage}.
     *
     * @param context The context
     * @param entityResolver The entity resolver to use
     * @param dbProvider The database provider to use
     * @param txPolicy The transaction policy
     */
    public RdbEventStorage(Context context, EntityResolver entityResolver, DBProvider dbProvider, DBTransactionPolicy txPolicy) {
        super(context, dbProvider, txPolicy);
        this.entityResolver = entityResolver;
    }

    @Override
    public String nextId() throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            if (connection.getAutoCommit()) {
                throw new SQLException("Generating unique identifier is threadsafe if and only if it is executed in a transaction.");
            }
            return Appointment2Event.asString(IDGenerator.getId(context, Types.APPOINTMENT, connection));
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            release(connection, 1);
        }
    }

    @Override
    public long countEvents() throws OXException {
        return countEvents(null);
    }

    @Override
    public long countEvents(SearchTerm<?> searchTerm) throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return countEvents(connection, false, context.getContextId(), searchTerm);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public long countEventTombstones(SearchTerm<?> searchTerm) throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return countEvents(connection, true, context.getContextId(), searchTerm);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public List<Event> searchEvents(SearchTerm<?> searchTerm, SearchOptions sortOptions, EventField[] fields) throws OXException {
        return searchEvents(searchTerm, null, sortOptions, fields);
    }

    @Override
    public List<Event> searchEvents(SearchTerm<?> searchTerm, List<SearchFilter> filters, SearchOptions sortOptions, EventField[] fields) throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return selectEvents(connection, false, context.getContextId(), searchTerm, filters, sortOptions, fields);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public List<Event> searchOverlappingEvents(List<Attendee> attendees, boolean includeTransparent, SearchOptions searchOptions, EventField[] fields) throws OXException {
        Set<Integer> userIDs = new HashSet<Integer>();
        Set<Integer> otherEntityIDs = new HashSet<Integer>();
        for (Attendee attendee : attendees) {
            if (null == attendee.getCuType() || false == CalendarUtils.isInternal(attendee)) {
                continue;
            }
            if (CalendarUserType.INDIVIDUAL.equals(attendee.getCuType())) {
                userIDs.add(I(attendee.getEntity()));
            } else {
                otherEntityIDs.add(I(attendee.getEntity()));
            }
        }
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return selectOverlappingEvents(connection, context.getContextId(), I2i(userIDs), I2i(otherEntityIDs), includeTransparent, searchOptions, fields);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public List<Event> searchEventTombstones(SearchTerm<?> searchTerm, SearchOptions searchOptions, EventField[] fields) throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return selectEvents(connection, true, context.getContextId(), searchTerm, null, searchOptions, fields);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public Event loadEvent(String objectID, EventField[] fields) throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return selectEvent(connection, context.getContextId(), asInt(objectID), fields);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public Event loadException(String seriesID, RecurrenceId recurrenceID, EventField[] fields) throws OXException {
        long recurrenceDatePosition;
        if (PositionAwareRecurrenceId.class.isInstance(recurrenceID)) {
            recurrenceDatePosition = ((PositionAwareRecurrenceId) recurrenceID).getRecurrenceDatePosition().getTime();
        } else {
            recurrenceDatePosition = Event2Appointment.getRecurrenceDatePosition(recurrenceID).getTime();
        }
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return selectException(connection, context.getContextId(), asInt(seriesID), recurrenceDatePosition, fields);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public void insertEvent(Event event) throws OXException {

        boolean ja = false;
        if (ja)
        {
            StorageMigration storageMigration = new StorageMigration(Services.get(), context.getContextId());
            //                        storageMigration.checkRead();
            try {
                MigrationResult result = storageMigration.run();

                System.out.println(result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            updated = insertEvent(connection, context.getContextId(), event);
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e, MAPPER, event, connection, "prg_dates");
        } finally {
            release(connection, updated);
        }
    }

    @Override
    public void insertEvents(List<Event> events) throws OXException {
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            for (Event event : events) {
                updated += insertEvent(connection, context.getContextId(), event);
            }
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e, MAPPER, null, connection, "prg_dates");
        } finally {
            release(connection, updated);
        }
    }

    @Override
    public void updateEvent(Event event) throws OXException {
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            updated = updateEvent(connection, context.getContextId(), asInt(event.getId()), event);
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e, MAPPER, event, connection, "prg_dates");
        } finally {
            release(connection, updated);
        }
    }

    @Override
    public void insertEventTombstone(Event event) throws OXException {
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            updated = insertTombstoneEvent(connection, context.getContextId(), event);
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e, MAPPER, event, connection, "del_dates");
        } finally {
            release(connection, updated);
        }
    }

    @Override
    public void insertEventTombstones(List<Event> events) throws OXException {
        if (null == events || 0 == events.size()) {
            return;
        }
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            for (Event event : events) {
                updated += insertTombstoneEvent(connection, context.getContextId(), event);
            }
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e, MAPPER, null, connection, "del_dates");
        } finally {
            release(connection, updated);
        }
    }

    @Override
    public void deleteEvent(String objectID) throws OXException {
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            updated = deleteEvent(connection, context.getContextId(), asInt(objectID));
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e, MAPPER, null, connection, "prg_dates");
        } finally {
            release(connection, updated);
        }
    }

    private static int deleteEvent(Connection connection, int contextID, int objectID) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM prg_dates WHERE cid=? AND intfield01=?;")) {
            stmt.setInt(1, contextID);
            stmt.setInt(2, objectID);
            return logExecuteUpdate(stmt);
        }
    }

    private int insertTombstoneEvent(Connection connection, int contextID, Event event) throws SQLException, OXException {
        return insertOrReplaceEvent(connection, "del_dates", true, contextID, event);
    }

    private int insertEvent(Connection connection, int contextID, Event event) throws SQLException, OXException {
        return insertOrReplaceEvent(connection, "prg_dates", false, contextID, event);
    }

    private int insertOrReplaceEvent(Connection connection, String tableName, boolean replace, int contextID, Event event) throws SQLException, OXException {
        EventField[] mappedFields = MAPPER.getMappedFields();
        String sql = new StringBuilder()
            .append(replace ? "REPLACE" : "INSERT").append(" INTO ").append(tableName).append(' ')
            .append("(cid,").append(MAPPER.getColumns(mappedFields)).append(") ").append("VALUES (?,").append(MAPPER.getParameters(mappedFields)).append(");")
        .toString();
        Event eventData = Compat.adjustPriorSave(this, connection, event);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, contextID);
            MAPPER.setParameters(stmt, parameterIndex, eventData, mappedFields);
            return logExecuteUpdate(stmt);
        }
    }

    private int updateEvent(Connection connection, int contextID, int objectID, Event event) throws SQLException, OXException {
        EventField[] assignedfields = MAPPER.getAssignedFields(event);
        String sql = new StringBuilder()
            .append("UPDATE prg_dates SET ").append(MAPPER.getAssignments(assignedfields)).append(' ')
            .append("WHERE cid=? AND intfield01=?;")
        .toString();
        Event eventData = Compat.adjustPriorSave(this, connection, event);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            parameterIndex = MAPPER.setParameters(stmt, parameterIndex, eventData, assignedfields);
            stmt.setInt(parameterIndex++, contextID);
            stmt.setInt(parameterIndex++, objectID);
            return logExecuteUpdate(stmt);
        }
    }

    private Event selectEvent(Connection connection, int contextID, int objectID, EventField[] fields) throws SQLException, OXException {
        EventField[] mappedFields = MAPPER.getMappedFields(fields);
        String sql = new StringBuilder()
            .append("SELECT ").append(MAPPER.getColumns(mappedFields)).append(" FROM prg_dates ")
            .append("WHERE cid=? AND intfield01=?;")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, contextID);
            stmt.setInt(2, objectID);
            ResultSet resultSet = logExecuteQuery(stmt);
            if (resultSet.next()) {
                return readEvent(connection, resultSet, mappedFields, null);
            }
        }
        return null;
    }

    private Event selectException(Connection connection, int contextID, int seriesID, long recurrenceDatePosition, EventField[] fields) throws SQLException, OXException {
        EventField[] mappedFields = MAPPER.getMappedFields(fields);
        String sql = new StringBuilder()
            .append("SELECT ").append(MAPPER.getColumns(mappedFields)).append(" FROM prg_dates ")
            .append("WHERE cid=? AND intfield02=? AND field08=? AND intfield01<>intfield02;")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, contextID);
            stmt.setInt(2, seriesID);
            stmt.setString(3, String.valueOf(recurrenceDatePosition));
            ResultSet resultSet = logExecuteQuery(stmt);
            if (resultSet.next()) {
                return readEvent(connection, resultSet, mappedFields, null);
            }
        }
        return null;
    }

    private Event readEvent(Connection connection, ResultSet resultSet, EventField[] fields, String columnLabelPrefix) throws SQLException, OXException {
        return Compat.adjustAfterLoad(this, connection, MAPPER.fromResultSet(resultSet, fields, columnLabelPrefix));
    }

    private List<Event> selectEvents(Connection connection, boolean deleted, int contextID, SearchTerm<?> searchTerm, List<SearchFilter> filters, SearchOptions searchOptions, EventField[] fields) throws SQLException, OXException {
        EventField[] mappedFields = MAPPER.getMappedFields(fields);
        SearchAdapter adapter = new SearchAdapter(contextID, null, "d.", "m.", "e.").append(searchTerm).append(filters);
        StringBuilder stringBuilder = new StringBuilder()
            .append("SELECT DISTINCT ").append(MAPPER.getColumns(mappedFields, "d.")).append(' ')
            .append("FROM ").append(deleted ? "del_dates" : "prg_dates").append(" AS d ")
        ;
        if (adapter.usesInternalAttendees()) {
            stringBuilder.append("LEFT JOIN ").append(deleted ? "del_dates_members" : "prg_dates_members").append(" AS m ")
            .append("ON d.cid=m.cid AND d.intfield01=m.object_id ");
        }
        if (adapter.usesExternalAttendees()) {
            stringBuilder.append("LEFT JOIN ").append(deleted ? "delDateExternal" : "dateExternal").append(" AS e ")
            .append("ON d.cid=e.cid AND d.intfield01=e.objectId ");
        }
        stringBuilder.append("WHERE d.cid=? ");
        if (null != searchOptions) {
            if (false == deleted && null != searchOptions.getFrom()) {
                stringBuilder.append("AND d.timestampfield02>? ");
            }
            if (false == deleted && null != searchOptions.getUntil()) {
                stringBuilder.append("AND d.timestampfield01<? ");
            }
        }
        stringBuilder.append("AND ").append(adapter.getClause()).append(getSortOptions(MAPPER, searchOptions, "d.")).append(';');
        List<Event> events = new ArrayList<Event>();
        try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, contextID);
            if (false == deleted && null != searchOptions && null != searchOptions.getFrom()) {
                stmt.setTimestamp(parameterIndex++, new Timestamp(searchOptions.getFrom().getTime()));
            }
            if (false == deleted && null != searchOptions && null != searchOptions.getUntil()) {
                stmt.setTimestamp(parameterIndex++, new Timestamp(searchOptions.getUntil().getTime()));
            }
            adapter.setParameters(stmt, parameterIndex);
            ResultSet resultSet = logExecuteQuery(stmt);
            while (resultSet.next()) {
                events.add(readEvent(connection, resultSet, mappedFields, "d."));
            }
        }
        return events;
    }

    private long countEvents(Connection connection, boolean deleted, int contextID, SearchTerm<?> searchTerm) throws SQLException, OXException {
        SearchAdapter adapter = new SearchAdapter(contextID, null, "d.", "m.", "e.").append(searchTerm);
        StringBuilder stringBuilder = new StringBuilder()
            .append("SELECT COUNT (DISTINCT d.intfield01) FROM ").append(deleted ? "del_dates" : "prg_dates").append(" AS d ")
        ;
        if (adapter.usesInternalAttendees()) {
            stringBuilder.append("LEFT JOIN ").append(deleted ? "del_dates_members" : "prg_dates_members").append(" AS m ")
                .append("ON d.cid=m.cid AND d.intfield01=m.object_id ");
        }
        if (adapter.usesExternalAttendees()) {
            stringBuilder.append("LEFT JOIN ").append(deleted ? "delDateExternal" : "dateExternal").append(" AS e ")
                .append("ON d.cid=e.cid AND d.intfield01=e.objectId ");
        }
        stringBuilder.append("WHERE d.cid=? ").append("AND ").append(adapter.getClause()).append(';');
        try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, contextID);
            adapter.setParameters(stmt, parameterIndex);
            try (ResultSet resultSet = logExecuteQuery(stmt)) {
                return resultSet.next() ? resultSet.getLong(1) : 0;
            }
        }
    }

    private List<Event> selectOverlappingEvents(Connection connection, int contextID, int[] userIDs, int[] otherEntityIDs, boolean includeTransparent, SearchOptions searchOptions, EventField[] fields) throws SQLException, OXException {
        EventField[] mappedFields = MAPPER.getMappedFields(fields);
        StringBuilder stringBuilder = new StringBuilder()
            .append("SELECT DISTINCT ").append(MAPPER.getColumns(mappedFields, "d.")).append(" FROM prg_dates AS d");
        if (null != userIDs && 0 < userIDs.length) {
            stringBuilder.append(" LEFT JOIN prg_dates_members AS m ON d.cid=m.cid AND d.intfield01=m.object_id");
        }
        if (null != otherEntityIDs && 0 < otherEntityIDs.length) {
            stringBuilder.append(" LEFT JOIN prg_date_rights AS r ON d.cid=r.cid AND d.intfield01=r.object_id");
        }
        stringBuilder.append(" WHERE d.cid=?");
        if (false == includeTransparent) {
            stringBuilder.append(" AND d.intfield06<>4");
        }
        if (null != searchOptions && null != searchOptions.getFrom()) {
            stringBuilder.append(" AND d.timestampfield02>=?");
        }
        if (null != searchOptions && null != searchOptions.getUntil()) {
            stringBuilder.append(" AND d.timestampfield01<=?");
        }
        if (null != userIDs && 0 < userIDs.length || null != otherEntityIDs && 0 < otherEntityIDs.length) {
            stringBuilder.append(" AND (");
            if (null != userIDs && 0 < userIDs.length) {
                if (1 == userIDs.length) {
                    stringBuilder.append("m.member_uid=?");
                } else {
                    stringBuilder.append("m.member_uid IN (").append(EventMapper.getParameters(userIDs.length)).append(')');
                }
                if (null != otherEntityIDs && 0 < otherEntityIDs.length) {
                    stringBuilder.append(" OR ");
                }
            }
            if (null != otherEntityIDs && 0 < otherEntityIDs.length) {
                if (1 == otherEntityIDs.length) {
                    stringBuilder.append("r.id=?");
                } else {
                    stringBuilder.append("r.id IN (").append(EventMapper.getParameters(otherEntityIDs.length)).append(')');
                }
            }
            stringBuilder.append(')');
        }
        stringBuilder.append(getSortOptions(MAPPER, searchOptions, "d.")).append(';');
        List<Event> events = new ArrayList<Event>();
        try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, contextID);
            if (null != searchOptions && null != searchOptions.getFrom()) {
                stmt.setTimestamp(parameterIndex++, new Timestamp(searchOptions.getFrom().getTime()));
            }
            if (null != searchOptions && null != searchOptions.getUntil()) {
                stmt.setTimestamp(parameterIndex++, new Timestamp(searchOptions.getUntil().getTime()));
            }
            if (null != userIDs && 0 < userIDs.length) {
                for (int id : userIDs) {
                    stmt.setInt(parameterIndex++, id);
                }
            }
            if (null != otherEntityIDs && 0 < otherEntityIDs.length) {
                for (int id : otherEntityIDs) {
                    stmt.setInt(parameterIndex++, id);
                }
            }
            ResultSet resultSet = logExecuteQuery(stmt);
            while (resultSet.next()) {
                events.add(readEvent(connection, resultSet, mappedFields, "d."));
            }
        }
        return events;
    }

    /**
     * Selects recurrence data for a specific series.
     *
     * @param connection The database connection to use
     * @param seriesID The series identifier to load the recurrence data for
     * @param deleted <code>true</code> to read from the <i>tombstone</i> tables, <code>false</code>, otherwise
     * @return The recurrence data, or <code>null</code> if not found
     */
    RecurrenceData selectRecurrenceData(Connection connection, int seriesID, boolean deleted) throws SQLException, OXException {
        EventField[] fields = MAPPER.getMappedFields(new EventField[] {
            EventField.ID, EventField.SERIES_ID, EventField.RECURRENCE_RULE, EventField.START_DATE, EventField.END_DATE
        });
        String sql = new StringBuilder()
            .append("SELECT ").append(MAPPER.getColumns(fields))
            .append(" FROM ").append(deleted ? "del_dates" : "prg_dates")
            .append(" WHERE cid=? AND intfield01=? AND intfield02=?;")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, context.getContextId());
            stmt.setInt(2, seriesID);
            stmt.setInt(3, seriesID);
            ResultSet resultSet = logExecuteQuery(stmt);
            if (resultSet.next()) {
                return new DefaultRecurrenceData(readEvent(connection, resultSet, fields, null));
            }
        }
        return null;
    }

    /**
     * Gets the used entity resolver.
     *
     * @return The entity resolver, or <code>null</code> if not set
     */
    EntityResolver getEntityResolver() {
        return entityResolver;
    }

}
