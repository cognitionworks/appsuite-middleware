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

package com.openexchange.chronos.provider.caching.internal.response;

import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.getFlags;
import static com.openexchange.chronos.common.CalendarUtils.getSearchTerm;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.provider.caching.CachingCalendarAccess;
import com.openexchange.chronos.provider.caching.internal.Services;
import com.openexchange.chronos.provider.caching.internal.handler.utils.HandlerHelper;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.EventID;
import com.openexchange.chronos.service.RecurrenceService;
import com.openexchange.chronos.service.SearchOptions;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.chronos.storage.operation.OSGiCalendarStorageOperation;
import com.openexchange.exception.OXException;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;

/**
 * {@link DedicatedEventsResponseGenerator}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.10.0
 */
public class DedicatedEventsResponseGenerator extends ResponseGenerator {

    private final List<EventID> eventIDs;

    public DedicatedEventsResponseGenerator(CachingCalendarAccess cachedCalendarAccess, List<EventID> eventIDs) {
        super(cachedCalendarAccess);
        this.eventIDs = eventIDs;
    }

    public List<Event> generate() throws OXException {
        return new OSGiCalendarStorageOperation<List<Event>>(Services.getServiceLookup(), this.cachedCalendarAccess.getSession().getContext().getContextId(), this.cachedCalendarAccess.getAccount().getAccountId()) {

            @Override
            protected List<Event> call(CalendarStorage storage) throws OXException {
                final Map<String, List<EventID>> idsPerFolder = HandlerHelper.sortEventIDsPerFolderId(getEventIDs());
                Map<String, List<Event>> eventsPerFolderId = new HashMap<String, List<Event>>(idsPerFolder.size());
                for (Entry<String, List<EventID>> entry : idsPerFolder.entrySet()) {
                    eventsPerFolderId.put(entry.getKey(), readEventsInFolder(storage, entry.getKey(), entry.getValue()));
                }
                List<Event> orderedEvents = new ArrayList<Event>(getEventIDs().size());
                for (EventID eventID : getEventIDs()) {
                    List<Event> eventsInFolder = eventsPerFolderId.get(eventID.getFolderID());
                    Event event = find(eventsInFolder, eventID.getObjectID(), eventID.getRecurrenceID());
                    if (null == event) {
                        org.slf4j.LoggerFactory.getLogger(DedicatedEventsResponseGenerator.class).debug("Event with {} not found, skipping.", eventID);
                    }
                    orderedEvents.add(event);
                }
                EventField[] fields = getFields(cachedCalendarAccess.getParameters().get(CalendarParameters.PARAMETER_FIELDS, EventField[].class));
                return storage.getUtilities().loadAdditionalEventData(cachedCalendarAccess.getAccount().getUserId(), orderedEvents, fields);
            }

        }.executeQuery();
    }

    protected List<Event> readEventsInFolder(CalendarStorage calendarStorage, String folderId, List<EventID> eventIDs) throws OXException {
        Set<String> objectIDs = new HashSet<String>(eventIDs.size());
        for (EventID eventID : eventIDs) {
            if (folderId.equals(eventID.getFolderID())) {
                objectIDs.add(eventID.getObjectID());
            }
        }
        List<Event> events = readEventsInFolder(calendarStorage, folderId, objectIDs.toArray(new String[objectIDs.size()]), null);
        List<Event> orderedEvents = new ArrayList<Event>(eventIDs.size());
        for (EventID eventID : eventIDs) {
            Event event = CalendarUtils.find(events, eventID.getObjectID());
            if (null == event) {
                continue;
            }
            RecurrenceId recurrenceId = eventID.getRecurrenceID();
            event.setFolderId(folderId);
            event.setFlags(getFlags(event, cachedCalendarAccess.getAccount().getUserId()));
            if (null != recurrenceId) {
                if (isSeriesMaster(event)) {
                    if (null != calendarStorage.getEventStorage().loadException(event.getId(), recurrenceId, new EventField[] { EventField.ID })) {
                        throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(eventID.getObjectID(), recurrenceId);
                    }
                    Iterator<Event> iterator = Services.getService(RecurrenceService.class).iterateEventOccurrences(event, new Date(recurrenceId.getValue().getTimestamp()), null);
                    if (false == iterator.hasNext()) {
                        throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(eventID.getObjectID(), recurrenceId);
                    }
                    orderedEvents.add(iterator.next());
                } else if (recurrenceId.equals(event.getRecurrenceId())) {
                    orderedEvents.add(event);
                } else {
                    throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(eventID.getObjectID(), recurrenceId);
                }
            } else {
                orderedEvents.add(event);
            }
        }
        return orderedEvents;
    }

    protected List<Event> readEventsInFolder(CalendarStorage calendarStorage, String folderId, String[] objectIDs, Date updatedSince) throws OXException {
        SearchTerm<?> folderSearchTerm = getSearchTerm(EventField.FOLDER_ID, SingleOperation.EQUALS, folderId);
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND).addSearchTerm(folderSearchTerm);
        if (null != objectIDs) {
            if (0 == objectIDs.length) {
                return Collections.emptyList();
            } else if (1 == objectIDs.length) {
                searchTerm.addSearchTerm(getSearchTerm(EventField.ID, SingleOperation.EQUALS, objectIDs[0]));
            } else {
                CompositeSearchTerm orTerm = new CompositeSearchTerm(CompositeOperation.OR);
                for (String objectID : objectIDs) {
                    orTerm.addSearchTerm(getSearchTerm(EventField.ID, SingleOperation.EQUALS, objectID));
                }
                searchTerm.addSearchTerm(orTerm);
            }
        }
        if (null != updatedSince) {
            searchTerm.addSearchTerm(getSearchTerm(EventField.LAST_MODIFIED, SingleOperation.GREATER_THAN, updatedSince));
        }
        EventField[] fields = getFields(this.cachedCalendarAccess.getParameters().get(CalendarParameters.PARAMETER_FIELDS, EventField[].class));
        SearchOptions searchOptions = new SearchOptions(this.cachedCalendarAccess.getParameters());
        List<Event> events = calendarStorage.getEventStorage().searchEvents(searchTerm, searchOptions, fields);
        events = calendarStorage.getUtilities().loadAdditionalEventData(cachedCalendarAccess.getAccount().getUserId(), events, fields);
        return events;
    }

    protected List<EventID> getEventIDs() {
        return eventIDs;
    }

}
