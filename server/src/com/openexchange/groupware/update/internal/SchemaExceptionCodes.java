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

package com.openexchange.groupware.update.internal;

import static com.openexchange.groupware.update.internal.SchemaExceptionMessages.ALREADY_LOCKED_MSG;
import static com.openexchange.groupware.update.internal.SchemaExceptionMessages.DATABASE_DOWN_MSG;
import static com.openexchange.groupware.update.internal.SchemaExceptionMessages.LOCK_FAILED_MSG;
import static com.openexchange.groupware.update.internal.SchemaExceptionMessages.MISSING_VERSION_ENTRY_MSG;
import static com.openexchange.groupware.update.internal.SchemaExceptionMessages.MULTIPLE_VERSION_ENTRY_MSG;
import static com.openexchange.groupware.update.internal.SchemaExceptionMessages.SQL_PROBLEM_MSG;
import static com.openexchange.groupware.update.internal.SchemaExceptionMessages.UNLOCK_FAILED_MSG;
import static com.openexchange.groupware.update.internal.SchemaExceptionMessages.UPDATE_CONFLICT_MSG;
import static com.openexchange.groupware.update.internal.SchemaExceptionMessages.WRONG_ROW_COUNT_MSG;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionCode;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.groupware.update.SchemaException;

/**
 * Exception codes for the {@link SchemaException}.
 * 
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public enum SchemaExceptionCodes implements OXExceptionCode {

    /**
     * No row found in table version in schema %1$s.
     */
    MISSING_VERSION_ENTRY(MISSING_VERSION_ENTRY_MSG, Category.CATEGORY_CONFIGURATION, 1),
    /**
     * Multiple rows found in table version in schema %1$s.
     */
    MULTIPLE_VERSION_ENTRY(MULTIPLE_VERSION_ENTRY_MSG, Category.CATEGORY_CONFIGURATION, 2),
    /**
     * Update conflict detected. Another process is currently updating schema %1$s.
     */
    ALREADY_LOCKED(ALREADY_LOCKED_MSG, Category.CATEGORY_PERMISSION_DENIED, 3),
    /**
     * Locking schema %1$s failed. Lock information could not be written to database.
     */
    LOCK_FAILED(LOCK_FAILED_MSG, Category.CATEGORY_ERROR, 4),
    /**
     * Update conflict detected. Schema %1$s is not marked as locked.
     */
    UPDATE_CONFLICT(UPDATE_CONFLICT_MSG, Category.CATEGORY_ERROR, 5),
    /**
     * Schema %1$s could not be unlocked. Lock information could no be removed from database.
     */
    UNLOCK_FAILED(UNLOCK_FAILED_MSG, Category.CATEGORY_ERROR, 6),
    /**
     * A SQL problem occurred: %1$s.
     */
    SQL_PROBLEM(SQL_PROBLEM_MSG, Category.CATEGORY_ERROR, 7),
    /**
     * Cannot get database connection.
     */
    DATABASE_DOWN(DATABASE_DOWN_MSG, Category.CATEGORY_SERVICE_DOWN, 8),
    /**
     * Processed a wrong number of rows in database. Expected %1$d rows but worked on %2$d rows.
     */
    WRONG_ROW_COUNT(WRONG_ROW_COUNT_MSG, Category.CATEGORY_ERROR, 9);

    /**
     * Message of the exception.
     */
    final String message;

    /**
     * Category of the exception.
     */
    final Category category;

    /**
     * Detail number of the exception.
     */
    final int number;

    /**
     * Default constructor.
     * 
     * @param message message.
     * @param category category.
     * @param number detail number.
     */
    private SchemaExceptionCodes(final String message, final Category category, final int number) {
        this.message = message;
        this.category = category;
        this.number = number;
    }

    public String getPrefix() {
        return "UPD";
    }

    public Category getCategory() {
        return category;
    }

    public int getNumber() {
        return number;
    }

    public String getMessage() {
        return message;
    }
    
    public boolean equals(final OXException e) {
        return getPrefix().equals(e.getPrefix()) && e.getCode() == getNumber();
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
