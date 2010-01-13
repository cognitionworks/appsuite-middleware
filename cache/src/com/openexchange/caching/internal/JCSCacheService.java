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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.caching.internal;

import java.io.InputStream;
import java.io.Serializable;
import org.apache.jcs.JCS;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheException;
import com.openexchange.caching.CacheKey;
import com.openexchange.caching.CacheService;

/**
 * {@link JCSCacheService} - Cache service implementation through JCS cache.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class JCSCacheService implements CacheService {

    private static final JCSCacheService SINGLETON = new JCSCacheService();

    /**
     * Gets the singleton instance of JCS cache service
     * 
     * @return The singleton instance of JCS cache service
     */
    public static JCSCacheService getInstance() {
        return SINGLETON;
    }

    /**
     * Initializes a new {@link JCSCacheService}
     */
    private JCSCacheService() {
        super();
    }

    public void freeCache(final String name) {
        if (JCSCacheServiceInit.getInstance().isDefaultCacheRegion(name)) {
            // No freeing of a default cache, this is done on bundle stop
            return;
        }
        JCSCacheServiceInit.getInstance().freeCache(name);
        /*-
         * try {
        	final Cache c = getCache(name);
        	if (null != c) {
        		c.dispose();
        	}
        } catch (final CacheException e) {
        	LOG.error(e.getMessage(), e);
        }
         */
    }

    public Cache getCache(final String name) throws CacheException {
        try {
            /*
             * The JCS cache manager already tracks initialized caches though the same region name always points to the same cache
             */
            return new JCSCache(JCS.getInstance(name));
        } catch (final org.apache.jcs.access.exception.CacheException e) {
            throw new CacheException(CacheException.Code.CACHE_ERROR, e, e.getMessage());
        } catch (final NullPointerException npe) {
            /*
             * Can't use JCS without a configuration file or to be more precise a configuration file which lacks a region of the specified
             * name. It should fail more gracefully, but that's a minor concern in the eyes of JCS developer.
             */
            throw new CacheException(CacheException.Code.MISSING_CACHE_REGION, npe, name);
        }
    }

    public void loadConfiguration(final String cacheConfigFile) throws CacheException {
        JCSCacheServiceInit.getInstance().loadConfiguration(cacheConfigFile);
    }

    public void loadConfiguration(final InputStream inputStream) throws CacheException {
        JCSCacheServiceInit.getInstance().loadConfiguration(inputStream);
    }

    public void loadDefaultConfiguration() throws CacheException {
        JCSCacheServiceInit.getInstance().loadDefaultConfiguration();
    }

    public CacheKey newCacheKey(final int contextId, final int objectId) {
        return new CacheKeyImpl(contextId, objectId);
    }

    public CacheKey newCacheKey(final int contextId, final Serializable obj) {
        return new CacheKeyImpl(contextId, obj);
    }

    public CacheKey newCacheKey(final int contextId, final Serializable... objs) {
        return new CacheKeyImpl(contextId, objs);
    }

}
