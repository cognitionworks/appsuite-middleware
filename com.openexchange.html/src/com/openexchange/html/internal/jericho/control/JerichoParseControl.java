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

package com.openexchange.html.internal.jericho.control;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.DelayQueue;

/**
 * {@link JerichoParseControl}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.2
 */
public class JerichoParseControl {

    private static final JerichoParseControl INSTANCE = new JerichoParseControl();

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static JerichoParseControl getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------------------------------------------------------------

    private final DelayQueue<JerichoParseTask> queue;

    /**
     * Initializes a new {@link JerichoParseControl}.
     */
    private JerichoParseControl() {
        super();
        queue = new DelayQueue<JerichoParseTask>();
    }

    /**
     * Adds the specified task.
     *
     * @param task The task
     * @return <tt>true</tt>
     */
    public boolean add(JerichoParseTask task) {
        return queue.offer(task);
    }

    /**
     * Removes the specified task.
     *
     * @param task The task to remove
     * @return <code>true</code> if such a task was removed; otherwise <code>false</code>
     */
    public boolean remove(JerichoParseTask task) {
        return queue.remove(task);
    }

    /**
     * Await expired parse tasks from this control.
     *
     * @return The expired parse tasks
     * @throws InterruptedException If interrupted while waiting
     */
    List<JerichoParseTask> awaitExpired() throws InterruptedException {
        JerichoParseTask expired = queue.take();
        List<JerichoParseTask> expirees = new LinkedList<JerichoParseTask>();
        expirees.add(expired);
        queue.drainTo(expirees);
        return expirees;
    }

    /**
     * Removes expired parse tasks from this control.
     *
     * @return The expired parse tasks
     */
    List<JerichoParseTask> removeExpired() {
        List<JerichoParseTask> expirees = new LinkedList<JerichoParseTask>();
        queue.drainTo(expirees);
        return expirees;
    }

}
