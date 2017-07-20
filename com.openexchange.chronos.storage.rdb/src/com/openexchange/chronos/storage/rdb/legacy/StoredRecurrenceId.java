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

package com.openexchange.chronos.storage.rdb.legacy;

import java.util.TimeZone;
import org.dmfs.rfc5545.DateTime;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.storage.CalendarStorage;

/**
 * {@link CalendarStorage}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class StoredRecurrenceId implements RecurrenceId {

    private final int recurrencePosition;

    /**
     * Initializes a new {@link StoredRecurrenceId}.
     *
     * @param recurrencePosition The legacy, 1-based recurrence position
     */
    public StoredRecurrenceId(int recurrencePosition) {
        super();
        this.recurrencePosition = recurrencePosition;
    }

    @Override
    public DateTime getValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the legacy, 1-based recurrence position
     *
     * @return The recurrence position
     */
    public int getRecurrencePosition() {
        return recurrencePosition;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + recurrencePosition;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StoredRecurrenceId other = (StoredRecurrenceId) obj;
        if (recurrencePosition != other.recurrencePosition)
            return false;
        return true;
    }

    @Override
    public int compareTo(RecurrenceId other) {
        if (null == other) {
            return 1;
        }
        if (StoredRecurrenceId.class.isInstance(other)) {
            return Integer.compare(getRecurrencePosition(), ((StoredRecurrenceId) other).getRecurrencePosition());
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(RecurrenceId other, TimeZone timeZone) {
        return compareTo(other);
    }

}
