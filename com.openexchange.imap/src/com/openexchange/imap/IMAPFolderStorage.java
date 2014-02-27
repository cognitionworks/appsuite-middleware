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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

import static com.openexchange.java.Strings.quoteReplacement;
import static com.openexchange.mail.MailServletInterface.mailInterfaceMonitor;
import static com.openexchange.mail.dataobjects.MailFolder.DEFAULT_FOLDER_ID;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Quota;
import javax.mail.Quota.Resource;
import javax.mail.StoreClosedException;
import javax.mail.search.FlagTerm;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Reloadable;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.i18n.MailStrings;
import com.openexchange.groupware.ldap.UserExceptionCode;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.imap.OperationKey.Type;
import com.openexchange.imap.acl.ACLExtension;
import com.openexchange.imap.cache.FolderCache;
import com.openexchange.imap.cache.ListLsubCache;
import com.openexchange.imap.cache.ListLsubEntry;
import com.openexchange.imap.cache.ListLsubRuntimeException;
import com.openexchange.imap.cache.MBoxEnabledCache;
import com.openexchange.imap.cache.NamespaceFoldersCache;
import com.openexchange.imap.cache.RightsCache;
import com.openexchange.imap.cache.RootSubfolderCache;
import com.openexchange.imap.cache.UserFlagsCache;
import com.openexchange.imap.command.CopyIMAPCommand;
import com.openexchange.imap.command.FlagsIMAPCommand;
import com.openexchange.imap.config.IMAPConfig;
import com.openexchange.imap.config.IMAPReloadable;
import com.openexchange.imap.converters.IMAPFolderConverter;
import com.openexchange.imap.dataobjects.IMAPMailFolder;
import com.openexchange.imap.entity2acl.Entity2ACL;
import com.openexchange.imap.entity2acl.Entity2ACLArgs;
import com.openexchange.imap.entity2acl.Entity2ACLExceptionCode;
import com.openexchange.imap.entity2acl.UserGroupID;
import com.openexchange.imap.notify.internal.IMAPNotifierMessageRecentListener;
import com.openexchange.imap.services.Services;
import com.openexchange.imap.util.IMAPSessionStorageAccess;
import com.openexchange.java.StringAllocator;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.api.IMailFolderStorageEnhanced2;
import com.openexchange.mail.api.IMailFolderStorageInfoSupport;
import com.openexchange.mail.api.MailFolderStorage;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.mail.dataobjects.MailFolder.DefaultFolderType;
import com.openexchange.mail.dataobjects.MailFolderDescription;
import com.openexchange.mail.dataobjects.MailFolderInfo;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.mail.mime.MimeMailExceptionCode;
import com.openexchange.mail.permission.DefaultMailPermission;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.mail.utils.StorageUtility;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.sun.mail.iap.CommandFailedException;
import com.sun.mail.iap.ParsingException;
import com.sun.mail.imap.ACL;
import com.sun.mail.imap.DefaultFolder;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.Rights;

