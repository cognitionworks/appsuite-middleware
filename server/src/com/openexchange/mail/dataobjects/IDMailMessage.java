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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.mail.dataobjects;

import java.io.InputStream;
import javax.activation.DataHandler;
import com.openexchange.exception.OXException;

/**
 * {@link IDMailMessage} - Supports only {@link #getMailId()} and {@link #getFolder()}.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class IDMailMessage extends MailMessage {

    private static final long serialVersionUID = -8945006270321242506L;

    private long uid;

    private String mailId;

    private char separator;

    private int seqnum;

    /**
     * Initializes a new {@link IDMailMessage}
     */
    public IDMailMessage(final String mailId, final String folder) {
        super();
        this.mailId = mailId;
        if (null == mailId) {
            uid = -1L;
        } else {
            try {
                uid = Long.parseLong(mailId);
            } catch (final NumberFormatException e) {
                uid = -1L;
            }
        }
        setFolder(folder);
    }

    @Override
    public String getMailId() {
        return mailId;
    }

    @Override
    public int getUnreadMessages() {
        throw new UnsupportedOperationException("IDMailMessage.getUnreadMessages() not supported");
    }

    @Override
    public void setMailId(final String id) {
        mailId = id;
        if (null == id) {
            uid = -1L;
        } else {
            try {
                uid = Long.parseLong(id);
            } catch (final NumberFormatException e) {
                uid = -1L;
            }
        }
    }

    /**
     * Gets the UID
     * 
     * @return The UID or <code>-1</code> if absent
     */
    public long getUid() {
        return uid;
    }

    /**
     * Sets the UID
     * 
     * @param uid The UID to set or <code>-1</code> to indicate absence
     */
    public void setUid(final long uid) {
        this.uid = uid;
    }

    /**
     * Gets the separator.
     * 
     * @return The separator
     */
    public char getSeparator() {
        return separator;
    }

    /**
     * Sets the separator.
     * 
     * @param separator The separator to set
     */
    public void setSeparator(final char separator) {
        this.separator = separator;
    }

    /**
     * Gets the sequence number.
     * 
     * @return The sequence number
     */
    public int getSeqnum() {
        return seqnum;
    }

    /**
     * Sets the sequence number.
     * 
     * @param seqnum The sequence number to set
     */
    public void setSeqnum(final int seqnum) {
        this.seqnum = seqnum;
    }

    @Override
    public void setUnreadMessages(final int unreadMessages) {
        throw new UnsupportedOperationException("IDMailMessage.setUnreadMessages() not supported");
    }

    @Override
    public Object getContent() throws OXException {
        throw new UnsupportedOperationException("IDMailMessage.getContent() not supported");
    }

    @Override
    public DataHandler getDataHandler() throws OXException {
        throw new UnsupportedOperationException("IDMailMessage.getDataHandler() not supported");
    }

    @Override
    public int getEnclosedCount() throws OXException {
        throw new UnsupportedOperationException("IDMailMessage.getEnclosedCount() not supported");
    }

    @Override
    public MailPart getEnclosedMailPart(final int index) throws OXException {
        throw new UnsupportedOperationException("IDMailMessage.getEnclosedMailPart() not supported");
    }

    @Override
    public InputStream getInputStream() throws OXException {
        throw new UnsupportedOperationException("IDMailMessage.getInputStream() not supported");
    }

    @Override
    public void loadContent() throws OXException {
        // Nothing to do
    }

    @Override
    public void prepareForCaching() {
        // Nothing to do
    }

}
