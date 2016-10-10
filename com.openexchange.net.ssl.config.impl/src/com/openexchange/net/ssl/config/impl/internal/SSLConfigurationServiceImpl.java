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

package com.openexchange.net.ssl.config.impl.internal;

import com.openexchange.config.ConfigurationService;
import com.openexchange.net.ssl.config.SSLConfigurationService;
import com.openexchange.net.ssl.config.TrustLevel;

/**
 * The {@link SSLConfigurationServiceImpl} provides user specific configuration with regards to SSL
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.8.3
 */
public class SSLConfigurationServiceImpl implements SSLConfigurationService {

    private ConfigurationService configService;

    public SSLConfigurationServiceImpl(ConfigurationService configService) {
        this.configService = configService;
    }

    @Override
    public boolean isWhitelisted(String hostName) {
        return SSLProperties.isWhitelisted(hostName);
    }

    @Override
    public TrustLevel getTrustLevel() {
        return SSLProperties.trustLevel();
    }

    @Override
    public String[] getSupportedProtocols() {
        return SSLProperties.supportedProtocols();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return SSLProperties.supportedCipherSuites();
    }

    @Override
    public void reload() {
        SSLProperties.reload();
    }

    @Override
    public boolean isVerifyHostname() {
        return this.configService.getBoolProperty(SSLProperties.HOSTNAME_VERIFICATION_ENABLED.getName(), SSLProperties.HOSTNAME_VERIFICATION_ENABLED.getDefaultBoolean());
    }

    @Override
    public boolean isDefaultTruststoreEnabled() {
        return this.configService.getBoolProperty(SSLProperties.DEFAULT_TRUSTSTORE_ENABLED.getName(), SSLProperties.DEFAULT_TRUSTSTORE_ENABLED.getDefaultBoolean());
    }

    @Override
    public boolean isCustomTruststoreEnabled() {
        return this.configService.getBoolProperty(SSLProperties.CUSTOM_TRUSTSTORE_ENABLED.getName(), SSLProperties.CUSTOM_TRUSTSTORE_ENABLED.getDefaultBoolean());
    }

    @Override
    public String getCustomTruststoreLocation() {
        return this.configService.getProperty(SSLProperties.CUSTOM_TRUSTSTORE_LOCATION.getName(), SSLProperties.CUSTOM_TRUSTSTORE_LOCATION.getDefault());
    }

    @Override
    public String getCustomTruststorePassword() {
        return this.configService.getProperty(SSLProperties.CUSTOM_TRUSTSTORE_PASSWORD.getName(), SSLProperties.CUSTOM_TRUSTSTORE_PASSWORD.getDefault());
    }
}
