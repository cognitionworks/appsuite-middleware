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

package com.openexchange.tools.servlet.http;

import java.lang.reflect.Constructor;
import javax.servlet.http.HttpServlet;
import com.openexchange.tools.servlet.ServletConfigLoader;

/**
 * {@link SingletonServletQueue} - A singleton servlet queue.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SingletonServletQueue implements ServletQueue {

    private static final Object[] INIT_ARGS = new Object[] {};

    private final Constructor<?> servletConstructor;

    private final String servletPath;

    private final HttpServlet singleton;

    /**
     * Initializes a new {@link SingletonServletQueue}.
     * 
     * @param singleton The singleton HTTP servlet
     * @param servletConstructor The servlet constructor to create new servlet instances on demand
     * @param servletPath The servlet path
     */
    public SingletonServletQueue(final HttpServlet singleton, final Constructor<?> servletConstructor, final String servletPath) {
        super();
        this.singleton = singleton;
        this.servletConstructor = servletConstructor;
        this.servletPath = servletPath;
    }

    public HttpServlet createServletInstance(final String servletKey) {
        if (servletConstructor == null) {
            return null;
        }
        try {
            final HttpServlet servletInstance = (HttpServlet) servletConstructor.newInstance(INIT_ARGS);
            servletInstance.init(ServletConfigLoader.getDefaultInstance().getConfig(
                servletInstance.getClass().getCanonicalName(),
                servletKey));
            return servletInstance;
        } catch (final Throwable t) {
            org.apache.commons.logging.LogFactory.getLog(FiFoServletQueue.class).error(t.getMessage(), t);
        }
        return null;
    }

    public HttpServlet dequeue() {
        throw new UnsupportedOperationException("SingletonServletQueue.dequeue()");
    }

    public void enqueue(final HttpServlet servlet) {
        // Nothing to do
    }

    public HttpServlet get() {
        return singleton;
    }

    public String getServletPath() {
        return servletPath;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean isFull() {
        return true;
    }

    public boolean isSingleton() {
        return true;
    }

    public int size() {
        return 1;
    }

}
