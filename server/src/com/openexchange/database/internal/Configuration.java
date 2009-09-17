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

package com.openexchange.database.internal;

import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openexchange.config.ConfigurationService;
import com.openexchange.database.DBPoolingExceptionCodes;
import com.openexchange.database.DBPoolingException;

/**
 * Contains the settings to connect to the configuration database.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class Configuration {

    private static final Log LOG = LogFactory.getLog(Configuration.class);

    private static final String CONFIG_FILENAME = "configdb.properties";

    private Properties props;

    private Properties readProps = new Properties();

    private Properties writeProps = new Properties();

    private final ConnectionPool.Config poolConfig = ConnectionPool.DEFAULT_CONFIG;

    Configuration() {
        super();
    }

    boolean isWriteDefined() {
        return Boolean.parseBoolean(getProperty(Property.SEPERATE_WRITE, "false"));
    }

    String getReadUrl() {
        return getProperty(Property.READ_URL);
    }

    Properties getReadProps() {
        return readProps;
    }

    String getWriteUrl() {
        return getProperty(Property.WRITE_URL);
    }

    Properties getWriteProps() {
        return writeProps;
    }

    private String getProperty(final Property property) {
        return getProperty(property, null);
    }

    private interface Convert<T> {
        T convert(String toConvert);
    }

    private <T> T getUniversal(Property property, T def, Convert<T> converter) {
        final T retval;
        if (props != null && props.containsKey(property.propertyName)) {
            retval = converter.convert(props.getProperty(property.propertyName));
        } else {
            retval = def;
        }
        return retval;
    }

    String getProperty(Property property, String def) {
        return getUniversal(property, def, new Convert<String>() {
            public String convert(String toConvert) {
                return toConvert;
            }
        });
    }

    int getInt(Property property, int def) {
        return getUniversal(property, Integer.valueOf(def), new Convert<Integer>() {
            public Integer convert(String toConvert) {
                return Integer.valueOf(toConvert);
            }
        }).intValue();
    }

    long getLong(Property property, long def) {
        return getUniversal(property, Long.valueOf(def), new Convert<Long>() {
            public Long convert(String toConvert) {
                return Long.valueOf(toConvert);
            }
        }).longValue();
    }

    boolean getBoolean(Property property, boolean def) {
        return getUniversal(property, Boolean.valueOf(def), new Convert<Boolean>() {
            public Boolean convert(String toConvert) {
                return Boolean.valueOf(toConvert);
            }
        }).booleanValue();
    }

    /**
     * {@inheritDoc}
     */
    public void readConfiguration(ConfigurationService service) throws DBPoolingException {
        if (null != props) {
            throw DBPoolingExceptionCodes.ALREADY_INITIALIZED.create(this.getClass().getName());
        }
        props = service.getFile(CONFIG_FILENAME);
        if (props.isEmpty()) {
            throw DBPoolingExceptionCodes.MISSING_CONFIGURATION.create();
        }
        separateReadWrite();
        loadDrivers();
        initPoolConfig();
    }

    private void separateReadWrite() {
        for (Object tmp : props.keySet()) {
            final String key = (String) tmp;
            if (key.startsWith("readProperty.")) {
                final String value = props.getProperty(key);
                final int equalSignPos = value.indexOf('=');
                final String readKey = value.substring(0, equalSignPos);
                final String readValue = value.substring(equalSignPos + 1);
                readProps.put(readKey, readValue);
            } else
            if (key.startsWith("writeProperty.")) {
                final String value = props.getProperty(key);
                final int equalSignPos = value.indexOf('=');
                final String readKey = value.substring(0, equalSignPos);
                final String readValue = value.substring(equalSignPos + 1);
                writeProps.put(readKey, readValue);
            }
        }
    }

    private void loadDrivers() throws DBPoolingException {
        final String readDriverClass = getProperty(Property.READ_DRIVER_CLASS);
        if (null == readDriverClass) {
            throw DBPoolingExceptionCodes.PROPERTY_MISSING.create(Property.READ_DRIVER_CLASS.propertyName);
        }
        try {
            Class.forName(readDriverClass);
        } catch (ClassNotFoundException e) {
            throw DBPoolingExceptionCodes.NO_DRIVER.create(e, readDriverClass);
        }
        if (!isWriteDefined()) {
            return;
        }
        final String writeDriverClass = getProperty(Property.WRITE_DRIVER_CLASS);
        if (null == writeDriverClass) {
            throw DBPoolingExceptionCodes.PROPERTY_MISSING.create(Property.WRITE_DRIVER_CLASS.propertyName);
        }
        try {
            Class.forName(writeDriverClass);
        } catch (ClassNotFoundException e) {
            throw DBPoolingExceptionCodes.NO_DRIVER.create(e, writeDriverClass);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        props = null;
        readProps.clear();
        writeProps.clear();
    }

    /**
     * Reads the pooling configuration from the configdb.properties file.
     */
    private void initPoolConfig() {
        poolConfig.minIdle = getInt(Property.MIN_IDLE, poolConfig.minIdle);
        poolConfig.maxIdle = getInt(Property.MAX_IDLE, poolConfig.maxIdle);
        poolConfig.maxIdleTime = getLong(Property.MAX_IDLE_TIME, poolConfig.maxIdleTime);
        poolConfig.maxActive = getInt(Property.MAX_ACTIVE, poolConfig.maxActive);
        poolConfig.maxWait = getLong(Property.MAX_WAIT, poolConfig.maxWait);
        poolConfig.maxLifeTime = getLong(Property.MAX_LIFE_TIME, poolConfig.maxLifeTime);
        poolConfig.exhaustedAction = ConnectionPool.ExhaustedActions.valueOf(getProperty(
            Property.EXHAUSTED_ACTION,
            poolConfig.exhaustedAction.name()));
        poolConfig.testOnActivate = getBoolean(Property.TEST_ON_ACTIVATE, poolConfig.testOnActivate);
        poolConfig.testOnDeactivate = getBoolean(Property.TEST_ON_DEACTIVATE, poolConfig.testOnDeactivate);
        poolConfig.testOnIdle = getBoolean(Property.TEST_ON_IDLE, poolConfig.testOnIdle);
        poolConfig.testThreads = getBoolean(Property.TEST_THREADS, poolConfig.testThreads);
        poolConfig.forceWriteOnly = getBoolean(Property.WRITE_ONLY, false);
        LOG.info(poolConfig.toString());
    }

    ConnectionPool.Config getPoolConfig() {
        return poolConfig;
    }

    /**
     * Enumeration of all properties in the configdb.properties file.
     */
    public static enum Property {
        /**
         * URL for configdb read.
         */
        READ_URL("readUrl"),
        /**
         * URL for configdb write.
         */
        WRITE_URL("writeUrl"),
        /**
         * Class name of driver for configdb read.
         */
        READ_DRIVER_CLASS("readDriverClass"),
        /**
         * Class name of driver for configdb write.
         */
        WRITE_DRIVER_CLASS("writeDriverClass"),
        /**
         * Use a seperate pool for write connections.
         */
        SEPERATE_WRITE("useSeparateWrite"),
        /**
         * Interval of the cleaner threads.
         */
        CLEANER_INTERVAL("cleanerInterval"),
        /**
         * Minimum of idle connections.
         */
        MIN_IDLE("minIdle"),
        /**
         * Maximum of idle connections.
         */
        MAX_IDLE("maxIdle"),
        /**
         * Maximum idle time.
         */
        MAX_IDLE_TIME("maxIdleTime"),
        /**
         * Maximum of active connections.
         */
        MAX_ACTIVE("maxActive"),
        /**
         * Maximum time to wait for a connection.
         */
        MAX_WAIT("maxWait"),
        /**
         * Maximum life time of a connection.
         */
        MAX_LIFE_TIME("maxLifeTime"),
        /**
         * Action if the maximum is reached.
         */
        EXHAUSTED_ACTION("exhaustedAction"),
        /**
         * Validate connections if they are activated.
         */
        TEST_ON_ACTIVATE("testOnActivate"),
        /**
         * Validate connections if they are deactivated.
         */
        TEST_ON_DEACTIVATE("testOnDeactivate"),
        /**
         * Validate connections on a pool clean run.
         */
        TEST_ON_IDLE("testOnIdle"),
        /**
         * Test threads if they use connections correctly.
         */
        TEST_THREADS("testThreads"),

        /**
         * Forces the use of the write db on all requests
         */
        WRITE_ONLY("writeOnly");

        /**
         * Name of the property in the server.properties file.
         */
        private String propertyName;

        /**
         * Default constructor.
         * @param propertyName Name of the property in the server.properties
         * file.
         */
        private Property(String propertyName) {
            this.propertyName = propertyName;
        }
    }
}
