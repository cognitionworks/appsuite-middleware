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

import static com.openexchange.chronos.impl.Check.requireCalendarPermission;
import static com.openexchange.chronos.impl.Utils.getFields;
import static com.openexchange.chronos.impl.Utils.getFolder;
import static com.openexchange.chronos.impl.Utils.getSearchTerm;
import static com.openexchange.chronos.impl.Utils.getVisibleFolders;
import static com.openexchange.folderstorage.Permission.NO_PERMISSIONS;
import static com.openexchange.folderstorage.Permission.READ_FOLDER;
import static com.openexchange.folderstorage.Permission.READ_OWN_OBJECTS;
import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.common.mapping.EventMapper;
import com.openexchange.chronos.impl.Check;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.SearchFilter;
import com.openexchange.chronos.service.SearchOptions;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.java.Strings;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;

/**
 * {@link SearchPerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class SearchPerformer extends AbstractQueryPerformer {

    private final int minimumSearchPatternLength;

    /**
     * Initializes a new {@link SearchPerformer}.
     *
     * @param session The calendar session
     * @param storage The underlying calendar storage
     */
    public SearchPerformer(CalendarSession session, CalendarStorage storage) throws OXException {
        super(session, storage);
        this.minimumSearchPatternLength = session.getConfig().getMinimumSearchPatternLength();
    }

    /**
     * Performs the operation.
     *
     * @param folderIDs The identifiers of the folders to perform the search in, or <code>null</code> to search all visible folders
     * @param pattern The pattern to search for
     * @return The found events
     */
    public List<Event> perform(String[] folderIDs, String pattern) throws OXException {
        return perform(folderIDs, null, Collections.singletonList(pattern));
    }

    /**
     * Performs the operation.
     *
     * @param folderIDs The identifiers of the folders to perform the search in, or <code>null</code> to search all visible folders
     * @param filters A list of additional filters to be applied on the search, or <code>null</code> if not specified
     * @param queries The queries to search for
     * @return The found events
     */
    public List<Event> perform(String[] folderIDs, List<SearchFilter> filters, List<String> queries) throws OXException {
        List<UserizedFolder> folders = getFolders(folderIDs);
        EventField[] fields = getFields(session, EventField.ORGANIZER, EventField.ATTENDEES);
        SearchOptions sortOptions = new SearchOptions(session);
        List<Event> events = new ArrayList<Event>();
        List<Event> foundEvents = storage.getEventStorage().searchEvents(buildSearchTerm(folders, queries), filters, sortOptions, fields);
        for (Event event : storage.getUtilities().loadAdditionalEventData(-1, foundEvents, fields)) {
            List<UserizedFolder> foldersForEvent = getFoldersForEvent(folders, event);
            if (1 == foldersForEvent.size()) {
                events.addAll(postProcess(Collections.singletonList(event), foldersForEvent.get(0), false));
            } else {
                for (UserizedFolder folder : foldersForEvent) {
                    Event copiedEvent = EventMapper.getInstance().copy(event, new Event(), (EventField[]) null);
                    events.addAll(postProcess(Collections.singletonList(copiedEvent), folder, false));
                }
            }
            getSelfProctection().checkEventCollection(events);
        }
        return sortEvents(events);
    }

    private SearchTerm<?> buildSearchTerm(List<UserizedFolder> folders, List<String> queries) throws OXException {
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND).addSearchTerm(getFolderIdsTerm(folders));
        if (null != queries) {
            for (String query : queries) {
                if (false == isWildcardOnly(query)) {
                    String pattern = addWildcards(Check.minimumSearchPatternLength(query, minimumSearchPatternLength), true, true);
                    searchTerm.addSearchTerm(new CompositeSearchTerm(CompositeOperation.OR)
                        .addSearchTerm(getSearchTerm(EventField.SUMMARY, SingleOperation.EQUALS, pattern))
                        .addSearchTerm(getSearchTerm(EventField.DESCRIPTION, SingleOperation.EQUALS, pattern))
                        .addSearchTerm(getSearchTerm(EventField.CATEGORIES, SingleOperation.EQUALS, pattern))
                    );
                }
            }
        }
        return 1 == searchTerm.getOperands().length ? searchTerm.getOperands()[0] : searchTerm;
    }

    private static SearchTerm<?> getPublicFolderIdsTerm(Set<String> folderIDs, boolean onlyOwn, int userID) {
        if (null == folderIDs || 0 == folderIDs.size()) {
            return null;
        }
        SearchTerm<?> searchTerm;
        if (1 == folderIDs.size()) {
            searchTerm = getSearchTerm(EventField.FOLDER_ID, SingleOperation.EQUALS, folderIDs.iterator().next());
        } else {
            CompositeSearchTerm orTerm = new CompositeSearchTerm(CompositeOperation.OR);
            for (String folderID : folderIDs) {
                orTerm.addSearchTerm(getSearchTerm(EventField.FOLDER_ID, SingleOperation.EQUALS, folderID));
            }
            searchTerm = orTerm;
        }
        if (onlyOwn) {
            searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
                .addSearchTerm(getSearchTerm(EventField.CREATED_BY, SingleOperation.EQUALS, I(userID)))
                .addSearchTerm(searchTerm);
        }
        return searchTerm;
    }

    private static SearchTerm<?> getPersonalFolderIdsTerm(Entry<Integer, Set<String>> personalFolderIDs, boolean onlyOwn, int userID) {
        Set<String> folderIDs = personalFolderIDs.getValue();
        Integer entityID = personalFolderIDs.getKey();
        if (null == folderIDs || 0 == folderIDs.size()) {
            return null;
        }
        SearchTerm<?> eventFolderTerm;
        SearchTerm<?> attendeeFolderTerm;
        if (1 == folderIDs.size()) {
            String folderID = folderIDs.iterator().next();
            eventFolderTerm = getSearchTerm(EventField.FOLDER_ID, SingleOperation.EQUALS, folderID);
            attendeeFolderTerm = getSearchTerm(AttendeeField.FOLDER_ID, SingleOperation.EQUALS, folderID);
        } else {
            CompositeSearchTerm compositeEventFolderTerm = new CompositeSearchTerm(CompositeOperation.OR);
            CompositeSearchTerm compositeAttendeeFolderTerm = new CompositeSearchTerm(CompositeOperation.OR);
            for (String folderID : folderIDs) {
                compositeEventFolderTerm.addSearchTerm(getSearchTerm(EventField.FOLDER_ID, SingleOperation.EQUALS, folderID));
                compositeAttendeeFolderTerm.addSearchTerm(getSearchTerm(AttendeeField.FOLDER_ID, SingleOperation.EQUALS, folderID));
            }
            eventFolderTerm = compositeEventFolderTerm;
            attendeeFolderTerm = compositeAttendeeFolderTerm;
        }
        SearchTerm<?> searchTerm = new CompositeSearchTerm(CompositeOperation.OR)
            .addSearchTerm(eventFolderTerm)
            .addSearchTerm(new CompositeSearchTerm(CompositeOperation.AND)
                .addSearchTerm(getSearchTerm(AttendeeField.ENTITY, SingleOperation.EQUALS, I(entityID)))
                .addSearchTerm(attendeeFolderTerm))
        ;
        if (onlyOwn) {
            searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
                .addSearchTerm(searchTerm)
                .addSearchTerm(getSearchTerm(EventField.CREATED_BY, SingleOperation.EQUALS, userID));
        }
        return searchTerm;
    }

    private static SearchTerm<?> getFolderIdsTerm(List<UserizedFolder> folders) throws OXException {
        if (null == folders || 0 == folders.size()) {
            return null;
        }
        int userID = folders.get(0).getSession().getUserId();
        /*
         * distinguish between public / non-public folders, and "read all" / "read only own" permissions
         */
        Set<String> publicFolders = new HashSet<String>();
        Set<String> publicFoldersOnlyOwn = new HashSet<String>();
        Map<Integer, Set<String>> personalFoldersPerEntity = new HashMap<Integer, Set<String>>();
        Map<Integer, Set<String>> personalFoldersPerEntityOnlyOwn = new HashMap<Integer, Set<String>>();
        for (UserizedFolder folder : folders) {
            requireCalendarPermission(folder, READ_FOLDER, READ_OWN_OBJECTS, NO_PERMISSIONS, NO_PERMISSIONS);
            String folderID = folder.getID();
            if (PublicType.getInstance().equals(folder.getType())) {
                if (folder.getOwnPermission().getReadPermission() < Permission.READ_ALL_OBJECTS) {
                    publicFoldersOnlyOwn.add(folderID);
                } else {
                    publicFolders.add(folderID);
                }
            } else {
                Integer entityID = I(folder.getCreatedBy());
                if (folder.getOwnPermission().getReadPermission() < Permission.READ_ALL_OBJECTS) {
                    Set<String> personalFolders = personalFoldersPerEntityOnlyOwn.get(entityID);
                    if (null == personalFolders) {
                        personalFolders = new HashSet<String>();
                        personalFoldersPerEntityOnlyOwn.put(entityID, personalFolders);
                    }
                    personalFolders.add(folderID);
                } else {
                    Set<String> personalFolders = personalFoldersPerEntity.get(entityID);
                    if (null == personalFolders) {
                        personalFolders = new HashSet<String>();
                        personalFoldersPerEntity.put(entityID, personalFolders);
                    }
                    personalFolders.add(folderID);
                }
            }
        }
        /*
         * construct search term
         */
        CompositeSearchTerm compositeTerm = new CompositeSearchTerm(CompositeOperation.OR);
        if (0 < publicFolders.size()) {
            compositeTerm.addSearchTerm(getPublicFolderIdsTerm(publicFolders, false, userID));
        }
        if (0 < publicFoldersOnlyOwn.size()) {
            compositeTerm.addSearchTerm(getPublicFolderIdsTerm(publicFoldersOnlyOwn, true, userID));
        }
        for (Entry<Integer, Set<String>> entry : personalFoldersPerEntity.entrySet()) {
            compositeTerm.addSearchTerm(getPersonalFolderIdsTerm(entry, false, userID));
        }
        for (Entry<Integer, Set<String>> entry : personalFoldersPerEntityOnlyOwn.entrySet()) {
            compositeTerm.addSearchTerm(getPersonalFolderIdsTerm(entry, true, userID));
        }
        return 1 == compositeTerm.getOperands().length ? compositeTerm.getOperands()[0] : compositeTerm;
    }

    private List<UserizedFolder> getFolders(String[] folderIDs) throws OXException {
        List<UserizedFolder> folders;
        if (null == folderIDs) {
            folders = getVisibleFolders(session);
        } else {
            folders = new ArrayList<UserizedFolder>(folderIDs.length);
            for (String folderID : folderIDs) {
                folders.add(getFolder(session, folderID));
            }
        }
        return folders;
    }

    private static List<UserizedFolder> getFoldersForEvent(List<UserizedFolder> possibleFolders, Event event) throws OXException {
        List<UserizedFolder> folders = new ArrayList<UserizedFolder>();
        for (UserizedFolder folder : possibleFolders) {
            if (Utils.isInFolder(event, folder)) {
                folders.add(folder);
            }
        }
        return folders;
    }

    private static boolean isWildcardOnly(String query) {
        return Strings.isEmpty(query) || "*".equals(query);
    }

    private static String addWildcards(String pattern, boolean prepend, boolean append) {
        if ((null == pattern || 0 == pattern.length()) && (append || prepend)) {
            return "*";
        }
        if (null != pattern) {
            if (prepend && '*' != pattern.charAt(0)) {
                pattern = "*" + pattern;
            }
            if (append && '*' != pattern.charAt(pattern.length() - 1)) {
                pattern = pattern + "*";
            }
        }
        return pattern;
    }

}
