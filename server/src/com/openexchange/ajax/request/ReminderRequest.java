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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.ajax.request;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.fields.CalendarFields;
import com.openexchange.ajax.parser.DataParser;
import com.openexchange.ajax.writer.ReminderWriter;
import com.openexchange.api.OXMandatoryFieldException;
import com.openexchange.api.OXObjectNotFoundException;
import com.openexchange.api2.OXException;
import com.openexchange.api2.ReminderSQLInterface;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.EnumComponent;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.groupware.calendar.CalendarRecurringCollection;
import com.openexchange.groupware.calendar.CalendarSql;
import com.openexchange.groupware.calendar.RecurringResult;
import com.openexchange.groupware.calendar.RecurringResults;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.reminder.EmptyReminderDeleteImpl;
import com.openexchange.groupware.reminder.ReminderDeleteInterface;
import com.openexchange.groupware.reminder.ReminderHandler;
import com.openexchange.groupware.reminder.ReminderObject;
import com.openexchange.groupware.tasks.ModifyThroughDependant;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.session.Session;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIteratorException;
import com.openexchange.tools.servlet.AjaxException;
import com.openexchange.tools.servlet.OXJSONException;

public class ReminderRequest {
    
    private final Session sessionObj;

    private final Context ctx;
    
    private final User userObj;
    
    private Date timestamp;
    
    private static final Log LOG = LogFactory.getLog(ReminderRequest.class);
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public ReminderRequest(final Session sessionObj, final Context ctx) {
        this.sessionObj = sessionObj;
        this.ctx = ctx;
        userObj = UserStorage.getStorageUser(sessionObj.getUserId(), ctx);
    }
    
    public Object action(final String action, final JSONObject jsonObject) throws OXMandatoryFieldException, OXException, JSONException, SearchIteratorException, AjaxException, OXJSONException {
        if (action.equalsIgnoreCase(AJAXServlet.ACTION_DELETE)) {
            return actionDelete(jsonObject);
        } else if (action.equalsIgnoreCase(AJAXServlet.ACTION_UPDATES)) {
            return actionUpdates(jsonObject);
        } else if (action.equalsIgnoreCase(AJAXServlet.ACTION_RANGE)) {
            return actionRange(jsonObject);
        } else {
            throw new AjaxException(AjaxException.Code.UnknownAction, action);
        }
    }
    
    private JSONArray actionDelete(final JSONObject jsonObject) throws OXMandatoryFieldException, JSONException, OXException, OXJSONException, AjaxException {
        final JSONObject jData = DataParser.checkJSONObject(jsonObject, "data");
        final int id = DataParser.checkInt(jData, AJAXServlet.PARAMETER_ID);
        final int recurrencePosition = DataParser.parseInt(jData, CalendarFields.RECURRENCE_POSITION);
        
        final JSONArray jsonArray = new JSONArray();
        
        try {
            final Context ctx = ContextStorage.getInstance().getContext(sessionObj.getContextId());
            final ReminderSQLInterface reminderSql = new ReminderHandler(ctx);
        	
            final ReminderObject reminder = reminderSql.loadReminder(id);
            final ReminderDeleteInterface reminderDeleteInterface;
            final int module = reminder.getModule();
            switch (module) {
            	case Types.APPOINTMENT:
            		reminderDeleteInterface = new EmptyReminderDeleteImpl();
            		break;
            	case Types.TASK:
            		reminderDeleteInterface = new ModifyThroughDependant();
            		break;
            	default:
            		reminderDeleteInterface = new EmptyReminderDeleteImpl();
            }
            
            reminderSql.setReminderDeleteInterface(reminderDeleteInterface);
            
            if (reminder.isRecurrenceAppointment()) {
                final int targetId = Integer.parseInt(reminder.getTargetId());
                final int inFolder = Integer.parseInt(reminder.getFolder());
                
                final ReminderObject nextReminder = getNextRecurringReminder(targetId, recurrencePosition, inFolder, sessionObj);
                if (nextReminder != null) {
                    reminder.setDate(nextReminder.getDate());
                    reminderSql.updateReminder(reminder);
                    final JSONArray jsonResponseArray = new JSONArray();
                    jsonResponseArray.put(id);
                    return jsonResponseArray;
                }
                reminderSql.deleteReminder(id);
            } else {
                reminderSql.deleteReminder(id);
            }
        } catch(final SQLException sqle) {
            throw new OXException("SQLException occurred", sqle);
        } catch(final OXException oxe) {
            LOG.debug(oxe.getMessage(), oxe);
            
            if (oxe.getComponent().equals(EnumComponent.REMINDER) && oxe.getDetailNumber() == 9) {
                jsonArray.put(id);
                return jsonArray;
            }
            throw oxe;
        } catch(final AbstractOXException exc) {
            throw new OXException(exc);
        }
        
        return jsonArray;
    }
    
