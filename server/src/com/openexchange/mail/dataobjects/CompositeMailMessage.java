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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import com.openexchange.mail.MailException;
import com.openexchange.mail.mime.MIMEDefaultSession;
import com.openexchange.mail.mime.MIMEMailException;
import com.openexchange.mail.mime.MIMETypes;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.datasource.MessageDataSource;

/**
 * {@link CompositeMailMessage} - Extends the common {@link MailMessage} class by the possibility to add extra parts to an existing
 * {@link MailMessage} instance whose MIME type is <code>multipart/*</code>
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CompositeMailMessage extends MailMessage {

    private static final long serialVersionUID = -3153633514125635904L;

    private final MailMessage delegate;

    private final transient List<MailPart> additionalParts;

    private final int delegateEnclosedCount;

    /**
     * Constructor
     * 
     * @param delegate The delegate mail
     * @throws MailException If invocation of {@link MailMessage#getEnclosedCount()} fails
     */
    public CompositeMailMessage(final MailMessage delegate) throws MailException {
        super();
        if (!delegate.getContentType().isMimeType(MIMETypes.MIME_MULTIPART_ALL)) {
            throw new IllegalArgumentException("Specified delegate mail must be of MIME type multipart/*");
        }
        this.delegate = delegate;
        if (delegate.containsAppendVCard()) {
            setAppendVCard(delegate.isAppendVCard());
        }
        if (delegate.containsBcc()) {
            addBcc(delegate.getBcc());
        }
        if (delegate.containsCc()) {
            addCc(delegate.getCc());
        }
        if (delegate.containsColorLabel()) {
            setColorLabel(delegate.getColorLabel());
        }
        if (delegate.containsContentId()) {
            setContentId(delegate.getContentId());
        }
        if (delegate.containsContentType()) {
            setContentType(delegate.getContentType());
        }
        if (delegate.containsContentDisposition()) {
            setContentDisposition(delegate.getContentDisposition());
        }
        if (delegate.containsDispositionNotification()) {
            setDispositionNotification(delegate.getDispositionNotification());
        }
        if (delegate.containsFileName()) {
            setFileName(delegate.getFileName());
        }
        if (delegate.containsFlags()) {
            setFlags(delegate.getFlags());
        }
        if (delegate.containsFolder()) {
            setFolder(delegate.getFolder());
        }
        if (delegate.containsFrom()) {
            addFrom(delegate.getFrom());
        }
        if (delegate.containsHasAttachment()) {
            setHasAttachment(delegate.hasAttachment());
        }
        if (delegate.containsHeaders()) {
            final int len = delegate.getHeadersSize();
            final Iterator<Map.Entry<String, String>> iter = delegate.getHeadersIterator();
            for (int i = 0; i < len; i++) {
                final Map.Entry<String, String> e = iter.next();
                addHeader(e.getKey(), e.getValue());
            }
        }
        if (delegate.containsMsgref()) {
            setMsgref(delegate.getMsgref());
        }
        if (delegate.containsPriority()) {
            setPriority(delegate.getPriority());
        }
        if (delegate.containsReceivedDate()) {
            setReceivedDate(delegate.getReceivedDate());
        }
        if (delegate.containsSentDate()) {
            setSentDate(delegate.getSentDate());
        }
        if (delegate.containsSequenceId()) {
            setSequenceId(delegate.getSequenceId());
        }
        if (delegate.containsSize()) {
            setSize(delegate.getSize());
        }
        if (delegate.containsSubject()) {
            setSubject(delegate.getSubject());
        }
        if (delegate.containsThreadLevel()) {
            setThreadLevel(delegate.getThreadLevel());
        }
        if (delegate.containsTo()) {
            addTo(delegate.getTo());
        }
        setMailId(delegate.getMailId());
        if (delegate.containsUserFlags()) {
            addUserFlags(delegate.getUserFlags());
        }
        additionalParts = new ArrayList<MailPart>();
        delegateEnclosedCount = delegate.getEnclosedCount();
    }

    /**
     * Appends the specified mail part
     * 
     * @param mailPart The mail part to add
     */
    public void addAdditionalParts(final MailPart mailPart) {
        additionalParts.add(mailPart);
    }

    /**
     * Removes the mail part at the specified position. Shifts any subsequent mail parts to the left (subtracts one from their indices).
     * Returns the mail part that was removed.
     * 
     * @param index The index
     * @return The mail part that was removed.
     */
    public MailPart removeAdditionalParts(final int index) {
        return additionalParts.remove(index);
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.dataobjects.MailPart#getContent()
     */
    @Override
    public Object getContent() throws MailException {
        return delegate.getContent();
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.dataobjects.MailPart#getDataHandler()
     */
    @Override
    public DataHandler getDataHandler() throws MailException {
        return delegate.getDataHandler();
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.dataobjects.MailPart#getEnclosedCount()
     */
    @Override
    public int getEnclosedCount() throws MailException {
        return delegateEnclosedCount + additionalParts.size();
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.dataobjects.MailPart#getEnclosedMailPart(int)
     */
    @Override
    public MailPart getEnclosedMailPart(final int index) throws MailException {
        if (delegateEnclosedCount > 0) {
            /*
             * Delegate mail holds enclosed parts
             */
            if (index >= delegateEnclosedCount) {
                try {
                    return additionalParts.get(index - delegateEnclosedCount);
                } catch (final IndexOutOfBoundsException e) {
                    return null;
                }
            }
            return delegate.getEnclosedMailPart(index);
        }
        try {
            return additionalParts.get(index);
        } catch (final IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("No mail part at index " + index);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.dataobjects.MailPart#getInputStream()
     */
    @Override
    public InputStream getInputStream() throws MailException {
        return delegate.getInputStream();
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.dataobjects.MailPart#loadContent()
     */
    @Override
    public void loadContent() throws MailException {
        delegate.loadContent();
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.mail.dataobjects.MailPart#prepareForCaching()
     */
    @Override
    public void prepareForCaching() {
        delegate.prepareForCaching();
    }

    @Override
    public long getMailId() {
        return delegate.getMailId();
    }

    @Override
    public void setMailId(final long id) {
        delegate.setMailId(id);
    }

    @Override
    public int getUnreadMessages() {
        return delegate.getUnreadMessages();
    }

    @Override
    public void setUnreadMessages(final int unreadMessages) {
        delegate.setUnreadMessages(unreadMessages);
    }

    @Override
    public void writeTo(final OutputStream out) throws MailException {
        if (additionalParts.isEmpty()) {
            delegate.writeTo(out);
            return;
        }
        try {
            final MimeMessage mimeMessage = new MimeMessage(MIMEDefaultSession.getDefaultSession());
            /*
             * Copy headers
             */
            final int size = getHeadersSize();
            final Iterator<Map.Entry<String, String>> iter = getHeadersIterator();
            for (int i = 0; i < size; i++) {
                final Map.Entry<String, String> entry = iter.next();
                mimeMessage.addHeader(entry.getKey(), entry.getValue());
            }
            final MimeMultipart mimeMultipart = new MimeMultipart("mixed");
            /*
             * Add parts from delegate
             */
            for (int i = 0; i < delegateEnclosedCount; i++) {
                final MailPart mp = delegate.getEnclosedMailPart(i);
                final MimeBodyPart bodyPart = new MimeBodyPart();
                bodyPart.setDataHandler(new DataHandler(new MessageDataSource(mp.getInputStream(), mp.getContentType())));
                bodyPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, mp.getContentType().toString());
                bodyPart.setDisposition(mp.getContentDisposition().getDisposition());
                if (mp.getFileName() != null) {
                    bodyPart.setFileName(mp.getFileName());
                }
                mimeMultipart.addBodyPart(bodyPart);
            }
            /*
             * Add additional parts
             */
            for (final MailPart mp : additionalParts) {
                final MimeBodyPart bodyPart = new MimeBodyPart();
                bodyPart.setDataHandler(new DataHandler(new MessageDataSource(mp.getInputStream(), mp.getContentType())));
                bodyPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, mp.getContentType().toString());
                bodyPart.setDisposition(mp.getContentDisposition().getDisposition());
                if (mp.getFileName() != null) {
                    bodyPart.setFileName(mp.getFileName());
                }
                mimeMultipart.addBodyPart(bodyPart);
            }
            mimeMessage.setContent(mimeMultipart);
            mimeMessage.writeTo(out);
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e);
        } catch (final IOException e) {
            throw new MailException(MailException.Code.IO_ERROR, e, e.getMessage());
        }
    }

}
