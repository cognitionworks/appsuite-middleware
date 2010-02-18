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

package com.openexchange.ajax.appointment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.appointment.action.AllRequest;
import com.openexchange.ajax.appointment.action.DeleteRequest;
import com.openexchange.ajax.appointment.action.GetRequest;
import com.openexchange.ajax.appointment.action.GetResponse;
import com.openexchange.ajax.appointment.action.InsertRequest;
import com.openexchange.ajax.appointment.action.ListRequest;
import com.openexchange.ajax.appointment.action.SearchRequest;
import com.openexchange.ajax.appointment.action.SearchResponse;
import com.openexchange.ajax.appointment.action.UpdateRequest;
import com.openexchange.ajax.appointment.action.UpdateResponse;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.framework.CommonAllResponse;
import com.openexchange.ajax.framework.CommonListResponse;
import com.openexchange.ajax.framework.ListIDs;
import com.openexchange.ajax.parser.ParticipantParser;
import com.openexchange.groupware.calendar.TimeTools;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.ExternalUserParticipant;
import com.openexchange.groupware.container.participants.ConfirmStatus;
import com.openexchange.groupware.container.participants.ConfirmableParticipant;

/**
 * Checks if the calendar component correctly fills the confirmations JSON appointment attributes.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class ConfirmationsTest extends AbstractAJAXSession {

    private static final int[] COLUMNS = { Appointment.OBJECT_ID, Appointment.FOLDER_ID, Appointment.CONFIRMATIONS };
    private AJAXClient client;
    private int folderId;
    private TimeZone tz;
    private Appointment appointment;
    private ExternalUserParticipant participant;

    public ConfirmationsTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        client = getClient();
        folderId = client.getValues().getPrivateAppointmentFolder();
        tz = client.getValues().getTimeZone();
        appointment = new Appointment();
        appointment.setTitle("Test appointment for testing confirmations");
        Calendar calendar = TimeTools.createCalendar(tz);
        appointment.setStartDate(calendar.getTime());
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        appointment.setEndDate(calendar.getTime());
        appointment.setParentFolderID(folderId);
        appointment.setIgnoreConflicts(true);
        participant = new ExternalUserParticipant("external1@example.com");
        participant.setDisplayName("External user");
        appointment.addParticipant(participant);
        client.execute(new InsertRequest(appointment, tz)).fillAppointment(appointment);
    }

    @Override
    protected void tearDown() throws Exception {
        client.execute(new DeleteRequest(appointment));
        super.tearDown();
    }

    public void testGet() throws Throwable {
        GetResponse response = client.execute(new GetRequest(appointment));
        Appointment test = response.getAppointment(tz);
        ConfirmableParticipant[] confirmations = test.getConfirmations();
        checkConfirmations(confirmations);
    }

    private void checkConfirmations(ConfirmableParticipant[] confirmations) {
        assertNotNull("Response does not contain any confirmations.", confirmations);
        // Following expected must be 2 if internal user participants get its way into the confirmations array.
        assertEquals("Number of external participant confirmations does not match.", 1, confirmations.length);
        assertEquals("Mailaddress of external participant does not match.", participant.getEmailAddress(), confirmations[0].getEmailAddress());
        assertEquals("Display name of external participant does not match.", participant.getDisplayName(), confirmations[0].getDisplayName());
        assertEquals("Confirm status does not match.", ConfirmStatus.NONE, confirmations[0].getStatus());
        assertEquals("Confirm message does not match.", participant.getMessage(), confirmations[0].getMessage());
    }

    public void testAll() throws Throwable {
        CommonAllResponse response = client.execute(new AllRequest(folderId, COLUMNS, appointment.getStartDate(), appointment.getEndDate(), tz));
        int objectIdPos = response.getColumnPos(Appointment.OBJECT_ID);
        int confirmationsPos = response.getColumnPos(Appointment.CONFIRMATIONS);
        JSONArray jsonConfirmations = null;
        for (Object[] tmp : response) {
            if (appointment.getObjectID() == ((Integer) tmp[objectIdPos]).intValue()) {
                jsonConfirmations = (JSONArray) tmp[confirmationsPos];
            }
        }
        checkConfirmations(jsonConfirmations);
    }

    public void testList() throws Throwable {
        CommonListResponse response = client.execute(new ListRequest(ListIDs.l(new int[] { folderId, appointment.getObjectID() }), COLUMNS));
        int objectIdPos = response.getColumnPos(Appointment.OBJECT_ID);
        int confirmationsPos = response.getColumnPos(Appointment.CONFIRMATIONS);
        JSONArray jsonConfirmations = null;
        for (Object[] tmp : response) {
            if (appointment.getObjectID() == ((Integer) tmp[objectIdPos]).intValue()) {
                jsonConfirmations = (JSONArray) tmp[confirmationsPos];
            }
        }
        checkConfirmations(jsonConfirmations);
    }

    public void testSearch() throws Throwable {
        SearchResponse response = client.execute(new SearchRequest("*", folderId, COLUMNS));
        int objectIdPos = response.getColumnPos(Appointment.OBJECT_ID);
        int confirmationsPos = response.getColumnPos(Appointment.CONFIRMATIONS);
        JSONArray jsonConfirmations = null;
        for (Object[] tmp : response) {
            if (appointment.getObjectID() == ((Integer) tmp[objectIdPos]).intValue()) {
                jsonConfirmations = (JSONArray) tmp[confirmationsPos];
            }
        }
        checkConfirmations(jsonConfirmations);
    }

    private void checkConfirmations(JSONArray jsonConfirmations) throws JSONException {
        assertNotNull("Response does not contain confirmations.", jsonConfirmations);
        ParticipantParser parser = new ParticipantParser();
        List<ConfirmableParticipant> confirmations = new ArrayList<ConfirmableParticipant>();
        int length = jsonConfirmations.length();
        for (int i = 0; i < length; i++) {
            JSONObject jsonConfirmation = jsonConfirmations.getJSONObject(i);
            confirmations.add(parser.parseConfirmation(true, jsonConfirmation));
        }
        checkConfirmations(confirmations.toArray(new ConfirmableParticipant[confirmations.size()]));
    }

    public void testUpdate() throws Throwable {
        Appointment updated = new Appointment();
        updated.setObjectID(appointment.getObjectID());
        updated.setParentFolderID(appointment.getParentFolderID());
        updated.setLastModified(appointment.getLastModified());
        updated.setTitle("Updated test appointment for testing confirmations");
        participant = new ExternalUserParticipant("external1@example.com");
        participant.setDisplayName("External user");
        updated.addParticipant(participant);
        participant = new ExternalUserParticipant("external2@example.com");
        participant.setDisplayName("External user 2");
        updated.addParticipant(participant);
        UpdateResponse response = client.execute(new UpdateRequest(updated, tz));
        appointment.setLastModified(response.getTimestamp());
        GetResponse response2 = client.execute(new GetRequest(appointment));
        checkConfirmations(response2.getAppointment(tz).getConfirmations());
    }
}
