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

package com.openexchange.imap;

import static com.openexchange.mail.MailServletInterface.mailInterfaceMonitor;
import static com.openexchange.mail.dataobjects.MailFolder.DEFAULT_FOLDER_ID;
import static com.openexchange.mail.mime.utils.MIMEMessageUtility.fold;
import static com.openexchange.mail.mime.utils.MIMEStorageUtility.getFetchProfile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.StoreClosedException;
import javax.mail.internet.MimeMessage;
import com.openexchange.groupware.ldap.UserException;
import com.openexchange.imap.AllFetch.LowCostItem;
import com.openexchange.imap.cache.RightsCache;
import com.openexchange.imap.cache.UserFlagsCache;
import com.openexchange.imap.command.CopyIMAPCommand;
import com.openexchange.imap.command.FetchIMAPCommand;
import com.openexchange.imap.command.FlagsIMAPCommand;
import com.openexchange.imap.config.IIMAPProperties;
import com.openexchange.imap.search.IMAPSearch;
import com.openexchange.imap.services.IMAPServiceRegistry;
import com.openexchange.imap.sort.IMAPSort;
import com.openexchange.imap.threadsort.ThreadSortMailMessage;
import com.openexchange.imap.threadsort.ThreadSortNode;
import com.openexchange.imap.threadsort.ThreadSortUtil;
import com.openexchange.imap.util.IMAPSessionStorageAccess;
import com.openexchange.mail.IndexRange;
import com.openexchange.mail.MailException;
import com.openexchange.mail.MailField;
import com.openexchange.mail.MailFields;
import com.openexchange.mail.MailPath;
import com.openexchange.mail.MailSortField;
import com.openexchange.mail.OrderDirection;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.compose.ComposeType;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.mime.ExtendedMimeMessage;
import com.openexchange.mail.mime.MIMEMailException;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.converters.MIMEMessageConverter;
import com.openexchange.mail.mime.filler.MIMEMessageFiller;
import com.openexchange.mail.search.SearchTerm;
import com.openexchange.mail.utils.MailMessageComparator;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountException;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.server.ServiceException;
import com.openexchange.session.Session;
import com.openexchange.spamhandler.SpamHandlerRegistry;
import com.openexchange.user.UserService;
import com.sun.mail.iap.CommandFailedException;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.AppendUID;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.Rights;

