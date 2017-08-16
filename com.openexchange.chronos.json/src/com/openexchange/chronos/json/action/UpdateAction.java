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

import static com.openexchange.chronos.service.CalendarParameters.PARAMETER_IGNORE_CONFLICTS;
import static com.openexchange.tools.arrays.Collections.unmodifiableSet;
import java.util.Date;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.json.converter.CalendarResultConverter;
import com.openexchange.chronos.json.converter.EventConflictResultConverter;
import com.openexchange.chronos.json.converter.EventMapper;
import com.openexchange.chronos.json.oauth.ChronosOAuthScope;
import com.openexchange.chronos.provider.composition.IDBasedCalendarAccess;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.exception.OXException;
import com.openexchange.oauth.provider.resourceserver.annotations.OAuthAction;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;

/**
 * {@link UpdateAction}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.0
 */
@OAuthAction(ChronosOAuthScope.OAUTH_WRITE_SCOPE)
public class UpdateAction extends ChronosAction {

    private static final Set<String> OPTIONAL_PARAMETERS = unmodifiableSet("sendInternalNotifications", PARAMETER_IGNORE_CONFLICTS);

    /**
     * Initializes a new {@link UpdateAction}.
     *
     * @param services A service lookup reference
     */
    protected UpdateAction(ServiceLookup services) {
        super(services);
    }

    @Override
    protected Set<String> getOptionalParameters() {
        return OPTIONAL_PARAMETERS;
    }

    @Override
    protected AJAXRequestResult perform(IDBasedCalendarAccess calendarAccess, AJAXRequestData requestData) throws OXException {
        long clientTimestamp = parseClientTimestamp(requestData);
        Object data = requestData.getData();
        if (data == null || !(data instanceof JSONObject)) {
            throw AjaxExceptionCodes.ILLEGAL_REQUEST_BODY.create();
        }
        JSONObject jsonEvent = (JSONObject) data;

        Event event;
        try {
            event = EventMapper.getInstance().deserialize(jsonEvent, EventMapper.getInstance().getMappedFields());
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_READ_ERROR.create(e.getMessage(), e);
        }
        try {
            CalendarResult calendarResult = calendarAccess.updateEvent(parseIdParameter(requestData), event, clientTimestamp);
            return new AJAXRequestResult(calendarResult, new Date(calendarResult.getTimestamp()), CalendarResultConverter.INPUT_FORMAT);
        } catch (OXException e) {
            if (isConflict(e)) {
                return new AJAXRequestResult(e.getProblematics(), EventConflictResultConverter.INPUT_FORMAT);
            }
            throw e;
        }
    }

}
