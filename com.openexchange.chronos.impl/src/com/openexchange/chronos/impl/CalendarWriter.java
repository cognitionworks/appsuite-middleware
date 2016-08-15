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

import static com.openexchange.chronos.impl.CalendarUtils.find;
import static com.openexchange.chronos.impl.CalendarUtils.getUserIDs;
import static com.openexchange.chronos.impl.CalendarUtils.i;
import static com.openexchange.chronos.impl.CalendarUtils.isAttendee;
import static com.openexchange.chronos.impl.CalendarUtils.isOrganizer;
import static com.openexchange.chronos.impl.Check.requireCalendarContentType;
import static com.openexchange.chronos.impl.Check.requireCalendarPermission;
import static com.openexchange.chronos.impl.Check.requireDeletePermission;
import static com.openexchange.chronos.impl.Check.requireUpToDateTimestamp;
import static com.openexchange.folderstorage.Permission.CREATE_OBJECTS_IN_FOLDER;
import static com.openexchange.folderstorage.Permission.DELETE_ALL_OBJECTS;
import static com.openexchange.folderstorage.Permission.DELETE_OWN_OBJECTS;
import static com.openexchange.folderstorage.Permission.NO_PERMISSIONS;
import static com.openexchange.folderstorage.Permission.READ_ALL_OBJECTS;
import static com.openexchange.folderstorage.Permission.READ_FOLDER;
import static com.openexchange.folderstorage.Permission.READ_OWN_OBJECTS;
import static com.openexchange.folderstorage.Permission.WRITE_ALL_OBJECTS;
import static com.openexchange.folderstorage.Permission.WRITE_OWN_OBJECTS;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.CalendarSession;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.EventID;
import com.openexchange.chronos.UserizedEvent;
import com.openexchange.chronos.impl.osgi.Services;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.chronos.storage.CalendarStorageFactory;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.groupware.ldap.User;
import com.openexchange.java.Strings;

