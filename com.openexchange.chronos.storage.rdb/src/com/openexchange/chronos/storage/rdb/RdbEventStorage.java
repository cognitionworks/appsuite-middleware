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

package com.openexchange.chronos.storage.rdb;

import static com.openexchange.chronos.common.CalendarUtils.add;
import static com.openexchange.chronos.common.CalendarUtils.isFloating;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.I2i;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultRecurrenceData;
import com.openexchange.chronos.service.EntityResolver;
import com.openexchange.chronos.service.RecurrenceService;
import com.openexchange.chronos.service.SearchFilter;
import com.openexchange.chronos.service.SearchOptions;
import com.openexchange.chronos.service.SortOrder;
import com.openexchange.chronos.storage.EventStorage;
import com.openexchange.chronos.storage.rdb.osgi.Services;
import com.openexchange.database.provider.DBProvider;
import com.openexchange.database.provider.DBTransactionPolicy;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.tools.mappings.database.DbMapping;
import com.openexchange.search.SearchTerm;

/**
 * {@link RdbEventStorage}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class RdbEventStorage extends RdbStorage implements EventStorage {

    private static final EventMapper MAPPER = EventMapper.getInstance();

    private final int accountId;
    private final EntityProcessor entityProcessor;

    /**
     * Initializes a new {@link RdbEventStorage}.
     *
     * @param context The context
     * @param accountId The account identifier
     * @param entityResolver The entity resolver to use, or <code>null</code> if not available
     * @param dbProvider The database provider to use
     * @param txPolicy The transaction policy
     */
    public RdbEventStorage(Context context, int accountId, EntityResolver entityResolver, DBProvider dbProvider, DBTransactionPolicy txPolicy) {
        super(context, dbProvider, txPolicy);
        this.accountId = accountId;
        this.entityProcessor = new EntityProcessor(entityResolver);
    }

    @Override
    public String nextId() throws OXException {
        String value = null;
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            value = asString(nextId(connection, accountId, "calendar_event_sequence"));
            updated = 1;
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            release(connection, updated);
        }
        return value;
    }

    @Override
    public List<Event> searchEvents(SearchTerm<?> searchTerm, SearchOptions searchOptions, EventField[] fields) throws OXException {
        return searchEvents(searchTerm, null, searchOptions, fields);
    }

    @Override
    public List<Event> searchEvents(SearchTerm<?> searchTerm, List<SearchFilter> filters, SearchOptions searchOptions, EventField[] fields) throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return selectEvents(connection, false, searchTerm, filters, searchOptions, fields);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public Event loadEvent(String eventId, EventField[] fields) throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return selectEvent(connection, eventId, fields);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public Event loadException(String seriesId, RecurrenceId recurrenceId, EventField[] fields) throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return selectException(connection, seriesId, recurrenceId, fields);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public void insertEvent(Event event) throws OXException {
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            updated = insertEvent(connection, event);
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e, MAPPER, event, connection, "calendar_event");
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
            if (needsRangeUpdate(event)) {
                updated = updateEvent(connection, event.getId(), event, getRangeFrom(event), getRangeUntil(event));
            } else {
                updated = updateEvent(connection, event.getId(), event);
            }
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e, MAPPER, event, connection, "calendar_event");
        } finally {
            release(connection, updated);
        }
    }

    @Override
    public void deleteEvent(String eventId) throws OXException {
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            updated = deleteEvent(connection, eventId);
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e, MAPPER, null, connection, "calendar_event");
        } finally {
            release(connection, updated);
        }
    }

    @Override
    public List<Event> searchDeletedEvents(SearchTerm<?> searchTerm, SearchOptions searchOptions, EventField[] fields) throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return selectEvents(connection, true, searchTerm, null, searchOptions, fields);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public List<Event> searchOverlappingEvents(List<Attendee> attendees, boolean includeTransparent, SearchOptions searchOptions, EventField[] fields) throws OXException {
        Set<Integer> entities = new HashSet<Integer>();
        for (Attendee attendee : attendees) {
            if (CalendarUtils.isInternal(attendee)) {
                entities.add(I(attendee.getEntity()));
            }
        }
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return selectOverlappingEvents(connection, I2i(entities), includeTransparent, searchOptions, fields);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public void insertTombstoneEvent(Event event) throws OXException {
        int updated = 0;
        Connection connection = null;
        try {
            connection = dbProvider.getWriteConnection(context);
            txPolicy.setAutoCommit(connection, false);
            updated = replaceTombstoneEvent(connection, event);
            txPolicy.commit(connection);
        } catch (SQLException e) {
            throw asOXException(e, MAPPER, event, connection, "calendar_event_tombstone");
        } finally {
            release(connection, updated);
        }
    }

    private int deleteEvent(Connection connection, String id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM calendar_event WHERE cid=? AND account=? AND id=?;")) {
            stmt.setInt(1, context.getContextId());
            stmt.setInt(2, accountId);
            stmt.setInt(3, asInt(id));
            return logExecuteUpdate(stmt);
        }
    }

    private int insertEvent(Connection connection, Event event) throws SQLException, OXException {
        EventField[] mappedFields = MAPPER.getMappedFields();
        String sql = new StringBuilder()
            .append("INSERT INTO calendar_event ")
            .append("(cid,account,").append(MAPPER.getColumns(mappedFields)).append(",rangeFrom,rangeUntil) ")
            .append("VALUES (?,?,").append(MAPPER.getParameters(mappedFields)).append(",?,?);")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, context.getContextId());
            stmt.setInt(parameterIndex++, accountId);
            parameterIndex = MAPPER.setParameters(stmt, parameterIndex, entityProcessor.adjustPriorSave(event), mappedFields);
            stmt.setLong(parameterIndex++, getRangeFrom(event));
            stmt.setLong(parameterIndex++, getRangeUntil(event));
            return logExecuteUpdate(stmt);
        }
    }

    private int replaceTombstoneEvent(Connection connection, Event event) throws SQLException, OXException {
        EventField[] mappedFields = MAPPER.getMappedFields();
        String sql = new StringBuilder()
            .append("REPLACE INTO calendar_event_tombstone ")
            .append("(cid,account,").append(MAPPER.getColumns(mappedFields)).append(",rangeFrom,rangeUntil) ")
            .append("VALUES (?,?,").append(MAPPER.getParameters(mappedFields)).append(",?,?);")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, context.getContextId());
            stmt.setInt(parameterIndex++, accountId);
            parameterIndex = MAPPER.setParameters(stmt, parameterIndex, entityProcessor.adjustPriorSave(event), mappedFields);
            stmt.setLong(parameterIndex++, getRangeFrom(event));
            stmt.setLong(parameterIndex++, getRangeUntil(event));
            return logExecuteUpdate(stmt);
        }
    }

    private int updateEvent(Connection connection, String id, Event event) throws SQLException, OXException {
        EventField[] assignedfields = MAPPER.getAssignedFields(event);
        String sql = new StringBuilder()
            .append("UPDATE calendar_event SET ").append(MAPPER.getAssignments(assignedfields))
            .append(" WHERE cid=? AND account=? AND id=?;")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            parameterIndex = MAPPER.setParameters(stmt, parameterIndex, entityProcessor.adjustPriorSave(event), assignedfields);
            stmt.setInt(parameterIndex++, context.getContextId());
            stmt.setInt(parameterIndex++, accountId);
            stmt.setInt(parameterIndex++, asInt(id));
            return logExecuteUpdate(stmt);
        }
    }

    private int updateEvent(Connection connection, String id, Event event, long rangeFrom, long rangeUntil) throws SQLException, OXException {
        EventField[] assignedfields = MAPPER.getAssignedFields(event);
        String sql = new StringBuilder()
            .append("UPDATE calendar_event SET ").append(MAPPER.getAssignments(assignedfields))
            .append(",rangeFrom=?,rangeUntil=? WHERE cid=? AND account=? AND id=?;")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            parameterIndex = MAPPER.setParameters(stmt, parameterIndex, entityProcessor.adjustPriorSave(event), assignedfields);
            stmt.setLong(parameterIndex++, rangeFrom);
            stmt.setLong(parameterIndex++, rangeUntil);
            stmt.setInt(parameterIndex++, context.getContextId());
            stmt.setInt(parameterIndex++, accountId);
            stmt.setInt(parameterIndex++, asInt(id));
            return logExecuteUpdate(stmt);
        }
    }

    private Event selectEvent(Connection connection, String id, EventField[] fields) throws SQLException, OXException {
        EventField[] mappedFields = MAPPER.getMappedFields(fields);
        String sql = new StringBuilder()
            .append("SELECT ").append(MAPPER.getColumns(mappedFields)).append(" FROM calendar_event ")
            .append("WHERE cid=? AND account=? AND id=?;")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, context.getContextId());
            stmt.setInt(2, accountId);
            stmt.setInt(3, asInt(id));
            ResultSet resultSet = logExecuteQuery(stmt);
            if (resultSet.next()) {
                return readEvent(resultSet, mappedFields, null);
            }
        }
        return null;
    }

    private Event selectException(Connection connection, String seriesId, RecurrenceId recurrenceId, EventField[] fields) throws SQLException, OXException {
        EventField[] mappedFields = MAPPER.getMappedFields(fields);
        String sql = new StringBuilder()
            .append("SELECT ").append(MAPPER.getColumns(mappedFields)).append(" FROM calendar_event ")
            .append("WHERE cid=? AND account=? AND series=? AND recurrence=?;")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, context.getContextId());
            stmt.setInt(2, accountId);
            stmt.setInt(3, asInt(seriesId));
            stmt.setLong(4, recurrenceId.getValue());
            ResultSet resultSet = logExecuteQuery(stmt);
            if (resultSet.next()) {
                return readEvent(resultSet, mappedFields, null);
            }
        }
        return null;
    }

    private List<Event> selectEvents(Connection connection, boolean deleted, SearchTerm<?> searchTerm, List<SearchFilter> filters, SearchOptions searchOptions, EventField[] fields) throws SQLException, OXException {
        EventField[] mappedFields = MAPPER.getMappedFields(fields);
        SearchAdapter adapter = new SearchAdapter(context.getContextId(), null, "e.", "a.").append(searchTerm).append(filters);
        StringBuilder stringBuilder = new StringBuilder()
            .append("SELECT DISTINCT ").append(MAPPER.getColumns(mappedFields, "e."))
            .append(" FROM ").append(deleted ? "calendar_event_tombstone" : "calendar_event").append(" AS e ")
        ;
        if (adapter.usesAttendees()) {
            stringBuilder.append(" LEFT JOIN ").append(deleted ? "calendar_attendee_tombstone" : "calendar_attendee").append(" AS a ")
            .append("ON e.cid=a.cid AND e.account=a.account AND e.id=a.event");
        }
        stringBuilder.append(" WHERE e.cid=? AND e.account=?");
        if (null != searchOptions && null != searchOptions.getFrom()) {
            stringBuilder.append(" AND e.rangeUntil>?");
        }
        if (null != searchOptions && null != searchOptions.getUntil()) {
            stringBuilder.append(" AND e.rangeFrom<?");
        }
        stringBuilder.append(" AND ").append(adapter.getClause()).append(getSortOptions(searchOptions, "e.")).append(';');
        List<Event> events = new ArrayList<Event>();
        try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, context.getContextId());
            stmt.setInt(parameterIndex++, accountId);
            if (null != searchOptions && null != searchOptions.getFrom()) {
                stmt.setLong(parameterIndex++, searchOptions.getFrom().getTime());
            }
            if (null != searchOptions && null != searchOptions.getUntil()) {
                stmt.setLong(parameterIndex++, searchOptions.getUntil().getTime());
            }
            adapter.setParameters(stmt, parameterIndex++);
            ResultSet resultSet = logExecuteQuery(stmt);
            while (resultSet.next()) {
                events.add(readEvent(resultSet, mappedFields, "e."));
            }
        }
        return events;
    }

    private List<Event> selectOverlappingEvents(Connection connection, int[] entities, boolean includeTransparent, SearchOptions searchOptions, EventField[] fields) throws SQLException, OXException {
        EventField[] mappedFields = MAPPER.getMappedFields(fields);
        StringBuilder stringBuilder = new StringBuilder()
            .append("SELECT DISTINCT ").append(MAPPER.getColumns(mappedFields, "e."))
            .append(" FROM calendar_event AS e")
        ;
        if (null != entities && 0 < entities.length) {
            stringBuilder.append(" LEFT JOIN calendar_attendee AS a ON e.cid=a.cid AND e.account=a.account AND e.id=a.event");
        }
        stringBuilder.append(" WHERE e.cid=? AND e.account=?");
        if (null != searchOptions && null != searchOptions.getFrom()) {
            stringBuilder.append(" AND e.rangeUntil>?");
        }
        if (null != searchOptions && null != searchOptions.getUntil()) {
            stringBuilder.append(" AND e.rangeFrom<?");
        }
        if (false == includeTransparent) {
            stringBuilder.append(" AND e.transp<>0");
        }
        if (null != entities && 0 < entities.length) {
            if (1 == entities.length) {
//                stringBuilder.append(" AND a.entity=?");
                stringBuilder.append(" AND ((e.folder IS NULL AND a.entity=?) OR (e.folder IS NOT NULL AND e.createdBy=?))");
            } else {
//                stringBuilder.append(" AND a.entity IN (").append(EventMapper.getParameters(entities.length)).append(')');
                stringBuilder.append(" AND ((e.folder IS NULL AND a.entity IN (").append(EventMapper.getParameters(entities.length)).append(")) OR ")
                    .append("(e.folder IS NOT NULL AND e.createdBy IN (").append(EventMapper.getParameters(entities.length)).append("))");
            }
        }
        stringBuilder.append(getSortOptions(searchOptions, "e.")).append(';');
        List<Event> events = new ArrayList<Event>();
        try (PreparedStatement stmt = connection.prepareStatement(stringBuilder.toString())) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, context.getContextId());
            stmt.setInt(parameterIndex++, accountId);
            if (null != searchOptions && null != searchOptions.getFrom()) {
                stmt.setLong(parameterIndex++, searchOptions.getFrom().getTime());
            }
            if (null != searchOptions && null != searchOptions.getUntil()) {
                stmt.setLong(parameterIndex++, searchOptions.getUntil().getTime());
            }
            if (null != entities && 0 < entities.length) {
                for (int entity : entities) {
                    stmt.setInt(parameterIndex++, entity);
                    stmt.setInt(parameterIndex++, entity);
                }
            }
            ResultSet resultSet = logExecuteQuery(stmt);
            while (resultSet.next()) {
                events.add(readEvent(resultSet, mappedFields, "e."));
            }
        }
        return events;
    }

    private Event readEvent(ResultSet resultSet, EventField[] fields, String columnLabelPrefix) throws SQLException, OXException {
        Event event = MAPPER.fromResultSet(resultSet, fields, columnLabelPrefix);
        return entityProcessor.adjustAfterLoad(event);
    }

    /**
     * Gets the SQL representation of the supplied sort options, optionally prefixing any used column identifiers.
     *
     * @param searchOptions The search options to get the SQL representation for
     * @param prefix The prefix to use, or <code>null</code> if not needed
     * @return The <code>ORDER BY ... LIMIT ...</code> clause, or an empty string if no sort options were specified
     */
    private static String getSortOptions(SearchOptions searchOptions, String prefix) throws OXException {
        if (null == searchOptions || SearchOptions.EMPTY.equals(searchOptions)) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        SortOrder[] sortOrders = searchOptions.getSortOrders();
        if (null != sortOrders && 0 < sortOrders.length) {
            stringBuilder.append(" ORDER BY ").append(getColumnLabel(sortOrders[0].getBy(), prefix)).append(sortOrders[0].isDescending() ? " DESC" : " ASC");
            for (int i = 1; i < sortOrders.length; i++) {
                stringBuilder.append(", ").append(getColumnLabel(sortOrders[i].getBy(), prefix)).append(sortOrders[i].isDescending() ? " DESC" : " ASC");
            }
        }
        if (0 < searchOptions.getLimit()) {
            stringBuilder.append(" LIMIT ");
            if (0 < searchOptions.getOffset()) {
                stringBuilder.append(searchOptions.getOffset()).append(", ");
            }
            stringBuilder.append(searchOptions.getLimit());
        }
        return stringBuilder.toString();
    }

    private static String getColumnLabel(EventField field, String prefix) throws OXException {
        DbMapping<? extends Object, Event> mapping = MAPPER.get(field);
        return null != prefix ? mapping.getColumnLabel(prefix) : mapping.getColumnLabel();
    }

    /**
     * Gets a value indicating whether the supplied updated event data also requires updating the <code>rangeFrom</code> and
     * <code>rangeUntil</code> columns, depending on which properties were changed.
     *
     * @param event The event to check
     * @return <code>true</code> if the range needs to be updated, <code>false</code>, otherwise
     */
    private static boolean needsRangeUpdate(Event event) {
        return event.containsStartDate() || event.containsStartTimeZone() || event.containsEndDate() || event.containsEndTimeZone() ||
            event.containsAllDay() || event.containsRecurrenceRule();
    }

    /**
     * Calculates the start time of the effective range of an event, i.e. the start of the period the event spans. The range is always
     * the maximum, timezone-independent range for any possible occurrence of the event, i.e. the range for <i>floating</i> events is
     * expanded with the minimum/maximum timezone offsets, and the period for recurring event series will span from the first until the
     * last possible occurrence (or {@link Long#MAX_VALUE} for never ending series).
     *
     * @param event The event to get the range for
     * @return The start time of the effective range of an event
     */
    private static long getRangeFrom(Event event) {
        Date rangeFrom = event.getStartDate();
        if (isFloating(event)) {
            /*
             * add easternmost offset (GMT +14:00)
             */
            rangeFrom = add(rangeFrom, Calendar.HOUR_OF_DAY, -14);
        }
        return rangeFrom.getTime();
    }

    /**
     * Calculates the end time of the effective range of an event, i.e. the end of the period the event spans. The range is always
     * the maximum, timezone-independent range for any possible occurrence of the event, i.e. the range for <i>floating</i> events is
     * expanded with the minimum/maximum timezone offsets, and the period for recurring event series will span from the first until the
     * last possible occurrence (or {@link Long#MAX_VALUE} for never ending series).
     *
     * @param event The event to get the range for
     * @return The start time of the effective range of an event
     */
    private static long getRangeUntil(Event event) throws OXException {
        Date rangeUntil = null != event.getEndDate() ? event.getEndDate() : event.getStartDate();
        if (isSeriesMaster(event)) {
            /*
             * take over end-date of last occurrence
             */
            RecurrenceId lastRecurrenceId = Services.getService(RecurrenceService.class).getLastOccurrence(new DefaultRecurrenceData(event));
            if (null == lastRecurrenceId) {
                return Long.MAX_VALUE; // never ending series
            }
            long eventDuration = event.getEndDate().getTime() - event.getStartDate().getTime();
            rangeUntil = new Date(lastRecurrenceId.getValue() + eventDuration);
        }
        if (isFloating(event)) {
            /*
             * add offset of 'westernmost' timezone offset (GMT-12:00)
             */
            rangeUntil = add(rangeUntil, Calendar.HOUR_OF_DAY, 12);
        }
        return rangeUntil.getTime();
    }

}
