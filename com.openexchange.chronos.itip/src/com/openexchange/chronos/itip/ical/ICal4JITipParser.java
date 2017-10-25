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

package com.openexchange.chronos.itip.ical;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.ical.ICalService;
import com.openexchange.chronos.ical.ImportedCalendar;
import com.openexchange.chronos.itip.ITipMessage;
import com.openexchange.chronos.itip.ITipMethod;
import com.openexchange.chronos.itip.ITipSpecialHandling;
import com.openexchange.chronos.itip.osgi.Services;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;

/**
 * {@link ICal4JITipParser}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class ICal4JITipParser {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ICal4JITipParser.class);

    public List<ITipMessage> parseMessage(String icalText, TimeZone defaultTZ, int owner) throws OXException {
        try {
            return parseMessage(new ByteArrayInputStream(icalText.getBytes("UTF-8")), defaultTZ, owner);
        } catch (UnsupportedEncodingException e) {
            LOG.error("", e);
        }
        return Collections.emptyList();
    }

    public List<ITipMessage> parseMessage(InputStream ical, TimeZone defaultTZ, int owner) throws OXException {
        List<ITipMessage> messages = new ArrayList<ITipMessage>();
        Map<String, ITipMessage> messagesPerUID = new HashMap<String, ITipMessage>();
        ICalService iCalService = Services.getService(ICalService.class);
        ImportedCalendar calendar = iCalService.importICal(ical, iCalService.initParameters());

        boolean microsoft = looksLikeMicrosoft(calendar);

        ITipMethod methodValue = (calendar.getMethod() == null) ? ITipMethod.NO_METHOD : ITipMethod.get(calendar.getMethod());

        for (Event event : calendar.getEvents()) {
            ITipMessage message = messagesPerUID.get(event.getUid());
            if (message == null) {
                message = new ITipMessage();
                if (microsoft) {
                    message.addFeature(ITipSpecialHandling.MICROSOFT);
                }
                message.setMethod(methodValue);
                messagesPerUID.put(event.getUid(), message);
                messages.add(message);
            }

            if (owner > 0) {
                message.setOwner(owner);
            }

            if (event.containsRecurrenceId()) {
                message.addException(event);
            } else {
                message.setEvent(event);
            }

            // TODO: Comment
        }

        return messages;
    }

    private boolean looksLikeMicrosoft(ImportedCalendar calendar) {
        String property = calendar.getProdId();
        return null != property && Strings.toLowerCase(property).indexOf("microsoft") >= 0;
    }
}
