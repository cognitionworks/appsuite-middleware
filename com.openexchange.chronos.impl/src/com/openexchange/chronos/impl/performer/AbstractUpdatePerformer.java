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
import static com.openexchange.chronos.common.CalendarUtils.getAlarmIDs;
import static com.openexchange.chronos.common.CalendarUtils.getFolderView;
import static com.openexchange.chronos.common.CalendarUtils.isGroupScheduled;
import static com.openexchange.chronos.common.CalendarUtils.isLastUserAttendee;
import static com.openexchange.chronos.common.CalendarUtils.isOrganizer;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.impl.Check.classificationAllowsUpdate;
import static com.openexchange.chronos.impl.Check.requireCalendarPermission;
import static com.openexchange.chronos.impl.Utils.getCalendarUser;
import static com.openexchange.chronos.impl.Utils.getChangeExceptionDates;
import static com.openexchange.chronos.impl.Utils.getSearchTerm;
import static com.openexchange.folderstorage.Permission.DELETE_ALL_OBJECTS;
import static com.openexchange.folderstorage.Permission.DELETE_OWN_OBJECTS;
import static com.openexchange.folderstorage.Permission.NO_PERMISSIONS;
import static com.openexchange.folderstorage.Permission.READ_FOLDER;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.AlarmField;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Classification;
import com.openexchange.chronos.DelegatingEvent;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.UnmodifiableEvent;
import com.openexchange.chronos.common.AlarmUtils;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.mapping.AlarmMapper;
import com.openexchange.chronos.common.mapping.EventMapper;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.Check;
import com.openexchange.chronos.impl.Consistency;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.CollectionUpdate;
import com.openexchange.chronos.service.ItemUpdate;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.java.Autoboxing;
import com.openexchange.java.Strings;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SingleSearchTerm.SingleOperation;

