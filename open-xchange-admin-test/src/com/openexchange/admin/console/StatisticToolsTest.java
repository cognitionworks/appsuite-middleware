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

package com.openexchange.admin.console;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author d7
 */
public class StatisticToolsTest extends AbstractTest {

    private int returnCodeXchange;

    private int returnCodeAll;

    private int returnCodeThreadpool;

    private int returnCodeRuntime;

    private int returnCodeOs;

    private int returnCodeThreading;

    private int returnCodeShowOperations;

    private int returnCodeDooperation;

    private int returnCodeMemory;

    private int returnCodeGc;

    private int returnCodeMemoryFull;

    private int returnCodeDocumentconverter;

    private int returnCodeOffice;

    @Test
    public void testGetXchangeStats() {
        resetBuffers();
        final StatisticTools statisticTools = new StatisticTools() {

            @Override
            protected void sysexit(int exitCode) {
                StatisticToolsTest.this.returnCodeXchange = exitCode;
            }
        };
        statisticTools.start(new String[] { "-x" }, "showruntimestats");
        assertEquals("Expected 0 as return code!", 0, this.returnCodeXchange);
    }

    @Test
    public void testGetAllStats() {
        resetBuffers();
        final StatisticTools statisticTools = new StatisticTools() {

            @Override
            protected void sysexit(int exitCode) {
                StatisticToolsTest.this.returnCodeAll = exitCode;
            }
        };
        statisticTools.start(new String[] { "-a" }, "showruntimestats");
        assertEquals("Expected 0 as return code!", 0, this.returnCodeAll);
    }

    @Test
    public void testGetThreadpoolstats() {
        resetBuffers();
        final StatisticTools statisticTools = new StatisticTools() {

            @Override
            protected void sysexit(int exitCode) {
                StatisticToolsTest.this.returnCodeThreadpool = exitCode;
            }
        };
        statisticTools.start(new String[] { "-p" }, "showruntimestats");
        assertEquals("Expected 0 as return code!", 0, this.returnCodeThreadpool);
    }

    @Test
    public void testGetRuntimestats() {
        resetBuffers();
        final StatisticTools statisticTools = new StatisticTools() {

            @Override
            protected void sysexit(int exitCode) {
                StatisticToolsTest.this.returnCodeRuntime = exitCode;
            }
        };
        statisticTools.start(new String[] { "-r" }, "showruntimestats");
        assertEquals("Expected 0 as return code!", 0, this.returnCodeRuntime);
    }

    @Test
    public void testGetOsstats() {
        resetBuffers();
        final StatisticTools statisticTools = new StatisticTools() {

            @Override
            protected void sysexit(int exitCode) {
                StatisticToolsTest.this.returnCodeOs = exitCode;
            }
        };
        statisticTools.start(new String[] { "-o" }, "showruntimestats");
        assertEquals("Expected 0 as return code!", 0, this.returnCodeOs);
    }

    @Test
    public void testGetThreadingstats() {
        resetBuffers();
        final StatisticTools statisticTools = new StatisticTools() {

            @Override
            protected void sysexit(int exitCode) {
                StatisticToolsTest.this.returnCodeThreading = exitCode;
            }
        };
        statisticTools.start(new String[] { "-t" }, "showruntimestats");
        assertEquals("Expected 0 as return code!", 0, this.returnCodeThreading);
    }

    @Test
    public void testGetShowoperationsstats() {
        resetBuffers();
        final StatisticTools statisticTools = new StatisticTools() {

            @Override
            protected void sysexit(int exitCode) {
                StatisticToolsTest.this.returnCodeShowOperations = exitCode;
            }
        };
        statisticTools.start(new String[] { "-s" }, "showruntimestats");
        assertEquals("Expected 0 as return code!", 0, this.returnCodeShowOperations);
    }

    @Test
    public void testGetDooperation() {
        resetBuffers();
        final StatisticTools statisticTools = new StatisticTools() {

            @Override
            protected void sysexit(int exitCode) {
                StatisticToolsTest.this.returnCodeDooperation = exitCode;
            }
        };
        statisticTools.start(new String[] { "-d" }, "showruntimestats");
        assertEquals("Expected 0 as return code!", 0, this.returnCodeDooperation);
    }

    @Test
    public void testGetMemory() {
        resetBuffers();
        final StatisticTools statisticTools = new StatisticTools() {

            @Override
            protected void sysexit(int exitCode) {
                StatisticToolsTest.this.returnCodeMemory = exitCode;
            }
        };
        statisticTools.start(new String[] { "-m" }, "showruntimestats");
        assertEquals("Expected 0 as return code!", 0, this.returnCodeMemory);
    }

    @Test
    public void testGetGcstats() {
        resetBuffers();
        final StatisticTools statisticTools = new StatisticTools() {

            @Override
            protected void sysexit(int exitCode) {
                StatisticToolsTest.this.returnCodeGc = exitCode;
            }
        };
        statisticTools.start(new String[] { "-z" }, "showruntimestats");
        assertEquals("Expected 0 as return code!", 0, this.returnCodeGc);
    }

    @Test
    public void testGetMemoryFullstats() {
        resetBuffers();
        final StatisticTools statisticTools = new StatisticTools() {

            @Override
            protected void sysexit(int exitCode) {
                StatisticToolsTest.this.returnCodeMemoryFull = exitCode;
            }
        };
        statisticTools.start(new String[] { "-M" }, "showruntimestats");
        assertEquals("Expected 0 as return code!", 0, this.returnCodeMemoryFull);
    }

    @Test
    public void testGetDocumentconverterstats() {
        resetBuffers();
        final StatisticTools statisticTools = new StatisticTools() {

            @Override
            protected void sysexit(int exitCode) {
                StatisticToolsTest.this.returnCodeDocumentconverter = exitCode;
            }
        };
        statisticTools.start(new String[] { "-y" }, "showruntimestats");
        assertEquals("Expected 0 as return code!", 0, this.returnCodeDocumentconverter);
    }

    @Test
    public void testGetOfficestats() {
        resetBuffers();
        final StatisticTools statisticTools = new StatisticTools() {

            @Override
            protected void sysexit(int exitCode) {
                StatisticToolsTest.this.returnCodeOffice = exitCode;
            }
        };
        statisticTools.start(new String[] { "-f" }, "showruntimestats");
        assertEquals("Expected 0 as return code!", 0, this.returnCodeOffice);
    }
}
