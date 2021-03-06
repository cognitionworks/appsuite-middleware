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

package com.openexchange.sessiond.impl.util;

import java.util.Collections;
import java.util.List;
import com.openexchange.sessiond.impl.container.SessionControl;

/**
 * {@link RotateShortResult} - The result of invoking {@code rotateShort()} from <code>com.openexchange.sessiond.impl.SessionData</code>
 * providing a listing of <b>non-transient</b> sessions for:
 * <ul>
 * <li>Sessions moved to long-term container</li>
 * <li>Timed out sessions</li>
 * </ul>
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.3
 */
public class RotateShortResult {

    private final List<SessionControl> movedToLongTerm;
    private final List<SessionControl> removed;

    /**
     * Initializes a new {@link RotateShortResult}.
     *
     * @param movedToLongTerm A listing of sessions moved to long-term container or <code>null</code>
     * @param removed A listing of timed out sessions or <code>null</code>
     */
    public RotateShortResult(List<SessionControl> movedToLongTerm, List<SessionControl> removed) {
        super();
        this.movedToLongTerm = movedToLongTerm == null ? Collections.emptyList() : movedToLongTerm;
        this.removed = removed == null ? Collections.emptyList() : removed;
    }

    /**
     * Gets the <b>non-transient</b> sessions, which were moved to long-term container.
     *
     * @return The <b>non-transient</b>sessions, which were moved to long-term container
     */
    public List<SessionControl> getMovedToLongTerm() {
        return movedToLongTerm;
    }

    /**
     * Gets the <b>non-transient</b> sessions, which were removed
     *
     * @return The <b>non-transient</b> sessions, which were removed
     */
    public List<SessionControl> getRemoved() {
        return removed;
    }

    /**
     * Checks if both collections are empty.
     *
     * @return <code>true</code> if both are empty; otherwise <code>false</code>
     */
    public boolean isEmpty() {
        return movedToLongTerm.isEmpty() && removed.isEmpty();
    }
}
