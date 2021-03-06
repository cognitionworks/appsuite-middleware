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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.find.json.actions;

import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.EnqueuableAJAXActionService;
import com.openexchange.ajax.requesthandler.jobqueue.JobKey;
import com.openexchange.exception.OXException;
import com.openexchange.find.Module;
import com.openexchange.find.SearchRequest;
import com.openexchange.find.SearchResult;
import com.openexchange.find.SearchService;
import com.openexchange.find.facet.ActiveFacet;
import com.openexchange.find.json.FindRequest;
import com.openexchange.find.json.Offset;
import com.openexchange.find.json.QueryResult;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link QueryAction}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since 7.6.0
 */
public class QueryAction extends AbstractFindAction implements EnqueuableAJAXActionService {

    /**
     * Initializes a new {@link QueryAction}.
     *
     * @param services The service look-up
     */
    public QueryAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    public EnqueuableAJAXActionService.Result isEnqueueable(AJAXRequestData request, ServerSession session) throws OXException {
        try {
            String module = request.requireParameter("module");

            JSONObject data = (JSONObject) request.requireData();
            long start = data.getLong("start");
            long size = data.getLong("size");
            JSONArray jFacets = data.getJSONArray("facets");

            JSONObject jKeyDesc = new JSONObject(4);
            jKeyDesc.put("module", "find");
            jKeyDesc.put("action", "query");
            jKeyDesc.put("findModule", module);
            jKeyDesc.put("start", start);
            jKeyDesc.put("size", size);
            jKeyDesc.put("facets", jFacets);

            return EnqueuableAJAXActionService.resultFor(true, new JobKey(session.getUserId(), session.getContextId(), jKeyDesc.toString()), this);
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    protected AJAXRequestResult doPerform(final FindRequest request) throws OXException, JSONException {
        final SearchService searchService = getSearchService();
        final String[] columns = request.getColumns();
        final Module module = request.requireModule();
        final Offset offset = request.getOffset();
        if (offset.len <= 0) {
            return new AJAXRequestResult(SearchResult.EMPTY, SearchResult.class.getName());
        }

        final List<ActiveFacet> activeFacets = request.getActiveFacets();
        Map<String, String> options = request.getOptions();
        final SearchRequest searchRequest = new SearchRequest(offset.off, offset.len, activeFacets, options, columns);
        final SearchResult searchResult = searchService.search(searchRequest, module, request.getServerSession());
        return new AJAXRequestResult(new QueryResult(searchRequest, searchResult), QueryResult.class.getName());
    }

}
