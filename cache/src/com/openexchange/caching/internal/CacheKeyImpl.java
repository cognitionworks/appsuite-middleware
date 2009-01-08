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

package com.openexchange.caching.internal;

import java.io.Serializable;
import com.openexchange.caching.CacheKey;

/**
 * {@link CacheKeyImpl} - A cache key that consists of a context ID and an unique (serializable) identifier of any object.
 */
public class CacheKeyImpl implements CacheKey {

    /**
     * For serialization.
     */
    private static final long serialVersionUID = -3144968305668671430L;

    /**
     * Unique identifier of the context.
     */
    private final int contextId;

    /**
     * Object key of the cached object.
     */
    private final Serializable keyObj;

    /**
     * Hash code of the context specific object.
     */
    private final int hash;

    /**
     * Initializes a new {@link CacheKeyImpl}
     * 
     * @param contextId The context ID
     * @param objectId The object ID
     */
    public CacheKeyImpl(final int contextId, final int objectId) {
        this(contextId, Integer.valueOf(objectId));
    }

    /**
     * Initializes a new {@link CacheKeyImpl}
     * 
     * @param contextId The context ID
     * @param obj Any instance of {@link Serializable} to identify the cached object.
     */
    public CacheKeyImpl(final int contextId, final Serializable obj) {
        super();
        this.contextId = contextId;
        keyObj = obj;
        hash = obj.hashCode() ^ contextId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof CacheKeyImpl)) {
            return false;
        }
        final CacheKeyImpl other = (CacheKeyImpl) obj;
        return contextId == other.contextId && keyObj.equals(other.keyObj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return new StringBuilder(32).append("CacheKey: context=").append(contextId).append(" | key=").append(keyObj.toString()).toString();
    }

    public int getContextId() {
        return contextId;
    }

    public Serializable getObject() {
        return keyObj;
    }
}
