package com.openexchange.ajax.appointment;

import java.util.*;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.xml.sax.SAXException;

import com.openexchange.ajax.AppointmentTest;
import com.openexchange.ajax.appointment.action.AllRequest;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXSession;
import com.openexchange.ajax.framework.CommonAllResponse;
import com.openexchange.ajax.framework.Executor;
import com.openexchange.groupware.container.AppointmentObject;

import static com.openexchange.groupware.calendar.TimeTools.D;
import com.openexchange.tools.servlet.AjaxException;

public class AllTest extends AppointmentTest {

	private static final Log LOG = LogFactory.getLog(AllTest.class);

    private static final int[] SIMPLE_COLUMNS = new int[]{AppointmentObject.OBJECT_ID, AppointmentObject.FOLDER_ID, AppointmentObject.TITLE, AppointmentObject.START_DATE, AppointmentObject.END_DATE};

    public AllTest(final String name) {
		super(name);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
    }

    protected void tearDown() throws Exception {
        clean();
    }

    public void testShouldListAppointmentsInPrivateFolder() throws Exception{
        AppointmentObject appointment = new AppointmentObject();
        appointment.setStartDate(D("24/02/1998 12:00"));
        appointment.setEndDate(D("24/02/1998 14:00"));
        appointment.setTitle("Appointment 1 for All Test");
        appointment.setParentFolderID(appointmentFolderId);
        create( appointment );

        AppointmentObject anotherAppointment = new AppointmentObject();
        anotherAppointment.setStartDate(D("03/05/1999 10:00"));
        anotherAppointment.setEndDate(D("03/05/1999 10:30"));
        anotherAppointment.setTitle("Appointment 2 for All Test");
        anotherAppointment.setParentFolderID(appointmentFolderId);
        create( anotherAppointment );

        AllRequest all = new AllRequest(appointmentFolderId, SIMPLE_COLUMNS, D("01/01/1990 00:00"), D("01/01/2000 00:00"), utc);
        CommonAllResponse allResponse = getClient().execute(all);

        // Verify appointments are included in response
        JSONArray data = (JSONArray) allResponse.getData();
        assertInResponse(data, appointment, anotherAppointment);
    }

    public void testShouldOnlyListAppointmentsInSpecifiedTimeRange() throws JSONException, AjaxException, IOException, SAXException {
        AppointmentObject appointment = new AppointmentObject();
        appointment.setStartDate(D("24/02/1998 12:00"));
        appointment.setEndDate(D("24/02/1998 14:00"));
        appointment.setTitle("Appointment 1 for All Test");
        appointment.setParentFolderID(appointmentFolderId);
        create( appointment );

        AppointmentObject anotherAppointment = new AppointmentObject();
        anotherAppointment.setStartDate(D("03/05/1999 10:00"));
        anotherAppointment.setEndDate(D("03/05/1999 10:30"));
        anotherAppointment.setTitle("Appointment 2 for All Test");
        anotherAppointment.setParentFolderID(appointmentFolderId);
        create( anotherAppointment );

        AllRequest all = new AllRequest(appointmentFolderId, SIMPLE_COLUMNS, D("01/01/1999 00:00"), D("01/01/2000 00:00"), utc);
        CommonAllResponse allResponse = getClient().execute(all);

        // Verify appointments are included in response
        JSONArray data = (JSONArray) allResponse.getData();

        assertNotInResponse(data, appointment);
        assertInResponse(data, anotherAppointment);
                
    }

    private void assertInResponse(JSONArray data, AppointmentObject...appointments) throws JSONException {
        Set<Integer> expectedIds = new HashSet<Integer>();
        Map<Integer, AppointmentObject> id2appointment = new HashMap<Integer, AppointmentObject>();
        for(AppointmentObject appointment : appointments) {
            expectedIds.add(appointment.getObjectID());
            id2appointment.put(appointment.getObjectID(), appointment);
        }
        for(int i = 0, size = data.length(); i < size; i++) {
            JSONArray row = data.getJSONArray(i);

            int id = row.getInt(0);
            int folderId = row.getInt(1);
            String title = row.getString(2);
            long startDate = row.getLong(3);
            long endDate = row.getLong(4);

            AppointmentObject expectedAppointment = id2appointment.get(id);
            expectedIds.remove(id);

            if(expectedAppointment != null) {
                assertEquals(folderId, expectedAppointment.getParentFolderID());
                assertEquals(title, expectedAppointment.getTitle());
                assertEquals(startDate , expectedAppointment.getStartDate().getTime());
                assertEquals(endDate, expectedAppointment.getEndDate().getTime());
            }
        }

        assertTrue("Missing ids: "+expectedIds, expectedIds.isEmpty());
    }

    private void assertNotInResponse(JSONArray data, AppointmentObject...appointments) throws JSONException {
        Set<Integer> ids = new HashSet<Integer>();
        for(AppointmentObject appointment : appointments) {
            ids.add(appointment.getObjectID());
        }
        for(int i = 0, size = data.length(); i < size; i++) {
            JSONArray row = data.getJSONArray(i);

            int id = row.getInt(0);

            assertFalse(ids.contains(id));
        }
     }

    public void testShowAppointmentsBetween() throws Exception {
		final Date start = new Date(System.currentTimeMillis()-(dayInMillis*7));
		final Date end = new Date(System.currentTimeMillis()+(dayInMillis*7));
		
		final int cols[] = new int[]{ AppointmentObject.OBJECT_ID };
		
		final AppointmentObject[] appointmentArray = listAppointment(getWebConversation(), appointmentFolderId, cols, start, end, timeZone, false, PROTOCOL + getHostName(), getSessionId());
	}
	
