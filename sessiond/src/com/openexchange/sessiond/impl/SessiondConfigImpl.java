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

package com.openexchange.sessiond.impl;

import static com.openexchange.sessiond.SessiondProperty.SESSIOND_AUTOLOGIN;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.config.ConfigTools;
import com.openexchange.config.ConfigurationService;

/**
 * SessionConfig
 *
 * @author <a href="mailto:sebastian.kauss@open-xchange.org">Sebastian Kauss</a>
 */
public class SessiondConfigImpl implements SessiondConfigInterface {

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(SessiondConfigImpl.class));
    private static final boolean DEBUG = LOG.isDebugEnabled();
    private static final long SHORT_CONTAINER_LIFE_TIME = 6l * 60l * 1000l;
    private static final long LONG_CONTAINER_LIFE_TIME = 60l * 60l * 1000l;

    private int maxSession = 5000;
    private int maxSessionPerUser = 100;
    private long sessionShortLifeTime = 60l * 60l * 1000l;
    private long randomTokenTimeout = 60l * 1000l;
    private long longLifeTime = 7l * 24l * 60l * 60l * 1000l;
    private boolean autoLogin = false;

    public SessiondConfigImpl(final ConfigurationService conf) {
        maxSession = parseProperty(conf, "com.openexchange.sessiond.maxSession", maxSession);
        if (DEBUG) {
            LOG.debug("Sessiond property: com.openexchange.sessiond.maxSession=" + maxSession);
        }

        maxSessionPerUser = parseProperty(conf, "com.openexchange.sessiond.maxSessionPerUser", maxSessionPerUser);
        if (DEBUG) {
            LOG.debug("Sessiond property: com.openexchange.sessiond.maxSessionPerUser=" + maxSessionPerUser);
        }

        sessionShortLifeTime = parseProperty(conf, "com.openexchange.sessiond.sessionDefaultLifeTime", (int) sessionShortLifeTime);
        if (DEBUG) {
            LOG.debug("Sessiond property: com.openexchange.sessiond.sessionDefaultLifeTime=" + sessionShortLifeTime);
        }

        String tmp = conf.getProperty("com.openexchange.sessiond.randomTokenTimeout", "1M");
        randomTokenTimeout = ConfigTools.parseTimespan(tmp);
        if (DEBUG) {
            LOG.debug("Sessiond property: com.openexchange.sessiond.randomTokenTimeout=" + randomTokenTimeout);
        }

        tmp = conf.getProperty("com.openexchange.sessiond.sessionLongLifeTime", "1W");
        longLifeTime = ConfigTools.parseTimespan(tmp);

        tmp = conf.getProperty(SESSIOND_AUTOLOGIN.getPropertyName(), SESSIOND_AUTOLOGIN.getDefaultValue());
        autoLogin = Boolean.parseBoolean(tmp);
    }

    @Override
    public long getSessionContainerTimeout() {
        return SHORT_CONTAINER_LIFE_TIME;
    }

    @Override
    public long getNumberOfSessionContainers() {
        return sessionShortLifeTime / SHORT_CONTAINER_LIFE_TIME;
    }

    @Override
    public int getMaxSessions() {
        return maxSession;
    }

    @Override
    public int getMaxSessionsPerUser() {
        return maxSessionPerUser;
    }

    @Override
    public long getLifeTime() {
        return sessionShortLifeTime;
    }

    @Override
    public long getRandomTokenTimeout() {
        return randomTokenTimeout;
    }

    public long getLongLifeTime() {
        return longLifeTime;
    }

    @Override
    public long getNumberOfLongTermSessionContainers() {
        return (longLifeTime - sessionShortLifeTime) / LONG_CONTAINER_LIFE_TIME;
    }

    @Override
    public boolean isAutoLogin() {
        return autoLogin;
    }

    public static int parseProperty(ConfigurationService prop, String name, int value) {
        final String tmp = prop.getProperty(name, "");
        if (tmp.trim().length() > 0) {
            try {
                return Integer.parseInt(tmp.trim());
            } catch (final NumberFormatException ex) {
                LOG.warn("property no parsable: " + name + ':' + value);
            }
        }
        return value;
    }

    @Override
    public long getLongTermSessionContainerTimeout() {
        return LONG_CONTAINER_LIFE_TIME;
    }
}
