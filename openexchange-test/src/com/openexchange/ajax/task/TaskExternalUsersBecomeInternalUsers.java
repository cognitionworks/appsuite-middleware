
package com.openexchange.ajax.task;

import java.util.TimeZone;
import com.openexchange.ajax.contact.action.GetContactForUserRequest;
import com.openexchange.ajax.contact.action.GetResponse;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXClient.User;
import com.openexchange.groupware.container.ExternalUserParticipant;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.groupware.tasks.Task;

public class TaskExternalUsersBecomeInternalUsers extends ManagedTaskTest {

    public TaskExternalUsersBecomeInternalUsers(String name) {
        super();
    }

    public void testExternalParticipantBecomesUserParticipantIfAddressMatches() throws Exception {
        AJAXClient client2 = new AJAXClient(User.User2);
        int user2id = client2.getValues().getUserId();
        GetResponse response = client2.execute(new GetContactForUserRequest(user2id, true, TimeZone.getDefault()));
        String user2email = response.getContact().getEmail1();

        Task task = generateTask("Task to test the transformation of external participants into internal ones");
        task.addParticipant(new ExternalUserParticipant(user2email));
        task.addParticipant(new UserParticipant(user2id));

        manager.insertTaskOnServer(task);
        Task actual = manager.getTaskFromServer(task);

        boolean foundAsExternal = false, foundAsInternal = false;

        Participant[] participants = actual.getParticipants();
        for (Participant participant : participants) {
            if (participant.getType() == Participant.EXTERNAL_USER) {
                if (participant.getEmailAddress().equals(user2email)) {
                    foundAsExternal = true;
                }
            }
            if (participant.getType() == Participant.USER) {
                if (participant.getIdentifier() == user2id) {
                    foundAsInternal = true;
                }
            }
        }
        assertFalse("Should not find user listed as external participant", foundAsExternal);
        assertTrue("Should find user listed as internal participant", foundAsInternal);
    }

    public void testExternalParticipantIsRemovedIfAddressMatchesUserParticipant() throws Exception {
        int userId = getClient().getValues().getUserId();

        GetResponse response = getClient().execute(new GetContactForUserRequest(userId, true, TimeZone.getDefault()));
        String user1email = response.getContact().getEmail1();

        Task task = generateTask("Another task to test the transformation of external participants into internal ones");
        task.addParticipant(new ExternalUserParticipant(user1email));

        manager.insertTaskOnServer(task);
        Task actual = manager.getTaskFromServer(task);

        boolean foundAsExternal = false;
        int foundAsInternal = 0;

        Participant[] participants = actual.getParticipants();
        for (Participant participant : participants) {
            if (participant.getType() == Participant.EXTERNAL_USER) {
                if (participant.getEmailAddress().equals(user1email)) {
                    foundAsExternal = true;
                }
            }
            if (participant.getType() == Participant.USER) {
                if (participant.getIdentifier() == userId) {
                    foundAsInternal++;
                }
            }
        }
        assertFalse("Should not find creator listed as external participant", foundAsExternal);
        assertEquals("Should find creator listed as internal participant once and only once", 1, foundAsInternal);
    }

    public void testExternalParticipantBecomesUserParticipantIfAddressMatchesAfterUpdateToo() throws Exception {
        AJAXClient client2 = new AJAXClient(User.User2);
        int user2id = client2.getValues().getUserId();
        GetResponse response = client2.execute(new GetContactForUserRequest(user2id, true, TimeZone.getDefault()));
        String user2email = response.getContact().getEmail1();

        Task task = generateTask("Another task to test the transformation of external participants into internal ones");
        Task result = manager.insertTaskOnServer(task);

        Task update = new Task();
        update.setLastModified(result.getLastModified());
        update.setObjectID(result.getObjectID());
        update.setParentFolderID(result.getParentFolderID());
        update.addParticipant(new ExternalUserParticipant(user2email));
        update.addParticipant(new UserParticipant(user2id));

        manager.updateTaskOnServer(update);

        Task actual = manager.getTaskFromServer(task);

        boolean foundAsExternal = false, foundAsInternal = false;

        Participant[] participants = actual.getParticipants();
        for (Participant participant : participants) {
            if (participant.getType() == Participant.EXTERNAL_USER) {
                if (participant.getEmailAddress().equals(user2email)) {
                    foundAsExternal = true;
                }
            }
            if (participant.getType() == Participant.USER) {
                if (participant.getIdentifier() == user2id) {
                    foundAsInternal = true;
                }
            }
        }
        assertFalse("Should not find user listed as external participant", foundAsExternal);
        assertTrue("Should find user listed as internal participant", foundAsInternal);
    }

}
