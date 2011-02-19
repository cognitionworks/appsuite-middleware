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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.oauth.json.oauthaccount.actions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.oauth.OAuthAccount;
import com.openexchange.oauth.OAuthConstants;
import com.openexchange.oauth.OAuthInteraction;
import com.openexchange.oauth.OAuthService;
import com.openexchange.oauth.OAuthToken;
import com.openexchange.oauth.json.AbstractOAuthAJAXActionService;
import com.openexchange.oauth.json.Tools;
import com.openexchange.oauth.json.oauthaccount.AccountField;
import com.openexchange.oauth.json.oauthaccount.AccountWriter;
import com.openexchange.oauth.json.oauthaccount.multiple.AccountMultipleHandlerFactory;
import com.openexchange.tools.servlet.AjaxException;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link InitAction}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class InitAction extends AbstractOAuthAJAXActionService {

    /**
     * Initializes a new {@link InitAction}.
     */
    public InitAction() {
        super();
    }

    public AJAXRequestResult perform(final AJAXRequestData request, final ServerSession session) throws AbstractOXException {
        try {
            final String accountId = request.getParameter("id");
            if (null == accountId) {
                /*
                 * Call-back with action=create
                 */
                return createCallbackAction(request, session);
            }
            /*
             * Call-back with action=reauthorize
             */
            return reauthorizeCallbackAction(accountId, request, session);
        } catch (final JSONException e) {
            throw new AjaxException(AjaxException.Code.JSONError, e, e.getMessage());
        }
    }

    private AJAXRequestResult createCallbackAction(final AJAXRequestData request, final ServerSession session) throws AbstractOXException, JSONException {
        final OAuthService oAuthService = getOAuthService();
        /*
         * Parse parameters
         */
        final String serviceId = request.getParameter(AccountField.SERVICE_ID.getName());
        if (serviceId == null) {
            throw new AjaxException(AjaxException.Code.MISSING_PARAMETER, AccountField.SERVICE_ID.getName());
        }
        /*
         * Generate UUID
         */
        final String uuid = UUID.randomUUID().toString();
        /*
         * Compose call-back URL
         */
        final StringBuilder callbackUrlBuilder = new StringBuilder(256);
        callbackUrlBuilder.append(request.isSecure() ? "https://" : "http://");
        callbackUrlBuilder.append(request.getHostname());
        callbackUrlBuilder.append("/ajax/").append(AccountMultipleHandlerFactory.MODULE);
        callbackUrlBuilder.append("?action=create");
        callbackUrlBuilder.append("&respondWithHTML=true&session=").append(session.getSessionID());
        {
            final String name = AccountField.DISPLAY_NAME.getName();
            final String displayName = request.getParameter(name);
            if (displayName != null) {
                callbackUrlBuilder.append('&').append(name).append('=').append(urlEncode(displayName));
            }
        }
        callbackUrlBuilder.append('&').append(AccountField.SERVICE_ID.getName()).append('=').append(urlEncode(serviceId));
        callbackUrlBuilder.append('&').append(OAuthConstants.SESSION_PARAM_UUID).append('=').append(uuid);
        /*
         * Invoke
         */
        final OAuthInteraction interaction = oAuthService.initOAuth(serviceId, callbackUrlBuilder.toString());
        final OAuthToken requestToken = interaction.getRequestToken();
        final Map<String, Object> oauthState = new HashMap<String, Object>();
        oauthState.put("secret", requestToken.getSecret());
        oauthState.put(OAuthConstants.ARGUMENT_CALLBACK, callbackUrlBuilder.toString());
        
        session.setParameter(uuid, oauthState);
        /*
         * Write as JSON
         */
        final JSONObject jsonInteraction = AccountWriter.write(interaction, uuid);
        /*
         * Return appropriate result
         */
        return new AJAXRequestResult(jsonInteraction);
    }

    private AJAXRequestResult reauthorizeCallbackAction(final String accountId, final AJAXRequestData request, final ServerSession session) throws AbstractOXException, JSONException {
        final OAuthService oAuthService = getOAuthService();
        /*
         * Get account by identifier
         */
        final OAuthAccount account = oAuthService.getAccount(Tools.getUnsignedInteger(accountId), session.getUserId(), session.getContextId());
        final String serviceId = account.getMetaData().getId();
        /*
         * Generate UUID
         */
        final String uuid = UUID.randomUUID().toString();
        /*
         * Compose call-back URL
         */
        final StringBuilder callbackUrlBuilder = new StringBuilder(256);
        callbackUrlBuilder.append(request.isSecure() ? "https://" : "http://");
        callbackUrlBuilder.append(request.getHostname());
        callbackUrlBuilder.append("/ajax/").append(AccountMultipleHandlerFactory.MODULE);
        callbackUrlBuilder.append("?action=reauthorize");
        callbackUrlBuilder.append("&id=").append(account.getId());
        callbackUrlBuilder.append("&respondWithHTML=true&session=").append(session.getSessionID());
        {
            final String name = AccountField.DISPLAY_NAME.getName();
            final String displayName = request.getParameter(name);
            if (displayName != null) {
                callbackUrlBuilder.append('&').append(name).append('=').append(urlEncode(displayName));
            }
        }
        callbackUrlBuilder.append('&').append(AccountField.SERVICE_ID.getName()).append('=').append(urlEncode(serviceId));
        callbackUrlBuilder.append('&').append(OAuthConstants.SESSION_PARAM_UUID).append('=').append(uuid);
        /*
         * Invoke
         */
        final OAuthInteraction interaction = oAuthService.initOAuth(serviceId, callbackUrlBuilder.toString());
        final OAuthToken requestToken = interaction.getRequestToken();
        session.setParameter(uuid, requestToken.getSecret());
        /*
         * Write as JSON
         */
        final JSONObject jsonInteraction = AccountWriter.write(interaction, uuid);
        /*
         * Return appropriate result
         */
        return new AJAXRequestResult(jsonInteraction);
    }

    private static String urlEncode(final String s) {
        try {
            return URLEncoder.encode(s, "ISO-8859-1");
        } catch (final UnsupportedEncodingException e) {
            return s;
        }
    }

}
