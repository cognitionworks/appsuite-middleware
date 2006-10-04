package com.openexchange.webdav.action;

import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.jdom.Namespace;

import com.openexchange.test.XMLCompare;
import com.openexchange.webdav.protocol.Protocol;
import com.openexchange.webdav.protocol.WebdavProperty;
import com.openexchange.webdav.protocol.WebdavResource;
import com.openexchange.webdav.protocol.util.Utils;

public class PropfindTest extends ActionTestCase {
	
	private static final Namespace TEST_NS = Namespace.getNamespace("http://www.open-xchange.com/namespace/webdav-test");
	
	//TODO: noroot
	
	public void testOneProperty() throws Exception {
		final String INDEX_HTML_URL = testCollection+"/index.html";
		
		Date lastModified = factory.resolveResource(INDEX_HTML_URL).getLastModified();
		
		String body = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:propfind xmlns:D=\"DAV:\"><D:prop><D:getlastmodified/></D:prop></D:propfind>";
		String expect = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:multistatus xmlns:D=\"DAV:\"><D:response><D:href>http://localhost/"+INDEX_HTML_URL+"</D:href><D:propstat><D:prop><D:getlastmodified>"+Utils.convert(lastModified)+"</D:getlastmodified></D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat></D:response></D:multistatus>";
		
		MockWebdavRequest req = new MockWebdavRequest(factory, "http://localhost/");
		MockWebdavResponse res = new MockWebdavResponse();
		
		req.setBodyAsString(body);
		req.setUrl(INDEX_HTML_URL);
		
		WebdavAction action = new WebdavPropfindAction();
		action.perform(req, res);
		
		assertEquals(Protocol.SC_MULTISTATUS, res.getStatus());
		
		
		XMLCompare compare = new XMLCompare();
		compare.setCheckTextNames("getlastmodified","status");
		assertTrue(compare.compare(expect, res.getResponseBodyAsString()));
		
		req = new MockWebdavRequest(factory, "http://localhost/");
		res = new MockWebdavResponse();
		
		req.setBodyAsString(body);
		req.setUrl(INDEX_HTML_URL);
		req.setHeader("depth", "0");
		action.perform(req, res);
		
		assertEquals(Protocol.SC_MULTISTATUS, res.getStatus());
		
		assertTrue(compare.compare(expect, res.getResponseBodyAsString()));
		
	}	
	
	public void testManyProperties() throws Exception {
		final String INDEX_HTML_URL = testCollection+"/index.html";
		
		Date lastModified = factory.resolveResource(INDEX_HTML_URL).getLastModified();
		
		String body = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:propfind xmlns:D=\"DAV:\"><D:prop><D:getlastmodified/><D:displayname/><D:resourcetype/></D:prop></D:propfind>";
		String expect = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:multistatus xmlns:D=\"DAV:\"><D:response><D:href>http://localhost/"+INDEX_HTML_URL+"</D:href><D:propstat><D:prop><D:getlastmodified>"+Utils.convert(lastModified)+"</D:getlastmodified><D:displayname>index.html</D:displayname><D:resourcetype /></D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat></D:response></D:multistatus>";
		
		MockWebdavRequest req = new MockWebdavRequest(factory, "http://localhost/");
		MockWebdavResponse res = new MockWebdavResponse();
		
		req.setBodyAsString(body);
		req.setUrl(INDEX_HTML_URL);
		
		WebdavAction action = new WebdavPropfindAction();
		action.perform(req, res);
		assertEquals(Protocol.SC_MULTISTATUS, res.getStatus());
		
		XMLCompare compare = new XMLCompare();
		compare.setCheckTextNames("getlastmodified", "displayname","resourcetype","status" );
		
		assertTrue(compare.compare(expect, res.getResponseBodyAsString()));
	}
	
