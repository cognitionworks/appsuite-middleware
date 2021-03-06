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

package com.openexchange.net.osgi;

import java.util.concurrent.atomic.AtomicReference;
import org.osgi.framework.BundleActivator;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.ForcedReloadable;
import com.openexchange.monitoring.sockets.SocketLoggerRegistryService;
import com.openexchange.net.HostList;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.server.ServiceLookup;

/**
 * {@link NetSSLActivator} - The {@link BundleActivator activator} for monitoring bundle.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.3
 */
public final class NetSSLActivator extends HousekeepingActivator {

    private static final AtomicReference<ServiceLookup> REF = new AtomicReference<>();

    /**
     * Sets the service lookup.
     *
     * @param serviceLookup The service lookup or <code>null</code>
     */
    private static void setServiceLookup(final ServiceLookup serviceLookup) {
        REF.set(serviceLookup);
    }

    /**
     * Gets the service lookup.
     *
     * @return The service lookup or <code>null</code>
     */
    public static ServiceLookup getServiceLookup() {
        return REF.get();
    }

    /**
     * Initializes a new {@link NetSSLActivator}
     */
    public NetSSLActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return EMPTY_CLASSES;
    }

    @Override
    protected synchronized void startBundle() throws Exception {
        setServiceLookup(this);
        trackService(SocketLoggerRegistryService.class);
        openTrackers();

        registerService(ForcedReloadable.class, new ForcedReloadable() {

            @Override
            public void reloadConfiguration(ConfigurationService configService) {
                HostList.flushCache();
            }
        });
    }

    @Override
    protected synchronized void stopBundle() throws Exception {
        super.stopBundle();
        setServiceLookup(null);
    }
}
