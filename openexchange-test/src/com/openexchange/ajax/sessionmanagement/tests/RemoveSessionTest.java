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

package com.openexchange.ajax.sessionmanagement.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.openexchange.ajax.sessionmanagement.AbstractSessionManagementTest;
import com.openexchange.ajax.sessionmanagement.actions.GetSessionsRequest;
import com.openexchange.ajax.sessionmanagement.actions.GetSessionsResponse;
import com.openexchange.ajax.sessionmanagement.actions.RemoveSessionRequest;
import com.openexchange.session.management.ManagedSession;


/**
 * {@link RemoveSessionTest}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.0
 */
public class RemoveSessionTest extends AbstractSessionManagementTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

//    @Test
//    public void testRemoveSession() throws Exception {
//        String sessionId = testClient2.getSession().getId();
//        RemoveSessionRequest req = new RemoveSessionRequest(sessionId);
//        RemoveSessionResponse resp = testClient1.execute(req);
//        assertFalse(resp.hasError());
//
//        GetSessionsRequest getReq = new GetSessionsRequest();
//        GetSessionsResponse getResp = testClient1.execute(getReq);
//        assertFalse(getResp.hasError());
//        Collection<ManagedSession> sessions = getResp.getSessions();
//        assertEquals(1, sessions.size());
//        for (ManagedSession session : sessions) {
//            assertEquals(testClient1.getSession().getId(), session.getSessionId());
//            assertNotEquals(sessionId, session.getSessionId());
//        }
//    }

    @Test
    public void testRemoveSession_WrongSessionId() throws Exception {
        RemoveSessionRequest req = new RemoveSessionRequest("thisWillFail", false);
        testClient1.execute(req);

        GetSessionsRequest getReq = new GetSessionsRequest();
        GetSessionsResponse getResp = testClient1.execute(getReq);
        assertFalse(getResp.hasError());
        Collection<ManagedSession> sessions = getResp.getSessions();
        assertEquals(2, sessions.size());
        for (ManagedSession session : sessions) {
            assertTrue(testClient1.getSession().getId().equals(session.getSessionId()) || testClient2.getSession().getId().equals(session.getSessionId()));
        }
    }

}
