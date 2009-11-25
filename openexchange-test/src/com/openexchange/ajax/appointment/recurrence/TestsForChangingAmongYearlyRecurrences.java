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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.ajax.appointment.recurrence;

import java.io.IOException;
import org.json.JSONException;
import org.xml.sax.SAXException;
import com.openexchange.ajax.appointment.helper.AbstractAssertion;
import com.openexchange.ajax.appointment.helper.Changes;
import com.openexchange.ajax.appointment.helper.Expectations;
import com.openexchange.ajax.appointment.helper.OXError;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.tools.servlet.AjaxException;

/**
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 */
public class TestsForChangingAmongYearlyRecurrences extends ManagedAppointmentTest {

    public TestsForChangingAmongYearlyRecurrences(String name) {
        super(name);
    }

    private Appointment generateYearlyAppointment() throws AjaxException, IOException, SAXException, JSONException {
        Appointment app = AbstractAssertion.generateDefaultAppointment(calendarManager.getPrivateFolder());
        app.set(Appointment.RECURRENCE_TYPE, Appointment.YEARLY);
        app.set(Appointment.INTERVAL, 1);
        app.set(Appointment.DAY_IN_MONTH, 1);
        app.set(Appointment.MONTH, 1);
        return app;
    }

    public void testShouldChangeFromYearly1ToYearly2() throws Exception {
        Appointment app = generateYearlyAppointment();

        Changes changes = new Changes();
        changes.put(Appointment.RECURRENCE_TYPE, Appointment.YEARLY);
        changes.put(Appointment.INTERVAL, 1);
        changes.put(Appointment.DAY_IN_MONTH, 1);
        changes.put(Appointment.MONTH, 1);
        changes.put(Appointment.DAYS, Appointment.MONDAY); // this is the actual change

        Expectations expectations = new Expectations(changes);

        positiveAssertion.check(app, changes, expectations);
    }

    public void testShouldFailChangingFromYearly1ToYearly2UsingOnlyAdditionalData() throws Exception {
        Appointment app = generateYearlyAppointment();

        Changes changes = new Changes();
        changes.put(Appointment.DAYS, Appointment.MONDAY);

        negativeAssertionOnUpdate.check(app, changes, new OXError("APP", 999));
    }

    public void testShouldChangeFromYearly2ToYearly1With127() throws Exception {
        Appointment app = generateYearlyAppointment();

        Changes changes = new Changes();
        changes.put(Appointment.RECURRENCE_TYPE, Appointment.YEARLY);
        changes.put(Appointment.DAYS, 127); // DUH!
        changes.put(Appointment.INTERVAL, 1);
        changes.put(Appointment.DAY_IN_MONTH, 1);
        changes.put(Appointment.MONTH, 1);

        Expectations expectations = new Expectations(changes);

        positiveAssertion.check(app, changes, expectations);
    }

    public void testShouldChangeFromYearly2ToYearly1WithNull() throws Exception {
        Appointment app = generateYearlyAppointment();

        Changes changes = new Changes();
        changes.put(Appointment.RECURRENCE_TYPE, Appointment.YEARLY);
        changes.put(Appointment.DAYS, null);
        changes.put(Appointment.INTERVAL, 1);
        changes.put(Appointment.DAY_IN_MONTH, 1);
        changes.put(Appointment.MONTH, 1);

        Expectations expectations = new Expectations(changes);
        expectations.put(Appointment.DAYS, 127);

        positiveAssertion.check(app, changes, expectations);
    }

    public void testShouldFailChangingFromYearly2ToYearly1WhileMissingMonth() throws Exception {
        Appointment app = generateYearlyAppointment();

        Changes changes = new Changes();
        changes.put(Appointment.RECURRENCE_TYPE, Appointment.YEARLY);
        changes.put(Appointment.DAYS, 1);
        changes.put(Appointment.INTERVAL, 1);
        changes.put(Appointment.DAY_IN_MONTH, 1);

        negativeAssertionOnUpdate.check(app, changes, new OXError("APP",91));
    }
    
    public void testShouldFailChangingFromYearly2ToYearly1UsingOnlyAdditionalData() throws Exception {
        Appointment app = generateYearlyAppointment();

        Changes changes = new Changes();
        changes.put(Appointment.DAYS, 127);

        negativeAssertionOnUpdate.check(app, changes, new OXError("APP", 999));
    }

}
