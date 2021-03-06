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

package com.openexchange.chronos.provider;

import java.util.Objects;
import com.openexchange.groupware.EntityInfo;

/**
 * {@link DefaultFileStoragePermission}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class DefaultCalendarPermission implements CalendarPermission {

    /**
     * Prepares a default <i>read-only</i> permission for the supplied user identifier.
     *
     * @param userID The identifier of the user to prepare the permission for
     * @return The default <i>read-only</i> permission for the supplied user
     */
    public static CalendarPermission readOnlyPermissionsFor(int userID) {
        return new DefaultCalendarPermission(
            String.valueOf(userID),
            userID,
            null,
            CalendarPermission.READ_FOLDER,
            CalendarPermission.READ_ALL_OBJECTS,
            CalendarPermission.NO_PERMISSIONS,
            CalendarPermission.NO_PERMISSIONS,
            false,
            false,
            CalendarPermission.NO_PERMISSIONS)
        ;
    }
    
    /**
     * Prepares a default <i>admin</i> permission for the supplied user identifier.
     *
     * @param userID The identifier of the user to prepare the permission for
     * @return The default <i>admin</i> permission for the supplied user
     */
    public static CalendarPermission adminPermissionsFor(int userID) {
        return new DefaultCalendarPermission(
            String.valueOf(userID),
            userID,
            null,
            CalendarPermission.MAX_PERMISSION,
            CalendarPermission.READ_ALL_OBJECTS,
            CalendarPermission.NO_PERMISSIONS,
            CalendarPermission.NO_PERMISSIONS,
            true,
            false,
            CalendarPermission.NO_PERMISSIONS)
        ;
    }

    private int system;
    private int deletePermission;
    private int folderPermission;
    private int readPermission;
    private int writePermission;
    private boolean admin;
    private String identifier;
    private int entity;
    private EntityInfo entityInfo;
    private boolean group;

    /**
     * Initializes an {@link DefaultCalendarPermission}.
     */
    public DefaultCalendarPermission() {
        super();
    }

    public DefaultCalendarPermission(CalendarPermission permission) {
        this(permission.getIdentifier(), permission.getEntity(), permission.getEntityInfo(), permission.getFolderPermission(), permission.getReadPermission(), permission.getWritePermission(), permission.getDeletePermission(), permission.isAdmin(), permission.isGroup(), permission.getSystem());
    }

    public DefaultCalendarPermission(String identifier, int entiy, EntityInfo entityInfo, int folderPermission, int readPermission, int writePermission, int deletePermission, boolean admin, boolean group, int system) {
        super();
        this.identifier = identifier;
        this.entityInfo = entityInfo;
        this.system = system;
        this.deletePermission = deletePermission;
        this.folderPermission = folderPermission;
        this.readPermission = readPermission;
        this.writePermission = writePermission;
        this.admin = admin;
        this.entity = entiy;
        this.group = group;
    }

    @Override
    public int getDeletePermission() {
        return deletePermission;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public int getEntity() {
        return entity;
    }

    @Override
    public EntityInfo getEntityInfo() {
        return entityInfo;
    }

    @Override
    public int getFolderPermission() {
        return folderPermission;
    }

    @Override
    public int getReadPermission() {
        return readPermission;
    }

    @Override
    public int getSystem() {
        return system;
    }

    @Override
    public int getWritePermission() {
        return writePermission;
    }

    @Override
    public boolean isAdmin() {
        return admin;
    }

    @Override
    public boolean isGroup() {
        return group;
    }

    @Override
    public void setAdmin(final boolean admin) {
        this.admin = admin;
    }

    @Override
    public void setAllPermissions(final int folderPermission, final int readPermission, final int writePermission, final int deletePermission) {
        this.folderPermission = folderPermission;
        this.readPermission = readPermission;
        this.deletePermission = deletePermission;
        this.writePermission = writePermission;
    }

    @Override
    public void setDeletePermission(final int permission) {
        deletePermission = permission;
    }

    @Override
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public void setEntity(final int entity) {
        this.entity = entity;
    }

    @Override
    public void setEntityInfo(EntityInfo entityInfo) {
        this.entityInfo = entityInfo;
    }

    @Override
    public void setFolderPermission(final int permission) {
        folderPermission = permission;
    }

    @Override
    public void setGroup(final boolean group) {
        this.group = group;
    }

    @Override
    public void setMaxPermissions() {
        folderPermission = CalendarPermission.MAX_PERMISSION;
        readPermission = CalendarPermission.MAX_PERMISSION;
        deletePermission = CalendarPermission.MAX_PERMISSION;
        writePermission = CalendarPermission.MAX_PERMISSION;
        admin = true;
    }

    @Override
    public void setNoPermissions() {
        folderPermission = CalendarPermission.NO_PERMISSIONS;
        readPermission = CalendarPermission.NO_PERMISSIONS;
        deletePermission = CalendarPermission.NO_PERMISSIONS;
        writePermission = CalendarPermission.NO_PERMISSIONS;
        admin = false;
    }

    @Override
    public void setReadPermission(final int permission) {
        readPermission = permission;
    }

    @Override
    public void setSystem(final int system) {
        this.system = system;
    }

    @Override
    public void setWritePermission(final int permission) {
        writePermission = permission;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (admin ? 1231 : 1237);
        result = prime * result + deletePermission;
        result = prime * result + entity;
        result = prime * result + (null != identifier ? identifier.hashCode() : 0);
        result = prime * result + folderPermission;
        result = prime * result + (group ? 1231 : 1237);
        result = prime * result + readPermission;
        result = prime * result + system;
        result = prime * result + writePermission;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof CalendarPermission)) {
            return false;
        }
        final CalendarPermission other = (CalendarPermission) obj;
        if (admin != other.isAdmin()) {
            return false;
        }
        if (deletePermission != other.getDeletePermission()) {
            return false;
        }
        if (false == Objects.equals(identifier, other.getIdentifier())) {
            return false;
        }
        if (entity != other.getEntity()) {
            return false;
        }
        if (folderPermission != other.getFolderPermission()) {
            return false;
        }
        if (group != other.isGroup()) {
            return false;
        }
        if (readPermission != other.getReadPermission()) {
            return false;
        }
        if (system != other.getSystem()) {
            return false;
        }
        if (writePermission != other.getWritePermission()) {
            return false;
        }
        return true;
    }

}
