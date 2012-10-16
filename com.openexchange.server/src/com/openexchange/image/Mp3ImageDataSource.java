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

package com.openexchange.image;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.datatype.DataTypes;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.framebody.FrameBodyAPIC;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.conversion.Data;
import com.openexchange.conversion.DataArguments;
import com.openexchange.conversion.DataExceptionCodes;
import com.openexchange.conversion.DataProperties;
import com.openexchange.conversion.SimpleData;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.file.storage.FileStorageFileAccess;
import com.openexchange.file.storage.composition.IDBasedFileAccess;
import com.openexchange.file.storage.composition.IDBasedFileAccessFactory;
import com.openexchange.filemanagement.ManagedFile;
import com.openexchange.filemanagement.ManagedFileManagement;
import com.openexchange.java.Streams;
import com.openexchange.mail.mime.MimeType2ExtMap;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.tools.stream.UnsynchronizedByteArrayInputStream;

/**
 * {@link Mp3ImageDataSource}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class Mp3ImageDataSource implements ImageDataSource {

    private static final Log LOG = com.openexchange.log.Log.loggerFor(Mp3ImageDataSource.class);

    private static final Mp3ImageDataSource INSTANCE = new Mp3ImageDataSource();

    /**
     * Gets the instance.
     * 
     * @return The instance
     */
    public static Mp3ImageDataSource getInstance() {
        return INSTANCE;
    }

    private static final String[] ARGS = { "com.openexchange.file.storage.folder", "com.openexchange.file.storage.id" };

    /**
     * Initializes a new {@link Mp3ImageDataSource}.
     */
    private Mp3ImageDataSource() {
        super();
    }

    @Override
    public <D> Data<D> getData(final Class<? extends D> type, final DataArguments dataArguments, final Session session) throws OXException {
        if (!InputStream.class.equals(type)) {
            throw DataExceptionCodes.TYPE_NOT_SUPPORTED.create(type.getName());
        }
        /*
         * Get arguments
         */
        final String folderId;
        {
            final String val = dataArguments.get(ARGS[0]);
            if (val == null) {
                throw DataExceptionCodes.MISSING_ARGUMENT.create(ARGS[0]);
            }
            folderId = val.toString();
        }
        final String fileId;
        {
            final String val = dataArguments.get(ARGS[1]);
            if (val == null) {
                throw DataExceptionCodes.MISSING_ARGUMENT.create(ARGS[1]);
            }
            fileId = val.toString();
        }
        /*
         * Get MP3 image
         */
        String mimeType = null;
        byte[] imageBytes = null;
        {
            final byte[] mp3Bytes = optData(fileId, folderId, ServerSessionAdapter.valueOf(session));
            final ManagedFileManagement fileManagement = ServerServiceRegistry.getInstance().getService(ManagedFileManagement.class);
            final ManagedFile managedFile = fileManagement.createManagedFile(mp3Bytes);
            try {
                final File tmpFile = managedFile.getFile();
                // Create MP3 file
                final MP3File mp3 = new MP3File(tmpFile);
                // Get appropriate cover tag
                final TagField imageField = mp3.getID3v2Tag().getFirstField(FieldKey.COVER_ART);
                if (imageField instanceof AbstractID3v2Frame) {
                    final FrameBodyAPIC imageFrameBody = (FrameBodyAPIC) ((AbstractID3v2Frame) imageField).getBody();
                    if (!imageFrameBody.isImageUrl()) {
                        imageBytes = (byte[]) imageFrameBody.getObjectValue(DataTypes.OBJ_PICTURE_DATA);
                        mimeType = (String) imageFrameBody.getObjectValue(DataTypes.OBJ_MIME_TYPE);
                    }
                }
            } catch (final IOException e) {
                throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
            } catch (final TagException e) {
                throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
            } catch (final ReadOnlyFileException e) {
                throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
            } catch (final InvalidAudioFrameException e) {
                throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
            } finally {
                managedFile.delete();
            }
        }
        // Return
        final DataProperties properties = new DataProperties();
        if (imageBytes == null) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(new StringBuilder("Requested a non-existing image in contact: object-id=").append(fileId).append(" folder=").append(
                    folderId).append(" context=").append(session.getContextId()).append(" session-user=").append(session.getUserId()).append(
                    "\nReturning an empty image as fallback.").toString());
            }
            properties.put(DataProperties.PROPERTY_CONTENT_TYPE, mimeType);
            properties.put(DataProperties.PROPERTY_SIZE, String.valueOf(0));
            return new SimpleData<D>((D) (new UnsynchronizedByteArrayInputStream(new byte[0])), properties);
        }
        properties.put(DataProperties.PROPERTY_CONTENT_TYPE, mimeType);
        properties.put(DataProperties.PROPERTY_SIZE, String.valueOf(imageBytes.length));
        if (null != mimeType) {
            final List<String> extensions = MimeType2ExtMap.getFileExtensions(mimeType);
            properties.put(DataProperties.PROPERTY_NAME, "image." + extensions.get(0));
        }
        return new SimpleData<D>((D) (new UnsynchronizedByteArrayInputStream(imageBytes)), properties);
    }

    @Override
    public String[] getRequiredArguments() {
        final String[] args = new String[ARGS.length];
        System.arraycopy(ARGS, 0, args, 0, ARGS.length);
        return args;
    }

    @Override
    public Class<?>[] getTypes() {
        return new Class<?>[] { InputStream.class };
    }

    private static final String REGISTRATION_NAME = "com.openexchange.file.storage.mp3Cover";

    @Override
    public String getRegistrationName() {
        return REGISTRATION_NAME;
    }

    private static final String ALIAS = "/file/mp3Cover";

    @Override
    public String getAlias() {
        return ALIAS;
    }

    @Override
    public ImageLocation parseUrl(final String url) {
        return ImageUtility.parseImageLocationFrom(url);
    }

    @Override
    public DataArguments generateDataArgumentsFrom(final ImageLocation imageLocation) {
        final DataArguments dataArguments = new DataArguments(2);
        dataArguments.put(ARGS[0], imageLocation.getFolder());
        dataArguments.put(ARGS[1], imageLocation.getId());
        return dataArguments;
    }

    @Override
    public String generateUrl(final ImageLocation imageLocation, final Session session) throws OXException {
        final StringBuilder sb = new StringBuilder(64);
        ImageUtility.startImageUrl(imageLocation, session, this, true, sb);
        final com.openexchange.file.storage.File file = optFile(imageLocation, ServerSessionAdapter.valueOf(session));
        if (null != file) {
            sb.append('&').append("timestamp=").append(file.getLastModified().getTime());
        }
        return sb.toString();
    }

    @Override
    public long getExpires() {
        return -1L;
    }

    @Override
    public String getETag(final ImageLocation imageLocation, final Session session) throws OXException {
        final char delim = '#';
        final StringBuilder builder = new StringBuilder(128);
        builder.append(delim).append(imageLocation.getFolder());
        final com.openexchange.file.storage.File file = optFile(imageLocation, ServerSessionAdapter.valueOf(session));
        if (null != file) {
            builder.append(delim).append(file.getLastModified().getTime());
        }
        builder.append(delim);
        return ImageUtility.getMD5(builder.toString(), "hex");
    }

    @Override
    public ImageLocation parseRequest(final AJAXRequestData requestData) {
        return ImageUtility.parseImageLocationFrom(requestData);
    }

    private static com.openexchange.file.storage.File optFile(final ImageLocation imageLocation, final ServerSession session) throws OXException {
        return optFile(imageLocation.getId(), imageLocation.getFolder(), session);
    }

    private static com.openexchange.file.storage.File optFile(final String fileId, final String folderId, final ServerSession session) throws OXException {
        if (!session.getUserConfiguration().hasInfostore()) {
            throw FileStorageExceptionCodes.FILE_NOT_FOUND.create(fileId, folderId);
        }
        final ServerServiceRegistry serviceRegistry = ServerServiceRegistry.getInstance();
        final IDBasedFileAccess fileAccess = serviceRegistry.getService(IDBasedFileAccessFactory.class).createAccess(session);
        try {
            final com.openexchange.file.storage.File mp3File = fileAccess.getFileMetadata(fileId, FileStorageFileAccess.CURRENT_VERSION);
            // Check MIME type
            String fileMIMEType = mp3File.getFileMIMEType();
            fileMIMEType = null == fileMIMEType ? null : fileMIMEType.toLowerCase(Locale.ENGLISH);
            if (null != fileMIMEType) {
                if (!isMp3(fileMIMEType)) {
                    throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create("File is not an MP3 file: " + fileMIMEType);
                }
            } else {
                String fileName = mp3File.getFileName();
                fileName = null == fileName ? null : fileName.toLowerCase(Locale.ENGLISH);
                if (null != fileName && !fileName.endsWith(".mp3")) {
                    throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create("File is not an MP3 file: " + fileMIMEType);
                }
            }
            return mp3File;
        } catch (final RuntimeException e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    private static byte[] optData(final String fileId, final String folderId, final ServerSession session) throws OXException {
        if (!session.getUserConfiguration().hasInfostore()) {
            throw FileStorageExceptionCodes.FILE_NOT_FOUND.create(fileId, folderId);
        }
        final ServerServiceRegistry serviceRegistry = ServerServiceRegistry.getInstance();
        final IDBasedFileAccess fileAccess = serviceRegistry.getService(IDBasedFileAccessFactory.class).createAccess(session);
        InputStream mp3File = null;
        try {
            mp3File = fileAccess.getDocument(fileId, FileStorageFileAccess.CURRENT_VERSION);
            final ByteArrayOutputStream outputStream = Streams.newByteArrayOutputStream(8192);
            final byte[] buf = new byte[2048];
            for (int r = mp3File.read(buf, 0, 2048); r > 0; r = mp3File.read(buf, 0, 2048)) {
                outputStream.write(buf, 0, r);
            }
            return outputStream.toByteArray();
        } catch (final IOException e) {
            throw FileStorageExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw FileStorageExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            Streams.close(mp3File);
        }
    }

    private static final Set<String> MIME_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "audio/mpeg",
        "audio/x-mpeg",
        "audio/mp3",
        "audio/x-mp3",
        "audio/mpeg3",
        "audio/x-mpeg3",
        "audio/mpg",
        "audio/x-mpg",
        "audio/x-mpegaudio")));

    private static boolean isMp3(final String mimeType) {
        if (null == mimeType) {
            return false;
        }
        return MIME_TYPES.contains(mimeType);
    }

}
