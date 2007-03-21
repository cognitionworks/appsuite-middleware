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

package com.openexchange.cache;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jcs.auxiliary.remote.behavior.IRemoteCacheAttributes;
import org.apache.jcs.auxiliary.remote.behavior.IRemoteCacheListener;
import org.apache.jcs.auxiliary.remote.behavior.IRemoteCacheObserver;
import org.apache.jcs.auxiliary.remote.behavior.IRemoteCacheService;
import org.apache.jcs.auxiliary.remote.behavior.IRemoteCacheServiceAdmin;
import org.apache.jcs.auxiliary.remote.server.behavior.IRemoteCacheServerAttributes;
import org.apache.jcs.engine.CacheEventQueueFactory;
import org.apache.jcs.engine.CacheListeners;
import org.apache.jcs.engine.behavior.ICacheElement;
import org.apache.jcs.engine.behavior.ICacheEventQueue;
import org.apache.jcs.engine.behavior.ICacheListener;
import org.apache.jcs.engine.control.CompositeCache;
import org.apache.jcs.engine.control.CompositeCacheManager;

/**
 * This class provides remote cache services. The remote cache server propagates
 * events from local caches to other local caches. It can also store cached
 * data, making it available to new clients.
 * <p>
 * Remote cache servers can be clustered. If the cache used by this remote cache
 * is configured to use a remote cache of type cluster, the two remote caches
 * will communicate with each other. Remote and put requests can be sent from
 * one remote to another. If they are configured to broadcast such event to
 * their client, then remove an puts can be sent to all locals in the cluster.
 * <p>
 * Get requests are made between clustered servers if AllowClusterGet is true.
 * You can setup several clients to use one remote server and several to use
 * another. The get locad will be distributed between the two servers. Since
 * caches are usually high get and low put, this should allow you to scale.
 */
