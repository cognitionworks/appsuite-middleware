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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.chronos.provider.ical.exception;

import com.openexchange.i18n.LocalizableStrings;

/**
 * 
 * {@link ICalProviderExceptionMessages}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.10.0
 */
public class ICalProviderExceptionMessages implements LocalizableStrings {

    public static final String MISSING_FEED_URI_MSG = "The feed URI is missing.";

    public static final String BAD_FEED_URI_MSG = "The feed URI does not match the standard.";

    public static final String FEED_URI_NOT_ALLOWED_MSG = "Cannot connect to feed with URL: %1$s. Please change the URL and try again.";

    public static final String NO_FEED_MSG = "The provided URL %1$s does not contain content as expected. Please change the URL and try again.";

    public static final String NOT_ALLOWED_CHANGE_MSG = "The field %1$s cannot be changed.";

    public static final String BAD_PARAMETER_MSG = "The field '%1$s' contains an unexpected value '%2$s'";

    public static final String FEED_SIZE_EXCEEDED_MSG = "Unfortunately your requested feed cannot be subscribed due to size limitations.";

    public static final String UNEXPECTED_FEED_ERROR_MSG = "Unfortunately the given feed URL cannot be processed as expected.";

    public static final String REMOTE_SERVICE_UNAVAILABLE_MSG = "The remote service is unavailable at the moment. There is nothing we can do about it. Please try again later.";

    public static final String REMOTE_INTERNAL_SERVER_ERROR_MSG = "An internal server error occurred on the feed provider side. There is nothing we can do about it.";

    public static final String REMOTE_SERVER_ERROR_MSG = "A remote server error occurred on the feed provider side. There is nothing we can do about it.";

    /**
     * Initializes a new {@link ICalProviderExceptionMessages}.
     */
    private ICalProviderExceptionMessages() {
        super();
    }
}
