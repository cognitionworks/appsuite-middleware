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

package com.openexchange.mail.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.openexchange.caching.CacheKey;
import com.openexchange.mail.MailSessionParameterNames;
import com.openexchange.session.Session;

/**
 * {@link SessionMailCache} - Several cacheable data bound to a user session.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public final class SessionMailCache {

    private final Map<CacheKey, Object> cache;

    /**
     * Gets the session-bound mail cache.
     * 
     * @param session The session whose mail cache shall be returned
     * @return The session-bound mail cache.
     */
    public static SessionMailCache getInstance(final Session session) {
        SessionMailCache mailCache = null;
        try {
            mailCache = (SessionMailCache) session.getParameter(MailSessionParameterNames.PARAM_MAIL_CACHE);
        } catch (final ClassCastException e) {
            /*
             * Class version does not match; just renew session cache.
             */
            mailCache = null;
        }
        if (null == mailCache) {
            synchronized (session) {
                mailCache = (SessionMailCache) session.getParameter(MailSessionParameterNames.PARAM_MAIL_CACHE);
                if (null == mailCache) {
                    mailCache = new SessionMailCache();
                    session.setParameter(MailSessionParameterNames.PARAM_MAIL_CACHE, mailCache);
                }
            }
        }
        return mailCache;
    }

    /**
     * Default constructor
     */
    private SessionMailCache() {
        super();
        cache = new ConcurrentHashMap<CacheKey, Object>();
    }

    /**
     * Puts specified <code>entry</code> into cache if
     * {@link SessionMailCacheEntry#getValue()} is not <code>null</code>.
     * <p>
     * {@link SessionMailCacheEntry#getKey()} is used as key and
     * {@link SessionMailCacheEntry#getValue()} as value.
     * 
     * @param <V> The cache entry's type
     * @param entry The mail cache entry
     */
    public void put(final SessionMailCacheEntry<?> entry) {
        if (null != entry.getValue()) {
            cache.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Gets the entry acquired through {@link SessionMailCacheEntry#getKey()}.
     * If present it's applied to <code>entry</code> via
     * {@link SessionMailCacheEntry#setValue(Object)}.
     * 
     * @param <V> The cache entry's type
     * @param entry The mail cache entry
     */
    public <V extends Object> void get(final SessionMailCacheEntry<V> entry) {
        entry.setValue(entry.getEntryClass().cast(cache.get(entry.getKey())));
    }

    /**
     * Removes the entry acquired through {@link SessionMailCacheEntry#getKey()}
     * . If present it's applied to <code>entry</code> via
     * {@link SessionMailCacheEntry#setValue(Object)}.
     * 
     * @param <V> The cache entry's type
     * @param entry The mail cache entry
     */
    public <V extends Object> void remove(final SessionMailCacheEntry<V> entry) {
        entry.setValue(entry.getEntryClass().cast(cache.remove(entry.getKey())));
    }

    /**
     * Clears all entries contained in cache.
     */
    public void clear() {
        cache.clear();
    }
}