/**
 * {@link CalendarWriter}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class CalendarWriter extends CalendarReader {

    /**
     * Initializes a new {@link CalendarWriter}.
     *
     * @param session The session
     */
    public CalendarWriter(CalendarSession session) throws OXException {
        this(session, Services.getService(CalendarStorageFactory.class).create(session.getContext()));
    }

    /**
     * Initializes a new {@link CalendarWriter}.
     *
     * @param session The session
     * @param storage The storage
     */
    public CalendarWriter(CalendarSession session, CalendarStorage storage) {
        super(session, storage);
    }

    public UserizedEvent insertEvent(UserizedEvent event) throws OXException {
        return insertEvent(getFolder(event.getFolderId()), event);
    }

    public UserizedEvent updateEvent(EventID eventID, UserizedEvent event, long clientTimestamp) throws OXException {
        return updateEvent(getFolder(eventID.getFolderID()), eventID.getObjectID(), event, clientTimestamp);
    }

    public UserizedEvent updateAttendee(int folderID, int objectID, Attendee attendee) throws OXException {
        return updateAttendee(getFolder(folderID), objectID, attendee);
    }

    public void deleteEvent(int folderID, int objectID, long clientTimestamp) throws OXException {
        deleteEvent(getFolder(folderID), objectID, clientTimestamp);
    }

    protected void deleteEvent(UserizedFolder folder, int objectID, long clientTimestamp) throws OXException {
        requireCalendarContentType(folder);
        requireDeletePermission(folder, DELETE_OWN_OBJECTS);
        Event originalEvent = storage.getEventStorage().loadEvent(objectID, null);
        if (null == originalEvent) {
            throw OXException.notFound(String.valueOf(objectID));//TODO
        }
        List<Attendee> originalAttendees = new ArrayList<Attendee>();
        requireUpToDateTimestamp(originalEvent, clientTimestamp);
        if (session.getUser().getId() != originalEvent.getCreatedBy()) {
            requireDeletePermission(folder, DELETE_ALL_OBJECTS);
        }
        Date now = new Date();
        User calendarUser = getCalendarUser(folder);
        if (isOrganizer(originalEvent, calendarUser.getId())) {
            /*
             * deletion by organizer
             */
            storage.getEventStorage().insertTombstoneEvent(EventMapper.getInstance().getTombstone(originalEvent, now, calendarUser.getId()));
            storage.getAttendeeStorage().insertTombstoneAttendees(objectID, AttendeeMapper.getInstance().getTombstones(originalAttendees));
            storage.getAlarmStorage().deleteAlarms(objectID);
            storage.getEventStorage().deleteEvent(objectID);
            storage.getAttendeeStorage().deleteAttendees(objectID);

        } else if (CalendarUtils.contains(originalAttendees, calendarUser.getId())) {
            /*
             * deletion as attendee
             */
            if (1 == originalEvent.getAttendees().size()) {
                Event tombstoneEvent = EventMapper.getInstance().getTombstone(originalEvent, now, calendarUser.getId());
                storage.getAttendeeStorage().insertTombstoneAttendees(objectID, AttendeeMapper.getInstance().getTombstones(originalAttendees));
                storage.getEventStorage().insertTombstoneEvent(tombstoneEvent);
                storage.getAlarmStorage().deleteAlarms(objectID);
                storage.getEventStorage().deleteEvent(objectID);
                storage.getAttendeeStorage().deleteAttendees(objectID);
            } else {
                Attendee originalAttendee = CalendarUtils.find(originalAttendees, calendarUser.getId());
                storage.getEventStorage().insertTombstoneEvent(EventMapper.getInstance().getTombstone(originalEvent, now, calendarUser.getId()));
                storage.getAttendeeStorage().insertTombstoneAttendee(objectID, originalAttendee);
                storage.getAlarmStorage().deleteAlarms(objectID, calendarUser.getId());
                storage.getAttendeeStorage().deleteAttendees(objectID);
                Event eventUpdate = new Event();
                eventUpdate.setId(objectID);
                Consistency.setModified(now, eventUpdate, calendarUser.getId());
                storage.getEventStorage().updateEvent(eventUpdate);
            }
        } else {
            /*
             * deletion as ?
             */

        }
    }

    private UserizedEvent insertEvent(UserizedFolder folder, UserizedEvent userizedEvent) throws OXException {
        requireCalendarPermission(folder, CREATE_OBJECTS_IN_FOLDER, NO_PERMISSIONS, WRITE_OWN_OBJECTS, NO_PERMISSIONS);
        Event event = userizedEvent.getEvent();
        User calendarUser = getCalendarUser(folder);
        Date now = new Date();
        Consistency.setCreated(now, event, calendarUser.getId());
        Consistency.setModified(now, event, session.getUser().getId());
        if (null == event.getOrganizer()) {
            Consistency.setOrganizer(event, calendarUser, session.getUser());
        }
        Consistency.setTimeZone(event, calendarUser);
        Consistency.adjustAllDayDates(event);
        event.setSequence(0);
        if (Strings.isNotEmpty(event.getUid())) {
            if (0 < resolveUid(event.getUid())) {
                throw OXException.general("Duplicate uid"); //TODO
            }
        } else {
            event.setUid(UUID.randomUUID().toString());
        }
        event.setPublicFolderId(PublicType.getInstance().equals(folder.getType()) ? i(folder) : 0);
        /*
         * assign new object identifier
         */
        int objectID = storage.nextObjectID();
        event.setId(objectID);
        if (event.containsRecurrenceRule() && null != event.getRecurrenceRule()) {
            event.setSeriesId(objectID);
        }
        /*
         * insert event, attendees & alarms of user
         */
        storage.getEventStorage().insertEvent(event);
        storage.getAttendeeStorage().insertAttendees(objectID, new AttendeeHelper(session, folder, null, event.getAttendees()).getAttendeesToInsert());
        if (userizedEvent.containsAlarms() && null != userizedEvent.getAlarms() && 0 < userizedEvent.getAlarms().size()) {
            storage.getAlarmStorage().insertAlarms(objectID, calendarUser.getId(), userizedEvent.getAlarms());
        }
        return readEvent(folder, objectID);
    }

    private UserizedEvent updateEvent(UserizedFolder folder, int objectID, UserizedEvent userizedEvent, long clientTimestamp) throws OXException {
        /*
         * load original event data
         */
        Event originalEvent = storage.getEventStorage().loadEvent(objectID, null);
        if (null == originalEvent) {
            throw OXException.notFound(String.valueOf(objectID)); // TODO
        }
        /*
         * check current session user's permissions
         */
        if (session.getUser().getId() == originalEvent.getCreatedBy()) {
            requireCalendarPermission(folder, READ_FOLDER, READ_OWN_OBJECTS, WRITE_OWN_OBJECTS, NO_PERMISSIONS);
        } else {
            requireCalendarPermission(folder, READ_FOLDER, READ_ALL_OBJECTS, WRITE_ALL_OBJECTS, NO_PERMISSIONS);
        }
        requireUpToDateTimestamp(originalEvent, clientTimestamp);

        User calendarUser = getCalendarUser(folder);
        Date now = new Date();
        Event event = userizedEvent.getEvent();

        if (userizedEvent.containsFolderId() && 0 < userizedEvent.getFolderId() && i(folder) != userizedEvent.getFolderId()) {
            /*
             * move ...
             */
            UserizedFolder targetFolder = getFolder(userizedEvent.getFolderId());
            requireCalendarPermission(targetFolder, CREATE_OBJECTS_IN_FOLDER, NO_PERMISSIONS, NO_PERMISSIONS, NO_PERMISSIONS);
            if (session.getUser().getId() == originalEvent.getCreatedBy()) {
                requireCalendarPermission(folder, READ_FOLDER, NO_PERMISSIONS, NO_PERMISSIONS, DELETE_OWN_OBJECTS);
            } else {
                requireCalendarPermission(folder, READ_FOLDER, NO_PERMISSIONS, NO_PERMISSIONS, DELETE_ALL_OBJECTS);
            }
            if (PublicType.getInstance().equals(folder.getType()) && false == PublicType.getInstance().equals(targetFolder.getType()) ||
                PublicType.getInstance().equals(targetFolder.getType()) && false == PublicType.getInstance().equals(folder.getType())) {
                throw OXException.general("unsupported move");
            }
            User targetCalendarUser = getCalendarUser(targetFolder);
            if (PublicType.getInstance().equals(folder.getType()) && PublicType.getInstance().equals(targetFolder.getType())) {
                /*
                 * move from one public folder to another, update event's folder
                 */
                Event eventUpdate = new Event();
                eventUpdate.setId(objectID);
                eventUpdate.setPublicFolderId(i(targetFolder));
                Consistency.setModified(now, eventUpdate, calendarUser.getId());
                storage.getEventStorage().insertTombstoneEvent(EventMapper.getInstance().getTombstone(originalEvent, now, calendarUser.getId()));
                storage.getEventStorage().updateEvent(eventUpdate);
            } else if (calendarUser.getId() == targetCalendarUser.getId()) {
                /*
                 * move from one personal folder to another, update attendee's folder
                 */
                List<Attendee> originalAttendees = storage.getAttendeeStorage().loadAttendees(objectID);
                Attendee originalAttendee = find(originalAttendees, calendarUser.getId());
                if (null == originalAttendee) {
                    throw OXException.notFound(calendarUser.toString());
                }
                storage.getEventStorage().insertTombstoneEvent(EventMapper.getInstance().getTombstone(originalEvent, now, calendarUser.getId()));
                storage.getAttendeeStorage().insertTombstoneAttendee(objectID, AttendeeMapper.getInstance().getTombstone(originalAttendee));
                Attendee attendeeUpdate = new Attendee();
                AttendeeMapper.getInstance().copy(originalAttendee, attendeeUpdate, AttendeeField.ENTITY, AttendeeField.MEMBER, AttendeeField.CU_TYPE, AttendeeField.URI);
                attendeeUpdate.setFolderID(i(targetFolder));
                storage.getAttendeeStorage().updateAttendee(objectID, attendeeUpdate);
            } else {
                /*
                 * move from one personal folder to another user's personal folder, add or adjust corresponding default attendee
                 */
                List<Attendee> originalAttendees = storage.getAttendeeStorage().loadAttendees(objectID);
                //                Attendee attendee = find(originalAttendees, calendarUser.getId());
                //                if (null == attendee) {
                //                    throw OXException.notFound(calendarUser.toString());
                //                }
//                storage.getAttendeeStorage().insertTombstoneAttendees(objectID, Collections.singletonList(getTombstone(attendee)));
//                storage.getAttendeeStorage().deleteAttendees(objectID, Collections.singletonList(attendee));
                Attendee originalAttendee = find(originalAttendees, targetCalendarUser.getId());
                if (null != originalAttendee) {
                    storage.getEventStorage().insertTombstoneEvent(EventMapper.getInstance().getTombstone(originalEvent, now, calendarUser.getId()));
                    storage.getAttendeeStorage().insertTombstoneAttendee(objectID, AttendeeMapper.getInstance().getTombstone(originalAttendee));
                    Attendee attendeeUpdate = new Attendee();
                    AttendeeMapper.getInstance().copy(originalAttendee, attendeeUpdate, AttendeeField.ENTITY, AttendeeField.MEMBER, AttendeeField.CU_TYPE, AttendeeField.URI);
                    attendeeUpdate.setFolderID(i(targetFolder));
                    storage.getAttendeeStorage().updateAttendee(objectID, attendeeUpdate);
                } else {
                    originalAttendee = new AttendeeHelper(session, targetFolder, null, null).getAttendeesToInsert().get(0);
                    storage.getAttendeeStorage().insertAttendees(objectID, Collections.singletonList(originalAttendee));
                }
            }
            /*
             * take over new parent folder
             */
            folder = targetFolder;
        }
        if (isOrganizer(originalEvent, calendarUser.getId())) {
            /*
             * no organizer or update by (or on behalf of) organizer
             */
            if (event.containsAttendees()) {
                /*
                 * process any attendee updates
                 */
                List<Attendee> originalAttendees = storage.getAttendeeStorage().loadAttendees(objectID);
                AttendeeHelper attendeeHelper = new AttendeeHelper(session, folder, originalAttendees, event.getAttendees());
                List<Attendee> attendeesToDelete = attendeeHelper.getAttendeesToDelete();
                if (null != attendeesToDelete && 0 < attendeesToDelete.size()) {
                    if (CalendarUtils.contains(attendeesToDelete, calendarUser.getId())) {
                        throw OXException.general("cannot remove calendar user");
                    }
                    /*
                     * insert tombstone entries
                     */
                    storage.getEventStorage().insertTombstoneEvent(EventMapper.getInstance().getTombstone(originalEvent, now, calendarUser.getId()));
                    storage.getAttendeeStorage().insertTombstoneAttendees(objectID, AttendeeMapper.getInstance().getTombstones(attendeesToDelete));
                    /*
                     * remove attendee data
                     */
                    storage.getAttendeeStorage().deleteAttendees(objectID, attendeesToDelete);
                    storage.getAlarmStorage().deleteAlarms(objectID, getUserIDs(attendeesToDelete));
                }
                List<Attendee> attendeesToUpdate = attendeeHelper.getAttendeesToUpdate();
                if (null != attendeesToUpdate && 0 < attendeesToUpdate.size()) {
                    storage.getAttendeeStorage().updateAttendees(objectID, attendeesToUpdate);
                }
                List<Attendee> attendeesToInsert = attendeeHelper.getAttendeesToInsert();
                if (null != attendeesToInsert && 0 < attendeesToInsert.size()) {
                    storage.getAttendeeStorage().insertAttendees(objectID, attendeesToInsert);
                }
            }
            /*
             * update event data
             */
            Event eventUpdate = EventMapper.getInstance().getDifferences(originalEvent, event);
            for (EventField field : EventMapper.getInstance().getAssignedFields(eventUpdate)) {
                switch (field) {
                    case ALL_DAY:
                        /*
                         * adjust start- and enddate, too, if required
                         */
                        EventMapper.getInstance().copyIfNotSet(originalEvent, eventUpdate, EventField.START_DATE, EventField.END_DATE);
                        Consistency.adjustAllDayDates(eventUpdate);
                        break;
                    case RECURRENCE_RULE:
                        /*
                         * ensure all necessary recurrence related data is present in passed event update
                         */
                        EventMapper.getInstance().copyIfNotSet(originalEvent, eventUpdate, EventField.START_DATE, EventField.END_DATE, EventField.START_TIMEZONE, EventField.END_TIMEZONE, EventField.ALL_DAY);
                        break;
                    case UID:
                    case CREATED:
                    case CREATED_BY:
                    case SEQUENCE:
                    case SERIES_ID:
                        throw OXException.general("not allowed change");
                    default:
                        break;
                }
            }
            eventUpdate.setId(objectID);
            Consistency.setModified(now, eventUpdate, calendarUser.getId());
            eventUpdate.setSequence(originalEvent.getSequence() + 1);
            storage.getEventStorage().updateEvent(eventUpdate);
            /*
             * update alarms for calendar user
             */
            if (userizedEvent.containsAlarms()) {
                List<Alarm> alarms = userizedEvent.getAlarms();
                if (null == alarms) {
                    storage.getAlarmStorage().deleteAlarms(objectID, calendarUser.getId());
                } else {
                    storage.getAlarmStorage().updateAlarms(objectID, calendarUser.getId(), alarms);
                }
            }
        } else if (isAttendee(originalEvent, calendarUser.getId())) {
            /*
             * update by attendee
             */
            //TODO: allowed attendee changes
            throw new OXException();
        } else if (isAttendee(event, calendarUser.getId())) {
            /*
             * party crasher?
             */

        } else {
            /*
             * update by?
             */
        }
        return readEvent(folder, objectID);
    }

    private UserizedEvent updateAttendee(UserizedFolder folder, int objectID, Attendee attendee) throws OXException {
        /*
         * load original event data & target attendee
         */
        Event originalEvent = storage.getEventStorage().loadEvent(objectID, null);
        if (null == originalEvent) {
            throw OXException.notFound(String.valueOf(objectID));//TODO
        }
        Attendee originalAttendee = find(storage.getAttendeeStorage().loadAttendees(objectID), attendee);
        if (null == originalAttendee) {
            throw OXException.notFound(attendee.toString());//TODO
        }
        /*
         * check current session user's permissions
         */
        if (session.getUser().getId() == originalEvent.getCreatedBy()) {
            requireCalendarPermission(folder, READ_FOLDER, READ_OWN_OBJECTS, WRITE_OWN_OBJECTS, NO_PERMISSIONS);
        } else {
            requireCalendarPermission(folder, READ_FOLDER, READ_ALL_OBJECTS, WRITE_ALL_OBJECTS, NO_PERMISSIONS);
        }
        User calendarUser = getCalendarUser(folder);
        if (0 < originalAttendee.getEntity() && calendarUser.getId() != originalAttendee.getEntity() && session.getUser().getId() != originalAttendee.getEntity()) {
            // TODO: allowed for proxy user? calendarUser.getId() != originalAttendee.getEntity()
            throw OXException.general("can't confirm for someone else");
        }
        /*
         * determine & check modified fields
         */
        Attendee attendeeUpdate = AttendeeMapper.getInstance().getDifferences(originalAttendee, attendee);
        AttendeeField[] updatedFields = AttendeeMapper.getInstance().getAssignedFields(attendeeUpdate);
        if (0 == updatedFields.length) {
            // TODO or throw?
            return readEvent(folder, objectID);
        }
        for (AttendeeField field : AttendeeMapper.getInstance().getAssignedFields(attendeeUpdate)) {
            switch (field) {
                case FOLDER_ID:
                    /*
                     * move to other folder; perform additional checks
                     */
                    if (originalAttendee.getFolderID() != i(folder)) {
                        throw OXException.general("wrong source folder for update");
                    }
                    if (PublicType.getInstance().equals(folder.getType())) {
                        throw OXException.general("not allowed to move event from public to personal folder");
                    }
                    UserizedFolder targetFolder = getFolder(attendeeUpdate.getFolderID());
                    requireCalendarPermission(targetFolder, CREATE_OBJECTS_IN_FOLDER, NO_PERMISSIONS, NO_PERMISSIONS, NO_PERMISSIONS);
                    if (session.getUser().getId() == originalEvent.getCreatedBy()) {
                        requireCalendarPermission(folder, READ_FOLDER, NO_PERMISSIONS, NO_PERMISSIONS, DELETE_OWN_OBJECTS);
                    } else {
                        requireCalendarPermission(folder, READ_FOLDER, NO_PERMISSIONS, NO_PERMISSIONS, DELETE_ALL_OBJECTS);
                    }
                    if (folder.getCreatedBy() != targetFolder.getCreatedBy()) {
                        throw OXException.general("not allowed folder change for attendee");
                    }
                    break;
                case CU_TYPE:
                case ENTITY:
                case MEMBER:
                case URI:
                    throw OXException.general("not allowed change");
                default:
                    break;
            }
        }
        /*
         * perform update
         */
        Date now = new Date();
        AttendeeMapper.getInstance().copy(originalAttendee, attendeeUpdate, AttendeeField.ENTITY, AttendeeField.MEMBER, AttendeeField.CU_TYPE, AttendeeField.URI);
        if (session.getUser().getId() != calendarUser.getId() && false == attendeeUpdate.containsSentBy()) {
            attendeeUpdate.setSentBy(CalendarUtils.getCalAddress(session.getUser()));
        }
        if (attendeeUpdate.containsFolderID()) {
            storage.getEventStorage().insertTombstoneEvent(EventMapper.getInstance().getTombstone(originalEvent, now, calendarUser.getId()));
            storage.getAttendeeStorage().insertTombstoneAttendee(objectID, AttendeeMapper.getInstance().getTombstone(originalAttendee));
        }
        storage.getAttendeeStorage().updateAttendee(objectID, attendeeUpdate);
        Event eventUpdate = new Event();
        eventUpdate.setId(objectID);
        Consistency.setModified(now, eventUpdate, session.getUser().getId());
        storage.getEventStorage().updateEvent(eventUpdate);
        return readEvent(folder, objectID);
    }

}
