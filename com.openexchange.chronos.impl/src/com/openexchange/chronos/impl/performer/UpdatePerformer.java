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

import static com.openexchange.chronos.common.CalendarUtils.getUserIDs;
import static com.openexchange.chronos.common.CalendarUtils.hasExternalOrganizer;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesException;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.impl.Check.requireCalendarPermission;
import static com.openexchange.chronos.impl.Check.requireUpToDateTimestamp;
import static com.openexchange.chronos.impl.Utils.i;
import static com.openexchange.folderstorage.Permission.NO_PERMISSIONS;
import static com.openexchange.folderstorage.Permission.READ_ALL_OBJECTS;
import static com.openexchange.folderstorage.Permission.READ_FOLDER;
import static com.openexchange.folderstorage.Permission.READ_OWN_OBJECTS;
import static com.openexchange.folderstorage.Permission.WRITE_ALL_OBJECTS;
import static com.openexchange.folderstorage.Permission.WRITE_OWN_OBJECTS;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.tools.arrays.Collections.isNullOrEmpty;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.AlarmField;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.Transp;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.AlarmMapper;
import com.openexchange.chronos.impl.AttendeeHelper;
import com.openexchange.chronos.impl.AttendeeMapper;
import com.openexchange.chronos.impl.CalendarResultImpl;
import com.openexchange.chronos.impl.Check;
import com.openexchange.chronos.impl.Consistency;
import com.openexchange.chronos.impl.CreateResultImpl;
import com.openexchange.chronos.impl.DefaultItemUpdate;
import com.openexchange.chronos.impl.EventMapper;
import com.openexchange.chronos.impl.UpdateResultImpl;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.CollectionUpdate;
import com.openexchange.chronos.service.EventConflict;
import com.openexchange.chronos.service.ItemUpdate;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.groupware.ldap.User;

