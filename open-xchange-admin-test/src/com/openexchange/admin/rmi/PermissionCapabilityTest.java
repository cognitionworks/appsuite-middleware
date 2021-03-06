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

package com.openexchange.admin.rmi;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Test;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.factory.ContextFactory;
import com.openexchange.admin.rmi.factory.UserFactory;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.userconfiguration.PermissionCheckerCodes;

/**
 * {@link PermissionCapabilityTest} - tests that capabilities which are actually permissions cannot be provisioned
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.4
 */
public class PermissionCapabilityTest extends AbstractRMITest {

    protected Context context;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = getContextManager().create(ContextFactory.createContext(5000L), contextAdminCredentials);
    }

    /**
     * Tests that provisioning of illegal capabilities is not possible
     */
    @Test
    public void testProvisioningAnIllegalCapability() {
        // provisioning via user attributes
        User user = UserFactory.createUser(PermissionCapabilityTest.class.getSimpleName() +"_"+ System.currentTimeMillis(), "secret", TEST_DOMAIN, context);
        user.setUserAttribute("config", "com.openexchange.capability.infostore", Boolean.FALSE.toString());
        user.setUserAttribute("config", "com.openexchange.capability.contacts", Boolean.FALSE.toString());
        try {
            getUserManager().create(context, user, contextAdminCredentials);
            fail("Expecting an exception.");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidDataException);
            Throwable cause = e.getCause();
            assertTrue(cause instanceof OXException);
            OXException oxe = (OXException) cause;
            assertTrue(PermissionCheckerCodes.ILLEGAL_USER_ATTRIBUTE.equals(oxe));
            assertTrue(Arrays.asList(oxe.getDisplayArgs()).stream().filter((arg) -> arg instanceof String && ((String) arg).contains("infostore") && ((String) arg).contains("contacts")).findFirst().isPresent());
        }

        // provisioning via capabilities
        user = UserFactory.createUser(PermissionCapabilityTest.class.getSimpleName() +"_"+ System.currentTimeMillis(), "secret", TEST_DOMAIN, context);
        try {
            user = getUserManager().create(context, user, contextAdminCredentials);
            HashSet<String> set = new HashSet<>();
            set.add("infostore");
            set.add("contacts");
            getUserManager().change(context, user, set, Collections.emptySet(), Collections.emptySet(), contextAdminCredentials);
            fail("Expecting an exception.");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidDataException);
            Throwable cause = e.getCause();
            assertTrue(cause instanceof OXException);
            OXException oxe = (OXException) cause;
            assertTrue(PermissionCheckerCodes.ILLEGAL_CAPABILITY.equals(oxe));
            assertTrue(Arrays.asList(oxe.getDisplayArgs()).stream().filter((arg) -> arg instanceof String && ((String) arg).contains("infostore") && ((String) arg).contains("contacts")).findFirst().isPresent());
        }

        // removal of existing illegal capabilities must be possible
        user = UserFactory.createUser(PermissionCapabilityTest.class.getSimpleName() +"_"+ System.currentTimeMillis(), "secret", TEST_DOMAIN, context);
        try {
            user = getUserManager().create(context, user, contextAdminCredentials);
            HashSet<String> set = new HashSet<>();
            set.add("infostore");
            set.add("contacts");
            getUserManager().change(context, user, Collections.emptySet(), Collections.emptySet(), set, contextAdminCredentials);
        } catch (@SuppressWarnings("unused") Exception e) {
            fail("Expecting no exception.");
        }
    }

}
