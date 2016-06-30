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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.chronos;

import java.util.List;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link UserizedEvent}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class UserizedEvent {

    private final int folderId;
    private final List<Alarm> alarms;
    private final Event event;
    private final ServerSession session;
    private final int onBehalfOf;

    /**
     * Initializes a new {@link UserizedEvent}.
     *
     * @param session The current user's session
     * @param onBehalfOf The identifier of the attendee the session's user is acting on behalf of, or <code>0</code> if not specified
     * @param folderId The folder identifier representing the view on the event
     * @param event The underlying event data
     * @param alarms The attendee's alarms for the event
     */
    public UserizedEvent(ServerSession session, int onBehalfOf, int folderId, Event event, List<Alarm> alarms) {
        super();
        this.session = session;
        this.event = event;
        this.onBehalfOf = onBehalfOf;
        this.folderId = folderId;
        this.alarms = alarms;
    }

    /**
     * Gets the folder identifier representing the view on the event
     *
     * @return The folder identifier
     */
    public int getFolderId() {
        return folderId;
    }

    /**
     * Gets the attendee's alarms for the event
     *
     * @return The alarms
     */
    public List<Alarm> getAlarms() {
        return alarms;
    }

    /**
     * Gets the underlying event data
     *
     * @return The event data
     */
    public Event getEvent() {
        return event;
    }

    /**
     * Gets the current user's session
     *
     * @return The session
     */
    public ServerSession getSession() {
        return session;
    }

    /**
     * Gets the identifier of the attendee the session's user is acting on behalf of.
     *
     * @return The identifier of the attendee the session's user is acting on behalf of, or <code>0</code> if not specified
     */
    public int getOnBehalfOf() {
        return onBehalfOf;
    }

}
