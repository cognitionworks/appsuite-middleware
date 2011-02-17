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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.oauth;

/**
 * {@link OAuthConstants}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class OAuthConstants {

    /**
     * Initializes a new {@link OAuthConstants}.
     */
    private OAuthConstants() {
        super();
    }

    /*-
     * ------------------- Arguments -------------------
     */

    /**
     * The argument name for PIN. <code>java.lang.String</code>
     */
    public static final String ARGUMENT_PIN = "pin";

    /**
     * The argument name for display name. <code>java.lang.String</code>
     */
    public static final String ARGUMENT_DISPLAY_NAME = "displayName";

    /**
     * The argument name for request token. <code>com.openexchange.oauth.OAuthToken</code>
     */
    public static final String ARGUMENT_REQUEST_TOKEN = "requestToken";

    /*-
     * ------------------- URL parameters -------------------
     */

    /**
     * The URL parameter added to call-back URL providing the OAuth token according to specification.
     */
    public static final String URLPARAM_OAUTH_TOKEN = "oauth_token";

    /**
     * The URL parameter added to call-back URL providing the OAuth verifier according to specification.
     */
    public static final String URLPARAM_OAUTH_VERIFIER = "oauth_verifier";

    /*-
     * ------------------- Session parameters -------------------
     */

    /**
     * The session parameter providing the UUID associated with token secret.
     */
    public static final String SESSION_PARAM_UUID = "uuid";

    /*-
     * ------------------- ID type identifier for accounts -------------------
     */

    /**
     * The type constant used for generated IDs.
     */
    public static final String TYPE_ACCOUNT = "com.openexchange.oauth.account";

    public static final String CALLBACK = "callbackURL";

}
