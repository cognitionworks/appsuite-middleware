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

package com.openexchange.resource.internal;

import java.sql.Connection;
import java.sql.SQLException;
import com.openexchange.database.DBPoolingException;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.impl.IDGenerator;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.resource.Resource;
import com.openexchange.resource.ResourceException;
import com.openexchange.resource.storage.ResourceStorage;
import com.openexchange.server.impl.DBPool;
import com.openexchange.tools.sql.DBUtils;

/**
 * {@link ResourceCreate} - Performs insertion of a {@link Resource resource}.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ResourceCreate {

    private static final org.apache.commons.logging.Log LOG = com.openexchange.exception.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(ResourceCreate.class));

    private final User user;

    private final Context ctx;

    private final Resource resource;

    private final ResourceStorage storage;

    /**
     * Initializes a new {@link ResourceCreate}
     * 
     * @param ctx The context
     * @param resource The resource to insert
     */
    ResourceCreate(final User user, final Context ctx, final Resource resource) {
        super();
        this.user = user;
        this.ctx = ctx;
        this.resource = resource;
        storage = ResourceStorage.getInstance();
    }

    /**
     * Performs the insert.
     * <ol>
     * <li>At first permission is checked</li>
     * <li>All necessary checks are performed: data completeness, data validation, and check for duplicate resources.</li>
     * <li>Then the transaction-bounded insert into storage takes place</li>
     * <li>At last, the insert is propagated to system (cache invalidation, etc.)</li>
     * </ol>
     * 
     * @throws ResourceException If insert fails
     */
    void perform() throws ResourceException {
        allow();
        check();
        insert();
        propagate();
    }

    /**
     * Check permission
     * 
     * @throws ResourceException If permission is not granted
     */
    private void allow() throws ResourceException {
        /*
         * At the moment security service is not used for timing reasons but is ought to be used later on
         */
        try {
            if (!UserConfigurationStorage.getInstance().getUserConfiguration(user.getId(), ctx).isEditResource()) {
                throw new ResourceException(ResourceException.Code.PERMISSION, Integer.valueOf(ctx.getContextId()));
            }
        } catch (final OXException e1) {
            throw new ResourceException(e1);
        }
        /*
         * TODO: Remove statements above and replace with commented call below
         */
        // checkBySecurityService();
    }

    /**
     * Check permission: Invoke {@link BundleAccessSecurityService#checkPermission(String[], String) checkPermission()} on
     * {@link BundleAccessSecurityService security service}
     * 
     * @throws ResourceException If permission is not granted
     */
    // private void checkBySecurityService() throws ResourceException {
    // final BundleAccessSecurityService securityService = ServerServiceRegistry.getInstance().getService(
    // BundleAccessSecurityService.class);
    // if (securityService == null) {
    // throw new ResourceException(new OXException(OXException.Code.SERVICE_UNAVAILABLE,
    // BundleAccessSecurityService.class.getName()));
    // }
    // final Set<String> permissions = user.getAttributes().get("permission");
    // try {
    // securityService.checkPermission(permissions == null ? null : permissions.toArray(new String[permissions
    // .size()]), PATH);
    // } catch (final BundleAccessException e) {
    // throw new ResourceException(e);
    // }
    // }
    /**
     * This method performs all necessary checks before creating a resource.
     * 
     * @throws ResourceException if a problem was detected during checks.
     */
    private void check() throws ResourceException {
        if (null == resource) {
            throw new ResourceException(ResourceException.Code.NULL);
        }
        /*
         * Check mandatory fields: identifier, displayName, and mail
         */
        if (isEmpty(resource.getSimpleName()) || isEmpty(resource.getDisplayName()) || isEmpty(resource.getMail())) {
            throw new ResourceException(ResourceException.Code.MANDATORY_FIELD);
        }
        /*
         * Check for invalid values
         */
        if (!ResourceTools.validateResourceIdentifier(resource.getSimpleName())) {
            throw new ResourceException(ResourceException.Code.INVALID_RESOURCE_IDENTIFIER, resource.getSimpleName());
        }
        if (!ResourceTools.validateResourceEmail(resource.getMail())) {
            throw new ResourceException(ResourceException.Code.INVALID_RESOURCE_MAIL, resource.getMail());
        }
        /*
         * Check if another resource with the same textual identifier or email address exists in storage
         */
        try {
            if (storage.searchResources(resource.getSimpleName(), ctx).length > 0) {
                throw new ResourceException(ResourceException.Code.RESOURCE_CONFLICT, resource.getSimpleName());
            }
            if (storage.searchResourcesByMail(resource.getMail(), ctx).length > 0) {
                throw new ResourceException(ResourceException.Code.RESOURCE_CONFLICT_MAIL, resource.getMail());
            }
        } catch (final LdapException e) {
            throw new ResourceException(e);
        }

    }

    /**
     * Inserts all data for the resource into the database.
     * 
     * @throws ResourceException
     */
    private void insert() throws ResourceException {
        final Connection con;
        try {
            con = DBPool.pickupWriteable(ctx);
        } catch (final DBPoolingException e) {
            throw new ResourceException(ResourceException.Code.NO_CONNECTION, e);
        }
        try {
            con.setAutoCommit(false);
            insert(con);
            con.commit();
        } catch (final SQLException e) {
            DBUtils.rollback(con);
            throw new ResourceException(ResourceException.Code.SQL_ERROR, e);
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (final SQLException e) {
                LOG.error("Problem setting autocommit to true.", e);
            }
            DBPool.closeWriterSilent(ctx, con);
        }
    }

    /**
     * Propagates insertion to system: Possible cache invalidation, etc.
     */
    private void propagate() {
        // TODO: Check if any caches should be invalidated
    }

    /**
     * This method calls the plain insert methods.
     * 
     * @param con writable database connection in transaction or not.
     * @throws ResourceException if some problem occurs.
     */
    void insert(final Connection con) throws ResourceException {
        try {
            final int id = IDGenerator.getId(ctx.getContextId(), Types.PRINCIPAL, con);
            resource.setIdentifier(id);
            storage.insertResource(ctx, con, resource);
        } catch (final SQLException e) {
            throw new ResourceException(ResourceException.Code.SQL_ERROR, e);
        }
    }

    private static boolean isEmpty(final String s) {
        if (null == s || s.length() == 0) {
            return true;
        }
        final char[] chars = s.toCharArray();
        for (final char c : chars) {
            if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }
}
