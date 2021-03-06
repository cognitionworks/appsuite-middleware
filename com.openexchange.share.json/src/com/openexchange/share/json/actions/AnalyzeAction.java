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

package com.openexchange.share.json.actions;

import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.conversion.ConversionResult;
import com.openexchange.conversion.ConversionService;
import com.openexchange.conversion.DataArguments;
import com.openexchange.conversion.DataHandler;
import com.openexchange.conversion.SimpleData;
import com.openexchange.conversion.datahandler.DataHandlers;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.share.subscription.ShareLinkAnalyzeResult;
import com.openexchange.share.subscription.ShareSubscriptionRegistry;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link AnalyzeAction} - Analyzes a share link from a remote server and returns a state indicating on the operation for the link
 * and which service can handle the link
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.5
 */
public class AnalyzeAction extends AbstractShareSubscriptionAction {

    /**
     * Initializes a new {@link AnalyzeAction}.
     * 
     * @param services The service lookup
     */
    public AnalyzeAction(ServiceLookup services) {
        super(services);
    }

    @Override
    public AJAXRequestResult perform(AJAXRequestData requestData, ServerSession session, JSONObject json, String shareLink) throws OXException {
        ShareSubscriptionRegistry service = services.getServiceSafe(ShareSubscriptionRegistry.class);
        ShareLinkAnalyzeResult result = service.analyze(session, shareLink);
        JSONObject jsonObject = new JSONObject(5);
        try {
            jsonObject.put("state", result.getState());
            putDetails(result, jsonObject);
            return createResponse(result.getInfos(), jsonObject);
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e.getMessage(), e);
        }
    }

    private void putDetails(ShareLinkAnalyzeResult result, JSONObject jsonObject) throws OXException, JSONException {
        OXException details = result.getDetails();
        if (null == details) {
            return;
        }
        details.setStackTrace(new StackTraceElement[0]);
        ConversionService conversionService = services.getServiceSafe(ConversionService.class);
        DataHandler error2json = conversionService.getDataHandler(DataHandlers.OXEXCEPTION2JSON);

        ConversionResult rs = error2json.processData(new SimpleData<OXException>(details), new DataArguments(), null);
        Object data = rs.getData();
        if (data != null && JSONObject.class.isInstance(data)) {
            jsonObject.put("error", data);
        }
    }

}
