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

package com.openexchange.folderstorage.internal.performers;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.Folder;
import com.openexchange.folderstorage.GuestPermission;
import com.openexchange.folderstorage.Permission;
import com.openexchange.groupware.ComparedPermissions;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.user.UserService;

/**
 * Helper class to calculate a diff of the folder permissions on an update request.
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public class ComparedFolderPermissions extends ComparedPermissions<Permission, GuestPermission> {

    private final Context context;
    private final UserService userService;
    private final Connection connection;
    private final Map<Integer, User> users = new HashMap<>();;

    /**
     * Initializes a new {@link ComparedFolderPermissions}.
     *
     * @param context The context
     * @param newPermissions The new permissions
     * @param originalPermissions The original permissions
     * @param userService The user service
     * @param connection The database connection used to load users, or <code>null</code>
     * @throws OXException
     */
    public ComparedFolderPermissions(Context context, Permission[] newPermissions, Permission[] originalPermissions, UserService userService, Connection connection) throws OXException {
        super(newPermissions, originalPermissions);
        this.context = context;
        this.userService = userService;
        this.connection = connection;
        calc();
    }

    /**
     * Initializes a new {@link ComparedFolderPermissions}.
     *
     * @param context The context
     * @param newFolder The modified object sent by the client; not <code>null</code>
     * @param origFolder The original object loaded from the storage; not <code>null</code>
     * @param userService The user service; not <code>null</code>
     * @param connection The database connection used to load users, or <code>null</code>
     * @throws OXException If errors occur when loading additional data for the comparison
     */
    public ComparedFolderPermissions(Context context, Folder newFolder, Folder origFolder, UserService userService, Connection connection) throws OXException {
        this(context, newFolder.getPermissions(), origFolder.getPermissions(), userService, connection);
    }

    @Override
    protected boolean isSystemPermission(Permission p) {
        return p.getSystem() != 0;
    }

    @Override
    protected boolean isUnresolvedGuestPermission(Permission p) {
        return p instanceof GuestPermission;
    }

    @Override
    protected boolean isGroupPermission(Permission p) {
        return p.isGroup();
    }

    @Override
    protected int getEntityId(Permission p) {
        return p.getEntity();
    }

    @Override
    protected boolean areEqual(Permission p1, Permission p2) {
        if (p1 == null) {
            if (p2 == null) {
                return true;
            }

            return false;
        }

        if (p2 == null) {
            return false;
        }

        return p1.equals(p2);
    }

    @Override
    protected boolean isGuestUser(int userId) throws OXException {
        return getUser(userId).isGuest();
    }

    public User getUser(int userId) throws OXException {
        User user = users.get(userId);
        if (user != null) {
            return user;
        }

        if (connection == null) {
            user =  userService.getUser(userId, context.getContextId());
        } else {
            user = userService.getUser(connection, userId, context);
        }
        users.put(userId, user);
        return user;
    }

}
