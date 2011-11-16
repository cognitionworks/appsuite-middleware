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

package com.openexchange.messaging.generic.internal;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openexchange.context.ContextService;
import com.openexchange.crypto.CryptoService;
import com.openexchange.database.DatabaseService;
import com.openexchange.datatypes.genericonf.storage.GenericConfigurationStorageService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.messaging.MessagingAccount;
import com.openexchange.messaging.MessagingExceptionCodes;
import com.openexchange.messaging.MessagingService;
import com.openexchange.messaging.generic.DefaultMessagingAccount;
import com.openexchange.messaging.generic.services.MessagingGenericServiceRegistry;
import com.openexchange.messaging.registry.MessagingServiceRegistry;
import com.openexchange.secret.SecretService;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.sql.DBUtils;

/**
 * {@link RdbMessagingAccountStorage} - The messaging account storage backed by database.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since Open-Xchange v6.16
 */
public class RdbMessagingAccountStorage implements MessagingAccountStorage {

    /**
     * The {@link DatabaseService} class.
     */
    private static final Class<DatabaseService> CLAZZ_DB = DatabaseService.class;

    /**
     * The {@link GenericConfigurationStorageService} class.
     */
    private static final Class<GenericConfigurationStorageService> CLAZZ_GEN_CONF = GenericConfigurationStorageService.class;

    private static final RdbMessagingAccountStorage INSTANCE = new RdbMessagingAccountStorage();

    /**
     * Gets the database-backed instance of account storage manager.
     *
     * @return The database-backed instance of account storage manager
     */
    public static RdbMessagingAccountStorage getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes a new {@link RdbMessagingAccountStorage}.
     */
    private RdbMessagingAccountStorage() {
        super();
    }

    private static final String SQL_SELECT = "SELECT confId, displayName FROM messagingAccount WHERE cid = ? AND user = ? AND serviceId = ? AND account = ?";

    private static final String SQL_SELECT_CONFIDS_FOR_USER = "SELECT confId FROM messagingAccount WHERE cid = ? AND user = ? AND serviceId = ?";

