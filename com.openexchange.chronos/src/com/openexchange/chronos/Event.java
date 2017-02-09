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

package com.openexchange.chronos;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * {@link Event}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 * @see <a href="https://tools.ietf.org/html/rfc5545#section-3.6.1">RFC 5545, section 3.6.1</a>
 */
public class Event {

    private int id;
    private int folderId;
    private int publicFolderId;
    private String uid;
    private String filename;

    private int sequence;
    private Date created;
    private int createdBy;
    private Date lastModified;
    private int modifiedBy;

    private String summary;
    private String location;
    private String description;
    private List<String> categories;
    private Classification classification;
    private String color;

    private Date startDate;
    private String startTimeZone;
    private Date endDate;
    private String endTimeZone;
    private boolean allDay;
    private Transp transp;

    private int seriesId;
    private String recurrenceRule;
    private RecurrenceId recurrenceId;
    private List<Date> changeExceptionDates;
    private List<Date> deleteExceptionDates;

    private EventStatus status;
    private Organizer organizer;
    private List<Attendee> attendees;

    private List<Attachment> attachments;
    private List<Alarm> alarms;

    private final EnumSet<EventField> setFields;

    /**
     * Initializes a new {@link Event}.
     */
    public Event() {
        super();
        this.setFields = EnumSet.noneOf(EventField.class);
    }

    /**
     * Gets a value indicating whether a specific property is set in the event or not.
     *
     * @param field The field to check
     * @return <code>true</code> if the field is set, <code>false</code>, otherwise
     */
    public boolean isSet(EventField field) {
        return setFields.contains(field);
    }

    /**
     * Gets the object identifier of the event.
     *
     * @return The object identifier
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the object identifier of the event.
     *
     * @param value The object identifier to set
     */
    public void setId(int value) {
        id = value;
        setFields.add(EventField.ID);
    }

    /**
     * Removes the object identifier of the event.
     */
    public void removeId() {
        id = 0;
        setFields.remove(EventField.ID);
    }

    /**
     * Gets a value indicating whether the object identifier of the event has been set or not.
     *
     * @return <code>true</code> if the object identifier is set, <code>false</code>, otherwise
     */
    public boolean containsId() {
        return isSet(EventField.ID);
    }

    /**
     * Gets the folder identifier of the event.
     *
     * @return The folder identifier
     */
    public int getFolderId() {
        return folderId;
    }

    /**
     * Sets the folder identifier of the event.
     *
     * @param value The folder identifier to set
     */
    public void setFolderId(int value) {
        folderId = value;
        setFields.add(EventField.FOLDER_ID);
    }

    /**
     * Removes the folder identifier of the event.
     */
    public void removeFolderId() {
        folderId = 0;
        setFields.remove(EventField.FOLDER_ID);
    }

    /**
     * Gets a value indicating whether the folder identifier of the event has been set or not.
     *
     * @return <code>true</code> if the folder identifier is set, <code>false</code>, otherwise
     */
    public boolean containsFolderId() {
        return isSet(EventField.FOLDER_ID);
    }

    /**
     * Gets the public folder identifier of the event.
     *
     * @return The public folder identifier
     */
    public int getPublicFolderId() {
        return publicFolderId;
    }

    /**
     * Sets the public folder identifier of the event.
     *
     * @param value The public folder identifier to set
     */
    public void setPublicFolderId(int value) {
        publicFolderId = value;
        setFields.add(EventField.PUBLIC_FOLDER_ID);
    }

    /**
     * Removes the public folder identifier of the event.
     */
    public void removePublicFolderId() {
        publicFolderId = 0;
        setFields.remove(EventField.PUBLIC_FOLDER_ID);
    }

    /**
     * Gets a value indicating whether the public folder identifier of the event has been set or not.
     *
     * @return <code>true</code> if the public folder identifier is set, <code>false</code>, otherwise
     */
    public boolean containsPublicFolderId() {
        return isSet(EventField.PUBLIC_FOLDER_ID);
    }

    /**
     * Gets the universal identifier of the event.
     *
     * @return The universal identifier
     */
    public String getUid() {
        return uid;
    }

    /**
     * Sets the universal identifier of the event.
     *
     * @param value The universal identifier to set
     */
    public void setUid(String value) {
        uid = value;
        setFields.add(EventField.UID);
    }

    /**
     * Removes the universal identifier of the event.
     */
    public void removeUid() {
        uid = null;
        setFields.remove(EventField.UID);
    }

    /**
     * Gets a value indicating whether the universal identifier of the event has been set or not.
     *
     * @return <code>true</code> if the universal identifier is set, <code>false</code>, otherwise
     */
    public boolean containsUid() {
        return setFields.contains(EventField.UID);
    }

