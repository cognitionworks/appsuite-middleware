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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.mail.dataobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.mail.permission.DefaultMailPermission;
import com.openexchange.mail.permission.MailPermission;

/**
 * {@link MailFolder} - a data container object for a mail folder
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class MailFolder implements Serializable {

    /**
     * Serial Version UID
     */
    private static final long serialVersionUID = -8203697938992090309L;

    private String name;

    private boolean b_name;

    private String fullname;

    private boolean b_fullname;

    private String parentFullname;

    private boolean b_parentFullname;

    private boolean subscribed;

    private boolean b_subscribed;

    private boolean hasSubfolders;

    private boolean b_hasSubfolders;

    private boolean hasSubscribedSubfolders;

    private boolean b_hasSubscribedSubfolders;

    private boolean exists;

    private boolean b_exists;

    private boolean holdsMessages;

    private boolean b_holdsMessages;

    private boolean holdsFolders;

    private boolean b_holdsFolders;

    private int messageCount;

    private boolean b_messageCount;

    private int newMessageCount;

    private boolean b_newMessageCount;

    private int unreadMessageCount;

    private boolean b_unreadMessageCount;

    private int deletedMessageCount;

    private boolean b_deletedMessageCount;

    private char separator;

    private boolean b_separator;

    private MailPermission ownPermission;

    private boolean b_ownPermission;

    private boolean supportsUserFlags;

    private boolean b_supportsUserFlags;

    private boolean rootFolder;

    private boolean b_rootFolder;

    private boolean defaultFolder;

    private boolean b_defaultFolder;

    private List<MailPermission> permissions;

    private boolean b_permissions;

    /**
     * Virtual name of mailbox's root folder
     * 
     * @value "E-Mail"
     */
    public static final String DEFAULT_FOLDER_NAME = "E-Mail";

    /**
     * Virtual fullname of mailbox's root folder
     * 
     * @value "default"
     */
    public static final String DEFAULT_FOLDER_ID = "default";

    /**
     * Initializes a new {@link MailFolder}
     */
    public MailFolder() {
        super();
    }

    /**
     * Gets the fullname
     * 
     * @return The fullname ({@link #DEFAULT_FOLDER_ID} if this mail folder denotes the root folder)
     */
    public String getFullname() {
        return fullname;
    }

    /**
     * @return <code>true</code> if fullname is set; otherwise <code>false</code>
     */
    public boolean containsFullname() {
        return b_fullname;
    }

    /**
     * Removes the fullname
     */
    public void removeFullname() {
        fullname = null;
        b_fullname = false;
    }

    /**
     * Sets this mail folder's fullname.
     * <p>
     * If this mail folder denotes the root folder, {@link #DEFAULT_FOLDER_ID} is supposed to be set as fullname.
     * 
     * @param fullname the fullname to set
     */
    public void setFullname(final String fullname) {
        this.fullname = fullname;
        b_fullname = true;
    }

    /**
     * @return the hasSubfolders
     */
    public boolean hasSubfolders() {
        return hasSubfolders;
    }

    /**
     * @return <code>true</code> if hasSubfolders is set; otherwise <code>false</code>
     */
    public boolean containsSubfolders() {
        return b_hasSubfolders;
    }

    /**
     * Removes hasSubfolders
     */
    public void removeSubfolders() {
        hasSubfolders = false;
        b_hasSubfolders = false;
    }

    /**
     * @param hasSubfolders the hasSubfolders to set
     */
    public void setSubfolders(final boolean hasSubfolders) {
        this.hasSubfolders = hasSubfolders;
        b_hasSubfolders = true;
    }

    /**
     * @return the hasSubscribedSubfolders
     */
    public boolean hasSubscribedSubfolders() {
        return hasSubscribedSubfolders;
    }

    /**
     * @return <code>true</code> if hasSubscribedSubfolders is set; otherwise <code>false</code>
     */
    public boolean containsSubscribedSubfolders() {
        return b_hasSubscribedSubfolders;
    }

    /**
     * Removes hasSubscribedSubfolders
     */
    public void removeSubscribedSubfolders() {
        hasSubscribedSubfolders = false;
        b_hasSubscribedSubfolders = false;
    }

    /**
     * @param hasSubscribedSubfolders the hasSubscribedSubfolders to set
     */
    public void setSubscribedSubfolders(final boolean hasSubscribedSubfolders) {
        this.hasSubscribedSubfolders = hasSubscribedSubfolders;
        b_hasSubscribedSubfolders = true;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return <code>true</code> if name is set; otherwise <code>false</code>
     */
    public boolean containsName() {
        return b_name;
    }

    /**
     * Removes the name
     */
    public void removeName() {
        name = null;
        b_name = false;
    }

    /**
     * Sets this mail folder's name.
     * <p>
     * If this mail folder denotes the root folder, {@link #DEFAULT_FOLDER_NAME} is supposed to be set as name.
     * 
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
        b_name = true;
    }

    /**
     * Returns whether the denoted mail folder is subscribed or not
     * <p>
     * If mailing system does not support subscription, <code>true</code> is supposed to be returned.
     * 
     * @return Whether the denoted mail folder is subscribed or not
     */
    public boolean isSubscribed() {
        return subscribed;
    }

    /**
     * @return <code>true</code> if subscribed is set; otherwise <code>false</code>
     */
    public boolean containsSubscribed() {
        return b_subscribed;
    }

    /**
     * Removes the subscription status
     */
    public void removeSubscribed() {
        subscribed = false;
        b_subscribed = false;
    }

    /**
     * Sets the subscription status for this mail folder.
     * <p>
     * If mailing system does not support subscription, <code>true</code> is supposed to be set as subscription status.
     * 
     * @param subscribed the subscription status to set
     */
    public void setSubscribed(final boolean subscribed) {
        this.subscribed = subscribed;
        b_subscribed = true;
    }

    /**
     * Gets the number of messages marked for deletion in this folder
     * 
     * @return The number of messages marked for deletion in this folder or <code>-1</code> if this mail folder does not hold messages
     * @see #isHoldsMessages()
     */
    public int getDeletedMessageCount() {
        return deletedMessageCount;
    }

    /**
     * @return <code>true</code> if deletedMessageCount is set; otherwise <code>false</code>
     */
    public boolean containsDeletedMessageCount() {
        return b_deletedMessageCount;
    }

    /**
     * Removes the number of messages marked for deletion in this folder
     */
    public void removeDeletedMessageCount() {
        deletedMessageCount = 0;
        b_deletedMessageCount = false;
    }

    /**
     * Sets the number of messages marked for deletion in this folder
     * 
     * @param deletedMessageCount The number of messages marked for deletion or <code>-1</code> if this mail folder does not hold messages
     */
    public void setDeletedMessageCount(final int deletedMessageCount) {
        this.deletedMessageCount = deletedMessageCount;
        b_deletedMessageCount = true;
    }

    /**
     * Checks if this folder exists
     * 
     * @return <code>true</code> if folder exists in mailbox; otherwise <code>false</code>
     */
    public boolean exists() {
        return exists;
    }

    /**
     * @return <code>true</code> if exists status is set; otherwise <code>false</code>
     */
    public boolean containsExists() {
        return b_exists;
    }

    /**
     * Removes exists status
     */
    public void removeExists() {
        exists = false;
        b_exists = false;
    }

    /**
     * Sets the exists status
     * 
     * @param exists <code>true</code> if folder exists in mailbox; otherwise <code>false</code>
     */
    public void setExists(final boolean exists) {
        this.exists = exists;
        b_exists = true;
    }

    /**
     * Gets the number of messages
     * 
     * @return The number of messages or <code>-1</code> if this mail folder does not hold messages
     * @see #isHoldsMessages()
     */
    public int getMessageCount() {
        return messageCount;
    }

    /**
     * @return <code>true</code> if messageCount is set; otherwise <code>false</code>
     */
    public boolean containsMessageCount() {
        return b_messageCount;
    }

    /**
     * Removes the message-count
     */
    public void removeMessageCount() {
        messageCount = 0;
        b_messageCount = false;
    }

    /**
     * Sets the number of messages
     * 
     * @param messageCount The number of messages or <code>-1</code> if this mail folder does not hold messages
     */
    public void setMessageCount(final int messageCount) {
        this.messageCount = messageCount;
        b_messageCount = true;
    }

    /**
     * Gets the number of new messages (since last time this folder was accessed)
     * 
     * @return The number of new messages or <code>-1</code> if this mail folder does not hold messages
     * @see #isHoldsMessages()
     */
    public int getNewMessageCount() {
        return newMessageCount;
    }

    /**
     * @return <code>true</code> if newMessageCount is set; otherwise <code>false</code>
     */
    public boolean containsNewMessageCount() {
        return b_newMessageCount;
    }

    /**
     * Removes the new-message-count
     */
    public void removeNewMessageCount() {
        newMessageCount = 0;
        b_newMessageCount = false;
    }

    /**
     * Sets the number of new messages
     * 
     * @param newMessageCount The number of new messages or <code>-1</code> if this mail folder does not hold messages
     */
    public void setNewMessageCount(final int newMessageCount) {
        this.newMessageCount = newMessageCount;
        b_newMessageCount = true;
    }

    /**
     * Gets the number of unread messages
     * 
     * @return The number of unread messages or <code>-1</code> if this mail folder does not hold messages
     * @see #isHoldsMessages()
     */
    public int getUnreadMessageCount() {
        return unreadMessageCount;
    }

    /**
     * @return <code>true</code> if unreadMessageCount is set; otherwise <code>false</code>
     */
    public boolean containsUnreadMessageCount() {
        return b_unreadMessageCount;
    }

    /**
     * Removes the unread-message-count
     */
    public void removeUnreadMessageCount() {
        unreadMessageCount = 0;
        b_unreadMessageCount = false;
    }

    /**
     * Sets the number of unread messages
     * 
     * @param unreadMessageCount The number of unread messages or <code>-1</code> if this mail folder does not hold messages
     */
    public void setUnreadMessageCount(final int unreadMessageCount) {
        this.unreadMessageCount = unreadMessageCount;
        b_unreadMessageCount = true;
    }

    /**
     * Gets the separator
     * 
     * @see MailConfig#getDefaultSeparator()
     * @return the separator
     */
    public char getSeparator() {
        return separator;
    }

    /**
     * @return <code>true</code> if separator is set; otherwise <code>false</code>
     */
    public boolean containsSeparator() {
        return b_separator;
    }

    /**
     * Removes the separator
     */
    public void removeSeparator() {
        separator = '0';
        b_separator = false;
    }

    /**
     * Sets the separator.
     * <p>
     * If mailing system does not support a separator character, {@link MailConfig#getDefaultSeparator()} should to be used.
     * 
     * @param separator the separator to set
     */
    public void setSeparator(final char separator) {
        this.separator = separator;
        b_separator = true;
    }

    /**
     * Gets the parent fullname
     * 
     * @return The parent fullname or <code>null</code> if this mail folder denotes the root folder
     */
    public String getParentFullname() {
        return parentFullname;
    }

    /**
     * @return <code>true</code> if parentFullname is set; otherwise <code>false</code>
     */
    public boolean containsParentFullname() {
        return b_parentFullname;
    }

    /**
     * Removes the parentFullname
     */
    public void removeParentFullname() {
        parentFullname = null;
        b_parentFullname = false;
    }

    /**
     * Sets the parent fullname
     * <p>
     * If this mail folder denotes the root folder, <code>null</code> is supposed to be set.
     * 
     * @param parentFullname the parent fullname to set
     */
    public void setParentFullname(final String parentFullname) {
        this.parentFullname = parentFullname;
        b_parentFullname = true;
    }

    /**
     * Checks if this folder holds messages
     * 
     * @return <code>true</code> if folder holds messages; otherwise <code>false</code>
     */
    public boolean isHoldsMessages() {
        return holdsMessages;
    }

    /**
     * @return <code>true</code> if this folder holds messages; otherwise <code>false</code>
     */
    public boolean containsHoldsMessages() {
        return b_holdsMessages;
    }

    /**
     * Removes the holds messages flag
     */
    public void removeHoldsMessages() {
        holdsMessages = false;
        b_holdsMessages = false;
    }

    /**
     * Sets if this folder holds messages
     * 
     * @param holdsMessages <code>true</code> if folder holds messages; otherwise <code>false</code>
     */
    public void setHoldsMessages(final boolean holdsMessages) {
        this.holdsMessages = holdsMessages;
        b_holdsMessages = true;
    }

    /**
     * Checks if this folder holds folders
     * 
     * @return <code>true</code> if folder holds folders; otherwise <code>false</code>
     */
    public boolean isHoldsFolders() {
        return holdsFolders;
    }

    /**
     * @return <code>true</code> if this folder holds folders; otherwise <code>false</code>
     */
    public boolean containsHoldsFolders() {
        return b_holdsFolders;
    }

    /**
     * Removes the holds folders flag
     */
    public void removeHoldsFolders() {
        holdsFolders = false;
        b_holdsFolders = false;
    }

    /**
     * Sets if this folder holds folders
     * 
     * @param holdsFolders <code>true</code> if folder holds folders; otherwise <code>false</code>
     */
    public void setHoldsFolders(final boolean holdsFolders) {
        this.holdsFolders = holdsFolders;
        b_holdsFolders = true;
    }

    /**
     * Gets the permission for currently logged-in user accessing this folder
     * <p>
     * The returned permission should reflect user's permission regardless if mailing system supports permissions or not. An instance of
     * {@link DefaultMailPermission} is supposed to be returned on missing permissions support except for the root folder. The root folder
     * should indicate no object permissions in any case, but the folder permission varies if mailing system allows subfolder creation below
     * root folder or not. The returned permission must reflect the allowed behavior.
     * 
     * @return The own permission
     */
    public MailPermission getOwnPermission() {
        return ownPermission;
    }

    /**
     * @return <code>true</code> if own permission is set; otherwise <code>false</code>
     */
    public boolean containsOwnPermission() {
        return b_ownPermission;
    }

    /**
     * Removes the own permission
     */
    public void removeOwnPermission() {
        ownPermission = null;
        b_ownPermission = false;
    }

    /**
     * Sets own permission.
     * <p>
     * Apply an instance of {@link DefaultMailPermission} if mailing system does not support permissions, except if this mail folder denotes
     * the root folder, then apply altered instance of {@link DefaultMailPermission} with no object permissions but properly reflects folder
     * permission as described in {@link #getOwnPermission()}.
     * 
     * @param ownPermission the own permission to set
     */
    public void setOwnPermission(final MailPermission ownPermission) {
        this.ownPermission = ownPermission;
        b_ownPermission = true;
    }

    /**
     * Checks if this folder denotes the root folder
     * 
     * @return <code>true</code> if this folder denotes the root folder; otherwise <code>false</code>
     */
    public boolean isRootFolder() {
        return rootFolder;
    }

    /**
     * @return <code>true</code> if rootFolder is set; otherwise <code>false</code>
     */
    public boolean containsRootFolder() {
        return b_rootFolder;
    }

    /**
     * Removes the root folder flag
     */
    public void removeRootFolder() {
        rootFolder = false;
        b_rootFolder = false;
    }

    /**
     * Sets the root folder flag
     * 
     * @param rootFolder the root folder flag to set
     */
    public void setRootFolder(final boolean rootFolder) {
        this.rootFolder = rootFolder;
        b_rootFolder = true;
    }

    /**
     * Checks if this folder denotes a default folder (Drafts, Sent, Trash, etc.)
     * 
     * @return <code>true</code> if this folder denotes a default folder; otherwise <code>false</code>
     */
    public boolean isDefaultFolder() {
        return defaultFolder;
    }

    /**
     * @return <code>true</code> if default folder is set; otherwise <code>false</code>
     */
    public boolean containsDefaultFolder() {
        return b_defaultFolder;
    }

    /**
     * Removes the default folder flag
     */
    public void removeDefaultFolder() {
        defaultFolder = false;
        b_defaultFolder = false;
    }

    /**
     * Sets the default folder flag
     * 
     * @param defaultFolder the default folder flag to set
     */
    public void setDefaultFolder(final boolean defaultFolder) {
        this.defaultFolder = defaultFolder;
        b_defaultFolder = true;
    }

    /**
     * Adds a permission
     * 
     * @param permission The permission to add
     */
    public void addPermission(final MailPermission permission) {
        if (null == permission) {
            return;
        } else if (null == permissions) {
            permissions = new ArrayList<MailPermission>();
            b_permissions = true;
        }
        permissions.add(permission);
    }

    /**
     * Adds an array of permissions
     * 
     * @param permissions The array of permissions to add
     */
    public void addPermissions(final MailPermission[] permissions) {
        if ((null == permissions) || (permissions.length == 0)) {
            return;
        } else if (null == this.permissions) {
            this.permissions = new ArrayList<MailPermission>(permissions.length);
            b_permissions = true;
        }
        this.permissions.addAll(Arrays.asList(permissions));
    }

    /**
     * Adds a collection of permissions
     * 
     * @param permissions The collection of permissions to add
     */
    public void addPermissions(final Collection<? extends MailPermission> permissions) {
        if ((null == permissions) || (permissions.isEmpty())) {
            return;
        } else if (null == this.permissions) {
            this.permissions = new ArrayList<MailPermission>(permissions.size());
            b_permissions = true;
        }
        this.permissions.addAll(permissions);
    }

    /**
     * @return <code>true</code> if permissions are set; otherwise <code>false</code>
     */
    public boolean containsPermissions() {
        return b_permissions;
    }

    /**
     * Removes the permissions
     */
    public void removePermissions() {
        permissions = null;
        b_permissions = false;
    }

    private static final MailPermission[] EMPTY_PERMS = new MailPermission[0];

    /**
     * @return the permissions as array of {@link MailPermission}
     */
    public MailPermission[] getPermissions() {
        if (null == permissions) {
            return EMPTY_PERMS;
        }
        return permissions.toArray(new MailPermission[permissions.size()]);
    }

    @Override
    public String toString() {
        return containsFullname() ? getFullname() : "[no fullname]";
    }

    /**
     * Checks if this folder supports user flags
     * 
     * @return <code>true</code> if this folder supports user flags; otherwise <code>false</code>
     */
    public boolean isSupportsUserFlags() {
        return supportsUserFlags;
    }

    /**
     * @return <code>true</code> if supportsUserFlags is set; otherwise <code>false</code>
     */
    public boolean containsSupportsUserFlags() {
        return b_supportsUserFlags;
    }

    /**
     * Removes the supports-user-flags flag
     */
    public void removeSupportsUserFlags() {
        b_supportsUserFlags = false;
        b_supportsUserFlags = false;
    }

    /**
     * Sets the supports-user-flags flag
     * 
     * @param supportsUserFlags the supports-user-flags flag to set
     */
    public void setSupportsUserFlags(final boolean supportsUserFlags) {
        this.supportsUserFlags = supportsUserFlags;
        b_supportsUserFlags = true;
    }

}
