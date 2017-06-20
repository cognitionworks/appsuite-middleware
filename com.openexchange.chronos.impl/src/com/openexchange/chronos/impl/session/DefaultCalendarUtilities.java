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

package com.openexchange.chronos.impl.session;

import java.util.List;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.impl.EventMapper;
import com.openexchange.chronos.impl.EventUpdateImpl;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.CalendarUtilities;
import com.openexchange.chronos.service.EntityResolver;
import com.openexchange.chronos.service.EventUpdate;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.tools.mappings.MappedIncorrectString;
import com.openexchange.groupware.tools.mappings.MappedTruncation;
import com.openexchange.server.ServiceLookup;

/**
 * {@link DefaultCalendarUtilities}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class DefaultCalendarUtilities implements CalendarUtilities {

    private final CalendarSession session;
    private final ServiceLookup services;

    /**
     * Initializes a new {@link DefaultCalendarUtilities}.
     *
     * @param services A service lookup reference
     */
    public DefaultCalendarUtilities(ServiceLookup services) {
        this(services, null);
    }

    /**
     * Initializes a new {@link DefaultCalendarUtilities}.
     *
     * @param services A service lookup reference
     * @param session The underlying calendar session, or <code>null</code> if not bound to a specific session
     */
    public DefaultCalendarUtilities(ServiceLookup services, CalendarSession session) {
        super();
        this.services = services;
        this.session = session;
    }

    @Override
    public EventUpdate compare(Event original, Event update, boolean considerUnset, EventField... ignoredFields) throws OXException {
        return new EventUpdateImpl(original, update, considerUnset, ignoredFields);
    }

    @Override
    public boolean handleIncorrectString(OXException e, Event event) {
        boolean hasReplaced = false;
        if (null != event) {
            try {
                hasReplaced |= MappedIncorrectString.replace(e.getProblematics(), event, "");
            } catch (ClassCastException | OXException x1) {
                // also try attendees
                List<Attendee> attendees = event.getAttendees();
                if (null != attendees) {
                    for (Attendee attendee : attendees) {
                        try {
                            hasReplaced |= MappedIncorrectString.replace(e.getProblematics(), attendee, "");
                        } catch (ClassCastException | OXException x2) {
                            // ignore
                        }
                    }
                }
            }
        }
        return hasReplaced;
    }

    @Override
    public boolean handleDataTruncation(OXException e, Event event) {
        boolean hasTrimmed = false;
        if (null != event) {
            try {
                hasTrimmed |= MappedTruncation.truncate(e.getProblematics(), event);
            } catch (ClassCastException | OXException x1) {
                // also try attendees
                List<Attendee> attendees = event.getAttendees();
                if (null != attendees) {
                    for (Attendee attendee : attendees) {
                        try {
                            hasTrimmed |= MappedTruncation.truncate(e.getProblematics(), attendee);
                        } catch (ClassCastException | OXException x2) {
                            // ignore
                        }
                    }
                }
            }
        }
        return hasTrimmed;
    }

    @Override
    public Event copyEvent(Event event, EventField... fields) throws OXException {
        return EventMapper.getInstance().copy(event, null, fields);
    }

    @Override
    public EntityResolver getEntityResolver(int contextId) throws OXException {
        if (null != session && session.getContextId() == contextId) {
            return session.getEntityResolver();
        }
        return new DefaultEntityResolver(contextId, services);
    }

}
