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

package com.openexchange.groupware.contexts.impl;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openexchange.cache.dynamic.impl.OXObjectFactory;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheException;
import com.openexchange.caching.CacheService;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.contexts.impl.ContextException.Code;
import com.openexchange.groupware.update.Updater;
import com.openexchange.server.services.ServerServiceRegistry;

/**
 * This class implements a caching for the context storage. It provides a proxy
 * implementation for the Context interface to the outside world to be able to
 * keep the referenced context data up-to-date.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class CachingContextStorage extends ContextStorage {

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog(CachingContextStorage.class);
   
    private static final String REGION_NAME = "Context";

    /**
     * Boolean flag for started status
     */
    private boolean started;

    /**
     * Lock for the cache.
     */
    private final Lock cacheLock;

    /**
     * Implementation of the context storage that does persistent storing.
     */
    private final ContextStorage persistantImpl;

    /**
     * Default constructor.
     * @param persistantImpl implementation of the ContextStorage that does
     * Persistent storing.
     */
    public CachingContextStorage(final ContextStorage persistantImpl) {
        super();
        cacheLock = new ReentrantLock();
        this.persistantImpl = persistantImpl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getContextId(final String loginInfo) throws ContextException {
    	final CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
    	if (null == cacheService) {
    		return persistantImpl.getContextId(loginInfo);
    	}
    	try {
			final Cache cache = cacheService.getCache(REGION_NAME);
			Integer contextId = (Integer) cache.get(loginInfo);
			if (null == contextId) {
				if (LOG.isTraceEnabled()) {
					LOG.trace("Cache MISS. Login info: " + loginInfo);
				}
				contextId = Integer.valueOf(persistantImpl.getContextId(loginInfo));
				if (NOT_FOUND != contextId.intValue()) {
					try {
						cache.put(loginInfo, contextId);
					} catch (final CacheException e) {
						throw new ContextException(Code.CACHE_PUT, e);
					}
				}
			} else if (LOG.isTraceEnabled()) {
				LOG.trace("Cache HIT. Login info: " + loginInfo);
			}
			return contextId.intValue();
		} catch (final CacheException e) {
			throw new ContextException(e);
		}
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public ContextExtended loadContext(final int contextId) throws ContextException {
		final CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
		if (cacheService == null) {
			return persistantImpl.loadContext(contextId);
		}
		final OXObjectFactory<ContextExtended> factory = new OXObjectFactory<ContextExtended>() {
			public Serializable getKey() {
				return Integer.valueOf(contextId);
			}

			public ContextExtended load() throws AbstractOXException {
				final ContextExtended retval = persistantImpl.loadContext(contextId);
				final Updater updater = Updater.getInstance();
				if (updater.isLocked(retval)) {
					retval.setUpdating(true);
				} else if (updater.toUpdate(retval)) {
					updater.startUpdate(retval);
					retval.setUpdating(true);
				}
				return retval;
			}

			public Lock getCacheLock() {
				return cacheLock;
			}
		};
		try {
			return new ContextReloader(factory, REGION_NAME);
		} catch (final AbstractOXException e) {
			if (e instanceof ContextException) {
				throw (ContextException) e;
			}
			throw new ContextException(e);
		}
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> getAllContextIds() throws ContextException {
        return persistantImpl.getAllContextIds();
    }

    @Override
	protected void startUp() throws ContextException {
        if (started) {
            LOG.error("Duplicate initialization of CachingContextStorage.");
            return;
        }
        persistantImpl.startUp();
        started = true;
    }

    @Override
    protected void shutDown() throws ContextException {
        if (!started) {
            LOG.error("Duplicate shutdown of CachingContextStorage.");
            return;
        }
        final CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
        if (cacheService != null) {
            try {
                cacheService.freeCache(REGION_NAME);
            } catch (final CacheException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        persistantImpl.shutDown();
        started = false;
    }
    
    /**
     * {@inheritDoc}
     */
	@Override
	public void invalidateContext(final int contextId) throws ContextException {
		final CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
		if (cacheService == null) {
			// Cache not initialized.
			return;
		}
		try {
			final Cache cache = cacheService.getCache(REGION_NAME);
			cacheLock.lock();
			try {
				cache.remove(Integer.valueOf(contextId));
			} catch (final CacheException e) {
				throw new ContextException(ContextException.Code.CACHE_REMOVE, e, String.valueOf(contextId));
			} finally {
				cacheLock.unlock();
			}
		} catch (final CacheException e) {
			throw new ContextException(e);
		}
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidateLoginInfo(final String loginContextInfo) throws ContextException {
		final CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
		if (null == cacheService) {
			// Cache not initialized.
			return;
		}
		try {
			final Cache cache = cacheService.getCache(REGION_NAME);
			cacheLock.lock();
			try {
				cache.remove(loginContextInfo);
			} catch (final CacheException e) {
				throw new ContextException(ContextException.Code.CACHE_REMOVE, e, loginContextInfo);
			} finally {
				cacheLock.unlock();
			}
		} catch (final CacheException e) {
			throw new ContextException(e);
		}
	}
}
