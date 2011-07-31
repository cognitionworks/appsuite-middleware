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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import com.openexchange.exception.OXException;
import com.openexchange.java.util.UUIDs;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.utils.MailFolderUtility;
import com.openexchange.session.Session;

/**
 * {@link AttachmentToken}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class AttachmentToken implements AttachmentTokenConstants {

    private final String id;

    private final long ttlMillis;

    private final AtomicLong timeoutStamp;

    private int contextId;

    private int userId;

    private int accountId;

    private String mailId;

    private String attachmentId;

    private String fullName;

    private MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess;

    private String clientIp;

    private String client;

    /**
     * Initializes a new {@link AttachmentToken}.
     */
    public AttachmentToken(final long ttlMillis) {
        super();
        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("ttlMillis must be positive.");
        }
        this.id =
            new StringBuilder(64).append(UUIDs.getUnformattedString(UUID.randomUUID())).append('.').append(
                UUIDs.getUnformattedString(UUID.randomUUID())).toString();
        this.ttlMillis = ttlMillis;
        timeoutStamp = new AtomicLong(System.currentTimeMillis() + ttlMillis);
    }

    /**
     * Sets the access information.
     * 
     * @param accountId The account identifier
     * @param session The session
     * @return This token with access information applied
     */
    public AttachmentToken setAccessInfo(final int accountId, final Session session) {
        this.accountId = accountId;
        this.contextId = session.getContextId();
        this.userId = session.getUserId();
        this.clientIp = session.getLocalIp();
        this.client = session.getClient();
        return this;
    }

    /**
     * Sets the attachment information.
     * 
     * @param mailId The mail identifier
     * @param attachmentId The attachment identifier
     * @return This token with access attachment applied
     */
    public AttachmentToken setAttachmentInfo(final String fullName, final String mailId, final String attachmentId) {
        this.fullName = fullName;
        this.mailId = mailId;
        this.attachmentId = attachmentId;
        return this;
    }

    /**
     * Touches this token.
     * 
     * @return This token with elapse timeout reseted
     */
    public AttachmentToken touch() {
        long cur;
        do {
            cur = timeoutStamp.get();
        } while (!timeoutStamp.compareAndSet(cur, System.currentTimeMillis() + ttlMillis));
        return this;
    }

    /**
     * Gets the token identifier.
     * 
     * @return The token identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Checks if this token is expired.
     * 
     * @return <code>true</code> if this token is expired; otherwise <code>false</code>
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= timeoutStamp.get();
    }

    /**
     * Gets the associated attachment.
     * <p>
     * <b>Note</b>: After calling this method {@link #close()} needs to be called!
     * 
     * @return The associated attachment
     * @throws MailException
     * @see {@link #close()}
     */
    public MailPart getAttachment() throws OXException {
        mailAccess = MailAccess.getInstance(userId, contextId, accountId);
        mailAccess.connect();
        return mailAccess.getMessageStorage().getAttachment(
            MailFolderUtility.prepareMailFolderParam(fullName).getFullname(),
            mailId,
            attachmentId);
    }

    /**
     * Closes associated mail access (if opened)
     */
    public void close() {
        if (null != mailAccess) {
            mailAccess.close(true);
            mailAccess = null;
        }
    }

    /**
     * Gets the client IP address.
     * 
     * @return The client IP address
     */
    public String getClientIp() {
        return clientIp;
    }

    /**
     * Gets the client
     * 
     * @return The client
     */
    public String getClient() {
        return client;
    }

}
