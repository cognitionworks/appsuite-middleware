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

package com.openexchange.groupware.filestore;

import static com.openexchange.java.Autoboxing.I;
import java.io.Serializable;
import java.sql.Connection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.cache.dynamic.impl.CacheProxy;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheService;
import com.openexchange.caching.dynamic.OXObjectFactory;
import com.openexchange.exception.OXException;
import com.openexchange.server.services.ServerServiceRegistry;

public class CachingFilestoreStorage extends FilestoreStorage {

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(CachingFilestoreStorage.class));
    private static final String REGION_NAME = "Filestore";
    private static final Lock CACHE_LOCK = new ReentrantLock();

    private final FilestoreStorage delegate;

    public CachingFilestoreStorage(final FilestoreStorage fs) {
        this.delegate = fs;
    }

    @Override
    public Filestore getFilestore(final int id) throws OXException {
        final FilestoreFactory factory = new FilestoreFactory(id, delegate);
        try {
            return CacheProxy.getCacheProxy(factory, REGION_NAME, Filestore.class);
        } catch (final IllegalArgumentException e) {
            /*
             * Should not occur
             */
            LOG.error(e.getMessage(), e);
            return delegate.getFilestore(id);
        } catch (final OXException e) {
            throw e;
        }
    }

    private static final class FilestoreFactory implements OXObjectFactory<Filestore> {

        private final Integer id;

        private final FilestoreStorage delegate;

        public FilestoreFactory(final int id, final FilestoreStorage delegate) {
            super();
            this.id = I(id);
            this.delegate = delegate;
        }

        @Override
        public Serializable getKey() {
            return id;
        }

        @Override
        public Filestore load() throws OXException {
            return delegate.getFilestore(id.intValue());
        }

        @Override
        public Lock getCacheLock() {
            return CACHE_LOCK;
        }
    }

    @Override
    public Filestore getFilestore(final Connection con, final int id) throws OXException {
        final CacheService service = ServerServiceRegistry.getInstance().getService(CacheService.class);
        Cache filestoreCache = null;
        if (service != null) {
            try {
                filestoreCache = service.getCache(REGION_NAME);
            } catch (final OXException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        Filestore retval = null;
        if (null != filestoreCache) {
            retval = (Filestore) filestoreCache.get(I(id));
        }
        if (null == retval) {
            retval = delegate.getFilestore(con, id);
            if (null != filestoreCache) {
                CACHE_LOCK.lock();
                try {
                    filestoreCache.put(I(id), retval);
                } catch (final OXException e) {
                    LOG.error(e.getMessage(), e);
                } finally {
                    CACHE_LOCK.unlock();
                }
            }
        }
        return retval;
    }
}
