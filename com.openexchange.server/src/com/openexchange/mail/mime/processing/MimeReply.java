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

package com.openexchange.mail.mime.processing;

import static com.openexchange.mail.mime.utils.MIMEMessageUtility.parseAddressList;
import static com.openexchange.mail.mime.utils.MIMEMessageUtility.unfold;
import static java.util.regex.Matcher.quoteReplacement;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.i18n.MailStrings;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailPath;
import com.openexchange.mail.MailSessionCache;
import com.openexchange.mail.MailSessionParameterNames;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.MailAccess;
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
import com.openexchange.mail.mime.utils.MIMEMessageUtility;
import com.openexchange.mail.parser.MailMessageParser;
import com.openexchange.mail.parser.handlers.InlineContentHandler;
import com.openexchange.mail.text.HTMLProcessing;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.mail.utils.CharsetDetector;
import com.openexchange.mail.utils.MessageUtility;
import com.openexchange.mail.utils.StorageUtility;
import com.openexchange.session.Session;
import com.openexchange.tools.regex.MatcherReplacer;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;

/**
 * {@link MimeReply} - MIME message reply.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MimeReply {

    private static final org.apache.commons.logging.Log LOG = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(MimeReply.class));

    private static final String PREFIX_RE = "Re: ";

    /**
     * No instantiation.
     */
    private MimeReply() {
        super();
    }

    /**
     * Composes a reply message from specified original message based on MIME objects from <code>JavaMail</code> API.
     *
     * @param originalMail The referenced original mail
     * @param replyAll <code>true</code> to reply to all; otherwise <code>false</code>
     * @param session The session containing needed user data
     * @param accountId The account ID
     * @return An instance of {@link MailMessage} representing an user-editable reply mail
     * @throws OXException If reply mail cannot be composed
     */
    public static MailMessage getReplyMail(final MailMessage originalMail, final boolean replyAll, final Session session, final int accountId) throws OXException {
        return getReplyMail(originalMail, replyAll, session, accountId, null);
    }

    /**
     * Composes a reply message from specified original message based on MIME objects from <code>JavaMail</code> API.
     *
     * @param originalMail The referenced original mail
     * @param replyAll <code>true</code> to reply to all; otherwise <code>false</code>
     * @param session The session containing needed user data
     * @param accountId The account ID
     * @param usm The user mail settings to use; leave to <code>null</code> to obtain from specified session
     * @return An instance of {@link MailMessage} representing an user-editable reply mail
     * @throws OXException If reply mail cannot be composed
     */
    public static MailMessage getReplyMail(final MailMessage originalMail, final boolean replyAll, final Session session, final int accountId, final UserSettingMail usm) throws OXException {
        boolean preferToAsRecipient = false;
        final String originalMailFolder = originalMail.getFolder();
        MailPath msgref = null;
        if (originalMail.getMailId() != null && originalMailFolder != null) {
            msgref = new MailPath(accountId, originalMailFolder, originalMail.getMailId());
            /*
             * Properly set preferToAsRecipient dependent on whether original mail's folder denotes the default sent folder or drafts folder
             */
            final String[] arr =
                MailSessionCache.getInstance(session).getParameter(accountId, MailSessionParameterNames.getParamDefaultFolderArray());
            if (arr == null) {
                final MailAccess<?, ?> mailAccess = MailAccess.getInstance(session, accountId);
                mailAccess.connect();
                try {
                    final IMailFolderStorage folderStorage = mailAccess.getFolderStorage();
                    preferToAsRecipient =
                        originalMailFolder.equals(folderStorage.getSentFolder()) || originalMailFolder.equals(folderStorage.getDraftsFolder());
                } finally {
                    mailAccess.close(false);
                }
            } else {
                preferToAsRecipient =
                    originalMailFolder.equals(arr[StorageUtility.INDEX_SENT]) || originalMailFolder.equals(arr[StorageUtility.INDEX_DRAFTS]);
            }
        }
        return getReplyMail(
            originalMail,
            msgref,
            replyAll,
            preferToAsRecipient,
            session,
            accountId,
            MIMEDefaultSession.getDefaultSession(),
            usm);
    }

    /**
     * Composes a reply message from specified original message based on MIME objects from <code>JavaMail</code> API.
     *
     * @param originalMsg The referenced original message
     * @param msgref The message reference
     * @param replyAll <code>true</code> to reply to all; otherwise <code>false</code>
     * @param preferToAsRecipient <code>true</code> to prefer header 'To' as recipient; otherwise <code>false</code>
     * @param session The session containing needed user data
     * @param accountId The account ID
     * @param mailSession The mail session
     * @param userSettingMail The user mail settings to use; leave to <code>null</code> to obtain from specified session
     * @return An instance of {@link MailMessage} representing an user-editable reply mail
     * @throws OXException If reply mail cannot be composed
     */
    private static MailMessage getReplyMail(final MailMessage originalMsg, final MailPath msgref, final boolean replyAll, final boolean preferToAsRecipient, final Session session, final int accountId, final javax.mail.Session mailSession, final UserSettingMail userSettingMail) throws OXException {
        try {
            final Context ctx = ContextStorage.getStorageContext(session.getContextId());
            final UserSettingMail usm =
                userSettingMail == null ? UserSettingMailStorage.getInstance().getUserSettingMail(session.getUserId(), ctx) : userSettingMail;
            /*
             * New MIME message with a dummy session
             */
            final MimeMessage replyMsg = new MimeMessage(MIMEDefaultSession.getDefaultSession());
            /*
             * Set headers of reply message
             */
            final String subjectPrefix = PREFIX_RE;
            String subjectHdrValue = MIMEMessageUtility.checkNonAscii(originalMsg.getHeader(MessageHeaders.HDR_SUBJECT, null));
            if (subjectHdrValue == null) {
                subjectHdrValue = "";
            }
            final String rawSubject = unfold(subjectHdrValue);
            {
                final String decodedSubject = MIMEMessageUtility.decodeMultiEncodedHeader(rawSubject);
                final String newSubject =
                    decodedSubject.regionMatches(true, 0, subjectPrefix, 0, 4) ? decodedSubject : new StringBuilder().append(subjectPrefix).append(
                        decodedSubject).toString();
                replyMsg.setSubject(newSubject, MailProperties.getInstance().getDefaultMimeCharset());
            }
            /*
             * Set the appropriate recipients. Taken from RFC 822 section 4.4.4: If the "Reply-To" field exists, then the reply should go to
             * the addresses indicated in that field and not to the address(es) indicated in the "From" field.
             */
            final InternetAddress[] recipientAddrs;
            if (preferToAsRecipient) {
                final String hdrVal = originalMsg.getHeader(MessageHeaders.HDR_TO, MessageHeaders.HDR_ADDR_DELIM);
                if (null == hdrVal) {
                    recipientAddrs = new InternetAddress[0];
                } else {
                    recipientAddrs = parseAddressList(hdrVal, true);
                }
            } else {
                final String[] replyTo = originalMsg.getHeader(MessageHeaders.HDR_REPLY_TO);
                if (MIMEMessageUtility.isEmptyHeader(replyTo)) {
                    /*
                     * Set from as recipient
                     */
                    recipientAddrs = originalMsg.getFrom();
                } else {
                    /*
                     * Message holds header 'Reply-To'
                     */
                    recipientAddrs = QuotedInternetAddress.parseHeader(unfold(replyTo[0]), true);
                }
            }
            if (replyAll) {
                /*
                 * Create a filter which is used to sort out addresses before adding them to either field 'To' or 'Cc'
                 */
                final Set<InternetAddress> filter = new HashSet<InternetAddress>();
                /*
                 * Add user's address to filter
                 */
                if (InternetAddress.getLocalAddress(mailSession) != null) {
                    filter.add(InternetAddress.getLocalAddress(mailSession));
                }
                /*
                 * Add any other address the user is known by to filter
                 */
                final String alternates = mailSession.getProperty("mail.alternates");
                if (alternates != null) {
                    filter.addAll(Arrays.asList(parseAddressList(alternates, false)));
                }
                /*
                 * Add user's aliases to filter
                 */
                final String[] userAddrs = UserStorage.getStorageUser(session.getUserId(), ctx).getAliases();
                if (userAddrs != null && userAddrs.length > 0) {
                    final StringBuilder addrBuilder = new StringBuilder();
                    addrBuilder.append(userAddrs[0]);
                    for (int i = 1; i < userAddrs.length; i++) {
                        addrBuilder.append(',').append(userAddrs[i]);
                    }
                    filter.addAll(Arrays.asList(parseAddressList(addrBuilder.toString(), false)));
                }
                /*
                 * Determine if other original recipients should be added to 'Cc'.
                 */
                final boolean replyallcc = usm.isReplyAllCc();
                /*
                 * Filter the recipients of 'Reply-To'/'From' field
                 */
                final Set<InternetAddress> filteredAddrs = filter(filter, recipientAddrs);
                /*
                 * Add filtered recipients from 'To' field
                 */
                String hdrVal = originalMsg.getHeader(MessageHeaders.HDR_TO, MessageHeaders.HDR_ADDR_DELIM);
                InternetAddress[] toAddrs = null;
                if (hdrVal != null) {
                    filteredAddrs.addAll(filter(filter, (toAddrs = parseAddressList(hdrVal, true))));
                }
                /*
                 * ... and add filtered addresses to either 'To' or 'Cc' field
                 */
                if (!filteredAddrs.isEmpty()) {
                    if (replyallcc) {
                        // Put original sender into 'To'
                        replyMsg.addRecipients(RecipientType.TO, recipientAddrs);
                        // All other into 'Cc'
                        filteredAddrs.removeAll(Arrays.asList(recipientAddrs));
                        replyMsg.addRecipients(RecipientType.CC, filteredAddrs.toArray(new InternetAddress[filteredAddrs.size()]));
                    } else {
                        replyMsg.addRecipients(RecipientType.TO, filteredAddrs.toArray(new InternetAddress[filteredAddrs.size()]));
                    }
                } else if (toAddrs != null) {
                    final Set<InternetAddress> tmpSet = new HashSet<InternetAddress>(Arrays.asList(recipientAddrs));
                    tmpSet.removeAll(Arrays.asList(toAddrs));
                    if (tmpSet.isEmpty()) {
                        /*
                         * The message was sent from the user to himself. In this special case allow user's own address in field 'To' to
                         * avoid an empty 'To' field
                         */
                        replyMsg.addRecipients(RecipientType.TO, recipientAddrs);
                    }
                }
                /*
                 * Filter recipients from 'Cc' field
                 */
                filteredAddrs.clear();
                hdrVal = originalMsg.getHeader(MessageHeaders.HDR_CC, MessageHeaders.HDR_ADDR_DELIM);
                if (hdrVal != null) {
                    filteredAddrs.addAll(filter(filter, parseAddressList(unfold(hdrVal), true)));
                }
                if (!filteredAddrs.isEmpty()) {
                    replyMsg.addRecipients(RecipientType.CC, filteredAddrs.toArray(new InternetAddress[filteredAddrs.size()]));
                }
                /*
                 * Filter recipients from 'Bcc' field
                 */
                filteredAddrs.clear();
                hdrVal = originalMsg.getHeader(MessageHeaders.HDR_BCC, MessageHeaders.HDR_ADDR_DELIM);
                if (hdrVal != null) {
                    filteredAddrs.addAll(filter(filter, parseAddressList(unfold(hdrVal), true)));
                }
                if (!filteredAddrs.isEmpty()) {
                    replyMsg.addRecipients(RecipientType.BCC, filteredAddrs.toArray(new InternetAddress[filteredAddrs.size()]));
                }
            } else {
                /*
                 * Plain reply: Just put original sender into 'To' field
                 */
                replyMsg.addRecipients(RecipientType.TO, recipientAddrs);
            }
            /*
             * Set mail text of reply message
             */
            if (usm.isIgnoreOriginalMailTextOnReply()) {
                /*
                 * Add empty text content as message's body
                 */
                replyMsg.setText("", MailProperties.getInstance().getDefaultMimeCharset(), "plain");
                replyMsg.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
                replyMsg.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMETypes.MIME_TEXT_PLAIN_TEMPL.replaceFirst(
                    "#CS#",
                    MailProperties.getInstance().getDefaultMimeCharset()));
                final MailMessage replyMail = MIMEMessageConverter.convertMessage(replyMsg);
                if (null != msgref) {
                    replyMail.setMsgref(msgref);
                }
                return replyMail;
            }
            /*
             * Add reply text
             */
            final ContentType retvalContentType = new ContentType();
            final String replyText;
            {
                final List<String> list = new ArrayList<String>();
                {
                    final User user = UserStorage.getStorageUser(session.getUserId(), ctx);
                    final Locale locale = user.getLocale();
                    final LocaleAndTimeZone ltz = new LocaleAndTimeZone(locale, user.getTimeZone());
                    generateReplyText(originalMsg, retvalContentType, new StringHelper(locale), ltz, usm, mailSession, list);
                }
                final StringBuilder replyTextBuilder = new StringBuilder(8192 << 1);
                for (int i = list.size() - 1; i >= 0; i--) {
                    replyTextBuilder.append(list.get(i));
                }
                if (retvalContentType.getPrimaryType() == null) {
                    retvalContentType.setContentType(MIMETypes.MIME_TEXT_PLAIN);
                }
                {
                    final String cs = retvalContentType.getCharsetParameter();
                    if (cs == null || "US-ASCII".equalsIgnoreCase(cs) || !CharsetDetector.isValid(cs)) {
                        // Missing or non-unicode charset
                        retvalContentType.setCharsetParameter(MailProperties.getInstance().getDefaultMimeCharset());
                    }
                }
                replyText = replyTextBuilder.toString();
            }
            /*
             * Compose reply mail
             */
            final MailMessage replyMail;
            /*-
             * Withhold inline images. Those images are inserted through image service framework on message transport
             *
            if (retvalContentType.isMimeType(MIMETypes.MIME_TEXT_HTM_ALL) && MIMEMessageUtility.hasEmbeddedImages(replyText)) {
                // Prepare to append inline content
                final Multipart multiRelated = new MimeMultipart("related");
                {
                    final MimeBodyPart text = new MimeBodyPart();
                    text.setText(replyText, retvalContentType.getCharsetParameter(), retvalContentType.getSubType());
                    text.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
                    text.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMEMessageUtility.fold(14, retvalContentType.toString()));
                    multiRelated.addBodyPart(text);
                }
                replyMsg.setContent(multiRelated);
                replyMsg.saveChanges();
                replyMail = new CompositeMailMessage(MIMEMessageConverter.convertMessage(replyMsg));
                // Append inline content
                appendInlineContent(originalMsg, (CompositeMailMessage) replyMail, MIMEMessageUtility.getContentIDs(replyText));
            } else {
                // Set message's content directly to reply text
                replyMsg.setText(replyText, retvalContentType.getCharsetParameter(), retvalContentType.getSubType());
                replyMsg.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
                replyMsg.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMEMessageUtility.fold(14, retvalContentType.toString()));
                replyMsg.saveChanges();
                replyMail = MIMEMessageConverter.convertMessage(replyMsg);
            }
             */
            /*
             * Set message's content directly to reply text
             */
            replyMsg.setText(replyText, retvalContentType.getCharsetParameter(), retvalContentType.getSubType());
            replyMsg.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
            replyMsg.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMEMessageUtility.foldContentType(retvalContentType.toString()));
            replyMsg.saveChanges();
            replyMail = MIMEMessageConverter.convertMessage(replyMsg);
            if (null != msgref) {
                replyMail.setMsgref(msgref);
            }
            return replyMail;
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e);
        } catch (final IOException e) {
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        } catch (final OXException e) {
            throw new OXException(e);
        }

    }

    private static void appendInlineContent(final MailMessage originalMail, final CompositeMailMessage replyMail, final List<String> cids) throws OXException {
        final InlineContentHandler handler = new InlineContentHandler(cids);
        new MailMessageParser().parseMailMessage(originalMail, handler);
        final Map<String, MailPart> inlineContents = handler.getInlineContents();
        for (final String cid : cids) {
            final MailPart part = inlineContents.get(cid);
            if (null != part) {
                replyMail.addAdditionalParts(part);
            }
        }
    }

    /**
     * Filters given address array against given filter set. All addresses currently contained in filter set are removed from specified
     * <code>addrs</code> and all addresses not contained in filter set are added to filter set for future invocations.
     *
     * @param filter The current address filter
     * @param addrs The address list to filter
     * @return The filtered set of addresses
     */
    private static Set<InternetAddress> filter(final Set<InternetAddress> filter, final InternetAddress[] addrs) {
        if (addrs == null) {
            return new HashSet<InternetAddress>(0);
        }
        final Set<InternetAddress> set = new HashSet<InternetAddress>(Arrays.asList(addrs));
        /*
         * Remove all addresses from set which are contained in filter
         */
        set.removeAll(filter);
        /*
         * Add new addresses to filter
         */
        filter.addAll(set);
        return set;
    }

    private static final Pattern PATTERN_DATE = Pattern.compile(Pattern.quote("#DATE#"));

    private static final Pattern PATTERN_TIME = Pattern.compile(Pattern.quote("#TIME#"));

    private static final Pattern PATTERN_SENDER = Pattern.compile(Pattern.quote("#SENDER#"));

    private static final String MULTIPART = "multipart/";

    private static final String TEXT = "text/";

    private static final String TEXT_HTM = "text/htm";

    /**
     * Gathers all text bodies and appends them to given text builder.
     *
     * @param msg The root message
     * @param retvalContentType The return value's content type
     * @param strHelper The i18n string helper
     * @return <code>true</code> if any text was found; otherwise <code>false</code>
     * @throws OXException
     * @throws MessagingException
     * @throws IOException
     */
    private static boolean generateReplyText(final MailMessage msg, final ContentType retvalContentType, final StringHelper strHelper, final LocaleAndTimeZone ltz, final UserSettingMail usm, final javax.mail.Session mailSession, final List<String> replyTexts) throws OXException, MessagingException, IOException {
        final StringBuilder textBuilder = new StringBuilder(8192);
        final ContentType contentType = msg.getContentType();
        boolean found = false;
        if (contentType.startsWith(MULTIPART)) {
            final ParameterContainer pc =
                new ParameterContainer(retvalContentType, textBuilder, strHelper, usm, mailSession, ltz, replyTexts);
            found |= gatherAllTextContents(msg, contentType, pc);
        } else if (contentType.startsWith(TEXT) && !MimeProcessingUtility.isSpecial(contentType.getBaseType())) {
            if (retvalContentType.getPrimaryType() == null) {
                final String text = MimeProcessingUtility.handleInlineTextPart(msg, contentType, usm.isDisplayHtmlInlineContent());
                retvalContentType.setContentType(contentType);
                textBuilder.append(text);
            } else {
                final String text = MimeProcessingUtility.handleInlineTextPart(msg, contentType, usm.isDisplayHtmlInlineContent());
                MimeProcessingUtility.appendRightVersion(retvalContentType, contentType, text, textBuilder);
            }
            found = true;
        }
        if (found) {
            final boolean isHtml = retvalContentType.startsWith(TEXT_HTM);
            String replyPrefix = strHelper.getString(MailStrings.REPLY_PREFIX);
            {
                final Date date = msg.getSentDate();
                try {
                    replyPrefix =
                        PATTERN_DATE.matcher(replyPrefix).replaceFirst(
                            date == null ? "" : quoteReplacement(MimeProcessingUtility.getFormattedDate(date, DateFormat.LONG, ltz.locale, ltz.timeZone)));
                } catch (final Exception e) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn(e.getMessage(), e);
                    }
                    replyPrefix = PATTERN_DATE.matcher(replyPrefix).replaceFirst("");
                }

                try {
                    replyPrefix =
                        PATTERN_TIME.matcher(replyPrefix).replaceFirst(
                            date == null ? "" : quoteReplacement(MimeProcessingUtility.getFormattedTime(date, DateFormat.SHORT, ltz.locale, ltz.timeZone)));
                } catch (final Exception e) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn(e.getMessage(), e);
                    }
                    replyPrefix = PATTERN_TIME.matcher(replyPrefix).replaceFirst("");
                }
            }
            {
                final InternetAddress[] from = msg.getFrom();
                replyPrefix =
                    PATTERN_SENDER.matcher(replyPrefix).replaceFirst(from == null || from.length == 0 ? "" : quoteReplacement(from[0].toUnicodeString()));
            }
            {
                final char nextLine = '\n';
                if (isHtml) {
                    replyPrefix =
                        HTMLProcessing.htmlFormat(new StringBuilder(replyPrefix.length() + 1).append(nextLine).append(replyPrefix).append(
                            nextLine).append(nextLine).toString());
                } else {
                    replyPrefix =
                        new StringBuilder(replyPrefix.length() + 1).append(nextLine).append(replyPrefix).append(nextLine).append(nextLine).toString();
                }
            }
            /*
             * Surround with quote
             */
            final String replyTextBody;
            if (isHtml) {
                replyTextBody = quoteHtml(textBuilder.toString());
            } else {
                replyTextBody = quoteText(textBuilder.toString());
            }
            textBuilder.setLength(0);
            textBuilder.append(replyPrefix);
            textBuilder.append(replyTextBody);
        }
        replyTexts.add(textBuilder.toString());
        // parentTextBuilder.append(textBuilder);
        return found;
    }

    /**
     * Gathers all text bodies and appends them to given text builder.
     *
     * @return <code>true</code> if any text was found; otherwise <code>false</code>
     * @throws OXException If a mail error occurs
     * @throws MessagingException If a messaging error occurs
     * @throws IOException If an I/O error occurs
     */
    private static boolean gatherAllTextContents(final MailPart multipartPart, final ContentType mpContentType, final ParameterContainer pc) throws OXException, MessagingException, IOException {
        final int count = multipartPart.getEnclosedCount();
        final ContentType partContentType = new ContentType();
        boolean found = false;
        if (pc.usm.isDisplayHtmlInlineContent() && mpContentType.startsWith(MIMETypes.MIME_MULTIPART_ALTERNATIVE) && count >= 2) {
            /*
             * Prefer HTML content within multipart/alternative part
             */
            found = getTextContent(true, multipartPart, count, partContentType, pc);
            if (!found) {
                /*
                 * No HTML part found, retry with any text part
                 */
                found = getTextContent(false, multipartPart, count, partContentType, pc);
            }
        } else {
            /*
             * Get any text content
             */
            found = getTextContent(false, multipartPart, count, partContentType, pc);
        }
        /*
         * Look for enclosed messages in any case
         */
        for (int i = count - 1; i >= 0; i--) {
            final MailPart part = multipartPart.getEnclosedMailPart(i);
            partContentType.setContentType(part.getContentType());
            if (partContentType.startsWith(MIMETypes.MIME_MESSAGE_RFC822)) {
                final MailMessage enclosedMsg = (MailMessage) part.getContent();
                found |= generateReplyText(enclosedMsg, pc.retvalContentType, pc.strHelper, pc.ltz, pc.usm, pc.mailSession, pc.replyTexts);
            } else if (MimeProcessingUtility.fileNameEndsWith(".eml", part, partContentType)) {
                /*
                 * Create message from input stream
                 */
                final InputStream is = part.getInputStream();
                final ByteArrayOutputStream tmp = new UnsynchronizedByteArrayOutputStream(8192);
                final byte[] buf = new byte[4096];
                int read = -1;
                while ((read = is.read(buf, 0, buf.length)) != -1) {
                    tmp.write(buf, 0, read);
                }
                final MailMessage attachedMsg = MIMEMessageConverter.convertMessage(tmp.toByteArray());// MimeMessage(mailSession,
                // part.getInputStream());
                found |= generateReplyText(attachedMsg, pc.retvalContentType, pc.strHelper, pc.ltz, pc.usm, pc.mailSession, pc.replyTexts);
            }
        }
        return found;
    }

    private static boolean getTextContent(final boolean preferHTML, final MailPart multipartPart, final int count, final ContentType partContentType, final ParameterContainer pc) throws OXException, MessagingException, IOException {
        boolean found = false;
        for (int i = 0; !found && i < count; i++) {
            final MailPart part = multipartPart.getEnclosedMailPart(i);
            partContentType.setContentType(part.getContentType());
            if (partContentType.startsWith(preferHTML ? TEXT_HTM : TEXT) && MimeProcessingUtility.isInline(part, partContentType) && !MimeProcessingUtility.isSpecial(partContentType.getBaseType())) {
                if (pc.retvalContentType.getPrimaryType() == null) {
                    pc.retvalContentType.setContentType(partContentType);
                    final String charset = MessageUtility.checkCharset(part, partContentType);
                    pc.retvalContentType.setCharsetParameter(charset);
                    final String text =
                        preferHTML ? MimeProcessingUtility.readContent(part, charset) : MimeProcessingUtility.handleInlineTextPart(
                            part,
                            pc.retvalContentType,
                            pc.usm.isDisplayHtmlInlineContent());
                    pc.textBuilder.append(text);
                } else {
                    final String charset = MessageUtility.checkCharset(part, partContentType);
                    partContentType.setCharsetParameter(charset);
                    final String text =
                        MimeProcessingUtility.handleInlineTextPart(part, partContentType, pc.usm.isDisplayHtmlInlineContent());
                    MimeProcessingUtility.appendRightVersion(pc.retvalContentType, partContentType, text, pc.textBuilder);
                }
                found = true;
            } else if (partContentType.startsWith(MULTIPART)) {
                found |= gatherAllTextContents(part, partContentType, pc);
            }
        }
        return found;
    }

    private static String quoteText(final String textContent) {
        return textContent.replaceAll("(?m)^", "> ");
    }

    private static final Pattern PATTERN_HTML_START = Pattern.compile("<html[^>]*?>", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_HTML_END = Pattern.compile("</html>", Pattern.CASE_INSENSITIVE);

    private static final String BLOCKQUOTE_START =
        "<blockquote type=\"cite\" style=\"margin-left: 0px; padding-left: 10px; border-left: solid 1px blue;\">\n";

    private static final String BLOCKQUOTE_END = "</blockquote>\n<br>&nbsp;";

    private static String quoteHtml(final String htmlContent) {
        Matcher m = PATTERN_HTML_START.matcher(htmlContent);
        final MatcherReplacer mr = new MatcherReplacer(m, htmlContent);
        final StringBuilder sb = new StringBuilder(htmlContent.length());
        if (m.find()) {
            mr.appendLiteralReplacement(sb, BLOCKQUOTE_START);
        } else {
            sb.append(BLOCKQUOTE_START);
        }
        mr.appendTail(sb);
        {
            final String s = sb.toString();
            m = PATTERN_HTML_END.matcher(s);
            mr.resetTo(m, s);
        }
        sb.setLength(0);
        if (m.find()) {
            mr.appendLiteralReplacement(sb, BLOCKQUOTE_END);
            mr.appendTail(sb);
        } else {
            mr.appendTail(sb);
            sb.append(BLOCKQUOTE_END);
        }
        return sb.toString();
    }

    private static final class ParameterContainer {

        final ContentType retvalContentType;

        final StringBuilder textBuilder;

        final StringHelper strHelper;

        final UserSettingMail usm;

        final javax.mail.Session mailSession;

        final LocaleAndTimeZone ltz;

        final List<String> replyTexts;

        ParameterContainer(final ContentType retvalContentType, final StringBuilder textBuilder, final StringHelper strHelper, final UserSettingMail usm, final javax.mail.Session mailSession, final LocaleAndTimeZone ltz, final List<String> replyTexts) {
            super();
            this.retvalContentType = retvalContentType;
            this.textBuilder = textBuilder;
            this.strHelper = strHelper;
            this.usm = usm;
            this.mailSession = mailSession;
            this.ltz = ltz;
            this.replyTexts = replyTexts;
        }

    }

}
