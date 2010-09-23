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
package com.openexchange.mailfilter.ajax.exceptions;

import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.Component;
import com.openexchange.groupware.EnumComponent;

/**
 * {@link OXMailfilterException}
 *
 * @author <a href="mailto:dennis.sieben@open-xchange.com">Dennis Sieben</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class OXMailfilterException extends AbstractOXException {

    private static final long serialVersionUID = -4143376197058972709L;

    public static enum Code {

    	/**
    	 * %s
    	 */
    	PROBLEM("%s", Category.CODE_ERROR, 1),
        /**
         * %s
         */
        SESSION_EXPIRED("%s", Category.PERMISSION, 200),
        /**
         * Missing parameter %s
         */
        MISSING_PARAMETER("Missing parameter %s", Category.CODE_ERROR, 1),
        /**
         * Invalid credentials
         */
        INVALID_CREDENTIALS("Invalid sieve credentials", Category.PERMISSION, 2),
        /**
         * A JSON error occurred: %s
         */
        JSON_ERROR("A JSON error occurred: %s", Category.CODE_ERROR, 3),
        /**
         * Property error: %s
         */
        PROPERTY_ERROR("Property error: %s", Category.SETUP_ERROR, 4),
        /**
         * Sieve error: %1$s
         */
        SIEVE_ERROR("Sieve error: %1$s", Category.CODE_ERROR, 5),
        /**
         * mail filter servlet cannot be registered: %s
         */
        SERVLET_REGISTRATION_FAILED("mail filter servlet cannot be registered: %s", Category.CODE_ERROR, 6),
        /**
         * The position where the rule should be added is too big
         */
        POSITION_TOO_BIG("The position where the rule should be added is too big", Category.CODE_ERROR, 7),
        /**
         * A rule with the id %1$s does not exist for user %2$s in context %3$s
         */
        NO_SUCH_ID("A rule with the id %1$s does not exist for user %2$s in context %3$s", Category.CODE_ERROR, 8),
        /**
         * The id is missing inside the update request
         */
        ID_MISSING("The id is missing inside the update request or has a non integer type", Category.CODE_ERROR, 9),
        /**
         * A server name cannot be found in the server URL
         */
        NO_SERVERNAME_IN_SERVERURL("A server name cannot be found in the server URL", Category.CODE_ERROR, 10),
        /**
         * The login type given in the config file is not a valid one
         */
        NO_VALID_LOGIN_TYPE("The login type given in the config file is not a valid one", Category.CODE_ERROR, 11),
        /**
         * The credsrc given in the config file is not a valid one
         */
        NO_VALID_CREDSRC("The credsrc given in the config file is not a valid one", Category.CODE_ERROR, 12),
        /**
         * The encoding given is not supported by Java
         */
        UNSUPPORTED_ENCODING("The encoding given is not supported by Java", Category.CODE_ERROR, 13),
        /**
         * Error in low level connection to sieve server
         */
        IO_CONNECTION_ERROR("Error in low level connection to sieve server %1$s at host %2$s", Category.CODE_ERROR, 14),
        /**
         * Error while communicating with the sieve server %1$s at port %2$s for user %3$s in context %4$s
         */
        SIEVE_COMMUNICATION_ERROR("Error while communicating with the sieve server %1$s at port %2$s for user %3$s in context %4$s", Category.CODE_ERROR, 15),
        /**
         * Lexical error: %1$s
         */
        LEXICAL_ERROR("Lexical error: %1$s", Category.CODE_ERROR, 16),
        /**
         * Input string "%1$s" is not a number.
         */
        NAN("Input string \"%1$s\" is not a number.", Category.USER_INPUT, 17),
        /**
         * The field \"%1$s\" must have a value, but is not set.
         */
        EMPTY_MANDATORY_FIELD("The field \"%1$s\" must have a value, but is not set", Category.USER_INPUT, 18),
        /**
         * The configuration requests a masterpassword but none is given in the configuration file
         */
        NO_MASTERPASSWORD_SET("The configuration requests a masterpassword but none is given in the configuration file", Category.CODE_ERROR, 19),
        /**
         * The passwordSource given in the config file is not a valid one
         */
        NO_VALID_PASSWORDSOURCE("The passwordSource given in the config file is not a valid one", Category.CODE_ERROR, 20);


        private final String message;

        private final int detailNumber;

        private final Category category;

        private Code(final String message, final Category category, final int detailNumber) {
            this.message = message;
            this.detailNumber = detailNumber;
            this.category = category;
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

    }

    public OXMailfilterException(final AbstractOXException cause) {
        super(cause);
    }

    public OXMailfilterException(final Code code, final Object... messageArgs) {
        this(code, null, messageArgs);
    }

    public OXMailfilterException(final Code code, final Throwable cause, final Object... messageArgs) {
        super(EnumComponent.MAIL_FILTER, code.getCategory(), code.getNumber(), code.getMessage(), cause);
        super.setMessageArgs(messageArgs);
    }

    public OXMailfilterException(final Component component, final Category category, final int detailNumber, final String message, final Throwable cause) {
        super(component, category, detailNumber, message, cause);
    }

    public OXMailfilterException(final Component component, final String message, final AbstractOXException cause) {
        super(component, message, cause);
    }

    public OXMailfilterException(final Component component, final Code code, final Throwable cause, final Object... messageArgs) {
        super(component, code.getCategory(), code.getNumber(), code.getMessage(), cause);
        setMessageArgs(messageArgs);
    }
}
