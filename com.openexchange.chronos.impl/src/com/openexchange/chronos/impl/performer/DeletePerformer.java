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

package com.openexchange.chronos.impl.performer;

import static com.openexchange.chronos.common.CalendarUtils.contains;
import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.isGroupScheduled;
import static com.openexchange.chronos.common.CalendarUtils.isLastUserAttendee;
import static com.openexchange.chronos.common.CalendarUtils.isOrganizer;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesException;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.chronos.impl.Check.requireCalendarPermission;
import static com.openexchange.chronos.impl.Check.requireUpToDateTimestamp;
import static com.openexchange.folderstorage.Permission.DELETE_ALL_OBJECTS;
import static com.openexchange.folderstorage.Permission.DELETE_OWN_OBJECTS;
import static com.openexchange.folderstorage.Permission.NO_PERMISSIONS;
import static com.openexchange.folderstorage.Permission.READ_FOLDER;
import java.util.SortedSet;
import java.util.TreeSet;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.DefaultRecurrenceData;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.CalendarResultImpl;
import com.openexchange.chronos.impl.Check;
import com.openexchange.chronos.impl.Consistency;
import com.openexchange.chronos.impl.UpdateResultImpl;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.RecurrenceData;
import com.openexchange.chronos.service.RecurrenceIterator;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.UserizedFolder;

