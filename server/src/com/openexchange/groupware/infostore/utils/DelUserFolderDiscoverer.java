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

package com.openexchange.groupware.infostore.utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import com.openexchange.api2.OXException;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.infostore.InfostoreException;
import com.openexchange.groupware.infostore.InfostoreExceptionCodes;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.tx.DBProvider;
import com.openexchange.groupware.tx.DBService;
import com.openexchange.groupware.userconfiguration.RdbUserConfigurationStorage;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.tools.iterator.FolderObjectIterator;
import com.openexchange.tools.oxfolder.OXFolderIteratorSQL;

public class DelUserFolderDiscoverer extends DBService {

    public DelUserFolderDiscoverer() {
        super();
    }

    public DelUserFolderDiscoverer(final DBProvider provider) {
        super(provider);
    }

    public List<FolderObject> discoverFolders(final int userId, final Context ctx) throws OXException {
        final List<FolderObject> discovered = new ArrayList<FolderObject>();
        try {
            final User user = UserStorage.getInstance().getUser(userId, ctx);
            final UserConfiguration userConfig = RdbUserConfigurationStorage.loadUserConfiguration(userId, ctx);

            final Queue<FolderObject> queue = ((FolderObjectIterator) OXFolderIteratorSQL.getAllVisibleFoldersIteratorOfModule(
                userId,
                user.getGroups(),
                userConfig.getAccessibleModules(),
                FolderObject.INFOSTORE,
                ctx)).asQueue();
            folder: for (final FolderObject fo : queue) {
                if (isVirtual(fo)) {
                    continue folder;
                }
                for (final OCLPermission perm : fo.getPermissionsAsArray()) {
                    if (someoneElseMayReadInfoitems(perm, userId)) {
                        continue folder;
                    }
                }
                discovered.add(fo);
            }

        } catch (final AbstractOXException x) {
            throw new InfostoreException(x);
        } catch (final SQLException e) {
            throw InfostoreExceptionCodes.SQL_PROBLEM.create(e, "");
        }

        return discovered;
    }

    private boolean isVirtual(final FolderObject fo) {
        final int id = fo.getObjectID();
        return id == FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID || id == FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID;
    }

    private boolean someoneElseMayReadInfoitems(final OCLPermission perm, final int userId) {
       return (perm.isGroupPermission() || perm.getEntity() != userId) && (perm.canReadAllObjects() || perm.canReadOwnObjects());
    }
}
