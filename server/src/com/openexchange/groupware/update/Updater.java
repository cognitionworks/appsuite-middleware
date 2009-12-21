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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.groupware.update;

import com.openexchange.groupware.contexts.Context;

/**
 * Interface for the updater.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public abstract class Updater {

    /**
     * Default constructor.
     */
    protected Updater() {
        super();
    }

    /**
     * Factory method to get an updater.
     * @return the updater.
     * @throws UpdateException if instantiating the implementation fails.
     */
    public static Updater getInstance() {
        return new UpdaterImpl();
    }

    /**
     * @param ctx Context inside the schema.
     * @return <code>true</code> if the schema must be updated.
     * @throws UpdateException if an exception occurs.
     */
    public final boolean toUpdate(Context ctx) throws UpdateException {
        return toUpdate(ctx.getContextId());
    }

    /**
     * @param contextId Identifier of a context inside the schema.
     * @return <code>true</code> if the schema must be updated.
     * @throws UpdateException if an exception occurs.
     */
    public abstract boolean toUpdate(int contextId) throws UpdateException;

    /**
     * Starts the update process on a schema.
     * @param contextId Context inside the schema.
     * @throws UpdateException if an exception occurs.
     */
    public final void startUpdate(Context ctx) throws UpdateException {
        startUpdate(ctx.getContextId());
    }

    /**
     * Starts the update process on a schema.
     * @param contextId Identifier of a context inside the schema.
     * @throws UpdateException if an exception occurs.
     */
    public abstract void startUpdate(int contextId) throws UpdateException;

    /**
     * @param ctx Context inside the schema.
     * @return <code>true</code> if the schema the context resides in is
     * currently updated.
     * @throws UpdateException if an exception occurs.
     */
    public final boolean isLocked(Context ctx) throws UpdateException {
        return isLocked(ctx.getContextId());
    }

    /**
     * @param contextId Identifier of a context inside the schema.
     * @return <code>true</code> if the schema the context resides in is currently updated.
     * @throws UpdateException if an exception occurs.
     */
    public abstract boolean isLocked(int contextId) throws UpdateException;

    /**
     * Determines if given database schema is currently locked due to a running
     * update process
     *
     * @param schema -
     *            the schema name
     * @param writePoolId -
     *            the ID of write pool (master database)
     * @return <code>true</code> if the schema is currently updates; otherwise
     *         <code>false</code>
     * @throws UpdateException - if any exception occurs
     */
    public abstract boolean isLocked(final String schema, final int writePoolId)
        throws UpdateException;

    /**
     * Determines if given database schema needs to be updated
     *
     * @param schema -
     *            the schema name
     * @param writePoolId -
     *            the ID of write pool (master database)
     * @return <code>true</code> if the schema needs to be updated; otherwise <code>false</code>
     * @throws UpdateException - if any exception occurs
     */
    public abstract boolean toUpdate(final String schema, final int writePoolId)
        throws UpdateException;
}
