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

package com.openexchange.ajax.mail.filter.tests.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import com.openexchange.ajax.framework.AbstractAPIClientSession;
import com.openexchange.testing.httpclient.models.MailFilterConfigDatav2;
import com.openexchange.testing.httpclient.models.MailFilterConfigResponsev2;
import com.openexchange.testing.httpclient.modules.MailfilterApi;

/**
 * {@link ConfigTest}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class ConfigTest extends AbstractAPIClientSession {

    /**
     * Initializes a new {@link ConfigTest}.
     *
     * @param name The test case's name
     */
    public ConfigTest() {
        super();
    }

    /**
     * Test the GET /ajax/mailfilter/v2?action=config API call
     */
    @Test
    public void testConfig() throws Exception {
        MailfilterApi api = new MailfilterApi(getApiClient());
        MailFilterConfigResponsev2 response = api.getConfigV2(null);

        assertNotNull("The mail filter configuration response is null", response);
        assertNotNull("The mail filter configuration data is null", response.getData());
        MailFilterConfigDatav2 config = response.getData();
        assertNotNull("The 'tests' list is null", config.getTests());
        assertNotNull("The 'actionCommands' list is null", config.getActioncmds());

        assertFalse("The 'tests' list is empty", config.getTests().isEmpty());
        assertFalse("The 'actionCommands list is empty", config.getActioncmds().isEmpty());
    }
}
