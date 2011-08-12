package com.openexchange.ajax.infostore;


import com.openexchange.ajax.infostore.test.InfostoreManagedTests;
import junit.framework.Test;
import junit.framework.TestSuite;

public class InfostoreAJAXSuite extends TestSuite{
	public static Test suite(){

		final TestSuite tests = new TestSuite();
		tests.addTestSuite( AllTest.class );
		tests.addTestSuite( DeleteTest.class );
		tests.addTestSuite( GetTest.class );
		tests.addTestSuite( ListTest.class );
		tests.addTestSuite( NewTest.class );
		tests.addTestSuite( UpdatesTest.class );
		tests.addTestSuite( UpdateTest.class );
		tests.addTestSuite( VersionsTest.class );
		tests.addTestSuite( DetachTest.class );
		tests.addTestSuite( DocumentTest.class );
		tests.addTestSuite( CopyTest.class );
		tests.addTestSuite( LockTest.class );
		tests.addTestSuite( SaveAsTest.class );
		tests.addTestSuite( SearchTest.class );
		tests.addTest ( new InfostoreManagedTests() );

		return tests;
	}
}
