
package com.openexchange.groupware;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import junit.framework.TestCase;

import com.openexchange.api.OXObjectNotFoundException;
import com.openexchange.api2.OXException;
import com.openexchange.event.impl.EventConfigImpl;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.groupware.calendar.CalendarRecurringCollection;
import com.openexchange.groupware.calendar.CalendarSql;
import com.openexchange.groupware.calendar.OXCalendarException;
import com.openexchange.groupware.calendar.RecurringResult;
import com.openexchange.groupware.calendar.RecurringResults;
import com.openexchange.groupware.configuration.AbstractConfigWrapper;
import com.openexchange.groupware.container.AppointmentObject;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.Participants;
import com.openexchange.groupware.container.ResourceParticipant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextImpl;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.server.impl.DBPool;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.sessiond.impl.SessionObject;
import com.openexchange.sessiond.impl.SessionObjectWrapper;
import com.openexchange.test.AjaxInit;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.oxfolder.OXFolderManager;
import com.openexchange.tools.oxfolder.OXFolderManagerImpl;


public class CalendarRecurringTests extends TestCase {
    
    //public static final long SUPER_END = 253402210800000L; // 31.12.9999 00:00:00 (GMT)
    public static final String TIMEZONE = "Europe/Berlin";
    private static int userid = 11; // bishoph
    public final static int contextid = 1;
    
    private static boolean init = false;
    
    @Override
	protected void setUp() throws Exception {
        super.setUp();
        //com.openexchange.groupware.Init.initContext();
        final EventConfigImpl event = new EventConfigImpl();
        event.setEventQueueEnabled(false);
        this.userid = getUserId();
        ContextStorage.start();
    }
    
    @Override
	protected void tearDown() throws Exception {
        if (init) {
            init = false;
            Init.stopServer();
        }
        super.tearDown();
    }
    
    private static Properties getAJAXProperties() {
        final Properties properties = AjaxInit.getAJAXProperties();
        return properties;
    }
    
    private static int resolveUser(final String u) throws Exception {
        final UserStorage uStorage = UserStorage.getInstance();
        return uStorage.getUserId(u, getContext());
    }
    
    public static int getUserId() throws Exception {
        if (!init) {
            Init.startServer();
            init = true;
        }
        final String user = AbstractConfigWrapper.parseProperty(getAJAXProperties(), "user_participant2", "");
        return resolveUser(user);
    }
    
    public static Context getContext() {
        return new ContextImpl(contextid);
    }
    
    public static int getPrivateFolder() throws Exception {
        int privatefolder = 0;
        final Context context = getContext();
        privatefolder = CalendarTest.getCalendarDefaultFolderForUser(userid, context);
        return privatefolder;
    }
    
    public void testBasicRecurring() throws Throwable {
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        assertFalse(cdao.calculateRecurrence());
        cdao.setStartDate(new Date(0));
        cdao.setEndDate(new Date(0));
        cdao.setUntil(new Date(0));
        cdao.setTitle("testBasicRecurring");
        cdao.setRecurrenceID(1);
        assertFalse(cdao.calculateRecurrence());
        cdao.setRecurrenceType(CalendarObject.DAILY);
        assertFalse(cdao.calculateRecurrence());
        cdao.setRecurrenceCalculator(1);
        assertFalse(cdao.calculateRecurrence());
        cdao.setInterval(1);
        assertTrue(cdao.calculateRecurrence());
    }
    
    public void testBasicRecurringWithOccurrence() throws Throwable {
        
        final Context context = new ContextImpl(contextid);
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, context.getContextId(), "myTestIdentifier");
        final int fid = CalendarTest.getCalendarDefaultFolderForUser(userid, context);
        
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setContext(context);
        cdao.setTimezone(TIMEZONE);
        
        CalendarTest.fillDatesInDao(cdao);
        cdao.removeUntil();
        cdao.setOccurrence(5);
        assertFalse("test if no until is set", cdao.containsUntil());
        long test_until = cdao.getEndDate().getTime() + (CalendarRecurringCollection.MILLI_DAY * 5);
        test_until = CalendarRecurringCollection.normalizeLong(test_until);
        
        cdao.setTitle("testBasicRecurringWithOccurrence");
        cdao.setRecurrenceType(CalendarObject.DAILY);
        cdao.setInterval(1);
        
        CalendarRecurringCollection.fillDAO(cdao);
        assertEquals("Check correct until for tis Occurrence", test_until, cdao.getUntil().getTime());
        
        cdao.setParentFolderID(fid);
        cdao.setIgnoreConflicts(true);
        final CalendarSql csql = new CalendarSql(so);
        csql.insertAppointmentObject(cdao);
        final int object_id = cdao.getObjectID();
        
        final CalendarDataObject test_dao = csql.getObjectById(object_id, fid);
        
        assertEquals("Check correct Occurrence value", 5, test_dao.getOccurrence());
        
        CalendarRecurringCollection.fillDAO(cdao);
        
        //RecurringResults m = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 0);
        //for (int a = 0; a < m.size(); a++) {
        //    RecurringResult rs = m.getRecurringResult(a);
        //    System.out.println(">>> "+new Date(rs.getStart()));
        //}
        
