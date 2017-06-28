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
 *     Copyright (C) 2017-2020 OX Software GmbH
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

package com.openexchange.chronos.availability;

import java.util.Date;
import java.util.List;
import com.openexchange.chronos.Classification;
import com.openexchange.chronos.ExtendedProperties;
import com.openexchange.chronos.FbType;
import com.openexchange.chronos.Organizer;

/**
 * {@link CalendarAvailability} - Defines periods of availability for a calendar user.
 * Provides a grouping of available time information over a specific range of time.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @see <a href="https://tools.ietf.org/html/rfc7953#section-3.1">RFC 7953, section 3.1</a>
 */
public class CalendarAvailability {

    /** The 'dtstamp' */
    private long creationTimestamp;
    private String uid;

    private FbType busyType = FbType.BUSY_UNAVAILABLE;
    private Classification classification;

    private int priority;
    private int sequence;

    private Date created;
    private int createdBy;
    private Date lastModified;

    private Date startTime;
    private String startTimeZone;
    private Date endTime;
    private String endTimeZone;

    private String description;
    private String location;
    private Organizer organizer;
    private String url;

    private long duration; //FIXME: as integer or another type?

    private ExtendedProperties extendedProperties;
    private List<String> categories;

    private List<CalendarFreeSlot> calendarFreeSlots;

    // TODO: map iana-properties?

    /**
     * Initialises a new {@link CalendarAvailability}.
     */
    public CalendarAvailability() {
        super();
    }

    /**
     * Sets the creationTimestamp
     *
     * @param creationTimestamp The creationTimestamp to set
     */
    public void setCreationTimestamp(long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    /**
     * Sets the uid
     *
     * @param uid The uid to set
     */
    public void setUid(String uid) {
        this.uid = uid;
    }

    /**
     * Gets the creationTimestamp
     *
     * @return The creationTimestamp
     */
    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    /**
     * Gets the uid
     *
     * @return The uid
     */
    public String getUid() {
        return uid;
    }

    /**
     * Gets the busyType
     *
     * @return The busyType
     */
    public FbType getBusyType() {
        return busyType;
    }

    /**
     * Sets the busyType
     *
     * @param busyType The busyType to set
     */
    public void setBusyType(FbType busyType) {
        this.busyType = busyType;
    }

    /**
     * Gets the classification
     *
     * @return The classification
     */
    public Classification getClassification() {
        return classification;
    }

    /**
     * Sets the classification
     *
     * @param classification The classification to set
     */
    public void setClassification(Classification classification) {
        this.classification = classification;
    }

    /**
     * Gets the priority
     *
     * @return The priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Sets the priority
     *
     * @param priority The priority to set
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Gets the sequence
     *
     * @return The sequence
     */
    public int getSequence() {
        return sequence;
    }

    /**
     * Sets the sequence
     *
     * @param sequence The sequence to set
     */
    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    /**
     * Gets the created
     *
     * @return The created
     */
    public Date getCreated() {
        return created;
    }

    /**
     * Sets the created
     *
     * @param created The created to set
     */
    public void setCreated(Date created) {
        this.created = created;
    }

    /**
     * Gets the createdBy
     *
     * @return The createdBy
     */
    public int getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the createdBy
     *
     * @param createdBy The createdBy to set
     */
    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the lastModified
     *
     * @return The lastModified
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * Sets the lastModified
     *
     * @param lastModified The lastModified to set
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Gets the startTime
     *
     * @return The startTime
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Sets the startTime
     *
     * @param startTime The startTime to set
     */
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    /**
     * Gets the startTimeZone
     *
     * @return The startTimeZone
     */
    public String getStartTimeZone() {
        return startTimeZone;
    }

    /**
     * Sets the startTimeZone
     *
     * @param startTimeZone The startTimeZone to set
     */
    public void setStartTimeZone(String startTimeZone) {
        this.startTimeZone = startTimeZone;
    }

    /**
     * Gets the endTime
     *
     * @return The endTime
     */
    public Date getEndTime() {
        return endTime;
    }

    /**
     * Sets the endTime
     *
     * @param endTime The endTime to set
     */
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    /**
     * Gets the endTimeZone
     *
     * @return The endTimeZone
     */
    public String getEndTimeZone() {
        return endTimeZone;
    }

    /**
     * Sets the endTimeZone
     *
     * @param endTimeZone The endTimeZone to set
     */
    public void setEndTimeZone(String endTimeZone) {
        this.endTimeZone = endTimeZone;
    }

    /**
     * Gets the description
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description
     *
     * @param description The description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the location
     *
     * @return The location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the location
     *
     * @param location The location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Gets the organizer
     *
     * @return The organizer
     */
    public Organizer getOrganizer() {
        return organizer;
    }

    /**
     * Sets the organizer
     *
     * @param organizer The organizer to set
     */
    public void setOrganizer(Organizer organizer) {
        this.organizer = organizer;
    }

    /**
     * Gets the url
     *
     * @return The url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the url
     *
     * @param url The url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Gets the duration
     *
     * @return The duration
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Sets the duration
     *
     * @param duration The duration to set
     */
    public void setDuration(long duration) {
        this.duration = duration;
    }

    /**
     * Gets the extendedProperties
     *
     * @return The extendedProperties
     */
    public ExtendedProperties getExtendedProperties() {
        return extendedProperties;
    }

    /**
     * Sets the extendedProperties
     *
     * @param extendedProperties The extendedProperties to set
     */
    public void setExtendedProperties(ExtendedProperties extendedProperties) {
        this.extendedProperties = extendedProperties;
    }

    /**
     * Gets the categories
     *
     * @return The categories
     */
    public List<String> getCategories() {
        return categories;
    }

    /**
     * Sets the categories
     *
     * @param categories The categories to set
     */
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    /**
     * Gets the calendarFreeSlots
     *
     * @return The calendarFreeSlots
     */
    public List<CalendarFreeSlot> getCalendarFreeSlots() {
        return calendarFreeSlots;
    }

    /**
     * Sets the calendarFreeSlots
     *
     * @param calendarFreeSlots The calendarFreeSlots to set
     */
    public void setCalendarFreeSlots(List<CalendarFreeSlot> calendarFreeSlots) {
        this.calendarFreeSlots = calendarFreeSlots;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CalendarAvailability [uid=").append(getUid()).append(", busyType=").append(busyType).append(", startTime=").append(startTime).append(", endTime=").append(endTime).append(", description=").append(description).append("]");
        return builder.toString();
    }
}
