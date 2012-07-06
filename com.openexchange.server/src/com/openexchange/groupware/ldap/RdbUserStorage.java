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

package com.openexchange.groupware.ldap;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.I2i;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.tools.sql.DBUtils.IN_LIMIT;
import static com.openexchange.tools.sql.DBUtils.autocommit;
import static com.openexchange.tools.sql.DBUtils.closeSQLStuff;
import static com.openexchange.tools.sql.DBUtils.getIN;
import static com.openexchange.tools.sql.DBUtils.rollback;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.impl.IDGenerator;
import com.openexchange.log.LogFactory;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.passwordchange.PasswordMechanism;
import com.openexchange.server.impl.DBPool;
import com.openexchange.tools.Collections.SmartIntArray;
import com.openexchange.tools.StringCollection;
import com.openexchange.tools.arrays.Arrays;
import com.openexchange.tools.sql.DBUtils;

/**
 * This class implements the user storage using a relational database instead
 * of a directory service.
 */
public class RdbUserStorage extends UserStorage {

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(RdbUserStorage.class));

    private static final String SELECT_ALL_USER = "SELECT id,userPassword,mailEnabled,imapServer,imapLogin,smtpServer,mailDomain," +
        "shadowLastChange,mail,timeZone,preferredLanguage,passwordMech,contactId FROM user WHERE user.cid=?";

    private static final String SELECT_USER = SELECT_ALL_USER + " AND id IN (";

    private static final String SELECT_ATTRS = "SELECT id,name,value FROM user_attribute WHERE cid=? AND id IN (";

    private static final String SELECT_CONTACT = "SELECT intfield01,field03,field02,field01 FROM prg_contacts WHERE cid=? AND intfield01 IN (";

    private static final String SELECT_ID = "SELECT id FROM login2user WHERE cid=? AND uid=?";

    private static final String SELECT_LOGIN = "SELECT id,uid FROM login2user where cid=? AND id IN (";

    private static final String SELECT_IMAPLOGIN = "SELECT id FROM user WHERE cid=? AND imapLogin=?";

    private static final String SQL_UPDATE_PASSWORD = "UPDATE user SET userPassword = ?, shadowLastChange = ? WHERE cid = ? AND id = ?";

    private static final String INSERT_USER = "INSERT INTO user (cid, id, imapServer, imapLogin, mail, mailDomain, mailEnabled, " +
        "preferredLanguage, shadowLastChange, smtpServer, timeZone, userPassword, contactId, passwordMech, uidNumber, gidNumber, " +
        "homeDirectory, loginShell) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_ATTRIBUTES = "INSERT INTO user_attribute (cid, id, name, value) VALUES (?, ?, ?, ?)";

    private static final String INSERT_LOGIN_INFO = "INSERT INTO login2user (cid, id, uid) VALUES (?, ?, ?)";


    /**
     * Default constructor.
     */
    public RdbUserStorage() {
        super();
    }

    @Override
    public int getUserId(final String uid, final Context context) throws OXException {
        Connection con = null;
        try {
            con = DBPool.pickup(context);
        } catch (final OXException e) {
            throw LdapExceptionCode.NO_CONNECTION.create(e).setPrefix("USR");
        }
        PreparedStatement stmt = null;
        ResultSet result = null;
        int userId = -1;
        try {
            stmt = con.prepareStatement(SELECT_ID);
            stmt.setInt(1, context.getContextId());
            stmt.setString(2, uid);
            result = stmt.executeQuery();
            if (result.next()) {
                userId = result.getInt(1);
            } else {
                throw LdapExceptionCode.USER_NOT_FOUND.create(uid, I(context.getContextId())).setPrefix("USR");
            }
        } catch (final SQLException e) {
            throw LdapExceptionCode.SQL_ERROR.create(e, e.getMessage()).setPrefix("USR");
        } finally {
            closeSQLStuff(result, stmt);
            DBPool.closeReaderSilent(context, con);
        }
        return userId;
    }
    
    @Override
    public int createUser(final Connection con, final Context context, final User user) throws OXException {
        PreparedStatement stmt = null;
        try {
            final int userId = IDGenerator.getId(context, com.openexchange.groupware.Types.PRINCIPAL, con);
            stmt = con.prepareStatement(INSERT_USER);
            int i = 1;
            stmt.setInt(i++, context.getContextId());
            stmt.setInt(i++, userId);
            setStringOrNull(i++, stmt, user.getImapServer());
            setStringOrNull(i++, stmt, user.getImapLogin());
            setStringOrNull(i++, stmt, user.getMail());
            setStringOrNull(i++, stmt, user.getMailDomain());
            
            /*
             * Setting mailEnabled to true as it is not part of the user object.
             * Referring to com.openexchange.admin.rmi.dataobjects.User this value is not used anyway.
             * This may(!) cause a loss of data during a user move/copy.
             */            
            stmt.setInt(i++, 1);
            
            setStringOrNull(i++, stmt, user.getPreferredLanguage());
            stmt.setInt(i++, user.getShadowLastChange());
            setStringOrNull(i++, stmt, user.getSmtpServer());
            setStringOrNull(i++, stmt, user.getTimeZone());
            setStringOrNull(i++, stmt, user.getUserPassword());
            stmt.setInt(i++, user.getContactId());
            setStringOrNull(i++, stmt, user.getPasswordMech());
            
            /*
             * Now we fill uidNumber, gidNumber, homeDirectory and loginShell.
             * As this seems not to be used anymore we set this manually.
             * This may(!) cause a loss of data during a user move/copy.
             */
            stmt.setInt(i++, 0);
            stmt.setInt(i++, 0);
            setStringOrNull(i++, stmt, "/home/" + user.getGivenName());
            setStringOrNull(i++, stmt, "/bin/bash");        
            stmt.executeUpdate();
            
            writeLoginInfo(con, user, context, userId); 
            writeUserAttributes(con, user, context, userId);                       
            return userId;
        } catch (final SQLException e) {
            throw UserExceptionCode.SQL_ERROR.create(e);
        } finally {
            closeSQLStuff(stmt);
        }
    }
    
    private void writeLoginInfo(final Connection con, final User user, final Context context, final int userId) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(INSERT_LOGIN_INFO);
            stmt.setInt(1, context.getContextId());
            stmt.setInt(2, userId);
            stmt.setString(3, user.getLoginInfo());
            
            stmt.executeUpdate();
        } finally {
            closeSQLStuff(stmt);
        }
    }

    private void writeUserAttributes(final Connection con, final User user, final Context context, final int userId) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(INSERT_ATTRIBUTES);
            final Map<String, Set<String>> attributes = user.getAttributes();
            for (final String key : attributes.keySet()) {
                final Set<String> valueSet = attributes.get(key);
                for (final String value : valueSet) {
                    stmt.setInt(1, context.getContextId());
                    stmt.setInt(2, userId);
                    stmt.setString(3, key);
                    stmt.setString(4, value);
                    
                    stmt.addBatch();
                }                
            }
            
            stmt.executeBatch();            
        } finally {
            closeSQLStuff(stmt);
        }
    }

    @Override
    public int createUser(final Context context, final User user) throws OXException {
        Connection con = null;
        try {
            con = DBPool.pickup(context);
            return createUser(con, context, user);
        } finally {
            DBPool.closeReaderSilent(context, con);
        }
    }
    
    private void setStringOrNull(final int parameter, final PreparedStatement stmt, final String value) throws SQLException {
        if (value == null) {
            stmt.setNull(parameter, java.sql.Types.VARCHAR);
        } else {
            stmt.setString(parameter, value);
        }
    }

    @Override
    public User getUser(final int userId, final Context context) throws OXException {
        final Connection con;
        try {
            con = DBPool.pickup(context);
        } catch (final OXException e) {
            throw LdapExceptionCode.NO_CONNECTION.create(e).setPrefix("USR");
        }
        try {
            return getUser(context, con, new int[] { userId })[0];
        } catch (final OXException e) {
            throw new OXException(e);
        } finally {
            DBPool.closeReaderSilent(context, con);
        }
    }

    @Override
    public User getUser(final Context ctx, final int userId, final Connection con) throws OXException {
        return getUser(ctx, con, new int[] { userId })[0];
    }

    private User[] getUser(final Context ctx, final Connection con, final int[] userIds) throws OXException {
        final int length = userIds.length;
        if (0 == length) {
            return new User[0];
        }
        final Map<Integer, UserImpl> users = new HashMap<Integer, UserImpl>(length);
        try {
            for (int i = 0; i < userIds.length; i += IN_LIMIT) {
                PreparedStatement stmt = null;
                ResultSet result = null;
                try {
                    final int[] currentUserIds = Arrays.extract(userIds, i, IN_LIMIT);
                    stmt = con.prepareStatement(getIN(SELECT_USER, currentUserIds.length));
                    int pos = 1;
                    stmt.setInt(pos++, ctx.getContextId());
                    for (final int userId : currentUserIds) {
                        stmt.setInt(pos++, userId);
                    }
                    result = stmt.executeQuery();
                    while (result.next()) {
                        final UserImpl user = new UserImpl();
                        pos = 1;
                        user.setId(result.getInt(pos++));
                        user.setUserPassword(result.getString(pos++));
                        user.setMailEnabled(result.getBoolean(pos++));
                        user.setImapServer(result.getString(pos++));
                        user.setImapLogin(result.getString(pos++));
                        user.setSmtpServer(result.getString(pos++));
                        user.setMailDomain(result.getString(pos++));
                        user.setShadowLastChange(result.getInt(pos++));
                        if (result.wasNull()) {
                            user.setShadowLastChange(-1);
                        }
                        user.setMail(result.getString(pos++));
                        user.setTimeZone(result.getString(pos++));
                        user.setPreferredLanguage(result.getString(pos++));
                        user.setPasswordMech(result.getString(pos++));
                        user.setContactId(result.getInt(pos++));
                        users.put(I(user.getId()), user);
                    }
                } finally {
                    closeSQLStuff(result, stmt);
                }
            }
        } catch (final SQLException e) {
            throw UserExceptionCode.LOAD_FAILED.create(e, e.getMessage());
        }
        for (final int userId : userIds) {
            if (!users.containsKey(I(userId))) {
                throw UserExceptionCode.USER_NOT_FOUND.create(I(userId), I(ctx.getContextId()));
            }
        }
        loadLoginInfo(ctx, con, users);
        loadContact(ctx, con, users);
        loadGroups(ctx, con, users);
        loadAttributes(ctx, con, users);
        final User[] retval = new User[users.size()];
        for (int i = 0; i < length; i++) {
            retval[i] = users.get(I(userIds[i]));
        }
        return retval;
    }

    @Override
    public User[] getUser(final Context ctx) throws OXException {
        final Connection con;
        try {
            con = DBPool.pickup(ctx);
        } catch (final OXException e) {
            throw new OXException(e);
        }
        try {
            return getUser(ctx, con, listAllUser(ctx, con));
        } finally {
            DBPool.closeReaderSilent(ctx, con);
        }
    }

    @Override
    public User[] getUser(final Context ctx, final int[] userIds) throws OXException {
        if (0 == userIds.length) {
            return new User[0];
        }
        final Connection con;
        try {
            con = DBPool.pickup(ctx);
        } catch (final OXException e) {
            throw new OXException(e);
        }
        try {
            return getUser(ctx, con, userIds);
        } finally {
            DBPool.closeReaderSilent(ctx, con);
        }
    }

    private void loadLoginInfo(final Context context, final Connection con, final Map<Integer, UserImpl> users) throws OXException {
        try {
            final Iterator<Integer> iter = users.keySet().iterator();
            for (int i = 0; i < users.size(); i += IN_LIMIT) {
                PreparedStatement stmt = null;
                ResultSet result = null;
                try {
                    final int length = Arrays.determineRealSize(users.size(), i, IN_LIMIT);
                    stmt = con.prepareStatement(getIN(SELECT_LOGIN, length));
                    int pos = 1;
                    stmt.setInt(pos++, context.getContextId());
                    for (int j = 0; j < length; j++) {
                        stmt.setInt(pos++, i(iter.next()));
                    }
                    result = stmt.executeQuery();
                    while (result.next()) {
                        users.get(I(result.getInt(1))).setLoginInfo(result.getString(2));
                    }
                } finally {
                    closeSQLStuff(result, stmt);
                }
            }
        } catch (final SQLException e) {
            throw UserExceptionCode.SQL_ERROR.create(e, e.getMessage());
        }
    }

    private void loadContact(final Context ctx, final Connection con, final Map<Integer, UserImpl> users) throws OXException {
        try {
            final Iterator<UserImpl> iter = users.values().iterator();
            for (int i = 0; i < users.size(); i += IN_LIMIT) {
                PreparedStatement stmt = null;
                ResultSet result = null;
                try {
                    final int length = Arrays.determineRealSize(users.size(), i, IN_LIMIT);
                    stmt = con.prepareStatement(getIN(SELECT_CONTACT, length));
                    int pos = 1;
                    stmt.setInt(pos++, ctx.getContextId());
                    final Map<Integer, UserImpl> userByContactId = new HashMap<Integer, UserImpl>(length, 1);
                    for (int j = 0; j < length; j++) {
                        final UserImpl user = iter.next();
                        stmt.setInt(pos++, user.getContactId());
                        userByContactId.put(I(user.getContactId()), user);
                    }
                    result = stmt.executeQuery();
                    while (result.next()) {
                        pos = 1;
                        final UserImpl user = userByContactId.get(I(result.getInt(pos++)));
                        user.setGivenName(result.getString(pos++));
                        user.setSurname(result.getString(pos++));
                        user.setDisplayName(result.getString(pos++));
                    }
                } finally {
                    closeSQLStuff(result, stmt);
                }
            }
        } catch (final SQLException e) {
            throw UserExceptionCode.SQL_ERROR.create(e, e.getMessage());
        }
    }

    private void loadGroups(final Context context, final Connection con, final Map<Integer, UserImpl> users) throws OXException {
        final Map<Integer, List<Integer>> tmp = new HashMap<Integer, List<Integer>>(users.size(), 1);
        for (final User user : users.values()) {
            final List<Integer> userGroups = new ArrayList<Integer>();
            userGroups.add(I(0));
            tmp.put(I(user.getId()), userGroups);
        }
        try {
            final Iterator<Integer> iter = users.keySet().iterator();
            for (int i = 0; i < users.size(); i += IN_LIMIT) {
                PreparedStatement stmt = null;
                ResultSet result = null;
                try {
                    final int length = Arrays.determineRealSize(users.size(), i, IN_LIMIT);
                    final String sql = getIN("SELECT member,id FROM groups_member WHERE cid=? AND member IN (", length);
                    stmt = con.prepareStatement(sql);
                    int pos = 1;
                    stmt.setInt(pos++, context.getContextId());
                    for (int j = 0; j < length; j++) {
                        stmt.setInt(pos++, i(iter.next()));
                    }
                    result = stmt.executeQuery();
                    while (result.next()) {
                        tmp.get(I(result.getInt(1))).add(I(result.getInt(2)));
                    }
                } finally {
                    closeSQLStuff(result, stmt);
                }
            }
        } catch (final SQLException e) {
            throw UserExceptionCode.SQL_ERROR.create(e, e.getMessage());
        }
        for (final UserImpl user : users.values()) {
            user.setGroups(I2i(tmp.get(I(user.getId()))));
        }
    }

    private void loadAttributes(final Context context, final Connection con, final Map<Integer, UserImpl> users) throws OXException {
        final Map<Integer, Map<String, Set<String>>> usersAttrs = new HashMap<Integer, Map<String, Set<String>>>();
        try {
            final Iterator<Integer> iter = users.keySet().iterator();
            for (int i = 0; i < users.size(); i += IN_LIMIT) {
                PreparedStatement stmt = null;
                ResultSet result = null;
                try {
                    final int length = Arrays.determineRealSize(users.size(), i, IN_LIMIT);
                    stmt = con.prepareStatement(getIN(SELECT_ATTRS, length));
                    int pos = 1;
                    stmt.setInt(pos++, context.getContextId());
                    for (int j = 0; j < length; j++) {
                        final int userId = i(iter.next());
                        stmt.setInt(pos++, userId);
                        usersAttrs.put(I(userId), new HashMap<String, Set<String>>());
                    }
                    result = stmt.executeQuery();
                    // Gather attributes
                    while (result.next()) {
                        final Map<String, Set<String>> attrs = usersAttrs.get(I(result.getInt(1)));
                        final String name = result.getString(2);
                        Set<String> set = attrs.get(name);
                        if (null == set) {
                            set = new HashSet<String>();
                            attrs.put(name, set);
                        }
                        set.add(result.getString(3));
                    }
                } finally {
                    closeSQLStuff(result, stmt);
                }
            }
        } catch (final SQLException e) {
            throw UserExceptionCode.SQL_ERROR.create(e, e.getMessage());
        }
        for (final UserImpl user : users.values()) {
            final Map<String, Set<String>> attrs = usersAttrs.get(I(user.getId()));
            // Check for aliases
            {
                final Set<String> aliases = attrs.get("alias");
                if (aliases == null) {
                    user.setAliases(new String[0]);
                } else {
                    final List<String> tmp = new ArrayList<String>(aliases.size());
                    for (final String alias : aliases) {
                        try {
                            tmp.add(new QuotedInternetAddress(alias, false).toUnicodeString());
                        } catch (final Exception e) {
                            tmp.add(alias);
                        }
                    }
                    user.setAliases(tmp.toArray(new String[tmp.size()]));
                }
            }
            // Apply attributes
            for (final Map.Entry<String, Set<String>> entry : attrs.entrySet()) {
                entry.setValue(Collections.unmodifiableSet(entry.getValue()));
            }
            user.setAttributes(Collections.unmodifiableMap(attrs));
        }
    }

    @Override
    public void updateUserInternal(final User user, final Context context) throws OXException {
        final int contextId = context.getContextId();
        final int userId = user.getId();
        final String timeZone = user.getTimeZone();
        final String preferredLanguage = user.getPreferredLanguage();
        final String password = user.getUserPassword();
        final String mech = user.getPasswordMech();
        final int shadowLastChanged = user.getShadowLastChange();

        try {
            final DBUtils.TransactionRollbackCondition condition = new DBUtils.TransactionRollbackCondition(3);
            do {
                final Connection con;
                try {
                    con = DBPool.pickupWriteable(context);
                } catch (final OXException e) {
                    throw LdapExceptionCode.NO_CONNECTION.create(e).setPrefix("USR");
                }
                condition.resetTransactionRollbackException();
                try {
                    DBUtils.startTransaction(con);
                    // Update time zone and language
                    if (null != timeZone && null != preferredLanguage) {
                        PreparedStatement stmt = null;
                        try {
                            final String sql = "UPDATE user SET timeZone=?,preferredLanguage=? WHERE cid=? AND id=?";
                            stmt = con.prepareStatement(sql);
                            int pos = 1;
                            stmt.setString(pos++, timeZone);
                            stmt.setString(pos++, preferredLanguage);
                            stmt.setInt(pos++, contextId);
                            stmt.setInt(pos++, userId);
                            stmt.execute();
                        } finally {
                            closeSQLStuff(stmt);
                        }
                    }
                    if (null != user.getAttributes()) {
                        updateAttributes(context, user, con);
                    }
                    if (null != password && null != mech) {
                        String encodedPassword = null;
                        PreparedStatement stmt = null;
                        try {
                            encodedPassword = PasswordMechanism.getEncodedPassword(mech, password);
                            stmt = con.prepareStatement(SQL_UPDATE_PASSWORD);
                            int pos = 1;
                            stmt.setString(pos++, encodedPassword);
                            stmt.setInt(pos++, shadowLastChanged);
                            stmt.setInt(pos++, contextId);
                            stmt.setInt(pos++, userId);
                            stmt.execute();
                        } catch (final UnsupportedEncodingException e) {
                            throw new SQLException(e.toString());
                        } catch (final NoSuchAlgorithmException e) {
                            throw new SQLException(e.toString());
                        } finally {
                            closeSQLStuff(stmt);
                        }
                    }
                    con.commit();
                } catch (final SQLException e) {
                    rollback(con);
                    if (!condition.isFailedTransactionRollback(e)) {
                        throw LdapExceptionCode.SQL_ERROR.create(e, e.getMessage()).setPrefix("USR");
                    }
                } catch (final OXException e) {
                    rollback(con);
                    throw new OXException(e);
                } finally {
                    autocommit(con);
                    DBPool.closeWriterSilent(context, con);
                }
            } while (condition.checkRetry());
        } catch (final SQLException e) {
            throw LdapExceptionCode.SQL_ERROR.create(e, e.getMessage()).setPrefix("USR");
        }
    }

    @Override
    public void setUserAttribute(final String name, final String value, final int userId, final Context context) throws OXException {
        if (null == name) {
            throw LdapExceptionCode.UNEXPECTED_ERROR.create("Attribute name is null.").setPrefix("USR");
        }
        final String attrName = new StringBuilder("attr_").append(name).toString();
        setAttribute(attrName, value, userId, context);
    }

    @Override
    public void setAttribute(final String name, final String value, final int userId, final Context context) throws OXException {
        if (null == name) {
            throw LdapExceptionCode.UNEXPECTED_ERROR.create("Attribute name is null.").setPrefix("USR");
        }
        final Connection con;
        try {
            con = DBPool.pickupWriteable(context);
        } catch (final OXException e) {
            throw LdapExceptionCode.NO_CONNECTION.create(e).setPrefix("USR");
        }
        try {
            con.setAutoCommit(false);
            setAttribute(context.getContextId(), con, userId, name, value);
            con.commit();
        } catch (final SQLException e) {
            rollback(con);
            throw LdapExceptionCode.SQL_ERROR.create(e, e.getMessage()).setPrefix("USR");
        } catch (final Exception e) {
            rollback(con);
            throw LdapExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage()).setPrefix("USR");
        } finally {
            autocommit(con);
            DBPool.closeWriterSilent(context, con);
        }
    }

    private void setAttribute(final int contextId, final Connection con, final int userId, final String name, final String value) throws SQLException {
        setAttribute(contextId, con, userId, name, value, 0);
    }

    private static final int MAX_RETRY = 3;

    private void setAttribute(final int contextId, final Connection con, final int userId, final String name, final String value, final int retryCount) throws SQLException {
        PreparedStatement stmt = null;
        boolean retry = false;
        try {
            stmt = con.prepareStatement("SELECT value FROM user_attribute WHERE cid=? AND id=? AND name=? FOR UPDATE");
            int pos = 1;
            stmt.setInt(pos++, contextId);
            stmt.setInt(pos++, userId);
            stmt.setString(pos, name);
            stmt.executeQuery();
        } finally {
            closeSQLStuff(stmt);
            stmt = null;
        }
        try {
            stmt = con.prepareStatement("DELETE FROM user_attribute WHERE cid=? AND id=? AND name=?");
            int pos = 1;
            stmt.setInt(pos++, contextId);
            stmt.setInt(pos++, userId);
            stmt.setString(pos, name);
            stmt.executeUpdate();
        } finally {
            closeSQLStuff(stmt);
            stmt = null;
        }
        if (null != value) {
            try {
                stmt = con.prepareStatement("INSERT INTO user_attribute (cid,id,name,value) VALUES (?,?,?,?)");
                int pos = 1;
                stmt.setInt(pos++, contextId);
                stmt.setInt(pos++, userId);
                stmt.setString(pos++, name);
                stmt.setString(pos, value);
                try {
                    stmt.executeUpdate();
                } catch (final SQLException e) {
                    if (retryCount > MAX_RETRY) {
                        throw e;
                    }
                    // Another thread inserted in the meantime: Retry
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Detected concurrent insertion of a user's attribute.", e);
                    }
                    retry = true;
                }
            } finally {
                closeSQLStuff(stmt);
            }
        }
        if (retry) {
            setAttribute(contextId, con, userId, name, value, retryCount + 1);
        }
    }

    @Override
    public String getUserAttribute(final String name, final int userId, final Context context) throws OXException {
        if (null == name) {
            throw LdapExceptionCode.UNEXPECTED_ERROR.create("Attribute name is null.").setPrefix("USR");
        }
        final Connection con = DBPool.pickup(context);
        try {
            final String attrName = new StringBuilder("attr_").append(name).toString();
            return getAttribute(context.getContextId(), con, userId, attrName);
        } catch (final SQLException e) {
            throw LdapExceptionCode.SQL_ERROR.create(e, e.getMessage()).setPrefix("USR");
        } catch (final Exception e) {
            throw LdapExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage()).setPrefix("USR");
        } finally {
            DBPool.closeWriterSilent(context, con);
        }
    }

    private String getAttribute(final int contextId, final Connection con, final int userId, final String name) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT value FROM user_attribute WHERE cid=? AND id=? AND name=?");
            int pos = 1;
            stmt.setInt(pos++, contextId);
            stmt.setInt(pos++, userId);
            stmt.setString(pos, name);
            rs = stmt.executeQuery();
            return rs.next() ? rs.getString(1) : null;
        } finally {
            closeSQLStuff(rs, stmt);
        }
    }

    private void updateAttributes(final Context ctx, final User user, final Connection con) throws SQLException, OXException {
        final int contextId = ctx.getContextId();
        final int userId = user.getId();
        final UserImpl load = new UserImpl();
        load.setId(userId);
        final Map<Integer, UserImpl> loadMap = new HashMap<Integer, UserImpl>(1);
        loadMap.put(I(userId), load);
        loadAttributes(ctx, con, loadMap);
        final Map<String, Set<String>> oldAttributes = load.getAttributes();
        final Map<String, Set<String>> attributes = user.getAttributes();
        final Map<String, Set<String>> added = new HashMap<String, Set<String>>();
        final Map<String, Set<String>> removed = new HashMap<String, Set<String>>();
        final Map<String, Set<String[]>> changed = new HashMap<String, Set<String[]>>();
        calculateDifferences(oldAttributes, attributes, added, removed, changed);
        PreparedStatement stmt = null;
        // Add new attributes
        if (!added.isEmpty()) {
            try {
                stmt = con.prepareStatement("INSERT INTO user_attribute (cid,id,name,value) VALUES (?,?,?,?)");
                stmt.setInt(1, contextId);
                stmt.setInt(2, userId);
                int size = 0;
                for (final Map.Entry<String, Set<String>> entry : added.entrySet()) {
                    for (final String value : entry.getValue()) {
                        stmt.setString(3, entry.getKey());
                        stmt.setString(4, value);
                        stmt.addBatch();
                        size++;
                    }
                }
                final int[] mLines = stmt.executeBatch();
                int lines = 0;
                for (final int mLine : mLines) {
                    lines += mLine;
                }
                if (size != lines) {
                    final OXException e = UserExceptionCode.UPDATE_ATTRIBUTES_FAILED.create(I(contextId), I(userId));
                    LOG.error(String.format("Old: %1$s, New: %2$s, Added: %3$s, Removed: %4$s, Changed: %5$s.", oldAttributes, attributes, added, removed, toString(changed)), e);
                    throw e;
                }
            } finally {
                closeSQLStuff(stmt);
            }
        }
        // Remove attributes
        if (!removed.isEmpty()) {
            try {
                stmt = con.prepareStatement("DELETE FROM user_attribute WHERE cid=? AND id=? AND name=? AND value=?");
                stmt.setInt(1, contextId);
                stmt.setInt(2, userId);
                int size = 0;
                for (final Map.Entry<String, Set<String>> entry : removed.entrySet()) {
                    for (final String value : entry.getValue()) {
                        stmt.setString(3, entry.getKey());
                        stmt.setString(4, value);
                        stmt.addBatch();
                        size++;
                    }
                }
                final int[] mLines = stmt.executeBatch();
                int lines = 0;
                for (final int mLine : mLines) {
                    lines += mLine;
                }
                if (size != lines) {
                    final OXException e = UserExceptionCode.UPDATE_ATTRIBUTES_FAILED.create(I(contextId), I(userId));
                    LOG.error(String.format("Old: %1$s, New: %2$s, Added: %3$s, Removed: %4$s, Changed: %5$s.", oldAttributes, attributes, added, removed, toString(changed)), e);
                    throw e;
                }
            } finally {
                closeSQLStuff(stmt);
            }
        }
        // Update attributes
        if (!changed.isEmpty()) {
            try {
                stmt = con.prepareStatement("UPDATE user_attribute SET value=? WHERE cid=? AND id=? AND name=? AND value=?");
                stmt.setInt(2, contextId);
                stmt.setInt(3, userId);
                int size = 0;
                for (final Map.Entry<String, Set<String[]>> entry : changed.entrySet()) {
                    for (final String[] value : entry.getValue()) {
                        stmt.setString(4, entry.getKey());
                        stmt.setString(5, value[0]);
                        stmt.setString(1, value[1]);
                        stmt.addBatch();
                        size++;
                    }
                }
                final int[] mLines = stmt.executeBatch();
                int lines = 0;
                for (final int mLine : mLines) {
                    lines += mLine;
                }
                if (size != lines) {
                    // Ignoring the failed update of a clients login timestamp. This only happens if a parallel login with the same client took place.
                    boolean onlyLogins = true;
                    for (final String name : changed.keySet()) {
                        if (!name.startsWith("client:")) {
                            onlyLogins = false;
                            break;
                        }
                    }
                    if (!onlyLogins) {
                        final OXException e = UserExceptionCode.UPDATE_ATTRIBUTES_FAILED.create(I(contextId), I(userId));
                        LOG.error(String.format("Old: %1$s, New: %2$s, Added: %3$s, Removed: %4$s, Changed: %5$s.", oldAttributes, attributes, added, removed, toString(changed)), e);
                        throw e;
                    }
                }
            } finally {
                closeSQLStuff(stmt);
            }
        }
    }

    private String toString(final Map<String, Set<String[]>> changed) {
        final StringBuilder sb = new StringBuilder("{");
        for (final Map.Entry<String, Set<String[]>> entry : changed.entrySet()) {
            sb.append(entry.getKey());
            sb.append("=[");
            for (final String[] value : entry.getValue()) {
                sb.append(value[0]);
                sb.append("=>");
                sb.append(value[1]);
                sb.append(',');
            }
            sb.setCharAt(sb.length() - 1, ']');
            sb.append(',');
        }
        sb.setCharAt(sb.length() - 1, '}');
        return sb.toString();
    }

    static void calculateDifferences(final Map<String, Set<String>> oldAttributes, final Map<String, Set<String>> newAttributes, final Map<String, Set<String>> added, final Map<String, Set<String>> removed, final Map<String, Set<String[]>> changed) {
        // Find added keys
        added.putAll(newAttributes);
        for (final String key : oldAttributes.keySet()) { added.remove(key); }
        // Find removed keys
        removed.putAll(oldAttributes);
        for (final String key : newAttributes.keySet()) { removed.remove(key); }
        // Now the keys that are contained in old and new attributes.
        for (final String key : newAttributes.keySet()) {
            if (oldAttributes.containsKey(key)) {
                compareValues(key, oldAttributes.get(key), newAttributes.get(key), added, removed, changed);
            }
        }
    }

    private static void compareValues(final String name, final Set<String> oldSet, final Set<String> newSet, final Map<String, Set<String>> added, final Map<String, Set<String>> removed, final Map<String, Set<String[]>> changed) {
        final Set<String> addedValues = new HashSet<String>();
        final Set<String> removedValues = new HashSet<String>();
        // Find added values for a key.
        addedValues.addAll(newSet);
        addedValues.removeAll(oldSet);
        // Find removed values for a key.
        removedValues.addAll(oldSet);
        removedValues.removeAll(newSet);
        final Iterator<String> addedIter = addedValues.iterator();
        final Iterator<String> removedIter = removedValues.iterator();
        while (addedIter.hasNext() && removedIter.hasNext()) {
            Set<String[]> values = changed.get(name);
            if (null == values) {
                values = new HashSet<String[]>();
                changed.put(name, values);
            }
            values.add(new String[] { removedIter.next(), addedIter.next() });
        }
        while (addedIter.hasNext()) {
            add(added, name, addedIter.next());
        }
        while (removedIter.hasNext()) {
            add(removed, name, removedIter.next());
        }
    }

    private static void add(final Map<String, Set<String>> attributes, final String name, final String value) {
        Set<String> values = attributes.get(name);
        if (null == values) {
            values = new HashSet<String>();
            attributes.put(name, values);
        }
        values.add(value);
    }

    @Override
    public User[] searchUserByName(final String name, final Context context, final int searchType) throws OXException {
        if (0 == searchType) {
            return new User[0];
        }
        final Connection con = DBPool.pickup(context);
        try {
            final String pattern = StringCollection.prepareForSearch(name, false, true);
            final int contextId = context.getContextId();
            final TIntSet userIds = new TIntHashSet();
            PreparedStatement stmt = null;
            ResultSet result = null;
            final boolean searchLoginName = (searchType & SEARCH_LOGIN_NAME) > 0;
            final boolean searchDisplayName = (searchType & SEARCH_DISPLAY_NAME) > 0;
            if (searchDisplayName && searchLoginName) {
                try {
                    stmt = con.prepareStatement("SELECT con.userid FROM prg_contacts con JOIN login2user lu ON con.userid = lu.id AND con.cid = lu.cid WHERE con.cid = ? AND (lu.uid LIKE ? OR con.field01 LIKE ?)");
                    stmt.setInt(1, contextId);
                    stmt.setString(2, pattern);
                    stmt.setString(3, pattern);
                    result = stmt.executeQuery();
                    while (result.next()) {
                        userIds.add(result.getInt(1));
                    }
                } catch (final SQLException e) {
                    throw LdapExceptionCode.SQL_ERROR.create(e, e.getMessage()).setPrefix("USR");
                } finally {
                    closeSQLStuff(result, stmt);
                }
            } else {
                if (searchLoginName) {
                    try {
                        stmt = con.prepareStatement("SELECT id FROM login2user WHERE cid=? AND uid LIKE ?");
                        stmt.setInt(1, contextId);
                        stmt.setString(2, pattern);
                        result = stmt.executeQuery();
                        while (result.next()) {
                            userIds.add(result.getInt(1));
                        }
                    } catch (final SQLException e) {
                        throw LdapExceptionCode.SQL_ERROR.create(e, e.getMessage()).setPrefix("USR");
                    } finally {
                        closeSQLStuff(result, stmt);
                    }
                }
                if (searchDisplayName) {
                    try {
                        stmt = con.prepareStatement("SELECT userid FROM prg_contacts WHERE cid=? AND fid=? AND userid IS NOT NULL AND field01 LIKE ?");
                        stmt.setInt(1, contextId);
                        stmt.setInt(2, FolderObject.SYSTEM_LDAP_FOLDER_ID);
                        stmt.setString(3, pattern);
                        result = stmt.executeQuery();
                        while (result.next()) {
                            userIds.add(result.getInt(1));
                        }
                    } catch (final SQLException e) {
                        throw LdapExceptionCode.SQL_ERROR.create(e, e.getMessage()).setPrefix("USR");
                    } finally {
                        closeSQLStuff(result, stmt);
                    }
                }
            }
            return getUser(context, userIds.toArray());
        } finally {
            DBPool.closeReaderSilent(context, con);
        }
    }

    @Override
    public User searchUser(final String email, final Context context) throws OXException {
        String sql = "SELECT id FROM user WHERE cid=? AND mail LIKE ?";
        final Connection con = DBPool.pickup(context);
        try {
            final String pattern = StringCollection.prepareForSearch(email, false, true);
            PreparedStatement stmt = null;
            ResultSet result = null;
            int userId = -1;
            try {
                stmt = con.prepareStatement(sql);
                stmt.setInt(1, context.getContextId());
                stmt.setString(2, pattern);
                result = stmt.executeQuery();
                if (result.next()) {
                    userId = result.getInt(1);
                }
            } catch (final SQLException e) {
                throw LdapExceptionCode.SQL_ERROR.create(e, e.getMessage()).setPrefix("USR");
            } finally {
                closeSQLStuff(result, stmt);
            }
            try {
                if (userId == -1) {
                    sql = "SELECT id FROM user_attribute WHERE cid=? AND name=? AND value LIKE ?";
                    stmt = con.prepareStatement(sql);
                    int pos = 1;
                    stmt.setInt(pos++, context.getContextId());
                    stmt.setString(pos++, "alias");
                    stmt.setString(pos++, pattern);
                    result = stmt.executeQuery();
                    if (result.next()) {
                        userId = result.getInt(1);
                    }
                }
                if (userId == -1) {
                    throw LdapExceptionCode.NO_USER_BY_MAIL.create(email).setPrefix("USR");
                }
                return getUser(context, con, new int[] { userId })[0];
            } catch (final SQLException e) {
                throw LdapExceptionCode.SQL_ERROR.create(e, e.getMessage()).setPrefix("USR");
            } finally {
                closeSQLStuff(result, stmt);
            }
        } finally {
            DBPool.closeReaderSilent(context, con);
        }
    }

    @Override
    public int[] listModifiedUser(final Date modifiedSince, final Context context)
        throws OXException {
        Connection con = null;
        try {
            con = DBPool.pickup(context);
        } catch (final Exception e) {
            throw LdapExceptionCode.NO_CONNECTION.create(e).setPrefix("USR");
        }
        final String sql = "SELECT id FROM user LEFT JOIN prg_contacts ON (user.cid=prg_contacts.cid AND user.contactId=prg_contacts.intfield01) WHERE cid=? AND changing_date>=?";
        int[] users;
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, context.getContextId());
            stmt.setTimestamp(2, new Timestamp(modifiedSince.getTime()));
            result = stmt.executeQuery();
            final List<Integer> tmp = new ArrayList<Integer>();
            while (result.next()) {
                tmp.add(I(result.getInt(1)));
            }
            users = new int[tmp.size()];
            for (int i = 0; i < users.length; i++) {
                users[i] = tmp.get(i).intValue();
            }
        } catch (final SQLException e) {
            throw LdapExceptionCode.SQL_ERROR.create(e, e.getMessage()).setPrefix("USR");
        } finally {
            closeSQLStuff(result, stmt);
            DBPool.closeReaderSilent(context, con);
        }
        return users;
    }

    @Override
    public int[] listAllUser(final Context context) throws OXException {
        Connection con = null;
        try {
            con = DBPool.pickup(context);
        } catch (final Exception e) {
            throw UserExceptionCode.NO_CONNECTION.create(e);
        }
        try {
            return listAllUser(context, con);
        } finally {
            DBPool.closeReaderSilent(context, con);
        }
    }

    private int[] listAllUser(final Context ctx, final Connection con) throws OXException {
        final int[] users;
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = con.prepareStatement("SELECT id FROM user WHERE user.cid=?");
            stmt.setInt(1, ctx.getContextId());
            result = stmt.executeQuery();
            final TIntList tmp = new TIntArrayList();
            while (result.next()) {
                tmp.add(result.getInt(1));
            }
            users = tmp.toArray();
        } catch (final SQLException e) {
            throw UserExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(result, stmt);
        }
        return users;
    }

    @Override
    public int[] resolveIMAPLogin(final String imapLogin, final Context context) throws OXException {
        Connection con = null;
        try {
            con = DBPool.pickup(context);
        } catch (final Exception e) {
            throw UserExceptionCode.NO_CONNECTION.create(e);
        }
        final int[] users;
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = con.prepareStatement(SELECT_IMAPLOGIN);
            final int cid = context.getContextId();
            stmt.setInt(1, cid);
            stmt.setString(2, imapLogin);
            result = stmt.executeQuery();
            final SmartIntArray sia = new SmartIntArray(4);
            if (result.next()) {
                do {
                    sia.append(result.getInt(1));
                } while (result.next());
            } else {
                throw UserExceptionCode.USER_NOT_FOUND.create(
                        imapLogin, I(cid));
            }
            users = sia.toArray();
        } catch (final SQLException e) {
            throw UserExceptionCode.SQL_ERROR.create(e, e
                .getMessage());
        } finally {
            closeSQLStuff(result, stmt);
            DBPool.closeReaderSilent(context, con);
        }
        return users;
    }

    @Override
    public void invalidateUser(final Context ctx, final int userId) {
        // Nothing to do.
    }

    @Override
    protected void startInternal() {
        // Nothing to set up.
    }

    @Override
    protected void stopInternal() {
        // Nothing to tear down.
    }
}
