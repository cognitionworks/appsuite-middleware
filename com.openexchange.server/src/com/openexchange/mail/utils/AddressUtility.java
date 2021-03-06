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

package com.openexchange.mail.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;

/**
 * {@link AddressUtility}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since 7.8.0
 */
public class AddressUtility {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AddressUtility.class);

    /**
     * Grabs the addresses from specified message.
     *
     * @param mail The mail message to get addresses from
     * @param session The associated session
     * @return The addresses
     * @throws OXException If addresses cannot be returned
     */
    public static Set<InternetAddress> getAddresses(MailMessage mail, ServerSession session) throws OXException {
        if (mail == null) {
            return Collections.emptySet();
        }

        return getFilteredAddresses(mail, getAliases(session));
    }

    /**
     * Gets user's alias addresses
     *
     * @param session The user-associated session
     * @return The user's alias addresses
     * @throws OXException If user's alias addresses cannot be returned
     */
    public static Set<InternetAddress> getAliases(ServerSession session) throws OXException {
        if (session == null) {
            return Collections.emptySet();
        }

        try {
            Set<InternetAddress> knownAddresses = new HashSet<InternetAddress>(4);
            UserSettingMail usm = session.getUserSettingMail();
            if (usm.getSendAddr() != null && usm.getSendAddr().length() > 0) {
                knownAddresses.add(new QuotedInternetAddress(usm.getSendAddr()));
            }
            final User user = UserStorage.getInstance().getUser(session.getUserId(), session.getContextId());
            knownAddresses.add(new QuotedInternetAddress(user.getMail()));
            final String[] aliases = user.getAliases();
            for (String alias : aliases) {
                knownAddresses.add(new QuotedInternetAddress(alias));
            }
            return knownAddresses;
        } catch (AddressException e) {
            LOG.warn("User's email aliases could not be loaded", e);
            return Collections.emptySet();
        }
    }

    /**
     * Grabs the addresses from specified message (filtered by given alias addresses).
     *
     * @param mail The mail message to get addresses from
     * @param aliases The user's alias addresses, which should not be contained in returned set
     * @return The addresses
     */
    public static Set<InternetAddress> getFilteredAddresses(MailMessage mail, Set<InternetAddress> aliases) {
        if (mail == null) {
            return Collections.emptySet();
        }

        // Collect available addresses from given mail message
        Set<InternetAddress> addrs = addAll(mail.getFrom(), null);
        addrs = addAll(mail.getTo(), addrs);
        addrs = addAll(mail.getCc(), addrs);
        addrs = addAll(mail.getBcc(), addrs);
        if (addrs == null) {
            return Collections.emptySet();
        }

        if (aliases != null) {
            // Filter by aliases
            addrs.removeAll(aliases);
        }
        return addrs;
    }

    private static Set<InternetAddress> addAll(InternetAddress[] addressesToAdd, Set<InternetAddress> targetSet) {
        Set<InternetAddress> addrs = targetSet;
        if (addressesToAdd.length> 0) {
            if (addrs == null) {
                addrs = new HashSet<InternetAddress>(Arrays.asList(addressesToAdd));
            } else {
                addrs.addAll(Arrays.asList(addressesToAdd));
            }
        }
        return addrs;
    }

}
