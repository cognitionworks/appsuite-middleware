/*
 *
 *    OPEN-XCHANGE legal information
 *
 *    All intellectual property rights in the Software are protected by
 *    international copyright laws.
 *
 *
 *    In some countries OX, OX Open-Xchange, open xchange and OXtender
 *    as well as the corresponding Logos OX Open-Xchange and OX are registered
 *    trademarks of the Open-Xchange, Inc. group of companies.
 *    The use of the Logos is not covered by the GNU General Public License.
 *    Instead, you are allowed to use these Logos according to the terms and
 *    conditions of the Creative Commons License, Version 2.5, Attribution,
 *    Non-commercial, ShareAlike, and the interpretation of the term
 *    Non-commercial applicable to the aforementioned license is published
 *    on the web site http://www.open-xchange.com/EN/legal/index.html.
 *
 *    Please make sure that third-party modules and libraries are used
 *    according to their respective licenses.
 *
 *    Any modifications to this package must retain all copyright notices
 *    of the original copyright holder(s) for the original code used.
 *
 *    After any such modifications, the original and derivative code shall remain
 *    under the copyright of the copyright holder(s) and/or original author(s)per
 *    the Attribution and Assignment Agreement that can be located at
 *    http://www.open-xchange.com/EN/developer/. The contributing author shall be
 *    given Attribution for the derivative code and a license granting use.
 *
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
 *     Mail: info@open-xchange.com
 *
 *
 *     This program is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License, Version 2 as published
 *     by the Free Software Foundation.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *     or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *     for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc., 59
 *     Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package com.openexchange.test.osgi;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.openexchange.ajax.LoginTest;

/**
 * {@link BundleTestAuthentication} - Text absence of authentication bundle
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public final class BundleTestAuthentication extends AbstractBundleTest {

	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory
			.getLog(BundleTestAuthentication.class);

	private static final String BUNDLE_ID = "com.openexchange.authentication.database";

	private static final String LOGIN_URL = "/ajax/login";

	/**
	 * Initializes a new {@link BundleTestAuthentication}
	 */
	public BundleTestAuthentication(final String name) {
		super(name);
	}

	public void testAuthenticationAbsence() {
		try {
			final LoginTest loginTest = new LoginTest("LoginTest");
			final JSONObject jsonObject = login(getWebConversation(), loginTest.getHostName(), loginTest.getLogin(),
					loginTest.getPassword());

			/*
			 * Check for error
			 */
			assertTrue("No error contained in returned JSON object", jsonObject.has("error")
					&& !jsonObject.isNull("error"));

			/*
			 * Check for code "SRV-0001": Missing service
			 */
			assertTrue("Missing error code", jsonObject.has("code") && !jsonObject.isNull("code"));
			assertTrue("Unexpected error code: " + jsonObject.getString("code"), "SRV-0001".equals(jsonObject
					.get("code")));

			/*
			 * Check for proper error parameters
			 */
			assertTrue("Missing error parameters", jsonObject.has("error_params") && !jsonObject.isNull("error_params"));
			final JSONArray jArray = jsonObject.getJSONArray("error_params");
			assertTrue("Unexpected error parameters: " + jArray, jArray.length() == 1
					&& "com.openexchange.authentication.AuthenticationService".equals(jArray.getString(0)));

		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private static JSONObject login(final WebConversation conversation, final String hostname, final String login,
			final String password) throws IOException, SAXException, JSONException {
		final WebRequest req = new PostMethodWebRequest(PROTOCOL + hostname + LOGIN_URL);
		req.setParameter("action", "login");
		req.setParameter("name", login);
		req.setParameter("password", password);
		final WebResponse resp = conversation.getResponse(req);
		assertEquals("Response code is not okay.", HttpServletResponse.SC_OK, resp.getResponseCode());
		final String body = resp.getText();
		final JSONObject json;
		try {
			json = new JSONObject(body);
		} catch (final JSONException e) {
			LOG.error("Can't parse this body to JSON: \"" + body + '\"');
			throw e;
		}
		return json;
	}

	@Override
	protected String getBundleName() {
		return BUNDLE_ID;
	}

}
