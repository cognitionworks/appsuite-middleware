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
 *     Copyright (C) 2017-2020 OX Software GmbH
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

package com.openexchange.config.lean;

import java.util.Map;

/**
 * {@link Property} - Describes a lean property with a fully qualified name and a default value
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 */
public interface Property {

    /**
     * Gets the fully qualified name of the {@link Property}
     *
     * @return the fully qualified name of the {@link Property}
     */
    String getFQPropertyName();

    /**
     * Gets the fully qualified name of the {@link Property}
     *
     * @param optionals A {@link Map} containing values for optional parts in the fully
     *            qualified name. E.g. <code>com.openexchange.test.[replaceMe]</code>
     *            is returned as <code>com.openexchange.test.success</code> if the Map
     *            contains a key called <code>replaceMe</code> with the value
     *            <code>success</code>. If the Map does not contain the key
     *            <code>com.openexchange.test.[replaceMe]</code> is returned.
     *
     * @return the fully qualified name of the {@link Property}
     */
    default String getFQPropertyName(Map<String, String> optionals) {
        String fqn = getFQPropertyName();
        if (null == optionals || (fqn.indexOf('[') < 0) || optionals.isEmpty()) {
            // No need to change anything
            return fqn;
        }

        // Contains optional parameters
        int length = fqn.length();
        StringBuilder builder = null;
        for (int i = 0; i < length; i++) {
            char c = fqn.charAt(i);
            if (c == '[') {
                // Find associated closing bracket
                int pos = fqn.indexOf(']', i);
                if (pos > 0) {
                    String toReplace = fqn.substring(i + 1, pos);
                    String optValue = optionals.get(toReplace);
                    if (null != optValue) {
                        // Replacement available
                        if (null == builder) {
                            builder = new StringBuilder(length);
                            if (i > 0) {
                                builder.append(fqn, 0, i);
                            }
                        }
                        builder.append(optValue);
                        i += toReplace.length() + 1;
                    } else {
                        // Add '[' character as no replacement available in map
                        if (null != builder) {
                            builder.append(c);
                        }
                    }
                } else {
                    // Add '[' character as no closing ']' present
                    if (null != builder) {
                        builder.append(c);
                    }
                }
            } else {
                // Not a '[' character
                if (null != builder) {
                    builder.append(c);
                }
            }
        }
        return null == builder ? fqn : builder.toString();
    }

    /**
     * Returns the default value of the {@link Property}
     *
     * @return The default value of the {@link Property}
     */
    Object getDefaultValue();

    /**
     * Returns the default value of the {@link Property}
     *
     * @param clazz The class of the {@link Property} which it will be casted to.
     * @return the default value of the {@link Property}
     * @throws IllegalArgumentException If specified type does not match the one of the default value
     */
    default <T extends Object> T getDefaultValue(Class<T> clazz) throws IllegalArgumentException {
        /*
         * Check data
         */
        if (null == clazz) {
            return null;
        }
        Object defaultValue = getDefaultValue();
        if (null == defaultValue) {
            return null;
        }
        
        /*
         * Check this order to be able to cast to sub-classes, too
         */
        if (clazz.isAssignableFrom(defaultValue.getClass())) {
            return clazz.cast(defaultValue);
        }
        throw new IllegalArgumentException("The object cannot be converted to the specified type '" + clazz.getCanonicalName() + "'");
    }
}
