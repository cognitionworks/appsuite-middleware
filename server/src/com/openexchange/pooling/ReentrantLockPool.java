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

package com.openexchange.pooling;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of the object pool.
 * @param <T> type of pooled objects.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class ReentrantLockPool<T> implements Pool<T>, Runnable {

    static final Log LOG = LogFactory.getLog(ReentrantLockPool.class);

    private final int minIdle;

    private final int maxIdle;

    private final long maxIdleTime;

    private final int maxActive;

    private final long maxWait;

    private final long maxLifeTime;

    private ExhaustedActions exhaustedAction;

    private final boolean testOnActivate;

    private final boolean testOnDeactivate;

    private final boolean testOnIdle;

    private final boolean testThreads;

    private boolean running = true;

    private final PoolImplData<T> data = new PoolImplData<T>();

    private final PoolableLifecycle<T> lifecycle;

    private final ReentrantLock lock = new ReentrantLock(true);

    private final Condition idleAvailable = lock.newCondition();

    private final long[] useTimes = new long[1000];

    private int useTimePointer;

    /**
     * The longest time an object has been used.
     */
    private long maxUseTime;

    /**
     * The shortest time an object has been used.
     */
    private long minUseTime = Long.MAX_VALUE;

    /**
     * Number of broken objects. An object is broken if
     * {@link PoolableLifecycle#activate(PooledData)},
     * {@link PoolableLifecycle#deactivate(PooledData)} or
     * {@link PoolableLifecycle#validate(PooledData)} return <code>false</code>.
     */
    private int numBroken;

    /**
     * Default constructor.
     * @param lifecycle Implementation of the interface for handling the life
     * cycle of pooled objects.
     * @param config Configuration of the pool parameters.
     */
    public ReentrantLockPool(final PoolableLifecycle<T> lifecycle,
        final Config config) {
        super();
        minIdle = Math.max(0, config.minIdle);
        maxIdle = config.maxIdle;
        maxIdleTime = config.maxIdleTime;
        maxActive = config.maxActive;
        maxWait = config.maxWait;
        maxLifeTime = config.maxLifeTime;
        exhaustedAction = config.exhaustedAction;
        testOnActivate = config.testOnActivate;
        testOnDeactivate = config.testOnDeactivate;
        testOnIdle = config.testOnIdle;
        testThreads = config.testThreads;
        this.lifecycle = lifecycle;
        try {
            ensureMinIdle();
        } catch (final PoolingException e) {
            LOG.error("Problem while creating initial objects.", e);
        }
    }

    /**
     * @return the exhaustedAction
     */
    public ExhaustedActions getExhaustedAction() {
        return exhaustedAction;
    }

    /**
     * @param exhaustedAction the exhaustedAction to set
     */
    public void setExhaustedAction(final ExhaustedActions exhaustedAction) {
        this.exhaustedAction = exhaustedAction;
    }

    /**
     * @return the lifecycle
     */
    protected final PoolableLifecycle<T> getLifecycle() {
        return lifecycle;
    }

    /**
     * @return the testThreads
     */
    public boolean isTestThreads() {
        return testThreads;
    }

    /**
     * {@inheritDoc}
     */
    public void back(final T pooled) throws PoolingException {
        if (null == pooled) {
            throw new PoolingException(
                "A null reference was returned to pool.");
        }
        back(pooled, true);
    }

    /**
     * Puts an object into the pool.
     * @param pooled Object to pool.
     * @param decrementActive <code>true</code> if an active object is returned.
     * @return if the object has been put to pool.
     * @throws PoolingException if the object does not belong to this pool.
     */
    private boolean back(final T pooled, final boolean decrementActive)
        throws PoolingException {
        final long startTime = System.currentTimeMillis();
        // checks
        final PooledData <T> metaData;
        if (decrementActive) {
            lock.lock();
            try {
                metaData = data.getActive(pooled);
            } finally {
                lock.unlock();
            }
        } else {
            metaData = new PooledData<T>(pooled);
        }
        // object of this pool?
        if (null == metaData) {
            throw new PoolingException("Object \"" + pooled
                + "\" does not belong to this pool.");
        }
        // reuseable?
        boolean poolable;
        if (running) {
            if (testOnDeactivate) {
                poolable = lifecycle.validate(metaData);
            } else {
                poolable = lifecycle.deactivate(metaData);
            }
            if (!poolable) {
                numBroken++;
            }
            poolable &= (maxLifeTime <= 0
                || metaData.getLiveTime() < maxLifeTime);
        } else {
            poolable = false;
        }
        // return to pool
        boolean destroy = !poolable;
        lock.lock();
        try {
            if (testThreads) {
                data.removeByThread(metaData);
            }
            // statistics
            final long useTime = metaData.getTimeDiff();
            useTimes[useTimePointer++] = useTime;
            useTimePointer = useTimePointer % useTimes.length;
            maxUseTime = Math.max(maxUseTime, useTime);
            minUseTime = Math.min(minUseTime, useTime);
            // update meta data
            metaData.resetTrace();
            metaData.touch();
            if (decrementActive) {
                data.removeActive(metaData);
            }
            idleAvailable.signal();
            if (maxIdle > 0 && data.numIdle() >= maxIdle) {
                destroy = true;
            } else if (poolable) {
                data.addIdle(metaData);
            }
        } finally {
            lock.unlock();
        }
        if (destroy) {
            LOG.trace("Destroying object.");
            lifecycle.destroy(metaData.getPooled());
        }
        LOG.trace("Back time: " + getWaitTime(startTime));
        return !destroy;
    }

    /**
     * {@inheritDoc}
     */
    public T get() throws PoolingException {
        final long startTime = System.currentTimeMillis();
        while (running) {
            PooledData<T> retval;
            boolean created = false;
            lock.lock();
            try {
                if (testThreads) {
                    final PooledData<T> other;
                    final Thread thread = Thread.currentThread();
                    other = data.getByThread(thread);
                    if (other != null && thread.equals(other.getThread())) {
                        if (LOG.isDebugEnabled()) {
                            PoolingException e = new PoolingException("Found thread using two objects. First get.");
                            if (null != other.getTrace()) {
                                e.setStackTrace(other.getTrace());
                            }
                            LOG.debug(e.getMessage(), e);
                            e = new PoolingException("Found thread using two objects. Second get.");
                            e.fillInStackTrace();
                            LOG.debug(e.getMessage(), e);
                        }
                    }
                }
                retval = data.popIdle();
                if (null == retval && maxActive > 0 && data.numActive() >= maxActive) {
                    // now we are getting in trouble. no more idle objects, a maximum number of active is defined and we reached this
                    // border.
                    switch (exhaustedAction) {
                    case GROW:
                        break;
                    case FAIL:
                        throw new PoolingException("Pool exhausted.");
                    case BLOCK:
                        final String threadName = Thread.currentThread().getName();
                        PoolingException warn = new PoolingException("Thread " + threadName
                            + " is sent to sleep until an object in the pool is available. " + data.numActive()
                            + " objects are already in use.");
                        LOG.warn(warn.getMessage(), warn);
                        final long sleepStartTime = System.currentTimeMillis();
                        boolean timedOut = false;
                        try {
                            if (maxWait > 0) {
                                timedOut = !idleAvailable.await(maxWait - getWaitTime(startTime), TimeUnit.MILLISECONDS);
                            } else {
                                idleAvailable.await();
                            }
                        } catch (InterruptedException e) {
                            LOG.error("Thread " + threadName + " was interrupted.", e);
                        }
                        warn = new PoolingException("Thread " + threadName + " slept for " + (System.currentTimeMillis() - sleepStartTime)
                            + "ms.");
                        LOG.warn(warn.getMessage(), warn);
                        if (timedOut) {
                            idleAvailable.signal();
                            throw new PoolingException("Wait time exceeded. Active: " + data.numActive() + ", Idle: " + data.numIdle()
                                + ", Waiting: " + lock.getWaitQueueLength(idleAvailable) + ", Time: " + getWaitTime(startTime));
                        }
                        continue;
                    default:
                        throw new IllegalStateException("Unknown exhausted action: " + exhaustedAction);
                    }
                }
                if (null == retval) {
                    data.addCreating();
                }
            } finally {
                lock.unlock();
            }
            // create
            if (null == retval) {
                LOG.trace("Creating object.");
                final T pooled;
                try {
                    pooled = lifecycle.create();
                } catch (Exception e) {
                    lock.lock();
                    try {
                        data.removeCreating();
                        idleAvailable.signal();
                    } finally {
                        lock.unlock();
                    }
                    throw new PoolingException("Cannot create pooled object.", e);
                }
                retval = new PooledData<T>(pooled);
                lock.lock();
                try {
                    data.addActive(retval);
                    data.removeCreating();
                } finally {
                    lock.unlock();
                }
                created = true;
            }
            // LifeCycle
            if (!lifecycle.activate(retval) || (testOnActivate && !lifecycle.validate(retval))) {
                lock.lock();
                try {
                    data.removeActive(retval);
                    idleAvailable.signal();
                } finally {
                    lock.unlock();
                }
                numBroken++;
                lifecycle.destroy(retval.getPooled());
                if (created) {
                    throw new PoolingException("Problem while creating new object.");
                }
                continue;
            }
            final Thread thread = Thread.currentThread();
            retval.setThread(thread);
            retval.touch();
            if (testThreads) {
                retval.setTrace(thread.getStackTrace());
                lock.lock();
                try {
                    data.addByThread(retval);
                } finally {
                    lock.unlock();
                }
            }
            if (LOG.isTraceEnabled()) {
                LOG.trace("Get time: " + getWaitTime(startTime) + ", Created: " + created);
            }
            return retval.getPooled();
        }
        throw new PoolingException("Pool has been stopped.");
    }

    private static final long getWaitTime(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {
        running = false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        lock.lock();
        try {
            return data.isIdleEmpty() && data.isActiveEmpty();
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getNumIdle() {
        lock.lock();
        try {
            return data.numIdle();
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getNumActive() {
        lock.lock();
        try {
            return data.numActive();
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getPoolSize() {
        lock.lock();
        try {
            return data.numActive() + data.numIdle();
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return the number of threads waiting for an object.
     */
    public int getNumWaiting() {
        lock.lock();
        try {
            return lock.getWaitQueueLength(idleAvailable);
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxUseTime() {
        return maxUseTime;
    }

    /**
     * {@inheritDoc}
     */
    public long getMinUseTime() {
        return minUseTime;
    }

    /**
     * {@inheritDoc}
     */
    public int getNumBroken() {
        return numBroken;
    }

    /**
     * {@inheritDoc}
     */
    public void resetMaxUseTime() {
        maxUseTime = 0;
    }

    /**
     * {@inheritDoc}
     */
    public void resetMinUseTime() {
        minUseTime = Long.MAX_VALUE;
    }

    public double getAvgUseTime() {
        double retval = 0;
        for (final long useTime : useTimes) {
            retval += useTime;
        }
        return retval / useTimes.length;
    }

    private final Runnable cleaner = new Runnable() {
        public void run() {
            try {
                Thread thread = Thread.currentThread();
                String origName = thread.getName();
                thread.setName("PoolCleaner");
                ReentrantLockPool.this.run();
                thread.setName(origName);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    };

    public Runnable getCleanerTask() {
        return cleaner;
    }

    private boolean isBelowMinIdle() {
        final int numIdle;
        final int numActive;
        lock.lock();
        try {
            numIdle = data.numIdle();
            numActive = data.numActive();
        } finally {
            lock.unlock();
        }
        final int maxCreate;
        if (-1 == maxActive) {
            maxCreate = Integer.MAX_VALUE;
        } else {
            maxCreate = Math.max(0, maxActive - numActive - numIdle);
        }
        return Math.min(minIdle - numIdle, maxCreate) > 0;
    }

    /**
     * Fills the pool that it provides the minimum count of idle object without
     * exceeding the maximum number objects.
     * @throws PoolingException if creating an object or putting it to the pool
     * fails.
     */
    private void ensureMinIdle() throws PoolingException {
        boolean successfullyAdded = true;
        while (isBelowMinIdle() && successfullyAdded) {
            successfullyAdded &= createObject();
        }
    }

    /**
     * Creates an object and puts it to the pool.
     * @return <code>true</code> if the object has been put into pool.
     * @throws PoolingException if creating the object throws an exception.
     */
    private boolean createObject() throws PoolingException {
        final T pooled;
        try {
            pooled = lifecycle.create();
        } catch (final Exception e) {
            throw new PoolingException("Cannot create pooled object.", e);
        }
        return back(pooled, false);
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        final long startTime = System.currentTimeMillis();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting cleaner run.");
        }
        final List<PooledData<T>> removed = new ArrayList<PooledData<T>>();
        lock.lock();
        try {
            int idleSize = data.numIdle();
            boolean remove = true;
            for (int index = 0; remove && index < idleSize;) {
                final PooledData<T> metaData = data.getIdle(index);
                remove = (
                        // timeout
                        maxIdleTime > 0 && metaData.getTimeDiff() > maxIdleTime
                    ) || (
                        maxLifeTime > 0 && metaData.getLiveTime() > maxLifeTime
                    ) || (
                        // object not valid anymore
                        testOnIdle && !(lifecycle.activate(metaData)
                        && lifecycle.validate(metaData)
                        && lifecycle.deactivate(metaData))
                    );
                if (remove) {
                    data.removeIdle(index);
                    idleSize = data.numIdle();
                    removed.add(metaData);
                    continue;
                }
                index++;
            }
            final Iterator<PooledData<T>> iter = data.listActive();
            while (iter.hasNext()) {
                final PooledData<T> metaData = iter.next();
                if (metaData.getTimeDiff() > maxIdleTime) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Object was not returned. Fetched: ");
                    sb.append(metaData.getTimestamp());
                    sb.append(", UseTime: ");
                    sb.append(metaData.getTimeDiff());
                    sb.append(", ID: ");
                    sb.append(metaData.getIdentifier());
                    sb.append(", Object: ");
                    sb.append(metaData.getPooled());
                    final PoolingException e = new PoolingException(sb.toString());
                    if (testThreads && null != metaData.getTrace()) {
                        e.setStackTrace(metaData.getTrace());
                    }
                    LOG.error(e.getMessage(), e);
                    iter.remove();
                    idleAvailable.signal();
                }
            }
        } finally {
            lock.unlock();
        }
        for (final PooledData<T> metaData : removed) {
            lifecycle.destroy(metaData.getPooled());
        }
        try {
            ensureMinIdle();
        } catch (final PoolingException e) {
            LOG.error("Problem creating the minimum number of connections.", e);
        }
        LOG.trace("Clean run ending. Time: " + getWaitTime(startTime));
    }

    public static class Config implements Cloneable {
        public int minIdle;
        public int maxIdle;
        public long maxIdleTime;
        public int maxActive;
        public long maxWait;
        public long maxLifeTime;
        public ExhaustedActions exhaustedAction;
        public boolean testOnActivate;
        public boolean testOnDeactivate;
        public boolean testOnIdle;
        public boolean testThreads;
        public boolean forceWriteOnly;

        /**
         * Default constructor.
         */
        public Config() {
            super();
            minIdle = 0;
            maxIdle = -1;
            maxIdleTime = 60000;
            maxActive = -1;
            maxWait = 10000;
            maxLifeTime = -1;
            exhaustedAction = ExhaustedActions.GROW;
            testOnActivate = true;
            testOnDeactivate = true;
            testOnIdle = false;
            testThreads = false;
            forceWriteOnly = false;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public Config clone() {
            try {
                return (Config) super.clone();
            } catch (final CloneNotSupportedException e) {
                // Will not appear!
                throw new Error("Assertion failed!", e);
            }
        }
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Database pooling options:\n\tMinimum idle connections: ");
            sb.append(minIdle);
            sb.append("\n\tMaximum idle connections: ");
            sb.append(maxIdle);
            sb.append("\n\tMaximum idle time: ");
            sb.append(maxIdleTime);
            sb.append("ms\n\tMaximum active connections: ");
            sb.append(maxActive);
            sb.append("\n\tMaximum wait time for a connection: ");
            sb.append(maxWait);
            sb.append("ms\n\tMaximum life time of a connection: ");
            sb.append(maxLifeTime);
            sb.append("ms\n\tAction if connections exhausted: ");
            sb.append(exhaustedAction.toString());
            sb.append("\n\tTest connections on activate  : ");
            sb.append(testOnActivate);
            sb.append("\n\tTest connections on deactivate: ");
            sb.append(testOnDeactivate);
            sb.append("\n\tTest idle connections         : ");
            sb.append(testOnIdle);
            sb.append("\n\tTest threads for bad connection usage (SLOW): ");
            sb.append(testThreads);
            sb.append("\n\tForce the use of write connections only: ");
            sb.append(forceWriteOnly);
            return sb.toString();
        }
    }
}
