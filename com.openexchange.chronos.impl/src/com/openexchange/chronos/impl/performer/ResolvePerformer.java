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

package com.openexchange.chronos.impl.performer;

import static com.openexchange.chronos.common.CalendarUtils.filterByUid;
import static com.openexchange.chronos.common.CalendarUtils.getEventID;
import static com.openexchange.chronos.common.CalendarUtils.getEventsByUID;
import static com.openexchange.chronos.common.CalendarUtils.getFields;
import static com.openexchange.chronos.common.CalendarUtils.getObjectIDs;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesException;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.common.CalendarUtils.sortSeriesMasterFirst;
import static com.openexchange.chronos.common.SearchUtils.getSearchTerm;
import static com.openexchange.chronos.impl.Check.requireCalendarPermission;
import static com.openexchange.chronos.impl.Utils.getCalendarUserId;
import static com.openexchange.chronos.impl.Utils.getFolder;
import static com.openexchange.chronos.impl.Utils.getFolderIdTerm;
import static com.openexchange.folderstorage.Permission.NO_PERMISSIONS;
import static com.openexchange.folderstorage.Permission.READ_ALL_OBJECTS;
import static com.openexchange.folderstorage.Permission.READ_FOLDER;
import static com.openexchange.folderstorage.Permission.READ_OWN_OBJECTS;
import static com.openexchange.java.Autoboxing.i;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultEventsResult;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventID;
import com.openexchange.chronos.service.EventsResult;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.type.SharedType;
import com.openexchange.java.Strings;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.search.internal.operands.ColumnFieldOperand;

