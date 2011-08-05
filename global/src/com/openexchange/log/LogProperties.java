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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link LogProperties} - Provides thread-local log properties.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class LogProperties {

    /**
     * Initializes a new {@link LogProperties}.
     */
    private LogProperties() {
        super();
    }

    /**
     * The copy-on-write list containing sorted property names.
     */
    private static final List<String> PROPERTY_NAMES = new CopyOnWriteArrayList<String>();

    /**
     * Sets the configured log property names.
     *
     * @param propertyNames The log property names
     */
    public static void configuredProperties(final Collection<String> propertyNames) {
        PROPERTY_NAMES.clear();
        PROPERTY_NAMES.addAll(new TreeSet<String>(propertyNames));
    }

    /**
     * Gets the list containing sorted property names.
     *
     * @return The list containing sorted property names
     */
    public static List<String> getPropertyNames() {
        return PROPERTY_NAMES;
    }

    /**
     * The {@link ThreadLocal} variable.
     */
    private static final ConcurrentMap<Thread, Map<String, Object>> THREAD_LOCAL = new ConcurrentHashMap<Thread, Map<String,Object>>();

    /**
     * Gets the thread-local log properties.
     *
     * @return The log properties
     */
    public static Map<String, Object> optLogProperties() {
        return THREAD_LOCAL.get(Thread.currentThread());
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
     */
    public static Map<String, Object> getLogProperties() {
        final Thread thread = Thread.currentThread();
        Map<String, Object> map = THREAD_LOCAL.get(thread);
        if (null == map) {
            final Map<String, Object> newmap = new HashMap<String, Object>();
            map = THREAD_LOCAL.put(thread, newmap);
            if (null == map) {
                map = newmap;
            }
        }
        return map;
    }

    /**
     * Puts specified log property. A <code>null</code> value removes the property.
     *
     * @param name The property name
     * @param value The property value
     */
    public static void putLogProperty(final String name, final Object value) {
        if (null == value) {
            getLogProperties().remove(name);
        } else {
            getLogProperties().put(name, value);
        }
    }

}
