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

package com.openexchange.admin.rmi.impl;

import com.openexchange.admin.plugins.ContextDbLookupPluginInterface;
import com.openexchange.admin.plugins.OXContextPluginInterface;
import com.openexchange.admin.plugins.PluginException;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.Database;
import com.openexchange.admin.rmi.dataobjects.SchemaSelectStrategy;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.dataobjects.UserModuleAccess;
import com.openexchange.admin.rmi.exceptions.ContextExistsException;
import com.openexchange.admin.rmi.exceptions.EnforceableDataObjectException;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.services.PluginInterfaces;
import com.openexchange.admin.storage.interfaces.OXToolStorageInterface;
import com.openexchange.admin.tools.GenericChecks;
import com.openexchange.exception.LogLevel;

public abstract class OXContextCommonImpl extends OXCommonImpl {

    private final static org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(OXContextCommonImpl.class);

    /**
     * Initializes a new {@link OXContextCommonImpl}.
     *
     */
    protected OXContextCommonImpl() {
        super();
    }

    protected void createchecks(final Context ctx, final User admin_user, @SuppressWarnings("unused") final OXToolStorageInterface tool) throws StorageException, InvalidDataException {
        try {
            Boolean ret = null;

            // Trigger plugin extensions
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXContextPluginInterface oxContextPlugin : pluginInterfaces.getContextPlugins().getServiceList()) {
                        ret = oxContextPlugin.checkMandatoryMembersContextCreate(ctx);
                    }
                }
            }

            if (ret == null || ret.booleanValue()) {
                if (!ctx.mandatoryCreateMembersSet()) {
                    throw new InvalidDataException("Mandatory fields in context not set: " + ctx.getUnsetMembers());
                }
            }
        } catch (EnforceableDataObjectException e) {
            throw new InvalidDataException(e.getMessage());
        } catch (PluginException e) {
            throw StorageException.wrapForRMI(e);
        }

        try {
            if (!admin_user.mandatoryCreateMembersSet()) {
                throw new InvalidDataException("Mandatory fields in admin user not set: " + admin_user.getUnsetMembers());
            }
        } catch (EnforceableDataObjectException e) {
            throw new InvalidDataException(e.getMessage());
        }

        GenericChecks.checkValidMailAddress(admin_user.getPrimaryEmail());

        checkUserAttributes(admin_user);
    }

    protected abstract Context createmaincall(final Context ctx, final User admin_user, Database db, UserModuleAccess access, final Credentials auth, SchemaSelectStrategy schemaSelectStrategy) throws StorageException, InvalidDataException, ContextExistsException;

    protected SchemaSelectStrategy getDefaultSchemaSelectStrategy() {
        return SchemaSelectStrategy.getDefault();
    }

    protected Context createcommon(final Context ctx, final User admin_user, final Database db, final UserModuleAccess access, final Credentials auth, SchemaSelectStrategy schemaSelectStrategy) throws InvalidCredentialsException, ContextExistsException, InvalidDataException, StorageException {
        try {
            try {
                doNullCheck(ctx, admin_user);
            } catch (InvalidDataException e1) {
                final InvalidDataException invalidDataException = new InvalidDataException("Context or user not correct");
                log(LogLevel.ERROR, LOGGER, auth, invalidDataException, "");
                throw invalidDataException;
            }

            BasicAuthenticator.createPluginAwareAuthenticator().doAuthentication(auth);

            log(LogLevel.DEBUG, LOGGER, auth, null, "{} - {}", ctx, admin_user);
            final OXToolStorageInterface tool = OXToolStorageInterface.getInstance();
            Context ret = ctx;
            callBeforeDbLookupPluginMethods(new Context[] { ret }, auth);
            if (isAnyPluginLoaded()) {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXContextPluginInterface contextInterface : pluginInterfaces.getContextPlugins().getServiceList()) {
                        ret = contextInterface.preCreate(ret, admin_user, auth);
                    }
                }
            }

            createchecks(ret, admin_user, tool);

            // Ensure context identifier is contained in login mappings
            {
                final String sContextId = ret.getIdAsString();
                if (null != sContextId) {
                    ret.addLoginMapping(sContextId);
                }
            }

            // Ensure context name is contained in login mappings
            {
                final String name = ret.getName();
                if (null != name) {
                    // Add the name of the context to the login mappings and the id
                    ret.addLoginMapping(name);
                }
            }

            final Context retval = createmaincall(ret, admin_user, db, access, auth, schemaSelectStrategy);
            callAfterDbLookupPluginMethods(new Context[] { retval }, auth);
            if (retval.getName() == null) {
                retval.setName(String.valueOf(retval.getId()));
            }
            return retval;
        } catch (PluginException e) {
            throw logAndReturnException(LOGGER, StorageException.wrapForRMI(e), auth, ctx.getIdAsString());
        }
    }

    /**
     * Check if plugins are loaded
     *
     * @return <code>true</code> if a plugin is loaded
     */
    protected boolean isAnyPluginLoaded() {
        final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
        return null != pluginInterfaces && false == pluginInterfaces.getContextPlugins().getServiceList().isEmpty();
    }

    /**
     * Execute <i>beforeContextDbLookup</i> method of plugins registered for {@link ContextDbLookupPluginInterface}
     *
     * @param ctxs The context's
     * @param credentials The admin credentials
     * @throws StorageException When plugin exceptions occurred
     */
    protected void callBeforeDbLookupPluginMethods(final Context[] ctxs, final Credentials credentials) throws StorageException {
        final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
        if (null == pluginInterfaces) {
            return;
        }
        for (final ContextDbLookupPluginInterface dbLookupPlugin : pluginInterfaces.getDBLookupPlugins().getServiceList()) {
            try {
                log(LogLevel.DEBUG, LOGGER, credentials, null, "Calling beforeDBLookup for plugin: {}", dbLookupPlugin.getClass().getName());
                dbLookupPlugin.beforeContextDbLookup(credentials, ctxs);
            } catch (PluginException e) {
                log(LogLevel.ERROR, LOGGER, credentials, e, "Error while calling beforeDBLookup of plugin {}", dbLookupPlugin.getClass().getName());
                throw StorageException.wrapForRMI(e);
            }
        }
    }

    protected void callAfterDbLookupPluginMethods(final Context[] ctxs, final Credentials auth) throws StorageException {
        final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
        if (null == pluginInterfaces) {
            return;
        }
        for (final ContextDbLookupPluginInterface dbLookupPlugin : pluginInterfaces.getDBLookupPlugins().getServiceList()) {
            try {
                log(LogLevel.DEBUG, LOGGER, auth, null, "Calling afterContextDBLookup for plugin: {}", dbLookupPlugin.getClass().getName());
                dbLookupPlugin.afterContextDbLookup(auth, ctxs);
            } catch (PluginException e) {
                log(LogLevel.ERROR, LOGGER, auth, e, "Error while calling afterContextDBLookup of plugin {}", dbLookupPlugin.getClass().getName());
                throw StorageException.wrapForRMI(e);
            }
        }
    }

    protected String callSearchDbLookupPluginMethods(String searchPattern, Credentials auth) throws StorageException {
        final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
        if (null == pluginInterfaces) {
            return searchPattern;
        }

        String search_pattern = searchPattern;
        for (ContextDbLookupPluginInterface dblu : pluginInterfaces.getDBLookupPlugins().getServiceList()) {
            log(LogLevel.DEBUG, LOGGER, auth, null, "Calling searchTermDBLookup for plugin: {}", dblu.getClass().getName());
            try {
                String new_search_pattern = dblu.searchPatternDbLookup(auth, search_pattern);
                if (null != new_search_pattern) {
                    search_pattern = new_search_pattern;
                }
            } catch (PluginException e) {
                throw logAndReturnException(LOGGER, StorageException.wrapForRMI(e), auth);
            }
        }
        return search_pattern;
    }

}
