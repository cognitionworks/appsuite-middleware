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

package com.openexchange.sessiond;

import static com.openexchange.sessiond.SessionExceptionMessages.*;
import com.openexchange.exceptions.OXErrorMessage;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.sessiond.exception.SessionExceptionFactory;

/**
 * {@link SessionExceptionCodes}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public enum SessionExceptionCodes implements OXErrorMessage {

    SESSIOND_EXCEPTION(SESSIOND_EXCEPTION_MSG, Category.CODE_ERROR, 1),

    MAX_SESSION_EXCEPTION(MAX_SESSION_EXCEPTION_MSG, Category.CODE_ERROR, 2),

    SESSIOND_CONFIG_EXCEPTION(SESSIOND_CONFIG_EXCEPTION_MSG, Category.CODE_ERROR, 3),

    MISSING_PROPERTY(MISSING_PROPERTY_MSG, Category.SETUP_ERROR, 4),

    /** Unknown event topic %s */
    UNKNOWN_EVENT_TOPIC(UNKNOWN_EVENT_TOPIC_MSG, Category.CODE_ERROR, 5),

    /** Password could not be changed */
    PASSWORD_UPDATE_FAILED(PASSWORD_UPDATE_FAILED_MSG, Category.CODE_ERROR, 6),

    /** Max. session size for user %1$s in context %2$s exceeded */
    MAX_SESSION_PER_USER_EXCEPTION(MAX_SESSION_PER_USER_EXCEPTION_MSG, Category.CODE_ERROR, 7),

    /** Found duplicate used authentication identifier. Login of existing session: %1$s. Current denied login request: %2$s. */
    DUPLICATE_AUTHID(DUPLICATE_AUTHID_MSG, Category.CODE_ERROR, 8),
    /** SessionD returned wrong session with identifier %1$s for given session identifier %2$s. */
    WRONG_SESSION(WRONG_SESSION_MSG, Category.CODE_ERROR, 9),
    /** Got a collision while adding a new session to the session container. Colliding session has login %1$s and new session has login %2$s. */
    SESSIONID_COLLISION(SESSIONID_COLLISION_MSG, Category.CODE_ERROR, 10),
    /** Received wrong session %1$s having random %2$s when looking for random %3$s and session %4$s. */
    WRONG_BY_RANDOM(WRONG_BY_RANDOM_MSG, Category.CODE_ERROR, 11),

    SESSION_PARAMETER_MISSING(SESSION_PARAMETER_MISSING_MSG, Category.CODE_ERROR, 201, "Every AJAX request must contain a parameter named \"session\" that value contains the identifier of the session cookie."),

    SESSION_EXPIRED(SESSION_EXPIRED_MSG, Category.TRY_AGAIN, 203, "A session with the given identifier can not be found."),

    CONTEXT_LOCKED(CONTEXT_LOCKED_MSG, Category.TRY_AGAIN, 204),

    WRONG_CLIENT_IP(WRONG_CLIENT_IP_MSG, Category.PERMISSION, 205, "The client IP address of all session requests are checked to see whether they match the IP address when the session was created."),

    WRONG_SESSION_SECRET(WRONG_SESSION_SECRET_MSG, Category.TRY_AGAIN, 206);

    private final String message;
    private final Category category;
    private final int number;
    private final String help;

    private SessionExceptionCodes(final String message, final Category category, final int number) {
        this(message, category, number, null);
    }

    private SessionExceptionCodes(final String message, final Category category, final int number, final String help) {
        this.message = message;
        this.category = category;
        this.number = number;
        this.help = help;
    }

    public int getDetailNumber() {
        return number;
    }

    public String getMessage() {
        return message;
    }

    public String getHelp() {
        return help;
    }

    public Category getCategory() {
        return category;
    }

    public SessiondException create(final Object... messageArgs) {
        return SessionExceptionFactory.getInstance().create(this, messageArgs);
    }

    public SessiondException create(final Throwable cause, final Object... messageArgs) {
        return SessionExceptionFactory.getInstance().create(this, cause, messageArgs);
    }
}
