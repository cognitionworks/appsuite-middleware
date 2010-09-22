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

import java.security.GeneralSecurityException;
import java.util.List;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ho.yaml.Yaml;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.CrawlerCookieManager;
import com.gargoylesoftware.htmlunit.CrawlerCookieSpec;
import com.gargoylesoftware.htmlunit.CrawlerCookieSpecWithQuirkyQuotes;
import com.gargoylesoftware.htmlunit.CrawlerWebConnection;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.openexchange.subscribe.Subscription;
import com.openexchange.subscribe.SubscriptionErrorMessage;
import com.openexchange.subscribe.SubscriptionException;
import com.openexchange.subscribe.crawler.internal.LoginStep;
import com.openexchange.subscribe.crawler.internal.LogoutStep;
import com.openexchange.subscribe.crawler.internal.NeedsLoginStepString;
import com.openexchange.subscribe.crawler.internal.Step;
import com.openexchange.subscribe.crawler.osgi.Activator;

/**
 * A crawling workflow. This holds the individual Steps and the session information (WebClient instance).
 * 
 * @author <a href="mailto:karsten.will@open-xchange.com">Karsten Will</a>
 */
public class Workflow {

    private List<Step> steps;

    private String loginStepString;

    private Subscription subscription;

    private boolean useThreadedRefreshHandler;

    private static final Log LOG = LogFactory.getLog(Workflow.class);

    private Activator activator;

    private boolean enableJavascript;

    private boolean debuggingEnabled = false;

    private boolean mobileUserAgent = false;
    
    private boolean quirkyCookieQuotes;

    public Workflow() {

    }

    public Workflow(final List<Step> steps) {
        this.steps = steps;
        useThreadedRefreshHandler = false;
    }

    // Convenience method for setting username and password after the workflow was created
    public Object[] execute(final String username, final String password) throws SubscriptionException {
        for (final Step currentStep : steps) {
            if (debuggingEnabled) {
                currentStep.setDebuggingEnabled(true);
            }
            if (currentStep instanceof LoginStep) {
                ((LoginStep) currentStep).setUsername(username);
                ((LoginStep) currentStep).setPassword(password);
                loginStepString = Yaml.dump(currentStep);
            }
            if (currentStep instanceof NeedsLoginStepString && null != loginStepString) {
                ((NeedsLoginStepString) currentStep).setLoginStepString(loginStepString);
            }
        }
        return execute();
    }

    public Object[] execute() throws SubscriptionException {

        // emulate a specific browser
        // final WebClient webClient = new WebClient(BrowserVersion.FIREFOX_3);
        BrowserVersion browser = BrowserVersion.FIREFOX_3;
        if (mobileUserAgent) {
            browser.setUserAgent("Mozilla/5.0 (iPhone; U; CPU iPhone OS 3_0 like Mac OS X; en-us) AppleWebKit/528.18 (KHTML, like Gecko) Version/4.0 Mobile/7A341 Safari/528.16");
        }
        final WebClient webClient = new WebClient(browser);

        // use a custom CookiePolicy to be more lenient and thereby work with more websites
        CrawlerWebConnection crawlerConnection = new CrawlerWebConnection(webClient);
        // the same applies to SSL: Be as lenient, and thereby as compatible, as possible
        try {
            webClient.setUseInsecureSSL(true);
        } catch (GeneralSecurityException e) {
            LOG.error(e);
        }
        // ... and to javascript as well
        webClient.setThrowExceptionOnScriptError(false);
        if (quirkyCookieQuotes) {crawlerConnection.setQuirkyCookieQuotes(true);}
        CookiePolicy.registerCookieSpec("crawler-special", CrawlerCookieSpec.class);
        CookiePolicy.registerCookieSpec("crawler-special-qq", CrawlerCookieSpecWithQuirkyQuotes.class);
        webClient.setCookieManager(new CrawlerCookieManager());
        // System.out.println(CookiePolicy.getCookieSpec("crawler-special"));

        webClient.setWebConnection(crawlerConnection);
        // Javascript is disable by default for security reasons but may be activated for single crawlers
        webClient.setJavaScriptEnabled(enableJavascript);
        webClient.setTimeout(60000);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        if (useThreadedRefreshHandler) {
            webClient.setRefreshHandler(new ThreadedRefreshHandler());
        }
        try {

            Step previousStep = null;
            Object result = null;

            for (final Step currentStep : steps) {
                if (previousStep != null) {
                    currentStep.setInput(previousStep.getOutput());
                }
                currentStep.setWorkflow(this);
                LOG.info("Current Step : " + currentStep.getClass());
                currentStep.execute(webClient);
                previousStep = currentStep;
                // if step fails try it 2 more times before crying foul
                if (!currentStep.executedSuccessfully()) {
                    currentStep.execute(webClient);
                    if (!currentStep.executedSuccessfully()) {
                        currentStep.execute(webClient);
                        if (!currentStep.executedSuccessfully()) {
                            throw SubscriptionErrorMessage.COMMUNICATION_PROBLEM.create();
                        }
                    }                    
                }
                if (!(currentStep instanceof LogoutStep)) {
                    result = currentStep.getOutput();
                }
            }        

            webClient.closeAllWindows();
            return (Object[]) result;
        } 
        catch (NullPointerException e) {
            LOG.error(e);
            throw SubscriptionErrorMessage.COMMUNICATION_PROBLEM.create();
        }
        catch (ClassCastException e) {
            LOG.error(e);
            throw SubscriptionErrorMessage.COMMUNICATION_PROBLEM.create();
        }
        finally {
            MultiThreadedHttpConnectionManager manager = (MultiThreadedHttpConnectionManager) crawlerConnection.getHttpClient().getHttpConnectionManager();
            manager.shutdown();
        }
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(final List<Step> steps) {
        this.steps = steps;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(final Subscription subscription) {
        this.subscription = subscription;
    }

    public String getLoginStepString() {
        return loginStepString;
    }

    public void setLoginStepString(final String loginStepString) {
        this.loginStepString = loginStepString;
    }

    public boolean isUseThreadedRefreshHandler() {
        return useThreadedRefreshHandler;
    }

    public void setUseThreadedRefreshHandler(final boolean useThreadedRefreshHandler) {
        this.useThreadedRefreshHandler = useThreadedRefreshHandler;
    }

    public Activator getActivator() {
        return activator;
    }

    public void setActivator(Activator activator) {
        this.activator = activator;
    }

    public boolean isEnableJavascript() {
        return enableJavascript;
    }

    public void setEnableJavascript(boolean enableJavascript) {
        this.enableJavascript = enableJavascript;
    }

    public boolean isDebuggingEnabled() {
        return debuggingEnabled;
    }

    public void setDebuggingEnabled(boolean debuggingEnabled) {
        this.debuggingEnabled = debuggingEnabled;
    }

    public boolean isMobileUserAgent() {
        return mobileUserAgent;
    }

    public void setMobileUserAgent(boolean mobileUserAgent) {
        this.mobileUserAgent = mobileUserAgent;
    }

    
    public boolean isQuirkyCookieQuotes() {
        return quirkyCookieQuotes;
    }

    
    public void setQuirkyCookieQuotes(boolean quirkyCookieQuotes) {
        this.quirkyCookieQuotes = quirkyCookieQuotes;
    }
    
    

}
