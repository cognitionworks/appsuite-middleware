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

package com.openexchange.spamhandler.spamassassin.exceptions;

import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.Component;
import com.openexchange.groupware.EnumComponent;
import com.openexchange.exception.OXException;


public class SpamhandlerSpamassassinException extends OXException {

    public static enum Code {
        /**
         * Spamd returned wrong exit code "%s"
         */
        WRONG_SPAMD_EXIT("Spamd returned wrong exit code \"%s\"", Category.CODE_ERROR, 3000),

        /**
         * Internal error: Wrong arguments are given to the tell command: "%s"
         */
        WRONG_TELL_CMD_ARGS("Internal error: Wrong arguments are given to the tell command: \"%s\"", Category.CODE_ERROR, 3001),

        /**
         * Error during communication with spamd: "%s"
         */
        COMMUNICATION_ERROR("Error during communication with spamd: \"%s\"", Category.CODE_ERROR, 3002),

        /**
         * Can't handle spam because MailService isn't available
         */
        MAILSERVICE_MISSING("Can't handle spam because MailService isn't available", Category.CODE_ERROR, 3003),
        
        /**
         * Error while getting spamd provider from service: "%s"
         */
        ERROR_GETTING_SPAMD_PROVIDER("Error while getting spamd provider from service: \"%s\"", Category.CODE_ERROR, 3004);


        
        
        private final Category category;

        private final int detailNumber;

        private final String message;

        private Code(final String message, final Category category, final int detailNumber) {
            this.message = message;
            this.detailNumber = detailNumber;
            this.category = category;
        }

        public Category getCategory() {
            return category;
        }

        public String getMessage() {
            return message;
        }

        public int getNumber() {
            return detailNumber;
        }
    }
    
    private static final transient Object[] EMPTY_ARGS = new Object[0];
    
    private static final transient org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(SpamhandlerSpamassassinException.class);
    
    /**
     * For serialization
     */
    private static final long serialVersionUID = 8765339558941873978L;

    public SpamhandlerSpamassassinException(AbstractOXException cause) {
        super(cause);
    }

    public SpamhandlerSpamassassinException(final Code code) {
        this(code, EMPTY_ARGS);
    }
    
    public SpamhandlerSpamassassinException(final Code code, final Object... messageArgs) {
        this(code, null, messageArgs);
    }

    public SpamhandlerSpamassassinException(final Code code, final Throwable cause, final Object... messageArgs) {
        super(EnumComponent.MAIL, code.category, code.detailNumber, code.message, cause);
        super.setMessageArgs(messageArgs);
    }

    public SpamhandlerSpamassassinException(final Component component, final Category category, final int detailNumber, final String message, final Throwable cause) {
        super(component, category, detailNumber, message, cause);
    }

}
