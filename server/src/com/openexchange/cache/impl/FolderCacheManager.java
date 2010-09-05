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

package com.openexchange.cache.impl;

import static com.openexchange.java.Autoboxing.I;
import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.ajax.fields.FolderFields;
import com.openexchange.api2.OXException;
import com.openexchange.cache.dynamic.impl.OXObjectFactory;
import com.openexchange.cache.dynamic.impl.Refresher;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheException;
import com.openexchange.caching.CacheKey;
import com.openexchange.caching.CacheService;
import com.openexchange.caching.ElementAttributes;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.oxfolder.OXFolderException;
import com.openexchange.tools.oxfolder.OXFolderNotFoundException;
import com.openexchange.tools.oxfolder.OXFolderProperties;
import com.openexchange.tools.oxfolder.OXFolderException.FolderCode;

/**
 * {@link FolderCacheManager} - Holds a cache for instances of {@link FolderObject}
 * <p>
 * <b>NOTE:</b> Only cloned versions of {@link FolderObject} instances are put into or received from cache. That prevents the danger of
 * further working on and therefore changing cached instances.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class FolderCacheManager {

    private static final Log LOG = LogFactory.getLog(FolderCacheManager.class);

    private static volatile FolderCacheManager instance;

    private static final String FOLDER_CACHE_REGION_NAME = "OXFolderCache";

    private Cache folderCache;

    private final Lock cacheLock;

    /**
     * Initializes a new {@link FolderCacheManager}.
     * 
     * @throws OXException If initialization fails
     */
    private FolderCacheManager() throws OXException {
        super();
        cacheLock = new ReentrantLock(true);
        initCache();
    }

    /**
     * Checks if folder cache has been initialized.
     * 
     * @return <code>true</code> if folder cache has been initialized; otherwise <code>false</code>
     */
    public static boolean isInitialized() {
        return (instance != null);
    }

    /**
     * Checks if folder cache is enabled (through configuration).
     * 
     * @return <code>true</code> if folder cache is enabled; otherwise <code>false</code>
     */
    public static boolean isEnabled() {
        return OXFolderProperties.isEnableFolderCache();
    }

    /**
     * Initializes the singleton instance of folder cache {@link FolderCacheManager manager}.
     * 
     * @throws OXException If initialization fails
     */
    public static void initInstance() throws OXException {
        if (instance == null) {
            synchronized (FolderCacheManager.class) {
                if (instance == null) {
                    instance = new FolderCacheManager();
                }
            }
        }
    }

    /**
     * Gets the singleton instance of folder cache {@link FolderCacheManager manager}.
     * 
     * @return The singleton instance of folder cache {@link FolderCacheManager manager}.
     * @throws FolderCacheNotEnabledException If folder cache is explicitly disabled through
     *             {@link OXFolderProperties#isEnableFolderCache() property}
     * @throws OXException If initialization fails
     */
    public static FolderCacheManager getInstance() throws FolderCacheNotEnabledException, OXException {
        if (!OXFolderProperties.isEnableFolderCache()) {
            throw new FolderCacheNotEnabledException();
        }
        if (instance == null) {
            synchronized (FolderCacheManager.class) {
                if (instance == null) {
                    instance = new FolderCacheManager();
                }
            }
        }
        return instance;
    }

    /**
     * Releases the singleton instance of folder cache {@link FolderCacheManager manager}.
     */
    public static void releaseInstance() {
        if (instance != null) {
            synchronized (FolderCacheManager.class) {
                if (instance != null) {
                    instance = null;
                    final CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
                    if (null != cacheService) {
                        try {
                            cacheService.freeCache(FOLDER_CACHE_REGION_NAME);
                        } catch (final CacheException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Initializes cache reference.
     * 
     * @throws OXFolderException If initializing the cache reference fails
     */
    public void initCache() throws OXFolderException {
        if (folderCache != null) {
            return;
        }
        try {
            folderCache = ServerServiceRegistry.getInstance().getService(CacheService.class).getCache(FOLDER_CACHE_REGION_NAME);
        } catch (final CacheException e) {
            throw new OXFolderException(FolderCode.FOLDER_CACHE_INITIALIZATION_FAILED, e, FOLDER_CACHE_REGION_NAME, e.getMessage());
        }
    }

    /**
     * Releases cache reference.
     * 
     * @throws OXFolderException If clearing cache fails
     */
    public void releaseCache() throws OXFolderException {
        if (folderCache == null) {
            return;
        }
        try {
            folderCache.clear();
        } catch (final CacheException e) {
            throw new OXFolderException(FolderCode.FOLDER_CACHE_INITIALIZATION_FAILED, e, FOLDER_CACHE_REGION_NAME, e.getMessage());
        }
        folderCache = null;
    }

    CacheKey getCacheKey(final int cid, final int objectId) {
        return folderCache.newCacheKey(cid, objectId);
    }

    Lock getCacheLock() {
        return cacheLock;
    }

    private class FolderFactory implements OXObjectFactory<FolderObject> {

        private final Context ctx;

        private final int folderId;

        FolderFactory(final Context ctx, final int folderId) {
            super();
            this.ctx = ctx;
            this.folderId = folderId;
        }

        public Lock getCacheLock() {
            return FolderCacheManager.this.getCacheLock();
        }

        public Serializable getKey() {
            return getCacheKey(ctx.getContextId(), folderId);
        }

        public FolderObject load() throws AbstractOXException {
            return loadFolderObjectInternal(folderId, ctx, null);
        }
    }

    /**
     * Fetches <code>FolderObject</code> which matches given object id. If none found or <code>fromCache</code> is not set the folder will
     * be loaded from underlying database store and automatically put into cache.
     * <p>
     * <b>NOTE:</b> This method returns a clone of cached <code>FolderObject</code> instance. Thus any modifications made to the referenced
     * object will not affect cached version
     * 
     * @throws OXException If a caching error occurs
     */
    public FolderObject getFolderObject(final int objectId, final boolean fromCache, final Context ctx, final Connection readCon) throws OXException {
        if (null == folderCache) {
            throw new FolderCacheNotEnabledException();
        }
        try {
            if (fromCache) {
                /*
                 * Conditional put into cache: Put only if absent.
                 */
                if (null != readCon) {
                    putIfAbsentInternal(new LoadingFolderProvider(objectId, ctx, readCon), ctx, null);
                }
            } else {
                /*
                 * Forced put into cache: Always put.
                 */
                putFolderObject(loadFolderObjectInternal(objectId, ctx, readCon), ctx, true, null);
            }
            /*
             * Return refreshable object
             */
            return Refresher.refresh(FOLDER_CACHE_REGION_NAME, folderCache, new FolderFactory(ctx, objectId)).clone();
        } catch (final AbstractOXException e) {
            if (e instanceof OXException) {
                throw (OXException) e;
            }
            throw new OXException(e);
        }
    }

    /**
     * <p>
     * Fetches <code>FolderObject</code> which matches given object id.
     * </p>
     * <p>
     * <b>NOTE:</b> This method returns a clone of cached <code>FolderObject</code> instance. Thus any modifications made to the referenced
     * object will not affect cached version
     * </p>
     * 
     * @return The matching <code>FolderObject</code> instance else <code>null</code>
     */
    public FolderObject getFolderObject(final int objectId, final Context ctx) {
        if (null == folderCache) {
            return null;
        }
        cacheLock.lock();
        try {
            final Object tmp = folderCache.get(getCacheKey(ctx.getContextId(), objectId));
            // Refresher uses Condition objects to prevent multiple threads loading same folder.
            if (tmp instanceof FolderObject) {
                return ((FolderObject) tmp).clone();
            }
            return null;
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * Loads the folder which matches given object id from underlying database store and puts it into cache.
     * <p>
     * <b>NOTE:</b> This method returns a clone of cached <code>FolderObject</code> instance. Thus any modifications made to the referenced
     * object will not affect cached version
     * 
     * @return The matching <code>FolderObject</code> instance fetched from storage else <code>null</code>
     * @throws OXException If a caching error occurs
     */
    public FolderObject loadFolderObject(final int folderId, final Context ctx, final Connection readCon) throws OXException {
        final CacheKey key = getCacheKey(ctx.getContextId(), folderId);
        cacheLock.lock();
        try {
            final Object tmp = folderCache.get(key);
            if (tmp instanceof FolderObject) {
                folderCache.remove(key);
            }
        } catch (final CacheException e) {
            throw new OXException(e);
        } finally {
            cacheLock.unlock();
        }
        if (null != readCon) {
            putIfAbsent(loadFolderObjectInternal(folderId, ctx, readCon), ctx, null);
        }
        try {
            return Refresher.refresh(FOLDER_CACHE_REGION_NAME, folderCache, new FolderFactory(ctx, folderId)).clone();
        } catch (final AbstractOXException e) {
            if (e instanceof OXException) {
                throw (OXException) e;
            }
            throw new OXException(e);
        }
    }

    /**
     * Loads the folder object from underlying database storage whose id matches given parameter <code>folderId</code>.
     * <p>
     * 
     * @param folderId The folder ID
     * @param ctx The context
     * @param readCon A readable connection (<b>optional</b>), pass <code>null</code> to fetch a new one from connection pool
     * @return The object loaded from DB.
     * @throws OXException If folder object could not be loaded or a caching error occurs
     */
    FolderObject loadFolderObjectInternal(final int folderId, final Context ctx, final Connection readCon) throws OXException {
        if (folderId <= 0) {
            throw new OXFolderNotFoundException(folderId, ctx);
        }
        return FolderObject.loadFolderObjectFromDB(folderId, ctx, readCon);
    }

    /**
     * If the specified folder object is not already in cache, it is put into cache.
     * <p>
     * <b>NOTE:</b> This method puts a clone of given <code>FolderObject</code> instance into cache. Thus any modifications made to the
     * referenced object will not affect cached version
     * 
     * @param folderObj The folder object
     * @param ctx The context
     * @param elemAttribs The element's attributes (<b>optional</b>), pass <code>null</code> to use the default attributes
     * @return The previous folder object available in cache, or <tt>null</tt> if there was none
     * @throws OXException If put-if-absent operation fails
     */
    public FolderObject putIfAbsent(final FolderObject folderObj, final Context ctx, final ElementAttributes elemAttribs) throws OXException {
        if (null == folderCache) {
            throw new FolderCacheNotEnabledException();
        }
        if (!folderObj.containsObjectID()) {
            throw new OXFolderException(FolderCode.MISSING_FOLDER_ATTRIBUTE, FolderFields.ID, I(-1), I(ctx.getContextId()));
        }
        return putIfAbsentInternal(new InstanceFolderProvider(folderObj), ctx, elemAttribs);
    }

    private FolderObject putIfAbsentInternal(final FolderProvider folderObj, final Context ctx, final ElementAttributes elemAttribs) throws OXException {
        final CacheKey key = getCacheKey(ctx.getContextId(), folderObj.getObjectID());
        cacheLock.lock();
        try {
            final Object tmp = folderCache.get(key);
            if (tmp instanceof FolderObject) {
                // Already in cache
                return ((FolderObject) tmp).clone();
            }
            Condition cond = null;
            if (tmp instanceof Condition) {
                cond = (Condition) tmp;
            } else {
                // Remove to distribute PUT as REMOVE
                folderCache.remove(key);
            }
            if (elemAttribs == null) {
                /*
                 * Put with default attributes
                 */
                folderCache.put(key, folderObj.getFolderObject().clone());
            } else {
                folderCache.put(key, folderObj.getFolderObject().clone(), elemAttribs);
            }
            if (null != cond) {
                cond.signalAll();
            }
            /*
             * Return null to indicate successful insertion
             */
            return null;
        } catch (final CacheException e) {
            throw new OXException(e);
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * Simply puts given <code>FolderObject</code> into cache if object's id is different to zero.
     * <p>
     * <b>NOTE:</b> This method puts a clone of given <code>FolderObject</code> instance into cache. Thus any modifications made to the
     * referenced object will not affect cached version
     * 
     * @param folderObj The folder object
     * @param ctx The context
     * @throws OXException If a caching error occurs
     */
    public void putFolderObject(final FolderObject folderObj, final Context ctx) throws OXException {
        putFolderObject(folderObj, ctx, true, null);
    }

    /**
     * <p>
     * Simply puts given <code>FolderObject</code> into cache if object's id is different to zero. If flag <code>overwrite</code> is set to
     * <code>false</code> then this method returns immediately if cache already holds a matching entry.
     * </p>
     * <p>
     * <b>NOTE:</b> This method puts a clone of given <code>FolderObject</code> instance into cache. Thus any modifications made to the
     * referenced object will not affect cached version
     * </p>
     * 
     * @param folderObj The folder object
     * @param ctx The context
     * @param overwrite <code>true</code> to overwrite; otherwise <code>false</code>
     * @param elemAttribs The element's attributes (<b>optional</b>), pass <code>null</code> to use the default attributes
     * @throws OXException If a caching error occurs
     */
    public void putFolderObject(final FolderObject folderObj, final Context ctx, final boolean overwrite, final ElementAttributes elemAttribs) throws OXException {
        if (null == folderCache) {
            return;
        }
        if (!folderObj.containsObjectID()) {
            throw new OXFolderException(FolderCode.MISSING_FOLDER_ATTRIBUTE, FolderFields.ID, I(-1), I(ctx.getContextId()));
        }
        if (null != elemAttribs) {
            /*
             * Ensure isLateral is set to false
             */
            elemAttribs.setIsLateral(false);
        }
        /*
         * Put clone of new object into cache.
         */
        final FolderObject clone = folderObj.clone();
        final CacheKey key = getCacheKey(ctx.getContextId(), folderObj.getObjectID());
        cacheLock.lock();
        try {
            final Object tmp = folderCache.get(key);
            // If there is currently an object associated with this key in the region it is replaced.
            Condition cond = null;
            if (overwrite) {
                if (tmp instanceof FolderObject) {
                    // Remove to distribute PUT as REMOVE
                    folderCache.remove(key);
                } else if (tmp instanceof Condition) {
                    cond = (Condition) tmp;
                }
            } else {
                // Another thread made a PUT in the meantime. Return cause we may not overwrite.
                if (tmp instanceof FolderObject) {
                    return;
                } else if (tmp instanceof Condition) {
                    cond = (Condition) tmp;
                }
            }
            if (elemAttribs == null) {
                // Put with default attributes
                folderCache.put(key, clone);
            } else {
                folderCache.put(key, clone, elemAttribs);
            }
            if (null != cond) {
                cond.signalAll();
            }
        } catch (final CacheException e) {
            throw new OXException(e);
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * Removes matching folder object from cache
     * 
     * @param key The key
     * @param ctx The context
     * @throws OXException If a caching error occurs
     */
    public void removeFolderObject(final int key, final Context ctx) throws OXException {
        if (null == folderCache) {
            return;
        }
        // Remove object from cache if exist
        if (key > 0) {
            final CacheKey cacheKey = getCacheKey(ctx.getContextId(), key);
            cacheLock.lock();
            try {
                final Object tmp = folderCache.get(cacheKey);
                if (!(tmp instanceof Condition)) {
                    folderCache.remove(cacheKey);
                }
            } catch (final CacheException e) {
                throw new OXException(e);
            } finally {
                cacheLock.unlock();
            }
        }
    }

    /**
     * Removes matching folder objects from cache
     * 
     * @param keys The keys
     * @param ctx The context
     * @throws OXException If a caching error occurs
     */
    public void removeFolderObjects(final int[] keys, final Context ctx) throws OXException {
        if (null == folderCache) {
            return;
        } else if (keys == null || keys.length == 0) {
            return;
        }
        final List<CacheKey> cacheKeys = new ArrayList<CacheKey>();
        for (final int key : keys) {
            if (key > 0) {
                cacheKeys.add(getCacheKey(ctx.getContextId(), key));
            }
        }
        /*
         * Remove objects from cache
         */
        cacheLock.lock();
        try {
            for (final CacheKey cacheKey : cacheKeys) {
                final Object tmp = folderCache.get(cacheKey);
                if (!(tmp instanceof Condition)) {
                    folderCache.remove(cacheKey);
                }
            }
        } catch (final CacheException e) {
            throw new OXException(e);
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * Removes all folder objects from this cache
     * 
     * @throws OXException If folder cache cannot be cleared
     */
    public void clearAll() throws OXException {
        if (null == folderCache) {
            return;
        }
        try {
            folderCache.clear();
        } catch (final CacheException e) {
            throw new OXException(e);
        }
    }

    /**
     * Returns default element attributes for this cache
     * 
     * @return default element attributes for this cache or <code>null</code>
     * @throws CacheException If a caching error occurs
     */
    public ElementAttributes getDefaultFolderObjectAttributes() throws CacheException {
        if (null == folderCache) {
            return null;
        }
        /*
         * Returns a copy NOT a reference
         */
        return folderCache.getDefaultElementAttributes();
    }

    private static interface FolderProvider {

        FolderObject getFolderObject() throws OXException;

        int getObjectID();
    }

    private static final class InstanceFolderProvider implements FolderProvider {

        private final FolderObject folderObject;

        public InstanceFolderProvider(final FolderObject folderObject) {
            super();
            this.folderObject = folderObject;
        }

        public FolderObject getFolderObject() throws OXException {
            return folderObject;
        }

        public int getObjectID() {
            return folderObject.getObjectID();
        }

    }

    private static final class LoadingFolderProvider implements FolderProvider {

        private final Connection readCon;

        private final int folderId;

        private final Context ctx;

        public LoadingFolderProvider(final int folderId, final Context ctx, final Connection readCon) {
            super();
            this.folderId = folderId;
            this.ctx = ctx;
            this.readCon = readCon;
        }

        public FolderObject getFolderObject() throws OXException {
            if (folderId <= 0) {
                throw new OXFolderNotFoundException(folderId, ctx);
            }
            return FolderObject.loadFolderObjectFromDB(folderId, ctx, readCon);
        }

        public int getObjectID() {
            return folderId;
        }

    }

}
