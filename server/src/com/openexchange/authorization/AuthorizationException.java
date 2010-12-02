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

package com.openexchange.authorization;

import com.openexchange.exceptions.ErrorMessage;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.Component;

public class AuthorizationException extends AbstractOXException {

    private static final long serialVersionUID = 3522013540920856005L;

    private static final String STR_COMPONENT = "AUTHORIZATION";

    /**
     * The {@link Component} for authorization exception.
     */
    public static final Component AUTHORIZATION_COMPONENT = new Component() {

        private static final long serialVersionUID = -7217868089213025324L;

        public String getAbbreviation() {
            return STR_COMPONENT;
        }
    };

    private StackTraceElement[] elements;
    
    /**
     * Initializes a new {@link AuthorizationException}.
     * 
     * @param exc The cause
     */
    public AuthorizationException(final AbstractOXException exc) {
        super(exc);
    }

    /**
     * Initializes a new {@link AuthorizationException}.
     * 
     * @param message The message
     * @param cause The cause
     */
    public AuthorizationException(final String message, final AbstractOXException cause) {
        super(AUTHORIZATION_COMPONENT, message, cause);
    }

    /**
     * Initializes a new {@link AuthorizationException}.
     * 
     * @param category The category
     * @param detailNumber The detail number
     * @param message The message
     * @param cause The cause
     */
    public AuthorizationException(final Category category, final int detailNumber, final String message, final Throwable cause) {
        super(AUTHORIZATION_COMPONENT, category, detailNumber, message, cause);
    }

    /**
     * Initializes a new {@link AuthorizationException}.
     * 
     * @param message The error message
     * @param cause The cause
     * @param args The message arguments
     */
    public AuthorizationException(ErrorMessage message, Throwable cause, Object... args) {
        super(message, cause);
        setMessageArgs(args);
    }

    public StackTraceElement[] getElements() {
        return elements;
    }
    
    public void printStarterTrace(){
        if(elements == null) {
            System.err.println("No Stack Trace recorded");
            return;
        }
        for(final StackTraceElement element : elements) {
            System.err.println(element);
        }
    }
}
