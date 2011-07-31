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

package com.openexchange.mail.attachment;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.openexchange.exception.OXException;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;

/**
 * {@link AttachmentTokenRegistry}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class AttachmentTokenRegistry implements AttachmentTokenConstants {

    /**
     * The logger.
     */
    protected static final org.apache.commons.logging.Log LOG = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(AttachmentTokenRegistry.class));

    private static volatile AttachmentTokenRegistry singleton;

    /**
     * Gets the singleton instance.
     *
     * @return The singleton instance
     * @throws OXException If instance initialization fails
     */
    public static AttachmentTokenRegistry getInstance() throws OXException {
        AttachmentTokenRegistry tmp = singleton;
        if (null == tmp) {
            synchronized (AttachmentTokenRegistry.class) {
                tmp = singleton;
                if (null == tmp) {
                    singleton = tmp = new AttachmentTokenRegistry();
                }
            }
        }
        return tmp;
    }

    /**
     * Releases the singleton instance.
     */
    public static void releaseInstance() {
        if (null != singleton) {
            synchronized (AttachmentTokenRegistry.class) {
                if (null != singleton) {
                    singleton.dispose();
                    singleton = null;
                }
            }
        }
    }

    private final ConcurrentMap<Key, ConcurrentMap<String, AttachmentToken>> map;

    private final ConcurrentMap<String, AttachmentToken> tokens;

    private final ScheduledTimerTask timerTask;

    /**
     * Initializes a new {@link AttachmentTokenRegistry}.
     *
     * @throws OXException If initialization fails
     */
    private AttachmentTokenRegistry() throws OXException {
        super();
        map = new ConcurrentHashMap<AttachmentTokenRegistry.Key, ConcurrentMap<String, AttachmentToken>>();
        tokens = new ConcurrentHashMap<String, AttachmentToken>();
        final TimerService timerService = ServerServiceRegistry.getInstance().getService(TimerService.class, true);
        final Runnable task = new CleanExpiredTokensRunnable(map, tokens);
        timerTask = timerService.scheduleWithFixedDelay(task, CLEANER_FREQUENCY, CLEANER_FREQUENCY);
    }

    /**
     * Disposes this registry.
     */
    private void dispose() {
        timerTask.cancel(false);
        tokens.clear();
        map.clear();
    }

    /**
     * Drops tokens for given user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    public void dropFor(final int userId, final int contextId) {
        final ConcurrentMap<String, AttachmentToken> userTokens = map.remove(keyFor(userId, contextId));
        if (null != userTokens) {
            for (final Iterator<Entry<String, AttachmentToken>> iter = userTokens.entrySet().iterator(); iter.hasNext();) {
                final Entry<String, AttachmentToken> entry = iter.next();
                tokens.remove(entry.getValue().getId());
                iter.remove();

            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(new StringBuilder("Cleaned user-sensitive attachment tokens for user ").append(userId).append(" in context ").append(
                contextId).toString());
        }
    }

    /**
     * Drops tokens for given session.
     *
     * @param Session The session
     */
    public void dropFor(final Session session) {
        final ConcurrentMap<String, AttachmentToken> userTokens = map.remove(keyFor(session));
        if (null != userTokens) {
            final AttachmentToken token = userTokens.remove(session.getSessionID());
            if (null != token) {
                tokens.remove(token.getId());
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(new StringBuilder("Cleaned user-sensitive attachment tokens for user ").append(session.getUserId()).append(" in context ").append(
                session.getContextId()).toString());
        }
    }

    /**
     * Gets the token for specified token identifier.
     *
     * @param tokenId The token identifier
     * @return The token or <code>null</code> if absent or expired
     */
    public AttachmentToken getToken(final String tokenId) {
        final AttachmentToken attachmentToken = tokens.get(tokenId);
        if (null == attachmentToken) {
            return null;
        }
        if (attachmentToken.isExpired()) {
            tokens.remove(tokenId);
            /*
             * Clean from other map, too
             */
            for (final Iterator<ConcurrentMap<String, AttachmentToken>> iterator = map.values().iterator(); iterator.hasNext();) {
                final ConcurrentMap<String, AttachmentToken> userTokens = iterator.next();
                NextToken: for (final Iterator<AttachmentToken> iter2 = userTokens.values().iterator(); iter2.hasNext();) {
                    final AttachmentToken token = iter2.next();
                    if (token.getId().equals(tokenId)) {
                        iter2.remove();
                        break NextToken;
                    }
                }
                if (userTokens.isEmpty()) {
                    iterator.remove();
                }
            }
            return null;
        }
        return attachmentToken.touch();
    }

    /**
     * Puts specified token into this registry.
     *
     * @param token The token
     * @param session The session providing user data
     */
    public void putToken(final AttachmentToken token, final Session session) {
        final Key key = keyFor(session);
        ConcurrentMap<String, AttachmentToken> userTokens = map.remove(key);
        if (null == userTokens) {
            final ConcurrentMap<String, AttachmentToken> newmap = new ConcurrentHashMap<String, AttachmentToken>();
            userTokens = map.putIfAbsent(key, newmap);
            if (null == userTokens) {
                userTokens = newmap;
            }
        }
        userTokens.put(session.getSessionID(), token);
        tokens.put(token.getId(), token);
    }

    private static Key keyFor(final Session session) {
        return keyFor(session.getUserId(), session.getContextId());
    }

    private static Key keyFor(final int user, final int context) {
        return new Key(user, context);
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

    private static final class CleanExpiredTokensRunnable implements Runnable {

        private final ConcurrentMap<Key, ConcurrentMap<String, AttachmentToken>> rmap;

        private final ConcurrentMap<String, AttachmentToken> rtokens;

        protected CleanExpiredTokensRunnable(final ConcurrentMap<Key, ConcurrentMap<String, AttachmentToken>> rmap, final ConcurrentMap<String, AttachmentToken> rtokens) {
            super();
            this.rmap = rmap;
            this.rtokens = rtokens;
        }

        @Override
        public void run() {
            try {
                for (final Iterator<ConcurrentMap<String, AttachmentToken>> it1 = rmap.values().iterator(); it1.hasNext();) {
                    final ConcurrentMap<String, AttachmentToken> userTokens = it1.next();
                    for (final Iterator<AttachmentToken> it2 = userTokens.values().iterator(); it2.hasNext();) {
                        final AttachmentToken token = it2.next();
                        if (token.isExpired()) {
                            rtokens.remove(token.getId());
                            it2.remove();
                        }
                    }
                }
            } catch (final Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }

    }
}