    /**
     * Gets the filename of the event.
     *
     * @return The filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Sets the filename of the event.
     *
     * @param value The filename to set
     */
    public void setFilename(String value) {
        filename = value;
        setFields.add(EventField.FILENAME);
    }

    /**
     * Removes the filename of the event.
     */
    public void removeFilename() {
        filename = null;
        setFields.remove(EventField.FILENAME);
    }

    /**
     * Gets a value indicating whether the filename of the event has been set or not.
     *
     * @return <code>true</code> if the filename is set, <code>false</code>, otherwise
     */
    public boolean containsFilename() {
        return setFields.contains(EventField.FILENAME);
    }

    /**
     * Gets the sequence number of the event.
     *
     * @return The sequence number
     */
    public int getSequence() {
        return sequence;
    }

    /**
     * Sets the sequence number of the event.
     *
     * @param value The sequence number to set
     */
    public void setSequence(int value) {
        sequence = value;
        setFields.add(EventField.SEQUENCE);
    }

    /**
     * Removes the sequence number of the event.
     */
    public void removeSequence() {
        sequence = 0;
        setFields.remove(EventField.SEQUENCE);
    }

    /**
     * Gets a value indicating whether the sequence number of the event has been set or not.
     *
     * @return <code>true</code> if the sequence number is set, <code>false</code>, otherwise
     */
    public boolean containsSequence() {
        return isSet(EventField.SEQUENCE);
    }

    /**
     * Gets the creation date of the event.
     *
     * @return The creation date
     */
    public Date getCreated() {
        return created;
    }

    /**
     * Sets the creation date of the event.
     *
     * @param value The creation date to set
     */
    public void setCreated(Date value) {
        created = value;
        setFields.add(EventField.CREATED);
    }

    /**
     * Removes the creation date of the event.
     */
    public void removeCreated() {
        created = null;
        setFields.remove(EventField.CREATED);
    }

    /**
     * Gets a value indicating whether the creation date of the event has been set or not.
     *
     * @return <code>true</code> if the creation date is set, <code>false</code>, otherwise
     */
    public boolean containsCreated() {
        return isSet(EventField.CREATED);
    }

    /**
     * Gets the identifier of the user who has created the event.
     *
     * @return The identifier of the creator
     */
    public int getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the identifier of the user who has created the event.
     *
     * @param value The identifier of the creator to set
     */
    public void setCreatedBy(int value) {
        createdBy = value;
        setFields.add(EventField.CREATED_BY);
    }

    /**
     * Removes the identifier of the user who has created the event.
     */
    public void removeCreatedBy() {
        createdBy = 0;
        setFields.remove(EventField.CREATED_BY);
    }

    /**
     * Gets a value indicating whether the identifier of the user who has created the event has been set or not.
     *
     * @return <code>true</code> if the identifier of the creator is set, <code>false</code>, otherwise
     */
    public boolean containsCreatedBy() {
        return isSet(EventField.CREATED_BY);
    }

    /**
     * Gets the last modification date of the event.
     *
     * @return The last modification date
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * Sets the last modification date of the event.
     *
     * @param value The last modification date to set
     */
    public void setLastModified(Date value) {
        lastModified = value;
        setFields.add(EventField.LAST_MODIFIED);
    }

    /**
     * Removes the last modification date of the event.
     */
    public void removeLastModified() {
        lastModified = null;
        setFields.remove(EventField.LAST_MODIFIED);
    }

    /**
     * Gets a value indicating whether the last modification date of the event has been set or not.
     *
     * @return <code>true</code> if the last modification date is set, <code>false</code>, otherwise
     */
    public boolean containsLastModified() {
        return isSet(EventField.LAST_MODIFIED);
    }

    /**
     * Gets the identifier of the user who has last modified the event.
     *
     * @return The identifier of the last modifying user
     */
    public int getModifiedBy() {
        return modifiedBy;
    }

    /**
     * Sets the identifier of the user who has last modified the event.
     *
     * @param value The identifier of the last modifying user to set
     */
    public void setModifiedBy(int value) {
        modifiedBy = value;
        setFields.add(EventField.MODIFIED_BY);
    }

    /**
     * Removes the identifier of the user who has last modified the event.
     */
    public void removeModifiedBy() {
        modifiedBy = 0;
        setFields.remove(EventField.MODIFIED_BY);
    }

    /**
     * Gets a value indicating whether the identifier of the user who has last modified the event has been set or not.
     *
     * @return <code>true</code> if the identifier of the last modifying user is set, <code>false</code>, otherwise
     */
    public boolean containsModifiedBy() {
        return isSet(EventField.MODIFIED_BY);
    }

