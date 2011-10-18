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

package com.openexchange.chat.db.osgi;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import com.openexchange.chat.ChatService;
import com.openexchange.chat.Presence;
import com.openexchange.chat.db.DBChatAccess;
import com.openexchange.chat.db.DBChatService;
import com.openexchange.chat.db.DBChatServiceLookup;
import com.openexchange.chat.db.DBRoster;
import com.openexchange.chat.util.ChatUserImpl;
import com.openexchange.chat.util.PresenceImpl;
import com.openexchange.context.ContextService;
import com.openexchange.database.DatabaseService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.id.IDGeneratorService;
import com.openexchange.server.osgiservice.HousekeepingActivator;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondEventConstants;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.timer.TimerService;
import com.openexchange.user.UserService;

/**
 * {@link DBChatActivator}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class DBChatActivator extends HousekeepingActivator {

    protected static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(DBChatActivator.class));

    /**
     * Initializes a new {@link DBChatActivator}.
     */
    public DBChatActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] {
            ThreadPoolService.class, TimerService.class, SessiondService.class, DatabaseService.class, UserService.class,
            ContextService.class, IDGeneratorService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        DBChatServiceLookup.set(this);
        /*
         * Register service
         */
        registerService(ChatService.class, new DBChatService());
        /*
         * Register event handler
         */
        {
            final Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>(1);
            serviceProperties.put(EventConstants.EVENT_TOPIC, SessiondEventConstants.getAllTopics());
            registerService(EventHandler.class, new EventHandler() {

                @Override
                public void handleEvent(final Event event) {
                    final String topic = event.getTopic();
                    if (SessiondEventConstants.TOPIC_REMOVE_DATA.equals(topic)) {
                        @SuppressWarnings("unchecked") final Map<String, Session> container =
                            (Map<String, Session>) event.getProperty(SessiondEventConstants.PROP_CONTAINER);
                        for (final Session session : container.values()) {
                            handleRemovedSession(session);
                        }
                    } else if (SessiondEventConstants.TOPIC_REMOVE_SESSION.equals(topic)) {
                        final Session session = (Session) event.getProperty(SessiondEventConstants.PROP_SESSION);
                        handleRemovedSession(session);
                    } else if (SessiondEventConstants.TOPIC_REMOVE_CONTAINER.equals(topic)) {
                        @SuppressWarnings("unchecked") final Map<String, Session> container =
                            (Map<String, Session>) event.getProperty(SessiondEventConstants.PROP_CONTAINER);
                        for (final Session session : container.values()) {
                            handleRemovedSession(session);
                        }
                    } else if (SessiondEventConstants.TOPIC_ADD_SESSION.equals(topic)) {
                        final Session session = (Session) event.getProperty(SessiondEventConstants.PROP_SESSION);
                        handleAddedSession(session);
                    } else if (SessiondEventConstants.TOPIC_REACTIVATE_SESSION.equals(topic)) {
                        @SuppressWarnings("unchecked") final Map<String, Session> container =
                            (Map<String, Session>) event.getProperty(SessiondEventConstants.PROP_CONTAINER);
                        for (final Session session : container.values()) {
                            handleAddedSession(session);
                        }
                    }
                }

                private void handleAddedSession(final Session session) {
                    try {
                        final DBRoster dbRoster = DBRoster.optRosterFor(session.getContextId());
                        if (null != dbRoster) {
                            final PresenceImpl presence = new PresenceImpl();
                            final int userId = session.getUserId();
                            presence.setFrom(new ChatUserImpl(String.valueOf(userId), getUserName(userId, session.getContextId())));
                            dbRoster.notifyRosterListeners(presence);
                        }
                    } catch (final Exception e) {
                        // Failed handling session
                        LOG.warn("Failed handling tracked added session for LIST/LSUB cache.", e);
                    }
                }

                private void handleRemovedSession(final Session session) {
                    try {
                        if (null != getService(SessiondService.class).getAnyActiveSessionForUser(session.getUserId(), session.getContextId())) {
                            // Other active session present
                            return;
                        }
                        /*-
                         * Last session gone: clean up
                         * 
                         * 1. Mark as unavailable in roster
                         * 2. Remove associated chat access
                         */
                        final DBRoster dbRoster = DBRoster.optRosterFor(session.getContextId());
                        if (null != dbRoster) {
                            final PresenceImpl presence = new PresenceImpl(Presence.Type.UNAVAILABLE);
                            final int userId = session.getUserId();
                            presence.setFrom(new ChatUserImpl(String.valueOf(userId), getUserName(userId, session.getContextId())));
                            dbRoster.notifyRosterListeners(presence);
                        }
                        // Drop chat access for associated user
                        DBChatAccess.removeDbChatAccess(session);
                    } catch (final Exception e) {
                        // Failed handling session
                        LOG.warn("Failed handling tracked removed session for LIST/LSUB cache.", e);
                    }
                }

                private String getUserName(final int userId, final int cid) throws OXException {
                    final Context ctx = getService(ContextService.class).getContext(cid);
                    return getService(UserService.class).getUser(userId, ctx).getDisplayName();
                }

            },
                serviceProperties);
        }
    }

    @Override
    protected void cleanUp() {
        super.cleanUp();
        DBChatServiceLookup.set(null);
    }

}
