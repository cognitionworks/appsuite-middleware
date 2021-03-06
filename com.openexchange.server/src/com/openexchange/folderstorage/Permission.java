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
 *    trademarks of the OX Software GmbH group of companies.
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

package com.openexchange.folderstorage;

import java.io.Serializable;
import com.openexchange.groupware.EntityInfo;

/**
 * {@link Permission} - A folder permission.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface Permission extends Cloneable, Serializable {

    /**
     * The constant for no permission at all.
     */
    public static final int NO_PERMISSIONS = 0;

    /**
     * The constant for maximum permission.
     */
    public static final int MAX_PERMISSION = 128;

    /**
     * The permission constant granting folder visibility.
     */
    public static final int READ_FOLDER = 2;

    /**
     * The permission constant granting folder visibility and allowing to create objects in folder.
     */
    public static final int CREATE_OBJECTS_IN_FOLDER = 4;

    /**
     * The permission constant granting folder visibility, allowing to create objects in folder, and allowing to create subfolders below
     * folder.
     */
    public static final int CREATE_SUB_FOLDERS = 8;

    /**
     * The permission constant granting visibility for own objects.
     */
    public static final int READ_OWN_OBJECTS = 2;

    /**
     * The permission constant granting visibility for all objects.
     */
    public static final int READ_ALL_OBJECTS = 4;

    /**
     * The permission constant allowing to edit own objects.
     */
    public static final int WRITE_OWN_OBJECTS = 2;

    /**
     * The permission constant allowing to edit all objects.
     */
    public static final int WRITE_ALL_OBJECTS = 4;

    /**
     * The permission constant allowing to remove own objects.
     */
    public static final int DELETE_OWN_OBJECTS = 2;

    /**
     * The permission constant allowing to remove all objects.
     */
    public static final int DELETE_ALL_OBJECTS = 4;

    /*-
     * #################### METHODS ####################
     */

    /**
     * Creates and returns a copy of this object.
     *
     * @return A clone of this instance.
     */
    public Object clone();

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    @Override
    public boolean equals(Object obj);

    /**
     * Gets this folder permission's system bit mask.
     *
     * @return This folder permission's system bit mask
     */
    public int getSystem();

    /**
     * Sets this folder permission's system bit mask.
     *
     * @param system This folder permission's system bit mask
     */
    public void setSystem(int system);

    /**
     * Gets this folder permission's type.
     *
     * @return This folder permission's type.
     */
    public FolderPermissionType getType();

    /**
     * Sets this folder permission's type.
     *
     * @param type This folder permission's type.
     */
    public void setType(FolderPermissionType type);

    /**
     * If this permission is handed down from a parent folder this method retrieves the id of the sharing folder.
     *
     * @return The id of the sharing folder.
     */
    public String getPermissionLegator();

    /**
     * Sets the id of the folder who has handed down this permission
     *
     * @param legator The id of the sharing folder
     */
    public void setPermissionLegator(String legator);

    /**
     * Checks if this folder permission's entity is a group.
     *
     * @return <code>true</code> if this folder permission's entity is a group; otherwise <code>false</code>
     */
    public boolean isGroup();

    /**
     * Sets if this folder permission's entity is a group.
     *
     * @param group <code>true</code> if this folder permission's entity is a group; otherwise <code>false</code>
     */
    public void setGroup(boolean group);

    /**
     * Checks if this folder is visible.
     *
     * @return <code>true</code> if this folder is visible (either admin or appropriate folder permission); otherwise <code>false</code>
     */
    public boolean isVisible();

    /**
     * Gets the qualified identifier of the entity associated with this permission.
     * 
     * @return The identifier
     */
    String getIdentifier();

    /**
     * Sets the qualified identifier of the entity associated with this permission.
     * 
     * @param identifier The identifier to set
     */
    void setIdentifier(String identifier);

    /**
     * Gets this folder permission's entity identifier.
     *
     * @return This folder permission's entity identifier
     */
    public int getEntity();

    /**
     * Sets this folder permission's entity identifier.
     *
     * @param entity The entity identifier
     */
    public void setEntity(int entity);

    /**
     * Gets additional information about the entity associated with this permission.
     * 
     * @return The entity info, or <code>null</code> if not available
     */
    EntityInfo getEntityInfo();

    /**
     * Sets additional information about the entity associated with this permission.
     * 
     * @param entityInfo The entity info to set
     */
    void setEntityInfo(EntityInfo entityInfo);

    /**
     * Checks if this folder permission denotes its entity as a folder administrator.
     *
     * @return <code>true</code> if this folder permission's entity is a folder administrator; otherwise <code>false</code>
     */
    public boolean isAdmin();

    /**
     * Sets if this folder permission denotes its entity as a folder administrator.
     *
     * @param admin <code>true</code> if this folder permission's entity is a folder administrator; otherwise <code>false</code>
     */
    public void setAdmin(boolean admin);

    /**
     * Gets the folder permission.
     * <p>
     * Returned value is one of:
     * <ul>
     * <li>{@link #NO_PERMISSIONS}</li>
     * <li>{@link #READ_FOLDER}</li>
     * <li>{@link #CREATE_OBJECTS_IN_FOLDER}</li>
     * <li>{@link #CREATE_SUB_FOLDERS}</li>
     * <li>{@link #MAX_PERMISSION}</li>
     * </ul>
     *
     * @return The folder permission
     */
    public int getFolderPermission();

    /**
     * Sets the folder permission.
     * <p>
     * Passed value is one of:
     * <ul>
     * <li>{@link #NO_PERMISSIONS}</li>
     * <li>{@link #READ_FOLDER}</li>
     * <li>{@link #CREATE_OBJECTS_IN_FOLDER}</li>
     * <li>{@link #CREATE_SUB_FOLDERS}</li>
     * <li>{@link #MAX_PERMISSION}</li>
     * </ul>
     *
     * @param permission The folder permission
     */
    public void setFolderPermission(int permission);

    /**
     * Gets the read permission.
     * <p>
     * Returned value is one of:
     * <ul>
     * <li>{@link #NO_PERMISSIONS}</li>
     * <li>{@link #READ_OWN_OBJECTS}</li>
     * <li>{@link #READ_ALL_OBJECTS}</li>
     * <li>{@link #MAX_PERMISSION}</li>
     * </ul>
     *
     * @return The read permission
     */
    public int getReadPermission();

    /**
     * Sets the read permission.
     * <p>
     * Passed value is one of:
     * <ul>
     * <li>{@link #NO_PERMISSIONS}</li>
     * <li>{@link #READ_OWN_OBJECTS}</li>
     * <li>{@link #READ_ALL_OBJECTS}</li>
     * <li>{@link #MAX_PERMISSION}</li>
     * </ul>
     *
     * @param permission The read permission
     */
    public void setReadPermission(int permission);

    /**
     * Gets the write permission.
     * <p>
     * Returned value is one of:
     * <ul>
     * <li>{@link #NO_PERMISSIONS}</li>
     * <li>{@link #WRITE_OWN_OBJECTS}</li>
     * <li>{@link #WRITE_ALL_OBJECTS}</li>
     * <li>{@link #MAX_PERMISSION}</li>
     * </ul>
     *
     * @return The write permission
     */
    public int getWritePermission();

    /**
     * Sets the write permission.
     * <p>
     * Passed value is one of:
     * <ul>
     * <li>{@link #NO_PERMISSIONS}</li>
     * <li>{@link #WRITE_OWN_OBJECTS}</li>
     * <li>{@link #WRITE_ALL_OBJECTS}</li>
     * <li>{@link #MAX_PERMISSION}</li>
     * </ul>
     *
     * @param permission The write permission
     */
    public void setWritePermission(int permission);

    /**
     * Gets the delete permission.
     * <p>
     * Returned value is one of:
     * <ul>
     * <li>{@link #NO_PERMISSIONS}</li>
     * <li>{@link #DELETE_OWN_OBJECTS}</li>
     * <li>{@link #DELETE_ALL_OBJECTS}</li>
     * <li>{@link #MAX_PERMISSION}</li>
     * </ul>
     *
     * @return The delete permission
     */
    public int getDeletePermission();

    /**
     * Sets the delete permission.
     * <p>
     * Passed value is one of:
     * <ul>
     * <li>{@link #NO_PERMISSIONS}</li>
     * <li>{@link #DELETE_OWN_OBJECTS}</li>
     * <li>{@link #DELETE_ALL_OBJECTS}</li>
     * <li>{@link #MAX_PERMISSION}</li>
     * </ul>
     *
     * @param permission The delete permission
     */
    public void setDeletePermission(int permission);

    /**
     * Convenience method to set all permissions at once.
     *
     * @param folderPermission The folder permission
     * @param readPermission The read permission
     * @param writePermission The write permission
     * @param deletePermission The delete permission
     * @see #setFolderPermission(int)
     * @see #setReadPermission(int)
     * @see #setWritePermission(int)
     * @see #setDeletePermission(int)
     */
    public void setAllPermissions(int folderPermission, int readPermission, int writePermission, int deletePermission);

    /**
     * Convenience method which passes {@link #MAX_PERMISSION} to all permissions and sets folder administrator flag to <code>true</code>.
     */
    public void setMaxPermissions();

    /**
     * Convenience method which passes {@link #NO_PERMISSIONS} to all permissions and sets folder administrator flag to <code>false</code>.
     */
    public void setNoPermissions();
}
