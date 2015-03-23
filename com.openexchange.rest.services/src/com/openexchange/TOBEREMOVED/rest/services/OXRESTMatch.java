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

package com.openexchange.TOBEREMOVED.rest.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * An {@link OXRESTMatch} denotes a successful match of a route for a path.
 * e.g. the route /resources/:myResourceId matches the path /resources/12 with a
 * value for myResourceId of 12
 *
 * @see #getParameters()
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class OXRESTMatch {

    private final Map<String, String> parameters;
    private final OXRESTRoute route;
    private final List<String> parameterNames;

    /**
     * Initializes a new {@link OXRESTMatch}.
     *
     * @param route The associated route that creates this match
     * @param parameters The parameter map
     * @param parameterNames The parameter names (from left to right)
     */
    public OXRESTMatch(OXRESTRoute route, Map<String, String> parameters, ArrayList<String> parameterNames) {
        super();
        this.parameters = parameters;
        this.route = route;
        this.parameterNames = parameterNames;
    }

    /**
     * The route that created this match
     */
    public OXRESTRoute getRoute() {
        return route;
    }

    /**
     * Get the parameter name (from left to right) by index
     */
    public String getParameterName(int index) {
        try {
            return parameterNames.get(index);
        } catch (IndexOutOfBoundsException e) {
            // No such parameter index
            return null;
        }
    }

    /**
     * Get extracted parameters
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(512);
        if (route != null) {
            builder.append(route).append(" ");
        }
        if (parameters != null) {
            builder.append(parameters).append(" ");
        }
        return builder.toString();
    }

}
