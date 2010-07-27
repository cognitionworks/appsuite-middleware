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

package com.openexchange.cache.dynamic.impl;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.cache.OXCachingException;
import com.openexchange.cache.OXCachingException.Code;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheException;
import com.openexchange.caching.CacheService;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.server.services.ServerServiceRegistry;

/**
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public abstract class Refresher<T extends Serializable> {

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog(Refresher.class);

    /**
     * Factory for reloading cached objects.
     */
    private final OXObjectFactory<T> factory;

    /**
     * The cache region name.
     */
    private final String regionName;

    /**
     * Default constructor.
     * 
     * @throws IllegalArgumentException If provided region name is <code>null</code>
     */
    protected Refresher(final OXObjectFactory<T> factory, final String regionName) {
        super();
        this.factory = factory;
        if (null == regionName) {
            throw new IllegalArgumentException("Cache region name is null");
        }
        this.regionName = regionName;
    }

    private Cache getCache() throws CacheException {
        return getCache(regionName);
    }

    protected void cache(T obj) throws CacheException {
        cache(obj, getCache(), factory);
    }

    /**
     * Checks if the object was removed from the cache and must be reloaded from the database.
     * @throws AbstractOXException if loading or putting into cache fails.
     */
    protected T refresh() throws AbstractOXException {
        return refresh(regionName, factory);
    }

    public static <T extends Serializable> T cache(T obj, Cache cache, OXObjectFactory<T> factory) throws CacheException {
        T retval = null;
        final Lock lock = factory.getCacheLock();
        final Serializable key = factory.getKey(); 
        lock.lock();
        try {
            Object tmp = cache.get(key);
            if (null == tmp) {
                cache.put(key, obj);
            } else if (tmp instanceof Condition) {
                cache.put(key, obj);
                ((Condition) tmp).signalAll();
            } else {
                // If object is already in cache, return it instead of putting new object into cache.
                @SuppressWarnings("unchecked")
                T tmp2 = (T) tmp;
                retval = tmp2;
            }
        } finally {
            lock.unlock();
        }
        return retval;
    }

    public static <T extends Serializable> T refresh(String regionName, OXObjectFactory<T> factory) throws AbstractOXException {
        return refresh(regionName, getCache(regionName), factory);
    }

    public static <T extends Serializable> T refresh(String regionName, Cache cache, OXObjectFactory<T> factory) throws AbstractOXException {
        if (null == cache) {
            return factory.load();
        }
        T retval = null;
        final Lock lock = factory.getCacheLock();
        final Serializable key = factory.getKey(); 
        Condition cond = null;
        lock.lock();
        try {
            Object tmp = cache.get(key);
            if (null == tmp) {
                // I am the thread to load the object. Put temporary condition
                // into cache.
                cond = lock.newCondition();
                try {
                    cache.putSafe(key, (Serializable) cond);
                } catch (final CacheException e) {
                    throw new OXCachingException(Code.FAILED_PUT, e);
                }
            } else if (tmp instanceof Condition) {
                // I have to wait for another thread to load the object.
                cond = (Condition) tmp;
                if (cond.await(1, TimeUnit.SECONDS)) {
                    // Other thread finished loading the object.
                    final Object tmp2 = cache.get(key);
                    if (null != tmp2 && !(tmp2 instanceof Condition)) {
                        @SuppressWarnings("unchecked")
                        T tmp3 = (T) tmp2;
                        retval = tmp3;
                        cond = null;
                    }
                } else {
                    // We have to load it, too.
                    LOG.warn("Found 2 threads loading object \"" + String.valueOf(key) + "\" after 1 second into Cache \"" + regionName + "\"");
                }
            } else {
                // Only other option is that the cache contains the delegate object.
                @SuppressWarnings("unchecked")
                T tmp2 = (T) tmp;
                retval = tmp2;
            }
        } catch (final InterruptedException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
        if (null != cond) {
            try {
                retval = factory.load();
            } catch (final AbstractOXException e) {
                cache.remove(key);
                throw e;
            }
            lock.lock();
            try {
                cache.put(key, retval);
                cond.signalAll();
            } catch (final CacheException e) {
                throw new OXCachingException(Code.FAILED_PUT, e);
            } finally {
                lock.unlock();
            }
        }
        return retval;
    }

    public static Cache getCache(String regionName) throws CacheException {
        if (null == regionName) {
            throw new CacheException(CacheException.Code.INVALID_CACHE_REGION_NAME, regionName);
        }
        final CacheService service = ServerServiceRegistry.getInstance().getService(CacheService.class);
        if (null == service) {
            return null;
        }
        return service.getCache(regionName);
    }

}
