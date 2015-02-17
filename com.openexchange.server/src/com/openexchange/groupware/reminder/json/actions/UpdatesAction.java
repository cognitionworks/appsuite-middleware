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

package com.openexchange.groupware.reminder.json.actions;

import static com.openexchange.tools.TimeZoneUtils.getTimeZone;
import java.util.Date;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.writer.ReminderWriter;
import com.openexchange.api2.ReminderService;
import com.openexchange.documentation.RequestMethod;
import com.openexchange.documentation.annotations.Action;
import com.openexchange.documentation.annotations.Parameter;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.reminder.ReminderHandler;
import com.openexchange.groupware.reminder.ReminderObject;
import com.openexchange.groupware.reminder.json.ReminderAJAXRequest;
import com.openexchange.groupware.reminder.json.ReminderActionFactory;
import com.openexchange.oauth.provider.OAuthAction;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link UpdatesAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@Action(method = RequestMethod.GET, name = "updates", description = "Get updated reminders", parameters = {
    @Parameter(name = "session", description = "A session ID previously obtained from the login module.")
}, responseDescription = "")
@OAuthAction(ReminderActionFactory.OAUTH_READ_SCOPE)
public final class UpdatesAction extends AbstractReminderAction {

    /**
     * Initializes a new {@link UpdatesAction}.
     */
    public UpdatesAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final ReminderAJAXRequest req) throws OXException, JSONException {
        final Date timestamp = req.checkDate(AJAXServlet.PARAMETER_TIMESTAMP);
        final TimeZone timeZone;
        {
            final String timeZoneId = req.getParameter(AJAXServlet.PARAMETER_TIMEZONE);
            timeZone = null == timeZoneId ? req.getTimeZone() : getTimeZone(timeZoneId);
        }

        final JSONArray jsonResponseArray = new JSONArray();
        SearchIterator<?> it = null;
        try {
            final ServerSession session = req.getSession();
            final ReminderService reminderSql = new ReminderHandler(session.getContext());

            it = reminderSql.listModifiedReminder(session.getUserId(), timestamp);
            while (it.hasNext()) {
                final ReminderWriter reminderWriter = new ReminderWriter(timeZone);
                final ReminderObject reminder = (ReminderObject) it.next();

                if (reminder.isRecurrenceAppointment()) {
                    // final int targetId = reminder.getTargetId();
                    // final int inFolder = reminder.getFolder();

                    // currently disabled because not used by the UI
                    // final ReminderObject latestReminder = getLatestReminder(targetId, inFolder, sessionObj, end);
                    //
                    // if (latestReminder == null) {
                    // continue;
                    // } else {
                    // reminderObj.setDate(latestReminder.getDate());
                    // reminderObj.setRecurrencePosition(latestReminder.getRecurrencePosition());
                    // }
                }

                if (hasModulePermission(reminder, session) && stillAccepted(reminder, session)) {
                    final JSONObject jsonReminderObj = new JSONObject();
                    reminderWriter.writeObject(reminder, jsonReminderObj);
                    jsonResponseArray.put(jsonReminderObj);
                }
            }

            return new AJAXRequestResult(jsonResponseArray, timestamp, "json");
        } finally {
            if (null != it) {
                it.close();
            }
        }
    }

}
