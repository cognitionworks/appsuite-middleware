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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.tools.servlet.http.osgi;

import java.util.Dictionary;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import com.openexchange.tools.servlet.http.HttpServletManager;

/**
 * {@link HttpServiceImpl} - The HTTP Service allows other bundles in the OSGi environment to dynamically register resources and servlets
 * into the URI namespace of HTTP Service. A bundle may later unregister its resources or servlets.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class HttpServiceImpl implements HttpService {

    /**
     * Default constructor.
     */
    public HttpServiceImpl() {
        super();
    }

    public HttpContext createDefaultHttpContext() {
        return new HttpContextImpl();
    }

    public void registerResources(final String alias, final String name, final HttpContext context) throws NamespaceException {
        final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(HttpServiceImpl.class);
        if (log.isTraceEnabled()) {
            log.trace("HttpServiceImpl.registerResources() not implemented");
        }
    }

    public void registerServlet(final String alias, final Servlet servlet, @SuppressWarnings("unchecked") final Dictionary initparams, final HttpContext context) throws ServletException {
        try {
            @SuppressWarnings("unchecked") final Dictionary<String, String> dic = initparams;
            HttpServletManager.registerServlet(alias, (HttpServlet) servlet, dic);
        } catch (final ClassCastException e) {
            final ServletException se = new ServletException("Only http servlets are supported", e);
            se.initCause(e);
            throw se;
        }
    }

    public void unregister(final String alias) {
        HttpServletManager.unregisterServlet(alias);
    }

}