/**
 * {@link DeletePerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class DeletePerformer extends AbstractUpdatePerformer {

    /**
     * Initializes a new {@link DeletePerformer}.
     *
     * @param storage The underlying calendar storage
     * @param session The calendar session
     * @param folder The calendar folder representing the current view on the events
     */
    public DeletePerformer(CalendarStorage storage, CalendarSession session, UserizedFolder folder) throws OXException {
        super(storage, session, folder);
    }

    /**
     * Performs the deletion of an event.
     *
     * @param objectID The identifier of the event to delete
     * @param recurrenceId The recurrence identifier of the occurrence to delete, or <code>null</code> if no specific occurrence is targeted
     * @param clientTimestamp The client timestamp to catch concurrent modifications
     * @return The result
     */
    public CalendarResultImpl perform(String objectID, RecurrenceId recurrenceId, long clientTimestamp) throws OXException {
        /*
         * load original event data & attendees
         */
        Event originalEvent = loadEventData(objectID, false);
        /*
         * check current session user's permissions
         */
        Check.eventIsInFolder(originalEvent, folder);
        requireDeletePermissions(originalEvent);
        requireUpToDateTimestamp(originalEvent, clientTimestamp);
        if (null == recurrenceId) {
            deleteEvent(originalEvent);
        } else {
            deleteRecurrence(originalEvent, recurrenceId);
        }
        return result;
    }

    /**
     * Deletes a single event.
     *
     * @param originalEvent The original event to delete
     * @return The result
     */
    private void deleteEvent(Event originalEvent) throws OXException {
        if (false == isGroupScheduled(originalEvent) || isOrganizer(originalEvent, calendarUserId) || isLastUserAttendee(originalEvent.getAttendees(), calendarUserId)) {
            /*
             * deletion of not group-scheduled event / by organizer / last user attendee
             */
            if (isSeriesException(originalEvent)) {
                deleteException(originalEvent);
            } else {
                delete(originalEvent);
            }
            return;
        }
        Attendee userAttendee = find(originalEvent.getAttendees(), calendarUserId);
        if (null != userAttendee) {
            /*
             * deletion as one of the attendees
             */
            if (isSeriesException(originalEvent)) {
                deleteException(originalEvent, userAttendee);
            } else {
                delete(originalEvent, userAttendee);
            }
            return;
        }
        /*
         * no delete permissions, otherwise
         */
        throw CalendarExceptionCodes.NO_DELETE_PERMISSION.create(folder.getID());
    }

    /**
     * Deletes a specific recurrence of a recurring event.
     *
     * @param originalEvent The original exception event, or the targeted series master event
     * @return The result
     */
    private void deleteRecurrence(Event originalEvent, RecurrenceId recurrenceId) throws OXException {
        if (false == isGroupScheduled(originalEvent) || isOrganizer(originalEvent, calendarUserId) || isLastUserAttendee(originalEvent.getAttendees(), calendarUserId)) {
            /*
             * deletion of not group-scheduled event / by organizer / last user attendee
             */
            if (isSeriesMaster(originalEvent)) {
                recurrenceId = Check.recurrenceIdExists(session.getRecurrenceService(), originalEvent, recurrenceId);
                Event originalExceptionEvent = optExceptionData(originalEvent.getSeriesId(), recurrenceId);
                if (null != originalExceptionEvent) {
                    /*
                     * deletion of existing change exception
                     */
                    // deleteException(loadExceptionData(originalEvent.getId(), recurrenceID));
                    // TODO: not supported in old stack (attempt fails with APP-0011), so throwing exception as expected by test for now
                    // com.openexchange.ajax.appointment.recurrence.TestsForCreatingChangeExceptions.testShouldFailIfTryingToCreateADeleteExceptionOnTopOfAChangeException())
                    throw CalendarExceptionCodes.INVALID_RECURRENCE_ID.create(
                        new Exception("Deletion of existing change exception not supported"), recurrenceId, originalEvent.getRecurrenceRule());
                } else {
                    /*
                     * creation of new delete exception
                     */
                    addDeleteExceptionDate(originalEvent, recurrenceId);
                }
                return;
            } else if (isSeriesException(originalEvent)) {
                /*
                 * deletion of existing change exception
                 */
                deleteException(originalEvent);
                return;
            }
            /*
             * unsupported, otherwise
             */
            throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(originalEvent.getId(), String.valueOf(recurrenceId));
        }
        Attendee userAttendee = find(originalEvent.getAttendees(), calendarUserId);
        if (null != userAttendee) {
            /*
             * deletion as attendee
             */
            if (isSeriesMaster(originalEvent)) {
                recurrenceId = Check.recurrenceIdExists(session.getRecurrenceService(), originalEvent, recurrenceId);
                Event originalExceptionEvent = optExceptionData(originalEvent.getSeriesId(), recurrenceId);
                if (null != originalExceptionEvent) {
                    /*
                     * deletion of existing change exception
                     */
                    // deleteException(loadExceptionData(originalEvent.getId(), recurrenceID), userAttendee);
                    // TODO: not supported in old stack (attempt fails with APP-0011), so throwing exception as expected by test for now
                    // com.openexchange.ajax.appointment.recurrence.TestsForCreatingChangeExceptions.testShouldFailIfTryingToCreateADeleteExceptionOnTopOfAChangeException())
                    throw CalendarExceptionCodes.INVALID_RECURRENCE_ID.create(
                        new Exception("Deletion of existing change exception not supported"), recurrenceId, originalEvent.getRecurrenceRule());
                } else {
                    /*
                     * creation of new delete exception
                     */
                    deleteFromRecurrence(originalEvent, recurrenceId, userAttendee);
                }
                return;
            } else if (isSeriesException(originalEvent)) {
                /*
                 * deletion of existing change exception
                 */
                deleteException(originalEvent, userAttendee);
                return;
            }
            /*
             * unsupported, otherwise
             */
            throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(originalEvent.getId(), String.valueOf(recurrenceId));
        }
        /*
         * no delete permissions, otherwise
         */
        throw CalendarExceptionCodes.NO_DELETE_PERMISSION.create(folder.getID());
    }

    /**
     * Adds a specific recurrence identifier to the series master's delete exception array, i.e. creates a new delete exception. A
     * previously existing entry for the recurrence identifier in the master's change exception date array is removed implicitly. In case
     * there are no occurrences remaining at all after the deletion, the whole series event is deleted.
     *
     * @param originalMasterEvent The original series master event
     * @param recurrenceID The recurrence identifier of the occurrence to add
     */
    private void addDeleteExceptionDate(Event originalMasterEvent, RecurrenceId recurrenceID) throws OXException {
        /*
         * build new set of delete exception dates
         */
        SortedSet<RecurrenceId> deleteExceptionDates = new TreeSet<RecurrenceId>();
        if (null != originalMasterEvent.getDeleteExceptionDates()) {
            deleteExceptionDates.addAll(originalMasterEvent.getDeleteExceptionDates());
        }
        if (false == deleteExceptionDates.add(recurrenceID)) {
            // TODO throw/log?
        }
        /*
         * check if there are any further occurrences left
         */
        RecurrenceData recurrenceData = new DefaultRecurrenceData(originalMasterEvent.getRecurrenceRule(), originalMasterEvent.getStartDate(), null);
        boolean hasOccurrences = false;
        RecurrenceIterator<RecurrenceId> iterator = session.getRecurrenceService().iterateRecurrenceIds(recurrenceData);
        while (iterator.hasNext()) {
            if (false == contains(deleteExceptionDates, iterator.next())) {
                hasOccurrences = true;
                break;
            }
        }
        if (hasOccurrences) {
            /*
             * update series master accordingly
             */
            Event eventUpdate = new Event();
            eventUpdate.setId(originalMasterEvent.getId());
            eventUpdate.setDeleteExceptionDates(deleteExceptionDates);
            Consistency.setModified(timestamp, eventUpdate, calendarUserId);
            storage.getEventStorage().updateEvent(eventUpdate);
            Event updatedMasterEvent = loadEventData(originalMasterEvent.getId());
            result.addUpdate(new UpdateResultImpl(originalMasterEvent, updatedMasterEvent));
        } else {
            /*
             * delete series master
             */
            delete(originalMasterEvent);
        }
    }

    /**
     * Deletes an existing change exception. Besides the removal of the change exception data via {@link #delete(Event)}, this also
     * includes adjusting the master event's change- and delete exception date arrays.
     *
     * @param originalExceptionEvent The original exception event
     */
    private void deleteException(Event originalExceptionEvent) throws OXException {
        /*
         * delete the exception
         */
        String seriesID = originalExceptionEvent.getSeriesId();
        RecurrenceId recurrenceID = originalExceptionEvent.getRecurrenceId();
        delete(originalExceptionEvent);
        /*
         * update the series master accordingly
         */
        addDeleteExceptionDate(loadEventData(seriesID), recurrenceID);
    }

    /**
     * Deletes a specific internal user attendee from an existing change exception. Besides the removal of the attendee via
     * {@link #delete(Event, Attendee)}, this also includes 'touching' the master event's last-modification timestamp.
     *
     * @param originalExceptionEvent The original exception event
     * @param originalAttendee The original attendee to delete
     */
    private void deleteException(Event originalExceptionEvent, Attendee originalAttendee) throws OXException {
        /*
         * delete the attendee in the exception
         */
        String seriesID = originalExceptionEvent.getSeriesId();
        delete(originalExceptionEvent, originalAttendee);
        /*
         * 'touch' the series master accordingly
         */
        Event originalMasterEvent = loadEventData(seriesID);
        touch(seriesID);
        result.addUpdate(new UpdateResultImpl(originalMasterEvent, loadEventData(seriesID)));
    }

    private void requireDeletePermissions(Event originalEvent) throws OXException {
        if (session.getUserId() == originalEvent.getCreatedBy()) {
            requireCalendarPermission(folder, READ_FOLDER, NO_PERMISSIONS, NO_PERMISSIONS, DELETE_OWN_OBJECTS);
        } else {
            requireCalendarPermission(folder, READ_FOLDER, NO_PERMISSIONS, NO_PERMISSIONS, DELETE_ALL_OBJECTS);
        }
        Check.classificationAllowsUpdate(folder, originalEvent);
    }

}
