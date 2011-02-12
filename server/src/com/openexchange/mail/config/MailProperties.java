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

package com.openexchange.mail.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.mail.MailException;
import com.openexchange.mail.api.IMailProperties;
import com.openexchange.mail.api.MailConfig.LoginSource;
import com.openexchange.mail.api.MailConfig.PasswordSource;
import com.openexchange.mail.api.MailConfig.ServerSource;
import com.openexchange.mail.partmodifier.DummyPartModifier;
import com.openexchange.mail.partmodifier.PartModifier;
import com.openexchange.server.services.ServerServiceRegistry;

/**
 * {@link MailProperties} - Global mail properties read from properties file.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MailProperties implements IMailProperties {

    private static final Log LOG = LogFactory.getLog(MailProperties.class);

    private static volatile MailProperties instance;

    /**
     * Gets the singleton instance of {@link MailProperties}.
     * 
     * @return The singleton instance of {@link MailProperties}
     */
    public static MailProperties getInstance() {
        MailProperties tmp = instance;
        if (null == tmp) {
            synchronized (MailProperties.class) {
                tmp = instance;
                if (null == tmp) {
                    tmp = instance = new MailProperties();
                }
            }
        }
        return tmp;
    }

    /**
     * Releases the singleton instance of {@link MailProperties}.
     */
    public static void releaseInstance() {
        if (null != instance) {
            synchronized (MailProperties.class) {
                if (null != instance) {
                    instance = null;
                }
            }
        }
    }

    private final AtomicBoolean loaded;

    /*-
     * Fields for global properties
     */

    private LoginSource loginSource;

    private PasswordSource passwordSource;

    private ServerSource mailServerSource;

    private ServerSource transportServerSource;

    private String mailServer;

    private String transportServer;

    private String masterPassword;

    private int mailFetchLimit;

    private int attachDisplaySize;

    private boolean userFlagsEnabled;

    private boolean allowNestedDefaultFolderOnAltNamespace;

    private String defaultMimeCharset;

    private boolean ignoreSubscription;

    private boolean ignorePOP3StorageFolders;

    private char defaultSeparator;

    private String[] quoteLineColors;

    private Properties javaMailProperties;

    private boolean watcherEnabled;

    private int watcherTime;

    private int watcherFrequency;

    private boolean watcherShallClose;

    private boolean supportSubscription;

    private String[] phishingHeaders;

    private String defaultMailProvider;

    private boolean adminMailLoginEnabled;

    private int mailAccessCacheShrinkerSeconds;

    private int mailAccessCacheIdleSeconds;

    private boolean addClientIPAddress;
    
    private boolean rateLimitPrimaryOnly;
    
    private int rateLimit;
    
    private int maxToCcBcc;

    /**
     * Initializes a new {@link MailProperties}
     */
    private MailProperties() {
        super();
        loaded = new AtomicBoolean();
        defaultSeparator = '/';
    }

    /**
     * Exclusively loads the global mail properties
     * 
     * @throws MailConfigException If loading of global mail properties fails
     */
    public void loadProperties() throws MailConfigException {
        if (!loaded.get()) {
            synchronized (loaded) {
                if (!loaded.get()) {
                    loadProperties0();
                    loaded.set(true);
                    loaded.notifyAll();
                }
            }
        }
    }

    /**
     * Exclusively resets the global mail properties
     */
    public void resetProperties() {
        if (loaded.get()) {
            synchronized (loaded) {
                if (loaded.get()) {
                    resetFields();
                    loaded.set(false);
                }
            }
        }
    }

    /**
     * Waits for loading this properties.
     * 
     * @throws InterruptedException If another thread interrupted the current thread before or while the current thread was waiting for
     *             loading the properties.
     */
    public void waitForLoading() throws InterruptedException {
        if (!loaded.get()) {
            synchronized (loaded) {
                while (!loaded.get()) {
                    loaded.wait();
                }
            }
        }
    }

    private void resetFields() {
        loginSource = null;
        passwordSource = null;
        mailServerSource = null;
        transportServerSource = null;
        mailServer = null;
        transportServer = null;
        masterPassword = null;
        mailFetchLimit = 0;
        attachDisplaySize = 0;
        userFlagsEnabled = false;
        allowNestedDefaultFolderOnAltNamespace = false;
        defaultMimeCharset = null;
        ignoreSubscription = false;
        ignorePOP3StorageFolders = false;
        defaultSeparator = '/';
        quoteLineColors = null;
        javaMailProperties = null;
        watcherEnabled = false;
        watcherTime = 0;
        watcherFrequency = 0;
        watcherShallClose = false;
        supportSubscription = false;
        defaultMailProvider = null;
        adminMailLoginEnabled = false;
        mailAccessCacheShrinkerSeconds = 0;
        mailAccessCacheIdleSeconds = 0;
        addClientIPAddress = false;
        rateLimitPrimaryOnly = true;
        rateLimit = 0;
        maxToCcBcc = 0;
    }

    private void loadProperties0() throws MailConfigException {
        final StringBuilder logBuilder = new StringBuilder(1024);
        logBuilder.append("\nLoading global mail properties...\n");

        final ConfigurationService configuration = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);

        {
            final String loginStr = configuration.getProperty("com.openexchange.mail.loginSource");
            if (loginStr == null) {
                throw new MailConfigException("Property \"com.openexchange.mail.loginSource\" not set");
            }
            final LoginSource loginSource = LoginSource.parse(loginStr.trim());
            if (null == loginSource) {
                throw new MailConfigException(new StringBuilder(256).append(
                    "Unknown value in property \"com.openexchange.mail.loginSource\": ").append(loginStr).toString());
            }
            this.loginSource = loginSource;
            logBuilder.append("\tLogin Source: ").append(this.loginSource.toString()).append('\n');
        }

        {
            final String pwStr = configuration.getProperty("com.openexchange.mail.passwordSource");
            if (pwStr == null) {
                throw new MailConfigException("Property \"com.openexchange.mail.passwordSource\" not set");
            }
            final PasswordSource pwSource = PasswordSource.parse(pwStr.trim());
            if (null == pwSource) {
                throw new MailConfigException(new StringBuilder(256).append(
                    "Unknown value in property \"com.openexchange.mail.passwordSource\": ").append(pwStr).toString());
            }
            passwordSource = pwSource;
            logBuilder.append("\tPassword Source: ").append(passwordSource.toString()).append('\n');
        }

        {
            final String mailSrcStr = configuration.getProperty("com.openexchange.mail.mailServerSource");
            if (mailSrcStr == null) {
                throw new MailConfigException("Property \"com.openexchange.mail.mailServerSource\" not set");
            }
            final ServerSource mailServerSource = ServerSource.parse(mailSrcStr.trim());
            if (null == mailServerSource) {
                throw new MailConfigException(new StringBuilder(256).append(
                    "Unknown value in property \"com.openexchange.mail.mailServerSource\": ").append(mailSrcStr).toString());
            }
            this.mailServerSource = mailServerSource;
            logBuilder.append("\tMail Server Source: ").append(this.mailServerSource.toString()).append('\n');
        }

        {
            final String transSrcStr = configuration.getProperty("com.openexchange.mail.transportServerSource");
            if (transSrcStr == null) {
                throw new MailConfigException("Property \"com.openexchange.mail.transportServerSource\" not set");
            }
            final ServerSource transportServerSource = ServerSource.parse(transSrcStr.trim());
            if (null == transportServerSource) {
                throw new MailConfigException(new StringBuilder(256).append(
                    "Unknown value in property \"com.openexchange.mail.transportServerSource\": ").append(transSrcStr).toString());
            }
            this.transportServerSource = transportServerSource;
            logBuilder.append("\tTransport Server Source: ").append(this.transportServerSource.toString()).append('\n');
        }

        {
            mailServer = configuration.getProperty("com.openexchange.mail.mailServer");
            if (mailServer != null) {
                mailServer = mailServer.trim();
            }
        }

        {
            transportServer = configuration.getProperty("com.openexchange.mail.transportServer");
            if (transportServer != null) {
                transportServer = transportServer.trim();
            }
        }

        {
            masterPassword = configuration.getProperty("com.openexchange.mail.masterPassword");
            if (masterPassword != null) {
                masterPassword = masterPassword.trim();
            }
        }

        final String fallbackPrefix = "\". Setting to fallback: ";
        {
            final String mailFetchLimitStr = configuration.getProperty("com.openexchange.mail.mailFetchLimit", "1000").trim();
            try {
                mailFetchLimit = Integer.parseInt(mailFetchLimitStr);
                logBuilder.append("\tMail Fetch Limit: ").append(mailFetchLimit).append('\n');
            } catch (final NumberFormatException e) {
                mailFetchLimit = 1000;
                logBuilder.append("\tMail Fetch Limit: Non parseable value \"").append(mailFetchLimitStr).append(fallbackPrefix).append(
                    mailFetchLimit).append('\n');
            }
        }

        {
            final String attachDisplaySizeStr = configuration.getProperty("com.openexchange.mail.attachmentDisplaySizeLimit", "8192").trim();
            try {
                attachDisplaySize = Integer.parseInt(attachDisplaySizeStr);
                logBuilder.append("\tAttachment Display Size Limit: ").append(attachDisplaySize).append('\n');
            } catch (final NumberFormatException e) {
                attachDisplaySize = 8192;
                logBuilder.append("\tAttachment Display Size Limit: Non parseable value \"").append(attachDisplaySizeStr).append(
                    fallbackPrefix).append(attachDisplaySize).append('\n');
            }
        }

        {
            final String tmp = configuration.getProperty("com.openexchange.mail.mailAccessCacheShrinkerSeconds", "3").trim();
            try {
                mailAccessCacheShrinkerSeconds = Integer.parseInt(tmp);
                logBuilder.append("\tMail Access Cache shrinker-interval seconds: ").append(mailAccessCacheShrinkerSeconds).append('\n');
            } catch (final NumberFormatException e) {
                mailAccessCacheShrinkerSeconds = 3;
                logBuilder.append("\tMail Access Cache shrinker-interval seconds: Non parseable value \"").append(tmp).append(
                    fallbackPrefix).append(mailAccessCacheShrinkerSeconds).append('\n');
            }
        }

        {
            final String tmp = configuration.getProperty("com.openexchange.mail.mailAccessCacheIdleSeconds", "7").trim();
            try {
                mailAccessCacheIdleSeconds = Integer.parseInt(tmp);
                logBuilder.append("\tMail Access Cache idle seconds: ").append(mailAccessCacheIdleSeconds).append('\n');
            } catch (final NumberFormatException e) {
                mailAccessCacheIdleSeconds = 7;
                logBuilder.append("\tMail Access Cache idle seconds: Non parseable value \"").append(tmp).append(fallbackPrefix).append(
                    mailAccessCacheIdleSeconds).append('\n');
            }
        }

        {
            final String userFlagsStr = configuration.getProperty("com.openexchange.mail.userFlagsEnabled", "false").trim();
            userFlagsEnabled = Boolean.parseBoolean(userFlagsStr);
            logBuilder.append("\tUser Flags Enabled: ").append(userFlagsEnabled).append('\n');
        }

        {
            final String allowNestedStr = configuration.getProperty("com.openexchange.mail.allowNestedDefaultFolderOnAltNamespace", "false").trim();
            allowNestedDefaultFolderOnAltNamespace = Boolean.parseBoolean(allowNestedStr);
            logBuilder.append("\tAllow Nested Default Folders on AltNamespace: ").append(allowNestedDefaultFolderOnAltNamespace).append(
                '\n');
        }

        {
            final String defaultMimeCharsetStr = configuration.getProperty("mail.mime.charset", "UTF-8").trim();
            /*
             * Check validity
             */
            try {
                Charset.forName(defaultMimeCharsetStr);
                defaultMimeCharset = defaultMimeCharsetStr;
                logBuilder.append("\tDefault MIME Charset: ").append(defaultMimeCharset).append('\n');
            } catch (final Throwable t) {
                defaultMimeCharset = "UTF-8";
                logBuilder.append("\tDefault MIME Charset: Unsupported charset \"").append(defaultMimeCharsetStr).append(fallbackPrefix).append(
                    defaultMimeCharset).append('\n');
            }
            /*
             * Add to system properties, too
             */
            System.getProperties().setProperty("mail.mime.charset", defaultMimeCharset);
        }

        {
            final String defaultMailProviderStr = configuration.getProperty("com.openexchange.mail.defaultMailProvider", "imap").trim();
            defaultMailProvider = defaultMailProviderStr;
            logBuilder.append("\tDefault Mail Provider: ").append(defaultMailProvider).append('\n');
        }

        {
            final String adminMailLoginEnabledStr = configuration.getProperty("com.openexchange.mail.adminMailLoginEnabled", "false").trim();
            adminMailLoginEnabled = Boolean.parseBoolean(adminMailLoginEnabledStr);
            logBuilder.append("\tAdmin Mail Login Enabled: ").append(adminMailLoginEnabled).append('\n');
        }

        {
            final String ignoreSubsStr = configuration.getProperty("com.openexchange.mail.ignoreSubscription", "false").trim();
            ignoreSubscription = Boolean.parseBoolean(ignoreSubsStr);
            logBuilder.append("\tIgnore Folder Subscription: ").append(ignoreSubscription).append('\n');
        }

        {
            final String tmp = configuration.getProperty("com.openexchange.mail.ignorePOP3StorageFolders", "false").trim();
            ignorePOP3StorageFolders = Boolean.parseBoolean(tmp);
            logBuilder.append("\tIgnore POP3 Storage Folders: ").append(ignorePOP3StorageFolders).append('\n');
        }

        {
            final String supSubsStr = configuration.getProperty("com.openexchange.mail.supportSubscription", "true").trim();
            supportSubscription = Boolean.parseBoolean(supSubsStr);
            logBuilder.append("\tSupport Subscription: ").append(supportSubscription).append('\n');
        }

        {
            final String tmp = configuration.getProperty("com.openexchange.mail.addClientIPAddress", "false").trim();
            addClientIPAddress = Boolean.parseBoolean(tmp);
            logBuilder.append("\tAdd Client IP Address: ").append(addClientIPAddress).append('\n');
        }

        {
            final char defaultSep = configuration.getProperty("com.openexchange.mail.defaultSeparator", "/").trim().charAt(0);
            if (defaultSep <= 32) {
                defaultSeparator = '/';
                logBuilder.append("\tDefault Separator: Invalid separator (decimal ascii value=").append((int) defaultSep).append(
                    "). Setting to fallback: ").append(defaultSeparator).append('\n');
            } else {
                defaultSeparator = defaultSep;
                logBuilder.append("\tDefault Separator: ").append(defaultSeparator).append('\n');
            }
        }

        {
            final String partModifierStr = configuration.getProperty(
                "com.openexchange.mail.partModifierImpl",
                DummyPartModifier.class.getName()).trim();
            try {
                PartModifier.init(partModifierStr);
                logBuilder.append("\tPartModifier Implementation: ").append(PartModifier.getInstance().getClass().getName()).append('\n');
            } catch (final MailException e) {
                try {
                    PartModifier.init(DummyPartModifier.class.getName());
                } catch (final MailException e1) {
                    /*
                     * Cannot occur
                     */
                    LOG.error(e.getMessage(), e);
                }
                logBuilder.append("\tPartModifier Implementation: Unknown class \"").append(partModifierStr).append(fallbackPrefix).append(
                    DummyPartModifier.class.getName()).append('\n');
            }
        }

        {
            final String quoteColors = configuration.getProperty("com.openexchange.mail.quoteLineColors", "#666666").trim();
            if (Pattern.matches("((#[0-9a-fA-F&&[^,]]{6})(?:\r?\n|\\z|\\s*,\\s*))+", quoteColors)) {
                quoteLineColors = quoteColors.split("\\s*,\\s*");
                logBuilder.append("\tHTML Quote Colors: ").append(quoteColors).append('\n');
            } else {
                quoteLineColors = new String[] { "#666666" };
                logBuilder.append("\tHTML Quote Colors: Invalid sequence of colors \"").append(quoteColors).append(
                    "\". Setting to fallback: #666666").append('\n');
            }
        }

        {
            final String watcherEnabledStr = configuration.getProperty("com.openexchange.mail.watcherEnabled", "false").trim();
            watcherEnabled = Boolean.parseBoolean(watcherEnabledStr);
            logBuilder.append("\tWatcher Enabled: ").append(watcherEnabled).append('\n');
        }

        {
            final String watcherTimeStr = configuration.getProperty("com.openexchange.mail.watcherTime", "60000").trim();
            try {
                watcherTime = Integer.parseInt(watcherTimeStr);
                logBuilder.append("\tWatcher Time: ").append(watcherTime).append('\n');
            } catch (final NumberFormatException e) {
                watcherTime = 60000;
                logBuilder.append("\tWatcher Time: Invalid value \"").append(watcherTimeStr).append(fallbackPrefix).append(watcherTime).append(
                    '\n');
            }
        }

        {
            final String watcherFeqStr = configuration.getProperty("com.openexchange.mail.watcherFrequency", "10000").trim();
            try {
                watcherFrequency = Integer.parseInt(watcherFeqStr);
                logBuilder.append("\tWatcher Frequency: ").append(watcherFrequency).append('\n');
            } catch (final NumberFormatException e) {
                watcherFrequency = 10000;
                logBuilder.append("\tWatcher Frequency: Invalid value \"").append(watcherFeqStr).append(fallbackPrefix).append(
                    watcherFrequency).append('\n');
            }
        }

        {
            final String watcherShallCloseStr = configuration.getProperty("com.openexchange.mail.watcherShallClose", "false").trim();
            watcherShallClose = Boolean.parseBoolean(watcherShallCloseStr);
            logBuilder.append("\tWatcher Shall Close: ").append(watcherShallClose).append('\n');
        }

        {
            final String phishingHdrsStr = configuration.getProperty("com.openexchange.mail.phishingHeader", "").trim();
            if (null != phishingHdrsStr && phishingHdrsStr.length() > 0) {
                phishingHeaders = phishingHdrsStr.split(" *, *");
            } else {
                phishingHeaders = null;
            }
        }

        {
            final String rateLimitPrimaryOnlyStr = configuration.getProperty("com.openexchange.mail.rateLimitPrimaryOnly", "true").trim();
            rateLimitPrimaryOnly = Boolean.parseBoolean(rateLimitPrimaryOnlyStr);
            logBuilder.append("\tRate limit primary only: ").append(rateLimitPrimaryOnly).append('\n');
        }
        
        {
            final String rateLimitStr = configuration.getProperty("com.openexchange.mail.rateLimit", "0").trim();
            try {
                rateLimit = Integer.parseInt(rateLimitStr);
                logBuilder.append("\tSent Rate limit: ").append(rateLimit).append('\n');
            } catch (final NumberFormatException e) {
                rateLimit = 0;
                logBuilder.append("\tSent Rate limit: Invalid value \"").append(rateLimitStr).append("\". Setting to fallback ").append(
                    rateLimit).append('\n');
                
            }
        }

        {
            final String maxToCcBccStr = configuration.getProperty("com.openexchange.mail.maxToCcBcc", "0").trim();
            try {
                maxToCcBcc = Integer.parseInt(maxToCcBccStr);
                logBuilder.append("\tmaxToCcBcc: ").append(maxToCcBcc).append('\n');
            } catch (final NumberFormatException e) {
                maxToCcBcc = 0;
                logBuilder.append("\tmaxToCcBcc: Invalid value \"").append(maxToCcBccStr).append("\". Setting to fallback ").append(
                    maxToCcBcc).append('\n');
                
            }
        }
        
        {
            String javaMailPropertiesStr = configuration.getProperty("com.openexchange.mail.JavaMailProperties");
            if (null != javaMailPropertiesStr) {
                javaMailPropertiesStr = javaMailPropertiesStr.trim();
                if (javaMailPropertiesStr.indexOf("@oxgroupwaresysconfdir@") >= 0) {
                    final String configPath = configuration.getProperty("CONFIGPATH");
                    if (null == configPath) {
                        throw new MailConfigException("Missing property \"CONFIGPATH\" in system.properties");
                    }
                    javaMailPropertiesStr = javaMailPropertiesStr.replaceFirst("@oxgroupwaresysconfdir@", configPath);
                }
                javaMailProperties = readPropertiesFromFile(javaMailPropertiesStr);
                if (javaMailProperties.size() == 0) {
                    javaMailProperties = null;
                }
            }
            logBuilder.append("\tJavaMail Properties loaded: ").append(javaMailProperties != null).append('\n');
        }

        logBuilder.append("Global mail properties successfully loaded!");
        if (LOG.isInfoEnabled()) {
            LOG.info(logBuilder.toString());
        }
    }

    /**
     * Reads the properties from specified property file and returns an appropriate instance of {@link Properties}
     * 
     * @param propFile The property file
     * @return The appropriate instance of {@link Properties}
     * @throws MailConfigException If reading property file fails
     */
    protected static Properties readPropertiesFromFile(final String propFile) throws MailConfigException {
        final Properties properties = new Properties();
        final FileInputStream fis;
        try {
            fis = new FileInputStream(new File(propFile));
        } catch (final FileNotFoundException e) {
            throw new MailConfigException(
                new StringBuilder(256).append("Properties not found at location: ").append(propFile).toString(),
                e);
        }
        try {
            properties.load(fis);
            return properties;
        } catch (final IOException e) {
            throw new MailConfigException(
                new StringBuilder(256).append("I/O error while reading properties from file \"").append(propFile).append("\": ").append(
                    e.getMessage()).toString(),
                e);
        } finally {
            try {
                fis.close();
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public boolean isAllowNestedDefaultFolderOnAltNamespace() {
        return allowNestedDefaultFolderOnAltNamespace;
    }

    public int getAttachDisplaySize() {
        return attachDisplaySize;
    }

    /**
     * Gets the default MIME charset.
     * 
     * @return The default MIME charset
     */
    public String getDefaultMimeCharset() {
        return defaultMimeCharset;
    }

    /**
     * Gets the default mail provider.
     * 
     * @return The default mail provider
     */
    public String getDefaultMailProvider() {
        return defaultMailProvider;
    }

    /**
     * Indicates if admin mail login is enabled; meaning whether admin user's try to login to mail system is permitted or not.
     * 
     * @return <code>true</code> if admin mail login is enabled; otherwise <code>false</code>
     */
    public boolean isAdminMailLoginEnabled() {
        return adminMailLoginEnabled;
    }

    public char getDefaultSeparator() {
        return defaultSeparator;
    }

    public boolean isIgnoreSubscription() {
        return ignoreSubscription;
    }

    public boolean isIgnorePOP3StorageFolders() {
        return ignorePOP3StorageFolders;
    }

    public boolean isSupportSubscription() {
        return supportSubscription;
    }

    /**
     * Checks if client's IP address should be added to mail headers on delivery as custom header <code>"X-Originating-IP"</code>.
     * 
     * @return <code>true</code> if client's IP address should be added otherwise <code>false</code>
     */
    public boolean isAddClientIPAddress() {
        return addClientIPAddress;
    }

    /**
     * Gets the JavaMail properties.
     * 
     * @return The JavaMail properties
     */
    public Properties getJavaMailProperties() {
        return javaMailProperties;
    }

    /**
     * Gets the login source.
     * 
     * @return The login source
     */
    public LoginSource getLoginSource() {
        return loginSource;
    }

    /**
     * Gets the password source.
     * 
     * @return The password source
     */
    public PasswordSource getPasswordSource() {
        return passwordSource;
    }

    /**
     * Gets the mail server source.
     * 
     * @return The mail server source
     */
    public ServerSource getMailServerSource() {
        return mailServerSource;
    }

    /**
     * Gets the transport server source.
     * 
     * @return The transport server source
     */
    public ServerSource getTransportServerSource() {
        return transportServerSource;
    }

    public int getMailFetchLimit() {
        return mailFetchLimit;
    }

    /**
     * Gets the global mail server.
     * 
     * @return The global mail server
     */
    public String getMailServer() {
        return mailServer;
    }

    /**
     * Gets the master password.
     * 
     * @return The master password
     */
    public String getMasterPassword() {
        return masterPassword;
    }

    /**
     * Gets the sent mail rate limit (how many mails can be sent in 
     * 
     * @return
     */
    public int getMaxToCcBcc() {
        return maxToCcBcc;
    }

    /**
     * Gets the quote line colors.
     * 
     * @return The quote line colors
     */
    public String[] getQuoteLineColors() {
        return quoteLineColors;
    }

    /**
     * Gets the sent mail rate limit (how many mails can be sent in 
     * 
     * @return
     */
    public int getRateLimit() {
        return rateLimit;
    }

    /**
     * Gets the setting if the rate limit should only affect the primary
     * account or all accounts 
     * 
     * @return
     */
    public boolean getRateLimitPrimaryOnly() {
        return rateLimitPrimaryOnly;
    }
    
    /**
     * Gets the global transport server
     * 
     * @return The global transport server
     */
    public String getTransportServer() {
        return transportServer;
    }

    public boolean isUserFlagsEnabled() {
        return userFlagsEnabled;
    }

    public boolean isWatcherEnabled() {
        return watcherEnabled;
    }

    public int getWatcherFrequency() {
        return watcherFrequency;
    }

    public boolean isWatcherShallClose() {
        return watcherShallClose;
    }

    public int getWatcherTime() {
        return watcherTime;
    }

    /**
     * Gets the phishing headers.
     * 
     * @return The phishing headers or <code>null</code> if none defined
     */
    public String[] getPhishingHeaders() {
        if (null == phishingHeaders) {
            return null;
        }
        final String[] retval = new String[phishingHeaders.length];
        System.arraycopy(phishingHeaders, 0, retval, 0, phishingHeaders.length);
        return retval;
    }

    public int getMailAccessCacheShrinkerSeconds() {
        return mailAccessCacheShrinkerSeconds;
    }

    public int getMailAccessCacheIdleSeconds() {
        return mailAccessCacheIdleSeconds;
    }

}
