package com.openexchange.webdav.protocol;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import com.openexchange.tools.collections.Collector;
import com.openexchange.tools.collections.Injector;
import com.openexchange.tools.collections.OXCollections;
import com.openexchange.webdav.protocol.Protocol.Property;


public class CollectionTest extends ResourceTest {
	
	public static final String INDEX_HTML = "<html><head /><body>Company Site</body></html>";
	public static final String SITEMAP_HTML = "<html><head /><body>You are here</body></html>";
	public static final String INDEX3_HTML = "<html><head /><body>GUI Site</body></html>";
	public static final String INDEX2_HTML = "<html><head /><body>PM Site</body></html>";

	public void testRoot() throws Exception {
		
		List<WebdavResource> children = FACTORY.resolveCollection("/").getChildren();
		int size = children.size();
		FACTORY.resolveCollection("/test").create();
		List<WebdavResource> childrenAfter = FACTORY.resolveCollection("/").getChildren();
		assertEquals(childrenAfter.toString(), size+1, childrenAfter.size());
		
		Set<String> childrenNames = new HashSet<String>();
		for(WebdavResource res : childrenAfter) { childrenNames.add(res.getDisplayName()); }
		for(WebdavResource res : children) { childrenNames.remove(res.getDisplayName());   }
		assertEquals("test" , childrenNames.iterator().next());
	}

	@Override
	public void testBody() throws Exception {
		WebdavCollection coll = createResource().toCollection();
		
		String content = "Hallo Welt!";
		byte[] bytes = content.getBytes("UTF-8");
		
		try {
			coll.putBody(new ByteArrayInputStream(bytes));
			fail("Collections shouldn't accept bodies");
		} catch (WebdavException x) {
			assertEquals(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,x.getStatus());
		}
	}
	
	public static void createStructure(WebdavCollection coll, WebdavFactory factory) throws WebdavException, UnsupportedEncodingException {
		String content = "Hallo Welt!";
		byte[] bytes = content.getBytes("UTF-8");
		
		WebdavResource res = coll.resolveResource("index.html");
		res.putBody(new ByteArrayInputStream(INDEX_HTML.getBytes("UTF-8")));
		res.setContentType("text/html");
		res.putBodyAndGuessLength(new ByteArrayInputStream(bytes));
		res.create();
		
		res = coll.resolveResource("sitemap.html");
		res.putBody(new ByteArrayInputStream(SITEMAP_HTML.getBytes("UTF-8")));
		res.setContentType("text/html");
		res.setLength((long)SITEMAP_HTML.getBytes("UTF-8").length);
		res.create();
		
		res = coll.resolveCollection("development");
		res.create();
		
		res = res.toCollection().resolveCollection("gui");
		res.create();
		
		res = res.toCollection().resolveResource("index3.html");
		res.putBody(new ByteArrayInputStream(INDEX3_HTML.getBytes("UTF-8")));
		res.setContentType("text/html");
		res.setLength((long)INDEX3_HTML.getBytes("UTF-8").length);
		res.create();
		
		res = factory.resolveCollection(coll.getUrl()+"/pm");
		res.create();
		
		res = coll.resolveCollection("pm").resolveResource("index2.html");
		res.putBody(new ByteArrayInputStream(INDEX2_HTML.getBytes("UTF-8")));
		res.setContentType("text/html");
		res.setLength((long)INDEX2_HTML.getBytes("UTF-8").length);
		res.create();
	}
	
	public void testChildren() throws Exception {
		WebdavCollection coll = createResource().toCollection();
		createStructure(coll, resourceManager);
		
		List<WebdavResource> children = coll.getChildren();
		assertResources(children, "index.html", "sitemap.html", "development", "pm");
		
		WebdavCollection dev = coll.resolveCollection("development");
		children = dev.getChildren();
		assertResources(children,"gui");
		
		WebdavCollection gui = dev.resolveCollection("gui");
		children = gui.getChildren();
		assertResources(children, "index3.html");
		
		WebdavCollection pm = coll.resolveCollection("pm");
		children = pm.getChildren();
		assertResources(children,"index2.html");
		
		WebdavResource res = pm.resolveResource("index2.html");
		res.delete();
		
		children = pm.getChildren();
		assertResources(children);
		
		
	}

