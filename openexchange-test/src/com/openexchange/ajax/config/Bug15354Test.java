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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.ajax.config;

import static com.openexchange.java.Autoboxing.B;
import java.util.Arrays;
import java.util.Random;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.ajax.config.actions.GetRequest;
import com.openexchange.ajax.config.actions.SetRequest;
import com.openexchange.ajax.config.actions.Tree;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXClient.User;
import com.openexchange.ajax.framework.AbstractAJAXSession;

/**
 * {@link Bug15354Test}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class Bug15354Test extends AbstractAJAXSession {

    static final Log LOG = LogFactory.getLog(Bug15354Test.class);
    private static final int ITERATIONS = 10000;
    private static final int NEEDED_BROKEN = 10;
    private final BetaWriter[] writer = new BetaWriter[5];
    private final Thread[] thread = new Thread[writer.length];
    private AJAXClient client;
    private boolean origValue;
    private Object[] origAliases;

    public Bug15354Test(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        client = getClient();
        origValue = client.execute(new GetRequest(Tree.Beta)).getBoolean();
        origAliases = client.execute(new GetRequest(Tree.MailAddresses)).getArray();
        assertNotNull("Aliases are null.", origAliases);
        Arrays.sort(origAliases);
        for (int i = 0; i < writer.length; i++) {
            writer[i] = new BetaWriter();
            thread[i] = new Thread(writer[i]);
        }
        for (int i = 0; i < thread.length; i++) {
            thread[i].start();
        }
    }

    @Override
    public void tearDown() throws Exception {
        for (int i = 0; i < writer.length; i++) {
            writer[i].stop();
        }
        for (int i = 0; i < thread.length; i++) {
            thread[i].join();
        }
        for (int i = 0; i < writer.length; i++) {
            assertNull(writer[i].getThrowable());
        }
        client.execute(new SetRequest(Tree.Beta, B(origValue)));
        super.tearDown();
    }

    public void testAliases() throws Throwable {
        int consecutiveBrokenReads = 0;
        for (int i = 0; i < ITERATIONS && consecutiveBrokenReads < NEEDED_BROKEN; i++) {
            Object[] testAliases = client.execute(new GetRequest(Tree.MailAddresses)).getArray();
            if (null == testAliases) {
                consecutiveBrokenReads++;
            } else if (origAliases.length != testAliases.length) {
                consecutiveBrokenReads++;
            } else {
                Arrays.sort(testAliases);
                boolean match = true;
                for (int j = 0; j < origAliases.length && match; j++) {
                    if (!origAliases[j].equals(testAliases[j])) {
                        match = false;
                    }
                }
                if (match) {
                    consecutiveBrokenReads = 0;
                } else {
                    consecutiveBrokenReads++;
                }
            }
        }
        // Final test.
        Object[] testAliases = client.execute(new GetRequest(Tree.MailAddresses)).getArray();
        assertNotNull("Aliases are null.", origAliases);
        assertNotNull("Aliases are null.", testAliases);
        assertEquals("Number of aliases are not equal.", origAliases.length, testAliases.length);
        Arrays.sort(origAliases);
        Arrays.sort(testAliases);
        for (int i = 0; i < origAliases.length; i++) {
            assertEquals("Aliases are not the same.", origAliases[i], testAliases[i]);
        }
    }

    private static final class BetaWriter implements Runnable {
        private boolean run = true;
        private Throwable t;
        BetaWriter() {
            super();
        }
        void stop() {
            run = false;
        }
        Throwable getThrowable() {
            return t;
        }
        public void run() {
            Random rand = new Random(System.currentTimeMillis());
            try {
                AJAXClient client = new AJAXClient(User.User1);
                while (run) {
                    client.execute(new SetRequest(Tree.Beta, B(rand.nextBoolean())));
                }
                client.logout();
            } catch (Throwable t2) {
                LOG.error(t2.getMessage(), t2);
                t = t2;
            }
        }
    }
}
