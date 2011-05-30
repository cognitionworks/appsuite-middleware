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

package com.openexchange.groupware.settings.impl;

import static com.openexchange.tools.sql.DBUtils.autocommit;
import static com.openexchange.tools.sql.DBUtils.closeSQLStuff;
import static com.openexchange.tools.sql.DBUtils.rollback;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.database.DBPoolingException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.settings.IValueHandler;
import com.openexchange.groupware.settings.Setting;
import com.openexchange.groupware.settings.SettingException;
import com.openexchange.groupware.settings.SettingException.Code;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.server.impl.DBPool;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;

/**
 * This class implements the storage for settings using a relational database.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class RdbSettingStorage extends SettingStorage {

    private static final Log LOG = LogFactory.getLog(RdbSettingStorage.class);

    /**
     * SQL statement for loading one specific user setting.
     */
    private static final String SELECT_VALUE = "SELECT value FROM user_setting "
        + "WHERE cid=? AND user_id=? AND path_id=?";

    /**
     * SQL statement for inserting one specific user setting.
     */
    private static final String INSERT_SETTING =
        "INSERT INTO user_setting (value,cid,user_id,path_id) VALUES (?,?,?,?)";

    /**
     * SQL statement for updating one specific user setting.
     */
    private static final String UPDATE_SETTING = "UPDATE user_setting "
        + "SET value=? WHERE cid=? AND user_id=? AND path_id=?";

    /**
     * SQL statement for checking if a setting for a user exists.
     */
    private static final String SETTING_EXISTS = "SELECT COUNT(value) "
        + "FROM user_setting WHERE cid=? AND user_id=? AND path_id=?";

    /**
     * Reference to the context.
     */
    private final Session session;

    private final int ctxId;

    private final int userId;

    /**
     * Context.
     */
    private final Context ctx;

    private final User user;

    private final UserConfiguration userConfig;

    /**
     * Default constructor.
     * @param session Session.
     * @throws SettingException if the initialization of the setting tree fails.
     */
    RdbSettingStorage(final Session session) throws SettingException {
        this(session, session.getContextId(), session.getUserId());
    }

    RdbSettingStorage(final Session session, final int ctxId,
        final int userId) throws SettingException {
        super();
        this.session = session;
        this.ctxId = ctxId;
        this.userId = userId;
        if (session instanceof ServerSession) {
            final ServerSession serverSession = (ServerSession) session;
            ctx = serverSession.getContext();
            user = serverSession.getUser();
            userConfig = serverSession.getUserConfiguration();
        } else {
            ctx = Tools.getContext(ctxId);
            user = Tools.getUser(ctx, userId);
            userConfig = Tools.getUserConfiguration(ctx, userId);
        }
    }

    RdbSettingStorage(final Session session, final Context ctx, final User user,
        final UserConfiguration userConfig) {
        super();
        this.session = session;
        this.ctx = ctx;
        this.ctxId = ctx.getContextId();
        this.user = user;
        this.userId = user.getId();
        this.userConfig = userConfig;
    }

    /**
     * Special constructor for admin daemon.
     * @param ctxId
     * @param userId
     */
    RdbSettingStorage(final int ctxId, final int userId) {
        super();
        this.session = null;
        this.ctxId = ctxId;
        this.userId = userId;
        this.ctx = null;
        this.user = null;
        this.userConfig = null;
    }

    @Override
    public void save(final Setting setting) throws SettingException {
        save(null, setting);
    }

    @Override
    public void save(final Connection con, final Setting setting) throws
        SettingException {
        if (!setting.isLeaf()) {
            throw new SettingException(Code.NOT_LEAF, setting.getName());
        }
        if (setting.isShared()) {
            final IValueHandler value = ConfigTree.getSharedValue(setting);
            if (null != value && value.isWritable()) {
                value.writeValue(session, ctx, user, setting);
            } else {
                final SettingException e = new SettingException(Code.NO_WRITE,
                    setting.getName());
                LOG.debug(e.getMessage(), e);
            }
        } else {
            saveInternal(con, setting);
        }
    }

    /**
     * Internally saves a setting into the database.
     * @param con a writable database connection or <code>null</code>.
     * @param setting setting to store.
     * @throws SettingException if storing fails.
     */
    private void saveInternal(final Connection con, final Setting setting)
        throws SettingException {
        if (null == con) {
            final Connection myCon;
            try {
                myCon = DBPool.pickupWriteable(ctx);
            } catch (final DBPoolingException e) {
                throw new SettingException(Code.NO_CONNECTION, e);
            }
            try {
                myCon.setAutoCommit(false);
                saveInternal2(myCon, setting);
                myCon.commit();
            } catch (final SQLException e) {
                rollback(myCon);
                throw new SettingException(Code.SQL_ERROR, e);
            } finally {
                autocommit(myCon);
                DBPool.closeWriterSilent(ctx, myCon);
            }
        } else {
            saveInternal2(con, setting);
        }
    }

    /**
     * Internally saves a setting into the database.
     * @param con a writable database connection.
     * @param setting setting to store.
     * @throws SettingException if storing fails.
     */
    private void saveInternal2(final Connection con, final Setting setting)
        throws SettingException {
        final boolean update = settingExists(con, userId, setting);
        PreparedStatement stmt = null;
        try {
            if (update) {
                stmt = con.prepareStatement(UPDATE_SETTING);
            } else {
                stmt = con.prepareStatement(INSERT_SETTING);
            }
            int pos = 1;
            stmt.setString(pos++, setting.getSingleValue().toString());
            stmt.setInt(pos++, ctxId);
            stmt.setInt(pos++, userId);
            stmt.setInt(pos++, setting.getId());
            stmt.executeUpdate();
        } catch (final SQLException e) {
            throw new SettingException(Code.SQL_ERROR, e);
        } finally {
            closeSQLStuff(null, stmt);
        }
    }

    @Override
    public void readValues(final Setting setting) throws SettingException {
        final Connection con;
        if (!setting.isLeaf()) {
            readSubValues(setting);
            return;
        }
        if (setting.isShared()) {
            readSharedValue(setting);
        } else {
            try {
                con = DBPool.pickup(ctx);
            } catch (final DBPoolingException e) {
                throw new SettingException(Code.NO_CONNECTION, e);
            }
            try {
                readValues(con, setting);
            } finally {
                DBPool.closeReaderSilent(ctx, con);
            }
        }
    }

    @Override
    public void readValues(final Connection con, final Setting setting) throws SettingException {
        if (!setting.isLeaf()) {
            readSubValues(setting);
            return;
        }
        if (setting.isShared()) {
            readSharedValue(setting);
        } else {
            PreparedStatement stmt = null;
            ResultSet result = null;
            try {
                stmt = con.prepareStatement(SELECT_VALUE);
                int pos = 1;
                stmt.setInt(pos++, ctxId);
                stmt.setInt(pos++, userId);
                stmt.setInt(pos++, setting.getId());
                result = stmt.executeQuery();
                if (result.next()) {
                    setting.setSingleValue(result.getString(1));
                } else {
                    setting.setSingleValue(null);
                }
            } catch (final SQLException e) {
                throw new SettingException(Code.SQL_ERROR, e);
            } finally {
                closeSQLStuff(result, stmt);
            }
        }
    }

    /**
     * Reads a shared value.
     * @param setting setting Setting.
     */
    private void readSharedValue(final Setting setting) {
        final IValueHandler reader = ConfigTree.getSharedValue(setting);
        if (null != reader) {
            if (reader.isAvailable(userConfig)) {
                try {
                    reader.getValue(session, ctx, user, userConfig, setting);
                } catch (final SettingException e) {
                    LOG.error("Problem while reading setting value.", e);
                }
            } else {
                setting.getParent().removeElement(setting);
            }
        }
    }

    /**
     * Checks if a setting is already stored in the database.
     * @param con readonly database connection.
     * @param userId unique identifier of the user.
     * @param setting Setting.
     * @return <code>true</code> if a value for the setting exists in the
     * database.
     * @throws SettingException if an error occurs.
     */
    private boolean settingExists(final Connection con, final int userId,
        final Setting setting)
        throws SettingException {
        boolean exists = false;
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = con.prepareStatement(SETTING_EXISTS);
            int pos = 1;
            stmt.setInt(pos++, ctxId);
            stmt.setInt(pos++, userId);
            stmt.setInt(pos++, setting.getId());
            result = stmt.executeQuery();
            if (result.next()) {
                exists = result.getInt(1) == 1;
            }
        } catch (final SQLException e) {
            throw new SettingException(Code.SQL_ERROR, e);
        } finally {
            closeSQLStuff(result, stmt);
        }
        return exists;
    }

    /**
     * Reads all sub values of a setting.
     * @param setting setting to read.
     * @throws SettingException if an error occurs while reading the setting.
     */
    private void readSubValues(final Setting setting)
        throws SettingException {
        for (final Setting subSetting : setting.getElements()) {
            readValues(subSetting);
        }
        // During reading values all childs may be removed.
        if (setting.isLeaf()) {
            setting.getParent().removeElement(setting);
        }
    }
}
