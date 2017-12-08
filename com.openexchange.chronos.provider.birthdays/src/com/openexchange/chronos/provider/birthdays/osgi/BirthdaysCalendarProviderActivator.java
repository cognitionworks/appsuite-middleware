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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.chronos.provider.birthdays.osgi;

import static com.openexchange.chronos.provider.birthdays.BirthdaysCalendarProvider.CAPABILITY_NAME;
import static org.slf4j.LoggerFactory.getLogger;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import com.openexchange.capabilities.CapabilityChecker;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.chronos.provider.CalendarProvider;
import com.openexchange.chronos.provider.account.AdministrativeCalendarAccountService;
import com.openexchange.chronos.provider.account.CalendarAccountService;
import com.openexchange.chronos.provider.birthdays.BirthdaysCalendarProvider;
import com.openexchange.chronos.provider.birthdays.ContactEventHandler;
import com.openexchange.chronos.service.CalendarUtilities;
import com.openexchange.chronos.service.RecurrenceService;
import com.openexchange.chronos.storage.CalendarStorageFactory;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.contact.ContactService;
import com.openexchange.context.ContextService;
import com.openexchange.conversion.ConversionService;
import com.openexchange.database.DatabaseService;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * {@link BirthdaysCalendarProviderActivator}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class BirthdaysCalendarProviderActivator extends HousekeepingActivator {

    /**
     * Initializes a new {@link BirthdaysCalendarProviderActivator}.
     */
    public BirthdaysCalendarProviderActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ContactService.class, RecurrenceService.class, CalendarUtilities.class, FolderService.class,
            CalendarStorageFactory.class, DatabaseService.class, ContextService.class, CalendarAccountService.class,
            AdministrativeCalendarAccountService.class, ConversionService.class, ConfigViewFactory.class, CapabilityService.class
        };
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            getLogger(BirthdaysCalendarProviderActivator.class).info("starting bundle {}", context.getBundle());
            /*
             * register calendar provider
             */
            BirthdaysCalendarProvider calendarProvider = new BirthdaysCalendarProvider(this);
            registerService(CalendarProvider.class, calendarProvider);
            /*
             * declare capability & register an appropriate checker
             */
            getService(CapabilityService.class).declareCapability(CAPABILITY_NAME);
            registerService(CapabilityChecker.class, new CapabilityChecker() {

                @Override
                public boolean isEnabled(String capability, Session session) throws OXException {
                    return CAPABILITY_NAME.equals(capability) && calendarProvider.isEnabled(ServerSessionAdapter.valueOf(session));
                }
            }, singletonDictionary(CapabilityChecker.PROPERTY_CAPABILITIES, CAPABILITY_NAME));
            /*
             * register event handler for contact changes
             */
            registerService(EventHandler.class, new ContactEventHandler(this), singletonDictionary(EventConstants.EVENT_TOPIC, ContactEventHandler.TOPICS));
        } catch (Exception e) {
            getLogger(BirthdaysCalendarProviderActivator.class).error("error starting {}", context.getBundle(), e);
            throw e;
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        getLogger(BirthdaysCalendarProviderActivator.class).info("stopping bundle {}", context.getBundle());
        super.stopBundle();
    }

}
