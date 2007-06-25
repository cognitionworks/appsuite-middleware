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

package com.openexchange.groupware;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.jcs.JCS;
import org.apache.jcs.access.exception.CacheException;

import com.openexchange.cache.CacheKey;
import com.openexchange.cache.Configuration;
import com.openexchange.configuration.ConfigurationInit;
import com.openexchange.groupware.UserConfigurationException.UserConfigurationCode;
import com.openexchange.groupware.contexts.Context;

/**
 * CachingUserConfigurationStorage
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public class CachingUserConfigurationStorage extends UserConfigurationStorage {

	private static final String CACHE_REGION_NAME = "UserConfiguration";

	private transient final UserConfigurationStorage delegateStorage;

	private final Lock WRITE_LOCK;

	private final JCS cache;

	/**
	 * Constructor
	 * 
	 * @param ctx -
	 *            the context
	 * @throws UserConfigurationException -
	 *             if cache initialization fails
	 */
	public CachingUserConfigurationStorage() throws UserConfigurationException {
		super();
		WRITE_LOCK = new ReentrantLock();
		this.delegateStorage = new RdbUserConfigurationStorage();
		try {
			ConfigurationInit.init();
			Configuration.load();
			cache = JCS.getInstance(CACHE_REGION_NAME);
		} catch (final CacheException e) {
			throw new UserConfigurationException(UserConfigurationCode.CACHE_INITIALIZATION_FAILED, e,
					CACHE_REGION_NAME);
		} catch (final FileNotFoundException e) {
			throw new UserConfigurationException(UserConfigurationCode.CACHE_INITIALIZATION_FAILED, e,
					CACHE_REGION_NAME);
		} catch (final IOException e) {
			throw new UserConfigurationException(UserConfigurationCode.CACHE_INITIALIZATION_FAILED, e,
					CACHE_REGION_NAME);
		} catch (final AbstractOXException e) {
			throw new UserConfigurationException(UserConfigurationCode.CACHE_INITIALIZATION_FAILED, e,
					CACHE_REGION_NAME);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.openexchange.groupware.UserConfigurationStorage#getUserConfiguration(int)
	 */
	@Override
	public UserConfiguration getUserConfiguration(final int userId, final Context ctx)
			throws UserConfigurationException {
		return getUserConfiguration(userId, null, ctx);
	}

	private static final CacheKey getKey(final int userId, final Context ctx) {
		return new CacheKey(ctx, userId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.openexchange.groupware.UserConfigurationStorage#getUserConfiguration(int,
	 *      int[])
	 */
	@Override
	public UserConfiguration getUserConfiguration(final int userId, final int[] groups, final Context ctx)
			throws UserConfigurationException {
		final CacheKey key = getKey(userId, ctx);
		UserConfiguration userConfig = (UserConfiguration) cache.get(key);
		if (null == userConfig) {
			WRITE_LOCK.lock();
			try {
				if (null == (userConfig = (UserConfiguration) cache.get(key))) {
					userConfig = delegateStorage.getUserConfiguration(userId, groups, ctx);
					cache.put(key, userConfig);
				}
			} catch (final CacheException e) {
				throw new UserConfigurationException(UserConfigurationCode.CACHE_PUT_ERROR, e, e.getLocalizedMessage());
			} finally {
				WRITE_LOCK.unlock();
			}
		}
		return userConfig;
	}

}
