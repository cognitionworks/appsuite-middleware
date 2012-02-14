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

package com.openexchange.imap;

import static com.openexchange.mail.MailServletInterface.mailInterfaceMonitor;
import static com.openexchange.mail.dataobjects.MailFolder.DEFAULT_FOLDER_ID;
import static com.openexchange.mail.mime.utils.MIMEMessageUtility.fold;
import static com.openexchange.mail.mime.utils.MIMEStorageUtility.getFetchProfile;
import gnu.trove.list.TIntList;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.mail.FetchProfile;
import javax.mail.FetchProfile.Item;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.StoreClosedException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.ParameterList;
import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import com.openexchange.exception.OXException;
import com.openexchange.imap.AllFetch.LowCostItem;
import com.openexchange.imap.cache.ListLsubCache;
import com.openexchange.imap.cache.ListLsubEntry;
import com.openexchange.imap.cache.RightsCache;
import com.openexchange.imap.cache.UserFlagsCache;
import com.openexchange.imap.command.AbstractIMAPCommand;
import com.openexchange.imap.command.BodyFetchIMAPCommand;
import com.openexchange.imap.command.BodystructureFetchIMAPCommand;
import com.openexchange.imap.command.CopyIMAPCommand;
import com.openexchange.imap.command.FetchIMAPCommand;
import com.openexchange.imap.command.FetchIMAPCommand.FetchProfileModifier;
import com.openexchange.imap.command.FlagsIMAPCommand;
import com.openexchange.imap.command.MoveIMAPCommand;
import com.openexchange.imap.command.NewFetchIMAPCommand;
import com.openexchange.imap.command.SimpleFetchIMAPCommand;
import com.openexchange.imap.config.IIMAPProperties;
import com.openexchange.imap.search.IMAPSearch;
import com.openexchange.imap.services.IMAPServiceRegistry;
import com.openexchange.imap.sort.IMAPSort;
import com.openexchange.imap.threadsort.ThreadSortNode;
import com.openexchange.imap.threadsort.ThreadSortUtil;
import com.openexchange.imap.util.IMAPSessionStorageAccess;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;
import com.openexchange.java.UnsynchronizedByteArrayInputStream;
import com.openexchange.mail.IndexRange;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailField;
import com.openexchange.mail.MailFields;
import com.openexchange.mail.MailPath;
import com.openexchange.mail.MailSortField;
import com.openexchange.mail.OrderDirection;
import com.openexchange.mail.api.IMailMessageStorageBatch;
import com.openexchange.mail.api.IMailMessageStorageExt;
import com.openexchange.mail.api.ISimplifiedThreadStructure;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.IDMailMessage;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.ThreadSortMailMessage;
import com.openexchange.mail.dataobjects.compose.ComposeType;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.mime.ExtendedMimeMessage;
import com.openexchange.mail.mime.MIMEMailException;
import com.openexchange.mail.mime.MIMEMailExceptionCode;
import com.openexchange.mail.mime.ManagedMimeMessage;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.converters.MIMEMessageConverter;
import com.openexchange.mail.mime.filler.MIMEMessageFiller;
import com.openexchange.mail.search.SearchTerm;
import com.openexchange.mail.text.TextFinder;
import com.openexchange.mail.utils.MailMessageComparator;
import com.openexchange.mail.utils.MessageUtility;
import com.openexchange.mail.uuencode.UUEncodedMultiPart;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.session.Session;
import com.openexchange.spamhandler.SpamHandlerRegistry;
import com.openexchange.textxtraction.TextXtractService;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.UserService;
import com.sun.mail.iap.BadCommandException;
import com.sun.mail.iap.CommandFailedException;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.AppendUID;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.Rights;
import com.sun.mail.imap.protocol.BODYSTRUCTURE;

