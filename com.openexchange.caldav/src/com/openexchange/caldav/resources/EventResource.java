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

package com.openexchange.caldav.resources;

import static com.openexchange.dav.DAVProtocol.protocolException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.caldav.CalDAVImport;
import com.openexchange.caldav.CaldavProtocol;
import com.openexchange.caldav.EventPatches;
import com.openexchange.caldav.GroupwareCaldavFactory;
import com.openexchange.caldav.PhantomMaster;
import com.openexchange.caldav.Tools;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.ical.CalendarExport;
import com.openexchange.chronos.ical.ICalParameters;
import com.openexchange.chronos.ical.ICalService;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.chronos.service.CalendarService;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.CalendarUtilities;
import com.openexchange.chronos.service.EventID;
import com.openexchange.dav.DAVProtocol;
import com.openexchange.dav.DAVUserAgent;
import com.openexchange.dav.PreconditionException;
import com.openexchange.dav.resources.DAVCollection;
import com.openexchange.dav.resources.DAVObjectResource;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.notify.hostname.HostData;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;
import com.openexchange.user.UserService;
import com.openexchange.webdav.protocol.WebdavPath;
import com.openexchange.webdav.protocol.WebdavProperty;
import com.openexchange.webdav.protocol.WebdavProtocolException;
import com.openexchange.webdav.protocol.WebdavResource;