/**
 * {@link IMAPFolderStorage} - The IMAP folder storage implementation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class IMAPFolderStorage extends MailFolderStorage implements IMailFolderStorageEnhanced2, IMailFolderStorageInfoSupport {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IMAPFolderStorage.class);

    /**
     * Gets the max. length for a mailbox name
     */
    private static volatile Integer maxMailboxNameLength;
    private static int maxMailboxNameLength() {
        Integer tmp = maxMailboxNameLength;
        if (null == tmp) {
            synchronized (IMAPFolderStorage.class) {
                tmp = maxMailboxNameLength;
                if (null == tmp) {
                    final ConfigurationService service = Services.getService(ConfigurationService.class);
                    tmp = Integer.valueOf(null == service ? 60 : service.getIntProperty("com.openexchange.imap.maxMailboxNameLength", 60));
                    maxMailboxNameLength = tmp;
                }
            }
        }
        return tmp.intValue();
    }

    static {
        IMAPReloadable.getInstance().addReloadable(new Reloadable() {

            @Override
            public void reloadConfiguration(final ConfigurationService configService) {
                maxMailboxNameLength = null;
                invalidChars = null;
            }

            @Override
            public Map<String, String[]> getConfigfileNames() {
                return null;
            }
        });
    }

    private static final String STR_INBOX = "INBOX";

    private static final char[] STR_MSEC = new char[] { 'm','s','e','c' };

    private final IMAPStore imapStore;
    private final IMAPAccess imapAccess;
    private final int accountId;
    private final Session session;
    private final Context ctx;
    private final IMAPConfig imapConfig;

    private Character separator;
    private IMAPDefaultFolderChecker checker;

    /**
     * Initializes a new {@link IMAPFolderStorage}
     *
     * @param imapStore The IMAP store
     * @param imapAccess The IMAP access
     * @param session The session providing needed user data
     * @throws OXException If context loading fails
     */
    public IMAPFolderStorage(final IMAPStore imapStore, final IMAPAccess imapAccess, final Session session) throws OXException {
        super();
        this.imapStore = imapStore;
        this.imapAccess = imapAccess;
        accountId = imapAccess.getAccountId();
        this.session = session;
        ctx = session instanceof ServerSession ? ((ServerSession) session).getContext() : ContextStorage.getStorageContext(session.getContextId());
        imapConfig = imapAccess.getIMAPConfig();
    }

    /**
     * Gets the associated session.
     *
     * @return The session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Gets the associated context.
     *
     * @return The context
     */
    public Context getContext() {
        return ctx;
    }

    /**
     * Gets the associated IMAP configuration.
     *
     * @return The IMAP configuration
     */
    public IMAPConfig getImapConfig() {
        return imapConfig;
    }

    /**
     * Gets the IMAP access.
     *
     * @return The IMAP access
     */
    public IMAPAccess getImapAccess() {
        return imapAccess;
    }

    /**
     * Gets the associated IMAP store.
     *
     * @return The IMAP store
     */
    public IMAPStore getImapStore() {
        return imapStore;
    }

    /**
     * Gets the associated account identifier.
     *
     * @return The account identifier
     */
    public int getAccountId() {
        return accountId;
    }

    /**
     * Clears the folder cache.
     */
    public void clearCache() {
        FolderCache.removeCachedFolders(session, accountId);
        // ListLsubCache.clearCache(accountId, session);
    }

    /**
     * Removes the IMAP folder denoted by specified full name.
     *
     * @param modifiedFullname The full name of the folder which has been modified
     */
    public void removeFromCache(final String modifiedFullname) {
        FolderCache.removeCachedFolder(modifiedFullname, session, accountId);
        // ListLsubCache.removeCachedEntry(modifiedFullname, accountId, session);
    }

    /**
     * Decrements unread message counter from cached IMAP folder.
     *
     * @param fullName The IMAP folder full name
     */
    public void decrementUnreadMessageCount(final String fullName) {
        FolderCache.decrementUnreadMessageCount(fullName, session, accountId);
    }

    /**
     * Updates the cached IMAP folder if message has changed.
     *
     * @param imapFolder The IMAP folder
     * @param total The message count
     * @return <code>true</code> if updated; otherwise <code>false</code>
     */
    public boolean updateCacheIfDiffer(final IMAPFolder imapFolder, final int total) {
        final String fullName = imapFolder.getFullName();
        final MailFolder mailFolder = FolderCache.optCachedFolder(fullName, this);
        if (null != mailFolder) {
            try {
                final int cachedTotal = mailFolder.getMessageCount();
                if (cachedTotal >= 0 && cachedTotal != total) {
                    FolderCache.updateCachedFolder(fullName, this, imapFolder);
                    return true;
                }
            } catch (final OXException e) {
                LOG.warn("Updating IMAP folder cache failed.", e);
                FolderCache.removeCachedFolder(fullName, session, accountId);
                ListLsubCache.removeCachedEntry(fullName, accountId, session);
            }
        }
        return false;
    }

    /**
     * Removes the IMAP folders denoted by specified set of full names.
     *
     * @param modifiedFullnames The full names of the folders which have been modified
     */
    public void removeFromCache(final Set<String> modifiedFullnames) {
        for (final String modifiedFullname : modifiedFullnames) {
            FolderCache.removeCachedFolder(modifiedFullname, session, accountId);
            // ListLsubCache.removeCachedEntry(modifiedFullname, accountId, session);
        }
    }

    private IMAPDefaultFolderChecker getChecker() {
        if (null == checker) {
            if (imapConfig.asMap().containsKey("SPECIAL-USE")) {
                // Supports SPECIAL-USE capability
                checker = new SpecialUseDefaultFolderChecker(accountId, session, ctx, imapStore, imapAccess);
            } else {
                checker = new IMAPDefaultFolderChecker(accountId, session, ctx, imapStore, imapAccess);
            }
        }
        return checker;
    }

    private char getSeparator() throws MessagingException {
        if (null == separator) {
            separator = Character.valueOf(imapStore.getDefaultFolder().getSeparator());
        }
        return separator.charValue();
    }

    @Override
    public boolean isInfoSupported() throws OXException {
        return true;
    }

    @Override
    public MailFolderInfo getFolderInfo(final String fullName) throws OXException {
        try {
            final String fn = (DEFAULT_FOLDER_ID.equals(fullName) ? "" : fullName);
            final ListLsubEntry entry = ListLsubCache.getCachedLISTEntry(fn, accountId, imapStore, session);
            if (entry.exists()) {
                return toFolderInfo(entry);
            }

            // Retrieve folder...
            IMAPFolder f;
            if (0 == fn.length()) {
                f = (IMAPFolder) imapStore.getDefaultFolder();
            } else {
                f = (IMAPFolder) imapStore.getFolder(fullName);
            }
            // ... and check existence
            if (!f.exists()) {
                f = checkForNamespaceFolder(fn);
                if (null == f) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
            }

            return IMAPFolderConverter.convertFolder(f, session, imapAccess, ctx).asMailFolderInfo(accountId);
        } catch (final MessagingException e) {
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, new HashMap<String, Object>(0));
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public List<MailFolderInfo> getAllFolderInfos(final boolean subscribedOnly) throws OXException {
        return getFolderInfos(null, subscribedOnly);
    }

    @Override
    public List<MailFolderInfo> getFolderInfos(final String optParentFullName, final boolean subscribedOnly) throws OXException {
        try {
            final String pfn = null == optParentFullName ? null : (DEFAULT_FOLDER_ID.equals(optParentFullName) ? "" : optParentFullName);
            final List<ListLsubEntry> allEntries = ListLsubCache.getAllEntries(pfn, accountId, subscribedOnly, imapStore, session);

            // User's locale
            final Locale locale = (session instanceof ServerSession ? ((ServerSession) session).getUser() : UserStorage.getInstance().getUser(session.getUserId(), session.getContextId())).getLocale();

            // Check whether to consider standard folders
            if (!considerStandardFolders(pfn)) {
                final List<MailFolderInfo> retval = new ArrayList<MailFolderInfo>(allEntries.size());

                // Fill list
                for (final ListLsubEntry entry : allEntries) {
                    final MailFolderInfo mfi = toFolderInfo(entry);
                    retval.add(mfi);
                }

                // Sort & return
                Collections.sort(retval, new FullDisplayNameComparator(locale));
                return retval;
            }

            // ------------------------------------------------------------------------------------------------------ //
            // Fill map
            final int size = allEntries.size();
            final Map<String, MailFolderInfo> map = new HashMap<String, MailFolderInfo>(size);
            for (final ListLsubEntry entry : allEntries) {
                final MailFolderInfo mfi = toFolderInfo(entry);
                map.put(mfi.getFullname(), mfi);
            }

            // Determine standard folders
            {
                final StringHelper stringHelper = StringHelper.valueOf(locale);
                for (int index = 0; index < 7; index++) {
                    final String fn = getChecker().getDefaultFolder(index);
                    if (null != fn) {
                        final MailFolderInfo mfi = map.get(fn);
                        if (null != mfi) {
                            mfi.setDefaultFolder(true);
                            switch (index) {
                            case StorageUtility.INDEX_CONFIRMED_HAM:
                                mfi.setDefaultFolderType(DefaultFolderType.CONFIRMED_HAM);
                                mfi.setDisplayName(stringHelper.getString(MailStrings.CONFIRMED_HAM));
                                break;
                            case StorageUtility.INDEX_CONFIRMED_SPAM:
                                mfi.setDefaultFolderType(DefaultFolderType.CONFIRMED_SPAM);
                                mfi.setDisplayName(stringHelper.getString(MailStrings.CONFIRMED_SPAM));
                                break;
                            case StorageUtility.INDEX_DRAFTS:
                                mfi.setDefaultFolderType(DefaultFolderType.DRAFTS);
                                mfi.setDisplayName(stringHelper.getString(MailStrings.DRAFTS));
                                break;
                            case StorageUtility.INDEX_SENT:
                                mfi.setDefaultFolderType(DefaultFolderType.SENT);
                                mfi.setDisplayName(stringHelper.getString(MailStrings.SENT));
                                break;
                            case StorageUtility.INDEX_SPAM:
                                mfi.setDefaultFolderType(DefaultFolderType.SPAM);
                                mfi.setDisplayName(stringHelper.getString(MailStrings.SPAM));
                                break;
                            case StorageUtility.INDEX_TRASH:
                                mfi.setDefaultFolderType(DefaultFolderType.TRASH);
                                mfi.setDisplayName(stringHelper.getString(MailStrings.TRASH));
                                break;
                            case StorageUtility.INDEX_INBOX:
                                mfi.setDefaultFolderType(DefaultFolderType.INBOX);
                                mfi.setDisplayName(stringHelper.getString(MailStrings.INBOX));
                                break;
                            default:
                                break;
                            }
                        }
                    }
                }
            }

            // Sort & return
            final List<MailFolderInfo> retval = new ArrayList<MailFolderInfo>(map.values());
            Collections.sort(retval, new FullDisplayNameComparator(locale));
            return retval;
        } catch (final MessagingException e) {
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, new HashMap<String, Object>(0));
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private boolean considerStandardFolders(final String parentFullName) throws OXException {
        final boolean considerStandardFolders;
        if (null == parentFullName) {
            considerStandardFolders = true;
        } else {
            final String prefix = getDefaultFolderPrefix();
            final int pLength = prefix.length();
            considerStandardFolders = 0 == pLength ? 0 == parentFullName.length() : (parentFullName.equals(prefix.substring(0, pLength - 1)));
        }
        return considerStandardFolders;
    }

    private MailFolderInfo toFolderInfo(final ListLsubEntry entry) {
        final MailFolderInfo mfi = new MailFolderInfo();
        mfi.setAccountId(accountId);
        mfi.setSeparator(entry.getSeparator());
        final String fullName = entry.getFullName();

        mfi.setName(entry.getName());
        mfi.setHoldsFolders(entry.hasInferiors());
        mfi.setHoldsMessages(entry.canOpen());
        mfi.setSubscribed(true);
        mfi.setSubfolders(entry.hasChildren());
        mfi.setSubscribedSubfolders(entry.hasChildren());

        if (0 == fullName.length()) {
            mfi.setRootFolder(true);
            mfi.setParentFullname(null);
            mfi.setFullname(DEFAULT_FOLDER_ID);
        } else {
            mfi.setDefaultFolder(false);
            mfi.setDefaultFolderType(DefaultFolderType.NONE);
            mfi.setParentFullname(entry.getParent().getFullName());
            mfi.setFullname(fullName);
        }

        return mfi;
    }

    @Override
    public int[] getTotalAndUnreadCounter(final String fullName) throws OXException {
        if (DEFAULT_FOLDER_ID.equals(fullName)) {
            return new int[] { 0, 0 };
        }
        try {
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            final ListLsubEntry entry = ListLsubCache.getCachedLISTEntry(fullName, accountId, imapStore, session);
            {
                if (!doesExist(entry)) {
                    final IMAPFolder f = checkForNamespaceFolder(fullName);
                    if (null == f) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                    }
                    return new int[] { 0, 0 };
                }
                if (!entry.canOpen()) {
                    return new int[] { 0, 0 };
                }
                try {
                    return IMAPCommandsCollection.getTotalAndUnread(imapStore, fullName);
                } catch (final MessagingException e) {
                    return new int[] { 0, 0 };
                }
            }
        } catch (final MessagingException e) {
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, mapFor("fullName", fullName));
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public int getUnreadCounter(final String fullName) throws OXException {
        if (DEFAULT_FOLDER_ID.equals(fullName)) {
            return 0;
        }
        try {
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            final ListLsubEntry entry = ListLsubCache.getCachedLISTEntry(fullName, accountId, imapStore, session);
            {
                if (!doesExist(entry)) {
                    final IMAPFolder f = checkForNamespaceFolder(fullName);
                    if (null == f) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                    }
                    return 0;
                }
                if (!entry.canOpen()) {
                    return 0;
                }
                try {
                    return IMAPCommandsCollection.getTotalAndUnread(imapStore, fullName)[1];
                } catch (final MessagingException e) {
                    return 0;
                }
            }
        } catch (final MessagingException e) {
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, mapFor("fullName", fullName));
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public int getNewCounter(final String fullName) throws OXException {
        if (DEFAULT_FOLDER_ID.equals(fullName)) {
            return 0;
        }
        try {
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            final ListLsubEntry entry = ListLsubCache.getCachedLISTEntry(fullName, accountId, imapStore, session);
            {
                if (!doesExist(entry)) {
                    final IMAPFolder f = checkForNamespaceFolder(fullName);
                    if (null == f) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                    }
                    return 0;
                }
                if (!entry.canOpen()) {
                    return 0;
                }
                try {
                    return IMAPCommandsCollection.getRecent(imapStore, fullName);
                } catch (final MessagingException e) {
                    return 0;
                }
            }
        } catch (final MessagingException e) {
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, mapFor("fullName", fullName));
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public int getTotalCounter(final String fullName) throws OXException {
        if (DEFAULT_FOLDER_ID.equals(fullName)) {
            return 0;
        }
        try {
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            final ListLsubEntry entry = ListLsubCache.getCachedLISTEntry(fullName, accountId, imapStore, session);
            {
                if (!doesExist(entry)) {
                    final IMAPFolder f = checkForNamespaceFolder(fullName);
                    if (null == f) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                    }
                    return 0;
                }
                if (!entry.canOpen()) {
                    return 0;
                }
                try {
                    return IMAPCommandsCollection.getTotalAndUnread(imapStore, fullName)[0];
                } catch (final MessagingException e) {
                    return 0;
                }
            }
        } catch (final MessagingException e) {
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, mapFor("fullName", fullName));
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public boolean exists(final String fullName) throws OXException {
        try {
            if (DEFAULT_FOLDER_ID.equals(fullName) || STR_INBOX.equals(fullName)) {
                return true;
            }
            if (ListLsubCache.getCachedLISTEntry(fullName, accountId, imapStore, session).exists()) {
                return true;
            }
            if (checkForNamespaceFolder(fullName) != null) {
                return true;
            }
            // The hard way...
            final boolean exists = imapStore.getFolder(fullName).exists();
            if (exists) {
                // IMAP does signal folder existence, but not reflected in caches
                FolderCache.removeCachedFolders(session, accountId);
                ListLsubCache.clearCache(accountId, session);
                return true;
            }
            return false;
        } catch (final MessagingException e) {
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, mapFor("fullName", fullName));
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public MailFolder getFolder(final String fullName) throws OXException {
        return FolderCache.getCachedFolder(fullName, this);
    }

    // private static final String PATTERN_ALL = "%";

    @Override
    public MailFolder[] getSubfolders(final String parentFullName, final boolean all) throws OXException {
        try {
            IMAPFolderWorker.checkFailFast(imapStore, parentFullName);
            if (DEFAULT_FOLDER_ID.equals(parentFullName)) {
                final IMAPFolder parent = (IMAPFolder) imapStore.getDefaultFolder();
                final boolean subscribed = (!MailProperties.getInstance().isIgnoreSubscription() && !all);
                /*
                 * Request subfolders the usual way
                 */
                final List<ListLsubEntry> subfolders;
                /*
                 * Obtain folder lock once to avoid multiple acquire/releases when invoking folder's getXXX() methods
                 */
                final List<String> additionalFullNames = new ArrayList<String>(4);
                synchronized (parent) {
                    subfolders = new ArrayList<ListLsubEntry>();
                    {
                        final List<ListLsubEntry> children;
                        if (subscribed) {
                            children = getLSUBEntry("", parent).getChildren();
                        } else {
                            children = getLISTEntry("", parent).getChildren();
                        }
                        subfolders.addAll(children);
                        boolean containsInbox = false;
                        for (int i = 0; i < children.size() && !containsInbox; i++) {
                            containsInbox = STR_INBOX.equals(children.get(i).getFullName());
                        }
                        if (!containsInbox) {
                            /*
                             * Add folder INBOX manually
                             */
                            subfolders.add(0, getLISTEntry(STR_INBOX, parent));
                        }
                    }
                    if (imapConfig.getImapCapabilities().hasNamespace()) {
                        /*
                         * Merge with namespace folders
                         */
                        {
                            final String[] personalNamespaces =
                                NamespaceFoldersCache.getPersonalNamespaces(imapStore, true, session, accountId);
                            if (null == personalNamespaces || 1 != personalNamespaces.length || !STR_INBOX.equals(personalNamespaces[0])) {
                                /*
                                 * Personal namespace(s) does not only consist of INBOX folder
                                 */
                                mergeWithNamespaceFolders(subfolders, personalNamespaces, subscribed, parent, additionalFullNames);
                            }
                        }
                        {
                            mergeWithNamespaceFolders(
                                subfolders,
                                NamespaceFoldersCache.getUserNamespaces(imapStore, true, session, accountId),
                                subscribed,
                                parent,
                                additionalFullNames);
                        }
                        {
                            mergeWithNamespaceFolders(
                                subfolders,
                                NamespaceFoldersCache.getSharedNamespaces(imapStore, true, session, accountId),
                                subscribed,
                                parent,
                                additionalFullNames);
                        }
                    }
                }
                /*
                 * Output subfolders
                 */
                final List<MailFolder> list =
                    new ArrayList<MailFolder>(subfolders.size() + (additionalFullNames.isEmpty() ? 0 : additionalFullNames.size()));
                for (final ListLsubEntry subfolder : subfolders) {
                    final String subfolderFullName = subfolder.getFullName();
                    try {
                        list.add(FolderCache.getCachedFolder(subfolderFullName, this));
                    } catch (final OXException e) {
                        if (MimeMailExceptionCode.FOLDER_NOT_FOUND.getNumber() != e.getCode()) {
                            throw e;
                        }
                        /*
                         * Obviously folder does (no more) exist
                         */
                        FolderCache.removeCachedFolder(subfolderFullName, session, accountId);
                        ListLsubCache.removeCachedEntry(subfolderFullName, accountId, session);
                        RightsCache.removeCachedRights(subfolderFullName, session, accountId);
                        UserFlagsCache.removeUserFlags(subfolderFullName, session, accountId);
                    }
                }
                if (!additionalFullNames.isEmpty()) {
                    for (final String fn : additionalFullNames) {
                        final MailFolder namespaceFolder = namespaceFolderFor(fn, parent, subscribed);
                        if (null != namespaceFolder) {
                            list.add(namespaceFolder);
                        }
                    }
                }
                return list.toArray(new MailFolder[list.size()]);
            }
            IMAPFolder parent = getIMAPFolder(parentFullName);
            final ListLsubEntry parentEntry = getLISTEntry(parentFullName, parent);
            /*
             * Obtain folder lock once to avoid multiple acquire/releases when invoking folder's getXXX() methods
             */
            if (doesExist(parentEntry)) {
                /*
                 * Holds LOOK-UP right?
                 */
                if (imapConfig.isSupportsACLs() && parentEntry.canOpen()) {
                    try {
                        if (!imapConfig.getACLExtension().canLookUp(RightsCache.getCachedRights(parent, true, session, accountId))) {
                            throw IMAPException.create(IMAPException.Code.NO_LOOKUP_ACCESS, imapConfig, session, parentFullName);
                        }
                    } catch (final MessagingException e) {
                        if (!startsWithNamespaceFolder(parentFullName, parentEntry.getSeparator())) {
                            throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, parentFullName);
                        }
                    }
                }
                return getSubfolderArray(all, parent);
            }
            /*
             * Check for namespace folder
             */
            parent = checkForNamespaceFolder(parentFullName);
            if (null != parent) {
                return getSubfolderArray(all, parent);
            }
            return EMPTY_PATH;
        } catch (final MessagingException e) {
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, mapFor("fullName", parentFullName));
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private MailFolder namespaceFolderFor(final String fn, final IMAPFolder parent, final boolean subscribed) throws OXException, MessagingException {
        final ListLsubEntry listEntry = getLISTEntry(fn, parent);
        if (null == listEntry || !listEntry.exists() || (subscribed ? !listEntry.isSubscribed() : false)) {
            return null;
        }
        final IMAPMailFolder mailFolder = new IMAPMailFolder();
        mailFolder.setRootFolder(false);
        mailFolder.setExists(true);
        mailFolder.setSeparator(listEntry.getSeparator());
        mailFolder.setFullname(fn);
        mailFolder.setName(listEntry.getName());
        mailFolder.setHoldsMessages(listEntry.canOpen());
        mailFolder.setHoldsFolders(listEntry.hasInferiors());
        mailFolder.setNonExistent(false);
        mailFolder.setShared(true);
        mailFolder.setSubfolders(listEntry.hasChildren());
        mailFolder.setSubscribed(listEntry.isSubscribed());
        mailFolder.setSubscribedSubfolders(ListLsubCache.hasAnySubscribedSubfolder(fn, accountId, parent, session));
        mailFolder.setParentFullname(null);
        mailFolder.setDefaultFolder(false);
        mailFolder.setDefaultFolderType(DefaultFolderType.NONE);
        {
            final DefaultMailPermission perm = new DefaultMailPermission();
            perm.setAllPermission(0,0,0,0);
            perm.setFolderAdmin(false);
            perm.setEntity(session.getUserId());
            perm.setGroupPermission(false);
            mailFolder.setOwnPermission(perm);
            mailFolder.addPermission(perm);
        }
        mailFolder.setMessageCount(-1);
        mailFolder.setNewMessageCount(-1);
        mailFolder.setUnreadMessageCount(-1);
        mailFolder.setDeletedMessageCount(-1);
        mailFolder.setSupportsUserFlags(false);
        return mailFolder;
    }

    private MailFolder[] getSubfolderArray(final boolean all, final IMAPFolder parent) throws MessagingException, OXException {
        final boolean subscribed = !MailProperties.getInstance().isIgnoreSubscription() && !all;
        final List<ListLsubEntry> subfolders;
        {
            final ListLsubEntry entry = subscribed ? getLSUBEntry(parent) : getLISTEntry(parent);
            subfolders = new ArrayList<ListLsubEntry>(entry.getChildren());
        }
        /*
         * Merge with namespace folders if NAMESPACE capability is present
         */
        final List<String> additionalFullNames = new ArrayList<String>(4);
        if (imapConfig.getImapCapabilities().hasNamespace()) {

            mergeWithNamespaceFolders(subfolders, NamespaceFoldersCache.getPersonalNamespaces(imapStore, true, session, accountId), subscribed, parent, additionalFullNames);

            mergeWithNamespaceFolders(subfolders, NamespaceFoldersCache.getUserNamespaces(imapStore, true, session, accountId), subscribed, parent, additionalFullNames);

            mergeWithNamespaceFolders(subfolders, NamespaceFoldersCache.getSharedNamespaces(imapStore, true, session, accountId), subscribed, parent, additionalFullNames);

        }
        /*
         * Convert to MailFolder instances
         */
        final List<MailFolder> list = new ArrayList<MailFolder>(subfolders.size() + (additionalFullNames.isEmpty() ? 0 : additionalFullNames.size()));
        for (final ListLsubEntry current : subfolders) {
            final MailFolder mailFolder = FolderCache.getCachedFolder(current.getFullName(), this);
            if (mailFolder.exists()) {
                list.add(mailFolder);
            }
        }
        if (!additionalFullNames.isEmpty()) {
            for (final String fn : additionalFullNames) {
                list.add(FolderCache.getCachedFolder(fn, this));
            }
        }
        return list.toArray(new MailFolder[list.size()]);
    }

    private void mergeWithNamespaceFolders(final List<ListLsubEntry> subfolders, final String[] namespaces, final boolean subscribed, final IMAPFolder parent, final List<String> additionalFullNames) throws MessagingException, OXException {
        if (null == namespaces || namespaces.length == 0) {
            return;
        }
        final String[] namespaceFolders = new String[namespaces.length];
        System.arraycopy(namespaces, 0, namespaceFolders, 0, namespaces.length);
        final char sep = getSeparator(parent);
        final String parentFullname = parent.getFullName();
        final boolean isRoot = (0 == parentFullname.length());
        NextNSFolder: for (int i = 0; i < namespaceFolders.length; i++) {
            final String nsFullname = namespaceFolders[i];
            if ((nsFullname == null) || (nsFullname.length() == 0)) {
                namespaceFolders[i] = null;
                continue NextNSFolder;
            }
            /*
             * Check if namespace folder's prefix matches parent full name ; e.g "INBOX" or "INBOX/#shared"
             */
            final int pos = nsFullname.lastIndexOf(sep);
            if (pos > 0) { // Located below other folder than root
                if (!nsFullname.substring(0, pos).equals(parentFullname)) {
                    namespaceFolders[i] = null;
                    continue NextNSFolder;
                }
            } else if (!isRoot) { // Should be located below root
                namespaceFolders[i] = null;
                continue NextNSFolder;
            }
            /*
             * Check if already contained in passed list
             */
            for (final ListLsubEntry subfolder : subfolders) {
                if (nsFullname.equals(subfolder.getFullName())) {
                    /*
                     * Namespace folder already contained in subfolder list
                     */
                    namespaceFolders[i] = null;
                    continue NextNSFolder;
                }
            }
        }
        if (subscribed) {
            /*
             * Remove not-subscribed namespace folders
             */
            for (int i = 0; i < namespaceFolders.length; i++) {
                final String nsFullname = namespaceFolders[i];
                if (nsFullname != null && !IMAPCommandsCollection.isSubscribed(nsFullname, sep, true, parent)) {
                    namespaceFolders[i] = null;
                }
            }
        }
        /*
         * Add remaining namespace folders to subfolder list
         */
        for (final String fullName : namespaceFolders) {
            if (fullName != null) {
                additionalFullNames.add(fullName);
                // subfolders.add(new NamespaceFolder(imapStore, fullName, sep));
            }
        }
    }

    /**
     * Checks if given full name matches a namespace folder
     *
     * @param fullName The folder's full name
     * @return The corresponding namespace folder or <code>null</code>
     * @throws MessagingException
     */
    public IMAPFolder checkForNamespaceFolder(final String fullName) throws MessagingException {
        if (NamespaceFoldersCache.containedInPersonalNamespaces(fullName, imapStore, true, session, accountId)) {
            return new NamespaceFolder(imapStore, fullName, getSeparator());
        }
        if (NamespaceFoldersCache.containedInUserNamespaces(fullName, imapStore, true, session, accountId)) {
            return new NamespaceFolder(imapStore, fullName, getSeparator());
        }
        if (NamespaceFoldersCache.containedInSharedNamespaces(fullName, imapStore, true, session, accountId)) {
            return new NamespaceFolder(imapStore, fullName, getSeparator());
        }
        return null;
    }

    @Override
    public MailFolder getRootFolder() throws OXException {
        return FolderCache.getCachedFolder(MailFolder.DEFAULT_FOLDER_ID, this);
    }

    @Override
    public void checkDefaultFolders() throws OXException {
        getChecker().checkDefaultFolders();
    }

    private static final int FOLDER_TYPE = (Folder.HOLDS_MESSAGES | Folder.HOLDS_FOLDERS);

    @Override
    public String createFolder(final MailFolderDescription toCreate) throws OXException {
        final String name = toCreate.getName();
        if (isEmpty(name)) {
            throw MailExceptionCode.INVALID_FOLDER_NAME_EMPTY.create();
        }
        if (name.length() > maxMailboxNameLength()) {
            throw MailExceptionCode.INVALID_FOLDER_NAME_TOO_LONG.create(Integer.valueOf(maxMailboxNameLength()));
        }
        try {
            final String parentFullname = toCreate.getParentFullname();
            final String fullName = DEFAULT_FOLDER_ID.equals(parentFullname) ? name : new com.openexchange.java.StringAllocator(parentFullname).append(toCreate.getSeparator()).append(name).toString();
            if (getIMAPFolder(fullName).exists()) {
                throw IMAPException.create(IMAPException.Code.DUPLICATE_FOLDER, imapConfig, session, fullName);
            }
        } catch (final MessagingException e) {
            // Ignore for now
        }
        boolean created = false;
        IMAPFolder createMe = null;
        try {
            /*
             * Insert
             */
            String parentFullname = toCreate.getParentFullname();
            FolderCache.removeCachedFolder(parentFullname, session, accountId);
            final boolean isParentDefault;
            IMAPFolder parent;
            if (DEFAULT_FOLDER_ID.equals(parentFullname)) {
                parent = (IMAPFolder) imapStore.getDefaultFolder();
                parentFullname = "";
                isParentDefault = true;
            } else {
                if (toCreate.containsSeparator() && !checkFolderPathValidity(parentFullname, toCreate.getSeparator())) {
                    throw IMAPException.create(IMAPException.Code.INVALID_FOLDER_NAME, imapConfig, session, toCreate.getName(), invalidCharsString(toCreate.getSeparator()));
                }
                parent = getIMAPFolder(parentFullname);
                isParentDefault = false;
            }
            /*
             * Obtain folder lock once to avoid multiple acquire/releases when invoking folder's getXXX() methods
             */
            synchronized (parent) {
                if (!parent.exists()) {
                    parent = checkForNamespaceFolder(parentFullname);
                    if (null == parent) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, parentFullname);
                    }
                }
                /*
                 * Check if parent holds folders
                 */
                if ((parent.getType() & Folder.HOLDS_FOLDERS) == 0) {
                    throw IMAPException.create(
                        IMAPException.Code.FOLDER_DOES_NOT_HOLD_FOLDERS,
                        imapConfig,
                        session,
                        isParentDefault ? DEFAULT_FOLDER_ID : parentFullname);
                }
                /*
                 * Check ACLs if enabled
                 */
                if (imapConfig.isSupportsACLs()) {
                    try {
                        if (isParentDefault) {
                            if (!(RootSubfolderCache.canCreateSubfolders((DefaultFolder) parent, true, session, accountId).booleanValue())) {
                                throw IMAPException.create(IMAPException.Code.NO_CREATE_ACCESS, imapConfig, session, DEFAULT_FOLDER_ID);
                            }
                        } else {
                            if (!imapConfig.getACLExtension().canCreate(RightsCache.getCachedRights(parent, true, session, accountId))) {
                                throw IMAPException.create(IMAPException.Code.NO_CREATE_ACCESS, imapConfig, session, parentFullname);
                            }
                        }
                    } catch (final MessagingException e) {
                        /*
                         * MYRIGHTS command failed for given mailbox
                         */
                        if (!imapConfig.getImapCapabilities().hasNamespace() || !NamespaceFoldersCache.containedInPersonalNamespaces(
                            parentFullname,
                            imapStore,
                            true,
                            session,
                            accountId)) {
                            /*
                             * No namespace support or given parent is NOT covered by user's personal namespaces.
                             */
                            throw IMAPException.create(
                                IMAPException.Code.NO_ACCESS,
                                imapConfig,
                                session,
                                e,
                                isParentDefault ? DEFAULT_FOLDER_ID : parentFullname);
                        }
                        LOG.debug("MYRIGHTS command failed on namespace folder", e);
                    }
                }
                /*
                 * Check if IMAP server is in MBox format; meaning folder either hold messages or subfolders but not both
                 */
                final char separator = getSeparator();
                final boolean mboxEnabled =
                    MBoxEnabledCache.isMBoxEnabled(imapConfig, parent, new com.openexchange.java.StringAllocator(parent.getFullName()).append(separator).toString());
                if (!checkFolderNameValidity(name, separator, mboxEnabled)) {
                    throw IMAPException.create(IMAPException.Code.INVALID_FOLDER_NAME, imapConfig, session, name, invalidCharsString(separator));
                }
                if (isParentDefault) {
                    /*
                     * Below default folder
                     */
                    createMe = getIMAPFolder(name);
                } else {
                    createMe =
                        (IMAPFolder) imapStore.getFolder(new com.openexchange.java.StringAllocator(parent.getFullName()).append(separator).append(name).toString());
                }
                /*
                 * Obtain folder lock once to avoid multiple acquire/releases when invoking folder's getXXX() methods
                 */
                synchronized (createMe) {
                    if (createMe.exists()) {
                        throw IMAPException.create(IMAPException.Code.DUPLICATE_FOLDER, imapConfig, session, createMe.getFullName());
                    }
                    final int ftype = mboxEnabled ? getNameOf(createMe).endsWith(String.valueOf(separator)) ? Folder.HOLDS_FOLDERS : Folder.HOLDS_MESSAGES : FOLDER_TYPE;
                    try {
                        if (!(created = createMe.create(ftype))) {
                            throw IMAPException.create(IMAPException.Code.FOLDER_CREATION_FAILED, imapConfig, session, createMe.getFullName(), isParentDefault ? DEFAULT_FOLDER_ID : parent.getFullName());
                        }
                    } catch (final MessagingException e) {
                        if (!"Unsupported type".equals(e.getMessage())) {
                            if (e.getNextException() instanceof com.sun.mail.iap.BadCommandException) {
                                // Bad input for associated IMAP server
                                throw IMAPException.create(IMAPException.Code.FOLDER_CREATION_FAILED, imapConfig, session, e, createMe.getFullName(), isParentDefault ? DEFAULT_FOLDER_ID : parent.getFullName());
                            }

                            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, mapFor("fullName", createMe.getFullName()));
                        }
                        LOG.warn("IMAP folder creation failed due to unsupported type. Going to retry with fallback type HOLDS-MESSAGES.", e);
                        if (!(created = createMe.create(Folder.HOLDS_MESSAGES))) {
                            throw IMAPException.create(IMAPException.Code.FOLDER_CREATION_FAILED, imapConfig, session, e, createMe.getFullName(), isParentDefault ? DEFAULT_FOLDER_ID : parent.getFullName());
                        }
                        LOG.info("IMAP folder created with fallback type HOLDS_MESSAGES");
                    }
                    /*
                     * Subscribe
                     */
                    createMe.open(Folder.READ_WRITE);
                    if (!MailProperties.getInstance().isSupportSubscription()) {
                        IMAPCommandsCollection.forceSetSubscribed(imapStore, createMe.getFullName(), true);
                    } else if (toCreate.containsSubscribed()) {
                        IMAPCommandsCollection.forceSetSubscribed(imapStore, createMe.getFullName(), toCreate.isSubscribed());
                    } else {
                        IMAPCommandsCollection.forceSetSubscribed(imapStore, createMe.getFullName(), true);
                    }
                    /*
                     * Apply ACLs if supported by IMAP server
                     */
                    if (imapConfig.isSupportsACLs() && toCreate.containsPermissions()) {
                        final ACL[] initialACLs = getACLSafe(createMe);
                        if (initialACLs != null) {
                            final ACL[] newACLs = permissions2ACL(toCreate.getPermissions(), createMe);
                            final Entity2ACL entity2ACL = getEntity2ACL();
                            final Entity2ACLArgs args = IMAPFolderConverter.getEntity2AclArgs(session, createMe, imapConfig);
                            final Map<String, ACL> m = acl2map(newACLs);
                            if (!equals(initialACLs, m, entity2ACL, args)) {
                                final ACLExtension aclExtension = imapConfig.getACLExtension();
                                if (aclExtension.canSetACL(createMe.myRights())) {
                                    boolean adminFound = false;
                                    for (int i = 0; (i < newACLs.length) && !adminFound; i++) {
                                        if (aclExtension.canSetACL(newACLs[i].getRights())) {
                                            adminFound = true;
                                        }
                                    }
                                    if (!adminFound) {
                                        throw IMAPException.create(IMAPException.Code.NO_ADMIN_ACL, imapConfig, session, createMe.getFullName());
                                    }
                                    /*
                                     * Apply new ACLs
                                     */
                                    final Map<String, ACL> om = acl2map(initialACLs);
                                    for (int i = 0; i < newACLs.length; i++) {
                                        createMe.addACL(validate(newACLs[i], om));
                                    }
                                    /*
                                     * Remove other ACLs
                                     */
                                    final ACL[] removedACLs = getRemovedACLs(m, initialACLs);
                                    if (removedACLs.length > 0) {
                                        for (int i = 0; i < removedACLs.length; i++) {
                                            if (isKnownEntity(removedACLs[i].getName(), entity2ACL, ctx, args)) {
                                                createMe.removeACL(removedACLs[i].getName());
                                            }
                                        }
                                    }
                                } else {
                                    /*
                                     * Add a warning
                                     */
                                    imapAccess.addWarnings(Collections.<OXException> singletonList(IMAPException.create(IMAPException.Code.NO_ADMINISTER_ACCESS_ON_INITIAL, imapConfig, session, createMe.getFullName())));
                                }
                            }
                        }
                    }
                    return createMe.getFullName();
                }
            }
        } catch (final MessagingException e) {
            if (createMe != null && created) {
                try {
                    if (doesExist(createMe, false)) {
                        createMe.delete(true);
                        created = false;
                    }
                } catch (final Throwable e2) {
                    LOG.error("Temporary created IMAP folder \"{}could not be deleted", createMe.getFullName(),
                        e2);
                }
            }
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, null);
        } catch (final OXException e) {
            /*
             * No folder deletion on IMAP error "NO_ADMINISTER_ACCESS_ON_INITIAL"
             */
            if (e.isPrefix("MSG") && IMAPException.Code.NO_ADMINISTER_ACCESS_ON_INITIAL.getNumber() != e.getCode()) {
                if (createMe != null && created) {
                    try {
                        if (doesExist(createMe, false)) {
                            createMe.delete(true);
                            created = false;
                        }
                    } catch (final Throwable e2) {
                        LOG.error("Temporary created IMAP folder \"{}\" could not be deleted", createMe.getFullName(),
                            e2);
                    }
                }
            }
            throw e;
        } catch (final RuntimeException e) {
            if (createMe != null && created) {
                try {
                    if (doesExist(createMe, false)) {
                        createMe.delete(true);
                        created = false;
                    }
                } catch (final Throwable e2) {
                    LOG.error("Temporary created IMAP folder \"{}\" could not be deleted", createMe.getFullName(),
                        e2);
                }
            }
            throw handleRuntimeException(e);
        } catch (final Exception e) {
            if (createMe != null && created) {
                try {
                    if (doesExist(createMe, false)) {
                        createMe.delete(true);
                        created = false;
                    }
                } catch (final Throwable e2) {
                    LOG.error("Temporary created IMAP folder \"{}\" could not be deleted", createMe.getFullName(),
                        e2);
                }
            }
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            if (createMe != null) {
                if (created) {
                    try {
                        final Folder parent = createMe.getParent();
                        if (null != parent) {
                            final String parentFullName = parent.getFullName();
                            ListLsubCache.addSingle(parentFullName, accountId, createMe, session);
                            if ("".equals(parentFullName)) {
                                ListLsubCache.addSingle(MailFolder.DEFAULT_FOLDER_ID, accountId, createMe, session);
                            }
                            ListLsubCache.addSingle(createMe.getFullName(), accountId, createMe, session);
                        } else {
                            ListLsubCache.clearCache(accountId, session);
                        }
                    } catch (final MessagingException e) {
                        // Updating LIST/LSUB cache failed
                        ListLsubCache.clearCache(accountId, session);
                    } finally {
                        closeSafe(createMe);
                    }
                } else {
                    closeSafe(createMe);
                }
            }
        }
    }

    private Entity2ACL getEntity2ACL() throws OXException {
        return Entity2ACL.getInstance(imapStore, imapConfig);
    }

    @Override
    public String renameFolder(final String fullName, final String newName) throws OXException {
        if (DEFAULT_FOLDER_ID.equals(fullName)) {
            final String name;
            if (accountId == MailAccount.DEFAULT_ID) {
                name = MailFolder.DEFAULT_FOLDER_NAME;
            } else {
                final MailAccountStorageService mass = Services.getService(MailAccountStorageService.class);
                if (null == mass) {
                    name = MailFolder.DEFAULT_FOLDER_NAME;
                } else {
                    name = mass.getMailAccount(accountId, session.getUserId(), session.getContextId()).getName();
                }

            }
            throw IMAPException.create(IMAPException.Code.NO_RENAME_ACCESS, imapConfig, session, name);
        }
        try {
            IMAPFolder renameMe = getIMAPFolder(fullName);
            if (!doesExist(renameMe, false)) {
                renameMe = checkForNamespaceFolder(fullName);
                if (null == renameMe) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
            }
            FolderCache.removeCachedFolders(session, accountId);
            /*
             * Obtain folder lock once to avoid multiple acquire/releases when invoking folder's getXXX() methods
             */
            synchronized (renameMe) {
                if (imapConfig.isSupportsACLs() && ((renameMe.getType() & Folder.HOLDS_MESSAGES) > 0)) {
                    try {
                        if (!imapConfig.getACLExtension().canCreate(RightsCache.getCachedRights(renameMe, true, session, accountId))) {
                            throw IMAPException.create(IMAPException.Code.NO_RENAME_ACCESS, imapConfig, session, fullName);
                        }
                    } catch (final MessagingException e) {
                        /*
                         * MYRIGHTS command failed for given mailbox
                         */
                        throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, fullName);
                    }
                }
                if (getChecker().isDefaultFolder(fullName)) {
                    throw IMAPException.create(IMAPException.Code.NO_DEFAULT_FOLDER_UPDATE, imapConfig, session, renameMe.getFullName());
                }
                /*
                 * Notify message storage about outstanding rename
                 */
                imapAccess.getMessageStorage().notifyIMAPFolderModification(fullName);
                final char separator = renameMe.getSeparator();
                if (isEmpty(newName)) {
                    throw MailExceptionCode.INVALID_FOLDER_NAME_EMPTY.create();
                } else if (newName.indexOf(separator) != -1) {
                    throw MailExceptionCode.INVALID_FOLDER_NAME2.create(newName, Character.toString(separator));
                } else if (newName.length() > maxMailboxNameLength()) {
                    throw MailExceptionCode.INVALID_FOLDER_NAME_TOO_LONG.create(Integer.valueOf(maxMailboxNameLength()));
                }
                /*-
                 * Perform rename operation
                 *
                 * Rename can only be invoked on a closed folder
                 */
                if (renameMe.isOpen()) {
                    renameMe.close(false);
                }
                final boolean mboxEnabled;
                final IMAPFolder renameFolder;
                {
                    final IMAPFolder par = (IMAPFolder) renameMe.getParent();
                    final String parentFullName = par.getFullName();
                    final com.openexchange.java.StringAllocator tmp = new com.openexchange.java.StringAllocator();
                    if (parentFullName.length() > 0) {
                        tmp.append(parentFullName).append(separator);
                    }
                    tmp.append(newName);
                    renameFolder = (IMAPFolder) imapStore.getFolder(tmp.toString());
                    /*
                     * Check for MBox
                     */
                    mboxEnabled =
                        MBoxEnabledCache.isMBoxEnabled(imapConfig, par, new com.openexchange.java.StringAllocator(par.getFullName()).append(separator).toString());
                }
                if (doesExist(renameFolder, false)) {
                    throw IMAPException.create(IMAPException.Code.DUPLICATE_FOLDER, imapConfig, session, renameFolder.getFullName());
                }
                if (!checkFolderNameValidity(newName, separator, mboxEnabled)) {
                    throw IMAPException.create(IMAPException.Code.INVALID_FOLDER_NAME, imapConfig, session, newName, invalidCharsString(separator));
                }
                /*
                 * Remember subscription status
                 */
                Map<String, Boolean> subscriptionStatus;
                final String newFullName = renameFolder.getFullName();
                final String oldFullName = renameMe.getFullName();
                try {
                    subscriptionStatus = getSubscriptionStatus(renameMe, oldFullName, newFullName);
                } catch (final MessagingException e) {
                    LOG.warn("Subscription status of folder \"{}\" and its subfolders could not be stored prior to rename operation", renameMe.getFullName());
                    subscriptionStatus = null;
                }
                removeSessionData(renameMe);
                /*
                 * Unsubscribe sub-tree
                 */
                setFolderSubscription(renameMe, false);
                /*
                 * Rename
                 */
                boolean success = false;
                try {
                    if (renameMe.isOpen()) {
                        renameMe.close(false);
                    } else {
                        // Enforce close
                        IMAPCommandsCollection.forceCloseCommand(renameMe);
                    }
                    final long start = System.currentTimeMillis();
                    IMAPCommandsCollection.renameFolder(renameMe, renameFolder);
                    success = true;
                    // success = moveMe.renameTo(renameFolder);
                    mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
                } catch (final MessagingException e) {
                    /*
                     * Rename failed
                     */
                    throw IMAPException.create(
                        IMAPException.Code.RENAME_FAILED,
                        imapConfig,
                        session,
                        e,
                        renameMe.getFullName(),
                        newFullName,
                        e.getMessage());
                } finally {
                    if (!success) {
                        setFolderSubscription(renameMe, true);
                    }
                }
                /*
                 * Success?
                 */
                if (!success) {
                    throw IMAPException.create(IMAPException.Code.RENAME_FAILED, imapConfig, session, renameMe.getFullName(), newFullName, "<not-available>");
                }
                renameMe = getIMAPFolder(oldFullName);
                if (renameMe.exists()) {
                    deleteFolder(renameMe);
                }
                renameMe = getIMAPFolder(newFullName);
                /*
                 * Apply remembered subscription status
                 */
                if (subscriptionStatus == null) {
                    /*
                     * At least subscribe to renamed folder
                     */
                    renameMe.setSubscribed(true);
                } else {
                    applySubscriptionStatus(renameMe, subscriptionStatus);
                }
                /*
                 * Return new full name
                 */
                return renameMe.getFullName();
            }
        } catch (final MessagingException e) {
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, mapFor("fullName", fullName));
        } catch (final IMAPException e) {
            throw e;
        } catch (final OXException e) {
            throw e;
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        } finally {
            ListLsubCache.clearCache(accountId, session);
        }
    }

    @Override
    public String moveFolder(final String fullName, final String newFullname) throws OXException {
        if (DEFAULT_FOLDER_ID.equals(fullName) || DEFAULT_FOLDER_ID.equals(newFullname)) {
            throw IMAPException.create(IMAPException.Code.NO_ROOT_MOVE, imapConfig, session, new Object[0]);
        }
        try {
            if (DEFAULT_FOLDER_ID.equals(fullName)) {
                throw MailExceptionCode.NO_ROOT_FOLDER_MODIFY_DELETE.create();
            }
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            IMAPFolder moveMe = getIMAPFolder(fullName);
            if (!doesExist(moveMe, false)) {
                moveMe = checkForNamespaceFolder(fullName);
                if (null == moveMe) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
            }
            FolderCache.removeCachedFolders(session, accountId);
            ListLsubCache.clearCache(accountId, session);
            /*
             * Obtain folder lock once to avoid multiple acquire/releases when invoking folder's getXXX() methods
             */
            synchronized (moveMe) {
                /*
                 * Notify message storage about outstanding move
                 */
                imapAccess.getMessageStorage().notifyIMAPFolderModification(fullName);
                final char separator = getSeparator(moveMe);
                final String oldParent = moveMe.getParent().getFullName();
                final String newParent;
                final String newName;
                {
                    final int pos = newFullname.lastIndexOf(separator);
                    if (pos == -1) {
                        newParent = "";
                        newName = newFullname;
                    } else {
                        if (pos == newFullname.length() - 1) {
                            throw IMAPException.create(IMAPException.Code.INVALID_FOLDER_NAME, imapConfig, session, newFullname, invalidCharsString(separator));
                        }
                        newParent = newFullname.substring(0, pos);
                        newName = newFullname.substring(pos + 1);
                        if (!checkFolderPathValidity(newParent, separator)) {
                            throw IMAPException.create(IMAPException.Code.INVALID_FOLDER_NAME, imapConfig, session, newName, invalidCharsString(separator));
                        }
                    }
                }
                if (newName.length() > maxMailboxNameLength()) {
                    throw MailExceptionCode.INVALID_FOLDER_NAME_TOO_LONG.create(Integer.valueOf(maxMailboxNameLength()));
                }
                /*
                 * Check for move
                 */
                final boolean move = !newParent.equals(oldParent);
                /*
                 * Check for rename. Rename must not be performed if a move has already been done
                 */
                final boolean rename = (!move && !newName.equals(getNameOf(moveMe)));
                if (move) {
                    /*
                     * Perform move operation
                     */
                    final String oldFullname = moveMe.getFullName();
                    if (getChecker().isDefaultFolder(oldFullname)) {
                        throw IMAPException.create(IMAPException.Code.NO_DEFAULT_FOLDER_UPDATE, imapConfig, session, oldFullname);
                    }
                    IMAPFolder destFolder;
                    final boolean isDestRoot;
                    if ("".equals(newParent)) {
                        destFolder = (IMAPFolder) imapStore.getDefaultFolder();
                        isDestRoot = true;
                    } else {
                        destFolder = getIMAPFolder(newParent);
                        isDestRoot = false;
                    }
                    if (!doesExist(destFolder, false)) {
                        destFolder = checkForNamespaceFolder(newParent);
                        if (null == destFolder) {
                            /*
                             * Destination folder could not be found, thus an invalid name was specified by user
                             */
                            throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, isDestRoot ? DEFAULT_FOLDER_ID : newParent);
                        }
                    }
                    synchronized (destFolder) {
                        if ((destFolder.getType() & Folder.HOLDS_FOLDERS) == 0) {
                            throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_FOLDERS, imapConfig, session, isDestRoot ? DEFAULT_FOLDER_ID : destFolder.getFullName());
                        }
                        if (imapConfig.isSupportsACLs() && ((destFolder.getType() & Folder.HOLDS_MESSAGES) > 0)) {
                            try {
                                if (isDestRoot) {
                                    if (!(RootSubfolderCache.canCreateSubfolders((DefaultFolder) destFolder, true, session, accountId).booleanValue())) {
                                        throw IMAPException.create(IMAPException.Code.NO_CREATE_ACCESS, imapConfig, session, DEFAULT_FOLDER_ID);
                                    }
                                } else {
                                    if (!imapConfig.getACLExtension().canCreate(RightsCache.getCachedRights(destFolder, true, session, accountId))) {
                                        throw IMAPException.create(IMAPException.Code.NO_CREATE_ACCESS, imapConfig, session, isDestRoot ? DEFAULT_FOLDER_ID : newParent);
                                    }
                                }
                            } catch (final MessagingException e) {
                                /*
                                 * MYRIGHTS command failed for given mailbox
                                 */
                                if (!imapConfig.getImapCapabilities().hasNamespace() || !NamespaceFoldersCache.containedInPersonalNamespaces(newParent, imapStore, true, session, accountId)) {
                                    /*
                                     * No namespace support or given parent is NOT covered by user's personal namespaces.
                                     */
                                    throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, isDestRoot ? DEFAULT_FOLDER_ID : newParent);
                                }
                                LOG.debug("MYRIGHTS command failed on namespace folder", e);
                            }
                        }
                        final boolean mboxEnabled = MBoxEnabledCache.isMBoxEnabled(imapConfig, destFolder, new com.openexchange.java.StringAllocator(destFolder.getFullName()).append(separator).toString());
                        if (!checkFolderNameValidity(newName, separator, mboxEnabled)) {
                            throw IMAPException.create(IMAPException.Code.INVALID_FOLDER_NAME, imapConfig, session, newName, invalidCharsString(separator));
                        }
                        if (isSubfolderOf(destFolder.getFullName(), oldFullname, separator)) {
                            throw IMAPException.create(IMAPException.Code.NO_MOVE_TO_SUBFLD, imapConfig, session, getNameOf(moveMe), getNameOf(destFolder));
                        }
                        moveMe = moveFolder(moveMe, destFolder, newName);
                    }
                }
                /*
                 * Is rename operation?
                 */
                if (rename) {
                    /*
                     * Perform rename operation
                     */
                    if (getChecker().isDefaultFolder(moveMe.getFullName())) {
                        throw IMAPException.create(IMAPException.Code.NO_DEFAULT_FOLDER_UPDATE, imapConfig, session, moveMe.getFullName());
                    } else if (imapConfig.isSupportsACLs() && ((moveMe.getType() & Folder.HOLDS_MESSAGES) > 0)) {
                        try {
                            if (!imapConfig.getACLExtension().canCreate(RightsCache.getCachedRights(moveMe, true, session, accountId))) {
                                throw IMAPException.create(IMAPException.Code.NO_CREATE_ACCESS, imapConfig, session, moveMe.getFullName());
                            }
                        } catch (final MessagingException e) {
                            throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, moveMe.getFullName());
                        }
                    }
                    /*
                     * Rename can only be invoked on a closed folder
                     */
                    if (moveMe.isOpen()) {
                        moveMe.close(false);
                    }
                    final boolean mboxEnabled;
                    final IMAPFolder renameFolder;
                    {
                        final IMAPFolder par = (IMAPFolder) moveMe.getParent();
                        final String parentFullName = par.getFullName();
                        final com.openexchange.java.StringAllocator tmp = new com.openexchange.java.StringAllocator();
                        if (parentFullName.length() > 0) {
                            tmp.append(parentFullName).append(separator);
                        }
                        tmp.append(newName);
                        renameFolder = (IMAPFolder) imapStore.getFolder(tmp.toString());
                        /*
                         * Check for MBox
                         */
                        mboxEnabled = MBoxEnabledCache.isMBoxEnabled(imapConfig, par, new com.openexchange.java.StringAllocator(par.getFullName()).append(separator).toString());
                    }
                    if (doesExist(renameFolder, false)) {
                        throw IMAPException.create(IMAPException.Code.DUPLICATE_FOLDER, imapConfig, session, renameFolder.getFullName());
                    }
                    if (!checkFolderNameValidity(newName, separator, mboxEnabled)) {
                        throw IMAPException.create(IMAPException.Code.INVALID_FOLDER_NAME, imapConfig, session, newName, invalidCharsString(separator));
                    }
                    /*
                     * Remember subscription status
                     */
                    Map<String, Boolean> subscriptionStatus;
                    final String newFullName = renameFolder.getFullName();
                    final String oldFullName = moveMe.getFullName();
                    try {
                        subscriptionStatus = getSubscriptionStatus(moveMe, oldFullName, newFullName);
                    } catch (final MessagingException e) {
                        LOG.warn("Subscription status of folder \"{}\" and its subfolders could not be stored prior to rename operation", moveMe.getFullName());
                        subscriptionStatus = null;
                    }
                    removeSessionData(moveMe);
                    /*
                     * Unsubscribe sub-tree
                     */
                    setFolderSubscription(moveMe, false);
                    /*
                     * Rename
                     */
                    boolean success = false;
                    try {
                        if (moveMe.isOpen()) {
                            moveMe.close(false);
                        } else {
                            // Enforce close
                            IMAPCommandsCollection.forceCloseCommand(moveMe);
                        }
                        final long start = System.currentTimeMillis();
                        IMAPCommandsCollection.renameFolder(moveMe, renameFolder);
                        success = true;
                        // success = moveMe.renameTo(renameFolder);
                        mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
                    } catch (final MessagingException e) {
                        /*
                         * Rename failed
                         */
                        throw IMAPException.create(IMAPException.Code.RENAME_FAILED, imapConfig, session, e, moveMe.getFullName(), newFullName, e.getMessage());

                    } finally {
                        if (!success) {
                            setFolderSubscription(moveMe, true);
                        }
                    }
                    /*
                     * Success?
                     */
                    if (!success) {
                        throw IMAPException.create(IMAPException.Code.RENAME_FAILED, imapConfig, session, moveMe.getFullName(), newFullName, "<not-available>");
                    }
                    moveMe = getIMAPFolder(oldFullName);
                    if (doesExist(moveMe, false)) {
                        deleteFolder(moveMe);
                    }
                    moveMe = getIMAPFolder(newFullName);
                    /*
                     * Apply remembered subscription status
                     */
                    if (subscriptionStatus == null) {
                        /*
                         * At least subscribe to renamed folder
                         */
                        moveMe.setSubscribed(true);
                    } else {
                        applySubscriptionStatus(moveMe, subscriptionStatus);
                    }
                }
                return moveMe.getFullName();
            }
        } catch (final MessagingException e) {
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, mapFor("fullName", fullName));
        } catch (final IMAPException e) {
            throw e;
        } catch (final OXException e) {
            throw e;
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        } finally {
            ListLsubCache.clearCache(accountId, session);
        }
    }

    @Override
    public String updateFolder(final String fullName, final MailFolderDescription toUpdate) throws OXException {
        boolean changed = false;
        try {
            if (DEFAULT_FOLDER_ID.equals(fullName)) {
                throw MailExceptionCode.NO_ROOT_FOLDER_MODIFY_DELETE.create();
            }
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            IMAPFolder updateMe = getIMAPFolder(fullName);
            if (!doesExist(updateMe, true)) {
                updateMe = checkForNamespaceFolder(fullName);
                if (null == updateMe) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
            }
            FolderCache.removeCachedFolder(fullName, session, accountId);
            /*
             * Obtain folder lock once to avoid multiple acquire/releases when invoking folder's getXXX() methods
             */
            synchronized (updateMe) {
                /*
                 * Check for standard folder & possible subscribe operation
                 */
                final IMAPDefaultFolderChecker checker = getChecker();
                boolean defaultFolder = false;
                if ("INBOX".equals(fullName)) {
                    defaultFolder = true;
                } else if (fullName.equals(checker.getDefaultFolder(StorageUtility.INDEX_TRASH)) ) {
                    defaultFolder = true;
                } else if (fullName.equals(checker.getDefaultFolder(StorageUtility.INDEX_DRAFTS)) ) {
                    defaultFolder = true;
                } else if (fullName.equals(checker.getDefaultFolder(StorageUtility.INDEX_SENT)) ) {
                    defaultFolder = true;
                } else if (fullName.equals(checker.getDefaultFolder(StorageUtility.INDEX_SPAM)) ) {
                    defaultFolder = true;
                }
                final boolean performSubscription = MailProperties.getInstance().isIgnoreSubscription() && defaultFolder ? false : performSubscribe(toUpdate, updateMe);
                if (performSubscription && defaultFolder && !toUpdate.isSubscribed()) {
                    throw IMAPException.create(IMAPException.Code.NO_DEFAULT_FOLDER_UNSUBSCRIBE, imapConfig, session, fullName);
                }
                /*
                 * Notify message storage
                 */
                imapAccess.getMessageStorage().notifyIMAPFolderModification(fullName);
                /*
                 * Proceed update
                 */
                if (imapConfig.isSupportsACLs() && toUpdate.containsPermissions()) {
                    final ACL[] oldACLs = getACLSafe(updateMe);
                    if (oldACLs != null) {
                        final ACL[] newACLs = permissions2ACL(toUpdate.getPermissions(), updateMe);
                        final Entity2ACL entity2ACL = getEntity2ACL();
                        final Entity2ACLArgs args = IMAPFolderConverter.getEntity2AclArgs(session, updateMe, imapConfig);
                        final Map<String, ACL> m = acl2map(newACLs);
                        if (!equals(oldACLs, m, entity2ACL, args)) {
                            /*
                             * Default folder is affected, check if owner still holds full rights
                             */
                            final ACLExtension aclExtension = imapConfig.getACLExtension();
                            if (getChecker().isDefaultFolder(updateMe.getFullName()) && !stillHoldsFullRights(
                                updateMe,
                                newACLs,
                                aclExtension)) {
                                throw IMAPException.create(
                                    IMAPException.Code.NO_DEFAULT_FOLDER_UPDATE,
                                    imapConfig,
                                    session,
                                    updateMe.getFullName());
                            }
                            if (!aclExtension.canSetACL(RightsCache.getCachedRights(updateMe, true, session, accountId))) {
                                throw IMAPException.create(
                                    IMAPException.Code.NO_ADMINISTER_ACCESS,
                                    imapConfig,
                                    session,
                                    updateMe.getFullName());
                            }
                            /*
                             * Check new ACLs
                             */
                            if (newACLs.length == 0) {
                                throw IMAPException.create(IMAPException.Code.NO_ADMIN_ACL, imapConfig, session, updateMe.getFullName());
                            }
                            {
                                boolean adminFound = false;
                                for (int i = 0; (i < newACLs.length) && !adminFound; i++) {
                                    if (aclExtension.canSetACL(newACLs[i].getRights())) {
                                        adminFound = true;
                                    }
                                }
                                if (!adminFound) {
                                    throw IMAPException.create(IMAPException.Code.NO_ADMIN_ACL, imapConfig, session, updateMe.getFullName());
                                }
                            }
                            /*
                             * Remove deleted ACLs
                             */
                            final ACL[] removedACLs = getRemovedACLs(m, oldACLs);
                            if (removedACLs.length > 0) {
                                for (int i = 0; i < removedACLs.length; i++) {
                                    if (isKnownEntity(removedACLs[i].getName(), entity2ACL, ctx, args)) {
                                        updateMe.removeACL(removedACLs[i].getName());
                                        changed = true;
                                    }
                                }
                            }
                            /*
                             * Change existing ACLs according to new ACLs
                             */
                            final Map<String, ACL> om = acl2map(oldACLs);
                            for (int i = 0; i < newACLs.length; i++) {
                                updateMe.addACL(validate(newACLs[i], om));
                                changed = true;
                            }
                            /*
                             * Since the ACLs have changed remove cached rights
                             */
                            FolderCache.removeCachedFolder(fullName, session, accountId);
                            RightsCache.removeCachedRights(updateMe, session, accountId);
                        }
                    }
                }
                if (/* !MailProperties.getInstance().isIgnoreSubscription() && */performSubscription) {
                    /*
                     * Check read permission
                     */
                    if (imapConfig.isSupportsACLs()) {
                        if ((updateMe.getType() & Folder.HOLDS_MESSAGES) > 0) {
                            try {
                                if (!imapConfig.getACLExtension().canLookUp(RightsCache.getCachedRights(updateMe, true, session, accountId))) {
                                    throw IMAPException.create(IMAPException.Code.NO_LOOKUP_ACCESS, imapConfig, session, fullName);
                                }
                            } catch (final MessagingException e) {
                                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, fullName);
                            }
                        } else {
                            throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, fullName);
                        }
                    }
                    final boolean subscribe = toUpdate.isSubscribed();
                    updateMe.setSubscribed(subscribe);
                    IMAPCommandsCollection.forceSetSubscribed(imapStore, updateMe.getFullName(), subscribe);
                    FolderCache.removeCachedFolders(session, accountId);
                    changed = true;
                }
                return updateMe.getFullName();
            }
        } catch (final MessagingException e) {
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, mapFor("fullName", fullName));
        } catch (final IMAPException e) {
            throw e;
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        } finally {
            if (changed) {
                ListLsubCache.clearCache(accountId, session);
            }
        }
    }

    private static boolean performSubscribe(final MailFolderDescription toUpdate, final IMAPFolder updateMe) {
        return toUpdate.containsSubscribed() && (toUpdate.isSubscribed() != updateMe.isSubscribed());
    }

    private void deleteTemporaryCreatedFolder(final IMAPFolder temporaryFolder) throws OXException, MessagingException {
        if (doesExist(temporaryFolder, false)) {
            try {
                temporaryFolder.delete(true);
            } catch (final MessagingException e1) {
                LOG.error("Temporary created folder could not be deleted: {}", temporaryFolder.getFullName(), e1);
            }
        }
    }

    @Override
    public String deleteFolder(final String fullName, final boolean hardDelete) throws OXException {
        boolean clearListLsubCache = false;
        try {
            if (DEFAULT_FOLDER_ID.equals(fullName)) {
                throw MailExceptionCode.NO_ROOT_FOLDER_MODIFY_DELETE.create();
            }
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            IMAPFolder deleteMe = getIMAPFolder(fullName);
            if (!doesExist(deleteMe, false)) {
                deleteMe = checkForNamespaceFolder(fullName);
                if (null == deleteMe) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
            }
            FolderCache.removeCachedFolders(session, accountId);
            clearListLsubCache = true;
            synchronized (deleteMe) {
                imapAccess.getMessageStorage().notifyIMAPFolderModification(fullName);
                if (hardDelete) {
                    /*
                     * Delete permanently
                     */
                    deleteFolder(deleteMe);
                } else {
                    final String trashFullName = getTrashFolder();
                    final IMAPFolder trashFolder = (IMAPFolder) imapStore.getFolder(trashFullName);
                    if (isSubfolderOf(deleteMe.getParent().getFullName(), trashFullName, getSeparator()) || !inferiors(trashFolder)) {
                        /*
                         * Delete permanently
                         */
                        deleteFolder(deleteMe);
                    } else {
                        /*
                         * Just move this folder to trash
                         */
                        imapAccess.getMessageStorage().notifyIMAPFolderModification(trashFolder.getFullName());
                        final String name = getNameOf(deleteMe);
                        int appendix = 1;
                        final com.openexchange.java.StringAllocator sb = new com.openexchange.java.StringAllocator();
                        IMAPFolder newFolder =
                            (IMAPFolder) imapStore.getFolder(sb.append(trashFolder.getFullName()).append(getSeparator(trashFolder)).append(
                                name).toString());
                        while (newFolder.exists()) {
                            /*
                             * A folder of the same name already exists. Append appropriate appendix to folder name and check existence
                             * again.
                             */
                            if (sb.length() > 0) {
                                sb.reinitTo(0);
                            }
                            newFolder =
                                (IMAPFolder) imapStore.getFolder(sb.append(trashFolder.getFullName()).append(getSeparator(trashFolder)).append(
                                    name).append('_').append(++appendix).toString());
                        }
                        synchronized (newFolder) {
                            try {
                                moveFolder(deleteMe, trashFolder, newFolder, false);
                            } catch (final OXException e) {
                                deleteTemporaryCreatedFolder(newFolder);
                                throw e;
                            } catch (final MessagingException e) {
                                deleteTemporaryCreatedFolder(newFolder);
                                throw e;
                            }
                        }
                    }
                }
                return fullName;
            }
        } catch (final MessagingException e) {
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, mapFor("fullName", fullName));
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        } finally {
            if (clearListLsubCache) {
                ListLsubCache.clearCache(accountId, session);
            }
        }
    }

    private boolean inferiors(final IMAPFolder imapFolder) throws OXException, MessagingException {
        return getLISTEntry(imapFolder).hasInferiors();
    }

    private static final Flags FLAGS_DELETED = new Flags(Flags.Flag.DELETED);

    @Override
    public void expungeFolder(final String fullName) throws OXException {
        expungeFolder(fullName, false);
    }

    @Override
    public void expungeFolder(final String fullName, final boolean hardDelete) throws OXException {
        try {
            if (DEFAULT_FOLDER_ID.equals(fullName)) {
                return;
            }
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            IMAPFolder f = getIMAPFolderWithRecentListener(fullName);
            if (!doesExist(f, true)) {
                f = checkForNamespaceFolder(fullName);
                if (null == f) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
            }
            FolderCache.removeCachedFolder(fullName, session, accountId);
            // ListLsubCache.removeCachedEntry(fullName, accountId, session);
            synchronized (f) {
                imapAccess.getMessageStorage().notifyIMAPFolderModification(fullName);
                try {
                    if (!isSelectable(f)) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, f.getFullName());
                    }
                    if (imapConfig.isSupportsACLs()) {
                        final Rights myrights = RightsCache.getCachedRights(f, true, session, accountId);
                        if (!imapConfig.getACLExtension().canRead(myrights)) {
                            throw IMAPException.create(IMAPException.Code.NO_READ_ACCESS, imapConfig, session, f.getFullName());
                        }
                        if (!imapConfig.getACLExtension().canDeleteMessages(myrights)) {
                            throw IMAPException.create(IMAPException.Code.NO_DELETE_ACCESS, imapConfig, session, f.getFullName());
                        }
                    }
                } catch (final MessagingException e) {
                    throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, f.getFullName());
                }
                /*
                 * Remove from session storage
                 */
                if (IMAPSessionStorageAccess.isEnabled()) {
                    IMAPSessionStorageAccess.removeDeletedFolder(accountId, session, fullName);
                }
                final OperationKey opKey = new OperationKey(Type.MSG_DELETE, accountId, new Object[] { fullName });
                boolean marked = false;
                f.open(Folder.READ_WRITE);
                try {
                    final int msgCount = f.getMessageCount();
                    if (msgCount <= 0) {
                        /*
                         * Empty folder
                         */
                        return;
                    }

                    marked = setMarker(opKey, f);

                    String trashFullname = null;
                    final boolean hardDeleteMsgsByConfig = UserSettingMailStorage.getInstance().getUserSettingMail(session.getUserId(), ctx).isHardDeleteMsgs();
                    final boolean backup = (!hardDelete && !hardDeleteMsgsByConfig && !isSubfolderOf(
                        f.getFullName(),
                        (trashFullname = getTrashFolder()),
                        getSeparator()));
                    if (backup) {
                        imapAccess.getMessageStorage().notifyIMAPFolderModification(trashFullname);
                        final Message[] candidates = f.search(new FlagTerm(FLAGS_DELETED, true));
                        if (null != candidates && candidates.length > 0) {
                            f.copyMessages(candidates, imapStore.getFolder(trashFullname));
                        }
                    }
                }  finally {
                    if (marked) {
                        unsetMarker(opKey);
                    }
                    f.close(true);
                }
            }
        } catch (final MessagingException e) {
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, mapFor("fullName", fullName));
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        } finally {
            ListLsubCache.clearCache(accountId, session);
        }
    }

    @Override
    public void clearFolder(final String fullName, final boolean hardDelete) throws OXException {
        try {
            if (DEFAULT_FOLDER_ID.equals(fullName)) {
                throw MailExceptionCode.NO_ROOT_FOLDER_MODIFY_DELETE.create();
            }
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            IMAPFolder f = getIMAPFolderWithRecentListener(fullName);
            if (!doesExist(f, true)) {
                f = checkForNamespaceFolder(fullName);
                if (null == f) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
            }
            FolderCache.removeCachedFolder(fullName, session, accountId);
            // ListLsubCache.removeCachedEntry(fullName, accountId, session);
            synchronized (f) {
                imapAccess.getMessageStorage().notifyIMAPFolderModification(fullName);
                try {
                    if (!isSelectable(f)) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, f.getFullName());
                    }
                    if (imapConfig.isSupportsACLs()) {
                        final Rights myrights = RightsCache.getCachedRights(f, true, session, accountId);
                        if (!imapConfig.getACLExtension().canRead(myrights)) {
                            throw IMAPException.create(IMAPException.Code.NO_READ_ACCESS, imapConfig, session, f.getFullName());
                        }
                        if (!imapConfig.getACLExtension().canDeleteMessages(myrights)) {
                            throw IMAPException.create(IMAPException.Code.NO_DELETE_ACCESS, imapConfig, session, f.getFullName());
                        }
                    }
                } catch (final MessagingException e) {
                    throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, f.getFullName());
                }
                /*
                 * Remove from session storage
                 */
                if (IMAPSessionStorageAccess.isEnabled()) {
                    IMAPSessionStorageAccess.removeDeletedFolder(accountId, session, fullName);
                }
                final OperationKey opKey = new OperationKey(Type.MSG_DELETE, accountId, new Object[] { fullName });
                boolean marked = false;
                f.open(Folder.READ_WRITE);
                try {
                    int msgCount = f.getMessageCount();
                    if (msgCount <= 0) {
                        /*
                         * Empty folder
                         */
                        return;
                    }

                    marked = setMarker(opKey, f);

                    String trashFullname = null;
                    final boolean hardDeleteMsgsByConfig = UserSettingMailStorage.getInstance().getUserSettingMail(session.getUserId(), ctx).isHardDeleteMsgs();
                    final boolean backup =
                        (!hardDelete && !hardDeleteMsgsByConfig && !isSubfolderOf(f.getFullName(), (trashFullname = getTrashFolder()), getSeparator()));
                    if (backup) {
                        imapAccess.getMessageStorage().notifyIMAPFolderModification(trashFullname);
                    }
                    final int blockSize = imapConfig.getIMAPProperties().getBlockSize();
                    if (blockSize > 0) {
                        /*
                         * Block-wise deletion
                         */
                        while (msgCount > blockSize) {
                            /*
                             * Don't adapt sequence number since folder expunge already resets message numbering
                             */
                            if (backup) {
                                try {
                                    new CopyIMAPCommand(f, 1, blockSize, trashFullname).doCommand();
                                } catch (final MessagingException e) {
                                    if (e.getMessage().indexOf("Over quota") > -1) {
                                        /*
                                         * We face an Over-Quota-Exception
                                         */
                                        throw MailExceptionCode.DELETE_FAILED_OVER_QUOTA.create(e, new Object[0]);
                                    }
                                    final Exception nestedExc = e.getNextException();
                                    if (nestedExc != null && nestedExc.getMessage().indexOf("Over quota") > -1) {
                                        /*
                                         * We face an Over-Quota-Exception
                                         */
                                        throw MailExceptionCode.DELETE_FAILED_OVER_QUOTA.create(e, new Object[0]);
                                    }
                                    throw IMAPException.create(
                                        IMAPException.Code.MOVE_ON_DELETE_FAILED,
                                        imapConfig,
                                        session,
                                        e,
                                        new Object[0]);
                                }
                            }
                            /*
                             * Delete through storing \Deleted flag...
                             */
                            new FlagsIMAPCommand(f, 1, blockSize, FLAGS_DELETED, true, true).doCommand();
                            /*
                             * ... and perform EXPUNGE
                             */
                            try {
                                IMAPCommandsCollection.fastExpunge(f);
                            } catch (final FolderClosedException e) {
                                /*
                                 * Not possible to retry since connection is broken
                                 */
                                throw IMAPException.create(
                                    IMAPException.Code.CONNECT_ERROR,
                                    imapConfig,
                                    session,
                                    e,
                                    imapConfig.getServer(),
                                    imapConfig.getLogin());
                            } catch (final StoreClosedException e) {
                                /*
                                 * Not possible to retry since connection is broken
                                 */
                                throw IMAPException.create(
                                    IMAPException.Code.CONNECT_ERROR,
                                    imapConfig,
                                    session,
                                    e,
                                    imapConfig.getServer(),
                                    imapConfig.getLogin());
                            }
                            /*
                             * Decrement
                             */
                            msgCount -= blockSize;
                        }
                    }
                    if (msgCount == 0) {
                        /*
                         * All messages already cleared through previous block-wise deletion
                         */
                        return;
                    }
                    if (backup) {
                        try {
                            new CopyIMAPCommand(f, trashFullname).doCommand();
                        } catch (final MessagingException e) {
                            if (e.getNextException() instanceof CommandFailedException) {
                                final CommandFailedException exc = (CommandFailedException) e.getNextException();
                                if (exc.getMessage().indexOf("Over quota") > -1) {
                                    /*
                                     * We face an Over-Quota-Exception
                                     */
                                    throw MailExceptionCode.DELETE_FAILED_OVER_QUOTA.create(e, new Object[0]);
                                }
                            }
                            throw IMAPException.create(IMAPException.Code.MOVE_ON_DELETE_FAILED, imapConfig, session, e, new Object[0]);
                        }
                    }
                    /*
                     * Delete through storing \Deleted flag...
                     */
                    new FlagsIMAPCommand(f, FLAGS_DELETED, true, true).doCommand();
                    /*
                     * ... and perform EXPUNGE
                     */
                    final long start = System.currentTimeMillis();
                    IMAPCommandsCollection.fastExpunge(f);
                    mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
                } finally {
                    closeSafe(f);
                    if (marked) {
                        unsetMarker(opKey);
                    }
                }
            }
        } catch (final MessagingException e) {
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, mapFor("fullName", fullName));
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        } finally {
            ListLsubCache.clearCache(accountId, session);
        }
    }

    private boolean isSelectable(final IMAPFolder imapFolder) throws OXException, MessagingException {
        return getLISTEntry(imapFolder).canOpen();
    }

    @Override
    public MailFolder[] getPath2DefaultFolder(final String fullName) throws OXException {
        try {
            if (fullName.equals(DEFAULT_FOLDER_ID)) {
                return EMPTY_PATH;
            }
            IMAPFolder f = getIMAPFolder(fullName);
            if (!doesExist(f, true)) {
                f = checkForNamespaceFolder(fullName);
                if (null == f) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
            }
            /*
             * Obtain folder lock once to avoid multiple acquire/releases when invoking folder's getXXX() methods
             */
            if (imapConfig.isSupportsACLs() && isSelectable(f)) {
                try {
                    if (!imapConfig.getACLExtension().canLookUp(RightsCache.getCachedRights(f, true, session, accountId))) {
                        throw IMAPException.create(IMAPException.Code.NO_LOOKUP_ACCESS, imapConfig, session, fullName);
                    }
                } catch (final MessagingException e) {
                    throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, fullName);
                }
            }
            final List<MailFolder> list = new ArrayList<MailFolder>();
            final String defaultFolder = "";
            String fn;
            while (!(fn = f.getFullName()).equals(defaultFolder)) {
                list.add(FolderCache.getCachedFolder(fn, this));
                f = (IMAPFolder) f.getParent();
            }
            return list.toArray(new MailFolder[list.size()]);
        } catch (final MessagingException e) {
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, mapFor("fullName", fullName));
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public String getDefaultFolderPrefix() throws OXException {
        final String urlName = urlName("ns-", imapConfig.getServer(), imapConfig.getPort(), imapConfig.getLogin(), imapConfig.getPassword());
        String defaultFolderPrefix = (String) session.getParameter(urlName);
        if (null == defaultFolderPrefix) {
            defaultFolderPrefix = getDefaultFolderPrefix0();
            session.setParameter(urlName, defaultFolderPrefix);
        }
        return defaultFolderPrefix;
    }

    private String getDefaultFolderPrefix0() throws OXException {
        try {
            // Special handling for GMail...
            {
                final String server = imapConfig.getServer().toLowerCase(Locale.US);
                if ("imap.gmail.com".equals(server) || "imap.googlemail.com".equals(server)) {
                    /*
                     * Look-up special GMail folder: [GMail], [Google Mail], ...
                     */
                    final ListLsubEntry rootEntry = ListLsubCache.getCachedLISTEntry("", accountId, imapStore, session);
                    final List<ListLsubEntry> children = rootEntry.getChildren();
                    final String prefix = "[G";
                    for (final ListLsubEntry child : children) {
                        final String fullName = child.getFullName();
                        if (fullName.startsWith(prefix)) {
                            return fullName + child.getSeparator();
                        }
                    }
                }
            }
            // Try NAMESPACE command
            final char[] sep = new char[1];
            final String prefixByInferiors = prefixByInferiors(sep);
            final String prefix;
            try {
                final String[] namespaces = NamespaceFoldersCache.getPersonalNamespaces(imapStore, true, session, accountId);
                if (null == namespaces || 0 == namespaces.length) {
                    // No namespaces available
                    LOG.info("IMAP server {} does not provide a personal namespace for login {}. Using fall-back \"by inferiors\" detection: \"{}\" (user={}, context={})", imapConfig.getServer(), imapConfig.getLogin(), prefixByInferiors, session.getUserId(), session.getContextId());
                    return prefixByInferiors;
                }
                prefix = namespaces[0];
            } catch (final MessagingException e) {
                LOG.error("NAMESPACE command failed for any reason", e);
                return prefixByInferiors;
            }
            final boolean isEmpty = prefix.length() == 0;
            if (isEmpty && RootSubfolderCache.canCreateSubfolders((DefaultFolder) imapStore.getDefaultFolder(), true, session, accountId).booleanValue()) {
                return prefix;
            }
            final String retvalPrefix = new com.openexchange.java.StringAllocator(isEmpty ? STR_INBOX : prefix).append(sep[0]).toString();
            if (!retvalPrefix.equals(prefixByInferiors)) {
                LOG.warn("The personal namespace indicated by NAMESPACE command does not match root folder's capabilities: {} IS NOT {}", retvalPrefix, prefixByInferiors);
            }
            return retvalPrefix;
        } catch (final MessagingException e) {
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, null);
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private String prefixByInferiors(final char[] sep) throws OXException {
        try {
            final DefaultFolder defaultFolder = (DefaultFolder) imapStore.getDefaultFolder();
            final char separator = defaultFolder.getSeparator();
            sep[0] = separator;
            if (!RootSubfolderCache.canCreateSubfolders(defaultFolder, true, session, accountId).booleanValue() || MailProperties.getInstance().isAllowNestedDefaultFolderOnAltNamespace()) {
                return new com.openexchange.java.StringAllocator(STR_INBOX).append(separator).toString();
            }
            return "";
        } catch (final MessagingException e) {
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, null);
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public String getConfirmedHamFolder() throws OXException {
        return getChecker().getDefaultFolder(StorageUtility.INDEX_CONFIRMED_HAM);
    }

    @Override
    public String getConfirmedSpamFolder() throws OXException {
        return getChecker().getDefaultFolder(StorageUtility.INDEX_CONFIRMED_SPAM);
    }

    @Override
    public String getDraftsFolder() throws OXException {
        return getChecker().getDefaultFolder(StorageUtility.INDEX_DRAFTS);
    }

    @Override
    public String getSentFolder() throws OXException {
        return getChecker().getDefaultFolder(StorageUtility.INDEX_SENT);
    }

    @Override
    public String getSpamFolder() throws OXException {
        return getChecker().getDefaultFolder(StorageUtility.INDEX_SPAM);
    }

    @Override
    public String getTrashFolder() throws OXException {
        return getChecker().getDefaultFolder(StorageUtility.INDEX_TRASH);
    }

    @Override
    public void releaseResources() throws IMAPException {
        // Nothing to release
    }

    @Override
    public com.openexchange.mail.Quota[] getQuotas(final String folder, final com.openexchange.mail.Quota.Type[] types) throws OXException {
        try {
            final String fullName = folder == null ? STR_INBOX : folder;
            final boolean isDefaultFolder = fullName.equals(DEFAULT_FOLDER_ID);
            final IMAPFolder f = (IMAPFolder) (isDefaultFolder ? imapStore.getDefaultFolder() : imapStore.getFolder(fullName));
            /*
             * Obtain folder lock once to avoid multiple acquire/releases when invoking folder's getXXX() methods
             */
            synchronized (f) {
                if (!isDefaultFolder && !doesExist(f, true)) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
                try {
                    if (!isSelectable(f)) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, fullName);
                    }
                    if (imapConfig.isSupportsACLs()) {
                        final Rights myrights = RightsCache.getCachedRights(f, true, session, accountId);
                        if (!imapConfig.getACLExtension().canRead(myrights)) {
                            throw IMAPException.create(IMAPException.Code.NO_READ_ACCESS, imapConfig, session, fullName);
                        }
                        /*-
                         * TODO: Why check DELETE access when requesting quota?
                         *
                        if (!imapConfig.getACLExtension().canDeleteMailbox(myrights)) {
                            throw IMAPException.create(IMAPException.Code.NO_DELETE_ACCESS, imapConfig, session, fullName);
                        }
                         */
                    }
                } catch (final MessagingException e) {
                    throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, fullName);
                }
                // f.open(Folder.READ_ONLY);
                if (!imapConfig.getImapCapabilities().hasQuota()) {
                    return com.openexchange.mail.Quota.getUnlimitedQuotas(types);
                }
                Quota[] folderQuota = null;
                try {
                    final long start = System.currentTimeMillis();
                    folderQuota = f.getQuota();
                    mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
                } catch (final MessagingException mexc) {
                    if (mexc.getNextException() instanceof ParsingException) {
                        try {
                            final long start = System.currentTimeMillis();
                            folderQuota = IMAPCommandsCollection.getQuotaRoot(f);
                            mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
                        } catch (final MessagingException inner) {
                            LOG.warn("", inner);
                            return com.openexchange.mail.Quota.getUnlimitedQuotas(types);
                        }
                    } else {
                        throw mexc;
                    }
                }
                if (folderQuota == null || folderQuota.length == 0) {
                    return com.openexchange.mail.Quota.getUnlimitedQuotas(types);
                }
                final Quota.Resource[] resources = folderQuota[0].resources;
                if (resources.length == 0) {
                    return com.openexchange.mail.Quota.getUnlimitedQuotas(types);
                }
                final com.openexchange.mail.Quota[] quotas = new com.openexchange.mail.Quota[types.length];
                for (int i = 0; i < types.length; i++) {
                    final String typeStr = types[i].toString();
                    /*
                     * Find corresponding resource to current type
                     */
                    Resource resource = null;
                    for (int k = 0; k < resources.length && resource == null; k++) {
                        if (typeStr.equalsIgnoreCase(resources[k].name)) {
                            resource = resources[k];
                        }
                    }
                    if (resource == null) {
                        /*
                         * No quota limitation found that applies to current resource type
                         */
                        quotas[i] = com.openexchange.mail.Quota.getUnlimitedQuota(types[i]);
                    } else {
                        quotas[i] = new com.openexchange.mail.Quota(resource.limit, resource.usage, types[i]);
                    }
                }
                return quotas;
            }
        } catch (final MessagingException e) {
            throw IMAPException.handleMessagingException(e, imapConfig, session, accountId, mapFor("fullName", folder));
        } catch (final RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    /*
     * ++++++++++++++++++ Helper methods ++++++++++++++++++
     */

    private void unsetMarker(final OperationKey key) {
        OperationKey.unsetMarker(key, session);
    }

    private boolean setMarker(final OperationKey key, final Folder imapFolder) throws OXException {
        final int result = OperationKey.setMarker(key, session);
        if (result < 0) {
            // In use...
            throw MimeMailExceptionCode.IN_USE_ERROR_EXT.create(
                imapConfig.getServer(),
                imapConfig.getLogin(),
                Integer.valueOf(session.getUserId()),
                Integer.valueOf(session.getContextId()),
                MimeMailException.appendInfo("Mailbox is currently in use.", imapFolder));
        }
        return result > 0;
    }

    private static void closeSafe(final Folder folder) {
        if (null != folder) {
            try {
                folder.close(false);
            } catch (final Exception e) {
                // Ignore
            }
        }
    }

    /*-
     * Get the QUOTA resource with the highest usage-per-limitation value
     *
     * @param resources The QUOTA resources
     * @return The QUOTA resource with the highest usage to limitation relation
     *
     *
     * private static Resource getMaxUsageResource(final Quota.Resource[] resources) {
     *     final Resource maxUsageResource;
     *     {
     *         int index = 0;
     *         long maxUsage = resources[0].usage / resources[0].limit;
     *         for (int i = 1; i &lt; resources.length; i++) {
     *             final long tmp = resources[i].usage / resources[i].limit;
     *             if (tmp &gt; maxUsage) {
     *                 maxUsage = tmp;
     *                 index = i;
     *             }
     *         }
     *         maxUsageResource = resources[index];
     *     }
     *     return maxUsageResource;
     * }
     */

    /**
     * Get the ACL list of specified folder
     *
     * @param imapFolder The IMAP folder
     * @return The ACL list or <code>null</code> if any error occurred
     */
    private static ACL[] getACLSafe(final IMAPFolder imapFolder) {
        try {
            return imapFolder.getACL();
        } catch (final MessagingException e) {
            LOG.debug("", e);
            return null;
        }
    }

    private void deleteFolder(final IMAPFolder deleteMe) throws OXException, MessagingException {
        final String fullName = deleteMe.getFullName();
        if (getChecker().isDefaultFolder(fullName)) {
            throw IMAPException.create(IMAPException.Code.NO_DEFAULT_FOLDER_DELETE, imapConfig, session, fullName);
        } else if (!doesExist(deleteMe, false)) {
            throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
        }
        try {
            if (imapConfig.isSupportsACLs() && ((deleteMe.getType() & Folder.HOLDS_MESSAGES) > 0) && !imapConfig.getACLExtension().canDeleteMailbox(
                RightsCache.getCachedRights(deleteMe, true, session, accountId))) {
                throw IMAPException.create(IMAPException.Code.NO_CREATE_ACCESS, imapConfig, session, fullName);
            }
        } catch (final MessagingException e) {
            throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, fullName);
        }
        if (deleteMe.isOpen()) {
            deleteMe.close(false);
        }
        /*
         * Unsubscribe prior to deletion
         */
        IMAPCommandsCollection.forceSetSubscribed(imapStore, fullName, false);
        removeSessionData(deleteMe);
        final long start = System.currentTimeMillis();
        if (!deleteMe.delete(true)) {
            throw IMAPException.create(IMAPException.Code.DELETE_FAILED, imapConfig, session, fullName);
        }
        mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
        /*
         * Remove cache entries
         */
        FolderCache.removeCachedFolder(fullName, session, accountId);
        // ListLsubCache.clearCache(accountId, session);
        RightsCache.removeCachedRights(deleteMe, session, accountId);
        UserFlagsCache.removeUserFlags(deleteMe, session, accountId);
    }

    private boolean stillHoldsFullRights(final IMAPFolder defaultFolder, final ACL[] newACLs, final ACLExtension aclExtension) throws OXException {
        /*
         * Ensure that owner still holds full rights
         */
        final String ownerACLName =
            getEntity2ACL().getACLName(session.getUserId(), ctx, IMAPFolderConverter.getEntity2AclArgs(session, defaultFolder, imapConfig));
        final Rights fullRights = aclExtension.getFullRights();
        for (final ACL newACL : newACLs) {
            if (newACL.getName().equals(ownerACLName) && newACL.getRights().contains(fullRights)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Boolean> getSubscriptionStatus(final IMAPFolder f, final String oldFullName, final String newFullName) throws MessagingException, OXException {
        final Map<String, Boolean> retval = new HashMap<String, Boolean>();
        getSubscriptionStatus(retval, f, oldFullName, newFullName);
        return retval;
    }

    private void getSubscriptionStatus(final Map<String, Boolean> m, final IMAPFolder f, final String oldFullName, final String newFullName) throws MessagingException, OXException {
        if ((f.getType() & Folder.HOLDS_FOLDERS) > 0) {
            final Folder[] folders = f.list();
            for (int i = 0; i < folders.length; i++) {
                getSubscriptionStatus(m, (IMAPFolder) folders[i], oldFullName, newFullName);
            }
        }
        m.put(f.getFullName().replaceFirst(quoteReplacement(oldFullName), quoteReplacement(newFullName)), Boolean.valueOf(f.isSubscribed()));
    }

    private void setFolderSubscription(final IMAPFolder f, final boolean subscribed) throws MessagingException {
        if ((f.getType() & Folder.HOLDS_FOLDERS) > 0) {
            final Folder[] folders = f.list();
            for (int i = 0; i < folders.length; i++) {
                setFolderSubscription((IMAPFolder) folders[i], subscribed);
            }
        }
        f.setSubscribed(subscribed);
    }

    private void applySubscriptionStatus(final IMAPFolder f, final Map<String, Boolean> m) throws MessagingException, OXException {
        if ((f.getType() & Folder.HOLDS_FOLDERS) > 0) {
            final Folder[] folders = f.list();
            for (int i = 0; i < folders.length; i++) {
                applySubscriptionStatus((IMAPFolder) folders[i], m);
            }
        }
        Boolean b = m.get(f.getFullName());
        if (b == null) {
            LOG.warn("No stored subscription status found for {}", f.getFullName());
            b = Boolean.TRUE;
        }
        f.setSubscribed(b.booleanValue());
    }

    private IMAPFolder moveFolder(final IMAPFolder toMove, final IMAPFolder destFolder, final String folderName) throws MessagingException, OXException {
        String name = folderName;
        if (name == null) {
            name = getNameOf(toMove);
        }
        final String destFullname = destFolder.getFullName();
        final IMAPFolder newFolder;
        {
            final com.openexchange.java.StringAllocator sb = new com.openexchange.java.StringAllocator();
            if (destFullname.length() > 0) {
                sb.append(destFullname).append(getSeparator(destFolder));
            }
            sb.append(name);
            newFolder = (IMAPFolder) imapStore.getFolder(sb.toString());
        }
        return moveFolder(toMove, destFolder, newFolder, true);
    }

    private IMAPFolder moveFolder(final IMAPFolder toMove, final IMAPFolder destFolder, final IMAPFolder newFolder, final boolean checkForDuplicate) throws MessagingException, OXException {
        final String destFullName = destFolder.getFullName();
        if ((destFolder.getType() & Folder.HOLDS_FOLDERS) == 0) {
            throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_FOLDERS, imapConfig, session, destFullName);
        }
        final String moveFullname = toMove.getFullName();
        final int toMoveType = toMove.getType();
        if (imapConfig.isSupportsACLs() && ((toMoveType & Folder.HOLDS_MESSAGES) > 0)) {
            try {
                if (!imapConfig.getACLExtension().canRead(RightsCache.getCachedRights(toMove, true, session, accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_READ_ACCESS, imapConfig, session, moveFullname);
                }
            } catch (final MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, moveFullname);
            }
            try {
                if (!imapConfig.getACLExtension().canCreate(RightsCache.getCachedRights(toMove, true, session, accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_CREATE_ACCESS, imapConfig, session, moveFullname);
                }
            } catch (final MessagingException e) {
                /*
                 * MYRIGHTS command failed for given mailbox
                 */
                if (!imapConfig.getImapCapabilities().hasNamespace() || !NamespaceFoldersCache.containedInPersonalNamespaces(
                    moveFullname,
                    imapStore,
                    true,
                    session,
                    accountId)) {
                    /*
                     * No namespace support or given parent is NOT covered by user's personal namespaces.
                     */
                    throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, moveFullname);
                }
                LOG.debug("MYRIGHTS command failed on namespace folder", e);
            }
        }
        /*
         * Move by creating a new folder, copying all messages and deleting old folder
         */
        if (checkForDuplicate && newFolder.exists()) {
            throw IMAPException.create(IMAPException.Code.DUPLICATE_FOLDER, imapConfig, session, getNameOf(newFolder));
        }
        /*
         * Perform RENAME
         */
        final boolean subscribed = toMove.isSubscribed();
        if (!toMove.renameTo(newFolder)) {
            throw IMAPException.create(
                IMAPException.Code.FOLDER_CREATION_FAILED,
                imapConfig,
                session,
                newFolder.getFullName(),
                destFolder instanceof DefaultFolder ? DEFAULT_FOLDER_ID : destFullName);
        }
        /*
         * Apply original subscription status
         */
        newFolder.setSubscribed(subscribed);
        /*
         * Delete/unsubscribe old folder
         */
        IMAPCommandsCollection.forceSetSubscribed(imapStore, moveFullname, false);
        /*
         * Notify message storage
         */
        imapAccess.getMessageStorage().notifyIMAPFolderModification(moveFullname);
        /*
         * Remove cache entries
         */
        FolderCache.removeCachedFolder(moveFullname, session, accountId);
        // ListLsubCache.clearCache(accountId, session);
        RightsCache.removeCachedRights(moveFullname, session, accountId);
        UserFlagsCache.removeUserFlags(moveFullname, session, accountId);
        if (IMAPSessionStorageAccess.isEnabled()) {
            IMAPSessionStorageAccess.removeDeletedFolder(accountId, session, moveFullname);
        }
        return newFolder;
    }

    private ACL[] permissions2ACL(final OCLPermission[] perms, final IMAPFolder imapFolder) throws OXException, MessagingException {
        final List<ACL> acls = new ArrayList<ACL>(perms.length);
        for (int i = 0; i < perms.length; i++) {
            final ACLPermission aclPermission = getACLPermission(perms[i]);
            try {
                acls.add(aclPermission.getPermissionACL(
                    IMAPFolderConverter.getEntity2AclArgs(session, imapFolder, imapConfig),
                    imapConfig,
                    imapStore,
                    ctx));
            } catch (final OXException e) {
                if (Entity2ACLExceptionCode.UNKNOWN_USER.equals(e)) {
                    // Obviously the user is not known, skip
                    LOG.debug("User {} is not known on IMAP server \"{}\"", aclPermission.getEntity(), imapConfig.getImapServerAddress());
                } else if (UserExceptionCode.USER_NOT_FOUND.equals(e)) {
                    // Obviously the user is not known, skip
                    LOG.debug("User 0} is not known on IMAP server \"{}\"", aclPermission.getEntity(), imapConfig.getImapServerAddress());
                } else {
                    throw e;
                }
            }
        }
        return acls.toArray(new ACL[acls.size()]);
    }

    private ACLPermission getACLPermission(final OCLPermission permission) {
        if (permission instanceof ACLPermission) {
            return (ACLPermission) permission;
        }
        final ACLPermission retval = new ACLPermission();
        retval.setEntity(permission.getEntity());
        retval.setDeleteObjectPermission(permission.getDeletePermission());
        retval.setFolderAdmin(permission.isFolderAdmin());
        retval.setFolderPermission(permission.getFolderPermission());
        retval.setGroupPermission(permission.isGroupPermission());
        retval.setName(permission.getName());
        retval.setReadObjectPermission(permission.getReadPermission());
        retval.setSystem(permission.getSystem());
        retval.setWriteObjectPermission(permission.getWritePermission());
        return retval;
    }

    private static ACL[] getRemovedACLs(final Map<String, ACL> newACLs, final ACL[] oldACLs) {
        final List<ACL> retval = new ArrayList<ACL>();
        for (final ACL oldACL : oldACLs) {
            final ACL newACL = newACLs.get(oldACL.getName());
            if (null == newACL) {
                retval.add(oldACL);
            }
        }
        return retval.toArray(new ACL[retval.size()]);
    }

    private static boolean isKnownEntity(final String entity, final Entity2ACL entity2ACL, final Context ctx, final Entity2ACLArgs args) {
        try {
            return !UserGroupID.NULL.equals(entity2ACL.getEntityID(entity, ctx, args));
        } catch (final OXException e) {
            return false;
        }
    }

    private boolean equals(final ACL[] oldACLs, final Map<String, ACL> newACLs, final Entity2ACL entity2ACL, final Entity2ACLArgs args) {
        int examined = 0;
        for (final ACL oldACL : oldACLs) {
            final String oldName = oldACL.getName();
            if (isKnownEntity(oldName, entity2ACL, ctx, args)) {
                final ACL newACL = newACLs.get(oldName/* .toLowerCase(Locale.ENGLISH) */);
                if (null == newACL) {
                    // No corresponding entity in new ACLs
                    return false;
                }
                // Remember number of corresponding entities
                examined++;
                // Check ACLS' rights ignoring POST right
                if (!equalRights(oldACL.getRights().toString(), newACL.getRights().toString(), true)) {
                    return false;
                }
            }
        }
        return (examined == newACLs.size());
    }

    private static String stripPOSTRight(final String rights) {
        final com.openexchange.java.StringAllocator sb = new com.openexchange.java.StringAllocator(rights.length());
        final int length = rights.length();
        for (int i = 0; i < length; i++) {
            final char c = rights.charAt(i);
            if ('p' != c && 'P' != c) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean equalRights(final String rights1, final String rights2, final boolean ignorePOST) {
        final char[] r1;
        final char[] r2;
        if (ignorePOST) {
            r1 = stripPOSTRight(rights1).toCharArray();
            r2 = stripPOSTRight(rights2).toCharArray();
        } else {
            r1 = rights1.toCharArray();
            r2 = rights2.toCharArray();
        }
        if (r1.length != r2.length) {
            return false;
        }
        Arrays.sort(r1);
        Arrays.sort(r2);
        return Arrays.equals(r1, r2);
    }

    private static Map<String, ACL> acl2map(final ACL[] acls) {
        final Map<String, ACL> m = new HashMap<String, ACL>(acls.length);
        for (final ACL acl : acls) {
            m.put(acl.getName()/* .toLowerCase(Locale.ENGLISH) */, acl);
        }
        return m;
    }

    private static ACL validate(final ACL newACL, final Map<String, ACL> oldACLs) {
        final ACL oldACL = oldACLs.get(newACL.getName());
        if (null == oldACL) {
            /*
             * Either no corresponding old ACL or old ACL's rights is not equal to "p"
             */
            return newACL;
        }
        final Rights newRights = newACL.getRights();
        final Rights oldRights = oldACL.getRights();
        /*
         * Handle the POST-to-NOT-MAPPABLE problem
         */
        if (oldRights.contains(Rights.Right.POST) && !newRights.contains(Rights.Right.POST)) {
            newRights.add(Rights.Right.POST);
        }
        /*
         * Handle the READ-KEEP_SEEN-to-READ problem
         */
        if (oldRights.contains(Rights.Right.READ) && newRights.contains(Rights.Right.READ)) {
            /*
             * Both allow READ access
             */
            if (!oldRights.contains(Rights.Right.KEEP_SEEN) && newRights.contains(Rights.Right.KEEP_SEEN)) {
                newRights.remove(Rights.Right.KEEP_SEEN);
            }
        }
        return newACL;
    }

    /*-
     * Determines if <i>altNamespace</i> is enabled for mailbox. If <i>altNamespace</i> is enabled all folder which are logically located
     * below INBOX folder are represented as INBOX's siblings in IMAP folder tree. Dependent on IMAP server's implementation the INBOX
     * folder is then marked with attribute <code>\NoInferiors</code> meaning it no longer allows subfolders.
     *
     * @param imapStore - the IMAP store (mailbox)
     * @return <code>true</code> if altNamespace is enabled; otherwise <code>false</code>
     * @throws MessagingException - if IMAP's NAMESPACE command fails
    private static boolean isPersonalNamespaceEmpty(final IMAPStore imapStore) throws MessagingException {
        boolean altnamespace = false;
        final Folder[] pn = imapStore.getPersonalNamespaces();
        if ((pn.length != 0) && (pn[0].getFullName().trim().length() == 0)) {
            altnamespace = true;
        }
        return altnamespace;
    }*/

    private static final TIntSet WILDCARDS = new TIntHashSet(Arrays.asList(Integer.valueOf('%'), Integer.valueOf('*')));

    private static volatile TIntSet invalidChars;
    private static TIntSet invalidChars() {
        TIntSet tmp = invalidChars;
        if (null == tmp) {
            synchronized (IMAPFolderStorage.class) {
                tmp = invalidChars;
                if (null == tmp) {
                    final ConfigurationService service = Services.getService(ConfigurationService.class);
                    if (null == service) {
                        return new TIntHashSet(0);
                    }
                    final String invalids = service.getProperty("com.openexchange.imap.invalidMailboxNameCharacters");
                    if (isEmpty(invalids)) {
                        tmp = new TIntHashSet(0);
                    } else {
                        final String[] sa = Strings.splitByWhitespaces(Strings.unquote(invalids));
                        final int length = sa.length;
                        tmp = new TIntHashSet(length);
                        for (int i = 0; i < length; i++) {
                            tmp.add(sa[i].charAt(0));
                        }
                    }
                    invalidChars = tmp;
                }
            }
        }
        return tmp;
    }

    /**
     * Gets a <code>String</code> containing those characters considered as invalid for a mailbox name.
     *
     * @return A <code>String</code> containing invalid characters
     */
    public static String invalidCharsString(final char separator) {
        final TIntSet invalidChars = new TIntHashSet(invalidChars());
        invalidChars.addAll(WILDCARDS);
        invalidChars.add(separator);
        final StringAllocator sb = new StringAllocator(invalidChars.size());
        invalidChars.forEach(new TIntProcedure() {

            @Override
            public boolean execute(int c) {
                sb.append((char) c);
                return true;
            }
        });
        return sb.toString();
    }

    /**
     * Checks id specified folder name is allowed to be used on folder creation. The folder name is valid if the separator character does
     * not appear or provided that MBox format is enabled may only appear at name's end.
     *
     * @param name The folder name to check.
     * @param separator The separator character.
     * @param mboxEnabled <code>true</code> If MBox format is enabled; otherwise <code>false</code>
     * @return <code>true</code> if folder name is valid; otherwise <code>false</code>
     */
    private static boolean checkFolderNameValidity(final String name, final char separator, final boolean mboxEnabled) {
        final TIntProcedure procedure = new TIntProcedure() {

            @Override
            public boolean execute(final int value) {
                return name.indexOf(value) < 0;
            }
        };

        // Check for possibly contained wild-cards
        if (!WILDCARDS.forEach(procedure)) {
            return false;
        }

        // Check for possibly contained invalid characters (as per configuration)
        final TIntSet invalidChars = invalidChars();
        if (null != invalidChars && !invalidChars.isEmpty()) {
            if (!invalidChars.forEach(procedure)) {
                return false;
            }
        }

        // Check for possibly contained separator character (dependent on mbox format)
        final int pos = name.indexOf(separator);
        return mboxEnabled ? (pos < 0) || (pos == name.length() - 1) : (pos < 0);
    }

    private static final String REGEX_TEMPL = "[\\S\\p{Blank}&&[^\\p{Cntrl}#SEP#]]+(?:\\Q#SEP#\\E[\\S\\p{Blank}&&[^\\p{Cntrl}#SEP#]]+)*";

    private static final Pattern PAT_SEP = Pattern.compile("#SEP#");

    private static boolean checkFolderPathValidity(final String path, final char separator) {
        if ((path != null) && (path.length() > 0)) {
            return Pattern.compile(PAT_SEP.matcher(REGEX_TEMPL).replaceAll(String.valueOf(separator))).matcher(path).matches();
        }
        return false;
    }

    private void removeSessionData(final Folder f) {
        if (!IMAPSessionStorageAccess.isEnabled()) {
            return;
        }
        try {
            final Folder[] fs = f.list();
            for (int i = 0; i < fs.length; i++) {
                removeSessionData(fs[i]);
            }
            IMAPSessionStorageAccess.removeDeletedFolder(accountId, session, f.getFullName());
        } catch (final MessagingException e) {
            LOG.error("", e);
        }
    }

    private IMAPFolder getIMAPFolder(final String fullName) throws MessagingException {
        return DEFAULT_FOLDER_ID.equals(fullName) ? (IMAPFolder) imapStore.getDefaultFolder() : (IMAPFolder) imapStore.getFolder(fullName);
    }

    private IMAPFolder getIMAPFolderWithRecentListener(final String fullName) throws MessagingException {
        final IMAPFolder ret =
            DEFAULT_FOLDER_ID.equals(fullName) ? (IMAPFolder) imapStore.getDefaultFolder() : (IMAPFolder) imapStore.getFolder(fullName);
        if (MailAccount.DEFAULT_ID == accountId && imapConfig.getIMAPProperties().notifyRecent()) {
            IMAPNotifierMessageRecentListener.addNotifierFor(ret, fullName, accountId, session, true);
        }
        return ret;
    }

    private boolean doesExist(final IMAPFolder imapFolder, final boolean readOnly) throws OXException, MessagingException {
        final String fullName = imapFolder.getFullName();
        return STR_INBOX.equals(fullName) || (readOnly ? getLISTEntry(fullName, imapFolder).exists() : imapFolder.exists());
    }

    private static boolean doesExist(final ListLsubEntry entry) {
        return STR_INBOX.equals(entry.getFullName()) || (entry.exists());
    }

    private ListLsubEntry getLISTEntry(final IMAPFolder imapFolder) throws OXException, MessagingException {
        return getLISTEntry(imapFolder.getFullName(), imapFolder);
    }

    private ListLsubEntry getLISTEntry(final String fullName, final IMAPFolder imapFolder) throws OXException, MessagingException {
        return ListLsubCache.getCachedLISTEntry(fullName, accountId, imapFolder, session);
    }

    private ListLsubEntry getLSUBEntry(final IMAPFolder imapFolder) throws OXException, MessagingException {
        return getLSUBEntry(imapFolder.getFullName(), imapFolder);
    }

    private ListLsubEntry getLSUBEntry(final String fullName, final IMAPFolder imapFolder) throws OXException, MessagingException {
        return ListLsubCache.getCachedLSUBEntry(fullName, accountId, imapFolder, session);
    }

    private char getSeparator(final IMAPFolder imapFolder) throws OXException, MessagingException {
        return getLISTEntry(STR_INBOX, imapFolder).getSeparator();
    }

    private String getNameOf(final IMAPFolder imapFolder) throws OXException, MessagingException {
        final String fullName = imapFolder.getFullName();
        return fullName.substring(fullName.lastIndexOf(getSeparator(imapFolder)) + 1);
    }

    private OXException handleRuntimeException(final RuntimeException e) {
        if (e instanceof ListLsubRuntimeException) {
            ListLsubCache.clearCache(accountId, session);
            return MailExceptionCode.INTERRUPT_ERROR.create(e, e.getMessage());
        }
        return MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
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

    private boolean startsWithNamespaceFolder(final String fullName, final char separator) throws MessagingException {
        for (final String nsFullName : NamespaceFoldersCache.getUserNamespaces(imapStore, true, session, accountId)) {
            if (isSubfolderOf(fullName, nsFullName, separator)) {
                return true;
            }
        }
        for (final String nsFullName : NamespaceFoldersCache.getSharedNamespaces(imapStore, true, session, accountId)) {
            if (isSubfolderOf(fullName, nsFullName, separator)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Object> mapFor(final String... pairs) {
        if (null == pairs) {
            return null;
        }
        final int length = pairs.length;
        if (0 == length || (length % 2) != 0) {
            return null;
        }
        final Map<String, Object> map = new HashMap<String, Object>(length >> 1);
        for (int i = 0; i < length; i+=2) {
            map.put(pairs[i], pairs[i+1]);
        }
        return map;
    }

    /** Check for an empty string */
    private static boolean isEmpty(final String string) {
        return Strings.isEmpty(string);
    }

    private static final class FullDisplayNameComparator implements Comparator<MailFolderInfo> {

        private final Collator collator;

        FullDisplayNameComparator(final Locale locale) {
            super();
            collator = Collator.getInstance(locale);
            collator.setStrength(Collator.SECONDARY);
        }

        @Override
        public int compare(final MailFolderInfo o1, final MailFolderInfo o2) {
            /*
             * Compare by full name
             */
            return collator.compare(o1.getFullDisplayName(), o2.getFullDisplayName());
        }

    } // End of FullNameComparator

    private static String urlName(final String prefix, final String host, final int port, final String username, final String pw) {
        // Start with "protocol:"
        final StringAllocator tempURL = new StringAllocator(128);

        if (null != prefix) {
            tempURL.append(prefix);
        }

        tempURL.append("imap://");

        // Add the user:password@
        if (username != null) {
            tempURL.append(username);
            if (pw != null) {
                tempURL.append(':');
                tempURL.append(pw);
            }
            tempURL.append('@');
        }

        // Add host
        if (host != null) {
            tempURL.append(host);
        }

        // Add port
        if (port > 0) {
            tempURL.append(':');
            tempURL.append(Integer.toString(port));
        }

        return tempURL.toString();
    }

}
