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

package com.openexchange.ajax.appointment.bugtests;

import static com.openexchange.groupware.calendar.TimeTools.D;
import static org.junit.Assert.assertEquals;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.Participant;
import com.openexchange.server.impl.OCLPermission;

/**
 * {@link Bug26842Test}
 * 
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class Bug26842Test extends AbstractAJAXSession {

    private FolderObject folder;

    public Bug26842Test() {
        super();
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        folder = ftm.generatePublicFolder("26842-" + UUID.randomUUID().toString(), FolderObject.CALENDAR, FolderObject.SYSTEM_PUBLIC_FOLDER_ID);

        OCLPermission permission = new OCLPermission();
        permission.setEntity(getClient().getValues().getUserId());
        permission.setGroupPermission(false);
        permission.setFolderAdmin(true);
        permission.setAllPermission(OCLPermission.CREATE_OBJECTS_IN_FOLDER, OCLPermission.READ_ALL_OBJECTS, OCLPermission.WRITE_ALL_OBJECTS, OCLPermission.DELETE_ALL_OBJECTS);
        folder.addPermission(permission);

        ftm.insertFolderOnServer(folder);
    }

    @Test
    public void testBug() throws Exception {
        Appointment app = new Appointment();
        app.setTitle("Bug 26842 Test");
        app.setStartDate(D("29.05.2013 08:00"));
        app.setEndDate(D("29.05.2013 08:00"));
        app.setParticipants(new Participant[] {});
        app.setParentFolderID(folder.getObjectID());
        app.setIgnoreConflicts(true);

        catm.insert(app);
        Appointment appointment = catm.get(app.getParentFolderID(), app.getObjectID());
        assertEquals("Wrong participants.", getClient().getValues().getUserId(), appointment.getParticipants()[0].getIdentifier());
    }

}
