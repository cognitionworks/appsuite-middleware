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

package com.openexchange.health.impl.osgi;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import com.hazelcast.core.HazelcastInstance;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.database.DatabaseService;
import com.openexchange.health.MWHealthCheck;
import com.openexchange.health.MWHealthCheckService;
import com.openexchange.health.impl.HealthCheckResponseProviderImpl;
import com.openexchange.health.impl.MWHealthCheckServiceImpl;
import com.openexchange.health.impl.checks.AllPluginsLoadedCheck;
import com.openexchange.health.impl.checks.ConfigDBCheck;
import com.openexchange.health.impl.checks.HazelcastCheck;
import com.openexchange.health.impl.checks.JVMHeapCheck;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.pluginsloaded.PluginsLoadedService;
import com.openexchange.threadpool.ThreadPoolService;

/**
 * {@link MWHealthCheckActivator}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.1
 */
public class MWHealthCheckActivator extends HousekeepingActivator {

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ThreadPoolService.class, PluginsLoadedService.class, LeanConfigurationService.class,
            DatabaseService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        MWHealthCheckServiceImpl service = new MWHealthCheckServiceImpl(this);
        registerService(MWHealthCheckService.class, service);

        registerService(MWHealthCheck.class, new AllPluginsLoadedCheck(this));
        registerService(MWHealthCheck.class, new ConfigDBCheck(this));
        registerService(MWHealthCheck.class, new HazelcastCheck(this));
        registerService(MWHealthCheck.class, new JVMHeapCheck());

        HealthCheckResponse.setResponseProvider(new HealthCheckResponseProviderImpl());

        track(MWHealthCheck.class, new MWHealthCheckTracker(context, service));
        track(HealthCheck.class, new HealthCheckTracker(context, service));
        track(HazelcastInstance.class);
        openTrackers();
    }

}
