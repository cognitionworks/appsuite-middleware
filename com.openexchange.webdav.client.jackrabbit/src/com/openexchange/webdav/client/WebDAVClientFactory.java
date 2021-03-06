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

package com.openexchange.webdav.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import com.openexchange.exception.OXException;
import com.openexchange.session.Session;
import com.openexchange.webdav.client.jackrabbit.WebDAVClientImpl;

/**
 * {@link WebDAVClientFactory}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.4
 */
public interface WebDAVClientFactory {

    /**
     * Initializes a new {@link WebDAVClientImpl}.
     *
     * @param client The underlying HTTP client to use
     * @param baseUrl The URL of the WebDAV host to connect to
     * @return An initialized WebDAV client
     * @throws If WebDAV client cannot be created
     */
    WebDAVClient create(HttpClient client, URI baseUrl) throws OXException;

    /**
     * Initializes a new {@link WebDAVClientImpl}.
     *
     * @param client The underlying HTTP client to use
     * @param baseUrl The URL of the WebDAV host to connect to
     * @return An initialized WebDAV client
     * @throws If WebDAV client cannot be created
     */
    default WebDAVClient create(HttpClient client, String baseUrl) throws OXException {
        try {
            return create(client, new URI(baseUrl));
        } catch (URISyntaxException e) {
            throw WebDAVClientExceptionCodes.UNABLE_TO_PARSE_URI.create(baseUrl, e);
        }
    }

    /**
     * Initializes a new {@link WebDAVClientImpl}.
     *
     * @param client The underlying HTTP client to use
     * @param context The underlying HTTP context to use
     * @param baseUrl The URL of the WebDAV host to connect to
     * @return An initialized WebDAV client
     * @throws If WebDAV client cannot be created
     */
    WebDAVClient create(HttpClient client, HttpContext context, URI baseUrl) throws OXException;

    /**
     * Initializes a new {@link WebDAVClientImpl}.
     *
     * @param client The underlying HTTP client to use
     * @param context The underlying HTTP context to use
     * @param baseUrl The URL of the WebDAV host to connect to
     * @return An initialized WebDAV client
     * @throws If WebDAV client cannot be created
     */
    default WebDAVClient create(HttpClient client, HttpContext context, String baseUrl) throws OXException {
        try {
            return create(client, context, new URI(baseUrl));
        } catch (URISyntaxException e) {
            throw WebDAVClientExceptionCodes.UNABLE_TO_PARSE_URI.create(baseUrl, e);
        }
    }

    /**
     * Initializes a new {@link WebDAVClientImpl}.
     *
     * @param session The users session
     * @param accountId The account id
     * @param baseUrl The URL of the WebDAV host to connect to
     * @param optClientId The optional http client id to use
     * @param context The {@link HttpClientContext} to use
     * @return An initialized WebDAV client
     * @throws If WebDAV client cannot be created
     */
    WebDAVClient create(Session session, String accountId, URI baseUrl, Optional<String> optClientId, HttpContext context) throws OXException;

    /**
     * Initializes a new {@link WebDAVClientImpl}.
     *
     * @param session The users session
     * @param accountId The account id
     * @param baseUrl The URL of the WebDAV host to connect to
     * @param context The {@link HttpClientContext} to use
     * @return An initialized WebDAV client
     * @throws If WebDAV client cannot be created
     */
    default WebDAVClient create(Session session, String accountId, String baseUrl, HttpContext context) throws OXException {
        try {
            return create(session, accountId, new URI(baseUrl), Optional.empty(), context);
        } catch (URISyntaxException e) {
            throw WebDAVClientExceptionCodes.UNABLE_TO_PARSE_URI.create(baseUrl, e);
        }
    }

}

