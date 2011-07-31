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

package com.openexchange.oauth.linkedin;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Scanner;
import junit.framework.TestCase;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.LinkedInApi;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import com.openexchange.groupware.container.Contact;
import com.openexchange.oauth.DefaultOAuthToken;
import com.openexchange.oauth.linkedin.osgi.Activator;
/**
 * {@link LinkedInConnectionTest}
 *
 * @author <a href="mailto:karsten.will@open-xchange.com">Karsten Will</a>
 */
public class LinkedInConnectionTest extends TestCase {

    private LinkedInServiceImpl linkedIn;

    private final String apiKey = "PLEASE_INSERT_VALID_KEY_HERE";
    private final String apiSecret = "PLEASE_INSERT_VALID_SECRET_HERE";

    @Override
    public void setUp(){
        Activator activator = new Activator();
//        OAuthServiceMetaDataLinkedInImpl linkedInMetadata = new OAuthServiceMetaDataLinkedInImpl();
//        activator.setLinkedInMetadata(linkedInMetadata);
        linkedIn = new LinkedInServiceImpl(activator);
        activator.setOauthService(new MockOAuthService());
    }

    @Override
    public void tearDown(){

    }

    public void testAccountCreation(){
        // This is basically scribes example
        OAuthService service = new ServiceBuilder().provider(LinkedInApi.class).apiKey(apiKey).apiSecret(apiSecret).build();

        System.out.println("=== LinkedIn's OAuth Workflow ===");
        System.out.println();

        // Obtain the Request Token
        System.out.println("Fetching the Request Token...");
        Token requestToken = service.getRequestToken();
        System.out.println("Got the Request Token!");
        System.out.println();

        DefaultOAuthToken oAuthToken = new DefaultOAuthToken();
        oAuthToken.setToken(requestToken.getToken());
        oAuthToken.setSecret(requestToken.getSecret());

        System.out.println("https://api.linkedin.com/uas/oauth/authorize?oauth_token="+oAuthToken.getToken());
        System.out.println("And paste the verifier here");
        System.out.print(">>");

        Scanner in = new Scanner(System.in);
        Verifier verifier = new Verifier(in.nextLine());
        System.out.println();

        // Trade the Request Token and Verifier for the Access Token
        System.out.println("Trading the Request Token for an Access Token...");
        Token accessToken = service.getAccessToken(requestToken, verifier);
        System.out.println("Got the Access Token!");
        System.out.println("(if your curious it looks like this: " + accessToken.getToken() + "(Token), "+accessToken.getSecret()+"(Secret) )");
        System.out.println();
    }

    public void testUsageOfExistingAccount(){
        List<Contact> contacts = linkedIn.getContacts("password",1,1,1);
        for (Contact contact : contacts){
            System.out.println(contact.getGivenName() + " " + contact.getSurName());
        }
    }

    public void testXMLParsing(){

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("/Users/karstenwill/Documents/Development/ox_projectset_workspace/com.openexchange.oauth.linkedin/local_only/linkedin.xml"), "UTF8"));
            String string = "";
            String line;
            while ((line = reader.readLine()) != null) {
                string += line + "\n";
            }
            List<Contact> contacts = linkedIn.parseIntoContacts(string);
            System.out.println("No of contacts : " + contacts.size());
            for (Contact contact : contacts){
                if (contact.getSurName().equals("Geck")){
                    System.out.println("Birthday : " + contact.getBirthday());
                    System.out.println("telephone_home1  : " + contact.getTelephoneHome1());
                    System.out.println("note  : " + contact.getNote());
                }
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
