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

package com.openexchange.groupware.reminder.json.actions;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.tools.TimeZoneUtils.getTimeZone;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.ajax.writer.ReminderWriter;
import com.openexchange.chronos.AlarmTrigger;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarService;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.reminder.ReminderObject;
import com.openexchange.groupware.reminder.ReminderService;
import com.openexchange.groupware.reminder.json.ReminderAJAXRequest;
import com.openexchange.server.ServiceLookup;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.tools.oxfolder.OXFolderExceptionCode;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;


/**
 * {@link RangeAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@RestrictedAction(module = AbstractReminderAction.MODULE, type = RestrictedAction.Type.READ)
public final class RangeAction extends AbstractReminderAction {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(RangeAction.class);

    /**
     * Initializes a new {@link RangeAction}.
     * @param services
     */
    public RangeAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final ReminderAJAXRequest req) throws OXException, JSONException {
        final Date end = req.checkDate(AJAXServlet.PARAMETER_END);
        final TimeZone tz = req.getTimeZone();
        final TimeZone timeZone;
        {
            final String timeZoneId = req.getParameter(AJAXServlet.PARAMETER_TIMEZONE);
            timeZone = null == timeZoneId ? tz : getTimeZone(timeZoneId);
        }


        final ReminderWriter reminderWriter = new ReminderWriter(timeZone);
        try {
            final ServerSession session = req.getSession();
            final ReminderService reminderService = ServerServiceRegistry.getInstance().getService(ReminderService.class, true);
            final User user = session.getUser();
            final List<ReminderObject> reminders = reminderService.getArisingReminder(session, session.getContext(), user, end);
            final JSONArray jsonResponseArray = new JSONArray();
            for (ReminderObject reminder : reminders) {
                try {
                    if (isRequested(req, reminder) && hasModulePermission(reminder, session) && stillAccepted(reminder, session)) {
                        final JSONObject jsonReminderObj = new JSONObject(12);
                        reminderWriter.writeObject(reminder, jsonReminderObj);
                        jsonResponseArray.put(jsonReminderObj);
                    }
                } catch (OXException e) {
                    if (!OXFolderExceptionCode.NOT_EXISTS.equals(e)) {
                        throw e;
                    }
                    LOG.warn("Cannot load target object of this reminder.", e);
                    deleteReminderSafe(req.getSession(), reminder, user.getId(), reminderService);
                }
            }

            if (req.getOptModules() == null || req.getOptModules().isEmpty() || req.getOptModules().contains(I(Types.APPOINTMENT))) {
                addEventReminder(session, jsonResponseArray, reminderWriter, end);
            }

            return new AJAXRequestResult(jsonResponseArray, "json");
        } catch (OXException e) {
            throw e;
        }
    }

    /**
     * Adds event reminder to the response array
     *
     * @param session The user session
     * @param jsonResponseArray The response array
     * @param reminderWriter The {@link ReminderWriter} to use
     * @param until The upper limit of query
     * @throws JSONException
     * @throws OXException
     */
    private void addEventReminder(Session session, JSONArray jsonResponseArray, ReminderWriter reminderWriter, Date until) throws JSONException, OXException {
        CalendarService calService = ServerServiceRegistry.getInstance().getService(CalendarService.class);
        CalendarSession calSession = calService.init(session);
        calSession.set(CalendarParameters.PARAMETER_RANGE_END, until);

        List<AlarmTrigger> alarmTrigger = calService.getAlarmTriggers(calSession, Collections.singleton("DISPLAY"));
        for (AlarmTrigger trigger : alarmTrigger) {
            convertAlarmTrigger2Reminder(calSession, trigger, reminderWriter, jsonResponseArray);
        }
    }

}
