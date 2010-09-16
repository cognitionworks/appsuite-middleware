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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.subscribe.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.openexchange.calendar.CalendarSql;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.subscribe.Subscription;
import com.openexchange.subscribe.SubscriptionSession;
import com.openexchange.tools.iterator.SearchIterator;

/**
 * {@link CalendarFolderUpdaterStrategy}
 * 
 * @author <a href="mailto:karsten.will@open-xchange.com">Karsten Will</a>
 */
public class CalendarFolderUpdaterStrategy implements FolderUpdaterStrategy<CalendarDataObject> {

    private static final int SQL_INTERFACE = 1;

    private static final int SUBSCRIPTION = 2;

    private static final int[] COMPARISON_COLUMNS = {
        Appointment.OBJECT_ID, Appointment.FOLDER_ID, Appointment.TITLE, Appointment.START_DATE, Appointment.END_DATE, Appointment.UID, Appointment.NOTE, Appointment.LAST_MODIFIED, Appointment.SEQUENCE };

    public int calculateSimilarityScore(CalendarDataObject original, CalendarDataObject candidate, Object session) throws AbstractOXException {
        int score = 0;
        // A score of 10 is sufficient for a match        
        // If the UID is the same we can assume it is the same event. Please note the UID is assumed to have been saved with contextid and folder-id as prefix here
        String candidatesUID = getPrefixForUID(original) + candidate.getUid();
        if ((isset(original.getUid()) || isset(candidate.getUid())) && eq(original.getUid(), candidatesUID)) {
            score += 10;
        }
        if ((isset(original.getTitle()) || isset(candidate.getTitle())) && eq(original.getTitle(), candidate.getTitle())) {
            score += 5;
        }
        if ((isset(original.getNote()) || isset(candidate.getNote())) && eq(original.getNote(), candidate.getNote())) {
            score += 3;
        }
        if (original.getStartDate() != null && candidate.getStartDate() != null && eq(original.getStartDate(), candidate.getStartDate())) {
            score += 3;
        }
        if (original.getEndDate() != null && candidate.getEndDate() != null && eq(original.getEndDate(), candidate.getEndDate())) {
            score += 3;
        }

        return score;
    }

    private boolean isset(String s) {
        return s == null || s.length() > 0;
    }

    protected boolean eq(Object o1, Object o2) {
        if (o1 == null || o2 == null) {
            return false;
        } else {
            return o1.equals(o2);
        }
    }

    public void closeSession(Object session) throws AbstractOXException {

    }

    public Collection<CalendarDataObject> getData(Subscription subscription, Object session) throws AbstractOXException {
        CalendarSql calendarSql = (CalendarSql) getFromSession(SQL_INTERFACE, session);

        int folderId = subscription.getFolderIdAsInt();
        // get all the appointments, from the beginning of time (1970) till the end of time
        Date startDate = new Date(0);
        Date endDate = new Date(Long.MAX_VALUE);
        SearchIterator<Appointment> appointmentsInFolder;
        List<CalendarDataObject> retval = new ArrayList<CalendarDataObject>();
        try {
            appointmentsInFolder = calendarSql.getAppointmentsBetweenInFolder(folderId, COMPARISON_COLUMNS, startDate, endDate, 0, "ASC");

            while (appointmentsInFolder.hasNext()) {
                retval.add((CalendarDataObject) appointmentsInFolder.next());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return retval;
    }

    public int getThreshold(Object session) throws AbstractOXException {
        return 9;
    }

    public boolean handles(FolderObject folder) {
        return folder.getModule() == FolderObject.CALENDAR;
    }

    public void save(CalendarDataObject newElement, Object session) throws AbstractOXException {
        CalendarSql calendarSql = (CalendarSql) getFromSession(SQL_INTERFACE, session);
        Subscription subscription = (Subscription) getFromSession(SUBSCRIPTION, session);
        newElement.setParentFolderID(subscription.getFolderIdAsInt());
        newElement.setContext(subscription.getContext());
        addPrefixToUID(newElement);  
        calendarSql.insertAppointmentObject(newElement);
    }

    private Object getFromSession(int key, Object session) {
        return ((Map<Integer, Object>) session).get(key);
    }

    public Object startSession(Subscription subscription) throws AbstractOXException {
        Map<Integer, Object> userInfo = new HashMap<Integer, Object>();
        userInfo.put(SQL_INTERFACE, new CalendarSql(new SubscriptionSession(subscription)));
        userInfo.put(SUBSCRIPTION, subscription);
        return userInfo;
    }

    public void update(CalendarDataObject original, CalendarDataObject update, Object session) throws AbstractOXException {
        CalendarSql calendarSql = (CalendarSql) getFromSession(SQL_INTERFACE, session);

        update.setParentFolderID(original.getParentFolderID());
        update.setObjectID(original.getObjectID());
        update.setLastModified(original.getLastModified());
        update.setContext(original.getContext());
        addPrefixToUID(update);

        calendarSql.updateAppointmentObject(update, original.getParentFolderID(), original.getLastModified());
    }
    
    private void addPrefixToUID (CalendarDataObject cdo){        
                cdo.setUid(getPrefixForUID(cdo) + cdo.getUid());            
    }

    private String getPrefixForUID (CalendarDataObject cdo){
        if (null != cdo.getUid()){
            if (! cdo.getUid().equals("")){
                    return Integer.toString(cdo.getContextID() + cdo.getParentFolderID());
            }
        }
        return "";        
    }
}
