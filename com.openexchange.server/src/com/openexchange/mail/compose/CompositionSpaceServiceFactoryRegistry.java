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

package com.openexchange.mail.compose;

import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.annotation.SingletonService;
import com.openexchange.session.Session;

/**
 * {@link CompositionSpaceServiceFactoryRegistry} - A registry for composition space service factories.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.5
 */
@SingletonService
public interface CompositionSpaceServiceFactoryRegistry {

    /**
     * Gets the highest-ranked composition space service factory for session-associated user.
     *
     * @param session The session providing user information
     * @return The highest-ranked composition space service factory
     * @throws OXException If highest-ranked composition space service factory cannot be returned
     */
    CompositionSpaceServiceFactory getHighestRankedFactoryFor(Session session) throws OXException;

    /**
     * Gets the composition space service factories for session-associated user.
     *
     * @param session The session providing user information
     * @return The composition space service factories ordered by ranking
     * @throws OXException If composition space service factories cannot be returned
     */
    List<CompositionSpaceServiceFactory> getFactoriesFor(Session session) throws OXException;

    /**
     * Gets the composition space service factory associated with given identifier for session-associated user.
     *
     * @param serviceId The service identifier
     * @param session The session providing user information
     * @return The composition space service factory
     * @throws OXException If composition space service factory cannot be returned
     */
    CompositionSpaceServiceFactory getFactoryFor(String serviceId, Session session) throws OXException;

}
