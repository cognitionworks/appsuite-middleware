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

package com.openexchange.groupware.update.tasks;

import static com.openexchange.tools.sql.DBUtils.closeSQLStuff;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openexchange.database.AbstractCreateTableImpl;
import com.openexchange.database.DBPoolingException;
import com.openexchange.databaseold.Database;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.update.DefaultAttributes;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.Schema;
import com.openexchange.groupware.update.TaskAttributes;
import com.openexchange.groupware.update.UpdateException;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTask;
import com.openexchange.groupware.update.UpdateTaskAdapter;
import com.openexchange.groupware.update.UpdateTaskV2;
import com.openexchange.tools.update.Tools;

/**
 * {@link HeaderCacheCreateTableTask} - Inserts necessary tables to support MAL Poll bundle features.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class HeaderCacheCreateTableTask extends AbstractCreateTableImpl implements UpdateTaskV2 {

    private static final Log LOG = LogFactory.getLog(HeaderCacheCreateTableTask.class);

    public int addedWithVersion() {
        return Schema.NO_VERSION;
    }

    public int getPriority() {
        return UpdateTask.UpdateTaskPriority.NORMAL.priority;
    }

    private static String getCreateMailUUIDTable() {
        return "CREATE TABLE mailUUID (" + 
		" cid INT4 unsigned NOT NULL," + 
		" user INT4 unsigned NOT NULL," + 
		" account INT4 unsigned NOT NULL," + 
		" fullname VARCHAR(128) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL," + 
		" id VARCHAR(70) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL," + 
		" uuid BINARY(16) NOT NULL," + 
		" PRIMARY KEY (cid, user, account, fullname, id)," + 
		" INDEX (cid, user, uuid)," + 
		" FOREIGN KEY (cid, user) REFERENCES user (cid, id)" + 
		") ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;";
    }

    private static final String getCreateHeaderBlobTable() {
        return "CREATE TABLE headersAsBlob (" + 
		" cid INT4 unsigned NOT NULL," + 
		" user INT4 unsigned NOT NULL," + 
		" uuid BINARY(16) NOT NULL," + 
		" flags INT4 unsigned NOT NULL default '0'," + 
		" receivedDate bigint(64) default NULL," + 
		" rfc822Size bigint(64) UNSIGNED NOT NULL," + 
		" userFlags VARCHAR(1024) collate utf8_unicode_ci default NULL," + 
		" headers BLOB," + 
		" PRIMARY KEY (cid, user, uuid)," + 
		" FOREIGN KEY (cid, user, uuid) REFERENCES mailUUID (cid, user, uuid)" + 
		") ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;";
    }

    @Override
    public String[] getCreateStatements() {
        return new String[] { getCreateMailUUIDTable(), getCreateHeaderBlobTable() };
    }

    public String[] requiredTables() {
        return new String[] { "user" };
    }

    public String[] tablesToCreate() {
        return new String[] { "mailUUID", "headersAsBlob" };
    }

    public String[] getDependencies() {
        return new String[0];
    }

    public TaskAttributes getAttributes() {
        return new DefaultAttributes();
    }

    public void perform(final Schema schema, final int contextId) throws AbstractOXException {
        UpdateTaskAdapter.perform(this, schema, contextId);
    }

    public void perform(final PerformParameters params) throws AbstractOXException {
        final int contextId = params.getContextId();
        createTable("mailUUID", getCreateMailUUIDTable(), contextId);
        createTable("headersAsBlob", getCreateHeaderBlobTable(), contextId);
        if (LOG.isInfoEnabled()) {
            LOG.info("UpdateTask 'HeaderCacheCreateTableTask' successfully performed!");
        }
    }

    private void createTable(final String tablename, final String sqlCreate, final int contextId) throws UpdateException {
        final Connection writeCon;
        try {
            writeCon = Database.get(contextId, true);
        } catch (final DBPoolingException e) {
            throw new UpdateException(e);
        }
        PreparedStatement stmt = null;
        try {
            if (Tools.tableExists(writeCon, tablename)) {
                return;
            }
            stmt = writeCon.prepareStatement(sqlCreate);
            stmt.executeUpdate();
        } catch (final SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } finally {
            closeSQLStuff(stmt);
            Database.back(contextId, true, writeCon);
        }
    }
}
