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

package com.openexchange.chronos.json.action;

import static com.openexchange.tools.arrays.Collections.unmodifiableSet;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.json.converter.CalendarResultConverter;
import com.openexchange.chronos.json.converter.mapper.AttendeesMapping;
import com.openexchange.chronos.provider.composition.IDBasedCalendarAccess;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 *
 * {@link UpdateAttendeeAction}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.0
 */
@RestrictedAction(module = ChronosAction.MODULE, type = RestrictedAction.Type.WRITE)
public class UpdateAttendeeAction extends ChronosAction {

    /**
     * Initializes a new {@link UpdateAttendeeAction}.
     *
     * @param services
     */
    protected UpdateAttendeeAction(ServiceLookup services) {
        super(services);
    }

    private static final Set<String> OPTIONAL_PARAMETERS = unmodifiableSet(PARAM_SCHEDULING, PARAM_CHECK_CONFLICTS, PARAM_RANGE_START, PARAM_RANGE_END, PARAM_EXPAND, PARAM_FIELDS, PARAM_PUSH_TOKEN);

    private static final String ATTENDEE_FIELD = "attendee";
    private static final String ALARMS_FIELD = "alarms";

    @Override
    protected Set<String> getOptionalParameters() {
        return OPTIONAL_PARAMETERS;
    }

    @Override
    protected AJAXRequestResult perform(IDBasedCalendarAccess calendarAccess, AJAXRequestData requestData) throws OXException {
        long clientTimestamp = parseClientTimestamp(requestData);
        JSONObject jsonObject = requestData.getData(JSONObject.class);
        if (null == jsonObject) {
            throw AjaxExceptionCodes.ILLEGAL_REQUEST_BODY.create();
        }
        /*
         * parse attendee & optional alarms
         */
        Attendee attendee;
        List<Alarm> alarms;
        try {
            attendee = AttendeesMapping.deserializeAttendee(jsonObject.getJSONObject(ATTENDEE_FIELD));
            if (false == jsonObject.has(ALARMS_FIELD)) {
                alarms = null;
            } else if (jsonObject.isNull(ALARMS_FIELD)) {
                alarms = Collections.emptyList();
            } else {
                alarms = parseAlarms(jsonObject.getJSONArray(ALARMS_FIELD), getTimeZone(requestData));
            }
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_READ_ERROR.create(e, e.getMessage());
        }
        if (!attendee.containsUri() && !attendee.containsEntity()) {
            ServerSession session = requestData.getSession();
            if (session != null) {
                attendee.setEntity(session.getUserId());
            }
        }
        /*
         * perform update & return result
         */
        try {
            CalendarResult calendarResult = calendarAccess.updateAttendee(parseIdParameter(requestData), attendee, alarms, clientTimestamp);
            return new AJAXRequestResult(calendarResult, new Date(calendarResult.getTimestamp()), CalendarResultConverter.INPUT_FORMAT);
        } catch (OXException e) {
            return handleConflictException(e);
        }
    }

}
