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

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.grizzly.http.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.ConnectionProbe;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.PortRange;
import org.glassfish.grizzly.Processor;
import org.glassfish.grizzly.TransportProbe;
import org.glassfish.grizzly.attributes.AttributeBuilder;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.ContentEncoding;
import org.glassfish.grizzly.http.GZipContentEncoding;
import org.glassfish.grizzly.http.LZMAContentEncoding;
import org.glassfish.grizzly.http.server.filecache.FileCache;
import org.glassfish.grizzly.http.server.jmx.JmxEventListener;
import org.glassfish.grizzly.memory.MemoryProbe;
import org.glassfish.grizzly.monitoring.MonitoringConfig;
import org.glassfish.grizzly.monitoring.jmx.GrizzlyJmxManager;
import org.glassfish.grizzly.monitoring.jmx.JmxObject;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.rcm.ResourceAllocationFilter;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.ssl.SSLFilter;
import org.glassfish.grizzly.threadpool.DefaultWorkerThread;
import org.glassfish.grizzly.threadpool.ThreadPoolProbe;
import org.glassfish.grizzly.utils.Charsets;
import org.glassfish.grizzly.utils.DelayedExecutor;
import org.glassfish.grizzly.utils.IdleTimeoutFilter;
import com.openexchange.http.grizzly.GrizzlyConfig;


/**
 *
 */
public class OXHttpServer extends HttpServer {

    private static final Logger LOGGER = Grizzly.logger(OXHttpServer.class);

    /**
     * Configuration details for this server instance.
     */
    private final ServerConfiguration serverConfig = new ServerConfiguration(this);


    /**
     * Flag indicating whether or not this server instance has been started.
     */
    private boolean started;

    /**
     * HttpHandler, which processes HTTP requests
     */
    private final HttpHandlerChain httpHandlerChain = new HttpHandlerChain(this);

    /**
     * Mapping of {@link NetworkListener}s, by name, used by this server
     *  instance.
     */
    private final Map<String, NetworkListener> listeners =
            new HashMap<String, NetworkListener>(2);

    private volatile ExecutorService auxExecutorService;

    private volatile DelayedExecutor delayedExecutor;

    protected volatile GrizzlyJmxManager jmxManager;

    protected volatile JmxObject managementObject;

//    private volatile JmxObject serviceManagementObject;


    // ---------------------------------------------------------- Public Methods


    /**
     * @return the {@link ServerConfiguration} used to configure this
     *  {@link HttpServer} instance
     */
    @Override
    public final ServerConfiguration getServerConfiguration() {
        return serverConfig;
    }


