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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.subscribe.crawler;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.client.OAuthClient;
import net.oauth.client.httpclient4.HttpClient4;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.openexchange.subscribe.SubscriptionException;
import com.openexchange.subscribe.crawler.internal.AbstractStep;
import com.openexchange.subscribe.crawler.internal.LoginStep;

/**
 * {@link StringByOAuthRequestStep}
 * 
 * @author <a href="mailto:karsten.will@open-xchange.com">Karsten Will</a>
 */
public class StringByOAuthRequestStep extends AbstractStep<String, Object> implements LoginStep {

    private String username, password, consumerSecret, consumerKey, requestUrl, authorizationUrl, accessUrl, callbackUrl;

    private String requestToken, tokenSecret;
    private String nameOfUserField, nameOfPasswordField;
    private String accessToken, apiRequest;

    private OAuthAccessor oAuthAccessor;

    private static final Log LOG = LogFactory.getLog(StringByOAuthRequestStep.class);

    public StringByOAuthRequestStep() {
    }

    public StringByOAuthRequestStep(String username, String password, String consumerSecret, String consumerKey, String requestUrl, String authorizationUrl, String accessUrl, String callbackUrl, String nameOfUserField, String nameOfPasswordField, String apiRequest) {
        this.username = username;
        this.password = password;
        this.consumerSecret = consumerSecret;
        this.consumerKey = consumerKey;
        this.requestUrl = requestUrl;
        this.authorizationUrl = authorizationUrl;
        this.accessUrl = accessUrl;
        this.callbackUrl = callbackUrl;
        this.nameOfPasswordField = nameOfPasswordField;
        this.nameOfUserField = nameOfUserField;
        this.apiRequest = apiRequest;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.subscribe.crawler.internal.AbstractStep#execute(com.gargoylesoftware.htmlunit.WebClient)
     */
    @Override
    public void execute(WebClient webClient) throws SubscriptionException {
        
        // Request the OAuth token
        OAuthClient client = new OAuthClient(new HttpClient4());
        try {
            oAuthAccessor = createOAuthAccessor();
            client.getRequestToken(oAuthAccessor);
            requestToken = oAuthAccessor.requestToken;
            tokenSecret = oAuthAccessor.tokenSecret;

            LOG.info("Successfully requested OAuth token");
        } catch (IOException e) {
            LOG.error(e);
        } catch (OAuthException e) {
            LOG.error(e);
        } catch (URISyntaxException e) {
            LOG.error(e);
        } catch (NullPointerException e) {
            LOG.error(e);
        }

        // Authorize the request 
        String verifier = "";
        try {
            oAuthAccessor = createOAuthAccessor();
            Properties paramProps = new Properties();
            paramProps.setProperty("application_name", "Open-Xchange Contact Aggregator");
            paramProps.setProperty("oauth_token", requestToken);
            OAuthMessage response = sendRequest(paramProps, oAuthAccessor.consumer.serviceProvider.userAuthorizationURL);
            LOG.info("Successfully requested authorization-url: "+response.URL);   
            
            // Fill out form / confirm the access otherwise
            LoginPageByFormActionRegexStep authorizeStep = new LoginPageByFormActionRegexStep("", response.URL,  username, password, "/uas/oauth/authorize/submit", nameOfUserField, nameOfPasswordField, ".*", 1, "");
            authorizeStep.execute(webClient);
            HtmlPage pageWithVerifier = authorizeStep.getOutput();
            String pageString2 = pageWithVerifier.getWebResponse().getContentAsString();
            LOG.debug("Page contains the verifier : " + pageString2.contains("access-code")); 
            LOG.debug("Cookie-Problem : " + pageString2.contains("Please make sure you have cookies"));
            
            // get the verifier
            Pattern pattern = Pattern.compile("access-code\">([0-9]*)<");
            Matcher matcher = pattern.matcher(pageString2);
            if (matcher.find() && matcher.groupCount() == 1){
                verifier = matcher.group(1);
                LOG.info("Request authorized, verifier found.");
            } else {
                LOG.error("Verifier not found");
            }
            LOG.debug("This is the verifier : " + verifier);
            //openPageInBrowser(pageWithVerifier);
        } catch (IOException e) {
            LOG.error(e);
        } catch (URISyntaxException e) {
            LOG.error(e);
        } catch (OAuthException e) {
            LOG.error(e);
        }

        
        // Access and confirm using the verifier
        try {
            Properties paramProps = new Properties();
            paramProps.setProperty("oauth_token", requestToken);
            //not in OAuth-Spec and maybe specific to linkedin
            paramProps.setProperty("oauth_verifier", verifier);
            OAuthMessage response = sendRequest(paramProps, accessUrl);
            accessToken = response.getParameter("oauth_token");
            tokenSecret = response.getParameter("oauth_token_secret");
            LOG.info("Accessed and conformed using the verifier");
        } catch (IOException e) {
            LOG.error(e);
        } catch (URISyntaxException e) {
            LOG.error(e);
        } catch (OAuthException e) {
            LOG.error(e);
        }
        
        // Execute an API-Request (fully logged in now)        
        try {
            Properties paramProps = new Properties();
            paramProps.setProperty("oauth_token", accessToken);

            OAuthMessage response = sendRequest(paramProps, apiRequest);
            String result = response.readBodyAsString();
            LOG.info("Successfully executed an API-Request");
            executedSuccessfully = true;
            LOG.debug("This is the result of the whole operation : " + result);
            output = result;
        } catch (IOException e) {
            LOG.error(e);
        } catch (URISyntaxException e) {
            LOG.error(e);
        } catch (OAuthException e) {
            LOG.error(e);
        }

        
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.subscribe.crawler.internal.LoginStep#getBaseUrl()
     */
    public String getBaseUrl() {
        return "";
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.subscribe.crawler.internal.LoginStep#setPassword(java.lang.String)
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.subscribe.crawler.internal.LoginStep#setUsername(java.lang.String)
     */
    public void setUsername(String username) {
        this.username = username;
    }

    private OAuthAccessor createOAuthAccessor() {
        OAuthServiceProvider provider = new OAuthServiceProvider(requestUrl, authorizationUrl, accessUrl);
        OAuthConsumer consumer = new OAuthConsumer(callbackUrl, consumerKey, consumerSecret, provider);
        return new OAuthAccessor(consumer);
    }

    private OAuthMessage sendRequest(Map map, String url) throws IOException, URISyntaxException, OAuthException {
        List<Map.Entry> params = new ArrayList<Map.Entry>();
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry p = (Map.Entry) it.next();
            params.add(new OAuth.Parameter((String) p.getKey(), (String) p.getValue()));
        }
        OAuthAccessor accessor = createOAuthAccessor();
        accessor.tokenSecret = tokenSecret;
        OAuthClient client = new OAuthClient(new HttpClient4());
        return client.invoke(accessor, "GET", url, params);
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public void setAccessUrl(String accessUrl) {
        this.accessUrl = accessUrl;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getRequestToken() {
        return requestToken;
    }

    public void setRequestToken(String requestToken) {
        this.requestToken = requestToken;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    
    public String getNameOfUserField() {
        return nameOfUserField;
    }

    
    public void setNameOfUserField(String nameOfUserField) {
        this.nameOfUserField = nameOfUserField;
    }

    
    public String getNameOfPasswordField() {
        return nameOfPasswordField;
    }

    
    public void setNameOfPasswordField(String nameOfPasswordField) {
        this.nameOfPasswordField = nameOfPasswordField;
    }

    
    public String getApiRequest() {
        return apiRequest;
    }

    
    public void setApiRequest(String apiRequest) {
        this.apiRequest = apiRequest;
    }

    
    
}
