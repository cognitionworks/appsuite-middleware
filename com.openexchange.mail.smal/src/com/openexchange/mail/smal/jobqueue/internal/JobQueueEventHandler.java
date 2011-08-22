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

package com.openexchange.mail.smal.jobqueue.internal;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import com.openexchange.imap.cache.ListLsubCache.Key;
import com.openexchange.mail.smal.SMALServiceLookup;
import com.openexchange.mail.smal.jobqueue.Job;
import com.openexchange.mail.smal.jobqueue.JobQueue;
import com.openexchange.mail.smal.jobqueue.MailAccountJob;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondEventConstants;

/**
 * {@link JobQueueEventHandler}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class JobQueueEventHandler implements EventHandler {

    private static final org.apache.commons.logging.Log LOG =
        com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(JobQueueEventHandler.class));

    /**
     * Initializes a new {@link JobQueueEventHandler}.
     */
    public JobQueueEventHandler() {
        super();
    }

    @Override
    public void handleEvent(final Event event) {
        final String topic = event.getTopic();
        if (SessiondEventConstants.TOPIC_REMOVE_DATA.equals(topic)) {
            @SuppressWarnings("unchecked") final Map<String, Session> container =
                (Map<String, Session>) event.getProperty(SessiondEventConstants.PROP_CONTAINER);
            for (final Session session : container.values()) {
                handleDroppedSession(session);
            }
        } else if (SessiondEventConstants.TOPIC_REMOVE_SESSION.equals(topic)) {
            final Session session = (Session) event.getProperty(SessiondEventConstants.PROP_SESSION);
            handleDroppedSession(session);
        } else if (SessiondEventConstants.TOPIC_REMOVE_CONTAINER.equals(topic)) {
            @SuppressWarnings("unchecked") final Map<String, Session> container =
                (Map<String, Session>) event.getProperty(SessiondEventConstants.PROP_CONTAINER);
            for (final Session session : container.values()) {
                handleDroppedSession(session);
            }
        } else if (SessiondEventConstants.TOPIC_ADD_SESSION.equals(topic)) {
            final Session session = (Session) event.getProperty(SessiondEventConstants.PROP_SESSION);
            handleAddedSession(session);
        } else if (SessiondEventConstants.TOPIC_REACTIVATE_SESSION.equals(topic)) {
            final Session session = (Session) event.getProperty(SessiondEventConstants.PROP_SESSION);
            handleAddedSession(session);
        }
    }

    private void handleDroppedSession(final Session session) {
        try {
            final Queue<Job> jobs = (Queue<Job>) session.getParameter("com.openexchange.mail.smal.jobqueue.jobs");
            if (null != jobs) {
                while (!jobs.isEmpty()) {
                    final Job job = jobs.poll();
                    if (!job.isDone()) {
                        job.cancel();
                    }
                }
            }
        } catch (final Exception e) {
            // Failed handling session
            LOG.warn("Failed handling tracked removed session.", e);
        } finally {
            session.setParameter("com.openexchange.mail.smal.jobqueue.jobs", null);
        }
    }

    private void handleAddedSession(final Session session) {
        try {
            /*
             * Add jobs
             */
            final MailAccountStorageService storageService = SMALServiceLookup.getServiceStatic(MailAccountStorageService.class);
            final int userId = session.getUserId();
            final int contextId = session.getContextId();
            final Queue<Job> jobs = getJobsFrom(session);
            final JobQueue jobQueue = JobQueue.getInstance();
            for (final MailAccount account : storageService.getUserMailAccounts(userId, contextId)) {
                final MailAccountJob maj = new MailAccountJob(account.getId(), userId, contextId, "INBOX");
                if (jobQueue.addJob(maj)) {
                    jobs.offer(maj);
                }
            }
            /*
             * Periodic job
             */
            as


        } catch (final Exception e) {
            // Failed handling session
            LOG.warn("Failed handling tracked added session.", e);
        }
    }

    private static Queue<Job> getJobsFrom(final Session session) {
        final Lock lock = (Lock) session.getParameter(Session.PARAM_LOCK);
        if (null == lock) {
            synchronized (session) {
                return getJobsFrom0(session);
            }
        }
        lock.lock();
        try {
            return getJobsFrom0(session);
        } finally {
            lock.unlock();
        }
    }

    private static Queue<Job> getJobsFrom0(final Session session) {
        Queue<Job> jobs = (Queue<Job>) session.getParameter("com.openexchange.mail.smal.jobqueue.jobs");
        if (null == jobs) {
            jobs = new ConcurrentLinkedQueue<Job>();
            session.setParameter("com.openexchange.mail.smal.jobqueue.jobs", jobs);
        }
        return jobs;
    }

    private static Key keyFor(final Session session) {
        return new Key(session.getUserId(), session.getContextId());
    }

    private static final class Key {

        private final int cid;

        private final int user;

        private final int hash;

        public Key(final int user, final int cid) {
            super();
            this.user = user;
            this.cid = cid;
            final int prime = 31;
            int result = 1;
            result = prime * result + cid;
            result = prime * result + user;
            hash = result;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Key)) {
                return false;
            }
            final Key other = (Key) obj;
            if (cid != other.cid) {
                return false;
            }
            if (user != other.user) {
                return false;
            }
            return true;
        }

    } // End of class Key

}
