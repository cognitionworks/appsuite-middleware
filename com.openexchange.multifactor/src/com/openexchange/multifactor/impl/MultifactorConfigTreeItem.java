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

package com.openexchange.multifactor.impl;

import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.settings.IValueHandler;
import com.openexchange.groupware.settings.PreferencesItemService;
import com.openexchange.groupware.settings.ReadOnlyValue;
import com.openexchange.groupware.settings.Setting;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.jslob.ConfigTreeEquivalent;
import com.openexchange.multifactor.MultifactorProperties;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.user.User;

/**
 * {@link MultifactorConfigTreeItem}
 * Pass the configured value of com.openexchange.multifactor.allowMultiple to io.ox/multifactor//allowMultiple
 *
 * @author <a href="mailto:greg.hill@open-xchange.com">Greg Hill</a>
 * @since v7.10.2
 */
public class MultifactorConfigTreeItem implements ConfigTreeEquivalent, PreferencesItemService {

    @Override
    public String getConfigTreePath() {
        return "modules/multifactor/allowMultiple";
    }

    @Override
    public String getJslobPath() {
        return "io.ox/multifactor//allowMultiple";
    }

    @Override
    public String[] getPath() {
        return new String[] { "modules", "multifactor", "allowMultiple" };
    }

    @Override
    public IValueHandler getSharedValue() {
        return new ReadOnlyValue() {

            @Override
            public void getValue(Session session, Context ctx, User user, UserConfiguration userConfig, Setting setting) throws OXException {
                ConfigViewFactory factory = ServerServiceRegistry.getInstance().getService(ConfigViewFactory.class);

                Boolean enabled = (MultifactorProperties.allowMultiple.getDefaultValue() instanceof Boolean) ? (Boolean) MultifactorProperties.allowMultiple.getDefaultValue() : Boolean.TRUE;

                if (factory != null) {
                    ConfigView view = factory.getView(session.getUserId(), session.getContextId());
                    ComposedConfigProperty<Boolean> property = view.property(MultifactorProperties.allowMultiple.getFQPropertyName(), Boolean.class);
                    if (property.isDefined()) {
                        enabled = property.get();
                    }
                }

                setting.setSingleValue(enabled);
            }

            @Override
            public boolean isAvailable(UserConfiguration userConfig) {
                return true;
            }

        };
    }
}
