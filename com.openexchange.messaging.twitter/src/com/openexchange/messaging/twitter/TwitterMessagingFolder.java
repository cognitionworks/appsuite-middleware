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

package com.openexchange.messaging.twitter;

import java.util.Collections;
import java.util.List;
import com.openexchange.messaging.DefaultMessagingPermission;
import com.openexchange.messaging.MessagingFolder;
import com.openexchange.messaging.MessagingPermission;
import com.openexchange.messaging.MessagingPermissions;

/**
 * {@link TwitterMessagingFolder}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class TwitterMessagingFolder implements MessagingFolder {

    private static final TwitterMessagingFolder INSTANCE = new TwitterMessagingFolder();

    /**
     * Gets the instance.
     * 
     * @return The instance
     */
    public static TwitterMessagingFolder getInstance() {
        return INSTANCE;
    }

    private final MessagingPermission ownPermission;

    /**
     * Initializes a new {@link TwitterMessagingFolder}.
     */
    private TwitterMessagingFolder() {
        super();
        final MessagingPermission mp = DefaultMessagingPermission.newInstance();
        mp.setAllPermissions(
            MessagingPermission.READ_FOLDER,
            MessagingPermission.READ_ALL_OBJECTS,
            MessagingPermission.NO_PERMISSIONS,
            MessagingPermission.DELETE_OWN_OBJECTS);
        mp.setAdmin(false);
        ownPermission = MessagingPermissions.unmodifiablePermission(mp);
    }

    public List<String> getCapabilities() {
        return Collections.emptyList();
    }

    public String getId() {
        return MessagingFolder.ROOT_FULLNAME;
    }

    public String getName() {
        return MessagingFolder.ROOT_FULLNAME;
    }

    public MessagingPermission getOwnPermission() {
        return ownPermission;
    }

    public String getParentId() {
        return null;
    }

    public List<MessagingPermission> getPermissions() {
        return Collections.emptyList();
    }

    public boolean hasSubfolders() {
        return false;
    }

    public boolean hasSubscribedSubfolders() {
        return false;
    }

    public boolean isSubscribed() {
        return true;
    }

    public int getDeletedMessageCount() {
        return 0;
    }

    public int getMessageCount() {
        return TwitterConstants.TIMELINE_LENGTH;
    }

    public int getNewMessageCount() {
        return 0;
    }

    public int getUnreadMessageCount() {
        return 0;
    }

    public boolean isDefaultFolder() {
        return false;
    }

    public boolean isHoldsFolders() {
        return false;
    }

    public boolean isHoldsMessages() {
        return true;
    }

    public boolean isRootFolder() {
        return true;
    }

    public boolean containsDefaultFolderType() {
        return true;
    }

    public DefaultFolderType getDefaultFolderType() {
        return DefaultFolderType.NONE;
    }

    public char getSeparator() {
        return '\0';
    }

}
