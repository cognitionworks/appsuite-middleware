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

package com.openexchange.mail.mime.filler;

import static com.openexchange.mail.text.TextProcessing.performLineFolding;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import com.openexchange.conversion.ConversionService;
import com.openexchange.conversion.Data;
import com.openexchange.conversion.DataProperties;
import com.openexchange.exception.OXException;
import com.openexchange.filemanagement.ManagedFile;
import com.openexchange.filemanagement.ManagedFileManagement;
import com.openexchange.groupware.contact.ContactInterface;
import com.openexchange.groupware.contact.ContactInterfaceDiscoveryService;
import com.openexchange.groupware.contact.Contacts;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.i18n.MailStrings;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.html.HTMLService;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.image.ImageDataSource;
import com.openexchange.image.ImageLocation;
import com.openexchange.image.ImageUtility;
import com.openexchange.log.LogProperties;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.dataobjects.compose.ComposeType;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.dataobjects.compose.ComposedMailPart;
import com.openexchange.mail.dataobjects.compose.ComposedMailPart.ComposedPartType;
import com.openexchange.mail.dataobjects.compose.ReferencedMailPart;
import com.openexchange.mail.dataobjects.compose.TextBodyMailPart;
import com.openexchange.mail.mime.ContentDisposition;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MIMEMailException;
import com.openexchange.mail.mime.MIMEType2ExtMap;
import com.openexchange.mail.mime.MIMETypes;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.mime.datasource.MessageDataSource;
import com.openexchange.mail.mime.utils.MIMEMessageUtility;
import com.openexchange.mail.mime.utils.sourcedimage.SourcedImage;
import com.openexchange.mail.mime.utils.sourcedimage.SourcedImageUtility;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.server.impl.DBPool;
import com.openexchange.server.impl.Version;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.tools.regex.MatcherReplacer;
import com.openexchange.tools.stream.UnsynchronizedByteArrayInputStream;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;
import com.openexchange.tools.versit.Versit;
import com.openexchange.tools.versit.VersitDefinition;
import com.openexchange.tools.versit.VersitObject;
import com.openexchange.tools.versit.converter.ConverterException;
import com.openexchange.tools.versit.converter.OXContainerConverter;
import com.openexchange.user.UserService;

