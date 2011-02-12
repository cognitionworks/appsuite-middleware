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

package com.openexchange.folderstorage.mail;

import static com.openexchange.mail.utils.MailFolderUtility.prepareFullname;
import static com.openexchange.mail.utils.MailFolderUtility.prepareMailFolderParam;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import com.openexchange.cache.OXCachingException;
import com.openexchange.database.DBPoolingException;
import com.openexchange.databaseold.Database;
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
import com.openexchange.folderstorage.mail.contentType.DraftsContentType;
import com.openexchange.folderstorage.mail.contentType.MailContentType;
import com.openexchange.folderstorage.mail.contentType.SentContentType;
import com.openexchange.folderstorage.mail.contentType.SpamContentType;
import com.openexchange.folderstorage.mail.contentType.TrashContentType;
import com.openexchange.folderstorage.type.MailType;
import com.openexchange.folderstorage.type.PrivateType;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.i18n.MailStrings;
import com.openexchange.groupware.ldap.User;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.mail.FullnameArgument;
import com.openexchange.mail.MailException;
import com.openexchange.mail.MailField;
import com.openexchange.mail.MailProviderRegistry;
import com.openexchange.mail.MailSessionCache;
import com.openexchange.mail.MailSessionParameterNames;
import com.openexchange.mail.MailSortField;
import com.openexchange.mail.OrderDirection;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.api.MailProvider;
import com.openexchange.mail.cache.MailMessageCache;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.mail.dataobjects.MailFolderDescription;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.event.EventPool;
import com.openexchange.mail.event.PooledEvent;
import com.openexchange.mail.mime.MIMEMailException;
import com.openexchange.mail.permission.MailPermission;
import com.openexchange.mail.utils.StorageUtility;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountException;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.UnifiedINBOXManagement;
import com.openexchange.server.ServiceException;
import com.openexchange.server.osgiservice.ServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.tools.sql.DBUtils;

