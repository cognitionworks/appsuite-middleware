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

package com.openexchange.clientinfo.impl;

import java.util.Locale;
import com.openexchange.clientinfo.ClientInfo;
import com.openexchange.clientinfo.ClientInfoStrings;
import com.openexchange.clientinfo.ClientInfoType;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.java.Strings;


/**
 * {@link WebClientInfo}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.0
 */
public class WebClientInfo implements ClientInfo {

    private final String client;
    private final String platform;
    private final String platformVersion;
    private final String browser;
    private final String browserVersion;

    public WebClientInfo(String client, String platform, String platformVersion, String browser, String browserVersion) {
        this.client = client;
        this.platform = platform;
        this.platformVersion = platformVersion;
        this.browser = browser;
        this.browserVersion = browserVersion;
    }

    @Override
    public String toString(Locale locale) {
        StringHelper helper = StringHelper.valueOf(locale);
        String out;
        if (Strings.isNotEmpty(platform) && Strings.isNotEmpty(platformVersion)) {
            out = helper.getString(ClientInfoStrings.DEFAULT_CLIENT_INFO_MESSAGE);
            return String.format(out, client, platform, platformVersion, browser, browserVersion);
        } else if (Strings.isNotEmpty(browser) && Strings.isNotEmpty(browserVersion)) {
            out = helper.getString(ClientInfoStrings.CLIENT_BROWSER_INFO_MESSAGE);
            return String.format(out, client, browser, browserVersion);
        } else {
            out = helper.getString(ClientInfoStrings.CLIENT_INFO_MESSAGE);
            return String.format(out, client);
        }
    }

    @Override
    public ClientInfoType getType() {
        return ClientInfoType.BROWSER;
    }

    @Override
    public String getPlatform() {
        return platform;
    }

    @Override
    public String getPlatformVersion() {
        return platformVersion;
    }

    @Override
    public String getApp() {
        return browser;
    }

    @Override
    public String getAppVersion() {
        return browserVersion;
    }

}
