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
 *    trademarks of the OX Software GmbH. group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.ConnectionProbe;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.PortRange;
import org.glassfish.grizzly.Processor;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.TransportProbe;
import org.glassfish.grizzly.attributes.AttributeBuilder;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.CompressionConfig;
import org.glassfish.grizzly.http.CompressionConfig.CompressionMode;
import org.glassfish.grizzly.http.ContentEncoding;
import org.glassfish.grizzly.http.GZipContentEncoding;
import org.glassfish.grizzly.http.LZMAContentEncoding;
import org.glassfish.grizzly.http.server.filecache.FileCache;
import org.glassfish.grizzly.http.server.jmxbase.JmxEventListener;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.memory.MemoryProbe;
import org.glassfish.grizzly.monitoring.MonitoringConfig;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.ssl.SSLBaseFilter;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.threadpool.DefaultWorkerThread;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.grizzly.threadpool.ThreadPoolProbe;
import org.glassfish.grizzly.utils.DelayedExecutor;
import org.glassfish.grizzly.utils.Futures;
import org.glassfish.grizzly.utils.IdleTimeoutFilter;
import com.openexchange.http.grizzly.GrizzlyConfig;

/**
 * {@link OXHttpServer}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.4
 */
public class OXHttpServer extends HttpServer {

    private static final Logger LOGGER = Grizzly.logger(HttpServer.class);

    /**
     * Flag indicating whether or not this server instance has been started.
     */
    private State state = State.STOPPED;

    /**
     * Future to control graceful shutdown status
     */
    private FutureImpl<HttpServer> shutdownFuture;

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

    @SuppressWarnings("hiding")
    volatile DelayedExecutor delayedExecutor;

    private final GrizzlyConfig grizzlyConfig;

    // ---------------------------------------------------------- Public Methods

