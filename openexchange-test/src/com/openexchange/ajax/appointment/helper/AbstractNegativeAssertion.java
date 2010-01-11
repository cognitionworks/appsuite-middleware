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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.ajax.appointment.helper;

import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.Changes;
import com.openexchange.test.CalendarTestManager;

/**
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 */
public abstract class AbstractNegativeAssertion extends AbstractAssertion {

    public AbstractNegativeAssertion(CalendarTestManager manager, int folderToWorkIn) {
        super();
        this.manager = manager;
        manager.setFailOnError(false);
        this.folder = folderToWorkIn;
    }

    public void check(Changes changes, OXError expectedError){
        check(generateDefaultAppointment(), changes, expectedError);
    }
    
    public abstract void check(Appointment startWith, Changes changes, OXError expectedError);

    protected void createAndCheck(Appointment startWith, Changes changes, OXError expectedError) {
        approachUsedForTest = "Create directly";

        changes.update(startWith);
        create(startWith);

        checkForError(expectedError);
    }

    protected void updateAndCheck(Appointment startWith, Changes changes, OXError expectedError) {
        approachUsedForTest = "Create and update";

        create(startWith);
        update(startWith, changes);

        checkForError(expectedError);
    }

    private void checkForError(OXError expectedError) {
        methodUsedForTest = "Check lastException field";
        assertTrue(state() + " Expecting exception, did not get one", manager.hasLastException());
        try {
            AbstractOXException lastException = (AbstractOXException) manager.getLastException();
            assertTrue(
                state() + " Expected error: " + expectedError + ", actual error: " + lastException.getErrorCode(),
                expectedError.matches(lastException));
        } catch (ClassCastException e) {
            fail2("Should have an OXException, but could not cast it into one");
        }
    }
}
