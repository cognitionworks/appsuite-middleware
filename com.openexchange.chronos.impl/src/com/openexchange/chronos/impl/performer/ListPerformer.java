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

package com.openexchange.chronos.impl.performer;

import static com.openexchange.chronos.common.CalendarUtils.initCalendar;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.chronos.impl.Utils.find;
import static com.openexchange.chronos.impl.Utils.getTimeZone;
import static com.openexchange.chronos.impl.Utils.i;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.I2i;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventID;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.UserizedFolder;

/**
 * {@link ListPerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class ListPerformer extends AbstractQueryPerformer {

    /**
     * Initializes a new {@link ListPerformer}.
     *
     * @param session The calendar session
     * @param storage The underlying calendar storage
     */
    public ListPerformer(CalendarSession session, CalendarStorage storage) throws OXException {
        super(session, storage);
    }

    /**
     * Performs the operation.
     *
     * @param eventIDs The identifiers of the events to get
     * @return The loaded events
     */
    public List<Event> perform(List<EventID> eventIDs) throws OXException {
        Map<UserizedFolder, List<EventID>> idsPerFolder = getIdsPerFolder(eventIDs);
        Map<Integer, List<Event>> eventsPerFolderId = new HashMap<Integer, List<Event>>(idsPerFolder.size());
        for (Map.Entry<UserizedFolder, List<EventID>> entry : idsPerFolder.entrySet()) {
            List<Event> eventsInFolder = readEventsInFolder(entry.getKey(), entry.getValue());
            eventsPerFolderId.put(I(i(entry.getKey())), postProcess(eventsInFolder, entry.getKey(), true));
        }
        List<Event> orderedEvents = new ArrayList<Event>(eventIDs.size());
        for (EventID eventID : eventIDs) {
            List<Event> eventsInFolder = eventsPerFolderId.get(I(eventID.getFolderID()));
            Event event = find(eventsInFolder, eventID.getObjectID(), eventID.getRecurrenceID());
            if (null == event) {
                org.slf4j.LoggerFactory.getLogger(ListPerformer.class).debug("Event with {} not found, skipping.", eventID);
            }
            orderedEvents.add(event);
        }
        return orderedEvents;
    }

    private List<Event> readEventsInFolder(UserizedFolder folder, List<EventID> eventIDs) throws OXException {
        Set<Integer> objectIDs = new HashSet<Integer>(eventIDs.size());
        int folderID = i(folder);
        for (EventID eventID : eventIDs) {
            if (folderID == eventID.getFolderID()) {
                objectIDs.add(I(eventID.getObjectID()));
            }
        }
        List<Event> events = readEventsInFolder(folder, I2i(objectIDs), false, null);
        List<Event> orderedEvents = new ArrayList<Event>(eventIDs.size());
        for (EventID eventID : eventIDs) {
            Event event = find(events, eventID.getObjectID());
            if (null == event) {
                continue;
            }
            if (null != eventID.getRecurrenceID()) {
                if (isSeriesMaster(event)) {
                    Calendar fromCalendar = initCalendar(getTimeZone(session), eventID.getRecurrenceID().getValue());
                    Iterator<Event> iterator = session.getRecurrenceService().calculateInstancesRespectExceptions(event, fromCalendar, null, I(1), null);
                    if (false == iterator.hasNext()) {
                        throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(I(eventID.getObjectID()), eventID.getRecurrenceID());
                    }
                    orderedEvents.add(iterator.next());
                } else if (eventID.getRecurrenceID().equals(event.getRecurrenceId())) {
                    orderedEvents.add(event);
                } else {
                    throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(I(eventID.getObjectID()), eventID.getRecurrenceID());
                }
            } else {
                orderedEvents.add(event);
            }
        }
        return orderedEvents;
    }

    private Map<UserizedFolder, List<EventID>> getIdsPerFolder(List<EventID> eventIDs) throws OXException {
        Map<Integer, List<EventID>> idsPerFolderId = new HashMap<Integer, List<EventID>>();
        for (EventID eventID : eventIDs) {
            com.openexchange.tools.arrays.Collections.put(idsPerFolderId, I(eventID.getFolderID()), eventID);
        }
        Map<UserizedFolder, List<EventID>> idsPerFolder = new HashMap<UserizedFolder, List<EventID>>(idsPerFolderId.size());
        for (Map.Entry<Integer, List<EventID>> entry : idsPerFolderId.entrySet()) {
            idsPerFolder.put(Utils.getFolder(session, entry.getKey().intValue()), entry.getValue());
        }
        return idsPerFolder;
    }

}
