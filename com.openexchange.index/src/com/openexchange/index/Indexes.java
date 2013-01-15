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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.index;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * {@link Indexes} - Utility class.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class Indexes {

    /**
     * Initializes a new {@link Indexes}.
     */
    private Indexes() {
        super();
    }

    private static final class EmptyIndexResult implements IndexResult<Object>, Serializable {

        private static final long serialVersionUID = 619652174130414850L;

        protected EmptyIndexResult() {
            super();
        }

        @Override
        public int getNumFound() {
            return 0;
        }

        @Override
        public List<IndexDocument<Object>> getResults() {
            return Collections.emptyList();
        }

        @Override
        public Map<IndexField, Map<String, Long>> getFacetCounts() {
            return Collections.emptyMap();
        }

    }

    private static final EmptyIndexResult EMPTY_INDEX_RESULT = new EmptyIndexResult();

    /**
     * Gets the empty <b>IndexResult</b> (immutable). This <b>IndexResult</b> is serializable.
     *
     * @return The empty <b>IndexResult</b>
     */
    @SuppressWarnings("unchecked")
    public static final <V> IndexResult<V> emptyResult() {
        return (IndexResult<V>) EMPTY_INDEX_RESULT;
    }

}
