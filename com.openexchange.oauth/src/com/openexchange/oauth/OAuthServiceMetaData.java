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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

import java.util.Map;
import com.openexchange.exception.OXException;


/**
 * {@link OAuthServiceMetaData} - Represents the OAuth service meta data.
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:karsten.will@open-xchange.com">Karsten Will</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface OAuthServiceMetaData {

    /**
     * Gets this meta data's identifier.
     *
     * @return The identifier
     */
    String getId();

    /**
     * Gets the display name.
     *
     * @return The display name
     */
    String getDisplayName();

    /**
     * Gets the API key.
     *
     * @return The API key
     */
    String getAPIKey();

    /**
     * Gets the API secret.
     *
     * @return The API secret
     */
    String getAPISecret();

    /**
     * Indicates if this meta data needs a request token to obtain authorization URL.
     *
     * @return <code>true</code> if this meta data needs a request token to obtain authorization URL; otherwise <code>false</code> to pass
     *         <code>null</code>
     */
    boolean needsRequestToken();

    /**
     * Gets the optional scope; a comma-separated list of scope items.
     *
     * @return The scope or <code>null</code>
     */
    String getScope();

    /**
     * Processes specified authorization URL.
     *
     * @return The processed authorization URL
     */
    String processAuthorizationURL(String authUrl);

    /**
     * Processes specified arguments.
     *
     * @param arguments The arguments. You can store additional information here
     * @param parameter The parameters. The request parameters sent to the callback url. You may want to extract items from these and store them in arguments for later processing
     * @param state The state
     */
    void processArguments(Map<String, Object> arguments, Map<String, String> parameter, Map<String, Object> state);

    /**
     * Gets the optional OAuth token.
     *
     * @param arguments The OAuth arguments
     * @return The OAuth token or <code>null</code>
     * @throws OXException If an error occurs returning the token
     */
    OAuthToken getOAuthToken(Map<String, Object> arguments) throws OXException;

    /**
     * Initiates contact and returns the initial oauth interaction. This is an optional method, just return null when you
     * do not need to do anything special here.
     * @param callbackUrl
     * @return
     */
    OAuthInteraction initOAuth(String callbackUrl) throws OXException;

    /**
     * Gives the strategy the opportunity to modify a callback URL.
     * @param callbackUrl
     * @return the modified callback URL
     */
    String modifyCallbackURL(String callbackUrl);

    /**
     * Gets the style of API (e.g. Facebook, Twitter...).
     * @return
     */
	API getAPI();

	/**
	 * Whether to register a token based deferrer.
	 */
	boolean registerTokenBasedDeferrer();

}
