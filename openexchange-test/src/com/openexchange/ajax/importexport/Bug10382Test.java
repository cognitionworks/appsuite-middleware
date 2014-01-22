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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.ajax.importexport;

import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXClient.User;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.framework.Executor;
import com.openexchange.ajax.importexport.actions.ICalImportRequest;
import com.openexchange.ajax.importexport.actions.ICalImportResponse;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.importexport.exceptions.ImportExportExceptionCodes;

/**
 * Test class for bug 10382.
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class Bug10382Test extends AbstractAJAXSession {

    private AJAXClient client;

    private AJAXClient client2;

    public Bug10382Test(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        client = getClient();
        client2 = new AJAXClient(User.User2);
    }

    @Override
    protected void tearDown() throws Exception {
        client2.logout();
        super.tearDown();
    }

    /**
     * Tries to import an iCal. TODO Use a user in a context with only webmail package available.
     *
     * @throws Throwable if an exception occurs.
     */
    public void testBug() throws Throwable {
        final Appointment appointment = new Appointment();
        appointment.setTitle("Bug10382Test");
        final ICalImportResponse response = Executor.execute(client, new ICalImportRequest(
            client2.getValues().getPrivateAppointmentFolder(),
            Tools.toICal(client, appointment),
            false));
        assertTrue("ICal imported without permissions.", response.hasError());
        final OXException exception = response.getException();
        assertNotNull("Expected exception but got nothing.", exception);
        assertTrue("Expected error code I_E-"+ImportExportExceptionCodes.NO_IMPORTER.getNumber()+" but got "+exception.getErrorCode(), exception.similarTo(ImportExportExceptionCodes.NO_IMPORTER));
    }
}
