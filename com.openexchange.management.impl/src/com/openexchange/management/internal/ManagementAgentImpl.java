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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.management.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.management.remote.rmi.RMIJRMPServerImpl;
import javax.management.remote.rmi.RMIServerImpl;
import com.openexchange.exception.OXException;
import com.openexchange.management.ManagementExceptionCode;
import com.openexchange.management.ManagementService;

/**
 * {@link ManagementAgentImpl} - A JMX agent implementation
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ManagementAgentImpl extends AbstractAgent implements ManagementService {

    private static final ManagementAgentImpl instance = new ManagementAgentImpl();

    /**
     * Gets the singleton instance
     *
     * @return The singleton instance
     */
    public static ManagementAgentImpl getInstance() {
        return instance;
    }

    /*
     * Member fields
     */
    private int jmxPort;

    private int jmxServerPort;

    private boolean jmxSinglePort;

    private String jmxBindAddr;

    private String jmxLogin;

    private String jmxPassword;

    private final Stack<ObjectName> objectNames = new Stack<ObjectName>();

    private JMXServiceURL jmxURL;

    private final AtomicBoolean running = new AtomicBoolean();

    private ManagementAgentImpl() {
        super();
        jmxPort = 9999;
        jmxServerPort = 3000;
        jmxSinglePort = false;
    }

    @Override
    public void run() {
        initializeMBeanServer();
    }

    private void initializeMBeanServer() {
        if (running.get()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("MonitorAgent already running...");
            }
            return;
        }
        try {
            if (jmxSinglePort) {
                // We create a couple of SslRMIClientSocketFactory and
                // SslRMIServerSocketFactory. We will use the same factories to export
                // the RMI Registry and the JMX RMI Connector Server objects. This
                // will allow us to use the same port for all the exported objects.
                // If we didn't use the same factories everywhere, we would have to
                // use at least two ports, because two different RMI Socket Factories
                // cannot share the same port.
                //
                //// final CustomRMISocketFactory sf = new CustomRMISocketFactory(jmxBindAddr == null ? "*" : jmxBindAddr.trim());
                final RMIClientSocketFactory csf = new CustomSslRMIClientSocketFactory();
                final RMIServerSocketFactory ssf = new CustomSslRmiServerSocketFactory(jmxBindAddr == null ? "*" : jmxBindAddr.trim());
                // Create the RMI Registry using the SSL socket factories above.
                // In order to use a single port, we must use these factories
                // everywhere, or nowhere. Since we want to use them in the JMX
                // RMI Connector server, we must also use them in the RMI Registry.
                // Otherwise, we wouldn't be able to use a single port.
                //
                final Registry registry;
                {
                    Registry registry0 = null;
                    try {
                        /*
                         * If following calls succeed, a RMI registry has already been created that listens on this port
                         */
                        registry0 = LocateRegistry.getRegistry(jmxPort);
                        registry0.list();
                    } catch (final RemoteException e) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(
                                "No responsive RMI registry found that listens on port " + jmxPort + ". A new one is going to be created",
                                e);
                        }
                        /*
                         * Create a new one
                         */
                        registry0 = LocateRegistry.createRegistry(jmxPort, csf, ssf);
                    }
                    registry = registry0;
                }
                registries.put(Integer.valueOf(jmxPort), registry);
                if (LOG.isInfoEnabled()) {
                    LOG.info(new StringBuilder(128).append("RMI registry created on port ").append(jmxPort).append(" and bind address ").append(
                        jmxBindAddr == null ? "*" : jmxBindAddr.trim()).toString());
                }
                // Environment map.
                //
                final Map<String, Object> env = new HashMap<String, Object>();
                // Manually creates and binds a JMX RMI Connector Server stub with the
                // registry created above: the port we pass here is the port that can
                // be specified in "service:jmx:rmi://"+hostname+":"+port - where the
                // RMI server stub and connection objects will be exported.
                // Here we choose to use the same port as was specified for the
                // RMI Registry. We can do so because we're using \*the same\* client
                // and server socket factories, for the registry itself \*and\* for this
                // object.
                //
                final RMIServerImpl stub = new RMIJRMPServerImpl(jmxPort, csf, ssf, env);

                // Now specify the SSL Socket Factories:
                //
                // For the client side (remote)
                //
                //// env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE,csf);
                // For the server side (local)
                //
                ////env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE,ssf);
                // For binding the JMX RMI Connector Server with the registry
                // created above:
                //
                // //env.put("com.sun.jndi.rmi.factory.socket", csf);
                // Create an RMI connector server.
                //
                // As specified in the JMXServiceURL the RMIServer stub will be
                // registered in the RMI registry running in the local host on
                // port 3000 with the name "jmxrmi". This is the same name the
                // out-of-the-box management agent uses to register the RMIServer
                // stub too.
                //
                // The port specified in "service:jmx:rmi://"+hostname+":"+port
                // is the second port, where RMI connection objects will be exported.
                // Here we use the same port as that we choose for the RMI registry.
                // The port for the RMI registry is specified in the second part
                // of the URL, in "rmi://"+hostname+":"+port
                //
                final String hostname = InetAddress.getLocalHost().getHostName();
                final JMXServiceURL url =
                    new JMXServiceURL(
                        "service:jmx:rmi://" + hostname + ":" + jmxPort + "/jndi/rmi://" + hostname + ":" + jmxPort + "/jmxrmi");
                // Now create the server from the JMXServiceURL
                //
                //// final JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs)
                // Now create the server manually....
                // We can't use the JMXConnectorServerFactory because of
                // http://bugs.sun.com/view_bug.do?bug_id=5107423
                //
                final JMXConnectorServer cs = new RMIConnectorServer(new JMXServiceURL("rmi", hostname, jmxPort), env, stub, mbs) {

                    @Override
                    public JMXServiceURL getAddress() {
                        return url;
                    }

                    @Override
                    public synchronized void start() throws IOException {
                        try {
                            registry.bind("jmxrmi", stub);
                        } catch (final AlreadyBoundException x) {
                            final IOException io = new IOException(x.getMessage());
                            io.initCause(x);
                            throw io;
                        }
                        super.start();
                    }
                };

                // Start the RMI connector server.
                //
                cs.start();
                connectors.put(url, cs);
                if (LOG.isInfoEnabled()) {
                    LOG.info(new StringBuilder("JMX connector server on ").append(url).append(" started"));
                }
                jmxURL = url;
            } else {
                addRMIRegistry(jmxPort, jmxBindAddr);
                /*
                 * Create a JMX connector and start it
                 */
                final String ip = getHostName(jmxBindAddr.charAt(0) == '*' ? "localhost" : jmxBindAddr);
                /*-
                 * Start JMX URL
                 * service:jmx:rmi://<TARGET_MACHINE>:<JMX_RMI_SERVER_PORT>/jndi/rmi://<TARGET_MACHINE>:<RMI_REGISTRY_PORT>/jmxrmi
                 * The RMI registry tells the JMX clients where to find the JMX RMI port, specified via the jmxrmi key
                 * The RMI port is generally known and can be set via properties.
                 * The JMX RMI server port is normally chosen by the jvm at random
                 * 
                 * To obtain the target machine connect to service:jmx:rmi:///jndi/rmi://<TARGET_MACHINE>:<RMI_REGISTRY_PORT>/jmxrmi
                 * To obtain the JMX RMI server port connect to service:jmx:rmi/<TARGET_MACHINE>/jndi/rmi://<TARGET_MACHINE>:<RMI_REGISTRY_PORT>/jmxrmi
                 *  
                 *  Our URL service:jmx:rmi:///jndi/rmi://localhost:9999/server
                 */
                final StringBuilder jmxURLBuilder = new StringBuilder(128).append("service:jmx:rmi://");
                /*
                 * Set RMIServer and RMIConnection host & port
                 */
                jmxURLBuilder.append(ip == null ? "localhost" : ip).append(':').append(jmxServerPort);
                /*-
                 * Set RMI Registry host & port
                 * 
                 * In the URL, jmxServerPort is the port number on which the RMIServer and RMIConnection remote objects are exported and
                 * jmxPort is the port number of the RMI Registry.
                 */
                jmxURLBuilder.append("/jndi/rmi://").append(ip == null ? "localhost" : ip).append(':').append(jmxPort).append("/server");
                jmxURL = addConnectorServer(jmxURLBuilder.toString(), jmxLogin, jmxPassword);
            }
            if (LOG.isInfoEnabled()) {
                LOG.info(new StringBuilder(128).append("\n\n\tUse JConsole or MC4J to connect to MBeanServer with this url: ").append(
                    jmxURL).append("\n").toString());
            }
            running.set(true);
        } catch (final MalformedURLException e) {
            LOG.error(e.getMessage(), e);
        } catch (final UnknownHostException e) {
            LOG.error(e.getMessage(), e);
        } catch (final RemoteException e) {
            LOG.error(e.getMessage(), e);
        } catch (final IOException e) {
            LOG.error(e.getMessage(), e);
        } catch (final OXException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        if (!running.get()) {
            return;
        }
        try {
            while (!objectNames.isEmpty()) {
                unregisterMBean(objectNames.pop());
            }
        } catch (final OXException e) {
            LOG.error(e.getMessage(), e);
        }
        removeConnectorServer(jmxURL);
        /*
         * By now there's no API call to close/unexport a RMI registry. Therefore the RMI registry created in start() method still remains
         * in VM. See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4457683 or
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4508962 for details.
         */
        running.set(false);
    }

    private static String getHostName(final String host) {
        if (host == null) {
            return null;
        }
        try {
            return InetAddress.getByName(host).getHostName();
        } catch (final UnknownHostException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void registerMBean(final String name, final Object mbean) throws OXException {
        if (!running.get()) {
            throw ManagementExceptionCode.NOT_RUNNING.create();
        }
        final ObjectName objectName;
        try {
            objectName = new ObjectName(name);
        } catch (final MalformedObjectNameException e) {
            throw ManagementExceptionCode.MALFORMED_OBJECT_NAME.create(e, name);
        }
        super.registerMBean(objectName, mbean);
        objectNames.push(objectName);
    }

    @Override
    public void registerMBean(final ObjectName objectName, final Object mbean) throws OXException {
        if (!running.get()) {
            throw ManagementExceptionCode.NOT_RUNNING.create();
        }
        super.registerMBean(objectName, mbean);
        objectNames.push(objectName);
    }

    @Override
    public void unregisterMBean(final String name) throws OXException {
        if (!running.get()) {
            throw ManagementExceptionCode.NOT_RUNNING.create();
        }
        final ObjectName objectName;
        try {
            objectName = new ObjectName(name);
        } catch (final MalformedObjectNameException e) {
            throw ManagementExceptionCode.MALFORMED_OBJECT_NAME.create(e, name);
        }
        super.unregisterMBean(objectName);
        objectNames.remove(objectName);
    }

    @Override
    public void unregisterMBean(final ObjectName objectName) throws OXException {
        if (!running.get()) {
            throw ManagementExceptionCode.NOT_RUNNING.create();
        }
        super.unregisterMBean(objectName);
        objectNames.remove(objectName);
    }

    /**
     * Sets whether to use a single JMX port; meaning RMI registry port and the one used to export JMX RMI connection objects are the same.
     * 
     * @param jmxSinglePort <code>true</code> to use a single JMX port; otherwise <code>false</code>
     */
    public void setJmxSinglePort(final boolean jmxSinglePort) {
        this.jmxSinglePort = jmxSinglePort;
    }

    /**
     * Sets the JMX server port.
     *
     * @param jmxServerPort The JMX server port to set
     */
    public void setJmxServerPort(final int jmxServerPort) {
        this.jmxServerPort = jmxServerPort;
    }

    /**
     * Sets the JMX port
     *
     * @param jmxPort The JMX port
     */
    public void setJmxPort(final int jmxPort) {
        this.jmxPort = jmxPort;
    }

    /**
     * Sets the JMX bind address
     *
     * @param jmxBindAddr The JMX bind address or <code>"*"</code>
     */
    public void setJmxBindAddr(final String jmxBindAddr) {
        this.jmxBindAddr = jmxBindAddr;
    }

    /**
     * Sets the JMX login
     *
     * @param jmxLogin The JMX login to set
     */
    public void setJmxLogin(final String jmxLogin) {
        this.jmxLogin = jmxLogin;
    }

    /**
     * Sets the JMX password
     *
     * @param jmxPassword the JMX password
     */
    public void setJmxPassword(final String jmxPassword) {
        this.jmxPassword = jmxPassword;
    }

}
