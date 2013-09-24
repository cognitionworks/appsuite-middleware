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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

import static com.openexchange.mail.utils.MailFolderUtility.prepareMailFolderParam;
import gnu.trove.map.hash.TIntIntHashMap;
import java.util.Locale;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.AbstractFolder;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.FolderExtension;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.StorageParameters;
import com.openexchange.folderstorage.SystemContentType;
import com.openexchange.folderstorage.Type;
import com.openexchange.folderstorage.mail.contentType.DraftsContentType;
import com.openexchange.folderstorage.mail.contentType.MailContentType;
import com.openexchange.folderstorage.mail.contentType.SentContentType;
import com.openexchange.folderstorage.mail.contentType.SpamContentType;
import com.openexchange.folderstorage.mail.contentType.TrashContentType;
import com.openexchange.folderstorage.type.MailType;
import com.openexchange.folderstorage.type.SystemType;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.i18n.MailStrings;
import com.openexchange.groupware.ldap.User;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.mail.IndexRange;
import com.openexchange.mail.MailField;
import com.openexchange.mail.MailSortField;
import com.openexchange.mail.OrderDirection;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailFolderStorageEnhanced;
import com.openexchange.mail.api.IMailFolderStorageEnhanced2;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.mail.permission.DefaultMailPermission;
import com.openexchange.mail.permission.MailPermission;
import com.openexchange.mail.utils.MailFolderUtility;
import com.openexchange.mailaccount.UnifiedInboxManagement;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.server.services.ServerServiceRegistry;

