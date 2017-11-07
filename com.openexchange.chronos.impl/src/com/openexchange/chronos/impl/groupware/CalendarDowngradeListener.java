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

package com.openexchange.chronos.impl.groupware;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.service.CalendarHandler;
import com.openexchange.chronos.service.CalendarUtilities;
import com.openexchange.chronos.storage.CalendarStorageFactory;
import com.openexchange.database.provider.SimpleDBProvider;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.downgrade.DowngradeEvent;
import com.openexchange.groupware.downgrade.DowngradeListener;
import com.openexchange.osgi.ServiceSet;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * {@link CalendarDowngradeListener} - {@link DowngradeListener} for calendar data
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.0
 */
public class CalendarDowngradeListener implements DowngradeListener {

    private final CalendarStorageFactory      factory;
    private final CalendarUtilities           calendarUtilities;
    private final ServiceSet<CalendarHandler> calendarHandlers;

    /**
     * Initializes a new {@link CalendarDeleteListener}.
     * 
     * @param factory The {@link CalendarStorageFactory}
     * @param calendarUtilities The {@link CalendarUtilities}
     * @param calendarHandlers The {@link CalendarHandler}s to notify
     */
    public CalendarDowngradeListener(CalendarStorageFactory factory, CalendarUtilities calendarUtilities, ServiceSet<CalendarHandler> calendarHandlers) {
        super();
        this.factory = factory;
        this.calendarUtilities = calendarUtilities;
        this.calendarHandlers = calendarHandlers;
    }

    @Override
    public void downgradePerformed(DowngradeEvent event) throws OXException {
        if (false == event.getNewUserConfiguration().hasCalendar()) {
            int userId = event.getNewUserConfiguration().getUserId();
            StorageUpdater updater = new StorageUpdater(event.getContext(), userId, null, calendarUtilities, factory, new SimpleDBProvider(event.getReadCon(), event.getWriteCon()), calendarHandlers);

            // Get all events which the user attends and that are *NOT* located in his private folders.
            CompositeSearchTerm term = new CompositeSearchTerm(CompositeOperation.AND)
                .addSearchTerm(CalendarUtils.getSearchTerm(AttendeeField.ENTITY, SingleOperation.EQUALS, Integer.valueOf(userId)))
                .addSearchTerm(CalendarUtils.getSearchTerm(AttendeeField.FOLDER_ID, SingleOperation.ISNULL));
            List<String> publicEvents = updater.searchEvents(term, new EventField[] { EventField.ID }).stream().map(Event::getId).collect(Collectors.toList());

            // Get all events the user attends
            List<Event> events = updater.searchEvents();

            ServerSession serverSession = ServerSessionAdapter.valueOf(userId, event.getContext().getContextId());
            Date date = new Date();

            List<Event> eventsToRemoveTheAttendee = new LinkedList<>();
            for (final Event e : events) {
                if (publicEvents.contains(e.getId()) && false == CalendarUtils.isLastUserAttendee(e.getAttendees(), userId)) {
                    // Don't delete public events where other user still attend
                    eventsToRemoveTheAttendee.add(e);
                }
            }
            updater.removeAttendeeFrom(eventsToRemoveTheAttendee, date);
            events.removeAll(eventsToRemoveTheAttendee);
            // Delete all other events
            updater.deleteEvent(events, serverSession, date);

            // Trigger calendar events
            updater.notifyCalendarHandlers(event.getSession());
        }
    }

    @Override
    public int getOrder() {
        return 1;
    }

}
