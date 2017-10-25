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

package com.openexchange.chronos.itip;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.common.mapping.EventUpdateImpl;
import com.openexchange.chronos.itip.analyzers.AbstractITipAnalyzer;
import com.openexchange.chronos.itip.osgi.Services;
import com.openexchange.chronos.itip.tools.ITipEventUpdate;
import com.openexchange.chronos.service.EventConflict;
import com.openexchange.chronos.service.RecurrenceIterator;
import com.openexchange.chronos.service.RecurrenceService;
import com.openexchange.exception.OXException;

/**
 * 
 * {@link ITipChange}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.0
 */
public class ITipChange {

    public static enum Type {
        CREATE, UPDATE, DELETE, CREATE_DELETE_EXCEPTION;
    }

    private Type type;

    private Event currentEvent;

    private Event newEvent;

    private List<EventConflict> conflicts;

    private Event master;

    private Event deleted;

    private boolean isException = false;

    private ParticipantChange participantChange;

    private ITipEventUpdate diff;

    private List<String> diffDescription;

    private String introduction;

    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public Event getNewEvent() {
        return newEvent;
    }

    public void setNewEvent(Event newEvent) {
        this.newEvent = newEvent;
    }

    public Event getCurrentEvent() throws OXException {
        if (currentEvent == null) {
            if (isException && master != null && newEvent != null && newEvent.getRecurrenceId() != null) {
                // TODO: Calculate original ocurrence time for diff
                RecurrenceService recurrenceService = Services.getService(RecurrenceService.class);
                Calendar recurrenceId = GregorianCalendar.getInstance(newEvent.getRecurrenceId().getValue().getTimeZone());
                recurrenceId.setTimeInMillis(newEvent.getRecurrenceId().getValue().getTimestamp());
                int position = recurrenceService.calculateRecurrencePosition(master, recurrenceId);

                RecurrenceIterator<Event> recurrenceIterator = recurrenceService.iterateEventOccurrences(master, null, null);
                int count = 0;
                while (recurrenceIterator.hasNext()) {
                    count++;
                    if (count == position) {
                        return recurrenceIterator.next();
                    }
                }
            }
        }
        return currentEvent;
    }

    public void setCurrentEvent(Event currentEvent) {
        this.currentEvent = currentEvent;
    }

    public List<EventConflict> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<EventConflict> conflicts) {
        this.conflicts = conflicts;
    }

    public Event getMasterEvent() {
        return master;
    }

    public void setMaster(Event master) {
        this.master = master;
    }

    public Event getDeletedEvent() {
        return deleted;
    }

    public void setDeleted(Event deleted) {
        this.deleted = deleted;
    }

    public void setException(boolean b) {
        isException = b;
    }

    public boolean isException() {
        return isException;
    }

    public ParticipantChange getParticipantChange() {
        return participantChange;
    }

    public void setParticipantChange(ParticipantChange participantChange) {
        this.participantChange = participantChange;
    }

    public ITipEventUpdate getDiff() throws OXException {
        autodiff();
        return diff;
    }

    private void autodiff() throws OXException {
        if (currentEvent != null && newEvent != null && type == Type.UPDATE) {
            diff = new ITipEventUpdate(new EventUpdateImpl(currentEvent, newEvent, false, AbstractITipAnalyzer.SKIP));
        }

        if (isException && master != null && newEvent != null && type == Type.CREATE) {
            RecurrenceService recurrenceService = Services.getService(RecurrenceService.class);
            Calendar recurrenceId = GregorianCalendar.getInstance(newEvent.getRecurrenceId().getValue().getTimeZone());
            recurrenceId.setTimeInMillis(newEvent.getRecurrenceId().getValue().getTimestamp());
            int position = recurrenceService.calculateRecurrencePosition(master, recurrenceId);

            RecurrenceIterator<Event> recurrenceIterator = recurrenceService.iterateEventOccurrences(master, null, null);
            int count = 0;
            while (recurrenceIterator.hasNext()) {
                count++;
                if (count == position) {
                    Event occurrence = recurrenceIterator.next();
                    diff = new ITipEventUpdate(new EventUpdateImpl(occurrence, newEvent, false, AbstractITipAnalyzer.SKIP));
                }
            }
        }
    }

    public void setDiffDescription(List<String> diffDescription) {
        this.diffDescription = diffDescription;
    }

    public List<String> getDiffDescription() {
        return diffDescription;
    }

    public void setIntroduction(String message) {
        this.introduction = message;
    }

    public String getIntroduction() {
        return introduction;
    }

}
