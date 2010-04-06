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

package com.openexchange.messaging;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link SimpleMessagingMessage}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class SimpleMessagingMessage implements MessagingMessage, MessagingBodyPart {

    private int colorLabel;

    private int flags;

    private String folder;

    private long receivedDate;

    private Collection<String> userFlags;

    private String disposition;

    private String fileName;

    private Map<String, Collection<MessagingHeader>> headers = new HashMap<String, Collection<MessagingHeader>>();

    private String sectionId;

    private MessagingContent content;

    private long size;

    private int threadLevel;

    private MultipartContent parent;

    private ContentType contentType;

    private String id;

    private String picture;

    public int getColorLabel() {
        return colorLabel;
    }

    public int getFlags() {
        return flags;
    }

    public String getFolder() {
        return folder;
    }

    public long getReceivedDate() {
        return receivedDate;
    }

    public Collection<String> getUserFlags() {
        return userFlags;
    }

    public MessagingContent getContent() throws MessagingException {
        return content;
    }

    public String getDisposition() throws MessagingException {
        return disposition;
    }

    public String getFileName() throws MessagingException {
        return fileName;
    }

    public MessagingHeader getFirstHeader(final String name) throws MessagingException {
        final Collection<MessagingHeader> collection = headers.get(name);
        return null == collection ? null : (collection.isEmpty() ? null : collection.iterator().next());
    }

    public Collection<MessagingHeader> getHeader(final String name) {
        return headers.get(name);
    }

    public Map<String, Collection<MessagingHeader>> getHeaders() {
        return headers;
    }

    public String getSectionId() {
        return sectionId;
    }

    public long getSize() {
        return size;
    }

    public int getThreadLevel() {
        return threadLevel;
    }

    public void writeTo(final OutputStream os) throws IOException, MessagingException {
        throw new UnsupportedOperationException();
    }

    public void setColorLabel(final int colorLabel) {
        this.colorLabel = colorLabel;
    }

    public void setFlags(final int flags) {
        this.flags = flags;
    }

    public void setFolder(final String folder) {
        this.folder = folder;
    }

    public void setReceivedDate(final long receivedDate) {
        this.receivedDate = receivedDate;
    }

    public void setUserFlags(final Collection<String> userFlags) {
        this.userFlags = userFlags;
    }

    public void setDisposition(final String disposition) {
        this.disposition = disposition;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public void setHeaders(final Map<String, Collection<MessagingHeader>> headers) {
        this.headers = headers;
    }
    
    public void putHeader(MessagingHeader header) {
        if(headers.containsKey(header.getName())) {
            headers.get(header.getName()).add(header);
        } else {
            headers.put(header.getName(), new ArrayList<MessagingHeader>(Arrays.asList(header)));
        }
        
    }

    public void setSectionId(final String sectionId) {
        this.sectionId = sectionId;
    }

    public void setContent(final String content) {
        this.content = new StringContent(content);
    }

    public void setSize(final long size) {
        this.size = size;
    }

    public void setThreadLevel(final int threadLevel) {
        this.threadLevel = threadLevel;
    }

    public void setContent(final byte[] bytes) {
        this.content = new ByteArrayContent(bytes);
    }

    public void setContent(final MessagingBodyPart... parts) {
        this.content = new MessagingPartArrayContent(parts);
    }

    public MultipartContent getParent() throws MessagingException {
        return parent;
    }

    public void setParent(final MultipartContent parent) {
        this.parent = parent;
    }

    public ContentType getContentType() throws MessagingException {
        return contentType;
    }

    public void setContentType(final ContentType contentType) {
        this.contentType = contentType;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getPicture() {
        return picture;
    }
    
    public void setPicture(String picture) {
        this.picture = picture;
    }

    public void setContentReference(String string) {
      this.content =  new ReferenceContent(string);
    }


}
