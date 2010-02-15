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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.ajax.writer;

import static com.openexchange.java.Autoboxing.I;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.fields.AppointmentFields;
import com.openexchange.groupware.calendar.CalendarCollectionService;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.server.services.ServerServiceRegistry;

/**
 * {@link AppointmentWriter} - Writer for appointments
 *
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class AppointmentWriter extends CalendarWriter {

    private static final Log LOG = LogFactory.getLog(AppointmentWriter.class);

    private CalendarCollectionService calColl;

    /**
     * Initializes a new {@link AppointmentWriter}
     *
     * @param timeZone
     *            The user time zone
     */
    public AppointmentWriter(final TimeZone timeZone) {
        super(timeZone, null);
    }

    public CalendarCollectionService getCalendarCollectionService(){
        if ( null == calColl ) {
            calColl = ServerServiceRegistry.getInstance().getService(CalendarCollectionService.class);
        }
        return calColl;
    }

    public void setCalendarCollectionService(CalendarCollectionService calColl){
        this.calColl = calColl;
    }

    public void writeArray(final Appointment appointmentObj, final int cols[], final Date betweenStart,
            final Date betweenEnd, final JSONArray jsonArray) throws JSONException {
        if (appointmentObj.getFullTime() && betweenStart != null && betweenEnd != null) {
            if (getCalendarCollectionService().inBetween(appointmentObj.getStartDate().getTime(), appointmentObj.getEndDate()
                    .getTime(), betweenStart.getTime(), betweenEnd.getTime())) {
                writeArray(appointmentObj, cols, jsonArray);
            }
        } else {
            writeArray(appointmentObj, cols, jsonArray);
        }
    }

    public void writeArray(final Appointment appointment, final int[] columns, final JSONArray json) throws JSONException {
        final JSONArray array = new JSONArray();
        for (int column : columns) {
            writeField(appointment, column, timeZone, array);
        }
        json.put(array);
    }

    public void writeAppointment(final Appointment appointmentObject, final JSONObject jsonObj) throws JSONException {
        super.writeFields(appointmentObject, timeZone, jsonObj);
        if (appointmentObject.containsTitle()) {
            writeParameter(AppointmentFields.TITLE, appointmentObject.getTitle(), jsonObj);
        }
        final boolean isFullTime = appointmentObject.getFullTime();
        if (isFullTime) {
            writeParameter(AppointmentFields.START_DATE, appointmentObject.getStartDate(), jsonObj);
            writeParameter(AppointmentFields.END_DATE, appointmentObject.getEndDate(), jsonObj);
        } else {
            if (appointmentObject.getRecurrenceType() == Appointment.NO_RECURRENCE) {
                writeParameter(AppointmentFields.START_DATE, appointmentObject.getStartDate(), timeZone, jsonObj);
                writeParameter(AppointmentFields.END_DATE, appointmentObject.getEndDate(), timeZone, jsonObj);
            } else {
                writeParameter(AppointmentFields.START_DATE, appointmentObject.getStartDate(), appointmentObject
                        .getStartDate(), timeZone, jsonObj);
                writeParameter(AppointmentFields.END_DATE, appointmentObject.getEndDate(), appointmentObject
                        .getEndDate(), timeZone, jsonObj);
            }
        }
        if (appointmentObject.containsShownAs()) {
            writeParameter(AppointmentFields.SHOW_AS, appointmentObject.getShownAs(), jsonObj);
        }
        if (appointmentObject.containsLocation()) {
            writeParameter(AppointmentFields.LOCATION, appointmentObject.getLocation(), jsonObj);
        }
        if (appointmentObject.containsNote()) {
            writeParameter(AppointmentFields.NOTE, appointmentObject.getNote(), jsonObj);
        }
        if (appointmentObject.containsFullTime()) {
            writeParameter(AppointmentFields.FULL_TIME, appointmentObject.getFullTime(), jsonObj);
        }
        if (appointmentObject.containsAlarm()) {
            writeParameter(AppointmentFields.ALARM, appointmentObject.getAlarm(), jsonObj);
        }
        if (appointmentObject.containsRecurrenceType()) {
            writeRecurrenceParameter(appointmentObject, jsonObj);
        }
        if (appointmentObject.containsRecurrenceID()) {
            writeParameter(AppointmentFields.RECURRENCE_ID, appointmentObject.getRecurrenceID(), jsonObj);
        }
        if (appointmentObject.containsRecurrencePosition()) {
            writeParameter(AppointmentFields.RECURRENCE_POSITION, appointmentObject.getRecurrencePosition(), jsonObj);
        }
        if (appointmentObject.containsRecurrenceDatePosition()) {
            writeParameter(AppointmentFields.RECURRENCE_DATE_POSITION, appointmentObject.getRecurrenceDatePosition(),
                    jsonObj);
        }
        if (appointmentObject.containsParticipants()) {
            jsonObj.put(AppointmentFields.PARTICIPANTS, getParticipantsAsJSONArray(appointmentObject));
        }
        if (appointmentObject.containsUserParticipants()) {
            jsonObj.put(AppointmentFields.USERS, getUsersAsJSONArray(appointmentObject));
        }
        if (appointmentObject.getIgnoreConflicts()) {
            writeParameter(AppointmentFields.IGNORE_CONFLICTS, true, jsonObj);
        }
        if (appointmentObject.containsTimezone()) {
            writeParameter(AppointmentFields.TIMEZONE, appointmentObject.getTimezoneFallbackUTC(), jsonObj);
        }
        if (appointmentObject.containsRecurringStart()) {
            writeParameter(AppointmentFields.RECURRENCE_START, appointmentObject.getRecurringStart(), jsonObj);
        }
        if (appointmentObject instanceof CalendarDataObject && ((CalendarDataObject) appointmentObject).isHardConflict()) {
            writeParameter(AppointmentFields.HARD_CONFLICT, true, jsonObj);
        }
    }

    protected void writeField(Appointment appointment, int column, TimeZone tz, JSONArray json) throws JSONException {
        AppointmentFieldWriter writer = WRITER_MAP.get(I(column));
        if (null != writer) {
            writer.write(appointment, json);
            return;
        } else if (super.writeField(appointment, column, tz, json)) {
            return;
        }
        // No appropriate static writer found, write manually
        final boolean isFullTime = appointment.getFullTime();
        switch (column) {
        case Appointment.START_DATE:
            if (isFullTime) {
                writeValue(appointment.getStartDate(), json);
            } else {
                if (appointment.getRecurrenceType() == Appointment.NO_RECURRENCE) {
                    writeValue(appointment.getStartDate(), timeZone, json);
                } else {
                    writeValue(appointment.getStartDate(), appointment.getStartDate(), timeZone, json);
                }
            }
            break;
        case Appointment.END_DATE:
            if (isFullTime) {
                writeValue(appointment.getEndDate(), json);
            } else {
                if (appointment.getRecurrenceType() == Appointment.NO_RECURRENCE) {
                    writeValue(appointment.getEndDate(), timeZone, json);
                } else {
                    writeValue(appointment.getEndDate(), appointment.getEndDate(), timeZone, json);
                }
            }
            break;
        default:
            LOG.warn("Column " + column + " is unknown for appointment.");
        }
    }

    private static interface AppointmentFieldWriter {
        /**
         * Writes this writer's value taken from specified appointment object to
         * given JSON array
         *
         * @param appointmentObject
         *            The appointment object
         * @param jsonArray
         *            The JSON array
         * @throws JSONException
         *             If writing to JSON array fails
         */
        public void write(Appointment appointmentObject, JSONArray jsonArray) throws JSONException;
    }

    /*-
     * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     * ++++++++++++++++++++ INITIALIZATION OF FIELD WRITERS ++++++++++++++++++++
     * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     */

    private static final Map<Integer, AppointmentFieldWriter> WRITER_MAP;

    static {
        final Map<Integer, AppointmentFieldWriter> m = new HashMap<Integer, AppointmentFieldWriter>(24, 1);
        m.put(I(Appointment.TITLE), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getTitle(), jsonArray);
            }
        });
        m.put(I(Appointment.SHOWN_AS), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getShownAs(), jsonArray, appointmentObject.containsShownAs());
            }
        });
        m.put(I(Appointment.LOCATION), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getLocation(), jsonArray);
            }
        });
        m.put(I(Appointment.FULL_TIME), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getFullTime(), jsonArray, appointmentObject.containsFullTime());
            }
        });
        m.put(I(Appointment.NOTE), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getNote(), jsonArray);
            }
        });
        // modification for mobility support
        m.put(I(Appointment.RECURRENCE_ID), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getRecurrenceID(), jsonArray, appointmentObject.containsRecurrenceID());
            }
        });
        m.put(I(Appointment.RECURRENCE_TYPE), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getRecurrenceType(), jsonArray, appointmentObject.containsRecurrenceType());
            }
        });
        m.put(I(Appointment.INTERVAL), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getInterval(), jsonArray, appointmentObject.containsInterval());
            }
        });
        m.put(I(Appointment.DAYS), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getDays(), jsonArray, appointmentObject.containsDays());
            }
        });
        m.put(I(Appointment.DAY_IN_MONTH), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getDayInMonth(), jsonArray, appointmentObject.containsDayInMonth());
            }
        });
        m.put(I(Appointment.MONTH), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getMonth(), jsonArray, appointmentObject.containsMonth());
            }
        });
        m.put(I(Appointment.UNTIL), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                if (appointmentObject.containsOccurrence()) {
                    jsonArray.put(JSONObject.NULL);
                } else {
                    if (appointmentObject.containsUntil())
                        writeValue(appointmentObject.getUntil(), jsonArray);
                    else
                        jsonArray.put(JSONObject.NULL);
                }
            }
        });
        m.put(I(Appointment.RECURRENCE_COUNT), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getOccurrence(), jsonArray, appointmentObject.containsOccurrence());
            }
        });
        m.put(I(Appointment.RECURRENCE_DATE_POSITION), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getRecurrenceDatePosition(), jsonArray);
            }
        });
        m.put(I(Appointment.DELETE_EXCEPTIONS), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                final JSONArray jsonDeleteExceptionArray = getExceptionAsJSONArray(appointmentObject
                        .getDeleteException());
                if (jsonDeleteExceptionArray == null) {
                    jsonArray.put(JSONObject.NULL);
                } else {
                    jsonArray.put(jsonDeleteExceptionArray);
                }
            }
        });
        m.put(I(Appointment.CHANGE_EXCEPTIONS), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                final JSONArray jsonChangeExceptionArray = getExceptionAsJSONArray(appointmentObject
                        .getChangeException());
                if (jsonChangeExceptionArray == null) {
                    jsonArray.put(JSONObject.NULL);
                } else {
                    jsonArray.put(jsonChangeExceptionArray);
                }
            }
        });
        // end of modification for mobility support
        m.put(I(Appointment.RECURRENCE_POSITION), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getRecurrencePosition(), jsonArray, appointmentObject
                        .containsRecurrencePosition());
            }
        });
        m.put(I(Appointment.TIMEZONE), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getTimezoneFallbackUTC(), jsonArray);
            }
        });
        m.put(I(Appointment.RECURRENCE_START), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getRecurringStart(), jsonArray, appointmentObject.containsRecurringStart());
            }
        });
        m.put(I(Appointment.PARTICIPANTS), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray)
                    throws JSONException {
                final JSONArray jsonParticipantArray = getParticipantsAsJSONArray(appointmentObject);
                if (jsonParticipantArray == null) {
                    jsonArray.put(JSONObject.NULL);
                } else {
                    jsonArray.put(jsonParticipantArray);
                }
            }
        });
        m.put(I(Appointment.USERS), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray)
                    throws JSONException {
                final JSONArray jsonUserArray = getUsersAsJSONArray(appointmentObject);
                if (jsonUserArray == null) {
                    jsonArray.put(JSONObject.NULL);
                } else {
                    jsonArray.put(jsonUserArray);
                }
            }
        });
        m.put(I(Appointment.ALARM), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getAlarm(), jsonArray, appointmentObject.containsAlarm());
            }
        });
        m.put(I(Appointment.NOTIFICATION), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getNotification(), jsonArray, appointmentObject.getNotification());
            }
        });
        m.put(I(Appointment.RECURRENCE_CALCULATOR), new AppointmentFieldWriter() {
            public void write(Appointment appointmentObject, JSONArray jsonArray) {
                writeValue(appointmentObject.getRecurrenceCalculator(), jsonArray);
            }
        });
        m.put(I(Appointment.ORGANIZER), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getOrganizer(), jsonArray, appointmentObject.containsOrganizer());
            }
        });
        m.put(I(Appointment.UID), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getUid(), jsonArray, appointmentObject.containsUid());
            }
        });
        m.put(I(Appointment.SEQUENCE), new AppointmentFieldWriter() {
            public void write(final Appointment appointmentObject, final JSONArray jsonArray) {
                writeValue(appointmentObject.getSequence(), jsonArray, appointmentObject.containsSequence());
            }
        });
        WRITER_MAP = Collections.unmodifiableMap(m);
    }
}
