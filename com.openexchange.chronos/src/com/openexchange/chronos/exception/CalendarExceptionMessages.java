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

package com.openexchange.chronos.exception;

import com.openexchange.i18n.LocalizableStrings;

/**
 * {@link CalendarExceptionMessages}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class CalendarExceptionMessages implements LocalizableStrings {

    public static final String EVENT_NOT_FOUND_MSG = "The requested event was not found.";

    public static final String ATTENDEE_NOT_FOUND_MSG = "The requested attendee was not found.";

    public static final String ACCOUNT_NOT_FOUND_MSG = "The requested calendar account was not found.";

    public static final String PROVIDER_NOT_AVAILABLE_MSG = "The calendar provider \"%1$s\" is not available.";

    public static final String UNSUPPORTED_OPERATION_FOR_PROVIDER_MSG = "The requested operation is not supported for calendar provider \"%1$s\".";

    public static final String NO_PERMISSION_MSG = "The operation could not be completed due to insufficient permissions.";

    public static final String MISSING_CAPABILITY_MSG = "The operation could not be completed due to missing capabilities.";

    public static final String UNSUPPORTED_FOLDER_MSG = "The supplied folder is not supported. Please select a valid folder and try again.";

    public static final String CONCURRENT_MODIFICATION_MSG = "The operation could not be completed due to a concurrent modification. Please reload the data and try again.";

    public static final String UID_CONFLICT_MSG = "The event could not be created due to another conflicting event with the same unique identifier.";

    public static final String EVENT_CONFLICTS_MSG = "The event conflicts with one or more other events.";

    public static final String MANDATORY_FIELD_MSG = "The field \"%1$s\" is mandatory. Please supply a valid value and try again.";

    public static final String END_BEFORE_START_MSG = "The end date lies before the start date. Please correct the event times and try again.";

    public static final String UNSUPPORTED_CLASSIFICATION_MSG = "Events in non-personal folders must not be classified as \"private\" or \"confidential\".";

    public static final String UNSUPPORTED_CLASSIFICATION_FOR_MOVE_MSG = "Events classified as \"private\" or \"confidential\" cannot be moved to this type of folder.";

    public static final String UNSUPPORTED_CLASSIFICATION_FOR_OCCURRENCE_MSG = "Occurrences of event series must not be classified differently.";

    public static final String INVALID_RRULE_MSG = "The supplied recurrence rule is invalid. Please correct the rule and try again.";

    public static final String INVALID_TIMEZONE_MSG = "The supplied timezone is invalid. Please select a valid timezone and try again.";

    public static final String INVALID_GEO_LOCATION_MSG = "The supplied geographical location is invalid. Please select valid coordinates and try again.";

    public static final String UNSUPPORTED_RRULE_MSG = "The supplied recurrence rule is not supported. Please use adjust the rule and try again.";

    public static final String INVALID_RECURRENCE_ID_MSG = "The targeted occurrence is not part of the event series. Please select a valid recurrence identifier and try again.";

    public static final String MOVE_SERIES_NOT_SUPPORTED_MSG = "Moving an event series into another folder is not supported.";

    public static final String MOVE_OCCURRENCE_NOT_SUPPORTED_MSG = "Moving an occurrence of an event series into another folder is not supported.";

    public static final String INVALID_CALENDAR_USER_MSG = "The calendar user \"%1$s\" is invalid.";

    public static final String QUERY_TOO_SHORT_MSG = "In order to accomplish the search, %1$d or more characters are required.";

    public static final String INCORRECT_STRING_MSG = "The character \"%1$s\" in field \"%2$s\" can't be saved. Please remove the problematic character and try again.";

    public static final String DATA_TRUNCATION_MSG = "Some data entered exceeded the field limit. Please shorten the value for \"%1$s\" (limit: %2$d, current: %3$d) and try again.";

    public static final String IGNORED_INVALID_DATA_MSG = "The value for \"%2$s\" is invalid and wasn't be applied.";

    public static final String USER_HAS_NO_AVAILABILITY_SET = "The user '%1$s' has set no availability.";

    /**
     * Initializes a new {@link CalendarExceptionMessages}.
     */
    private CalendarExceptionMessages() {
        super();
    }
}
