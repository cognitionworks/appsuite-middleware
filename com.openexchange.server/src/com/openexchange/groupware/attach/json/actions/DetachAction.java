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

package com.openexchange.groupware.attach.json.actions;

import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;

/**
 * {@link DetachAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class DetachAction extends AbstractAttachmentAction {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DetachAction.class);

    /**
     * Initializes a new {@link DetachAction}.
     */
    public DetachAction(final ServiceLookup serviceLookup) {
        super(serviceLookup);
    }

    @Override
    public AJAXRequestResult perform(final AJAXRequestData requestData, final ServerSession session) throws OXException {
        try {
            require(requestData, AJAXServlet.PARAMETER_FOLDERID, AJAXServlet.PARAMETER_MODULE, AJAXServlet.PARAMETER_ATTACHEDID);
            final int folderId = requireNumber(requestData, AJAXServlet.PARAMETER_FOLDERID);
            final int attachedId = requireNumber(requestData, AJAXServlet.PARAMETER_ATTACHEDID);
            final int moduleId = requireNumber(requestData, AJAXServlet.PARAMETER_MODULE);

            final JSONArray idsArray = (JSONArray) requestData.requireData();

            final int[] ids = new int[idsArray.length()];
            for (int i = 0; i < idsArray.length(); i++) {
                try {
                    ids[i] = idsArray.getInt(i);
                } catch (JSONException e) {
                    final String string = idsArray.getString(i);
                    try {
                        ids[i] = Integer.parseInt(string);
                    } catch (NumberFormatException e1) {
                        throw AjaxExceptionCodes.INVALID_PARAMETER.create(string);
                    }
                }
            }

            final Date timestamp = detach(session, folderId, attachedId, moduleId, ids);
            return new AJAXRequestResult("", timestamp, "string");
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    private Date detach(final ServerSession session, final int folderId, final int attachedId, final int moduleId, final int[] ids) throws OXException {
        long timestamp = 0;
        try {
            ATTACHMENT_BASE.startTransaction();

            final Context ctx = session.getContext();
            final User user = session.getUser();
            final UserConfiguration userConfig = session.getUserConfiguration();
            timestamp = ATTACHMENT_BASE.detachFromObject(folderId, attachedId, moduleId, ids, session, ctx, user, userConfig);

            ATTACHMENT_BASE.commit();
        } catch (Throwable t) {
            rollback();
            if (t instanceof OXException) {
                throw (OXException) t;
            }
            throw new OXException(t);
        } finally {
            try {
                ATTACHMENT_BASE.finish();
            } catch (OXException e) {
                LOG.error("", e);
            }
        }
        return new Date(timestamp);
    }

}
