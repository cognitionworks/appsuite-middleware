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
 *    trademarks of the OX Software GmbH. group of companies.
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

package com.openexchange.mail.compose.json.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.upload.StreamedUpload;
import com.openexchange.groupware.upload.StreamedUploadFileIterator;
import com.openexchange.groupware.upload.impl.UploadEvent;
import com.openexchange.groupware.upload.impl.UploadException;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailPath;
import com.openexchange.mail.compose.Attachment.ContentDisposition;
import com.openexchange.mail.compose.ClientToken;
import com.openexchange.mail.compose.CompositionSpaceId;
import com.openexchange.mail.compose.CompositionSpaceService;
import com.openexchange.mail.compose.MessageDescription;
import com.openexchange.mail.compose.UploadLimits;
import com.openexchange.mail.compose.json.converter.CompositionSpaceJSONResultConverter;
import com.openexchange.mail.compose.json.util.UploadFileFileIterator;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link SendCompositionSpaceAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.2
 */
public class SendCompositionSpaceAction extends AbstractMailComposeAction {

    /**
     * Initializes a new {@link SendCompositionSpaceAction}.
     *
     * @param services The service look-up
     */
    public SendCompositionSpaceAction(ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult doPerform(AJAXRequestData requestData, ServerSession session) throws OXException, JSONException {
        String sId = requestData.requireParameter("id");
        CompositionSpaceId compositionSpaceId = parseCompositionSpaceId(sId);

        CompositionSpaceService compositionSpaceService = getCompositionSpaceService(compositionSpaceId.getServiceId(), session);
        final ClientToken clientToken = getClientToken(requestData);

        // Check for optional body data
        Optional<StreamedUploadFileIterator> optionalUploadedAttachments = Optional.empty();
        {
            // Determine upload quotas
            UploadLimits uploadLimits = compositionSpaceService.getAttachmentUploadLimits(compositionSpaceId.getId());
            boolean hasFileUploads = requestData.hasUploads(uploadLimits.getPerAttachmentLimit(), uploadLimits.getPerRequestLimit(), true);
            StreamedUpload upload = null;
            UploadEvent uploadEvent = null;
            try {
                upload = requestData.getStreamedUpload();
            } catch (OXException e) {
                if (!UploadException.UploadCode.FAILED_STREAMED_UPLOAD.equals(e)) {
                    throw e;
                }
                uploadEvent = requestData.getUploadEvent();
            }
            if (null != upload) {
                String disposition = upload.getFormField("contentDisposition");
                if (null == disposition) {
                    disposition = ContentDisposition.ATTACHMENT.getId();
                }

                // Check for JSON data
                JSONObject jMessage = null;
                {
                    String expectedJsonContent = upload.getFormField("JSON");
                    if (Strings.isNotEmpty(expectedJsonContent)) {
                        jMessage = new JSONObject(expectedJsonContent);
                    }
                }

                if (null != jMessage) {
                    MessageDescription md = new MessageDescription();
                    parseJSONMessage(jMessage, md);
                    compositionSpaceService.updateCompositionSpace(compositionSpaceId.getId(), md, clientToken);
                }

                if (hasFileUploads) {
                    optionalUploadedAttachments = Optional.of(upload.getUploadFiles());
                }
            } else if (uploadEvent != null) {
                String disposition = uploadEvent.getFormField("contentDisposition");
                if (null == disposition) {
                    disposition = ContentDisposition.ATTACHMENT.getId();
                }

                // Check for JSON data
                JSONObject jMessage = null;
                {
                    String expectedJsonContent = uploadEvent.getFormField("JSON");
                    if (Strings.isNotEmpty(expectedJsonContent)) {
                        jMessage = new JSONObject(expectedJsonContent);
                    }
                }

                if (null != jMessage) {
                    MessageDescription md = new MessageDescription();
                    parseJSONMessage(jMessage, md);
                    compositionSpaceService.updateCompositionSpace(compositionSpaceId.getId(), md, clientToken);
                }

                if (hasFileUploads) {
                    optionalUploadedAttachments = Optional.of(new UploadFileFileIterator(uploadEvent.getUploadFiles()));
                }
            }
        }

        List<OXException> warnings = new ArrayList<OXException>(4);
        MailPath mailPath = compositionSpaceService.transportCompositionSpace(compositionSpaceId.getId(), optionalUploadedAttachments, session.getUserSettingMail(), requestData, warnings, true, clientToken);

        AJAXRequestResult result;
        if (null == mailPath) {
            result = new AJAXRequestResult(new JSONObject(2).put("success", true), "json");
        } else {
            JSONObject jMailPath = CompositionSpaceJSONResultConverter.convertMailPath(mailPath);
            result =  new AJAXRequestResult(jMailPath, "json");
        }
        result.addWarnings(warnings);
        result.addWarnings(compositionSpaceService.getWarnings());
        return result;
    }

}
