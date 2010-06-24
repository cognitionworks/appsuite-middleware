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

package com.openexchange.threadpool;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * {@link SimThreadPoolService}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class SimThreadPoolService implements ThreadPoolService {

    private ExecutorService executor;

    public SimThreadPoolService() {
        super();
        executor = Executors.newSingleThreadExecutor();
    }

    public int getActiveCount() {
        throw new UnsupportedOperationException();
    }

    public long getCompletedTaskCount() {
        throw new UnsupportedOperationException();
    }

    public Executor getExecutor() {
        return executor;
    }

    public int getLargestPoolSize() {
        throw new UnsupportedOperationException();
    }

    public int getPoolSize() {
        throw new UnsupportedOperationException();
    }

    public long getTaskCount() {
        throw new UnsupportedOperationException();
    }

    public <T> CompletionFuture<T> invoke(Collection<? extends Task<T>> tasks) {
        throw new UnsupportedOperationException();
    }

    public <T> CompletionFuture<T> invoke(Task<T>[] tasks) {
        throw new UnsupportedOperationException();
    }

    public <T> CompletionFuture<T> invoke(Collection<? extends Task<T>> tasks, RefusedExecutionBehavior<T> refusedExecutionBehavior) {
        throw new UnsupportedOperationException();
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Task<T>> tasks) {
        throw new UnsupportedOperationException();
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Task<T>> tasks, long timeout) {
        throw new UnsupportedOperationException();
    }

    public boolean isShutdown() {
        return null == executor;
    }

    public boolean isTerminated() {
        return null == executor;
    }

    public <T> Future<T> submit(Task<T> task) {
        return executor.submit(task);
    }

    public <T> Future<T> submit(Task<T> task, RefusedExecutionBehavior<T> refusedExecutionBehavior) {
        throw new UnsupportedOperationException();
    }

    public void shutdown() {
        executor.shutdown();
        executor = null;
    }
}