    /**
     * Initializes a new {@link OXHttpServer}.
     *
     * @param grizzlyConfig The Grizzly configuration
     */
    public OXHttpServer(GrizzlyConfig grizzlyConfig) {
        super();
        this.grizzlyConfig = grizzlyConfig;
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

        if (state == State.RUNNING) {
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

        listeners.put(listener.getName(), listener);
    }


    /**
     * @param name the {@link NetworkListener} name.
     * @return the {@link NetworkListener}, if any, associated with the
     *  specified <code>name</code>.
     */
    @Override
    public synchronized NetworkListener getListener(final String name) {

        return listeners.get(name);

    }


    /**
     * @return a <code>read only</code> {@link Collection} over the listeners
     *  associated with this <code>HttpServer</code> instance.
     */
    @Override
    public synchronized Collection<NetworkListener> getListeners() {
        return Collections.unmodifiableCollection(listeners.values());
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
     * @return {@link NetworkListener}, that has been removed, or <tt>null</tt>
     *      if the listener with the given name doesn't exist
     */
    @Override
    public synchronized NetworkListener removeListener(final String name) {

        final NetworkListener listener = listeners.remove(name);
        if (listener != null) {
            if (listener.isStarted()) {
                try {
                    listener.shutdownNow();
                } catch (IOException ioe) {
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.log(Level.SEVERE,
                                   "Failed to shutdown listener [{0}] : {1}",
                                    new Object[] { listener.toString(), ioe.toString() });
                        LOGGER.log(Level.SEVERE, ioe.toString(), ioe);
                    }
                }
            }
        }
        return listener;

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

        if (state != State.RUNNING) {
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

        if (state == State.RUNNING) {
            return;
        } else if (state == State.STOPPING) {
            throw new IllegalStateException("The server is currently in pending"
                    + " shutdown state. You have to either wait for shutdown to"
                    + " complete or force it by calling shutdownNow()");
        }
        state = State.RUNNING;
        shutdownFuture = null;

        configureAuxThreadPool();

        delayedExecutor = new DelayedExecutor(auxExecutorService);
        delayedExecutor.start();

        for (final NetworkListener listener : listeners.values()) {
            configureListener(listener);
        }

        ServerConfiguration serverConfig = getServerConfiguration();
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
        ServerConfiguration serverConfig = getServerConfiguration();
        serverConfig.addJmxEventListener(httpHandlerChain);

        synchronized (serverConfig.handlersSync) {
            for (final HttpHandler httpHandler : serverConfig.orderedHandlers) {
                httpHandlerChain.addHandler(httpHandler,
                        serverConfig.handlers.get(httpHandler));
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
    public boolean isStarted() {
        return state != State.STOPPED;
    }

    @Override
    public synchronized GrizzlyFuture<HttpServer> shutdown(final long gracePeriod,
                                                           final TimeUnit timeUnit) {
        if (state != State.RUNNING) {
            return shutdownFuture != null ? shutdownFuture :
                    Futures.<HttpServer> createReadyFuture(this);
        }

        shutdownFuture = Futures.createSafeFuture();
        state = State.STOPPING;

        final int listenersCount = listeners.size();
        final FutureImpl<HttpServer> shutdownFutureLocal = shutdownFuture;

        final CompletionHandler<NetworkListener> shutdownCompletionHandler =
                new EmptyCompletionHandler<NetworkListener>() {
                    final AtomicInteger counter =
                            new AtomicInteger(listenersCount);

                    @Override
                    public void completed(final NetworkListener networkListener) {
                        if (counter.decrementAndGet() == 0) {
                            try {
                                shutdownNow();
                                shutdownFutureLocal.result(OXHttpServer.this);
                            } catch (Throwable e) {
                                shutdownFutureLocal.failure(e);
                            }
                        }
                    }
                };

        if (listenersCount > 0) {
            for (NetworkListener listener : listeners.values()) {
                listener.shutdown(gracePeriod, timeUnit).addCompletionHandler(shutdownCompletionHandler);
            }
        } else {
            // No listeners (edge-case), so call shutdown now to ensure the server is properly torn down.
            shutdownNow();
            shutdownFutureLocal.result(OXHttpServer.this);
        }


        return shutdownFuture;
    }

    /**
     * <p>
     * Gracefully shuts down the <code>HttpServer</code> instance.
     * </p>
     */
    @Override
    public synchronized GrizzlyFuture<HttpServer> shutdown() {
        return shutdown(-1, TimeUnit.MILLISECONDS);
    }

    /**
     * <p>
     * Immediately shuts down the <code>HttpServer</code> instance.
     * </p>
     */
    @Override
    public synchronized void shutdownNow() {

        if (state == State.STOPPED) {
            return;
        }
        state = State.STOPPED;

        try {
            ServerConfiguration serverConfig = getServerConfiguration();
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
            delayedExecutor.destroy();
            delayedExecutor = null;

            stopAuxThreadPool();

            if (serverConfig.isJmxEnabled()) {
                disableJMX();
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, null, e);
        } finally {
            for (final NetworkListener listener : listeners.values()) {
                final Processor<?> p = listener.getTransport().getProcessor();
                if (p instanceof FilterChain) {
                    ((FilterChain) p).clear();
                }
            }

            if (shutdownFuture != null) {
                shutdownFuture.result(this);
            }
        }

    }

    /**
     * <p>
     * Immediately shuts down the <code>HttpServer</code> instance.
     * </p>
     *
     * @deprecated use {@link #shutdownNow()}
     */
    @Deprecated
    @Override
    public void stop() {
        shutdownNow();
    }

    /**
     * @return a <code>HttpServer</code> configured to listen to requests
     * on {@link NetworkListener#DEFAULT_NETWORK_HOST}:{@link NetworkListener#DEFAULT_NETWORK_PORT},
     * using the directory in which the server was launched the server's document root
     */
    public static HttpServer createSimpleServer() {

        return createSimpleServer(".");

    }


    /**
     * @param docRoot the document root,
     *   can be <code>null</code> when no static pages are needed
     *
     * @return a <code>HttpServer</code> configured to listen to requests
     * on {@link NetworkListener#DEFAULT_NETWORK_HOST}:{@link NetworkListener#DEFAULT_NETWORK_PORT},
     * using the specified <code>docRoot</code> as the server's document root
     */
    public static HttpServer createSimpleServer(final String docRoot) {

        return createSimpleServer(docRoot, NetworkListener.DEFAULT_NETWORK_PORT);

    }


    /**
     * @param docRoot the document root,
     *   can be <code>null</code> when no static pages are needed
     * @param port the network port to which this listener will bind
     *
     * @return a <code>HttpServer</code> configured to listen to requests
     * on {@link NetworkListener#DEFAULT_NETWORK_HOST}:<code>port</code>,
     * using the specified <code>docRoot</code> as the server's document root
     */
    public static HttpServer createSimpleServer(final String docRoot,
                                                final int port) {

        return createSimpleServer(docRoot, NetworkListener.DEFAULT_NETWORK_HOST, port);

    }


    /**
     * @param docRoot the document root,
     *   can be <code>null</code> when no static pages are needed
     * @param range port range to attempt to bind to
     *
     * @return a <code>HttpServer</code> configured to listen to requests
     * on {@link NetworkListener#DEFAULT_NETWORK_HOST}:<code>[port-range]</code>,
     * using the specified <code>docRoot</code> as the server's document root
     */
    public static HttpServer createSimpleServer(final String docRoot,
                                                final PortRange range) {

        return createSimpleServer(docRoot,
                NetworkListener.DEFAULT_NETWORK_HOST,
                range);

    }

    /**
     * @param docRoot the document root,
     *   can be <code>null</code> when no static pages are needed
     * @param socketAddress the endpoint address to which this listener will bind
     *
     * @return a <code>HttpServer</code> configured to listen to requests
     * on <code>socketAddress</code>,
     * using the specified <code>docRoot</code> as the server's document root
     */
    public static HttpServer createSimpleServer(final String docRoot,
                                                final SocketAddress socketAddress) {

        final InetSocketAddress inetAddr = (InetSocketAddress) socketAddress;
        return createSimpleServer(docRoot, inetAddr.getHostName(), inetAddr.getPort());
    }

    /**
     * @param docRoot the document root,
     *   can be <code>null</code> when no static pages are needed
     * @param host the network port to which this listener will bind
     * @param port the network port to which this listener will bind
     *
     * @return a <code>HttpServer</code> configured to listen to requests
     * on <code>host</code>:<code>port</code>,
     * using the specified <code>docRoot</code> as the server's document root
     */
    public static HttpServer createSimpleServer(final String docRoot,
                                                final String host,
                                                final int port) {

        return createSimpleServer(docRoot, host, new PortRange(port));

    }

    /**
     * @param docRoot the document root,
     *   can be <code>null</code> when no static pages are needed
     * @param host the network port to which this listener will bind
     * @param range port range to attempt to bind to
     *
     * @return a <code>HttpServer</code> configured to listen to requests
     * on <code>host</code>:<code>[port-range]</code>,
     * using the specified <code>docRoot</code> as the server's document root
     */
    public static HttpServer createSimpleServer(final String docRoot,
                                                final String host,
                                                final PortRange range) {

        final HttpServer server = new HttpServer();
        final ServerConfiguration config = server.getServerConfiguration();
        if (docRoot != null) {
            config.addHttpHandler(new StaticHttpHandler(docRoot), "/");
        }
        final NetworkListener listener =
                new NetworkListener("grizzly",
                                    host,
                                    range);
        server.addListener(listener);
        return server;

    }


    // --------------------------------------------------------- Private Methods


    @SuppressWarnings("deprecation")
    private void configureListener(final NetworkListener listener) {
        FilterChain chain = listener.getFilterChain();
        if (chain == null) {
            final FilterChainBuilder builder = FilterChainBuilder.stateless();
            builder.add(new TransportFilter());
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
                final SSLBaseFilter filter = new SSLBaseFilter(sslConfig);
                builder.add(filter);

            }
            final int maxHeaderSize = listener.getMaxHttpHeaderSize() == -1
                                        ? org.glassfish.grizzly.http.HttpServerFilter.DEFAULT_MAX_HTTP_PACKET_HEADER_SIZE
                                        : listener.getMaxHttpHeaderSize();

            // Passing null value for the delayed executor, because IdleTimeoutFilter should
            // handle idle connections for us
            final org.glassfish.grizzly.http.HttpServerFilter httpServerCodecFilter =
                    new CustomHttpCodecFilter(listener.isChunkingEnabled(),
                                         maxHeaderSize,
                                         null,
                                         listener.getKeepAlive(),
                                         null,
                                         listener.getMaxRequestHeaders(),
                                         listener.getMaxResponseHeaders());
            final Set<ContentEncoding> contentEncodings =
                    configureCompressionEncodings(listener);
            for (ContentEncoding contentEncoding : contentEncodings) {
                httpServerCodecFilter.addContentEncoding(contentEncoding);
            }
            ServerConfiguration serverConfig = getServerConfiguration();
            httpServerCodecFilter.setAllowPayloadForUndefinedHttpMethods(
                    serverConfig.isAllowPayloadForUndefinedHttpMethods());
            httpServerCodecFilter.setMaxPayloadRemainderToSkip(
                    serverConfig.getMaxPayloadRemainderToSkip());

            httpServerCodecFilter.getMonitoringConfig().addProbes(
                    serverConfig.getMonitoringConfig().getHttpConfig().getProbes());
            builder.add(httpServerCodecFilter);

            builder.add(new IdleTimeoutFilter(delayedExecutor,
                    listener.getKeepAlive().getIdleTimeoutInSeconds(),
                    TimeUnit.SECONDS));

            final Transport transport = listener.getTransport();
            final FileCache fileCache = listener.getFileCache();
            fileCache.initialize(delayedExecutor);
            final FileCacheFilter fileCacheFilter = new FileCacheFilter(fileCache);
            fileCache.getMonitoringConfig().addProbes(
                    serverConfig.getMonitoringConfig().getFileCacheConfig().getProbes());
            builder.add(fileCacheFilter);

            final ServerFilterConfiguration config = new ServerFilterConfiguration(serverConfig);

            if (listener.isSendFileExplicitlyConfigured()) {
                config.setSendFileEnabled(listener.isSendFileEnabled());
                fileCache.setFileSendEnabled(listener.isSendFileEnabled());
            }

            if (listener.getBackendConfiguration() != null) {
                config.setBackendConfiguration(listener.getBackendConfiguration());
            }

            if (listener.getDefaultErrorPageGenerator() != null) {
                config.setDefaultErrorPageGenerator(listener.getDefaultErrorPageGenerator());
            }

            if (listener.getSessionManager() != null) {
                config.setSessionManager(listener.getSessionManager());
            }

            config.setTraceEnabled(config.isTraceEnabled() || listener.isTraceEnabled());

            config.setMaxFormPostSize(listener.getMaxFormPostSize());
            config.setMaxBufferedPostSize(listener.getMaxBufferedPostSize());

            config.setSessionTimeoutSeconds(grizzlyConfig.getCookieMaxInactivityInterval());

            final HttpServerFilter httpServerFilter = new OXHttpServerFilter(grizzlyConfig, config, delayedExecutor);
            httpServerFilter.setHttpHandler(httpHandlerChain);

            httpServerFilter.getMonitoringConfig().addProbes(
                    serverConfig.getMonitoringConfig().getWebServerConfig().getProbes());

            builder.add(httpServerFilter);

            final AddOn[] addons = listener.getAddOnSet().getArray();
            if (addons != null) {
                for (AddOn addon : addons) {
                    addon.setup(listener, builder);
                }
            }

            chain = builder.build();
            listener.setFilterChain(chain);

            final int transactionTimeout = listener.getTransactionTimeout();
            if (transactionTimeout >= 0) {
                ThreadPoolConfig threadPoolConfig = transport.getWorkerThreadPoolConfig();

                if (threadPoolConfig != null) {
                    threadPoolConfig.setTransactionTimeout(
                            delayedExecutor,
                            transactionTimeout,
                            TimeUnit.SECONDS);
                }

            }

        }
        configureMonitoring(listener);
    }

    @Override
    protected Set<ContentEncoding> configureCompressionEncodings(
            final NetworkListener listener) {

        final CompressionConfig compressionConfig = listener.getCompressionConfig();

        if (compressionConfig.getCompressionMode() != CompressionMode.OFF) {
            final ContentEncoding gzipContentEncoding = new GZipContentEncoding(
                GZipContentEncoding.DEFAULT_IN_BUFFER_SIZE,
                GZipContentEncoding.DEFAULT_OUT_BUFFER_SIZE,
                new CompressionEncodingFilter(compressionConfig,
                    GZipContentEncoding.getGzipAliases()));
            final ContentEncoding lzmaEncoding = new LZMAContentEncoding(
                    new CompressionEncodingFilter(compressionConfig,
                    LZMAContentEncoding.getLzmaAliases()));
            final Set<ContentEncoding> set = new HashSet<ContentEncoding>(2);
            set.add(gzipContentEncoding);
            set.add(lzmaEncoding);
            return set;
        } else {
            return Collections.emptySet();
        }
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

        ServerConfiguration serverConfig = getServerConfiguration();
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
                        getServerConfiguration().getName() + "-" + threadCounter.getAndIncrement(),
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
    synchronized void onAddHttpHandler(HttpHandler httpHandler,
            final HttpHandlerRegistration[] registrations) {
        if (isStarted()) {
            httpHandlerChain.addHandler(httpHandler, registrations);
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
