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

package com.openexchange.mail.parser;

import static com.openexchange.mail.MailServletInterface.mailInterfaceMonitor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import net.fortuna.ical4j.model.Property;
import net.freeutils.tnef.Attachment;
import net.freeutils.tnef.Attr;
import net.freeutils.tnef.MAPIProp;
import net.freeutils.tnef.MAPIProps;
import net.freeutils.tnef.RawInputStream;
import net.freeutils.tnef.TNEFInputStream;
import net.freeutils.tnef.TNEFUtils;
import net.freeutils.tnef.mime.ContactHandler;
import net.freeutils.tnef.mime.RawDataSource;
import net.freeutils.tnef.mime.ReadReceiptHandler;
import net.freeutils.tnef.mime.TNEFMime;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.i18n.LocaleTools;
import com.openexchange.mail.MailException;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.mime.ContentDisposition;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MIMEDefaultSession;
import com.openexchange.mail.mime.MIMEType2ExtMap;
import com.openexchange.mail.mime.MIMETypes;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.TNEFBodyPart;
import com.openexchange.mail.mime.converters.MIMEMessageConverter;
import com.openexchange.mail.mime.dataobjects.MIMERawSource;
import com.openexchange.mail.mime.datasource.MessageDataSource;
import com.openexchange.mail.mime.utils.MIMEMessageUtility;
import com.openexchange.mail.utils.CharsetDetector;
import com.openexchange.mail.utils.MessageUtility;
import com.openexchange.mail.uuencode.UUEncodedMultiPart;
import com.openexchange.tools.stream.UnsynchronizedByteArrayInputStream;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;
import com.openexchange.tools.tnef.TNEF2ICal;

