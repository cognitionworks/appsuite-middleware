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

package com.openexchange.messaging.twitter;

import static com.openexchange.messaging.twitter.TwitterMessagingUtility.checkContent;
import static com.openexchange.messaging.twitter.TwitterMessagingUtility.parseUnsignedLong;
import com.openexchange.messaging.MessagingAccount;
import com.openexchange.messaging.MessagingAccountTransport;
import com.openexchange.messaging.MessagingAddressHeader;
import com.openexchange.messaging.MessagingException;
import com.openexchange.messaging.MessagingHeader;
import com.openexchange.messaging.MessagingMessage;
import com.openexchange.messaging.StringContent;
import com.openexchange.messaging.twitter.services.TwitterMessagingServiceRegistry;
import com.openexchange.server.ServiceException;
import com.openexchange.session.Session;
import com.openexchange.twitter.Paging;
import com.openexchange.twitter.TwitterAccess;
import com.openexchange.twitter.TwitterException;
import com.openexchange.twitter.TwitterExceptionCodes;
import com.openexchange.twitter.TwitterService;

/**
 * {@link TwitterMessagingAccountTransport}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class TwitterMessagingAccountTransport implements MessagingAccountTransport {

    private final MessagingAccount account;

    private final TwitterService twitterService;

    private final TwitterAccess twitterAccess;

    private final Session session;

    private boolean connected;

    /**
     * Initializes a new {@link TwitterMessagingAccountTransport}.
     * 
     * @throws MessagingException If initialization fails
     */
    public TwitterMessagingAccountTransport(final MessagingAccount account, final Session session) throws MessagingException {
        super();
        this.session = session;
        this.account = account;
        try {
            twitterService = TwitterMessagingServiceRegistry.getServiceRegistry().getService(TwitterService.class, true);
            final String login = (String) account.getConfiguration().get(TwitterConstants.TWITTER_LOGIN);
            final String password = (String) account.getConfiguration().get(TwitterConstants.TWITTER_PASSWORD);
            twitterAccess = twitterService.getTwitterAccess(login, password);
        } catch (final ServiceException e) {
            throw new MessagingException(e);
        }
    }

    public void transport(final MessagingMessage message, final MessagingAddressHeader recipients) throws MessagingException {
        final String messageType;
        {
            final MessagingHeader header = message.getFirstHeader(MessagingHeader.KnownHeader.MESSAGE_TYPE.toString());
            messageType = null == header ? null : header.getValue();
        }
        if (TwitterConstants.TYPE_DIRECT_MESSAGE.equalsIgnoreCase(messageType)) {
            /*
             * A direct message
             */
            try {
                final StringContent content = checkContent(StringContent.class, message);
                final String screenName;
                {
                    final MessagingHeader header = message.getFirstHeader(MessagingHeader.KnownHeader.TO.toString());
                    screenName = null == header ? null : header.getValue();
                }
                if (null == screenName) {
                    throw TwitterExceptionCodes.MISSING_PROPERTY.create(MessagingHeader.KnownHeader.TO.toString());
                }
                twitterAccess.sendDirectMessage(screenName, content.toString());
            } catch (final TwitterException e) {
                throw new MessagingException(e);
            }
        } else if (TwitterConstants.TYPE_RETWEET.equalsIgnoreCase(messageType)) {
            /*
             * A retweet
             */
            try {
                final StringContent content = checkContent(StringContent.class, message);
                final long inReplyTo;
                {
                    final MessagingHeader header = message.getFirstHeader(TwitterConstants.HEADER_STATUS_ID);
                    inReplyTo = null == header ? -1L : parseUnsignedLong(header.getValue());
                }
                if (inReplyTo > 0) {
                    twitterAccess.updateStatus(content.toString(), inReplyTo);
                } else {
                    twitterAccess.updateStatus(content.toString());
                }
            } catch (final TwitterException e) {
                throw new MessagingException(e);
            }

        } else {
            /*
             * A normal tweet
             */
            try {
                final StringContent content = checkContent(StringContent.class, message);
                twitterAccess.updateStatus(content.toString());
            } catch (final TwitterException e) {
                throw new MessagingException(e);
            }
        }
    }

    public void close() {
        connected = false;
    }

    public void connect() throws MessagingException {
        connected = true;
    }

    public boolean ping() throws MessagingException {
        try {
            final Paging paging = twitterService.newPaging();
            paging.count(1);
            twitterAccess.getFriendsTimeline(paging);
            return true;
        } catch (final TwitterException e) {
            return false;
        }
    }

    public boolean isConnected() {
        return connected;
    }

}
