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

import static com.openexchange.chronos.common.CalendarUtils.contains;
import static com.openexchange.chronos.common.CalendarUtils.filter;
import static com.openexchange.chronos.common.CalendarUtils.filterByMembership;
import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.isInternal;
import static com.openexchange.chronos.common.CalendarUtils.isLastUserAttendee;
import static com.openexchange.chronos.impl.Utils.getCalendarUserId;
import static com.openexchange.chronos.impl.Utils.isEnforceDefaultAttendee;
import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.common.mapping.AttendeeMapper;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.ItemUpdate;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.folderstorage.type.PublicType;

/**
 * {@link AttendeeHelper}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class AttendeeHelper {

    /** The constant used to indicate a common "public" parent folder for internal user attendees */
    public static final String ATTENDEE_PUBLIC_FOLDER_ID = String.valueOf(-2);

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AttendeeHelper.class);

    private final CalendarSession session;
    private final UserizedFolder folder;
    private final List<Attendee> originalAttendees;
    private final List<Attendee> attendeesToInsert;
    private final List<Attendee> attendeesToDelete;
    private final List<Attendee> attendeesToUpdate;

    /**
     * Initializes a new {@link AttendeeHelper} for a new event.
     *
     * @param session The calendar session
     * @param folder The parent folder of the event being processed
     * @param requestedAttendees The list of attendees, as supplied by the client
     */
    public static AttendeeHelper onNewEvent(CalendarSession session, UserizedFolder folder, List<Attendee> requestedAttendees) throws OXException {
        AttendeeHelper attendeeHelper = new AttendeeHelper(session, folder, null);
        attendeeHelper.processNewEvent(emptyForNull(requestedAttendees));
        return attendeeHelper;
    }

    /**
     * Initializes a new {@link AttendeeHelper} for an updated event.
     *
     * @param session The calendar session
     * @param folder The parent folder of the event being processed
     * @param originalAttendees The original list of attendees
     * @param updatedAttendees The new/updated list of attendees, as supplied by the client
     */
    public static AttendeeHelper onUpdatedEvent(CalendarSession session, UserizedFolder folder, List<Attendee> originalAttendees, List<Attendee> updatedAttendees) throws OXException {
        AttendeeHelper attendeeHelper = new AttendeeHelper(session, folder, originalAttendees);
        attendeeHelper.processUpdatedEvent(emptyForNull(updatedAttendees));
        return attendeeHelper;
    }

    /**
     * Initializes a new {@link AttendeeHelper} for an updated event, only considering updated attendees in case they're explicitly set in
     * the passed event update.
     *
     * @param session The calendar session
     * @param folder The parent folder of the event being processed
     * @param originalAttendees The original list of attendees
     * @param updatedEvent The updated event data as supplied by the client
     */
    public static AttendeeHelper onUpdatedEvent(CalendarSession session, UserizedFolder folder, List<Attendee> originalAttendees, Event updatedEvent) throws OXException {
        AttendeeHelper attendeeHelper = new AttendeeHelper(session, folder, originalAttendees);
        if (updatedEvent.containsAttendees()) {
            attendeeHelper.processUpdatedEvent(emptyForNull(updatedEvent.getAttendees()));
        }
        return attendeeHelper;
    }

    /**
     * Initializes a new {@link AttendeeHelper} for a deleted event.
     *
     * @param session The calendar session
     * @param folder The parent folder of the event being processed
     * @param originalAttendees The original list of attendees
     */
    public static AttendeeHelper onDeletedEvent(CalendarSession session, UserizedFolder folder, List<Attendee> originalAttendees) throws OXException {
        AttendeeHelper attendeeHelper = new AttendeeHelper(session, folder, originalAttendees);
        attendeeHelper.processDeletedEvent();
        return attendeeHelper;
    }

    /**
     * Initializes a new {@link AttendeeHelper}.
     *
     * @param session The calendar session
     * @param folder The parent folder of the event being processed
     * @param originalAttendees The original attendees of the event, or <code>null</code> for new event creations
     */
    private AttendeeHelper(CalendarSession session, UserizedFolder folder, List<Attendee> originalAttendees) throws OXException {
        this.session = session;
        this.folder = folder;
        this.originalAttendees = emptyForNull(originalAttendees);
        this.attendeesToInsert = new ArrayList<Attendee>();
        this.attendeesToDelete = new ArrayList<Attendee>();
        this.attendeesToUpdate = new ArrayList<Attendee>();
    }

    /**
     * Gets a list of newly added attendees that should be inserted.
     *
     * @return The attendees, or an empty list if there are none
     */
    public List<Attendee> getAttendeesToInsert() throws OXException {
        return attendeesToInsert;
    }

    /**
     * Gets a list of removed attendees that should be deleted.
     *
     * @return The attendees, or an empty list if there are none
     */
    public List<Attendee> getAttendeesToDelete() {
        return attendeesToDelete;
    }

    /**
     * Gets a list of modified added attendees that should be updated.
     *
     * @return The attendees, or an empty list if there are none
     */
    public List<Attendee> getAttendeesToUpdate() {
        return attendeesToUpdate;
    }

    /**
     * Gets a value indicating if there are any attendee-related changes.
     *
     * @return <code>true</code> if there are any changes, <code>false</code>, otherwise
     */
    public boolean hasChanges() {
        return 0 < attendeesToInsert.size() || 0 < attendeesToDelete.size() || 0 < attendeesToUpdate.size();
    }

    /**
     * Gets a "forecast" of the resulting attendee list after all changes are applied to the original list of attendees. No data is
     * actually changed, i.e. the internal list of attendees to insert, update and delete are still intact.
     *
     * @return The changed list of attendees
     */
    public List<Attendee> previewChanges() throws OXException {
        List<Attendee> newAttendees = new ArrayList<Attendee>(originalAttendees);
        newAttendees.removeAll(attendeesToDelete);
        for (Attendee attendeeToUpdate : attendeesToUpdate) {
            Attendee originalAttendee = find(originalAttendees, attendeeToUpdate);
            newAttendees.remove(originalAttendee);
            Attendee newAttendee = AttendeeMapper.getInstance().copy(originalAttendee, null, (AttendeeField[]) null);
            AttendeeMapper.getInstance().copy(attendeeToUpdate, newAttendee, AttendeeField.RSVP, AttendeeField.COMMENT, AttendeeField.PARTSTAT, AttendeeField.ROLE);
            newAttendees.add(newAttendee);
        }
        newAttendees.addAll(attendeesToInsert);
        return newAttendees;
    }

    private void processNewEvent(List<Attendee> requestedAttendees) throws OXException {
        session.getEntityResolver().prefetch(requestedAttendees);
        requestedAttendees = session.getEntityResolver().prepare(requestedAttendees);
        /*
         * always start with attendee for default calendar user in folder
         */
        Attendee defaultAttendee = getDefaultAttendee(session, folder, requestedAttendees);
        attendeesToInsert.add(defaultAttendee);
        if (null != requestedAttendees && 0 < requestedAttendees.size()) {
            /*
             * prepare & add all further attendees
             */
            List<Attendee> attendeeList = new ArrayList<Attendee>();
            attendeeList.add(defaultAttendee);
            attendeesToInsert.addAll(prepareNewAttendees(attendeeList, requestedAttendees));
        }
        /*
         * apply proper default attendee handling afterwards
         */
        handleDefaultAttendee(isEnforceDefaultAttendee(session));
    }

    private void processUpdatedEvent(List<Attendee> updatedAttendees) throws OXException {
        session.getEntityResolver().prefetch(updatedAttendees);
        updatedAttendees = session.getEntityResolver().prepare(updatedAttendees);
        AbstractCollectionUpdate<Attendee, AttendeeField> attendeeDiff = Utils.getAttendeeUpdates(originalAttendees, updatedAttendees);
        List<Attendee> attendeeList = new ArrayList<Attendee>(originalAttendees);
        /*
         * delete removed attendees
         */
        List<Attendee> removedAttendees = attendeeDiff.getRemovedItems();
        for (Attendee removedAttendee : removedAttendees) {
            if (isEnforceDefaultAttendee(session) && false == PublicType.getInstance().equals(folder.getType()) && removedAttendee.getEntity() == folder.getCreatedBy()) {
                /*
                 * preserve default calendar user in personal folders
                 */
                LOG.info("Implicitly preserving default calendar user {} in personal folder {}.", I(removedAttendee.getEntity()), folder);
                continue;
            }
            if (CalendarUserType.GROUP.equals(removedAttendee.getCuType())) {
                /*
                 * only remove group attendee in case all originally participating members are also removed
                 */
                if (hasAttendingGroupMembers(removedAttendee.getUri(), originalAttendees, removedAttendees)) {
                    // preserve group attendee
                    LOG.debug("Ignoring removal of group {} as long as there are attending members.", I(removedAttendee.getEntity()));
                    continue;
                }
            }
            if (null != removedAttendee.getMember() && 0 < removedAttendee.getMember().size()) {
                /*
                 * only remove group member attendee in case either the corresponding group attendee itself, or *not all* originally participating members are also removed
                 */
                if (false == containsAllUris(removedAttendees, removedAttendee.getMember())) {
                    boolean attendingOtherMembers = false;
                    for (String groupUri : removedAttendee.getMember()) {
                        if (hasAttendingGroupMembers(groupUri, originalAttendees, removedAttendees)) {
                            attendingOtherMembers = true;
                            break;
                        }
                    }
                    if (false == attendingOtherMembers) {
                        // preserve group member attendee
                        LOG.debug("Ignoring removal of group member attendee {} as long as group unchanged.", I(removedAttendee.getEntity()));
                        continue;
                    }
                }
            }
            attendeeList.remove(removedAttendee);
            attendeesToDelete.add(removedAttendee);
        }
        /*
         * apply updated attendee data
         */
        for (ItemUpdate<Attendee, AttendeeField> attendeeUpdate : attendeeDiff.getUpdatedItems()) {
            Attendee attendee = AttendeeMapper.getInstance().copy(attendeeUpdate.getOriginal(), null, AttendeeField.ENTITY, AttendeeField.MEMBER, AttendeeField.CU_TYPE, AttendeeField.URI);
            AttendeeMapper.getInstance().copy(attendeeUpdate.getUpdate(), attendee, AttendeeField.RSVP, AttendeeField.COMMENT, AttendeeField.PARTSTAT, AttendeeField.ROLE);
            if (false == isInternal(attendee) && false == session.getConfig().isSkipExternalAttendeeURIChecks()) {
                attendee = Check.requireValidEMail(attendee);
            }
            attendeesToUpdate.add(attendee);
        }
        /*
         * prepare & add all new attendees
         */
        attendeesToInsert.addAll(prepareNewAttendees(attendeeList, attendeeDiff.getAddedItems()));
        /*
         * apply proper default attendee handling afterwards
         */
        handleDefaultAttendee(isEnforceDefaultAttendee(session));
    }

    private void processDeletedEvent() {
        attendeesToDelete.addAll(originalAttendees);
    }

    private List<Attendee> prepareNewAttendees(List<Attendee> existingAttendees, List<Attendee> newAttendees) throws OXException {
        List<Attendee> attendees = new ArrayList<Attendee>(newAttendees.size());
        /*
         * add internal user attendees
         */
        boolean inPublicFolder = PublicType.getInstance().equals(folder.getType());
        for (Attendee userAttendee : filter(newAttendees, Boolean.TRUE, CalendarUserType.INDIVIDUAL)) {
            if (contains(existingAttendees, userAttendee) || contains(attendees, userAttendee)) {
                LOG.debug("Skipping duplicate user attendee {}", userAttendee);
                continue;
            }
            userAttendee = session.getEntityResolver().applyEntityData(userAttendee, AttendeeField.ROLE, AttendeeField.RSVP);
            userAttendee.setFolderID(inPublicFolder ?
                ATTENDEE_PUBLIC_FOLDER_ID : session.getConfig().getDefaultFolderID(userAttendee.getEntity()));
            if (false == userAttendee.containsPartStat() || null == userAttendee.getPartStat()) {
                userAttendee.setPartStat(session.getConfig().getInitialPartStat(userAttendee.getEntity(), inPublicFolder));
            }
            attendees.add(userAttendee);
        }
        /*
         * resolve & add any internal group attendees
         */
        boolean resolveGroupAttendees = session.getConfig().isResolveGroupAttendees();
        for (Attendee groupAttendee : filter(newAttendees, Boolean.TRUE, CalendarUserType.GROUP)) {
            if (contains(existingAttendees, groupAttendee) || contains(attendees, groupAttendee)) {
                LOG.debug("Skipping duplicate group attendee {}", groupAttendee);
                continue;
            }
            groupAttendee = session.getEntityResolver().applyEntityData(groupAttendee);
            if (false == resolveGroupAttendees) {
                attendees.add(groupAttendee);
            } else {
                LOG.debug("Skipping group attendee {}; only resolving group members.", groupAttendee);
            }
            for (int memberID : session.getEntityResolver().getGroupMembers(groupAttendee.getEntity())) {
                if (contains(existingAttendees, memberID) || contains(attendees, memberID)) {
                    LOG.debug("Skipping explicitly added group member {}", I(memberID));
                    continue;
                }
                Attendee memberAttendee = session.getEntityResolver().prepareUserAttendee(memberID);
                memberAttendee.setFolderID(PublicType.getInstance().equals(folder.getType()) ?
                    ATTENDEE_PUBLIC_FOLDER_ID : session.getConfig().getDefaultFolderID(memberID));
                memberAttendee.setPartStat(session.getConfig().getInitialPartStat(memberID, inPublicFolder));
                if (false == resolveGroupAttendees) {
                    memberAttendee.setMember(Collections.singletonList(groupAttendee.getUri()));
                }
                attendees.add(memberAttendee);
            }
        }
        /*
         * resolve & add any internal resource attendees
         */
        for (Attendee resourceAttendee : filter(newAttendees, Boolean.TRUE, CalendarUserType.RESOURCE)) {
            if (contains(existingAttendees, resourceAttendee) || contains(attendees, resourceAttendee)) {
                LOG.debug("Skipping duplicate resource attendee {}", resourceAttendee);
                continue;
            }
            attendees.add(session.getEntityResolver().applyEntityData(resourceAttendee, AttendeeField.ROLE));
        }
        /*
         * take over any external attendees
         */
        for (Attendee attendee : filter(newAttendees, Boolean.FALSE, (CalendarUserType[]) null)) {
            attendee = session.getEntityResolver().applyEntityData(attendee);
            if (contains(existingAttendees, attendee) || contains(attendees, attendee)) {
                LOG.debug("Skipping duplicate external attendee {}", attendee);
                continue;
            }
            attendees.add(session.getConfig().isSkipExternalAttendeeURIChecks() ? attendee : Check.requireValidEMail(attendee));
        }
        return attendees;
    }

    /**
     * Gets the <i>default</i> attendee that is always added to a newly inserted event, based on the target folder type.<p/>
     * For <i>public</i> folders, this is an attendee for the current session's user, otherwise (<i>private</i> or <i>shared</i>, an
     * attendee for the folder owner (i.e. the calendar user) is prepared.
     *
     * @param session The calendar session
     * @param folder The folder to get the default attendee for
     * @param requestedAttendees The attendees as supplied by the client, or <code>null</code> if not available
     * @return The default attendee
     */
    public static Attendee getDefaultAttendee(CalendarSession session, UserizedFolder folder, List<Attendee> requestedAttendees) throws OXException {
        /*
         * prepare attendee for default calendar user in folder
         */
        int calendarUserId = getCalendarUserId(folder);
        Attendee defaultAttendee = session.getEntityResolver().prepareUserAttendee(calendarUserId);
        defaultAttendee.setPartStat(ParticipationStatus.ACCEPTED);
        defaultAttendee.setFolderID(PublicType.getInstance().equals(folder.getType()) ? ATTENDEE_PUBLIC_FOLDER_ID : folder.getID());
        if (session.getUserId() != calendarUserId) {
            defaultAttendee.setSentBy(session.getEntityResolver().applyEntityData(new CalendarUser(), session.getUserId()));
        }
        /*
         * take over additional properties from corresponding requested attendee
         */
        Attendee requestedAttendee = find(requestedAttendees, defaultAttendee);
        if (null != requestedAttendee) {
            AttendeeMapper.getInstance().copy(requestedAttendee, defaultAttendee,
                AttendeeField.RSVP, AttendeeField.COMMENT, AttendeeField.PARTSTAT, AttendeeField.ROLE, AttendeeField.PARTSTAT);
        }
        return defaultAttendee;
    }

    private static List<Attendee> emptyForNull(List<Attendee> attendees) {
        return null == attendees ? Collections.<Attendee> emptyList() : attendees;
    }

    /**
     * Processes the lists of attendees to update/delete/insert in terms of the configured handling of the implicit attendee for the
     * actual calendar user.
     * <p/>
     * If the default attendee is enforced, this method ensures that the calendar user attendee is always present in
     * personal calendar folders. Otherwise, if the actual calendar user would be the last one in the resulting attendee list, this
     * attendee is removed.
     *
     * @param enforceDefaultAttendee <code>true</code> the current calendar user should be added as default attendee to events implicitly,
     *            <code>false</code>, otherwise
     */
    private void handleDefaultAttendee(boolean enforceDefaultAttendee) throws OXException {
        /*
         * no default-attendee handling in public folders
         */
        if (PublicType.getInstance().equals(folder.getType())) {
            return;
        }
        /*
         * check if resulting attendees would lead to a "group-scheduled" event or not
         */
        int calendarUserId = getCalendarUserId(folder);
        List<Attendee> attendees = previewChanges();
        if (false == enforceDefaultAttendee && (attendees.isEmpty() || isLastUserAttendee(attendees, calendarUserId))) {
            /*
             * event is not (or no longer) a group-scheduled one, remove default attendee
             */
            Attendee defaultAttendee = find(attendeesToInsert, calendarUserId);
            if (null != defaultAttendee) {
                attendeesToInsert.remove(defaultAttendee);
            }
            defaultAttendee = find(attendeesToUpdate, calendarUserId);
            if (null != defaultAttendee) {
                attendeesToUpdate.remove(defaultAttendee);
                attendeesToDelete.add(defaultAttendee);
            } else {
                defaultAttendee = find(originalAttendees, calendarUserId);
                if (null != defaultAttendee) {
                    attendeesToDelete.add(defaultAttendee);
                }
            }
        } else {
            /*
             * ensure the calendar user is always present in group-scheduled events in personal calendar folders
             */
            if (false == PublicType.getInstance().equals(folder.getType()) && false == contains(attendees, calendarUserId)) {
                Attendee defaultAttendee = find(attendeesToDelete, calendarUserId);
                if (null != defaultAttendee) {
                    LOG.info("Implicitly preserving default calendar user {} in personal folder {}.", I(calendarUserId), folder);
                    attendeesToDelete.remove(defaultAttendee);
                } else {
                    LOG.info("Implicitly adding default calendar user {} in personal folder {}.", I(calendarUserId), folder);
                    attendeesToInsert.add(getDefaultAttendee(session, folder, null));
                }
            }
        }
    }

    /**
     * Gets a value indicating whether a resulting attendee list is going to contain at least one member of a specific group or not.
     *
     * @param groupUri The uri of the group to check
     * @param originalAttendees The list of original attendees
     * @param removedAttendees The list of removed attendees
     * @return <code>true</code> if there'll be at least one group member afterwards, <code>false</code>, otherwise
     */
    private static boolean hasAttendingGroupMembers(String groupUri, List<Attendee> originalAttendees, List<Attendee> removedAttendees) {
        for (Attendee originalMemberAttendee : filterByMembership(originalAttendees, groupUri)) {
            if (null == find(removedAttendees, originalMemberAttendee)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAllUris(List<Attendee> attendees, List<String> uris) {
        if (null == uris || 0 == uris.size()) {
            return true;
        }
        for (String uri : uris) {
            if (false == containsUri(attendees, uri)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsUri(List<Attendee> attendees, String uri) {
        if (null == attendees || 0 == attendees.size()) {
            return false;
        }
        for (Attendee attendee : attendees) {
            if (uri.equalsIgnoreCase(attendee.getUri())) {
                return true;
            }
        }
        return false;
    }

}
