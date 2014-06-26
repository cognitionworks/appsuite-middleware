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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.push.mail.notify;

import com.openexchange.exception.OXException;
import com.openexchange.mail.utils.MailFolderUtility;
import com.openexchange.push.PushListener;
import com.openexchange.push.PushUtility;
import com.openexchange.session.Session;

/**
 * {@link MailNotifyPushListener} - The {@link PushListener}.
 *
 */
public final class MailNotifyPushListener implements PushListener {

    /**
     * A placeholder constant for account ID.
     */
    private static final int ACCOUNT_ID = 0;

    /**
     * Gets the account ID constant.
     *
     * @return The account ID constant
     */
    public static int getAccountId() {
        return ACCOUNT_ID;
    }

    /**
     * Initializes a new {@link MailNotifyPushListener}.
     *
     * @param session The needed session to obtain and connect mail access instance
     * @return A new {@link MailNotifyPushListener}.
     */
    public static MailNotifyPushListener newInstance(final Session session) {
        return new MailNotifyPushListener(session);
    }

    /*
     * Member section
     */

    private final Session session;

    private final int userId;

    private final int contextId;

    private volatile boolean started;

    /**
     * Initializes a new {@link MailNotifyPushListener}.
     *
     * @param session The needed session to obtain and connect mail access instance
     */
    private MailNotifyPushListener(final Session session) {
        super();
        this.session = session;
        this.userId = session.getUserId();
        this.contextId = session.getContextId();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(128).append("session-ID=").append(session.getSessionID());
        sb.append(", user=").append(userId).append(", context=").append(contextId);
        return sb.toString();
    }

    /**
     * Opens this listener (if {@link #isIgnoreOnGlobal()} returns <code>false</code>).
     *
     * @throws OXException If listener cannot be opened
     */
    public void open() throws OXException {
        // Nothing to do
    }

    /**
     * Closes this listener.
     */
    public void close() {
        // NOthing to do
    }

    @Override
    public void notifyNewMail() throws OXException {
        PushUtility.triggerOSGiEvent(MailFolderUtility.prepareFullname(ACCOUNT_ID, "INBOX"), session, false);
    }

    /**
     * Checks if this listener has been started.
     *
     * @return <code>true</code> if this listener has been started; otherwise <code>false</code>
     */
    public boolean isStarted() {
        return started;
    }

}