    private JSONArray actionUpdates(final JSONObject jsonObject) throws OXMandatoryFieldException, JSONException, OXException, SearchIteratorException, OXJSONException, AjaxException {
        timestamp = DataParser.checkDate(jsonObject, AJAXServlet.PARAMETER_TIMESTAMP);
        
        final JSONArray jsonResponseArray = new JSONArray();
        SearchIterator it = null;
       
        try {
        	final Context ctx = ContextStorage.getInstance().getContext(sessionObj);
            final ReminderSQLInterface reminderSql = new ReminderHandler(ctx);
            it = reminderSql.listModifiedReminder(userObj.getId(), timestamp);

            while (it.hasNext()) {
                final ReminderWriter reminderWriter = new ReminderWriter(TimeZone.getTimeZone(userObj.getTimeZone()));
                final ReminderObject reminderObj = (ReminderObject)it.next();
                
                if (reminderObj.isRecurrenceAppointment()) {
                    final int targetId = Integer.parseInt(reminderObj.getTargetId());
                    final int inFolder = Integer.parseInt(reminderObj.getFolder());
                    
//                    currently disabled because not used by the UI
//                    final ReminderObject latestReminder = getLatestReminder(targetId, inFolder, sessionObj, end);
//
//                    if (latestReminder == null) {
//                        continue;
//                    } else {
//                        reminderObj.setDate(latestReminder.getDate());
//                        reminderObj.setRecurrencePosition(latestReminder.getRecurrencePosition());
//                    }
                }
                
                if (hasModulePermission(reminderObj)) {
                    final JSONObject jsonReminderObj = new JSONObject();
                    reminderWriter.writeObject(reminderObj, jsonReminderObj);
                    jsonResponseArray.put(jsonReminderObj);
                }
            }
            
            return jsonResponseArray;
        } catch (final AbstractOXException e) {
        	throw new OXException(e);
        } finally {
            if (null != it) {
                it.close();
            }
        }
    }
    
    private JSONArray actionRange(final JSONObject jsonObject) throws OXMandatoryFieldException, JSONException, OXException, SearchIteratorException, OXJSONException, AjaxException {
        final Date end = DataParser.checkDate(jsonObject, AJAXServlet.PARAMETER_END);
        
        SearchIterator it = null;
        
        final JSONArray jsonResponseArray = new JSONArray();
        
        try {
        	final Context ctx = ContextStorage.getInstance().getContext(sessionObj.getContextId());
            final ReminderSQLInterface reminderSql = new ReminderHandler(ctx);
            it = reminderSql.listReminder(userObj.getId(), end);
            
            
            while (it.hasNext()) {
                final ReminderWriter reminderWriter = new ReminderWriter(TimeZone.getTimeZone(userObj.getTimeZone()));
                final ReminderObject reminderObj = (ReminderObject)it.next();
                
                if (reminderObj.isRecurrenceAppointment()) {
                    final int targetId = Integer.parseInt(reminderObj.getTargetId());
                    final int inFolder = Integer.parseInt(reminderObj.getFolder());
                    final Date oldReminderDate = reminderObj.getDate();
                    
                    ReminderObject latestReminder = null;
                    
                    try {
                    	latestReminder = getLatestRecurringReminder(targetId, inFolder, sessionObj, end, oldReminderDate);
                    } catch (final OXObjectNotFoundException exc) {
                    	LOG.warn("Cannot load target object of this reminder");
                    	reminderSql.deleteReminder(targetId, userObj.getId(), reminderObj.getModule());
                    }
                    
                    if (latestReminder == null) {
                        continue;
                    }
                    reminderObj.setDate(latestReminder.getDate());
                    reminderObj.setRecurrencePosition(latestReminder.getRecurrencePosition());
                }
                
                if (hasModulePermission(reminderObj)) {
                    final JSONObject jsonReminderObj = new JSONObject();
                    reminderWriter.writeObject(reminderObj, jsonReminderObj);
                    jsonResponseArray.put(jsonReminderObj);
                }
            }
            
            return jsonResponseArray;
        } catch (final SQLException e) {
            throw new OXException("SQLException occurred", e);
        } catch (final AbstractOXException e) {
            throw new OXException(e);
        } finally {
            if (null != it) {
                it.close();
            }
        }
    }
    
