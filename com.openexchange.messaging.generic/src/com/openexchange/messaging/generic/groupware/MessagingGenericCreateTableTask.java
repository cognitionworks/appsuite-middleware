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

package com.openexchange.messaging.generic.groupware;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.openexchange.database.AbstractCreateTableImpl;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.Attributes;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.TaskAttributes;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTaskV2;
import com.openexchange.messaging.generic.services.MessagingGenericServiceRegistry;

/**
 * {@link MessagingGenericCreateTableTask}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MessagingGenericCreateTableTask extends AbstractCreateTableImpl implements UpdateTaskV2 {

    /**
     * Initializes a new {@link MessagingGenericCreateTableTask}.
     */
    public MessagingGenericCreateTableTask() {
        super();
    }

    private String getMessagingAccountTable() {
        return "CREATE TABLE messagingAccount (" +
        " cid INT4 unsigned NOT NULL," +
        " user INT4 unsigned NOT NULL," +
        " serviceId VARCHAR(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL," +
        " account INT4 unsigned NOT NULL," +
        " confId INT4 unsigned NOT NULL," +
        " displayName VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL," +
        " PRIMARY KEY (cid, user, serviceId, account)" +
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    }

    @Override
    protected String[] getCreateStatements() {
        return new String[] { getMessagingAccountTable() };
    }

    @Override
    public TaskAttributes getAttributes() {
        return new Attributes();
    }

    @Override
    public String[] getDependencies() {
        return new String[0];
    }

    @Override
    public void perform(final PerformParameters params) throws com.openexchange.exception.OXException {
        createTable("messagingAccount", getMessagingAccountTable(), params.getConnectionProvider().getConnection());
        final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MessagingGenericCreateTableTask.class);
        logger.info("UpdateTask ''{}'' successfully performed!", MessagingGenericCreateTableTask.class.getSimpleName());
    }

    @Override
    public String[] requiredTables() {
        return new String[] { "user" };
    }

    @Override
    public String[] tablesToCreate() {
        return new String[] { "messagingAccount" };
    }

    private void createTable(String tablename, String sqlCreate, Connection connection) throws OXException {
        PreparedStatement stmt = null;
        try {
            if (tableExists(connection, tablename)) {
                return;
            }

            stmt = connection.prepareStatement(sqlCreate);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    private boolean tableExists(final Connection con, final String table) throws SQLException {
        final DatabaseMetaData metaData = con.getMetaData();
        ResultSet rs = null;
        try {
            rs = metaData.getTables(null, null, table, new String[] { "TABLE" });
            return (rs.next() && rs.getString("TABLE_NAME").equals(table));
        } finally {
            Databases.closeSQLStuff(rs);
        }
    }
}
