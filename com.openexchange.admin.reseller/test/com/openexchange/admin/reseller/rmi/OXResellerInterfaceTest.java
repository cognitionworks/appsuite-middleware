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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;

import org.junit.Test;

import com.openexchange.admin.reseller.rmi.dataobjects.ResellerAdmin;
import com.openexchange.admin.reseller.rmi.dataobjects.Restriction;
import com.openexchange.admin.reseller.rmi.exceptions.OXResellerException;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.exceptions.ContextExistsException;
import com.openexchange.admin.rmi.exceptions.DatabaseUpdateException;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;
import com.openexchange.admin.rmi.exceptions.PoolException;
import com.openexchange.admin.rmi.exceptions.StorageException;

public class OXResellerInterfaceTest extends OXResellerAbstractTest {

    @Test
    public void testCreate() throws MalformedURLException, RemoteException, NotBoundException, InvalidDataException, InvalidCredentialsException, StorageException, OXResellerException {
        final Credentials creds = DummyMasterCredentials();

        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);
        
        ResellerAdmin adm = oxresell.create(TestAdminUser(), creds);
        ResellerAdmin admch = oxresell.create(TestAdminUser(TESTCHANGEUSER,"Test Change User"), creds);

        System.out.println(adm);
        
        assertNotNull("creation of ResellerAdmin failed",adm);
        assertNotNull("creation of ResellerAdmin failed",admch);
        assertTrue("creation of ResellerAdmin failed",adm.getId() > 0);
        assertTrue("creation of ResellerAdmin failed",admch.getId() > 0);
    }

    @Test
    public void testCreateWithRestrictions() throws MalformedURLException, RemoteException, NotBoundException, InvalidDataException, InvalidCredentialsException, StorageException, OXResellerException {
        final Credentials creds = DummyMasterCredentials();

        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);

        for(final String user : new String[]{ TESTRESTRICTIONUSER, TESTRESTCHANGERICTIONUSER} ) {
            ResellerAdmin adm = TestAdminUser(user,"Test Restriction User");
            HashSet<Restriction> res = new HashSet<Restriction>();
            res.add(MaxContextRestriction());
            res.add(MaxContextQuotaRestriction());
            adm.setRestrictions(res);
            adm = oxresell.create(adm, creds);

            System.out.println(adm);

            assertNotNull("creation of ResellerAdmin failed",adm);
            assertTrue("creation of ResellerAdmin failed",adm.getId() > 0);
        }
    }

    @Test
    public void testChangeWithRestrictions() throws MalformedURLException, RemoteException, NotBoundException, InvalidDataException, InvalidCredentialsException, StorageException, OXResellerException {
        final Credentials creds = DummyMasterCredentials();

        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);

        ResellerAdmin adm = oxresell.getData(TestAdminUser(TESTRESTCHANGERICTIONUSER), creds);
        Restriction r = getRestrictionByName(Restriction.MAX_OVERALL_CONTEXT_QUOTA_PER_SUBADMIN, adm.getRestrictions());
        assertNotNull("Restriction Restriction.MAX_CONTEXT_QUOTA not found",r);
        r.setValue("2000");
        oxresell.change(adm, creds);
        
        adm = oxresell.getData(TestAdminUser(TESTRESTCHANGERICTIONUSER), creds);
        r = getRestrictionByName(Restriction.MAX_OVERALL_CONTEXT_QUOTA_PER_SUBADMIN, adm.getRestrictions());

        assertNotNull("Restriction Restriction.MAX_CONTEXT_QUOTA not found",r);
        assertEquals("Change Restriction value failed","2000", r.getValue());
    }

    @Test
    public void testChange() throws MalformedURLException, RemoteException, NotBoundException, InvalidDataException, StorageException, OXResellerException, InvalidCredentialsException {
        final Credentials creds = DummyMasterCredentials();

        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);
        
        ResellerAdmin adm = new ResellerAdmin(TESTCHANGEUSER);
        final String newdisp = "New Display name";
        adm.setDisplayname(newdisp);
        
        oxresell.change(adm, creds);
        
        ResellerAdmin chadm = oxresell.getData(new ResellerAdmin(TESTCHANGEUSER), creds);
        
        assertEquals("getData must return changed Displayname",adm.getDisplayname(), chadm.getDisplayname());
    }

    @Test
    public void testChangeName() throws MalformedURLException, RemoteException, NotBoundException, InvalidDataException, StorageException, OXResellerException, InvalidCredentialsException {
        final Credentials creds = DummyMasterCredentials();

        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);
        
        
        ResellerAdmin adm = oxresell.getData(new ResellerAdmin(TESTCHANGEUSER), creds);
        adm.setName(CHANGEDNAME);
        oxresell.change(adm, creds);
        ResellerAdmin newadm = new ResellerAdmin();
        newadm.setId(adm.getId());
        ResellerAdmin chadm = oxresell.getData(newadm, creds);
        assertEquals("getData must return changed name",adm.getName(), chadm.getName());
    }

    @Test(expected=StorageException.class)
    public void testChangeNameWithoutID() throws MalformedURLException, RemoteException, NotBoundException, InvalidDataException, StorageException, OXResellerException, InvalidCredentialsException {
        final Credentials creds = DummyMasterCredentials();

        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);
        
        ResellerAdmin adm = new ResellerAdmin();
        adm.setName(CHANGEDNAME+"new");
        oxresell.change(adm, creds);
    }

    @Test
    public void testGetData() throws MalformedURLException, RemoteException, NotBoundException, InvalidDataException, InvalidCredentialsException, StorageException, PoolException, SQLException, OXResellerException {
        final Credentials creds = DummyMasterCredentials();
        final ResellerAdmin adm = TestAdminUser();
        
        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);
        
        final ResellerAdmin dbadm = oxresell.getData(new ResellerAdmin(TESTUSER), creds);
        
        assertEquals("getData returned wrong data",adm.getName(), dbadm.getName());
        assertEquals("getData returned wrong data",adm.getDisplayname(), dbadm.getDisplayname());
    }
    
    @Test
    public void testGetDataWithRestrictions() throws MalformedURLException, RemoteException, NotBoundException, InvalidDataException, InvalidCredentialsException, StorageException, PoolException, SQLException, OXResellerException {
        final Credentials creds = DummyMasterCredentials();
        final ResellerAdmin adm = TestAdminUser(TESTRESTRICTIONUSER,"Test Restriction User");
        
        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);
        
        final ResellerAdmin dbadm = oxresell.getData(adm, creds);
        
        HashSet<Restriction> res = dbadm.getRestrictions();
        assertNotNull("ResellerAdmin must contain Restrictions",res);

        boolean foundmaxctx = getRestrictionByName(Restriction.MAX_CONTEXT_PER_SUBADMIN, res) == null ? false : true;
        boolean foundmaxctxquota = getRestrictionByName(Restriction.MAX_OVERALL_CONTEXT_QUOTA_PER_SUBADMIN, res) == null ? false : true;

        assertTrue(MaxContextQuotaRestriction().getName() + " must be contained in ResellerAdmin",foundmaxctx);
        assertTrue(MaxContextRestriction().getName() + " must be contained in ResellerAdmin",foundmaxctxquota);
        assertEquals("getData returned wrong data",adm.getName(), dbadm.getName());
        assertEquals("getData returned wrong data",adm.getDisplayname(), dbadm.getDisplayname());
    }

    @Test
    public void testList() throws MalformedURLException, RemoteException, NotBoundException, InvalidDataException, StorageException, InvalidCredentialsException {
        final Credentials creds = DummyMasterCredentials();
        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);
        
        ResellerAdmin[] res = oxresell.list("*", creds);
        for(final ResellerAdmin adm : res) {
            System.out.println(adm);
        }
        assertEquals("list must return three entries",4, res.length);
    }

    @Test
    public void testApplyRestrictionsToContext() throws MalformedURLException, RemoteException, NotBoundException, StorageException, InvalidCredentialsException, InvalidDataException, ContextExistsException, NoSuchContextException, DatabaseUpdateException, OXResellerException {
        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);

        int ctxid=234242;
        for(final Credentials creds : new Credentials[]{DummyMasterCredentials(), TestUserCredentials()} ) {
            final Context ctx = createContext(ctxid++, creds);

            HashSet<Restriction> res = new HashSet<Restriction>();
            res.add(MaxUserPerContextRestriction());

            oxresell.applyRestrictionsToContext(res, ctx, creds);
        }
    }

    @Test
    public void testGetRestrictionsFromContext() throws MalformedURLException, RemoteException, NotBoundException, StorageException, InvalidCredentialsException, InvalidDataException, ContextExistsException, NoSuchContextException, DatabaseUpdateException, OXResellerException {
        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);

        int ctxid=234242;
        for(final Credentials creds : new Credentials[]{DummyMasterCredentials(), TestUserCredentials()} ) {
            final Context ctx = new Context(ctxid++);

            HashSet<Restriction> res = oxresell.getRestrictionsFromContext(ctx, creds);
            assertNotNull("Context restrictions must not be null",res);
            assertEquals("Context restrictions must contain one restriction",1, res.size());
            assertEquals("Restriction value does not match expected value", MaxUserPerContextRestriction().getValue(), res.toArray(new Restriction[res.size()])[0].getValue());
            deleteContext(ctx, creds);
        }
    }

    @Test
    public void testDeleteContextOwningSubadmin() throws MalformedURLException, RemoteException, NotBoundException, StorageException, InvalidCredentialsException, InvalidDataException, ContextExistsException, NoSuchContextException, DatabaseUpdateException, OXResellerException {
        final Credentials creds = DummyMasterCredentials();
        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);

        oxresell.create(TestAdminUser("owned"), creds);
        final Context ctx = createContext(12345, new Credentials("owned","secret"));
        
        boolean deleteFailed = false;
        try {
            oxresell.delete(TestAdminUser("owned"), creds);
        } catch (OXResellerException e) {
            deleteFailed = true;
        }
        assertTrue("deletion of ResellerAdmin must fail",deleteFailed);
        
        deleteContext(ctx, new Credentials("owned","secret"));
        oxresell.delete(TestAdminUser("owned"), creds);
    }

    @Test
    public void testDelete() throws MalformedURLException, RemoteException, NotBoundException, InvalidDataException, StorageException, OXResellerException, InvalidCredentialsException {
        final Credentials creds = DummyMasterCredentials();

        final OXResellerInterface oxresell = (OXResellerInterface)Naming.lookup(getRMIHostUrl() + OXResellerInterface.RMI_NAME);

        oxresell.delete(TestAdminUser(), creds);
        oxresell.delete(new ResellerAdmin(CHANGEDNAME), creds);
        for(final String user : new String[]{ TESTRESTRICTIONUSER, TESTRESTCHANGERICTIONUSER} ) {
            oxresell.delete(TestAdminUser(user), creds);
        }
    }

}
