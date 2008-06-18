package com.openexchange.webdav.xml.appointment.recurrence;


import junit.framework.Test;
import junit.framework.TestSuite;

public class RecurrenceTestSuite extends TestSuite{
	
	public static Test suite(){
		final TestSuite tests = new TestSuite();
		tests.addTestSuite( DailyRecurrenceTest.class );
		tests.addTestSuite( Bug9742Test.class );
		return tests;
	}
}