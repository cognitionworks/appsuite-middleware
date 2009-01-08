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

package com.openexchange.tools.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import com.openexchange.ajp13.AJPv13ServletInputStream;
import com.openexchange.ajp13.exception.AJPv13Exception;
import com.openexchange.ajp13.exception.AJPv13Exception.AJPCode;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.configuration.ServerConfig.Property;
import com.openexchange.mail.MailException;
import com.openexchange.mail.mime.ContentType;

/**
 * {@link ServletRequestWrapper}
 * 
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ServletRequestWrapper implements ServletRequest {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ServletRequestWrapper.class);

    private static final Set<String> singleValueHeaders = new HashSet<String>();

    public static final String CONTENT_TYPE = "content-type";

    public static final String CONTENT_LENGTH = "content-length";

    static {
        singleValueHeaders.add(CONTENT_TYPE);
        singleValueHeaders.add(CONTENT_LENGTH);
    }

    private final Map<String, Object> attributes;

    private final Map<String, String[]> parameters;

    protected final Map<String, String[]> headers;

    private String characterEncoding;

    private String protocol = "HTTP/1.1";

    private String remote_addr;

    private String remote_host;

    private String server_name;

    private String scheme;

    private int server_port;

    private boolean is_secure;

    private AJPv13ServletInputStream servletInputStream;

    /**
     * Initializes a new {@link ServletRequestWrapper}
     * 
     * @throws AJPv13Exception If instantiation fails
     */
    public ServletRequestWrapper() throws AJPv13Exception {
        super();
        attributes = new HashMap<String, Object>();
        parameters = new HashMap<String, String[]>();
        headers = new HashMap<String, String[]>();
        setHeaderInternal(CONTENT_LENGTH, String.valueOf(-1), false);
    }

    public void setContentLength(final int contentLength) throws AJPv13Exception {
        setHeaderInternal(CONTENT_LENGTH, String.valueOf(contentLength), false);
    }

    public void setContentType(final String contentType) throws AJPv13Exception {
        setHeaderInternal(CONTENT_TYPE, contentType, true);
    }

    public void setParameter(final String name, final String value) {
        if (parameters.containsKey(name)) {
            final String[] oldValues = parameters.get(name);
            final String[] newValues = new String[oldValues.length + 1];
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
            newValues[newValues.length - 1] = value;
            parameters.put(name, newValues);
        } else {
            parameters.put(name, new String[] { value });
        }
    }

    public final void setHeader(final String nameArg, final String value, final boolean isContentType) throws AJPv13Exception {
        setHeaderInternal(nameArg.toLowerCase(Locale.ENGLISH), value, isContentType);
    }

    private final void setHeaderInternal(final String name, final String value, final boolean isContentType) throws AJPv13Exception {
        if (isContentType) {
            handleContentType(value);
        }
        if (headers.containsKey(name) && !singleValueHeaders.contains(name)) {
            /*
             * Header may carry multiple values
             */
            final String[] oldValues = headers.get(name);
            final String[] newValues = new String[oldValues.length + 1];
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
            newValues[newValues.length - 1] = value;
            headers.put(name, newValues);
        } else {
            headers.put(name, new String[] { value });
        }
    }

    private final void handleContentType(final String value) throws AJPv13Exception {
        if (value != null && value.length() > 0) {
            final ContentType ct;
            try {
                ct = new ContentType(value);
            } catch (final MailException e) {
                LOG.error(e.getMessage(), e);
                throw new AJPv13Exception(AJPCode.INVALID_CONTENT_TYPE, true, e, value);
            }
            if (ct.getCharsetParameter() == null) {
                /*
                 * Although http defines to use charset "ISO-8859-1" if protocol is set to "HTTP/1.1", we use a pre-defined charset given
                 * through config file
                 */
                try {
                    setCharacterEncoding(ServerConfig.getProperty(Property.DefaultEncoding));
                } catch (final UnsupportedEncodingException e) {
                    throw new AJPv13Exception(AJPCode.UNSUPPORTED_ENCODING, true, e, ServerConfig.getProperty(Property.DefaultEncoding));
                }
            } else {
                try {
                    setCharacterEncoding(ct.getCharsetParameter());
                } catch (final UnsupportedEncodingException e) {
                    throw new AJPv13Exception(AJPCode.UNSUPPORTED_ENCODING, true, e, ct.getCharsetParameter());
                }
            }
        } else {
            /*
             * Although http defines to use charset "ISO-8859-1" if protocol is set to "HTTP/1.1", we use a pre-defined charset given
             * through config file
             */
            try {
                setCharacterEncoding(ServerConfig.getProperty(Property.DefaultEncoding));
            } catch (final UnsupportedEncodingException e) {
                throw new AJPv13Exception(AJPCode.UNSUPPORTED_ENCODING, true, e, ServerConfig.getProperty(Property.DefaultEncoding));
            }
        }
    }

    public String getHeader(final String nameArg) {
        final String name = nameArg.toLowerCase(Locale.ENGLISH);
        return headers.containsKey(name) ? makeString(headers.get(name)) : null;
    }

    public boolean containsHeader(final String name) {
        return headers.containsKey(name.toLowerCase(Locale.ENGLISH));
    }

    public Enumeration<?> getHeaders(final String name) {
        return makeEnumeration(headers.get(name.toLowerCase(Locale.ENGLISH)));
    }

    public Enumeration<?> getHeaderNames() {
        return new IteratorEnumeration(headers.keySet().iterator());
    }

    public void setParameterValues(final String name, final String[] values) {
        parameters.put(name, values);
    }

    public String[] getParameterValues(final String name) {
        return parameters.get(name);
    }

    public String getParameter(final String name) {
        return parameters.containsKey(name) ? (parameters.get(name))[0] : null;
    }

    public Enumeration<?> getParameterNames() {
        return new IteratorEnumeration(parameters.keySet().iterator());
    }

    public Map<?, ?> getParameterMap() {
        return parameters;
    }

    public Object getAttribute(final String name) {
        return attributes.get(name);
    }

    public boolean containsAttribute(final String name) {
        return attributes.containsKey(name);
    }

    public Enumeration<?> getAttributeNames() {
        return new IteratorEnumeration(attributes.keySet().iterator());
    }

    public void removeAttribute(final String name) {
        attributes.remove(name);
    }

    public void setAttribute(final String name, final Object value) {
        if (value != null) {
            attributes.put(name, value);
        }
    }

    public String getRealPath(final String string) {
        return null;
    }

    public RequestDispatcher getRequestDispatcher(final String string) {
        return null;
    }

    public void setCharacterEncoding(final String characterEncodingArg) throws UnsupportedEncodingException {
        String characterEncoding = characterEncodingArg;
        if (characterEncoding.charAt(0) == '"' && characterEncoding.charAt(characterEncoding.length() - 1) == '"') {
            characterEncoding = characterEncoding.substring(1, characterEncoding.length() - 1);
        }
        new String(new byte[] {}, characterEncoding);
        this.characterEncoding = characterEncoding;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }

    public Enumeration<?> getLocales() {
        return null;
    }

    public Locale getLocale() {
        return null;
    }

    /**
     * Sets the servlet input stream of this servlet request
     * 
     * @param is The servlet input stream
     */
    public void setInputStream(final AJPv13ServletInputStream is) {
        servletInputStream = is;
    }

    /**
     * Sets/appends new data to this servlet request's input stream
     * 
     * @param newData The new data to set/append
     * @throws IOException If an I/O error occurs
     */
    public void setData(final byte[] newData) throws IOException {
        servletInputStream.setData(newData);
    }

    public ServletInputStream getInputStream() throws IOException {
        if (servletInputStream == null) {
            throw new IOException("no ServletInputStream found!");
        }
        return servletInputStream;
    }

    public void removeInputStream() {
        servletInputStream = null;
    }

    public String getContentType() {
        return getHeader(CONTENT_TYPE);
    }

    public int getContentLength() {
        return Integer.parseInt(getHeader(CONTENT_LENGTH));
    }

    public String getCharacterEncoding() {
        /*
         * if (characterEncoding == null) { // CHARACTER ENCODING MUST NOT BE NULL characterEncoding =
         * ServerConfig.getProperty(Property.DefaultEncoding); }
         */
        return characterEncoding;
    }

    public BufferedReader getReader() {
        return null;
    }

    public void setRemoteAddr(final String remote_addr) {
        this.remote_addr = remote_addr;
    }

    public String getRemoteAddr() {
        return remote_addr;
    }

    public void setRemoteHost(final String remote_host) {
        this.remote_host = remote_host;
    }

    public String getRemoteHost() {
        return remote_host;
    }

    public String getScheme() {
        if (scheme == null) {
            if (protocol == null) {
                return null;
            }
            /*
             * Determine scheme from protocol (in the form protocol/majorVersion.minorVersion) and isSecure information
             */
            scheme = new StringBuilder(protocol.substring(0, protocol.indexOf('/')).toLowerCase(Locale.ENGLISH)).append(
                is_secure ? "s" : "").toString();
        }
        return scheme;
    }

    public void setServerName(final String server_name) {
        this.server_name = server_name;
    }

    public String getServerName() {
        return server_name;
    }

    public void setServerPort(final int server_port) {
        this.server_port = server_port;
    }

    public int getServerPort() {
        return server_port;
    }

    public void setSecure(final boolean is_secure) {
        this.is_secure = is_secure;
    }

    public boolean isSecure() {
        return is_secure;
    }

    protected String makeString(final String[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        return values[0];
    }

    protected Enumeration<?> makeEnumeration(final Object obj) {
        final Class<?> type = obj.getClass();
        if (!type.isArray()) {
            throw new IllegalArgumentException(obj.getClass().toString());
        }
        return (new Enumeration<Object>() {

            int size = Array.getLength(obj);

            int cursor;

            public boolean hasMoreElements() {
                return (cursor < size);
            }

            public Object nextElement() {
                return Array.get(obj, cursor++);
            }
        });
    }

    public int getRemotePort() {
        return 0;
    }

    public String getLocalName() {
        return null;
    }

    public String getLocalAddr() {
        return null;
    }

    public int getLocalPort() {
        return 0;
    }

    /**
     * IteratorEnumeration
     * 
     * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
     */
    private static class IteratorEnumeration implements Enumeration<Object> {

        private final Iterator<?> iter;

        public IteratorEnumeration(final Iterator<?> iter) {
            this.iter = iter;
        }

        public boolean hasMoreElements() {
            return iter.hasNext();
        }

        public Object nextElement() {
            return iter.next();
        }

    }
}
