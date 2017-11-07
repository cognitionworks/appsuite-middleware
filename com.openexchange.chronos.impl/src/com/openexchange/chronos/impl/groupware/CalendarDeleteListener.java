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

package com.openexchange.chronos.impl.groupware;

import java.sql.Connection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.ResourceId;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.service.CalendarHandler;
import com.openexchange.chronos.service.CalendarUtilities;
import com.openexchange.chronos.storage.CalendarStorageFactory;
import com.openexchange.database.provider.DBProvider;
import com.openexchange.database.provider.SimpleDBProvider;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.delete.DeleteEvent;
import com.openexchange.groupware.delete.DeleteFailedExceptionCodes;
import com.openexchange.groupware.delete.DeleteListener;
import com.openexchange.osgi.ServiceSet;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * {@link CalendarDeleteListener} - Cleans up calendar data on deletion of types
 * <li>{@link DeleteEvent#TYPE_USER}</li>
 * <li>{@link DeleteEvent#TYPE_GROUP}</li>
 * <li>{@link DeleteEvent#TYPE_RESOURCE} </li>
 * 
 * Type {@link DeleteEvent#TYPE_CONTEXT} is handled by com.openexchange.admin.storage.mysqlStorage.OXContextMySQLStorage#deleteTablesData.
 * Type {@link DeleteEvent#TYPE_RESOURCE_GROUP} will thrown an appropriated error.
 * 
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.0
 */
public final class CalendarDeleteListener implements DeleteListener {

    private final CalendarStorageFactory      factory;
    private final CalendarUtilities           calendarUtilities;
    private final ServiceSet<CalendarHandler> calendarHandlers;

    /**
     * Initializes a new {@link CalendarDeleteListener}.
     * 
     * @param factory The {@link CalendarStorageFactory}
     * @param calendarUtilities The {@link CalendarUtilities}
     * @param calendarHandlers The {@link CalendarHandler}s to notify
     */
    public CalendarDeleteListener(CalendarStorageFactory factory, CalendarUtilities calendarUtilities, ServiceSet<CalendarHandler> calendarHandlers) {
        super();
        this.factory = factory;
        this.calendarUtilities = calendarUtilities;
        this.calendarHandlers = calendarHandlers;
    }

    @Override
    public void deletePerformed(DeleteEvent deleteEvent, Connection readCon, Connection writeCon) throws OXException {
        switch (deleteEvent.getType()) {
            case DeleteEvent.TYPE_USER:
                if (DeleteEvent.SUBTYPE_ANONYMOUS_GUEST != deleteEvent.getSubType() && DeleteEvent.SUBTYPE_INVITED_GUEST != deleteEvent.getSubType()) {
                    purgeUserData(new SimpleDBProvider(readCon, writeCon), deleteEvent.getContext(), deleteEvent.getId(), deleteEvent.getDestinationUserID(), deleteEvent.getSession());
                }
                break;
            case DeleteEvent.TYPE_GROUP:
            case DeleteEvent.TYPE_RESOURCE:
                deleteAttendee(new SimpleDBProvider(readCon, writeCon), deleteEvent.getContext(), deleteEvent.getId(), deleteEvent.getSession());
                break;
            case DeleteEvent.TYPE_RESOURCE_GROUP:
                throw DeleteFailedExceptionCodes.ERROR.create("Deletion of type RESOURCE_GROUP is not supported!");
            case DeleteEvent.TYPE_CONTEXT:
                /*
                 * DeleteEvent.TYPE_CONTEXT is handled by com.openexchange.admin.storage.mysqlStorage.OXContextMySQLStorage.deleteTablesData(String, Integer, Connection, boolean)
                 * All tables containing a 'cid' will be cleaned up by this. So there is no need to delete anything here.
                 */
                break;
            default:
                throw DeleteFailedExceptionCodes.UNKNOWN_TYPE.create(Integer.valueOf(deleteEvent.getType()));
        }
    }

    /**
     * Purges the user data
     * 
     * @param dbProvider The {@link DBProvider}
     * @param context The {@link Context}
     * @param userId The user identifier
     * @param destinationUserId The identifier of the destination user specified in {@link DeleteEvent#getDestinationUserID()}
     * @param adminSession The context admins session
     * @throws OXException In case service is unavailable or SQL error
     */
    private void purgeUserData(DBProvider dbProvider, Context context, int userId, Integer destinationUserId, Session adminSession) throws OXException {
        StorageUpdater updater = new StorageUpdater(context, userId, destinationUserId, calendarUtilities, factory, dbProvider, calendarHandlers);

        /*
         * Update events where the user is attendee, delete where he is the last internal user
         */
        Date date = new Date();
        List<Event> events = updater.searchEvents();
        List<Event> eventsToDelete = new LinkedList<>();
        ServerSession serverSession = ServerSessionAdapter.valueOf(userId, context.getContextId());
        for (final Event event : events) {
            if (CalendarUtils.isLastUserAttendee(event.getAttendees(), userId)) {
                // The attendee is the only one left, delete event
                eventsToDelete.add(event);
            }
        }
        updater.deleteEvent(eventsToDelete, serverSession, date);
        events.removeAll(eventsToDelete);
        updater.removeAttendeeFrom(events, date);

        /*
         * Update event fields where the user might be referenced
         */
        EventField[] fields = new EventField[] { EventField.ID, EventField.CREATED_BY, EventField.MODIFIED_BY, EventField.CALENDAR_USER, EventField.ORGANIZER };

        // Update events which the user created
        events = updater.searchEvents(CalendarUtils.getSearchTerm(EventField.CREATED_BY, SingleOperation.EQUALS, Integer.valueOf(userId)), fields);
        updater.replaceAttendeeIn(events, date);

        // Update events where the user is the modifier
        events = updater.searchEvents(CalendarUtils.getSearchTerm(EventField.MODIFIED_BY, SingleOperation.EQUALS, Integer.valueOf(userId)), fields);
        updater.replaceAttendeeIn(events, date);

        // Update events where the user is the calendar user
        events = updater.searchEvents(CalendarUtils.getSearchTerm(EventField.CALENDAR_USER, SingleOperation.EQUALS, Integer.valueOf(userId)), fields);
        updater.replaceAttendeeIn(events, date);

        // Update events where the user is the organizer
        events = updater.searchEvents(CalendarUtils.getSearchTerm(EventField.ORGANIZER, SingleOperation.EQUALS, ResourceId.forUser(context.getContextId(), userId)), fields);
        updater.replaceAttendeeIn(events, date);

        /*
         * Delete account
         */
        updater.deleteAccount();

        // Trigger calendar events
        updater.notifyCalendarHandlers(adminSession);
    }

    /**
     * Removes the given attendee from every event it attends, set the modification date in the event accordingly
     * and finally deletes the attendee.
     * 
     * @param dbProvider The {@link DBProvider}
     * @param context The {@link Context}
     * @param attendeeId The identifier of the attendee. Should be either a {@link CalendarUserType#GROUP} or {@link CalendarUserType#RESOURCE}
     * @param adminSession The context admins session
     * @throws OXException In case service is unavailable or SQL error
     */
    private void deleteAttendee(DBProvider dbProvider, Context context, int attendeeId, Session adminSession) throws OXException {
        StorageUpdater updater = new StorageUpdater(context, attendeeId, null, calendarUtilities, factory, dbProvider, calendarHandlers);
        Date date = new Date();
        updater.removeAttendeeFrom(updater.searchEvents(), date);
        updater.notifyCalendarHandlers(adminSession);
    }
}
