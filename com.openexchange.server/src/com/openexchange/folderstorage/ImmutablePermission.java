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

import com.openexchange.groupware.EntityInfo;

/**
 * {@link ImmutablePermission} - An immutable permission.
 * <p>
 * Invocations of any setter method will throw an {@link UnsupportedOperationException}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.4
 */
public class ImmutablePermission extends BasicPermission {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 6640988722846121827L;

    /**
     * Creates a new builder.
     *
     * @return The new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder for an immutable permission */
    public static class Builder {

        private String identifier;
        private int entity = -1;
        private EntityInfo entityInfo;
        private boolean group;
        private int system;
        private FolderPermissionType type = FolderPermissionType.NORMAL;
        private String legator;
        private boolean admin;
        private int folderPermission;
        private int readPermission;
        private int writePermission;
        private int deletePermission;

        /**
         * Initializes a new {@link ImmutablePermission.Builder}.
         */
        Builder() {
            super();
        }

        /**
         * Resets this builder.
         *
         * @return This builder
         */
        public Builder reset() {
            entity = -1;
            group = false;
            system = 0;
            type = FolderPermissionType.NORMAL;
            legator = null;
            admin = true;
            folderPermission = 0;
            readPermission = 0;
            writePermission = 0;
            deletePermission = 0;
            return this;
        }

        /**
         * Sets the identifier
         *
         * @param entity The identifier to set
         * @return This builder
         */
        public Builder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * Sets the entity identifier; either user or group identifier
         *
         * @param entity The entity identifier to set
         * @return This builder
         */
        public Builder setEntity(int entity) {
            this.entity = entity;
            return this;
        }

        /**
         * Sets the entity info
         *
         * @param entityInfo The entity info to set
         * @return This builder
         */
        public Builder setEntity(EntityInfo entityInfo) {
            this.entityInfo = entityInfo;
            return this;
        }

        /**
         * Sets the group flag
         *
         * @param group The group flag to set
         * @return This builder
         */
        public Builder setGroup(boolean group) {
            this.group = group;
            return this;
        }

        /**
         * Sets the system flag
         *
         * @param system The system flag to set
         * @return This builder
         */
        public Builder setSystem(int system) {
            this.system = system;
            return this;
        }

        /**
         * Sets the type.
         *
         * @param type The permission type
         * @return This builder
         */
        public Builder setFolderPermissionType(FolderPermissionType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the legator.
         *
         * @param legator The legator
         * @return This builder
         */
        public Builder setFolderPermissionType(String legator) {
            this.legator = legator;
            return this;
        }


        /**
         * Sets the admin flag
         *
         * @param admin The admin flag to set
         * @return This builder
         */
        public Builder setAdmin(boolean admin) {
            this.admin = admin;
            return this;
        }

        /**
         * Sets the folder permission
         *
         * @param folderPermission The folder permission to set
         * @return This builder
         */
        public Builder setFolderPermission(int folderPermission) {
            this.folderPermission = folderPermission;
            return this;
        }

        /**
         * Sets the read permission
         *
         * @param readPermission The read permission to set
         * @return This builder
         */
        public Builder setReadPermission(int readPermission) {
            this.readPermission = readPermission;
            return this;
        }

        /**
         * Sets the write permission
         *
         * @param writePermission The write permission to set
         * @return This builder
         */
        public Builder setWritePermission(int writePermission) {
            this.writePermission = writePermission;
            return this;
        }

        /**
         * Sets the delete permission
         *
         * @param deletePermission The delete permission to set
         * @return This builder
         */
        public Builder setDeletePermission(int deletePermission) {
            this.deletePermission = deletePermission;
            return this;
        }

        /**
         * Builds the immutable permission from this builder's arguments.
         *
         * @return The immutable permission
         */
        public ImmutablePermission build() {
            return new ImmutablePermission(identifier, entity, entityInfo, group, system, admin, folderPermission, readPermission, writePermission, deletePermission, type, legator);
        }
    }

    // ------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link ImmutablePermission}.
     */
    ImmutablePermission(String identifier, int entity, EntityInfo entityInfo, boolean group, int system, boolean admin, int folderPermission, int readPermission, int writePermission, int deletePermission, FolderPermissionType type, String legator) {
        super(entity, group, Permissions.createPermissionBits(folderPermission, readPermission, writePermission, deletePermission, admin));
        this.identifier = null != identifier ? identifier : String.valueOf(entity);
        this.entityInfo = entityInfo;
        this.system = system;
        this.type = type;
        this.legator = legator;
    }

    @Override
    public void setAdmin(final boolean admin) {
        throw new UnsupportedOperationException("ImmutablePermission.setAdmin()");
    }

    @Override
    public void setAllPermissions(final int folderPermission, final int readPermission, final int writePermission, final int deletePermission) {
        throw new UnsupportedOperationException("ImmutablePermission.setAllPermissions()");
    }

    @Override
    public void setDeletePermission(final int permission) {
        throw new UnsupportedOperationException("ImmutablePermission.setDeletePermission()");
    }

    @Override
    public void setIdentifier(String identifier) {
        throw new UnsupportedOperationException("ImmutablePermission.setIdentifier()");
    }

    @Override
    public void setEntity(final int entity) {
        throw new UnsupportedOperationException("ImmutablePermission.setEntity()");
    }

    @Override
    public void setEntityInfo(EntityInfo entityInfo) {
        throw new UnsupportedOperationException("ImmutablePermission.setEntityInfo()");
    }

    @Override
    public void setFolderPermission(final int permission) {
        throw new UnsupportedOperationException("ImmutablePermission.setFolderPermission()");
    }

    @Override
    public void setGroup(final boolean group) {
        throw new UnsupportedOperationException("ImmutablePermission.setGroup()");
    }

    @Override
    public void setMaxPermissions() {
        throw new UnsupportedOperationException("ImmutablePermission.setMaxPermissions()");
    }

    @Override
    public void setNoPermissions() {
        throw new UnsupportedOperationException("ImmutablePermission.setNoPermissions()");
    }

    @Override
    public void setReadPermission(final int permission) {
        throw new UnsupportedOperationException("ImmutablePermission.setReadPermission()");
    }

    @Override
    public void setSystem(final int system) {
        throw new UnsupportedOperationException("ImmutablePermission.setSystem()");
    }

    @Override
    public void setWritePermission(final int permission) {
        throw new UnsupportedOperationException("ImmutablePermission.setWritePermission()");
    }

    @Override
    public void setType(FolderPermissionType type) {
        throw new UnsupportedOperationException("ImmutablePermission.setType()");
    }

    @Override
    public void setPermissionLegator(String legator) {
        throw new UnsupportedOperationException("ImmutablePermission.setPermissionLegator()");
    }

}
