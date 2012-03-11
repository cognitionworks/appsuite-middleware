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

package com.openexchange.mailaccount;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.session.Session;

/**
 * {@link MailAccountStorageService} - The storage service for mail accounts.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface MailAccountStorageService {

    /**
     * Invalidates specified mail account.
     *
     * @param id The account ID
     * @param user The user ID
     * @param cid The context ID
     * @throws OXException If invalidation fails
     */
    public void invalidateMailAccount(int id, int user, int cid) throws OXException;

    /**
     * Gets the mail account identified by specified ID.
     *
     * @param id The mail account ID
     * @param user The user ID
     * @param cid The context ID
     * @return The mail account
     * @throws OXException If the mail account cannot be returned
     */
    public MailAccount getMailAccount(int id, int user, int cid) throws OXException;

    /**
     * Gets the mail accounts belonging to specified user in given context.
     *
     * @param user The user ID
     * @param cid The context ID
     * @return The user's mail accounts
     * @throws OXException If the mail accounts cannot be returned
     */
    public MailAccount[] getUserMailAccounts(int user, int cid) throws OXException;

    /**
     * Gets the mail accounts belonging to specified user in given context.
     *
     * @param user The user ID
     * @param cid The context ID
     * @param con The connection to use
     * @return The user's mail accounts
     * @throws OXException If the mail accounts cannot be returned
     */
    public MailAccount[] getUserMailAccounts(int user, int cid, Connection con) throws OXException;

    /**
     * Gets the default mail account belonging to specified user in given context.
     *
     * @param user The user ID
     * @param cid The context ID
     * @return The user's default mail account
     * @throws OXException If the default mail account cannot be returned
     */
    public MailAccount getDefaultMailAccount(int user, int cid) throws OXException;

    /**
     * Updates mail account's value taken from specified mail account.
     *
     * @param mailAccount The mail account containing the values to update.
     * @param attributes The attributes to update
     * @param user The user ID
     * @param cid The context ID
     * @param session The session
     * @throws OXException If the mail account cannot be updated
     */
    public void updateMailAccount(MailAccountDescription mailAccount, Set<Attribute> attributes, int user, int cid, Session session) throws OXException;

    /**
     * Updates mail account's value taken from specified mail account.
     *
     * @param mailAccount The mail account containing the values to update.
     * @param attributes The attributes to update
     * @param user The user ID
     * @param cid The context ID
     * @param session The session
     * @param con writable database connection.
     * @param changePrimary <code>true</code> to change primary account, too.
     * @throws OXException If the mail account cannot be updated
     */
    void updateMailAccount(MailAccountDescription mailAccount, Set<Attribute> attributes, int user, int cid, Session session, Connection con, boolean changePrimary) throws OXException;

    /**
     * Updates mail account's value taken from specified mail account.
     *
     * @param mailAccount The mail account containing the values to update.
     * @param user The user ID
     * @param cid The context ID
     * @param session The session
     * @throws OXException If the mail account cannot be updated
     */
    public void updateMailAccount(MailAccountDescription mailAccount, int user, int cid, Session session) throws OXException;

    /**
     * Inserts mail account's value taken from specified mail account.
     *
     * @param mailAccount The mail account containing the values to update.
     * @param user The user ID
     * @param ctx The context
     * @param session The session; set to <code>null</code> to insert mail account with an empty password
     * @return The ID of the newly created mail account
     * @throws OXException If the mail account cannot be updated
     */
    public int insertMailAccount(MailAccountDescription mailAccount, int user, Context ctx, Session session) throws OXException;

    /**
     * Inserts mail account's value taken from specified mail account.
     *
     * @param mailAccount The mail account containing the values to update.
     * @param user The user ID
     * @param ctx The context
     * @param session The session; set to <code>null</code> to insert mail account with an empty password
     * @param con writable database connection
     * @return The ID of the newly created mail account
     * @throws OXException If the mail account cannot be updated
     */
    int insertMailAccount(MailAccountDescription mailAccount, int user, Context ctx, Session session, Connection con) throws OXException;

    /**
     * Deletes the mail account identified by specified ID.
     *
     * @param id The mail account ID
     * @param properties Optional properties for delete event (passed to {@link MailAccountDeleteListener} instances)
     * @param user The user ID
     * @param cid The context ID
     * @throws OXException If the mail account cannot be deleted
     */
    public void deleteMailAccount(int id, Map<String, Object> properties, int user, int cid) throws OXException;

    /**
     * Deletes the mail account identified by specified ID.
     *
     * @param id The mail account ID
     * @param properties Optional properties for delete event (passed to {@link MailAccountDeleteListener} instances)
     * @param user The user ID
     * @param cid The context ID
     * @param deletePrimary <code>true</code> to delete also the primary mail account if the user is deleted.
     * @throws OXException If the mail account cannot be deleted
     */
    public void deleteMailAccount(int id, Map<String, Object> properties, int user, int cid, boolean deletePrimary) throws OXException;

    /**
     * Deletes the mail account identified by specified ID.
     *
     * @param id The mail account ID
     * @param properties Optional properties for delete event (passed to {@link MailAccountDeleteListener} instances)
     * @param user The user ID
     * @param cid The context ID
     * @param deletePrimary <code>true</code> to delete also the primary mail account if the user is deleted.
     * @param con The connection to use
     * @throws OXException If the mail account cannot be deleted
     */
    public void deleteMailAccount(int id, Map<String, Object> properties, int user, int cid, boolean deletePrimary, Connection con) throws OXException;

    /**
     * Gets the mail accounts of the users whose login matches specified login.
     *
     * @param login The login
     * @param cid The context ID
     * @return The mail accounts of the users whose login matches specified login
     * @throws OXException If resolving the login fails
     */
    public MailAccount[] resolveLogin(String login, int cid) throws OXException;

    /**
     * Gets the mail accounts of the users whose login matches specified login on specified server.
     *
     * @param login The login
     * @param server The server's internet address
     * @param cid The context ID
     * @return The mail accounts of the users whose login matches specified login on specified server
     * @throws OXException If resolving the login fails
     */
    public MailAccount[] resolveLogin(String login, InetSocketAddress server, int cid) throws OXException;

    /**
     * Gets the mail accounts of the users whose primary email address matches specified email on specified server.
     *
     * @param primaryAddress The primary email address
     * @param cid The context ID
     * @return The mail accounts of the users whose login matches specified login on specified server
     * @throws OXException If resolving the primary address fails
     */
    public MailAccount[] resolvePrimaryAddr(String primaryAddress, int cid) throws OXException;

    /**
     * Gets the mail account matching specified primary email address of given user in given context.
     *
     * @param primaryAddress The primary address to look for
     * @param user The user ID
     * @param cid The context ID
     * @return The ID of the mail account matching specified primary email address or <code>-1</code> if none found
     * @throws OXException If look-up by primary address caused a conflict
     */
    public int getByPrimaryAddress(String primaryAddress, int user, int cid) throws OXException;

    /**
     * Gets those mail accounts of given user in given context whose host name occurs in specified collection of host names.
     *
     * @param hostNames The host names
     * @param user The user identifier
     * @param cid The context identifier
     * @return The binary-sorted identifiers of matching mail accounts
     * @throws OXException If look-up by host names caused an error
     */
    public int[] getByHostNames(Collection<String> hostNames, int user, int cid) throws OXException;

    /**
     * Gets the transport account for specified account ID.
     *
     * @param id The account ID
     * @param user The user ID
     * @param cid The context ID
     * @return The mail account providing the information for the appropriate transport account.
     * @throws OXException If transport look-up fails
     */
    public MailAccount getTransportAccountForID(int id, int user, int cid) throws OXException;

    /**
     * Decodes stored encrypted strings using the old secret and encode them again using the new secret.
     *
     * @param user The user ID
     * @param cid The context ID
     * @param oldSecret The secret used for decrypting the stored passwords
     * @param newSecret The secret to use for encrypting the passwords again
     * @throws OXException
     */
    public void migratePasswords(int user, int cid, String oldSecret, String newSecret) throws OXException;

    /**
     * Finds out whether the user has items that are encrypted
     */
    public boolean hasAccounts(Session session) throws OXException;
}
