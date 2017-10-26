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

package com.openexchange.chronos.itip.generators.changes.generators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.itip.Messages;
import com.openexchange.chronos.itip.generators.ArgumentType;
import com.openexchange.chronos.itip.generators.Sentence;
import com.openexchange.chronos.itip.generators.changes.ChangeDescriptionGenerator;
import com.openexchange.chronos.itip.tools.ITipEventUpdate;
import com.openexchange.chronos.service.CollectionUpdate;
import com.openexchange.chronos.service.ItemUpdate;
import com.openexchange.exception.OXException;
import com.openexchange.group.Group;
import com.openexchange.group.GroupService;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.resource.Resource;
import com.openexchange.resource.ResourceService;
import com.openexchange.user.UserService;

/**
 * {@link Participants}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class Participants implements ChangeDescriptionGenerator {

    private static final EventField[] FIELDS = new EventField[] { EventField.ATTENDEES };

    protected static enum ChangeType {
        ADD, REMOVE, ACCEPT, DECLINE, TENTATIVE
    }

    private static final Map<ChangeType, String> PARTICIPANT_MESSAGE_MAP = new HashMap<ChangeType, String>() {

        {
            put(ChangeType.ADD, Messages.HAS_ADDED_PARTICIPANT);

            put(ChangeType.REMOVE, Messages.HAS_REMOVED_PARTICIPANT);

            put(ChangeType.ACCEPT, Messages.HAS_CHANGED_STATE);
            put(ChangeType.DECLINE, Messages.HAS_CHANGED_STATE);
            put(ChangeType.TENTATIVE, Messages.HAS_CHANGED_STATE);

        }
    };

    private static final Map<ChangeType, String> GROUP_MESSAGE_MAP = new HashMap<ChangeType, String>() {

        {
            put(ChangeType.ADD, Messages.HAS_INVITED_GROUP);
            put(ChangeType.REMOVE, Messages.HAS_REMOVED_GROUP);
        }
    };

    private static final Map<ChangeType, String> RESOURCE_MESSAGE_MAP = new HashMap<ChangeType, String>() {

        {
            put(ChangeType.ADD, Messages.HAS_ADDED_RESOURCE);
            put(ChangeType.REMOVE, Messages.HAS_REMOVED_RESOURCE);
        }
    };

    private final UserService users;
    private final GroupService groups;
    private final ResourceService resources;

    private final boolean stateChanges;

    public Participants(UserService users, GroupService groups, ResourceService resources, boolean stateChanges) {
        super();
        this.users = users;
        this.groups = groups;
        this.resources = resources;
        this.stateChanges = stateChanges;
    }

    @Override
    public List<Sentence> getDescriptions(Context ctx, Event original, Event updated, ITipEventUpdate diff, Locale locale, TimeZone timezone) throws OXException {
        Set<Integer> attendeeIds = new HashSet<>();
        Set<Integer> groupAttendeeIds = new HashSet<>();
        Set<Integer> resourceAttendeeIds = new HashSet<>();

        Map<Integer, ChangeType> attendeeChange = new HashMap<>();
        Map<Integer, ChangeType> groupChange = new HashMap<>();
        Map<Integer, ChangeType> resourceChange = new HashMap<>();
        Map<String, ChangeType> externalChange = new HashMap<>();

        Set<String> external = new HashSet<>();
        if (updated.getAttendees() != null) {
            for (Attendee a : updated.getAttendees()) {
                if (!CalendarUtils.isInternal(a)) {
                    external.add(a.getEMail());
                }
            }
        }

        if (original.getAttendees() != null) {
            for (Attendee a : original.getAttendees()) {
                if (!CalendarUtils.isInternal(a)) {
                    external.add(a.getEMail());
                }
            }
        }

        if (diff.getUpdatedFields().contains(EventField.ATTENDEES)) {
            CollectionUpdate<Attendee, AttendeeField> attendeeUpdates = diff.getAttendeeUpdates();
            investigateSetOperation(attendeeUpdates, attendeeIds, groupAttendeeIds, resourceAttendeeIds, attendeeChange, resourceChange, groupChange, externalChange, ChangeType.ADD, attendeeUpdates.getAddedItems());
            investigateSetOperation(attendeeUpdates, attendeeIds, groupAttendeeIds, resourceAttendeeIds, attendeeChange, resourceChange, groupChange, externalChange, ChangeType.REMOVE, attendeeUpdates.getRemovedItems());
            investigateChanges(attendeeUpdates, attendeeIds, attendeeChange, externalChange, external);
        }

        List<Sentence> changes = new ArrayList<Sentence>();

        for (Integer attendeeId : attendeeIds) {
            User u = users.getUser(attendeeId, ctx);
            ChangeType changeType = attendeeChange.get(attendeeId);
            switch (changeType) {
                case ADD:
                case REMOVE:
                    changes.add(new Sentence(PARTICIPANT_MESSAGE_MAP.get(changeType)).add(u.getDisplayName(), ArgumentType.PARTICIPANT));
                    break;
                case ACCEPT:
                    changes.add(new Sentence(PARTICIPANT_MESSAGE_MAP.get(changeType)).add(u.getDisplayName(), ArgumentType.PARTICIPANT).addStatus(ParticipationStatus.ACCEPTED));
                    break;
                case DECLINE:
                    changes.add(new Sentence(PARTICIPANT_MESSAGE_MAP.get(changeType)).add(u.getDisplayName(), ArgumentType.PARTICIPANT).addStatus(ParticipationStatus.DECLINED));
                    break;
                case TENTATIVE:
                    changes.add(new Sentence(PARTICIPANT_MESSAGE_MAP.get(changeType)).add(u.getDisplayName(), ArgumentType.PARTICIPANT).addStatus(ParticipationStatus.TENTATIVE));
                    break;
            }
        }

        List<String> externalMails = new ArrayList<String>(externalChange.keySet());
        Collections.sort(externalMails);
        for (String mail : externalMails) {
            ChangeType changeType = externalChange.get(mail);
            if (changeType == null) {
                continue;
            }
            switch (changeType) {
                case ADD:
                case REMOVE:
                    changes.add(new Sentence(PARTICIPANT_MESSAGE_MAP.get(changeType)).add(mail, ArgumentType.PARTICIPANT));
                    break;
                case ACCEPT:
                    changes.add(new Sentence(PARTICIPANT_MESSAGE_MAP.get(changeType)).add(mail, ArgumentType.PARTICIPANT).addStatus(ParticipationStatus.ACCEPTED));
                    break;
                case DECLINE:
                    changes.add(new Sentence(PARTICIPANT_MESSAGE_MAP.get(changeType)).add(mail, ArgumentType.PARTICIPANT).addStatus(ParticipationStatus.DECLINED));
                    break;
                case TENTATIVE:
                    changes.add(new Sentence(PARTICIPANT_MESSAGE_MAP.get(changeType)).add(mail, ArgumentType.PARTICIPANT).addStatus(ParticipationStatus.TENTATIVE));
                    break;
            }
        }

        for (Integer id : groupChange.keySet()) {
            Group group = groups.getGroup(ctx, id);
            ChangeType changeType = groupChange.get(id);
            if (changeType == null) {
                continue;
            }
            switch (changeType) {
                case ADD:
                case REMOVE:
                    changes.add(new Sentence(GROUP_MESSAGE_MAP.get(changeType)).add(group.getDisplayName(), ArgumentType.PARTICIPANT));
                    break;
                default: // Skip
            }
        }

        for (Entry<Integer, ChangeType> entry : resourceChange.entrySet()) {
            Resource resource = resources.getResource(entry.getKey(), ctx);
            ChangeType changeType = entry.getValue();
            if (changeType == null) {
                continue;
            }
            switch (changeType) {
                case ADD:
                case REMOVE:
                    changes.add(new Sentence(RESOURCE_MESSAGE_MAP.get(changeType)).add(resource.getDisplayName(), ArgumentType.PARTICIPANT));
                    break;
                default: // Skip
            }
        }

        return changes;
    }

    private void investigateChanges(CollectionUpdate<Attendee, AttendeeField> difference, Set<Integer> userIds, Map<Integer, ChangeType> userChange, Map<String, ChangeType> externalChange, Set<String> external) {

        List<? extends ItemUpdate<Attendee, AttendeeField>> updatedItems = difference.getUpdatedItems();
        for (ItemUpdate<Attendee, AttendeeField> itemUpdate : updatedItems) {
            ChangeType changeType = null;
            if (itemUpdate.getUpdatedFields().contains(AttendeeField.PARTSTAT)) {
                ParticipationStatus newPartStat = itemUpdate.getUpdate().getPartStat();
                if (newPartStat.equals(ParticipationStatus.DECLINED)) {
                    changeType = ChangeType.DECLINE;
                } else if (newPartStat.equals(ParticipationStatus.TENTATIVE)) {
                    changeType = ChangeType.TENTATIVE;
                } else {
                    changeType = ChangeType.ACCEPT;
                }
            }

            Attendee original = itemUpdate.getOriginal();
            if (CalendarUtils.isInternal(original)) {
                userIds.add(original.getEntity());
                userChange.put(original.getEntity(), changeType);
            } else {
                externalChange.put(original.getEMail(), changeType);
            }
        }

        if (difference.getAddedItems() != null && !difference.getAddedItems().isEmpty()) {
            for (Attendee added : difference.getAddedItems()) {
                if (CalendarUtils.isInternal(added)) {
                    userIds.add(added.getEntity());
                    userChange.put(added.getEntity(), ChangeType.ADD);
                } else {
                    externalChange.put(added.getEMail(), ChangeType.ADD);
                }
            }
        }

        if (difference.getRemovedItems() != null && !difference.getRemovedItems().isEmpty()) {
            for (Attendee removed : difference.getRemovedItems()) {
                if (CalendarUtils.isInternal(removed)) {
                    userIds.add(removed.getEntity());
                    userChange.put(removed.getEntity(), ChangeType.REMOVE);
                } else {
                    externalChange.put(removed.getEMail(), ChangeType.REMOVE);
                }
            }
        }
    }

    private void investigateSetOperation(CollectionUpdate<Attendee, AttendeeField> difference, Set<Integer> userIds, Set<Integer> groupIds, Set<Integer> resourceIds, Map<Integer, ChangeType> userChange, Map<Integer, ChangeType> resourceChange, Map<Integer, ChangeType> groupChange, Map<String, ChangeType> externalChange, ChangeType changeType, List<Attendee> list) {
        for (Attendee added : list) {
            if (added.getCuType().equals(CalendarUserType.INDIVIDUAL)) {
                if (CalendarUtils.isInternal(added)) {
                    userIds.add(added.getEntity());
                    userChange.put(added.getEntity(), changeType);
                } else {
                    externalChange.put(added.getEMail(), changeType);
                }
            } else if (added.getCuType().equals(CalendarUserType.RESOURCE)) {
                resourceIds.add(added.getEntity());
                resourceChange.put(added.getEntity(), changeType);
            } else if (added.getCuType().equals(CalendarUserType.GROUP)) {
                groupIds.add(added.getEntity());
                groupChange.put(added.getEntity(), changeType);
            }
        }
    }

    @Override
    public EventField[] getFields() {
        return FIELDS;
    }

}
