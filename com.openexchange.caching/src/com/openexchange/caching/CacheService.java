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

package com.openexchange.caching;

import java.io.InputStream;
import java.io.Serializable;
import com.openexchange.exception.OXException;

/**
 * {@link CacheService} - The cache service.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface CacheService {

    /**
     * Gets a cache which accesses the provided region.
     * <p>
     * An already initialized cache for specified region is kept in a map to avoid multiple instantiations for the same region.
     *
     * @param name The region name
     * @return A cache which accesses the provided region.
     * @throws OXException If cache cannot be obtained
     */
    public Cache getCache(String name) throws OXException;

    /**
     * The cache identified through given name is removed from this cache service and all of its items are going to be disposed.<br>
     * <b>Note</b>: Caches which were added through default configuration are not freed from this cache service; meaning those invocations
     * are treated as a no-op.
     * <p>
     * The cache is then no more accessible through this cache service; meaning attempts to get the cache by {@link #getCache(String)} will
     * fail.
     * <p>
     * The freed cache is re-accessible if its configuration is again fed into this cache service via {@link #loadConfiguration(String)} or
     * {@link #loadDefaultConfiguration()}.
     *
     * @param name The name of the cache region that ought to be freed
     */
    public void freeCache(String name) throws OXException;

    /**
     * Additionally feeds the cache manager with specified cache configuration file.
     * <p>
     * The cache manager reads a default configuration - defined through property "com.openexchange.caching.configfile" in
     * 'system.properties' file - on initialization automatically. Therefore this method is useful to extend or overwrite the loaded default
     * configuration and needs <b>not</b> to be invoked to initialize the cache manager at all.
     *
     * @param cacheConfigFile The cache configuration file
     * @throws OXException If configuration fails
     */
    public void loadConfiguration(String cacheConfigFile) throws OXException;

    /**
     * Additionally feeds the cache manager with specified input stream. The stream will be closed.
     * <p>
     * The cache manager reads a default configuration - defined through property "com.openexchange.caching.configfile" in
     * 'system.properties' file - on initialization automatically. Therefore this method is useful to extend or overwrite the loaded default
     * configuration and needs <b>not</b> to be invoked to initialize the cache manager at all.
     *
     * @param inputStream The input stream to read from
     * @throws OXException If configuration fails
     */
    public void loadConfiguration(InputStream inputStream) throws OXException;

    /**
     * Re-Loads the cache manager's default configuration.
     *
     * @throws OXException If configuration fails
     */
    public void loadDefaultConfiguration() throws OXException;

    /**
     * Creates a new instance of {@link CacheKey} consisting of specified context ID and object ID.
     *
     * @param contextId The context ID
     * @param objectId The object ID
     * @return The new instance of {@link CacheKey}
     */
    public CacheKey newCacheKey(int contextId, int objectId);

    /**
     * Creates a new instance of {@link CacheKey} consisting of specified context ID and serializable object.
     *
     * @param contextId The context ID
     * @param obj The serializable object
     * @return new instance of {@link CacheKey}
     */
    public CacheKey newCacheKey(int contextId, Serializable obj);

    /**
     * Creates a new instance of {@link CacheKey} consisting of specified context ID and serializable objects.
     *
     * @param contextId The context ID
     * @param objs The serializable objects
     * @return new instance of {@link CacheKey}
     */
    public CacheKey newCacheKey(int contextId, Serializable... objs);

}
