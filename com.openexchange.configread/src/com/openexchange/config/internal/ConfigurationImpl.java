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

package com.openexchange.config.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.ho.yaml.Yaml;
import org.ho.yaml.exception.YamlException;
import com.openexchange.annotation.NonNull;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Filter;
import com.openexchange.config.ForcedReloadable;
import com.openexchange.config.PropertyFilter;
import com.openexchange.config.PropertyListener;
import com.openexchange.config.Reloadable;
import com.openexchange.config.WildcardFilter;
import com.openexchange.config.cascade.ReinitializableConfigProviderService;
import com.openexchange.config.internal.filewatcher.FileWatcher;
import com.openexchange.exception.OXException;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;

/**
 * {@link ConfigurationImpl}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ConfigurationImpl implements ConfigurationService {

    /**
     * The logger constant.
     */
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ConfigurationImpl.class);

    private static final class PropertyFileFilter implements FileFilter {

        private final String ext;
        private final String mpasswd;

        PropertyFileFilter() {
            super();
            ext = ".properties";
            mpasswd = "mpasswd";
        }

        @Override
        public boolean accept(final File pathname) {
            return pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(ext) || mpasswd.equals(pathname.getName());
        }

    }

    private static final String[] getDirectories() {
        final List<String> tmp = new ArrayList<String>();
        for (final String property : new String[] { "openexchange.propdir" }) {
            final String sysProp = System.getProperty(property);
            if (null != sysProp) {
                tmp.add(sysProp);
            }
        }
        return tmp.toArray(new String[tmp.size()]);
    }

    private static interface FileNameMatcher {

        boolean matches(String filename, File file);
    }

    private static final FileNameMatcher PATH_MATCHER = new FileNameMatcher() {

        @Override
        public boolean matches(String filename, File file) {
            return file.getPath().endsWith(filename);
        }
    };

    private static final FileNameMatcher NAME_MATCHER = new FileNameMatcher() {

        @Override
        public boolean matches(String filename, File file) {
            return file.getName().equals(filename);
        }
    };

    /*-
     * ------------- Member stuff -------------
     */

    private final ConcurrentMap<String, Reloadable> reloadableServices;

    private final Map<String, String> texts;

    private final File[] dirs;

    /** Maps file paths of the .properties file to their properties. */
    private final Map<File, Properties> propertiesByFile;

    /** Maps property names to their values. */
    private final Map<String, String> properties;

    /** Maps property names to the file path of the .properties file containing the property. */
    private final Map<String, String> propertiesFiles;

    /** Maps objects to yaml filename, with a path */
    private final Map<File, byte[]> yamlFiles;

    /** Maps filenames to whole file paths for yaml lookup */
    private final Map<String, File> yamlPaths;

    private final Map<File, byte[]> xmlFiles;

    /** The <code>ConfigProviderServiceImpl</code> reference. */
    private volatile ConfigProviderServiceImpl configProviderServiceImpl;

    private final Collection<ReinitializableConfigProviderService> reinitQueue;

    /**
     * Initializes a new configuration. The properties directory is determined by system property "<code>openexchange.propdir</code>"
     */
    public ConfigurationImpl(Collection<ReinitializableConfigProviderService> reinitQueue) {
        this(getDirectories(), reinitQueue);
    }

    /**
     * Initializes a new configuration
     *
     * @param directory The directory where property files are located
     */
    public ConfigurationImpl(String[] directories, Collection<ReinitializableConfigProviderService> reinitQueue) {
        super();
        this.reinitQueue = null == reinitQueue ? Collections.<ReinitializableConfigProviderService> emptyList() : reinitQueue;
        reloadableServices = new ConcurrentHashMap<String, Reloadable>(128, 0.9f, 1);
        propertiesByFile = new HashMap<File, Properties>(256);
        texts = new ConcurrentHashMap<String, String>(1024, 0.9f, 1);
        properties = new HashMap<String, String>(2048);
        propertiesFiles = new HashMap<String, String>(2048);
        yamlFiles = new HashMap<File, byte[]>(64);
        yamlPaths = new HashMap<String, File>(64);
        dirs = new File[directories.length];
        xmlFiles = new HashMap<File, byte[]>(2048);
        loadConfiguration(directories);
    }

    private void loadConfiguration(String[] directories) {
        if (null == directories || directories.length == 0) {
            throw new IllegalArgumentException("Missing configuration directory path.");
        }

        // First filter+processor pair
        FileFilter fileFilter = new PropertyFileFilter();
        FileProcessor processor = new FileProcessor() {

            @Override
            public void processFile(final File file) {
                processPropertiesFile(file);
            }

        };

        // Second filter+processor pair
        FileFilter fileFilter2 = new FileFilter() {

            @Override
            public boolean accept(final File pathname) {
                return pathname.isDirectory() || pathname.getName().endsWith(".yml") || pathname.getName().endsWith(".yaml");
            }

        };

        final Map<String, File> yamlPaths = this.yamlPaths;
        final Map<File, byte[]> yamlFiles = this.yamlFiles;
        FileProcessor processor2 = new FileProcessor() {

            @Override
            public void processFile(final File file) {
                yamlPaths.put(file.getName(), file);
                yamlFiles.put(file, readFile(file).getBytes());
            }

        };

        FileFilter fileFilter3 = new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".xml");
            }
        };

        final Map<File, byte[]> xmlFiles = this.xmlFiles;
        FileProcessor processor3 = new FileProcessor() {

            @Override
            public void processFile(File file) {
                byte[] hash = getHash(file);
                xmlFiles.put(file, hash);
            }
        };

        for (int i = 0; i < directories.length; i++) {
            if (null == directories[i]) {
                throw new IllegalArgumentException("Given configuration directory path is null.");
            }
            final File dir = new File(directories[i]);
            dirs[i] = dir;
            if (!dir.exists()) {
                throw new IllegalArgumentException(MessageFormat.format("Not found: \"{0}\".", directories[i]));
            } else if (!dir.isDirectory()) {
                throw new IllegalArgumentException(MessageFormat.format("Not a directory: {0}", directories[i]));
            }
            // Process: First round
            processDirectory(dir, fileFilter, processor);
            // Process: Second round
            processDirectory(dir, fileFilter2, processor2);
            // Process: Third round
            processDirectory(dir, fileFilter3, processor3);
        }
    }

    private synchronized void processDirectory(final File dir, final FileFilter fileFilter, final FileProcessor processor) {
        final File[] files = dir.listFiles(fileFilter);
        if (files == null) {
            LOG.info("Can't read {}. Skipping.", dir);
            return;
        }
        for (final File file : files) {
            if (file.isDirectory()) {
                processDirectory(file, fileFilter, processor);
            } else {
                /**
                 * Preparations for US: 55795476 Change configuration values without restarting the systems final FileWatcher fileWatcher =
                 * FileWatcher.getFileWatcher(file); fileWatcher.addFileListener(new ProcessingFileListener(file, processor));
                 */
                processor.processFile(file);
            }
        }
    }

    void processPropertiesFile(final File propFile) {
        try {
            if (!propFile.exists() || !propFile.canRead()) {
                return;
            }
            Properties tmp = loadProperties(propFile);
            propertiesByFile.put(propFile, tmp);

            String propFilePath = propFile.getPath();
            for (Map.Entry<Object, Object> e : tmp.entrySet()) {
                String propName = e.getKey().toString().trim();
                String otherValue = properties.get(propName);
                if (otherValue != null && !otherValue.equals(e.getValue())) {
                    String otherFile = propertiesFiles.get(propName);
                    LOG.debug("Overwriting property {} from file ''{}'' with property from file ''{}'', overwriting value ''{}'' with value ''{}''", propName, otherFile, propFilePath, otherValue, e.getValue());
                }
                properties.put(propName, e.getValue().toString().trim());
                propertiesFiles.put(propName, propFilePath);
            }
        } catch (IOException e) {
            LOG.warn("An I/O error occurred while processing .properties file \"{}\".", propFile, e);
        } catch (IllegalArgumentException encodingError) {
            LOG.warn("A malformed Unicode escape sequence in .properties file \"{}\".", propFile, encodingError);
        } catch (RuntimeException e) {
            LOG.warn("An error occurred while processing .properties file \"{}\".", propFile, e);
        }
    }

    private static Properties loadProperties(final File propFile) throws IOException {
        final InputStream fis = new BufferedInputStream(new FileInputStream(propFile), 65536);
        try {
            final Properties tmp = new Properties();
            tmp.load(fis);
            return tmp;
        } finally {
            Streams.close(fis);
        }
    }

    @Override
    public Filter getFilterFromProperty(final String name) {
        final String value = properties.get(name);
        if (null == value) {
            return null;
        }
        return new WildcardFilter(value);
    }

    @Override
    public String getProperty(final String name) {
        return properties.get(name);
    }

    @Override
    public String getProperty(final String name, final String defaultValue) {
        final String value = properties.get(name);
        return null == value ? defaultValue : value;
    }

    @Override
    public String getProperty(final String name, final PropertyListener listener) {
        if (watchProperty(name, listener)) {
            return properties.get(name);
        }
        return null;
    }

    @Override
    public String getProperty(final String name, final String defaultValue, final PropertyListener listener) {
        if (watchProperty(name, listener)) {
            return properties.get(name);
        }
        return defaultValue;
    }

    @Override
    public List<String> getProperty(String name, String defaultValue, String separator) {
        String property = getProperty(name, defaultValue);
        return Strings.splitAndTrim(property, separator);
    }

    @Override
    public List<String> getProperty(String name, String defaultValue, PropertyListener propertyListener, String separator) {
        if (watchProperty(name, propertyListener)) {
            return getProperty(name, defaultValue, separator);
        }
        return Strings.splitAndTrim(defaultValue, separator);
    }

    @Override
    public void removePropertyListener(final String name, final PropertyListener listener) {
        final PropertyWatcher pw = PropertyWatcher.getPropertyWatcher(name);
        if (pw != null) {
            pw.removePropertyListener(listener);
            if (pw.isEmpty()) {
                final PropertyWatcher removedWatcher = PropertyWatcher.removePropertWatcher(name);
                final FileWatcher fileWatcher = FileWatcher.optFileWatcher(new File(propertiesFiles.get(name)));
                if (null != fileWatcher) {
                    fileWatcher.removeFileListener(removedWatcher);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getFile(final String fileName) {
        return getFile(fileName, null);
    }

    /**
     * {@inheritDoc}
     */
    public Properties getFile(final String filename, final PropertyListener listener) {
        if (null == filename) {
            return new Properties();
        }

        boolean isPath = filename.indexOf(File.separatorChar) >= 0;
        FileNameMatcher matcher = isPath ? PATH_MATCHER : NAME_MATCHER;

        for (Map.Entry<File, Properties> entry : propertiesByFile.entrySet()) {
            if (matcher.matches(filename, entry.getKey())) {
                Properties retval = new Properties();
                retval.putAll(entry.getValue());
                if (listener != null) {
                    for (Object k : retval.keySet()) {
                        getProperty((String) k, listener);
                    }
                }
                return retval;
            }
        }

        return new Properties();
    }

    @Override
    public Map<String, String> getProperties(final PropertyFilter filter) throws OXException {
        if (null == filter) {
            return new HashMap<String, String>(properties);
        }
        final Map<String, String> ret = new LinkedHashMap<String, String>(32);
        for (final Entry<String, String> entry : this.properties.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (filter.accept(key, value)) {
                ret.put(key, value);
            }
        }
        return ret;
    }

    @Override
    public Properties getPropertiesInFolder(final String folderName) {
        return getPropertiesInFolder(folderName, null);
    }

    public Properties getPropertiesInFolder(final String folderName, final PropertyListener listener) {
        final Properties retval = new Properties();
        final Iterator<Entry<String, String>> iter = propertiesFiles.entrySet().iterator();
        String fldName = folderName;
        for (final File dir : dirs) {
            fldName = dir.getAbsolutePath() + File.separatorChar + fldName + File.separatorChar;
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

    /**
     * Watch a property for changes.
     *
     * @param name the name of the property to watch
     * @param propertyListener the PropertyListener to register for property changes
     * @return true if the property with the given name can be found and a watcher is added, else false
     */
    private boolean watchProperty(final String name, final PropertyListener propertyListener) {
        final String value = properties.get(name);
        if (null == value) {
            LOG.error("Unable to watch missing property: {}", name);
            return false;
        }
        final PropertyWatcher pw = PropertyWatcher.addPropertyWatcher(name, value, true);
        pw.addPropertyListener(propertyListener);
        final FileWatcher fileWatcher = FileWatcher.getFileWatcher(new File(propertiesFiles.get(name)));
        fileWatcher.addFileListener(pw);
        fileWatcher.startFileWatcher(10000);
        return true;
    }

    @Override
    public boolean getBoolProperty(final String name, final boolean defaultValue) {
        final String prop = properties.get(name);
        if (null != prop) {
            return Boolean.parseBoolean(prop.trim());
        }
        return defaultValue;
    }

    @Override
    public boolean getBoolProperty(final String name, final boolean defaultValue, final PropertyListener propertyListener) {
        if (watchProperty(name, propertyListener)) {
            return getBoolProperty(name, defaultValue);
        }
        return defaultValue;
    }

    @Override
    public int getIntProperty(final String name, final int defaultValue) {
        final String prop = properties.get(name);
        if (prop != null) {
            try {
                return Integer.parseInt(prop.trim());
            } catch (final NumberFormatException e) {
                LOG.trace("", e);
            }
        }
        return defaultValue;
    }

    @Override
    public int getIntProperty(final String name, final int defaultValue, final PropertyListener propertyListener) {
        if (watchProperty(name, propertyListener)) {
            return getIntProperty(name, defaultValue);
        }
        return defaultValue;
    }

    @Override
    public Iterator<String> propertyNames() {
        return properties.keySet().iterator();
    }

    @Override
    public int size() {
        return properties.size();
    }

    @Override
    public File getFileByName(final String fileName) {
        if (null == fileName) {
            return null;
        }
        for (final String dir : getDirectories()) {
            final File f = traverseForFile(new File(dir), fileName);
            if (f != null) {
                return f;
            }
        }
        /*
         * Try guessing the filename separator
         */
        String fn;
        int pos;
        if ((pos = fileName.lastIndexOf('/')) >= 0 || (pos = fileName.lastIndexOf('\\')) >= 0) {
            fn = fileName.substring(pos + 1);
        } else {
            LOG.warn("No such file: {}", fileName);
            return null;
        }
        for (final String dir : getDirectories()) {
            final File f = traverseForFile(new File(dir), fn);
            if (f != null) {
                return f;
            }
        }
        LOG.warn("No such file: {}", fileName);
        return null;
    }

    private File traverseForFile(final File file, final String fileName) {
        if (null == file) {
            return null;
        }
        if (file.isFile()) {
            if (fileName.equals(file.getName())) {
                // Found
                return file;
            }
            return null;
        }
        final File[] subs = file.listFiles();
        if (subs != null) {
            for (final File sub : subs) {
                final File f = traverseForFile(sub, fileName);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }

    @Override
    public File getDirectory(final String directoryName) {
        if (null == directoryName) {
            return null;
        }
        for (final String dir : getDirectories()) {
            final File fdir = traverseForDir(new File(dir), directoryName);
            if (fdir != null) {
                return fdir;
            }
        }
        LOG.warn("No such directory: {}", directoryName);
        return null;
    }

    private File traverseForDir(final File file, final String directoryName) {
        if (null == file) {
            return null;
        }
        if (file.isDirectory() && directoryName.equals(file.getName())) {
            // Found
            return file;
        }
        final File[] subDirs = file.listFiles(new FileFilter() {

            @Override
            public boolean accept(final File file) {
                return file.isDirectory();
            }
        });
        if (subDirs != null) {
            // Check first-level sub-directories first
            for (final File subDir : subDirs) {
                if (subDir.isDirectory() && directoryName.equals(subDir.getName())) {
                    return subDir;
                }
            }
            // Then check recursively
            for (final File subDir : subDirs) {
                final File dir = traverseForDir(subDir, directoryName);
                if (dir != null) {
                    return dir;
                }
            }
        }
        return null;
    }

    @Override
    public String getText(final String fileName) {
        final String text = texts.get(fileName);
        if (text != null) {
            return text;
        }

        for (final String dir : getDirectories()) {
            final String s = traverse(new File(dir), fileName);
            if (s != null) {
                texts.put(fileName, s);
                return s;
            }
        }
        return null;
    }

    private String traverse(final File file, final String filename) {
        if (null == file) {
            return null;
        }
        if (file.isFile()) {
            if (file.getName().equals(filename)) {
                return readFile(file);
            }
            return null;
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

    String readFile(final File file) {
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file));

            final StringBuilder builder = new StringBuilder((int) file.length());
            final int buflen = 8192;
            final char[] cbuf = new char[buflen];

            for (int read; (read = reader.read(cbuf, 0, buflen)) > 0;) {
                builder.append(cbuf, 0, read);
            }
            return builder.toString();
        } catch (final IOException x) {
            LOG.error("Can't read file: {}", file);
            return null;
        } finally {
            Streams.close(reader);
        }
    }

    @Override
    public Object getYaml(final String filename) {
        if (null == filename) {
            return null;
        }

        boolean isPath = filename.indexOf(File.separatorChar) >= 0;
        if (isPath) {
            FileNameMatcher matcher = PATH_MATCHER;
            for (Map.Entry<File, byte[]> entry : yamlFiles.entrySet()) {
                if (matcher.matches(filename, entry.getKey())) {
                    try {
                        return Yaml.load(Charsets.toString(entry.getValue(), Charsets.UTF_8));
                    } catch (YamlException e) {
                        // Failed to load .yml file
                        throw new IllegalStateException("Failed to load YAML file '" + entry.getKey() + "'. Reason:" + e.getMessage(), e);
                    }
                }
            }

            // No such YAML file
            return null;
        }

        // Look-up by file name
        File path = yamlPaths.get(filename);
        if (path == null) {
            path = yamlPaths.get(filename + ".yml");
            if (path == null) {
                path = yamlPaths.get(filename + ".yaml");
                if (path == null) {
                    return null;
                }
            }
        }

        try {
            return Yaml.load(Charsets.toString(yamlFiles.get(path), Charsets.UTF_8));
        } catch (YamlException e) {
            // Failed to load .yml file
            throw new IllegalStateException("Failed to load YAML file '" + path + "'. Reason:" + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getYamlInFolder(final String folderName) {
        final Map<String, Object> retval = new HashMap<String, Object>();
        final Iterator<Entry<File, byte[]>> iter = yamlFiles.entrySet().iterator();
        String fldName = folderName;
        for (final File dir : dirs) {
            fldName = dir.getAbsolutePath() + File.separatorChar + fldName + File.separatorChar;
            while (iter.hasNext()) {
                final Entry<File, byte[]> entry = iter.next();
                String pathName = entry.getKey().getPath();
                if (pathName.startsWith(fldName)) {
                    try {
                        retval.put(pathName, Yaml.load(Charsets.toString(entry.getValue(), Charsets.UTF_8)));
                    } catch (YamlException e) {
                        // Failed to load .yml file
                        throw new IllegalStateException("Failed to load YAML file '" + pathName + "'. Reason:" + e.getMessage(), e);
                    }
                }
            }
        }
        return retval;
    }

    /**
     * Propagates the reloaded configuration among registered listeners.
     */
    public void reloadConfiguration() {
        LOG.info("Reloading configuration...");

        // Copy current content to get associated files on check for expired PropertyWatchers
        final Map<File, Properties> oldPropertiesByFile = new HashMap<File, Properties>(propertiesByFile);
        final Map<File, byte[]> oldXml = new HashMap<File, byte[]>(xmlFiles);
        final Map<File, byte[]> oldYaml = new HashMap<File, byte[]>(yamlFiles);

        // Clear maps
        properties.clear();
        propertiesByFile.clear();
        propertiesFiles.clear();
        texts.clear();
        yamlFiles.clear();
        yamlPaths.clear();
        xmlFiles.clear();

        // (Re-)load configuration
        loadConfiguration(getDirectories());

        // Re-initialize config-cascade
        reinitConfigCascade();

        // Check if properties have been changed, execute only forced ones if not
        Set<File> changes = getChanges(oldPropertiesByFile, oldXml, oldYaml);
        if (changes.isEmpty()) {
            LOG.info("No changes in *.properties, *.xml, *.yaml configuration files detected");

            // Trigger only forced ones
            for (Reloadable reloadable : reloadableServices.values()) {
                try {
                    if (ForcedReloadable.class.isInstance(reloadable)) {
                        reloadable.reloadConfiguration(this);
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to let reloaded configuration be handled by: {}", reloadable.getClass().getName(), e);
                }
            }

            return;
        }

        // Continue to reload
        LOG.info("Detected changes in the following configuration files: {}", changes);

        // Propagate reloaded configuration among Reloadables
        for (Reloadable reloadable : reloadableServices.values()) {
            try {
                Map<String, String[]> configFileNames = reloadable.getConfigFileNames();
                if (null == configFileNames || configFileNames.isEmpty()) {
                    // Reloadable does not indicate the files of interest

                    reloadable.reloadConfiguration(this);
                } else {
                    // Reloadable does indicate the files of interest; thus check against changed ones

                    boolean doReload = false;
                    for (Iterator<String> it = configFileNames.keySet().iterator(); !doReload && it.hasNext();) {
                        String fileName = it.next();
                        for (File changedFilePath : changes) {
                            if (changedFilePath.getName().equals(fileName)) {
                                doReload = true;
                            }
                        }
                    }
                    if (doReload) {
                        reloadable.reloadConfiguration(this);
                    }
                }
            } catch (Exception e) {
                LOG.warn("Failed to let reloaded configuration be handled by: {}", reloadable.getClass().getName(), e);
            }
        }

        // Check for expired PropertyWatchers
        /*-
         *
        for (final PropertyWatcher watcher : watchers.values()) {
            final String propertyName = watcher.getName();
            if (!properties.containsKey(propertyName)) {
                final PropertyWatcher removedWatcher = PropertyWatcher.removePropertWatcher(propertyName);
                final FileWatcher fileWatcher = FileWatcher.optFileWatcher(new File(propertiesFilesCopy.get(propertyName)));
                if (null != fileWatcher) {
                    fileWatcher.removeFileListener(removedWatcher);
                }
            }
        }
         *
         */
    }

    private void reinitConfigCascade() {
        ConfigProviderServiceImpl configProvider = this.configProviderServiceImpl;
        boolean reinitMyProvider = true;

        for (ReinitializableConfigProviderService reinit : reinitQueue) {
            if (reinit == configProvider) {
                reinitMyProvider = false;
            }
            try {
                reinit.reinit();
                LOG.info("Re-initialized configuration provider for scope \"{}\"", reinit.getScope());
            } catch (Exception e) {
                LOG.warn("Failed to re-initialize configuration provider for scope \"{}\"", reinit.getScope(), e);
            }
        }

        if (reinitMyProvider && configProvider != null) {
            try {
                configProvider.reinit();
                LOG.info("Re-initialized configuration provider for scope \"server\"");
            } catch (Exception e) {
                LOG.warn("Failed to re-initialize configuration provider for scope \"server\"", e);
            }
        }
    }

    /**
     * Adds specified <code>Reloadable</code> instance.
     *
     * @param service The instance to add
     * @return <code>true</code> if successfully added; otherwise <code>false</code> if already present
     */
    public boolean addReloadable(Reloadable service) {
        if (null != service) {
            return null == reloadableServices.putIfAbsent(service.getClass().getName(), service);
        }
        LOG.warn("Tried to add null to reloadable services");
        return false;
    }

    /**
     * Removes specified <code>Reloadable</code> instance.
     *
     * @param service The instance to remove
     */
    public void removeReloadable(Reloadable service) {
        if (null != service) {
            reloadableServices.remove(service.getClass().getName());
        } else {
            LOG.warn("Tried to remove null from reloadable services");
        }
    }

    /**
     * Sets associated <code>ConfigProviderServiceImpl</code> instance
     *
     * @param configProviderServiceImpl The instance
     */
    public void setConfigProviderServiceImpl(ConfigProviderServiceImpl configProviderServiceImpl) {
        this.configProviderServiceImpl = configProviderServiceImpl;
    }

    @NonNull
    private Set<File> getChanges(Map<File, Properties> oldPropertiesByFile, Map<File, byte[]> oldXml, Map<File, byte[]> oldYaml) {
        Set<File> result = new HashSet<File>(oldPropertiesByFile.size());

        // Check for changes in .properties files
        for (Map.Entry<File, Properties> newEntry : propertiesByFile.entrySet()) {
            File pathname = newEntry.getKey();
            Properties newProperties = newEntry.getValue();
            Properties oldProperties = oldPropertiesByFile.get(pathname);
            if (null == oldProperties || !newProperties.equals(oldProperties)) {
                // New or changed .properties file
                result.add(pathname);
            }
        }
        {
            Set<File> removedFiles = new HashSet<File>(oldPropertiesByFile.keySet());
            removedFiles.removeAll(propertiesByFile.keySet());
            result.addAll(removedFiles);
        }

        // Do the same for XML files
        for (File file : xmlFiles.keySet()) {
            byte[] oldHash = oldXml.get(file);
            byte[] newHash = xmlFiles.get(file);
            if (null == oldHash || !Arrays.equals(oldHash, newHash)) {
                result.add(file);
            }
        }
        {
            Set<File> removedXml = new HashSet<File>(oldXml.keySet());
            removedXml.removeAll(xmlFiles.keySet());
            result.addAll(removedXml);
        }

        // ... and one more time for YAMLs
        for (File filename : yamlFiles.keySet()) {
            byte[] oldHash = oldYaml.get(filename);
            byte[] newHash = yamlFiles.get(filename);
            if (null == oldHash || !Arrays.equals(oldHash, newHash)) {
                result.add(filename);
            }
        }
        {
            Set<File> removedYaml = new HashSet<File>(oldYaml.keySet());
            removedYaml.removeAll(yamlFiles.keySet());
            result.addAll(removedYaml);
        }

        return result;
    }

    /**
     * Gets all currently tracked <code>Reloadable</code> instances.
     *
     * @return The <code>Reloadable</code> instances
     */
    public Collection<Reloadable> getReloadables() {
        return reloadableServices.values();
    }

    byte[] getHash(File file) {
        byte[] retval = null;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
            md.update(readFile(file).getBytes());
            retval = md.digest();
        } catch (NoSuchAlgorithmException e) {
            // Should not happen
        }
        return retval;
    }

}
