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

package com.openexchange.groupware.contexts.impl;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException.Code;
import com.openexchange.session.Session;

/**
 * This class defines the methods for accessing the storage of contexts.
 * TODO We should introduce a logic layer above this context storage layer. That layer should then trigger the update tasks.
 * Nearly all accesses to the ContextStorage need then to be replaced with an access to the ContextService.
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class ContextStorage {

    /**
     * Logger.
     */
    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(ContextStorage.class));

    /**
     * Singleton implementation.
     */
    private static volatile ContextStorage impl;

    /**
     * Will be returned if a context cannot be found through its login info.
     */
    public static final int NOT_FOUND = -1;

    /**
     * Creates an instance implementing the context storage.
     * 
     * @return an instance implementing the context storage.
     */
    public static ContextStorage getInstance() {
        return impl;
    }

    /**
     * Instantiates an implementation of the context interface and fill its attributes according to the needs to be able to separate
     * contexts.
     * 
     * @param loginContextInfo the login info for the context.
     * @return the unique identifier of the context or <code>-1</code> if no matching context exists.
     * @throws ContextException if an error occurs.
     */
    public abstract int getContextId(String loginContextInfo) throws ContextException;

    public final Context getContext(final Session session) throws ContextException {
        return getContext(session.getContextId());
    }

    /**
     * Creates a context implementation for the given context unique identifier.
     * 
     * @param contextId unique identifier of the context.
     * @return an implementation of the context or <code>null</code> if the context with the given identifier can't be found.
     * @throws ContextException if an error occurs.
     */
    public Context getContext(final int contextId) throws ContextException {
        final Context retval = loadContext(contextId);
        if (retval.isUpdating()) {
            throw new ContextException(Code.UPDATE);
        }
        return retval;
    }

    /**
     * Loads the context object.
     * 
     * @param contextId unique identifier of the context to load.
     * @return the context object.
     * @throws ContextException if loading the context fails.
     */
    public abstract ContextExtended loadContext(int contextId) throws ContextException;

    /**
     * Invalidates the context object in cache(s).
     * 
     * @param contextId unique identifier of the context to invalidate
     * @throws ContextException if invalidating the context fails
     */
    public void invalidateContext(final int contextId) throws ContextException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("invalidateContext not implemented in " + this.getClass().getCanonicalName());
        }
    }

    /**
     * Invalidates a login information in the cache.
     * 
     * @param loginContextInfo login information to invalidate.
     * @throws ContextException if invalidating the login information fails.
     */
    public void invalidateLoginInfo(final String loginContextInfo) throws ContextException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("invalidateLoginInfo not implemented in " + this.getClass().getCanonicalName());
        }
    }

    /**
     * Gives a list of all context ids which are stored in the config database.
     * 
     * @return the list of context ids
     * @throws ContextException if reading the contexts fails.
     */
    public abstract List<Integer> getAllContextIds() throws ContextException;

    /**
     * Internal start-up routine invoked in {@link #start()}
     * 
     * @throws ContextException If an error occurs
     */
    protected abstract void startUp() throws ContextException;

    /**
     * Internal shut-down routine invoked in {@link #stop()}
     * 
     * @throws ContextException If an error occurs
     */
    protected abstract void shutDown() throws ContextException;

    /**
     * Initialization.
     * 
     * @throws ContextException if initialization of contexts fails.
     */
    public static void start() throws ContextException {
        if (null != impl) {
            LOG.error("Duplicate initialization of ContextStorage.");
            return;
        }
        impl = new CachingContextStorage(new RdbContextStorage());
        impl.startUp();
    }

    /**
     * Shutdown.
     */
    public static void stop() throws ContextException {
        if (null == impl) {
            LOG.error("Duplicate shutdown of ContextStorage.");
            return;
        }
        impl.shutDown();
        impl = null;
    }

    /**
     * Convenience method for getting the context.
     * 
     * @param session The session providing the context ID
     * @return the context data object or null if the context with the given identifier can't be found.
     * @throws ContextException if getting the context fails.
     */
    public static Context getStorageContext(final Session session) throws ContextException {
        return getStorageContext(session.getContextId());
    }

    /**
     * Convenience method for getting the context.
     * 
     * @param contextId unique identifier of the context.
     * @return the context data object or null if the context with the given identifier can't be found.
     * @throws ContextException if getting the context fails.
     */
    public static Context getStorageContext(final int contextId) throws ContextException {
        return getInstance().getContext(contextId);
    }
}
