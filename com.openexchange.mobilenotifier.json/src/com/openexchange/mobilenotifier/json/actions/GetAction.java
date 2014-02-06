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

package com.openexchange.mobilenotifier.json.actions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.documentation.RequestMethod;
import com.openexchange.documentation.annotations.Action;
import com.openexchange.documentation.annotations.Parameter;
import com.openexchange.exception.OXException;
import com.openexchange.mobilenotifier.MobileNotifierService;
import com.openexchange.mobilenotifier.MobileNotifierServiceRegistry;
import com.openexchange.mobilenotifier.json.MobileNotifierRequest;
import com.openexchange.mobilenotifier.json.convert.MobileNotifyField;
import com.openexchange.mobilenotifier.json.convert.NotifyItemWriter;
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
    @Parameter(name = "provider", description = "The requested providers.") }, responseDescription = "An JSON object describing an notification template for one or multiple providers.")
public class GetAction extends AbstractMobileNotifierAction {

    /**
     * Initializes a new {@link GetAction}.
     * 
     * @param services The service look-up
     */
    public GetAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(MobileNotifierRequest req) throws OXException, JSONException {
        String[] providers = req.getParameterAsStringArray("provider");

        final MobileNotifierServiceRegistry mobileNotifierRegistry = getService(MobileNotifierServiceRegistry.class);
        if (null == mobileNotifierRegistry) {
            throw ServiceExceptionCode.absentService(MobileNotifierServiceRegistry.class);
        }

        final ServerSession session = req.getSession();
        int uid = session.getUserId();
        int cid = session.getContextId();
        /*
         * Writes a JSON notify item structure
         */
        final JSONObject itemJsonObject = new JSONObject();
        final JSONObject providerObject = new JSONObject();

        mobileNotifierRegistry.getAllServices(uid, cid);
        for (String provider : providers) {
            MobileNotifierService notifierService = mobileNotifierRegistry.getService(provider, uid, cid);
            itemJsonObject.put(notifierService.getFrontendName(), NotifyItemWriter.write(notifierService));
        }
        providerObject.put(MobileNotifyField.PROVIDER, itemJsonObject);
        return new AJAXRequestResult(providerObject);
    }
}
