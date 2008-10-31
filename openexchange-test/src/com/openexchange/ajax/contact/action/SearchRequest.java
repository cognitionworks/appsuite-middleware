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
package com.openexchange.ajax.contact.action;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.fields.SearchFields;
import com.openexchange.ajax.framework.AJAXRequest;
import com.openexchange.ajax.framework.AbstractAJAXParser;

/**
 * @author Francisco Laguna <francisco.laguna@open-xchange.com>
 */
public class SearchRequest  extends AbstractContactRequest<SearchResponse> {
    private final JSONObject body = new JSONObject();
    private final SearchParser searchParser;
    private final List<AJAXRequest.Parameter> params = new ArrayList<AJAXRequest.Parameter>();

    public SearchRequest(final String pattern, final int inFolder, final int[] columns, final boolean failOnError) {
        this(pattern, inFolder, columns, -1, null, failOnError);
    }

    public SearchRequest(final String pattern, final int inFolder, final int[] columns, final int orderBy, final String orderDir, final boolean failOnError) {
        searchParser = new SearchParser(failOnError, columns);

        param(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_SEARCH);
        param(AJAXServlet.PARAMETER_COLUMNS, join(columns));
        if(orderBy != -1) {
            param(AJAXServlet.PARAMETER_SORT, String.valueOf(orderBy));
            param(AJAXServlet.PARAMETER_ORDER, orderDir);
        }

        try {
            if(inFolder != -1) {
                body.put(AJAXServlet.PARAMETER_INFOLDER, inFolder);
            }
            if(pattern != null) {
                body.put(SearchFields.PATTERN, pattern);
            }
        } catch (final JSONException e) {
            throw new IllegalStateException(e); // Shouldn't happen
        }


    }

    private void param(final String key, final String value) {
        if(value != null) {
            params.add(new AJAXRequest.Parameter(key, value));
        }
    }

    public AJAXRequest.Method getMethod() {
        return AJAXRequest.Method.PUT;
    }

    public AJAXRequest.Parameter[] getParameters() {
        return params.toArray(new AJAXRequest.Parameter[params.size()]);
    }

    public AbstractAJAXParser<SearchResponse> getParser() {
        return searchParser;
    }

    public Object getBody() throws JSONException {
        return body;
    }

    private String join(final int[] values) {
        final StringBuilder b = new StringBuilder();
        for(final int v : values) { b.append(v).append(", ");}
        b.setLength(b.length()-2);
        return b.toString();
    }
}
