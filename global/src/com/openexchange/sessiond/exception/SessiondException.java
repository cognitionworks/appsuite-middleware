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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.sessiond.exception;

import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.EnumComponent;

/**
 * Exception if something not works as expected with the session.
 * 
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class SessiondException extends AbstractOXException {

    /**
     * For Serialization.
     */
    private static final long serialVersionUID = 6320550676305333711L;

    /**
     * Initializes a new exception using the information provided by the cause.
     * 
     * @param cause the cause of the exception.
     */
    public SessiondException(final AbstractOXException cause) {
        super(cause);
    }

    /**
     * Constructor with all parameters.
     * 
     * @param code code
     */
    public SessiondException(final Code code) {
        super(EnumComponent.SESSION, code.getCategory(), code.getDetailNumber(), code.getMessage(), null);
    }

    /**
     * Constructor with all parameters.
     * 
     * @param code code
     * @param cause the cause.
     * @param msgArgs arguments for the exception message.
     */
    public SessiondException(final Category category, final String message, final int detailNumber, final Throwable cause) {
        super(EnumComponent.SESSION, category, detailNumber, message, cause);
    }

    /**
     * Constructor with all parameters.
     * 
     * @param code code
     * @param cause the cause.
     * @param msgArgs arguments for the exception message.
     */
    public SessiondException(final Code code, final Throwable cause, final Object... msgArgs) {
        super(EnumComponent.SESSION, code.getCategory(), code.getDetailNumber(), code.getMessage(), cause);
        setMessageArgs(msgArgs);
    }

    public SessiondException(Code code, Object... msgArgs) {
        this(code, null, msgArgs);
    }

    public SessiondException(final EnumComponent component, final Category category, final int number, final String message, final Throwable cause, final Object... msgArgs) {
        super(component, category, number, message, cause);
        setMessageArgs(msgArgs);
    }

    public enum Code {
        SESSIOND_EXCEPTION("Sessiond Exception", 1, AbstractOXException.Category.CODE_ERROR),
        MAX_SESSION_EXCEPTION("Max Session size reached", 2, AbstractOXException.Category.CODE_ERROR),
        SESSIOND_CONFIG_EXCEPTION("Sessiond Config Exception", 3, AbstractOXException.Category.CODE_ERROR),
        MISSING_PROPERTY("Missing property '%s'", 4, AbstractOXException.Category.SETUP_ERROR),
        /**
         * Unknown event topic %s
         */
        UNKNOWN_EVENT_TOPIC("Unknown event topic %s", 5, AbstractOXException.Category.CODE_ERROR),
        /**
         * Password could not be changed
         */
        PASSWORD_UPDATE_FAILED("Password could not be changed", 6, AbstractOXException.Category.CODE_ERROR),
        /**
         * Max. session size for user %1$s in context %2$s exceeded
         */
        MAX_SESSION_PER_USER_EXCEPTION("Max. session size for user %1$s in context %2$s exceeded", 7, AbstractOXException.Category.CODE_ERROR),
        /**
         * Found duplicate used authentication identifier. Login of existing session: %1$s. Current denied login request: %2$s.
         */
        DUPLICATE_AUTHID("Found duplicate used authentication identifier. Login of existing session: %1$s. Current denied login request: %2$s.", 8, Category.CODE_ERROR);

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
         * 
         * @param message message.
         * @param category category.
         * @param detailNumber detail number.
         */
        private Code(final String message, final int detailNumber, final Category category) {
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
