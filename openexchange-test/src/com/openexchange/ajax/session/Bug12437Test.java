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

package com.openexchange.ajax.session;

import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXSession;
import com.openexchange.ajax.session.actions.LoginRequest;
import com.openexchange.ajax.session.actions.LoginResponse;
import com.openexchange.configuration.AJAXConfig;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.EnumComponent;
import com.openexchange.groupware.AbstractOXException.Category;

import junit.framework.TestCase;

/**
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class Bug12437Test extends TestCase {

    private AJAXClient client;

    private String login;

    private String password;

    /**
     * Default constructor.
     * @param name test name.
     */
    public Bug12437Test(final String name) {
        super(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AJAXConfig.init();
        final AJAXSession session = new AJAXSession();
        client = new AJAXClient(session);
        login = "some invalid login";
        password = "some invalid password";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void tearDown() throws Exception {
        client.getSession().getConversation().clearContents();
        super.tearDown();
    }

    /**
     * Checks if login with wrong credentials gives LGI-0006.
     */
    public void testWrongErrorCode() throws Throwable {
        final LoginRequest request = new LoginRequest(login, password, false);
        final LoginResponse response = client.execute(request);
        assertTrue("Wrong credentials are not detected.", response.hasError());
        final AbstractOXException exc = response.getException();
        assertEquals("Wrong exception message.", EnumComponent.LOGIN, exc.getComponent());
        assertEquals("Wrong exception message.", Category.USER_INPUT, exc.getCategory());
        assertEquals("Wrong exception message.", 6, exc.getDetailNumber());
    }
}
