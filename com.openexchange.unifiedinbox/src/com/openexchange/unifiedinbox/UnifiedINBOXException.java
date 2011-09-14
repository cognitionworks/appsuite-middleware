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

package com.openexchange.unifiedinbox;

import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionCode;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.mime.MIMEMailExceptionCode;

/**
 * {@link UnifiedINBOXException} - Indicates a Unified INBOX error.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class UnifiedINBOXException extends OXException {

    /**
     * Serial Version UID
     */
    private static final long serialVersionUID = -8226676160145457046L;

    public static enum Code implements OXExceptionCode {

        /**
         * Unified INBOX does not support to create folders.
         */
        CREATE_DENIED("Unified INBOX does not support to create folders.", Category.CATEGORY_ERROR, 2001),
        /**
         * Unified INBOX does not support to delete folders.
         */
        DELETE_DENIED("Unified INBOX does not support to delete folders.", Category.CATEGORY_ERROR, 2002),
        /**
         * Unified INBOX does not support to update folders.
         */
        UPDATE_DENIED("Unified INBOX does not support to update folders.", Category.CATEGORY_ERROR, 2003),
        /**
         * Unified INBOX does not support to move messages.
         */
        MOVE_MSGS_DENIED("Unified INBOX does not support to move messages.", Category.CATEGORY_ERROR, 2004),
        /**
         * Unified INBOX does not support to copy messages.
         */
        COPY_MSGS_DENIED("Unified INBOX does not support to copy messages.", Category.CATEGORY_ERROR, 2005),
        /**
         * Append messages failed.
         */
        APPEND_MSGS_DENIED("Append messages failed.", Category.CATEGORY_ERROR, 2006),
        /**
         * Unified INBOX does not support draft messages.
         */
        DRAFTS_NOT_SUPPORTED("Unified INBOX does not support draft messages.", Category.CATEGORY_ERROR, 2007),
        /**
         * Unified INBOX does not support to move folders.
         */
        MOVE_DENIED("Unified INBOX does not support to move folders.", Category.CATEGORY_ERROR, 2008),
        /**
         * Unified INBOX does not support mail folder creation
         */
        FOLDER_CREATION_FAILED("Unified INBOX does not support mail folder creation", Category.CATEGORY_ERROR, 2009),
        /**
         * Unified INBOX does not support to clear INBOX folder.
         */
        CLEAR_NOT_SUPPORTED("Unified INBOX does not support to clear INBOX folder.", Category.CATEGORY_ERROR, 2010),
        /**
         * No connection available to access mailbox
         */
        NOT_CONNECTED("No connection available to access mailbox", Category.CATEGORY_ERROR, 2011),
        /**
         * Folder %1$s does not hold messages and is therefore not selectable.
         */
        FOLDER_DOES_NOT_HOLD_MESSAGES(MailExceptionCode.FOLDER_DOES_NOT_HOLD_MESSAGES),
        /**
         * Mail folder could not be found: %1$s.
         */
        FOLDER_NOT_FOUND(MIMEMailExceptionCode.FOLDER_NOT_FOUND),
        /**
         * Unknown default folder fullname: %1$s.
         */
        UNKNOWN_DEFAULT_FOLDER_INDEX("Unknown default folder fullname: %1$s.", Category.CATEGORY_ERROR, 2012),
        /**
         * Move operation aborted. Source and destination folder are equal.
         */
        NO_EQUAL_MOVE("Move operation aborted. Source and destination folder are equal.", Category.CATEGORY_ERROR, 2013),
        /**
         * Request aborted due to timeout of %1$s %2$s.
         */
        TIMEOUT("Request aborted due to timeout of %1$s %2$s.", Category.CATEGORY_ERROR, 2014),
        /**
         * Invalid destination folder. Don't know where to append the mails.
         */
        INVALID_DESTINATION_FOLDER("Invalid destination folder. Don't know where to append the mails.", Category.CATEGORY_USER_INPUT, 2015),
        ;

        private final String message;

        private final int detailNumber;

        private final Category category;

        private final String prefix;

        private Code(final String message, final Category category, final int detailNumber) {
            this.message = message;
            this.detailNumber = detailNumber;
            this.category = category;
            prefix = UnifiedINBOXProvider.PROTOCOL_UNIFIED_INBOX.getName();
        }

        private Code(final MailExceptionCode code) {
            message = code.getMessage();
            detailNumber = code.getNumber();
            category = code.getCategory();
            prefix = code.getPrefix();
        }

        private Code(final MIMEMailExceptionCode code) {
            message = code.getMessage();
            detailNumber = code.getNumber();
            category = code.getCategory();
            prefix = code.getPrefix();
        }

        public String getPrefix() {
            return prefix;
        }

        public Category getCategory() {
            return category;
        }

        public int getNumber() {
            return detailNumber;
        }

        public String getMessage() {
            return message;
        }

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

    /**
     * Gets the message corresponding to specified error code with given message arguments applied.
     *
     * @param code The code
     * @param msgArgs The message arguments
     * @return The message corresponding to specified error code with given message arguments applied
     */
    public static String getFormattedMessage(final Code code, final Object... msgArgs) {
        return String.format(code.getMessage(), msgArgs);
    }

    /**
     * Initializes a new {@link UnifiedINBOXException}
     */
    private UnifiedINBOXException() {
        super();
    }

}
