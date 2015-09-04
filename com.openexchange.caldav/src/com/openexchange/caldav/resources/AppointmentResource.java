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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.api2.AppointmentSQLInterface;
import com.openexchange.caldav.GroupwareCaldavFactory;
import com.openexchange.caldav.ParticipantTools;
import com.openexchange.caldav.Patches;
import com.openexchange.caldav.Tools;
import com.openexchange.calendar.AppointmentDiff;
import com.openexchange.data.conversion.ical.ConversionError;
import com.openexchange.data.conversion.ical.ConversionWarning;
import com.openexchange.data.conversion.ical.ConversionWarning.Code;
import com.openexchange.data.conversion.ical.ICalEmitter;
import com.openexchange.data.conversion.ical.ICalSession;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXException.IncorrectString;
import com.openexchange.exception.OXException.Truncated;
import com.openexchange.folderstorage.Type;
import com.openexchange.folderstorage.type.PrivateType;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.folderstorage.type.SharedType;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.UserParticipant;
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
            Appointment originalAppointment = parent.load(this.object, false);
            Date clientLastModified = this.object.getLastModified();
            if (clientLastModified.before(originalAppointment.getLastModified())) {
                throw WebdavProtocolException.Code.EDIT_CONFLICT.create(getUrl(), HttpServletResponse.SC_CONFLICT);
            }
            /*
             * update appointment
             */
            if (false == Patches.Incoming.tryRestoreParticipants(originalAppointment, appointmentToSave)) {
                Patches.Incoming.patchResources(originalAppointment, appointmentToSave);
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
                getAppointmentInterface().updateAppointmentObject(appointmentToSave, parentFolderID, clientLastModified, checkPermissions);
                if (null != appointmentToSave.getLastModified()) {
                    clientLastModified = appointmentToSave.getLastModified();
                }
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
                Appointment originalException = getMatchingException(originalExceptions, exceptionToSave.getRecurrenceDatePosition());
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
                 * update exception
                 */
                // TODO fix for bug 32536 - closing reminder for appointment results in 'containing changes'
                if (null != originalException && false == containsChanges(originalException, exceptionToSave)) {
                    LOG.debug("No changes detected in {}, skipping update.", exceptionToSave);
                } else {
                    try {
                        getAppointmentInterface().updateAppointmentObject(exceptionToSave, parentFolderID, clientLastModified, checkPermissions);
                    } catch (OXException e) {
                        if ("OX-0001".equals(e.getErrorCode())) {
                            StringBuilder stringBuilder = new StringBuilder("Exception when saving exception: ").append(e.getMessage()).append("\r\n")
                                .append("appointmentToSave: ").append(appointmentToSave).append("\r\n")
                                .append("originalAppointment: ").append(originalAppointment).append("\r\n")
                                .append("exceptionToSave: ").append(exceptionToSave).append("\r\n")
                                .append("originalException: ").append(originalException).append("\r\n")
                                .append("originalExceptions: ").append(Arrays.toString(originalExceptions)).append("\r\n")
                                .append("parsedICal:\r\n").append(parsedICal).append("\r\n")
                            ;
                            LOG.warn(stringBuilder.toString());
                        }
                        throw e;
                    }
                    if (null != exceptionToSave.getLastModified()) {
                        clientLastModified = exceptionToSave.getLastModified();
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
            getAppointmentInterface().insertAppointmentObject(this.appointmentToSave);
            Date clientLastModified = appointmentToSave.getLastModified();
            /*
             * create change exceptions
             */
            for (final CalendarDataObject exception : exceptionsToSave) {
                exception.removeObjectID(); // in case it's already assigned due to retry operations
                exception.setObjectID(appointmentToSave.getObjectID());
                Patches.Incoming.removeParticipantsForPrivateAppointmentInPublicfolder(
                    factory.getSession().getUserId(), parent.getFolder(), exception);
                getAppointmentInterface().updateAppointmentObject(exception, parentFolderID, clientLastModified);
                clientLastModified = exception.getLastModified();
            }
            /*
             * create delete exceptions
             */
            for (final CalendarDataObject exception : deleteExceptionsToSave) {
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

    @Override
    protected String generateICal() throws OXException {
        final ICalEmitter icalEmitter = factory.getIcalEmitter();
        final ICalSession session = icalEmitter.createSession();
        final List<ConversionError> conversionErrors = new LinkedList<ConversionError>();
        final List<ConversionWarning> conversionWarnings = new LinkedList<ConversionWarning>();
        try {
            /*
             * load appointment and change exceptions
             */
            CalendarDataObject appointment = parent.load(object, true);
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
                for (Appointment changeException : changeExceptions) {
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
        } catch (final UnsupportedEncodingException e) {
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
            throw new ConversionError(0, Code.INSUFFICIENT_INFORMATION);
        }
    }

    String parsedICal;

    private List<CalendarDataObject> parse(InputStream body) throws IOException, ConversionError {
        try {
            if (LOG.isTraceEnabled() || 1 == 1) {
                byte[] iCal = Streams.stream2bytes(body);
                parsedICal = new String(iCal, Charsets.UTF_8);
                LOG.trace(parsedICal);
                body = Streams.newByteArrayInputStream(iCal);
            }
            return factory.getIcalParser().parseAppointments(
                body, getTimeZone(), factory.getContext(), new ArrayList<ConversionError>(), new ArrayList<ConversionWarning>());
        } finally {
            Streams.close(body);
        }
    }

    private static boolean differs(Object value1, Object value2) {
        if (value1 == value2) {
            return false;
        } else if (null == value1 && null != value2) {
            return true;
        } else if (null == value2) {
            return true;
        } else if (String.class.isInstance(value1) && String.class.isInstance(value2)) {
            return 0 != ((String)value1).trim().compareTo(((String)value2).trim());
        } else if (Participant[].class.isInstance(value1)) {
            return false == ParticipantTools.equals((Participant[])value1, (Participant[])value2, true);
        } else if (Comparable.class.isInstance(value1)) {
            return 0 != ((Comparable)value1).compareTo(value2);
        } else {
            return false;
        }
    }

    private static boolean differs(int field, Appointment oldAppointment, CalendarDataObject cdo) {
        return oldAppointment.contains(field) && false == cdo.contains(field) ||
            false == oldAppointment.contains(field) && cdo.contains(field) ||
            differs(oldAppointment.get(field), cdo.get(field));
    }

    private static boolean containsChanges(Appointment oldAppointment, CalendarDataObject cdo) {
        boolean useDiff = true;
        if (useDiff) {
            AppointmentDiff diff = AppointmentDiff.compare(oldAppointment, cdo);
            return diff.anyFieldChangedOf(CALDAV_FIELDS) ||
                CalendarObject.NO_RECURRENCE != oldAppointment.getRecurrenceType() &&
                CalendarObject.NO_RECURRENCE != cdo.getRecurrenceType() &&
                diff.anyFieldChangedOf(RECURRENCE_FIELDS);
        }
        try {
            /*
             * check appointment fields
             */
            for (int field : CALDAV_FIELDS) {
                if (differs(field, oldAppointment, cdo)) {
                    return true;
                }
            }
            if (CalendarObject.NO_RECURRENCE != oldAppointment.getRecurrenceType() &&
                CalendarObject.NO_RECURRENCE != cdo.getRecurrenceType()) {
                for (int field : RECURRENCE_FIELDS) {
                    if (differs(field, oldAppointment, cdo)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) { // not enough trust in generic comparisons
            LOG.error("Error checking for appointment changes", e);
            return true;
        }
        return false;
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
