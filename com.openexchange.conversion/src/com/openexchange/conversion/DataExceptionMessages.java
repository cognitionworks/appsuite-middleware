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

package com.openexchange.conversion;

import com.openexchange.exceptions.LocalizableStrings;

/**
 * Exception messages for {@link DataException} that must be translated.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class DataExceptionMessages implements LocalizableStrings {

    public static final String TYPE_NOT_SUPPORTED_MSG = "The given type of %1$s is not supported";
    public static final String MISSING_ARGUMENT_MSG = "Missing argument %1$s";
    public static final String INVALID_ARGUMENT_MSG = "Invalid value for argument %1$s: %2$s";
    public static final String UNKNOWN_DATA_SOURCE_MSG = "Unknown data source identifier: %1$s";
    public static final String UNKNOWN_DATA_HANDLER_MSG = "Unknown data handler identifier: %1$s";
    public static final String NO_MATCHING_TYPE_MSG = "No matching type could be found for data source %1$s and data handler %2$s";
    public static final String ERROR_MSG = "An error occurred: %1$s";
    public static final String TRUNCATED_MSG = "The following field(s) are too long: %1$s";

    /**
     * Prevent instantiation.
     */
    private DataExceptionMessages() {
        super();
    }
}
