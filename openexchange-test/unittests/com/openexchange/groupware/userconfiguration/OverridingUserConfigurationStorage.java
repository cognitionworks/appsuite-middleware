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

package com.openexchange.groupware.userconfiguration;

import java.util.ArrayList;
import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;

/**
 * {@link OverridingUserConfigurationStorage}
 */
public class OverridingUserConfigurationStorage extends UserConfigurationStorage{

    protected UserConfigurationStorage delegate = null;

    public OverridingUserConfigurationStorage(final UserConfigurationStorage delegate) {
        this.delegate = delegate;
    }

    @Override
    protected void startInternal() throws OXException {
        delegate.startInternal();
    }

    @Override
    protected void stopInternal() throws OXException {
        delegate.stopInternal();
    }

    @Override
    public void initExtendedPermissions(final int userId, final Context ctx) {
        delegate.initExtendedPermissions(userId, ctx);
    }

    @Override
    public UserConfiguration getUserConfiguration(final int userId, final int[] groups, final Context ctx) throws OXException {
        final UserConfiguration config = getOverride(userId, groups, ctx);
        if( config != null) {
            return config;
        }
        return delegate.getUserConfiguration(userId, groups, ctx);
    }

    @Override
    public UserConfiguration[] getUserConfiguration(final Context ctx, final User[] users) throws OXException {
        final List<UserConfiguration> retval = new ArrayList<UserConfiguration>();
        for (final User user : users) {
            retval.add(getUserConfiguration(user.getId(), user.getGroups(), ctx));
        }
        return retval.toArray(new UserConfiguration[retval.size()]);
    }

    @Override
    public void clearStorage() throws OXException {
        delegate.clearStorage();
    }

    @Override
    public void removeUserConfiguration(final int userId, final Context ctx) throws OXException {
        delegate.removeUserConfiguration(userId,ctx);
    }

    public UserConfiguration getOverride(final int userId, final int[] groups, final Context ctx) throws OXException {
        return null;
    }

    public void override() throws OXException {
        UserConfigurationStorage.setInstance(this);
    }

    public void takeBack() throws OXException {
        UserConfigurationStorage.setInstance(delegate);
    }

    @Override
    public void saveUserConfiguration(final int permissionBits, final int userId, final Context ctx) throws OXException {
        delegate.saveUserConfiguration(permissionBits, userId, ctx);
    }
}