/**
 * {@link IMAPMessageStorage} - The IMAP implementation of message storage.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class IMAPMessageStorage extends IMAPFolderWorker {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(IMAPMessageStorage.class);

    private static final boolean DEBUG = LOG.isDebugEnabled();

    /**
     * Serial version UID
     */
    private static final long serialVersionUID = 1467121647337217270L;

    /*-
     * Flag constants
     */

    private static final Flags FLAGS_DRAFT = new Flags(Flags.Flag.DRAFT);

    private static final Flags FLAGS_DELETED = new Flags(Flags.Flag.DELETED);

    /*-
     * String constants
     */

    private static final String STR_MSEC = "msec";

    /*-
     * Members
     */

    private MailAccount mailAccount;

    private Locale locale;

    private IIMAPProperties imapProperties;

    /**
     * Initializes a new {@link IMAPMessageStorage}.
     * 
     * @param imapStore The IMAP store
     * @param imapAccess The IMAP access
     * @param session The session providing needed user data
     * @throws IMAPException If context loading fails
     */
    public IMAPMessageStorage(final IMAPStore imapStore, final IMAPAccess imapAccess, final Session session) throws IMAPException {
        super(imapStore, imapAccess, session);
    }

    private MailAccount getMailAccount() throws MailException {
        if (mailAccount == null) {
            try {
                final MailAccountStorageService storageService = IMAPServiceRegistry.getService(MailAccountStorageService.class, true);
                mailAccount = storageService.getMailAccount(accountId, session.getUserId(), session.getContextId());
            } catch (final ServiceException e) {
                throw new MailException(e);
            } catch (final MailAccountException e) {
                throw new MailException(e);
            }
        }
        return mailAccount;
    }

    private Locale getLocale() throws MailException {
        if (locale == null) {
            try {
                final UserService userService = IMAPServiceRegistry.getService(UserService.class, true);
                locale = userService.getUser(session.getUserId(), ctx).getLocale();
            } catch (final ServiceException e) {
                throw new MailException(e);
            } catch (final UserException e) {
                throw new MailException(e);
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
    public MailMessage[] getMessagesLong(final String fullname, final long[] mailIds, final MailField[] mailFields) throws MailException {
        if ((mailIds == null) || (mailIds.length == 0)) {
            return EMPTY_RETVAL;
        }
        final MailFields fieldSet = new MailFields(mailFields);
        final boolean body;
        /*
         * Check for field FULL
         */
        if (fieldSet.contains(MailField.FULL)) {
            final MailMessage[] mails = new MailMessage[mailIds.length];
            for (int j = 0; j < mails.length; j++) {
                mails[j] = getMessageLong(fullname, mailIds[j], false);
            }
            return mails;
        }
        /*
         * Get messages with given fields filled
         */
        body = fieldSet.contains(MailField.BODY) || fieldSet.contains(MailField.FULL);
        try {
            imapFolder = setAndOpenFolder(imapFolder, fullname, Folder.READ_ONLY);
            /*
             * Fetch desired messages by given UIDs. Turn UIDs to corresponding sequence numbers to maintain order cause some IMAP servers
             * ignore the order of UIDs provided in a "UID FETCH" command.
             */
            final int[] seqNums = IMAPCommandsCollection.uids2SeqNums(imapFolder, mailIds);
            final Message[] messages = new Message[seqNums.length];
            final MailField[] fields = fieldSet.toArray();
            final FetchProfile fetchProfile = getFetchProfile(fields, getIMAPProperties().isFastFetch());
            final boolean isRev1 = imapConfig.getImapCapabilities().hasIMAP4rev1();
            int lastPos = 0;
            int pos = 0;
            while (pos < seqNums.length) {
                if (seqNums[pos] <= 0) {
                    final int len = pos - lastPos;
                    if (len > 0) {
                        fetchValidSeqNumsWithFallback(lastPos, len, seqNums, messages, fetchProfile, isRev1, body);
                    }
                    // Determine next valid position
                    pos++;
                    while (pos < seqNums.length && -1 == seqNums[pos]) {
                        pos++;
                    }
                    lastPos = pos;
                } else {
                    pos++;
                }
            }
            if (lastPos < pos) {
                fetchValidSeqNumsWithFallback(lastPos, pos - lastPos, seqNums, messages, fetchProfile, isRev1, body);
            }
            if (fieldSet.contains(MailField.ACCOUNT_NAME) || fieldSet.contains(MailField.FULL)) {
                return setAccountInfo(convert2Mails(messages, fields, body));
            }
            return convert2Mails(messages, fields, body);
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        }
    }

    private void fetchValidSeqNumsWithFallback(final int lastPos, final int len, final int[] seqNums, final Message[] messages, final FetchProfile fetchProfile, final boolean isRev1, final boolean body) throws MailException, MessagingException {
        try {
            fetchValidSeqNums(lastPos, len, seqNums, messages, fetchProfile, isRev1, body, false);
        } catch (final FolderClosedException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final StoreClosedException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final MessagingException e) {
            if (DEBUG) {
                LOG.debug("Fetch with BODYSTRUCTURE failed.", e);
            }
            fetchValidSeqNums(lastPos, len, seqNums, messages, fetchProfile, isRev1, body, true);
        }
    }

    private void fetchValidSeqNums(final int lastPos, final int len, final int[] seqNums, final Message[] messages, final FetchProfile fetchProfile, final boolean isRev1, final boolean body, final boolean ignoreBodystructure) throws MessagingException {
        final int[] subarr = new int[len];
        System.arraycopy(seqNums, lastPos, subarr, 0, len);
        final long start = System.currentTimeMillis();
        final Message[] submessages;
        if (ignoreBodystructure) {
            submessages =
                new FetchIMAPCommand(imapFolder, isRev1, subarr, FetchIMAPCommand.getSafeFetchProfile(fetchProfile), false, true, body).setDetermineAttachmentyHeader(
                    true).doCommand();
        } else {
            submessages = new FetchIMAPCommand(imapFolder, isRev1, subarr, fetchProfile, false, true, body).doCommand();
        }
        final long time = System.currentTimeMillis() - start;
        mailInterfaceMonitor.addUseTime(time);
        if (DEBUG) {
            LOG.debug(new StringBuilder(128).append("IMAP fetch for ").append(subarr.length).append(" messages took ").append(time).append(
                STR_MSEC).toString());
        }
        System.arraycopy(submessages, 0, messages, lastPos, submessages.length);
    }

    @Override
    public MailMessage getMessageLong(final String fullname, final long msgUID, final boolean markSeen) throws MailException {
        try {
            imapFolder = setAndOpenFolder(imapFolder, fullname, Folder.READ_WRITE);
            final IMAPMessage msg;
            {
                final long start = System.currentTimeMillis();
                msg = (IMAPMessage) imapFolder.getMessageByUID(msgUID);
                mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
            }
            if (msg == null) {
                // throw new MailException(MailException.Code.MAIL_NOT_FOUND,
                // String.valueOf(msgUID), imapFolder
                // .toString());
                return null;
            }
            // TODO: Examine behavior when applying: msg.setPeek(!markSeen);
            final MailMessage mail;
            try {
                mail = MIMEMessageConverter.convertMessage(msg);
            } catch (final MIMEMailException e) {
                if (MIMEMailException.Code.MESSAGE_REMOVED.getNumber() == e.getDetailNumber()) {
                    /*
                     * Obviously message was removed in the meantime
                     */
                    return null;
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
                            msg.setFlags(FLAGS_SEEN, true);
                            mail.setFlag(MailMessage.FLAG_SEEN, true);
                            mail.setUnreadMessages(mail.getUnreadMessages() <= 0 ? 0 : mail.getUnreadMessages() - 1);
                        }
                    } catch (final MessagingException e) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn(new StringBuilder("/SEEN flag could not be set on message #").append(mail.getMailId()).append(
                                " in folder ").append(mail.getFolder()).toString(), e);
                        }
                    }
                } else {
                    /*
                     * Switch \Seen flag
                     */
                    msg.setFlags(FLAGS_SEEN, true);
                    mail.setFlag(MailMessage.FLAG_SEEN, true);
                    mail.setUnreadMessages(mail.getUnreadMessages() <= 0 ? 0 : mail.getUnreadMessages() - 1);
                }
            }
            return setAccountInfo(mail);
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        }
    }

    @Override
    public MailMessage[] searchMessages(final String fullname, final IndexRange indexRange, final MailSortField sortField, final OrderDirection order, final SearchTerm<?> searchTerm, final MailField[] mailFields) throws MailException {
        try {
            imapFolder = setAndOpenFolder(imapFolder, fullname, Folder.READ_ONLY);
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
                if (MailSortField.RECEIVED_DATE.equals(sortField) && onlyLowCostFields(usedFields)) {
                    return performLowCostFetch(fullname, usedFields, order, indexRange);
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
        }
    }

    @Override
    public MailMessage[] getThreadSortedMessages(final String fullname, final IndexRange indexRange, final MailSortField sortField, final OrderDirection order, final SearchTerm<?> searchTerm, final MailField[] mailFields) throws MailException {
        try {
            if (!imapConfig.getImapCapabilities().hasThreadReferences()) {
                throw IMAPException.create(IMAPException.Code.THREAD_SORT_NOT_SUPPORTED, imapConfig, session, new Object[0]);
            }
            imapFolder = setAndOpenFolder(imapFolder, fullname, Folder.READ_ONLY);
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
            final int[] seqnums;
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
            usedFields.add(null == sortField ? MailField.RECEIVED_DATE : MailField.toField(sortField.getListField()));
            final FetchProfile fetchProfile = getFetchProfile(usedFields.toArray(), getIMAPProperties().isFastFetch());
            final boolean body = usedFields.contains(MailField.BODY) || usedFields.contains(MailField.FULL);
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
            Collections.sort(
                structuredList,
                new MailMessageComparator(MailSortField.RECEIVED_DATE, OrderDirection.DESC.equals(order), null));
            /*
             * Output as flat list
             */
            final List<MailMessage> flatList = new ArrayList<MailMessage>(msgs.length);
            ThreadSortUtil.toFlatList(structuredList, flatList);
            return flatList.toArray(new MailMessage[flatList.size()]);
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        }
    }

    @Override
    public MailMessage[] getUnreadMessages(final String fullname, final MailSortField sortField, final OrderDirection order, final MailField[] mailFields, final int limit) throws MailException {
        try {
            imapFolder = setAndOpenFolder(imapFolder, fullname, Folder.READ_ONLY);
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
                    IMAPCommandsCollection.getUnreadMessages(imapFolder, fields, sortField, getIMAPProperties().isFastFetch());
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
        }
    }

    @Override
    public void deleteMessagesLong(final String fullname, final long[] msgUIDs, final boolean hardDelete) throws MailException {
        try {
            imapFolder = setAndOpenFolder(imapFolder, fullname, Folder.READ_WRITE);
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
            if (hardDelete || usm.isHardDeleteMsgs()) {
                blockwiseDeletion(msgUIDs, false, null);
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
            final boolean backup = (!(fullname.startsWith(trashFullname)));
            blockwiseDeletion(msgUIDs, backup, backup ? trashFullname : null);
            IMAPSessionStorageAccess.removeDeletedSessionData(msgUIDs, accountId, session, fullname);
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        }
    }

    private void blockwiseDeletion(final long[] msgUIDs, final boolean backup, final String trashFullname) throws MailException, MessagingException {
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

    private void deleteByUIDs(final String trashFullname, final boolean backup, final long[] uids, final StringBuilder sb) throws MailException, MessagingException {
        if (backup) {
            /*
             * Copy messages to folder "TRASH"
             */
            try {
                if (DEBUG) {
                    final long start = System.currentTimeMillis();
                    new CopyIMAPCommand(imapFolder, uids, trashFullname, false, true).doCommand();
                    final long time = System.currentTimeMillis() - start;
                    sb.setLength(0);
                    LOG.debug(sb.append("\"Soft Delete\": ").append(uids.length).append(" messages copied to default trash folder \"").append(
                        trashFullname).append("\" in ").append(time).append(STR_MSEC).toString());
                } else {
                    new CopyIMAPCommand(imapFolder, uids, trashFullname, false, true).doCommand();
                }
            } catch (final MessagingException e) {
                if (e.getMessage().indexOf("Over quota") > -1) {
                    /*
                     * We face an Over-Quota-Exception
                     */
                    throw new MailException(MailException.Code.DELETE_FAILED_OVER_QUOTA, e, new Object[0]);
                }
                final Exception nestedExc = e.getNextException();
                if (nestedExc != null && nestedExc.getMessage().indexOf("Over quota") > -1) {
                    /*
                     * We face an Over-Quota-Exception
                     */
                    throw new MailException(MailException.Code.DELETE_FAILED_OVER_QUOTA, e, new Object[0]);
                }
                throw IMAPException.create(IMAPException.Code.MOVE_ON_DELETE_FAILED, imapConfig, session, e, new Object[0]);
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
        }
    }

    @Override
    public long[] copyMessagesLong(final String sourceFolder, final String destFolder, final long[] mailIds, final boolean fast) throws MailException {
        return copyOrMoveMessages(sourceFolder, destFolder, mailIds, false, fast);
    }

    @Override
    public long[] moveMessagesLong(final String sourceFolder, final String destFolder, final long[] mailIds, final boolean fast) throws MailException {
        if (DEFAULT_FOLDER_ID.equals(destFolder)) {
            throw IMAPException.create(IMAPException.Code.NO_ROOT_MOVE, imapConfig, session, new Object[0]);
        }
        return copyOrMoveMessages(sourceFolder, destFolder, mailIds, true, fast);
    }

    private long[] copyOrMoveMessages(final String sourceFullname, final String destFullname, final long[] mailIds, final boolean move, final boolean fast) throws MailException {
        try {
            if (null == mailIds) {
                throw IMAPException.create(IMAPException.Code.MISSING_PARAMETER, imapConfig, session, "mailIDs");
            } else if ((sourceFullname == null) || (sourceFullname.length() == 0)) {
                throw IMAPException.create(IMAPException.Code.MISSING_SOURCE_TARGET_FOLDER_ON_MOVE, imapConfig, session, "source");
            } else if ((destFullname == null) || (destFullname.length() == 0)) {
                throw IMAPException.create(IMAPException.Code.MISSING_SOURCE_TARGET_FOLDER_ON_MOVE, imapConfig, session, "target");
            } else if (sourceFullname.equals(destFullname) && move) {
                throw IMAPException.create(IMAPException.Code.NO_EQUAL_MOVE, imapConfig, session, sourceFullname);
            } else if (0 == mailIds.length) {
                // Nothing to move
                return new long[0];
            }
            /*
             * Open and check user rights on source folder
             */
            imapFolder = setAndOpenFolder(imapFolder, sourceFullname, Folder.READ_WRITE);
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
                final IMAPFolder destFolder = (IMAPFolder) imapStore.getFolder(destFullname);
                try {
                    if (!destFolder.exists()) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, destFullname);
                    }
                    if ((destFolder.getType() & Folder.HOLDS_MESSAGES) == 0) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, destFullname);
                    }
                } catch (final MessagingException e) {
                    throw IMAPException.handleMessagingException(e, imapConfig, session);
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
                    final long[] uids = copyOrMoveByUID(move, fast, destFullname, tmp, debug);
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
            final long[] uids = copyOrMoveByUID(move, fast, destFullname, remain, debug);
            System.arraycopy(uids, 0, result, offset, uids.length);
            if (move) {
                /*
                 * Force folder cache update through a close
                 */
                imapFolder.close(false);
                resetIMAPFolder();
            }
            final String draftFullname = imapAccess.getFolderStorage().getDraftsFolder();
            if (destFullname.equals(draftFullname)) {
                /*
                 * A copy/move to drafts folder. Ensure to set \Draft flag.
                 */
                final IMAPFolder destFolder = setAndOpenFolder(destFullname, Folder.READ_WRITE);
                try {
                    if (destFolder.getMessageCount() > 0) {
                        if (DEBUG) {
                            final long start = System.currentTimeMillis();
                            new FlagsIMAPCommand(destFolder, FLAGS_DRAFT, true, true).doCommand();
                            final long time = System.currentTimeMillis() - start;
                            LOG.debug(new StringBuilder(128).append(
                                "A copy/move to default drafts folder => All messages' \\Draft flag in ").append(destFullname).append(
                                " set in ").append(time).append(STR_MSEC).toString());
                        } else {
                            new FlagsIMAPCommand(destFolder, FLAGS_DRAFT, true, true).doCommand();
                        }
                    }
                } finally {
                    destFolder.close(false);
                }
            } else if (sourceFullname.equals(draftFullname)) {
                /*
                 * A copy/move from drafts folder. Ensure to unset \Draft flag.
                 */
                final IMAPFolder destFolder = setAndOpenFolder(destFullname, Folder.READ_WRITE);
                try {
                    if (DEBUG) {
                        final long start = System.currentTimeMillis();
                        new FlagsIMAPCommand(destFolder, FLAGS_DRAFT, false, true).doCommand();
                        final long time = System.currentTimeMillis() - start;
                        LOG.debug(new StringBuilder(128).append("A copy/move from default drafts folder => All messages' \\Draft flag in ").append(
                            destFullname).append(" unset in ").append(time).append(STR_MSEC).toString());
                    } else {
                        new FlagsIMAPCommand(destFolder, FLAGS_DRAFT, false, true).doCommand();
                    }
                } finally {
                    destFolder.close(false);
                }
            }
            if (move) {
                IMAPSessionStorageAccess.removeDeletedSessionData(mailIds, accountId, session, sourceFullname);
            }
            return result;
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        }
    }

    private long[] copyOrMoveByUID(final boolean move, final boolean fast, final String destFullname, final long[] tmp, final StringBuilder sb) throws MessagingException, MailException, IMAPException {
        long[] uids;
        if (DEBUG) {
            final long start = System.currentTimeMillis();
            uids = new CopyIMAPCommand(imapFolder, tmp, destFullname, false, fast).doCommand();
            final long time = System.currentTimeMillis() - start;
            sb.setLength(0);
            LOG.debug(sb.append(tmp.length).append(" messages copied in ").append(time).append(STR_MSEC).toString());
        } else {
            uids = new CopyIMAPCommand(imapFolder, tmp, destFullname, false, fast).doCommand();
        }
        if (!fast && ((uids == null) || noUIDsAssigned(uids, tmp.length))) {
            /*
             * Invalid UIDs
             */
            uids = getDestinationUIDs(tmp, destFullname);
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
    public long[] appendMessagesLong(final String destFullname, final MailMessage[] mailMessages) throws MailException {
        if (null == mailMessages || mailMessages.length == 0) {
            return new long[0];
        }
        try {
            /*
             * Open and check user rights on source folder
             */
            imapFolder = setAndOpenFolder(imapFolder, destFullname, Folder.READ_WRITE);
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
            /*
             * Convert messages to JavaMail message objects
             */
            final Message[] msgs = MIMEMessageConverter.convertMailMessages(mailMessages, true);
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
            if (hasUIDPlus) {
                // Perform append expecting APPENUID response code
                retval = checkAndConvertAppendUID(imapFolder.appendUIDMessages(msgs));
            } else {
                // Perform simple append
                imapFolder.appendMessages(msgs);
            }
            if (retval.length > 0) {
                /*
                 * Close affected IMAP folder to ensure consistency regarding IMAFolder's internal cache.
                 */
                notifyIMAPFolderModification(destFullname);
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
            notifyIMAPFolderModification(destFullname);
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
        }
    }

    @Override
    public void updateMessageFlagsLong(final String fullname, final long[] msgUIDs, final int flagsArg, final boolean set) throws MailException {
        if (null == msgUIDs || 0 == msgUIDs.length) {
            // Nothing to do
            return;
        }
        try {
            imapFolder = setAndOpenFolder(imapFolder, fullname, Folder.READ_WRITE);
            /*
             * Remove non user-alterable system flags
             */
            int flags = flagsArg;
            if (((flags & MailMessage.FLAG_RECENT) > 0)) {
                flags = flags ^ MailMessage.FLAG_RECENT;
            }
            if (((flags & MailMessage.FLAG_USER) > 0)) {
                flags = flags ^ MailMessage.FLAG_USER;
            }
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
                affectedFlags.add(Flags.Flag.DELETED);
                applyFlags = true;
            }
            if (((flags & MailMessage.FLAG_DRAFT) > 0)) {
                if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                    throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
                affectedFlags.add(Flags.Flag.DRAFT);
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
                handleSpamByUID(msgUIDs, set, true, fullname, Folder.READ_WRITE);
            } else {
                /*
                 * Force JavaMail's cache update through folder closure
                 */
                imapFolder.close(false);
                resetIMAPFolder();
            }
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        }
    }

    @Override
    public void updateMessageColorLabelLong(final String fullname, final long[] msgUIDs, final int colorLabel) throws MailException {
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
            imapFolder = setAndOpenFolder(imapFolder, fullname, Folder.READ_WRITE);
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
        }
    }

    @Override
    public MailMessage saveDraft(final String draftFullname, final ComposedMailMessage composedMail) throws MailException {
        try {
            final MimeMessage mimeMessage = new MimeMessage(imapAccess.getSession());
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
                mimeMessage.setFlag(Flags.Flag.DRAFT, true);
                mimeMessage.saveChanges();
                /*
                 * Append message to draft folder
                 */
                uid = appendMessagesLong(draftFullname, new MailMessage[] { MIMEMessageConverter.convertMessage(mimeMessage) })[0];
            } finally {
                composedMail.cleanUp();
            }
            /*
             * Check for draft-edit operation: Delete old version
             */
            final MailPath msgref = composedMail.getMsgref();
            if (msgref != null && draftFullname.equals(msgref.getFolder())) {
                if (accountId != msgref.getAccountId()) {
                    LOG.warn(
                        new StringBuilder("Differing account ID in msgref attribute.\nMessage storage account ID: ").append(accountId).append(
                            ".\nmsgref account ID: ").append(msgref.getAccountId()).toString(),
                        new Throwable());
                }
                deleteMessagesLong(msgref.getFolder(), new long[] { Long.parseLong(msgref.getMailID()) }, true);
                composedMail.setMsgref(null);
            }
            /*
             * Force folder update
             */
            notifyIMAPFolderModification(draftFullname);
            /*
             * Return draft mail
             */
            return getMessageLong(draftFullname, uid, true);
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        } catch (final IOException e) {
            throw IMAPException.create(IMAPException.Code.IO_ERROR, imapConfig, session, e, e.getMessage());
        }
    }

    @Override
    public MailMessage[] getNewAndModifiedMessages(final String folder, final MailField[] fields) throws MailException {
        // TODO: Needs to be thoroughly tested
        return EMPTY_RETVAL;
        // return getChangedMessages(folder, fields, 0);
    }

    @Override
    public MailMessage[] getDeletedMessages(final String folder, final MailField[] fields) throws MailException {
        // TODO: Needs to be thoroughly tested
        return EMPTY_RETVAL;
        // return getChangedMessages(folder, fields, 1);
    }

    private MailMessage[] getChangedMessages(final String folder, final MailField[] fields, final int index) throws MailException {
        try {
            imapFolder = setAndOpenFolder(imapFolder, folder, Folder.READ_ONLY);
            try {
                if (!holdsMessages()) {
                    throw IMAPException.create(
                        IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES,
                        imapConfig,
                        session,
                        imapFolder.getFullName());
                }
            } catch (final MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, imapFolder.getFullName());
            }
            final long[] uids = IMAPSessionStorageAccess.getChanges(accountId, imapFolder, session, index + 1)[index];
            return getMessagesLong(folder, uids, fields);
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig, session);
        }
    }

    /*-
     * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     * +++++++++++++++++ Helper methods +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     */

    /**
     * Performs the FETCH command on currently active IMAP folder on all messages using the 1:* sequence range argument.
     * 
     * @param fullname The IMAP folder's fullname
     * @param lowCostFields The low-cost fields
     * @param order The order direction (needed to possibly flip the results)
     * @return The fetched mail messages with only ID and folder ID set.
     * @throws MessagingException If a messaging error occurs
     * @throws MailException If a mail error occurs
     */
    private MailMessage[] performLowCostFetch(final String fullname, final MailFields lowCostFields, final OrderDirection order, final IndexRange indexRange) throws MessagingException, MailException {
        /*
         * Perform simple fetch
         */
        MailMessage[] retval;
        {
            final LowCostItem[] lowCostItems = getLowCostItems(lowCostFields);
            final long start = System.currentTimeMillis();
            retval = AllFetch.fetchLowCost(imapFolder, lowCostItems, OrderDirection.ASC.equals(order), imapConfig, session);
            mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
            if (DEBUG) {
                LOG.debug(new StringBuilder(128).append(fullname).append(": IMAP all fetch >>>FETCH 1:* (").append(
                    AllFetch.getFetchCommand(lowCostItems)).append(")<<< took ").append((System.currentTimeMillis() - start)).append(
                    STR_MSEC).toString(), new Throwable());
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

    private static final MailFields FIELDS_ENV =
        new MailFields(
            new MailField[] { MailField.SENT_DATE, MailField.FROM, MailField.TO, MailField.CC, MailField.BCC, MailField.SUBJECT });

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

    private static final EnumSet<MailField> LOW_COST =
        EnumSet.of(
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
     * @param destFullname The destination folder's fullname
     * @return The corresponding UIDs in destination folder
     * @throws MessagingException
     * @throws IMAPException
     */
    private long[] getDestinationUIDs(final long[] msgUIDs, final String destFullname) throws MessagingException, IMAPException {
        /*
         * No COPYUID present in response code. Since UIDs are assigned in strictly ascending order in the mailbox (refer to IMAPv4 rfc3501,
         * section 2.3.1.1), we can discover corresponding UIDs by selecting the destination mailbox and detecting the location of messages
         * placed in the destination mailbox by using FETCH and/or SEARCH commands (e.g., for Message-ID or some unique marker placed in the
         * message in an APPEND).
         */
        final long[] retval = new long[msgUIDs.length];
        Arrays.fill(retval, -1L);
        if (!IMAPCommandsCollection.canBeOpened(imapFolder, destFullname, Folder.READ_ONLY)) {
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
            final IMAPFolder destFolder = (IMAPFolder) imapStore.getFolder(destFullname);
            destFolder.open(Folder.READ_ONLY);
            try {
                /*
                 * Find this message ID in destination folder
                 */
                long startUID = IMAPCommandsCollection.messageId2UID(messageId, destFolder);
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

    private void handleSpamByUID(final long[] msgUIDs, final boolean isSpam, final boolean move, final String fullname, final int desiredMode) throws MessagingException, MailException {
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
                try {
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
                    imapFolder = setAndOpenFolder(imapFolder, fullname, desiredMode);
                } catch (final MailException e) {
                    throw new IMAPException(e);
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
            try {
                IMAPProvider.getInstance().getSpamHandler().handleHam(
                    accountId,
                    imapFolder.getFullName(),
                    longs2uids(msgUIDs),
                    move,
                    session);
                /*
                 * Close and reopen to force internal message cache update
                 */
                resetIMAPFolder();
                imapFolder = setAndOpenFolder(imapFolder, fullname, desiredMode);
            } catch (final MailException e) {
                throw new IMAPException(e);
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
     * @throws MailException If mail account cannot be obtained
     */
    private MailMessage setAccountInfo(final MailMessage mailMessage) throws MailException {
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
     * @throws MailException If mail account cannot be obtained
     */
    private MailMessage[] setAccountInfo(final MailMessage[] mailMessages) throws MailException {
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

    private MailMessage[] convert2Mails(final Message[] msgs, final MailField[] fields) throws MailException {
        return convert2Mails(msgs, fields, false);
    }

    private MailMessage[] convert2Mails(final Message[] msgs, final MailField[] fields, final boolean includeBody) throws MailException {
        return MIMEMessageConverter.convertMessages(msgs, fields, includeBody);
    }

}
