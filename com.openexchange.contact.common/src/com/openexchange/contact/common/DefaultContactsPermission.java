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

package com.openexchange.contact.common;

/**
 * {@link DefaultContactsPermission}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.5
 */
public class DefaultContactsPermission implements ContactsPermission {

    /**
     * Prepares a default <i>read-only</i> permission for the supplied user identifier.
     *
     * @param userID The identifier of the user to prepare the permission for
     * @return The default <i>read-only</i> permission for the supplied user
     */
    public static ContactsPermission readOnlyPermissionsFor(int userID) {
        //@formatter:off
        return new DefaultContactsPermission(
            userID,
            ContactsPermission.READ_FOLDER,
            ContactsPermission.READ_ALL_OBJECTS,
            ContactsPermission.NO_PERMISSIONS,
            ContactsPermission.NO_PERMISSIONS,
            false,
            false,
            ContactsPermission.NO_PERMISSIONS)
        ;
        //@formatter:on
    }

    /**
     * Prepares a default <i>admin</i> permission for the supplied user identifier.
     *
     * @param userID The identifier of the user to prepare the permission for
     * @return The default <i>admin</i> permission for the supplied user
     */
    public static ContactsPermission adminPermissionsFor(int userID) {
        //@formatter:off
        return new DefaultContactsPermission(
            userID,
            ContactsPermission.MAX_PERMISSION,
            ContactsPermission.READ_ALL_OBJECTS,
            ContactsPermission.NO_PERMISSIONS,
            ContactsPermission.NO_PERMISSIONS,
            true,
            false,
            ContactsPermission.NO_PERMISSIONS)
        ;
        //@formatter:on
    }

    private int system;
    private int deletePermission;
    private int folderPermission;
    private int readPermission;
    private int writePermission;
    private boolean admin;
    private int entity;
    private boolean group;

    /**
     * Initializes an {@link DefaultContactsPermission}.
     */
    public DefaultContactsPermission() {
        super();
    }

    public DefaultContactsPermission(int entiy, int folderPermission, int readPermission, int writePermission, int deletePermission, boolean admin, boolean group, int system) {
        super();
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
    public int getEntity() {
        return entity;
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
    public void setEntity(final int entity) {
        this.entity = entity;
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
        folderPermission = ContactsPermission.MAX_PERMISSION;
        readPermission = ContactsPermission.MAX_PERMISSION;
        deletePermission = ContactsPermission.MAX_PERMISSION;
        writePermission = ContactsPermission.MAX_PERMISSION;
        admin = true;
    }

    @Override
    public void setNoPermissions() {
        folderPermission = ContactsPermission.NO_PERMISSIONS;
        readPermission = ContactsPermission.NO_PERMISSIONS;
        deletePermission = ContactsPermission.NO_PERMISSIONS;
        writePermission = ContactsPermission.NO_PERMISSIONS;
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
        if (!(obj instanceof ContactsPermission)) {
            return false;
        }
        final ContactsPermission other = (ContactsPermission) obj;
        if (admin != other.isAdmin()) {
            return false;
        }
        if (deletePermission != other.getDeletePermission()) {
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
