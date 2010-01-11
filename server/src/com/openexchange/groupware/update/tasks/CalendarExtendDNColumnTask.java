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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.openexchange.database.DBPoolingException;
import com.openexchange.databaseold.Database;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.EnumComponent;
import com.openexchange.groupware.OXExceptionSource;
import com.openexchange.groupware.OXThrowsMultiple;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.update.Schema;
import com.openexchange.groupware.update.UpdateException;
import com.openexchange.groupware.update.UpdateTask;
import com.openexchange.groupware.update.exception.Classes;
import com.openexchange.groupware.update.exception.UpdateExceptionFactory;

/**
 * {@link CalendarExtendDNColumnTask} - Extends size of <tt>VARCHAR</tt> column <i>dn</i> in both working and backup table of
 * <i>prg_date_rights</i>.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@OXExceptionSource(classId = Classes.UPDATE_TASK, component = EnumComponent.UPDATE)
public class CalendarExtendDNColumnTask implements UpdateTask {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(CalendarExtendDNColumnTask.class);

    private static final UpdateExceptionFactory EXCEPTION = new UpdateExceptionFactory(CalendarExtendDNColumnTask.class);

    /**
     * Desired size for display name taken from ContactsFieldSizeUpdateTask.
     */
    private static final int DESIRED_SIZE = 320;

    public int addedWithVersion() {
        return 33;
    }

    public int getPriority() {
        /*
         * Modification on database: highest priority.
         */
        return UpdateTask.UpdateTaskPriority.HIGHEST.priority;
    }

    public void perform(final Schema schema, final int contextId) throws AbstractOXException {
        if (LOG.isInfoEnabled()) {
            LOG.info("Starting " + CalendarExtendDNColumnTask.class.getSimpleName());
        }
        modifyColumnInTable("prg_date_rights", contextId);
        modifyColumnInTable("del_date_rights", contextId);
        if (LOG.isInfoEnabled()) {
            LOG.info(CalendarExtendDNColumnTask.class.getSimpleName() + " finished.");
        }
    }

    private static final String SQL_MODIFY = "ALTER TABLE #TABLE# MODIFY dn varchar(" + DESIRED_SIZE + ") collate utf8_unicode_ci default NULL";

    private void modifyColumnInTable(final String tableName, final int contextId) throws UpdateException {
        if (checkColumnInTable(tableName, contextId)) {
            if (LOG.isInfoEnabled()) {
                LOG.info(CalendarExtendDNColumnTask.class.getSimpleName() + ": Going to extend size of column `dn` in table `" + tableName + "`.");
            }
            /*
             * Column does not exist yet
             */
            final Connection writeCon;
            try {
                writeCon = Database.getNoTimeout(contextId, true);
            } catch (final DBPoolingException e) {
                throw new UpdateException(e);
            }
            PreparedStatement stmt = null;
            try {
                try {
                    stmt = writeCon.prepareStatement(SQL_MODIFY.replaceFirst("#TABLE#", tableName));
                    stmt.executeUpdate();
                } catch (final SQLException e) {
                    throw wrapSQLException(e);
                }
            } finally {
                closeSQLStuff(null, stmt);
                if (writeCon != null) {
                    Database.backNoTimeout(contextId, true, writeCon);
                }
            }
            if (LOG.isInfoEnabled()) {
                LOG.info(CalendarExtendDNColumnTask.class.getSimpleName() + ": Size of column `dn` in table `" + tableName + "` successfully extended.");
            }
        }
    }

    private boolean checkColumnInTable(final String tableName, final int contextId) throws UpdateException {
        final Connection writeCon;
        try {
            writeCon = Database.get(contextId, true);
        } catch (final DBPoolingException e) {
            throw new UpdateException(e);
        }
        ResultSet rs = null;
        try {
            final DatabaseMetaData metadata = writeCon.getMetaData();
            rs = metadata.getColumns(null, null, tableName, null);
            while (rs.next()) {
                final String name = rs.getString("COLUMN_NAME");
                if ("dn".equals(name)) {
                    final int size = rs.getInt("COLUMN_SIZE");
                    return (size < DESIRED_SIZE);
                }
            }
            throw notFound(tableName);
        } catch (final SQLException e) {
            throw wrapSQLException(e);
        } finally {
            closeSQLStuff(rs);
            Database.back(contextId, true, writeCon);
        }
    }

    @OXThrowsMultiple(category = { Category.CODE_ERROR }, desc = { "" }, exceptionId = { 1 }, msg = { "SQL error occurred while performing task CalendarExtendDNColumnTask: %1$s." })
    private static UpdateException wrapSQLException(final SQLException e) {
        return EXCEPTION.create(1, e, e.getMessage());
    }

    @OXThrowsMultiple(category = { Category.CODE_ERROR }, desc = { "" }, exceptionId = { 2 }, msg = { "Column \"dn\" not found in table: %1$s." })
    private static UpdateException notFound(final String tableName) {
        return EXCEPTION.create(2, tableName);
    }
}
