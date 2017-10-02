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

package com.openexchange.ajax.framework;

import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import com.google.code.tempusfugit.concurrency.ConcurrentTestRunner;
import com.google.code.tempusfugit.concurrency.annotations.Concurrent;
import com.openexchange.ajax.chronos.EnhancedApiClient;
import com.openexchange.exception.OXException;
import com.openexchange.test.pool.TestContext;
import com.openexchange.test.pool.TestContextPool;
import com.openexchange.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import com.openexchange.testing.httpclient.modules.LoginApi;

/**
 *
 * {@link AbstractAPIClientSession}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.0
 */
@RunWith(ConcurrentTestRunner.class)
@Concurrent(count = 5)
public abstract class AbstractAPIClientSession {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractAPIClientSession.class);

    protected TestContext testContext;
    protected TestUser admin;
    protected TestUser testUser;
    protected TestUser testUser2;
    protected LoginApi loginApi;
    protected ApiClient apiClient;
    private EnhancedApiClient enhancedApiClient;

    private Set<ApiClient> apiClients;

    /**
     * Default constructor.
     *
     * @param name name of the test.
     */
    protected AbstractAPIClientSession() {
        super();
    }

    /**
     * Gets the client identifier to use when performing a login
     *
     * @return The client identifier or <code>null</code> to use default one (<code>"com.openexchange.ajax.framework.AJAXClient"</code>)
     */
    protected String getClientId() {
        return null;
    }

    protected ApiClient getClient() {
        return apiClient;
    }

    @Before
    public void setUp() throws Exception {
        ProvisioningSetup.init();

        testContext = TestContextPool.acquireContext(this.getClass().getCanonicalName());
        Assert.assertNotNull("Unable to retrieve a context!", testContext);
        testUser = testContext.acquireUser();
        testUser2 = testContext.acquireUser();
        admin = testContext.getAdmin();
        apiClient = generateClient(testUser);
        rememberClient(apiClient);

        enhancedApiClient = generateEnhancedClient(testUser);
        rememberClient(enhancedApiClient);

    }

    protected void rememberClient(ApiClient client) {
        if (apiClients == null) {
            apiClients = new HashSet<>(1);
        }
        apiClients.add(client);
    }

    @After
    public void tearDown() throws Exception {
        for (ApiClient client : apiClients) {
            logoutClient(client);
        }
        TestContextPool.backContext(testContext);
    }

    /**
     * Does a logout for the client. Errors won't be logged.
     * Example:
     * <p>
     * <code>
     * client = logoutClient(client);
     * </code>
     * </p>
     *
     * @param client to logout
     * @return <code>null</code> to prepare client for garbage collection
     */
    protected final ApiClient logoutClient(ApiClient client) {
        return logoutClient(client, false);
    }

    /**
     * Does a logout for the client.
     * Example:
     * <p>
     * <code>
     * client = logoutClient(client, true);
     * </code>
     * </p>
     *
     * @param client to logout
     * @param loggin Whether to log an error or not
     * @return <code>null</code> to prepare client for garbage collection
     */
    protected final ApiClient logoutClient(ApiClient client, boolean loggin) {
        try {
            if (client != null) {
                client.logout();
            }
        } catch (Exception e) {
            if (loggin) {
                LOG.error("Unable to correctly tear down test setup.", e);
            }
        }
        return null;
    }

    /**
     * Generates a new {@link AJAXClient}. Uses standard client identifier.
     * Generated client needs a <b>logout in tearDown()</b>
     *
     * @return The new {@link AJAXClient}
     * @throws OXException In case no client could be created
     */
    protected final ApiClient generateDefaultClient() throws OXException {
        return generateClient(getClientId());
    }

    /**
     * Generates a new {@link AJAXClient}.
     * Generated client needs a <b>logout in tearDown()</b>
     *
     * @param client The client identifier to use when performing a login
     * @return The new {@link AJAXClient}
     * @throws OXException In case no client could be created
     */
    protected final ApiClient generateClient(String client) throws OXException {
        return generateClient(client, testContext.acquireUser());
    }

    /**
     * Generates a new {@link AJAXClient} for the {@link TestUser}.
     * Generated client needs a <b>logout in tearDown()</b>
     *
     * @param user The {@link TestUser} to create a client for
     * @return The new {@link AJAXClient}
     * @throws OXException In case no client could be created
     */
    protected final ApiClient generateClient(TestUser user) throws OXException {
        return generateClient(getClientId(), user);
    }

    /**
     * Generates a new {@link AJAXClient} for the {@link TestUser}.
     * Generated client needs a <b>logout in tearDown()</b>
     *
     * @param client The client identifier to use when performing a login
     * @param user The {@link TestUser} to create a client for
     * @return The new {@link AJAXClient}
     * @throws OXException In case no client could be created
     */
    protected final ApiClient generateClient(String client, TestUser user) throws OXException {
        if (null == user) {
            LOG.error("Can only create a client for an valid user");
            throw new OXException();
        }
        ApiClient newClient;
        try {
            newClient = new ApiClient();
            newClient.setBasePath("http://localhost/ajax");
            newClient.setUserAgent("ox-test-client");
        } catch (Exception e) {
            LOG.error("Could not generate new client for user {} in context {} ", user.getUser(), user.getContext());
            throw new OXException();
        }
        return newClient;
    }

    /**
     * Generates a new {@link EnhancedApiClient} for the {@link TestUser}.
     * Generated client needs a <b>logout in tearDown()</b>
     *
     * @param client The client identifier to use when performing a login
     * @param user The {@link TestUser} to create a client for
     * @return The new {@link EnhancedApiClient}
     * @throws OXException In case no client could be created
     */
    protected final EnhancedApiClient generateEnhancedClient(TestUser user) throws OXException {
        if (null == user) {
            LOG.error("Can only create a client for an valid user");
            throw new OXException();
        }
        EnhancedApiClient newClient;
        try {
            newClient = new EnhancedApiClient();
            newClient.setBasePath("http://localhost/ajax");
            newClient.setUserAgent("ox-test-client");
        } catch (Exception e) {
            LOG.error("Could not generate new client for user {} in context {} ", user.getUser(), user.getContext());
            throw new OXException();
        }
        return newClient;
    }

    /**
     * Gets the enhancedApiClient
     *
     * @return The enhancedApiClient
     */
    public EnhancedApiClient getEnhancedApiClient() {
        return enhancedApiClient;
    }

    /**
     * Sets the enhancedApiClient
     *
     * @param enhancedApiClient The enhancedApiClient to set
     */
    public void setEnhancedApiClient(EnhancedApiClient enhancedApiClient) {
        this.enhancedApiClient = enhancedApiClient;
    }
}
