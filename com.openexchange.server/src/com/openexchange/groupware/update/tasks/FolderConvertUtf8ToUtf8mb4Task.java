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
 *    trademarks of the OX Software GmbH. group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.groupware.update.tasks;

import java.sql.Connection;
import java.sql.SQLException;
import com.google.common.collect.ImmutableList;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.SimpleConvertUtf8ToUtf8mb4UpdateTask;

/**
 * {@link FolderConvertUtf8ToUtf8mb4Task} - Converts folder tables to utf8mb4.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.0
 */
public class FolderConvertUtf8ToUtf8mb4Task extends SimpleConvertUtf8ToUtf8mb4UpdateTask {

    /**
     * Initializes a new {@link FolderConvertUtf8ToUtf8mb4Task}.
     */
    public FolderConvertUtf8ToUtf8mb4Task() {
        //@formatter:off
        super(ImmutableList.of("oxfolder_tree", "oxfolder_permissions", "oxfolder_specialfolders", "oxfolder_userfolders",
            "oxfolder_userfolders_standardfolders", "del_oxfolder_tree", "del_oxfolder_permissions", "oxfolder_lock",
            "oxfolder_property"),
            AddTypeToFolderPermissionTableUpdateTask.class.getName());
        //@formatter:on
    }

    @Override
    protected void before(PerformParameters params, Connection connection) throws SQLException {
        recreateKey(connection, "virtualTree", new String[] { "cid", "tree", "user", "parentId" }, new int[] { -1, -1, -1, 191 });
        recreateKey(connection, "virtualTree", new String[] { "cid", "tree", "user", "shadow" }, new int[] { -1, -1, -1, 191 });

        recreateKey(connection, "virtualBackupTree", new String[] { "cid", "tree", "user", "parentId" }, new int[] { -1, -1, -1, 191 });
        recreateKey(connection, "virtualBackupTree", new String[] { "cid", "tree", "user", "shadow" }, new int[] { -1, -1, -1, 191 });
    }

    @Override
    protected void after(PerformParameters params, Connection connection) throws SQLException {
        String schema = params.getSchema().getSchema();
        changeTable(connection, schema, "virtualTree", ImmutableList.of("folderId", "parentId"));
        changeTable(connection, schema, "virtualPermission", ImmutableList.of("folderId"));
        changeTable(connection, schema, "virtualSubscription", ImmutableList.of("folderId"));

        changeTable(connection, schema, "virtualBackupTree", ImmutableList.of("folderId", "parentId"));
        changeTable(connection, schema, "virtualBackupPermission", ImmutableList.of("folderId"));
        changeTable(connection, schema, "virtualBackupSubscription", ImmutableList.of("folderId"));
    }
}