    @Override
    public MessagingAccount getAccount(final String serviceId, final int id, final Session session, final Modifier modifier) throws OXException {
        final DatabaseService databaseService = getService(CLAZZ_DB);
        /*
         * Readable connection
         */
        final int contextId = session.getContextId();
        final Connection rc;
        try {
            rc = databaseService.getReadOnly(contextId);
        } catch (final OXException e) {
            throw new OXException(e);
        }
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = rc.prepareStatement(SQL_SELECT);
            int pos = 1;
            stmt.setInt(pos++, contextId);
            stmt.setInt(pos++, session.getUserId());
            stmt.setString(pos++, serviceId);
            stmt.setInt(pos, id);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                throw MessagingExceptionCodes.ACCOUNT_NOT_FOUND.create(
                    Integer.valueOf(id),
                    serviceId,
                    Integer.valueOf(session.getUserId()),
                    Integer.valueOf(contextId));
            }
            final MessagingService messagingService = getService(MessagingServiceRegistry.class).getMessagingService(serviceId, session.getUserId(), session.getContextId());
            final DefaultMessagingAccount account = new DefaultMessagingAccount();
            account.setId(id);
            account.setMessagingService(messagingService);
            account.setDisplayName(rs.getString(2));
            {
                final GenericConfigurationStorageService genericConfStorageService = getService(CLAZZ_GEN_CONF);
                final Map<String, Object> configuration = new HashMap<String, Object>();
                genericConfStorageService.fill(rc, getContext(session), rs.getInt(1), configuration);
                /*
                 * Decrypt password fields for clear-text representation in account's configuration
                 */
                final Set<String> secretPropNames = messagingService.getSecretProperties();
                if (!secretPropNames.isEmpty()) {
                    for (final String passwordElementName : secretPropNames) {
                        final String toDecrypt = (String) configuration.get(passwordElementName);
                        if (null != toDecrypt) {
                            try {
                                final String decrypted = decrypt(toDecrypt, session);
                                configuration.put(passwordElementName, decrypted);
                            } catch (final OXException x) {
                                // Must not be fatal
                                configuration.put(passwordElementName, "");
                                // Supply a (probably false) password anyway.
                            }
                        }
                    }
                }
                account.setConfiguration(configuration);
            }
            return modifier.modifyOutgoing(account);
        } catch (final SQLException e) {
            throw MessagingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } catch (final OXException e) {
            throw new OXException(e);
        } finally {
            DBUtils.closeSQLStuff(rs, stmt);
            databaseService.backReadOnly(contextId, rc);
        }
    }

    private static final String SQL_SELECT_ACCOUNTS = "SELECT account, confId, displayName FROM messagingAccount WHERE cid = ? AND user = ? AND serviceId = ?";

    @Override
    public List<MessagingAccount> getAccounts(final String serviceId, final Session session, final Modifier modifier) throws OXException {
        final DatabaseService databaseService = getService(CLAZZ_DB);
        /*
         * Readable connection
         */
        final int contextId = session.getContextId();
        final Connection rc;
        try {
            rc = databaseService.getReadOnly(contextId);
        } catch (final OXException e) {
            throw new OXException(e);
        }
        List<MessagingAccount> accounts;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = rc.prepareStatement(SQL_SELECT_ACCOUNTS);
            int pos = 1;
            stmt.setInt(pos++, contextId);
            stmt.setInt(pos++, session.getUserId());
            stmt.setString(pos, serviceId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                accounts = new ArrayList<MessagingAccount>(4);
                final GenericConfigurationStorageService genericConfStorageService = getService(CLAZZ_GEN_CONF);
                final MessagingService messagingService;
                {
                    final MessagingServiceRegistry registry = getService(MessagingServiceRegistry.class);
                    messagingService = registry.getMessagingService(serviceId, session.getUserId(), session.getContextId());
                }
                do {
                    final DefaultMessagingAccount account = new DefaultMessagingAccount();
                    account.setDisplayName(rs.getString(3));
                    final Map<String, Object> configuration = new HashMap<String, Object>();
                    genericConfStorageService.fill(rc, getContext(session), rs.getInt(2), configuration);
                    account.setConfiguration(configuration);
                    account.setId(rs.getInt(1));
                    account.setMessagingService(messagingService);
                    accounts.add(modifier.modifyOutgoing(account));
                } while (rs.next());
            } else {
                accounts = Collections.emptyList();
            }
            return accounts;
        } catch (final SQLException e) {
            throw MessagingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } catch (final OXException e) {
            throw new OXException(e);
        } finally {
            DBUtils.closeSQLStuff(rs, stmt);
            databaseService.backReadOnly(contextId, rc);
        }
    }

    /**
     * Gets the identifiers of user-associated accounts of a certain service.
     *
     * @param serviceId The service identifier
     * @param session The session
     * @return The identifiers of user-associated accounts of a certain service
     * @throws OXException If identifiers cannot be returned
     */
    public TIntList getAccountIDs(final String serviceId, final Session session) throws OXException {
        final DatabaseService databaseService = getService(CLAZZ_DB);
        /*
         * Readable connection
         */
        final int contextId = session.getContextId();
        final Connection rc;
        try {
            rc = databaseService.getReadOnly(contextId);
        } catch (final OXException e) {
            throw new OXException(e);
        }
        TIntList accounts;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = rc.prepareStatement(SQL_SELECT_ACCOUNTS);
            int pos = 1;
            stmt.setInt(pos++, contextId);
            stmt.setInt(pos++, session.getUserId());
            stmt.setString(pos, serviceId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                accounts = new TIntArrayList(4);
                do {
                    accounts.add(rs.getInt(1));
                } while (rs.next());
            } else {
                accounts = new TIntArrayList(0);
            }
            return accounts;
        } catch (final SQLException e) {
            throw MessagingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            DBUtils.closeSQLStuff(rs, stmt);
            databaseService.backReadOnly(contextId, rc);
        }
    }

    private static final String SQL_INSERT = "INSERT INTO messagingAccount (cid, user, account, confId, serviceId, displayName) VALUES (?, ?, ?, ?, ?, ?)";

    @Override
    public int addAccount(final String serviceId, final MessagingAccount account, final Session session, final Modifier modifier) throws OXException {
        final DatabaseService databaseService = getService(CLAZZ_DB);
        /*
         * Writable connection
         */
        final int contextId = session.getContextId();
        final Connection wc;
        try {
            wc = databaseService.getWritable(contextId);
            wc.setAutoCommit(false); // BEGIN
        } catch (final SQLException e) {
            throw MessagingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        }
        PreparedStatement stmt = null;
        try {
            /*
             * Save account configuration using generic conf
             */
            modifier.modifyIncoming(account);
            final int genericConfId;
            {
                final GenericConfigurationStorageService genericConfStorageService = getService(CLAZZ_GEN_CONF);
                final Map<String, Object> configuration = new HashMap<String, Object>(account.getConfiguration());
                /*
                 * Encrypt password fields to not having clear-text representation in database
                 */
                final MessagingService messagingService = getService(MessagingServiceRegistry.class).getMessagingService(serviceId, session.getUserId(), session.getContextId());
                final Set<String> secretPropNames = messagingService.getSecretProperties();
                if (!secretPropNames.isEmpty()) {
                    for (final String passwordElementName : secretPropNames) {
                        final String toCrypt = (String) configuration.get(passwordElementName);
                        if (null != toCrypt) {
                            final String encrypted = encrypt(toCrypt, session);
                            configuration.put(passwordElementName, encrypted);
                        }
                    }
                }
                genericConfId = genericConfStorageService.save(wc, getContext(session), configuration);
            }
            /*
             * Insert account data
             */
            stmt = wc.prepareStatement(SQL_INSERT);
            int pos = 1;
            stmt.setInt(pos++, contextId);
            stmt.setInt(pos++, session.getUserId());
            stmt.setInt(pos++, genericConfId);
            stmt.setInt(pos++, genericConfId);
            stmt.setString(pos++, serviceId);
            stmt.setString(pos, account.getDisplayName());
            stmt.executeUpdate();
            wc.commit(); // COMMIT
            return genericConfId;
        } catch (final OXException e) {
            DBUtils.rollback(wc); // ROLL-BACK
            throw e;
        } catch (final SQLException e) {
            DBUtils.rollback(wc); // ROLL-BACK
            throw MessagingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            DBUtils.rollback(wc); // ROLL-BACK
            throw MessagingExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            DBUtils.closeSQLStuff(stmt);
            DBUtils.autocommit(wc);
            databaseService.backWritable(contextId, wc);
        }
    }

    private String encrypt(final String toCrypt, final Session session) throws OXException, OXException {
        final CryptoService cryptoService = getService(CryptoService.class);
        final SecretService secretService = getService(SecretService.class);
        return cryptoService.encrypt(toCrypt, secretService.getSecret(session));
    }

    private String decrypt(final String toDecrypt, final Session session) throws OXException, OXException {
        final CryptoService cryptoService = getService(CryptoService.class);
        final SecretService secretService = getService(SecretService.class);
        return cryptoService.decrypt(toDecrypt, secretService.getSecret(session));
    }

    private static final String SQL_DELETE = "DELETE FROM messagingAccount WHERE cid = ? AND user = ? AND serviceId = ? AND account = ?";

    @Override
    public void deleteAccount(final String serviceId, final MessagingAccount account, final Session session, final Modifier modifier) throws OXException {
        final DatabaseService databaseService = getService(CLAZZ_DB);
        /*
         * Writable connection
         */
        final int contextId = session.getContextId();
        final Connection wc;
        try {
            wc = databaseService.getWritable(contextId);
            wc.setAutoCommit(false); // BEGIN
        } catch (final OXException e) {
            throw new OXException(e);
        } catch (final SQLException e) {
            throw MessagingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        }
        PreparedStatement stmt = null;
        try {
            modifier.modifyIncoming(account);
            final int accountId = account.getId();
            /*
             * Delete account configuration using generic conf
             */
            {
                final GenericConfigurationStorageService genericConfStorageService = getService(CLAZZ_GEN_CONF);
                final int genericConfId = getGenericConfId(contextId, session.getUserId(), serviceId, accountId, wc);
                genericConfStorageService.delete(wc, getContext(session), genericConfId);
            }
            /*
             * Delete account data
             */
            stmt = wc.prepareStatement(SQL_DELETE);
            int pos = 1;
            stmt.setInt(pos++, contextId);
            stmt.setInt(pos++, session.getUserId());
            stmt.setString(pos++, serviceId);
            stmt.setInt(pos, accountId);
            stmt.executeUpdate();
            wc.commit(); // COMMIT
        } catch (final OXException e) {
            DBUtils.rollback(wc); // ROLL-BACK
            throw new OXException(e);
        } catch (final SQLException e) {
            DBUtils.rollback(wc); // ROLL-BACK
            throw MessagingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            DBUtils.rollback(wc); // ROLL-BACK
            throw MessagingExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            DBUtils.closeSQLStuff(stmt);
            DBUtils.autocommit(wc);
            databaseService.backWritable(contextId, wc);
        }
    }

    private static final String SQL_UPDATE = "UPDATE messagingAccount SET displayName = ? WHERE cid = ? AND user = ? AND serviceId = ? AND account = ?";

    @Override
    public void updateAccount(final String serviceId, final MessagingAccount account, final Session session, final Modifier modifier) throws OXException {
        final DatabaseService databaseService = getService(CLAZZ_DB);
        /*
         * Writable connection
         */
        final int contextId = session.getContextId();
        final Connection wc;
        try {
            wc = databaseService.getWritable(contextId);
            wc.setAutoCommit(false); // BEGIN
        } catch (final OXException e) {
            throw new OXException(e);
        } catch (final SQLException e) {
            throw MessagingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        }
        PreparedStatement stmt = null;
        try {
            modifier.modifyIncoming(account);
            /*
             * Update account configuration using generic conf
             */
            {
                Map<String, Object> configuration = account.getConfiguration();
                if (null != configuration) {
                    configuration = new HashMap<String, Object>(configuration);
                    final GenericConfigurationStorageService genericConfStorageService = getService(CLAZZ_GEN_CONF);
                    final int genericConfId = getGenericConfId(contextId, session.getUserId(), serviceId, account.getId(), wc);
                    /*
                     * Encrypt password fields to not having clear-text representation in database
                     */
                    final MessagingService messagingService = getService(MessagingServiceRegistry.class).getMessagingService(serviceId, session.getUserId(), session.getContextId());
                    final Set<String> secretPropNames = messagingService.getSecretProperties();
                    if (!secretPropNames.isEmpty()) {
                        for (final String passwordElementName : secretPropNames) {
                            final String toCrypt = (String) configuration.get(passwordElementName);
                            if (null != toCrypt) {
                                final String encrypted = encrypt(toCrypt, session);
                                configuration.put(passwordElementName, encrypted);
                            }
                        }
                    }
                    genericConfStorageService.update(wc, getContext(session), genericConfId, configuration);
                }
            }
            /*
             * Update account data
             */
            final String displayName = account.getDisplayName();
            if (null != displayName) {
                stmt = wc.prepareStatement(SQL_UPDATE);
                int pos = 1;
                stmt.setString(pos++, displayName);
                stmt.setInt(pos++, contextId);
                stmt.setInt(pos++, session.getUserId());
                stmt.setString(pos++, serviceId);
                stmt.setInt(pos, account.getId());
                stmt.executeUpdate();
            }
            wc.commit(); // COMMIT
        } catch (final OXException e) {
            DBUtils.rollback(wc); // ROLL-BACK
            throw new OXException(e);
        } catch (final SQLException e) {
            DBUtils.rollback(wc); // ROLL-BACK
            throw MessagingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } catch (final Exception e) {
            DBUtils.rollback(wc); // ROLL-BACK
            throw MessagingExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            DBUtils.closeSQLStuff(stmt);
            DBUtils.autocommit(wc);
            databaseService.backWritable(contextId, wc);
        }
    }

    private static <S> S getService(final Class<? extends S> clazz) throws OXException {
        try {
            return MessagingGenericServiceRegistry.getServiceRegistry().getService(clazz, true);
        } catch (final OXException e) {
            throw new OXException(e);
        }
    }

    private static Context getContext(final Session session) throws OXException {
        if (session instanceof ServerSession) {
            return ((ServerSession) session).getContext();
        }
        try {
            return getService(ContextService.class).getContext(session.getContextId());
        } catch (final OXException e) {
            throw new OXException(e);
        }
    }

    private static int getGenericConfId(final int contextId, final int userId, final String serviceId, final int accountId, final Connection con) throws OXException, SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement(SQL_SELECT);
            int pos = 1;
            stmt.setInt(pos++, contextId);
            stmt.setInt(pos++, userId);
            stmt.setString(pos++, serviceId);
            stmt.setInt(pos, accountId);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                throw MessagingExceptionCodes.ACCOUNT_NOT_FOUND.create(
                    Integer.valueOf(accountId),
                    serviceId,
                    Integer.valueOf(userId),
                    Integer.valueOf(contextId));
            }
            return rs.getInt(1);
        } finally {
            DBUtils.closeSQLStuff(rs, stmt);
        }
    }

    public String checkSecretCanDecryptStrings(final MessagingService parentService, final Session session, final String secret) throws OXException {
        final Set<String> secretProperties = parentService.getSecretProperties();
        if (secretProperties.isEmpty()) {
            return null;
        }
        final TIntList confIds = getConfIDsForUser(session.getContextId(), session.getUserId(), parentService.getId());
        final GenericConfigurationStorageService genericConfStorageService = getService(CLAZZ_GEN_CONF);
        final CryptoService cryptoService = getService(CryptoService.class);

        final Context ctx = getContext(session);
        final HashMap<String, Object> content = new HashMap<String, Object>();
        int confId = -1;
        try {
            for (int i = 0, size = confIds.size(); i < size; i++) {
                confId = confIds.get(i);
                content.clear();
                genericConfStorageService.fill(ctx, confId, content);

                for (final String field : secretProperties) {
                    final String encrypted = (String) content.get(field);
                    if (encrypted != null) {
                        cryptoService.decrypt(encrypted, secret);
                    }
                }
            }
        } catch (final OXException e) {
            throw e;
        }
        return null;
    }

    private TIntList getConfIDsForUser(final int contextId, final int userId, final String serviceId) throws OXException {
        final TIntList confIds = new TIntArrayList(20);
        final DatabaseService databaseService = getService(CLAZZ_DB);
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection con = null;
        try {
            con = databaseService.getReadOnly(contextId);
            stmt = con.prepareStatement(SQL_SELECT_CONFIDS_FOR_USER);
            int pos = 1;
            stmt.setInt(pos++, contextId);
            stmt.setInt(pos++, userId);
            stmt.setString(pos++, serviceId);

            rs = stmt.executeQuery();

            while (rs.next()) {
                final int confId = rs.getInt(1);
                confIds.add(confId);
            }

        } catch (final SQLException e) {
            throw MessagingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } catch (final OXException e) {
            throw new OXException(e);
        } finally {
            DBUtils.closeSQLStuff(rs, stmt);
            if (con != null) {
                databaseService.backReadOnly(contextId, con);
            }
        }
        return confIds;
    }

    public void migrateToNewSecret(final MessagingService parentService, final String oldSecret, final String newSecret, final Session session) throws OXException {
        final Set<String> secretProperties = parentService.getSecretProperties();
        if (secretProperties.isEmpty()) {
            return;
        }
        final TIntList confIds = getConfIDsForUser(session.getContextId(), session.getUserId(), parentService.getId());
        final GenericConfigurationStorageService genericConfStorageService = getService(CLAZZ_GEN_CONF);
        final CryptoService cryptoService = getService(CryptoService.class);

        final Context ctx = getContext(session);
        final HashMap<String, Object> content = new HashMap<String, Object>();
        try {
            for (int i = 0, size = confIds.size(); i < size; i++) {
                final int confId = confIds.get(i);
                content.clear();
                genericConfStorageService.fill(ctx, confId, content);
                final HashMap<String, Object> update = new HashMap<String, Object>();
                for (final String field : secretProperties) {
                    final String encrypted = (String) content.get(field);
                    if (encrypted != null) {
                        try {
                            // Try using the new secret. Maybe this account doesn't need the migration
                            cryptoService.decrypt(encrypted, newSecret);
                        } catch (final OXException x) {
                            // Needs migration
                            final String transcripted = cryptoService.encrypt(cryptoService.decrypt(encrypted, oldSecret), newSecret);
                            update.put(field, transcripted);
                        }
                    }
                }
                genericConfStorageService.update(ctx, confId, update);
            }
        } catch (final OXException e) {
            throw e;
        }
    }
}
