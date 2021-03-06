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

package com.openexchange.chronos.scheduling.changes.impl.desc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.openexchange.annotation.NonNull;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.itip.Messages;
import com.openexchange.chronos.itip.generators.ArgumentType;
import com.openexchange.chronos.scheduling.changes.Description;
import com.openexchange.chronos.scheduling.changes.impl.ChangeDescriber;
import com.openexchange.chronos.scheduling.changes.impl.SentenceImpl;
import com.openexchange.chronos.service.EventUpdate;
import com.openexchange.java.Strings;

/**
 * {@link LocationDescriber}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.3
 */
public class LocationDescriber implements ChangeDescriber {

    /**
     * Initializes a new {@link LocationDescriber}.
     */
    public LocationDescriber() {
        super();
    }

    @Override
    @NonNull
    public EventField[] getFields() {
        return new EventField[] { EventField.GEO, EventField.LOCATION };
    }

    @Override
    public Description describe(EventUpdate eventUpdate) {
        boolean changedLocation = eventUpdate.getUpdatedFields().contains(EventField.LOCATION);
        boolean changedGeo = eventUpdate.getUpdatedFields().contains(EventField.GEO);
        if (false == changedLocation && false == changedGeo) {
            return null;
        }

        /*
         * Create data for the message, e.g. "Berlin, 52.520008, 13.404954"
         */
        StringBuilder sb = new StringBuilder();
        if (changedLocation && Strings.isNotEmpty(eventUpdate.getUpdate().getLocation())) {
            sb.append(eventUpdate.getUpdate().getLocation());
        }
        if (changedGeo) {
            String description = describeGeo(eventUpdate);
            if (Strings.isNotEmpty(description)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(description);
            }
        }

        /*
         * Check that we described a change. If not, Geo or location has been removed.
         */
        SentenceImpl sentence;
        if (sb.length() > 0) {
            sentence = new SentenceImpl(Messages.HAS_CHANGED_LOCATION).add(sb.toString(), ArgumentType.UPDATED);
        } else {
            sentence = new SentenceImpl(Messages.HAS_REMOVED_LOCATION);
        }
        return new DefaultDescription(Collections.singletonList(sentence), getEventFields(changedGeo, changedLocation));
    }

    private static String describeGeo(EventUpdate eventUpdate) {
        double[] coordinates = eventUpdate.getUpdate().getGeo();
        if (null != coordinates && coordinates.length == 2) {
            return Double.toString(coordinates[0]) + ", " + Double.toString(coordinates[1]);
        }
        return null;
    }

    private List<EventField> getEventFields(boolean changedGeo, boolean changedLocation) {
        ArrayList<EventField> fields = new ArrayList<EventField>(2);
        if (changedGeo) {
            fields.add(EventField.GEO);
        }
        if (changedLocation) {
            fields.add(EventField.LOCATION);
        }
        return fields;
    }

}
