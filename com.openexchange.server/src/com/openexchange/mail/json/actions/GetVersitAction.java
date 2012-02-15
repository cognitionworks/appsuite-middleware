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
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.Mail;
import com.openexchange.ajax.fields.CommonFields;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.CommonObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.json.OXJSONWriter;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailServletInterface;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.json.MailRequest;
import com.openexchange.mail.mime.MimeTypes;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.versit.converter.ConverterException;
import com.openexchange.tools.versit.utility.VersitUtility;

/**
 * {@link GetVersitAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class GetVersitAction extends AbstractMailAction {

    /**
     * Initializes a new {@link GetVersitAction}.
     *
     * @param services
     */
    public GetVersitAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final MailRequest req) throws OXException {
        try {
            final ServerSession session = req.getSession();
            /*
             * Read in parameters
             */
            final String folderPath = req.checkParameter(Mail.PARAMETER_FOLDERID);
            final String uid = req.checkParameter(Mail.PARAMETER_ID);
            // final String msgUID =
            // paramContainer.checkStringParam(PARAMETER_ID);
            final String partIdentifier = req.checkParameter(Mail.PARAMETER_MAILATTCHMENT);
            /*
             * Get mail interface
             */
            final MailServletInterface mailInterface = getMailInterface(req);
            final CommonObject[] insertedObjs;
            {
                final MailPart versitPart = mailInterface.getMessageAttachment(folderPath, uid, partIdentifier, false);
                /*
                 * Save dependent on content type
                 */
                final Context ctx = ContextStorage.getStorageContext(session.getContextId());
                final List<CommonObject> retvalList = new ArrayList<CommonObject>();
                if (versitPart.getContentType().isMimeType(MimeTypes.MIME_TEXT_X_VCARD) || versitPart.getContentType().isMimeType(
                    MimeTypes.MIME_TEXT_VCARD)) {
                    /*
                     * Save VCard
                     */
                    VersitUtility.saveVCard(
                        versitPart.getInputStream(),
                        versitPart.getContentType().getBaseType(),
                        versitPart.getContentType().containsCharsetParameter() ? versitPart.getContentType().getCharsetParameter() : MailProperties.getInstance().getDefaultMimeCharset(),
                        retvalList,
                        session,
                        ctx);
                } else if (versitPart.getContentType().isMimeType(MimeTypes.MIME_TEXT_X_VCALENDAR) || versitPart.getContentType().isMimeType(
                    MimeTypes.MIME_TEXT_CALENDAR)) {
                    /*
                     * Save ICalendar
                     */
                    VersitUtility.saveICal(
                        versitPart.getInputStream(),
                        versitPart.getContentType().getBaseType(),
                        versitPart.getContentType().containsCharsetParameter() ? versitPart.getContentType().getCharsetParameter() : MailProperties.getInstance().getDefaultMimeCharset(),
                        retvalList,
                        session,
                        ctx);
                } else {
                    throw MailExceptionCode.UNSUPPORTED_VERSIT_ATTACHMENT.create(versitPart.getContentType());
                }
                insertedObjs = retvalList.toArray(new CommonObject[retvalList.size()]);
            }
            final OXJSONWriter jsonWriter = new OXJSONWriter();
            jsonWriter.array();
            final JSONObject jo = new JSONObject();
            for (int i = 0; i < insertedObjs.length; i++) {
                final CommonObject current = insertedObjs[i];
                jo.reset();
                jo.put(CommonFields.ID, current.getObjectID());
                jo.put(CommonFields.FOLDER_ID, current.getParentFolderID());
                jsonWriter.value(jo);
            }
            jsonWriter.endArray();
            final AJAXRequestResult data = new AJAXRequestResult(jsonWriter.getObject(), "json");
            return data;
        } catch (final RuntimeException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (final JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        } catch (final ConverterException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

}
