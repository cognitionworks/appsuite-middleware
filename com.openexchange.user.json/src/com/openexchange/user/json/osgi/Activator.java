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

package com.openexchange.user.json.osgi;

import com.openexchange.ajax.requesthandler.osgiservice.AJAXModuleActivator;
import com.openexchange.api2.ContactInterfaceFactory;
import com.openexchange.groupware.contact.ContactInterfaceDiscoveryService;
import com.openexchange.server.osgiservice.RegistryServiceTrackerCustomizer;
import com.openexchange.user.UserService;
import com.openexchange.user.json.Constants;
import com.openexchange.user.json.actions.UserActionFactory;
import com.openexchange.user.json.services.ServiceRegistry;

/**
 * {@link Activator} - Activator for user component.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class Activator extends AJAXModuleActivator {

    /**
     * Initializes a new {@link Activator}.
     */
    public Activator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return EMPTY_CLASSES;
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            /*
             * Register user multiple service
             */
            registerModule(UserActionFactory.getInstance(), Constants.MODULE);
            /*
             * User service tracker
             */
            track(UserService.class, new RegistryServiceTrackerCustomizer<UserService>(
                context,
                ServiceRegistry.getInstance(),
                UserService.class));
            /*
             * Contact interface factory tracker
             */
            track(ContactInterfaceFactory.class, new RegistryServiceTrackerCustomizer<ContactInterfaceFactory>(
                context,
                ServiceRegistry.getInstance(),
                ContactInterfaceFactory.class));
            track(ContactInterfaceDiscoveryService.class, new RegistryServiceTrackerCustomizer<ContactInterfaceDiscoveryService>(
                context,
                ServiceRegistry.getInstance(),
                ContactInterfaceDiscoveryService.class));
        } catch (final Exception e) {
            final org.apache.commons.logging.Log LOG = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(Activator.class));
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

}
