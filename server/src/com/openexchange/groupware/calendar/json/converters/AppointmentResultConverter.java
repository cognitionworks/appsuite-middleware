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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.groupware.calendar.json.converters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.fields.CalendarFields;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.Converter;
import com.openexchange.ajax.writer.AppointmentWriter;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.calendar.CalendarCollectionService;
import com.openexchange.groupware.calendar.OXCalendarExceptionCodes;
import com.openexchange.groupware.calendar.RecurringResultInterface;
import com.openexchange.groupware.calendar.RecurringResultsInterface;
import com.openexchange.groupware.calendar.json.AppointmentAJAXRequest;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.container.CommonObject;
import com.openexchange.groupware.container.DataObject;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link AppointmentListResultConverter}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class AppointmentResultConverter extends AbstractCalendarJSONResultConverter {
	private static final org.apache.commons.logging.Log LOG =
		        com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(AppointmentResultConverter.class));

	private static final String INPUT_FORMAT = "appointment";
    /**
     * Initializes a new {@link AppointmentListResultConverter}.
     */
    public AppointmentResultConverter() {
        super();
    }

    protected void convertCalendar(final String action, final Collection<Appointment> appointmentList, final AppointmentAJAXRequest req, final AJAXRequestResult result, final TimeZone userTimeZone) throws OXException {
        if (action.equalsIgnoreCase(AJAXServlet.ACTION_UPDATES)) {
            convert4Updates(appointmentList, req, result, userTimeZone);
        } else if (action.equalsIgnoreCase(AJAXServlet.ACTION_FREEBUSY)) {
            convert4FreeBusy(appointmentList, req, result, userTimeZone);
        } else {
            convert(appointmentList, req, result, userTimeZone);
        }
    }

    protected void convert(final Collection<Appointment> appointmentList, final AppointmentAJAXRequest req, final AJAXRequestResult result, final TimeZone userTimeZone) throws OXException {
        final Date startUTC = req.optDate(AJAXServlet.PARAMETER_START);
        final Date endUTC = req.optDate(AJAXServlet.PARAMETER_END);
        final int[] columns = req.checkIntArray(AJAXServlet.PARAMETER_COLUMNS);

        final TimeZone timeZone;
        {
            final String timeZoneId = req.getParameter(AJAXServlet.PARAMETER_TIMEZONE);
            timeZone = null == timeZoneId ? userTimeZone : getTimeZone(timeZoneId);
        }
        final JSONArray jsonResponseArray = new JSONArray();
        final AppointmentWriter writer = new AppointmentWriter(timeZone);

        for (final Appointment appointment : appointmentList) {
            try {
                writer.writeArray(appointment, columns, startUTC, endUTC, jsonResponseArray);
            } catch (final JSONException e) {
                throw OXJSONExceptionCodes.JSON_WRITE_ERROR.create(e);
            }
        }

        result.setResultObject(jsonResponseArray, OUTPUT_FORMAT);
    }

    protected void convert4FreeBusy(final Collection<Appointment> appointmentList, final AppointmentAJAXRequest req, final AJAXRequestResult result, final TimeZone userTimeZone) throws OXException {
        final TimeZone timeZone;
        {
            final String timeZoneId = req.getParameter(AJAXServlet.PARAMETER_TIMEZONE);
            timeZone = null == timeZoneId ? userTimeZone : getTimeZone(timeZoneId);
        }

        final JSONArray jsonResponseArray = new JSONArray();
        final AppointmentWriter appointmentWriter = new AppointmentWriter(timeZone);
        for (final Appointment appointment : appointmentList) {
            final JSONObject jsonAppointmentObj = new JSONObject();
            try {
                appointmentWriter.writeAppointment(appointment, jsonAppointmentObj);
                jsonResponseArray.put(jsonAppointmentObj);
            } catch (final JSONException e) {
                throw OXJSONExceptionCodes.JSON_WRITE_ERROR.create(e);
            }
        }

        result.setResultObject(jsonResponseArray, OUTPUT_FORMAT);
    }

    protected void convert4Updates(final Collection<Appointment> appointments, final AppointmentAJAXRequest req, final AJAXRequestResult result, final TimeZone userTimeZone) throws OXException {
        final Date startUTC = req.optDate(AJAXServlet.PARAMETER_START);
        final Date endUTC = req.optDate(AJAXServlet.PARAMETER_END);
        final int[] columns = req.checkIntArray(AJAXServlet.PARAMETER_COLUMNS);
        /*
         * Create list with support for Iterator.remove()
         */
        final List<Appointment> appointmentList = new ArrayList<Appointment>(appointments);
        /*
         * Any deleted appointments?
         */
        final List<Appointment> deletedAppointments = new LinkedList<Appointment>();
        for (final Iterator<Appointment> iter = appointmentList.iterator(); iter.hasNext();) {
            final Appointment appointment = iter.next();
            if (hasOnlyId(appointment, columns)) {
                deletedAppointments.add(appointment);
                iter.remove();
            }
        }

        final TimeZone timeZone;
        {
            final String timeZoneId = req.getParameter(AJAXServlet.PARAMETER_TIMEZONE);
            timeZone = null == timeZoneId ? userTimeZone : getTimeZone(timeZoneId);
        }
        final JSONArray jsonResponseArray = new JSONArray();
        final AppointmentWriter writer = new AppointmentWriter(timeZone);

        for (final Appointment appointment : appointmentList) {
            try {
                writer.writeArray(appointment, columns, startUTC, endUTC, jsonResponseArray);
            } catch (final JSONException e) {
                throw OXJSONExceptionCodes.JSON_WRITE_ERROR.create(e);
            }
        }

        if (!deletedAppointments.isEmpty()) {
            for (final Appointment appointment : deletedAppointments) {
                jsonResponseArray.put(appointment.getObjectID());
            }
        }

        result.setResultObject(jsonResponseArray, OUTPUT_FORMAT);
    }

    private static boolean hasOnlyId(final Appointment appointment, final int[] columns) {
        if (!appointment.containsObjectID()) {
            return false;
        }
        if (CommonObject.Marker.ID_ONLY.equals(appointment.getMarker())) {
            return true;
        }
        for (final int column : columns) {
            if (DataObject.OBJECT_ID != column && appointment.contains(column)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public String getInputFormat() {
        return INPUT_FORMAT;
    }

    @Override
    protected void convertCalendar(final AppointmentAJAXRequest request, final AJAXRequestResult result, final ServerSession session, final Converter converter, final TimeZone userTimeZone) throws OXException {
        final Object resultObject = result.getResultObject();
        final String action = request.getParameter(AJAXServlet.PARAMETER_ACTION);
        if (resultObject instanceof Appointment) {
            convertCalendar((Appointment) resultObject, request, result, session, userTimeZone);
        } else {
            @SuppressWarnings("unchecked") final Collection<Appointment> appointments = (Collection<Appointment>) resultObject;
            convertCalendar(action, appointments, request, result, userTimeZone);
        }
    }

    private void convertCalendar(final Appointment appointmentobject, final AppointmentAJAXRequest request, final AJAXRequestResult result, final ServerSession session, final TimeZone userTimeZone) throws OXException {
        final JSONObject jsonResponseObj = new JSONObject();
        final CalendarCollectionService recColl = ServerServiceRegistry.getInstance().getService(CalendarCollectionService.class);
        final int recurrencePosition = request.optInt(CalendarFields.RECURRENCE_POSITION);

        final TimeZone timeZone;
        {
            final String timeZoneId = request.getParameter(AJAXServlet.PARAMETER_TIMEZONE);
            timeZone = null == timeZoneId ? userTimeZone : getTimeZone(timeZoneId);
        }
        final AppointmentWriter appointmentwriter = new AppointmentWriter(timeZone);
        appointmentwriter.setSession(session);

        if (appointmentobject.getRecurrenceType() != CalendarObject.NONE && recurrencePosition > 0) {
            // Commented this because this is done in CalendarOperation.loadAppointment():207 that calls extractRecurringInformation()
            // appointmentobject.calculateRecurrence();
            final RecurringResultsInterface recuResults =
                recColl.calculateRecurring(appointmentobject, 0, 0, recurrencePosition, CalendarCollectionService.MAX_OCCURRENCESE, true);
            if (recuResults.size() == 0) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(new StringBuilder(32).append("No occurrence at position ").append(recurrencePosition));
                }
                throw OXCalendarExceptionCodes.UNKNOWN_RECURRENCE_POSITION.create(Integer.valueOf(recurrencePosition));
            }
            final RecurringResultInterface resultInterface = recuResults.getRecurringResult(0);
            appointmentobject.setStartDate(new Date(resultInterface.getStart()));
            appointmentobject.setEndDate(new Date(resultInterface.getEnd()));
            appointmentobject.setRecurrencePosition(resultInterface.getPosition());

            try {
                appointmentwriter.writeAppointment(appointmentobject, jsonResponseObj);
            } catch (final JSONException e) {
                throw OXJSONExceptionCodes.JSON_WRITE_ERROR.create(e);
            }
        } else {
            try {
                appointmentwriter.writeAppointment(appointmentobject, jsonResponseObj);
            } catch (final JSONException e) {
                throw OXJSONExceptionCodes.JSON_WRITE_ERROR.create(e);
            }
        }

        result.setResultObject(jsonResponseObj, OUTPUT_FORMAT);
    }

}
