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

package com.openexchange.mobilepush.templating.json.actions;

import java.util.Date;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.documentation.RequestMethod;
import com.openexchange.documentation.annotations.Action;
import com.openexchange.documentation.annotations.Parameter;
import com.openexchange.exception.OXException;
import com.openexchange.mobilepush.templating.MobilePushService;
import com.openexchange.mobilepush.templating.MobileNotifierServiceRegistry;
import com.openexchange.mobilepush.templating.json.MobilePushRequest;
import com.openexchange.mobilepush.templating.json.convert.MobileNotifyField;
import com.openexchange.mobilepush.templating.json.convert.NotifyItemWriter;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link GetAction}
 *
 * @author <a href="mailto:Lars.Hoogestraat@open-xchange.com">Lars Hoogestraat</a>
 * @since 7.6.0
 */
@Action(method = RequestMethod.GET, name = "get", description = "Get a notifaction item", parameters = {
    @Parameter(name = "session", description = "A session ID previously obtained from the login module."),
    @Parameter(name = "provider", description = "The requested providers.") }, responseDescription = "Response with timestamp: An JSON object describing an mobile notification object for one or multiple providers.")
public class GetAction extends AbstractMobilePushAction {

    /**
     * Initializes a new {@link GetAction}.
     *
     * @param services The service look-up
     */
    public GetAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(MobilePushRequest req) throws OXException, JSONException {
        String providerParam = req.checkParameter("provider");
        String[] providers = req.getParameterAsStringArray(providerParam);

        final MobileNotifierServiceRegistry mobileNotifierRegistry = getService(MobileNotifierServiceRegistry.class);
        if (null == mobileNotifierRegistry) {
            throw ServiceExceptionCode.absentService(MobileNotifierServiceRegistry.class);
        }

        final ServerSession session = req.getSession();
        final int uid = session.getUserId();
        final int cid = session.getContextId();
        final JSONObject itemJSON = new JSONObject();
        final JSONObject providerJSON = new JSONObject();

        // Get service for provider
        for (String provider : providers) {
            MobilePushService notifierService = mobileNotifierRegistry.getService(provider, uid, cid);
            itemJSON.put(notifierService.getFrontendName(), NotifyItemWriter.write(notifierService.getItems(session)));
        }
        providerJSON.put(MobileNotifyField.PROVIDER, itemJSON);
        return new AJAXRequestResult(providerJSON, new Date(System.currentTimeMillis()), "json");
    }
}
