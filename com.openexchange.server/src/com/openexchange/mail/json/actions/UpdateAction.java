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

import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.Mail;
import com.openexchange.ajax.fields.CommonFields;
import com.openexchange.ajax.fields.DataFields;
import com.openexchange.ajax.fields.FolderChildFields;
import com.openexchange.ajax.fields.FolderFields;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.documentation.RequestMethod;
import com.openexchange.documentation.annotations.Action;
import com.openexchange.documentation.annotations.Parameter;
import com.openexchange.exception.OXException;
import com.openexchange.json.OXJSONWriter;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailJSONField;
import com.openexchange.mail.MailServletInterface;
import com.openexchange.mail.json.MailRequest;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link UpdateAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@Action(method = RequestMethod.PUT, name = "update", description = "Update/move mails", parameters = {
    @Parameter(name = "session", description = "A session ID previously obtained from the login module."),
    @Parameter(name = "id", description = "Object ID of the requested mail."),
    @Parameter(name = "message_id", description = "(Preliminary) The value of \"Message-Id\" header of the requested mail. This parameter is a substitute for \"id\" parameter."),
    @Parameter(name = "folder", description = "Object ID of the folder.")
}, requestBody = "A JSON object which carries the new values that ought to be applied to mail as described in Update mail or Update mail extended (available with SP6 v6.10).",
responseDescription = "A JSON object containing the Object ID of the updated mail and its folder.")
public final class UpdateAction extends AbstractMailAction {

    /**
     * Initializes a new {@link UpdateAction}.
     *
     * @param services
     */
    public UpdateAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final MailRequest req) throws OXException {
        try {
            final ServerSession session = req.getSession();
            /*
             * Read in parameters
             */
            final String sourceFolder = req.checkParameter(AJAXServlet.PARAMETER_FOLDERID);
            final JSONObject bodyObj = (JSONObject) req.getRequest().getData();
            final String destFolder = bodyObj.hasAndNotNull(FolderChildFields.FOLDER_ID) ? bodyObj.getString(FolderChildFields.FOLDER_ID) : null;
            final Integer colorLabel =
                bodyObj.hasAndNotNull(CommonFields.COLORLABEL) ? Integer.valueOf(bodyObj.getInt(CommonFields.COLORLABEL)) : null;
            final Integer flagBits =
                bodyObj.hasAndNotNull(MailJSONField.FLAGS.getKey()) ? Integer.valueOf(bodyObj.getInt(MailJSONField.FLAGS.getKey())) : null;
            boolean flagVal = false;
            if (flagBits != null) {
                /*
                 * Look for boolean value
                 */
                flagVal =
                    (bodyObj.has(MailJSONField.VALUE.getKey()) && !bodyObj.isNull(MailJSONField.VALUE.getKey()) ? bodyObj.getBoolean(MailJSONField.VALUE.getKey()) : false);
            }
            final Integer setFlags = bodyObj.hasAndNotNull("set_flags") ? Integer.valueOf(bodyObj.getInt("set_flags")) : null;
            final Integer clearFlags = bodyObj.hasAndNotNull("clear_flags") ? Integer.valueOf(bodyObj.getInt("clear_flags")) : null;
            /*
             * Get mail interface
             */
            final MailServletInterface mailInterface = getMailInterface(req);
            final OXJSONWriter jsonWriter = new OXJSONWriter();
            /*
             * Start response
             */
            jsonWriter.object();
            final String uid;
            {
                String tmp = req.getParameter(AJAXServlet.PARAMETER_ID);
                if (null == tmp) {
                    tmp = req.getParameter(Mail.PARAMETER_MESSAGE_ID);
                    if (null == tmp) {
                        uid = null;
                    } else {
                        uid = mailInterface.getMailIDByMessageID(sourceFolder, tmp);
                    }
                } else {
                    uid = tmp;
                }
            }

            String folderId = sourceFolder;
            String mailId = uid;
            if (colorLabel != null) {
                /*
                 * Update color label
                 */
                mailInterface.updateMessageColorLabel(sourceFolder, uid == null ? null : new String[] { uid }, colorLabel.intValue());
            }
            if (flagBits != null) {
                /*
                 * Update system flags which are allowed to be altered by client
                 */
                mailInterface.updateMessageFlags(sourceFolder, uid == null ? null : new String[] { uid }, flagBits.intValue(), flagVal);
            }
            if (setFlags != null) {
                /*
                 * Add system flags which are allowed to be altered by client
                 */
                mailInterface.updateMessageFlags(sourceFolder, uid == null ? null : new String[] { uid }, setFlags.intValue(), true);
            }
            if (clearFlags != null) {
                /*
                 * Remove system flags which are allowed to be altered by client
                 */
                mailInterface.updateMessageFlags(sourceFolder, uid == null ? null : new String[] { uid }, clearFlags.intValue(), false);
            }
            if (destFolder != null) {
                /*
                 * Perform move operation
                 */
                mailId = mailInterface.copyMessages(sourceFolder, destFolder, new String[] { uid }, true)[0];
                folderId = destFolder;
            }
            jsonWriter.key(FolderChildFields.FOLDER_ID).value(folderId);
            jsonWriter.key(DataFields.ID).value(mailId);
            jsonWriter.endObject();
            return new AJAXRequestResult(jsonWriter.getObject(), "json");
        } catch (final JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

}
