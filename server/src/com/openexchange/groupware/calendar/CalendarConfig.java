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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.groupware.calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.configuration.ConfigurationException;
import com.openexchange.configuration.SystemConfig;
import com.openexchange.configuration.ConfigurationException.Code;
import com.openexchange.configuration.SystemConfig.Property;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.server.Initialization;
import com.openexchange.server.impl.Starter;
import com.openexchange.tools.conf.AbstractConfig;

/**
 * Configuration class for calendar options.
 * <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class CalendarConfig extends AbstractConfig implements Initialization {
    
    private static final CalendarConfig singleton = new CalendarConfig();
    
    private static final Property KEY = Property.CALENDAR;
    
    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(CalendarConfig.class));
    
    private static boolean solo_reminder_trigger_event = true;
    private static boolean check_and_remove_past_reminders = true;
    private static int max_operations_in_recurrence_calculations;

    private static boolean CACHED_ITERATOR_FAST_FETCH = true;
    
    private static boolean seriesconflictlimit = true;
    
    private static boolean undefinedstatusconflict = true;
    
    public static boolean isCACHED_ITERATOR_FAST_FETCH() {
        return CACHED_ITERATOR_FAST_FETCH;
    }

    
    public static int getMAX_PRE_FETCH() {
        return MAX_PRE_FETCH;
    }

    private static int MAX_PRE_FETCH = 20;

    /**
     * Prevent instantiation.
     */
    private CalendarConfig() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getPropertyFileName() throws ConfigurationException {
        final String filename = SystemConfig.getProperty(KEY);
        if (null == filename) {
            throw new ConfigurationException(Code.PROPERTY_MISSING,
                    KEY.getPropertyName());
        }
        return filename;
    }
    
    public static String getProperty(final String key) {
        return singleton.getPropertyInternal(key);
    }

    /**
     * @return the singleton instance.
     */
    public static final CalendarConfig getInstance() {
        return singleton;
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws AbstractOXException {
        if (isPropertiesLoadInternal()) {
            LOG.error("Duplicate initialization of CalendarConfig.");
            return;
        }
        reinit();
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws AbstractOXException {
        if (!isPropertiesLoadInternal()) {
            LOG.error("Duplicate shutdown of CalendarConfig.");
            return;
        }
        clearProperties();
    }

    /**
     * FIXME remove this method.
     * @throws ConfigurationException
     * @deprecated use normal server startup through {@link Starter}.
     */
    @Deprecated
	public static void init() throws ConfigurationException {
        if (null == singleton) {
            reinit();
        }
    }
    
    public static void reinit() throws ConfigurationException {
        singleton.loadPropertiesInternal();
        String check_cached_iterator_fast_fetch = CalendarConfig.getProperty("CACHED_ITERATOR_FAST_FETCH");
        if (check_cached_iterator_fast_fetch != null) {
            check_cached_iterator_fast_fetch = check_cached_iterator_fast_fetch.trim();
            if (check_cached_iterator_fast_fetch.equalsIgnoreCase("FALSE") || check_cached_iterator_fast_fetch.equalsIgnoreCase("0")) {
                CACHED_ITERATOR_FAST_FETCH = false;
            }
        }
        String check_max_pre_fetch_size = CalendarConfig.getProperty("MAX_PRE_FETCH");
        if (check_max_pre_fetch_size != null){
            check_max_pre_fetch_size = check_max_pre_fetch_size.trim();
            try {
                final int mfs = Integer.parseInt(check_max_pre_fetch_size);
                if (mfs > 1 && mfs < 1000) {
                    MAX_PRE_FETCH = mfs;
                }
            } catch(final NumberFormatException nfe) {
                LOG.error("Unable to parse config parameter MAX_PRE_FETCH: "+check_max_pre_fetch_size);
            }
        }
        String check_and_remove_past_reminders_string = CalendarConfig.getProperty("CHECK_AND_REMOVE_PAST_REMINDERS");
        if (check_and_remove_past_reminders_string != null){
            check_and_remove_past_reminders_string = check_and_remove_past_reminders_string.trim();
            if (check_and_remove_past_reminders_string.equalsIgnoreCase("FALSE")) {
                check_and_remove_past_reminders = false;
            }
        }     
        String solo_reminder_trigger_event_string = CalendarConfig.getProperty("CHECK_AND_AVOID_SOLO_REMINDER_TRIGGER_EVENTS");
        if (solo_reminder_trigger_event_string != null){
            solo_reminder_trigger_event_string = solo_reminder_trigger_event_string.trim();
            if (solo_reminder_trigger_event_string.equalsIgnoreCase("FALSE")) {
                solo_reminder_trigger_event = false;
            }
        }

        String max_operations_in_recurrence_calculations_string = CalendarConfig.getProperty("MAX_OPERATIONS_IN_RECURRENCE_CALCULATIONS");
        if (max_operations_in_recurrence_calculations_string == null) {
            max_operations_in_recurrence_calculations = 999 * 50;
        } else {
        	max_operations_in_recurrence_calculations_string = max_operations_in_recurrence_calculations_string.trim();
        	try {
				max_operations_in_recurrence_calculations = Integer.parseInt(max_operations_in_recurrence_calculations_string);
			} catch (final NumberFormatException e) {
				LOG.error("Unable to parse config parameter MAX_OPERATIONS_IN_RECURRENCE_CALCULATIONS: "+max_operations_in_recurrence_calculations_string);
				max_operations_in_recurrence_calculations = 999 * 50;
			} 
        }
        
        String series_conflict_limit_string = CalendarConfig.getProperty("com.openexchange.calendar.seriesconflictlimit");
        if (series_conflict_limit_string != null){
            series_conflict_limit_string = series_conflict_limit_string.trim();
            if (series_conflict_limit_string.equalsIgnoreCase("FALSE")) {
                seriesconflictlimit = false;
            }
        }
        
        String undefined_status_conflict_string = CalendarConfig.getProperty("com.openexchange.calendar.undefinedstatusconflict");
        if (undefined_status_conflict_string != null){
            undefined_status_conflict_string = undefined_status_conflict_string.trim();
            if (undefined_status_conflict_string.equalsIgnoreCase("FALSE")) {
                undefinedstatusconflict = false;
            }
        }
    }
    
    public static boolean getCheckAndRemovePastReminders() {
        return check_and_remove_past_reminders;
    }
    
    public static boolean getSoloReminderTriggerEvent() {
        return solo_reminder_trigger_event;
    }

    public static int getMaxOperationsInRecurrenceCalculations() {
        return max_operations_in_recurrence_calculations;
    }
    
    public static boolean getSeriesConflictLimit() {
        return seriesconflictlimit;
    }
    
    public static boolean getUndefinedStatusConflict() {
        return undefinedstatusconflict;
    }
    
    //friendly methods for testing purposes
    
    static void setCheckAndRemovePastReminders(boolean value) {
        check_and_remove_past_reminders = value;
    }
    
    static void setSoloReminderTriggerEvent(boolean value) {
        solo_reminder_trigger_event = value;
    }

    static void setMaxOperationsInRecurrenceCalculations(int value) {
        max_operations_in_recurrence_calculations = value;
    }
    
    static void setSeriesConflictLimit(boolean value) {
        seriesconflictlimit = value;
    }
    
    static void setUndefinedStatusConflict(boolean value) {
        undefinedstatusconflict = value;
    }
}
