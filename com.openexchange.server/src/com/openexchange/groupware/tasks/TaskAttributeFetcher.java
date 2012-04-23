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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.groupware.tasks;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.openexchange.ajax.fields.ParticipantsFields;
import com.openexchange.ajax.fields.TaskFields;
import com.openexchange.search.SearchAttributeFetcher;

/**
 * {@link TaskAttributeFetcher}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class TaskAttributeFetcher implements SearchAttributeFetcher<Task> {

    private static final org.apache.commons.logging.Log LOG = com.openexchange.log.Log.valueOf(com.openexchange.log.LogFactory.getLog(AttributeGetter.class));

    private static interface AttributeGetter {

        public Object getObject(Task candidate);
    }

    private static final Map<String, AttributeGetter> GETTERS;

    static {
        final Map<String, AttributeGetter> m = new HashMap<String, AttributeGetter>(25);

        m.put(TaskFields.ACTUAL_COSTS, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Float.valueOf(candidate.getActualCosts());
            }
        });

        m.put(TaskFields.ACTUAL_DURATION, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Long.valueOf(candidate.getActualDuration());
            }
        });

        m.put(TaskFields.AFTER_COMPLETE, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getAfterComplete();
            }
        });

        m.put(TaskFields.BILLING_INFORMATION, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getBillingInformation();
            }
        });

        m.put(TaskFields.COMPANIES, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getCompanies();
            }
        });

        m.put(TaskFields.CURRENCY, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getCurrency();
            }
        });

        m.put(TaskFields.DATE_COMPLETED, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getDateCompleted();
            }
        });

        m.put(TaskFields.PERCENT_COMPLETED, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getPercentComplete());
            }
        });

        m.put(TaskFields.PRIORITY, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getPriority());
            }
        });

        m.put(TaskFields.PROJECT_ID, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getProjectID());
            }
        });

        m.put(TaskFields.STATUS, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getStatus());
            }
        });

        m.put(TaskFields.TARGET_COSTS, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Float.valueOf(candidate.getTargetCosts());
            }
        });

        m.put(TaskFields.TARGET_DURATION, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Long.valueOf(candidate.getTargetDuration());
            }
        });

        m.put(TaskFields.TRIP_METER, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getTripMeter();
            }
        });

        /*-
         * Calendar Fields
         */

        m.put(TaskFields.ALARM, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getAlarm();
            }
        });

        m.put(TaskFields.CHANGE_EXCEPTIONS, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getChangeException();
            }
        });

        m.put(ParticipantsFields.CONFIRM_MESSAGE, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getConfirmMessage();
            }
        });

        m.put(ParticipantsFields.CONFIRMATION, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getConfirm());
            }
        });

        m.put(TaskFields.DAY_IN_MONTH, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getDayInMonth());
            }
        });

        m.put(TaskFields.DAYS, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getDays());
            }
        });

        m.put(TaskFields.DELETE_EXCEPTIONS, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getDeleteException();
            }
        });

        m.put(TaskFields.END_DATE, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getEndDate();
            }
        });

        m.put(TaskFields.INTERVAL, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getInterval());
            }
        });

        m.put(TaskFields.MONTH, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getMonth());
            }
        });

        m.put(TaskFields.NOTE, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getNote();
            }
        });

        m.put(TaskFields.NOTIFICATION, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Boolean.valueOf(candidate.getNotification());
            }
        });

        m.put(TaskFields.OCCURRENCES, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getOccurrence());
            }
        });

        m.put(TaskFields.PARTICIPANTS, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getParticipants();
            }
        });

        m.put(TaskFields.RECURRENCE_CALCULATOR, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getRecurrenceCalculator());
            }
        });

        m.put(TaskFields.RECURRENCE_DATE_POSITION, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getRecurrenceDatePosition();
            }
        });

        m.put(TaskFields.RECURRENCE_ID, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getRecurrenceID());
            }
        });

        m.put(TaskFields.RECURRENCE_POSITION, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getRecurrencePosition());
            }
        });

        m.put(TaskFields.RECURRENCE_START, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                // TODO: Proper recurrence start date
                return candidate.getStartDate();
            }
        });

        m.put(TaskFields.RECURRENCE_TYPE, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getRecurrenceType());
            }
        });

        m.put(TaskFields.START_DATE, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getStartDate();
            }
        });

        m.put(TaskFields.TITLE, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getTitle();
            }
        });

        m.put(TaskFields.UNTIL, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getUntil();
            }
        });
        m.put(TaskFields.USERS, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getUsers();
            }
        });

        /*-
         * Common fields
         */

        m.put(TaskFields.CATEGORIES, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getCategories();
            }
        });

        m.put(TaskFields.COLORLABEL, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getLabel());
            }
        });

        m.put(TaskFields.CREATED_BY, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getCreatedBy());
            }
        });

        m.put(TaskFields.CREATION_DATE, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getCreationDate();
            }
        });

        m.put(TaskFields.FOLDER_ID, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getParentFolderID());
            }
        });

        m.put(TaskFields.ID, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getObjectID());
            }
        });

        m.put(TaskFields.LAST_MODIFIED, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return candidate.getLastModified();
            }
        });

        m.put(TaskFields.MODIFIED_BY, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Integer.valueOf(candidate.getModifiedBy());
            }
        });

        m.put(TaskFields.PRIVATE_FLAG, new AttributeGetter() {

            @Override
            public Object getObject(final Task candidate) {
                return Boolean.valueOf(candidate.getPrivateFlag());
            }
        });

        GETTERS = Collections.unmodifiableMap(m);
    }

    private static final TaskAttributeFetcher instance = new TaskAttributeFetcher();

    /**
     * Gets the task attribute fetcher instance.
     *
     * @return The task attribute fetcher instance.
     */
    public static TaskAttributeFetcher getInstance() {
        return instance;
    }

    /**
     * Initializes a new {@link TaskAttributeFetcher}.
     */
    private TaskAttributeFetcher() {
        super();
    }

    @Override
    public <T> T getAttribute(final String attributeName, final Task candidate) {
        final AttributeGetter getter = GETTERS.get(attributeName);
        if (null == getter) {
            if (LOG.isInfoEnabled()) {
                LOG.info("No getter for field: " + attributeName);
            }
            return null;
        }
        return (T) getter.getObject(candidate);
    }

}
