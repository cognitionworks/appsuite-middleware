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
import static com.openexchange.chronos.common.CalendarUtils.initCalendar;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.chronos.impl.Check.requireCalendarPermission;
import static com.openexchange.chronos.impl.Utils.anonymizeIfNeeded;
import static com.openexchange.chronos.impl.Utils.appendCommonTerms;
import static com.openexchange.chronos.impl.Utils.getCalendarUser;
import static com.openexchange.chronos.impl.Utils.getFields;
import static com.openexchange.chronos.impl.Utils.getFolderIdTerm;
import static com.openexchange.chronos.impl.Utils.getFrom;
import static com.openexchange.chronos.impl.Utils.getSearchTerm;
import static com.openexchange.chronos.impl.Utils.getTimeZone;
import static com.openexchange.chronos.impl.Utils.getUntil;
import static com.openexchange.chronos.impl.Utils.i;
import static com.openexchange.chronos.impl.Utils.isExcluded;
import static com.openexchange.chronos.impl.Utils.isResolveOccurrences;
import static com.openexchange.chronos.impl.Utils.sort;
import static com.openexchange.folderstorage.Permission.NO_PERMISSIONS;
import static com.openexchange.folderstorage.Permission.READ_FOLDER;
import static com.openexchange.folderstorage.Permission.READ_OWN_OBJECTS;
import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.DataAwareRecurrenceId;
import com.openexchange.chronos.common.DefaultRecurrenceData;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.impl.osgi.Services;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.RecurrenceData;
import com.openexchange.chronos.service.RecurrenceService;
import com.openexchange.chronos.service.SortOptions;
import com.openexchange.chronos.service.UserizedEvent;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SingleSearchTerm.SingleOperation;