/**
 * {@link UpdatePerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class UpdatePerformer extends AbstractUpdatePerformer {

    /**
     * Initializes a new {@link UpdatePerformer}.
     *
     * @param storage The underlying calendar storage
     * @param session The calendar session
     * @param folder The calendar folder representing the current view on the events
     */
    public UpdatePerformer(CalendarStorage storage, CalendarSession session, UserizedFolder folder) throws OXException {
        super(storage, session, folder);
    }

    public CalendarResultImpl perform(int objectID, Event updatedEvent, long clientTimestamp) throws OXException {
        /*
         * load original event data
         */
        Event originalEvent = loadEventData(objectID);
        requireUpToDateTimestamp(originalEvent, clientTimestamp);
        /*
         * update event or event occurrence
         */
        if (CalendarUtils.isSeriesMaster(originalEvent) && updatedEvent.containsRecurrenceId() && null != updatedEvent.getRecurrenceId()) {
            updateEvent(originalEvent, updatedEvent, updatedEvent.getRecurrenceId());
        } else {
            updateEvent(originalEvent, updatedEvent);
        }
        return result;
    }

    private void updateEvent(Event originalEvent, Event updatedEvent, RecurrenceId recurrenceID) throws OXException {
        if (isSeriesMaster(originalEvent)) {
            if (null != originalEvent.getDeleteExceptionDates() && originalEvent.getDeleteExceptionDates().contains(new Date(recurrenceID.getValue()))) {
                throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(I(originalEvent.getSeriesId()), recurrenceID);
            }
            if (null != originalEvent.getChangeExceptionDates() && originalEvent.getChangeExceptionDates().contains(new Date(recurrenceID.getValue()))) {
                /*
                 * update for existing change exception
                 */
                Event originalExceptionEvent = loadExceptionData(originalEvent.getId(), recurrenceID);
                updateEvent(originalExceptionEvent, updatedEvent);
            } else {
                /*
                 * update for new change exception, prepare & insert a plain exception first
                 */
                Event newExceptionEvent = prepareException(originalEvent, Check.recurrenceIdExists(originalEvent, recurrenceID));
                storage.getEventStorage().insertEvent(newExceptionEvent);
                /*
                 * take over all original attendees & alarms
                 */
                List<Attendee> excpetionAttendees = new ArrayList<Attendee>(originalEvent.getAttendees());
                storage.getAttendeeStorage().insertAttendees(newExceptionEvent.getId(), excpetionAttendees);
                /*
                 * take over all original alarms
                 */
                for (Entry<Integer, List<Alarm>> entry : storage.getAlarmStorage().loadAlarms(originalEvent).entrySet()) {
                    storage.getAlarmStorage().insertAlarms(newExceptionEvent, entry.getKey().intValue(), entry.getValue());
                }
                /*
                 * reload the newly created exception as 'original' & perform the update
                 * - recurrence rule is forcibly ignored during update to satisfy UsmFailureDuringRecurrenceTest.testShouldFailWhenTryingToMakeAChangeExceptionASeriesButDoesNot()
                 * - sequence number is also ignored (since possibly incremented implicitly before)
                 */
                newExceptionEvent = loadEventData(newExceptionEvent.getId());
                updateEvent(newExceptionEvent, updatedEvent, EventField.RECURRENCE_RULE, EventField.SEQUENCE);
                addChangeExceptionDate(originalEvent, recurrenceID);
                result.addCreation(new CreateResultImpl(loadEventData(newExceptionEvent.getId())));
            }
        } else if (isSeriesException(originalEvent)) {
            /*
             * update for existing change exception
             */
            updateEvent(originalEvent, updatedEvent);
        } else {
            /*
             * unsupported, otherwise
             */
            throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(I(originalEvent.getId()), String.valueOf(recurrenceID));
        }
    }

    private void updateEvent(Event originalEvent, Event updatedEvent, EventField... ignoredFields) throws OXException {
        /*
         * check current session user's permissions
         */
        if (needsExistenceCheckInTargetFolder(originalEvent, updatedEvent)) {
            Check.eventIsInFolder(originalEvent, folder);
        }
        if (session.getUser().getId() == originalEvent.getCreatedBy()) {
            requireCalendarPermission(folder, READ_FOLDER, READ_OWN_OBJECTS, WRITE_OWN_OBJECTS, NO_PERMISSIONS);
        } else {
            requireCalendarPermission(folder, READ_FOLDER, READ_ALL_OBJECTS, WRITE_ALL_OBJECTS, NO_PERMISSIONS);
        }
        /*
         * update event data
         */
        boolean wasUpdated = false;
        ItemUpdate<Event, EventField> eventUpdate = prepareEventUpdate(originalEvent, updatedEvent, ignoredFields);
        if (null != eventUpdate) {
            /*
             * check for conflicts
             */
            if (needsConflictCheck(eventUpdate)) {
                Event newEvent = originalEvent.clone();
                EventMapper.getInstance().copy(eventUpdate.getUpdate(), newEvent, EventField.values());
                List<Attendee> newAttendees;
                if (updatedEvent.containsAttendees()) {
                    newAttendees = AttendeeHelper.onUpdatedEvent(session, folder, originalEvent.getAttendees(), updatedEvent.getAttendees()).apply(originalEvent.getAttendees());
                } else {
                    newAttendees = originalEvent.getAttendees();
                }
                List<EventConflict> conflicts = new ConflictCheckPerformer(session, storage).perform(newEvent, newAttendees);
                if (null != conflicts && 0 < conflicts.size()) {
                    result.addConflicts(conflicts);
                    return;
                }
            }
            /*
             * perform update
             */
            Consistency.setModified(timestamp, eventUpdate.getUpdate(), session.getUser().getId());
            if (needsSequenceNumberIncrement(eventUpdate)) {
                eventUpdate.getUpdate().setSequence(originalEvent.getSequence() + 1);
            }
            if (isSeriesMaster(originalEvent) && needsChangeExceptionsReset(eventUpdate)) {
                /*
                 * reset change & delete exceptions
                 */
                eventUpdate.getUpdate().setDeleteExceptionDates(null);
                eventUpdate.getUpdate().setChangeExceptionDates(null);
                deleteExceptions(originalEvent.getSeriesId(), originalEvent.getChangeExceptionDates());
            }
            storage.getEventStorage().updateEvent(eventUpdate.getUpdate());
            wasUpdated = true;
        }
        /*
         * process any attendee updates
         */
        if (updatedEvent.containsAttendees()) {
            updateAttendees(originalEvent, updatedEvent);
            wasUpdated |= true;
        } else if (null != eventUpdate && needsParticipationStatusReset(eventUpdate)) {
            wasUpdated |= resetParticipationStatus(originalEvent.getId(), originalEvent.getAttendees());
        }
        /*
         * process any alarm updates for the calendar user
         */
        CollectionUpdate<Alarm, AlarmField> alarmUpdate = null;
        if (updatedEvent.containsAlarms()) {
            alarmUpdate = updateAlarms(originalEvent, calendarUser, updatedEvent.getAlarms());
            wasUpdated |= false == alarmUpdate.isEmpty();
        }
        /*
         * update any stored alarm triggers of all users if required
         */
        if (null != eventUpdate && needsAlarmTriggerUpdate(eventUpdate)) {
            Event changedEvent = storage.getEventStorage().loadEvent(originalEvent.getId(), null);
            storage.getAlarmStorage().updateAlarms(changedEvent);
        }
        if (wasUpdated) {
            UpdateResultImpl updateResult = new UpdateResultImpl(originalEvent, i(folder), loadEventData(originalEvent.getId()));
            if (null != alarmUpdate && false == alarmUpdate.isEmpty()) {
                updateResult.setAlarmUpdates(alarmUpdate);
            }
            result.addUpdate(updateResult);
        }
    }


    /**
     * Determines if it's allowed to skip the check if the updated event already exists in the targeted folder or not.
     * <p/>
     * The skip may be checked under certain circumstances, particularly:
     * <ul>
     * <li>the event has an <i>external</i> organizer</li>
     * <li>the organizer matches in the original and in the updated event</li>
     * <li>the unique identifier matches in the original and in the updated event</li>
     * <li>the updated event's sequence number is not smaller than the sequence number of the original event</li>
     * </ul>
     *
     * @param originalEvent The original event
     * @param updatedEvent The updated event
     * @return <code>true</code> if the check should be performed, <code>false</code>, otherwise
     * @see <a href="https://bugs.open-xchange.com/show_bug.cgi?id=29566#c12">Bug 29566</a>, <a href="https://bugs.open-xchange.com/show_bug.cgi?id=23181"/>Bug 23181</a>
     */
    public boolean needsExistenceCheckInTargetFolder(Event originalEvent, Event updatedEvent) {
        if (hasExternalOrganizer(originalEvent) && matches(originalEvent.getOrganizer(), updatedEvent.getOrganizer()) &&
            originalEvent.getUid().equals(updatedEvent.getUid()) && updatedEvent.getSequence() >= originalEvent.getSequence()) {
            return false;
        }
        return true;
    }

    /**
     * Gets a value indicating whether a recurring master event's change exceptions should be reset along with the update or not.
     *
     * @param eventUpdate The event update to evaluate
     * @return <code>true</code> if the change exceptions should be reseted, <code>false</code>, otherwise
     */
    private boolean needsChangeExceptionsReset(ItemUpdate<Event, EventField> eventUpdate) throws OXException {
        if (eventUpdate.containsAnyChangeOf(new EventField[] {
            EventField.RECURRENCE_ID, EventField.RECURRENCE_RULE, EventField.START_DATE, EventField.END_DATE, EventField.START_TIMEZONE, EventField.END_TIMEZONE, EventField.ALL_DAY
        })) {
            return true;
        }
        if (eventUpdate.getUpdatedFields().contains(EventField.RECURRENCE_RULE) && null == eventUpdate.getUpdate().getRecurrenceRule()) {
            return true;
        }
        return false;
    }

    /**
     * Gets a value indicating whether the participation status of the event's attendees needs to be reset along with the update or not.
     *
     * @param eventUpdate The event update to evaluate
     * @return <code>true</code> if the attendee's participation status should be reseted, <code>false</code>, otherwise
     */
    private boolean needsParticipationStatusReset(ItemUpdate<Event, EventField> eventUpdate) throws OXException {
        return eventUpdate.containsAnyChangeOf(new EventField[] {
            EventField.RECURRENCE_RULE, EventField.START_DATE, EventField.END_DATE, EventField.START_TIMEZONE, EventField.END_TIMEZONE, EventField.ALL_DAY
        });
    }

    /**
     * Gets a value indicating whether conflict checks should take place along with the update or not.
     *
     * @param eventUpdate The event update to evaluate
     * @return <code>true</code> if conflict checks should take place, <code>false</code>, otherwise
     */
    private boolean needsConflictCheck(ItemUpdate<Event, EventField> eventUpdate) throws OXException {
        if (eventUpdate.containsAnyChangeOf(new EventField[] {
            EventField.RECURRENCE_RULE, EventField.START_DATE, EventField.END_DATE, EventField.START_TIMEZONE, EventField.END_TIMEZONE, EventField.ALL_DAY
        })) {
            return true;
        }
        if (eventUpdate.getUpdatedFields().contains(EventField.TRANSP) && Transp.TRANSPARENT.equals(eventUpdate.getOriginal().getTransp().getValue())) {
            return true;
        }
        AttendeeHelper attendeeHelper = AttendeeHelper.onUpdatedEvent(session, folder, eventUpdate.getOriginal().getAttendees(), eventUpdate.getUpdate().getAttendees());
        if (0 < attendeeHelper.getAttendeesToInsert().size()) {
            return true;
        }
        return false;
    }

    /**
     * Gets a value indicating whether the event's sequence number ought to be incremented along with the update or not.
     *
     * @param eventUpdate The event update to evaluate
     * @return <code>true</code> if the event's sequence number should be updated, <code>false</code>, otherwise
     */
    private boolean needsSequenceNumberIncrement(ItemUpdate<Event, EventField> eventUpdate) throws OXException {
        if (eventUpdate.containsAnyChangeOf(new EventField[] {
            EventField.SUMMARY, EventField.LOCATION, EventField.RECURRENCE_RULE, EventField.START_DATE, EventField.END_DATE, EventField.START_TIMEZONE, EventField.END_TIMEZONE, EventField.ALL_DAY
        })) {
            return true;
        }
        AttendeeHelper attendeeHelper = AttendeeHelper.onUpdatedEvent(session, folder, eventUpdate.getOriginal().getAttendees(), eventUpdate.getUpdate().getAttendees());
        if (0 < attendeeHelper.getAttendeesToDelete().size() || 0 < attendeeHelper.getAttendeesToInsert().size() || 0 < attendeeHelper.getAttendeesToUpdate().size()) {
            //TODO: more distinct evaluation of attendee updates
            return true;
        }
        return false;
    }

    private boolean needsAlarmTriggerUpdate(ItemUpdate<Event, EventField> eventUpdate) throws OXException {
        return eventUpdate.containsAnyChangeOf(new EventField[] {
            EventField.START_DATE, EventField.END_DATE, EventField.START_TIMEZONE, EventField.END_TIMEZONE, EventField.ALL_DAY
        });
    }

    private void updateAttendees(Event originalEvent, Event updatedEvent) throws OXException {
        AttendeeHelper attendeeHelper = AttendeeHelper.onUpdatedEvent(session, folder, originalEvent.getAttendees(), updatedEvent.getAttendees());
        /*
         * perform attendee deletions
         */
        List<Attendee> attendeesToDelete = attendeeHelper.getAttendeesToDelete();
        if (0 < attendeesToDelete.size()) {
            //TODO: any checks prior removal? user a must not delete user b?
            storage.getEventStorage().insertTombstoneEvent(EventMapper.getInstance().getTombstone(originalEvent, timestamp, calendarUser.getId()));
            storage.getAttendeeStorage().insertTombstoneAttendees(originalEvent.getId(), AttendeeMapper.getInstance().getTombstones(attendeesToDelete));
            storage.getAttendeeStorage().deleteAttendees(originalEvent.getId(), attendeesToDelete);
            storage.getAlarmStorage().deleteAlarms(originalEvent.getId(), getUserIDs(attendeesToDelete));
        }
        /*
         * perform attendee updates
         */
        List<Attendee> attendeesToUpdate = attendeeHelper.getAttendeesToUpdate();
        if (0 < attendeesToUpdate.size()) {
            //TODO: any checks prior removal? user a must not update user b?
            storage.getAttendeeStorage().updateAttendees(originalEvent.getId(), attendeesToUpdate);
        }
        /*
         * perform attendee inserts
         */
        if (0 < attendeeHelper.getAttendeesToInsert().size()) {
            //TODO: any checks prior removal? user a must not add user b if not organizer?
            storage.getAttendeeStorage().insertAttendees(originalEvent.getId(), attendeeHelper.getAttendeesToInsert());
        }
    }

    /**
     * Updates a calendar user's alarms for a specific event.
     *
     * @param event The event to update the alarms in
     * @param calendarUser The calendar user whose alarms are updated
     * @param updatedAlarms The updated alarms
     * @return A corresponding collection update
     */
    private CollectionUpdate<Alarm, AlarmField> updateAlarms(Event event, User calendarUser, List<Alarm> updatedAlarms) throws OXException {
        List<Alarm> originalAlarms = storage.getAlarmStorage().loadAlarms(event, calendarUser.getId());
        CollectionUpdate<Alarm, AlarmField> alarmUpdates = AlarmMapper.getInstance().getAlarmUpdate(originalAlarms, updatedAlarms);
        if (false == alarmUpdates.isEmpty()) {
            storage.getAlarmStorage().updateAlarms(event, calendarUser.getId(), updatedAlarms);
            List<Alarm> newAlarms = storage.getAlarmStorage().loadAlarms(event, calendarUser.getId());
            return AlarmMapper.getInstance().getAlarmUpdate(originalAlarms, newAlarms);
        }
        return alarmUpdates;
    }

    /**
     * Resets the participation status of all individual attendees - excluding the current calendar user - to
     * {@link ParticipationStatus#NEEDS_ACTION} for a specific event.
     *
     * @param objectID The identifier of the event to reste the participation status for
     * @param attendees The event's attendees
     * @return <code>true</code> if at least one attendee was updated, <code>false</code>, otherwise
     */
    private boolean resetParticipationStatus(int objectID, List<Attendee> attendees) throws OXException {
        List<Attendee> attendeesToUpdate = new ArrayList<Attendee>();
        for (Attendee attendee : CalendarUtils.filter(attendees, null, CalendarUserType.INDIVIDUAL)) {
            if (calendarUser.getId() == attendee.getEntity() || ParticipationStatus.NEEDS_ACTION.equals(attendee.getPartStat())) {
                continue;
            }
            Attendee attendeeUpdate = new Attendee();
            AttendeeMapper.getInstance().copy(attendee, attendeeUpdate, AttendeeField.ENTITY, AttendeeField.MEMBER, AttendeeField.CU_TYPE, AttendeeField.URI);
            attendeeUpdate.setPartStat(ParticipationStatus.NEEDS_ACTION); //TODO: or reset to initial partstat based on folder type?
            attendeesToUpdate.add(attendeeUpdate);
        }
        if (0 < attendeesToUpdate.size()) {
            storage.getAttendeeStorage().updateAttendees(objectID, attendeesToUpdate);
            return true;
        }
        return false;
    }

    private ItemUpdate<Event, EventField> prepareEventUpdate(Event originalEvent, Event updatedEvent, EventField... ignoredFields) throws OXException {
        /*
         * determine & check modified fields
         */
        Event eventUpdate = EventMapper.getInstance().getDifferences(originalEvent, updatedEvent, true, ignoredFields);
        EventField[] updatedFields = EventMapper.getInstance().getAssignedFields(eventUpdate);
        if (0 == updatedFields.length) {
            // TODO or throw?
            return null;
        }
        for (EventField field : EventMapper.getInstance().getAssignedFields(eventUpdate)) {
            switch (field) {
                case CLASSIFICATION:
                    Check.mandatoryFields(eventUpdate, EventField.CLASSIFICATION);
                    Check.classificationIsValid(eventUpdate.getClassification(), folder);
                    break;
                case ALL_DAY:
                    /*
                     * adjust start- and enddate, too, if required
                     */
                    EventMapper.getInstance().copyIfNotSet(originalEvent, eventUpdate, EventField.START_DATE, EventField.END_DATE);
                    Consistency.adjustAllDayDates(eventUpdate);
                    break;
                case RECURRENCE_RULE:
                    /*
                     * deny update for change exceptions (but ignore if set to 'null')
                     */
                    if (isSeriesException(originalEvent)) {
                        if (null == eventUpdate.getRecurrenceRule()) {
                            eventUpdate.removeRecurrenceRule();
                            break;
                        }
                        // TODO: better ignore? com.openexchange.ajax.appointment.recurrence.UsmFailureDuringRecurrenceTest.testShouldFailWhenTryingToMakeAChangeExceptionASeriesButDoesNot()
                        //       vs. com.openexchange.ajax.appointment.recurrence.TestsForModifyingChangeExceptions.testShouldNotAllowTurningAChangeExceptionIntoASeries()
                        throw CalendarExceptionCodes.FORBIDDEN_CHANGE.create(I(originalEvent.getId()), field);
                    }
                    if (isSeriesMaster(originalEvent) && null == eventUpdate.getRecurrenceRule()) {
                        /*
                         * series to single event, remove recurrence & ensure all necessary recurrence data is present in passed event update
                         */
                        EventMapper.getInstance().copyIfNotSet(originalEvent, eventUpdate, EventField.SERIES_ID, EventField.START_DATE, EventField.END_DATE, EventField.START_TIMEZONE, EventField.END_TIMEZONE, EventField.ALL_DAY);
                        eventUpdate.setSeriesId(0);
                        eventUpdate.setChangeExceptionDates(null);
                        eventUpdate.setDeleteExceptionDates(null);
                        break;
                    }
                    /*
                     * ensure all necessary recurrence related data is present in passed event update & check rule validity
                     */
                    EventMapper.getInstance().copyIfNotSet(originalEvent, eventUpdate, EventField.SERIES_ID, EventField.START_DATE, EventField.END_DATE, EventField.START_TIMEZONE, EventField.END_TIMEZONE, EventField.ALL_DAY);
                    Check.recurrenceRuleIsValid(eventUpdate);
                    /*
                     * single event to series, assign new recurrence id
                     */
                    if (0 >= originalEvent.getSeriesId()) {
                        eventUpdate.setSeriesId(originalEvent.getId());
                    }
                    break;
                case START_DATE:
                case END_DATE:
                    /*
                     * ensure all necessary recurrence related data is present in passed event update & check rule validity & re-validate start- and end date
                     */
                    EventMapper.getInstance().copyIfNotSet(originalEvent, eventUpdate, EventField.RECURRENCE_RULE, EventField.SERIES_ID, EventField.START_DATE, EventField.END_DATE, EventField.START_TIMEZONE, EventField.END_TIMEZONE, EventField.ALL_DAY);
                    Check.startAndEndDate(eventUpdate);
                    break;
                case RECURRENCE_ID:
                    if (false == isSeriesException(originalEvent) && null == eventUpdate.getRecurrenceId()) {
                        // ignore neutral value
                        break;
                    }
                    throw CalendarExceptionCodes.FORBIDDEN_CHANGE.create(I(originalEvent.getId()), field);
                case DELETE_EXCEPTION_DATES:
                    if (isNullOrEmpty(eventUpdate.getDeleteExceptionDates()) && isNullOrEmpty(originalEvent.getDeleteExceptionDates())) {
                        // ignore neutral value
                        break;
                    }
                    if (false == isSeriesMaster(originalEvent)) {
                        throw CalendarExceptionCodes.FORBIDDEN_CHANGE.create(I(originalEvent.getId()), field);
                    }
                    if (null != eventUpdate.getDeleteExceptionDates()) {
                        Check.recurrenceIdsExist(originalEvent, eventUpdate.getDeleteExceptionDates());
                    }
                    break;
                case CHANGE_EXCEPTION_DATES:
                    if (isNullOrEmpty(eventUpdate.getDeleteExceptionDates()) && isNullOrEmpty(originalEvent.getDeleteExceptionDates())) {
                        // ignore neutral value
                        break;
                    }
                    throw CalendarExceptionCodes.FORBIDDEN_CHANGE.create(I(originalEvent.getId()), field);
                case ORGANIZER:
                    Organizer organizer = eventUpdate.getOrganizer();
                    if (null == organizer) {
                        // ignore implicitly
                        eventUpdate.removeOrganizer();
                    } else {
                        organizer = session.getEntityResolver().prepare(organizer, CalendarUserType.INDIVIDUAL);
                        if (false == CalendarUtils.matches(originalEvent.getOrganizer(), organizer)) {
                            throw CalendarExceptionCodes.FORBIDDEN_CHANGE.create(I(originalEvent.getId()), field);
                        }
                        eventUpdate.removeOrganizer();
                    }
                    break;
                case CREATED:
                    // ignore implicitly
                    eventUpdate.removeCreated();
                    break;
                case CREATED_BY:
                    // ignore implicitly
                    eventUpdate.removeCreatedBy();
                    break;
                case SEQUENCE:
                    // ignore implicitly
                    eventUpdate.removeSequence();
                    break;
                case UID:
                case SERIES_ID:
                case PUBLIC_FOLDER_ID:
                    throw CalendarExceptionCodes.FORBIDDEN_CHANGE.create(I(originalEvent.getId()), field);
                default:
                    break;
            }
        }
        EventMapper.getInstance().copy(originalEvent, eventUpdate, EventField.ID);
        Consistency.setModified(timestamp, eventUpdate, session.getUser().getId());
        return new DefaultItemUpdate<Event, EventField>(EventMapper.getInstance(), originalEvent, eventUpdate);
    }

}
