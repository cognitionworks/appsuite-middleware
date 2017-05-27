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
import static com.openexchange.folderstorage.Permission.CREATE_OBJECTS_IN_FOLDER;
import static com.openexchange.folderstorage.Permission.NO_PERMISSIONS;
import static com.openexchange.folderstorage.Permission.WRITE_OWN_OBJECTS;
import java.util.List;
import java.util.UUID;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Classification;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.EventStatus;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.TimeTransparency;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.AttendeeHelper;
import com.openexchange.chronos.impl.CalendarResultImpl;
import com.openexchange.chronos.impl.Check;
import com.openexchange.chronos.impl.Consistency;
import com.openexchange.chronos.impl.CreateResultImpl;
import com.openexchange.chronos.impl.EventMapper;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.java.Autoboxing;
import com.openexchange.java.Strings;

/**
 * {@link CreatePerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class CreatePerformer extends AbstractUpdatePerformer {

    /**
     * Initializes a new {@link CreatePerformer}.
     *
     * @param storage The underlying calendar storage
     * @param session The calendar session
     * @param folder The calendar folder representing the current view on the events
     */
    public CreatePerformer(CalendarStorage storage, CalendarSession session, UserizedFolder folder) throws OXException {
        super(storage, session, folder);
    }

    /**
     * Performs the creation of an event.
     *
     * @param event The event to create
     * @return The result
     */
    public CalendarResultImpl perform(Event event) throws OXException {
        /*
         * check current session user's permissions
         */
        requireCalendarPermission(folder, CREATE_OBJECTS_IN_FOLDER, NO_PERMISSIONS, WRITE_OWN_OBJECTS, NO_PERMISSIONS);
        /*
         * prepare event & attendee data for insert, check for conflicts
         */
        Event newEvent = prepareEvent(event);
        List<Attendee> newAttendees = prepareAttendees(event.getAttendees());
        Check.noConflicts(storage, session, newEvent, newAttendees);
        /*
         * insert event, attendees, attachments & alarms of user
         */
        storage.getEventStorage().insertEvent(newEvent);
        storage.getAttendeeStorage().insertAttendees(newEvent.getId(), newAttendees);
        if (null != event.getAlarms() && 0 < event.getAlarms().size()) {
            newEvent.setFolderId(folder.getID());
            insertAlarms(newEvent, calendarUser.getId(), Check.alarmsAreValid(event.getAlarms()), false);
        }
        if (null != event.getAttachments() && 0 < event.getAttachments().size()) {
            storage.getAttachmentStorage().insertAttachments(session.getSession(), folder.getID(), newEvent.getId(), event.getAttachments());
        }
        result.addCreation(new CreateResultImpl(loadEventData(newEvent.getId())));
        return result;
    }

    private List<Attendee> prepareAttendees(List<Attendee> attendeeData) throws OXException {
        return AttendeeHelper.onNewEvent(session, folder, attendeeData).getAttendeesToInsert();
    }

    private Event prepareEvent(Event eventData) throws OXException {
        Event event = new Event();
        /*
         * identifiers
         */
        event.setId(storage.getEventStorage().nextId());
        event.setPublicFolderId(PublicType.getInstance().equals(folder.getType()) ? folder.getID() : null);
        event.setSequence(0);
        if (false == eventData.containsUid() || Strings.isEmpty(eventData.getUid())) {
            event.setUid(UUID.randomUUID().toString());
        } else {
            event.setUid(Check.uidIsUnique(storage, eventData));
        }
        /*
         * creation/modification metadata, organizer
         */
        Consistency.setCreated(timestamp, event, calendarUser.getId());
        Consistency.setModified(timestamp, event, calendarUser.getId());
        event.setOrganizer(prepareOrganizer(eventData.getOrganizer()));
        /*
         * date/time related properties
         */
        Check.startAndEndDate(eventData);
        EventMapper.getInstance().copy(eventData, event, EventField.START_DATE, EventField.END_DATE, EventField.START_TIMEZONE, EventField.END_TIMEZONE);
        event.setAllDay(eventData.containsAllDay() ? eventData.getAllDay() : false);
        Consistency.adjustAllDayDates(event);
        Consistency.setTimeZone(event, calendarUser);
        /*
         * classification, status, transparency, color
         */
        if (eventData.containsClassification() && null != eventData.getClassification()) {
            event.setClassification(Check.classificationIsValid(eventData.getClassification(), folder));
        } else {
            event.setClassification(Classification.PUBLIC);
        }
        event.setStatus(eventData.containsStatus() && null != eventData.getStatus() ? eventData.getStatus() : EventStatus.CONFIRMED);
        event.setTransp(eventData.containsTransp() && null != eventData.getTransp() ? eventData.getTransp() : TimeTransparency.OPAQUE);
        event.setColor(eventData.containsColor() ? eventData.getColor() : null);
        /*
         * recurrence related fields
         */
        if (eventData.containsRecurrenceRule() && null != eventData.getRecurrenceRule()) {
            event.setRecurrenceRule(Check.recurrenceRuleIsValid(session.getRecurrenceService(), eventData));
            event.setSeriesId(event.getId());
            if (eventData.containsDeleteExceptionDates()) {
                event.setDeleteExceptionDates(Check.recurrenceIdsExist(session.getRecurrenceService(), eventData, eventData.getDeleteExceptionDates()));
            }
        }
        /*
         * copy over further (unchecked) event fields
         */
        return EventMapper.getInstance().copy(eventData, event, EventField.SUMMARY, EventField.LOCATION, EventField.DESCRIPTION, EventField.CATEGORIES, EventField.FILENAME);
    }

    /**
     * Prepares the organizer for a new event, taking over an external organizer if specified.
     *
     * @param organizerData The organizer as defined by the client
     * @return The prepared organizer
     */
    private Organizer prepareOrganizer(Organizer organizerData) throws OXException {
        Organizer organizer;
        if (null != organizerData) {
            organizer = session.getEntityResolver().prepare(organizerData, CalendarUserType.INDIVIDUAL);
            if (0 < organizer.getEntity()) {
                /*
                 * internal organizer must match the actual calendar user if specified
                 */
                if (organizer.getEntity() != calendarUser.getId()) {
                    throw CalendarExceptionCodes.INVALID_CALENDAR_USER.create(organizer.getUri(), Autoboxing.I(organizer.getEntity()), CalendarUserType.INDIVIDUAL);
                }
            } else {
                /*
                 * take over external organizer as-is
                 */
                return session.getConfig().isSkipExternalAttendeeURIChecks() ? organizer : Check.requireValidEMail(organizer);
            }
        } else {
            /*
             * prepare default organizer for calendar user
             */
            organizer = session.getEntityResolver().applyEntityData(new Organizer(), calendarUser.getId());
        }
        /*
         * apply "sent-by" property if someone is acting on behalf of the calendar user
         */
        if (calendarUser.getId() != session.getUserId()) {
            organizer.setSentBy(session.getEntityResolver().applyEntityData(new CalendarUser(), session.getUserId()));
        }
        return organizer;
    }

}
