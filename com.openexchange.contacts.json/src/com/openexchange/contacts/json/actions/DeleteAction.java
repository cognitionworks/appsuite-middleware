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

package com.openexchange.contacts.json.actions;

import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.contacts.json.ContactRequest;
import com.openexchange.documentation.RequestMethod;
import com.openexchange.documentation.annotations.Action;
import com.openexchange.documentation.annotations.Parameter;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.ContactInterface;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link DeleteAction}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
*/
@Action(method = RequestMethod.PUT, name = "delete", description = "Delete contacts.", parameters = {
    @Parameter(name = "session", description = "A session ID previously obtained from the login module."),
    @Parameter(name = "timestamp", description = "Timestamp of the last update of the deleted contacts.")
}, requestBody = "An object with the fields \"id\" or \"ids\" and \"folder\". \"id\" contains an integer value for deleting a single contact while \"ids\" must not be set. Use the array \"ids\" to delete multiple contacts.",
responseDescription = "")
public class DeleteAction extends ContactAction {

    /**
     * Initializes a new {@link DeleteAction}.
     * @param serviceLookup
     */
    public DeleteAction(final ServiceLookup serviceLookup) {
        super(serviceLookup);
    }

    @Override
    protected AJAXRequestResult perform(final ContactRequest req) throws OXException {
        final ServerSession session = req.getSession();
        final long timestamp = req.getTimestamp();
        final Date date = new Date(timestamp);
        if (req.getData() instanceof JSONObject) {
            final int[] deleteRequestData = req.getDeleteRequestData();
            final ContactInterface contactInterface = getContactInterfaceDiscoveryService().newContactInterface(
                deleteRequestData[1],
                session);
            contactInterface.deleteContactObject(deleteRequestData[0], deleteRequestData[1], date);
        } else {
            JSONArray jsonArray = (JSONArray) req.getData();
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject json = jsonArray.getJSONObject(i);
                    final int id = json.getInt("id");
                    final int folder = json.getInt("folder");
                    final ContactInterface contactInterface = getContactInterfaceDiscoveryService().newContactInterface(
                        folder,
                        session);
                    contactInterface.deleteContactObject(id, folder, date);
                } catch (JSONException e) {
                    throw AjaxExceptionCodes.JSON_ERROR.create(e);
                }
            }
        }
        final JSONObject response = new JSONObject();
        return new AJAXRequestResult(response, date, "json");
    }
    
    @Override
    protected AJAXRequestResult perform2(final ContactRequest req) throws OXException {
        final ServerSession session = req.getSession();
        final long timestamp = req.getTimestamp();
        final Date date = new Date(timestamp);
        
        if (req.getData() instanceof JSONObject) {
            final int[] deleteRequestData = req.getDeleteRequestData();
            getContactService().deleteContact(session, Integer.toString(deleteRequestData[1]), Integer.toString(deleteRequestData[0]), date);
        } else {
            JSONArray jsonArray = (JSONArray) req.getData();
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject json = jsonArray.getJSONObject(i);
                    final int id = json.getInt("id");
                    final int folder = json.getInt("folder");
                    getContactService().deleteContact(session, String.valueOf(folder), String.valueOf(id), date);
                } catch (JSONException e) {
                    throw AjaxExceptionCodes.JSON_ERROR.create(e);
                }
            }
        }
        final JSONObject response = new JSONObject();
        return new AJAXRequestResult(response, date, "json");
    }

}
