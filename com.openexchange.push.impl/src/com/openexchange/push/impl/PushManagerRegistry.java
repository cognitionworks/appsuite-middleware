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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.push.impl;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import com.openexchange.exception.OXException;
import com.openexchange.push.PushListener;
import com.openexchange.push.PushListenerService;
import com.openexchange.push.PushManagerExtendedService;
import com.openexchange.push.PushManagerService;
import com.openexchange.push.PushUtility;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link PushManagerRegistry} - The push manager registry.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class PushManagerRegistry implements PushListenerService {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(PushManagerRegistry.class);

    /** The <code>PushManagerRegistry</code> instance */
    private static volatile PushManagerRegistry instance;

    /**
     * Initializes push manager registry.
     */
    public static synchronized void init(ServiceLookup services) {
        if (null == instance) {
            instance = new PushManagerRegistry(services);
        }
    }

    /**
     * Shuts down push manager registry.
     */
    public static synchronized void shutdown() {
        instance = null;
    }

    /**
     * Gets the push manager registry.
     *
     * @return The push manager registry
     */
    public static PushManagerRegistry getInstance() {
        return instance;
    }

    /*-
     * --------------------------------------------------------- Member section ----------------------------------------------------------
     */

    private final ConcurrentMap<Class<? extends PushManagerService>, PushManagerService> map;
    private final ServiceLookup services;

    /**
     * Initializes a new {@link PushManagerRegistry}.
     *
     * @param services
     */
    private PushManagerRegistry(ServiceLookup services) {
        super();
        this.services = services;
        map = new ConcurrentHashMap<Class<? extends PushManagerService>, PushManagerService>();
    }

    @Override
    public boolean[] hasListenerFor(int[] userIds, int contextId) {
        boolean[] ret = new boolean[userIds.length];
        Arrays.fill(ret, false);

        // Iterate push managers
        for (Iterator<PushManagerService> pushManagersIterator = getPushManagers(); pushManagersIterator.hasNext();) {
            PushManagerService pushManager = pushManagersIterator.next();
            if (pushManager instanceof PushManagerExtendedService) {
                PushManagerExtendedService serviceExtended = (PushManagerExtendedService) pushManager;

                boolean[] result = serviceExtended.hasListenerFor(contextId, userIds);
                for (int i = 0; i < result.length; i++) {
                    ret[i] |= result[i];
                }
            }
        }

        return ret;
    }

    @Override
    public boolean registerPermanentListenerFor(int userId, int contextId) throws OXException {
        return PushDbUtils.insertPushRegistration(userId, contextId);
    }

    @Override
    public boolean unregisterPermanentListenerFor(int userId, int contextId) throws OXException {
        return PushDbUtils.deletePushRegistration(userId, contextId);
    }

    @Override
    public PushListener startListenerFor(Session session) {
        /*
         * Check session's client identifier
         */
        if (!PushUtility.allowedClient(session.getClient())) {
            /*
             * No push listener for the client associated with current session.
             */
            return null;
        }
        /*
         * Iterate push managers
         */
        for (Iterator<PushManagerService> pushManagersIterator = map.values().iterator(); pushManagersIterator.hasNext();) {
            try {
                PushManagerService pushManager = pushManagersIterator.next();
                // Initialize a new push listener for session
                PushListener pl = pushManager.startListener(session);
                if (null != pl) {
                    LOG.debug("Started push listener for user {} in context {} by push manager \"{}\"", Integer.valueOf(session.getUserId()), Integer.valueOf(session.getContextId()), pushManager);
                    return pl;
                }
            } catch (OXException e) {
                LOG.error("Push error while starting push listener.", e);
            } catch (RuntimeException e) {
                LOG.error("Runtime error while starting push listener.", e);
            }
        }
        return null;
    }

    @Override
    public boolean stopListenerFor(Session session) {
        if (!PushUtility.allowedClient(session.getClient())) {
            /*
             * No push listener for the client associated with current session.
             */
            return false;
        }
        /*
         * Iterate push managers
         */
        for (Iterator<PushManagerService> pushManagersIterator = map.values().iterator(); pushManagersIterator.hasNext();) {
            try {
                PushManagerService pushManager = pushManagersIterator.next();
                // Stop listener for session
                boolean stopped = pushManager.stopListener(session);
                if (stopped) {
                    LOG.debug("Stopped push listener for user {} in context {} by push manager \"{}\"", Integer.valueOf(session.getUserId()), Integer.valueOf(session.getContextId()), pushManager);
                    return true;
                }
            } catch (OXException e) {
                LOG.error("Push error while stopping push listener.", e);
            } catch (RuntimeException e) {
                LOG.error("Runtime error while stopping push listener.", e);
            }
        }
        return false;
    }

    /**
     * Adds specified push manager service.
     *
     * @param pushManager The push manager service to add
     * @return <code>true</code> if push manager service could be successfully added; otherwise <code>false</code>
     */
    public boolean addPushManager(final PushManagerService pushManager) {
        Class<? extends PushManagerService> clazz = pushManager.getClass();
        return null == map.putIfAbsent(clazz, pushManager);
    }

    /**
     * Removes specified push manager service.
     *
     * @param pushManager The push manager service to remove
     */
    public void removePushManager(final PushManagerService pushManager) {
        map.remove(pushManager.getClass());
    }

    /**
     * Gets a read-only {@link Iterator iterator} over the push managers in this registry.
     * <p>
     * Invoking {@link Iterator#remove() remove} will throw an {@link UnsupportedOperationException}.
     *
     * @return A read-only {@link Iterator iterator} over the push managers in this registry.
     */
    public Iterator<PushManagerService> getPushManagers() {
        return unmodifiableIterator(map.values().iterator());
    }

    // -----------------------------------------------------------------------------------------------------------------------------

    /**
     * Strips the <tt>remove()</tt> functionality from an existing iterator.
     * <p>
     * Wraps the supplied iterator into a new one that will always throw an <tt>UnsupportedOperationException</tt> if its <tt>remove()</tt>
     * method is called.
     *
     * @param iterator The iterator to turn into an unmodifiable iterator.
     * @return An iterator with no remove functionality.
     */
    private static <T> Iterator<T> unmodifiableIterator(final Iterator<T> iterator) {
        if (iterator == null) {
            @SuppressWarnings("unchecked") final Iterator<T> empty = EMPTY_ITER;
            return empty;
        }

        return new Iterator<T>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @SuppressWarnings("rawtypes")
    private static final Iterator EMPTY_ITER = new Iterator() {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            return null;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    };

}
