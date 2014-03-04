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
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.framework.AbstractAJAXParser;
import com.openexchange.find.Module;
import com.openexchange.find.calendar.CalendarFacetType;
import com.openexchange.find.common.CommonFacetType;
import com.openexchange.find.common.SimpleDisplayItem;
import com.openexchange.find.contacts.ContactsFacetType;
import com.openexchange.find.drive.DriveFacetType;
import com.openexchange.find.facet.ActiveFacet;
import com.openexchange.find.facet.DisplayItem;
import com.openexchange.find.facet.DisplayItemVisitor;
import com.openexchange.find.facet.Facet;
import com.openexchange.find.facet.FacetType;
import com.openexchange.find.facet.FacetValue;
import com.openexchange.find.facet.Filter;
import com.openexchange.find.mail.MailFacetType;
import com.openexchange.find.tasks.TasksFacetType;

/**
 * {@link AutocompleteRequest}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class AutocompleteRequest extends AbstractFindRequest<AutocompleteResponse> {

    private final boolean failOnError;
    private final String prefix;
    private final String module;
    private final List<ActiveFacet> activeFacets;

    /**
     * Initializes a new {@link AutocompleteRequest}.
     */
    public AutocompleteRequest(final String prefix, final String module) {
        this(prefix, module, null, true);
    }

    /**
     * Initializes a new {@link AutocompleteRequest}.
     */
    public AutocompleteRequest(final String prefix, final String module, final List<ActiveFacet> activeFacets) {
        this(prefix, module, activeFacets, true);
    }

    /**
     * Initializes a new {@link AutocompleteRequest}.
     */
    public AutocompleteRequest(final String prefix, final String module, final List<ActiveFacet> activeFacets, final boolean failOnError) {
        super();
        this.failOnError = failOnError;
        this.prefix = prefix;
        this.module = module;
        this.activeFacets = activeFacets;
    }

    @Override
    public com.openexchange.ajax.framework.AJAXRequest.Method getMethod() {
        return com.openexchange.ajax.framework.AJAXRequest.Method.PUT;
    }

    @Override
    public com.openexchange.ajax.framework.AJAXRequest.Parameter[] getParameters() throws IOException, JSONException {
        final List<Parameter> list = new LinkedList<Parameter>();
        list.add(new Parameter(AJAXServlet.PARAMETER_ACTION, "autocomplete"));
        list.add(new Parameter("module", module));
        return list.toArray(new Parameter[0]);
    }

    @Override
    public AbstractAJAXParser<? extends AutocompleteResponse> getParser() {
        return new AutocompleteParser(module, failOnError);
    }

    @Override
    public Object getBody() throws IOException, JSONException {
        final JSONObject jBody = new JSONObject(2);
        jBody.put("prefix", prefix);
        addFacets(jBody, activeFacets);
        return jBody;
    }

    private static class AutocompleteParser extends AbstractAJAXParser<AutocompleteResponse> {

        private final String module;

        /**
         * Initializes a new {@link AutocompleteParser}.
         */
        protected AutocompleteParser(final String module, final boolean failOnError) {
            super(failOnError);
            this.module = module;
        }

        @Override
        protected AutocompleteResponse createResponse(final Response response) throws JSONException {
            final JSONObject jResponse = (JSONObject) response.getData();

            final JSONArray jFacets = jResponse.getJSONArray("facets");
            final int length = jFacets.length();
            final List<Facet> facets = new ArrayList<Facet>(length);
            for (int i = 0; i < length; i++) {
                facets.add(parseJFacet(jFacets.getJSONObject(i)));
            }
            return new AutocompleteResponse(response, facets);
        }

        private Facet parseJFacet(final JSONObject jFacet) throws JSONException {
            // Type information
            final String id = jFacet.getString("id");
            final FacetType facetType = facetTypeFor(Module.moduleFor(module), id);

            // Facets
            final JSONArray jFacetValues = jFacet.getJSONArray("values");
            final int len = jFacetValues.length();
            final List<FacetValue> values = new ArrayList<FacetValue>(len);
            for (int i = 0; i < len; i++) {
                values.add(parseJFacetValue(jFacetValues.getJSONObject(i)));
            }

            return new Facet(facetType, values);
        }

        private FacetValue parseJFacetValue(final JSONObject jFacetValue) throws JSONException {
            final String displayName = jFacetValue.getString("display_name");
            final int count = jFacetValue.optInt("count", -1);
            final List<Filter> filters = new LinkedList<Filter>();
            if (jFacetValue.has("filter")) {
                final JSONObject jFilter = jFacetValue.getJSONObject("filter");
                filters.add(parseJFilter(jFilter));
            } else {
                final JSONArray options = jFacetValue.getJSONArray("options");
                for (int i = 0; i < options.length(); i++) {
                    final JSONObject jOption = options.getJSONObject(i);
                    filters.add(parseJOption(jOption));
                }
            }

            return new FacetValue(jFacetValue.getString("id"), new SimpleDisplayItem(displayName), count, filters);
        }

        private Filter parseJOption(final JSONObject jOption) throws JSONException {
            final String id = jOption.optString("id");
            final String displayName = jOption.optString("display_name");
            JSONObject jFilter = jOption.getJSONObject("filter");
            final JSONArray jQueries = jFilter.getJSONArray("queries");
            int length = jQueries.length();
            final List<String> queries = new LinkedList<String>();
            for (int i = 0; i < length; i++) {
                queries.add(jQueries.getString(i));
            }

            final JSONArray jFields = jFilter.getJSONArray("fields");
            length= jFields.length();
            final List<String> fields = new LinkedList<String>();
            for (int i = 0; i < length; i++) {
                fields.add(jFields.getString(i));
            }

            return new Filter(id, displayName, fields, queries);
        }

        private Filter parseJFilter(final JSONObject jFilter) throws JSONException {
            final JSONArray jQueries = jFilter.getJSONArray("queries");
            int length = jQueries.length();
            final List<String> queries = new LinkedList<String>();
            for (int i = 0; i < length; i++) {
                queries.add(jQueries.getString(i));
            }

            final JSONArray jFields = jFilter.getJSONArray("fields");
            length= jFields.length();
            final List<String> fields = new LinkedList<String>();
            for (int i = 0; i < length; i++) {
                fields.add(jFields.getString(i));
            }

            return new Filter(fields, queries);
        }

        private static FacetType facetTypeFor(Module module, String id) {
            FacetType type = null;
            switch(module) {
                case MAIL:
                    type = MailFacetType.getById(id);
                    break;

                case CALENDAR:
                    type = CalendarFacetType.getById(id);
                    break;

                case CONTACTS:
                    type = ContactsFacetType.getById(id);
                    break;

                case DRIVE:
                    type = DriveFacetType.getById(id);
                    break;

                case TASKS:
                    type = TasksFacetType.getById(id);
                    break;

                default:
                    return null;
            }

            if (type == null) {
                type = CommonFacetType.getById(id);
            }

            return type;
        }

        private DisplayItem parseJDisplayItem(final JSONObject jDisplayItem) throws JSONException {
            final String defaultValue = jDisplayItem.getString("defaultValue");
            final Map<String, Object> item = jDisplayItem.asMap();
            return new DisplayItem() {

                @Override
                public String getDefaultValue() {
                    return defaultValue;
                }

                @Override
                public void accept(final DisplayItemVisitor visitor) {
                    // Nothing
                }

                @Override
                public Map<String, Object> getItem() {
                    return item;
                }

                @Override
                public boolean isLocalizable() {
                    return false;
                }
            };
        }
    }

}
