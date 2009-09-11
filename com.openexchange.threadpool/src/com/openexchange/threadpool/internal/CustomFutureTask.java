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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.threadpool.internal;

import java.util.concurrent.FutureTask;
import com.openexchange.threadpool.RefusedExecutionBehavior;
import com.openexchange.threadpool.Task;

/**
 * {@link CustomFutureTask} - A custom {@link FutureTask}.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class CustomFutureTask<V> extends FutureTask<V> {

    private final Task<V> task;

    private final RefusedExecutionBehavior<V> refusedExecutionBehavior;

    /**
     * Initializes a new {@link CustomFutureTask}.
     * 
     * @param task The task
     */
    public CustomFutureTask(final Task<V> task) {
        this(task, null);
    }

    /**
     * Initializes a new {@link CustomFutureTask}.
     * 
     * @param task The task
     * @param refusedExecutionBehavior The refused execution behavior
     */
    public CustomFutureTask(final Task<V> task, final RefusedExecutionBehavior<V> refusedExecutionBehavior) {
        super(task);
        this.task = task;
        this.refusedExecutionBehavior = refusedExecutionBehavior;
    }

    /**
     * Gets the task performed by this future task.
     * 
     * @return The task
     */
    public Task<V> getTask() {
        return task;
    }

    /**
     * Gets the refused execution behavior.
     * 
     * @return The refused execution behavior or <code>null</code> if task has no individual behavior
     */
    public RefusedExecutionBehavior<V> getRefusedExecutionBehavior() {
        return refusedExecutionBehavior;
    }

    /**
     * Sets the result of this future to the given value unless this future has already been set or has been canceled.
     * 
     * @param v The value
     */
    @Override
    public void set(final V v) {
        super.set(v);
    }

    /**
     * Causes this future to report an <tt>ExecutionException</tt> with the given throwable as its cause, unless this Future has already
     * been set or has been canceled.
     * 
     * @param t The cause of failure.
     */
    @Override
    public void setException(final Throwable t) {
        super.setException(t);
    }

}
