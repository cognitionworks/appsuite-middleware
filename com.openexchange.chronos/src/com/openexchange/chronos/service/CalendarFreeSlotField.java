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
 *     Copyright (C) 2017-2020 OX Software GmbH
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

package com.openexchange.chronos.service;

import java.util.EnumSet;

/**
 * {@link CalendarFreeSlotField}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public enum CalendarFreeSlotField implements CalendarAvailabilityField {

    id(true, false),
    calendarAvailabilityId(true, false),
    user(true, false),

    // the following are REQUIRED but MUST NOT occur more than once
    dtstamp(true, false),
    dtstart(true, false),
    uid(true, false),

    // the following are OPTIONAL but MUST NOT occur more than once
    created(false, false),
    description(false, false),
    lastModified(false, false),
    location(false, false),
    recurid(false, false),
    rrule(false, false),
    summary(false, false),
    dtend(false, false),
    duration(false, false),

    // the following are OPTIONAL and MAY occur more than once
    categories(false, true),
    comment(false, true),
    contact(false, true),
    exdate(false, true),
    rdate(false, true),
    extendedProperties(false, true),
    ianaProperties(false, true),
    ;

    private final boolean mandatory;
    private final boolean multiOccurrent;

    /**
     * Initialises a new {@link CalendarFreeSlotField}.
     * 
     * @param mandatory whether the field is mandatory
     * @param multiOccurrent whether the field can appear more than once
     */
    private CalendarFreeSlotField(boolean mandatory, boolean multiOccurrent) {
        this.mandatory = mandatory;
        this.multiOccurrent = multiOccurrent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.chronos.service.CalendarAvailabilityField#isMandatory()
     */
    @Override
    public boolean isMandatory() {
        return mandatory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.chronos.service.CalendarAvailabilityField#isMultiOccurrent()
     */
    @Override
    public boolean isMultiOccurrent() {
        return multiOccurrent;
    }

    /**
     * Returns the mandatory fields
     * 
     * @return The mandatory fields
     */
    public static EnumSet<CalendarFreeSlotField> getMandatoryFields() {
        return EnumSet.of(dtstart, dtstamp, uid);
    }

}