/**
 * {@link EventResource}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class EventResource extends DAVObjectResource<Event> {

    private static final String CONTENT_TYPE = "text/calendar; charset=UTF-8";
    private static final int MAX_RETRIES = 3;
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventResource.class);

    private final EventCollection parent;
    private final Event object;

    private byte[] iCalFile;
    private CalDAVImport caldavImport;

    /**
     * Initializes a new {@link EventResource}.
     *
     * @param parent The parent event collection
     * @param event The represented event
     * @param url The WebDAV path to the resource
     * @throws OXException
     */
    public EventResource(EventCollection parent, Event event, WebdavPath url) throws OXException {
        super(parent, event, url);
        this.parent = parent;
        this.object = event;
    }

    /**
     * Gets the parent event collection of this resource.
     *
     * @return The parent event collection
     */
    public EventCollection getParent() {
        return parent;
    }

    /**
     * Gets the underlying event represented through this resource.
     *
     * @return The event, or <code>null</code> if not existent
     */
    public Event getEvent() {
        return object;
    }

    /**
     * Gets the calendar session.
     *
     * @return The calendar session
     */
    public CalendarSession getCalendarSession() throws WebdavProtocolException {
        return parent.getCalendarSession();
    }

    @Override
    protected String getFileExtension() {
        return CalDAVResource.EXTENSION_ICS;
    }

    @Override
    protected Date getCreationDate(Event object) {
        return null != object ? object.getCreated() : null;
    }

    @Override
    protected Date getLastModified(Event object) {
        return null != object ? new Date(object.getTimestamp()) : null;
    }

    @Override
    protected int getId(Event object) {
        return null != object ? Integer.parseInt(object.getId()) : null;
    }

    @Override
    public HostData getHostData() throws WebdavProtocolException {
        return super.getHostData();
    }

    @Override
    public DAVUserAgent getUserAgent() {
        return super.getUserAgent();
    }

    @Override
    public GroupwareCaldavFactory getFactory() {
        return parent.getFactory();
    }

    @Override
    public void putBody(InputStream body, boolean guessSize) throws WebdavProtocolException {
        try {
            caldavImport = new CalDAVImport(this, body);
        } catch (OXException e) {
            throw getProtocolException(e);
        }
    }

    private byte[] getICalFile() throws WebdavProtocolException {
        if (null == iCalFile) {
            try {
                iCalFile = generateICal();
            } catch (OXException e) {
                throw getProtocolException(e);
            }
        }
        return iCalFile;
    }

    private byte[] generateICal() throws OXException {
        InputStream inputStream = null;
        try {
            /*
             * init export
             */
            CalendarSession calendarSession = getCalendarSession();
            calendarSession.set(CalendarParameters.PARAMETER_FIELDS, null);
            ICalService iCalService = getFactory().requireService(ICalService.class);
            ICalParameters iCalParameters = EventPatches.applyIgnoredProperties(this, iCalService.initParameters());
            CalendarExport calendarExport = iCalService.exportICal(iCalParameters);
            List<Event> changeExceptions = null;
            if (PhantomMaster.class.isInstance(object)) {
                /*
                 * no access to parent recurring master, use detached occurrences as exceptions
                 */
                changeExceptions = calendarSession.getCalendarService().getChangeExceptions(calendarSession, object.getFolderId(), object.getSeriesId());
            } else {
                /*
                 * load all event data & add (master) event to export
                 */
                Event event = calendarSession.getCalendarService().getEvent(calendarSession, object.getFolderId(), new EventID(object.getFolderId(), object.getId(), object.getRecurrenceId()));
                event = EventPatches.Outgoing.applyAll(this, event);
                calendarExport.add(event);
                if (CalendarUtils.isSeriesMaster(object)) {
                    changeExceptions = calendarSession.getCalendarService().getChangeExceptions(calendarSession, object.getFolderId(), object.getSeriesId());
                }
            }
            /*
             * add change exceptions to export
             */
            if (null != changeExceptions && 0 < changeExceptions.size()) {
                for (Event changeException : changeExceptions) {
                    changeException = EventPatches.Outgoing.applyAll(this, changeException);
                    calendarExport.add(changeException);
                }
            }
            /*
             * add any extended properties
             */
            EventPatches.Outgoing.applyExport(this, calendarExport);
            inputStream = calendarExport.getClosingStream();
            return Streams.stream2bytes(inputStream);
        } catch (IOException e) {
            throw protocolException(getUrl(), e);
        } finally {
            Streams.close(inputStream);
        }
    }

    @Override
    public Long getLength() throws WebdavProtocolException {
        byte[] iCalFile = getICalFile();
        return new Long(null != iCalFile ? iCalFile.length : 0);
    }

    @Override
    public String getContentType() throws WebdavProtocolException {
        return CONTENT_TYPE;
    }

    @Override
    public InputStream getBody() throws WebdavProtocolException {
        byte[] iCalFile = getICalFile();
        if (null != iCalFile) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("iCal file: {}", new String(iCalFile, Charsets.UTF_8));
            }
            return Streams.newByteArrayInputStream(iCalFile);
        }
        return null;
    }

    @Override
    protected WebdavProperty internalGetProperty(String namespace, String name) throws WebdavProtocolException {
        if (CaldavProtocol.CAL_NS.getURI().equals(namespace) && "calendar-data".equals(name)) {
            WebdavProperty property = new WebdavProperty(namespace, name);
            byte[] iCalFile = getICalFile();
            if (null != iCalFile) {
                property.setXML(true);
                property.setValue("<![CDATA[" + new String(iCalFile, Charsets.UTF_8) + "]]>");
            }
            return property;
        }
        if (CaldavProtocol.CALENDARSERVER_NS.getURI().equals(namespace) && ("created-by".equals(name) || "updated-by".equals(name))) {
            WebdavProperty property = new WebdavProperty(namespace, name);
            if (null != object) {
                int entityID;
                Date timestamp;
                if ("created-by".equals(name)) {
                    entityID = object.getCreatedBy();
                    timestamp = object.getCreated();
                } else {
                    entityID = object.getModifiedBy();
                    timestamp = new Date(object.getTimestamp());
                }
                try {
                    User user = factory.getService(UserService.class).getUser(entityID, factory.getContext());
                    property.setXML(true);
                    property.setValue(new StringBuilder()
                        .append("<CS:first-name>").append(user.getGivenName()).append("</CS:first-name>")
                        .append("<CS:last-name>").append(user.getSurname()).append("</CS:last-name>")
                        .append("<CS:dtstamp>").append(Tools.formatAsUTC(timestamp)).append("</CS:dtstamp>")
                        .append("<D:href>mailto:").append(user.getMail()).append("</D:href>")
                    .toString());
                } catch (OXException e) {
                    LOG.warn("error resolving user '{}'", entityID, e);
                }
            }
            return property;
        }
        return null;
    }

    @Override
    public EventResource move(WebdavPath dest, boolean noroot, boolean overwrite) throws WebdavProtocolException {
        WebdavResource destinationResource = factory.resolveResource(dest);
        DAVCollection destinationCollection = destinationResource.isCollection() ? (DAVCollection) destinationResource : getFactory().resolveCollection(dest.parent());
        if (false == parent.getClass().isInstance(destinationCollection)) {
            throw protocolException(getUrl(), HttpServletResponse.SC_FORBIDDEN);
        }
        EventCollection targetCollection = null;
        try {
            targetCollection = (EventCollection) destinationCollection;
        } catch (ClassCastException e) {
            throw protocolException(getUrl(), e, HttpServletResponse.SC_FORBIDDEN);
        }
        EventID eventID = new EventID(parent.folderID, object.getId());
        try {
            CalendarSession calendarSession = factory.getService(CalendarService.class).init(factory.getSession());
            calendarSession.set(CalendarParameters.PARAMETER_IGNORE_CONFLICTS, Boolean.TRUE);
            calendarSession.getCalendarService().moveEvent(calendarSession, eventID, targetCollection.folderID, object.getTimestamp());
        } catch (OXException e) {
            throw getProtocolException(e);
        }
        return this;
    }

    @Override
    public void create() throws WebdavProtocolException {
        int retryCount = 0;
        do {
            try {
                createEvent(caldavImport);
                return;
            } catch (OXException e) {
                if (++retryCount < MAX_RETRIES) {
                    Boolean handled = handleOnCreate(caldavImport, e);
                    if (Boolean.TRUE.equals(handled)) {
                        continue;
                    } else if (Boolean.FALSE.equals(handled)) {
                        return;
                    }
                }
                throw getProtocolException(e);
            }
        } while (true);
    }

    @Override
    public void delete() throws WebdavProtocolException {
        int retryCount = 0;
        do {
            try {
                deleteEvent(object);
                return;
            } catch (OXException e) {
                if (++retryCount < MAX_RETRIES) {
                    Boolean handled = handleOnDelete(object, e);
                    if (Boolean.TRUE.equals(handled)) {
                        continue;
                    } else if (Boolean.FALSE.equals(handled)) {
                        return;
                    }
                }
                throw getProtocolException(e);
            }
        } while (true);
    }

    @Override
    public void save() throws WebdavProtocolException {
        int retryCount = 0;
        do {
            try {
                updateEvent(caldavImport);
                return;
            } catch (OXException e) {
                if (++retryCount < MAX_RETRIES) {
                    Boolean handled = handleOnUpdate(caldavImport, e);
                    if (Boolean.TRUE.equals(handled)) {
                        continue;
                    } else if (Boolean.FALSE.equals(handled)) {
                        return;
                    }
                }
                throw getProtocolException(e);
            }
        } while (true);
    }

    private void deleteEvent(Event event) throws OXException {
        if (false == exists()) {
            throw protocolException(getUrl(), HttpServletResponse.SC_NOT_FOUND);
        }
        List<EventID> eventIDs;
        if (PhantomMaster.class.isInstance(event)) {
            List<Event> detachedOccurrences = ((PhantomMaster) event).getDetachedOccurrences();
            eventIDs = new ArrayList<EventID>(detachedOccurrences.size());
            for (Event detachedOccurrence : detachedOccurrences) {
                eventIDs.add(new EventID(detachedOccurrence.getFolderId(), detachedOccurrence.getId()));
            }
        } else {
            eventIDs = Collections.singletonList(new EventID(event.getFolderId(), event.getId()));
        }
        CalendarSession calendarSession = getCalendarSession();
        for (EventID id: eventIDs){
            calendarSession.getCalendarService().deleteEvent(calendarSession, id, event.getTimestamp());
        }
    }

    private void updateEvent(CalDAVImport caldavImport) throws OXException {
        if (false == exists()) {
            throw protocolException(getUrl(), HttpServletResponse.SC_NOT_FOUND);
        }
        CalendarSession calendarSession = getCalendarSession();
        calendarSession.set(CalendarParameters.PARAMETER_IGNORE_CONFLICTS, Boolean.TRUE);
        long clientTimestamp = object.getTimestamp();
        caldavImport = EventPatches.Incoming.applyAll(this, caldavImport);
        if (null != caldavImport.getEvent() && false == Tools.isPhantomMaster(object)) {
            /*
             * update event
             */
            EventID eventID = new EventID(parent.folderID, object.getId());
            CalendarResult result = calendarSession.getCalendarService().updateEvent(calendarSession, eventID, caldavImport.getEvent(), clientTimestamp);
            if (result.getUpdates().isEmpty()) {
                LOG.debug("{}: Master event {} not updated.", getUrl(), eventID);
            } else {
                clientTimestamp = result.getTimestamp();
                calendarSession.getEntityResolver().trackAttendeeUsage(result);
            }
        }
        /*
         * update exceptions
         */
        for (Event changeException : caldavImport.getChangeExceptions()) {
            EventID eventID = new EventID(parent.folderID, object.getId(), changeException.getRecurrenceId());
            CalendarResult result = calendarSession.getCalendarService().updateEvent(calendarSession, eventID, changeException, clientTimestamp);
            if (result.getUpdates().isEmpty()) {
                LOG.debug("{}: Exception {} not updated.", getUrl(), eventID);
            } else {
                clientTimestamp = result.getTimestamp();
                calendarSession.getEntityResolver().trackAttendeeUsage(result);
            }
        }
    }

    private Event createEvent(CalDAVImport caldavImport) throws OXException {
        if (exists()) {
            throw protocolException(getUrl(), HttpServletResponse.SC_CONFLICT);
        }
        if (null == caldavImport.getEvent()) {
            throw new PreconditionException(DAVProtocol.CAL_NS.getURI(), "valid-calendar-object-resource", url, HttpServletResponse.SC_FORBIDDEN);
        }
        /*
         * create event
         */
        CalendarSession calendarSession = factory.getService(CalendarService.class).init(factory.getSession());
        calendarSession.set(CalendarParameters.PARAMETER_IGNORE_CONFLICTS, Boolean.TRUE);
        caldavImport = EventPatches.Incoming.applyAll(this, caldavImport);
        CalendarResult result = calendarSession.getCalendarService().createEvent(calendarSession, parent.folderID, caldavImport.getEvent());
        if (result.getCreations().isEmpty()) {
            LOG.warn("{}: No event created.", getUrl());
            throw new PreconditionException(DAVProtocol.CAL_NS.getURI(), "valid-calendar-object-resource", url, HttpServletResponse.SC_FORBIDDEN);
        }
        Event createdEvent = result.getCreations().get(0).getCreatedEvent();
        long clientTimestamp = result.getTimestamp();
        calendarSession.getEntityResolver().trackAttendeeUsage(result);
        /*
         * create exceptions
         */
        for (Event changeException : caldavImport.getChangeExceptions()) {
            EventID eventID = new EventID(createdEvent.getFolderId(), createdEvent.getId(), changeException.getRecurrenceId());
            result = calendarSession.getCalendarService().updateEvent(calendarSession, eventID, changeException, clientTimestamp);
            if (result.getCreations().isEmpty()) {
                LOG.warn("{}: No exception created.", getUrl());
                throw new PreconditionException(DAVProtocol.CAL_NS.getURI(), "valid-calendar-object-resource", url, HttpServletResponse.SC_FORBIDDEN);
            }
            clientTimestamp = result.getTimestamp();
            calendarSession.getEntityResolver().trackAttendeeUsage(result);
        }
        return createdEvent;
    }

    /**
     * Tries to handle a {@link WebdavProtocolException} that occurred during resource deletion automatically.
     *
     * @param event The event being deleted
     * @param e The exception
     * @return {@link Boolean#TRUE} if the delete operation should be tried again, {@link Boolean#FALSE} if the problem was handled
     *         successfully and the delete operation should not be tried again, or <code>null</code> if not recoverable at all
     */
    private Boolean handleOnDelete(Event event, OXException e) {
        return null;
    }

    /**
     * Tries to handle a {@link WebdavProtocolException} that occurred during resource update automatically.
     *
     * @param caldavImport The CalDAV import the event should be updated from
     * @param e The exception
     * @return {@link Boolean#TRUE} if the update operation should be tried again, {@link Boolean#FALSE} if the problem was handled
     *         successfully and the update operation should not be tried again, or <code>null</code> if not recoverable at all
     */
    private Boolean handleOnUpdate(CalDAVImport caldavImport, OXException e) {
        try {
            switch (e.getErrorCode()) {
                case "CAL-5071": // Incorrect string [string %1$s, field %2$s, column %3$s]
                    return handleIncorrectString(caldavImport, e);
                case "CAL-5070": // Data truncation [field %1$s, limit %2$d, current %3$d]
                    return handleDataTruncation(caldavImport, e);
            }
        } catch (Exception x) {
            LOG.warn("Error during automatic handling of {}", e.getErrorCode(), x);
        }
        return null;
    }

    /**
     * Tries to handle a {@link WebdavProtocolException} that occurred during resource creation automatically.
     *
     * @param caldavImport The CalDAV import the event shoudl be created from
     * @param e The exception
     * @return {@link Boolean#TRUE} if the create operation should be tried again, {@link Boolean#FALSE} if the problem was handled
     *         successfully and the create operation should not be tried again, or <code>null</code> if not recoverable at all
     */
    private Boolean handleOnCreate(CalDAVImport caldavImport, OXException e) {
        try {
            switch (e.getErrorCode()) {
                case "CAL-4090": // UID conflict [uid %1$s, conflicting id %2$d]
                    return handleUIDConflict(caldavImport, e);
                case "CAL-5071": // Incorrect string [string %1$s, field %2$s, column %3$s]
                    return handleIncorrectString(caldavImport, e);
                case "CAL-5070": // Data truncation [field %1$s, limit %2$d, current %3$d]
                    return handleDataTruncation(caldavImport, e);
            }
        } catch (Exception x) {
            LOG.warn("Error during automatic handling of {}", e.getErrorCode(), x);
        }
        return null;
    }

    private Boolean handleIncorrectString(CalDAVImport caldavImport, OXException e) throws Exception {
        /*
         * replace mapped incorrect strings, indicate "try again" if replacements were possible
         */
        LOG.info("Incorrect string detected, replacing problematic characters and trying again.");
        CalendarUtilities calendarUtilities = getCalendarSession().getUtilities();
        boolean hasReplaced = calendarUtilities.handleIncorrectString(e, caldavImport.getEvent());
        for (Event changeException : caldavImport.getChangeExceptions()) {
            hasReplaced |= calendarUtilities.handleIncorrectString(e, changeException);
        }
        return hasReplaced ? Boolean.TRUE : null;
    }

    private Boolean handleDataTruncation(CalDAVImport caldavImport, OXException e) throws Exception {
        /*
         * trim mapped data truncations, indicate "try again" if possible
         */
        LOG.info("Data truncation detected, trimming problematic fields and trying again.");
        CalendarUtilities calendarUtilities = getCalendarSession().getUtilities();
        boolean hasTrimmed = calendarUtilities.handleDataTruncation(e, caldavImport.getEvent());
        hasTrimmed |= calendarUtilities.handleDataTruncation(e, caldavImport.getEvent().getAttendees());
        for (Event changeException : caldavImport.getChangeExceptions()) {
            hasTrimmed |= calendarUtilities.handleDataTruncation(e, changeException);
        }
        return hasTrimmed ? Boolean.TRUE : null;
    }

    /**
     * Handles an UID conflict during a create operation by attempting to perform an update of the existing event data instead.
     *
     * @param caldavImport The calendar data to import
     * @param e The exception that occurred
     * @return {@link Boolean#FALSE} if successfully handled and there's no need to try again, <code>null</code>, otherwise
     */
    private Boolean handleUIDConflict(CalDAVImport caldavImport, OXException e) throws Exception {
        /*
         * get identifier of conflicting event
         */
        CalendarSession calendarSession = parent.getCalendarSession();
        String conflictingId = null;
        if (null != e.getLogArgs() && 1 < e.getLogArgs().length && null != e.getLogArgs()[1] && String.class.isInstance(e.getLogArgs()[1])) {
            conflictingId = (String) e.getLogArgs()[1];
        }
        if (null == conflictingId) {
            conflictingId = calendarSession.getCalendarService().getUtilities().resolveByUID(calendarSession, caldavImport.getUID());
        }
        if (null != conflictingId) {
            /*
             * try to perform an update of the existing event with the imported data from the client
             */
            LOG.info("Event {} already exists (id {}), trying again as update.", caldavImport.getUID(), conflictingId);
            long clientTimestamp = System.currentTimeMillis(); //TODO timestamp from where?
            EventID eventID = new EventID(parent.folderID, conflictingId);
            CalendarResult result = calendarSession.getCalendarService().updateEvent(calendarSession, eventID, caldavImport.getEvent(), clientTimestamp);
            if (0 < result.getUpdates().size()) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    /**
     * Gets an appropriate WebDAV protocol exception for the supplied OX exception.
     *
     * @param e The OX exception to get the protocol exception for
     * @return The protocol exception
     */
    private WebdavProtocolException getProtocolException(OXException e) {
        switch (e.getErrorCode()) {
            case "CAL-4091":
            case "CAL-4092":
                LOG.info("{}: PUT operation failed due to non-ignorable conflicts.", getUrl());
                return new PreconditionException(e, DAVProtocol.CAL_NS.getURI(), "allowed-organizer-scheduling-object-change", getUrl(), HttpServletResponse.SC_FORBIDDEN);
            case "ICAL-0003":
            case "ICAL-0004":
            case "CAL-4221":
            case "CAL-4229":
                return new PreconditionException(e, DAVProtocol.CAL_NS.getURI(), "valid-calendar-data", getUrl(), HttpServletResponse.SC_FORBIDDEN);
            default:
                return com.openexchange.dav.DAVProtocol.protocolException(getUrl(), e);
        }
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getUrl());
        if (null != object) {
            stringBuilder.append(" [").append(object).append(']');
        }
        if (null != caldavImport) {
            stringBuilder.append(" [").append(caldavImport).append(']');
        }
        return stringBuilder.toString();
    }

}
