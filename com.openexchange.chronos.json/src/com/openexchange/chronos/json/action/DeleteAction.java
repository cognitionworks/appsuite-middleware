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

import static com.openexchange.chronos.service.CalendarParameters.PARAMETER_TIMESTAMP;
import static com.openexchange.tools.arrays.Collections.unmodifiableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.chronos.common.DefaultRecurrenceId;
import com.openexchange.chronos.json.converter.CalendarResultConverter;
import com.openexchange.chronos.json.exception.CalendarExceptionCodes;
import com.openexchange.chronos.json.exception.CalendarExceptionDetail;
import com.openexchange.chronos.provider.composition.CompositeEventID;
import com.openexchange.chronos.provider.composition.IDBasedCalendarAccess;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;

/**
 * {@link DeleteAction}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.0
 */
public class DeleteAction extends ChronosAction {

    private static final Set<String> REQUIRED_PARAMETERS = unmodifiableSet(PARAMETER_TIMESTAMP);

    private static final String ID_FIELD = "id";
    private static final String RECURENCE_ID_FIELD = "recurrenceId";

    /**
     * Initializes a new {@link DeleteAction}.
     *
     * @param services A service lookup reference
     */
    protected DeleteAction(ServiceLookup services) {
        super(services);
    }

    @Override
    protected Set<String> getRequiredParameters() {
        return REQUIRED_PARAMETERS;
    }

    @Override
    protected AJAXRequestResult perform(IDBasedCalendarAccess calendarAccess, AJAXRequestData requestData) throws OXException {

        Object data = requestData.getData();
        if (data == null || !(data instanceof JSONArray)) {
            throw AjaxExceptionCodes.ILLEGAL_REQUEST_BODY.create();
        }
        JSONArray ids = (JSONArray) data;
        try {
            List<CompositeEventID> compositeEventIDs = new ArrayList<>(ids.length());
            for (int x = 0; x < ids.length(); x++) {
                JSONObject jsonObject = ids.getJSONObject(x);
                String id = jsonObject.getString(ID_FIELD);
                if (jsonObject.has(RECURENCE_ID_FIELD)) {
                    String recurrenceId = jsonObject.getString(RECURENCE_ID_FIELD);
                    compositeEventIDs.add(new CompositeEventID(CompositeEventID.parse(id), new DefaultRecurrenceId(recurrenceId)));
                } else {
                    compositeEventIDs.add(CompositeEventID.parse(id));
                }
            }

            Map<CompositeEventID, OXException> errors = null;
            CalendarResult deleteEvent = null;
            for (CompositeEventID id : compositeEventIDs) {
                try {
                    deleteEvent = calendarAccess.deleteEvent(id);
                } catch (OXException e) {
                    if (errors == null) {
                        errors = new HashMap<>();
                    }
                    errors.put(id, e);
                }
            }

            if (errors != null && !errors.isEmpty()) {
                if (errors.size() == 1) {
                    CompositeEventID errorId = errors.keySet().iterator().next();
                    throw CalendarExceptionCodes.ERROR_DELETING_EVENT.create(errorId, errors.get(errorId).getDisplayMessage(requestData.getSession().getUser().getLocale()));
                } else {
                    OXException ex = CalendarExceptionCodes.ERROR_DELETING_EVENTS.create();
                    for (CompositeEventID id : errors.keySet()) {
                        ex.addDetail(new CalendarExceptionDetail(errors.get(id), id));
                    }
                    throw ex;
                }
            }
            return new AJAXRequestResult(deleteEvent, deleteEvent.getTimestamp(), CalendarResultConverter.INPUT_FORMAT);
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_READ_ERROR.create(e.getMessage(), e);
        }
    }

}
