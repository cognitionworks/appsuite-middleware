package com.openexchange.groupware.notify;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import junit.framework.TestCase;

import com.openexchange.groupware.Init;
import com.openexchange.groupware.UserConfiguration;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.container.ExternalUserParticipant;
import com.openexchange.groupware.container.GroupParticipant;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.ResourceParticipant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.ContextImpl;
import com.openexchange.groupware.ldap.Group;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.MockGroupLookup;
import com.openexchange.groupware.ldap.MockResourceLookup;
import com.openexchange.groupware.ldap.MockUserLookup;
import com.openexchange.groupware.ldap.Resource;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserConfigurationFactory;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.i18n.TemplateListResourceBundle;
import com.openexchange.sessiond.SessionObject;

public class ParticipantNotifyTest extends TestCase{
	
	private static final MockGroupLookup GROUP_STORAGE = new MockGroupLookup();
	private static final MockUserLookup USER_STORAGE = new MockUserLookup();
	private static final MockResourceLookup RESOURCE_STORAGE = new MockResourceLookup();
	
	private static final UserConfigurationFactory USER_CONFIGS = new UserConfigurationFactory();
	
	
	public static final int EN = 0;
	public static final int DE = 1;
	
	private TestParticipantNotify notify = new TestParticipantNotify();
	
	private Date start = new Date();
	private Date end = new Date();
	private SessionObject session = null;
	
	
	public void testSimple() throws Exception{
		Participant[] participants = getParticipants(U(2),G(),S(), R());
		Task t = getTask(participants);
		
		notify.taskCreated(t,session);
		
		Message msg = notify.getMessages().get(0);
		
		String[] participantNames = parseParticipants( msg );
		
		assertNames( msg.addresses, "user1@test.invalid" );
		assertLanguage( EN , msg );
		assertNames( participantNames,"User 1" );
		assertEquals(200, msg.folderId);
		
		notify.clearMessages();
		
		participants = getParticipants(U(4), G(),S(), R());
		t = getTask(participants);
		
		notify.taskCreated(t,session);
		
		msg = notify.getMessages().get(0);
		participantNames = parseParticipants( msg );
		
		assertNames( msg.addresses, "user3@test.invalid" );
		assertLanguage( DE , msg );
		assertNames( participantNames,"User 3" );
		assertEquals(400, msg.folderId);
		
	}
	
	
	public void testExternal() throws Exception{
		Participant[] participants = getParticipants(U(),G(),S("don.external@external.invalid"), R());
		Task t = getTask(participants);
		
		notify.taskCreated(t,session);
		
		Message msg = notify.getMessages().get(0);
		
		String[] participantNames = parseParticipants( msg );
		
		assertNames( msg.addresses, "don.external@external.invalid" );
		assertNames( participantNames,"don.external@external.invalid" );	
	}
	
	public void testNoSend() throws Exception{
		Participant[] participants = getParticipants(U(6,2),G(),S(), R());
		Task t = getTask(participants);
		
		notify.taskCreated(t,session);
		
		Message msg = notify.getMessages().get(0);
		
		String[] participantNames = parseParticipants( msg );
		
		assertNames( msg.addresses, "user1@test.invalid" );
		assertLanguage( EN , msg );
		assertNames( participantNames,"User 5", "User 1" );
		
		notify.clearMessages();
		
		participants = getParticipants(U(), G(1),S(), R());
		t = getTask(participants);
		
		notify.taskCreated(t,session);
		
		
		List<String> deAddresses = new ArrayList<String>();
		List<String> enAddresses = new ArrayList<String>();
		
		
		for(Message message : notify.getMessages()){
			assertNames(parseParticipants(message), "The Mailadmin", "User 1", "User 2", "User 3", "User 4", "User 5", "User 6", "User 7","User 8","User 9");
			int lang = guessLanguage(message);
			switch(lang) {
			case DE:
				deAddresses.addAll(message.addresses);
				break;
			case EN:
				enAddresses.addAll(message.addresses);
				break;
			}
		}
		
		if (Locale.getDefault().getLanguage().equalsIgnoreCase("de")) {
			assertNames(deAddresses, "user2@test.invalid","user3@test.invalid", "user4@test.invalid", "user6@test.invalid", "user8@test.invalid","user9@test.invalid");
			assertNames(enAddresses, "mailadmin@test.invalid", "user1@test.invalid", "user7@test.invalid", "user5@test.invalid");	
		} else {
			assertNames(deAddresses, "user2@test.invalid","user3@test.invalid", "user4@test.invalid", "user6@test.invalid", "user8@test.invalid");
			assertNames(enAddresses, "mailadmin@test.invalid", "user1@test.invalid", "user7@test.invalid", "user5@test.invalid","user9@test.invalid");	
		}
	}
	
