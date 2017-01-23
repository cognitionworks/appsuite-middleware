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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.webdav;

import java.util.Properties;
import javax.xml.parsers.SAXParserFactory;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import com.meterware.httpunit.Base64;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.openexchange.groupware.configuration.AbstractConfigWrapper;
import com.openexchange.test.WebdavInit;
import com.openexchange.webdav.xml.GroupUserTest;
import com.openexchange.webdav.xml.framework.Constants;
import junit.framework.TestCase;

/**
 * @author <a href="mailto:sebastian.kauss@open-xchange.org">Sebastian Kauss</a>
 */
public abstract class AbstractWebdavTest extends TestCase {

    protected static final String PROTOCOL = "http://";

    protected static final String webdavPropertiesFile = "webdavPropertiesFile";

    protected static final String propertyHost = "hostname";

    protected static final Namespace webdav = Constants.NS_DAV;

    protected String hostName = "localhost";

    protected String login = null;

    protected String password = null;

    protected String secondlogin = null;

    protected String context;

    protected int userId = -1;

    protected Properties webdavProps = null;

    protected String authData = null;

    protected WebRequest req = null;

    protected WebResponse resp = null;

    protected WebConversation webCon = null;

    protected WebConversation secondWebCon = null;

    protected static final int dayInMillis = 86400000;

    public static final String AUTHORIZATION = "authorization";

    public AbstractWebdavTest() {
        super();
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
        context = AbstractConfigWrapper.parseProperty(webdavProps, "contextName", "defaultcontext");

        secondlogin = AbstractConfigWrapper.parseProperty(webdavProps, "secondlogin", "");

        hostName = AbstractConfigWrapper.parseProperty(webdavProps, "hostname", "localhost");

        try {
            SAXParserFactory fac = SAXParserFactory.newInstance();
            fac.setNamespaceAware(true);
            fac.setValidating(false);
            XMLReaders nonvalidating = XMLReaders.NONVALIDATING;
            new SAXBuilder();
        } catch (Throwable t) {
            throw new Exception(t);
        }
        userId = GroupUserTest.getUserId(getWebConversation(), PROTOCOL + getHostName(), getLogin(), getPassword(), context);
        assertTrue("user not found", userId != -1);

        authData = getAuthData(login, password, context);
    }

    protected static String getAuthData(String login, String password, String context) throws Exception {
        if (password == null) {
            password = "";
        }
        if (context != null && context.length() > 0) {
            login = login + "@" + context;
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
