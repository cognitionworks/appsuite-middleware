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

package com.openexchange.mail.dataobjects.compose;

import static com.openexchange.mail.utils.MessageUtility.readStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.internet.MimeUtility;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.FileStorageException;
import com.openexchange.file.storage.FileStorageFileAccess;
import com.openexchange.file.storage.composition.IDBasedFileAccess;
import com.openexchange.file.storage.composition.IDBasedFileAccessFactory;
import com.openexchange.mail.MailException;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.mime.MIMETypes;
import com.openexchange.mail.mime.datasource.StreamDataSource;
import com.openexchange.server.ServiceException;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.tx.TransactionException;

/**
 * {@link InfostoreDocumentMailPart} - A {@link MailPart} implementation that provides the input stream to an infostore document
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class InfostoreDocumentMailPart extends MailPart implements ComposedMailPart {

    /**
	 * 
	 */
    private static final long serialVersionUID = -3158021272821196715L;

    private static final transient org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(InfostoreDocumentMailPart.class);

    private transient DataSource dataSource;

    private transient final StreamDataSource.InputStreamProvider inputStreamProvider;

    private transient Object cachedContent;

    /**
     * Constructor
     * 
     * @param documentId The document's unique ID
     * @param session The session providing needed user data
     * @throws MailException If infostore document cannot be read
     */
    public InfostoreDocumentMailPart(final String documentId, final Session session) throws MailException {
        super();
        IDBasedFileAccess fileAccess = null;
        try {
            final IDBasedFileAccessFactory fileAccessFactory = ServerServiceRegistry.getInstance().getService(IDBasedFileAccessFactory.class, true);
            fileAccess = fileAccessFactory.createAccess(session);
            final File fileMetadata = fileAccess.getFileMetadata(documentId, FileStorageFileAccess.CURRENT_VERSION);
            setSize(fileMetadata.getFileSize());
            final String docMIMEType = fileMetadata.getFileMIMEType();
            setContentType(docMIMEType == null || docMIMEType.length() == 0 ? MIMETypes.MIME_APPL_OCTET : fileMetadata.getFileMIMEType());
            try {
                setFileName(MimeUtility.encodeText(fileMetadata.getFileName(), MailProperties.getInstance().getDefaultMimeCharset(), "Q"));
            } catch (final UnsupportedEncodingException e) {
                setFileName(fileMetadata.getFileName());
            }
            final DocumentInputStreamProvider tmp = new DocumentInputStreamProvider(fileAccess, documentId);
            tmp.setName(getFileName());
            inputStreamProvider = tmp;
        } catch (final FileStorageException e) {
            throw new MailException(e);
        } catch (ServiceException e) {
            throw new MailException(e);
        } finally{
            if(fileAccess != null) {
                try {
                    fileAccess.finish();
                } catch (TransactionException e) {
                    // IGNORE
                }
            }
        }
    }

    private DataSource getDataSource() {
        /*
         * Lazy creation
         */
        if (null == dataSource) {
            dataSource = new StreamDataSource(inputStreamProvider, getContentType().toString());
        }
        return dataSource;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.dataobjects.MailPart#getContent()
     */
    @Override
    public Object getContent() throws MailException {
        if (cachedContent != null) {
            return cachedContent;
        }
        if (getContentType().isMimeType("text/*")) {
            String charset = getContentType().getCharsetParameter();
            if (charset == null) {
                charset = "US-ASCII";
            }
            InputStream docInputSream = null;
            try {
                docInputSream = inputStreamProvider.getInputStream();
                cachedContent = readStream(docInputSream, charset);
            } catch (final FileNotFoundException e) {
                throw new MailException(MailException.Code.IO_ERROR, e, e.getMessage());
            } catch (final IOException e) {
                throw new MailException(MailException.Code.IO_ERROR, e, e.getMessage());
            } finally {
                if (docInputSream != null) {
                    try {
                        docInputSream.close();
                    } catch (final IOException e) {
                        LOG.error(e.getMessage(), e);
                    }
                    docInputSream = null;
                }
            }
            return cachedContent;
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.dataobjects.MailPart#getDataHandler()
     */
    @Override
    public DataHandler getDataHandler() throws MailException {
        return new DataHandler(getDataSource());
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.dataobjects.MailPart#getEnclosedCount()
     */
    @Override
    public int getEnclosedCount() throws MailException {
        return NO_ENCLOSED_PARTS;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.dataobjects.MailPart#getEnclosedMailPart(int)
     */
    @Override
    public MailPart getEnclosedMailPart(final int index) throws MailException {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.dataobjects.MailPart#getInputStream()
     */
    @Override
    public InputStream getInputStream() throws MailException {
        try {
            return inputStreamProvider.getInputStream();
        } catch (final IOException e) {
            throw new MailException(MailException.Code.IO_ERROR, e, e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.dataobjects.MailPart#prepareForCaching()
     */
    @Override
    public void prepareForCaching() {
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.dataobjects.MailPart#loadContent()
     */
    @Override
    public void loadContent() {
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.transport.smtp.dataobjects.SMTPMailPart#getType()
     */
    public ComposedPartType getType() {
        return ComposedMailPart.ComposedPartType.DOCUMENT;
    }

    private static final class DocumentInputStreamProvider implements StreamDataSource.InputStreamProvider {

        private final IDBasedFileAccess fileAccess;

        private final String documentId;

        private String name;

        public DocumentInputStreamProvider(final IDBasedFileAccess fileAccess, final String documentId) {
            super();
            this.fileAccess = fileAccess;
            this.documentId = documentId;
        }

        public InputStream getInputStream() throws IOException {
            try {
                return fileAccess.getDocument(documentId, FileStorageFileAccess.CURRENT_VERSION);
            } catch (final FileStorageException e) {
                final IOException io = new IOException("Input stream cannot be retrieved");
                io.initCause(e);
                throw io;
            }
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }
}
