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

package com.openexchange.chronos.ical.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.Availability;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.ExtendedProperties;
import com.openexchange.chronos.ExtendedProperty;
import com.openexchange.chronos.ExtendedPropertyParameter;
import com.openexchange.chronos.FreeBusyData;
import com.openexchange.chronos.ical.ICalExceptionCodes;
import com.openexchange.chronos.ical.ICalParameters;
import com.openexchange.chronos.ical.ImportedAlarm;
import com.openexchange.chronos.ical.ImportedCalendar;
import com.openexchange.chronos.ical.ImportedEvent;
import com.openexchange.chronos.ical.ical4j.ICal4JParser;
import com.openexchange.chronos.ical.ical4j.VCalendar;
import com.openexchange.chronos.ical.ical4j.mapping.ICalMapper;
import com.openexchange.exception.OXException;
import com.openexchange.java.Autoboxing;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.CalendarParser;
import net.fortuna.ical4j.data.CalendarParserFactory;
import net.fortuna.ical4j.data.FoldingWriter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.extensions.caldav.parameter.CalendarServerAttendeeRef;
import net.fortuna.ical4j.extensions.caldav.parameter.CalendarServerDtStamp;
import net.fortuna.ical4j.extensions.caldav.property.Acknowledged;
import net.fortuna.ical4j.extensions.caldav.property.AlarmAgent;
import net.fortuna.ical4j.extensions.caldav.property.CalendarServerAccess;
import net.fortuna.ical4j.extensions.caldav.property.CalendarServerAttendeeComment;
import net.fortuna.ical4j.extensions.caldav.property.CalendarServerPrivateComment;
import net.fortuna.ical4j.extensions.caldav.property.DefaultAlarm;
import net.fortuna.ical4j.extensions.caldav.property.Proximity;
import net.fortuna.ical4j.extensions.outlook.AllDayEvent;
import net.fortuna.ical4j.extensions.outlook.BusyStatus;
import net.fortuna.ical4j.extensions.property.WrCalName;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterFactoryImpl;
import net.fortuna.ical4j.model.ParameterFactoryRegistry;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactoryImpl;
import net.fortuna.ical4j.model.PropertyFactoryRegistry;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VAvailability;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VFreeBusy;
import net.fortuna.ical4j.model.parameter.XParameter;
import net.fortuna.ical4j.model.property.XProperty;

