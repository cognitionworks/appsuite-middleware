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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.logging;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.StatusListenerAsList;
import ch.qos.logback.core.status.StatusManager;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Reloadable;

/**
 * {@link LogConfigReloadable}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since 7.6.0
 */
public class LogConfigReloadable implements Reloadable {

    private static final Logger LOG = LoggerFactory.getLogger(LogConfigReloadable.class);

    private static final String CONFIGFILE = "logback.xml";

    private static final String[] PROPERTIES = new String[] { "all properties in file" };

    /**
     * Initializes a new {@link LogConfigReloadable}.
     */
    public LogConfigReloadable() {
        super();
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.config.Reloadable#reloadConfiguration(com.openexchange.config.ConfigurationService)
     */
    @Override
    public void reloadConfiguration(ConfigurationService configService) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ContextInitializer ci = new ContextInitializer(loggerContext);
        URL url = ci.findURLOfDefaultConfigurationFile(true);
        StatusListenerAsList statusListenerAsList = new StatusListenerAsList();
        StatusManager sm = loggerContext.getStatusManager();
        loggerContext.reset();
        // after a reset the statusListenerAsList gets removed as a listener
        sm.add(statusListenerAsList);
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            configurator.doConfigure(url);
        } catch (JoranException e) {
            LOG.error("Error reloading logback configuration: {}", e);
        } finally {
            sm.remove(statusListenerAsList);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.config.Reloadable#getConfigfileNames()
     */
    @Override
    public Map<String, String[]> getConfigfileNames() {
        Map<String, String[]> map = new HashMap<String, String[]>(1);
        map.put(CONFIGFILE, PROPERTIES);
        return map;
    }

}
