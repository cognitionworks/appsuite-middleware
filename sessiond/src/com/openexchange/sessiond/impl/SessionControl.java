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

package com.openexchange.sessiond.impl;

import com.openexchange.session.Session;

/**
 * {@link SessionControl} - Holds a {@link Session} instance and remembers life-cycle timestamps such as last-accessed, creation-time, etc.
 * 
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class SessionControl {

    /**
     * Time stamp when this session control was created.
     */
    private long creationTime;

    /**
     * The associated session.
     */
    private Session session;

    /**
     * Initializes a new {@link SessionControl}
     * 
     * @param session The stored session
     */
    public SessionControl(final Session session) {
        super();
        renew0(session);
    }

    /**
     * (Atomically) Renews this session control:
     * <ul>
     * <li>Applies specified session</li>
     * <li>Sets specified life time</li>
     * <li>Sets creation time stamp to current time millis</li>
     * <li>Sets last-accessed time stamp to current time millis</li>
     * </ul>
     */
    public void renew(final Session sessionParam) {
        synchronized (this) {
            renew0(sessionParam);
        }
    }

    private void renew0(final Session sessionParam) {
        this.session = sessionParam;
        creationTime = System.currentTimeMillis();
    }

    /**
     * Gets the stored session
     * 
     * @return The stored session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Gets the creation-time timestamp
     * 
     * @return The creation-time timestamp
     */
    public long getCreationTime() {
        return creationTime;
    }
}
