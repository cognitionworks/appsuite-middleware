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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.groupware.calendar.json.actions;

import static com.openexchange.tools.TimeZoneUtils.getTimeZone;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.json.JSONException;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.container.DateOrderObject;
import com.openexchange.ajax.fields.OrderFields;
import com.openexchange.ajax.request.AppointmentRequest;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.api2.AppointmentSQLInterface;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.calendar.CalendarCollectionService;
import com.openexchange.groupware.calendar.OXCalendarExceptionCodes;
import com.openexchange.groupware.calendar.RecurringResultInterface;
import com.openexchange.groupware.calendar.RecurringResultsInterface;
import com.openexchange.groupware.calendar.json.AppointmentAJAXRequest;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.groupware.search.Order;
import com.openexchange.server.ServiceLookup;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link AllAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class AllAction extends AbstractAppointmentAction {

    private static final org.apache.commons.logging.Log LOG =
        com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(AllAction.class));

    /**
     * Initializes a new {@link AllAction}.
     * @param services
     */
    public AllAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final AppointmentAJAXRequest req) throws OXException, JSONException {
        Date timestamp = new Date(0);

        SearchIterator<Appointment> it = null;

        final int[] columns = req.checkIntArray(AJAXServlet.PARAMETER_COLUMNS);
        final Date startUTC = req.checkDate(AJAXServlet.PARAMETER_START);
        final Date endUTC = req.checkDate(AJAXServlet.PARAMETER_END);
        final Date start = req.applyTimeZone2Date(startUTC.getTime());
        final Date end = req.applyTimeZone2Date(endUTC.getTime());
        final int folderId = req.getFolderId();
        final int orderBy = req.optInt(AJAXServlet.PARAMETER_SORT);
        final boolean showPrivateAppointments = Boolean.parseBoolean(req.getParameter(AJAXServlet.PARAMETER_SHOW_PRIVATE_APPOINTMENTS));
        final boolean listOrder;
        if (orderBy == CalendarObject.START_DATE || orderBy == CalendarObject.END_DATE) {
            listOrder = true;
        } else {
            listOrder = false;
        }

        final List<DateOrderObject> objectList = new ArrayList<DateOrderObject>();

        final String orderDirString = req.getParameter(AJAXServlet.PARAMETER_ORDER);
        final Order orderDir = OrderFields.parse(orderDirString);

        final boolean bRecurrenceMaster = Boolean.parseBoolean(req.getParameter(AppointmentRequest.RECURRENCE_MASTER));

//        final int leftHandLimit = req.optInt(AJAXServlet.LEFT_HAND_LIMIT);
//        final int rightHandLimit = req.optInt(AJAXServlet.RIGHT_HAND_LIMIT);

        final TimeZone timeZone;
        {
            final String timeZoneId = req.getParameter(AJAXServlet.PARAMETER_TIMEZONE);
            timeZone = null == timeZoneId ? req.getTimeZone() : getTimeZone(timeZoneId);
        }

        boolean showAppointmentInAllFolders = false;

        if (folderId == 0) {
            showAppointmentInAllFolders = true;
        }

        final ServerSession session = req.getSession();
        try {
            final AppointmentSQLInterface appointmentsql = getService().createAppointmentSql(session);
            final CalendarCollectionService calColl = ServerServiceRegistry.getInstance().getService(CalendarCollectionService.class);
            final int userId = session.getUserId();
            if (showAppointmentInAllFolders) {
                it = appointmentsql.getAppointmentsBetween(userId, start, end, _appointmentFields, orderBy, orderDir);
            } else {
                final boolean old = appointmentsql.getIncludePrivateAppointments();
                appointmentsql.setIncludePrivateAppointments(showPrivateAppointments);
                it = appointmentsql.getAppointmentsBetweenInFolder(folderId, _appointmentFields, start, end, orderBy, orderDir);
                appointmentsql.setIncludePrivateAppointments(old);
            }

            Date lastModified = new Date(0);
            final List<Appointment> appointmentList = new ArrayList<Appointment>();
            while (it.hasNext()) {
                final Appointment appointment = it.next();
                boolean written = false;

                // Workaround to fill appointments with alarm times
                // TODO: Move me down into the right layer if there is time for some refactoring.
                if (com.openexchange.tools.arrays.Arrays.contains(columns, Appointment.ALARM)) {
                    if (!appointment.containsAlarm() && appointment.containsUserParticipants()) {
                        final OXFolderAccess ofa = new OXFolderAccess(session.getContext());

                        try {
                            final int folderType = ofa.getFolderType(appointment.getParentFolderID(), userId);
                            final int owner = ofa.getFolderOwner(appointment.getParentFolderID());

                            switch (folderType) {
                            case FolderObject.PRIVATE:
                                for (final UserParticipant u : appointment.getUsers()) {
                                    if (u.getIdentifier() == userId && u.getAlarmMinutes() > -1) {
                                        appointment.setAlarm(u.getAlarmMinutes());
                                        break;
                                    }
                                }
                                break;

                            case FolderObject.SHARED:
                                for (final UserParticipant u : appointment.getUsers()) {
                                    if (u.getIdentifier() == owner && u.getAlarmMinutes() > -1) {
                                        appointment.setAlarm(u.getAlarmMinutes());
                                        break;
                                    }
                                }
                                break;
                            }
                        } catch (final OXException e) {
                            LOG.error("An error occurred during filling an appointment with alarm information.", e);
                        }
                    }
                }
                // End of workaround

                if (appointment.getRecurrenceType() != CalendarObject.NONE && appointment.getRecurrencePosition() == 0) {
                    if (bRecurrenceMaster) {
                        RecurringResultsInterface recuResults = null;
                        try {
                            recuResults = calColl.calculateFirstRecurring(appointment);
                            written = true;
                        } catch (final OXException e) {
                            LOG.error("Can not calculate recurrence " + appointment.getObjectID() + ':' + session.getContextId(), e);
                        }
                        if (recuResults != null && recuResults.size() == 1) {
                            appointment.setStartDate(new Date(recuResults.getRecurringResult(0).getStart()));
                            appointment.setEndDate(new Date(recuResults.getRecurringResult(0).getEnd()));

                            appointmentList.add(appointment);
                        } else {
                            LOG.warn("cannot load first recurring appointment from appointment object: " + +appointment.getRecurrenceType() + " / " + appointment.getObjectID() + "\n\n\n");
                        }
                    } else {
                        // Commented this because this is done in CalendarOperation.next():726 that calls extractRecurringInformation()
                        // appointment.calculateRecurrence();
                        RecurringResultsInterface recuResults = null;
                        try {
                            recuResults = calColl.calculateRecurring(appointment, start.getTime(), end.getTime(), 0);
                            written = true;
                        } catch (final OXException e) {
                            LOG.error("Can not calculate recurrence " + appointment.getObjectID() + ':' + session.getContextId(), e);
                        }
                        if (recuResults != null) {
                            for (int a = 0; a < recuResults.size(); a++) {
                                final RecurringResultInterface result = recuResults.getRecurringResult(a);
                                appointment.setStartDate(new Date(result.getStart()));
                                appointment.setEndDate(new Date(result.getEnd()));
                                appointment.setRecurrencePosition(result.getPosition());

                                // add to order list
                                if (listOrder) {
                                    final DateOrderObject dateOrderObject = new DateOrderObject(getDateByFieldId(
                                        orderBy,
                                        appointment,
                                        timeZone), appointment.clone());
                                    objectList.add(dateOrderObject);
                                } else {
                                    checkAndAddAppointment(appointmentList, appointment, startUTC, endUTC, calColl);
                                }
                            }
                        }
                    }
                }

                if (!written) {
                    // add to order list
                    if (listOrder) {
                        final DateOrderObject dateOrderObject = new DateOrderObject(
                            getDateByFieldId(orderBy, appointment, timeZone),
                            appointment.clone());
                        objectList.add(dateOrderObject);
                    } else {
                        checkAndAddAppointment(appointmentList, appointment, startUTC, endUTC, calColl);
                    }
                }

                lastModified = appointment.getLastModified();

                if (timestamp.getTime() < lastModified.getTime()) {
                    timestamp = lastModified;
                }
            }

            if (listOrder && !objectList.isEmpty()) {
                final DateOrderObject[] dateOrderObjectArray = objectList.toArray(new DateOrderObject[objectList.size()]);
                Arrays.sort(dateOrderObjectArray);

                switch (orderDir) {
                case ASCENDING:
                case NO_ORDER:
                    for (int a = 0; a < dateOrderObjectArray.length; a++) {
                        final Appointment appointmentObj = (Appointment) dateOrderObjectArray[a].getObject();
                        checkAndAddAppointment(appointmentList, appointmentObj, startUTC, endUTC, calColl);
                    }
                    break;
                case DESCENDING:
                    for (int a = dateOrderObjectArray.length - 1; a >= 0; a--) {
                        final Appointment appointmentObj = (Appointment) dateOrderObjectArray[a].getObject();
                        checkAndAddAppointment(appointmentList, appointmentObj, startUTC, endUTC, calColl);
                    }
                }
            }

            return new AJAXRequestResult(appointmentList, timestamp, "appointment");
        } catch (final SQLException e) {
            throw OXCalendarExceptionCodes.CALENDAR_SQL_ERROR.create(e, new Object[0]);
        } finally {
            if (it != null) {
                it.close();
            }
        }
    }

}