    /**
     * Gets the summary of the event.
     *
     * @return The summary
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Sets the summary of the event.
     *
     * @param value The summary to set
     */
    public void setSummary(String value) {
        summary = value;
        setFields.add(EventField.SUMMARY);
    }

    /**
     * Removes the summary of the event.
     */
    public void removeSummary() {
        summary = null;
        setFields.remove(EventField.SUMMARY);
    }

    /**
     * Gets a value indicating whether the summary of the event has been set or not.
     *
     * @return <code>true</code> if the summary is set, <code>false</code>, otherwise
     */
    public boolean containsSummary() {
        return setFields.contains(EventField.SUMMARY);
    }

    /**
     * Gets the location of the event.
     *
     * @return The location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the location of the event.
     *
     * @param value The location to set
     */
    public void setLocation(String value) {
        location = value;
        setFields.add(EventField.LOCATION);
    }

    /**
     * Removes the location of the event.
     */
    public void removeLocation() {
        location = null;
        setFields.remove(EventField.LOCATION);
    }

    /**
     * Gets a value indicating whether the location of the event has been set or not.
     *
     * @return <code>true</code> if the location is set, <code>false</code>, otherwise
     */
    public boolean containsLocation() {
        return setFields.contains(EventField.LOCATION);
    }

    /**
     * Gets the description of the event.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the event.
     *
     * @param value The description to set
     */
    public void setDescription(String value) {
        description = value;
        setFields.add(EventField.DESCRIPTION);
    }

    /**
     * Removes the description of the event.
     */
    public void removeDescription() {
        description = null;
        setFields.remove(EventField.DESCRIPTION);
    }

    /**
     * Gets a value indicating whether the description of the event has been set or not.
     *
     * @return <code>true</code> if the description is set, <code>false</code>, otherwise
     */
    public boolean containsDescription() {
        return setFields.contains(EventField.DESCRIPTION);
    }

    /**
     * Gets the categories of the event.
     *
     * @return The categories
     */
    public List<String> getCategories() {
        return categories;
    }

    /**
     * Sets the categories of the event.
     *
     * @param value The categories to set
     */
    public void setCategories(List<String> value) {
        categories = value;
        setFields.add(EventField.CATEGORIES);
    }

    /**
     * Removes the categories of the event.
     */
    public void removeCategories() {
        categories = null;
        setFields.remove(EventField.CATEGORIES);
    }

    /**
     * Gets a value indicating whether the categories of the event has been set or not.
     *
     * @return <code>true</code> if the categories is set, <code>false</code>, otherwise
     */
    public boolean containsCategories() {
        return setFields.contains(EventField.CATEGORIES);
    }

    /**
     * Gets the classification of the event.
     *
     * @return The classification
     */
    public Classification getClassification() {
        return classification;
    }

    /**
     * Sets the classification of the event.
     *
     * @param value The classification to set
     */
    public void setClassification(Classification value) {
        classification = value;
        setFields.add(EventField.CLASSIFICATION);
    }

    /**
     * Removes the classification of the event.
     */
    public void removeClassification() {
        classification = null;
        setFields.remove(EventField.CLASSIFICATION);
    }

    /**
     * Gets a value indicating whether the classification of the event has been set or not.
     *
     * @return <code>true</code> if the classification is set, <code>false</code>, otherwise
     */
    public boolean containsClassification() {
        return setFields.contains(EventField.CLASSIFICATION);
    }

    /**
     * Gets the color of the event.
     *
     * @return The color
     */
    public String getColor() {
        return color;
    }

    /**
     * Sets the color of the event.
     *
     * @param value The color to set
     */
    public void setColor(String value) {
        color = value;
        setFields.add(EventField.COLOR);
    }

    /**
     * Removes the color of the event.
     */
    public void removeColor() {
        color = null;
        setFields.remove(EventField.COLOR);
    }

    /**
     * Gets a value indicating whether the color of the event has been set or not.
     *
     * @return <code>true</code> if the color is set, <code>false</code>, otherwise
     */
    public boolean containsColor() {
        return setFields.contains(EventField.COLOR);
    }

    /**
     * Gets the start date of the event.
     *
     * @return The start date
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Sets the start date of the event.
     *
     * @param value The start date to set
     */
    public void setStartDate(Date value) {
        startDate = value;
        setFields.add(EventField.START_DATE);
    }

    /**
     * Removes the start date of the event.
     */
    public void removeStartDate() {
        startDate = null;
        setFields.remove(EventField.START_DATE);
    }

