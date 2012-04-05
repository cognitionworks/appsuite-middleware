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

package com.openexchange.jslob.json.action;

import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.documentation.RequestMethod;
import com.openexchange.documentation.annotations.Action;
import com.openexchange.documentation.annotations.Parameter;
import com.openexchange.exception.OXException;
import com.openexchange.jslob.JSlob;
import com.openexchange.jslob.JSlobService;
import com.openexchange.jslob.json.JSlobRequest;
import com.openexchange.server.ServiceLookup;

/**
 * {@link SetAction}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 */
@Action(
    name = "set"
    , description = "Sets the JSlob associated with the current user and context. Performs a delete if the passed JSlob is null." 
    , method = RequestMethod.PUT
    , parameters = {
        @Parameter(name = "serviceId", description = "Identifier for the JSLobService lookup in the JSlobServiceRegistry.", optional=true)
        , @Parameter(name = "id", description = "The path of the JSlob.", optional=true)
    }
    , requestBody = "The JSON object to set with the current user and context or null to delete the JSlob."
)

public final class SetAction extends JSlobAction {

    /**
     * Initializes a new {@link SetAction}.
     * 
     * @param services The service look-up
     */
    public SetAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final JSlobRequest jslobRequest) throws OXException {
        String serviceId = jslobRequest.getParameter("serviceId", String.class);
        if (null == serviceId) {
            serviceId = DEFAULT_SERVICE_ID;
        }
        final JSlobService jslobService = getJSlobService(serviceId);

        final String id = jslobRequest.checkParameter("id");
        final Object data = jslobRequest.getRequestData().getData();
        /*
         * A null value is considered as a remove operation
         */
        final JSlob jslob = null == data ? JSlob.EMPTY_JSLOB : new JSlob((JSONObject) data);
        jslobService.set(id, jslob, jslobRequest.getUserId(), jslobRequest.getContextId());
        return new AJAXRequestResult();
    }

    @Override
    public String getAction() {
        return "set";
    }

}
