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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.caldav.resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.api2.AppointmentSQLInterface;
import com.openexchange.api2.ReminderService;
import com.openexchange.caldav.CalDAVAgent;
import com.openexchange.caldav.GroupwareCaldavFactory;
import com.openexchange.caldav.ParticipantTools;
import com.openexchange.caldav.Patches;
import com.openexchange.caldav.Tools;
import com.openexchange.calendar.AppointmentDiff;
import com.openexchange.data.conversion.ical.ConversionError;
import com.openexchange.data.conversion.ical.ConversionWarning;
import com.openexchange.data.conversion.ical.ICalEmitter;
import com.openexchange.data.conversion.ical.ICalSession;
import com.openexchange.database.DatabaseService;
import com.openexchange.dav.DAVProtocol;
import com.openexchange.dav.PreconditionException;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXException.IncorrectString;
import com.openexchange.exception.OXException.Truncated;
import com.openexchange.folderstorage.Type;
import com.openexchange.folderstorage.type.PrivateType;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.folderstorage.type.SharedType;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.groupware.calendar.RecurringResultInterface;
import com.openexchange.groupware.calendar.RecurringResultsInterface;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.groupware.reminder.ReminderExceptionCode;
import com.openexchange.groupware.reminder.ReminderHandler;
import com.openexchange.groupware.reminder.ReminderObject;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;
import com.openexchange.webdav.protocol.WebdavPath;
import com.openexchange.webdav.protocol.WebdavProtocolException;

