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

package com.openexchange.messaging;

/**
 * {@link MessagingPermissions} - Tools for {@link MessagingPermission} class.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MessagingPermissions {

    /**
     * Initializes a new {@link MessagingPermissions}.
     */
    private MessagingPermissions() {
        super();
    }

    /**
     * Gets an unmodifiable view of the specified permission. Attempts to modify the returned permission result in an
     * <tt>UnsupportedOperationException</tt>.
     * 
     * @param messagingPermission The messaging permission
     * @return An unmodifiable view of the specified permission
     */
    public static MessagingPermission unmodifiablePermission(final MessagingPermission messagingPermission) {
        return new UnmodifiableMessagingPermission(messagingPermission);
    }

    static final class UnmodifiableMessagingPermission implements MessagingPermission {

        private MessagingPermission delegate;

        UnmodifiableMessagingPermission(final MessagingPermission delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public Object clone() {
            try {
                final UnmodifiableMessagingPermission clone = (UnmodifiableMessagingPermission) super.clone();
                clone.delegate = (MessagingPermission) (null == delegate ? null : delegate.clone());
                return clone;
            } catch (final CloneNotSupportedException e) {
                throw new InternalError(e.getMessage());
            }
        }

        @Override
        public boolean equals(final Object obj) {
            return delegate.equals(obj);
        }

        public int getDeletePermission() {
            return delegate.getDeletePermission();
        }

        public int getEntity() {
            return delegate.getEntity();
        }

        public int getFolderPermission() {
            return delegate.getFolderPermission();
        }

        public int getReadPermission() {
            return delegate.getReadPermission();
        }

        public int getSystem() {
            return delegate.getSystem();
        }

        public int getWritePermission() {
            return delegate.getWritePermission();
        }

        public boolean isAdmin() {
            return delegate.isAdmin();
        }

        public boolean isGroup() {
            return delegate.isGroup();
        }

        public void setAdmin(final boolean admin) {
            throw new UnsupportedOperationException("MessagingPermissions.UnmodifiableMessagingPermission.setAdmin()");
        }

        public void setAllPermissions(final int folderPermission, final int readPermission, final int writePermission, final int deletePermission) {
            throw new UnsupportedOperationException("MessagingPermissions.UnmodifiableMessagingPermission.setAllPermissions()");
        }

        public void setDeletePermission(final int permission) {
            throw new UnsupportedOperationException("MessagingPermissions.UnmodifiableMessagingPermission.setDeletePermission()");
        }

        public void setEntity(final int entity) {
            throw new UnsupportedOperationException("MessagingPermissions.UnmodifiableMessagingPermission.setEntity()");
        }

        public void setFolderPermission(final int permission) {
            throw new UnsupportedOperationException("MessagingPermissions.UnmodifiableMessagingPermission.setFolderPermission()");
        }

        public void setGroup(final boolean group) {
            throw new UnsupportedOperationException("MessagingPermissions.UnmodifiableMessagingPermission.setGroup()");
        }

        public void setMaxPermissions() {
            throw new UnsupportedOperationException("MessagingPermissions.UnmodifiableMessagingPermission.setMaxPermissions()");
        }

        public void setNoPermissions() {
            throw new UnsupportedOperationException("MessagingPermissions.UnmodifiableMessagingPermission.setNoPermissions()");
        }

        public void setReadPermission(final int permission) {
            throw new UnsupportedOperationException("MessagingPermissions.UnmodifiableMessagingPermission.setReadPermission()");
        }

        public void setSystem(final int system) {
            throw new UnsupportedOperationException("MessagingPermissions.UnmodifiableMessagingPermission.setSystem()");
        }

        public void setWritePermission(final int permission) {
            throw new UnsupportedOperationException("MessagingPermissions.UnmodifiableMessagingPermission.setWritePermission()");
        }

    } // End of UnmodifiableMessagingPermission

}
