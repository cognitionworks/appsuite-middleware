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

package com.openexchange.rest.client.httpclient;

import com.openexchange.annotation.NonNull;

/**
 * {@link WildcardHttpClientConfigProvider} - A provider for a concrete HTTP configuration using a wild-card expression to determine the
 * associated client.
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.4
 */
public interface WildcardHttpClientConfigProvider extends HttpClientConfigProvider {

    /**
     * Provides a wild-card expression as {@link String} that can be used to determine if a specific
     * client identifier can be used to generate a configuration.
     * <p>
     * In other words, the wild-card expression is used to identify the correct provider.
     *
     * @return A wild-card expression to match client identifier, never <code>null</code>. Expression should be a simple wild-card,
     *         like e.g. <code>myRegex-id-?</code> for matching one specific character or <code>myRegex*</code> for multiple
     *         characters followed.
     */
    @NonNull
    String getClientIdPattern();

    /**
     * Get the name of the group each client created with this provider belongs to.
     * <p>
     * Should be nearly the same value as per {@link #getClientIdPattern()} but without wild-cards.
     * This name will be used in case no specific configuration for the client is set, but for the
     * group of client represented by this provider.
     *
     * @return The group name
     */
    @NonNull
    String getGroupName();

    /**
     * Configures the {@link HttpBasicConfig}.
     * <p>
     * This method is for implementations that needs to set values based on additional properties.
     * If no adjustments needs to be performed, the implementor <b>MUST</b> not change anything.
     *
     * @param clientId The actual HTTP client identifier
     * @param config The configuration to adjust
     * @return The {@link HttpBasicConfig}
     */
    default HttpBasicConfig configureHttpBasicConfig(String clientId, HttpBasicConfig config) {
        return config;
    }

}