	public void testIterate() throws Exception {
		WebdavCollection coll = createResource().toCollection();
		createStructure(coll, resourceManager);
		assertResources(coll,"index.html", "sitemap.html", "development", "pm","gui","index2.html", "index3.html"); // Note: Children ONLY
		assertResources(coll.toIterable(1), "index.html", "sitemap.html", "development", "pm");
		assertResources(coll.toIterable(0));
		
		try{
			coll.toIterable(23);
			fail();
		} catch (IllegalArgumentException x) {
			assertTrue(true);
		}
	}
	
	public void testDelete() throws Exception {
		WebdavCollection coll = createResource().toCollection();
		createStructure(coll, resourceManager);
		WebdavCollection dev = coll.resolveCollection("development");
		
		List<WebdavResource> subList = new ArrayList<WebdavResource>();
		subList.add(dev);
		subList = OXCollections.inject(subList, dev, new Collector<WebdavResource>());
		
		dev.delete();
		assertResources(coll,"index.html", "sitemap.html", "pm","index2.html");
		
		assertFalse(dev.exists());
		for(WebdavResource res : subList) {
			assertFalse(res.exists());
		}
	}
	
	public void testMove() throws Exception {
		WebdavCollection coll = createResource().toCollection();
		createStructure(coll, resourceManager);
		WebdavCollection dev = coll.resolveCollection("development");
		
		Date lastModified = dev.getLastModified();
		Date creationDate = dev.getCreationDate();
		
		dev.setDisplayName("myDisplayName");
		
		WebdavProperty prop = new WebdavProperty();
		prop.setName("myvalue");
		prop.setNamespace("ox");
		prop.setValue("gnaaa!");
		
		dev.putProperty(prop);
		
		
		List<String> subList = new ArrayList<String>();
		subList = OXCollections.inject(subList, dev, new DisplayNameCollector());
		
		Thread.sleep(1000);
		
		dev.move(coll.getUrl()+"/dev2");
		
		assertFalse(dev.exists());
		
		WebdavCollection dev2 = coll.resolveCollection("dev2");
		assertResources(dev2, subList.toArray(new String[subList.size()]));
		
		assertEquals("myDisplayName",dev2.getDisplayName());
		assertFalse(lastModified.equals(dev2.getLastModified()));
		assertEquals(creationDate,dev2.getCreationDate());
		assertEquals("gnaaa!",dev2.getProperty("ox","myvalue").getValue());
		
		dev.create();
		lastModified = dev.getLastModified();
		Thread.sleep(1000);
		
		dev2.move(dev.getUrl(), true, true);
		assertEquals(lastModified, dev.getLastModified());
		
	}
	
	public void testCopy() throws Exception {
		WebdavCollection coll = createResource().toCollection();
		createStructure(coll, resourceManager);
		WebdavCollection dev = coll.resolveCollection("development");
		
		Date lastModified = dev.getLastModified();
		Date creationDate = dev.getCreationDate();
		
		dev.setDisplayName("myDisplayName");
		
		WebdavProperty prop = new WebdavProperty();
		prop.setName("myvalue");
		prop.setNamespace("ox");
		prop.setValue("gnaaa!");
		
		dev.putProperty(prop);
		
		
		List<String> subList = new ArrayList<String>();
		subList = OXCollections.inject(subList, dev, new DisplayNameCollector());
		
		Thread.sleep(1000);
		dev.copy(coll.getUrl()+"/dev2");
		
		assertTrue(dev.exists());
		
		WebdavCollection dev2 = coll.resolveCollection("dev2");
		assertResources(dev2, subList.toArray(new String[subList.size()]));
		
		assertEquals("myDisplayName",dev2.getDisplayName());
		assertFalse(lastModified.equals(dev2.getLastModified()));
		assertFalse(creationDate.equals(dev2.getCreationDate()));
		assertEquals("gnaaa!",dev2.getProperty("ox","myvalue").getValue());
		
	}

	public void testConflict() throws Exception {
		WebdavResource res = super.createResource();
		try {
			resourceManager.resolveCollection(res.getUrl()+"/collection").create();
			fail();
		} catch (WebdavException x) {
			assertEquals(HttpServletResponse.SC_CONFLICT,x.getStatus());
		}
	}
	
