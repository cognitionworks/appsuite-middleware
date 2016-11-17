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

package com.openexchange.chronos.compat;

import java.util.Date;
import com.openexchange.chronos.common.DataAwareRecurrenceId;
import com.openexchange.chronos.service.RecurrenceData;

/**
 * {@link PositionAwareRecurrenceId}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class PositionAwareRecurrenceId extends DataAwareRecurrenceId {

    private final int recurrencePosition;
    private final Date recurrenceDatePosition;

    /**
     * Initializes a new {@link PositionAwareRecurrenceId}.
     *
     * @param recurrenceData The underlying recurrence data of the corresponding series
     * @param value The recurrence-id value, represented as the number of milliseconds since January 1, 1970, 00:00:00 GMT
     * @param recurrencePosition The legacy, 1-based recurrence position
     * @param recurrenceDatePosition The legacy recurrence date position
     */
    public PositionAwareRecurrenceId(RecurrenceData recurrenceData, long value, int recurrencePosition, Date recurrenceDatePosition) {
        super(recurrenceData, value);
        this.recurrencePosition = recurrencePosition;
        this.recurrenceDatePosition = recurrenceDatePosition;
    }

    /**
     * Gets the formerly used recurrence position, i.e. the 1-based, sequential position in the series where the original occurrence
     * would have been.
     *
     * @return The recurrence position
     */
    public int getRecurrencePosition() {
        return recurrencePosition;
    }

    /**
     * Gets the formerly used recurrence date position, i.e. the date where the original occurrence would have been, as UTC date with
     * truncated time fraction.
     *
     * @return The legacy recurrence date position
     */
    public Date getRecurrenceDatePosition() {
        return recurrenceDatePosition;
    }

}
