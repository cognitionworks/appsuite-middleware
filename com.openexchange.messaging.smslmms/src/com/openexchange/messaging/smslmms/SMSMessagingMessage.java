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

package com.openexchange.messaging.smslmms;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.exception.OXException;
import com.openexchange.filemanagement.ManagedFile;
import com.openexchange.filemanagement.ManagedFileManagement;
import com.openexchange.messaging.CaptchaParams;
import com.openexchange.messaging.ContentDisposition;
import com.openexchange.messaging.ContentType;
import com.openexchange.messaging.ManagedFileContent;
import com.openexchange.messaging.MessagingContent;
import com.openexchange.messaging.MessagingFolder;
import com.openexchange.messaging.MessagingHeader;
import com.openexchange.messaging.MessagingPart;
import com.openexchange.messaging.ParameterizedMessagingMessage;
import com.openexchange.messaging.StringContent;
import com.openexchange.messaging.StringMessageHeader;
import com.openexchange.messaging.generic.internet.MimeAddressMessagingHeader;
import com.openexchange.messaging.generic.internet.MimeContentDisposition;
import com.openexchange.messaging.generic.internet.MimeContentType;
import com.openexchange.messaging.generic.internet.MimeMessagingBodyPart;
import com.openexchange.messaging.generic.internet.MimeMultipartContent;
import com.openexchange.server.ServiceExceptionCodes;

