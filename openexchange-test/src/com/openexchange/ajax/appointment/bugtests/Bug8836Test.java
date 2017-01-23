
package com.openexchange.ajax.appointment.bugtests;

import java.util.Date;
import org.junit.Test;
import com.openexchange.ajax.AppointmentTest;
import com.openexchange.groupware.container.Appointment;

public class Bug8836Test extends AppointmentTest {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Bug8836Test.class);

    @Test
    public void testBug8836() throws Exception {
        final Appointment appointmentObj = createAppointmentObject("testBug8836");
        appointmentObj.setIgnoreConflicts(true);
        final int objectId = insertAppointment(getWebConversation(), appointmentObj, timeZone, getHostName(), getSessionId());

        appointmentObj.setObjectID(objectId);
        appointmentObj.setPrivateFlag(true);

        Appointment loadAppointment = loadAppointment(getWebConversation(), objectId, appointmentFolderId, timeZone, getHostName(), getSessionId());
        Date modified = new Date(loadAppointment.getLastModified().getTime() + 1);

        updateAppointment(getWebConversation(), appointmentObj, objectId, appointmentFolderId, modified, timeZone, getHostName(), getSessionId());

        loadAppointment = loadAppointment(getWebConversation(), objectId, appointmentFolderId, timeZone, getHostName(), getSessionId());
        modified = new Date(loadAppointment.getLastModified().getTime() + 1);

        compareObject(appointmentObj, loadAppointment);

        deleteAppointment(getWebConversation(), objectId, appointmentFolderId, modified, getHostName(), getSessionId(), false);
    }
}
