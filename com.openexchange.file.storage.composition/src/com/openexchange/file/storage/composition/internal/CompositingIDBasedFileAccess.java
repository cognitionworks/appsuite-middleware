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

package com.openexchange.file.storage.composition.internal;

import static com.openexchange.file.storage.composition.internal.IDManglingFileCustomizer.fixIDs;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.File.Field;
import com.openexchange.file.storage.FileStorageAccount;
import com.openexchange.file.storage.FileStorageAccountAccess;
import com.openexchange.file.storage.FileStorageAccountManager;
import com.openexchange.file.storage.FileStorageFileAccess;
import com.openexchange.file.storage.FileStorageFileAccess.IDTuple;
import com.openexchange.file.storage.FileStorageFileAccess.SortDirection;
import com.openexchange.file.storage.FileStorageService;
import com.openexchange.file.storage.composition.IDBasedFileAccess;
import com.openexchange.groupware.results.AbstractTimedResult;
import com.openexchange.groupware.results.Delta;
import com.openexchange.groupware.results.TimedResult;
import com.openexchange.session.Session;
import com.openexchange.tools.iterator.MergingSearchIterator;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIteratorAdapter;
import com.openexchange.tx.AbstractService;
import com.openexchange.tx.TransactionException;

/**
 * {@link CompositingIDBasedFileAccess}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public abstract class CompositingIDBasedFileAccess extends AbstractService<Transaction> implements IDBasedFileAccess {

    protected Session session;
    
    private final ThreadLocal<Map<String, FileStorageAccountAccess>> connectedAccounts = new ThreadLocal<Map<String, FileStorageAccountAccess>>();
    private final ThreadLocal<List<FileStorageAccountAccess>> accessesToClose = new ThreadLocal<List<FileStorageAccountAccess>>();
    
    public CompositingIDBasedFileAccess(final Session session) {
        super();
        this.session = session;
        connectedAccounts.set(new HashMap<String, FileStorageAccountAccess>());
        accessesToClose.set(new LinkedList<FileStorageAccountAccess>());
    }

    public boolean exists(final String id, final int version) throws OXException {
        final FileID fileID = new FileID(id);
        return getFileAccess(fileID.getService(), fileID.getAccountId()).exists(fileID.getFolderId(), fileID.getFileId(), version);
    }

    public Delta<File> getDelta(final String folderId, final long updateSince, final List<Field> columns, final boolean ignoreDeleted) throws OXException {
        final FolderID folderID = new FolderID(folderId);
        final Delta<File> delta = getFileAccess(folderID.getService(), folderID.getAccountId()).getDelta(
            folderID.getFolderId(),
            updateSince,
            addIDColumns(columns),
            ignoreDeleted);
        return fixIDs(delta, folderID.getService(), folderID.getAccountId());
    }

    public Delta<File> getDelta(final String folderId, final long updateSince, final List<Field> columns, final Field sort, final SortDirection order, final boolean ignoreDeleted) throws OXException {
        final FolderID folderID = new FolderID(folderId);
        final Delta<File> delta = getFileAccess(folderID.getService(), folderID.getAccountId()).getDelta(
            folderID.getFolderId(),
            updateSince,
            addIDColumns(columns),
            sort,
            order,
            ignoreDeleted);
        return fixIDs(delta, folderID.getService(), folderID.getAccountId());
    }

    public InputStream getDocument(final String id, final int version) throws OXException {
        final FileID fileID = new FileID(id);

        return getFileAccess(fileID.getService(), fileID.getAccountId()).getDocument(fileID.getFolderId(), fileID.getFileId(), version);
    }

    public TimedResult<File> getDocuments(final String folderId) throws OXException {
        final FolderID folderID = new FolderID(folderId);
        final TimedResult<File> result = getFileAccess(folderID.getService(), folderID.getAccountId()).getDocuments(folderID.getFolderId());
        return fixIDs(result, folderID.getService(), folderID.getAccountId());
    }

    public TimedResult<File> getDocuments(final String folderId, final List<Field> columns) throws OXException {
        final FolderID folderID = new FolderID(folderId);
        return getFileAccess(folderID.getService(), folderID.getAccountId()).getDocuments(folderID.getFolderId(), addIDColumns(columns));
    }

    public TimedResult<File> getDocuments(final String folderId, final List<Field> columns, final Field sort, final SortDirection order) throws OXException {
        final FolderID folderID = new FolderID(folderId);
        final TimedResult<File> result = getFileAccess(folderID.getService(), folderID.getAccountId()).getDocuments(
            folderID.getFolderId(),
            addIDColumns(columns),
            sort,
            order);
        return fixIDs(result, folderID.getService(), folderID.getAccountId());
    }

    public TimedResult<File> getDocuments(final List<String> ids, final List<Field> columns) throws OXException {
        final List<File> files = new ArrayList<File>(100);
        for (final String id : ids) {
            if(exists(id, FileStorageFileAccess.CURRENT_VERSION)) {
                final File fileMetadata = getFileMetadata(id, FileStorageFileAccess.CURRENT_VERSION);
                files.add(fileMetadata);
            }
        }

        return new AbstractTimedResult<File>(new SearchIteratorAdapter<File>(files.iterator())) {

            @Override
            protected long extractTimestamp(final File object) {
                return object.getSequenceNumber();
            }

        };
    }

    public File getFileMetadata(final String id, final int version) throws OXException {
        final FileID fileID = new FileID(id);
        final File fileMetadata = getFileAccess(fileID.getService(), fileID.getAccountId()).getFileMetadata(
            fileID.getFolderId(),
            fileID.getFileId(),
            version);
        return fixIDs(fileMetadata, fileID.getService(), fileID.getAccountId());
    }

    public TimedResult<File> getVersions(final String id) throws OXException {
        final FileID fileID = new FileID(id);
        final TimedResult<File> result = getFileAccess(fileID.getService(), fileID.getAccountId()).getVersions(
            fileID.getFolderId(),
            fileID.getFileId());
        return fixIDs(result, fileID.getService(), fileID.getAccountId());
    }

    public TimedResult<File> getVersions(final String id, final List<Field> columns) throws OXException {
        final FileID fileID = new FileID(id);
        final TimedResult<File> result = getFileAccess(fileID.getService(), fileID.getAccountId()).getVersions(
            fileID.getFolderId(),
            fileID.getFileId(),
            addIDColumns(columns));
        return fixIDs(result, fileID.getService(), fileID.getAccountId());
    }

    public TimedResult<File> getVersions(final String id, final List<Field> columns, final Field sort, final SortDirection order) throws OXException {
        final FileID fileID = new FileID(id);
        final TimedResult<File> result = getFileAccess(fileID.getService(), fileID.getAccountId()).getVersions(
            fileID.getFolderId(),
            fileID.getFileId(),
            addIDColumns(columns),
            sort,
            order);
        return fixIDs(result, fileID.getService(), fileID.getAccountId());
    }

    public void lock(final String id, final long diff) throws OXException {
        final FileID fileID = new FileID(id);
        getFileAccess(fileID.getService(), fileID.getAccountId()).lock(fileID.getFolderId(), fileID.getFileId(), diff);
    }

    public void removeDocument(final String folderId, final long sequenceNumber) throws OXException {
        final FolderID id = new FolderID(folderId);

        getFileAccess(id.getService(), id.getAccountId()).removeDocument(id.getFolderId(), sequenceNumber);
    }

    public List<String> removeDocument(final List<String> ids, final long sequenceNumber) throws OXException {
        final Map<FileStorageFileAccess, List<IDTuple>> deleteOperations = new HashMap<FileStorageFileAccess, List<IDTuple>>();
        for (final String id : ids) {
            final FileID fileID = new FileID(id);
            final FileStorageFileAccess fileAccess = getFileAccess(fileID.getService(), fileID.getAccountId());

            List<IDTuple> deletes = deleteOperations.get(fileAccess);
            if (deletes == null) {
                deletes = new ArrayList<IDTuple>();
                deleteOperations.put(fileAccess, deletes);
            }

            deletes.add(new FileStorageFileAccess.IDTuple(fileID.getFolderId(), fileID.getFileId()));
        }

        final List<String> notDeleted = new ArrayList<String>(ids.size());

        for (final Map.Entry<FileStorageFileAccess, List<IDTuple>> deleteOp : deleteOperations.entrySet()) {
            final FileStorageFileAccess access = deleteOp.getKey();
            final List<IDTuple> conflicted = access.removeDocument(deleteOp.getValue(), sequenceNumber);
            for (final IDTuple tuple : conflicted) {
                final FileStorageAccountAccess accountAccess = access.getAccountAccess();
                notDeleted.add(new FileID(
                    accountAccess.getService().getId(),
                    accountAccess.getAccountId(),
                    tuple.getFolder(),
                    tuple.getId()).toUniqueID());
            }
        }
        return notDeleted;
    }

    public int[] removeVersion(final String id, final int[] versions) throws OXException {
        final FileID fileID = new FileID(id);

        return getFileAccess(fileID.getService(), fileID.getAccountId()).removeVersion(fileID.getFolderId(), fileID.getFileId(), versions);
    }

    private static interface FileAccessDelegation {

        public void call(FileStorageFileAccess access) throws OXException;
    }

    protected void save(final File document, final InputStream data, final long sequenceNumber, final List<Field> modifiedColumns, final FileAccessDelegation delegation) throws OXException {
        final String id = document.getId();
        FolderID folderID = null;
        if(document.getFolderId() != null) {
            folderID = new FolderID(document.getFolderId());
        } 
        if (id != FileStorageFileAccess.NEW) {
            final FileID fileID = new FileID(id);
            if(folderID == null) {
                folderID = new FolderID(fileID.getService(), fileID.getAccountId(), fileID.getFolderId());
            }
            if(!(fileID.getService().equals(folderID.getService())) || !(fileID.getAccountId().equals(folderID.getAccountId()))) {
                move(document, data, sequenceNumber, modifiedColumns);
                return;
            }
            
            document.setId(fileID.getFileId());
            if(folderID == null) {
                folderID = new FolderID(fileID.getService(), fileID.getAccountId(), fileID.getFolderId());
            }
        }
        document.setFolderId(folderID.getFolderId());
        delegation.call(getFileAccess(folderID.getService(), folderID.getAccountId()));
        document.setId(new FileID(folderID.getService(), folderID.getAccountId(), document.getFolderId(), document.getId()).toUniqueID());
        document.setFolderId(new FolderID(folderID.getService(), folderID.getAccountId(), document.getFolderId()).toUniqueID());

    }

    protected void move(final File document, InputStream data, final long sequenceNumber, final List<Field> modifiedColumns) throws OXException {
        final FileID id = new FileID(document.getId()); // signifies the source
        final FolderID folderId = new FolderID(document.getFolderId()); // signifies the destination

        final boolean partialUpdate = modifiedColumns != null && !modifiedColumns.isEmpty();
        final boolean hasUpload = data != null;

        final FileStorageFileAccess destAccess = getFileAccess(folderId.getService(), folderId.getAccountId());
        final FileStorageFileAccess sourceAccess = getFileAccess(id.getService(), id.getAccountId());

        document.setId(FileStorageFileAccess.NEW);
        document.setFolderId(folderId.getFolderId());

        if (!hasUpload) {
            data = sourceAccess.getDocument(id.getFolderId(), id.getFileId(), FileStorageFileAccess.CURRENT_VERSION);
        }

        if (partialUpdate) {
            final File original = sourceAccess.getFileMetadata(id.getFolderId(), id.getFileId(), FileStorageFileAccess.CURRENT_VERSION);
            final Set<Field> fieldsToSkip = new HashSet<Field>(modifiedColumns);
            fieldsToSkip.add(Field.FOLDER_ID);
            fieldsToSkip.add(Field.ID);
            fieldsToSkip.add(Field.LAST_MODIFIED);
            fieldsToSkip.add(Field.CREATED);

            final Set<Field> toCopy = EnumSet.complementOf(EnumSet.copyOf(fieldsToSkip));

            document.copyFrom(original, toCopy.toArray(new File.Field[toCopy.size()]));

        }

        destAccess.saveDocument(document, data, FileStorageFileAccess.UNDEFINED_SEQUENCE_NUMBER);

        document.setId(new FileID(folderId.getService(), folderId.getAccountId(), document.getFolderId(), document.getId()).toUniqueID());
        document.setFolderId(new FolderID(folderId.getService(), folderId.getAccountId(), document.getFolderId()).toUniqueID());

        sourceAccess.removeDocument(Arrays.asList(new FileStorageFileAccess.IDTuple(id.getFolderId(), id.getFileId())), sequenceNumber);
    }

    public void saveDocument(final File document, final InputStream data, final long sequenceNumber) throws OXException {
        save(document, data, sequenceNumber, null, new FileAccessDelegation() {

            public void call(final FileStorageFileAccess access) throws OXException {
                access.saveDocument(document, data, sequenceNumber);
            }

        });
    }

    public void saveDocument(final File document, final InputStream data, final long sequenceNumber, final List<Field> modifiedColumns) throws OXException {
        save(document, data, sequenceNumber, modifiedColumns, new FileAccessDelegation() {

            public void call(final FileStorageFileAccess access) throws OXException {
                access.saveDocument(document, data, sequenceNumber, modifiedColumns);
            }

        });
    }

    public void saveFileMetadata(final File document, final long sequenceNumber) throws OXException {
        save(document, null, sequenceNumber, null, new FileAccessDelegation() {

            public void call(final FileStorageFileAccess access) throws OXException {
                access.saveFileMetadata(document, sequenceNumber);
            }

        });
    }

    public void saveFileMetadata(final File document, final long sequenceNumber, final List<Field> modifiedColumns) throws OXException {
        save(document, null, sequenceNumber, modifiedColumns, new FileAccessDelegation() {

            public void call(final FileStorageFileAccess access) throws OXException {
                access.saveFileMetadata(document, sequenceNumber, modifiedColumns);
            }

        });
    }
    
    public String copy(final String sourceId, final String destFolderId, final File update, InputStream newData, final List<File.Field> fields) throws OXException {
        final FileID source = new FileID(sourceId);
        FolderID dest = null;
        
        File fileMetadata = null;
        if(destFolderId != null) {
            dest = new FolderID(destFolderId);
        } else {
            fileMetadata = getFileMetadata(sourceId, FileStorageFileAccess.CURRENT_VERSION);
            dest = new FolderID(fileMetadata.getFolderId());
        }
        
        if(source.getService().equals(dest.getService()) && source.getAccountId().equals(dest.getAccountId())) {
            final FileStorageFileAccess fileAccess = getFileAccess(source.getService(), source.getAccountId());
            final IDTuple destAddress = fileAccess.copy(new IDTuple(source.getFolderId(), source.getFileId()), dest.getFolderId(), update, newData, fields);
            return new FileID(source.getService(), source.getAccountId(), destAddress.getFolder(), destAddress.getId()).toUniqueID();
        }
        
        if(fileMetadata == null) {
            fileMetadata = getFileMetadata(sourceId, FileStorageFileAccess.CURRENT_VERSION);
        }
        
        if(update != null) {
            fileMetadata.copyFrom(update, fields.toArray(new File.Field[fields.size()]));
        }
        
        if(newData == null) {
            newData = getDocument(sourceId, FileStorageFileAccess.CURRENT_VERSION);
        }
        
        fileMetadata.setId(FileStorageFileAccess.NEW);
        fileMetadata.setFolderId(destFolderId);
        
        if(newData == null) {
            saveFileMetadata(fileMetadata, FileStorageFileAccess.UNDEFINED_SEQUENCE_NUMBER);
        } else {
            saveDocument(fileMetadata, newData, FileStorageFileAccess.UNDEFINED_SEQUENCE_NUMBER);
        }
        
        return fileMetadata.getId();
    }

    public SearchIterator<File> search(final String query, List<Field> cols, final String folderId, final Field sort, final SortDirection order, final int start, final int end) throws OXException {
        cols = addIDColumns(cols);
        if (folderId == FileStorageFileAccess.ALL_FOLDERS) {
            final List<FileStorageFileAccess> all = getAllFileStorageAccesses();
            final List<SearchIterator<File>> results = new ArrayList<SearchIterator<File>>(all.size());
            for (final FileStorageFileAccess files : all) {
                final SearchIterator<File> result = files.search(query, cols, folderId, sort, order, start, end);
                if(result != null) {
                    final FileStorageAccountAccess accountAccess = files.getAccountAccess();
                    results.add(fixIDs(result, accountAccess.getService().getId(), accountAccess.getAccountId()));
                }
            }
            return new MergingSearchIterator<File>(order.comparatorBy(sort), results);
        }
        final FolderID id = new FolderID(folderId);
        return getFileAccess(id.getService(), id.getAccountId()).search(query, cols, id.getFolderId(), sort, order, start, end);
    }

    public void touch(final String id) throws OXException {
        final FileID fileID = new FileID(id);
        getFileAccess(fileID.getService(), fileID.getAccountId()).touch(fileID.getFolderId(), fileID.getFileId());
    }

    public void unlock(final String id) throws OXException {
        final FileID fileID = new FileID(id);
        getFileAccess(fileID.getService(), fileID.getAccountId()).unlock(fileID.getFolderId(), fileID.getFileId());
    }

    protected List<File.Field> addIDColumns(List<File.Field> columns) {
        final boolean hasID = columns.contains(File.Field.ID);
        final boolean hasFolder = columns.contains(File.Field.FOLDER_ID);
        final boolean hasLastModified = columns.contains(File.Field.LAST_MODIFIED);

        if (hasID && hasFolder && hasLastModified) {
            return columns;
        }

        columns = new ArrayList<File.Field>(columns);

        if (!hasID) {
            columns.add(File.Field.ID);
        }

        if (!hasFolder) {
            columns.add(File.Field.FOLDER_ID);
        }

        if (!hasLastModified) {
            columns.add(File.Field.LAST_MODIFIED);
        }

        return columns;

    }

    protected FileStorageFileAccess getFileAccess(final String serviceId, final String accountId) throws OXException {
        final FileStorageAccountAccess cached = connectedAccounts.get().get(serviceId+"/"+accountId);
        if(cached != null) {
            return cached.getFileAccess();
        }
        final FileStorageService fileStorage = getFileStorageService(serviceId);

        final FileStorageAccountAccess accountAccess = fileStorage.getAccountAccess(accountId, session);
        connect( accountAccess );
        return accountAccess.getFileAccess();
    }

    private void connect(final FileStorageAccountAccess accountAccess) throws OXException {
        final String id = accountAccess.getService().getId()+"/"+accountAccess.getAccountId();
        
        
        if(!connectedAccounts.get().containsKey(id)) {
            connectedAccounts.get().put(id, accountAccess);
            accountAccess.connect();
            accessesToClose.get().add(accountAccess);
        }
    }

    protected List<FileStorageFileAccess> getAllFileStorageAccesses() throws OXException {
        final List<FileStorageFileAccess> retval = new ArrayList<FileStorageFileAccess>();
        final List<FileStorageService> allFileStorageServices = getAllFileStorageServices();
        for (final FileStorageService fileStorageService : allFileStorageServices) {
            final FileStorageAccountManager accountManager = fileStorageService.getAccountManager();
            final List<FileStorageAccount> accounts = accountManager.getAccounts(session);
            for (final FileStorageAccount fileStorageAccount : accounts) {
                final FileStorageAccountAccess accountAccess = fileStorageService.getAccountAccess(fileStorageAccount.getId(), session);
                connect( accountAccess );
                retval.add(accountAccess.getFileAccess());
            }
        }
        return retval;
    }

    protected abstract FileStorageService getFileStorageService(String serviceId) throws OXException;

    protected abstract List<FileStorageService> getAllFileStorageServices() throws OXException;

    // Transaction Handling
    /*
     * (non-Javadoc)
     * @see com.openexchange.tx.AbstractService#commit(java.lang.Object)
     */
    @Override
    protected void commit(final Transaction transaction) throws TransactionException {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.tx.AbstractService#createTransaction()
     */
    @Override
    protected Transaction createTransaction() throws TransactionException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.tx.AbstractService#rollback(java.lang.Object)
     */
    @Override
    protected void rollback(final Transaction transaction) throws TransactionException {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.tx.TransactionAware#setCommitsTransaction(boolean)
     */
    public void setCommitsTransaction(final boolean commits) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.tx.TransactionAware#setRequestTransactional(boolean)
     */
    public void setRequestTransactional(final boolean transactional) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.tx.TransactionAware#setTransactional(boolean)
     */
    public void setTransactional(final boolean transactional) {
        // TODO Auto-generated method stub

    }
    
    @Override
    public void startTransaction() throws TransactionException {
        super.startTransaction();
        connectedAccounts.get().clear();
        accessesToClose.get().clear();
    }
    
    @Override
    public void finish() throws TransactionException {
        connectedAccounts.get().clear();
        for(final FileStorageAccountAccess acc : accessesToClose.get()) {
            acc.close();
        }
        accessesToClose.get().clear();
        super.finish();
    }
}
