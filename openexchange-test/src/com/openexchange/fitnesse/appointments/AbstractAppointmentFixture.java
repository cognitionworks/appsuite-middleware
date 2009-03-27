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

package com.openexchange.fitnesse.appointments;

import java.io.IOException;
import java.util.Map;
import org.json.JSONException;
import org.xml.sax.SAXException;
import com.openexchange.ajax.kata.Step;
import com.openexchange.ajax.kata.appointments.ParticipantComparisonFailure;
import com.openexchange.ajax.kata.appointments.UserParticipantComparisonFailure;
import com.openexchange.fitnesse.AbstractStepFixture;
import com.openexchange.fitnesse.environment.PrincipalResolver;
import com.openexchange.fitnesse.exceptions.FitnesseException;
import com.openexchange.fitnesse.wrappers.FixtureDataWrapper;
import com.openexchange.groupware.container.AppointmentObject;
import com.openexchange.groupware.container.Participant;
import com.openexchange.test.fixtures.AppointmentFixtureFactory;
import com.openexchange.test.fixtures.Fixture;
import com.openexchange.test.fixtures.FixtureException;
import com.openexchange.test.fixtures.Fixtures;
import com.openexchange.tools.servlet.AjaxException;


/**
 * {@link AbstractAppointmentFixture}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 *
 */
public abstract class AbstractAppointmentFixture extends AbstractStepFixture {

    @Override
    protected Step createStep(FixtureDataWrapper data) throws FixtureException, AjaxException, IOException, SAXException, JSONException, FitnesseException {
        String fixtureName = data.getFixtureName();
        AppointmentObject appointment = createAppointment(fixtureName, data);
        
        Step step = createStep(appointment, fixtureName, data.getExpectedError());
        return step;
    }
    
    /**
     * Creates an appointment via AppointmentFixtureFactory
     * @throws FitnesseException 
     */
    public AppointmentObject createAppointment(String fixtureName, FixtureDataWrapper data) throws FixtureException, AjaxException, IOException, SAXException, JSONException, FitnesseException{
        AppointmentFixtureFactory appointmentFixtureFactory = new AppointmentFixtureFactory(null, null);
        Map<String, Map<String, String>> fixtureMap = data.asFixtureMap("appointment");
        String participants = fixtureMap.get("appointment").remove("participants");
        Fixtures<AppointmentObject> fixtures = appointmentFixtureFactory.createFixture(fixtureName, fixtureMap);
        Fixture<AppointmentObject> entry = fixtures.getEntry("appointment");
        resolveParticipants(entry, participants);
        int folderId = getClient().getValues().getPrivateAppointmentFolder();
        return (AppointmentObject) addFolder(entry.getEntry(), data, folderId);
    }
   

    private void resolveParticipants(Fixture<AppointmentObject> entry, String participants) throws FitnesseException {
        String[] participantsList = participants.split("\\s*,\\s*");
        PrincipalResolver resolver = new PrincipalResolver( environment.getClient() );
        for(String participant: participantsList){
            Participant resolvedParticipant = resolver.resolveEntity(participant);
            entry.getEntry().addParticipant(resolvedParticipant);
        }
        
    }

    protected abstract Step createStep(AppointmentObject appointment, String fixtureName, String expectedError);

    
    public int findFailedFieldPosition(String expectedValue, Throwable t) {
        if(UserParticipantComparisonFailure.class.isInstance(t)) {
            return data.getHeader().indexOf("users");
        }
        if(ParticipantComparisonFailure.class.isInstance(t)) {
            return data.getHeader().indexOf("participants");
        }
        return super.findFailedFieldPosition(expectedValue, t);
    }

}
