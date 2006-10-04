package com.openexchange.webdav.action;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.openexchange.webdav.action.ifheader.IfHeader;
import com.openexchange.webdav.action.ifheader.IfHeaderParseException;
import com.openexchange.webdav.action.ifheader.IfHeaderParser;
import com.openexchange.webdav.protocol.WebdavCollection;
import com.openexchange.webdav.protocol.WebdavException;
import com.openexchange.webdav.protocol.WebdavFactory;
import com.openexchange.webdav.protocol.WebdavResource;

public class MockWebdavRequest implements WebdavRequest {

	private String url;
	private String uriPrefix = null;
	private WebdavFactory factory;
	private String content;
	private Map<String,String> headers = new HashMap<String,String>();
	private WebdavResource res = null;
	private WebdavResource dest;
	
	public MockWebdavRequest(WebdavFactory factory, String prefix) {
		this.factory = factory;
		this.uriPrefix = prefix;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}

	public WebdavResource getResource() throws WebdavException {
		if(res != null)
			return res;
		return res = factory.resolveResource(url);
	}
	
	public WebdavResource getDestination() throws WebdavException {
		if(null == getHeader("destination"))
			return null;
		if(dest != null)
			return dest;
		return dest = factory.resolveResource(getHeader("destination"));
	}
	
	public WebdavCollection getCollection() throws WebdavException {
		if(res != null)
			return (WebdavCollection) res;
		return (WebdavCollection) (res = factory.resolveCollection(url));
	}
	
	public String getDestinationUrl(){
		return getHeader("destination");
	}

	public String getUrl() {
		return url;
	}

	public void setBodyAsString(String content) {
		this.content = content;
	}
	
	public InputStream getBody(){
		try {
			return new ByteArrayInputStream(content.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void setHeader(String header, String value) {
		headers .put(header.toLowerCase(), value);
	}

	public String getHeader(String header) {
		return headers.get(header.toLowerCase());
	}

	public List<String> getHeaderNames() {
		return new LinkedList<String>(headers.keySet());
	}

	public Document getBodyAsDocument() throws JDOMException, IOException {
		return new SAXBuilder().build(getBody());
	}

	public String getURLPrefix() {
		return uriPrefix;
	}

	public IfHeader getIfHeader() throws IfHeaderParseException {
		String ifHeader = getHeader("If");
		if(ifHeader == null)
			return null;
		return new IfHeaderParser().parse(getHeader("If"));
	}

}
