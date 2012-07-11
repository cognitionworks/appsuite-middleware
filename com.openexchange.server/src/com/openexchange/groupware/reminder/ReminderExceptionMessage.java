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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.groupware.reminder;

import com.openexchange.i18n.LocalizableStrings;

/**
 * {@link ReminderExceptionMessage}
 * 
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public class ReminderExceptionMessage implements LocalizableStrings {

    /**
     * Initializes a new {@link ReminderExceptionMessage}.
     */
    private ReminderExceptionMessage() {
        super();
    }

    /**
     * User is missing for the reminder.
     */
    public final static String MANDATORY_FIELD_USER_MSG = "User is missing for the reminder.";

    /**
     * Identifier of the object is missing.
     */
    public final static String MANDATORY_FIELD_TARGET_ID_MSG = "Object identifier is missing.";

    /**
     * Alarm date for the reminder is missing.
     */
    public final static String MANDATORY_FIELD_ALARM_MSG = "Alarm date for the reminder is missing.";

    public final static String INSERT_EXCEPTION_MSG = "Unable to insert reminder";

    public final static String UPDATE_EXCEPTION_MSG = "Unable to update reminder";

    public final static String DELETE_EXCEPTION_MSG = "Unable to delete reminder";

    public final static String LOAD_EXCEPTION_MSG = "Unable to load reminder";

    public final static String LIST_EXCEPTION_MSG = "Unable to list reminder";

    /** Can not find reminder with identifier %1$d in context %2$d. */
    public final static String NOT_FOUND_MSG = "Reminder with identifier %1$d can not be found in context %2$d.";

    /**
     * Folder of the object is missing.
     */
    public final static String MANDATORY_FIELD_FOLDER_MSG = "Object folder is missing";

    /**
     * Module type of the object is missing.
     */
    public final static String MANDATORY_FIELD_MODULE_MSG = "Object's module type is missing";

    /**
     * Updated too many reminders.
     */
    public final static String TOO_MANY_MSG = "Updated too many reminders.";

    /** SQL Problem: %1$s. */
    public final static String SQL_ERROR_MSG = "SQL Problem: \"%1$s\"";

    /** No target service is registered for module %1$d. */
    public final static String NO_TARGET_SERVICE_MSG = "No target service is registered for module %1$d.";

    /**
     * Unexpected error: %1$s
     */
    public final static String UNEXPECTED_ERROR_MSG = "Unexpected error: %1$s";

    /**
     * Reminder identifier is missing.
     */
    public final static String MANDATORY_FIELD_ID_MSG = "Reminder identifier is missing.";

}
