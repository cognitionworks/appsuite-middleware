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
package com.openexchange.data.conversion.ical.ical4j.internal;

import java.util.ArrayList;
import java.util.List;
import net.fortuna.ical4j.model.component.VEvent;
import com.openexchange.data.conversion.ical.ical4j.internal.appointment.ChangeExceptions;
import com.openexchange.data.conversion.ical.ical4j.internal.appointment.DeleteExceptions;
import com.openexchange.data.conversion.ical.ical4j.internal.appointment.IgnoreConflicts;
import com.openexchange.data.conversion.ical.ical4j.internal.appointment.Location;
import com.openexchange.data.conversion.ical.ical4j.internal.appointment.PrivateAppointmentsHaveNoParticipants;
import com.openexchange.data.conversion.ical.ical4j.internal.appointment.RequireEndDate;
import com.openexchange.data.conversion.ical.ical4j.internal.appointment.RequireStartDate;
import com.openexchange.data.conversion.ical.ical4j.internal.appointment.Transparency;
import com.openexchange.data.conversion.ical.ical4j.internal.calendar.Alarm;
import com.openexchange.data.conversion.ical.ical4j.internal.calendar.Categories;
import com.openexchange.data.conversion.ical.ical4j.internal.calendar.CreatedAndDTStamp;
import com.openexchange.data.conversion.ical.ical4j.internal.calendar.CreatedBy;
import com.openexchange.data.conversion.ical.ical4j.internal.calendar.Duration;
import com.openexchange.data.conversion.ical.ical4j.internal.calendar.End;
import com.openexchange.data.conversion.ical.ical4j.internal.calendar.Klass;
import com.openexchange.data.conversion.ical.ical4j.internal.calendar.LastModified;
import com.openexchange.data.conversion.ical.ical4j.internal.calendar.Note;
import com.openexchange.data.conversion.ical.ical4j.internal.calendar.Participants;
import com.openexchange.data.conversion.ical.ical4j.internal.calendar.Recurrence;
import com.openexchange.data.conversion.ical.ical4j.internal.calendar.ReplyParticipants;
import com.openexchange.data.conversion.ical.ical4j.internal.calendar.RequestParticipants;
import com.openexchange.data.conversion.ical.ical4j.internal.calendar.Sequence;
import com.openexchange.data.conversion.ical.ical4j.internal.calendar.Start;
import com.openexchange.data.conversion.ical.ical4j.internal.calendar.Title;
import com.openexchange.data.conversion.ical.ical4j.internal.calendar.Uid;
import com.openexchange.groupware.container.Appointment;

/**
 * @author Francisco Laguna <francisco.laguna@open-xchange.com>
 */
public final class AppointmentConverters {
    public static final AttributeConverter<VEvent, Appointment>[] ALL;

    public static final AttributeConverter<VEvent, Appointment>[] REQUEST;

    public static final AttributeConverter<VEvent, Appointment>[] REPLY;
    
    public static final AttributeConverter<VEvent, Appointment>[] CANCEL;

    /**
     * Prevent instantiation.
     */
    private AppointmentConverters() {
        super();
    }

    static {
        final List<AttributeConverter<VEvent, Appointment>> tmp = new ArrayList<AttributeConverter<VEvent, Appointment>>();
        tmp.add(new Title<VEvent, Appointment>());
        tmp.add(new Note<VEvent, Appointment>());

        final Start<VEvent, Appointment> start = new Start<VEvent, Appointment>();
        start.setVerifier(new RequireStartDate());
        tmp.add(start);

        tmp.add(new End<VEvent, Appointment>());

        final Duration<VEvent, Appointment> duration = new Duration<VEvent, Appointment>();
        duration.setVerifier(new RequireEndDate());
        tmp.add(duration);

        tmp.add(new Klass<VEvent, Appointment>());

        tmp.add(new Location());
        tmp.add(new Transparency());

        tmp.add(new Categories<VEvent, Appointment>());

        tmp.add(new Recurrence<VEvent, Appointment>());
        tmp.add(new DeleteExceptions());
        tmp.add(new ChangeExceptions());

        tmp.add(new Alarm<VEvent, Appointment>());
        tmp.add(new IgnoreConflicts());
        tmp.add(new Uid<VEvent, Appointment>());

        tmp.add(new CreatedAndDTStamp<VEvent, Appointment>());
        tmp.add(new LastModified<VEvent, Appointment>());

        tmp.add(new CreatedBy<VEvent, Appointment>());
        tmp.add(new Sequence<VEvent, Appointment>());
        
        
        // All standard converters
        final List<AttributeConverter<VEvent, Appointment>> all = new ArrayList<AttributeConverter<VEvent, Appointment>>(tmp);
        final Participants<VEvent, Appointment> participants = new Participants<VEvent, Appointment>();
        participants.setVerifier(new PrivateAppointmentsHaveNoParticipants());
        all.add(participants);
        ALL = all.toArray(new AttributeConverter[all.size()]);
        
        // Special Participant Converters for IMip
        final List<AttributeConverter<VEvent, Appointment>> request = new ArrayList<AttributeConverter<VEvent, Appointment>>(tmp);
        final RequestParticipants<VEvent, Appointment> requestParticipants = new RequestParticipants<VEvent, Appointment>();
        request.add(requestParticipants);
        REQUEST = request.toArray(new AttributeConverter[request.size()]);
        
        final List<AttributeConverter<VEvent, Appointment>> reply = new ArrayList<AttributeConverter<VEvent, Appointment>>(tmp);
        final ReplyParticipants<VEvent, Appointment> replyParticipants = new ReplyParticipants<VEvent, Appointment>();
        reply.add(replyParticipants);
        REPLY = reply.toArray(new AttributeConverter[reply.size()]);

        CANCEL = ALL;
    }
}
