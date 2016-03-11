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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

import static com.openexchange.imap.threader.Threadables.applyThreaderTo;
import static com.openexchange.mail.mime.utils.MimeStorageUtility.getFetchProfile;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.mail.FetchProfile;
import javax.mail.Message;
import javax.mail.MessagingException;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Reloadable;
import com.openexchange.exception.OXException;
import com.openexchange.imap.command.MailMessageFillerIMAPCommand;
import com.openexchange.imap.command.MessageFetchIMAPCommand;
import com.openexchange.imap.command.SimpleFetchIMAPCommand;
import com.openexchange.imap.config.IMAPReloadable;
import com.openexchange.imap.search.IMAPSearch;
import com.openexchange.imap.services.Services;
import com.openexchange.imap.threader.Threadable;
import com.openexchange.imap.threader.Threadables;
import com.openexchange.imap.threader.references.Conversation;
import com.openexchange.imap.threader.references.ConversationCache;
import com.openexchange.imap.threader.references.Conversations;
import com.openexchange.imap.threadsort.MessageInfo;
import com.openexchange.imap.threadsort.ThreadSortNode;
import com.openexchange.imap.threadsort.ThreadSortUtil;
import com.openexchange.imap.threadsort2.ThreadSorts;
import com.openexchange.mail.IndexRange;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailField;
import com.openexchange.mail.MailFields;
import com.openexchange.mail.MailSortField;
import com.openexchange.mail.OrderDirection;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.ThreadSortMailMessage;
import com.openexchange.mail.mime.ExtendedMimeMessage;
import com.openexchange.mail.search.SearchTerm;
import com.openexchange.mail.utils.MailMessageComparator;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.session.Session;
import com.openexchange.threadpool.AbstractTask;
import com.openexchange.threadpool.ThreadPools;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TLongObjectProcedure;
import gnu.trove.set.hash.TIntHashSet;

