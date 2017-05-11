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

import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.chronos.impl.Utils.getCalendarUser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.Period;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.AttendeeMapper;
import com.openexchange.chronos.impl.CalendarResultImpl;
import com.openexchange.chronos.impl.Consistency;
import com.openexchange.chronos.impl.CreateResultImpl;
import com.openexchange.chronos.impl.DeleteResultImpl;
import com.openexchange.chronos.impl.EventMapper;
import com.openexchange.chronos.impl.UpdateResultImpl;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.groupware.ldap.User;

/**
 * {@link AbstractUpdatePerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public abstract class AbstractUpdatePerformer {

    protected final CalendarSession session;
    protected final CalendarStorage storage;
    protected final User calendarUser;
    protected final UserizedFolder folder;
    protected final Date timestamp;
    protected final CalendarResultImpl result;

    /**
     * Initializes a new {@link AbstractUpdatePerformer}.
     *
     * @param storage The underlying calendar storage
     * @param session The calendar session
     * @param folder The calendar folder representing the current view on the events
     */
    protected AbstractUpdatePerformer(CalendarStorage storage, CalendarSession session, UserizedFolder folder) throws OXException {
        super();
        this.folder = folder;
        this.calendarUser = getCalendarUser(folder);
        this.session = session;
        this.timestamp = new Date();
        this.storage = storage;
        this.result = new CalendarResultImpl(session, calendarUser, folder.getID()).applyTimestamp(timestamp);
    }

    /**
     * Prepares a new change exception for a recurring event series.
     *
     * @param originalMasterEvent The original master event
     * @param recurrenceID The recurrence identifier
     * @return The prepared exception event
     */
    protected Event prepareException(Event originalMasterEvent, RecurrenceId recurrenceID) throws OXException {
        Event exceptionEvent = EventMapper.getInstance().copy(originalMasterEvent, new Event(), (EventField[]) null);
        exceptionEvent.setId(storage.getEventStorage().nextId());
        exceptionEvent.setRecurrenceId(recurrenceID);
        exceptionEvent.setChangeExceptionDates(new TreeSet<RecurrenceId>(Collections.singleton(recurrenceID)));
        exceptionEvent.setDeleteExceptionDates(null);
        exceptionEvent.setStartDate(new Date(recurrenceID.getValue()));
        exceptionEvent.setEndDate(new Date(recurrenceID.getValue() + new Period(originalMasterEvent).getDuration()));
        Consistency.setCreated(timestamp, exceptionEvent, originalMasterEvent.getCreatedBy());
        Consistency.setModified(timestamp, exceptionEvent, session.getUserId());
        return exceptionEvent;
    }

    /**
     * <i>Touches</i> an event in the storage by setting it's last modification timestamp and modified-by property to the current
     * timestamp and calendar user.
     *
     * @param id The identifier of the event to <i>touch</i>
     */
    protected void touch(String id) throws OXException {
        Event eventUpdate = new Event();
        eventUpdate.setId(id);
        Consistency.setModified(timestamp, eventUpdate, session.getUserId());
        storage.getEventStorage().updateEvent(eventUpdate);
    }

    /**
     * Adds a specific recurrence identifier to the series master's change exception array and updates the series master event in the
     * storage. Also, an appropriate update result is added.
     *
     * @param originalMasterEvent The original series master event
     * @param recurrenceID The recurrence identifier of the occurrence to add
     */
    protected void addChangeExceptionDate(Event originalMasterEvent, RecurrenceId recurrenceID) throws OXException {
        SortedSet<RecurrenceId> changeExceptionDates = new TreeSet<RecurrenceId>();
        if (null != originalMasterEvent.getChangeExceptionDates()) {
            changeExceptionDates.addAll(originalMasterEvent.getChangeExceptionDates());
        }
        if (false == changeExceptionDates.add(recurrenceID)) {
            // TODO throw/log?
        }
        Event eventUpdate = new Event();
        eventUpdate.setId(originalMasterEvent.getId());
        eventUpdate.setChangeExceptionDates(changeExceptionDates);
        Consistency.setModified(timestamp, eventUpdate, calendarUser.getId());
        storage.getEventStorage().updateEvent(eventUpdate);
        result.addUpdate(new UpdateResultImpl(originalMasterEvent, loadEventData(originalMasterEvent.getId())));
    }

    /**
     * Deletes a single event from the storage. This can be used for any kind of event, i.e. a single, non-recurring event, an existing
     * exception of an event series, or an event series. For the latter one, any existing event exceptions are deleted as well.
     * <p/>
     * The event's attendees are loaded on demand if not yet present in the passed <code>originalEvent</code> {@code originalEvent}.
     * <p/>
     * The deletion includes:
     * <ul>
     * <li>insertion of a <i>tombstone</i> record for the original event</li>
     * <li>insertion of <i>tombstone</i> records for the original event's attendees</li>
     * <li>deletion of any alarms associated with the event</li>
     * <li>deletion of any attachments associated with the event</li>
     * <li>deletion of the event</li>
     * <li>deletion of the event's attendees</li>
     * </ul>
     *
     * @param originalEvent The original event to delete
     */
    protected void delete(Event originalEvent) throws OXException {
        /*
         * recursively delete any existing event exceptions
         */
        if (isSeriesMaster(originalEvent)) {
            deleteExceptions(originalEvent.getSeriesId(), originalEvent.getChangeExceptionDates());
        }
        /*
         * delete event data from storage
         */
        String id = originalEvent.getId();
        storage.getEventStorage().insertTombstoneEvent(EventMapper.getInstance().getTombstone(originalEvent, timestamp, calendarUser.getId()));
        storage.getAttendeeStorage().insertTombstoneAttendees(id, AttendeeMapper.getInstance().getTombstones(originalEvent.getAttendees()));
        storage.getAlarmStorage().deleteAlarms(id);
        storage.getAttachmentStorage().deleteAttachments(session.getSession(), folder.getID(), id, originalEvent.getAttachments());
        storage.getEventStorage().deleteEvent(id);
        storage.getAttendeeStorage().deleteAttendees(id, originalEvent.getAttendees());
        result.addDeletion(new DeleteResultImpl(originalEvent));
    }

    /**
     * Deletes a specific internal user attendee from a single event from the storage. This can be used for any kind of event, i.e. a
     * single, non-recurring event, an existing exception of an event series, or an event series. For the latter one, the attendee is deleted from
     * any existing event exceptions as well.
     * <p/>
     * The deletion includes:
     * <ul>
     * <li>insertion of a <i>tombstone</i> record for the original event</li>
     * <li>insertion of <i>tombstone</i> records for the original attendee</li>
     * <li>deletion of any alarms of the attendee associated with the event</li>
     * <li>deletion of the attendee from the event</li>
     * <li>update of the last-modification timestamp of the original event</li>
     * </ul>
     *
     * @param originalEvent The original event to delete
     * @param originalAttendee The original attendee to delete
     */
    protected void delete(Event originalEvent, Attendee originalAttendee) throws OXException {
        /*
         * recursively delete any existing event exceptions for this attendee
         */
        int userID = originalAttendee.getEntity();
        if (isSeriesMaster(originalEvent)) {
            deleteExceptions(originalEvent.getSeriesId(), originalEvent.getChangeExceptionDates(), userID);
        }
        /*
         * delete event data from storage for this attendee
         */
        String objectID = originalEvent.getId();
        storage.getEventStorage().insertTombstoneEvent(EventMapper.getInstance().getTombstone(originalEvent, timestamp, calendarUser.getId()));
        storage.getAttendeeStorage().insertTombstoneAttendee(objectID, originalAttendee);
        storage.getAttendeeStorage().deleteAttendees(objectID, Collections.singletonList(originalAttendee));
        storage.getAlarmStorage().deleteAlarms(objectID, userID);
        /*
         * 'touch' event & add track update result
         */
        touch(objectID);
        result.addUpdate(new UpdateResultImpl(originalEvent, loadEventData(objectID)));
    }

    /**
     * Deletes change exception events from the storage.
     * <p/>
     * For each change exception, the data is removed by invoking {@link #delete(Event)} for the exception.
     *
     * @param seriesID The series identifier
     * @param exceptionDates The recurrence identifiers of the change exceptions to delete
     */
    protected void deleteExceptions(String seriesID, Collection<RecurrenceId> exceptionDates) throws OXException {
        for (Event originalExceptionEvent : loadExceptionData(seriesID, exceptionDates)) {
            delete(originalExceptionEvent);
        }
    }

    /**
     * Deletes a specific internal user attendee from change exception events from the storage.
     * <p/>
     * For each change exception, the data is removed by invoking {@link #delete(Event, Attendee)} for the exception, in case the
     * user is found the exception's attendee list.
     *
     * @param seriesID The series identifier
     * @param exceptionDates The recurrence identifiers of the change exceptions to delete
     * @param userID The identifier of the user attendee to delete
     */
    protected void deleteExceptions(String seriesID, Collection<RecurrenceId> exceptionDates, int userID) throws OXException {
        for (Event originalExceptionEvent : loadExceptionData(seriesID, exceptionDates)) {
            Attendee originalUserAttendee = find(originalExceptionEvent.getAttendees(), userID);
            if (null != originalUserAttendee) {
                delete(originalExceptionEvent, originalUserAttendee);
            }
        }
    }

    /**
     * Deletes a specific internal user attendee from a specific occurrence of a series event that does not yet exist as change exception.
     * This includes the creation of the corresponding change exception, and the removal of the user attendee from this exception's
     * attendee list.
     *
     * @param originalMasterEvent The original series master event
     * @param recurrenceID The recurrence identifier of the occurrence to remove the attendee for
     * @param originalAttendee The original attendee to delete from the recurrence
     */
    protected void deleteFromRecurrence(Event originalMasterEvent, RecurrenceId recurrenceID, Attendee originalAttendee) throws OXException {
        /*
         * create new exception event
         */
        Event exceptionEvent = prepareException(originalMasterEvent, recurrenceID);
        storage.getEventStorage().insertEvent(exceptionEvent);
        /*
         * take over all other original attendees
         */
        List<Attendee> excpetionAttendees = new ArrayList<Attendee>(originalMasterEvent.getAttendees());
        excpetionAttendees.remove(originalAttendee);
        storage.getAttendeeStorage().insertAttendees(exceptionEvent.getId(), excpetionAttendees);
        /*
         * take over all other original alarms
         */
        for (Entry<Integer, List<Alarm>> entry : storage.getAlarmStorage().loadAlarms(originalMasterEvent).entrySet()) {
            int userID = entry.getKey().intValue();
            if (userID != originalAttendee.getEntity()) {
                storage.getAlarmStorage().insertAlarms(exceptionEvent, userID, entry.getValue());
            }
        }
        /*
         * take over all original attachments
         */
        storage.getAttachmentStorage().insertAttachments(session.getSession(), folder.getID(), exceptionEvent.getId(), originalMasterEvent.getAttachments());
        result.addCreation(new CreateResultImpl(loadEventData(exceptionEvent.getId())));
        /*
         * track new change exception date in master
         */
        addChangeExceptionDate(originalMasterEvent, recurrenceID);
    }

    /**
     * Loads all data for a specific event, including attendees, attachments and alarms.
     * <p/>
     * The parent folder identifier is set based on {@link AbstractUpdatePerformer#folder}
     *
     * @param id The identifier of the event to load
     * @return The event data
     * @throws OXException {@link CalendarExceptionCodes#EVENT_NOT_FOUND}
     */
    protected Event loadEventData(String id) throws OXException {
        Event event = storage.getEventStorage().loadEvent(id, null);
        if (null == event) {
            throw CalendarExceptionCodes.EVENT_NOT_FOUND.create(id);
        }
        event = Utils.loadAdditionalEventData(storage, calendarUser.getId(), event, EventField.values());
        event.setFolderId(folder.getID());
        return event;
    }

    protected List<Event> loadExceptionData(String seriesID, Collection<RecurrenceId> recurrenceIDs) throws OXException {
        List<Event> exceptions = new ArrayList<Event>();
        if (null != recurrenceIDs && 0 < recurrenceIDs.size()) {
            for (RecurrenceId recurrenceID : recurrenceIDs) {
                Event exception = storage.getEventStorage().loadException(seriesID, recurrenceID, null);
                if (null == exception) {
                    throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(seriesID, String.valueOf(recurrenceID));
                }
                exception.setFolderId(folder.getID());
                exceptions.add(exception);
            }
        }
        return Utils.loadAdditionalEventData(storage, calendarUser.getId(), exceptions, EventField.values());
    }

    protected Event loadExceptionData(String seriesID, RecurrenceId recurrenceID) throws OXException {
        Event exception = storage.getEventStorage().loadException(seriesID, recurrenceID, null);
        if (null == exception) {
            throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(seriesID, String.valueOf(recurrenceID));
        }
        exception = Utils.loadAdditionalEventData(storage, calendarUser.getId(), exception, EventField.values());
        exception.setFolderId(folder.getID());
        return exception;
    }

    /**
     * Gets the identifier of a specific user's default personal calendar folder.
     *
     * @param userID The identifier of the user to retrieve the default calendar identifier for
     * @return The default calendar folder identifier
     */
    protected String getDefaultCalendarID(int userID) throws OXException {
        return session.getConfig().getDefaultFolderID(userID);
    }

}
