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

package com.openexchange.tools.servlet.http;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;

/**
 * HttpSessionWrapper
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class HttpSessionWrapper implements HttpSession {

    private final Map<String, Object> attributes;

    private final Map<String, Object> values;

    private long creationTime = new Date(0).getTime();

    private long lastAccessedTime = new Date(0).getTime();

    private final String id;

    private int maxInactiveIntervall = -1;

    private ServletContext servletContext;

    private HttpSessionContext sessionContext;

    /**
     * Indicates if the client does not yet know about the session or if the client chooses not to join the session
     */
    private boolean newSession = true;

    public HttpSessionWrapper(final String id) {
        super();
        attributes = new HashMap<String, Object>();
        values = new HashMap<String, Object>();
        creationTime = lastAccessedTime = System.currentTimeMillis();
        this.id = id;
    }

    public Object getAttribute(final String attributeName) {
        lastAccessedTime = System.currentTimeMillis();
        return attributes.get(attributeName);
    }

    public Enumeration<?> getAttributeNames() {
        lastAccessedTime = System.currentTimeMillis();
        return new IteratorEnumeration(attributes.keySet().iterator());
    }

    public long getCreationTime() {
        lastAccessedTime = System.currentTimeMillis();
        return creationTime;
    }

    public String getId() {
        lastAccessedTime = System.currentTimeMillis();
        return id;
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public int getMaxInactiveInterval() {
        lastAccessedTime = System.currentTimeMillis();
        return maxInactiveIntervall;
    }

    public ServletContext getServletContext() {
        lastAccessedTime = System.currentTimeMillis();
        return servletContext;
    }

    public void setServletContext(final ServletContext context) {
        servletContext = context;
    }

    public HttpSessionContext getSessionContext() {
        lastAccessedTime = System.currentTimeMillis();
        return sessionContext;
    }

    public Object getValue(final String valueName) {
        lastAccessedTime = System.currentTimeMillis();
        return values.get(valueName);
    }

    public String[] getValueNames() {
        lastAccessedTime = System.currentTimeMillis();
        final int size = values.size();
        final String[] valueNames = new String[size];
        final Iterator<String> iter = values.keySet().iterator();
        for (int i = 0; i < size; i++) {
            valueNames[i] = iter.next();
        }
        return valueNames;
    }

    public void invalidate() {
        lastAccessedTime = System.currentTimeMillis();
        /*
         * Remove attributes
         */
        final List<String> toRemove = new ArrayList<String>(attributes.keySet());
        for (final String attributeName : toRemove) {
            removeAttribute(attributeName);
        }
        /*
         * Remove values
         */
        toRemove.clear();
        toRemove.addAll(values.keySet());
        for (final String valueName : toRemove) {
            removeValue(valueName);
        }
        servletContext = null;
        sessionContext = null;
    }

    public boolean isNew() {
        lastAccessedTime = System.currentTimeMillis();
        return newSession;
    }

    public void setNew(final boolean newSession) {
        this.newSession = newSession;
    }

    public void putValue(final String valueName, final Object value) {
        lastAccessedTime = System.currentTimeMillis();
        values.put(valueName, value);
        if (value instanceof HttpSessionBindingListener) {
            final HttpSessionBindingListener listener = (HttpSessionBindingListener) value;
            listener.valueBound(new HttpSessionBindingEvent(this, valueName));
        }
    }

    public void removeAttribute(final String attributeName) {
        lastAccessedTime = System.currentTimeMillis();
        final Object removedObj = attributes.remove(attributeName);
        if (removedObj instanceof HttpSessionBindingListener) {
            final HttpSessionBindingListener listener = (HttpSessionBindingListener) removedObj;
            listener.valueUnbound(new HttpSessionBindingEvent(this, attributeName));
        }
    }

    public void removeValue(final String valueName) {
        lastAccessedTime = System.currentTimeMillis();
        final Object removedObj = values.remove(valueName);
        if (removedObj instanceof HttpSessionBindingListener) {
            final HttpSessionBindingListener listener = (HttpSessionBindingListener) removedObj;
            listener.valueUnbound(new HttpSessionBindingEvent(this, valueName));
        }
    }

    public void setAttribute(final String attributeName, final Object attributeValue) {
        lastAccessedTime = System.currentTimeMillis();
        if (attributeValue != null) {
            attributes.put(attributeName, attributeValue);
        }
        if (attributeValue instanceof HttpSessionBindingListener) {
            final HttpSessionBindingListener listener = (HttpSessionBindingListener) attributeValue;
            listener.valueBound(new HttpSessionBindingEvent(this, attributeName));
        }
    }

    public void setMaxInactiveInterval(final int maxInactiveIntervall) {
        lastAccessedTime = System.currentTimeMillis();
        this.maxInactiveIntervall = maxInactiveIntervall;
    }

    private static class IteratorEnumeration implements Enumeration<Object> {

        private final Iterator<?> iter;

        private IteratorEnumeration(final Iterator<?> iter) {
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
