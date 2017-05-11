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

import java.sql.SQLException;
import java.util.Date;
import java.util.Set;
import org.json.JSONException;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.api2.AppointmentSQLInterface;
import com.openexchange.calendar.json.AppointmentAJAXRequest;
import com.openexchange.calendar.json.AppointmentActionFactory;
import com.openexchange.calendar.json.actions.chronos.IDBasedCalendarAction;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.provider.composition.CompositeEventID;
import com.openexchange.chronos.provider.composition.IDBasedCalendarAccess;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.calendar.AppointmentSqlFactoryService;
import com.openexchange.groupware.calendar.OXCalendarExceptionCodes;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.oauth.provider.resourceserver.annotations.OAuthAction;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link GetAction}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
@OAuthAction(AppointmentActionFactory.OAUTH_READ_SCOPE)
public final class GetAction extends IDBasedCalendarAction {

    /**
     * Initializes a new {@link GetAction}.
     *
     * @param services
     */
    public GetAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final AppointmentAJAXRequest req) throws OXException {
        Date timestamp = null;
        final int id = req.checkInt(AJAXServlet.PARAMETER_ID);
        final int inFolder = req.checkInt(AJAXServlet.PARAMETER_FOLDERID);

        final ServerSession session = req.getSession();
        final AppointmentSqlFactoryService sqlFactoryService = getService();
        if (null == sqlFactoryService) {
            throw ServiceExceptionCode.serviceUnavailable(AppointmentSqlFactoryService.class);
        }
        final AppointmentSQLInterface appointmentsql = sqlFactoryService.createAppointmentSql(session);

        try {
            final Appointment appointmentobject = appointmentsql.getObjectById(id, inFolder);
            if (shouldAnonymize(appointmentobject, session.getUserId())) {
                anonymize(appointmentobject);
            }

            timestamp = appointmentobject.getLastModified();

            return new AJAXRequestResult(appointmentobject, timestamp, "appointment");
        } catch (final SQLException e) {
            throw OXCalendarExceptionCodes.CALENDAR_SQL_ERROR.create(e, new Object[0]);
        }
    }

    private static final Set<String> OPTIONAL_PARAMETERS = com.openexchange.tools.arrays.Collections.unmodifiableSet(AJAXServlet.PARAMETER_TIMEZONE);

    @Override
    protected Set<String> getOptionalParameters() {
        return OPTIONAL_PARAMETERS;
    }

    @Override
    protected AJAXRequestResult perform(CalendarSession session, AppointmentAJAXRequest request) throws OXException, JSONException {
        String folderId = request.checkParameter(AJAXServlet.PARAMETER_FOLDERID);
        String objectId = request.checkParameter(AJAXServlet.PARAMETER_ID);
        session.set(RECURRENCE_MASTER, Boolean.TRUE);
        Event event = session.getCalendarService().getEvent(session, folderId, objectId);
        Appointment appointment = getEventConverter(session).getAppointment(event);
        return new AJAXRequestResult(appointment, event.getLastModified(), "appointment");
    }

    @Override
    protected AJAXRequestResult perform(IDBasedCalendarAccess access, AppointmentAJAXRequest request) throws OXException, JSONException {
        String folderId = request.checkParameter(AJAXServlet.PARAMETER_FOLDERID);
        String objectId = request.checkParameter(AJAXServlet.PARAMETER_ID);
        int recurrencePosition = request.optInt("recurrence_position");
        //TODO: recurrecneId

        if (0 < recurrencePosition) {
            System.out.println();
        }

        CompositeEventID eventID = CompositeEventID.parse(objectId);
        //        EventID eventID = getEventConverter().getEventID(access, folderId, objectId, recurrencePosition);

        Event event = access.getEvent(eventID);
        Appointment appointment = getEventConverter(access).getAppointment(event);
        return new AJAXRequestResult(appointment, event.getLastModified(), "appointment");
    }

}
