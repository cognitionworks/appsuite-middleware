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

package com.openexchange.messaging.facebook;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * {@link FacebookMessagingExceptionCodes} - Enumeration of all {@link OXException}s.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since Open-Xchange v6.16
 */
public enum FacebookMessagingExceptionCodes implements DisplayableOXExceptionCode {

    /**
     * An error occurred: %1$s
     */
    UNEXPECTED_ERROR("An error occurred: %1$s", CATEGORY_ERROR, 1),
    /**
     * A SQL error occurred: %1$s
     */
    SQL_ERROR("A SQL error occurred: %1$s", CATEGORY_ERROR, 2, OXExceptionStrings.SQL_ERROR_MSG),
    /**
     * An I/O error occurred: %1$s
     */
    IO_ERROR("An I/O error occurred: %1$s", CATEGORY_ERROR, 3),
    /**
     * An I/O error occurred: %1$s
     */
    JSON_ERROR("A JSON error occurred: %1$s", CATEGORY_ERROR, 4),

    // <---------------------- begin currently unused ---------------------->

    /**
     * Login to Facebook failed for login %1$s.
     */
    FAILED_LOGIN("Login to Facebook failed for login %1$s.", CATEGORY_ERROR, 5),
    /**
     * Communication error with Facebook service: %1$s
     */
    COMMUNICATION_ERROR("Communication error with Facebook service: %1$s", CATEGORY_SERVICE_DOWN, 6),
    /**
     * Login form not found on page: %1$s
     */
    LOGIN_FORM_NOT_FOUND("Login form not found on page: %1$s", CATEGORY_SERVICE_DOWN, 7),
    /**
     * Element with attribute %1$s not found on page %2$s.
     */
    ELEMENT_NOT_FOUND("Element with attribute %1$s not found on page %2$s.", CATEGORY_SERVICE_DOWN, 8),
    /**
     * Missing permission "%1$s" in Facebook login %2$s. Please copy following URL to your browser, login as %2$s (if not done yet) and grant access:<br>
     * %3$s
     */
    MISSING_PERMISSION("Missing permission \"%1$s\" in Facebook login %2$s. Please copy following URL to your browser, login as %2$s (if not done yet) and grant access:\n%3$s", CATEGORY_SERVICE_DOWN, 9),
    /**
     * An error occurred during the processing of a script.
     */
    SCRIPT_ERROR("An error occurred during the processing of a script.", CATEGORY_SERVICE_DOWN, 10),
    /**
     * Missing permission for the application associated with configured Facebook API key: %1$s<br>
     * Please grant access for that application in your Facebook account settings.
     */
    MISSING_APPLICATION_PERMISSION("Missing permission for the application associated with configured Facebook API key: %1$s\n" + "Please grant access for that application in your Facebook account settings.", CATEGORY_SERVICE_DOWN, 10),

    // <---------------------- end currently unused ---------------------->

    /**
     * FQL query result size (%1$s) does not match requested number of post identifiers (%2$s).
     */
    FQL_QUERY_RESULT_MISMATCH("FQL query result size (%1$s) does not match requested number of post identifiers (%2$s).", CATEGORY_ERROR, 11),
    /**
     * Unsupported query type: %1$s.
     */
    UNSUPPORTED_QUERY_TYPE("Unsupported query type: %1$s.", CATEGORY_ERROR, 12),
    /**
     * An OAuth error occurred: %1$s.
     */
    OAUTH_ERROR("An OAuth error occurred: %1$s.", CATEGORY_USER_INPUT, 13, FacebookMessagingExceptionMessages.OAUTH_ERROR_MSG),
    /**
     * A FQL error of type %1$s occurred: %2$s.
     */
    FQL_ERROR("A FQL error of type %1$s occurred: %2$s.", CATEGORY_ERROR, 14),
    /**
     * FQL response body cannot be parsed to a JSON value:<br>
     * %1$s
     */
    INVALID_RESPONSE_BODY("FQL response body cannot be parsed to a JSON value:\n%1$s", CATEGORY_ERROR, 15),
    /**
     * XML parse error: %1$s.
     */
    XML_PARSE_ERROR("XML parse error: %1$s.", CATEGORY_ERROR, 16),
    /**
     * Missing Facebook configuration. Please re-create Facebook account.
     */
    MISSING_CONFIG(FacebookMessagingExceptionMessages.MISSING_CONFIG_MSG, CATEGORY_USER_INPUT, 17, FacebookMessagingExceptionMessages.MISSING_CONFIG_MSG),
    /**
     * Missing Facebook configuration parameter "%1$s". Please re-create Facebook account.
     */
    MISSING_CONFIG_PARAM(FacebookMessagingExceptionMessages.MISSING_CONFIG_PARAM_MSG, CATEGORY_USER_INPUT, 18, FacebookMessagingExceptionMessages.MISSING_CONFIG_MSG),
    /**
     * A Facebook API error occurred. Error code: %1$s. Error message: "%2$s". Please refer to http://fbdevwiki.com/wiki/Error_codes to look-up error code.
     */
    FB_API_ERROR("A Facebook API error occurred. Error code: %1$s. Error message: \"%2$s\". Please refer to http://fbdevwiki.com/wiki/Error_codes to look-up error code.", CATEGORY_SERVICE_DOWN, 19),

    ;


    private final Category category;

    private final int detailNumber;

    private final String message;

    private final String displayMessage;

    private FacebookMessagingExceptionCodes(final String message, final Category category, final int detailNumber) {
        this(message, category, detailNumber, null);
    }

    private FacebookMessagingExceptionCodes(final String message, final Category category, final int detailNumber, final String displayMessage) {
        this.message = message;
        this.detailNumber = detailNumber;
        this.category = category;
        this.displayMessage = displayMessage == null ? OXExceptionStrings.MESSAGE : displayMessage;
    }

    @Override
    public String getPrefix() {
        return "FACEBOOK";
    }

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getNumber() {
        return detailNumber;
    }

    @Override
    public boolean equals(final OXException e) {
        return OXExceptionFactory.getInstance().equals(this, e);
    }

    @Override
    public String getDisplayMessage() {
        return displayMessage;
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     *
     * @return The newly created {@link OXException} instance
     */
    public OXException create() {
        return OXExceptionFactory.getInstance().create(this, new Object[0]);
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     *
     * @param args The message arguments in case of printf-style message
     * @return The newly created {@link OXException} instance
     */
    public OXException create(final Object... args) {
        return OXExceptionFactory.getInstance().create(this, (Throwable) null, args);
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     *
     * @param cause The optional initial cause
     * @param args The message arguments in case of printf-style message
     * @return The newly created {@link OXException} instance
     */
    public OXException create(final Throwable cause, final Object... args) {
        return OXExceptionFactory.getInstance().create(this, cause, args);
    }
}