    /**
     * <p>
     * Adds the specified <code>listener</code> to the server instance.
     * </p>
     *
     * <p>
     * If the server is already running when this method is called, the listener
     * will be started.
     * </p>
     *
     * @param listener the {@link NetworkListener} to associate with this
     *  server instance.
     */
    @Override
    public synchronized void addListener(final NetworkListener listener) {

        if (!started) {
            listeners.put(listener.getName(), listener);
        } else {
            configureListener(listener);
            if (!listener.isStarted()) {
                try {
                    listener.start();
                } catch (IOException ioe) {
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.log(Level.SEVERE,
                                "Failed to start listener [{0}] : {1}",
                                new Object[] { listener.toString(), ioe.toString() });
                        LOGGER.log(Level.SEVERE, ioe.toString(), ioe);
                    }
                }
            }
        }

    }


    /**
     * @param name the {@link NetworkListener} name.
     * @return the {@link NetworkListener}, if any, associated with the
     *  specified <code>name</code>.
     */
    @Override
    public NetworkListener getListener(final String name) {

        return listeners.get(name);

    }


    /**
     * @return a <code>read only</code> {@link Collection} over the listeners
     *  associated with this <code>HttpServer</code> instance.
     */
    @Override
    public Collection<NetworkListener> getListeners() {
        return Collections.unmodifiableMap(listeners).values();
    }


    /**
     * <p>
     * Removes the {@link NetworkListener} associated with the specified
     * <code>name</code>.
     * </p>
     *
     * <p>
     * If the server is running when this method is invoked, the listener will
     * be stopped before being returned.
     * </p>
     *
     * @param name the name of the {@link NetworkListener} to remove.
     */
    @Override
    public NetworkListener removeListener(final String name) {

        final NetworkListener listener = listeners.remove(name);
        if (listener != null) {
            if (listener.isStarted()) {
                try {
                    listener.stop();
                } catch (IOException ioe) {
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.log(Level.SEVERE,
                                   "Failed to stop listener [{0}] : {1}",
                                    new Object[] { listener.toString(), ioe.toString() });
                        LOGGER.log(Level.SEVERE, ioe.toString(), ioe);
                    }
                }
            }
        }
        return listener;

    }

    @Override
    DelayedExecutor getDelayedExecutor() {
        return delayedExecutor;
    }

    /**
     * <p>
     * Starts the listeners of the <code>HttpServer</code>.
     * </p>
     *
     * @throws IOException if an error occurs while attempting to start the server.
     * @throws IllegalStateException If HTTP server was not started, yet
     * @see #start()
     */
    public synchronized void startListeners() throws IOException {

        if (!started) {
            throw new IllegalStateException("Http server not started, yet.");
        }

        for (final NetworkListener listener : listeners.values()) {
            try {
                listener.start();
            } catch (IOException ioe) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "Failed to start listener [{0}] : {1}", new Object[] { listener.toString(), ioe.toString() });
                    LOGGER.log(Level.FINEST, ioe.toString(), ioe);
                }

                throw ioe;
            }
        }
    }

    /**
     * <p>
     * Starts the <code>HttpServer</code>.
     * </p>
     *
     * @throws IOException if an error occurs while attempting to start the
     *  server.
     */
    @Override
    public synchronized void start() throws IOException{

        if (started) {
            return;
        }
        started = true;

        configureAuxThreadPool();

        delayedExecutor = new DelayedExecutor(auxExecutorService);
        delayedExecutor.start();

        for (final NetworkListener listener : listeners.values()) {
            configureListener(listener);
        }

        if (serverConfig.isJmxEnabled()) {
            enableJMX();
        }

        /*-
         *
        for (final NetworkListener listener : listeners.values()) {
            try {
                listener.start();
            } catch (IOException ioe) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST,
                            "Failed to start listener [{0}] : {1}",
                            new Object[]{listener.toString(), ioe.toString()});
                    LOGGER.log(Level.FINEST, ioe.toString(), ioe);
                }

                throw ioe;
            }
        }
        */

        setupHttpHandler();

        if (serverConfig.isJmxEnabled()) {
            for (final JmxEventListener l : serverConfig.getJmxEventListeners()) {
                l.jmxEnabled();
            }
        }

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.log(Level.INFO, "[{0}] Started.", getServerConfiguration().getName());
        }

    }

    private void setupHttpHandler() {

        serverConfig.addJmxEventListener(httpHandlerChain);

        synchronized (serverConfig.handlersSync) {
            for (final HttpHandler httpHandler : serverConfig.orderedHandlers) {
                final String[] mappings = serverConfig.handlers.get(httpHandler);
                httpHandlerChain.addHandler(httpHandler, mappings);
            }
        }
        httpHandlerChain.start();

    }


    private void tearDownHttpHandler() {

        httpHandlerChain.destroy();

    }


    /**
     * @return the {@link HttpHandler} used by this <code>HttpServer</code>
     *  instance.
     */
    @Override
    public HttpHandler getHttpHandler() {
        return httpHandlerChain;
    }


    /**
     * @return <code>true</code> if this <code>HttpServer</code> has
     *  been started.
     */
    @Override
    public synchronized boolean isStarted() {
        return started;
    }


    @Override
    public JmxObject getManagementObject(boolean clear) {
        if (!clear && managementObject == null) {
            synchronized (serverConfig) {
                if (managementObject == null) {
                    managementObject = new org.glassfish.grizzly.http.server.jmx.HttpServer(this);
                }
            }
        }
        try {
            return managementObject;
        } finally {
            if (clear) {
                managementObject = null;
            }
        }
    }


    /**
     * <p>
     * Stops the <code>HttpServer</code> instance.
     * </p>
     */
    @Override
    public synchronized void stop() {

        if (!started) {
            return;
        }
        started = false;



        try {

            if (serverConfig.isJmxEnabled()) {
                for (final JmxEventListener l : serverConfig.getJmxEventListeners()) {
                    l.jmxDisabled();
                }
            }

            tearDownHttpHandler();

            final String[] names = listeners.keySet().toArray(new String[listeners.size()]);
            for (final String name : names) {
                removeListener(name);
            }

            delayedExecutor.stop();
            delayedExecutor = null;

            stopAuxThreadPool();

            if (serverConfig.isJmxEnabled()) {
                disableJMX();
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, null, e);
        } finally {
            for (final NetworkListener listener : listeners.values()) {
                final Processor p = listener.getTransport().getProcessor();
                if (p instanceof FilterChain) {
                    ((FilterChain) p).clear();
                }
            }
        }

    }


    /**
     * @return a <code>HttpServer</code> configured to listen to requests
     * on {@link NetworkListener#DEFAULT_NETWORK_HOST}:{@link NetworkListener#DEFAULT_NETWORK_PORT},
     * using the directory in which the server was launched the server's document root.
     */
    public static HttpServer createSimpleServer() {

        return createSimpleServer(".");

    }


    /**
     * @param path the document root.
     *
     * @return a <code>HttpServer</code> configured to listen to requests
     * on {@link NetworkListener#DEFAULT_NETWORK_HOST}:{@link NetworkListener#DEFAULT_NETWORK_PORT},
     * using the specified <code>path</code> as the server's document root.
     */
    public static HttpServer createSimpleServer(final String path) {

        return createSimpleServer(path, NetworkListener.DEFAULT_NETWORK_PORT);

    }


    /**
     * @param path the document root.
     * @param port the network port to which this listener will bind.
     *
     * @return a <code>HttpServer</code> configured to listen to requests
     * on {@link NetworkListener#DEFAULT_NETWORK_HOST}:<code>port</code>,
     * using the specified <code>path</code> as the server's document root.
     */
    public static HttpServer createSimpleServer(final String path,
                                                final int port) {

        return createSimpleServer(path, NetworkListener.DEFAULT_NETWORK_HOST, port);

    }


    /**
     * @param path the document root.
     * @param range port range to attempt to bind to.
     *
     * @return a <code>HttpServer</code> configured to listen to requests
     * on {@link NetworkListener#DEFAULT_NETWORK_HOST}:<code>[port-range]</code>,
     * using the specified <code>path</code> as the server's document root.
     */
    public static HttpServer createSimpleServer(final String path,
                                                final PortRange range) {

        return createSimpleServer(path,
                NetworkListener.DEFAULT_NETWORK_HOST,
                range);

    }

    /**
     * @param path the document root.
     * @param socketAddress the endpoint address to which this listener will bind.
     *
     * @return a <code>HttpServer</code> configured to listen to requests
     * on <code>socketAddress</code>,
     * using the specified <code>path</code> as the server's document root.
     */
    public static HttpServer createSimpleServer(final String path,
                                                final SocketAddress socketAddress) {

        final InetSocketAddress inetAddr = (InetSocketAddress) socketAddress;
        return createSimpleServer(path, inetAddr.getHostName(), inetAddr.getPort());
    }

    /**
     * @param path the document root.
     * @param host the network port to which this listener will bind.
     * @param port the network port to which this listener will bind.
     *
     * @return a <code>HttpServer</code> configured to listen to requests
     * on <code>host</code>:<code>port</code>,
     * using the specified <code>path</code> as the server's document root.
     */
    public static HttpServer createSimpleServer(final String path,
                                                final String host,
                                                final int port) {

        return createSimpleServer(path, host, new PortRange(port));

    }

    /**
     * @param path the document root.
     * @param host the network port to which this listener will bind.
     * @param range port range to attempt to bind to.
     *
     * @return a <code>HttpServer</code> configured to listen to requests
     * on <code>host</code>:<code>[port-range]</code>,
     * using the specified <code>path</code> as the server's document root.
     */
    public static HttpServer createSimpleServer(final String path,
                                                final String host,
                                                final PortRange range) {

        final HttpServer server = new HttpServer();
        final ServerConfiguration config = server.getServerConfiguration();
        if (path != null) {
            config.addHttpHandler(new StaticHttpHandler(path), "/");
        }
        final NetworkListener listener =
                new NetworkListener("grizzly",
                                    host,
                                    range);
        server.addListener(listener);
        return server;

    }

    // ------------------------------------------------------- Protected Methods


    @Override
    protected void enableJMX() {

        if (jmxManager == null) {
            synchronized (serverConfig) {
                if (jmxManager == null) {
                    jmxManager= GrizzlyJmxManager.instance();
                }
            }
        }
        jmxManager.registerAtRoot(getManagementObject(false),
                                  serverConfig.getName());

    }


    @Override
    protected void disableJMX() {

        if (jmxManager != null) {
            jmxManager.deregister(getManagementObject(true));
        }

    }


    // --------------------------------------------------------- Private Methods


    private void configureListener(final NetworkListener listener) {
        FilterChain chain = listener.getFilterChain();
        if (chain == null) {
            final FilterChainBuilder builder = FilterChainBuilder.stateless();
            builder.add(new TransportFilter());
            builder.add(new IdleTimeoutFilter(delayedExecutor,
                    listener.getKeepAlive().getIdleTimeoutInSeconds(),
                    TimeUnit.SECONDS));
            if (listener.isSecure()) {
                SSLEngineConfigurator sslConfig = listener.getSslEngineConfig();
                if (sslConfig == null) {
                    sslConfig = new SSLEngineConfigurator(
                            SSLContextConfigurator.DEFAULT_CONFIG,
                            false,
                            false,
                            false);
                    listener.setSSLEngineConfig(sslConfig);
                }
                final SSLFilter filter = new SSLFilter(sslConfig, null);
                builder.add(filter);

            }
            final int maxHeaderSize = listener.getMaxHttpHeaderSize() == -1
                                        ? org.glassfish.grizzly.http.HttpServerFilter.DEFAULT_MAX_HTTP_PACKET_HEADER_SIZE
                                        : listener.getMaxHttpHeaderSize();

            // Passing null value for the delayed executor, because IdleTimeoutFilter should
            // handle idle connections for us
            final org.glassfish.grizzly.http.HttpServerFilter httpServerFilter =
                    new org.glassfish.grizzly.http.HttpServerFilter(listener.isChunkingEnabled(),
                                         maxHeaderSize,
                                         null,
                                         listener.getKeepAlive(),
                                         null,
                                         listener.getMaxRequestHeaders(),
                                         listener.getMaxResponseHeaders());
            final Set<ContentEncoding> contentEncodings =
                    configureCompressionEncodings(listener);
            for (ContentEncoding contentEncoding : contentEncodings) {
                httpServerFilter.addContentEncoding(contentEncoding);
            }
            if (listener.isRcmSupportEnabled()) {
                builder.add(new ResourceAllocationFilter());
            }

            httpServerFilter.setAllowPayloadForUndefinedHttpMethods(
                serverConfig.isAllowPayloadForUndefinedHttpMethods());

            httpServerFilter.getMonitoringConfig().addProbes(
                    serverConfig.getMonitoringConfig().getHttpConfig().getProbes());
            builder.add(httpServerFilter);

            final FileCache fileCache = listener.getFileCache();
            fileCache.initialize(listener.getTransport().getMemoryManager(), delayedExecutor);
            final FileCacheFilter fileCacheFilter = new FileCacheFilter(fileCache);
            fileCache.getMonitoringConfig().addProbes(
                    serverConfig.getMonitoringConfig().getFileCacheConfig().getProbes());
            builder.add(fileCacheFilter);

            final ServerFilterConfiguration config = new ServerFilterConfiguration(serverConfig);

            final HttpServerFilter webServerFilter = new OXHttpServerFilter(
                    config,
                    delayedExecutor);

            if (listener.isSendFileExplicitlyConfigured()) {
                config.setSendFileEnabled(listener.isSendFileEnabled());
            }

            if (listener.getScheme() != null) {
                config.setScheme(listener.getScheme());
            }

            config.setTraceEnabled(config.isTraceEnabled() || listener.isTraceEnabled());
            config.setMaxFormPostSize(listener.getMaxFormPostSize());
            config.setMaxBufferedPostSize(listener.getMaxBufferedPostSize());
            config.setDefaultQueryEncoding(Charsets.lookupCharset(GrizzlyConfig.getInstance().getDefaultEncoding()));


            webServerFilter.setHttpHandler(httpHandlerChain);

            webServerFilter.getMonitoringConfig().addProbes(
                    serverConfig.getMonitoringConfig().getWebServerConfig().getProbes());

            builder.add(webServerFilter);

            final AddOn[] addons = listener.getAddOnSet().getArray();
            if (addons != null) {
                for (AddOn addon : addons) {
                    addon.setup(listener, builder);
                }
            }

            chain = builder.build();
            listener.setFilterChain(chain);
        }
        configureMonitoring(listener);
    }

    @Override
    protected Set<ContentEncoding> configureCompressionEncodings(final NetworkListener listener) {
            final String mode = listener.getCompression();
            int compressionMinSize = listener.getCompressionMinSize();
            CompressionLevel compressionLevel;
            try {
                compressionLevel = CompressionLevel.getCompressionLevel(mode);
            } catch (IllegalArgumentException e) {
                try {
                    // Try to parse compression as an int, which would give the
                    // minimum compression size
                    compressionLevel = CompressionLevel.ON;
                    compressionMinSize = Integer.parseInt(mode);
                } catch (Exception ignore) {
                    compressionLevel = CompressionLevel.OFF;
                }
            }
            final String compressableMimeTypesString = listener.getCompressableMimeTypes();
            final String noCompressionUserAgentsString = listener.getNoCompressionUserAgents();
            final String[] compressableMimeTypes =
                    ((compressableMimeTypesString != null)
                            ? compressableMimeTypesString.split(",")
                            : new String[0]);
            final String[] noCompressionUserAgents =
                    ((noCompressionUserAgentsString != null)
                            ? noCompressionUserAgentsString.split(",")
                            : new String[0]);
            final ContentEncoding gzipContentEncoding = new GZipContentEncoding(
                GZipContentEncoding.DEFAULT_IN_BUFFER_SIZE,
                GZipContentEncoding.DEFAULT_OUT_BUFFER_SIZE,
                new CompressionEncodingFilter(compressionLevel, compressionMinSize,
                    compressableMimeTypes,
                    noCompressionUserAgents,
                    GZipContentEncoding.getGzipAliases()));
            final ContentEncoding lzmaEncoding = new LZMAContentEncoding(new CompressionEncodingFilter(compressionLevel, compressionMinSize,
                    compressableMimeTypes,
                    noCompressionUserAgents,
                    LZMAContentEncoding.getLzmaAliases()));
            final Set<ContentEncoding> set = new HashSet<ContentEncoding>(2);
            set.add(gzipContentEncoding);
            set.add(lzmaEncoding);
            return set;
        }

    @SuppressWarnings("unchecked")
    private void configureMonitoring(final NetworkListener listener) {
        final TCPNIOTransport transport = listener.getTransport();

        final MonitoringConfig<TransportProbe> transportMonitoringCfg =
                transport.getMonitoringConfig();
        final MonitoringConfig<ConnectionProbe> connectionMonitoringCfg =
                transport.getConnectionMonitoringConfig();
        final MonitoringConfig<MemoryProbe> memoryMonitoringCfg =
                transport.getMemoryManager().getMonitoringConfig();
        final MonitoringConfig<ThreadPoolProbe> threadPoolMonitoringCfg =
                transport.getThreadPoolMonitoringConfig();

        transportMonitoringCfg.clearProbes();
        connectionMonitoringCfg.clearProbes();
        memoryMonitoringCfg.clearProbes();
        threadPoolMonitoringCfg.clearProbes();

        transportMonitoringCfg.addProbes(serverConfig.getMonitoringConfig()
                .getTransportConfig().getProbes());
        connectionMonitoringCfg.addProbes(serverConfig.getMonitoringConfig()
                .getConnectionConfig().getProbes());
        memoryMonitoringCfg.addProbes(serverConfig.getMonitoringConfig()
                .getMemoryConfig().getProbes());
        threadPoolMonitoringCfg.addProbes(serverConfig.getMonitoringConfig()
                .getThreadPoolConfig().getProbes());

    }

    private void configureAuxThreadPool() {
        final AtomicInteger threadCounter = new AtomicInteger();

        auxExecutorService = Executors.newCachedThreadPool(
                new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                final Thread newThread = new DefaultWorkerThread(
                        AttributeBuilder.DEFAULT_ATTRIBUTE_BUILDER,
                        serverConfig.getName() + "-" + threadCounter.getAndIncrement(),
                        null,
                        r);
                newThread.setDaemon(true);
                return newThread;
            }
        });
    }


    private void stopAuxThreadPool() {
        final ExecutorService localThreadPool = auxExecutorService;
        auxExecutorService = null;

        if (localThreadPool != null) {
            localThreadPool.shutdownNow();
        }
    }

    //************ Runtime config change listeners ******************

    /**
     * Modifies handlers mapping during runtime.
     */
    @Override
    synchronized void onAddHttpHandler(HttpHandler httpHandler, String[] mapping) {
        if (isStarted()) {
            httpHandlerChain.addHandler(httpHandler, mapping);
        }
    }

    /**
     * Modifies handlers mapping during runtime.
     */
    @Override
    synchronized void onRemoveHttpHandler(HttpHandler httpHandler) {
        if (isStarted()) {
            httpHandlerChain.removeHttpHandler(httpHandler);
        }
    }
}
