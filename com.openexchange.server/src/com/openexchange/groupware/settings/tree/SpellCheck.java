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

package com.openexchange.groupware.settings.tree;

import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.settings.IValueHandler;
import com.openexchange.groupware.settings.PreferencesItemService;
import com.openexchange.groupware.settings.Setting;
import com.openexchange.groupware.settings.impl.AbstractUserFuncs;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.session.Session;
import com.openexchange.user.User;

/**
 * {@link SpellCheck} - Configuration tree entry for the spell check functionality of the user.
 * <p>
 * <b>Note</b>: This item is currently disabled in <code>com.openexchange.groupware.settings.impl.ConfigTree</code> class.<br>
 * By now a global option to enable/disable spell check functionality is sufficient.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SpellCheck implements PreferencesItemService {

    private static final String NAME = "spellcheck";

    public SpellCheck() {
        super();
    }

    @Override
    public String[] getPath() {
        return new String[] { NAME };
    }

    @Override
    public IValueHandler getSharedValue() {
        return new AbstractUserFuncs() {
            @Override
            public void getValue(final Session session, final Context ctx, final User user, final UserConfiguration userConfig, final Setting setting) throws OXException {
                String set = user.getAttributes().get(NAME);
                if (null != set) {
                    setting.setSingleValue(Boolean.valueOf(set));
                } else {
                    setting.setSingleValue(Boolean.FALSE);
                }
            }
            @Override
            public boolean isAvailable(final UserConfiguration userConfig) {
                return true;
            }
            @Override
            public boolean isWritable() {
                return true;
            }
            @Override
            public void writeValue(Session session, Context ctx, User user, Setting setting) throws OXException {
                String value = setting.getSingleValue().toString();
                UserStorage.getInstance().setAttribute(NAME, value, user.getId(), ctx);
            }
        };
    }
}