/**
 * {@link IMAPMessageStorage} - The IMAP implementation of message storage.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class IMAPMessageStorage extends IMAPFolderWorker implements IMailMessageStorageExt, IMailMessageStorageBatch, ISimplifiedThreadStructure {

    private static final org.apache.commons.logging.Log LOG =
        com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(IMAPMessageStorage.class));

    private static final boolean DEBUG = LOG.isDebugEnabled();

    /*-
     * Flag constants
     */

    /**
     * This message is a draft. This flag is set by clients to indicate that the message is a draft message.
     */
    private static final Flag DRAFT = Flags.Flag.DRAFT;

    /**
     * This message is marked deleted. Clients set this flag to mark a message as deleted. The expunge operation on a folder removes all
     * messages in that folder that are marked for deletion.
     */
    private static final Flag DELETED = Flags.Flag.DELETED;

    /**
     * The Flags object initialized with the \Draft system flag.
     */
    private static final Flags FLAGS_DRAFT = new Flags(DRAFT);

    /**
     * The Flags object initialized with the \Deleted system flag.
     */
    private static final Flags FLAGS_DELETED = new Flags(DELETED);

    /*-
     * String constants
     */

    private static final String STR_MSEC = "msec";

    private static final boolean LOOK_UP_INBOX_ONLY = true;

    /*-
     * Members
     */

    private MailAccount mailAccount;

    private Locale locale;

    private IIMAPProperties imapProperties;

    private final IMAPFolderStorage imapFolderStorage;

    /**
     * Initializes a new {@link IMAPMessageStorage}.
     * 
     * @param imapStore The IMAP store
     * @param imapAccess The IMAP access
     * @param session The session providing needed user data
     * @throws OXException If initialization fails
     */
    public IMAPMessageStorage(final AccessedIMAPStore imapStore, final IMAPAccess imapAccess, final Session session) throws OXException {
        super(imapStore, imapAccess, session);
        imapFolderStorage = imapAccess.getFolderStorage();
    }

    private MailAccount getMailAccount() throws OXException {
        if (mailAccount == null) {
            try {
                final MailAccountStorageService storageService = IMAPServiceRegistry.getService(MailAccountStorageService.class, true);
                mailAccount = storageService.getMailAccount(accountId, session.getUserId(), session.getContextId());
            } catch (final RuntimeException e) {
                throw handleRuntimeException(e);
            }
        }
        return mailAccount;
    }

    private Locale getLocale() throws OXException {
        if (locale == null) {
            try {
                if (session instanceof ServerSession) {
                    locale = ((ServerSession) session).getUser().getLocale();
                } else {
                    final UserService userService = IMAPServiceRegistry.getService(UserService.class, true);
                    locale = userService.getUser(session.getUserId(), ctx).getLocale();
                }
            } catch (final RuntimeException e) {
                throw handleRuntimeException(e);
            }
        }
        return locale;
    }

    private IIMAPProperties getIMAPProperties() {
        if (null == imapProperties) {
            imapProperties = imapConfig.getIMAPProperties();
        }
        return imapProperties;
    }

    @Override
    public MailMessage[] getMessages(final String fullName, final String[] mailIds, final MailField[] mailFields, final String[] headerNames) throws OXException {
        if ((mailIds == null) || (mailIds.length == 0)) {
            return EMPTY_RETVAL;
        }
        return getMessagesInternal(fullName, uids2longs(mailIds), mailFields, headerNames);
    }

    private static String extractPlainText(final String content, final String optMimeType) throws OXException {
        final TextXtractService textXtractService = IMAPServiceRegistry.getService(TextXtractService.class);
        return textXtractService.extractFrom(new UnsynchronizedByteArrayInputStream(content.getBytes(Charsets.UTF_8)), optMimeType);
    }

    @Override
    public String[] getPrimaryContentsLong(final String fullName, final long[] mailIds) throws OXException {
        if (!imapConfig.getImapCapabilities().hasIMAP4rev1()) {
            return super.getPrimaryContentsLong(fullName, mailIds);
        }
        try {
            imapFolder = setAndOpenFolder(imapFolder, fullName, Folder.READ_ONLY);
            final BODYSTRUCTURE[] bodystructures = new BodystructureFetchIMAPCommand(imapFolder, mailIds).doCommand();
            final String[] retval = new String[mailIds.length];

            for (int i = 0; i < bodystructures.length; i++) {
                final BODYSTRUCTURE bodystructure = bodystructures[i];
                if (null != bodystructure) {
                    try {
                        retval[i] = handleBODYSTRUCTURE(fullName, mailIds[i], bodystructure, null, 1, new boolean[1]);
                    } catch (final Exception e) {
                        if (DEBUG) {
                            LOG.debug("Ignoring failed handling of BODYSTRUCTURE item: " + e.getMessage(), e);
                        }
                        retval[i] = null;
                    }
                }
            }
            return retval;
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private static final Whitelist WHITELIST = Whitelist.relaxed();

    // private static final Pattern PATTERN_CRLF = Pattern.compile("(\r?\n)+");

    private String handleBODYSTRUCTURE(final String fullName, final long mailId, final BODYSTRUCTURE bodystructure, final String prefix, final int partCount, final boolean[] mpDetected) throws OXException {
        try {
            final String type = bodystructure.type.toLowerCase(Locale.ENGLISH);
            if ("text".equals(type)) {
                final String sequenceId = getSequenceId(prefix, partCount);
                String content;
                {
                    final byte[] bytes = new BodyFetchIMAPCommand(imapFolder, mailId, sequenceId, true).doCommand();
                    final ParameterList cParams = bodystructure.cParams;
                    content = readContent(bytes, null == cParams ? null : cParams.get("charset"), bodystructure.encoding);
                }
                final String subtype = bodystructure.subtype.toLowerCase(Locale.ENGLISH);
                if ("plain".equals(subtype)) {
                    if (UUEncodedMultiPart.isUUEncoded(content)) {
                        final UUEncodedMultiPart uuencodedMP = new UUEncodedMultiPart(content);
                        if (uuencodedMP.isUUEncoded()) {
                            content = uuencodedMP.getCleanText();
                        }
                    }
                    return content;
                }
                if (subtype.startsWith("htm")) {
                    return new Renderer(new Segment(new Source(content), 0, content.length())).setMaxLineLength(9999).setIncludeHyperlinkURLs(false).toString();
                    // content = PATTERN_CRLF.matcher(content).replaceAll("");// .replaceAll("(  )+", "");
                }
                try {
                    return extractPlainText(content, new StringBuilder(type).append('/').append(subtype).toString());
                } catch (final OXException e) {
                    if (!subtype.startsWith("htm")) {
                        final StringBuilder sb =
                            new StringBuilder("Failed extracting plain text from \"text/").append(subtype).append("\" part:\n");
                        sb.append(" context=").append(session.getContextId());
                        sb.append(", user=").append(session.getUserId());
                        sb.append(", account=").append(accountId);
                        sb.append(", full-name=").append(fullName);
                        sb.append(", uid=").append(mailId);
                        sb.append(", sequence-id=").append(sequenceId);
                        LOG.warn(sb.toString());
                        throw e;
                    }
                    /*
                     * Retry with sanitized HTML content
                     */
                    return extractPlainText(Jsoup.clean(content, WHITELIST), new StringBuilder(type).append('/').append(subtype).toString());
                }
            }
            if ("multipart".equals(type)) {
                final String mpId = null == prefix && !mpDetected[0] ? "" : getSequenceId(prefix, partCount);
                final String mpPrefix;
                if (mpDetected[0]) {
                    mpPrefix = mpId;
                } else {
                    mpPrefix = prefix;
                    mpDetected[0] = true;
                }
                final BODYSTRUCTURE[] bodies = bodystructure.bodies;
                final String subtype = bodystructure.subtype.toLowerCase(Locale.ENGLISH);
                final int count = bodies.length;
                if ("alternative".equals(subtype)) {
                    /*
                     * Prefer HTML text over plain text
                     */
                    String text = null;
                    for (int i = 0; i < count; i++) {
                        final BODYSTRUCTURE bp = bodies[i];
                        final String bpType = bp.type.toLowerCase(Locale.ENGLISH);
                        final String bpSubtype = bp.subtype.toLowerCase(Locale.ENGLISH);
                        if ("text".equals(bpType) && "plain".equals(bpSubtype)) {
                            if (text == null) {
                                text = handleBODYSTRUCTURE(fullName, mailId, bp, mpPrefix, i + 1, mpDetected);
                            }
                            continue;
                        } else if ("text".equals(bpType) && bpSubtype.startsWith("htm")) {
                            final String s = handleBODYSTRUCTURE(fullName, mailId, bp, mpPrefix, i + 1, mpDetected);
                            if (s != null) {
                                return s;
                            }
                        } else if ("multipart".equals(bpType)) {
                            final String s = handleBODYSTRUCTURE(fullName, mailId, bp, mpPrefix, i + 1, mpDetected);
                            if (s != null) {
                                return s;
                            }
                        }
                    }
                    return text;
                }
                /*
                 * A regular multipart
                 */
                for (int i = 0; i < count; i++) {
                    final String s = handleBODYSTRUCTURE(fullName, mailId, bodies[i], mpPrefix, i + 1, mpDetected);
                    if (s != null) {
                        return s;
                    }
                }
            }
            final String subtype = bodystructure.subtype.toLowerCase(Locale.ENGLISH);
            if ("application".equals(type) && (subtype.startsWith("application/ms-tnef") || subtype.startsWith("application/vnd.ms-tnef"))) {
                final String sequenceId = getSequenceId(prefix, partCount);
                final byte[] bytes = new BodyFetchIMAPCommand(imapFolder, mailId, sequenceId, true).doCommand();
                return new TextFinder().handleTNEFStream(Streams.newByteArrayInputStream(bytes));
            }
            return null;
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final IOException e) {
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        }
    }

    private static String readContent(final byte[] bytes, final String charset, final String encoding) throws IOException {
        if (null == encoding) {
            return MessageUtility.readStream(Streams.newByteArrayInputStream(bytes), charset);
        }
        InputStream in;
        try {
            in = MimeUtility.decode(Streams.newByteArrayInputStream(bytes), encoding);
        } catch (final MessagingException e) {
            in = Streams.newByteArrayInputStream(bytes);
        }
        return MessageUtility.readStream(in, charset);
    }

    /**
     * Composes part's sequence ID from given prefix and part's count
     * 
     * @param prefix The prefix (may be <code>null</code>)
     * @param partCount The part count
     * @return The sequence ID
     */
    private static String getSequenceId(final String prefix, final int partCount) {
        if (prefix == null) {
            return String.valueOf(partCount);
        }
        return new StringBuilder(prefix).append('.').append(partCount).toString();
    }

    @Override
    public MailMessage[] getMessagesLong(final String fullName, final long[] mailIds, final MailField[] mailFields) throws OXException {
        if ((mailIds == null) || (mailIds.length == 0)) {
            return EMPTY_RETVAL;
        }
        return getMessagesInternal(fullName, mailIds, mailFields, null);
    }

    private MailMessage[] getMessagesInternal(final String fullName, final long[] uids, final MailField[] mailFields, final String[] headerNames) throws OXException {
        final MailFields fieldSet = new MailFields(mailFields);
        /*
         * Check for field FULL
         */
        if (fieldSet.contains(MailField.FULL) || fieldSet.contains(MailField.BODY)) {
            final MailMessage[] mails = new MailMessage[uids.length];
            for (int j = 0; j < mails.length; j++) {
                mails[j] = getMessageLong(fullName, uids[j], false);
            }
            return mails;
        }
        /*
         * Get messages with given fields filled
         */
        try {
            imapFolder = setAndOpenFolder(imapFolder, fullName, Folder.READ_ONLY);
            /*
             * Fetch desired messages by given UIDs. Turn UIDs to corresponding sequence numbers to maintain order cause some IMAP servers
             * ignore the order of UIDs provided in a "UID FETCH" command.
             */
            final MailMessage[] messages;
            final MailField[] fields = fieldSet.toArray();
            if (imapConfig.asMap().containsKey("UIDPLUS")) {
                final TLongObjectHashMap<MailMessage> fetchedMsgs =
                    fetchValidWithFallbackFor(
                        uids,
                        uids.length,
                        getFetchProfile(fields, headerNames, null, null, getIMAPProperties().isFastFetch()),
                        imapConfig.getImapCapabilities().hasIMAP4rev1(),
                        false);
                /*
                 * Fill array
                 */
                messages = new MailMessage[uids.length];
                for (int i = 0; i < uids.length; i++) {
                    messages[i] = fetchedMsgs.get(uids[i]);
                }
            } else {
                final TLongIntMap seqNumsMap = IMAPCommandsCollection.uids2SeqNumsMap(imapFolder, uids);
                final TLongObjectMap<MailMessage> fetchedMsgs =
                    fetchValidWithFallbackFor(
                        seqNumsMap.values(),
                        seqNumsMap.size(),
                        getFetchProfile(fields, headerNames, null, null, getIMAPProperties().isFastFetch()),
                        imapConfig.getImapCapabilities().hasIMAP4rev1(),
                        true);
                /*
                 * Fill array
                 */
                messages = new MailMessage[uids.length];
                for (int i = 0; i < uids.length; i++) {
                    messages[i] = fetchedMsgs.get(seqNumsMap.get(uids[i]));
                }
            }
            /*
             * Check field existence
             */
            MIMEMessageConverter.checkFieldExistence(messages, mailFields);
            if (fieldSet.contains(MailField.ACCOUNT_NAME) || fieldSet.contains(MailField.FULL)) {
                return setAccountInfo(messages);
            }
            return messages;
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private TLongObjectHashMap<MailMessage> fetchValidWithFallbackFor(final Object array, final int len, final FetchProfile fetchProfile, final boolean isRev1, final boolean seqnum) throws OXException {
        final String key = new StringBuilder(16).append(accountId).append(".imap.fetch.modifier").toString();
        final FetchProfile fp = fetchProfile;
        int retry = 0;
        while (true) {
            try {
                final FetchProfileModifier modifier = (FetchProfileModifier) session.getParameter(key);
                if (null == modifier) {
                    // session.setParameter(key, FetchIMAPCommand.DEFAULT_PROFILE_MODIFIER);
                    return fetchValidFor(array, len, fp, isRev1, seqnum, false);
                }
                return fetchValidFor(array, len, modifier.modify(fp), isRev1, seqnum, modifier.byContentTypeHeader());
            } catch (final FolderClosedException e) {
                throw MIMEMailException.handleMessagingException(e, imapConfig, session);
            } catch (final StoreClosedException e) {
                throw MIMEMailException.handleMessagingException(e, imapConfig, session);
            } catch (final MessagingException e) {
                final Exception nextException = e.getNextException();
                if ((nextException instanceof BadCommandException) || (nextException instanceof CommandFailedException)) {
                    if (DEBUG) {
                        final StringBuilder sb = new StringBuilder(128).append("Fetch with fetch profile failed: ");
                        for (final Item item : fetchProfile.getItems()) {
                            sb.append(item.getClass().getSimpleName()).append(',');
                        }
                        for (final String name : fetchProfile.getHeaderNames()) {
                            sb.append(name).append(',');
                        }
                        sb.deleteCharAt(sb.length() - 1);
                        LOG.debug(sb.toString(), e);
                    }
                    if (0 == retry) {
                        session.setParameter(key, FetchIMAPCommand.NO_BODYSTRUCTURE_PROFILE_MODIFIER);
                        retry++;
                    } else if (1 == retry) {
                        session.setParameter(key, FetchIMAPCommand.HEADERLESS_PROFILE_MODIFIER);
                        retry++;
                    } else {
                        throw MIMEMailException.handleMessagingException(e, imapConfig, session);
                    }
                } else {
                    throw MIMEMailException.handleMessagingException(e, imapConfig, session);
                }
            } catch (final ArrayIndexOutOfBoundsException e) {
                /*
                 * May occur while parsing invalid BODYSTRUCTURE response
                 */
                if (DEBUG) {
                    final StringBuilder sb = new StringBuilder(128).append("Fetch with fetch profile failed: ");
                    for (final Item item : fetchProfile.getItems()) {
                        sb.append(item.getClass().getSimpleName()).append(',');
                    }
                    for (final String name : fetchProfile.getHeaderNames()) {
                        sb.append(name).append(',');
                    }
                    sb.deleteCharAt(sb.length() - 1);
                    LOG.debug(sb.toString(), e);
                }
                if (0 == retry) {
                    session.setParameter(key, FetchIMAPCommand.NO_BODYSTRUCTURE_PROFILE_MODIFIER);
                    retry++;
                } else if (1 == retry) {
                    session.setParameter(key, FetchIMAPCommand.HEADERLESS_PROFILE_MODIFIER);
                    retry++;
                } else {
                    throw handleRuntimeException(e);
                }
            } catch (final RuntimeException e) {
                throw handleRuntimeException(e);
            }
        }
    }

    private TLongObjectHashMap<MailMessage> fetchValidFor(final Object array, final int len, final FetchProfile fetchProfile, final boolean isRev1, final boolean seqnum, final boolean byContentType) throws MessagingException, OXException {
        final TLongObjectHashMap<MailMessage> map = new TLongObjectHashMap<MailMessage>(len);
        // final MailMessage[] tmp = new NewFetchIMAPCommand(imapFolder, getSeparator(imapFolder), isRev1, array, fetchProfile, false,
        // false, false).setDetermineAttachmentByHeader(byContentType).doCommand();
        final NewFetchIMAPCommand command;
        if (array instanceof long[]) {
            command =
                new NewFetchIMAPCommand(imapFolder, getSeparator(imapFolder), isRev1, (long[]) array, fetchProfile).setDetermineAttachmentByHeader(byContentType);
        } else {
            command =
                new NewFetchIMAPCommand(imapFolder, getSeparator(imapFolder), isRev1, (int[]) array, fetchProfile).setDetermineAttachmentByHeader(byContentType);
        }
        final long start = System.currentTimeMillis();
        final MailMessage[] tmp = command.doCommand();
        final long time = System.currentTimeMillis() - start;
        mailInterfaceMonitor.addUseTime(time);
        if (DEBUG) {
            LOG.debug(new StringBuilder(128).append("IMAP fetch for ").append(len).append(" messages took ").append(time).append(STR_MSEC).toString());
        }
        for (final MailMessage mailMessage : tmp) {
            final IDMailMessage idmm = (IDMailMessage) mailMessage;
            if (null != idmm) {
                map.put(seqnum ? idmm.getSeqnum() : idmm.getUid(), idmm);
            }
        }
        return map;
    }

    @Override
    public MailMessage[] getMessagesByMessageID(final String... messageIDs) throws OXException {
        try {
            final int length = messageIDs.length;
            int count = 0;
            final MailMessage[] retval = new MailMessage[length];
            imapFolder = setAndOpenFolder(imapFolder, "INBOX", Folder.READ_ONLY);
            final long[] uids = IMAPCommandsCollection.messageId2UID(imapFolder, messageIDs);
            for (int i = 0; i < uids.length; i++) {
                final long uid = uids[i];
                if (uid != -1) {
                    retval[i] = new IDMailMessage(String.valueOf(uid), "INBOX");
                    count++;
                }
            }
            if (count == length || LOOK_UP_INBOX_ONLY) {
                return retval;
            }
            /*
             * Look-up other folders
             */
            recursiveMessageIDLookUp((IMAPFolder) imapStore.getDefaultFolder(), messageIDs, retval, count);
            return retval;
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private int recursiveMessageIDLookUp(final IMAPFolder parentFolder, final String[] messageIDs, final MailMessage[] retval, final int countArg) throws OXException, MessagingException {
        int count = countArg;
        final Folder[] folders = parentFolder.list();
        for (int i = 0; count >= 0 && i < folders.length; i++) {
            final String fullName = folders[i].getFullName();
            final IMAPFolder imapFolder = setAndOpenFolder(fullName, Folder.READ_ONLY);
            final long[] uids = IMAPCommandsCollection.messageId2UID(imapFolder, messageIDs);
            for (int k = 0; k < uids.length; k++) {
                final long uid = uids[k];
                if (uid != -1) {
                    retval[k] = new IDMailMessage(fullName, Long.toString(uid));
                    count++;
                }
            }
            if (count == messageIDs.length) {
                return -1;
            }
            count = recursiveMessageIDLookUp(imapFolder, messageIDs, retval, count);
        }
        return count;
    }

    @Override
    public MailMessage getMessageLong(final String fullName, final long msgUID, final boolean markSeen) throws OXException {
        try {
            final int desiredMode = markSeen ? Folder.READ_WRITE : Folder.READ_ONLY;
            imapFolder = setAndOpenFolder(imapFolder, fullName, desiredMode);
            final IMAPMessage msg;
            {
                final long start = System.currentTimeMillis();
                msg = (IMAPMessage) imapFolder.getMessageByUID(msgUID);
                mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
            }
            if (msg == null) {
                // throw new OXException(OXException.Code.MAIL_NOT_FOUND,
                // String.valueOf(msgUID), imapFolder
                // .toString());
                return null;
            }
            msg.setPeek(!markSeen);
            final MailMessage mail;
            try {
                mail = MIMEMessageConverter.convertMessage(msg, false);
                mail.setFolder(fullName);
                mail.setMailId(Long.toString(msgUID));
                mail.setUnreadMessages(IMAPCommandsCollection.getUnread(imapFolder));
            } catch (final OXException e) {
                if (MIMEMailExceptionCode.MESSAGE_REMOVED.getNumber() == e.getCode() && e.isPrefix("MSG")) {
                    /*
                     * Obviously message was removed in the meantime
                     */
                    return null;
                }
                /*
                 * Check for generic messaging error
                 */
                if (MIMEMailExceptionCode.MESSAGING_ERROR.getNumber() == e.getCode() && e.isPrefix("MSG")) {
                    /*-
                     * Detected generic messaging error. This most likely hints to a severe JavaMail problem.
                     *
                     * Perform some debug logs for traceability...
                     */
                    if (DEBUG) {
                        final StringBuilder sb = new StringBuilder(128);
                        sb.append("Generic messaging error occurred for mail \"").append(msgUID).append("\" in folder \"");
                        sb.append(fullName).append("\" with login \"").append(imapConfig.getLogin()).append("\" on server \"");
                        sb.append(imapConfig.getServer()).append("\" (user=").append(session.getUserId());
                        sb.append(", context=").append(session.getContextId()).append("): ").append(e.getMessage());
                        LOG.debug(sb.toString(), e);
                    }
                }
                throw e;
            }
            if (!mail.isSeen() && markSeen) {
                mail.setPrevSeen(false);
                if (imapConfig.isSupportsACLs()) {
                    try {
                        if (aclExtension.canKeepSeen(RightsCache.getCachedRights(imapFolder, true, session, accountId))) {
                            /*
                             * User has \KEEP_SEEN right: Switch \Seen flag
                             */
                            setSeenFlag(fullName, mail, msg);
                        }
                    } catch (final MessagingException e) {
                        imapFolderStorage.removeFromCache(fullName);
                        if (LOG.isWarnEnabled()) {
                            LOG.warn(
                                new StringBuilder("/SEEN flag could not be set on message #").append(mail.getMailId()).append(" in folder ").append(
                                    mail.getFolder()).toString(),
                                e);
                        }
                    }
                } else {
                    setSeenFlag(fullName, mail, msg);
                }
            }
            return setAccountInfo(mail);
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private void setSeenFlag(final String fullName, final MailMessage mail, final IMAPMessage msg) {
        try {
            msg.setFlags(FLAGS_SEEN, true);
            mail.setFlag(MailMessage.FLAG_SEEN, true);
            final int cur = mail.getUnreadMessages();
            mail.setUnreadMessages(cur <= 0 ? 0 : cur - 1);
            imapFolderStorage.decrementUnreadMessageCount(fullName);
        } catch (final Exception e) {
            imapFolderStorage.removeFromCache(fullName);
            if (LOG.isWarnEnabled()) {
                LOG.warn(
                    new StringBuilder("/SEEN flag could not be set on message #").append(mail.getMailId()).append(" in folder ").append(
                        mail.getFolder()).toString(),
                    e);
            }
        }
    }

    @Override
    public MailMessage[] searchMessages(final String fullName, final IndexRange indexRange, final MailSortField sortField, final OrderDirection order, final SearchTerm<?> searchTerm, final MailField[] mailFields) throws OXException {
        try {
            imapFolder = setAndOpenFolder(imapFolder, fullName, Folder.READ_ONLY);
            if (imapFolder.getMessageCount() == 0) {
                return EMPTY_RETVAL;
            }
            final MailFields usedFields = new MailFields();
            // Add desired fields
            usedFields.addAll(mailFields);
            // Add sort field
            usedFields.add(null == sortField ? MailField.RECEIVED_DATE : MailField.toField(sortField.getListField()));
            /*
             * Shall a search be performed?
             */
            final int[] filter;
            if (null == searchTerm) {
                // TODO: enable if action=updates shall be performed
                if (!IMAPSessionStorageAccess.hasSessionStorage(accountId, imapFolder, session)) {
                    IMAPSessionStorageAccess.fillSessionStorage(accountId, imapFolder, session);
                }
                /*
                 * Check if an all-fetch can be performed to only obtain UIDs of all folder's messages: FETCH 1: (UID)
                 */
                final MailFields mfs = new MailFields(mailFields);
                if (((null == sortField) || MailSortField.RECEIVED_DATE.equals(sortField)) && onlyLowCostFields(mfs)) {
                    final MailMessage[] mailMessages = performLowCostFetch(fullName, mfs, order, indexRange);
                    imapFolderStorage.updateCacheIfDiffer(fullName, mailMessages.length);
                    return mailMessages;
                }
                /*
                 * Proceed with common handling
                 */
                filter = null;
            } else {
                /*
                 * Preselect message list according to given search pattern
                 */
                filter = IMAPSearch.searchMessages(imapFolder, searchTerm, imapConfig);
                if ((filter == null) || (filter.length == 0)) {
                    return EMPTY_RETVAL;
                }
            }
            MailMessage[] mails = null;
            Message[] msgs = IMAPSort.sortMessages(imapFolder, usedFields, filter, sortField, order, getLocale(), imapConfig);
            if (null != msgs) {
                /*
                 * Sort was performed on IMAP server
                 */
                if (indexRange != null) {
                    final int fromIndex = indexRange.start;
                    int toIndex = indexRange.end;
                    if (msgs.length == 0) {
                        return EMPTY_RETVAL;
                    }
                    if ((fromIndex) > msgs.length) {
                        /*
                         * Return empty iterator if start is out of range
                         */
                        return EMPTY_RETVAL;
                    }
                    /*
                     * Reset end index if out of range
                     */
                    if (toIndex >= msgs.length) {
                        toIndex = msgs.length;
                    }
                    final Message[] tmp = msgs;
                    final int retvalLength = toIndex - fromIndex;
                    msgs = new Message[retvalLength];
                    System.arraycopy(tmp, fromIndex, msgs, 0, retvalLength);
                }
                mails =
                    convert2Mails(msgs, usedFields.toArray(), usedFields.contains(MailField.BODY) || usedFields.contains(MailField.FULL));
                if (usedFields.contains(MailField.ACCOUNT_NAME) || usedFields.contains(MailField.FULL)) {
                    setAccountInfo(mails);
                }
            } else {
                /*
                 * Do application sort
                 */
                final int size = filter == null ? imapFolder.getMessageCount() : filter.length;
                final FetchProfile fetchProfile = getFetchProfile(usedFields.toArray(), getIMAPProperties().isFastFetch());
                final boolean body = usedFields.contains(MailField.BODY) || usedFields.contains(MailField.FULL);
                if (DEBUG) {
                    final long start = System.currentTimeMillis();
                    if (filter == null) {
                        msgs =
                            new FetchIMAPCommand(imapFolder, imapConfig.getImapCapabilities().hasIMAP4rev1(), fetchProfile, size, body).doCommand();
                    } else {
                        msgs =
                            new FetchIMAPCommand(
                                imapFolder,
                                imapConfig.getImapCapabilities().hasIMAP4rev1(),
                                filter,
                                fetchProfile,
                                false,
                                false,
                                body).doCommand();
                    }
                    final long time = System.currentTimeMillis() - start;
                    LOG.debug(new StringBuilder(128).append("IMAP fetch for ").append(size).append(" messages took ").append(time).append(
                        "msec").toString());
                } else {
                    if (filter == null) {
                        msgs =
                            new FetchIMAPCommand(imapFolder, imapConfig.getImapCapabilities().hasIMAP4rev1(), fetchProfile, size, body).doCommand();
                    } else {
                        msgs =
                            new FetchIMAPCommand(
                                imapFolder,
                                imapConfig.getImapCapabilities().hasIMAP4rev1(),
                                filter,
                                fetchProfile,
                                false,
                                false,
                                body).doCommand();
                    }
                }
                if ((msgs == null) || (msgs.length == 0)) {
                    return new MailMessage[0];
                }
                mails = convert2Mails(msgs, usedFields.toArray(), body);
                if (usedFields.contains(MailField.ACCOUNT_NAME) || usedFields.contains(MailField.FULL)) {
                    setAccountInfo(mails);
                }
                /*
                 * Perform sort on temporary list
                 */
                final List<MailMessage> msgList = Arrays.asList(mails);
                Collections.sort(msgList, new MailMessageComparator(sortField, order == OrderDirection.DESC, getLocale()));
                mails = msgList.toArray(mails);
                /*
                 * Get proper sub-array if an index range is specified
                 */
                if (indexRange != null) {
                    final int fromIndex = indexRange.start;
                    int toIndex = indexRange.end;
                    if ((mails == null) || (msgs.length == 0)) {
                        return EMPTY_RETVAL;
                    }
                    if ((fromIndex) > mails.length) {
                        /*
                         * Return empty iterator if start is out of range
                         */
                        return EMPTY_RETVAL;
                    }
                    /*
                     * Reset end index if out of range
                     */
                    if (toIndex >= mails.length) {
                        toIndex = mails.length;
                    }
                    final MailMessage[] tmp = mails;
                    final int retvalLength = toIndex - fromIndex;
                    mails = new MailMessage[retvalLength];
                    System.arraycopy(tmp, fromIndex, mails, 0, retvalLength);
                }
            }
            return mails;
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private static final MailMessageComparator COMPARATOR = new MailMessageComparator(MailSortField.RECEIVED_DATE, true, null);

    @Override
    public List<List<MailMessage>> getThreadSortedMessages(final String fullName, final MailSortField sortField, final OrderDirection order, final MailField[] mailFields) throws OXException {
        try {
            if (!imapConfig.getImapCapabilities().hasThreadReferences()) {
                throw IMAPException.create(IMAPException.Code.THREAD_SORT_NOT_SUPPORTED, imapConfig, session, new Object[0]);
            }
            imapFolder = setAndOpenFolder(imapFolder, fullName, Folder.READ_ONLY);
            final TIntList seqNums;
            final List<ThreadSortNode> threadList;
            {
                /*
                 * Sort messages by thread reference
                 */
                final long start = System.currentTimeMillis();
                final String threadResp = ThreadSortUtil.getThreadResponse(imapFolder, "ALL");
                mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
                /*
                 * Parse THREAD response to a list structure and extract sequence numbers
                 */
                threadList = ThreadSortUtil.parseThreadResponse(threadResp);
                seqNums = ThreadSortUtil.getSeqNumsFromThreadResponse(threadResp);
            }
            /*
             * Fetch messages
             */
            final MailFields usedFields = new MailFields();
            // Add desired fields
            usedFields.addAll(mailFields);
            usedFields.add(MailField.THREAD_LEVEL);
            // Add sort field
            usedFields.add(null == sortField ? MailField.RECEIVED_DATE : MailField.toField(sortField.getListField()));
            final FetchProfile fetchProfile = getFetchProfile(usedFields.toArray(), getIMAPProperties().isFastFetch());
            final boolean body = usedFields.contains(MailField.BODY) || usedFields.contains(MailField.FULL);
            final boolean descending = OrderDirection.DESC.equals(order);
            if (!body) {
                final long start = System.currentTimeMillis();
                final TLongObjectMap<MailMessage> messages =
                    new SimpleFetchIMAPCommand(
                        imapFolder,
                        getSeparator(imapFolder),
                        imapConfig.getImapCapabilities().hasIMAP4rev1(),
                        seqNums.toArray(),
                        fetchProfile).doCommand();
                mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
                /*
                 * Apply account identifier
                 */
                for (final MailMessage mail : messages.valueCollection()) {
                    setAccountInfo(mail);
                }
                /*
                 * Generate structure
                 */
                final List<ThreadSortMailMessage> structuredList = ThreadSortUtil.toThreadSortStructure(threadList, messages);
                final List<List<MailMessage>> list = ThreadSortUtil.toSimplifiedStructure(structuredList, COMPARATOR);
                /*
                 * Sort according to order direction
                 */
                final MailSortField effectiveSortField = null == sortField ? MailSortField.RECEIVED_DATE : sortField;
                final MailMessageComparator comparator = new MailMessageComparator(effectiveSortField, descending, null);
                final Comparator<List<MailMessage>> listComparator = new Comparator<List<MailMessage>>() {

                    @Override
                    public int compare(final List<MailMessage> o1, final List<MailMessage> o2) {
                        return comparator.compare(o1.get(0), o2.get(0));
                    }
                };
                Collections.sort(list, listComparator);
                return list;
            }
            /*
             * Include body
             */
            final Message[] msgs =
                new FetchIMAPCommand(imapFolder, imapConfig.getImapCapabilities().hasIMAP4rev1(), seqNums, fetchProfile, false, true, body).doCommand();
            /*
             * Apply thread level
             */
            applyThreadLevel(threadList, 0, msgs, 0);
            /*
             * Generate structured list
             */
            final List<ThreadSortMailMessage> structuredList;
            {
                final MailMessage[] mails = setAccountInfo(convert2Mails(msgs, usedFields.toArray(), body));
                structuredList = ThreadSortUtil.toThreadSortStructure(mails);
            }
            /*
             * Sort according to order direction
             */
            final List<List<MailMessage>> list = ThreadSortUtil.toSimplifiedStructure(structuredList, COMPARATOR);
            /*
             * Sort according to order direction
             */
            final MailSortField effectiveSortField = null == sortField ? MailSortField.RECEIVED_DATE : sortField;
            final MailMessageComparator comparator = new MailMessageComparator(effectiveSortField, descending, null);
            final Comparator<List<MailMessage>> listComparator = new Comparator<List<MailMessage>>() {

                @Override
                public int compare(final List<MailMessage> o1, final List<MailMessage> o2) {
                    return comparator.compare(o1.get(0), o2.get(0));
                }
            };
            Collections.sort(list, listComparator);
            return list;
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public MailMessage[] getThreadSortedMessages(final String fullName, final IndexRange indexRange, final MailSortField sortField, final OrderDirection order, final SearchTerm<?> searchTerm, final MailField[] mailFields) throws OXException {
        try {
            if (!imapConfig.getImapCapabilities().hasThreadReferences()) {
                throw IMAPException.create(IMAPException.Code.THREAD_SORT_NOT_SUPPORTED, imapConfig, session, new Object[0]);
            }
            imapFolder = setAndOpenFolder(imapFolder, fullName, Folder.READ_ONLY);
            /*
             * Shall a search be performed?
             */
            final int[] filter;
            if (null == searchTerm) {
                filter = null;
            } else {
                /*
                 * Preselect message list according to given search pattern
                 */
                filter = IMAPSearch.searchMessages(imapFolder, searchTerm, imapConfig);
                if ((filter == null) || (filter.length == 0)) {
                    return EMPTY_RETVAL;
                }
            }
            final MailSortField effectiveSortField = null == sortField ? MailSortField.RECEIVED_DATE : sortField;
            final boolean descending = OrderDirection.DESC.equals(order);
            final TIntList seqnums;
            final List<ThreadSortNode> threadList;
            {
                /*
                 * Sort messages by thread reference
                 */
                final String sortRange;
                if (null == filter) {
                    /*
                     * Select all messages
                     */
                    sortRange = "ALL";
                } else {
                    /*
                     * Define sequence of valid message numbers: e.g.: 2,34,35,43,51
                     */
                    final StringBuilder tmp = new StringBuilder(filter.length << 2);
                    tmp.append(filter[0]);
                    for (int i = 1; i < filter.length; i++) {
                        tmp.append(',').append(filter[i]);
                    }
                    sortRange = tmp.toString();
                }
                /*
                 * Get THREAD response; e.g: "((1)(2)(3)(4)(5)(6)(7)(8)(9)(10)(11)(12)(13))"
                 */
                final long start = System.currentTimeMillis();
                final String threadResp = ThreadSortUtil.getThreadResponse(imapFolder, sortRange);
                mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
                /*
                 * Parse THREAD response to a list structure and extract sequence numbers
                 */
                threadList = ThreadSortUtil.parseThreadResponse(threadResp);
                seqnums = ThreadSortUtil.getSeqNumsFromThreadResponse(threadResp);
            }
            /*
             * Fetch messages
             */
            final MailFields usedFields = new MailFields();
            // Add desired fields
            usedFields.addAll(mailFields);
            usedFields.add(MailField.THREAD_LEVEL);
            // Add sort field
            usedFields.add(MailField.toField(effectiveSortField.getListField()));
            final FetchProfile fetchProfile = getFetchProfile(usedFields.toArray(), getIMAPProperties().isFastFetch());
            final boolean body = usedFields.contains(MailField.BODY) || usedFields.contains(MailField.FULL);
            if (!body) {
                final long start = System.currentTimeMillis();
                final TLongObjectMap<MailMessage> messages =
                    new SimpleFetchIMAPCommand(
                        imapFolder,
                        getSeparator(imapFolder),
                        imapConfig.getImapCapabilities().hasIMAP4rev1(),
                        seqnums.toArray(),
                        fetchProfile).doCommand();
                mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
                final List<ThreadSortMailMessage> structuredList = ThreadSortUtil.toThreadSortStructure(threadList, messages);
                /*
                 * Sort according to order direction
                 */
                Collections.sort(structuredList, new MailMessageComparator(effectiveSortField, descending, getLocale()));
                /*
                 * Output as flat list
                 */
                final List<MailMessage> flatList = new ArrayList<MailMessage>(messages.size());
                if (usedFields.contains(MailField.ACCOUNT_NAME) || usedFields.contains(MailField.FULL)) {
                    for (final MailMessage mail : flatList) {
                        setAccountInfo(mail);
                    }
                }
                ThreadSortUtil.toFlatList(structuredList, flatList);
                return flatList.toArray(new MailMessage[flatList.size()]);
            }
            /*
             * Include body
             */
            Message[] msgs =
                new FetchIMAPCommand(imapFolder, imapConfig.getImapCapabilities().hasIMAP4rev1(), seqnums, fetchProfile, false, true, body).doCommand();
            /*
             * Apply thread level
             */
            applyThreadLevel(threadList, 0, msgs, 0);
            /*
             * ... and return
             */
            if (indexRange != null) {
                final int fromIndex = indexRange.start;
                int toIndex = indexRange.end;
                if ((msgs == null) || (msgs.length == 0)) {
                    return EMPTY_RETVAL;
                }
                if ((fromIndex) > msgs.length) {
                    /*
                     * Return empty iterator if start is out of range
                     */
                    return EMPTY_RETVAL;
                }
                /*
                 * Reset end index if out of range
                 */
                if (toIndex >= msgs.length) {
                    toIndex = msgs.length;
                }
                final Message[] tmp = msgs;
                final int retvalLength = toIndex - fromIndex;
                msgs = new ExtendedMimeMessage[retvalLength];
                System.arraycopy(tmp, fromIndex, msgs, 0, retvalLength);
            }
            /*
             * Generate structured list
             */
            final List<ThreadSortMailMessage> structuredList;
            {
                final MailMessage[] mails;
                if (usedFields.contains(MailField.ACCOUNT_NAME) || usedFields.contains(MailField.FULL)) {
                    mails = setAccountInfo(convert2Mails(msgs, usedFields.toArray(), body));
                } else {
                    mails = convert2Mails(msgs, usedFields.toArray(), body);
                }
                structuredList = ThreadSortUtil.toThreadSortStructure(mails);
            }
            /*
             * Sort according to order direction
             */
            Collections.sort(structuredList, new MailMessageComparator(effectiveSortField, descending, getLocale()));
            /*
             * Output as flat list
             */
            final List<MailMessage> flatList = new ArrayList<MailMessage>(msgs.length);
            ThreadSortUtil.toFlatList(structuredList, flatList);
            return flatList.toArray(new MailMessage[flatList.size()]);
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public MailMessage[] getUnreadMessages(final String fullName, final MailSortField sortField, final OrderDirection order, final MailField[] mailFields, final int limit) throws OXException {
        try {
            imapFolder = setAndOpenFolder(imapFolder, fullName, Folder.READ_ONLY);
            MailMessage[] mails;
            {
                /*
                 * Ensure mail ID is contained in requested fields
                 */
                final MailFields fieldSet = new MailFields(mailFields);
                final MailField[] fields = fieldSet.toArray();
                /*
                 * Get ( & fetch) new messages
                 */
                final long start = System.currentTimeMillis();
                final Message[] msgs =
                    IMAPCommandsCollection.getUnreadMessages(imapFolder, fields, sortField, order, getIMAPProperties().isFastFetch(), limit);
                mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
                if ((msgs == null) || (msgs.length == 0) || limit == 0) {
                    return EMPTY_RETVAL;
                }
                /*
                 * Sort
                 */
                mails = convert2Mails(msgs, fields);
                if (fieldSet.contains(MailField.ACCOUNT_NAME) || fieldSet.contains(MailField.FULL)) {
                    setAccountInfo(mails);
                }
                final List<MailMessage> msgList = Arrays.asList(mails);
                Collections.sort(msgList, new MailMessageComparator(sortField, order == OrderDirection.DESC, getLocale()));
                mails = msgList.toArray(mails);
            }
            /*
             * Check for limit
             */
            if (limit > 0 && limit < mails.length) {
                final MailMessage[] retval = new MailMessage[limit];
                System.arraycopy(mails, 0, retval, 0, limit);
                mails = retval;
            }
            return mails;
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public void deleteMessagesLong(final String fullName, final long[] msgUIDs, final boolean hardDelete) throws OXException {
        try {
            imapFolder = setAndOpenFolder(imapFolder, fullName, Folder.READ_WRITE);
            try {
                if (!holdsMessages()) {
                    throw IMAPException.create(
                        IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES,
                        imapConfig,
                        session,
                        imapFolder.getFullName());
                }
                if (imapConfig.isSupportsACLs() && !aclExtension.canDeleteMessages(RightsCache.getCachedRights(
                    imapFolder,
                    true,
                    session,
                    accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_DELETE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
            } catch (final MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, imapFolder.getFullName());
            }
            imapFolderStorage.removeFromCache(fullName);
            if (hardDelete || usm.isHardDeleteMsgs()) {
                blockwiseDeletion(msgUIDs, false, null);
                notifyIMAPFolderModification(fullName);
                return;
            }
            final String trashFullname = imapAccess.getFolderStorage().getTrashFolder();
            if (null == trashFullname) {
                // TODO: Bug#8992 -> What to do if trash folder is null
                if (LOG.isErrorEnabled()) {
                    LOG.error("\n\tDefault trash folder is not set: aborting delete operation");
                }
                throw IMAPException.create(IMAPException.Code.MISSING_DEFAULT_FOLDER_NAME, imapConfig, session, "trash");
            }
            final boolean backup = (!isSubfolderOf(fullName, trashFullname, getSeparator(imapFolder)));
            blockwiseDeletion(msgUIDs, backup, backup ? trashFullname : null);
            if (IMAPSessionStorageAccess.isEnabled()) {
                IMAPSessionStorageAccess.removeDeletedSessionData(msgUIDs, accountId, session, fullName);
            }
            notifyIMAPFolderModification(fullName);
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private void blockwiseDeletion(final long[] msgUIDs, final boolean backup, final String trashFullname) throws OXException, MessagingException {
        if (0 == msgUIDs.length) {
            // Nothing to do on empty ID array
            return;
        }
        final StringBuilder debug = DEBUG ? new StringBuilder(128) : null;
        final long[] remain;
        final int blockSize = getIMAPProperties().getBlockSize();
        if (blockSize > 0 && msgUIDs.length > blockSize) {
            /*
             * Block-wise deletion
             */
            int offset = 0;
            final long[] tmp = new long[blockSize];
            for (int len = msgUIDs.length; len > blockSize; len -= blockSize) {
                System.arraycopy(msgUIDs, offset, tmp, 0, tmp.length);
                offset += blockSize;
                deleteByUIDs(trashFullname, backup, tmp, debug);
            }
            remain = new long[msgUIDs.length - offset];
            System.arraycopy(msgUIDs, offset, remain, 0, remain.length);
        } else {
            remain = msgUIDs;
        }
        deleteByUIDs(trashFullname, backup, remain, debug);
        /*
         * Close folder to force JavaMail-internal message cache update
         */
        imapFolder.close(false);
        resetIMAPFolder();
    }

    private void deleteByUIDs(final String trashFullname, final boolean backup, final long[] uids, final StringBuilder sb) throws OXException, MessagingException {
        if (backup) {
            /*
             * Copy messages to folder "TRASH"
             */
            final boolean supportsMove = imapConfig.asMap().containsKey("MOVE");
            try {
                AbstractIMAPCommand<long[]> command;
                if (supportsMove) {
                    command = new MoveIMAPCommand(imapFolder, uids, trashFullname, false, true);
                } else {
                    command = new CopyIMAPCommand(imapFolder, uids, trashFullname, false, true);
                }
                if (DEBUG) {
                    final long start = System.currentTimeMillis();
                    command.doCommand();
                    final long time = System.currentTimeMillis() - start;
                    sb.setLength(0);
                    if (supportsMove) {
                        LOG.debug(sb.append("\"Move\": ").append(uids.length).append(" messages moved to default trash folder \"").append(
                            trashFullname).append("\" in ").append(time).append(STR_MSEC).toString());
                    } else {
                        LOG.debug(sb.append("\"Soft Delete\": ").append(uids.length).append(" messages copied to default trash folder \"").append(
                            trashFullname).append("\" in ").append(time).append(STR_MSEC).toString());
                    }
                } else {
                    command.doCommand();
                }
            } catch (final MessagingException e) {
                if (e.getMessage().toLowerCase(Locale.US).indexOf("quota") >= 0) {
                    /*
                     * We face an Over-Quota-Exception
                     */
                    throw MailExceptionCode.DELETE_FAILED_OVER_QUOTA.create(e, new Object[0]);
                }
                final Exception nestedExc = e.getNextException();
                if (nestedExc != null && nestedExc.getMessage().toLowerCase(Locale.US).indexOf("quota") >= 0) {
                    /*
                     * We face an Over-Quota-Exception
                     */
                    throw MailExceptionCode.DELETE_FAILED_OVER_QUOTA.create(e, new Object[0]);
                }
                throw IMAPException.create(IMAPException.Code.MOVE_ON_DELETE_FAILED, imapConfig, session, e, new Object[0]);
            }
            if (supportsMove) {
                return;
            }
        }
        /*
         * Mark messages as \DELETED...
         */
        if (DEBUG) {
            final long start = System.currentTimeMillis();
            new FlagsIMAPCommand(imapFolder, uids, FLAGS_DELETED, true, true, false).doCommand();
            final long dur = System.currentTimeMillis() - start;
            sb.setLength(0);
            LOG.debug(sb.append(uids.length).append(" messages marked as deleted (through system flag \\DELETED) in ").append(dur).append(
                STR_MSEC).toString());
        } else {
            new FlagsIMAPCommand(imapFolder, uids, FLAGS_DELETED, true, true, false).doCommand();
        }
        /*
         * ... and perform EXPUNGE
         */
        try {
            IMAPCommandsCollection.uidExpungeWithFallback(imapFolder, uids, imapConfig.getImapCapabilities().hasUIDPlus());
        } catch (final FolderClosedException e) {
            /*
             * Not possible to retry since connection is broken
             */
            throw IMAPException.create(
                IMAPException.Code.CONNECT_ERROR,
                imapConfig,
                session,
                e,
                imapAccess.getMailConfig().getServer(),
                imapAccess.getMailConfig().getLogin());
        } catch (final StoreClosedException e) {
            /*
             * Not possible to retry since connection is broken
             */
            throw IMAPException.create(
                IMAPException.Code.CONNECT_ERROR,
                imapConfig,
                session,
                e,
                imapAccess.getMailConfig().getServer(),
                imapAccess.getMailConfig().getLogin());
        } catch (final MessagingException e) {
            throw IMAPException.create(
                IMAPException.Code.UID_EXPUNGE_FAILED,
                imapConfig,
                session,
                e,
                Arrays.toString(uids),
                imapFolder.getFullName(),
                e.getMessage());
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public long[] copyMessagesLong(final String sourceFolder, final String destFolder, final long[] mailIds, final boolean fast) throws OXException {
        return copyOrMoveMessages(sourceFolder, destFolder, mailIds, false, fast);
    }

    @Override
    public long[] moveMessagesLong(final String sourceFolder, final String destFolder, final long[] mailIds, final boolean fast) throws OXException {
        if (DEFAULT_FOLDER_ID.equals(destFolder)) {
            throw IMAPException.create(IMAPException.Code.NO_ROOT_MOVE, imapConfig, session, new Object[0]);
        }
        return copyOrMoveMessages(sourceFolder, destFolder, mailIds, true, fast);
    }

    private long[] copyOrMoveMessages(final String sourceFullName, final String destFullName, final long[] mailIds, final boolean move, final boolean fast) throws OXException {
        try {
            if (null == mailIds) {
                throw IMAPException.create(IMAPException.Code.MISSING_PARAMETER, imapConfig, session, "mailIDs");
            } else if ((sourceFullName == null) || (sourceFullName.length() == 0)) {
                throw IMAPException.create(IMAPException.Code.MISSING_SOURCE_TARGET_FOLDER_ON_MOVE, imapConfig, session, "source");
            } else if ((destFullName == null) || (destFullName.length() == 0)) {
                throw IMAPException.create(IMAPException.Code.MISSING_SOURCE_TARGET_FOLDER_ON_MOVE, imapConfig, session, "target");
            } else if (sourceFullName.equals(destFullName) && move) {
                throw IMAPException.create(IMAPException.Code.NO_EQUAL_MOVE, imapConfig, session, sourceFullName);
            } else if (0 == mailIds.length) {
                // Nothing to move
                return new long[0];
            }
            imapFolderStorage.clearCache();
            /*
             * Open and check user rights on source folder
             */
            imapFolder = setAndOpenFolder(imapFolder, sourceFullName, move ? Folder.READ_WRITE : Folder.READ_ONLY);
            try {
                if (!holdsMessages()) {
                    throw IMAPException.create(
                        IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES,
                        imapConfig,
                        session,
                        imapFolder.getFullName());
                }
                if (move && imapConfig.isSupportsACLs() && !aclExtension.canDeleteMessages(RightsCache.getCachedRights(
                    imapFolder,
                    true,
                    session,
                    accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_DELETE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
            } catch (final MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, imapFolder.getFullName());
            }
            {
                /*
                 * Open and check user rights on destination folder
                 */
                final IMAPFolder destFolder = (IMAPFolder) imapStore.getFolder(destFullName);
                {
                    final ListLsubEntry listEntry = ListLsubCache.getCachedLISTEntry(destFullName, accountId, destFolder, session);
                    if (!STR_INBOX.equals(destFullName) && !listEntry.exists()) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, destFullName);
                    }
                    if (!listEntry.canOpen()) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, destFullName);
                    }
                }
                try {
                    /*
                     * Check if COPY/APPEND is allowed on destination folder
                     */
                    if (imapConfig.isSupportsACLs() && !aclExtension.canInsert(RightsCache.getCachedRights(
                        destFolder,
                        true,
                        session,
                        accountId))) {
                        throw IMAPException.create(IMAPException.Code.NO_INSERT_ACCESS, imapConfig, session, destFolder.getFullName());
                    }
                } catch (final MessagingException e) {
                    throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, destFolder.getFullName());
                }
            }
            /*
             * Copy operation
             */
            final long[] result = new long[mailIds.length];
            final int blockSize = getIMAPProperties().getBlockSize();
            final StringBuilder debug = DEBUG ? new StringBuilder(128) : null;
            int offset = 0;
            final long[] remain;
            if (blockSize > 0 && mailIds.length > blockSize) {
                /*
                 * Block-wise deletion
                 */
                final long[] tmp = new long[blockSize];
                for (int len = mailIds.length; len > blockSize; len -= blockSize) {
                    System.arraycopy(mailIds, offset, tmp, 0, tmp.length);
                    final long[] uids = copyOrMoveByUID(move, fast, destFullName, tmp, debug);
                    /*
                     * Append UIDs
                     */
                    System.arraycopy(uids, 0, result, offset, uids.length);
                    offset += blockSize;
                }
                remain = new long[mailIds.length - offset];
                System.arraycopy(mailIds, offset, remain, 0, remain.length);
            } else {
                remain = mailIds;
            }
            final long[] uids = copyOrMoveByUID(move, fast, destFullName, remain, debug);
            System.arraycopy(uids, 0, result, offset, uids.length);
            if (move) {
                /*
                 * Force folder cache update through a close
                 */
                imapFolder.close(false);
                resetIMAPFolder();
            }
            final String draftFullname = imapAccess.getFolderStorage().getDraftsFolder();
            if (destFullName.equals(draftFullname)) {
                /*
                 * A copy/move to drafts folder. Ensure to set \Draft flag.
                 */
                final IMAPFolder destFolder = setAndOpenFolder(destFullName, Folder.READ_WRITE);
                try {
                    if (destFolder.getMessageCount() > 0) {
                        if (DEBUG) {
                            final long start = System.currentTimeMillis();
                            new FlagsIMAPCommand(destFolder, FLAGS_DRAFT, true, true).doCommand();
                            final long time = System.currentTimeMillis() - start;
                            LOG.debug(new StringBuilder(128).append(
                                "A copy/move to default drafts folder => All messages' \\Draft flag in ").append(destFullName).append(
                                " set in ").append(time).append(STR_MSEC).toString());
                        } else {
                            new FlagsIMAPCommand(destFolder, FLAGS_DRAFT, true, true).doCommand();
                        }
                    }
                } finally {
                    destFolder.close(false);
                }
            } else if (sourceFullName.equals(draftFullname)) {
                /*
                 * A copy/move from drafts folder. Ensure to unset \Draft flag.
                 */
                final IMAPFolder destFolder = setAndOpenFolder(destFullName, Folder.READ_WRITE);
                try {
                    if (DEBUG) {
                        final long start = System.currentTimeMillis();
                        new FlagsIMAPCommand(destFolder, FLAGS_DRAFT, false, true).doCommand();
                        final long time = System.currentTimeMillis() - start;
                        LOG.debug(new StringBuilder(128).append("A copy/move from default drafts folder => All messages' \\Draft flag in ").append(
                            destFullName).append(" unset in ").append(time).append(STR_MSEC).toString());
                    } else {
                        new FlagsIMAPCommand(destFolder, FLAGS_DRAFT, false, true).doCommand();
                    }
                } finally {
                    destFolder.close(false);
                }
            }
            if (move && IMAPSessionStorageAccess.isEnabled()) {
                IMAPSessionStorageAccess.removeDeletedSessionData(mailIds, accountId, session, sourceFullName);
            }
            return result;
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private long[] copyOrMoveByUID(final boolean move, final boolean fast, final String destFullName, final long[] tmp, final StringBuilder sb) throws MessagingException, OXException, IMAPException {
        final boolean supportsMove = move && imapConfig.asMap().containsKey("MOVE");
        final AbstractIMAPCommand<long[]> command;
        if (supportsMove) {
            command = new CopyIMAPCommand(imapFolder, tmp, destFullName, false, fast);
        } else {
            command = new CopyIMAPCommand(imapFolder, tmp, destFullName, false, fast);
        }
        long[] uids;
        if (DEBUG) {
            final long start = System.currentTimeMillis();
            uids = command.doCommand();
            final long time = System.currentTimeMillis() - start;
            sb.setLength(0);
            if (supportsMove) {
                LOG.debug(sb.append(tmp.length).append(" messages moved in ").append(time).append(STR_MSEC).toString());
            } else {
                LOG.debug(sb.append(tmp.length).append(" messages copied in ").append(time).append(STR_MSEC).toString());
            }
        } else {
            uids = command.doCommand();
        }
        if (!fast && ((uids == null) || noUIDsAssigned(uids, tmp.length))) {
            /*
             * Invalid UIDs
             */
            uids = getDestinationUIDs(tmp, destFullName);
        }
        if (supportsMove) {
            return uids;
        }
        if (move) {
            if (DEBUG) {
                final long start = System.currentTimeMillis();
                new FlagsIMAPCommand(imapFolder, tmp, FLAGS_DELETED, true, true, false).doCommand();
                final long time = System.currentTimeMillis() - start;
                sb.setLength(0);
                LOG.debug(sb.append(tmp.length).append(" messages marked as expunged (through system flag \\DELETED) in ").append(time).append(
                    STR_MSEC).toString());
            } else {
                new FlagsIMAPCommand(imapFolder, tmp, FLAGS_DELETED, true, true, false).doCommand();
            }
            try {
                IMAPCommandsCollection.uidExpungeWithFallback(imapFolder, tmp, imapConfig.getImapCapabilities().hasUIDPlus());
            } catch (final FolderClosedException e) {
                /*
                 * Not possible to retry since connection is broken
                 */
                throw IMAPException.create(
                    IMAPException.Code.CONNECT_ERROR,
                    imapConfig,
                    session,
                    e,
                    imapAccess.getMailConfig().getServer(),
                    imapAccess.getMailConfig().getLogin());
            } catch (final StoreClosedException e) {
                /*
                 * Not possible to retry since connection is broken
                 */
                throw IMAPException.create(
                    IMAPException.Code.CONNECT_ERROR,
                    imapConfig,
                    session,
                    e,
                    imapAccess.getMailConfig().getServer(),
                    imapAccess.getMailConfig().getLogin());
            } catch (final MessagingException e) {
                if (e.getNextException() instanceof ProtocolException) {
                    final ProtocolException protocolException = (ProtocolException) e.getNextException();
                    final Response response = protocolException.getResponse();
                    if (response != null && response.isBYE()) {
                        /*
                         * The BYE response is always untagged, and indicates that the server is about to close the connection.
                         */
                        throw IMAPException.create(
                            IMAPException.Code.CONNECT_ERROR,
                            imapConfig,
                            session,
                            e,
                            imapAccess.getMailConfig().getServer(),
                            imapAccess.getMailConfig().getLogin());
                    }
                    final Throwable cause = protocolException.getCause();
                    if (cause instanceof StoreClosedException) {
                        /*
                         * Connection is down. No retry.
                         */
                        throw IMAPException.create(
                            IMAPException.Code.CONNECT_ERROR,
                            imapConfig,
                            session,
                            e,
                            imapAccess.getMailConfig().getServer(),
                            imapAccess.getMailConfig().getLogin());
                    } else if (cause instanceof FolderClosedException) {
                        /*
                         * Connection is down. No retry.
                         */
                        throw IMAPException.create(
                            IMAPException.Code.CONNECT_ERROR,
                            imapConfig,
                            session,
                            e,
                            imapAccess.getMailConfig().getServer(),
                            imapAccess.getMailConfig().getLogin());
                    }
                }
                throw IMAPException.create(
                    IMAPException.Code.UID_EXPUNGE_FAILED,
                    imapConfig,
                    session,
                    e,
                    Arrays.toString(tmp),
                    imapFolder.getFullName(),
                    e.getMessage());
            }
        }
        return uids;
    }

    @Override
    public long[] appendMessagesLong(final String destFullName, final MailMessage[] mailMessages) throws OXException {
        if (null == mailMessages || mailMessages.length == 0) {
            return new long[0];
        }
        Message[] msgs = null;
        try {
            /*
             * Open and check user rights on source folder
             */
            imapFolder = setAndOpenFolder(imapFolder, destFullName, Folder.READ_WRITE);
            try {
                if (!holdsMessages()) {
                    throw IMAPException.create(
                        IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES,
                        imapConfig,
                        session,
                        imapFolder.getFullName());
                }
                if (imapConfig.isSupportsACLs() && !aclExtension.canInsert(RightsCache.getCachedRights(imapFolder, true, session, accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_INSERT_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
            } catch (final MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, imapFolder.getFullName());
            }
            imapFolderStorage.removeFromCache(destFullName);
            /*
             * Convert messages to JavaMail message objects
             */
            msgs =
                MIMEMessageConverter.convertMailMessages(
                    mailMessages,
                    MIMEMessageConverter.BEHAVIOR_CLONE | MIMEMessageConverter.BEHAVIOR_STREAM2FILE);
            /*
             * Drop special "x-original-headers" header
             */
            for (final Message message : msgs) {
                message.removeHeader("x-original-headers");
            }
            /*
             * Check if destination folder supports user flags
             */
            final boolean supportsUserFlags = UserFlagsCache.supportsUserFlags(imapFolder, true, session, accountId);
            if (!supportsUserFlags) {
                /*
                 * Remove all user flags from messages before appending to folder
                 */
                for (final Message message : msgs) {
                    removeUserFlagsFromMessage(message);
                }
            }
            /*
             * Mark first message for later lookup
             */
            final String hash = randomUUID();
            msgs[0].setHeader(MessageHeaders.HDR_X_OX_MARKER, fold(13, hash));
            /*
             * ... and append them to folder
             */
            long[] retval = new long[0];
            final boolean hasUIDPlus = imapConfig.getImapCapabilities().hasUIDPlus();
            try {
                if (hasUIDPlus) {
                    // Perform append expecting APPENUID response code
                    retval = checkAndConvertAppendUID(imapFolder.appendUIDMessages(msgs));
                } else {
                    // Perform simple append
                    imapFolder.appendMessages(msgs);
                }
            } catch (final MessagingException e) {
                final Exception nextException = e.getNextException();
                if (nextException instanceof com.sun.mail.iap.CommandFailedException) {
                    throw IMAPException.create(IMAPException.Code.INVALID_MESSAGE, imapConfig, session, e, new Object[0]);
                }
                throw e;
            }
            if (retval.length > 0) {
                /*
                 * Close affected IMAP folder to ensure consistency regarding IMAFolder's internal cache.
                 */
                notifyIMAPFolderModification(destFullName);
                return retval;
            }
            /*-
             * OK, go the long way:
             * 1. Find the marker in folder's messages
             * 2. Get the UIDs from found message's position
             */
            if (hasUIDPlus && LOG.isWarnEnabled()) {
                /*
                 * Missing UID information in APPENDUID response
                 */
                LOG.warn("Missing UID information in APPENDUID response");
            }
            retval = new long[msgs.length];
            final long[] uids = IMAPCommandsCollection.findMarker(hash, retval.length, imapFolder);
            if (uids.length == 0) {
                Arrays.fill(retval, -1L);
            } else {
                System.arraycopy(uids, 0, retval, 0, uids.length);
            }
            /*
             * Close affected IMAP folder to ensure consistency regarding IMAFolder's internal cache.
             */
            notifyIMAPFolderModification(destFullName);
            return retval;
        } catch (final MessagingException e) {
            if (DEBUG) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException) {
                    final StringBuilder sb = new StringBuilder(8192);
                    sb.append("\r\nAPPEND command failed. Printing messages' headers for debugging purpose:\r\n");
                    for (int i = 0; i < mailMessages.length; i++) {
                        sb.append("----------------------------------------------------\r\n\r\n");
                        sb.append(i + 1).append(". message's header:\r\n");
                        sb.append(mailMessages[i].getHeaders().toString());
                        sb.append("----------------------------------------------------\r\n\r\n");
                    }
                    LOG.debug(sb.toString());
                }
            }
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        } finally {
            if (null != msgs) {
                for (final Message message : msgs) {
                    if (message instanceof ManagedMimeMessage) {
                        ((ManagedMimeMessage) message).cleanUp();
                    }
                }
            }
        }
    }

    @Override
    public void updateMessageFlagsLong(final String fullName, final long[] msgUIDs, final int flagsArg, final boolean set) throws OXException {
        if (null == msgUIDs || 0 == msgUIDs.length) {
            // Nothing to do
            return;
        }
        try {
            imapFolder = setAndOpenFolder(imapFolder, fullName, Folder.READ_WRITE);
            /*
             * Remove non user-alterable system flags
             */
            imapFolderStorage.removeFromCache(fullName);
            int flags = flagsArg;
            flags &= ~MailMessage.FLAG_RECENT;
            flags &= ~MailMessage.FLAG_USER;
            /*
             * Set new flags...
             */
            final Rights myRights = imapConfig.isSupportsACLs() ? RightsCache.getCachedRights(imapFolder, true, session, accountId) : null;
            final Flags affectedFlags = new Flags();
            boolean applyFlags = false;
            if (((flags & MailMessage.FLAG_ANSWERED) > 0)) {
                if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                    throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
                affectedFlags.add(Flags.Flag.ANSWERED);
                applyFlags = true;
            }
            if (((flags & MailMessage.FLAG_DELETED) > 0)) {
                if (imapConfig.isSupportsACLs() && !aclExtension.canDeleteMessages(myRights)) {
                    throw IMAPException.create(IMAPException.Code.NO_DELETE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
                affectedFlags.add(DELETED);
                applyFlags = true;
            }
            if (((flags & MailMessage.FLAG_DRAFT) > 0)) {
                if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                    throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
                affectedFlags.add(DRAFT);
                applyFlags = true;
            }
            if (((flags & MailMessage.FLAG_FLAGGED) > 0)) {
                if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                    throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
                affectedFlags.add(Flags.Flag.FLAGGED);
                applyFlags = true;
            }
            if (((flags & MailMessage.FLAG_SEEN) > 0)) {
                if (imapConfig.isSupportsACLs() && !aclExtension.canKeepSeen(myRights)) {
                    throw IMAPException.create(IMAPException.Code.NO_KEEP_SEEN_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
                affectedFlags.add(Flags.Flag.SEEN);
                applyFlags = true;
            }
            /*
             * Check for forwarded flag (supported through user flags)
             */
            Boolean supportsUserFlags = null;
            if (((flags & MailMessage.FLAG_FORWARDED) > 0)) {
                supportsUserFlags = Boolean.valueOf(UserFlagsCache.supportsUserFlags(imapFolder, true, session, accountId));
                if (supportsUserFlags.booleanValue()) {
                    if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                        throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                    }
                    affectedFlags.add(MailMessage.USER_FORWARDED);
                    applyFlags = true;
                } else if (DEBUG) {
                    LOG.debug(new StringBuilder().append("IMAP server ").append(imapConfig.getImapServerSocketAddress()).append(
                        " does not support user flags. Skipping forwarded flag."));
                }
            }
            /*
             * Check for read acknowledgment flag (supported through user flags)
             */
            if (((flags & MailMessage.FLAG_READ_ACK) > 0)) {
                if (null == supportsUserFlags) {
                    supportsUserFlags = Boolean.valueOf(UserFlagsCache.supportsUserFlags(imapFolder, true, session, accountId));
                }
                if (supportsUserFlags.booleanValue()) {
                    if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                        throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                    }
                    affectedFlags.add(MailMessage.USER_READ_ACK);
                    applyFlags = true;
                } else if (DEBUG) {
                    LOG.debug(new StringBuilder().append("IMAP server ").append(imapConfig.getImapServerSocketAddress()).append(
                        " does not support user flags. Skipping read-ack flag."));
                }
            }
            if (applyFlags) {
                if (DEBUG) {
                    final long start = System.currentTimeMillis();
                    new FlagsIMAPCommand(imapFolder, msgUIDs, affectedFlags, set, true, false).doCommand();
                    final long time = System.currentTimeMillis() - start;
                    LOG.debug(new StringBuilder(128).append("Flags applied to ").append(msgUIDs.length).append(" messages in ").append(time).append(
                        STR_MSEC).toString());
                } else {
                    new FlagsIMAPCommand(imapFolder, msgUIDs, affectedFlags, set, true, false).doCommand();
                }
            }
            /*
             * Check for spam action
             */
            if (usm.isSpamEnabled() && ((flags & MailMessage.FLAG_SPAM) > 0)) {
                handleSpamByUID(msgUIDs, set, true, fullName, Folder.READ_WRITE);
            } else {
                /*
                 * Force JavaMail's cache update through folder closure
                 */
                imapFolder.close(false);
                resetIMAPFolder();
            }
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public void updateMessageFlags(final String fullName, final int flagsArg, final boolean set) throws OXException {
        if (null == fullName) {
            // Nothing to do
            return;
        }
        try {
            imapFolder = setAndOpenFolder(imapFolder, fullName, Folder.READ_WRITE);
            /*
             * Remove non user-alterable system flags
             */
            imapFolderStorage.removeFromCache(fullName);
            int flags = flagsArg;
            flags &= ~MailMessage.FLAG_RECENT;
            flags &= ~MailMessage.FLAG_USER;
            /*
             * Set new flags...
             */
            final Rights myRights = imapConfig.isSupportsACLs() ? RightsCache.getCachedRights(imapFolder, true, session, accountId) : null;
            final Flags affectedFlags = new Flags();
            boolean applyFlags = false;
            if (((flags & MailMessage.FLAG_ANSWERED) > 0)) {
                if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                    throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
                affectedFlags.add(Flags.Flag.ANSWERED);
                applyFlags = true;
            }
            if (((flags & MailMessage.FLAG_DELETED) > 0)) {
                if (imapConfig.isSupportsACLs() && !aclExtension.canDeleteMessages(myRights)) {
                    throw IMAPException.create(IMAPException.Code.NO_DELETE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
                affectedFlags.add(DELETED);
                applyFlags = true;
            }
            if (((flags & MailMessage.FLAG_DRAFT) > 0)) {
                if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                    throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
                affectedFlags.add(DRAFT);
                applyFlags = true;
            }
            if (((flags & MailMessage.FLAG_FLAGGED) > 0)) {
                if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                    throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
                affectedFlags.add(Flags.Flag.FLAGGED);
                applyFlags = true;
            }
            if (((flags & MailMessage.FLAG_SEEN) > 0)) {
                if (imapConfig.isSupportsACLs() && !aclExtension.canKeepSeen(myRights)) {
                    throw IMAPException.create(IMAPException.Code.NO_KEEP_SEEN_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
                affectedFlags.add(Flags.Flag.SEEN);
                applyFlags = true;
            }
            /*
             * Check for forwarded flag (supported through user flags)
             */
            Boolean supportsUserFlags = null;
            if (((flags & MailMessage.FLAG_FORWARDED) > 0)) {
                supportsUserFlags = Boolean.valueOf(UserFlagsCache.supportsUserFlags(imapFolder, true, session, accountId));
                if (supportsUserFlags.booleanValue()) {
                    if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                        throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                    }
                    affectedFlags.add(MailMessage.USER_FORWARDED);
                    applyFlags = true;
                } else if (DEBUG) {
                    LOG.debug(new StringBuilder().append("IMAP server ").append(imapConfig.getImapServerSocketAddress()).append(
                        " does not support user flags. Skipping forwarded flag."));
                }
            }
            /*
             * Check for read acknowledgment flag (supported through user flags)
             */
            if (((flags & MailMessage.FLAG_READ_ACK) > 0)) {
                if (null == supportsUserFlags) {
                    supportsUserFlags = Boolean.valueOf(UserFlagsCache.supportsUserFlags(imapFolder, true, session, accountId));
                }
                if (supportsUserFlags.booleanValue()) {
                    if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                        throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                    }
                    affectedFlags.add(MailMessage.USER_READ_ACK);
                    applyFlags = true;
                } else if (DEBUG) {
                    LOG.debug(new StringBuilder().append("IMAP server ").append(imapConfig.getImapServerSocketAddress()).append(
                        " does not support user flags. Skipping read-ack flag."));
                }
            }
            if (applyFlags) {
                if (DEBUG) {
                    final long start = System.currentTimeMillis();
                    new FlagsIMAPCommand(imapFolder, affectedFlags, set, true).doCommand();
                    final long time = System.currentTimeMillis() - start;
                    LOG.debug(new StringBuilder(128).append("Flags applied to all messages in ").append(time).append(STR_MSEC).toString());
                } else {
                    new FlagsIMAPCommand(imapFolder, affectedFlags, set, true).doCommand();
                }
            }
            /*
             * Check for spam action
             */
            if (usm.isSpamEnabled() && ((flags & MailMessage.FLAG_SPAM) > 0)) {
                final long[] uids = IMAPCommandsCollection.getUIDs(imapFolder);
                handleSpamByUID(uids, set, true, fullName, Folder.READ_WRITE);
            } else {
                /*
                 * Force JavaMail's cache update through folder closure
                 */
                imapFolder.close(false);
                resetIMAPFolder();
            }
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public void updateMessageColorLabelLong(final String fullName, final long[] msgUIDs, final int colorLabel) throws OXException {
        if (null == msgUIDs || 0 == msgUIDs.length) {
            // Nothing to do
            return;
        }
        try {
            if (!MailProperties.getInstance().isUserFlagsEnabled()) {
                /*
                 * User flags are disabled
                 */
                if (DEBUG) {
                    LOG.debug("User flags are disabled or not supported. Update of color flag ignored.");
                }
                return;
            }
            imapFolder = setAndOpenFolder(imapFolder, fullName, Folder.READ_WRITE);
            try {
                if (!holdsMessages()) {
                    throw IMAPException.create(
                        IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES,
                        imapConfig,
                        session,
                        imapFolder.getFullName());
                }
                if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(RightsCache.getCachedRights(imapFolder, true, session, accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
            } catch (final MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, imapFolder.getFullName());
            }
            if (!UserFlagsCache.supportsUserFlags(imapFolder, true, session, accountId)) {
                LOG.error(new StringBuilder().append("Folder \"").append(imapFolder.getFullName()).append(
                    "\" does not support user-defined flags. Update of color flag ignored."));
                return;
            }
            /*
             * Remove all old color label flag(s) and set new color label flag
             */
            imapFolderStorage.removeFromCache(fullName);
            long start = System.currentTimeMillis();
            IMAPCommandsCollection.clearAllColorLabels(imapFolder, msgUIDs);
            mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
            if (DEBUG) {
                LOG.debug(new StringBuilder(128).append("All color flags cleared from ").append(msgUIDs.length).append(" messages in ").append(
                    (System.currentTimeMillis() - start)).append(STR_MSEC).toString());
            }
            start = System.currentTimeMillis();
            IMAPCommandsCollection.setColorLabel(imapFolder, msgUIDs, MailMessage.getColorLabelStringValue(colorLabel));
            mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
            if (DEBUG) {
                LOG.debug(new StringBuilder(128).append("All color flags set in ").append(msgUIDs.length).append(" messages in ").append(
                    (System.currentTimeMillis() - start)).append(STR_MSEC).toString());
            }
            /*
             * Force JavaMail's cache update through folder closure
             */
            imapFolder.close(false);
            resetIMAPFolder();
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public void updateMessageColorLabel(final String fullName, final int colorLabel) throws OXException {
        if (null == fullName) {
            // Nothing to do
            return;
        }
        try {
            if (!MailProperties.getInstance().isUserFlagsEnabled()) {
                /*
                 * User flags are disabled
                 */
                if (DEBUG) {
                    LOG.debug("User flags are disabled or not supported. Update of color flag ignored.");
                }
                return;
            }
            imapFolder = setAndOpenFolder(imapFolder, fullName, Folder.READ_WRITE);
            try {
                if (!holdsMessages()) {
                    throw IMAPException.create(
                        IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES,
                        imapConfig,
                        session,
                        imapFolder.getFullName());
                }
                if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(RightsCache.getCachedRights(imapFolder, true, session, accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
            } catch (final MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, imapFolder.getFullName());
            }
            if (!UserFlagsCache.supportsUserFlags(imapFolder, true, session, accountId)) {
                LOG.error(new StringBuilder().append("Folder \"").append(imapFolder.getFullName()).append(
                    "\" does not support user-defined flags. Update of color flag ignored."));
                return;
            }
            /*
             * Remove all old color label flag(s) and set new color label flag
             */
            imapFolderStorage.removeFromCache(fullName);
            long start = System.currentTimeMillis();
            IMAPCommandsCollection.clearAllColorLabels(imapFolder, null);
            mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
            if (DEBUG) {
                LOG.debug(new StringBuilder(128).append("All color flags cleared from all messages in ").append(
                    (System.currentTimeMillis() - start)).append(STR_MSEC).toString());
            }
            start = System.currentTimeMillis();
            IMAPCommandsCollection.setColorLabel(imapFolder, null, MailMessage.getColorLabelStringValue(colorLabel));
            mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
            if (DEBUG) {
                LOG.debug(new StringBuilder(128).append("All color flags set in all messages in ").append(
                    (System.currentTimeMillis() - start)).append(STR_MSEC).toString());
            }
            /*
             * Force JavaMail's cache update through folder closure
             */
            imapFolder.close(false);
            resetIMAPFolder();
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public MailMessage saveDraft(final String draftFullName, final ComposedMailMessage composedMail) throws OXException {
        try {
            final MimeMessage mimeMessage = new MimeMessage(imapAccess.getMailSession());
            /*
             * Fill message
             */
            final long uid;
            final MIMEMessageFiller filler = new MIMEMessageFiller(session, ctx);
            composedMail.setFiller(filler);
            try {
                /*
                 * Set headers
                 */
                filler.setMessageHeaders(composedMail, mimeMessage);
                /*
                 * Set common headers
                 */
                filler.setCommonHeaders(mimeMessage);
                /*
                 * Fill body
                 */
                filler.fillMailBody(composedMail, mimeMessage, ComposeType.NEW);
                mimeMessage.setFlag(DRAFT, true);
                mimeMessage.saveChanges();
                /*
                 * Append message to draft folder
                 */
                imapFolderStorage.removeFromCache(draftFullName);
                uid = appendMessagesLong(draftFullName, new MailMessage[] { MIMEMessageConverter.convertMessage(mimeMessage, false) })[0];
            } finally {
                composedMail.cleanUp();
            }
            /*
             * Check for draft-edit operation: Delete old version
             */
            final MailPath msgref = composedMail.getMsgref();
            if (msgref != null && draftFullName.equals(msgref.getFolder())) {
                if (accountId != msgref.getAccountId()) {
                    LOG.warn(
                        new StringBuilder("Differing account ID in msgref attribute.\nMessage storage account ID: ").append(accountId).append(
                            ".\nmsgref account ID: ").append(msgref.getAccountId()).toString(),
                        new Throwable());
                }
                deleteMessagesLong(msgref.getFolder(), new long[] { parseUnsignedLong(msgref.getMailID()) }, true);
                composedMail.setMsgref(null);
            }
            /*
             * Force folder update
             */
            notifyIMAPFolderModification(draftFullName);
            /*
             * Return draft mail
             */
            return getMessageLong(draftFullName, uid, true);
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final IOException e) {
            throw IMAPException.create(IMAPException.Code.IO_ERROR, imapConfig, session, e, e.getMessage());
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public MailMessage[] getNewAndModifiedMessages(final String fullName, final MailField[] fields) throws OXException {
        // TODO: Needs to be thoroughly tested
        return EMPTY_RETVAL;
        // return getChangedMessages(folder, fields, 0);
    }

    @Override
    public MailMessage[] getDeletedMessages(final String fullName, final MailField[] fields) throws OXException {
        // TODO: Needs to be thoroughly tested
        return EMPTY_RETVAL;
        // return getChangedMessages(folder, fields, 1);
    }

    private MailMessage[] getChangedMessages(final String folder, final MailField[] fields, final int index) throws OXException {
        try {
            imapFolder = setAndOpenFolder(imapFolder, folder, Folder.READ_ONLY);
            if (!holdsMessages()) {
                throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, imapFolder.getFullName());
            }
            final long[] uids = IMAPSessionStorageAccess.getChanges(accountId, imapFolder, session, index + 1)[index];
            return getMessagesLong(folder, uids, fields);
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    /*-
     * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     * +++++++++++++++++ Helper methods +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     */

    private static final MailFields MAILFIELDS_DEFAULT = new MailFields(MailField.ID, MailField.FOLDER_ID);

    private static boolean assumeIMAPSortIsReliable() {
        return false; // Introduce config parameter?
    }

    /**
     * Performs the FETCH command on currently active IMAP folder on all messages using the 1:* sequence range argument.
     * 
     * @param fullName The IMAP folder's full name
     * @param lowCostFields The low-cost fields
     * @param order The order direction (needed to possibly flip the results)
     * @return The fetched mail messages with only ID and folder ID set.
     * @throws MessagingException If a messaging error occurs
     */
    private MailMessage[] performLowCostFetch(final String fullName, final MailFields lowCostFields, final OrderDirection order, final IndexRange indexRange) throws MessagingException {
        /*
         * Perform simple fetch
         */
        MailMessage[] retval = null;
        {
            boolean allFetch = true;
            if (assumeIMAPSortIsReliable() && MAILFIELDS_DEFAULT.equals(lowCostFields)) { // Enable if sure that IMAP sort works reliably
                try {
                    final long start = System.currentTimeMillis();
                    final long[] uids = IMAPSort.allUIDs(imapFolder, OrderDirection.DESC.equals(order), imapConfig);
                    mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
                    if (null != uids) {
                        final int len = uids.length;
                        final List<MailMessage> list = new ArrayList<MailMessage>(len);
                        for (int i = 0; i < len; i++) {
                            final IDMailMessage mail = new IDMailMessage(Long.toString(uids[i]), fullName);
                            list.add(mail);
                        }
                        retval = list.toArray(new MailMessage[list.size()]);
                        allFetch = false;
                    }
                } catch (final MessagingException e) {
                    LOG.warn(
                        new StringBuilder("SORT command on IMAP server \"").append(imapConfig.getServer()).append("\" failed with login ").append(
                            imapConfig.getLogin()).append(" (user=").append(session.getUserId()).append(", context=").append(
                            session.getContextId()).append("): ").append(e.getMessage()),
                        e);
                }
            }
            if (allFetch) {
                lowCostFields.add(MailField.RECEIVED_DATE);
                final LowCostItem[] lowCostItems = getLowCostItems(lowCostFields);
                final long start = System.currentTimeMillis();
                retval = AllFetch.fetchLowCost(imapFolder, lowCostItems, OrderDirection.ASC.equals(order), imapConfig, session);
                mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
                if (DEBUG) {
                    LOG.debug(
                        new StringBuilder(128).append(fullName).append(": IMAP all fetch >>>FETCH 1:* (").append(
                            AllFetch.getFetchCommand(lowCostItems)).append(")<<< took ").append((System.currentTimeMillis() - start)).append(
                            STR_MSEC).toString(),
                        new Throwable());
                }
            }
        }
        if (retval == null || retval.length == 0) {
            return EMPTY_RETVAL;
        }
        if (indexRange != null) {
            final int fromIndex = indexRange.start;
            int toIndex = indexRange.end;
            if ((fromIndex) > retval.length) {
                /*
                 * Return empty iterator if start is out of range
                 */
                return EMPTY_RETVAL;
            }
            /*
             * Reset end index if out of range
             */
            if (toIndex >= retval.length) {
                toIndex = retval.length;
            }
            final MailMessage[] tmp = retval;
            final int retvalLength = toIndex - fromIndex;
            retval = new MailMessage[retvalLength];
            System.arraycopy(tmp, fromIndex, retval, 0, retvalLength);
        }
        return retval;
    }

    private static final MailFields FIELDS_ENV = new MailFields(new MailField[] {
        MailField.SENT_DATE, MailField.FROM, MailField.TO, MailField.CC, MailField.BCC, MailField.SUBJECT });

    private static LowCostItem[] getLowCostItems(final MailFields fields) {
        final Set<LowCostItem> l = EnumSet.noneOf(LowCostItem.class);
        if (fields.contains(MailField.RECEIVED_DATE)) {
            l.add(LowCostItem.INTERNALDATE);
        }
        if (fields.contains(MailField.ID)) {
            l.add(LowCostItem.UID);
        }
        if (fields.contains(MailField.FLAGS) || fields.contains(MailField.COLOR_LABEL)) {
            l.add(LowCostItem.FLAGS);
        }
        if (fields.contains(MailField.CONTENT_TYPE)) {
            l.add(LowCostItem.BODYSTRUCTURE);
        }
        if (fields.contains(MailField.SIZE)) {
            l.add(LowCostItem.SIZE);
        }
        if (fields.containsAny(FIELDS_ENV)) {
            l.add(LowCostItem.ENVELOPE);
        }
        return l.toArray(new LowCostItem[l.size()]);
    }

    private static final EnumSet<MailField> LOW_COST = EnumSet.of(
        MailField.ID,
        MailField.FOLDER_ID,
        MailField.RECEIVED_DATE,
        MailField.FLAGS,
        MailField.COLOR_LABEL,
        MailField.SIZE,
        MailField.CONTENT_TYPE,
        MailField.SENT_DATE,
        MailField.FROM,
        MailField.TO,
        MailField.CC,
        MailField.BCC,
        MailField.SUBJECT);

    private static boolean onlyLowCostFields(final MailFields fields) {
        final Set<MailField> set = fields.toSet();
        if (!set.removeAll(LOW_COST)) {
            return false;
        }
        return set.isEmpty();
    }

    private static int applyThreadLevel(final List<ThreadSortNode> threadList, final int level, final Message[] msgs, final int index) {
        if (null == threadList) {
            return index;
        }
        int idx = index;
        final int threadListSize = threadList.size();
        final Iterator<ThreadSortNode> iter = threadList.iterator();
        for (int i = 0; i < threadListSize; i++) {
            final ThreadSortNode currentNode = iter.next();
            ((ExtendedMimeMessage) msgs[idx]).setThreadLevel(level);
            idx++;
            idx = applyThreadLevel(currentNode.getChilds(), level + 1, msgs, idx);
        }
        return idx;
    }

    private static boolean noUIDsAssigned(final long[] arr, final int expectedLen) {
        final long[] tmp = new long[expectedLen];
        Arrays.fill(tmp, -1L);
        return Arrays.equals(arr, tmp);
    }

    /**
     * Determines the corresponding UIDs in destination folder
     * 
     * @param msgUIDs The UIDs in source folder
     * @param destFullName The destination folder's full name
     * @return The corresponding UIDs in destination folder
     * @throws MessagingException
     * @throws OXException
     */
    private long[] getDestinationUIDs(final long[] msgUIDs, final String destFullName) throws MessagingException, OXException {
        /*
         * No COPYUID present in response code. Since UIDs are assigned in strictly ascending order in the mailbox (refer to IMAPv4 rfc3501,
         * section 2.3.1.1), we can discover corresponding UIDs by selecting the destination mailbox and detecting the location of messages
         * placed in the destination mailbox by using FETCH and/or SEARCH commands (e.g., for Message-ID or some unique marker placed in the
         * message in an APPEND).
         */
        final long[] retval = new long[msgUIDs.length];
        Arrays.fill(retval, -1L);
        if (!IMAPCommandsCollection.canBeOpened(imapFolder, destFullName, Folder.READ_ONLY)) {
            // No look-up possible
            return retval;
        }
        final String messageId;
        {
            int minIndex = 0;
            long minVal = msgUIDs[0];
            for (int i = 1; i < msgUIDs.length; i++) {
                if (msgUIDs[i] < minVal) {
                    minIndex = i;
                    minVal = msgUIDs[i];
                }
            }
            final IMAPMessage imapMessage = (IMAPMessage) (imapFolder.getMessageByUID(msgUIDs[minIndex]));
            if (imapMessage == null) {
                /*
                 * No message found whose UID matches msgUIDs[minIndex]
                 */
                messageId = null;
            } else {
                messageId = imapMessage.getMessageID();
            }
        }
        if (messageId != null) {
            final IMAPFolder destFolder = (IMAPFolder) imapStore.getFolder(destFullName);
            destFolder.open(Folder.READ_ONLY);
            try {
                /*
                 * Find this message ID in destination folder
                 */
                long startUID = IMAPCommandsCollection.messageId2UID(destFolder, messageId)[0];
                if (startUID != -1) {
                    for (int i = 0; i < msgUIDs.length; i++) {
                        retval[i] = startUID++;
                    }
                }
            } finally {
                destFolder.close(false);
            }
        }
        return retval;
    }

    private void handleSpamByUID(final long[] msgUIDs, final boolean isSpam, final boolean move, final String fullName, final int desiredMode) throws MessagingException, OXException {
        /*
         * Check for spam handling
         */
        if (usm.isSpamEnabled()) {
            final boolean locatedInSpamFolder = imapAccess.getFolderStorage().getSpamFolder().equals(imapFolder.getFullName());
            if (isSpam) {
                if (locatedInSpamFolder) {
                    /*
                     * A message that already has been detected as spam should again be learned as spam: Abort.
                     */
                    return;
                }
                /*
                 * Handle spam
                 */
                {
                    SpamHandlerRegistry.getSpamHandlerBySession(session, accountId, IMAPProvider.getInstance()).handleSpam(
                        accountId,
                        imapFolder.getFullName(),
                        longs2uids(msgUIDs),
                        move,
                        session);
                    /*
                     * Close and reopen to force internal message cache update
                     */
                    resetIMAPFolder();
                    imapFolder = setAndOpenFolder(imapFolder, fullName, desiredMode);
                }
                return;
            }
            if (!locatedInSpamFolder) {
                /*
                 * A message that already has been detected as ham should again be learned as ham: Abort.
                 */
                return;
            }
            /*
             * Handle ham.
             */
            {
                SpamHandlerRegistry.getSpamHandlerBySession(session, accountId, IMAPProvider.getInstance()).handleHam(
                    accountId,
                    imapFolder.getFullName(),
                    longs2uids(msgUIDs),
                    move,
                    session);
                /*
                 * Close and reopen to force internal message cache update
                 */
                resetIMAPFolder();
                imapFolder = setAndOpenFolder(imapFolder, fullName, desiredMode);
            }
        }
    }

    /**
     * Checks and converts specified APPENDUID response.
     * 
     * @param appendUIDs The APPENDUID response
     * @return An array of long for each valid {@link AppendUID} element or a zero size array of long if an invalid {@link AppendUID}
     *         element was detected.
     */
    private static long[] checkAndConvertAppendUID(final AppendUID[] appendUIDs) {
        if (appendUIDs == null || appendUIDs.length == 0) {
            return new long[0];
        }
        final long[] retval = new long[appendUIDs.length];
        for (int i = 0; i < appendUIDs.length; i++) {
            if (appendUIDs[i] == null) {
                /*
                 * A null element means the server didn't return UID information for the appended message.
                 */
                return new long[0];
            }
            retval[i] = appendUIDs[i].uid;
        }
        return retval;
    }

    /**
     * Removes all user flags from given message's flags
     * 
     * @param message The message whose user flags shall be removed
     * @throws MessagingException If removing user flags fails
     */
    private static void removeUserFlagsFromMessage(final Message message) throws MessagingException {
        final String[] userFlags = message.getFlags().getUserFlags();
        if (userFlags.length > 0) {
            /*
             * Create a new flags container necessary for later removal
             */
            final Flags remove = new Flags();
            for (final String userFlag : userFlags) {
                remove.add(userFlag);
            }
            /*
             * Remove gathered user flags from message's flags; flags which do not occur in flags object are unaffected.
             */
            message.setFlags(remove, false);
        }
    }

    /**
     * Generates a UUID using {@link UUID#randomUUID()}; e.g.:<br>
     * <i>a5aa65cb-6c7e-4089-9ce2-b107d21b9d15</i>
     * 
     * @return A UUID string
     */
    private static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Sets account ID and name in given instance of {@link MailMessage}.
     * 
     * @param mailMessages The {@link MailMessage} instance
     * @return The given instance of {@link MailMessage} with account ID and name set
     * @throws OXException If mail account cannot be obtained
     */
    private MailMessage setAccountInfo(final MailMessage mailMessage) throws OXException {
        if (null == mailMessage) {
            return null;
        }
        final MailAccount account = getMailAccount();
        final String name = account.getName();
        final int id = account.getId();
        mailMessage.setAccountId(id);
        mailMessage.setAccountName(name);
        return mailMessage;
    }

    /**
     * Sets account ID and name in given instances of {@link MailMessage}.
     * 
     * @param mailMessages The {@link MailMessage} instances
     * @return The given instances of {@link MailMessage} each with account ID and name set
     * @throws OXException If mail account cannot be obtained
     */
    private MailMessage[] setAccountInfo(final MailMessage[] mailMessages) throws OXException {
        final MailAccount account = getMailAccount();
        final String name = account.getName();
        final int id = account.getId();
        for (int i = 0; i < mailMessages.length; i++) {
            final MailMessage mailMessage = mailMessages[i];
            if (null != mailMessage) {
                mailMessage.setAccountId(id);
                mailMessage.setAccountName(name);
            }
        }
        return mailMessages;
    }

    private MailMessage[] convert2Mails(final Message[] msgs, final MailField[] fields) throws OXException {
        return convert2Mails(msgs, fields, null, false);
    }

    private MailMessage[] convert2Mails(final Message[] msgs, final MailField[] fields, final boolean includeBody) throws OXException {
        return convert2Mails(msgs, fields, null, includeBody);
    }

    private MailMessage[] convert2Mails(final Message[] msgs, final MailField[] fields, final String[] headerNames) throws OXException {
        return convert2Mails(msgs, fields, headerNames, false);
    }

    private MailMessage[] convert2Mails(final Message[] msgs, final MailField[] fields, final String[] headerNames, final boolean includeBody) throws OXException {
        return MIMEMessageConverter.convertMessages(msgs, fields, headerNames, includeBody);
    }

    private char getSeparator(final IMAPFolder imapFolder) throws OXException, MessagingException {
        return getLISTEntry(STR_INBOX, imapFolder).getSeparator();
    }

    private ListLsubEntry getLISTEntry(final String fullName, final IMAPFolder imapFolder) throws OXException, MessagingException {
        return ListLsubCache.getCachedLISTEntry(fullName, accountId, imapFolder, session);
    }

    private static boolean isSubfolderOf(final String fullName, final String possibleParent, final char separator) {
        if (!fullName.startsWith(possibleParent)) {
            return false;
        }
        final int length = possibleParent.length();
        if (length >= fullName.length()) {
            return true;
        }
        return fullName.charAt(length) == separator;
    }

}
