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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Classification;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.UserizedEvent;
import com.openexchange.chronos.compat.AppointmentConstants;
import com.openexchange.chronos.compat.Recurrence;
import com.openexchange.database.DatabaseService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.impl.IDGenerator;
import com.openexchange.java.Strings;
import com.openexchange.tools.sql.DBUtils;

/**
 * {@link CalendarStorage}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class RdbCalendarStorage {

    private final int contextID;
    private final DatabaseService databaseService;

    /**
     * Initializes a new {@link RdbChecksumStore}.
     *
     * @param contextID The context ID
     */
    public RdbCalendarStorage(int contextID) throws OXException {
        super();
        this.contextID = contextID;
        this.databaseService = Services.getService(DatabaseService.class, true);
    }

    public UserizedEvent loadEvent(int userID, int objectID, int folderID) {

        return null;
    }

    public List<Alarm> loadAlarms(int userID, int objectID) throws OXException {
        Connection connection = databaseService.getReadOnly(contextID);
        try {
            return loadAlarms(connection, userID, objectID);
        } finally {
            databaseService.backReadOnly(contextID, connection);
        }
    }

    public List<Alarm> loadAlarms(Connection connection, int userID, int objectID) {


        return null;
    }

    private static List<Alarm> loadAlarms(Connection connection, int contextID, int objectID, int userID) {
        return null;
    }

    public int createEvent(Event event) throws OXException {
        Connection connection = databaseService.getWritable(contextID);
        try {
            int objectID = IDGenerator.getId(contextID, Types.APPOINTMENT, connection);
            insertEvent(connection, contextID, objectID, event);

            return objectID;
        } catch (SQLException e) {
            throw new OXException(e);
        } finally {
            databaseService.backWritable(contextID, connection);
        }
    }

    public Event loadEvent(int objectID) throws OXException {
        Connection connection = databaseService.getReadOnly(contextID);
        try {
            Event event = selectEvent(connection, contextID, objectID);
            event.setAttendees(selectAttendees(connection, contextID, objectID));

            return event;
        } catch (SQLException e) {
            throw new OXException(e);
        } finally {
            databaseService.backReadOnly(contextID, connection);
        }
    }

    private static int insertEvent(Connection connection, int contextID, int objectID, Event event) throws SQLException {
        List<Attendee> attendees = new ArrayList<Attendee>();
        PreparedStatement stmt = null;
        try {
        } finally {
            DBUtils.closeSQLStuff(stmt);
        }
        return 0;
    }

    private static Event selectEvent(Connection connection, int contextID, int objectID) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(SQL.SELECT_EVENT_STMT);
            stmt.setInt(1, contextID);
            stmt.setInt(2, objectID);
            ResultSet resultSet = SQL.logExecuteQuery(stmt);
            if (resultSet.next()) {
                Event event = new Event();
                event.setId(objectID);
                event.setCreated(resultSet.getTimestamp("creating_date"));
                event.setCreatedBy(resultSet.getInt("created_from"));
                event.setLastModified(new Date(resultSet.getLong("changing_date")));
                event.setModifiedBy(resultSet.getInt("changed_from"));
                // fid
                event.setClassification(resultSet.getBoolean("pflag") ? Classification.PRIVATE : Classification.PUBLIC);
                event.setStartDate(resultSet.getTimestamp("timestampfield01"));
                event.setEndDate(resultSet.getTimestamp("timestampfield02"));
                event.setStartTimezone(resultSet.getString("timezone"));
                event.setRecurrenceId(resultSet.getInt("intfield02"));
                // intfield03
                // intfield04
                // intfield05
                // intfield06
                event.setStatus(AppointmentConstants.getEventStatus(resultSet.getInt("intfield06")));
                event.setAllDay(resultSet.getBoolean("intfield07"));
                // intfield08
                event.setSummary(resultSet.getString("field01"));
                event.setLocation(resultSet.getString("field02"));
                event.setDescription(resultSet.getString("field04"));
                event.setRecurrenceRule(Recurrence.getRecurrenceRule(resultSet.getString("field06")));
                event.setDeleteExceptionDates(parseExceptionDates(resultSet.getString("field07")));
                event.setChangeExceptionDates(parseExceptionDates(resultSet.getString("field08")));
                event.setCategories(parseSeparatedStrings(resultSet.getString("field09")));
                event.setUid(resultSet.getString("uid"));
                String organizerMail = resultSet.getString("organizer");
                int organizerId = resultSet.getInt("organizerId");
                if (Strings.isNotEmpty(organizerMail) || 0 < organizerId) {
                    Organizer organizer = new Organizer();
                    organizer.setCuType(CalendarUserType.INDIVIDUAL);
                    if (Strings.isNotEmpty(organizerMail)) {
                        organizer.setUri("mailto:" + organizerMail);
                    }
                    if (0 < organizerId) {
                        organizer.setEntity(organizerId);
                    }
                    event.setOrganizer(organizer);
                }
                int sequence = resultSet.getInt("sequence");
                if (false == resultSet.wasNull()) {
                    event.setSequence(Integer.valueOf(sequence));
                }
                // principal
                // principalId
                event.setFilename(resultSet.getString("filename"));
                return event;
            }
        } finally {
            DBUtils.closeSQLStuff(stmt);
        }
        return null;
    }

    private static List<Attendee> selectAttendees(Connection connection, int contextID, int objectID) throws SQLException {
        List<Attendee> attendees = new ArrayList<Attendee>();
        attendees.addAll(selectInternalAttendees(connection, contextID, objectID));
        attendees.addAll(selectExternalAttendees(connection, contextID, objectID));
        return attendees;
    }

    private static List<Attendee> selectInternalAttendees(Connection connection, int contextID, int objectID) throws SQLException {
        List<Attendee> attendees = new ArrayList<Attendee>();
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(SQL.SELECT_INTERNAL_ATTENDEES_STMT);
            stmt.setInt(1, contextID);
            stmt.setInt(2, objectID);
            ResultSet resultSet = SQL.logExecuteQuery(stmt);
            while (resultSet.next()) {
                Attendee attendee = new Attendee();
                attendee.setEntity(resultSet.getInt("r.id"));
                attendee.setCuType(AppointmentConstants.getCalendarUserType(resultSet.getInt("r.type")));
                String mailAddress = resultSet.getString("r.ma");
                if (null != mailAddress) {
                    attendee.setUri("mailto:" + mailAddress);
                }
                attendee.setCommonName(resultSet.getString("r.dn"));
                int confirm = resultSet.getInt("m.confirm");
                if (resultSet.wasNull()) {
                    attendee.setPartStat(ParticipationStatus.ACCEPTED);
                } else {
                    attendee.setPartStat(AppointmentConstants.getParticipationStatus(confirm));
                }
                attendee.setComment(resultSet.getString("m.reason"));
                attendees.add(attendee);
            }
        } finally {
            DBUtils.closeSQLStuff(stmt);
        }
        return attendees;
    }

    private static List<Attendee> selectExternalAttendees(Connection connection, int contextID, int objectID) throws SQLException {
        List<Attendee> attendees = new ArrayList<Attendee>();
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(SQL.SELECT_EXTERNAL_ATTENDEES_STMT);
            stmt.setInt(1, contextID);
            stmt.setInt(2, objectID);
            ResultSet resultSet = SQL.logExecuteQuery(stmt);
            while (resultSet.next()) {
                Attendee attendee = new Attendee();
                String mailAddress = resultSet.getString("mailAddress");
                if (null != mailAddress) {
                    attendee.setUri("mailto:" + mailAddress);
                }
                attendee.setCommonName(resultSet.getString("displayName"));
                attendee.setPartStat(AppointmentConstants.getParticipationStatus(resultSet.getInt("confirm")));
                attendee.setComment(resultSet.getString("reason"));
                attendees.add(attendee);
            }
        } finally {
            DBUtils.closeSQLStuff(stmt);
        }
        return attendees;
    }

    private static List<Date> parseExceptionDates(String timestamps) {

        return null;
    }

    private static String parseRecurrenceRule(String seriesPattern) {

        return null;
    }

    private static List<String> parseSeparatedStrings(String strings) {
        String[] splittedStrings = Strings.splitByCommaNotInQuotes(strings);
        return null == splittedStrings ? null : Arrays.asList(splittedStrings);
    }

}