	public void testPropNames() throws Exception {
		final String INDEX_HTML_URL = testCollection+"/index.html";
		
		String body = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:propfind xmlns:D=\"DAV:\"><D:propname /></D:propfind>";
		String expect = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><multistatus xmlns=\"DAV:\"><response><href>http://localhost/"+INDEX_HTML_URL+"</href><propstat><prop><getlastmodified /><creationdate /><resourcetype /><displayname /><getcontentlanguage /><getcontentlength /><getcontenttype /><getetag /><lockdiscovery /><supportedlock /><source /></prop><status>HTTP/1.1 200 OK</status></propstat></response></multistatus>";
		
		MockWebdavRequest req = new MockWebdavRequest(factory, "http://localhost/");
		MockWebdavResponse res = new MockWebdavResponse();
		
		req.setBodyAsString(body);
		req.setUrl(INDEX_HTML_URL);
		
		WebdavAction action = new WebdavPropfindAction();
		action.perform(req, res);
		assertEquals(Protocol.SC_MULTISTATUS, res.getStatus());
		
		XMLCompare compare = new XMLCompare();
		compare.setCheckTextNames("creationdate", "resourcetype", "displayname", "getcontenttype", "getcontentlanguage", "getcontentlength", "getlastmodified", "getetag","source","status","lockdiscovery" ,"supportedlock");
		
		assertTrue(compare.compare(expect, res.getResponseBodyAsString()));
	}
	
	public void testAllProperties() throws Exception {
		final String INDEX_HTML_URL = testCollection+"/index.html";
		
		WebdavResource resource = factory.resolveResource(INDEX_HTML_URL);
		
		String body = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:propfind xmlns:D=\"DAV:\"><D:allprop /></D:propfind>"; 
		String expect = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><multistatus xmlns=\"DAV:\"><response><href>http://localhost/"+INDEX_HTML_URL+"</href><propstat><prop><getlastmodified>"+Utils.convert(resource.getLastModified())+"</getlastmodified> <creationdate>"+Utils.convert(resource.getCreationDate())+"</creationdate><resourcetype /><displayname>"+resource.getDisplayName()+"</displayname><getcontentlanguage>"+resource.getLanguage()+"</getcontentlanguage><getcontentlength>"+resource.getLength()+"</getcontentlength><getcontenttype>"+resource.getContentType()+"</getcontenttype><getetag>"+resource.getETag()+"</getetag><lockdiscovery /><supportedlock><lockentry><lockscope><exclusive/></lockscope><locktype><write/></locktype></lockentry><lockentry><lockscope><shared/></lockscope><locktype><write/></locktype></lockentry></supportedlock><source /></prop><status>HTTP/1.1 200 OK</status></propstat></response></multistatus>";
		
		MockWebdavRequest req = new MockWebdavRequest(factory, "http://localhost/");
		MockWebdavResponse res = new MockWebdavResponse();
		
		req.setBodyAsString(body);
		req.setUrl(INDEX_HTML_URL);
		
		WebdavAction action = new WebdavPropfindAction();
		action.perform(req, res);
		assertEquals(Protocol.SC_MULTISTATUS, res.getStatus());
		
		XMLCompare compare = new XMLCompare();
		compare.setCheckTextNames("creationdate", "resourcetype", "displayname", "getcontenttype", "getcontentlanguage", "getcontentlength", "getlastmodified", "getetag", "source","lockdiscovery" ,"supportedlock"); // FIXME: lockdiscovery und supportedlock
		assertTrue(compare.compare(expect, res.getResponseBodyAsString()));
	}
	
