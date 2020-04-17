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

package com.openexchange.admin.reseller.rmi;

import static org.junit.Assert.assertTrue;
import java.rmi.ServerException;
import java.util.Stack;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.openexchange.admin.reseller.rmi.dataobjects.ResellerAdmin;
import com.openexchange.admin.reseller.rmi.dataobjects.Restriction;
import com.openexchange.admin.reseller.rmi.extensions.OXContextExtensionImpl;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.dataobjects.UserModuleAccess;
import com.openexchange.admin.rmi.exceptions.DuplicateExtensionException;
import com.openexchange.admin.rmi.factory.ResellerAdminFactory;
import com.openexchange.admin.rmi.factory.UserFactory;

public class OXResellerUserTest extends AbstractOXResellerTest {

    /**
     * Initialises a new {@link OXResellerUserTest}.
     */
    public OXResellerUserTest() {
        super();
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testCreateTooManyOverallUser() throws Exception {
        ResellerAdmin adm = ResellerAdminFactory.createRandomResellerAdmin();
        adm.setRestrictions(new Restriction[] { MaxOverallUserRestriction(6) });
        getResellerManager().create(adm);
        try {
            Stack<Context> ctxstack = new Stack<Context>();
            Credentials resellerRandomCredentials = ResellerRandomCredentials(adm.getName());
            try {
                // create 3 contexts with 1 user -> 6 user total
                for (final Context ctx : new Context[] { createContext(resellerRandomCredentials), createContext(resellerRandomCredentials), createContext(resellerRandomCredentials) }) {
                    ctxstack.push(ctx);
                    User contextAdmin = UserFactory.createContextAdmin();
                    Credentials ctxauth = new Credentials(contextAdmin.getName(), contextAdmin.getPassword());
                    for (int i = 1; i < 2; i++) {
                        System.out.println("creating user " + i + " in Context " + ctx.getId());
                        createUser(ctx, ctxauth);
                    }
                }

                // 7th user must fail
                boolean createFailed = false;
                try {
                    User contextAdmin = UserFactory.createContextAdmin();
                    createUser(ctxstack.firstElement(), new Credentials(contextAdmin.getName(), contextAdmin.getPassword()));
                } catch (ServerException e) {
                    createFailed = true;
                }
                assertTrue("Create user must fail", createFailed);
            } finally {
                for (final Context ctx : ctxstack) {
                    deleteContext(ctx, resellerRandomCredentials);
                }
            }
        } finally {
            getResellerManager().delete(adm);
        }
    }

    @Test
    public void testCreateTooManyPerContextUser() throws Exception {
        ResellerAdmin adm = ResellerAdminFactory.createRandomResellerAdmin();
        final Credentials creds = ResellerRandomCredentials(adm.getName());

        getResellerManager().create(adm);
        try {
            Context ctx = createContext(creds);
            try {
                try {
                    ctx.addExtension(new OXContextExtensionImpl(new Restriction[] { MaxUserPerContextRestriction() }));
                } catch (DuplicateExtensionException e1) {
                    // Because the context is newly created this exception cannot occur
                    e1.printStackTrace();
                }
                // TODO Here we call change context to apply the restrictions if the create call is ready to handle extensions
                // this can be done directly with the create call
                getContextManager().change(ctx, creds);

                User oxadmin = UserFactory.createContextAdmin();
                Credentials ctxadmcreds = new Credentials(oxadmin.getName(), oxadmin.getPassword());
                createUser(ctx, ctxadmcreds);
                createUser(ctx, ctxadmcreds);

                // 3rd user must fail
                boolean createFailed = false;
                try {
                    createUser(ctx, ctxadmcreds);
                } catch (ServerException e) {
                    createFailed = true;
                }
                assertTrue("Create user must fail", createFailed);
            } finally {
                deleteContext(ctx, creds);
            }
        } finally {
            getResellerManager().delete(adm);
        }
    }

    /*
     * NOTE: this test must be changed, if /opt/open-xchange/etc/admindaemon/ModuleAccessDefinitions.properties
     * will be changed!
     */
    @Test
    public void testCreateTooManyPerContextUserByModuleAccess() throws Exception {
        ResellerAdmin adm = ResellerAdminFactory.createRandomResellerAdmin();
        final Credentials creds = ResellerRandomCredentials(adm.getName());

        getResellerManager().create(adm);
        try {
            Context ctx = createContext(creds);
            try {
                try {
                    ctx.addExtension(new OXContextExtensionImpl(new Restriction[] { new Restriction(Restriction.MAX_USER_PER_CONTEXT_BY_MODULEACCESS_PREFIX + "webmail_plus", "2"), new Restriction(Restriction.MAX_USER_PER_CONTEXT_BY_MODULEACCESS_PREFIX + "premium", "2")
                    }));
                } catch (DuplicateExtensionException e1) {
                    // Because the context is newly created this exception cannot occur
                    e1.printStackTrace();
                }
                // TODO Here we call change context to apply the restrictions if the create call is ready to handle extensions
                // this can be done directly with the create call
                getContextManager().change(ctx, creds);

                try {
                    Thread.sleep(500);
                    // Short sleep so the context is fully ready.
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                // webmail test (default perms)
                User oxadmin = UserFactory.createContextAdmin();
                Credentials ctxadmcreds = new Credentials(oxadmin.getName(), oxadmin.getPassword());
                createUser(ctx, ctxadmcreds);

                // 3rd user must fail
                boolean createFailed = false;
                try {
                    createUser(ctx, ctxadmcreds);
                } catch (ServerException e) {
                    createFailed = true;
                }
                assertTrue("Create user must fail", createFailed);

                // premium test
                // premium=contacts,webmail,calendar,delegatetask,tasks,editpublicfolders,infostore,
                // readcreatesharedfolders,ical,vcard,webdav,webdavxml
                final UserModuleAccess access = new UserModuleAccess();
                access.disableAll();
                access.setContacts(true);
                access.setWebmail(true);
                access.setCalendar(true);
                access.setDelegateTask(true);
                access.setTasks(true);
                access.setEditPublicFolders(true);
                access.setInfostore(true);
                access.setReadCreateSharedFolders(true);
                access.setIcal(true);
                access.setVcard(true);
                access.setWebdav(true);
                access.setWebdavXml(true);
                access.setGlobalAddressBookDisabled(false);

                createUser(ctx, access, ctxadmcreds);
                createUser(ctx, access, ctxadmcreds);

                // 3rd user must fail
                createFailed = false;
                try {
                    createUser(ctx, access, ctxadmcreds);
                } catch (ServerException e) {
                    createFailed = true;
                }
                assertTrue("Create user must fail", createFailed);
            } finally {
                deleteContext(ctx, creds);
            }
        } finally {
            getResellerManager().delete(adm);
        }
    }
}
