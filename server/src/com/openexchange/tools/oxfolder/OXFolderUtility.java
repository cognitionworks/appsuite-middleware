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

package com.openexchange.tools.oxfolder;

import gnu.trove.TIntArrayList;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.openexchange.api2.OXException;
import com.openexchange.database.DBPoolingException;
import com.openexchange.group.Group;
import com.openexchange.group.GroupStorage;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.data.Check;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.server.impl.EffectivePermission;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link OXFolderUtility} - Provides utility methods for folder operations.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class OXFolderUtility {

    private static final org.apache.commons.logging.Log LOG = com.openexchange.log.Log.valueOf(com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(OXFolderUtility.class)));

    private static final String STR_EMPTY = "";

    /**
     * Initializes a new {@link OXFolderUtility}.
     */
    private OXFolderUtility() {
        super();
    }

    /**
     * Checks for duplicate folder name considering locale-sensitive folder names.
     * 
     * @param parentFolderId The parent folder ID
     * @param folderName The folder name to check
     * @param locale The user's locale
     * @param ctx The context
     * @throws OXFolderException If a duplicate folder exists
     */
    public static void checki18nString(final int parentFolderId, final String folderName, final Locale locale, final Context ctx) throws OXFolderException {
        if (FolderObject.SYSTEM_PUBLIC_FOLDER_ID == parentFolderId) {
            if (FolderObject.getFolderString(FolderObject.SYSTEM_LDAP_FOLDER_ID, locale).equalsIgnoreCase(folderName)) {
                final String parentFolderName =
                    new StringBuilder(FolderObject.getFolderString(parentFolderId, locale)).append(" (").append(parentFolderId).append(')').toString();
                /*
                 * A duplicate folder exists
                 */
                throw new OXFolderException(
                    OXFolderException.FolderCode.NO_DUPLICATE_FOLDER,
                    parentFolderName,
                    Integer.valueOf(ctx.getContextId()));
            }
            if (!OXFolderProperties.isIgnoreSharedAddressbook() && FolderObject.getFolderString(
                FolderObject.SYSTEM_GLOBAL_FOLDER_ID,
                locale).equalsIgnoreCase(folderName)) {
                final String parentFolderName =
                    new StringBuilder(FolderObject.getFolderString(parentFolderId, locale)).append(" (").append(parentFolderId).append(')').toString();
                /*
                 * A duplicate folder exists
                 */
                throw new OXFolderException(
                    OXFolderException.FolderCode.NO_DUPLICATE_FOLDER,
                    parentFolderName,
                    Integer.valueOf(ctx.getContextId()));
            }
        }
    }

    /**
     * Tests if specified folder contains contains duplicate permissions.
     * 
     * @param folder The folder whose permissions shall be checked.
     * @param ctx The context
     * @throws OXFolderException If specified folder contains contains duplicate permissions.
     */
    public static void checkForDuplicateNonSystemPermissions(final FolderObject folder, final Context ctx) throws OXFolderException {
        final OCLPermission[] permissions = folder.getNonSystemPermissionsAsArray();
        if (permissions.length == 0) {
            return;
        }
        final Set<Integer> set = new HashSet<Integer>(permissions.length);
        set.add(Integer.valueOf(permissions[0].getEntity()));
        for (int i = 1; i < permissions.length; i++) {
            final OCLPermission permission = permissions[i];
            final Integer key = Integer.valueOf(permission.getEntity());
            if (set.contains(key)) {
                if (permission.isGroupPermission()) {
                    throw new OXFolderException(OXFolderException.FolderCode.DUPLICATE_GROUP_PERMISSION, getGroupName(
                        permission.getEntity(),
                        ctx));
                }
                throw new OXFolderException(
                    OXFolderException.FolderCode.DUPLICATE_USER_PERMISSION,
                    getUserName(permission.getEntity(), ctx));
            }
            set.add(key);
        }
    }

    /**
     * Checks for similar named shared folder
     * 
     * @param userIds The user IDs
     * @param allSharedFolders The shared folders
     * @param folderName The folder name of the folder that shall be shared
     * @param ctx The context
     * @throws OXFolderException If a similar named shared folder is already shared to a user
     */
    public static void checkSimilarNamedSharedFolder(final Set<Integer> userIds, final FolderObject[] allSharedFolders, final String folderName, final Context ctx) throws OXFolderException {
        final List<Integer> affectedUsers = new ArrayList<Integer>();
        for (final FolderObject f : allSharedFolders) {
            if (null == f) {
                continue;
            }
            final List<OCLPermission> l = f.getPermissions();
            for (final OCLPermission permission : l) {
                if (permission.isGroupPermission()) {
                    /*
                     * Check against group members
                     */
                    try {
                        final int[] members = GroupStorage.getInstance().getGroup(permission.getEntity(), ctx).getMember();
                        for (int j = 0; j < members.length; j++) {
                            final Integer cur = Integer.valueOf(members[j]);
                            if (userIds.contains(cur) && f.getFolderName().equals(folderName)) {
                                affectedUsers.add(cur);
                                // TODO: Throw exception if bug #9111 says
                                // so
                            }
                        }
                    } catch (final LdapException e) {
                        LOG.error(e.getMessage(), e);
                    }
                } else {
                    /*
                     * Check against entity itself
                     */
                    final Integer cur = Integer.valueOf(permission.getEntity());
                    if (userIds.contains(cur) && f.getFolderName().equals(folderName)) {
                        affectedUsers.add(cur);
                        // TODO: Throw exception if bug #9111 says so
                    }
                }
            }
        }
        if (!affectedUsers.isEmpty()) {
            throw new OXFolderException(OXFolderException.FolderCode.SIMILAR_NAMED_SHARED_FOLDER, folderName);
        }
    }

    /**
     * Checks for invalid characters in folder name
     * 
     * @param checkMe The folder whose name shall be checked
     * @throws OXFolderException If folder name contains invalid characters
     */
    public static void checkFolderStringData(final FolderObject checkMe) throws OXFolderException {
        final String result;
        if (checkMe.containsFolderName() && (result = Check.containsInvalidChars(checkMe.getFolderName())) != null) {
            throw new OXFolderException(OXFolderException.FolderCode.INVALID_DATA, result);
        }
    }

    /**
     * Checks if permissions from given folder specify (at least) one folder admin and if creating user is the folder admin for his default
     * folders.
     * 
     * @param folderObj The folder
     * @param userId The user ID
     * @param ctx The context
     * @throws OXException If specified folder's permissions do not pass the checks
     */
    public static void checkFolderPermissions(final FolderObject folderObj, final int userId, final Context ctx) throws OXException {
        final boolean isPrivate = (folderObj.getType() == FolderObject.PRIVATE || folderObj.getType() == FolderObject.SHARED);
        int adminPermissionCount = 0;
        final int permissionsSize = folderObj.getPermissions().size();
        final Iterator<OCLPermission> iter = folderObj.getPermissions().iterator();
        final int creator = folderObj.containsCreatedBy() ? folderObj.getCreatedBy() : userId;
        final boolean isDefaultFolder = folderObj.containsDefaultFolder() ? folderObj.isDefaultFolder() : false;
        boolean creatorIsAdmin = false;
        for (int i = 0; i < permissionsSize; i++) {
            final OCLPermission oclPerm = iter.next();
            if (oclPerm.getEntity() < 0) {
                throw new OXFolderException(
                    OXFolderException.FolderCode.INVALID_ENTITY,
                    Integer.valueOf(oclPerm.getEntity()),
                    getFolderName(folderObj),
                    Integer.valueOf(ctx.getContextId()));
            }
            if (oclPerm.isFolderAdmin()) {
                adminPermissionCount++;
                if (isPrivate && folderObj.getModule() != FolderObject.SYSTEM_MODULE) {
                    checkPrivateAdminPerm(adminPermissionCount, creator, oclPerm);
                }
                if (isDefaultFolder && !creatorIsAdmin) {
                    creatorIsAdmin = (oclPerm.getEntity() == creator);
                }
            }
        }
        if (adminPermissionCount == 0) {
            throw new OXFolderLogicException(OXFolderException.FolderCode.NO_FOLDER_ADMIN);
        } else if (isDefaultFolder && !creatorIsAdmin) {
            throw new OXFolderException(OXFolderException.FolderCode.CREATOR_IS_NOT_ADMIN, getUserName(creator, ctx), getFolderName(
                folderObj.getObjectID(),
                ctx));
        }
    }

    private static void checkPrivateAdminPerm(final int adminPermissionCount, final int creator, final OCLPermission oclPerm) throws OXFolderLogicException {
        if (adminPermissionCount > 1) {
            throw new OXFolderLogicException(OXFolderException.FolderCode.ONLY_ONE_PRIVATE_FOLDER_ADMIN);
        }
        if (oclPerm.isGroupPermission()) {
            throw new OXFolderLogicException(OXFolderException.FolderCode.NO_PRIVATE_FOLDER_ADMIN_GROUP);
        }
        if (creator != oclPerm.getEntity()) {
            throw new OXFolderLogicException(OXFolderException.FolderCode.ONLY_PRIVATE_FOLDER_OWNER_ADMIN);
        }
    }

    /**
     * Ensures that an user who does not hold full shared folder access cannot share one of his private folders
     * 
     * @param folderObj The folder object
     * @param sessionUserConfig The session user's configuration
     * @param ctx The context
     * @throws OXException If an user tries to share a folder although he is not allowed to
     */
    public static void checkPermissionsAgainstSessionUserConfig(final FolderObject folderObj, final UserConfiguration sessionUserConfig, final Context ctx) throws OXException {
        final List<OCLPermission> perms = folderObj.getPermissions();
        final int size = perms.size();
        final Iterator<OCLPermission> iter = perms.iterator();
        final boolean isPrivate = (folderObj.getType() == FolderObject.PRIVATE);
        final boolean hasFullSharedFolderAccess = sessionUserConfig.hasFullSharedFolderAccess();
        for (int i = 0; i < size; i++) {
            final OCLPermission oclPerm = iter.next();
            if (!hasFullSharedFolderAccess && isPrivate && i > 0 && !isEmptyPermission(oclPerm)) {
                /*
                 * Prevent user from sharing a private folder cause he does not hold full shared folder access due to its user configuration
                 */
                throw new OXFolderException(
                    OXFolderException.FolderCode.SHARE_FORBIDDEN,
                    getUserName(sessionUserConfig.getUserId(), ctx),
                    getFolderName(folderObj),
                    Integer.valueOf(ctx.getContextId()));
            }
        }
    }

    private static boolean isEmptyPermission(final OCLPermission oclPerm) {
        return (!oclPerm.isFolderAdmin() && oclPerm.getFolderPermission() == OCLPermission.NO_PERMISSIONS && oclPerm.getReadPermission() == OCLPermission.NO_PERMISSIONS && oclPerm.getWritePermission() == OCLPermission.NO_PERMISSIONS && oclPerm.getDeletePermission() == OCLPermission.NO_PERMISSIONS);
    }

    /**
     * Checks system folder permissions.
     * 
     * @param folderId The folder ID
     * @param newPerms The update-operation permissions
     * @param ctx The context
     * @throws OXFolderException If changing system folder's permission is denied
     */
    public static void checkSystemFolderPermissions(final int folderId, final OCLPermission[] newPerms, final User user, final Context ctx) throws OXFolderException {
        if (folderId >= FolderObject.MIN_FOLDER_ID) {
            return;
        }
        final int[] allowedObjectPermissions = maxAllowedObjectPermissions(folderId);
        final int allowedFolderPermission = maxAllowedFolderPermission(folderId);
        final int admin = ctx.getMailadmin();
        if (FolderObject.SYSTEM_LDAP_FOLDER_ID == folderId) {
            for (int i = 0; i < newPerms.length; i++) {
                final OCLPermission newPerm = newPerms[i];
                if (newPerm.isGroupPermission()) {
                    final String i18nName = FolderObject.getFolderString(folderId, user.getLocale());
                    throw new OXFolderException(OXFolderException.FolderCode.NO_GROUP_PERMISSION, null == i18nName ? getFolderName(
                        folderId,
                        ctx) : i18nName, Integer.valueOf(ctx.getContextId()));
                }
                /*
                 * Only context admin may hold administer right and folder visibility change only
                 */
                checkSystemFolderObjectPermissions(folderId, newPerm, admin, allowedObjectPermissions, allowedFolderPermission, user, ctx);
            }
        } else {
            for (int i = 0; i < newPerms.length; i++) {
                final OCLPermission newPerm = newPerms[i];
                if (!newPerm.isGroupPermission() && newPerm.getEntity() != admin) {
                    final String i18nName = FolderObject.getFolderString(folderId, user.getLocale());
                    throw new OXFolderException(OXFolderException.FolderCode.NO_INDIVIDUAL_PERMISSION, null == i18nName ? getFolderName(
                        folderId,
                        ctx) : i18nName, Integer.valueOf(ctx.getContextId()));
                }
                /*
                 * Only context admin may hold administer right and folder visibility change only
                 */
                checkSystemFolderObjectPermissions(folderId, newPerm, admin, allowedObjectPermissions, allowedFolderPermission, user, ctx);
            }
        }
    }

    private static void checkSystemFolderObjectPermissions(final int folderId, final OCLPermission toCheck, final int admin, final int[] allowedObjectPermissions, final int maxAllowedFolderPermission, final User user, final Context ctx) throws OXFolderException {
        /*
         * Only context admin may hold administer right and folder visibility change only
         */
        if ((toCheck.getEntity() == admin ? !toCheck.isFolderAdmin() : toCheck.isFolderAdmin()) || !checkObjectPermissions(
            toCheck,
            allowedObjectPermissions) || toCheck.getFolderPermission() > maxAllowedFolderPermission) {
            final String i18nName = FolderObject.getFolderString(folderId, user.getLocale());
            throw new OXFolderException(OXFolderException.FolderCode.FOLDER_VISIBILITY_PERMISSION_ONLY, null == i18nName ? getFolderName(
                folderId,
                ctx) : i18nName, Integer.valueOf(ctx.getContextId()));
        }
    }

    private static boolean checkObjectPermissions(final OCLPermission p, final int[] allowedObjectPermissions) {
        return (p.getReadPermission() == allowedObjectPermissions[0]) && (p.getWritePermission() == allowedObjectPermissions[1]) && (p.getDeletePermission() == allowedObjectPermissions[2]);
    }

    private static final int[] NO_OBJECT_PERMISSIONS =
        { OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS };

    private static int[] maxAllowedObjectPermissions(final int folderId) {
        if (FolderObject.SYSTEM_LDAP_FOLDER_ID == folderId) {
            return new int[] {
                OCLPermission.READ_ALL_OBJECTS,
                OXFolderProperties.isEnableInternalUsersEdit() ? OCLPermission.WRITE_OWN_OBJECTS : OCLPermission.NO_PERMISSIONS,
                OCLPermission.NO_PERMISSIONS };
        } else if (FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID == folderId) {
            return NO_OBJECT_PERMISSIONS;
        } else {
            return NO_OBJECT_PERMISSIONS;
        }
    }

    private static int maxAllowedFolderPermission(final int folderId) {
        if (FolderObject.SYSTEM_LDAP_FOLDER_ID == folderId) {
            return OCLPermission.READ_FOLDER;
        } else if (FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID == folderId) {
            return OCLPermission.CREATE_SUB_FOLDERS;
        } else if (FolderObject.SYSTEM_PUBLIC_FOLDER_ID == folderId) {
            return OCLPermission.CREATE_SUB_FOLDERS;
        } else if (FolderObject.SYSTEM_INFOSTORE_FOLDER_ID == folderId) {
            return OCLPermission.READ_FOLDER;
        } else {
            return OCLPermission.NO_PERMISSIONS;
        }
    }

    /**
     * Gets the permissions without folder access by comparing specified storage-version permissions with update-operation permissions
     * 
     * @param newPerms The update-operation permissions
     * @param storagePerms The storage-version permissions
     * @return The permissions without folder access
     */
    public static OCLPermission[] getPermissionsWithoutFolderAccess(final OCLPermission[] newPerms, final OCLPermission[] storagePerms) {
        final List<OCLPermission> removed = new ArrayList<OCLPermission>(4);
        final Set<Integer> entities = new HashSet<Integer>(4);
        for (final OCLPermission storagePerm : storagePerms) {
            boolean found = false;
            for (int i = 0; i < newPerms.length && !found; i++) {
                if (newPerms[i].getEntity() == storagePerm.getEntity()) {
                    /*
                     * Still present in new permissions
                     */
                    found = (newPerms[i].getFolderPermission() > OCLPermission.NO_PERMISSIONS);
                }
            }
            if (!found) {
                removed.add(storagePerm);
                entities.add(Integer.valueOf(storagePerm.getEntity()));
            }
        }
        for (final OCLPermission newPerm : newPerms) {
            boolean found = false;
            for (int i = 0; i < storagePerms.length && !found; i++) {
                if (newPerm.getEntity() == storagePerms[i].getEntity()) {
                    found = true;
                }
            }
            if (!found && newPerm.getFolderPermission() <= OCLPermission.NO_PERMISSIONS && !entities.contains(Integer.valueOf(newPerm.getEntity()))) {
                /*
                 * A newly added permission which grants no folder-read access
                 */
                removed.add(newPerm);
            }
        }
        return removed.toArray(new OCLPermission[removed.size()]);
    }

    /**
     * Checks every <b>user permission</b> against user configuration settings
     * 
     * @param folderObj The folder object
     * @param ctx The context
     * @throws OXException If a composed permission does not obey user's configuration
     */
    public static void checkPermissionsAgainstUserConfigs(final FolderObject folderObj, final Context ctx) throws OXException {
        final int size = folderObj.getPermissions().size();
        final Iterator<OCLPermission> iter = folderObj.getPermissions().iterator();
        final UserConfigurationStorage userConfigStorage = UserConfigurationStorage.getInstance();
        for (int i = 0; i < size; i++) {
            final OCLPermission assignedPerm = iter.next();
            if (!assignedPerm.isGroupPermission()) {
                final OCLPermission maxApplicablePerm =
                    getMaxApplicablePermission(folderObj, userConfigStorage.getUserConfiguration(assignedPerm.getEntity(), ctx));
                if (!isApplicable(maxApplicablePerm, assignedPerm)) {
                    throw new OXFolderException(
                        OXFolderException.FolderCode.UNAPPLICABLE_FOLDER_PERM,
                        getUserName(assignedPerm.getEntity(), ctx),
                        getFolderName(folderObj),
                        Integer.valueOf(ctx.getContextId()));
                }
            }
        }
    }

    private static OCLPermission getMaxApplicablePermission(final FolderObject folderObj, final UserConfiguration userConfig) {
        final EffectivePermission retval =
            new EffectivePermission(
                userConfig.getUserId(),
                folderObj.getObjectID(),
                folderObj.getType(userConfig.getUserId()),
                folderObj.getModule(),
                userConfig);
        retval.setFolderAdmin(true);
        retval.setAllPermission(
            OCLPermission.ADMIN_PERMISSION,
            OCLPermission.ADMIN_PERMISSION,
            OCLPermission.ADMIN_PERMISSION,
            OCLPermission.ADMIN_PERMISSION);
        return retval;
    }

    private static boolean isApplicable(final OCLPermission maxApplicablePerm, final OCLPermission assignedPerm) {
        if (!maxApplicablePerm.isFolderAdmin() && assignedPerm.isFolderAdmin()) {
            return false;
        }
        return (maxApplicablePerm.getFolderPermission() >= assignedPerm.getFolderPermission() && maxApplicablePerm.getReadPermission() >= assignedPerm.getReadPermission() && maxApplicablePerm.getWritePermission() >= assignedPerm.getWritePermission() && maxApplicablePerm.getDeletePermission() >= assignedPerm.getDeletePermission());
    }

    /**
     * This routine ensures that owner of parental shared folder gets full access (incl. folder admin) to shared subfolder
     * 
     * @param parentOwner The user ID of parent folder owner
     * @param folderObj The shared subfolder
     * @param userId The user ID
     * @param ctx The context
     * @throws OXException If permission check fails
     */
    public static void checkSharedSubfolderOwnerPermission(final FolderObject parent, final FolderObject folderObj, final int userId, final Context ctx) throws OXException {
        final List<OCLPermission> ocls = folderObj.getPermissions();
        final int size = ocls.size();
        /*
         * Look for existing permissions for parent owner
         */
        boolean pownerFound = false;
        for (int i = 0; i < size; i++) {
            final OCLPermission cur = ocls.get(i);
            if (cur.getEntity() == parent.getCreatedBy()) {
                /*
                 * In any case grant full access
                 */
                cur.setFolderAdmin(true);
                cur.setAllPermission(
                    OCLPermission.ADMIN_PERMISSION,
                    OCLPermission.ADMIN_PERMISSION,
                    OCLPermission.ADMIN_PERMISSION,
                    OCLPermission.ADMIN_PERMISSION);
                pownerFound = true;
            } else if (cur.isFolderAdmin()) {
                throw new OXFolderException(
                    OXFolderException.FolderCode.INVALID_SHARED_FOLDER_SUBFOLDER_PERMISSION,
                    getUserName(userId, ctx),
                    getFolderName(folderObj),
                    Integer.valueOf(ctx.getContextId()),
                    getFolderName(folderObj),
                    Integer.valueOf(ctx.getContextId()),
                    getFolderName(parent));
            }
        }
        if (!pownerFound) {
            /*
             * Add full permission for parent folder owner
             */
            final OCLPermission pownerPerm = new OCLPermission();
            pownerPerm.setEntity(parent.getCreatedBy());
            pownerPerm.setFolderAdmin(true);
            pownerPerm.setAllPermission(
                OCLPermission.ADMIN_PERMISSION,
                OCLPermission.ADMIN_PERMISSION,
                OCLPermission.ADMIN_PERMISSION,
                OCLPermission.ADMIN_PERMISSION);
            ocls.add(pownerPerm);
        }
        folderObj.setPermissionsNoClone(ocls);
    }

    private static final int[] SORTED_STD_MODULES =
        { FolderObject.TASK, FolderObject.CALENDAR, FolderObject.CONTACT, FolderObject.UNBOUND };

    /**
     * Checks specified new folder module against parent folder
     * 
     * @param parentId The parent folder ID
     * @param parentModule The parent module
     * @param newFolderModule The new folder module
     * @param cid The context ID
     * @return <code>true</code> if parent folder allows specified new folder module; otherwise <code>false</code>
     * @throws OXException If specified module is unknown
     */
    public static boolean checkFolderModuleAgainstParentModule(final int parentId, final int parentModule, final int newFolderModule, final int cid) throws OXException {
        if (parentModule == FolderObject.TASK || parentModule == FolderObject.CALENDAR || parentModule == FolderObject.CONTACT) {
            return (Arrays.binarySearch(SORTED_STD_MODULES, newFolderModule) >= 0);
        } else if (parentModule == FolderObject.SYSTEM_MODULE) {
            if (parentId == FolderObject.SYSTEM_PRIVATE_FOLDER_ID || parentId == FolderObject.SYSTEM_PUBLIC_FOLDER_ID) {
                return (Arrays.binarySearch(SORTED_STD_MODULES, newFolderModule) >= 0);
            } else if (parentId == FolderObject.SYSTEM_INFOSTORE_FOLDER_ID) {
                return (newFolderModule == FolderObject.INFOSTORE);
            }
        } else if (parentModule == FolderObject.PROJECT) {
            return (newFolderModule == FolderObject.PROJECT);
        } else if (parentModule == FolderObject.INFOSTORE) {
            return (newFolderModule == FolderObject.INFOSTORE);
        } else {
            throw new OXFolderException(OXFolderException.FolderCode.UNKNOWN_MODULE, Integer.valueOf(parentModule), Integer.valueOf(cid));
        }
        return true;
    }

    /**
     * Checks specified folder type against parent folder
     * 
     * @param parentFolder The parent folder
     * @param newFolderType The folder type
     * @return <code>true</code> if parent allows specified folder type; otherwise <code>false</code>
     */
    public static boolean checkFolderTypeAgainstParentType(final FolderObject parentFolder, final int newFolderType) {
        final int enforcedType;
        switch (parentFolder.getObjectID()) {
        case FolderObject.SYSTEM_PRIVATE_FOLDER_ID:
            enforcedType = FolderObject.PRIVATE;
            break;
        case FolderObject.SYSTEM_PUBLIC_FOLDER_ID:
            enforcedType = FolderObject.PUBLIC;
            break;
        case FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID:
            enforcedType = FolderObject.PUBLIC;
            break;
        case FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID:
            enforcedType = FolderObject.PUBLIC;
            break;
        default:
            enforcedType = parentFolder.getType();
        }
        return (newFolderType == enforcedType);
    }

    /**
     * Checks if specified folder ID is a descendant folder
     * 
     * @param parentIDList The parent IDs
     * @param possibleDescendant The ID of possible descendant folder
     * @param readCon A connection with read-only capability
     * @param ctx The context
     * @return <code>true</code> if specified folder ID is a descendant folder; otherwise <code>false</code>
     * @throws SQLException If a SQL error occurs
     * @throws DBPoolingException If a pooling error occurs
     */
    public static boolean isDescendentFolder(final TIntArrayList parentIDList, final int possibleDescendant, final Connection readCon, final Context ctx) throws SQLException, DBPoolingException {
        final int size = parentIDList.size();
        boolean isDescendant = false;
        for (int i = 0; i < size && !isDescendant; i++) {
            final TIntArrayList subfolderIDs = OXFolderSQL.getSubfolderIDs(parentIDList.getQuick(i), readCon, ctx);
            final int subsize = subfolderIDs.size();
            for (int j = 0; j < subsize && !isDescendant; j++) {
                final int current = subfolderIDs.getQuick(j);
                isDescendant |= (current == possibleDescendant);
            }
            if (isDescendant) {
                /*
                 * Matching descendant found
                 */
                return true;
            }
            /*
             * Recursive call with collected subfolder ids
             */
            isDescendant = isDescendentFolder(subfolderIDs, possibleDescendant, readCon, ctx);
        }
        return isDescendant;
    }

    /**
     * Checks if given update object indicates to perform a rename-only operation compared to storage object.
     * 
     * @param updateObject The update object
     * @param storageObject The storage object
     * @return <code>true</code> if given update object indicates to perform a rename-only operation; otherwise <code>false</code>
     */
    public static boolean isRenameOnly(final FolderObject updateObject, final FolderObject storageObject) {
        if (!updateObject.containsFolderName()) {
            return false;
        }
        final String newName = updateObject.getFolderName();
        if (null == newName || newName.equals(storageObject.getFolderName())) {
            return false;
        }
        /*-
         * Ok, folder name differs. Check other fields.
         * 
         * Permissions
         */
        if (updateObject.containsPermissions()) {
            final List<OCLPermission> updateList = Arrays.asList(updateObject.getNonSystemPermissionsAsArray());
            if (null != updateList && isDifferent(Arrays.asList(storageObject.getNonSystemPermissionsAsArray()), updateList)) {
                return false;
            }
        }
        /*
         * Parent
         */
        if (updateObject.containsParentFolderID()) {
            final int updateParent = updateObject.getParentFolderID();
            if (updateParent > 0 && storageObject.getParentFolderID() != updateParent) {
                return false;
            }
        }
        /*
         * Module
         */
        if (updateObject.containsModule()) {
            final int updateModule = updateObject.getModule();
            if (updateModule > 0 && storageObject.getModule() != updateModule) {
                return false;
            }
        }
        /*
         * Rename only
         */
        return true;
    }

    /**
     * Checks if specified permission lists are different.
     * 
     * @param storageList The storage-version permissions
     * @param updateList The update-version permissions
     * @return <code>true</code> if different; otherwise <code>false</code>
     */
    public static boolean isDifferent(final List<OCLPermission> storageList, final List<OCLPermission> updateList) {
        if (updateList.isEmpty()) {
            return false;
        }
        final int ssize = storageList.size();
        for (final OCLPermission update : updateList) {
            boolean found = false;
            for (int i = 0; i < ssize && !found; i++) {
                final OCLPermission storage = storageList.get(i);
                if (storage.getEntity() == update.getEntity()) {
                    found = true;
                    if (!update.equalsPermission(storage)) {
                        return true;
                    }

                }
            }
            if (!found) {
                return true;
            }
        }
        /*
         * Same
         */
        return false;
    }

    /**
     * Gets those entities which are new or updated in given update list compared to given storage list or whole update list if storage list
     * is <code>null</code>.
     * 
     * @param storageList The storage list of permissions (if <code>null</code> whole update list entities are added)
     * @param updateList The update list of permissions
     * @param user The user ID
     * @return A set of <code>int</code> containing share user IDs.
     */
    public static Set<Integer> getShareUsers(final List<OCLPermission> storageList, final List<OCLPermission> updateList, final int user, final Context ctx) {
        final Set<Integer> retval = new HashSet<Integer>();
        if (null == storageList) {
            for (final OCLPermission update : updateList) {
                addPermissionUsers(update, retval, ctx);
            }
        } else {
            final int ssize = storageList.size();
            for (final OCLPermission update : updateList) {
                boolean found = false;
                for (int i = 0; i < ssize && !found; i++) {
                    final OCLPermission storage = storageList.get(i);
                    if (storage.getEntity() == update.getEntity()) {
                        found = true;
                        if (!update.equalsPermission(storage)) {
                            addPermissionUsers(update, retval, ctx);
                        }

                    }
                }
                if (!found) {
                    addPermissionUsers(update, retval, ctx);
                }
            }
        }
        /*
         * Remove user ID...
         */
        retval.remove(Integer.valueOf(user));
        /*
         * ... and return set
         */
        return retval;
    }

    /**
     * Adds permission-associated user ID or group member IDs to specified set dependent on whether given permission denotes an user or a
     * group permission.
     * 
     * @param permission The permission
     * @param users The set of user IDs
     * @param ctx The context (possibly needed to resolve group)
     */
    private static void addPermissionUsers(final OCLPermission permission, final Set<Integer> users, final Context ctx) {
        if (permission.isGroupPermission()) {
            /*
             * Resolve group
             */
            try {
                final int[] members = GroupStorage.getInstance().getGroup(permission.getEntity(), ctx).getMember();
                for (int j = 0; j < members.length; j++) {
                    users.add(Integer.valueOf(members[j]));
                }
            } catch (final LdapException e) {
                LOG.error(e.getMessage(), e);
            }
        } else {
            users.add(Integer.valueOf(permission.getEntity()));
        }
    }

    /**
     * Gets the folder name for logging/messaging purpose
     * 
     * @param fo The folder
     * @return The folder name for logging/messaging purpose
     */
    public static String getFolderName(final FolderObject fo) {
        return new StringBuilder().append(fo.getFolderName() == null ? STR_EMPTY : fo.getFolderName()).append(" (").append(fo.getObjectID()).append(
            ')').toString();
    }

    /**
     * Gets the folder name for logging/messaging purpose
     * 
     * @param folderId The folder ID
     * @param ctx The context
     * @return The folder name for logging/messaging purpose
     */
    public static String getFolderName(final int folderId, final Context ctx) {
        try {
            return new StringBuilder().append(new OXFolderAccess(ctx).getFolderName(folderId)).append(" (").append(folderId).append(')').toString();
        } catch (final OXException e) {
            return String.valueOf(folderId);
        }
    }

    /**
     * Gets the user name for logging/messaging purpose.
     * 
     * @param session The session
     * @param u The user
     * @return The user name for logging/messaging purpose.
     */
    public static String getUserName(final Session session, final User u) {
        if (session == null) {
            return STR_EMPTY;
        }
        if (u.getDisplayName() == null) {
            return new StringBuilder().append(u.getGivenName()).append(' ').append(u.getSurname()).append(" (").append(u.getId()).append(
                ')').toString();
        }
        return new StringBuilder().append(u.getDisplayName()).append(" (").append(u.getId()).append(')').toString();
    }

    /**
     * Gets the user name for logging/messaging purpose.
     * 
     * @param userId The user ID
     * @param ctx The context
     * @return The user name for logging/messaging purpose.
     */
    public static String getUserName(final int userId, final Context ctx) {
        final User u;
        try {
            u = UserStorage.getInstance().getUser(userId, ctx);
        } catch (final LdapException e) {
            return String.valueOf(userId);
        }
        if (u == null) {
            return String.valueOf(userId);
        } else if (u.getDisplayName() == null) {
            return new StringBuilder().append(u.getGivenName()).append(' ').append(u.getSurname()).append(" (").append(userId).append(')').toString();
        }
        return new StringBuilder().append(u.getDisplayName()).append(" (").append(userId).append(')').toString();
    }

    /**
     * Gets the user name for logging/messaging purpose.
     * 
     * @param session The server session
     * @return The user name for logging/messaging purpose.
     */
    public static String getUserName(final ServerSession session) {
        final User u = session.getUser();
        if (u.getDisplayName() == null) {
            return new StringBuilder().append(u.getGivenName()).append(' ').append(u.getSurname()).append(" (").append(u.getId()).append(
                ')').toString();
        }
        return new StringBuilder().append(u.getDisplayName()).append(" (").append(u.getId()).append(')').toString();
    }

    /**
     * Gets the group name for logging/messaging purpose.
     * 
     * @param groupId The group ID
     * @param ctx The context
     * @return The group name for logging/messaging purpose.
     */
    public static String getGroupName(final int groupId, final Context ctx) {
        final Group g;
        try {
            g = GroupStorage.getInstance().getGroup(groupId, ctx);
        } catch (final LdapException e) {
            return String.valueOf(groupId);
        }
        if (g == null) {
            return String.valueOf(groupId);
        } else if (g.getDisplayName() == null) {
            return new StringBuilder().append(g.getSimpleName()).append(" (").append(groupId).append(')').toString();
        }
        return new StringBuilder().append(g.getDisplayName()).append(" (").append(groupId).append(')').toString();
    }

    private static final String STR_TYPE_PRIVATE = "'private'";

    private static final String STR_TYPE_PUBLIC = "'public'";

    private static final String STR_SYSTEM = "'system'";

    private static final String STR_UNKNOWN = "'unknown'";

    /**
     * Gets the given type's string representation
     * 
     * @param type The type
     * @return The type's string representation
     */
    public static String folderType2String(final int type) {
        switch (type) {
        case FolderObject.PRIVATE:
            return STR_TYPE_PRIVATE;
        case FolderObject.PUBLIC:
            return STR_TYPE_PUBLIC;
        case FolderObject.SYSTEM_TYPE:
            return STR_SYSTEM;
        default:
            return STR_UNKNOWN;
        }
    }

    private static final String STR_MODULE_CALENDAR = "'calendar'";

    private static final String STR_MODULE_TASK = "'task'";

    private static final String STR_MODULE_CONTACT = "'contact'";

    private static final String STR_MODULE_UNBOUND = "'unbound'";

    private static final String STR_MODULE_PROJECT = "'project'";

    private static final String STR_MODULE_INFOSTORE = "'infostore'";

    /**
     * Gets the given module's string representation
     * 
     * @param module The module
     * @return The module's string representation
     */
    public static String folderModule2String(final int module) {
        switch (module) {
        case FolderObject.CALENDAR:
            return STR_MODULE_CALENDAR;
        case FolderObject.TASK:
            return STR_MODULE_TASK;
        case FolderObject.CONTACT:
            return STR_MODULE_CONTACT;
        case FolderObject.UNBOUND:
            return STR_MODULE_UNBOUND;
        case FolderObject.SYSTEM_MODULE:
            return STR_SYSTEM;
        case FolderObject.PROJECT:
            return STR_MODULE_PROJECT;
        case FolderObject.INFOSTORE:
            return STR_MODULE_INFOSTORE;
        default:
            return STR_UNKNOWN;
        }
    }

}