        //assertFalse("Test that until is not set", test_dao.containsUntil());
    }
    
    public void testBasicRecurringWithoutUntilAndWithoutOccurrence() throws Throwable {
        
        final Context context = new ContextImpl(contextid);
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, context.getContextId(), "myTestIdentifier");
        final int fid = CalendarTest.getCalendarDefaultFolderForUser(userid, context);
        
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setContext(context);
        cdao.setTimezone(TIMEZONE);
        
        CalendarTest.fillDatesInDao(cdao);
        
        cdao.setTitle("testBasicRecurringWithoutUntilAndWithoutOccurrence");
        cdao.setRecurrenceType(CalendarObject.DAILY);
        cdao.setInterval(1);
        
        CalendarRecurringCollection.fillDAO(cdao);
        
        cdao.removeUntil();
        assertFalse("test if no until is set", cdao.containsUntil());
        assertFalse("test if no Occurrence is set", cdao.containsOccurrence());
        
        long test_until = cdao.getEndDate().getTime() + (CalendarRecurringCollection.MILLI_YEAR * CalendarRecurringCollection.getMAX_END_YEARS());
        test_until = CalendarRecurringCollection.normalizeLong(test_until);
        final CalendarDataObject clone = (CalendarDataObject) cdao.clone();
		clone.setEndDate(new Date(test_until));
		final RecurringResults rresults;
		try {
			rresults = CalendarRecurringCollection.calculateRecurring(clone, 0, 0, 0);
			final RecurringResult rresult = rresults
					.getRecurringResultByPosition(CalendarRecurringCollection.MAXTC + 1);
			if (rresult != null) {
				test_until = CalendarRecurringCollection.normalizeLong(rresult.getEnd());
			}
		} catch (final OXException e) {
			// Keep max. end date
		}
        
        assertEquals("Check correct until for tis Occurrence", test_until, cdao.getUntil().getTime());
        
        cdao.setParentFolderID(fid);
        cdao.setIgnoreConflicts(true);
        final CalendarSql csql = new CalendarSql(so);
        csql.insertAppointmentObject(cdao);
        final int object_id = cdao.getObjectID();
        
        final CalendarDataObject test_dao = csql.getObjectById(object_id, fid);
        
        assertFalse("test if no Occurrence is set", test_dao.containsOccurrence());
        //assertFalse("Test that until is not set", test_dao.containsUntil()); // Check this
    }
    
    public void testBasicRecurringWithoutUntil() throws Throwable {
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        assertFalse(cdao.calculateRecurrence());
        cdao.setStartDate(new Date(0));
        cdao.setEndDate(new Date(0));
        cdao.setTitle("Basic Recurring Test");
        cdao.setRecurrenceID(1);
        assertFalse(cdao.calculateRecurrence());
        cdao.setRecurrenceType(CalendarObject.DAILY);
        assertFalse(cdao.calculateRecurrence());
        cdao.setRecurrenceCalculator(1);
        assertFalse(cdao.calculateRecurrence());
        cdao.setInterval(1);
        assertTrue(cdao.calculateRecurrence());
    }
    
    
    public void testDailyRecurring() throws Throwable {
        final long s = 1149768000000L; // 08.06.2006 12:00 (GMT)
        final long e = 1149771600000L; // 08.06.2006 13:00 (GMT)
        final long u = 1150156800000L; // 13.06.2006 00:00 (GMT)
        final long u_test = 1150106400000L; // 12.06.2006 10:00 (GMT)
        final String testrecurrence = "t|1|i|1|s|"+s+"|e|"+u+"|";
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        cdao.setStartDate(new Date(s));
        cdao.setEndDate(new Date(e));
        cdao.setUntil(new Date(u_test));
        cdao.setTitle("Daily Appointment Test");
        cdao.setRecurrence(testrecurrence);
        cdao.setRecurrenceCalculator(1);
        cdao.setRecurrenceID(1);
        
        
        final long pass_one_start = System.currentTimeMillis();
        
        CalendarRecurringCollection.fillDAO(cdao);
        
        final long pass_one_end = System.currentTimeMillis();
        final long pass_one_time = pass_one_end - pass_one_start;
        
        final long pass_two_start = System.currentTimeMillis();
        
        CalendarRecurringCollection.fillDAO(cdao);
        
        final long pass_two_end = System.currentTimeMillis();
        final long pass_two_time = pass_two_end - pass_two_start;
        
        final String check = CalendarRecurringCollection.createDSString(cdao);
        assertTrue("Checking daily sequence", check.equals(testrecurrence));
        RecurringResults m = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 0);
        assertEquals("Check calculation", 6, m.size());
        m = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 1);
        assertEquals("Check calculation", 1, m.size());
        
        double percent = pass_two_time/10;
        percent = pass_one_time * 100 / percent;
        
    }
    
    public void testDailyByPosition() throws Throwable {
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        cdao.setTitle("Daily Appointment By Position Test");
        CalendarTest.fillDatesInDao(cdao);
        cdao.setRecurrenceType(CalendarObject.DAILY);
        cdao.setInterval(1);
        CalendarRecurringCollection.fillDAO(cdao);
        final RecurringResults rrs = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 0);
        for (int a = 0; a < rrs.size(); a++) {
            final RecurringResults rrs_check = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, rrs.getRecurringResult(a).getPosition());
            final RecurringResult rr = rrs_check.getRecurringResult(0);
            assertEquals("Check correct recurrence position", rrs.getRecurringResult(a).getPosition(), rr.getPosition());
            assertEquals("Check correct start time", rrs.getRecurringResult(a).getStart(), rr.getStart());
            assertTrue("Check correct position calculation", a+1 == rr.getPosition());
        }
    }
    
    
    public void testDailyRecurringWithDAO() throws Throwable {
        final long s = 1149768000000L; // 08.06.2006 12:00 (GMT)
        final long e = 1149771600000L; // 08.06.2006 13:00 (GMT)
        final long u = 1150156800000L; // 13.06.2006 00:00 (GMT)
        final long u_test = 1150106400000L; // 12.06.2006 10:00 (GMT)
        final String testrecurrence = "t|1|i|1|s|"+s+"|e|"+u+"|";
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        cdao.setStartDate(new Date(s));
        cdao.setEndDate(new Date(e));
        cdao.setUntil(new Date(u_test));
        cdao.setTitle("Daily Appointment Test only with DAO");
        cdao.setRecurrenceCalculator(1);
        cdao.setRecurrenceID(1);
        cdao.setRecurrenceType(CalendarObject.DAILY);
        cdao.setInterval(1);
        CalendarRecurringCollection.fillDAO(cdao);
        final String check = CalendarRecurringCollection.createDSString(cdao);
        assertTrue("Checking daily sequence", check.equals(testrecurrence));
        RecurringResults m = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 0);
        assertEquals("Check calculation", 6, m.size());
        
        m = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 1);
        assertEquals("Check calculation", 1, m.size());
    }
    
    
    public void testWeeklyRecurrence() throws Throwable {
        
//
//      Juni 2006
//So Mo Di Mi Do Fr Sa
//             1  2  3
// 4  5  6  7  8  9 10
//11 12 13 14 15 16 17
//18 19 20 21 22 23 24
//25 26 27 28 29 30
        
        final long s = 1149768000000L; // 08.06.2006 12:00 (GMT)
        final long e = 1149771600000L; // 08.06.2006 13:00 (GMT)
        final long u = 1151100000000L; // 24.06.2006 00:00 (GMT)
        final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        RecurringResults m = null;
        CalendarDataObject cdao = null;
        
        // MONDAY
        cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        cdao.setStartDate(new Date(s));
        cdao.setEndDate(new Date(e));
        cdao.setUntil(new Date(u));
        cdao.setTitle("Weekly Appointment Test");
        cdao.setRecurrenceCalculator(1);
        cdao.setRecurrenceID(1);
        cdao.setRecurrenceType(CalendarObject.WEEKLY);
        cdao.setInterval(1);
        cdao.setDays(AppointmentObject.MONDAY);
        CalendarRecurringCollection.fillDAO(cdao);
        m = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 1);
        c.setTimeInMillis(new Date(m.getRecurringResult(0).getStart()).getTime());
        assertEquals("First day check (MONDAY)", c.get(Calendar.DAY_OF_WEEK), Calendar.MONDAY);
        
        
        // SUNDAY
        cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        cdao.setStartDate(new Date(s));
        cdao.setEndDate(new Date(e));
        cdao.setUntil(new Date(u));
        cdao.setTitle("Weekly Appointment Test");
        cdao.setRecurrenceCalculator(1);
        cdao.setRecurrenceID(1);
        cdao.setRecurrenceType(CalendarObject.WEEKLY);
        cdao.setInterval(1);
        cdao.setDays(AppointmentObject.SUNDAY);
        CalendarRecurringCollection.fillDAO(cdao);
        m = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 1);
        c.setTimeInMillis(new Date(m.getRecurringResult(0).getStart()).getTime());
        assertEquals("First day check (SUNDAY)", c.get(Calendar.DAY_OF_WEEK), Calendar.SUNDAY);
        
        // TUESDAY
        cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        cdao.setStartDate(new Date(s));
        cdao.setEndDate(new Date(e));
        cdao.setUntil(new Date(u));
        cdao.setTitle("Weekly Appointment Test");
        cdao.setRecurrenceCalculator(1);
        cdao.setRecurrenceID(1);
        cdao.setRecurrenceType(CalendarObject.WEEKLY);
        cdao.setInterval(1);
        cdao.setDays(AppointmentObject.TUESDAY);
        CalendarRecurringCollection.fillDAO(cdao);
        m = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 1);
        c.setTimeInMillis(new Date(m.getRecurringResult(0).getStart()).getTime());
        assertEquals("First day check (TUESDAY)", c.get(Calendar.DAY_OF_WEEK), Calendar.TUESDAY);
        
        
        // WEDNESDAY
        cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        cdao.setStartDate(new Date(s));
        cdao.setEndDate(new Date(e));
        cdao.setUntil(new Date(u));
        cdao.setTitle("Weekly Appointment Test");
        cdao.setRecurrenceCalculator(1);
        cdao.setRecurrenceID(1);
        cdao.setRecurrenceType(CalendarObject.WEEKLY);
        cdao.setInterval(1);
        cdao.setDays(AppointmentObject.WEDNESDAY);
        CalendarRecurringCollection.fillDAO(cdao);
        m = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 1);
        c.setTimeInMillis(new Date(m.getRecurringResult(0).getStart()).getTime());
        assertEquals("First day check (WEDNESDAY)", c.get(Calendar.DAY_OF_WEEK), Calendar.WEDNESDAY);
        
        // THURSDAY
        cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        cdao.setStartDate(new Date(s));
        cdao.setEndDate(new Date(e));
        cdao.setUntil(new Date(u));
        cdao.setTitle("Weekly Appointment Test");
        cdao.setRecurrenceCalculator(1);
        cdao.setRecurrenceID(1);
        cdao.setRecurrenceType(CalendarObject.WEEKLY);
        cdao.setInterval(1);
        cdao.setDays(AppointmentObject.THURSDAY);
        CalendarRecurringCollection.fillDAO(cdao);
        m = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 1);
        c.setTimeInMillis(new Date(m.getRecurringResult(0).getStart()).getTime());
        assertEquals("First day check (THURSDAY)", c.get(Calendar.DAY_OF_WEEK), Calendar.THURSDAY);
        
        // FRIDAY
        cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        cdao.setStartDate(new Date(s));
        cdao.setEndDate(new Date(e));
        cdao.setUntil(new Date(u));
        cdao.setTitle("Weekly Appointment Test");
        cdao.setRecurrenceCalculator(1);
        cdao.setRecurrenceID(1);
        cdao.setRecurrenceType(CalendarObject.WEEKLY);
        cdao.setInterval(1);
        cdao.setDays(AppointmentObject.FRIDAY);
        CalendarRecurringCollection.fillDAO(cdao);
        m = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 1);
        c.setTimeInMillis(new Date(m.getRecurringResult(0).getStart()).getTime());
        assertEquals("First day check (FRIDAY)", c.get(Calendar.DAY_OF_WEEK), Calendar.FRIDAY);
        
        // SATURDAY
        cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        cdao.setStartDate(new Date(s));
        cdao.setEndDate(new Date(e));
        cdao.setUntil(new Date(u));
        cdao.setTitle("Weekly Appointment Test");
        cdao.setRecurrenceCalculator(1);
        cdao.setRecurrenceID(1);
        cdao.setRecurrenceType(CalendarObject.WEEKLY);
        cdao.setInterval(1);
        cdao.setDays(AppointmentObject.SATURDAY);
        CalendarRecurringCollection.fillDAO(cdao);
        m = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 1);
        c.setTimeInMillis(new Date(m.getRecurringResult(0).getStart()).getTime());
        assertEquals("First day check (SATURDAY)", c.get(Calendar.DAY_OF_WEEK), Calendar.SATURDAY);
        
        // MONDAY + THURSDAY
        cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        cdao.setStartDate(new Date(s));
        cdao.setEndDate(new Date(e));
        cdao.setUntil(new Date(u));
        cdao.setTitle("Weekly Appointment Test");
        cdao.setRecurrenceCalculator(1);
        cdao.setRecurrenceID(1);
        cdao.setRecurrenceType(CalendarObject.WEEKLY);
        cdao.setInterval(1);
        cdao.setDays(AppointmentObject.MONDAY + AppointmentObject.THURSDAY);
        CalendarRecurringCollection.fillDAO(cdao);
        m = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 0);
        assertEquals("Check calculation", 5, m.size());
        
        c.setTimeInMillis(new Date(m.getRecurringResult(0).getStart()).getTime());
        assertEquals("First day check (THURSDAY)", c.get(Calendar.DAY_OF_WEEK), Calendar.THURSDAY);
        c.setTimeInMillis(new Date(m.getRecurringResult(1).getStart()).getTime());
        assertEquals("First day check (MONDAY)", c.get(Calendar.DAY_OF_WEEK), Calendar.MONDAY);
        
        
        // SUNDAY + MONDAY
        cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        cdao.setStartDate(new Date(s));
        cdao.setEndDate(new Date(e));
        cdao.setUntil(new Date(u));
        cdao.setTitle("Weekly Appointment Test");
        cdao.setRecurrenceCalculator(1);
        cdao.setRecurrenceID(1);
        cdao.setRecurrenceType(CalendarObject.WEEKLY);
        cdao.setInterval(1);
        cdao.setDays(AppointmentObject.MONDAY + AppointmentObject.SUNDAY);
        CalendarRecurringCollection.fillDAO(cdao);
        m = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 0);
        assertEquals("Check calculation", 5, m.size());
        
        c.setTimeInMillis(new Date(m.getRecurringResult(0).getStart()).getTime());
        assertEquals("First day check (SUNDAY)", c.get(Calendar.DAY_OF_WEEK), Calendar.SUNDAY);
        c.setTimeInMillis(new Date(m.getRecurringResult(1).getStart()).getTime());
        assertEquals("First day check (MONDAY)", c.get(Calendar.DAY_OF_WEEK), Calendar.MONDAY);
        
        
    }
    
    public void testSaveRecurring() throws Throwable {
        final Context context = new ContextImpl(contextid);
        
        final CalendarDataObject cdao = new CalendarDataObject();
        
        long s = System.currentTimeMillis();
        long cals = s;
        final long calsmod = s%CalendarRecurringCollection.MILLI_DAY;
        cals = cals- calsmod;
        final long endcalc = 3600000;
        long mod = s%3600000;
        s = s - mod;
        final long saves = s;
        final long e = s + endcalc;
        final long savee = e;
        long u = s + (CalendarRecurringCollection.MILLI_DAY * 10);
        mod = u%CalendarRecurringCollection.MILLI_DAY;
        u = u - mod;
        
        final Calendar start = Calendar.getInstance(TimeZone.getTimeZone(("Europe/Berlin")));
        start.setTimeInMillis(saves);
        final Calendar ende = Calendar.getInstance(TimeZone.getTimeZone(("Europe/Berlin")));
        ende.setTimeInMillis(savee);
        
        cdao.setStartDate(new Date(s));
        cdao.setEndDate(new Date(e));
        cdao.setUntil(new Date(u));
        cdao.setTitle("testSaveRecurring");
        cdao.setRecurrenceType(CalendarObject.DAILY);
        cdao.setRecurrenceCalculator(1);
        cdao.setInterval(1);
        cdao.setDays(1);
        
        cdao.setParentFolderID(CalendarTest.getCalendarDefaultFolderForUser(userid, context));
        
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, context.getContextId(), "myTestIdentifier");
        cdao.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        cdao.setIgnoreConflicts(true);
        final CalendarSql csql = new CalendarSql(so);
        csql.insertAppointmentObject(cdao);
        
        final RecurringResults rss = CalendarRecurringCollection.calculateRecurring(cdao, cals, u, 0);
        assertEquals("Testing size ", rss.size(), 10);
        for (int a = 0; a < rss.size(); a++) {
            final RecurringResult rs = rss.getRecurringResult(a);
            assertEquals("Testing start time", rs.getStart(), start.getTimeInMillis());
            assertEquals("Testing end time", rs.getEnd(), ende.getTimeInMillis());
            assertEquals("Testing Position", a+1, rs.getPosition());
            start.add(Calendar.DAY_OF_MONTH, 1);
            ende.add(Calendar.DAY_OF_MONTH, 1);
        }
    }
    
    public void testDeleteSingleRecurringAppointment() throws Throwable {
        final Context context = new ContextImpl(contextid);
        final CalendarDataObject cdao = new CalendarDataObject();
        
        long s = System.currentTimeMillis();
        long cals = s;
        final long calsmod = s%CalendarRecurringCollection.MILLI_DAY;
        cals = cals- calsmod;
        final long endcalc = 3600000;
        long mod = s%3600000;
        s = s - mod;
        final long saves = s;
        final long e = s + endcalc;
        final long savee = e;
        long u = s + (CalendarRecurringCollection.MILLI_DAY * 10);
        mod = u%CalendarRecurringCollection.MILLI_DAY;
        u = u - mod;
        
        
        final Calendar start = Calendar.getInstance(TimeZone.getTimeZone(("Europe/Berlin")));
        start.setTimeInMillis(saves);
        final Calendar ende = Calendar.getInstance(TimeZone.getTimeZone(("Europe/Berlin")));
        ende.setTimeInMillis(savee);
        
        final String user2 = AbstractConfigWrapper.parseProperty(getAJAXProperties(), "user_participant3", "");
        final int uid2 = resolveUser(user2);
        
        final int folder_id = CalendarTest.getCalendarDefaultFolderForUser(userid, context);
        final int folder_id2 = CalendarTest.getCalendarDefaultFolderForUser(uid2, context);
        
        cdao.setStartDate(new Date(s));
        cdao.setEndDate(new Date(e));
        cdao.setUntil(new Date(u));
        cdao.setTitle("testDeleteSingleRecurringAppointment - step 1 - insert ");
        cdao.setRecurrenceType(CalendarObject.DAILY);
        cdao.setRecurrenceCalculator(1);
        cdao.setInterval(1);
        cdao.setDays(1);
        
        cdao.setParentFolderID(folder_id);
        
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, context.getContextId(), "myTestIdentifier");
        final SessionObject so2 = SessionObjectWrapper.createSessionObject(uid2, context.getContextId(), "myTestIdentifier");
        
        final Participants p = new Participants();
        final Participant pa = new UserParticipant(userid);
        p.add(pa);
        
        final Participant pa2 = new UserParticipant(uid2);        
        p.add(pa2);
        
        cdao.setParticipants(p.getList());
        
        cdao.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        cdao.setIgnoreConflicts(true);
        final CalendarSql csql = new CalendarSql(so);
        csql.insertAppointmentObject(cdao);
        final int object_id = cdao.getObjectID();
        final Date last = cdao.getLastModified();
        
        RecurringResults rss = CalendarRecurringCollection.calculateRecurring(cdao, cals, u, 0);
        assertEquals("Testing size ", rss.size(), 10);
        for (int a = 0; a < rss.size(); a++) {
            final RecurringResult rs = rss.getRecurringResult(a);
            assertEquals("Testing start time", rs.getStart(), start.getTimeInMillis());
            assertEquals("Testing end time", rs.getEnd(), ende.getTimeInMillis());
            assertEquals("Testing Position", a+1, rs.getPosition());
            start.add(Calendar.DAY_OF_MONTH, 1);
            ende.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        // Delete (virtually) a single app in the sequence
        final CalendarDataObject test_delete = csql.getObjectById(object_id, folder_id);
        rss = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 0);
        assertEquals("Testing size ", rss.size(), 11);
        
        final CalendarDataObject delete_owner = new CalendarDataObject();
        delete_owner.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        delete_owner.setRecurrencePosition(3);
        delete_owner.setObjectID(object_id);
        csql.deleteAppointmentObject(delete_owner, folder_id, new Date());
        
        final CalendarDataObject test_object = csql.getObjectById(object_id, folder_id);
        rss = CalendarRecurringCollection.calculateRecurring(test_object, 0, 0, 0);
        assertEquals("Testing size after deleteing single app from sequence", rss.size(), 10);
        
        rss = CalendarRecurringCollection.calculateRecurring(test_object, 0, 0, 3);
        assertEquals("Testing size after requesting single deleted app from sequence", rss.size(), 0);
        
        // Now we delete a virtual exception and we are not the owner
        
        final CalendarSql csql2 = new CalendarSql(so2);
        final CalendarDataObject test_delete_not_owner = new CalendarDataObject();
        test_delete_not_owner.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        test_delete_not_owner.setObjectID(object_id);
        test_delete_not_owner.setRecurrencePosition(5);
        csql2.deleteAppointmentObject(test_delete_not_owner, folder_id2, new Date());
        
        final CalendarDataObject test_master_object = csql.getObjectById(object_id, folder_id);
        final UserParticipant up[] = test_master_object.getUsers();
        assertEquals("Testing participants in master object", up.length, 2);
        
        final int cols[] = new int[] { AppointmentObject.TITLE,  AppointmentObject.OBJECT_ID, AppointmentObject.RECURRENCE_ID, AppointmentObject.RECURRENCE_POSITION, AppointmentObject.RECURRENCE_TYPE, AppointmentObject.DELETE_EXCEPTIONS, AppointmentObject.CHANGE_EXCEPTIONS };
        
        SearchIterator si = csql.getModifiedAppointmentsInFolder(folder_id, cols, last, true);
        
        boolean found_exception = false;
        while (si.hasNext()) {
            final CalendarDataObject tcdao = (CalendarDataObject)si.next();
            if (tcdao.getRecurrenceID() == object_id && tcdao.getObjectID() != object_id) {
                // found the single exception we have just created
                found_exception = true;
                final Date test_deleted_exceptions[] = tcdao.getDeleteException();
                final Date test_changed_exceptions[] = tcdao.getChangeException();
                assertTrue("Test deleted exception is NULL" , test_deleted_exceptions == null);
                assertTrue("Test changed exception is NULL" , test_changed_exceptions == null);
                assertEquals("Check correct recurrence position", 5, tcdao.getRecurrencePosition());
            } else {
                final Date test_deleted_exceptions[] = tcdao.getDeleteException();
                final Date test_changed_exceptions[] = tcdao.getChangeException();
                assertTrue("Test deleted exception is NULL" , test_deleted_exceptions == null);
                assertTrue("Test changed exception is ! NULL" , test_changed_exceptions != null);
                assertTrue("Test changed exception is 1" , test_changed_exceptions.length == 1);
                assertEquals("Check master recurrence position", 0, tcdao.getRecurrencePosition());
            }
        }
        si.close();
        assertTrue("Found created exception ", found_exception);
        
        // delete whole sequence incl. all exceptions
        final CalendarDataObject delete_all = new CalendarDataObject();
        delete_all.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        delete_all.setObjectID(object_id);
        csql.deleteAppointmentObject(delete_all, folder_id, new Date());
        
        si = csql.getModifiedAppointmentsInFolder(folder_id, cols, last, true);
        while (si.hasNext()) {
            final CalendarDataObject tcdao = (CalendarDataObject)si.next();
            assertFalse("Object should not exists anymore ", tcdao.getRecurrenceID() == object_id);
        }
        
    }
    
    
    public void testRecurringSimpleUpdate() throws Throwable {
        final Context context = new ContextImpl(contextid);
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, context.getContextId(), "myTestIdentifier");
        
        final int folder_id = CalendarTest.getCalendarDefaultFolderForUser(userid, context);
        
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        cdao.setParentFolderID(folder_id);
        
        CalendarTest.fillDatesInDao(cdao);
        
        cdao.setTitle("testRecurringSimpleUpdate - Step 1 - Insert");
        cdao.setRecurrenceType(CalendarObject.DAILY);
        cdao.setRecurrenceCalculator(1);
        cdao.setInterval(1);
        cdao.setDays(1);
        
        cdao.setIgnoreConflicts(true);
        
        final CalendarSql csql = new CalendarSql(so);
        csql.insertAppointmentObject(cdao);
        final int object_id = cdao.getObjectID();
        
        
        final CalendarDataObject testobject = csql.getObjectById(object_id, folder_id);
        final Date save_start = testobject.getStartDate();
        final Date save_end = testobject.getEndDate();
        final String rec_string = CalendarRecurringCollection.createDSString(testobject);
        
        final CalendarDataObject update = new CalendarDataObject();
        update.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        update.setTitle("testRecurringSimpleUpdate - Step 2 - Update");
        update.setObjectID(object_id);
        update.setIgnoreConflicts(true);
        
        csql.updateAppointmentObject(update, folder_id, new Date());
        
        CalendarDataObject testobject_update = csql.getObjectById(object_id, folder_id);
        assertEquals("Check start date", save_start, testobject_update.getStartDate());
        assertEquals("Check end date", save_end, testobject_update.getEndDate());
        assertTrue("Check rec string", rec_string.equals(CalendarRecurringCollection.createDSString(testobject_update)));
        
        
        final RecurringResults rss = CalendarRecurringCollection.calculateRecurring(testobject_update, 0, 0, 1);
        assertTrue("Got results ", rss.size() == 1);
        final RecurringResult rs = rss.getRecurringResult(0);
        
        final CalendarDataObject update_with_times = new CalendarDataObject();
        
        update_with_times.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        update_with_times.setTitle("testRecurringSimpleUpdate - Step 3 - Update");
        update_with_times.setObjectID(object_id);
        update_with_times.setStartDate(new Date(rs.getStart()));
        update_with_times.setEndDate(new Date(rs.getEnd()));
        update_with_times.setIgnoreConflicts(true);
        
        csql.updateAppointmentObject(update_with_times, folder_id, new Date());
        
        testobject_update = csql.getObjectById(object_id, folder_id);
        assertEquals("Check start date", save_start, testobject_update.getStartDate());
        assertEquals("Check end date", save_end, testobject_update.getEndDate());
        assertEquals("Check rec string", rec_string, CalendarRecurringCollection.createDSString(testobject_update));
        
        
        final CalendarDataObject update_with_changed_times = new CalendarDataObject();
        
        update_with_changed_times.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        update_with_changed_times.setTitle("testRecurringSimpleUpdate - Step 4 - Update");
        update_with_changed_times.setObjectID(object_id);
        update_with_changed_times.setStartDate(new Date(rs.getStart()+3600000));
        update_with_changed_times.setEndDate(new Date(rs.getEnd()+3600000));
        update_with_changed_times.setIgnoreConflicts(true);
        
        csql.updateAppointmentObject(update_with_changed_times, folder_id, new Date());
        
        testobject_update = csql.getObjectById(object_id, folder_id);
        assertEquals("Check start date", new Date(save_start.getTime()+3600000), testobject_update.getStartDate());
        assertEquals("Check end date", new Date(save_end.getTime()+3600000), testobject_update.getEndDate());
        
        assertTrue("Check rec string", !rec_string.equals(CalendarRecurringCollection.createDSString(testobject_update)));
        
    }
    
    public void testUpdateSimpleAppointmentToRecurring() throws Throwable {
        final Context context = new ContextImpl(contextid);
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, context.getContextId(), "myTestIdentifier");
        
        final int folder_id = CalendarTest.getCalendarDefaultFolderForUser(userid, context);
        
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        cdao.setParentFolderID(folder_id);
        
        CalendarTest.fillDatesInDao(cdao);
        
        cdao.setTitle("testUpdateSimpleAppointmentToRecurring - Step 1 - Insert");
        cdao.setIgnoreConflicts(true);
        
        final CalendarSql csql = new CalendarSql(so);
        csql.insertAppointmentObject(cdao);
        final int object_id = cdao.getObjectID();
        
        final CalendarDataObject update = new CalendarDataObject();
        update.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        update.setTitle("testUpdateSimpleAppointmentToRecurring - Step 2 - Update - Recurring");
        update.setObjectID(object_id);
        update.setRecurrenceType(CalendarObject.DAILY);
        update.setRecurrenceCalculator(1);
        update.setInterval(1);
        update.setDays(1);
        update.setUntil(cdao.getUntil());
        update.setIgnoreConflicts(true);
        
        csql.updateAppointmentObject(update, folder_id, new Date());
        
        final CalendarDataObject testobject = csql.getObjectById(object_id, folder_id);
        
        final RecurringResults rss = CalendarRecurringCollection.calculateRecurring(testobject, 0, 0, 0);
        assertTrue("Test object is not null", rss != null);
        assertEquals("Testing size ", rss.size(), 11);
        final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        c.setTimeInMillis(cdao.getStartDate().getTime());
        for (int a = 0; a < rss.size(); a++) {
            final RecurringResult rs = rss.getRecurringResult(a);
            assertEquals("Check correct start time ", c.getTimeInMillis(), rs.getStart());
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        final CalendarDataObject update_normal = new CalendarDataObject();
        update_normal.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        update_normal.setTitle("testUpdateSimpleAppointmentToRecurring - Step 3 - Update - Normal");
        update_normal.setObjectID(object_id);
        update_normal.setRecurrenceType(CalendarObject.NO_RECURRENCE);
        update_normal.setIgnoreConflicts(true);
        
        csql.updateAppointmentObject(update_normal, folder_id, new Date());
        
        final CalendarDataObject testobject2 = csql.getObjectById(object_id, folder_id);
        assertEquals("Check if appointment is no sequence", CalendarDataObject.NO_RECURRENCE, testobject2.getRecurrenceType());
        final String null_check = "\""+testobject2.getRecurrence()+"\"";
        assertEquals("Check that the recurrence is null ", "\"null\"", null_check);
        //assertNull("Check that the recurrence is null", testobject2.getRecurrence()); // This seems to be a bug, but i cant check null!!
        
    }
    
    public void testCreateExceptionFromRecurring() throws Throwable {
        final Context context = new ContextImpl(contextid);
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, context.getContextId(), "myTestIdentifier");
        
        final int folder_id = CalendarTest.getCalendarDefaultFolderForUser(userid, context);
        
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        cdao.setParentFolderID(folder_id);
        
        CalendarTest.fillDatesInDao(cdao);
        
        cdao.setTitle("testCreateExceptionFromRecurring - Step 1 - Insert");
        cdao.setRecurrenceType(CalendarObject.DAILY);
        cdao.setRecurrenceCalculator(1);
        cdao.setInterval(1);
        cdao.setDays(1);
        
        cdao.setIgnoreConflicts(true);
        
        final CalendarSql csql = new CalendarSql(so);
        csql.insertAppointmentObject(cdao);
        final int object_id = cdao.getObjectID();
        final Date last = cdao.getLastModified();
        
        final CalendarDataObject update = new CalendarDataObject();
        update.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        update.setObjectID(object_id);
        update.setIgnoreConflicts(true);
        
        
        final RecurringResults rss = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 3);
        final RecurringResult rs = rss.getRecurringResult(0);
        final long new_start = rs.getStart()+3600000;
        final long new_end = rs.getEnd()+3600000;
        
        final Date test_new_start_date = new Date(new_start);
        final Date test_new_end_date = new Date(new_end);
        
        update.setStartDate(test_new_start_date);
        update.setEndDate(test_new_end_date);
        
        update.setTitle("testCreateExceptionFromRecurring - Step 2 - Update (create exception)");
        update.setRecurrencePosition(3);
        
        
        csql.updateAppointmentObject(update, folder_id, new Date());
        assertTrue("Got a new object_id" , object_id != update.getObjectID());
        
        final CalendarDataObject testobject = csql.getObjectById(object_id, folder_id);
        final RecurringResults rss_test = CalendarRecurringCollection.calculateRecurring(testobject, 0, 0, 3, 999, true);
        final RecurringResult rs_test = rss.getRecurringResult(0);
        
        
        assertTrue("Got RecurringResult", rs_test != null);
        assertEquals("Check first calc", rs.getStart(), rs_test.getStart());
        assertTrue("Got correct exception", 3 == rs_test.getPosition());
        
        final long exception_date = rs_test.getNormalized();
        
        final java.util.Date exceptions[] = testobject.getChangeException();
        assertTrue("Got exceptions", exceptions != null);
        
        assertEquals("Check correct exception calculation", exception_date, exceptions[0].getTime());
        
        
        final int cols[] = new int[] { AppointmentObject.TITLE,  AppointmentObject.START_DATE, AppointmentObject.END_DATE, AppointmentObject.OBJECT_ID, AppointmentObject.RECURRENCE_ID, AppointmentObject.RECURRENCE_POSITION, AppointmentObject.RECURRENCE_TYPE, AppointmentObject.DELETE_EXCEPTIONS, AppointmentObject.CHANGE_EXCEPTIONS };
        SearchIterator si = csql.getModifiedAppointmentsInFolder(folder_id, cols, last, true);
        
        boolean found_exception = false;
        while (si.hasNext()) {
            final CalendarDataObject tcdao = (CalendarDataObject)si.next();
            if (tcdao.getRecurrenceID() == object_id && tcdao.getRecurrencePosition() == 3) {
                // found the single exception we have just have created
                assertTrue("Test if we got a unique ID", tcdao.getRecurrenceID() != tcdao.getObjectID());
                found_exception = true;
                assertEquals("Test exception start date" , test_new_start_date.getTime(), tcdao.getStartDate().getTime());
                assertEquals("Test exception end date" , test_new_end_date.getTime(), tcdao.getEndDate().getTime());
            }
        }
        si.close();
        assertTrue("Found exception",  found_exception);
        
        si = csql.getAppointmentsBetweenInFolder(folder_id, cols, new Date(0), new Date(253402210800000L), 0, null);
        int counter = 0;
        while (si.hasNext()) {
            final CalendarDataObject tcdao = (CalendarDataObject)si.next();
            if (tcdao.getRecurrenceID() == object_id) {
                counter ++;
            } else if (tcdao.getRecurrenceID() == object_id && tcdao.getRecurrencePosition() == 3) {
                final java.util.Date check_exception[] = tcdao.getChangeException();
                assertTrue("Got exceptions", check_exception != null);            	
            }
 
        }
        si.close();
        assertEquals("Check correct number of results" , 2 , counter);
        
    }
    
    public void testWeeklyMonday()  throws Throwable {
        RecurringResults m = null;
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        CalendarTest.fillDatesInDao(cdao);
        cdao.removeUntil();
        cdao.setTitle("testWeeklyMonday");
        cdao.setRecurrenceID(1);
        cdao.setRecurrenceType(CalendarObject.WEEKLY);
        cdao.setInterval(1);
        cdao.setDays(AppointmentObject.MONDAY);
        CalendarRecurringCollection.fillDAO(cdao);
        m = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 0);
        for (int a = 0; a < m.size(); a++) {
            final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
            c.setTimeInMillis(m.getRecurringResult(a).getStart());
            assertEquals("First day check (MONDAY)", Calendar.MONDAY, c.get(Calendar.DAY_OF_WEEK));
        }
    }

    public void testCreateExceptionFromRecurringWithDatePosition() throws Throwable {
        final Context context = new ContextImpl(contextid);
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, context.getContextId(), "myTestIdentifier");
        
        final int folder_id = CalendarTest.getCalendarDefaultFolderForUser(userid, context);
        
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        cdao.setParentFolderID(folder_id);
        
        CalendarTest.fillDatesInDao(cdao);
        
        cdao.setTitle("testCreateExceptionFromRecurringWithDatePosition - Step 1 - Insert");
        cdao.setRecurrenceType(CalendarObject.DAILY);
        cdao.setRecurrenceCalculator(1);
        cdao.setInterval(1);
        cdao.setDays(1);
        
        final Participants p = new Participants();
        final Participant resource = new ResourceParticipant(100);        
        //p.add(resource); // TODO: Write a new test case with resources

        cdao.setParticipants(p.getList());        
        
        cdao.setIgnoreConflicts(true);
        
        final CalendarSql csql = new CalendarSql(so);
        csql.insertAppointmentObject(cdao);
        final int object_id = cdao.getObjectID();
        final Date last = cdao.getLastModified();
        
        final CalendarDataObject update = new CalendarDataObject();
        update.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        update.setObjectID(object_id);
        update.setIgnoreConflicts(true);
        
        
        final RecurringResults rss = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 3);
        final RecurringResult rs = rss.getRecurringResult(0);
        final long new_start = rs.getStart()+3600000;
        final long new_end = rs.getEnd()+3600000;
        
        final Date test_new_start_date = new Date(new_start);
        final Date test_new_end_date = new Date(new_end);
        
        update.setStartDate(test_new_start_date);
        update.setEndDate(test_new_end_date);
        
        update.setTitle("testCreateExceptionFromRecurringWithDatePosition - Step 2 - Update (create exception)");
        // The date when change exception shall take place
        final long changeExceptionDate = CalendarRecurringCollection.normalizeLong(new_start);
        update.setRecurrenceDatePosition(new Date(changeExceptionDate));
        
        
        csql.updateAppointmentObject(update, folder_id, new Date());
        
        final CalendarDataObject testobject = csql.getObjectById(object_id, folder_id);
        final RecurringResults rss_test = CalendarRecurringCollection.calculateRecurring(testobject, 0, 0, 3, 999, true);
        final RecurringResult rs_test = rss_test.getRecurringResult(0);
        
        
        assertTrue("Got RecurringResult", rs_test != null);
        assertEquals("Check first calc", rs.getStart(), rs_test.getStart());
        assertTrue("Got correct exception", 3 == rs_test.getPosition());
        
        final long exception_date = rs_test.getNormalized();
        
        final java.util.Date exceptions[] = testobject.getChangeException();
        assertTrue("Got exceptions", exceptions != null);
        
        assertEquals("Check correct exception calculation", exception_date, changeExceptionDate);
        assertEquals("Check correct exception calculation with stored date", exception_date, exceptions[0].getTime());
        
        
        final int cols[] = new int[] { AppointmentObject.TITLE,  AppointmentObject.START_DATE, AppointmentObject.END_DATE, AppointmentObject.OBJECT_ID, AppointmentObject.RECURRENCE_ID, AppointmentObject.RECURRENCE_POSITION, AppointmentObject.RECURRENCE_TYPE, AppointmentObject.DELETE_EXCEPTIONS, AppointmentObject.CHANGE_EXCEPTIONS };
        SearchIterator si = csql.getModifiedAppointmentsInFolder(folder_id, cols, last, true);
        
        boolean found_exception = false;
        while (si.hasNext()) {
            final CalendarDataObject tcdao = (CalendarDataObject)si.next();
            if (tcdao.getRecurrenceID() == object_id && tcdao.getRecurrencePosition() == 3) {
                // found the single exception we have just have created
                assertTrue("Test if we got a unique ID", tcdao.getRecurrenceID() != tcdao.getObjectID());
                found_exception = true;
                assertEquals("Test exception start date" , test_new_start_date.getTime(), tcdao.getStartDate().getTime());
                assertEquals("Test exception end date" , test_new_end_date.getTime(), tcdao.getEndDate().getTime());
            }
        }
        si.close();
        assertTrue("Found exception",  found_exception);
        
        si = csql.getAppointmentsBetweenInFolder(folder_id, cols, new Date(0), new Date(253402210800000L), 0, null);
        int counter = 0;
        while (si.hasNext()) {
            final CalendarDataObject tcdao = (CalendarDataObject)si.next();
            if (tcdao.getRecurrenceID() == object_id) {
                counter ++;
            }
        }
        si.close();
        assertEquals("Check correct number of results" , 2 , counter);
        
    }
    
    public void testWeeklyDifferentInterval()  throws Throwable {
        
        final long s = 1149501600000L; // 05.06.2006 12:00 (GMT)
        final long e = 1149505200000L; // 05.06.2006 13:00 (GMT)
        
        final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        c.setTimeInMillis(s);
        
        RecurringResults m = null;
        // SUNDAY + MONDAY
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        cdao.setStartDate(new Date(s));
        cdao.setEndDate(new Date(e));
        cdao.setOccurrence(10);
        
        cdao.setTitle("Weekly Appointment Test");
        cdao.setRecurrenceID(1);
        cdao.setRecurrenceType(CalendarObject.WEEKLY);
        cdao.setInterval(2);
        cdao.setDays(AppointmentObject.MONDAY + AppointmentObject.WEDNESDAY + AppointmentObject.FRIDAY);
        CalendarRecurringCollection.fillDAO(cdao);
        m = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 0);
        
        c.setTimeInMillis(new Date(m.getRecurringResult(0).getStart()).getTime());
        assertEquals("First day check (MONDAY)", c.get(Calendar.DAY_OF_WEEK), Calendar.MONDAY);
        c.setTimeInMillis(new Date(m.getRecurringResult(1).getStart()).getTime());
        assertEquals("First day check (WEDNESDAY)", c.get(Calendar.DAY_OF_WEEK), Calendar.WEDNESDAY);
        c.setTimeInMillis(new Date(m.getRecurringResult(2).getStart()).getTime());
        assertEquals("First day check (FRIDAY)", c.get(Calendar.DAY_OF_WEEK), Calendar.FRIDAY);
    }
    
    public void testCorrectUntitCalculation()  throws Throwable {
        final RecurringResults m = null;
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        CalendarTest.fillDatesInDao(cdao);
        cdao.removeUntil();
        cdao.setTitle("testCorrectUntitCalculation");
        cdao.setRecurrenceType(CalendarObject.WEEKLY);
        cdao.setInterval(1);
        cdao.setDays(AppointmentObject.WEDNESDAY);
        CalendarRecurringCollection.fillDAO(cdao);
        
        long check_until = CalendarRecurringCollection
				.normalizeLong((cdao.getStartDate().getTime() + (CalendarRecurringCollection.MILLI_YEAR * CalendarRecurringCollection
						.getMAX_END_YEARS())));
		final CalendarDataObject clone = (CalendarDataObject) cdao.clone();
		clone.setEndDate(new Date(check_until));
		final RecurringResults rresults;
		try {
			rresults = CalendarRecurringCollection.calculateRecurring(clone, 0, 0, 0);
			final RecurringResult rresult = rresults
					.getRecurringResultByPosition(CalendarRecurringCollection.MAXTC + 1);
			if (rresult != null) {
				check_until = CalendarRecurringCollection.normalizeLong(rresult.getEnd());
			}
		} catch (final OXException e) {
			// Keep max. end date
		}
        
        assertEquals("Check correct until for !yearly " , check_until , cdao.getUntil().getTime());
        
        final CalendarDataObject cdao2 = new CalendarDataObject();
        cdao2.setTimezone(TIMEZONE);
        CalendarTest.fillDatesInDao(cdao2);
        cdao2.removeUntil();
        cdao2.setTitle("testCorrectUntitCalculation yearly");
        cdao2.setRecurrenceType(CalendarObject.YEARLY);
        cdao2.setInterval(1);
        cdao2.setMonth(Calendar.AUGUST);
        cdao2.setDayInMonth(20);
        CalendarRecurringCollection.fillDAO(cdao);
        
        final long check_until2 = CalendarRecurringCollection.normalizeLong( (cdao.getStartDate().getTime() + (CalendarRecurringCollection.MILLI_YEAR *  99)) );
        
        assertEquals("Check correct until for yearly " , check_until2 , cdao2.getUntil().getTime());
        
        
    }
    
    public void testFlagSingleException() throws Throwable {
        final Context context = new ContextImpl(contextid);
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, context.getContextId(), "myTestIdentifier");
        
        final int folder_id = CalendarTest.getCalendarDefaultFolderForUser(userid, context);
        
        final RecurringResults m = null;
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        cdao.setContext(context);
        CalendarTest.fillDatesInDao(cdao);
        cdao.removeUntil();
        cdao.setTitle("testFlagSingleException");
        cdao.setParentFolderID(folder_id);
        cdao.setRecurrenceType(CalendarObject.DAILY);
        cdao.setInterval(1);
        cdao.setOccurrence(7);
        
        cdao.setIgnoreConflicts(true);
        
        final CalendarSql csql = new CalendarSql(so);
        csql.insertAppointmentObject(cdao);
        final int object_id = cdao.getObjectID();
        final Date last = cdao.getLastModified();
        
        final CalendarDataObject flag_exception = new CalendarDataObject();
        flag_exception.setContext(context);
        flag_exception.setObjectID(object_id);
        flag_exception.setRecurrencePosition(2);
        flag_exception.setLabel(2);
        flag_exception.setIgnoreConflicts(true);
        
        csql.updateAppointmentObject(flag_exception, folder_id, last);
        final int object_exception_id = flag_exception.getObjectID();
        final Date last_exception = flag_exception.getLastModified();
        
        assertTrue("Exception created (object_id:object_exception_id) ("+object_id+":"+object_exception_id+")", object_id != object_exception_id);
        //assertEquals("Return value is recurrence position", 2, flag_exception.getRecurrencePosition());
        
        final CalendarDataObject testobject = csql.getObjectById(object_id, folder_id);
        final CalendarDataObject testobject_exception = csql.getObjectById(object_exception_id, folder_id);
        assertEquals("Check recurrence id", testobject.getRecurrenceID(), testobject_exception.getRecurrenceID());
        
        
    }
    public void testWholeDayRecurringWithOccurrence() throws Throwable {
        final Context context = new ContextImpl(contextid);
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, context.getContextId(), "myTestIdentifier");
        
        final int folder_id = CalendarTest.getCalendarDefaultFolderForUser(userid, context);
        
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        cdao.setContext(context);
        CalendarTest.fillDatesInDao(cdao);
        cdao.removeUntil();
        final long s = CalendarRecurringCollection.normalizeLong(cdao.getStartDate().getTime());
        cdao.setStartDate(new Date(s));
        final long e = s+CalendarRecurringCollection.MILLI_DAY;
        cdao.setEndDate(new Date(e));
        cdao.setFullTime(true);
        long start_time = cdao.getStartDate().getTime();
        start_time = CalendarRecurringCollection.normalizeLong(start_time);
        cdao.setTitle("testWholeDayRecurringWithOccurrence");
        cdao.setParentFolderID(folder_id);
        cdao.setRecurrenceType(CalendarObject.DAILY);
        cdao.setInterval(1);
        
        final int occurrence = 2;
        
        cdao.setOccurrence(occurrence);
        
        cdao.setIgnoreConflicts(true);
        
        final CalendarSql csql = new CalendarSql(so);
        csql.insertAppointmentObject(cdao);
        final int object_id = cdao.getObjectID();
        final Date last = cdao.getLastModified();
        
        final CalendarDataObject testobject = csql.getObjectById(object_id, folder_id);
        
        final Date check_time = new Date(start_time + (CalendarRecurringCollection.MILLI_DAY*occurrence));
        
        assertEquals("Check correct until", check_time, testobject.getUntil());
    }
    
    public void testComplexOccurrence() throws Throwable {
        final Context context = new ContextImpl(contextid);
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, context.getContextId(), "myTestIdentifier");
        
        final int folder_id = CalendarTest.getCalendarDefaultFolderForUser(userid, context);
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        cdao.setContext(context);
        CalendarTest.fillDatesInDao(cdao);
        cdao.removeUntil();
        cdao.setTitle("testComplexOccurrence - Step 1");
        cdao.setParentFolderID(folder_id);
        cdao.setRecurrenceType(CalendarObject.DAILY);
        cdao.setInterval(1);
        cdao.setOccurrence(3);
        cdao.setIgnoreConflicts(true);
        final CalendarSql csql = new CalendarSql(so);
        csql.insertAppointmentObject(cdao);
        final int object_id = cdao.getObjectID();
        
        final CalendarDataObject testobject = csql.getObjectById(object_id, folder_id);
        
        final CalendarDataObject update = new CalendarDataObject();
        update.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        update.setIgnoreConflicts(true);
        update.setTitle("testComplexOccurrence - Step 2");
        update.setOccurrence(5);
        update.setRecurrenceType(CalendarObject.DAILY);
        update.setInterval(1);
        update.setObjectID(object_id);
        
        //update.setStartDate(cdao.getStartDate());
        //update.setEndDate(cdao.getEndDate());
        
        
        csql.updateAppointmentObject(update, folder_id, cdao.getLastModified());
        
        final CalendarDataObject testobject2 = csql.getObjectById(object_id, folder_id);
        
        assertTrue("Check that until date is different", testobject.getUntil().getTime() != testobject2.getUntil().getTime());
        
    }
    
    public void testRecurringConflictHandling() throws Throwable {
        final Context context = new ContextImpl(contextid);
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, context.getContextId(), "myTestIdentifier");
        
        final int fid = CalendarTest.getCalendarDefaultFolderForUser(userid, context);
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setTimezone(TIMEZONE);
        cdao.setContext(context);
        CalendarTest.fillDatesInDao(cdao);
        cdao.setTitle("ConflictAppointment");
        cdao.setIgnoreConflicts(true);
        cdao.setParentFolderID(fid);
        final CalendarDataObject cdao2 = (CalendarDataObject) cdao.clone();
        
        final CalendarSql csql = new CalendarSql(so);
        csql.insertAppointmentObject(cdao);
        final int object_id = cdao.getObjectID();
        
        cdao2.setTitle("testRecurringConflictHandling");
        cdao2.setRecurrenceType(CalendarObject.DAILY);
        cdao2.setInterval(7);
        cdao2.setParentFolderID(fid);
        cdao2.setIgnoreConflicts(false);
        cdao2.removeUntil();
        
        CalendarDataObject conflicts[] = csql.insertAppointmentObject(cdao2);
        
        assertTrue("Recurring Appointments should not conflict!", conflicts == null);
        
        final int object_id2 = cdao2.getObjectID();
        
        final CalendarDataObject flag_exception = new CalendarDataObject();
        flag_exception.setContext(context);
        flag_exception.setObjectID(object_id2);
        flag_exception.setRecurrencePosition(1);
        flag_exception.setLabel(2);
        flag_exception.setIgnoreConflicts(true);
        
        conflicts = csql.updateAppointmentObject(flag_exception, fid, cdao2.getLastModified());
        
        assertTrue("Recurring Appointments should not conflict!", conflicts == null);
    }
    
    
    public void testMonthlyRecurringWithDayLightSavingTimeShift() throws Throwable {
        final Context context = new ContextImpl(contextid);
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, context.getContextId(), "myTestIdentifier");
        
        final int fid = CalendarTest.getCalendarDefaultFolderForUser(userid, context);
        
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setContext(context);
        cdao.setTimezone(TIMEZONE);
        
        final long s = 1172754000000L; // 01.03.2007 14:00:00 CET
        final long e = 1172757600000L; // 01.03.2007 15:00:00 CET
        
        cdao.setStartDate(new Date(s));
        cdao.setEndDate(new Date(e));
        
        final Calendar check_start = Calendar.getInstance();
        check_start.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
        check_start.setTimeInMillis(s);
        final int check_start_hour = check_start.get(Calendar.HOUR);
        final int check_start_minute = check_start.get(Calendar.MINUTE);
        
        cdao.setTitle("testMonthlyRecurringWithDayLightSavingTimeShift");
        
        cdao.setRecurrenceType(CalendarObject.MONTHLY);
        cdao.setDays(CalendarObject.FRIDAY);
        cdao.setDayInMonth(1);
        cdao.setInterval(1);
        
        cdao.setOccurrence(3);
        
        cdao.setParentFolderID(fid);
        cdao.setIgnoreConflicts(true);
        
        final CalendarSql csql = new CalendarSql(so);
        csql.insertAppointmentObject(cdao);
        final int object_id = cdao.getObjectID();
        
        final CalendarDataObject test_dao = csql.getObjectById(object_id, fid);
        
        CalendarRecurringCollection.fillDAO(cdao);
        
        final RecurringResults m = CalendarRecurringCollection.calculateRecurring(test_dao, 0, 0, 0);
        assertTrue("Result != null", m != null);
        assertTrue("Got results", m.size() > 0);
        
        for (int a = 0; a < m.size(); a++) {
            final RecurringResult rs = m.getRecurringResult(a);
            
            final Calendar test_start = Calendar.getInstance();
            test_start.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
            test_start.setTimeInMillis(rs.getStart());
            final int test_start_hour = test_start.get(Calendar.HOUR);
            final int test_start_minute = test_start.get(Calendar.MINUTE);
            
            //System.out.println(">>> "+new Date(rs.getStart()));
            assertEquals("Test hour (of occurence "+rs.getPosition()+")", check_start_hour, test_start_hour);
            assertEquals("Test minute (of occurence "+rs.getPosition()+")", check_start_minute, test_start_minute);
            
        }
        
    }
    
    
    public void testYearly() throws Throwable {
        final Context context = new ContextImpl(contextid);
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, context.getContextId(), "myTestIdentifier");
        
        final int fid = CalendarTest.getCalendarDefaultFolderForUser(userid, context);
        
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setContext(context);
        cdao.setTimezone(TIMEZONE);
        CalendarTest.fillDatesInDao(cdao);
        cdao.removeUntil();
        cdao.setTitle("testYearly");
        cdao.setRecurrenceType(CalendarObject.YEARLY);
        cdao.setInterval(1);
        cdao.setMonth(Calendar.FEBRUARY);
        cdao.setDayInMonth(19);
        
        cdao.setParentFolderID(fid);
        cdao.setIgnoreConflicts(true);
        
        final CalendarSql csql = new CalendarSql(so);
        csql.insertAppointmentObject(cdao);
        final int object_id = cdao.getObjectID();
        
        final CalendarDataObject test_dao = csql.getObjectById(object_id, fid);
        
        CalendarRecurringCollection.fillDAO(test_dao);
        
        final RecurringResults m = CalendarRecurringCollection.calculateRecurring(test_dao, 0, 0, 0);
        assertTrue("Result != null", m != null);
        assertTrue("Got results", m.size() > 0);
        
        for (int a = 0; a < m.size(); a++) {
            final RecurringResult rs = m.getRecurringResult(a);
            
            final Calendar test_start = Calendar.getInstance();
            test_start.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
            test_start.setTimeInMillis(rs.getStart());
            final int test_month = test_start.get(Calendar.MONTH);
            final int test_day_of_month = test_start.get(Calendar.DAY_OF_MONTH);
            
            assertEquals("Test month (of occurence "+rs.getPosition()+")", Calendar.FEBRUARY, test_month);
            assertEquals("Test day_of_month (of occurence "+rs.getPosition()+")", 19, test_day_of_month);
            
        }
        
    }
    
    
    public void testErrors() throws Throwable {
        final Context context = new ContextImpl(contextid);
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setContext(context);
        cdao.setTimezone(TIMEZONE);
        CalendarTest.fillDatesInDao(cdao);
        cdao.removeUntil();
        cdao.setTitle("testYearly");
        cdao.setRecurrenceType(CalendarObject.DAILY);
        cdao.setInterval(-1);
        boolean check_if_error_is_thrown = false;
        try {
            final RecurringResults m = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 0);
            fail("An error must occur at this time. Fix me!");
        } catch(final OXCalendarException oxce) {
            check_if_error_is_thrown = true;
        }
        
        assertTrue("Check if the error was thrown", check_if_error_is_thrown);
        
    }
    
    public void testMoveExceptionToDifferentFolerAndSetPrivateFlag() throws Throwable {
        final Context context = new ContextImpl(contextid);
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, getContext().getContextId(), "myTestSearch");
               
        final Connection readcon = DBPool.pickup(context);
        final Connection writecon = DBPool.pickupWriteable(context);        
        
        final int fid = AppointmentBugTests.getPrivateFolder(userid);
        
        final OXFolderManager oxma = new OXFolderManagerImpl(so, readcon, writecon);
        FolderObject fo = new FolderObject();
        
        final CalendarSql csql = new CalendarSql(so);
        
        final OCLPermission oclp1 = new OCLPermission();
        oclp1.setEntity(userid);
        oclp1.setAllPermission(OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION);
        oclp1.setFolderAdmin(true);
        final OCLPermission oclp2 = new OCLPermission();
        
        fo.setFolderName("testExceptionHandlingFolder");
        fo.setParentFolderID(fid);
        fo.setModule(FolderObject.CALENDAR);
        fo.setType(FolderObject.PRIVATE);
        fo.setPermissionsAsArray(new OCLPermission[] { oclp1 });       
        
        int test_folder = 0;
        try {
            fo = oxma.createFolder(fo, true, System.currentTimeMillis());
            test_folder = fo.getObjectID();       
            
            final CalendarDataObject cdao = new CalendarDataObject();
            cdao.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
            cdao.setParentFolderID(fid);
        
            CalendarTest.fillDatesInDao(cdao);
        
            cdao.setTitle("testMoveExceptionToDifferentFolerAndSetPrivateFlag");
            cdao.setRecurrenceType(CalendarObject.DAILY);
            cdao.setRecurrenceCalculator(1);
            cdao.setInterval(1);
            cdao.setDays(1);        
            cdao.setIgnoreConflicts(true);
        

            csql.insertAppointmentObject(cdao);
            final int object_id = cdao.getObjectID();            
 
            final CalendarDataObject exception = new CalendarDataObject();
            exception.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
            exception.setObjectID(object_id);
            exception.setIgnoreConflicts(true);            
            exception.setTitle("testMoveExceptionToDifferentFolerAndSetPrivateFlag - Update (create exception)");
            exception.setRecurrencePosition(3);            
            
            csql.updateAppointmentObject(exception, fid, new Date());
            final int exception_id = exception.getObjectID();
            assertTrue("Object was created", exception_id > 0);
            assertTrue("Got a new object_id" , object_id != exception_id);
            
            final CalendarDataObject test_move_folder = new CalendarDataObject();
            test_move_folder.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
            test_move_folder.setObjectID(exception_id);
            test_move_folder.setParentFolderID(test_folder);
            test_move_folder.setIgnoreConflicts(true);
            try {
                csql.updateAppointmentObject(test_move_folder, fid, new Date());
                fail("An exception should not be moved to a different folder");
            } catch(final OXCalendarException oxca) {
                // this is what we want
                assertEquals("Check correct error code", 66, oxca.getDetailNumber());                
            }
            
            final CalendarDataObject test_private_flag = new CalendarDataObject();
            test_private_flag.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
            test_private_flag.setObjectID(exception_id);
            test_private_flag.setParentFolderID(fid);
            test_private_flag.setPrivateFlag(true);
            test_private_flag.setIgnoreConflicts(true);
            try {
                csql.updateAppointmentObject(test_private_flag, fid, new Date());
                fail("An exception should not be flagged as private");
            } catch(final OXCalendarException oxca) {
                // this is what we want
                assertEquals("Check correct error code", 69, oxca.getDetailNumber()); 
            }            
            
        } finally {
            try {
                if (test_folder > 0) {
                    oxma.deleteFolder(new FolderObject(test_folder), true, System.currentTimeMillis());
                } 
            } catch(final Exception e) {
                e.printStackTrace();
                fail("Error deleting folder object.");
            }
        }
        try {
            DBPool.push(context, readcon);
            DBPool.pushWrite(context, writecon);        
        } catch(final Exception ignore) { 
            ignore.printStackTrace();
        }                        
        
    }
    
    public void testCreateAndDeleteException() throws Throwable {
        final Context context = new ContextImpl(contextid);
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, context.getContextId(), "myTestIdentifier");
        
        final int folder_id = CalendarTest.getCalendarDefaultFolderForUser(userid, context);
        
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        cdao.setParentFolderID(folder_id);
        
        CalendarTest.fillDatesInDao(cdao);
        
        cdao.setTitle("testCreateAndDeleteException");
        cdao.setRecurrenceType(CalendarObject.DAILY);
        cdao.setRecurrenceCalculator(1);
        cdao.setInterval(1);
        cdao.setDays(1);
        
        cdao.setIgnoreConflicts(true);
        
        final CalendarSql csql = new CalendarSql(so);
        csql.insertAppointmentObject(cdao);
        final int object_id = cdao.getObjectID();
        final Date last = cdao.getLastModified();
        
        final CalendarDataObject update = new CalendarDataObject();
        update.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        update.setObjectID(object_id);
        update.setIgnoreConflicts(true);
        
        
        final RecurringResults rss = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 3);
        final RecurringResult rs = rss.getRecurringResult(0);
        final long new_start = rs.getStart()+3600000;
        final long new_end = rs.getEnd()+3600000;
        
        final Date test_new_start_date = new Date(new_start);
        final Date test_new_end_date = new Date(new_end);
        
        update.setStartDate(test_new_start_date);
        update.setEndDate(test_new_end_date);
        
        update.setTitle("testCreateAndDeleteException - Exception");
        update.setRecurrencePosition(3);
        
        
        csql.updateAppointmentObject(update, folder_id, new Date());
        assertTrue("Got a new object_id" , object_id != update.getObjectID());
        
        final int exception_object_id = update.getObjectID();
        
        final CalendarDataObject testobject = csql.getObjectById(exception_object_id, folder_id);
        assertTrue("Got correct exception", 3 == testobject.getRecurrencePosition());
        
        assertTrue("Check if recurring_id is set", testobject.containsRecurrenceID());
        assertTrue("Check if recurring_id ("+testobject.getRecurrenceID()+") > 0 ", testobject.getRecurrenceID() > 0);
        assertEquals("Check if object is still a recurring event", true, testobject.isSequence());        
        
        // Now delete the existing exception with the global recurring instead
        // with the direct delete command
        
        
        final CalendarDataObject delete = new CalendarDataObject();
        delete.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        delete.setObjectID(object_id);
        delete.setIgnoreConflicts(true);        
        delete.setTitle("testCreateAndDeleteException - delete single exception and change title");
        final Date changed_exceptions[] = testobject.getChangeException();
        final long changeExceptionDate = changed_exceptions[0].getTime();
        assertTrue("Got changed exceptions", changed_exceptions != null);
        delete.setDeleteExceptions(new Date[] { changed_exceptions[0] });
        
        csql.updateAppointmentObject(delete, folder_id, new Date());
        
        final CalendarDataObject test_dao = csql.getObjectById(object_id, folder_id);
        assertTrue("Check if recurring_id is set", test_dao.containsRecurrenceID());
        assertTrue("Check if recurring_id ("+test_dao.getRecurrenceID()+") > 0 ", test_dao.getRecurrenceID() > 0);
        assertEquals("Check if object is still a recurring event", true, test_dao.isSequence());
        
        
        try {
        	final CalendarDataObject check_object = csql.getObjectById(exception_object_id, folder_id);
        	fail("The exception still exists but should be deleted!");
        } catch(final OXObjectNotFoundException e) { 
        	// this is what we want
        	final int x = 0;
        }

        assertTrue("Change exception has not been removed in database", test_dao.getChangeException() == null || test_dao.getChangeException().length == 0);
        assertTrue("Delete exception has not been stored to database", test_dao.getDeleteException() != null && test_dao.getDeleteException().length == 1);
        assertTrue("Delete exception date is not equal to previous change exception date", changeExceptionDate == test_dao.getDeleteException()[0].getTime());
    }

    public void testDeleteException() throws Throwable {
        final Context context = new ContextImpl(contextid);
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, context.getContextId(), "myTestIdentifier");
        
        final int folder_id = CalendarTest.getCalendarDefaultFolderForUser(userid, context);
        
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        cdao.setParentFolderID(folder_id);
        
        CalendarTest.fillDatesInDao(cdao);
        
        cdao.setTitle("testDeleteException");
        cdao.setRecurrenceType(CalendarObject.DAILY);
        cdao.setRecurrenceCalculator(1);
        cdao.setInterval(1);
        cdao.setDays(1);
        
        cdao.setIgnoreConflicts(true);
        
        final CalendarSql csql = new CalendarSql(so);
        csql.insertAppointmentObject(cdao);
        final int object_id = cdao.getObjectID();
        final Date last = cdao.getLastModified();
        
        final CalendarDataObject update = new CalendarDataObject();
        update.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        update.setObjectID(object_id);
        update.setIgnoreConflicts(true);
        
        // Get all occurrences
        final RecurringResults rss = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 0, CalendarRecurringCollection.MAXTC, true);
        
        // Create one delete exception first
        final RecurringResult first = rss.getRecurringResult(0);
        final long deleteExceptionDate = CalendarRecurringCollection.normalizeLong(first.getStart());
        update.setDeleteExceptions(new Date[] { new Date(deleteExceptionDate) });
        
        csql.updateAppointmentObject(update, folder_id, new Date());
        
        // Reload
        final CalendarDataObject tdao = csql.getObjectById(object_id, folder_id);
        
        assertTrue("Delete exception not contained in recurring appointment", tdao.containsDeleteExceptions() && tdao.getDeleteException().length > 0);
        

    }

    public void testDeleteExceptionUntilFullDelete() throws Throwable {
        final Context context = new ContextImpl(contextid);
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, context.getContextId(), "myTestIdentifier");
        
        final int folder_id = CalendarTest.getCalendarDefaultFolderForUser(userid, context);
        
        final CalendarDataObject cdao = new CalendarDataObject();
        cdao.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        cdao.setParentFolderID(folder_id);
        
        CalendarTest.fillDatesInDao(cdao);
        
        cdao.setTitle("testDeleteExceptionUntilFullDelete");
        cdao.setRecurrenceType(CalendarObject.DAILY);
        cdao.setRecurrenceCalculator(1);
        cdao.setInterval(1);
        cdao.setDays(1);
        
        cdao.setIgnoreConflicts(true);
        
        final CalendarSql csql = new CalendarSql(so);
        csql.insertAppointmentObject(cdao);
        final int object_id = cdao.getObjectID();
        final Date last = cdao.getLastModified();
        
        final CalendarDataObject update = new CalendarDataObject();
        update.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        update.setObjectID(object_id);
        update.setIgnoreConflicts(true);
        
        // Get all occurrences
        final RecurringResults rss = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 0, CalendarRecurringCollection.MAXTC, true);

        final int size = rss.size();
        final List<Date> ddates = new ArrayList<Date>(size);
        for (int i = 0; i < size; i++) {
        	final RecurringResult rs = rss.getRecurringResult(i);
        	final long deleteExceptionDate = CalendarRecurringCollection.normalizeLong(rs.getStart());
        	ddates.add(new Date(deleteExceptionDate));
		}

        update.setDeleteExceptions(ddates.toArray(new Date[ddates.size()]));
        
        csql.updateAppointmentObject(update, folder_id, new Date());
        
        // Reload should fail since all occurrences were deleted
        try {
        	final CalendarDataObject check_object = csql.getObjectById(object_id, folder_id);
        	fail("The recurring appointment still exists but should have been deleted!");
        } catch(final OXObjectNotFoundException e) { 
        	// this is what we want
        	final int x = 0;
        }
    }
 
    public void testDeleteSingleRecurringAppointmentsUntilFullDelete() throws Throwable {
        final Context context = new ContextImpl(contextid);
        final CalendarDataObject cdao = new CalendarDataObject();
        
        long s = System.currentTimeMillis();
        long cals = s;
        final long calsmod = s%CalendarRecurringCollection.MILLI_DAY;
        cals = cals- calsmod;
        final long endcalc = 3600000;
        long mod = s%3600000;
        s = s - mod;
        final long saves = s;
        final long e = s + endcalc;
        final long savee = e;
        long u = s + (CalendarRecurringCollection.MILLI_DAY * 10);
        mod = u%CalendarRecurringCollection.MILLI_DAY;
        u = u - mod;
        
        
        final Calendar start = Calendar.getInstance(TimeZone.getTimeZone(("Europe/Berlin")));
        start.setTimeInMillis(saves);
        final Calendar ende = Calendar.getInstance(TimeZone.getTimeZone(("Europe/Berlin")));
        ende.setTimeInMillis(savee);
        
        final String user2 = AbstractConfigWrapper.parseProperty(getAJAXProperties(), "user_participant3", "");
        final int uid2 = resolveUser(user2);
        
        final int folder_id = CalendarTest.getCalendarDefaultFolderForUser(userid, context);
        final int folder_id2 = CalendarTest.getCalendarDefaultFolderForUser(uid2, context);
        
        cdao.setStartDate(new Date(s));
        cdao.setEndDate(new Date(e));
        cdao.setUntil(new Date(u));
        cdao.setTitle("testDeleteSingleRecurringAppointment - step 1 - insert ");
        cdao.setRecurrenceType(CalendarObject.DAILY);
        cdao.setRecurrenceCalculator(1);
        cdao.setInterval(1);
        cdao.setDays(1);
        
        cdao.setParentFolderID(folder_id);
        
        final SessionObject so = SessionObjectWrapper.createSessionObject(userid, context.getContextId(), "myTestIdentifier");
        final SessionObject so2 = SessionObjectWrapper.createSessionObject(uid2, context.getContextId(), "myTestIdentifier");
        
        final Participants p = new Participants();
        final Participant pa = new UserParticipant(userid);
        p.add(pa);
        
        final Participant pa2 = new UserParticipant(uid2);        
        p.add(pa2);
        
        cdao.setParticipants(p.getList());
        
        cdao.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
        cdao.setIgnoreConflicts(true);
        final CalendarSql csql = new CalendarSql(so);
        csql.insertAppointmentObject(cdao);
        final int object_id = cdao.getObjectID();
        final Date last = cdao.getLastModified();

        RecurringResults rss = CalendarRecurringCollection.calculateRecurring(cdao, cals, u, 0);
        assertEquals("Testing size ", rss.size(), 10);
        for (int a = 0; a < rss.size(); a++) {
            final RecurringResult rs = rss.getRecurringResult(a);
            assertEquals("Testing start time", rs.getStart(), start.getTimeInMillis());
            assertEquals("Testing end time", rs.getEnd(), ende.getTimeInMillis());
            assertEquals("Testing Position", a+1, rs.getPosition());
            start.add(Calendar.DAY_OF_MONTH, 1);
            ende.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        // Delete all single appointments in the sequence
        final CalendarDataObject test_delete = csql.getObjectById(object_id, folder_id);
        rss = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, 0);
        assertEquals("Testing size ", rss.size(), 11);
        
        for (int i = 1; i <= rss.size(); i++) {
        	final RecurringResults foo = CalendarRecurringCollection.calculateRecurring(cdao, 0, 0, i);
        	final RecurringResult bar = foo.getRecurringResult(0);
        	assertTrue("Recurrence position is null",bar != null);
        	assertEquals("Recurrence position mismatch", bar.getPosition(), i);
        }

        for (int i = 1; i <= rss.size(); i++) {
        	 final CalendarDataObject delete_owner = new CalendarDataObject();
             delete_owner.setContext(ContextStorage.getInstance().getContext(so.getContextId()));
             delete_owner.setRecurrencePosition(i);
             delete_owner.setObjectID(object_id);
             csql.deleteAppointmentObject(delete_owner, folder_id, new Date());
		}
        
        // Check if full delete has been performed since all occurrences were deleted
        try {
        	final CalendarDataObject check_object = csql.getObjectById(object_id, folder_id);
        	fail("The recurring appointment still exists but should have been deleted! " + check_object.getObjectID());
        } catch(final OXObjectNotFoundException exc) { 
        	// this is what we want
        	final int x = 0;
        }
        
        
        
    }
}
