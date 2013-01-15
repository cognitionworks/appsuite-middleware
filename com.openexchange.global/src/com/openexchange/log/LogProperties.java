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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.log;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.openexchange.java.StringAllocator;

/**
 * {@link LogProperties} - Provides thread-local log properties.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class LogProperties {

    /**
     * Enumeration of log properties' names.
     */
    public static enum Name {
        /**
         * com.openexchange.ajax.requestNumber
         */
        AJAX_REQUEST_NUMBER("com.openexchange.ajax.requestNumber"),
        /**
         * com.openexchange.ajax.action
         */
        AJAX_ACTION("com.openexchange.ajax.action"),
        /**
         * com.openexchange.ajpv13.requestURI
         */
        AJP_REQUEST_URI("com.openexchange.ajpv13.requestURI"),
        /**
         * com.openexchange.ajpv13.servletPath
         */
        AJP_SERVLET_PATH("com.openexchange.ajpv13.servletPath"),
        /**
         * com.openexchange.ajpv13.pathInfo
         */
        AJP_PATH_INFO("com.openexchange.ajpv13.pathInfo"),
        /**
         * com.openexchange.ajpv13.requestIp
         */
        AJP_REQUEST_IP("com.openexchange.ajpv13.requestIp"),
        /**
         * com.openexchange.ajpv13.requestId
         */
        AJP_REQUEST_ID("com.openexchange.ajpv13.requestId"),
        /**
         * com.openexchange.ajpv13.serverName
         */
        AJP_SERVER_NAME("com.openexchange.ajpv13.serverName"),
        /**
         * com.openexchange.ajpv13.threadName
         */
        AJP_THREAD_NAME("com.openexchange.ajpv13.threadName"),
        /**
         * com.openexchange.ajpv13.remotePort
         */
        AJP_REMOTE_PORT("com.openexchange.ajpv13.remotePort"),
        /**
         * com.openexchange.ajpv13.remoteAddres
         */
        AJP_REMOTE_ADDRESS("com.openexchange.ajpv13.remoteAddress"),
        /**
         * com.openexchange.ajp13.httpSession
         */
        AJP_HTTP_SESSION("com.openexchange.ajp13.httpSession"),
        /**
         * com.openexchange.session.sessionId
         */
        SESSION_SESSION_ID("com.openexchange.session.sessionId"),
        /**
         * com.openexchange.session.userId
         */
        SESSION_USER_ID("com.openexchange.session.userId"),
        /**
         * com.openexchange.session.contextId
         */
        SESSION_CONTEXT_ID("com.openexchange.session.contextId"),
        /**
         * com.openexchange.session.clientId
         */
        SESSION_CLIENT_ID("com.openexchange.session.clientId"),
        /**
         * com.openexchange.session.session
         */
        SESSION_SESSION("com.openexchange.session.session"),
        /**
         * com.openexchange.grizzly.requestURI
         */
        GRIZZLY_REQUEST_URI("com.openexchange.grizzly.requestURI"),
        /**
         * com.openexchange.grizzly.servletPath
         */
        GRIZZLY_SERVLET_PATH("com.openexchange.grizzly.servletPath"),
        /**
         * com.openexchange.grizzly.pathInfo
         */
        GRIZZLY_PATH_INFO("com.openexchange.grizzly.pathInfo"),
        /**
         * com.openexchange.grizzly.requestIp
         */
        GRIZZLY_REQUEST_IP("com.openexchange.grizzly.requestIp"),
        /**
         * com.openexchange.grizzly.serverName
         */
        GRIZZLY_SERVER_NAME("com.openexchange.grizzly.serverName"),
        /**
         * com.openexchange.grizzly.threadName
         */
        GRIZZLY_THREAD_NAME("com.openexchange.grizzly.threadName"),
        /**
         * com.openexchange.grizzly.remotePort
         */
        GRIZZLY_REMOTE_PORT("com.openexchange.grizzly.remotePort"),
        /**
         * com.openexchange.grizzly.remoteAddres
         */
        GRIZZLY_REMOTE_ADDRESS("com.openexchange.grizzly.remoteAddress"),
        /**
         * com.openexchange.http.grizzly.session
         */
        GRIZZLY_HTTP_SESSION("com.openexchange.http.grizzly.session"),
        /**
         * javax.servlet.servletPath
         */
        SERVLET_SERVLET_PATH("javax.servlet.servletPath"),
        /**
         * javax.servlet.pathInfo
         */
        SERVLET_PATH_INFO("javax.servlet.pathInfo"),
        /**
         * javax.servlet.queryString
         */
        SERVLET_QUERY_STRING("javax.servlet.queryString"),
        /**
         * com.openexchange.file.storage.accountId
         */
        FILE_STORAGE_ACCOUNT_ID("com.openexchange.file.storage.accountId"),
        /**
         * com.openexchange.file.storage.configuration
         */
        FILE_STORAGE_CONFIGURATION("com.openexchange.file.storage.configuration"),
        /**
         * com.openexchange.file.storage.serviceId
         */
        FILE_STORAGE_SERVICE_ID("com.openexchange.file.storage.serviceId"),
        /**
         * com.openexchange.mail.host
         */
        MAIL_HOST("com.openexchange.mail.host"),
        /**
         * com.openexchange.mail.fullName
         */
        MAIL_FULL_NAME("com.openexchange.mail.fullName"),
        /**
         * com.openexchange.mail.mailId
         */
        MAIL_MAIL_ID("com.openexchange.mail.mailId"),
        /**
         * com.openexchange.mail.accountId
         */
        MAIL_ACCOUNT_ID("com.openexchange.mail.accountId"),
        /**
         * com.openexchange.mail.login
         */
        MAIL_LOGIN("com.openexchange.mail.login"),
        /**
         * com.openexchange.database.schema
         */
        DATABASE_SCHEMA("com.openexchange.database.schema"),

        ;

        private final String name;
        private Name(final String name) {
            this.name = name;
        }

        /**
         * Gets the name
         *
         * @return The name
         */
        public String getName() {
            return name;
        }

        private static final Map<String, Name> STRING2NAME;
        static {
            final Name[] values = Name.values();
            final Map<String, Name> m = new HashMap<String, Name>(values.length);
            for (final Name name : values) {
                m.put(name.getName(), name);
            }
            STRING2NAME = m;
        }

        /**
         * Gets the associated {@code Name} enum.
         *
         * @param sName The name string
         * @return The {@code Name} enum or <code>null</code>
         */
        public static Name nameFor(final String sName) {
            return null == sName ? null : STRING2NAME.get(sName);
        }
    }

    /**
     * Initializes a new {@link LogProperties}.
     */
    private LogProperties() {
        super();
    }

    /**
     * The copy-on-write list containing sorted property names.
     */
    private static final List<LogPropertyName> PROPERTY_NAMES = new CopyOnWriteArrayList<LogPropertyName>();

    /**
     * Sets the configured log property names.
     *
     * @param propertyNames The log property names
     */
    public static void configuredProperties(final Collection<LogPropertyName> propertyNames) {
        PROPERTY_NAMES.clear();
        if (null != propertyNames && !propertyNames.isEmpty()) {
            PROPERTY_NAMES.addAll(new TreeSet<LogPropertyName>(propertyNames));
        }
    }

    /**
     * Gets the list containing sorted property names.
     *
     * @return The list containing sorted property names
     */
    public static List<LogPropertyName> getPropertyNames() {
        return PROPERTY_NAMES;
    }

    /**
     * Checks if thread-local log properties are enabled.
     *
     * @return <code>true</code> if thread-local log properties are enabled; otherwise <code>false</code>
     */
    public static boolean isEnabled() {
        return !PROPERTY_NAMES.isEmpty();
    }

    /**
     * The {@link ThreadLocal} variable.
     */
    private static final ConcurrentMap<Thread, Props> THREAD_LOCAL = new ConcurrentHashMap<Thread, Props>();

    /**
     * Gets the thread-local log properties.
     *
     * @return The log properties or <code>null</code>
     * @see #isEnabled()
     */
    public static Props optLogProperties() {
        Props props = THREAD_LOCAL.get(Thread.currentThread());
        if (props == null) {
        	return null;
        }
        return props;
    }

    /**
     * Gets the thread-local log properties for specified thread.
     *
     * @param thread The thread
     * @return The log properties or <code>null</code>
     * @see #isEnabled()
     */
    public static Props optLogProperties(final Thread thread) {
        if (null == thread) {
            return null;
        }
        Props props = THREAD_LOCAL.get(thread);
        if (props == null) {
            return null;
        }
        return props;
    }

    /**
     * Removes the log properties for calling thread.
     */
    public static void removeLogProperties() {
        THREAD_LOCAL.remove(Thread.currentThread());
    }

    /**
     * Gets the thread-local log properties.
     *
     * @return The log properties
     * @see #isEnabled()
     */
    public static Props getLogProperties() {
        final Thread thread = Thread.currentThread();
        Props props = THREAD_LOCAL.get(thread);
        if (null == props) {
            final Props newprops = new Props();
            props = THREAD_LOCAL.putIfAbsent(thread, newprops);
            if (null == props) {
                props = newprops;
            }
        }
        return props;
    }

    /**
     * Clones the thread-local log properties.
     *
     * @param other The other thread
     */
    public static void cloneLogProperties(final Thread other) {
        final Thread thread = Thread.currentThread();
        final Props props = THREAD_LOCAL.get(thread);
        if (null == props) {
            return;
        }
        THREAD_LOCAL.put(other, new Props(props));
    }

    /**
     * Gets the thread-local log property associated with specified name.
     *
     * @param name The property name
     * @return The log property or <code>null</code> if absent
     */
    public static <V> V getLogProperty(final LogProperties.Name name) {
        final Thread thread = Thread.currentThread();
        final Props props = THREAD_LOCAL.get(thread);
        return null == props ? null : props.<V>get(name);
    }

    /**
     * Puts specified log property. A <code>null</code> value removes the property.
     *
     * @param name The property name
     * @param value The property value
     * @see #isEnabled()
     */
    public static void putLogProperty(final LogProperties.Name name, final Object value) {
        if (null == value) {
            getLogProperties().remove(name);
        } else {
            getLogProperties().put(name, value);
        }
    }

    /**
     * Get the thread local LogProperties and pretty-prints them into a Sting.
     * The String will contain one ore more lines formatted like:
     * <pre>
     * "propertyName1=propertyValue1"
     * "propertyName2=propertyValue2"
     * "propertyName3=propertyValue3"
     * </pre>
     * where the properties are sorted alphabetically.
     */
    public static String getAndPrettyPrint() {
        return getAndPrettyPrint(Collections.<LogProperties.Name> emptySet());
    }

    /**
     * Get the thread local LogProperties and pretty-prints them into a Sting.
     * The String will contain one ore more lines formatted like:
     * <pre>
     * "propertyName1=propertyValue1"
     * "propertyName2=propertyValue2"
     * "propertyName3=propertyValue3"
     * </pre>
     * where the properties are sorted alphabetically.
     * 
     * @param nonMatching The property name to ignore
     */
    public static String getAndPrettyPrint(final LogProperties.Name nonMatching) {
        return getAndPrettyPrint(EnumSet.<LogProperties.Name> of(nonMatching));
    }

    /**
     * Get the thread local LogProperties and pretty-prints them into a Sting.
     * The String will contain one ore more lines formatted like:
     * <pre>
     * "propertyName1=propertyValue1"
     * "propertyName2=propertyValue2"
     * "propertyName3=propertyValue3"
     * </pre>
     * where the properties are sorted alphabetically.
     * 
     * @param nonMatching The property names to ignore
     */
    public static String getAndPrettyPrint(final LogProperties.Name... nonMatching) {
        return getAndPrettyPrint(EnumSet.<LogProperties.Name> copyOf(Arrays.asList(nonMatching)));
    }

    /**
     * Get the thread local LogProperties and pretty-prints them into a Sting.
     * The String will contain one ore more lines formatted like:
     * <pre>
     * "propertyName1=propertyValue1"
     * "propertyName2=propertyValue2"
     * "propertyName3=propertyValue3"
     * </pre>
     * where the properties are sorted alphabetically.
     * 
     * @param nonMatching The property names to ignore
     */
    public static String getAndPrettyPrint(final Collection<LogProperties.Name> nonMatching) {
        String logString = "";
        final Props logProperties = getLogProperties();
        // If we have additional log properties from the ThreadLocal add it to the logBuilder
        if (logProperties != null) {
            StringAllocator logBuilder = new StringAllocator(1024);
            Map<LogProperties.Name, Object> propertyMap = logProperties.getMap();
            // Sort the properties for readability
            Map<String, String> sorted = new TreeMap<String, String>();
            for (Entry<LogProperties.Name, Object> propertyEntry : propertyMap.entrySet()) {
                LogProperties.Name propertyName = propertyEntry.getKey();
                if (null == nonMatching || !nonMatching.contains(propertyName)) {
                    Object value = propertyEntry.getValue();
                    if (null != value) {
                        sorted.put(propertyName.getName(), value.toString());
                    }
                }
            }
            // And add them to the logBuilder
            for (Map.Entry<String, String> propertyEntry : sorted.entrySet()) {
                logBuilder.append(propertyEntry.getKey()).append('=').append(propertyEntry.getValue()).append('\n');
            }
            logString=logBuilder.toString();
        }
        return logString;
    }

}
