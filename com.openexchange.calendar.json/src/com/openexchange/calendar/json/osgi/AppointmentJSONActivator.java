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

package com.openexchange.calendar.json.osgi;

import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.ajax.requesthandler.ResultConverter;
import com.openexchange.ajax.requesthandler.osgiservice.AJAXModuleActivator;
import com.openexchange.calendar.json.AppointmentActionFactory;
import com.openexchange.calendar.json.converters.AppointmentResultConverter;
import com.openexchange.calendar.json.converters.EventResultConverter;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.capabilities.CapabilitySet;
import com.openexchange.chronos.itip.ITipActionPerformerFactoryService;
import com.openexchange.chronos.itip.ITipAnalyzerService;
import com.openexchange.chronos.itip.json.ITipActionFactory;
import com.openexchange.chronos.service.CalendarService;
import com.openexchange.chronos.service.RecurrenceService;
import com.openexchange.conversion.ConversionService;
import com.openexchange.groupware.userconfiguration.Permission;
import com.openexchange.oauth.provider.resourceserver.scope.AbstractScopeProvider;
import com.openexchange.oauth.provider.resourceserver.scope.OAuthScopeProvider;
import com.openexchange.objectusecount.ObjectUseCountService;
import com.openexchange.osgi.RankingAwareNearRegistryServiceTracker;
import com.openexchange.user.UserService;

/**
 * {@link AppointmentJSONActivator}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public class AppointmentJSONActivator extends AJAXModuleActivator {

    private static final Class<?>[] NEEDED = new Class[] {
        CalendarService.class, UserService.class, RecurrenceService.class, ConversionService.class, ITipActionPerformerFactoryService.class, ITipAnalyzerService.class
    };

    @Override
    protected Class<?>[] getNeededServices() {
        return NEEDED;
    }

    @Override
    protected void startBundle() throws Exception {
        //        final Dictionary<String, Integer> props = new Hashtable<String, Integer>(1, 1);
        //        props.put(TargetService.MODULE_PROPERTY, I(Types.APPOINTMENT));
        //        registerService(TargetService.class, new ModifyThroughDependant(), props);
        registerModule(new AppointmentActionFactory(this), "calendar");
        registerService(ResultConverter.class, new AppointmentResultConverter(this));
        registerService(ResultConverter.class, new EventResultConverter(this));



        RankingAwareNearRegistryServiceTracker<ITipAnalyzerService> rankingTracker = new RankingAwareNearRegistryServiceTracker<>(context, ITipAnalyzerService.class, 0);
        RankingAwareNearRegistryServiceTracker<ITipActionPerformerFactoryService> factoryTracker = new RankingAwareNearRegistryServiceTracker<>(context, ITipActionPerformerFactoryService.class, 0);
        ServiceTracker<CapabilityService, CapabilityService> capabilityTracker = track(CapabilityService.class);
        rememberTracker(rankingTracker);
        rememberTracker(factoryTracker);
        rememberTracker(capabilityTracker);
        openTrackers();

        ITipActionFactory.INSTANCE = new ITipActionFactory(this, rankingTracker, factoryTracker);
        registerModule(ITipActionFactory.INSTANCE, "calendar/itip");


        registerService(OAuthScopeProvider.class, new AbstractScopeProvider(AppointmentActionFactory.OAUTH_READ_SCOPE, OAuthScopeDescription.READ_ONLY) {

            @Override
            public boolean canBeGranted(CapabilitySet capabilities) {
                return capabilities.contains(Permission.CALENDAR.getCapabilityName());
            }
        });
        registerService(OAuthScopeProvider.class, new AbstractScopeProvider(AppointmentActionFactory.OAUTH_WRITE_SCOPE, OAuthScopeDescription.WRITABLE) {

            @Override
            public boolean canBeGranted(CapabilitySet capabilities) {
                return capabilities.contains(Permission.CALENDAR.getCapabilityName());
            }
        });

        trackService(ObjectUseCountService.class);
        openTrackers();
    }

}
