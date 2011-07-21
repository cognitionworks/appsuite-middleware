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

package com.openexchange.group.internal;

import static com.openexchange.java.Autoboxing.I;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.database.DBPoolingException;
import com.openexchange.group.Group;
import com.openexchange.group.GroupException;
import com.openexchange.group.GroupStorage;
import com.openexchange.group.GroupException.Code;
import com.openexchange.group.GroupStorage.StorageType;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.delete.DeleteEvent;
import com.openexchange.groupware.delete.DeleteFailedException;
import com.openexchange.groupware.delete.DeleteRegistry;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserException;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.userconfiguration.UserConfigurationException;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.server.impl.DBPool;
import com.openexchange.tools.sql.DBUtils;

/**
 * This class integrates all operations to be done for deleting a group.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class Delete {

    /**
     * Logger.
     */
    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(Delete.class));

    /**
     * Context.
     */
    private final Context ctx;

    /**
     * User for checking permissions.
     */
    private final User user;

    /**
     * Unique identifier of the group to delete.
     */
    private final int groupId;

    private final Date lastRead;

    /**
     * Storage API for groups.
     */
    private static final GroupStorage storage = GroupStorage.getInstance();

    /**
     * cache field for the group.
     */
    private transient Group orig;

    /**
     * Default constructor.
     * @param ctx Context.
     * @param user User for permission checks.
     * @param groupId unique identifier of the group to delete.
     */
    Delete(final Context ctx, final User user, final int groupId,
        final Date lastRead) {
        super();
        this.ctx = ctx;
        this.user = user;
        this.groupId = groupId;
        this.lastRead = lastRead;
    }

    Group getOrig() throws GroupException {
        if (null == orig) {
            try {
                orig = storage.getGroup(groupId, ctx);
            } catch (final LdapException e) {
                throw new GroupException(e);
            }
        }
        return orig;
    }

    /**
     * This method integrates all several methods for the different operations
     * of deleting a group.
     * @throws GroupException if something during delete fails.
     */
    void perform() throws GroupException {
        allowed();
        check();
        delete();
        propagate();
    }

    private void allowed() throws GroupException {
        try {
            if (!UserConfigurationStorage.getInstance().getUserConfiguration(user.getId(), ctx).isEditGroup()) {
                throw new GroupException(Code.NO_DELETE_PERMISSION);
            }
            if (groupId == GroupTools.GROUP_ZERO.getIdentifier()) {
                try {
                    throw new GroupException(Code.NO_GROUP_DELETE, GroupTools.getGroupZero(ctx).getDisplayName());
                } catch (final UserException e) {
                    LOG.error(e.getMessage(), e);
                    throw new GroupException(Code.NO_GROUP_DELETE, I(GroupStorage.GROUP_ZERO_IDENTIFIER));
                } catch (final LdapException e) {
                    LOG.error(e.getMessage(), e);
                    throw new GroupException(Code.NO_GROUP_DELETE, I(GroupStorage.GROUP_ZERO_IDENTIFIER));
                }
            }
        } catch (final UserConfigurationException e) {
            throw new GroupException(e);
        }
    }

    private void check() throws GroupException {
        // Does the group exist?
        getOrig();
        // Group 1 can not be deleted
        if (GroupStorage.GROUP_ZERO_IDENTIFIER == groupId || 1 == groupId) {
            throw new GroupException(Code.NO_GROUP_DELETE, getOrig().getDisplayName());
        }
    }

    /**
     * Deletes all data for the group in the database. This includes deleting
     * everything that references the group.
     * @throws GroupException
     */
    private void delete() throws GroupException {
        final Connection con;
        try {
            con = DBPool.pickupWriteable(ctx);
        } catch (final DBPoolingException e) {
            throw new GroupException(Code.NO_CONNECTION, e);
        }
        try {
            con.setAutoCommit(false);
            propagateDelete(con);
            delete(con);
            con.commit();
        } catch (final SQLException e) {
            DBUtils.rollback(con);
            throw new GroupException(Code.SQL_ERROR, e, e.getMessage());
        } catch (final GroupException e) {
            DBUtils.rollback(con);
            throw e;
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (final SQLException e) {
                LOG.error("Problem setting autocommit to true.", e);
            }
            DBPool.closeWriterSilent(ctx, con);
        }
    }

    private void propagateDelete(final Connection con) throws GroupException {
        // Delete all references to that group.
        final DeleteEvent event = new DeleteEvent(getOrig(), groupId,
            DeleteEvent.TYPE_GROUP, ctx);
        try {
            DeleteRegistry.getInstance().fireDeleteEvent(event, con, con);
        } catch (final DeleteFailedException e) {
            throw new GroupException(e);
        }
    }

    private void delete(final Connection con) throws GroupException {
        // Delete the group.
        storage.deleteMember(ctx, con, getOrig(), getOrig().getMember());
        storage.deleteGroup(ctx, con, groupId, lastRead);
        // Remember as deleted group.
        final Group del = new Group();
        final Group orig = getOrig();
        del.setIdentifier(orig.getIdentifier());
        del.setDisplayName(orig.getDisplayName());
        del.setSimpleName(orig.getSimpleName());
        del.setLastModified(new Date());
        storage.insertGroup(ctx, con, del, StorageType.DELETED);
    }

    /**
     * Inform the rest of the system about the deleted group.
     * @throws GroupException if something during propagate fails.
     */
    private void propagate() throws GroupException {
        UserStorage storage = UserStorage.getInstance();
        try {
            storage.invalidateUser(ctx, getOrig().getMember());
        } catch (UserException e) {
            throw new GroupException(e);
        }
    }
}