/**
 * {@link MailFolderImpl} - A mail folder.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MailFolderImpl extends AbstractFolder implements FolderExtension {

    private static final long serialVersionUID = 6445442372690458946L;

    private static final org.apache.commons.logging.Log LOG = com.openexchange.log.Log.valueOf(com.openexchange.log.LogFactory.getLog(MailFolderImpl.class));
    private static final boolean DEBUG = LOG.isDebugEnabled();

    private static final String PROTOCOL_UNIFIED_INBOX = UnifiedInboxManagement.PROTOCOL_UNIFIED_INBOX;

    /**
     * The mail folder content type.
     */
    public static enum MailFolderType {
        NONE(MailContentType.getInstance(), 0),
        ROOT(SystemContentType.getInstance(), 0),
        INBOX(MailContentType.getInstance(), 7), // FolderObject.MAIL
        DRAFTS(DraftsContentType.getInstance(), 9),
        SENT(SentContentType.getInstance(), 10),
        SPAM(SpamContentType.getInstance(), 11),
        TRASH(TrashContentType.getInstance(), 12);

        private final ContentType contentType;

        private final int type;

        private MailFolderType(final ContentType contentType, final int type) {
            this.contentType = contentType;
            this.type = type;
        }

        /**
         * Gets the content type associated with this mail folder type.
         *
         * @return The content type
         */
        public ContentType getContentType() {
            return contentType;
        }

        /**
         * Gets the type.
         *
         * @return The type
         */
        public int getType() {
            return type;
        }

    }

    private final MailFolderType mailFolderType;
    private final boolean cacheable;
    private final String fullName;
    private final int accountId;
    private final int userId;
    private final int contextId;
    private String localizedName;

    private static final int BIT_USER_FLAG = (1 << 29);

    private static final int BIT_RENAME_FLAG = (1 << 30);

    /**
     * Initializes a new {@link MailFolderImpl} from given mail folder.
     * <p>
     * Subfolder identifiers and tree identifier are not set within this constructor.
     *
     * @param mailFolder The underlying mail folder
     * @param accountId The account identifier
     * @param mailConfig The mail configuration
     * @param user The user
     * @param context The context
     * @param fullnameProvider The (optional) full name provider
     * @param translate Whether to translate folders according to user's locale
     * @throws OXException If creation fails
     */
    public MailFolderImpl(final MailFolder mailFolder, final int accountId, final MailConfig mailConfig, final StorageParameters params, final DefaultFolderFullnameProvider fullnameProvider) throws OXException {
        this(mailFolder, accountId, mailConfig, params.getUser(), params.getContext(), fullnameProvider);
    }

    /**
     * Initializes a new {@link MailFolderImpl} from given mail folder.
     * <p>
     * Subfolder identifiers and tree identifier are not set within this constructor.
     *
     * @param mailFolder The underlying mail folder
     * @param accountId The account identifier
     * @param mailConfig The mail configuration
     * @param user The user
     * @param context The context
     * @param fullnameProvider The (optional) full name provider
     * @throws OXException If creation fails
     */
    public MailFolderImpl(final MailFolder mailFolder, final int accountId, final MailConfig mailConfig, final User user, final Context context, final DefaultFolderFullnameProvider fullnameProvider) throws OXException {
        super();
        this.accountId = accountId;
        userId = user.getId();
        contextId = context.getContextId();
        fullName = mailFolder.getFullname();
        id = MailFolderUtility.prepareFullname(accountId, fullName);
        final String folderName = mailFolder.getName();
        name = folderName;
        localizedName = ("INBOX".equals(fullName) ? StringHelper.valueOf(user.getLocale()).getString(MailStrings.INBOX) : folderName);
        // FolderObject.SYSTEM_PRIVATE_FOLDER_ID
        parent =
            mailFolder.isRootFolder() ? FolderStorage.PRIVATE_ID : MailFolderUtility.prepareFullname(
                accountId,
                mailFolder.getParentFullname());
        final MailPermission[] mailPermissions = mailFolder.getPermissions();
        permissions = new Permission[mailPermissions.length];
        for (int i = 0; i < mailPermissions.length; i++) {
            permissions[i] = new MailPermissionImpl(mailPermissions[i]);
        }
        type = SystemType.getInstance();
        final boolean ignoreSubscription = mailConfig.getMailProperties().isIgnoreSubscription();
        subscribed = ignoreSubscription ? true : mailFolder.isSubscribed(); // || mailFolder.hasSubscribedSubfolders();
        subscribedSubfolders = ignoreSubscription ? mailFolder.hasSubfolders() : mailFolder.hasSubscribedSubfolders();
        this.capabilities = mailConfig.getCapabilities().getCapabilities();
        {
            final String value =
                mailFolder.isRootFolder() ? "" : new com.openexchange.java.StringAllocator(16).append('(').append(mailFolder.getMessageCount()).append('/').append(
                    mailFolder.getUnreadMessageCount()).append(')').toString();
            summary = value;
        }
        deefault = /* mailFolder.isDefaultFolder(); */0 == accountId && mailFolder.isDefaultFolder();
        total = mailFolder.getMessageCount();
        nu = mailFolder.getNewMessageCount();
        unread = mailFolder.getUnreadMessageCount();
        deleted = mailFolder.getDeletedMessageCount();
        final MailPermission mp;
        if (mailFolder.isRootFolder()) {
            mailFolderType = MailFolderType.ROOT;
            final MailPermission rootPermission = mailFolder.getOwnPermission();
            if (rootPermission == null) {
                mp = new DefaultMailPermission();
                mp.setAllPermission(
                    OCLPermission.CREATE_SUB_FOLDERS,
                    OCLPermission.NO_PERMISSIONS,
                    OCLPermission.NO_PERMISSIONS,
                    OCLPermission.NO_PERMISSIONS);
                mp.setFolderAdmin(false);
            } else {
                mp = rootPermission;
            }
        } else {
            mp = mailFolder.getOwnPermission();
            MailFolderType mailFolderType = MailFolderType.NONE;
            /*
             * Check if entity's permission allows to read the folder: Every mail folder listed is at least visible to user
             */
            for (final Permission pe : permissions) {
                if ((pe.getEntity() == mp.getEntity()) && (pe.getFolderPermission() <= Permission.NO_PERMISSIONS)) {
                    pe.setFolderPermission(Permission.READ_FOLDER);
                }
            }
            final ConfigurationService configurationService = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
            final boolean translateDefaultFolders = (null == configurationService || configurationService.getBoolProperty("com.openexchange.mail.translateDefaultFolders", true));
            if (mailFolder.containsDefaultFolderType()) {
                switch (mailFolder.getDefaultFolderType()) {
                case INBOX:
                    mailFolderType = MailFolderType.INBOX;
                    break;
                case TRASH:
                    localizedName = translateDefaultFolders ? (StringHelper.valueOf(user.getLocale()).getString(MailStrings.TRASH)) : (MailStrings.TRASH.equals(folderName) ? StringHelper.valueOf(user.getLocale()).getString(MailStrings.TRASH) : folderName);
                    mailFolderType = MailFolderType.TRASH;
                    break;
                case SENT:
                    localizedName = translateDefaultFolders ? (StringHelper.valueOf(user.getLocale()).getString(MailStrings.SENT)) : (MailStrings.SENT.equals(folderName) ? StringHelper.valueOf(user.getLocale()).getString(MailStrings.SENT) : folderName);
                    mailFolderType = MailFolderType.SENT;
                    break;
                case SPAM:
                    localizedName = translateDefaultFolders ? (StringHelper.valueOf(user.getLocale()).getString(MailStrings.SPAM)) : (MailStrings.SPAM.equals(folderName) ? StringHelper.valueOf(user.getLocale()).getString(MailStrings.SPAM) : folderName);
                    mailFolderType = MailFolderType.SPAM;
                    break;
                case DRAFTS:
                    localizedName = translateDefaultFolders ? (StringHelper.valueOf(user.getLocale()).getString(MailStrings.DRAFTS)) : (MailStrings.DRAFTS.equals(folderName) ? StringHelper.valueOf(user.getLocale()).getString(MailStrings.DRAFTS) : folderName);
                    mailFolderType = MailFolderType.DRAFTS;
                    break;
                case CONFIRMED_SPAM:
                    localizedName = StringHelper.valueOf(user.getLocale()).getString(MailStrings.CONFIRMED_SPAM);
                    break;
                case CONFIRMED_HAM:
                    localizedName = StringHelper.valueOf(user.getLocale()).getString(MailStrings.CONFIRMED_HAM);
                    break;
                default:
                    // Nope
                }
            } else if (null != fullName) {
                if (null == fullnameProvider) {
                    mailFolderType = MailFolderType.ROOT;
                } else {
                    try {
                        if (fullName.equals(fullnameProvider.getDraftsFolder())) {
                            localizedName = translateDefaultFolders ? (StringHelper.valueOf(user.getLocale()).getString(MailStrings.DRAFTS)) : (MailStrings.DRAFTS.equals(folderName) ? StringHelper.valueOf(user.getLocale()).getString(MailStrings.DRAFTS) : folderName);
                            mailFolderType = MailFolderType.DRAFTS;
                        } else if (fullName.equals(fullnameProvider.getINBOXFolder())) {
                            mailFolderType = MailFolderType.INBOX;
                        } else if (fullName.equals(fullnameProvider.getSentFolder())) {
                            localizedName = translateDefaultFolders ? (StringHelper.valueOf(user.getLocale()).getString(MailStrings.SENT)) : (MailStrings.SENT.equals(folderName) ? StringHelper.valueOf(user.getLocale()).getString(MailStrings.SENT) : folderName);
                            mailFolderType = MailFolderType.SENT;
                        } else if (fullName.equals(fullnameProvider.getSpamFolder())) {
                            localizedName = translateDefaultFolders ? (StringHelper.valueOf(user.getLocale()).getString(MailStrings.SPAM)) : (MailStrings.SPAM.equals(folderName) ? StringHelper.valueOf(user.getLocale()).getString(MailStrings.SPAM) : folderName);
                            mailFolderType = MailFolderType.SPAM;
                        } else if (fullName.equals(fullnameProvider.getTrashFolder())) {
                            localizedName = translateDefaultFolders ? (StringHelper.valueOf(user.getLocale()).getString(MailStrings.TRASH)) : (MailStrings.TRASH.equals(folderName) ? StringHelper.valueOf(user.getLocale()).getString(MailStrings.TRASH) : folderName);
                            mailFolderType = MailFolderType.TRASH;
                        } else if (fullName.equals(fullnameProvider.getConfirmedSpamFolder())) {
                            localizedName = StringHelper.valueOf(user.getLocale()).getString(MailStrings.CONFIRMED_SPAM);
                        } else if (fullName.equals(fullnameProvider.getConfirmedHamFolder())) {
                            localizedName = StringHelper.valueOf(user.getLocale()).getString(MailStrings.CONFIRMED_HAM);
                        }
                    } catch (final OXException e) {
                        com.openexchange.log.Log.valueOf(com.openexchange.log.LogFactory.getLog(MailFolderImpl.class)).error(e.getMessage(), e);
                        mailFolderType = MailFolderType.NONE;
                    }
                }
            } else {
                mailFolderType = MailFolderType.NONE;
            }
            this.mailFolderType = mailFolderType;
        }
        if (!mailFolder.isHoldsFolders() && mp.canCreateSubfolders()) {
            // Cannot contain subfolders; therefore deny subfolder creation
            mp.setFolderPermission(OCLPermission.CREATE_OBJECTS_IN_FOLDER);
        }
        if (!mailFolder.isHoldsMessages() && mp.canReadOwnObjects()) {
            // Cannot contain messages; therefore deny read access. Folder is not selectable.
            mp.setReadObjectPermission(OCLPermission.NO_PERMISSIONS);
        }
        int permissionBits =
            createPermissionBits(
                mp.getFolderPermission(),
                mp.getReadPermission(),
                mp.getWritePermission(),
                mp.getDeletePermission(),
                mp.isFolderAdmin());
        if (mailFolder.isSupportsUserFlags()) {
            permissionBits |= BIT_USER_FLAG;
        }
        final int canRename = mp.canRename();
        if (canRename > 0) {
            permissionBits |= BIT_RENAME_FLAG;
        }
        bits = permissionBits;
        /*
         * Check if folder is cacheable
         */
        boolean cache = true;
        if (mailFolder.containsShared() && mailFolder.isShared()) { // A shared mail folder must not be cacheable
            cache = false;
        } else if (mailFolder.isTrash()) { // Trash folder must not be cacheable
            cache = false;
        } else if (isUnifiedMail(mailFolder)) { // Unified mail must not be cacheable
            cache = false;
        }
        cacheable = cache;
    }

    private boolean isUnifiedMail(final MailFolder mailFolder) {
        return PROTOCOL_UNIFIED_INBOX.equals(mailFolder.getProperty("protocol"));
    }

    /**
     * The actual max permission that can be transfered in field 'bits' or JSON's permission object
     */
    private static final int MAX_PERMISSION = 64;

    private static final TIntIntHashMap MAPPING = new TIntIntHashMap(6) {
        { //Unnamed Block.
            put(Permission.MAX_PERMISSION, MAX_PERMISSION);
            put(MAX_PERMISSION, MAX_PERMISSION);
            put(0, 0);
            put(2, 1);
            put(4, 2);
            put(8, 4);
        }
    };

    static int createPermissionBits(final Permission perm) {
        return createPermissionBits(
            perm.getFolderPermission(),
            perm.getReadPermission(),
            perm.getWritePermission(),
            perm.getDeletePermission(),
            perm.isAdmin());
    }

    static int createPermissionBits(final int fp, final int rp, final int wp, final int dp, final boolean adminFlag) {
        int retval = 0;
        int i = 4;
        retval += (adminFlag ? 1 : 0) << (i-- * 7)/*Number of bits to be shifted*/;
        retval += MAPPING.get(dp) << (i-- * 7);
        retval += MAPPING.get(wp) << (i-- * 7);
        retval += MAPPING.get(rp) << (i-- * 7);
        retval += MAPPING.get(fp) << (i * 7);
        return retval;
    }

    /*-
     *
    private static final int[] mapping = { 0, -1, 1, -1, 2, -1, -1, -1, 4 };

    static int createPermissionBits(final int fp, final int orp, final int owp, final int odp, final boolean adminFlag) throws FolderException {
        final int[] perms = new int[5];
        perms[0] = fp == MAX_PERMISSION ? OCLPermission.ADMIN_PERMISSION : fp;
        perms[1] = orp == MAX_PERMISSION ? OCLPermission.ADMIN_PERMISSION : orp;
        perms[2] = owp == MAX_PERMISSION ? OCLPermission.ADMIN_PERMISSION : owp;
        perms[3] = odp == MAX_PERMISSION ? OCLPermission.ADMIN_PERMISSION : odp;
        perms[4] = adminFlag ? 1 : 0;
        return createPermissionBits(perms);
    }

    private static final int MAX_PERMISSION = 64;

    private static int createPermissionBits(final int[] permission) throws FolderException {
        int retval = 0;
        boolean first = true;
        for (int i = permission.length - 1; i >= 0; i--) {
            final int shiftVal = (i * 7); // Number of bits to be shifted
            if (first) {
                retval += permission[i] << shiftVal;
                first = false;
            } else {
                if (permission[i] == OCLPermission.ADMIN_PERMISSION) {
                    retval += MAX_PERMISSION << shiftVal;
                } else {
                    try {
                        retval += mapping[permission[i]] << shiftVal;
                    } catch (final Exception e) {
                        throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
                    }
                }
            }
        }
        return retval;
    }
     *
     */

    private static final MailField[] FIELDS_ID = new MailField[] { MailField.ID };

    @Override
    public int getUnread() {
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            mailAccess = MailAccess.getInstance(userId, contextId, accountId);
            mailAccess.connect(false);
            final IMailFolderStorage folderStorage = mailAccess.getFolderStorage();
            if (folderStorage instanceof IMailFolderStorageEnhanced) {
                return ((IMailFolderStorageEnhanced) folderStorage).getUnreadCounter(ensureFullName(fullName));
            }
            return mailAccess.getMessageStorage().getUnreadMessages(ensureFullName(fullName), MailSortField.RECEIVED_DATE, OrderDirection.DESC, FIELDS_ID, -1).length;
        } catch (final OXException e) {
            if (DEBUG) {
                LOG.debug("Cannot return up-to-date unread counter.", e);
            }
            return super.getUnread();
        } catch (final Exception e) {
            if (DEBUG) {
                LOG.debug("Cannot return up-to-date unread counter.", e);
            }
            return super.getUnread();
        } finally {
            if (null != mailAccess) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public int getTotal() {
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            mailAccess = MailAccess.getInstance(userId, contextId, accountId);
            mailAccess.connect(false);
            final IMailFolderStorage folderStorage = mailAccess.getFolderStorage();
            if (folderStorage instanceof IMailFolderStorageEnhanced) {
                return ((IMailFolderStorageEnhanced) folderStorage).getTotalCounter(ensureFullName(fullName));
            }
            return mailAccess.getMessageStorage().searchMessages(ensureFullName(fullName), IndexRange.NULL, MailSortField.RECEIVED_DATE, OrderDirection.ASC, null, FIELDS_ID).length;
        } catch (final OXException e) {
            if (DEBUG) {
                LOG.debug("Cannot return up-to-date total counter.", e);
            }
            return super.getTotal();
        } catch (final Exception e) {
            if (DEBUG) {
                LOG.debug("Cannot return up-to-date total counter.", e);
            }
            return super.getTotal();
        } finally {
            if (null != mailAccess) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public int[] getTotalAndUnread() {
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            mailAccess = MailAccess.getInstance(userId, contextId, accountId);
            mailAccess.connect(false);
            final IMailFolderStorage folderStorage = mailAccess.getFolderStorage();
            if (folderStorage instanceof IMailFolderStorageEnhanced2) {
                return ((IMailFolderStorageEnhanced2) folderStorage).getTotalAndUnreadCounter(ensureFullName(fullName));
            }
            final String ensuredFullName = ensureFullName(fullName);
            int unread, total;
            if (folderStorage instanceof IMailFolderStorageEnhanced) {
                final IMailFolderStorageEnhanced storageEnhanced = (IMailFolderStorageEnhanced) folderStorage;
                unread = storageEnhanced.getUnreadCounter(ensuredFullName);
                total = storageEnhanced.getTotalCounter(ensuredFullName);
            } else {
                unread = mailAccess.getMessageStorage().getUnreadMessages(ensuredFullName, MailSortField.RECEIVED_DATE, OrderDirection.DESC, FIELDS_ID, -1).length;
                total = mailAccess.getMessageStorage().searchMessages(ensuredFullName, IndexRange.NULL, MailSortField.RECEIVED_DATE, OrderDirection.ASC, null, FIELDS_ID).length;
            }
            return new int[] { total, unread };
        } catch (final OXException e) {
            if (DEBUG) {
                LOG.debug("Cannot return up-to-date total counter.", e);
            }
            return null;
        } catch (final Exception e) {
            if (DEBUG) {
                LOG.debug("Cannot return up-to-date total counter.", e);
            }
            return null;
        } finally {
            if (null != mailAccess) {
                mailAccess.close(true);
            }
        }
    }

    private static String ensureFullName(final String fullName) {
        return prepareMailFolderParam(fullName).getFullname();
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    @Override
    public boolean isCacheable() {
        return cacheable;
    }

    @Override
    public ContentType getContentType() {
        return mailFolderType.getContentType();
    }

    @Override
    public int getDefaultType() {
        return mailFolderType.getType();
    }

    @Override
    public void setDefaultType(final int defaultType) {
        // Nothing to do
    }

    @Override
    public Type getType() {
        return MailType.getInstance();
    }

    @Override
    public void setContentType(final ContentType contentType) {
        // Nothing to do
    }

    @Override
    public void setType(final Type type) {
        // Nothing to do
    }

    @Override
    public boolean isGlobalID() {
        return false;
    }

    @Override
    public String getLocalizedName(final Locale locale) {
        final String localizedName = this.localizedName;
        if (null == localizedName) {
            return name;
        }
        return localizedName;
    }

}
