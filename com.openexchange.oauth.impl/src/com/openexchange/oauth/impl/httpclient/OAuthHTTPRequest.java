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

package com.openexchange.oauth.impl.httpclient;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.scribe.model.OAuthRequest;
import com.openexchange.exception.OXException;
import com.openexchange.http.client.builder.HTTPRequest;
import com.openexchange.http.client.builder.HTTPResponse;
import com.openexchange.net.ssl.SSLSocketFactoryProvider;
import com.openexchange.oauth.OAuthExceptionCodes;
import com.openexchange.oauth.impl.services.Services;

/**
 * {@link OAuthHTTPRequest} - The HTTP OAuth request.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a> Error handling
 */
public class OAuthHTTPRequest implements HTTPRequest {

	private final OAuthRequest delegate;
	private final Map<String, String> parameters;

	/**
	 * Initializes a new {@link OAuthHTTPRequest}.
	 */
	public OAuthHTTPRequest(OAuthRequest req, Map<String, String> parameters) {
	    super();
		delegate = req;
		this.parameters = parameters;
	}

	@Override
	public HTTPResponse execute() throws OXException {
	    try {
            delegate.setConnectTimeout(5, TimeUnit.SECONDS);
            delegate.setReadTimeout(15, TimeUnit.SECONDS);

            SSLSocketFactoryProvider factoryProvider = Services.optService(SSLSocketFactoryProvider.class);
            if (null != factoryProvider) {
                delegate.setSSLSocketFactory(factoryProvider.getDefault());
            }

            // Wrap response & return
            return new HttpOauthResponse(delegate.send());
        } catch (org.scribe.exceptions.OAuthException e) {
            // Handle Scribe's org.scribe.exceptions.OAuthException (inherits from RuntimeException)
            Throwable cause = e.getCause();
            if (cause instanceof java.net.SocketTimeoutException) {
                // A socket timeout
                throw OAuthExceptionCodes.CONNECT_ERROR.create(cause, new Object[0]);
            }

            throw OAuthExceptionCodes.OAUTH_ERROR.create(cause, e.getMessage());
        }
	}

	@Override
	public Map<String, String> getParameters() {
		return parameters;
	}

	@Override
	public Map<String, String> getHeaders() {
		return delegate.getHeaders();
	}
}
