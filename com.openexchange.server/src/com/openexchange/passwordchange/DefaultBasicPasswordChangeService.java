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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.passwordchange;

import static com.openexchange.tools.sql.DBUtils.autocommit;
import static com.openexchange.tools.sql.DBUtils.closeSQLStuff;
import static com.openexchange.tools.sql.DBUtils.rollback;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import com.openexchange.databaseold.Database;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserExceptionCode;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.user.UserService;


/**
 * {@link DefaultBasicPasswordChangeService}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.0
 */
public class DefaultBasicPasswordChangeService extends BasicPasswordChangeService {

    /**
     * Initializes a new {@link DefaultBasicPasswordChangeService}.
     */
    public DefaultBasicPasswordChangeService() {
        super();
    }

    @Override
    protected void update(PasswordChangeEvent event) throws OXException {
        String encodedPassword;
        Context ctx = event.getContext();
        {
            UserService userService = ServerServiceRegistry.getInstance().getService(UserService.class);
            if (userService == null) {
                throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create( UserService.class.getName());
            }
            User user = userService.getUser(event.getSession().getUserId(), ctx);

            // Get encoded version of new password
            encodedPassword = getEncodedPassword(user.getPasswordMech(), event.getNewPassword());
        }

        // Update database
        Connection writeCon = Database.get(ctx, true);
        boolean rollback = false;
        try {
            writeCon.setAutoCommit(false);
            rollback = true;
            update(writeCon, encodedPassword, event.getSession().getUserId(), ctx.getContextId());
            deleteAttr(writeCon, event.getSession().getUserId(), ctx.getContextId());
            writeCon.commit();
            rollback = false;
        } catch (final SQLException e) {
            throw UserExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            throw UserExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            if (rollback) {
                rollback(writeCon);
            }
            autocommit(writeCon);
            Database.back(ctx, true, writeCon);
        }
    }

    private void update(Connection writeCon, String encodedPassword, int userId, int contextId) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = writeCon.prepareStatement("UPDATE user SET userPassword = ?, shadowLastChange = ? WHERE cid = ? AND id = ?");
            int pos = 1;
            stmt.setString(pos++, encodedPassword);
            stmt.setInt(pos++,(int)(System.currentTimeMillis()/1000));
            stmt.setInt(pos++, contextId);
            stmt.setInt(pos++, userId);
            stmt.executeUpdate();
        } finally {
            closeSQLStuff(stmt);
        }
    }

    private void deleteAttr(Connection writeCon, int userId, int contextId) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = writeCon.prepareStatement("DELETE FROM user_attribute WHERE cid = ? AND id = ? AND name = ?");
            int pos = 1;
            stmt.setInt(pos++, contextId);
            stmt.setInt(pos++, userId);
            stmt.setString(pos++, "passcrypt");
            stmt.executeUpdate();
        } finally {
            closeSQLStuff(stmt);
        }
    }

}
