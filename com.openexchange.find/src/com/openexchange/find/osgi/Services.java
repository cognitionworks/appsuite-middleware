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

package com.openexchange.find.osgi;

import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;

/**
 * 
 * {@link Services}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class Services {

    private static final AtomicReference<ServiceLookup> LOOKUP = new AtomicReference<ServiceLookup>();

    /**
     * Initialises a new {@link Services}.
     */
    private Services() {
        super();
    }

    /**
     * Retrieves the {@link ConfigurationService}
     * 
     * @return the {@link ConfigurationService}
     * @throws OXException
     */
    public static ConfigurationService getConfigurationService() throws OXException {
        return requireService(ConfigurationService.class);
    }

    /**
     * Sets the service lookup instance
     * 
     * @param lookup the {@link ServiceLookup} instance
     */
    public static void setServiceLookup(ServiceLookup lookup) {
        LOOKUP.set(lookup);
    }

    /**
     * Looks up a required service
     * 
     * @param clazz The class of the service
     * @return The service
     * @throws OXException if the service is absent
     */
    public static <T> T requireService(Class<T> clazz) throws OXException {
        T service = getServiceLookup().getService(clazz);
        if (null == service) {
            throw ServiceExceptionCode.absentService(clazz);
        }
        return service;
    }

    /**
     * Gets the {@link ServiceLookup} instance
     * 
     * @return the {@link ServiceLookup} instance
     */
    private static ServiceLookup getServiceLookup() {
        ServiceLookup serviceLookup = LOOKUP.get();
        if (serviceLookup == null) {
            throw new IllegalStateException("ServiceLookup was null!");
        }

        return serviceLookup;
    }
}
