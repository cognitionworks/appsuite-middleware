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

package com.openexchange.preferences;

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.b;
import static com.openexchange.tools.sql.DBUtils.closeSQLStuff;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import com.openexchange.database.DBPoolingException;
import com.openexchange.databaseold.Database;
import com.openexchange.groupware.settings.SettingException;

/**
 * Interface for accessing configuration settings.
 * 
 * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ServerUserSetting {

    private static final Attribute<Boolean> CONTACT_COLLECT_ENABLED = new Attribute<Boolean>() {

        public Boolean getAttribute(final ResultSet rs) throws SQLException {
            return Boolean.valueOf(rs.getBoolean(getColumnName()));
        }

        public String getColumnName() {
            return "contact_collect_enabled";
        }

        public void setAttribute(final PreparedStatement pstmt, final Boolean value) throws SQLException {
            if (null == value) {
                pstmt.setNull(1, Types.BOOLEAN);
            } else {
                pstmt.setBoolean(1, value.booleanValue());
            }
        }

    };

    private static final Attribute<Integer> CONTACT_COLLECT_FOLDER = new Attribute<Integer>() {

        public Integer getAttribute(final ResultSet rs) throws SQLException {
            int retval = rs.getInt(getColumnName());
            return rs.wasNull() ? null : I(retval);
        }

        public String getColumnName() {
            return "contact_collect_folder";
        }

        public void setAttribute(final PreparedStatement pstmt, final Integer value) throws SQLException {
            if (null == value) {
                pstmt.setNull(1, Types.INTEGER);
            } else {
                pstmt.setInt(1, value.intValue());
            }
        }

    };

    private static final Attribute<Boolean> CONTACT_COLLECT_ON_MAIL_ACCESS = new Attribute<Boolean>() {

        public Boolean getAttribute(final ResultSet rs) throws SQLException {
            return Boolean.valueOf(rs.getBoolean(getColumnName()));
        }

        public String getColumnName() {
            return "contactCollectOnMailAccess";
        }

        public void setAttribute(final PreparedStatement pstmt, final Boolean value) throws SQLException {
            if (value == null) {
                pstmt.setBoolean(1, true);
            } else {
                pstmt.setBoolean(1, value.booleanValue());
            }
        }

    };

    private static final Attribute<Boolean> CONTACT_COLLECT_ON_MAIL_TRANSPORT = new Attribute<Boolean>() {

        public Boolean getAttribute(final ResultSet rs) throws SQLException {
            return Boolean.valueOf(rs.getBoolean(getColumnName()));
        }

        public String getColumnName() {
            return "contactCollectOnMailTransport";
        }

        public void setAttribute(final PreparedStatement pstmt, final Boolean value) throws SQLException {
            if (value == null) {
                pstmt.setBoolean(1, true);
            } else {
                pstmt.setBoolean(1, value.booleanValue());
            }
        }

    };

    private static final Attribute<Integer> DEFAULT_STATUS_PRIVATE = new Attribute<Integer>() {

        public Integer getAttribute(final ResultSet rs) throws SQLException {
            return I(rs.getInt(getColumnName()));
        }

        public String getColumnName() {
            return "defaultStatusPrivate";
        }

        public void setAttribute(final PreparedStatement pstmt, final Integer value) throws SQLException {
            if (value == null) {
                pstmt.setInt(1, 0);
            } else {
                pstmt.setInt(1, value.intValue());
            }
        }

    };

    private static final Attribute<Integer> DEFAULT_STATUS_PUBLIC = new Attribute<Integer>() {

        public Integer getAttribute(final ResultSet rs) throws SQLException {
            return I(rs.getInt(getColumnName()));
        }

        public String getColumnName() {
            return "defaultStatusPublic";
        }

        public void setAttribute(final PreparedStatement pstmt, final Integer value) throws SQLException {
            if (value == null) {
                pstmt.setInt(1, 0);
            } else {
                pstmt.setInt(1, value.intValue());
            }
        }

    };

    private static final Attribute<Integer> FOLDER_TREE = new Attribute<Integer>() {

        public Integer getAttribute(ResultSet rs) throws SQLException {
            int tmp = rs.getInt(getColumnName());
            final Integer retval;
            if (rs.wasNull()) {
                retval = null;
            } else {
                retval = I(tmp);
            }
            return retval;
        }

        public String getColumnName() {
            return "folderTree";
        }

        public void setAttribute(PreparedStatement pstmt, Integer value) throws SQLException {
            if (value == null) {
                pstmt.setInt(1, 0);
            } else {
                pstmt.setInt(1, value.intValue());
            }
        }        
    };

    private static final ServerUserSetting defaultInstance = new ServerUserSetting();

    /**
     * Gets the default instance.
     * 
     * @return The default instance.
     */
    public static ServerUserSetting getInstance() {
        return defaultInstance;
    }

    /**
     * Gets the instance using specified connection.
     * 
     * @param connection The connection to use.
     * @return The instance using specified connection.
     */
    public static ServerUserSetting getInstance(final Connection connection) {
        return new ServerUserSetting(connection);
    }

    /*-
     * ################### Member fields & methods ###################
     */

    private final Connection connection;

    /**
     * Initializes a new {@link ServerUserSetting}.
     */
    private ServerUserSetting() {
        this(null);
    }

    /**
     * Initializes a new {@link ServerUserSetting}.
     * 
     * @param connection The connection to use.
     */
    private ServerUserSetting(final Connection connection) {
        super();
        this.connection = connection;
    }

    /**
     * Complete feature is enabled if one of its sub switches is enabled.
     * 
     * @param cid context id
     * @param user user id
     * @return The value or <code>false</code> if no entry is found.
     */
    public Boolean isContactCollectionEnabled(final int cid, final int user) throws SettingException {
        return B(b(isContactCollectOnMailAccess(cid, user)) || b(isContactCollectOnMailTransport(cid, user)));
    }

    /**
     * Sets the folder used to store collected contacts.
     * 
     * @param cid context id
     * @param user user id
     * @param folder folder id
     */
    public void setContactCollectionFolder(final int cid, final int user, final Integer folder) throws SettingException {
        setAttribute(cid, user, CONTACT_COLLECT_FOLDER, folder);
    }

    /**
     * Returns the folder used to store collected contacts.
     * 
     * @param cid The context id
     * @param user The user id
     * @return folder id or <code>null</code> if no entry found.
     */
    public Integer getContactCollectionFolder(final int cid, final int user) throws SettingException {
        return getAttribute(cid, user, CONTACT_COLLECT_FOLDER);
    }

    /**
     * Sets the flag for contact collection on incoming mails.
     * 
     * @param cid The context id
     * @param user The user id
     * @param value The flag to set
     * @throws SettingException If a setting error occurs
     */
    public void setContactCollectOnMailAccess(final int cid, final int user, final boolean value) throws SettingException {
        setAttribute(cid, user, CONTACT_COLLECT_ON_MAIL_ACCESS, Boolean.valueOf(value));
    }

    /**
     * Gets the flag for contact collection on incoming mails. If <code>null</code> default if <code>false</code>.
     * 
     * @param cid The context id
     * @param user The user id
     * @return The flag for contact collection on incoming mails or <code>false</code>
     * @throws SettingException If a setting error occurs
     */
    public Boolean isContactCollectOnMailAccess(final int cid, final int user) throws SettingException {
        final Boolean attribute = getAttribute(cid, user, CONTACT_COLLECT_ON_MAIL_ACCESS);
        return null == attribute ? Boolean.FALSE : attribute;
    }

    /**
     * Sets the flag for contact collection on outgoing mails.
     * 
     * @param cid The context id
     * @param user The user id
     * @param value The flag to set
     * @throws SettingException If a setting error occurs
     */
    public void setContactCollectOnMailTransport(final int cid, final int user, final boolean value) throws SettingException {
        setAttribute(cid, user, CONTACT_COLLECT_ON_MAIL_TRANSPORT, Boolean.valueOf(value));
    }

    /**
     * Gets the flag for contact collection on outgoing mails. If <code>null</code> default if <code>false</code>.
     * 
     * @param cid The context id
     * @param user The user id
     * @return The flag for contact collection on outgoing mails or <code>false</code>
     * @throws SettingException If a setting error occurs
     */
    public Boolean isContactCollectOnMailTransport(final int cid, final int user) throws SettingException {
        final Boolean attribute = getAttribute(cid, user, CONTACT_COLLECT_ON_MAIL_TRANSPORT);
        return null == attribute ? Boolean.FALSE : attribute;
    }

    /**
     * Returns the default confirmation status for private folders. If no value is set this parameter defaults to 0.
     * 
     * @param cid
     * @param user
     * @return
     * @throws SettingException If a setting error occurs
     */
    public Integer getDefaultStatusPrivate(final int cid, final int user) throws SettingException {
        Integer value = getAttribute(cid, user, DEFAULT_STATUS_PRIVATE);
        if (value == null) {
            value = I(0);
        }
        return value;
    }

    /**
     * Sets the default confirmation status for private folders. <code>null</code> will default to 0.
     * 
     * @param cid
     * @param user
     * @param status
     * @throws SettingException
     */
    public void setDefaultStatusPrivate(final int cid, final int user, final Integer status) throws SettingException {
        setAttribute(cid, user, DEFAULT_STATUS_PRIVATE, status);
    }

    /**
     * Returns the default confirmation status for public folders. If no value is set this parameter defaults to 0.
     * 
     * @param cid
     * @param user
     * @return
     * @throws SettingException
     */
    public Integer getDefaultStatusPublic(final int cid, final int user) throws SettingException {
        Integer value = getAttribute(cid, user, DEFAULT_STATUS_PUBLIC);
        if (value == null) {
            value = I(0);
        }
        return value;
    }

    /**
     * Sets the default confirmation status for public folders. <code>null</code> will default to 0.
     * 
     * @param cid
     * @param user
     * @param status
     * @throws SettingException
     */
    public void setDefaultStatusPublic(final int cid, final int user, final Integer status) throws SettingException {
        setAttribute(cid, user, DEFAULT_STATUS_PUBLIC, status);
    }

    /**
     * Get the selected folder tree for the user. Return value may be <code>null</code> if the user does not have selected a folder tree.
     * @param cid context identifier
     * @param user user identifier.
     * @return the selected folder tree or <code>null</code>
     * @throws SettingException if reading the value from the database fails.
     */
    public Integer getFolderTree(int cid, int user) throws SettingException {
        return getAttribute(cid, user, FOLDER_TREE);
    }

    public void setFolderTree(int cid, int user, Integer value) throws SettingException {
        setAttribute(cid, user, FOLDER_TREE, value);
    }

    private <T> T getAttribute(final int cid, final int user, final Attribute<T> attribute) throws SettingException {
        final Connection con;
        if (connection == null) {
            try {
                con = Database.get(cid, false);
            } catch (final DBPoolingException e) {
                throw new SettingException(e);
            }
        } else {
            con = connection;
        }
        try {
            return getAttribute(cid, user, attribute, con);
        } finally {
            if (null == connection) {
                Database.back(cid, false, con);
            }
        }
    }

    private <T> void setAttribute(final int cid, final int user, final Attribute<T> attribute, final T value) throws SettingException {
        final Connection con;
        if (connection == null) {
            try {
                con = Database.get(cid, true);
            } catch (final DBPoolingException e) {
                throw new SettingException(e);
            }
        } else {
            con = connection;
        }
        try {
            if (hasEntry(cid, user, con)) {
                updateAttribute(cid, user, attribute, value, con);
            } else {
                insertAttribute(cid, user, attribute, value, con);
            }
        } finally {
            if (null == connection) {
                Database.back(cid, true, con);
            }
        }
    }

    void deleteEntry(int cid, int user) throws SettingException {
        final Connection con;
        if (connection == null) {
            try {
                con = Database.get(cid, true);
            } catch (final DBPoolingException e) {
                throw new SettingException(e);
            }
        } else {
            con = connection;
        }
        try {
            deleteEntry(cid, user, con);
        } finally {
            if (null == connection) {
                Database.back(cid, true, con);
            }
        }
    }

    private <T> T getAttribute(final int cid, final int user, final Attribute<T> attribute, final Connection con) throws SettingException {
        T retval = null;
        final String select = "SELECT " + attribute.getColumnName() + " FROM user_setting_server WHERE cid=? AND user=?";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement(select);
            stmt.setInt(1, cid);
            stmt.setInt(2, user);
            rs = stmt.executeQuery();
            if (rs.next()) {
                retval = attribute.getAttribute(rs);
            }
        } catch (final SQLException e) {
            throw new SettingException(SettingException.Code.SQL_ERROR, e);
        } finally {
            closeSQLStuff(rs, stmt);
        }
        return retval;
    }

    private <T> void updateAttribute(final int cid, final int user, final Attribute<T> attribute, final T value, final Connection con) throws SettingException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("UPDATE user_setting_server SET " + attribute.getColumnName() + "=? WHERE cid=? AND user=?");
            attribute.setAttribute(stmt, value);
            stmt.setInt(2, cid);
            stmt.setInt(3, user);
            stmt.execute();
        } catch (final SQLException e) {
            throw new SettingException(SettingException.Code.SQL_ERROR, e);
        } finally {
            closeSQLStuff(stmt);
        }
    }

    private <T> void insertAttribute(final int cid, final int user, final Attribute<T> attribute, final T value, final Connection con) throws SettingException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("INSERT INTO user_setting_server (" + attribute.getColumnName() + ",cid,user) VALUES (?,?,?)");
            attribute.setAttribute(stmt, value);
            stmt.setInt(2, cid);
            stmt.setInt(3, user);
            stmt.execute();
        } catch (final SQLException e) {
            throw new SettingException(SettingException.Code.SQL_ERROR, e);
        } finally {
            closeSQLStuff(null, stmt);
        }
    }

    private void deleteEntry(final int cid, final int user, final Connection con) throws SettingException {
        PreparedStatement stmt = null;
        final ResultSet rs = null;
        try {
            stmt = con.prepareStatement("DELETE FROM user_setting_server WHERE cid=? AND user=?");
            stmt.setInt(1, cid);
            stmt.setInt(2, user);
            stmt.executeUpdate();
        } catch (final SQLException e) {
            throw new SettingException(SettingException.Code.SQL_ERROR, e);
        } finally {
            closeSQLStuff(rs, stmt);
        }
    }

    private boolean hasEntry(final int cid, final int user, final Connection con) throws SettingException {
        boolean retval = false;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT user FROM user_setting_server WHERE cid=? AND user=?");
            stmt.setInt(1, cid);
            stmt.setInt(2, user);
            rs = stmt.executeQuery();
            retval = rs.next();
        } catch (final SQLException e) {
            throw new SettingException(SettingException.Code.SQL_ERROR, e);
        } finally {
            closeSQLStuff(rs, stmt);
        }
        return retval;
    }

    private static interface Attribute<T> {

        void setAttribute(PreparedStatement pstmt, T value) throws SQLException;

        T getAttribute(ResultSet rs) throws SQLException;

        String getColumnName();
    }

}
