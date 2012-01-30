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

package com.openexchange.data.conversion.ical.ical4j;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Comment;
import net.fortuna.ical4j.model.property.Method;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.data.conversion.ical.ConversionError;
import com.openexchange.data.conversion.ical.ConversionWarning;
import com.openexchange.data.conversion.ical.ical4j.internal.AppointmentConverters;
import com.openexchange.data.conversion.ical.ical4j.internal.AttributeConverter;
import com.openexchange.data.conversion.ical.itip.ITipMessage;
import com.openexchange.data.conversion.ical.itip.ITipMethod;
import com.openexchange.data.conversion.ical.itip.ITipParser;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.contexts.Context;

/**
 * {@link ICal4JITipParser}
 * 
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class ICal4JITipParser extends ICal4JParser implements ITipParser {

    private static Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(ICal4JITipParser.class));

    public List<ITipMessage> parseMessage(String icalText, TimeZone defaultTZ, Context ctx, List<ConversionError> errors, List<ConversionWarning> warnings) throws ConversionError {
        try {
            return parseMessage(new ByteArrayInputStream(icalText.getBytes("UTF-8")), defaultTZ, ctx, errors, warnings);
        } catch (UnsupportedEncodingException e) {
            LOG.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public List<ITipMessage> parseMessage(InputStream ical, TimeZone defaultTZ, Context ctx, List<ConversionError> errors, List<ConversionWarning> warnings) throws ConversionError {
        List<ITipMessage> messages = new ArrayList<ITipMessage>();
        Map<String, ITipMessage> messagesPerUID = new HashMap<String, ITipMessage>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(ical, "UTF-8"));

            net.fortuna.ical4j.model.Calendar calendar = parse(reader);

            Method method = (Method) calendar.getProperty(Method.METHOD);
            ITipMethod methodValue = ITipMethod.get(method.getValue());
            
            List<AttributeConverter<VEvent, Appointment>> converters = AppointmentConverters.getConverters(methodValue);

            int i = 0;
            for (Object componentObj : calendar.getComponents(VEvent.VEVENT)) {
                Component vevent = (Component) componentObj;
                try {
                    CalendarDataObject appointment = convertAppointment(i++, (VEvent) vevent, defaultTZ, converters, ctx, warnings);
                    ITipMessage message = messagesPerUID.get(appointment.getUid());
                    if (message == null) {
                        message = new ITipMessage();
                        message.setMethod(methodValue);
                        messagesPerUID.put(appointment.getUid(), message);
                        messages.add(message);
                    }
                    if (appointment.containsRecurrenceDatePosition()) {
                        message.addException(appointment);
                    } else {
                        message.setAppointment(appointment);
                    }
                    if (vevent.getProperty(Comment.COMMENT) != null ) {
                        message.setComment(vevent.getProperty(Comment.COMMENT).getValue());
                    }
                    
                } catch (ConversionError conversionError) {
                    conversionError.printStackTrace();
                    errors.add(conversionError);
                }
            }

        } catch (UnsupportedEncodingException e) {
            // IGNORE
        } catch (ConversionError e) {
            errors.add(e);
        } finally {
            closeSafe(reader);
        }

        return messages;
    }

}
