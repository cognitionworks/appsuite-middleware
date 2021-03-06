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

import static com.openexchange.java.Autoboxing.b;
import static com.openexchange.java.Autoboxing.i;
import java.util.List;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import com.openexchange.config.PropertyFilter;
import com.openexchange.config.SimConfigurationService;
import com.openexchange.exception.OXException;

/**
 * {@link SimLeanConfigurationService}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class SimLeanConfigurationService implements LeanConfigurationService {

    public SimConfigurationService delegateConfigurationService;

    /**
     * Initialises a new {@link SimMailFilterConfigurationService}.
     */
    public SimLeanConfigurationService(SimConfigurationService simConfigurationService) {
        super();
        this.delegateConfigurationService = simConfigurationService;
    }

    @Override
    public String getProperty(Property property) {
        return delegateConfigurationService.getProperty(property.getFQPropertyName());
    }

    @Override
    public int getIntProperty(Property property) {
        return delegateConfigurationService.getIntProperty(property.getFQPropertyName(), i(property.getDefaultValue(Integer.class)));
    }

    @Override
    public boolean getBooleanProperty(Property property) {
        return delegateConfigurationService.getBoolProperty(property.getFQPropertyName(), b(property.getDefaultValue(Boolean.class)));
    }

    @Override
    public float getFloatProperty(Property property) {
        return Float.parseFloat(delegateConfigurationService.getProperty(property.getFQPropertyName(), property.getDefaultValue(String.class)));
    }

    @Override
    public long getLongProperty(Property property) {
        return Long.parseLong(delegateConfigurationService.getProperty(property.getFQPropertyName(), property.getDefaultValue(String.class)));
    }

    @Override
    public String getProperty(int userId, int contextId, Property property) {
        return delegateConfigurationService.getProperty(property.getFQPropertyName());
    }

    @Override
    public int getIntProperty(int userId, int contextId, Property property) {
        return delegateConfigurationService.getIntProperty(property.getFQPropertyName(), i(property.getDefaultValue(Integer.class)));
    }

    @Override
    public boolean getBooleanProperty(int userId, int contextId, Property property) {
        return delegateConfigurationService.getBoolProperty(property.getFQPropertyName(), b(property.getDefaultValue(Boolean.class)));
    }

    @Override
    public float getFloatProperty(int userId, int contextId, Property property) {
        return Float.parseFloat(delegateConfigurationService.getProperty(property.getFQPropertyName(), property.getDefaultValue(String.class)));
    }

    @Override
    public long getLongProperty(int userId, int contextId, Property property) {
        return Long.parseLong(delegateConfigurationService.getProperty(property.getFQPropertyName(), property.getDefaultValue(String.class)));
    }

    @Override
    public String getProperty(Property property, Map<String, String> optionals) {
        return delegateConfigurationService.getProperty(property.getFQPropertyName(), property.getDefaultValue(String.class));
    }

    @Override
    public int getIntProperty(Property property, Map<String, String> optionals) {
        return delegateConfigurationService.getIntProperty(property.getFQPropertyName(), property.getDefaultValue(Integer.class).intValue());
    }

    @Override
    public boolean getBooleanProperty(Property property, Map<String, String> optionals) {
        return delegateConfigurationService.getBoolProperty(property.getFQPropertyName(), property.getDefaultValue(Boolean.class).booleanValue());
    }

    @Override
    public float getFloatProperty(Property property, Map<String, String> optionals) {
        return Float.parseFloat(delegateConfigurationService.getProperty(property.getFQPropertyName(), property.getDefaultValue(String.class)));
    }

    @Override
    public long getLongProperty(Property property, Map<String, String> optionals) {
        return Long.parseLong(delegateConfigurationService.getProperty(property.getFQPropertyName(), property.getDefaultValue(String.class)));
    }

    @Override
    public String getProperty(int userId, int contextId, Property property, Map<String, String> optionals) {
        return delegateConfigurationService.getProperty(property.getFQPropertyName(), property.getDefaultValue(String.class));
    }

    @Override
    public int getIntProperty(int userId, int contextId, Property property, Map<String, String> optionals) {
        return delegateConfigurationService.getIntProperty(property.getFQPropertyName(), property.getDefaultValue(Integer.class).intValue());
    }

    @Override
    public boolean getBooleanProperty(int userId, int contextId, Property property, Map<String, String> optionals) {
        return delegateConfigurationService.getBoolProperty(property.getFQPropertyName(), property.getDefaultValue(Boolean.class).booleanValue());
    }

    @Override
    public float getFloatProperty(int userId, int contextId, Property property, Map<String, String> optionals) {
        return Float.parseFloat(delegateConfigurationService.getProperty(property.getFQPropertyName(), property.getDefaultValue(String.class)));
    }

    @Override
    public long getLongProperty(int userId, int contextId, Property property, Map<String, String> optionals) {
        return Long.parseLong(delegateConfigurationService.getProperty(property.getFQPropertyName(), property.getDefaultValue(String.class)));
    }

    @Override
    public String getProperty(int userId, int contextId, Property property, List<String> scopes, Map<String, String> optionals) {
        return delegateConfigurationService.getProperty(property.getFQPropertyName(), property.getDefaultValue(String.class));
    }

    @Override
    public int getIntProperty(int userId, int contextId, Property property, List<String> scopes, Map<String, String> optionals) {
        return delegateConfigurationService.getIntProperty(property.getFQPropertyName(), property.getDefaultValue(Integer.class).intValue());
    }

    @Override
    public boolean getBooleanProperty(int userId, int contextId, Property property, List<String> scopes, Map<String, String> optionals) {
        return delegateConfigurationService.getBoolProperty(property.getFQPropertyName(), property.getDefaultValue(Boolean.class).booleanValue());
    }

    @Override
    public float getFloatProperty(int userId, int contextId, Property property, List<String> scopes, Map<String, String> optionals) {
        return Float.parseFloat(delegateConfigurationService.getProperty(property.getFQPropertyName(), property.getDefaultValue(String.class)));
    }

    @Override
    public long getLongProperty(int userId, int contextId, Property property, List<String> scopes, Map<String, String> optionals) {
        return Long.parseLong(delegateConfigurationService.getProperty(property.getFQPropertyName(), property.getDefaultValue(String.class)));
    }

    @Override
    public Map<String, String> getProperties(PropertyFilter propertyFilter) {
        try {
            return delegateConfigurationService.getProperties(propertyFilter);
        } catch (OXException e) {
            return ImmutableMap.of();
        }
    }

}