	public void testMethodNotAllowed() throws Exception {
		WebdavCollection col = createResource().toCollection();
		try {
			col.create();
			fail();
		} catch (WebdavException x) {
			assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, x.getStatus());
		}
	}
	@Override
	public void testLock() throws Exception {
		super.testLock();
		WebdavCollection coll = createResource().toCollection();
		createStructure(coll, resourceManager);
		
		//Test Depth-Infinity lock
		WebdavLock lock = new WebdavLock();
		lock.setType(WebdavLock.Type.WRITE_LITERAL);
		lock.setScope(WebdavLock.Scope.EXCLUSIVE_LITERAL);
		lock.setDepth(WebdavCollection.INFINITY);
		lock.setOwner("me");
		lock.setTimeout(WebdavLock.NEVER);
		
		coll.lock(lock);
		
		Set<String> urls = new HashSet<String>();
		for(WebdavResource res : coll) {
			urls.add(res.getUrl());
			assertNotNull(res.getLock(lock.getToken()));
		}
		
		coll.unlock(lock.getToken());
		
		// Test Depth 1 lock
		lock.setToken(null);
		lock.setDepth(1);
		coll.lock(lock);
		
		for(WebdavResource res : coll.toIterable(1)){
			urls.remove(res.getUrl());
			assertNotNull(res.getLock(lock.getToken()));
		}
		
		// All level 2+ resources should be left in urls and should not be locked
		
		for(String url : urls) {
			WebdavResource res = resourceManager.resolveResource(url);
			assertEquals(0, res.getLocks().size());
		}
		
		coll.unlock(lock.getToken());
	}
	
	@Override
	protected WebdavResource createResource() throws WebdavException{
		WebdavResource resource = FACTORY.resolveCollection(testCollection+"/testResource"+Math.random());
		assertFalse(resource.exists());
		resource.create();
		resource = resourceManager.resolveResource(resource.getUrl());
		assertTrue(resource.exists());
		return resource;
	}

	@Override
	protected List<Property> getPropertiesToTest() {
		return FACTORY.getProtocol().VALUES;
	}

	@Override
	public Object resourceType() throws WebdavException {
		WebdavCollection coll = createResource().toCollection();
		assertEquals(Protocol.COLLECTION, coll.getResourceType());
		assertEquals(coll.getResourceType(), coll.getProperty("DAV:", "resourcetype").getValue());
		return null;
	}

	@Override
	public Object contentLanguage() throws WebdavException {
		WebdavResource res = createResource();
		String defaultLanguage = null;
		assertEquals(res.getLanguage(), res.getProperty("DAV:", "getcontentlanguage"));
		assertEquals(defaultLanguage, res.getLanguage());
		
		try {
			res.setLanguage("de");
			fail("Could update language");
		} catch (WebdavException x) {
			assertTrue(true);
		}
		
		WebdavProperty prop = Protocol.GETCONTENTLANGUAGE_LITERAL.getWebdavProperty();
		prop.setValue("de");
		
		try {
			res.putProperty(prop);
			fail("Could update language");
		} catch (WebdavException x) {
			assertTrue(true);
		}
		
		return null;
	}

	@Override
	public Object contentLength() throws WebdavException {
		WebdavResource res = createResource();
		assertEquals(res.getLength(), res.getProperty("DAV:", "getcontentlength"));
		assertEquals(null, res.getLength());
		
		
		try {
			res.setLength(23l);
			fail("Could update length");
		} catch (WebdavException x) {
			assertTrue(true);
		}
		
		WebdavProperty prop = Protocol.GETCONTENTLENGTH_LITERAL.getWebdavProperty();
		prop.setValue("2");
		
		try {
			res.putProperty(prop);
			fail("Could update length");
		} catch (WebdavException x) {
			assertTrue(true);
		}

		return null;
	}

	@Override
	public Object etag() throws WebdavException{
		WebdavResource res = createResource();
		assertEquals(res.getETag(), res.getProperty("DAV:", "getetag"));
		assertEquals(null, res.getETag());
		
		return null;
	}
	
	@Override
	public Object contentType() throws WebdavException {
		WebdavResource res = createResource();
		try {
			res.setContentType("text/plain");
			fail("Could update content type");
		} catch (WebdavException x) {
			assertTrue(true);
		}
		
		WebdavProperty prop = Protocol.GETCONTENTTYPE_LITERAL.getWebdavProperty();
		prop.setValue("text/plain");
		
		try {
			res.putProperty(prop);
			fail("Could update content type");
		} catch (WebdavException x) {
			assertTrue(true);
		}

		return null;
	}
	
	
	protected static final class DisplayNameCollector implements Injector<List<String>, WebdavResource> {

		public List<String> inject(List<String> list, WebdavResource element) {
			try {
				list.add(element.getDisplayName());
			} catch (WebdavException e) {
				list.add(e.toString());
			}
			return list;
		}
		
	}
}
