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

package com.openexchange.share.core.tools;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserImpl;
import com.openexchange.groupware.modules.Module;
import com.openexchange.groupware.userconfiguration.Permission;
import com.openexchange.groupware.userconfiguration.UserConfigurationCodes;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.guest.GuestService;
import com.openexchange.java.Strings;
import com.openexchange.passwordmechs.PasswordMech;
import com.openexchange.server.ServiceLookup;
import com.openexchange.share.AuthenticationMode;
import com.openexchange.share.Share;
import com.openexchange.share.ShareCryptoService;
import com.openexchange.share.ShareExceptionCodes;
import com.openexchange.share.ShareTarget;
import com.openexchange.share.recipient.AnonymousRecipient;
import com.openexchange.share.recipient.GuestRecipient;
import com.openexchange.share.recipient.ShareRecipient;
import com.openexchange.userconf.UserConfigurationService;
import com.openexchange.userconf.UserPermissionService;

/**
 * {@link ShareTool}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public class ShareTool {

    /**
     * Extracts the first value of a specific attribute from a user.
     *
     * @param user The user to get the attribute value for
     * @param name The name of the attribute to get
     * @return The first found attribute value, or <code>null</code> if not found
     */
    public static String getUserAttribute(User user, String name) {
        Map<String, Set<String>> attributes = user.getAttributes();
        if (attributes == null) {
            return null;
        }

        Set<String> match = attributes.get(name);
        if (match == null || match.isEmpty()) {
            return null;
        }

        return match.iterator().next();
    }

    /**
     * Gets permission bits suitable for a guest user being allowed to access all supplied share targets. Besides the concrete module
     * permission(s), this includes the permission bits to access shared and public folders, as well as the bit to turn off portal
     * access.
     *
     * @param recipient The share recipient
     * @param targets The share targets
     * @return The permission bits
     * @throws OXException
     */
    public static int getRequiredPermissionBits(ShareRecipient recipient, List<ShareTarget> targets) throws OXException {
        Set<Integer> modules = new HashSet<Integer>(targets.size());
        for (ShareTarget target : targets) {
            modules.add(target.getModule());
        }
        return getRequiredPermissionBits(getAuthenticationMode(recipient), modules);
    }

    /**
     * Gets permission bits suitable for a guest user being allowed to access all supplied modules. Besides the concrete module
     * permission(s), this includes the permission bits to access shared and public folders, as well as the bit to turn off portal
     * access.
     *
     * @param guest The guest user
     * @param modules The module identifiers
     * @return The permission bits
     * @throws OXException
     */
    public static int getRequiredPermissionBits(User guest, Collection<Integer> modules) throws OXException {
        return getRequiredPermissionBits(getAuthenticationMode(guest), modules);
    }

    /**
     * Gets permission bits suitable for a guest user being allowed to access all supplied modules. Besides the concrete module
     * permission(s), this includes the permission bits to access shared and public folders, as well as the bit to turn off portal
     * access.
     *
     * @param guest The guest user
     * @param modules The module identifiers
     * @return The permission bits
     * @throws OXException
     */
    private static int getRequiredPermissionBits(AuthenticationMode authentication, Collection<Integer> modules) throws OXException {
        Set<Permission> perms = new HashSet<Permission>(8);
        perms.add(Permission.DENIED_PORTAL);
        perms.add(Permission.EDIT_PUBLIC_FOLDERS);
        perms.add(Permission.READ_CREATE_SHARED_FOLDERS);
        if (AuthenticationMode.GUEST == authentication || AuthenticationMode.GUEST_PASSWORD == authentication) {
            perms.add(Permission.EDIT_PASSWORD);
        }
        for (Integer module : modules) {
            addModulePermissions(perms, module.intValue());
        }
        return Permission.toBits(perms);
    }

    /**
     * Adds a module permission to the supplied permission set.
     *
     * @param perms The permission set
     * @param module The module to add the permissions for
     * @return The adjusted permission set
     * @throws OXException
     */
    private static Set<Permission> addModulePermissions(Set<Permission> perms, int module) throws OXException {
        Module matchingModule = Module.getForFolderConstant(module);
        if (null != matchingModule) {
            Permission modulePermission = matchingModule.getPermission();
            if (null == modulePermission) {
                throw ShareExceptionCodes.UNEXPECTED_ERROR.create("No module permission for module " + matchingModule);
            }
            perms.add(modulePermission);
        }
        return perms;
    }

    /**
     * Prepares a new share.
     *
     * @param contextID The context ID
     * @param sharingUser The sharing user
     * @param guestUserID The guest user ID
     * @param target The share target
     * @return The share
     */
    public static Share prepareShare(int contextID, User sharingUser, int guestUserID, ShareTarget target) {
        Date now = new Date();
        Share share = new Share();
        share.setTarget(target);
        share.setCreated(now);
        share.setModified(now);
        share.setCreatedBy(sharingUser.getId());
        share.setModifiedBy(sharingUser.getId());
        share.setGuest(guestUserID);
        return share;
    }

    /**
     * Prepares a guest user instance based on the supplied share recipient.
     *
     * @param services The service lookup reference
     * @param sharingUser The sharing user
     * @param recipient The recipient description
     * @return The guest user
     */
    public static UserImpl prepareGuestUser(ServiceLookup services, int contextId, User sharingUser, ShareRecipient recipient) throws OXException {
        if (AnonymousRecipient.class.isInstance(recipient)) {
            return prepareGuestUser(services, sharingUser, (AnonymousRecipient) recipient);
        } else if (GuestRecipient.class.isInstance(recipient)) {
            return prepareGuestUser(services, contextId, sharingUser, (GuestRecipient) recipient);
        } else {
            throw ShareExceptionCodes.UNEXPECTED_ERROR.create("unsupported share recipient: " + recipient);
        }
    }

    /**
     * Prepares a (named) guest user instance. If no password is defined in the supplied guest recipient, an auto-generated one is used.
     *
     * @param services The service lookup reference
     * @param sharingUser The sharing user
     * @param recipient The recipient description
     * @return The guest user
     */
    private static UserImpl prepareGuestUser(ServiceLookup services, int contextId, User sharingUser, GuestRecipient recipient) throws OXException {
        String groupId = services.getService(ConfigViewFactory.class).getView(sharingUser.getId(), contextId).opt("com.openexchange.context.group", String.class, "default");
        /*
         * try to lookup & reuse data from existing guest in other context via guest service
         */
        UserImpl copiedUser = services.getService(GuestService.class).createUserCopy(recipient.getEmailAddress(), groupId, contextId);
        if (copiedUser != null) {
            return prepareGuestUser(sharingUser, copiedUser);
        }
        /*
         * prepare new guest user for recipient & set "was created" marker
         */
        UserImpl guestUser = prepareGuestUser(sharingUser);
        guestUser.setDisplayName(recipient.getDisplayName());
        guestUser.setMail(recipient.getEmailAddress());
        guestUser.setLoginInfo(recipient.getEmailAddress());
        guestUser.setPasswordMech(PasswordMech.BCRYPT.getIdentifier());
        if (Strings.isNotEmpty(recipient.getPassword())) {
            try {
                guestUser.setUserPassword(PasswordMech.BCRYPT.encode(recipient.getPassword()));
            } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                throw ShareExceptionCodes.UNEXPECTED_ERROR.create(e, "Could not encode new password for guest user");
            }
        }
        return guestUser;
    }

    /**
     * Prepares an anonymous guest user instance.
     *
     * @param services The service lookup reference
     * @param sharingUser The sharing user
     * @param recipient The recipient description
     * @return The guest user
     */
    private static UserImpl prepareGuestUser(ServiceLookup services, User sharingUser, AnonymousRecipient recipient) throws OXException {
        UserImpl guestUser = prepareGuestUser(sharingUser);
        guestUser.setDisplayName("Guest");
        guestUser.setMail("");
        if (null != recipient.getPassword()) {
            guestUser.setUserPassword(services.getService(ShareCryptoService.class).encrypt(recipient.getPassword()));
            guestUser.setPasswordMech(ShareCryptoService.PASSWORD_MECH_ID);
        } else {
            guestUser.setPasswordMech("");
        }
        return guestUser;
    }

    /**
     * Prepares a guest user instance based on a "parent" sharing user.
     *
     * @param sharingUser The sharing user
     * @return The guest user
     */
    private static UserImpl prepareGuestUser(User sharingUser) {
        UserImpl guestUser = new UserImpl();
        guestUser.setCreatedBy(sharingUser.getId());
        guestUser.setPreferredLanguage(sharingUser.getPreferredLanguage());
        guestUser.setTimeZone(sharingUser.getTimeZone());
        guestUser.setMailEnabled(true);
        ShareToken.assignBaseToken(guestUser);
        return guestUser;
    }

    /**
     * Prepares a guest user instance based on a "parent" sharing user.
     *
     * @param sharingUser The sharing user
     * @param guestUser The existing guest user to prepare
     * @return The guest user
     */
    private static UserImpl prepareGuestUser(User sharingUser, UserImpl guestUser) {
        if (guestUser == null) {
            return prepareGuestUser(sharingUser);
        }
        guestUser.setCreatedBy(sharingUser.getId());
        guestUser.setPreferredLanguage(sharingUser.getPreferredLanguage());
        guestUser.setTimeZone(sharingUser.getTimeZone());
        guestUser.setMailEnabled(true);
        ShareToken.assignBaseToken(guestUser);
        return guestUser;
    }

    /**
     * Prepares a user contact for a guest user.
     *
     * @param services The service lookup reference
     * @param contextId The context identifier
     * @param sharingUser The sharing user
     * @param guestUser The guest user
     * @return The guest contact
     */
    public static Contact prepareGuestContact(ServiceLookup services, int contextId, User sharingUser, User guestUser) throws OXException {
        String groupId = services.getService(ConfigViewFactory.class).getView(sharingUser.getId(), contextId).opt("com.openexchange.context.group", String.class, "default");
        /*
         * try to lookup & reuse data from existing guest in other context via guest service
         */
        Contact copiedContact = services.getService(GuestService.class).createContactCopy(guestUser.getMail(), groupId, contextId, guestUser.getId());
        if (null != copiedContact) {
            return copiedContact;
        }
        /*
         * prepare new contact for recipient
         */
        Contact contact = new Contact();
        contact.setParentFolderID(FolderObject.VIRTUAL_GUEST_CONTACT_FOLDER_ID);
        contact.setCreatedBy(sharingUser.getId());
        contact.setDisplayName(guestUser.getDisplayName());
        contact.setEmail1(guestUser.getMail());
        return contact;
    }

    /**
     * Filters out all expired shares from the supplied list.
     *
     * @param shares The shares to filter
     * @return The expired shares that were removed from the supplied list, or <code>null</code> if no shares were expired
     */
    public static List<Share> filterExpiredShares(List<Share> shares) {
        List<Share> expiredShares = null;
        if (null != shares && 0 < shares.size()) {
            Iterator<Share> iterator = shares.iterator();
            while (iterator.hasNext()) {
                Share share = iterator.next();
                if (null != share.getTarget() && share.getTarget().isExpired()) {
                    if (null == expiredShares) {
                        expiredShares = new ArrayList<Share>();
                    }
                    iterator.remove();
                    expiredShares.add(share);
                }
            }
        }
        return expiredShares;
    }

    /**
     * Gets all identifiers specified in the supplied shares.
     *
     * @param shares The shares to get the guest users for
     * @return The guest user identifiers in a set
     */
    public static Set<Integer> getGuestIDs(List<Share> shares) {
        if (null == shares || 0 == shares.size()) {
            return Collections.emptySet();
        }
        Set<Integer> guestIDs = new HashSet<Integer>();
        for (Share share : shares) {
            guestIDs.add(Integer.valueOf(share.getGuest()));
        }
        return guestIDs;
    }

    /**
     * Maps each different target of the supplied shares to one or more referenced guest user identifiers.
     *
     * @param shares The shares to perform the mapping for
     * @return All different share targets, mapped to the referenced guest user identifiers.
     */
    public static Map<ShareTarget, Set<Integer>> mapGuestsByTarget(List<Share> shares) {
        if (null == shares || 0 == shares.size()) {
            return Collections.emptyMap();
        }
        Map<ShareTarget, Set<Integer>> guestsByTarget = new HashMap<ShareTarget, Set<Integer>>();
        for (Share share : shares) {
            Set<Integer> guestIDs = guestsByTarget.get(share.getTarget());
            if (null == guestIDs) {
                guestIDs = new HashSet<Integer>();
                guestsByTarget.put(share.getTarget(), guestIDs);
            }
            guestIDs.add(Integer.valueOf(share.getGuest()));
        }
        return guestsByTarget;
    }

    /**
     * Checks a share target for validity before saving or updating it, throwing an exception if validation fails.
     *
     * @param target The target to validate
     * @throws OXException
     */
    public static void validateTarget(ShareTarget target) throws OXException {
        if (0 == target.getOwnedBy()) {
            throw ShareExceptionCodes.UNEXPECTED_ERROR.create("No owned by information specified in share target");
        }
        if (null == target.getItem() && null == target.getFolder()) {
            throw ShareExceptionCodes.UNEXPECTED_ERROR.create("No folder or item specified in share target");
        }
    }

    /**
     * Checks a share target for validity before saving or updating it, throwing an exception if validation fails.
     *
     * @param targets The targets to validate
     * @throws OXException
     */
    public static void validateTargets(Collection<ShareTarget> targets) throws OXException {
        for (ShareTarget target : targets) {
            validateTarget(target);
        }
    }

    /**
     * Sets a user's permission bits. This includes assigning initial permission bits, as well as updating already existing permissions.
     *
     * @param services A service lookup reference
     * @param connection The database connection to use
     * @param context The context
     * @param userID The identifier of the user to set the permission bits for
     * @param permissionBits The permission bits to set
     * @param merge <code>true</code> to merge with the previously assigned permissions, <code>false</code> to overwrite
     * @return The updated permission bits
     * @throws OXException
     */
    public static UserPermissionBits setPermissionBits(ServiceLookup services, Connection connection, Context context, int userID, int permissionBits, boolean merge) throws OXException {
        UserPermissionService userPermissionService = services.getService(UserPermissionService.class);
        UserPermissionBits userPermissionBits = null;
        try {
            userPermissionBits = userPermissionService.getUserPermissionBits(connection, userID, context);
        } catch (OXException e) {
            if (false == UserConfigurationCodes.NOT_FOUND.equals(e)) {
                throw e;
            }
        }
        if (null == userPermissionBits) {
            /*
             * save permission bits
             */
            userPermissionBits = new UserPermissionBits(permissionBits, userID, context.getContextId());
            userPermissionService.saveUserPermissionBits(connection, userPermissionBits);
        } else if (userPermissionBits.getPermissionBits() != permissionBits) {
            /*
             * update permission bits
             */
            userPermissionBits.setPermissionBits(merge ? permissionBits | userPermissionBits.getPermissionBits() : permissionBits);
            userPermissionService.saveUserPermissionBits(connection, userPermissionBits);
            /*
             * invalidate affected user configuration
             */
            services.getService(UserConfigurationService.class).removeUserConfiguration(userID, context);
        }
        return userPermissionBits;
    }

    /**
     * Gets the authentication mode applicable for the supplied guest user.
     *
     * @param guest The guest user
     * @return The authentication mode
     */
    public static AuthenticationMode getAuthenticationMode(User guest) {
        if (Strings.isEmpty(guest.getMail())) {
            if (guest.getUserPassword() == null) {
                return AuthenticationMode.ANONYMOUS;
            } else {
                return AuthenticationMode.ANONYMOUS_PASSWORD;
            }
        } else {
            if (guest.getUserPassword() == null) {
                return AuthenticationMode.GUEST;
            } else {
                return AuthenticationMode.GUEST_PASSWORD;
            }
        }
    }

    /**
     * Gets the authentication mode applicable for the supplied share recipient.
     *
     * @param guest The guest user
     * @return The authentication mode, or <code>null</code> if the recipient does not denote guest authentication
     */
    public static AuthenticationMode getAuthenticationMode(ShareRecipient recipient) {
        switch (recipient.getType()) {
            case ANONYMOUS:
                return Strings.isEmpty(((AnonymousRecipient) recipient).getPassword()) ?
                    AuthenticationMode.ANONYMOUS : AuthenticationMode.ANONYMOUS_PASSWORD;
            case GUEST:
                if (((GuestRecipient) recipient).getPassword() == null) {
                    return AuthenticationMode.GUEST;
                }
                return AuthenticationMode.GUEST_PASSWORD;
            default:
                return null;
        }
    }

    /**
     * Checks whether the passed user is a guest user and its authentication mode is  either
     * {@link AuthenticationMode#ANONYMOUS} or {@link AuthenticationMode#ANONYMOUS_PASSWORD}.
     *
     * @param user The user to check
     * @return <code>true</code> if the user is an anonymous guest
     */
    public static boolean isAnonymousGuest(User user) {
        if (user.isGuest() && Strings.isEmpty(user.getMail())) {
            return true;
        }

        return false;
    }

}
