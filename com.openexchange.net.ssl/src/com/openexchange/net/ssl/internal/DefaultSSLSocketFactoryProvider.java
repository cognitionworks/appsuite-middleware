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

package com.openexchange.net.ssl.internal;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import com.openexchange.java.util.Tools;
import com.openexchange.log.LogProperties;
import com.openexchange.net.ssl.SSLSocketFactoryProvider;
import com.openexchange.net.ssl.TrustedSSLSocketFactory;
import com.openexchange.net.ssl.config.SSLConfigurationService;
import com.openexchange.net.ssl.config.TrustLevel;
import com.openexchange.net.ssl.config.UserAwareSSLConfigurationService;
import com.openexchange.net.ssl.osgi.Services;
import com.openexchange.tools.ssl.TrustAllSSLSocketFactory;

/**
 * The provider of the {@link SSLSocketFactory} based on the configuration made by the administrator.
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.8.3
 */
public class DefaultSSLSocketFactoryProvider implements SSLSocketFactoryProvider {

    private final SSLConfigurationService sslConfigService;

    /**
     * Initializes a new {@link DefaultSSLSocketFactoryProvider}.
     */
    public DefaultSSLSocketFactoryProvider(SSLConfigurationService sslConfigService) {
        super();
        this.sslConfigService = sslConfigService;
    }

    private TrustLevel getEffectiveTrustLevel() {
        if (sslConfigService.getTrustLevel().equals(TrustLevel.TRUST_ALL)) {
            // Globally configured to use trust-all socket factory
            return TrustLevel.TRUST_ALL;
        }

        UserAwareSSLConfigurationService userSSLConfig = Services.getService(UserAwareSSLConfigurationService.class);
        if (null == userSSLConfig) {
            // Absent user-aware SSL config service. This happens for setups w/o a user/context service.
            return TrustLevel.TRUST_ALL;
        }

        // Try to determine by user
        int user = Tools.getUnsignedInteger(LogProperties.get(LogProperties.Name.SESSION_USER_ID));
        int context = Tools.getUnsignedInteger(LogProperties.get(LogProperties.Name.SESSION_CONTEXT_ID));
        if ((user < 0) || (context < 0)) {
            // No user-specific socket factory selectable
            return TrustLevel.TRUST_RESTRICTED;
        }

        return userSSLConfig.isTrustAll(user, context) ? TrustLevel.TRUST_ALL : TrustLevel.TRUST_RESTRICTED;
    }

    @Override
    public SSLSocketFactory getDefault() {
        TrustLevel effectiveTrustLevel = getEffectiveTrustLevel();
        return TrustLevel.TRUST_ALL == effectiveTrustLevel ? TrustAllSSLSocketFactory.getDefault() : TrustedSSLSocketFactory.getDefault();
    }

    @Override
    public SSLContext getOriginatingDefaultContext() {
        TrustLevel effectiveTrustLevel = getEffectiveTrustLevel();
        return TrustLevel.TRUST_ALL == effectiveTrustLevel ? TrustAllSSLSocketFactory.getCreatingDefaultContext() : TrustedSSLSocketFactory.getCreatingDefaultContext();
    }
}
