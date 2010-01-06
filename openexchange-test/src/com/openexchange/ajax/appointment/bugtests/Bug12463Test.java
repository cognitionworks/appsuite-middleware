package com.openexchange.ajax.appointment.bugtests;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.openexchange.ajax.appointment.action.DeleteRequest;
import com.openexchange.ajax.appointment.action.GetRequest;
import com.openexchange.ajax.appointment.action.GetResponse;
import com.openexchange.ajax.appointment.action.InsertRequest;
import com.openexchange.ajax.appointment.action.ListRequest;
import com.openexchange.ajax.appointment.action.UpdateRequest;
import com.openexchange.ajax.appointment.action.UpdateResponse;
import com.openexchange.ajax.appointment.action.UpdatesRequest;
import com.openexchange.ajax.appointment.action.UpdatesResponse;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.framework.CommonInsertResponse;
import com.openexchange.groupware.calendar.TimeTools;
import com.openexchange.groupware.container.Appointment;

/**
 * Checks, if a change of the time of a sequence forces deletion of all exceptions.
 * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
 *
 */
public final class Bug12463Test extends AbstractAJAXSession {

    public Bug12463Test(String name) {
        super(name);
    }
    
    public void testBugAsWritte() throws Throwable {
        final AJAXClient client = getClient();
        final int folderId = client.getValues().getPrivateAppointmentFolder();
        final TimeZone tz = client.getValues().getTimeZone();
        final Appointment sequence = new Appointment();
        int objectId = 0;
        Date lastModified = null;
        
        try {
            
            //Step 1
            //Prepare appointment
            sequence.setTitle("Bug 12463 Test - Sequence");
            sequence.setParentFolderID(folderId);
            sequence.setIgnoreConflicts(true);
            final Calendar calendar = TimeTools.createCalendar(tz);
            calendar.set(Calendar.HOUR_OF_DAY, 8);
            sequence.setStartDate(calendar.getTime());
            calendar.set(Calendar.HOUR_OF_DAY, 9);
            sequence.setEndDate(calendar.getTime());
            sequence.setRecurrenceType(Appointment.DAILY);
            sequence.setInterval(1);
            
            //Insert
            final InsertRequest insertRequest = new InsertRequest(sequence, tz);
            final CommonInsertResponse insertResponse = client.execute(insertRequest);
            sequence.setObjectID(insertResponse.getId());
            sequence.setLastModified(insertResponse.getTimestamp());
            objectId = sequence.getObjectID();
            sequence.setObjectID(objectId);
            lastModified = sequence.getLastModified();
            
            //Step 2
            //Load occurrence for changing
            GetRequest getRequest= new GetRequest(folderId, sequence.getObjectID(), 3);
            GetResponse getResponse = client.execute(getRequest);
            Appointment occurrence = getResponse.getAppointment(tz);
            
            //Create exception
            Appointment exception = new Appointment();
            exception.setObjectID(occurrence.getObjectID());
            exception.setParentFolderID(folderId);
            exception.setLastModified(occurrence.getLastModified());
            exception.setRecurrencePosition(occurrence.getRecurrencePosition());
            exception.setIgnoreConflicts(true);
            calendar.setTime(occurrence.getEndDate());
            exception.setStartDate(calendar.getTime());
            calendar.add(Calendar.HOUR, 1);
            exception.setEndDate(calendar.getTime());
            
            //Update occurrence
            UpdateRequest updateRequest = new UpdateRequest(exception, tz);
            UpdateResponse updateResponse = client.execute(updateRequest);
            exception.setLastModified(updateResponse.getTimestamp());
            Date lastModifiedOfOccurenceUpdate = exception.getLastModified();
            lastModified = exception.getLastModified();
            
            //Step 3
            //Create whole sequence change
            Appointment changeSequence = new Appointment();
            changeSequence.setIgnoreConflicts(true);
            changeSequence.setObjectID(sequence.getObjectID());
            changeSequence.setParentFolderID(sequence.getParentFolderID());
            changeSequence.setLastModified(exception.getLastModified());
            changeSequence.setRecurrenceType(Appointment.DAILY);
            changeSequence.setInterval(1);
            calendar.setTime(sequence.getStartDate());
            calendar.add(Calendar.HOUR_OF_DAY, -1);
            changeSequence.setStartDate(calendar.getTime());
            calendar.setTime(sequence.getEndDate());
            calendar.add(Calendar.HOUR_OF_DAY, -1);
            changeSequence.setEndDate(calendar.getTime());
            
            //Update sequence
            updateRequest = new UpdateRequest(changeSequence, tz);
            updateResponse = client.execute(updateRequest);
            changeSequence.setLastModified(updateResponse.getTimestamp());
            lastModified = changeSequence.getLastModified();
            
            //Load occurrence again
            getRequest= new GetRequest(folderId, sequence.getObjectID(), 3);
            getResponse = client.execute(getRequest);
            occurrence = getResponse.getAppointment(tz);
            
            //Check time of occurrence
            calendar.setTime(changeSequence.getStartDate());
            final int sequenceStartTime = calendar.get(Calendar.HOUR_OF_DAY);
            calendar.setTime(changeSequence.getEndDate());
            final int sequenceEndTime = calendar.get(Calendar.HOUR_OF_DAY);
            calendar.setTime(occurrence.getStartDate());
            final int occurrenceStartTime = calendar.get(Calendar.HOUR_OF_DAY);
            calendar.setTime(occurrence.getEndDate());
            final int occurrenceEndTime = calendar.get(Calendar.HOUR_OF_DAY);
            assertEquals("Start time does not match sequence start time", sequenceStartTime, occurrenceStartTime);
            assertEquals("End time does not match sequence end time", sequenceEndTime, occurrenceEndTime);
            
            //Check if sequence still has any exceptions
            int[] columns = new int[]{
                Appointment.START_DATE,
                Appointment.END_DATE,
                Appointment.OBJECT_ID,
                Appointment.RECURRENCE_ID
            };
            UpdatesRequest updatesRequest = new UpdatesRequest(folderId, columns, lastModifiedOfOccurenceUpdate, true);
            UpdatesResponse updatesResponse = client.execute(updatesRequest);
            List<Appointment> appointments = updatesResponse.getAppointments(tz);
            for(Appointment current: appointments) {
                if(current.getObjectID() != sequence.getObjectID() && current.getRecurrenceID() == sequence.getObjectID()) {
                    fail("Found exception of sequence.");
                }
            }
            
        } finally {
            
            if(objectId != 0 && lastModified != null) {
                final DeleteRequest deleteRequest = new DeleteRequest(objectId, folderId, lastModified);
                client.execute(deleteRequest);
            }
            
        }
    }

}