/**
 * {@link SMSMessagingMessage} - Represents a SMS message.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SMSMessagingMessage implements ParameterizedMessagingMessage {

    private static final long serialVersionUID = 5324611878535898301L;

    private static final AtomicReference<ManagedFileManagement> FILE_MANAGEMENT = new AtomicReference<ManagedFileManagement>();

    /**
     * Sets the tracked file management.
     * 
     * @param fileManagement The file management
     */
    public static void setManagedFileManagement(final ManagedFileManagement fileManagement) {
        FILE_MANAGEMENT.set(fileManagement);
    }

    private static final String MESSAGE_ID = "smsMessage";

    private static final ContentType CONTENT_TYPE;

    private static final ContentDisposition CONTENT_DISPOSITION;

    static {
        final ContentType contentType = new MimeContentType();
        contentType.setPrimaryType("text");
        contentType.setSubType("plain");
        CONTENT_TYPE = contentType;

        final ContentDisposition contentDisposition = new MimeContentDisposition();
        contentDisposition.setDisposition(MessagingPart.INLINE);
        CONTENT_DISPOSITION = contentDisposition;
    }

    private final Map<String, Collection<MessagingHeader>> headers = new HashMap<String, Collection<MessagingHeader>>(16);

    private MessagingContent content;

    private final long size;

    private final Map<String, Object> parameters;

    private CaptchaParams captchaParams;

    /**
     * Initializes a new {@link SMSMessagingMessage}.
     * 
     * @param recipient The recipient of the direct message
     * @param from The sending user
     */
    public SMSMessagingMessage(final String sender, final String receiver, final String message) {
        super();
        parameters = new HashMap<String, Object>(4);
        /*
         * Assign string content and size
         */
        content = new StringContent(message);
        size = message.length();
        /*
         * Assign headers
         */
        headers.put(CONTENT_TYPE.getName(), wrap(CONTENT_TYPE));
        headers.put(CONTENT_DISPOSITION.getName(), wrap(CONTENT_DISPOSITION));
        {
            final String name = MessagingHeader.KnownHeader.FROM.toString();
            headers.put(name, wrap(MimeAddressMessagingHeader.valueOfPlain(name, null, sender)));
        }
        {
            final String name = MessagingHeader.KnownHeader.TO.toString();
            headers.put(name, wrap(MimeAddressMessagingHeader.valueOfPlain(name, null, receiver)));
        }
        {
            final String name = MessagingHeader.KnownHeader.SUBJECT.toString();
            headers.put(name, getSimpleHeader(name, message));
        }
        {
            final String name = MessagingHeader.KnownHeader.MESSAGE_TYPE.toString();
            headers.put(name, getSimpleHeader(name, MESSAGE_ID));
        }
    }

    @Override
    public int getColorLabel() {
        return 0;
    }

    @Override
    public int getFlags() {
        return 0;
    }

    @Override
    public String getFolder() {
        return MessagingFolder.ROOT_FULLNAME;
    }

    @Override
    public long getReceivedDate() {
        return -1L;
    }

    @Override
    public Collection<String> getUserFlags() {
        return Collections.emptyList();
    }

    @Override
    public MessagingContent getContent() {
        return content;
    }

    @Override
    public String getDisposition() {
        return MessagingPart.INLINE;
    }

    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public MessagingHeader getFirstHeader(final String name) {
        final Collection<MessagingHeader> collection = getHeader(name);
        return null == collection ? null : (collection.isEmpty() ? null : collection.iterator().next());
    }

    @Override
    public Collection<MessagingHeader> getHeader(final String name) {
        return headers.get(name);
    }

    @Override
    public Map<String, Collection<MessagingHeader>> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    @Override
    public String getSectionId() {
        return null;
    }

    @Override
    public void writeTo(final OutputStream os) {
        // Nothing to do.
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public int getThreadLevel() {
        return 0;
    }

    @Override
    public ContentType getContentType() {
        return CONTENT_TYPE;
    }

    private static Collection<MessagingHeader> wrap(final MessagingHeader... headers) {
        return Collections.unmodifiableCollection(Arrays.asList(headers));
    }

    private static Collection<MessagingHeader> getSimpleHeader(final String name, final String value) {
        return wrap(new StringMessageHeader(name, value));
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getPicture() {
        return null;
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public Object getParameter(final String name) {
        return parameters.get(name);
    }

    @Override
    public void putParameter(final String name, final Object value) {
        parameters.put(name, value);
    }

    @Override
    public boolean putParameterIfAbsent(final String name, final Object value) {
        if (parameters.containsKey(name)) {
            return false;
        }
        parameters.put(name, value);
        return true;
    }

    @Override
    public void clearParameters() {
        parameters.clear();
    }

    @Override
    public boolean containsParameter(final String name) {
        return parameters.containsKey(name);
    }

    /**
     * Sets the captcha parameters
     * 
     * @param params The captcha parameters
     */
    public void setCaptchaParameters(final CaptchaParams captchaParams) {
        this.captchaParams = captchaParams;
        parameters.put(PARAM_CAPTCHA_PARAMS, captchaParams);
    }

    /**
     * Gets the captcha parameters
     * 
     * @return The captcha parameters
     */
    public CaptchaParams getCaptchaParams() {
        if (null == captchaParams) {
            captchaParams = (CaptchaParams) parameters.get(PARAM_CAPTCHA_PARAMS);
        }
        return captchaParams;
    }

    /**
     * Adds specified attachment identifier.
     * 
     * @param attachmentId The attachment identifier
     * @throws OXException If attaching denoted file fails
     */
    public void addAttachment(final String attachmentId) throws OXException {
        /*
         * Ensure presence of needed service
         */
        final ManagedFileManagement managedFileManagement = FILE_MANAGEMENT.get();
        if (null == managedFileManagement) {
            throw ServiceExceptionCodes.SERVICE_UNAVAILABLE.create(ManagedFileManagement.class.getName());
        }
        /*
         * Check current message's content
         */
        final MessagingContent content = this.content;
        final MimeMultipartContent mimeMultipartContent;
        if (content instanceof MimeMultipartContent) {
            mimeMultipartContent = (MimeMultipartContent) content;
        } else {
            mimeMultipartContent = new MimeMultipartContent("mixed");
            final MimeMessagingBodyPart bodyPart = new MimeMessagingBodyPart(mimeMultipartContent);
            final MessagingHeader contentType = CONTENT_TYPE;
            bodyPart.setContent(content, contentType.getValue());
            bodyPart.setHeader(contentType);
            bodyPart.setHeader(CONTENT_DISPOSITION);
            mimeMultipartContent.addBodyPart(bodyPart);
            this.content = mimeMultipartContent;
            /*
             * Fix message headers
             */
            headers.remove(CONTENT_TYPE.getName());
            headers.remove(CONTENT_DISPOSITION.getName());
            headers.put(CONTENT_TYPE.getName(), wrap(new MimeContentType(mimeMultipartContent.getContentType())));
        }
        /*
         * Create an appropriate body part for referenced file
         */
        final MimeMessagingBodyPart bodyPart = new MimeMessagingBodyPart(mimeMultipartContent);
        final ManagedFile managedFile = managedFileManagement.getByID(attachmentId);
        final MessagingContent attachmentContent = new ManagedFileContentImpl(managedFile);
        bodyPart.setContent(attachmentContent, managedFile.getContentType());
        bodyPart.setDisposition(managedFile.getContentDisposition());
        bodyPart.setFileName(managedFile.getFileName());
        mimeMultipartContent.addBodyPart(bodyPart);
    }

    private static final class ManagedFileContentImpl implements ManagedFileContent {

        private final ManagedFile managedFile;

        /**
         * Initializes a new {@link ManagedFileContentImplementation}.
         * 
         * @param managedFile
         */
        public ManagedFileContentImpl(final ManagedFile managedFile) {
            super();
            this.managedFile = managedFile;
        }

        @Override
        public InputStream getData() throws OXException {
            return managedFile.getInputStream();
        }

        @Override
        public String getFileName() {
            return managedFile.getFileName();
        }

        @Override
        public String getContentType() {
            return managedFile.getContentType();
        }
    }

}
