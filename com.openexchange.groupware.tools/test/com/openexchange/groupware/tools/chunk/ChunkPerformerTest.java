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

package com.openexchange.groupware.tools.chunk;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import com.openexchange.exception.OXException;


/**
 * {@link ChunkPerformerTest}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public class ChunkPerformerTest {

    private static final int CHUNK_SIZE = 3;

    @Test
    public void testWithoutInitialOffset() throws OXException {
        final List<String> bunchOfStrings = prepareStrings();
        final List<String> results = new ArrayList<String>();
        final Incrementable i = new Incrementable();

        ChunkPerformer.perform(new Performable() {
            @Override
            public int perform(int off, int len) throws OXException {
                List<String> subList = bunchOfStrings.subList(off, len);
                for (String str : subList) {
                    results.add(str);
                }

                i.inc();
                return subList.size();
            }

            @Override
            public int getLength() {
                return bunchOfStrings.size();
            }

            @Override
            public int getInitialOffset() {
                return 0;
            }

            @Override
            public int getChunkSize() {
                return CHUNK_SIZE;
            }
        });

        assertEquals(4, i.get());
        assertEquals(bunchOfStrings.size(), results.size());
        int found = 0;
        for (String expected : bunchOfStrings) {
            for (String str : results) {
                if (str.equals(expected)) {
                    ++found;
                    break;
                }
            }
        }
        assertEquals(bunchOfStrings.size(), found);
    }

    @Test
    public void testWithInitialOffset() throws OXException {
        final List<String> bunchOfStrings = prepareStrings();
        final List<String> results = new ArrayList<String>();
        List<String> toCompare = bunchOfStrings.subList(1, bunchOfStrings.size());
        final Incrementable i = new Incrementable();

        ChunkPerformer.perform(new Performable() {
            @Override
            public int perform(int off, int len) throws OXException {
                List<String> subList = bunchOfStrings.subList(off, len);
                for (String str : subList) {
                    results.add(str);
                }

                i.inc();
                return subList.size();
            }

            @Override
            public int getLength() {
                return bunchOfStrings.size() - 1;
            }

            @Override
            public int getInitialOffset() {
                return 1;
            }

            @Override
            public int getChunkSize() {
                return CHUNK_SIZE;
            }
        });

        assertEquals(3, i.get());
        assertEquals(toCompare.size() - 1, results.size());
        int found = 0;
        for (String expected : toCompare) {
            for (String str : results) {
                if (str.equals(expected)) {
                    ++found;
                    break;
                }
            }
        }
        assertEquals(toCompare.size() - 1, found);
    }

    @Test
    public void testWithChunkSize0() throws OXException {
        final List<String> bunchOfStrings = prepareStrings();
        final List<String> results = new ArrayList<String>();
        final Incrementable i = new Incrementable();

        ChunkPerformer.perform(new Performable() {
            @Override
            public int perform(int off, int len) throws OXException {
                List<String> subList = bunchOfStrings.subList(off, len);
                for (String str : subList) {
                    results.add(str);
                }

                i.inc();
                return subList.size();
            }

            @Override
            public int getLength() {
                return bunchOfStrings.size();
            }

            @Override
            public int getInitialOffset() {
                return 0;
            }

            @Override
            public int getChunkSize() {
                return 0;
            }
        });

        assertEquals(1, i.get());
        assertEquals(bunchOfStrings.size(), results.size());
        int found = 0;
        for (String expected : bunchOfStrings) {
            for (String str : results) {
                if (str.equals(expected)) {
                    ++found;
                    break;
                }
            }
        }
        assertEquals(bunchOfStrings.size(), found);
    }

    private static List<String> prepareStrings() {
        final List<String> bunchOfStrings = new ArrayList<String>();
        for (int i = 0; i < 10; i++) {
            bunchOfStrings.add(UUID.randomUUID().toString());
        }

        return bunchOfStrings;
    }

    private static final class Incrementable {

        private int i;

        public Incrementable() {
            super();
            i = 0;
        }

        public void inc() {
            i++;
        }

        public int get() {
            return i;
        }
    }

}
