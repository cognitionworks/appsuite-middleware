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

package com.openexchange.oauth.twitter;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.scribe.builder.api.Api;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.Verb;
import com.openexchange.exception.OXException;
import com.openexchange.oauth.KnownApi;
import com.openexchange.oauth.OAuthToken;
import com.openexchange.oauth.impl.AbstractExtendedScribeAwareOAuthServiceMetaData;
import com.openexchange.oauth.scope.OAuthScope;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link TwitterOAuthServiceMetaData}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class TwitterOAuthServiceMetaData extends AbstractExtendedScribeAwareOAuthServiceMetaData {

    private static final String IDENTITY_URL = "https://api.twitter.com/1.1/account/verify_credentials.json";
    private static final String IDENTITY_FIELD_NAME = "id_str";

    /**
     * Initializes a new {@link TwitterOAuthServiceMetaData}.
     */
    public TwitterOAuthServiceMetaData(ServiceLookup services) {
        super(services, KnownApi.TWITTER, true, true, TwitterOAuthScope.values());
    }

    @Override
    public Class<? extends Api> getScribeService() {
        return TwitterApi.class;
    }

    @Override
    protected String getPropertyId() {
        return "twitter";
    }

    @Override
    protected Collection<OAuthPropertyID> getExtraPropertyNames() {
        return Collections.emptyList();
    }

    @Override
    public String processAuthorizationURL(final String authUrl, Session session) {
        return authUrl;
    }

    @Override
    public void processArguments(final Map<String, Object> arguments, final Map<String, String> parameter, final Map<String, Object> state) throws OXException {
        // no-op
    }

    @Override
    public String getRegisterToken(String authUrl) {
        return null;
    }

    @Override
    public OAuthToken getOAuthToken(final Map<String, Object> arguments, Set<OAuthScope> scopes) throws OXException {
        return null;
    }

    @Override
    public Verb getIdentityHTTPMethod() {
        return Verb.GET;
    }

    @Override
    public String getIdentityURL(String accessToken) {
        return IDENTITY_URL;
    }

    @Override
    public String getIdentityFieldName() {
        return IDENTITY_FIELD_NAME;
    }

}
