package com.openexchange.webdav.xml.attachment;


import junit.framework.Test;
import junit.framework.TestSuite;

public class AttachmentWebdavSuite extends TestSuite{
	
	public static Test suite(){
		TestSuite tests = new TestSuite();
		tests.addTestSuite( DeleteTest.class );
		tests.addTestSuite( ListTest.class );
		tests.addTestSuite( NewTest.class );
		tests.addTestSuite( UpdateTest.class );
		
		return tests;
	}
}