/**
 * {@link IMAPConversationWorker} - The IMAP implementation of message storage.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class IMAPConversationWorker {

    private static volatile Boolean useImapThreaderIfSupported;
    /** <b>Only</b> applies to: getThreadSortedMessages(...) in ISimplifiedThreadStructure. Default is <code>false</code> */
    static boolean useImapThreaderIfSupported() {
        Boolean b = useImapThreaderIfSupported;
        if (null == b) {
            synchronized (IMAPConversationWorker.class) {
                b = useImapThreaderIfSupported;
                if (null == b) {
                    final ConfigurationService service = Services.getService(ConfigurationService.class);
                    b = Boolean.valueOf(null != service && service.getBoolProperty("com.openexchange.imap.useImapThreaderIfSupported", false));
                    useImapThreaderIfSupported = b;
                }
            }
        }
        return b.booleanValue();
    }

    static {
        IMAPReloadable.getInstance().addReloadable(new Reloadable() {

            @SuppressWarnings("synthetic-access")
            @Override
            public void reloadConfiguration(final ConfigurationService configService) {
                useImapThreaderIfSupported = null;
            }

            @Override
            public Map<String, String[]> getConfigFileNames() {
                return null;
            }
        });
    }

    /*-
     * Members
     */

    private final IMAPMessageStorage imapMessageStorage;
    private final IMAPFolderStorage imapFolderStorage;

    /**
     * Initializes a new {@link IMAPConversationWorker}.
     *
     * @param imapMessageStorage The connected IMAP message storage
     * @param imapFolderStorage The connected IMAP folder storage
     */
    public IMAPConversationWorker(IMAPMessageStorage imapMessageStorage, IMAPFolderStorage imapFolderStorage) {
        super();
        this.imapMessageStorage = imapMessageStorage;
        this.imapFolderStorage = imapFolderStorage;
    }

    private static final MailMessageComparator COMPARATOR_DESC = new MailMessageComparator(MailSortField.RECEIVED_DATE, true, null);

    private static final int CONVERSATION_CACHE_THRESHOLD = 10000;

    /**
     * Gets the message conversations
     *
     * @param fullName The full name
     * @param includeSent Whether to include sent messages
     * @param cache Currently unused
     * @param indexRange The index range
     * @param max The max. number of messages
     * @param sortField The sort field
     * @param order The sort order
     * @param mailFields The mail fields to set
     * @return The message conversations
     * @throws OXException If message conversations cannot be returned
     */
    public List<List<MailMessage>> getThreadSortedMessages(final String fullName, final boolean includeSent, final boolean cache, final IndexRange indexRange, final long max, final MailSortField sortField, final OrderDirection order, final MailField[] mailFields) throws OXException {
        IMAPFolder sentFolder = null;
        try {
            final String sentFullName = imapFolderStorage.getSentFolder();
            imapMessageStorage.openReadOnly(fullName);
            final int messageCount = imapMessageStorage.getImapFolder().getMessageCount();
            if (0 >= messageCount || (null != indexRange && (indexRange.end - indexRange.start) < 1)) {
                return Collections.emptyList();
            }
            int lookAhead;
            if (max <= 0) {
                lookAhead = -1;
            } else {
                lookAhead = 1000;
                if (null != indexRange) {
                    while (indexRange.end >= (lookAhead / 2)) {
                        lookAhead = lookAhead + 1000;
                    }
                }
                if (lookAhead > messageCount) {
                    lookAhead = -1;
                }
            }
            final boolean mergeWithSent = includeSent && !sentFullName.equals(fullName);
            /*
             * Sort messages by thread reference
             */
            final MailFields usedFields = new MailFields(mailFields);
            IMAPMessageStorage.prepareMailFieldsForVirtualFolder(usedFields, fullName);
            usedFields.add(MailField.THREAD_LEVEL);
            usedFields.add(MailField.RECEIVED_DATE);
            usedFields.add(null == sortField ? MailField.RECEIVED_DATE : MailField.toField(sortField.getListField()));
            final boolean body = usedFields.contains(MailField.BODY) || usedFields.contains(MailField.FULL);
            if (body && mergeWithSent) {
                throw MailExceptionCode.ILLEGAL_ARGUMENT.create();
            }
            if (useImapThreaderIfSupported() && imapMessageStorage.getImapConfig().getImapCapabilities().hasThreadReferences()) {
                return doImapThreadSort(fullName, indexRange, sortField, order, sentFullName, messageCount, lookAhead, mergeWithSent, mailFields);
            }
            // Use built-in algorithm
            return doReferenceOnlyThreadSort(fullName, indexRange, sortField, order, sentFullName, lookAhead, mergeWithSent, mailFields);
        } catch (final MessagingException e) {
            throw imapMessageStorage.handleMessagingException(fullName, e);
        } catch (final RuntimeException e) {
            throw imapMessageStorage.handleRuntimeException(e);
        } finally {
            IMAPMessageStorage.closeSafe(sentFolder);
            IMAPFolderWorker.clearCache(imapMessageStorage.getImapFolder());
        }
    }

    private static final MailFields FIELDS_FLAGS = new MailFields(MailField.FLAGS, MailField.COLOR_LABEL);

    private List<List<MailMessage>> doReferenceOnlyThreadSort(final String fullName, final IndexRange indexRange, final MailSortField sortField, final OrderDirection order, final String sentFullName, int lookAhead, final boolean mergeWithSent, final MailField[] mailFields) throws MessagingException, OXException {
        final MailFields usedFields = new MailFields(mailFields);
        usedFields.add(MailField.THREAD_LEVEL);
        usedFields.add(MailField.RECEIVED_DATE);
        usedFields.add(null == sortField ? MailField.RECEIVED_DATE : MailField.toField(sortField.getListField()));
        final boolean body = usedFields.contains(MailField.BODY) || usedFields.contains(MailField.FULL);
        if (body && mergeWithSent) {
            throw MailExceptionCode.ILLEGAL_ARGUMENT.create();
        }
        final boolean isRev1 = imapMessageStorage.getImapConfig().getImapCapabilities().hasIMAP4rev1();

        // Check cache
        final ConversationCache conversationCache = ConversationCache.getInstance();
        if (false == body) {
            if (conversationCache.containsCachedConversations(fullName, imapMessageStorage.getAccountId(), imapMessageStorage.getSession())) {
                int total = imapMessageStorage.getImapFolder().getMessageCount();
                long uidNext = imapMessageStorage.getImapFolder().getUIDNext();
                int sentTotal = 0;
                long sentUidNext = 0L;
                if (mergeWithSent) {
                    // Switch folder
                    imapMessageStorage.openReadOnly(sentFullName);

                    sentTotal = imapMessageStorage.getImapFolder().getMessageCount();
                    sentUidNext = imapMessageStorage.getImapFolder().getUIDNext();

                    // Switch back folder
                    imapMessageStorage.openReadOnly(fullName);
                }

                String argsHash = ConversationCache.getArgsHash(sortField, order, lookAhead, mergeWithSent, usedFields, total, uidNext, sentTotal, sentUidNext);
                List<List<MailMessage>> list = conversationCache.getCachedConversations(fullName, imapMessageStorage.getAccountId(), argsHash, imapMessageStorage.getSession());
                if (null != list) {
                    // Slice & fill with recent flags
                    if (usedFields.containsAny(FIELDS_FLAGS)) {
                        return sliceAndFill(list, fullName, indexRange, sentFullName, mergeWithSent, FIELDS_FLAGS, body, isRev1);
                    }
                    return sliceMessages(list, indexRange);
                }
            }
        }

        // No suitable cache content - Generate from scratch
        conversationCache.removeAccountConversations(imapMessageStorage.getAccountId(), imapMessageStorage.getSession());

        // Define the behavior how to query the conversation-relevant information from IMAP; either via ENVELOPE or by dedicated headers
        final boolean byEnvelope = false;

        // Grab conversations
        final String argsHash;
        List<Conversation> conversations;
        {
            // Retrieve from actual folder
            int total = imapMessageStorage.getImapFolder().getMessageCount();
            long uidNext = imapMessageStorage.getImapFolder().getUIDNext();
            FetchProfile fp;
            if (byEnvelope) {
                fp = Conversations.getFetchProfileConversationByEnvelope(null == sortField ? MailField.RECEIVED_DATE : MailField.toField(sortField.getListField()));
            } else {
                fp = Conversations.getFetchProfileConversationByHeaders(null == sortField ? MailField.RECEIVED_DATE : MailField.toField(sortField.getListField()));
            }
            conversations = Conversations.conversationsFor(imapMessageStorage.getImapFolder(), lookAhead, order, fp, imapMessageStorage.getImapServerInfo(), byEnvelope);
            // Retrieve from sent folder
            int sentTotal = 0;
            long sentUidNext = 0L;
            if (mergeWithSent) {
                // Switch folder
                imapMessageStorage.openReadOnly(sentFullName);
                sentTotal = imapMessageStorage.getImapFolder().getMessageCount();
                sentUidNext = imapMessageStorage.getImapFolder().getUIDNext();
                // Get sent messages
                List<MailMessage> sentMessages = Conversations.messagesFor(imapMessageStorage.getImapFolder(), lookAhead, order, fp, imapMessageStorage.getImapServerInfo(), byEnvelope);
                if (false == sentMessages.isEmpty()) {
                    // Filter messages already contained in conversations
                    {
                        Set<String> allMessageIds = new HashSet<String>(conversations.size());
                        for (final Conversation conversation : conversations) {
                            conversation.addMessageIdsTo(allMessageIds);
                        }
                        List<MailMessage> tmp = null;
                        for (MailMessage sentMessage : sentMessages) {
                            if (false == allMessageIds.contains(sentMessage.getMessageId())) {
                                if (null == tmp) {
                                    tmp = new LinkedList<MailMessage>();
                                }
                                tmp.add(sentMessage);
                            }
                        }
                        if (null != tmp) {
                            sentMessages = tmp;
                        }
                    }

                    if (false == sentMessages.isEmpty()) {
                        // Add to conversation if references or referenced-by
                        for (Conversation conversation : conversations) {
                            for (MailMessage sentMessage : sentMessages) {
                                if (conversation.referencesOrIsReferencedBy(sentMessage)) {
                                    conversation.addMessage(sentMessage);
                                }
                            }
                        }
                    }
                }
                // Switch back folder
                imapMessageStorage.openReadOnly(fullName);
            }
            argsHash = body ? null : ConversationCache.getArgsHash(sortField, order, lookAhead, mergeWithSent, usedFields, total, uidNext, sentTotal, sentUidNext);
        }
        // Fold it
        Conversations.fold(conversations);
        // Comparator
        MailMessageComparator threadComparator = COMPARATOR_DESC;
        // Sort
        final List<List<MailMessage>> list = new LinkedList<List<MailMessage>>();
        for (final Conversation conversation : conversations) {
            list.add(conversation.getMessages(threadComparator));
        }
        conversations = null;
        // Sort root elements
        {
            MailSortField effectiveSortField = null == sortField ? MailSortField.RECEIVED_DATE : sortField;
            Comparator<List<MailMessage>> listComparator = getListComparator(effectiveSortField, order, fullName, imapMessageStorage.getLocale());
            Collections.sort(list, listComparator);
        }
        // Slice & fill
        if (body || (lookAhead > CONVERSATION_CACHE_THRESHOLD)) {
            // Body requested - Do not cache at all
            return sliceAndFill(list, fullName, indexRange, sentFullName, mergeWithSent, usedFields, body, isRev1);
        }

        // Check for requested slice
        if (null == indexRange) {
            // Fill (except flags)
            fillMessages(list, fullName, sentFullName, mergeWithSent, usedFields, body, isRev1);
            // Put into cache
            conversationCache.putCachedConversations(list, fullName, imapMessageStorage.getAccountId(), argsHash, imapMessageStorage.getSession());
            // All
            return list;
        }

        // Load slices in a separate thread?
        boolean loadSeparately = true;
        if (!loadSeparately) {
            // Fill
            fillMessages(list, fullName, sentFullName, mergeWithSent, usedFields, body, isRev1);
            // Put into cache
            conversationCache.putCachedConversations(list, fullName, imapMessageStorage.getAccountId(), argsHash, imapMessageStorage.getSession());
            // Slice
            return sliceMessages(list, indexRange);
        }

        // Use a separate thread...
        Object[] parts = slicePartsFrom(list, indexRange);
        @SuppressWarnings("unchecked")
        final List<List<MailMessage>> first = (List<List<MailMessage>>) parts[0];
        @SuppressWarnings("unchecked")
        List<List<MailMessage>> slice = (List<List<MailMessage>>) parts[1];
        if (null == slice) {
            // Return empty iterator if start is out of range
            return Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        final List<List<MailMessage>> rest = (List<List<MailMessage>>) parts[2];
        parts = null;

        // Fill slice with this thread
        fillMessages(slice, fullName, sentFullName, mergeWithSent, usedFields, body, isRev1);

        // Fill others with another thread & put complete list into cache after all filled
        if (null != first || null != rest) {
            final MailAccount mailAccount = imapMessageStorage.getMailAccount();
            final Session ses = imapMessageStorage.getSession();
            final IMAPMessageStorage imapMessageStorage = this.imapMessageStorage;
            AbstractTask<Void> t = new AbstractTask<Void>() {

                @Override
                public Void call() throws Exception {
                    MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
                    try {
                        mailAccess = MailAccess.getInstance(ses, imapMessageStorage.getAccountId());
                        mailAccess.connect();
                        IMAPStore imapStore = IMAPMessageStorage.getImapMessageStorageFrom(mailAccess).getImapStore();

                        if (null != first) {
                            fillMessagesStatic(first, fullName, sentFullName, mergeWithSent, usedFields, isRev1, imapStore, imapMessageStorage.getImapServerInfo(), mailAccount);
                        }
                        if (null != rest) {
                            fillMessagesStatic(rest, fullName, sentFullName, mergeWithSent, usedFields, isRev1, imapStore, imapMessageStorage.getImapServerInfo(), mailAccount);
                        }
                    } finally {
                        if (null != mailAccess) {
                            mailAccess.close(true);
                        }
                    }

                    // Put into cache
                    conversationCache.putCachedConversations(list, fullName, imapMessageStorage.getAccountId(), argsHash, imapMessageStorage.getSession());

                    return null;
                }
            };
            ThreadPools.getThreadPool().submit(t);
        } else {
            // Put into cache
            conversationCache.putCachedConversations(slice, fullName, imapMessageStorage.getAccountId(), argsHash, imapMessageStorage.getSession());
        }

        return slice;
    }

    private List<List<MailMessage>> sliceAndFill(List<List<MailMessage>> listOfConversations, String fullName, IndexRange indexRange, String sentFullName, boolean mergeWithSent, MailFields usedFields, boolean body, boolean isRev1) throws MessagingException, OXException {
        // Check for index range
        List<List<MailMessage>> list = sliceMessages(listOfConversations, indexRange);
        // Fill requested fields
        fillMessages(list, fullName, sentFullName, mergeWithSent, usedFields, body, isRev1);
        // Return list
        return list;
    }

    private Object[] slicePartsFrom(List<List<MailMessage>> listOfConversations, IndexRange indexRange) {
        List<List<MailMessage>> list = listOfConversations;
        // Check for index range
        int fromIndex = indexRange.start;
        int size = list.size();
        if ((fromIndex) > size) {
            // Return empty iterator if start is out of range
            return new Object[] { list, null, null };
        }
        // Reset end index if out of range
        int toIndex = indexRange.end;
        if (toIndex >= size) {
            if (fromIndex == 0) {
                return new Object[] { null, list, null };
            }
            toIndex = size;
        }
        return new Object[] { fromIndex > 0 ? list.subList(0, fromIndex) : null, list.subList(fromIndex, toIndex), toIndex < size ? list.subList(toIndex, size) : null };
    }

    private List<List<MailMessage>> sliceMessages(List<List<MailMessage>> listOfConversations, IndexRange indexRange) {
        List<List<MailMessage>> list = listOfConversations;
        // Check for index range
        if (null != indexRange) {
            int fromIndex = indexRange.start;
            int toIndex = indexRange.end;
            int size = list.size();
            if ((fromIndex) > size) {
                // Return empty iterator if start is out of range
                return Collections.emptyList();
            }
            // Reset end index if out of range
            if (toIndex >= size) {
                if (fromIndex == 0) {
                    return list;
                }
                toIndex = size;
            }
            list = list.subList(fromIndex, toIndex);
        }
        // Return list
        return list;
    }

    static void fillMessagesStatic(List<List<MailMessage>> list, String fullName, String sentFullName, boolean mergeWithSent, MailFields usedFields, boolean isRev1, IMAPStore imapStore, IMAPServerInfo imapServerInfo, MailAccount mailAccount) throws MessagingException, OXException {
        IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(fullName);
        imapFolder.open(IMAPFolder.READ_ONLY);
        try {
            if (mergeWithSent) {
                FetchProfile fetchProfile = IMAPMessageStorage.checkFetchProfile(getFetchProfile(usedFields.toArray(), true));
                List<MailMessage> msgs = new LinkedList<MailMessage>();
                List<MailMessage> sentmsgs = new LinkedList<MailMessage>();
                for (List<MailMessage> conversation : list) {
                    for (MailMessage m : conversation) {
                        if (sentFullName.equals(m.getFolder())) {
                            sentmsgs.add(m);
                        } else {
                            msgs.add(m);
                        }
                    }
                }
                new MailMessageFillerIMAPCommand(msgs, isRev1, fetchProfile, imapServerInfo, imapFolder).doCommand();
                if (!sentmsgs.isEmpty()) {
                    // Switch folder
                    imapFolder.close(false);
                    imapFolder = (IMAPFolder) imapStore.getFolder(sentFullName);
                    imapFolder.open(IMAPFolder.READ_ONLY);
                    new MailMessageFillerIMAPCommand(sentmsgs, isRev1, fetchProfile, imapServerInfo, imapFolder).doCommand();
                }
            } else {
                List<MailMessage> msgs = new LinkedList<MailMessage>();
                for (List<MailMessage> conversation : list) {
                    msgs.addAll(conversation);
                }
                new MailMessageFillerIMAPCommand(msgs, isRev1, getFetchProfile(usedFields.toArray(), true), imapServerInfo, imapFolder).doCommand();
            }
            /*
             * Apply account identifier
             */
            IMAPMessageStorage.setAccountInfo2(list, mailAccount);
        } finally {
            imapFolder.close(false);
        }
    }

    private void fillMessages(List<List<MailMessage>> list, String fullName, String sentFullName, boolean mergeWithSent, MailFields usedFields, boolean body, boolean isRev1) throws MessagingException, OXException {
        // Fill messages
        if (mergeWithSent) {
            FetchProfile fetchProfile = IMAPMessageStorage.checkFetchProfile(getFetchProfile(usedFields.toArray(), true));
            List<MailMessage> msgs = new LinkedList<MailMessage>();
            List<MailMessage> sentmsgs = new LinkedList<MailMessage>();
            for (List<MailMessage> conversation : list) {
                for (MailMessage m : conversation) {
                    if (sentFullName.equals(m.getFolder())) {
                        sentmsgs.add(m);
                    } else {
                        msgs.add(m);
                    }
                }
            }
            new MailMessageFillerIMAPCommand(msgs, isRev1, fetchProfile, imapMessageStorage.getImapServerInfo(), imapMessageStorage.getImapFolder()).doCommand();
            if (!sentmsgs.isEmpty()) {
                // Switch folder
                imapMessageStorage.openReadOnly(sentFullName);
                new MailMessageFillerIMAPCommand(sentmsgs, isRev1, fetchProfile, imapMessageStorage.getImapServerInfo(), imapMessageStorage.getImapFolder()).doCommand();
                // Switch back folder
                imapMessageStorage.openReadOnly(fullName);
            }
        } else {
            if (body) {
                List<List<MailMessage>> newlist = new LinkedList<List<MailMessage>>();
                for (List<MailMessage> conversation : list) {
                    List<MailMessage> newconversation = new LinkedList<MailMessage>();
                    for (MailMessage mailMessage : conversation) {
                        newconversation.add(imapMessageStorage.getMessage(mailMessage.getFolder(), mailMessage.getMailId(), false));
                    }
                    newlist.add(newconversation);
                }
                list = newlist;
            } else {
                List<MailMessage> msgs = new LinkedList<MailMessage>();
                for (List<MailMessage> conversation : list) {
                    msgs.addAll(conversation);
                }
                new MailMessageFillerIMAPCommand(msgs, isRev1, getFetchProfile(usedFields.toArray(), true), imapMessageStorage.getImapServerInfo(), imapMessageStorage.getImapFolder()).doCommand();
            }
        }
        /*
         * Apply account identifier
         */
        imapMessageStorage.setAccountInfo2(list);
    }

    private List<List<MailMessage>> doImapThreadSort(final String fullName, final IndexRange indexRange, final MailSortField sortField, final OrderDirection order, final String sentFullName, final int messageCount, int lookAhead, final boolean mergeWithSent, final MailField[] mailFields) throws OXException, MessagingException {
        // Parse THREAD response to a list structure

        final MailFields usedFields = new MailFields(mailFields);
        usedFields.add(MailField.THREAD_LEVEL);
        usedFields.add(MailField.RECEIVED_DATE);
        usedFields.add(null == sortField ? MailField.RECEIVED_DATE : MailField.toField(sortField.getListField()));
        final boolean body = usedFields.contains(MailField.BODY) || usedFields.contains(MailField.FULL);
        if (body && mergeWithSent) {
            throw MailExceptionCode.ILLEGAL_ARGUMENT.create();
        }
        final boolean byEnvelope = false;
        final boolean isRev1 = imapMessageStorage.getImapConfig().getImapCapabilities().hasIMAP4rev1();

        List<List<MailMessage>> list;
        if (mergeWithSent) {
            FetchProfile fp;
            if (byEnvelope) {
                fp = Conversations.getFetchProfileConversationByEnvelope(null == sortField ? MailField.RECEIVED_DATE : MailField.toField(sortField.getListField()));
            } else {
                fp = Conversations.getFetchProfileConversationByHeaders(null == sortField ? MailField.RECEIVED_DATE : MailField.toField(sortField.getListField()));
            }
            List<Conversation> conversations = ThreadSorts.getConversationList(imapMessageStorage.getImapFolder(), getSortRange(lookAhead, messageCount, order), isRev1, fp, imapMessageStorage.getImapServerInfo());
            // Merge with sent folder
            {
                // Switch folder
                imapMessageStorage.openReadOnly(sentFullName);
                final List<MailMessage> sentMessages = Conversations.messagesFor(imapMessageStorage.getImapFolder(), lookAhead, order, fp, imapMessageStorage.getImapServerInfo(), byEnvelope);
                for (final Conversation conversation : conversations) {
                    for (final MailMessage sentMessage : sentMessages) {
                        if (conversation.referencesOrIsReferencedBy(sentMessage)) {
                            conversation.addMessage(sentMessage);
                        }
                    }
                }
                // Switch back folder
                imapMessageStorage.openReadOnly(fullName);
            }
            final MailMessageComparator threadComparator = COMPARATOR_DESC;
            // Sort
            list = new LinkedList<List<MailMessage>>();
            for (final Conversation conversation : conversations) {
                list.add(conversation.getMessages(threadComparator));
            }
            conversations = null;
            // Sort root elements
            {
                final MailSortField effectiveSortField = null == sortField ? MailSortField.RECEIVED_DATE : sortField;
                final Comparator<List<MailMessage>> listComparator = getListComparator(effectiveSortField, order, fullName, imapMessageStorage.getLocale());
                Collections.sort(list, listComparator);
            }
            // Check for index range
            if (null != indexRange) {
                final int fromIndex = indexRange.start;
                int toIndex = indexRange.end;
                final int size = list.size();
                if ((fromIndex) > size) {
                    // Return empty iterator if start is out of range
                    return Collections.emptyList();
                }
                // Reset end index if out of range
                if (toIndex >= size) {
                    toIndex = size;
                }
                list = list.subList(fromIndex, toIndex);
            }
            // Fill selected chunk
            if (!list.isEmpty()) {
                FetchProfile fetchProfile = IMAPMessageStorage.checkFetchProfile(getFetchProfile(usedFields.toArray(), true));
                List<MailMessage> msgs = new LinkedList<MailMessage>();
                List<MailMessage> sentmsgs = new LinkedList<MailMessage>();
                for (List<MailMessage> conversation : list) {
                    for (MailMessage m : conversation) {
                        if (mergeWithSent && sentFullName.equals(m.getFolder())) {
                            sentmsgs.add(m);
                        } else {
                            msgs.add(m);
                        }
                    }
                }
                new MailMessageFillerIMAPCommand(msgs, isRev1, fetchProfile, imapMessageStorage.getImapServerInfo(), imapMessageStorage.getImapFolder()).doCommand();
                if (!sentmsgs.isEmpty()) {
                    // Switch folder
                    imapMessageStorage.openReadOnly(sentFullName);
                    new MailMessageFillerIMAPCommand(sentmsgs, isRev1, fetchProfile, imapMessageStorage.getImapServerInfo(), imapMessageStorage.getImapFolder()).doCommand();
                }
            }
        } else {
            list = ThreadSorts.getConversations(imapMessageStorage.getImapFolder(), getSortRange(lookAhead, messageCount, order), isRev1, imapMessageStorage.getImapServerInfo(), null == sortField ? MailField.RECEIVED_DATE : MailField.toField(sortField.getListField()));
            // Sort root elements
            {
                final MailSortField effectiveSortField = null == sortField ? MailSortField.RECEIVED_DATE : sortField;
                final Comparator<List<MailMessage>> listComparator = getListComparator(effectiveSortField, order, fullName, imapMessageStorage.getLocale());
                Collections.sort(list, listComparator);
            }
            // Check for index range
            if (null != indexRange) {
                final int fromIndex = indexRange.start;
                int toIndex = indexRange.end;
                final int size = list.size();
                if ((fromIndex) > size) {
                    // Return empty iterator if start is out of range
                    return Collections.emptyList();
                }
                // Reset end index if out of range
                if (toIndex >= size) {
                    toIndex = size;
                }
                list = list.subList(fromIndex, toIndex);
            }
            // Fill selected chunk
            if (body) {
                List<List<MailMessage>> newlist = new LinkedList<List<MailMessage>>();
                for (List<MailMessage> conversation : list) {
                    List<MailMessage> newconversation = new LinkedList<MailMessage>();
                    for (MailMessage mailMessage : conversation) {
                        newconversation.add(imapMessageStorage.getMessage(mailMessage.getFolder(), mailMessage.getMailId(), false));
                    }
                    newlist.add(newconversation);
                }
                list = newlist;
            } else {
                List<MailMessage> msgs = new LinkedList<MailMessage>();
                for (List<MailMessage> conversation : list) {
                    msgs.addAll(conversation);
                }
                new MailMessageFillerIMAPCommand(msgs, isRev1, getFetchProfile(usedFields.toArray(), true), imapMessageStorage.getImapServerInfo(), imapMessageStorage.getImapFolder()).doCommand();
            }
        }
        // Apply account identifier
        imapMessageStorage.setAccountInfo2(list);
        // Return list
        return list;
    }

    private String getSortRange(int lookAhead, final int messageCount, final OrderDirection order) {
        final String sortRange;
        if (lookAhead <= 0) {
            sortRange = "ALL";
        } else {
            if (OrderDirection.DESC.equals(order)) {
                sortRange = (Integer.toString(messageCount - lookAhead + 1) + ':' + Integer.toString(messageCount));
            } else {
                sortRange = ("1:" + Integer.toString(lookAhead));
            }
        }
        return sortRange;
    }

    private Comparator<List<MailMessage>> getListComparator(final MailSortField sortField, final OrderDirection order, final String fullName, final Locale locale) {
        final MailMessageComparator comparator = new MailMessageComparator(sortField, OrderDirection.DESC.equals(order), locale);
        Comparator<List<MailMessage>> listComparator = new Comparator<List<MailMessage>>() {

            @Override
            public int compare(final List<MailMessage> o1, final List<MailMessage> o2) {
                MailMessage msg1 = lookUpFirstBelongingToFolder(fullName, o1);
                MailMessage msg2 = lookUpFirstBelongingToFolder(fullName, o2);

                int result = comparator.compare(msg1, msg2);
                if ((0 != result) || (MailSortField.RECEIVED_DATE != sortField)) {
                    return result;
                }

                // Zero as comparison result AND primarily sorted by received-date
                final String inReplyTo1 = msg1.getInReplyTo();
                final String inReplyTo2 = msg2.getInReplyTo();
                if (null == inReplyTo1) {
                    result = null == inReplyTo2 ? 0 : -1;
                } else {
                    result = null == inReplyTo2 ? 1 : 0;
                }
                return 0 == result ? new MailMessageComparator(MailSortField.SENT_DATE, OrderDirection.DESC.equals(order), null).compare(msg1, msg2) : result;
            }

            private MailMessage lookUpFirstBelongingToFolder(String fullName, List<MailMessage> mails) {
                for (MailMessage mail : mails) {
                    if (fullName.equals(mail.getFolder())) {
                        return mail;
                    }
                }
                return mails.get(0);
            }
        };
        return listComparator;
    }

    // -----------------------------------------------------------------------------------------------------------------------------------

    public MailMessage[] getThreadSortedMessages(final String fullName, final IndexRange indexRange, final MailSortField sortField, final OrderDirection order, final SearchTerm<?> searchTerm, final MailField[] mailFields) throws OXException {
        try {
            imapMessageStorage.openReadOnly(fullName);
            if (0 >= imapMessageStorage.getImapFolder().getMessageCount()) {
                return IMAPMessageStorage.EMPTY_RETVAL;
            }
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
                filter = IMAPSearch.searchMessages(imapMessageStorage.getImapFolder(), searchTerm, imapMessageStorage.getImapConfig());
                if ((filter == null) || (filter.length == 0)) {
                    return IMAPMessageStorage.EMPTY_RETVAL;
                }
            }
            final MailSortField effectiveSortField = null == sortField ? MailSortField.RECEIVED_DATE : sortField;
            final boolean descending = OrderDirection.DESC.equals(order);
            /*
             * Create threaded structure dependent on THREAD=REFERENCES capability
             */
            final String threadResp;
            if (imapMessageStorage.getImapConfig().getImapCapabilities().hasThreadReferences()) {
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
                threadResp = ThreadSortUtil.getThreadResponse(imapMessageStorage.getImapFolder(), sortRange);
            } else {
                Threadable threadable = Threadables.getAllThreadablesFrom(imapMessageStorage.getImapFolder(), -1);
                threadable = applyThreaderTo(threadable);
                threadResp = Threadables.toThreadReferences(threadable, null == filter ? null : new TIntHashSet(filter));
            }
            /*
             * Parse THREAD response to a list structure and extract sequence numbers
             */
            final List<ThreadSortNode> threadList = ThreadSortUtil.parseThreadResponse(threadResp);
            if (null == threadList) {
                // No threads found
                return imapMessageStorage.getAllMessages(fullName, indexRange, sortField, order, mailFields);
            }
            final List<MessageInfo> messageIds = ThreadSortUtil.fromThreadResponse(threadList);
            final TIntObjectMap<MessageInfo> seqNum2MessageId = new TIntObjectHashMap<MessageInfo>(messageIds.size());
            for (final MessageInfo messageId : messageIds) {
                seqNum2MessageId.put(messageId.getMessageNumber(), messageId);
            }
            /*
             * Fetch messages
             */
            final MailFields usedFields = new MailFields();
            IMAPMessageStorage.prepareMailFieldsForVirtualFolder(usedFields, fullName);
            // Add desired fields
            usedFields.addAll(mailFields);
            usedFields.add(MailField.THREAD_LEVEL);
            // Add sort field
            usedFields.add(MailField.toField(effectiveSortField.getListField()));
            final FetchProfile fetchProfile = IMAPMessageStorage.checkFetchProfile(getFetchProfile(usedFields.toArray(), imapMessageStorage.getIMAPProperties().isFastFetch()));
            final boolean body = usedFields.contains(MailField.BODY) || usedFields.contains(MailField.FULL);
            if (!body) {
                final Map<MessageInfo, MailMessage> mapping;
                {
                    TLongObjectMap<MailMessage> messages = new SimpleFetchIMAPCommand(imapMessageStorage.getImapFolder(), imapMessageStorage.getSeparator(imapMessageStorage.getImapFolder()), imapMessageStorage.getImapConfig().getImapCapabilities().hasIMAP4rev1(), MessageInfo.toSeqNums(messageIds).toArray(), fetchProfile, imapMessageStorage.getImapServerInfo()).doCommand();
                    mapping = new HashMap<MessageInfo, MailMessage>(messages.size());
                    messages.forEachEntry(new TLongObjectProcedure<MailMessage>() {

                        @Override
                        public boolean execute(final long seqNum, final MailMessage m) {
                            mapping.put(seqNum2MessageId.get((int) seqNum), m);
                            return true;
                        }
                    });
                }
                final List<ThreadSortMailMessage> structuredList = ThreadSortUtil.toThreadSortStructure(threadList, mapping);
                /*
                 * Sort according to order direction
                 */
                Collections.sort(structuredList, new MailMessageComparator(effectiveSortField, descending, imapMessageStorage.getLocale()));
                /*
                 * Output as flat list
                 */
                final List<MailMessage> flatList = new LinkedList<MailMessage>();
                if (usedFields.contains(MailField.ACCOUNT_NAME) || usedFields.contains(MailField.FULL)) {
                    for (final MailMessage mail : flatList) {
                        imapMessageStorage.setAccountInfo(mail);
                    }
                }
                ThreadSortUtil.toFlatList(structuredList, flatList);
                return flatList.toArray(new MailMessage[flatList.size()]);
            }
            /*
             * Include body
             */
            Message[] msgs = new MessageFetchIMAPCommand(imapMessageStorage.getImapFolder(), imapMessageStorage.getImapConfig().getImapCapabilities().hasIMAP4rev1(), MessageInfo.toSeqNums(messageIds), fetchProfile, imapMessageStorage.getImapServerInfo(), false, true, body).doCommand();
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
                    return IMailMessageStorage.EMPTY_RETVAL;
                }
                if ((fromIndex) > msgs.length) {
                    /*
                     * Return empty iterator if start is out of range
                     */
                    return IMailMessageStorage.EMPTY_RETVAL;
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
                    mails = imapMessageStorage.setAccountInfo(imapMessageStorage.convert2Mails(msgs, usedFields.toArray(), body));
                } else {
                    mails = imapMessageStorage.convert2Mails(msgs, usedFields.toArray(), body);
                }
                structuredList = ThreadSortUtil.toThreadSortStructure(mails);
            }
            /*
             * Sort according to order direction
             */
            Collections.sort(structuredList, new MailMessageComparator(effectiveSortField, descending, imapMessageStorage.getLocale()));
            /*
             * Output as flat list
             */
            final List<MailMessage> flatList = new LinkedList<MailMessage>();
            ThreadSortUtil.toFlatList(structuredList, flatList);
            return flatList.toArray(new MailMessage[flatList.size()]);
        } catch (final MessagingException e) {
            throw imapMessageStorage.handleMessagingException(fullName, e);
        } catch (final RuntimeException e) {
            throw imapMessageStorage.handleRuntimeException(e);
        } finally {
            IMAPFolderWorker.clearCache(imapMessageStorage.getImapFolder());
        }
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

}