    /**
     * Gets a value indicating whether the start date of the event has been set or not.
     *
     * @return <code>true</code> if the start date is set, <code>false</code>, otherwise
     */
    public boolean containsStartDate() {
        return setFields.contains(EventField.START_DATE);
    }

    /**
     * Gets the start timezone of the event.
     *
     * @return The start timezone
     */
    public String getStartTimeZone() {
        return startTimeZone;
    }

    /**
     * Sets the start timezone of the event.
     *
     * @param value The start timezone to set
     */
    public void setStartTimeZone(String value) {
        startTimeZone = value;
        setFields.add(EventField.START_TIMEZONE);
    }

    /**
     * Removes the start timezone of the event.
     */
    public void removeStartTimeZone() {
        startTimeZone = null;
        setFields.remove(EventField.START_TIMEZONE);
    }

    /**
     * Gets a value indicating whether the start timezone of the event has been set or not.
     *
     * @return <code>true</code> if the start timezone is set, <code>false</code>, otherwise
     */
    public boolean containsStartTimeZone() {
        return setFields.contains(EventField.START_TIMEZONE);
    }

    /**
     * Gets the end date of the event.
     *
     * @return The end date
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Sets the end date of the event.
     *
     * @param value The end date to set
     */
    public void setEndDate(Date value) {
        endDate = value;
        setFields.add(EventField.END_DATE);
    }

    /**
     * Removes the end date of the event.
     */
    public void removeEndDate() {
        endDate = null;
        setFields.remove(EventField.END_DATE);
    }

    /**
     * Gets a value indicating whether the end date of the event has been set or not.
     *
     * @return <code>true</code> if the end date is set, <code>false</code>, otherwise
     */
    public boolean containsEndDate() {
        return setFields.contains(EventField.END_DATE);
    }

    /**
     * Gets the end timezone of the event.
     *
     * @return The end timezone
     */
    public String getEndTimeZone() {
        return endTimeZone;
    }

    /**
     * Sets the end timezone of the event.
     *
     * @param value The end timezone to set
     */
    public void setEndTimeZone(String value) {
        endTimeZone = value;
        setFields.add(EventField.END_TIMEZONE);
    }

    /**
     * Removes the end timezone of the event.
     */
    public void removeEndTimeZone() {
        endTimeZone = null;
        setFields.remove(EventField.END_TIMEZONE);
    }

    /**
     * Gets a value indicating whether the end timezone of the event has been set or not.
     *
     * @return <code>true</code> if the end timezone is set, <code>false</code>, otherwise
     */
    public boolean containsEndTimeZone() {
        return setFields.contains(EventField.END_TIMEZONE);
    }

    /**
     * Gets the all-day character of the event.
     *
     * @return The all-day character
     */
    public boolean getAllDay() {
        return allDay;
    }

    /**
     * Gets the all-day character of the event.
     *
     * @return The all-day character
     */
    public boolean isAllDay() {
        return allDay;
    }

    /**
     * Sets the all-day character of the event.
     *
     * @param value The all-day character to set
     */
    public void setAllDay(boolean value) {
        allDay = value;
        setFields.add(EventField.ALL_DAY);
    }

    /**
     * Removes the all-day character of the event.
     */
    public void removeAllDay() {
        allDay = false;
        setFields.remove(EventField.ALL_DAY);
    }

    /**
     * Gets a value indicating whether the all-day character of the event has been set or not.
     *
     * @return <code>true</code> if the all-day character is set, <code>false</code>, otherwise
     */
    public boolean containsAllDay() {
        return setFields.contains(EventField.ALL_DAY);
    }

    /**
     * Gets the time transparency of the event.
     *
     * @return The time transparency
     */
    public Transp getTransp() {
        return transp;
    }

    /**
     * Sets the time transparency of the event.
     *
     * @param value The time transparency to set
     */
    public void setTransp(Transp value) {
        transp = value;
        setFields.add(EventField.TRANSP);
    }

    /**
     * Removes the time transparency of the event.
     */
    public void removeTransp() {
        transp = null;
        setFields.remove(EventField.TRANSP);
    }

    /**
     * Gets a value indicating whether the time transparency of the event has been set or not.
     *
     * @return <code>true</code> if the time transparency is set, <code>false</code>, otherwise
     */
    public boolean containsTransp() {
        return setFields.contains(EventField.TRANSP);
    }

    /**
     * Gets the series identifier of the event.
     *
     * @return The series identifier
     */
    public int getSeriesId() {
        return seriesId;
    }

