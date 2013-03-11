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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.realtime.packet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import com.openexchange.exception.OXException;
import com.openexchange.realtime.packet.Presence.Type;
import com.openexchange.realtime.payload.PayloadTree;

/**
 * {@link PresenceTest}
 * 
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 */
public class PresenceTest {

    private static ID fromID = new ID("ox", null, "marc.arens", "premium", null);

    private static PresenceState away = PresenceState.AWAY;

    private static String message = "I'll be back!";

    private static byte priority = 1;

    @Test
    public void testInitialPresenceBuilder() {
        Presence initialPresence = Presence.builder().from(fromID).build();

        assertEquals(fromID, initialPresence.getFrom());
        assertNull(initialPresence.getTo());
        assertEquals(PresenceState.ONLINE, initialPresence.getState());
        assertEquals("", initialPresence.getMessage());
        assertEquals(0, initialPresence.getPriority());
        assertEquals(Type.NONE, initialPresence.getType());
        assertNull(initialPresence.getError());
        assertEquals(Collections.EMPTY_LIST, initialPresence.getPayloads());

    }

    @Test
    public void testUpdatePresenceBuilder() {
        // @formatter:off
        Presence updatePresence = Presence.builder()
            .from(fromID)
            .state(away)
            .message(message)
            .priority(priority)
            .build();
        // @formatter:on

        assertEquals(fromID, updatePresence.getFrom());
        assertNull(updatePresence.getTo());
        assertEquals(away, updatePresence.getState());
        assertEquals(message, updatePresence.getMessage());
        assertEquals(priority, updatePresence.getPriority());
        assertEquals(Type.NONE, updatePresence.getType());
        assertNull(updatePresence.getError());

        // Payload checks
        assertEquals(updatePresence.getDefaultPayloads(), updatePresence.getPayloads());
        assertEquals(3, updatePresence.getPayloads().size());
        ArrayList<PayloadTree> statusTrees = new ArrayList<PayloadTree>(updatePresence.getPayloads(Presence.STATUS_PATH));
        ArrayList<PayloadTree> messageTrees = new ArrayList<PayloadTree>(updatePresence.getPayloads(Presence.MESSAGE_PATH));
        ArrayList<PayloadTree> priorityTrees = new ArrayList<PayloadTree>(updatePresence.getPayloads(Presence.PRIORITY_PATH));

        assertEquals(1, statusTrees.size());
        assertEquals(1, messageTrees.size());
        assertEquals(1, priorityTrees.size());

        assertEquals(away, statusTrees.get(0).getRoot().getData());
        assertEquals(message, messageTrees.get(0).getRoot().getData());
        assertEquals(priority, priorityTrees.get(0).getRoot().getData());

    }

    @Test
    public void testCopyConstructor() throws OXException {
        // @formatter:off
        Presence awayPresence = Presence.builder()
            .from(fromID)
            .state(away)
            .message(message)
            .priority(priority)
            .build();
        // @formatter:on

        Presence copiedAwayPresence = new Presence(awayPresence);

        awayPresence.setFrom(new ID("ox", "francisco.laguna", "premium", "macbook air"));
        awayPresence.setPriority((byte) -1);
        awayPresence.setState(PresenceState.DO_NOT_DISTURB);
        awayPresence.setMessage("Planning the future of the ox backend");
        assertThat(awayPresence.getFrom(), is(not(copiedAwayPresence.getFrom())));
        assertThat(awayPresence.getPriority(), is(not(copiedAwayPresence.getPriority())));
        assertThat(awayPresence.getState(), is(not(copiedAwayPresence.getState())));
        assertThat(awayPresence.getMessage(), is(not(copiedAwayPresence.getMessage())));

        assertEquals(fromID, copiedAwayPresence.getFrom());
        assertEquals(message, copiedAwayPresence.getMessage());
        assertEquals(priority, copiedAwayPresence.getPriority());
        assertEquals(away, copiedAwayPresence.getState());

        assertEquals(message, getFirstTreeRootData(copiedAwayPresence.getPayloads(Presence.MESSAGE_PATH)));
        assertEquals(priority, getFirstTreeRootData(copiedAwayPresence.getPayloads(Presence.PRIORITY_PATH)));
        assertEquals(away, getFirstTreeRootData(copiedAwayPresence.getPayloads(Presence.STATUS_PATH)));
    }

    private Object getFirstTreeRootData(Collection<PayloadTree> trees) {
        List<PayloadTree> payloads = new ArrayList<PayloadTree>(trees);
        return payloads.get(0).getRoot().getData();
    }

}
