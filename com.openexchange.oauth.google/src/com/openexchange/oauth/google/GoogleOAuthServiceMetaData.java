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

package com.openexchange.oauth.google;

import java.util.Collection;
import java.util.Collections;
import org.scribe.builder.api.Api;
import org.scribe.builder.api.Google2Api;
import org.scribe.model.Verb;
import com.openexchange.exception.OXException;
import com.openexchange.oauth.KnownApi;
import com.openexchange.oauth.impl.AbstractExtendedScribeAwareOAuthServiceMetaData;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link GoogleOAuthServiceMetaData}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public final class GoogleOAuthServiceMetaData extends AbstractExtendedScribeAwareOAuthServiceMetaData {

    private static final String IDENTITY_URL = "https://www.googleapis.com/oauth2/v1/userinfo";
    private static final String IDENTITY_FIELD_NAME = "id";

    /**
     * Initializes a new {@link GoogleOAuthServiceMetaData}.
     *
     * @param services the service lookup instance
     */
    public GoogleOAuthServiceMetaData(final ServiceLookup services) {
        super(services, KnownApi.GOOGLE, GoogleOAuthScope.values());
    }

    @Override
    protected String getPropertyId() {
        return "google";
    }

    @Override
    protected Collection<OAuthPropertyID> getExtraPropertyNames() {
        return Collections.singletonList(OAuthPropertyID.redirectUrl);
    }

    @Override
    public Class<? extends Api> getScribeService() {
        return Google2Api.class;
    }

    @Override
    public String processAuthorizationURL(String authUrl, Session session) throws OXException {
        StringBuilder authUrlBuilder = new StringBuilder(super.processAuthorizationURL(authUrl, session));
        // Request a refresh token, too and keep the already granted scopes
        return authUrlBuilder.append("&approval_prompt=force").append("&access_type=offline").append("&include_granted_scopes=true").toString();
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
