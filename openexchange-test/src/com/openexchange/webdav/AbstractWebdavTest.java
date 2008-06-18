package com.openexchange.webdav;

import java.util.Properties;

import junit.framework.TestCase;

import org.jdom.Namespace;

import com.meterware.httpunit.Base64;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.openexchange.groupware.configuration.AbstractConfigWrapper;
import com.openexchange.test.WebdavInit;
import com.openexchange.webdav.xml.GroupUserTest;

/**
 * @author <a href="mailto:sebastian.kauss@open-xchange.org">Sebastian Kauss</a>
 */

public abstract class AbstractWebdavTest extends TestCase {
	
	protected static final String PROTOCOL = "http://";
	
	protected static final String webdavPropertiesFile = "webdavPropertiesFile";
	
	protected static final String propertyHost = "hostname";
	
	protected static final Namespace webdav = Namespace.getNamespace("D", "DAV:");
	
	protected String hostName = "localhost";
	
	protected String login = null;
	
	protected String password = null;
	
	protected String secondlogin = null;
	
	protected int userId = -1;
	
	protected Properties webdavProps = null;
	
	protected String authData = null;
	
	protected WebRequest req = null;
	
	protected WebResponse resp = null;
	
	protected WebConversation webCon = null;
	
	protected WebConversation secondWebCon = null;
	
	protected static final int dayInMillis = 86400000;
	
	public static final String AUTHORIZATION = "authorization";
	
	public AbstractWebdavTest(final String name) {
		super(name);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		webCon = new WebConversation();
		secondWebCon = new WebConversation();
		
		webdavProps = WebdavInit.getWebdavProperties();
		
		login = AbstractConfigWrapper.parseProperty(webdavProps, "login", "");
		password = AbstractConfigWrapper.parseProperty(webdavProps, "password", "");

		secondlogin = AbstractConfigWrapper.parseProperty(webdavProps, "secondlogin", "");
		
		hostName = AbstractConfigWrapper.parseProperty(webdavProps, "hostname", "localhost");
		
		userId = GroupUserTest.getUserId(getWebConversation(), PROTOCOL + getHostName(), getLogin(), getPassword());
		assertTrue("user not found", userId != -1);
		
		authData = getAuthData(login, password);		
	} 
	
	protected static String getAuthData(final String login, String password) throws Exception {
		if (password == null) {
			password = "";
		}
		
		return new String(Base64.encode(login + ":" + password));
	}
	
	protected WebConversation getWebConversation() {
		return webCon;
	}
	
	protected WebConversation getNewWebConversation() {
		return new WebConversation();
	}

	protected WebConversation getSecondWebConversation() {
		return secondWebCon;
	}
	
	protected String getHostName() {
		return hostName;
	}
	
	protected String getLogin() {
		return login;
	}

	protected String getPassword() {
		return password;
	}

	protected String getSecondLogin() {
		return secondlogin;
	}
}
