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

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import org.apache.commons.logging.Log;
import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.HazelcastInstance;
import com.openexchange.exception.OXException;
import com.openexchange.service.indexing.IndexingService;
import com.openexchange.service.indexing.JobInfo;
import com.openexchange.threadpool.Task;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.ThreadRenamer;

/**
 * {@link AnotherIndexingService}
 * 
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public class AnotherIndexingService implements IndexingService {

    private static final Log LOG = com.openexchange.log.Log.loggerFor(AnotherIndexingService.class);

    @Override
    public void scheduleJob(boolean async, final JobInfo info, final Date startDate, final long repeatInterval, final int priority) throws OXException {
        if (LOG.isTraceEnabled()) {
            String at = startDate == null ? "now" : startDate.toString();
            LOG.trace("Scheduling job " + info.toString() + " at " + at + " with interval " + repeatInterval + " and priority " + priority + ".");
        }
        
        Task<Object> task = new TaskAdapter(new ScheduleJobCallable(info, startDate, repeatInterval, priority));
        try {
            if (async) {
                ThreadPoolService threadPoolService = getThreadPoolService();
                threadPoolService.submit(task);
            } else {
                task.call();
            }
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
        }
    }

    @Override
    public void unscheduleJob(boolean async, JobInfo info) throws OXException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Unscheduling job " + info.toString());
        }
        
        HazelcastInstance hazelcast = Services.getService(HazelcastInstance.class);
        ExecutorService executorService = hazelcast.getExecutorService();
        FutureTask<Object> task = new DistributedTask<Object>(new UnscheduleJobCallable(info), hazelcast.getCluster().getMembers());
        executorService.submit(task);
    }

    @Override
    public void unscheduleAllForUser(boolean async, int contextId, int userId) throws OXException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Unscheduling all jobs for user " + userId + " in context " + contextId + ".");
        }
        
        HazelcastInstance hazelcast = Services.getService(HazelcastInstance.class);
        ExecutorService executorService = hazelcast.getExecutorService();
        FutureTask<Object> task = new DistributedTask<Object>(new UnscheduleAllJobsCallable(contextId, userId), hazelcast.getCluster().getMembers());
        executorService.submit(task);
    }

    @Override
    public void unscheduleAllForContext(boolean async, int contextId) throws OXException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Unscheduling all jobs for context " + contextId + ".");
        }
        
        HazelcastInstance hazelcast = Services.getService(HazelcastInstance.class);
        ExecutorService executorService = hazelcast.getExecutorService();
        FutureTask<Object> task = new DistributedTask<Object>(new UnscheduleAllJobsCallable(contextId, -1), hazelcast.getCluster().getMembers());
        executorService.submit(task);
    }

    ThreadPoolService getThreadPoolService() throws OXException {
        ThreadPoolService threadPoolService = Services.getService(ThreadPoolService.class);
        return threadPoolService;
    }

    private static final class TaskAdapter implements Task<Object> {

        private final Callable<Object> callable;

        public TaskAdapter(Callable<Object> callable) {
            super();
            this.callable = callable;
        }

        @Override
        public Object call() throws Exception {
            return callable.call();
        }

        @Override
        public void setThreadName(ThreadRenamer threadRenamer) {
            return;
        }

        @Override
        public void beforeExecute(Thread t) {
            return;
        }

        @Override
        public void afterExecute(Throwable t) {
            return;
        }

    }
}
