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

package com.openexchange.mail;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.mail.utils.MailFolderUtility.prepareFullname;
import static com.openexchange.mail.utils.MailFolderUtility.prepareMailFolderParam;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import com.openexchange.cache.OXCachingException;
import com.openexchange.configuration.ConfigurationException;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.dataretention.DataRetentionException;
import com.openexchange.dataretention.DataRetentionService;
import com.openexchange.dataretention.RetentionData;
import com.openexchange.filemanagement.ManagedFile;
import com.openexchange.filemanagement.ManagedFileException;
import com.openexchange.filemanagement.ManagedFileManagement;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.userconfiguration.UserConfigurationException;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.mail.MailException.Code;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.mail.cache.JSONMessageCache;
import com.openexchange.mail.cache.MailMessageCache;
import com.openexchange.mail.cache.MailPrefetcherCallable;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.mail.dataobjects.MailFolderDescription;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.dataobjects.compose.ComposeType;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.event.EventPool;
import com.openexchange.mail.event.PooledEvent;
import com.openexchange.mail.mime.MIMEMailException;
import com.openexchange.mail.mime.MIMETypes;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.mime.converters.MIMEMessageConverter;
import com.openexchange.mail.mime.processing.MimeForward;
import com.openexchange.mail.parser.MailMessageParser;
import com.openexchange.mail.parser.handlers.NonInlineForwardPartHandler;
import com.openexchange.mail.permission.MailPermission;
import com.openexchange.mail.search.HeaderTerm;
import com.openexchange.mail.search.SearchTerm;
import com.openexchange.mail.search.SearchUtility;
import com.openexchange.mail.search.service.SearchTermMapper;
import com.openexchange.mail.transport.MailTransport;
import com.openexchange.mail.transport.TransportProvider;
import com.openexchange.mail.transport.TransportProviderRegistry;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.mail.utils.StorageUtility;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountException;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.server.ServiceException;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.spamhandler.SpamHandlerRegistry;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.ThreadPools;
import com.openexchange.threadpool.behavior.CallerRunsBehavior;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIteratorAdapter;
import com.openexchange.tools.iterator.SearchIteratorDelegator;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.sql.SearchStrings;
import com.openexchange.user.UserService;

