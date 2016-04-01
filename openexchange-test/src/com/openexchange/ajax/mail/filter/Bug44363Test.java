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

package com.openexchange.ajax.mail.filter;

import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXSession;
import com.openexchange.ajax.mail.filter.action.AbstractAction;
import com.openexchange.ajax.mail.filter.action.Discard;
import com.openexchange.ajax.mail.filter.action.Vacation;
import com.openexchange.ajax.mail.filter.comparison.ContainsComparison;
import com.openexchange.ajax.mail.filter.test.HeaderTest;

/**
 * {@link Bug44363Test}
 * 
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class Bug44363Test extends AbstractMailFilterTest {

    private AJAXClient client;

    /**
     * Initialises a new {@link Bug44363Test}.
     * 
     * @param name
     */
    public Bug44363Test(String name) {
        super(name);
    }

    /**
     * Test case for bug 44363
     * <ul>
     * <li>Create a vacation rule</li>
     * <li>Create some other arbitrary rule</li>
     * <li>Deactivate the other rule</li>
     * <li>Deactivate the vacation rule</li>
     * </ul>
     * Assert there are still two rules present
     * 
     * @throws Exception if an error is occurred
     */
    public void testBug44363() throws Exception {
        final AJAXSession ajaxSession = getSession();
        client = getClient();
        deleteAllExistingRules(client.getValues().getDefaultAddress(), ajaxSession);

        // Create a vacation rule with a single lined dot '.' character
        Rule vacationRule;
        {
            vacationRule = new Rule();
            vacationRule.setName("Vacation Notice");
            vacationRule.setActive(true);
            vacationRule.setActioncmds(new AbstractAction[] { new Vacation(7, new String[] { client.getValues().getDefaultAddress() }, "Vacation Notice for Bug 44363", "Multiline text with\n\n.\n\n a single lined dot character for bug 44363") });
            final ContainsComparison conComp = new ContainsComparison();
            vacationRule.setTest(new HeaderTest(conComp, new String[] { "Subject" }, new String[] { "Vacation for 44363" }));
            String vacationId = insertRule(vacationRule, null, ajaxSession);
            vacationRule.setId(vacationId);
        }

        // Create some other rule
        Rule otherRule;
        {
            otherRule = new Rule();
            otherRule.setName("Some Rule for Bug 44363");
            otherRule.setActive(true);
            otherRule.setActioncmds(new AbstractAction[] { new Discard() });
            ContainsComparison conComp = new ContainsComparison();
            otherRule.setTest(new HeaderTest(conComp, new String[] { "Subject" }, new String[] { "Bug 44363" }));
            String otherId = insertRule(otherRule, null, ajaxSession);
            otherRule.setId(otherId);
        }

        // Assert we have 2 rules
        String[] idArray = getIdArray(null, ajaxSession);
        assertEquals("two rules expected", 2, idArray.length);

        // Deactivate the other rule
        otherRule.setActive(false);
        updateRule(otherRule, client.getValues().getDefaultAddress(), ajaxSession);

        // Deactivate the vacation notice
        vacationRule.setActive(false);
        updateRule(vacationRule, client.getValues().getDefaultAddress(), ajaxSession);

        // Assert that we still have two rules
        idArray = getIdArray(null, ajaxSession);
        assertEquals("two rules expected", 2, idArray.length);
    }
}