	public void testResolveGroup() throws Exception{
		Participant[] participants = getParticipants(U(),G(2),S(), R());
		Task t = getTask(participants);
		t.setUsers((UserParticipant[])null); // If the user participants are not set, fall back to resolving groups in ParticipantNotify
		notify.taskCreated(t,session);
		
		Message msg = notify.getMessages().get(0);
		
		String[] participantNames = parseParticipants( msg );
		
		assertAddresses( notify.getMessages(), "user2@test.invalid", "user4@test.invalid", "user6@test.invalid", "user8@test.invalid");
		assertLanguage( DE , msg );
		assertNames( participantNames, "User 2", "User 4", "User 6", "User 8" );
	}

	public void testNoSendDouble() throws Exception{
		Participant[] participants = getParticipants(U(3),G(2),S("user2@test.invalid"), R());
		Task t = getTask(participants);
		
		notify.taskCreated(t,session);
		
		Message msg = notify.getMessages().get(0);
		
		String[] participantNames = parseParticipants( msg );
		assertAddresses( notify.getMessages(), "user2@test.invalid", "user4@test.invalid", "user6@test.invalid", "user8@test.invalid");
		assertLanguage( DE , msg );
		assertNames( participantNames, "User 2", "User 4", "User 6", "User 8" );
	}
	
	public void testResources() throws Exception {
		Participant[] participants = getParticipants(U(2),G(),S(),R(1));
		
		Task t = getTask(participants);
		
		notify.taskCreated(t,session);
		assertAddresses(notify.getMessages(), "user1@test.invalid","resource_admin1@test.invalid");
	}
	
	public static final void assertLanguage(int lang, Message msg) {
		assertEquals(lang,guessLanguage(msg));
	}
	
	public static final void assertNames(String[] names, String...expected) {
		assertNames(Arrays.asList(names),expected);
	}
	
	public static final void assertNames(Iterable<String> names, String...expected) {
		Set<String> expectSet = new HashSet<String>(Arrays.asList(expected));
		
		for(String name : names) {
			assertTrue(names.toString(), expectSet.remove(name));
		}
	}
	
	public static final void assertAddresses(Collection<Message> messages, String...addresses) {
		List<String> collected = new ArrayList<String>();
		for(Message msg : messages) {
			collected.addAll(msg.addresses);
		}
		assertNames(collected,addresses);
	}
	
	
	public Task getTask(Participant[] participants) throws LdapException {
		Task task = new Task();
		task.setStartDate(start);
		task.setEndDate(end);
		task.setTitle("TestSimple");
		task.setCreatedBy(session.getUserObject().getId());
		task.setNotification(true);
		//task.setModifiedBy(session.getUserObject().getId());
		
		
		task.setParticipants(participants);
		
		List<UserParticipant> userParticipants = new ArrayList<UserParticipant>();
		for(Participant p : participants) {
			switch(p.getType()){
			case Participant.USER :
				userParticipants.add((UserParticipant)p);
				break;
			case Participant.GROUP :
				int[] memberIds = G(p.getIdentifier())[0].getMember();
				User[] asUsers = U(memberIds);
				Participant[] userParticipantsFromGroup = getParticipants(asUsers, G(), S(), R());
				for(Participant up : userParticipantsFromGroup) {
					userParticipants.add((UserParticipant)up);
				}
				break;
			}
		}
		
		task.setUsers(userParticipants);
		
		return task;
	}
	
	public static User[] U(int...ids) throws LdapException {
		User[] users = new User[ids.length];
		int i = 0;
		for(int id : ids) {
			users[i++] = USER_STORAGE.getUser(id);
		}
		return users;
	}
	
	public static final Group[] G(int...ids) throws LdapException {
		Group[] groups = new Group[ids.length];
		int i = 0;
		for(int id : ids) {
			groups[i++] = GROUP_STORAGE.getGroup(id);       
		}
		return groups;
	}
	
	public static final String[] S(String...strings) {
		return strings;
	}
	
