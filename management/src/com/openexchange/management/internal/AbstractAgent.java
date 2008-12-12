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

package com.openexchange.management.internal;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.monitor.GaugeMonitor;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXPrincipal;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.security.auth.Subject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openexchange.management.ManagementException;

/**
 * {@link AbstractAgent} - An abstract JMX agent
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class AbstractAgent {

	static final Log LOG = LogFactory.getLog(AbstractAgent.class);

	private static final class AbstractAgentSocketFactory extends RMISocketFactory implements Serializable {

		/**
		 * serialVersionUID
		 */
		private static final long serialVersionUID = 8324426326551371658L;

		private final int backlog;

		private final InetAddress bindAddress;

		public AbstractAgentSocketFactory(final int backlog, final String bindAddr) throws UnknownHostException {
			this.backlog = backlog < 1 ? 50 : backlog;
			bindAddress = bindAddr.charAt(0) == '*' ? null : InetAddress.getByName(bindAddr);
		}

		@Override
		public ServerSocket createServerSocket(final int port) throws IOException {
			return new ServerSocket(port, backlog, bindAddress);
		}

		@Override
		public Socket createSocket(final String host, final int port) throws IOException {
			return new Socket(bindAddress == null ? InetAddress.getByName(host) : bindAddress, port);
		}

	}

	private static final class AbstractAgentJMXAuthenticator implements JMXAuthenticator {

		private static volatile Charset US_ASCII;

		private static Charset getUSASCII() {
			if (US_ASCII == null) {
				synchronized (AbstractAgentJMXAuthenticator.class) {
					if (US_ASCII == null) {
						US_ASCII = Charset.forName("US-ASCII");
					}
				}
			}
			return US_ASCII;
		}

		private final String[] credentials;

		public AbstractAgentJMXAuthenticator(final String[] credentials) {
			super();
			this.credentials = new String[credentials.length];
			System.arraycopy(credentials, 0, this.credentials, 0, credentials.length);
		}

		public Subject authenticate(final Object credentials) {
			if (!(credentials instanceof String[])) {
				if (credentials == null) {
					throw new SecurityException("Credentials required");
				}
				throw new SecurityException("Credentials should be String[]");
			}
			final String[] creds = (String[]) credentials;
			if (creds.length != 2) {
				throw new SecurityException("Credentials should have 2 elements");
			}
			/*
			 * Perform authentication
			 */
			final String username = creds[0];
			final String password = creds[1];
			if ((this.credentials[0].equals(username)) && (this.credentials[1].equals(makeSHAPasswd(password)))) {
				return new Subject(true, Collections.singleton(new JMXPrincipal(username)), Collections.EMPTY_SET,
						Collections.EMPTY_SET);
			}
			throw new SecurityException("Invalid credentials");

		}

		private static String makeSHAPasswd(final String raw) {
			MessageDigest md;

			try {
				md = MessageDigest.getInstance("SHA-1");
			} catch (final NoSuchAlgorithmException e) {
				LOG.error(e.getMessage(), e);
				return raw;
			}

			final byte[] salt = {};

			md.reset();
			try {
				md.update(raw.getBytes("UTF-8"));
			} catch (final UnsupportedEncodingException e) {
				/*
				 * Cannot occur
				 */
				LOG.error(e.getMessage(), e);
			}
			md.update(salt);

			final String ret = getUSASCII().decode(
					ByteBuffer.wrap(Base64.encodeBase64(md.digest()))).toString();

			return ret;
		}

	}

	private final AtomicBoolean initialized;

	private final Map<Integer, Registry> registries;

	private final Map<JMXServiceURL, JMXConnectorServer> connectors;

	protected final AtomicReference<GaugeMonitor> gaugeMonitorRef;

	protected RMISocketFactory rmiSocketFactory;

	protected MBeanServer mbs;

	/**
	 * Initializes a new {@link AbstractAgent}
	 */
	protected AbstractAgent() {
		super();
		gaugeMonitorRef = new AtomicReference<GaugeMonitor>();
		initialized = new AtomicBoolean();
		registries = new HashMap<Integer, Registry>();
		connectors = new HashMap<JMXServiceURL, JMXConnectorServer>();
		mbs = ManagementFactory.getPlatformMBeanServer();
	}

	/**
	 * Uniquely identifies the MBean and registers it to MBeanServer
	 * 
	 * @param name
	 *            The MBeans's object name as a string
	 * @param mbean
	 *            The MBean object
	 * @throws ManagementException
	 *             If registering the MBean to MBeanServer fails
	 */
	public void registerMBean(final String name, final Object mbean) throws ManagementException {
		try {
			registerMBean(new ObjectName(name), mbean);
		} catch (final MalformedObjectNameException e) {
			throw new ManagementException(ManagementException.Code.MALFORMED_OBJECT_NAME, e, name);
		}
	}

	/**
	 * Unregister the MBean identified through specified object name as a
	 * string.
	 * 
	 * @param name
	 *            The MBean's object name as a string
	 * @throws ManagementException
	 *             If unregistering the MBean fails
	 */
	public void unregisterMBean(final String name) throws ManagementException {
		try {
			unregisterMBean(new ObjectName(name));
		} catch (final MalformedObjectNameException e) {
			throw new ManagementException(ManagementException.Code.MALFORMED_OBJECT_NAME, e, name);
		}
	}

	/**
	 * Uniquely identifies the MBean and registers it to MBeanServer
	 * 
	 * @param objectName
	 *            The MBean's object name
	 * @param mbean
	 *            The MBean object
	 * @throws ManagementException
	 *             If registering the MBean to MBeanServer fails
	 */
	public void registerMBean(final ObjectName objectName, final Object mbean) throws ManagementException {
		if (mbs.isRegistered(objectName)) {
		    LOG.warn(objectName.getCanonicalName() + " already registered");
			return;
		}
		try {
			mbs.registerMBean(mbean, objectName);
		} catch (final InstanceAlreadyExistsException e) {
			throw new ManagementException(ManagementException.Code.ALREADY_EXISTS, e, mbean.getClass().getName());
		} catch (final MBeanRegistrationException e) {
			throw new ManagementException(ManagementException.Code.MBEAN_REGISTRATION, e, mbean.getClass().getName());
		} catch (final NotCompliantMBeanException e) {
			throw new ManagementException(ManagementException.Code.NOT_COMPLIANT_MBEAN, e, mbean.getClass().getName());
		}
		LOG.debug(objectName.getCanonicalName() + " registered");
	}

	/**
	 * Unregister the MBean identified through specified object name.
	 * 
	 * @param objectName
	 *            The MBean's object name
	 * @throws ManagementException
	 *             If unregistering the MBean fails
	 */
	public void unregisterMBean(final ObjectName objectName) throws ManagementException {
		if (mbs.isRegistered(objectName)) {
			try {
				mbs.unregisterMBean(objectName);
			} catch (final InstanceNotFoundException e) {
				throw new ManagementException(ManagementException.Code.NOT_FOUND, e, objectName);
			} catch (final MBeanRegistrationException e) {
				throw new ManagementException(ManagementException.Code.MBEAN_REGISTRATION, e, objectName);
			}
			LOG.debug(objectName.getCanonicalName() + " unregistered");
		}
	}

	/**
	 * Adds the MBean identified by given object name to {@link GaugeMonitor}
	 * for being observed
	 * 
	 * @param objectName
	 *            The MBean's object name
	 * @param attributeName
	 *            The observed attribute
	 * @throws ManagementException
	 *             If adding the MBean to {@link GaugeMonitor} fails
	 */
	public final void addObservedMBean(final ObjectName objectName, final String attributeName)
			throws ManagementException {
		GaugeMonitor gm = gaugeMonitorRef.get();
		while (gm == null) {
			final GaugeMonitor newgm = new GaugeMonitor();
			newgm.setGranularityPeriod(5000);
			newgm.setDifferenceMode(false);
			newgm.setNotifyHigh(false);
			newgm.setNotifyLow(true);
			newgm.setThresholds(Integer.valueOf(50), Integer.valueOf(5));
			if (gaugeMonitorRef.compareAndSet(gm, newgm)) {
				/*
				 * No other thread initialized gauge monitor in the meantime;
				 * thus register and start it
				 */
				gm = gaugeMonitorRef.get();
				try {
					mbs.registerMBean(gm, new ObjectName("Services:type=GaugeMonitor"));
				} catch (final InstanceAlreadyExistsException e) {
					throw new ManagementException(ManagementException.Code.ALREADY_EXISTS, e, gm.getClass().getName());
				} catch (final MBeanRegistrationException e) {
					throw new ManagementException(ManagementException.Code.MBEAN_REGISTRATION, e, gm.getClass()
							.getName());
				} catch (final NotCompliantMBeanException e) {
					throw new ManagementException(ManagementException.Code.NOT_COMPLIANT_MBEAN, e, gm.getClass()
							.getName());
				} catch (final MalformedObjectNameException e) {
					throw new ManagementException(ManagementException.Code.MALFORMED_OBJECT_NAME, e,
							"Services:type=GaugeMonitor");
				}
				gm.start();
			} else {
				gm = gaugeMonitorRef.get();
			}
		}
		gm.addObservedObject(objectName);
		gm.setObservedAttribute(attributeName);
	}

	/**
	 * Removes the observed MBean identified by specified object name from the
	 * {@link GaugeMonitor}
	 * 
	 * @param objectName
	 *            The MBean's object name
	 * @throws ManagementException
	 *             If removing the MBean from {@link GaugeMonitor} fails
	 */
	public final void removeObservedMBean(final ObjectName objectName) {
		final GaugeMonitor gm = gaugeMonitorRef.get();
		if (gm != null) {
			gm.removeObservedObject(objectName);
		}
	}

	/**
	 * Adds a RMI registry
	 * 
	 * @param port
	 *            The port on which the RMI registry should listen on
	 * @param bindAddr
	 *            The bind address for created sockets by RMI registry
	 * @throws ManagementException
	 *             If RMI registry cannot be created
	 */
	protected final void addRMIRegistry(final int port, final String bindAddr) throws ManagementException {
		Registry registry = null;
		try {
			if (initialized.compareAndSet(false, true)) {
				rmiSocketFactory = new AbstractAgentSocketFactory(0, bindAddr);
			}
			try {
				/*
				 * If following calls succeed, a RMI registry has already been
				 * created that listens on this port
				 */
				registry = LocateRegistry.getRegistry(port);
				registry.list();
			} catch (final RemoteException e) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("No responsive RMI registry found that listens on port " + port
							+ ". A new one is going to be created", e);
				}
				/*
				 * Create a new one
				 */
				registry = LocateRegistry.createRegistry(port, rmiSocketFactory, rmiSocketFactory);
			}
			registries.put(Integer.valueOf(port), registry);
			if (LOG.isInfoEnabled()) {
				LOG.info(new StringBuilder(128).append("RMI registry created on port ").append(port).append(
						" and bind address ").append(bindAddr).toString());
			}
		} catch (final UnknownHostException e) {
			throw new ManagementException(ManagementException.Code.UNKNOWN_HOST_ERROR, e, e.getMessage());
		} catch (final RemoteException e) {
			throw new ManagementException(ManagementException.Code.REMOTE_ERROR, e, e.getMessage());
		}
	}

	/**
	 * Lists the RMI registries ports
	 * 
	 * @return An array of <code>int</code> representing RMI ports
	 */
	public final int[] getRMIRegistryPorts() {
		final int size = registries.size();
		final Iterator<Integer> iter = registries.keySet().iterator();
		final int[] portnumbers = new int[size];
		for (int i = 0; i < size; i++) {
			portnumbers[i] = iter.next().intValue();
		}
		return portnumbers;
	}

	/**
	 * Creates a JMX connector server bound to specified JMX URL.
	 * 
	 * @param urlstr
	 *            The JMX URL as a string
	 * @param jmxLogin
	 *            The JMX login or <code>null</code> to use no authentication
	 *            for connecting to specified JMX URL
	 * @param jmxPassword
	 *            The JMX password (only needed if previous parameter is not
	 *            <code>null</code>)
	 * @return The {@link JMXServiceURL} to which the connector is bound
	 * @throws ManagementException
	 *             If connector cannot be added
	 */
	protected final JMXServiceURL addConnectorServer(final String urlstr, final String jmxLogin,
			final String jmxPassword) throws ManagementException {
		final JMXServiceURL url;
		try {
			url = new JMXServiceURL(urlstr);
		} catch (final MalformedURLException e) {
			throw new ManagementException(ManagementException.Code.MALFORMED_URL, e, urlstr);
		}
		if (connectors.containsKey(url)) {
			throw new ManagementException(ManagementException.Code.JMX_URL_ALREADY_BOUND, urlstr);
		}
		try {
			final Map<String, Object> environment;
			// environment.put(RMIConnectorServer.
			// RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, rmiSocketFactory);
			// environment.put(RMIConnectorServer.
			// RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, rmiSocketFactory);
			if (jmxLogin == null || jmxPassword == null) {
				environment = null;
			} else {
				environment = new HashMap<String, Object>(1);
				environment.put(RMIConnectorServer.AUTHENTICATOR, new AbstractAgentJMXAuthenticator(new String[] {
						jmxLogin, jmxPassword }));
			}
			final JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(url, environment, mbs);
			cs.start();
			connectors.put(url, cs);
			if (LOG.isInfoEnabled()) {
				LOG.info(new StringBuilder("JMX connector server on ").append(urlstr).append(" started"));
			}
			return url;
		} catch (final IOException e) {
			throw new ManagementException(ManagementException.Code.IO_ERROR, e, e.getMessage());
		}
	}

	/**
	 * Removes the JMX connector server bound to specified JMX URL
	 * 
	 * @param url
	 *            The JMX URL
	 */
	protected final void removeConnectorServer(final JMXServiceURL url) {
		final JMXConnectorServer connector = connectors.remove(url);
		if (connector == null) {
			return;
		}
		try {
			connector.stop();
			if (LOG.isInfoEnabled()) {
				LOG.info(new StringBuilder("JMX connector server on ").append(url).append(" stopped"));
			}
		} catch (final IOException e) {
			LOG.error(new StringBuilder("JMX connector server on ").append(url).append(" could not be stopped")
					.toString(), e);
			return;
		}
	}

	/**
	 * Starts the agent
	 */
	public abstract void run();

	/**
	 * Shuts the agent down
	 */
	public abstract void stop();

	/**
	 * Gets a free port at the time of call to this method. The logic leverages
	 * the built in java.net.ServerSocket implementation which binds a server
	 * socket to a free port when instantiated with a port <code> 0 </code>.
	 * <P>
	 * Note that this method guarantees the availability of the port only at the
	 * time of call. The method does not bind to this port.
	 * <p>
	 * Checking for free port can fail for several reasons which may indicate
	 * potential problems with the system. This method acknowledges the fact and
	 * following is the general contract:
	 * <li>Best effort is made to find a port which can be bound to. All the
	 * exceptional conditions in the due course are considered SEVERE.
	 * <li>If any exceptional condition is experienced, <code> 0 </code> is
	 * returned, indicating that the method failed for some reasons and the
	 * callers should take the corrective action. (The method need not always
	 * throw an exception for this).
	 * <li>Method is synchronized on this class.
	 * 
	 * @return integer depicting the free port number available at this time 0
	 *         otherwise.
	 */
	protected static final int getFreePort() {
		int freePort = 0;
		boolean portFound = false;
		ServerSocket serverSocket = null;
		synchronized (AbstractAgent.class) {
			try {
				/*
				 * following call normally returns the free port, to which the
				 * ServerSocket is bound.
				 */
				serverSocket = new ServerSocket(0);
				freePort = serverSocket.getLocalPort();
				portFound = true;
			} catch (final Exception e) {
				/*
				 * Squelch the exception
				 */
				LOG.error(e.getLocalizedMessage(), e);
			} finally {
				if (!portFound) {
					freePort = 0;
				}
				try {
					if (serverSocket != null) {
						serverSocket.close();
						if (!serverSocket.isClosed()) {
							throw new Exception("local exception ...");
						}
					}
				} catch (final Exception e) {
					/*
					 * Squelch the exception
					 */
					freePort = 0;
				}
			}
			return freePort;
		}
	}

}
