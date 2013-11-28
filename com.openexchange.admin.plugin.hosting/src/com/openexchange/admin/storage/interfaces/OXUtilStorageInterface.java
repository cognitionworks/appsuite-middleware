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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.admin.storage.interfaces;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


import com.openexchange.admin.daemons.ClientAdminThreadExtended;
import com.openexchange.admin.rmi.dataobjects.Filestore;
import com.openexchange.admin.rmi.dataobjects.MaintenanceReason;
import com.openexchange.admin.rmi.dataobjects.Database;

import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.rmi.dataobjects.Server;
import com.openexchange.admin.tools.AdminCacheExtended;
import com.openexchange.admin.tools.PropertyHandler;
import com.openexchange.admin.tools.PropertyHandlerExtended;

/**
 * This interface provides an abstraction to the storage of the util information
 *
 * @author d7
 * @author cutmasta
 *
 */
public abstract class OXUtilStorageInterface {

    /**
     * Proxy attribute for the class implementing this interface.
     */
    private static Class<? extends OXUtilStorageInterface> implementingClass;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OXUtilStorageInterface.class);

    protected static AdminCacheExtended cache = null;

    protected static PropertyHandler prop = null;

    static {
        cache = ClientAdminThreadExtended.cache;
        prop = cache.getProperties();
    }

    /**
     * Creates a new instance implementing the group storage interface.
     * @return an instance implementing the group storage interface.
     * @throws com.openexchange.admin.rmi.exceptions.StorageException Storage exception
     */
    public static OXUtilStorageInterface getInstance() throws StorageException {
        synchronized (OXUtilStorageInterface.class) {
            if (null == implementingClass) {
                final String className = prop.getProp(PropertyHandlerExtended.UTIL_STORAGE, null);
                if (null != className) {
                    try {
                        implementingClass = Class.forName(className).asSubclass(OXUtilStorageInterface.class);
                    } catch (final ClassNotFoundException e) {
                        log.error(e.getMessage(), e);
                        throw new StorageException(e);
                    }
                } else {
                    final StorageException storageException = new StorageException("Property for util_storage not defined");
                    log.error(storageException.getMessage(), storageException);
                    throw storageException;
                }
            }
        }
        Constructor<? extends OXUtilStorageInterface> cons;
        try {
            cons = implementingClass.getConstructor(new Class[] {});
            return cons.newInstance(new Object[] {});
        } catch (final SecurityException e) {
            log.error(e.getMessage(), e);
            throw new StorageException(e);
        } catch (final NoSuchMethodException e) {
            log.error(e.getMessage(), e);
            throw new StorageException(e);
        } catch (final IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            throw new StorageException(e);
        } catch (final InstantiationException e) {
            log.error(e.getMessage(), e);
            throw new StorageException(e);
        } catch (final IllegalAccessException e) {
            log.error(e.getMessage(), e);
            throw new StorageException(e);
        } catch (final InvocationTargetException e) {
            log.error(e.getMessage(), e);
            throw new StorageException(e);
        }
    }

    /**
     * Register filestore in configbdb.
     *
     * @param fstore
     *            filestore object
     * @return the id of the created filestore as a long.
     * @throws StorageException
     */
    public abstract int registerFilestore(final Filestore fstore) throws StorageException;

    /**
     * Changes a given filestore
     *
     * @param fstore
     *            filestore object
     * @throws StorageException
     */
    public abstract void changeFilestore(final Filestore fstore) throws StorageException;

    /**
     * List all registered file stores.
     * @param pattern a pattern to search for
     * @return an array of file store objects
     * @throws StorageException
     */
    public abstract Filestore[] listFilestores(String pattern, boolean omitUsage) throws StorageException;

    /**
     * get filestore by ID
     * @param id
     * @return Filestore
     * @throws StorageException
     */
    public abstract Filestore getFilestore(final int id) throws StorageException;

    /**
     * Load a filestore. Specify whether the file store usage should be calculated by summing up all filestore usages.
     * @param filestoreId
     * @param loadUsage - Whether the usage must be determined. Note: This is very slow.
     * @return
     * @throws StorageException
     */
    public abstract Filestore getFilestore(int filestoreId, boolean loadUsage) throws StorageException;

    /**
     * Unregister filestore from configbdb
     *
     * @param store_id
     *            the id of the filestore
     * @throws StorageException
     */
    public abstract void unregisterFilestore(final int store_id) throws StorageException;

    /**
     * Iterates across all existing filestores and searches for one having enough space for a context.
     */
    public abstract Filestore findFilestoreForContext() throws StorageException;

    public abstract boolean hasSpaceForAnotherContext(Filestore filestore) throws StorageException;

    /**
     * Create a new maintenance reason in configdb.They are needed to disable a
     * context.
     *
     * @param reason
     *            the MaintenanceReason
     * @return the id as a long of the new created reason
     * @throws StorageException
     */
    public abstract int createMaintenanceReason(final MaintenanceReason reason) throws StorageException;

    /**
     * Delete reason from configdb
     *
     * @param reason
     *            the MaintenanceReason
     * @throws StorageException
     */
    public abstract void deleteMaintenanceReason(final int[] reason_ids) throws StorageException;

    /**
     * @param reason_id
     *            the id of a MaintenanceReason
     * @return MaintenanceReason from configdb identified by the reason_id
     * @throws StorageException
     */
    public abstract MaintenanceReason[] getMaintenanceReasons(final int[] reason_id) throws StorageException;

    /**
     * @return an array of all available MaintenanceReasons in configdb.
     * @throws StorageException
     */
    public abstract MaintenanceReason[] getAllMaintenanceReasons() throws StorageException;

    /**
     * @return an array of all available MaintenanceReasons in configdb match the specified pattern
     * @throws StorageException
     */
    public abstract MaintenanceReason[] listMaintenanceReasons(final String search_pattern) throws StorageException;

    /**
     * Register a new Database in configdb
     *
     * @param db
     *            a database object to register
     * @return long with the id of the database
     * @throws StorageException
     */
    public abstract int registerDatabase(final Database db) throws StorageException;

    /**
     * Creates a new database from scratch on the given database host. Is used
     * ONLY internally at the moment.
     *
     * @param db
     *            a database object to create
     * @throws StorageException
     */
    public abstract void createDatabase(final Database db) throws StorageException;

    /**
     * Delete a complete database(scheme) from the given database host. Is used
     * ONYL internally at the moment.
     *
     * @param db
     *            a database object to be deleted
     * @throws StorageException
     */
    public abstract void deleteDatabase(final Database db) throws StorageException;

    // TODO: cutamasta: please fill javadoc comment
    /**
     * @param db
     *            a database object to be changed
     * @throws StorageException
     */
    public abstract void changeDatabase(final Database db) throws StorageException;

    /**
     * Registers a new server in the configdb
     *
     * @param serverName
     *            a server name to be registered
     * @return long with the id of the server
     * @throws StorageException
     */
    public abstract int registerServer(final String serverName) throws StorageException;

    /**
     * Unregister a database from configdb
     *
     * @param db_id
     *            a database id which is unregistered
     * @throws StorageException
     */
    public abstract void unregisterDatabase(final int db_id, final boolean isMaster) throws StorageException;

    /**
     * Unregister a server from configdb
     *
     * @param server_id
     *            a server id which is unregistered
     * @throws StorageException
     */
    public abstract void unregisterServer(final int server_id) throws StorageException;

    /**
     * Searches for databases matching search_pattern
     *
     * @param search_pattern
     *            a pattern to search for
     * @return a database array
     * @throws StorageException
     */
    public abstract Database[] searchForDatabase(final String search_pattern) throws StorageException;

    /**
     * Searchs for server matching given search_pattern
     *
     * @param search_pattern
     *            a pattern to search for
     * @return Server array with found servers
     * @throws StorageException
     */
    public abstract Server[] searchForServer(final String search_pattern) throws StorageException;


}
