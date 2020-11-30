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

import java.util.Date;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.fields.AppointmentFields;
import com.openexchange.ajax.fields.DataFields;
import com.openexchange.ajax.fields.FolderChildFields;
import com.openexchange.ajax.parser.DataParser;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.calendar.json.AppointmentAJAXRequest;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventID;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * {@link CopyAction}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
@RestrictedAction(module = AppointmentAction.MODULE, type = RestrictedAction.Type.WRITE)
public final class CopyAction extends AppointmentAction {

    /**
     * Initializes a new {@link CopyAction}.
     *
     * @param services A service lookup reference
     */
    public CopyAction(ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(CalendarSession session, AppointmentAJAXRequest request) throws OXException, JSONException {
        if (false == Boolean.parseBoolean(request.getParameter(AppointmentFields.IGNORE_CONFLICTS))) {
            session.set(CalendarParameters.PARAMETER_CHECK_CONFLICTS, Boolean.TRUE);
        }
        String folderId = request.checkParameter(AJAXServlet.PARAMETER_FOLDERID);
        String objectId = request.checkParameter(AJAXServlet.PARAMETER_ID);
        JSONObject jsonObject = request.getData();
        String targetFolderID = DataParser.checkString(jsonObject, FolderChildFields.FOLDER_ID);
        Event event = session.getCalendarService().getEvent(session, folderId, new EventID(folderId, objectId));
        event.removeId();
        event.removeUid();
        event.removeFolderId();
        CalendarResult result = session.getCalendarService().createEvent(session, targetFolderID, event);
        if (null != result.getCreations() && 0 < result.getCreations().size()) {
            String id = result.getCreations().get(0).getCreatedEvent().getId();
            return new AJAXRequestResult(new JSONObject().put(DataFields.ID, id), new Date(result.getTimestamp()), "json");
        }
        return null; //TODO: conflicts
    }

}
