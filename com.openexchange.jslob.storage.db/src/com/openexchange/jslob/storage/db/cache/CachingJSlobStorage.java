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

package com.openexchange.jslob.storage.db.cache;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheService;
import com.openexchange.exception.OXException;
import com.openexchange.java.StringAllocator;
import com.openexchange.jslob.JSlob;
import com.openexchange.jslob.JSlobExceptionCodes;
import com.openexchange.jslob.JSlobId;
import com.openexchange.jslob.storage.JSlobStorage;
import com.openexchange.jslob.storage.db.DBJSlobStorage;
import com.openexchange.jslob.storage.db.osgi.DBJSlobStorageActivcator;
import com.openexchange.jslob.storage.db.util.DelayedStoreOp;
import com.openexchange.jslob.storage.db.util.DelayedStoreOpDelayQueue;
import com.openexchange.threadpool.ThreadPools;

/**
 * {@link CachingJSlobStorage}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CachingJSlobStorage implements JSlobStorage, Runnable {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CachingJSlobStorage.class);

    private static final String REGION_NAME = Constants.REGION_NAME;

    private static final AtomicReference<CacheService> SERVICE = new AtomicReference<CacheService>();

    /**
     * Sets the {@link CacheService}.
     *
     * @param service The service
     */
    public static void setCacheService(final CacheService service) {
        SERVICE.set(service);
    }

    private static CachingJSlobStorage instance;

    /**
     * Initializes
     */
    public static synchronized CachingJSlobStorage initialize(final DBJSlobStorage delegate) {
        CachingJSlobStorage tmp = instance;
        if (null == tmp) {
            tmp = new CachingJSlobStorage(delegate);
            ThreadPools.getThreadPool().submit(ThreadPools.task(tmp));
            instance = tmp;
        }
        return tmp;
    }

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static synchronized CachingJSlobStorage getInstance() {
        return instance;
    }

    /**
     * Shuts-down
     */
    public static synchronized void shutdown() {
        final CachingJSlobStorage tmp = instance;
        if (null != tmp) {
            tmp.release();
            instance = null;
        }
    }

    /** The poison element */
    private static final DelayedStoreOp POISON = new DelayedStoreOp(null, null, null, true);

    /** Proxy attribute for the object implementing the persistent methods. */
    private final DBJSlobStorage delegate;

    /** The queue for delayed store operations */
    private final DelayedStoreOpDelayQueue delayedStoreOps;

    /** The keep-going flag */
    private final AtomicBoolean keepgoing;

    /**
     * Initializes a new {@link CachingJSlobStorage}.
     */
    private CachingJSlobStorage(final DBJSlobStorage delegate) {
        super();
        this.delegate = delegate;
        delayedStoreOps = new DelayedStoreOpDelayQueue();
        keepgoing = new AtomicBoolean(true);
    }

    @Override
    public void run() {
        final List<DelayedStoreOp> objects = new ArrayList<DelayedStoreOp>(16);
        while (keepgoing.get()) {
            try {
                objects.clear();
                // Blocking wait for at least 1 DelayedPushMsObject to expire.
                final DelayedStoreOp object = delayedStoreOps.take();
                if (POISON == object) {
                    return;
                }
                objects.add(object);
                // Drain more if available
                delayedStoreOps.drainTo(objects);
                final Cache cache = optCache();
                if (null != cache) {
                    try {
                        if (writeMultiple2DB(objects, cache)) {
                            // Reached poison element
                            return;
                        }
                    } catch (final OXException e) {
                        // Multiple store failed
                        if (!JSlobExceptionCodes.UNEXPECTED_ERROR.equals(e) || !SQLException.class.isInstance(e.getCause())) {
                            throw e;
                        }
                        boolean leave = false;
                        for (final DelayedStoreOp delayedStoreOp : objects) {
                            if (POISON == delayedStoreOp) {
                                // Reached poison element
                                leave = true;
                            } else if (delayedStoreOp != null) {
                                try {
                                    write2DB(delayedStoreOp, cache);
                                } catch (final Exception x) {
                                    LOG.error("JSlobs could not be flushed to database", x);
                                }
                            }
                        }
                        if (leave) {
                            return;
                        }
                    }
                }
            } catch (final Exception e) {
                LOG.error("Checking for delayed JSlobs failed", e);
            }
        }
    }

    private void write2DB(final DelayedStoreOp delayedStoreOp, final Cache cache) throws OXException {
        final Object obj = cache.getFromGroup(delayedStoreOp.id, delayedStoreOp.group);
        if (obj instanceof JSlob) {
            final JSlob t = (JSlob) obj;
            // Write to store
            delegate.store(delayedStoreOp.jSlobId, t);
            // Propagate among remote caches
            cache.putInGroup(delayedStoreOp.id, delayedStoreOp.group, t.setId(delayedStoreOp.jSlobId), true);
        }
    }

    private boolean writeMultiple2DB(final List<DelayedStoreOp> delayedStoreOps, final Cache cache) throws OXException {
        boolean leave = false;
        // Collect valid delayed store operations
        int size = delayedStoreOps.size();
        final Map<JSlobId, JSlob> jslobs = new HashMap<JSlobId, JSlob>(size);
        for (int i = 0; i < size; i++) {
            final DelayedStoreOp delayedStoreOp = delayedStoreOps.get(i);
            if (POISON == delayedStoreOp) {
                leave = true;
            } else if (delayedStoreOp != null) {
                final Object obj = cache.getFromGroup(delayedStoreOp.id, delayedStoreOp.group);
                if (obj instanceof JSlob) {
                    jslobs.put(delayedStoreOp.jSlobId, (JSlob) obj);
                }
            }
        }
        // Store them
        delegate.storeMultiple(jslobs);
        // Invalidate remote caches
        for (final Entry<JSlobId, JSlob> entry : jslobs.entrySet()) {
            final JSlobId id = entry.getKey();
            cache.putInGroup(id.getId(), groupName(id), entry.getValue().setId(id), true);
        }
        return leave;
    }

    /**
     * Drops all JSlob entries associated with specified user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    public void dropAllUserJSlobs(final int userId, final int contextId) {
        final Cache cache = optCache();
        if (null != cache) {
            for (final String serviceId : DBJSlobStorageActivcator.SERVICE_IDS) {
                cache.invalidateGroup(new StringAllocator(serviceId).append('@').append(userId).append('@').append(contextId).toString());
            }
        }
    }

    private void release() {
        keepgoing.set(false);
        delayedStoreOps.offer(POISON);
        final CacheService cacheService = SERVICE.get();
        if (null != cacheService) {
            try {
                final Cache cache = cacheService.getCache(REGION_NAME);
                flushDelayedOps2Storage(cache);
                cache.clear();
                cache.dispose();
            } catch (final Exception e) {
                // Ignore
            }
        }
    }

    private void flushDelayedOps2Storage(final Cache cache) {
        if (null != cache) {
            for (final DelayedStoreOp delayedStoreOp : delayedStoreOps) {
                if (delayedStoreOp != null && POISON != delayedStoreOp) {
                    try {
                        write2DB(delayedStoreOp, cache);
                    } catch (final Exception e) {
                        LOG.error("JSlobs could not be flushed to database", e);
                    }
                }
            }
        }
    }

    private Cache optCache() {
        try {
            final CacheService cacheService = SERVICE.get();
            return null == cacheService ? null : cacheService.getCache(REGION_NAME);
        } catch (final OXException e) {
            LOG.warn("Failed to get cache.", e);
        }
        return null;
    }

    private String groupName(final JSlobId id) {
        return new StringAllocator(id.getServiceId()).append('@').append(id.getUser()).append('@').append(id.getContext()).toString();
    }

    @Override
    public String getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public boolean store(final JSlobId id, final JSlob t) throws OXException {
        final Cache cache = optCache();
        if (null == cache) {
            return delegate.store(id, t);
        }

        // Explicitly invalidate cache entry
        final String groupName = groupName(id);
        cache.removeFromGroup(id.getId(), groupName);

        // Delay store operation
        if (delayedStoreOps.offer(new DelayedStoreOp(id.getId(), groupName, id, false))) {
            // Added to delay queue -- put current to cache
            cache.putInGroup(id.getId(), groupName, t.setId(id), false);
            return true;
        }
        // Not possible to add to delay queue
        final boolean storeResult = delegate.store(id, t);
        cache.putInGroup(id.getId(), groupName, t.setId(id), !storeResult);
        return storeResult;
    }

    @Override
    public void invalidate(final JSlobId id) {
        final Cache cache = optCache();
        if (null != cache) {
            cache.removeFromGroup(id.getId(), groupName(id));
        }
    }

    @Override
    public JSlob load(final JSlobId id) throws OXException {
        final Cache cache = optCache();
        if (null == cache) {
            return delegate.load(id);
        }
        final Object object = cache.getFromGroup(id.getId(), groupName(id));
        if (object instanceof JSlob) {
            return (JSlob) object;
        }
        final JSlob loaded = delegate.load(id);
        cache.putInGroup(id.getId(), groupName(id), loaded, false);
        return loaded.clone();
    }

    @Override
    public JSlob opt(final JSlobId id) throws OXException {
        final Cache cache = optCache();
        if (null == cache) {
            return delegate.opt(id);
        }
        final String groupName = groupName(id);
        {
            final Object fromCache = cache.getFromGroup(id.getId(), groupName);
            if (null != fromCache) {
                return ((JSlob) fromCache).clone();
            }
        }
        // Optional retrieval from DB storage
        final JSlob opt = delegate.opt(id);
        if (null == opt) {
            // Null
            return null;
        }
        cache.putInGroup(id.getId(), groupName, opt, false);
        return opt.clone();
    }

    @Override
    public List<JSlob> list(List<JSlobId> ids) throws OXException {
        final Cache cache = optCache();
        if (null == cache) {
            return delegate.list(ids);
        }

        final int size = ids.size();
        final Map<String, JSlob> map = new HashMap<String, JSlob>(size);
        final List<JSlobId> toLoad = new ArrayList<JSlobId>(size);
        for (int i = 0; i < size; i++) {
            final JSlobId id = ids.get(i);
            final Object object = cache.getFromGroup(id.getId(), groupName(id));
            if (object instanceof JSlob) {
                map.put(id.getId(), (JSlob) object);
            } else {
                toLoad.add(id);
            }
        }

        if (!toLoad.isEmpty()) {
            final List<JSlob> loaded = delegate.list(toLoad);
            for (final JSlob jSlob : loaded) {
                if (null != jSlob) {
                    final JSlobId id = jSlob.getId();
                    cache.putInGroup(id.getId(), groupName(id), jSlob, false);
                    map.put(id.getId(), jSlob.clone());
                }
            }
        }

        final List<JSlob> ret = new ArrayList<JSlob>(size);
        for (final JSlobId id : ids) {
            ret.add(null == id ? null : map.get(id.getId()));
        }
        return ret;
    }

    @Override
    public Collection<JSlob> list(final JSlobId id) throws OXException {
        final Cache cache = optCache();
        if (null == cache) {
            return delegate.list(id);
        }
        final Collection<String> ids = delegate.getIDs(id);
        final List<JSlob> ret = new ArrayList<JSlob>(ids.size());
        final String serviceId = id.getServiceId();
        final int user = id.getUser();
        final int context = id.getContext();
        for (final String sId : ids) {
            ret.add(load(new JSlobId(serviceId, sId, user, context)));
        }
        return ret;
    }

    @Override
    public JSlob remove(final JSlobId id) throws OXException {
        final Cache cache = optCache();
        if (null != cache) {
            cache.removeFromGroup(id.getId(), groupName(id));
        }
        return delegate.remove(id);
    }

    @Override
    public boolean lock(final JSlobId jslobId) throws OXException {
        return delegate.lock(jslobId);
    }

    @Override
    public void unlock(final JSlobId jslobId) throws OXException {
        delegate.unlock(jslobId);
    }

}