/**
 * {@link AbstractUpdatePerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public abstract class AbstractUpdatePerformer extends AbstractQueryPerformer {

    protected final CalendarUser calendarUser;
    protected final int calendarUserId;
    protected final UserizedFolder folder;
    protected final Date timestamp;

    protected final ResultTracker resultTracker;

    /**
     * Initializes a new {@link AbstractUpdatePerformer}.
     *
     * @param storage The underlying calendar storage
     * @param session The calendar session
     * @param folder The calendar folder representing the current view on the events
     */
    protected AbstractUpdatePerformer(CalendarStorage storage, CalendarSession session, UserizedFolder folder) throws OXException {
        super(session, storage);
        this.folder = folder;
        this.calendarUser = getCalendarUser(session, folder);
        this.calendarUserId = calendarUser.getEntity();
        this.timestamp = new Date();
        this.resultTracker = new ResultTracker(storage, session, folder, timestamp.getTime(), getSelfProtection());
    }

    /**
     * Prepares a new change exception for a recurring event series.
     *
     * @param originalMasterEvent The original master event
     * @param recurrenceID The recurrence identifier
     * @return The prepared exception event
     */
    protected Event prepareException(Event originalMasterEvent, RecurrenceId recurrenceID) throws OXException {
        Event exceptionEvent = EventMapper.getInstance().copy(originalMasterEvent, new Event(), true, (EventField[]) null);
        exceptionEvent.setId(storage.getEventStorage().nextId());
        exceptionEvent.setRecurrenceId(recurrenceID);
        exceptionEvent.setDeleteExceptionDates(null);
        exceptionEvent.setStartDate(CalendarUtils.calculateStart(originalMasterEvent, recurrenceID));
        exceptionEvent.setEndDate(CalendarUtils.calculateEnd(originalMasterEvent, recurrenceID));
        Consistency.setCreated(timestamp, exceptionEvent, originalMasterEvent.getCreatedBy());
        Consistency.setModified(session, timestamp, exceptionEvent, session.getUserId());
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
        Consistency.setModified(session, timestamp, eventUpdate, session.getUserId());
        storage.getEventStorage().updateEvent(eventUpdate);
    }

    /**
     * Deletes a single event from the storage. This can be used for any kind of event, i.e. a single, non-recurring event, an existing
     * exception of an event series, or an event series. For the latter one, any existing event exceptions are deleted as well.
     * <p/>
     * The event's attendees are loaded on demand if not yet present in the passed <code>originalEvent</code> {@code originalEvent}.
     * <p/>
     * The deletion includes:
     * <ul>
     * <ul>insertion of a <i>tombstone</i> record for the original event</ul>
     * <ul>insertion of <i>tombstone</i> records for the original event's attendees</ul>
     * <ul>deletion of any alarms associated with the event</ul>
     * <ul>deletion of any attachments associated with the event</ul>
     * <ul>deletion of the event</ul>
     * <ul>deletion of the event's attendees</ul>
     * </ul>
     *
     * @param originalEvent The original event to delete
     */
    protected void delete(Event originalEvent) throws OXException {
        /*
         * recursively delete any existing event exceptions
         */
        if (isSeriesMaster(originalEvent)) {
            for (Event changeException : loadExceptionData(originalEvent.getSeriesId())) {
                delete(changeException);
            }
        }
        /*
         * delete event data from storage
         */
        String id = originalEvent.getId();
        Event tombstone = storage.getUtilities().getTombstone(originalEvent, timestamp, calendarUser);
        tombstone.setAttendees(storage.getUtilities().getTombstones(originalEvent.getAttendees()));
        storage.getEventStorage().insertEventTombstone(tombstone);
        storage.getAttendeeStorage().insertAttendeeTombstones(id, tombstone.getAttendees());
        storage.getAlarmStorage().deleteAlarms(id);
        storage.getAttachmentStorage().deleteAttachments(session.getSession(), folder.getID(), id, originalEvent.getAttachments());
        storage.getEventStorage().deleteEvent(id);
        storage.getAttendeeStorage().deleteAttendees(id);
        storage.getAlarmTriggerStorage().deleteTriggers(id);
        /*
         * track deletion in result
         */
        resultTracker.trackDeletion(originalEvent);
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
        int userId = originalAttendee.getEntity();
        if (isSeriesMaster(originalEvent)) {
            deleteExceptions(originalEvent.getSeriesId(), getChangeExceptionDates(storage, originalEvent.getSeriesId()), userId);
        }
        /*
         * delete event data from storage for this attendee
         */
        String id = originalEvent.getId();
        Event tombstone = storage.getUtilities().getTombstone(originalEvent, timestamp, calendarUser);
        tombstone.setAttendees(Collections.singletonList(storage.getUtilities().getTombstone(originalAttendee)));
        storage.getEventStorage().insertEventTombstone(tombstone);
        storage.getAttendeeStorage().insertAttendeeTombstones(id, originalEvent.getAttendees());
        storage.getAttendeeStorage().deleteAttendees(id, Collections.singletonList(originalAttendee));
        storage.getAlarmStorage().deleteAlarms(id, userId);

        /*
         * 'touch' event & track calendar results
         */
        touch(id);
        Event updatedEvent = loadEventData(id);
        resultTracker.trackUpdate(originalEvent, updatedEvent);

        // Update alarm trigger
        storage.getAlarmTriggerStorage().deleteTriggers(updatedEvent.getId());
        Set<RecurrenceId> exceptions = null;
        if (isSeriesMaster(originalEvent)) {
            exceptions = getChangeExceptionDates(storage, originalEvent.getSeriesId());
            if (originalEvent.getDeleteExceptionDates() != null) {
                exceptions.addAll(originalEvent.getDeleteExceptionDates());
            }
        }
        storage.getAlarmTriggerStorage().insertTriggers(updatedEvent, exceptions);
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
     * @param recurrenceId The recurrence identifier of the occurrence to remove the attendee for
     * @param originalAttendee The original attendee to delete from the recurrence
     */
    protected void deleteFromRecurrence(Event originalMasterEvent, RecurrenceId recurrenceId, Attendee originalAttendee) throws OXException {
        /*
         * check if quota is exceeded before inserting new events
         */
        Check.quotaNotExceeded(storage, session);
        /*
         * prepare & insert a new plain exception
         */
        Event exceptionEvent = prepareException(originalMasterEvent, recurrenceId);
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
            int userId = entry.getKey().intValue();
            if (userId != originalAttendee.getEntity()) {
                insertAlarms(exceptionEvent, userId, entry.getValue(), true);
            }
        }
        /*
         * take over all original attachments
         */
        storage.getAttachmentStorage().insertAttachments(session.getSession(), folder.getID(), exceptionEvent.getId(), originalMasterEvent.getAttachments());
        /*
         * touch series master event
         */
        touch(originalMasterEvent.getId());
        /*
         * track results
         */
        Event createdException = loadEventData(exceptionEvent.getId());
        Event updatedMasterEvent = loadEventData(originalMasterEvent.getId());
        removeAlarmTrigger(createdException, updatedMasterEvent);
        resultTracker.trackCreation(createdException);
        resultTracker.trackUpdate(originalMasterEvent, updatedMasterEvent);
    }

    private void removeAlarmTrigger(Event createdException, Event updatedMasterEvent) throws OXException {

        storage.getAlarmTriggerStorage().insertTriggers(createdException, null);


        Set<RecurrenceId> exceptions = getChangeExceptionDates(storage, updatedMasterEvent.getSeriesId());
        if (updatedMasterEvent.getDeleteExceptionDates() != null) {
            exceptions.addAll(updatedMasterEvent.getDeleteExceptionDates());
        }
        storage.getAlarmTriggerStorage().deleteTriggers(updatedMasterEvent.getId());
        storage.getAlarmTriggerStorage().insertTriggers(updatedMasterEvent, exceptions);
    }

    /**
     * Inserts alarm data for an event of a specific user, optionally assigning new alarm UIDs in case the alarms are copied over from
     * another event. A new unique alarm identifier is always assigned, and the event is passed from the calendar user's folder view to the
     * storage implicitly (based on {@link Utils#getFolderView}.
     *
     * @param event The event the alarms are associated with
     * @param userId The identifier of the user the alarms should be inserted for
     * @param alarms The alarms to insert
     * @param forceNewUids <code>true</code> if new UIDs should be assigned even if already set in the supplied alarms, <code>false</code>, otherwise
     */
    protected void insertAlarms(Event event, int userId, List<Alarm> alarms, boolean forceNewUids) throws OXException {
        if (null == alarms || 0 == alarms.size()) {
            return;
        }
        List<Alarm> newAlarms = new ArrayList<Alarm>(alarms.size());
        for (Alarm alarm : alarms) {
            Alarm newAlarm = AlarmMapper.getInstance().copy(alarm, null, (AlarmField[]) null);
            newAlarm.setId(storage.getAlarmStorage().nextId());
            if (forceNewUids || false == newAlarm.containsUid() || Strings.isEmpty(newAlarm.getUid())) {
                newAlarm.setUid(UUID.randomUUID().toString());
            }
            newAlarms.add(newAlarm);
        }
        final String folderView = getFolderView(event, userId);
        if (false == folderView.equals(event.getFolderId())) {
            event = new DelegatingEvent(event) {

                @Override
                public String getFolderId() {
                    return folderView;
                }

                @Override
                public boolean containsFolderId() {
                    return true;
                }
            };
        }
        storage.getAlarmStorage().insertAlarms(event, userId, newAlarms);
    }

    /**
     * Updates a calendar user's alarms for a specific event.
     *
     * @param event The event to update the alarms in
     * @param userId The identifier of the calendar user whose alarms are updated
     * @param originalAlarms The original alarms, or <code>null</code> if there are none
     * @param updatedAlarms The updated alarms
     * @return <code>true</code> if there were any updates, <code>false</code>, otherwise
     */
    protected boolean updateAlarms(Event event, int userId, List<Alarm> originalAlarms, List<Alarm> updatedAlarms) throws OXException {
        CollectionUpdate<Alarm, AlarmField> alarmUpdates = AlarmUtils.getAlarmUpdates(originalAlarms, updatedAlarms);
        if (alarmUpdates.isEmpty()) {
            return false;
        }
        //        requireWritePermissions(event);
        /*
         * delete removed alarms
         */
        List<Alarm> removedItems = alarmUpdates.getRemovedItems();
        if (0 < removedItems.size()) {
            storage.getAlarmStorage().deleteAlarms(event.getId(), userId, getAlarmIDs(removedItems));
        }
        /*
         * save updated alarms
         */
        List<? extends ItemUpdate<Alarm, AlarmField>> updatedItems = alarmUpdates.getUpdatedItems();
        if (0 < updatedItems.size()) {
            List<Alarm> alarms = new ArrayList<Alarm>(updatedItems.size());
            for (ItemUpdate<Alarm, AlarmField> itemUpdate : updatedItems) {
                Alarm alarm = AlarmMapper.getInstance().copy(itemUpdate.getOriginal(), null, (AlarmField[]) null);
                AlarmMapper.getInstance().copy(itemUpdate.getUpdate(), alarm, AlarmField.values());
                alarm.setId(itemUpdate.getOriginal().getId());
                alarm.setUid(itemUpdate.getOriginal().getUid());
                alarms.add(Check.alarmIsValid(alarm));
            }
            final String folderView = getFolderView(event, userId);
            if (false == folderView.equals(event.getFolderId())) {
                Event userizedEvent = new DelegatingEvent(event) {

                    @Override
                    public String getFolderId() {
                        return folderView;
                    }

                    @Override
                    public boolean containsFolderId() {
                        return true;
                    }
                };
                storage.getAlarmStorage().updateAlarms(userizedEvent, userId, alarms);
            } else {
                storage.getAlarmStorage().updateAlarms(event, userId, alarms);
            }
        }
        /*
         * insert new alarms
         */
        insertAlarms(event, userId, alarmUpdates.getAddedItems(), false);
        storage.getAlarmTriggerStorage().deleteTriggers(event.getId());
        Set<RecurrenceId> exceptions = null;
        if (isSeriesMaster(event)) {
            exceptions = getChangeExceptionDates(storage, event.getSeriesId());
            if (event.getDeleteExceptionDates() != null) {
                exceptions.addAll(event.getDeleteExceptionDates());
            }
        }
        storage.getAlarmTriggerStorage().insertTriggers(event, exceptions);

        return true;
    }

    /**
     * Loads all non user-specific data for a specific event, including attendees and attachments.
     * <p/>
     * No <i>userization</i> of the event is performed and no alarm data is fetched for a specific attendee, i.e. only the plain/vanilla
     * event data is loaded from the storage.
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
        return new UnmodifiableEvent(storage.getUtilities().loadAdditionalEventData(-1, event, null));
    }

    /**
     * Loads all non user-specific data for multiple events, including attendees and attachments.
     * <p/>
     * No <i>userization</i> of the events is performed and no alarm data is fetched for a specific attendee, i.e. only the plain/vanilla
     * event data is loaded from the storage.
     *
     * @param ids The identifiers of the event to load
     * @return The event data
     * @throws OXException {@link CalendarExceptionCodes#EVENT_NOT_FOUND}
     */
    protected List<Event> loadEventData(List<String> ids) throws OXException {
        if (null == ids || 0 == ids.size()) {
            return Collections.emptyList();
        }
        if (1 == ids.size()) {
            return Collections.singletonList(loadEventData(ids.get(0)));
        }
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.OR);
        for (String id : ids) {
            searchTerm.addSearchTerm(getSearchTerm(EventField.ID, SingleOperation.EQUALS, id));
        }

        List<Event> foundEvents = storage.getEventStorage().searchEvents(searchTerm, null, null);
        foundEvents = storage.getUtilities().loadAdditionalEventData(-1, foundEvents, null);
        List<Event> events = new ArrayList<Event>(ids.size());
        for (String id : ids) {
            Event event = find(foundEvents, id);
            if (null == event) {
                throw CalendarExceptionCodes.EVENT_NOT_FOUND.create(id);
            }
            events.add(new UnmodifiableEvent(event));
        }
        return events;
    }

    /**
     * Loads all non user-specific data for a all exceptions of an event series, including attendees and attachments.
     * <p/>
     * No <i>userization</i> of the exception events is performed and no alarm data is fetched for a specific attendee, i.e. only the
     * plain/vanilla event data is loaded from the storage.
     *
     * @param seriesId The identifier of the event series to load the exceptions from
     * @return The event exception data
     */
    protected List<Event> loadExceptionData(String seriesId) throws OXException {
        List<Event> exceptions = storage.getEventStorage().loadExceptions(seriesId, null);
        if (0 < exceptions.size()) {
            exceptions = storage.getUtilities().loadAdditionalEventData(-1, exceptions, null);
            List<Event> unmodifiableExceptions = new ArrayList<Event>(exceptions.size());
            for (Event exception : exceptions) {
                unmodifiableExceptions.add(new UnmodifiableEvent(exception));
            }
            exceptions = unmodifiableExceptions;
        }
        return exceptions;
    }

    /**
     * Loads all non user-specific data for a collection of exceptions of an event series, including attendees and attachments.
     * <p/>
     * No <i>userization</i> of the exception events is performed and no alarm data is fetched for a specific attendee, i.e. only the
     * plain/vanilla event data is loaded from the storage.
     *
     * @param seriesId The identifier of the event series to load the exceptions from
     * @param recurrenceIds The recurrence identifiers of the exceptions to load
     * @return The event exception data
     * @throws OXException {@link CalendarExceptionCodes#EVENT_RECURRENCE_NOT_FOUND}
     */
    protected List<Event> loadExceptionData(String seriesId, Collection<RecurrenceId> recurrenceIds) throws OXException {
        List<Event> exceptions = new ArrayList<Event>();
        if (null != recurrenceIds && 0 < recurrenceIds.size()) {
            for (RecurrenceId recurrenceId : recurrenceIds) {
                Event exception = storage.getEventStorage().loadException(seriesId, recurrenceId, null);
                if (null == exception) {
                    throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(seriesId, String.valueOf(recurrenceId));
                }
                exceptions.add(exception);
            }
        }
        return storage.getUtilities().loadAdditionalEventData(-1, exceptions, null);
    }

    /**
     * Optionally loads all non user-specific data for a specific exception of an event series, including attendees and attachments.
     * <p/>
     * No <i>userization</i> of the exception event is performed and no alarm data is fetched for a specific attendee, i.e. only the
     * plain/vanilla event data is loaded from the storage.
     *
     * @param seriesId The identifier of the event series to load the exception from
     * @param recurrenceId The recurrence identifier of the exception to load
     * @return The event exception data, or <code>null</code> if not found
     */
    protected Event optExceptionData(String seriesId, RecurrenceId recurrenceId) throws OXException {
        Event changeException = storage.getEventStorage().loadException(seriesId, recurrenceId, null);
        if (null != changeException) {
            changeException = storage.getUtilities().loadAdditionalEventData(-1, changeException, null);
        }
        return changeException;
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

    /**
     * Prepares the organizer for an event, taking over an external organizer if specified.
     *
     * @param organizerData The organizer as defined by the client, or <code>null</code> to prepare the current calendar user as organizer
     * @return The prepared organizer
     */
    protected Organizer prepareOrganizer(Organizer organizerData) throws OXException {
        Organizer organizer;
        if (null != organizerData) {
            organizer = session.getEntityResolver().prepare(organizerData, CalendarUserType.INDIVIDUAL);
            if (0 < organizer.getEntity()) {
                /*
                 * internal organizer must match the actual calendar user if specified
                 */
                if (organizer.getEntity() != calendarUserId) {
                    throw CalendarExceptionCodes.INVALID_CALENDAR_USER.create(organizer.getUri(), Autoboxing.I(organizer.getEntity()), CalendarUserType.INDIVIDUAL);
                }
            } else {
                /*
                 * take over external organizer as-is
                 */
                return session.getConfig().isSkipExternalAttendeeURIChecks() ? organizer : Check.requireValidEMail(organizer);
            }
        } else {
            /*
             * prepare a default organizer for calendar user
             */
            organizer = session.getEntityResolver().applyEntityData(new Organizer(), calendarUserId);
        }
        /*
         * apply "sent-by" property if someone is acting on behalf of the calendar user
         */
        if (null != organizer && calendarUserId != session.getUserId()) {
            organizer.setSentBy(session.getEntityResolver().applyEntityData(new CalendarUser(), session.getUserId()));
        }
        return organizer;
    }

    /**
     * Creates a <i>userized</i> version of an event, representing the current calendar user's point of view on the event data, as derived
     * via {@link #calendarUserId}. This includes
     * <ul>
     * <li><i>anonymization</i> of restricted event data in case the event it is not marked as {@link Classification#PUBLIC}, and the
     * current session's user is neither creator, nor attendee of the event.</li>
     * <li>selecting the appropriate parent folder identifier for the specific user</li>
     * <li>apply <i>userized</i> versions of change- and delete-exception dates in the series master event based on the user's actual
     * attendance</li>
     * <li>taking over the user's personal list of alarms for the event</li>
     * </ul>
     *
     * @param event The event to userize from the current calendar user's point of view
     * @return The <i>userized</i> event
     * @see Utils#applyExceptionDates
     * @see Utils#anonymizeIfNeeded
     */
    protected Event userize(Event event) throws OXException {
        return userize(event, calendarUserId);
    }

    /**
     * Creates a <i>userized</i> version of an event, representing the current calendar user's point of view on the event data, as derived
     * via {@link #calendarUserId}. This includes
     * <ul>
     * <li><i>anonymization</i> of restricted event data in case the event it is not marked as {@link Classification#PUBLIC}, and the
     * current session's user is neither creator, nor attendee of the event.</li>
     * <li>selecting the appropriate parent folder identifier for the specific user</li>
     * <li>apply <i>userized</i> versions of change- and delete-exception dates in the series master event based on the user's actual
     * attendance</li>
     * <li>taking over the user's personal list of alarm for the event</li>
     * </ul>
     *
     * @param event The event to userize from the current calendar user's point of view
     * @param alarms The alarms to take over
     * @return The <i>userized</i> event
     * @see Utils#applyExceptionDates
     * @see Utils#anonymizeIfNeeded
     */
    protected Event userize(Event event, List<Alarm> alarms) throws OXException {
        event = userize(event, calendarUserId);
        if (null == alarms && null != event.getAlarms() || null != alarms && null == event.getAlarms()) {
            final List<Alarm> userizedAlarms = alarms;
            event = new DelegatingEvent(event) {

                @Override
                public List<Alarm> getAlarms() {
                    return userizedAlarms;
                }

                @Override
                public boolean containsAlarms() {
                    return true;
                }
            };
        }
        return event;
    }

    /**
     * Checks that the current session's user is able to delete a specific event, by either requiring delete access for <i>own</i> or
     * <i>all</i> objects, based on the user being the creator of the event or not.
     * <p/>
     * Additionally, the event's classification is checked.
     *
     * @param originalEvent The event to check the user's delete permissions for
     * @throws OXException {@link CalendarExceptionCodes#NO_DELETE_PERMISSION}, {@link CalendarExceptionCodes#RESTRICTED_BY_CLASSIFICATION}
     * @see Check#requireCalendarPermission
     * @see Check#classificationAllowsUpdate
     */
    protected void requireDeletePermissions(Event originalEvent) throws OXException {
        if (matches(originalEvent.getCreatedBy(), session.getUserId())) {
            requireCalendarPermission(folder, READ_FOLDER, NO_PERMISSIONS, NO_PERMISSIONS, DELETE_OWN_OBJECTS);
        } else {
            requireCalendarPermission(folder, READ_FOLDER, NO_PERMISSIONS, NO_PERMISSIONS, DELETE_ALL_OBJECTS);
        }
        classificationAllowsUpdate(folder, originalEvent);
    }

    /**
     * Gets a value indicating whether a delete operation performed in the current folder from the calendar user's perspective would lead
     * to a <i>real</i> deletion of the event from the storage, or if only the calendar user is removed from the attendee list, hence
     * rather an update is performed.
     * <p/>
     * A deletion leads to a complete removal if
     * <ul>
     * <li>the event is located in a <i>public folder</i></li>
     * <li>or the event is not <i>group-scheduled</i></li>
     * <li>or the calendar user is the organizer of the event</li>
     * <li>or the calendar user is the last internal user attendee in the event</li>
     * </ul>
     *
     * @param originalEvent The original event to check
     * @return <code>true</code> if a deletion would lead to a removal of the event, <code>false</code>, otherwise
     */
    protected boolean deleteRemovesEvent(Event originalEvent) {
        return PublicType.getInstance().equals(folder.getType()) ||
            false == isGroupScheduled(originalEvent) ||
            isOrganizer(originalEvent, calendarUserId) ||
            isLastUserAttendee(originalEvent.getAttendees(), calendarUserId)
        ;
    }

}
