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

package com.openexchange.mail.mime.processing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.i18n.MailStrings;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.mail.MailException;
import com.openexchange.mail.MailPath;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.CompositeMailMessage;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MIMEDefaultSession;
import com.openexchange.mail.mime.MIMEMailException;
import com.openexchange.mail.mime.MIMETypes;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.mime.converters.MIMEMessageConverter;
import com.openexchange.mail.mime.dataobjects.MIMEMailMessage;
import com.openexchange.mail.mime.dataobjects.NestedMessageMailPart;
import com.openexchange.mail.mime.datasource.StreamDataSource;
import com.openexchange.mail.mime.utils.MIMEMessageUtility;
import com.openexchange.mail.parser.MailMessageParser;
import com.openexchange.mail.parser.handlers.NonInlineForwardPartHandler;
import com.openexchange.mail.text.HTMLProcessing;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.mail.utils.MessageUtility;
import com.openexchange.session.Session;
import com.openexchange.tools.regex.MatcherReplacer;
import com.openexchange.tools.stream.UnsynchronizedByteArrayInputStream;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;

/**
 * {@link MimeForward} - MIME message forward.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MimeForward {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(MimeForward.class);

    private static final String PREFIX_FWD = "Fwd: ";

    /**
     * No instantiation
     */
    private MimeForward() {
        super();
    }

    /**
     * Composes a forward message from specified original messages based on MIME objects from <code>JavaMail</code> API.
     * <p>
     * If multiple messages are given these messages are forwarded as attachments.
     * 
     * @param originalMails The referenced original mails
     * @param session The session containing needed user data
     * @param accountID The account ID of the referenced original mails
     * @return An instance of {@link MailMessage} representing an user-editable forward mail
     * @throws MailException If forward mail cannot be composed
     */
    public static MailMessage getFowardMail(final MailMessage[] originalMails, final Session session, final int accountID) throws MailException {
        return getFowardMail(originalMails, session, accountID, null);
    }

    /**
     * Composes a forward message from specified original messages based on MIME objects from <code>JavaMail</code> API.
     * <p>
     * If multiple messages are given these messages are forwarded as attachments.
     * 
     * @param originalMails The referenced original mails
     * @param session The session containing needed user data
     * @param accountID The account ID of the referenced original mails
     * @param usm The user mail settings to use; leave to <code>null</code> to obtain from specified session
     * @return An instance of {@link MailMessage} representing an user-editable forward mail
     * @throws MailException If forward mail cannot be composed
     */
    public static MailMessage getFowardMail(final MailMessage[] originalMails, final Session session, final int accountID, final UserSettingMail usm) throws MailException {
        for (int i = 0; i < originalMails.length; i++) {
            final MailMessage cur = originalMails[i];
            if (cur.getMailId() != null && cur.getFolder() != null && cur.getAccountId() != accountID) {
                cur.setAccountId(accountID);
            }
        }
        /*
         * Compose forward message
         */
        return getFowardMail0(originalMails, session, usm);
    }

    /**
     * Composes a forward message from specified original messages taken from possibly differing accounts based on MIME objects from
     * <code>JavaMail</code> API.
     * <p>
     * If multiple messages are given these messages are forwarded as attachments.
     * 
     * @param originalMails The referenced original mails
     * @param session The session containing needed user data
     * @param accountIDs The account IDs of the referenced original mails
     * @param usm The user mail settings to use; leave to <code>null</code> to obtain from specified session
     * @return An instance of {@link MailMessage} representing an user-editable forward mail
     * @throws MailException If forward mail cannot be composed
     */
    public static MailMessage getFowardMail(final MailMessage[] originalMails, final Session session, final int[] accountIDs, final UserSettingMail usm) throws MailException {
        for (int i = 0; i < originalMails.length; i++) {
            final MailMessage cur = originalMails[i];
            if (cur.getMailId() != null && cur.getFolder() != null && cur.getAccountId() != accountIDs[i]) {
                cur.setAccountId(accountIDs[i]);
            }
        }
        /*
         * Compose forward message
         */
        return getFowardMail0(originalMails, session, usm);
    }

    /**
     * Composes a forward message from specified original messages based on MIME objects from <code>JavaMail</code> API.
     * <p>
     * If multiple messages are given these messages are forwarded as attachments.
     * 
     * @param originalMsgs The referenced original messages
     * @param session The session containing needed user data
     * @param userSettingMail The user mail settings to use; leave to <code>null</code> to obtain from specified session
     * @return An instance of {@link MailMessage} representing an user-editable forward mail
     * @throws MailException If forward mail cannot be composed
     */
    private static MailMessage getFowardMail0(final MailMessage[] originalMsgs, final Session session, final UserSettingMail userSettingMail) throws MailException {
        try {
            /*
             * New MIME message with a dummy session
             */
            final Context ctx = ContextStorage.getStorageContext(session.getContextId());
            final UserSettingMail usm =
                userSettingMail == null ? UserSettingMailStorage.getInstance().getUserSettingMail(session.getUserId(), ctx) : userSettingMail;
            final MimeMessage forwardMsg = new MimeMessage(MIMEDefaultSession.getDefaultSession());
            {
                /*
                 * Set its headers. Start with subject constructed from first message.
                 */
                final String subjectPrefix = PREFIX_FWD;
                String origSubject = originalMsgs[0].getHeader(MessageHeaders.HDR_SUBJECT, null);
                if (origSubject == null) {
                    forwardMsg.setSubject(subjectPrefix, MailProperties.getInstance().getDefaultMimeCharset());
                } else {
                    origSubject = MIMEMessageUtility.unfold(origSubject);
                    final String subject =
                        MIMEMessageUtility.decodeMultiEncodedHeader(origSubject.regionMatches(
                            true,
                            0,
                            subjectPrefix,
                            0,
                            subjectPrefix.length()) ? origSubject : new StringBuilder(subjectPrefix.length() + origSubject.length()).append(
                            subjectPrefix).append(origSubject).toString());
                    forwardMsg.setSubject(subject, MailProperties.getInstance().getDefaultMimeCharset());
                }
            }
            /*
             * Set from
             */
            if (usm.getSendAddr() != null) {
                forwardMsg.setFrom(new QuotedInternetAddress(usm.getSendAddr(), true));
            }
            if (usm.isForwardAsAttachment() || originalMsgs.length > 1) {
                /*
                 * Attachment-Forward
                 */
                return asAttachmentForward(originalMsgs, forwardMsg);
            }
            /*
             * Inline-Forward
             */
            return asInlineForward(originalMsgs[0], session, ctx, usm, forwardMsg);
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e);
        } catch (final IOException e) {
            throw new MailException(MailException.Code.IO_ERROR, e, e.getMessage());
        } catch (final ContextException e) {
            throw new MailException(e);
        }
    }

    private static final String MULTIPART = "multipart/";

    private static final String TEXT = "text/";

    private static final String TEXT_HTM = "text/htm";

    private static MailMessage asInlineForward(final MailMessage originalMsg, final Session session, final Context ctx, final UserSettingMail usm, final MimeMessage forwardMsg) throws MailException, MessagingException, IOException {
        /*
         * Check for message reference
         */
        final MailPath msgref = originalMsg.getMailPath();
        final ContentType originalContentType = originalMsg.getContentType();
        final MailMessage forwardMail;
        if (originalContentType.startsWith(MULTIPART)) {
            final Multipart multipart = new MimeMultipart();
            {
                /*
                 * Grab first seen text from original message
                 */
                final ContentType contentType = new ContentType();
                final String firstSeenText = getFirstSeenText(originalMsg, contentType, usm);
                {
                    final String cs = contentType.getCharsetParameter();
                    if (cs == null || "US-ASCII".equalsIgnoreCase(cs)) {
                        contentType.setCharsetParameter(MailProperties.getInstance().getDefaultMimeCharset());
                    }
                }
                /*
                 * Add appropriate text part prefixed with forward text
                 */
                final MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(
                    generateForwardText(firstSeenText == null ? "" : firstSeenText, new LocaleAndTimeZone(UserStorage.getStorageUser(
                        session.getUserId(),
                        ctx)), originalMsg, contentType.startsWith(TEXT_HTM)),
                    contentType.getCharsetParameter(),
                    contentType.getSubType());
                textPart.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
                textPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMEMessageUtility.foldContentType(contentType.toString()));
                multipart.addBodyPart(textPart);
                forwardMsg.setContent(multipart);
                forwardMsg.saveChanges();
            }
            final CompositeMailMessage compositeMail = new CompositeMailMessage(MIMEMessageConverter.convertMessage(forwardMsg));
            /*
             * Add all non-inline parts through a handler to keep original sequence IDs
             */
            final NonInlineForwardPartHandler handler = new NonInlineForwardPartHandler();
            new MailMessageParser().setInlineDetectorBehavior(true).parseMailMessage(originalMsg, handler);
            final List<MailPart> parts = handler.getNonInlineParts();
            for (final MailPart mailPart : parts) {
                mailPart.getContentDisposition().setDisposition(Part.ATTACHMENT);
                compositeMail.addAdditionalParts(mailPart);
            }
            forwardMail = compositeMail;
        } else if (originalContentType.startsWith(TEXT)) {
            /*
             * Original message is a simple text mail: Add message body prefixed with forward text
             */
            {
                final String cs = originalContentType.getCharsetParameter();
                if (null == cs) {
                    originalContentType.setCharsetParameter(MessageUtility.checkCharset(originalMsg, originalContentType));
                }
            }
            final String content = MimeProcessingUtility.readContent(originalMsg, originalContentType.getCharsetParameter());
            forwardMsg.setText(
                generateForwardText(content == null ? "" : content, new LocaleAndTimeZone(UserStorage.getStorageUser(
                    session.getUserId(),
                    ctx)), originalMsg, originalContentType.startsWith(TEXT_HTM)),
                originalContentType.getCharsetParameter(),
                originalContentType.getSubType());
            forwardMsg.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
            forwardMsg.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMEMessageUtility.foldContentType(originalContentType.toString()));
            forwardMsg.saveChanges();
            forwardMail = MIMEMessageConverter.convertMessage(forwardMsg);
        } else {
            /*
             * Mail only consists of one non-textual part
             */
            final Multipart multipart = new MimeMultipart();
            /*
             * Add appropriate text part prefixed with forward text
             */
            {
                final ContentType contentType = new ContentType(MIMETypes.MIME_TEXT_PLAIN);
                contentType.setCharsetParameter(MailProperties.getInstance().getDefaultMimeCharset());
                final MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(generateForwardText(
                    "",
                    new LocaleAndTimeZone(UserStorage.getStorageUser(session.getUserId(), ctx)),
                    originalMsg,
                    false), MailProperties.getInstance().getDefaultMimeCharset(), "plain");
                textPart.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
                textPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMEMessageUtility.foldContentType(contentType.toString()));
                multipart.addBodyPart(textPart);
                forwardMsg.setContent(multipart);
                forwardMsg.saveChanges();
            }
            final CompositeMailMessage compositeMail = new CompositeMailMessage(MIMEMessageConverter.convertMessage(forwardMsg));
            /*
             * Add the single "attachment"
             */
            final MailPart attachmentMailPart;
            {
                final MimeBodyPart attachmentPart = new MimeBodyPart();
                {
                    final StreamDataSource.InputStreamProvider isp = new StreamDataSource.InputStreamProvider() {

                        public InputStream getInputStream() throws IOException {
                            try {
                                return originalMsg.getInputStream();
                            } catch (final MailException e) {
                                final IOException io = new IOException(e.getMessage());
                                io.initCause(e);
                                throw io;
                            }
                        }

                        public String getName() {
                            return null;
                        }
                    };
                    attachmentPart.setDataHandler(new DataHandler(new StreamDataSource(isp, originalContentType.toString())));
                }
                for (final Iterator<Map.Entry<String, String>> e = originalMsg.getHeadersIterator(); e.hasNext();) {
                    final Map.Entry<String, String> header = e.next();
                    final String name = header.getKey();
                    if (name.toLowerCase(Locale.ENGLISH).startsWith("content-")) {
                        attachmentPart.addHeader(name, header.getValue());
                    }
                }
                attachmentMailPart = MIMEMessageConverter.convertPart(attachmentPart, false);
            }
            attachmentMailPart.setSequenceId(String.valueOf(1));
            compositeMail.addAdditionalParts(attachmentMailPart);
            forwardMail = compositeMail;
        }
        if (null != msgref) {
            forwardMail.setMsgref(msgref);
        }
        return forwardMail;
    }

    private static MailMessage asAttachmentForward(final MailMessage[] originalMsgs, final MimeMessage forwardMsg) throws MessagingException, MailException {
        final CompositeMailMessage compositeMail;
        {
            final Multipart multipart = new MimeMultipart();
            /*
             * Add empty text content as message's body
             */
            final MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText("", MailProperties.getInstance().getDefaultMimeCharset(), "plain");
            textPart.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
            textPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMETypes.MIME_TEXT_PLAIN_TEMPL.replaceFirst(
                "#CS#",
                MailProperties.getInstance().getDefaultMimeCharset()));
            multipart.addBodyPart(textPart);
            forwardMsg.setContent(multipart);
            forwardMsg.saveChanges();
            compositeMail = new CompositeMailMessage(MIMEMessageConverter.convertMessage(forwardMsg));
        }
        /*
         * Attach messages
         */
        for (final MailMessage originalMsg : originalMsgs) {
            final MailMessage nested;
            if (originalMsg instanceof MIMEMailMessage) {
                nested = MIMEMessageConverter.convertMessage(((MIMEMailMessage) originalMsg).getMimeMessage());
            } else {
                final ByteArrayOutputStream tmp = new UnsynchronizedByteArrayOutputStream((int) originalMsg.getSize());
                originalMsg.writeTo(tmp);
                nested =
                    MIMEMessageConverter.convertMessage(new MimeMessage(
                        MIMEDefaultSession.getDefaultSession(),
                        new UnsynchronizedByteArrayInputStream(tmp.toByteArray())));
            }
            nested.setMsgref(originalMsg.getMailPath());
            compositeMail.addAdditionalParts(new NestedMessageMailPart(nested));
        }
        return compositeMail;
    }

    /**
     * Determines the first seen text in given multipart content with recursive iteration over enclosed multipart contents.
     * 
     * @param mp The multipart object
     * @param retvalContentType The return value's content type (gets filled during processing and should therefore be empty)
     * @return The first seen text content
     * @throws MailException
     * @throws MessagingException
     * @throws IOException
     */
    private static String getFirstSeenText(final MailPart multipartPart, final ContentType retvalContentType, final UserSettingMail usm) throws MailException, MessagingException, IOException {
        final ContentType contentType = multipartPart.getContentType();
        final int count = multipartPart.getEnclosedCount();
        final ContentType partContentType = new ContentType();
        if ((contentType.startsWith(MIMETypes.MIME_MULTIPART_ALTERNATIVE) || contentType.startsWith(MIMETypes.MIME_MULTIPART_RELATED)) && usm.isDisplayHtmlInlineContent() && count >= 2) {
            /*
             * Get html content
             */
            for (int i = 0; i < count; i++) {
                final MailPart part = multipartPart.getEnclosedMailPart(i);
                partContentType.setContentType(part.getContentType());
                if (partContentType.startsWith(TEXT_HTM) && MimeProcessingUtility.isInline(part, partContentType)) {
                    final String charset = MessageUtility.checkCharset(part, partContentType);
                    retvalContentType.setContentType(partContentType);
                    retvalContentType.setCharsetParameter(charset);
                    return MimeProcessingUtility.readContent(part, charset);
                } else if (partContentType.startsWith(MULTIPART)) {
                    final String text = getFirstSeenText(part, retvalContentType, usm);
                    if (text != null) {
                        return text;
                    }
                }
            }
        }
        /*
         * Get any text content
         */
        for (int i = 0; i < count; i++) {
            final MailPart part = multipartPart.getEnclosedMailPart(i);
            partContentType.setContentType(part.getContentType());
            if (partContentType.startsWith(TEXT) && MimeProcessingUtility.isInline(part, partContentType)) {
                final String charset = MessageUtility.checkCharset(part, partContentType);
                retvalContentType.setContentType(partContentType);
                retvalContentType.setCharsetParameter(charset);
                return MimeProcessingUtility.handleInlineTextPart(part, retvalContentType, usm.isDisplayHtmlInlineContent());
            } else if (partContentType.startsWith(MULTIPART)) {
                final String text = getFirstSeenText(part, retvalContentType, usm);
                if (text != null) {
                    return text;
                }
            }
        }
        /*
         * No text content found
         */
        return null;
    }

    private static final Pattern PATTERN_BODY = Pattern.compile("<body[^>]*?>", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_FROM = Pattern.compile(Pattern.quote("#FROM#"));

    private static final Pattern PATTERN_TO = Pattern.compile(Pattern.quote("#TO#"));

    private static final Pattern PATTERN_CCLINE = Pattern.compile(Pattern.quote("#CC_LINE#"));

    private static final Pattern PATTERN_DATE = Pattern.compile(Pattern.quote("#DATE#"));

    private static final Pattern PATTERN_TIME = Pattern.compile(Pattern.quote("#TIME#"));

    private static final Pattern PATTERN_SUBJECT = Pattern.compile(Pattern.quote("#SUBJECT#"));

    /**
     * Generates the forward text on an inline-forward operation.
     * 
     * @param firstSeenText The first seen text from original message
     * @param ltz The locale that determines format of date and time strings and time zone as well
     * @param msg The original message
     * @param html <code>true</code> if given text is html content; otherwise <code>false</code>
     * @return The forward text
     */
    private static String generateForwardText(final String firstSeenText, final LocaleAndTimeZone ltz, final MailMessage msg, final boolean html) {
        final StringHelper strHelper = new StringHelper(ltz.locale);
        String forwardPrefix = strHelper.getString(MailStrings.FORWARD_PREFIX);
        {
            final InternetAddress[] from = msg.getFrom();
            forwardPrefix =
                PATTERN_FROM.matcher(forwardPrefix).replaceFirst(from == null || from.length == 0 ? "" : from[0].toUnicodeString());
        }
        {
            final InternetAddress[] to = msg.getTo();
            forwardPrefix =
                PATTERN_TO.matcher(forwardPrefix).replaceFirst(to == null || to.length == 0 ? "" : MimeProcessingUtility.addrs2String(to));
        }
        {
            final InternetAddress[] cc = msg.getCc();
            forwardPrefix =
                PATTERN_CCLINE.matcher(forwardPrefix).replaceFirst(
                    cc == null || cc.length == 0 ? "" : new StringBuilder(64).append("\nCc: ").append(
                        MimeProcessingUtility.addrs2String(cc)).toString());
        }
        {
            final Date date = msg.getSentDate();
            try {
                forwardPrefix =
                    PATTERN_DATE.matcher(forwardPrefix).replaceFirst(
                        date == null ? "" : MimeProcessingUtility.getFormattedDate(date, DateFormat.LONG, ltz.locale, ltz.timeZone));
            } catch (final Exception t) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(t.getMessage(), t);
                }
                forwardPrefix = PATTERN_DATE.matcher(forwardPrefix).replaceFirst("");
            }
            try {
                forwardPrefix =
                    PATTERN_TIME.matcher(forwardPrefix).replaceFirst(
                        date == null ? "" : MimeProcessingUtility.getFormattedDate(date, DateFormat.SHORT, ltz.locale, ltz.timeZone));
            } catch (final Exception t) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(t.getMessage(), t);
                }
                forwardPrefix = PATTERN_TIME.matcher(forwardPrefix).replaceFirst("");
            }

        }
        {
            final String decodedSubject = MIMEMessageUtility.decodeMultiEncodedHeader(msg.getSubject());
            forwardPrefix =
                PATTERN_SUBJECT.matcher(forwardPrefix).replaceFirst(decodedSubject == null ? "" : Matcher.quoteReplacement(decodedSubject));
        }
        if (html) {
            forwardPrefix = HTMLProcessing.htmlFormat(forwardPrefix);
        }
        final String linebreak = html ? "<br>" : "\r\n";
        if (html) {
            final Matcher m = PATTERN_BODY.matcher(firstSeenText);
            final MatcherReplacer mr = new MatcherReplacer(m, firstSeenText);
            final StringBuilder replaceBuffer = new StringBuilder(firstSeenText.length() + 256);
            if (m.find()) {
                mr.appendLiteralReplacement(replaceBuffer, new StringBuilder(forwardPrefix.length() + 16).append(linebreak).append(
                    m.group()).append(forwardPrefix).append(linebreak).append(linebreak).toString());
            } else {
                replaceBuffer.append(linebreak).append(forwardPrefix).append(linebreak).append(linebreak);
            }
            mr.appendTail(replaceBuffer);
            return replaceBuffer.toString();
        }
        return new StringBuilder(firstSeenText.length() + 256).append(linebreak).append(forwardPrefix).append(linebreak).append(linebreak).append(
            firstSeenText).toString();
    }

    /*-
     * 
    private static void addNonInlineParts(final MimeMessage originalMsg, final CompositeMailMessage forwardMail) throws MailException {
        final MailMessage originalMail = MIMEMessageConverter.convertMessage(originalMsg);
        final NonInlineForwardPartHandler handler = new NonInlineForwardPartHandler();
        new MailMessageParser().parseMailMessage(originalMail, handler);
        final List<MailPart> nonInlineParts = handler.getNonInlineParts();
        for (final MailPart mailPart : nonInlineParts) {
            forwardMail.addAdditionalParts(mailPart);
        }
    }
     */

}
