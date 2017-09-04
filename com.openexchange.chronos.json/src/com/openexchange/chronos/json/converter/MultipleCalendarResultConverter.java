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

package com.openexchange.chronos.json.converter;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.Converter;
import com.openexchange.ajax.requesthandler.ResultConverter;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.json.converter.mapper.EventMapper;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.chronos.service.CreateResult;
import com.openexchange.chronos.service.DeleteResult;
import com.openexchange.chronos.service.EventID;
import com.openexchange.chronos.service.UpdateResult;
import com.openexchange.exception.OXException;
import com.openexchange.session.Session;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link MultipleCalendarResultConverter}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.0
 */
public class MultipleCalendarResultConverter implements ResultConverter {

    public static final String INPUT_FORMAT = "calendarResults";

    @Override
    public String getInputFormat() {
        return INPUT_FORMAT;
    }

    @Override
    public String getOutputFormat() {
        return "json";
    }

    @Override
    public Quality getQuality() {
        return Quality.GOOD;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void convert(AJAXRequestData requestData, AJAXRequestResult result, ServerSession session, Converter converter) throws OXException {
        /*
         * determine timezone
         */
        String timeZoneID = requestData.getParameter("timezone");
        if (null == timeZoneID) {
            timeZoneID = session.getUser().getTimeZone();
        }
        /*
         * check and convert result object
         */
        Object resultObject = result.getResultObject();
        if (resultObject instanceof List) {
            resultObject = convertCalendarResult((List<CalendarResult>) resultObject, timeZoneID, requestData.getSession());
        } else {
            throw new UnsupportedOperationException();
        }
        result.setResultObject(resultObject, "json");
    }

    private JSONObject convertCalendarResult(List<CalendarResult> calendarResults, String timeZoneID, Session session) throws OXException {
        JSONObject result = new JSONObject(1);
        try {
            List<Event> creates = new ArrayList<Event>();
            List<Event> updates = new ArrayList<Event>();
            List<Event> deletes = new ArrayList<Event>();

            for (CalendarResult calendarResult : calendarResults) {

                for (CreateResult createResult : calendarResult.getCreations()) {
                    creates.add(createResult.getCreatedEvent());
                }

                for (UpdateResult updatedResult : calendarResult.getUpdates()) {
                    boolean isCreated = false;
                    for (Event created : creates) {
                        if (created.getUid().equals(updatedResult.getUpdate())) {
                            creates.remove(created);
                            creates.add(updatedResult.getUpdate());
                            isCreated = true;
                            break;
                        }
                    }
                    if (!isCreated) {
                        for (Event update : updates) {
                            if (update.getUid().equals(updatedResult.getUpdate().getUid())) {
                                updates.remove(update);
                                break;
                            }
                        }
                        updates.add(updatedResult.getUpdate());
                    }
                }
                for (DeleteResult deleteResult : calendarResult.getDeletions()) {
                    EventID eventID = deleteResult.getEventID();
                    Event deletedEvent = new Event();
                    deletedEvent.setId(eventID.getObjectID());
                    deletedEvent.setFolderId(eventID.getFolderID());
                    if (null != eventID.getRecurrenceID()) {
                        deletedEvent.setRecurrenceId(eventID.getRecurrenceID());
                    }
                    deletedEvent.setTimestamp(deleteResult.getTimestamp());
                    deletes.add(deletedEvent);
                }
            }

            result.put("created", convertEvents(creates, timeZoneID, session));
            result.put("updated", convertEvents(updates, timeZoneID, session));
            result.put("deleted", convertEvents(deletes, timeZoneID, session));
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_WRITE_ERROR.create(e);
        }
        return result;
    }

    private JSONArray convertEvents(List<Event> results, String timeZoneID, Session session) throws OXException {
        JSONArray events = new JSONArray(results.size());
        for (Event event : results) {
            events.put(convertEvent(event, timeZoneID, session));
        }
        return events;
    }

    private JSONObject convertEvent(Event event, String timeZoneID, Session session) throws OXException {
        try {
            return EventMapper.getInstance().serialize(event, EventMapper.getInstance().getAssignedFields(event), timeZoneID, session);
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_WRITE_ERROR.create(e);
        }
    }

}