/**
 * {@link MailServletInterfaceImpl} - The mail servlet interface implementation.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
final class MailServletInterfaceImpl extends MailServletInterface {

    private static final MailField[] FIELDS_FULL = new MailField[] { MailField.FULL };

    private static final MailField[] FIELDS_ID_INFO = new MailField[] { MailField.ID, MailField.FOLDER_ID };

    private static final MailField[] HEADERS = { MailField.ID, MailField.HEADERS };

    private static final String[] STR_ARR = new String[0];

    private static final String INBOX_ID = "INBOX";

    private static final int MAX_NUMBER_OF_MESSAGES_2_CACHE = 50;

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(MailServletInterfaceImpl.class);

    private static final boolean DEBUG_ENABLED = LOG.isDebugEnabled();

    /*-
     * ++++++++++++++ Fields ++++++++++++++
     */

    private final Context ctx;

    private final int contextId;

    private boolean init;

    private MailConfig mailConfig;

    private MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess;

    private int accountId;

    private final Session session;

    private final UserSettingMail usm;

    private Locale locale;

    /**
     * Initializes a new {@link MailServletInterfaceImpl}.
     * 
     * @throws MailException If user has no mail access or properties cannot be successfully loaded
     */
    MailServletInterfaceImpl(final Session session) throws MailException {
        super();
        try {
            this.ctx =
                (session instanceof ServerSession) ? ((ServerSession) session).getContext() : ContextStorage.getInstance().getContext(
                    session.getContextId());
        } catch (final ContextException e) {
            throw new MailException(e);
        }
        try {
            if (!UserConfigurationStorage.getInstance().getUserConfiguration(session.getUserId(), ctx).hasWebMail()) {
                throw new MailException(MailException.Code.NO_MAIL_ACCESS);
            }
        } catch (final UserConfigurationException e) {
            throw new MailException(e);
        }
        this.session = session;
        this.contextId = session.getContextId();
        usm = UserSettingMailStorage.getInstance().getUserSettingMail(session.getUserId(), ctx);
    }

    private Locale getUserLocale() {
        if (null == locale) {
            final UserService userService = ServerServiceRegistry.getInstance().getService(UserService.class);
            if (null == userService) {
                return Locale.ENGLISH;
            }
            try {
                locale = userService.getUser(session.getUserId(), ctx).getLocale();
            } catch (final com.openexchange.groupware.ldap.UserException e) {
                LOG.warn(e.getMessage(), e);
                return Locale.ENGLISH;
            }
        }
        return locale;
    }

    @Override
    public boolean clearFolder(final String folder) throws MailException {
        final FullnameArgument fullnameArgument = prepareMailFolderParam(folder);
        final int accountId = fullnameArgument.getAccountId();
        initConnection(fullnameArgument.getAccountId());
        final String fullname = fullnameArgument.getFullname();
        /*
         * Only backup if no hard-delete is set in user's mail configuration and fullname does not denote trash (sub)folder
         */
        final boolean backup =
            (!UserSettingMailStorage.getInstance().getUserSettingMail(session.getUserId(), ctx).isHardDeleteMsgs() && !(fullname.startsWith(mailAccess.getFolderStorage().getTrashFolder())));
        mailAccess.getFolderStorage().clearFolder(fullname, !backup);
        postEvent(accountId, fullname, true);
        final String trashFullname = prepareMailFolderParam(getTrashFolder(accountId)).getFullname();
        if (backup) {
            postEvent(accountId, trashFullname, true);
        }
        try {
            /*
             * Update JSON cache
             */
            final JSONMessageCache cache = JSONMessageCache.getInstance();
            if (null != cache) {
                cache.removeFolder(fullnameArgument.getAccountId(), fullname, session);
            }
            /*
             * Update message cache
             */
            MailMessageCache.getInstance().removeFolderMessages(fullnameArgument.getAccountId(), fullname, session.getUserId(), contextId);
        } catch (final OXCachingException e) {
            LOG.error(e.getMessage(), e);
        }
        if (fullname.startsWith(trashFullname)) {
            // Special handling
            final MailFolder[] subf = mailAccess.getFolderStorage().getSubfolders(fullname, true);
            for (int i = 0; i < subf.length; i++) {
                final String subFullname = subf[i].getFullname();
                mailAccess.getFolderStorage().deleteFolder(subFullname, true);
                postEvent(accountId, subFullname, false);
            }
            postEvent(accountId, trashFullname, false);
        }
        return true;
    }

    @Override
    public void close(final boolean putIntoCache) throws MailException {
        try {
            if (mailAccess != null) {
                mailAccess.close(putIntoCache);
            }
        } finally {
            mailAccess = null;
            init = false;
        }
    }

    private static final int SPAM_HAM = -1;

    private static final int SPAM_NOOP = 0;

    private static final int SPAM_SPAM = 1;

    @Override
    public String[] copyMessages(final String sourceFolder, final String destFolder, final String[] msgUIDs, final boolean move) throws MailException {
        final FullnameArgument source = prepareMailFolderParam(sourceFolder);
        final FullnameArgument dest = prepareMailFolderParam(destFolder);
        final String sourceFullname = source.getFullname();
        final String destFullname = dest.getFullname();
        final int sourceAccountId = source.getAccountId();
        initConnection(sourceAccountId);
        final int destAccountId = dest.getAccountId();
        if (sourceAccountId == destAccountId) {
            if (move) {
                /*
                 * Check for spam action; meaning a move/copy from/to spam folder
                 */
                final String spamFullname = mailAccess.getFolderStorage().getSpamFolder();
                final int spamAction;
                if (usm.isSpamEnabled()) {
                    spamAction =
                        spamFullname.equals(sourceFullname) ? SPAM_HAM : (spamFullname.equals(destFullname) ? SPAM_SPAM : SPAM_NOOP);
                } else {
                    spamAction = SPAM_NOOP;
                }
                if (spamAction != SPAM_NOOP) {
                    if (spamAction == SPAM_SPAM) {
                        /*
                         * Handle spam
                         */
                        SpamHandlerRegistry.getSpamHandlerBySession(session, accountId).handleSpam(
                            accountId,
                            sourceFullname,
                            msgUIDs,
                            false,
                            session);
                    } else {
                        /*
                         * Handle ham.
                         */
                        SpamHandlerRegistry.getSpamHandlerBySession(session, accountId).handleHam(
                            accountId,
                            sourceFullname,
                            msgUIDs,
                            false,
                            session);
                    }
                }
            }
            final String[] maildIds;
            if (move) {
                maildIds = mailAccess.getMessageStorage().moveMessages(sourceFullname, destFullname, msgUIDs, false);
                postEvent(sourceAccountId, sourceFullname, true);
            } else {
                maildIds = mailAccess.getMessageStorage().copyMessages(sourceFullname, destFullname, msgUIDs, false);
            }
            postEvent(sourceAccountId, destFullname, true);
            try {
                /*
                 * Update JSON cache
                 */
                final JSONMessageCache cache = JSONMessageCache.getInstance();
                if (null != cache) {
                    if (move) {
                        cache.removeFolder(sourceAccountId, sourceFullname, session);
                    }
                    cache.removeFolder(destAccountId, destFullname, session);
                }
                /*
                 * Update message cache
                 */
                if (move) {
                    MailMessageCache.getInstance().removeFolderMessages(sourceAccountId, sourceFullname, session.getUserId(), contextId);
                }
                MailMessageCache.getInstance().removeFolderMessages(destAccountId, destFullname, session.getUserId(), contextId);
            } catch (final OXCachingException e) {
                LOG.error(e.getMessage(), e);
            }
            return maildIds;
        }
        final MailAccess<?, ?> destAccess = initMailAccess(destAccountId);
        try {
            if (move) {
                /*
                 * Check for spam action; meaning a move/copy from/to spam folder
                 */
                int spamActionSource = SPAM_NOOP;
                int spamActionDest = SPAM_NOOP;
                if (usm.isSpamEnabled()) {
                    if (sourceFullname.equals(mailAccess.getFolderStorage().getSpamFolder())) {
                        spamActionSource = SPAM_HAM;
                    }
                    if (destFullname.equals(destAccess.getFolderStorage().getSpamFolder())) {
                        spamActionDest = SPAM_SPAM;
                    }
                }
                if (SPAM_HAM == spamActionSource) {
                    /*
                     * Handle ham.
                     */
                    SpamHandlerRegistry.getSpamHandlerBySession(session, accountId).handleHam(
                        accountId,
                        sourceFullname,
                        msgUIDs,
                        false,
                        session);
                }
                if (SPAM_SPAM == spamActionDest) {
                    /*
                     * Handle spam
                     */
                    SpamHandlerRegistry.getSpamHandlerBySession(session, accountId).handleSpam(
                        accountId,
                        sourceFullname,
                        msgUIDs,
                        false,
                        session);
                }
            }
            // Fetch messages from source folder
            final MailMessage[] messages = mailAccess.getMessageStorage().getMessages(sourceFullname, msgUIDs, FIELDS_FULL);
            // Append them to destination folder
            final String[] maildIds = destAccess.getMessageStorage().appendMessages(destFullname, messages);
            // Delete source messages if a move shall be performed
            if (move) {
                mailAccess.getMessageStorage().deleteMessages(sourceFullname, messages2ids(messages), true);
                postEvent(sourceAccountId, sourceFullname, true);
            }
            postEvent(destAccountId, destFullname, true);
            try {
                /*
                 * Update JSON cache
                 */
                final JSONMessageCache cache = JSONMessageCache.getInstance();
                if (cache != null) {
                    if (move) {
                        cache.removeFolder(sourceAccountId, sourceFullname, session);
                    }
                    cache.removeFolder(destAccountId, destFullname, session);
                }
                if (move) {
                    /*
                     * Update message cache
                     */
                    MailMessageCache.getInstance().removeFolderMessages(sourceAccountId, sourceFullname, session.getUserId(), contextId);
                }
                MailMessageCache.getInstance().removeFolderMessages(destAccountId, destFullname, session.getUserId(), contextId);
            } catch (final OXCachingException e) {
                LOG.error(e.getMessage(), e);
            }
            return maildIds;

        } finally {
            destAccess.close(true);
        }
    }

    @Override
    public String deleteFolder(final String folder) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        final String fullname = argument.getFullname();
        /*
         * Only backup if fullname does not denote trash (sub)folder
         */
        final IMailFolderStorage folderStorage = mailAccess.getFolderStorage();
        final String trashFullname = folderStorage.getTrashFolder();
        final boolean hardDelete = fullname.startsWith(trashFullname);
        /*
         * Remember subfolder tree
         */
        final Map<String, Map<?, ?>> subfolders = subfolders(fullname);
        final String retval = prepareFullname(accountId, folderStorage.deleteFolder(fullname, hardDelete));
        try {
            /*
             * Update JSON cache
             */
            final JSONMessageCache cache = JSONMessageCache.getInstance();
            if (null != cache) {
                cache.removeFolder(accountId, fullname, session);
            }
            /*
             * Update message cache
             */
            MailMessageCache.getInstance().removeFolderMessages(accountId, fullname, session.getUserId(), contextId);
        } catch (final OXCachingException e) {
            LOG.error(e.getMessage(), e);
        }
        if (!hardDelete) {
            // New folder in trash folder
            postEvent(accountId, trashFullname, false);
        }
        postEvent4Subfolders(accountId, subfolders);
        return retval;
    }

    private void postEvent4Subfolders(final int accountId, final Map<String, Map<?, ?>> subfolders) {
        final int size = subfolders.size();
        final Iterator<Entry<String, Map<?, ?>>> iter = subfolders.entrySet().iterator();
        for (int i = 0; i < size; i++) {
            final Entry<String, Map<?, ?>> entry = iter.next();
            final @SuppressWarnings("unchecked") Map<String, Map<?, ?>> m = (Map<String, Map<?, ?>>) entry.getValue();
            if (!m.isEmpty()) {
                postEvent4Subfolders(accountId, m);
            }
            postEvent(accountId, entry.getKey(), false);
        }
    }

    private Map<String, Map<?, ?>> subfolders(final String fullname) throws MailException {
        final Map<String, Map<?, ?>> m = new HashMap<String, Map<?, ?>>();
        subfoldersRecursively(fullname, m);
        return m;
    }

    private void subfoldersRecursively(final String parent, final Map<String, Map<?, ?>> m) throws MailException {
        final MailFolder[] mailFolders = mailAccess.getFolderStorage().getSubfolders(parent, true);
        if (null == mailFolders || 0 == mailFolders.length) {
            final Map<String, Map<?, ?>> emptyMap = Collections.emptyMap();
            m.put(parent, emptyMap);
        } else {
            final Map<String, Map<?, ?>> subMap = new HashMap<String, Map<?, ?>>();
            final int size = mailFolders.length;
            for (int i = 0; i < size; i++) {
                final String fullname = mailFolders[i].getFullname();
                subfoldersRecursively(fullname, subMap);
            }
            m.put(parent, subMap);
        }
    }

    @Override
    public boolean deleteMessages(final String folder, final String[] msgUIDs, final boolean hardDelete) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        final String fullname = argument.getFullname();
        /*
         * Hard-delete if hard-delete is set in user's mail configuration or fullname denotes trash (sub)folder
         */
        final String trashFullname = mailAccess.getFolderStorage().getTrashFolder();
        final boolean hd =
            (hardDelete || UserSettingMailStorage.getInstance().getUserSettingMail(session.getUserId(), ctx).isHardDeleteMsgs() || (null != trashFullname && fullname.startsWith(trashFullname)));
        mailAccess.getMessageStorage().deleteMessages(fullname, msgUIDs, hd);
        try {
            /*
             * Update JSON cache
             */
            final JSONMessageCache jsonMessageCache = JSONMessageCache.getInstance();
            if (null != jsonMessageCache) {
                for (final String uid : msgUIDs) {
                    jsonMessageCache.remove(accountId, fullname, uid, session);
                }
            }
            /*
             * Update message cache
             */
            MailMessageCache.getInstance().removeFolderMessages(accountId, fullname, session.getUserId(), contextId);
        } catch (final OXCachingException e) {
            LOG.error(e.getMessage(), e);
        }
        postEvent(accountId, fullname, true);
        if (!hd) {
            postEvent(accountId, trashFullname, true);
        }
        return true;
    }

    @Override
    public int[] getAllMessageCount(final String folder) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        initConnection(argument.getAccountId());
        final String fullname = argument.getFullname();
        final MailFolder f = mailAccess.getFolderStorage().getFolder(fullname);
        return new int[] { f.getMessageCount(), f.getNewMessageCount(), f.getUnreadMessageCount(), f.getDeletedMessageCount() };
    }

    @Override
    public SearchIterator<MailMessage> getAllMessages(final String folder, final int sortCol, final int order, final int[] fields, final int[] fromToIndices) throws MailException {
        return getMessages(folder, fromToIndices, sortCol, order, null, null, false, fields);
    }

    @Override
    public SearchIterator<MailMessage> getAllThreadedMessages(final String folder, final int sortCol, final int order, final int[] fields, final int[] fromToIndices) throws MailException {
        return getThreadedMessages(folder, fromToIndices, sortCol, order, null, null, false, fields);
    }

    @Override
    public SearchIterator<MailFolder> getChildFolders(final String parentFolder, final boolean all) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(parentFolder);
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        final String parentFullname = argument.getFullname();
        final List<MailFolder> children = Arrays.asList(mailAccess.getFolderStorage().getSubfolders(parentFullname, all));
        if (children.isEmpty()) {
            return SearchIteratorAdapter.createEmptyIterator();
        }
        /*
         * Check if denoted parent can hold default folders like Trash, Sent, etc.
         */
        if (!MailFolder.DEFAULT_FOLDER_ID.equals(parentFullname) && !INBOX_ID.equals(parentFullname)) {
            /*
             * Denoted parent is not capable to hold default folders. Therefore output as it is.
             */
            Collections.sort(children, new SimpleMailFolderComparator(getUserLocale()));
            return new SearchIteratorDelegator<MailFolder>(children.iterator(), children.size());
        }
        /*
         * Ensure default folders are at first positions
         */
        final String[] names;
        if (isDefaultFoldersChecked(accountId)) {
            names = getSortedDefaultMailFolders(accountId);
        } else {
            final List<String> tmp = new ArrayList<String>();

            FullnameArgument fa = prepareMailFolderParam(getInboxFolder(accountId));
            if (null != fa) {
                tmp.add(fa.getFullname());
            }

            fa = prepareMailFolderParam(getDraftsFolder(accountId));
            if (null != fa) {
                tmp.add(fa.getFullname());
            }

            fa = prepareMailFolderParam(getSentFolder(accountId));
            if (null != fa) {
                tmp.add(fa.getFullname());
            }

            fa = prepareMailFolderParam(getSpamFolder(accountId));
            if (null != fa) {
                tmp.add(fa.getFullname());
            }

            fa = prepareMailFolderParam(getTrashFolder(accountId));
            if (null != fa) {
                tmp.add(fa.getFullname());
            }

            names = tmp.toArray(new String[tmp.size()]);
        }
        /*
         * Sort them
         */
        Collections.sort(children, new MailFolderComparator(names, getUserLocale()));
        return new SearchIteratorDelegator<MailFolder>(children.iterator(), children.size());
    }

    @Override
    public String getConfirmedHamFolder(final int accountId) throws MailException {
        if (isDefaultFoldersChecked(accountId)) {
            return prepareFullname(accountId, getDefaultMailFolder(StorageUtility.INDEX_CONFIRMED_HAM, accountId));
        }
        initConnection(accountId);
        return prepareFullname(accountId, mailAccess.getFolderStorage().getConfirmedHamFolder());
    }

    @Override
    public String getConfirmedSpamFolder(final int accountId) throws MailException {
        if (isDefaultFoldersChecked(accountId)) {
            return prepareFullname(accountId, getDefaultMailFolder(StorageUtility.INDEX_CONFIRMED_SPAM, accountId));
        }
        initConnection(accountId);
        return prepareFullname(accountId, mailAccess.getFolderStorage().getConfirmedSpamFolder());
    }

    private String getDefaultMailFolder(final int index, final int accountId) {
        final String[] arr =
            MailSessionCache.getInstance(session).getParameter(accountId, MailSessionParameterNames.getParamDefaultFolderArray());
        return arr == null ? null : arr[index];
    }

    private String[] getSortedDefaultMailFolders(final int accountId) {
        final String[] arr =
            MailSessionCache.getInstance(session).getParameter(accountId, MailSessionParameterNames.getParamDefaultFolderArray());
        if (arr == null) {
            return STR_ARR;
        }
        return new String[] {
            INBOX_ID, arr[StorageUtility.INDEX_DRAFTS], arr[StorageUtility.INDEX_SENT], arr[StorageUtility.INDEX_SPAM],
            arr[StorageUtility.INDEX_TRASH] };
    }

    @Override
    public int getDeletedMessageCount(final String folder) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        initConnection(argument.getAccountId());
        final String fullname = argument.getFullname();
        return mailAccess.getFolderStorage().getFolder(fullname).getDeletedMessageCount();
    }

    @Override
    public String getDraftsFolder(final int accountId) throws MailException {
        if (isDefaultFoldersChecked(accountId)) {
            return prepareFullname(accountId, getDefaultMailFolder(StorageUtility.INDEX_DRAFTS, accountId));
        }
        initConnection(accountId);
        return prepareFullname(accountId, mailAccess.getFolderStorage().getDraftsFolder());
    }

    @Override
    public MailFolder getFolder(final String folder, final boolean checkFolder) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        initConnection(argument.getAccountId());
        final String fullname = argument.getFullname();
        return mailAccess.getFolderStorage().getFolder(fullname);
    }

    @Override
    public MailMessage getForwardMessageForDisplay(final String[] folders, final String[] fowardMsgUIDs, final UserSettingMail usm) throws MailException {
        if ((null == folders) || (null == fowardMsgUIDs) || (folders.length != fowardMsgUIDs.length)) {
            throw new IllegalArgumentException("Illegal arguments");
        }
        final FullnameArgument[] arguments = new FullnameArgument[folders.length];
        for (int i = 0; i < folders.length; i++) {
            arguments[i] = prepareMailFolderParam(folders[i]);
        }
        boolean sameAccount = true;
        final int accountId = arguments[0].getAccountId();
        for (int i = 1; i < arguments.length && sameAccount; i++) {
            sameAccount = accountId == arguments[i].getAccountId();
        }
        if (sameAccount) {
            initConnection(accountId);
            final MailMessage[] originalMails = new MailMessage[folders.length];
            for (int i = 0; i < arguments.length; i++) {
                final MailMessage origMail = mailAccess.getMessageStorage().getMessage(arguments[i].getFullname(), fowardMsgUIDs[i], false);
                if (null == origMail) {
                    throw new MailException(MailException.Code.MAIL_NOT_FOUND, fowardMsgUIDs[i], arguments[i].getFullname());
                }
                originalMails[i] = origMail;
            }
            return mailAccess.getLogicTools().getFowardMessage(originalMails, usm);
        }
        final MailMessage[] originalMails = new MailMessage[folders.length];
        for (int i = 0; i < arguments.length && sameAccount; i++) {
            final MailAccess<?, ?> ma = initMailAccess(arguments[i].getAccountId());
            try {
                final MailMessage origMail = ma.getMessageStorage().getMessage(arguments[i].getFullname(), fowardMsgUIDs[i], false);
                if (null == origMail) {
                    throw new MailException(MailException.Code.MAIL_NOT_FOUND, fowardMsgUIDs[i], arguments[i].getFullname());
                }
                originalMails[i] = origMail;
                origMail.loadContent();
            } finally {
                ma.close(true);
            }
        }
        final int[] accountIDs = new int[originalMails.length];
        for (int i = 0; i < accountIDs.length; i++) {
            accountIDs[i] = arguments[i].getAccountId();
        }
        return MimeForward.getFowardMail(originalMails, session, accountIDs, usm);
    }

    @Override
    public String getInboxFolder(final int accountId) throws MailException {
        if (isDefaultFoldersChecked(accountId)) {
            return prepareFullname(accountId, INBOX_ID);
        }
        initConnection(accountId);
        return prepareFullname(accountId, mailAccess.getFolderStorage().getFolder(INBOX_ID).getFullname());
    }

    @Override
    public MailConfig getMailConfig() throws MailException {
        return mailConfig;
    }

    @Override
    public int getAccountID() {
        return accountId;
    }

    private static final MailListField[] FIELDS_FLAGS = new MailListField[] { MailListField.FLAGS };

    private static final transient Object[] ARGS_FLAG_SEEN_SET = new Object[] { Integer.valueOf(MailMessage.FLAG_SEEN) };

    private static final transient Object[] ARGS_FLAG_SEEN_UNSET = new Object[] { Integer.valueOf(-1 * MailMessage.FLAG_SEEN) };

    @Override
    public MailMessage getMessage(final String folder, final String msgUID) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        if (MailFolder.DEFAULT_FOLDER_ID.equals(folder)) {
            throw new MailException(MailException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, MailFolder.DEFAULT_FOLDER_ID);
        }
        final String fullname = argument.getFullname();
        final MailMessage mail = mailAccess.getMessageStorage().getMessage(fullname, msgUID, true);
        if (mail != null) {
            /*
             * Update cache since \Seen flag is possibly changed
             */
            try {
                if (MailMessageCache.getInstance().containsFolderMessages(accountId, fullname, session.getUserId(), contextId)) {
                    /*
                     * Update cache entry
                     */
                    MailMessageCache.getInstance().updateCachedMessages(
                        new String[] { mail.getMailId() },
                        accountId,
                        fullname,
                        session.getUserId(),
                        contextId,
                        FIELDS_FLAGS,
                        mail.isSeen() ? ARGS_FLAG_SEEN_SET : ARGS_FLAG_SEEN_UNSET);

                }
            } catch (final OXCachingException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return mail;
    }

    @Override
    public MailPart getMessageAttachment(final String folder, final String msgUID, final String attachmentPosition, final boolean displayVersion) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        final String fullname = argument.getFullname();
        return mailAccess.getMessageStorage().getAttachment(fullname, msgUID, attachmentPosition);
    }

    @Override
    public ManagedFile getMessageAttachments(final String folder, final String msgUID, final String[] attachmentPositions) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        final String fullname = argument.getFullname();
        /*
         * Get parts
         */
        final MailPart[] parts = new MailPart[attachmentPositions.length];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = mailAccess.getMessageStorage().getAttachment(fullname, msgUID, attachmentPositions[i]);
        }
        /*
         * Store them temporary to files
         */
        final ManagedFileManagement mfm;
        try {
            mfm = ServerServiceRegistry.getInstance().getService(ManagedFileManagement.class, true);
        } catch (final ServiceException e) {
            throw new MailException(e);
        }
        final ManagedFile[] files = new ManagedFile[parts.length];
        try {
            for (int i = 0; i < files.length; i++) {
                final MailPart part = parts[i];
                if (null == part) {
                    files[i] = null;
                } else {
                    files[i] = mfm.createManagedFile(part.getInputStream());
                }
            }
            /*
             * ZIP them
             */
            try {
                final File tempFile = mfm.newTempFile();
                final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempFile));
                try {
                    final byte[] buf = new byte[8192];
                    for (int i = 0; i < files.length; i++) {
                        final ManagedFile file = files[i];
                        if (null != file) {
                            final FileInputStream in = new FileInputStream(file.getFile());
                            try {
                                /*
                                 * Add ZIP entry to output stream
                                 */
                                out.putNextEntry(new ZipEntry(parts[i].getFileName()));
                                /*
                                 * Transfer bytes from the file to the ZIP file
                                 */
                                int len;
                                while ((len = in.read(buf)) > 0) {
                                    out.write(buf, 0, len);
                                }
                                /*
                                 * Complete the entry
                                 */
                                out.closeEntry();
                            } finally {
                                try {
                                    in.close();
                                } catch (final IOException e) {
                                    LOG.error(e.getMessage(), e);
                                }
                            }
                        }
                    }
                } finally {
                    // Complete the ZIP file
                    try {
                        out.close();
                    } catch (final IOException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
                /*
                 * Return managed file
                 */
                return mfm.createManagedFile(tempFile);
            } catch (final IOException e) {
                throw new MailException(MailException.Code.IO_ERROR, e, e.getMessage());
            }
        } catch (final ManagedFileException e) {
            throw new MailException(e);
        } finally {
            for (int i = 0; i < files.length; i++) {
                final ManagedFile file = files[i];
                if (null != file) {
                    file.delete();
                }
            }
        }
    }

    @Override
    public int getMessageCount(final String folder) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        final String fullname = argument.getFullname();
        return mailAccess.getFolderStorage().getFolder(fullname).getMessageCount();
    }

    @Override
    public MailPart getMessageImage(final String folder, final String msgUID, final String cid) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        final String fullname = argument.getFullname();
        return mailAccess.getMessageStorage().getImageAttachment(fullname, msgUID, cid);
    }

    @Override
    public MailMessage[] getMessageList(final String folder, final String[] uids, final int[] fields, final String[] headerFields) throws MailException {
        /*
         * Although message cache is only used within mail implementation, we have to examine if cache already holds desired messages. If
         * the cache holds the desired messages no connection has to be fetched/established. This avoids a lot of overhead.
         */
        final int accountId;
        final String fullname;
        {
            final FullnameArgument argument = prepareMailFolderParam(folder);
            accountId = argument.getAccountId();
            fullname = argument.getFullname();
        }
        final boolean loadHeaders = (null != headerFields && 0 < headerFields.length);
        /*-
         * Check for presence in cache
         * TODO: Think about switching to live-fetch if loadHeaders is true. Loading all data once may be faster than
         * first loading from cache then loading missing headers in next step
         */
        try {
            final MailMessage[] mails =
                MailMessageCache.getInstance().getMessages(uids, accountId, fullname, session.getUserId(), contextId);
            if (null != mails) {
                /*
                 * List request can be served from cache; apply proper account ID to (unconnected) mail servlet interface
                 */
                this.accountId = accountId;
                /*
                 * Check if headers shall be loaded
                 */
                if (loadHeaders) {
                    /*
                     * Load headers of cached mails
                     */
                    final List<String> loadMe = new ArrayList<String>(mails.length);
                    final Map<String, MailMessage> finder = new HashMap<String, MailMessage>(mails.length);
                    for (final MailMessage mail : mails) {
                        final String mailId = mail.getMailId();
                        finder.put(mailId, mail);
                        if (!mail.containsHeaders()) {
                            loadMe.add(mailId);
                        }
                    }
                    initConnection(accountId);
                    for (final MailMessage header : mailAccess.getMessageStorage().getMessages(fullname, loadMe.toArray(STR_ARR), HEADERS)) {
                        finder.get(header.getMailId()).addHeaders(header.getHeaders());
                    }
                }
                /*
                 * Prefetch messages
                 */
                prefetchJSONMessages(fullname, uids);
                return mails;
            }
        } catch (final OXCachingException e) {
            LOG.error(e.getMessage(), e);
        }
        /*
         * Live-Fetch from mail storage
         */
        initConnection(accountId);
        /*
         * Get appropriate mail fields
         */
        final MailField[] mailFields;
        if (loadHeaders) {
            /*
             * Ensure MailField.HEADERS is contained
             */
            final MailFields col = new MailFields(MailField.toFields(MailListField.getFields(fields)));
            col.add(MailField.HEADERS);
            mailFields = col.toArray();
        } else {
            mailFields = MailField.toFields(MailListField.getFields(fields));
        }
        final MailMessage[] mails = mailAccess.getMessageStorage().getMessages(fullname, uids, mailFields);
        try {
            if (MailMessageCache.getInstance().containsFolderMessages(accountId, fullname, session.getUserId(), contextId)) {
                MailMessageCache.getInstance().putMessages(accountId, mails, session.getUserId(), contextId);
            }
        } catch (final OXCachingException e) {
            LOG.error(e.getMessage(), e);
        }
        /*
         * Prefetch messages
         */
        prefetchJSONMessages(fullname, uids);
        return mails;
    }

    private void prefetchJSONMessages(final String fullname, final String[] mailIds) {
        /*
         * Pre-Fetch messages' JSON representations for external mail accounts
         */
        if (accountId != MailAccount.DEFAULT_ID) {
            try {
                final String[] prefetchIds;
                if (mailIds.length > MAX_NUMBER_OF_MESSAGES_2_CACHE) {
                    prefetchIds = new String[MAX_NUMBER_OF_MESSAGES_2_CACHE];
                    System.arraycopy(mailIds, 0, prefetchIds, 0, MAX_NUMBER_OF_MESSAGES_2_CACHE);
                } else {
                    prefetchIds = mailIds;
                }
                final ThreadPoolService threadPool = ServerServiceRegistry.getInstance().getService(ThreadPoolService.class, true);
                threadPool.submit(ThreadPools.task(new MailPrefetcherCallable(session, accountId, fullname, prefetchIds, false, threadPool)));
            } catch (final ServiceException e) {
                LOG.error(e.getMessage(), e);
            }
        } else if (DEBUG_ENABLED) {
            LOG.debug("No message prefetch for primary mail account.");
        }
    }

    @Override
    public SearchIterator<MailMessage> getMessages(final String folder, final int[] fromToIndices, final int sortCol, final int order, final com.openexchange.search.SearchTerm<?> searchTerm, final boolean linkSearchTermsWithOR, final int[] fields) throws MailException {
        return getMessagesInternal(prepareMailFolderParam(folder), SearchTermMapper.map(searchTerm), fromToIndices, sortCol, order, fields);
    }

    @Override
    public SearchIterator<MailMessage> getMessages(final String folder, final int[] fromToIndices, final int sortCol, final int order, final int[] searchCols, final String[] searchPatterns, final boolean linkSearchTermsWithOR, final int[] fields) throws MailException {
        checkPatternLength(searchPatterns);
        final SearchTerm<?> searchTerm =
            (searchCols == null) || (searchCols.length == 0) ? null : SearchUtility.parseFields(
                searchCols,
                searchPatterns,
                linkSearchTermsWithOR);
        return getMessagesInternal(prepareMailFolderParam(folder), searchTerm, fromToIndices, sortCol, order, fields);
    }

    private SearchIterator<MailMessage> getMessagesInternal(final FullnameArgument argument, final SearchTerm<?> searchTerm, final int[] fromToIndices, final int sortCol, final int order, final int[] fields) throws MailException {
        /*
         * Identify and sort messages according to search term and sort criteria while only fetching their IDs
         */
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        final String fullname = argument.getFullname();
        MailMessage[] mails =
            mailAccess.getMessageStorage().searchMessages(
                fullname,
                null == fromToIndices ? IndexRange.NULL : new IndexRange(fromToIndices[0], fromToIndices[1]),
                MailSortField.getField(sortCol),
                OrderDirection.getOrderDirection(order),
                searchTerm,
                FIELDS_ID_INFO);
        if ((mails == null) || (mails.length == 0)) {
            return SearchIteratorAdapter.<MailMessage> createEmptyIterator();
        }
        final boolean cachable = (mails.length < mailAccess.getMailConfig().getMailProperties().getMailFetchLimit());
        final MailField[] useFields;
        final boolean onlyFolderAndID;
        if (cachable) {
            /*
             * Selection fits into cache: Prepare for caching
             */
            useFields = com.openexchange.mail.mime.utils.MIMEStorageUtility.getCacheFieldsArray();
            onlyFolderAndID = false;
        } else {
            useFields = MailField.getFields(fields);
            onlyFolderAndID = onlyFolderAndID(useFields);
        }
        /*
         * Extract IDs
         */
        final String[] mailIds = new String[mails.length];
        for (int i = 0; i < mailIds.length; i++) {
            mailIds[i] = mails[i].getMailId();
        }
        if (!onlyFolderAndID) {
            /*
             * Fetch identified messages by their IDs and pre-fill them according to specified fields
             */
            mails = mailAccess.getMessageStorage().getMessages(fullname, mailIds, useFields);
        }
        /*
         * Put message information into cache
         */
        try {
            /*
             * Remove old user cache entries
             */
            // TODO: JSONMessageCache.getInstance().removeAllFoldersExcept(accountId, fullname, session);
            MailMessageCache.getInstance().removeUserMessages(session.getUserId(), contextId);
            if ((cachable) && (mails != null) && (mails.length > 0)) {
                /*
                 * ... and put new ones
                 */
                MailMessageCache.getInstance().putMessages(accountId, mails, session.getUserId(), contextId);
            }
        } catch (final OXCachingException e) {
            LOG.error(e.getMessage(), e);
        }
        return SearchIteratorAdapter.<MailMessage> createArrayIterator(mails);
    }

    /**
     * Checks if specified fields only consist of mail ID and folder ID
     * 
     * @param fields The fields to check
     * @return <code>true</code> if specified fields only consist of mail ID and folder ID; otherwise <code>false</code>
     */
    private static boolean onlyFolderAndID(final MailField[] fields) {
        if (fields.length != 2) {
            return false;
        }
        int i = 0;
        for (final MailField field : fields) {
            if (MailField.ID.equals(field)) {
                i |= 1;
            } else if (MailField.FOLDER_ID.equals(field)) {
                i |= 2;
            }
        }
        return (i == 3);
    }

    @Override
    public int getNewMessageCount(final String folder) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        final String fullname = argument.getFullname();
        return mailAccess.getFolderStorage().getFolder(fullname).getNewMessageCount();
    }

    @Override
    public SearchIterator<MailMessage> getNewMessages(final String folder, final int sortCol, final int order, final int[] fields, final int limit) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        final String fullname = argument.getFullname();
        return SearchIteratorAdapter.createArrayIterator(mailAccess.getMessageStorage().getUnreadMessages(
            fullname,
            MailSortField.getField(sortCol),
            OrderDirection.getOrderDirection(order),
            MailField.toFields(MailListField.getFields(fields)),
            limit));
    }

    @Override
    public SearchIterator<MailFolder> getPathToDefaultFolder(final String folder) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        final String fullname = argument.getFullname();
        return SearchIteratorAdapter.createArrayIterator(mailAccess.getFolderStorage().getPath2DefaultFolder(fullname));
    }

    @Override
    public long[][] getQuotas(final int[] types) throws MailException {
        initConnection(MailAccount.DEFAULT_ID);
        final com.openexchange.mail.Quota.Type[] qtypes = new com.openexchange.mail.Quota.Type[types.length];
        for (int i = 0; i < qtypes.length; i++) {
            qtypes[i] = getType(types[i]);
        }
        final com.openexchange.mail.Quota[] quotas = mailAccess.getFolderStorage().getQuotas(INBOX_ID, qtypes);
        final long[][] retval = new long[quotas.length][];
        for (int i = 0; i < retval.length; i++) {
            retval[i] = quotas[i].toLongArray();
        }
        return retval;
    }

    @Override
    public long getQuotaLimit(final int type) throws MailException {
        initConnection(MailAccount.DEFAULT_ID);
        if (QUOTA_RESOURCE_STORAGE == type) {
            return mailAccess.getFolderStorage().getStorageQuota(INBOX_ID).getLimit();
        } else if (QUOTA_RESOURCE_MESSAGE == type) {
            return mailAccess.getFolderStorage().getMessageQuota(INBOX_ID).getLimit();
        }
        throw new IllegalArgumentException("Unknown quota resource type: " + type);
    }

    @Override
    public long getQuotaUsage(final int type) throws MailException {
        initConnection(MailAccount.DEFAULT_ID);
        if (QUOTA_RESOURCE_STORAGE == type) {
            return mailAccess.getFolderStorage().getStorageQuota(INBOX_ID).getUsage();
        } else if (QUOTA_RESOURCE_MESSAGE == type) {
            return mailAccess.getFolderStorage().getMessageQuota(INBOX_ID).getUsage();
        }
        throw new IllegalArgumentException("Unknown quota resource type: " + type);
    }

    private static com.openexchange.mail.Quota.Type getType(final int type) {
        if (QUOTA_RESOURCE_STORAGE == type) {
            return com.openexchange.mail.Quota.Type.STORAGE;
        } else if (QUOTA_RESOURCE_MESSAGE == type) {
            return com.openexchange.mail.Quota.Type.MESSAGE;
        }
        throw new IllegalArgumentException("Unknown quota resource type: " + type);
    }

    @Override
    public MailMessage getReplyMessageForDisplay(final String folder, final String replyMsgUID, final boolean replyToAll, final UserSettingMail usm) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        final String fullname = argument.getFullname();
        final MailMessage originalMail = mailAccess.getMessageStorage().getMessage(fullname, replyMsgUID, false);
        if (null == originalMail) {
            throw new MailException(MailException.Code.MAIL_NOT_FOUND, replyMsgUID, fullname);
        }
        return mailAccess.getLogicTools().getReplyMessage(originalMail, replyToAll, usm);
    }

    @Override
    public SearchIterator<MailFolder> getRootFolders() throws MailException {
        initConnection(MailAccount.DEFAULT_ID);
        return SearchIteratorAdapter.createArrayIterator(new MailFolder[] { mailAccess.getFolderStorage().getRootFolder() });
    }

    @Override
    public String getSentFolder(final int accountId) throws MailException {
        if (isDefaultFoldersChecked(accountId)) {
            return prepareFullname(accountId, getDefaultMailFolder(StorageUtility.INDEX_SENT, accountId));
        }
        initConnection(accountId);
        return prepareFullname(accountId, mailAccess.getFolderStorage().getSentFolder());
    }

    @Override
    public String getSpamFolder(final int accountId) throws MailException {
        if (isDefaultFoldersChecked(accountId)) {
            return prepareFullname(accountId, getDefaultMailFolder(StorageUtility.INDEX_SPAM, accountId));
        }
        initConnection(accountId);
        return prepareFullname(accountId, mailAccess.getFolderStorage().getSpamFolder());
    }

    @Override
    public SearchIterator<MailMessage> getThreadedMessages(final String folder, final int[] fromToIndices, final int sortCol, final int order, final int[] searchCols, final String[] searchPatterns, final boolean linkSearchTermsWithOR, final int[] fields) throws MailException {
        checkPatternLength(searchPatterns);
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        final String fullname = argument.getFullname();
        final SearchTerm<?> searchTerm =
            (searchCols == null) || (searchCols.length == 0) ? null : SearchUtility.parseFields(
                searchCols,
                searchPatterns,
                linkSearchTermsWithOR);
        /*
         * Identify and thread-sort messages according to search term while only fetching their IDs
         */
        MailMessage[] mails =
            mailAccess.getMessageStorage().getThreadSortedMessages(
                fullname,
                fromToIndices == null ? IndexRange.NULL : new IndexRange(fromToIndices[0], fromToIndices[1]),
                MailSortField.getField(sortCol),
                OrderDirection.getOrderDirection(order),
                searchTerm,
                FIELDS_ID_INFO);
        if ((mails == null) || (mails.length == 0)) {
            return SearchIteratorAdapter.<MailMessage> createEmptyIterator();
        }
        final MailField[] useFields;
        final boolean onlyFolderAndID;
        if (mails.length < mailAccess.getMailConfig().getMailProperties().getMailFetchLimit()) {
            /*
             * Selection fits into cache: Prepare for caching
             */
            useFields = com.openexchange.mail.mime.utils.MIMEStorageUtility.getCacheFieldsArray();
            onlyFolderAndID = false;
        } else {
            useFields = MailField.toFields(MailListField.getFields(fields));
            onlyFolderAndID = onlyFolderAndID(useFields);
        }
        if (!onlyFolderAndID) {
            /*
             * Extract IDs
             */
            final String[] mailIds = new String[mails.length];
            for (int i = 0; i < mailIds.length; i++) {
                mailIds[i] = mails[i].getMailId();
            }
            /*
             * Fetch identified messages by their IDs and pre-fill them according to specified fields
             */
            final MailMessage[] fetchedMails = mailAccess.getMessageStorage().getMessages(fullname, mailIds, useFields);
            /*
             * Apply thread level
             */
            for (int i = 0; i < fetchedMails.length; i++) {
                fetchedMails[i].setThreadLevel(mails[i].getThreadLevel());
            }
            mails = fetchedMails;
        }
        try {
            /*
             * Remove old user cache entries
             */
            final JSONMessageCache cache = JSONMessageCache.getInstance();
            if (null != cache) {
                cache.removeFolder(accountId, fullname, session);
            }
            /*
             * Remove old user cache entries
             */
            MailMessageCache.getInstance().removeFolderMessages(accountId, fullname, session.getUserId(), contextId);
            if ((mails.length > 0) && (mails.length < mailAccess.getMailConfig().getMailProperties().getMailFetchLimit())) {
                /*
                 * ... and put new ones
                 */
                MailMessageCache.getInstance().putMessages(accountId, mails, session.getUserId(), contextId);
            }
        } catch (final OXCachingException e) {
            LOG.error(e.getMessage(), e);
        }
        return SearchIteratorAdapter.createArrayIterator(mails);
    }

    private void checkPatternLength(final String[] patterns) throws MailException {
        final int minimumSearchCharacters;
        try {
            minimumSearchCharacters = ServerConfig.getInt(ServerConfig.Property.MINIMUM_SEARCH_CHARACTERS);
        } catch (final ConfigurationException e) {
            throw new MailException(e);
        }
        if (0 == minimumSearchCharacters || null == patterns) {
            return;
        }
        for (final String pattern : patterns) {
            if (null != pattern && SearchStrings.lengthWithoutWildcards(pattern) < minimumSearchCharacters) {
                throw new MailException(Code.PATTERN_TOO_SHORT, I(minimumSearchCharacters));
            }
        }
    }

    @Override
    public String getTrashFolder(final int accountId) throws MailException {
        if (isDefaultFoldersChecked(accountId)) {
            return prepareFullname(accountId, getDefaultMailFolder(StorageUtility.INDEX_TRASH, accountId));
        }
        initConnection(accountId);
        return prepareFullname(accountId, mailAccess.getFolderStorage().getTrashFolder());
    }

    @Override
    public int getUnreadMessageCount(final String folder) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int accountId = argument.getAccountId();
        final String fullname = argument.getFullname();

        final int retval;

        if (!init) {
            mailAccess = MailAccess.getInstance(session, accountId);
            retval = mailAccess.getUnreadMessagesCount(fullname);
            mailConfig = mailAccess.getMailConfig();
            this.accountId = accountId;
            init = true;
        } else if (accountId != mailAccess.getAccountId()) {
            mailAccess.close(true);
            mailAccess = MailAccess.getInstance(session, accountId);
            retval = mailAccess.getUnreadMessagesCount(fullname);
            mailConfig = mailAccess.getMailConfig();
            this.accountId = accountId;
        } else {
            retval = mailAccess.getUnreadMessagesCount(fullname);
        }

        return retval;
    }

    private void initConnection(final int accountId) throws MailException {
        if (!init) {
            mailAccess = initMailAccess(accountId);
            mailConfig = mailAccess.getMailConfig();
            this.accountId = accountId;
            init = true;
        } else if (accountId != mailAccess.getAccountId()) {
            mailAccess.close(true);
            mailAccess = initMailAccess(accountId);
            mailConfig = mailAccess.getMailConfig();
            this.accountId = accountId;
        }
    }

    private MailAccess<?, ?> initMailAccess(final int accountId) throws MailException {
        /*
         * Fetch a mail access (either from cache or a new instance)
         */
        final MailAccess<?, ?> mailAccess = MailAccess.getInstance(session, accountId);
        if (!mailAccess.isConnected()) {
            /*
             * Get new mail configuration
             */
            final long start = System.currentTimeMillis();
            try {
                mailAccess.connect();
                MailServletInterface.mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
                MailServletInterface.mailInterfaceMonitor.changeNumSuccessfulLogins(true);
            } catch (final MailException e) {
                final int number = e.getDetailNumber();
                if (number == MIMEMailException.Code.LOGIN_FAILED.getNumber() || number == MIMEMailException.Code.INVALID_CREDENTIALS.getNumber()) {
                    MailServletInterface.mailInterfaceMonitor.changeNumFailedLogins(true);
                }
                throw e;
            }
        }
        return mailAccess;
    }

    private boolean isDefaultFoldersChecked(final int accountId) {
        final Boolean b =
            MailSessionCache.getInstance(session).getParameter(accountId, MailSessionParameterNames.getParamDefaultFolderChecked());
        return (b != null) && b.booleanValue();
    }

    @Override
    public String saveDraft(final ComposedMailMessage draftMail, final boolean autosave, final int accountId) throws MailException {
        if (autosave) {
            return autosaveDraft(draftMail, accountId);
        }
        initConnection(accountId);
        final String draftFullname = mailAccess.getFolderStorage().getDraftsFolder();
        final String retval = mailAccess.getMessageStorage().saveDraft(draftFullname, draftMail).getMailPath().toString();
        postEvent(accountId, draftFullname, true);
        return retval;
    }

    private String autosaveDraft(final ComposedMailMessage draftMail, final int accountId) throws MailException {
        initConnection(accountId);
        final String draftFullname = mailAccess.getFolderStorage().getDraftsFolder();
        /*
         * Auto-save draft
         */
        if (!draftMail.isDraft()) {
            draftMail.setFlag(MailMessage.FLAG_DRAFT, true);
        }
        final MailPath msgref = draftMail.getMsgref();
        MailAccess<?, ?> otherAccess = null;
        try {
            final MailMessage origMail;
            if (null == msgref || !draftFullname.equals(msgref.getFolder())) {
                origMail = null;
            } else {
                if (msgref.getAccountId() == accountId) {
                    origMail = mailAccess.getMessageStorage().getMessage(msgref.getFolder(), msgref.getMailID(), false);
                } else {
                    otherAccess = MailAccess.getInstance(session, msgref.getAccountId());
                    otherAccess.connect(true);
                    origMail = otherAccess.getMessageStorage().getMessage(msgref.getFolder(), msgref.getMailID(), false);
                }
                if (origMail != null) {
                    /*
                     * Check for attachments and add them
                     */
                    final NonInlineForwardPartHandler handler = new NonInlineForwardPartHandler();
                    new MailMessageParser().parseMailMessage(origMail, handler);
                    final List<MailPart> parts = handler.getNonInlineParts();
                    if (!parts.isEmpty()) {
                        final TransportProvider tp = TransportProviderRegistry.getTransportProviderBySession(session, accountId);
                        for (final MailPart mailPart : parts) {
                            /*
                             * Create and add a referenced part from original draft mail
                             */
                            draftMail.addEnclosedPart(tp.getNewReferencedPart(mailPart, session));
                        }
                    }
                }
            }
            final String uid;
            {
                final MailMessage filledMail = MIMEMessageConverter.fillComposedMailMessage(draftMail);
                filledMail.setFlag(MailMessage.FLAG_DRAFT, true);
                /*
                 * Append message to draft folder without invoking draftMail.cleanUp() afterwards to avoid loss of possibly uploaded images
                 */
                uid = mailAccess.getMessageStorage().appendMessages(draftFullname, new MailMessage[] { filledMail })[0];
            }
            /*
             * Check for draft-edit operation: Delete old version
             */
            if (origMail != null) {
                if (origMail.isDraft() && null != msgref) {
                    if (msgref.getAccountId() == accountId) {
                        mailAccess.getMessageStorage().deleteMessages(msgref.getFolder(), new String[] { msgref.getMailID() }, true);
                    } else if (null != otherAccess) {
                        otherAccess.getMessageStorage().deleteMessages(msgref.getFolder(), new String[] { msgref.getMailID() }, true);
                    }
                }
                draftMail.setMsgref(null);
            }
            /*
             * Return draft mail
             */
            final MailMessage m = mailAccess.getMessageStorage().getMessage(draftFullname, uid, true);
            if (null == m) {
                throw new MailException(MailException.Code.MAIL_NOT_FOUND, Long.valueOf(uid), draftFullname);
            }
            postEvent(accountId, draftFullname, true);
            return m.getMailPath().toString();
        } finally {
            if (null != otherAccess) {
                otherAccess.close(true);
            }
        }
    }

    @Override
    public String saveFolder(final MailFolderDescription mailFolder) throws MailException {
        if (!mailFolder.containsExists() && !mailFolder.containsFullname()) {
            throw new MailException(MailException.Code.INSUFFICIENT_FOLDER_ATTR);
        }
        if ((mailFolder.containsExists() && mailFolder.exists()) || ((mailFolder.getFullname() != null) && mailAccess.getFolderStorage().exists(
            mailFolder.getFullname()))) {
            /*
             * Update
             */
            final int accountId = mailFolder.getAccountId();
            String fullname = mailFolder.getFullname();
            initConnection(accountId);
            final char separator = mailFolder.getSeparator();
            final String oldParent;
            final String oldName;
            {
                final int pos = fullname.lastIndexOf(separator);
                if (pos == -1) {
                    oldParent = "";
                    oldName = fullname;
                } else {
                    oldParent = fullname.substring(0, pos);
                    oldName = fullname.substring(pos + 1);
                }
            }
            boolean movePerformed = false;
            /*
             * Check if a move shall be performed
             */
            if (mailFolder.containsParentFullname()) {
                final int parentAccountID = mailFolder.getParentAccountId();
                if (accountId == parentAccountID) {
                    final String newParent = mailFolder.getParentFullname();
                    final StringBuilder newFullname = new StringBuilder(newParent).append(mailFolder.getSeparator());
                    if (mailFolder.containsName()) {
                        newFullname.append(mailFolder.getName());
                    } else {
                        newFullname.append(oldName);
                    }
                    if (!newParent.equals(oldParent)) { // move & rename
                        final Map<String, Map<?, ?>> subfolders = subfolders(fullname);
                        fullname = mailAccess.getFolderStorage().moveFolder(fullname, newFullname.toString());
                        movePerformed = true;
                        postEvent4Subfolders(accountId, subfolders);
                        postEvent(accountId, newParent, false);
                    }
                } else {
                    // Move to another account
                    final MailAccess<?, ?> otherAccess = initMailAccess(parentAccountID);
                    try {
                        final String newParent = mailFolder.getParentFullname();
                        // Check if parent mail folder exists
                        final MailFolder p = otherAccess.getFolderStorage().getFolder(newParent);
                        // Check permission on new parent
                        final MailPermission ownPermission = p.getOwnPermission();
                        if (!ownPermission.canCreateSubfolders()) {
                            throw new MailException(MailException.Code.NO_CREATE_ACCESS, newParent);
                        }
                        // Check for duplicate
                        final MailFolder[] tmp = otherAccess.getFolderStorage().getSubfolders(newParent, true);
                        final String lookFor = mailFolder.containsName() ? mailFolder.getName() : oldName;
                        for (final MailFolder sub : tmp) {
                            if (sub.getName().equals(lookFor)) {
                                throw new MailException(MailException.Code.DUPLICATE_FOLDER, lookFor);
                            }
                        }
                        // Copy
                        final String destFullname =
                            fullCopy(
                                mailAccess,
                                fullname,
                                otherAccess,
                                newParent,
                                p.getSeparator(),
                                session.getUserId(),
                                otherAccess.getMailConfig().getCapabilities().hasPermissions());
                        postEvent(parentAccountID, newParent, false);
                        // Delete source
                        final Map<String, Map<?, ?>> subfolders = subfolders(fullname);
                        mailAccess.getFolderStorage().deleteFolder(fullname, true);
                        // Perform other updates
                        final String prepareFullname =
                            prepareFullname(parentAccountID, otherAccess.getFolderStorage().updateFolder(destFullname, mailFolder));
                        postEvent4Subfolders(accountId, subfolders);
                        return prepareFullname;
                    } finally {
                        otherAccess.close(true);
                    }
                }
            }
            /*
             * Check if a rename shall be performed
             */
            if (!movePerformed && mailFolder.containsName()) {
                final String newName = mailFolder.getName();
                if (!newName.equals(oldName)) { // rename
                    fullname = mailAccess.getFolderStorage().renameFolder(fullname, newName);
                    postEvent(accountId, fullname, false);
                }
            }
            /*
             * Handle update of permission or subscription
             */
            final String prepareFullname = prepareFullname(accountId, mailAccess.getFolderStorage().updateFolder(fullname, mailFolder));
            postEvent(accountId, fullname, false);
            return prepareFullname;
        }
        /*
         * Insert
         */
        final int accountId = mailFolder.getParentAccountId();
        initConnection(accountId);
        final String prepareFullname = prepareFullname(accountId, mailAccess.getFolderStorage().createFolder(mailFolder));
        postEvent(accountId, mailFolder.getParentFullname(), false);
        return prepareFullname;
    }

    private static String fullCopy(final MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> srcAccess, final String srcFullname, final MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> destAccess, final String destParent, final char destSeparator, final int user, final boolean hasPermissions) throws MailException {
        // Create folder
        final MailFolder source = srcAccess.getFolderStorage().getFolder(srcFullname);
        final MailFolderDescription mfd = new MailFolderDescription();
        mfd.setName(source.getName());
        mfd.setParentFullname(destParent);
        mfd.setSeparator(destSeparator);
        mfd.setSubscribed(source.isSubscribed());
        if (hasPermissions) {
            // Copy permissions
            final MailPermission[] perms = source.getPermissions();
            try {
                for (int i = 0; i < perms.length; i++) {
                    mfd.addPermission((MailPermission) perms[i].clone());
                }
            } catch (final CloneNotSupportedException e) {
                throw new MailException(MailException.Code.UNEXPECTED_ERROR, e, e.getMessage());
            }
        }
        final String destFullname = destAccess.getFolderStorage().createFolder(mfd);
        // Copy messages
        final MailMessage[] msgs =
            srcAccess.getMessageStorage().getAllMessages(srcFullname, null, MailSortField.RECEIVED_DATE, OrderDirection.ASC, FIELDS_FULL);
        final IMailMessageStorage destMessageStorage = destAccess.getMessageStorage();
        // Append messages to destination account
        /* final String[] mailIds = */destMessageStorage.appendMessages(destFullname, msgs);
        /*-
         * 
        // Ensure flags
        final String[] arr = new String[1];
        for (int i = 0; i < msgs.length; i++) {
            final MailMessage m = msgs[i];
            final String mailId = mailIds[i];
            if (null != m && null != mailId) {
                arr[0] = mailId;
                // System flags
                destMessageStorage.updateMessageFlags(destFullname, arr, m.getFlags(), true);
                // Color label
                if (m.containsColorLabel() && m.getColorLabel() != MailMessage.COLOR_LABEL_NONE) {
                    destMessageStorage.updateMessageColorLabel(destFullname, arr, m.getColorLabel());
                }
            }
        }
         */
        // Iterate subfolders
        final MailFolder[] tmp = srcAccess.getFolderStorage().getSubfolders(srcFullname, true);
        for (int i = 0; i < tmp.length; i++) {
            fullCopy(srcAccess, tmp[i].getFullname(), destAccess, destFullname, destSeparator, user, hasPermissions);
        }
        return destFullname;
    }

    @Override
    public String sendMessage(final ComposedMailMessage composedMail, final ComposeType type, final int accountId) throws MailException {
        /*
         * Initialize
         */
        initConnection(accountId);
        final MailTransport transport = MailTransport.getInstance(session, accountId);
        try {
            /*
             * Send mail
             */
            final long startTransport = System.currentTimeMillis();
            final MailMessage sentMail = transport.sendMailMessage(composedMail, type);
            /*
             * Email successfully sent, trigger data retention
             */
            final DataRetentionService retentionService = ServerServiceRegistry.getInstance().getService(DataRetentionService.class);
            if (null != retentionService) {
                /*
                 * Create runnable task
                 */
                final Runnable r = new Runnable() {

                    public void run() {
                        try {
                            final RetentionData retentionData = retentionService.newInstance();
                            retentionData.setStartTime(new Date(startTransport));
                            retentionData.setIdentifier(transport.getTransportConfig().getLogin());
                            retentionData.setIPAddress(session.getLocalIp());
                            retentionData.setSenderAddress(sentMail.getFrom()[0].getAddress());
                            final Set<InternetAddress> recipients = new HashSet<InternetAddress>(Arrays.asList(sentMail.getTo()));
                            recipients.addAll(Arrays.asList(sentMail.getCc()));
                            recipients.addAll(Arrays.asList(sentMail.getBcc()));
                            final int size = recipients.size();
                            final String[] recipientsArr = new String[size];
                            final Iterator<InternetAddress> it = recipients.iterator();
                            for (int i = 0; i < size; i++) {
                                recipientsArr[i] = it.next().getAddress();
                            }
                            retentionData.setRecipientAddresses(recipientsArr);
                            /*
                             * Finally store it
                             */
                            retentionService.storeOnTransport(retentionData);
                        } catch (final MailException e) {
                            LOG.error(e.getMessage(), e);
                        } catch (final DataRetentionException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                };
                /*
                 * Check if timer service is available to delegate execution
                 */
                final ThreadPoolService threadPool = ServerServiceRegistry.getInstance().getService(ThreadPoolService.class);
                if (null == threadPool) {
                    // Execute in this thread
                    r.run();
                } else {
                    // Delegate runnable to thread pool
                    threadPool.submit(ThreadPools.task(r), CallerRunsBehavior.getInstance());
                }
            }
            /*
             * Check for a reply/forward
             */
            if (ComposeType.REPLY.equals(type)) {
                final MailPath path = composedMail.getMsgref();
                if (null == path) {
                    LOG.warn("Missing msgref on reply. Corresponding mail cannot be marked as answered.", new Throwable());
                } else {
                    /*
                     * Mark referenced mail as answered
                     */
                    final String fullname = path.getFolder();
                    final String[] uids = new String[] { path.getMailID() };
                    mailAccess.getMessageStorage().updateMessageFlags(fullname, uids, MailMessage.FLAG_ANSWERED, true);
                    try {
                        /*
                         * Update JSON cache
                         */
                        final JSONMessageCache cache = JSONMessageCache.getInstance();
                        if (null != cache) {
                            cache.removeFolder(mailAccess.getAccountId(), fullname, session);
                        }
                        if (MailMessageCache.getInstance().containsFolderMessages(
                            mailAccess.getAccountId(),
                            fullname,
                            session.getUserId(),
                            contextId)) {
                            /*
                             * Update cache entries
                             */
                            MailMessageCache.getInstance().updateCachedMessages(
                                uids,
                                mailAccess.getAccountId(),
                                fullname,
                                session.getUserId(),
                                contextId,
                                FIELDS_FLAGS,
                                new Object[] { Integer.valueOf(MailMessage.FLAG_ANSWERED) });
                        }
                    } catch (final OXCachingException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            } else if (ComposeType.FORWARD.equals(type)) {
                final MailPath supPath = composedMail.getMsgref();
                if (null == supPath) {
                    final int count = composedMail.getEnclosedCount();
                    final String[] ids = new String[1];
                    for (int i = 0; i < count; i++) {
                        final MailPart part = composedMail.getEnclosedMailPart(i);
                        final MailPath path = part.getMsgref();
                        if ((path != null) && part.getContentType().isMimeType(MIMETypes.MIME_MESSAGE_RFC822)) {
                            /*
                             * Mark referenced mail as forwarded
                             */
                            ids[0] = path.getMailID();
                            mailAccess.getMessageStorage().updateMessageFlags(path.getFolder(), ids, MailMessage.FLAG_FORWARDED, true);
                            try {
                                JSONMessageCache.getInstance().removeFolder(mailAccess.getAccountId(), path.getFolder(), session);
                                if (MailMessageCache.getInstance().containsFolderMessages(
                                    mailAccess.getAccountId(),
                                    path.getFolder(),
                                    session.getUserId(),
                                    contextId)) {
                                    /*
                                     * Update cache entries
                                     */
                                    MailMessageCache.getInstance().updateCachedMessages(
                                        ids,
                                        mailAccess.getAccountId(),
                                        path.getFolder(),
                                        session.getUserId(),
                                        contextId,
                                        FIELDS_FLAGS,
                                        new Object[] { Integer.valueOf(MailMessage.FLAG_FORWARDED) });
                                }
                            } catch (final OXCachingException e) {
                                LOG.error(e.getMessage(), e);
                            }
                        }
                    }
                } else {
                    /*
                     * Mark referenced mail as forwarded
                     */
                    final String fullname = supPath.getFolder();
                    final String[] uids = new String[] { supPath.getMailID() };
                    mailAccess.getMessageStorage().updateMessageFlags(fullname, uids, MailMessage.FLAG_FORWARDED, true);
                    try {
                        JSONMessageCache.getInstance().removeFolder(mailAccess.getAccountId(), fullname, session);
                        if (MailMessageCache.getInstance().containsFolderMessages(
                            mailAccess.getAccountId(),
                            fullname,
                            session.getUserId(),
                            contextId)) {
                            /*
                             * Update cache entries
                             */
                            MailMessageCache.getInstance().updateCachedMessages(
                                uids,
                                mailAccess.getAccountId(),
                                fullname,
                                session.getUserId(),
                                contextId,
                                FIELDS_FLAGS,
                                new Object[] { Integer.valueOf(MailMessage.FLAG_FORWARDED) });
                        }
                    } catch (final OXCachingException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
            if (UserSettingMailStorage.getInstance().getUserSettingMail(session.getUserId(), ctx).isNoCopyIntoStandardSentFolder()) {
                /*
                 * No copy in sent folder
                 */
                return null;
            }
            /*
             * Append to Sent folder
             */
            final long start = System.currentTimeMillis();
            final String sentFullname = mailAccess.getFolderStorage().getSentFolder();
            final String[] uidArr;
            try {
                uidArr = mailAccess.getMessageStorage().appendMessages(sentFullname, new MailMessage[] { sentMail });
                try {
                    /*
                     * Update caches
                     */
                    JSONMessageCache.getInstance().removeFolder(mailAccess.getAccountId(), sentFullname, session);

                    MailMessageCache.getInstance().removeFolderMessages(
                        mailAccess.getAccountId(),
                        sentFullname,
                        session.getUserId(),
                        contextId);
                } catch (final OXCachingException e) {
                    LOG.error(e.getMessage(), e);
                }
            } catch (final MailException e) {
                if (e.getMessage().indexOf("quota") != -1) {
                    throw new MailException(MailException.Code.COPY_TO_SENT_FOLDER_FAILED_QUOTA, e, new Object[0]);
                }
                throw new MailException(MailException.Code.COPY_TO_SENT_FOLDER_FAILED, e, new Object[0]);
            }
            if ((uidArr != null) && (uidArr[0] != null)) {
                /*
                 * Mark appended sent mail as seen
                 */
                mailAccess.getMessageStorage().updateMessageFlags(sentFullname, uidArr, MailMessage.FLAG_SEEN, true);
            }
            final MailPath retval = new MailPath(mailAccess.getAccountId(), sentFullname, uidArr[0]);
            if (DEBUG_ENABLED) {
                LOG.debug(new StringBuilder(128).append("Mail copy (").append(retval.toString()).append(") appended in ").append(
                    System.currentTimeMillis() - start).append("msec").toString());
            }
            return retval.toString();
        } finally {
            transport.close();
        }
    }

    @Override
    public void sendReceiptAck(final String folder, final String msgUID, final String fromAddr) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int acc = argument.getAccountId();
        try {
            final MailAccountStorageService ss = ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class, true);
            final MailAccount ma = ss.getMailAccount(acc, session.getUserId(), session.getContextId());
            if (ma.isDefaultAccount()) {
                /*
                 * Check for valid from address
                 */
                try {
                    final Set<InternetAddress> validAddrs = new HashSet<InternetAddress>(4);
                    if (usm.getSendAddr() != null && usm.getSendAddr().length() > 0) {
                        validAddrs.add(new QuotedInternetAddress(usm.getSendAddr()));
                    }
                    final User user = UserStorage.getStorageUser(session.getUserId(), session.getContextId());
                    validAddrs.add(new QuotedInternetAddress(user.getMail()));
                    final String[] aliases = user.getAliases();
                    for (final String alias : aliases) {
                        validAddrs.add(new QuotedInternetAddress(alias));
                    }
                    if (!validAddrs.contains(new QuotedInternetAddress(fromAddr))) {
                        throw new MailException(MailException.Code.INVALID_SENDER, fromAddr);
                    }
                } catch (final AddressException e) {
                    throw MIMEMailException.handleMessagingException(e);
                }
            } else {
                if (!new QuotedInternetAddress(ma.getPrimaryAddress()).equals(new QuotedInternetAddress(fromAddr))) {
                    throw new MailException(MailException.Code.INVALID_SENDER, fromAddr);
                }
            }
        } catch (final AddressException e) {
            throw MIMEMailException.handleMessagingException(e);
        } catch (final ServiceException e) {
            throw new MailException(e);
        } catch (final MailAccountException e) {
            throw new MailException(e);
        }
        /*
         * Initialize
         */
        initConnection(acc);
        final String fullname = argument.getFullname();
        final MailTransport transport = MailTransport.getInstance(session);
        try {
            transport.sendReceiptAck(mailAccess.getMessageStorage().getMessage(fullname, msgUID, false), fromAddr);
        } finally {
            transport.close();
        }
        mailAccess.getMessageStorage().updateMessageFlags(fullname, new String[] { msgUID }, MailMessage.FLAG_READ_ACK, true);
    }

    private static final MailListField[] FIELDS_COLOR_LABEL = new MailListField[] { MailListField.COLOR_LABEL };

    @Override
    public void updateMessageColorLabel(final String folder, final String[] msgUID, final int newColorLabel) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        final String fullname = argument.getFullname();
        mailAccess.getMessageStorage().updateMessageColorLabel(fullname, msgUID, newColorLabel);
        /*
         * Update caches
         */
        {
            final JSONMessageCache jsonMessageCache = JSONMessageCache.getInstance();
            if (null != jsonMessageCache) {
                final List<String> updateIds = new ArrayList<String>(msgUID.length);
                for (int i = 0; i < msgUID.length; i++) {
                    final String uid = msgUID[i];
                    if (jsonMessageCache.containsKey(accountId, fullname, uid, session)) {
                        updateIds.add(uid);
                    }
                }
                if (!updateIds.isEmpty()) {
                    /*
                     * Update color label in JSON message cache
                     */
                    jsonMessageCache.updateColorFlag(accountId, fullname, msgUID, newColorLabel, session);
                }
            }
        }
        try {
            if (MailMessageCache.getInstance().containsFolderMessages(accountId, fullname, session.getUserId(), contextId)) {
                /*
                 * Update cache entries
                 */
                MailMessageCache.getInstance().updateCachedMessages(
                    msgUID,
                    accountId,
                    fullname,
                    session.getUserId(),
                    contextId,
                    FIELDS_COLOR_LABEL,
                    new Object[] { Integer.valueOf(newColorLabel) });
            }
        } catch (final OXCachingException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public String getMailIDByMessageID(final String folder, final String messageID) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        final String fullname = argument.getFullname();
        final MailMessage[] messages =
            mailAccess.getMessageStorage().searchMessages(
                fullname,
                null,
                MailSortField.RECEIVED_DATE,
                OrderDirection.ASC,
                new HeaderTerm("Message-Id", messageID),
                FIELDS_ID_INFO);
        if (null == messages || 1 != messages.length) {
            throw new MailException(MailException.Code.MAIL_NOT_FOUN_BY_MESSAGE_ID, fullname, messageID);
        }
        return messages[0].getMailId();
    }

    @Override
    public void updateMessageFlags(final String folder, final String[] mailIDs, final int flagBits, final boolean flagVal) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        final String fullname = argument.getFullname();
        mailAccess.getMessageStorage().updateMessageFlags(fullname, mailIDs, flagBits, flagVal);
        /*
         * Update caches
         */
        {
            final JSONMessageCache jsonMessageCache = JSONMessageCache.getInstance();
            if (null != jsonMessageCache) {
                final List<String> updateIds = new ArrayList<String>(mailIDs.length);
                for (int i = 0; i < mailIDs.length; i++) {
                    final String uid = mailIDs[i];
                    if (jsonMessageCache.containsKey(accountId, fullname, uid, session)) {
                        updateIds.add(uid);
                    }
                }
                if (!updateIds.isEmpty()) {
                    // toArray
                    final String[] updateIdsArr = updateIds.toArray(new String[updateIds.size()]);
                    // Optimize for set to seen/unseen
                    int flags = flagBits;
                    if ((flags & MailMessage.FLAG_SEEN) > 0) {
                        // Strip \Seen flag from bit mask
                        flags = (flags & ~MailMessage.FLAG_SEEN);
                        // Invoke special method for \Seen flag
                        final int unread = mailAccess.getUnreadMessagesCount(fullname);
                        jsonMessageCache.switchSeenFlag(accountId, fullname, updateIdsArr, flagVal, unread, session);
                    }
                    if (flags > 0) { // Any flags left after \Seen removed?
                        jsonMessageCache.updateFlags(accountId, fullname, updateIdsArr, flags, flagVal, session);
                    }
                }
            }
        }
        if (usm.isSpamEnabled() && ((flagBits & MailMessage.FLAG_SPAM) > 0)) {
            /*
             * Remove from caches
             */
            try {
                if (MailMessageCache.getInstance().containsFolderMessages(accountId, fullname, session.getUserId(), contextId)) {
                    MailMessageCache.getInstance().removeMessages(mailIDs, accountId, fullname, session.getUserId(), contextId);

                }
            } catch (final OXCachingException e) {
                LOG.error(e.getMessage(), e);
            }
        } else {
            try {
                if (MailMessageCache.getInstance().containsFolderMessages(accountId, fullname, session.getUserId(), contextId)) {
                    /*
                     * Update cache entries
                     */
                    MailMessageCache.getInstance().updateCachedMessages(
                        mailIDs,
                        accountId,
                        fullname,
                        session.getUserId(),
                        contextId,
                        FIELDS_FLAGS,
                        new Object[] { Integer.valueOf(flagVal ? flagBits : (flagBits * -1)) });
                }
            } catch (final OXCachingException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public MailMessage[] getUpdatedMessages(final String folder, final int[] fields) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        final String fullname = argument.getFullname();
        return mailAccess.getMessageStorage().getNewAndModifiedMessages(fullname, MailField.getFields(fields));
    }

    @Override
    public MailMessage[] getDeletedMessages(final String folder, final int[] fields) throws MailException {
        final FullnameArgument argument = prepareMailFolderParam(folder);
        final int accountId = argument.getAccountId();
        initConnection(accountId);
        final String fullname = argument.getFullname();
        return mailAccess.getMessageStorage().getDeletedMessages(fullname, MailField.getFields(fields));
    }

    /*-
     * ################################################################################
     * #############################   HELPER CLASSES   ###############################
     * ################################################################################
     */

    private static final class MailFolderComparator implements Comparator<MailFolder> {

        private final Map<String, Integer> indexMap;

        private final Collator collator;

        private final Integer na;

        public MailFolderComparator(final String[] names, final Locale locale) {
            super();
            indexMap = new HashMap<String, Integer>(names.length);
            for (int i = 0; i < names.length; i++) {
                indexMap.put(names[i], Integer.valueOf(i));
            }
            na = Integer.valueOf(names.length);
            collator = Collator.getInstance(locale);
            collator.setStrength(Collator.SECONDARY);
        }

        private Integer getNumberOf(final String name) {
            final Integer ret = indexMap.get(name);
            if (null == ret) {
                return na;
            }
            return ret;
        }

        public int compare(final MailFolder o1, final MailFolder o2) {
            if (o1.isDefaultFolder()) {
                if (o2.isDefaultFolder()) {
                    return getNumberOf(o1.getFullname()).compareTo(getNumberOf(o2.getFullname()));
                }
                return -1;
            }
            if (o2.isDefaultFolder()) {
                return 1;
            }
            return collator.compare(o1.getName(), o2.getName());
        }
    }

    private static final class SimpleMailFolderComparator implements Comparator<MailFolder> {

        private final Collator collator;

        public SimpleMailFolderComparator(final Locale locale) {
            super();
            collator = Collator.getInstance(locale);
            collator.setStrength(Collator.SECONDARY);
        }

        public int compare(final MailFolder o1, final MailFolder o2) {
            return collator.compare(o1.getName(), o2.getName());
        }
    }

    private static String[] messages2ids(final MailMessage[] messages) {
        if (null == messages) {
            return null;
        }
        final String[] retval = new String[messages.length];
        for (int i = 0; i < messages.length; i++) {
            retval[i] = messages[i].getMailId();
        }
        return retval;
    }

    private void postEvent(final int accountId, final String fullname, final boolean contentRelated) {
        if (MailAccount.DEFAULT_ID != accountId) {
            /*
             * TODO: No event for non-primary account?
             */
            return;
        }
        EventPool.getInstance().put(
            new PooledEvent(contextId, session.getUserId(), accountId, prepareFullname(accountId, fullname), contentRelated, session));
    }

}
