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

package com.openexchange.calendar.printing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import junit.framework.TestCase;

/**
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 */
public class CPParameterTest extends TestCase {

    private class MockRequest implements HttpServletRequest {

        private Map<String, Object> attributes = new HashMap<String, Object>();
        private Map<String, String> parameters = new HashMap<String, String>();

        public String getAuthType() {
            return null;
        }

        public String getContextPath() {
            return null;
        }

        public Cookie[] getCookies() {
            return null;
        }

        public long getDateHeader(String name) {
            // TODO Auto-generated method stub
            return 0;
        }

        public String getHeader(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        public Enumeration getHeaderNames() {
            // TODO Auto-generated method stub
            return null;
        }

        public Enumeration getHeaders(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        public int getIntHeader(String name) {
            // TODO Auto-generated method stub
            return 0;
        }

        public String getMethod() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getPathInfo() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getPathTranslated() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getQueryString() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getRemoteUser() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getRequestURI() {
            // TODO Auto-generated method stub
            return null;
        }

        public StringBuffer getRequestURL() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getRequestedSessionId() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getServletPath() {
            // TODO Auto-generated method stub
            return null;
        }

        public HttpSession getSession() {
            // TODO Auto-generated method stub
            return null;
        }

        public HttpSession getSession(boolean create) {
            // TODO Auto-generated method stub
            return null;
        }

        public Principal getUserPrincipal() {
            // TODO Auto-generated method stub
            return null;
        }

        public boolean isRequestedSessionIdFromCookie() {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean isRequestedSessionIdFromURL() {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean isRequestedSessionIdFromUrl() {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean isRequestedSessionIdValid() {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean isUserInRole(String role) {
            // TODO Auto-generated method stub
            return false;
        }

        public Object getAttribute(String name) {
            return attributes.get(name);
        }

        public Enumeration getAttributeNames() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getCharacterEncoding() {
            // TODO Auto-generated method stub
            return null;
        }

        public int getContentLength() {
            // TODO Auto-generated method stub
            return 0;
        }

        public String getContentType() {
            // TODO Auto-generated method stub
            return null;
        }

        public ServletInputStream getInputStream() throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        public String getLocalAddr() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getLocalName() {
            // TODO Auto-generated method stub
            return null;
        }

        public int getLocalPort() {
            // TODO Auto-generated method stub
            return 0;
        }

        public Locale getLocale() {
            // TODO Auto-generated method stub
            return null;
        }

        public Enumeration getLocales() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getParameter(String name) {
            return parameters.get(name);
        }

        public Map<String,String> getParameterMap() {
            return parameters;
        }

        public Enumeration getParameterNames() {
            // TODO Auto-generated method stub
            return null;
        }

        public String[] getParameterValues(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        public String getProtocol() {
            // TODO Auto-generated method stub
            return null;
        }

        public BufferedReader getReader() throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * @see javax.servlet.ServletRequest#getRealPath(java.lang.String)
         */
        public String getRealPath(String path) {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * @see javax.servlet.ServletRequest#getRemoteAddr()
         */
        public String getRemoteAddr() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * @see javax.servlet.ServletRequest#getRemoteHost()
         */
        public String getRemoteHost() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * @see javax.servlet.ServletRequest#getRemotePort()
         */
        public int getRemotePort() {
            // TODO Auto-generated method stub
            return 0;
        }

        /*
         * (non-Javadoc)
         * @see javax.servlet.ServletRequest#getRequestDispatcher(java.lang.String)
         */
        public RequestDispatcher getRequestDispatcher(String path) {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * @see javax.servlet.ServletRequest#getScheme()
         */
        public String getScheme() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * @see javax.servlet.ServletRequest#getServerName()
         */
        public String getServerName() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * @see javax.servlet.ServletRequest#getServerPort()
         */
        public int getServerPort() {
            // TODO Auto-generated method stub
            return 0;
        }

        /*
         * (non-Javadoc)
         * @see javax.servlet.ServletRequest#isSecure()
         */
        public boolean isSecure() {
            // TODO Auto-generated method stub
            return false;
        }

        public void removeAttribute(String name) {
            attributes.remove(name);
        }

        public void setAttribute(String name, Object o) {
            attributes.put(name, o);
        }
        

        public void setParameter(String key, String value) {
            parameters.put(key,value);
        }

        public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
            // TODO Auto-generated method stub

        }

    }

    public void testShouldCryIfMissingFields() {
        MockRequest mockRequest = new MockRequest();
        CPParameters params = new CPParameters(mockRequest);
        assertTrue("No parameters given, should miss fields", params.isMissingFields());
    }

    public void testShouldCryIfCannotParseValue() {
        MockRequest mockRequest = new MockRequest();
        mockRequest.setParameter(CPParameters.WEEK_START_DAY, "Elvis");
        CPParameters params = new CPParameters(mockRequest);
        assertTrue("Parameter with bullshit value given, should fail", params.hasUnparseableFields());
        assertTrue("Parameter with bullshit value given, should be listed as missing field", params.getUnparseableFields().contains(
            CPParameters.WEEK_START_DAY));
    }

    public void testShouldNotLeaveMissingParamFieldEmptyWhenEncounteringNumberFormatExceptionWhileParsingFieldValue() {
        MockRequest mockRequest = new MockRequest();
        mockRequest.setAttribute(CPParameters.WEEK_START_DAY, "Elvis");
        CPParameters params = new CPParameters(mockRequest);
        assertTrue("Should still miss fields", params.isMissingFields());
        assertTrue("Should at least miss the field it was trying to parse", params.getMissingFields().contains(CPParameters.WEEK_START_DAY));

    }
}
