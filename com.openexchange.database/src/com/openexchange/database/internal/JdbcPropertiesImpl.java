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

package com.openexchange.database.internal;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.database.JdbcProperties;

/**
 * {@link JdbcPropertiesImpl} - Provides the currently active JDBC properties as specified in <code>"dbconnector.yaml"</code> file.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.1
 */
public class JdbcPropertiesImpl implements JdbcProperties {

    private static final JdbcPropertiesImpl INSTANCE = new JdbcPropertiesImpl();

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static JdbcPropertiesImpl getInstance() {
        return INSTANCE;
    }

    /**
     * Removes possible parameters appended to specified JDBC URL and returns it.
     *
     * @param url The URL to remove possible parameters from
     * @return The parameter-less JDBC URL
     */
    public static String doRemoveParametersFromJdbcUrl(String url) {
        if (null == url) {
            return url;
        }

        int paramStart = url.indexOf('?');
        return paramStart >= 0 ? url.substring(0, paramStart) : url;
    }

    // --------------------------------------------------------------------------------------------------------------------------------

    private final AtomicReference<Properties> jdbcPropsReference;

    /**
     * Initializes a new {@link JdbcPropertiesImpl}.
     */
    private JdbcPropertiesImpl() {
        super();
        jdbcPropsReference = new AtomicReference<Properties>();
    }

    /**
     * Gets the reference to the currently active JDBC properties.
     * <p>
     * <div style="margin-left: 0.1in; margin-right: 0.5in; margin-bottom: 0.1in; background-color:#FFDDDD;">
     * <b>Note</b>: Modifying the returned <code>java.util.Properties</code> instance is reflected in JDBC properties
     * </div>
     *
     * @return The JDBC properties or <code>null</code> if not yet initialized
     */
    @Override
    public Properties getJdbcPropertiesRaw() {
        return jdbcPropsReference.get();
    }

    /**
     * Gets a copy of the currently active JDBC properties.
     *
     * @return The JDBC properties copy or <code>null</code> if not yet initialized
     */
    @Override
    public Properties getJdbcPropertiesCopy() {
        Properties properties = jdbcPropsReference.get();
        if (null == properties) {
            return null;
        }

        Properties copy = new Properties();
        copy.putAll(properties);
        return copy;
    }

    /**
     * Sets the JDBC properties to use.
     *
     * @param jdbcProperties The JDBC properties or <code>null</code> to clear them
     */
    public void setJdbcProperties(Properties jdbcProperties) {
        jdbcPropsReference.set(jdbcProperties);
    }

}
