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

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.share.ShareInfo;
import com.openexchange.share.ShareTarget;
import com.openexchange.share.core.performer.CreatePerformer;
import com.openexchange.share.groupware.ModuleSupport;
import com.openexchange.share.notification.ShareNotificationService;
import com.openexchange.share.notification.ShareNotificationService.Transport;
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
     * @param translatorFactory
     */
    public InviteAction(ServiceLookup services) {
        super(services);
    }

    @Override
    public AJAXRequestResult perform(AJAXRequestData requestData, ServerSession session) throws OXException {
        try {
            JSONObject data = (JSONObject) requestData.requireData();
            List<ShareRecipient> recipients = ShareJSONParser.parseRecipients(data.getJSONArray("recipients"));
            List<ShareTarget> targets = ShareJSONParser.parseTargets(data.getJSONArray("targets"), getTimeZone(requestData, session),
                services.getService(ModuleSupport.class));
            String message = data.optString("message", null);
            /*
             * create the shares, notify recipients
             */
            CreatePerformer createPerformer = new CreatePerformer(recipients, targets, session, services);
            Map<ShareRecipient, List<ShareInfo>> createdShares = createPerformer.perform();
            /*
             * Send notifications. For now we only have a mail transport. The API might get expanded to allow additional transports.
             */
            ShareNotificationService shareNotificationService = services.getService(ShareNotificationService.class);
            List<OXException> warnings = shareNotificationService.sendShareCreatedNotifications(Transport.MAIL, createdShares, message, session, requestData);
            /*
             * construct & return appropriate json result
             */
            AJAXRequestResult result = new AJAXRequestResult();
            result.addWarnings(warnings);
            JSONArray jTokens = new JSONArray(recipients.size());
            for (ShareRecipient recipient : recipients) {
                List<ShareInfo> shares = createdShares.get(recipient);
                if (null == shares || 0 == shares.size()) {
                    // internal recipient
                    jTokens.put(JSONObject.NULL);
                } else {
                    // external recipient
                    if (1 == shares.size()) {
                        jTokens.put(shares.get(0).getToken());
                    } else {
                        jTokens.put(shares.get(0).getGuest().getBaseToken());
                    }
                }
            }
            result.setResultObject(jTokens, "json");
            result.setTimestamp(new Date());
            return result;
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

}
