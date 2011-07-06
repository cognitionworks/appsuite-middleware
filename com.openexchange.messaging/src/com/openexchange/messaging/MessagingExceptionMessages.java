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

package com.openexchange.messaging;

import com.openexchange.i18n.LocalizableStrings;

/**
 * {@link MessagingExceptionMessages} - Exception messages for {@link OXException} that needs to be translated.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since Open-Xchange v6.16
 */
public final class MessagingExceptionMessages implements LocalizableStrings {

    // An error occurred: %1$s
    public static final String UNEXPECTED_ERROR_MSG = "An error occurred: %1$s";

    // A SQL error occurred: %1$s
    public static final String SQL_ERROR_MSG = "A SQL error occurred: %1$s";

    // An I/O error occurred: %1$s
    public static final String IO_ERROR_MSG = "An I/O error occurred: %1$s";

    // A JSON error occurred: %1$s
    public static final String JSON_ERROR_MSG = "A JSON error occurred: %1$s";

    // Messaging account %1$s of service "%2$s" could not be found for user %3$s in context %4$s.
    public static final String ACCOUNT_NOT_FOUND_MSG = "Messaging account %1$s of service \"%2$s\" could not be found for user %3$s in context %4$s.";

    // The operation is not supported by service %1$s.
    public static final String OPERATION_NOT_SUPPORTED_MSG = "The operation is not supported by service %1$s.";

    // The folder "%1$s" cannot be found in account %2$s of service "%3$s" of user %4$s in context %5$s.
    public static final String FOLDER_NOT_FOUND_MSG = "The folder \"%1$s\" cannot be found in account %2$s of service \"%3$s\" of user %4$s in context %5$s.";

    // Invalid message identifier: %1$s
    public static final String INVALID_MESSAGE_IDENTIFIER_MSG = "Invalid message identifier: %1$s";

    // Invalid header "%1$s": %2$s
    public static final String INVALID_HEADER_MSG = "Invalid header \"%1$s\": %2$s";

    // Unknown action to perform: %1$s.
    public static final String UNKNOWN_ACTION_MSG = "Unknown action to perform: %1$s.";

    // A messaging error occurred: %1$s
    public static final String MESSAGING_ERROR_MSG = "A messaging error occurred: %1$s";

    // Wrongly formatted address: %1$s.
    public static final String ADDRESS_ERROR_MSG = "Wrongly formatted address: %1$s.";

    // Unknown messaging content: %1$s.
    public static final String UNKNOWN_MESSAGING_CONTENT_MSG = "Unknown messaging content: %1$s.";
    
    // Unknown messaging service: %1$s.
    public static final String UNKNOWN_MESSAGING_SERVICE_MSG = "Unknown messaging service: %1$s.";
    
    //  Missing parameter: %1$s
    public static final String MISSING_PARAMETER_MSG = "Missing parameter: %1$s.";
    
    // Invalid parameter: %1$s with value '%2$s'.
    public static final String INVALID_PARAMETER_MSG = "Invalid parameter: %1$s with value '%2$s'.";

    // Messaging part is read-only: %1$s
    public static final String READ_ONLY_MSG = "Messaging part is read-only.: %1$s";

    // Unknown color label index: %1$s
    public static final String UNKNOWN_COLOR_LABEL_MSG = "Unknown color label index: %1$s";

    // A duplicate folder named "%1$s" already exists below parent folder "%2$s".
    public static final String DUPLICATE_FOLDER_MSG = "A duplicate folder named \"%1$s\" already exists below parent folder \"%2$s\".";

    // No create access on mail folder %1$s.
    public static final String NO_CREATE_ACCESS_MSG = "No create access on mail folder %1$s.";

    // Not connected
    public static final String NOT_CONNECTED_MSG = "Not connected";

    // Invalid sorting column. Cannot sort by %1$s.
    public static final String INVALID_SORTING_COLUMN_MSG = "Invalid sorting column. Cannot sort by %1$s.";

    // No attachment found with section identifier %1$s in message %2$s in folder %3$s.
    public static final String ATTACHMENT_NOT_FOUND_MSG = "No attachment found with section identifier %1$s in message %2$s in folder %3$s.";

    // Message %1$s not found in folder %2$s.
    public static final String MESSAGE_NOT_FOUND_MSG = "Message %1$s not found in folder %2$s.";

    /**
     * Initializes a new {@link MessagingExceptionMessages}.
     */
    private MessagingExceptionMessages() {
        super();
    }

}
