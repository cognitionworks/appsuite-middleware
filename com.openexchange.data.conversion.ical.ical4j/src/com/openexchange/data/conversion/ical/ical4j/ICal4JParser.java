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
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.property.Completed;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Due;
import net.fortuna.ical4j.util.CompatibilityHints;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.data.conversion.ical.ConversionError;
import com.openexchange.data.conversion.ical.ConversionWarning;
import com.openexchange.data.conversion.ical.ICalParser;
import com.openexchange.data.conversion.ical.ical4j.internal.AppointmentConverters;
import com.openexchange.data.conversion.ical.ical4j.internal.AttributeConverter;
import com.openexchange.data.conversion.ical.ical4j.internal.TaskConverters;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.tasks.Task;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * {@link ICal4JParser} - The {@link ICalParser} using <a href="http://ical4j.sourceforge.net/">ICal4j</a> library.
 *
 * @author Francisco Laguna <francisco.laguna@open-xchange.com>
 * @author Tobias Prinz <tobias.prinz@open-xchange.com> (bug workarounds)
 */
public class ICal4JParser implements ICalParser {

    private static final String UTF8 = "UTF-8";

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(ICal4JParser.class));

    private static final Map<String, Integer> WEEKDAYS = new HashMap<String, Integer>(7);
    static {
        WEEKDAYS.put("MO", Integer.valueOf(Appointment.MONDAY));
        WEEKDAYS.put("TU", Integer.valueOf(Appointment.TUESDAY));
        WEEKDAYS.put("WE", Integer.valueOf(Appointment.WEDNESDAY));
        WEEKDAYS.put("TH", Integer.valueOf(Appointment.THURSDAY));
        WEEKDAYS.put("FR", Integer.valueOf(Appointment.FRIDAY));
        WEEKDAYS.put("SA", Integer.valueOf(Appointment.SATURDAY));
        WEEKDAYS.put("SO", Integer.valueOf(Appointment.SUNDAY));
    }

    public ICal4JParser() {
        CompatibilityHints.setHintEnabled(
                CompatibilityHints.KEY_RELAXED_UNFOLDING, true);
        CompatibilityHints.setHintEnabled(
                CompatibilityHints.KEY_OUTLOOK_COMPATIBILITY, true);
        CompatibilityHints.setHintEnabled(
                CompatibilityHints.KEY_NOTES_COMPATIBILITY, true);
        CompatibilityHints.setHintEnabled(
        		CompatibilityHints.KEY_RELAXED_PARSING, true);
        CompatibilityHints.setHintEnabled(
              	CompatibilityHints.KEY_RELAXED_VALIDATION, true);

    }

    public List<CalendarDataObject> parseAppointments(final String icalText, final TimeZone defaultTZ, final Context ctx, final List<ConversionError> errors, final List<ConversionWarning> warnings) throws ConversionError {
        try {
            return parseAppointments(new ByteArrayInputStream(icalText.getBytes(UTF8)), defaultTZ, ctx, errors, warnings);
        } catch (final UnsupportedEncodingException e) {
            LOG.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public List<CalendarDataObject> parseAppointments(final InputStream ical, final TimeZone defaultTZ, final Context ctx, final List<ConversionError> errors, final List<ConversionWarning> warnings) throws ConversionError {
        final List<CalendarDataObject> appointments = new ArrayList<CalendarDataObject>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(ical, UTF8));

            while(true) {
                final net.fortuna.ical4j.model.Calendar calendar = parse(reader);
                if(calendar == null) { break; }
                int i = 0;
                for(final Object componentObj : calendar.getComponents("VEVENT")) {
                    final Component vevent = (Component) componentObj;
                    try {
                        appointments.add(convertAppointment(i++, (VEvent)vevent, defaultTZ, ctx, warnings ));
                    } catch (final ConversionError conversionError) {
                        errors.add(conversionError);
                    }
                }
            }

        } catch (final UnsupportedEncodingException e) {
            // IGNORE
        } catch (final ConversionError e){
        	errors.add(e);
        } finally {
            closeSafe(reader);
        }



        return appointments;
    }

    public String parseProperty(final String propertyName, final InputStream ical) {
        if (null == propertyName || null == ical) {
            return null;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(ical, UTF8));
            final net.fortuna.ical4j.model.Calendar calendar = parse(reader);
            if (calendar == null) {
                return null;
            }
            final Property property = calendar.getProperty(propertyName.toUpperCase(Locale.US));
            return null == property ? null : property.getValue();
        } catch (final UnsupportedEncodingException e) {
            // IGNORE
            return null;
        } catch (final ConversionError e){
            return null;
        } catch (final RuntimeException e){
            return null;
        } finally {
            closeSafe(reader);
        }
    }

    public List<Task> parseTasks(final String icalText, final TimeZone defaultTZ, final Context ctx, final List<ConversionError> errors, final List<ConversionWarning> warnings) throws ConversionError {
        try {
            return parseTasks(new ByteArrayInputStream(icalText.getBytes(UTF8)), defaultTZ, ctx, errors, warnings);
        } catch (final UnsupportedEncodingException e) {
            LOG.error(e.getMessage(), e);
        }
        return new LinkedList<Task>();
    }

    public List<Task> parseTasks(final InputStream ical, final TimeZone defaultTZ, final Context ctx, final List<ConversionError> errors, final List<ConversionWarning> warnings) throws ConversionError {
        final List<Task> tasks = new ArrayList<Task>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(ical, UTF8));
            while(true) {
                final net.fortuna.ical4j.model.Calendar calendar = parse(reader);
                if(calendar == null) { break; }
                int i = 0;
                for(final Object componentObj : calendar.getComponents("VTODO")) {
                    final Component vtodo = (Component) componentObj;
                    try {
                        tasks.add(convertTask(i++, (VToDo) vtodo, defaultTZ, ctx, warnings ));
                    } catch (final ConversionError conversionError) {
                        errors.add(conversionError);
                    }
                }
            }

        } catch (final UnsupportedEncodingException e) {
            // IGNORE
        } finally {
            closeSafe(reader);
        }
        return tasks;
    }

    private static void closeSafe(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final IOException e) {
                // Ignore
            }
        }
    }


    protected CalendarDataObject convertAppointment(final int index, final VEvent vevent, final TimeZone defaultTZ, final Context ctx, final List<ConversionWarning> warnings) throws ConversionError {

        final CalendarDataObject appointment = new CalendarDataObject();

        final TimeZone tz = determineTimeZone(vevent, defaultTZ);

        for (final AttributeConverter<VEvent, Appointment> converter : AppointmentConverters.ALL) {
            if (converter.hasProperty(vevent)) {
                converter.parse(index, vevent, appointment, tz, ctx, warnings);
            }
            converter.verify(index, appointment, warnings);
        }



        appointment.setTimezone(getTimeZoneID(tz));

        return appointment;
    }

    protected Task convertTask(final int index, final VToDo vtodo, final TimeZone defaultTZ, final Context ctx, final List<ConversionWarning> warnings) throws ConversionError{
        final TimeZone tz = determineTimeZone(vtodo, defaultTZ);
        final Task task = new Task();
        for (final AttributeConverter<VToDo, Task> converter : TaskConverters.ALL) {
            if (converter.hasProperty(vtodo)) {
                converter.parse(index, vtodo, task, tz, ctx, warnings);
            }
            converter.verify(index, task, warnings);
        }
        return task;
    }


    private static final TimeZone determineTimeZone(final CalendarComponent component,
        final TimeZone defaultTZ){
        for (final String name : new String[] { DtStart.DTSTART, DtEnd.DTEND, Due.DUE, Completed.COMPLETED }) {
            final DateProperty dateProp = (DateProperty) component.getProperty(name);
            if (dateProp != null) {
                return chooseTimeZone(dateProp, defaultTZ);
            }
        }

        return null;
    }

    private static final TimeZone chooseTimeZone(final DateProperty dateProperty, final TimeZone defaultTZ) {
        TimeZone tz = defaultTZ;
        if (dateProperty.isUtc()) {
            tz = TimeZone.getTimeZone("UTC");
        }
        final TimeZone inTZID = (null != dateProperty.getParameter("TZID")) ? TimeZone.getTimeZone(dateProperty.getParameter("TZID").getValue()) : null;
        if (null != inTZID) {
            tz = inTZID;
        }
        return tz;
    }

    private String getTimeZoneID(final TimeZone tz) {
        if(net.fortuna.ical4j.model.TimeZone.class.isAssignableFrom(tz.getClass())) {
            return "UTC";
        }
        if(tz.getID().equals("GMT")) { // Hack for VTIMEZONE. iCal4J sets timezone to GMT, though we prefer UTC
            return "UTC";
        }
        return tz.getID();
    }

    private net.fortuna.ical4j.model.Calendar parse(final BufferedReader reader) throws ConversionError {
        final CalendarBuilder builder = new CalendarBuilder();

        try {
            final StringBuilder chunk = new StringBuilder();
            String line;
            boolean read = false;
            boolean timezoneStarted = false; //hack to fix bug 11958
            boolean timezoneEnded = false; //hack to fix bug 11958
            boolean timezoneRead = false; //hack to fix bug 11958
            final StringBuilder timezoneInfo = new StringBuilder(); //hack to fix bug 11958
            // Copy until we find an END:VCALENDAR
            boolean beginFound = false;
            while((line = reader.readLine()) != null) {
            	if(!beginFound && line.endsWith("BEGIN:VCALENDAR")){
            		line = removeByteOrderMarks(line);
            	}
                if(line.startsWith("BEGIN:VCALENDAR")) {
                    beginFound = true;
                } else if ( !beginFound && !"".equals(line)) {
                    throw new ConversionError(-1, ConversionWarning.Code.DOES_NOT_LOOK_LIKE_ICAL_FILE);
                }
                if(!line.startsWith("END:VCALENDAR")){ //hack to fix bug 11958
                	if(line.matches("^\\s*BEGIN:VTIMEZONE")){
                		timezoneStarted = true;
                	}
                    if(!line.matches("\\s*")) {
                        read = true;
                        if(timezoneStarted && !timezoneEnded){ //hack to fix bug 11958
                        	timezoneInfo.append(line).append("\n");
                        } else {
                        	chunk.append(line).append("\n");
                        }
                    }
                	if(line.matches("^\\s*END:VTIMEZONE")){ //hack to fix bug 11958
                		timezoneEnded = true;
                		timezoneRead = true && timezoneStarted;
                	}
                } else {
                    break;
                }
            }
            if(!read) {  return null; }
            chunk.append("END:VCALENDAR\n");
            if(timezoneRead){
            	int locationForInsertion = chunk.indexOf("BEGIN:");
            	if(locationForInsertion > -1){
            		locationForInsertion = chunk.indexOf("BEGIN:", locationForInsertion + 1);
            		if(locationForInsertion > -1){
            			chunk.insert(locationForInsertion, timezoneInfo);
            		}
            	}
            }
            final StringReader chunkedReader = new StringReader(
            	workaroundFor19463(
            	workaroundFor16895(
            	workaroundFor16613(
            	workaroundFor16367(
            	workaroundFor17492(
            	workaroundFor17963(
            	removeAnnoyingWhitespaces(chunk.toString()
                )))))))
            ); // FIXME: Encoding?
            return builder.build(chunkedReader);
        } catch (final IOException e) {
            //IGNORE
        } catch (final ParserException e) {
            LOG.warn(e.getMessage(), e);
            throw new ConversionError(-1, ConversionWarning.Code.PARSE_EXCEPTION, e.getMessage());
        }
        return null;
    }

    private String workaroundFor17963(final String input) {
    	return input.replaceAll("EXDATE:(\\d+)([\\n\\r])", "EXDATE:$1T000000$2");
	}

	private String workaroundFor17492(final String input) {
    	return input.replaceAll(";SCHEDULE-AGENT=", ";X-CALDAV-SCHEDULE-AGENT=");
	}

	private String workaroundFor19463(final String input) {
		return input
			.replaceAll("TZOFFSETFROM:\\s*(\\d\\d\\d\\d)", "TZOFFSETFROM:+$1")
			.replaceAll("TZOFFSETTO:\\s*(\\d\\d\\d\\d)",   "TZOFFSETTO:+$1")
			;
	}

	/**
     * Method written out of laziness: Because you can spread iCal attributes
     * over several lines with newlines followed by a white space while a normal
     * newline means a new attribute starts, one would need to parse the whole file
     * (with a lookahead) before fixing errors. That means no regular expressions
     * allowed. Since spreading just makes it nicer to read for humans, this method
     * strips those newline+whitespace elements so we can use simple regexps.
     */
    private String removeAnnoyingWhitespaces(final String input) {
		return input.replaceAll("\n\\s+", "");
	}

	private String workaroundFor16895(final String input) {
		/* Bug in Zimbra: They like to use an EMAIL element for the
		 * ATTENDEE property, though there is none.
		 */
		return input.replaceAll("ATTENDEE([^\n]*?);EMAIL=", "ATTENDEE$1;X-ZIMBRA-EMAIL=");
	}

	private String workaroundFor16367(final String input) {
        /* Bug in MS Exchange: If you use a CN element, it must have a value.
         * MS Exchange has an empty value, which we now replace properly.
         */
        return input.replaceAll("CN=:", "CN=\"\":");
    }

    private String workaroundFor16613(final String input) {
        /*
         * Bug in Groupwise: There is no attribute ID for ATTACH. Experimental
         * ones are allowed, but they would start with X-GW for Groupwise.
         * We ignore those.
         */
        return input.replaceAll("\nATTACH(.*?);ID=(.+?)([:;])" , "\nATTACH$1$3");
    }

    private String removeByteOrderMarks(String line){
    	char[] buf = line.toCharArray();
    	int length = buf.length;

		if(length > 3) {
            if(Character.getNumericValue(buf[0]) < 0 && Character.getNumericValue(buf[1]) < 0 && Character.getNumericValue(buf[2]) < 0 && Character.getNumericValue(buf[3]) < 0){
				if(Character.getType(buf[0]) == 15 && Character.getType(buf[1]) == 15 && Character.getType(buf[2]) == 28 && Character.getType(buf[3]) == 28) {
                    return new String(Arrays.copyOfRange(buf, 3, length));
                }
				if(Character.getType(buf[0]) == 28 && Character.getType(buf[1]) == 28 && Character.getType(buf[2]) == 15 && Character.getType(buf[3]) == 15) {
                    return new String(Arrays.copyOfRange(buf, 3, length));
                }
			}
        }
		if(length > 1) {
            if(Character.getNumericValue(buf[0]) < 0 && Character.getNumericValue(buf[1]) < 0) {
                if(Character.getType(buf[0]) == 28 && Character.getType(buf[1]) == 28) {
                    return new String(Arrays.copyOfRange(buf, 2, length));
                }
            }
        }
		if(length > 0) {
            if(Character.getNumericValue(buf[0]) < 0) {
                if(Character.getType(buf[0]) == 16) {
                    return new String(Arrays.copyOfRange(buf, 1, length));
                }
            }
        }
		return line;
    }

}