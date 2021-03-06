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
 *    trademarks of the OX Software GmbH. group of companies.
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

package com.openexchange.ajax.system;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import org.json.JSONException;
import org.junit.Test;
import com.openexchange.exception.OXException;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.WhoAmIResponse;

/**
 * {@link WhoAmITest}
 *
 * @author <a href="mailto:anna.ottersbach@open-xchange.com">Anna Ottersbach</a>
 * @since v7.10.3
 */
public class WhoAmITest extends AbstractSystemTest {

    @Test
    public void testSystemWhoAmI_normalTest_ResponseAvailable() throws ApiException, OXException, IOException, JSONException {
        String sessionId = apiClient.getSession();
        WhoAmIResponse response = api.whoami();
        assertNull(response.getData().getRandom());
        assertEquals(sessionId, response.getData().getSession());
        assertEquals(apiClient.getUser(), response.getData().getUser());
        assertEquals(apiClient.getUserId().toString(), response.getData().getUserId());
        assertEquals(I(getClient().getValues().getContextId()).toString(), response.getData().getContextId());
        assertNull(response.getData().getRequiresMultifactor());
        assertEquals(getClient().getValues().getLocale().toString(), response.getData().getLocale());

    }

    @Test
    public void testSystemWhoAmI_WrongSessionId_ResponseNull() throws ApiException {
        String old = api.getApiClient().getSession();
        try {
            api.getApiClient().setApiKey("abcdefghijklmnopqrxtuvwxyz");
            WhoAmIResponse response = api.whoami();
            assertNull(response.getData());
            assertTrue(response.getError().contains("session expired"));
        } finally {
            api.getApiClient().setApiKey(old);
        }
    }

}
