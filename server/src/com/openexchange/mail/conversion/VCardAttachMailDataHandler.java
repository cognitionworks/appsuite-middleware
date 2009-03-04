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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.mail.conversion;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.conversion.Data;
import com.openexchange.conversion.DataArguments;
import com.openexchange.conversion.DataException;
import com.openexchange.conversion.DataExceptionCodes;
import com.openexchange.conversion.DataHandler;
import com.openexchange.conversion.DataProperties;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.i18n.MailStrings;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.upload.impl.AJAXUploadFile;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.mail.MailException;
import com.openexchange.mail.MailJSONField;
import com.openexchange.mail.MailListField;
import com.openexchange.mail.json.writer.MessageWriter;
import com.openexchange.mail.mime.ContentDisposition;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MIMEDefaultSession;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.converters.MIMEMessageConverter;
import com.openexchange.mail.mime.datasource.MessageDataSource;
import com.openexchange.mail.mime.utils.MIMEMessageUtility;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.mail.utils.DisplayMode;
import com.openexchange.session.Session;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;

/**
 * {@link VCardAttachMailDataHandler}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class VCardAttachMailDataHandler implements DataHandler {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(VCardAttachMailDataHandler.class);

    private static final String[] ARGS = {};

    private static final Class<?>[] TYPES = { InputStream.class, byte[].class };

    /**
     * Initializes a new {@link VCardAttachMailDataHandler}
     */
    public VCardAttachMailDataHandler() {
        super();
    }

    public String[] getRequiredArguments() {
        return ARGS;
    }

    public Class<?>[] getTypes() {
        return TYPES;
    }

    public Object processData(final Data<? extends Object> data, final DataArguments dataArguments, final Session session) throws DataException {
        final Context ctx;
        final UserSettingMail usm;
        try {
            ctx = ContextStorage.getStorageContext(session);
            usm = UserSettingMailStorage.getInstance().getUserSettingMail(session.getUserId(), ctx);
        } catch (final ContextException e) {
            throw new DataException(e);
        }
        try {
            /*
             * Temporary store VCard as a file for later transport
             */
            final File tmpFile = File.createTempFile("openexchange", null, new File(
                ServerConfig.getProperty(ServerConfig.Property.UploadDirectory)));
            tmpFile.deleteOnExit();
            final DataProperties vcardProperties = data.getDataProperties();
            final byte[] vcardBytes = getBytesFromVCard(data.getData());
            /*
             * Write bytes to file
             */
            write2File(tmpFile, vcardBytes);
            final AJAXUploadFile uploadFile = new AJAXUploadFile(tmpFile, System.currentTimeMillis());
            String fileName = vcardProperties.get(DataProperties.PROPERTY_NAME);
            if (fileName == null) {
                fileName = "vcard.vcf";
            } else {
                fileName = MimeUtility.encodeText(fileName, "UTF-8", "Q");
            }
            uploadFile.setFileName(fileName);
            /*
             * Compose content-type
             */
            final ContentType ct = new ContentType(vcardProperties.get(DataProperties.PROPERTY_CONTENT_TYPE));
            ct.setCharsetParameter(vcardProperties.get(DataProperties.PROPERTY_CHARSET));
            uploadFile.setContentType(ct.toString());
            uploadFile.setSize(vcardBytes.length);
            final String fileId = UUID.randomUUID().toString();
            session.putUploadedFile(fileId, uploadFile);
            /*
             * Compose a new mail
             */
            final MimeMessage mimeMessage = new MimeMessage(MIMEDefaultSession.getDefaultSession());
            /*
             * Set default subject
             */
            mimeMessage.setSubject(new StringHelper(UserStorage.getStorageUser(session.getUserId(), ctx).getLocale()).getString(MailStrings.DEFAULT_SUBJECT));
            /*
             * Set from
             */
            if (usm.getSendAddr() != null) {
                mimeMessage.setFrom(new InternetAddress(usm.getSendAddr(), true));
            }
            /*
             * Create multipart and its nested parts
             */
            final MimeMultipart mimeMultipart = new MimeMultipart("mixed");
            /*
             * Append empty text part
             */
            {
                final MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText("", "text/html; charset=UTF-8", "html");
                textPart.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
                textPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, "text/html; charset=UTF-8");
                mimeMultipart.addBodyPart(textPart);
            }
            /*
             * Append VCard data
             */
            {
                final MimeBodyPart vcardPart = new MimeBodyPart();
                /*
                 * Set appropriate JAF-DataHandler in VCard part
                 */
                vcardPart.setDataHandler(new javax.activation.DataHandler(new MessageDataSource(vcardBytes, ct.toString())));
                if (fileName != null) {
                    final ContentDisposition cd = new ContentDisposition(Part.ATTACHMENT);
                    cd.setFilenameParameter(fileName);
                    vcardPart.setHeader(MessageHeaders.HDR_CONTENT_DISPOSITION, MIMEMessageUtility.fold(21, cd.toString()));
                }
                vcardPart.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
                if (fileName != null && !ct.containsNameParameter()) {
                    ct.setNameParameter(fileName);
                }
                vcardPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMEMessageUtility.fold(14, ct.toString()));
                mimeMultipart.addBodyPart(vcardPart);
            }
            mimeMessage.setContent(mimeMultipart);
            mimeMessage.saveChanges();
            /*
             * Return mail's JSON object
             */
            final JSONObject mailObject = MessageWriter.writeMailMessage(
                MIMEMessageConverter.convertMessage(mimeMessage),
                DisplayMode.MODIFYABLE,
                session,
                null);
            addFileInformation(mailObject, fileId);
            return mailObject;
        } catch (final MailException e) {
            throw new DataException(e);
        } catch (final MessagingException e) {
            throw DataExceptionCodes.ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw DataExceptionCodes.ERROR.create(e, e.getMessage());
        } catch (final JSONException e) {
            throw DataExceptionCodes.ERROR.create(e, e.getMessage());
        }
    }

    private static final String FILE_PREFIX = "file://";

    private static void addFileInformation(final JSONObject mailObject, final String fileId) throws JSONException, DataException {
        if (!mailObject.has(MailJSONField.ATTACHMENTS.getKey()) || mailObject.isNull(MailJSONField.ATTACHMENTS.getKey())) {
            throw DataExceptionCodes.ERROR.create(new StringBuilder(64).append("Parsed JSON mail object does not contain field '").append(
                MailJSONField.ATTACHMENTS.getKey()).append('\'').toString());
        }
        final JSONArray attachmentArray = mailObject.getJSONArray(MailJSONField.ATTACHMENTS.getKey());
        final int len = attachmentArray.length();
        if (len != 2) {
            throw DataExceptionCodes.ERROR.create("Number of attachments in parsed JSON mail object is not equal to 2");
        }
        final JSONObject vcardAttachmentObject = attachmentArray.getJSONObject(1);
        vcardAttachmentObject.remove(MailListField.ID.getKey());
        vcardAttachmentObject.put(
            MailListField.ID.getKey(),
            new StringBuilder(FILE_PREFIX.length() + fileId.length()).append(FILE_PREFIX).append(fileId).toString());
    }

    private static void write2File(final File tmpFile, final byte[] vcardBytes) throws FileNotFoundException, IOException {
        final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tmpFile));
        try {
            bos.write(vcardBytes);
            bos.flush();
        } finally {
            bos.close();
        }
    }

    private static byte[] getBytesFromVCard(final Object vcard) throws DataException {
        try {
            final ByteArrayOutputStream bout = new UnsynchronizedByteArrayOutputStream(1024);
            if (vcard instanceof InputStream) {
                final InputStream in = (InputStream) vcard;
                final byte[] buf = new byte[1024];
                int len = -1;
                while ((len = in.read(buf)) != -1) {
                    bout.write(buf, 0, len);
                }
            } else if (vcard instanceof byte[]) {
                bout.write((byte[]) vcard);
            } else {
                throw DataExceptionCodes.TYPE_NOT_SUPPORTED.create(vcard.getClass().getName());
            }
            return bout.toByteArray();
        } catch (final IOException e) {
            throw DataExceptionCodes.ERROR.create(e, e.getMessage());
        }
    }

}
