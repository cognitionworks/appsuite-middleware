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
package com.openexchange.admin.reseller.rmi;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashSet;

import org.junit.Test;

import com.openexchange.admin.reseller.rmi.dataobjects.ResellerAdmin;
import com.openexchange.admin.reseller.rmi.dataobjects.Restriction;
import com.openexchange.admin.reseller.rmi.exceptions.OXResellerException;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.exceptions.ContextExistsException;
import com.openexchange.admin.rmi.exceptions.DatabaseUpdateException;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;
import com.openexchange.admin.rmi.exceptions.StorageException;


public class OXResellerUserTest extends OXResellerAbstractTest {

    
    @Test
    public void testCreateTooManyOverallUser() throws MalformedURLException, RemoteException, NotBoundException, InvalidDataException, StorageException, InvalidCredentialsException, OXResellerException, ContextExistsException, NoSuchContextException, DatabaseUpdateException {
        final Credentials creds = DummyMasterCredentials();

        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);

        ResellerAdmin adm = FooAdminUser();
        HashSet<Restriction> res = new HashSet<Restriction>();
        res.add(MaxOverallUserRestriction(6));
        adm.setRestrictions(res);
        oxresell.create(adm, creds);

        // create 3 contexts with 1 user -> 6 user total
        for(final Context ctx : new Context[]{createContext(1337, ResellerFooCredentials()),
                createContext(1338, ResellerFooCredentials()), createContext(1339, ResellerFooCredentials())} ){
            Credentials ctxauth = new Credentials(ContextAdmin().getName(),ContextAdmin().getPassword());
            for(int i=1; i<2; i++) {
                System.out.println("creating user " + i + " in Context " + ctx.getId());
                createUser(ctx, ctxauth);
            }
        }

        // 7th user must fail
        boolean createFailed = false;
        try {
            createUser(new Context(1337), new Credentials(ContextAdmin().getName(),ContextAdmin().getPassword()));
        } catch (StorageException e) {
            createFailed = true;
        }
        assertTrue("Create user must fail",createFailed);

        for(final Context ctx : new Context[]{new Context(1337), new Context(1338), new Context(1339)} ){
            deleteContext(ctx, ResellerFooCredentials());
        }

        oxresell.delete(FooAdminUser(), DummyMasterCredentials());
    }

    @Test
    public void testCreateTooManyPerContextUser() throws MalformedURLException, RemoteException, NotBoundException, InvalidDataException, StorageException, InvalidCredentialsException, OXResellerException, ContextExistsException, NoSuchContextException, DatabaseUpdateException {
        final Credentials creds = ResellerFooCredentials();

        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);

        ResellerAdmin adm = FooAdminUser();
        oxresell.create(adm, DummyMasterCredentials());

        Context ctx = createContext(1337, creds);
        HashSet<Restriction> res = new HashSet<Restriction>();
        res.add(MaxUserPerContextRestriction());
        oxresell.applyRestrictionsToContext(res, ctx, creds);
        
        User oxadmin = ContextAdmin();
        Credentials ctxadmcreds = new Credentials(oxadmin.getName(), oxadmin.getPassword());
        createUser(ctx, ctxadmcreds);

        // 3rd user must fail
        boolean createFailed = false;
        try {
            createUser(ctx, ctxadmcreds);
        } catch (StorageException e) {
            createFailed = true;
        }
        assertTrue("Create user must fail",createFailed);

        deleteContext(ctx, creds);
        oxresell.delete(FooAdminUser(), DummyMasterCredentials());
    }
}
