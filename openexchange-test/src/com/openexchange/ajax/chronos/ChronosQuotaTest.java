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

package com.openexchange.ajax.chronos;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.is;
import java.rmi.Naming;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import com.openexchange.admin.rmi.OXUserInterface;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.UserProperty;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAPIClientSession;
import com.openexchange.configuration.AJAXConfig;
import com.openexchange.configuration.AJAXConfig.Property;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import com.openexchange.testing.httpclient.models.QuotasResponse;
import com.openexchange.testing.httpclient.modules.QuotaApi;

/**
 * {@link ChronosQuotaTest}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.0
 */
public class ChronosQuotaTest extends AbstractAPIClientSession {

    // XXX Change to 'calendar' after replacing old calendar
    private static final String MODULE = "calendar.chronos";

    private AJAXClient ajaxClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        ajaxClient = new AJAXClient(testUser);
        Map<String, String> userAttributes = new HashMap<String, String>(1);
        userAttributes.put("com.openexchange.quota.calendar", "1");
        setQuota(userAttributes);
    }

    @Test
    public void testGetQuota() throws Exception {
        ApiClient client = getClient();
        client.login(testUser.getLogin(), testUser.getPassword());
        QuotaApi api = new QuotaApi(client);
        rememberClient(client);
        QuotasResponse response = api.getQuotaInformation(client.getSession(), MODULE, "0");
        Object data = response.getData();

        // Check for the right type
        assertThat("Can't assign response data to the known output", data, instanceOf(Map.class));

        // Suppress since we already checked ..
        @SuppressWarnings("unchecked") Map<String, Object> info = (Map<String, Object>) data;

        // Check output
        assertThat("Account identifier does not match", info.get("account_id"), is("0"));
        assertThat("Account name does not match", info.get("account_name"), is("Internal Calendar"));
        assertThat("Account quota does not match", info.get("countquota"), is(new Integer(1)));
        /*
         * Can't check something like
         * assertThat("Account use does not match", info.get("countuse"), is(new Integer(0)));
         * cause other test might create event that get not deleted..
         */
    }

    @Override
    public void tearDown() throws Exception {
        Map<String, String> userAttributes = new HashMap<String, String>(1);
        userAttributes.put("com.openexchange.quota.calendar", "-1");
        setQuota(userAttributes);
        if (null != ajaxClient) {
            ajaxClient.logout();
        }
        super.tearDown();
    }

    private void setQuota(Map<String, String> props) throws Exception {
        com.openexchange.admin.rmi.dataobjects.User user = new com.openexchange.admin.rmi.dataobjects.User(ajaxClient.getValues().getUserId());
        for (String property : props.keySet()) {
            user.setUserAttribute("config", property, props.get(property));
        }
        Credentials credentials = new Credentials(admin.getUser(), admin.getPassword());
        OXUserInterface iface = (OXUserInterface) Naming.lookup("rmi://" + AJAXConfig.getProperty(Property.RMI_HOST) + ":1099/" + OXUserInterface.RMI_NAME);
        iface.change(new Context(Integer.valueOf(ajaxClient.getValues().getContextId())), user, credentials);

        List<UserProperty> userConfigurationSource = iface.getUserConfigurationSource(new Context(Integer.valueOf(ajaxClient.getValues().getContextId())), user, "quota", credentials);
        System.out.println("User configuration related to 'quota' after changing the following properties:");
        for (String property : props.keySet()) {
            System.out.println(property + "' to " + props.get(property));
        }
        for (UserProperty prop : userConfigurationSource) {
            System.out.println("Property " + prop.getName() + "(" + prop.getScope() + "): " + prop.getValue());
        }
    }

}
