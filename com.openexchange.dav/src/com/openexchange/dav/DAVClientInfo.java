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

package com.openexchange.dav;

import java.util.Locale;
import com.openexchange.clientinfo.ClientInfo;
import com.openexchange.clientinfo.ClientInfoType;


/**
 * {@link DAVClientInfo}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.0
 */
public class DAVClientInfo implements ClientInfo {

    private final String platform;
    private final String platformVersion;
    private final String app;
    private final String appVersion;

    public DAVClientInfo(String platform, String platformVersion, String app, String appVersion) {
        super();
        this.platform = platform;
        this.platformVersion = platformVersion;
        this.app = app;
        this.appVersion = appVersion;
    }

    public DAVClientInfo(String app) {
        this(null, null, app, null);
    }

    @Override
    public ClientInfoType getType() {
        return ClientInfoType.SYNC;
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
        return app;
    }

    @Override
    public String getAppVersion() {
        return appVersion;
    }

    @Override
    public String toString(Locale locale) {
        return app;
    }

}
