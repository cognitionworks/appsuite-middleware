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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.imap;

import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.imap.acl.ACLExtension;
import com.openexchange.imap.config.IMAPConfig;
import com.openexchange.imap.entity2acl.Entity2ACL;
import com.openexchange.imap.entity2acl.Entity2ACLArgs;
import com.openexchange.imap.entity2acl.UserGroupID;
import com.openexchange.mail.permission.MailPermission;
import com.openexchange.server.impl.OCLPermission;
import com.sun.mail.imap.ACL;
import com.sun.mail.imap.Rights;

/**
 * {@link ACLPermission} - Maps existing folder permissions to corresponding IMAP ACL.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ACLPermission extends MailPermission {

    private static final long serialVersionUID = -3140342221453395764L;

    private static final transient org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ACLPermission.class);

    private transient ACL acl;

    private int canRename;

    /**
     * Initializes a new {@link ACLPermission}.
     */
    public ACLPermission() {
        super();
        canRename = -1;
    }

    @Override
    public void setEntity(final int entity) {
        super.setEntity(entity);
        acl = null;
    }

    @Override
    public void setFolderAdmin(final boolean folderAdmin) {
        super.setFolderAdmin(folderAdmin);
        acl = null;
    }

    @Override
    public void setGroupPermission(final boolean groupPermission) {
        super.setGroupPermission(groupPermission);
        acl = null;
    }

    @Override
    public boolean setFolderPermission(final int p) {
        acl = null;
        return super.setFolderPermission(p);
    }

    @Override
    public boolean setReadObjectPermission(final int p) {
        acl = null;
        return super.setReadObjectPermission(p);
    }

    @Override
    public boolean setWriteObjectPermission(final int p) {
        acl = null;
        return super.setWriteObjectPermission(p);
    }

    @Override
    public boolean setDeleteObjectPermission(final int p) {
        acl = null;
        return super.setDeleteObjectPermission(p);
    }

    @Override
    public boolean setAllObjectPermission(final int pr, final int pw, final int pd) {
        acl = null;
        return super.setAllObjectPermission(pr, pw, pd);
    }

    @Override
    public boolean setAllPermission(final int fp, final int opr, final int opw, final int opd) {
        acl = null;
        return super.setAllPermission(fp, opr, opw, opd);
    }

    private static final String ERR = "This method is not applicable to an ACL permission";

    @Override
    public void setFuid(final int pid) {
        if (LOG.isWarnEnabled()) {
            LOG.warn(ERR);
        }
    }

    @Override
    public int getFuid() {
        if (LOG.isWarnEnabled()) {
            LOG.warn(ERR);
        }
        return -1;
    }

    @Override
    public void reset() {
        super.reset();
        acl = null;
    }

    @Override
    public int canRename() {
        return canRename;
    }
    
    /**
     * Sets the rename flag.
     * 
     * @param canRename The rename flag
     */
    public void setCanRename(final int canRename) {
        this.canRename = canRename;
    }

    /*-
     * Full rights: "acdilprsw"
     */

    /**
     * Maps this permission to ACL rights and fills them into an instance of {@link ACL}.
     * 
     * @param args The IMAP-server-specific arguments used for mapping
     * @param imapConfig The user's IMAP configuration
     * @param ctx The context
     * @return An instance of {@link ACL} representing this permission's rights
     * @throws AbstractOXException If this permission cannot be mapped to an instance of {@link ACL}
     */
    public ACL getPermissionACL(final Entity2ACLArgs args, final IMAPConfig imapConfig, final Context ctx) throws AbstractOXException {
        if (acl != null) {
            /*
             * Return caches ACL
             */
            return acl;
        }
        final Rights rights = permission2Rights(this, imapConfig);
        return (acl = new ACL(Entity2ACL.getInstance(imapConfig).getACLName(getEntity(), ctx, args), rights));
    }

    /**
     * Parses the rights given through specified instance of {@link ACL} into this permission object.
     * 
     * @param acl The source instance of {@link ACL}
     * @param args The IMAP-server-specific arguments used for mapping
     * @param imapConfig The user's IMAP configuration
     * @param ctx The context
     * @throws AbstractOXException If given ACL cannot be applied to this permission
     */
    public void parseACL(final ACL acl, final Entity2ACLArgs args, final IMAPConfig imapConfig, final Context ctx) throws AbstractOXException {
        final UserGroupID res = Entity2ACL.getInstance(imapConfig).getEntityID(acl.getName(), ctx, args);
        setEntity(res.getId());
        setGroupPermission(res.isGroup());
        parseRights(acl.getRights(), imapConfig);
        this.acl = acl;
    }

    /**
     * Parses given rights into this permission object
     * 
     * @param rights -The rights to parse
     * @param imapConfig The IMAP configuration
     * @throws IMAPException If an IMAP error occurs
     */
    public void parseRights(final Rights rights, final IMAPConfig imapConfig) throws IMAPException {
        rights2Permission(rights, this, imapConfig);
        canRename = imapConfig.getACLExtension().canCreate(rights) ? 1 : 0;
    }

    /**
     * Maps given permission to rights
     * 
     * @param permission The permission
     * @param imapConfig The IMAP configuration
     * @return Mapped rights
     */
    public static Rights permission2Rights(final OCLPermission permission, final IMAPConfig imapConfig) {
        final Rights rights = new Rights();
        final ACLExtension aclExtension = imapConfig.getACLExtension();
        boolean hasAnyRights = false;
        if (permission.isFolderAdmin()) {
            aclExtension.addFolderAdminRights(rights);
            hasAnyRights = true;
        }
        if (permission.canCreateSubfolders()) {
            aclExtension.addCreateSubfolders(rights);
            hasAnyRights = true;
        } else if (permission.canCreateObjects()) {
            aclExtension.addCreateObjects(rights);
            hasAnyRights = true;
        } else if (permission.isFolderVisible()) {
            aclExtension.addFolderVisibility(rights);
            hasAnyRights = true;
        }
        if (permission.getReadPermission() >= OCLPermission.READ_ALL_OBJECTS) {
            aclExtension.addReadAllKeepSeen(rights);
            hasAnyRights = true;
        }
        if (permission.getWritePermission() >= OCLPermission.WRITE_ALL_OBJECTS) {
            aclExtension.addWriteAll(rights);
            hasAnyRights = true;
        }
        if (permission.getDeletePermission() >= OCLPermission.DELETE_ALL_OBJECTS) {
            aclExtension.addDeleteAll(rights);
            hasAnyRights = true;
        }
        if (hasAnyRights) {
            aclExtension.addNonMappable(rights);
        }
        return rights;
    }

    /**
     * Parses specified rights into given permission object. If the latter parameter is left to <code>null</code>, a new instance of
     * {@link OCLPermission} is going to be created, filled, and returned. Otherwise the given instance of {@link OCLPermission} is filled
     * and returned.
     * 
     * @param rights The rights to parse
     * @param permission The permission object which may be <code>null</code>
     * @param imapConfig The IMAP configuration
     * @return The corresponding permission
     */
    public static OCLPermission rights2Permission(final Rights rights, final OCLPermission permission, final IMAPConfig imapConfig) {
        final OCLPermission oclPermission = permission == null ? new OCLPermission() : permission;
        final ACLExtension aclExtension = imapConfig.getACLExtension();
        /*
         * Folder admin
         */
        oclPermission.setFolderAdmin(aclExtension.containsFolderAdminRights(rights));
        /*
         * Folder permission
         */
        if (aclExtension.containsCreateSubfolders(rights)) {
            oclPermission.setFolderPermission(OCLPermission.CREATE_SUB_FOLDERS);
        } else if (aclExtension.containsCreateObjects(rights)) {
            oclPermission.setFolderPermission(OCLPermission.CREATE_OBJECTS_IN_FOLDER);
        } else if (aclExtension.containsFolderVisibility(rights)) {
            oclPermission.setFolderPermission(OCLPermission.READ_FOLDER);
        } else {
            oclPermission.setFolderPermission(OCLPermission.NO_PERMISSIONS);
        }
        /*
         * Read permission
         */
        if (aclExtension.containsReadAll(rights)) {
            oclPermission.setReadObjectPermission(OCLPermission.READ_ALL_OBJECTS);
        } else {
            oclPermission.setReadObjectPermission(OCLPermission.NO_PERMISSIONS);
        }
        /*
         * Write permission
         */
        if (aclExtension.containsWriteAll(rights)) {
            oclPermission.setWriteObjectPermission(OCLPermission.WRITE_ALL_OBJECTS);
        } else {
            oclPermission.setWriteObjectPermission(OCLPermission.NO_PERMISSIONS);
        }
        /*
         * Delete permission
         */
        if (aclExtension.containsDeleteAll(rights)) {
            oclPermission.setDeleteObjectPermission(OCLPermission.DELETE_ALL_OBJECTS);
        } else {
            oclPermission.setDeleteObjectPermission(OCLPermission.NO_PERMISSIONS);
        }
        return oclPermission;
    }

    @Override
    public Object clone() {
        try {
            final ACLPermission clone = (ACLPermission) super.clone();
            // if (null == acl) {
            // clone.acl = null;
            // } else {
            // clone.acl = new ACL(acl.getName(), (Rights) acl.getRights().clone());
            // }
            clone.acl = null;
            return clone;
        } catch (final CloneNotSupportedException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException("CloneNotSupportedException even though it's cloenable", e);
        }
    }
}
