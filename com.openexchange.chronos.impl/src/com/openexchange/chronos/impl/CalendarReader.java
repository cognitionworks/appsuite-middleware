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

package com.openexchange.chronos.impl;

import static com.openexchange.chronos.impl.CalendarUtils.appendCommonTerms;
import static com.openexchange.chronos.impl.CalendarUtils.getFolderIdTerm;
import static com.openexchange.chronos.impl.CalendarUtils.getSearchTerm;
import static com.openexchange.chronos.impl.CalendarUtils.isAttendee;
import static com.openexchange.chronos.impl.Check.requireCalendarContentType;
import static com.openexchange.chronos.impl.Check.requireFolderPermission;
import static com.openexchange.chronos.impl.Check.requireReadPermission;
import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.CalendarService;
import com.openexchange.chronos.CalendarSession;
import com.openexchange.chronos.CalendarStorage;
import com.openexchange.chronos.CalendarStorageFactory;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.EventID;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.UserizedEvent;
import com.openexchange.chronos.impl.osgi.Services;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.FolderResponse;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.Type;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.folderstorage.database.contentType.CalendarContentType;
import com.openexchange.folderstorage.type.PrivateType;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.folderstorage.type.SharedType;
import com.openexchange.group.Group;
import com.openexchange.group.GroupService;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.ldap.User;
import com.openexchange.java.Autoboxing;
import com.openexchange.resource.Resource;
import com.openexchange.resource.ResourceService;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.search.internal.operands.ColumnFieldOperand;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.user.UserService;

