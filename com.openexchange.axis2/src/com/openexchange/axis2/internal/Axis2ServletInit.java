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

package com.openexchange.axis2.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletException;

import org.apache.axis2.transport.http.AxisServlet;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import com.openexchange.axis2.exeptions.OXAxis2Exception;
import com.openexchange.axis2.services.Axis2ServletServiceRegistry;
import com.openexchange.config.ConfigurationService;
import com.openexchange.server.Initialization;

/**
 * {@link Axis2ServletInit}
 * 
 * @author <a href="mailto:choeger@open-xchange.com">Carsten Hoeger</a>
 * 
 */
public class Axis2ServletInit implements Initialization {

    /*
     * redirect all requests with axis2 to the axis servlet
     */
    private static final String SC_AXIS2_SRVLT_ALIAS = "servlet/axis2/*";

    /*
     *  path to the axis2.xml configuration file
     */
    private static final String AXIS2_XML_PATH_PROPERTY = "axis2.xml.path";
    
    /*
     * directory where to find the modules and services subdirectory
     */
    private static final String AXIS2_REPOSITORY_PATH_PROPERTY = "axis2.repository.path";

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(Axis2ServletInit.class);

    private static final Axis2ServletInit instance = new Axis2ServletInit();

    public static Axis2ServletInit getInstance() {
        return instance;
    }

    private final AtomicBoolean started;

    /**
     * Initializes a new {@link Axis2ServletInit}
     * @throws MalformedURLException 
     */
    private Axis2ServletInit() {
        super();
        started = new AtomicBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.server.Initialization#start()
     */
    public void start() throws OXAxis2Exception {
        if (!started.compareAndSet(false, true)) {
            LOG.error("Axis2Servlet already started.");
            return;
        }

        final HttpService httpService = Axis2ServletServiceRegistry.getServiceRegistry().getService(HttpService.class);
        if (httpService == null) {
            LOG.error("HTTP service is null. Axis2 servlet cannot be registered");
            return;
        }
        try {
            /*
             * Register axis2 servlet
             */
            final ConfigurationService config = Axis2ServletServiceRegistry.getServiceRegistry().getService(ConfigurationService.class);

            final String xml_path_property = config.getProperty(AXIS2_XML_PATH_PROPERTY);
            if( xml_path_property == null ) {
                throw new OXAxis2Exception(OXAxis2Exception.Code.PROPERTY_ERROR, AXIS2_XML_PATH_PROPERTY + " not set");
            }
            final String repository_path_property = config.getProperty(AXIS2_REPOSITORY_PATH_PROPERTY);
            if( repository_path_property == null ) {
                throw new OXAxis2Exception(OXAxis2Exception.Code.PROPERTY_ERROR, AXIS2_REPOSITORY_PATH_PROPERTY + " not set");
            }

            java.util.Hashtable<String, String> servletConf = new java.util.Hashtable<String, String>();

            servletConf.put(AXIS2_XML_PATH_PROPERTY, xml_path_property);
            servletConf.put(AXIS2_REPOSITORY_PATH_PROPERTY, repository_path_property);
            
            httpService.registerServlet(SC_AXIS2_SRVLT_ALIAS, new AxisServlet(), servletConf, null);
        } catch (final ServletException e) {
            throw new OXAxis2Exception(OXAxis2Exception.Code.SERVLET_REGISTRATION_FAILED, e, e
                    .getLocalizedMessage());
        } catch (final NamespaceException e) {
            throw new OXAxis2Exception(OXAxis2Exception.Code.SERVLET_REGISTRATION_FAILED, e, e
                    .getLocalizedMessage());
        }/* catch (MalformedURLException e) {
            throw new OXAxis2Exception(OXAxis2Exception.Code.SERVLET_REGISTRATION_FAILED, e, e
                    .getLocalizedMessage());
        }*/
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.server.Initialization#stop()
     */
    public void stop() {
        if (!started.compareAndSet(true, false)) {
            LOG.error("Axis2 servlet has not been started.");
            return;
        }
        final HttpService httpService = Axis2ServletServiceRegistry.getServiceRegistry().getService(HttpService.class);
        if (httpService == null) {
            LOG.error("HTTP service is null. Axis2 servlet cannot be unregistered");
        } else {
            /*
             * Unregister mail filter servlet
             */
            httpService.unregister(SC_AXIS2_SRVLT_ALIAS);
        }
    }

    /**
     * Checks if {@link SessiondInit} is started
     * 
     * @return <code>true</code> if {@link SessiondInit} is started; otherwise
     *         <code>false</code>
     */
    public boolean isStarted() {
        return started.get();
    }
}
