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

package com.openexchange.subscribe.json.actions;

import static com.openexchange.java.Autoboxing.B;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.subscribe.Subscription;
import com.openexchange.subscribe.SubscriptionSource;

/**
 * {@link AbstractSubscribeActionTest}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.8.1
 */
public class AbstractSubscribeActionTest {

    private Subscription subscription;

    @Mock
    private SubscriptionSource source;

    @Mock
    private ServiceLookup services;

    @Mock
    private ConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        subscription = new Subscription();
        subscription.setSource(source);
        Mockito.when(source.getId()).thenReturn(AbstractSubscribeAction.MICROFORMATS_ID);
        Mockito.when(services.getService(ConfigurationService.class)).thenReturn(configurationService);
    }

     @Test
     public void testCheckCreateModifyEnabled_enabledViaConfig_doNotThrowException() throws OXException {
        Mockito.when(B(configurationService.getBoolProperty("com.openexchange.subscribe.microformats.createModifyEnabled", false))).thenReturn(Boolean.TRUE);

        NewSubscriptionAction abstractSubscribeAction = new NewSubscriptionAction(services);
        abstractSubscribeAction.checkAllowed(subscription);

    }

    @Test(expected = OXException.class)
     public void testCheckCreateModifyEnabled_disabledViaConfig_doThrowException() throws Exception {
        Mockito.when(B(configurationService.getBoolProperty("com.openexchange.subscribe.microformats.createModifyEnabled", false))).thenReturn(Boolean.FALSE);

        NewSubscriptionAction abstractSubscribeAction = new NewSubscriptionAction(services);
        abstractSubscribeAction.checkAllowed(subscription);
    }

     @Test
     public void testCheckCreateModifyEnabled_noOXMFSubscription_doNotThrowException() throws OXException {
        Mockito.when(B(configurationService.getBoolProperty("com.openexchange.subscribe.microformats.createModifyEnabled", false))).thenReturn(Boolean.TRUE);
        Mockito.when(source.getId()).thenReturn("not_the_OXMF_type");

        NewSubscriptionAction abstractSubscribeAction = new NewSubscriptionAction(services);
        abstractSubscribeAction.checkAllowed(subscription);
    }

     @Test
     public void testCheckCreateModifyEnabled_noOXMFSubscriptionButDisabled_doNotThrowException() throws OXException {
        Mockito.when(B(configurationService.getBoolProperty("com.openexchange.subscribe.microformats.createModifyEnabled", false))).thenReturn(Boolean.FALSE);
        Mockito.when(source.getId()).thenReturn("not_the_OXMF_type");

        NewSubscriptionAction abstractSubscribeAction = new NewSubscriptionAction(services);
        abstractSubscribeAction.checkAllowed(subscription);
    }
}
