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

package com.openexchange.admin.storage.mysqlStorage;

import static com.openexchange.tools.sql.DBUtils.autocommit;
import static com.openexchange.tools.sql.DBUtils.closeSQLStuff;
import static com.openexchange.tools.sql.DBUtils.rollback;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.admin.daemons.ClientAdminThread;
import com.openexchange.admin.exceptions.OXGenericException;
import com.openexchange.admin.rmi.dataobjects.Database;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.storage.sqlStorage.CreateTableRegistry;
import com.openexchange.admin.tools.AdminCache;
import com.openexchange.database.CreateTableService;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.update.SchemaException;
import com.openexchange.groupware.update.SchemaStore;
import com.openexchange.groupware.update.UpdateTask;
import com.openexchange.groupware.update.Updater;

public class OXUtilMySQLStorageCommon {

    private static final Log LOG = LogFactory.getLog(OXUtilMySQLStorageCommon.class);

    private static AdminCache cache = ClientAdminThread.cache;

    public void createDatabase(Database db) throws StorageException {
        final List<String> createTableStatements;
        try {
            createTableStatements = cache.getOXDBInitialQueries();
        } catch (OXGenericException e) {
            LOG.error("Error reading DB init Queries!", e);
            throw new StorageException(e);
        }
        final Connection con;
        String sql_pass = "";
        if (db.getPassword() != null) {
            sql_pass = db.getPassword();
        }
        try {
            con = cache.getSimpleSQLConnectionWithoutTimeout(db.getUrl(), db.getLogin(), sql_pass, db.getDriver());
        } catch (SQLException e) {
            LOG.error("SQL Error", e);
            throw new StorageException(e.toString(), e);
        } catch (ClassNotFoundException e) {
            LOG.error("Driver not found to create database ", e);
            throw new StorageException(e);
        }
        boolean created = false;
        try {
            con.setAutoCommit(false);
            if (existsDatabase(con, db.getScheme())) {
                throw new StorageException("Database \"" + db.getScheme() + "\" already exists");
            }
            createDatabase(con, db.getScheme());
            // Only delete the schema if it has been created successfully. Otherwise it may happen that we delete a longly existing schema.
            // See bug 18788.
            created = true;
            con.setCatalog(db.getScheme());
            pumpData2DatabaseOld(con, createTableStatements);
            pumpData2DatabaseNew(con, CreateTableRegistry.getInstance().getList());
            initUpdateTaskTable(con, db.getMasterId().intValue(), db.getScheme());
            con.commit();
        } catch (SQLException e) {
            rollback(con);
            if (created) {
                deleteDatabase(con, db);
            }
            throw new StorageException(e.toString());
        } catch (StorageException e) {
            rollback(con);
            if (created) {
                deleteDatabase(con, db);
            }
            throw e;
        } finally {
            autocommit(con);
            cache.closeSimpleConnection(con);
        }
    }

