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

package com.openexchange.groupware.update.tasks;

import static com.openexchange.database.Databases.autocommit;
import static com.openexchange.database.Databases.rollback;
import static com.openexchange.database.Databases.startTransaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.Attributes;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.TaskAttributes;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTaskAdapter;
import com.openexchange.mail.usersetting.UserSettingMail;

/**
 * {@link CheckForPresetMessageFormatInJSLob} - Check for possibly preset message format preference in JSLob and aligns the DB value accordingly.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since 7.6.2
 */
public class CheckForPresetMessageFormatInJSLob extends UpdateTaskAdapter {

    public CheckForPresetMessageFormatInJSLob() {
        super();
    }

    @Override
    public TaskAttributes getAttributes() {
        return new Attributes();
    }

    @Override
    public String[] getDependencies() {
        return new String[] { "com.openexchange.jslob.storage.db.groupware.DBJSlobIncreaseBlobSizeTask" };
    }

    @Override
    public void perform(PerformParameters params) throws OXException {
        Connection con = params.getConnection();
        int rollback = 0;
        try {
            startTransaction(con);
            rollback = 1;

            List<Object[]> messageFormats = getMessageFormats(con);
            applytMessageFormats(messageFormats, con);

            con.commit();
            rollback = 2;
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw UpdateExceptionCodes.OTHER_PROBLEM.create(e, e.getMessage());
        } finally {
            if (rollback > 0) {
                if (rollback==1) {
                    rollback(con);
                }
                autocommit(con);
            }
        }
    }

    private void applytMessageFormats(List<Object[]> messageFormats, Connection con) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("UPDATE user_setting_mail SET msg_format=? WHERE cid=? AND user=? AND msg_format<>?");

            for (Object[] objs : messageFormats) {
                String format = (String) objs[2];
                int iFormat;
                if ("html".equalsIgnoreCase(format)) {
                    iFormat = UserSettingMail.MSG_FORMAT_HTML_ONLY;
                } else if ("text".equalsIgnoreCase(format)) {
                    iFormat = UserSettingMail.MSG_FORMAT_TEXT_ONLY;
                } else {
                    iFormat = UserSettingMail.MSG_FORMAT_BOTH;
                }
                stmt.setInt(1, iFormat);
                stmt.setInt(2, ((Integer) objs[0]).intValue());
                stmt.setInt(3, ((Integer) objs[1]).intValue());
                stmt.setInt(4, iFormat);
                stmt.addBatch();
            }

            stmt.executeBatch();
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    private List<Object[]> getMessageFormats(Connection con) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT cid, user, data FROM jsonStorage WHERE serviceId=? AND id=? AND data LIKE ?");
            stmt.setString(1, "com.openexchange.jslob.config");
            stmt.setString(2, "io.ox/mail");
            stmt.setString(3, "%\"messageFormat\":%");
            rs = stmt.executeQuery();
            if (false == rs.next()) {
                return Collections.emptyList();
            }
            List<Object[]> results = new LinkedList<Object[]>();
            do {
                try {
                    int contextId = rs.getInt(1);
                    int userId = rs.getInt(2);
                    String format = new JSONObject(rs.getString(3)).getString("messageFormat");
                    Object[] objs = new Object[3];
                    objs[0] = Integer.valueOf(contextId);
                    objs[1] = Integer.valueOf(userId);
                    objs[2] = format;
                    results.add(objs);
                } catch (@SuppressWarnings("unused") JSONException e) {
                    // Corrupt JSLob. Ignore
                }
            } while (rs.next());
            return results;
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

}
