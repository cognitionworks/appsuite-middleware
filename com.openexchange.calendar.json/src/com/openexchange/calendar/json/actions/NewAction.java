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

package com.openexchange.calendar.json.actions;

import static com.openexchange.tools.TimeZoneUtils.getTimeZone;
import java.util.Date;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.fields.DataFields;
import com.openexchange.ajax.parser.AppointmentParser;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.writer.AppointmentWriter;
import com.openexchange.api2.AppointmentSQLInterface;
import com.openexchange.calendar.json.AppointmentAJAXRequest;
import com.openexchange.documentation.RequestMethod;
import com.openexchange.documentation.annotations.Action;
import com.openexchange.documentation.annotations.Parameter;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link NewAction}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
@Action(method = RequestMethod.PUT, name = "new", description = "Create an appointment.", parameters = {
    @Parameter(name = "session", description = "A session ID previously obtained from the login module.")
}, requestBody = "Appointment object as described in Common object data, Detailed task and appointment data and Detailed appointment data. The field id is not present.",
responseDescription = "If the appointment was created successfully, an object with the attribute id of the newly created appointment. If the appointment could not be created due to conflicts, the response body is an object with the field conflicts, which is an array of appointment objects which caused the conflict. Each appointment object which represents a resource conflict contains an additional field hard_conflict with the Boolean value true. If the user does not have read access to a conflicting appointment, only the fields id, start_date, end_date, shown_as and participants are present and the field participants contains only the participants which caused the conflict.")
public final class NewAction extends AppointmentAction {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(NewAction.class);

    /**
     * Initializes a new {@link NewAction}.
     * @param services
     */
    public NewAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final AppointmentAJAXRequest req) throws OXException, JSONException {
        final JSONObject jData = req.getData();
        final TimeZone timeZone;
        {
            final String timeZoneId = req.getParameter(AJAXServlet.PARAMETER_TIMEZONE);
            timeZone = null == timeZoneId ? req.getTimeZone() : getTimeZone(timeZoneId);
        }
        final ServerSession session = req.getSession();

        final CalendarDataObject appointmentObj = new CalendarDataObject();
        appointmentObj.setContext(session.getContext());

        final AppointmentParser appointmentParser = new AppointmentParser(timeZone);
        appointmentParser.parse(appointmentObj, jData);

        if (!appointmentObj.containsParentFolderID()) {
            throw AjaxExceptionCodes.MISSING_PARAMETER.create( AJAXServlet.PARAMETER_FOLDERID);
        }

        convertExternalToInternalUsersIfPossible(appointmentObj, session.getContext(), LOG);

        final AppointmentSQLInterface appointmentSql = getService().createAppointmentSql(session);
        final Appointment[] conflicts = appointmentSql.insertAppointmentObject(appointmentObj);

        final JSONObject jsonResponseObj = new JSONObject();

        Date timestamp = null;
        if (conflicts == null) {
            jsonResponseObj.put(DataFields.ID, appointmentObj.getObjectID());
            timestamp = appointmentObj.getLastModified();
        } else {
            final JSONArray jsonConflictArray = new JSONArray(conflicts.length);
            final AppointmentWriter appointmentWriter = new AppointmentWriter(timeZone).setSession(req.getSession());
            for (final Appointment conflict : conflicts) {
                final JSONObject jsonAppointmentObj = new JSONObject();
                appointmentWriter.writeAppointment(conflict, jsonAppointmentObj);
                jsonConflictArray.put(jsonAppointmentObj);
            }

            jsonResponseObj.put("conflicts", jsonConflictArray);
        }

        return new AJAXRequestResult(jsonResponseObj, timestamp, "json");

    }

}
