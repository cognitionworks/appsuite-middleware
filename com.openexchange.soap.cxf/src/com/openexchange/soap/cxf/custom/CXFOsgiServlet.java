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

package com.openexchange.soap.cxf.custom;

import java.lang.reflect.Field;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.cxf.transport.servlet.ServletController;
import org.apache.cxf.transport.servlet.servicelist.ServiceListGeneratorServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link CXFOsgiServlet}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.10.0
 */
public class CXFOsgiServlet extends CXFNonSpringServlet {

    private static final Logger LOG = LoggerFactory.getLogger(CXFOsgiServlet.class);

    /** serialVersionUID */
    private static final long serialVersionUID = 2941782846712141279L;

    @Override
    protected void finalizeServletInit(ServletConfig servletConfig) throws ServletException {
        // do not call super.finalizeServletInit(...) as reading files from WEB-INF folder is not possible
    }

    @Override
    protected ServletController createServletController(ServletConfig servletConfig) {
        Field f = null;
        try {
            f = CXFNonSpringServlet.class.getDeclaredField("destinationRegistry");
            f.setAccessible(true);
            DestinationRegistry registry = (DestinationRegistry) f.get(this);

            HttpServlet serviceListGeneratorServlet = new ServiceListGeneratorServlet(registry, bus);
            return new CustomServletController(registry, servletConfig, serviceListGeneratorServlet);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            LOG.error("Unable to create custom ServletController. Falling back to the default one.", e);
            return super.createServletController(servletConfig);
        } finally {
            if (f != null) {
                f.setAccessible(false);
            }
        }
    }
}