	public void testShowAllAppointmentWhereIAmParticipant() throws Exception {
		final Date start = new Date(System.currentTimeMillis()-(dayInMillis*7));
		final Date end = new Date(System.currentTimeMillis()+(dayInMillis*7));
		
		final int cols[] = new int[]{ AppointmentObject.OBJECT_ID };
		
		final AppointmentObject[] appointmentArray = listAppointment(getWebConversation(), appointmentFolderId, cols, start, end, timeZone, true, PROTOCOL + getHostName(), getSessionId());
	}
	
	public void testShowFullTimeAppointments() throws Exception {
		final int cols[] = new int[]{ AppointmentObject.OBJECT_ID };
		
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.setTimeInMillis(startTime);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		final Date startDate = calendar.getTime();
		
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		
		final Date endDate = calendar.getTime();
		
		final AppointmentObject appointmentObj = createAppointmentObject("testShowFullTimeAppointments");
		appointmentObj.setStartDate(startDate);
		appointmentObj.setEndDate(endDate);
		appointmentObj.setFullTime(true);
		appointmentObj.setIgnoreConflicts(true);
		final int objectId = insertAppointment(getWebConversation(), appointmentObj, timeZone, getHostName(), getSessionId());
		
		calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		Date start = calendar.getTime();
		Date end = new Date(start.getTime()+dayInMillis);
		
		AppointmentObject[] appointmentArray = listAppointment(getWebConversation(), appointmentFolderId, cols, start, end, timeZone, false, getHostName(), getSessionId());
		boolean found = false;
		for (int a = 0; a < appointmentArray.length; a++) {
			if (appointmentArray[a].getObjectID() == objectId) {
				found = true;
			}
		}
		assertTrue("appointment not found in day view", found);
		
        // one day less
		start = new Date(calendar.getTimeInMillis()-dayInMillis);
		end = new Date(start.getTime()+dayInMillis);
		appointmentArray = listAppointment(getWebConversation(), appointmentFolderId, cols, start, end, timeZone, false, getHostName(), getSessionId());
		found = false;
		for (int a = 0; a < appointmentArray.length; a++) {
			if (appointmentArray[a].getObjectID() == objectId) {
				found = true;
			}
		}
		assertFalse("appointment found one day before start date in day view", found);
		
		// one day more
		start = new Date(calendar.getTimeInMillis()+dayInMillis);
		end = new Date(start.getTime()+dayInMillis);
		appointmentArray = listAppointment(getWebConversation(), appointmentFolderId, cols, start, end, timeZone, false, getHostName(), getSessionId());
		found = false;
		for (int a = 0; a < appointmentArray.length; a++) {
			if (appointmentArray[a].getObjectID() == objectId) {
				found = true;
			}
		}
		assertFalse("appointment found one day after start date in day view", found);

		deleteAppointment(getWebConversation(), objectId, appointmentFolderId, getHostName(), getSessionId());
	}

    // Bug 12171
    public void testShowOcurrences() throws Exception {
        final int cols[] = new int[]{ AppointmentObject.OBJECT_ID, AppointmentObject.RECURRENCE_COUNT};

        final AppointmentObject appointmentObj = createAppointmentObject("testShowOcurrences");
        appointmentObj.setStartDate(new Date());
        appointmentObj.setEndDate(new Date(System.currentTimeMillis() + 60*60*1000));
        appointmentObj.setRecurrenceType(AppointmentObject.DAILY);
        appointmentObj.setInterval(1);
        appointmentObj.setOccurrence(3);
        appointmentObj.setIgnoreConflicts(true);
        final int objectId = insertAppointment(getWebConversation(), appointmentObj, timeZone, getHostName(), getSessionId());

        final AppointmentObject[] appointmentArray = listAppointment(getWebConversation(), appointmentFolderId, cols, new Date(0), new Date(Long.MAX_VALUE), timeZone, false, getHostName(), getSessionId());

        for(final AppointmentObject loaded : appointmentArray) {
            if(loaded.getObjectID() == objectId) {
                assertEquals(appointmentObj.getOccurrence(), loaded.getOccurrence());
            }
        }
    }

    // Node 2652
    public void testLastModifiedUTC() throws Exception {
        final AJAXClient client = new AJAXClient(new AJAXSession(getWebConversation(), getSessionId()));
        final int cols[] = new int[]{ AppointmentObject.OBJECT_ID, AppointmentObject.FOLDER_ID, AppointmentObject.LAST_MODIFIED_UTC};

        final AppointmentObject appointmentObj = createAppointmentObject("testShowLastModifiedUTC");
        appointmentObj.setStartDate(new Date());
        appointmentObj.setEndDate(new Date(System.currentTimeMillis() + 60*60*1000));
        appointmentObj.setIgnoreConflicts(true);
        final int objectId = insertAppointment(getWebConversation(), appointmentObj, timeZone, getHostName(), getSessionId());
        try {
            final AllRequest req = new AllRequest(appointmentFolderId, cols, new Date(0), new Date(Long.MAX_VALUE), TimeZone.getTimeZone("UTC"));

            final CommonAllResponse response = Executor.execute(client, req);
            final JSONArray arr = (JSONArray) response.getResponse().getData();

            assertNotNull(arr);
            final int size = arr.length();
            assertTrue(size > 0);
            for(int i = 0; i < size; i++ ){
                final JSONArray objectData = arr.optJSONArray(i);
                assertNotNull(objectData);
                assertNotNull(objectData.opt(2));
            }
        } finally {
            deleteAppointment(getWebConversation(), objectId, appointmentFolderId, getHostName(), getSessionId());    
        }
    }
}
