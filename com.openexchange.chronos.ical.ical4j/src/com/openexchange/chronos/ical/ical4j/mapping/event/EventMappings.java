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

package com.openexchange.chronos.ical.ical4j.mapping.event;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.ical.ical4j.extensions.Conference;
import com.openexchange.chronos.ical.ical4j.mapping.ICalMapping;
import net.fortuna.ical4j.extensions.outlook.AllDayEvent;
import net.fortuna.ical4j.extensions.outlook.BusyStatus;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;

/**
 * {@link EventMappings}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class EventMappings {

	/**
	 * Holds a collection of all known event mappings.
	 */
	public static List<ICalMapping<VEvent, Event>> ALL = Collections.<ICalMapping<VEvent, Event>>unmodifiableList(Arrays.asList(
        new AttachmentMapping(),
		new AttendeeMapping(),
        new CategoriesMapping(),
        new ClassMapping(),
		new CreatedMapping(),
		new DescriptionMapping(),
        new DtEndMapping(),
		new DtStampMapping(),
		new DtStartMapping(),
        new DurationMapping(),
        new ExDateMapping(),
        new RDateMapping(),
        new GeoMapping(),
		new LastModifiedMapping(),
		new LocationMapping(),
		new OrganizerMapping(),
        new RecurrenceIdMapping(),
        new RelatedToMapping(),
        new RRuleMapping(),
		new SequenceMapping(),
		new StatusMapping(),
		new SummaryMapping(),
		new TranspMapping(),
        new UidMapping(),
        new UrlMapping(),
        new XMicrosoftAllDayEventMapping(),
        new XMicrosoftBusyStatusMapping(),
        new ConferencesMapping(),
        new ExtendedPropertiesMapping(
            Property.ATTACH, Property.ATTENDEE, Property.CATEGORIES, Property.CLASS, Property.CREATED, Property.DESCRIPTION,
            Property.DTEND, Property.DTSTAMP, Property.DTSTART, Property.DURATION, Property.EXDATE, Property.GEO, Property.LAST_MODIFIED,
            Property.LOCATION, Property.ORGANIZER, Property.RDATE, Property.RECURRENCE_ID, Property.RELATED_TO, Property.RRULE,
            Property.SEQUENCE, Property.STATUS, Property.SUMMARY, Property.TRANSP, Property.UID, Property.URL, AllDayEvent.PROPERTY_NAME,
            BusyStatus.PROPERTY_NAME, Conference.PROPERTY_NAME)
	));

    /**
     * Initializes a new {@link EventMappings}.
     */
	private EventMappings() {
		super();
	}

}