	public void testCollection() throws Exception {
		final String DEVELOPMENT_URL = testCollection+"/development";
		final String PM_URL = testCollection+"/pm";
		final String INDEX_HTML_URL = testCollection+"/index.html";
		final String SITEMAP_HTML_URL = testCollection+"/sitemap.html";
		final String GUI_URL = DEVELOPMENT_URL+"/gui";
		final String INDEX3_HTML_URL = GUI_URL+"/index3.html";
		
		String testCollDispName = factory.resolveResource(testCollection).getDisplayName();
		
		String body = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:propfind xmlns:D=\"DAV:\"><D:prop><D:displayname/><D:resourcetype /></D:prop></D:propfind>";
		String expect = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:multistatus xmlns:D=\"DAV:\"><D:response><D:href>http://localhost/"+DEVELOPMENT_URL+"</D:href><D:propstat><D:prop><D:displayname>development</D:displayname><D:resourcetype><D:collection /></D:resourcetype></D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat></D:response></D:multistatus>";
		
		MockWebdavRequest req = new MockWebdavRequest(factory, "http://localhost/");
		MockWebdavResponse res = new MockWebdavResponse();
		
		req.setBodyAsString(body);
		req.setUrl(DEVELOPMENT_URL);
		req.setHeader("depth", "0");
		
		WebdavAction action = new WebdavPropfindAction();
		action.perform(req, res);
		assertEquals(Protocol.SC_MULTISTATUS, res.getStatus());
		
		XMLCompare compare = new XMLCompare();
		compare.setCheckTextNames("displayname","status");
		assertTrue(compare.compare(expect, res.getResponseBodyAsString()));
		
		body = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:propfind xmlns:D=\"DAV:\"><D:prop><D:displayname/></D:prop></D:propfind>";
		expect = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:multistatus xmlns:D=\"DAV:\"><D:response><D:href>http://localhost"+testCollection+"</D:href><D:propstat><D:prop><D:displayname>"+testCollDispName+"</D:displayname></D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat></D:response><D:response><D:href>http://localhost/"+DEVELOPMENT_URL+"</D:href><D:propstat><D:prop><D:displayname>development</D:displayname></D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat></D:response><D:response><D:href>http://localhost/"+PM_URL+"</D:href><D:propstat><D:prop><D:displayname>pm</D:displayname></D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat></D:response><D:response><D:href>http://localhost/"+INDEX_HTML_URL+"</D:href><D:propstat><D:prop><D:displayname>index.html</D:displayname></D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat></D:response><D:response><D:href>http://localhost/"+SITEMAP_HTML_URL+"</D:href><D:propstat><D:prop><D:displayname>sitemap.html</D:displayname></D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat></D:response></D:multistatus>";
		res = new MockWebdavResponse();
		
		req = new MockWebdavRequest(factory, "http://localhost/");
		
		req.setBodyAsString(body);
		req.setHeader("depth","1");
		req.setUrl(testCollection);
		
		action.perform(req, res);
		assertTrue(compare.compare(expect, res.getResponseBodyAsString()));
		
		expect = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:multistatus xmlns:D=\"DAV:\"><D:response><D:href>http://localhost/"+DEVELOPMENT_URL+"</D:href><D:propstat><D:prop><D:displayname>development</D:displayname></D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat></D:response><D:response><D:href>http://localhost/"+GUI_URL+"</D:href><D:propstat><D:prop><D:displayname>gui</D:displayname></D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat></D:response><D:response><D:href>http://localhost/"+INDEX3_HTML_URL+"</D:href><D:propstat><D:prop><D:displayname>index3.html</D:displayname></D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat></D:response></D:multistatus>";
		res = new MockWebdavResponse();
		req = new MockWebdavRequest(factory, "http://localhost/");
		
		req.setBodyAsString(body);
		req.setHeader("depth","infinity");
		req.setUrl(DEVELOPMENT_URL);
		
		action.perform(req, res);
		assertEquals(Protocol.SC_MULTISTATUS, res.getStatus());
		
		assertTrue(compare.compare(expect, res.getResponseBodyAsString()));
		
	}
	
	public void testXMLProperty() throws Exception {
		final String INDEX_HTML_URL = testCollection+"/index.html";
		
		WebdavResource resource = factory.resolveResource(INDEX_HTML_URL);
		WebdavProperty property = new WebdavProperty();
		property.setNamespace(TEST_NS.getURI());
		property.setName("test");
		property.setValue("<quark xmlns=\"http://www.open-xchange.com/namespace/webdav-test\"> In the left corner: The incredible Tessssssst Vallllhhhhuuuuuueeeee!</quark>");
		property.setXML(true);
		resource.putProperty(property);
		resource.save();
		
		String body = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:propfind xmlns:D=\"DAV:\" xmlns:OX=\""+TEST_NS.getURI()+"\"><D:prop><OX:test/></D:prop></D:propfind>";
		String expect = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><D:multistatus xmlns:D=\"DAV:\" xmlns:OX=\""+TEST_NS.getURI()+"\"><D:response><D:href>http://localhost/"+INDEX_HTML_URL+"</D:href><D:propstat><D:prop><OX:test><OX:quark> In the left corner: The incredible Tessssssst Vallllhhhhuuuuuueeeee!</OX:quark></OX:test></D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat></D:response></D:multistatus>";
		
		MockWebdavRequest req = new MockWebdavRequest(factory, "http://localhost/");
		MockWebdavResponse res = new MockWebdavResponse();
		
		req.setBodyAsString(body);
		req.setUrl(INDEX_HTML_URL);
		
		WebdavAction action = new WebdavPropfindAction();
		action.perform(req, res);
		assertEquals(Protocol.SC_MULTISTATUS, res.getStatus());
		
		XMLCompare compare = new XMLCompare();
		compare.setCheckTextNames("test","quark", "status");
		
		assertTrue(compare.compare(expect, res.getResponseBodyAsString()));
		
	}
	
}