    /**
     * Sets the series identifier of the event.
     *
     * @param value The series identifier to set
     */
    public void setSeriesId(int value) {
        seriesId = value;
        setFields.add(EventField.SERIES_ID);
    }

    /**
     * Removes the series identifier of the event.
     */
    public void removeSeriesId() {
        seriesId = 0;
        setFields.remove(EventField.SERIES_ID);
    }

    /**
     * Gets a value indicating whether the series identifier of the event has been set or not.
     *
     * @return <code>true</code> if the series identifier is set, <code>false</code>, otherwise
     */
    public boolean containsSeriesId() {
        return setFields.contains(EventField.SERIES_ID);
    }

    /**
     * Gets the recurrence rule of the event.
     *
     * @return The recurrence rule
     */
    public String getRecurrenceRule() {
        return recurrenceRule;
    }

    /**
     * Sets the recurrence rule of the event.
     *
     * @param value The recurrence rule to set
     */
    public void setRecurrenceRule(String value) {
        recurrenceRule = value;
        setFields.add(EventField.RECURRENCE_RULE);
    }

    /**
     * Removes the recurrence rule of the event.
     */
    public void removeRecurrenceRule() {
        recurrenceRule = null;
        setFields.remove(EventField.RECURRENCE_RULE);
    }

    /**
     * Gets a value indicating whether the recurrence rule of the event has been set or not.
     *
     * @return <code>true</code> if the recurrence rule is set, <code>false</code>, otherwise
     */
    public boolean containsRecurrenceRule() {
        return setFields.contains(EventField.RECURRENCE_RULE);
    }

    /**
     * Gets the recurrence identifier of the event.
     *
     * @return The recurrence identifier
     */
    public RecurrenceId getRecurrenceId() {
        return recurrenceId;
    }

    /**
     * Sets the recurrence identifier of the event.
     *
     * @param value The recurrence identifier to set
     */
    public void setRecurrenceId(RecurrenceId value) {
        recurrenceId = value;
        setFields.add(EventField.RECURRENCE_ID);
    }

    /**
     * Removes the recurrence identifier of the event.
     */
    public void removeRecurrenceId() {
        recurrenceId = null;
        setFields.remove(EventField.RECURRENCE_ID);
    }

    /**
     * Gets a value indicating whether the recurrence identifier of the event has been set or not.
     *
     * @return <code>true</code> if the recurrence identifier is set, <code>false</code>, otherwise
     */
    public boolean containsRecurrenceId() {
        return setFields.contains(EventField.RECURRENCE_ID);
    }

    /**
     * Gets the change exception dates of the event.
     *
     * @return The change exception dates
     */
    public List<Date> getChangeExceptionDates() {
        return changeExceptionDates;
    }

    /**
     * Sets the change exception dates of the event.
     *
     * @param value The change exception dates to set
     */
    public void setChangeExceptionDates(List<Date> value) {
        changeExceptionDates = value;
        setFields.add(EventField.CHANGE_EXCEPTION_DATES);
    }

    /**
     * Removes the change exception dates of the event.
     */
    public void removeChangeExceptionDates() {
        changeExceptionDates = null;
        setFields.remove(EventField.CHANGE_EXCEPTION_DATES);
    }

    /**
     * Gets a value indicating whether the change exception dates of the event has been set or not.
     *
     * @return <code>true</code> if the change exception dates is set, <code>false</code>, otherwise
     */
    public boolean containsChangeExceptionDates() {
        return setFields.contains(EventField.CHANGE_EXCEPTION_DATES);
    }

    /**
     * Gets the delete exception dates of the event.
     *
     * @return The delete exception dates
     */
    public List<Date> getDeleteExceptionDates() {
        return deleteExceptionDates;
    }

    /**
     * Sets the delete exception dates of the event.
     *
     * @param value The delete exception dates to set
     */
    public void setDeleteExceptionDates(List<Date> value) {
        deleteExceptionDates = value;
        setFields.add(EventField.DELETE_EXCEPTION_DATES);
    }

    /**
     * Removes the delete exception dates of the event.
     */
    public void removeDeleteExceptionDates() {
        deleteExceptionDates = null;
        setFields.remove(EventField.DELETE_EXCEPTION_DATES);
    }

    /**
     * Gets a value indicating whether the delete exception dates of the event has been set or not.
     *
     * @return <code>true</code> if the delete exception dates is set, <code>false</code>, otherwise
     */
    public boolean containsDeleteExceptionDates() {
        return setFields.contains(EventField.DELETE_EXCEPTION_DATES);
    }

    /**
     * Gets the status of the event.
     *
     * @return The status
     */
    public EventStatus getStatus() {
        return status;
    }

