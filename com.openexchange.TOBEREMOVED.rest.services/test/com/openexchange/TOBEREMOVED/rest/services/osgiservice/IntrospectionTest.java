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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.TOBEREMOVED.rest.services.osgiservice;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import com.openexchange.TOBEREMOVED.rest.services.OXRESTRoute;
import com.openexchange.TOBEREMOVED.rest.services.OXRESTService;
import com.openexchange.TOBEREMOVED.rest.services.Response;
import com.openexchange.TOBEREMOVED.rest.services.annotations.*;
import com.openexchange.TOBEREMOVED.rest.services.internal.OXRESTServiceWrapper;
import com.openexchange.TOBEREMOVED.rest.services.osgiservice.IntrospectingServiceFactory;
import com.openexchange.TOBEREMOVED.rest.services.osgiservice.ReflectiveServiceWrapper;
import com.openexchange.exception.OXException;
import static org.junit.Assert.*;


/**
 * {@link IntrospectionTest}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class IntrospectionTest {
    @ROOT("/root")
    public static class TestService extends OXRESTService<Object> {
        private boolean afterCalled;
        private boolean beforeCalled;

        @GET("/get")
        public String get() {
            return "get";
        }
        
        @PUT("/put")
        public String put() {
            return "put";
        }
        
        @POST("/post")
        public String post() {
            return "post";
        }
        
        @PATCH("/patch")
        public String patch() {
            return "patch";
        }
        
        @DELETE("/delete")
        public String delete() {
            return "delete";
        }
        
        @OPTIONS("/options")
        public String options() {
            return "options";
        }
        
        @LINK("/link")
        public String link() {
            return "link";
        }
        
        @UNLINK("/unlink")
        public String unlink() {
            return "unlink";
        }
        
        @Override
        public void before() {
            this.beforeCalled = true;
        }
        
        @Override
        public void after() {
            this.afterCalled = true;
        }
        
        public boolean wereBeforeAndAfterCalled() {
            return beforeCalled && afterCalled;
        }
        
        public Object getContext() {
            return context;
        }
    }
    
    @ROOT("/controlFlow")
    public static class TestControlFlow extends OXRESTService<Void> {
        private boolean beforeHalt;
        private boolean afterHalt;
        private boolean beforeCalled;
        private boolean afterCalled;

        @GET("/testHalt")
        public void testHalt() {
            beforeHalt = true;
            halt();
            afterHalt = true;
        }
        
        public boolean wasHaltedCorrectly() {
            return beforeHalt && !afterHalt;
        }

        @Override
        public void before() {
            this.beforeCalled = true;
        }
        
        @Override
        public void after() {
            this.afterCalled = true;
        }
        
        public boolean wereBeforeAndAfterCalled() {
            return beforeCalled && afterCalled;
        }
    }
    
    @Test 
    public void testRoot() {
        IntrospectingServiceFactory<Object> factory = new IntrospectingServiceFactory<Object>(TestService.class, null, null);
        assertEquals("/root", factory.getRoot());
    }
    
    @Test
    public void testListRoutes() {
        IntrospectingServiceFactory<Object> factory = new IntrospectingServiceFactory<Object>(TestService.class, null, null);
        List<OXRESTRoute> routes = factory.getRoutes();
        assertEquals(8, routes.size());
        
        for(String method: Arrays.asList("get", "put", "post", "patch", "delete", "options", "link", "unlink")) {
            boolean found = false;
            for(OXRESTRoute route: routes) {
                if (route.getMethod().equalsIgnoreCase(method) && route.getPath().equals("/" + method)) {
                    found = true;
                }
            }
            assertTrue("Missing route for method "+ method, found);
        }
    }
    
    @Test
    public void testMethods() throws OXException {
        IntrospectingServiceFactory<Object> factory = new IntrospectingServiceFactory<Object>(TestService.class, null, null);
        List<OXRESTRoute> routes = factory.getRoutes();
        for (OXRESTRoute route : routes) {
            OXRESTServiceWrapper wrapper = factory.newWrapper(route.match(route.getMethod(), route.getPath()));
            Response response = wrapper.execute();
            assertEquals(route.getPath().substring(1), response.getBody().iterator().next());
            ReflectiveServiceWrapper refWrapper = (ReflectiveServiceWrapper) wrapper;
            OXRESTService delegate = refWrapper.getDelegate();
            TestService testService = (TestService) delegate;
            
            assertTrue(testService.wereBeforeAndAfterCalled());
        }
    }
    
    @Test
    public void testPopulatesContext() {
        Object object = new Object();
        IntrospectingServiceFactory<Object> factory = new IntrospectingServiceFactory<Object>(TestService.class, null, object);
        OXRESTRoute route = factory.getRoutes().get(0);
        OXRESTServiceWrapper wrapper = factory.newWrapper(route.match(route.getMethod(), route.getPath()));
        ReflectiveServiceWrapper refWrapper = (ReflectiveServiceWrapper) wrapper;
        OXRESTService delegate = refWrapper.getDelegate();
        TestService testService = (TestService) delegate;
        assertEquals(object, testService.getContext());
    }
    
    @Test
    public void testControlFlow() throws OXException {
        IntrospectingServiceFactory<Void> factory = new IntrospectingServiceFactory<Void>(TestControlFlow.class, null, null);
        OXRESTRoute route = factory.getRoutes().get(0);
        OXRESTServiceWrapper wrapper = factory.newWrapper(route.match(route.getMethod(), route.getPath()));
        wrapper.execute();
        ReflectiveServiceWrapper refWrapper = (ReflectiveServiceWrapper) wrapper;
        OXRESTService delegate = refWrapper.getDelegate();
        TestControlFlow tcf = (TestControlFlow) delegate;
        
        assertTrue(tcf.wereBeforeAndAfterCalled());
        assertTrue(tcf.wasHaltedCorrectly());
    }
}
