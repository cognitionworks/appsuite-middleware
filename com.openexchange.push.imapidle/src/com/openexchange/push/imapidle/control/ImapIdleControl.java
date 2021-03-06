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

package com.openexchange.push.imapidle.control;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.DelayQueue;
import com.openexchange.push.imapidle.ImapIdlePushListener;
import com.sun.mail.imap.IMAPFolder;

/**
 * {@link ImapIdleControl}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.1
 */
public class ImapIdleControl {

    private final DelayQueue<ImapIdleRegistration> queue;

    /**
     * Initializes a new {@link ImapIdleControl}.
     */
    public ImapIdleControl() {
        super();
        queue = new DelayQueue<ImapIdleRegistration>();
    }

    /**
     * Adds the specified IMAP-IDLE push listener with given timeout.
     *
     * @param listener The listener to add
     * @param imapFolder The IMAP folder to idle on
     * @param timeoutMillis The timeout
     * @return <tt>true</tt>
     */
    public boolean add(ImapIdlePushListener listener, IMAPFolder imapFolder, long timeoutMillis) {
        return queue.offer(new ImapIdleRegistration(listener, imapFolder, timeoutMillis));
    }

    /**
     * Removes the specified IMAP-IDLE push listener.
     *
     * @param listener The listener to remove
     * @return <code>true</code> if such a listener was removed; otherwise <code>false</code>
     */
    public boolean remove(ImapIdlePushListener listener) {
        return queue.remove(new ImapIdleRegistration(listener));
    }

    /**
     * Removes expired push listeners from this control.
     *
     * @return The expired push listeners
     */
    List<ImapIdleRegistration> removeExpired() {
        List<ImapIdleRegistration> expirees = new LinkedList<ImapIdleRegistration>();
        queue.drainTo(expirees);
        return expirees;
    }

    /**
     * Awaits expired push listeners from this control.
     *
     * @return The expired push listeners
     * @throws InterruptedException If waiting thread is interrupted
     */
    List<ImapIdleRegistration> awaitExpired() throws InterruptedException {
        ImapIdleRegistration taken = queue.take();

        List<ImapIdleRegistration> expirees = new LinkedList<ImapIdleRegistration>();
        expirees.add(taken);

        queue.drainTo(expirees);
        return expirees;
    }

    /**
     * Awaits expired push listeners from this control uninterruptibly.
     *
     * @return The expired push listeners
     */
    List<ImapIdleRegistration> awaitExpiredUninterruptible() {
        boolean interrupted = false;
        try {
            ImapIdleRegistration taken = null;
            while (null == taken) {
                try {
                    taken = queue.take();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }

            List<ImapIdleRegistration> expirees = new LinkedList<ImapIdleRegistration>();
            expirees.add(taken);

            queue.drainTo(expirees);
            return expirees;
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
