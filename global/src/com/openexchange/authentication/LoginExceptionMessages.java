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

package com.openexchange.authentication;

import com.openexchange.exceptions.LocalizableStrings;

/**
 * Exception messages for the {@link LoginException} that must be translated.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class LoginExceptionMessages implements LocalizableStrings {

    // The authentication mechanism is completely replaceable. Some hoster need
    // to ban users. This message is used therefore.
    // %s is replaced with some login name.
    public static final String ACCOUNT_LOCKED_MSG = "Account \"%s\" is locked.";

    // Provisioning of some account may take some time although the login is
    // already possible. If creating the account has not finished on OX side the
    // login mechanism can use this message to prevent the login.
    // %s is replaced with some login name.
    public static final String ACCOUNT_NOT_READY_YET_MSG = "Account \"%s\" is not ready yet.";

    // If the problem could not be specified in some more detailed way this
    // message can be used.
    // %s is replaced by some own message that will not be translated.
    public static final String UNKNOWN_MSG = "Unknown problem: \"%s\".";

    // This message can be used if the authentication systems are not reachable.
    // The customer should try some time later again.
    public static final String COMMUNICATION_MSG = "Login not possible at the moment. Please try again later.";

    // The supplied credentials for the authentication are invalid.
    public static final String INVALID_CREDENTIALS_MSG = "Invalid credentials.";

    // The instantiation of the login implementing class has failed. This will
    // not occur anymore since SP4 release.
    public static final String INSTANTIATION_FAILED_MSG = "Instantiating the class failed.";

    // The classes are not named anymore anywhere. So this will not occur
    // anymore since SP4 release.
    public static final String CLASS_NOT_FOUND_MSG = "Class %1$s can not be found.";

    // This message can be used if the configuration of the authentication
    // mechanism is not complete and some configuration option is missing.
    // %1$s is replaced with the name of the missing configuration option.
    public static final String MISSING_PROPERTY_MSG = "Missing property %1$s.";

    // If the authentication mechanism uses a database this message can be used
    // if the database can not be read for authenticating the user.
    public static final String DATABASE_DOWN_MSG = "Database down.";

    /**
     * Prevent instantiation.
     */
    private LoginExceptionMessages() {
        super();
    }
}
