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

package com.openexchange.file.storage.googledrive;

import static com.openexchange.java.Strings.isEmpty;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.File.Field;
import com.openexchange.file.storage.FileDelta;
import com.openexchange.file.storage.FileStorageAccount;
import com.openexchange.file.storage.FileStorageAccountAccess;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.file.storage.FileStorageFileAccess;
import com.openexchange.file.storage.FileStorageFolder;
import com.openexchange.file.storage.FileStorageSequenceNumberProvider;
import com.openexchange.file.storage.FileTimedResult;
import com.openexchange.file.storage.ThumbnailAware;
import com.openexchange.file.storage.googledrive.access.GoogleDriveAccess;
import com.openexchange.groupware.results.Delta;
import com.openexchange.groupware.results.TimedResult;
import com.openexchange.java.Strings;
import com.openexchange.session.Session;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIteratorAdapter;

/**
 * {@link GoogleDriveFileAccess}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class GoogleDriveFileAccess extends AbstractGoogleDriveAccess implements ThumbnailAware, FileStorageSequenceNumberProvider {

    private static final String QUERY_STRING_FILES_ONLY = GoogleDriveConstants.QUERY_STRING_FILES_ONLY;
    private static final String QUERY_STRING_FILES_ONLY_EXCLUDING_TRASH = GoogleDriveConstants.QUERY_STRING_FILES_ONLY_EXCLUDING_TRASH;

    // ----------------------------------------------------------------------------------------------------------------------- //

    private final GoogleDriveAccountAccess accountAccess;
    private final int userId;

    /**
     * Initializes a new {@link GoogleDriveFileAccess}.
     */
    public GoogleDriveFileAccess(GoogleDriveAccess googleDriveAccess, FileStorageAccount account, Session session, GoogleDriveAccountAccess accountAccess) throws OXException {
        super(googleDriveAccess, account, session);
        this.accountAccess = accountAccess;
        userId = session.getUserId();
    }

    @Override
    public void startTransaction() throws OXException {
        // Nope
    }

    @Override
    public void commit() throws OXException {
        // Nope
    }

    @Override
    public void rollback() throws OXException {
        // Nope
    }

    @Override
    public void finish() throws OXException {
        // Nope
    }

    @Override
    public void setTransactional(final boolean transactional) {
        // Nope
    }

    @Override
    public void setRequestTransactional(final boolean transactional) {
        // Nope
    }

    @Override
    public void setCommitsTransaction(final boolean commits) {
        // Nope
    }

    @Override
    public boolean exists(final String folderId, final String id, final String version) throws OXException {
        try {
            Drive drive = googleDriveAccess.getDrive(session);
            com.google.api.services.drive.model.File file = drive.files().get(id).execute();
            Boolean explicitlyTrashed = file.getExplicitlyTrashed();
            return !isDir(file) && (null == explicitlyTrashed || !explicitlyTrashed.booleanValue());
        } catch (final HttpResponseException e) {
            if (404 == e.getStatusCode()) {
                return false;
            }
            throw handleHttpResponseError(null, e);
        } catch (final IOException e) {
            throw GoogleDriveExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw GoogleDriveExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public File getFileMetadata(final String folderId, final String id, final String version) throws OXException {
        try {
            Drive drive = googleDriveAccess.getDrive(session);
            if (FileStorageFileAccess.CURRENT_VERSION == version) {

            } else {
//                drive.revisions().get(id, version).execute();
            }

            com.google.api.services.drive.model.File file = drive.files().get(id).execute();
            checkFileValidity(file);
            return createFile(folderId, id, file, null);
        } catch (final HttpResponseException e) {
            throw handleHttpResponseError(id, e);
        } catch (final IOException e) {
            throw GoogleDriveExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw GoogleDriveExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public IDTuple saveFileMetadata(final File file, final long sequenceNumber) throws OXException {
        return saveFileMetadata(file, sequenceNumber, null);
    }

    @Override
    public IDTuple saveFileMetadata(File file, long sequenceNumber, List<Field> modifiedFields) throws OXException {
        if (null == modifiedFields || modifiedFields.contains(Field.FILENAME)) {
            try {
                Drive drive = googleDriveAccess.getDrive(session);

                com.google.api.services.drive.model.File savedFile = new com.google.api.services.drive.model.File();
                if (FileStorageFileAccess.NEW != file.getId()) {
                    savedFile.setId(file.getId());
                    savedFile.setTitle(file.getFileName());
                    savedFile = drive.files().patch(file.getId(), savedFile).execute();
                } else {
                    savedFile.setTitle(file.getFileName());
                    savedFile = drive.files().insert(savedFile).execute();
                }
                return new IDTuple(file.getFolderId(), savedFile.getId());
            } catch (final HttpResponseException e) {
                throw handleHttpResponseError(file.getId(), e);
            } catch (final IOException e) {
                throw GoogleDriveExceptionCodes.IO_ERROR.create(e, e.getMessage());
            } catch (final RuntimeException e) {
                throw GoogleDriveExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
        }
        return new IDTuple(file.getFolderId(), file.getId());
    }

    @Override
    public IDTuple copy(IDTuple source, String version, String destFolder, File update, InputStream newFil, List<Field> modifiedFields) throws OXException {
        if (version != CURRENT_VERSION) {
            // can only copy the current revision
            throw GoogleDriveExceptionCodes.VERSIONING_NOT_SUPPORTED.create();
        }
        String id = source.getId();
        try {
            Drive drive = googleDriveAccess.getDrive(session);

            // Get source file
            com.google.api.services.drive.model.File srcFile = drive.files().get(id).execute();
            checkFileValidity(srcFile);

            // Determine destination identifier
            String destId = toGoogleDriveFolderId(destFolder);

            // Check destination folder
            String title = srcFile.getTitle();
            {
                String baseName;
                String ext;
                {
                    int dotPos = title.lastIndexOf('.');
                    if (dotPos > 0) {
                        baseName = title.substring(0, dotPos);
                        ext = title.substring(dotPos);
                    } else {
                        baseName = title;
                        ext = "";
                    }
                }
                int count = 1;
                boolean keepOn = true;
                while (keepOn) {
                    Drive.Children.List list = drive.children().list(destId);
                    list.setQ(new StringBuilder().append("title = '").append(title).append("' and ").append(QUERY_STRING_FILES_ONLY_EXCLUDING_TRASH).toString());

                    ChildList childList = list.execute();
                    if (!childList.getItems().isEmpty()) {
                        title = new StringBuilder(baseName).append(" (").append(count++).append(')').append(ext).toString();
                    } else {
                        keepOn = false;
                    }
                }
            }

            // Create a file at destination directory
            com.google.api.services.drive.model.File copy = new com.google.api.services.drive.model.File();
            copy.setTitle(title);
            copy.setParents(Collections.<ParentReference> singletonList(new ParentReference().setId(destId)));
            if (null != update) {
                if (false == Strings.isEmpty(update.getTitle()) && (null == modifiedFields || modifiedFields.contains(File.Field.FILENAME)) &&
                    false == update.getTitle().equals(srcFile.getTitle())) {
                    copy.setTitle(update.getTitle());
                }
            }

            // Copy file
            com.google.api.services.drive.model.File copiedFile = drive.files().copy(id, copy).execute();

            return new IDTuple(destFolder, copiedFile.getId());
        } catch (final HttpResponseException e) {
            throw handleHttpResponseError(id, e);
        } catch (final IOException e) {
            throw GoogleDriveExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw GoogleDriveExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public IDTuple move(IDTuple source, String destFolder, long sequenceNumber, File update, List<File.Field> modifiedFields) throws OXException {
        String id = source.getId();
        try {
            Drive drive = googleDriveAccess.getDrive(session);

            // Get source file
            com.google.api.services.drive.model.File srcFile = drive.files().get(id).execute();
            checkFileValidity(srcFile);

            // Determine destination identifier
            String destId = toGoogleDriveFolderId(destFolder);

            // Check destination folder
            {
                for (ParentReference parentReference : srcFile.getParents()) {
                    if (parentReference.getId().equals(destId)) {
                        return source;
                    }
                }
            }

            // Create patch file
            com.google.api.services.drive.model.File patch = new com.google.api.services.drive.model.File();
            patch.setParents(Collections.<ParentReference> singletonList(new ParentReference().setId(destFolder)));
            if (null != update) {
                if (false == Strings.isEmpty(update.getTitle()) && (null == modifiedFields || modifiedFields.contains(File.Field.FILENAME)) &&
                    false == update.getTitle().equals(srcFile.getTitle())) {
                    patch.setTitle(update.getTitle());
                }
            }

            // Patch the file
            com.google.api.services.drive.model.File patchedFile = drive.files().patch(id, patch).execute();

            return new IDTuple(destFolder, patchedFile.getId());
        } catch (final HttpResponseException e) {
            throw handleHttpResponseError(id, e);
        } catch (final IOException e) {
            throw GoogleDriveExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw GoogleDriveExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public InputStream getDocument(final String folderId, final String id, final String version) throws OXException {
        try {
            Drive drive = googleDriveAccess.getDrive(session);

            // Get file
            com.google.api.services.drive.model.File file = drive.files().get(id).execute();
            checkFileValidity(file);

            String downloadUrl = file.getDownloadUrl();
            if (downloadUrl == null || downloadUrl.length() <= 0) {
                // The file doesn't have any content stored on Drive.
                throw GoogleDriveExceptionCodes.NO_CONTENT.create(id);
            }

            HttpResponse resp = drive.getRequestFactory().buildGetRequest(new GenericUrl(downloadUrl)).execute();
            return resp.getContent();
        } catch (final HttpResponseException e) {
            throw handleHttpResponseError(id, e);
        } catch (final IOException e) {
            throw GoogleDriveExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw GoogleDriveExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public InputStream getThumbnailStream(String folderId, String id, String version) throws OXException {
        try {
            Drive drive = googleDriveAccess.getDrive(session);

            // Get file
            com.google.api.services.drive.model.File file = drive.files().get(id).setFields("thumbnailLink").execute();
            checkFileValidity(file);

            String thumbnailLink = file.getThumbnailLink();
            if (thumbnailLink == null || thumbnailLink.length() <= 0) {
                // The file doesn't have a thumbnail
                return null;
            }

            HttpResponse resp = drive.getRequestFactory().buildGetRequest(new GenericUrl(thumbnailLink)).execute();
            return resp.getContent();
        } catch (final HttpResponseException e) {
            throw handleHttpResponseError(id, e);
        } catch (final IOException e) {
            throw GoogleDriveExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw GoogleDriveExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public IDTuple saveDocument(File file, InputStream data, long sequenceNumber) throws OXException {
        return saveDocument(file, data, sequenceNumber, null);
    }

    @Override
    public IDTuple saveDocument(File file, InputStream data, long sequenceNumber, List<Field> modifiedFields) throws OXException {
        String id = file.getId();
        try {
            Drive drive = googleDriveAccess.getDrive(session);

            if (isEmpty(id) || !exists(null, id, CURRENT_VERSION)) {
                // Insert
                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                fileMetadata.setTitle(file.getFileName());
                // Determine folder identifier
                String folderId = FileStorageFolder.ROOT_FULLNAME.equals(file.getFolderId()) ? getRootFolderId() : file.getFolderId();
                fileMetadata.setParents(Collections.<ParentReference> singletonList(new ParentReference().setId(folderId)));

                Drive.Files.Insert insert = drive.files().insert(fileMetadata, new InputStreamContent(file.getFileMIMEType(), data));
                MediaHttpUploader uploader = insert.getMediaHttpUploader();
                uploader.setDirectUploadEnabled(true);
                String newId = insert.execute().getId();
                file.setId(newId);
                return new IDTuple(folderId, newId);
            } else {
                // Update
                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                fileMetadata.setId(id);
                fileMetadata.setTitle(file.getFileName());
                // Determine folder identifier
                String folderId = FileStorageFolder.ROOT_FULLNAME.equals(file.getFolderId()) ? getRootFolderId() : file.getFolderId();
                fileMetadata.setParents(Collections.<ParentReference> singletonList(new ParentReference().setId(folderId)));

                Drive.Files.Update update = drive.files().update(id, fileMetadata, new InputStreamContent(file.getFileMIMEType(), data));
                MediaHttpUploader uploader = update.getMediaHttpUploader();
                uploader.setDirectUploadEnabled(true);
                fileMetadata = update.execute();
                return new IDTuple(folderId, fileMetadata.getId());
            }
        } catch (final HttpResponseException e) {
            throw handleHttpResponseError(id, e);
        } catch (final IOException e) {
            throw GoogleDriveExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw GoogleDriveExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public void removeDocument(String folderId, long sequenceNumber) throws OXException {
        try {
            Drive drive = googleDriveAccess.getDrive(session);

            // Determine folder identifier
            String fid = toGoogleDriveFolderId(folderId);

            // Query all files
            Drive.Children.List list = drive.children().list(fid);
            list.setQ(QUERY_STRING_FILES_ONLY_EXCLUDING_TRASH);
            list.setFields("kind,nextPageToken,items(id)");

            boolean hardDelete = isTrashed(fid, drive);

            ChildList childList = list.execute();
            if (!childList.getItems().isEmpty()) {
                for (ChildReference child : childList.getItems()) {
                    if (hardDelete) {
                        drive.files().delete(child.getId()).execute();
                    } else {
                        drive.files().trash(child.getId()).execute();
                    }
                }

                String nextPageToken = childList.getNextPageToken();
                while (!isEmpty(nextPageToken)) {
                    list.setPageToken(nextPageToken);
                    childList = list.execute();
                    if (!childList.getItems().isEmpty()) {
                        for (ChildReference child : childList.getItems()) {
                            if (hardDelete) {
                                drive.files().delete(child.getId()).execute();
                            } else {
                                drive.files().trash(child.getId()).execute();
                            }
                        }
                    }

                    nextPageToken = childList.getNextPageToken();
                }
            }
        } catch (final HttpResponseException e) {
            throw handleHttpResponseError(folderId, e);
        } catch (final IOException e) {
            throw GoogleDriveExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw GoogleDriveExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public List<IDTuple> removeDocument(List<IDTuple> ids, long sequenceNumber) throws OXException {
        return removeDocument(ids, sequenceNumber, false);
    }

    @Override
    public List<IDTuple> removeDocument(List<IDTuple> ids, long sequenceNumber, boolean hardDelete) throws OXException {
        try {
            Drive drive = googleDriveAccess.getDrive(session);

            List<IDTuple> ret = new ArrayList<IDTuple>(ids.size());
            for (IDTuple id : ids) {
                try {
                    boolean delete = hardDelete || isTrashed(toGoogleDriveFolderId(id.getFolder()), drive);
                    if (delete) {
                        drive.files().delete(id.getId()).execute();
                    } else {
                        drive.files().trash(id.getId()).execute();
                    }
                } catch (final HttpResponseException e) {
                    if (404 != e.getStatusCode()) {
                        ret.add(id);
                    } else {
                        throw e;
                    }
                }
            }
            return ret;
        } catch (final HttpResponseException e) {
            throw handleHttpResponseError(null, e);
        } catch (final IOException e) {
            throw GoogleDriveExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw GoogleDriveExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public void touch(String folderId, String id) throws OXException {
        exists(folderId, id, CURRENT_VERSION);
    }

    @Override
    public TimedResult<File> getDocuments(String folderId) throws OXException {
        try {
            Drive drive = googleDriveAccess.getDrive(session);

            // Determine folder identifier
            String fid = toGoogleDriveFolderId(folderId);

            Drive.Files.List list = drive.files().list();
            list.setQ(QUERY_STRING_FILES_ONLY_EXCLUDING_TRASH + " and '" + fid + "' in parents");
            {
                StringBuilder googleDriveFields = new StringBuilder(512);
                googleDriveFields.append("kind,nextPageToken,items(");
                googleDriveFields.append(GoogleDriveConstants.FIELDS_DEFAULT).append(')');
                list.setFields(googleDriveFields.toString());
            }

            FileList fileList = list.execute();
            List<File> files = new LinkedList<File>();
            if (!fileList.getItems().isEmpty()) {
                for (com.google.api.services.drive.model.File child : fileList.getItems()) {
                    files.add(createFile(folderId, child.getId(), child, null));
                }

                String nextPageToken = fileList.getNextPageToken();
                while (!isEmpty(nextPageToken)) {
                    list.setPageToken(nextPageToken);
                    fileList = list.execute();
                    if (!fileList.getItems().isEmpty()) {
                        for (com.google.api.services.drive.model.File child : fileList.getItems()) {
                            files.add(createFile(folderId, child.getId(), child, null));
                        }
                    }

                    nextPageToken = fileList.getNextPageToken();
                }
            }

            return new FileTimedResult(files);
        } catch (final HttpResponseException e) {
            throw handleHttpResponseError(folderId, e);
        } catch (final IOException e) {
            throw GoogleDriveExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw GoogleDriveExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public TimedResult<File> getDocuments(final String folderId, final List<Field> fields) throws OXException {
        return getDocuments(folderId);
    }

    @Override
    public TimedResult<File> getDocuments(String folderId, List<Field> fields, Field sort, SortDirection order) throws OXException {
        try {
            Drive drive = googleDriveAccess.getDrive(session);

            // Determine folder identifier
            String fid = toGoogleDriveFolderId(folderId);

            Drive.Files.List list = drive.files().list();
            list.setQ(QUERY_STRING_FILES_ONLY_EXCLUDING_TRASH + " and '" + fid + "' in parents");
            {
                StringBuilder googleDriveFields = new StringBuilder(512);
                googleDriveFields.append("kind,nextPageToken,items(");
                for (Field field : fields) {
                    appendField(field, googleDriveFields);
                }
                if (null != sort) {
                    appendField(sort, googleDriveFields);
                }
                googleDriveFields.setCharAt(googleDriveFields.length() - 1, ')');
                list.setFields(googleDriveFields.toString());
            }

            FileList fileList = list.execute();
            List<com.google.api.services.drive.model.File> items = fileList.getItems();
            List<File> files = new LinkedList<File>();
            if (!items.isEmpty()) {
                for (com.google.api.services.drive.model.File child : items) {
                    files.add(createFile(folderId, child.getId(), child, fields));
                }

                String nextPageToken = fileList.getNextPageToken();
                while (!isEmpty(nextPageToken)) {
                    list.setPageToken(nextPageToken);
                    fileList = list.execute();
                    items = fileList.getItems();
                    if (!items.isEmpty()) {
                        for (com.google.api.services.drive.model.File child : items) {
                            files.add(createFile(folderId, child.getId(), child, fields));
                        }
                    }

                    nextPageToken = fileList.getNextPageToken();
                }
            }

            // Sort collection if needed
            sort(files, sort, order);
            return new FileTimedResult(files);
        } catch (final HttpResponseException e) {
            throw handleHttpResponseError(folderId, e);
        } catch (final IOException e) {
            throw GoogleDriveExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw GoogleDriveExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public TimedResult<File> getDocuments(List<IDTuple> ids, List<Field> fields) throws OXException {
        try {
            Drive drive = googleDriveAccess.getDrive(session);

            List<File> files = new LinkedList<File>();
            for (IDTuple idTuple : ids) {
                String fileId = idTuple.getId();
                try {
                    files.add(createFile(idTuple.getFolder(), fileId, drive.files().get(fileId).execute(), fields));
                } catch (HttpResponseException e) {
                    if (404 == e.getStatusCode()) {
                        throw FileStorageExceptionCodes.FILE_NOT_FOUND.create(e, idTuple.getId(), idTuple.getFolder());
                    }
                    throw e;
                }
            }

            return new FileTimedResult(files);
        } catch (HttpResponseException e) {
            throw handleHttpResponseError(null, e);
        } catch (IOException e) {
            throw GoogleDriveExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw GoogleDriveExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    private static final SearchIterator<File> EMPTY_ITER = SearchIteratorAdapter.emptyIterator();

    @Override
    public Delta<File> getDelta(String folderId, long updateSince, List<Field> fields, boolean ignoreDeleted) throws OXException {
        return new FileDelta(EMPTY_ITER, EMPTY_ITER, EMPTY_ITER, 0L);
    }

    @Override
    public Delta<File> getDelta(String folderId, long updateSince, List<Field> fields, Field sort, SortDirection order, boolean ignoreDeleted) throws OXException {
        return new FileDelta(EMPTY_ITER, EMPTY_ITER, EMPTY_ITER, 0L);
    }

    @Override
    public SearchIterator<File> search(String pattern, List<Field> fields, String folderId, Field sort, SortDirection order, int start, int end) throws OXException {
        try {
            // Search by pattern
            List<File> files = searchByFileNamePattern(pattern, folderId);

            // Sort collection
            sort(files, sort, order);

            // Start, end...
            if ((start != NOT_SET) && (end != NOT_SET)) {
                final int size = files.size();
                if ((start) > size) {
                    /*
                     * Return empty iterator if start is out of range
                     */
                    return SearchIteratorAdapter.emptyIterator();
                }
                /*
                 * Reset end index if out of range
                 */
                int toIndex = end;
                if (toIndex >= size) {
                    toIndex = size;
                }
                files = files.subList(start, toIndex);
            }

            return new SearchIteratorAdapter<File>(files.iterator(), files.size());
        } catch (final RuntimeException e) {
            throw GoogleDriveExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public Map<String, Long> getSequenceNumbers(List<String> folderIds) throws OXException {
        Long largestChangeId;
        try {
            Drive drive = googleDriveAccess.getDrive(session);
            ChangeList changeList = drive.changes().list().setFields("largestChangeId").execute();
            largestChangeId = changeList.getLargestChangeId();
        } catch (HttpResponseException e) {
            throw handleHttpResponseError(null, e);
        } catch (IOException e) {
            throw GoogleDriveExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw GoogleDriveExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
        Map<String, Long> sequenceNumbers = new HashMap<String, Long>(folderIds.size());
        for (String folderId : folderIds) {
            sequenceNumbers.put(folderId, largestChangeId);
        }
        return sequenceNumbers;
    }

    private List<File> searchByFileNamePattern(String pattern, String folderId) throws OXException {
        try {
            Drive drive = googleDriveAccess.getDrive(session);
            String fid = null == folderId ? null : toGoogleDriveFolderId(folderId);

            if (isEmpty(pattern)) {
                // Get all files
                List<File> files = new LinkedList<File>();
                {
                    Drive.Files.List list = drive.files().list();
                    {
                        StringBuilder qBuilder = new StringBuilder(128).append(QUERY_STRING_FILES_ONLY);
                        if (null != fid) {
                            qBuilder.append(" and '").append(fid).append("' in parents");
                        }
                        list.setQ(qBuilder.toString());
                    }

                    FileList fileList = list.execute();
                    if (!fileList.getItems().isEmpty()) {
                        for (com.google.api.services.drive.model.File file : fileList.getItems()) {
                            files.add(createFile(folderId, file.getId(), file, null));
                        }

                        String nextPageToken = fileList.getNextPageToken();
                        while (!isEmpty(nextPageToken)) {
                            list.setPageToken(nextPageToken);
                            fileList = list.execute();
                            if (!fileList.getItems().isEmpty()) {
                                for (com.google.api.services.drive.model.File file : fileList.getItems()) {
                                    files.add(createFile(folderId, file.getId(), file, null));
                                }
                            }

                            nextPageToken = fileList.getNextPageToken();
                        }
                    }
                }

                return files;
            }

            // Search by pattern
            List<File> files = new LinkedList<File>();
            {
                Drive.Files.List list = drive.files().list();
                list.setFields("kind,nextPageToken,items("+GoogleDriveConstants.FIELDS_DEFAULT+")");

                {
                    StringBuilder qBuilder = new StringBuilder(128);
                    qBuilder.append('\'').append(fid).append("' in parents and ");
                    qBuilder.append("title contains '").append(pattern).append("' and ").append(QUERY_STRING_FILES_ONLY);
                    if (null != fid) {
                        qBuilder.append(" and '").append(fid).append("' in parents");
                    }
                    list.setQ(qBuilder.toString());
                }

                FileList fileList = list.execute();
                if (!fileList.getItems().isEmpty()) {
                    for (com.google.api.services.drive.model.File child : fileList.getItems()) {
                        files.add(createFile(folderId, child.getId(), child, null));
                    }

                    String nextPageToken = fileList.getNextPageToken();
                    while (!isEmpty(nextPageToken)) {
                        list.setPageToken(nextPageToken);
                        fileList = list.execute();
                        if (!fileList.getItems().isEmpty()) {
                            for (com.google.api.services.drive.model.File child : fileList.getItems()) {
                                files.add(createFile(folderId, child.getId(), child, null));
                            }
                        }

                        nextPageToken = fileList.getNextPageToken();
                    }
                }
            }

            return files;
        } catch (final HttpResponseException e) {
            throw handleHttpResponseError(null, e);
        } catch (final IOException e) {
            throw GoogleDriveExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw GoogleDriveExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public FileStorageAccountAccess getAccountAccess() {
        return accountAccess;
    }

    /**
     * Creates a {@link GoogleDriveFile} based on a {@link com.google.api.services.drive.model.File}.
     *
     * @param folderId The folder identifier to apply
     * @param fileId The file identifier to apply
     * @param file The file
     * @param fields The fields to assign, or <code>null</code> to set all fields
     * @return The file
     */
    private GoogleDriveFile createFile(String folderId, String fileId, com.google.api.services.drive.model.File file, List<Field> fields) throws OXException {
        return new GoogleDriveFile(folderId, fileId, userId, getRootFolderId()).parseGoogleDriveFile(file, fields);
    }

    private void checkFileValidity(com.google.api.services.drive.model.File file) throws OXException {
        if (isDir(file)) {
            throw GoogleDriveExceptionCodes.NOT_A_FILE.create(file.getId());
        }
        checkIfTrashed(file);
    }

    /**
     * Sorts the supplied list of files if needed.
     *
     * @param files The files to sort
     * @param sort The sort order, or <code>null</code> if not specified
     * @param order The sort direction
     */
    private static void sort(List<File> files, Field sort, SortDirection order) {
        if (null != sort && 1 < files.size()) {
            Collections.sort(files, order.comparatorBy(sort));
        }
    }

    private static void appendField(Field field, StringBuilder sb) {
        switch (field) {
        case ID:
            if (sb.indexOf("id") < 0) {
                sb.append("id,");
            }
            break;
        case CREATED:
            if (sb.indexOf("createdDate") < 0) {
                sb.append("createdDate,");
            }
            break;
        case LAST_MODIFIED:
            /* fall-through */
        case LAST_MODIFIED_UTC:
            if (sb.indexOf("modifiedDate") < 0) {
                sb.append("modifiedDate,");
            }
            break;
        case TITLE:
            /* fall-through */
        case FILENAME:
            if (sb.indexOf("title") < 0) {
                sb.append("title,");
            }
            break;
        case FILE_MIMETYPE:
            if (sb.indexOf("mimeType") < 0) {
                sb.append("mimeType,");
            }
            break;
        case FILE_SIZE:
            if (sb.indexOf("fileSize") < 0) {
                sb.append("fileSize,");
            }
            break;
        case URL:
            if (sb.indexOf("downloadUrl") < 0) {
                sb.append("downloadUrl,");
            }
            break;
        case DESCRIPTION:
            if (sb.indexOf("description") < 0) {
                sb.append("description,");
            }
            break;
        case VERSION:
            if (sb.indexOf("version") < 0) {
                sb.append("version,");
            }
            break;
        case FILE_MD5SUM:
            if (sb.indexOf("md5Checksum") < 0) {
                sb.append("md5Checksum,");
            }
        default:
            break;
        }
    }

}
