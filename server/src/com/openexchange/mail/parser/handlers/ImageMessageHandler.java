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

package com.openexchange.mail.parser.handlers;

import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import com.openexchange.mail.MailException;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MIMEDefaultSession;
import com.openexchange.mail.mime.MIMEMailException;
import com.openexchange.mail.mime.MIMETypes;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.converters.MIMEMessageConverter;
import com.openexchange.mail.mime.utils.MIMEMessageUtility;
import com.openexchange.mail.parser.MailMessageHandler;
import com.openexchange.mail.parser.MailMessageParser;
import com.openexchange.mail.uuencode.UUEncodedPart;

/**
 * {@link ImageMessageHandler}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ImageMessageHandler implements MailMessageHandler {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ImageMessageHandler.class);

    private final String cid;

    private MailPart imagePart;

    /**
     * Constructor
     */
    public ImageMessageHandler(final String cid) {
        super();
        if (cid == null || cid.length() == 0) {
            throw new IllegalArgumentException("Image's Content-ID must not be null or empty");
        }
        this.cid = cid;
    }

    /**
     * @return The image part or <code>null</code> if none matching image part has been found
     */
    public MailPart getImagePart() {
        return imagePart;
    }

    private static final String IMAGE = "image/";

    public boolean handleAttachment(final MailPart part, final boolean isInline, final String baseContentType, final String fileName, final String id) throws MailException {
        if (part.getContentType().startsWith(IMAGE) || part.getContentType().startsWith(MIMETypes.MIME_APPL_OCTET)) {
            String cid = part.getContentId();
            if (cid == null || cid.length() == 0) {
                /*
                 * Try to read from headers
                 */
                cid = part.getFirstHeader(MessageHeaders.HDR_CONTENT_ID);
                if (cid == null || cid.length() == 0) {
                    /*
                     * Compare with filename
                     */
                    final String realFilename = MIMEMessageUtility.getRealFilename(part);
                    if (MIMEMessageUtility.equalsCID(this.cid, realFilename)) {
                        imagePart = part;
                        return false;
                    }
                    return true;
                }
            }
            if (MIMEMessageUtility.equalsCID(this.cid, cid)) {
                imagePart = part;
                return false;
            }
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleBccRecipient(javax.mail.internet.InternetAddress[])
     */
    public boolean handleBccRecipient(final InternetAddress[] recipientAddrs) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleCcRecipient(javax.mail.internet.InternetAddress[])
     */
    public boolean handleCcRecipient(final InternetAddress[] recipientAddrs) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleColorLabel(int)
     */
    public boolean handleColorLabel(final int colorLabel) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleContentId(java.lang.String)
     */
    public boolean handleContentId(final String contentId) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleFrom(javax.mail.internet.InternetAddress[])
     */
    public boolean handleFrom(final InternetAddress[] fromAddrs) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleHeaders(int, java.util.Iterator)
     */
    public boolean handleHeaders(final int size, final Iterator<Entry<String, String>> iter) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleImagePart(com.openexchange.mail.dataobjects.MailPart, java.lang.String,
     * java.lang.String, java.lang.String)
     */
    public boolean handleImagePart(final MailPart part, final String imageCID, final String baseContentType, final boolean isInline, final String fileName, final String id) throws MailException {
        if (imageCID == null) {
            /*
             * Compare with filename
             */
            final String realFilename = MIMEMessageUtility.getRealFilename(part);
            if (MIMEMessageUtility.equalsCID(cid, realFilename)) {
                imagePart = part;
                return false;
            }
            return true;
        } else if (MIMEMessageUtility.equalsCID(cid, imageCID)) {
            imagePart = part;
            return false;
        } else {
            /*
             * Compare with filename
             */
            final String realFilename = MIMEMessageUtility.getRealFilename(part);
            if (MIMEMessageUtility.equalsCID(cid, realFilename)) {
                imagePart = part;
                return false;
            }
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleInlineHtml(java.lang.String, com.openexchange.tools.mail.ContentType,
     * long, java.lang.String, java.lang.String)
     */
    public boolean handleInlineHtml(final String htmlContent, final ContentType contentType, final long size, final String fileName, final String id) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleInlinePlainText(java.lang.String, com.openexchange.tools.mail.ContentType,
     * long, java.lang.String, java.lang.String)
     */
    public boolean handleInlinePlainText(final String plainTextContent, final ContentType contentType, final long size, final String fileName, final String id) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleInlineUUEncodedAttachment(com.openexchange.tools.mail.UUEncodedPart,
     * java.lang.String)
     */
    public boolean handleInlineUUEncodedAttachment(final UUEncodedPart part, final String id) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleInlineUUEncodedPlainText(java.lang.String,
     * com.openexchange.tools.mail.ContentType, int, java.lang.String, java.lang.String)
     */
    public boolean handleInlineUUEncodedPlainText(final String decodedTextContent, final ContentType contentType, final int size, final String fileName, final String id) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleMessageEnd(com.openexchange.mail.dataobjects.MailMessage)
     */
    public void handleMessageEnd(final MailMessage mail) throws MailException {
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleMultipart(com.openexchange.mail.dataobjects.MailPart, int,
     * java.lang.String)
     */
    public boolean handleMultipart(final MailPart mp, final int bodyPartCount, final String id) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleNestedMessage(com.openexchange.mail.dataobjects.MailMessage,
     * java.lang.String)
     */
    public boolean handleNestedMessage(final MailPart mailPart, final String id) throws MailException {
        final Object content = mailPart.getContent();
        final MailMessage nestedMail;
        if (content instanceof MailMessage) {
            nestedMail = (MailMessage) content;
        } else if (content instanceof InputStream) {
            try {
                nestedMail = MIMEMessageConverter.convertMessage(new MimeMessage(
                    MIMEDefaultSession.getDefaultSession(),
                    (InputStream) content));
            } catch (final MessagingException e) {
                throw MIMEMailException.handleMessagingException(e);
            }
        } else {
            LOG.error("Ignoring nested message. Cannot handle part's content which should be a RFC822 message according to its content type: " + (null == content ? "null" : content.getClass().getSimpleName()));
            return true;
        }
        final ImageMessageHandler handler = new ImageMessageHandler(cid);
        new MailMessageParser().parseMailMessage(nestedMail, handler, id);
        if (handler.getImagePart() != null) {
            imagePart = handler.getImagePart();
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handlePriority(int)
     */
    public boolean handlePriority(final int priority) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleMsgRef(java.lang.String)
     */
    public boolean handleMsgRef(final String msgRef) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleDispositionNotification(javax.mail.internet.InternetAddress)
     */
    public boolean handleDispositionNotification(final InternetAddress dispositionNotificationTo, final boolean seen) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleReceivedDate(java.util.Date)
     */
    public boolean handleReceivedDate(final Date receivedDate) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleSentDate(java.util.Date)
     */
    public boolean handleSentDate(final Date sentDate) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleSpecialPart(com.openexchange.mail.dataobjects.MailPart, java.lang.String,
     * java.lang.String)
     */
    public boolean handleSpecialPart(final MailPart part, final String baseContentType, final String fileName, final String id) throws MailException {
        return handleAttachment(
            part,
            !Part.ATTACHMENT.equalsIgnoreCase(part.getContentDisposition().getDisposition()),
            baseContentType,
            fileName,
            id);
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleSubject(java.lang.String)
     */
    public boolean handleSubject(final String subject) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleSystemFlags(int)
     */
    public boolean handleSystemFlags(final int flags) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleToRecipient(javax.mail.internet.InternetAddress[])
     */
    public boolean handleToRecipient(final InternetAddress[] recipientAddrs) throws MailException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.parser.MailMessageHandler#handleUserFlags(java.lang.String[])
     */
    public boolean handleUserFlags(final String[] userFlags) throws MailException {
        return true;
    }

}
