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

package com.openexchange.messaging.facebook.session;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.openexchange.messaging.MessagingException;
import com.openexchange.messaging.facebook.services.FacebookMessagingServiceRegistry;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.tools.Collections;

/**
 * {@link FacebookSessionRegistry}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since Open-Xchange v6.16
 */
public final class FacebookSessionRegistry {

    private static final FacebookSessionRegistry INSTANCE = new FacebookSessionRegistry();

    /**
     * Gets the registry instance.
     * 
     * @return The registry instance
     */
    public static FacebookSessionRegistry getInstance() {
        return INSTANCE;
    }

    private final ConcurrentMap<SimpleKey, FacebookSession> map;

    /**
     * Initializes a new {@link FacebookSessionRegistry}.
     */
    private FacebookSessionRegistry() {
        super();
        map = new ConcurrentHashMap<SimpleKey, FacebookSession>();
    }

    /**
     * Closes all sessions contained in this registry.
     */
    public void closeAll() {
        for (final Iterator<FacebookSession> i = map.values().iterator(); i.hasNext();) {
            i.next().close();
        }
    }

    /**
     * Opens all sessions contained in this registry.
     */
    public void openAll() {
        for (final Iterator<FacebookSession> i = map.values().iterator(); i.hasNext();) {
            final FacebookSession ses = i.next();
            try {
                ses.connect();
            } catch (final MessagingException e) {
                org.apache.commons.logging.LogFactory.getLog(FacebookSessionRegistry.class).error(
                    MessageFormat.format("Connecting facebook session failed. Removing session from registry: {0}", ses.toString()),
                    e);
                i.remove();
            }
        }
    }

    /**
     * Adds specified facebook session.
     * 
     * @param contextId The context identifier
     * @param userId The user identifier
     * @param facebookSession The facebook session to add
     * @return The previous associated session, or <code>null</code> if there was no session.
     */
    public FacebookSession addSession(final int contextId, final int userId, final FacebookSession facebookSession) {
        return map.putIfAbsent(SimpleKey.valueOf(contextId, userId), facebookSession);
    }

    /**
     * Check presence of the facebook session associated with given user-context-pair.
     * 
     * @param contextId The context identifier
     * @param userId The user identifier
     * @return <code>true</code> if such a facebook session is present; otherwise <code>false</code>
     */
    public boolean containsSession(final int contextId, final int userId) {
        return map.containsKey(SimpleKey.valueOf(contextId, userId));
    }

    /**
     * Gets the facebook session associated with given user-context-pair.
     * 
     * @param contextId The context identifier
     * @param userId The user identifier
     * @return The facebook session or <code>null</code>
     */
    public FacebookSession getSession(final int contextId, final int userId) {
        return map.get(SimpleKey.valueOf(contextId, userId));
    }

    /**
     * Removes specified session identifier associated with given user-context-pair and the facebook session as well, if no more
     * user-associated session identifiers are present.
     * 
     * @param contextId The context identifier
     * @param userId The user identifier
     * @return <code>true</code> if a facebook session for given user-context-pair was found and removed; otherwise <code>false</code>
     */
    public boolean removeSessionIfLast(final int contextId, final int userId) {
        final SessiondService sessiondService = FacebookMessagingServiceRegistry.getServiceRegistry().getService(SessiondService.class);
        if (null == sessiondService || 0 == sessiondService.getUserSessions(userId, contextId)) {
            return removeSession(SimpleKey.valueOf(contextId, userId));
        }
        return false;
    }

    /**
     * Purges specified user's facebook sessions.
     * 
     * @param contextId The context identifier
     * @param userId The user identifier
     * @return <code>true</code> if a facebook session for given user-context-pair was found and purged; otherwise <code>false</code>
     */
    public boolean purgeUserSession(final int contextId, final int userId) {
        return removeSession(SimpleKey.valueOf(contextId, userId));
    }

    private boolean removeSession(final SimpleKey key) {
        final FacebookSession ses = map.remove(key);
        if (null != ses) {
            ses.close();
            return true;
        }
        return false;
    }

    /**
     * Gets a read-only {@link Iterator iterator} over the facebook sessions in this registry.
     * <p>
     * Invoking {@link Iterator#remove() remove} will throw an {@link UnsupportedOperationException}.
     * 
     * @return A read-only {@link Iterator iterator} over the facebook sessions in this registry.
     */
    public Iterator<FacebookSession> getSessions() {
        return Collections.unmodifiableIterator(map.values().iterator());
    }

    private static final class SimpleKey {

        public static SimpleKey valueOf(final int cid, final int user) {
            return new SimpleKey(cid, user);
        }

        final int cid;

        final int user;

        private final int hash;

        private SimpleKey(final int cid, final int user) {
            super();
            this.cid = cid;
            this.user = user;
            // hash code
            final int prime = 31;
            int result = 1;
            result = prime * result + cid;
            result = prime * result + user;
            hash = result;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SimpleKey)) {
                return false;
            }
            final SimpleKey other = (SimpleKey) obj;
            if (cid != other.cid) {
                return false;
            }
            if (user != other.user) {
                return false;
            }
            return true;
        }
    } // End of SimpleKey

}
