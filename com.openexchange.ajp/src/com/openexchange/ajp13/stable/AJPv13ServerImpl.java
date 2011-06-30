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

package com.openexchange.ajp13.stable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.NotCompliantMBeanException;
import org.apache.commons.logging.Log;
import com.openexchange.ajp13.AJPv13Config;
import com.openexchange.ajp13.AJPv13Server;
import com.openexchange.ajp13.exception.AJPv13Exception;
import com.openexchange.ajp13.exception.AJPv13Exception.AJPCode;
import com.openexchange.ajp13.monitoring.AJPv13Monitors;
import com.openexchange.ajp13.monitoring.AJPv13TaskMonitorMBean;
import com.openexchange.ajp13.servlet.ServletConfigLoader;

/**
 * {@link AJPv13ServerImpl} - The AJP server which accepts incoming socket connections and delegates its processing to a dedicated AJP
 * listener
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class AJPv13ServerImpl extends AJPv13Server implements Runnable {

    private static final transient org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(AJPv13ServerImpl.class);

    private static final DecimalFormat DF = new DecimalFormat("0000");

    static final AJPv13TaskMonitor LISTENER_MONITOR;

    static {
        AJPv13TaskMonitor tmp = null;
        try {
            tmp = new AJPv13TaskMonitor();
        } catch (final NotCompliantMBeanException e) {
            LOG.error(e.getMessage(), e);
        }
        LISTENER_MONITOR = tmp;
    }

    /**
     * Gets the listener monitor.
     * 
     * @return The listener monitor.
     */
    public static AJPv13TaskMonitorMBean getListenerMonitor() {
        return LISTENER_MONITOR;
    }

    // member fields
    private ServerSocket serverSocket;

    private Thread[] threadArr;

    private final AtomicBoolean running;

    /**
     * Initializes a new {@link AJPv13ServerImpl}
     */
    public AJPv13ServerImpl() {
        super();
        running = new AtomicBoolean();
    }

    /**
     * Starts this AJP server instance
     * 
     * @throws AJPv13Exception If starting this instance fails
     */
    @Override
    public void startServer() throws AJPv13Exception {
        if (running.compareAndSet(false, true)) {
            try {
                serverSocket = new ServerSocket(AJPv13Config.getAJPPort(), DEFAULT_BACKLOG, AJPv13Config.getAJPBindAddress());
            } catch (final IOException ex) {
                throw new AJPv13Exception(AJPCode.STARTUP_ERROR, false, ex, Integer.valueOf(AJPv13Config.getAJPPort()));
            }
            ServletConfigLoader.initDefaultInstance(AJPv13Config.getServletConfigs());
            initializePools();
            AJPv13Watcher.initializeAJPv13Watcher();
            /*
             * Initialize server threads
             */
            threadArr = new Thread[AJPv13Config.getAJPServerThreadSize()];
            final CountDownLatch startGate = new CountDownLatch(1);
            if (threadArr.length > 0) {
                final StringBuilder sb = new StringBuilder(32);
                threadArr[0] = new Thread(new GateRunnable(startGate, this, LOG));
                threadArr[0].setName(sb.append("AJPServer-").append(DF.format((1))).toString());
                threadArr[0].setPriority(Thread.MAX_PRIORITY);
                threadArr[0].start();
                for (int i = 1; i < threadArr.length; i++) {
                    threadArr[i] = new Thread(new GateRunnable(startGate, this, LOG));
                    sb.setLength(0);
                    threadArr[i].setName(sb.append("AJPServer-").append(DF.format((i + 1))).toString());
                    threadArr[i].setPriority(Thread.MAX_PRIORITY);
                    threadArr[i].start();
                }
            }
            /*
             * Open gate to start-up all server threads at the same time
             */
            startGate.countDown();
            AJPv13Monitors.AJP_MONITOR_SERVER_THREADS.setNumActive(threadArr.length);
        } else {
            if (LOG.isInfoEnabled()) {
                LOG.info("AJPv13Server is already running...");
            }
        }
    }

    /**
     * Stops this AJP server instance
     */
    @Override
    public void stopServer() {
        if (running.compareAndSet(true, false)) {
            /*
             * Stop listeners
             */
            AJPv13Watcher.stopListeners();
            /*
             * Reset watcher
             */
            AJPv13Watcher.resetAJPv13Watcher();
            /*
             * Reset pools
             */
            resetPools();
            /*
             * Reset default servlet config loader
             */
            ServletConfigLoader.resetDefaultInstance();
            /*
             * Interrupt & destroy threads
             */
            final StringBuilder sb = new StringBuilder(128);
            for (int i = 0; i < threadArr.length; i++) {
                try {
                    threadArr[i].interrupt();
                } catch (final Exception e) {
                    LOG.error(sb.append(threadArr[i].getName()).append(" could NOT be interrupted").toString(), e);
                    sb.setLength(0);
                } finally {
                    threadArr[i] = null;
                }
            }
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (final IOException e) {
                    LOG.error(
                        sb.append("AJP server socket bound to port ").append(AJPv13Config.getAJPPort()).append(" cannot be closed").toString(),
                        e);
                    sb.setLength(0);
                }
                serverSocket = null;
            }
            AJPv13Monitors.AJP_MONITOR_SERVER_THREADS.setNumActive(0);
        } else {
            if (LOG.isInfoEnabled()) {
                LOG.info("AJPv13Server is not running and thus does not need to be stopped");
            }
        }
    }

    /**
     * Initializes associated pools: listener pool, connection pool, and request handler pool.
     */
    private void initializePools() {
        resetPools();
        AJPv13ListenerPool.initPool();
        if (AJPv13Config.useAJPConnectionPool()) {
            AJPv13ConnectionPool.initConnectionPool();
        }
        if (AJPv13Config.useAJPRequestHandlerPool()) {
            AJPv13RequestHandlerPool.initPool();
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("All pools initialized...");
        }
    }

    /**
     * Resets associated pools: listener pool, connection pool, and request handler pool.
     */
    private void resetPools() {
        if (running.get()) {
            AJPv13ListenerPool.resetPool();
            if (AJPv13Config.useAJPConnectionPool()) {
                AJPv13ConnectionPool.resetConnectionPool();
            }
            if (AJPv13Config.useAJPRequestHandlerPool()) {
                AJPv13RequestHandlerPool.resetPool();
            }
        }
    }

    /**
     * Checks if this AJP server instance is running
     * 
     * @return <code>true</code> if this AJP server instance is running; otherwise <code>false</code>
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    public void run() {
        boolean keepOnRunning = true;
        AcceptSocket: while (keepOnRunning && running.get()) {
            Socket client;
            try {
                client = serverSocket.accept();
                if (Thread.currentThread().isInterrupted()) {
                    break AcceptSocket;
                }
                final long start = System.currentTimeMillis();
                client.setTcpNoDelay(true);
                incrementNumberOfOpenAJPSockets();
                AJPv13Listener l = AJPv13ListenerPool.getListener();
                while (!l.startListener(client)) {
                    /*
                     * Not possible to start current listener, get next one from pool and let the current one die...
                     */
                    l = AJPv13ListenerPool.getListener();
                }
                AJPv13Monitors.AJP_MONITOR_SERVER_THREADS.addUseTime(System.currentTimeMillis() - start);
            } catch (final java.net.SocketException e) {
                /*
                 * Socket closed while being blocked in accept
                 */
                if (LOG.isDebugEnabled()) {
                    LOG.debug("AJP socket closed", e);
                }
                LOG.info("AJPv13Server down");
                keepOnRunning = false;
            } catch (final IOException ex) {
                LOG.error(ex.getMessage(), ex);
                keepOnRunning = false;
            }
        }
    }

    private static final class GateRunnable implements Runnable {

        private final transient org.apache.commons.logging.Log logger;

        private final Runnable task;

        private final CountDownLatch latch;

        public GateRunnable(final CountDownLatch latch, final Runnable task, final Log logger) {
            super();
            this.task = task;
            this.latch = latch;
            this.logger = logger;
        }

        public void run() {
            try {
                latch.await();
                task.run();
            } catch (final InterruptedException e) {
                logger.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            } catch (final Exception e) {
                logger.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
    }

}
