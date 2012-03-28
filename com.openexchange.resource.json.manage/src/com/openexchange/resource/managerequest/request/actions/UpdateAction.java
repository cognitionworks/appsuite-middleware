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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.resource.managerequest.request.actions;

import java.util.Date;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.documentation.RequestMethod;
import com.openexchange.documentation.annotations.Action;
import com.openexchange.documentation.annotations.Parameter;
import com.openexchange.exception.OXException;
import com.openexchange.resource.ResourceService;
import com.openexchange.resource.managerequest.request.ResourceAJAXRequest;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link UpdateAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@Action(method = RequestMethod.PUT, name = "update", description = "Change a resource", parameters = {
    @Parameter(name = "session", description = "A session ID previously obtained from the login module."),
    @Parameter(name = "id", description = "Object ID of the resource to update."),
    @Parameter(name = "timestamp", description = "Time stamp of the resource to update. If the resource was modified after the specified time stamp, then the update must fail.")
}, requestBody = "Resource object as described in Resource response. Only modified fields are present and the field id is omitted.",
responseDescription = "Nothing, except the standard response object with empty data, the timestamp of the updated resource, and maybe errors.")
public final class UpdateAction extends AbstractResourceAction {

    /**
     * Initializes a new {@link UpdateAction}.
     * @param services
     */
    public UpdateAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final ResourceAJAXRequest req) throws OXException, JSONException {
        final ResourceService resourceService = getService(ResourceService.class);
        if (null == resourceService) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create( ResourceService.class.getName());
        }
        /*
         * Check for "data"
         */
        final int identifier = req.checkInt(AJAXServlet.PARAMETER_ID);
        final JSONObject jData = req.getData();
        final com.openexchange.resource.Resource resource = com.openexchange.resource.json.ResourceParser.parseResource(jData);
        resource.setIdentifier(identifier);
        final Date clientLastModified = req.getDate(AJAXServlet.PARAMETER_TIMESTAMP);
        /*
         * Update resource
         */
        final ServerSession session = req.getSession();
        resourceService.update(session.getUser(), session.getContext(), resource, clientLastModified);
        /*
         * Write empty JSON object
         */
        return new AJAXRequestResult(new JSONObject(), resource.getLastModified(), "json");
    }

}