class RemoteCacheServer extends UnicastRemoteObject implements IRemoteCacheService, IRemoteCacheObserver,
		IRemoteCacheServiceAdmin, Unreferenced {

	private static final long serialVersionUID = -6598502101970048656L;

	private final static transient Log log = LogFactory.getLog(RemoteCacheServer.class);

	/** timing -- if we should record operation times. */
	protected final static boolean timing = true;

	private int puts;

	// Maps cache name to CacheListeners object.
	// association of listeners (regions).
	private final Map<String, CacheListeners> cacheListenersMap = new HashMap<String, CacheListeners>();

	private final Map<String, CacheListeners> clusterListenersMap = new HashMap<String, CacheListeners>();

	private CompositeCacheManager cacheManager;

	// relates listener id with a type
	private final Map<Long, Integer> idTypeMap = new HashMap<Long, Integer>();

	// private transient int listenerId = 0;
	private int[] listenerId = new int[1];

	/** Configuration settings. */
	protected IRemoteCacheServerAttributes rcsa;

	/** The interval at which we will log updates. */
	private static final int logInterval = 100;

	/**
	 * Constructor for the RemoteCacheServer object. Thiks initializes the
	 * server with the values from the config file.
	 * <p>
	 * 
	 * @param rcsa
	 * @throws RemoteException
	 * @exception IOException
	 */
	RemoteCacheServer(IRemoteCacheServerAttributes rcsa) throws RemoteException {
		super(rcsa.getServicePort());
		this.rcsa = rcsa;
		final Properties prop = new Properties();
		try {
			FileInputStream fis = null;
			try {
				prop.load((fis = new FileInputStream(rcsa.getConfigFileName())));
			} finally {
				if (fis != null) {
					fis.close();
					fis = null;
				}
			}
		} catch (Exception e) {
			log.error("Could not load properties from file " + rcsa.getConfigFileName(), e);
		}
		init(prop);
	}

	/**
	 * Initialize the RMI Cache Server from a properties file.
	 * <p>
	 * 
	 * @param prop
	 */
	private void init(final Properties prop) {
		cacheManager = createCacheManager(prop);

		// cacheManager would have created a number of ICache objects.
		// Use these objects to set up the cacheListenersMap.
		final String[] list = cacheManager.getCacheNames();
		for (int i = 0; i < list.length; i++) {
			final String name = list[i];
			cacheListenersMap.put(name, new CacheListeners(cacheManager.getCache(name)));
		}
	}

	/**
	 * Subclass can override this method to create the specific cache manager.
	 * <p>
	 * 
	 * @param prop
	 *            The anem of the configuration file.
	 * @return The cache hub configured with this configuration file.
	 */
	private CompositeCacheManager createCacheManager(final Properties prop) {
		final CompositeCacheManager hub = CompositeCacheManager.getUnconfiguredInstance();

		if (prop == null) {
			hub.configure("/remote.cache.ccf");
		} else {
			hub.configure(prop);
		}
		return hub;
	}

	/**
	 * Returns the cache lsitener for the specified cache. Creates the cache and
	 * the cache descriptor if they do not already exist.
	 * <p>
	 * 
	 * @param cacheName
	 * @return The cacheListeners value
	 */
	private CacheListeners getCacheListeners(final String cacheName) {
		CacheListeners cacheListeners = cacheListenersMap.get(cacheName);
		synchronized (cacheListenersMap) {
			if (cacheListeners == null) {
				cacheListeners = cacheListenersMap.get(cacheName);
				if (cacheListeners == null) {
					final Properties props = new Properties();
					try {
						props.load(new FileInputStream(System.getProperty("cache.ccf")));
					} catch (Exception e) {
						log.error("Problem loading property file " + System.getProperty("cache.ccf"), e);
					}
					cacheManager.configure(props);
					cacheListeners = new CacheListeners(cacheManager.getCache(cacheName));
					cacheListenersMap.put(cacheName, cacheListeners);
				}
			}
		}
		return cacheListeners;
	}

	/**
	 * Gets the clusterListeners attribute of the RemoteCacheServer object.
	 * <p>
	 * 
	 * @todo may be able to remove this
	 * 
	 * @param cacheName
	 * @return The clusterListeners value
	 */
	private CacheListeners getClusterListeners(final String cacheName) {
		CacheListeners cacheListeners = clusterListenersMap.get(cacheName);
		synchronized (clusterListenersMap) {
			if (cacheListeners == null) {
				cacheListeners = clusterListenersMap.get(cacheName);
				if (cacheListeners == null) {
					cacheListeners = new CacheListeners(cacheManager.getCache(cacheName));
					clusterListenersMap.put(cacheName, cacheListeners);
				}
			}
		}
		return cacheListeners;
	}

	/**
	 * Puts a cache bean to the remote cache and notifies all listeners which
	 * <br>
	 * <ol>
	 * <li>have a different listener id than the originating host;
	 * <li>are currently subscribed to the related cache.
	 * </ol>
	 * <p>
	 * 
	 * @param item
	 * @throws IOException
	 * 
	 */
	public void put(final ICacheElement item) {
		update(item);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.jcs.engine.behavior.ICacheService#update(org.apache.jcs.engine.behavior.ICacheElement)
	 */
	public void update(final ICacheElement item) {
		update(item, 0);
	}

	/**
	 * An update can come from either a local cache's remote auxiliary, or it
	 * can come from a remote server. A remote server is considered a a source
	 * of type cluster.
	 * <p>
	 * If the update came from a cluster, then we should tell the cache manager
	 * that this was a remote put. This way, any lateral and remote auxiliaries
	 * configured for the region will not be updated. This is basically how a
	 * remote listener works when plugged into a local cache.
	 * <p>
	 * If the cluster is configured to keep local cluster consistency, then all
	 * listeners will be updated. This allows cluster server A to update cluster
	 * server B and then B to update its clients if it is told to keep local
	 * cluster consistency. Otherwise, server A will update server B and B will
	 * not tell its clients. If you cluster using lateral caches for instance,
	 * this is how it will work. Updates to a cluster node, will never get to
	 * the leavess. The remote cluster, with local cluster consistency, allows
	 * you to update leaves. This basically allows you to have a failover remote
	 * server.
	 * <p>
	 * Since currently a cluster will not try to get from other cluster servers,
	 * you can scale a bit with a cluster configuration. Puts and removes will
	 * be broadcasted to all clients, but the get load on a remote server can be
	 * reduced.
	 * <p>
	 * 
	 * @param item
	 * @param requesterId
	 * @throws IOException
	 */
	public void update(final ICacheElement item, final long requesterId) {

		long start = 0;
		if (timing) {
			start = System.currentTimeMillis();
		}

		if (log.isInfoEnabled()) {
			// not thread safe, but it doesn't have to be accurate
			puts++;
			if (puts % logInterval == 0) {
				log.info("puts = " + puts);
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("In update, put " + item.getKey() + " in " + item.getCacheName());
		}

		try {
			final CacheListeners cacheDesc = getCacheListeners(item.getCacheName());
			/* Object val = */item.getVal();

			final Integer remoteTypeL = idTypeMap.get(Long.valueOf(requesterId));
			if (log.isDebugEnabled()) {
				log.debug("In update, requesterId = [" + requesterId + "] remoteType = " + remoteTypeL);
			}

			boolean fromCluster = false;
			if (remoteTypeL != null && remoteTypeL.intValue() == IRemoteCacheAttributes.CLUSTER) {
				fromCluster = true;
			}
			// ordered cache item update and notification.
			synchronized (cacheDesc) {
				try {
					final CompositeCache c = (CompositeCache) cacheDesc.cache;

					// If the source of this request was not from a cluster,
					// then consider it a local update. The cache manager will
					// try to
					// update all auxiliaries.
					//
					// This requires that two local caches not be connected to
					// two clustered remote caches. The failover runner will
					// have to make sure of this. ALos, the local cache needs
					// avoid updating this source. Will need to pass the source
					// id somehow. The remote cache should udate all local
					// caches
					// but not update the cluster source. Cluster remote caches
					// should only be updated by the server and not the
					// RemoteCache.
					if (fromCluster) {
						if (log.isDebugEnabled()) {
							log.debug("Put FROM cluster, NOT updating other auxiliaries for region. "
									+ " requesterId [" + requesterId + ']');
						}
						c.localUpdate(item);
					} else {
						if (log.isDebugEnabled()) {
							log.debug("Put NOT from cluster, updating other auxiliaries for region. "
									+ " requesterId [" + requesterId + ']');
						}
						c.update(item);
					}
				} catch (IOException ce) {
					// swallow
					if (log.isInfoEnabled()) {
						log
								.info("Exception caught updating item. requesterId [" + requesterId + "] "
										+ ce.getMessage());
					}
				}

				// UPDATE LOCALS IF A REQUEST COMES FROM A CLUSTER
				// IF LOCAL CLUSTER CONSISTENCY IS CONFIGURED
				if (!fromCluster || (fromCluster && rcsa.getLocalClusterConsistency())) {
					final ICacheEventQueue[] qlist = getEventQList(cacheDesc, requesterId);

					if (qlist != null) {
						if (log.isDebugEnabled()) {
							log.debug("qlist.length = " + qlist.length);
						}
						for (int i = 0; i < qlist.length; i++) {
							qlist[i].addPutEvent(item);
						}
					} else {
						if (log.isDebugEnabled()) {
							log.debug("q list is null");
						}
					}
				}
			}
		} catch (IOException e) {
			log.error("Trouble in Update. requesterId [" + requesterId + ']', e);
		}

		// TODO use JAMON for timing
		if (timing) {
			final long end = System.currentTimeMillis();
			if (log.isDebugEnabled()) {
				log.debug("put took " + String.valueOf(end - start) + " ms.");
			}
		}
	}

	/**
	 * Gets the eventQList attribute of the RemoteCacheServer object. This
	 * returns the event queues stored in the cacheListeners object for a
	 * particuylar region, if the queue is not for this requester.
	 * <p>
	 * Basically, this makes sure that a request from a particular local cache,
	 * identified by its listener id, does not result in a call to that same
	 * listener.
	 * <p>
	 * 
	 * @param cacheListeners
	 * @param requesterId
	 * @return The eventQList value
	 */
	private ICacheEventQueue[] getEventQList(final CacheListeners cacheListeners, final long requesterId) {
		ICacheEventQueue[] list = null;
		synchronized (cacheListeners.eventQMap) {
			list = (ICacheEventQueue[]) cacheListeners.eventQMap.values().toArray(new ICacheEventQueue[0]);
		}
		int count = 0;
		// Set those not qualified to null; Count those qualified.
		for (int i = 0; i < list.length; i++) {
			final ICacheEventQueue q = list[i];
			if (q.isWorking() && q.getListenerId() != requesterId) {
				count++;
			} else {
				list[i] = null;
			}
		}
		if (count == list.length) {
			// All qualified.
			return list;
		}

		// Returns only the qualified.
		ICacheEventQueue[] qq = new ICacheEventQueue[count];
		count = 0;
		for (int i = 0; i < list.length; i++) {
			if (list[i] != null) {
				qq[count++] = list[i];
			}
		}
		return qq;
	}

	/**
	 * Returns a cache value from the specified remote cache; or null if the
	 * cache or key does not exist.
	 * <p>
	 * 
	 * @param cacheName
	 * @param key
	 * @return ICacheElement
	 * @throws IOException
	 */
	public ICacheElement get(final String cacheName, final Serializable key) {
		return this.get(cacheName, key, 0);
	}

	/**
	 * Returns a cache bean from the specified cache; or null if the key does
	 * not exist.
	 * <p>
	 * Adding the requestor id, allows the cache to determine the sournce of the
	 * get.
	 * <p>
	 * 
	 * @param cacheName
	 * @param key
	 * @param requesterId
	 * @return ICacheElement
	 * @throws IOException
	 */
	public ICacheElement get(final String cacheName, final Serializable key, final long requesterId) {
		final Integer remoteTypeL = idTypeMap.get(Long.valueOf(requesterId));

		if (log.isDebugEnabled()) {
			log.debug("get " + key + " from cache " + cacheName + " requesterId = [" + requesterId + "] remoteType = "
					+ remoteTypeL);
		}

		// Since a non-receiving remote cache client will not register a
		// listener, it will not have a listener id assigned from the server. As
		// such the remote server cannot determine if it is a cluster or a
		// normal client. It will assume that it is a normal client.

		boolean fromCluster = false;
		if (remoteTypeL != null && remoteTypeL.intValue() == IRemoteCacheAttributes.CLUSTER) {
			fromCluster = true;
		}

		CacheListeners cacheDesc = null;
		try {
			cacheDesc = getCacheListeners(cacheName);
		} catch (Exception e) {
			log.error("Problem getting listeners.", e);
		}

		if (cacheDesc == null) {
			return null;
		}
		final CompositeCache c = (CompositeCache) cacheDesc.cache;

		ICacheElement element = null;

		// If we have a get come in from a client and we don't have the item
		// locally, we will allow the cache to look in other non local sources,
		// such as a remote cache or a lateral.
		//
		// Since remote servers never get from clients and clients never go
		// remote from a remote call, this
		// will not result in any loops.
		//
		// This is the only instance I can think of where we allow a remote get
		// from a remote call. The purpose is to allow remote cache servers to
		// talk to each other. If one goes down, you want it to be able to get
		// data from those that were up when the failed server comes back o
		// line.

		if (!fromCluster && this.rcsa.getAllowClusterGet()) {
			if (log.isDebugEnabled()) {
				log.debug("NonLocalGet. fromCluster [" + fromCluster + "] AllowClusterGet ["
						+ this.rcsa.getAllowClusterGet() + ']');
			}
			element = c.get(key);
		} else {
			// Gets from cluster type remote will end up here.
			// Gets from all clients will end up here if allow cluster get is
			// false.

			if (log.isDebugEnabled()) {
				log.debug("LocalGet.  fromCluster [" + fromCluster + "] AllowClusterGet ["
						+ this.rcsa.getAllowClusterGet() + ']');
			}
			element = c.localGet(key);
		}

		return element;
	}

	/**
	 * Gets the set of keys of objects currently in the group.
	 * <p>
	 * 
	 * @param cacheName
	 * @param group
	 * @return A Set of group keys
	 */
	public Set getGroupKeys(final String cacheName, final String group) {
		CacheListeners cacheDesc = null;
		try {
			cacheDesc = getCacheListeners(cacheName);
		} catch (Exception e) {
			log.error("Problem getting listeners.", e);
		}

		if (cacheDesc == null) {
			return Collections.EMPTY_SET;
		}
		final CompositeCache c = (CompositeCache) cacheDesc.cache;
		return c.getGroupKeys(group);
	}

	/**
	 * Removes the given key from the specified remote cache. Defaults the
	 * listener id to 0.
	 * <p>
	 * 
	 * @param cacheName
	 * @param key
	 * @throws IOException
	 */
	public void remove(final String cacheName, final Serializable key) throws IOException {
		remove(cacheName, key, 0);
	}

	/**
	 * Remove the key from the cache region and don't tell the source listener
	 * about it.
	 * <p>
	 * 
	 * @param cacheName
	 * @param key
	 * @param requesterId
	 * @throws IOException
	 */
	public void remove(final String cacheName, final Serializable key, final long requesterId) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("remove " + key + " from cache " + cacheName);
		}
		final CacheListeners cacheDesc = cacheListenersMap.get(cacheName);

		final Integer remoteTypeL = idTypeMap.get(Long.valueOf(requesterId));
		boolean fromCluster = false;
		if (remoteTypeL != null && remoteTypeL.intValue() == IRemoteCacheAttributes.CLUSTER) {
			fromCluster = true;
		}

		if (cacheDesc != null) {
			// best attempt to achieve ordered cache item removal and
			// notification.
			synchronized (cacheDesc) {

				boolean removeSuccess = false;

				// No need to notify if it was not cached.
				final CompositeCache c = (CompositeCache) cacheDesc.cache;

				if (fromCluster) {
					if (log.isDebugEnabled()) {
						log.debug("Remove FROM cluster, NOT updating other auxiliaries for region");
					}
					removeSuccess = c.localRemove(key);
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Remove NOT from cluster, updating other auxiliaries for region");
					}
					removeSuccess = c.remove(key);
				}

				if (log.isDebugEnabled()) {
					log.debug("remove " + key + " from cache " + cacheName + " success (was it found) = "
							+ removeSuccess);
				}

				// UPDATE LOCALS IF A REQUEST COMES FROM A CLUSTER
				// IF LOCAL CLUSTER CONSISTENCY IS CONFIGURED
				if (!fromCluster || (fromCluster && rcsa.getLocalClusterConsistency())) {
					final ICacheEventQueue[] qlist = getEventQList(cacheDesc, requesterId);

					for (int i = 0; i < qlist.length; i++) {
						qlist[i].addRemoveEvent(key);
					}
				}
			}
		}
	}

	/**
	 * Remove all keys from the sepcified remote cache.
	 * <p>
	 * 
	 * @param cacheName
	 * @throws IOException
	 */
	public void removeAll(final String cacheName) throws IOException {
		removeAll(cacheName, 0);
	}

	/**
	 * Remove all keys from the sepcified remote cache.
	 * <p>
	 * 
	 * @param cacheName
	 * @param requesterId
	 * @throws IOException
	 */
	public void removeAll(final String cacheName, final long requesterId) throws IOException {
		final CacheListeners cacheDesc = cacheListenersMap.get(cacheName);

		Integer remoteTypeL = idTypeMap.get(Long.valueOf(requesterId));
		boolean fromCluster = false;
		if (remoteTypeL != null && remoteTypeL.intValue() == IRemoteCacheAttributes.CLUSTER) {
			fromCluster = true;
		}

		if (cacheDesc != null) {
			// best attempt to achieve ordered cache item removal and
			// notification.
			synchronized (cacheDesc) {
				// No need to broadcast, or notify if it was not cached.
				final CompositeCache c = (CompositeCache) cacheDesc.cache;

				if (fromCluster) {
					if (log.isDebugEnabled()) {
						log.debug("RemoveALL FROM cluster, NOT updating other auxiliaries for region");
					}
					c.localRemoveAll();
				} else {
					if (log.isDebugEnabled()) {
						log.debug("RemoveALL NOT from cluster, updating other auxiliaries for region");
					}
					c.removeAll();
				}

				// update registered listeners
				if (!fromCluster || (fromCluster && rcsa.getLocalClusterConsistency())) {

					final ICacheEventQueue[] qlist = getEventQList(cacheDesc, requesterId);

					for (int i = 0; i < qlist.length; i++) {
						qlist[i].addRemoveAllEvent();
					}
				}
			}
		}
	}

	/**
	 * Frees the specified remote cache.
	 * <p>
	 * 
	 * @param cacheName
	 * @throws IOException
	 */
	public void dispose(final String cacheName) throws IOException {
		dispose(cacheName, 0);
	}

	/**
	 * Frees the specified remote cache.
	 * <p>
	 * 
	 * @param cacheName
	 * @param requesterId
	 * @throws IOException
	 */
	public void dispose(final String cacheName, final long requesterId) throws IOException {
		if (log.isInfoEnabled()) {
			log.info("Dispose request received from listener [" + requesterId + ']');
		}

		final CacheListeners cacheDesc = cacheListenersMap.get(cacheName);

		// this is dangerous
		if (cacheDesc != null) {
			// best attempt to achieve ordered free-cache-op and notification.
			synchronized (cacheDesc) {
				final ICacheEventQueue[] qlist = getEventQList(cacheDesc, requesterId);

				for (int i = 0; i < qlist.length; i++) {
					qlist[i].addDisposeEvent();
				}
				cacheManager.freeCache(cacheName);
			}
		}
	}

	/**
	 * Frees all remote caches.
	 * <p>
	 * 
	 * @throws IOException
	 */
	public void release() throws IOException {
		synchronized (cacheListenersMap) {
			for (final Iterator<CacheListeners> iter = cacheListenersMap.values().iterator(); iter.hasNext();) {
				final CacheListeners cacheDesc = iter.next();
				final ICacheEventQueue[] qlist = getEventQList(cacheDesc, 0);

				for (int i = 0; i < qlist.length; i++) {
					qlist[i].addDisposeEvent();
				}
			}
			cacheManager.release();
		}
	}

	/**
	 * Removes dead event queues. Should clean out deregistered listeners.
	 * <p>
	 * 
	 * @param eventQMap
	 */
	private static void cleanupEventQMap(final Map eventQMap) {
		synchronized (eventQMap) {
			for (final Iterator itr = eventQMap.entrySet().iterator(); itr.hasNext();) {
				final Map.Entry e = (Map.Entry) itr.next();
				final ICacheEventQueue q = (ICacheEventQueue) e.getValue();

				// this does not care if the q is alive (i.e. if
				// there are active threads; it cares if the queue
				// is working -- if it has not encoutnered errors
				// above the failure threshhold
				if (!q.isWorking()) {
					itr.remove();
					log.warn("Cache event queue " + q + " is not working and removed from cache server.");
				}
			}
		}
	}

	/**
	 * Subscribes to the specified remote cache.
	 * <p>
	 * If the client id is 0, then the remote cache server will increment it's
	 * local count and assign an id to the client.
	 * <p>
	 * 
	 * @param cacheName
	 *            the specified remote cache.
	 * @param listener
	 *            object to notify for cache changes. must be synchronized since
	 *            there are remote calls involved.
	 * @throws IOException
	 */
	public void addCacheListener(final String cacheName, final ICacheListener listener) throws IOException {
		if (cacheName == null || listener == null) {
			throw new IllegalArgumentException("cacheName and listener must not be null");
		}
		CacheListeners cacheDesc;

		final IRemoteCacheListener ircl = (IRemoteCacheListener) listener;

		final String listenerAddress = ircl.getLocalHostAddress();

		final int remoteType = ircl.getRemoteType();
		if (remoteType == IRemoteCacheAttributes.CLUSTER) {
			log.debug("adding cluster listener, listenerAddress [" + listenerAddress + ']');
			cacheDesc = getClusterListeners(cacheName);
		} else {
			log.debug("adding normal listener, listenerAddress [" + listenerAddress + ']');
			cacheDesc = getCacheListeners(cacheName);
		}
		final Map eventQMap = cacheDesc.eventQMap;
		cleanupEventQMap(eventQMap);

		// synchronized ( listenerId )
		synchronized (ICacheListener.class) {
			long id = 0;
			try {
				id = listener.getListenerId();
				// clients problably shouldn't do this.
				if (id == 0) {
					// must start at one so the next gets recognized
					final long listenerIdB = nextListenerId();
					if (log.isDebugEnabled()) {
						log.debug("listener id=" + (listenerIdB & 0xff) + " addded for cache [" + cacheName
								+ "], listenerAddress [" + listenerAddress + ']');
					}
					listener.setListenerId(listenerIdB);
					id = listenerIdB;

					// in case it needs synchronization
					if (log.isInfoEnabled()) {
						log.info("adding vm listener under new id = [" + listenerIdB + "], listenerAddress ["
								+ listenerAddress + ']');
					}
				} else {
					if (log.isInfoEnabled()) {
						log.info("adding listener under existing id = [" + id + "], listenerAddress ["
								+ listenerAddress + ']');
					}
					// should confirm the the host is the same as we have on
					// record, just in case a client has made a mistake.
				}

				// relate the type to an id
				this.idTypeMap.put(Long.valueOf(id), Integer.valueOf(remoteType));
			} catch (IOException ioe) {
				log.error("Problem setting listener id, listenerAddress [" + listenerAddress + ']', ioe);
			}

			final CacheEventQueueFactory fact = new CacheEventQueueFactory();
			final ICacheEventQueue q = fact.createCacheEventQueue(listener, id, cacheName, rcsa.getEventQueuePoolName(), rcsa
					.getEventQueueTypeFactoryCode());

			eventQMap.put(Long.valueOf(listener.getListenerId()), q);

			if (log.isInfoEnabled()) {
				log.info("Region " + cacheName + "'s listener size = " + cacheDesc.eventQMap.size());
			}
		}
	}

	/**
	 * Subscribes to all remote caches.
	 * <p>
	 * 
	 * @param listener
	 *            The feature to be added to the CacheListener attribute
	 * @throws IOException
	 */
	public void addCacheListener(final ICacheListener listener) throws IOException {
		for (final Iterator<String> iter = cacheListenersMap.keySet().iterator(); iter.hasNext();) {
			final String cacheName = iter.next();
			addCacheListener(cacheName, listener);

			if (log.isDebugEnabled()) {
				log.debug("Adding listener for cache [" + cacheName + ']');
			}
		}
	}

	/**
	 * Unsibscribe this listener from this region. If the listener is
	 * registered, it will be removed from the event queue map list.
	 * <p>
	 * 
	 * @param cacheName
	 * @param listenerId
	 */
	public void removeCacheListener(final String cacheName, final ICacheListener listener) throws IOException {
		removeCacheListener(cacheName, listener.getListenerId());
	}

	/**
	 * Unsibscribe this listener from this region. If the listener is
	 * registered, it will be removed from the event queue map list.
	 * <p>
	 * 
	 * @param cacheName
	 * @param listenerId
	 */
	public void removeCacheListener(final String cacheName, final long listenerId) {
		if (log.isInfoEnabled()) {
			log.info("Removing listener for cache region = [" + cacheName + "] and listenerId [" + listenerId + ']');
		}

		final CacheListeners cacheDesc = getCacheListeners(cacheName);
		final Map eventQMap = cacheDesc.eventQMap;
		cleanupEventQMap(eventQMap);
		final ICacheEventQueue q = (ICacheEventQueue) eventQMap.remove(Long.valueOf(listenerId));

		if (q != null) {
			if (log.isDebugEnabled()) {
				log.debug("Found queue for cache region = [" + cacheName + "] and listenerId  [" + listenerId + ']');
			}
			q.destroy();
			cleanupEventQMap(eventQMap);
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Did not find queue for cache region = [" + cacheName + "] and listenerId [" + listenerId
						+ ']');
			}
		}

		if (log.isInfoEnabled()) {
			log.info("After removing listener [" + listenerId + "] cache region " + cacheName + "'s listener size ["
					+ cacheDesc.eventQMap.size() + ']');
		}
	}

	/**
	 * Unsubscribes from all remote caches.
	 * <p>
	 * 
	 * @param listener
	 * @throws IOException
	 */
	public void removeCacheListener(final ICacheListener listener) throws IOException {
		for (final Iterator<String> iter = cacheListenersMap.keySet().iterator(); iter.hasNext();) {
			final String cacheName = iter.next();
			removeCacheListener(cacheName, listener);

			if (log.isInfoEnabled()) {
				log.info("Removing listener for cache [" + cacheName + ']');
			}
		}
	}

	/**
	 * Shuts down the remote server.
	 * <p>
	 * 
	 * @throws IOException
	 */
	public void shutdown() {
		//TODO: RemoteCacheServerFactory.shutdownImpl("", Registry.REGISTRY_PORT);
	}

	/**
	 * Shuts down a server at a particular host and port. Then it calls shutdown
	 * on the cache itself.
	 * <p>
	 * 
	 * @param host
	 * @param port
	 * @throws IOException
	 */
	public void shutdown(final String host, final int port) {
		if (log.isInfoEnabled()) {
			log.info("Received shutdown request.  Shutting down server.");
		}
		// TODO: RemoteCacheServerFactory.shutdownImpl(host, port);
		this.cacheManager.shutDown();
	}

	/**
	 * Called by the RMI runtime sometime after the runtime determines that the
	 * reference list, the list of clients referencing the remote object,
	 * becomes empty.
	 */
	// TODO: test out the DGC.
	public void unreferenced() {
		if (log.isInfoEnabled()) {
			log.info("*** Server now unreferenced and subject to GC. ***");
		}
	}

	/**
	 * Returns the next generated listener id [0,255].
	 * <p>
	 * 
	 * @return the listener id of a client. This should be unique for this
	 *         server.
	 */
	private long nextListenerId() {
		long id = 0;
		if (listenerId[0] == Long.MAX_VALUE) {
			synchronized (listenerId) {
				id = listenerId[0];
				listenerId[0] = 0;
				// TODO: record & check if the generated id is currently being
				// used by a valid listener. Currently if the id wraps after
				// Long.MAX_VALUE,
				// we just assume it won't collide with an existing listener who
				// is live.
			}
		} else {
			synchronized (listenerId) {
				id = ++listenerId[0];
			}
		}
		return id;
	}

	/**
	 * Gets the stats attribute of the RemoteCacheServer object.
	 * <p>
	 * 
	 * @return The stats value
	 * @throws IOException
	 */
	public String getStats() {
		return cacheManager.getStats();
	}
}
