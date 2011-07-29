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

package com.openexchange.groupware.attach.json.actions;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import com.openexchange.ajax.Attachment;
import com.openexchange.ajax.container.ByteArrayFileHolder;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.attach.AttachmentMetadata;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.log.Log;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;

/**
 * {@link GetDocumentAction}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class GetDocumentAction extends AbstractAttachmentAction {

    private static final org.apache.commons.logging.Log LOG =
        Log.valueOf(org.apache.commons.logging.LogFactory.getLog(GetDocumentAction.class));

    /**
     * Initializes a new {@link GetDocumentAction}.
     */
    public GetDocumentAction(final ServiceLookup serviceLookup) {
        super(serviceLookup);
    }

    public AJAXRequestResult perform(final AJAXRequestData request, final ServerSession session) throws OXException {
        require(
            request,
            Attachment.PARAMETER_FOLDERID,
            Attachment.PARAMETER_ATTACHEDID,
            Attachment.PARAMETER_MODULE,
            Attachment.PARAMETER_ID);

        int folderId, attachedId, moduleId, id;
        final String contentType = request.getParameter(Attachment.PARAMETER_CONTENT_TYPE);
        folderId = requireNumber(request, Attachment.PARAMETER_FOLDERID);
        attachedId = requireNumber(request, Attachment.PARAMETER_ATTACHEDID);
        moduleId = requireNumber(request, Attachment.PARAMETER_MODULE);
        id = requireNumber(request, Attachment.PARAMETER_ID);

        return document(
            folderId,
            attachedId,
            moduleId,
            id,
            contentType,
            session.getContext(),
            session.getUser(),
            session.getUserConfiguration());
    }

    private AJAXRequestResult document(final int folderId, final int attachedId, final int moduleId, final int id, final String contentType, final Context ctx, final User user, final UserConfiguration userConfig) throws OXException {
        try {
            ATTACHMENT_BASE.startTransaction();
            final AttachmentMetadata attachment = ATTACHMENT_BASE.getAttachment(folderId, attachedId, moduleId, id, ctx, user, userConfig);
            /*
             * Get bytes
             */
            final ByteArrayOutputStream os;
            final InputStream documentData = ATTACHMENT_BASE.getAttachedFile(folderId, attachedId, moduleId, id, ctx, user, userConfig);
            try {
                os = new UnsynchronizedByteArrayOutputStream();
                final byte[] buffer = new byte[0xFFFF];
                int bytesRead = 0;
                while ((bytesRead = documentData.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();

            } finally {
                documentData.close();
            }
            /*
             * File holder
             */
            final ByteArrayFileHolder fileHolder = new ByteArrayFileHolder(os.toByteArray());
            fileHolder.setContentType(contentType);
            fileHolder.setName(attachment.getFilename());
            ATTACHMENT_BASE.commit();
            return new AJAXRequestResult(fileHolder, "file");
        } catch (final Throwable t) {
            // This is a bit convoluted: In case the contentType is not
            // overridden the returned file will be opened
            // in a new window. To call the JS callback routine from a popup we
            // can use parent.callback_error() but
            // must use window.opener.callback_error()
            rollback();
            if (t instanceof OXException) {
                throw (OXException) t;
            }
            throw new OXException(t);
        } finally {
            try {
                ATTACHMENT_BASE.finish();
            } catch (final OXException e) {
                LOG.debug("", e);
            }
        }
    }

}
