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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.groupware.ldap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import com.openexchange.config.ConfigurationService;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.server.services.ServerServiceRegistry;

/**
 * {@link UserAttributeAccess} - Provides utility methods to access/update a user's attributes.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class UserAttributeAccess {

    /**
     * No instantiation.
     */
    private UserAttributeAccess() {
        super();
    }

    /**
     * Gets the specified <code>boolean</code> property from configuration service read from appropriate properties files.
     * 
     * @param name The property's name
     * @param defaultValue The default <code>boolean</code> value to return if property is missing
     * @return The <code>boolean</code> value
     */
    public static boolean getBooleanProperty(final String name, final boolean defaultValue) {
        final ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
        if (null == service) {
            return defaultValue;
        }
        return service.getBoolProperty(name, defaultValue);
    }

    /**
     * Gets the specified <code>boolean</code> attribute from given user.
     * <p>
     * This methods assumes that at most one value is associated with specified attribute name.
     * 
     * @param name The name of the <code>boolean</code> attribute
     * @param userId The identifier of the user
     * @param contextId The identifier of user's context
     * @param defaultValue The default value to return if user has no attribute of specified name
     * @return The value of the <code>boolean</code> attribute
     */
    public static boolean getBooleanAttribute(final String name, final int userId, final int contextId, final boolean defaultValue) {
        return getBooleanAttribute(name, UserStorage.getStorageUser(userId, contextId), defaultValue);
    }

    /**
     * Gets the specified <code>boolean</code> attribute from given user.
     * <p>
     * This methods assumes that at most one value is associated with specified attribute name.
     * 
     * @param name The name of the <code>boolean</code> attribute
     * @param user The user
     * @param defaultValue The default value to return if user has no attribute of specified name
     * @return The value of the <code>boolean</code> attribute
     */
    public static boolean getBooleanAttribute(final String name, final User user, final boolean defaultValue) {
        final Map<String, Set<String>> attributes = user.getAttributes();
        if (null == attributes) {
            return defaultValue;
        }
        final Set<String> bset = attributes.get(name);
        if (null == bset || bset.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(bset.iterator().next());
    }

    /**
     * Set specified <code>boolean</code> attribute for given user.
     * 
     * @param name The attribute name
     * @param value The attribute <code>boolean</code> value
     * @param user The user
     * @param context The context
     * @throws LdapException If setting <code>boolean</code> attribute fails
     */
    public static void setBooleanAttribute(final String name, final boolean value, final User user, final Context context) throws LdapException {
        final Map<String, Set<String>> attributes = user.getAttributes();
        /*
         * Create a modifiable map from existing unmodifiable map
         */
        final Map<String, Set<String>> newAttributes = new HashMap<String, Set<String>>(attributes.size());
        for (final Iterator<Entry<String, Set<String>>> iter = attributes.entrySet().iterator(); iter.hasNext();) {
            final Entry<String, Set<String>> entry = iter.next();
            newAttributes.put(entry.getKey(), new HashSet<String>(entry.getValue()));
        }
        /*
         * Add specified boolean value
         */
        Set<String> set = newAttributes.get(name);
        if (null == set) {
            set = new HashSet<String>();
            newAttributes.put(name, set);
        } else {
            set.clear();
        }
        set.add(String.valueOf(value));
        /*
         * Save modification
         */
        final UserImpl userImpl = new UserImpl();
        // userImpl.setTimeZone(user.getTimeZone());
        // userImpl.setPreferredLanguage(user.getPreferredLanguage());
        userImpl.setAttributes(newAttributes);
        UserStorage.getInstance().updateUser(user, context);
    }

}
