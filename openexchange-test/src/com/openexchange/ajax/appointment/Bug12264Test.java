package com.openexchange.ajax.appointment;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import com.openexchange.ajax.appointment.action.DeleteRequest;
import com.openexchange.ajax.appointment.action.GetRequest;
import com.openexchange.ajax.appointment.action.GetResponse;
import com.openexchange.ajax.appointment.action.InsertRequest;
import com.openexchange.ajax.appointment.action.UpdateRequest;
import com.openexchange.ajax.appointment.action.UpdateResponse;
import com.openexchange.ajax.fields.AppointmentFields;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.framework.CommonInsertResponse;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.calendar.OXCalendarException;
import com.openexchange.groupware.calendar.TimeTools;
import com.openexchange.groupware.container.AppointmentObject;

/**
 * 
 * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
 *
 */
public class Bug12264Test extends AbstractAJAXSession {
    
    //Global fields, because we are handling only one appointment in one folder, all the time.
    private AJAXClient client = null;
    private int objectId = 0;
    private int folderId = 0;
    private Date lastModified = null;
    private TimeZone tz = null;
    private AppointmentObject appointment = null;

    public Bug12264Test(String name) {
        super(name);
    }
    
    //Tests
    public void testSetUntilToNullWithExistingOccurrences() throws Throwable {
        prepareWithOccurrences("bug 12264 test - set until to null with existing occurrences");
        
        try {
            //Insert appointment
            insertAppointment();
            
            //Update appointment with until = null
            appointment.removeOccurrence();
            UpdateRequest updateRequest = new UntilNullAppointmentUpdateRequest(appointment, tz);
            UpdateResponse updateResponse = client.execute(updateRequest);
            appointment.setLastModified(updateResponse.getTimestamp());
            lastModified = appointment.getLastModified();
            
            //Load and check appointment
            getAppointment();
            checkForNoEnd();
        } finally {
            cleanUp();
        }
    }
    
    public void testSetUntilToNullWithExistingUntil() throws Throwable {
        prepareWithUntil("bug 12264 test - set until to null with existing until");
        
        try {
            //Insert appointment
            insertAppointment();
            
            //Update appointment with until = null
            appointment.removeOccurrence();
            UpdateRequest updateRequest = new UntilNullAppointmentUpdateRequest(appointment, tz);
            UpdateResponse updateResponse = client.execute(updateRequest);
            appointment.setLastModified(updateResponse.getTimestamp());
            lastModified = appointment.getLastModified();
            
            //Load and check appointment
            getAppointment();
            checkForNoEnd();
        } finally {
            cleanUp();
        }
    }
    
    public void testSetOccurrencesTo0WithExistingOccurrences() throws Throwable {
        prepareWithOccurrences("bug 12264 test - set occurrences to 0 with existing occurrences");
        
        try {
            //Insert appointment
            insertAppointment();
            
            //Update appointment with occurrences = 0
            appointment.setOccurrence(0);
            updateAppointment(true);
            
            //Load and check appointment
            getAppointment();
            checkForNoEnd();
        } finally {
            cleanUp();
        }
    }
    
    public void testSetOccurrencesTo0WithExistingUntil() throws Throwable {
        prepareWithUntil("bug 12264 test - set occurrences to 0 with existing until");
        
        try {
            //Insert appointment
            insertAppointment();
            
            //Update appointment with occurrences = 0
            appointment.setOccurrence(0);
            appointment.removeUntil();
            updateAppointment(true);
            
            //Load and check appointment
            getAppointment();
            checkForNoEnd();
        } finally {
            cleanUp();
        }
    }
    
