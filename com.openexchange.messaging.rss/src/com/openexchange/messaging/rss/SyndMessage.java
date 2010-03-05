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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.openexchange.messaging.ContentType;
import com.openexchange.messaging.MessagingContent;
import com.openexchange.messaging.MessagingException;
import com.openexchange.messaging.MessagingExceptionCodes;
import com.openexchange.messaging.MessagingHeader;
import com.openexchange.messaging.MessagingMessage;
import com.openexchange.messaging.StringContent;
import com.openexchange.messaging.StringMessageHeader;
import com.openexchange.messaging.MessagingHeader.KnownHeader;
import com.openexchange.messaging.generic.Utility;
import com.openexchange.messaging.generic.internet.MimeContentType;
import com.openexchange.messaging.generic.internet.MimeMessagingBodyPart;
import com.openexchange.messaging.generic.internet.MimeMultipartContent;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

public class SyndMessage implements MessagingMessage {

    private static final String CONTENT_TYPE = "Content-Type";
    private SyndEntry entry;

    private Map<String, Collection<MessagingHeader>> headers = new HashMap<String, Collection<MessagingHeader>>();
    private MessagingContent content;
    private String folder;
    private SyndFeed feed;
     
    public SyndMessage(SyndFeed feed, SyndEntry syndEntry, String folder) throws MessagingException {
        this.entry = syndEntry;
        this.folder = folder;
        this.feed = feed;
        
        addStringHeader(KnownHeader.SUBJECT, syndEntry.getTitle());
        //addStringHeader(KnownHeader.FROM, syndEntry.getAuthor());
        List<SyndContent> contents = syndEntry.getContents();
        // For now we'll only use the first content element
        
        if(contents.size() > 0) {
            SyndContent content = contents.get(0);
            setContent(content);
        } else if (entry.getDescription() != null){
            setContent(entry.getDescription());
        } else if (entry.getTitle() != null) {
            setContent(entry.getTitleEx());
        }
    }

    private void setContent(SyndContent content) throws MessagingException {
        String type = content.getType();
        if(type == null) {
            type = "text/plain";
        }
        if(knowsType(type)) {
            if(!type.startsWith("text")) {
                type = "text/"+type;
            }
        }
        
        if( isHTML(type) ) {
            String textVersion = Utility.textFormat(content.getValue());
            
            MimeMultipartContent multipart = new MimeMultipartContent();
            MimeMessagingBodyPart textPart = new MimeMessagingBodyPart();
            textPart.setContent(new StringContent(textVersion), "text/plain");
            
            multipart.addBodyPart(textPart);
            
            MimeMessagingBodyPart htmlPart = new MimeMessagingBodyPart();
            htmlPart.setContent(new StringContent(content.getValue()), type);
            
            multipart.addBodyPart(htmlPart);
            
            MimeContentType contentType = new MimeContentType("multipart/alternative");
            addHeader(KnownHeader.CONTENT_TYPE, contentType);
            
            this.content = multipart;
        } else {
            MimeContentType contentType = new MimeContentType(type);
            addHeader(KnownHeader.CONTENT_TYPE, contentType);
            this.content = new StringContent(content.getValue());
        }
        
    }

    private boolean isHTML(String type) {
        return type.endsWith("html");
    }

    private boolean knowsType(String type) {
        return type.contains("plain") || type.contains("html");
    }

    private void addStringHeader(KnownHeader header, String value) {
        headers.put(header.toString(), Arrays.asList((MessagingHeader)new StringMessageHeader(header.toString(), value)));
    }

    private void addHeader(KnownHeader header, MessagingHeader value) {
        headers.put(header.toString(), Arrays.asList(value));
    }
    
    public int getColorLabel() throws MessagingException {
        return -1;
    }

    public int getFlags() throws MessagingException {
        return -1;
    }

    public String getFolder() {
        return folder;
    }

    public String getId() {
        return entry.getLink();
    }

    public long getReceivedDate() {
        return ((Date) tryThese(entry.getPublishedDate(), entry.getUpdatedDate(), new Date(-1))).getTime();
    }

    public int getThreadLevel() {
        return -1;
    }

    public Collection<String> getUserFlags() throws MessagingException {
        List categories = entry.getCategories();
        if(categories == null) {
            return null;
        }
        List<String> strings = new LinkedList<String>();
        for(Object cat : categories) {
            strings.add(cat.toString());
        }
        return strings;
    }

    public MessagingContent getContent() throws MessagingException {
        return content;
    }

    public ContentType getContentType() throws MessagingException {
        return (ContentType) getFirstHeader(CONTENT_TYPE);
    }

    public String getDisposition() throws MessagingException {
        return INLINE;
    }

    public String getFileName() throws MessagingException {
        return null;
    }

    public MessagingHeader getFirstHeader(String name) throws MessagingException {
        if(headers.containsKey(name)) {
            return headers.get(name).iterator().next();
        }
        return null;
    }

    public Collection<MessagingHeader> getHeader(String name) throws MessagingException {
        if(headers.containsKey(name)) {
            return headers.get(name);
        }
        return null;
    }

    public Map<String, Collection<MessagingHeader>> getHeaders() throws MessagingException {
        return headers;
    }

    public String getSectionId() {
        return null;
    }

    public long getSize() throws MessagingException {
        return 0;
    }

    public void writeTo(OutputStream os) throws IOException, MessagingException {
        throw MessagingExceptionCodes.OPERATION_NOT_SUPPORTED.create();
    }
    
    public String getPicture() {
        SyndFeed source = (entry.getSource() != null) ? entry.getSource() : feed;
        if(null != source.getImage()) {
            return source.getImage().getUrl();
        }
        return null;
    }
    
    protected Object tryThese(Object...objects) {
        for (Object object : objects) {
            if(object != null) {
                return object;
            }
        }
        return null;
    }

}