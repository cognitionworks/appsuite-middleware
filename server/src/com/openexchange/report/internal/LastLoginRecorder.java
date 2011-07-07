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

package com.openexchange.report.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserImpl;
import com.openexchange.login.LoginHandlerService;
import com.openexchange.login.LoginRequest;
import com.openexchange.login.LoginResult;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.user.UserService;

/**
 * {@link LastLoginRecorder} records the last login of a user in its user attributes.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class LastLoginRecorder implements LoginHandlerService {

    public LastLoginRecorder() {
        super();
    }

    public void handleLogin(final LoginResult login) throws OXException {
        final LoginRequest request = login.getRequest();
        String key;
        if (null != request.getClient()) {
            key = request.getClient();
        } else if (null != request.getInterface()) {
            key = request.getInterface().toString();
        } else {
            return;
        }
        
        key = "client:" + key;
        final Context ctx = login.getContext();
        if (ctx.isReadOnly()) {
            return;
        }
        final User origUser = login.getUser();
        final Map<String, Set<String>> attributes = new HashMap<String, Set<String>>();
        attributes.putAll(origUser.getAttributes());
        final Set<String> value = new HashSet<String>();
        value.add(Long.toString(System.currentTimeMillis()));
        attributes.put(key, value);
        final UserImpl newUser = new UserImpl(origUser);
        newUser.setAttributes(attributes);
        UserService service;
        try {
            service = ServerServiceRegistry.getInstance().getService(UserService.class, true);
            service.updateUser(newUser, ctx);
        } catch (final OXException e) {
            throw e;
        }
    }

    public void handleLogout(final LoginResult logout) {
        // Nothing to to.
    }
}
