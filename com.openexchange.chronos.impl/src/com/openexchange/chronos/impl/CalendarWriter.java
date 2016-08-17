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

import static com.openexchange.chronos.impl.CalendarUtils.contains;
import static com.openexchange.chronos.impl.CalendarUtils.filter;
import static com.openexchange.chronos.impl.CalendarUtils.find;
import static com.openexchange.chronos.impl.CalendarUtils.getCalendarUser;
import static com.openexchange.chronos.impl.CalendarUtils.getUserIDs;
import static com.openexchange.chronos.impl.CalendarUtils.i;
import static com.openexchange.chronos.impl.CalendarUtils.isAttendee;
import static com.openexchange.chronos.impl.CalendarUtils.isOrganizer;
import static com.openexchange.chronos.impl.CalendarUtils.isSeriesException;
import static com.openexchange.chronos.impl.CalendarUtils.isSeriesMaster;
import static com.openexchange.chronos.impl.Check.requireCalendarPermission;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.CalendarSession;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
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
        /*
         * load original event data
         */
        Event originalEvent = storage.getEventStorage().loadEvent(objectID, null);
        if (null == originalEvent) {
            throw OXException.notFound(String.valueOf(objectID));//TODO
        }
        /*
         * check current session user's permissions
         */
        if (session.getUser().getId() != originalEvent.getCreatedBy()) {
            requireCalendarPermission(folder, READ_FOLDER, NO_PERMISSIONS, NO_PERMISSIONS, DELETE_ALL_OBJECTS);
        } else {
            requireCalendarPermission(folder, READ_FOLDER, NO_PERMISSIONS, NO_PERMISSIONS, DELETE_OWN_OBJECTS);
        }
        requireUpToDateTimestamp(originalEvent, clientTimestamp);
        /*
         * determine deletion mode
         */
        Date now = new Date();
        User calendarUser = getCalendarUser(folder);
        List<Attendee> originalAttendees = storage.getAttendeeStorage().loadAttendees(objectID);
        List<Attendee> userAttendees = filter(originalAttendees, Boolean.TRUE, CalendarUserType.INDIVIDUAL);
        if (isOrganizer(originalEvent, calendarUser.getId()) || 1 == userAttendees.size() && calendarUser.getId() == userAttendees.get(0).getEntity()) {
            /*
             * deletion by organizer / last user attendee
             */
            storage.getEventStorage().insertTombstoneEvent(EventMapper.getInstance().getTombstone(originalEvent, now, calendarUser.getId()));
            storage.getAttendeeStorage().insertTombstoneAttendees(objectID, AttendeeMapper.getInstance().getTombstones(originalAttendees));
            storage.getAlarmStorage().deleteAlarms(objectID);
            storage.getEventStorage().deleteEvent(objectID);
            storage.getAttendeeStorage().deleteAttendees(objectID);
        } else if (contains(userAttendees, calendarUser.getId())) {
            /*
             * deletion as one of the user attendees
             */
            Attendee originalAttendee = find(userAttendees, calendarUser.getId());
            storage.getEventStorage().insertTombstoneEvent(EventMapper.getInstance().getTombstone(originalEvent, now, calendarUser.getId()));
            storage.getAttendeeStorage().insertTombstoneAttendee(objectID, originalAttendee);
            storage.getAlarmStorage().deleteAlarms(objectID, calendarUser.getId());
            Event eventUpdate = new Event();
            eventUpdate.setId(objectID);
            Consistency.setModified(now, eventUpdate, calendarUser.getId());
            storage.getEventStorage().updateEvent(eventUpdate);
        } else {
            /*
             * deletion as ?
             */
            throw OXException.general("unsupported deletion");
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

        /*
         * perform move operation & take over new parent folder
         */
        if (userizedEvent.containsFolderId() && 0 < userizedEvent.getFolderId() && i(folder) != userizedEvent.getFolderId()) {
            UserizedFolder targetFolder = getFolder(userizedEvent.getFolderId());
            moveEvent(originalEvent, folder, targetFolder);
            folder = targetFolder;
        }
        /*
         * update alarms for calendar user
         */
        if (userizedEvent.containsAlarms()) {
            if (null == userizedEvent.getAlarms()) {
                storage.getAlarmStorage().deleteAlarms(objectID, calendarUser.getId());
            } else {
                storage.getAlarmStorage().updateAlarms(objectID, calendarUser.getId(), userizedEvent.getAlarms());
            }
        }
        /*
         * process any attendee updates
         */
        if (userizedEvent.getEvent().containsAttendees()) {
            List<Attendee> originalAttendees = storage.getAttendeeStorage().loadAttendees(objectID);
            List<Attendee> updatedAttendees = userizedEvent.getEvent().getAttendees();
            AttendeeHelper attendeeHelper = new AttendeeHelper(session, folder, originalAttendees, updatedAttendees);
            /*
             * perform attendee deletions
             */
            List<Attendee> attendeesToDelete = attendeeHelper.getAttendeesToDelete();
            if (0 < attendeesToDelete.size()) {
                if (isOrganizer(originalEvent, calendarUser.getId())) {
                    if (contains(attendeesToDelete, calendarUser.getId())) {
                        throw OXException.general("cannot remove organizer");
                    }
                } else if (isAttendee(originalEvent, calendarUser.getId())) {
                    if (1 < attendeesToDelete.size() || calendarUser.getId() != attendeesToDelete.get(0).getEntity()) {
                        //                        throw OXException.general("not allowed attendee change");
                    }
                } else {
                    //                  throw OXException.general("not allowed attendee change");
                }
                storage.getEventStorage().insertTombstoneEvent(EventMapper.getInstance().getTombstone(originalEvent, now, calendarUser.getId()));
                storage.getAttendeeStorage().insertTombstoneAttendees(objectID, AttendeeMapper.getInstance().getTombstones(attendeesToDelete));
                storage.getAttendeeStorage().deleteAttendees(objectID, attendeesToDelete);
                storage.getAlarmStorage().deleteAlarms(objectID, getUserIDs(attendeesToDelete));
            }
            /*
             * perform attendee updates
             */
            List<Attendee> attendeesToUpdate = attendeeHelper.getAttendeesToUpdate();
            if (0 < attendeesToUpdate.size()) {
                if (isOrganizer(originalEvent, calendarUser.getId())) {
                    // okay
                } else if (isAttendee(originalEvent, calendarUser.getId())) {
                    if (1 < attendeesToUpdate.size() || calendarUser.getId() != attendeesToUpdate.get(0).getEntity()) {
                        //                        throw OXException.general("not allowed attendee change");
                    }
                } else {
                    //                    throw OXException.general("not allowed attendee change");
                }
                storage.getAttendeeStorage().updateAttendees(objectID, attendeeHelper.getAttendeesToUpdate());
            }
            /*
             * add new attendees
             */
            if (0 < attendeeHelper.getAttendeesToInsert().size()) {
                if (false == isOrganizer(originalEvent, calendarUser.getId())) {
                    //                    throw OXException.general("not allowed attendee change");
                }
                storage.getAttendeeStorage().insertAttendees(objectID, attendeeHelper.getAttendeesToInsert());
            }
        }
        /*
         * update event data
         */
        Event eventUpdate = Consistency.prepareEventUpdate(originalEvent, userizedEvent.getEvent());
        eventUpdate.setId(objectID);
        Consistency.setModified(now, eventUpdate, calendarUser.getId());
        eventUpdate.setSequence(originalEvent.getSequence() + 1);
        storage.getEventStorage().updateEvent(eventUpdate);

        return readEvent(folder, objectID);
    }

    private void moveEvent(Event event, UserizedFolder folder, UserizedFolder targetFolder) throws OXException {
        /*
         * check current session user's permissions
         */
        requireCalendarPermission(targetFolder, CREATE_OBJECTS_IN_FOLDER, NO_PERMISSIONS, NO_PERMISSIONS, NO_PERMISSIONS);
        if (session.getUser().getId() == event.getCreatedBy()) {
            requireCalendarPermission(folder, READ_FOLDER, READ_OWN_OBJECTS, WRITE_OWN_OBJECTS, DELETE_OWN_OBJECTS);
        } else {
            requireCalendarPermission(folder, READ_FOLDER, READ_ALL_OBJECTS, WRITE_ALL_OBJECTS, DELETE_ALL_OBJECTS);
        }
        /*
         * check for move attempt of recurring appointment
         */
        if (isSeriesMaster(event) || isSeriesException(event)) {
            throw OXException.general("moving recurring appointments not allowed");
        }
        int objectID = event.getId();
        Date now = new Date();
        /*
         * perform move operation based on parent folder types
         */
        User calendarUser = getCalendarUser(folder);
        User targetCalendarUser = getCalendarUser(targetFolder);
        if (PublicType.getInstance().equals(folder.getType()) && PublicType.getInstance().equals(targetFolder.getType())) {
            /*
             * move from one public folder to another, update event's folder
             */
            Event eventUpdate = new Event();
            eventUpdate.setId(objectID);
            eventUpdate.setPublicFolderId(i(targetFolder));
            Consistency.setModified(now, eventUpdate, calendarUser.getId());
            storage.getEventStorage().insertTombstoneEvent(EventMapper.getInstance().getTombstone(event, now, calendarUser.getId()));
            storage.getEventStorage().updateEvent(eventUpdate);
            return;
        }
        if (false == PublicType.getInstance().equals(folder.getType()) && false == PublicType.getInstance().equals(targetFolder.getType())) {
            /*
             * move between personal calendar folders
             */
            if (calendarUser.getId() == targetCalendarUser.getId()) {
                /*
                 * move from one personal folder to another of the same user, update attendee's folder
                 */
                List<Attendee> originalAttendees = storage.getAttendeeStorage().loadAttendees(objectID);
                Attendee originalAttendee = find(originalAttendees, calendarUser.getId());
                if (null == originalAttendee) {
                    throw OXException.notFound(calendarUser.toString());
                }
                storage.getEventStorage().insertTombstoneEvent(EventMapper.getInstance().getTombstone(event, now, calendarUser.getId()));
                storage.getAttendeeStorage().insertTombstoneAttendee(objectID, AttendeeMapper.getInstance().getTombstone(originalAttendee));
                Attendee attendeeUpdate = new Attendee();
                AttendeeMapper.getInstance().copy(originalAttendee, attendeeUpdate, AttendeeField.ENTITY, AttendeeField.MEMBER, AttendeeField.CU_TYPE, AttendeeField.URI);
                attendeeUpdate.setFolderID(i(targetFolder));
                storage.getAttendeeStorage().updateAttendee(objectID, attendeeUpdate);
            } else {
                /*
                 * move from one personal folder to another user's personal folder, take over target folder for new default attendee
                 * and reset personal calendar folders of further user attendees
                 */
                storage.getEventStorage().insertTombstoneEvent(EventMapper.getInstance().getTombstone(event, now, calendarUser.getId()));
                List<Attendee> originalAttendees = storage.getAttendeeStorage().loadAttendees(objectID);
                for (Attendee originalAttendee : filter(originalAttendees, Boolean.TRUE, CalendarUserType.INDIVIDUAL)) {
                    Attendee attendeeUpdate = new Attendee();
                    AttendeeMapper.getInstance().copy(originalAttendee, attendeeUpdate, AttendeeField.ENTITY, AttendeeField.MEMBER, AttendeeField.CU_TYPE, AttendeeField.URI);
                    if (targetCalendarUser.getId() == originalAttendee.getEntity()) {
                        attendeeUpdate.setFolderID(i(targetFolder));
                    } else {
                        attendeeUpdate.setFolderID(AttendeeHelper.getDefaultFolderID(session.getContext(), getUser(originalAttendee.getEntity())));
                    }
                    if (attendeeUpdate.getFolderID() != originalAttendee.getFolderID()) {
                        storage.getAttendeeStorage().insertTombstoneAttendee(objectID, AttendeeMapper.getInstance().getTombstone(originalAttendee));
                        storage.getAttendeeStorage().updateAttendee(objectID, attendeeUpdate);
                    }
                }
                /*
                 * ensure to add default calendar user if not already present
                 */
                if (false == contains(originalAttendees, targetCalendarUser.getId())) {
                    Attendee defaultAttendee = new AttendeeHelper(session, targetFolder, null, null).getAttendeesToInsert().get(0);
                    storage.getAttendeeStorage().insertAttendees(objectID, Collections.singletonList(defaultAttendee));
                }
            }
            /*
             * finally touch event
             */
            Event eventUpdate = new Event();
            eventUpdate.setId(objectID);
            Consistency.setModified(now, eventUpdate, calendarUser.getId());
            storage.getEventStorage().updateEvent(eventUpdate);
            return;
        }
        if (PublicType.getInstance().equals(folder.getType()) && false == PublicType.getInstance().equals(targetFolder.getType())) {
            /*
             * move from public to personal folder, take over default personal folders for user attendees
             */
            if (0 < event.getSeriesId()) {
                throw OXException.general("can't move recurring events to/from public folder");//TODO
            }
            storage.getEventStorage().insertTombstoneEvent(EventMapper.getInstance().getTombstone(event, now, calendarUser.getId()));
            List<Attendee> originalAttendees = storage.getAttendeeStorage().loadAttendees(objectID);
            for (Attendee originalAttendee : filter(originalAttendees, Boolean.TRUE, CalendarUserType.INDIVIDUAL)) {
                Attendee attendeeUpdate = new Attendee();
                AttendeeMapper.getInstance().copy(originalAttendee, attendeeUpdate, AttendeeField.ENTITY, AttendeeField.MEMBER, AttendeeField.CU_TYPE, AttendeeField.URI);
                if (targetCalendarUser.getId() == originalAttendee.getEntity()) {
                    attendeeUpdate.setFolderID(i(targetFolder));
                } else {
                    attendeeUpdate.setFolderID(AttendeeHelper.getDefaultFolderID(session.getContext(), getUser(originalAttendee.getEntity())));
                }
                storage.getAttendeeStorage().updateAttendee(objectID, attendeeUpdate);
            }
            /*
             * remove previous public folder id from event & touch event
             */
            Event eventUpdate = new Event();
            eventUpdate.setId(objectID);
            eventUpdate.setPublicFolderId(0);
            Consistency.setModified(now, eventUpdate, calendarUser.getId());
            storage.getEventStorage().updateEvent(eventUpdate);
            return;
        }
        if (false == PublicType.getInstance().equals(folder.getType()) && PublicType.getInstance().equals(targetFolder.getType())) {
            /*
             * move from personal to public folder, take over common public folder identifier for user attendees
             */
            if (0 < event.getSeriesId()) {
                throw OXException.general("can't move recurring events to/from public folder");
            }
            storage.getEventStorage().insertTombstoneEvent(EventMapper.getInstance().getTombstone(event, now, calendarUser.getId()));
            List<Attendee> originalAttendees = storage.getAttendeeStorage().loadAttendees(objectID);
            for (Attendee originalAttendee : filter(originalAttendees, Boolean.TRUE, CalendarUserType.INDIVIDUAL)) {
                storage.getAttendeeStorage().insertTombstoneAttendee(objectID, AttendeeMapper.getInstance().getTombstone(originalAttendee));
                Attendee attendeeUpdate = new Attendee();
                AttendeeMapper.getInstance().copy(originalAttendee, attendeeUpdate, AttendeeField.ENTITY, AttendeeField.MEMBER, AttendeeField.CU_TYPE, AttendeeField.URI);
                attendeeUpdate.setFolderID(AttendeeHelper.ATTENDEE_PUBLIC_FOLDER_ID);
                storage.getAttendeeStorage().updateAttendee(objectID, attendeeUpdate);
            }
            /*
             * take over new public folder id & touch event
             */
            Event eventUpdate = new Event();
            eventUpdate.setId(objectID);
            eventUpdate.setPublicFolderId(i(targetFolder));
            Consistency.setModified(now, eventUpdate, calendarUser.getId());
            storage.getEventStorage().updateEvent(eventUpdate);
            return;
        }
        /*
         * not supported move operation, otherwise
         */
        throw OXException.general("unsupported move");
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