    protected boolean hasModulePermission(final ReminderObject reminderObj) {
		switch (reminderObj.getModule()) {
		case Types.APPOINTMENT:
			return UserConfigurationStorage.getInstance().getUserConfigurationSafe(sessionObj.getUserId(),
					ctx).hasCalendar();
		case Types.TASK:
			return UserConfigurationStorage.getInstance().getUserConfigurationSafe(sessionObj.getUserId(),
					ctx).hasTask();
		default:
			return true;
		}
	}
    
    /**
	 * This method returns the lastest reminder object of the recurrence
	 * appointment. The reminder object contains only the alarm attribute and
	 * the recurrence position.
	 */   
    protected ReminderObject getLatestRecurringReminder(final int objectId, final int inFolder, final Session sessionObj, final Date endRange, final Date oldReminderDate) throws OXException, SQLException {
        final CalendarSql calendarSql = new CalendarSql(sessionObj);
        final CalendarDataObject calendarDataObject = calendarSql.getObjectById(objectId, inFolder);
        final int alarm = calendarDataObject.getAlarm();
        
        final TimeZone timeZone = TimeZone.getTimeZone(userObj.getTimeZone());
        final Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTime(endRange);
        
        calendar.add(Calendar.MONTH, -3);
        
        final Date startRange = calendar.getTime();
        
        final RecurringResults recurringResults = CalendarRecurringCollection.calculateRecurring(calendarDataObject, startRange.getTime(),  endRange.getTime(), 0, 0, false);
        if (recurringResults != null) {
            final List<ReminderObject> reminderList = new ArrayList<ReminderObject>();
            for (int a = 0; a < recurringResults.size(); a++) {
                final RecurringResult recurringResult = recurringResults.getRecurringResult(a);
                final ReminderObject reminderObj = new ReminderObject();
                reminderObj.setRecurrenceAppointment(true);
                reminderObj.setRecurrencePosition(recurringResult.getPosition());
                
                calendar.setTimeInMillis(recurringResult.getStart());
                calendar.add(Calendar.MINUTE, 0-alarm);
                
                reminderObj.setDate(calendar.getTime());
                
                if (oldReminderDate.getTime() <= reminderObj.getDate().getTime()) {
                    reminderList.add(reminderObj);
                    // return reminderObj;
                }
            }
            
            if (reminderList.size() > 0) {
                return reminderList.get(reminderList.size()-1);
            }
        }
        
        return null;
    }
    
    /**
     * This method returns the next reminder object of the recurrence appointment. The reminder object contains only
     * the alarm attribute and the recurrence position. 
     **/
    protected ReminderObject getNextRecurringReminder(final int objectId, final int recurrencePosition, final int inFolder, final Session sessionObj) throws OXException, SQLException {
        final CalendarSql calendarSql = new CalendarSql(sessionObj);
        final CalendarDataObject calendarDataObject = calendarSql.getObjectById(objectId, inFolder);
        final int alarm = calendarDataObject.getAlarm();
        
        final RecurringResults recurringResults = CalendarRecurringCollection.calculateRecurring(calendarDataObject, 0, 0, recurrencePosition+1, 0, false);
        if (recurringResults != null && recurringResults.size() >= 1) {
            final RecurringResult recurringResult = recurringResults.getRecurringResult(recurringResults.size()-1);
            final ReminderObject reminderObj = new ReminderObject();
            reminderObj.setRecurrenceAppointment(true);
            reminderObj.setRecurrencePosition(recurringResult.getPosition());
            
            final TimeZone timeZone = TimeZone.getTimeZone(userObj.getTimeZone());
            final Calendar calendar = Calendar.getInstance(timeZone);
            calendar.setTimeInMillis(recurringResult.getStart());
            calendar.add(Calendar.MINUTE, 0-alarm);
            
            reminderObj.setDate(calendar.getTime());
            
            return reminderObj;
        }
        
        return null;
    }
}
