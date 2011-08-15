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

package com.openexchange.mail.mime.dataobjects;

import static com.openexchange.mail.utils.CharsetDetector.detectCharset;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.DefaultFile;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.FileStorageFileAccess;
import com.openexchange.file.storage.composition.IDBasedFileAccess;
import com.openexchange.file.storage.composition.IDBasedFileAccessFactory;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MIMEType2ExtMap;
import com.openexchange.mail.mime.MIMETypes;
import com.openexchange.mail.mime.datasource.MessageDataSource;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.stream.UnsynchronizedByteArrayInputStream;

/**
 * {@link MIMEFileStoreMailPart} - A {@link MailPart} implementation that keeps a reference to a temporary uploaded file that shall be added
 * as an attachment later
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class MIMEFileStoreMailPart extends MailPart {

    private static final long serialVersionUID = 257902073011243269L;

    private static final transient org.apache.commons.logging.Log LOG =
        com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(MIMEFileStoreMailPart.class));

    private final IDBasedFileAccessFactory fileAccessFactory;

    private final int userId;

    private final int contextId;

    private final String id;

    private transient MessageDataSource dataSource;

    private transient Object cachedContent;

    /**
     * Initializes a new {@link MIMEFileStoreMailPart}
     * 
     * @param fileDataSource The file data source
     * @throws OXException If upload file's content type cannot be parsed
     */
    protected MIMEFileStoreMailPart(final DataSource dataSource, final Session session) throws OXException {
        super();
        try {
            final File file = new DefaultFile();
            file.setId(FileStorageFileAccess.NEW);
            file.setFolderId(String.valueOf(new OXFolderAccess(getContextFrom(session)).getDefaultFolder(
                session.getUserId(),
                FolderObject.INFOSTORE)));
            final String name = dataSource.getName();
            setContentType(prepareContentType(dataSource.getContentType(), name));

            if (getContentType().getCharsetParameter() == null && getContentType().startsWith(TEXT)) {
                /*
                 * Guess charset for textual attachment
                 */
                final String cs = detectCharset(dataSource.getInputStream());
                getContentType().setCharsetParameter(cs);
            }

            file.setFileName(name);
            file.setFileMIMEType(getContentType().toString());
            file.setTitle(name);
            /*
             * Put attachment's document to dedicated infostore folder
             */
            fileAccessFactory = ServerServiceRegistry.getInstance().getService(IDBasedFileAccessFactory.class, true);
            final IDBasedFileAccess fileAccess = fileAccessFactory.createAccess(session);
            boolean retry = true;
            int count = 1;
            final StringBuilder hlp = new StringBuilder(16);
            while (retry) {
                /*
                 * Get attachment's input stream
                 */
                final InputStream in = dataSource.getInputStream();
                try {
                    /*
                     * Start InfoStore transaction
                     */
                    fileAccess.startTransaction();
                    try {
                        fileAccess.saveDocument(file, in, FileStorageFileAccess.DISTANT_FUTURE);
                        fileAccess.commit();
                        retry = false;
                    } catch (final OXException x) {
                        fileAccess.rollback();
                        if (!x.isPrefix("IFO")) {
                            throw x;
                        }
                        if (441 != x.getCode()) {
                            throw new OXException(x);
                        }
                        /*
                         * Duplicate document name, thus retry with a new name
                         */
                        hlp.setLength(0);
                        final int pos = name.lastIndexOf('.');
                        final String newName;
                        if (pos >= 0) {
                            newName =
                                hlp.append(name.substring(0, pos)).append("_(").append(++count).append(')').append(name.substring(pos)).toString();
                        } else {
                            newName = hlp.append(name).append("_(").append(++count).append(')').toString();
                        }
                        file.setFileName(newName);
                        file.setTitle(newName);
                    } catch (final RuntimeException e) {
                        fileAccess.rollback();
                        throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
                    } finally {
                        fileAccess.finish();
                    }
                } finally {
                    try {
                        in.close();
                    } catch (final IOException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
            id = String.valueOf(file.getId());
            userId = session.getUserId();
            contextId = session.getContextId();
        } catch (final IOException e) {
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        }
    }

    private static Context getContextFrom(final Session session) throws OXException {
        if (session instanceof ServerSession) {
            return ((ServerSession) session).getContext();
        }
        return ContextStorage.getStorageContext(session.getContextId());
    }

    private Session getSession() throws OXException {
        final Collection<Session> sessions =
            ServerServiceRegistry.getInstance().getService(SessiondService.class, true).getSessions(userId, contextId);
        if (null == sessions || sessions.isEmpty()) {
            throw MailExceptionCode.MISSING_FIELD.create("session");
        }
        return sessions.iterator().next();
    }

    private static String prepareContentType(final String contentType, final String preparedFileName) {
        if (null == contentType || contentType.length() == 0) {
            return MIMETypes.MIME_APPL_OCTET;
        }
        final String retval;
        {
            final int mlen = contentType.length() - 1;
            if (0 == contentType.indexOf('"') && mlen == contentType.lastIndexOf('"')) {
                retval = contentType.substring(1, mlen);
            } else {
                retval = contentType;
            }
        }
        if ("multipart/form-data".equalsIgnoreCase(retval)) {
            return MIMEType2ExtMap.getContentType(preparedFileName);

        }
        return contentType;
    }

    private static final String TEXT = "text/";

    private MessageDataSource getDataSource() throws OXException {
        /*
         * Lazy creation
         */
        if (null == dataSource) {
            try {
                final ContentType contentType = getContentType();
                final MessageDataSource mds =
                    new MessageDataSource(fileAccessFactory.createAccess(getSession()).getDocument(
                        id,
                        FileStorageFileAccess.CURRENT_VERSION), contentType);
                if (contentType.getCharsetParameter() == null && contentType.startsWith(TEXT)) {
                    /*
                     * Guess charset for textual attachment
                     */
                    final String cs = detectCharset(new UnsynchronizedByteArrayInputStream(mds.getData()));
                    contentType.setCharsetParameter(cs);
                    mds.setContentType(contentType.toString());
                }
                dataSource = mds;
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
                dataSource = new MessageDataSource(new byte[0], MIMETypes.MIME_APPL_OCTET);
            }
        }
        return dataSource;
    }

    /**
     * Gets the identifier of the file associated with this mail part
     * 
     * @return The identifier of the file associated with this mail part
     */
    public String getFileId() {
        return id;
    }

    @Override
    public Object getContent() throws OXException {
        if (cachedContent != null) {
            return cachedContent;
        }
        if (getContentType().isMimeType(MIMETypes.MIME_TEXT_ALL)) {
            final MessageDataSource mds = getDataSource();
            String charset = getContentType().getCharsetParameter();
            if (charset == null) {
                charset = "UTF-8";
            }
            try {
                cachedContent = new String(mds.getData(), charset);
            } catch (final UnsupportedEncodingException e) {
                throw MailExceptionCode.ENCODING_ERROR.create(e, e.getMessage());
            }
            return cachedContent;
        }
        return null;
    }

    @Override
    public DataHandler getDataHandler() throws OXException {
        return new DataHandler(getDataSource());
    }

    @Override
    public int getEnclosedCount() throws OXException {
        return NO_ENCLOSED_PARTS;
    }

    @Override
    public MailPart getEnclosedMailPart(final int index) throws OXException {
        return null;
    }

    @Override
    public InputStream getInputStream() throws OXException {
        try {
            return getDataSource().getInputStream();
        } catch (final IOException e) {
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public void loadContent() {
        // Nothing to do
    }

    @Override
    public void prepareForCaching() {
        // Nothing to do
    }

}
