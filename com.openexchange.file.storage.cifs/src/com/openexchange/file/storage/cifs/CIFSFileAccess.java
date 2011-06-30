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

package com.openexchange.file.storage.cifs;

import static com.openexchange.file.storage.cifs.Utils.checkFolderId;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import org.apache.commons.httpclient.URI;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.File.Field;
import com.openexchange.file.storage.FileDelta;
import com.openexchange.file.storage.FileStorageAccount;
import com.openexchange.file.storage.FileStorageAccountAccess;
import com.openexchange.file.storage.FileStorageException;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.file.storage.FileStorageFileAccess;
import com.openexchange.file.storage.FileTimedResult;
import com.openexchange.groupware.results.Delta;
import com.openexchange.groupware.results.TimedResult;
import com.openexchange.session.Session;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIteratorAdapter;
import com.openexchange.tx.TransactionException;

/**
 * {@link CIFSFileAccess}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CIFSFileAccess extends AbstractCIFSAccess implements FileStorageFileAccess {

    private final FileStorageAccountAccess accountAccess;
   
    /**
     * Initializes a new {@link CIFSFileAccess}.
     */
    public CIFSFileAccess(final String rootUrl, final NtlmPasswordAuthentication auth, final FileStorageAccount account, final Session session, final FileStorageAccountAccess accountAccess) {
        super(rootUrl, auth, account, session);
        this.accountAccess = accountAccess;
    }

    public void startTransaction() throws TransactionException {
        // TODO Auto-generated method stub

    }

    public void commit() throws TransactionException {
        // TODO Auto-generated method stub

    }

    public void rollback() throws TransactionException {
        // TODO Auto-generated method stub

    }

    public void finish() throws TransactionException {
        // TODO Auto-generated method stub

    }

    public void setTransactional(final boolean transactional) {
        // TODO Auto-generated method stub

    }

    public void setRequestTransactional(final boolean transactional) {
        // TODO Auto-generated method stub

    }

    public void setCommitsTransaction(final boolean commits) {
        // TODO Auto-generated method stub

    }

    public boolean exists(final String folderId, final String id, final int version) throws FileStorageException {
        try {
            /*
             * Check
             */
            final String fid = checkFolderId(folderId, rootUrl);
            final URI uri = new URI(fid + id, true);
            final SmbFile smbFile = new SmbFile(uri.toString(), auth);
            if (!smbFile.exists()) {
                return false;
            }
            if (!smbFile.isFile()) {
                /*
                 * Not a directory
                 */
                throw CIFSExceptionCodes.NOT_A_FILE.create(folderId);
            }
            return true;
        } catch (final FileStorageException e) {
            throw e;
        } catch (final SmbException e) {
            throw CIFSExceptionCodes.SMB_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    public File getFileMetadata(final String folderId, final String id, final int version) throws FileStorageException {
        if (version != CURRENT_VERSION) {
            throw CIFSExceptionCodes.VERSIONING_NOT_SUPPORTED.create();
        }
        try {
            /*
             * Check
             */
            final String fid = checkFolderId(folderId, rootUrl);
            final URI uri = new URI(fid + id, true);
            final SmbFile smbFile = new SmbFile(uri.toString(), auth);
            if (!smbFile.exists()) {
                throw CIFSExceptionCodes.NOT_FOUND.create(uri.toString());
            }
            if (!smbFile.isFile()) {
                /*
                 * Not a directory
                 */
                throw CIFSExceptionCodes.NOT_A_FILE.create(uri.toString());
            }
            /*
             * Start conversion
             */
            return new CIFSFile(fid, id, session.getUserId()).parseSmbFile(smbFile);
        } catch (final FileStorageException e) {
            throw e;
        } catch (final SmbException e) {
            throw CIFSExceptionCodes.SMB_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    public void saveFileMetadata(final File file, final long sequenceNumber) throws FileStorageException {
        createSmbFile(file, null);
    }

    public void saveFileMetadata(final File file, final long sequenceNumber, final List<Field> modifiedFields) throws FileStorageException {
        createSmbFile(file, modifiedFields);
    }

    private SmbFile createSmbFile(final File file, final List<Field> modifiedFields) throws FileStorageException {
        try {
            final Set<Field> set =
                null == modifiedFields || modifiedFields.isEmpty() ? EnumSet.allOf(Field.class) : EnumSet.copyOf(modifiedFields);
            /*
             * Check
             */
            final String folderId = checkFolderId(file.getFolderId(), rootUrl);
            final String id;
            {
                final String fid = file.getId();
                if (null == fid) {
                    final String name = file.getFileName();
                    if (null == name) {
                        throw CIFSExceptionCodes.MISSING_FILE_NAME.create();
                    }
                    id = name;
                    file.setId(id);
                } else {
                    id = fid;
                }
            }
            final URI uri = new URI(folderId + id, true);
            /*
             * Convert file to SMB representation
             */
            final SmbFile smbFile = new SmbFile(uri.toString(), auth);
            /*
             * Create if non-existent
             */
            if (!smbFile.exists()) {
                smbFile.createNewFile();
            }
            final long now = System.currentTimeMillis();
            if (set.contains(Field.CREATED)) {
                smbFile.setCreateTime(now);
            }
            if (set.contains(Field.LAST_MODIFIED)) {
                smbFile.setLastModified(now);
            }
            smbFile.setReadWrite();
            return smbFile;
        } catch (final FileStorageException e) {
            throw e;
        } catch (final SmbException e) {
            throw CIFSExceptionCodes.SMB_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    public IDTuple copy(final IDTuple source, final String destFolder, final File update, final InputStream newFil, final List<Field> modifiedFields) throws FileStorageException {
        try {
            final String fid = checkFolderId(source.getFolder(), rootUrl);
            final URI uri = new URI(fid + source.getId(), true);
            /*
             * Check validity
             */
            final SmbFile copyMe = new SmbFile(uri.toString(), auth);
            if (!copyMe.exists()) {
                throw CIFSExceptionCodes.NOT_FOUND.create(uri.toString());
            }
            if (!copyMe.isFile()) {
                throw CIFSExceptionCodes.NOT_A_FILE.create(uri.toString());
            }
            final SmbFile dest = new SmbFile(checkFolderId(destFolder, rootUrl) + source.getId(), auth);
            /*
             * Perform COPY
             */
            copyMe.copyTo(dest);
            /*
             * Save
             */
            saveDocument0(update, newFil, modifiedFields);
            /*
             * Return
             */
            return new IDTuple(destFolder, source.getId());
        } catch (final FileStorageException e) {
            throw e;
        } catch (final SmbException e) {
            throw CIFSExceptionCodes.SMB_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    public InputStream getDocument(final String folderId, final String id, final int version) throws FileStorageException {
        try {
            final String fid = checkFolderId(folderId, rootUrl);
            final URI uri = new URI(fid + id, true);
            final SmbFile smbFile = new SmbFile(uri.toString(), auth);
            if (!smbFile.exists()) {
                throw CIFSExceptionCodes.NOT_FOUND.create(uri.toString());
            }
            if (!smbFile.isFile()) {
                /*
                 * Not a directory
                 */
                throw CIFSExceptionCodes.NOT_A_FILE.create(uri.toString());
            }
            /*
             * Get SMB file's input stream
             */
            return new SmbFileInputStream(smbFile);
        } catch (final FileStorageException e) {
            throw e;
        } catch (final SmbException e) {
            throw CIFSExceptionCodes.SMB_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    public void saveDocument(final File file, final InputStream data, final long sequenceNumber) throws FileStorageException {
        saveDocument0(file, data, null);
    }

    public void saveDocument(final File file, final InputStream data, final long sequenceNumber, final List<Field> modifiedFields) throws FileStorageException {
        saveDocument0(file, data, modifiedFields);
    }

    private void saveDocument0(final File file, final InputStream data, final List<Field> modifiedFields) throws FileStorageException {
        try {
            /*
             * Save metadata
             */
            final SmbFile newSmbFile = createSmbFile(file, modifiedFields);
            /*
             * Upload data
             */
            final SmbFileOutputStream outputStream = new SmbFileOutputStream(newSmbFile, false);
            try {
                final byte[] buf = new byte[8192];
                int read;
                while ((read = data.read(buf)) != -1) {
                    outputStream.write(buf, 0, read);
                }
                outputStream.flush();
            } finally {
                outputStream.close();
            }
        } catch (final FileStorageException e) {
            throw e;
        } catch (final SmbException e) {
            throw CIFSExceptionCodes.SMB_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            try {
                data.close();
            } catch (final IOException e) {
                org.apache.commons.logging.LogFactory.getLog(CIFSFileAccess.class).error(e.getMessage(), e);
            }
        }
    }

    public void removeDocument(final String folderId, final long sequenceNumber) throws FileStorageException {
        try {
            /*
             * Get & check folder
             */
            final String fid = checkFolderId(folderId, rootUrl);
            final SmbFile smbFolder = new SmbFile(fid, auth);
            if (!smbFolder.exists()) {
                throw CIFSExceptionCodes.NOT_FOUND.create(folderId);
            }
            if (!smbFolder.isDirectory()) {
                throw CIFSExceptionCodes.NOT_A_FOLDER.create(folderId);
            }
            /*
             * List its sub-resources
             */
            final SmbFile[] subFiles = smbFolder.listFiles();
            for (final SmbFile subFile : subFiles) {
                if (subFile.isFile()) {
                    subFile.delete();
                }
            }
        } catch (final FileStorageException e) {
            throw e;
        } catch (final SmbException e) {
            throw CIFSExceptionCodes.SMB_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    public List<IDTuple> removeDocument(final List<IDTuple> ids, final long sequenceNumber) throws FileStorageException {
        try {
            final List<IDTuple> ret = new ArrayList<FileStorageFileAccess.IDTuple>();
            for (final IDTuple id : ids) {
                final String fid = checkFolderId(id.getFolder(), rootUrl);
                final URI uri = new URI(fid + id.getId(), true);
                /*
                 * Check validity
                 */
                final SmbFile smbFile = new SmbFile(uri.toString(), auth);
                if (smbFile.exists()) {
                    /*
                     * Check for file
                     */
                    if (!smbFile.isFile()) {
                        throw CIFSExceptionCodes.NOT_A_FILE.create(uri.toString());
                    }
                    /*
                     * Delete
                     */
                    try {
                        smbFile.delete();
                    } catch (final SmbException e) {
                        /*
                         * Delete failed
                         */
                        ret.add(id);
                    }
                }
            }
            /*
             * Return
             */
            return ret;
        } catch (final FileStorageException e) {
            throw e;
        } catch (final SmbException e) {
            throw CIFSExceptionCodes.SMB_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    public int[] removeVersion(final String folderId, final String id, final int[] versions) throws FileStorageException {
        for (final int version : versions) {
            if (version != CURRENT_VERSION) {
                throw CIFSExceptionCodes.VERSIONING_NOT_SUPPORTED.create();
            }
        }
        try {
            final String fid = checkFolderId(folderId, rootUrl);
            final URI uri = new URI(fid + id, true);
            /*
             * Check validity
             */
            final SmbFile smbFile = new SmbFile(uri.toString(), auth);
            if (!smbFile.exists()) {
                /*
                 * NO-OP for us
                 */
                return new int[0];
            }
            if (!smbFile.isFile()) {
                throw CIFSExceptionCodes.NOT_A_FILE.create(uri.toString());
            }
            /*
             * Delete
             */
            smbFile.delete();
            /*
             * Return empty array
             */
            return new int[0];
        } catch (final FileStorageException e) {
            throw e;
        } catch (final SmbException e) {
            throw CIFSExceptionCodes.SMB_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    public void unlock(final String folderId, final String id) throws FileStorageException {
        // TODO Auto-generated method stub

    }

    public void lock(final String folderId, final String id, final long diff) throws FileStorageException {
        // TODO Auto-generated method stub

    }

    public void touch(final String folderId, final String id) throws FileStorageException {
        try {
            /*
             * Check
             */
            final String fid = checkFolderId(folderId, rootUrl);
            final URI uri = new URI(fid + id, true);
            /*
             * Check validity
             */
            final SmbFile smbFile = new SmbFile(uri.toString(), auth);
            if (!smbFile.exists()) {
                throw CIFSExceptionCodes.NOT_FOUND.create(uri.toString());
            }
            if (!smbFile.isFile()) {
                throw CIFSExceptionCodes.NOT_A_FILE.create(uri.toString());
            }
            /*
             * Update
             */
            smbFile.setLastModified(System.currentTimeMillis());
        } catch (final FileStorageException e) {
            throw e;
        } catch (final SmbException e) {
            throw CIFSExceptionCodes.SMB_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    public TimedResult<File> getDocuments(final String folderId) throws FileStorageException {
        return new FileTimedResult(getFileList(folderId, null));
    }

    public TimedResult<File> getDocuments(final String folderId, final List<Field> fields) throws FileStorageException {
        return new FileTimedResult(getFileList(folderId, fields));
    }

    private List<File> getFileList(final String folderId, final List<Field> fields) throws FileStorageException {
        try {
            /*
             * Get & check folder
             */
            final String fid = checkFolderId(folderId, rootUrl);
            final SmbFile smbFolder = new SmbFile(fid, auth);
            if (!smbFolder.exists()) {
                throw CIFSExceptionCodes.NOT_FOUND.create(folderId);
            }
            if (!smbFolder.isDirectory()) {
                throw CIFSExceptionCodes.NOT_A_FOLDER.create(folderId);
            }
            /*
             * List its sub-resources
             */
            final SmbFile[] subFiles = smbFolder.listFiles();
            final List<File> files = new ArrayList<File>(subFiles.length);
            for (final SmbFile subFile : subFiles) {
                if (subFile.isFile()) {
                    files.add(new CIFSFile(fid, subFile.getName(), session.getUserId()).parseSmbFile(subFile, fields));
                }
            }
            /*
             * Return list
             */
            return files;
        } catch (final FileStorageException e) {
            throw e;
        } catch (final SmbException e) {
            throw CIFSExceptionCodes.SMB_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    public TimedResult<File> getDocuments(final String folderId, final List<Field> fields, final Field sort, final SortDirection order) throws FileStorageException {
        final List<File> files = getFileList(folderId, fields);
        /*
         * Sort list
         */
        Collections.sort(files, order.comparatorBy(sort));
        /*
         * Return sorted result
         */
        return new FileTimedResult(files);
    }

    public TimedResult<File> getVersions(final String folderId, final String id) throws FileStorageException {
        return new FileTimedResult(Collections.singletonList(getFileMetadata(folderId, id, CURRENT_VERSION)));
    }

    public TimedResult<File> getVersions(final String folderId, final String id, final List<Field> fields) throws FileStorageException {
        return new FileTimedResult(Collections.singletonList(getFileMetadata(folderId, id, CURRENT_VERSION)));
    }

    public TimedResult<File> getVersions(final String folderId, final String id, final List<Field> fields, final Field sort, final SortDirection order) throws FileStorageException {
        return new FileTimedResult(Collections.singletonList(getFileMetadata(folderId, id, CURRENT_VERSION)));
    }

    public TimedResult<File> getDocuments(final List<IDTuple> ids, final List<Field> fields) throws FileStorageException {
        try {
            /*
             * Iterate identifiers
             */
            final List<File> files = new ArrayList<File>(ids.size());
            for (final IDTuple id : ids) {
                final String fid = checkFolderId(id.getFolder(), rootUrl);
                final URI uri = new URI(fid + id.getId(), true);
                final SmbFile smbFile = new SmbFile(uri.toString(), auth);
                if (!smbFile.exists()) {
                    throw CIFSExceptionCodes.NOT_FOUND.create(uri.toString());
                }
                if (!smbFile.isFile()) {
                    /*
                     * Not a directory
                     */
                    throw CIFSExceptionCodes.NOT_A_FILE.create(uri.toString());
                }
                files.add(new CIFSFile(fid, id.getId(), session.getUserId()).parseSmbFile(smbFile, fields));
            }
            /*
             * Return
             */
            return new FileTimedResult(files);
        } catch (final FileStorageException e) {
            throw e;
        } catch (final SmbException e) {
            throw CIFSExceptionCodes.SMB_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    private static final SearchIterator<File> EMPTY_ITER = SearchIteratorAdapter.emptyIterator();

    public Delta<File> getDelta(final String folderId, final long updateSince, final List<Field> fields, final boolean ignoreDeleted) throws FileStorageException {
        return new FileDelta(EMPTY_ITER, EMPTY_ITER, EMPTY_ITER, 0L);
    }

    public Delta<File> getDelta(final String folderId, final long updateSince, final List<Field> fields, final Field sort, final SortDirection order, final boolean ignoreDeleted) throws FileStorageException {
        return new FileDelta(EMPTY_ITER, EMPTY_ITER, EMPTY_ITER, 0L);
    }

    public SearchIterator<File> search(final String pattern, final List<Field> fields, final String folderId, final Field sort, final SortDirection order, final int start, final int end) throws FileStorageException {
        final List<File> results;
        if (ALL_FOLDERS == folderId) {
            /*
             * Recursively search files in directories
             */
            results = new ArrayList<File>();
            recursiveSearchFile(pattern, rootUrl, fields, results);
        } else {
            /*
             * Get files from folder
             */
            results = getFileList(folderId, fields);
            /*
             * Filter by search pattern
             */
            for (final Iterator<File> iterator = results.iterator(); iterator.hasNext();) {
                final File file = iterator.next();
                if (!file.matches(pattern)) {
                    iterator.remove();
                }
            }
        }
        /*
         * Empty?
         */
        if (results.isEmpty()) {
            return SearchIteratorAdapter.emptyIterator();
        }
        /*
         * Sort
         */
        Collections.sort(results, order.comparatorBy(sort));
        /*
         * Consider start/end index
         */
        if (start != NOT_SET && end != NOT_SET && end > start) {

            final int fromIndex = start;
            int toIndex = end;
            if ((fromIndex) > results.size()) {
                /*
                 * Return empty iterator if start is out of range
                 */
                return SearchIteratorAdapter.emptyIterator();
            }
            /*
             * Reset end index if out of range
             */
            if (toIndex >= results.size()) {
                toIndex = results.size();
            }
            /*
             * Return
             */
            final List<File> subList = results.subList(fromIndex, toIndex);
            return new SearchIteratorAdapter<File>(subList.iterator(), subList.size());
        }
        /*
         * Return sorted result
         */
        return new SearchIteratorAdapter<File>(results.iterator(), results.size());
    }

    private void recursiveSearchFile(final String pattern, final String folderId, final List<Field> fields, final List<File> results) throws FileStorageException {
        try {
            /*
             * Check
             */
            final String fid = checkFolderId(folderId, rootUrl);
            final SmbFile smbFolder = new SmbFile(fid, auth);
            if (!smbFolder.exists()) {
                throw CIFSExceptionCodes.NOT_FOUND.create(folderId);
            }
            if (!smbFolder.isDirectory()) {
                throw CIFSExceptionCodes.NOT_A_FOLDER.create(folderId);
            }
            final SmbFile[] subFiles = smbFolder.listFiles();
            for (final SmbFile subFile : subFiles) {
                if (subFile.isDirectory()) {
                    recursiveSearchFile(pattern, subFile.getPath(), fields, results);
                } else {
                    final CIFSFile file = new CIFSFile(folderId, subFile.getName(), session.getUserId()).parseSmbFile(subFile, fields);
                    if (file.matches(pattern)) {
                        results.add(file);
                    }
                }
            }
        } catch (final FileStorageException e) {
            throw e;
        } catch (final SmbException e) {
            throw CIFSExceptionCodes.SMB_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    public FileStorageAccountAccess getAccountAccess() {
        return accountAccess;
    }

}
