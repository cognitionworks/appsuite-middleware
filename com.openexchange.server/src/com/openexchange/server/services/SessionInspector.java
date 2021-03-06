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

package com.openexchange.server.services;

import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.exception.OXException;
import com.openexchange.session.Reply;
import com.openexchange.session.Session;
import com.openexchange.session.inspector.Reason;
import com.openexchange.session.inspector.SessionInspectorChain;

/**
 * Registry for tracked {@link SessionInspectorChain} instance.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class SessionInspector {

    private static final SessionInspectorChain NOOP_CHAIN = new SessionInspectorChain() {

        @Override
        public Reply onSessionMiss(String sessionId, HttpServletRequest request, HttpServletResponse response) throws OXException {
            return Reply.CONTINUE;
        }

        @Override
        public Reply onSessionHit(Session session, HttpServletRequest request, HttpServletResponse response) throws OXException {
            return Reply.CONTINUE;
        }

        @Override
        public Reply onAutoLoginFailed(Reason reason, HttpServletRequest request, HttpServletResponse response) throws OXException {
            return Reply.CONTINUE;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }
    };

    private static final SessionInspector SINGLETON = new SessionInspector();

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static SessionInspector getInstance() {
        return SINGLETON;
    }

    // -------------------------------------------------------------------------------------------------

    private final AtomicReference<SessionInspectorChain> reference;

    private SessionInspector() {
        super();
        reference = new AtomicReference<SessionInspectorChain>(NOOP_CHAIN);
    }

    /**
     * Gets the registered {@code SessionInspectorChain}.
     *
     * @return The registered {@code SessionInspectorChain} or <code>null</code>
     */
    public SessionInspectorChain getChain() {
        return reference.get();
    }

    /**
     * Sets the tracked <code>SessionInspectorChain</code> instance.
     *
     * @param service The tracked <code>SessionInspectorChain</code> instance
     * @return <code>true</code> if given <code>SessionInspectorChain</code> instance could be successfully supplied; otherwise <code>false</code>
     */
    public boolean setService(final SessionInspectorChain service) {
        return reference.compareAndSet(NOOP_CHAIN, service);
    }

    /**
     * Drops the tracked <code>SessionInspectorChain</code> instance.
     *
     * @param service The tracked <code>SessionInspectorChain</code> instance
     * @return <code>true</code> if given <code>SessionInspectorChain</code> instance could be successfully dropped; otherwise <code>false</code>
     */
    public boolean dropService(final SessionInspectorChain service) {
        return reference.compareAndSet(service, NOOP_CHAIN);
    }

}
