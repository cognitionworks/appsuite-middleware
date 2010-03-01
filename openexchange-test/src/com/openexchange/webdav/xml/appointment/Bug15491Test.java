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

package com.openexchange.webdav.xml.appointment;

import static com.openexchange.groupware.calendar.TimeTools.D;
import java.util.Calendar;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.webdav.xml.AppointmentTest;

/**
 * {@link Bug15491Test}
 * 
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class Bug15491Test extends AppointmentTest {

    private Appointment appointment;
    private int objectId;

    /**
     * Initializes a new {@link Bug15491Test}.
     * 
     * @param name
     */
    public Bug15491Test(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        appointment = new Appointment();
        appointment.setTitle("testBug13262");
        appointment.setStartDate(D("10.10.2010 10:00"));
        appointment.setEndDate(D("10.10.2010 11:00"));
        appointment.setParentFolderID(appointmentFolderId);
        appointment.setIgnoreConflicts(true);
    }

    @Override
    protected void tearDown() throws Exception {
        if (objectId != -1) {
            deleteAppointment(getWebConversation(), objectId, appointmentFolderId, PROTOCOL + getHostName(), getLogin(), getPassword());
        }
        super.tearDown();
    }
    
    public void testRead() throws Exception {
        objectId = insertAppointment(getWebConversation(), appointment, PROTOCOL + getHostName(), getLogin(), getPassword());

        Appointment loadAppointment = loadAppointment(getWebConversation(), objectId, appointmentFolderId, getHostName(), getLogin(), getPassword());
        assertNotNull("Loaded Appointment is null", loadAppointment);
        
        assertNotNull("Uid is null", loadAppointment.getUid());
        assertFalse("Uid is empty", loadAppointment.getUid().trim().equals(""));
    }
    
    public void testWrite() throws Exception {
        appointment.setUid("ichbineineuid");
        objectId = insertAppointment(getWebConversation(), appointment, PROTOCOL + getHostName(), getLogin(), getPassword());

        Appointment loadAppointment = loadAppointment(getWebConversation(), objectId, appointmentFolderId, getHostName(), getLogin(), getPassword());
        assertNotNull("Loaded Appointment is null", loadAppointment);
        
        assertNotNull("Uid is null", loadAppointment.getUid());
        assertEquals("Wrong Uid", "ichbineineuid", loadAppointment.getUid());
    }
}
