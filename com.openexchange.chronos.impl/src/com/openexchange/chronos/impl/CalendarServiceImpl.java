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

package com.openexchange.chronos.impl;

import static com.openexchange.chronos.impl.Utils.getFolder;
import static com.openexchange.java.Autoboxing.L;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.impl.performer.AllPerformer;
import com.openexchange.chronos.impl.performer.ChangeExceptionsPerformer;
import com.openexchange.chronos.impl.performer.CreatePerformer;
import com.openexchange.chronos.impl.performer.DeletePerformer;
import com.openexchange.chronos.impl.performer.GetPerformer;
import com.openexchange.chronos.impl.performer.ListPerformer;
import com.openexchange.chronos.impl.performer.MovePerformer;
import com.openexchange.chronos.impl.performer.ResolveFilenamePerformer;
import com.openexchange.chronos.impl.performer.ResolveUidPerformer;
import com.openexchange.chronos.impl.performer.SearchPerformer;
import com.openexchange.chronos.impl.performer.SequenceNumberPerformer;
import com.openexchange.chronos.impl.performer.TouchPerformer;
import com.openexchange.chronos.impl.performer.UpdateAttendeePerformer;
import com.openexchange.chronos.impl.performer.UpdatePerformer;
import com.openexchange.chronos.impl.performer.UpdatesPerformer;
import com.openexchange.chronos.impl.session.DefaultCalendarSession;
import com.openexchange.chronos.service.CalendarHandler;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.chronos.service.CalendarService;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventID;
import com.openexchange.chronos.service.SearchFilter;
import com.openexchange.chronos.service.UpdatesResult;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.ServiceSet;
import com.openexchange.session.Session;

