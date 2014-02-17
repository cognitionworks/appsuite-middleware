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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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
package com.openexchange.find;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import com.openexchange.find.facet.Facet;
import com.openexchange.find.facet.FacetValue;

/**
 * Encapsulates an autocomplete request.
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since 7.6.0
 */
public class AutocompleteRequest implements Serializable {

    private static final long serialVersionUID = -5927763265822552092L;

    private final String prefix;

    private final List<Facet> activeFacets;

    /**
     * Initializes a new {@link AutocompleteRequest}.
     *
     * @param prefix The prefix to autocomplete on; must not be <code>null</code>
     */
    public AutocompleteRequest(final String prefix) {
        this(prefix, Collections.<Facet>emptyList());
    }

    /**
     * Initializes a new {@link AutocompleteRequest}.
     *
     * @param prefix The prefix to autocomplete on; must not be <code>null</code>
     * @param activeFacets The list of currently active facets; must not be <code>null</code>
     */
    public AutocompleteRequest(final String prefix, final List<Facet> activeFacets) {
        super();
        this.prefix = prefix;
        this.activeFacets = activeFacets;
    }

    /**
     * @return The prefix to autocomplete on. Must not end
     * with a wildcard character. Must never be <code>null</code>.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Gets a list of facets that are currently set. The
     * according {@link FacetValue}s have to be removed from
     * the response object.
     * @return The list of facets. May be empty but never <code>null</code>.
     */
    public List<Facet> getActiveFactes() {
        return activeFacets;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AutocompleteRequest other = (AutocompleteRequest) obj;
        if (prefix == null) {
            if (other.prefix != null) {
                return false;
            }
        } else if (!prefix.equals(other.prefix)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "AutocompleteRequest [prefix=" + prefix + "]";
    }

}
