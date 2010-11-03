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

package com.openexchange.file.storage.webdav;

import static com.openexchange.file.storage.webdav.WebDAVFileStorageResourceUtil.checkFolderId;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.LockMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PropPatchMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.client.methods.UnLockMethod;
import org.apache.jackrabbit.webdav.header.CodedUrlHeader;
import org.apache.jackrabbit.webdav.header.IfHeader;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.transaction.TransactionConstants;
import org.apache.jackrabbit.webdav.transaction.TransactionInfo;
import org.w3c.dom.Element;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.File.Field;
import com.openexchange.file.storage.FileStorageAccount;
import com.openexchange.file.storage.FileStorageAccountAccess;
import com.openexchange.file.storage.FileStorageException;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.file.storage.FileStorageFileAccess;
import com.openexchange.file.storage.FileTimedResult;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.results.Delta;
import com.openexchange.groupware.results.TimedResult;
import com.openexchange.session.Session;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIteratorAdapter;
import com.openexchange.tx.TransactionException;

/**
 * {@link WebDAVFileStorageFileAccess}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class WebDAVFileStorageFileAccess extends AbstractWebDAVAccess implements FileStorageFileAccess {

    private static final class LockTokenKey {

        private final String folderId;

        private final String id;

        public LockTokenKey(final String folderId, final String id) {
            super();
            this.folderId = folderId;
            this.id = id;
        }

        public String getFolderId() {
            return folderId;
        }

        public String getId() {
            return id;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((folderId == null) ? 0 : folderId.hashCode());
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof LockTokenKey)) {
                return false;
            }
            final LockTokenKey other = (LockTokenKey) obj;
            if (folderId == null) {
                if (other.folderId != null) {
                    return false;
                }
            } else if (!folderId.equals(other.folderId)) {
                return false;
            }
            if (id == null) {
                if (other.id != null) {
                    return false;
                }
            } else if (!id.equals(other.id)) {
                return false;
            }
            return true;
        }

    }

    private final Map<LockTokenKey, String> lockTokenMap;

    private final String rootUri;

    private final WebDAVFileStorageAccountAccess accountAccess;

    private volatile String transactionToken;

    /**
     * Initializes a new {@link WebDAVFileStorageFileAccess}.
     */
    public WebDAVFileStorageFileAccess(final HttpClient client, final FileStorageAccount account, final WebDAVFileStorageAccountAccess accountAccess, final Session session) {
        super(client, account, session);
        rootUri = (String) account.getConfiguration().get(WebDAVConstants.WEBDAV_URL);
        this.accountAccess = accountAccess;
        lockTokenMap = new ConcurrentHashMap<LockTokenKey, String>();
    }

    /**
     * Clean up.
     */
    public void cleanUp() {
        /*
         * Remove all pending locks
         */
        for (final Entry<LockTokenKey, String> entry : lockTokenMap.entrySet()) {
            try {
                unlock0(entry.getKey(), entry.getValue());
            } catch (final FileStorageException e) {
                org.apache.commons.logging.LogFactory.getLog(WebDAVFileStorageFileAccess.class).error(e.getMessage(), e);
            }
        }
    }

    private void initMethod(final String folderId, final String id, final DavMethod davMethod) {
        initMethod(folderId, id, davMethod, !isLockMethod(davMethod));
    }

    private void initMethod(final String folderId, final String id, final DavMethod davMethod, final boolean addIfHeader) {
        if (addIfHeader) {
            // http://blog.ropardo.ro/getting-the-lock-token-for-an-item-in-jackrabbit/
            // http://www.koders.com/java/fid411ED72BF4F6D245D33A7E6A4F48127AF190657A.aspx?s=mdef:getUserId
            final String thisTransactionToken = transactionToken;
            if (null != thisTransactionToken) {
                CodedUrlHeader codedUrl = new CodedUrlHeader("Transaction", thisTransactionToken);
                davMethod.setRequestHeader(codedUrl);
                codedUrl = new CodedUrlHeader(TransactionConstants.HEADER_TRANSACTIONID, thisTransactionToken);
                davMethod.setRequestHeader(codedUrl);
            }
            if (null != folderId && null != id) {
                // Lock token from former exclusive lock for a certain item
                final String lockToken = lockTokenMap.get(new LockTokenKey(folderId, id));
                if (null != lockToken) {
                    final IfHeader ifH = new IfHeader(new String[] { lockToken });
                    davMethod.setRequestHeader(ifH.getHeaderName(), ifH.getHeaderValue());
                }
            }
        }
    }

    private static boolean isLockMethod(final DavMethod method) {
        final int code = DavMethods.getMethodCode(method.getName());
        return DavMethods.DAV_LOCK == code || DavMethods.DAV_UNLOCK == code;
    }

    public void startTransaction() throws TransactionException {
        if (null != transactionToken) {
            /*
             * Transaction already started
             */
            return;
        }
        try {
            final URI uri = new URI(rootUri, true);
            /*
             * Create proper LockInfo
             */
            // final LockInfo lockInfo = new LockInfo(DavConstants.INFINITE_TIMEOUT);
            // lockInfo.setType(Type.create(TransactionConstants.XML_TRANSACTION, DavConstants.NAMESPACE)); // or
            // lockInfo.setType(TransactionConstants.TRANSACTION); with different namespace "dcr" instead of "d"
            // lockInfo.setScope(Scope.create(TransactionConstants.XML_GLOBAL, DavConstants.NAMESPACE)); // or
            // lockInfo.setScope(TransactionConstants.GLOBAL); with different namespace "dcr" instead of "d"
            // final LockMethod lockMethod = new LockMethod(uri.toString(), lockInfo);

            final LockMethod lockMethod =
                new LockMethod(
                    uri.toString(),
                    TransactionConstants.LOCAL,
                    TransactionConstants.TRANSACTION,
                    null,
                    DavConstants.INFINITE_TIMEOUT,
                    true);
            try {
                client.executeMethod(lockMethod);
                /*
                 * Check if request was successfully executed
                 */
                lockMethod.checkSuccess();
                /*
                 * Obtain & remember lock token
                 */
                final String lockToken = lockMethod.getLockToken();
                transactionToken = lockToken;
            } finally {
                lockMethod.releaseConnection();
            }
        } catch (final HttpException e) {
            throw new TransactionException(WebDAVFileStorageExceptionCodes.HTTP_ERROR.create(e, e.getMessage()));
        } catch (final IOException e) {
            throw new TransactionException(FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage()));
        } catch (final DavException e) {
            throw new TransactionException(WebDAVFileStorageExceptionCodes.DAV_ERROR.create(e, e.getMessage()));
        }
    }

    public void commit() throws TransactionException {
        if (null == transactionToken) {
            /*
             * Transaction not started
             */
            return;
        }
        try {
            final URI uri = new URI(rootUri, true);
            final UnLockMethod method = new UnLockMethod(uri.toString(), transactionToken);
            try {
                method.setRequestBody(new TransactionInfo(true)); // COMMIT
                client.executeMethod(method);
                /*
                 * Check if request was successfully executed
                 */
                method.checkSuccess();
                transactionToken = null;
            } finally {
                method.releaseConnection();
            }
        } catch (final HttpException e) {
            throw new TransactionException(WebDAVFileStorageExceptionCodes.HTTP_ERROR.create(e, e.getMessage()));
        } catch (final IOException e) {
            throw new TransactionException(FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage()));
        } catch (final DavException e) {
            throw new TransactionException(WebDAVFileStorageExceptionCodes.DAV_ERROR.create(e, e.getMessage()));
        }
    }

    public void rollback() throws TransactionException {
        if (null == transactionToken) {
            /*
             * Transaction not started
             */
            return;
        }
        try {
            final URI uri = new URI(rootUri, true);
            final UnLockMethod method = new UnLockMethod(uri.toString(), transactionToken);
            try {
                method.setRequestBody(new TransactionInfo(false)); // ROLL-BACK
                client.executeMethod(method);
                /*
                 * Check if request was successfully executed
                 */
                method.checkSuccess();
                transactionToken = null;
            } finally {
                method.releaseConnection();
            }
        } catch (final HttpException e) {
            throw new TransactionException(WebDAVFileStorageExceptionCodes.HTTP_ERROR.create(e, e.getMessage()));
        } catch (final IOException e) {
            throw new TransactionException(FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage()));
        } catch (final DavException e) {
            throw new TransactionException(WebDAVFileStorageExceptionCodes.DAV_ERROR.create(e, e.getMessage()));
        }
    }

    public void finish() throws TransactionException {
        /*
         * Nope
         */
    }

    public void setTransactional(final boolean transactional) {
        // TODO Auto-generated method stub

    }

    public void setRequestTransactional(final boolean transactional) {
        // TODO Auto-generated method stub

    }

    public void setCommitsTransaction(final boolean commits) {
        // Nope
    }

    public IDTuple copy(IDTuple source, String destFolder, File update, InputStream newFil, List<Field> modifiedFields) throws FileStorageException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean exists(final String folderId, final String id, final int version) throws FileStorageException {
        try {
            final String fid = checkFolderId(folderId);
            final URI uri = new URI(fid + id, true);
            final DavMethod propFindMethod = new PropFindMethod(fid, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
            try {
                initMethod(fid, id, propFindMethod);
                client.executeMethod(propFindMethod);
                /*
                 * Check if request was successfully executed
                 */
                propFindMethod.checkSuccess();
                /*
                 * Get MultiStatus response
                 */
                final MultiStatus multiStatus = propFindMethod.getResponseBodyAsMultiStatus();
                /*
                 * Find MultiStatus for specified folder URI
                 */
                final URI tmp = new URI(fid, true);
                for (final MultiStatusResponse multiStatusResponse : multiStatus.getResponses()) {
                    tmp.setEscapedPath(multiStatusResponse.getHref());
                    if (uri.equals(tmp)) {
                        /*
                         * Set for 200 (OK) status
                         */
                        final DavPropertySet propertySet = multiStatusResponse.getProperties(HttpServletResponse.SC_OK);
                        if (null != propertySet && !propertySet.isEmpty()) {
                            /*
                             * Check for collection
                             */
                            @SuppressWarnings("unchecked") final DavProperty<Element> davProperty =
                                (DavProperty<Element>) propertySet.get(DavConstants.PROPERTY_RESOURCETYPE);
                            final Element resourceType = davProperty.getValue();
                            if (null != resourceType && "collection".equalsIgnoreCase(resourceType.getLocalName())) {
                                /*
                                 * Not a file
                                 */
                                throw WebDAVFileStorageExceptionCodes.NOT_A_FILE.create(id);
                            }
                            /*
                             * Found file
                             */
                            return true;
                        }
                    }
                }
                return false;
            } finally {
                propFindMethod.releaseConnection();
            }
        } catch (final FileStorageException e) {
            throw e;
        } catch (final HttpException e) {
            throw WebDAVFileStorageExceptionCodes.HTTP_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final DavException e) {
            throw WebDAVFileStorageExceptionCodes.DAV_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    public File getFileMetadata(final String folderId, final String id, final int version) throws FileStorageException {
        if (version != CURRENT_VERSION) {
            throw WebDAVFileStorageExceptionCodes.VERSIONING_NOT_SUPPORTED.create();
        }
        try {
            final String fid = checkFolderId(folderId);
            final URI uri = new URI(fid + id, true);
            final DavMethod propFindMethod = new PropFindMethod(fid, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
            try {
                initMethod(fid, id, propFindMethod);
                client.executeMethod(propFindMethod);
                /*
                 * Check if request was successfully executed
                 */
                propFindMethod.checkSuccess();
                /*
                 * Get MultiStatus response
                 */
                final MultiStatus multiStatus = propFindMethod.getResponseBodyAsMultiStatus();
                /*
                 * Find MultiStatus for specified folder URI
                 */
                final URI tmp = new URI(fid, true);
                for (final MultiStatusResponse multiStatusResponse : multiStatus.getResponses()) {
                    tmp.setEscapedPath(multiStatusResponse.getHref());
                    if (uri.equals(tmp)) {
                        /*
                         * Set for 200 (OK) status
                         */
                        final DavPropertySet propertySet = multiStatusResponse.getProperties(HttpServletResponse.SC_OK);
                        if (null != propertySet && !propertySet.isEmpty()) {
                            /*
                             * Check for collection
                             */
                            @SuppressWarnings("unchecked") final DavProperty<Element> davProperty =
                                (DavProperty<Element>) propertySet.get(DavConstants.PROPERTY_RESOURCETYPE);
                            final Element resourceType = davProperty.getValue();
                            if (null != resourceType && "collection".equalsIgnoreCase(resourceType.getLocalName())) {
                                /*
                                 * Not a file
                                 */
                                throw WebDAVFileStorageExceptionCodes.NOT_A_FILE.create(id);
                            }
                            /*
                             * Found file
                             */
                            return new WebDAVFileStorageFile(fid, id, session.getUserId()).parseDavPropertySet(propertySet);
                        }
                    }
                }
                /*
                 * File not found
                 */
                throw FileStorageExceptionCodes.FILE_NOT_FOUND.create(id, fid);
            } finally {
                propFindMethod.releaseConnection();
            }
        } catch (final FileStorageException e) {
            throw e;
        } catch (final HttpException e) {
            throw WebDAVFileStorageExceptionCodes.HTTP_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final DavException e) {
            throw WebDAVFileStorageExceptionCodes.DAV_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    public void saveFileMetadata(final File file, final long sequenceNumber) throws FileStorageException {
        saveFileMetadata0(file, null);
    }

    public void saveFileMetadata(final File file, final long sequenceNumber, final List<Field> modifiedFields) throws FileStorageException {
        saveFileMetadata0(file, modifiedFields);
    }

    private void saveFileMetadata0(final File file, final List<Field> modifiedFields) throws FileStorageException {
        try {
            final String folderId = checkFolderId(file.getFolderId());
            final String id;
            {
                final String fid = file.getId();
                if (null == fid) {
                    final String name = file.getFileName();
                    if (null == name) {
                        throw WebDAVFileStorageExceptionCodes.MISSING_FILE_NAME.create();
                    }
                    id = name;
                } else {
                    id = fid;
                }
            }
            final URI uri = new URI(folderId + id, true);
            /*
             * Convert file to DAV representation
             */
            final WebDAVFileStorageDavRepr davRepr = new WebDAVFileStorageDavRepr(file, modifiedFields);
            /*
             * Check existence
             */
            if (!exists(folderId, id, CURRENT_VERSION)) {
                lock(folderId, id, FileStorageFileAccess.DISTANT_FUTURE);
                try {
                    /*
                     * Perform PUT first to create the resource
                     */
                    final PutMethod putMethod = new PutMethod(uri.toString());
                    try {
                        initMethod(folderId, id, putMethod);
                        client.executeMethod(putMethod);
                        /*
                         * Check if request was successfully executed
                         */
                        putMethod.checkSuccess();
                        file.setId(id);
                    } finally {
                        putMethod.releaseConnection();
                    }
                } finally {
                    unlock(folderId, id);
                }
            }
            /*
             * Perform PropPatch
             */
            final DavMethod davMethod = new PropPatchMethod(uri.toString(), davRepr.getSetProperties(), davRepr.getRemoveProperties());
            try {
                initMethod(folderId, id, davMethod);
                client.executeMethod(davMethod);
                /*
                 * Check if request was successfully executed
                 */
                davMethod.checkSuccess();
            } finally {
                davMethod.releaseConnection();
            }
        } catch (final HttpException e) {
            throw WebDAVFileStorageExceptionCodes.HTTP_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final DavException e) {
            throw WebDAVFileStorageExceptionCodes.DAV_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            if (e instanceof FileStorageException) {
                throw (FileStorageException) e;
            }
            if (e instanceof AbstractOXException) {
                throw new FileStorageException((AbstractOXException) e);
            }
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    public InputStream getDocument(final String folderId, final String id, final int version) throws FileStorageException {
        try {
            final String fid = checkFolderId(folderId);
            final URI uri = new URI(fid + id, true);
            final GetMethod getMethod = new GetMethod(uri.toString());
            try {
                // initMethod(folderId, id, getMethod);
                client.executeMethod(getMethod);
                /*
                 * Check if request was successfully executed
                 */
                if (HttpServletResponse.SC_NOT_FOUND == getMethod.getStatusCode()) {
                    throw FileStorageExceptionCodes.FILE_NOT_FOUND.create(fid, id);
                }
                if (HttpServletResponse.SC_OK != getMethod.getStatusCode()) {
                    throw WebDAVFileStorageExceptionCodes.HTTP_ERROR.create(getMethod.getStatusText());
                }
                /*
                 * Return as stream
                 */
                return new HttpMethodInputStream(getMethod);
            } finally {
                /*
                 * No connection release since stream is returned to caller
                 */
                // getMethod.releaseConnection();
            }
        } catch (final FileStorageException e) {
            throw e;
        } catch (final HttpException e) {
            throw WebDAVFileStorageExceptionCodes.HTTP_ERROR.create(e, e.getMessage());
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

    private void saveDocument0(final File file, final InputStream data, final List<Field> modifiedColumns) throws FileStorageException {
        try {
            /*
             * Save metadata
             */
            saveFileMetadata0(file, modifiedColumns);
            /*
             * Save content
             */
            final String folderId = checkFolderId(file.getFolderId());
            final String id = file.getId();
            final URI uri = new URI(checkFolderId(folderId) + id, true);
            final PutMethod putMethod = new PutMethod(uri.toString());
            putMethod.setRequestEntity(new InputStreamRequestEntity(data));
            try {
                initMethod(folderId, id, putMethod);
                client.executeMethod(putMethod);
                /*
                 * Check if request was successfully executed
                 */
                putMethod.checkSuccess();
            } finally {
                putMethod.releaseConnection();
            }
        } catch (final FileStorageException e) {
            throw e;
        } catch (final HttpException e) {
            throw WebDAVFileStorageExceptionCodes.HTTP_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final DavException e) {
            throw WebDAVFileStorageExceptionCodes.DAV_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            if (e instanceof AbstractOXException) {
                throw new FileStorageException((AbstractOXException) e);
            }
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            /*
             * Close given stream
             */
            try {
                data.close();
            } catch (final IOException e) {
                org.apache.commons.logging.LogFactory.getLog(WebDAVFileStorageFileAccess.class).error(e.getMessage(), e);
            }
        }
    }

    public void removeDocument(final String folderId, final long sequenceNumber) throws FileStorageException {
        accountAccess.getFolderAccess().clearFolder(checkFolderId(folderId));
    }

    public List<IDTuple> removeDocument(final List<IDTuple> ids, final long sequenceNumber) throws FileStorageException {
        try {
            final List<IDTuple> ret = new ArrayList<IDTuple>(ids.size());
            for (final IDTuple idTuple : ids) {
                final String folderId = checkFolderId(idTuple.getFolder());
                final String id = idTuple.getId();
                final URI uri = new URI(checkFolderId(folderId) + id, true);
                final DeleteMethod deleteMethod = new DeleteMethod(uri.toString());
                try {
                    initMethod(folderId, id, deleteMethod);
                    client.executeMethod(deleteMethod);
                    /*
                     * Check if request was successfully executed
                     */
                    final int statusCode = deleteMethod.getStatusCode();
                    if (HttpServletResponse.SC_NOT_FOUND == statusCode) {
                        /*
                         * No-op for us...
                         */
                    } else if (HttpServletResponse.SC_OK != statusCode && HttpServletResponse.SC_NO_CONTENT != statusCode ) {
                        ret.add(idTuple);
                    }
                } finally {
                    deleteMethod.releaseConnection();
                }
            }
            return ret;
        } catch (final HttpException e) {
            throw WebDAVFileStorageExceptionCodes.HTTP_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    public int[] removeVersion(final String folderId, final String id, final int[] versions) throws FileStorageException {
        for (final int version : versions) {
            if (version != CURRENT_VERSION) {
                throw WebDAVFileStorageExceptionCodes.VERSIONING_NOT_SUPPORTED.create();
            }
        }
        try {
            final String fid = checkFolderId(folderId);
            final URI uri = new URI(fid + id, true);
            final DeleteMethod deleteMethod = new DeleteMethod(uri.toString());
            try {
                initMethod(fid, id, deleteMethod);
                client.executeMethod(deleteMethod);
                /*
                 * Check if request was successfully executed
                 */
                if (HttpServletResponse.SC_NOT_FOUND == deleteMethod.getStatusCode()) {
                    /*
                     * No-op for us...
                     */
                } else if (HttpServletResponse.SC_OK != deleteMethod.getStatusCode()) {
                    throw WebDAVFileStorageExceptionCodes.HTTP_ERROR.create(deleteMethod.getStatusText());
                }
            } finally {
                deleteMethod.releaseConnection();
            }
            return new int[0];
        } catch (final HttpException e) {
            throw WebDAVFileStorageExceptionCodes.HTTP_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    public void unlock(final String folderId, final String id) throws FileStorageException {
        final String fid = checkFolderId(folderId);
        final LockTokenKey lockTokenKey = new LockTokenKey(fid, id);
        final String lockToken = lockTokenMap.get(lockTokenKey);
        unlock0(lockTokenKey, lockToken);
        lockTokenMap.remove(lockTokenKey);
    }

    private void unlock0(final LockTokenKey lockTokenKey, final String lockToken) throws FileStorageException {
        try {
            final String folderId = lockTokenKey.getFolderId();
            final String id = lockTokenKey.getId();
            final URI uri = new URI(checkFolderId(folderId) + id, true);
            final UnLockMethod unlockMethod = new UnLockMethod(uri.toString(), lockToken);
            try {
                initMethod(folderId, id, unlockMethod);
                client.executeMethod(unlockMethod);
                /*
                 * Check if request was successfully executed
                 */
                unlockMethod.checkSuccess();
            } finally {
                unlockMethod.releaseConnection();
            }
        } catch (final HttpException e) {
            throw WebDAVFileStorageExceptionCodes.HTTP_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final DavException e) {
            throw WebDAVFileStorageExceptionCodes.DAV_ERROR.create(e, e.getMessage());
        }
    }

    public void lock(final String folderId, final String id, final long diff) throws FileStorageException {
        try {
            final String fid = checkFolderId(folderId);
            final URI uri = new URI(fid + id, true);
            final LockMethod lockMethod =
                new LockMethod(
                    uri.toString(),
                    Scope.EXCLUSIVE,
                    Type.WRITE,
                    null /* accountAccess.getUser() */,
                    DavConstants.INFINITE_TIMEOUT,
                    true);
            try {
                initMethod(fid, id, lockMethod);
                client.executeMethod(lockMethod);
                /*
                 * Check if request was successfully executed
                 */
                lockMethod.checkSuccess();
                /*
                 * Obtain & remember lock token
                 */
                final String lockToken = lockMethod.getLockToken();
                lockTokenMap.put(new LockTokenKey(fid, id), lockToken);
            } finally {
                lockMethod.releaseConnection();
            }
        } catch (final HttpException e) {
            throw WebDAVFileStorageExceptionCodes.HTTP_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final DavException e) {
            throw WebDAVFileStorageExceptionCodes.DAV_ERROR.create(e, e.getMessage());
        }
    }

    public void touch(final String folderId, final String id) throws FileStorageException {
        /*
         * Update last-modified time stamp through a PropPatch on a dummy property
         */
        try {
            final String fid = checkFolderId(folderId);
            final URI uri = new URI(fid + id, true);
            /*
             * Create DAV representation
             */
            final DavPropertySet setProperties = new DavPropertySet();
            setProperties.add(new DefaultDavProperty<String>(
                "dummyProperty",
                String.valueOf(System.currentTimeMillis()),
                WebDAVConstants.OX_NAMESPACE));
            final DavMethod propPatchMethod = new PropPatchMethod(uri.toString(), setProperties, new DavPropertyNameSet());
            try {
                initMethod(fid, id, propPatchMethod);
                client.executeMethod(propPatchMethod);
                /*
                 * Check if request was successfully executed
                 */
                propPatchMethod.checkSuccess();
            } finally {
                propPatchMethod.releaseConnection();
            }
        } catch (final HttpException e) {
            throw WebDAVFileStorageExceptionCodes.HTTP_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final DavException e) {
            throw WebDAVFileStorageExceptionCodes.DAV_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            if (e instanceof FileStorageException) {
                throw (FileStorageException) e;
            }
            if (e instanceof AbstractOXException) {
                throw new FileStorageException((AbstractOXException) e);
            }
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
             * Check
             */
            final URI uri = new URI(folderId, true);
            final List<File> files;
            final DavMethod propFindMethod = new PropFindMethod(folderId, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
            try {
                initMethod(folderId, null, propFindMethod);
                client.executeMethod(propFindMethod);
                /*
                 * Check if request was successfully executed
                 */
                propFindMethod.checkSuccess();
                /*
                 * Get MultiStatus response
                 */
                final MultiStatus multiStatus = propFindMethod.getResponseBodyAsMultiStatus();
                /*
                 * Find MultiStatus for specified folder URI
                 */
                final URI tmp = new URI(folderId, true);
                final MultiStatusResponse[] multiStatusResponses = multiStatus.getResponses();
                files = new ArrayList<File>(multiStatusResponses.length);
                for (final MultiStatusResponse multiStatusResponse : multiStatusResponses) {
                    final String href = multiStatusResponse.getHref();
                    tmp.setEscapedPath(href);
                    if (uri.equals(tmp)) {
                        /*
                         * Set for 200 (OK) status
                         */
                        final DavPropertySet propertySet = multiStatusResponse.getProperties(HttpServletResponse.SC_OK);
                        if (null == propertySet || propertySet.isEmpty()) {
                            throw FileStorageExceptionCodes.FOLDER_NOT_FOUND.create(
                                folderId,
                                account.getId(),
                                WebDAVConstants.ID,
                                Integer.valueOf(session.getUserId()),
                                Integer.valueOf(session.getContextId()));
                        }
                        /*
                         * Check for collection
                         */
                        @SuppressWarnings("unchecked") final DavProperty<Element> davProperty =
                            (DavProperty<Element>) propertySet.get(DavConstants.PROPERTY_RESOURCETYPE);
                        final Element resourceType = davProperty.getValue();
                        if (null == resourceType || !"collection".equalsIgnoreCase(resourceType.getLocalName())) {
                            /*
                             * Not a collection
                             */
                            throw WebDAVFileStorageExceptionCodes.NOT_A_FOLDER.create(folderId);
                        }
                    } else {
                        /*
                         * Set for 200 (OK) status
                         */
                        final DavPropertySet propertySet = multiStatusResponse.getProperties(HttpServletResponse.SC_OK);
                        if (null != propertySet && !propertySet.isEmpty()) {
                            /*
                             * Check for collection
                             */
                            @SuppressWarnings("unchecked") final DavProperty<Element> davProperty =
                                (DavProperty<Element>) propertySet.get(DavConstants.PROPERTY_RESOURCETYPE);
                            final Element resourceType = davProperty.getValue();
                            if (null == resourceType || !"collection".equalsIgnoreCase(resourceType.getLocalName())) {
                                /*
                                 * File
                                 */
                                files.add(new WebDAVFileStorageFile(folderId, extractFileName(href), session.getUserId()).parseDavPropertySet(propertySet, fields));
                            }
                        }
                    }
                }
            } finally {
                propFindMethod.releaseConnection();
            }
            return files;
        } catch (final FileStorageException e) {
            throw e;
        } catch (final HttpException e) {
            throw WebDAVFileStorageExceptionCodes.HTTP_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final DavException e) {
            throw WebDAVFileStorageExceptionCodes.DAV_ERROR.create(e, e.getMessage());
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

    public TimedResult<File> getVersions(final String folder, final String id, final List<Field> fields) throws FileStorageException {
        return new FileTimedResult(Collections.singletonList(getFileMetadata(folder, id, CURRENT_VERSION)));
    }

    public TimedResult<File> getVersions(final String folderId, final String id, final List<Field> fields, final Field sort, final SortDirection order) throws FileStorageException {
        return new FileTimedResult(Collections.singletonList(getFileMetadata(folderId, id, CURRENT_VERSION)));
    }

    public TimedResult<File> getDocuments(final List<IDTuple> ids, final List<Field> fields) throws FileStorageException {
        final List<File> list = new ArrayList<File>(ids.size());
        for (final IDTuple idTuple : ids) {
            list.add(getFileMetadata(idTuple.getFolder(), idTuple.getId(), CURRENT_VERSION));
        }
        return  new FileTimedResult(list);
    }

    public Delta<File> getDelta(final String folderId, final long updateSince, final List<Field> fields, final boolean ignoreDeleted) throws FileStorageException {
        // either all or none
        return null;
    }

    public Delta<File> getDelta(final String folderId, final long updateSince, final List<Field> fields, final Field sort, final SortDirection order, final boolean ignoreDeleted) throws FileStorageException {
        // either all or none
        return null;
    }

    public SearchIterator<File> search(final String pattern, final List<Field> fields, final String folderId, final Field sort, final SortDirection order, final int start, final int end) throws FileStorageException {
        final List<File> results;
        if (ALL_FOLDERS.equals(folderId)) {
            /*
             * Recursively search files in directories
             */
            results = new ArrayList<File>();
            recursiveSearchFile(pattern, rootUri, fields, results);
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
            return SearchIteratorAdapter.createEmptyIterator();
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
                return SearchIteratorAdapter.createEmptyIterator();
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
            final URI uri = new URI(folderId, true);
            final DavMethod propFindMethod = new PropFindMethod(folderId, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
            try {
                initMethod(folderId, null, propFindMethod);
                client.executeMethod(propFindMethod);
                /*
                 * Check if request was successfully executed
                 */
                propFindMethod.checkSuccess();
                /*
                 * Get MultiStatus response
                 */
                final MultiStatus multiStatus = propFindMethod.getResponseBodyAsMultiStatus();
                /*
                 * Find MultiStatus for specified folder URI
                 */
                final URI tmp = new URI(folderId, true);
                final MultiStatusResponse[] multiStatusResponses = multiStatus.getResponses();
                for (final MultiStatusResponse multiStatusResponse : multiStatusResponses) {
                    final String href = multiStatusResponse.getHref();
                    tmp.setEscapedPath(href);
                    if (uri.equals(tmp)) {
                        /*
                         * Set for 200 (OK) status
                         */
                        final DavPropertySet propertySet = multiStatusResponse.getProperties(HttpServletResponse.SC_OK);
                        if (null == propertySet || propertySet.isEmpty()) {
                            throw FileStorageExceptionCodes.FOLDER_NOT_FOUND.create(
                                folderId,
                                account.getId(),
                                WebDAVConstants.ID,
                                Integer.valueOf(session.getUserId()),
                                Integer.valueOf(session.getContextId()));
                        }
                        /*
                         * Check for collection
                         */
                        @SuppressWarnings("unchecked") final DavProperty<Element> davProperty =
                            (DavProperty<Element>) propertySet.get(DavConstants.PROPERTY_RESOURCETYPE);
                        final Element resourceType = davProperty.getValue();
                        if (null == resourceType || !"collection".equalsIgnoreCase(resourceType.getLocalName())) {
                            /*
                             * Not a collection
                             */
                            throw WebDAVFileStorageExceptionCodes.NOT_A_FOLDER.create(folderId);
                        }
                    } else {
                        /*
                         * Set for 200 (OK) status
                         */
                        final DavPropertySet propertySet = multiStatusResponse.getProperties(HttpServletResponse.SC_OK);
                        if (null != propertySet && !propertySet.isEmpty()) {
                            /*
                             * Check for collection
                             */
                            @SuppressWarnings("unchecked") final DavProperty<Element> davProperty =
                                (DavProperty<Element>) propertySet.get(DavConstants.PROPERTY_RESOURCETYPE);
                            final Element resourceType = davProperty.getValue();
                            if (null != resourceType && "collection".equalsIgnoreCase(resourceType.getLocalName())) {
                                /*
                                 * A directory
                                 */
                                recursiveSearchFile(pattern, tmp.toString(), fields, results);
                            } else {
                                /*
                                 * File
                                 */
                                final WebDAVFileStorageFile davFile = new WebDAVFileStorageFile(folderId, extractFileName(href), session.getUserId()).parseDavPropertySet(propertySet, fields);
                                if (davFile.matches(pattern)) {
                                    results.add(davFile);
                                }
                            }
                        }
                    }
                }
            } finally {
                propFindMethod.releaseConnection();
            }
        } catch (final FileStorageException e) {
            throw e;
        } catch (final HttpException e) {
            throw WebDAVFileStorageExceptionCodes.HTTP_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final DavException e) {
            throw WebDAVFileStorageExceptionCodes.DAV_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    public FileStorageAccountAccess getAccountAccess() {
        return accountAccess;
    }

    private static String extractFileName(final String href) {
        final int pos = href.lastIndexOf('/');
        return pos > 0 ? href.substring(pos + 1) : href;
    }

}