/**
 * {@link CalendarServiceImpl}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class CalendarServiceImpl implements CalendarService {

    private final ServiceSet<CalendarHandler> calendarHandlers;

    /**
     * Initializes a new {@link CalendarServiceImpl}.
     *
     * @param calendarHandlers The calendar handlers service set
     */
    public CalendarServiceImpl(ServiceSet<CalendarHandler> calendarHandlers) {
        super();
        this.calendarHandlers = calendarHandlers;
    }

    @Override
    public CalendarSession init(Session session) throws OXException {
        return init(session, null);
    }

    @Override
    public CalendarSession init(Session session, CalendarParameters parameters) throws OXException {
        DefaultCalendarSession calendarSession = new DefaultCalendarSession(session, this);
        if (null != parameters) {
            for (Entry<String, Object> entry : parameters.entrySet()) {
                calendarSession.set(entry.getKey(), entry.getValue());
            }
        }
        return calendarSession;
    }

    @Override
    public List<Event> getChangeExceptions(CalendarSession session, final String folderID, final String objectID) throws OXException {
        return new AbstractCalendarStorageOperation<List<Event>>(session) {

            @Override
            protected List<Event> execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new ChangeExceptionsPerformer(session, storage).perform(getFolder(session, folderID), objectID);
            }
        }.executeQuery();
    }

    @Override
    public long getSequenceNumber(CalendarSession session, final String folderID) throws OXException {
        return new AbstractCalendarStorageOperation<Long>(session) {

            @Override
            protected Long execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return L(new SequenceNumberPerformer(session, storage).perform(getFolder(session, folderID)));
            }
        }.executeQuery().longValue();
    }

    @Override
    public String resolveByUID(CalendarSession session, final String uid) throws OXException {
        return new AbstractCalendarStorageOperation<String>(session) {

            @Override
            protected String execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new ResolveUidPerformer(storage).perform(uid);
            }
        }.executeQuery();
    }

    @Override
    public String resolveByFilename(CalendarSession session, final String filename) throws OXException {
        return new AbstractCalendarStorageOperation<String>(session) {

            @Override
            protected String execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new ResolveFilenamePerformer(storage).perform(filename);
            }
        }.executeQuery();
    }

    @Override
    public List<Event> searchEvents(CalendarSession session, final String[] folderIDs, final String pattern) throws OXException {
        return new AbstractCalendarStorageOperation<List<Event>>(session) {

            @Override
            protected List<Event> execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new SearchPerformer(session, storage).perform(folderIDs, pattern);
            }
        }.executeQuery();
    }

    @Override
    public List<Event> searchEvents(CalendarSession session, final String[] folderIDs, final List<SearchFilter> filters, final List<String> queries) throws OXException {
        return new AbstractCalendarStorageOperation<List<Event>>(session) {

            @Override
            protected List<Event> execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new SearchPerformer(session, storage).perform(folderIDs, filters, queries);
            }
        }.executeQuery();
    }

    @Override
    public Event getEvent(CalendarSession session, final String folderID, final EventID eventId) throws OXException {
        return new AbstractCalendarStorageOperation<Event>(session) {

            @Override
            protected Event execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new GetPerformer(session, storage).perform(getFolder(session, folderID), eventId);
            }
        }.executeQuery();
    }

    @Override
    public List<Event> getEvents(CalendarSession session, final List<EventID> eventIDs) throws OXException {
        return new AbstractCalendarStorageOperation<List<Event>>(session) {

            @Override
            protected List<Event> execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new ListPerformer(session, storage).perform(eventIDs);
            }
        }.executeQuery();
    }

    @Override
    public List<Event> getEventsInFolder(CalendarSession session, final String folderID) throws OXException {
        return new AbstractCalendarStorageOperation<List<Event>>(session) {

            @Override
            protected List<Event> execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new AllPerformer(session, storage).perform(getFolder(session, folderID));
            }
        }.executeQuery();
    }

    @Override
    public List<Event> getEventsOfUser(final CalendarSession session) throws OXException {
        return new AbstractCalendarStorageOperation<List<Event>>(session) {

            @Override
            protected List<Event> execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new AllPerformer(session, storage).perform();
            }
        }.executeQuery();
    }

    @Override
    public UpdatesResult getUpdatedEventsInFolder(CalendarSession session, final String folderID, final Date updatedSince) throws OXException {
        return new AbstractCalendarStorageOperation<UpdatesResult>(session) {

            @Override
            protected UpdatesResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new UpdatesPerformer(session, storage).perform(getFolder(session, folderID), updatedSince);
            }
        }.executeQuery();
    }

    @Override
    public UpdatesResult getUpdatedEventsOfUser(CalendarSession session, final Date updatedSince) throws OXException {
        return new AbstractCalendarStorageOperation<UpdatesResult>(session) {

            @Override
            protected UpdatesResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new UpdatesPerformer(session, storage).perform(updatedSince);
            }
        }.executeQuery();
    }

    @Override
    public CalendarResult createEvent(CalendarSession session, final String folderId, final Event event) throws OXException {
        /*
         * insert event & notify handlers
         */
        return notifyHandlers(new AbstractCalendarStorageOperation<CalendarResult>(session) {

            @Override
            protected CalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new CreatePerformer(storage, session, getFolder(session, folderId)).perform(event);
            }
        }.executeUpdate());
    }

    @Override
    public CalendarResult updateEvent(CalendarSession session, final EventID eventID, final Event event) throws OXException {
        /*
         * update event & notify handlers
         */
        return notifyHandlers(new AbstractCalendarStorageOperation<CalendarResult>(session) {

            @Override
            protected CalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                Long clientTimestampValue = session.get(CalendarParameters.PARAMETER_TIMESTAMP, Long.class);
                long clientTimestamp = null != clientTimestampValue ? clientTimestampValue.longValue() : -1L;
                return new UpdatePerformer(storage, session, getFolder(session, eventID.getFolderID()))
                    .perform(eventID.getObjectID(), eventID.getRecurrenceID(), event, clientTimestamp);
            }

        }.executeUpdate());
    }

    @Override
    public CalendarResult touchEvent(CalendarSession session, final EventID eventID) throws OXException {
        /*
         * touch event & notify handlers
         */
        return notifyHandlers(new AbstractCalendarStorageOperation<CalendarResult>(session) {

            @Override
            protected CalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new TouchPerformer(storage, session, getFolder(session, eventID.getFolderID())).perform(eventID.getObjectID());
            }

        }.executeUpdate());
    }

    @Override
    public CalendarResult moveEvent(CalendarSession session, final EventID eventID, final String folderId) throws OXException {
        /*
         * move event & notify handlers
         */
        return notifyHandlers(new AbstractCalendarStorageOperation<CalendarResult>(session) {

            @Override
            protected CalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                Long clientTimestampValue = session.get(CalendarParameters.PARAMETER_TIMESTAMP, Long.class);
                long clientTimestamp = null != clientTimestampValue ? clientTimestampValue.longValue() : -1L;
                return new MovePerformer(storage, session, getFolder(session, eventID.getFolderID()))
                    .perform(eventID.getObjectID(), getFolder(session, folderId), clientTimestamp);
            }
        }.executeUpdate());
    }

    @Override
    public CalendarResult updateAttendee(CalendarSession session, final EventID eventID, final Attendee attendee) throws OXException {
        /*
         * update attendee & notify handlers
         */
        return notifyHandlers(new AbstractCalendarStorageOperation<CalendarResult>(session) {

            @Override
            protected CalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                Long clientTimestamp = session.get(CalendarParameters.PARAMETER_TIMESTAMP, Long.class);
                return new UpdateAttendeePerformer(storage, session, getFolder(session, eventID.getFolderID()))
                    .perform(eventID.getObjectID(), eventID.getRecurrenceID(), attendee, clientTimestamp);

            }
        }.executeUpdate());
    }

    @Override
    public CalendarResult deleteEvent(CalendarSession session, final EventID eventID) throws OXException {
        /*
         * delete event
         */
        CalendarResult result = new AbstractCalendarStorageOperation<CalendarResult>(session) {

            @Override
            protected CalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                Long clientTimestampValue = session.get(CalendarParameters.PARAMETER_TIMESTAMP, Long.class);
                long clientTimestamp = null != clientTimestampValue ? clientTimestampValue.longValue() : -1L;

                return new DeletePerformer(storage, session, getFolder(session, eventID.getFolderID())).perform(eventID.getObjectID(), eventID.getRecurrenceID(), clientTimestamp);

            }
        }.executeUpdate();

        /*
         * notify handlers
         */
        notifyHandlers(result);
        return result;
    }

    private CalendarResult notifyHandlers(CalendarResult result) {
        for (CalendarHandler handler : calendarHandlers) {
            handler.handle(result);
        }
        return result;
    }
}