/**
 * {@link ResolvePerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class ResolvePerformer extends AbstractQueryPerformer {

    /**
     * Initializes a new {@link ResolvePerformer}.
     *
     * @param session The calendar session
     * @param storage The underlying calendar storage
     */
    public ResolvePerformer(CalendarSession session, CalendarStorage storage) {
        super(session, storage);
    }

    /**
     * Performs the resolve by id operation.
     *
     * @param eventId The identifier of the event to resolve
     * @param sequence The expected sequence number to match, or <code>null</code> to resolve independently of the event's sequence number
     * @return The resolved event, or <code>null</code> if not found
     */
    public Event resolveById(String eventId, Integer sequence) throws OXException {
        /*
         * load event data, check permissions & apply folder identifier
         */
        Event event = storage.getEventStorage().loadEvent(eventId, null);
        if (null == event || null != sequence && i(sequence) != event.getSequence()) {
            return null;
        }
        int calendarUserId = session.getUserId();
        event = storage.getUtilities().loadAdditionalEventData(calendarUserId, event, null);
        String folderId = CalendarUtils.getFolderView(event, calendarUserId);
        CalendarFolder folder = getFolder(session, folderId, false);
        if (false == hasReadPermission(event)) {
            if (false == matches(event.getCreatedBy(), session.getUserId())) {
                requireCalendarPermission(folder, READ_FOLDER, READ_ALL_OBJECTS, NO_PERMISSIONS, NO_PERMISSIONS);
            } else {
                requireCalendarPermission(folder, READ_FOLDER, READ_OWN_OBJECTS, NO_PERMISSIONS, NO_PERMISSIONS);
            }
        }
        return postProcessor().process(event, folder).getFirstEvent();
    }

    /**
     * Performs the resolve by uid operation.
     *
     * @param uid The unique identifier to resolve
     * @return The identifier of the resolved event, or <code>null</code> if not found
     */
    public String resolveByUid(String uid) throws OXException {
        /*
         * construct search term
         */
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
            .addSearchTerm(getSearchTerm(EventField.UID, SingleOperation.EQUALS, uid))
            .addSearchTerm(new CompositeSearchTerm(CompositeOperation.OR)
                .addSearchTerm(getSearchTerm(EventField.SERIES_ID, SingleOperation.ISNULL))
                .addSearchTerm(getSearchTerm(EventField.ID, SingleOperation.EQUALS, new ColumnFieldOperand<EventField>(EventField.SERIES_ID)))
            )
        ;
        /*
         * search for an event matching the UID & verify equality via String#equals
         */
        List<Event> events = filterByUid(storage.getEventStorage().searchEvents(searchTerm, null, new EventField[] { EventField.ID, EventField.UID }), uid);
        if (1 < events.size()) {
            String conflictingIds = events.stream().map(Event::getId).collect(Collectors.joining(", "));
            Exception cause = new IllegalStateException("UID \"" + uid + "\" resolves to multiple events [" + conflictingIds + ']');
            throw CalendarExceptionCodes.UID_CONFLICT.create(cause, uid, conflictingIds);
        }
        return events.isEmpty() ? null : events.get(0).getId();
    }

    /**
     * Resolves an unique identifier within the scope of a specific calendar user, i.e. the unique identifier is resolved to events
     * residing in the user's <i>personal</i>, as well as <i>public</i> calendar folders.
     *
     * @param uid The unique identifier to resolve
     * @param calendarUserId The identifier of the calendar user the unique identifier should be resolved for
     * @return The identifier of the resolved event, or <code>null</code> if not found
     */
    public EventID resolveByUid(String uid, int calendarUserId) throws OXException {
        return resolveByUid(uid, null, calendarUserId);
    }

    /**
     * Resolves an unique identifier and optional recurrence identifier pair within the scope of a specific calendar user, i.e. the unique
     * identifier is resolved to events residing in the user's <i>personal</i>, as well as <i>public</i> calendar folders.
     *
     * @param uid The unique identifier to resolve
     * @param recurrenceId The recurrence identifier to match, or <code>null</code> to resolve to non-recurring or series master events only
     * @param calendarUserId The identifier of the calendar user the unique identifier should be resolved for
     * @return The identifier of the resolved event, or <code>null</code> if not found
     */
    public EventID resolveByUid(String uid, RecurrenceId recurrenceId, int calendarUserId) throws OXException {
        /*
         * construct search term & lookup matching events in storage
         */
        CompositeSearchTerm searchTerm;
        if (null == recurrenceId) {            
            searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
                .addSearchTerm(getSearchTerm(EventField.UID, SingleOperation.EQUALS, uid))
                .addSearchTerm(new CompositeSearchTerm(CompositeOperation.OR)
                    .addSearchTerm(getSearchTerm(EventField.SERIES_ID, SingleOperation.ISNULL))
                    .addSearchTerm(getSearchTerm(EventField.ID, SingleOperation.EQUALS, new ColumnFieldOperand<EventField>(EventField.SERIES_ID)))
                );
        } else {
            searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
                .addSearchTerm(getSearchTerm(EventField.UID, SingleOperation.EQUALS, uid))
                .addSearchTerm(new CompositeSearchTerm(CompositeOperation.OR)
                    .addSearchTerm(new CompositeSearchTerm(CompositeOperation.NOT).addSearchTerm(getSearchTerm(EventField.SERIES_ID, SingleOperation.ISNULL)))
                    .addSearchTerm(getSearchTerm(EventField.ID, SingleOperation.NOT_EQUALS, new ColumnFieldOperand<EventField>(EventField.SERIES_ID)))
                );
        }        
        EventField[] fields = { EventField.ID, EventField.SERIES_ID, EventField.RECURRENCE_ID, EventField.FOLDER_ID, EventField.UID, EventField.CALENDAR_USER };
        List<Event> events = filterByUid(storage.getEventStorage().searchEvents(searchTerm, null, fields), uid);
        /*
         * load calendar user's attendee data for found events and resolve to first matching event for this folder's calendar user
         */
        Map<String, Attendee> attendeePerEvent = storage.getAttendeeStorage().loadAttendee(
            getObjectIDs(events), session.getEntityResolver().prepareUserAttendee(calendarUserId), (AttendeeField[]) null);
        for (Event event : events) {
            /*
             * match recurrence if specified
             */
            if (null != recurrenceId && false == recurrenceId.equals(event.getRecurrenceId()) || null == recurrenceId && isSeriesException(event)) {
                continue;
            }
            if (null != event.getFolderId()) {
                /*
                 * common folder identifier is assigned, consider 'resolved' in non-shared folder
                 */
                CalendarFolder folder = getFolder(session, event.getFolderId(), false);
                if (false == SharedType.getInstance().equals(folder.getType())) {
                    return getEventID(event);
                }
            } else {
                /*
                 * group scheduled event with no common folder identifier assigned, consider 'resolved' if calendar user attends
                 */
                Attendee attendee = attendeePerEvent.get(event.getId());
                if (null != attendee && Strings.isNotEmpty(attendee.getFolderId())) {
                    return new EventID(attendee.getFolderId(), event.getId(), event.getRecurrenceId());
                }
            }
        }
        /*
         * not found, otherwise
         */
        return null;
    }

    private List<Event> resolveByField(EventField field, CalendarFolder folder, String resourceName, EventField[] fields) throws OXException {
        /*
         * construct search term to lookup matching events in folder
         */
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
            .addSearchTerm(getFolderIdTerm(session, folder))
            .addSearchTerm(getSearchTerm(field, SingleOperation.EQUALS, resourceName))
        ;
        /*
         * return matching events
         */
        List<Event> events = storage.getEventStorage().searchEvents(searchTerm, null, fields);
        events = storage.getUtilities().loadAdditionalEventData(folder.getCalendarUserId(), events, fields);
        return sortSeriesMasterFirst(events);
    }

    /**
     * Resolves a specific event (and any overridden instances or <i>change exceptions</i>) by its externally used resource name, which
     * typically matches the event's UID or filename property. The lookup is performed within a specific folder in a case-sensitive way.
     * If an event series with overridden instances is matched, the series master event will be the first event in the returned list.
     * <p/>
     * It is also possible that only overridden instances of an event series are returned, which may be the case for <i>detached</i>
     * instances where the user has no access to the corresponding series master event.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_FIELDS}</li>
     * </ul>
     *
     * @param folderId The identifier of the folder to resolve the resource name in
     * @param resourceName The resource name to resolve
     * @return The resolved event(s), or <code>null</code> if no matching event was found
     * @see <a href="https://tools.ietf.org/html/rfc4791#section-4.1">RFC 4791, section 4.1</a>
     */
    public EventsResult resolve(String folderId, String resourceName) throws OXException {
        /*
         * resolve by UID or filename
         */
        CalendarFolder folder = getFolder(session, folderId);
        EventField[] fields = getFields(session, EventField.ORGANIZER, EventField.ATTENDEES);
        List<Event> events = resolveByField(EventField.UID, folder, resourceName, fields);
        if (null == events || events.isEmpty()) {
            events = resolveByField(EventField.FILENAME, folder, resourceName, fields);
            if (null == events || events.isEmpty()) {
                return null;
            }
        }
        return getResult(folder, events);
    }

    /**
     * Resolves multiple events (and any overridden instances or <i>change exceptions</i>) by their externally used resource name, which
     * typically matches the event's UID or filename property. The lookup is performed within a specific folder in a case-sensitive way.
     * If an event series with overridden instances is matched, the series master event will be the first event in the returned list of
     * the corresponding events result.
     * <p/>
     * It is also possible that only overridden instances of an event series are returned, which may be the case for <i>detached</i>
     * instances where the user has no access to the corresponding series master event.
     *
     * @param folderId The identifier of the folder to resolve the resource names in
     * @param resourceNames The resource names to resolve
     * @return The resolved event(s), mapped to their corresponding resource name
     * @see <a href="https://tools.ietf.org/html/rfc4791#section-4.1">RFC 4791, section 4.1</a>
     */
    public Map<String, EventsResult> resolve(String folderId, List<String> resourceNames) throws OXException {
        if (null == resourceNames || resourceNames.isEmpty()) {
            return Collections.emptyMap();
        }
        CalendarFolder folder = getFolder(session, folderId);
        /*
         * construct search term to find events and overridden instances in folder by uid
         */
        SearchTerm<?> searchTerm;
        if (1 == resourceNames.size()) {
            searchTerm = getSearchTerm(EventField.UID, SingleOperation.EQUALS, resourceNames.get(0));
        } else {
            CompositeSearchTerm orTerm = new CompositeSearchTerm(CompositeOperation.OR);
            for (String uid : resourceNames) {
                orTerm.addSearchTerm(getSearchTerm(EventField.UID, SingleOperation.EQUALS, uid));
            }
            searchTerm = orTerm;
        }
        //        searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
        //            .addSearchTerm(getFolderIdTerm(session, folder))
        //            .addSearchTerm(searchTerm)
        //        ;
        /*
         * perform search & map resulting events by UID
         */
        EventField[] fields = getFields(session, EventField.ORGANIZER, EventField.ATTENDEES);
        List<Event> events = storage.getEventStorage().searchEvents(searchTerm, null, fields);
        events = storage.getUtilities().loadAdditionalEventData(getCalendarUserId(folder), events, fields);
        Map<String, List<Event>> eventsByUID = getEventsByUID(events, false);
        /*
         * generate appropriate result for each requested resource name
         */
        Map<String, EventsResult> resultsPerResourceName = new HashMap<String, EventsResult>(resourceNames.size());
        for (String resourceName : resourceNames) {
            EventsResult result = getResult(folder, eventsByUID.get(resourceName));
            if (null == result) {
                /*
                 * try and resolve resource name manually as fallback; mark as not found, otherwise
                 */
                result = resolve(folderId, resourceName);
                if (null == result) {
                    result = new DefaultEventsResult(CalendarExceptionCodes.EVENT_NOT_FOUND_IN_FOLDER.create(folderId, resourceName));
                }
            }
            resultsPerResourceName.put(resourceName, result);
        }
        return resultsPerResourceName;
    }

    private EventsResult getResult(CalendarFolder folder, List<Event> events) {
        events = sortSeriesMasterFirst(events);
        if (null == events || events.isEmpty()) {
            return null;
        }
        for (Iterator<Event> iterator = events.iterator(); iterator.hasNext();) {
            Event event = iterator.next();
            if (false == Utils.isInFolder(event, folder) || false == Utils.isVisible(folder, event)) {
                iterator.remove();
            }
        }
        if (events.isEmpty()) {
            return null;
        }
        try {
            return postProcessor().process(events, folder).getEventsResult();
        } catch (OXException e) {
            return new DefaultEventsResult(e);
        }
    }

}
