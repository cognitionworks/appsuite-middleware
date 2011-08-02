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
import java.util.Date;
import java.util.LinkedList;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.fields.OrderFields;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.writer.AppointmentWriter;
import com.openexchange.api2.AppointmentSQLInterface;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.calendar.CalendarCollectionService;
import com.openexchange.groupware.calendar.OXCalendarExceptionCodes;
import com.openexchange.groupware.calendar.RecurringResultInterface;
import com.openexchange.groupware.calendar.RecurringResultsInterface;
import com.openexchange.groupware.calendar.json.AppointmentAJAXRequest;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.search.AppointmentSearchObject;
import com.openexchange.groupware.search.Order;
import com.openexchange.server.ServiceLookup;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.iterator.SearchIterator;


/**
 * {@link NewAppointmentsSearchAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class NewAppointmentsSearchAction extends AbstractAppointmentAction {

    private static final org.apache.commons.logging.Log LOG =
        com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(NewAppointmentsSearchAction.class));

    /**
     * Initializes a new {@link NewAppointmentsSearchAction}.
     * @param services
     */
    public NewAppointmentsSearchAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final AppointmentAJAXRequest req) throws OXException, JSONException {
        final TimeZone timeZone;
        {
            final String timeZoneId = req.getParameter(AJAXServlet.PARAMETER_TIMEZONE);
            timeZone = null == timeZoneId ? req.getTimeZone() : getTimeZone(timeZoneId);
        }
        final int[] columns = req.checkIntArray(AJAXServlet.PARAMETER_COLUMNS);
        final Date start = req.checkTime(AJAXServlet.PARAMETER_START, timeZone);
        final Date end = req.checkTime(AJAXServlet.PARAMETER_END, timeZone);

        final Date startUTC = req.checkDate(AJAXServlet.PARAMETER_START);
        final Date endUTC = req.checkDate(AJAXServlet.PARAMETER_END);

        int orderBy = req.optInt(AJAXServlet.PARAMETER_SORT);

        if (orderBy == 0) {
            orderBy = CalendarObject.START_DATE;
        }

        String orderDirString = req.getParameter(AJAXServlet.PARAMETER_ORDER);
        if (orderDirString == null) {
            orderDirString = "asc";
        }
        final Order orderDir = OrderFields.parse(orderDirString);

        final int limit = req.checkInt("limit");

        Date timestamp = new Date(0);

        final Date lastModified = null;

        final AppointmentSearchObject searchObj = new AppointmentSearchObject();
        searchObj.setRange(new Date[] { start, end });

        final LinkedList<Appointment> appointmentList = new LinkedList<Appointment>();

        final JSONArray jsonResponseArray = new JSONArray();

        SearchIterator<Appointment> searchIterator = null;
        try {
            final AppointmentSQLInterface appointmentsql = getService().createAppointmentSql(req.getSession());
            final CalendarCollectionService recColl = ServerServiceRegistry.getInstance().getService(CalendarCollectionService.class);
            searchIterator = appointmentsql.getAppointmentsByExtendedSearch(searchObj, orderBy, orderDir, _appointmentFields);

            final AppointmentWriter appointmentwriter = new AppointmentWriter(timeZone);

            while (searchIterator.hasNext()) {
                final Appointment appointmentobject = searchIterator.next();
                boolean processed = false;
                if (appointmentobject.getRecurrenceType() != CalendarObject.NONE && appointmentobject.getRecurrencePosition() == 0) {
                    // Commented this because this is done in CalendarOperation.next():726 that calls extractRecurringInformation()
                    // appointmentobject.calculateRecurrence();
                    RecurringResultsInterface recuResults = null;
                    try {
                        recuResults = recColl.calculateRecurring(appointmentobject, start.getTime(), end.getTime(), 0);
                        processed = true;
                    } catch (final OXException x) {
                        LOG.error("Can not calculate recurrence " + appointmentobject.getObjectID() + ":" + req.getSession().getContextId(), x);
                    }
                    if (recuResults != null && recuResults.size() > 0) {
                        final RecurringResultInterface result = recuResults.getRecurringResult(0);
                        appointmentobject.setStartDate(new Date(result.getStart()));
                        appointmentobject.setEndDate(new Date(result.getEnd()));
                        appointmentobject.setRecurrencePosition(result.getPosition());

                        if (appointmentobject.getFullTime()) {
                            if (recColl.inBetween(
                                appointmentobject.getStartDate().getTime(),
                                appointmentobject.getEndDate().getTime(),
                                startUTC.getTime(),
                                endUTC.getTime())) {
                                compareStartDateForList(appointmentList, appointmentobject, limit);
                            }
                        } else {
                            compareStartDateForList(appointmentList, appointmentobject, limit);
                        }
                    }
                }
                if (!processed) {
                    if (appointmentobject.getFullTime() && (startUTC != null && endUTC != null)) {
                        if (recColl.inBetween(
                            appointmentobject.getStartDate().getTime(),
                            appointmentobject.getEndDate().getTime(),
                            startUTC.getTime(),
                            endUTC.getTime())) {
                            compareStartDateForList(appointmentList, appointmentobject, limit);
                        }
                    } else {
                        compareStartDateForList(appointmentList, appointmentobject, limit);
                    }
                }

                if (timestamp.before(appointmentobject.getLastModified())) {
                    timestamp = appointmentobject.getLastModified();
                }
            }

            for (int a = 0; a < appointmentList.size(); a++) {
                final Appointment appointmentObj = appointmentList.get(a);
                if (appointmentObj.getFullTime()) {
                    appointmentwriter.writeArray(appointmentObj, columns, startUTC, endUTC, jsonResponseArray);
                } else {
                    appointmentwriter.writeArray(appointmentObj, columns, jsonResponseArray);
                }
            }

            return new AJAXRequestResult(jsonResponseArray, timestamp, "json");
        } catch (final SQLException e) {
            throw OXCalendarExceptionCodes.CALENDAR_SQL_ERROR.create(e, new Object[0]);
        } finally {
            if (searchIterator != null) {
                searchIterator.close();
            }
        }
    }

}
