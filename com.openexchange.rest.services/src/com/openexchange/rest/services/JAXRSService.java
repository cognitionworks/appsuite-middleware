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

package com.openexchange.rest.services;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.server.ServiceLookup;

/**
 * A convenience superclass for JAX-RS based REST services. Provides
 * access to all potential necessary context objects like {@link HttpServletRequest}
 * or {@link Application} and lets you work with a plain {@link AJAXRequestData}
 * instance, if wanted.
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.6.1
 */
public abstract class JAXRSService {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(JAXRSService.class);
    }

    @Context
    protected Application application;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected Request request;

    @Context
    protected HttpHeaders httpHeaders;

    @Context
    protected SecurityContext securityContext;

    @Context
    protected javax.ws.rs.ext.Providers providers;

    @Context
    protected HttpServletRequest servletRequest;

    @Context
    protected HttpServletResponse servletResponse;

    private final ServiceLookup services;
    private final BundleContext bundleContext;

    /**
     * Initializes a new {@link JAXRSService}.
     *
     * @param services The OSGi services to use
     */
    protected JAXRSService(ServiceLookup services) {
        super();
        if (services == null) {
            throw new IllegalArgumentException("ServiceLookup must not be null!");
        }

        this.services = services;
        this.bundleContext = null;
    }

    /**
     * Initializes a new {@link JAXRSService}.
     *
     * @param bundleContext The bundle context to use
     */
    protected JAXRSService(BundleContext bundleContext) {
        super();
        if (bundleContext == null) {
            throw new IllegalArgumentException("BundleContext must not be null!");
        }

        this.services = null;
        this.bundleContext = bundleContext;
    }

    /**
     * Parses the HTTP request headers and body into an {@link AJAXRequestData} instance.
     *
     * @return The request data
     * @throws BadRequestException if parsing fails
     */
    protected AJAXRequestData getAJAXRequestData() {
            AJAXRequestData ajaxRequestData = RequestTool.getAJAXRequestData(
                httpHeaders,
                uriInfo,
                servletRequest,
                servletResponse);

            return ajaxRequestData;
    }

    /**
     * Gets a service either via (at initialization time) provided {@link ServiceLookup} or {@link BundleContext}.
     *
     * @param clazz The class of the service interface
     * @return The service instance
     * @throws ServiceUnavailableException If the service is not available
     */
    protected <T> T getService(Class<T> clazz) {
        T service = optService(clazz);
        if (service != null) {
            return service;
        }
        ServiceUnavailableException e = new ServiceUnavailableException();
        LoggerHolder.LOG.error("Service '{}' is not available.", clazz.getName(), e);
        throw e;
    }

    /**
     * Gets a service either via (at initialization time) provided {@link ServiceLookup} or {@link BundleContext}.
     *
     * @param clazz The class of the service interface
     * @return The service instance or <code>null</code> if the service is not available
     */
    protected <T> T optService(Class<T> clazz) {
        T service = null;
        if (services != null) {
            service = services.getService(clazz);
            if (service == null) {
                service = services.getOptionalService(clazz);
            }
        }

        if (service == null && bundleContext != null) {
            ServiceReference<T> ref = bundleContext.getServiceReference(clazz);
            if (ref != null) {
                service = bundleContext.getService(ref);
            }
        }

        return service;
    }

}
