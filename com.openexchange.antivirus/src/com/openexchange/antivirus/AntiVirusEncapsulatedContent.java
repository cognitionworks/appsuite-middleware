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

package com.openexchange.antivirus;

import java.util.Map;
import java.util.Optional;

/**
 * {@link AntiVirusEncapsulatedContent} - Used to encapsulate additional HTTP content, such
 * as the original HTTP request/response headers to the ICAP request.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.5
 * @see <a href="https://tools.ietf.org/html/rfc3507#section-4.4">RFC-3507, Section 4.4</a>
 */
public interface AntiVirusEncapsulatedContent {

    /**
     * Returns the optional original HTTP request, e.g. <code>GET /api/files/someFile.jpg HTTP/1.1</code>
     *
     * @return the optional original HTTP request
     */
    Optional<String> getOriginalRequest();

    /**
     * Returns a map with the original request headers
     *
     * @return a map with the original request headers
     *         or an empty map
     */
    Map<String, String> getOriginalRequestHeaders();

    /**
     * Returns (if available) the original response's status line,
     * e.g. <code>HTTP/1.1 200 OK</code>
     *
     * @return The optional response's status line
     */
    Optional<String> getOriginalResponseLine();

    /**
     * Returns the optional original response's headers
     *
     * @return The optional original response's headers
     *         or an empty map
     */
    Map<String, String> getOriginalResponseHeaders();
}
