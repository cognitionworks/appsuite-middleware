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



package com.openexchange.groupware.ldap;

/**
 * Interface for the user object.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public interface User {

    /**
     * Getter for userPassword.
     * @return Password.
     */
    String getUserPassword();

    /**
     * Getter for uid.
     * @return User identifier.
     */
    int getId();

    /**
     * Getter for mailEnabled.
     * @return <code>true</code> if user is enabled.
     */
    boolean isMailEnabled();

    /**
     * Getter for shadowLastChange.
     * @return Days since Jan 1, 1970 that password was last changed.
     */
    int getShadowLastChange();

    /**
     * Getter for imapServer.
     * @return IMAP server.
     */
    String getImapServer();

    /**
     * Returns the login for the imap server. This field must not be filled.
     * Check IMAP configuration.
     * @return the login for the imap server.
     */
    String getImapLogin();

    /**
     * Getter for smtpServer.
     * @return SMTP server.
     */
    String getSmtpServer();

    /**
     * Getter for mailDomain.
     * @return mail domain.
     */
    String getMailDomain();

    /**
     * Getter for givenName.
     * @return given name.
     */
    String getGivenName();

    /**
     * Getter for sure name.
     * @return sure name.
     */
    String getSurname();

    /**
     * Getter for mail.
     * @return mail address.
     */
    String getMail();

    /**
     * @return mail aliases.
     */
    String[] getAliases();

    /**
     * Getter for displayName.
     * @return Display name.
     */
    String getDisplayName();

    /**
     * Getter for timeZone.
     * @return Timezone.
     */
    String getTimeZone();

    /**
     * Getter for preferredLanguage. The preferred language of the user.
     * According to RFC 2798 and 2068 it should be something like de-de, en-gb
     * or en.
     * @return Preferred Language.
     */
    String getPreferredLanguage();

    /**
     * Getter for groups.
     * @return the groups this user is member of.
     */
    int[] getGroups();

    /**
     * @return the contactId
     */
    int getContactId();

}
