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

package com.openexchange.groupware.notify;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.configuration.ConfigurationException;
import com.openexchange.configuration.SystemConfig;
import com.openexchange.configuration.ConfigurationException.Code;
import com.openexchange.configuration.SystemConfig.Property;
import com.openexchange.server.Initialization;
import com.openexchange.tools.conf.AbstractConfig;

/**
 * DEPENDS ON: SystemConfig
 */
public class NotificationConfig extends AbstractConfig implements Initialization {

    private static final Log LOG = LogFactory.getLog(NotificationConfig.class);

    private static final Property KEY = Property.NOTIFICATION;

    enum NotificationProperty{

        NOTIFY_ON_DELETE("notify_participants_on_delete"),
        OBJECT_LINK("object_link"),
        INTERNAL_IMIP("imipForInternalUsers");


        private final String name;

        private NotificationProperty(final String name){
            this.name = name;
        }

        public String getName(){
            return name;
        }

    }

    private static NotificationConfig INSTANCE = new NotificationConfig();

    public static NotificationConfig getInstance() {
        return INSTANCE;
    }

    @Override
    protected String getPropertyFileName() throws ConfigurationException {
        final String filename = SystemConfig.getProperty(KEY);
        if (null == filename) {
            throw new ConfigurationException(Code.PROPERTY_MISSING,
                KEY.getPropertyName());
        }
        return filename;
    }

    public static String getProperty(final NotificationProperty prop, final String def) {
        if(!INSTANCE.isPropertiesLoadInternal()) {
            try {
                INSTANCE.loadPropertiesInternal();
            } catch (final ConfigurationException e) {
                LOG.error(e);
                return def;
            }
        }
        if(!INSTANCE.isPropertiesLoadInternal()) {
            return def;
        }
        return INSTANCE.getPropertyInternal(prop.getName(), def);
    }

    public static boolean getPropertyAsBoolean(final NotificationProperty prop, final boolean def) {
        final String boolVal = getProperty(prop,null);
        if(boolVal == null) {
            return def;
        }
        return Boolean.parseBoolean(boolVal);
    }

    public void start() throws ConfigurationException {
        if(!INSTANCE.isPropertiesLoadInternal()) {
            INSTANCE.loadPropertiesInternal();
        }
        NotificationPool.getInstance().startup();
    }

    public void stop() {
         NotificationPool.getInstance().shutdown();
        INSTANCE = new NotificationConfig();
    }

}