    private boolean existsDatabase(Connection con, String name) throws StorageException {
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = con.prepareStatement("SHOW DATABASES LIKE ?");
            stmt.setString(1, name);
            result = stmt.executeQuery();
            return result.next();
        } catch (SQLException e) {
            LOG.error("SQL Error", e);
            throw new StorageException(e.getMessage(), e);
        } finally {
            closeSQLStuff(result, stmt);
        }
    }

    private void createDatabase(Connection con, String name) throws StorageException {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.executeUpdate("CREATE DATABASE `" + name + "` DEFAULT CHARACTER SET utf8 COLLATE utf8_unicode_ci");
        } catch (SQLException e) {
            LOG.error("SQL Error", e);
            throw new StorageException(e.getMessage(), e);
        } finally {
            closeSQLStuff(stmt);
        }
    }

    private void pumpData2DatabaseOld(Connection con, List<String> db_queries) throws StorageException {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            for (String sqlCreate : db_queries) {
                stmt.addBatch(sqlCreate);
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            LOG.error("SQL Error", e);
            throw new StorageException(e.getMessage(), e);
        } finally {
            closeSQLStuff(stmt);
        }
    }

    private void pumpData2DatabaseNew(Connection con, List<CreateTableService> createTables) throws StorageException {
        Set<String> existingTables = new HashSet<String>();
        for (String table : FROM_SCRIPTS) {
            existingTables.add(table);
        }
        List<CreateTableService> toCreate = new ArrayList<CreateTableService>(createTables.size());
        toCreate.addAll(createTables);
        CreateTableService next;
        try {
            while ((next = findNext(toCreate, existingTables)) != null) {
                next.perform(con);
                for (String createdTable : next.tablesToCreate()) {
                    existingTables.add(createdTable);
                }
                toCreate.remove(next);
            }
        } catch (AbstractOXException e) {
            throw new StorageException(e.getMessage(), e);
        }
        if (!toCreate.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unable to determine next CreateTableService to execute.\n");
            sb.append("Existing tables: ");
            for (String existingTable : existingTables) {
                sb.append(existingTable);
                sb.append(',');
            }
            sb.setCharAt(sb.length() - 1, '\n');
            for (CreateTableService service : toCreate) {
                sb.append(service.getClass().getName());
                sb.append(": ");
                for (String tableToCreate : service.requiredTables()) {
                    sb.append(tableToCreate);
                    sb.append(',');
                }
                sb.setCharAt(sb.length() - 1, '\n');
            }
            sb.setLength(sb.length() - 1);
            throw new StorageException(sb.toString());
        }
    }

    private CreateTableService findNext(List<CreateTableService> toCreate, Set<String> existingTables) {
        for (CreateTableService service : toCreate) {
            List<String> requiredTables = new ArrayList<String>();
            for (String requiredTable : service.requiredTables()) {
                requiredTables.add(requiredTable);
            }
            if (existingTables.containsAll(requiredTables)) {
                return service;
            }
        }
        return null;
    }

    private void initUpdateTaskTable(Connection con, int poolId, String schema) throws StorageException {
        UpdateTask[] tasks = Updater.getInstance().getAvailableUpdateTasks();
        SchemaStore store = SchemaStore.getInstance();
        try {
            for (UpdateTask task : tasks) {
                store.addExecutedTask(con, task.getClass().getName(), true, poolId, schema);
            }
        } catch (SchemaException e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    public void deleteDatabase(Database db) throws StorageException {
        final Connection con;
        try {
            con = cache.getSimpleSQLConnectionWithoutTimeout(db.getUrl(), db.getLogin(), db.getPassword(), db.getDriver());
        } catch (SQLException e) {
            LOG.error("SQL Error", e);
            throw new StorageException(e.toString(), e);
        } catch (ClassNotFoundException e) {
            LOG.error("Driver not found to create database ", e);
            throw new StorageException(e);
        }
        try {
            deleteDatabase(con, db);
        } finally {
            cache.closeSimpleConnection(con);
        }
    }

    private void deleteDatabase(final Connection con, Database db) throws StorageException {
        Statement stmt = null;
        try {
            con.setAutoCommit(false);
            stmt = con.createStatement();
            stmt.executeUpdate("DROP DATABASE IF EXISTS `" + db.getScheme() + "`");
            con.commit();
        } catch (SQLException e) {
            rollback(con);
            throw new StorageException(e.getMessage(), e);
        } finally {
            closeSQLStuff(stmt);
        }
    }

    private static final String[] FROM_SCRIPTS = {
        "sequence_id", "sequence_principal", "sequence_resource", "sequence_resource_group", "sequence_folder", "sequence_calendar",
        "sequence_contact", "sequence_task", "sequence_project", "sequence_infostore", "sequence_forum", "sequence_pinboard",
        "sequence_attachment", "sequence_gui_setting", "sequence_reminder", "sequence_ical", "sequence_webdav", "sequence_uid_number",
        "sequence_gid_number", "sequence_mail_service", "groups", "del_groups", "user", "del_user", "groups_member", "login2user",
        "user_attribute", "resource", "del_resource", "oxfolder_tree", "oxfolder_permissions",
        "oxfolder_specialfolders", "oxfolder_userfolders", "oxfolder_userfolders_standardfolders", "del_oxfolder_tree",
        "del_oxfolder_permissions", "oxfolder_lock", "oxfolder_property", "user_configuration", "user_setting_mail",
        "user_setting_mail_signature", "user_setting_spellcheck", "user_setting_admin", "user_setting", "user_setting_server", "prg_dates",
        "prg_date_rights", "del_date_rights", "del_dates", "del_dates_members", "prg_dates_members", "prg_dlist", "del_dlist",
        "prg_contacts_linkage", "prg_contacts_image", "del_contacts_image", "del_contacts", "prg_contacts", "task", "task_folder",
        "task_participant", "task_eparticipant", "task_removedparticipant", "del_task", "del_task_folder", "del_task_participant",
        "del_task_eparticipant", "infostore", "infostore_document", "del_infostore", "del_infostore_document", "infostore_property",
        "infostore_lock", "lock_null", "lock_null_lock", "prg_attachment", "del_attachment", "prg_links", "reminder", "filestore_usage",
        "ical_principal", "ical_ids", "vcard_principal", "vcard_ids", "version" };
}