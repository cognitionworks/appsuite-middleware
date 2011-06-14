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

package com.openexchange.imap.entity2acl;

import java.io.IOException;
import javax.mail.MessagingException;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.imap.config.IMAPConfig;
import com.openexchange.mail.MailException;
import com.openexchange.mail.mime.MIMEMailException;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.server.impl.OCLPermission;
import com.sun.mail.imap.IMAPStore;

/**
 * {@link Entity2ACL} - Maps numeric entity IDs to corresponding IMAP login name (used in ACLs) and vice versa
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class Entity2ACL {

    private static volatile boolean instantiated;

    /**
     * The constant reflecting the found group {@link OCLPermission#ALL_GROUPS_AND_USERS}.
     */
    protected static final UserGroupID ALL_GROUPS_AND_USERS = new UserGroupID(OCLPermission.ALL_GROUPS_AND_USERS, true);

    /**
     * Singleton
     */
    private static Entity2ACL singleton;

    /**
     * Creates a new instance implementing the {@link Entity2ACL} interface.
     * 
     * @param imapConfig The user's IMAP config
     * @return an instance implementing the {@link Entity2ACL} interface.
     * @throws Entity2ACLException if the instance can't be created.
     */
    public static final Entity2ACL getInstance(final IMAPConfig imapConfig) throws Entity2ACLException {
        if (instantiated && MailAccount.DEFAULT_ID == imapConfig.getAccountId()) {
            /*
             * Auto-detection is turned off, return configured implementation
             */
            return singleton;
        }
        /*
         * Auto-detect dependent on user's IMAP settings
         */
        try {
            return Entity2ACLAutoDetector.getEntity2ACLImpl(imapConfig);
        } catch (final IOException e) {
            throw new Entity2ACLException(Entity2ACLException.Code.IO_ERROR, e, e.getMessage());
        }
    }

    /**
     * Creates a new instance implementing the {@link Entity2ACL} interface.
     * 
     * @param imapStore The IMAP store
     * @param imapConfig The user's IMAP configuration
     * @return an instance implementing the {@link Entity2ACL} interface.
     * @throws Entity2ACLException if the instance can't be created.
     * @throws MailException If a mail error occurs
     */
    public static final Entity2ACL getInstance(final IMAPStore imapStore, final IMAPConfig imapConfig) throws Entity2ACLException, MailException {
        if (instantiated && MailAccount.DEFAULT_ID == imapConfig.getAccountId()) {
            /*
             * Auto-detection is turned off, return configured implementation
             */
            return singleton;
        }
        try {
            return Entity2ACLAutoDetector.impl4(imapStore.getGreeting(), imapConfig);
        } catch (MessagingException e) {
            throw MIMEMailException.handleMessagingException(e, imapConfig);
        }
    }

    /**
     * Resets entity2acl
     */
    final static void resetEntity2ACL() {
        singleton = null;
        instantiated = false;
    }

    /**
     * Only invoked if auto-detection is turned off
     * 
     * @param singleton The singleton instance of {@link Entity2ACL}
     */
    final static void setInstance(final Entity2ACL singleton) {
        Entity2ACL.singleton = singleton;
        instantiated = true;
    }

    /*-
     * Member section
     */

    /**
     * Initializes a new {@link Entity2ACL}
     */
    protected Entity2ACL() {
        super();
    }

    /**
     * Returns a newly created {@link UserGroupID} instance reflecting a found user.
     * 
     * @param userId The user ID
     * @return A newly created {@link UserGroupID} instance reflecting a found user.
     */
    protected final UserGroupID getUserRetval(final int userId) {
        if (userId < 0) {
            return UserGroupID.NULL;
        }
        return new UserGroupID(userId, false);
    }

    /**
     * Determines the entity name of the user/group whose ID matches given <code>entity</code> that is used in IMAP server's ACL list.
     * 
     * @param entity The user/group ID
     * @param ctx The context
     * @param args The arguments container
     * @return the IMAP login of the user/group whose ID matches given <code>entity</code>
     * @throws AbstractOXException If user/group could not be found
     */
    public abstract String getACLName(int entity, Context ctx, Entity2ACLArgs args) throws AbstractOXException;

    /**
     * Determines the user/group ID whose either ACL entity name or user name matches given <code>pattern</code>.
     * 
     * @param pattern The pattern for either IMAP login or user name
     * @param ctx The context
     * @param args The arguments container
     * @return An instance of {@link UserGroupID} providing the user/group identifier whose IMAP login matches given <code>pattern</code> or
     *         {@link UserGroupID#NULL} if none found.
     * @throws AbstractOXException If user/group search fails
     */
    public abstract UserGroupID getEntityID(final String pattern, Context ctx, Entity2ACLArgs args) throws AbstractOXException;

}
