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

import static com.openexchange.chronos.common.CalendarUtils.add;
import static com.openexchange.chronos.common.CalendarUtils.calculateEnd;
import static com.openexchange.chronos.common.CalendarUtils.calculateStart;
import static com.openexchange.chronos.common.CalendarUtils.combine;
import static com.openexchange.chronos.common.CalendarUtils.contains;
import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.getExceptionDateUpdates;
import static com.openexchange.chronos.common.CalendarUtils.initRecurrenceRule;
import static com.openexchange.chronos.common.CalendarUtils.isPublicClassification;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesException;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.common.CalendarUtils.shiftRecurrenceId;
import static com.openexchange.chronos.common.CalendarUtils.shiftRecurrenceIds;
import static com.openexchange.chronos.impl.Utils.asList;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.tools.arrays.Collections.isNullOrEmpty;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.AlarmField;
import com.openexchange.chronos.Attachment;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.AlarmUtils;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultRecurrenceData;
import com.openexchange.chronos.common.mapping.AttendeeMapper;
import com.openexchange.chronos.common.mapping.DefaultItemUpdate;
import com.openexchange.chronos.common.mapping.EventMapper;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.AttendeeHelper;
import com.openexchange.chronos.impl.Check;
import com.openexchange.chronos.impl.Consistency;
import com.openexchange.chronos.impl.DeltaEvent;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.CollectionUpdate;
import com.openexchange.chronos.service.EventUpdate;
import com.openexchange.chronos.service.ItemUpdate;
import com.openexchange.chronos.service.RecurrenceData;
import com.openexchange.chronos.service.SimpleCollectionUpdate;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.groupware.tools.mappings.Mapping;