    /**
     * Sets the status of the event.
     *
     * @param value The status to set
     */
    public void setStatus(EventStatus value) {
        status = value;
        setFields.add(EventField.STATUS);
    }

    /**
     * Removes the status of the event.
     */
    public void removeStatus() {
        status = null;
        setFields.remove(EventField.STATUS);
    }

    /**
     * Gets a value indicating whether the status of the event has been set or not.
     *
     * @return <code>true</code> if the status is set, <code>false</code>, otherwise
     */
    public boolean containsStatus() {
        return setFields.contains(EventField.STATUS);
    }

    /**
     * Gets the organizer of the event.
     *
     * @return The organizer
     */
    public Organizer getOrganizer() {
        return organizer;
    }

    /**
     * Sets the organizer of the event.
     *
     * @param value The organizer to set
     */
    public void setOrganizer(Organizer value) {
        organizer = value;
        setFields.add(EventField.ORGANIZER);
    }

    /**
     * Removes the organizer of the event.
     */
    public void removeOrganizer() {
        organizer = null;
        setFields.remove(EventField.ORGANIZER);
    }

    /**
     * Gets a value indicating whether the organizer of the event has been set or not.
     *
     * @return <code>true</code> if the organizer is set, <code>false</code>, otherwise
     */
    public boolean containsOrganizer() {
        return setFields.contains(EventField.ORGANIZER);
    }

    /**
     * Gets the attendees of the event.
     *
     * @return The attendees
     */
    public List<Attendee> getAttendees() {
        return attendees;
    }

    /**
     * Sets the attendees of the event.
     *
     * @param value The attendees to set
     */
    public void setAttendees(List<Attendee> value) {
        attendees = value;
        setFields.add(EventField.ATTENDEES);
    }

    /**
     * Removes the attendees of the event.
     */
    public void removeAttendees() {
        attendees = null;
        setFields.remove(EventField.ATTENDEES);
    }

    /**
     * Gets a value indicating whether the attendees of the event have been set or not.
     *
     * @return <code>true</code> if the attendees are set, <code>false</code>, otherwise
     */
    public boolean containsAttendees() {
        return setFields.contains(EventField.ATTENDEES);
    }

    /**
     * Gets the attachments of the event.
     *
     * @return The attachments
     */
    public List<Attachment> getAttachments() {
        return attachments;
    }

    /**
     * Sets the attachments of the event.
     *
     * @param value The attachments to set
     */
    public void setAttachments(List<Attachment> value) {
        attachments = value;
        setFields.add(EventField.ATTACHMENTS);
    }

    /**
     * Removes the attachments of the event.
     */
    public void removeAttachments() {
        attachments = null;
        setFields.remove(EventField.ATTACHMENTS);
    }

    /**
     * Gets a value indicating whether the attachments of the event have been set or not.
     *
     * @return <code>true</code> if the attachments are set, <code>false</code>, otherwise
     */
    public boolean containsAttachments() {
        return setFields.contains(EventField.ATTACHMENTS);
    }

    /**
     * Gets the alarms of the event.
     *
     * @return The alarms
     */
    public List<Alarm> getAlarms() {
        return alarms;
    }

    /**
     * Sets the alarms of the event.
     *
     * @param value The alarms to set
     */
    public void setAlarms(List<Alarm> value) {
        alarms = value;
        setFields.add(EventField.ALARMS);
    }

    /**
     * Removes the alarms of the event.
     */
    public void removeAlarms() {
        alarms = null;
        setFields.remove(EventField.ALARMS);
    }

    /**
     * Gets a value indicating whether the alarms of the event have been set or not.
     *
     * @return <code>true</code> if the alarms are set, <code>false</code>, otherwise
     */
    public boolean containsAlarms() {
        return setFields.contains(EventField.ALARMS);
    }

    /**
     * Returns the Start of the event as Calendar with appropriate TimeZone.
     *
     * @return The Start of the event
     */
    public Calendar getStart() {
        Calendar retval = GregorianCalendar.getInstance(TimeZone.getTimeZone(getStartTimeZone()));
        retval.setTime(getStartDate());
        return retval;
    }

    /**
     * Returns the End of the event as Calendar with appropriate TimeZone.
     *
     * @return The End of the event
     */
    public Calendar getEnd() {
        Calendar retval = GregorianCalendar.getInstance(TimeZone.getTimeZone(getStartTimeZone()));
        retval.setTime(getEndDate());
        return retval;
    }

    /**
     * Sets the timezone of the start and end date.
     *
     * @param timeZone The timezone
     */
    public void setTimeZone(String timeZone) {
        setStartTimeZone(timeZone);
        setEndTimeZone(timeZone);
    }

