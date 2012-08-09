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

package com.openexchange.config;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
 */
public class SimConfigurationService implements ConfigurationService {

    public Map<String, String> stringProperties = new HashMap<String, String>();

    @Override
    public Filter getFilterFromProperty(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean getBoolProperty(final String name, final boolean defaultValue) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Properties getFile(final String fileName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getIntProperty(final String name, final int defaultValue) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Properties getPropertiesInFolder(final String folderName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getProperty(final String name) {
        return stringProperties.get(name);
    }

    @Override
    public String getProperty(final String name, final String defaultValue) {
        return stringProperties.containsKey(name) ? stringProperties.get(name) : defaultValue;
    }

    @Override
    public String getProperty(final String name, final PropertyListener listener) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getProperty(final String name, final String defaultValue, final PropertyListener listener) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterator<String> propertyNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void removePropertyListener(final String name, final PropertyListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public File getFileByName(String fileName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public File getDirectory(String directoryName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getText(final String fileName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getYaml(String filename) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> getYamlInFolder(String dirName) {
        // TODO Auto-generated method stub
        return null;
    }

}
