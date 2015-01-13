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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.guest.internal;

import java.sql.Connection;
import java.sql.SQLException;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.guest.GuestExceptionCodes;
import com.openexchange.server.ServiceLookup;

/**
 *
 * Used to handle connections transactionally.
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since 7.8.0
 */
public class ConnectionHelper {

    private final ServiceLookup services;
    private final boolean ownsConnection;
    private final Connection connection;
    private boolean committed;

    /**
     * Initializes a new {@link ConnectionHelper}.
     *
     * @param contextID The context ID
     * @param services The service lookup
     * @param needsWritable <code>true</code> if a writable connection is required, <code>false</code>, otherwise
     * @throws OXException
     */
    public ConnectionHelper(ServiceLookup services, boolean needsWritable) throws OXException {
        super();
        this.services = services;
        DatabaseService dbService = services.getService(DatabaseService.class);
        this.connection = needsWritable ? dbService.getWritable() : dbService.getReadOnly();
        this.ownsConnection = true;
    }

    /**
     * Gets the underlying connection.
     *
     * @return the connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Starts the transaction on the underlying connection in case the connection is owned by this instance.
     *
     * @throws OXException
     */
    public void start() throws OXException {
        if (ownsConnection) {
            try {
                Databases.startTransaction(connection);
            } catch (SQLException e) {
                throw GuestExceptionCodes.DB_ERROR.create(e, e.getMessage());
            }
        }
    }

    /**
     * Commits the transaction on the underlying connection in case the connection is owned by this instance.
     *
     * @throws OXException
     */
    public void commit() throws OXException {
        if (ownsConnection) {
            try {
                connection.commit();
            } catch (SQLException e) {
                throw GuestExceptionCodes.DB_ERROR.create(e, e.getMessage());
            }
            committed = true;
        }
    }

    /**
     * Backs the underlying connection in case the connection is owned by this instance, rolling back automatically if not yet committed.
     *
     * @throws OXException
     */
    public void finish() {
        if (ownsConnection) {
            if (false == committed) {
                Databases.rollback(connection);
            }
            Databases.autocommit(connection);
            services.getService(DatabaseService.class).backWritable(connection);
        }
    }
}