/**
 * {@link ICalUtils}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class ICalUtils {

    static final PropertyFactoryRegistry PROPERTY_FACTORY = initPropertyFactory();
    static final ParameterFactoryRegistry PARAMETER_FACTORY = initParameterFactory();

    private static final byte[] VEVENT_PROLOGUE = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\n".getBytes(Charsets.UTF_8);
    private static final byte[] VEVENT_EPILOGUE = "END:VEVENT\r\nEND:VCALENDAR\r\n".getBytes(Charsets.UTF_8);
    private static final byte[] VALARM_PROLOGUE = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nBEGIN:VALARM\r\n".getBytes(Charsets.UTF_8);
    private static final byte[] VALARM_EPILOGUE = "END:VALARM\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n".getBytes(Charsets.UTF_8);

    static String optPropertyValue(Property property) {
        return null != property ? property.getValue() : null;
    }

    /**
     * Gets the iCal parameters, or the default parameters if passed instance is <code>null</code>.
     *
     * @param parameters The parameters as passed from the client
     * @return The parameters, or the default parameters if passed instance is <code>null</code>
     */
    public static ICalParameters getParametersOrDefault(ICalParameters parameters) {
        return null != parameters ? parameters : new ICalParametersImpl();
    }

    static Calendar importCalendar(InputStream iCalFile, ICalParameters parameters) throws OXException {
        ICalParameters iCalParameters = getParametersOrDefault(parameters);
        CalendarBuilder calendarBuilder = getCalendarBuilder(iCalParameters);
        Calendar calendar;
        try {
            if (Boolean.TRUE.equals(parameters.get(ICalParameters.SANITIZE_INPUT, Boolean.class))) {
                calendar = new ICal4JParser().parse(calendarBuilder, iCalFile);
            } else {
                calendar = calendarBuilder.build(iCalFile);
            }
        } catch (IOException e) {
            throw ICalExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (ParserException e) {
            throw asOXException(e);
        }
        if (null == calendar) {
            throw ICalExceptionCodes.NO_CALENDAR.create();
        }
        return calendar;
    }

    static ImportedCalendar importCalendar(InputStream iCalFile, ICalMapper mapper, ICalParameters parameters) throws OXException {
        ICalParameters iCalParameters = getParametersOrDefault(parameters);
        CalendarBuilder calendarBuilder = getCalendarBuilder(iCalParameters);
        Calendar calendar;
        try {
            if (Boolean.TRUE.equals(parameters.get(ICalParameters.SANITIZE_INPUT, Boolean.class))) {
                calendar = new ICal4JParser().parse(calendarBuilder, iCalFile);
            } else {
                calendar = calendarBuilder.build(iCalFile);
            }
        } catch (IOException e) {
            throw ICalExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (ParserException e) {
            throw asOXException(e);
        }
        if (null == calendar) {
            throw ICalExceptionCodes.NO_CALENDAR.create();
        }
        return importCalendar(new VCalendar(calendar), mapper, iCalParameters);
    }

    static ImportedCalendar importCalendar(VCalendar vCalendar, ICalMapper mapper, ICalParameters parameters) throws OXException {
        List<OXException> warnings = new ArrayList<OXException>();
        removeProperties(vCalendar, parameters.get(ICalParameters.IGNORED_PROPERTIES, String[].class));
        ImportedCalendar importedCalendar = new ImportedCalendar(mapper.importVCalendar(vCalendar, parameters, warnings), warnings);
        importedCalendar.setEvents(importEvents(vCalendar.getEvents(), mapper, parameters));
        importedCalendar.setFreeBusyDatas(importFreeBusys(vCalendar.getFreeBusys(), mapper, parameters));
        importedCalendar.setAvailabilities(importAvailability(vCalendar.getAvailability(), mapper, parameters));
        return importedCalendar;
    }

    static List<Event> importEvents(ComponentList eventComponents, ICalMapper mapper, ICalParameters parameters) throws OXException {
        if (null == eventComponents) {
            return null;
        }
        List<Event> events = new ArrayList<Event>(eventComponents.size());
        int index = 0;
        for (Iterator<?> iterator = eventComponents.iterator(); iterator.hasNext();) {
            events.add(importEvent(index, (VEvent) iterator.next(), mapper, parameters));
        }
        return events;
    }

    static Event importEvent(int index, VEvent vEvent, ICalMapper mapper, ICalParameters parameters) throws OXException {
        List<OXException> warnings = new ArrayList<OXException>();
        removeProperties(vEvent, parameters.get(ICalParameters.IGNORED_PROPERTIES, String[].class));
        ImportedEvent importedEvent = new ImportedEvent(index, mapper.importVEvent(vEvent, parameters, warnings), warnings);
        importedEvent.setAlarms(importAlarms(vEvent.getAlarms(), mapper, parameters));
        return importedEvent;
    }

    static List<Alarm> importAlarms(ComponentList alarmComponents, ICalMapper mapper, ICalParameters parameters) throws OXException {
        if (null == alarmComponents || alarmComponents.isEmpty()) {
            return null;
        }
        List<Alarm> alarms = new ArrayList<Alarm>(alarmComponents.size());
        int index = 0;
        for (Iterator<?> iterator = alarmComponents.iterator(); iterator.hasNext(); index++) {
            alarms.add(importAlarm(index, (VAlarm) iterator.next(), mapper, parameters));
        }
        return alarms;
    }

    static ImportedAlarm importAlarm(int index, VAlarm vAlarm, ICalMapper mapper, ICalParameters parameters) throws OXException {
        List<OXException> warnings = new ArrayList<OXException>();
        removeProperties(vAlarm, parameters.get(ICalParameters.IGNORED_PROPERTIES, String[].class));
        ImportedAlarm importedAlarm = new ImportedAlarm(index, mapper.importVAlarm(vAlarm, parameters, warnings), warnings);
        return importedAlarm;
    }

    static List<FreeBusyData> importFreeBusys(ComponentList freeBusyComponents, ICalMapper mapper, ICalParameters parameters) throws OXException {
        if (null == freeBusyComponents) {
            return null;
        }
        List<FreeBusyData> alarms = new ArrayList<FreeBusyData>(freeBusyComponents.size());
        int index = 0;
        for (Iterator<?> iterator = freeBusyComponents.iterator(); iterator.hasNext(); index++) {
            alarms.add(importFreeBusy(index, (VFreeBusy) iterator.next(), mapper, parameters));
        }
        return alarms;
    }

    /**
     * Imports the specified {@link VAvailability} components
     * 
     * @param availabilityComponent A {@link ComponentList} with all the {@link VAvailability} components
     * @param mapper The {@link ICalMapper}
     * @param parameters The {@link ICalParameters}
     * @return A {@link List} with the imported {@link Availability} components
     * @throws OXException if an error is occurred
     */
    static Availability importAvailability(Component availabilityComponent, ICalMapper mapper, ICalParameters parameters) throws OXException {
        if (null == availabilityComponent) {
            return null;
        }
        return importAvailability((VAvailability) availabilityComponent, mapper, parameters);
    }

    static FreeBusyData importFreeBusy(int index, VFreeBusy vFreeBusy, ICalMapper mapper, ICalParameters parameters) throws OXException {
        List<OXException> warnings = new ArrayList<OXException>();
        removeProperties(vFreeBusy, parameters.get(ICalParameters.IGNORED_PROPERTIES, String[].class));
        return mapper.importVFreeBusy(vFreeBusy, parameters, warnings);
    }

    /**
     * Imports the specified {@link VAvailability} component
     * 
     * @param vAvailability The {@link VAvailability} to import
     * @param mapper The {@link ICalMapper} to use
     * @param parameters The {@link ICalParameters}
     * @return The imported {@link VAvailability} component as {@link Availability}
     * @throws OXException if a parsing error occurs
     */
    static Availability importAvailability(VAvailability vAvailability, ICalMapper mapper, ICalParameters parameters) throws OXException {
        List<OXException> warnings = new ArrayList<OXException>();
        removeProperties(vAvailability, parameters.get(ICalParameters.IGNORED_PROPERTIES, String[].class));
        return mapper.importVAvailability(vAvailability, parameters, warnings);
    }

    static ThresholdFileHolder exportCalendar(Calendar calendar, ICalParameters parameters) throws OXException {
        ThresholdFileHolder fileHolder = new ThresholdFileHolder();
        exportCalendar(calendar, parameters, fileHolder.asOutputStream());
        return fileHolder;
    }

    static void exportCalendar(Calendar calendar, ICalParameters parameters, OutputStream outputStream) throws OXException {
        CalendarOutputter outputter = new CalendarOutputter(false);
        try {
            outputter.output(calendar, outputStream);
        } catch (IOException e) {
            throw ICalExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (ValidationException e) {
            throw asOXException(e);
        }
    }

    static void exportCalendar(VCalendar vCalendar, ICalParameters parameters, OutputStream outputStream) throws OXException {
        CalendarOutputter outputter = new CalendarOutputter(false);
        try {
            outputter.output(vCalendar.getCalendar(), outputStream);
        } catch (IOException e) {
            throw ICalExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (ValidationException e) {
            throw asOXException(e);
        }
    }

    static ThresholdFileHolder exportComponent(Component component, ICalParameters parameters) throws OXException {
        ThresholdFileHolder fileHolder = new ThresholdFileHolder();
        FoldingWriter writer = null;
        try {
            writer = new FoldingWriter(new OutputStreamWriter(fileHolder.asOutputStream(), Charsets.UTF_8), FoldingWriter.REDUCED_FOLD_LENGTH);
            PropertyList properties = component.getProperties();
            for (Iterator<?> iterator = properties.iterator(); iterator.hasNext();) {
                writer.write(iterator.next().toString());
            }
        } catch (IOException e) {
            throw new OXException(e);
        } finally {
            Streams.close(writer);
        }
        return fileHolder;
    }

    /**
     * Exports a collection of extended properties to the target calendar component.
     *
     * @param extendedProperties The extended properties to export
     * @param component The component to export the properties to
     */
    private static void exportProperties(ExtendedProperties extendedProperties, Component component) {
        for (Property property : exportProperties(extendedProperties)) {
            component.getProperties().add(property);
        }
    }

    private static List<Property> exportProperties(List<ExtendedProperty> extendedProperties) {
        if (null == extendedProperties || 0 == extendedProperties.size()) {
            return Collections.emptyList();
        }
        List<Property> properties = new ArrayList<Property>(extendedProperties.size());
        for (ExtendedProperty extendedProperty : extendedProperties) {
            properties.add(exportProperty(extendedProperty));
        }
        return properties;
    }

    static Property exportProperty(ExtendedProperty extendedProperty) {
        return new XProperty(extendedProperty.getName(), exportParameters(extendedProperty.getParameters()), extendedProperty.getValue());
    }

    private static ParameterList exportParameters(List<ExtendedPropertyParameter> propertyParameters) {
        ParameterList parameterList = new ParameterList();
        if (null != propertyParameters && 0 < propertyParameters.size()) {
            for (ExtendedPropertyParameter propertyParameter : propertyParameters) {
                parameterList.add(new XParameter(propertyParameter.getName(), propertyParameter.getValue()));
            }
        }
        return parameterList;
    }

    static <T extends Component> T removeProperties(T component, String[] propertyNames) {
        if (null == propertyNames || 0 == propertyNames.length) {
            return component;
        }
        for (String propertyName : propertyNames) {
            PropertyList propertyList = getProperties(component, propertyName);
            for (Iterator<?> iterator = propertyList.iterator(); iterator.hasNext();) {
                component.getProperties().remove((Property) iterator.next());
            }
        }
        return component;
    }

    private static List<ExtendedProperty> importProperties(Component component, String[] propertyNames) {
        if (null == propertyNames || 0 == propertyNames.length) {
            return Collections.emptyList();
        }
        List<ExtendedProperty> extendedProperties = new ArrayList<ExtendedProperty>(propertyNames.length);
        for (String propertyName : propertyNames) {
            extendedProperties.addAll(importProperties(getProperties(component, propertyName)));
        }
        return extendedProperties;
    }

    private static PropertyList getProperties(Component component, String propertyName) {
        if (-1 == propertyName.indexOf('*')) {
            return component.getProperties(propertyName);
        }
        Pattern pattern = Pattern.compile(Strings.wildcardToRegex(propertyName));
        PropertyList matchingProperties = new PropertyList();
        PropertyList properties = component.getProperties();
        for (int i = 0; i < properties.size(); i++) {
            Property property = (Property) properties.get(i);
            if (null != property.getName() && pattern.matcher(property.getName()).matches()) {
                matchingProperties.add(property);
            }
        }
        return matchingProperties;
    }

    private static List<ExtendedProperty> importProperties(PropertyList propertyList) {
        if (null == propertyList || 0 == propertyList.size()) {
            return Collections.emptyList();
        }
        List<ExtendedProperty> extendedProperties = new ArrayList<ExtendedProperty>(propertyList.size());
        for (Iterator<?> iterator = propertyList.iterator(); iterator.hasNext();) {
            Property property = (Property) iterator.next();
            extendedProperties.add(new ExtendedProperty(property.getName(), property.getValue(), importParameters(property.getParameters())));
        }
        return extendedProperties;
    }

    private static List<ExtendedPropertyParameter> importParameters(ParameterList parameterList) {
        if (null == parameterList || 0 == parameterList.size()) {
            return Collections.emptyList();
        }
        List<ExtendedPropertyParameter> propertyParameters = new ArrayList<ExtendedPropertyParameter>(parameterList.size());
        for (Iterator<?> iterator = parameterList.iterator(); iterator.hasNext();) {
            Parameter parameter = (Parameter) iterator.next();
            propertyParameters.add(new ExtendedPropertyParameter(parameter.getName(), parameter.getValue()));
        }
        return propertyParameters;
    }

    private static CalendarBuilder getCalendarBuilder(ICalParameters parameters) {
        ICalParameters iCalParameters = getParametersOrDefault(parameters);
        CalendarParser calendarParser = CalendarParserFactory.getInstance().createParser();
        TimeZoneRegistry timeZoneRegistry = iCalParameters.get(ICalParametersImpl.TIMEZONE_REGISTRY, TimeZoneRegistry.class);
        if (null == timeZoneRegistry) {
            timeZoneRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();
            iCalParameters.set(ICalParametersImpl.TIMEZONE_REGISTRY, timeZoneRegistry);
        }
        return new CalendarBuilder(calendarParser, PROPERTY_FACTORY, PARAMETER_FACTORY, timeZoneRegistry);
    }

    private static VEvent parseVEventComponent(IFileHolder fileHolder, ICalParameters parameters, List<OXException> warnings) throws OXException {
        try (InputStream inputStream = fileHolder.getStream()) {
            return parseVEventComponent(inputStream, parameters, warnings);
        } catch (IOException e) {
            throw new OXException(e);
        }
    }

    private static VEvent parseVEventComponent(InputStream inputStream, ICalParameters parameters, List<OXException> warnings) throws OXException {
        Enumeration<InputStream> streamSequence = Collections.enumeration(Arrays.asList(Streams.newByteArrayInputStream(VEVENT_PROLOGUE), inputStream, Streams.newByteArrayInputStream(VEVENT_EPILOGUE)));
        SequenceInputStream sequenceStream = null;
        Calendar calendar = null;
        CalendarBuilder calendarBuilder = getCalendarBuilder(parameters);
        try {
            sequenceStream = new SequenceInputStream(streamSequence);
            calendar = calendarBuilder.build(sequenceStream);
        } catch (IOException e) {
            throw new OXException(e);
        } catch (ParserException e) {
            throw asOXException(e);
        } finally {
            Streams.close(sequenceStream);
        }
        return (VEvent) calendar.getComponent(Component.VEVENT);
    }

    static VAlarm parseVAlarmComponent(IFileHolder fileHolder, ICalParameters parameters, List<OXException> warnings) throws OXException {
        try (InputStream inputStream = fileHolder.getStream()) {
            return parseVAlarmComponent(inputStream, parameters, warnings);
        } catch (IOException e) {
            throw new OXException(e);
        }
    }

    static VAlarm parseVAlarmComponent(InputStream inputStream, ICalParameters parameters, List<OXException> warnings) throws OXException {
        Enumeration<InputStream> streamSequence = Collections.enumeration(Arrays.asList(Streams.newByteArrayInputStream(VALARM_PROLOGUE), inputStream, Streams.newByteArrayInputStream(VALARM_EPILOGUE)));
        SequenceInputStream sequenceStream = null;
        Calendar calendar = null;
        CalendarBuilder calendarBuilder = getCalendarBuilder(parameters);
        try {
            sequenceStream = new SequenceInputStream(streamSequence);
            calendar = calendarBuilder.build(sequenceStream);
        } catch (IOException e) {
            throw new OXException(e);
        } catch (ParserException e) {
            throw asOXException(e);
        } finally {
            Streams.close(sequenceStream);
        }
        return (VAlarm) calendar.getComponent(Component.VALARM);
    }

    private static PropertyFactoryRegistry initPropertyFactory() {
        PropertyFactoryRegistry factory = new PropertyFactoryRegistry();
        factory.register(Acknowledged.PROPERTY_NAME, PropertyFactoryImpl.getInstance());
        factory.register(AlarmAgent.PROPERTY_NAME, AlarmAgent.FACTORY);
        factory.register(CalendarServerAccess.PROPERTY_NAME, CalendarServerAccess.FACTORY);
        factory.register(CalendarServerAttendeeComment.PROPERTY_NAME, CalendarServerAttendeeComment.FACTORY);
        factory.register(CalendarServerPrivateComment.PROPERTY_NAME, CalendarServerPrivateComment.FACTORY);
        factory.register(DefaultAlarm.PROPERTY_NAME, DefaultAlarm.FACTORY);
        factory.register(Proximity.PROPERTY_NAME, Proximity.FACTORY);
        factory.register(WrCalName.PROPERTY_NAME, WrCalName.FACTORY);
        factory.register(AllDayEvent.PROPERTY_NAME, AllDayEvent.FACTORY);
        factory.register(BusyStatus.PROPERTY_NAME, BusyStatus.FACTORY);
        return factory;
    }

    private static ParameterFactoryRegistry initParameterFactory() {
        ParameterFactoryRegistry factory = new ParameterFactoryRegistry();
        factory.register(CalendarServerAttendeeRef.PARAMETER_NAME, ParameterFactoryImpl.getInstance());
        factory.register(CalendarServerDtStamp.PARAMETER_NAME, ParameterFactoryImpl.getInstance());
        return factory;
    }

    private static OXException asOXException(ValidationException e) {
        return ICalExceptionCodes.VALIDATION_FAILED.create(e, e.getMessage());
    }

    private static OXException asOXException(ParserException e) {
        String message = e.getMessage();
        if (null != message) {
            message = message.replaceFirst("Error at line \\d+:", ""); // net.fortuna.ical4j.data.ParserException.ERROR_MESSAGE_PATTERN
        }
        return ICalExceptionCodes.PARSER_ERROR.create(e, Autoboxing.I(e.getLineNo()), message);
    }

}
