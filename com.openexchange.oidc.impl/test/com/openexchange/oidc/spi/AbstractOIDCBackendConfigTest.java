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

package com.openexchange.oidc.spi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import com.openexchange.config.PropertyFilter;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import com.openexchange.oidc.OIDCBackendConfig;

/**
 * {@link AbstractOIDCBackendConfigTest}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.10.3
 */
public class AbstractOIDCBackendConfigTest {

    @Test
    public void loadAllDefaults() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        LeanConfigurationService leanDefaults = new ReturnDefaultsLeanConfig();
        AbstractOIDCBackendConfig backendConfig = new AbstractOIDCBackendConfig(leanDefaults, "") {};

        for (Method m : OIDCBackendConfig.class.getMethods()) {
            if (m.getParameterCount() == 0 && !m.getReturnType().equals(Void.class)) {
                Object val = m.invoke(backendConfig);
                if (val instanceof String) {
                    val = '"' + val.toString() + '"';
                }
                System.out.println(m.getName() + ": " + val);
            }
        }
    }

    /**
     * {@link ReturnDefaultsLeanConfig}
     *
     * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
     * @since v7.10.3
     */
    private static final class ReturnDefaultsLeanConfig implements LeanConfigurationService {

        /**
         * Initializes a new {@link ReturnDefaultsLeanConfig}.
         */
        public ReturnDefaultsLeanConfig() {
            super();
        }

        @Override
        public String getProperty(Property property) {
            return property.getDefaultValue(String.class);
        }

        @Override
        public int getIntProperty(Property property) {
            return property.getDefaultValue(Integer.class).intValue();
        }

        @Override
        public boolean getBooleanProperty(Property property) {
            return property.getDefaultValue(Boolean.class).booleanValue();
        }

        @Override
        public float getFloatProperty(Property property) {
            return property.getDefaultValue(Float.class).floatValue();
        }

        @Override
        public long getLongProperty(Property property) {
            return property.getDefaultValue(Long.class).longValue();
        }

        @Override
        public String getProperty(int userId, int contextId, Property property) {
            return getProperty(property);
        }

        @Override
        public int getIntProperty(int userId, int contextId, Property property) {
            return getIntProperty(property);
        }

        @Override
        public boolean getBooleanProperty(int userId, int contextId, Property property) {
            return getBooleanProperty(property);
        }

        @Override
        public float getFloatProperty(int userId, int contextId, Property property) {
            return getFloatProperty(property);
        }

        @Override
        public long getLongProperty(int userId, int contextId, Property property) {
            return getLongProperty(property);
        }

        @Override
        public String getProperty(Property property, Map<String, String> optionals) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int getIntProperty(Property property, Map<String, String> optionals) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public boolean getBooleanProperty(Property property, Map<String, String> optionals) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public float getFloatProperty(Property property, Map<String, String> optionals) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public long getLongProperty(Property property, Map<String, String> optionals) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public String getProperty(int userId, int contextId, Property property, Map<String, String> optionals) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int getIntProperty(int userId, int contextId, Property property, Map<String, String> optionals) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public boolean getBooleanProperty(int userId, int contextId, Property property, Map<String, String> optionals) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public float getFloatProperty(int userId, int contextId, Property property, Map<String, String> optionals) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public long getLongProperty(int userId, int contextId, Property property, Map<String, String> optionals) {
            // TODO Auto-generated method stub
            return 0;
        }

        @SuppressWarnings("unused")
        private <T> T getDefault(Property property, Class<T> coerceTo) {
            return property.getDefaultValue(coerceTo);
        }

        @Override
        public Map<String, String> getProperties(PropertyFilter propertyFilter) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getProperty(int userId, int contextId, Property property, List<String> scopes, Map<String, String> optionals) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int getIntProperty(int userId, int contextId, Property property, List<String> scopes, Map<String, String> optionals) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public boolean getBooleanProperty(int userId, int contextId, Property property, List<String> scopes, Map<String, String> optionals) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public float getFloatProperty(int userId, int contextId, Property property, List<String> scopes, Map<String, String> optionals) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public long getLongProperty(int userId, int contextId, Property property, List<String> scopes, Map<String, String> optionals) {
            // TODO Auto-generated method stub
            return 0;
        }

    }

}
