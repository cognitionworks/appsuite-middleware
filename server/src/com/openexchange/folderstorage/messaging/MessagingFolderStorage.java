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

package com.openexchange.folderstorage.messaging;

import static com.openexchange.folderstorage.messaging.MessagingFolderStorageServiceRegistry.getServiceRegistry;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.Folder;
import com.openexchange.folderstorage.FolderException;
import com.openexchange.folderstorage.FolderExceptionErrorMessage;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.FolderType;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.SortableId;
import com.openexchange.folderstorage.StorageParameters;
import com.openexchange.folderstorage.StoragePriority;
import com.openexchange.folderstorage.StorageType;
import com.openexchange.folderstorage.Type;
import com.openexchange.folderstorage.messaging.contentType.DraftsContentType;
import com.openexchange.folderstorage.messaging.contentType.MessagingContentType;
import com.openexchange.folderstorage.messaging.contentType.SentContentType;
import com.openexchange.folderstorage.messaging.contentType.SpamContentType;
import com.openexchange.folderstorage.messaging.contentType.TrashContentType;
import com.openexchange.folderstorage.type.MessagingType;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.mail.MailSessionCache;
import com.openexchange.mail.MailSessionParameterNames;
import com.openexchange.mail.messaging.MailMessagingService;
import com.openexchange.mail.utils.StorageUtility;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.messaging.DefaultMessagingFolder;
import com.openexchange.messaging.DefaultMessagingPermission;
import com.openexchange.messaging.MessagingAccount;
import com.openexchange.messaging.MessagingAccountAccess;
import com.openexchange.messaging.MessagingException;
import com.openexchange.messaging.MessagingExceptionCodes;
import com.openexchange.messaging.MessagingField;
import com.openexchange.messaging.MessagingFolder;
import com.openexchange.messaging.MessagingFolderAccess;
import com.openexchange.messaging.MessagingMessage;
import com.openexchange.messaging.MessagingMessageAccess;
import com.openexchange.messaging.MessagingPermission;
import com.openexchange.messaging.MessagingService;
import com.openexchange.messaging.OrderDirection;
import com.openexchange.messaging.registry.MessagingServiceRegistry;
import com.openexchange.server.ServiceException;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * {@link MessagingFolderStorage} - The messaging folder storage.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MessagingFolderStorage implements FolderStorage {

    private static final String PARAM = MessagingParameterConstants.PARAM_MESSAGING_ACCESS;

    private static volatile boolean mailFolderStorageAvailable;

    /**
     * Sets whether mail folder storage is available.
     * 
     * @param mailFolderStorageAvailable <code>true</code> if mail folder storage is available; otherwise <code>false</code>
     */
    public static void setMailFolderStorageAvailable(final boolean mailFolderStorageAvailable) {
        MessagingFolderStorage.mailFolderStorageAvailable = mailFolderStorageAvailable;
    }

    private static final class Key {

        static Key newInstance(final int accountId, final String serviceId) {
            return new Key(accountId, serviceId);
        }

        private final int accountId;

        private final String serviceId;

        private final int hash;

        private Key(final int accountId, final String serviceId) {
            super();
            this.accountId = accountId;
            this.serviceId = serviceId;

            final int prime = 31;
            int result = 1;
            result = prime * result + accountId;
            result = prime * result + ((serviceId == null) ? 0 : serviceId.hashCode());
            hash = result;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Key)) {
                return false;
            }
            final Key other = (Key) obj;
            if (accountId != other.accountId) {
                return false;
            }
            if (serviceId == null) {
                if (other.serviceId != null) {
                    return false;
                }
            } else if (!serviceId.equals(other.serviceId)) {
                return false;
            }
            return true;
        }

    } // End of class Key

    private static final String PRIVATE_FOLDER_ID = String.valueOf(FolderObject.SYSTEM_PRIVATE_FOLDER_ID);

    /**
     * Initializes a new {@link MessagingFolderStorage}.
     */
    public MessagingFolderStorage() {
        super();
    }

    public void restore(final String treeId, final String folderId, final StorageParameters storageParameters) throws FolderException {
        // TODO:
    }

    public void checkConsistency(final String treeId, final StorageParameters storageParameters) throws FolderException {
        // Nothing to do
    }

    public SortableId[] getVisibleFolders(final String treeId, final ContentType contentType, final Type type, final StorageParameters storageParameters) throws FolderException {
        throw new UnsupportedOperationException("VirtualFolderStorage.getVisibleSubfolders()");
    }

    private MessagingAccountAccess getMessagingAccessForAccount(final String serviceId, final int accountId, final Session session, final ConcurrentMap<Key, MessagingAccountAccess> accesses) throws FolderException {
        final Key key = Key.newInstance(accountId, serviceId);
        MessagingAccountAccess accountAccess = accesses.get(key);
        if (null == accountAccess) {
            try {
                accountAccess =
                    getServiceRegistry().getService(MessagingServiceRegistry.class, true).getMessagingService(serviceId).getAccountAccess(
                        accountId,
                        session);
            } catch (final MessagingException e) {
                throw new FolderException(e);
            } catch (final ServiceException e) {
                throw new FolderException(e);
            }
            final MessagingAccountAccess prev = accesses.putIfAbsent(key, accountAccess);
            if (null != prev) {
                accountAccess = prev;
            }
        }
        return accountAccess;
    }

    private void openMessagingAccess(final MessagingAccountAccess accountAccess) throws MessagingException {
        if (!accountAccess.isConnected()) {
            try {
                accountAccess.connect();
            } catch (final MessagingException e) {
                throw e;
            }
        }
    }

    public ContentType[] getSupportedContentTypes() {
        return new ContentType[] {
            MessagingContentType.getInstance(), DraftsContentType.getInstance(), SentContentType.getInstance(),
            SpamContentType.getInstance(), TrashContentType.getInstance() };
    }

    public ContentType getDefaultContentType() {
        return MessagingContentType.getInstance();
    }

    public void commitTransaction(final StorageParameters params) throws FolderException {
        @SuppressWarnings("unchecked") final ConcurrentMap<Key, MessagingAccountAccess> accesses =
            (ConcurrentMap<Key, MessagingAccountAccess>) params.getParameter(
                MessagingFolderType.getInstance(),
                PARAM);
        if (null != accesses) {
            try {
                final Collection<MessagingAccountAccess> values = accesses.values();
                for (final MessagingAccountAccess messagingAccess : values) {
                    messagingAccess.close();
                }
            } finally {
                params.putParameter(MessagingFolderType.getInstance(), PARAM, null);
            }
        }
    }

    public void createFolder(final Folder folder, final StorageParameters storageParameters) throws FolderException {
        try {
            @SuppressWarnings("unchecked") final ConcurrentMap<Key, MessagingAccountAccess> accesses =
                (ConcurrentMap<Key, MessagingAccountAccess>) storageParameters.getParameter(
                    MessagingFolderType.getInstance(),
                    PARAM);
            if (null == accesses) {
                throw FolderExceptionErrorMessage.MISSING_PARAMETER.create(PARAM);
            }

            final MessagingFolderIdentifier mfi = new MessagingFolderIdentifier(folder.getParentID());
            final String serviceId = mfi.getServiceId();
            final int accountId = mfi.getAccountId();
            final MessagingAccountAccess accountAccess =
                getMessagingAccessForAccount(serviceId, accountId, storageParameters.getSession(), accesses);
            openMessagingAccess(accountAccess);

            final DefaultMessagingFolder dmf = new DefaultMessagingFolder();
            dmf.setExists(false);
            dmf.setParentId(mfi.getFullname());
            // Other
            dmf.setName(folder.getName());
            dmf.setSubscribed(folder.isSubscribed());
            // Permissions
            final Permission[] permissions = folder.getPermissions();
            if (null != permissions && permissions.length > 0) {
                final MessagingPermission[] messagingPermissions = new MessagingPermission[permissions.length];
                final Session session = storageParameters.getSession();
                if (null == session) {
                    throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
                }
                for (int i = 0; i < permissions.length; i++) {
                    final Permission permission = permissions[i];
                    final MessagingPermission dmp = DefaultMessagingPermission.newInstance();
                    dmp.setEntity(permission.getEntity());
                    dmp.setAllPermissions(
                        permission.getFolderPermission(),
                        permission.getReadPermission(),
                        permission.getWritePermission(),
                        permission.getDeletePermission());
                    dmp.setAdmin(permission.isAdmin());
                    dmp.setGroup(permission.isGroup());
                    messagingPermissions[i] = dmp;
                }
                dmf.setPermissions(Arrays.asList(messagingPermissions));
            }

            final String fullname = accountAccess.getFolderAccess().createFolder(dmf);
            folder.setID(new MessagingFolderIdentifier(serviceId, accountId, fullname).toString());
        } catch (final MessagingException e) {
            throw new FolderException(e);
        }
    }

    public void clearFolder(final String treeId, final String folderId, final StorageParameters storageParameters) throws FolderException {
        try {
            @SuppressWarnings("unchecked") final ConcurrentMap<Key, MessagingAccountAccess> accesses =
                (ConcurrentMap<Key, MessagingAccountAccess>) storageParameters.getParameter(
                    MessagingFolderType.getInstance(),
                    PARAM);
            if (null == accesses) {
                throw FolderExceptionErrorMessage.MISSING_PARAMETER.create(PARAM);
            }

            final MessagingFolderIdentifier mfi = new MessagingFolderIdentifier(folderId);
            final MessagingAccountAccess accountAccess =
                getMessagingAccessForAccount(mfi.getServiceId(), mfi.getAccountId(), storageParameters.getSession(), accesses);
            openMessagingAccess(accountAccess);

            final String fullname = mfi.getFullname();
            /*
             * Only backup if fullname does not denote trash (sub)folder
             */
            final MessagingFolderAccess folderAccess = accountAccess.getFolderAccess();
            final String trashFolder = folderAccess.getTrashFolder();
            folderAccess.clearFolder(fullname, (null != trashFolder && fullname.startsWith(trashFolder)));
        } catch (final MessagingException e) {
            throw new FolderException(e);
        }
    }

    public void deleteFolder(final String treeId, final String folderId, final StorageParameters storageParameters) throws FolderException {
        try {
            @SuppressWarnings("unchecked") final ConcurrentMap<Key, MessagingAccountAccess> accesses =
                (ConcurrentMap<Key, MessagingAccountAccess>) storageParameters.getParameter(
                    MessagingFolderType.getInstance(),
                    PARAM);
            if (null == accesses) {
                throw FolderExceptionErrorMessage.MISSING_PARAMETER.create(PARAM);
            }

            final MessagingFolderIdentifier mfi = new MessagingFolderIdentifier(folderId);
            final MessagingAccountAccess accountAccess =
                getMessagingAccessForAccount(mfi.getServiceId(), mfi.getAccountId(), storageParameters.getSession(), accesses);
            openMessagingAccess(accountAccess);

            final String fullname = mfi.getFullname();
            /*
             * Only backup if fullname does not denote trash (sub)folder
             */
            final MessagingFolderAccess folderAccess = accountAccess.getFolderAccess();
            final String trashFolder = folderAccess.getTrashFolder();
            folderAccess.deleteFolder(fullname, (null != trashFolder && fullname.startsWith(trashFolder)));
        } catch (final MessagingException e) {
            throw new FolderException(e);
        }
    }

    public String getDefaultFolderID(final User user, final String treeId, final ContentType contentType, final Type type, final StorageParameters storageParameters) throws FolderException {
        if (!(contentType instanceof MessagingContentType)) {
            throw FolderExceptionErrorMessage.UNKNOWN_CONTENT_TYPE.create(contentType.toString());
        }

        final String mailServiceId = MailMessagingService.ID;
        final int primaryAccountId = MailAccount.DEFAULT_ID;
        if (MessagingContentType.getInstance().equals(contentType)) {
            // Return primary account's INBOX folder
            return MessagingFolderIdentifier.getFQN(mailServiceId, primaryAccountId, "INBOX");
        }
        try {
            @SuppressWarnings("unchecked") final ConcurrentMap<Key, MessagingAccountAccess> accesses =
                (ConcurrentMap<Key, MessagingAccountAccess>) storageParameters.getParameter(
                    MessagingFolderType.getInstance(),
                    PARAM);
            if (null == accesses) {
                throw FolderExceptionErrorMessage.MISSING_PARAMETER.create(PARAM);
            }
            /*
             * Open account access
             */
            final MessagingAccountAccess accountAccess =
                getMessagingAccessForAccount(mailServiceId, primaryAccountId, storageParameters.getSession(), accesses);
            openMessagingAccess(accountAccess);
            // Return primary account's default folder
            if (DraftsContentType.getInstance().equals(contentType)) {
                return MessagingFolderIdentifier.getFQN(mailServiceId, primaryAccountId, accountAccess.getFolderAccess().getDraftsFolder());
            }
            if (SentContentType.getInstance().equals(contentType)) {
                return MessagingFolderIdentifier.getFQN(mailServiceId, primaryAccountId, accountAccess.getFolderAccess().getSentFolder());
            }
            if (SpamContentType.getInstance().equals(contentType)) {
                return MessagingFolderIdentifier.getFQN(mailServiceId, primaryAccountId, accountAccess.getFolderAccess().getSpamFolder());
            }
            if (TrashContentType.getInstance().equals(contentType)) {
                return MessagingFolderIdentifier.getFQN(mailServiceId, primaryAccountId, accountAccess.getFolderAccess().getTrashFolder());
            }
            throw FolderExceptionErrorMessage.UNKNOWN_CONTENT_TYPE.create(contentType.toString());
        } catch (final MessagingException e) {
            throw new FolderException(e);
        }
    }

    public Type getTypeByParent(final User user, final String treeId, final String parentId, final StorageParameters storageParameters) throws FolderException {
        return MessagingType.getInstance();
    }

    public boolean containsForeignObjects(final User user, final String treeId, final String folderId, final StorageParameters storageParameters) throws FolderException {
        try {
            @SuppressWarnings("unchecked") final ConcurrentMap<Key, MessagingAccountAccess> accesses =
                (ConcurrentMap<Key, MessagingAccountAccess>) storageParameters.getParameter(
                    MessagingFolderType.getInstance(),
                    PARAM);
            if (null == accesses) {
                throw FolderExceptionErrorMessage.MISSING_PARAMETER.create(PARAM);
            }

            final MessagingFolderIdentifier mfi = new MessagingFolderIdentifier(folderId);
            final String serviceId = mfi.getServiceId();
            final int accountId = mfi.getAccountId();
            final String fullname = mfi.getFullname();

            final MessagingAccountAccess accountAccess =
                getMessagingAccessForAccount(serviceId, accountId, storageParameters.getSession(), accesses);

            if (!MessagingFolder.ROOT_FULLNAME.equals(fullname)) {
                openMessagingAccess(accountAccess);
                if (!accountAccess.getFolderAccess().exists(fullname)) {
                    throw MessagingExceptionCodes.FOLDER_NOT_FOUND.create(
                        fullname,
                        Integer.valueOf(accountId),
                        serviceId,
                        Integer.valueOf(storageParameters.getUserId()),
                        Integer.valueOf(storageParameters.getContextId()));
                }
            }
            return false;
        } catch (final MessagingException e) {
            throw new FolderException(e);
        }
    }

    public boolean isEmpty(final String treeId, final String folderId, final StorageParameters storageParameters) throws FolderException {
        try {
            @SuppressWarnings("unchecked") final ConcurrentMap<Key, MessagingAccountAccess> accesses =
                (ConcurrentMap<Key, MessagingAccountAccess>) storageParameters.getParameter(
                    MessagingFolderType.getInstance(),
                    PARAM);
            if (null == accesses) {
                throw FolderExceptionErrorMessage.MISSING_PARAMETER.create(PARAM);
            }

            final MessagingFolderIdentifier mfi = new MessagingFolderIdentifier(folderId);
            final String serviceId = mfi.getServiceId();
            final int accountId = mfi.getAccountId();
            final String fullname = mfi.getFullname();

            final MessagingAccountAccess accountAccess =
                getMessagingAccessForAccount(serviceId, accountId, storageParameters.getSession(), accesses);

            if (MessagingFolder.ROOT_FULLNAME.equals(fullname)) {
                return 0 == accountAccess.getRootFolder().getMessageCount();
            }
            /*
             * Non-root folder
             */
            openMessagingAccess(accountAccess);
            return 0 == accountAccess.getFolderAccess().getFolder(fullname).getMessageCount();
        } catch (final MessagingException e) {
            throw new FolderException(e);
        }
    }

    public void updateLastModified(final long lastModified, final String treeId, final String folderId, final StorageParameters storageParameters) throws FolderException {
        // Nothing to do
    }

    public List<Folder> getFolders(final String treeId, final List<String> folderIds, final StorageParameters storageParameters) throws FolderException {
        return getFolders(treeId, folderIds, StorageType.WORKING, storageParameters);
    }

    public List<Folder> getFolders(final String treeId, final List<String> folderIds, final StorageType storageType, final StorageParameters storageParameters) throws FolderException {
        final List<Folder> ret = new ArrayList<Folder>(folderIds.size());
        for (final String folderId : folderIds) {
            ret.add(getFolder(treeId, folderId, storageType, storageParameters));
        }
        return ret;
    }

    public Folder getFolder(final String treeId, final String folderId, final StorageParameters storageParameters) throws FolderException {
        return getFolder(treeId, folderId, StorageType.WORKING, storageParameters);
    }

    public Folder getFolder(final String treeId, final String folderId, final StorageType storageType, final StorageParameters storageParameters) throws FolderException {
        if (StorageType.BACKUP.equals(storageType)) {
            throw FolderExceptionErrorMessage.UNSUPPORTED_STORAGE_TYPE.create(storageType);
        }
        try {
            @SuppressWarnings("unchecked") final ConcurrentMap<Key, MessagingAccountAccess> accesses =
                (ConcurrentMap<Key, MessagingAccountAccess>) storageParameters.getParameter(
                    MessagingFolderType.getInstance(),
                    PARAM);
            if (null == accesses) {
                throw FolderExceptionErrorMessage.MISSING_PARAMETER.create(PARAM);
            }

            final MessagingFolderIdentifier mfi = new MessagingFolderIdentifier(folderId);
            final String serviceId = mfi.getServiceId();
            final int accountId = mfi.getAccountId();

            final String fullname = mfi.getFullname();
            final Session session = storageParameters.getSession();

            final Folder retval;
            final boolean hasSubfolders;
            if ("".equals(fullname)) {
                final MessagingServiceRegistry msr =
                    MessagingFolderStorageServiceRegistry.getServiceRegistry().getService(MessagingServiceRegistry.class, true);
                final MessagingService messagingService = msr.getMessagingService(serviceId);
                final MessagingAccount messagingAccount =
                    messagingService.getAccountManager().getAccount(accountId, session);

                if ("com.openexchange.messaging.rss".equals(serviceId)) {
                    retval = new ExternalMessagingAccountRootFolder(messagingAccount, serviceId, session);
                    hasSubfolders = false;
                } else if ("com.openexchange.messaging.facebook".equals(serviceId)) {
                    retval = new ExternalMessagingAccountRootFolder(messagingAccount, serviceId, session);
                    hasSubfolders = true;
                } else {
                    final MessagingAccountAccess accountAccess =
                        getMessagingAccessForAccount(serviceId, accountId, session, accesses);
                    openMessagingAccess(accountAccess);

                    final MessagingFolder rootFolder = accountAccess.getFolderAccess().getRootFolder();
                    retval = new MessagingFolderImpl(rootFolder, accountId, serviceId, null);
                    /*
                     * Set proper name
                     */
                    retval.setName(messagingAccount.getDisplayName());
                    hasSubfolders = rootFolder.hasSubfolders();
                }
                /*
                 * This one needs sorting. Just pass null or an empty array.
                 */
                retval.setSubfolderIDs(hasSubfolders ? null : new String[0]);
            } else {
                final MessagingAccountAccess accountAccess =
                    getMessagingAccessForAccount(serviceId, accountId, session, accesses);
                openMessagingAccess(accountAccess);
                final MessagingFolder messagingFolder = accountAccess.getFolderAccess().getFolder(fullname);
                retval =
                    new MessagingFolderImpl(
                        messagingFolder,
                        accountId,
                        serviceId,
                        new MessagingAccountAccessFullnameProvider(accountAccess));
                hasSubfolders = messagingFolder.hasSubfolders();
                /*
                 * Check if denoted parent can hold default folders like Trash, Sent, etc.
                 */
                if ("INBOX".equals(fullname)) {
                    /*
                     * This one needs sorting. Just pass null or an empty array.
                     */
                    retval.setSubfolderIDs(hasSubfolders ? null : new String[0]);
                } else {
                    /*
                     * Denoted parent is not capable to hold default folders. Therefore output as it is.
                     */
                    final List<MessagingFolder> children = Arrays.asList(accountAccess.getFolderAccess().getSubfolders(fullname, true));
                    Collections.sort(children, new SimpleMessagingFolderComparator(storageParameters.getUser().getLocale()));
                    final String[] subfolderIds = new String[children.size()];
                    int i = 0;
                    for (final MessagingFolder child : children) {
                        subfolderIds[i++] = MessagingFolderIdentifier.getFQN(serviceId, accountId, child.getId());
                    }
                    retval.setSubfolderIDs(subfolderIds);
                }
            }
            retval.setTreeID(treeId);

            return retval;
        } catch (final MessagingException e) {
            throw new FolderException(e);
        } catch (final ServiceException e) {
            throw new FolderException(e);
        }
    }

    private boolean isDefaultFoldersChecked(final int accountId, final Session session) {
        final Boolean b =
            MailSessionCache.getInstance(session).getParameter(accountId, MailSessionParameterNames.getParamDefaultFolderChecked());
        return (b != null) && b.booleanValue();
    }

    private String[] getSortedDefaultMessagingFolders(final int accountId, final Session session) {
        final String[] arr =
            MailSessionCache.getInstance(session).getParameter(accountId, MailSessionParameterNames.getParamDefaultFolderArray());
        if (arr == null) {
            return new String[0];
        }
        return new String[] {
            "INBOX", arr[StorageUtility.INDEX_DRAFTS], arr[StorageUtility.INDEX_SENT], arr[StorageUtility.INDEX_SPAM],
            arr[StorageUtility.INDEX_TRASH] };
    }

    public FolderType getFolderType() {
        return MessagingFolderType.getInstance();
    }

    public SortableId[] getSubfolders(final String treeId, final String parentId, final StorageParameters storageParameters) throws FolderException {
        try {
            final ServerSession session;
            {
                final Session s = storageParameters.getSession();
                if (null == s) {
                    throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
                }
                if (s instanceof ServerSession) {
                    session = (ServerSession) s;
                } else {
                    session = new ServerSessionAdapter(s);
                }
            }

            if (PRIVATE_FOLDER_ID.equals(parentId)) {
                /*
                 * Get all user messaging accounts
                 */
                final List<MessagingAccount> accounts = new ArrayList<MessagingAccount>(8);
                {
                    final MessagingServiceRegistry registry = getServiceRegistry().getService(MessagingServiceRegistry.class, true);
                    final List<MessagingService> allServices = registry.getAllServices();
                    final boolean available = mailFolderStorageAvailable;
                    final String mailMessagingServiceId = MailMessagingService.ID;
                    for (final MessagingService messagingService : allServices) {
                        /*
                         * Check if messaging service is mail
                         */
                        if (!available || !mailMessagingServiceId.equals(messagingService.getId())) {
                            final List<MessagingAccount> userAccounts = messagingService.getAccountManager().getAccounts(session);
                            for (final MessagingAccount userAccount : userAccounts) {
                                accounts.add(userAccount);
                            }
                        }
                    }
                }
                if (accounts.isEmpty()) {
                    return new SortableId[0];
                }
                final int size = accounts.size();
                if (size > 1) {
                    /*
                     * Sort by name
                     */
                    Collections.sort(accounts, new MessagingAccountComparator(session.getUser().getLocale()));
                }
                /*-
                 * TODO:
                 * 1. Check for messaging permission; e.g. session.getUserConfiguration().isMultipleMailAccounts()
                 *    Add primary only if not enabled
                 * 2. Strip Unified-Messaging account from obtained list
                 */
                final List<SortableId> list = new ArrayList<SortableId>(size);
                for (int j = 0; j < size; j++) {
                    final MessagingAccount acc = accounts.get(j);
                    list.add(new MessagingId(MessagingFolderIdentifier.getFQN(acc.getMessagingService().getId(), acc.getId(), ""), j, null));
                }
                return list.toArray(new SortableId[list.size()]);
            }

            // A messaging folder denoted by fullname
            @SuppressWarnings("unchecked") final ConcurrentMap<Key, MessagingAccountAccess> accesses =
                (ConcurrentMap<Key, MessagingAccountAccess>) storageParameters.getParameter(
                    MessagingFolderType.getInstance(),
                    PARAM);
            if (null == accesses) {
                throw FolderExceptionErrorMessage.MISSING_PARAMETER.create(PARAM);
            }

            final MessagingFolderIdentifier mfi = new MessagingFolderIdentifier(parentId);
            final String serviceId = mfi.getServiceId();
            final int accountId = mfi.getAccountId();
            final MessagingAccountAccess accountAccess =
                getMessagingAccessForAccount(serviceId, accountId, storageParameters.getSession(), accesses);
            openMessagingAccess(accountAccess);

            final String fullname = mfi.getFullname();

            final List<MessagingFolder> children = Arrays.asList(accountAccess.getFolderAccess().getSubfolders(fullname, true));
            /*
             * Check if denoted parent can hold default folders like Trash, Sent, etc.
             */
            if (!"".equals(fullname) && !"INBOX".equals(fullname)) {
                /*
                 * Denoted parent is not capable to hold default folders. Therefore output as it is.
                 */
                Collections.sort(children, new SimpleMessagingFolderComparator(storageParameters.getUser().getLocale()));
            } else {
                /*
                 * Ensure default folders are at first positions
                 */
                final String[] names;
                if (isDefaultFoldersChecked(accountId, storageParameters.getSession())) {
                    names = getSortedDefaultMessagingFolders(accountId, storageParameters.getSession());
                } else {
                    final List<String> tmp = new ArrayList<String>();
                    tmp.add("INBOX");

                    final MessagingFolderAccess folderAccess = accountAccess.getFolderAccess();
                    String fn = folderAccess.getDraftsFolder();
                    if (null != fn) {
                        tmp.add(fn);
                    }

                    fn = folderAccess.getSentFolder();
                    if (null != fn) {
                        tmp.add(fn);
                    }

                    fn = folderAccess.getSpamFolder();
                    if (null != fn) {
                        tmp.add(fn);
                    }

                    fn = folderAccess.getTrashFolder();
                    if (null != fn) {
                        tmp.add(fn);
                    }

                    names = tmp.toArray(new String[tmp.size()]);
                }
                /*
                 * Sort them
                 */
                Collections.sort(children, new MessagingFolderComparator(names, storageParameters.getUser().getLocale()));
            }

            final List<SortableId> list = new ArrayList<SortableId>(children.size());
            final int size = children.size();
            for (int j = 0; j < size; j++) {
                final MessagingFolder cur = children.get(j);
                list.add(new MessagingId(MessagingFolderIdentifier.getFQN(serviceId, accountId, cur.getId()), j, cur.getName()));
            }
            return list.toArray(new SortableId[list.size()]);
        } catch (final MessagingException e) {
            throw new FolderException(e);
        } catch (final ContextException e) {
            throw new FolderException(e);
        } catch (final ServiceException e) {
            throw new FolderException(e);
        }
    }

    public void rollback(final StorageParameters params) {
        @SuppressWarnings("unchecked") final ConcurrentMap<Key, MessagingAccountAccess> accesses =
            (ConcurrentMap<Key, MessagingAccountAccess>) params.getParameter(
                MessagingFolderType.getInstance(),
                PARAM);
        if (null != accesses) {
            try {
                final Collection<MessagingAccountAccess> values = accesses.values();
                for (final MessagingAccountAccess access : values) {
                    access.close();
                }
            } finally {
                params.putParameter(MessagingFolderType.getInstance(), PARAM, null);
            }
        }
    }

    public boolean startTransaction(final StorageParameters parameters, final boolean modify) throws FolderException {
        /*
         * Ensure session is present
         */
        if (null == parameters.getSession()) {
            throw FolderExceptionErrorMessage.MISSING_SESSION.create();
        }
        /*
         * Put map
         */
        return parameters.putParameterIfAbsent(
            MessagingFolderType.getInstance(),
            PARAM,
            new ConcurrentHashMap<Key, MessagingAccountAccess>());
    }

    public StoragePriority getStoragePriority() {
        return StoragePriority.NORMAL;
    }

    public boolean containsFolder(final String treeId, final String folderId, final StorageParameters storageParameters) throws FolderException {
        return containsFolder(treeId, folderId, StorageType.WORKING, storageParameters);
    }

    public boolean containsFolder(final String treeId, final String folderId, final StorageType storageType, final StorageParameters storageParameters) throws FolderException {
        if (StorageType.BACKUP.equals(storageType)) {
            return false;
        }
        try {
            @SuppressWarnings("unchecked") final ConcurrentMap<Key, MessagingAccountAccess> accesses =
                (ConcurrentMap<Key, MessagingAccountAccess>) storageParameters.getParameter(
                    MessagingFolderType.getInstance(),
                    PARAM);
            if (null == accesses) {
                throw FolderExceptionErrorMessage.MISSING_PARAMETER.create(PARAM);
            }

            final MessagingFolderIdentifier mfi = new MessagingFolderIdentifier(folderId);
            final String serviceId = mfi.getServiceId();
            final int accountId = mfi.getAccountId();
            final MessagingAccountAccess accountAccess =
                getMessagingAccessForAccount(serviceId, accountId, storageParameters.getSession(), accesses);
            openMessagingAccess(accountAccess);

            return accountAccess.getFolderAccess().exists(mfi.getFullname());
        } catch (final MessagingException e) {
            throw new FolderException(e);
        }
    }

    public String[] getDeletedFolderIDs(final String treeId, final Date timeStamp, final StorageParameters storageParameters) throws FolderException {
        return new String[0];
    }

    public String[] getModifiedFolderIDs(final String treeId, final Date timeStamp, final ContentType[] includeContentTypes, final StorageParameters storageParameters) throws FolderException {
        if (null == includeContentTypes || includeContentTypes.length == 0) {
            return new String[0];
        }
        final List<String> ret = new ArrayList<String>();
        final Set<ContentType> supported = new HashSet<ContentType>(Arrays.asList(getSupportedContentTypes()));
        for (final ContentType includeContentType : includeContentTypes) {
            if (supported.contains(includeContentType)) {
                final SortableId[] subfolders = getSubfolders(FolderStorage.REAL_TREE_ID, PRIVATE_FOLDER_ID, storageParameters);
                for (final SortableId sortableId : subfolders) {
                    ret.add(sortableId.getId());
                }
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

    public void updateFolder(final Folder folder, final StorageParameters storageParameters) throws FolderException {
        try {
            @SuppressWarnings("unchecked") final ConcurrentMap<Key, MessagingAccountAccess> accesses =
                (ConcurrentMap<Key, MessagingAccountAccess>) storageParameters.getParameter(
                    MessagingFolderType.getInstance(),
                    PARAM);
            if (null == accesses) {
                throw FolderExceptionErrorMessage.MISSING_PARAMETER.create(PARAM);
            }

            final MessagingFolderIdentifier mfi = new MessagingFolderIdentifier(folder.getID());
            final String serviceId = mfi.getServiceId();
            final int accountId = mfi.getAccountId();
            String id = mfi.getFullname();
            final MessagingAccountAccess accountAccess =
                getMessagingAccessForAccount(serviceId, accountId, storageParameters.getSession(), accesses);
            openMessagingAccess(accountAccess);

            final DefaultMessagingFolder dmf = new DefaultMessagingFolder();
            dmf.setExists(true);
            // Fullname
            dmf.setId(id);
            // TODO: dmf.setAccountId(accountId);
            // Parent
            final MessagingFolderIdentifier pfi;
            if (null != folder.getParentID()) {
                pfi = new MessagingFolderIdentifier(folder.getParentID());
                dmf.setParentId(pfi.getFullname());
                // TODO: dmf.setParentAccountId(parentArg.getAccountId());
            } else {
                pfi = null;
            }
            // Name
            if (null != folder.getName()) {
                dmf.setName(folder.getName());
            }
            // Subscribed
            dmf.setSubscribed(folder.isSubscribed());
            // Permissions
            final Permission[] permissions = folder.getPermissions();
            if (null != permissions && permissions.length > 0) {
                final MessagingPermission[] messagingPermissions = new MessagingPermission[permissions.length];
                final Session session = storageParameters.getSession();
                if (null == session) {
                    throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
                }
                for (int i = 0; i < permissions.length; i++) {
                    final Permission permission = permissions[i];
                    final MessagingPermission dmp = DefaultMessagingPermission.newInstance();
                    dmp.setEntity(permission.getEntity());
                    dmp.setAllPermissions(
                        permission.getFolderPermission(),
                        permission.getReadPermission(),
                        permission.getWritePermission(),
                        permission.getDeletePermission());
                    dmp.setAdmin(permission.isAdmin());
                    dmp.setGroup(permission.isGroup());
                    messagingPermissions[i] = dmp;
                }
                dmf.setPermissions(Arrays.asList(messagingPermissions));
            }
            /*
             * Load storage version
             */
            final String oldParent;
            final String oldName;
            {
                final MessagingFolder storageVersion = accountAccess.getFolderAccess().getFolder(id);
                oldParent = storageVersion.getParentId();
                oldName = storageVersion.getName();
                /*
                 * Set separator
                 */
                dmf.setSeparator(storageVersion.getSeparator());
            }

            // Here we go------------------------------------------------------------------------
            // TODO: Allow differing service identifiers in provided parent ID?

            final String newName = dmf.getName();

            boolean movePerformed = false;
            {
                /*
                 * Check if a move shall be performed
                 */
                final String newParent = dmf.getParentId();
                if (newParent != null) {
                    final int parentAccountID = pfi.getAccountId();
                    if (accountId == parentAccountID) {
                        /*
                         * Move to another parent in the same account
                         */
                        if (!newParent.equals(oldParent)) {
                            /*
                             * Check for possible duplicate folder
                             */
                            final boolean rename = (null != newName) && !newName.equals(oldName);
                            check4DuplicateFolder(accountAccess, newParent, rename ? newName : oldName);
                            /*
                             * Perform move operation
                             */
                            String movedFolder = accountAccess.getFolderAccess().moveFolder(id, newParent);
                            if (rename) {
                                /*
                                 * Perform rename
                                 */
                                movedFolder = accountAccess.getFolderAccess().renameFolder(movedFolder, newName);
                            }
                            folder.setID(MessagingFolderIdentifier.getFQN(serviceId, accountId, movedFolder));
                            movePerformed = true;
                        }
                    } else {
                        // Move to another account
                        final MessagingAccountAccess otherAccess =
                            getMessagingAccessForAccount(serviceId, parentAccountID, storageParameters.getSession(), accesses);
                        openMessagingAccess(otherAccess);
                        try {
                            // Check if parent messaging folder exists
                            final MessagingFolder p = otherAccess.getFolderAccess().getFolder(newParent);
                            // Check permission on new parent
                            final MessagingPermission ownPermission = p.getOwnPermission();
                            if (ownPermission.getFolderPermission() < MessagingPermission.CREATE_SUB_FOLDERS) {
                                throw MessagingExceptionCodes.NO_CREATE_ACCESS.create(newParent);
                            }
                            // Check for duplicate
                            check4DuplicateFolder(otherAccess, newParent, null == newName ? oldName : newName);
                            // Copy
                            final String destFullname =
                                fullCopy(
                                    accountAccess,
                                    id,
                                    otherAccess,
                                    newParent,
                                    p.getSeparator(),
                                    storageParameters.getUserId(),
                                    p.getCapabilities().contains(MessagingFolder.CAPABILITY_PERMISSIONS));
                            // Delete source
                            accountAccess.getFolderAccess().deleteFolder(id, true);
                            // Perform other updates
                            otherAccess.getFolderAccess().updateFolder(destFullname, dmf);
                        } finally {
                            otherAccess.close();
                        }
                    }
                }
            }
            /*
             * Check if a rename shall be performed
             */
            if (!movePerformed && newName != null && !newName.equals(oldName)) {
                id = accountAccess.getFolderAccess().renameFolder(id, newName);
                folder.setID(MessagingFolderIdentifier.getFQN(serviceId, accountId, id));
            }
            /*
             * Handle update of permission or subscription
             */
            accountAccess.getFolderAccess().updateFolder(id, dmf);
        } catch (final MessagingException e) {
            throw new FolderException(e);
        }
    }

    private void check4DuplicateFolder(final MessagingAccountAccess accountAccess, final String parentId, final String name2check) throws MessagingException {
        final MessagingFolder[] subfolders = accountAccess.getFolderAccess().getSubfolders(parentId, true);
        for (final MessagingFolder subfolder : subfolders) {
            if (name2check.equals(subfolder.getName())) {
                throw MessagingExceptionCodes.DUPLICATE_FOLDER.create(name2check, parentId);
            }
        }
    }

    private static String fullCopy(final MessagingAccountAccess srcAccess, final String srcFullname, final MessagingAccountAccess destAccess, final String destParent, final char destSeparator, final int user, final boolean hasPermissions) throws MessagingException {
        // Create folder
        final MessagingFolder source = srcAccess.getFolderAccess().getFolder(srcFullname);
        final DefaultMessagingFolder mfd = new DefaultMessagingFolder();
        mfd.setName(source.getName());
        mfd.setParentId(destParent);
        mfd.setSeparator(destSeparator);
        mfd.setSubscribed(source.isSubscribed());
        if (hasPermissions) {
            // Copy permissions
            final List<MessagingPermission> perms = source.getPermissions();
            for (final MessagingPermission perm : perms) {
                mfd.addPermission((MessagingPermission) perm.clone());
            }
        }
        final String destFullname = destAccess.getFolderAccess().createFolder(mfd);
        // Copy messages
        final List<MessagingMessage> msgs =
            srcAccess.getMessageAccess().getAllMessages(
                srcFullname,
                null,
                MessagingField.RECEIVED_DATE,
                OrderDirection.ASC,
                new MessagingField[] { MessagingField.FULL });
        final MessagingMessageAccess destMessageStorage = destAccess.getMessageAccess();
        // Append messages to destination account
        /* final String[] mailIds = */destMessageStorage.appendMessages(destFullname, msgs.toArray(new MessagingMessage[msgs.size()]));
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
        final MessagingFolder[] tmp = srcAccess.getFolderAccess().getSubfolders(srcFullname, true);
        for (final MessagingFolder element : tmp) {
            fullCopy(srcAccess, element.getId(), destAccess, destFullname, destSeparator, user, hasPermissions);
        }
        return destFullname;
    }

    private static final class MessagingAccountComparator implements Comparator<MessagingAccount> {

        private final Collator collator;

        MessagingAccountComparator(final Locale locale) {
            super();
            collator = Collator.getInstance(locale);
            collator.setStrength(Collator.SECONDARY);
        }

        public int compare(final MessagingAccount o1, final MessagingAccount o2) {
            /*-
             * 
            if (UnifiedINBOXManagement.PROTOCOL_UNIFIED_INBOX.equals(o1.getMailProtocol())) {
                if (UnifiedINBOXManagement.PROTOCOL_UNIFIED_INBOX.equals(o2.getMailProtocol())) {
                    return 0;
                }
                return -1;
            } else if (UnifiedINBOXManagement.PROTOCOL_UNIFIED_INBOX.equals(o2.getMailProtocol())) {
                return 1;
            }
            if (0 == o1.getId()) {
                if (0 == o2.getId()) {
                    return 0;
                }
                return -1;
            } else if (0 == o2.getId()) {
                return 1;
            }
            */
            return collator.compare(o1.getDisplayName(), o2.getDisplayName());
        }

    } // End of MessagingAccountComparator

    private static final class SimpleMessagingFolderComparator implements Comparator<MessagingFolder> {

        private final Collator collator;

        SimpleMessagingFolderComparator(final Locale locale) {
            super();
            collator = Collator.getInstance(locale);
            collator.setStrength(Collator.SECONDARY);
        }

        public int compare(final MessagingFolder o1, final MessagingFolder o2) {
            return collator.compare(o1.getName(), o2.getName());
        }
    } // End of SimpleMessagingFolderComparator

    private static final class MessagingFolderComparator implements Comparator<MessagingFolder> {

        private final Map<String, Integer> indexMap;

        private final Collator collator;

        private final Integer na;

        MessagingFolderComparator(final String[] names, final Locale locale) {
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

        public int compare(final MessagingFolder o1, final MessagingFolder o2) {
            if (o1.isDefaultFolder()) {
                if (o2.isDefaultFolder()) {
                    return getNumberOf(o1.getId()).compareTo(getNumberOf(o2.getId()));
                }
                return -1;
            }
            if (o2.isDefaultFolder()) {
                return 1;
            }
            return collator.compare(o1.getName(), o2.getName());
        }
    } // End of MessagingFolderComparator

}
