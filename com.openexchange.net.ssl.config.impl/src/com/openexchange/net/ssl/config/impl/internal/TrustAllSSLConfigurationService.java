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

package com.openexchange.net.ssl.config.impl.internal;

import java.util.List;
import com.openexchange.net.ssl.config.SSLConfigurationService;
import com.openexchange.net.ssl.config.TrustLevel;

/**
 * {@link TrustAllSSLConfigurationService} - The SSL configuration service in case trust-all is configured.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.8.3
 */
public class TrustAllSSLConfigurationService implements SSLConfigurationService {

    private static final TrustAllSSLConfigurationService INSTANCE = new TrustAllSSLConfigurationService();

    /**
     * Gets the singleton instance.
     *
     * @return The instance
     */
    public static TrustAllSSLConfigurationService getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link TrustAllSSLConfigurationService}.
     */
    private TrustAllSSLConfigurationService() {
        super();
    }

    @Override
    public boolean isWhitelisted(String hostName) {
        return true;
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.TRUST_ALL;
    }

    @Override
    public String[] getSupportedProtocols() {
        List<String> protocols = SSLProperties.getSupportedProtocols();
        return protocols.toArray(new String[protocols.size()]);
    }

    @Override
    public String[] getSupportedCipherSuites() {
        List<String> cipherSuites = SSLProperties.getSupportedCipherSuites();
        return cipherSuites.toArray(new String[cipherSuites.size()]);
    }

    @Override
    public boolean isVerifyHostname() {
        return false;
    }

    @Override
    public boolean isDefaultTruststoreEnabled() {
        return true;
    }

    @Override
    public boolean isCustomTruststoreEnabled() {
        return false;
    }

    @Override
    public String getCustomTruststoreLocation() {
        return null;
    }

    @Override
    public String getCustomTruststorePassword() {
        return null;
    }
}
