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
import java.util.Date;
import com.openexchange.database.DBPoolingException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.delete.DeleteEvent;
import com.openexchange.groupware.delete.DeleteFailedException;
import com.openexchange.groupware.delete.DeleteRegistry;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.resource.Resource;
import com.openexchange.resource.ResourceException;
import com.openexchange.resource.storage.ResourceStorage;
import com.openexchange.resource.storage.ResourceStorage.StorageType;
import com.openexchange.server.impl.DBPool;
import com.openexchange.tools.sql.DBUtils;

/**
 * {@link ResourceDelete} - Performs update of a {@link Resource resource}.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ResourceDelete {

    private static final org.apache.commons.logging.Log LOG = com.openexchange.exception.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(ResourceDelete.class));

    private final User user;

    private final Context ctx;

    private final Resource resource;

    private final ResourceStorage storage;

    private final Date clientLastModified;

    private transient Resource orig;

    /**
     * Initializes a new {@link ResourceDelete}
     * 
     * @param ctx The context
     * @param resource The resource to update
     * @param clientLastModified The client last-modified timestamp; may be <code>null</code> to omit timestamp comparison
     */
    ResourceDelete(final User user, final Context ctx, final Resource resource, final Date clientLastModified) {
        super();
        this.user = user;
        this.ctx = ctx;
        this.resource = resource;
        this.clientLastModified = clientLastModified;
        storage = ResourceStorage.getInstance();
    }

    private Resource getOrig() throws ResourceException {
        if (null == orig) {
            try {
                orig = storage.getResource(resource.getIdentifier(), ctx);
            } catch (final LdapException e) {
                throw new ResourceException(e);
            }
        }
        return orig;
    }

    /**
     * Performs the delete.
     * <ol>
     * <li>At first all necessary checks are performed: data completeness, data validation, permission, and check for duplicate resources.</li>
     * <li>Then the transaction-bounded delete in storage takes place</li>
     * <li>At last, the delete is propagated to system (cache invalidation, etc.)</li>
     * </ol>
     * 
     * @throws ResourceException If delete fails
     */
    void perform() throws ResourceException {
        allow();
        check();
        delete();
        propagate();
    }

    /**
     * Checks permission
     * 
     * @throws ResourceException If permission is denied
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
     * Checks permission: Invoke {@link BundleAccessSecurityService#checkPermission(String[], String) checkPermission()} on
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
     * This method performs all necessary checks before updating a resource.
     * 
     * @throws ResourceException If a problem was detected during checks.
     */
    private void check() throws ResourceException {
        if (null == resource) {
            throw new ResourceException(ResourceException.Code.NULL);
        }
        /*
         * Check mandatory fields
         */
        if (!resource.isIdentifierSet() || -1 == resource.getIdentifier()) {
            throw new ResourceException(ResourceException.Code.MANDATORY_FIELD);
        }
        /*
         * Check existence
         */
        getOrig();
        /*
         * Check timestamp
         */
        if (clientLastModified != null && clientLastModified.getTime() < getOrig().getLastModified().getTime()) {
            throw new ResourceException(ResourceException.Code.CONCURRENT_MODIFICATION);
        }
    }

    /**
     * Deletes all data for the resource in database.
     * 
     * @throws ResourceException
     */
    private void delete() throws ResourceException {
        final Connection con;
        try {
            con = DBPool.pickupWriteable(ctx);
        } catch (final DBPoolingException e) {
            throw new ResourceException(ResourceException.Code.NO_CONNECTION, e);
        }
        try {
            con.setAutoCommit(false);
            propagateDelete(con);
            delete(con);
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

    private void propagateDelete(final Connection con) throws ResourceException {
        /*
         * Delete all references to resource
         */
        final DeleteEvent event = new DeleteEvent(getOrig(), resource.getIdentifier(), DeleteEvent.TYPE_RESOURCE, ctx);
        try {
            DeleteRegistry.getInstance().fireDeleteEvent(event, con, con);
        } catch (final DeleteFailedException e) {
            throw new ResourceException(e);
        }
    }

    /**
     * Propagates insertion to system: Possible cache invalidation, etc.
     */
    private void propagate() {
        // TODO: Check if any caches should be invalidated
    }

    /**
     * This method calls the plain delete methods.
     * 
     * @param con writable database connection in transaction or not.
     * @throws ResourceException if some problem occurs.
     */
    void delete(final Connection con) throws ResourceException {
        storage.deleteResource(ctx, con, resource);
        /*
         * Put into delete table
         */
        final Resource orig = getOrig();
        resource.setAvailable(orig.isAvailable());
        resource.setDescription(orig.getDescription());
        resource.setDisplayName(orig.getDisplayName());
        resource.setIdentifier(orig.getIdentifier());
        resource.setMail(orig.getMail());
        resource.setSimpleName(orig.getSimpleName());
        resource.setLastModified(System.currentTimeMillis());
        storage.insertResource(ctx, con, resource, StorageType.DELETED);
    }

}