/**
 * {@link EventUpdateProcessor}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class EventUpdateProcessor implements EventUpdate {

    private final CalendarSession session;
    private final CalendarUser calendarUser;
    private final UserizedFolder folder;

    private final AttendeeHelper attendeeUpdates;
    private final SimpleCollectionUpdate<Attachment> attachmentUpdates;
    private final CollectionUpdate<Alarm, AlarmField> alarmUpdates;
    private final ItemUpdate<Event, EventField> eventUpdate;
    private final Event deltaEvent;
    private final CollectionUpdate<Event, EventField> exceptionUpdates;

    /**
     * Initializes a new {@link EventUpdateProcessor}.
     *
     * @param session The calendar session
     * @param folder The folder the update operation is performed in
     * @param originalEvent The original event being updated
     * @param originalChangeExceptions The change exceptions of the original series event, or <code>null</code> if not applicable
     * @param updatedEvent The updated event, as passed by the client
     * @param timestamp The timestamp to apply in the updated event data
     * @param ignoredFields Additional fields to ignore during the update
     */
    public EventUpdateProcessor(CalendarSession session, UserizedFolder folder, Event originalEvent, List<Event> originalChangeExceptions, Event updatedEvent, Date timestamp, EventField... ignoredFields) throws OXException {
        super();
        this.session = session;
        this.folder = folder;
        this.calendarUser = Utils.getCalendarUser(session, folder);
        /*
         * apply, check, adjust event update as needed
         */
        Event changedEvent = apply(originalEvent, updatedEvent, ignoredFields);
        checkIntegrity(originalEvent, changedEvent);
        ensureConsistency(originalEvent, changedEvent, timestamp);
        List<Event> changedChangeExceptions = adjustExceptions(originalEvent, changedEvent, originalChangeExceptions);
        /*
         * derive & take over event update
         */
        Set<EventField> differentFields = EventMapper.getInstance().getDifferentFields(originalEvent, changedEvent, true);
        this.eventUpdate = new DefaultItemUpdate<Event, EventField>(originalEvent, changedEvent, differentFields);
        this.attendeeUpdates = AttendeeHelper.onUpdatedEvent(session, folder, originalEvent.getAttendees(), changedEvent.getAttendees());
        this.attachmentUpdates = CalendarUtils.getAttachmentUpdates(originalEvent.getAttachments(), changedEvent.getAttachments());
        this.alarmUpdates = AlarmUtils.getAlarmUpdates(originalEvent.getAlarms(), changedEvent.getAlarms());
        this.exceptionUpdates = CalendarUtils.getEventUpdates(originalChangeExceptions, changedChangeExceptions, EventField.ID);
        /*
         * generate special 'delta' event on top of the changed event data to indicate actual differences during storage update
         */
        this.deltaEvent = new DeltaEvent(changedEvent, differentFields);
    }

    /**
     * Gets a collection update representing the implicit changes to existing change exception events.
     *
     * @return The exception updates, or an empty update if there are none
     */
    public CollectionUpdate<Event, EventField> getExceptionUpdates() {
        return exceptionUpdates;
    }

    /**
     * Gets a special 'delta' event on top of the changed event data to indicate actual property differences during the storage update.
     *
     * @return The generated delta event
     */
    public Event getDelta() {
        return deltaEvent;
    }

    @Override
    public Event getOriginal() {
        return eventUpdate.getOriginal();
    }

    @Override
    public Event getUpdate() {
        return eventUpdate.getUpdate();
    }

    @Override
    public Set<EventField> getUpdatedFields() {
        return eventUpdate.getUpdatedFields();
    }

    @Override
    public boolean containsAnyChangeOf(EventField[] fields) {
        return eventUpdate.containsAnyChangeOf(fields);
    }

    @Override
    public AttendeeHelper getAttendeeUpdates() {
        return attendeeUpdates;
    }

    @Override
    public CollectionUpdate<Alarm, AlarmField> getAlarmUpdates() {
        return alarmUpdates;
    }

    @Override
    public SimpleCollectionUpdate<Attachment> getAttachmentUpdates() {
        return attachmentUpdates;
    }

    /**
     * Adjusts any change- and delete exceptions of a recurring event along with the update of the series master. In particular, the
     * following changes are applied for the changed event and -exceptions:
     * <ul>
     * <li>If an event series is turned into a single event, any series exceptions are removed</li>
     * <li>If the series master event's start-date is changed, the recurrence identifiers of all change- and delete-exceptions are
     * adjusted accordingly to reflect the time shift</li>
     * <li>If the recurrence rule changes, any exceptions whose recurrence identifier no longer matches the recurrence are removed</li>
     * <li>Further changes of the series master event that can be taken over in the same way are propagated in existing change exception
     * events</li>
     * </ul>
     *
     * @param originalEvent The original event being updated
     * @param changedEvent The event representing the updated event
     * @param originalChangeExceptions The change exceptions of the original series event, or <code>null</code> if not applicable
     * @return The resulting list of (possibly adjusted) change exceptions
     */
    private List<Event> adjustExceptions(Event originalEvent, Event changedEvent, List<Event> originalChangeExceptions) throws OXException {
        if (false == isSeriesMaster(originalEvent)) {
            return Collections.emptyList();
        }
        if (null == changedEvent.getRecurrenceRule()) {
            /*
             * reset all delete- and change exceptions if recurrence is deleted & indicate an empty change exception list
             */
            changedEvent.setDeleteExceptionDates(null);
            changedEvent.setChangeExceptionDates(null);
            changedEvent.setSeriesId(null);
            return Collections.emptyList();
        }
        /*
         * adjust recurrence identifiers in series master and change exceptions (to reflect change of start date)
         */
        List<Event> changedChangeExceptions = adjustRecurrenceIds(originalEvent, changedEvent, originalChangeExceptions);
        /*
         * remove not matching recurrences in change- and delete-exceptions (to reflect change of recurrence rule)
         */
        changedChangeExceptions = removeInvalidRecurrenceIds(changedEvent, changedChangeExceptions);
        /*
         * take over non-conflicting changes in series master to change exceptions
         */
        changedChangeExceptions = propagateToChangeExceptions(originalEvent, changedEvent, originalChangeExceptions, changedChangeExceptions);
        return changedChangeExceptions;
    }

    private void ensureConsistency(Event originalEvent, Event updatedEvent, Date timestamp) throws OXException {
        Consistency.adjustAllDayDates(updatedEvent);
        /*
         * adjust recurrence-related properties
         */
        if (isSeriesMaster(originalEvent) && updatedEvent.containsRecurrenceRule() && null == updatedEvent.getRecurrenceRule()) {
            /*
             * series to single event, remove recurrence & ensure all necessary recurrence data is present in passed event update
             */
            updatedEvent.setSeriesId(null);
            updatedEvent.setChangeExceptionDates(null);
            updatedEvent.setDeleteExceptionDates(null);
        }
        if (false == isSeriesMaster(originalEvent) && false == isSeriesException(originalEvent) && updatedEvent.containsRecurrenceRule() && null != updatedEvent.getRecurrenceRule()) {
            /*
             * single event to series, take over series id
             */
            updatedEvent.setSeriesId(originalEvent.getId());
        }
        /*
         * apply & take over attendee updates in changed event
         */
        List<Attendee> updatedAttendees = AttendeeHelper.onUpdatedEvent(session, folder, originalEvent.getAttendees(), updatedEvent.getAttendees()).previewChanges();
        updatedEvent.setAttendees(AttendeeMapper.getInstance().copy(updatedAttendees, (AttendeeField[]) null));
        /*
         * adjust attendee-dependent fields (ignore for change exceptions)
         */
        if (isSeriesException(originalEvent)) {
            EventMapper.getInstance().copy(originalEvent, updatedEvent, EventField.ORGANIZER, EventField.FOLDER_ID, EventField.CALENDAR_USER);
        } else if (isNullOrEmpty(updatedEvent.getAttendees())) {
            adjustForNonGroupScheduled(originalEvent, updatedEvent);
        } else {
            adjustForGroupScheduled(originalEvent, updatedEvent);
        }
        /*
         * reset attendee's partstats if required
         */
        if (needsParticipationStatusReset(originalEvent, updatedEvent)) {
            resetParticipationStatus(updatedEvent.getAttendees());
        }
        /*
         * apply timestamp/last-modified info & increment sequence number as needed
         */
        if (originalEvent.getSequence() >= updatedEvent.getSequence() && needsSequenceNumberIncrement(originalEvent, updatedEvent)) {
            updatedEvent.setSequence(originalEvent.getSequence() + 1);
        }
        Consistency.setModified(session, timestamp, updatedEvent, session.getUserId());
    }

    private void checkIntegrity(Event originalEvent, Event updatedEvent, EventField updatedField) throws OXException {
        switch (updatedField) {
            case GEO:
                Check.geoLocationIsValid(updatedEvent);
                break;
            case CLASSIFICATION:
                /*
                 * check validity & deny update for change exceptions (treating PUBLIC as default value)
                 */
                Check.classificationIsValid(updatedEvent.getClassification(), folder);
                if (isSeriesException(originalEvent) && isPublicClassification(originalEvent) != isPublicClassification(updatedEvent)) {
                    throw CalendarExceptionCodes.UNSUPPORTED_CLASSIFICATION_FOR_OCCURRENCE.create(
                        String.valueOf(updatedEvent.getClassification()), originalEvent.getSeriesId(), String.valueOf(originalEvent.getRecurrenceId()));
                }
                break;
            case START_DATE:
            case END_DATE:
                Check.startAndEndDate(updatedEvent);
                break;
            case RECURRENCE_RULE:
                Check.recurrenceRuleIsValid(session.getRecurrenceService(), updatedEvent);
                /*
                 * deny update for change exceptions (but ignore if set to 'null')
                 */
                if (isSeriesException(originalEvent)) {
                    if (null != updatedEvent.getRecurrenceRule()) {
                        throw CalendarExceptionCodes.FORBIDDEN_CHANGE.create(originalEvent.getId(), updatedField);
                    }
                    updatedEvent.removeRecurrenceRule();
                }
                break;
            case UID:
            case SERIES_ID:
            case CALENDAR_USER:
                throw CalendarExceptionCodes.FORBIDDEN_CHANGE.create(originalEvent.getId(), updatedField);
            default:
                break;
        }
    }

    private void checkIntegrity(Event originalEvent, Event updatedEvent) throws OXException {
        EventField[] differentFields = EventMapper.getInstance().getDifferentFields(originalEvent, updatedEvent);
        for (EventField updatedField : differentFields) {
            checkIntegrity(originalEvent, updatedEvent, updatedField);
        }
    }

    /**
     * Generates the updated event by applying all changes to the original event data.
     * <ul>
     * <li>No further, implicit changes to ensure the consistency of the resulting event are performed</li>
     * <li>The updated fields collection in the resulting item update will reflect all <i>set</i> fields of the update</li>
     * </ul>
     *
     * @param originalEvent The original event being updated
     * @param updatedEvent The updated event, as passed by the client
     * @param ignoredFields Optional event fields to ignore during the update
     * @return The changed event
     */
    private static Event apply(Event originalEvent, Event updatedEvent, EventField... ignoredFields) throws OXException {
        /*
         * determine relevant changes in passed event update
         */
        Set<EventField> updatedFields = new HashSet<EventField>(java.util.Arrays.asList(EventMapper.getInstance().getAssignedFields(updatedEvent)));
        if (null != ignoredFields) {
            for (EventField ignoredField : ignoredFields) {
                updatedFields.remove(ignoredField);
            }
        }
        /*
         * strip any 'per-user' properties
         */
        updatedFields.remove(EventField.FOLDER_ID);
        updatedFields.remove(EventField.ALARMS);
        //        assignedFields.remove(EventField.TRANSP);
        /*
         * (virtually) apply all changes of the passed event update
         */
        EventField[] changedFields = updatedFields.toArray(new EventField[updatedFields.size()]);
        Event changedEvent = EventMapper.getInstance().copy(originalEvent, null, (EventField[]) null);
        return EventMapper.getInstance().copy(updatedEvent, changedEvent, changedFields);
    }

    /**
     * Prepares certain properties in the passed event update to reflect that the event is or is now a <i>group-scheduled</i> event.
     * <p/>
     * This includes the organizer property of the event, as well as the common parent folder identifier and associated the calendar user
     * of the event.
     *
     * @param originalEvent The original event
     * @param updatedEvent The updated event
     */
    private void adjustForGroupScheduled(Event originalEvent, Event updatedEvent) throws OXException {
        /*
         * group-scheduled event, ensure to take over an appropriate organizer & reset common calendar folder (unless public)
         */
        if (null == originalEvent.getOrganizer()) {
            updatedEvent.setOrganizer(prepareOrganizer(updatedEvent.getOrganizer()));
        } else if (updatedEvent.containsOrganizer()) {
            Organizer organizer = session.getEntityResolver().prepare(updatedEvent.getOrganizer(), CalendarUserType.INDIVIDUAL);
            if (null != organizer && false == matches(originalEvent.getOrganizer(), organizer)) {
                throw CalendarExceptionCodes.FORBIDDEN_CHANGE.create(originalEvent.getId(), EventField.ORGANIZER);
            }
            updatedEvent.removeOrganizer(); // ignore
        }
        if (PublicType.getInstance().equals(folder.getType())) {
            if (null == originalEvent.getFolderId() || updatedEvent.containsFolderId()) {
                updatedEvent.setFolderId(folder.getID());
            }
            if (null != originalEvent.getCalendarUser() || updatedEvent.containsCalendarUser()) {
                updatedEvent.setCalendarUser(null);
            }
        } else {
            if (null != originalEvent.getFolderId() || updatedEvent.containsFolderId()) {
                updatedEvent.setFolderId(null);
            }
            if (null == originalEvent.getCalendarUser()) {
                updatedEvent.setCalendarUser(calendarUser);
            } else if (updatedEvent.containsCalendarUser()) {
                if (false == matches(updatedEvent.getCalendarUser(), originalEvent.getCalendarUser())) {
                    throw CalendarExceptionCodes.FORBIDDEN_CHANGE.create(originalEvent.getId(), EventField.CALENDAR_USER);
                }
                updatedEvent.removeCalendarUser(); // ignore
            }
        }
    }

    /**
     * Prepares certain properties in the passed event update to reflect that the event is not or no longer a <i>group-scheduled</i> event.
     * <p/>
     * This includes the organizer property of the event, as well as the common parent folder identifier and associated the calendar user
     * of the event.
     *
     * @param originalEvent The original event
     * @param updatedEvent The updated event
     */
    private void adjustForNonGroupScheduled(Event originalEvent, Event updatedEvent) throws OXException {
        /*
         * no group-scheduled event (any longer), ensure to take over common calendar folder & user, remove organizer
         */
        if (null != originalEvent.getOrganizer() || updatedEvent.containsOrganizer()) {
            updatedEvent.setOrganizer(null);
        }
        if (false == folder.getID().equals(originalEvent.getFolderId()) || updatedEvent.containsFolderId()) {
            updatedEvent.setFolderId(folder.getID());
        }
        CalendarUser newCalendarUser = PublicType.getInstance().equals(folder.getType()) ? null : calendarUser;
        if (false == matches(newCalendarUser, originalEvent.getCalendarUser()) || updatedEvent.containsCalendarUser()) {
            updatedEvent.setCalendarUser(newCalendarUser);
        }
    }

    /**
     * Gets a value indicating whether the event's sequence number ought to be incremented along with the update or not.
     *
     * @param originalEvent The original event
     * @param updatedEvent The updated event
     * @return <code>true</code> if the event's sequence number should be updated, <code>false</code>, otherwise
     */
    private static boolean needsSequenceNumberIncrement(Event originalEvent, Event updatedEvent) throws OXException {
        EventField[] relevantFields = new EventField[] {
            EventField.SUMMARY, EventField.LOCATION, EventField.RECURRENCE_RULE, EventField.START_DATE, EventField.END_DATE };
        if (false == EventMapper.getInstance().equalsByFields(originalEvent, updatedEvent, relevantFields)) {
            return true;
        }
        CollectionUpdate<Attendee, AttendeeField> attendeeUpdates = CalendarUtils.getAttendeeUpdates(originalEvent.getAttendees(), updatedEvent.getAttendees());
        if (0 < attendeeUpdates.getAddedItems().size() || 0 < attendeeUpdates.getRemovedItems().size()) {
            //TODO: more distinct evaluation of attendee updates
            return true;
        }
        return false;
    }

    private List<Event> propagateToChangeExceptions(Event originalMaster, Event updatedMaster, List<Event> originalChangeExceptions, List<Event> updatedChangeExceptions) throws OXException {
        List<Event> changedChangeExceptions = EventMapper.getInstance().copy(updatedChangeExceptions, (EventField[]) null);
        /*
         * apply common changes in 'basic' fields'
         */
        EventField[] basicFields = {
            EventField.SUMMARY, EventField.LOCATION, EventField.DESCRIPTION, EventField.CATEGORIES, EventField.COLOR, EventField.URL, EventField.GEO
        };
        for (EventField field : basicFields) {
            propagateFieldUpdate(originalMaster, updatedMaster, field, changedChangeExceptions);
        }
        /*
         * take over changes in start- and/or end-date based on calculated original timeslot
         */
        if (false == EventMapper.getInstance().get(EventField.START_DATE).equals(originalMaster, updatedMaster) ||
            false == EventMapper.getInstance().get(EventField.END_DATE).equals(originalMaster, updatedMaster)) {
            for (Event changedChangeException : changedChangeExceptions) {
                Event originalChangeException = find(originalChangeExceptions, changedChangeException.getId());
                if (null == originalChangeException) {
                    continue;
                }
                DateTime originalOccurrenceStart = calculateStart(originalMaster, originalChangeException.getRecurrenceId());
                DateTime originalOccurrenceEnd = calculateEnd(originalMaster, originalChangeException.getRecurrenceId());
                if (originalOccurrenceStart.equals(originalChangeException.getStartDate()) && originalOccurrenceEnd.equals(originalChangeException.getEndDate())) {
                    changedChangeException.setStartDate(calculateStart(updatedMaster, changedChangeException.getRecurrenceId()));
                    changedChangeException.setEndDate(calculateEnd(updatedMaster, changedChangeException.getRecurrenceId()));
                }
            }
        }
        /*
         * apply added & removed attendees
         */
        AttendeeHelper attendeeUpdates = AttendeeHelper.onUpdatedEvent(session, folder, originalMaster.getAttendees(), updatedMaster.getAttendees());
        changedChangeExceptions = propagateAttendeeUpdates(attendeeUpdates, changedChangeExceptions);
        return changedChangeExceptions;
    }

    /**
     * Propagates an update of a specific property in a series master event to any change exception events, i.e. the property is also
     * updated in the change exception events if their value equals the value of the original series event.
     *
     * @param originalMaster The original series master event being updated
     * @param updatedmaster The updated series master event
     * @param field The event field to propagate
     * @param changeExceptions The list of events to propagate the field update to
     * @return The (possibly adjusted) list of change exception events
     */
    private static List<Event> propagateFieldUpdate(Event originalMaster, Event updatedMaster, EventField field, List<Event> changeExceptions) throws OXException {
        Mapping<? extends Object, Event> mapping = EventMapper.getInstance().get(field);
        if (mapping.equals(originalMaster, updatedMaster) || isNullOrEmpty(changeExceptions)) {
            return changeExceptions;
        }
        for (Event changeException : changeExceptions) {
            if (mapping.equals(originalMaster, changeException)) {
                mapping.copy(updatedMaster, changeException);
            }
        }
        return changeExceptions;
    }

    /**
     * Propagates any added and removed attendees found in a specific collection update to one or more events, i.e. added attendees are
     * also added in each attendee list of the supplied events, unless they do not already attend, and removed attendees are also removed
     * from each attendee list if contained.
     *
     * @param attendeeUpdates The attendee collection update to propagate
     * @param changeExceptions The list of events to propagate the attendee updates to
     * @return The (possibly adjusted) list of events
     */
    private static List<Event> propagateAttendeeUpdates(SimpleCollectionUpdate<Attendee> attendeeUpdates, List<Event> changeExceptions) {
        if (null == attendeeUpdates || attendeeUpdates.isEmpty() || isNullOrEmpty(changeExceptions)) {
            return changeExceptions;
        }
        for (Event changeException : changeExceptions) {
            for (Attendee addedAttendee : attendeeUpdates.getAddedItems()) {
                if (false == contains(changeException.getAttendees(), addedAttendee)) {
                    changeException.getAttendees().add(addedAttendee);
                }
            }
            for (Attendee removedAttendee : attendeeUpdates.getRemovedItems()) {
                Attendee matchingAttendee = find(changeException.getAttendees(), removedAttendee);
                if (null != matchingAttendee) {
                    changeException.getAttendees().remove(matchingAttendee);
                }
            }
        }
        return changeExceptions;
    }

    private List<Event> removeInvalidRecurrenceIds(Event seriesMaster, List<Event> changeExceptions) throws OXException {
        /*
         * build list of possible exception dates
         */
        SortedSet<RecurrenceId> exceptionDates = combine(seriesMaster.getDeleteExceptionDates(), seriesMaster.getChangeExceptionDates());
        if (exceptionDates.isEmpty()) {
            return Collections.emptyList();
        }
        RecurrenceData recurrenceData = new DefaultRecurrenceData(seriesMaster.getRecurrenceRule(), seriesMaster.getStartDate(), null);
        Date from = new Date(exceptionDates.first().getValue().getTimestamp());
        Date until = add(new Date(exceptionDates.last().getValue().getTimestamp()), Calendar.DATE, 1);
        List<RecurrenceId> possibleExceptionDates = asList(session.getRecurrenceService().iterateRecurrenceIds(recurrenceData, from, until));
        /*
         * remove not matching delete- and change exceptions
         */
        if (false == isNullOrEmpty(seriesMaster.getDeleteExceptionDates())) {
            SortedSet<RecurrenceId> newDeleteExceptionDates = new TreeSet<RecurrenceId>(seriesMaster.getDeleteExceptionDates());
            if (newDeleteExceptionDates.retainAll(possibleExceptionDates)) {
                seriesMaster.setDeleteExceptionDates(newDeleteExceptionDates);
            }
        }
        if (false == isNullOrEmpty(seriesMaster.getChangeExceptionDates())) {
            SortedSet<RecurrenceId> newChangeExceptionDates = new TreeSet<RecurrenceId>(seriesMaster.getChangeExceptionDates());
            if (newChangeExceptionDates.retainAll(possibleExceptionDates)) {
                seriesMaster.setChangeExceptionDates(newChangeExceptionDates);
            }
        }
        if (false == isNullOrEmpty(changeExceptions)) {
            List<Event> newChangeExceptions = new ArrayList<Event>(changeExceptions);
            if (newChangeExceptions.removeIf(event -> false == possibleExceptionDates.contains(event.getRecurrenceId()))) {
                changeExceptions = newChangeExceptions;
            }
        }
        return changeExceptions;
    }

    /**
     * Adjusts the recurrence identifiers of any change- and delete-exceptions in an event series to reflect a change of the series start
     * date by applying an offset based on the difference of an original and updated series start date.
     *
     * @param originalEvent The original event
     * @param updatedEvent The updated event
     * @param originalChangeExceptions The change exceptions of the original series event, or <code>null</code> if not applicable
     * @return The resulting list of (possibly adjusted) change exceptions
     */
    private static List<Event> adjustRecurrenceIds(Event originalEvent, Event updatedEvent, List<Event> originalChangeExceptions) throws OXException {
        if (false == isSeriesMaster(originalEvent)) {
            return Collections.emptyList();
        }
        DateTime originalSeriesStart = originalEvent.getStartDate();
        DateTime updatedSeriesStart = updatedEvent.getStartDate();
        if (false == originalSeriesStart.equals(updatedSeriesStart)) {
            /*
             * start-date changed, adjust recurrence identifiers for delete- and change-exception collections in changed event
             * accordingly (unless already done by the client)
             */
            updatedEvent.setDeleteExceptionDates(shiftRecurrenceIds(originalEvent.getDeleteExceptionDates(), updatedEvent.getDeleteExceptionDates(), originalSeriesStart, updatedSeriesStart));
            updatedEvent.setChangeExceptionDates(shiftRecurrenceIds(originalEvent.getChangeExceptionDates(), updatedEvent.getChangeExceptionDates(), originalSeriesStart, updatedSeriesStart));
            /*
             * also adjust recurrence identifier of existing change exceptions
             */
            if (false == isNullOrEmpty(originalChangeExceptions)) {
                List<Event> changedChangeExceptions = EventMapper.getInstance().copy(originalChangeExceptions, (EventField[]) null);
                for (Event changeException : changedChangeExceptions) {
                    RecurrenceId newRecurrenceId = shiftRecurrenceId(changeException.getRecurrenceId(), originalSeriesStart, updatedSeriesStart);
                    changeException.setRecurrenceId(newRecurrenceId);
                    changeException.setChangeExceptionDates(new TreeSet<RecurrenceId>(Collections.singleton(newRecurrenceId)));
                }
                return changedChangeExceptions;
            }
        }
        return originalChangeExceptions;
    }

    /**
     * Resets the participation status of all individual attendees - excluding the current calendar user - to
     * {@link ParticipationStatus#NEEDS_ACTION} for a specific event.
     *
     * @param attendees The event's attendees
     */
    private void resetParticipationStatus(List<Attendee> attendees) throws OXException {
        for (Attendee attendee : CalendarUtils.filter(attendees, null, CalendarUserType.INDIVIDUAL)) {
            if (calendarUser.getEntity() == attendee.getEntity() || ParticipationStatus.NEEDS_ACTION.equals(attendee.getPartStat())) {
                continue;
            }
            attendee.setPartStat(ParticipationStatus.NEEDS_ACTION); //TODO: or reset to initial partstat based on folder type?
        }
    }

    /**
     * Prepares the organizer for an event, taking over an external organizer if specified.
     *
     * @param organizerData The organizer as defined by the client, or <code>null</code> to prepare the current calendar user as organizer
     * @return The prepared organizer
     */
    private Organizer prepareOrganizer(Organizer organizerData) throws OXException {
        Organizer organizer;
        if (null != organizerData) {
            organizer = session.getEntityResolver().prepare(organizerData, CalendarUserType.INDIVIDUAL);
            if (0 < organizer.getEntity()) {
                /*
                 * internal organizer must match the actual calendar user if specified
                 */
                if (organizer.getEntity() != calendarUser.getEntity()) {
                    throw CalendarExceptionCodes.INVALID_CALENDAR_USER.create(organizer.getUri(), I(organizer.getEntity()), CalendarUserType.INDIVIDUAL);
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
            organizer = session.getEntityResolver().applyEntityData(new Organizer(), calendarUser.getEntity());
        }
        /*
         * apply "sent-by" property if someone is acting on behalf of the calendar user
         */
        if (null != organizer && calendarUser.getEntity() != session.getUserId()) {
            organizer.setSentBy(session.getEntityResolver().applyEntityData(new CalendarUser(), session.getUserId()));
        }
        return organizer;
    }

    /**
     * Gets a value indicating whether the participation status of the event's attendees needs to be reset along with the update or not.
     *
     * @param originalEvent The original event being updated
     * @param updatedEvent The updated event, as passed by the client
     * @return <code>true</code> if the attendee's participation status should be reseted, <code>false</code>, otherwise
     * @see <a href="https://tools.ietf.org/html/rfc6638#section-3.2.8">RFC 6638, section 3.2.8</a>
     */
    private boolean needsParticipationStatusReset(Event originalEvent, Event updatedEvent) throws OXException {
        if (false == CalendarUtils.isOrganizer(originalEvent, calendarUser.getEntity())) {
            /*
             * only reset if event is modified by organizer
             */
            return false;
        }
        if (false == EventMapper.getInstance().get(EventField.RECURRENCE_RULE).equals(originalEvent, updatedEvent)) {
            /*
             * reset if there are 'new' occurrences (caused by a modified or extended rule)
             */
            if (hasFurtherOccurrences(originalEvent.getRecurrenceRule(), updatedEvent.getRecurrenceRule())) {
                return true;
            }
        }
        if (false == EventMapper.getInstance().get(EventField.DELETE_EXCEPTION_DATES).equals(originalEvent, updatedEvent)) {
            /*
             * reset if there are 'new' occurrences (caused by the reinstatement of previous delete exceptions)
             */
            SimpleCollectionUpdate<RecurrenceId> exceptionDateUpdates = getExceptionDateUpdates(originalEvent.getDeleteExceptionDates(), updatedEvent.getDeleteExceptionDates());
            if (false == exceptionDateUpdates.getRemovedItems().isEmpty()) {
                return true;
            }
        }
        if (false == EventMapper.getInstance().get(EventField.START_DATE).equals(originalEvent, updatedEvent)) {
            /*
             * reset if updated start is before the original start
             */
            if (updatedEvent.getStartDate().before(originalEvent.getStartDate())) {
                return true;
            }
        }
        if (false == EventMapper.getInstance().get(EventField.END_DATE).equals(originalEvent, updatedEvent)) {
            /*
             * reset if updated end is after the original end
             */
            if (updatedEvent.getEndDate().after(originalEvent.getEndDate())) {
                return true;
            }
        }
        /*
         * no reset needed, otherwise
         */
        return false;
    }

    /**
     * Gets a value indicating whether an updated recurrence rule would produce further, additional occurrences compared to the original
     * rule.
     *
     * @param originalRRule The original recurrence rule, or <code>null</code> if there was none
     * @param updatedRRule The original recurrence rule, or <code>null</code> if there is none
     * @return <code>true</code> if the updated rule yields further occurrences, <code>false</code>, otherwise
     */
    private static boolean hasFurtherOccurrences(String originalRRule, String updatedRRule) throws OXException {
        if (null == originalRRule) {
            return null != updatedRRule;
        }
        if (null == updatedRRule) {
            return false;
        }
        RecurrenceRule originalRule = initRecurrenceRule(originalRRule);
        RecurrenceRule updatedRule = initRecurrenceRule(updatedRRule);
        /*
         * check if only UNTIL was changed
         */
        RecurrenceRule checkedRule = initRecurrenceRule(updatedRule.toString());
        checkedRule.setUntil(originalRule.getUntil());
        if (checkedRule.toString().equals(originalRule.toString())) {
            return 1 == CalendarUtils.compare(originalRule.getUntil(), updatedRule.getUntil(), null);
        }
        /*
         * check if only COUNT was changed
         */
        checkedRule = initRecurrenceRule(updatedRule.toString());
        if (null == originalRule.getCount()) {
            checkedRule.setUntil(null);
        } else {
            checkedRule.setCount(i(originalRule.getCount()));
        }
        if (checkedRule.toString().equals(originalRule.toString())) {
            int originalCount = null == originalRule.getCount() ? Integer.MAX_VALUE : i(originalRule.getCount());
            int updatedCount = null == updatedRule.getCount() ? Integer.MAX_VALUE : i(updatedRule.getCount());
            return updatedCount > originalCount;
        }
        /*
         * check if only the INTERVAL was extended
         */
        //TODO

        /*
         * check if each BY... part is equally or more restrictive
         */
        //TODO

        return true;
    }

}