    /**
     * Returns the start timezone, ignoring whatever the end timezone ist.
     *
     * @return The start timezon
     */
    public String getTimeZone() {
        return getStartTimeZone();
    }

    /**
     * Removes the start and end timezone.
     */
    public void removeTimeZone() {
        removeStartTimeZone();
        removeEndTimeZone();
    }

    /**
     * Gets a value indicating whether the start and end timezone are set or not.
     *
     * @return
     */
    public boolean containsTimeZone() {
        return containsStartTimeZone() && containsEndTimeZone();
    }

    @Override
    public String toString() {
        return "Event [id=" + getId() + ", summary=" + getSummary() + ", startDate=" + getStartDate() + ", endDate=" + getEndDate() + "]";
    }

    @Override
    public Event clone() {
        Event clone = new Event();
        if (containsAllDay()) {
            clone.setAllDay(getAllDay());
        }
        if (containsAttachments()) {
            clone.setAttachments(cloneList(getAttachments()));
        }
        if (containsAttendees()) {
            clone.setAttendees(cloneList(getAttendees()));
        }
        if (containsAlarms()) {
            clone.setAlarms(cloneList(getAlarms()));
        }
        if (containsCategories()) {
            clone.setCategories(cloneList(getCategories()));
        }
        if (containsChangeExceptionDates()) {
            clone.setChangeExceptionDates(cloneList(getChangeExceptionDates()));
        }
        if (containsClassification()) {
            clone.setClassification(getClassification());
        }
        if (containsColor()) {
            clone.setColor(getColor());
        }
        if (containsCreated()) {
            clone.setCreated(getCreated());
        }
        if (containsCreatedBy()) {
            clone.setCreatedBy(getCreatedBy());
        }
        if (containsDeleteExceptionDates()) {
            clone.setDeleteExceptionDates(cloneList(getDeleteExceptionDates()));
        }
        if (containsDescription()) {
            clone.setDescription(getDescription());
        }
        if (containsEndDate()) {
            clone.setEndDate(getEndDate());
        }
        if (containsEndTimeZone()) {
            clone.setEndTimeZone(getEndTimeZone());
        }
        if (containsFilename()) {
            clone.setFilename(getFilename());
        }
        if (containsFolderId()) {
            clone.setFolderId(getFolderId());
        }
        if (containsId()) {
            clone.setId(getId());
        }
        if (containsLastModified()) {
            clone.setLastModified(getLastModified());
        }
        if (containsLocation()) {
            clone.setLocation(getLocation());
        }
        if (containsModifiedBy()) {
            clone.setModifiedBy(getModifiedBy());
        }
        if (containsOrganizer()) {
            clone.setOrganizer(getOrganizer());
        }
        if (containsPublicFolderId()) {
            clone.setPublicFolderId(getPublicFolderId());
        }
        if (containsRecurrenceId()) {
            clone.setRecurrenceId(getRecurrenceId());
        }
        if (containsRecurrenceRule()) {
            clone.setRecurrenceRule(getRecurrenceRule());
        }
        if (containsSequence()) {
            clone.setSequence(getSequence());
        }
        if (containsSeriesId()) {
            clone.setSeriesId(getSeriesId());
        }
        if (containsStartDate()) {
            clone.setStartDate(getStartDate());
        }
        if (containsStartTimeZone()) {
            clone.setStartTimeZone(getStartTimeZone());
        }
        if (containsStatus()) {
            clone.setStatus(getStatus());
        }
        if (containsSummary()) {
            clone.setSummary(getSummary());
        }
        if (containsTransp()) {
            clone.setTransp(getTransp());
        }
        if (containsUid()) {
            clone.setUid(getUid());
        }
        return clone;
    }