/**
 * {@link CalendarService}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class CalendarReader {

    protected final CalendarSession session;
    protected final CalendarStorage storage;

    /**
     * Initializes a new {@link CalendarReader}.
     *
     * @param session The session
     */
    public CalendarReader(CalendarSession session) throws OXException {
        this(session, Services.getService(CalendarStorageFactory.class).create(session.getContext()));
    }

    /**
     * Initializes a new {@link CalendarReader}.
     *
     * @param session The session
     * @param storage The storage
     */
    public CalendarReader(CalendarSession session, CalendarStorage storage) {
        super();
        this.session = session;
        this.storage = storage;
    }

    public boolean[] hasEventsBetween(int userID, Date from, Date until, TimeZone timeZone) throws OXException {
        List<Boolean> hasEventsList = new ArrayList<Boolean>();
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
            .addSearchTerm(getSearchTerm(EventField.END_DATE, SingleOperation.GREATER_OR_EQUAL, from))
            .addSearchTerm(getSearchTerm(EventField.START_DATE, SingleOperation.LESS_THAN, until))
            .addSearchTerm(getSearchTerm(AttendeeField.ENTITY, SingleOperation.EQUALS, I(userID)))
            //TODO .addSearchTerm(getSearchTerm(AttendeeField.PARTSTAT, SingleOperation.NOT_EQUALS, ParticipationStatus.DECLINED))
        ;
        List<Event> events = storage.searchEvents(searchTerm, null);
        Calendar calendar = GregorianCalendar.getInstance(timeZone);
        calendar.setTime(from);
        Date minimumEndTime = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date maximumStartTime = calendar.getTime();
        while (maximumStartTime.before(until)) {
            Boolean hasEvents = Boolean.FALSE;
            for (Event event : events) {
                Attendee attendee = CalendarUtils.find(event.getAttendees(), userID);
                if (null == attendee || ParticipationStatus.DECLINED.equals(attendee.getPartStat())) {
                    continue; // skip
                }
                if (event.getRecurrenceId() == event.getId()) {
                    //TODO recurring: Services.getService(RecurrenceService.class).calculateInstances(event, start, end, -1); ...
                    continue;
                } else {
                    Date startDate = event.isAllDay() ? CalendarUtils.getDateInTimeZone(event.getStartDate(), timeZone) : event.getStartDate();
                    Date endDate = event.isAllDay() ? CalendarUtils.getDateInTimeZone(event.getEndDate(), timeZone) : event.getEndDate();
                    if (startDate.before(maximumStartTime) && endDate.after(minimumEndTime)) {
                        hasEvents = Boolean.TRUE;
                        break;
                    }
                }
            }
            hasEventsList.add(hasEvents);
            minimumEndTime = maximumStartTime;
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            maximumStartTime = calendar.getTime();
        }
        boolean[] hasEventsArray = new boolean[hasEventsList.size()];
        for (int i = 0; i < hasEventsArray.length; i++) {
            hasEventsArray[i] = hasEventsList.get(i).booleanValue();
        }
        return hasEventsArray;
    }

    public int resolveUid(String uid) throws OXException {
        /*
         * construct search term
         */
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
            .addSearchTerm(getSearchTerm(EventField.UID, SingleOperation.EQUALS, uid))
            .addSearchTerm(new CompositeSearchTerm(CompositeOperation.OR)
                .addSearchTerm(getSearchTerm(EventField.RECURRENCE_ID, SingleOperation.ISNULL))
                .addSearchTerm(getSearchTerm(EventField.ID, SingleOperation.EQUALS, new ColumnFieldOperand<EventField>(EventField.RECURRENCE_ID)))
            )
        ;
        /*
         * search for an event matching the UID
         */
        List<Event> events = storage.searchEvents(searchTerm, new EventField[] { EventField.ID });
        return 0 < events.size() ? events.get(0).getId() : 0;
    }

    public List<UserizedEvent> getChangeExceptions(int folderID, int objectID, EventField[] fields) throws OXException {
        return getChangeExceptions(getFolder(folderID), objectID, fields);
    }

    public List<UserizedEvent> searchEvents(int[] folderIDs, String pattern, EventField[] fields) throws OXException {
        List<UserizedFolder> folders;
        if (null == folderIDs || 0 == folderIDs.length) {
            folders = getVisibleFolders();
        } else {
            folders = new ArrayList<UserizedFolder>(folderIDs.length);
            for (int folderID : folderIDs) {
                folders.add(getFolder(folderID));
            }
        }
        return searchEvents(folders, pattern, fields);
    }

    protected List<UserizedEvent> searchEvents(List<UserizedFolder> folders, String pattern, EventField[] fields) throws OXException {
        if (null == folders || 0 == folders.size()) {
            return Collections.emptyList();
        }
        Check.requireMinimumSearchPatternLength(pattern);
        CompositeSearchTerm parentFolderTerm = new CompositeSearchTerm(CompositeOperation.OR);
        for (UserizedFolder folder : folders) {
            requireCalendarContentType(folder);
            requireFolderPermission(folder, Permission.READ_FOLDER);
            requireReadPermission(folder, Permission.READ_OWN_OBJECTS);
            parentFolderTerm.addSearchTerm(getFolderIdTerm(folder));
        }
        String wildcardPattern = pattern.startsWith("*") ? pattern : '*' + pattern;
        wildcardPattern = wildcardPattern.endsWith("*") ? wildcardPattern : wildcardPattern + '*';
        CompositeSearchTerm patternTerm = new CompositeSearchTerm(CompositeOperation.OR)
            .addSearchTerm(getSearchTerm(EventField.SUMMARY, SingleOperation.EQUALS, wildcardPattern))
            .addSearchTerm(getSearchTerm(EventField.DESCRIPTION, SingleOperation.EQUALS, wildcardPattern))
        ;
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
            .addSearchTerm(parentFolderTerm).addSearchTerm(patternTerm);
        List<Event> events = storage.searchEvents(searchTerm, fields);
        return userize(events, session.getUser().getId());
    }

    public UserizedEvent readEvent(EventID eventID, EventField[] fields) throws OXException {
        return readEvent(eventID.getFolderID(), eventID.getObjectID(), fields);
    }

    public UserizedEvent readEvent(int folderID, int objectID, EventField[] fields) throws OXException {
        return readEvent(getFolder(folderID), objectID, fields);
    }

    public List<UserizedEvent> readEventsInFolder(int folderID, Date from, Date until, Date updatedSince, EventField[] fields) throws OXException {
        return readEventsInFolder(getFolder(folderID), false, from, until, updatedSince, fields);
    }

    public List<UserizedEvent> readDeletedEventsInFolder(int folderID, Date from, Date until, Date deletedSince, EventField[] fields) throws OXException {
        return readEventsInFolder(getFolder(folderID), true, from, until, deletedSince, fields);
    }

    public List<UserizedEvent> readEventsOfUser(int userID, Date from, Date until, Date updatedSince, EventField[] fields) throws OXException {
        return readEventsOfUser(userID, false, from, until, updatedSince, fields);
    }

    public List<UserizedEvent> readDeletedEventsOfUser(int userID, Date from, Date until, Date deletedSince, EventField[] fields) throws OXException {
        return readEventsOfUser(userID, true, from, until, deletedSince, fields);
    }

    protected UserizedEvent readEvent(UserizedFolder folder, int objectID, EventField[] fields) throws OXException {
        requireCalendarContentType(folder);
        requireFolderPermission(folder, Permission.READ_FOLDER);
        requireReadPermission(folder, Permission.READ_OWN_OBJECTS);
        Event event = storage.loadEvent(objectID, fields);
        if (session.getUser().getId() != event.getCreatedBy()) {
            requireReadPermission(folder, Permission.READ_ALL_OBJECTS);
        }
        // TODO: check that event is actually located in the requested folder (and accessible by user)
        return userize(event, folder);
    }

    protected List<UserizedEvent> getChangeExceptions(UserizedFolder folder, int objectID, EventField[] fields) throws OXException {
        requireCalendarContentType(folder);
        requireFolderPermission(folder, Permission.READ_FOLDER);
        requireReadPermission(folder, Permission.READ_OWN_OBJECTS);
        /*
         * construct search term
         */
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
            .addSearchTerm(getFolderIdTerm(folder))
            .addSearchTerm(getSearchTerm(EventField.RECURRENCE_ID, SingleOperation.EQUALS, I(objectID)))
            .addSearchTerm(getSearchTerm(EventField.ID, SingleOperation.NOT_EQUALS, new ColumnFieldOperand<EventField>(EventField.RECURRENCE_ID)))
        ;
        /*
         * perform search & userize the results
         */
        List<Event> events = storage.searchEvents(searchTerm, fields);
        return userize(events, folder);
    }

    protected List<UserizedEvent> readEventsInFolder(UserizedFolder folder, boolean deleted, Date from, Date until, Date updatedSince, EventField[] fields) throws OXException {
        requireCalendarContentType(folder);
        requireFolderPermission(folder, Permission.READ_FOLDER);
        requireReadPermission(folder, Permission.READ_OWN_OBJECTS);
        /*
         * construct search term
         */
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND).addSearchTerm(getFolderIdTerm(folder));
        appendCommonTerms(searchTerm, from, until, updatedSince);
        /*
         * perform search & userize the results
         */
        List<Event> events = deleted ? storage.searchDeletedEvents(searchTerm, fields) : storage.searchEvents(searchTerm, fields);
        return userize(events, folder);
    }

    protected List<UserizedEvent> readEventsOfUser(int userID, boolean deleted, Date from, Date until, Date updatedSince, EventField[] fields) throws OXException {
        /*
         * construct search term
         */
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
            .addSearchTerm(getSearchTerm(AttendeeField.ENTITY, SingleOperation.EQUALS, I(userID)));
        appendCommonTerms(searchTerm, from, until, updatedSince);
        /*
         * perform search & userize the results for the current session's user
         */
        List<Event> events = deleted ? storage.searchDeletedEvents(searchTerm, fields) : storage.searchEvents(searchTerm, fields);
        return userize(events, session.getUser().getId());
    }

    private List<UserizedEvent> userize(List<Event> events, int forUser) throws OXException {
        List<UserizedEvent> userizedEvents = new ArrayList<UserizedEvent>(events.size());
        Map<Integer, List<Alarm>> alarmsById = readAlarms(events, forUser);
        for (Event event : events) {
            UserizedEvent userizedEvent = new UserizedEvent(session.getSession(), event);
            if (0 < event.getPublicFolderId()) {
                userizedEvent.setFolderId(event.getPublicFolderId());
            }
            Attendee userAttendee = CalendarUtils.find(event.getAttendees(), forUser);
            if (null != userAttendee) {
                userizedEvent.setAlarms(alarmsById.get(I(event.getId())));
                if (0 < userAttendee.getFolderID()) {
                    userizedEvent.setFolderId(userAttendee.getFolderID());
                }
            }
            userizedEvents.add(userizedEvent);
        }
        return userizedEvents;
    }

    private UserizedEvent userize(Event event, UserizedFolder inFolder) throws OXException {
        return userize(Collections.singletonList(event), inFolder).get(0);
    }

    private Map<Integer, List<Alarm>> readAlarms(List<Event> events, int userID) throws OXException {
        List<Integer> objectIDs = new ArrayList<Integer>(events.size());
        for (Event event : events) {
            if (isAttendee(event, userID)) {
                objectIDs.add(I(event.getId()));
            }
        }
        return 0 < objectIDs.size() ? storage.loadAlarms(Autoboxing.I2i(objectIDs), userID) : Collections.<Integer, List<Alarm>> emptyMap();
    }

    private List<UserizedEvent> userize(List<Event> events, UserizedFolder inFolder) throws OXException {
        User calendarUser = getCalendarUser(inFolder);
        int folderID = Integer.parseInt(inFolder.getID());
        List<UserizedEvent> userizedEvents = new ArrayList<UserizedEvent>(events.size());
        Map<Integer, List<Alarm>> alarmsById = readAlarms(events, calendarUser.getId());
        for (Event event : events) {
            UserizedEvent userizedEvent = new UserizedEvent(session.getSession(), event);
            userizedEvent.setFolderId(folderID);
            userizedEvent.setAlarms(alarmsById.get(I(event.getId())));
            userizedEvents.add(userizedEvent);
        }
        return userizedEvents;
    }

    protected UserizedFolder getFolder(int folderID) throws OXException {
        return Services.getService(FolderService.class).getFolder(FolderStorage.REAL_TREE_ID, String.valueOf(folderID), session.getSession(), null);
    }

    protected List<UserizedFolder> getVisibleFolders() throws OXException {
        return getVisibleFolders(PrivateType.getInstance(), SharedType.getInstance(), PublicType.getInstance());
    }

    protected List<UserizedFolder> getVisibleFolders(Type... types) throws OXException {
        List<UserizedFolder> visibleFolders = new ArrayList<UserizedFolder>();
        FolderService folderService = Services.getService(FolderService.class);
        for (Type type : types) {
            FolderResponse<UserizedFolder[]> response = folderService.getVisibleFolders(FolderStorage.REAL_TREE_ID, CalendarContentType.getInstance(), type, false, session.getSession(), null);
            UserizedFolder[] folders = response.getResponse();
            if (null != folders && 0 < folders.length) {
                visibleFolders.addAll(Arrays.asList(folders));
            }
        }
        return visibleFolders;
    }

    /**
     * Gets the actual target calendar user for a specific folder. This is either the current session's user for "private" or "public"
     * folders, or the folder owner for "shared" calendar folders.
     *
     * @param folder The folder to get the calendar user for
     * @return The calendar user
     */
    protected User getCalendarUser(UserizedFolder folder) throws OXException {
        return SharedType.getInstance().equals(folder.getType()) ? getUser(folder.getCreatedBy()) : session.getUser();
    }

    /**
     * Gets the "acting" calendar user for a specific folder, i.e. the proxy user who is acting on behalf of the calendar owner, which is
     * the current session's user in case the folder is a "shared" calendar, otherwise <code>null</code> for "private" or "public" folders.
     *
     * @param folder The folder to determine the proxy user for
     * @return The proxy calendar user, or <code>null</code> if the current session's user is acting on behalf of it's own
     */
    protected User getProxyUser(UserizedFolder folder) throws OXException {
        return SharedType.getInstance().equals(folder.getType()) ? getUser(folder.getCreatedBy()) : session.getUser();
    }

    protected User getUser(int userID) throws OXException {
        UserService userService = Services.getService(UserService.class);
        return userService.getUser(userID, session.getContext());
    }

    protected Group getGroup(int groupID) throws OXException {
        return Services.getService(GroupService.class).getGroup(session.getContext(), groupID);
    }

    protected Resource getResource(int resourceID) throws OXException {
        return Services.getService(ResourceService.class).getResource(resourceID, session.getContext());
    }

    protected int getDefaultFolderID(User user) throws OXException {
        //TODO: via higher level service?
        return new OXFolderAccess(session.getContext()).getDefaultFolderID(user.getId(), FolderObject.CALENDAR);
    }

}
