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
 *    trademarks of the OX Software GmbH. group of companies.
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

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import org.dmfs.rfc5545.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.scheduling.changes.Description;

/**
 * {@link ReschedulingDescriptionTest}
 *
 * @author <a href="mailto:anna.ottersbach@open-xchange.com">Anna Ottersbach</a>
 * @since v7.10.3
 */
public class ReschedulingDescriptionTest extends AbstractDescriptionTestMocking {

    private ReschedulingDescriber describer;

    private DateTime startDate1;
    private DateTime startDate2;
    private DateTime endDate1;
    private DateTime endDate2;
    private DateTime differentDay;
    private DateTime allDay1;
    private DateTime allDay2;
    private DateTime timezoneStartDate1;
    private DateTime timezoneDifferentDay;
    private DateTime timezoneEndDate1;
    private static final String TIMEZONE1 = "Europe/Berlin";
    private static final String TIMEZONE2 = "PST";
    private static final TimeZone timezone = TimeZone.getTimeZone(TIMEZONE1);
    private static final TimeZone timezone2 = TimeZone.getTimeZone(TIMEZONE2);
    private static final String RESCHEDULING = "The appointment was rescheduled";
    private static final String UPDATE_TIMEZONESTARTDATE = "The timezone of the appointment's start date was changed";
    private static final String UPDATE_TIMEZONEENDDATE = "The timezone of the appointment's end date was changed";
    private static final String UPDATE_TIMEZONEDATE = "The timezone of the appointment was changed";

    public ReschedulingDescriptionTest() {
        super();
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        describer = new ReschedulingDescriber();
        
        PowerMockito.when(B(fields.contains(EventField.START_DATE))).thenReturn(Boolean.TRUE);
        PowerMockito.when(B(fields.contains(EventField.END_DATE))).thenReturn(Boolean.TRUE);
        
        startDate1 = new DateTime(timezone, 2020, Calendar.JANUARY, 1, 10, 0, 0);
        startDate2 = new DateTime(timezone, 2020, Calendar.JANUARY, 1, 11, 0, 0);
        differentDay = new DateTime(timezone, 2020, Calendar.JANUARY, 2, 11, 0, 0);
        endDate1 = new DateTime(timezone, 2020, Calendar.JANUARY, 1, 14, 0, 0);
        endDate2 = new DateTime(timezone, 2020, Calendar.JANUARY, 1, 15, 0, 0);
        allDay1 = new DateTime(2020, Calendar.JANUARY, 1);
        allDay2 = new DateTime(2020, Calendar.JANUARY, 2);

        timezoneStartDate1 = new DateTime(timezone2, 2020, Calendar.JANUARY, 1, 10, 0, 0);
        timezoneDifferentDay = new DateTime(timezone2, 2020, Calendar.JANUARY, 2, 11, 0, 0);
        timezoneEndDate1 = new DateTime(timezone2, 2020, Calendar.JANUARY, 1, 14, 0, 0);
    }

    @Test
    public void testRescheduling_UpdateStartDate_DescriptionAvailable() {
        setDate(startDate1, endDate1, startDate2, endDate1);

        Description description = describer.describe(eventUpdate);
        testDescription(description);
        assertThat("Not matching size", I(description.getSentences().size()), is((I(1))));
        checkMessageStart(description, 0, RESCHEDULING, "January 1, 2020 10:00 AM - 2:00 PM", "January 1, 2020 11:00 AM - 2:00 PM");
    }

    @Test
    public void testRescheduling_UpdateEndDate_DescriptionAvailable() {
        setDate(startDate1, endDate1, startDate1, endDate2);

        Description description = describer.describe(eventUpdate);
        testDescription(description);
        assertThat("Not matching size", I(description.getSentences().size()), is((I(1))));
        checkMessageStart(description, 0, RESCHEDULING, "January 1, 2020 10:00 AM - 2:00 PM", "January 1, 2020 10:00 AM - 3:00 PM");
    }

    @Test
    public void testRescheduling_UpdateStartEnd_DescriptionAvailable() {
        setDate(startDate1, endDate1, startDate2, endDate2);

        Description description = describer.describe(eventUpdate);
        testDescription(description);
        assertThat("Not matching size", I(description.getSentences().size()), is((I(1))));
        checkMessageStart(description, 0, RESCHEDULING, "January 1, 2020 10:00 AM - 2:00 PM", "January 1, 2020 11:00 AM - 3:00 PM");
    }

    @Test
    public void testRescheduling_UpdateDay_DescriptionAvailable() {
        setDate(startDate1, endDate1, startDate1, differentDay);

        Description description = describer.describe(eventUpdate);
        testDescription(description);
        assertThat("Not matching size", I(description.getSentences().size()), is((I(1))));
        checkMessageStart(description, 0, RESCHEDULING, "January 1, 2020 10:00 AM - January 1, 2020 2:00 PM", "January 1, 2020 10:00 AM - January 2, 2020 11:00 AM");
    }

    @Test
    public void testRescheduling_UpdateDayReverse_DescriptionAvailable() {
        setDate(startDate1, endDate1, differentDay, endDate1);

        Description description = describer.describe(eventUpdate);
        testDescription(description);
        assertThat("Not matching size", I(description.getSentences().size()), is((I(1))));
        checkMessageStart(description, 0, RESCHEDULING, "January 1, 2020 10:00 AM - January 1, 2020 2:00 PM", "January 2, 2020 11:00 AM - January 1, 2020 2:00 PM");
    }

