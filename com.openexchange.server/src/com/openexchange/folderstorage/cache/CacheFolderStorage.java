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

package com.openexchange.folderstorage.cache;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheKey;
import com.openexchange.caching.CacheService;
import com.openexchange.concurrent.CallerRunsCompletionService;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.Folder;
import com.openexchange.folderstorage.FolderExceptionErrorMessage;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.FolderType;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.RemoveAfterAccessFolder;
import com.openexchange.folderstorage.SortableId;
import com.openexchange.folderstorage.StorageParameters;
import com.openexchange.folderstorage.StoragePriority;
import com.openexchange.folderstorage.StorageType;
import com.openexchange.folderstorage.Type;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.folderstorage.cache.lock.TreeLockManagement;
import com.openexchange.folderstorage.cache.memory.FolderMap;
import com.openexchange.folderstorage.cache.memory.FolderMapManagement;
import com.openexchange.folderstorage.internal.StorageParametersImpl;
import com.openexchange.folderstorage.internal.performers.ClearPerformer;
import com.openexchange.folderstorage.internal.performers.CreatePerformer;
import com.openexchange.folderstorage.internal.performers.DeletePerformer;
import com.openexchange.folderstorage.internal.performers.InstanceStorageParametersProvider;
import com.openexchange.folderstorage.internal.performers.PathPerformer;
import com.openexchange.folderstorage.internal.performers.SessionStorageParametersProvider;
import com.openexchange.folderstorage.internal.performers.StorageParametersProvider;
import com.openexchange.folderstorage.internal.performers.UpdatePerformer;
import com.openexchange.folderstorage.internal.performers.UpdatesPerformer;
import com.openexchange.groupware.ldap.User;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.mail.utils.MailFolderUtility;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.server.osgiservice.ServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.threadpool.ThreadPoolCompletionService;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.ThreadPools;
import com.openexchange.threadpool.behavior.AbortBehavior;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * {@link CacheFolderStorage} - The cache folder storage.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CacheFolderStorage implements FolderStorage {

    protected static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(CacheFolderStorage.class));

    private static final ThreadPools.ExpectedExceptionFactory<OXException> FACTORY =
        new ThreadPools.ExpectedExceptionFactory<OXException>() {

            @Override
            public Class<OXException> getType() {
                return OXException.class;
            }

            @Override
            public OXException newUnexpectedError(final Throwable t) {
                return FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(t, t.getMessage());
            }
        };

    private final String realTreeId;

    private final CacheFolderStorageRegistry registry;

    private volatile CacheService cacheService;

    private volatile Cache globalCache;

    // private Cache userCache;

    /**
     * Initializes a new {@link CacheFolderStorage}.
     */
    public CacheFolderStorage() {
        super();
        realTreeId = REAL_TREE_ID;
        registry = CacheFolderStorageRegistry.getInstance();
    }

    @Override
    public void clearCache(final int userId, final int contextId) {
        final Cache cache = globalCache;
        if (null != cache) {
            cache.invalidateGroup(String.valueOf(contextId));
        }
        dropUserEntries(userId, contextId);
    }

    /**
     * Clears this cache with respect to specified session.
     *
     * @param session The session
     */
    public void clear(final Session session) {
        clearCache(session.getUserId(), session.getContextId());
    }

    /**
     * Initializes this folder cache on available cache service.
     *
     * @throws OXException If initialization of this folder cache fails
     */
    public void onCacheAvailable() throws OXException {
        cacheService = CacheServiceRegistry.getServiceRegistry().getService(CacheService.class, true);
        globalCache = cacheService.getCache("GlobalFolderCache");
        // userCache = cacheService.getCache("UserFolderCache");
    }

    /**
     * Disposes this folder cache on absent cache service.
     *
     * @throws OXException If disposal of this folder cache fails
     */
    public void onCacheAbsent() throws OXException {
        final CacheService service = cacheService;
        final Cache cache = globalCache;
        if (cache != null) {
            try {
                cache.clear();
                if (null != service) {
                    service.freeCache("GlobalFolderCache");
                }
            } finally {
                globalCache = null;
            }
        }
        /*-
         *
        if (userCache != null) {
            try {
                userCache.clear();
                if (null != cacheService) {
                    cacheService.freeCache("UserFolderCache");
                }
            } catch (final CacheException e) {
                throw new OXException(e);
            } finally {
                userCache = null;
            }
        }
         */
        if (service != null) {
            cacheService = null;
        }
    }

    @Override
    public void checkConsistency(final String treeId, final StorageParameters storageParameters) throws OXException {
        final Lock lock = writeLockFor(treeId, storageParameters);
        lock.lock();
        try {
            for (final FolderStorage folderStorage : registry.getFolderStoragesForTreeID(treeId)) {
                final boolean started = folderStorage.startTransaction(storageParameters, true);
                try {
                    folderStorage.checkConsistency(treeId, storageParameters);
                    if (started) {
                        folderStorage.commitTransaction(storageParameters);
                    }
                } catch (final OXException e) {
                    if (started) {
                        folderStorage.rollback(storageParameters);
                    }
                    throw e;
                } catch (final Exception e) {
                    if (started) {
                        folderStorage.rollback(storageParameters);
                    }
                    throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
                }
            }
            final String realTreeId = this.realTreeId;
            if (realTreeId.equals(treeId)) {
                final ServerSession session = ServerSessionAdapter.valueOf(storageParameters.getSession());
                final ServiceRegistry serviceRegistry = CacheServiceRegistry.getServiceRegistry();
                final Runnable task = new Runnable() {
                    
                    @Override
                    public void run() {
                        final Lock lock = readLockFor(treeId, storageParameters);
                        lock.lock();
                        try {
                            final StorageParameters params = newStorageParameters(storageParameters);
                            if (session.getUserConfiguration().isMultipleMailAccounts()) {
                                final MailAccountStorageService storageService = serviceRegistry.getService(MailAccountStorageService.class, true);
                                final MailAccount[] accounts = storageService.getUserMailAccounts(session.getUserId(), session.getContextId());
                                for (final MailAccount mailAccount : accounts) {
                                    final int accountId = mailAccount.getId();
                                    if (accountId != MailAccount.DEFAULT_ID) {
                                        try {
                                            final String folderId =
                                                MailFolderUtility.prepareFullname(accountId, MailFolder.DEFAULT_FOLDER_ID);
                                            final Folder rootFolder = getFolder(realTreeId, folderId, params);
                                            final String[] subfolderIDs = rootFolder.getSubfolderIDs();
                                            if (null != subfolderIDs) {
                                                for (final String subfolderId : subfolderIDs) {
                                                    getFolder(realTreeId, subfolderId, params);
                                                }
                                            }
                                        } catch (final Exception e) {
                                            // Pre-Accessing external account folder failed.
                                            LOG.debug(e.getMessage(), e);
                                        }
                                    }
                                }
                            }
                        } catch (final Exception e) {
                            LOG.debug(e.getMessage(), e);
                        } finally {
                            lock.unlock();
                        }
                    }
                };
                serviceRegistry.getService(ThreadPoolService.class).submit(ThreadPools.task(task), AbortBehavior.getInstance());
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Checks if given folder storage is already contained in collection of opened storages. If yes, this method terminates immediately.
     * Otherwise the folder storage is opened according to specified modify flag and is added to specified collection of opened storages.
     */
    protected static void checkOpenedStorage(final FolderStorage checkMe, final StorageParameters params, final boolean modify, final java.util.Collection<FolderStorage> openedStorages) throws OXException {
        if (openedStorages.contains(checkMe)) {
            // Passed storage is already opened
            return;
        }
        // Passed storage has not been opened before. Open now and add to collection
        if (checkMe.startTransaction(params, modify)) {
            openedStorages.add(checkMe);
        }
    }

    @Override
    public void restore(final String treeId, final String folderId, final StorageParameters storageParameters) throws OXException {
        final Lock lock = writeLockFor(treeId, storageParameters);
        lock.lock();
        try {
            final FolderStorage storage = registry.getFolderStorage(treeId, folderId);
            if (null == storage) {
                throw FolderExceptionErrorMessage.NO_STORAGE_FOR_ID.create(treeId, folderId);
            }
            final boolean started = storage.startTransaction(storageParameters, false);
            try {
                storage.restore(treeId, folderId, storageParameters);
                if (started) {
                    storage.commitTransaction(storageParameters);
                }
            } catch (final OXException e) {
                if (started) {
                    storage.rollback(storageParameters);
                }
                throw e;
            } catch (final Exception e) {
                if (started) {
                    storage.rollback(storageParameters);
                }
                throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
            } finally {
                clear(storageParameters.getSession());
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Folder prepareFolder(final String treeId, final Folder folder, final StorageParameters storageParameters) throws OXException {
        final Lock lock = readLockFor(treeId, storageParameters);
        lock.lock();
        try {
            final String folderId = folder.getID();
            final FolderStorage storage = registry.getFolderStorage(treeId, folderId);
            if (null == storage) {
                throw FolderExceptionErrorMessage.NO_STORAGE_FOR_ID.create(treeId, folderId);
            }
            final boolean started = storage.startTransaction(storageParameters, false);
            try {
                final Folder preparedFolder = storage.prepareFolder(treeId, folder, storageParameters);
                if (started) {
                    storage.commitTransaction(storageParameters);
                }
                if (preparedFolder.isCacheable() && preparedFolder.isGlobalID() != folder.isGlobalID()) {
                    putFolder(preparedFolder, treeId, storageParameters);
                }
                return preparedFolder;
            } catch (final OXException e) {
                if (started) {
                    storage.rollback(storageParameters);
                }
                throw e;
            } catch (final Exception e) {
                if (started) {
                    storage.rollback(storageParameters);
                }
                throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
        } finally {
            lock.unlock();
        }
    }

    private PathPerformer newPathPerformer(final StorageParameters storageParameters) throws OXException {
        final Session session = storageParameters.getSession();
        if (null == session) {
            return new PathPerformer(storageParameters.getUser(), storageParameters.getContext(), null, registry);
        }
        try {
            return new PathPerformer(new ServerSessionAdapter(session), null, registry);
        } catch (final OXException e) {
            throw new OXException(e);
        }
    }

    @Override
    public ContentType getDefaultContentType() {
        return null;
    }

    @Override
    public void commitTransaction(final StorageParameters params) throws OXException {
        // Nothing to do
    }

    @Override
    public void createFolder(final Folder folder, final StorageParameters storageParameters) throws OXException {
        final String treeId = folder.getTreeID();
        final Lock lock = writeLockFor(treeId, storageParameters);
        lock.lock();
        try {
            final Session session = storageParameters.getSession();
            /*
             * Perform create operation via non-cache storage
             */
            final String folderId;
            if (null == session) {
                final CreatePerformer perf = new CreatePerformer(storageParameters.getUser(), storageParameters.getContext(), registry);
                perf.setCheck4Duplicates(false);
                folderId = perf.doCreate(folder);
            } else {
                final CreatePerformer createPerformer = new CreatePerformer(new ServerSessionAdapter(session), registry);
                createPerformer.setCheck4Duplicates(false);
                folderId = createPerformer.doCreate(folder);
            }
            /*
             * Get folder from appropriate storage
             */
            final FolderStorage storage = registry.getFolderStorage(treeId, folderId);
            if (null == storage) {
                throw FolderExceptionErrorMessage.NO_STORAGE_FOR_ID.create(treeId, folderId);
            }
            /*
             * Load created folder from real tree
             */
            final Folder createdFolder = loadFolder(realTreeId, folderId, StorageType.WORKING, true, storageParameters);
            final int contextId = storageParameters.getContextId();
            final int userId = storageParameters.getUserId();
            final FolderMapManagement folderMapManagement = FolderMapManagement.getInstance();
            {
                for (final Permission permission : createdFolder.getPermissions()) {
                    if (!permission.isGroup()) {
                        final int entity = permission.getEntity();
                        if (entity != userId) {
                            final FolderMap folderMap = folderMapManagement.optFor(entity, contextId);
                            if (folderMap != null) {
                                folderMap.remove(folder.getParentID(), treeId);
                                if (!realTreeId.equals(treeId)) {
                                    folderMap.remove(folder.getParentID(), realTreeId);
                                }
                            }
                        }
                    }
                }
            }
            if (createdFolder.isCacheable()) {
                putFolder(createdFolder, realTreeId, storageParameters);
            }
            /*
             * Remove parent from cache(s)
             */
            final Cache cache = globalCache;
            final String sContextId = String.valueOf(contextId);
            final String[] trees = new String[] { treeId, realTreeId };
            for (final String tid : trees) {
                final CacheKey cacheKey = newCacheKey(folder.getParentID(), tid);
                cache.removeFromGroup(cacheKey, sContextId);
                final FolderMap folderMap = folderMapManagement.optFor(userId, contextId);
                if (null != folderMap) {
                    folderMap.remove(folder.getParentID(), tid);
                }
            }
            for (final String tid : trees) {
                final CacheKey cacheKey = newCacheKey(createdFolder.getParentID(), tid);
                cache.removeFromGroup(cacheKey, sContextId);
                final FolderMap folderMap = folderMapManagement.optFor(userId, contextId);
                if (null != folderMap) {
                    folderMap.remove(createdFolder.getParentID(), tid);
                }
            }
            /*
             * Load parent from real tree
             */
            Folder parentFolder = loadFolder(realTreeId, folder.getParentID(), StorageType.WORKING, true, storageParameters);
            if (parentFolder.isCacheable()) {
                putFolder(parentFolder, realTreeId, storageParameters);
            }
            parentFolder = loadFolder(realTreeId, createdFolder.getParentID(), StorageType.WORKING, true, storageParameters);
            if (parentFolder.isCacheable()) {
                putFolder(parentFolder, realTreeId, storageParameters);
            }
        } finally {
            lock.unlock();
        }
    }

    protected void putFolder(final Folder folder, final String treeId, final StorageParameters storageParameters) throws OXException {
        /*
         * Put to cache
         */
        if (folder.isGlobalID()) {
            globalCache.putInGroup(newCacheKey(folder.getID(), treeId), String.valueOf(storageParameters.getContextId()), folder);
        } else {
            getFolderMapFor(storageParameters.getSession()).put(treeId, folder);
        }
    }

    /**
     * Removes specified folder and all of its predecessor folders from cache.
     *
     * @param id The folder identifier
     * @param treeId The tree identifier
     * @param singleOnly <code>true</code> if only specified folder should be removed; otherwise <code>false</code> for complete folder's
     *            path to root folder
     * @param session The session providing user information
     * @throws OXException If removal fails
     */
    public void removeFromCache(final String id, final String treeId, final boolean singleOnly, final Session session) throws OXException {
        final Lock lock = TreeLockManagement.getInstance().getFor(treeId, session).writeLock();
        lock.lock();
        try {
            if (singleOnly) {
                removeSingleFromCache(id, treeId, session.getUserId(), session, true);
            } else {
                removeFromCache(id, treeId, session, new PathPerformer(new ServerSessionAdapter(session), null, registry));
            }
        } finally {
            lock.unlock();
        }
    }

    private void removeFromCache(final String id, final String treeId, final Session session, final PathPerformer pathPerformer) throws OXException {
        if (null == id) {
            return;
        }
        {
            List<String> ids;
            try {
                if (existsFolder(treeId, id, StorageType.WORKING, pathPerformer.getStorageParameters())) {
                    final UserizedFolder[] path = pathPerformer.doPath(treeId, id, true);
                    ids = new ArrayList<String>(path.length);
                    for (final UserizedFolder userizedFolder : path) {
                        ids.add(userizedFolder.getID());
                    }
                } else {
                    ids = Collections.singletonList(id);
                }
            } catch (final Exception e) {
                final org.apache.commons.logging.Log log =
                    com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(CacheFolderStorage.class));
                if (log.isDebugEnabled()) {
                    log.debug(e.getMessage(), e);
                }
                try {
                    ids = new ArrayList<String>(Arrays.asList(pathPerformer.doForcePath(treeId, id, true)));
                } catch (final Exception e1) {
                    if (log.isDebugEnabled()) {
                        log.debug(e1.getMessage(), e1);
                    }
                    ids = Collections.singletonList(id);
                }
            }
            final int contextId = session.getContextId();
            final FolderMap folderMap = optFolderMapFor(session);
            final Cache cache = globalCache;
            if (realTreeId.equals(treeId)) {
                for (final String folderId : ids) {
                    cache.removeFromGroup(newCacheKey(folderId, treeId), String.valueOf(contextId));
                    if (null != folderMap) {
                        folderMap.remove(folderId, treeId);
                    }
                }
            } else {
                for (final String folderId : ids) {
                    cache.removeFromGroup(newCacheKey(folderId, treeId), String.valueOf(contextId));
                    if (null != folderMap) {
                        folderMap.remove(folderId, treeId);
                    }
                    // Now for real tree, too
                    cache.removeFromGroup(newCacheKey(folderId, realTreeId), String.valueOf(contextId));
                    if (null != folderMap) {
                        folderMap.remove(folderId, realTreeId);
                    }
                }
            }
        }
    }

    /**
     * Removes a single folder from cache.
     *
     * @param id The folder identifier
     * @param treeId The tree identifier
     * @param session The session
     */
    public void removeSingleFromCache(final String id, final String treeId, final int userId, final Session session, final boolean deleted) {
        removeSingleFromCache(id, treeId, userId, session.getContextId(), deleted);
    }

    /**
     * Removes a single folder from cache.
     *
     * @param id The folder identifier
     * @param treeId The tree identifier
     * @param contextId The context identifier
     */
    public void removeSingleFromCache(final String id, final String treeId, final int userId, final int contextId, final boolean deleted) {
        final Lock lock = TreeLockManagement.getInstance().getFor(treeId, userId, contextId).writeLock();
        lock.lock();
        try {
            final Cache cache = globalCache;
            final String sContextId = String.valueOf(contextId);
            CacheKey cacheKey = newCacheKey(id, treeId);
            Folder cachedFolder;
            if (deleted) {
                cachedFolder = (Folder) cache.getFromGroup(cacheKey, sContextId);
                if (null != cachedFolder) {
                    /*
                     * Drop parent, too
                     */
                    final String parentID = cachedFolder.getParentID();
                    if (null != parentID) {
                        cache.removeFromGroup(newCacheKey(parentID, treeId), sContextId);
                    }
                }
            }
            cache.removeFromGroup(cacheKey, sContextId);
            if (userId > 0) {
                final FolderMap folderMap = FolderMapManagement.getInstance().optFor(userId, contextId);
                if (null != folderMap) {
                    if (deleted) {
                        cachedFolder = folderMap.get(id, treeId);
                        if (null != cachedFolder) {
                            /*
                             * Drop parent, too
                             */
                            final String parentID = cachedFolder.getParentID();
                            if (null != parentID) {
                                folderMap.remove(parentID, treeId);
                            }
                        }
                    }
                    folderMap.remove(id, treeId);
                }
            }
            if (!realTreeId.equals(treeId)) {
                // Now for real tree, too
                cacheKey = newCacheKey(id, realTreeId);
                if (deleted) {
                    cachedFolder = (Folder) cache.getFromGroup(cacheKey, sContextId);
                    if (null != cachedFolder) {
                        /*
                         * Drop parent, too
                         */
                        final String parentID = cachedFolder.getParentID();
                        if (null != parentID) {
                            cache.removeFromGroup(newCacheKey(parentID, realTreeId), sContextId);
                        }
                    }
                }
                cache.removeFromGroup(cacheKey, sContextId);
                if (userId > 0) {
                    final FolderMap folderMap = FolderMapManagement.getInstance().optFor(userId, contextId);
                    if (null != folderMap) {
                        if (deleted) {
                            cachedFolder = folderMap.get(id, realTreeId);
                            if (null != cachedFolder) {
                                /*
                                 * Drop parent, too
                                 */
                                final String parentID = cachedFolder.getParentID();
                                if (null != parentID) {
                                    folderMap.remove(parentID, realTreeId);
                                }
                            }
                        }
                        folderMap.remove(id, realTreeId);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clearFolder(final String treeId, final String folderId, final StorageParameters storageParameters) throws OXException {
        final Lock lock = readLockFor(treeId, storageParameters);
        lock.lock();
        try {
            final Session session = storageParameters.getSession();
            /*
             * Perform clear operation via non-cache storage
             */
            if (null == session) {
                new ClearPerformer(storageParameters.getUser(), storageParameters.getContext(), registry).doClear(treeId, folderId);
            } else {
                try {
                    new ClearPerformer(new ServerSessionAdapter(session), registry).doClear(treeId, folderId);
                } catch (final OXException e) {
                    throw new OXException(e);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteFolder(final String treeId, final String folderId, final StorageParameters storageParameters) throws OXException {
        final Lock lock = writeLockFor(treeId, storageParameters);
        lock.lock();
        try {
            final String parentId;
            final String realParentId;
            final boolean cacheable;
            final boolean global;
            final String sContextId = String.valueOf(storageParameters.getContextId());
            {
                final Folder deleteMe;
                try {
                    deleteMe = getFolder(treeId, folderId, storageParameters);
                } catch (final OXException e) {
                    /*
                     * Obviously folder does not exist
                     */
                    final Session session = storageParameters.getSession();
                    globalCache.removeFromGroup(newCacheKey(folderId, treeId), sContextId);
                    final FolderMap folderMap = optFolderMapFor(session);
                    if (null != folderMap) {
                        folderMap.remove(folderId, treeId);
                    }
                    return;
                }
                {
                    final int contextId = storageParameters.getContextId();
                    final int userId = storageParameters.getUserId();
                    final FolderMapManagement folderMapManagement = FolderMapManagement.getInstance();
                    for (final Permission permission : deleteMe.getPermissions()) {
                        if (!permission.isGroup()) {
                            final int entity = permission.getEntity();
                            if (entity != userId) {
                                final FolderMap folderMap = folderMapManagement.optFor(entity, contextId);
                                if (folderMap != null) {
                                    folderMap.remove(folderId, treeId);
                                    if (!realTreeId.equals(treeId)) {
                                        folderMap.remove(folderId, realTreeId);
                                    }
                                    folderMap.remove(deleteMe.getParentID(), treeId);
                                    if (!realTreeId.equals(treeId)) {
                                        folderMap.remove(deleteMe.getParentID(), realTreeId);
                                    }
                                }
                            }
                        }
                    }
                }
                cacheable = deleteMe.isCacheable();
                global = deleteMe.isGlobalID();
                parentId = deleteMe.getParentID();
                if (!realTreeId.equals(treeId)) {
                    final StorageParameters parameters = newStorageParameters(storageParameters);
                    final FolderStorage folderStorage = registry.getFolderStorage(realTreeId, folderId);
                    final boolean started = folderStorage.startTransaction(parameters, false);
                    try {
                        realParentId = folderStorage.getFolder(realTreeId, folderId, parameters).getParentID();
                        if (started) {
                            folderStorage.commitTransaction(parameters);
                        }
                    } catch (final OXException e) {
                        if (started) {
                            folderStorage.rollback(parameters);
                        }
                        throw e;
                    } catch (final RuntimeException e) {
                        if (started) {
                            folderStorage.rollback(parameters);
                        }
                        throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e);
                    }
                } else {
                    realParentId = null;
                }
            }
            final Session session = storageParameters.getSession();
            if (cacheable) {
                /*
                 * Delete from cache
                 */
                if (global) {
                    globalCache.removeFromGroup(newCacheKey(folderId, treeId), sContextId);
                } else {
                    final FolderMap folderMap = optFolderMapFor(session);
                    if (null != folderMap) {
                        folderMap.remove(folderId, treeId);
                    }
                }
                /*
                 * ... and from parent folder's sub-folder list
                 */
                removeFromSubfolders(treeId, parentId, sContextId, session);
                if (null != realParentId) {
                    removeFromSubfolders(realTreeId, realParentId, sContextId, session);
                }
            }
            /*
             * Perform delete
             */
            if (null == session) {
                new DeletePerformer(storageParameters.getUser(), storageParameters.getContext(), registry).doDelete(
                    treeId,
                    folderId,
                    storageParameters.getTimeStamp());
            } else {
                try {
                    new DeletePerformer(new ServerSessionAdapter(session), registry).doDelete(
                        treeId,
                        folderId,
                        storageParameters.getTimeStamp());
                } catch (final OXException e) {
                    throw new OXException(e);
                }
            }
            /*
             * Refresh
             */
            if (null != realParentId && !FolderStorage.ROOT_ID.equals(realParentId)) {
                removeFromCache(realParentId, treeId, storageParameters.getSession(), newPathPerformer(storageParameters));
            }
            if (!FolderStorage.ROOT_ID.equals(parentId)) {
                removeFromCache(parentId, treeId, storageParameters.getSession(), newPathPerformer(storageParameters));
                final Folder parentFolder = loadFolder(treeId, parentId, StorageType.WORKING, true, storageParameters);
                if (parentFolder.isCacheable()) {
                    putFolder(parentFolder, treeId, storageParameters);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void removeFromSubfolders(final String treeId, final String parentId, final String contextId, final Session session) {
        registry.clearCaches(session.getUserId(), session.getContextId());
        globalCache.removeFromGroup(newCacheKey(parentId, treeId), contextId);
        final FolderMap folderMap = optFolderMapFor(session);
        if (null != folderMap) {
            folderMap.remove(parentId, treeId);
        }
    }

    @Override
    public String getDefaultFolderID(final User user, final String treeId, final ContentType contentType, final Type type, final StorageParameters storageParameters) throws OXException {
        final Lock lock = readLockFor(treeId, storageParameters);
        lock.lock();
        try {
            final FolderStorage storage = registry.getFolderStorageByContentType(treeId, contentType);
            if (null == storage) {
                throw FolderExceptionErrorMessage.NO_STORAGE_FOR_CT.create(treeId, contentType);
            }
            final String folderId;
            final boolean started = storage.startTransaction(storageParameters, false);
            try {
                folderId = storage.getDefaultFolderID(user, treeId, contentType, type, storageParameters);
                if (started) {
                    storage.commitTransaction(storageParameters);
                }
            } catch (final OXException e) {
                if (started) {
                    storage.rollback(storageParameters);
                }
                throw e;
            } catch (final Exception e) {
                if (started) {
                    storage.rollback(storageParameters);
                }
                throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
            return folderId;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Type getTypeByParent(final User user, final String treeId, final String parentId, final StorageParameters storageParameters) throws OXException {
        final Lock lock = readLockFor(treeId, storageParameters);
        lock.lock();
        try {
            final FolderStorage storage = registry.getFolderStorage(treeId, parentId);
            if (null == storage) {
                throw FolderExceptionErrorMessage.NO_STORAGE_FOR_ID.create(treeId, parentId);
            }
            final Type type;
            final boolean started = storage.startTransaction(storageParameters, false);
            try {
                type = storage.getTypeByParent(user, treeId, parentId, storageParameters);
                if (started) {
                    storage.commitTransaction(storageParameters);
                }
            } catch (final OXException e) {
                if (started) {
                    storage.rollback(storageParameters);
                }
                throw e;
            } catch (final Exception e) {
                if (started) {
                    storage.rollback(storageParameters);
                }
                throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
            return type;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean containsForeignObjects(final User user, final String treeId, final String folderId, final StorageParameters storageParameters) throws OXException {
        final Lock lock = readLockFor(treeId, storageParameters);
        lock.lock();
        try {
            /*
             * Get folder storage
             */
            final FolderStorage storage = registry.getFolderStorage(treeId, folderId);
            if (null == storage) {
                throw FolderExceptionErrorMessage.NO_STORAGE_FOR_ID.create(treeId, folderId);
            }
            final boolean started = storage.startTransaction(storageParameters, false);
            try {
                final boolean containsForeignObjects = storage.containsForeignObjects(user, treeId, folderId, storageParameters);
                if (started) {
                    storage.commitTransaction(storageParameters);
                }
                return containsForeignObjects;
            } catch (final OXException e) {
                if (started) {
                    storage.rollback(storageParameters);
                }
                throw e;
            } catch (final Exception e) {
                if (started) {
                    storage.rollback(storageParameters);
                }
                throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isEmpty(final String treeId, final String folderId, final StorageParameters storageParameters) throws OXException {
        final Lock lock = readLockFor(treeId, storageParameters);
        lock.lock();
        try {
            /*
             * Get folder storage
             */
            final FolderStorage storage = registry.getFolderStorage(treeId, folderId);
            if (null == storage) {
                throw FolderExceptionErrorMessage.NO_STORAGE_FOR_ID.create(treeId, folderId);
            }
            final boolean started = storage.startTransaction(storageParameters, false);
            try {
                final boolean isEmpty = storage.isEmpty(treeId, folderId, storageParameters);
                if (started) {
                    storage.commitTransaction(storageParameters);
                }
                return isEmpty;
            } catch (final OXException e) {
                if (started) {
                    storage.rollback(storageParameters);
                }
                throw e;
            } catch (final Exception e) {
                if (started) {
                    storage.rollback(storageParameters);
                }
                throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void updateLastModified(final long lastModified, final String treeId, final String folderId, final StorageParameters storageParameters) throws OXException {
        final Lock lock = writeLockFor(treeId, storageParameters);
        lock.lock();
        try {
            final FolderStorage storage = registry.getFolderStorage(treeId, folderId);
            if (null == storage) {
                throw FolderExceptionErrorMessage.NO_STORAGE_FOR_ID.create(treeId, folderId);
            }
            final boolean started = storage.startTransaction(storageParameters, false);
            try {
                storage.updateLastModified(lastModified, treeId, folderId, storageParameters);
                if (started) {
                    storage.commitTransaction(storageParameters);
                }
            } catch (final OXException e) {
                if (started) {
                    storage.rollback(storageParameters);
                }
                throw e;
            } catch (final Exception e) {
                if (started) {
                    storage.rollback(storageParameters);
                }
                throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
            /*
             * Invalidate cache entry
             */
            removeFromCache(folderId, treeId, storageParameters.getSession(), newPathPerformer(storageParameters));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Folder getFolder(final String treeId, final String folderId, final StorageParameters storageParameters) throws OXException {
        return getFolder(treeId, folderId, StorageType.WORKING, storageParameters);
    }

    @Override
    public List<Folder> getFolders(final String treeId, final List<String> folderIds, final StorageParameters storageParameters) throws OXException {
        return getFolders(treeId, folderIds, StorageType.WORKING, storageParameters);
    }

    @Override
    public Folder getFolder(final String treeId, final String folderId, final StorageType storageType, final StorageParameters storageParameters) throws OXException {
        final Lock lock = readLockFor(treeId, storageParameters);
        lock.lock();
        try {
            /*
             * Try from cache
             */
            Folder folder = getCloneFromCache(treeId, folderId, storageParameters);
            if (null != folder) {
                return folder;
            }
            /*
             * Load folder from appropriate storage
             */
            folder = loadFolder(treeId, folderId, storageType, storageParameters);
            /*
             * Check if folder is cacheable
             */
            if (folder.isCacheable()) {
                /*
                 * Put to cache and return a cloned version
                 */
                putFolder(folder, treeId, storageParameters);
                return (Folder) folder.clone();
            }
            /*
             * Return as-is since not cached
             */
            return folder;
        } finally {
            lock.unlock();
        }
    }

    private Folder getCloneFromCache(final String treeId, final String folderId, final StorageParameters storageParameters) {
        final int contextId = storageParameters.getContextId();
        /*
         * Try global cache key
         */
        Folder folder = (Folder) globalCache.getFromGroup(newCacheKey(folderId, treeId), String.valueOf(contextId));
        if (null != folder) {
            /*
             * Return a cloned version from global cache
             */
            return (Folder) folder.clone();
        }
        /*
         * Try user cache key
         */
        final FolderMap folderMap = optFolderMapFor(storageParameters.getSession());
        if (null != folderMap) {
            folder = folderMap.get(folderId, treeId);
            if (null != folder) {
                if (!folderMap.contains(folderId, treeId) && (folder instanceof RemoveAfterAccessFolder)) {
                    /*-
                     * Folder does no more exist in cache and implements RemoveAfterAccessFolder marker interface
                     * 
                     * Re-load that folder
                     */
                    final Runnable task = new Runnable() {
                        
                        @Override
                        public void run() {
                            final Lock lock = readLockFor(treeId, storageParameters);
                            lock.lock();
                            try {
                                final StorageParameters params = newStorageParameters(storageParameters);
                                Folder loaded = loadFolder(treeId, folderId, StorageType.WORKING, params);
                                putFolder(loaded, treeId, params);
                                // Check for subfolders
                                final String[] subfolderIDs = loaded.getSubfolderIDs();
                                if (null != subfolderIDs) {
                                    for (final String subfolderId : subfolderIDs) {
                                        loaded = loadFolder(treeId, subfolderId, StorageType.WORKING, params);
                                        putFolder(loaded, treeId, params);
                                    }
                                }
                            } catch (final Exception e) {
                                LOG.debug(e.getMessage(), e);
                            } finally {
                                lock.unlock();
                            }
                        }
                    };
                    final ThreadPoolService threadPool = CacheServiceRegistry.getServiceRegistry().getService(ThreadPoolService.class);
                    threadPool.submit(ThreadPools.task(task), AbortBehavior.getInstance());
                }
                /*
                 * Return a cloned version from user-bound cache
                 */
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Locally loaded folder " + folderId + " from context " + contextId + " for user " + storageParameters.getUserId());
                }
                return (Folder) folder.clone();
            }
        }
        /*
         * Cache miss
         */
        return null;
    }

    @Override
    public List<Folder> getFolders(final String treeId, final List<String> folderIds, final StorageType storageType, final StorageParameters storageParameters) throws OXException {
        final Lock lock = readLockFor(treeId, storageParameters);
        lock.lock();
        try {
            final int size = folderIds.size();
            final Folder[] ret = new Folder[size];
            final TObjectIntMap<String> toLoad = new TObjectIntHashMap<String>(size);
            /*
             * Get the ones from cache
             */
            for (int i = 0; i < size; i++) {
                /*
                 * Try from cache
                 */
                final String folderId = folderIds.get(i);
                final Folder folder = getCloneFromCache(treeId, folderId, storageParameters);
                if (null == folder) {
                    /*
                     * Cache miss; Load from storage
                     */
                    toLoad.put(folderId, i);
                } else {
                    /*
                     * Cache hit
                     */
                    ret[i] = folder;
                }
            }
            /*
             * Load the ones from storage
             */
            final Map<String, Folder> fromStorage;
            if (toLoad.isEmpty()) {
                fromStorage = Collections.emptyMap();
            } else {
                fromStorage = loadFolders(treeId, Arrays.asList(toLoad.keys(new String[toLoad.size()])), storageType, storageParameters);
            }
            /*
             * Fill return value
             */
            for (final Entry<String, Folder> entry : fromStorage.entrySet()) {
                Folder folder = entry.getValue();
                final int index = toLoad.get(entry.getKey());
                /*
                 * Put into cache
                 */
                if (folder.isCacheable()) {
                    /*
                     * Put to cache and create a cloned version
                     */
                    putFolder(folder, treeId, storageParameters);
                    folder = (Folder) folder.clone();
                }
                ret[index] = folder;
            }
            /*
             * Return
             */
            final List<Folder> l = new ArrayList<Folder>(ret.length);
            for (final Folder folder : ret) {
                if (null != folder) {
                    l.add(folder);
                }
            }
            return l;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public FolderType getFolderType() {
        return CacheFolderType.getInstance();
    }

    @Override
    public StoragePriority getStoragePriority() {
        return StoragePriority.HIGHEST;
    }

    @Override
    public SortableId[] getVisibleFolders(final String treeId, final ContentType contentType, final Type type, final StorageParameters storageParameters) throws OXException {
        final Lock lock = readLockFor(treeId, storageParameters);
        lock.lock();
        try {
            final FolderStorage folderStorage = registry.getFolderStorageByContentType(treeId, contentType);
            if (null == folderStorage) {
                throw FolderExceptionErrorMessage.NO_STORAGE_FOR_CT.create(treeId, contentType);
            }
            final boolean started = folderStorage.startTransaction(storageParameters, true);
            try {
                final SortableId[] ret = folderStorage.getVisibleFolders(treeId, contentType, type, storageParameters);
                if (started) {
                    folderStorage.commitTransaction(storageParameters);
                }
                return ret;
            } catch (final OXException e) {
                if (started) {
                    folderStorage.rollback(storageParameters);
                }
                throw e;
            } catch (final Exception e) {
                if (started) {
                    folderStorage.rollback(storageParameters);
                }
                throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public SortableId[] getSubfolders(final String treeId, final String parentId, final StorageParameters storageParameters) throws OXException {
        final Lock lock = readLockFor(treeId, storageParameters);
        lock.lock();
        try {
            final Folder parent = getFolder(treeId, parentId, storageParameters);
            final String[] subfolders = parent.getSubfolderIDs();
            final SortableId[] ret;
            if (null == subfolders) {
                /*
                 * Get needed storages
                 */
                final FolderStorage[] neededStorages = registry.getFolderStoragesForParent(treeId, parentId);
                if (0 == neededStorages.length) {
                    return new SortableId[0];
                }
                try {
                    final java.util.List<SortableId> allSubfolderIds;
                    if (1 == neededStorages.length) {
                        final FolderStorage neededStorage = neededStorages[0];
                        final boolean started = neededStorage.startTransaction(storageParameters, false);
                        try {
                            allSubfolderIds = Arrays.asList(neededStorage.getSubfolders(treeId, parentId, storageParameters));
                            if (started) {
                                neededStorage.commitTransaction(storageParameters);
                            }
                        } catch (final Exception e) {
                            if (started) {
                                neededStorage.rollback(storageParameters);
                            }
                            throw e;
                        }
                    } else {
                        allSubfolderIds = new ArrayList<SortableId>(neededStorages.length * 8);
                        final CompletionService<java.util.List<SortableId>> completionService =
                            new ThreadPoolCompletionService<java.util.List<SortableId>>(
                                CacheServiceRegistry.getServiceRegistry().getService(ThreadPoolService.class));
                        /*
                         * Get all visible subfolders from each storage
                         */
                        for (final FolderStorage neededStorage : neededStorages) {
                            completionService.submit(new Callable<java.util.List<SortableId>>() {

                                @Override
                                public java.util.List<SortableId> call() throws Exception {
                                    final StorageParameters newParameters = newStorageParameters(storageParameters);
                                    final boolean started = neededStorage.startTransaction(newParameters, false);
                                    try {
                                        final java.util.List<SortableId> l =
                                            Arrays.asList(neededStorage.getSubfolders(treeId, parentId, newParameters));
                                        if (started) {
                                            neededStorage.commitTransaction(newParameters);
                                        }
                                        return l;
                                    } catch (final Exception e) {
                                        if (started) {
                                            neededStorage.rollback(newParameters);
                                        }
                                        throw e;
                                    }
                                }
                            });
                        }
                        /*
                         * Wait for completion
                         */
                        final List<List<SortableId>> results =
                            ThreadPools.takeCompletionService(completionService, neededStorages.length, FACTORY);
                        for (final List<SortableId> result : results) {
                            allSubfolderIds.addAll(result);
                        }
                    }
                    /*
                     * Sort them
                     */
                    Collections.sort(allSubfolderIds);
                    ret = allSubfolderIds.toArray(new SortableId[allSubfolderIds.size()]);
                } catch (final OXException e) {
                    throw e;
                } catch (final Exception e) {
                    throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
                }
            } else {
                ret = new SortableId[subfolders.length];
                for (int i = 0; i < ret.length; i++) {
                    ret[i] = new CacheSortableId(subfolders[i], i, null);
                }
            }
            return ret;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ContentType[] getSupportedContentTypes() {
        return new ContentType[0];
    }

    @Override
    public void rollback(final StorageParameters params) {
        // Nothing to do
    }

    @Override
    public boolean startTransaction(final StorageParameters parameters, final boolean modify) throws OXException {
        return false;
    }

    @Override
    public void updateFolder(final Folder folder, final StorageParameters storageParameters) throws OXException {
        final String treeId = folder.getTreeID();
        final Lock lock = writeLockFor(treeId, storageParameters);
        lock.lock();
        try {
            final Session session = storageParameters.getSession();
            /*
             * Perform update operation via non-cache storage
             */
            final String oldFolderId = folder.getID();
            Folder storageVersion = getCloneFromCache(treeId, oldFolderId, storageParameters);
            if (null == storageVersion) {
                storageVersion = getFolder(treeId, oldFolderId, storageParameters);
            }
            final boolean isMove = null != folder.getParentID();
            final String oldParentId = isMove ? getFolder(treeId, oldFolderId, storageParameters).getParentID() : null;
            if (null == session) {
                final UpdatePerformer updatePerformer = new UpdatePerformer(storageParameters.getUser(), storageParameters.getContext(), registry);
                updatePerformer.setCheck4Duplicates(false);
                updatePerformer.doUpdate(folder, storageParameters.getTimeStamp());
            } else {
                final UpdatePerformer updatePerformer = new UpdatePerformer(new ServerSessionAdapter(session), registry);
                updatePerformer.setCheck4Duplicates(false);
                updatePerformer.doUpdate(folder, storageParameters.getTimeStamp());
            }
            /*
             * Get folder from appropriate storage
             */
            final String newFolderId = folder.getID();
            /*
             * Refresh/Invalidate folder
             */
            final int userId = storageParameters.getUserId();
            if (isMove) {
                removeSingleFromCache(oldFolderId, treeId, userId, session, false);
                removeFromCache(oldParentId, treeId, session, newPathPerformer(storageParameters));
            } else {
                removeFromCache(newFolderId, treeId, session, newPathPerformer(storageParameters));
            }
            /*
             * Put updated folder
             */
            final Folder updatedFolder = loadFolder(treeId, newFolderId, StorageType.WORKING, true, storageParameters);
            {
                final int contextId = storageParameters.getContextId();
                final FolderMapManagement folderMapManagement = FolderMapManagement.getInstance();
                final TIntSet done = new TIntHashSet(16);
                for (final Permission permission : updatedFolder.getPermissions()) {
                    if (!permission.isGroup()) {
                        final int entity = permission.getEntity();
                        if (entity != userId) {
                            final FolderMap folderMap = folderMapManagement.optFor(entity, contextId);
                            if (folderMap != null) {
                                folderMap.remove(newFolderId, treeId);
                                if (!realTreeId.equals(treeId)) {
                                    folderMap.remove(newFolderId, realTreeId);
                                }
                                folderMap.remove(updatedFolder.getParentID(), treeId);
                                if (!realTreeId.equals(treeId)) {
                                    folderMap.remove(updatedFolder.getParentID(), realTreeId);
                                }
                            }
                            done.add(entity);
                        }
                    }
                }
                for (final Permission permission : storageVersion.getPermissions()) {
                    if (!permission.isGroup()) {
                        final int entity = permission.getEntity();
                        if (entity != userId && !done.contains(entity)) {
                            final FolderMap folderMap = folderMapManagement.optFor(entity, contextId);
                            if (folderMap != null) {
                                folderMap.remove(oldFolderId, treeId);
                                if (!realTreeId.equals(treeId)) {
                                    folderMap.remove(oldFolderId, realTreeId);
                                }
                                folderMap.remove(storageVersion.getParentID(), treeId);
                                if (!realTreeId.equals(treeId)) {
                                    folderMap.remove(storageVersion.getParentID(), realTreeId);
                                }
                            }
                        }
                    }
                }
            }
            if (isMove) {
                /*
                 * Invalidate new parent folder
                 */
                final String newParentId = updatedFolder.getParentID();
                if (null != newParentId && !newParentId.equals(oldParentId)) {
                    removeSingleFromCache(newParentId, treeId, userId, storageParameters.getSession(), false);
                }
                /*
                 * Reload folders
                 */
                Folder f = loadFolder(realTreeId, newFolderId, StorageType.WORKING, true, storageParameters);
                removeSingleFromCache(f.getParentID(), treeId, userId, storageParameters.getSession(), false);
                if (f.isCacheable()) {
                    putFolder(f, realTreeId, storageParameters);
                }
                f = loadFolder(realTreeId, oldParentId, StorageType.WORKING, true, storageParameters);
                if (f.isCacheable()) {
                    putFolder(f, realTreeId, storageParameters);
                }
                f = loadFolder(realTreeId, newParentId, StorageType.WORKING, true, storageParameters);
                if (f.isCacheable()) {
                    putFolder(f, realTreeId, storageParameters);
                }
            } else {
                final Folder f = loadFolder(realTreeId, newFolderId, StorageType.WORKING, true, storageParameters);
                if (f.isCacheable()) {
                    putFolder(f, realTreeId, storageParameters);
                }
            }
            if (updatedFolder.isCacheable()) {
                putFolder(updatedFolder, treeId, storageParameters);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean containsFolder(final String treeId, final String folderId, final StorageParameters storageParameters) throws OXException {
        return containsFolder(treeId, folderId, StorageType.WORKING, storageParameters);
    }

    @Override
    public String[] getModifiedFolderIDs(final String treeId, final Date timeStamp, final ContentType[] includeContentTypes, final StorageParameters storageParameters) throws OXException {
        return getChangedFolderIDs(0, treeId, timeStamp, includeContentTypes, storageParameters);
    }

    @Override
    public String[] getDeletedFolderIDs(final String treeId, final Date timeStamp, final StorageParameters storageParameters) throws OXException {
        return getChangedFolderIDs(1, treeId, timeStamp, null, storageParameters);
    }

    private String[] getChangedFolderIDs(final int index, final String treeId, final Date timeStamp, final ContentType[] includeContentTypes, final StorageParameters storageParameters) throws OXException {
        final Lock lock = readLockFor(treeId, storageParameters);
        lock.lock();
        try {
            final Session session = storageParameters.getSession();
            /*
             * Perform update operation via non-cache storage
             */
            final UserizedFolder[] folders;
            final boolean ignoreDelete = index == 0;
            if (null == session) {
                folders =
                    new UpdatesPerformer(
                        storageParameters.getUser(),
                        storageParameters.getContext(),
                        storageParameters.getDecorator(),
                        registry).doUpdates(treeId, timeStamp, ignoreDelete, includeContentTypes)[index];
            } else {
                try {
                    folders =
                        new UpdatesPerformer(new ServerSessionAdapter(session), storageParameters.getDecorator(), registry).doUpdates(
                            treeId,
                            timeStamp,
                            ignoreDelete,
                            includeContentTypes)[index];
                } catch (final OXException e) {
                    throw new OXException(e);
                }
            }
            if (null == folders || folders.length == 0) {
                return new String[0];
            }
            final String[] ids = new String[folders.length];
            for (int i = 0; i < ids.length; i++) {
                ids[i] = folders[i].getID();
            }
            return ids;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean containsFolder(final String treeId, final String folderId, final StorageType storageType, final StorageParameters storageParameters) throws OXException {
        final Lock lock = readLockFor(treeId, storageParameters);
        lock.lock();
        try {
            final FolderStorage storage = registry.getFolderStorage(treeId, folderId);
            if (null == storage) {
                throw FolderExceptionErrorMessage.NO_STORAGE_FOR_ID.create(treeId, folderId);
            }
            final boolean started = storage.startTransaction(storageParameters, true);
            try {
                final boolean contains = storage.containsFolder(treeId, folderId, storageType, storageParameters);
                if (started) {
                    storage.commitTransaction(storageParameters);
                }
                return contains;
            } catch (final OXException e) {
                if (started) {
                    storage.rollback(storageParameters);
                }
                throw e;
            } catch (final Exception e) {
                if (started) {
                    storage.rollback(storageParameters);
                }
                throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
        } finally {
            lock.unlock();
        }
    }

    /*-
     * ++++++++++++++++++++++++++++++++++++ ++ + HELPERS + ++ ++++++++++++++++++++++++++++++++++++
     */

    /**
     * Creates the cache key for specified folder ID and tree ID pair.
     *
     * @param folderId The folder ID
     * @param treeId The tree ID
     * @return The cache key
     */
    private CacheKey newCacheKey(final String folderId, final String treeId) {
        return cacheService.newCacheKey(1, treeId, folderId);
    }

    /**
     * Creates a user-bound key.
     */
    private CacheKey newCacheKey(final String folderId, final String treeId, final int cid, final int user) {
        return cacheService.newCacheKey(cid, Integer.valueOf(user), treeId, folderId);
    }

    private boolean existsFolder(final String treeId, final String folderId, final StorageType storageType, final StorageParameters storageParameters) throws OXException {
        final FolderStorage storage = registry.getFolderStorage(treeId, folderId);
        if (null == storage) {
            throw FolderExceptionErrorMessage.NO_STORAGE_FOR_ID.create(treeId, folderId);
        }
        final boolean started = storage.startTransaction(storageParameters, false);
        try {
            final boolean exists = storage.containsFolder(treeId, folderId, storageType, storageParameters);
            if (started) {
                storage.commitTransaction(storageParameters);
            }
            return exists;
        } catch (final OXException e) {
            if (started) {
                storage.rollback(storageParameters);
            }
            throw e;
        } catch (final Exception e) {
            if (started) {
                storage.rollback(storageParameters);
            }
            throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    protected Folder loadFolder(final String treeId, final String folderId, final StorageType storageType, final StorageParameters storageParameters) throws OXException {
        return loadFolder(treeId, folderId, storageType, false, storageParameters);
    }

    private Folder loadFolder(final String treeId, final String folderId, final StorageType storageType, final boolean readWrite, final StorageParameters storageParameters) throws OXException {
        final FolderStorage storage = registry.getFolderStorage(treeId, folderId);
        if (null == storage) {
            throw FolderExceptionErrorMessage.NO_STORAGE_FOR_ID.create(treeId, folderId);
        }
        final boolean started = storage.startTransaction(storageParameters, readWrite);
        try {
            final Folder folder = storage.getFolder(treeId, folderId, storageType, storageParameters);
            if (started) {
                storage.commitTransaction(storageParameters);
            }
            return folder;
        } catch (final OXException e) {
            if (started) {
                storage.rollback(storageParameters);
            }
            throw e;
        } catch (final Exception e) {
            if (started) {
                storage.rollback(storageParameters);
            }
            throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    private Map<String, Folder> loadFolders(final String treeId, final List<String> folderIds, final StorageType storageType, final StorageParameters storageParameters) throws OXException {
        /*
         * Collect by folder storage
         */
        final int size = folderIds.size();
        final Map<FolderStorage, TIntList> map = new HashMap<FolderStorage, TIntList>(4);
        for (int i = 0; i < size; i++) {
            final String id = folderIds.get(i);
            final FolderStorage tmp = registry.getFolderStorage(treeId, id);
            if (null == tmp) {
                throw FolderExceptionErrorMessage.NO_STORAGE_FOR_ID.create(treeId, id);
            }
            TIntList list = map.get(tmp);
            if (null == list) {
                list = new TIntArrayList();
                map.put(tmp, list);
            }
            list.add(i);
        }
        /*
         * Process by folder storage
         */
        final CompletionService<Object> completionService;
        final StorageParametersProvider paramsProvider;
        if (1 == map.size()) {
            completionService = new CallerRunsCompletionService<Object>();
            paramsProvider = new InstanceStorageParametersProvider(storageParameters);
        } else {
            completionService =
                new ThreadPoolCompletionService<Object>(CacheServiceRegistry.getServiceRegistry().getService(ThreadPoolService.class, true));

            final Session session = storageParameters.getSession();
            paramsProvider =
                null == session ? new SessionStorageParametersProvider(storageParameters.getUser(), storageParameters.getContext()) : new SessionStorageParametersProvider(
                    (ServerSession) storageParameters.getSession());
        }
        /*
         * Create destination map
         */
        final Map<String, Folder> ret = new ConcurrentHashMap<String, Folder>(size);
        int taskCount = 0;
        for (final java.util.Map.Entry<FolderStorage, TIntList> entry : map.entrySet()) {
            final FolderStorage tmp = entry.getKey();
            final int[] indexes = entry.getValue().toArray();
            completionService.submit(new Callable<Object>() {

                @Override
                public Object call() throws Exception {
                    final StorageParameters newParameters = paramsProvider.getStorageParameters();
                    final List<FolderStorage> openedStorages = new ArrayList<FolderStorage>(2);
                    if (tmp.startTransaction(newParameters, false)) {
                        openedStorages.add(tmp);
                    }
                    try {
                        /*
                         * Create the list of IDs to load with current storage
                         */
                        final List<String> ids = new ArrayList<String>(indexes.length);
                        for (final int index : indexes) {
                            ids.add(folderIds.get(index));
                        }
                        /*
                         * Load them & commit
                         */
                        final List<Folder> folders = tmp.getFolders(treeId, ids, storageType, newParameters);
                        for (final FolderStorage fs : openedStorages) {
                            fs.commitTransaction(newParameters);
                        }
                        /*
                         * Fill into map
                         */
                        for (final Folder folder : folders) {
                            ret.put(folder.getID(), folder);
                        }
                        /*
                         * Return
                         */
                        return null;
                    } catch (final OXException e) {
                        for (final FolderStorage fs : openedStorages) {
                            fs.rollback(newParameters);
                        }
                        throw e;
                    } catch (final RuntimeException e) {
                        for (final FolderStorage fs : openedStorages) {
                            fs.rollback(newParameters);
                        }
                        throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e);
                    }
                }
            });
            taskCount++;
        }
        /*
         * Wait for completion
         */
        ThreadPools.takeCompletionService(completionService, taskCount, FACTORY);
        return ret;
    }

    /**
     * Creates a new storage parameter instance.
     *
     * @return A new storage parameter instance.
     */
    static StorageParameters newStorageParameters(final StorageParameters source) {
        final Session session = source.getSession();
        if (null == session) {
            return new StorageParametersImpl(source.getUser(), source.getContext());
        }
        return new StorageParametersImpl((ServerSession) session);
    }

    protected static Lock readLockFor(final String treeId, final StorageParameters params) {
        return lockFor(treeId, params).readLock();
    }

    protected static Lock writeLockFor(final String treeId, final StorageParameters params) {
        return lockFor(treeId, params).writeLock();
    }

    private static ReadWriteLock lockFor(final String treeId, final StorageParameters params) {
        return TreeLockManagement.getInstance().getFor(treeId, params.getUserId(), params.getContextId());
    }

    private static FolderMap getFolderMapFor(final Session session) {
        return FolderMapManagement.getInstance().getFor(session);
    }

    private static FolderMap optFolderMapFor(final Session session) {
        return FolderMapManagement.getInstance().optFor(session);
    }

    /**
     * Drops entries associated with specified user in given context.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    public static void dropUserEntries(final int userId, final int contextId) {
        final FolderMap folderMap = FolderMapManagement.getInstance().optFor(userId, contextId);
        if (null != folderMap) {
            folderMap.clear();
        }
    }

}
