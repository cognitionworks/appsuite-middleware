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

package com.openexchange.ajax.requesthandler;

import java.util.List;
import com.openexchange.ajax.requesthandler.DefaultConverter.Step;
import com.openexchange.ajax.requesthandler.ResultConverter.Quality;
import com.openexchange.tools.session.ServerSession;
import junit.framework.TestCase;

/**
 * {@link DefaultConverterTest}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class DefaultConverterTest extends TestCase {

    private DefaultConverter converter = new DefaultConverter();

    public void testSingleStepConversion() {
        TestConverter tc = new TestConverter("A", "B", Quality.GOOD);
        converter.addConverter(tc);
        Step shortestPath = converter.getShortestPath("A", "B");

        assertTrue(shortestPath.next == null);
        assertEquals(tc, shortestPath.converter);
    }

    public void testMultiStepConversion() {
        TestConverter a = new TestConverter("A", "B", Quality.GOOD);
        TestConverter b = new TestConverter("B", "C", Quality.GOOD);
        TestConverter c = new TestConverter("C", "D", Quality.GOOD);
        converter.addConverter(a);
        converter.addConverter(b);
        converter.addConverter(c);

        Step shortestPath = converter.getShortestPath("A", "D");

        assertPath(shortestPath, a, b, c);
    }

    public void testRetrace() {
        TestConverter a = new TestConverter("A", "B", Quality.GOOD);
        TestConverter b = new TestConverter("B", "C", Quality.GOOD);
        TestConverter b2 = new TestConverter("B", "E", Quality.GOOD);
        TestConverter c = new TestConverter("C", "D", Quality.GOOD);
        converter.addConverter(a);
        converter.addConverter(b);
        converter.addConverter(b2);
        converter.addConverter(c);

        Step shortestPath = converter.getShortestPath("A", "D");

        assertPath(shortestPath, a, b, c);
    }

    public void testPreferHighQuality() {
        TestConverter a = new TestConverter("A", "B", Quality.GOOD);
        TestConverter b = new TestConverter("B", "C", Quality.GOOD);
        TestConverter b2 = new TestConverter("B", "C", Quality.BAD);
        TestConverter c = new TestConverter("C", "D", Quality.GOOD);
        converter.addConverter(a);
        converter.addConverter(b);
        converter.addConverter(b2);
        converter.addConverter(c);

        Step shortestPath = converter.getShortestPath("A", "D");

        assertPath(shortestPath, a, b, c);
    }

    public void testImpossible() {
        TestConverter a = new TestConverter("A", "B", Quality.GOOD);
        TestConverter b = new TestConverter("B", "C", Quality.GOOD);
        TestConverter c = new TestConverter("C", "D", Quality.GOOD);
        converter.addConverter(a);
        converter.addConverter(b);
        converter.addConverter(c);
        
        try {
            converter.getShortestPath("A", "E");
            fail("Huh?!");
        } catch (IllegalArgumentException x) {
            assertTrue(true);
        }

    }

    private void assertPath(Step shortestPath, TestConverter... converters) {
        Step current = shortestPath;
        for (TestConverter testConverter : converters) {
            assertNotNull("Missing one for " + testConverter, current);
            assertEquals(testConverter, current.converter);
            current = current.next;
        }
    }

    public static class TestConverter implements ResultConverter {

        private String input;

        private String output;

        private Quality quality;

        public TestConverter(String input, String output, Quality quality) {
            super();
            this.input = input;
            this.output = output;
            this.quality = quality;
        }

        public void convert(AJAXRequestData request, AJAXRequestResult result, ServerSession session, Converter converter) {

        }

        public String getInputFormat() {
            return input;
        }

        public String getOutputFormat() {
            return output;
        }

        public Quality getQuality() {
            return quality;
        }

        public boolean handles(AJAXRequestData request, AJAXRequestResult result) {
            return true;
        }

        @Override
        public String toString() {
            return input + " -> " + output;
        }

    }

}
