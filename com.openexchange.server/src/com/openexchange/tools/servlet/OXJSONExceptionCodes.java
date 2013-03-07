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

package com.openexchange.tools.servlet;

import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionCode;
import com.openexchange.exception.OXExceptionFactory;

/**
 * Error codes for JSON exceptions.
 */
public enum OXJSONExceptionCodes implements OXExceptionCode {
    /**
     * Exception while writing JSON.
     */
    JSON_WRITE_ERROR(OXJSONExceptionMessage.JSON_WRITE_ERROR_MSG, Category.CATEGORY_ERROR, 1),
    /**
     * Exception while parsing JSON: "%s".
     */
    JSON_READ_ERROR(OXJSONExceptionMessage.JSON_READ_ERROR_MSG, Category.CATEGORY_ERROR, 2),
    /**
     * Invalid cookie.
     */
    INVALID_COOKIE(OXJSONExceptionMessage.INVALID_COOKIE_MSG, Category.CATEGORY_TRY_AGAIN, 3),
    /**
     * Exception while building JSON.
     */
    JSON_BUILD_ERROR(OXJSONExceptionMessage.JSON_BUILD_ERROR_MSG, Category.CATEGORY_ERROR, 4),
    /**
     * Value "%1$s" of attribute %s contains non digit characters.
     */
    CONTAINS_NON_DIGITS(OXJSONExceptionMessage.CONTAINS_NON_DIGITS_MSG, Category.CATEGORY_USER_INPUT, 5),
    /**
     * Too many digits within field %1$s.
     */
    TOO_BIG_NUMBER(OXJSONExceptionMessage.TOO_BIG_NUMBER_MSG, Category.CATEGORY_USER_INPUT, 6),
    /**
     * Unable to parse value "%1$s" within field %2$s as a number.
     */
    NUMBER_PARSING(OXJSONExceptionMessage.NUMBER_PARSING_MSG, Category.CATEGORY_ERROR, 7),
    /**
     * Invalid value \"%2$s\" in JSON attribute \"%1$s\".
     */
    INVALID_VALUE(OXJSONExceptionMessage.INVALID_VALUE_MSG, Category.CATEGORY_USER_INPUT, 8),
    /**
     * Missing field "%1$s" in JSON data.
     */
    MISSING_FIELD(OXJSONExceptionMessage.MISSING_FIELD_MSG, Category.CATEGORY_ERROR, 9);

    private static final String PREFIX = "SVL";

    private final String message;
    private final Category category;
    private final int number;

    private OXJSONExceptionCodes(final String message, final Category category, final int detailNumber) {
        this.message = message;
        this.category = category;
        number = detailNumber;
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
        return number;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public boolean equals(final OXException e) {
        return OXExceptionFactory.getInstance().equals(this, e);
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
