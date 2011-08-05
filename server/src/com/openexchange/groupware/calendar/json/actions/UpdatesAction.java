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
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.request.AppointmentRequest;
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
import com.openexchange.groupware.search.Order;
import com.openexchange.server.ServiceLookup;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link UpdatesAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class UpdatesAction extends AbstractAppointmentAction {

    private static final org.apache.commons.logging.Log LOG =
        com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(UpdatesAction.class));

    /**
     * Initializes a new {@link UpdatesAction}.
     *
     * @param services
     */
    public UpdatesAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final AppointmentAJAXRequest req) throws OXException, JSONException {
        final int[] columns = req.checkIntArray(AJAXServlet.PARAMETER_COLUMNS);
        final Date requestedTimestamp = req.checkDate(AJAXServlet.PARAMETER_TIMESTAMP);
        Date timestamp = new Date(requestedTimestamp.getTime());
        final TimeZone timeZone;
        {
            final String timeZoneId = req.getParameter(AJAXServlet.PARAMETER_TIMEZONE);
            timeZone = null == timeZoneId ? req.getTimeZone() : getTimeZone(timeZoneId);
        }

        final Date startUTC = req.getDate(AJAXServlet.PARAMETER_START);
        final Date endUTC = req.getDate(AJAXServlet.PARAMETER_END);
        final Date start = startUTC == null ? null : req.applyTimeZone2Date(startUTC.getTime());
        final Date end = endUTC == null ? null : req.applyTimeZone2Date(endUTC.getTime());
        final String ignore = req.getParameter(AJAXServlet.PARAMETER_IGNORE);

        final boolean bRecurrenceMaster = Boolean.parseBoolean(req.getParameter(AppointmentRequest.RECURRENCE_MASTER));
        final boolean showPrivates = Boolean.parseBoolean(req.getParameter(AJAXServlet.PARAMETER_SHOW_PRIVATE_APPOINTMENTS));
        final int folderId = req.optInt(AJAXServlet.PARAMETER_FOLDERID);

        boolean showAppointmentInAllFolders = false;

        if (folderId == 0) {
            showAppointmentInAllFolders = true;
        }

        boolean bIgnoreDelete = false;
        boolean bIgnoreModified = false;

        if (ignore != null && ignore.indexOf("deleted") != -1) {
            bIgnoreDelete = true;
        }

        if (ignore != null && ignore.indexOf("changed") != -1) {
            bIgnoreModified = true;
        }

        final JSONArray jsonResponseArray = new JSONArray();

        if (bIgnoreModified && bIgnoreDelete) {
            // nothing requested

            return new AJAXRequestResult(jsonResponseArray, timestamp, "json");
        }

        final ServerSession session = req.getSession();
        final AppointmentWriter appointmentWriter = new AppointmentWriter(timeZone);
        final AppointmentSQLInterface appointmentsql = getService().createAppointmentSql(session);
        final CalendarCollectionService recColl = ServerServiceRegistry.getInstance().getService(CalendarCollectionService.class);
        SearchIterator<Appointment> it = null;
        Date lastModified = null;
        appointmentsql.setIncludePrivateAppointments(showPrivates);
        try {
            if (!bIgnoreModified) {
                if (showAppointmentInAllFolders) {
                    it =
                        appointmentsql.getModifiedAppointmentsBetween(
                            session.getUserId(),
                            start,
                            end,
                            _appointmentFields,
                            requestedTimestamp,
                            0,
                            Order.NO_ORDER);
                } else {
                    if (start == null || end == null) {
                        it = appointmentsql.getModifiedAppointmentsInFolder(folderId, _appointmentFields, requestedTimestamp);
                    } else {
                        it = appointmentsql.getModifiedAppointmentsInFolder(folderId, start, end, _appointmentFields, requestedTimestamp);
                    }
                }

                while (it.hasNext()) {
                    final Appointment appointmentObj = it.next();
                    boolean written = false;
                    if (appointmentObj.getRecurrenceType() != CalendarObject.NONE && appointmentObj.getRecurrencePosition() == 0) {
                        if (bRecurrenceMaster) {
                            RecurringResultsInterface recuResults = null;
                            try {
                                recuResults = recColl.calculateFirstRecurring(appointmentObj);
                                written = true;
                            } catch (final OXException e) {
                                LOG.error("Can not calculate recurrence " + appointmentObj.getObjectID() + ':' + session.getContextId(), e);
                            }
                            if (recuResults != null && recuResults.size() != 1) {
                                LOG.warn("cannot load first recurring appointment from appointment object: " + +appointmentObj.getRecurrenceType() + " / " + appointmentObj.getObjectID() + "\n\n\n");
                            } else if (recuResults != null) {
                                appointmentObj.setStartDate(new Date(recuResults.getRecurringResult(0).getStart()));
                                appointmentObj.setEndDate(new Date(recuResults.getRecurringResult(0).getEnd()));

                                appointmentWriter.writeArray(appointmentObj, columns, jsonResponseArray);
                            }
                        } else {
                            // Commented this because this is done in CalendarOperation.next():726 that calls extractRecurringInformation()
                            // appointmentObj.calculateRecurrence();

                            RecurringResultsInterface recuResults = null;
                            try {
                                if (start == null || end == null) {
                                    recuResults = recColl.calculateFirstRecurring(appointmentObj);
                                    written = true;
                                } else {
                                    recuResults = recColl.calculateRecurring(appointmentObj, start.getTime(), end.getTime(), 0);
                                    written = true;
                                }
                            } catch (final OXException e) {
                                LOG.error("Can not calculate recurrence " + appointmentObj.getObjectID() + ':' + session.getContextId(), e);
                            }

                            if (recuResults != null) {
                                for (int a = 0; a < recuResults.size(); a++) {
                                    final RecurringResultInterface result = recuResults.getRecurringResult(a);
                                    appointmentObj.setStartDate(new Date(result.getStart()));
                                    appointmentObj.setEndDate(new Date(result.getEnd()));
                                    appointmentObj.setRecurrencePosition(result.getPosition());

                                    if (startUTC == null || endUTC == null) {
                                        appointmentWriter.writeArray(appointmentObj, columns, jsonResponseArray);
                                    } else {
                                        appointmentWriter.writeArray(appointmentObj, columns, startUTC, endUTC, jsonResponseArray);
                                    }
                                }
                            }
                        }
                    }
                    if (!written) {
                        if (startUTC == null || endUTC == null) {
                            appointmentWriter.writeArray(appointmentObj, columns, jsonResponseArray);
                        } else {
                            appointmentWriter.writeArray(appointmentObj, columns, startUTC, endUTC, jsonResponseArray);
                        }
                    }

                    lastModified = appointmentObj.getLastModified();

                    if (timestamp.getTime() < lastModified.getTime()) {
                        timestamp = lastModified;
                    }
                }
            }

            if (!bIgnoreDelete) {
                it = appointmentsql.getDeletedAppointmentsInFolder(folderId, _appointmentFields, requestedTimestamp);
                while (it.hasNext()) {
                    final Appointment appointmentObj = it.next();

                    jsonResponseArray.put(appointmentObj.getObjectID());

                    lastModified = appointmentObj.getLastModified();

                    if (timestamp.getTime() < lastModified.getTime()) {
                        timestamp = lastModified;
                    }
                }
            }

            return new AJAXRequestResult(jsonResponseArray, timestamp, "json");
        } catch (final SQLException e) {
            throw OXCalendarExceptionCodes.CALENDAR_SQL_ERROR.create(e, new Object[0]);
        } finally {
            if (it != null) {
                it.close();
            }
        }
    }

}