    public void testSetOccurrencesToNullWithExistingOccurrences() throws Throwable {
        prepareWithOccurrences("bug 12264 test - set occurrences to null with existing occurrences");
        
        try {
            //Insert appointment
            insertAppointment();
            
            //Update appointment with occurrences = null
            appointment.removeOccurrence();
            UpdateRequest updateRequest = new OccurrencesNullAppointmentUpdateRequest(appointment, tz);
            UpdateResponse updateResponse = client.execute(updateRequest);
            appointment.setLastModified(updateResponse.getTimestamp());
            lastModified = appointment.getLastModified();
            
            //Load and check appointment
            getAppointment();
            checkForNoEnd();
        } finally {
            cleanUp();
        }
    }
    
    public void testSetOccurrencesToNullWithExistingUntil() throws Throwable {
        prepareWithUntil("bug 12264 test - set occurrences to null with existing until");
        
        try {
            //Insert appointment
            insertAppointment();
            
            //Update appointment with occurrences = null
            appointment.removeOccurrence();
            appointment.removeUntil();
            UpdateRequest updateRequest = new OccurrencesNullAppointmentUpdateRequest(appointment, tz);
            UpdateResponse updateResponse = client.execute(updateRequest);
            appointment.setLastModified(updateResponse.getTimestamp());
            lastModified = appointment.getLastModified();
            
            //Load and check appointment
            getAppointment();
            checkForNoEnd();
        } finally {
            cleanUp();
        }
    }
    
    public void testBugAsWritten() throws Throwable {
        prepareWithOccurrences("bug 12264 test - as written");
        
        try {
            //Steps 1,2
            //Remove recurrence Information to get a single appointment.
            appointment.removeRecurrenceType();
            appointment.removeDays();
            appointment.removeInterval();
            appointment.removeOccurrence();
    
            insertAppointment();
            
            //Steps 3,4,5
            //Change to sequence, weekly, wednesday, ends after 2 occurrences
            appointment.setRecurrenceType(AppointmentObject.WEEKLY);
            appointment.setDays(AppointmentObject.WEDNESDAY);
            appointment.setInterval(1);
            appointment.setOccurrence(2);
            
            updateAppointment(true);
            
            //Load appointment and check occurrence value
            getAppointment();
            assertEquals("Unexpected occurrence value", 2, appointment.getOccurrence());
            
            //Steps 6,7
            //Change until to "null", that will be done via Special Request.
            appointment.removeOccurrence();
            UpdateRequest updateRequest = new UntilNullAppointmentUpdateRequest(appointment, tz);
            UpdateResponse updateResponse = client.execute(updateRequest);
            appointment.setLastModified(updateResponse.getTimestamp());
            lastModified = appointment.getLastModified();
            
            //Load appointment and check occurrence value
            getAppointment();
            checkForNoEnd();
        } finally {
            cleanUp();
        }
        
    }
    
    public void testBugAsWrittenAccordingComment11() throws Throwable {
       prepareWithOccurrences("bug 12264 test - as written, comment 11");
       
       try {
           insertAppointment();
           appointment.setUntil(new Date(0));
           UpdateResponse response = updateAppointment(false);
           assertTrue(response.hasError());
           AbstractOXException exception = response.getException();
           assertEquals("Wrong exception thrown.", OXCalendarException.Code.UNTIL_BEFORE_START_DATE.getDetailNumber(), exception.getDetailNumber());
           assertFalse("No occurrence left.", appointment.getOccurrence() == 0);
       } finally {
           cleanUp();
       }
    }
    
    //Private stuff
    private void checkForNoEnd() throws Throwable {
        if(appointment.containsUntil()) {
            assertNull("Until exists and is not null. Until: " + appointment.getUntil(), appointment.getUntil());
        }
        
        if(appointment.containsOccurrence()) {
            assertEquals("Occurrences exist and is not 0. Occurrences: " + appointment.getOccurrence(), 0, appointment.getOccurrence());
        }
    }
    
    private void insertAppointment() throws Throwable {
        final InsertRequest request = new InsertRequest(appointment, tz);
        final CommonInsertResponse response = client.execute(request);
        appointment.setObjectID(response.getId());
        appointment.setLastModified(response.getTimestamp());
        objectId = appointment.getObjectID();
        lastModified = appointment.getLastModified();
    }
    
