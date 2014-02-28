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

package com.openexchange.ajp13;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import com.openexchange.ajp13.exception.AJPv13Exception;
import com.openexchange.ajp13.util.IPTools;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Reloadable;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.configuration.SystemConfig;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.server.Initialization;
import com.openexchange.server.ServiceExceptionCode;

/**
 * {@link AJPv13Config} - The AJPv13 configuration
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 */
public final class AJPv13Config implements Initialization, Reloadable {

    // Final static fields
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AJPv13Config.class);

    private static final AJPv13Config instance = new AJPv13Config();

    public static AJPv13Config getInstance() {
        return instance;
    }

    private static final String CONFIGFILE = "server.properties";
    private static final String[] PROPERTIES = new String[] {"com.openexchange.server.knownProxies"};

    // fields
    private final AtomicBoolean started = new AtomicBoolean();

    private int serverThreadSize = 20;

    private int listenerPoolSize = 20;

    private int listenerReadTimeout = 60000;

    private int keepAliveTime = 20000;

    private int maxRequestParameterCount = 30;

    private boolean watcherEnabled;

    private boolean watcherPermission;

    private int watcherMaxRunningTime = 300000;

    private int watcherFrequency = 300000;

    private int servletPoolSize = 50;

    private int port = 8009;

    private String jvmRoute;

    private String servletConfigs;

    private InetAddress ajpBindAddr;

    private boolean logForwardRequest;

    private List<String> knownProxies = Collections.emptyList();

    private String forHeader = "X-Forwarded-For";

    private String protocolHeader = "X-Forwarded-Proto";

    private boolean isConsiderXForwards;

    private String contentSecurityPolicy;

    @Override
    public void start() throws OXException {
        if (!started.compareAndSet(false, true)) {
            LOG.error("{} already started", this.getClass().getName());
            return;
        }
        init();
    }

    @Override
    public void stop() {
        if (!started.compareAndSet(true, false)) {
            LOG.error("{} cannot be stopped since it has no been started before", this.getClass().getName());
            return;
        }
        reset();
    }

    private void reset() {
        serverThreadSize = 20;
        listenerPoolSize = 20;
        listenerReadTimeout = 60000;
        keepAliveTime = 20000;
        maxRequestParameterCount = 30;
        watcherEnabled = false;
        watcherPermission = false;
        watcherMaxRunningTime = 300000;
        watcherFrequency = 300000;
        servletPoolSize = 50;
        port = 8009;
        jvmRoute = null;
        servletConfigs = null;
        ajpBindAddr = null;
        logForwardRequest = false;
        knownProxies = Collections.emptyList();
        forHeader = "X-Forwarded-For";
        protocolHeader = "X-Forwarded-Proto";
        isConsiderXForwards = false;
        contentSecurityPolicy = null;
    }

    private void init() throws OXException {
        ConfigurationService configService = Services.getService(ConfigurationService.class);
        if (configService == null) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(ConfigurationService.class.getSimpleName());
        }
        try {
            // ajp.properties
            this.serverThreadSize = configService.getIntProperty("AJP_SERVER_THREAD_SIZE", 1);

            this.listenerPoolSize = configService.getIntProperty("AJP_LISTENER_POOL_SIZE", 1);

            this.listenerReadTimeout = configService.getIntProperty("AJP_LISTENER_READ_TIMEOUT", 60000);

            this.keepAliveTime = configService.getIntProperty("AJP_KEEP_ALIVE_TIME", 20000);

            this.servletPoolSize = configService.getIntProperty("SERVLET_POOL_SIZE", 1);

            this.servletConfigs = configService.getProperty("AJP_SERVLET_CONFIG_DIR");
            if (servletConfigs == null || "null".equalsIgnoreCase(servletConfigs)) {
                servletConfigs = "servletConfig";
            }
            final File servletConfigsFile = configService.getDirectory(servletConfigs);
            final boolean nonExisting = (null == servletConfigsFile) || !servletConfigsFile.exists() || !servletConfigsFile.isDirectory();
            if (nonExisting) {
                LOG.trace("{} does not exist or is not a directory", servletConfigsFile);
            }

            this.logForwardRequest = configService.getBoolProperty("AJP_LOG_FORWARD_REQUEST", false);

            // server.properties
            String bindAddr = configService.getProperty("com.openexchange.connector.networkListenerHost", "127.0.0.1");
            this.ajpBindAddr = bindAddr.equals("*") ? null : InetAddress.getByName(bindAddr);

            this.port = configService.getIntProperty("com.openexchange.connector.networkListenerPort", 8009);

            this.maxRequestParameterCount = configService.getIntProperty("com.openexchange.connector.maxRequestParameters", 30);

            this.jvmRoute = configService.getProperty("com.openexchange.server.backendRoute", "OX0");

            // requestwatcher.properties
            this.watcherEnabled = configService.getBoolProperty("com.openexchange.requestwatcher.isEnabled", true);

            this.watcherPermission = configService.getBoolProperty("com.openexchange.requestwatcher.restartPermission", false);

            this.watcherMaxRunningTime = configService.getIntProperty("com.openexchange.requestwatcher.maxRequestAge", 60000);

            this.watcherFrequency = configService.getIntProperty("com.openexchange.requestwatcher.frequency", 30000);

            {
                String sProxyCandidates = configService.getProperty("com.openexchange.server.knownProxies", "");
                if (Strings.isEmpty(sProxyCandidates)) {
                    knownProxies = Collections.emptyList();
                } else {
                    List<String> proxyCandidates = IPTools.splitAndTrim(sProxyCandidates, IPTools.COMMA_SEPARATOR);
                    List<String> erroneousIPs = IPTools.filterErroneousIPs(proxyCandidates);
                    if (!erroneousIPs.isEmpty()) {
                        LOG.warn("Falling back to empty list as com.openexchange.server.knownProxies contains malformed IPs: {}", erroneousIPs);
                    } else {
                        this.knownProxies = proxyCandidates;
                    }
                }
            }

            this.forHeader = configService.getProperty("com.openexchange.server.forHeader", "X-Forwarded-For");

            this.protocolHeader = configService.getProperty("com.openexchange.server.protocolHeader", "X-Forwarded-Proto");

            this.isConsiderXForwards = configService.getBoolProperty("com.openexchange.server.considerXForwards", false);

            {
                String csp = configService.getProperty("com.openexchange.servlet.contentSecurityPolicy", "").trim();
                csp = Strings.unquote(csp);
                this.contentSecurityPolicy = csp.trim();
            }

            logInfo(nonExisting ? " (non-existing)" : " (exists)");

        } catch (IOException ioEx) {
            throw new AJPv13Exception(AJPv13Exception.AJPCode.IO_ERROR, true, ioEx, ioEx.getMessage());
        }
    }

    private static void logInfo(final String desc) {
        LOG.info("{}", new Object() { @Override public String toString() {
            final StringBuilder logBuilder = new StringBuilder(1024);
            logBuilder.append("\nAJP CONFIGURATION:\n");
            logBuilder.append("\tAJP_PORT=").append(instance.port).append('\n');
            logBuilder.append("\tAJP_SERVER_THREAD_SIZE=").append(instance.serverThreadSize).append('\n');
            logBuilder.append("\tAJP_LISTENER_POOL_SIZE=").append(instance.listenerPoolSize).append('\n');
            logBuilder.append("\tAJP_LISTENER_READ_TIMEOUT=").append(instance.listenerReadTimeout).append('\n');
            logBuilder.append("\tAJP_KEEP_ALIVE_TIME=").append(instance.keepAliveTime).append('\n');
            logBuilder.append("\tAJP_MAX_REQUEST_PARAMETER_COUNT=").append(instance.maxRequestParameterCount).append('\n');
            logBuilder.append("\tAJP_WATCHER_ENABLED=").append(instance.watcherEnabled).append('\n');
            logBuilder.append("\tAJP_WATCHER_PERMISSION=").append(instance.watcherPermission).append('\n');
            logBuilder.append("\tAJP_WATCHER_MAX_RUNNING_TIME=").append(instance.watcherMaxRunningTime).append('\n');
            logBuilder.append("\tAJP_WATCHER_FREQUENCY=").append(instance.watcherFrequency).append('\n');
            logBuilder.append("\tSERVLET_POOL_SIZE=").append(instance.servletPoolSize).append('\n');
            logBuilder.append("\tAJP_JVM_ROUTE=").append(instance.jvmRoute).append('\n');
            logBuilder.append("\tAJP_LOG_FORWARD_REQUEST=").append(instance.logForwardRequest).append('\n');
            logBuilder.append("\tAJP_SERVLET_CONFIG_DIR=").append(instance.servletConfigs).append(null == desc ? "" : desc).append('\n');
            logBuilder.append("\tAJP_BIND_ADDR=").append(
                instance.ajpBindAddr == null ? "* (all interfaces)" : instance.ajpBindAddr.toString());
            return logBuilder.toString();
        }});
    }

    private AJPv13Config() {
        super();
    }

    /**
     * Gets if we should consider X-Forward-Headers that reach the backend.
     * Those can be spoofed by clients so we have to make sure to consider the headers only if the proxy/proxies reliably override those
     * headers for incoming requests.
     * Disabled by default as we now use relative redirects for Grizzly.
     * @return
     */
    public static boolean isConsiderXForwards() {
        return instance.isConsiderXForwards;
    }

    /**
     * Gets the knownProxies
     *
     * @return The knownProxies
     */
    public static List<String> getKnownProxies() {
        return instance.knownProxies;
    }

    /**
     * Gets the forHeader
     *
     * @return The forHeader
     */
    public static String getForHeader() {
        return instance.forHeader;
    }

    /**
     * Gets the protocolHeader
     *
     * @return The protocolHeader
     */
    public static String getProtocolHeader() {
        return instance.protocolHeader;
    }

    public static int getAJPPort() {
        return instance.port;
    }

    public static int getAJPServerThreadSize() {
        return instance.serverThreadSize;
    }

    /**
     * Gets the capacity for listener pool
     *
     * @return The capacity for listener pool
     */
    public static int getAJPListenerPoolSize() {
        return instance.listenerPoolSize;
    }

    public static int getAJPListenerReadTimeout() {
        return instance.listenerReadTimeout;
    }

    /**
     * Gets the keep-alive time
     *
     * @return The keep-alive time
     */
    public static int getKeepAliveTime() {
        return instance.keepAliveTime;
    }

    /**
     * Gets the max. request parameter count allowed.
     *
     * @return The max. request parameter count
     */
    public static int getMaxRequestParameterCount() {
        return instance.maxRequestParameterCount;
    }

    public static boolean getAJPWatcherEnabled() {
        return instance.watcherEnabled;
    }

    public static boolean getAJPWatcherPermission() {
        return instance.watcherPermission;
    }

    public static int getAJPWatcherMaxRunningTime() {
        return instance.watcherMaxRunningTime;
    }

    public static int getAJPWatcherFrequency() {
        return instance.watcherFrequency;
    }

    public static int getServletPoolSize() {
        return instance.servletPoolSize;
    }

    public static String getJvmRoute() {
        return instance.jvmRoute;
    }

    public static boolean isLogForwardRequest() {
        return instance.logForwardRequest;
    }

    public static String getServletConfigs() {
        return instance.servletConfigs;
    }

    /**
     * @return an instance if <code>java.net.InetAddress</code> if property AJP_BIND_ADDR is different to "*"; <code>null</code> otherwise
     */
    public static InetAddress getAJPBindAddress() {
        return instance.ajpBindAddr;
    }

    /**
     * Gets the <code>Content-Security-Policy</code> header.
     * <p>
     * Please refer to <a href="http://www.html5rocks.com/en/tutorials/security/content-security-policy/">An Introduction to Content Security Policy</a>
     *
     * @return The header value or null/empty string
     */
    public static String getContentSecurityPolicy() {
        return instance.contentSecurityPolicy;
    }

    /**
     * Gets the specified server property.
     *
     * @param property The server property
     * @return The property value
     */
    public static String getServerProperty(final ServerConfig.Property property) {
        final ConfigurationService service = Services.getService(ConfigurationService.class);
        return service == null ? property.getDefaultValue() : service.getProperty(property.getPropertyName(), property.getDefaultValue());
    }

    /**
     * Gets the specified system property.
     *
     * @param property The system property
     * @return The property value
     */
    public static String getSystemProperty(final SystemConfig.Property property) {
        final ConfigurationService service = Services.getService(ConfigurationService.class);
        return service == null ? null : service.getProperty(property.getPropertyName());
    }

    @Override
    public void reloadConfiguration(ConfigurationService configService) {
        String sProxyCandidates = configService.getProperty("com.openexchange.server.knownProxies", "");
        if (Strings.isEmpty(sProxyCandidates)) {
            knownProxies = Collections.emptyList();
        } else {
            List<String> proxyCandidates = IPTools.splitAndTrim(sProxyCandidates, IPTools.COMMA_SEPARATOR);
            List<String> erroneousIPs = IPTools.filterErroneousIPs(proxyCandidates);
            if (!erroneousIPs.isEmpty()) {
                LOG.warn("Falling back to empty list as com.openexchange.server.knownProxies contains malformed IPs: {}", erroneousIPs);
            } else {
                this.knownProxies = proxyCandidates;
            }
        }
    }

    @Override
    public Map<String, String[]> getConfigFileNames() {
        Map<String, String[]> map = new HashMap<String, String[]>(1);
        map.put(CONFIGFILE, PROPERTIES);
        return map;
    }

}