	public static final Resource[] R(int...ids) throws LdapException {
		Resource[] resources = new Resource[ids.length];
		int i = 0;
		for(int id : ids) {
			resources[i++] = RESOURCE_STORAGE.getResource(id);
		}
		return resources;
	}
	
	public static final Participant[] getParticipants(User[] users, Group[] groups, String[] external, Resource[] resources) {
		Participant[] participants = new Participant[users.length+groups.length+external.length+resources.length];
		
		int i = 0;
		
		for(User user : users) {
			UserParticipant p = new UserParticipant();
			p.setDisplayName(user.getDisplayName());
			p.setEmailAddress(user.getMail());
			p.setIdentifier(user.getId());
			p.setPersonalFolderId(user.getId()*100); // Imaginary
			participants[i++] = p;
		}
		
		for(Group group : groups) {
			Participant p = new GroupParticipant();
			p.setDisplayName(group.getDisplayName());
			p.setIdentifier(group.getIdentifier());
			participants[i++] = p;	
		}
		
		for(String externalMail : external) {
			Participant p = new ExternalUserParticipant();
			p.setDisplayName(externalMail);
			p.setEmailAddress(externalMail);
			participants[i++] = p;
		}
		
		for(Resource resource : resources) {
			Participant p = new ResourceParticipant();
			p.setIdentifier(resource.getIdentifier());
			p.setDisplayName(resource.getDisplayName());
			participants[i++] = p;
		}
		
		return participants;
	}
	
	public void setUp() throws Exception {
		
		String templates = Init.getTestProperty("templatePath");
		TemplateListResourceBundle.setTemplatePath(new File(templates));
		
		session = new SessionObject("my_fake_sessionid");
		
		session.setContext(new ContextImpl(1));
		session.setUserObject(new MockUserLookup().getUser(1)); 
		session.setUserConfiguration(new UserConfigurationFactory().getConfiguration(1));
	}
	
	public void tearDown() throws Exception {
		notify.clearMessages();
	}
	
	private String[] parseParticipants(Message msg) {
		int language = guessLanguage(msg);
		switch(language) {
		case DE: return getLines(msg,"Teilnehmer","Ressourcen");
		case EN: return getLines(msg,"Participants", "Resources");
		default: return null;
		}
	}

	private static int guessLanguage(Message msg) {
		String[] german = new String[]{"Aufgabe", "erstellt", "geändert", "entfernt"};
		for(String g : german) {
			if(msg.messageTitle.contains(g))
				return DE;
		}
		return EN;
	}
	
	private String[] getLines(Message msg, String from, String to) {
		boolean collect = false;
		List<String> collector = new ArrayList<String>();
		String[] allLines = msg.message.split("\n");
		for(String line : allLines) {
			line = line.trim();
			if(line.startsWith(to)) {
				break;
			}
			
			if(collect) {
				if(!"".equals(line)&&!line.matches("=+"))
					collector.add(line);
			}
			
			if(line.startsWith(from)) {
				collect = true;
			}
			
		}
		return collector.toArray(new String[collector.size()]);
	}


	private static final class Message {
		public String messageTitle;
		public String message;
		public List<String> addresses;
		public int folderId;
		
		public Message(String messageTitle, String message, List<String>addresses, int folderId) {
			this.messageTitle = messageTitle;
			this.message = message;
			this.addresses = addresses;
			this.folderId = folderId;
		}
	}
	
	private static final class TestParticipantNotify extends ParticipantNotify {

		private List<Message> messageCollector = new ArrayList<Message>();
		
		@Override
		protected Group[] resolveGroups(Context ctx, int... ids) throws LdapException {
			return G(ids);
		}

		@Override
		protected User[] resolveUsers(Context ctx, int... ids) throws LdapException {
			return U(ids);
		}

		protected Resource[] resolveResources(Context ctx, int...ids) throws LdapException{
			return R(ids);
		}
		
		public List<Message> getMessages(){
			return messageCollector;
		}
		
		public void clearMessages(){
			messageCollector.clear();
		}
		
		@Override
		protected void sendMessage(String messageTitle, String message, List<String> name, SessionObject session, CalendarObject obj, int folderId, State state) {
			messageCollector.add(new Message(messageTitle,message,name, folderId));
		}
		
		@Override
		protected UserConfiguration getUserConfiguration(int id, int[] groups, Context context) throws SQLException {
			return USER_CONFIGS.getConfiguration(id);
		}		
	}
}
