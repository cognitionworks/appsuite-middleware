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

package com.openexchange.subscribe.microformats;

import com.openexchange.exceptions.OXErrorMessage;
import com.openexchange.groupware.AbstractOXException.Category;


/**
 * {@link OXMFSubscriptionErrorMessage}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 *
 */
public enum OXMFSubscriptionErrorMessage implements OXErrorMessage{

    /**
     * A parsing error occurred: %1$s.
     */
    ParseException(Category.CODE_ERROR, 2, "Provide well-formed HTML.", OXMFErrorStrings.PARSE_EXCEPTION),
    IOException(Category.SUBSYSTEM_OR_SERVICE_DOWN, 3, "Try again later.", OXMFErrorStrings.IOException),
    HttpException(Category.SUBSYSTEM_OR_SERVICE_DOWN, 4, "Try again later.", OXMFErrorStrings.HttpException), 
    CAN_ONLY_POST_TO_EXTERNAL_SUBSCRIPTION_SOURCES(Category.INTERNAL_ERROR, 5,"Try again later.", OXMFErrorStrings.CAN_ONLY_POST_TO_EXTERNAL_SUBSCRIPTION_SOURCES),
    ERROR_LOADING_SUBSCRIPTION(Category.SUBSYSTEM_OR_SERVICE_DOWN, 6, "Try again later.", OXMFErrorStrings.ERROR_LOADING_SUBSCRIPTION), 
    ;
    
    private Category category;
    private int errorCode;
    private String help;
    private String message;
    
    public static final OXMFSubscriptionExceptionFactory EXCEPTIONS = new OXMFSubscriptionExceptionFactory();
    
    private OXMFSubscriptionErrorMessage(final Category category, final int errorCode, final String help, final String message) {
        this.category = category;
        this.errorCode = errorCode;
        this.help = help;
        this.message = message;
    }
    
    public Category getCategory() {
        return category;
    }

    public int getDetailNumber() {
        return errorCode;
    }

    public String getHelp() {
        return help;
    }

    public String getMessage() {
        return message;
    }
    
    public OXMFSubscriptionException create(final Throwable cause, final Object...args) {
        return EXCEPTIONS.create(this,cause, args);
    }
    
    public OXMFSubscriptionException create(final Object...args) {
        return EXCEPTIONS.create(this,args);
    }
}
