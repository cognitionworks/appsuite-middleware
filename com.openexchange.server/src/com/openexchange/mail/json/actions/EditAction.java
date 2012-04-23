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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

import java.util.ArrayList;
import java.util.List;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.Mail;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.upload.impl.UploadEvent;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailJSONField;
import com.openexchange.mail.MailServletInterface;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.json.MailRequest;
import com.openexchange.mail.json.parser.MessageParser;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link EditAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class EditAction extends AbstractMailAction {

    private static final org.apache.commons.logging.Log LOG =
        com.openexchange.log.Log.valueOf(com.openexchange.log.LogFactory.getLog(EditAction.class));

    /**
     * Initializes a new {@link EditAction}.
     * @param services
     */
    public EditAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final MailRequest req) throws OXException {
        final AJAXRequestData request = req.getRequest();
        List<OXException> warnings = new ArrayList<OXException>();
        
        try {
            if (!request.hasUploads()) {
                throw AjaxExceptionCodes.UNKNOWN_ACTION.create("edit");
            }
            final ServerSession session = req.getSession();
            final UploadEvent uploadEvent = request.getUploadEvent();
            /*
             * Edit draft
             */
            String msgIdentifier = null;
            {
                final JSONObject jsonMailObj = new JSONObject(uploadEvent.getFormField(Mail.UPLOAD_FORMFIELD_MAIL));
                //final ServerSession session = (ServerSession) uploadEvent.getParameter(UPLOAD_PARAM_SESSION);
                /*
                 * Resolve "From" to proper mail account
                 */
                final InternetAddress from;
                try {
                    from = MessageParser.getFromField(jsonMailObj)[0];
                } catch (final AddressException e) {
                    throw MimeMailException.handleMessagingException(e);
                }
                int accountId = resolveFrom2Account(session, from, false, true);
                /*
                 * Check if detected account has drafts
                 */
                final MailServletInterface msi = getMailInterface(req);
                if (msi.getDraftsFolder(accountId) == null) {
                    if (MailAccount.DEFAULT_ID == accountId) {
                        // Huh... No drafts folder in default account
                        throw MailExceptionCode.FOLDER_NOT_FOUND.create("Drafts");
                    }
                    LOG.warn(new StringBuilder(64).append("Mail account ").append(accountId).append(" for user ").append(
                        session.getUserId()).append(" in context ").append(session.getContextId()).append(
                        " has no drafts folder. Saving draft to default account's draft folder."));
                    // No drafts folder in detected mail account; auto-save to default account
                    accountId = MailAccount.DEFAULT_ID;
                }
                /*
                 * Parse with default account's transport provider
                 */
                if (jsonMailObj.hasAndNotNull(MailJSONField.FLAGS.getKey()) && (jsonMailObj.getInt(MailJSONField.FLAGS.getKey()) & MailMessage.FLAG_DRAFT) > 0) {
                    final ComposedMailMessage composedMail =
                        MessageParser.parse4Draft(jsonMailObj, uploadEvent, session, MailAccount.DEFAULT_ID, warnings);
                    /*
                     * ... and edit draft
                     */
                    msgIdentifier = msi.saveDraft(composedMail, false, accountId);
                } else {
                    throw MailExceptionCode.UNEXPECTED_ERROR.create("No new message on action=edit");
                }
            }
            if (msgIdentifier == null) {
                throw MailExceptionCode.SEND_FAILED_UNKNOWN.create();
            }
            /*
             * Create JSON response object
             */
            AJAXRequestResult result = new AJAXRequestResult(msgIdentifier, "string");
            result.addWarnings(warnings);
            return result;
        } catch (final JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

}