/**
 * {@link MIMEMessageFiller} - Provides basic methods to fills an instance of {@link MimeMessage} with headers/contents given through an
 * instance of {@link ComposedMailMessage}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class MIMEMessageFiller {

    private static final org.apache.commons.logging.Log LOG = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(MIMEMessageFiller.class));

    private static final boolean DEBUG = LOG.isDebugEnabled();

    private static final String PREFIX_PART = "part";

    private static final String EXT_EML = ".eml";

    private static final int BUF_SIZE = 0x2000;

    private static final String VERSION_1_0 = "1.0";

    private static final String VCARD_ERROR = "Error while appending user VCard";

    /*
     * Constants for Multipart types
     */
    private static final String MP_ALTERNATIVE = "alternative";

    private static final String MP_RELATED = "related";

    /*
     * Patterns for common MIME text types
     */
    private static final String REPLACE_CS = "#CS#";

    private static final String PAT_TEXT_CT = "text/plain; charset=#CS#";

    private static final String PAT_HTML_CT = "text/html; charset=#CS#";

    /*
     * Fields
     */
    protected final Session session;

    protected final Context ctx;

    protected final UserSettingMail usm;

    private Set<String> uploadFileIDs;

    private final HTMLService htmlService;

    /**
     * Initializes a new {@link MIMEMessageFiller}
     *
     * @param session The session providing user data
     * @param ctx The context
     */
    public MIMEMessageFiller(final Session session, final Context ctx) {
        this(session, ctx, UserSettingMailStorage.getInstance().getUserSettingMail(session.getUserId(), ctx));
    }

    /**
     * Initializes a new {@link MIMEMessageFiller}
     *
     * @param session The session providing user data
     * @param ctx The context
     * @param usm The user's mail settings
     */
    public MIMEMessageFiller(final Session session, final Context ctx, final UserSettingMail usm) {
        super();
        htmlService = ServerServiceRegistry.getInstance().getService(HTMLService.class);
        this.session = session;
        this.ctx = ctx;
        this.usm = usm;
    }

    /*
     * protected final Html2TextConverter getConverter() { if (converter == null) { converter = new Html2TextConverter(); } return
     * converter; }
     */

    /**
     * Deletes referenced local uploaded files from session and disk after filled instance of <code>{@link MimeMessage}</code> is dispatched
     */
    public void deleteReferencedUploadFiles() {
        if (uploadFileIDs != null) {
            final int size = uploadFileIDs.size();
            final Iterator<String> iter = uploadFileIDs.iterator();
            final ManagedFileManagement mfm = ServerServiceRegistry.getInstance().getService(ManagedFileManagement.class);
            if (mfm != null) {
                for (int i = 0; i < size; i++) {
                    try {
                        mfm.removeByID(iter.next());
                    } catch (final OXException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
            uploadFileIDs.clear();
        }
    }

    /**
     * Sets common headers in given MIME message: <code>X-Mailer</code> and <code>Organization</code>.
     *
     * @param mimeMessage The MIME message
     * @throws MessagingException If headers cannot be set
     */
    public void setCommonHeaders(final MimeMessage mimeMessage) throws MessagingException {
        /*
         * Set mailer
         */
        mimeMessage.setHeader(MessageHeaders.HDR_X_MAILER, "Open-Xchange Mailer v" + Version.getVersionString());
        /*
         * Set organization to context-admin's company field setting
         */
        try {
            final ContactInterface contactInterface =
                ServerServiceRegistry.getInstance().getService(ContactInterfaceDiscoveryService.class).newContactInterface(
                    FolderObject.SYSTEM_LDAP_FOLDER_ID,
                    session);

            final Contact c = contactInterface.getUserById(ctx.getMailadmin(), false);
            if (null != c && c.getCompany() != null && c.getCompany().length() > 0) {
                final String encoded =
                    MimeUtility.fold(14, MimeUtility.encodeText(c.getCompany(), MailProperties.getInstance().getDefaultMimeCharset(), null));
                mimeMessage.setHeader(MessageHeaders.HDR_ORGANIZATION, encoded);
            }
        } catch (final Exception e) {
            LOG.error("Header \"Organization\" could not be set", e);
        }
        /*
         * Add header X-Originating-IP containing the IP address of the client
         */
        if (MailProperties.getInstance().isAddClientIPAddress()) {
            addClientIPAddress(mimeMessage, session);
        }
    }

    /**
     * Add "X-Originating-IP" header.
     *
     * @param mimeMessage The MIME message
     * @param session The session
     * @throws MessagingException If an error occurs
     */
    public static void addClientIPAddress(final MimeMessage mimeMessage, final Session session) throws MessagingException {
        /*
         * Is IP check enabled
         */
        // final ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
        // final boolean ipCheck = service.getBoolProperty(com.openexchange.configuration.ServerConfig.Property.IP_CHECK.getPropertyName(),
        // false);
        {
            /*
             * Get IP from session
             */
            final String localIp = session.getLocalIp();
            if (isLocalhost(localIp)) {
                if (DEBUG) {
                    LOG.debug("Session provides localhost as client IP address: " + localIp);
                }
                // Prefer request's remote address if local IP seems to denote local host
                final Map<String, Object> logProperties = LogProperties.optLogProperties();
                final String clientIp = null == logProperties ? null : (String) logProperties.get("com.openexchange.ajp13.requestIp");
                mimeMessage.setHeader("X-Originating-IP", clientIp == null ? localIp : clientIp);
            } else {
                mimeMessage.setHeader("X-Originating-IP", localIp);
            }
        }
        // else {
        // /*
        // * IP check disabled: Prefer IP from client request
        // */
        // final Map<String, Object> logProperties = LogProperties.optLogProperties();
        // final String clientIp = null == logProperties ? null : (String) logProperties.get("com.openexchange.ajp13.requestIp");
        // mimeMessage.setHeader("X-Originating-IP", clientIp == null ? session.getLocalIp() : clientIp);
        // }
    }

    private static final Set<String> LOCAL_ADDRS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("127.0.0.1", "localhost", "::1")));

    private static boolean isLocalhost(final String localIp) {
        return LOCAL_ADDRS.contains(localIp);
    }

    private static final String[] SUPPRESS_HEADERS = {
        MessageHeaders.HDR_X_OX_VCARD, MessageHeaders.HDR_X_OXMSGREF, MessageHeaders.HDR_X_OX_MARKER, MessageHeaders.HDR_X_OX_NOTIFICATION,
        MessageHeaders.HDR_IMPORTANCE, MessageHeaders.HDR_X_PRIORITY, MessageHeaders.HDR_X_MAILER };

    /**
     * Sets necessary headers in specified MIME message: <code>From</code>/ <code>Sender</code>, <code>To</code>, <code>Cc</code>,
     * <code>Bcc</code>, <code>Reply-To</code>, <code>Subject</code>, etc.
     *
     * @param mail The composed mail
     * @param mimeMessage The MIME message
     * @throws MessagingException If headers cannot be set
     * @throws OXException If a mail error occurs
     */
    public void setMessageHeaders(final ComposedMailMessage mail, final MimeMessage mimeMessage) throws MessagingException, OXException {
        /*
         * Set from/sender
         */
        if (mail.containsFrom()) {
            /*
             * Get from
             */
            final InternetAddress from = mail.getFrom()[0];
            mimeMessage.setFrom(from);
            /*
             * Determine sender
             */
            InternetAddress sender = null;
            final MailAccountStorageService mass = ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class);
            if (null != mass) {
                try {
                    final int userId = session.getUserId();
                    final int contextId = session.getContextId();
                    int id = mass.getByPrimaryAddress(from.getAddress(), userId, contextId);
                    if (id < 0) {
                        id = mass.getByPrimaryAddress(QuotedInternetAddress.toIDN(from.getAddress()), userId, contextId);
                        if (id < 0) {
                            /*
                             * No appropriate mail account found which matches from address
                             */
                            final String sendAddr = usm.getSendAddr();
                            if (sendAddr != null && sendAddr.length() > 0) {
                                try {
                                    sender = new QuotedInternetAddress(sendAddr, true);
                                } catch (final AddressException e) {
                                    LOG.error("Default send address cannot be parsed", e);
                                }
                            }
                        }
                    }
                } catch (final OXException e) {
                    /*
                     * Conflict during look-up
                     */
                    LOG.debug(e.getMessage(), e);
                }
            }
            final List<InternetAddress> aliases;
            final UserService userService = ServerServiceRegistry.getInstance().getService(UserService.class, true);
            final User user = userService.getUser(session.getUserId(), ctx);
            aliases = new ArrayList<InternetAddress>();
            for (final String alias : user.getAliases()) {
                aliases.add(new QuotedInternetAddress(alias));
            }
            /*
             * Taken from RFC 822 section 4.4.2: In particular, the "Sender" field MUST be present if it is NOT the same as the "From"
             * Field.
             */
            if (sender != null && !from.equals(sender) && !aliases.contains(from)) {
                mimeMessage.setSender(sender);
            }
        }
        /*
         * Set to
         */
        if (mail.containsTo()) {
            mimeMessage.setRecipients(RecipientType.TO, mail.getTo());
        }
        /*
         * Set cc
         */
        if (mail.containsCc()) {
            mimeMessage.setRecipients(RecipientType.CC, mail.getCc());
        }
        /*
         * Bcc
         */
        if (mail.containsBcc()) {
            mimeMessage.setRecipients(RecipientType.BCC, mail.getBcc());
        }
        /*
         * Reply-To
         */
        setReplyTo(mail, mimeMessage);
        /*
         * Set subject
         */
        if (mail.containsSubject()) {
            mimeMessage.setSubject(mail.getSubject(), MailProperties.getInstance().getDefaultMimeCharset());
        }
        /*
         * Set sent date
         */
        if (mail.containsSentDate()) {
            final MailDateFormat mdf = MIMEMessageUtility.getMailDateFormat(session);
            synchronized (mdf) {
                mimeMessage.setHeader("Date", mdf.format(mail.getSentDate()));
            }
        }
        /*
         * Set flags
         */
        final Flags msgFlags = new Flags();
        if (mail.isAnswered()) {
            msgFlags.add(Flags.Flag.ANSWERED);
        }
        if (mail.isDeleted()) {
            msgFlags.add(Flags.Flag.DELETED);
        }
        if (mail.isDraft()) {
            msgFlags.add(Flags.Flag.DRAFT);
        }
        if (mail.isFlagged()) {
            msgFlags.add(Flags.Flag.FLAGGED);
        }
        if (mail.isRecent()) {
            msgFlags.add(Flags.Flag.RECENT);
        }
        if (mail.isSeen()) {
            msgFlags.add(Flags.Flag.SEEN);
        }
        if (mail.isUser()) {
            msgFlags.add(Flags.Flag.USER);
        }
        if (mail.isForwarded()) {
            msgFlags.add(MailMessage.USER_FORWARDED);
        }
        if (mail.isReadAcknowledgment()) {
            msgFlags.add(MailMessage.USER_READ_ACK);
        }
        if (mail.getColorLabel() != MailMessage.COLOR_LABEL_NONE) {
            msgFlags.add(MailMessage.getColorLabelStringValue(mail.getColorLabel()));
        }
        {
            final String[] userFlags = mail.getUserFlags();
            if (null != userFlags && userFlags.length > 0) {
                for (final String userFlag : userFlags) {
                    msgFlags.add(userFlag);
                }
            }
        }
        /*
         * Finally, apply flags to message
         */
        mimeMessage.setFlags(msgFlags, true);
        /*
         * Set disposition notification
         */
        if (mail.getDispositionNotification() != null) {
            if (mail.isDraft()) {
                mimeMessage.setHeader(MessageHeaders.HDR_X_OX_NOTIFICATION, mail.getDispositionNotification().toString());
            } else {
                mimeMessage.setHeader(MessageHeaders.HDR_DISP_TO, mail.getDispositionNotification().toString());
            }
        }
        /*
         * Set priority
         */
        final int priority = mail.getPriority();
        mimeMessage.setHeader(MessageHeaders.HDR_X_PRIORITY, String.valueOf(priority));
        if (MailMessage.PRIORITY_NORMAL == priority) {
            mimeMessage.setHeader(MessageHeaders.HDR_IMPORTANCE, "Medium");
        } else if (priority > MailMessage.PRIORITY_NORMAL) {
            mimeMessage.setHeader(MessageHeaders.HDR_IMPORTANCE, "Low");
        } else {
            mimeMessage.setHeader(MessageHeaders.HDR_IMPORTANCE, "High");
        }
        /*
         * Headers
         */
        for (final Iterator<Map.Entry<String, String>> iter = mail.getNonMatchingHeaders(SUPPRESS_HEADERS); iter.hasNext();) {
            final Map.Entry<String, String> entry = iter.next();
            final String name = entry.getKey();
            if (name.toLowerCase(Locale.ENGLISH).startsWith("x-")) {
                mimeMessage.addHeader(name, entry.getValue());
            }
        }
    }

    private void setReplyTo(final ComposedMailMessage mail, final MimeMessage mimeMessage) throws OXException, MessagingException {
        /*
         * Reply-To
         */
        final String hdrReplyTo = mail.getFirstHeader("Reply-To");
        if (null != hdrReplyTo) {
            InternetAddress[] replyTo = null;
            try {
                replyTo = QuotedInternetAddress.parse(hdrReplyTo, true);
            } catch (final AddressException e) {
                LOG.error("Specified Reply-To address cannot be parsed", e);
            }
            if (null != replyTo) {
                mimeMessage.setReplyTo(replyTo);
            } else if (mail.containsFrom()) {
                mimeMessage.setReplyTo(mail.getFrom());
            }
        } else if (usm.getReplyToAddr() == null || usm.getReplyToAddr().length() == 0) {
            InternetAddress[] replyTo = null;
            final MailAccountStorageService mass = ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class);
            if (null != mass) {
                final String sReplyTo = mass.getMailAccount(mail.getAccountId(), session.getUserId(), session.getContextId()).getReplyTo();
                if (null != sReplyTo) {
                    try {
                        replyTo = QuotedInternetAddress.parse(sReplyTo, true);
                    } catch (final AddressException e) {
                        LOG.error("Default Reply-To address cannot be parsed", e);
                    }
                }
            }
            if (null != replyTo) {
                mimeMessage.setReplyTo(replyTo);
            } else if (mail.containsFrom()) {
                mimeMessage.setReplyTo(mail.getFrom());
            }
        } else {
            try {
                mimeMessage.setReplyTo(QuotedInternetAddress.parse(usm.getReplyToAddr(), true));
            } catch (final AddressException e) {
                LOG.error("Default Reply-To address cannot be parsed", e);
                try {
                    mimeMessage.setHeader(
                        MessageHeaders.HDR_REPLY_TO,
                        MimeUtility.encodeWord(usm.getReplyToAddr(), MailProperties.getInstance().getDefaultMimeCharset(), "Q"));
                } catch (final UnsupportedEncodingException e1) {
                    /*
                     * Cannot occur since default mime charset is supported by JVM
                     */
                    LOG.error(e1.getMessage(), e1);
                }
            }
        }
    }

    /**
     * Sets the appropriate headers <code>In-Reply-To</code> and <code>References</code> in specified MIME message.
     * <p>
     * Moreover the <code>Reply-To</code> header is set.
     *
     * @param referencedMail The referenced mail
     * @param mimeMessage The MIME message
     * @throws MessagingException If setting the reply headers fails
     */
    public void setReplyHeaders(final MailMessage referencedMail, final MimeMessage mimeMessage) throws MessagingException {
        if (null == referencedMail) {
            /*
             * Obviously referenced mail does no more exist; cancel setting reply headers Message-Id, In-Reply-To, and References.
             */
            return;
        }
        final String pMsgId = referencedMail.getFirstHeader(MessageHeaders.HDR_MESSAGE_ID);
        if (pMsgId != null) {
            mimeMessage.setHeader(MessageHeaders.HDR_IN_REPLY_TO, pMsgId);
        }
        /*
         * Set References header field
         */
        final String pReferences = referencedMail.getFirstHeader(MessageHeaders.HDR_REFERENCES);
        final String pInReplyTo = referencedMail.getFirstHeader(MessageHeaders.HDR_IN_REPLY_TO);
        final StringBuilder refBuilder = new StringBuilder();
        if (pReferences != null) {
            /*
             * The "References:" field will contain the contents of the parent's "References:" field (if any) followed by the contents of
             * the parent's "Message-ID:" field (if any).
             */
            refBuilder.append(pReferences);
        } else if (pInReplyTo != null) {
            /*
             * If the parent message does not contain a "References:" field but does have an "In-Reply-To:" field containing a single
             * message identifier, then the "References:" field will contain the contents of the parent's "In-Reply-To:" field followed by
             * the contents of the parent's "Message-ID:" field (if any).
             */
            refBuilder.append(pInReplyTo);
        }
        if (pMsgId != null) {
            if (refBuilder.length() > 0) {
                refBuilder.append(' ');
            }
            refBuilder.append(pMsgId);
        }
        if (refBuilder.length() > 0) {
            /*
             * If the parent has none of the "References:", "In-Reply-To:", or "Message-ID:" fields, then the new message will have no
             * "References:" field.
             */
            mimeMessage.setHeader(MessageHeaders.HDR_REFERENCES, refBuilder.toString());
        }
    }

    /**
     * Sets the appropriate headers before message's transport: <code>Reply-To</code>, <code>Date</code>, and <code>Subject</code>
     *
     * @param mail The source mail
     * @param mimeMessage The MIME message
     * @throws OXException If a mail error occurs
     */
    public void setSendHeaders(final ComposedMailMessage mail, final MimeMessage mimeMessage) throws OXException {
        try {
            /*
             * Set the Reply-To header for future replies to this new message
             */
            setReplyTo(mail, mimeMessage);
            /*
             * Set sent date if not done, yet
             */
            {
                final Date sentDate = mimeMessage.getSentDate();
                final MailDateFormat mdf = MIMEMessageUtility.getMailDateFormat(session);
                synchronized (mdf) {
                    mimeMessage.setHeader("Date", mdf.format(sentDate == null ? new Date() : sentDate));
                }
            }
            /*
             * Set default subject if none set
             */
            if (null == mimeMessage.getSubject()) {
                mimeMessage.setSubject(StringHelper.valueOf(UserStorage.getStorageUser(session.getUserId(), ctx).getLocale()).getString(MailStrings.DEFAULT_SUBJECT));
            }
        } catch (final AddressException e) {
            throw MIMEMailException.handleMessagingException(e);
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e);
        }
    }

    /**
     * Fills the body of given instance of {@link MimeMessage} with the contents specified through given instance of
     * {@link ComposedMailMessage}.
     *
     * @param mail The source composed mail
     * @param mimeMessage The MIME message to fill
     * @param type The compose type
     * @throws MessagingException If a messaging error occurs
     * @throws OXException If a mail error occurs
     * @throws IOException If an I/O error occurs
     */
    public void fillMailBody(final ComposedMailMessage mail, final MimeMessage mimeMessage, final ComposeType type) throws MessagingException, OXException, IOException {
        /*
         * Store some flags
         */
        final boolean hasNestedMessages = false;
        final boolean hasAttachments;
        final boolean isAttachmentForward;
        {
            final int size = mail.getEnclosedCount();
            hasAttachments = size > 0;
            /*
             * A non-inline forward message
             */
            isAttachmentForward =
                ((ComposeType.FORWARD.equals(type)) && (usm.isForwardAsAttachment() || (size > 1 && hasOnlyReferencedMailAttachments(
                    mail,
                    size))));
        }
        /*
         * Initialize primary multipart
         */
        Multipart primaryMultipart = null;
        /*
         * Detect if primary multipart is of type multipart/mixed
         */
        if (hasNestedMessages || hasAttachments || mail.isAppendVCard() || isAttachmentForward) {
            primaryMultipart = new MimeMultipart();
        }
        /*
         * Content is expected to be multipart/alternative
         */
        final boolean sendMultipartAlternative;
        if (mail.isDraft()) {
            sendMultipartAlternative = false;
            if (mail.getContentType().startsWith(MIMETypes.MIME_MULTIPART_ALTERNATIVE)) {
                /*
                 * Allow only HTML if a draft message should be "sent"
                 */
                mail.setContentType(MIMETypes.MIME_TEXT_HTML);
            }
        } else {
            sendMultipartAlternative = mail.getContentType().startsWith(MIMETypes.MIME_MULTIPART_ALTERNATIVE);
        }
        /*
         * HTML content with embedded images
         */
        final TextBodyMailPart textBodyPart = mail.getBodyPart();
        /*
         * Check for with-source images
         */
        final String content;
        final Map<String, SourcedImage> images;
        {
            final StringBuilder sb = new StringBuilder((String) textBodyPart.getContent());
            images = SourcedImageUtility.hasSourcedImages(sb);
            content = sb.toString();
        }
        /*
         * Check embedded images
         */
        final boolean embeddedImages;
        if (!images.isEmpty()) {
            embeddedImages = true;
        } else if (sendMultipartAlternative || mail.getContentType().isMimeType(MIMETypes.MIME_TEXT_HTM_ALL)) {
            /*
             * Check for referenced images (by cid oder locally available)
             */
            embeddedImages = MIMEMessageUtility.hasEmbeddedImages(content) || MIMEMessageUtility.hasReferencedLocalImages(content);
        } else {
            embeddedImages = false;
        }
        /*
         * Compose message
         */
        final String charset = MailProperties.getInstance().getDefaultMimeCharset();
        if (hasAttachments || sendMultipartAlternative || isAttachmentForward || mail.isAppendVCard() || embeddedImages) {
            /*
             * If any condition is true, we ought to create a multipart/ message
             */
            if (sendMultipartAlternative) {
                final Multipart alternativeMultipart = createMultipartAlternative(mail, content, embeddedImages, images, textBodyPart);
                if (primaryMultipart == null) {
                    primaryMultipart = alternativeMultipart;
                } else {
                    final BodyPart bodyPart = new MimeBodyPart();
                    bodyPart.setContent(alternativeMultipart);
                    primaryMultipart.addBodyPart(bodyPart);
                }
            } else if (embeddedImages) {
                final Multipart relatedMultipart = createMultipartRelated(mail, content, images, new String[1]);
                if (primaryMultipart == null) {
                    primaryMultipart = relatedMultipart;
                } else {
                    final BodyPart bodyPart = new MimeBodyPart();
                    bodyPart.setContent(relatedMultipart);
                    primaryMultipart.addBodyPart(bodyPart);
                }
            } else {
                if (primaryMultipart == null) {
                    primaryMultipart = new MimeMultipart();
                }
                /*
                 * Convert html content to regular text if mail text is demanded to be text/plain
                 */
                if (mail.getContentType().isMimeType(MIMETypes.MIME_TEXT_PLAIN)) {
                    /*
                     * Append text content
                     */
                    final String plainText = textBodyPart.getPlainText();
                    if (null == plainText) {
                        /*-
                         * Expect HTML content
                         *
                         * Well-formed HTML
                         */
                        final String wellFormedHTMLContent = htmlService.getConformHTML(content, charset);
                        primaryMultipart.addBodyPart(createTextBodyPart(wellFormedHTMLContent, charset, false, true), 0);
                    } else {
                        primaryMultipart.addBodyPart(createTextBodyPart(plainText, charset, false, false), 0);
                    }
                } else {
                    /*-
                     * Append HTML content
                     *
                     * Well-formed HTML
                     */
                    final String wellFormedHTMLContent = htmlService.getConformHTML(content, charset);
                    primaryMultipart.addBodyPart(createHtmlBodyPart(wellFormedHTMLContent, charset));
                }
            }
            /*
             * Get number of enclosed parts
             */
            final int size = mail.getEnclosedCount();
            if (size > 0) {
                if (isAttachmentForward) {
                    /*
                     * Add referenced mail(s)
                     */
                    final StringBuilder sb = new StringBuilder(32);
                    final ByteArrayOutputStream out = new UnsynchronizedByteArrayOutputStream(BUF_SIZE);
                    final byte[] bbuf = new byte[BUF_SIZE];
                    for (int i = 0; i < size; i++) {
                        addNestedMessage(mail.getEnclosedMailPart(i), primaryMultipart, sb, out, bbuf);
                    }
                } else {
                    /*
                     * Add referenced parts from ONE referenced mail
                     */
                    for (int i = 0; i < size; i++) {
                        addMessageBodyPart(primaryMultipart, mail.getEnclosedMailPart(i), false);
                    }
                }
            }
            /*
             * Append VCard
             */
            AppendVCard: if (mail.isAppendVCard()) {
                final String fileName =
                    MimeUtility.encodeText(
                        new StringBuilder(UserStorage.getStorageUser(session.getUserId(), ctx).getDisplayName().replaceAll("\\s+", "")).append(
                            ".vcf").toString(),
                        charset,
                        "Q");
                for (int i = 0; i < size; i++) {
                    final MailPart part = mail.getEnclosedMailPart(i);
                    if (fileName.equalsIgnoreCase(part.getFileName())) {
                        /*
                         * VCard already attached in (former draft) message
                         */
                        break AppendVCard;
                    }
                }
                if (primaryMultipart == null) {
                    primaryMultipart = new MimeMultipart();
                }
                try {
                    final String userVCard = getUserVCard(charset);
                    /*
                     * Create a body part for vcard
                     */
                    final MimeBodyPart vcardPart = new MimeBodyPart();
                    /*
                     * Define content
                     */
                    final ContentType ct = new ContentType(MIMETypes.MIME_TEXT_X_VCARD);
                    ct.setCharsetParameter(charset);
                    vcardPart.setDataHandler(new DataHandler(new MessageDataSource(userVCard, ct)));
                    if (fileName != null && !ct.containsNameParameter()) {
                        ct.setNameParameter(fileName);
                    }
                    vcardPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMEMessageUtility.foldContentType(ct.toString()));
                    vcardPart.setHeader(MessageHeaders.HDR_MIME_VERSION, VERSION_1_0);
                    if (fileName != null) {
                        final ContentDisposition cd = new ContentDisposition(Part.ATTACHMENT);
                        cd.setFilenameParameter(fileName);
                        vcardPart.setHeader(
                            MessageHeaders.HDR_CONTENT_DISPOSITION,
                            MIMEMessageUtility.foldContentDisposition(cd.toString()));
                    }
                    /*
                     * Append body part
                     */
                    primaryMultipart.addBodyPart(vcardPart);
                    if (mail.isDraft()) {
                        mimeMessage.setHeader(MessageHeaders.HDR_X_OX_VCARD, "true");
                    }
                } catch (final OXException e) {
                    LOG.error(VCARD_ERROR, e);
                }
            }
            /*
             * Attach forwarded messages
             */
            // if (isAttachmentForward) {
            // if (primaryMultipart == null) {
            // primaryMultipart = new MimeMultipart();
            // }
            // final int count = mail.getEnclosedCount();
            // final MailMessage[] refMails = mail.getReferencedMails();
            // final StringBuilder sb = new StringBuilder(32);
            // final ByteArrayOutputStream out = new
            // UnsynchronizedByteArrayOutputStream();
            // for (final MailMessage refMail : refMails) {
            // out.reset();
            // sb.setLength(0);
            // refMail.writeTo(out);
            // addNestedMessage(primaryMultipart, new DataHandler(new
            // MessageDataSource(out.toByteArray(),
            // MIMETypes.MIME_MESSAGE_RFC822)), sb.append(
            // refMail.getSubject().replaceAll("\\p{Blank}+",
            // "_")).append(".eml").toString());
            // }
            // }
            /*
             * Finally set multipart
             */
            if (primaryMultipart != null) {
                mimeMessage.setContent(primaryMultipart);
            }
            return;
        }
        /*
         * Create a non-multipart message
         */
        if (mail.getContentType().isMimeType(MIMETypes.MIME_TEXT_ALL)) {
            final boolean isPlainText = mail.getContentType().isMimeType(MIMETypes.MIME_TEXT_PLAIN);
            if (mail.getContentType().getCharsetParameter() == null) {
                mail.getContentType().setCharsetParameter(charset);
            }
            if (primaryMultipart == null) {
                final String mailText;
                if (isPlainText) {
                    /*
                     * Convert HTML content to regular text (preserving links)
                     */
                    mailText =
                        performLineFolding(
                            htmlService.html2text(htmlService.getConformHTML(content, charset), true),
                            usm.getAutoLinebreak());
                    // mailText =
                    // performLineFolding(getConverter().convertWithQuotes
                    // ((String) mail.getContent()), false,
                    // usm.getAutoLinebreak());
                } else {
                    mailText = htmlService.getConformHTML(content, mail.getContentType().getCharsetParameter());
                }
                mimeMessage.setContent(mailText, mail.getContentType().toString());
                mimeMessage.setHeader(MessageHeaders.HDR_MIME_VERSION, VERSION_1_0);
                mimeMessage.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMEMessageUtility.foldContentType(mail.getContentType().toString()));
            } else {
                final MimeBodyPart msgBodyPart = new MimeBodyPart();
                msgBodyPart.setContent(mail.getContent(), mail.getContentType().toString());
                msgBodyPart.setHeader(MessageHeaders.HDR_MIME_VERSION, VERSION_1_0);
                msgBodyPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMEMessageUtility.foldContentType(mail.getContentType().toString()));
                primaryMultipart.addBodyPart(msgBodyPart);
            }
        } else {
            Multipart mp = null;
            if (primaryMultipart == null) {
                primaryMultipart = mp = new MimeMultipart();
            } else {
                mp = primaryMultipart;
            }
            final MimeBodyPart msgBodyPart = new MimeBodyPart();
            msgBodyPart.setText("", charset);
            final String disposition = msgBodyPart.getHeader(MessageHeaders.HDR_CONTENT_DISPOSITION, null);
            if (disposition == null) {
                msgBodyPart.setHeader(MessageHeaders.HDR_CONTENT_DISPOSITION, Part.INLINE);
            } else {
                final ContentDisposition contentDisposition = new ContentDisposition(disposition);
                contentDisposition.setDisposition(Part.INLINE);
                msgBodyPart.setHeader(
                    MessageHeaders.HDR_CONTENT_DISPOSITION,
                    MIMEMessageUtility.foldContentDisposition(contentDisposition.toString()));
            }
            mp.addBodyPart(msgBodyPart);
            addMessageBodyPart(mp, mail, true);
        }
        /*
         * if (hasNestedMessages) { if (primaryMultipart == null) { primaryMultipart = new MimeMultipart(); } message/rfc822 final int
         * nestedMsgSize = msgObj.getNestedMsgs().size(); final Iterator<JSONMessageObject> iter = msgObj.getNestedMsgs().iterator(); for
         * (int i = 0; i < nestedMsgSize; i++) { final JSONMessageObject nestedMsgObj = iter.next(); final MimeMessage nestedMsg = new
         * MimeMessage(mailSession); fillMessage(nestedMsgObj, nestedMsg, sendType); final MimeBodyPart msgBodyPart = new MimeBodyPart();
         * msgBodyPart.setContent(nestedMsg, MIME_MESSAGE_RFC822); primaryMultipart.addBodyPart(msgBodyPart); } }
         */
        /*
         * Finally set multipart
         */
        if (primaryMultipart != null) {
            mimeMessage.setContent(primaryMultipart);
        }
    }

    /**
     * Gets session user's VCard as a string.
     *
     * @param charset The charset to use for returned string
     * @return The session user's VCard as a string
     * @throws OXException If a mail error occurs
     */
    protected final String getUserVCard(final String charset) throws OXException {
        final User userObj = UserStorage.getStorageUser(session.getUserId(), ctx);
        Connection readCon = null;
        try {
            final OXContainerConverter converter = new OXContainerConverter(session, ctx);
            try {
                readCon = DBPool.pickup(ctx);
                Contact contactObj = null;
                try {
                    contactObj =
                        Contacts.getContactById(
                            userObj.getContactId(),
                            userObj.getId(),
                            userObj.getGroups(),
                            ctx,
                            UserConfigurationStorage.getInstance().getUserConfigurationSafe(session.getUserId(), ctx),
                            readCon);
                } catch (final OXException oxExc) {
                    throw new OXException(oxExc);
                } catch (final Exception e) {
                    throw MailExceptionCode.VERSIT_ERROR.create(e, e.getMessage());
                }
                final VersitObject versitObj = converter.convertContact(contactObj, "2.1");
                final ByteArrayOutputStream os = new UnsynchronizedByteArrayOutputStream();
                final VersitDefinition def = Versit.getDefinition(MIMETypes.MIME_TEXT_X_VCARD);
                final VersitDefinition.Writer w = def.getWriter(os, MailProperties.getInstance().getDefaultMimeCharset());
                def.write(w, versitObj);
                w.flush();
                os.flush();
                return new String(os.toByteArray(), charset);
            } finally {
                if (readCon != null) {
                    DBPool.closeReaderSilent(ctx, readCon);
                    readCon = null;
                }
                converter.close();
            }
        } catch (final ConverterException e) {
            throw MailExceptionCode.VERSIT_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Creates a "multipart/alternative" object.
     *
     * @param mail The source composed mail
     * @param mailBody The composed mail's HTML content
     * @param embeddedImages <code>true</code> if specified HTML content contains inline images (an appropriate "multipart/related" object
     *            is going to be created ); otherwise <code>false</code>.
     * @param images
     * @param textBodyPart The text body part
     * @return An appropriate "multipart/alternative" object.
     * @throws OXException If a mail error occurs
     * @throws MessagingException If a messaging error occurs
     */
    protected final Multipart createMultipartAlternative(final ComposedMailMessage mail, final String mailBody, final boolean embeddedImages, final Map<String, SourcedImage> images, final TextBodyMailPart textBodyPart) throws OXException, MessagingException {
        /*
         * Create an "alternative" multipart
         */
        final Multipart alternativeMultipart = new MimeMultipart(MP_ALTERNATIVE);
        /*
         * Define html content
         */
        final String charset = MailProperties.getInstance().getDefaultMimeCharset();
        final String htmlContent;
        if (embeddedImages) {
            /*
             * Create "related" multipart
             */
            final Multipart relatedMultipart;
            {
                final String[] arr = new String[1];
                relatedMultipart = createMultipartRelated(mail, mailBody, images, arr);
                htmlContent = arr[0];
            }
            /*
             * Add multipart/related as a body part to superior multipart
             */
            final BodyPart altBodyPart = new MimeBodyPart();
            altBodyPart.setContent(relatedMultipart);
            alternativeMultipart.addBodyPart(altBodyPart);
        } else {
            /*
             * Well-formed HTML
             */
            final String wellFormedHTMLContent = htmlService.getConformHTML(mailBody, charset);
            htmlContent = wellFormedHTMLContent;
            final BodyPart html = createHtmlBodyPart(wellFormedHTMLContent, charset);
            /*
             * Add HTML part to superior multipart
             */
            alternativeMultipart.addBodyPart(html);
        }
        /*
         * Define & prepend text content to first index position
         */
        final String plainText = textBodyPart.getPlainText();
        if (null == plainText) {
            alternativeMultipart.addBodyPart(createTextBodyPart(htmlContent, charset, true, true), 0);
        } else {
            alternativeMultipart.addBodyPart(createTextBodyPart(plainText, charset, true, false), 0);
        }
        return alternativeMultipart;
    }

    /**
     * Creates a "multipart/related" object. All inline images are going to be added to returned "multipart/related" object and
     * corresponding HTML content is altered to reference these images through "Content-Id".
     *
     * @param mail The source composed mail
     * @param mailBody The composed mail's HTML content
     * @param images The list of with-source images
     * @param htmlContent An array of {@link String} with length <code>1</code> serving as a container for altered HTML content
     * @return The created "multipart/related" object
     * @throws MessagingException If a messaging error occurs
     * @throws OXException If a mail error occurs
     */
    protected Multipart createMultipartRelated(final ComposedMailMessage mail, final String mailBody, final Map<String, SourcedImage> images, final String[] htmlContent) throws OXException, MessagingException {
        /*
         * Create "related" multipart
         */
        final Multipart relatedMultipart = new MimeMultipart(MP_RELATED);
        /*
         * Well-formed HTML
         */
        final String charset = MailProperties.getInstance().getDefaultMimeCharset();
        final String wellFormedHTMLContent = htmlService.getConformHTML(mailBody, charset);
        /*
         * Check for local images
         */
        htmlContent[0] = processReferencedLocalImages(wellFormedHTMLContent, relatedMultipart, this);
        /*
         * Process referenced local image files and insert returned html content as a new body part to first index
         */
        relatedMultipart.addBodyPart(createHtmlBodyPart(htmlContent[0], charset), 0);
        /*
         * Traverse Content-IDs occurring in original HTML content
         */
        final List<String> cidList = MIMEMessageUtility.getContentIDs(wellFormedHTMLContent);
        final StringBuilder tmp = new StringBuilder(32);
        NextImg: for (final String cid : cidList) {
            final BodyPart relatedImageBodyPart;
            final SourcedImage image = images.get(cid);
            if (null == image) {
                /*
                 * Get & remove inline image (to prevent being sent twice)
                 */
                final MailPart imgPart = getAndRemoveImageAttachment(cid, mail);
                if (imgPart == null) {
                    continue NextImg;
                }
                /*
                 * Create new body part from part's data handler
                 */
                relatedImageBodyPart = new MimeBodyPart();
                relatedImageBodyPart.setDataHandler(imgPart.getDataHandler());
                for (final Iterator<Map.Entry<String, String>> iter = imgPart.getHeadersIterator(); iter.hasNext();) {
                    final Map.Entry<String, String> e = iter.next();
                    relatedImageBodyPart.setHeader(e.getKey(), e.getValue());
                }

            } else {
                final DataSource dataSource;
                if ("base64".equalsIgnoreCase(image.getTransferEncoding())) {
                    dataSource = new MessageDataSource(Base64.decodeBase64(image.getData().getBytes(com.openexchange.java.Charsets.US_ASCII)), image.getContentType());
                } else {
                    /*
                     * Expect quoted-printable instead
                     */
                    try {
                        /*
                         * No need to specify a charset in String.getBytes(), quoted-printable is always ASCII
                         */
                        final byte[] bs = QuotedPrintableCodec.decodeQuotedPrintable(image.getData().getBytes());
                        dataSource = new MessageDataSource(bs, image.getContentType());
                    } catch (final DecoderException e) {
                        LOG.warn("Couldn't decode " + image.getTransferEncoding() + " image data.", e);
                        continue NextImg;
                    }
                }
                final MimeBodyPart imgBodyPart = new MimeBodyPart();
                imgBodyPart.setDataHandler(new DataHandler(dataSource));
                tmp.setLength(0);
                imgBodyPart.setContentID(tmp.append('<').append(cid).append('>').toString());
                final ContentDisposition contentDisposition = new ContentDisposition(Part.INLINE);
                imgBodyPart.setHeader(
                    MessageHeaders.HDR_CONTENT_DISPOSITION,
                    MIMEMessageUtility.foldContentDisposition(contentDisposition.toString()));
                final ContentType ct = new ContentType(image.getContentType());
                imgBodyPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMEMessageUtility.foldContentType(ct.toString()));
                relatedImageBodyPart = imgBodyPart;
            }
            /*
             * Add image to "related" multipart
             */
            relatedMultipart.addBodyPart(relatedImageBodyPart);
        }
        return relatedMultipart;
    }

    protected final void addMessageBodyPart(final Multipart mp, final MailPart part, final boolean inline) throws MessagingException, OXException, IOException {
        if (part.getContentType().startsWith(MIMETypes.MIME_MESSAGE_RFC822)) {
            // TODO: Works correctly?
            final StringBuilder sb = new StringBuilder(32);
            final ByteArrayOutputStream out = new UnsynchronizedByteArrayOutputStream(BUF_SIZE);
            final byte[] bbuf = new byte[BUF_SIZE];
            addNestedMessage(part, mp, sb, out, bbuf);
            return;
        }
        /*
         * A non-message attachment
         */
        final String fileName = part.getFileName();
        final ContentType ct = part.getContentType();
        if (ct.startsWith(MIMETypes.MIME_APPL_OCTET) && fileName != null) {
            /*
             * Try to determine MIME type
             */
            final String ct2 = MIMEType2ExtMap.getContentType(fileName);
            final int pos = ct2.indexOf('/');
            ct.setPrimaryType(ct2.substring(0, pos));
            ct.setSubType(ct2.substring(pos + 1));
        }
        final MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setDataHandler(part.getDataHandler());
        if (fileName != null && !ct.containsNameParameter()) {
            ct.setNameParameter(fileName);
        }
        messageBodyPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMEMessageUtility.foldContentType(ct.toString()));
        if (!inline) {
            /*
             * Force base64 encoding to keep data as it is
             */
            messageBodyPart.setHeader(MessageHeaders.HDR_CONTENT_TRANSFER_ENC, "base64");
        }
        /*
         * Disposition
         */
        final String disposition = messageBodyPart.getHeader(MessageHeaders.HDR_CONTENT_DISPOSITION, null);
        final ContentDisposition cd;
        if (disposition == null) {
            cd = new ContentDisposition(inline ? Part.INLINE : Part.ATTACHMENT);
        } else {
            cd = new ContentDisposition(disposition);
            cd.setDisposition(inline ? Part.INLINE : Part.ATTACHMENT);
        }
        if (fileName != null && !cd.containsFilenameParameter()) {
            cd.setFilenameParameter(fileName);
        }
        messageBodyPart.setHeader(MessageHeaders.HDR_CONTENT_DISPOSITION, MIMEMessageUtility.foldContentDisposition(cd.toString()));
        /*
         * Content-ID
         */
        if (part.getContentId() != null) {
            final String cid =
                part.getContentId().charAt(0) == '<' ? part.getContentId() : new StringBuilder(part.getContentId().length() + 2).append('<').append(
                    part.getContentId()).append('>').toString();
            messageBodyPart.setContentID(cid);
        }
        /*
         * Add to parental multipart
         */
        mp.addBodyPart(messageBodyPart);
    }

    protected void addNestedMessage(final MailPart mailPart, final Multipart primaryMultipart, final StringBuilder sb, final ByteArrayOutputStream out, final byte[] bbuf) throws OXException, IOException, MessagingException {
        final byte[] rfcBytes;
        {
            final InputStream in = mailPart.getInputStream();
            try {
                int len;
                while ((len = in.read(bbuf)) != -1) {
                    out.write(bbuf, 0, len);
                }
            } finally {
                in.close();
            }
            rfcBytes = out.toByteArray();
        }
        out.reset();
        final String fn;
        if (null == mailPart.getFileName()) {
            String subject =
                MIMEMessageUtility.checkNonAscii(new InternetHeaders(new UnsynchronizedByteArrayInputStream(rfcBytes)).getHeader(
                    MessageHeaders.HDR_SUBJECT,
                    null));
            if (null == subject || subject.length() == 0) {
                fn = sb.append(PREFIX_PART).append(EXT_EML).toString();
            } else {
                subject = MIMEMessageUtility.decodeMultiEncodedHeader(MIMEMessageUtility.unfold(subject));
                fn = sb.append(subject.replaceAll("\\p{Blank}+", "_")).append(EXT_EML).toString();
                sb.setLength(0);
            }
        } else {
            fn = mailPart.getFileName();
        }
        addNestedMessage(
            primaryMultipart,
            new DataHandler(new MessageDataSource(rfcBytes, MIMETypes.MIME_MESSAGE_RFC822)),
            fn,
            Part.INLINE.equalsIgnoreCase(mailPart.getContentDisposition().getDisposition()));
    }

    private final void addNestedMessage(final Multipart mp, final DataHandler dataHandler, final String filename, final boolean inline) throws MessagingException, OXException {
        /*
         * Create a body part for original message
         */
        final MimeBodyPart origMsgPart = new MimeBodyPart();
        /*
         * Set data handler
         */
        origMsgPart.setDataHandler(dataHandler);
        /*
         * Set content type
         */
        final ContentType ct = new ContentType(MIMETypes.MIME_MESSAGE_RFC822);
        if (null != filename) {
            ct.setNameParameter(filename);
        }
        origMsgPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMEMessageUtility.foldContentType(ct.toString()));
        /*
         * Set content disposition
         */
        final String disposition = origMsgPart.getHeader(MessageHeaders.HDR_CONTENT_DISPOSITION, null);
        final ContentDisposition cd;
        if (disposition == null) {
            cd = new ContentDisposition(inline ? Part.INLINE : Part.ATTACHMENT);
        } else {
            cd = new ContentDisposition(disposition);
            cd.setDisposition(inline ? Part.INLINE : Part.ATTACHMENT);
        }
        if (null != filename && !cd.containsFilenameParameter()) {
            cd.setFilenameParameter(filename);
        }
        origMsgPart.setHeader(MessageHeaders.HDR_CONTENT_DISPOSITION, MIMEMessageUtility.foldContentDisposition(cd.toString()));
        mp.addBodyPart(origMsgPart);
    }

    /*
     * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ +++++++++++++++++++++++++ HELPER METHODS +++++++++++++++++++++++++
     * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     */

    /**
     * Creates a body part of type <code>text/plain</code> from given HTML content
     *
     * @param content The content
     * @param charset The character encoding
     * @param appendHref <code>true</code> to append URLs contained in <i>href</i>s and <i>src</i>s; otherwise <code>false</code>
     * @param isHtml Whether provided content is HTML or not
     * @return A body part of type <code>text/plain</code> from given HTML content
     * @throws MessagingException If a messaging error occurs
     */
    protected final BodyPart createTextBodyPart(final String content, final String charset, final boolean appendHref, final boolean isHtml) throws MessagingException {
        /*
         * Convert html content to regular text. First: Create a body part for text content
         */
        final MimeBodyPart text = new MimeBodyPart();
        /*
         * Define text content
         */
        final String textContent;
        if (content == null || content.length() == 0) {
            textContent = "";
        } else if (isHtml) {
            textContent = performLineFolding(htmlService.html2text(content, appendHref), usm.getAutoLinebreak());
        } else {
            textContent = performLineFolding(content, usm.getAutoLinebreak());
        }
        text.setText(textContent, charset);
        // text.setText(performLineFolding(getConverter().convertWithQuotes(
        // htmlContent), false, usm.getAutoLinebreak()),
        // MailConfig.getDefaultMimeCharset());
        text.setHeader(MessageHeaders.HDR_MIME_VERSION, VERSION_1_0);
        text.setHeader(MessageHeaders.HDR_CONTENT_TYPE, PAT_TEXT_CT.replaceFirst(REPLACE_CS, charset));
        return text;
    }

    private static final String HTML_SPACE = "&#160;";

    /**
     * Creates a body part of type <code>text/html</code> from given HTML content
     *
     * @param wellFormedHTMLContent The well-formed HTML content
     * @param charset The charset
     * @return A body part of type <code>text/html</code> from given HTML content
     * @throws MessagingException If a messaging error occurs
     */
    protected final BodyPart createHtmlBodyPart(final String wellFormedHTMLContent, final String charset) throws MessagingException {
        final String contentType = PAT_HTML_CT.replaceFirst(REPLACE_CS, charset);
        final MimeBodyPart html = new MimeBodyPart();
        if (wellFormedHTMLContent == null || wellFormedHTMLContent.length() == 0) {
            html.setContent(htmlService.getConformHTML(HTML_SPACE, charset).replaceFirst(HTML_SPACE, ""), contentType);
        } else {
            html.setContent(wellFormedHTMLContent, contentType);
        }
        html.setHeader(MessageHeaders.HDR_MIME_VERSION, VERSION_1_0);
        html.setHeader(MessageHeaders.HDR_CONTENT_TYPE, contentType);
        return html;
    }

    /**
     * Processes referenced local images, inserts them as inlined html images and adds their binary data to parental instance of <code>
     * {@link Multipart}</code>.
     *
     * @param htmlContent The html content whose &lt;img&gt; tags must be replaced with real content IDs
     * @param mp The parental instance of <code>{@link Multipart}</code>
     * @param msgFiller The message filler
     * @return the replaced html content
     * @throws MessagingException If appending as body part fails
     * @throws OXException If a mail error occurs
     */
    protected final static String processReferencedLocalImages(final String htmlContent, final Multipart mp, final MIMEMessageFiller msgFiller) throws MessagingException, OXException {
        final Matcher m = MIMEMessageUtility.PATTERN_REF_IMG.matcher(htmlContent);
        final MatcherReplacer mr = new MatcherReplacer(m, htmlContent);
        final StringBuilder sb = new StringBuilder(htmlContent.length());
        if (m.find()) {
            msgFiller.uploadFileIDs = new HashSet<String>();
            final ManagedFileManagement mfm = ServerServiceRegistry.getInstance().getService(ManagedFileManagement.class);
            final ConversionService conversionService = ServerServiceRegistry.getInstance().getService(ConversionService.class);
            final Session session = msgFiller.session;
            final StringBuilder tmp = new StringBuilder(128);
            do {
                final String id = m.group(5);
                final ImageProvider imageProvider;
                if (mfm.contains(id)) {
                    try {
                        imageProvider = new ManagedFileImageProvider(mfm.getByID(id));
                    } catch (final OXException e) {
                        if (LOG.isWarnEnabled()) {
                            tmp.setLength(0);
                            LOG.warn(
                                tmp.append("Image with id \"").append(id).append("\" could not be loaded. Referenced image is skipped.").toString(),
                                e);
                        }
                        /*
                         * Anyway, replace image tag
                         */
                        tmp.setLength(0);
                        mr.appendLiteralReplacement(
                            sb,
                            m.group().replaceFirst(
                                "(?i)src=\"[^\"]*\"",
                                "src=\"cid:" + tmp.append(id).append('@').append("notfound").toString() + "\""));
                        continue;
                    }
                } else {
                    final ImageLocation imageLocation = ImageUtility.parseImageLocationFrom(m.group());
                    if (null == imageLocation) {
                        if (LOG.isWarnEnabled()) {
                            tmp.setLength(0);
                            LOG.warn(tmp.append("No image found with id \"").append(id).append("\". Referenced image is skipped.").toString());
                        }
                        /*
                         * Anyway, replace image tag
                         */
                        tmp.setLength(0);
                        mr.appendLiteralReplacement(
                            sb,
                            m.group().replaceFirst(
                                "(?i)src=\"[^\"]*\"",
                                "src=\"cid:" + tmp.append(id).append('@').append("notfound").toString() + "\""));
                        continue;
                    }
                    final ImageDataSource dataSource = (ImageDataSource) conversionService.getDataSource(imageLocation.getRegistrationName());
                    if (null == dataSource) {
                        if (LOG.isWarnEnabled()) {
                            tmp.setLength(0);
                            LOG.warn(tmp.append("No image data source found with id \"").append(imageLocation.getRegistrationName()).append("\". Referenced image is skipped.").toString());
                        }
                        /*
                         * Anyway, replace image tag
                         */
                        tmp.setLength(0);
                        mr.appendLiteralReplacement(
                            sb,
                            m.group().replaceFirst(
                                "(?i)src=\"[^\"]*\"",
                                "src=\"cid:" + tmp.append(id).append('@').append("notfound").toString() + "\""));
                        continue;
                    }
                    try {
                        imageProvider = new ImageDataImageProvider(dataSource, imageLocation, session);
                    } catch (final OXException e) {
                        if (e.isPrefix("MSG") && MailExceptionCode.IMAGE_ATTACHMENT_NOT_FOUND.getNumber() == e.getCode()) {
                            tmp.setLength(0);
                            mr.appendLiteralReplacement(
                                sb,
                                m.group().replaceFirst(
                                    "(?i)src=\"[^\"]*\"",
                                    "src=\"cid:" + tmp.append(id).append('@').append("notfound").toString() + "\""));
                            continue;
                        }
                        throw e;
                    }
                }
                final boolean appendBodyPart;
                if (msgFiller.uploadFileIDs.contains(id)) {
                    appendBodyPart = false;
                } else {
                    /*
                     * Remember id to avoid duplicate attachment and for later cleanup
                     */
                    msgFiller.uploadFileIDs.add(id);
                    appendBodyPart = true;
                }
                mr.appendLiteralReplacement(
                    sb,
                    m.group().replaceFirst(
                        "(?i)src=\"[^\"]*\"",
                        "src=\"cid:" + processLocalImage(imageProvider, id, appendBodyPart, tmp, mp) + "\""));
                // mr.appendLiteralReplacement(sb, IMG_PAT.replaceFirst("#1#", processLocalImage(imageProvider, id, appendBodyPart, tmp,
                // mp)));
            } while (m.find());
        }
        mr.appendTail(sb);
        return sb.toString();
    }

    private static final Pattern PATTERN_DASHES = Pattern.compile("-+");

    /**
     * Processes a local image and returns its content id
     *
     * @param imageProvider The uploaded file
     * @param id uploaded file's ID
     * @param appendBodyPart
     * @param tmp An instance of {@link StringBuilder}
     * @param mp The parental instance of {@link Multipart}
     * @return the content id
     * @throws MessagingException If appending as body part fails
     * @throws OXException If a mail error occurs
     */
    private final static String processLocalImage(final ImageProvider imageProvider, final String id, final boolean appendBodyPart, final StringBuilder tmp, final Multipart mp) throws MessagingException, OXException {
        /*
         * Determine filename
         */
        String fileName = imageProvider.getFileName();
        if (null == fileName) {
            /*
             * Generate dummy file name
             */
            final List<String> exts = MIMEType2ExtMap.getFileExtensions(imageProvider.getContentType().toLowerCase(Locale.ENGLISH));
            final StringBuilder sb = new StringBuilder("image.");
            if (exts == null) {
                sb.append("dat");
            } else {
                sb.append(exts.get(0));
            }
            fileName = sb.toString();
        } else {
            /*
             * Encode image's file name for being mail-safe
             */
            try {
                fileName = MimeUtility.encodeText(fileName, MailProperties.getInstance().getDefaultMimeCharset(), "Q");
            } catch (final UnsupportedEncodingException e) {
                fileName = imageProvider.getFileName();
            }
        }
        /*
         * ... and cid
         */
        tmp.setLength(0);
        tmp.append(PATTERN_DASHES.matcher(id).replaceAll("")).append('@').append(Version.NAME);
        final String cid = tmp.toString();
        if (appendBodyPart) {
            /*
             * Append body part
             */
            final MimeBodyPart imgBodyPart = new MimeBodyPart();
            imgBodyPart.setDataHandler(new DataHandler(imageProvider.getDataSource()));
            tmp.setLength(0);
            imgBodyPart.setContentID(tmp.append('<').append(cid).append('>').toString());
            final ContentDisposition contentDisposition = new ContentDisposition(Part.INLINE);
            if (fileName != null) {
                contentDisposition.setFilenameParameter(fileName);
            }
            imgBodyPart.setHeader(
                MessageHeaders.HDR_CONTENT_DISPOSITION,
                MIMEMessageUtility.foldContentDisposition(contentDisposition.toString()));
            final ContentType ct = new ContentType(imageProvider.getContentType());
            if (fileName != null && !ct.containsNameParameter()) {
                ct.setNameParameter(fileName);
            }
            imgBodyPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMEMessageUtility.foldContentType(ct.toString()));
            mp.addBodyPart(imgBodyPart);
        }
        return cid;
    }

    /**
     * Gets and removes the image attachment from specified mail whose <code>Content-Id</code> matches given <code>cid</code> argument
     *
     * @param cid The <code>Content-Id</code> of the image attachment
     * @param mail The mail containing the image attachment
     * @return The removed image attachment
     * @throws OXException If a mail error occurs
     */
    protected final static MailPart getAndRemoveImageAttachment(final String cid, final ComposedMailMessage mail) throws OXException {
        final int size = mail.getEnclosedCount();
        for (int i = 0; i < size; i++) {
            final MailPart enclosedPart = mail.getEnclosedMailPart(i);
            if (enclosedPart.containsContentId() && MIMEMessageUtility.equalsCID(cid, enclosedPart.getContentId())) {
                return mail.removeEnclosedPart(i);
            }
        }
        return null;
    }

    private static final boolean hasOnlyReferencedMailAttachments(final ComposedMailMessage mail, final int size) throws OXException {
        for (int i = 0; i < size; i++) {
            final MailPart part = mail.getEnclosedMailPart(i);
            if (!ComposedPartType.REFERENCE.equals(((ComposedMailPart) part).getType()) || !((ReferencedMailPart) part).isMail()) {
                return false;
            }
        }
        return true;
    }

    private static interface ImageProvider {

        public String getFileName();

        public DataSource getDataSource() throws OXException;

        public String getContentType();
    } // End of ImageProvider

    private static class ManagedFileImageProvider implements ImageProvider {

        private final ManagedFile managedFile;

        public ManagedFileImageProvider(final ManagedFile managedFile) {
            super();
            this.managedFile = managedFile;
        }

        @Override
        public String getContentType() {
            return managedFile.getContentType();
        }

        @Override
        public DataSource getDataSource() throws OXException {
            return new FileDataSource(managedFile.getFile());
        }

        @Override
        public String getFileName() {
            return managedFile.getFileName();
        }
    } // End of ManagedFileImageProvider

    private static class ImageDataImageProvider implements ImageProvider {

        private final Data<InputStream> data;

        private final String contentType;

        private final String fileName;

        public ImageDataImageProvider(final ImageDataSource imageData, final ImageLocation imageLocation, final Session session) throws OXException {
            super();
            this.data = imageData.getData(InputStream.class, imageData.generateDataArgumentsFrom(imageLocation), session);
            final DataProperties dataProperties = data.getDataProperties();
            contentType = dataProperties.get(DataProperties.PROPERTY_CONTENT_TYPE);
            fileName = dataProperties.get(DataProperties.PROPERTY_NAME);
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public DataSource getDataSource() throws OXException {
            try {
                return new MessageDataSource(data.getData(), contentType);
            } catch (final IOException e) {
                throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
            }
        }

        @Override
        public String getFileName() {
            return fileName;
        }
    } // End of ImageDataImageProvider

}