/**
 * {@link AppointmentResource}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class AppointmentResource extends CalDAVResource<Appointment> {

    /**
     * All appointment fields that may be set in iCal files
     */
    private static int[] CALDAV_FIELDS = {
        Appointment.END_DATE, // DTEND
        Appointment.SHOWN_AS, // TRANSP
        Appointment.LOCATION, // LOCATION
        Appointment.NOTE, // DESCRIPTION
        Appointment.PRIVATE_FLAG, // CLASS
        Appointment.TITLE, // SUMMARY
        Appointment.START_DATE, // DTSTART
        Appointment.PARTICIPANTS, Appointment.USERS, // ATTENDEE
        Appointment.FULL_TIME, // DTSTART/DTEND
        Appointment.ALARM, // VALARM
        Appointment.RECURRENCE_TYPE, // RRULE;FREQ
    };

    /**
     * All appointment recurrence fields that may be set in iCal files
     */
    private static int[] RECURRENCE_FIELDS = {
        Appointment.INTERVAL,
        Appointment.DAYS,
        Appointment.DAY_IN_MONTH,
        Appointment.MONTH,
        Appointment.RECURRENCE_COUNT,
        Appointment.UNTIL
    };

    private AppointmentSQLInterface appointmentInterface = null;
    private final AppointmentCollection parent;

    private List<CalendarDataObject> exceptionsToSave = null;
    private List<CalendarDataObject> deleteExceptionsToSave = null;
    private CalendarDataObject appointmentToSave = null;

    public AppointmentResource(final GroupwareCaldavFactory factory, final AppointmentCollection parent, final Appointment object, final WebdavPath url) throws OXException {
        super(factory, parent, object, url);
        this.parent = parent;
    }

    @Override
    public void create() throws WebdavProtocolException {
        try {
            super.create();
        } catch (WebdavProtocolException e) {
            handleOnCreate(e);
        }
    }

    @Override
    protected List<Appointment> getTargetedObjects(String[] recurrenceIDs) throws OXException {
        List<Appointment> objects = new ArrayList<Appointment>();
        if (null == recurrenceIDs) {
            /*
             * all recurrence instances are targeted
             */
            objects.add(object);
            CalendarDataObject[] changeExceptions = parent.loadChangeExceptions(object, false);
            if (null != changeExceptions && 0 < changeExceptions.length) {
                objects.addAll(Arrays.asList(changeExceptions));
            }
        } else {
            for (String recurrenceID : recurrenceIDs) {
                if ("M".equalsIgnoreCase(recurrenceID)) {
                    /*
                     * recurrence master instance
                     */
                    objects.add(object);
                } else {
                    /*
                     * specific instance, determine targeted recurrence date position
                     */
                    String exceptionICal = new StringBuilder()
                        .append("BEGIN:VCALENDAR\r\n")
                        .append("PRODID:a\r\n")
                        .append("BEGIN:VEVENT\r\n")
                        .append("DTSTART:").append(recurrenceID).append("\r\n")
                        .append("DTEND:").append(recurrenceID).append("\r\n")
                        .append("RECURRENCE-ID:").append(recurrenceID).append("\r\n")
                        .append("END:VEVENT\r\n")
                        .append("END:VCALENDAR\r\n")
                    .toString();
                    String timeZone = null != object.getTimezone() ? object.getTimezone() : factory.getUser().getTimeZone();
                    List<CalendarDataObject> appointments = factory.getIcalParser().parseAppointments(exceptionICal, TimeZone.getTimeZone(timeZone), factory.getContext(), new ArrayList<ConversionError>(), new ArrayList<ConversionWarning>());
                    if (null == appointments || 1 != appointments.size() || null == appointments.get(0).getRecurrenceDatePosition()) {
                        throw protocolException(HttpServletResponse.SC_NOT_FOUND);
                    }
                    /*
                     * get matching change exception
                     */
                    Date recurrenceDatePosition = appointments.get(0).getRecurrenceDatePosition();
                    CalendarDataObject[] originalExceptions = parent.loadChangeExceptions(object, false);
                    CalendarDataObject targetedException = getMatchingException(originalExceptions, recurrenceDatePosition);
                    if (null == targetedException) {
                        throw protocolException(HttpServletResponse.SC_NOT_FOUND);
                    }
                    objects.add(targetedException);
                }
            }
        }
        return objects;
    }

    private AppointmentSQLInterface getAppointmentInterface() {
        if (null == this.appointmentInterface) {
            this.appointmentInterface = factory.getAppointmentInterface();
        }
        return this.appointmentInterface;
    }

    @Override
    protected void deleteObject() throws OXException {
        try {
            getAppointmentInterface().deleteAppointmentObject(
                (CalendarDataObject) this.object, object.getParentFolderID(), object.getLastModified());
        } catch (final SQLException e) {
            throw protocolException(e);
        }
    }

    @Override
    protected void saveObject() throws OXException {
        saveObject(true);
    }

    protected void saveObject(boolean checkPermissions) throws OXException {
        try {
            /*
             * load original appointment
             */
            CalendarDataObject originalAppointment = parent.load(this.object, false);
            Date clientLastModified = this.object.getLastModified();
            if (clientLastModified.before(originalAppointment.getLastModified())) {
                throw WebdavProtocolException.Code.EDIT_CONFLICT.create(getUrl(), HttpServletResponse.SC_CONFLICT);
            }
            /*
             * handle private comments & reminders
             */
            handlePrivateComments(appointmentToSave);
            ReminderObject nextReminder = handleReminders(originalAppointment, appointmentToSave, exceptionsToSave);
            /*
             * update appointment
             */
            if (false == Patches.Incoming.tryRestoreParticipants(originalAppointment, appointmentToSave)) {
                Patches.Incoming.patchParticipantListRemovingAliases(factory, appointmentToSave);
                Patches.Incoming.patchParticipantListRemovingDoubleUsers(appointmentToSave);
                Patches.Incoming.removeParticipantsForPrivateAppointmentInPublicfolder(
                    factory.getSession().getUserId(), parent.getFolder(), appointmentToSave);
                if (PublicType.getInstance().equals(parent.getFolder().getType()) ||
                    PrivateType.getInstance().equals(parent.getFolder().getType())) {
                    Patches.Incoming.addUserParticipantIfEmpty(factory.getSession().getUserId(), appointmentToSave);
                }
            }
            checkForExplicitRemoves(originalAppointment, appointmentToSave);
            if (false == containsChanges(originalAppointment, appointmentToSave)) {
                LOG.debug("No changes detected in {}, skipping update.", appointmentToSave);
            } else {
                Appointment[] hardConflicts = getAppointmentInterface().updateAppointmentObject(appointmentToSave, parentFolderID, clientLastModified, checkPermissions);
                if (null != hardConflicts && 0 < hardConflicts.length) {
                    throw new PreconditionException(
                        DAVProtocol.CAL_NS.getURI(), "allowed-organizer-scheduling-object-change", getUrl(), HttpServletResponse.SC_FORBIDDEN);
                }
                if (null != appointmentToSave.getLastModified()) {
                    clientLastModified = appointmentToSave.getLastModified();
                }
            }
            /*
             * process attachments
             */
            Date lastModified = handleAttachments(originalAppointment, appointmentToSave);
            if (null != lastModified && lastModified.after(clientLastModified)) {
                clientLastModified = lastModified;
            }
            /*
             * save next reminder based on last acknowledged occurrence
             */
            if (null != nextReminder) {
                insertOrUpdateReminder(nextReminder);
            }
            if (0 == exceptionsToSave.size() && 0 == deleteExceptionsToSave.size()) {
                return;
            }
            /*
             * load original exceptions, transforming change- to delete-exceptions where user is removed from participants if needed (bug #26293)
             */
            CalendarDataObject[] originalExceptions = parent.loadChangeExceptions(this.object, false);
            if (null != originalExceptions && 0 < originalExceptions.length) {
                originalExceptions = Patches.Outgoing.setDeleteExceptionForRemovedParticipant(factory, originalAppointment, originalExceptions);
            }
            /*
             * update change exceptions
             */
            for (CalendarDataObject exceptionToSave : exceptionsToSave) {
                /*
                 * check if already deleted
                 */
                if (containsDeleteException(originalAppointment, exceptionToSave.getRecurrenceDatePosition())) {
                    LOG.debug("Delete exception {} already exists, skipping update.", exceptionToSave);
                    continue;
                }
                CalendarDataObject originalException = getMatchingException(originalExceptions, exceptionToSave.getRecurrenceDatePosition());
                handlePrivateComments(exceptionToSave);
                ReminderObject nextExceptionReminder = handleReminders(originalException, exceptionToSave, null);
                if (null != originalException) {
                    /*
                     * prepare exception update
                     */
                    exceptionToSave.setObjectID(originalException.getObjectID());
                    checkForExplicitRemoves(originalException, exceptionToSave);
                    if (false == Patches.Incoming.tryRestoreParticipants(originalException, exceptionToSave)) {
                        Patches.Incoming.patchParticipantListRemovingAliases(factory, exceptionToSave);
                        Patches.Incoming.patchParticipantListRemovingDoubleUsers(exceptionToSave);
                        Patches.Incoming.removeParticipantsForPrivateAppointmentInPublicfolder(
                            factory.getSession().getUserId(), parent.getFolder(), exceptionToSave);
                        if (PublicType.getInstance().equals(parent.getFolder().getType()) ||
                            PrivateType.getInstance().equals(parent.getFolder().getType())) {
                            Patches.Incoming.addUserParticipantIfEmpty(factory.getSession().getUserId(), exceptionToSave);
                        }
                    }
                } else {
                    /*
                     * prepare exception create
                     */
                    exceptionToSave.setObjectID(object.getObjectID());
                    if (false == Patches.Incoming.tryRestoreParticipants(originalAppointment, exceptionToSave)) {
                        Patches.Incoming.patchParticipantListRemovingAliases(factory, exceptionToSave);
                        Patches.Incoming.patchParticipantListRemovingDoubleUsers(exceptionToSave);
                        Patches.Incoming.removeParticipantsForPrivateAppointmentInPublicfolder(
                            factory.getSession().getUserId(), parent.getFolder(), exceptionToSave);
                        if (PublicType.getInstance().equals(parent.getFolder().getType()) ||
                            PrivateType.getInstance().equals(parent.getFolder().getType())) {
                            Patches.Incoming.addUserParticipantIfEmpty(factory.getSession().getUserId(), exceptionToSave);
                        }
                    }
                }
                /*
                 * create / update exception
                 */
                if (null != originalException && false == containsChanges(originalException, exceptionToSave)) {
                    LOG.debug("No changes detected in {}, skipping update.", exceptionToSave);
                } else {
                    Appointment[] hardConflicts = getAppointmentInterface().updateAppointmentObject(exceptionToSave, parentFolderID, clientLastModified, checkPermissions);
                    if (null != hardConflicts && 0 < hardConflicts.length) {
                        throw new PreconditionException(
                            DAVProtocol.CAL_NS.getURI(), "allowed-organizer-scheduling-object-change", getUrl(), HttpServletResponse.SC_FORBIDDEN);
                    }
                    if (null != exceptionToSave.getLastModified()) {
                        clientLastModified = exceptionToSave.getLastModified();
                    }
                }
                /*
                 * process attachments in exceptions (but not for the mac client who can't)
                 */
                if (false == CalDAVAgent.MAC_CALENDAR.equals(factory.getState().getUserAgent())) {
                    lastModified = handleAttachments(originalException, exceptionToSave);
                    if (null != lastModified && lastModified.after(clientLastModified)) {
                        clientLastModified = lastModified;
                    }
                }
                /*
                 * save next reminder based on last acknowledged occurrence
                 */
                if (null != nextExceptionReminder) {
                    ReminderObject reminder = optReminder(exceptionToSave);
                    if (null != reminder) {
                        reminder.setDate(nextExceptionReminder.getDate());
                        reminder.setRecurrencePosition(nextExceptionReminder.getRecurrencePosition());
                        insertOrUpdateReminder(reminder);
                    } else {
                        insertOrUpdateReminder(nextExceptionReminder);
                    }
                }
            }
            /*
             * update delete exceptions
             */
            for (CalendarDataObject deleteExceptionToSave : deleteExceptionsToSave) {
                if (containsDeleteException(originalAppointment, deleteExceptionToSave.getRecurrenceDatePosition())) {
                    LOG.debug("Delete exception {} already exists, skipping update.", deleteExceptionToSave);
                    continue;
                }
                Appointment originalException = getMatchingException(originalExceptions, deleteExceptionToSave.getRecurrenceDatePosition());
                if (null != originalException) {
                    /*
                     * prepare delete of existing exception
                     */
                    deleteExceptionToSave.setObjectID(originalException.getObjectID());
                } else {
                    /*
                     * prepare new delete exception
                     */
                    deleteExceptionToSave.setObjectID(object.getObjectID());
                }
                getAppointmentInterface().deleteAppointmentObject(deleteExceptionToSave, parentFolderID, clientLastModified, checkPermissions);
                if (null != deleteExceptionToSave.getLastModified()) {
                    clientLastModified = deleteExceptionToSave.getLastModified();
                }
            }
        } catch (SQLException e) {
            throw protocolException(e);
        }
    }

    @Override
    protected void createObject() throws OXException {
        try {
            /*
             * create appointment
             */
            appointmentToSave.removeObjectID(); // in case it's already assigned due to retry operations
            appointmentToSave.setParentFolderID(null != object ? object.getParentFolderID() : parentFolderID);
            if (PublicType.getInstance().equals(parent.getFolder().getType())) {
                Patches.Incoming.removeParticipantsForPrivateAppointmentInPublicfolder(
                    factory.getSession().getUserId(), parent.getFolder(), appointmentToSave);
                Patches.Incoming.addUserParticipantIfEmpty(factory.getSession().getUserId(), appointmentToSave);
            }
            handlePrivateComments(appointmentToSave);
            ReminderObject nextReminder = handleReminders(null, appointmentToSave, null);
            Appointment[] hardConflicts = getAppointmentInterface().insertAppointmentObject(appointmentToSave);
            if (null != hardConflicts && 0 < hardConflicts.length) {
                throw new PreconditionException(
                    DAVProtocol.CAL_NS.getURI(), "allowed-organizer-scheduling-object-change", getUrl(), HttpServletResponse.SC_FORBIDDEN);
            }
            Date clientLastModified = appointmentToSave.getLastModified();
            /*
             * process attachments & reminders
             */
            Date lastModified = handleAttachments(null, appointmentToSave);
            if (null != lastModified && lastModified.after(clientLastModified)) {
                clientLastModified = lastModified;
            }
            if (null != nextReminder) {
                ReminderObject reminder = optReminder(appointmentToSave);
                if (null != reminder) {
                    reminder.setDate(nextReminder.getDate());
                    reminder.setRecurrencePosition(nextReminder.getRecurrencePosition());
                    insertOrUpdateReminder(reminder);
                } else {
                    insertOrUpdateReminder(nextReminder);
                }
            }
            /*
             * create change exceptions
             */
            for (CalendarDataObject exception : exceptionsToSave) {
                exception.removeObjectID(); // in case it's already assigned due to retry operations
                exception.setObjectID(appointmentToSave.getObjectID());
                Patches.Incoming.removeParticipantsForPrivateAppointmentInPublicfolder(
                    factory.getSession().getUserId(), parent.getFolder(), exception);
                hardConflicts = getAppointmentInterface().updateAppointmentObject(exception, parentFolderID, clientLastModified);
                if (null != hardConflicts && 0 < hardConflicts.length) {
                    throw new PreconditionException(
                        DAVProtocol.CAL_NS.getURI(), "allowed-organizer-scheduling-object-change", getUrl(), HttpServletResponse.SC_FORBIDDEN);
                }
                clientLastModified = exception.getLastModified();
                /*
                 * process attachments in exceptions (but not for the mac client who can't)
                 */
                if (false == CalDAVAgent.MAC_CALENDAR.equals(factory.getState().getUserAgent())) {
                    lastModified = handleAttachments(null, exception);
                    if (null != lastModified && lastModified.after(clientLastModified)) {
                        clientLastModified = lastModified;
                    }
                }
            }
            /*
             * create delete exceptions
             */
            for (CalendarDataObject exception : deleteExceptionsToSave) {
                exception.setObjectID(appointmentToSave.getObjectID());
                getAppointmentInterface().deleteAppointmentObject(exception, parentFolderID, clientLastModified);
                clientLastModified = exception.getLastModified();
            }
        } catch (final SQLException e) {
            throw protocolException(e);
        }
    }

    @Override
    protected void move(CalDAVFolderCollection<Appointment> target) throws OXException {
        this.appointmentToSave = new CalendarDataObject();
        appointmentToSave.setObjectID(object.getObjectID());
        appointmentToSave.setParentFolderID(Tools.parse(target.getFolder().getID()));
        appointmentToSave.setContext(factory.getContext());
        Patches.Incoming.removeParticipantsForPrivateAppointmentInPublicfolder(
            factory.getSession().getUserId(), target.getFolder(), appointmentToSave);
        getAppointmentInterface().updateAppointmentObject(appointmentToSave, parentFolderID, object.getLastModified());
    }

    /**
     * Applies private attendee comment properties to the supplied appointment, based on the current session's user being the organizer
     * or attendee of the meeting.
     *
     * @param appointment The appointment to apply private attendee comment properties for
     */
    private void applyPrivateComments(CalendarDataObject appointment) throws OXException {
        if (appointment.getOrganizerId() == factory.getSession().getUserId()) {
            /*
             * provide all attendee comments for organizer
             */
            appointment.setProperty("com.openexchange.data.conversion.ical.participants.attendeeComments", Boolean.TRUE);
        } else {
            /*
             * provide the current users confirmation message
             */
            Participant[] participants = appointment.getParticipants();
            if (null != participants && 0 < participants.length) {
                /*
                 * set current users confirmation message
                 */
                for (Participant participant : participants) {
                    if (Participant.USER == participant.getType() && participant.getIdentifier() == factory.getSession().getUserId()) {
                        appointment.setProperty("com.openexchange.data.conversion.ical.participants.privateComment", ((UserParticipant) participant).getConfirmMessage());
                        break;
                    }
                }
            }
        }
    }

    private void applyReminderProperties(CalendarDataObject appointment) throws OXException {
        ReminderObject reminder = optReminder(appointment);
        if (null != reminder) {
            /*
             * set last acknowledged date one minute prior next trigger time
             */
            String timeZone = null != appointment.getTimezone() ? appointment.getTimezone() : factory.getUser().getTimeZone();
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
            calendar.setTime(reminder.getDate());
            calendar.add(Calendar.MINUTE, -1);
            appointment.setProperty("com.openexchange.data.conversion.ical.alarm.acknowledged", calendar.getTime());
            /*
             * check if reminder is a 'snoozed' one (by checking against the next regular trigger time)
             */
            Date now = new Date();
            Date rangeStart = now.after(reminder.getDate()) ? now : reminder.getDate();
            int reminderMinutes = appointment.getAlarm();
            calendar.setTime(rangeStart);
            calendar.add(Calendar.MINUTE, -1 * reminderMinutes);
            rangeStart = calendar.getTime();
            ReminderObject regularReminder = calculateNextReminder(appointment, rangeStart, null);
            if (null != regularReminder && regularReminder.getDate().before(reminder.getDate())) {
                /*
                 * consider reminder as 'snoozed', store parameter for client-specific serialization
                 */
                switch (factory.getState().getUserAgent()) {
                    case THUNDERBIRD_LIGHTNING:
                        /*
                         * Thunderbird/Lightning likes to have a custom "X-MOZ-SNOOZE-TIME-<timestamp_of_recurrence>" property for recurring
                         * events, and a custom "X-MOZ-SNOOZE-TIME" property for non-recurring ones
                         */
                        appointment.setProperty("com.openexchange.data.conversion.ical.alarm.mozSnooze", reminder.getDate());
                        if (appointment.isMaster()) {
                            calendar.setTime(regularReminder.getDate());
                            calendar.add(Calendar.MINUTE, reminderMinutes);
                            Date recurrenceID = calendar.getTime();
                            appointment.setProperty("com.openexchange.data.conversion.ical.alarm.mozSnoozeTimestamp", recurrenceID);
                        }
                        break;
                    case IOS_CALENDAR:
                    case MAC_CALENDAR:
                        /*
                         * Apple clients prefer a relative snooze time duration in the trigger of appointment series
                         */
                        if (appointment.isMaster()) {
                            calendar.setTime(regularReminder.getDate());
                            calendar.add(Calendar.MINUTE, reminderMinutes);
                            Date startDate = calendar.getTime();
                            long diff = startDate.getTime() - reminder.getDate().getTime();
                            if (diff >= 0) {
                                appointment.setProperty("com.openexchange.data.conversion.ical.alarm.relativeSnooze", Integer.valueOf((int) (diff / 1000)));
                                break;
                            }
                        }
                        // fall through, otherwise
                    default:
                        /*
                         * apply default snooze handling
                         */
                        appointment.setProperty("com.openexchange.data.conversion.ical.alarm.snooze", reminder.getDate());
                        break;
                }
            }
        }
    }

    @Override
    protected String generateICal() throws OXException {
        ICalEmitter icalEmitter = factory.getIcalEmitter();
        ICalSession session = icalEmitter.createSession();
        List<ConversionError> conversionErrors = new LinkedList<ConversionError>();
        List<ConversionWarning> conversionWarnings = new LinkedList<ConversionWarning>();
        try {
            /*
             * load appointment & apply extended properties for serialization
             */
            CalendarDataObject appointment = parent.load(object, true);
            applyReminderProperties(appointment);
            applyPrivateComments(appointment);
            applyAttachments(appointment);
            CalendarDataObject[] changeExceptions = 0 < object.getRecurrenceID() ? parent.loadChangeExceptions(object, true) : null;
            /*
             * transform change exceptions to delete-exceptions where user is removed from participants if needed (bug #26293)
             */
            if (null != changeExceptions && 0 < changeExceptions.length) {
                changeExceptions = Patches.Outgoing.setDeleteExceptionForRemovedParticipant(factory, appointment, changeExceptions);
            }
            /*
             * write appointment
             */
            icalEmitter.writeAppointment(session, appointment, factory.getContext(), conversionErrors, conversionWarnings);
            /*
             * write exceptions
             */
            if (null != changeExceptions && 0 < changeExceptions.length) {
                for (CalendarDataObject changeException : changeExceptions) {
                    applyReminderProperties(changeException);
                    applyPrivateComments(changeException);
                    if (false == CalDAVAgent.MAC_CALENDAR.equals(factory.getState().getUserAgent())) {
                        applyAttachments(changeException);
                    }
                    icalEmitter.writeAppointment(session, changeException, factory.getContext(), conversionErrors, conversionWarnings);
                }
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            icalEmitter.writeSession(session, bytes);
            /*
             * apply patches
             */
            String iCal = new String(bytes.toByteArray(), "UTF-8");
            iCal = Patches.Outgoing.removeEmptyRDates(iCal);
            return iCal;
        } catch (UnsupportedEncodingException e) {
            throw protocolException(e);
        }
    }

    @Override
    protected void deserialize(final InputStream body) throws OXException, IOException {
        final List<CalendarDataObject> appointments = this.parse(body);
        if (null != appointments && 0 < appointments.size()) {
            this.deleteExceptionsToSave = new ArrayList<CalendarDataObject>();
            this.exceptionsToSave = new ArrayList<CalendarDataObject>();
            for (final CalendarDataObject cdo : appointments) {
                cdo.setContext(factory.getContext());
                cdo.removeLastModified();
                cdo.setIgnoreConflicts(true);
                if (null != this.object) {
                    cdo.setParentFolderID(this.object.getParentFolderID());
                    cdo.removeUid();
                } else {
                    cdo.setParentFolderID(this.parentFolderID);
                }
                if (1 == appointments.size() || looksLikeMaster(cdo)) {
                    if (null != object) {
                        cdo.setObjectID(object.getObjectID());
                    }
                    this.appointmentToSave = cdo;
                    createNewDeleteExceptions(this.object, appointmentToSave);
                } else {
                    factory.getCalendarUtilities().removeRecurringType(cdo);
                    if (null != object) {
                        cdo.setRecurrenceID(object.getObjectID());
                    }
                    exceptionsToSave.add(cdo);
                }
            }
            /*
             * store filename when different from uid
             */
            final String resourceName = super.extractResourceName();
            if (null != resourceName && false == resourceName.equals(appointmentToSave.getUid())) {
                appointmentToSave.setFilename(resourceName);
            }
        } else {
            throw new PreconditionException(DAVProtocol.CAL_NS.getURI(), "supported-calendar-component", getUrl(), HttpServletResponse.SC_FORBIDDEN);
        }
    }

    private List<CalendarDataObject> parse(InputStream body) throws IOException, ConversionError {
        try {
            if (LOG.isTraceEnabled()) {
                byte[] iCal = Streams.stream2bytes(body);
                LOG.trace(new String(iCal, Charsets.UTF_8));
                body = Streams.newByteArrayInputStream(iCal);
            }
            return factory.getIcalParser().parseAppointments(
                body, getTimeZone(), factory.getContext(), new ArrayList<ConversionError>(), new ArrayList<ConversionWarning>());
        } finally {
            Streams.close(body);
        }
    }

    private static boolean containsChanges(Appointment oldAppointment, CalendarDataObject cdo) {
        AppointmentDiff diff = AppointmentDiff.compare(oldAppointment, cdo);
        return diff.anyFieldChangedOf(CALDAV_FIELDS) ||
            CalendarObject.NO_RECURRENCE != oldAppointment.getRecurrenceType() &&
            CalendarObject.NO_RECURRENCE != cdo.getRecurrenceType() &&
            diff.anyFieldChangedOf(RECURRENCE_FIELDS);
    }

    private void checkForExplicitRemoves(Appointment oldAppointment, CalendarDataObject updatedAppointment) {
        /*
         * reset previously set appointment fields
         */
        for (int field : CALDAV_FIELDS) {
            /*
             * skip special handlings
             */
            if (CalendarObject.ALARM == field || Appointment.SHOWN_AS == field) {
                continue;
            }
            if (oldAppointment.contains(field) && false == updatedAppointment.contains(field)) {
                updatedAppointment.set(field, updatedAppointment.get(field));
            }
        }
        /*
         * reset previously set recurrence specific fields
         */
        if (CalendarObject.NO_RECURRENCE != oldAppointment.getRecurrenceType() &&
            CalendarObject.NO_RECURRENCE != updatedAppointment.getRecurrenceType()) {
            for (int field : RECURRENCE_FIELDS) {
                if (oldAppointment.contains(field) && false == updatedAppointment.contains(field)) {
                    if (CalendarObject.UNTIL == field) {
                        // getUntil returns 'max until date' if not set
                        updatedAppointment.set(field, null);
                    } else if (CalendarObject.DAYS == field) {
                        // days must not be 'set' here, even not to '0'
                        updatedAppointment.removeDays();
                    } else {
                        updatedAppointment.set(field, updatedAppointment.get(field));
                    }
                }
            }
        }
        /*
         * special handling for "alarm"
         */
        Type folderType = parent.getFolder().getType();
        if (PublicType.getInstance().equals(folderType) || SharedType.getInstance().equals(folderType)) {
            int oldReminder = ParticipantTools.getReminderMinutes(oldAppointment, factory.getUser().getId());
            if (-1 != oldReminder && false == updatedAppointment.containsAlarm()) {
                updatedAppointment.setAlarm(-1);
            }
        } else if (PrivateType.getInstance().equals(folderType)) {
            if (oldAppointment.containsAlarm() && false == updatedAppointment.containsAlarm()) {
                updatedAppointment.setAlarm(-1);
            }
        }
        /*
         * special handling for "shown as"
         */
        if (updatedAppointment.containsShownAs() && oldAppointment.containsShownAs() &&
            updatedAppointment.getShownAs() != oldAppointment.getShownAs()) {
            if (Appointment.RESERVED == updatedAppointment.getShownAs() &&
                (Appointment.ABSENT == oldAppointment.getShownAs() || Appointment.TEMPORARY == oldAppointment.getShownAs())) {
                // don't change "shown as", since iCal maps absent/temporary to reserved
                updatedAppointment.removeShownAs();
            } else if ((updatedAppointment.containsOrganizerId() && factory.getSession().getUserId() != updatedAppointment.getOrganizerId() ||
                updatedAppointment.containsOrganizer() && null != updatedAppointment.getOrganizer() &&
                false == updatedAppointment.getOrganizer().equals(factory.getUser().getMail())) &&
                isConfirmationChange(oldAppointment, updatedAppointment)) {
                // don't change "shown as", since iCal clients tend to change the transparency on accept/decline actions of participants
                updatedAppointment.removeShownAs();
            }
        }
    }

    private boolean isConfirmationChange(Appointment oldAppointment, CalendarDataObject updatedAppointment) {
        UserParticipant oldParticipant = getCurrentUserParticipant(oldAppointment);
        UserParticipant updatedParticipant = getCurrentUserParticipant(updatedAppointment);
        return null != oldParticipant && null != updatedParticipant && oldParticipant.getConfirm() != updatedParticipant.getConfirm();
    }

    private UserParticipant getCurrentUserParticipant(Appointment appointment) {
        if (null != appointment && null != appointment.getParticipants() && 0 < appointment.getParticipants().length) {
            int userID = factory.getUser().getId();
            for (Participant participant : appointment.getParticipants()) {
                if (UserParticipant.class.isInstance(participant)) {
                    UserParticipant userParticipant = (UserParticipant)participant;
                    if (userID == userParticipant.getIdentifier()) {
                        return userParticipant;
                    }
                }
            }
        }
        return null;
    }

    private void createNewDeleteExceptions(final Appointment oldAppointment, final CalendarDataObject cdo) throws OXException {
        final Date[] wantedDeleteExceptions = cdo.getDeleteException();
        if (wantedDeleteExceptions == null || wantedDeleteExceptions.length == 0) {
            return;
        }
        // Normalize the wanted DelEx to midnight, and add them to our set.
        final Set<Date> wantedSet = new HashSet<Date>(Arrays.asList(wantedDeleteExceptions));

        if (null != oldAppointment && null != oldAppointment.getDeleteException()) {
            for (Date knownDeleteException : oldAppointment.getDeleteException()) {
                wantedSet.remove(knownDeleteException);
            }
        }

        for (final Date date : wantedSet) {
            final CalendarDataObject deleteException = new CalendarDataObject();
            deleteException.setRecurrenceDatePosition(date);
            deleteException.setContext(factory.getContext());
            deleteException.setParentFolderID(parentFolderID);
            deleteExceptionsToSave.add(deleteException);
        }

        cdo.removeDeleteExceptions();
    }

    @Override
    protected boolean trimTruncatedAttribute(final Truncated truncated) {
        boolean hasTrimmed = false;
        if (null != this.appointmentToSave) {
            hasTrimmed |= trimTruncatedAttribute(truncated, appointmentToSave);
        }
        if (null != this.exceptionsToSave && 0 < this.exceptionsToSave.size()) {
            for (final CalendarDataObject calendarObject : exceptionsToSave) {
                hasTrimmed |= trimTruncatedAttribute(truncated, calendarObject);
            }
        }
        return hasTrimmed;
    }

    @Override
    protected boolean replaceIncorrectStrings(IncorrectString incorrectString, String replacement) {
        boolean hasReplaced = false;
        if (null != this.appointmentToSave) {
            hasReplaced |= replaceIncorrectString(incorrectString, appointmentToSave, replacement);
        }
        if (null != this.exceptionsToSave && 0 < this.exceptionsToSave.size()) {
            for (final CalendarDataObject calendarObject : exceptionsToSave) {
                hasReplaced |= replaceIncorrectString(incorrectString, calendarObject, replacement);
            }
        }
        return hasReplaced;
    }

    /**
     * Tries to handle a {@link WebdavProtocolException} that occured during resource creation automatically.
     *
     * @param e The exception
     * @throws WebdavProtocolException If not handled
     */
    private void handleOnCreate(WebdavProtocolException e) throws WebdavProtocolException {
        if (null != e && null != e.getCause() && OXException.class.isInstance(e.getCause()) &&
            "APP-0100".equals(((OXException)e.getCause()).getErrorCode())) {
            /*
             * Cannot insert appointment (...). An appointment with the unique identifier (...) already exists.
             */
            try {
                int objectID = getAppointmentInterface().resolveUid(appointmentToSave.getUid());
                if (0 < objectID) {
                    CalendarDataObject existingAppointment = getAppointmentInterface().getObjectById(objectID);
                    if (isUpdate(appointmentToSave, existingAppointment) &&
                        PrivateType.getInstance().equals(parent.getFolder().getType())) {
                        LOG.debug("Considering appointment with UID '{}', sequence {} as update for appointment with object ID {}, sequence {}.", appointmentToSave.getUid(), appointmentToSave.getSequence(), objectID, existingAppointment.getSequence());
                        this.object = existingAppointment;
                        appointmentToSave.setObjectID(objectID);
                        appointmentToSave.removeParentFolderID();
                        this.saveObject(false); // update instead of create
                        return; // handled
                    }
                }
            } catch (OXException x) {
                LOG.warn("Error during automatic exception handling", x);
            } catch (SQLException x) {
                LOG.warn("Error during automatic exception handling", x);
            }
        }
        /*
         * re-throw if not handled
         */
        throw e;
    }

    /**
     * Handles incoming private attendee comments for the current user, based on the property
     * <code>com.openexchange.data.conversion.ical.participants.privateComment</code>.
     *
     * @param updatedAppointment The updated appointment
     */
    private void handlePrivateComments(CalendarDataObject updatedAppointment) {
        String privateComment = updatedAppointment.getProperty("com.openexchange.data.conversion.ical.participants.privateComment");
        if (null != privateComment) {
            updatedAppointment.removeProperty("com.openexchange.data.conversion.ical.participants.privateComment");
            Participant[] participants = updatedAppointment.getParticipants();
            if (null != participants && 0 < participants.length) {
                /*
                 * set current users confirmation message
                 */
                for (Participant participant : participants) {
                    if (Participant.USER == participant.getType() && participant.getIdentifier() == factory.getSession().getUserId()) {
                        ((UserParticipant) participant).setConfirmMessage(privateComment);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Handles incoming reminders that have previously been "acknowledged" or "snoozed" on the client-side, based on the properties
     * <code>com.openexchange.data.conversion.ical.alarm.acknowledged</code> and
     * <code>com.openexchange.data.conversion.ical.alarm.snooze</code> (as inserted by the iCal parser).
     * <p/>
     * In case of acknowledged alarms in single events (i.e. non-recurring appointments or explicit appointment exceptions),
     * the alarm property is removed from this appointment and a potentially stored reminder trigger is deleted. Snoozed alarms are
     * updated appropriately.
     * <p/>
     * For appointments representing the series master, the next reminder time is calculated based on the snooze- and acknowledged date
     * and returned to be stored in the reminder service later on.
     *
     * @param originalAppointment The original appointment, or <code>null</code> if there is none
     * @param updatedAppointment The updated appointment, possibly holding the
     *                           <code>com.openexchange.data.conversion.ical.alarm.acknowledged</code> property
     * @return The next reminder to store, or <code>null</code> if no further actions are required
     */
    private ReminderObject handleReminders(CalendarDataObject originalAppointment, CalendarDataObject updatedAppointment, List<CalendarDataObject> exceptionsToSave) throws OXException {
        if (false == updatedAppointment.containsAlarm()) {
            return null;
        }
        boolean recurring = null != originalAppointment && originalAppointment.isMaster() || looksLikeMaster(updatedAppointment);
        Date now = new Date();
        Date acknowledgedDate = updatedAppointment.getProperty("com.openexchange.data.conversion.ical.alarm.acknowledged");
        Date snoozeDate = updatedAppointment.getProperty("com.openexchange.data.conversion.ical.alarm.snooze");
        Integer relativeSnooze = updatedAppointment.getProperty("com.openexchange.data.conversion.ical.alarm.relativeSnooze");
        updatedAppointment.removeProperty("com.openexchange.data.conversion.ical.alarm.acknowledged");
        updatedAppointment.removeProperty("com.openexchange.data.conversion.ical.alarm.snooze");
        updatedAppointment.removeProperty("com.openexchange.data.conversion.ical.alarm.relativeSnooze");
        /*
         * check & adjust a snoozed alarm in a series occurrence in the Mac OS client's way
         */
        if (recurring && CalDAVAgent.MAC_CALENDAR.equals(factory.getState().getUserAgent()) &&
            null != exceptionsToSave && 1 == exceptionsToSave.size() && null != exceptionsToSave.get(0)) {
            CalendarDataObject exceptionToSave = exceptionsToSave.get(0);
            Date exceptionAcknowledgedDate = exceptionToSave.getProperty("com.openexchange.data.conversion.ical.alarm.acknowledged");
            Date exceptionSnoozeDate = exceptionToSave.getProperty("com.openexchange.data.conversion.ical.alarm.snooze");
            if (null != exceptionAcknowledgedDate && null != exceptionSnoozeDate && null != exceptionToSave.getStartDate() && null != exceptionToSave.getEndDate()) {
                AppointmentDiff diff = AppointmentDiff.compare(appointmentToSave, exceptionToSave,
                    Appointment.RECURRENCE_DATE_POSITION, Appointment.RECURRENCE_ID, Appointment.RECURRENCE_TYPE, Appointment.RECURRENCE_POSITION,
                    Appointment.RECURRENCE_START, Appointment.RECURRENCE_COUNT, Appointment.START_DATE, Appointment.END_DATE);
                if (diff.getUpdates().isEmpty()) {
                    RecurringResultsInterface recurringResults = factory.getCalendarUtilities().calculateRecurringIgnoringExceptions(
                        originalAppointment, exceptionToSave.getStartDate().getTime(), exceptionToSave.getEndDate().getTime(), 0);
                    for (int i = 0; i < recurringResults.size(); i++) {
                        RecurringResultInterface recurringResult = recurringResults.getRecurringResult(i);
                        if (null != recurringResult && recurringResult.getStart() == exceptionToSave.getStartDate().getTime() &&
                            recurringResult.getEnd() == exceptionToSave.getEndDate().getTime()) {
                            /*
                             * found new exception matching the original timeslot, containing an acknowledged & snoozed alarm =>
                             * take over properties to series master for further processing & ignore change new exception
                             */
                            acknowledgedDate = exceptionAcknowledgedDate;
                            snoozeDate = exceptionSnoozeDate;
                            exceptionsToSave.clear();
                            break;
                        }
                    }
                }
            }
        }
        /*
         * take over snoozed alarm if valid
         */
        if (null != snoozeDate && snoozeDate.after(now) && (null == acknowledgedDate || snoozeDate.after(acknowledgedDate))) {
            ReminderObject reminder = optReminder(originalAppointment);
            if (null == reminder) {
                reminder = new ReminderObject();
                reminder.setRecurrenceAppointment(recurring);
                reminder.setModule(Types.APPOINTMENT);
                reminder.setUser(factory.getUser().getId());
                if (null != originalAppointment) {
                    reminder.setFolder(originalAppointment.getParentFolderID());
                    reminder.setTargetId(originalAppointment.getObjectID());
                }
            }
            reminder.setDate(snoozeDate);
            return reminder;
        }
        if (null != relativeSnooze) {
            ReminderObject reminder = optReminder(originalAppointment);
            if (recurring) {
                Date startDate = null != acknowledgedDate ? acknowledgedDate : now;
                return calculateNextReminder(updatedAppointment, startDate, reminder, relativeSnooze.intValue());
            } else {
                Date startDate = null != acknowledgedDate ? acknowledgedDate : updatedAppointment.getStartDate();
                return calculateNextReminder(updatedAppointment, startDate, reminder, relativeSnooze.intValue());
            }
        }
        /*
         * if not yet acknowledged, take over reminder minutes if different from original
         */
        if (null == acknowledgedDate) {
            if (null != originalAppointment && originalAppointment.getAlarm() == updatedAppointment.getAlarm()) {
                updatedAppointment.removeAlarm();
            }
            return null;
        }
        /*
         * alarm is indicated as acknowledged, remove or re-schedule reminder
         */
        ReminderObject existingReminder = optReminder(originalAppointment);
        if (false == recurring) {
            String timeZone = null != updatedAppointment && null != updatedAppointment.getTimezone() ? updatedAppointment.getTimezone() :
                null != originalAppointment && null != originalAppointment.getTimezone() ? originalAppointment.getTimezone() : factory.getUser().getTimeZone();
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
            calendar.setTime(updatedAppointment.getStartDate());
            calendar.add(Calendar.MINUTE, -1 * updatedAppointment.getAlarm());
            Date trigger = calendar.getTime();
            /*
             * assume alarm is acknowledged, if acknowledged date is after trigger, and alarm is not updated concurrently
             */
            if (false == acknowledgedDate.before(trigger) && (null == originalAppointment || originalAppointment.getAlarm() == updatedAppointment.getAlarm())) {
                updatedAppointment.setAlarm(-1);
            }
        } else {
            /*
             * reminder of appointment series is acknowledged, calculate next trigger date
             */
            return calculateNextReminder(null != originalAppointment ? originalAppointment : updatedAppointment, acknowledgedDate, existingReminder);
        }
        return null;
    }

    /**
     * Optionally gets the current user's reminder associated with the supplied appointment.
     *
     * @param appointment The appointment to get the reminder for
     * @return The reminder, or <code>null</code> if there is none
     */
    private ReminderObject optReminder(CalendarDataObject appointment) throws OXException {
        if (null != appointment) {
            try {
                return new ReminderHandler(factory.getContext()).loadReminder(appointment.getObjectID(), factory.getSession().getUserId(), Types.APPOINTMENT);
            } catch (OXException e) {
                if (false == ReminderExceptionCode.NOT_FOUND.equals(e)) {
                    throw e;
                }
            }
        }
        return null;
    }

    private void insertOrUpdateReminder(ReminderObject reminder) throws OXException {
        ReminderService reminderService = new ReminderHandler(factory.getContext());
        DatabaseService databaseService = factory.requireService(DatabaseService.class);
        boolean committed = false;
        Connection connection = null;
        try {
            connection = databaseService.getWritable(factory.getContext());
            connection.setAutoCommit(false);
            if (0 < reminder.getObjectId()) {
                ReminderObject reloadedReminder = null;
                try {
                    reloadedReminder = reminderService.loadReminder(reminder.getTargetId(), reminder.getUser(), reminder.getModule(), connection);
                    reloadedReminder.setDate(reminder.getDate());
                    if (0 < reminder.getRecurrencePosition()) {
                        reloadedReminder.setRecurrencePosition(reminder.getRecurrencePosition());
                    }
                    reminderService.updateReminder(reloadedReminder, connection);
                    connection.commit();
                    committed = true;
                    return;
                } catch (OXException e) {
                    if (false == ReminderExceptionCode.NOT_FOUND.equals(e)) {
                        throw e;
                    }
                }
            }
            reminder.setObjectId(0);
            reminderService.insertReminder(reminder);
            connection.commit();
            committed = true;
        } catch (SQLException e) {
            throw ReminderExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            if (null != connection) {
                com.openexchange.tools.sql.DBUtils.autocommit(connection);
                if (committed) {
                    databaseService.backWritable(factory.getContext(), connection);
                } else {
                    databaseService.backWritableAfterReading(factory.getContext(), connection);
                }
            }
        }
    }

    /**
     * Calculates the next trigger date for an appointment's reminder after a specific date.
     *
     * @param appointment The appointment to calculate the reminder for
     * @param startDate The (exclusive) start date for the reminder date to consider
     * @param existingReminder A previously loaded existing reminder, or <code>null</code> if not available
     * @return The next reminder, or <code>null</code> if there is none
     * @throws OXException
     */
    private ReminderObject calculateNextReminder(CalendarDataObject appointment, Date startDate, ReminderObject existingReminder) throws OXException {
        return calculateNextReminder(appointment, startDate, existingReminder, 60 * appointment.getAlarm());
    }

    /**
     * Calculates the next trigger date for an appointment's reminder after a specific date.
     *
     * @param appointment The appointment to calculate the reminder for
     * @param startDate The (exclusive) start date for the reminder date to consider
     * @param existingReminder A previously loaded existing reminder, or <code>null</code> if not available
     * @param reminderSeconds The trigger interval of the reminder (prior the appointment's start) in seconds
     * @return The next reminder, or <code>null</code> if there is none
     * @throws OXException
     */
    private ReminderObject calculateNextReminder(CalendarDataObject appointment, Date startDate, ReminderObject existingReminder, int reminderSeconds) throws OXException {
        if (false == appointment.containsAlarm()) {
            return null;
        }
        String timeZone = null != appointment.getTimezone() ? appointment.getTimezone() : factory.getUser().getTimeZone();
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
        if (appointment.isMaster() || looksLikeMaster(appointment)) {
            RecurringResultsInterface recurringResults = factory.getCalendarUtilities().calculateRecurring(appointment, startDate.getTime(), appointment.getUntil().getTime(), 0);
            if (null == recurringResults || 0 == recurringResults.size()) {
                return null;
            }
            for (int i = 0; i < recurringResults.size(); i++) {
                RecurringResultInterface recurringResult = recurringResults.getRecurringResult(i);
                calendar.setTimeInMillis(recurringResult.getStart());
                calendar.add(Calendar.SECOND, -1 * reminderSeconds);
                if (calendar.getTime().after(startDate)) {
                    if (null == existingReminder) {
                        ReminderObject reminder = new ReminderObject();
                        reminder.setRecurrenceAppointment(true);
                        reminder.setRecurrencePosition(recurringResult.getPosition());
                        reminder.setDate(calendar.getTime());
                        reminder.setModule(Types.APPOINTMENT);
                        reminder.setUser(factory.getUser().getId());
                        reminder.setFolder(appointment.getParentFolderID());
                        reminder.setTargetId(appointment.getObjectID());
                        return reminder;
                    } else {
                        existingReminder.setRecurrenceAppointment(true);
                        existingReminder.setRecurrencePosition(recurringResult.getPosition());
                        existingReminder.setDate(calendar.getTime());
                        return existingReminder;
                    }
                }
            }
        } else {
            calendar.setTime(appointment.getStartDate());
            calendar.add(Calendar.SECOND, -1 * reminderSeconds);
            Date time = calendar.getTime();
            if (false == time.before(startDate)) {
                if (null == existingReminder) {
                    ReminderObject reminder = new ReminderObject();
                    reminder.setRecurrenceAppointment(false);
                    reminder.setDate(time);
                    reminder.setModule(Types.APPOINTMENT);
                    reminder.setUser(factory.getUser().getId());
                    reminder.setFolder(appointment.getParentFolderID());
                    reminder.setTargetId(appointment.getObjectID());
                    return reminder;
                } else {
                    existingReminder.setDate(time);
                    return existingReminder;
                }
            }
        }
        return null;
    }

    private static boolean isUpdate(CalendarDataObject newAppointment, CalendarDataObject existingAppointment) {
        /*
         * check uid
         */
        if (null == newAppointment.getUid() && false == newAppointment.getUid().equals(existingAppointment.getUid())) {
            return false;
        }
        /*
         * check sequence numbers
         */
        if (newAppointment.getSequence() <= existingAppointment.getSequence()) {
            return false;
        }
        /*
         * check organizer
         */
        if (null == newAppointment.getOrganizer() && null != existingAppointment.getOrganizer() ||
            newAppointment.containsOrganizerId() && newAppointment.getOrganizerId() != existingAppointment.getOrganizerId() ||
            null != newAppointment.getOrganizer() && false == newAppointment.getOrganizer().equals(existingAppointment.getOrganizer())) {
            return false;
        }
        /*
         * all checks passed, consider as update
         */
        return true;
    }

    private static boolean trimTruncatedAttribute(final Truncated truncated, final CalendarDataObject calendarObject) {
        int field = truncated.getId();
        if (field <= 0) {
            return false;
        }

        Object value = calendarObject.get(field);
        if (null != value && String.class.isInstance(value)) {
            String stringValue = (String)value;
            if (stringValue.length() > truncated.getMaxSize()) {
                calendarObject.set(field, stringValue.substring(0, truncated.getMaxSize()));
                return true;
            }
        }
        return false;
    }

    private static boolean replaceIncorrectString(IncorrectString incorrectString, CalendarDataObject calendarObject, String replacement) {
        Object value = calendarObject.get(incorrectString.getId());
        if (null != value && String.class.isInstance(value)) {
            String stringValue = (String) value;
            String replacedString = stringValue.replaceAll(incorrectString.getIncorrectString(), replacement);
            if (false == stringValue.equals(replacedString)) {
                calendarObject.set(incorrectString.getId(), replacedString);
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeMaster(final CalendarDataObject cdo) {
        return cdo.containsRecurrenceType() && CalendarObject.NO_RECURRENCE != cdo.getRecurrenceType();
    }

    private static CalendarDataObject getMatchingException(CalendarDataObject[] changeExceptions, Date recurrenceDatePosition) {
        if (null != changeExceptions) {
            for (CalendarDataObject existingException : changeExceptions) {
                if (existingException.getRecurrenceDatePosition().equals(recurrenceDatePosition)) {
                    return existingException;
                }
            }
        }
        return null;
    }

    private static boolean containsDeleteException(Appointment appointment, Date recurrenceDatePosition) {
        if (null != appointment.getDeleteException() && 0 < appointment.getDeleteException().length) {
            for (Date exception : appointment.getDeleteException()) {
                if (recurrenceDatePosition.equals(exception)) {
                    return true;
                }
            }
        }
        return false;
    }

}
