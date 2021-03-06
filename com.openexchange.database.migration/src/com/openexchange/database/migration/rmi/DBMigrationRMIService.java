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

package com.openexchange.database.migration.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * {@link DBMigrationRMIService}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.1
 */
public interface DBMigrationRMIService extends Remote {

    public static final String RMI_NAME = "DBMigrationRMIService";

    /**
     * Force running the core configdb changelog
     * 
     * @param schemaName The name of the schema for which to force the migration
     * @throws RemoteException if an error is occurred
     */
    void forceMigration(String schemaName) throws RemoteException;

    /**
     * Roll-back to the given tag of a change set of the core change log
     *
     * @param schemaName The name of the schema for which to roll-back the migration
     * @param changeSetTag the change set tag
     * @throws RemoteException if an error is occurred
     */
    void rollbackMigration(String schemaName, String changeSetTag) throws RemoteException;

    /**
     * Releases all configdb migration locks. Use this in case no lock can be acquired by liquibase.
     * 
     * @param schemaName The name of the schema for which to release the locks
     * @throws RemoteException if an error is occurred
     */
    void releaseLocks(String schemaName) throws RemoteException;

    /**
     * Gets a human-readable migration status string for the configdb.
     *
     * @param schemaName The name of the schema for which to get the migration status
     * @return The status
     * @throws RemoteException if an error is occurred
     */
    String getMigrationStatus(String schemaName) throws RemoteException;

    /**
     * Gets a human-readable lock status string for the configdb.
     *
     * @param schemaName The name of the schema for which to get the lock status
     * @return The status
     * @throws RemoteException if an error is occurred
     */
    String getLockStatus(String schemaName) throws RemoteException;
}
