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

package com.openexchange.service.indexing.impl.internal;

import java.net.InetSocketAddress;
import java.util.Date;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import com.openexchange.service.indexing.JobInfo;


/**
 * {@link Tools}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public class Tools {
    
    public static JobDetail buildJob(JobInfo info, Class<? extends Job> quartzJob) {
        JobDataMap jobData = new JobDataMap();
        jobData.put(JobConstants.JOB_INFO, info);
        JobDetail jobDetail = JobBuilder.newJob(quartzJob)
            .withIdentity(generateJobKey(info))
            .usingJobData(jobData)
            .build();
        return jobDetail;
    }
    
    public static Trigger buildTrigger(JobKey jobKey, JobInfo info, Date startDate, long repeatInterval, int priority) {
        Date tmpDate = startDate;
        if (tmpDate == null) {
            tmpDate = new Date();
        }
        
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
            .forJob(jobKey)
            .withIdentity(generateTriggerKey(info, tmpDate, repeatInterval))
            .startAt(tmpDate)
            .withPriority(priority)
            .usingJobData(JobConstants.START_DATE, tmpDate.getTime())
            .usingJobData(JobConstants.INTERVAL, repeatInterval)
            .usingJobData(JobConstants.PRIORITY, priority);
        
        
        if (repeatInterval > 0L) {
            triggerBuilder.withSchedule(
                SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMilliseconds(repeatInterval)
                .repeatForever()
                .withMisfireHandlingInstructionIgnoreMisfires());
        } else {
            triggerBuilder.withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionIgnoreMisfires());
        }
        Trigger trigger = triggerBuilder.build();
        return trigger;
    }
    
    public static JobKey generateJobKey(JobInfo info) {
        JobKey key = new JobKey(info.toUniqueId(), generateJobGroup(info.contextId, info.userId));
        return key;
    }

    public static TriggerKey generateTriggerKey(JobInfo info, Date startDate, long repeatInterval) {
        TriggerKey key = new TriggerKey(generateTriggerName(info, startDate, repeatInterval), generateTriggerGroup(info.contextId, info.userId));
        return key;
    }

    public static String generateJobGroup(int contextId) {
        return "indexingJobs/" + contextId;
    }

    public static String generateJobGroup(int contextId, int userId) {
        return "indexingJobs/" + contextId + '/' + userId;
    }

    public static String generateTriggerGroup(int contextId, int userId) {
        return "indexingTriggers/" + contextId + '/' + userId;
    }

    public static String generateTriggerName(JobInfo info, Date startDate, long repeatInterval) {
        StringBuilder sb = new StringBuilder(info.toUniqueId());
        sb.append('/');
        if (repeatInterval > 0L) {
            sb.append("withInterval/");
            sb.append(repeatInterval);
        } else {
            /*
             * Two one shot triggers within the same quarter of an hour have the same trigger key.
             * This avoids triggering jobs too often.
             */
            sb.append("oneShot/");
            long now = startDate.getTime();
            long millisSinceLastFullHour = now % (60000L * 60);
            long lastFullHourInMillis = now - millisSinceLastFullHour;
            long quarters = millisSinceLastFullHour / 60000L / 15;
            long time = lastFullHourInMillis + (quarters * 15 * 60000L);
            sb.append(time);
        }

        return sb.toString();
    }
    
    public static String resolveSocketAddress(InetSocketAddress addr) {
        if (addr.isUnresolved()) {
            return addr.getHostName();
        } else {
            return addr.getAddress().getHostAddress();
        }
    }

}
