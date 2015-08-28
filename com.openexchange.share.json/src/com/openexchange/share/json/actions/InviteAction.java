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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.share.json.actions;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.share.ShareExceptionCodes;
import com.openexchange.share.ShareInfo;
import com.openexchange.share.ShareTarget;
import com.openexchange.share.core.CreatedSharesImpl;
import com.openexchange.share.notification.ShareNotificationService.Transport;
import com.openexchange.share.recipient.RecipientType;
import com.openexchange.share.recipient.ShareRecipient;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link InviteAction}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.0
 */
public class InviteAction extends AbstractShareAction {

    /**
     * Initializes a new {@link InviteAction}.
     *
     * @param services The service lookup
     */
    public InviteAction(ServiceLookup services) {
        super(services);
    }

    @Override
    public AJAXRequestResult perform(AJAXRequestData requestData, ServerSession session) throws OXException {
        try {
            /*
             * parse targets, recipients & further parameters
             */
            JSONObject data = (JSONObject) requestData.requireData();
            ShareRecipient recipient = getParser().parseRecipient(data.getJSONObject("recipient"), getTimeZone(requestData, session));
            checkRecipient(recipient);
            ShareTarget target = getParser().parseTarget(data.getJSONObject("target"));
            String message = data.optString("message", null);
            Map<String, Object> meta = getParser().parseMeta(data.optJSONObject("meta"));
            /*
             * create the shares, notify recipients via mail
             */
            ShareInfo shareInfo = getShareService().addShare(session, target, recipient, meta).getShareInfo();
            CreatedSharesImpl createdShares = new CreatedSharesImpl(Collections.singletonMap(recipient, shareInfo));
            List<OXException> warnings = getNotificationService().sendShareCreatedNotifications(
                Transport.MAIL, createdShares, message, session, requestData.getHostData());
            /*
             * return appropriate result (including warnings)
             */
            JSONObject jsonResult = new JSONObject();
            jsonResult.put("entity", shareInfo.getGuest().getGuestID());
            AJAXRequestResult result = new AJAXRequestResult(jsonResult, shareInfo.getShare().getModified(), "json");
            result.addWarnings(warnings);
            return result;
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    private static void checkRecipient(ShareRecipient recipient) throws OXException {
        if (RecipientType.ANONYMOUS.equals(recipient.getType())) {
            throw ShareExceptionCodes.NO_INVITE_ANONYMOUS.create();
        }
    }

}
