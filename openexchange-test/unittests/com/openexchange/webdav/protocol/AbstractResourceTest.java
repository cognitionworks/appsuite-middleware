package com.openexchange.webdav.protocol;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import junit.framework.TestCase;
import com.openexchange.webdav.protocol.Protocol.Property;
import com.openexchange.webdav.protocol.util.PropertySwitch;

public abstract class AbstractResourceTest extends TestCase implements PropertySwitch {
	
public static final int SKEW = 1000;
	
	protected WebdavFactory resourceManager;
	
	protected WebdavPath testCollection = new WebdavPath("public_infostore","testCollection"+Math.random());
	
	protected abstract WebdavFactory getWebdavFactory() throws Exception;
	protected abstract List<Property> getPropertiesToTest() throws Exception;
	protected abstract WebdavResource createResource() throws Exception;

	
	@Override
	public void setUp() throws Exception{
		resourceManager = getWebdavFactory();
		
		final WebdavResource resource = resourceManager.resolveCollection(testCollection);
		assertTrue(resource.isCollection());
		resource.create();
	}
	

	@Override
	public void tearDown() throws Exception {
		resourceManager.resolveCollection(testCollection).delete();
	}
	
	public void testProperties() throws Exception{
		WebdavResource res = createResource();
		WebdavProperty prop = new WebdavProperty();
		prop.setNamespace("OXTest");
		prop.setName("myvalue");
		prop.setLanguage("en");
		prop.setValue("testValue");
		
		res.putProperty(prop);
		res.save();
	
		res = resourceManager.resolveResource(res.getUrl());
		
		assertEquals(prop, res.getProperty("OXTest","myvalue"));
		
		prop = new WebdavProperty();
		prop.setNamespace("OXTest");
		prop.setName("myvalue");
		prop.setLanguage("en");
		prop.setValue("testValue2");
		
		res.putProperty(prop);
		res.save();
		
		res = resourceManager.resolveResource(res.getUrl());
		
		assertEquals(prop, res.getProperty("OXTest","myvalue"));
		
		res.removeProperty("OXTest","myvalue");
		res.save();
		
		assertNull(res.getProperty("OXTest","myvalue"));
	}
	
	public void testMandatoryProperties() throws Exception {
		final List<Property> mandatory = getPropertiesToTest();
		for(final Property prop : mandatory) {
			prop.doSwitch(this);
		}
	}	
	
	public void testCreateLoadAndDelete() throws Exception {
		WebdavResource resource = createResource();
		assertTrue(resource.exists());
		resource.delete();
		resource = resourceManager.resolveResource(resource.getUrl());
		assertFalse(resource.exists());
	}
	
	
	
	public static void assertEquals(final Date d1, final Date d2, final int skew){
		final long l1 = d1.getTime();
		final long l2 = d2.getTime();
		final long diff = (l1 < l2) ? l2 - l1 : l1 - l2;
		if(diff > skew) {
			assertEquals(l1,l2);
		}
		assertTrue(true);
	}
	
	public static void assertResources(final Iterable<WebdavResource> resources, final String...displayNames) throws WebdavProtocolException{
		//assertEquals(displayNames.length, resources.size());
		
		final Set<String> nameSet = new HashSet<String>(Arrays.asList(displayNames));
		
		for(final WebdavResource res : resources) {
			assertTrue(res.getDisplayName()+" not expected",nameSet.remove(res.getDisplayName()));
		}
		assertTrue(nameSet.toString(),nameSet.isEmpty());
	}
	
	public static void assertOptions(final Iterable<Protocol.WEBDAV_METHOD> expect, final Protocol.WEBDAV_METHOD...methods) throws WebdavProtocolException{
		//assertEquals(displayNames.length, resources.size());
		
		final Set<Protocol.WEBDAV_METHOD> methodSet = new HashSet<Protocol.WEBDAV_METHOD>(Arrays.asList(methods));
		
		for(final Protocol.WEBDAV_METHOD method : expect) {
			assertTrue(method+" not expected",methodSet.remove(method));
		}
		assertTrue(methodSet.toString(),methodSet.isEmpty());
	}
	
	public void throwEx(final Exception x) throws WebdavProtocolException {
	    throw new WebdavProtocolException(x, new WebdavPath(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}
}
