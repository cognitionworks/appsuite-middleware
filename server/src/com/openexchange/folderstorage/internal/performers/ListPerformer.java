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

package com.openexchange.folderstorage.internal.performers;

import static com.openexchange.server.services.ServerServiceRegistry.getInstance;
import gnu.trove.TIntArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.concurrent.CallerRunsCompletionService;
import com.openexchange.folderstorage.Folder;
import com.openexchange.folderstorage.FolderException;
import com.openexchange.folderstorage.FolderExceptionErrorMessage;
import com.openexchange.folderstorage.FolderServiceDecorator;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.FolderStorageDiscoverer;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.SortableId;
import com.openexchange.folderstorage.StorageParameters;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.folderstorage.internal.CalculatePermission;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.server.OXException;
import com.openexchange.threadpool.ThreadPoolCompletionService;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.ThreadPools;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link ListPerformer} - Serves the <code>LIST</code> request.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ListPerformer extends AbstractUserizedFolderPerformer {

    private static final Log LOG = com.openexchange.exception.Log.valueOf(LogFactory.getLog(ListPerformer.class));

    private static final boolean DEBUG = LOG.isDebugEnabled();

    /**
     * Initializes a new {@link ListPerformer} from given session.
     * 
     * @param session The session
     * @param decorator The optional folder service decorator
     */
    public ListPerformer(final ServerSession session, final FolderServiceDecorator decorator) {
        super(session, decorator);
    }

    /**
     * Initializes a new {@link ListPerformer} from given user-context-pair.
     * 
     * @param user The user
     * @param context The context
     * @param decorator The optional folder service decorator
     */
    public ListPerformer(final User user, final Context context, final FolderServiceDecorator decorator) {
        super(user, context, decorator);
    }

    /**
     * Initializes a new {@link ListPerformer}.
     * 
     * @param session The session
     * @param decorator The optional folder service decorator
     * @param folderStorageDiscoverer The folder storage discoverer
     */
    public ListPerformer(final ServerSession session, final FolderServiceDecorator decorator, final FolderStorageDiscoverer folderStorageDiscoverer) {
        super(session, decorator, folderStorageDiscoverer);
    }

    /**
     * Initializes a new {@link ListPerformer}.
     * 
     * @param user The user
     * @param context The context
     * @param decorator The optional folder service decorator
     * @param folderStorageDiscoverer The folder storage discoverer
     */
    public ListPerformer(final User user, final Context context, final FolderServiceDecorator decorator, final FolderStorageDiscoverer folderStorageDiscoverer) {
        super(user, context, decorator, folderStorageDiscoverer);
    }

    /**
     * Performs the <code>LIST</code> request.
     * 
     * @param treeId The tree identifier
     * @param parentId The parent folder identifier
     * @param all <code>true</code> to get all subfolders regardless of their subscription status; otherwise <code>false</code> to only get
     *            subscribed ones
     * @return The user-sensitive subfolders
     * @throws FolderException If a folder error occurs
     */
    public UserizedFolder[] doList(final String treeId, final String parentId, final boolean all) throws FolderException {
        final FolderStorage folderStorage = folderStorageDiscoverer.getFolderStorage(treeId, parentId);
        if (null == folderStorage) {
            throw FolderExceptionErrorMessage.NO_STORAGE_FOR_ID.create(treeId, parentId);
        }
        final long start = DEBUG ? System.currentTimeMillis() : 0L;
        final List<FolderStorage> openedStorages = new ArrayList<FolderStorage>(4);
        if (folderStorage.startTransaction(storageParameters, false)) {
            openedStorages.add(folderStorage);
        }
        try {
            final UserizedFolder[] ret = doList(treeId, parentId, all, openedStorages);
            /*
             * Commit
             */
            for (final FolderStorage fs : openedStorages) {
                fs.commitTransaction(storageParameters);
            }
            if (DEBUG) {
                final long duration = System.currentTimeMillis() - start;
                LOG.debug(new StringBuilder().append("List.doList() took ").append(duration).append("msec for parent folder: ").append(
                    parentId).toString());
            }
            return ret;
        } catch (final FolderException e) {
            for (final FolderStorage fs : openedStorages) {
                fs.rollback(storageParameters);
            }
            throw e;
        } catch (final Exception e) {
            for (final FolderStorage fs : openedStorages) {
                fs.rollback(storageParameters);
            }
            throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Performs the <code>LIST</code> request.
     * 
     * @param treeId The tree identifier
     * @param parentId The parent folder identifier
     * @param all <code>true</code> to get all subfolders regardless of their subscription status; otherwise <code>false</code> to only get
     *            subscribed ones
     * @param startTransaction <code>true</code> to start own transaction; otherwise <code>false</code> if invoked from another action class
     * @return The user-sensitive subfolders
     * @throws FolderException If a folder error occurs
     */
    UserizedFolder[] doList(final String treeId, final String parentId, final boolean all, final java.util.Collection<FolderStorage> openedStorages) throws FolderException {
        final FolderStorage folderStorage = getOpenedStorage(parentId, treeId, storageParameters, openedStorages);
        final UserizedFolder[] ret;
        try {
            final Folder parent = folderStorage.getFolder(treeId, parentId, storageParameters);
            {
                /*
                 * Check folder permission for parent folder
                 */
                final Permission parentPermission;
                if (null == getSession()) {
                    parentPermission = CalculatePermission.calculate(parent, getUser(), getContext(), getAllowedContentTypes());
                } else {
                    parentPermission = CalculatePermission.calculate(parent, getSession(), getAllowedContentTypes());
                }
                if (!parentPermission.isVisible()) {
                    throw FolderExceptionErrorMessage.FOLDER_NOT_VISIBLE.create(
                        getFolderInfo4Error(parent),
                        getUserInfo4Error(),
                        getContextInfo4Error());
                }
            }
            /*
             * Get subfolder identifiers from folder itself
             */
            final String[] subfolderIds = parent.getSubfolderIDs();
            if (null == subfolderIds) {
                /*
                 * Need to get user-visible subfolders from appropriate storage
                 */
                ret = getSubfoldersFromStorages(treeId, parentId, all);
            } else {
                /*
                 * The subfolders can be completely fetched from already opened parent's folder storage
                 */

                /*
                 * Collect by folder storage
                 */
                final Map<FolderStorage, TIntArrayList> map = new HashMap<FolderStorage, TIntArrayList>(4);
                for (int i = 0; i < subfolderIds.length; i++) {
                    final String id = subfolderIds[i];
                    final FolderStorage tmp = folderStorageDiscoverer.getFolderStorage(treeId, id);
                    if (null == tmp) {
                        throw FolderExceptionErrorMessage.NO_STORAGE_FOR_ID.create(treeId, id);
                    }
                    TIntArrayList list = map.get(tmp);
                    if (null == list) {
                        list = new TIntArrayList();
                        map.put(tmp, list);
                    }
                    list.add(i);
                }
                /*
                 * Process by folder storage
                 */
                final UserizedFolder[] subfolders = new UserizedFolder[subfolderIds.length];
                final CompletionService<Object> completionService;
                final StorageParametersProvider paramsProvider;
                if (1 == map.size()) {
                    completionService = new CallerRunsCompletionService<Object>();
                    paramsProvider = new InstanceStorageParametersProvider(storageParameters);
                } else {
                    try {
                        completionService = new ThreadPoolCompletionService<Object>(getInstance().getService(ThreadPoolService.class, true));
                    } catch (final OXException e) {
                        throw new FolderException(e);
                    }
                    paramsProvider = null == session ? new SessionStorageParametersProvider(user, context) : new SessionStorageParametersProvider(session);
                }
                int taskCount = 0;
                for (final Entry<FolderStorage, TIntArrayList> entry : map.entrySet()) {
                    final FolderStorage tmp = entry.getKey();
                    final int[] indexes = entry.getValue().toNativeArray();
                    final Log log = LOG;
                    completionService.submit(new Callable<Object>() {

                        public Object call() throws Exception {
                            final StorageParameters newParameters = paramsProvider.getStorageParameters();
                            final List<FolderStorage> openedStorages = new ArrayList<FolderStorage>(2);
                            if (tmp.startTransaction(newParameters, false)) {
                                openedStorages.add(tmp);
                            }
                            try {
                                /*
                                 * Try to batch-load the folders
                                 */
                                List<Folder> folders;
                                try {
                                    final List<String> ids = new ArrayList<String>(indexes.length);
                                    for (final int index : indexes) {
                                        ids.add(subfolderIds[index]);
                                    }
                                    folders = tmp.getFolders(treeId, ids, newParameters);
                                    final Set<AbstractOXException> warnings = newParameters.getWarnings();
                                    if (!warnings.isEmpty()) {
                                        addWarning(warnings.iterator().next());
                                    }
                                } catch (final FolderException e) {
                                    /*
                                     * Batch-load failed...
                                     */
                                    if (log.isWarnEnabled()) {
                                        log.warn("Batch loading of folder failed. Fall-back to one-by-one loading.", e);
                                    }
                                    folders = null;
                                }
                                if (null == folders) {
                                    /*
                                     * Load them one-by-one
                                     */
                                    NextIndex: for (final int index : indexes) {
                                        final String id = subfolderIds[index];
                                        /*
                                         * Get subfolder from appropriate storage
                                         */
                                        final Folder subfolder;
                                        try {
                                            subfolder = tmp.getFolder(treeId, id, newParameters);
                                        } catch (final FolderException e) {
                                            log.warn(
                                                new StringBuilder(128).append("The folder with ID \"").append(id).append("\" in tree \"").append(treeId).append(
                                                    "\" could not be fetched from storage \"").append(tmp.getClass().getSimpleName()).append("\"").toString(),
                                                e);
                                            addWarning(e);
                                            continue NextIndex;
                                        }
                                        /*
                                         * Check for subscribed status dependent on parameter "all"
                                         */
                                        if (all || (subfolder.isSubscribed() || subfolder.hasSubscribedSubfolders())) {
                                            final Permission userPermission;
                                            if (null == getSession()) {
                                                userPermission =
                                                    CalculatePermission.calculate(subfolder, getUser(), getContext(), getAllowedContentTypes());
                                            } else {
                                                userPermission = CalculatePermission.calculate(subfolder, getSession(), getAllowedContentTypes());
                                            }
                                            if (userPermission.isVisible()) {
                                                subfolders[index] =
                                                    getUserizedFolder(subfolder, userPermission, treeId, all, true, newParameters, openedStorages);
                                            }
                                        }
                                    }
                                } else {
                                    /*
                                     * Convert to userized folders and put into array
                                     */
                                    final int size = folders.size();
                                    int j = 0;
                                    for (final int index : indexes) {
                                        if (j < size) {
                                            final Folder subfolder = folders.get(j++);
                                            /*
                                             * Check for subscribed status dependent on parameter "all"
                                             */
                                            if (all || (subfolder.isSubscribed() || subfolder.hasSubscribedSubfolders())) {
                                                final Permission userPermission;
                                                if (null == getSession()) {
                                                    userPermission =
                                                        CalculatePermission.calculate(subfolder, getUser(), getContext(), getAllowedContentTypes());
                                                } else {
                                                    userPermission = CalculatePermission.calculate(subfolder, getSession(), getAllowedContentTypes());
                                                }
                                                if (userPermission.isVisible()) {
                                                    subfolders[index] =
                                                        getUserizedFolder(subfolder, userPermission, treeId, all, true, newParameters, openedStorages);
                                                }
                                            }
                                        }
                                    }
                                }
                                /*
                                 * Commit
                                 */
                                for (final FolderStorage fs : openedStorages) {
                                    fs.commitTransaction(newParameters);
                                }
                                return null;
                            } catch (final FolderException e) {
                                for (final FolderStorage fs : openedStorages) {
                                    fs.rollback(newParameters);
                                }
                                throw e;
                            } catch (final Exception e) {
                                for (final FolderStorage fs : openedStorages) {
                                    fs.rollback(newParameters);
                                }
                                throw FolderException.newUnexpectedException(e);
                            }
                        }
                    });
                    taskCount++;
                }
                /*
                 * Wait for completion
                 */
                ThreadPools.takeCompletionService(completionService, taskCount, FACTORY);
                ret = trimArray(subfolders);
            }
        } catch (final FolderException e) {
            throw e;
        } catch (final Exception e) {
            throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
        return ret;
    }

    private UserizedFolder[] getSubfoldersFromStorages(final String treeId, final String parentId, final boolean all) throws FolderException {
        /*
         * Determine needed storages for given parent
         */
        final FolderStorage[] neededStorages = folderStorageDiscoverer.getFolderStoragesForParent(treeId, parentId);
        if (null == neededStorages || 0 == neededStorages.length) {
            return new UserizedFolder[0];
        }
        final List<SortableId> allSubfolderIds;
        if (1 == neededStorages.length) {
            final FolderStorage neededStorage = neededStorages[0];
            final boolean started = neededStorage.startTransaction(storageParameters, false);
            try {
                allSubfolderIds = Arrays.asList(neededStorage.getSubfolders(treeId, parentId, storageParameters));
                if (started) {
                    neededStorage.commitTransaction(storageParameters);
                }
            } catch (final FolderException e) {
                if (started) {
                    neededStorage.rollback(storageParameters);
                }
                throw e;
            } catch (final Exception e) {
                if (started) {
                    neededStorage.rollback(storageParameters);
                }
                throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
        } else {
            allSubfolderIds = new ArrayList<SortableId>(neededStorages.length * 8);
            final CompletionService<List<SortableId>> completionService;
            try {
                completionService =
                    new ThreadPoolCompletionService<List<SortableId>>(getInstance().getService(ThreadPoolService.class, true));
            } catch (final OXException e) {
                throw new FolderException(e);
            }
            /*
             * Get all visible subfolders from each storage
             */
            for (final FolderStorage neededStorage : neededStorages) {
                completionService.submit(new Callable<List<SortableId>>() {

                    public List<SortableId> call() throws FolderException {
                        final StorageParameters newParameters = newStorageParameters();
                        final boolean started = neededStorage.startTransaction(newParameters, false);
                        try {
                            final List<SortableId> l = Arrays.asList(neededStorage.getSubfolders(treeId, parentId, newParameters));
                            if (started) {
                                neededStorage.commitTransaction(newParameters);
                            }
                            return l;
                        } catch (final FolderException e) {
                            if (started) {
                                neededStorage.rollback(newParameters);
                            }
                            addWarning(e);
                            return Collections.<SortableId> emptyList();
                        } catch (final Exception e) {
                            if (started) {
                                neededStorage.rollback(newParameters);
                            }
                            final FolderException folderException = FolderException.newUnexpectedException(e);
                            addWarning(folderException);
                            return Collections.<SortableId> emptyList();
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
            /*
             * All failed with a warning?
             */
            if (!results.isEmpty() && (results.size() == getNumOfWarnings())) {
                /*
                 * Throw first warning in set
                 */
                final AbstractOXException e = getWarnings().iterator().next();
                e.setCategory(Category.CODE_ERROR);
                if (!(e instanceof FolderException)) {
                    throw new FolderException(e);
                }
                throw ((FolderException) e);
            }
        }
        /*
         * Sort them
         */
        Collections.sort(allSubfolderIds);
        final int size = allSubfolderIds.size();
        final UserizedFolder[] subfolders = new UserizedFolder[size];
        /*
         * Get corresponding user-sensitive folders
         */

        /*
         * Collect by folder storage
         */
        final Map<FolderStorage, TIntArrayList> map = new HashMap<FolderStorage, TIntArrayList>(4);
        for (int i = 0; i < size; i++) {
            final String id = allSubfolderIds.get(i).getId();
            final FolderStorage tmp = folderStorageDiscoverer.getFolderStorage(treeId, id);
            if (null == tmp) {
                throw FolderExceptionErrorMessage.NO_STORAGE_FOR_ID.create(treeId, id);
            }
            TIntArrayList list = map.get(tmp);
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
            try {
                completionService = new ThreadPoolCompletionService<Object>(getInstance().getService(ThreadPoolService.class, true));
            } catch (final OXException e) {
                throw new FolderException(e);
            }
            paramsProvider = null == session ? new SessionStorageParametersProvider(user, context) : new SessionStorageParametersProvider(session);
        }
        int taskCount = 0;
        for (final Entry<FolderStorage, TIntArrayList> entry : map.entrySet()) {
            final FolderStorage tmp = entry.getKey();
            final int[] indexes = entry.getValue().toNativeArray();
            final Log log = LOG;
            completionService.submit(new Callable<Object>() {

                public Object call() throws Exception {
                    final StorageParameters newParameters = paramsProvider.getStorageParameters();
                    final List<FolderStorage> openedStorages = new ArrayList<FolderStorage>(2);
                    if (tmp.startTransaction(newParameters, false)) {
                        openedStorages.add(tmp);
                    }
                    try {
                        /*
                         * Try to batch-load the folders
                         */
                        List<Folder> folders;
                        try {
                            final List<String> ids = new ArrayList<String>(indexes.length);
                            for (final int index : indexes) {
                                ids.add(allSubfolderIds.get(index).getId());
                            }
                            folders = tmp.getFolders(treeId, ids, newParameters);
                            final Set<AbstractOXException> warnings = newParameters.getWarnings();
                            if (!warnings.isEmpty()) {
                                addWarning(warnings.iterator().next());
                            }
                        } catch (final FolderException e) {
                            /*
                             * Batch-load failed...
                             */
                            if (log.isWarnEnabled()) {
                                log.warn("Batch loading of folder failed. Fall-back to one-by-one loading.", e);
                            }
                            folders = null;
                        }
                        if (null == folders) {
                            NextIndex: for (final int index : indexes) {
                                final String id = allSubfolderIds.get(index).getId();
                                /*
                                 * Get subfolder from appropriate storage
                                 */
                                final Folder subfolder;
                                try {
                                    subfolder = tmp.getFolder(treeId, id, newParameters);
                                } catch (final FolderException e) {
                                    log.warn(
                                        new StringBuilder(128).append("The folder with ID \"").append(id).append("\" in tree \"").append(treeId).append(
                                            "\" could not be fetched from storage \"").append(tmp.getClass().getSimpleName()).append("\"").toString(),
                                        e);
                                    addWarning(e);
                                    continue NextIndex;
                                }
                                /*
                                 * Check for subscribed status dependent on parameter "all"
                                 */
                                if (all || (subfolder.isSubscribed() || subfolder.hasSubscribedSubfolders())) {
                                    final Permission userPermission;
                                    if (null == getSession()) {
                                        userPermission =
                                            CalculatePermission.calculate(subfolder, getUser(), getContext(), getAllowedContentTypes());
                                    } else {
                                        userPermission = CalculatePermission.calculate(subfolder, getSession(), getAllowedContentTypes());
                                    }
                                    if (userPermission.isVisible()) {
                                        subfolders[index] =
                                            getUserizedFolder(subfolder, userPermission, treeId, all, true, newParameters, openedStorages);
                                    }
                                }
                            }
                        } else {
                            /*
                             * Convert to userized folders and put into array
                             */
                            final int size = folders.size();
                            int j = 0;
                            for (final int index : indexes) {
                                if (j < size) {
                                    final Folder subfolder = folders.get(j++);
                                    if (null != subfolder) {
                                        /*
                                         * Check for subscribed status dependent on parameter "all"
                                         */
                                        if (all || (subfolder.isSubscribed() || subfolder.hasSubscribedSubfolders())) {
                                            final Permission userPermission;
                                            if (null == getSession()) {
                                                userPermission =
                                                    CalculatePermission.calculate(subfolder, getUser(), getContext(), getAllowedContentTypes());
                                            } else {
                                                userPermission =
                                                    CalculatePermission.calculate(subfolder, getSession(), getAllowedContentTypes());
                                            }
                                            if (userPermission.isVisible()) {
                                                subfolders[index] =
                                                    getUserizedFolder(
                                                        subfolder,
                                                        userPermission,
                                                        treeId,
                                                        all,
                                                        true,
                                                        newParameters,
                                                        openedStorages);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        /*
                         * Commit
                         */
                        for (final FolderStorage fs : openedStorages) {
                            fs.commitTransaction(newParameters);
                        }
                        return null;
                    } catch (final FolderException e) {
                        for (final FolderStorage fs : openedStorages) {
                            fs.rollback(newParameters);
                        }
                        throw e;
                    } catch (final Exception e) {
                        for (final FolderStorage fs : openedStorages) {
                            fs.rollback(newParameters);
                        }
                        throw FolderException.newUnexpectedException(e);
                    }
                    
                }
            });
            taskCount++;
        }
        /*
         * Wait for completion
         */
        ThreadPools.takeCompletionService(completionService, taskCount, FACTORY);
        return trimArray(subfolders);
    }

    private static final ThreadPools.ExpectedExceptionFactory<FolderException> FACTORY =
        new ThreadPools.ExpectedExceptionFactory<FolderException>() {

            public Class<FolderException> getType() {
                return FolderException.class;
            }

            public FolderException newUnexpectedError(final Throwable t) {
                return FolderException.newUnexpectedException(t);
            }
        };

    /**
     * Creates a newly allocated array containing all elements of specified array in the same order except <code>null</code> values.
     * 
     * @param userizedFolders The array to trim
     * @return A newly allocated copy-array with <code>null</code> elements removed
     */
    private static UserizedFolder[] trimArray(final UserizedFolder[] userizedFolders) {
        if (null == userizedFolders) {
            return new UserizedFolder[0];
        }
        final List<UserizedFolder> l = new ArrayList<UserizedFolder>(userizedFolders.length);
        for (final UserizedFolder uf : userizedFolders) {
            if (null != uf) {
                l.add(uf);
            }
        }
        return l.toArray(new UserizedFolder[l.size()]);
    }

}