    @Test
    public void testRescheduling_UpdateAllDay_DescriptionAvailable() {
        setDate(allDay1, allDay1, allDay2, allDay2);

        Description description = describer.describe(eventUpdate);
        testDescription(description);
        assertThat("Not matching size", I(description.getSentences().size()), is((I(1))));
        checkMessageStart(description, 0, RESCHEDULING, "January 1, 2020", "January 2, 2020");

    }

    @Test
    public void testRescheduling_UpdateTimeZoneStartDate_DescriptionAvailable() {
        setDate(startDate1, endDate1, timezoneStartDate1, endDate1);

        Description description = describer.describe(eventUpdate);
        testDescription(description);
        assertThat("Not matching size", I(description.getSentences().size()), is((I(2))));
        checkMessageStart(description, 0, UPDATE_TIMEZONESTARTDATE, TIMEZONE1, TIMEZONE2);
        checkMessageStart(description, 1, RESCHEDULING, "January 1, 2020 10:00 AM - 2:00 PM", "January 1, 2020 7:00 PM - 2:00 PM");
    }

    @Test
    public void testRescheduling_UpdateTimeZoneEndDate_DescriptionAvailable() {
        setDate(startDate1, endDate1, startDate1, timezoneEndDate1);

        Description description = describer.describe(eventUpdate);
        testDescription(description);
        assertThat("Not matching size", I(description.getSentences().size()), is((I(2))));
        checkMessageStart(description, 0, UPDATE_TIMEZONEENDDATE, TIMEZONE1, TIMEZONE2);
        checkMessageStart(description, 1, RESCHEDULING, "January 1, 2020 10:00 AM - 2:00 PM", "January 1, 2020 10:00 AM - 11:00 PM");
    }

    @Test
    public void testRescheduling_UpdateTimeZoneStartEnd_DescriptionAvailable() {
        setDate(startDate1, endDate1, timezoneStartDate1, timezoneEndDate1);

        Description description = describer.describe(eventUpdate);
        testDescription(description);
        assertThat("Not matching size", I(description.getSentences().size()), is((I(2))));
        checkMessageStart(description, 0, UPDATE_TIMEZONEDATE, TIMEZONE1, TIMEZONE2);
        checkMessageStart(description, 1, RESCHEDULING, "January 1, 2020 10:00 AM - 2:00 PM", "January 1, 2020 7:00 PM - 11:00 PM");

    }

    @Test
    public void testRescheduling_UpdateTimeZoneDay_DescriptionAvailable() {
        setDate(startDate1, differentDay, startDate1, timezoneDifferentDay);

        Description description = describer.describe(eventUpdate);
        testDescription(description);
        assertThat("Not matching size", I(description.getSentences().size()), is((I(2))));
        checkMessageStart(description, 0, UPDATE_TIMEZONEENDDATE, TIMEZONE1, TIMEZONE2);
        checkMessageStart(description, 1, RESCHEDULING, "January 1, 2020 10:00 AM - January 2, 2020 11:00 AM", "January 1, 2020 10:00 AM - January 2, 2020 8:00 PM");
    }

    @Test
    public void testRescheduling_UpdateTimeZoneDayReverse_DescriptionAvailable() {
        setDate(differentDay, endDate1, timezoneDifferentDay, endDate1);

        Description description = describer.describe(eventUpdate);
        testDescription(description);
        assertThat("Not matching size", I(description.getSentences().size()), is((I(2))));
        checkMessageStart(description, 0, UPDATE_TIMEZONESTARTDATE, TIMEZONE1, TIMEZONE2);
        checkMessageStart(description, 1, RESCHEDULING, "January 2, 2020 11:00 AM - January 1, 2020 2:00 PM", "January 2, 2020 8:00 PM - January 1, 2020 2:00 PM");
    }

    @Test
    public void testRescheduling_NoChanges_DescriptionUnavailable() {
        setDate(startDate1, endDate1, startDate1, endDate1);

        Description description = describer.describe(eventUpdate);
        assertThat(description, nullValue());
    }

    // -------------------- HELPERS --------------------

    private void setDate(DateTime originalStartDate, DateTime originalEndDate, DateTime updatedStartDate, DateTime updatedEndDate) {
        PowerMockito.when(original.getStartDate()).thenReturn(originalStartDate);
        PowerMockito.when(original.getEndDate()).thenReturn(originalEndDate);
        PowerMockito.when(updated.getStartDate()).thenReturn(updatedStartDate);
        PowerMockito.when(updated.getEndDate()).thenReturn(updatedEndDate);
    }

    private void checkMessageStart(Description description, int sentenceIndex, String message, String containee1, String containee2) {
        String messageDescription = getMessage(description, sentenceIndex);
        assertTrue("The description \"" + description + "\" does not start with \"" + message + "\".", messageDescription.startsWith(message));
        assertTrue("The description \"" + description + "\" does not contain \"" + containee1 + "\".", messageDescription.contains(containee1));
        assertTrue("The description \"" + description + "\" does not contain \"" + containee2 + "\".", messageDescription.contains(containee2));
    }

    protected void testDescription(Description description) {
        assertThat("Should not be null", description, notNullValue());
        assertThat("Not matching size", I(description.getChangedFields().size()), is((I(2))));
        assertThat("Wrong field", description.getChangedFields().get(0), is(EventField.START_DATE));
        assertThat("Wrong field", description.getChangedFields().get(1), is(EventField.END_DATE));
    }

    protected String getMessage(Description description, int sentenceIndex) {
        return description.getSentences().get(sentenceIndex).getMessage(FORMAT, Locale.ENGLISH, timezone, null);
    }
}