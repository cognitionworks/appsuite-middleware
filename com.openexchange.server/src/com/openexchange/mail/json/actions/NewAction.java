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

package com.openexchange.mail.json.actions;

import java.io.IOException;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONValue;
import com.openexchange.ajax.Mail;
import com.openexchange.ajax.fields.DataFields;
import com.openexchange.ajax.fields.FolderChildFields;
import com.openexchange.ajax.helper.ParamContainer;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.upload.impl.UploadEvent;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailJSONField;
import com.openexchange.mail.MailServletInterface;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.cache.MailMessageCache;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.compose.ComposeType;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.json.MailRequest;
import com.openexchange.mail.json.parser.MessageParser;
import com.openexchange.mail.mime.MIMEDefaultSession;
import com.openexchange.mail.mime.MIMEMailException;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.mime.converters.MIMEMessageConverter;
import com.openexchange.mail.transport.MailTransport;
import com.openexchange.mail.utils.MailFolderUtility;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.preferences.ServerUserSetting;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.stream.UnsynchronizedByteArrayInputStream;


/**
 * {@link NewAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class NewAction extends AbstractMailAction {

    private static final org.apache.commons.logging.Log LOG =
        com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(NewAction.class));

    private static final boolean DEBUG = LOG.isDebugEnabled();

    /**
     * Initializes a new {@link NewAction}.
     * @param services
     */
    public NewAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final MailRequest req) throws OXException {
        final AJAXRequestData request = req.getRequest();
        try {
            if (request.hasUploads() || request.getParameter(Mail.UPLOAD_FORMFIELD_MAIL) != null) {
                final ServerSession session = req.getSession();
                final UploadEvent uploadEvent = request.getUploadEvent();
                String msgIdentifier = null;
                {
                    final JSONObject jsonMailObj;
                    {
                        final String json0 = uploadEvent.getFormField(Mail.UPLOAD_FORMFIELD_MAIL);
                        if (json0 == null || json0.trim().length() == 0) {
                            throw MailExceptionCode.MISSING_PARAM.create(Mail.UPLOAD_FORMFIELD_MAIL);
                        }
                        jsonMailObj = new JSONObject(json0);
                    }
                    /*-
                     * Parse
                     *
                     * Resolve "From" to proper mail account to select right transport server
                     */
                    final InternetAddress from;
                    try {
                        from = MessageParser.getFromField(jsonMailObj)[0];
                    } catch (final AddressException e) {
                        throw MIMEMailException.handleMessagingException(e);
                    }
                    int accountId;
                    try {
                        accountId = resolveFrom2Account(session, from, true, true);
                    } catch (final OXException e) {
                        if (MailExceptionCode.NO_TRANSPORT_SUPPORT.equals(e)) {
                            // Re-throw
                            throw e;
                        }
                        LOG.warn(new StringBuilder(128).append(e.getMessage()).append(". Using default account's transport.").toString());
                        // Send with default account's transport provider
                        accountId = MailAccount.DEFAULT_ID;
                    }
                    final MailServletInterface mailInterface = MailServletInterface.getInstance(session);
                    if (jsonMailObj.hasAndNotNull(MailJSONField.FLAGS.getKey()) && (jsonMailObj.getInt(MailJSONField.FLAGS.getKey()) & MailMessage.FLAG_DRAFT) > 0) {
                        /*
                         * ... and save draft
                         */
                        final ComposedMailMessage composedMail =
                            MessageParser.parse4Draft(jsonMailObj, uploadEvent, session, accountId);
                        msgIdentifier = mailInterface.saveDraft(composedMail, false, accountId);
                    } else {
                        /*
                         * ... and send message
                         */
                        final ComposedMailMessage[] composedMails =
                            MessageParser.parse4Transport(jsonMailObj, uploadEvent, session, accountId, request.isSecure() ? "https://" : "http://", request.getHostname());
                        final ComposeType sendType =
                            jsonMailObj.hasAndNotNull(Mail.PARAMETER_SEND_TYPE) ? ComposeType.getType(jsonMailObj.getInt(Mail.PARAMETER_SEND_TYPE)) : ComposeType.NEW;
                        msgIdentifier = mailInterface.sendMessage(composedMails[0], sendType, accountId);
                        for (int i = 1; i < composedMails.length; i++) {
                            mailInterface.sendMessage(composedMails[i], sendType, accountId);
                        }
                        /*
                         * Trigger contact collector
                         */
                        try {
                            final ServerUserSetting setting = ServerUserSetting.getInstance();
                            final int contextId = session.getContextId();
                            final int userId = session.getUserId();
                            if (setting.isContactCollectionEnabled(contextId, userId).booleanValue() && setting.isContactCollectOnMailTransport(
                                contextId,
                                userId).booleanValue()) {
                                triggerContactCollector(session, composedMails[0]);
                            }
                        } catch (final OXException e) {
                            LOG.warn("Contact collector could not be triggered.", e);
                        }
                    }
                }
                if (msgIdentifier == null) {
                    throw MailExceptionCode.SEND_FAILED_UNKNOWN.create();
                }
                /*
                 * Create JSON response object
                 */
                return new AJAXRequestResult(msgIdentifier, "string");
            }
            /*
             * Non-POST
             */
            final ServerSession session = req.getSession();
            /*
             * Read in parameters
             */
            final String folder = req.getParameter(Mail.PARAMETER_FOLDERID);
            final int flags;
            {
                final int i = req.optInt(Mail.PARAMETER_FLAGS);
                flags = MailRequest.NOT_FOUND == i ? 0 : i;
            }
            final boolean force;
            {
                String tmp = req.getParameter("force");
                if (null == tmp) {
                    force = false;
                } else {
                    tmp = tmp.trim();
                    force = "1".equals(tmp) || Boolean.parseBoolean(tmp);
                }
            }
            // Get rfc822 bytes and create corresponding mail message
            final QuotedInternetAddress defaultSendAddr = new QuotedInternetAddress(getDefaultSendAddress(session), true);
            final PutNewMailData data;
            {
                final MimeMessage message = new MimeMessage(MIMEDefaultSession.getDefaultSession(), new UnsynchronizedByteArrayInputStream(((String) req.getRequest().getData()).getBytes("US-ASCII")));
                final String fromAddr = message.getHeader(MessageHeaders.HDR_FROM, null);
                final InternetAddress fromAddress;
                final MailMessage mail;
                if (isEmpty(fromAddr)) {
                    // Add from address
                    fromAddress = defaultSendAddr;
                    message.setFrom(fromAddress);
                    mail = MIMEMessageConverter.convertMessage(message);
                } else {
                    fromAddress = new QuotedInternetAddress(fromAddr, true);
                    mail = MIMEMessageConverter.convertMessage(message);
                }
                data = new PutNewMailData() {

                    @Override
                    public MailMessage getMail() {
                        return mail;
                    }

                    @Override
                    public InternetAddress getFromAddress() {
                        return fromAddress;
                    }
                };
            }
            // Check if "folder" element is present which indicates to save given message as a draft or append to denoted folder
            final JSONValue responseData;
            if (folder == null) {
                responseData = appendDraft(session, flags, force, data.getFromAddress(), data.getMail());
            } else {
                final String[] ids;
                final MailServletInterface mailInterface = MailServletInterface.getInstance(session);
                try {
                    ids = mailInterface.appendMessages(folder, new MailMessage[] { data.getMail() }, force);
                    if (flags > 0) {
                        mailInterface.updateMessageFlags(folder, ids, flags, true);
                    }
                } finally {
                    mailInterface.close(true);
                }
                final JSONObject responseObj = new JSONObject();
                responseObj.put(FolderChildFields.FOLDER_ID, folder);
                responseObj.put(DataFields.ID, ids[0]);
                responseData = responseObj;
            }
            return new AJAXRequestResult(responseData, "json");
        } catch (final JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        } catch (final MessagingException e) {
            throw MIMEMailException.handleMessagingException(e);
        } catch (final RuntimeException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    private interface PutNewMailData {

        InternetAddress getFromAddress();

        MailMessage getMail();
    }

    private JSONObject appendDraft(final ServerSession session, final int flags, final boolean force, final InternetAddress from, final MailMessage m) throws OXException, OXException, JSONException {
        /*
         * Determine the account to transport with
         */
        final int accountId;
        {
            int accId;
            try {
                accId = resolveFrom2Account(session, from, true, !force);
            } catch (final OXException e) {
                if (MailExceptionCode.NO_TRANSPORT_SUPPORT.equals(e)) {
                    // Re-throw
                    throw e;
                }
                LOG.warn(new StringBuilder(128).append(e.getMessage()).append(". Using default account's transport.").toString());
                // Send with default account's transport provider
                accId = MailAccount.DEFAULT_ID;
            }
            accountId = accId;
        }
        /*
         * Missing "folder" element indicates to send given message via default mail account
         */
        final MailTransport transport = MailTransport.getInstance(session, accountId);
        try {
            /*
             * Send raw message source
             */
            final MailMessage sentMail = transport.sendRawMessage(m.getSourceBytes());
            JSONObject responseData = null;
            if (!session.getUserSettingMail().isNoCopyIntoStandardSentFolder()) {
                /*
                 * Copy in sent folder allowed
                 */
                final MailAccess<?, ?> mailAccess = MailAccess.getInstance(session, accountId);
                mailAccess.connect();
                try {
                    final String sentFullname =
                        MailFolderUtility.prepareMailFolderParam(mailAccess.getFolderStorage().getSentFolder()).getFullname();
                    final String[] uidArr;
                    try {
                        /*
                         * Append to default "sent" folder
                         */
                        if (flags != ParamContainer.NOT_FOUND) {
                            sentMail.setFlags(flags);
                        }
                        uidArr = mailAccess.getMessageStorage().appendMessages(sentFullname, new MailMessage[] { sentMail });
                        try {
                            /*
                             * Update cache
                             */
                            MailMessageCache.getInstance().removeFolderMessages(
                                accountId,
                                sentFullname,
                                session.getUserId(),
                                session.getContext().getContextId());
                        } catch (final OXException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    } catch (final OXException e) {
                        if (e.getMessage().indexOf("quota") != -1) {
                            throw MailExceptionCode.COPY_TO_SENT_FOLDER_FAILED_QUOTA.create(e, new Object[0]);
                        }
                        throw MailExceptionCode.COPY_TO_SENT_FOLDER_FAILED.create(e, new Object[0]);
                    }
                    if ((uidArr != null) && (uidArr[0] != null)) {
                        /*
                         * Mark appended sent mail as seen
                         */
                        mailAccess.getMessageStorage().updateMessageFlags(sentFullname, uidArr, MailMessage.FLAG_SEEN, true);
                    }
                    /*
                     * Compose JSON object
                     */
                    responseData = new JSONObject();
                    responseData.put(FolderChildFields.FOLDER_ID, MailFolderUtility.prepareFullname(MailAccount.DEFAULT_ID, sentFullname));
                    responseData.put(DataFields.ID, uidArr[0]);
                } finally {
                    mailAccess.close(true);
                }
            }
            return responseData;
        } finally {
            transport.close();
        }
    }

}
