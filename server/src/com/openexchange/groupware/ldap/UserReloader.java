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

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.openexchange.cache.dynamic.impl.OXObjectFactory;
import com.openexchange.cache.dynamic.impl.Refresher;
import com.openexchange.caching.CacheException;
import com.openexchange.groupware.AbstractOXException;

/**
 * This class is used overall behind the User interface and it manages to reload
 * the user object into the cache if the cache invalidated it.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
final class UserReloader extends Refresher<User> implements User {

    /**
     * For serialization.
     */
    private static final long serialVersionUID = -2424522083743916869L;

    /**
     * Cached delegate.
     */
    private User delegate;

    /**
     * Default constructor.
     * @param factory Factory for loading the object from the database.
     * @param regionName Name of the cache region that stores the object.
     * @throws AbstractOXException if initially loading the object fails.
     */
    UserReloader(final OXObjectFactory<User> factory, final String regionName) throws AbstractOXException {
        super(factory, regionName);
        this.delegate = refresh();
    }

    public UserReloader(OXObjectFactory<User> factory, User user, String regionName) throws CacheException {
        super(factory, regionName);
        delegate = user;
        cache(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        updateDelegate();
        return delegate.equals(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        updateDelegate();
        return delegate.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "UserReloader: " + delegate.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getAliases() {
        updateDelegate();
        return delegate.getAliases();
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Set<String>> getAttributes() {
        updateDelegate();
        return delegate.getAttributes();
    }

    /**
     * {@inheritDoc}
     */
    public int getContactId() {
        return delegate.getContactId();
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayName() {
        updateDelegate();
        return delegate.getDisplayName();
    }

    /**
     * {@inheritDoc}
     */
    public String getGivenName() {
        updateDelegate();
        return delegate.getGivenName();
    }

    /**
     * {@inheritDoc}
     */
    public int[] getGroups() {
        updateDelegate();
        return delegate.getGroups();
    }

    /**
     * {@inheritDoc}
     */
    public int getId() {
        return delegate.getId();
    }

    /**
     * {@inheritDoc}
     */
    public String getImapLogin() {
        updateDelegate();
        return delegate.getImapLogin();
    }

    /**
     * {@inheritDoc}
     */
    public String getImapServer() {
        updateDelegate();
        return delegate.getImapServer();
    }

    /**
     * {@inheritDoc}
     */
    public Locale getLocale() {
        updateDelegate();
        return delegate.getLocale();
    }

    /**
     * {@inheritDoc}
     */
    public String getLoginInfo() {
        updateDelegate();
        return delegate.getLoginInfo();
    }

    /**
     * {@inheritDoc}
     */
    public String getMail() {
        updateDelegate();
        return delegate.getMail();
    }

    /**
     * {@inheritDoc}
     */
    public String getMailDomain() {
        updateDelegate();
        return delegate.getMailDomain();
    }

    /**
     * {@inheritDoc}
     */
    public String getPasswordMech() {
        updateDelegate();
        return delegate.getPasswordMech();
    }

    /**
     * {@inheritDoc}
     */
    public String getPreferredLanguage() {
        updateDelegate();
        return delegate.getPreferredLanguage();
    }

    /**
     * {@inheritDoc}
     */
    public int getShadowLastChange() {
        updateDelegate();
        return delegate.getShadowLastChange();
    }

    /**
     * {@inheritDoc}
     */
    public String getSmtpServer() {
        updateDelegate();
        return delegate.getSmtpServer();
    }

    /**
     * {@inheritDoc}
     */
    public String getSurname() {
        updateDelegate();
        return delegate.getSurname();
    }

    /**
     * {@inheritDoc}
     */
    public String getTimeZone() {
        updateDelegate();
        return delegate.getTimeZone();
    }

    /**
     * {@inheritDoc}
     */
    public String getUserPassword() {
        updateDelegate();
        return delegate.getUserPassword();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMailEnabled() {
        updateDelegate();
        return delegate.isMailEnabled();
    }

    /**
     * @throws RuntimeException if refreshing fails.
     */
    private void updateDelegate() throws RuntimeException {
        try {
            this.delegate = refresh();
        } catch (final AbstractOXException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
