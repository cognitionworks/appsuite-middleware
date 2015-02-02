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
 *     Copyright (C) 2004-2015 Open-Xchange, Inc.
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

package com.openexchange.oauth.provider.servlets;

import static com.openexchange.tools.servlet.http.Tools.sendEmptyErrorResponse;
import static com.openexchange.tools.servlet.http.Tools.sendErrorResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.net.HttpHeaders;
import com.openexchange.exception.OXException;
import com.openexchange.oauth.provider.Client;
import com.openexchange.oauth.provider.OAuthGrant;
import com.openexchange.oauth.provider.OAuthProviderConstants;
import com.openexchange.oauth.provider.OAuthProviderService;
import com.openexchange.oauth.provider.internal.URLHelper;
import com.openexchange.tools.servlet.http.Tools;


/**
 * {@link TokenEndpoint}
 * <p>
 * <img src="./webflow.png" alt="OAuth Web Flow">
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public class TokenEndpoint extends HttpServlet {

    private static final long serialVersionUID = 7597205004658187201L;

    private static final Logger LOG = LoggerFactory.getLogger(TokenEndpoint.class);

    private final OAuthProviderService oAuthProvider;

    public TokenEndpoint(OAuthProviderService oAuthProvider) {
        super();
        this.oAuthProvider = oAuthProvider;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            if (!Tools.considerSecure(request)) {
                response.setHeader(HttpHeaders.LOCATION, URLHelper.getSecureLocation(request));
                response.sendError(HttpServletResponse.SC_MOVED_PERMANENTLY);
                return;
            }

            String clientId = request.getParameter(OAuthProviderConstants.PARAM_CLIENT_ID);
            if (clientId == null) {
                failWithMissingParameter(response, OAuthProviderConstants.PARAM_CLIENT_ID);
                return;
            }

            Client client = oAuthProvider.getClientById(clientId);
            if (client == null) {
                failWithInvalidParameter(response, OAuthProviderConstants.PARAM_CLIENT_ID);
                return;
            }

            if (!client.isEnabled()) {
                failWithInvalidParameter(response, OAuthProviderConstants.PARAM_CLIENT_ID);
                return;
            }

            String clientSecret = request.getParameter(OAuthProviderConstants.PARAM_CLIENT_SECRET);
            if (clientSecret == null) {
                failWithMissingParameter(response, OAuthProviderConstants.PARAM_CLIENT_SECRET);
                return;
            }

            if (!client.getSecret().equals(clientSecret)) {
                fail(response, HttpServletResponse.SC_UNAUTHORIZED, "unauthorized_client", "invalid client secret");
                return;
            }

            String grantType = request.getParameter(OAuthProviderConstants.PARAM_GRANT_TYPE);
            if (grantType == null) {
                failWithMissingParameter(response, OAuthProviderConstants.PARAM_GRANT_TYPE);
                return;
            }

            if ("authorization_code".equals(grantType)) {
                handleAuthorizationCode(client, request, response);
            } else if ("refresh_token".equals(grantType)) {
                handleRefreshToken(client, request, response);
            } else {
                failWithInvalidParameter(response, OAuthProviderConstants.PARAM_GRANT_TYPE);
                return;
            }
        } catch (OXException | JSONException e) {
            LOG.error("Token request failed", e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "{\"error_description\":\"internal error\",\"error\":\"server_error\"}");
        }
    }

    private void handleAuthorizationCode(Client client, HttpServletRequest req, HttpServletResponse resp) throws IOException, JSONException, OXException {
        String redirectUri = req.getParameter(OAuthProviderConstants.PARAM_REDIRECT_URI);
        if (redirectUri == null) {
            failWithMissingParameter(resp, OAuthProviderConstants.PARAM_REDIRECT_URI);
            return;
        }

        if (!client.hasRedirectURI(redirectUri)) {
            failWithInvalidParameter(resp, OAuthProviderConstants.PARAM_REDIRECT_URI);
            return;
        }

        String authCode = req.getParameter(OAuthProviderConstants.PARAM_CODE);
        if (authCode == null) {
            failWithMissingParameter(resp, OAuthProviderConstants.PARAM_CODE);
            return;
        }

        OAuthGrant token = oAuthProvider.redeemAuthCode(client, redirectUri, authCode);
        if (token == null) {
            failWithInvalidParameter(resp, OAuthProviderConstants.PARAM_CODE);
            return;
        }

        respondWithToken(token, resp);
    }

    private void handleRefreshToken(Client client, HttpServletRequest req, HttpServletResponse resp) throws IOException, JSONException, OXException {
        String refreshToken = req.getParameter(OAuthProviderConstants.PARAM_REFRESH_TOKEN);
        if (refreshToken == null) {
            failWithMissingParameter(resp, OAuthProviderConstants.PARAM_REFRESH_TOKEN);
            return;
        }

        OAuthGrant token = oAuthProvider.redeemRefreshToken(client, refreshToken);
        if (token == null) {
            failWithInvalidParameter(resp, OAuthProviderConstants.PARAM_REFRESH_TOKEN);
            return;
        }

        respondWithToken(token, resp);
    }

    private static void respondWithToken(OAuthGrant grant, HttpServletResponse resp) throws IOException, JSONException {
        JSONObject result = new JSONObject();
        result.put("access_token", grant.getAccessToken());
        result.put("refresh_token", grant.getRefreshToken());
        result.put("token_type", "Bearer");
        result.put("expires_in", TimeUnit.SECONDS.convert(grant.getExpirationDate().getTime(), TimeUnit.MILLISECONDS) - TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS));
        result.put("scope", grant.getScope().scopeString());

        resp.setContentType("application/json;charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = resp.getWriter();
        writer.write(result.toString());
        writer.flush();
    }

    private static void failWithMissingParameter(HttpServletResponse httpResponse, String param) throws IOException {
        fail(httpResponse, HttpServletResponse.SC_BAD_REQUEST, "invalid_request", "missing required parameter: " + param);
    }

    private static void failWithInvalidParameter(HttpServletResponse httpResponse, String param) throws IOException {
        fail(httpResponse, HttpServletResponse.SC_BAD_REQUEST, "invalid_request", "invalid parameter value: " + param);
    }

    private static void fail(HttpServletResponse httpResponse, int statusCode, String error, String errorDescription) throws IOException {
        try {
            JSONObject result = new JSONObject();
            result.put("error", error);
            result.put("error_description", errorDescription);
            sendErrorResponse(httpResponse, statusCode, result.toString());
        } catch (JSONException e) {
            LOG.error("Could not compile error response object", e);
            sendEmptyErrorResponse(httpResponse, statusCode);
        }
    }

}