/**
 * {@link MailFolderStorage} - The mail folder storage.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MailFolderStorage implements FolderStorage {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(MailFolderStorage.class);

    private static final String PRIVATE_FOLDER_ID = String.valueOf(FolderObject.SYSTEM_PRIVATE_FOLDER_ID);

    /**
     * Initializes a new {@link MailFolderStorage}.
     */
    public MailFolderStorage() {
        super();
    }

    public void checkConsistency(final String treeId, final StorageParameters storageParameters) throws FolderException {
        // Nothing to do
    }

    public ContentType[] getSupportedContentTypes() {
        return new ContentType[] {
            MailContentType.getInstance(), DraftsContentType.getInstance(), SentContentType.getInstance(), SpamContentType.getInstance(),
            TrashContentType.getInstance() };
    }

    public ContentType getDefaultContentType() {
        return MailContentType.getInstance();
    }

    public void commitTransaction(final StorageParameters params) throws FolderException {
        /*
         * Nothing to do
         */
    }

    public SortableId[] getVisibleFolders(final String treeId, final ContentType contentType, final Type type, final StorageParameters storageParameters) throws FolderException {
        if (!MailType.getInstance().equals(type) && !PrivateType.getInstance().equals(type)) {
            return new SortableId[0];
        }
        if (!MailContentType.getInstance().toString().equals(contentType.toString())) {
            return new SortableId[0];
        }
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            /*
             * Only primary account folders
             */
            final int accountId = MailAccount.DEFAULT_ID;
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect(true);
            final List<MailFolder> folders = new ArrayList<MailFolder>(32);
            final MailFolder rootFolder = mailAccess.getRootFolder();
            folders.add(rootFolder);
            /*
             * Start recursive iteration 
             */
            addSubfolders(MailFolder.DEFAULT_FOLDER_ID, folders, mailAccess.getFolderStorage());
            /*
             * Sort by name
             */
            Collections.sort(folders, new SimpleMailFolderComparator(storageParameters.getUser().getLocale()));
            final int size = folders.size();
            final List<SortableId> list = new ArrayList<SortableId>(size);
            for (int j = 0; j < size; j++) {
                final MailFolder tmp = folders.get(j);
                list.add(new MailId(prepareFullname(mailAccess.getAccountId(), tmp.getFullname()), j).setName(tmp.getName()));
            }
            return list.toArray(new SortableId[list.size()]);
        } catch (final MailException e) {
            throw new FolderException(e);
        } finally {
            closeMailAccess(mailAccess);
        }
    }

    private static void addSubfolders(final String fullname, final List<MailFolder> folders, final IMailFolderStorage folderStorage) throws MailException {
        final MailFolder[] subfolders = folderStorage.getSubfolders(fullname, false);
        for (final MailFolder subfolder : subfolders) {
            folders.add(subfolder);
            addSubfolders(subfolder.getFullname(), folders, folderStorage);
        }
    }

    public void restore(final String treeId, final String folderId, final StorageParameters storageParameters) throws FolderException {
        MailAccess<?, ?> mailAccess = null;
        try {
            final FullnameArgument argument = prepareMailFolderParam(folderId);
            final int accountId = argument.getAccountId();
            final String fullname = argument.getFullname();
            if (MailFolder.DEFAULT_FOLDER_ID.equals(fullname)) {
                throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create();
            }
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect(false);
            /*
             * Restore if absent
             */
            if (!mailAccess.getFolderStorage().exists(fullname)) {
                recreateMailFolder(accountId, fullname, session, mailAccess);
            }
            addWarnings(mailAccess, storageParameters);
        } catch (final MailException e) {
            throw new FolderException(e);
        } finally {
            closeMailAccess(mailAccess);
        }
    }

    public void createFolder(final Folder folder, final StorageParameters storageParameters) throws FolderException {
        MailAccess<?, ?> mailAccess = null;
        try {
            final FullnameArgument arg = prepareMailFolderParam(folder.getParentID());
            final int accountId = arg.getAccountId();
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect(false);
            final MailFolderDescription mfd = new MailFolderDescription();
            mfd.setExists(false);
            mfd.setParentFullname(arg.getFullname());
            mfd.setParentAccountId(accountId);
            // Separator
            mfd.setSeparator(mailAccess.getFolderStorage().getFolder(arg.getFullname()).getSeparator());
            // Other
            mfd.setName(folder.getName());
            mfd.setSubscribed(folder.isSubscribed());
            // Permissions
            final Permission[] permissions = folder.getPermissions();
            if (null != permissions && permissions.length > 0) {
                final MailPermission[] mailPermissions = new MailPermission[permissions.length];
                final MailProvider provider = MailProviderRegistry.getMailProviderBySession(session, accountId);
                for (int i = 0; i < permissions.length; i++) {
                    final Permission permission = permissions[i];
                    final MailPermission mailPerm = provider.createNewMailPermission();
                    mailPerm.setEntity(permission.getEntity());
                    mailPerm.setAllPermission(
                        permission.getFolderPermission(),
                        permission.getReadPermission(),
                        permission.getWritePermission(),
                        permission.getDeletePermission());
                    mailPerm.setFolderAdmin(permission.isAdmin());
                    mailPerm.setGroupPermission(permission.isGroup());
                    mailPermissions[i] = mailPerm;
                }
                mfd.addPermissions(mailPermissions);
            }
            final String fullname = mailAccess.getFolderStorage().createFolder(mfd);
            addWarnings(mailAccess, storageParameters);
            folder.setID(prepareFullname(accountId, fullname));
            postEvent(accountId, mfd.getParentFullname(), false, storageParameters);
        } catch (final MailException e) {
            throw new FolderException(e);
        } finally {
            closeMailAccess(mailAccess);
        }
    }

    public void clearFolder(final String treeId, final String folderId, final StorageParameters storageParameters) throws FolderException {
        MailAccess<?, ?> mailAccess = null;
        try {
            final FullnameArgument arg = prepareMailFolderParam(folderId);
            final int accountId = arg.getAccountId();
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect(true);
            final String fullname = arg.getFullname();
            /*
             * Only backup if fullname does not denote trash (sub)folder
             */
            final String trashFullname = mailAccess.getFolderStorage().getTrashFolder();
            final boolean hardDelete = fullname.startsWith(trashFullname);
            mailAccess.getFolderStorage().clearFolder(fullname, hardDelete);
            addWarnings(mailAccess, storageParameters);
            postEvent(accountId, fullname, true, storageParameters);
            if (!hardDelete) {
                postEvent(accountId, trashFullname, true, storageParameters);
            }
            try {
                /*
                 * Update message cache
                 */
                MailMessageCache.getInstance().removeFolderMessages(
                    accountId,
                    fullname,
                    storageParameters.getUserId(),
                    storageParameters.getContextId());
            } catch (final OXCachingException e) {
                LOG.error(e.getMessage(), e);
            }
            if (fullname.startsWith(trashFullname)) {
                // Special handling
                final MailFolder[] subf = mailAccess.getFolderStorage().getSubfolders(fullname, true);
                for (int i = 0; i < subf.length; i++) {
                    final String subFullname = subf[i].getFullname();
                    mailAccess.getFolderStorage().deleteFolder(subFullname, true);
                    postEvent(accountId, subFullname, false, storageParameters);
                }
                postEvent(accountId, trashFullname, false, storageParameters);
            }
        } catch (final MailException e) {
            throw new FolderException(e);
        } finally {
            closeMailAccess(mailAccess);
        }
    }

    public void deleteFolder(final String treeId, final String folderId, final StorageParameters storageParameters) throws FolderException {
        MailAccess<?, ?> mailAccess = null;
        try {
            final FullnameArgument arg = prepareMailFolderParam(folderId);
            final int accountId = arg.getAccountId();
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect(true);
            final String fullname = arg.getFullname();
            /*
             * Only backup if fullname does not denote trash (sub)folder
             */
            final String trashFullname = mailAccess.getFolderStorage().getTrashFolder();
            final boolean hardDelete = fullname.startsWith(trashFullname);
            mailAccess.getFolderStorage().deleteFolder(fullname, hardDelete);
            addWarnings(mailAccess, storageParameters);
            postEvent(accountId, fullname, false, true, false, storageParameters);
            try {
                /*
                 * Update message cache
                 */
                MailMessageCache.getInstance().removeFolderMessages(
                    accountId,
                    fullname,
                    storageParameters.getUserId(),
                    storageParameters.getContextId());
            } catch (final OXCachingException e) {
                LOG.error(e.getMessage(), e);
            }
            if (!hardDelete) {
                // New folder in trash folder
                postEvent(accountId, trashFullname, false, storageParameters);
            }
            final Map<String, Map<?, ?>> subfolders = subfolders(fullname, mailAccess);
            postEvent4Subfolders(accountId, subfolders, storageParameters);
        } catch (final MailException e) {
            throw new FolderException(e);
        } finally {
            closeMailAccess(mailAccess);
        }
    }

    public String getDefaultFolderID(final User user, final String treeId, final ContentType contentType, final Type type, final StorageParameters storageParameters) throws FolderException {
        if (!(contentType instanceof MailContentType)) {
            throw FolderExceptionErrorMessage.UNKNOWN_CONTENT_TYPE.create(contentType.toString());
        }
        if (MailContentType.getInstance().equals(contentType)) {
            return prepareFullname(MailAccount.DEFAULT_ID, "INBOX");
        }
        MailAccess<?, ?> mailAccess = null;
        try {
            /*
             * Open mail access
             */
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }
            mailAccess = MailAccess.getInstance(session, 0);
            mailAccess.connect(true);
            // Return primary account's default folder
            if (DraftsContentType.getInstance().equals(contentType)) {
                return prepareFullname(MailAccount.DEFAULT_ID, mailAccess.getFolderStorage().getDraftsFolder());
            }
            if (SentContentType.getInstance().equals(contentType)) {
                return prepareFullname(MailAccount.DEFAULT_ID, mailAccess.getFolderStorage().getSentFolder());
            }
            if (SpamContentType.getInstance().equals(contentType)) {
                return prepareFullname(MailAccount.DEFAULT_ID, mailAccess.getFolderStorage().getSpamFolder());
            }
            if (TrashContentType.getInstance().equals(contentType)) {
                return prepareFullname(MailAccount.DEFAULT_ID, mailAccess.getFolderStorage().getTrashFolder());
            }
            throw FolderExceptionErrorMessage.UNKNOWN_CONTENT_TYPE.create(contentType.toString());
        } catch (final MailException e) {
            throw new FolderException(e);
        } finally {
            closeMailAccess(mailAccess);
        }
    }

    public Type getTypeByParent(final User user, final String treeId, final String parentId, final StorageParameters storageParameters) throws FolderException {
        return MailType.getInstance();
    }

    public boolean containsForeignObjects(final User user, final String treeId, final String folderId, final StorageParameters storageParameters) throws FolderException {
        MailAccess<?, ?> mailAccess = null;
        try {
            final FullnameArgument argument = prepareMailFolderParam(folderId);
            final int accountId = argument.getAccountId();
            final String fullname = argument.getFullname();
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect(false);
            if (!MailFolder.DEFAULT_FOLDER_ID.equals(fullname) && !mailAccess.getFolderStorage().exists(fullname)) {
                throw new MailException(MailException.Code.FOLDER_NOT_FOUND, fullname);
            }
            addWarnings(mailAccess, storageParameters);
            return false;
        } catch (final MailException e) {
            throw new FolderException(e);
        } finally {
            closeMailAccess(mailAccess);
        }
    }

    public boolean isEmpty(final String treeId, final String folderId, final StorageParameters storageParameters) throws FolderException {
        MailAccess<?, ?> mailAccess = null;
        try {
            final FullnameArgument argument = prepareMailFolderParam(folderId);
            final int accountId = argument.getAccountId();
            final String fullname = argument.getFullname();
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }
            mailAccess = MailAccess.getInstance(session, accountId);
            if (MailFolder.DEFAULT_FOLDER_ID.equals(fullname)) {
                return 0 == mailAccess.getRootFolder().getMessageCount();
            }
            /*
             * Non-root folder
             */
            mailAccess.connect(false);
            return 0 == mailAccess.getFolderStorage().getFolder(fullname).getMessageCount();
        } catch (final MailException e) {
            throw new FolderException(e);
        } finally {
            closeMailAccess(mailAccess);
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
        MailAccess<?, ?> mailAccess = null;
        try {
            final FullnameArgument argument = prepareMailFolderParam(folderId);
            final int accountId = argument.getAccountId();
            final String fullname = argument.getFullname();
            final ServerSession session = getServerSession(storageParameters);
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }

            final MailAccount mailAccount;
            {
                final MailAccountStorageService storageService =
                    MailServiceRegistry.getServiceRegistry().getService(MailAccountStorageService.class, true);
                mailAccount = storageService.getMailAccount(accountId, storageParameters.getUserId(), storageParameters.getContextId());
            }

            mailAccess = MailAccess.getInstance(session, accountId);
            final Folder retval;
            final boolean hasSubfolders;
            if (MailFolder.DEFAULT_FOLDER_ID.equals(fullname)) {
                if (MailAccount.DEFAULT_ID == accountId) {
                    final MailFolder rootFolder = mailAccess.getRootFolder();
                    retval =
                        new MailFolderImpl(
                            rootFolder,
                            accountId,
                            mailAccess.getMailConfig(),
                            storageParameters,
                            null);
                    addWarnings(mailAccess, storageParameters);
                    hasSubfolders = rootFolder.hasSubfolders();
                } else {
                    /*
                     * An external account folder
                     */
                    retval = new ExternalMailAccountRootFolder(mailAccount, mailAccess.getMailConfig(), session);
                    hasSubfolders = true;
                }
                /*
                 * This one needs sorting. Just pass null or an empty array.
                 */
                retval.setSubfolderIDs(hasSubfolders ? null : new String[0]);
            } else {
                mailAccess.connect();
                final MailFolder mailFolder = getMailFolder(treeId, accountId, fullname, true, session, mailAccess);
                /*
                 * Generate mail folder from loaded one
                 */
                retval =
                    new MailFolderImpl(
                        mailFolder,
                        accountId,
                        mailAccess.getMailConfig(),
                        storageParameters,
                        new MailAccessFullnameProvider(mailAccess));
                hasSubfolders = mailFolder.hasSubfolders();
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
                    final List<MailFolder> children = Arrays.asList(mailAccess.getFolderStorage().getSubfolders(fullname, true));
                    Collections.sort(children, new SimpleMailFolderComparator(storageParameters.getUser().getLocale()));
                    final String[] subfolderIds = new String[children.size()];
                    int i = 0;
                    for (final MailFolder child : children) {
                        subfolderIds[i++] = prepareFullname(accountId, child.getFullname());
                    }
                    retval.setSubfolderIDs(subfolderIds);
                }
                addWarnings(mailAccess, storageParameters);
            }
            retval.setTreeID(treeId);

            return retval;
        } catch (final MailException e) {
            throw new FolderException(e);
        } catch (final ContextException e) {
            throw new FolderException(e);
        } catch (final ServiceException e) {
            throw new FolderException(e);
        } catch (final MailAccountException e) {
            throw new FolderException(e);
        } finally {
            closeMailAccess(mailAccess);
        }
    }

    private static ServerSession getServerSession(final StorageParameters storageParameters) throws FolderException, ContextException {
        final Session s = storageParameters.getSession();
        if (null == s) {
            throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
        }
        if (s instanceof ServerSession) {
            return (ServerSession) s;
        }
        return new ServerSessionAdapter(s);
    }

    private static MailFolder getMailFolder(final String treeId, final int accountId, final String fullname, final boolean createIfAbsent, final Session session, final MailAccess<?, ?> mailAccess) throws MailException {
        try {
            return mailAccess.getFolderStorage().getFolder(fullname);
        } catch (final MailException e) {
            if (!createIfAbsent) {
                throw e;
            }
            if ((MIMEMailException.Code.FOLDER_NOT_FOUND.getNumber() != e.getDetailNumber()) || FolderStorage.REAL_TREE_ID.equals(treeId)) {
                throw e;
            }
            return recreateMailFolder(accountId, fullname, session, mailAccess);
        }
    }

    private static MailFolder recreateMailFolder(final int accountId, final String fullname, final Session session, final MailAccess<?, ?> mailAccess) throws MailException {
        /*
         * Recreate the mail folder
         */
        final MailFolderDescription mfd = new MailFolderDescription();
        mfd.setExists(false);
        mfd.setAccountId(accountId);
        mfd.setParentAccountId(accountId);
        /*
         * Parent fullname & name
         */
        final char separator = mailAccess.getFolderStorage().getFolder("INBOX").getSeparator();
        final String[] parentAndName = splitBySeperator(fullname, separator);
        mfd.setParentFullname(parentAndName[0]);
        mfd.setName(parentAndName[1]);
        mfd.setSeparator(separator);
        {
            final MailPermission mailPerm = MailProviderRegistry.getMailProviderBySession(session, accountId).createNewMailPermission();
            mailPerm.setEntity(session.getUserId());
            mailPerm.setGroupPermission(false);
            mailPerm.setFolderAdmin(true);
            final int max = MailPermission.ADMIN_PERMISSION;
            mailPerm.setAllPermission(max, max, max, max);
            mfd.addPermission(mailPerm);
        }
        mfd.setSubscribed(true);
        /*
         * Create
         */
        final String id = mailAccess.getFolderStorage().createFolder(mfd);
        return mailAccess.getFolderStorage().getFolder(id);
    }

    private static String[] splitBySeperator(final String fullname, final char sep) {
        final int pos = fullname.lastIndexOf(sep);
        if (pos < 0) {
            return new String[] { MailFolder.DEFAULT_FOLDER_ID, fullname };
        }
        return new String[] { fullname.substring(0, pos), fullname.substring(pos + 1) };
    }

    private boolean isDefaultFoldersChecked(final int accountId, final Session session) {
        final Boolean b =
            MailSessionCache.getInstance(session).getParameter(accountId, MailSessionParameterNames.getParamDefaultFolderChecked());
        return (b != null) && b.booleanValue();
    }

    private String[] getSortedDefaultMailFolders(final int accountId, final Session session) {
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
        return MailFolderType.getInstance();
    }

    public SortableId[] getSubfolders(final String treeId, final String parentId, final StorageParameters storageParameters) throws FolderException {
        MailAccess<?, ?> mailAccess = null;
        try {
            final ServerSession session = getServerSession(storageParameters);
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }
            if (PRIVATE_FOLDER_ID.equals(parentId)) {
                /*
                 * Get all user mail accounts
                 */
                final List<MailAccount> accounts;
                final ServiceRegistry serviceRegistry = MailServiceRegistry.getServiceRegistry();
                if (session.getUserConfiguration().isMultipleMailAccounts()) {
                    final MailAccountStorageService storageService = serviceRegistry.getService(MailAccountStorageService.class, true);
                    final MailAccount[] accountsArr = storageService.getUserMailAccounts(session.getUserId(), session.getContextId());
                    final List<MailAccount> tmp = new ArrayList<MailAccount>(accountsArr.length);
                    tmp.addAll(Arrays.asList(accountsArr));
                    Collections.sort(tmp, new MailAccountComparator(session.getUser().getLocale()));
                    accounts = tmp;
                } else {
                    accounts = new ArrayList<MailAccount>(1);
                    final MailAccountStorageService storageService = serviceRegistry.getService(MailAccountStorageService.class, true);
                    accounts.add(storageService.getDefaultMailAccount(session.getUserId(), session.getContextId()));
                }
                if (UnifiedINBOXManagement.PROTOCOL_UNIFIED_INBOX.equals(accounts.get(0).getMailProtocol())) {
                    /*
                     * Ensure Unified INBOX is enabled; meaning at least one account is subscribed to Unified INBOX
                     */
                    final UnifiedINBOXManagement uim = serviceRegistry.getService(UnifiedINBOXManagement.class);
                    if (null == uim || !uim.isEnabled(session.getUserId(), session.getContextId())) {
                        accounts.remove(0);
                    }
                }
                final int size = accounts.size();
                final List<SortableId> list = new ArrayList<SortableId>(size);
                for (int j = 0; j < size; j++) {
                    list.add(new MailId(prepareFullname(accounts.get(j).getId(), MailFolder.DEFAULT_FOLDER_ID), j).setName(MailFolder.DEFAULT_FOLDER_NAME));
                }
                return list.toArray(new SortableId[list.size()]);
            }

            // A mail folder denoted by fullname
            final FullnameArgument argument = prepareMailFolderParam(parentId);
            final int accountId = argument.getAccountId();
            final String fullname = argument.getFullname();
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect(true);

            final List<MailFolder> children = new ArrayList<MailFolder>(Arrays.asList(mailAccess.getFolderStorage().getSubfolders(fullname, true)));
            /*
             * Filter against possible POP3 storage folders
             */
            if (MailAccount.DEFAULT_ID == accountId && MailProperties.getInstance().isIgnorePOP3StorageFolders()) {
                final Set<String> pop3StorageFolders = getPOP3StorageFolders(session);
                for (final Iterator<MailFolder> it = children.iterator(); it.hasNext();) {
                    final MailFolder mailFolder = it.next();
                    if (pop3StorageFolders.contains(mailFolder.getFullname())) {
                        it.remove();
                    }
                }            
            }
            addWarnings(mailAccess, storageParameters);
            /*
             * Check if denoted parent can hold default folders like Trash, Sent, etc.
             */
            if (!MailFolder.DEFAULT_FOLDER_ID.equals(fullname) && !"INBOX".equals(fullname)) {
                /*
                 * Denoted parent is not capable to hold default folders. Therefore output as it is.
                 */
                Collections.sort(children, new SimpleMailFolderComparator(storageParameters.getUser().getLocale()));
            } else {
                /*
                 * Ensure default folders are at first positions
                 */
                final String[] names;
                if (isDefaultFoldersChecked(accountId, storageParameters.getSession())) {
                    names = getSortedDefaultMailFolders(accountId, storageParameters.getSession());
                } else {
                    final List<String> tmp = new ArrayList<String>();
                    tmp.add("INBOX");

                    final IMailFolderStorage folderStorage = mailAccess.getFolderStorage();
                    String fn = folderStorage.getDraftsFolder();
                    if (null != fn) {
                        tmp.add(fn);
                    }

                    fn = folderStorage.getSentFolder();
                    if (null != fn) {
                        tmp.add(fn);
                    }

                    fn = folderStorage.getSpamFolder();
                    if (null != fn) {
                        tmp.add(fn);
                    }

                    fn = folderStorage.getTrashFolder();
                    if (null != fn) {
                        tmp.add(fn);
                    }

                    names = tmp.toArray(new String[tmp.size()]);
                }
                /*
                 * Sort them
                 */
                final Locale locale = storageParameters.getUser().getLocale();
                Collections.sort(children, new MailFolderComparator(names, locale));
                /*
                 * i18n INBOX name
                 */
                if (MailFolder.DEFAULT_FOLDER_ID.equals(fullname)) {
                    for (final MailFolder child : children) {
                        if ("INBOX".equals(child.getFullname())) {
                            child.setName(new StringHelper(locale).getString(MailStrings.INBOX));
                        }
                    }
                }
            }
            /*
             * Generate sorted IDs preserving order
             */
            final int size = children.size();
            final List<SortableId> list = new ArrayList<SortableId>(size);
            for (int j = 0; j < size; j++) {
                final MailFolder tmp = children.get(j);
                list.add(new MailId(prepareFullname(accountId, tmp.getFullname()), j).setName(tmp.getName()));
            }
            return list.toArray(new SortableId[list.size()]);
        } catch (final MailException e) {
            throw new FolderException(e);
        } catch (final ContextException e) {
            throw new FolderException(e);
        } catch (final ServiceException e) {
            throw new FolderException(e);
        } catch (final MailAccountException e) {
            throw new FolderException(e);
        } finally {
            closeMailAccess(mailAccess);
        }
    }

    public void rollback(final StorageParameters params) {
        /*
         * Nothing to do
         */
    }

    public boolean startTransaction(final StorageParameters parameters, final boolean modify) throws FolderException {
        /*
         * Nothing to do
         */
        return false;
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
        MailAccess<?, ?> mailAccess = null;
        try {
            final FullnameArgument argument = prepareMailFolderParam(folderId);
            final String fullname = argument.getFullname();
            if (MailFolder.DEFAULT_FOLDER_ID.equals(fullname)) {
                /*
                 * The default folder always exists
                 */
                return true;
            }
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }
            mailAccess = MailAccess.getInstance(session, argument.getAccountId());
            mailAccess.connect(true);
            final boolean exists = mailAccess.getFolderStorage().exists(fullname);
            addWarnings(mailAccess, storageParameters);
            return exists;
        } catch (final MailException e) {
            throw new FolderException(e);
        } finally {
            closeMailAccess(mailAccess);
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
                final SortableId[] subfolders = getSubfolders(treeId, PRIVATE_FOLDER_ID, storageParameters);
                for (final SortableId sortableId : subfolders) {
                    ret.add(sortableId.getId());
                }
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

    public void updateFolder(final Folder folder, final StorageParameters storageParameters) throws FolderException {
        MailAccess<?, ?> mailAccess = null;
        try {
            final int accountId;
            String fullname;
            {
                final FullnameArgument argument = prepareMailFolderParam(folder.getID());
                accountId = argument.getAccountId();
                fullname = argument.getFullname();
            }
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create(new Object[0]);
            }
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect(false);

            final MailFolderDescription mfd = new MailFolderDescription();
            mfd.setExists(true);
            // Fullname
            mfd.setFullname(fullname);
            mfd.setAccountId(accountId);
            // Parent
            if (null != folder.getParentID()) {
                final FullnameArgument parentArg = prepareMailFolderParam(folder.getParentID());
                mfd.setParentFullname(parentArg.getFullname());
                mfd.setParentAccountId(parentArg.getAccountId());
            }
            // Separator
            {
                final MailFolder mf = mailAccess.getFolderStorage().getFolder(fullname);
                mfd.setSeparator(mf.getSeparator());
            }
            // Name
            if (null != folder.getName()) {
                mfd.setName(folder.getName());
            }
            // Subscribed
            mfd.setSubscribed(folder.isSubscribed());
            // Permissions
            final Permission[] permissions = folder.getPermissions();
            if (null != permissions && permissions.length > 0) {
                final MailPermission[] mailPermissions = new MailPermission[permissions.length];
                final MailProvider provider = MailProviderRegistry.getMailProviderBySession(session, accountId);
                for (int i = 0; i < permissions.length; i++) {
                    final Permission permission = permissions[i];
                    final MailPermission mailPerm = provider.createNewMailPermission();
                    mailPerm.setEntity(permission.getEntity());
                    mailPerm.setAllPermission(
                        permission.getFolderPermission(),
                        permission.getReadPermission(),
                        permission.getWritePermission(),
                        permission.getDeletePermission());
                    mailPerm.setFolderAdmin(permission.isAdmin());
                    mailPerm.setGroupPermission(permission.isGroup());
                    mailPermissions[i] = mailPerm;
                }
                mfd.addPermissions(mailPermissions);
            }

            final char separator = mfd.getSeparator();
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
            if (mfd.containsParentFullname()) {
                final int parentAccountID = mfd.getParentAccountId();
                if (accountId == parentAccountID) {
                    final String newParent = mfd.getParentFullname();
                    final StringBuilder newFullname = new StringBuilder(16);
                    if (!MailFolder.DEFAULT_FOLDER_ID.equals(newParent)) {
                        newFullname.append(newParent).append(mfd.getSeparator());
                    }
                    newFullname.append(mfd.containsName() ? mfd.getName() : oldName);
                    if (!newParent.equals(oldParent)) { // move & rename
                        final Map<String, Map<?, ?>> subfolders = subfolders(fullname, mailAccess);
                        fullname = mailAccess.getFolderStorage().moveFolder(fullname, newFullname.toString());
                        folder.setID(prepareFullname(accountId, fullname));
                        postEvent4Subfolders(accountId, subfolders, storageParameters);
                        postEvent(accountId, newParent, false, storageParameters);
                        movePerformed = true;
                    }
                } else {
                    // Move to another account
                    final MailAccess<?, ?> otherAccess = MailAccess.getInstance(session, parentAccountID);
                    otherAccess.connect();
                    try {
                        final String newParent = mfd.getParentFullname();
                        // Check if parent mail folder exists
                        final MailFolder p = otherAccess.getFolderStorage().getFolder(newParent);
                        // Check permission on new parent
                        final MailPermission ownPermission = p.getOwnPermission();
                        if (!ownPermission.canCreateSubfolders()) {
                            throw new MailException(MailException.Code.NO_CREATE_ACCESS, newParent);
                        }
                        // Check for duplicate
                        final MailFolder[] tmp = otherAccess.getFolderStorage().getSubfolders(newParent, true);
                        final String lookFor = mfd.containsName() ? mfd.getName() : oldName;
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
                                storageParameters.getUserId(),
                                otherAccess.getMailConfig().getCapabilities().hasPermissions());
                        postEvent(parentAccountID, newParent, false, storageParameters);
                        // Delete source
                        final Map<String, Map<?, ?>> subfolders = subfolders(fullname, mailAccess);
                        mailAccess.getFolderStorage().deleteFolder(fullname, true);
                        // Perform other updates
                        otherAccess.getFolderStorage().updateFolder(destFullname, mfd);
                        postEvent4Subfolders(accountId, subfolders, storageParameters);
                    } finally {
                        otherAccess.close(true);
                    }
                }
            }
            /*
             * Check if a rename shall be performed
             */
            if (!movePerformed && mfd.containsName()) {
                final String newName = mfd.getName();
                if (!newName.equals(oldName)) { // rename
                    fullname = mailAccess.getFolderStorage().renameFolder(fullname, newName);
                    folder.setID(prepareFullname(accountId, fullname));
                    postEvent(accountId, fullname, false, storageParameters);
                }
            }
            /*
             * Handle update of permission or subscription
             */
            mailAccess.getFolderStorage().updateFolder(fullname, mfd);
            addWarnings(mailAccess, storageParameters);
            postEvent(accountId, fullname, false, storageParameters);
        } catch (final MailException e) {
            throw new FolderException(e);
        } finally {
            closeMailAccess(mailAccess);
        }
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
                for (final MailPermission perm : perms) {
                    mfd.addPermission((MailPermission) perm.clone());
                }
            } catch (final CloneNotSupportedException e) {
                throw new MailException(MailException.Code.UNEXPECTED_ERROR, e, e.getMessage());
            }
        }
        final String destFullname = destAccess.getFolderStorage().createFolder(mfd);
        // Copy messages
        final MailMessage[] msgs =
            srcAccess.getMessageStorage().getAllMessages(
                srcFullname,
                null,
                MailSortField.RECEIVED_DATE,
                OrderDirection.ASC,
                new MailField[] { MailField.FULL });
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
        for (final MailFolder element : tmp) {
            fullCopy(srcAccess, element.getFullname(), destAccess, destFullname, destSeparator, user, hasPermissions);
        }
        return destFullname;
    }

    private static final class MailAccountComparator implements Comparator<MailAccount> {

        private final Collator collator;

        public MailAccountComparator(final Locale locale) {
            super();
            collator = Collator.getInstance(locale);
            collator.setStrength(Collator.SECONDARY);
        }

        public int compare(final MailAccount o1, final MailAccount o2) {
            if (UnifiedINBOXManagement.PROTOCOL_UNIFIED_INBOX.equals(o1.getMailProtocol())) {
                if (UnifiedINBOXManagement.PROTOCOL_UNIFIED_INBOX.equals(o2.getMailProtocol())) {
                    return 0;
                }
                return -1;
            } else if (UnifiedINBOXManagement.PROTOCOL_UNIFIED_INBOX.equals(o2.getMailProtocol())) {
                return 1;
            }
            if (o1.isDefaultAccount()) {
                if (o2.isDefaultAccount()) {
                    return 0;
                }
                return -1;
            } else if (o2.isDefaultAccount()) {
                return 1;
            }
            return collator.compare(o1.getName(), o2.getName());
        }

    } // End of MailAccountComparator

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

    private static void postEvent4Subfolders(final int accountId, final Map<String, Map<?, ?>> subfolders, final StorageParameters params) {
        final int size = subfolders.size();
        final Iterator<Entry<String, Map<?, ?>>> iter = subfolders.entrySet().iterator();
        for (int i = 0; i < size; i++) {
            final Entry<String, Map<?, ?>> entry = iter.next();
            final @SuppressWarnings("unchecked") Map<String, Map<?, ?>> m = (Map<String, Map<?, ?>>) entry.getValue();
            if (!m.isEmpty()) {
                postEvent4Subfolders(accountId, m, params);
            }
            postEvent(accountId, entry.getKey(), false, true, false, params);
        }
    }

    private static Map<String, Map<?, ?>> subfolders(final String fullname, final MailAccess<?, ?> mailAccess) throws MailException {
        final Map<String, Map<?, ?>> m = new HashMap<String, Map<?, ?>>();
        subfoldersRecursively(fullname, m, mailAccess);
        return m;
    }

    private static void subfoldersRecursively(final String parent, final Map<String, Map<?, ?>> m, final MailAccess<?, ?> mailAccess) throws MailException {
        final MailFolder[] mailFolders = mailAccess.getFolderStorage().getSubfolders(parent, true);
        if (null == mailFolders || 0 == mailFolders.length) {
            final Map<String, Map<?, ?>> emptyMap = Collections.emptyMap();
            m.put(parent, emptyMap);
        } else {
            final Map<String, Map<?, ?>> subMap = new HashMap<String, Map<?, ?>>();
            final int size = mailFolders.length;
            for (int i = 0; i < size; i++) {
                final String fullname = mailFolders[i].getFullname();
                subfoldersRecursively(fullname, subMap, mailAccess);
            }
            m.put(parent, subMap);
        }
    }

    private static void postEvent(final int accountId, final String fullname, final boolean contentRelated, final StorageParameters params) {
        postEvent(accountId, fullname, contentRelated, false, params);
    }

    private static void postEvent(final int accountId, final String fullname, final boolean contentRelated, final boolean immediateDelivery, final StorageParameters params) {
        if (MailAccount.DEFAULT_ID != accountId) {
            /*
             * TODO: No event for non-primary account?
             */
            return;
        }
        EventPool.getInstance().put(
            new PooledEvent(params.getContextId(), params.getUserId(), accountId, prepareFullname(accountId, fullname), contentRelated, immediateDelivery, params.getSession()));
    }

    private static void postEvent(final String topic, final int accountId, final String fullname, final boolean contentRelated, final boolean immediateDelivery, final StorageParameters params) {
        if (MailAccount.DEFAULT_ID != accountId) {
            /*
             * TODO: No event for non-primary account?
             */
            return;
        }
        EventPool.getInstance().put(new PooledEvent(topic, params.getContextId(), params.getUserId(), accountId, prepareFullname(accountId, fullname), contentRelated, immediateDelivery, params.getSession()));
    }

    private static void postEvent(final int accountId, final String fullname, final boolean contentRelated, final boolean immediateDelivery, final boolean async, final StorageParameters params) {
        if (MailAccount.DEFAULT_ID != accountId) {
            /*
             * TODO: No event for non-primary account?
             */
            return;
        }
        EventPool.getInstance().put(
            new PooledEvent(params.getContextId(), params.getUserId(), accountId, prepareFullname(accountId, fullname), contentRelated, immediateDelivery, params.getSession()).setAsync(async));
    }

    private static void closeMailAccess(final MailAccess<?, ?> mailAccess) {
        if (null != mailAccess) {
            mailAccess.close(true);
        }
    }

    private static void addWarnings(final MailAccess<?, ?> mailAccess, final StorageParameters storageParameters) {
        final Collection<MailException> warnings = mailAccess.getWarnings();
        if (!warnings.isEmpty()) {
            for (final MailException mailException : warnings) {
                storageParameters.addWarning(mailException);
            }
        }
    }

    private static Set<String> getPOP3StorageFolders(final Session session) throws MailException {
        final int contextId = session.getContextId();
        final Connection con;
        try {
            con = Database.get(contextId, false);
        } catch (final DBPoolingException e) {
            throw new MailException(e);
        }
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT value FROM user_mail_account_properties WHERE cid = ? AND user = ? AND name = ?");
            stmt.setInt(1, contextId);
            stmt.setInt(2, session.getUserId());
            stmt.setString(3, "pop3.path");
            rs = stmt.executeQuery();
            final Set<String> set = new HashSet<String>();
            while (rs.next()) {
                set.add(rs.getString(1));
            }
            return set;
        } catch (final SQLException e) {
            throw new MailException(MailException.Code.UNEXPECTED_ERROR, e, e.getMessage());
        } finally {
            DBUtils.closeSQLStuff(rs, stmt);
            Database.back(contextId, false, con);
        }
    }

}
