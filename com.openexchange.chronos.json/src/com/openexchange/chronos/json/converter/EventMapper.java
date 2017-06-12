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

package com.openexchange.chronos.json.converter;

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.b;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.java.Autoboxing.l;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.Attachment;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Classification;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.EventStatus;
import com.openexchange.chronos.ExtendedProperties;
import com.openexchange.chronos.ExtendedProperty;
import com.openexchange.chronos.ExtendedPropertyParameter;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.TimeTransparency;
import com.openexchange.chronos.Trigger;
import com.openexchange.chronos.common.DefaultRecurrenceId;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.tools.mappings.json.BooleanMapping;
import com.openexchange.groupware.tools.mappings.json.DefaultJsonMapper;
import com.openexchange.groupware.tools.mappings.json.DefaultJsonMapping;
import com.openexchange.groupware.tools.mappings.json.IntegerMapping;
import com.openexchange.groupware.tools.mappings.json.JsonMapping;
import com.openexchange.groupware.tools.mappings.json.ListMapping;
import com.openexchange.groupware.tools.mappings.json.LongMapping;
import com.openexchange.groupware.tools.mappings.json.StringMapping;
import com.openexchange.groupware.tools.mappings.json.TimeMapping;
import com.openexchange.java.Enums;
import com.openexchange.session.Session;

