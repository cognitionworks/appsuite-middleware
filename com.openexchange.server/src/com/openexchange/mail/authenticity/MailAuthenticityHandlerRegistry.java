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
 *    trademarks of the OX Software GmbH. group of companies.
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

package com.openexchange.mail.authenticity;

import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.annotation.SingletonService;
import com.openexchange.session.Session;

/**
 * {@link MailAuthenticityHandlerRegistry}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.0
 */
@SingletonService
public interface MailAuthenticityHandlerRegistry {

    /**
     * Tests if mail authenticity verification is <b>not</b> enabled for session-associated user
     *
     * @param session The user's session
     * @return <code>true</code> if disabled; otherwise <code>false</code> if enabled
     * @throws OXException If test fails
     */
    boolean isNotEnabledFor(Session session) throws OXException;

    /**
     * Tests if mail authenticity verification is enabled for session-associated user
     *
     * @param session The user's session
     * @return <code>true</code> if enabled; otherwise <code>false</code> if disbaled
     * @throws OXExceptionIf test fails
     */
    boolean isEnabledFor(Session session) throws OXException;

    /**
     * Gets the date threshold (the number of milliseconds since January 1, 1970, 00:00:00 GMT) that defines which messages to consider.
     * <p>
     * Only such messages shall be considered whose received date is equal to or greater than date threshold.
     *
     * @param session The user's session
     * @return The date threshold or <code>0</code> (zero)
     * @throws OXException
     */
    long getDateThreshold(Session session) throws OXException;

    /**
     * Gets a sorted listing of applicable handlers
     *
     * @param session The user's session
     * @return The sorted listing
     * @throws OXException If sorted listing cannot be returned
     */
    List<MailAuthenticityHandler> getSortedApplicableHandlersFor(Session session) throws OXException;

}
