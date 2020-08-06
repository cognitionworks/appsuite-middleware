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

package com.openexchange.push.impl.jobqueue;

import static com.openexchange.java.Autoboxing.I;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.UpdateStatus;
import com.openexchange.groupware.update.Updater;
import com.openexchange.osgi.ShutDownRuntimeException;
import com.openexchange.push.PushExceptionCodes;
import com.openexchange.push.PushListener;
import com.openexchange.push.PushManagerExtendedService;
import com.openexchange.push.PushUser;
import com.openexchange.push.impl.PushManagerRegistry;
import com.openexchange.threadpool.ThreadPools;

/**
 * {@link PermanentListenerJobQueue}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.2
 */
public class PermanentListenerJobQueue {

    /** The logger */
    static final Logger LOG = org.slf4j.LoggerFactory.getLogger(PermanentListenerJobQueue.class);

    private static PermanentListenerJobQueue INSTANCE = new PermanentListenerJobQueue();

    /**
     * Gets the job queue instance
     *
     * @return The instance
     */
    public static PermanentListenerJobQueue getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final ReentrantReadWriteLock stoplock;
    private final Lock stoplockReadLock;
    final Map<PushUser, FutureTask<PushListener>> user2jobs;
    final BlockingQueue<FutureTask<PushListener>> jobs;
    final AtomicReference<Thread> workerReference;
    final boolean checkUpdateStatus;
    private boolean stopped;

    /**
     * Initializes a new {@link PermanentListenerJobQueue}.
     */
    private PermanentListenerJobQueue() {
        super();
        stoplock = new ReentrantReadWriteLock();
        stoplockReadLock = stoplock.readLock();
        user2jobs = new HashMap<>(256);
        jobs = new LinkedBlockingQueue<>();
        workerReference = new AtomicReference<Thread>(null);
        checkUpdateStatus = false;
        stopped = false;
    }

