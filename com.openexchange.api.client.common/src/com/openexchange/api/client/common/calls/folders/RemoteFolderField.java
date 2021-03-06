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

package com.openexchange.api.client.common.calls.folders;

/**
 * {@link RemoteFolderField}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.5
 */
public enum RemoteFolderField {

    // See com.openexchange.groupware.container.DataObject
    /**
     * The ID of the folder
     */
    ID(1, "id"),

    /**
     * The ID of the creator
     */
    CREATED_BY(2, "created_by"),

    /**
     * User ID of the user who last modified this object.
     */
    MODIFIED_BY(3, "modified_by"),

    /**
     * The date of creation
     */
    CREATION_DATE(4, "creation_date"),

    /**
     * The date of modification
     */
    LAST_MODIFIED(5, "last_modified"),

    // See com.openexchange.groupware.container.FolderChildObject
    /**
     * The object ID of the parent folder
     */
    FODLER_ID(20, "folder_id"),

    // See com.openexchange.groupware.container.FolderObject
    /**
     * The folder's title
     */
    TITLE(300, "title"),

    /**
     * The name of the module which implements this folder
     */
    MODULE(301, "module"),

    /**
     * True, if this folder has subfolders
     */
    SUBFOLDERS(304, "subfolders"),

    /**
     * Permissions which apply to the current user
     */
    OWN_RIGHTS(305, "own_rights"),

    /**
     * The folder's permission
     */
    PERMISSIONS(306, "permissions"),

    /**
     * Folder's subscription
     */
    SUBSCRIBED(314, "subscribed"),

    /**
     * Subscribed subfolders
     */
    SUBSCR_SUBFLDS(315, "subscr_subflds"),

    /**
     * Extended folder permissions
     */
    EXTENDED_PERMISSIONS(3060, "com.openexchange.share.extendedPermissions"),

    /**
     * Information about the entity that created the folder
     */
    CREATED_FROM(51, "created_from"),

    /**
     * Information about the entity that modified the folder
     */
    MODIFIED_FROM(52, "modified_from"),

    ;

    private final int column;

    private final String name;

    /**
     * Initializes a new {@link RemoteFolderField}.
     *
     * @param column The column ID
     * @param name The name of the field
     */
    private RemoteFolderField(int column, String name) {
        this.column = column;
        this.name = name;
    }

    /**
     *
     * Gets the column ID
     *
     * @return The column ID
     */
    public int getColumn() {
        return column;
    }

    /**
     * Gets the name
     *
     * @return The name
     */
    public String getName() {
        return name;
    }
}
