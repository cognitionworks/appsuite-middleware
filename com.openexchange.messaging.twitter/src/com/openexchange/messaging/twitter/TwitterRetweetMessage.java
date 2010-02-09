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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.openexchange.messaging.ContentDisposition;
import com.openexchange.messaging.ContentType;
import com.openexchange.messaging.MessagingContent;
import com.openexchange.messaging.MessagingException;
import com.openexchange.messaging.MessagingFolder;
import com.openexchange.messaging.MessagingHeader;
import com.openexchange.messaging.MessagingMessage;
import com.openexchange.messaging.MessagingPart;
import com.openexchange.messaging.StringContent;
import com.openexchange.messaging.StringMessageHeader;
import com.openexchange.messaging.generic.internet.MimeAddressMessagingHeader;
import com.openexchange.messaging.generic.internet.MimeContentDisposition;
import com.openexchange.messaging.generic.internet.MimeContentType;
import com.openexchange.twitter.Status;
import com.openexchange.twitter.User;

/**
 * {@link TwitterRetweetMessage}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class TwitterRetweetMessage implements MessagingMessage {

    private static final ContentType CONTENT_TYPE;

    private static final ContentDisposition CONTENT_DISPOSITION;

    static {
        final ContentType contentType = new MimeContentType();
        contentType.setPrimaryType("text");
        contentType.setSubType("plain");
        CONTENT_TYPE = contentType;

        final ContentDisposition contentDisposition = new MimeContentDisposition();
        contentDisposition.setDisposition(MessagingPart.INLINE);
        CONTENT_DISPOSITION = contentDisposition;
    }

    private final Map<String, Collection<MessagingHeader>> headers;

    private final StringContent content;

    private final long size;

    /**
     * Initializes a new {@link TwitterRetweetMessage}.
     * 
     * @param status The status to retweet
     * @param user The authenticated user
     */
    public TwitterRetweetMessage(final Status status, final User user) {
        super();
        /*
         * Assign string content and size
         */
        final String retweet;
        {
            final String text = status.getText();

            retweet =
                new StringBuilder(text.length() + 16).append("RT @").append(status.getUser().getScreenName()).append(": ").append(text).toString();
            final int len = retweet.length();

            final int maxTweetLength = TwitterConstants.MAX_TWEET_LENGTH;
            if (len > maxTweetLength) {
                content = new StringContent(retweet.substring(0, maxTweetLength));
                size = maxTweetLength;
            } else {
                content = new StringContent(retweet);
                size = len;
            }
        }
        /*
         * Assign headers
         */
        {
            final Map<String, Collection<MessagingHeader>> m = new HashMap<String, Collection<MessagingHeader>>(16);
            m.put(CONTENT_TYPE.getName(), wrap(CONTENT_TYPE));
            m.put(CONTENT_DISPOSITION.getName(), wrap(CONTENT_DISPOSITION));
            {
                final String name = MessagingHeader.KnownHeader.FROM.toString();
                m.put(name, wrap(MimeAddressMessagingHeader.valueOfPlain(name, user.getName(), user.getScreenName())));
            }
            {
                final String name = MessagingHeader.KnownHeader.SUBJECT.toString();
                m.put(name, getSimpleHeader(name, retweet));
            }
            {
                final String name = MessagingHeader.KnownHeader.MESSAGE_TYPE.toString();
                m.put(name, getSimpleHeader(name, TwitterConstants.TYPE_RETWEET));
            }
            {
                final String name = TwitterConstants.HEADER_STATUS_ID;
                m.put(name, getSimpleHeader(name, String.valueOf(status.getId())));
            }
            headers = Collections.unmodifiableMap(m);
        }
    }

    public int getColorLabel() {
        return 0;
    }

    public int getFlags() {
        return 0;
    }

    public String getFolder() {
        return MessagingFolder.ROOT_FULLNAME;
    }

    public long getReceivedDate() {
        return -1L;
    }

    public Collection<String> getUserFlags() {
        return Collections.emptyList();
    }

    public MessagingContent getContent() throws MessagingException {
        return content;
    }

    public String getDisposition() throws MessagingException {
        return MessagingPart.INLINE;
    }

    public String getFileName() throws MessagingException {
        return null;
    }

    public MessagingHeader getFirstHeader(final String name) throws MessagingException {
        final Collection<MessagingHeader> collection = getHeader(name);
        return null == collection ? null : (collection.isEmpty() ? null : collection.iterator().next());
    }

    public Collection<MessagingHeader> getHeader(final String name) {
        return headers.get(name);
    }

    public Map<String, Collection<MessagingHeader>> getHeaders() {
        return headers;
    }

    public String getSectionId() {
        return null;
    }

    public void writeTo(final OutputStream os) throws IOException, MessagingException {
        // TODO Auto-generated method stub
    }

    public long getSize() {
        return size;
    }

    public int getThreadLevel() {
        return 0;
    }

    public ContentType getContentType() throws MessagingException {
        return CONTENT_TYPE;
    }

    private static Collection<MessagingHeader> wrap(final MessagingHeader... headers) {
        return Collections.unmodifiableCollection(Arrays.asList(headers));
    }

    private static Collection<MessagingHeader> getSimpleHeader(final String name, final String value) {
        return wrap(new StringMessageHeader(name, value));
    }

    public String getId() {
        return null;
    }

    public String getPicture() {
        return null;
    }

}
