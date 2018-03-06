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

import static com.openexchange.chronos.common.CalendarUtils.getFields;
import static com.openexchange.chronos.common.SearchUtils.getSearchTerm;
import static com.openexchange.chronos.impl.Check.requireCalendarPermission;
import static com.openexchange.chronos.impl.Utils.getCalendarUserId;
import static com.openexchange.chronos.impl.Utils.getFolder;
import static com.openexchange.chronos.impl.Utils.getFolderIdTerm;
import static com.openexchange.chronos.impl.Utils.isEnforceDefaultAttendee;
import static com.openexchange.chronos.impl.Utils.isSkipClassifiedEvents;
import static com.openexchange.folderstorage.Permission.NO_PERMISSIONS;
import static com.openexchange.folderstorage.Permission.READ_FOLDER;
import static com.openexchange.folderstorage.Permission.READ_OWN_OBJECTS;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.common.DefaultEventsResult;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventsResult;
import com.openexchange.chronos.service.SearchOptions;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.Type;
import com.openexchange.folderstorage.type.PrivateType;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.tools.arrays.Arrays;

/**
 * {@link AllPerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class AllPerformer extends AbstractQueryPerformer {

    /** The synthetic identifier of the virtual 'all my events' calendar */
    private static final String VIRTUAL_ALL = "all";

    /** The synthetic identifier of the virtual 'all my events in public folders' calendar */
    private static final String VIRTUAL_ALL_PUBLIC = "allPublic";

    /**
     * Initializes a new {@link AllPerformer}.
     *
     * @param session The calendar session
     * @param storage The underlying calendar storage
     */
    public AllPerformer(CalendarSession session, CalendarStorage storage) throws OXException {
        super(session, storage);
    }

    /**
     * Performs the operation.
     *
     * @return The loaded events
     */
    public List<Event> perform() throws OXException {
        return perform(null, null);
    }

    /**
     * Performs the operation.
     *
     * @param partStats The participation status to include, or <code>null</code> to include all events independently of the user
     *            attendee's participation status
     * @param rsvp The reply expectation to include, or <code>null</code> to include all events independently of the user attendee's
     *            rsvp status
     * @return The loaded events
     */
    public List<Event> perform(Boolean rsvp, ParticipationStatus[] partStats) throws OXException {
        return getEventsOfUser(rsvp, partStats, null);
    }

    /**
     * Performs the operation.
     *
     * @param folderIds The identifiers of the parent folders to get all events from
     * @return The loaded events
     */
    public Map<String, EventsResult> perform(List<String> folderIds) throws OXException {
        if (null == folderIds || folderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, EventsResult> resultsPerFolderId = new HashMap<String, EventsResult>(folderIds.size());
        /*
         * get folders, storing a possible exception in the results
         */
        List<CalendarFolder> folders = new ArrayList<CalendarFolder>(folderIds.size());
        for (String folderId : folderIds) {
            try {
                if (VIRTUAL_ALL.equals(folderId)) {
                    /*
                     * add all events the user attends from all folders
                     */
                    resultsPerFolderId.put(folderId, new DefaultEventsResult(getEventsOfUser(null, null, null)));
                } else if (VIRTUAL_ALL_PUBLIC.equals(folderId)) {
                    /*
                     * add all events the user attends from all public folders
                     */
                    resultsPerFolderId.put(folderId, new DefaultEventsResult(getEventsOfUser(null, null, new Type[] { PublicType.getInstance() })));
                } else {
                    /*
                     * remember folder id for batch-retrieval
                     */
                    folders.add(getFolder(session, folderId));
                }
            } catch (OXException e) {
                /*
                 * track error for folder
                 */
                resultsPerFolderId.put(folderId, new DefaultEventsResult(e));
            }
        }
        /*
         * load event data per folder & additional event data per calendar user
         */
        EventField[] fields = getFields(session, EventField.ORGANIZER, EventField.ATTENDEES);
        SearchOptions searchOptions = new SearchOptions(session);
        Map<CalendarFolder, List<Event>> eventsPerFolder = new HashMap<CalendarFolder, List<Event>>(folders.size());
        for (Entry<Integer, List<CalendarFolder>> entry : getFoldersPerCalendarUserId(folders).entrySet()) {
            List<Event> eventsForCalendarUser = new ArrayList<Event>();
            for (CalendarFolder folder : entry.getValue()) {
                /*
                 * load events in folder
                 */
                try {
                    requireCalendarPermission(folder, READ_FOLDER, READ_OWN_OBJECTS, NO_PERMISSIONS, NO_PERMISSIONS);
                    List<Event> eventsInFolder = storage.getEventStorage().searchEvents(getFolderIdTerm(session, folder), searchOptions, fields);
                    eventsForCalendarUser.addAll(eventsInFolder);
                    eventsPerFolder.put(folder, eventsInFolder);
                } catch (OXException e) {
                    resultsPerFolderId.put(folder.getId(), new DefaultEventsResult(e));
                }
            }
            /*
             * batch-load additional event data for this calendar user
             */
            storage.getUtilities().loadAdditionalEventData(i(entry.getKey()), eventsForCalendarUser, fields);
        }
        /*
         * post process events, based on each requested folder's perspective
         */
        boolean skipClassified = isSkipClassifiedEvents(session);
        for (Entry<CalendarFolder, List<Event>> entry : eventsPerFolder.entrySet()) {
            resultsPerFolderId.put(entry.getKey().getId(), new DefaultEventsResult(postProcess(entry.getValue(), entry.getKey(), skipClassified, fields)));
            getSelfProtection().checkResultMap(resultsPerFolderId);
        }
        return resultsPerFolderId;
    }

    /**
     * Gets all events the current session user attends.
     *
     * @param partStats The participation status to include, or <code>null</code> to include all events independently of the user
     *            attendee's participation status
     * @param rsvp The reply expectation to include, or <code>null</code> to include all events independently of the user attendee's
     *            rsvp status
     * @param folderTypes The folder types to include, or <code>null</code> to include all events independently of the type of the folder
     *            they're located in
     * @return The loaded events
     */
    private List<Event> getEventsOfUser(Boolean rsvp, ParticipationStatus[] partStats, Type[] folderTypes) throws OXException {
        /*
         * search for events the current session's user attends
         */
        SearchTerm<?> searchTerm = getSearchTerm(AttendeeField.ENTITY, SingleOperation.EQUALS, I(session.getUserId()));
        if (null != rsvp) {
            /*
             * only include events with matching rsvp
             */
            searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
                .addSearchTerm(searchTerm)
                .addSearchTerm(getSearchTerm(AttendeeField.RSVP, SingleOperation.EQUALS, rsvp))
            ;
        }
        if (null != partStats) {
            /*
             * only include events with matching participation status
             */
            if (0 == partStats.length) {
                return Collections.emptyList();
            }
            if (1 == partStats.length) {
                searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
                    .addSearchTerm(searchTerm)
                    .addSearchTerm(getSearchTerm(AttendeeField.PARTSTAT, SingleOperation.EQUALS, partStats[0]))
                ;
            } else {
                CompositeSearchTerm orTerm = new CompositeSearchTerm(CompositeOperation.OR);
                for (ParticipationStatus partStat : partStats) {
                    orTerm.addSearchTerm(getSearchTerm(AttendeeField.PARTSTAT, SingleOperation.EQUALS, partStat));
                }
                searchTerm = new CompositeSearchTerm(CompositeOperation.AND).addSearchTerm(searchTerm).addSearchTerm(orTerm);
            }
        }
        if (false == isEnforceDefaultAttendee(session)) {
            /*
             * also include not group-scheduled events associated with the calendar user
             */
            searchTerm = new CompositeSearchTerm(CompositeOperation.OR)
                .addSearchTerm(getSearchTerm(EventField.CALENDAR_USER, SingleOperation.EQUALS, I(session.getUserId())))
                .addSearchTerm(searchTerm)
            ;
        }
        boolean includePrivate = null == folderTypes || Arrays.contains(folderTypes, PrivateType.getInstance());
        boolean includePublic = null == folderTypes || Arrays.contains(folderTypes, PublicType.getInstance());
        if (includePublic && false == includePrivate) {
            /*
             * only include events in public folders (that have a common folder identifier assigned)
             */
            searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
                .addSearchTerm(new CompositeSearchTerm(CompositeOperation.NOT)
                    .addSearchTerm(getSearchTerm(EventField.FOLDER_ID, SingleOperation.ISNULL)
            ));
        } else if (false == includePublic && includePrivate) {
            /*
             * only include events in non-public folders (that have no common folder identifier assigned)
             */
            searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
                .addSearchTerm(getSearchTerm(EventField.FOLDER_ID, SingleOperation.ISNULL)
            );
        }
        /*
         * perform search & userize the results for the current session's user
         */
        EventField[] fields = getFields(session, EventField.ORGANIZER, EventField.ATTENDEES);
        List<Event> events = storage.getEventStorage().searchEvents(searchTerm, new SearchOptions(session), fields);
        events = storage.getUtilities().loadAdditionalEventData(session.getUserId(), events, fields);
        return postProcess(events, session.getUserId(), false, fields);
    }

    /**
     * Performs the operation.
     *
     * @param folderId The identifier of the parent folder to get all events from
     * @return The loaded events
     */
    public List<Event> perform(String folderId) throws OXException {
        /*
         * perform search & userize the results based on the requested folder
         */
        CalendarFolder folder = getFolder(session, folderId);
        requireCalendarPermission(folder, READ_FOLDER, READ_OWN_OBJECTS, NO_PERMISSIONS, NO_PERMISSIONS);
        SearchTerm<?> searchTerm = getFolderIdTerm(session, folder);
        EventField[] fields = getFields(session);
        /*
         * get events with default fields & load additional event data as needed
         */
        List<Event> events = storage.getEventStorage().searchEvents(searchTerm, new SearchOptions(session), fields);
        events = storage.getUtilities().loadAdditionalEventData(getCalendarUserId(folder), events, fields);
        return postProcess(events, folder, isSkipClassifiedEvents(session), fields);
    }

    private static Map<Integer, List<CalendarFolder>> getFoldersPerCalendarUserId(List<CalendarFolder> folders) {
        Map<Integer, List<CalendarFolder>> foldersPerCalendarUserId = new HashMap<Integer, List<CalendarFolder>>();
        for (CalendarFolder folder : folders) {
            com.openexchange.tools.arrays.Collections.put(foldersPerCalendarUserId, I(getCalendarUserId(folder)), folder);
        }
        return foldersPerCalendarUserId;
    }

}
