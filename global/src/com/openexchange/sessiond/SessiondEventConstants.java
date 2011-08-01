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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.sessiond;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;

/**
 * {@link SessiondEventConstants} - Provides constants for {@link EventConstants#EVENT_TOPIC event topic} and property names accessible by
 * an {@link Event event}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SessiondEventConstants {

    private SessiondEventConstants() {
        super();
    }

    /**
     * The topic on single session removal.
     */
    public static final String TOPIC_REMOVE_SESSION = "com/openexchange/sessiond/remove/session";

    /**
     * The topic on session container removal.
     */
    public static final String TOPIC_REMOVE_CONTAINER = "com/openexchange/sessiond/remove/container";

    /**
     * This event topic is used when sessions walk into the long term session life time container. If this event is emitted all temporary
     * session data should be removed. A complete UI reload is suggested to get the session back out of the long term life time container or
     * at first, we expect that to reduce the amount of used memory.
     */
    public static final String TOPIC_REMOVE_DATA = "com/openexchange/sessiond/remove/data";

    /**
     * This event topic is used when a session is reactivated from the long term session life time container. Background tasks for the
     * session can be reactivated on this event again.
     */
    public static final String TOPIC_REACTIVATE_SESSION = "com/openexchange/sessiond/reactivate/session";

    /**
     * The topic on single session creation.
     */
    public static final String TOPIC_ADD_SESSION = "com/openexchange/sessiond/add/session";

    /**
     * An array of {@link String string} including all known topics.
     * <p>
     * Needed on event handler registration to a bundle context.
     */
    private static final String[] TOPICS = { TOPIC_REMOVE_SESSION, TOPIC_REMOVE_CONTAINER, TOPIC_REMOVE_DATA, TOPIC_ADD_SESSION };

    /**
     * Gets an array of {@link String string} including all known topics.
     * <p>
     * Needed on event handler registration to a bundle context.
     *
     * @return An array of {@link String string} including all known topics.
     */
    public static String[] getAllTopics() {
        final String[] retval = new String[TOPICS.length];
        System.arraycopy(TOPICS, 0, retval, 0, TOPICS.length);
        return retval;
    }

    /**
     * The property for a single session kept in event's properties.
     * <p>
     * Target object is an instance of <tt>com.openexchange.session.Session</tt>.
     */
    public static final String PROP_SESSION = "com.openexchange.sessiond.session";

    /**
     * The property for a session container kept in event's properties.
     * <p>
     * Target object is an instance of <tt>java.util.Map&lt;String, Session&gt;</tt>.
     */
    public static final String PROP_CONTAINER = "com.openexchange.sessiond.container";

}
