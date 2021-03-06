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

package com.openexchange.chronos.scheduling;

import java.util.Locale;
import java.util.TimeZone;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.regional.RegionalSettings;

/**
 * {@link RecipientSettings}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.3
 */
public interface RecipientSettings {

    /**
     * Gets the recipient.
     *
     * @return The recipient
     */
    CalendarUser getRecipient();

    /**
     * Gets the calendar user type of the recipient.
     *
     * @return The calendar user type of the recipient
     */
    CalendarUserType getRecipientType();

    /**
     * Gets a value indicating the preferred message format for the recipient.
     * <p>
     * The returned <code>int</code> value is wither <code>1</code> (text only), <code>2</code> (HTML only), or <code>3</code> (both).
     *
     * @return The desired message format
     * @see com.openexchange.mail.usersetting.UserSettingMail#getMsgFormat()
     */
    int getMsgFormat();

    /**
     * Gets the preferred locale to use for the recipient.
     *
     * @return The preferred locale
     */
    Locale getLocale();

    /**
     * Gets the preferred timezone to use for the recipient.
     *
     * @return The preferred timezone
     */
    TimeZone getTimeZone();

    /**
     * Gets customized regional settings to use for the recipient, if configured.
     *
     * @return The preferred regional settings, or <code>null</code> if not configured
     */
    RegionalSettings getRegionalSettings();

    /**
     * Gets a direct link to a specific event, from the recipient point of view.
     *
     * @param event The event to generate the link for
     * @return The direct link, or <code>null</code> if not applicable
     */
    String getDirectLink(Event event);

}
