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

package com.openexchange.mailmapping.impl;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.ConfigurationServices;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.mailmapping.ResolvedMail;
import com.openexchange.server.MockingServiceLookup;
import com.openexchange.user.User;
import com.openexchange.user.UserService;

/**
 * {@link DefaultMailMappingServiceTest}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConfigurationServices.class })
public class DefaultMailMappingServiceTest {

    private MockingServiceLookup services = null;
    private DefaultMailMappingService service = null;

    @Before
    public void setup() throws IOException {
        services = new MockingServiceLookup();
        Properties properties = mock(Properties.class);
        PowerMockito.mockStatic(ConfigurationServices.class);
        ConfigurationService configService = services.mock(ConfigurationService.class);
        when(configService.getFileByName(ArgumentMatchers.anyString())).thenReturn(mock(File.class));
        when(ConfigurationServices.loadPropertiesFrom(ArgumentMatchers.any(File.class))).thenReturn(properties);
        service = new DefaultMailMappingService(services);
    }

    @Test
    public void mailIsNull() throws OXException {
        assertNull(service.resolve(null));
    }

    @Test
    public void testResolve() throws OXException {
        ContextService contexts = services.mock(ContextService.class);
        UserService users = services.mock(UserService.class);

        Context ctx = mock(Context.class);
        when(I(ctx.getContextId())).thenReturn(I(42));

        User user = mock(User.class);
        when(I(user.getId())).thenReturn(I(12));

        when(I(contexts.getContextId("test.invalid"))).thenReturn(I(42));
        when(contexts.getContext(42)).thenReturn(ctx);

        when(users.searchUser("charlie@test.invalid", ctx, true)).thenReturn(user);

        ResolvedMail resolved = service.resolve("charlie@test.invalid", true);

        assertEquals(42, resolved.getContextID());
        assertEquals(12, resolved.getUserID());
    }

    @Test
    public void testResolveUnknownUser() throws OXException {
        ContextService contexts = services.mock(ContextService.class);
        UserService users = services.mock(UserService.class);

        Context ctx = mock(Context.class);
        when(I(ctx.getContextId())).thenReturn(I(42));

        when(I(contexts.getContextId("test.invalid"))).thenReturn(I(42));
        when(contexts.getContext(42)).thenReturn(ctx);

        when(users.searchUser("charlie@test.invalid", ctx, true)).thenReturn(null);

        ResolvedMail resolved = service.resolve("charlie@test.invalid");

        assertNull(resolved);
    }

    @Test
    public void testResolveUnknownContext() throws OXException {
        ContextService contexts = services.mock(ContextService.class);

        when(I(contexts.getContextId("test.invalid"))).thenReturn(I(0));

        ResolvedMail resolved = service.resolve("charlie@test.invalid");

        assertNull(resolved);
    }
}
