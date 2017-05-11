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

package com.openexchange.calendar.json.actions;

import static com.openexchange.tools.TimeZoneUtils.getTimeZone;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import org.json.JSONException;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.container.DateOrderObject;
import com.openexchange.ajax.fields.OrderFields;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.api2.AppointmentSQLInterface;
import com.openexchange.calendar.json.AppointmentAJAXRequest;
import com.openexchange.calendar.json.AppointmentActionFactory;
import com.openexchange.calendar.json.actions.chronos.IDBasedCalendarAction;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.provider.composition.CompositeFolderID;
import com.openexchange.chronos.provider.composition.IDBasedCalendarAccess;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.calendar.AppointmentSqlFactoryService;
import com.openexchange.groupware.calendar.CalendarCollectionService;
import com.openexchange.groupware.calendar.OXCalendarExceptionCodes;
import com.openexchange.groupware.calendar.RecurringResultInterface;
import com.openexchange.groupware.calendar.RecurringResultsInterface;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.groupware.search.Order;
import com.openexchange.oauth.provider.resourceserver.annotations.OAuthAction;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.collections.PropertizedList;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link AllAction}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
@OAuthAction(AppointmentActionFactory.OAUTH_READ_SCOPE)
public final class AllAction extends IDBasedCalendarAction {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AllAction.class);

    /**
     * Initializes a new {@link AllAction}.
     *
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
        int orderBy = req.optInt(AJAXServlet.PARAMETER_SORT);
        final boolean showPrivateAppointments = Boolean.parseBoolean(req.getParameter(AJAXServlet.PARAMETER_SHOW_PRIVATE_APPOINTMENTS));
        final boolean listOrder;

        if (orderBy == AppointmentAJAXRequest.NOT_FOUND) {
            orderBy = CalendarObject.START_DATE;
        }
        if (orderBy == CalendarObject.START_DATE || orderBy == CalendarObject.END_DATE) {
            listOrder = true;
        } else {
            listOrder = false;
        }

        final List<DateOrderObject> objectList = new ArrayList<DateOrderObject>();

        final String orderDirString = req.getParameter(AJAXServlet.PARAMETER_ORDER);
        final Order orderDir = OrderFields.parse(orderDirString);

        final boolean bRecurrenceMaster = Boolean.parseBoolean(req.getParameter(RECURRENCE_MASTER));

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
            final AppointmentSqlFactoryService sqlFactoryService = getService();
            if (null == sqlFactoryService) {
                throw ServiceExceptionCode.serviceUnavailable(AppointmentSqlFactoryService.class);
            }
            final AppointmentSQLInterface appointmentsql = sqlFactoryService.createAppointmentSql(session);
            final CalendarCollectionService calColl = getService(CalendarCollectionService.class);
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
            List<Appointment> appointmentList = new ArrayList<Appointment>();
            while (it.hasNext()) {
                Appointment appointment = it.next();
                boolean written = false;

                // Workaround to fill appointments with alarm times
                // TODO: Move me down into the right layer if there is time for some refactoring.
                if (com.openexchange.tools.arrays.Arrays.contains(columns, CalendarObject.ALARM)) {
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
                            LOG.error("Can not calculate recurrence {}:{}", appointment.getObjectID(), session.getContextId(), e);
                        }
                        if (recuResults != null && recuResults.size() == 1) {
                            appointment = appointment.clone();
                            appointment.setStartDate(new Date(recuResults.getRecurringResult(0).getStart()));
                            appointment.setEndDate(new Date(recuResults.getRecurringResult(0).getEnd()));

                            appointmentList.add(appointment);
                        } else {
                            LOG.warn("cannot load first recurring appointment from appointment object: {} / {}\n\n\n", appointment.getRecurrenceType(), appointment.getObjectID());
                        }
                    } else {
                        // Commented this because this is done in CalendarOperation.next():726 that calls extractRecurringInformation()
                        // appointment.calculateRecurrence();
                        RecurringResultsInterface recuResults = null;
                        try {
                            recuResults = calColl.calculateRecurring(appointment, start.getTime(), end.getTime(), 0);
                            written = true;
                        } catch (final OXException e) {
                            LOG.error("Can not calculate recurrence {}:{}", appointment.getObjectID(), session.getContextId(), e);
                        }
                        if (recuResults != null) {
                            for (int a = 0; a < recuResults.size(); a++) {
                                final RecurringResultInterface result = recuResults.getRecurringResult(a);
                                appointment = appointment.clone();
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
            it.close();
            it = null;

            if (listOrder && !objectList.isEmpty()) {
                Collections.sort(objectList);

                switch (orderDir) {
                    case ASCENDING:
                    case NO_ORDER:
                        for (final DateOrderObject dateOrderObject : objectList) {
                            checkAndAddAppointment(appointmentList, (Appointment) dateOrderObject.getObject(), startUTC, endUTC, calColl);
                        }
                        break;
                    case DESCENDING:
                        Collections.reverse(objectList);
                        for (final DateOrderObject dateOrderObject : objectList) {
                            checkAndAddAppointment(appointmentList, (Appointment) dateOrderObject.getObject(), startUTC, endUTC, calColl);
                        }
                }
            }

            final int leftHandLimit = req.optInt(AJAXServlet.LEFT_HAND_LIMIT);
            final int rightHandLimit = req.optInt(AJAXServlet.RIGHT_HAND_LIMIT);

            if (leftHandLimit >= 0 || rightHandLimit > 0) {
                final int size = appointmentList.size();
                final int fromIndex = leftHandLimit > 0 ? leftHandLimit : 0;
                final int toIndex = rightHandLimit > 0 ? (rightHandLimit > size ? size : rightHandLimit) : size;
                if ((fromIndex) > size) {
                    appointmentList = Collections.<Appointment> emptyList();
                } else if (fromIndex >= toIndex) {
                    appointmentList = Collections.<Appointment> emptyList();
                } else {
                    /*
                     * Check if end index is out of range
                     */
                    if (toIndex < size) {
                        appointmentList = appointmentList.subList(fromIndex, toIndex);
                    } else if (fromIndex > 0) {
                        appointmentList = appointmentList.subList(fromIndex, size);
                    }
                }
                appointmentList = new PropertizedList<Appointment>(appointmentList).setProperty("more", Integer.valueOf(size));
            }

            return new AJAXRequestResult(appointmentList, timestamp, "appointment");
        } catch (final SQLException e) {
            throw OXCalendarExceptionCodes.CALENDAR_SQL_ERROR.create(e, new Object[0]);
        } finally {
            SearchIterators.close(it);
        }
    }

    private static final Set<String> REQUIRED_PARAMETERS = com.openexchange.tools.arrays.Collections.unmodifiableSet(
        AJAXServlet.PARAMETER_COLUMNS, AJAXServlet.PARAMETER_START, AJAXServlet.PARAMETER_END
    );

    private static final Set<String> OPTIONAL_PARAMETERS = com.openexchange.tools.arrays.Collections.unmodifiableSet(
        AJAXServlet.PARAMETER_SHOW_PRIVATE_APPOINTMENTS, AJAXServlet.PARAMETER_RECURRENCE_MASTER,
        AJAXServlet.PARAMETER_TIMEZONE, AJAXServlet.PARAMETER_SORT, AJAXServlet.PARAMETER_ORDER
    );

    @Override
    protected Set<String> getRequiredParameters() {
        return REQUIRED_PARAMETERS;
    }

    @Override
    protected Set<String> getOptionalParameters() {
        return OPTIONAL_PARAMETERS;
    }

    @Override
    protected AJAXRequestResult perform(CalendarSession session, AppointmentAJAXRequest request) throws OXException, JSONException {
        if (false == session.contains(CalendarParameters.PARAMETER_RECURRENCE_MASTER)) {
            session.set(CalendarParameters.PARAMETER_RECURRENCE_MASTER, Boolean.FALSE);
        }
        List<Event> events;
        String folderId = request.getParameter(AJAXServlet.PARAMETER_FOLDERID);
        if (null == folderId || "0".equals(folderId)) {
            events = session.getCalendarService().getEventsOfUser(session);
        } else {
            events = session.getCalendarService().getEventsInFolder(session, folderId);
        }
        return getAppointmentResultWithTimestamp(getEventConverter(session), events);
    }

    @Override
    protected AJAXRequestResult perform(IDBasedCalendarAccess access, AppointmentAJAXRequest request) throws OXException, JSONException {
        if (false == access.contains(CalendarParameters.PARAMETER_RECURRENCE_MASTER)) {
            access.set(CalendarParameters.PARAMETER_RECURRENCE_MASTER, Boolean.FALSE);
        }
        List<Event> events;
        String folderId = request.getParameter(AJAXServlet.PARAMETER_FOLDERID);
        if (null == folderId || "0".equals(folderId)) {
            events = access.getEventsOfUser();
        } else {
            events = access.getEventsInFolder(CompositeFolderID.parse(folderId));
        }
        return getAppointmentResultWithTimestamp(getEventConverter(access), events);
    }

}
