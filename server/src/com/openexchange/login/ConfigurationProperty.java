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
 *    trademarks of the Open-Xchange, Inc. group of companies.
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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.login;

/**
 * {@link ConfigurationProperty}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public enum ConfigurationProperty {
    
    /**
     * Configures if some user is able to reenter his existing session after closing the browser tab or the complete browser. Setting this
     * to true may be a security risk for clients running on unsafe computers. If this is configured to true, check that the parameter
     * client contains the same identifier the UI sends as client parameter on normal login request. Otherwise the backend will not be able
     * to rediscover the users session after closing the browser tab.
     */ 
    HTTP_AUTH_AUTOLOGIN("com.openexchange.ajax.login.http-auth.autologin", Boolean.FALSE.toString()),

    /**
     * Every client tells the backend through the client parameter on the login request his identy. This is not possible when using the HTTP
     * Authorization Header based login. So the client identifier for that request is defined here. It must be the same identifier that the
     * web frontend uses, if you set com.openexchange.cookie.hash to calculate and want the previously configured autologin to work.
     */
    HTTP_AUTH_CLIENT("com.openexchange.ajax.login.http-auth.client", "com.openexchange.ox.gui.dhtml"),

    /**
     * The version of the client when using the HTTP Authorization Header based login. This should not be the normal web frontend version
     * because a different version can be used to distinguish logins through HTTP Authorization Header and normal login request. 
     */
    HTTP_AUTH_VERSION("com.openexchange.ajax.login.http-auth.version", "HTTP Auth"),

    /**
     * Configures which error page should be used for the login. The error page is only applied for the HTML Form login and the HTTP
     * Authorization Header login. All other login related requests provide normal JSON responses in error cases. The built-in error page
     * shows the error message for 5 seconds and then redirects to the referrer page. 
     */
    ERROR_PAGE_TEMPLATE("com.openexchange.ajax.login.errorPageTemplate", null);

    private final String propertyName;

    private final String defaultValue;

    private ConfigurationProperty(final String propertyName, final String defaultValue) {
        this.propertyName = propertyName;
        this.defaultValue = defaultValue;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
