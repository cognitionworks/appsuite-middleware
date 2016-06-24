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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarService;
import com.openexchange.chronos.CalendarStorage;
import com.openexchange.chronos.CalendarStorageFactory;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.UserizedEvent;
import com.openexchange.chronos.impl.osgi.Services;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.folderstorage.type.SharedType;
import com.openexchange.group.Group;
import com.openexchange.group.GroupService;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.ldap.User;
import com.openexchange.java.Strings;
import com.openexchange.resource.Resource;
import com.openexchange.resource.ResourceService;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.UserService;

/**
 * {@link CalendarService}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class CalendarWriter {

    private static final int ATTENDEE_FOLDER_ID_PUBLIC = 2;

    private final ServerSession session;
    private final CalendarStorage storage;
	
    /**
     * Initializes a new {@link CalendarWriter}.
     * 
     * @param session The session
     */
    public CalendarWriter(ServerSession session) throws OXException {
		super();
        this.session = session;
        this.storage = Services.getService(CalendarStorageFactory.class).create(session);
	}

    public UserizedEvent insertEvent(UserizedEvent event) throws OXException {
        return insertEvent(getFolder(event.getFolderId()), event);
    }

    private User getUser(int userID) throws OXException {
        UserService userService = Services.getService(UserService.class);
        return userService.getUser(userID, session.getContext());
    }

    private Group getGroup(int groupID) throws OXException {
        return Services.getService(GroupService.class).getGroup(session.getContext(), groupID);
    }

    private Resource getResource(int resourceID) throws OXException {
        return Services.getService(ResourceService.class).getResource(resourceID, session.getContext());
    }

    private int getDefaultFolderID(User user) throws OXException {
        return new OXFolderAccess(session.getContext()).getDefaultFolderID(user.getId(), FolderObject.CALENDAR);
        //TODO: via higher level service?
    }

    private static boolean contains(List<Attendee> attendees, Attendee attendee) {
        if (null != attendees) {
            for (Attendee candidateAttendee : attendees) {
                if (matches(attendee, candidateAttendee)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matches(Attendee attendee1, Attendee attendee2) {
        if (null == attendee1) {
            return null == attendee2;
        } else if (null != attendee2) {
            if (0 < attendee1.getEntity() && attendee1.getEntity() == attendee2.getEntity()) {
                return true;
            }
            if (null != attendee1.getUri() && attendee1.getUri().equals(attendee2.getUri())) {
                return true;
            }
        }
        return false;
    }

    private List<Attendee> prepateAttendees(UserizedFolder folder, List<Attendee> requestedAttendees) throws OXException {
        List<Attendee> preparedAttendees = new ArrayList<Attendee>();
        /*
         * always add at least folder owner / current user as attendee
         */
        {
            User user = SharedType.getInstance().equals(folder.getType()) ? getUser(folder.getCreatedBy()) : session.getUser();
            Attendee attendee = CalendarUtils.applyProperties(new Attendee(), user);
            attendee.setCuType(CalendarUserType.INDIVIDUAL);
            attendee.setPartStat(ParticipationStatus.ACCEPTED);
            attendee.setFolderID(PublicType.getInstance().equals(folder.getType()) ? -2 : Integer.valueOf(folder.getID()));
            preparedAttendees.add(attendee);
            if (null == requestedAttendees || 0 == requestedAttendees.size()) {
                return preparedAttendees;
            }
        }
        for (Attendee attendee : requestedAttendees) {
            if (0 < attendee.getEntity()) {
                if (CalendarUserType.GROUP.equals(attendee.getCuType())) {
                    /*
                     * verify existence of group attendee & resolve to members
                     */
                    Group group = getGroup(attendee.getEntity());
                    for (int userID : group.getMember()) {
                        User user = getUser(userID);
                        Attendee memberAttendee = CalendarUtils.applyProperties(new Attendee(), user);
                        if (false == contains(preparedAttendees, memberAttendee)) {
                            memberAttendee.setPartStat(ParticipationStatus.NEEDS_ACTION);
                            memberAttendee.setFolderID(PublicType.getInstance().equals(folder.getType()) ? -2 : getDefaultFolderID(user));
                            preparedAttendees.add(memberAttendee);
                        }
                    }
                } else if (CalendarUserType.RESOURCE.equals(attendee.getCuType()) || CalendarUserType.ROOM.equals(attendee.getCuType())) {
                    /*
                     * verify existence of resource attendee                    
                     */
                    Resource resource = getResource(attendee.getEntity());
                    Attendee resourceAttendee = new Attendee();
                    resourceAttendee.setPartStat(ParticipationStatus.ACCEPTED);
                    resourceAttendee.setUri(CalendarUtils.getCalAddress(session.getContextId(), resource));
                    resourceAttendee.setCommonName(resource.getDisplayName());
                    resourceAttendee.setComment(resource.getDescription());
                    if (false == contains(preparedAttendees, resourceAttendee)) {
                        preparedAttendees.add(resourceAttendee);
                    }
                } else {
                    /*
                     * verify existence of user attendee
                     */
                    User user = getUser(attendee.getEntity());
                    Attendee userAttendee = CalendarUtils.applyProperties(new Attendee(), user);
                    if (false == contains(preparedAttendees, userAttendee)) {
                        userAttendee.setPartStat(ParticipationStatus.NEEDS_ACTION);
                        userAttendee.setFolderID(PublicType.getInstance().equals(folder.getType()) ? -2 : getDefaultFolderID(user));
                        preparedAttendees.add(userAttendee);
                    }
                }
            } else {
                /*
                 * take over external attendee
                 */
                //TODO checks
                preparedAttendees.add(attendee);
            }
        }
        return preparedAttendees;
    }
    
    private UserizedEvent insertEvent(UserizedFolder folder, UserizedEvent event) throws OXException {
        Check.requireCalendarContentType(folder);
        Check.requireFolderPermission(folder, Permission.CREATE_OBJECTS_IN_FOLDER);
        Check.requireWritePermission(folder, Permission.WRITE_OWN_OBJECTS);
        
        Consistency.setCreatedNow(event, session.getUserId());
        Consistency.setModifiedNow(event, session.getUserId());
        event.setUid(Strings.isEmpty(event.getUid()) ? UUID.randomUUID().toString() : event.getUid());

        event.setPublicFolderId(PublicType.getInstance().equals(folder.getType()) ? Integer.parseInt(folder.getID()) : 0);

        /*
         * use current user as organizer & take over attendees
         */
        Consistency.setOrganizer(event, session.getUser());
        event.setAttendees(prepateAttendees(folder, event.getAttendees()));
        /*
         * insert event
         */
        int objectID = storage.insertEvent(event);
        return new CalendarReader(session).readEvent(folder, objectID);
    }


    private UserizedFolder getFolder(int folderID) throws OXException {
        return Services.getService(FolderService.class).getFolder(FolderStorage.REAL_TREE_ID, String.valueOf(folderID), session, null);
    }

}