/**
 * {@link AbstractQueryPerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public abstract class AbstractQueryPerformer {

    protected final CalendarSession session;
    protected final CalendarStorage storage;

    /**
     * Initializes a new {@link AbstractQueryPerformer}.
     *
     * @param storage The underlying calendar storage
     * @param session The calendar session
     */
    protected AbstractQueryPerformer(CalendarSession session, CalendarStorage storage) throws OXException {
        super();
        this.session = session;
        this.storage = storage;
    }

    protected List<UserizedEvent> userize(List<Event> events, int forUser, boolean includePrivate) throws OXException {
        List<UserizedEvent> userizedEvents = new ArrayList<UserizedEvent>(events.size());
        for (Event event : events) {
            if (isExcluded(event, session, includePrivate)) {
                continue;
            }
            Attendee userAttendee = find(event.getAttendees(), forUser);
            int folderID;
            if (null != userAttendee && 0 < userAttendee.getFolderID()) {
                folderID = userAttendee.getFolderID();
            } else if (0 < event.getPublicFolderId()) {
                folderID = event.getPublicFolderId();
            } else {
                throw OXException.general("No suitable parent folder for event " + event); //TODO shouldn't happen at all?
            }
            UserizedEvent userizedEvent = getUserizedEvent(event, folderID);
            if (isSeriesMaster(event) && isResolveOccurrences(session)) {
                userizedEvents.addAll(resolveOccurrences(userizedEvent));
            } else {
                userizedEvents.add(userizedEvent);
            }
        }
        return sort(userizedEvents, new SortOptions(session));
    }

    protected List<Event> readEventsInFolder(UserizedFolder folder, int[] objectIDs, boolean deleted, Date updatedSince) throws OXException {
        requireCalendarPermission(folder, READ_FOLDER, READ_OWN_OBJECTS, NO_PERMISSIONS, NO_PERMISSIONS);
        /*
         * construct search term
         */
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND).addSearchTerm(getFolderIdTerm(folder));
        if (null != objectIDs) {
            if (0 == objectIDs.length) {
                return Collections.emptyList();
            } else if (1 == objectIDs.length) {
                searchTerm.addSearchTerm(getSearchTerm(EventField.ID, SingleOperation.EQUALS, I(objectIDs[0])));
            } else {
                CompositeSearchTerm orTerm = new CompositeSearchTerm(CompositeOperation.OR);
                for (int objectID : objectIDs) {
                    orTerm.addSearchTerm(getSearchTerm(EventField.ID, SingleOperation.EQUALS, I(objectID)));
                }
                searchTerm.addSearchTerm(orTerm);
            }
        }
        appendCommonTerms(searchTerm, getFrom(session), getUntil(session), updatedSince);
        /*
         * perform search & userize the results
         */
        List<Event> events;
        if (deleted) {
            events = storage.getEventStorage().searchDeletedEvents(searchTerm, new SortOptions(session), getFields(session));
        } else {
            events = storage.getEventStorage().searchEvents(searchTerm, new SortOptions(session), getFields(session));
        }
        return readAdditionalEventData(events, getCalendarUser(folder).getId(), getFields(session));
    }

    protected List<Event> readAdditionalEventData(List<Event> events, int userID, EventField[] fields) throws OXException {
        return Utils.loadAdditionalEventData(storage, userID, events, fields);
    }

    protected Event readAdditionalEventData(Event event, int userID, EventField[] fields) throws OXException {
        return Utils.loadAdditionalEventData(storage, userID, event, fields);
    }

    protected Iterator<Event> resolveOccurrences(Event masterEvent, Date from, Date until) throws OXException {
        TimeZone timeZone = getTimeZone(session);
        Calendar fromCalendar = null == from ? null : initCalendar(timeZone, from);
        Calendar untilCalendar = null == until ? null : initCalendar(timeZone, until);
        return Services.getService(RecurrenceService.class).calculateInstancesRespectExceptions(masterEvent, fromCalendar, untilCalendar, null, null);
        //        return Services.getService(RecurrenceService.class).calculateInstances(masterEvent, fromCalendar, untilCalendar, null);
    }

    /**
     * Gets a recurrence iterator for the supplied series master event, iterating over the recurrence identifiers of the event. Any change-
     * and delete exceptions (as per {@link Event#getChangeExceptionDates()} and Event#getDeleteExceptionDates()} are skipped implicitly).
     *
     * @param masterEvent The recurring event master
     * @param from The start of the iteration interval, or <code>null</code> to start with the first occurrence
     * @param until The end of the iteration interval, or <code>null</code> to iterate until the last occurrence
     * @return The recurrence iterator
     */
    protected Iterator<RecurrenceId> getRecurrenceIterator(Event masterEvent, Date from, Date until) throws OXException {
        TimeZone timeZone = getTimeZone(session);
        Calendar fromCalendar = null == from ? null : initCalendar(timeZone, from);
        Calendar untilCalendar = null == until ? null : initCalendar(timeZone, until);
        return Services.getService(RecurrenceService.class).getRecurrenceIterator(masterEvent, fromCalendar, untilCalendar, true);
    }

    protected List<UserizedEvent> userize(List<Event> events, UserizedFolder inFolder, boolean includePrivate) throws OXException {
        int folderID = i(inFolder);
        List<UserizedEvent> userizedEvents = new ArrayList<UserizedEvent>(events.size());
        for (Event event : events) {
            if (isExcluded(event, session, includePrivate)) {
                continue;
            }
            UserizedEvent userizedEvent = getUserizedEvent(event, folderID);
            if (isSeriesMaster(event) && isResolveOccurrences(session)) {
                userizedEvents.addAll(resolveOccurrences(userizedEvent));
            } else {
                userizedEvents.add(userizedEvent);
            }
        }
        return sort(userizedEvents, new SortOptions(session));
    }

    private List<UserizedEvent> resolveOccurrences(UserizedEvent master) throws OXException {
        RecurrenceData recurrenceData = new DefaultRecurrenceData(master.getEvent());
        List<UserizedEvent> events = new ArrayList<UserizedEvent>();
        Iterator<Event> occurrences = resolveOccurrences(master.getEvent(), getFrom(session), getUntil(session));
        while (occurrences.hasNext()) {
            Event occurrence = occurrences.next();
            if (isExcluded(occurrence, session, true)) {
                continue;
            }
            occurrence.setRecurrenceId(new DataAwareRecurrenceId(recurrenceData, occurrence.getRecurrenceId().getValue()));
            events.add(getUserizedEvent(occurrence, master.getFolderId()));
        }
        return events;
    }

    protected UserizedEvent getUserizedEvent(Event event, int folderID) throws OXException {
        UserizedEvent userizedEvent = new UserizedEvent(session.getSession(), event, folderID);
        return anonymizeIfNeeded(userizedEvent);
    }

}
