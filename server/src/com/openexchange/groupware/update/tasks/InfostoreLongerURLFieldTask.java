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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.databaseold.Database;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.EnumComponent;
import com.openexchange.groupware.OXExceptionSource;
import com.openexchange.groupware.OXThrows;
import com.openexchange.groupware.update.Schema;
import com.openexchange.groupware.update.UpdateTask;
import com.openexchange.groupware.update.exception.Classes;
import com.openexchange.groupware.update.exception.UpdateExceptionFactory;

@OXExceptionSource(
        classId = Classes.UPDATE_TASK,
        component = EnumComponent.UPDATE
    )
public class InfostoreLongerURLFieldTask  implements UpdateTask {

    private final Log LOG = LogFactory.getLog(InfostoreLongerURLFieldTask.class);
    private static final UpdateExceptionFactory EXCEPTIONS = new UpdateExceptionFactory(InfostoreLongerURLFieldTask.class);

    public int addedWithVersion() {
        return 12;
    }

    public int getPriority() {
        return UpdateTask.UpdateTaskPriority.NORMAL.priority;
    }
    @OXThrows(
            category = AbstractOXException.Category.CODE_ERROR,
            desc = "",
            msg = "Error in SQL Statement",
            exceptionId = 1
    )
    public void perform(final Schema schema, final int contextId) throws AbstractOXException {
        Connection writeCon = null;
        PreparedStatement stmt = null;
        final PreparedStatement checkAvailable = null;
        final ResultSet rs = null;

        try {
            writeCon = Database.get(contextId, true);
            writeCon.setAutoCommit(false);
            stmt = writeCon.prepareStatement("ALTER TABLE infostore_document MODIFY url varchar(256)");
            stmt.executeUpdate();
            stmt.close();
            stmt = writeCon.prepareStatement("ALTER TABLE del_infostore_document MODIFY url varchar(256)");
            stmt.executeUpdate();
            writeCon.commit();
        } catch (final SQLException x) {
            try {
                writeCon.rollback();
            } catch (final SQLException x2) {
                LOG.error("Can't execute rollback.", x2);
            }
            EXCEPTIONS.create(1, x);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (final SQLException x) {
                    LOG.warn("Couldn't close statement", x);
                }
            }

            if (null != rs) {
                try {
                    rs.close();
                } catch (final SQLException x) {
                    LOG.warn("Couldn't close result set", x);
                }
            }

            if (writeCon != null) {
                try {
                    writeCon.setAutoCommit(true);
                } catch (final SQLException x) {
                    LOG.warn("Can't reset auto commit", x);
                }

                if (writeCon != null) {
                    Database.back(contextId, true, writeCon);
                }
            }
        }
    }
}
