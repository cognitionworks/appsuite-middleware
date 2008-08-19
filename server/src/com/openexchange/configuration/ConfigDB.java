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

package com.openexchange.configuration;

import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openexchange.configuration.ConfigurationException.Code;
import com.openexchange.server.Initialization;
import com.openexchange.tools.conf.AbstractConfig;

/**
 * Contains the settings for the ConfigDB.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class ConfigDB extends AbstractConfig implements Initialization {

    private static ConfigDB singleton = new ConfigDB();

    private static final SystemConfig.Property KEY = SystemConfig.Property
        .CONFIGDB;

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog(ConfigDB.class);

    private Properties readProps = new Properties();

    private Properties writeProps = new Properties();

    /**
     * Prevent instantiation.
     */
    private ConfigDB() {
        super();
    }

    public boolean isWriteDefined() {
        return Boolean.parseBoolean(getProperty(Property.SEPERATE_WRITE,
            "false"));
    }

    public String getReadUrl() {
        return getProperty(Property.READ_URL);
    }

    public Properties getReadProps() {
        return readProps;
    }

    public String getWriteUrl() {
        return getProperty(Property.WRITE_URL);
    }

    public Properties getWriteProps() {
        return writeProps;
    }

    private String getProperty(final Property property) {
        return getProperty(property, null);
    }

    private interface Convert<T> {
        T convert(String toConvert);
    }

    private <T> T getUniversal(final Property property, final T def,
        final Convert<T> converter) {
        final T retval;
        if (containsPropertyInternal(property.propertyName)) {
            retval = converter.convert(getPropertyInternal(property
                .propertyName));
        } else {
            retval = def;
        }
        return retval;
    }

    public String getProperty(final Property property,
        final String def) {
        return getUniversal(property, def, new Convert<String>() {
            public String convert(final String toConvert) {
                return toConvert;
            }
        });
    }

    public int getInt(final Property property, final int def) {
        return getUniversal(property, Integer.valueOf(def), new Convert<Integer>() {
            public Integer convert(final String toConvert) {
                return Integer.valueOf(toConvert);
            }
        }).intValue();
    }

    public long getLong(final Property property, final long def) {
        return getUniversal(property, Long.valueOf(def), new Convert<Long>() {
            public Long convert(final String toConvert) {
                return Long.valueOf(toConvert);
            }
        }).longValue();
    }

    public boolean getBoolean(final Property property, final boolean def) {
        return getUniversal(property, Boolean.valueOf(def), new Convert<Boolean>() {
            public Boolean convert(final String toConvert) {
                return Boolean.valueOf(toConvert);
            }
        }).booleanValue();
    }

    /**
     * @return the singleton instance.
     */
    public static ConfigDB getInstance() {
        return singleton;
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws ConfigurationException {
        if (isPropertiesLoadInternal()) {
            LOG.error("Duplicate initialization of ConfigDB.");
            return;
        }
        loadPropertiesInternal();
        separateReadWrite();
        loadDrivers();
    }

    private void separateReadWrite() {
        final Iterator<String> iter = keyIterator();
        while (iter.hasNext()) {
            final String key = iter.next();
            if (key.startsWith("readProperty.")) {
                final String value = getPropertyInternal(key);
                final int equalSignPos = value.indexOf('=');
                final String readKey = value.substring(0, equalSignPos);
                final String readValue = value.substring(equalSignPos + 1);
                readProps.put(readKey, readValue);
            } else
            if (key.startsWith("writeProperty.")) {
                final String value = getPropertyInternal(key);
                final int equalSignPos = value.indexOf('=');
                final String readKey = value.substring(0, equalSignPos);
                final String readValue = value.substring(equalSignPos + 1);
                writeProps.put(readKey, readValue);
            }
        }
    }

    private void loadDrivers() throws ConfigurationException {
        final String readDriverClass = getProperty(Property.READ_DRIVER_CLASS);
        if (null == readDriverClass) {
            throw new ConfigurationException(Code.PROPERTY_MISSING,
                Property.READ_DRIVER_CLASS.propertyName);
        }
        try {
            Class.forName(readDriverClass);
        } catch (final ClassNotFoundException e) {
            throw new ConfigurationException(Code.CLASS_NOT_FOUND, e,
                readDriverClass);
        }
        if (!isWriteDefined()) {
            return;
        }
        final String writeDriverClass = getProperty(Property
            .WRITE_DRIVER_CLASS);
        if (null == writeDriverClass) {
            throw new ConfigurationException(Code.PROPERTY_MISSING,
                Property.WRITE_DRIVER_CLASS.propertyName);
        }
        try {
            Class.forName(writeDriverClass);
        } catch (final ClassNotFoundException e) {
            throw new ConfigurationException(Code.CLASS_NOT_FOUND, e,
                writeDriverClass);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        clearProperties();
        readProps = new Properties();
        writeProps = new Properties();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getPropertyFileName() throws ConfigurationException {
        final String fileName = SystemConfig.getProperty(KEY);
        if (null == fileName) {
            throw new ConfigurationException(Code.PROPERTY_MISSING,
                KEY.getPropertyName());
        }
        return fileName;
    }

    /**
     * Enumeration of all properties in the configdb.properties file.
     */
    public static enum Property {
        /**
         * Name of the server.
         */
        SERVER_NAME("SERVER_NAME"),
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
        private Property(final String propertyName) {
            this.propertyName = propertyName;
        }
    }
}
