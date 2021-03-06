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

package com.openexchange.authentication.oauth.impl;

import static com.openexchange.java.Autoboxing.I;
import java.net.URI;
import com.openexchange.authentication.NamePart;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import com.openexchange.java.Strings;

/**
 * {@link DefaultOAuthAuthenticationConfig}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.10.3
 */
public class DefaultOAuthAuthenticationConfig implements OAuthAuthenticationConfig {

    private static final String EMPTY = "";
    static enum OAuthPropery implements Property {

        tokenEndpoint(EMPTY),
        clientId(EMPTY),
        clientSecret(EMPTY),
        scope(EMPTY),
        earlyTokenRefreshSeconds(I(60)),
        tokenLockTimeoutSeconds(I(5)),
        passwordGrantUserNamePart(NamePart.FULL.getConfigName()),
        contextLookupSource(LookupSource.LOGIN_NAME.getConfigName()),
        contextLookupParameter(EMPTY),
        contextLookupNamePart(NamePart.DOMAIN.getConfigName()),
        userLookupSource(LookupSource.LOGIN_NAME.getConfigName()),
        userLookupParameter(EMPTY),
        userLookupNamePart(NamePart.LOCAL_PART.getConfigName()),
        tryRecoverStoredTokens(Boolean.FALSE),
        keepPasswordInSession(Boolean.TRUE)
        ;

        public static final String PREFIX = "com.openexchange.authentication.oauth";

        private final String fqn;
        private final Object defaultValue;

        private OAuthPropery(Object defaultValue) {
            this.fqn = PREFIX + '.' + name();
            this.defaultValue = defaultValue;
        }

        @Override
        public String getFQPropertyName() {
            return fqn;
        }

        @Override
        public Object getDefaultValue() {
            return defaultValue;
        }
    }

    private final LeanConfigurationService configService;

    public DefaultOAuthAuthenticationConfig(final LeanConfigurationService configService) {
        this.configService = configService;
    }

    @Override
    public URI getTokenEndpoint() {
        return URI.create(requireValue(OAuthPropery.tokenEndpoint));
    }

    @Override
    public String getClientId() {
        return requireValue(OAuthPropery.clientId);
    }

    @Override
    public String getClientSecret() {
        return requireValue(OAuthPropery.clientSecret);
    }

    @Override
    public String getScope() {
        String scope = configService.getProperty(OAuthPropery.scope);
        if (Strings.isEmpty(scope)) {
            return null;
        }
        return scope;
    }

    @Override
    public int getEarlyTokenRefreshSeconds() {
        return configService.getIntProperty(OAuthPropery.earlyTokenRefreshSeconds);
    }

    @Override
    public long getTokenLockTimeoutSeconds() {
        return configService.getIntProperty(OAuthPropery.tokenLockTimeoutSeconds);
    }

    @Override
    public NamePart getPasswordGrantUserNamePart() {
        String value = requireValue(OAuthPropery.passwordGrantUserNamePart);
        return NamePart.of(value);
    }

    @Override
    public LookupSource getContextLookupSource() {
        String value = requireValue(OAuthPropery.contextLookupSource);
        return LookupSource.of(value);
    }

    @Override
    public String getContextLookupParameter() {
        if (getContextLookupSource() == LookupSource.RESPONSE_PARAMETER) {
            return requireValue(OAuthPropery.contextLookupParameter);
        }

        return null;
    }

    @Override
    public NamePart getContextLookupNamePart() {
        String value = requireValue(OAuthPropery.contextLookupNamePart);
        return NamePart.of(value);
    }

    @Override
    public LookupSource getUserLookupSource() {
        String value = requireValue(OAuthPropery.userLookupSource);
        return LookupSource.of(value);
    }

    @Override
    public String getUserLookupParameter() {
        if (getUserLookupSource() == LookupSource.RESPONSE_PARAMETER) {
            return requireValue(OAuthPropery.userLookupParameter);
        }

        return null;
    }

    @Override
    public boolean tryRecoverStoredTokens() {
        return configService.getBooleanProperty(OAuthPropery.tryRecoverStoredTokens);
    }

    @Override
    public boolean keepPasswordInSession() {
        return configService.getBooleanProperty(OAuthPropery.keepPasswordInSession);
    }

    @Override
    public NamePart getUserLookupNamePart() {
        String value = requireValue(OAuthPropery.userLookupNamePart);
        return NamePart.of(value);
    }

    private String requireValue(OAuthPropery property) {
        String value = configService.getProperty(property);
        if (Strings.isEmpty(value)) {
            throw new IllegalArgumentException("No such value for property: " + property.getFQPropertyName());
        }

        return value;
    }

}
