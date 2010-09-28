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

package com.openexchange.config.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.PropertyListener;
import com.openexchange.config.internal.filewatcher.FileWatcher;

/**
 * {@link ConfigurationImpl}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ConfigurationImpl implements ConfigurationService {

    private static final Log LOG = LogFactory.getLog(ConfigurationImpl.class);

    private static final String EXT = ".properties";

    private static final class PropertyFileFilter implements FileFilter {

        public PropertyFileFilter() {
            super();
        }

        public boolean accept(final File pathname) {
            return pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(EXT);
        }

    }

    private static final String[] getDirectories() {
        final List<String> tmp = new ArrayList<String>();
        for (final String property : new String[] { "openexchange.propdir", "openexchange.propdir2" }) {
            final String sysProp = System.getProperty(property);
            if (null != sysProp) {
                tmp.add(sysProp);
            }
        }
        return tmp.toArray(new String[tmp.size()]);
    }

    /*-
     * ------------- Member stuff -------------
     */

    private final Map<String, String> texts;

    private final File[] dirs;

    /**
     * Maps file paths of the .properties file to their properties.
     */
    private final Map<String, Properties> propertiesByFile;

    /**
     * Maps property names to their values.
     */
    private final Map<String, String> properties;

    /**
     * Maps property names to the file path of the .properties file containing the property.
     */
    private final Map<String, String> propertiesFiles;

    /**
     * Initializes a new configuration. The properties directory is determined by system property "<code>openexchange.propdir</code>"
     */
    public ConfigurationImpl() {
        this(getDirectories());
    }

    /**
     * Initializes a new configuration
     * 
     * @param directory The directory where property files are located
     */
    public ConfigurationImpl(final String[] directories) {
        super();
        if (null == directories || directories.length == 0) {
            throw new IllegalArgumentException("Missing configuration directory path.");
        }
        propertiesByFile = new HashMap<String, Properties>();
        texts = new ConcurrentHashMap<String, String>();
        properties = new HashMap<String, String>();
        propertiesFiles = new HashMap<String, String>();
        final FileFilter fileFilter = new PropertyFileFilter();
        dirs = new File[directories.length];
        for (int i = 0; i < directories.length; i++) {
            if (null == directories[i]) {
                throw new IllegalArgumentException("Given configuration directory path is null.");
            }
            dirs[i] = new File(directories[i]);
            if (!dirs[i].exists()) {
                throw new IllegalArgumentException(MessageFormat.format("Not found: \"{0}\".", directories[i]));
            } else if (!dirs[i].isDirectory()) {
                throw new IllegalArgumentException(MessageFormat.format("Not a directory: {0}", directories[i]));
            }
            processDirectory(dirs[i], fileFilter);
        }
    }

    private void processDirectory(final File dir, final FileFilter fileFilter) {
        final File[] files = dir.listFiles(fileFilter);
        if (files == null) {
            LOG.info(MessageFormat.format("Can't read {0}. Skipping.", dir));
            return;
        }
        for (final File file : files) {
            if (file.isDirectory()) {
                processDirectory(file, fileFilter);
            } else {
                processPropertiesFile(file);
            }
        }
    }

    private void processPropertiesFile(final File propFile) {
        try {
            final Properties tmp = loadProperties(propFile);
            final String propFilePath = propFile.getPath();
            propertiesByFile.put(propFilePath, tmp);
            final int size = tmp.size();
            final Iterator<Entry<Object, Object>> iter = tmp.entrySet().iterator();
            for (int i = 0; i < size; i++) {
                final Entry<Object, Object> e = iter.next();
                final String propName = e.getKey().toString().trim();
                final String otherValue = properties.get(propName);
                if (properties.containsKey(propName) && otherValue != null && !otherValue.equals(e.getValue())) {
                    final String otherFile = propertiesFiles.get(propName);
                    if (LOG.isWarnEnabled()) {
                        final StringBuilder sb =
                            new StringBuilder(64).append("Overwriting property ").append(propName).append(" from file '");
                        sb.append(otherFile).append("' with property from file '").append(propFilePath).append("', overwriting value '");
                        sb.append(otherValue).append("' with value '").append(e.getValue()).append("'.");
                        LOG.warn(sb.toString());
                    }
                }
                properties.put(propName, e.getValue().toString().trim());
                propertiesFiles.put(propName, propFilePath);
            }
        } catch (final IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private static Properties loadProperties(final File propFile) throws IOException {
        final FileInputStream fis = new FileInputStream(propFile);
        try {
            final Properties tmp = new Properties();
            tmp.load(fis);
            return tmp;
        } finally {
            try {
                fis.close();
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public String getProperty(final String name) {
        return properties.get(name);
    }

    public String getProperty(final String name, final String defaultValue) {
        return properties.containsKey(name) ? properties.get(name) : defaultValue;
    }

    public String getProperty(final String name, final PropertyListener listener) {
        if (properties.containsKey(name)) {
            final PropertyWatcher pw = PropertyWatcher.addPropertyWatcher(name, properties.get(name), true);
            pw.addPropertyListener(listener);
            final FileWatcher fileWatcher = FileWatcher.getFileWatcher(new File(propertiesFiles.get(name)));
            fileWatcher.addFileListener(pw);
            fileWatcher.startFileWatcher(10000);
            return properties.get(name);
        }
        return null;
    }

    public String getProperty(final String name, final String defaultValue, final PropertyListener listener) {
        if (properties.containsKey(name)) {
            final PropertyWatcher pw = PropertyWatcher.addPropertyWatcher(name, properties.get(name), true);
            pw.addPropertyListener(listener);
            final FileWatcher fileWatcher = FileWatcher.getFileWatcher(new File(propertiesFiles.get(name)));
            fileWatcher.addFileListener(pw);
            fileWatcher.startFileWatcher(10000);
            return properties.get(name);
        }
        return defaultValue;
    }

    public void removePropertyListener(final String name, final PropertyListener listener) {
        final PropertyWatcher pw = PropertyWatcher.getPropertyWatcher(name);
        if (pw != null) {
            pw.removePropertyListener(listener);
            if (pw.isEmpty()) {
                PropertyWatcher.removePropertWatcher(name);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Properties getFile(final String filename) {
        return getFile(filename, null);
    }

    /**
     * {@inheritDoc}
     */
    public Properties getFile(final String filename, final PropertyListener listener) {
        String key = null;
        for (final String k : propertiesByFile.keySet()) {
            if (k.endsWith(filename)) {
                key = k;
                break;
            }
        }

        if (key == null) {
            return new Properties();
        }

        final Properties tmp = propertiesByFile.get(key);
        final Properties retval = new Properties();

        for (final Entry<Object, Object> entry : tmp.entrySet()) {
            retval.put(entry.getKey(), entry.getValue());
        }

        if (listener != null) {
            for (final Object k : retval.keySet()) {
                getProperty((String) k, listener);
            }
        }
        return retval;
    }

    public Properties getPropertiesInFolder(final String folderName) {
        return getPropertiesInFolder(folderName, null);
    }

    public Properties getPropertiesInFolder(final String folderName, final PropertyListener listener) {
        final Properties retval = new Properties();
        final Iterator<Entry<String, String>> iter = propertiesFiles.entrySet().iterator();
        String fldName = folderName;
        for (final File dir : dirs) {
            fldName = dir.getAbsolutePath() + "/" + fldName + "/";
            while (iter.hasNext()) {
                final Entry<String, String> entry = iter.next();
                if (entry.getValue().startsWith(fldName)) {
                    final String value;
                    if (null == listener) {
                        value = getProperty(entry.getKey());
                    } else {
                        value = getProperty(entry.getKey(), listener);
                    } // FIXME: this could have been overridden by some property
                      // external to the requested folder.
                    retval.put(entry.getKey(), value);
                }
            }
        }
        return retval;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.config.Configuration#getProperty(java.lang.String, boolean)
     */
    public boolean getBoolProperty(final String name, final boolean defaultValue) {
        final String prop = properties.get(name);
        if (null != prop) {
            return Boolean.parseBoolean(prop.trim());
        }
        return defaultValue;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.config.Configuration#getProperty(java.lang.String, int)
     */
    public int getIntProperty(final String name, final int defaultValue) {
        final String prop = properties.get(name);
        if (prop != null) {
            try {
                return Integer.parseInt(prop.trim());
            } catch (final NumberFormatException e) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(e.getMessage(), e);
                }
            }
        }
        return defaultValue;
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.config.Configuration#propertyNames()
     */
    public Iterator<String> propertyNames() {
        return properties.keySet().iterator();
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.config.Configuration#size()
     */
    public int size() {
        return properties.size();
    }

    public String getText(final String filename) {
        final String text = texts.get(filename);
        if (text != null) {
            return text;
        }

        final String[] directories = getDirectories();
        for (final String dir : directories) {
            final String s = traverse(new File(dir), filename);
            if (s != null) {
                texts.put(filename, s);
                return s;
            }
        }
        return null;
    }

    private String traverse(final File file, final String filename) {
        if (file.getName().equals(filename) && file.isFile()) {
            BufferedReader r = null;
            try {
                r = new BufferedReader(new FileReader(file));
                final StringBuilder builder = new StringBuilder();
                String s = null;
                while ((s = r.readLine()) != null) {
                    builder.append(s).append("\n");
                }
                return builder.toString();
            } catch (final IOException x) {
                LOG.fatal("Can't read file: " + file);
                return null;
            } finally {
                if (r != null) {
                    try {
                        r.close();
                    } catch (final IOException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
        }
        final File[] files = file.listFiles();
        if (files != null) {
            for (final File f : files) {
                final String s = traverse(f, filename);
                if (s != null) {
                    return s;
                }
            }
        }
        return null;
    }
}
