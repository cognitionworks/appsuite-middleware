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

package com.openexchange.messaging.rss;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.openexchange.messaging.IndexRange;
import com.openexchange.messaging.MessagingAccountManager;
import com.openexchange.messaging.MessagingException;
import com.openexchange.messaging.MessagingExceptionCodes;
import com.openexchange.messaging.MessagingField;
import com.openexchange.messaging.MessagingMessage;
import com.openexchange.messaging.MessagingMessageAccess;
import com.openexchange.messaging.OrderDirection;
import com.openexchange.messaging.SearchTerm;
import com.openexchange.messaging.generic.MessagingComparator;
import com.openexchange.session.Session;
import com.sun.syndication.fetcher.FeedFetcher;


/**
 * {@link RSSMessageAccess}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class RSSMessageAccess extends RSSCommon implements MessagingMessageAccess {

    private FeedFetcher feedFetcher;
    private MessagingAccountManager accounts;
    
    private FeedAdapter feed = null;
    
    public RSSMessageAccess(int accountId, Session session, FeedFetcher fetcher, MessagingAccountManager accounts) {
        super(accountId, session);
        this.accountId = accountId;
        this.session = session;
        this.feedFetcher = fetcher;
        this.accounts = accounts;
    }

    public void appendMessages(final String folder, final MessagingMessage[] messages) throws MessagingException {
        checkFolder(folder);
        throw MessagingExceptionCodes.OPERATION_NOT_SUPPORTED.create(RSSMessagingService.ID);
    }


    public List<String> copyMessages(final String sourceFolder, final String destFolder, final String[] messageIds, final boolean fast) throws MessagingException {
        checkFolder(sourceFolder);
        checkFolder(destFolder);
        throw MessagingExceptionCodes.OPERATION_NOT_SUPPORTED.create(RSSMessagingService.ID);
    }

    public void deleteMessages(final String folder, final String[] messageIds, final boolean hardDelete) throws MessagingException {
        checkFolder(folder);
        throw MessagingExceptionCodes.OPERATION_NOT_SUPPORTED.create(RSSMessagingService.ID);
    }


    public List<MessagingMessage> getAllMessages(String folder, IndexRange indexRange, MessagingField sortField, OrderDirection order, MessagingField... fields) throws MessagingException {
        return searchMessages(folder, indexRange, sortField, order, null, fields);
    }

    public MessagingMessage getMessage(String folder, String id, boolean peek) throws MessagingException {
        checkFolder(folder);
        return loadFeed().get(id);
    }

    public List<MessagingMessage> getMessages(String folder, String[] messageIds, MessagingField[] fields) throws MessagingException {
        checkFolder(folder);
        List<MessagingMessage> messages = new ArrayList<MessagingMessage>(messageIds.length);
        for (String id : messageIds) {
            messages.add(getMessage(folder, id, true));
        }
        return messages;
    }

    public List<String> moveMessages(String sourceFolder, String destFolder, String[] messageIds, boolean fast) throws MessagingException {
        checkFolder(sourceFolder);
        checkFolder(destFolder);
        throw MessagingExceptionCodes.OPERATION_NOT_SUPPORTED.create(RSSMessagingService.ID);
    }

    public MessagingMessage perform(String folder, String id, String action) throws MessagingException {
        throw MessagingExceptionCodes.UNKNOWN_ACTION.create(action);
    }

    public MessagingMessage perform(String action) throws MessagingException {
        throw MessagingExceptionCodes.UNKNOWN_ACTION.create(action);
    }

    public MessagingMessage perform(MessagingMessage message, String action) throws MessagingException {
        throw MessagingExceptionCodes.UNKNOWN_ACTION.create(action);
    }

    public List<MessagingMessage> searchMessages(String folder, IndexRange indexRange, MessagingField sortField, OrderDirection order, SearchTerm<?> searchTerm, MessagingField[] fields) throws MessagingException {
        checkFolder(folder);
        List<SyndMessage> messages = loadFeed().getMessages();
        
        messages = filter(messages, searchTerm);
        sort(messages, sortField, order);
        messages = sublist(messages, indexRange);
        
        return new ArrayList<MessagingMessage>(messages);
    }

    private List<SyndMessage> sublist(List<SyndMessage> messages, IndexRange indexRange) {
        if(indexRange == null) {
            return messages;
        }
        int start = Math.min(indexRange.getStart(), messages.size()-1);
        int end = Math.min(indexRange.getEnd(), messages.size()-1);
        
        if (start < 0) {
            start = 0;
        }
        
        if(end < 0) {
            end = 0;
        }
        
        return messages.subList(start, end);
    }

    private void sort(List<SyndMessage> messages, MessagingField sortField, OrderDirection order) throws MessagingException {
        if(sortField == null) {
            return;
        }
        MessagingComparator comparator = new MessagingComparator(sortField);
        try {
            Collections.sort(messages, comparator);
            if(order == OrderDirection.DESC) {
                Collections.reverse(messages);
            }
        } catch (RuntimeException x) {
            Throwable cause = x.getCause();
            if(MessagingException.class.isInstance(cause)) {
                throw (MessagingException) cause;
            }
            throw x;
        }
    }

    private List<SyndMessage> filter(List<SyndMessage> messages, SearchTerm<?> searchTerm) throws MessagingException {
        if(searchTerm == null) {
            return messages;
        }
        
        List<SyndMessage> list = new ArrayList<SyndMessage>(messages.size());
        
        for (SyndMessage syndMessage : list) {
            if(searchTerm.matches(syndMessage)) {
                list.add(syndMessage);
            }
        }
        return list;
    }

    public void updateMessage(MessagingMessage message, MessagingField[] fields) throws MessagingException {
        throw MessagingExceptionCodes.OPERATION_NOT_SUPPORTED.create(RSSMessagingService.ID);
    }
    
    private FeedAdapter loadFeed() throws MessagingException {
        if(feed != null) {
            return feed;
        }
        String url = (String) accounts.getAccount(accountId, session).getConfiguration().get("url");
        
        try {
            return this.feed = new FeedAdapter(feedFetcher.retrieveFeed(new URL(url)), RSSMessagingService.buildFolderId(accountId, ""));
        } catch (Exception e) {
            throw MessagingExceptionCodes.MESSAGING_ERROR.create(e);
        }
    }


}
