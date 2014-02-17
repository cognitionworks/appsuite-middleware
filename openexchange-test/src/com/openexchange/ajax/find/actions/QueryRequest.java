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

package com.openexchange.ajax.find.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.find.PropDocument;
import com.openexchange.ajax.framework.AbstractAJAXParser;
import com.openexchange.find.Document;
import com.openexchange.find.SearchResult;
import com.openexchange.find.facet.Filter;

/**
 * {@link QueryRequest}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class QueryRequest extends AbstractFindRequest<QueryResponse> {

    private final boolean failOnError;
    private final int start;
    private final int size;
    private final List<String> queries;
    private final List<Filter> filters;
    private final String module;

    /**
     * Initializes a new {@link QueryRequest}.
     */
    public QueryRequest(int start, int size, List<String> queries, List<Filter> filters, String module) {
        this(true, start, size, queries, filters, module);
    }

    /**
     * Initializes a new {@link QueryRequest}.
     */
    public QueryRequest(boolean failOnError, int start, int size, List<String> queries, List<Filter> filters, String module) {
        super();
        this.failOnError = failOnError;
        this.start = start;
        this.size = size;
        this.queries = queries;
        this.filters = filters;
        this.module = module;
    }

    @Override
    public com.openexchange.ajax.framework.AJAXRequest.Method getMethod() {
        return com.openexchange.ajax.framework.AJAXRequest.Method.PUT;
    }

    @Override
    public com.openexchange.ajax.framework.AJAXRequest.Parameter[] getParameters() throws IOException, JSONException {
        final List<Parameter> list = new LinkedList<Parameter>();
        list.add(new Parameter(AJAXServlet.PARAMETER_ACTION, "query"));
        list.add(new Parameter("module", module));
        if (start >= 0 && size > 0) {
            list.add(new Parameter("start", start));
            list.add(new Parameter("size", size));
        }
        return list.toArray(new Parameter[0]);
    }

    @Override
    public AbstractAJAXParser<? extends QueryResponse> getParser() {
        return new QueryParser(failOnError);
    }

    @Override
    public Object getBody() throws IOException, JSONException {
        final JSONObject jBody = new JSONObject(3);

        // Add queries if present
        {
            final List<String> queries = this.queries;
            if (null != queries) {
                final JSONArray jQueries = new JSONArray(queries.size());
                for (final String sQuery : queries) {
                    jQueries.put(sQuery);
                }
                jBody.put("queries", jQueries);
            }
        }

        // Add filters if present
        final List<Filter> filters = this.filters;
        if (null != filters) {
            final JSONArray jFilters = new JSONArray(filters.size());
            for (final Filter filter : filters) {
                final JSONObject jFilter = new JSONObject(3);

                final Set<String> filterQueries = filter.getQueries();
                final JSONArray jQueries = new JSONArray(filterQueries.size());
                for (final String sQuery : filterQueries) {
                    jQueries.put(sQuery);
                }
                jFilter.put("queries", jQueries);

                final Set<String> fields = filter.getFields();
                final JSONArray jFields = new JSONArray(fields.size());
                for (final String sField : fields) {
                    jFields.put(sField);
                }
                jFilter.put("fields", jFields);
            }
            jBody.put("filters", jFilters);
        }

        return jBody;
    }

    private static class QueryParser extends AbstractAJAXParser<QueryResponse> {

        /**
         * Initializes a new {@link AutocompleteParser}.
         */
        protected QueryParser(final boolean failOnError) {
            super(failOnError);
        }

        @Override
        protected QueryResponse createResponse(final Response response) throws JSONException {
            final JSONObject jResponse = (JSONObject) response.getData();

            final int numFound = jResponse.optInt("numFound", -1);
            final int from = jResponse.optInt("from", -1);

            final JSONArray jDocuments = jResponse.getJSONArray("results");
            final int len = jDocuments.length();
            final List<Document> documents = new ArrayList<Document>(len);
            for (int i = 0; i < len; i++) {
                documents.add(new PropDocument(jDocuments.getJSONObject(i).asMap()));
            }

            return new QueryResponse(response, new SearchResult(numFound, from, documents));
        }
    }

}
