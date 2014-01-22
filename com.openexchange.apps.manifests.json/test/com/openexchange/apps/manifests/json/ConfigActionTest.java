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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.apps.manifests.json;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.osgiservice.AJAXModuleActivator;
import com.openexchange.apps.manifests.json.osgi.ServerConfigServicesLookup;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.test.mock.InjectionFieldConstants;
import com.openexchange.test.mock.MockUtils;
import com.openexchange.tools.session.ServerSession;


/**
 * Unit tests for {@link ConfigAction}
 * 
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since 7.4
 */
@RunWith(PowerMockRunner.class)
public class ConfigActionTest {

    /**
     * Instance under test
     */
    private ConfigAction configAction = null;

    /**
     * Mock for {@link ServerConfigServicesLookup}
     */
    @Mock
    private ServerConfigServicesLookup serverConfigServicesLookup = null;

    private JSONArray manifests = new JSONArray();

    /**
     * Mock for the service lookup
     */
    @Mock
    private AJAXModuleActivator serviceLookup = null;

    /**
     * Mock for the ServerSession
     */
    @Mock
    private ServerSession serverSession = null;

    @Mock
    private AJAXRequestData ajaxRequestData = null;

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private Properties properties;

    /**
     * {@inheritDoc}
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // SERVICES
        Mockito.when(this.serviceLookup.getService(ConfigurationService.class)).thenReturn(this.configurationService);
        PowerMockito.when(configurationService.getPropertiesInFolder(Matchers.anyString())).thenReturn(this.properties);
        Map<String, Object> defaultMap = new HashMap<String, Object>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("key", "value");
        defaultMap.put("default", map);
        PowerMockito.when(this.configurationService.getYaml(Matchers.anyString())).thenReturn(defaultMap);
    }


    @Test
    public void testPerform_EverythingFine_ReturnEmptyRequestResult() throws OXException {

        this.configAction = new ConfigAction(serviceLookup, manifests, serverConfigServicesLookup) {

            @Override
            protected void mixInConfigurationValues(JSONObject serverconfig, ServerSession session) throws OXException, JSONException {
                return;
            }

            @Override
            protected void addComputedValues(JSONObject serverconfig, AJAXRequestData requestData, ServerSession session) throws OXException, JSONException {
                return;
            }

            @Override
            protected JSONObject getFromConfiguration(AJAXRequestData requestData, ServerSession session) throws JSONException, OXException {
                return new JSONObject();
            }
        };

        AJAXRequestResult ajaxRequestResult = this.configAction.perform(this.ajaxRequestData, this.serverSession);

        Assert.assertNotNull(ajaxRequestResult.getResultObject());
    }

    @Test
    public void testGetFromConfiguration_ConfigurationFound_ReturnJSON() throws JSONException, OXException {
        this.configAction = new ConfigAction(serviceLookup, manifests, serverConfigServicesLookup);

        MockUtils.injectValueIntoPrivateField(this.configAction, InjectionFieldConstants.SERVICES, serviceLookup);

        JSONObject jsonObject = this.configAction.getFromConfiguration(this.ajaxRequestData, this.serverSession);

        Assert.assertEquals("Wrong number of configurations returned", 1, jsonObject.length());
    }

    @Test
    public void testGetFromConfiguration_NoConfigurationFound_ReturnEmptyJSON() throws JSONException, OXException {
        this.configAction = new ConfigAction(serviceLookup, manifests, serverConfigServicesLookup);

        Mockito.when(serviceLookup.getService(ConfigurationService.class).getYaml(Matchers.anyString())).thenReturn(null);
        MockUtils.injectValueIntoPrivateField(this.configAction, InjectionFieldConstants.SERVICES, serviceLookup);

        JSONObject jsonObject = this.configAction.getFromConfiguration(this.ajaxRequestData, this.serverSession);

        Assert.assertEquals("Wrong number of configurations returned", 0, jsonObject.length());
    }

    @Test
    public void testLooksApplicable_NotApplicable_ReturnFalse() throws OXException {
        this.configAction = new ConfigAction(serviceLookup, manifests, serverConfigServicesLookup);
        Map<String, Object> defaultMap = new HashMap<String, Object>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("key", "value");
        defaultMap.put("default", map);

        boolean applicable = this.configAction.looksApplicable(defaultMap,
            this.ajaxRequestData,
            this.serverSession);

        Assert.assertFalse(applicable);
    }

    @Test
    public void testLooksApplicable_ValueNull_ReturnFalse() throws OXException {
        this.configAction = new ConfigAction(serviceLookup, manifests, serverConfigServicesLookup);

        boolean applicable = this.configAction.looksApplicable(null, this.ajaxRequestData, this.serverSession);

        Assert.assertFalse(applicable);
    }

    @Test
    public void testLooksApplicable_RequestDataNull_ReturnFalse() throws OXException {
        this.configAction = new ConfigAction(serviceLookup, manifests, serverConfigServicesLookup);
        Map<String, Object> defaultMap = new HashMap<String, Object>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("key", "value");
        defaultMap.put("default", map);

        boolean applicable = this.configAction.looksApplicable(defaultMap, null, this.serverSession);

        Assert.assertFalse(applicable);
    }

    @Test
    public void testLooksApplicable_ServerSessionNull_ReturnFalse() throws OXException {
        this.configAction = new ConfigAction(serviceLookup, manifests, serverConfigServicesLookup);
        Map<String, Object> defaultMap = new HashMap<String, Object>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("key", "value");
        defaultMap.put("default", map);

        boolean applicable = this.configAction.looksApplicable(defaultMap, this.ajaxRequestData, null);

        Assert.assertFalse(applicable);
    }
}