/**
 * {@link MailMessageParser} - A callback parser to parse instances of {@link MailMessage} by invoking the <code>handleXXX()</code> methods
 * of given {@link MailMessageHandler} object
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MailMessageParser {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(MailMessageParser.class);

    private static final boolean WARN_ENABLED = LOG.isWarnEnabled();

    private static final int BUF_SIZE = 8192;

    private static final Iterator<Entry<String, String>> EMPTY_ITER = new Iterator<Entry<String, String>>() {

        public boolean hasNext() {
            return false;
        }

        public Entry<String, String> next() {
            throw new NoSuchElementException("Iterator is empty");
        }

        public void remove() {
            // Nothing to do
        }
    };

    private static interface InlineDetector {

        public boolean isInline(String disposition, String fileName);
    }

    /**
     * If disposition equals ignore-case <code>"INLINE"</code>, then it is treated as inline in any case.<br>
     * Only if disposition is <code>null</code> the file name is examined.
     */
    private static final InlineDetector LENIENT_DETECTOR = new InlineDetector() {

        public boolean isInline(final String disposition, final String fileName) {
            return Part.INLINE.equalsIgnoreCase(disposition) || ((disposition == null) && (fileName == null));
        }
    };

    /**
     * Considered as inline if disposition equals ignore-case <code>"INLINE"</code> OR is <code>null</code>, but in any case the file name
     * must be <code>null</code>.
     */
    private static final InlineDetector STRICT_DETECTOR = new InlineDetector() {

        public boolean isInline(final String disposition, final String fileName) {
            return (Part.INLINE.equalsIgnoreCase(disposition) || (disposition == null)) && (fileName == null);
        }
    };

    /*
     * +++++++++++++++++++ TNEF CONSTANTS +++++++++++++++++++
     */
    private static final String TNEF_IPM_CONTACT = "IPM.Contact";

    private static final String TNEF_IPM_MS_READ_RECEIPT = "IPM.Microsoft Mail.Read Receipt";

    // private static final String TNEF_IPM_MS_SCHEDULE_CANCELED = "IPM.Microsoft Schedule.MtgCncl";

    // private static final String TNEF_IPM_MS_SCHEDULE_REQUEST = "IPM.Microsoft Schedule.MtgReq";

    // private static final String TNEF_IPM_MS_SCHEDULE_ACCEPTED = "IPM.Microsoft Schedule.MtgRespP";

    // private static final String TNEF_IPM_MS_SCHEDULE_DECLINED = "IPM.Microsoft Schedule.MtgRespN";

    // private static final String TNEF_IPM_MS_SCHEDULE_TENTATIVE = "IPM.Microsoft Schedule.MtgRespA";

    /*
     * +++++++++++++++++++ MEMBERS +++++++++++++++++++
     */

    private boolean stop;

    private boolean multipartDetected;

    private InlineDetector inlineDetector;

    private final List<AbstractOXException> warnings;

    /**
     * Constructor
     */
    public MailMessageParser() {
        super();
        inlineDetector = LENIENT_DETECTOR;
        warnings = new ArrayList<AbstractOXException>(2);
    }

    /**
     * Switches the INLINE detector behavior.
     * 
     * @param strict <code>true</code> to perform strict INLINE detector behavior; otherwise <code>false</code>
     * @return This parser with new behavior applied
     */
    public MailMessageParser setInlineDetectorBehavior(final boolean strict) {
        inlineDetector = strict ? STRICT_DETECTOR : LENIENT_DETECTOR;
        return this;
    }

    /**
     * Gets possible warnings occurred during parsing.
     * 
     * @return The warnings
     */
    public List<AbstractOXException> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /**
     * Resets this parser and returns itself
     * 
     * @return The parser itself
     */
    public MailMessageParser reset() {
        stop = false;
        multipartDetected = false;
        return this;
    }

    /**
     * Parses specified mail using given handler as call-back
     * 
     * @param mail The mail to parse
     * @param handler The call-back handler
     * @throws MailException If parsing specified mail fails
     */
    public void parseMailMessage(final MailMessage mail, final MailMessageHandler handler) throws MailException {
        parseMailMessage(mail, handler, null);
    }

    /**
     * Parses specified mail using given handler as call-back and given initial prefix for mail part identifiers; e.g.
     * <code>&quot;1.1&quot;</code>.
     * 
     * @param mail The mail to parse
     * @param handler The call-back handler
     * @param prefix The initial prefix for mail part identifiers; e.g. <code>&quot;1.1&quot;</code>
     * @throws MailException If parsing specified mail fails
     */
    public void parseMailMessage(final MailMessage mail, final MailMessageHandler handler, final String prefix) throws MailException {
        if (null == mail) {
            throw new MailException(MailException.Code.MISSING_PARAMETER, "mail");
        }
        if (null == handler) {
            throw new MailException(MailException.Code.MISSING_PARAMETER, "handler");
        }
        try {
            /*
             * Parse mail's envelope
             */
            parseEnvelope(mail, handler);
            /*
             * Parse content
             */
            parseMailContent(mail, handler, prefix, 1);
        } catch (final IOException e) {
            final String mailId = mail.getMailId();
            final String folder = mail.getFolder();
            throw new MailException(
                MailException.Code.UNREADBALE_PART_CONTENT,
                e,
                null == mailId ? "" : mailId,
                null == folder ? "" : folder);
        }
        handler.handleMessageEnd(mail);
    }

    private void parseMailContent(final MailPart mailPartArg, final MailMessageHandler handler, final String prefix, final int partCountArg) throws MailException, IOException {
        if (stop) {
            return;
        }
        /*
         * Part modifier
         */
        final MailPart mailPart = MailConfig.usePartModifier() ? MailConfig.getPartModifier().modifyPart(mailPartArg) : mailPartArg;
        /*
         * Set part information
         */
        int partCount = partCountArg;
        final String disposition = mailPart.containsContentDisposition() ? mailPart.getContentDisposition().getDisposition() : null;
        final long size = mailPart.getSize();
        final ContentType contentType =
            mailPart.containsContentType() ? mailPart.getContentType() : new ContentType(MIMETypes.MIME_APPL_OCTET);
        final String lcct = LocaleTools.toLowerCase(contentType.getBaseType());
        final String filename = getFileName(mailPart.getFileName(), getSequenceId(prefix, partCount), lcct);

        /*
         * Parse part dependent on its MIME type
         */
        final boolean isInline = inlineDetector.isInline(disposition, mailPart.getFileName());
        /*-
         * formerly:
         * final boolean isInline = ((disposition == null
         *     || disposition.equalsIgnoreCase(Part.INLINE)) && mailPart.getFileName() == null);
         */
        if (isMultipart(lcct)) {
            try {
                final int count = mailPart.getEnclosedCount();
                if (count == -1) {
                    throw new MailException(MailException.Code.INVALID_MULTIPART_CONTENT);
                }
                final String mpId = null == prefix && !multipartDetected ? "" : getSequenceId(prefix, partCount);
                if (!mailPart.containsSequenceId()) {
                    mailPart.setSequenceId(mpId);
                }
                if (!handler.handleMultipart(mailPart, count, mpId)) {
                    stop = true;
                    return;
                }
                final String mpPrefix;
                if (multipartDetected) {
                    mpPrefix = mpId;
                } else {
                    mpPrefix = prefix;
                    multipartDetected = true;
                }
                for (int i = 0; i < count; i++) {
                    final MailPart enclosedContent = mailPart.getEnclosedMailPart(i);
                    parseMailContent(enclosedContent, handler, mpPrefix, i + 1);
                }
            } catch (final RuntimeException rte) {
                /*
                 * Parsing of multipart mail failed fatally; treat as empty plain-text mail
                 */
                LOG.error("Multipart mail could not be parsed", rte);
                warnings.add(new MailException(MailException.Code.UNPARSEABLE_MESSAGE, rte, new Object[0]));
                if (!handler.handleInlinePlainText(
                    "",
                    ContentType.DEFAULT_CONTENT_TYPE,
                    0,
                    filename,
                    MailMessageParser.getSequenceId(prefix, partCount))) {
                    stop = true;
                    return;
                }
            }
        } else if (isText(lcct)) {
            if (isInline) {
                final String content = readContent(mailPart, contentType);
                final UUEncodedMultiPart uuencodedMP = new UUEncodedMultiPart(content);
                if (uuencodedMP.isUUEncoded()) {
                    /*
                     * UUEncoded content detected. Handle normal text.
                     */
                    if (!handler.handleInlineUUEncodedPlainText(
                        uuencodedMP.getCleanText(),
                        contentType,
                        uuencodedMP.getCleanText().length(),
                        filename,
                        getSequenceId(prefix, partCount))) {
                        stop = true;
                        return;
                    }
                    /*
                     * Now handle uuencoded attachments
                     */
                    final int count = uuencodedMP.getCount();
                    if (count > 0) {
                        for (int a = 0; a < count; a++) {
                            /*
                             * Increment part count by 1
                             */
                            partCount++;
                            if (!handler.handleInlineUUEncodedAttachment(
                                uuencodedMP.getBodyPart(a),
                                MailMessageParser.getSequenceId(prefix, partCount))) {
                                stop = true;
                                return;
                            }
                        }
                    }
                } else {
                    /*
                     * Just non-encoded plain text
                     */
                    if (!handler.handleInlinePlainText(
                        content,
                        contentType,
                        size,
                        filename,
                        MailMessageParser.getSequenceId(prefix, partCount))) {
                        stop = true;
                        return;
                    }
                }
            } else {
                /*
                 * Non-Inline: Text attachment
                 */
                if (!mailPart.containsSequenceId()) {
                    mailPart.setSequenceId(getSequenceId(prefix, partCount));
                }
                if (!handler.handleAttachment(mailPart, false, lcct, filename, mailPart.getSequenceId())) {
                    stop = true;
                    return;
                }
            }
        } else if (isHtml(lcct)) {
            if (!mailPart.containsSequenceId()) {
                mailPart.setSequenceId(getSequenceId(prefix, partCount));
            }
            if (isInline) {
                if (!handler.handleInlineHtml(readContent(mailPart, contentType), contentType, size, filename, mailPart.getSequenceId())) {
                    stop = true;
                    return;
                }
            } else {
                if (!handler.handleAttachment(mailPart, false, lcct, filename, mailPart.getSequenceId())) {
                    stop = true;
                    return;
                }
            }
        } else if (isImage(lcct)) {
            if (!mailPart.containsSequenceId()) {
                mailPart.setSequenceId(getSequenceId(prefix, partCount));
            }
            if (!handler.handleImagePart(mailPart, mailPart.getContentId(), lcct, isInline, filename, mailPart.getSequenceId())) {
                stop = true;
                return;
            }
        } else if (isMessage(lcct)) {
            if (!mailPart.containsSequenceId()) {
                mailPart.setSequenceId(getSequenceId(prefix, partCount));
            }
            if (true || isInline) { // Fix for bug #16461: Show every RFC822 part as a nested mail
                if (!handler.handleNestedMessage(mailPart, getSequenceId(prefix, partCount))) {
                    stop = true;
                    return;
                }
            } else {
                if (!handler.handleAttachment(mailPart, isInline, MIMETypes.MIME_MESSAGE_RFC822, filename, mailPart.getSequenceId())) {
                    stop = true;
                    return;
                }
            }
        } else if (TNEFUtils.isTNEFMimeType(lcct)) {
            try {
                /*
                 * Here go with TNEF encoded messages. Since TNEF library is based on JavaMail API we are forced to use JavaMail-specific
                 * types regardless of the mail implementation. First, grab TNEF input stream.
                 */
                final TNEFInputStream tnefInputStream = new TNEFInputStream(mailPart.getInputStream());
                /*
                 * Wrapping TNEF message
                 */
                final net.freeutils.tnef.Message message = new net.freeutils.tnef.Message(tnefInputStream);
                /*
                 * Handle special conversion
                 */
                final Attr messageClass = message.getAttribute(Attr.attMessageClass);
                final String messageClassName = messageClass == null ? "" : ((String) messageClass.getValue());
                if (TNEF_IPM_CONTACT.equalsIgnoreCase(messageClassName)) {
                    /*
                     * Convert contact to standard vCard. Resulting Multipart object consists of only ONE BodyPart object which encapsulates
                     * converted VCard. But for consistency reasons keep the code structure to iterate over Multipart's child objects.
                     */
                    final Multipart mp;
                    try {
                        mp = ContactHandler.convert(message);
                    } catch (final RuntimeException e) {
                        LOG.error("Invalid TNEF contact", e);
                        return;
                    }
                    final int mpsize = mp.getCount();
                    for (int i = 0; i < mpsize; i++) {
                        /*
                         * Since TNEF library is based on JavaMail API we use an instance of IMAPMailContent regardless of the mail
                         * implementation
                         */
                        parseMailContent(MIMEMessageConverter.convertPart(mp.getBodyPart(i), false), handler, prefix, partCount++);
                    }
                    /*
                     * Stop to further process TNEF attachment
                     */
                    return;
                } else if (messageClassName.equalsIgnoreCase(TNEF_IPM_MS_READ_RECEIPT)) {
                    /*
                     * Convert read receipt to standard notification. Resulting Multipart object consists both the human readable text part
                     * and machine readable part.
                     */
                    final Multipart mp;
                    try {
                        mp = ReadReceiptHandler.convert(message);
                    } catch (final RuntimeException e) {
                        if (WARN_ENABLED) {
                            LOG.warn("Invalid TNEF read receipt", e);
                        }
                        return;
                    }
                    final int mpsize = mp.getCount();
                    for (int i = 0; i < mpsize; i++) {
                        /*
                         * Since TNEF library is based on JavaMail API we use an instance of IMAPMailContent regardless of the mail
                         * implementation
                         */
                        parseMailContent(MIMEMessageConverter.convertPart(mp.getBodyPart(i)), handler, prefix, partCount++);
                    }
                    /*
                     * Stop to further process TNEF attachment
                     */
                    return;
                } else if (TNEF2ICal.isVPart(messageClassName)) {
                    final net.fortuna.ical4j.model.Calendar calendar = TNEF2ICal.tnef2VPart(message);
                    if (null != calendar) {
                        /*
                         * VPart successfully converted. Generate appropriate body part.
                         */
                        final TNEFBodyPart part = new TNEFBodyPart();
                        /*
                         * Determine VPart's Content-Type
                         */
                        final String contentTypeStr;
                        {
                            final net.fortuna.ical4j.model.Property method = calendar.getProperties().getProperty(net.fortuna.ical4j.model.Property.METHOD);
                            if (null == method) {
                                contentTypeStr = "text/calendar; charset=UTF-8";
                            } else {
                                contentTypeStr = new StringBuilder("text/calendar; method=").append(method.getValue()).append("; charset=UTF-8").toString();
                            }
                        }
                        /*
                         * Set part's body
                         */
                        {
                            final byte[] bytes = calendar.toString().getBytes("UTF-8");
                            part.setDataHandler(new DataHandler(new MessageDataSource(bytes, contentTypeStr)));
                            part.setSize(bytes.length);
                        }
                        /*
                         * Set part's headers
                         */
                        part.setHeader(MessageHeaders.HDR_CONTENT_TYPE, contentTypeStr);
                        part.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
                        {
                            final net.fortuna.ical4j.model.Component vEvent = calendar.getComponents().getComponent(net.fortuna.ical4j.model.Component.VEVENT);
                            final Property summary = vEvent.getProperties().getProperty(net.fortuna.ical4j.model.Property.SUMMARY);
                            if (summary != null) {
                                part.setFileName(new StringBuilder(MimeUtility.encodeText(summary.getValue().replaceAll("\\s", "_"), MailProperties.getInstance().getDefaultMimeCharset(), "Q")).append(".ics").toString());
                            }
                        }
                        /*
                         * Parse part
                         */
                        parseMailContent(MIMEMessageConverter.convertPart(part), handler, prefix, partCount++);
                        /*
                         * Stop to further process TNEF attachment
                         */
                        return;
                    }
                }
                /*
                 * Look for body. Usually the body is the RTF text.
                 */
                final Attr attrBody = Attr.findAttr(message.getAttributes(), Attr.attBody);
                if (attrBody != null) {
                    final TNEFBodyPart bodyPart = new TNEFBodyPart();
                    final String value = (String) attrBody.getValue();
                    bodyPart.setText(value);
                    bodyPart.setSize(value.length());
                    parseMailContent(MIMEMessageConverter.convertPart(bodyPart), handler, prefix, partCount++);
                }
                final MAPIProps mapiProps = message.getMAPIProps();
                if (mapiProps != null) {
                    final RawInputStream ris = (RawInputStream) mapiProps.getPropValue(MAPIProp.PR_RTF_COMPRESSED);
                    if (ris != null) {
                        final TNEFBodyPart bodyPart = new TNEFBodyPart();
                        /*
                         * Decompress RTF body
                         */
                        final byte[] decompressedBytes = TNEFUtils.decompressRTF(ris.toByteArray());
                        final String contentTypeStr;
                        {
                            // final String charset = CharsetDetector.detectCharset(new
                            // UnsynchronizedByteArrayInputStream(decompressedBytes));
                            contentTypeStr = "application/rtf";
                        }
                        /*
                         * Set content through a data handler to avoid further exceptions raised by unavailable DCH (data content handler)
                         */
                        bodyPart.setDataHandler(new DataHandler(new MessageDataSource(decompressedBytes, contentTypeStr)));
                        bodyPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, contentTypeStr);
                        bodyPart.setSize(decompressedBytes.length);
                        parseMailContent(MIMEMessageConverter.convertPart(bodyPart), handler, prefix, partCount++);
                    }
                }
                /*
                 * Iterate TNEF attachments and nested messages
                 */
                final int s = message.getAttachments().size();
                if (s > 0) {
                    final Iterator<?> iter = message.getAttachments().iterator();
                    final ByteArrayOutputStream os = new UnsynchronizedByteArrayOutputStream(BUF_SIZE);
                    for (int i = 0; i < s; i++) {
                        final Attachment attachment = (Attachment) iter.next();
                        final TNEFBodyPart bodyPart = new TNEFBodyPart();
                        if (attachment.getNestedMessage() == null) {
                            /*
                             * Add TNEF attributes
                             */
                            bodyPart.setTNEFAttributes(attachment.getAttributes());
                            /*
                             * Translate TNEF attributes to MIME
                             */
                            final String attachFilename = attachment.getFilename();
                            String contentTypeStr = null;
                            if (attachment.getMAPIProps() != null) {
                                contentTypeStr = (String) attachment.getMAPIProps().getPropValue(MAPIProp.PR_ATTACH_MIME_TAG);
                            }
                            if ((contentTypeStr == null) && (attachFilename != null)) {
                                contentTypeStr = MIMEType2ExtMap.getContentType(attachFilename);
                            }
                            if (contentTypeStr == null) {
                                contentTypeStr = MIMETypes.MIME_APPL_OCTET;
                            }
                            final DataSource ds = new RawDataSource(attachment.getRawData(), contentTypeStr);
                            bodyPart.setDataHandler(new DataHandler(ds));
                            bodyPart.setHeader(
                                MessageHeaders.HDR_CONTENT_TYPE,
                                ContentType.prepareContentTypeString(contentTypeStr, attachFilename));
                            if (attachFilename != null) {
                                final ContentDisposition cd = new ContentDisposition(Part.ATTACHMENT);
                                cd.setFilenameParameter(attachFilename);
                                bodyPart.setHeader(
                                    MessageHeaders.HDR_CONTENT_DISPOSITION,
                                    MIMEMessageUtility.foldContentDisposition(cd.toString()));
                            }
                            os.reset();
                            attachment.writeTo(os);
                            bodyPart.setSize(os.size());
                            parseMailContent(MIMEMessageConverter.convertPart(bodyPart), handler, prefix, partCount++);
                        } else {
                            /*
                             * Nested message
                             */
                            final MimeMessage nestedMessage =
                                TNEFMime.convert(MIMEDefaultSession.getDefaultSession(), attachment.getNestedMessage());
                            os.reset();
                            nestedMessage.writeTo(os);
                            bodyPart.setDataHandler(new DataHandler(new MessageDataSource(os.toByteArray(), MIMETypes.MIME_MESSAGE_RFC822)));
                            bodyPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMETypes.MIME_MESSAGE_RFC822);
                            parseMailContent(MIMEMessageConverter.convertPart(bodyPart), handler, prefix, partCount++);
                        }
                    }
                } else {
                    // As attachment
                    if (null == messageClass) {
                        if (!mailPart.containsSequenceId()) {
                            mailPart.setSequenceId(getSequenceId(prefix, partCount));
                        }
                        if (!handler.handleAttachment(mailPart, isInline, lcct, filename, mailPart.getSequenceId())) {
                            stop = true;
                            return;
                        }
                    } else {
                        final TNEFBodyPart bodyPart = new TNEFBodyPart();
                        /*
                         * Add TNEF attributes
                         */
                        bodyPart.setTNEFAttributes(message.getAttributes());
                        /*
                         * Translate TNEF attributes to MIME
                         */
                        final String attachFilename = filename;
                        final DataSource ds = new RawDataSource(messageClass.getRawData(), MIMETypes.MIME_APPL_OCTET);
                        bodyPart.setDataHandler(new DataHandler(ds));
                        bodyPart.setHeader(
                            MessageHeaders.HDR_CONTENT_TYPE,
                            ContentType.prepareContentTypeString(MIMETypes.MIME_APPL_OCTET, attachFilename));
                        if (attachFilename != null) {
                            final ContentDisposition cd = new ContentDisposition(Part.ATTACHMENT);
                            cd.setFilenameParameter(attachFilename);
                            bodyPart.setHeader(
                                MessageHeaders.HDR_CONTENT_DISPOSITION,
                                MIMEMessageUtility.foldContentDisposition(cd.toString()));
                        }
                        bodyPart.setSize(messageClass.getLength());
                        parseMailContent(MIMEMessageConverter.convertPart(bodyPart), handler, prefix, partCount++);
                    }
                }
            } catch (final IOException tnefExc) {
                if (WARN_ENABLED) {
                    LOG.warn(tnefExc.getMessage(), tnefExc);
                }
                if (!mailPart.containsSequenceId()) {
                    mailPart.setSequenceId(getSequenceId(prefix, partCount));
                }
                if (!handler.handleAttachment(mailPart, isInline, lcct, filename, mailPart.getSequenceId())) {
                    stop = true;
                    return;
                }
            } catch (final MessagingException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(e.getMessage(), e);
                }
                if (!mailPart.containsSequenceId()) {
                    mailPart.setSequenceId(getSequenceId(prefix, partCount));
                }
                if (!handler.handleAttachment(mailPart, isInline, lcct, filename, mailPart.getSequenceId())) {
                    stop = true;
                    return;
                }
            }
        } else if (isSpecial(lcct)) {
            if (!mailPart.containsSequenceId()) {
                mailPart.setSequenceId(getSequenceId(prefix, partCount));
            }
            if (!handler.handleSpecialPart(mailPart, lcct, filename, mailPart.getSequenceId())) {
                stop = true;
                return;
            }
        } else {
            if (!mailPart.containsSequenceId()) {
                mailPart.setSequenceId(getSequenceId(prefix, partCount));
            }
            if (!handler.handleAttachment(mailPart, isInline, lcct, filename, mailPart.getSequenceId())) {
                stop = true;
                return;
            }
        }
    }

    private void parseEnvelope(final MailMessage mail, final MailMessageHandler handler) throws MailException {
        /*
         * FROM
         */
        handler.handleFrom(mail.getFrom());
        /*
         * RECIPIENTS
         */
        handler.handleToRecipient(mail.getTo());
        handler.handleCcRecipient(mail.getCc());
        handler.handleBccRecipient(mail.getBcc());
        /*
         * SUBJECT
         */
        handler.handleSubject(MIMEMessageUtility.decodeMultiEncodedHeader(mail.getSubject()));
        /*
         * SENT DATE
         */
        if (mail.getSentDate() != null) {
            handler.handleSentDate(mail.getSentDate());
        }
        /*
         * RECEIVED DATE
         */
        if (mail.getReceivedDate() != null) {
            handler.handleReceivedDate(mail.getReceivedDate());
        }
        /*
         * FLAGS
         */
        handler.handleSystemFlags(mail.getFlags());
        handler.handleUserFlags(mail.getUserFlags());
        /*
         * COLOR LABEL
         */
        handler.handleColorLabel(mail.getColorLabel());
        /*
         * PRIORITY
         */
        handler.handlePriority(mail.getPriority());
        /*
         * CONTENT-ID
         */
        if (mail.containsContentId()) {
            handler.handleContentId(mail.getContentId());
        }
        /*
         * MSGREF
         */
        if (mail.getMsgref() != null) {
            handler.handleMsgRef(mail.getMsgref().toString());
        }
        /*
         * DISPOSITION-NOTIFICATION-TO
         */
        if (mail.containsDispositionNotification() && (null != mail.getDispositionNotification())) {
            handler.handleDispositionNotification(
                mail.getDispositionNotification(),
                mail.containsPrevSeen() ? mail.isPrevSeen() : mail.isSeen());
        }
        /*
         * HEADERS
         */
        final int headersSize = mail.getHeadersSize();
        handler.handleHeaders(headersSize, headersSize > 0 ? mail.getHeadersIterator() : EMPTY_ITER);
    }

    private static final String PREFIX = "Part_";

    /**
     * Generates an appropriate filename from either specified <code>rawFileName</code> if not <code>null</code> or generates a filename
     * composed with <code>"Part_" + sequenceId</code>
     * 
     * @param rawFileName The raw filename obtained from mail part
     * @param sequenceId The part's sequence ID
     * @param baseMimeType The base MIME type to look up an appropriate file extension, if <code>rawFileName</code> is <code>null</code>
     * @return An appropriate filename
     */
    public static String getFileName(final String rawFileName, final String sequenceId, final String baseMimeType) {
        String filename = rawFileName;
        if ((filename == null) || isEmptyString(filename)) {
            final List<String> exts = MIMEType2ExtMap.getFileExtensions(baseMimeType.toLowerCase(Locale.ENGLISH));
            final StringBuilder sb = new StringBuilder(16).append(PREFIX).append(sequenceId).append('.');
            if (exts == null) {
                sb.append("dat");
            } else {
                sb.append(exts.get(0));
            }
            filename = sb.toString();
        } else {
            filename = MIMEMessageUtility.decodeMultiEncodedHeader(filename);
            // try {
            // filename = MimeUtility.decodeText(filename.replaceAll("\\?==\\?", "?= =?"));
            // } catch (final Exception e) {
            // LOG.error(e.getMessage(), e);
            // }
        }
        return filename;
    }

    private static boolean isEmptyString(final String str) {
        final char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (!Character.isWhitespace(chars[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Composes part's sequence ID from given prefix and part's count
     * 
     * @param prefix The prefix (may be <code>null</code>)
     * @param partCount The part count
     * @return The sequence ID
     */
    public static String getSequenceId(final String prefix, final int partCount) {
        if (prefix == null) {
            return String.valueOf(partCount);
        }
        return new StringBuilder(prefix).append('.').append(partCount).toString();
    }

    /**
     * Generates a filename consisting of common prefix "Part_" and part's sequence ID appended
     * 
     * @param sequenceId Part's sequence ID
     * @param baseMimeType The base MIME type to look up an appropriate file extension if <code>rawFileName</code> is <code>null</code>
     * @return The generated filename
     */
    public static String generateFilename(final String sequenceId, final String baseMimeType) {
        return getFileName(null, sequenceId, baseMimeType);
    }

    private static String readContent(final MailPart mailPart, final ContentType contentType) throws MailException, IOException {
        if (is7BitTransferEncoding(mailPart) && (mailPart instanceof MIMERawSource)) {
            try {
                final byte[] bytes = MessageUtility.getBytesFrom(((MIMERawSource) mailPart).getRawInputStream());
                if (!MessageUtility.isAscii(bytes)) {
                    try {
                        return new String(bytes, CharsetDetector.detectCharset(new UnsynchronizedByteArrayInputStream(bytes)));
                    } catch (final UnsupportedEncodingException e) {
                        return new String(bytes, "US-ASCII");
                    }
                }
                /*
                 * Return ASCII string
                 */
                return new String(bytes, "US-ASCII");
            } catch (final MailException e) {
                // getRawInputStream() failed
                if (LOG.isDebugEnabled()) {
                    LOG.debug("MIMERawSource.getRawInputStream() failed. Fallback to transfer-decoded reading.", e);
                }
            }
        }
        /*
         * Read content
         */
        final String charset = getCharset(mailPart, contentType);
        try {
            return MessageUtility.readMailPart(mailPart, charset);
        } catch (final java.io.CharConversionException e) {
            // Obviously charset was wrong or bogus implementation of character conversion
            final String fallback = "US-ASCII";
            if (WARN_ENABLED) {
                LOG.warn(
                    new StringBuilder("Character conversion exception while reading content with charset \"").append(charset).append(
                        "\". Using fallback charset \"").append(fallback).append("\" instead."),
                    e);
            }
            return MessageUtility.readMailPart(mailPart, fallback);
        }
    }

    private static boolean is7BitTransferEncoding(final MailPart mailPart) {
        final String transferEncoding = mailPart.getFirstHeader(MessageHeaders.HDR_CONTENT_TRANSFER_ENC);
        /*-
         * Taken from RFC 2045 Section 6.1. (Content-Transfer-Encoding Syntax):
         * ...
         * "Content-Transfer-Encoding: 7BIT" is assumed if the Content-Transfer-Encoding header field is not present.
         * ...
         */
        return (null == transferEncoding || "7bit".equalsIgnoreCase(transferEncoding.trim()));
    }

    private static String getCharset(final MailPart mailPart, final ContentType contentType) throws MailException {
        final String charset;
        if (mailPart.containsHeader(MessageHeaders.HDR_CONTENT_TYPE)) {
            String cs = contentType.getCharsetParameter();
            if (!CharsetDetector.isValid(cs)) {
                StringBuilder sb = null;
                if (null != cs) {
                    sb = new StringBuilder(64).append("Illegal or unsupported encoding: \"").append(cs).append("\".");
                    mailInterfaceMonitor.addUnsupportedEncodingExceptions(cs);
                }
                if (contentType.startsWith(PRIMARY_TEXT)) {
                    cs = CharsetDetector.detectCharset(mailPart.getInputStream());
                    if (WARN_ENABLED && null != sb) {
                        sb.append(" Using auto-detected encoding: \"").append(cs).append('"');
                        LOG.warn(sb.toString());
                    }
                } else {
                    cs = MailProperties.getInstance().getDefaultMimeCharset();
                    if (WARN_ENABLED && null != sb) {
                        sb.append(" Using fallback encoding: \"").append(cs).append('"');
                        LOG.warn(sb.toString());
                    }
                }
            }
            charset = cs;
        } else {
            if (contentType.startsWith(PRIMARY_TEXT)) {
                charset = CharsetDetector.detectCharset(mailPart.getInputStream());
            } else {
                charset = MailProperties.getInstance().getDefaultMimeCharset();
            }
        }
        return charset;
    }

    private static final String PRIMARY_TEXT = "text/";

    private static final String[] SUB_TEXT = { "plain", "enriched", "richtext", "rtf" };

    /**
     * Checks if content type matches one of text content types:
     * <ul>
     * <li><code>text/plain</code></li>
     * <li><code>text/enriched</code></li>
     * <li><code>text/richtext</code></li>
     * <li><code>text/rtf</code></li>
     * </ul>
     * 
     * @param contentType The content type
     * @return <code>true</code> if content type matches text; otherwise <code>false</code>
     */
    private static boolean isText(final String contentType) {
        if (contentType.startsWith(PRIMARY_TEXT, 0)) {
            final int off = PRIMARY_TEXT.length();
            for (final String subtype : SUB_TEXT) {
                if (contentType.startsWith(subtype, off)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final String PRIMARY_HTML = "text/htm";

    /**
     * Checks if content type matches <code>text/htm*</code> content type.
     * 
     * @param contentType The content type
     * @return <code>true</code> if content type matches <code>text/htm*</code>; otherwise <code>false</code>
     */
    private static boolean isHtml(final String contentType) {
        return contentType.startsWith(PRIMARY_HTML, 0);
    }

    private static final String PRIMARY_MULTI = "multipart/";

    /**
     * Checks if content type matches <code>multipart/*</code> content type.
     * 
     * @param contentType The content type
     * @return <code>true</code> if content type matches <code>multipart/*</code>; otherwise <code>false</code>
     */
    private static boolean isMultipart(final String contentType) {
        return contentType.startsWith(PRIMARY_MULTI, 0);
    }

    private static final String PRIMARY_IMAGE = "image/";

    /**
     * Checks if content type matches <code>image/*</code> content type.
     * 
     * @param contentType The content type
     * @return <code>true</code> if content type matches <code>image/*</code>; otherwise <code>false</code>
     */
    private static boolean isImage(final String contentType) {
        return contentType.startsWith(PRIMARY_IMAGE, 0);
    }

    private static final String PRIMARY_RFC822 = "message/rfc822";

    /**
     * Checks if content type matches <code>message/rfc822</code> content type.
     * 
     * @param contentType The content type
     * @return <code>true</code> if content type matches <code>message/rfc822</code>; otherwise <code>false</code>
     */
    private static boolean isMessage(final String contentType) {
        return contentType.startsWith(PRIMARY_RFC822, 0);
    }

    private static final String PRIMARY_MESSAGE = "message/";

    private static final String[] SUB_SPECIAL1 = { "delivery-status", "disposition-notification" };

    private static final String[] SUB_SPECIAL2 = { "rfc822-headers", "vcard", "x-vcard", "calendar", "x-vcalendar" };

    /**
     * Checks if content type matches one of special content types:
     * <ul>
     * <li><code>message/delivery-status</code></li>
     * <li><code>message/disposition-notification</code></li>
     * <li><code>text/rfc822-headers</code></li>
     * <li><code>text/vcard</code></li>
     * <li><code>text/x-vcard</code></li>
     * <li><code>text/calendar</code></li>
     * <li><code>text/x-vcalendar</code></li>
     * </ul>
     * 
     * @param contentType The content type
     * @return <code>true</code> if content type matches special; otherwise <code>false</code>
     */
    private static boolean isSpecial(final String contentType) {
        if (contentType.startsWith(PRIMARY_TEXT, 0)) {
            final int off = PRIMARY_TEXT.length();
            for (final String subtype : SUB_SPECIAL2) {
                if (contentType.startsWith(subtype, off)) {
                    return true;
                }
            }
        } else if (contentType.startsWith(PRIMARY_MESSAGE, 0)) {
            final int off = PRIMARY_MESSAGE.length();
            for (final String subtype : SUB_SPECIAL1) {
                if (contentType.startsWith(subtype, off)) {
                    return true;
                }
            }
        }
        return false;
    }

}
