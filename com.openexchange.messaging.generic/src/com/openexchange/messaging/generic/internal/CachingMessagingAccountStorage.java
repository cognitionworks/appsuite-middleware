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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.messaging.generic.internal;

import gnu.trove.list.TIntList;
import gnu.trove.procedure.TIntProcedure;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheKey;
import com.openexchange.caching.CacheService;
import com.openexchange.caching.dynamic.OXObjectFactory;
import com.openexchange.exception.OXException;
import com.openexchange.messaging.MessagingAccount;
import com.openexchange.messaging.MessagingService;
import com.openexchange.messaging.generic.services.MessagingGenericServiceRegistry;
import com.openexchange.session.Session;

/**
 * {@link CachingMessagingAccountStorage} - The messaging account manager backed by {@link CacheService}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since Open-Xchange v6.16
 */
public final class CachingMessagingAccountStorage implements MessagingAccountStorage {

    private static final CachingMessagingAccountStorage INSTANCE = new CachingMessagingAccountStorage();

    private static final String REGION_NAME = "MessagingAccount";

    /**
     * Gets the cache region name.
     *
     * @return The cache region name
     */
    public static String getRegionName() {
        return REGION_NAME;
    }

    /**
     * Gets the cache-backed instance.
     *
     * @return The cache-backed instance
     */
    public static CachingMessagingAccountStorage getInstance() {
        return INSTANCE;
    }

    /**
     * Generates a new cache key.
     *
     * @return The new cache key
     */
    static CacheKey newCacheKey(final CacheService cacheService, final String serviceId, final int id, final int user, final int cid) {
        return cacheService.newCacheKey(cid, serviceId, Integer.valueOf(id), Integer.valueOf(user));
    }

    private static void invalidateMessagingAccount(final String serviceId, final int id, final int user, final int cid) throws OXException {
        final CacheService cacheService = MessagingGenericServiceRegistry.getServiceRegistry().getService(CacheService.class);
        if (null != cacheService) {
            try {
                final Cache cache = cacheService.getCache(REGION_NAME);
                cache.remove(newCacheKey(cacheService, serviceId, id, user, cid));
            } catch (final OXException e) {
                throw new OXException(e);
            }
        }
    }

    /*-
     * ------------------------------ Member section ------------------------------
     */

    /**
     * The database-backed delegatee.
     */
    private final RdbMessagingAccountStorage delegatee;

    /**
     * Lock for the cache.
     */
    private final Lock cacheLock;

    /**
     * Initializes a new {@link CachingMessagingAccountStorage}.
     */
    private CachingMessagingAccountStorage() {
        super();
        delegatee = RdbMessagingAccountStorage.getInstance();
        cacheLock = new ReentrantLock(true);
    }

    /**
     * Invalidates specified account.
     *
     * @param serviceId The service identifier
     * @param id The account identifier
     * @param user The user identifier
     * @param contextId The context identifier
     * @throws OXException If invalidation fails
     */
    public void invalidate(final String serviceId, final int id, final int user, final int contextId) throws OXException {
        invalidateMessagingAccount(serviceId, id, user, contextId);
    }

    @Override
    public int addAccount(final String serviceId, final MessagingAccount account, final Session session, final Modifier modifier) throws OXException {
        return delegatee.addAccount(serviceId, account, session, modifier);
    }

    @Override
    public void deleteAccount(final String serviceId, final MessagingAccount account, final Session session, final Modifier modifier) throws OXException {
        delegatee.deleteAccount(serviceId, account, session, modifier);
        invalidateMessagingAccount(serviceId, account.getId(), session.getUserId(), session.getContextId());
    }

    @Override
    public MessagingAccount getAccount(final String serviceId, final int id, final Session session, final Modifier modifier) throws OXException {
        final CacheService cacheService = MessagingGenericServiceRegistry.getServiceRegistry().getService(CacheService.class);
        if (cacheService == null) {
            return delegatee.getAccount(serviceId, id, session, modifier);
        }
        final int user = session.getUserId();
        final int cid = session.getContextId();
        final MessagingAccountStorage d = delegatee;
        final Lock l = cacheLock;
        final OXObjectFactory<MessagingAccount> factory = new OXObjectFactory<MessagingAccount>() {

            @Override
            public Serializable getKey() {
                return newCacheKey(cacheService, serviceId, id, user, cid);
            }

            @Override
            public MessagingAccount load() throws OXException {
                return d.getAccount(serviceId, id, session, modifier);
            }

            @Override
            public Lock getCacheLock() {
                return l;
            }
        };
        try {
            return new MessagingAccountReloader(factory, REGION_NAME);
        } catch (final OXException e) {
            throw e;
        }
    }

    @Override
    public List<MessagingAccount> getAccounts(final String serviceId, final Session session, final Modifier modifier) throws OXException {
        final TIntList ids = delegatee.getAccountIDs(serviceId, session);
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        final List<MessagingAccount> accounts = new ArrayList<MessagingAccount>(ids.size());
        class AdderProcedure implements TIntProcedure {

            OXException me;

            @Override
            public boolean execute(final int id) {
                try {
                    accounts.add(getAccount(serviceId, id, session, modifier));
                    return true;
                } catch (final OXException e) {
                    me = e;
                    return false;
                }
            }

        }
        final AdderProcedure ap = new AdderProcedure();
        if (!ids.forEach(ap) && null != ap.me) {
            throw ap.me;
        }
        return accounts;
    }

    @Override
    public void updateAccount(final String serviceId, final MessagingAccount account, final Session session, final Modifier modifier) throws OXException {
        delegatee.updateAccount(serviceId, account, session, modifier);
        invalidateMessagingAccount(serviceId, account.getId(), session.getUserId(), session.getContextId());
    }

    public String checkSecretCanDecryptStrings(final MessagingService parentService, final Session session, final String secret) throws OXException {
        return delegatee.checkSecretCanDecryptStrings(parentService, session, secret);
    }

    public void migrateToNewSecret(final MessagingService parentService, final String oldSecret, final String newSecret, final Session session) throws OXException {
        delegatee.migrateToNewSecret(parentService, oldSecret, newSecret, session);
    }

    public boolean hasAccount(final MessagingService service, final Session session) throws OXException {
        return delegatee.hasAccount(service, session);
    }

}
