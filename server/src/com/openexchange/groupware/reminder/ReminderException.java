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

package com.openexchange.groupware.reminder;

import com.openexchange.api2.OXException;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.EnumComponent;

/**
 * ReminderException
 * @author <a href="mailto:sebastian.kauss@open-xchange.org">Sebastian Kauss</a>
 */
public class ReminderException extends OXException {

    /**
     * For serialization.
     */
    private static final long serialVersionUID = 3162824095925586553L;

    /**
     * Constructor using other {@link AbstractOXException}.
     * @param cause the initial cause.
     */
    public ReminderException(final AbstractOXException e) {
        super(e);
    }

    public ReminderException(final Code code, final Object... messageArgs) {
        this(code, null, messageArgs);
    }

    public ReminderException(final Code code, final Throwable cause, final Object... messageArgs) {
        super(EnumComponent.REMINDER, code.category, code.detailNumber, code.message, cause);
        setMessageArgs(messageArgs);
    }

    public enum Code {
        /**
         * User is missing for the reminder.
         */
        MANDATORY_FIELD_USER("User is missing for the reminder.", 1, Category.CODE_ERROR),
        /**
         * Identifier of the object is missing.
         */
        MANDATORY_FIELD_TARGET_ID("Identifier of the object is missing.", 2, Category.CODE_ERROR),
        /**
         * Alarm date for the reminder is missing.
         */
        MANDATORY_FIELD_ALARM("Alarm date for the reminder is missing.", 3, Category.CODE_ERROR),
        INSERT_EXCEPTION("Unable to insert reminder", 4, Category.CODE_ERROR),
        UPDATE_EXCEPTION("Unable to update reminder.", 5, Category.CODE_ERROR),
        DELETE_EXCEPTION("Unable to delete reminder", 6, Category.CODE_ERROR),
        LOAD_EXCEPTION("Unable to load reminder", 7, Category.CODE_ERROR),
        LIST_EXCEPTION("Unable to list reminder", 8, Category.CODE_ERROR),
        NOT_FOUND("Cannot find reminder (identifier %d). Context %d.", 9, Category.CODE_ERROR),
        /**
         * Folder of the object is missing.
         */
        MANDATORY_FIELD_FOLDER("Folder of the object is missing.", 10, Category.CODE_ERROR),
        /**
         * Module type of the object is missing.
         */
        MANDATORY_FIELD_MODULE("Module type of the object is missing.", 11, Category.CODE_ERROR),
        /**
         * Updated too many reminders.
         */
        TOO_MANY("Updated too many reminders.", 12, Category.CODE_ERROR),
        /**
         * SQL Problem: "%1$s".
         */
        SQL_ERROR("SQL Problem: \"%1$s\".", 13, Category.CODE_ERROR);

        /**
         * Message of the exception.
         */
        private final String message;

        /**
         * Category of the exception.
         */
        private final Category category;

        /**
         * Detail number of the exception.
         */
        private final int detailNumber;

        /**
         * Default constructor.
         * @param message message.
         * @param category category.
         * @param detailNumber detail number.
         */
        private Code(final String message, final int detailNumber, final Category category)  {
            this.message = message;
            this.category = category;
            this.detailNumber = detailNumber;
        }

        public Category getCategory() {
            return category;
        }

        public int getDetailNumber() {
            return detailNumber;
        }

        public String getMessage() {
            return message;
        }
    }
}