    /**
     * Stops this job queue.
     */
    public void stop() {
        Lock lock = this.stoplock.writeLock();
        lock.lock();
        try {
            if (stopped) {
                // Already stopped
                return;
            }

            stopped = true;
            jobs.clear();
            Thread worker = workerReference.getAndSet(null);
            if (null != worker) {
                worker.interrupt();
            }
            synchronized (user2jobs) {
                user2jobs.clear();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Schedules that a permanent listener will be started for given user in specified service.
     *
     * @param pushUser The push user
     * @param extendedService The service to run the listener
     * @return The scheduled job; otherwise <code>null</code> if there is already such a job or job could not be added
     * @throws ShutDownRuntimeException If server is about to shut-down
     */
    public PermanentListenerJob scheduleJob(PushUser pushUser, PushManagerExtendedService extendedService) {
        Lock lock = this.stoplockReadLock;
        lock.lock();
        try {
            if (stopped) {
                throw new ShutDownRuntimeException();
            }

            FutureTask<PushListener> newJob;
            synchronized (user2jobs) {
                if (user2jobs.containsKey(pushUser)) {
                    // There is already such a job
                    return null;
                }

                // None present, yet. Create a new job.
                newJob = new FutureTask<>(new PermanentListenerCallable(pushUser, extendedService, this));
                user2jobs.put(pushUser, newJob);
            }

            // Add to queue in case job was newly created
            boolean offered = jobs.offer(newJob);
            if (false == offered) {
                // Could not be offered to job queue. Run with this thread...
                boolean remove = true;
                try {
                    newJob.run();
                    removeJobFor(pushUser);
                    remove = false;

                    PushListener pushListener = ThreadPools.getFrom(newJob);
                    return new AlreadyExecutedPermanentListenerJob(pushUser, pushListener);
                } catch (Exception e) {
                    return new AlreadyExecutedPermanentListenerJob(pushUser, e);
                } finally {
                    if (remove) {
                        removeJobFor(pushUser);
                    }
                }
            }

            // Ensure worker is active
            if (null == workerReference.get()) {
                synchronized (this) {
                    if (null == workerReference.get()) {
                        Thread newWorker = new Thread(new ConsumerRunnable(), "PermanentListenerStarterThread");
                        workerReference.set(newWorker);
                        newWorker.start();
                    }
                }
            }

            // Job newly added
            return new PermanentListenerJobImpl(pushUser, newJob);
        } finally {
            lock.unlock();
        }
    }

    /**
     * In case a pending job still resides in job queue it is canceled; otherwise if already in execution completion is awaited.
     *
     * @param pushUser The push user to stop
     * @return <code>true</code> if permanent listener has been canceled; otherwise <code>false</code>
     */
    public boolean cancelJob(PushUser pushUser) {
        FutureTask<PushListener> job;
        synchronized (user2jobs) {
            job = user2jobs.remove(pushUser);
        }

        if (job != null) {
            if (jobs.remove(job)) {
                return true; // Permanent listener not stopped, but only removed from job queue.
            }

            // Already taken from queue. Thus in execution. Await completion.
            try {
                job.get();
            } catch (ExecutionException e) {
                // Ignore
            } catch (InterruptedException e) {
                // Keep interrupted status
                Thread.currentThread().interrupt();
            }
        }

        return false;
    }

    /**
     * Removes the job for push user from '<code>user2jobs</code>' mapping.
     *
     * @param pushUser The push user
     */
    void removeJobFor(PushUser pushUser) {
        synchronized (user2jobs) {
            user2jobs.remove(pushUser);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private class ConsumerRunnable implements Runnable {

        ConsumerRunnable() {
            super();
        }

        @Override
        public void run() {
            Thread currentThread = Thread.currentThread();
            boolean nullify = true;
            try {
                while (!currentThread.isInterrupted()) {
                    FutureTask<PushListener> job = jobs.poll(10, TimeUnit.SECONDS);
                    if (null == job) {
                        synchronized (user2jobs) {
                            if (user2jobs.isEmpty()) {
                                // Terminate worker...
                                workerReference.set(null);
                                nullify = false;
                                return;
                            }
                        }
                    } else {
                        job.run();
                    }
                }
            } catch (InterruptedException e) {
                // Keep interrupted status
                currentThread.interrupt();
            } catch (RuntimeException e) {
                LOG.error("Failed worker thread for permanent listener job queue", e);
            } finally {
                // For safety reason
                if (nullify) {
                    workerReference.set(null);
                }
            }
        }
    }

    private static class PermanentListenerCallable implements Callable<PushListener> {

        private final PushUser pushUser;
        private final PushManagerExtendedService extendedService;
        private final PermanentListenerJobQueue jobQueue;

        PermanentListenerCallable(PushUser pushUser, PushManagerExtendedService extendedService, PermanentListenerJobQueue jobQueue) {
            super();
            this.pushUser = pushUser;
            this.extendedService = extendedService;
            this.jobQueue = jobQueue;
        }

        @Override
        public PushListener call() {
            try {
                int contextId = pushUser.getContextId();
                int userId = pushUser.getUserId();

                // Start permanent listener for push user
                Thread currentThread = Thread.currentThread();
                int retry = 2;
                while (retry-- > 0 && !currentThread.isInterrupted()) {
                    try {
                        if (jobQueue.checkUpdateStatus && schemaBeingLockedOrNeedsUpdate(contextId)) {
                            LOG.info("Database schema is either currently updated or has (blocking) update tasks pending. Denied start-up of permanent push listener for user {} in context {} by push manager \"{}\"", I(userId), I(contextId), extendedService);
                            return null;
                        }

                        PushListener pl = extendedService.startPermanentListener(pushUser);
                        retry = 0;
                        if (null != pl) {
                            LOG.debug("Started permanent push listener for user {} in context {} by push manager \"{}\"", I(userId), I(contextId), extendedService);
                        }
                        return pl;
                    } catch (OXException e) {
                        if (PushExceptionCodes.AUTHENTICATION_ERROR.equals(e)) {
                            PushManagerRegistry.getInstance().handleInvalidCredentials(pushUser, true, e);
                        } else if (PushExceptionCodes.MISSING_PASSWORD.equals(e)) {
                            retry = 0;
                            PushManagerRegistry.getInstance().handleInvalidCredentials(pushUser, true, e);
                        } else {
                            retry = 0;
                            LOG.error("Error while starting permanent push listener for user {} in context {} by push manager \"{}\".", I(userId), I(contextId), extendedService, e);
                        }
                    } catch (RuntimeException e) {
                        retry = 0;
                        LOG.error("Runtime error while starting permanent push listener for user {} in context {} by push manager \"{}\".", I(userId), I(contextId), extendedService, e);
                    }
                }
                return null;
            } finally {
                jobQueue.removeJobFor(pushUser);
            }
        }

        private boolean schemaBeingLockedOrNeedsUpdate(int contextId) throws OXException {
            try {
                UpdateStatus status = Updater.getInstance().getStatus(contextId);
                return (status.blockingUpdatesRunning() || status.needsBlockingUpdates());
            } catch (OXException e) {
                if (e.getCode() == 102) {
                    // NOTE: this situation should not happen!
                    // it can only happen, when a schema has not been initialized correctly!
                    LOG.debug("FATAL: this error must not happen",e);
                }
                LOG.error("Error in checking/updating schema",e);
                throw e;
            }
        }
    }

    private static class PermanentListenerJobImpl implements PermanentListenerJob {

        private final PushUser pushUser;
        private final Future<PushListener> future;

        PermanentListenerJobImpl(PushUser pushUser, Future<PushListener> future) {
            super();
            this.pushUser = pushUser;
            this.future = future;
        }

        @Override
        public PushUser getPushUser() {
            return pushUser;
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        public PushListener get() throws InterruptedException, ExecutionException {
            return future.get();
        }

        @Override
        public PushListener get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return future.get(timeout, unit);
        }

        @Override
        public int compareTo(PermanentListenerJob o) {
            return pushUser.compareTo(o.getPushUser());
        }
    }

    private static class AlreadyExecutedPermanentListenerJob implements PermanentListenerJob {

        private final PushUser pushUser;
        private final PushListener pushListener;
        private final Exception executionException;

        AlreadyExecutedPermanentListenerJob(PushUser pushUser, PushListener pushListener) {
            super();
            this.pushUser = pushUser;
            this.pushListener = pushListener;
            this.executionException = null;
        }

        AlreadyExecutedPermanentListenerJob(PushUser pushUser, Exception executionException) {
            super();
            this.pushUser = pushUser;
            this.executionException = executionException;
            this.pushListener = null;
        }

        @Override
        public PushUser getPushUser() {
            return pushUser;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public PushListener get() throws InterruptedException, ExecutionException {
            if (null != executionException) {
                throw new ExecutionException(executionException);
            }
            return pushListener;
        }

        @Override
        public PushListener get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return get();
        }

        @Override
        public int compareTo(PermanentListenerJob o) {
            return pushUser.compareTo(o.getPushUser());
        }
    }

    private static class ScheduleKey {

        final PushUser pushUser;
        final PushManagerExtendedService extendedService;
        private final int hash;

        ScheduleKey(PushUser pushUser, PushManagerExtendedService extendedService) {
            super();
            if (pushUser == null || extendedService == null) {
                throw new IllegalArgumentException("Neither of the arguments may be null");
            }
            this.pushUser = pushUser;
            this.extendedService = extendedService;

            int prime = 31;
            int result = 1;
            result = prime * result + (pushUser.hashCode());
            result = prime * result + (extendedService.hashCode());
            hash = result;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ScheduleKey)) {
                return false;
            }
            ScheduleKey other = (ScheduleKey) obj;
            if (!pushUser.equals(other.pushUser)) {
                return false;
            }
            if (!extendedService.equals(other.extendedService)) {
                return false;
            }
            return true;
        }
    }

}
