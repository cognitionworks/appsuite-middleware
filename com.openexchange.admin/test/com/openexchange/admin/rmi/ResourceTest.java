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

package com.openexchange.admin.rmi;

import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Resource;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 *
 * @author cutmasta
 */
public class ResourceTest extends AbstractTest{
    
    private OXResourceInterface getResourceClient() throws NotBoundException, MalformedURLException, RemoteException{
        return (OXResourceInterface)Naming.lookup(getRMIHostUrl()+OXResourceInterface.RMI_NAME);
    }
    
    public void testCreate() throws Exception {
        OXResourceInterface oxres = getResourceClient();
        final int context_id = GroupTest.getContextID();
        final Context ctx = new Context(context_id);
        Resource res = getTestResourceObject("tescase-create-resource-"+System.currentTimeMillis());
        res.setId(oxres.create(ctx,res,DummyCredentials()));
        
        Resource[] srv_response = oxres.list(ctx,"*",DummyCredentials());
        if(srv_response==null){
            fail("server response is null");
        }
        boolean found_resource = false;
        for(int a = 0;a<srv_response.length;a++){
            Resource tmp = srv_response[a];
            if(tmp.getId()==res.getId()){
                assertEquals(res.getDescription(),tmp.getDescription());
                assertEquals(res.getDisplayname(),tmp.getDisplayname());
                assertEquals(res.getEmail(),tmp.getEmail());
                assertEquals(res.getName(),tmp.getName());
                found_resource = true;
            }
        }
        assertTrue("Expected to find resource with correct data",found_resource);
    }
    
    public void testChange() throws Exception {
        
        OXResourceInterface oxres = getResourceClient();
        final int context_id = GroupTest.getContextID();
        final Context ctx = new Context(context_id);
        Resource res = getTestResourceObject("tescase-create-resource-"+System.currentTimeMillis());
        res.setId(oxres.create(ctx,res,DummyCredentials()));
        
        Resource[] srv_response = oxres.list(ctx,"*",DummyCredentials());
        if(srv_response==null){
            fail("server response is null");
        }
        boolean found_resource = false;
        for(int a = 0;a<srv_response.length;a++){
            Resource tmp = srv_response[a];
            if(tmp.getId()==res.getId()){
                assertEquals(res.getDescription(),tmp.getDescription());
                assertEquals(res.getDisplayname(),tmp.getDisplayname());
                assertEquals(res.getEmail(),tmp.getEmail());
                assertEquals(res.getName(),tmp.getName());
                found_resource = true;
            }
        }
        
        assertTrue("Expected to find resource with correct data",found_resource);
        
        
        // set change data
        res.setAvailable(!res.isAvailable());
        res.setDescription(res.getDescription()+change_suffix);
        res.setDisplayname(res.getDisplayname()+change_suffix);
        res.setEmail(res.getEmail()+change_suffix);
        res.setName(res.getName()+change_suffix);
        
        // change on server
        oxres.change(ctx,res,DummyCredentials());
        
        // get resource from server and verify changed data
        Resource srv_res = oxres.get(ctx,res.getId(),DummyCredentials());        
        
        assertEquals(res.getDescription(),srv_res.getDescription());
        assertEquals(res.getDisplayname(),srv_res.getDisplayname());
        assertEquals(res.getEmail(),srv_res.getEmail());
        assertEquals(res.getName(),srv_res.getName());
        
    }
    
    public void testGet() throws Exception {
        OXResourceInterface oxres = getResourceClient();
        final int context_id = GroupTest.getContextID();
        final Context ctx = new Context(context_id);
        Resource res = getTestResourceObject("tescase-create-resource-"+System.currentTimeMillis());
        res.setId(oxres.create(ctx,res,DummyCredentials()));
        
        // get resource from server
        Resource srv_res = oxres.get(ctx,res.getId(),DummyCredentials());        
        
        assertEquals(res.getDescription(),srv_res.getDescription());
        assertEquals(res.getDisplayname(),srv_res.getDisplayname());
        assertEquals(res.getEmail(),srv_res.getEmail());
        assertEquals(res.getName(),srv_res.getName());
    }
    
    public void testDelete() throws Exception {
        OXResourceInterface oxres = getResourceClient();
        final int context_id = GroupTest.getContextID();
        final Context ctx = new Context(context_id);
        Resource res = getTestResourceObject("tescase-create-resource-"+System.currentTimeMillis());
        res.setId(oxres.create(ctx,res,DummyCredentials()));
        
        // get resource from server
        Resource srv_res = oxres.get(ctx,res.getId(),DummyCredentials());        
        
        assertEquals(res.getDescription(),srv_res.getDescription());
        assertEquals(res.getDisplayname(),srv_res.getDisplayname());
        assertEquals(res.getEmail(),srv_res.getEmail());
        assertEquals(res.getName(),srv_res.getName());
        
        
        // delete resource
        oxres.delete(ctx,res.getId(),DummyCredentials());
        
        // try to get resource again, this MUST fail
        try{
            srv_res = oxres.get(ctx,res.getId(),DummyCredentials());   
            fail("Expected that the resource was deleted!");
        }catch(InvalidDataException idv){  }
        
    }
    
    public void testList() throws Exception {
       OXResourceInterface oxres = getResourceClient();
        final int context_id = GroupTest.getContextID();
        final Context ctx = new Context(context_id);
        Resource res = getTestResourceObject("tescase-create-resource-"+System.currentTimeMillis());
        res.setId(oxres.create(ctx,res,DummyCredentials()));
        
        Resource[] srv_response = oxres.list(ctx,"*",DummyCredentials());
        if(srv_response==null){
            fail("server response was null");
        }
        assertTrue("Expected list size > 0 ",srv_response.length>0);
    }
    
    public static Resource getTestResourceObject(String name){
        Resource res = new Resource();
        res.setAvailable(true);
        res.setDescription("description of resource "+name);
        res.setDisplayname("displayname of resource "+name);
        res.setEmail("resource-email-"+name+"@"+TEST_DOMAIN);
        res.setName(name);
        return res;
    }
    
    
}