    private <T> List<T> cloneList(List<T> list) {
        if (null == list) {
            return null;
        }
        List<T> retval = new ArrayList<T>();
        retval.addAll(list);
        return retval;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (allDay ? 1231 : 1237);
        result = prime * result + ((attachments == null) ? 0 : attachments.hashCode());
        result = prime * result + ((attendees == null) ? 0 : attendees.hashCode());
        result = prime * result + ((categories == null) ? 0 : categories.hashCode());
        result = prime * result + ((changeExceptionDates == null) ? 0 : changeExceptionDates.hashCode());
        result = prime * result + ((classification == null) ? 0 : classification.hashCode());
        result = prime * result + ((color == null) ? 0 : color.hashCode());
        result = prime * result + ((created == null) ? 0 : created.hashCode());
        result = prime * result + createdBy;
        result = prime * result + ((deleteExceptionDates == null) ? 0 : deleteExceptionDates.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((endDate == null) ? 0 : endDate.hashCode());
        result = prime * result + ((endTimeZone == null) ? 0 : endTimeZone.hashCode());
        result = prime * result + ((filename == null) ? 0 : filename.hashCode());
        result = prime * result + id;
        result = prime * result + ((lastModified == null) ? 0 : lastModified.hashCode());
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + modifiedBy;
        result = prime * result + ((organizer == null) ? 0 : organizer.hashCode());
        result = prime * result + publicFolderId;
        result = prime * result + ((recurrenceId == null) ? 0 : recurrenceId.hashCode());
        result = prime * result + ((recurrenceRule == null) ? 0 : recurrenceRule.hashCode());
        result = prime * result + sequence;
        result = prime * result + seriesId;
        result = prime * result + ((setFields == null) ? 0 : setFields.hashCode());
        result = prime * result + ((startDate == null) ? 0 : startDate.hashCode());
        result = prime * result + ((startTimeZone == null) ? 0 : startTimeZone.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + ((summary == null) ? 0 : summary.hashCode());
        result = prime * result + ((transp == null) ? 0 : transp.hashCode());
        result = prime * result + ((uid == null) ? 0 : uid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Event other = (Event) obj;
        if (allDay != other.allDay)
            return false;
        if (attachments == null) {
            if (other.attachments != null)
                return false;
        } else if (!attachments.equals(other.attachments))
            return false;
        if (attendees == null) {
            if (other.attendees != null)
                return false;
        } else if (!attendees.equals(other.attendees))
            return false;
        if (categories == null) {
            if (other.categories != null)
                return false;
        } else if (!categories.equals(other.categories))
            return false;
        if (changeExceptionDates == null) {
            if (other.changeExceptionDates != null)
                return false;
        } else if (!changeExceptionDates.equals(other.changeExceptionDates))
            return false;
        if (classification != other.classification)
            return false;
        if (color == null) {
            if (other.color != null)
                return false;
        } else if (!color.equals(other.color))
            return false;
        if (created == null) {
            if (other.created != null)
                return false;
        } else if (!created.equals(other.created))
            return false;
        if (createdBy != other.createdBy)
            return false;
        if (deleteExceptionDates == null) {
            if (other.deleteExceptionDates != null)
                return false;
        } else if (!deleteExceptionDates.equals(other.deleteExceptionDates))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (endDate == null) {
            if (other.endDate != null)
                return false;
        } else if (!endDate.equals(other.endDate))
            return false;
        if (endTimeZone == null) {
            if (other.endTimeZone != null)
                return false;
        } else if (!endTimeZone.equals(other.endTimeZone))
            return false;
        if (filename == null) {
            if (other.filename != null)
                return false;
        } else if (!filename.equals(other.filename))
            return false;
        if (id != other.id)
            return false;
        if (lastModified == null) {
            if (other.lastModified != null)
                return false;
        } else if (!lastModified.equals(other.lastModified))
            return false;
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (!location.equals(other.location))
            return false;
        if (modifiedBy != other.modifiedBy)
            return false;
        if (organizer == null) {
            if (other.organizer != null)
                return false;
        } else if (!organizer.equals(other.organizer))
            return false;
        if (publicFolderId != other.publicFolderId)
            return false;
        if (recurrenceId == null) {
            if (other.recurrenceId != null)
                return false;
        } else if (!recurrenceId.equals(other.recurrenceId))
            return false;
        if (recurrenceRule == null) {
            if (other.recurrenceRule != null)
                return false;
        } else if (!recurrenceRule.equals(other.recurrenceRule))
            return false;
        if (sequence != other.sequence)
            return false;
        if (seriesId != other.seriesId)
            return false;
        if (setFields == null) {
            if (other.setFields != null)
                return false;
        } else if (!setFields.equals(other.setFields))
            return false;
        if (startDate == null) {
            if (other.startDate != null)
                return false;
        } else if (!startDate.equals(other.startDate))
            return false;
        if (startTimeZone == null) {
            if (other.startTimeZone != null)
                return false;
        } else if (!startTimeZone.equals(other.startTimeZone))
            return false;
        if (status != other.status)
            return false;
        if (summary == null) {
            if (other.summary != null)
                return false;
        } else if (!summary.equals(other.summary))
            return false;
        if (transp == null) {
            if (other.transp != null)
                return false;
        } else if (!transp.equals(other.transp))
            return false;
        if (uid == null) {
            if (other.uid != null)
                return false;
        } else if (!uid.equals(other.uid))
            return false;
        return true;
    }

}