/**
 * {@link EventMapper}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class EventMapper extends DefaultJsonMapper<Event, EventField> {

    private static final EventMapper INSTANCE = new EventMapper();

    private EventField[] mappedFields;

    /**
     * Gets the EventMapper instance.
     *
     * @return The EventMapper instance.
     */
    public static EventMapper getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes a new {@link EventMapper}.
     */
    private EventMapper() {
        super();
        this.mappedFields = mappings.keySet().toArray(newArray(mappings.keySet().size()));
    }

    public EventField[] getMappedFields() {
        return mappedFields;
    }

    @Override
    public Event newInstance() {
        return new Event();
    }

    @Override
    public EventField[] newArray(int size) {
        return new EventField[size];
    }

    @Override
    protected EnumMap<EventField, ? extends JsonMapping<? extends Object, Event>> createMappings() {
        EnumMap<EventField, JsonMapping<? extends Object, Event>> mappings = new
            EnumMap<EventField, JsonMapping<? extends Object, Event>>(EventField.class);
        mappings.put(EventField.ID, new StringMapping<Event>("id", ColumnIDs.ID) {

            @Override
            public boolean isSet(Event object) {
                return object.containsId();
            }

            @Override
            public void set(Event object, String value) throws OXException {
                object.setId(value);
            }

            @Override
            public String get(Event object) {
                return object.getId();
            }

            @Override
            public void remove(Event object) {
                object.removeId();
            }
        });
        mappings.put(EventField.FOLDER_ID, new StringMapping<Event>("folder", ColumnIDs.FOLDER_ID) {

            @Override
            public boolean isSet(Event object) {
                return object.containsFolderId();
            }

            @Override
            public void set(Event object, String value) throws OXException {
                object.setFolderId(value);
            }

            @Override
            public String get(Event object) {
                return object.getFolderId();
            }

            @Override
            public void remove(Event object) {
                object.removeFolderId();
            }
        });
        mappings.put(EventField.UID, new StringMapping<Event>("uid", ColumnIDs.UID) {

            @Override
            public boolean isSet(Event object) {
                return object.containsUid();
            }

            @Override
            public void set(Event object, String value) throws OXException {
                object.setUid(value);
            }

            @Override
            public String get(Event object) {
                return object.getUid();
            }

            @Override
            public void remove(Event object) {
                object.removeUid();
            }
        });
        mappings.put(EventField.FILENAME, new StringMapping<Event>("filename", ColumnIDs.FILENAME) {

            @Override
            public boolean isSet(Event object) {
                return object.containsFilename();
            }

            @Override
            public void set(Event object, String value) throws OXException {
                object.setFilename(value);
            }

            @Override
            public String get(Event object) {
                return object.getFilename();
            }

            @Override
            public void remove(Event object) {
                object.removeFilename();
            }
        });
        mappings.put(EventField.SEQUENCE, new IntegerMapping<Event>("sequence", ColumnIDs.SEQUENCE) {

            @Override
            public boolean isSet(Event object) {
                return object.containsSequence();
            }

            @Override
            public void set(Event object, Integer value) throws OXException {
                object.setSequence(null == value ? 0 : i(value));
            }

            @Override
            public Integer get(Event object) {
                return I(object.getSequence());
            }

            @Override
            public void remove(Event object) {
                object.removeSequence();
            }
        });
        mappings.put(EventField.CREATED, new TimeMapping<Event>("created", ColumnIDs.CREATED) {

            @Override
            public boolean isSet(Event object) {
                return object.containsCreated();
            }

            @Override
            public void set(Event object, Date value) throws OXException {
                object.setCreated(value);
            }

            @Override
            public Date get(Event object) {
                return object.getCreated();
            }

            @Override
            public void remove(Event object) {
                object.removeCreated();
            }
        });
        mappings.put(EventField.CREATED_BY, new IntegerMapping<Event>("createdBy", ColumnIDs.CREATED_BY) {

            @Override
            public boolean isSet(Event object) {
                return object.containsCreatedBy();
            }

            @Override
            public void set(Event object, Integer value) throws OXException {
                object.setCreatedBy(null == value ? 0 : i(value));
            }

            @Override
            public Integer get(Event object) {
                return I(object.getCreatedBy());
            }

            @Override
            public void remove(Event object) {
                object.removeCreatedBy();
            }
        });
        mappings.put(EventField.LAST_MODIFIED, new TimeMapping<Event>("lastModified", ColumnIDs.LAST_MODIFIED) {

            @Override
            public boolean isSet(Event object) {
                return object.containsLastModified();
            }

            @Override
            public void set(Event object, Date value) throws OXException {
                object.setLastModified(value);
            }

            @Override
            public Date get(Event object) {
                return object.getLastModified();
            }

            @Override
            public void remove(Event object) {
                object.removeLastModified();
            }
        });
        mappings.put(EventField.MODIFIED_BY, new IntegerMapping<Event>("modifiedBy", ColumnIDs.MODIFIED_BY) {

            @Override
            public boolean isSet(Event object) {
                return object.containsModifiedBy();
            }

            @Override
            public void set(Event object, Integer value) throws OXException {
                object.setModifiedBy(null == value ? 0 : i(value));
            }

            @Override
            public Integer get(Event object) {
                return I(object.getModifiedBy());
            }

            @Override
            public void remove(Event object) {
                object.removeModifiedBy();
            }
        });
        mappings.put(EventField.CALENDAR_USER, new IntegerMapping<Event>("calendarUser", ColumnIDs.CALENDAR_USER) {

            @Override
            public boolean isSet(Event object) {
                return object.containsCalendarUser();
            }

            @Override
            public void set(Event object, Integer value) throws OXException {
                object.setCalendarUser(null == value ? 0 : i(value));
            }

            @Override
            public Integer get(Event object) {
                return I(object.getCalendarUser());
            }

            @Override
            public void remove(Event object) {
                object.removeCalendarUser();
            }
        });
        mappings.put(EventField.SUMMARY, new StringMapping<Event>("summary", ColumnIDs.SUMMARY) {

            @Override
            public boolean isSet(Event object) {
                return object.containsSummary();
            }

            @Override
            public void set(Event object, String value) throws OXException {
                object.setSummary(value);
            }

            @Override
            public String get(Event object) {
                return object.getSummary();
            }

            @Override
            public void remove(Event object) {
                object.removeSummary();
            }
        });
        mappings.put(EventField.LOCATION, new StringMapping<Event>("location", ColumnIDs.LOCATION) {

            @Override
            public boolean isSet(Event object) {
                return object.containsLocation();
            }

            @Override
            public void set(Event object, String value) throws OXException {
                object.setLocation(value);
            }

            @Override
            public String get(Event object) {
                return object.getLocation();
            }

            @Override
            public void remove(Event object) {
                object.removeLocation();
            }
        });
        mappings.put(EventField.DESCRIPTION, new StringMapping<Event>("description", ColumnIDs.DESCRIPTION) {

            @Override
            public boolean isSet(Event object) {
                return object.containsDescription();
            }

            @Override
            public void set(Event object, String value) throws OXException {
                object.setDescription(value);
            }

            @Override
            public String get(Event object) {
                return object.getDescription();
            }

            @Override
            public void remove(Event object) {
                object.removeDescription();
            }
        });
        mappings.put(EventField.CATEGORIES, new ListMapping<String, Event>("categories", ColumnIDs.CATEGORIES) {

            @Override
            public boolean isSet(Event object) {
                return object.containsCategories();
            }

            @Override
            public void set(Event object, List<String> value) throws OXException {
                object.setCategories(value);
            }

            @Override
            public List<String> get(Event object) {
                return object.getCategories();
            }

            @Override
            public void remove(Event object) {
                object.removeDescription();
            }

            @Override
            protected String deserialize(JSONArray array, int index) throws JSONException, OXException {
                return array.getString(index);
            }
        });
        mappings.put(EventField.CLASSIFICATION, new StringMapping<Event>("class", ColumnIDs.CLASSIFICATION) {

            @Override
            public boolean isSet(Event object) {
                return object.containsClassification();
            }

            @Override
            public void set(Event object, String value) throws OXException {
                object.setClassification(null == value ? null : new Classification(value));
            }

            @Override
            public String get(Event object) {
                Classification value = object.getClassification();
                return null == value ? null : value.getValue();
            }

            @Override
            public void remove(Event object) {
                object.removeClassification();
            }
        });
        mappings.put(EventField.COLOR, new StringMapping<Event>("color", ColumnIDs.COLOR) {

            @Override
            public boolean isSet(Event object) {
                return object.containsColor();
            }

            @Override
            public void set(Event object, String value) throws OXException {
                object.setColor(value);
            }

            @Override
            public String get(Event object) {
                return object.getColor();
            }

            @Override
            public void remove(Event object) {
                object.removeColor();
            }
        });
        mappings.put(EventField.START_DATE, new TimeMapping<Event>("startDate", ColumnIDs.START_DATE) {

            @Override
            public boolean isSet(Event object) {
                return object.containsStartDate();
            }

            @Override
            public void set(Event object, Date value) throws OXException {
                object.setStartDate(value);
            }

            @Override
            public Date get(Event object) {
                return object.getStartDate();
            }

            @Override
            public void remove(Event object) {
                object.removeStartDate();
            }
        });
        mappings.put(EventField.START_TIMEZONE, new StringMapping<Event>("startTimezone", ColumnIDs.START_TIMEZONE) {

            @Override
            public boolean isSet(Event object) {
                return object.containsStartTimeZone();
            }

            @Override
            public void set(Event object, String value) throws OXException {
                object.setStartTimeZone(value);
            }

            @Override
            public String get(Event object) {
                return object.getStartTimeZone();
            }

            @Override
            public void remove(Event object) {
                object.removeStartTimeZone();
            }
        });
        mappings.put(EventField.END_DATE, new TimeMapping<Event>("endDate", ColumnIDs.END_DATE) {

            @Override
            public boolean isSet(Event object) {
                return object.containsEndDate();
            }

            @Override
            public void set(Event object, Date value) throws OXException {
                object.setEndDate(value);
            }

            @Override
            public Date get(Event object) {
                return object.getEndDate();
            }

            @Override
            public void remove(Event object) {
                object.removeEndDate();
            }
        });
        mappings.put(EventField.END_TIMEZONE, new StringMapping<Event>("endTimezone", ColumnIDs.END_TIMEZONE) {

            @Override
            public boolean isSet(Event object) {
                return object.containsEndTimeZone();
            }

            @Override
            public void set(Event object, String value) throws OXException {
                object.setEndTimeZone(value);
            }

            @Override
            public String get(Event object) {
                return object.getEndTimeZone();
            }

            @Override
            public void remove(Event object) {
                object.removeEndTimeZone();
            }
        });
        mappings.put(EventField.ALL_DAY, new BooleanMapping<Event>("allDay", ColumnIDs.ALL_DAY) {

            @Override
            public boolean isSet(Event object) {
                return object.containsAllDay();
            }

            @Override
            public void set(Event object, Boolean value) throws OXException {
                object.setAllDay(null == value ? false : b(value));
            }

            @Override
            public Boolean get(Event object) {
                return B(object.getAllDay());
            }

            @Override
            public void remove(Event object) {
                object.removeAllDay();
            }
        });
        mappings.put(EventField.TRANSP, new StringMapping<Event>("transp", ColumnIDs.TRANSP) {

            @Override
            public boolean isSet(Event object) {
                return object.containsTransp();
            }

            @Override
            public void set(Event object, String value) throws OXException {
                object.setTransp(Enums.parse(TimeTransparency.class, value, TimeTransparency.OPAQUE));
            }

            @Override
            public String get(Event object) {
                return object.getTransp().getValue();
            }

            @Override
            public void remove(Event object) {
                object.removeTransp();
            }
        });
        mappings.put(EventField.SERIES_ID, new StringMapping<Event>("seriedId", ColumnIDs.SERIES_ID) {

            @Override
            public boolean isSet(Event object) {
                return object.containsSeriesId();
            }

            @Override
            public void set(Event object, String value) throws OXException {
                object.setSeriesId(value);
            }

            @Override
            public String get(Event object) {
                return object.getSeriesId();
            }

            @Override
            public void remove(Event object) {
                object.removeSeriesId();
            }
        });
        mappings.put(EventField.RECURRENCE_RULE, new StringMapping<Event>("rrule", ColumnIDs.RECURRENCE_RULE) {

            @Override
            public boolean isSet(Event object) {
                return object.containsRecurrenceRule();
            }

            @Override
            public void set(Event object, String value) throws OXException {
                object.setRecurrenceRule(value);
            }

            @Override
            public String get(Event object) {
                return object.getRecurrenceRule();
            }

            @Override
            public void remove(Event object) {
                object.removeRecurrenceRule();
            }
        });
        mappings.put(EventField.RECURRENCE_ID, new LongMapping<Event>("recurrenceId", ColumnIDs.RECURRENCE_ID) {

            @Override
            public boolean isSet(Event object) {
                return object.containsRecurrenceId();
            }

            @Override
            public void set(Event object, Long value) throws OXException {
                object.setRecurrenceId(null == value ? null : new DefaultRecurrenceId(l(value)));
            }

            @Override
            public Long get(Event object) {
                RecurrenceId value = object.getRecurrenceId();
                return null == value ? null : L(value.getValue());
            }

            @Override
            public void remove(Event object) {
                object.removeRecurrenceId();
            }
        });
        mappings.put(EventField.CHANGE_EXCEPTION_DATES, new ListMapping<Long, Event>("changeExceptionDates", ColumnIDs.CHANGE_EXCEPTION_DATES) {

            @Override
            public boolean isSet(Event object) {
                return object.containsChangeExceptionDates();
            }

            @Override
            public void set(Event object, List<Long> value) throws OXException {
                if (null == value) {
                    object.setChangeExceptionDates(null);
                } else {
                    SortedSet<RecurrenceId> recurrenceIds = new TreeSet<RecurrenceId>();
                    for (Long timestamp : value) {
                        recurrenceIds.add(new DefaultRecurrenceId(timestamp));
                    }
                    object.setChangeExceptionDates(recurrenceIds);
                }
            }

            @Override
            public List<Long> get(Event object) {
                SortedSet<RecurrenceId> recurrenceIds = object.getChangeExceptionDates();
                if (null == recurrenceIds) {
                    return null;
                }
                List<Long> value = new ArrayList<Long>(recurrenceIds.size());
                for (RecurrenceId recurrenceId : recurrenceIds) {
                    value.add(L(recurrenceId.getValue()));
                }
                return value;
            }

            @Override
            public void remove(Event object) {
                object.removeChangeExceptionDates();
            }

            @Override
            protected Long deserialize(JSONArray array, int index) throws JSONException, OXException {
                return L(array.getLong(index));
            }
        });
        mappings.put(EventField.DELETE_EXCEPTION_DATES, new ListMapping<Long, Event>("deleteExceptionDates", ColumnIDs.DELETE_EXCEPTION_DATES) {

            @Override
            public boolean isSet(Event object) {
                return object.containsDeleteExceptionDates();
            }

            @Override
            public void set(Event object, List<Long> value) throws OXException {
                if (null == value) {
                    object.setDeleteExceptionDates(null);
                } else {
                    SortedSet<RecurrenceId> recurrenceIds = new TreeSet<RecurrenceId>();
                    for (Long timestamp : value) {
                        recurrenceIds.add(new DefaultRecurrenceId(timestamp));
                    }
                    object.setDeleteExceptionDates(recurrenceIds);
                }
            }

            @Override
            public List<Long> get(Event object) {
                SortedSet<RecurrenceId> recurrenceIds = object.getDeleteExceptionDates();
                if (null == recurrenceIds) {
                    return null;
                }
                List<Long> value = new ArrayList<Long>(recurrenceIds.size());
                for (RecurrenceId recurrenceId : recurrenceIds) {
                    value.add(L(recurrenceId.getValue()));
                }
                return value;
            }

            @Override
            public void remove(Event object) {
                object.removeDeleteExceptionDates();
            }

            @Override
            protected Long deserialize(JSONArray array, int index) throws JSONException, OXException {
                return L(array.getLong(index));
            }
        });
        mappings.put(EventField.STATUS, new StringMapping<Event>("status", ColumnIDs.STATUS) {

            @Override
            public boolean isSet(Event object) {
                return object.containsStatus();
            }

            @Override
            public void set(Event object, String value) throws OXException {
                object.setStatus(Enums.parse(EventStatus.class, value));
            }

            @Override
            public String get(Event object) {
                EventStatus status = object.getStatus();
                return null == status ? null : status.name();
            }

            @Override
            public void remove(Event object) {
                object.removeStatus();
            }
        });
        mappings.put(EventField.ORGANIZER, new DefaultJsonMapping<Organizer, Event>("organizer", ColumnIDs.ORGANIZER) {

            @Override
            public boolean isSet(Event object) {
                return object.containsOrganizer();
            }

            @Override
            public void set(Event object, Organizer value) throws OXException {
                object.setOrganizer(value);
            }

            @Override
            public Organizer get(Event object) {
                return object.getOrganizer();
            }

            @Override
            public void remove(Event object) {
                object.removeOrganizer();
            }

            @Override
            public void deserialize(JSONObject from, Event to) throws JSONException, OXException {
                set(to, deserializeCalendarUser(from, Organizer.class));
            }

            @Override
            public Object serialize(Event from, TimeZone timeZone, Session session) throws JSONException {
                return serializeCalendarUser(from.getOrganizer());
            }
        });
        mappings.put(EventField.ATTENDEES, new ListMapping<Attendee, Event>("attendees", ColumnIDs.ATTENDEES) {

            @Override
            public boolean isSet(Event object) {
                return object.containsAttendees();
            }

            @Override
            public void set(Event object, List<Attendee> value) throws OXException {
                object.setAttendees(value);
            }

            @Override
            public List<Attendee> get(Event object) {
                return object.getAttendees();
            }

            @Override
            public void remove(Event object) {
                object.removeAttendees();
            }

            @Override
            protected Attendee deserialize(JSONArray array, int index) throws JSONException, OXException {
                JSONObject jsonObject = array.getJSONObject(index);
                Attendee attendee = deserializeCalendarUser(jsonObject, Attendee.class);
                if (jsonObject.has("cuType")) {
                    attendee.setCuType(new CalendarUserType(jsonObject.getString("cuType")));
                }

                return attendee;
            }

            @Override
            public Object serialize(Event from, TimeZone timeZone, Session session) throws JSONException {
                List<Attendee> value = get(from);
                if (null == value) {
                    return null;
                }
                JSONArray jsonArray = new JSONArray(value.size());
                for (Attendee attendee : value) {
                    JSONObject jsonObject = serializeCalendarUser(attendee);
                    if (null != attendee.getCuType()) {
                        jsonObject.put("cuType", attendee.getCuType().getValue());
                    }
                    if (null != attendee.getRole()) {
                        jsonObject.put("role", attendee.getRole().getValue());
                    }
                    if (null != attendee.getPartStat()) {
                        jsonObject.put("partStat", attendee.getPartStat().getValue());
                    }
                    if (null != attendee.getComment()) {
                        jsonObject.put("comment", attendee.getComment());
                    }
                    if (null != attendee.getRsvp()) {
                        jsonObject.put("rsvp", attendee.getRsvp());
                    }
                    if (null != attendee.getFolderID()) {
                        jsonObject.put("folder", attendee.getFolderID());
                    }
                    if (null != attendee.getMember()) {
                        jsonObject.put("member", attendee.getMember());
                    }
                    jsonArray.put(jsonObject);
                }
                return jsonArray;
            }
        });
        mappings.put(EventField.ATTACHMENTS, new ListMapping<Attachment, Event>("attachments", ColumnIDs.ATTACHMENTS) {

            @Override
            public boolean isSet(Event object) {
                return object.containsAttachments();
            }

            @Override
            public void set(Event object, List<Attachment> value) throws OXException {
                object.setAttachments(value);
            }

            @Override
            public List<Attachment> get(Event object) {
                return object.getAttachments();
            }

            @Override
            public void remove(Event object) {
                object.removeAttachments();
            }

            @Override
            protected Attachment deserialize(JSONArray array, int index) throws JSONException, OXException {
                JSONObject jsonObject = array.getJSONObject(index);
                Attachment attachment = new Attachment();
                //TODO
                return attachment;
            }

            @Override
            public Object serialize(Event from, TimeZone timeZone, Session session) throws JSONException {
                List<Attachment> value = get(from);
                if (null == value) {
                    return null;
                }
                JSONArray jsonArray = new JSONArray(value.size());
                for (Attachment attachment : value) {
                    JSONObject jsonObject = new JSONObject();
                    if (null != attachment.getFilename()) {
                        jsonObject.put("filename", attachment.getFilename());
                    }
                    if (null != attachment.getFormatType()) {
                        jsonObject.put("fmtType", attachment.getFormatType());
                    }
                    if (0 < attachment.getSize()) {
                        jsonObject.put("size", attachment.getSize());
                    }
                    if (null != attachment.getCreated()) {
                        long date = attachment.getCreated().getTime();
                        date += timeZone.getOffset(date);
                        jsonObject.put("created", date);
                    }
                    if (0 < attachment.getManagedId()) {
                        jsonObject.put("managedId", attachment.getManagedId());
                    }
                    jsonArray.put(jsonObject);
                }
                return jsonArray;
            }
        });
        mappings.put(EventField.ALARMS, new ListMapping<Alarm, Event>("alarms", ColumnIDs.ALARMS) {

            @Override
            public boolean isSet(Event object) {
                return object.containsAlarms();
            }

            @Override
            public void set(Event object, List<Alarm> value) throws OXException {
                object.setAlarms(value);
            }

            @Override
            public List<Alarm> get(Event object) {
                return object.getAlarms();
            }

            @Override
            public void remove(Event object) {
                object.removeAlarms();
            }

            @Override
            protected Alarm deserialize(JSONArray array, int index) throws JSONException, OXException {
                JSONObject jsonObject = array.getJSONObject(index);
                Alarm alarm = new Alarm();
                //TODO
                return alarm;
            }

            @Override
            public Object serialize(Event from, TimeZone timeZone, Session session) throws JSONException {
                List<Alarm> value = get(from);
                if (null == value) {
                    return null;
                }
                JSONArray jsonArray = new JSONArray(value.size());
                for (Alarm alarm : value) {
                    JSONObject jsonObject = new JSONObject();
                    if (0 < alarm.getId()) {
                        jsonObject.put("id", alarm.getId());
                    }
                    if (null != alarm.getUid()) {
                        jsonObject.put("uid", alarm.getUid());
                    }
                    if (null != alarm.getAction()) {
                        jsonObject.put("action", alarm.getAction().getValue());
                    }
                    if (null != alarm.getAcknowledged()) {
                        long date = alarm.getAcknowledged().getTime();
                        date += timeZone.getOffset(date);
                        jsonObject.put("acknowledged", date);
                    }
                    if (null != alarm.getTrigger()) {
                        Trigger trigger = alarm.getTrigger();
                        JSONObject triggerJsonObject = new JSONObject();
                        if (null != trigger.getRelated()) {
                            triggerJsonObject.put("related", trigger.getRelated().name());
                        }
                        triggerJsonObject.putOpt("duration", trigger.getDuration());
                        if (null != trigger.getDateTime()) {
                            long date = trigger.getDateTime().getTime();
                            date += timeZone.getOffset(date);
                            jsonObject.put("dateTime", date);
                        }
                        jsonObject.put("trigger", triggerJsonObject);
                    }
                    if (null != alarm.getExtendedProperties()) {
                        jsonObject.put("extendedProperties", serializeExtendedProperties(alarm.getExtendedProperties()));
                    }
                    jsonArray.put(jsonObject);
                }
                return jsonArray;
            }
        });
        mappings.put(EventField.EXTENDED_PROPERTIES, new DefaultJsonMapping<ExtendedProperties, Event>("extendedProperties", ColumnIDs.EXTENDED_PROPERTIES) {

            @Override
            public boolean isSet(Event object) {
                return object.containsExtendedProperties();
            }

            @Override
            public void set(Event object, ExtendedProperties value) throws OXException {
                object.setExtendedProperties(value);
            }

            @Override
            public ExtendedProperties get(Event object) {
                return object.getExtendedProperties();
            }

            @Override
            public void remove(Event object) {
                object.removeExtendedProperties();
            }

            @Override
            public void deserialize(JSONObject from, Event to) throws JSONException, OXException {
                if (from.has(getAjaxName())) {
                    set(to, deserializeExtendedProperties(from.getJSONArray(getAjaxName())));
                }
            }

            @Override
            public Object serialize(Event from, TimeZone timeZone, Session session) throws JSONException {
                return serializeExtendedProperties(from.getExtendedProperties());
            }
        });

        return mappings;
    }

    private <T extends CalendarUser> T deserializeCalendarUser(JSONObject jsonObject, Class<T> calendarUserClass) throws JSONException {
        if (null == jsonObject) {
            return null;
        }
        T calendarUser;
        try {
            calendarUser = calendarUserClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new JSONException(e);
        }
        calendarUser.setUri(jsonObject.optString("uri", null));
        calendarUser.setCn(jsonObject.optString("cn", null));
        calendarUser.setSentBy(deserializeCalendarUser(jsonObject.optJSONObject("sentBy"), CalendarUser.class));
        if (jsonObject.has("entity")) {
            calendarUser.setEntity(jsonObject.getInt("entity"));
        }

        return calendarUser;
    }

    private static JSONObject serializeCalendarUser(CalendarUser calendarUser) throws JSONException {
        if (null == calendarUser) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOpt("uri", calendarUser.getUri());
        jsonObject.putOpt("cn", calendarUser.getCn());
        jsonObject.putOpt("sentBy", serializeCalendarUser(calendarUser.getSentBy()));
        if (0 < calendarUser.getEntity()) {
            jsonObject.put("entity", calendarUser.getEntity());
        }
        return jsonObject;
    }

    private static ExtendedProperties deserializeExtendedProperties(JSONArray jsonArray) throws JSONException {
        if (null == jsonArray) {
            return null;
        }
        ExtendedProperties extendedProperties = new ExtendedProperties();
        //TODO
        return extendedProperties;
    }

    private static JSONArray serializeExtendedProperties(ExtendedProperties extendedProperties) throws JSONException {
        if (null == extendedProperties) {
            return null;
        }
        JSONArray jsonArray = new JSONArray(extendedProperties.size());
        for (ExtendedProperty extendedProperty : extendedProperties) {
            jsonArray.put(serializeExtendedProperty(extendedProperty));
        }
        return jsonArray;
    }

    private static JSONObject serializeExtendedProperty(ExtendedProperty extendedProperty) throws JSONException {
        if (null == extendedProperty) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", extendedProperty.getName());
        jsonObject.put("value", extendedProperty.getValue());
        List<ExtendedPropertyParameter> parameters = extendedProperty.getParameters();
        if (null == parameters || parameters.isEmpty()) {
            return jsonObject;
        }
        JSONArray jsonParameters = new JSONArray(parameters.size());
        for (int i = 0; i < parameters.size(); i++) {
            ExtendedPropertyParameter parameter = parameters.get(i);
            jsonParameters.add(i, new JSONObject().putOpt("name", parameter.getName()).putOpt("value", parameter.getValue()));
        }
        jsonObject.put("parameters", jsonParameters);
        return jsonObject;
    }

}
