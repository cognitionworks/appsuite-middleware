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

package com.openexchange.consistency.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * {@link ConsistencyRMIService}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.1
 */
public interface ConsistencyRMIService extends Remote {

    public static final String RMI_NAME = ConsistencyRMIService.class.getSimpleName();

    /**
     * Checks and/or repairs the configuration database
     * 
     * @param repair flag to trigger the repair
     * @return A {@link List} with repair information
     * @throws RemoteException if an error is occurred
     */
    List<String> checkOrRepairConfigDB(boolean repair) throws RemoteException;

    /**
     * Lists all missing files in the specified context
     * 
     * @param contextId the context identifier
     * @return A {@link List} with all missing files
     * @throws RemoteException if an error is occurred
     */
    List<String> listMissingFilesInContext(int contextId) throws RemoteException;

    /**
     * Lists all missing files in the specified filestore
     * 
     * @param filestoreId the filestore identifier
     * @return A {@link List} with all missing files
     * @throws RemoteException if an error is occurred
     */
    Map<ConsistencyEntity, List<String>> listMissingFilesInFilestore(int filestoreId) throws RemoteException;

    /**
     * Lists all missing files in the specified database
     * 
     * @param databaseId the database identifier
     * @return A {@link List} with all missing files
     * @throws RemoteException if an error is occurred
     */
    Map<ConsistencyEntity, List<String>> listMissingFilesInDatabase(int databaseId) throws RemoteException;

    /**
     * Lists all missing files
     * 
     * @return A {@link List} with all missing files
     * @throws RemoteException if an error is occurred
     */
    Map<ConsistencyEntity, List<String>> listAllMissingFiles() throws RemoteException;

    /**
     * Lists all unassigned files in the specified context
     * 
     * @param contextId The context identifier
     * @return A {@link List} with all unassigned files
     * @throws RemoteException if an error is occurred
     */
    List<String> listUnassignedFilesInContext(int contextId) throws RemoteException;

    /**
     * Lists all unassigned files in the specified filestore
     * 
     * @param filestoreId The filestore identifier
     * @return A {@link List} with all unassigned files
     * @throws RemoteException if an error is occurred
     */
    Map<ConsistencyEntity, List<String>> listUnassignedFilesInFilestore(int filestoreId) throws RemoteException;

    /**
     * Lists all unassigned files in the specified database
     * 
     * @param databaseId The database identifier
     * @return A {@link List} with all unassigned files
     * @throws RemoteException if an error is occurred
     */
    Map<ConsistencyEntity, List<String>> listUnassignedFilesInDatabase(int databaseId) throws RemoteException;

    /**
     * Lists all unassigned files
     * 
     * @return A {@link List} with all unassigned files
     * @throws RemoteException if an error is occurred
     */
    Map<ConsistencyEntity, List<String>> listAllUnassignedFiles() throws RemoteException;

    /**
     * Repairs all files in the specified context by using the specified resolver policy
     * 
     * @param contextId The context identifier
     * @param resolverPolicy The name of the resolver policy
     * @throws RemoteException if an error is occurred
     */
    void repairFilesInContext(int contextId, String repairPolicy, String repairAction) throws RemoteException;

    /**
     * Repairs all files in the specified filestore by using the specified resolver policy
     * 
     * @param filestoreId The filestore identifier
     * @param resolverPolicy The name of the resolver policy
     * @throws RemoteException if an error is occurred
     */
    void repairFilesInFilestore(int filestoreId, String repairPolicy, String repairAction) throws RemoteException;

    /**
     * Repairs all files in the specified database by using the specified resolver policy
     * 
     * @param databaseId The database identifier
     * @param resolverPolicy The name of the resolver policy
     * @throws RemoteException if an error is occurred
     */
    void repairFilesInDatabase(int databaseId, String repairPolicy, String repairAction) throws RemoteException;

    /**
     * Repairs all files by using the specified resolver policy
     * 
     * @param resolverPolicy The name of the resolver policy
     * @throws RemoteException if an error is occurred
     */
    void repairAllFiles(String repairPolicy, String repairAction) throws RemoteException;
}
