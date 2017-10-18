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
 *     Copyright (C) 2017-2020 OX Software GmbH
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

package com.openexchange.chronos.schedjoules.impl;

import java.util.List;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.exception.OXException;
import com.openexchange.tools.id.IDMangler;

/**
 * {@link IDMangling}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class IDMangling {

    /** The fixed prefix used to quickly identify calendar folder identifiers. */
    private static final String CAL_PREFIX = "cal";

    /**
     * <i>Mangles</i> the supplied relative folder identifier, together with its corresponding account information.
     *
     * @param accountId The identifier of the account the folder originates in
     * @param relativeFolderId The relative folder identifier
     * @return The mangled folder identifier
     */
    public static String mangleFolderId(int accountId, String relativeFolderId) {
        return IDMangler.mangle(CAL_PREFIX, String.valueOf(accountId), relativeFolderId);
    }

    /**
     * Gets the relative representation of a specific unique composite folder identifier.
     * <p/>
     * {@link IDMangling#ROOT_FOLDER_IDS} are passed as-is implicitly.
     *
     * @param uniqueFolderId The unique composite folder identifier, e.g. <code>cal://3/1145</code>
     * @return The extracted relative folder identifier
     * @throws OXException {@link CalendarExceptionCodes#UNSUPPORTED_FOLDER} if passed identifier can't be unmangled to its relative representation
     */
    public static String getRelativeFolderId(String uniqueFolderId) throws OXException {
        try {
            return unmangleFolderId(uniqueFolderId).get(2);
        } catch (IllegalArgumentException e) {
            throw CalendarExceptionCodes.UNSUPPORTED_FOLDER.create(e, uniqueFolderId, null);
        }
    }

    /**
     * <i>Unmangles</i> the supplied unique folder identifier into its distinct components.
     *
     * @param uniqueFolderId The unique composite folder identifier, e.g. <code>cal://3/1145</code>
     * @return The unmangled components of the folder identifier
     * @throws IllegalArgumentException If passed identifier can't be unmangled into its distinct components
     */
    private static List<String> unmangleFolderId(String uniqueFolderId) {
        if (null == uniqueFolderId || false == uniqueFolderId.startsWith(CAL_PREFIX)) {
            throw new IllegalArgumentException(uniqueFolderId);
        }
        List<String> unmangled = IDMangler.unmangle(uniqueFolderId);
        if (null == unmangled || 3 > unmangled.size() || false == CAL_PREFIX.equals(unmangled.get(0))) {
            throw new IllegalArgumentException(uniqueFolderId);
        }
        return unmangled;
    }
}
