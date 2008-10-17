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

import static org.junit.Assert.assertEquals;
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
import com.openexchange.admin.rmi.OXContextInterface;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.exceptions.ContextExistsException;
import com.openexchange.admin.rmi.exceptions.DatabaseUpdateException;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;
import com.openexchange.admin.rmi.exceptions.StorageException;

public class OXResellerContextTest extends OXResellerAbstractTest {

    
    @Test(expected=InvalidCredentialsException.class)
    public void testListAllContextInvalidAuthNoUser() throws MalformedURLException, RemoteException, NotBoundException, StorageException, InvalidCredentialsException, InvalidDataException, OXResellerException{

        final OXContextInterface oxctx = (OXContextInterface)Naming.lookup(getRMIHostUrl() + OXContextInterface.RMI_NAME);
       
        oxctx.listAll(ResellerBarCredentials());
    }

    @Test(expected=InvalidCredentialsException.class)
    public void testListAllContextInvalidAuthWrongpasswd() throws MalformedURLException, RemoteException, NotBoundException, StorageException, InvalidCredentialsException, InvalidDataException, OXResellerException{

        final OXContextInterface oxctx = (OXContextInterface)Naming.lookup(getRMIHostUrl() + OXContextInterface.RMI_NAME);
        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);
        
        oxresell.create(FooAdminUser(), DummyMasterCredentials());
        Credentials creds = ResellerFooCredentials();
        creds.setPassword("wrongpass");
        oxctx.listAll(creds);
    }

    @Test
    public void testListAllContextValidAuth() throws MalformedURLException, RemoteException, NotBoundException, StorageException, InvalidCredentialsException, InvalidDataException, OXResellerException{
        final Credentials creds = ResellerFooCredentials();

        final OXContextInterface oxctx = (OXContextInterface)Naming.lookup(getRMIHostUrl() + OXContextInterface.RMI_NAME);
        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);
        
        oxctx.listAll(creds);
        oxresell.delete(FooAdminUser(), DummyMasterCredentials());
    }

    @Test
    public void testCreateTooManyContexts() throws MalformedURLException, RemoteException, NotBoundException, StorageException, InvalidCredentialsException, InvalidDataException, OXResellerException, ContextExistsException, NoSuchContextException, DatabaseUpdateException{
        final Credentials creds = DummyMasterCredentials();

        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);

        ResellerAdmin adm = FooAdminUser();
        HashSet<Restriction> res = new HashSet<Restriction>();
        res.add(MaxContextRestriction());
        adm.setRestrictions(res);
        oxresell.create(adm, creds);

        Context ctx1 = createContext(1337, ResellerFooCredentials());
        Context ctx2 = createContext(1338, ResellerFooCredentials());
        Context ctx3 = null;
        boolean failed_ctx3 = false;
        try {
            ctx3 = createContext(1339, ResellerFooCredentials());
        } catch (StorageException e) {
            failed_ctx3 = true;
        }
        assertTrue("creation of ctx3 must fail",failed_ctx3);
        
        deleteContext(ctx1, ResellerFooCredentials());
        deleteContext(ctx2, ResellerFooCredentials());
        if( ctx3 != null ) {
            deleteContext(ctx3, ResellerFooCredentials());
        }
        
        oxresell.delete(FooAdminUser(), DummyMasterCredentials());
    }

    @Test
    public void testCreateTooManyOverallUser() throws MalformedURLException, RemoteException, NotBoundException, StorageException, InvalidCredentialsException, InvalidDataException, OXResellerException, ContextExistsException, NoSuchContextException, DatabaseUpdateException{
        final Credentials creds = DummyMasterCredentials();

        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);

        ResellerAdmin adm = FooAdminUser();
        HashSet<Restriction> res = new HashSet<Restriction>();
        res.add(MaxOverallUserRestriction(2));
        adm.setRestrictions(res);
        oxresell.create(adm, creds);

        Context ctx1 = createContext(1337, ResellerFooCredentials());
        Context ctx2 = createContext(1338, ResellerFooCredentials());
        Context ctx3 = null;
        boolean failed_ctx3 = false;
        try {
            ctx3 = createContext(1339, ResellerFooCredentials());
        } catch (StorageException e) {
            failed_ctx3 = true;
        }
        assertTrue("creation of ctx3 must fail",failed_ctx3);
        
        deleteContext(ctx1, ResellerFooCredentials());
        deleteContext(ctx2, ResellerFooCredentials());
        if( ctx3 != null ) {
            deleteContext(ctx3, ResellerFooCredentials());
        }
        
        oxresell.delete(FooAdminUser(), DummyMasterCredentials());
    }

    @Test
    public void testListAndDeleteContextOwnedByReseller() throws MalformedURLException, RemoteException, NotBoundException, StorageException, InvalidCredentialsException, InvalidDataException, OXResellerException, ContextExistsException, NoSuchContextException, DatabaseUpdateException{
        final Credentials creds = DummyMasterCredentials();

        final OXContextInterface oxctx = (OXContextInterface)Naming.lookup(getRMIHostUrl() + OXContextInterface.RMI_NAME);
        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);

        oxresell.create(FooAdminUser(), creds);
        oxresell.create(BarAdminUser(), creds);

        Context ctx = createContext(1337, ResellerFooCredentials());
        Context[] ret = oxctx.listAll(ResellerFooCredentials());
        assertEquals("listAll must return one entry", 1, ret.length);
        
        ret = oxctx.listAll(ResellerBarCredentials());
        assertEquals("listAll must return no entries", 0, ret.length);

        deleteContext(ctx, ResellerFooCredentials());
        
        oxresell.delete(FooAdminUser(), DummyMasterCredentials());
        oxresell.delete(BarAdminUser(), DummyMasterCredentials());
    }
}
