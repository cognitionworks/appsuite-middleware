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

package com.openexchange.tools.iterator;

import gnu.trove.TIntHashSet;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import com.openexchange.api2.OXException;
import com.openexchange.cache.impl.FolderCacheManager;
import com.openexchange.cache.impl.FolderCacheNotEnabledException;
import com.openexchange.caching.CacheException;
import com.openexchange.caching.ElementAttributes;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.configuration.ServerConfig.Property;
import com.openexchange.database.DBPoolingException;
import com.openexchange.databaseold.Database;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.EnumComponent;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.server.ServiceException;
import com.openexchange.server.impl.DBPool;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.ThreadPools;
import com.openexchange.threadpool.behavior.CallerRunsBehavior;
import com.openexchange.tools.iterator.SearchIteratorException.Code;
import com.openexchange.tools.oxfolder.OXFolderProperties;

/**
 * {@link FolderObjectIterator} - A {@link SearchIterator} especially for instances of {@link FolderObject}.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class FolderObjectIterator implements SearchIterator<FolderObject> {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(FolderObjectIterator.class);

    /**
     * The empty folder iterator
     */
    public static final FolderObjectIterator EMPTY_FOLDER_ITERATOR = new FolderObjectIterator() {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public FolderObject next() {
            return null;
        }

        @Override
        public void close() {
            // Nothing to close
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean hasSize() {
            return true;
        }

        @Override
        public boolean hasWarnings() {
            return false;
        }

        @Override
        public AbstractOXException[] getWarnings() {
            return null;
        }

        @Override
        public void addWarning(final AbstractOXException warning) {
            // Nothing to add
        }
    };

    private final Queue<FolderObject> prefetchQueue;

    private boolean isClosed;

    private final boolean closeCon;

    private final TIntHashSet folderIds;

    private FolderObject next;

    private Statement stmt;

    private ResultSet rs;

    private Connection readCon;

    private final Context ctx;

    private ElementAttributes attribs;

    private final boolean resideInCache;

    private final boolean containsPermissions;

    private final HashMap<Integer, FolderObject> folders;

    private final List<AbstractOXException> warnings;

    private FolderObject future;

    private final PermissionLoader permissionLoader;

    private static final String[] selectFields = {
        "fuid", "parent", "fname", "module", "type", "creating_date", "created_from", "changing_date", "changed_from", "permission_flag",
        "subfolder_flag", "default_flag" };

    /**
     * Gets all necessary fields in right order to be used in an SQL <i>SELECT</i> statement needed to create instances of
     * {@link FolderObject}.
     * 
     * @param tableAlias The table alias used throughout corresponding SQL <i>SELECT</i> statement or <code>null</code> if no alias used.
     * @return All necessary fields in right order to be used in an SQL <i>SELECT</i> statement
     */
    public static final String getFieldsForSQL(final String tableAlias) {
        final StringBuilder fields = new StringBuilder(128);
        final String delim = ", ";
        if (tableAlias != null) {
            final String prefix = fields.append(tableAlias).append('.').toString();
            fields.setLength(0);
            fields.append(prefix).append(selectFields[0]);
            for (int i = 1; i < selectFields.length; i++) {
                fields.append(delim).append(prefix).append(selectFields[i]);
            }
        } else {
            fields.append(selectFields[0]);
            for (int i = 1; i < selectFields.length; i++) {
                fields.append(delim).append(selectFields[i]);
            }
        }
        return fields.toString();
    }

    private static final String[] selectFieldsPerm = { "permission_id", "fp", "orp", "owp", "odp", "admin_flag", "group_flag", "system" };

    /**
     * Gets all necessary fields in right order to be used in an SQL <i>SELECT</i> statement needed to create instances of
     * {@link FolderObject} with permissions applied.
     * 
     * @param folderAlias The table alias for folder used throughout corresponding SQL <i>SELECT</i> statement or <code>null</code> if no
     *            alias used.
     * @param permAlias The table alias for permissions used throughout corresponding SQL <i>SELECT</i> statement or <code>null</code> if no
     *            alias used.
     * @return All necessary fields in right order to be used in an SQL <i>SELECT</i> statement
     */
    public static final String getFieldsForSQLWithPermissions(final String folderAlias, final String permAlias) {
        final StringBuilder fields = new StringBuilder(256);
        final String delim = ", ";
        if (permAlias != null) {
            final String prefix = fields.append(permAlias).append('.').toString();
            fields.setLength(0);
            fields.append(prefix).append(selectFieldsPerm[0]);
            for (int i = 1; i < selectFieldsPerm.length; i++) {
                fields.append(delim).append(prefix).append(selectFieldsPerm[i]);
            }
        } else {
            fields.append(selectFieldsPerm[0]);
            for (int i = 1; i < selectFieldsPerm.length; i++) {
                fields.append(delim).append(selectFieldsPerm[i]);
            }
        }
        final String permFields = fields.toString();
        fields.setLength(0);
        /*
         * Now folder fields
         */
        if (folderAlias != null) {
            final String prefix = fields.append(folderAlias).append('.').toString();
            fields.setLength(0);
            fields.append(prefix).append(selectFields[0]);
            for (int i = 1; i < selectFields.length; i++) {
                fields.append(delim).append(prefix).append(selectFields[i]);
            }
        } else {
            fields.append(selectFields[0]);
            for (int i = 1; i < selectFields.length; i++) {
                fields.append(delim).append(selectFields[i]);
            }
        }
        /*
         * Append permission fields & return
         */
        return fields.append(delim).append(permFields).toString();
    }

    /**
     * Initializes a new {@link FolderObjectIterator}
     */
    FolderObjectIterator() {
        super();
        closeCon = false;
        resideInCache = false;
        containsPermissions = false;
        ctx = null;
        prefetchQueue = null;
        folderIds = null;
        folders = null;
        warnings = new ArrayList<AbstractOXException>(2);
        permissionLoader = null;
    }

    /**
     * Initializes a new {@link FolderObjectIterator} from specified collection.
     * 
     * @param col The collection containing instances of {@link FolderObject}
     * @param resideInCache If objects shall reside in cache permanently or shall be removed according to cache policy
     */
    public FolderObjectIterator(final Collection<FolderObject> col, final boolean resideInCache) {
        super();
        folderIds = null;
        folders = null;
        warnings = new ArrayList<AbstractOXException>(2);
        rs = null;
        stmt = null;
        ctx = null;
        closeCon = false;
        containsPermissions = false;
        this.resideInCache = resideInCache;
        if ((col == null) || col.isEmpty()) {
            next = null;
            prefetchQueue = null;
        } else {
            prefetchQueue = new LinkedList<FolderObject>(col);
            next = prefetchQueue.poll();
        }
        permissionLoader = null;
    }

    /**
     * Initializes a new {@link FolderObjectIterator}
     * 
     * @param rs The result set providing selected folder data
     * @param stmt The fired statement (to release all resources on iterator end)
     * @param resideInCache If objects shall reside in cache permanently or shall be removed according to cache policy
     * @param ctx The context
     * @param readCon A connection holding at least read capability
     * @param closeCon Whether to close given connection or not
     * @throws SearchIteratorException If instantiation fails.
     */
    public FolderObjectIterator(final ResultSet rs, final Statement stmt, final boolean resideInCache, final Context ctx, final Connection readCon, final boolean closeCon) throws SearchIteratorException {
        this(rs, stmt, resideInCache, false, ctx, readCon, closeCon);
    }

    /**
     * Initializes a new {@link FolderObjectIterator}
     * 
     * @param rs The result set providing selected folder data
     * @param stmt The fired statement (to release all resources on iterator end)
     * @param resideInCache If objects shall reside in cache permanently or shall be removed according to cache policy
     * @param containsPermissions If result set contains duplicates because of selected folder permissions; otherwise <code>false</code>
     * @param ctx The context
     * @param readCon A connection holding at least read capability
     * @param closeCon Whether to close given connection or not
     * @throws SearchIteratorException If instantiation fails.
     */
    public FolderObjectIterator(final ResultSet rs, final Statement stmt, final boolean resideInCache, final boolean containsPermissions, final Context ctx, final Connection readCon, final boolean closeCon) throws SearchIteratorException {
        super();
        if (OXFolderProperties.isEnableDBGrouping()) {
            folderIds = null;
        } else {
            folderIds = new TIntHashSet();
        }
        folders = containsPermissions ? new LinkedHashMap<Integer, FolderObject>(32) : null;
        warnings = new ArrayList<AbstractOXException>(2);
        this.rs = rs;
        this.stmt = stmt;
        this.readCon = readCon;
        this.ctx = ctx;
        this.closeCon = closeCon;
        this.resideInCache = resideInCache;
        this.containsPermissions = containsPermissions;
        if (containsPermissions) {
            permissionLoader = null;
        } else {
            permissionLoader = new PermissionLoader(ctx);
        }
        /*
         * Set next to first result set entry
         */
        final boolean prefetchEnabled = ServerConfig.getBoolean(Property.PrefetchEnabled);
        try {
            if (this.rs.next()) {
                next = createFolderObjectFromSelectedEntry();
            } else if (!prefetchEnabled) {
                closeResources();
            }
        } catch (final SQLException e) {
            throw new SearchIteratorException(Code.SQL_ERROR, e, EnumComponent.FOLDER, e.getMessage());
        } catch (final DBPoolingException e) {
            throw new SearchIteratorException(Code.DBPOOLING_ERROR, e, EnumComponent.FOLDER, e.getMessage());
        }
        if (prefetchEnabled) {
            prefetchQueue = new LinkedList<FolderObject>();
            /*
             * ResultSet prefetch is enabled. Fill iterator with whole ResultSet's content
             */
            try {
                while (this.rs.next()) {
                    FolderObject fo = createFolderObjectFromSelectedEntry();
                    while ((fo == null) && rs.next()) {
                        fo = createFolderObjectFromSelectedEntry();
                    }
                    prefetchQueue.offer(fo);
                }
                if (future != null) {
                    prefetchQueue.offer(future);
                    future = null;
                }
            } catch (final SQLException e) {
                throw new SearchIteratorException(Code.SQL_ERROR, e, EnumComponent.FOLDER, e.getMessage());
            } catch (final DBPoolingException e) {
                throw new SearchIteratorException(Code.DBPOOLING_ERROR, e, EnumComponent.FOLDER, e.getMessage());
            } finally {
                closeResources();
            }
        } else {
            prefetchQueue = null;
        }
    }

    private final ElementAttributes getEternalAttributes() throws FolderCacheNotEnabledException, CacheException, OXException {
        if (attribs == null) {
            attribs = FolderCacheManager.getInstance().getDefaultFolderObjectAttributes();
            attribs.setIdleTime(-1); // eternal
            attribs.setMaxLifeSeconds(-1); // eternal
            attribs.setIsEternal(true);
        }
        return attribs.copy();
    }

    /**
     * @return a <code>FolderObject</code> from current <code>ResultSet.next()</code> data
     */
    private final FolderObject createFolderObjectFromSelectedEntry() throws SQLException, DBPoolingException {
        // fname, fuid, module, type, creator
        final int folderId = rs.getInt(1);
        final FolderObject fo;
        if (containsPermissions) {
            /*
             * No cache look-up in this mode to not being confused with result set state
             */
            final Integer key = Integer.valueOf(folderId);
            FolderObject current = folders.get(key);
            if (null == current) {
                current = createNewFolderObject(folderId);
                folders.put(key, current);
                addPermissions(folderId, current);
            } else {
                addPermissions(folderId, current);
            }
            fo = current;
        } else {
            if (!OXFolderProperties.isEnableDBGrouping()) {
                if (folderIds.contains(folderId)) {
                    return null;
                }
                folderIds.add(folderId);
            }
            /*
             * Look up cache
             */
            if (FolderCacheManager.isInitialized()) {
                try {
                    final FolderObject fld = FolderCacheManager.getInstance().getFolderObject(folderId, ctx);
                    if (fld != null) {
                        return fld;
                    }
                } catch (final OXException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            /*
             * Not in cache; create from read data
             */
            fo = createNewFolderObject(folderId);
            /*
             * Read & set permissions
             */
            if (null == permissionLoader) {
                fo.setPermissionsAsArray(FolderObject.getFolderPermissions(folderId, ctx, readCon));
            } else {
                permissionLoader.submitPermissionsFor(folderId);
            }
        }
        return fo;
    }

    private FolderObject createNewFolderObject(final int folderId) throws SQLException {
        final FolderObject fo = new FolderObject(rs.getString(3), folderId, rs.getInt(4), rs.getInt(5), rs.getInt(7));
        fo.setParentFolderID(rs.getInt(2)); // parent
        long tStmp = rs.getLong(6); // creating_date
        if (rs.wasNull()) {
            fo.setCreationDate(new Date());
        } else {
            fo.setCreationDate(new Date(tStmp));
        }
        fo.setCreatedBy(rs.getInt(7)); // created_from
        tStmp = rs.getLong(8); // changing_date
        if (rs.wasNull()) {
            fo.setLastModified(new Date());
        } else {
            fo.setLastModified(new Date(tStmp));
        }
        fo.setModifiedBy(rs.getInt(9)); // changed_from
        fo.setPermissionFlag(rs.getInt(10));
        int subfolder = rs.getInt(11);
        if (rs.wasNull()) {
            subfolder = 0;
        }
        fo.setSubfolderFlag(subfolder > 0);
        int defaultFolder = rs.getInt(12);
        if (rs.wasNull()) {
            defaultFolder = 0;
        }
        fo.setDefaultFolder(defaultFolder > 0);
        return fo;
    }

    private void addPermissions(final int folderId, final FolderObject current) throws SQLException {
        future = null;
        addNewPermission(current);
        /*
         * Read all available permissions for current folder
         */
        boolean hasNext;
        while ((hasNext = rs.next()) && folderId == rs.getInt(1)) {
            addNewPermission(current);
        }
        if (hasNext) {
            final int fuid = rs.getInt(1);
            future = createNewFolderObject(fuid);
            folders.put(Integer.valueOf(fuid), future);
            /*
             * Add first available permission from current row
             */
            addNewPermission(future);
        }
    }

    private void addNewPermission(final FolderObject current) throws SQLException {
        final OCLPermission p = new OCLPermission();
        p.setEntity(rs.getInt(13)); // Entity
        p.setAllPermission(rs.getInt(14), rs.getInt(15), rs.getInt(16), rs.getInt(17)); // fp, orp, owp, and odp
        p.setFolderAdmin(rs.getInt(18) > 0 ? true : false); // admin_flag
        p.setGroupPermission(rs.getInt(19) > 0 ? true : false); // group_flag
        p.setSystem(rs.getInt(20)); // system
        current.addPermission(p);
    }

    private final void closeResources() throws SearchIteratorException {
        SearchIteratorException error = null;
        /*
         * Close ResultSet
         */
        if (rs != null) {
            try {
                rs.close();
            } catch (final SQLException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(e.getMessage(), e);
                }
                error = new SearchIteratorException(Code.SQL_ERROR, e, EnumComponent.FOLDER, e.getMessage());
            }
            rs = null;
        }
        /*
         * Close Statement
         */
        if (stmt != null) {
            try {
                stmt.close();
            } catch (final SQLException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(e.getMessage(), e);
                }
                if (error == null) {
                    error = new SearchIteratorException(Code.SQL_ERROR, e, EnumComponent.FOLDER, e.getMessage());
                }
            }
            stmt = null;
        }
        /*
         * Close connection
         */
        if (closeCon && (readCon != null)) {
            DBPool.push(ctx, readCon);
            readCon = null;
        }
        if (error != null) {
            throw error;
        }
    }

    public boolean hasNext() {
        if (isClosed) {
            return false;
        }
        return next != null;
    }

    public FolderObject next() throws SearchIteratorException {
        if (isClosed) {
            throw new SearchIteratorException(Code.CLOSED, EnumComponent.FOLDER);
        }
        try {
            final FolderObject retval = prepareFolderObject(next);
            next = null;
            if (prefetchQueue == null) {
                /*
                 * Select next from underlying ResultSet
                 */
                if (rs.next()) {
                    next = createFolderObjectFromSelectedEntry();
                    while ((next == null) && rs.next()) {
                        next = createFolderObjectFromSelectedEntry();
                    }
                    if (next == null) {
                        close();
                    }
                } else {
                    next = future;
                    close();
                }
            } else {
                /*
                 * Select next from queue
                 */
                if (!prefetchQueue.isEmpty()) {
                    next = prefetchQueue.poll();
                    while ((next == null) && !prefetchQueue.isEmpty()) {
                        next = prefetchQueue.poll();
                    }
                }
            }
            return retval;
        } catch (final SQLException e) {
            throw new SearchIteratorException(Code.SQL_ERROR, e, EnumComponent.FOLDER, e.getMessage());
        } catch (final DBPoolingException e) {
            throw new SearchIteratorException(Code.DBPOOLING_ERROR, e, EnumComponent.FOLDER, e.getMessage());
        }
    }

    private FolderObject prepareFolderObject(final FolderObject fo) throws SearchIteratorException {
        if (null == fo) {
            return null;
        }
        final int folderId = fo.getObjectID();
        if (!fo.containsPermissions()) {
            /*
             * No permissions set, yet
             */
            final OCLPermission[] permissions = null == permissionLoader ? null : permissionLoader.pollPermissionsFor(folderId, 2000L);
            try {
                fo.setPermissionsAsArray(null == permissions ? FolderObject.getFolderPermissions(folderId, ctx, readCon) : permissions);
            } catch (final DBPoolingException e) {
                throw new SearchIteratorException(e);
            } catch (final SQLException e) {
                throw new SearchIteratorException(Code.SQL_ERROR, e, EnumComponent.FOLDER, e.getMessage());
            }
        }
        /*
         * Determine if folder object should be put into cache or not
         */
        if (null != ctx && FolderCacheManager.isInitialized()) {
            try {
                FolderCacheManager.getInstance().putIfAbsent(fo, ctx, resideInCache ? getEternalAttributes() : null);
            } catch (final FolderCacheNotEnabledException e) {
                LOG.error(e.getMessage(), e);
            } catch (final OXException e) {
                LOG.error(e.getMessage(), e);
            } catch (final CacheException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        /*
         * Return prepared folder object
         */
        return fo;
    }

    public void close() {
        if (isClosed) {
            return;
        }
        next = null;
        try {
            closeResources();
        } catch (final SearchIteratorException e) {
            LOG.error(e.getMessage(), e);
        }
        /*
         * Close other stuff
         */
        if (null != prefetchQueue) {
            prefetchQueue.clear();
        }
        if (null != folderIds) {
            folderIds.clear();
        }
        if (null != folders) {
            folders.clear();
        }
        if (null != permissionLoader) {
            permissionLoader.close();
        }
        isClosed = true;
    }

    public int size() {
        if (prefetchQueue != null) {
            return prefetchQueue.size() + (next == null ? 0 : 1);
        }
        throw new UnsupportedOperationException("Method size() not implemented");
    }

    public boolean hasSize() {
        /*
         * Size can be predicted if prefetch queue is not null
         */
        return (prefetchQueue != null);
    }

    public void addWarning(final AbstractOXException warning) {
        warnings.add(warning);
    }

    public AbstractOXException[] getWarnings() {
        return warnings.isEmpty() ? null : warnings.toArray(new AbstractOXException[warnings.size()]);
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Creates a <code>java.util.Queue</code> containing all iterator's elements. All resources are closed immediately.
     * 
     * @return iterator's content backed up by a <code>java.util.Queue</code>
     * @throws SearchIteratorException if any error occurs
     */
    public Queue<FolderObject> asQueue() throws SearchIteratorException {
        return asLinkedList();
    }

    /**
     * Creates a <code>java.util.List</code> containing all iterator's elements. All resources are closed immediately.
     * 
     * @return iterator's content backed up by a <code>java.util.List</code>
     * @throws SearchIteratorException if any error occurs
     */
    public List<FolderObject> asList() throws SearchIteratorException {
        return asLinkedList();
    }

    private LinkedList<FolderObject> asLinkedList() throws SearchIteratorException {
        final LinkedList<FolderObject> retval = new LinkedList<FolderObject>();
        if (isClosed) {
            return retval;
        }
        try {
            if (next == null) {
                return retval;
            }
            retval.add(prepareFolderObject(next));
            if (prefetchQueue != null) {
                while (!prefetchQueue.isEmpty()) {
                    retval.add(prepareFolderObject(prefetchQueue.poll()));
                }
                return retval;
            }
            while (rs.next()) {
                FolderObject fo = createFolderObjectFromSelectedEntry();
                while ((fo == null) && rs.next()) {
                    fo = createFolderObjectFromSelectedEntry();
                }
                if (fo != null) {
                    retval.offer(prepareFolderObject(fo));
                }
            }
            return retval;
        } catch (final DBPoolingException e) {
            throw new SearchIteratorException(Code.DBPOOLING_ERROR, e, EnumComponent.FOLDER, e.getMessage());
        } catch (final SQLException e) {
            throw new SearchIteratorException(Code.SQL_ERROR, e, EnumComponent.FOLDER, e.getMessage());
        } finally {
            next = null;
            try {
                closeResources();
            } catch (final SearchIteratorException e) {
                LOG.error(e.getMessage(), e);
            }
            /*
             * Close other stuff
             */
            if (null != prefetchQueue) {
                prefetchQueue.clear();
            }
            if (null != folderIds) {
                folderIds.clear();
            }
            if (null != folders) {
                folders.clear();
            }
            if (null != permissionLoader) {
                permissionLoader.close();
            }
            isClosed = true;
        }
    }

    private static final class PermissionLoader {

        private final ConcurrentMap<Integer, Future<OCLPermission[]>> permsMap;

        private final BlockingQueue<Integer> queue;

        private final Future<Object> mainFuture;

        private final AtomicBoolean flag;

        public PermissionLoader(final Context ctx) throws SearchIteratorException {
            super();
            final AtomicBoolean flag = new AtomicBoolean(true);
            this.flag = new AtomicBoolean();
            final ConcurrentMap<Integer, Future<OCLPermission[]>> m = new ConcurrentHashMap<Integer, Future<OCLPermission[]>>();
            permsMap = m;
            final BlockingQueue<Integer> q = new LinkedBlockingQueue<Integer>();
            queue = q;
            try {
                final ThreadPoolService tps = ServerServiceRegistry.getInstance().getService(ThreadPoolService.class, true);
                mainFuture = tps.submit(ThreadPools.task(new Callable<Object>() {

                    public Object call() throws Exception {
                        try {
                            while (flag.get()) {
                                final Integer folderId = q.take();
                                /*
                                 * Add future to concurrent map
                                 */
                                final FutureTask<OCLPermission[]> f = new FutureTask<OCLPermission[]>(new Callable<OCLPermission[]>() {
        
                                    public OCLPermission[] call() throws Exception {
                                        final Connection readCon = Database.get(ctx, false);
                                        try {
                                            return FolderObject.getFolderPermissions(folderId.intValue(), ctx, readCon);
                                        } catch (final SQLException e) {
                                            throw new SearchIteratorException(Code.SQL_ERROR, e, EnumComponent.FOLDER, e.getMessage());
                                        } finally {
                                            Database.back(ctx, false, readCon);
                                        }
                                    }
                                });
                                m.put(folderId, f);
                                /*
                                 * Execute task with this thread
                                 */
                                f.run();
                            }
                            /*
                             * Return
                             */
                            return null;
                        } catch (final InterruptedException e) {
                            throw e;
                        }
                    }
                }), CallerRunsBehavior.<Object> getInstance());

            } catch (final ServiceException e) {
                throw new SearchIteratorException(e);
            }
        }

        public void close() {
            flag.set(false);
            mainFuture.cancel(true);
            queue.clear();
            permsMap.clear();
        }

        public void submitPermissionsFor(final int folderId) {
            queue.offer(Integer.valueOf(folderId));
        }

        public OCLPermission[] pollPermissionsFor(final int folderId, final long timeoutMsec) throws SearchIteratorException {
            final Future<OCLPermission[]> f = permsMap.get(Integer.valueOf(folderId));
            if (null == f) {
                return null;
            }
            try {
                return f.get(timeoutMsec, TimeUnit.MILLISECONDS);
            } catch (final InterruptedException e) {
                throw new SearchIteratorException(Code.UNEXPECTED_ERROR, e, EnumComponent.FOLDER, e.getMessage());
            } catch (final ExecutionException e) {
                throw new SearchIteratorException(ThreadPools.launderThrowable(e, AbstractOXException.class));
            } catch (final TimeoutException e) {
                /*
                 * Wait timed out
                 */
                return null;
            }
        }

    } // End of PermissionLoader
}
