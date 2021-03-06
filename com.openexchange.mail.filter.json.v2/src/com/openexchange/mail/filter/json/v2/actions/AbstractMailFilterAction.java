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

package com.openexchange.mail.filter.json.v2.actions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.EnqueuableAJAXActionService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mailfilter.Credentials;
import com.openexchange.mailfilter.exceptions.MailFilterExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link AbstractMailFilterAction}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.4
 */
public abstract class AbstractMailFilterAction implements EnqueuableAJAXActionService {

    protected final ServiceLookup services;
    private static final String UserNameParameter = "username";

    /**
     * Initializes a new {@link AbstractMailFilterAction}.
     */
    protected AbstractMailFilterAction(ServiceLookup services) {
        super();
        this.services = services;
    }

    protected JSONObject getJSONBody(Object data) throws OXException {

        if (!(data instanceof JSONObject)){
            throw AjaxExceptionCodes.INVALID_REQUEST_BODY.create(JSONObject.class.getSimpleName(), data.getClass().getSimpleName());
        }

        return (JSONObject) data;
    }

    protected JSONArray getJSONArrayBody(Object data) throws OXException {

        if (!(data instanceof JSONArray)){
            throw AjaxExceptionCodes.INVALID_REQUEST_BODY.create(JSONObject.class.getSimpleName(), data.getClass().getSimpleName());
        }

        return (JSONArray) data;
    }

    protected Integer getUniqueId(final JSONObject json) throws OXException {
        if (json.has("id") && !json.isNull("id")) {
            try {
                return Integer.valueOf(json.getInt("id"));
            } catch (JSONException e) {
                throw MailFilterExceptionCode.ID_MISSING.create();
            }
        }
        throw MailFilterExceptionCode.MISSING_PARAMETER.create("id");
    }

    protected Credentials getCredentials(Session session, AJAXRequestData request) {
        Credentials credentials = new Credentials(session);
        String userName = getUserName(request);
        if (Strings.isNotEmpty(userName)) {
            credentials.setUsername(userName);
        }
        return credentials;
    }

    private String getUserName(AJAXRequestData request) {
        return request.getParameter(UserNameParameter);
    }

    @Override
    public Result isEnqueueable(AJAXRequestData request, ServerSession session) throws OXException {
        return EnqueuableAJAXActionService.resultFor(false);
    }

}
