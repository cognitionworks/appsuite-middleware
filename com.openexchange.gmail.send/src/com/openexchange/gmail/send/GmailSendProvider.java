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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.gmail.send;

import java.util.Map;
import com.openexchange.exception.OXException;
import com.openexchange.gmail.send.config.GmailSendProperties;
import com.openexchange.gmail.send.config.GmailSendSessionProperties;
import com.openexchange.gmail.send.dataobjects.GmailSendBodyPart;
import com.openexchange.gmail.send.dataobjects.GmailSendDataPart;
import com.openexchange.gmail.send.dataobjects.GmailSendDocumentPart;
import com.openexchange.gmail.send.dataobjects.GmailSendFilePart;
import com.openexchange.gmail.send.dataobjects.GmailSendMailMessage;
import com.openexchange.gmail.send.dataobjects.GmailSendReferencedPart;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.upload.UploadFile;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.Protocol;
import com.openexchange.mail.api.AbstractProtocolProperties;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.dataobjects.compose.DataMailPart;
import com.openexchange.mail.dataobjects.compose.InfostoreDocumentMailPart;
import com.openexchange.mail.dataobjects.compose.ReferencedMailPart;
import com.openexchange.mail.dataobjects.compose.TextBodyMailPart;
import com.openexchange.mail.dataobjects.compose.UploadFileMailPart;
import com.openexchange.mail.transport.MailTransport;
import com.openexchange.mail.transport.TransportProvider;
import com.openexchange.session.Session;

/**
 * {@link GmailSendProvider} - The transport provider for Gmail Send.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class GmailSendProvider extends TransportProvider {

    /**
     * Gmail Send protocol
     */
    public static final Protocol PROTOCOL_GMAIL_SEND = new Protocol("gmailsend");

    private static final GmailSendProvider instance = new GmailSendProvider();

    /**
     * Gets the singleton instance of Gmail Send provider
     *
     * @return The singleton instance of Gmail Send provider
     */
    public static GmailSendProvider getInstance() {
        return instance;
    }

    /**
     * Initializes a new {@link GmailSendProvider}
     */
    private GmailSendProvider() {
        super();
    }

    @Override
    protected void startUp() throws OXException {
        super.startUp();
    }

    @Override
    protected void shutDown() throws OXException {
        GmailSendSessionProperties.resetDefaultSessionProperties();
        super.shutDown();
    }

    @Override
    public MailTransport createNewMailTransport(final Session session) throws OXException {
        return new GmailSendTransport(session);
    }

    @Override
    public MailTransport createNewMailTransport(final Session session, final int accountId) throws OXException {
        return new GmailSendTransport(session, accountId);
    }

    @Override
    public MailTransport createNewNoReplyTransport(int contextId) throws OXException {
        throw MailExceptionCode.UNSUPPORTED_OPERATION.create();
    }

    @Override
    public MailTransport createNewNoReplyTransport(int contextId, boolean useNoReplyAddress) throws OXException {
        throw MailExceptionCode.UNSUPPORTED_OPERATION.create();
    }

    @Override
    public ComposedMailMessage getNewComposedMailMessage(final Session session, final Context ctx) throws OXException {
        return new GmailSendMailMessage(session, ctx);
    }

    @Override
    public InfostoreDocumentMailPart getNewDocumentPart(final String documentId, final Session session) throws OXException {
        return new GmailSendDocumentPart(documentId, session);
    }

    @Override
    public UploadFileMailPart getNewFilePart(final UploadFile uploadFile) throws OXException {
        return new GmailSendFilePart(uploadFile);
    }

    @Override
    public ReferencedMailPart getNewReferencedPart(final MailPart referencedPart, final Session session) throws OXException {
        return new GmailSendReferencedPart(referencedPart, session);
    }

    @Override
    public ReferencedMailPart getNewReferencedMail(final MailMessage referencedMail, final Session session) throws OXException {
        return new GmailSendReferencedPart(referencedMail, session);
    }

    @Override
    public TextBodyMailPart getNewTextBodyPart(final String textBody) throws OXException {
        return new GmailSendBodyPart(textBody);
    }

    @Override
    public Protocol getProtocol() {
        return PROTOCOL_GMAIL_SEND;
    }

    @Override
    protected AbstractProtocolProperties getProtocolProperties() {
        return GmailSendProperties.getInstance();
    }

    @Override
    public DataMailPart getNewDataPart(final Object data, final Map<String, String> dataProperties, final Session session) throws OXException {
        return new GmailSendDataPart(data, dataProperties, session);
    }

}