    private UpdateResponse updateAppointment(boolean failOnError) throws Throwable {
        final UpdateRequest updateRequest = new UpdateRequest(appointment, tz, failOnError);
        final UpdateResponse updateResponse = client.execute(updateRequest);
        if (updateResponse.getTimestamp() != null) {
            appointment.setLastModified(updateResponse.getTimestamp());
        }
        lastModified = appointment.getLastModified();
        return updateResponse;
    }
    
    private void getAppointment() throws Throwable {
        final GetRequest getRequest = new GetRequest(folderId, appointment.getObjectID());
        final GetResponse getResponse = client.execute(getRequest);
        appointment =  getResponse.getAppointment(tz);
    }
    
    private void prepareWithOccurrences(String title) throws Throwable {
        client = getClient();
        tz = client.getValues().getTimeZone();
        folderId = client.getValues().getPrivateAppointmentFolder();
        
        appointment = new AppointmentObject();
        appointment.setTitle(title);
        appointment.setParentFolderID(folderId);
        appointment.setIgnoreConflicts(true);
        final Calendar calendar = TimeTools.createCalendar(tz);
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        appointment.setStartDate(calendar.getTime());
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        appointment.setEndDate(calendar.getTime());
        appointment.setRecurrenceType(AppointmentObject.WEEKLY);
        appointment.setDays(AppointmentObject.WEDNESDAY);
        appointment.setInterval(1);
        appointment.setOccurrence(2);
    }
    
    private void prepareWithUntil(String title) throws Throwable {
        client = getClient();
        tz = client.getValues().getTimeZone();
        folderId = client.getValues().getPrivateAppointmentFolder();
        
        appointment = new AppointmentObject();
        appointment.setTitle(title);
        appointment.setParentFolderID(folderId);
        appointment.setIgnoreConflicts(true);
        final Calendar calendar = TimeTools.createCalendar(tz);
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        appointment.setStartDate(calendar.getTime());
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        appointment.setEndDate(calendar.getTime());
        appointment.setRecurrenceType(AppointmentObject.WEEKLY);
        appointment.setDays(AppointmentObject.WEDNESDAY);
        appointment.setInterval(1);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, 7*3);
        appointment.setUntil(calendar.getTime());
    }
    
    private void cleanUp() throws Throwable {
        if (objectId != 0 && folderId != 0 && lastModified != null && client != null) {
            DeleteRequest deleteRequest = new DeleteRequest(objectId, folderId, lastModified);
            client.execute(deleteRequest);
        }
        client = null;
        objectId = 0;
        folderId = 0;
        lastModified = null;
        tz = null;
        appointment = null;
    }

    /**
     * A special UpdateRequest for Appointments, which has null in the field until.
     * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
     *
     */
    private class UntilNullAppointmentUpdateRequest extends UpdateRequest {

        public UntilNullAppointmentUpdateRequest(AppointmentObject appointmentObj, TimeZone timeZone) {
            super(appointmentObj, timeZone);
        }
        
        public JSONObject getBody() throws JSONException {
            final JSONObject json = super.getBody();
            json.put(AppointmentFields.UNTIL, JSONObject.NULL);
            return json;
        }
        
    }
    
    /**
     * A special UpdateRequest for Appointments, which has null in the field occurrences.
     * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
     *
     */
    private class OccurrencesNullAppointmentUpdateRequest extends UpdateRequest {
        public OccurrencesNullAppointmentUpdateRequest(AppointmentObject appointmentObj, TimeZone timeZone) {
            super(appointmentObj, timeZone);
        }
        @Override
        public JSONObject getBody() throws JSONException {
            final JSONObject json = super.getBody();
            json.put(AppointmentFields.OCCURRENCES, JSONObject.NULL);
            return json;
        }
    }

}
