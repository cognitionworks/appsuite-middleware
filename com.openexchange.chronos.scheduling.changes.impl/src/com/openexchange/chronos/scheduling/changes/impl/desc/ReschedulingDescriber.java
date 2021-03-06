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

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import org.dmfs.rfc5545.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.annotation.NonNull;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.common.mapping.EventMapper;
import com.openexchange.chronos.itip.Messages;
import com.openexchange.chronos.itip.generators.ArgumentType;
import com.openexchange.chronos.scheduling.changes.Description;
import com.openexchange.chronos.scheduling.changes.impl.ChangeDescriber;
import com.openexchange.chronos.scheduling.changes.impl.FormattableArgument;
import com.openexchange.chronos.scheduling.changes.impl.MessageContext;
import com.openexchange.chronos.scheduling.changes.impl.SentenceImpl;
import com.openexchange.chronos.service.EventUpdate;
import com.openexchange.exception.OXException;
import com.openexchange.regional.RegionalSettingsUtil;

/**
 * {@link ReschedulingDescriber}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.3
 */
public class ReschedulingDescriber implements ChangeDescriber {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReschedulingDescriber.class);

    @Override
    @NonNull
    public EventField[] getFields() {
        return new EventField[] { EventField.START_DATE, EventField.END_DATE };
    }

    @Override
    public Description describe(EventUpdate eventUpdate) {
        List<SentenceImpl> sentences = new LinkedList<>();

        handleTimezoneChange(sentences, eventUpdate.getOriginal(), eventUpdate.getUpdate());

        if (hasTimeChanged(eventUpdate.getOriginal(), eventUpdate.getUpdate())) {
            handleRescheduling(sentences, eventUpdate);
        }

        if (sentences.isEmpty()) {
            return null;
        }
        return new DefaultDescription(sentences, Arrays.asList(getFields()));
    }

    /*
     * ----------------- Helpers -----------------
     */

    private void handleTimezoneChange(List<SentenceImpl> sentences, Event original, Event update) {
        String originalStartId = getTimezone(original, EventField.START_DATE);
        String originalEndId = getTimezone(original, EventField.END_DATE);
        String updatedStartId = getTimezone(update, EventField.START_DATE);
        String updatedEndId = getTimezone(update, EventField.END_DATE);

        // Both dates were the same and changed to the same?
        if (originalStartId.equals(originalEndId) && updatedStartId.equals(updatedEndId)) {
            if (false == originalStartId.equals(updatedStartId)) {
                sentences.add(new SentenceImpl(Messages.HAS_RESCHEDULED_TIMEZONE).add(originalStartId, ArgumentType.ORIGINAL).add(updatedStartId, ArgumentType.UPDATED));
            }
            // Nothing changed
            return;
        }
        // Separate start and end date sentences
        if (false == originalStartId.equals(updatedStartId)) {
            sentences.add(new SentenceImpl(Messages.HAS_RESCHEDULED_TIMEZONE_START_DATE).add(originalStartId, ArgumentType.ORIGINAL).add(updatedStartId, ArgumentType.UPDATED));
        }
        if (false == originalEndId.equals(updatedEndId)) {
            sentences.add(new SentenceImpl(Messages.HAS_RESCHEDULED_TIMEZONE_END_DATE).add(originalEndId, ArgumentType.ORIGINAL).add(updatedEndId, ArgumentType.UPDATED));
        }
    }

    private String getTimezone(Event event, EventField field) {
        try {
            DateTime date = (DateTime) EventMapper.getInstance().get(field).get(event);
            if (null != date && null != date.getTimeZone()) {
                return date.getTimeZone().getID();
            }
        } catch (OXException e) {
            LOGGER.debug("Unexcpected error", e);
        }
        return "UTC";
    }

    private static boolean hasTimeChanged(Event original, Event update) {
        return false == (original.getStartDate().getTimestamp() == update.getStartDate().getTimestamp() && original.getEndDate().getTimestamp() == update.getEndDate().getTimestamp());
    }

    private void handleRescheduling(List<SentenceImpl> sentences, EventUpdate eventUpdate) {
        Object originalDate = time(eventUpdate, eventUpdate.getOriginal());
        Object updatedDate = time(eventUpdate, eventUpdate.getUpdate());
        sentences.add(new SentenceImpl(Messages.HAS_RESCHEDULED).add(originalDate, ArgumentType.ORIGINAL).add(updatedDate, ArgumentType.UPDATED));
    }

    private Object time(EventUpdate eventUpdate, Event event) {
        return new FormattableArgument() {

            @Override
            public Object format(MessageContext context) {
                Date startDate = new Date(event.getStartDate().getTimestamp());
                Date endDate = new Date(event.getEndDate().getTimestamp());

                DateFormat longDate = RegionalSettingsUtil.getDateFormat(context.getRegionalSettings(), DateFormat.LONG, context.getLocale());
                longDate.setTimeZone(context.getTimeZone());
                if (event.getStartDate().isAllDay()) {
                    longDate.setTimeZone(TimeZone.getTimeZone("UTC"));
                    endDate = forceCorrectDay(endDate);
                }

                DateFormat time = RegionalSettingsUtil.getTimeFormat(context.getRegionalSettings(), DateFormat.SHORT, context.getLocale());
                time.setTimeZone(context.getTimeZone());

                if (isSameDay(eventUpdate, event)) {
                    if (event.getStartDate().isAllDay()) {
                        return String.format("%s (%s)", longDate.format(startDate), Messages.FULL_TIME);
                    }
                    return String.format("%s - %s", longDate.format(startDate) + " " + time.format(startDate), time.format(endDate));
                }
                if (event.getStartDate().isAllDay()) {
                    return String.format("%s - %s (%s)", longDate.format(startDate), longDate.format(endDate), Messages.FULL_TIME);
                }
                return String.format("%s - %s", longDate.format(startDate) + " " + time.format(startDate), longDate.format(endDate) + " " + time.format(endDate));
            }
        };
    }

    static Date forceCorrectDay(Date endDate) {
        return new Date(endDate.getTime() - 1000); // Move this before midnight, so the time formatting routines don't lie
    }

    static boolean isSameDay(EventUpdate eventUpdate, Event event) {

        if (eventUpdate.getUpdatedFields().contains(EventField.START_DATE)) {
            if (isDayChange(eventUpdate.getOriginal().getStartDate(), eventUpdate.getUpdate().getStartDate())) {
                return false;
            }
        }

        if (eventUpdate.getUpdatedFields().contains(EventField.END_DATE)) {
            if (isDayChange(eventUpdate.getOriginal().getEndDate(), eventUpdate.getUpdate().getEndDate())) {
                return false;
            }
        }

        if (isDayChange(event.getStartDate(), event.getEndDate())) {
            return false;
        }

        return true;
    }

    private static boolean isDayChange(DateTime original, DateTime update) {
        if (original.getYear() != update.getYear()) {
            return true;
        }

        if (original.getMonth() != update.getMonth()) {
            return true;
        }

        if (original.getDayOfMonth() != update.getDayOfMonth()) {
            return true;
        }

        return false;
    }

}
