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

package com.openexchange.admin.properties;

import java.util.List;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import com.openexchange.config.cascade.ConfigViewScope;

/**
 * The scope for a configuration option.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.5
 */
public class PropertyScope {

    /** A listing of scopes: <code>user</code> -&gt; <code>context</code> -&gt; <code>server</code> */
    private static final List<String> SCOPES_FROM_USER = ImmutableList.of(ConfigViewScope.USER.getScopeName(), ConfigViewScope.CONTEXT.getScopeName(), ConfigViewScope.RESELLER.getScopeName(), ConfigViewScope.SERVER.getScopeName());

    /** A listing of scopes: <code>context</code> -&gt; <code>server</code> */
    private static final List<String> SCOPES_FROM_CONTEXT = ImmutableList.of(ConfigViewScope.CONTEXT.getScopeName(), ConfigViewScope.RESELLER.getScopeName(), ConfigViewScope.SERVER.getScopeName());

    /** A listing of scopes: <code>server</code> */
    private static final List<String> SCOPES_FROM_SERVER = ImmutableList.of(ConfigViewScope.SERVER.getScopeName());

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static final PropertyScope REGEX_SCOPE_SERVER = new PropertyScope(-1, -1, SCOPES_FROM_SERVER);

    /**
     * Gets the property scope for server.
     *
     * @return The property scope
     */
    public static PropertyScope propertyScopeForServer() {
        return REGEX_SCOPE_SERVER;
    }

    /**
     * Gets the property scope for context.
     *
     * @param contextId The context identifier
     * @return The property scope
     */
    public static PropertyScope propertyScopeForContext(int contextId) {
        return new PropertyScope(-1, contextId, SCOPES_FROM_CONTEXT);
    }

    /**
     * Gets the property scope for user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The property scope
     */
    public static PropertyScope propertyScopeForUser(int userId, int contextId) {
        return new PropertyScope(userId, contextId, SCOPES_FROM_USER);
    }

    /**
     * Gets the property scope for default search path.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The property scope
     */
    public static PropertyScope propertyScopeForDefaultSearchPath(int userId, int contextId) {
        return new PropertyScope(userId, contextId, null);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final int contextId;
    private final int userId;
    private final Optional<List<String>> optionalScopes;

    /**
     * Initializes a new {@link PropertyScope}.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param scopes The scopes to iterate
     */
    private PropertyScope(int userId, int contextId, List<String> scopes) {
        super();
        this.userId = userId;
        this.contextId = contextId;
        this.optionalScopes = Optional.ofNullable(scopes);
    }

    /**
     * Gets the context identifier.
     *
     * @return The context identifier
     */
    public int getContextId() {
        return contextId;
    }

    /**
     * Gets the user identifier.
     *
     * @return The user identifier
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Gets the optional scopes to iterate.
     * <p>
     * If absent/empty the regular search path is taken.
     *
     * @return The scopes
     */
    public Optional<List<String>> getScopes() {
        return optionalScopes;
    }
}