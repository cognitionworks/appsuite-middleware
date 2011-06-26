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

package com.openexchange.imap.notify.internal;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import com.openexchange.imap.notify.IMAPNotifierRegistryService;
import com.openexchange.imap.services.IMAPServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondService;

/**
 * {@link IMAPNotifierRegistry} - The registry for {@link IMAPNotifierTask notifier tasks}.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class IMAPNotifierRegistry implements IMAPNotifierConstants, IMAPNotifierRegistryService {

    private static final IMAPNotifierRegistryService INSTANCE = new IMAPNotifierRegistry();

    /**
     * Gets the registry instance.
     * 
     * @return The instance
     */
    public static IMAPNotifierRegistryService getInstance() {
        return INSTANCE;
    }

    private final ConcurrentMap<Key, ConcurrentMap<Integer, IMAPNotifierTask>> map;

    /**
     * Initializes a new {@link IMAPNotifierRegistry}.
     */
    private IMAPNotifierRegistry() {
        super();
        map = new ConcurrentHashMap<Key, ConcurrentMap<Integer, IMAPNotifierTask>>();
    }

    /*-
     * --------------------------------- Member stuff ----------------------------------
     */

    private static final Pattern SPLIT = Pattern.compile(" *, *");

    public boolean addTaskFor(final int accountId, final Session session) {
        /*
         * Check for available full names
         */
        final String[] fullNames;
        {
            final String notifierFullNames = NOTIFIER_FULL_NAMES;
            if (isEmpty(notifierFullNames)) {
                return false;
            }
            fullNames = SPLIT.split(notifierFullNames);
        }
        /*
         * Thread-safe start-up of a _single_ notifier task
         */
        final Key key = keyFor(session);
        ConcurrentMap<Integer, IMAPNotifierTask> tasks = map.get(key);
        if (null == tasks) {
            final ConcurrentMap<Integer, IMAPNotifierTask> newtasks = new ConcurrentHashMap<Integer, IMAPNotifierTask>();
            tasks = map.putIfAbsent(key, newtasks);
            if (null == tasks) {
                tasks = newtasks;
            }
        }
        final Integer accKey = Integer.valueOf(accountId);
        IMAPNotifierTask task = tasks.get(accKey);
        if (null != task) {
            /*
             * Already present
             */
            return false;
        }
        final IMAPNotifierTask newtask = new IMAPNotifierTask(accountId, session);
        task = tasks.putIfAbsent(accKey, newtask);
        if (null != task) {
            /*
             * Another thread won the put-if-absent call
             */
            return false;
        }
        task = newtask.addFullNames(fullNames);
        return task.startUp();
    }

    private static boolean isEmpty(final String str) {
        if (null == str) {
            return true;
        }
        final char[] chars = str.toCharArray();
        boolean empty = true;
        for (int i = 0; empty && i < chars.length; i++) {
            empty = Character.isWhitespace(chars[i]);
        }
        return empty;
    }

    public boolean containsTaskFor(final Session session) {
        return map.containsKey(keyFor(session));
    }

    public void removeTaskFor(final Session session) {
        final ConcurrentMap<Integer, IMAPNotifierTask> tasks = map.remove(keyFor(session));
        if (null == tasks) {
            return;
        }
        for (final Iterator<IMAPNotifierTask> it = tasks.values().iterator(); it.hasNext();) {
            it.next().shutDown();
        }
    }

    /*-
     * ------------------------------ EventHandler stuff ------------------------------
     */

    public void handleRemovedSession(final Session session) {
        final SessiondService service = IMAPServiceRegistry.getService(SessiondService.class);
        if (null == service || service.getUserSessions(session.getUserId(), session.getContextId()) <= 0) {
            removeTaskFor(session);
        }
    }

    /*-
     * ------------------------- Key class -----------------------
     */

    private static Key keyFor(final Session session) {
        return new Key(session.getUserId(), session.getContextId());
    }

    private static final class Key {

        private final int user;

        private final int context;

        private final int hash;

        protected Key(final int user, final int context) {
            super();
            this.user = user;
            this.context = context;
            hash = hashCode0();
        }

        private int hashCode0() {
            final int prime = 31;
            int result = 1;
            result = prime * result + context;
            result = prime * result + user;
            return result;
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
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Key other = (Key) obj;
            if (context != other.context) {
                return false;
            }
            if (user != other.user) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder(16);
            builder.append("{ Key [user=").append(user).append(", context=").append(context).append("] }");
            return builder.toString();
        }
    }

}
