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

package com.openexchange.ajax.session;

import com.openexchange.exception.OXException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.httpclient.Cookie;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.groupware.calendar.TimeTools;
import com.openexchange.test.json.JSONAssertion;

/**
 * Tests the login. This assumes autologin is allowed and cookie timeout is one week.
 * @author <a href="mailto:francisco.laguna@open-xchange.org">Francisco Laguna</a>
 */
public class LoginTest extends AbstractLoginTest {

    public LoginTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        createClient();
    }

    @Override
    protected void tearDown() throws Exception {
        // Nothing to do.
    }
    
    // Success Cases

    public void testSuccessfulLoginReturnsSession() throws Exception {
        assertResponseContains("session");
    }
    
    public void testSuccessfulLoginReturnsRandom() throws Exception {
        assertResponseContains("random");
    }
  
    public void testSuccessfulLoginSetsSecretCookie() throws Exception {
        rawLogin(USER1);
        Cookie[] cookies = currentClient.getClient().getState().getCookies();
        boolean found = false;
        List<String> cookieNames = new ArrayList<String>(cookies.length);
        for (Cookie cookie : cookies) {
            String name = cookie.getName();
            cookieNames.add(name);
            found = found || name.startsWith("open-xchange-secret");
        }
        
        assertTrue("Missing secret cookie: "+cookieNames.toString(), found);
    }
    
    public void testSuccessfulLoginDoesNotSetSessionCookie() throws Exception {
        // Note: This will fail a while, until UI uses new store action and we can get rid of the session cookie after login
        rawLogin(USER1);
        Cookie[] cookies = currentClient.getClient().getState().getCookies();
        boolean found = false;
        List<String> cookieNames = new ArrayList<String>(cookies.length);
        for (Cookie cookie : cookies) {
            String name = cookie.getName();
            cookieNames.add(name);
            found = found || name.startsWith("open-xchange-session");
        }
        
        assertFalse("Found session cookie, but shouldn't have: "+cookieNames.toString(), found);
    }
    
    public void testSecretCookiesDifferPerClientID() throws Exception {
        String[] credentials = credentials(USER1);
        
        inModule("login");
        
        raw("login", 
                "name", credentials[0],
                "password", credentials[1],
                "client" , "testclient1"
                 );
        
        raw("login", 
            "name", credentials[0],
            "password", credentials[1],
            "client" , "testclient2"
             );
    
        Cookie[] cookies = currentClient.getClient().getState().getCookies();
        int counter = 0;
        List<String> cookieNames = new ArrayList<String>(cookies.length);
        for (Cookie cookie : cookies) {
            String name = cookie.getName();
            cookieNames.add(name);
            if(name.startsWith("open-xchange-secret")) {
                counter++;
            }
        }
        
        assertTrue("Missing secret cookie: "+cookieNames.toString(), counter == 2);
        
    }
    
    public void testSecretCookieLifetimeIsLongerThanADay() throws Exception {
        rawLogin(USER1);
        Cookie[] cookies = currentClient.getClient().getState().getCookies();
        List<String> cookieNames = new ArrayList<String>(cookies.length);
        for (Cookie cookie : cookies) {
            String name = cookie.getName();
            cookieNames.add(name);
            if(name.startsWith("open-xchange-secret")) {
                assertNotNull(cookie.getExpiryDate());
                Date tomorrow = TimeTools.D("tomorrow");
                assertTrue(cookie.getExpiryDate().after(tomorrow));
              }
        }
    }
    
    public void testSuccessfulLoginAllowsSubsequentRequests() throws Exception {
        as(USER1);
        inModule("quota"); call("filestore"); // Send some request.

        assertNoError();
    }
    
    public void testRefreshSecretActionResetsSecretCookieLifetime() throws Exception {
        rawLogin(USER1);
        Date oldCookie = null, newCookie = null;

        Cookie[] cookies = currentClient.getClient().getState().getCookies();

        for(int i = 0; i < cookies.length; i++)
        	if(cookies[i].getName().startsWith("open-xchange-secret"))
        		oldCookie = cookies[i].getExpiryDate();

        Thread.sleep(1000);
        raw(AJAXServlet.ACTION_REFRESH_SECRET, AJAXServlet.PARAMETER_SESSION, rawResponse.getString(AJAXServlet.PARAMETER_SESSION));
        cookies = currentClient.getClient().getState().getCookies();
        
        for(int i = 0; i < cookies.length; i++)
        	if(cookies[i].getName().startsWith("open-xchange-secret"))
        		newCookie = cookies[i].getExpiryDate();
        
        assertNotNull("Precondition: Should find secret cookie after renewal", newCookie);
        assertNotNull("Precondition: Should find secret cookie first", oldCookie);
        
        assertTrue("Refreshed secret cookie should have newer expiry date", newCookie.compareTo(oldCookie) > 0);
    }
    
    // Error Cases
    
    public void testWrongCredentials() throws Exception {
        inModule("login"); 
        call("login", 
               "name", "foo",
               "password", "bar");
 
        assertError();
    }
    
    public void testNonExistingSessionIDOnSubsequentRequests() throws Exception {
        as(USER1);
        inModule("quota"); call("filestore", "session", "1234567"); // Send some request, and override the sessionID.
        
        assertError();
    }
    
    public void testSessionIDAndSecretMismatch() throws Exception {
        as(USER1);
        String sessionID = currentClient.getSessionID();
        
        as(USER2);
        inModule("quota"); call("filestore", "session", sessionID); // Send some request with user 1 session. Secrets will differ.
        
        assertError();
    }
    
    private void assertResponseContains(String key) throws Exception {
        rawLogin(USER1);
        assertRaw(new JSONAssertion().isObject().hasKey(key));
    }
    
    private void rawLogin(String user) throws Exception{
        String[] credentials = credentials(user);
        
        inModule("login");
        
        raw("login", 
                "name", credentials[0],
                "password", credentials[1]
                 );
        
    }

}
